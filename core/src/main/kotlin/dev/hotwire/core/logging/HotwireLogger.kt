package dev.hotwire.core.logging

interface HotwireLogger {
    fun d(tag: String, msg: String)
    fun w(tag: String, msg: String)
    fun e(tag: String, msg: String)
}