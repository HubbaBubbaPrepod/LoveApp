package com.example.loveapp.sync

import android.util.Log
import com.example.loveapp.data.repository.ActivityRepository
import com.example.loveapp.data.repository.ArtRepository
import com.example.loveapp.data.repository.CalendarRepository
import com.example.loveapp.data.repository.CycleRepository
import com.example.loveapp.data.repository.MoodRepository
import com.example.loveapp.data.repository.NoteRepository
import com.example.loveapp.data.repository.RelationshipRepository
import com.example.loveapp.data.repository.WishRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Refreshes all Room caches from the server in parallel.
 *
 * Called once from [SyncManager.init] after login so that every screen already
 * has data the moment it opens — no per-screen network wait.
 *
 * Each repo's [refreshFromServer] is fire-and-forget internally:
 * failures are swallowed so one failing endpoint never blocks the others.
 */
@Singleton
class DataPreloadManager @Inject constructor(
    private val noteRepository: NoteRepository,
    private val wishRepository: WishRepository,
    private val moodRepository: MoodRepository,
    private val activityRepository: ActivityRepository,
    private val calendarRepository: CalendarRepository,
    private val artRepository: ArtRepository,
    private val cycleRepository: CycleRepository,
    private val relationshipRepository: RelationshipRepository,
) {
    companion object {
        private const val TAG = "DataPreloadManager"
    }

    /** Refreshes all repositories in parallel. Suspends until all complete. */
    suspend fun preloadAll() = coroutineScope {
        Log.i(TAG, "Starting parallel preload of all repositories")
        val jobs = listOf(
            async { runCatching { noteRepository.refreshFromServer() }
                .onFailure { Log.w(TAG, "notes refresh failed", it) } },
            async { runCatching { wishRepository.refreshFromServer() }
                .onFailure { Log.w(TAG, "wishes refresh failed", it) } },
            async { runCatching { moodRepository.refreshFromServer() }
                .onFailure { Log.w(TAG, "moods refresh failed", it) } },
            async { runCatching { activityRepository.refreshFromServer() }
                .onFailure { Log.w(TAG, "activities refresh failed", it) } },
            async { runCatching { calendarRepository.refreshFromServer() }
                .onFailure { Log.w(TAG, "calendars refresh failed", it) } },
            async { runCatching { artRepository.refreshFromServer() }
                .onFailure { Log.w(TAG, "art refresh failed", it) } },
            async { runCatching { cycleRepository.refreshFromServer() }
                .onFailure { Log.w(TAG, "cycles refresh failed", it) } },
            async { runCatching { relationshipRepository.refreshFromServer() }
                .onFailure { Log.w(TAG, "relationship refresh failed", it) } },
        )
        jobs.forEach { it.await() }
        Log.i(TAG, "Preload complete")
    }
}
