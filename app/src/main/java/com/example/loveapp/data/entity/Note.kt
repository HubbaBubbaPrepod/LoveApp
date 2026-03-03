package com.example.loveapp.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val content: String,
    val userId: Int, // ID of the user who created it
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isPrivate: Boolean = false,
    val tags: String = "", // comma-separated tags
    val dueDate: Long? = null,
    val imageUrl: String? = null,
    // ── Display metadata (populated from server response) ───
    val displayName: String? = null,
    val userAvatar: String? = null,
    // ── Sync metadata ──────────────────────────────────────
    val serverId: Int? = null,          // server-assigned ID after first sync
    val syncPending: Boolean = true,    // true = not yet confirmed by server
    val serverUpdatedAt: Long? = null,  // server timestamp of last successful write
    val deletedAt: Long? = null         // soft-delete: non-null means deleted
)
