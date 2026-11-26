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
        versionCode = 174
        versionName = "1.1.50"
    }

    // One fixed signing key, so every build installs over the last one (no more uninstalling first).
    signingConfigs {
        getByName("debug") {
            storeFile = file("notiftiles-debug.keystore")
            // Your own key can come from GitHub Secrets (NT_KS_PASS); without it the key in the repo is used
            val pass = System.getenv("NT_KS_PASS")?.takeIf { it.isNotEmpty() } ?: "android"
            storePassword = pass
            keyAlias = "notiftiles"
            keyPassword = pass
        }
    }

    // A smaller, faster build signed with the same key, so it installs over the debug one
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug")
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
    testImplementation("junit:junit:4.13.2")
}
