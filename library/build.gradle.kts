import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("maven-publish")
}

val libVersion = "1.2.9"
android {
    namespace = "com.cyber.ads"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
        externalNativeBuild {
            cmake {
                cppFlags += ""
            }
        }

        ndk {
            //noinspection ChromeOsAbiSupport
            abiFilters += listOf("armeabi-v7a", "arm64-v8a")
        }
    }

    externalNativeBuild {
        cmake {
            // relative to module root
            path = file("src/main/cpp/CMakeLists.txt")
        }
    }

    buildTypes {
        debug {
            externalNativeBuild {
                cmake {
                    arguments += "-DDEBUG=1"
                }
            }
        }
        release {
            externalNativeBuild {
                cmake {
                    arguments += "-DDEBUG=0"
                }
            }

            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    buildFeatures {
        viewBinding = true
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
    implementation(libs.app.update.ktx)


    //Adjust
    implementation(libs.adjust.android.v520)
    implementation(libs.installreferrer)
    implementation(libs.play.services.ads.identifier)
    implementation(libs.adjust.android.webbridge.v520)

    implementation(libs.firebase.config.ktx)
    implementation("com.google.firebase:firebase-analytics:23.0.0")
    api("com.facebook.android:facebook-android-sdk:18.1.3")
    //iap
    implementation("com.android.billingclient:billing-ktx:8.0.0")
    //solar
    implementation("com.reyun.solar.engine.oversea:solar-engine-core:1.3.1.1")

    // appsflyer
    implementation("com.appsflyer:af-android-sdk:6.14.0")
    implementation("com.appsflyer:adrevenue:6.9.1")
}
val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(android.sourceSets["main"].java.srcDirs)  // Đã bao gồm cả kotlin folder
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}


afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])

                // Định danh thư viện
                groupId = "cyber.soft.global" // Bạn có thể đặt tuỳ ý, ví dụ com.github.tokiiapp
                artifactId = "ads"
                version = libVersion
            }
        }

        repositories {
            maven {
                name = "GitHubPackages"
                // Đường dẫn đến Repo Github của bạn
                url = uri("https://maven.pkg.github.com/tokiiapp/CyberLib")

                credentials {
                    // Code đọc user/pass từ local.properties
                    val props = Properties()
                    val localPropsFile = project.rootProject.file("local.properties")
                    if (localPropsFile.exists()) {
                        props.load(FileInputStream(localPropsFile))
                    }
                    username = props.getProperty("gpr.user") ?: System.getenv("GITHUB_ACTOR")
                    password = props.getProperty("gpr.key") ?: System.getenv("GITHUB_TOKEN")
                }
            }
        }
    }
}
