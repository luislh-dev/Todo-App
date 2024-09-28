// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    alias(libs.plugins.google.dagger.hilt.android) apply false
    id("com.google.devtools.ksp") version "1.9.20-1.0.13" apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
    alias(libs.plugins.crashlytics) apply false
}
