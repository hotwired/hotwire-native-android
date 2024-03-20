plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "dev.hotwire.demo"
    compileSdk = 34

    defaultConfig {
        applicationId = "dev.hotwire.demo"
        minSdk = 28
        targetSdk = 34
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
    // Material
    implementation("com.google.android.material:material:1.11.0")

    // AndroidX
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // JSON
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")

    // Images
    implementation("com.github.bumptech.glide:glide:4.15.1")

    implementation(project(":core"))
}