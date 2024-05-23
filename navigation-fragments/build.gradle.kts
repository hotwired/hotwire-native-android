plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "dev.hotwire.navigation"
    compileSdk = 34

    testOptions.unitTests.isIncludeAndroidResources = true
    testOptions.unitTests.isReturnDefaultValues = true
    testOptions.targetSdk = 34

    defaultConfig {
        minSdk = 28

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            consumerProguardFiles("proguard-consumer-rules.pro")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        buildConfig = true
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

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.9.23")

    // AndroidX
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.lifecycle:lifecycle-common:2.8.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // Material
    implementation("com.google.android.material:material:1.12.0")

    // Browser
    implementation("androidx.browser:browser:1.8.0")

    // Tests
    testImplementation("androidx.test:core:1.5.0") // Robolectric
    testImplementation("org.assertj:assertj-core:3.24.2")
    testImplementation("androidx.navigation:navigation-testing:2.7.5")
    testImplementation("org.robolectric:robolectric:4.11.1")
    testImplementation("org.mockito:mockito-core:5.2.0")
    testImplementation("com.nhaarman:mockito-kotlin:1.6.0")
    testImplementation("junit:junit:4.13.2")
}

// TODO add publishing support
