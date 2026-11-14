import SwiftUI
import Shared

/// Guitar neck where the user picks the string to tune, plus the automatic mode switch and the
/// reference tone. `selectedString` is the user's choice; `targetString` is what the tuner is
/// currently comparing against, which in automatic mode is the detected string.
struct StringSelector: View {
    let strings: [GuitarString]
    let selectedString: Int32?
    let targetString: Int32?
    let tunedStrings: Set<Int32>
    let tint: Color
    let isPlayingReference: Bool
    let onSelectString: (Int32?) -> Void
    let onToggleReference: () -> Void

    private var activeString: Int32? { selectedString ?? targetString }
    private var tunedCount: Int { strings.filter { tunedStrings.contains($0.number) }.count }

    var body: some View {
        VStack(spacing: 12) {
            HStack {
                autoButton
                Spacer()
                referenceButton
            }
            GuitarNeck(strings: strings, selectedString: selectedString, activeString: activeString,
                       tunedStrings: tunedStrings, tint: tint, onSelectString: onSelectString)
            Text(caption).font(.footnote).multilineTextAlignment(.center)
                .foregroundStyle(tunedCount == strings.count ? TunerPalette.green : .gray)
        }
    }

    private var autoButton: some View {
        let automatic = selectedString == nil
        return Button { onSelectString(nil) } label: {
            HStack(spacing: 6) {
                if automatic { Image(systemName: "checkmark").font(.caption.bold()) }
                Text("auto_mode")
            }
            .padding(.horizontal, 14).padding(.vertical, 8)
            .background(automatic ? Color.white.opacity(0.16) : .clear, in: Capsule())
            .overlay(Capsule().stroke(Color.gray.opacity(0.6)))
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(automatic ? .isSelected : [])
    }

    /// Plays (or stops) the correct pitch of the active string; disabled until a string is known.
    private var referenceButton: some View {
        let note = strings.first { $0.number == activeString }?.note
        let title: String
        if isPlayingReference { title = NSLocalizedString("stop_reference", comment: "Stop the reference tone") }
        else if let note { title = String(format: NSLocalizedString("hear_note", comment: "Play the note of a string"), note) }
        else { title = NSLocalizedString("hear_reference", comment: "Play a reference note") }
        return Button(action: onToggleReference) {
            Label(title, systemImage: isPlayingReference ? "stop.fill" : "play.fill")
        }
        .buttonStyle(.bordered)
        .disabled(!isPlayingReference && note == nil)
    }

    private var caption: String {
        String(format: NSLocalizedString("tuned_progress", comment: "Strings in tune"), tunedCount, strings.count)
    }
}

/// Six strings over a fretboard; each column, from badge to the end of the neck, selects its string.
private struct GuitarNeck: View {
    let strings: [GuitarString]
    let selectedString: Int32?
    let activeString: Int32?
    let tunedStrings: Set<Int32>
    let tint: Color
    let onSelectString: (Int32?) -> Void

    /// Space above the nut that holds each string's number and note badge.
    private let headHeight: CGFloat = 80
    private let neckHeight: CGFloat = 92

