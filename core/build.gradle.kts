plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "dev.hotwire.core"
    compileSdk = 34
    testOptions.unitTests.isIncludeAndroidResources = true

    defaultConfig {
        minSdk = 28
        targetSdk = 34
    }

    buildTypes {
        release {
            isMinifyEnabled = true
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
    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.9.10")

    // Material
    implementation("com.google.android.material:material:1.11.0")

    // AndroidX
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.lifecycle:lifecycle-extensions:2.2.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // JSON
    implementation("com.google.code.gson:gson:2.10.1")

    // Networking/API
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // Exported AndroidX dependencies
    api("androidx.appcompat:appcompat:1.6.1")
    api("androidx.core:core-ktx:1.12.0")
    api("androidx.webkit:webkit:1.8.0")
    api("androidx.activity:activity-ktx:1.8.1")
    api("androidx.fragment:fragment-ktx:1.6.2")
    api("androidx.navigation:navigation-fragment-ktx:2.7.5")
    api("androidx.navigation:navigation-ui-ktx:2.7.5")

    // Tests
    testImplementation("androidx.test:core:1.5.0") // Robolectric
    testImplementation("androidx.navigation:navigation-testing:2.7.5")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("org.assertj:assertj-core:3.24.2")
    testImplementation("org.robolectric:robolectric:4.11.1")
    testImplementation("org.mockito:mockito-core:5.2.0")
    testImplementation("com.nhaarman:mockito-kotlin:1.6.0")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.11.0")
    testImplementation("junit:junit:4.13.2")
}