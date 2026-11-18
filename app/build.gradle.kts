plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.visibleinfotech.e_ethanolUnloading"

    compileSdk = 36

    defaultConfig {
        applicationId = "com.visibleinfotech.e_ethanolUnloading"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        multiDexEnabled = true

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false

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

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {

    implementation(fileTree("libs") {
        include("*.jar", "*.aar")
    })

    implementation(files("libs/DeviceAPI_ver20220518_release.aar"))

    implementation("androidx.multidex:multidex:2.0.1")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.foundation)

    implementation(libs.androidx.appcompat)

    implementation("androidx.navigation:navigation-compose:2.7.7")

    implementation("androidx.compose.material:material-icons-extended:1.6.0")

    implementation("androidx.compose.material3:material3:1.1.1")

    implementation("io.coil-kt:coil-compose:2.5.0")


    implementation("com.maxkeppeler.sheets-compose-dialogs:calendar:1.3.0")
    implementation("com.maxkeppeler.sheets-compose-dialogs:core:1.3.0")
    implementation("androidx.compose.runtime:runtime-livedata:1.7.5")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("com.google.zxing:core:3.5.3")
    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.junit)

    androidTestImplementation(libs.androidx.espresso.core)

    androidTestImplementation(platform(libs.androidx.compose.bom))

    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    debugImplementation(libs.androidx.compose.ui.tooling)

    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
