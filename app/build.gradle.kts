plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.kiranacalculator"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.kiranacalculator"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    // Release signing is read from Gradle properties, so no key material lives in git:
    //   ./gradlew assembleRelease -PKIRANA_STORE_FILE=../kirana-release.keystore \
    //       -PKIRANA_STORE_PASSWORD=*** -PKIRANA_KEY_ALIAS=kirana -PKIRANA_KEY_PASSWORD=***
    // Without those properties the release variant falls back to the debug key, which is
    // fine for local testing but must not be uploaded anywhere.
    signingConfigs {
        create("release") {
            val storePath = project.findProperty("KIRANA_STORE_FILE") as String?
            if (storePath != null) {
                storeFile = file(storePath)
                storePassword = project.findProperty("KIRANA_STORE_PASSWORD") as String?
                keyAlias = project.findProperty("KIRANA_KEY_ALIAS") as String?
                keyPassword = project.findProperty("KIRANA_KEY_PASSWORD") as String?
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = if (project.hasProperty("KIRANA_STORE_FILE")) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
    // Intentionally empty: the app uses only the Android framework,
    // so it builds offline and the APK stays tiny.
}
