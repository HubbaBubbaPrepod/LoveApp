package com.example.loveapp.network

import android.util.Log
import com.example.loveapp.data.dao.ActivityLogDao
import com.example.loveapp.data.dao.CustomCalendarDao
import com.example.loveapp.data.dao.CustomCalendarEventDao
import com.example.loveapp.data.dao.MenstrualCycleDao
import com.example.loveapp.data.dao.MoodEntryDao
import com.example.loveapp.data.dao.NoteDao
import com.example.loveapp.data.dao.RelationshipInfoDao
import com.example.loveapp.data.dao.WishDao
import com.example.loveapp.data.entity.ActivityLog
import com.example.loveapp.data.entity.CustomCalendar
import com.example.loveapp.data.entity.CustomCalendarEvent
import com.example.loveapp.data.entity.MenstrualCycleEntry
import com.example.loveapp.data.entity.MoodEntry
import com.example.loveapp.data.entity.Note
import com.example.loveapp.data.entity.RelationshipInfo
import com.example.loveapp.data.entity.Wish
import com.example.loveapp.utils.TokenManager
import com.google.gson.Gson
import com.google.gson.JsonObject
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

/** Observable connection states exposed to the UI layer. */
enum class SocketState { DISCONNECTED, CONNECTING, CONNECTED, ERROR }

/**
 * Manages the Socket.IO connection lifecycle.
 *
 * Responsibilities:
 *  - Authenticate with JWT on every connection.
 *  - Receive `data-change` events and upsert them into Room as the source of truth.
 *  - Send `sync-request` on reconnect to catch up on missed changes.
 *  - Emit outgoing `data-change` events from callers (repositories / SyncWorker).
 *  - Expose [connectionState] so the UI can show an offline badge.
 */
