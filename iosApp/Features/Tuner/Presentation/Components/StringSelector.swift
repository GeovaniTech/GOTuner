import SwiftUI
import Shared

/// Row of note buttons where the user picks the string to tune, plus the automatic mode switch and
/// the reference tone. `selectedString` is the user's choice; `targetString` is what the tuner is
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

    var body: some View {
        VStack(spacing: 12) {
            HStack {
                autoButton
                Spacer()
                referenceButton
            }
            StringButtons(strings: strings, selectedString: selectedString, activeString: activeString,
                          tunedStrings: tunedStrings, tint: tint, onSelectString: onSelectString)
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
}

/// One button per string, thickest first; the whole column, number and badge, selects its string.
private struct StringButtons: View {
    let strings: [GuitarString]
    let selectedString: Int32?
    let activeString: Int32?
    let tunedStrings: Set<Int32>
    let tint: Color
    let onSelectString: (Int32?) -> Void

    var body: some View {
        HStack(spacing: 0) {
            ForEach(strings, id: \.number) { string in
                let tuned = tunedStrings.contains(string.number)
                Button { onSelectString(string.number) } label: {
                    VStack(spacing: 4) {
                        Text(String(format: NSLocalizedString("string_ordinal", comment: "String number"), Int(string.number)))
                            .font(.caption).foregroundStyle(.gray)
                        NoteBadge(note: string.note, selected: string.number == selectedString,
                                  active: string.number == activeString, tuned: tuned, tint: tint)
                    }
                    .padding(.vertical, 6)
                    .frame(maxWidth: .infinity)
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
    static let neutral = Color(red: 0.616, green: 0.651, blue: 0.729)
    static let track = Color(red: 0.255, green: 0.286, blue: 0.361)
    static let tick = Color(red: 0.439, green: 0.471, blue: 0.541)
}

#Preview {
    StringSelector(strings: StandardTuning.shared.strings, selectedString: nil, targetString: 5,
                   tunedStrings: [6], tint: TunerPalette.red, isPlayingReference: false,
                   onSelectString: { _ in }, onToggleReference: {})
        .padding().background(TunerPalette.background).preferredColorScheme(.dark)
}
