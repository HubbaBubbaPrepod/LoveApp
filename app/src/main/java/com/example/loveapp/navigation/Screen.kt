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
}
