import AVFoundation
import Shared

/// Plays a synthesized reference tone once through the shared audio session. Use from the main thread.
final class IOSTonePlayer: TonePlayer {
    private var engine: AVAudioEngine?
    private var node: AVAudioPlayerNode?
    private var generation = 0

    func play(samples: KotlinFloatArray, sampleRate: Int32, onFinished: @escaping () -> Void) {
        stop()
        generation += 1
        let token = generation
        // Stopping the node also fires its completion handler; the token drops those stale calls.
        let finish: () -> Void = { [weak self] in
            DispatchQueue.main.async {
                guard let self, self.generation == token else { return }
                self.teardown()
                onFinished()
            }
        }
        let count = Int(samples.size)
        guard count > 0,
              let format = AVAudioFormat(standardFormatWithSampleRate: Double(sampleRate), channels: 1),
              let buffer = AVAudioPCMBuffer(pcmFormat: format, frameCapacity: AVAudioFrameCount(count)),
              let channel = buffer.floatChannelData?[0]
        else { finish(); return }
        for index in 0..<count { channel[index] = samples.get(index: Int32(index)) }
        buffer.frameLength = AVAudioFrameCount(count)
        do {
            try TunerAudioSession.activate()
            let engine = AVAudioEngine()
            let node = AVAudioPlayerNode()
            engine.attach(node)
            engine.connect(node, to: engine.mainMixerNode, format: format)
            try engine.start()
            self.engine = engine
            self.node = node
            node.scheduleBuffer(buffer, at: nil, options: [], completionCallbackType: .dataPlayedBack) { _ in finish() }
            node.play()
        } catch {
            // A tone that cannot start still reports back, so the tuner resumes listening.
            finish()
        }
    }

    func stop() {
        generation += 1
        teardown()
    }

    private func teardown() {
        node?.stop()
        engine?.stop()
        node = nil
        engine = nil
    }
}
