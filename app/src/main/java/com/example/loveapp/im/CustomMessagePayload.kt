package com.example.loveapp.im

import com.google.gson.annotations.SerializedName

/**
 * Envelope for custom TIM messages (C2C custom element payload).
 * Love8 uses custom messages for Spark, Intimacy, LoveTouch, Drawing, etc.
 */
data class CustomMessagePayload(
    @SerializedName("type") val type: String,
    @SerializedName("data") val data: String? = null
) {
    companion object {
        const val TYPE_TEXT = "text"
        const val TYPE_IMAGE = "image"
        const val TYPE_VOICE = "voice"
        const val TYPE_VIDEO = "video"
        const val TYPE_LOCATION = "location"

        // Custom app-level types (like Love8)
        const val TYPE_SPARK = "spark"
        const val TYPE_INTIMACY = "intimacy"
        const val TYPE_LOVE_TOUCH = "love_touch"
        const val TYPE_LOVE_TOUCH_MOVE = "love_touch_move"
        const val TYPE_DRAW_ACTION = "draw_action"
        const val TYPE_GIFT = "gift"
        const val TYPE_SYSTEM = "system"
        const val TYPE_MISS_YOU = "miss_you"
    }
}
