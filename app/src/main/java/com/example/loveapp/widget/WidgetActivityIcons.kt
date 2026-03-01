package com.example.loveapp.widget

/**
 * Maps Material icon keys (from CUSTOM_ICON_OPTIONS) and built-in activity type keys
 * to emoji characters suitable for display inside Glance widgets.
 */
object WidgetActivityIcons {

    private val iconToEmoji = mapOf(
        // ── Built-in activity type keys ───────────────────────────────────────
        "work"     to "💼",
        "computer" to "💻",
        "sport"    to "🏃",
        "food"     to "🍽️",
        "walk"     to "🚶",
        "sleep"    to "😴",
        "reading"  to "📚",
        "social"   to "👥",
        "relax"    to "🧘",
        // ── Custom Material icon keys (mirrors CUSTOM_ICON_OPTIONS) ───────────
        "Favorite"         to "❤️",
        "Star"             to "⭐",
        "EmojiEvents"      to "🏆",
        "FitnessCenter"    to "🏋️",
        "DirectionsRun"    to "🏃",
        "SelfImprovement"  to "🧘",
        "Spa"              to "🌿",
        "LocalCafe"        to "☕",
        "LocalBar"         to "🍸",
        "Restaurant"       to "🍽️",
        "Fastfood"         to "🍔",
        "ShoppingCart"     to "🛒",
        "School"           to "🎓",
        "MenuBook"         to "📖",
        "MusicNote"        to "🎵",
        "Headphones"       to "🎧",
        "Videocam"         to "🎬",
        "PhotoCamera"      to "📷",
        "Brush"            to "🖌️",
        "Palette"          to "🎨",
        "Games"            to "🕹️",
        "SportsEsports"    to "🎮",
        "Pets"             to "🐾",
        "Park"             to "🌳",
        "FlightTakeoff"    to "✈️",
        "Hotel"            to "🏨",
        "LocalHospital"    to "🏥",
        "DirectionsCar"    to "🚗",
        "TwoWheeler"       to "🏍️",
        "Pool"             to "🏊",
        "SportsBasketball" to "🏀",
        "SportsSoccer"     to "⚽",
        "SportsTennis"     to "🎾",
        "Hiking"           to "🥾",
        "Sailing"          to "⛵",
        "Casino"           to "🎲",
        "Cake"             to "🎂",
        "CardGiftcard"     to "🎁",
        "Nightlife"        to "🌙",
        "DinnerDining"     to "🍴",
        "Work"             to "💼",
    )

    /**
     * Converts an [iconValue] (Material icon key, URL path, built-in type key) to a
     * single emoji character for widget display.
     * - URL / uploaded file path → 🖼️
     * - Known key → mapped emoji
     * - Unknown → ✨
     */
    fun toEmoji(iconValue: String?): String {
        if (iconValue.isNullOrBlank()) return "✨"
        if (iconValue.startsWith("http") || iconValue.startsWith("/uploads")) return "🖼️"
        return iconToEmoji[iconValue] ?: "✨"
    }
}
