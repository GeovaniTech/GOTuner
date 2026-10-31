import java.net.URI

plugins {
 id("com.android.application")
 id("org.jetbrains.kotlin.plugin.compose")
 id("com.google.gms.google-services")
 id("com.google.firebase.crashlytics")
}
android {
 namespace = "br.com.gotuner.android"; compileSdk = 36
 defaultConfig { applicationId = "br.com.gotuner"; minSdk = 26; targetSdk = 36; versionCode = 1; versionName = "1.0.0"; testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" }
 buildFeatures { compose = true; buildConfig = true }
 val privacyUrl = providers.environmentVariable("GOTUNER_PRIVACY_POLICY_URL").orElse("").get()
 require(privacyUrl.isEmpty() || (URI(privacyUrl).scheme == "https" && !URI(privacyUrl).host.isNullOrBlank())) { "Privacy policy URL must be an HTTPS URL." }
 defaultConfig { buildConfigField("String", "PRIVACY_POLICY_URL", "\"$privacyUrl\"") }
 val signingValues = listOf("GOTUNER_KEYSTORE_PATH", "GOTUNER_KEYSTORE_PASSWORD", "GOTUNER_KEY_ALIAS", "GOTUNER_KEY_PASSWORD").map { providers.environmentVariable(it).orNull }
 require(signingValues.all { it == null } || signingValues.all { !it.isNullOrBlank() }) { "Set all four GOTuner signing environment variables, or none for an unsigned bundle." }
 if (signingValues.all { !it.isNullOrBlank() }) {
  signingConfigs.create("production") {
   storeFile = file(signingValues[0]!!)
   storePassword = signingValues[1]
   keyAlias = signingValues[2]
   keyPassword = signingValues[3]
  }
 }
 buildTypes {
  debug {
   manifestPlaceholders["telemetryEnabled"] = "false"
  }
  release {
   isDebuggable = false
   isMinifyEnabled = true
   isShrinkResources = true
   proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
   manifestPlaceholders["telemetryEnabled"] = "true"
   signingConfig = signingConfigs.findByName("production")
  }
 }
 testOptions { unitTests.isReturnDefaultValues = true }
 lint { abortOnError = true; checkReleaseBuilds = true }
}
dependencies {
 testImplementation("junit:junit:4.13.2")
 androidTestImplementation("androidx.test:runner:1.7.0")
 androidTestImplementation("androidx.test:rules:1.7.0")
 androidTestImplementation("androidx.test:core:1.7.0")
 androidTestImplementation("androidx.test.ext:junit:1.3.0")
 // Firebase transitively brings Fragment; keep ActivityResult permission dispatch compatible.
 implementation("androidx.fragment:fragment:1.8.9")
 implementation(project(":shared"))
 implementation(platform("com.google.firebase:firebase-bom:35.1.0"))
 implementation("com.google.firebase:firebase-analytics")
 implementation("com.google.firebase:firebase-crashlytics")
 implementation("androidx.core:core-splashscreen:1.2.0")
 implementation("androidx.activity:activity-compose:1.12.4")
 implementation("androidx.compose.material3:material3:1.3.1")
 implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
 implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
}
