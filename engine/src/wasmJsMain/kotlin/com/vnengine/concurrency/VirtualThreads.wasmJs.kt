package com.vnengine.concurrency

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

actual fun createVirtualThreadDispatcher(): CoroutineDispatcher = Dispatchers.Default