@Singleton
class WebSocketManager @Inject constructor(
    private val tokenManager: TokenManager,
    private val noteDao: NoteDao,
    private val wishDao: WishDao,
    private val moodEntryDao: MoodEntryDao,
    private val activityLogDao: ActivityLogDao,
    private val menstrualCycleDao: MenstrualCycleDao,
    private val customCalendarDao: CustomCalendarDao,
    private val customCalendarEventDao: CustomCalendarEventDao,
    private val relationshipInfoDao: RelationshipInfoDao
) {
    companion object {
        private const val TAG = "WebSocketManager"
        private const val SERVER_URL = "https://love-app.ru"
        private const val PREF_LAST_SYNC = "ws_last_sync_ts"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gson = Gson()

    private var socket: Socket? = null

    private val _connectionState = MutableStateFlow(SocketState.DISCONNECTED)
    val connectionState: StateFlow<SocketState> = _connectionState.asStateFlow()

    /** Timestamp of the last successfully applied `data-change` event. */
    @Volatile
    private var lastSyncTimestamp: Long = 0L

    // ─── Lifecycle ────────────────────────────────────────────────────────

    /** Call once (e.g., from Application.onCreate or after login). */
    fun connect() {
        if (socket?.connected() == true) return
        scope.launch { connectWithToken() }
    }

    private suspend fun connectWithToken() {
        val token = tokenManager.getToken() ?: run {
            Log.w(TAG, "connect() skipped – no token available")
            return
        }
        _connectionState.value = SocketState.CONNECTING
        try {
            val opts = IO.Options.builder()
                .setAuth(mapOf("token" to token))
                .setReconnection(true)
                .setReconnectionAttempts(Int.MAX_VALUE)
                .setReconnectionDelay(1_000)
                .setReconnectionDelayMax(30_000)
                .build()

            socket = IO.socket(URI.create(SERVER_URL), opts).also { s ->
                s.on(Socket.EVENT_CONNECT)           { onConnect() }
                s.on(Socket.EVENT_DISCONNECT)         { onDisconnect() }
                s.on(Socket.EVENT_CONNECT_ERROR)      { args -> onError(args) }
                s.on("data-change")                   { args -> onDataChange(args) }
                s.on("sync-response")                 { args -> onSyncResponse(args) }
                s.connect()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Socket creation failed", e)
            _connectionState.value = SocketState.ERROR
        }
    }

    fun disconnect() {
        socket?.disconnect()
        socket = null
        _connectionState.value = SocketState.DISCONNECTED
    }

    /** Re-authenticate with a fresh token after token refresh. */
    fun reconnectWithNewToken() {
        disconnect()
        connect()
    }

    // ─── Outgoing events ─────────────────────────────────────────────────

    /**
     * Emit a data mutation to the server.
     * Returns `true` if the socket was connected and the event was sent.
     */
    fun emitChange(entityType: String, action: String, payload: Map<String, Any?>): Boolean {
        val s = socket ?: return false
        if (!s.connected()) return false
        val json = JSONObject(payload as Map<*, *>)
        val event = JSONObject().apply {
            put("entityType", entityType)
            put("action", action)
            put("data", json)
            put("clientTimestamp", System.currentTimeMillis())
        }
        s.emit("data-change", event)
        return true
    }

    // ─── Socket event handlers ────────────────────────────────────────────

    private fun onConnect() {
        Log.i(TAG, "Socket connected")
        _connectionState.value = SocketState.CONNECTED
        requestSync()
    }

    private fun onDisconnect() {
        Log.i(TAG, "Socket disconnected")
        _connectionState.value = SocketState.DISCONNECTED
    }

    private fun onError(args: Array<Any>) {
        Log.e(TAG, "Socket error: ${args.firstOrNull()}")
        _connectionState.value = SocketState.ERROR
    }

    private fun requestSync() {
        val s = socket ?: return
        val req = JSONObject().apply {
            put("since", lastSyncTimestamp)
        }
        s.emit("sync-request", req)
        Log.d(TAG, "Emitted sync-request since=$lastSyncTimestamp")
    }

    // ─── Incoming data-change ─────────────────────────────────────────────

    private fun onDataChange(args: Array<Any>) {
        val raw = args.firstOrNull() as? JSONObject ?: return
        val entityType = raw.optString("entityType") ?: return
        val action     = raw.optString("action") ?: return
        val data       = raw.optJSONObject("data") ?: return
        val serverTs   = raw.optLong("serverTimestamp", System.currentTimeMillis())

        Log.d(TAG, "data-change entity=$entityType action=$action")
        scope.launch {
            applyChange(entityType, action, data)
            if (serverTs > lastSyncTimestamp) lastSyncTimestamp = serverTs
        }
    }

    private fun onSyncResponse(args: Array<Any>) {
        val raw = args.firstOrNull() as? JSONObject ?: return
        Log.d(TAG, "sync-response received")
        scope.launch {
            val keys = raw.keys()
            while (keys.hasNext()) {
                val entityType = keys.next()
                val arr = raw.optJSONArray(entityType) ?: continue
                for (i in 0 until arr.length()) {
                    val item = arr.optJSONObject(i) ?: continue
                    applyChange(entityType, "upsert", item)
                }
            }
            lastSyncTimestamp = System.currentTimeMillis()
        }
    }

    // ─── Upsert helpers ──────────────────────────────────────────────────

    /**
     * Maps the raw JSON from the server onto a Room entity and persists it.
     * "delete" action performs a soft-delete (sets deletedAt).
     * All other actions upsert the row.
     */
    private suspend fun applyChange(entityType: String, action: String, data: JSONObject) {
        try {
            val j = gson.fromJson(data.toString(), JsonObject::class.java)
            val serverId = j.get("id")?.asInt ?: return

            when (entityType) {
                "note" -> upsertNote(serverId, action, j)
                "wish" -> upsertWish(serverId, action, j)
                "mood" -> upsertMood(serverId, action, j)
                "activity" -> upsertActivity(serverId, action, j)
                "cycle" -> upsertCycle(serverId, action, j)
                "calendar" -> upsertCalendar(serverId, action, j)
                "event" -> upsertEvent(serverId, action, j)
                "relationship" -> upsertRelationship(serverId, action, j)
                else -> Log.w(TAG, "Unknown entity type: $entityType")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply $entityType/$action", e)
        }
    }

    private fun Long?.orNow() = this ?: System.currentTimeMillis()

    private suspend fun upsertNote(serverId: Int, action: String, j: JsonObject) {
        val deletedAt = if (action == "delete") System.currentTimeMillis() else null
        val existing = noteDao.getByServerId(serverId)
        val note = Note(
            id            = existing?.id ?: 0,
            title         = j.get("title")?.asString ?: existing?.title ?: "",
            content       = j.get("content")?.asString ?: existing?.content ?: "",
            userId        = j.get("user_id")?.asInt ?: existing?.userId ?: 0,
            createdAt     = j.get("created_at")?.asLong ?: existing?.createdAt.orNow(),
            updatedAt     = j.get("updated_at")?.asLong ?: System.currentTimeMillis(),
            isPrivate     = j.get("is_private")?.asBoolean ?: existing?.isPrivate ?: false,
            tags          = j.get("tags")?.asString ?: existing?.tags ?: "",
            dueDate       = if (j.has("due_date") && !j.get("due_date").isJsonNull) j.get("due_date").asLong else existing?.dueDate,
            imageUrl      = if (j.has("image_url") && !j.get("image_url").isJsonNull) j.get("image_url").asString else existing?.imageUrl,
            serverId      = serverId,
            syncPending   = false,
            serverUpdatedAt = j.get("server_updated_at")?.asLong,
            deletedAt     = deletedAt ?: existing?.deletedAt
        )
        noteDao.upsert(note)
    }

    private suspend fun upsertWish(serverId: Int, action: String, j: JsonObject) {
        val deletedAt = if (action == "delete") System.currentTimeMillis() else null
        val existing = wishDao.getByServerId(serverId)
        val wish = Wish(
            id           = existing?.id ?: 0,
            title        = j.get("title")?.asString ?: existing?.title ?: "",
            description  = j.get("description")?.asString ?: existing?.description ?: "",
            userId       = j.get("user_id")?.asInt ?: existing?.userId ?: 0,
            createdAt    = j.get("created_at")?.asLong ?: existing?.createdAt.orNow(),
            isCompleted  = j.get("is_completed")?.asBoolean ?: existing?.isCompleted ?: false,
            completedAt  = if (j.has("completed_at") && !j.get("completed_at").isJsonNull) j.get("completed_at").asLong else existing?.completedAt,
            priority     = j.get("priority")?.asInt ?: existing?.priority ?: 0,
            imageUrl     = if (j.has("image_url") && !j.get("image_url").isJsonNull) j.get("image_url").asString else existing?.imageUrl,
            category     = j.get("category")?.asString ?: existing?.category ?: "",
            dueDate      = if (j.has("due_date") && !j.get("due_date").isJsonNull) j.get("due_date").asLong else existing?.dueDate,
            serverId     = serverId,
            syncPending  = false,
            serverUpdatedAt = j.get("server_updated_at")?.asLong,
            deletedAt    = deletedAt ?: existing?.deletedAt
        )
        wishDao.upsert(wish)
    }

    private suspend fun upsertMood(serverId: Int, action: String, j: JsonObject) {
        val deletedAt = if (action == "delete") System.currentTimeMillis() else null
        val existing = moodEntryDao.getByServerId(serverId)
        val mood = MoodEntry(
            id          = existing?.id ?: 0,
            userId      = j.get("user_id")?.asInt ?: existing?.userId ?: 0,
            moodType    = j.get("mood_type")?.asString ?: existing?.moodType ?: "",
            timestamp   = j.get("timestamp")?.asLong ?: existing?.timestamp.orNow(),
            date        = j.get("date")?.asString ?: existing?.date ?: "",
            note        = j.get("note")?.asString ?: existing?.note ?: "",
            color       = j.get("color")?.asString ?: existing?.color ?: "",
            serverId    = serverId,
            syncPending = false,
            serverUpdatedAt = j.get("server_updated_at")?.asLong,
            deletedAt   = deletedAt ?: existing?.deletedAt
        )
        moodEntryDao.upsert(mood)
    }

    private suspend fun upsertActivity(serverId: Int, action: String, j: JsonObject) {
        val deletedAt = if (action == "delete") System.currentTimeMillis() else null
        val existing = activityLogDao.getByServerId(serverId)
        val log = ActivityLog(
            id          = existing?.id ?: 0,
            userId      = j.get("user_id")?.asInt ?: existing?.userId ?: 0,
            title       = j.get("title")?.asString ?: existing?.title ?: "",
            description = j.get("description")?.asString ?: existing?.description ?: "",
            timestamp   = j.get("timestamp")?.asLong ?: existing?.timestamp.orNow(),
            date        = j.get("date")?.asString ?: existing?.date ?: "",
            imageUrls   = j.get("image_urls")?.asString ?: existing?.imageUrls ?: "",
            category    = j.get("category")?.asString ?: existing?.category ?: "",
            photoPath   = if (j.has("photo_path") && !j.get("photo_path").isJsonNull) j.get("photo_path").asString else existing?.photoPath,
            serverId    = serverId,
            syncPending = false,
            serverUpdatedAt = j.get("server_updated_at")?.asLong,
            deletedAt   = deletedAt ?: existing?.deletedAt
        )
        activityLogDao.upsert(log)
    }

    private suspend fun upsertCycle(serverId: Int, action: String, j: JsonObject) {
        val deletedAt = if (action == "delete") System.currentTimeMillis() else null
        val existing = menstrualCycleDao.getByServerId(serverId)
        val entry = MenstrualCycleEntry(
            id             = existing?.id ?: 0,
            userId         = j.get("user_id")?.asInt ?: existing?.userId ?: 0,
            cycleStartDate = j.get("cycle_start_date")?.asLong ?: existing?.cycleStartDate ?: 0L,
            cycleDuration  = j.get("cycle_duration")?.asInt ?: existing?.cycleDuration ?: 28,
            periodDuration = j.get("period_duration")?.asInt ?: existing?.periodDuration ?: 5,
            lastUpdated    = j.get("last_updated")?.asLong ?: System.currentTimeMillis(),
            symptoms       = j.get("symptoms")?.asString ?: existing?.symptoms ?: "",
            mood           = j.get("mood")?.asString ?: existing?.mood ?: "",
            notes          = j.get("notes")?.asString ?: existing?.notes ?: "",
            serverId       = serverId,
            syncPending    = false,
            serverUpdatedAt = j.get("server_updated_at")?.asLong,
            deletedAt      = deletedAt ?: existing?.deletedAt
        )
        menstrualCycleDao.upsert(entry)
    }

    private suspend fun upsertCalendar(serverId: Int, action: String, j: JsonObject) {
        val deletedAt = if (action == "delete") System.currentTimeMillis() else null
        val existing = customCalendarDao.getByServerId(serverId)
        val cal = CustomCalendar(
            id          = existing?.id ?: 0,
            name        = j.get("name")?.asString ?: existing?.name ?: "",
            description = j.get("description")?.asString ?: existing?.description ?: "",
            type        = j.get("type")?.asString ?: existing?.type ?: "custom",
            colorHex    = j.get("color_hex")?.asString ?: existing?.colorHex ?: "#FF4081",
            icon        = j.get("icon")?.asString ?: existing?.icon ?: "",
            createdAt   = j.get("created_at")?.asLong ?: existing?.createdAt.orNow(),
            userId      = j.get("user_id")?.asInt ?: existing?.userId ?: 0,
            serverId    = serverId,
            syncPending = false,
            serverUpdatedAt = j.get("server_updated_at")?.asLong,
            deletedAt   = deletedAt ?: existing?.deletedAt
        )
        customCalendarDao.upsert(cal)
    }

    private suspend fun upsertEvent(serverId: Int, action: String, j: JsonObject) {
        val deletedAt = if (action == "delete") System.currentTimeMillis() else null
        val existing = customCalendarEventDao.getByServerId(serverId)
        val event = CustomCalendarEvent(
            id          = existing?.id ?: 0,
            calendarId  = j.get("calendar_id")?.asInt ?: existing?.calendarId ?: 0,
            title       = j.get("title")?.asString ?: existing?.title ?: "",
            description = j.get("description")?.asString ?: existing?.description ?: "",
            eventDate   = j.get("event_date")?.asLong ?: existing?.eventDate ?: 0L,
            eventType   = j.get("event_type")?.asString ?: existing?.eventType ?: "other",
            imageUrl    = if (j.has("image_url") && !j.get("image_url").isJsonNull) j.get("image_url").asString else existing?.imageUrl,
            markedDate  = j.get("marked_date")?.asString ?: existing?.markedDate ?: "",
            createdAt   = j.get("created_at")?.asLong ?: existing?.createdAt.orNow(),
            serverId    = serverId,
            syncPending = false,
            serverUpdatedAt = j.get("server_updated_at")?.asLong,
            deletedAt   = deletedAt ?: existing?.deletedAt
        )
        customCalendarEventDao.upsert(event)
    }

    private suspend fun upsertRelationship(serverId: Int, action: String, j: JsonObject) {
        val deletedAt = if (action == "delete") System.currentTimeMillis() else null
        val existing = relationshipInfoDao.getByServerId(serverId)
        val rel = RelationshipInfo(
            id                    = existing?.id ?: 0,
            relationshipStartDate = j.get("relationship_start_date")?.asLong ?: existing?.relationshipStartDate ?: 0L,
            firstKissDate         = if (j.has("first_kiss_date") && !j.get("first_kiss_date").isJsonNull) j.get("first_kiss_date").asLong else existing?.firstKissDate,
            anniversaryDate       = if (j.has("anniversary_date") && !j.get("anniversary_date").isJsonNull) j.get("anniversary_date").asLong else existing?.anniversaryDate,
            userId1               = j.get("user_id_1")?.asInt ?: existing?.userId1 ?: 0,
            userId2               = j.get("user_id_2")?.asInt ?: existing?.userId2 ?: 0,
            nickname1             = j.get("nickname1")?.asString ?: existing?.nickname1 ?: "",
            nickname2             = j.get("nickname2")?.asString ?: existing?.nickname2 ?: "",
            createdAt             = j.get("created_at")?.asLong ?: existing?.createdAt.orNow(),
            serverId              = serverId,
            syncPending           = false,
            serverUpdatedAt       = j.get("server_updated_at")?.asLong,
            deletedAt             = deletedAt ?: existing?.deletedAt
        )
        relationshipInfoDao.upsert(rel)
    }
}
