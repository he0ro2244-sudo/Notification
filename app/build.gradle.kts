plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.notiftiles"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.notiftiles"
        minSdk = 31
        targetSdk = 34
        versionCode = 57
        versionName = "0.9.56"
    }

    // One fixed signing key, so every build installs over the last one (no more uninstalling first).
    signingConfigs {
        getByName("debug") {
            storeFile = file("notiftiles-debug.keystore")
            storePassword = "android"
            keyAlias = "notiftiles"
            keyPassword = "android"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // One-tap "turn on location" system prompt (falls back to the Location settings page without it).
    implementation("com.google.android.gms:play-services-location:21.3.0")
}
