plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.zafar.ichatai"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.zafar.ichatai"
        minSdk = 21
        targetSdk = 34
        versionCode = 2
        versionName = "1.0.1"

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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // OkHttp for HTTP calls to OpenRouter
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Firebase BoM and Analytics
    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))
    implementation("com.google.firebase:firebase-analytics")

    // Google Mobile Ads (AdMob)
    implementation("com.google.android.gms:play-services-ads:23.1.0")
}