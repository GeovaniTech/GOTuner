import SwiftUI

struct TuningIndicator: View {
    let cents: Double?
    let tint: Color
    var body: some View {
        VStack {
            GeometryReader { geometry in
                let width = max(0, geometry.size.width - 24)
                ZStack(alignment: .leading) {
                    Capsule().fill(.gray.opacity(0.4)).frame(height: 4)
                    ForEach(0..<11) { index in
                        Rectangle().fill(index == 5 ? Color.green : Color.gray).frame(width: 2, height: index == 5 ? 30 : 16).offset(x: 12 + width * Double(index) / 10)
                    }
                    if let cents {
                        Circle().fill(tint).frame(width: 16, height: 16).offset(x: 4 + width * (min(50, max(-50, cents)) + 50) / 100)
                            .animation(.easeOut(duration: 0.15), value: cents)
                    }
                }.frame(height: 64)
            }.frame(height: 64)
            HStack { Text("flat"); Spacer(); Text("in_tune"); Spacer(); Text("sharp") }
            HStack { Text("tighten_direction"); Spacer(); Text("loosen_direction") }.font(.caption).foregroundStyle(.secondary)
        }
    }
}

