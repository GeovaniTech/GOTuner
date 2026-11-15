import SwiftUI
import Shared

/// Strip of notes that slides under a fixed pointer, so the note under the pointer is the one the
/// sound is closest to. Every semitone is labelled and each tick between them is 10 cents; the
/// note of `targetFrequency`, the string being tuned, stands out from its neighbours. The detected
/// `frequency` is written under the pointer. Without a reading the strip rests on the target note.
struct NoteStrip: View {
    let frequency: Double?
    let targetFrequency: Double?
    let tint: Color

    /// E2, the lowest string: where the strip rests before anything is played or chosen.
    private let restingPosition = 40.0
    // Text follows Dynamic Type, so the strip grows with it instead of clipping labels.
    @ScaledMetric(relativeTo: .title) private var labelSize: CGFloat = 26
    @ScaledMetric(relativeTo: .title) private var octaveSize: CGFloat = 16
    @ScaledMetric(relativeTo: .subheadline) private var readingSize: CGFloat = 15

    var body: some View {
        let scale = NoteScale.shared
        let position = frequency.flatMap { scale.position(frequency: $0)?.doubleValue }
        let closest = frequency.flatMap { scale.closestSemitone(frequency: $0)?.int32Value }
        let target = targetFrequency.flatMap { scale.closestSemitone(frequency: $0)?.int32Value }
        let center = position ?? target.map(Double.init) ?? restingPosition
        let reading = (frequency.map { String(format: "%.1f", locale: Locale.current, $0) } ?? "--.-")
            + " " + NSLocalizedString("frequency_unit", comment: "Hertz")
        let shape = RoundedRectangle(cornerRadius: 12)

        NoteStripMarks(center: center, target: target, tint: tint, reading: reading,
                       labelSize: labelSize, octaveSize: octaveSize, readingSize: readingSize)
            .frame(height: NoteStripMarks.height(labelSize: labelSize, readingSize: readingSize))
            .clipShape(shape)
            .overlay(shape.stroke(TunerPalette.track, lineWidth: 1.5))
            .animation(.easeOut(duration: 0.2), value: center)
            .accessibilityElement()
            .accessibilityLabel(closest.map {
                String(format: NSLocalizedString("closest_note", comment: "Note under the pointer and its frequency"), scale.name(semitone: $0), reading)
            } ?? NSLocalizedString("no_note_detected", comment: "Nothing was heard yet"))
    }
}

/// Draws the strip centred on `center`, a position in semitones that animates as the pitch moves.
private struct NoteStripMarks: View, Animatable {
    var center: Double
    let target: Int32?
    let tint: Color
    let reading: String
    let labelSize: CGFloat
    let octaveSize: CGFloat
    let readingSize: CGFloat

    /// Semitones visible across the strip: the closest note plus one neighbour on each side.
    private static let visibleSemitones = 2.5
    /// One tick every 10 cents.
    private static let ticksPerSemitone = 10
    private static let lineHeight: CGFloat = 1.2
    private static let padding: CGFloat = 10
    private static let noteTickHeight: CGFloat = 50
    private static let halfTickHeight: CGFloat = 32
    private static let tickHeight: CGFloat = 20
    private static let arrowHeight: CGFloat = 12
    private static let arrowGap: CGFloat = 4

    static func height(labelSize: CGFloat, readingSize: CGFloat) -> CGFloat {
        (labelSize + readingSize) * lineHeight + padding * 2 + noteTickHeight + arrowGap * 2 + arrowHeight
    }

    var animatableData: Double {
        get { center }
        set { center = newValue }
    }

    var body: some View {
        Canvas { context, size in draw(in: context, size: size) }
    }

    private func draw(in context: GraphicsContext, size: CGSize) {
        let centerX = size.width / 2
        let semitoneWidth = size.width / Self.visibleSemitones
        let labelHeight = labelSize * Self.lineHeight
        let ticksBottom = Self.padding + labelHeight + Self.noteTickHeight
        let ticks = Self.ticksPerSemitone
        // Marks fade towards the edges: the strip continues beyond what is drawn.
        func fade(_ x: CGFloat) -> Double {
            let distance = min(1, abs(x - centerX) / centerX)
            return 1 - 0.8 * distance * distance
        }

        let firstTick = Int(((center - Self.visibleSemitones / 2) * Double(ticks)).rounded(.down)) - ticks
        let lastTick = Int(((center + Self.visibleSemitones / 2) * Double(ticks)).rounded(.up)) + ticks

        for tick in firstTick...lastTick {
            let x = centerX + (Double(tick) / Double(ticks) - center) * semitoneWidth
            let step = ((tick % ticks) + ticks) % ticks
            let isNote = step == 0
            let semitone = Int32((tick - step) / ticks)
            let color: Color = isNote ? (semitone == target ? .white : TunerPalette.neutral) : TunerPalette.tick
            let height = isNote ? Self.noteTickHeight : (step == ticks / 2 ? Self.halfTickHeight : Self.tickHeight)

            var line = Path()
            line.move(to: CGPoint(x: x, y: ticksBottom - height))
            line.addLine(to: CGPoint(x: x, y: ticksBottom))
            context.stroke(line, with: .color(color.opacity(fade(x))), lineWidth: isNote ? 2 : 1.5)

            if isNote {
                var faded = context
                faded.opacity = fade(x)
                faded.draw(label(NoteScale.shared.name(semitone: semitone), color: color),
                           at: CGPoint(x: x, y: Self.padding + labelHeight / 2))
            }
        }

        // Fixed pointer: the strip moves, the reading is always at the centre.
        var pointer = Path()
        pointer.move(to: CGPoint(x: centerX, y: ticksBottom - Self.noteTickHeight))
        pointer.addLine(to: CGPoint(x: centerX, y: ticksBottom))
        context.stroke(pointer, with: .color(tint), style: StrokeStyle(lineWidth: 3, lineCap: .round))

        let arrowTop = ticksBottom + Self.arrowGap
        let arrowBottom = arrowTop + Self.arrowHeight
        var arrow = Path()
        arrow.move(to: CGPoint(x: centerX, y: arrowTop))
        arrow.addLine(to: CGPoint(x: centerX + Self.arrowHeight * 0.7, y: arrowBottom))
        arrow.addLine(to: CGPoint(x: centerX - Self.arrowHeight * 0.7, y: arrowBottom))
        arrow.closeSubpath()
        context.fill(arrow, with: .color(tint))

        context.draw(Text(reading).font(.system(size: readingSize, weight: .medium)).foregroundColor(tint),
                     at: CGPoint(x: centerX, y: arrowBottom + Self.arrowGap), anchor: .top)
    }

    /// "D#2" with the octave smaller than the note, as on sheet music.
    private func label(_ name: String, color: Color) -> Text {
        let octaveStart = name.firstIndex { $0 == "-" || $0.isNumber } ?? name.endIndex
        var note = AttributedString(String(name[..<octaveStart]))
        note.font = .system(size: labelSize, weight: .bold)
        var octave = AttributedString(String(name[octaveStart...]))
        octave.font = .system(size: octaveSize, weight: .medium)
        return Text(note + octave).foregroundColor(color)
    }
}

#Preview {
    VStack(spacing: 16) {
        NoteStrip(frequency: 82.4, targetFrequency: 82.41, tint: TunerPalette.green)
        NoteStrip(frequency: 107.9, targetFrequency: 110, tint: TunerPalette.red)
        NoteStrip(frequency: nil, targetFrequency: 146.83, tint: .gray)
    }
    .padding().background(TunerPalette.background).preferredColorScheme(.dark)
}
