plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.android5.internetbrowser"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.android5.internetbrowser"
        minSdk = 23
        targetSdk = 36
        versionCode = 126
        versionName = "2.0-pr-android15 "

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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.material.icons.extended)
    implementation(libs.guava)
    implementation(libs.markwon.core)
    implementation(libs.markwon.ext.tables)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}