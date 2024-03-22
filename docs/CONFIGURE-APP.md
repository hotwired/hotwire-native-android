# Configure your App

## Create an Application instance
To customize your app you'll want to set a handful of configuration options before your `HotwireActivity` instance is created by the system. It's recommended to create your own `Application` instance and place the configuration code there.

**`DemoApplication.kt`:**
```kotlin
class DemoApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Set configuration options
    }
}
```

Refer to the demo [`DemoApplication`](../demo/src/main/kotlin/dev/hotwire/demo/DemoApplication.kt) as an example. Don't forget to reference the name of your `Application` instance in the app's [`AndroidManifest.xml`](../demo/src/main/AndroidManifest.xml) file, otherwise it won't be invoked on app startup.

