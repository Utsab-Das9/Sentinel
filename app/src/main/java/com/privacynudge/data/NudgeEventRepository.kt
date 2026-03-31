package com.privacynudge.data

/**
 * In-memory repository for nudge events.
 * In a production app, this would use Room database.
 */
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton repository for nudge events.
 * Uses StateFlow for real-time observation.
 */
class NudgeEventRepository private constructor() {
    private val _events = MutableStateFlow<List<NudgeEvent>>(emptyList())
    val events: StateFlow<List<NudgeEvent>> = _events.asStateFlow()

    fun addEvent(event: NudgeEvent) {
        val currentList = _events.value.toMutableList()
        currentList.add(0, event) // Add to front for newest first
        
        // Keep only last 100 events
        if (currentList.size > 100) {
            currentList.removeAt(currentList.lastIndex)
        }
        _events.value = currentList
    }

    fun clearEvents() {
        _events.value = emptyList()
    }

    companion object {
        @Volatile
        private var instance: NudgeEventRepository? = null

        fun getInstance(): NudgeEventRepository {
            return instance ?: synchronized(this) {
                instance ?: NudgeEventRepository().also { instance = it }
            }
        }
    }
}
