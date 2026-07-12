plugins {
    id("com.android.application")
}

val appVersionName = System.getenv("VERSION_NAME") ?: "1.0.0"
val appVersionCode = appVersionName.split('.').map(String::toInt).let { (major, minor, patch) ->
    major * 1_000_000 + minor * 1_000 + patch
}

android {
    namespace = "com.tika.gsaulife"
    compileSdk {
        version = release(37) {
            minorApiLevel = 0
        }
    }

    defaultConfig {
        applicationId = "com.tika.gsaulife"
        minSdk = 24
        targetSdk = 37
        versionCode = System.getenv("VERSION_CODE")?.toInt() ?: appVersionCode
        versionName = appVersionName
    }

    val storeFilePath = System.getenv("KEYSTORE_FILE")
    signingConfigs {
        if (storeFilePath != null) {
            create("release") {
                storeFile = file(storeFilePath)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (storeFilePath != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(project(":feature-card"))
    implementation(project(":feature-academic"))

    implementation(libs.androidx.activity)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.coroutines.android)
    implementation(libs.material)
    implementation(libs.okhttp)

    testImplementation(libs.test.json)
    testImplementation(libs.test.junit)
}
