import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Firebase (login + multiplayer) is enabled once app/google-services.json is present.
// Without it the app still builds and offline play works.
if (file("google-services.json").exists()) {
    apply(plugin = libs.plugins.google.services.get().pluginId)
}

// Versioning — see SPEC.md §2. Bump on EVERY code change.
val appVersionCode = 7
val appVersionName = "1.3.1"

// Highest versionCode ever uploaded to Play Console (kept in play-upload.properties).
// Release builds fail if appVersionCode isn't higher, because Play rejects reused codes.
val lastUploadedVersionCode = rootProject.file("play-upload.properties").let { f ->
    Properties().apply { if (f.exists()) f.inputStream().use { load(it) } }
        .getProperty("lastUploadedVersionCode")?.toIntOrNull() ?: 0
}
tasks.configureEach {
    if (name == "bundleRelease" || name == "assembleRelease") {
        val code = appVersionCode
        val last = lastUploadedVersionCode
        doFirst {
            check(code > last) {
                "versionCode $code must be higher than the last code uploaded to Play ($last). " +
                    "Bump appVersionCode in app/build.gradle.kts (see SPEC.md §2)."
            }
        }
    }
}

android {
    namespace = "com.satya.uno"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.satya.uno"
        minSdk = 24
        targetSdk = 37
        versionCode = appVersionCode
        versionName = appVersionName
    }

    // Upload key for Play Console — values live in keystore.properties (git-ignored, never committed).
    val keystoreFile = rootProject.file("keystore.properties")
    val keystore = Properties().apply { if (keystoreFile.exists()) keystoreFile.inputStream().use { load(it) } }
    signingConfigs {
        if (keystoreFile.exists()) {
            create("upload") {
                storeFile = rootProject.file(keystore.getProperty("storeFile"))
                storePassword = keystore.getProperty("storePassword")
                keyAlias = keystore.getProperty("keyAlias")
                keyPassword = keystore.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            if (keystoreFile.exists()) signingConfig = signingConfigs.getByName("upload")
            optimization {
                enable = true
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.kotlinx.coroutines.play.services)

    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    testImplementation(libs.junit)
    testImplementation(libs.org.json)
}
