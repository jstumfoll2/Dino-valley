plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.littledungeon"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.jstumfoll2.thelittledungeon"
        minSdk = 26
        targetSdk = 36
        versionCode = 5
        versionName = "0.5.0"

        // The narrator voice ships for 64-bit ARM phones only, which is every phone this targets.
        ndk { abiFilters += "arm64-v8a" }
    }

    // The voice model and the recorded sentences and sounds are read straight from the APK;
    // compressing them would only slow loading (and sound files are compressed already).
    androidResources {
        noCompress += listOf("onnx", "bin", "ogg")
    }

    // A shared debug key, so every build from CI installs over the last one without
    // uninstalling (which would erase progress). Debug keys aren't secret; F-Droid signs its own.
    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":engine"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
