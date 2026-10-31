package com.gotuner.tuner.presentation

import kotlinx.coroutines.Job

/** Owns the native UI's StateFlow observation; cancel when the UI adapter is disposed. */
class StateSubscription(private val job: Job) {
    fun cancel() = job.cancel()
}
