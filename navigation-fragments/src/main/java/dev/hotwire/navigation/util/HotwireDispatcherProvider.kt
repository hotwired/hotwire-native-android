package dev.hotwire.navigation.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

internal data class HotwireDispatcherProvider(
    val main: CoroutineDispatcher,
    var io: CoroutineDispatcher
)

internal val dispatcherProvider = HotwireDispatcherProvider(
    main = Dispatchers.Main,
    io = Dispatchers.IO
)
