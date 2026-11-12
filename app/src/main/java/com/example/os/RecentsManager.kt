package com.example.os

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Manages the stack of running recent apps in memory.
 */
class RecentsManager {
    private val _runningApps = MutableStateFlow<List<String>>(emptyList())
    val runningApps: StateFlow<List<String>> = _runningApps.asStateFlow()

    fun openApp(appId: String) {
        _runningApps.update { current ->
            val list = current.filter { it != appId }.toMutableList()
            list.add(0, appId) // Most recent at front
            list
        }
    }

    fun closeApp(appId: String) {
        _runningApps.update { current ->
            current.filter { it != appId }
        }
    }

    fun clearAll() {
        _runningApps.value = emptyList()
    }
}
