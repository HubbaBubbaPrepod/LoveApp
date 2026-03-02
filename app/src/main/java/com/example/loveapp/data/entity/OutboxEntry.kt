package com.example.loveapp.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Offline sync queue.  Every mutation that cannot be immediately confirmed
 * by the server gets enqueued here.  SyncWorker drains the table in order,
 * sending each entry over WebSocket (or falling back to REST) and removing
 * it only after receiving acknowledgement.
 */
@Entity(tableName = "outbox")
data class OutboxEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    /** Entity type: "note" | "wish" | "mood" | "activity" |
     *  "cycle" | "calendar" | "event" | "relationship"       */
    val entityType: String,

    /** CRUD action: "create" | "update" | "delete"            */
    val action: String,

    /** JSON-serialised entity payload (Gson.toJson of the entity) */
    val payload: String,

    /** Local Room primary key of the related entity             */
    val localId: Int,

    /** Server-assigned ID (null until first successful create) */
    val serverId: Int? = null,

    val createdAt: Long = System.currentTimeMillis(),

    /** Number of failed delivery attempts (for exponential back-off) */
    val retryCount: Int = 0,

    /** Timestamp after which the next retry is allowed          */
    val retryAfter: Long = 0L
)
