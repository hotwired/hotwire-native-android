plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
    id("signing")
}

val libVersionName by extra(version as String)
val libraryName by extra("Hotwire Native for Android - Fragment Navigation")
val libraryDescription by extra("Android framework for making Hotwire Native apps")

val publishedGroupId by extra("dev.hotwire")
val publishedArtifactId by extra("navigation-fragments")

val siteUrl by extra("https://github.com/hotwired/hotwire-native-android")
val gitUrl by extra("https://github.com/hotwired/hotwire-native-android.git")

val licenseType by extra("MIT License")
val licenseUrl by extra("https://github.com/hotwired/hotwire-native-android/blob/main/LICENSE")

val developerId by extra("basecamp")
val developerEmail by extra("androidteam@basecamp.com")

val isSonatypeRelease by extra(project.hasProperty("sonatype"))

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

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
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

    // Exported AndroidX dependencies
    api("androidx.activity:activity-ktx:1.9.0")
    api("androidx.fragment:fragment-ktx:1.7.1")
    api("androidx.navigation:navigation-fragment-ktx:2.7.7")
    api("androidx.navigation:navigation-ui-ktx:2.7.7")

    // Tests
    testImplementation("androidx.test:core:1.5.0") // Robolectric
    testImplementation("org.assertj:assertj-core:3.25.3")
    testImplementation("androidx.navigation:navigation-testing:2.7.7")
    testImplementation("org.robolectric:robolectric:4.12.1")
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("com.nhaarman:mockito-kotlin:1.6.0")
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
//   https://search.maven.org/artifact/dev.hotwire/navigation-fragments
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

