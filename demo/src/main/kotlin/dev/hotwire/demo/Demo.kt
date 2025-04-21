package dev.hotwire.demo

object Demo {
    // Update this to choose which demo is run
    val current: Environment = Environment.Remote
}

enum class Environment(val url: String) {
    Remote("https://hotwire-native-demo.dev"),
    Local("http://192.168.1.1:3000")
}
