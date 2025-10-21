plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("maven-publish")
}

val libVersion = "1.5.3.3"
android {
    namespace = "com.cyber.ads"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
    }

    buildTypes {
        release {
            isMinifyEnabled = true
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
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.process)

    // UI
    implementation(libs.androidx.appcompat)
    implementation(libs.materialish.progress)
    implementation(libs.shimmer)

    // Ads
    implementation(libs.play.services.ads)
    implementation(libs.sdp.android)

    // Other
    implementation(libs.gson)
    implementation(libs.lottie.v640)
    implementation(libs.user.messaging.platform)

    //Adjust
    implementation(libs.adjust.android.v520)
    implementation(libs.installreferrer)
    implementation(libs.play.services.ads.identifier)
    implementation(libs.adjust.android.webbridge.v520)

    implementation(libs.firebase.config.ktx)
    implementation("com.google.firebase:firebase-analytics:23.0.0")
    api("com.facebook.android:facebook-android-sdk:18.1.3")
    api("com.google.ads.mediation:applovin:13.4.0.0")
    api("com.google.ads.mediation:facebook:6.20.0.1")
    //iap
    implementation("com.android.billingclient:billing-ktx:8.0.0")
}

