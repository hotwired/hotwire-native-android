package dev.hotwire.core.logging

enum class HotwireLogLevel(val priority: Int) {
    VERBOSE(0),
    DEBUG(1),
    INFO(2),
    WARN(3),
    ERROR(4),
    NONE(5)
}