    var body: some View {
        ZStack(alignment: .top) {
            Canvas { context, size in draw(in: &context, size: size) }
            HStack(spacing: 0) {
                ForEach(strings, id: \.number) { string in
                    let tuned = tunedStrings.contains(string.number)
                    Button { onSelectString(string.number) } label: {
                        VStack(spacing: 4) {
                            Text(String(format: NSLocalizedString("string_ordinal", comment: "String number"), Int(string.number)))
                                .font(.caption).foregroundStyle(.gray)
                            NoteBadge(note: string.note, selected: string.number == selectedString,
                                      active: string.number == activeString, tuned: tuned, tint: tint)
                            Spacer(minLength: 0)
                        }
                        .padding(.top, 2)
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                    .accessibilityElement(children: .ignore)
                    .accessibilityLabel(String(format: NSLocalizedString("string_description", comment: "String number and note"), Int(string.number), string.note))
                    .accessibilityValue(tuned ? Text("in_tune") : Text(""))
                    .accessibilityAddTraits(string.number == selectedString ? [.isButton, .isSelected] : .isButton)
                }
            }
        }
        .frame(height: headHeight + neckHeight)
    }

    private func draw(in context: inout GraphicsContext, size: CGSize) {
        let column = size.width / CGFloat(max(strings.count, 1))
        let nutY = headHeight
        let boardLeft = max(0, column / 2 - 20)
        let boardWidth = size.width - 2 * boardLeft
        let neckLength = size.height - nutY
        let bottom = CGPoint(x: 0, y: size.height)

        // The neck fades out at the bottom: it continues beyond what is drawn.
        context.fill(
            Path(CGRect(x: boardLeft, y: nutY, width: boardWidth, height: neckLength)),
            with: .linearGradient(Gradient(colors: [TunerPalette.fretboard, TunerPalette.fretboard.opacity(0)]),
                                  startPoint: CGPoint(x: 0, y: nutY), endPoint: bottom))
        for (position, alpha) in [(0.46, 0.7), (0.86, 0.3)] {
            let y = nutY + neckLength * position
            var fret = Path()
            fret.move(to: CGPoint(x: boardLeft, y: y))
            fret.addLine(to: CGPoint(x: boardLeft + boardWidth, y: y))
            context.stroke(fret, with: .color(TunerPalette.fret.opacity(alpha)), lineWidth: 2)
        }
        context.fill(
            Path(roundedRect: CGRect(x: boardLeft - 2, y: nutY - 3, width: boardWidth + 4, height: 6), cornerRadius: 3),
            with: .color(TunerPalette.nut))

        for (index, string) in strings.enumerated() {
            let x = column * (CGFloat(index) + 0.5)
            // Thickest string first, as on the instrument.
            let width = max(1.4, 4.2 - 0.55 * CGFloat(index))
            let top = CGPoint(x: x, y: nutY - 12)
            let color: Color
            if string.number == activeString { color = tint }
            else if tunedStrings.contains(string.number) { color = TunerPalette.green }
            else if index < strings.count / 2 { color = TunerPalette.woundString }
            else { color = TunerPalette.plainString }
            var line = Path()
            line.move(to: top)
            line.addLine(to: CGPoint(x: x, y: size.height))
            if string.number == activeString {
                context.stroke(line,
                               with: .linearGradient(Gradient(colors: [color.opacity(0.28), .clear]), startPoint: top, endPoint: bottom),
                               style: StrokeStyle(lineWidth: width + 10, lineCap: .round))
            }
            context.stroke(line,
                           with: .linearGradient(Gradient(colors: [color, color.opacity(0.1)]),
                                                 startPoint: CGPoint(x: 0, y: nutY), endPoint: bottom),
                           lineWidth: width)
        }
    }
}

/// Round note label. Filled when the user picked the string, outlined in the reading's color when
/// the tuner detected it, and marked with a green check once the string was confirmed in tune.
private struct NoteBadge: View {
    let note: String
    let selected: Bool
    let active: Bool
    let tuned: Bool
    let tint: Color

    var body: some View {
        let container: Color = selected ? tint : (active ? tint.opacity(0.16) : TunerPalette.background)
        let border: Color = active ? tint : (tuned ? TunerPalette.green : TunerPalette.track)
        let content: Color = selected ? TunerPalette.background : (active ? tint : (tuned ? TunerPalette.green : .white))
        Text(note).font(.system(size: 16, weight: .bold)).foregroundStyle(content)
            .frame(width: 44, height: 44)
            .background(container, in: Circle())
            .overlay(Circle().stroke(border, lineWidth: 2))
            .overlay(alignment: .topTrailing) {
                if tuned {
                    Image(systemName: "checkmark").font(.system(size: 9, weight: .black))
                        .foregroundStyle(TunerPalette.background)
                        .frame(width: 18, height: 18)
                        .background(TunerPalette.green, in: Circle())
                        .overlay(Circle().stroke(TunerPalette.background, lineWidth: 2))
                        .offset(x: 5, y: -5)
                }
            }
    }
}

/// Colors shared by the tuner views.
enum TunerPalette {
    static let background = Color(red: 0.06, green: 0.075, blue: 0.105)
    static let green = Color(red: 0.4, green: 0.89, blue: 0.67)
    static let red = Color(red: 1, green: 0.45, blue: 0.48)
    static let track = Color(red: 0.255, green: 0.286, blue: 0.361)
    static let fretboard = Color(red: 0.18, green: 0.149, blue: 0.133)
    static let fret = Color(red: 0.541, green: 0.565, blue: 0.627)
    static let nut = Color(red: 0.914, green: 0.89, blue: 0.824)
    static let woundString = Color(red: 0.788, green: 0.643, blue: 0.42)
    static let plainString = Color(red: 0.835, green: 0.855, blue: 0.89)
}

#Preview {
    StringSelector(strings: StandardTuning.shared.strings, selectedString: nil, targetString: 5,
                   tunedStrings: [6], tint: TunerPalette.red, isPlayingReference: false,
                   onSelectString: { _ in }, onToggleReference: {})
        .padding().background(TunerPalette.background).preferredColorScheme(.dark)
}
