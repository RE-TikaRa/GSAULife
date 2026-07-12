plugins {
    id("com.android.library")
}

android {
    namespace = "com.tika.gsaulife.academic"
    resourcePrefix = "academic_"
    compileSdk {
        version = release(37) {
            minorApiLevel = 0
        }
    }

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        viewBinding = true
    }

    lint {
        disable += setOf(
            "UnsafeOptInUsageError",
            "UnsafeOptInUsageWarning",
        )
    }
}

dependencies {
    api(libs.androidx.fragment)

    implementation(libs.androidx.activity)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.gridlayout)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.coroutines.android)
    implementation(libs.jsoup)
    implementation(libs.material)
    implementation(libs.okhttp)

    testImplementation(libs.test.json)
    testImplementation(libs.test.junit)
}
