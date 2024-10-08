package dev.hotwire.demo

object Urls {
    // Update to switch between the demo server and local development
    private const val useLocalDev = false

    // Update with your local dev IP address
    private const val localDevUrl = "http://192.168.1.1:45678"

    // Remote demo server
    private const val remoteUrl = "https://hotwire-native-demo.dev"

    // Base app url
    private val appUrl = if (useLocalDev) localDevUrl else remoteUrl

    val homeUrl = appUrl
    val signInUrl = "$appUrl/signin"
    val numbersUrl = "$appUrl/numbers"
}
