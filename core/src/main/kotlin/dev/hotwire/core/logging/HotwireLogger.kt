package dev.hotwire.core.logging

interface HotwireLogger {
    var logLevel: HotwireLogLevel
    fun v(tag: String, msg: () -> String)
    fun d(tag: String, msg: () -> String)
    fun i(tag: String, msg: () -> String)
    fun w(tag: String, msg: () -> String)
    fun e(tag: String, throwable: Throwable?, msg: () -> String)
}