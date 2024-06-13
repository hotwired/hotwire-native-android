package dev.hotwire.core.turbo.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

internal data class DispatcherProvider(
    val main: CoroutineDispatcher,
    var io: CoroutineDispatcher
)

internal val dispatcherProvider = DispatcherProvider(
    main = Dispatchers.Main,
    io = Dispatchers.IO
)
