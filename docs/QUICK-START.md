# Quick Start Guide

## Contents

1. [Create a NavHostFragment](#create-a-navhostfragment)
1. [Create an Activity](#create-an-activity)
1. [Create a Web Fragment](#create-a-web-fragment)
1. [Create a Path Configuration](#create-a-path-configuration)

## Create a NavHostFragment

A [`NavHostFragment`](https://developer.android.com/reference/androidx/navigation/fragment/NavHostFragment) is a component available in [Android Jetpack](https://developer.android.com/jetpack) and is primarily responsible for providing "an area in your layout for self-contained navigation to occur."

The Hotwire extension of this class, `SessionNavHostFragment`, along with being responsible for self-contained `HotwireFragment` navigation, also manages a `Sesssion` and a `TurboWebView` instance. You will need to implement a couple things for this abstract class:

- The name of the `Session` (this is arbitrary, but must be unique in your app)
- The url of a starting location when your app starts up. Note: if you're running your app locally without HTTPS, you'll need to adjust your `android:usesCleartextTraffic` settings in the `debug/AndroidManifest.xml` (or use an Android Network security configuration), and target [`10.0.2.2` instead of `localhost`](https://developer.android.com/studio/run/emulator-networking) when using an emulator.

In its simplest form, the implementation of your `SessionNavHostFragment` will look like:

**`MainSessionNavHostFragment`:**
```kotlin
import dev.hotwire.core.turbo.session.SessionNavHostFragment

class MainSessionNavHostFragment : SessionNavHostFragment() {
    override val sessionName = "main"
    override val startLocation = "https://turbo-native-demo.glitch.me/"
}
```

Refer to the demo [`MainSessionNavHostFragment`](../demo/src/main/kotlin/dev/hotwire/demo/main/MainSessionNavHostFragment.kt) for an example.

## Create an Activity

It's strongly recommended to use a single-Activity architecture in your app. Generally, you'll have one `HotwireActivity` and many `HotwireFragment` instances.

### Create the HotwireActivity layout resource

You need to create a layout resource file that your `HotwireActivity` will use to host the `SessionNavHostFragment` that you created above.

Android Jetpack provides a [`FragmentContainerView`](https://developer.android.com/reference/androidx/fragment/app/FragmentContainerView) to contain `NavHostFragment` navigation. In its simplest form, your Activity layout file will look like:

**`res/layout/activity_main.xml`:**
```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <androidx.fragment.app.FragmentContainerView
        android:id="@+id/main_nav_host"
        android:name="dev.hotwire.demo.main.MainSessionNavHostFragment"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:defaultNavHost="false" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

Refer to the demo [`activity_main.xml`](../demo/src/main/res/layout/activity_main.xml) for an example.

### Create the HotwireActivity class

A Hotwire Activity is straightforward and needs to implement the [`HotwireActivity`](../core/src/main/kotlin/dev/hotwire/core/turbo/activities/HotwireActivity.kt) interface in order to provide a [`HotwireActivityDelegate`](../core/src/main/kotlin/dev/hotwire/core/turbo/delegates/HotwireActivityDelegate.kt).

Your Activity should extend Android Jetpack's [`AppCompatActivity`](https://developer.android.com/reference/androidx/appcompat/app/AppCompatActivity). In its simplest form, your Activity will look like:

**`MainActivity.kt`:**
```kotlin
class MainActivity : AppCompatActivity(), HotwireActivity {
    override lateinit var delegate: HotwireActivityDelegate

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        delegate = HotwireActivityDelegate(this, R.id.main_nav_host)
    }
}
```

_Note that `R.layout.activity_main` refers to the Activity layout file that you already created. `R.id.main_nav_host` refers to the `MainSessionNavHostFragment` that you created, hosted in the layout file._

Refer to the demo [`MainActivity`](../demo/src/main/kotlin/dev/hotwire/demo/main/MainActivity.kt) as an example. (Don't forget to add your Activity to your app's [`AndroidManifest.xml`](../demo/src/main/AndroidManifest.xml) file.)

## Configure your App

At a minimum, you'll want to set a handful configuration options before your `HotwireActivity` instance is created by the system. It's recommended to create your own `Application` instance and place the configuration code there. The configuration op 

**`DemoApplication.kt`:**
```kotlin
class DemoApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Configure debugging
        Hotwire.config.debugLoggingEnabled = BuildConfig.DEBUG
        Hotwire.config.webViewDebuggingEnabled = BuildConfig.DEBUG

        // Set the base url where your web app exists 
        Hotwire.appUrl = "https://turbo-native-demo.glitch.me/"

        // Set the user agent for every WebView request that is made. There's a
        // `userAgentSubstring()` helper available that you should include as 
        // part of your user agent so the app is properly identified as a Hotwire
        // Native app on your server.
        Hotwire.config.userAgent = "Demo App; ${Hotwire.config.userAgentSubstring()}"
    }
}
```

Refer to the demo [`DemoApplication`](../demo/src/main/kotlin/dev/hotwire/demo/DemoApplication.kt) as an example. (Don't forget to reference your `Application` instance in the app's [`AndroidManifest.xml`](../demo/src/main/AndroidManifest.xml) file.)

See the documentation to learn more about [configuring your app](CONFIGURE-APP.md).

## Create a Path Configuration

See the documentation to learn about setting up your [path configuration](PATH-CONFIGURATION.md)

## Navigation

See the documentation to learn about [navigating between destinations](NAVIGATION.md).

## Advanced Options

See the documentation to [learn about the advanced options available](ADVANCED-OPTIONS.md).
