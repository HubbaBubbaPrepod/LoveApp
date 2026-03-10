package com.example.loveapp.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Signup : Screen("signup")
    object Dashboard : Screen("dashboard")
    object Notes : Screen("notes")
    object NoteDetail : Screen("note_detail/{noteId}") {
        fun createRoute(noteId: Int = -1) = "note_detail/$noteId"
    }
    object Wishes : Screen("wishes")
    object WishDetail : Screen("wish_detail/{wishId}") {
        fun createRoute(wishId: Int = -1) = "wish_detail/$wishId"
    }
    object MoodTracker : Screen("mood_tracker")
    object ActivityFeed : Screen("activity_feed")
    object MenstrualCalendar : Screen("menstrual_calendar")
    object CustomCalendars : Screen("custom_calendars")
    object RelationshipDashboard : Screen("relationship_dashboard")
    object Settings : Screen("settings")
    object Pairing : Screen("pairing")
    object PrivacyPolicy : Screen("privacy_policy")
    object TermsOfUse : Screen("terms_of_use")
    object ProfileSetup : Screen("profile_setup")
    object ArtGallery : Screen("art_gallery")
    object CanvasEditor : Screen("canvas_editor/{canvasId}") {
        fun createRoute(canvasId: Int) = "canvas_editor/$canvasId"
    }
    object Chat : Screen("chat")
    object MemorialDays : Screen("memorial_days")
    object SparkInfo : Screen("spark_info")
    object TaskCenter : Screen("task_center")
    object SleepTracker : Screen("sleep_tracker")
    object Gallery : Screen("gallery")
    object LoveTouch : Screen("love_touch")
    object MissYou : Screen("miss_you")
    object AppLockSettings : Screen("app_lock_settings")
    object ChatSettings : Screen("chat_settings")
    object DailyQA : Screen("daily_qa")
    object Intimacy : Screen("intimacy")
    object Moments : Screen("moments")
    object Places : Screen("places")
    object Pet : Screen("pet")
    object Games : Screen("games")
    object Achievements : Screen("achievements")
    object Letters : Screen("letters")
    object BucketList : Screen("bucket_list")
    object MoodInsights : Screen("mood_insights")
    object Premium : Screen("premium")
    object Story : Screen("story")
    object LocationMap : Screen("location_map")
    object Geofences : Screen("geofences")
    object PhoneStatus : Screen("phone_status")
    object Shop : Screen("shop")
    object Drawing : Screen("drawing")
}
