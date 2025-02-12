plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("maven-publish")
    id("signing")
}

val libVersionName by extra(version as String)
val libraryName by extra("Hotwire Native for Android - Core")
val libraryDescription by extra("Android framework for making Hotwire Native apps")

val publishedGroupId by extra("dev.hotwire")
val publishedArtifactId by extra("core")

val siteUrl by extra("https://github.com/hotwired/hotwire-native-android")
val gitUrl by extra("https://github.com/hotwired/hotwire-native-android.git")

val licenseType by extra("MIT License")
val licenseUrl by extra("https://github.com/hotwired/hotwire-native-android/blob/main/LICENSE")

val developerId by extra("basecamp")
val developerEmail by extra("androidteam@basecamp.com")

val isSonatypeRelease by extra(project.hasProperty("sonatype"))

android {
    namespace = "dev.hotwire.core"
    compileSdk = 35

    testOptions.unitTests.isIncludeAndroidResources = true
    testOptions.unitTests.isReturnDefaultValues = true
    testOptions.targetSdk = 35

    defaultConfig {
        minSdk = 28
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

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect:2.1.0")

    // Material
    implementation("com.google.android.material:material:1.12.0")

    // AndroidX
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    implementation("androidx.lifecycle:lifecycle-common:2.8.7")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // JSON
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // Networking/API
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")

    // Exported AndroidX dependencies
    api("androidx.appcompat:appcompat:1.7.0")
    api("androidx.core:core-ktx:1.15.0")
    api("androidx.webkit:webkit:1.12.1")

    // Tests
    testImplementation("androidx.test:core:1.6.1") // Robolectric
    testImplementation("androidx.navigation:navigation-testing:2.8.6")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("org.assertj:assertj-core:3.26.3")
    testImplementation("org.robolectric:robolectric:4.13")
    testImplementation("org.mockito:mockito-core:5.14.2")
    testImplementation("com.nhaarman:mockito-kotlin:1.6.0")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("junit:junit:4.13.2")
}

tasks {
    // Only sign Sonatype release artifacts
    withType<Sign>().configureEach {
        onlyIf { isSonatypeRelease }
    }
}

// Sign Sonatype published release artifacts
if (isSonatypeRelease) {
    signing {
        val keyId = System.getenv("GPG_KEY_ID")
        val secretKey = System.getenv("GPG_SECRET_KEY")
        val password = System.getenv("GPG_PASSWORD")

        useInMemoryPgpKeys(keyId, secretKey, password)

        setRequired({ gradle.taskGraph.hasTask("publish") })
        sign(publishing.publications)
    }
}

// Publish to GitHub Packages via:
//   ./gradlew -Pversion=<version> clean build publish
//   https://github.com/orgs/hotwired/packages?repo_name=hotwire-native-android
// Publish to Maven Central via:
//   ./gradlew -Psonatype -Pversion=<version> clean build publish
//   https://search.maven.org/artifact/dev.hotwire/core
publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = publishedGroupId
            artifactId = publishedArtifactId
            version = libVersionName

            pom {
                name.set(libraryName)
                description.set(libraryDescription)
                url.set(siteUrl)

                licenses {
                    license {
                        name.set(licenseType)
                        url.set(licenseUrl)
                    }
                }
                developers {
                    developer {
                        id.set(developerId)
                        name.set(developerId)
                        email.set(developerEmail)
                    }
                }
                scm {
                    url.set(gitUrl)
                }
            }

            // Applies the component for the release build variant
            afterEvaluate {
                from(components["release"])
            }
        }
    }
    repositories {
        if (isSonatypeRelease) {
            maven {
                url = uri("https://s01.oss.sonatype.org/content/repositories/releases/")

                credentials {
                    username = System.getenv("SONATYPE_USER")
                    password = System.getenv("SONATYPE_PASSWORD")
                }
            }
        } else {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/hotwired/hotwire-native-android")

                credentials {
                    username = System.getenv("GITHUB_ACTOR")
                    password = System.getenv("GITHUB_TOKEN")
                }
            }
        }
    }
}
