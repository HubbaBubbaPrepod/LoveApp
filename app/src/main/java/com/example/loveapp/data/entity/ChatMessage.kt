package com.example.loveapp.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val senderId: Int,
    val receiverId: Int,
    val coupleKey: String = "",
    val messageType: String = "text", // text, image, emoji, voice, video, location, sticker, drawing, custom
    val content: String = "",
    val imageUrl: String? = null,
    val audioUrl: String? = null,
    val audioDurationSeconds: Int = 0,
    // ── Sticker fields ─────────────────────────────────────
    val stickerId: String? = null,
    val stickerUrl: String? = null,
    val isPopupSticker: Boolean = false,
    // ── Drawing field ──────────────────────────────────────
    val drawingUrl: String? = null,
    // ── Video fields ───────────────────────────────────────
    val videoUrl: String? = null,
    val videoDurationSeconds: Int = 0,
    val videoThumbnailUrl: String? = null,
    // ── Location fields ────────────────────────────────────
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationName: String? = null,
    // ── Emoji (I Miss U) ───────────────────────────────────
    val emojiType: String? = null,
    // ── Common ─────────────────────────────────────────────
    val isRead: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    // ── TIM SDK fields ─────────────────────────────────────
    val timMsgId: String? = null,
    val timCustomType: String? = null,
    val timCustomData: String? = null,
    val isRevoked: Boolean = false,
    // ── Sync metadata ──────────────────────────────────────
    val serverId: Int? = null,
    val syncPending: Boolean = true,
    val serverUpdatedAt: Long? = null,
    val deletedAt: Long? = null
)
