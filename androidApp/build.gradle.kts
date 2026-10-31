plugins { id("com.android.application"); id("org.jetbrains.kotlin.plugin.compose") }
android {
 namespace = "br.com.gotuner.android"; compileSdk = 36
 defaultConfig { applicationId = "br.com.gotuner"; minSdk = 26; targetSdk = 36; versionCode = 1; versionName = "1.0" }
 buildFeatures { compose = true }
}
dependencies {
 implementation(project(":shared"))
 implementation("androidx.core:core-splashscreen:1.2.0")
 implementation("androidx.activity:activity-compose:1.12.4")
 implementation("androidx.compose.material3:material3:1.3.1")
 implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
 implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
}
