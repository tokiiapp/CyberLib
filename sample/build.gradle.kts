plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("com.google.gms.google-services")
}

val packageName = "com.cyber.sample"

android {
    namespace = packageName
    compileSdk = 35

    defaultConfig {
        applicationId = packageName
        minSdk = 27
        targetSdk = 35
        versionCode = 100
        versionName = "1.0.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(project(":library"))

    implementation(libs.play.services.ads)
    implementation(libs.androidx.lifecycle.runtime.ktx)
}

apply("script.gradle.kts")
android.sourceSets.getByName("main").kotlin.srcDir("build/generated/source/remoteConfig/")
