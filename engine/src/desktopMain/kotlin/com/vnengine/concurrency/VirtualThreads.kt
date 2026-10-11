package com.vnengine.concurrency

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

actual fun createVirtualThreadDispatcher(): CoroutineDispatcher {
    return Executors.newVirtualThreadPerTaskExecutor().asCoroutineDispatcher()
}
