# Quick Start Guide

## Contents

1. [Introduction to NavigatorHosts](#introduction-to-navhostfragments)
1. [Create an Activity](#create-an-activity)
1. [Configure your app](#configure-your-app)
1. [Create a Path Configuration](#create-a-path-configuration)
1. [Navigation](#navigation)
1. [Advanced Options](#advanced-options)
2. [Local Development](#local-development)

## Introduction to NavigatorHosts

To start, a [`NavHostFragment`](https://developer.android.com/reference/androidx/navigation/fragment/NavHostFragment) is a component available in [Android Jetpack](https://developer.android.com/jetpack) and is primarily responsible for providing "an area in your layout for self-contained navigation to occur."

The Hotwire implementation of this class, `NavigatorHost`, along with being responsible for self-contained `HotwireFragment` navigation, also manages a `Navigator` with a `Sesssion` and `TurboWebView` instance.

## Create an Activity

It's strongly recommended to use a single-Activity architecture in your app. Generally, you'll have one `HotwireActivity` and many `HotwireFragment` instances.

### Create the HotwireActivity layout resource

You need to create a layout resource file that your `HotwireActivity` will use to host the `NavigatorHost`.

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
        android:name="dev.hotwire.core.navigation.session.NavigatorHost"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:defaultNavHost="false" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

Refer to the demo [`activity_main.xml`](../demo/src/main/res/layout/activity_main.xml) for an example.

### Implement the HotwireActivity abstract class

A Hotwire `Activity` is straightforward and needs to subclass the [`HotwireActivity`](../core/src/main/kotlin/dev/hotwire/core/turbo/activities/HotwireActivity.kt) class in order to provide `Navigator` configuration information.

`HotwireActivity` extends Android Jetpack's [`AppCompatActivity`](https://developer.android.com/reference/androidx/appcompat/app/AppCompatActivity). 

You'll need to provide at least one `NavigatorConfiguration` instance (one for each `NavigatorHost` that exists in our Activity layout). This includes:
- The `name` of the `Navigator` (this is arbitrary and helpful for debugging purposes, but each must be unique in your app)
- The `startLocation` url when your app starts up. Note: if you're running your app locally without HTTPS, see the [local development](#local-development) section.
- The `navigatorHostId`, which refers to the resource ID of the `NavigatorHost` in your Activity layout.

In its simplest form, your Activity will look like:

**`MainActivity.kt`:**
```kotlin
class MainActivity : HotwireActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun navigatorConfigurations() = listOf(
        NavigatorConfiguration(
            name = "main",
            startLocation = Urls.homeUrl,
            navigatorHostId = R.id.main_nav_host
        )
    )
}
```

_Note that `R.layout.activity_main` refers to the Activity layout file that you already created. `R.id.main_nav_host` refers to the `NavigatorHost` that placed in the layout file._

Refer to the demo [`MainActivity`](../demo/src/main/kotlin/dev/hotwire/demo/main/MainActivity.kt) as an example. (Don't forget to add your Activity to your app's [`AndroidManifest.xml`](../demo/src/main/AndroidManifest.xml) file.)

## Configure your App

See the documentation to learn more about [configuring your app](CONFIGURE-APP.md).

## Create a Path Configuration

See the documentation to learn about setting up your [path configuration](PATH-CONFIGURATION.md)

## Navigation

See the documentation to learn about [navigating between destinations](NAVIGATION.md).

## Advanced Options

See the documentation to [learn about the advanced options available](ADVANCED-OPTIONS.md).

## Local Development

If you're running your web app locally without HTTPS, you'll need to adjust the `android:usesCleartextTraffic` setting in the `AndroidManifest.xml` file (or use an Android Network security configuration). It's highly recommended to only allow `http` traffic in the `debug` manifest file, which only applies the setting to `debug` builds, not `release` builds. It'll look like this:

**`src/debug/AndroidManifest.xml`:**
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest
    package="dev.hotwire.demo"
    xmlns:android="http://schemas.android.com/apk/res/android">

    <application
        android:usesCleartextTraffic="true">
    </application>

</manifest>
```

Refer to the demo [`debug/AndroidManifest.xml`](../demo/src/debug/AndroidManifest.xml) as an example.

If you're using an emulator, target [`10.0.2.2` instead of `localhost`](https://developer.android.com/studio/run/emulator-networking) as the loopback interface.
