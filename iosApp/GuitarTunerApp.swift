import SwiftUI

@main
struct GuitarTunerApp: App {
    @StateObject private var tunerStore = AppDependencies.makeTunerStore()
    var body: some Scene { WindowGroup { TunerScreen(store: tunerStore) } }
}
