plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "dev.hotwire.demo"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.hotwire.demo"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        getByName("debug") {
            isDebuggable = true
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

    sourceSets {
        named("main")  { java { srcDirs("src/main/kotlin") } }
        named("test")  { java { srcDirs("src/test/kotlin") } }
        named("debug") { java { srcDirs("src/debug/kotlin") } }
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":navigation-fragments"))

    // Material
    implementation("com.google.android.material:material:1.12.0")

    // AndroidX
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    implementation("androidx.recyclerview:recyclerview:1.4.0")

    // JSON
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // Images
    implementation("io.coil-kt:coil:2.7.0")

}