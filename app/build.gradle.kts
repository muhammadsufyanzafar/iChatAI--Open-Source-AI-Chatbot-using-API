plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics") version "3.0.6" apply false
}

android {
    namespace = "com.zafar.ichatai"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.zafar.ichatai"
        minSdk = 21
        targetSdk = 34
        versionCode = 2
        versionName = "1.0.2"

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
        isCoreLibraryDesugaringEnabled = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    // If recyclerview is not in libs already, keep this explicit line:
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.drawerlayout:drawerlayout:1.2.0")

    // OkHttp for HTTP calls to OpenRouter
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Firebase BoM and Analytics
    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-crashlytics-ndk")

    // Google Mobile Ads
    implementation("com.google.android.gms:play-services-ads:23.1.0")

    // Room (Java)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation(libs.monitor)
    implementation(libs.androidx.junit)
    testImplementation("junit:junit:4.12")
    androidTestImplementation("junit:junit:4.12")
    annotationProcessor("androidx.room:room-compiler:2.6.1")

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
}