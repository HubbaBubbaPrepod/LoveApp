package com.example.loveapp.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gallery_photos")
data class GalleryPhoto(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val coupleKey: String = "",
    val userId: Int,
    val imageUrl: String,
    val thumbnailUrl: String? = null,
    val caption: String = "",
    val width: Int = 0,
    val height: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    // ── Sync metadata ──────────────────────────────────────
    val serverId: Int? = null,
    val syncPending: Boolean = true,
    val serverUpdatedAt: Long? = null,
    val deletedAt: Long? = null
)
