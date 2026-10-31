import AVFoundation
import Shared

/// All engine operations and shared processing run on this serial worker.
final class IOSAudioRecorder {
    private let worker = DispatchQueue(label: "com.gotuner.audio", qos: .userInitiated)
    private let model: TunerViewModel
    private var engine: AVAudioEngine?
    private var observers: [NSObjectProtocol] = []
    private var desired = false
    private var interrupted = false
    private var generation = 0
    private var frame = KotlinFloatArray(size: 4096)
    private var filled = 0
    init(model: TunerViewModel) {
        self.model = model
        let center = NotificationCenter.default
        observers.append(center.addObserver(forName: AVAudioSession.interruptionNotification, object: nil, queue: nil) { [weak self] note in
            guard let value = note.userInfo?[AVAudioSessionInterruptionTypeKey] as? UInt else { return }
            self?.worker.async { [weak self] in
                guard let self else { return }
                self.interrupted = value == AVAudioSession.InterruptionType.began.rawValue
                self.stopEngine()
                if !self.interrupted && self.desired { self.startEngine() }
            }
        })
        observers.append(center.addObserver(forName: AVAudioSession.routeChangeNotification, object: nil, queue: nil) { [weak self] _ in
            self?.worker.async { [weak self] in
                guard let self, self.desired, !self.interrupted else { return }
                self.stopEngine(); self.startEngine()
            }
        })
    }
    deinit { observers.forEach(NotificationCenter.default.removeObserver) }
    func start() {
        worker.async { [weak self] in
            guard let self else { return }
            self.desired = true
            guard self.engine == nil else { return }
            AVAudioApplication.requestRecordPermission { [weak self] granted in
                self?.worker.async { [weak self] in
                    guard let self else { return }
                    self.model.permission(granted: granted)
                    if granted && self.desired && !self.interrupted { self.startEngine() }
                }
            }
        }
    }
    func stop() { worker.async { [weak self] in self?.desired = false; self?.stopEngine() } }
    private func startEngine() {
        guard engine == nil else { return }
        do {
            let session = AVAudioSession.sharedInstance()
            try session.setCategory(.record, mode: .measurement, options: [])
            try session.setPreferredSampleRate(48000)
            try session.setPreferredIOBufferDuration(0.02)
            try session.setActive(true)
            let audio = AVAudioEngine()
            let input = audio.inputNode
            let format = input.outputFormat(forBus: 0)
            guard format.sampleRate > 0, format.channelCount > 0 else { throw AudioError.noInput }
            filled = 0; generation += 1
            let token = generation
            // The callback copies into a fixed pool. A busy worker drops audio rather than queues unbounded buffers.
            let pool = AudioBufferPool()
            input.installTap(onBus: 0, bufferSize: 2048, format: format) { [weak self] buffer, _ in
                guard let self, let channel = buffer.floatChannelData?[0], let slot = pool.take() else { return }
                let count = min(Int(buffer.frameLength), slot.samples.count)
                slot.samples.withUnsafeMutableBufferPointer { destination in destination.baseAddress!.update(from: channel, count: count) }
                self.worker.async { [weak self] in
                    defer { pool.put(slot) }
                    guard let self, self.engine != nil, self.generation == token else { return }
                    for index in 0..<count {
                        self.frame.set(index: Int32(self.filled), value: slot.samples[index]); self.filled += 1
                        if self.filled == 4096 {
                            self.model.process(samples: self.frame, sampleRate: Int32(format.sampleRate))
                            for i in 0..<2048 { self.frame.set(index: Int32(i), value: self.frame.get(index: Int32(i+2048))) }
                            self.filled = 2048
                        }
                    }
                }
            }
            engine = audio
            audio.prepare(); try audio.start()
            model.listening(active: true)
        } catch { stopEngine(); model.fail(message: "Cannot access microphone. Check access in Settings and retry.") }
    }
    private func stopEngine() {
        generation += 1
        if let engine { engine.inputNode.removeTap(onBus: 0); engine.stop() }
        engine = nil; filled = 0; model.listening(active: false)
        try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
    }
    private enum AudioError: Error { case noInput }
}
private final class AudioSlot { var samples = [Float](repeating: 0, count: 8192) }
private final class AudioBufferPool: @unchecked Sendable {
    private let lock = NSLock()
    private var slots = [AudioSlot(), AudioSlot(), AudioSlot()]
    func take() -> AudioSlot? { guard lock.try() else { return nil }; defer { lock.unlock() }; return slots.popLast() }
    func put(_ slot: AudioSlot) { lock.lock(); slots.append(slot); lock.unlock() }
}
