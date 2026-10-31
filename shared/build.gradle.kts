plugins { kotlin("multiplatform"); id("com.android.kotlin.multiplatform.library") }
kotlin {
    android { namespace = "br.com.gotuner.shared"; compileSdk = 36; minSdk = 26 }
    jvm()
    listOf(iosArm64(), iosSimulatorArm64()).forEach { it.binaries.framework { baseName = "Shared"; isStatic = true } }
    sourceSets {
        commonMain.dependencies { implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0") }
        commonTest.dependencies { implementation(kotlin("test")) }
    }
}
