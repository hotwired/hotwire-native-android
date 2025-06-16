import com.vanniktech.maven.publish.SonatypeHost

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.vanniktech.maven.publish")
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

android {
    namespace = "dev.hotwire.navigation"
    compileSdk = 35

    testOptions.unitTests.isIncludeAndroidResources = true
    testOptions.unitTests.isReturnDefaultValues = true
    testOptions.targetSdk = 35

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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
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
    implementation("org.jetbrains.kotlin:kotlin-reflect:2.1.20")

    // AndroidX
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.lifecycle:lifecycle-common:2.8.7")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // Material
    implementation("com.google.android.material:material:1.12.0")

    // Browser
    implementation("androidx.browser:browser:1.8.0")

    // Exported AndroidX dependencies
    api("androidx.activity:activity-ktx:1.10.1")
    api("androidx.fragment:fragment-ktx:1.8.6")
    api("androidx.navigation:navigation-fragment-ktx:2.8.9")
    api("androidx.navigation:navigation-ui-ktx:2.8.9")

    // Tests
    testImplementation("androidx.test:core:1.6.1") // Robolectric
    testImplementation("org.assertj:assertj-core:3.26.3")
    testImplementation("androidx.navigation:navigation-testing:2.8.9")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("org.mockito:mockito-core:5.14.2")
    testImplementation("com.nhaarman:mockito-kotlin:1.6.0")
    testImplementation("junit:junit:4.13.2")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

// Publish to GitHub Packages via:
//   ./gradlew -Pversion=<version> clean build publishAllPublicationsToGithubPackagesRepository
//   expected env variables: https://vanniktech.github.io/gradle-maven-publish-plugin/other/#github-packages-example
//   https://github.com/orgs/hotwired/packages?repo_name=hotwire-native-android
// Publish to Maven Central via:
//   ./gradlew -Pversion=<version> clean build publishAndReleaseToMavenCentral --no-configuration-cache
//   expected env variables: https://vanniktech.github.io/gradle-maven-publish-plugin/central/#secrets
//   https://central.sonatype.com/artifact/dev.hotwire/navigation-fragments

mavenPublishing {
    coordinates(groupId = publishedGroupId, artifactId = publishedArtifactId, version = libVersionName)

    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)

    signAllPublications()

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
}

publishing {
    repositories {
        maven {
            name = "githubPackages"
            url = uri("https://maven.pkg.github.com/hotwired/hotwire-native-android")
            // username and password (a personal Github access token) should be specified as
            // `ORG_GRADLE_PROJECT_githubPackagesUsername` and `ORG_GRADLE_PROJECT_githubPackagesPassword`
            // environment variables
            credentials(PasswordCredentials::class)
        }
    }
}

