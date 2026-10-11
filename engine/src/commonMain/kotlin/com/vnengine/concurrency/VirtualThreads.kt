package com.vnengine.concurrency

import kotlinx.coroutines.CoroutineDispatcher

expect fun createVirtualThreadDispatcher(): CoroutineDispatcher
