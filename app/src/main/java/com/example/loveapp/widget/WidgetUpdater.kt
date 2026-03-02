package com.example.loveapp.widget

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.example.loveapp.utils.DateUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Hilt singleton that pushes fresh data into Glance widget state and triggers re-render.
 * Injected into [com.example.loveapp.viewmodel.MoodViewModel] and
 * [com.example.loveapp.viewmodel.ActivityViewModel].
 */
@Singleton
class WidgetUpdater @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        // Mood widget keys – my data
        val KEY_MOOD_MY_TYPE = stringPreferencesKey("mood_my_type")
        val KEY_MOOD_MY_NOTE = stringPreferencesKey("mood_my_note")
        val KEY_MOOD_MY_NAME = stringPreferencesKey("mood_my_name")
        // Mood widget keys – partner data
        val KEY_MOOD_PT_TYPE = stringPreferencesKey("mood_pt_type")
        val KEY_MOOD_PT_NOTE = stringPreferencesKey("mood_pt_note")
        val KEY_MOOD_PT_NAME = stringPreferencesKey("mood_pt_name")
        val KEY_MOOD_DATE    = stringPreferencesKey("mood_date")
        // Mood widget avatar paths
        val KEY_MOOD_MY_AVATAR = stringPreferencesKey("mood_my_avatar")
        val KEY_MOOD_PT_AVATAR = stringPreferencesKey("mood_pt_avatar")

        // Activity widget keys – my data
        val KEY_ACT_MY_COUNT = intPreferencesKey("act_my_count")
        val KEY_ACT_MY_TYPES = stringPreferencesKey("act_my_types")
        val KEY_ACT_MY_ICONS = stringPreferencesKey("act_my_icons")
        val KEY_ACT_MY_NAME  = stringPreferencesKey("act_my_name")
        // Activity widget keys – partner data
        val KEY_ACT_PT_COUNT = intPreferencesKey("act_pt_count")
        val KEY_ACT_PT_TYPES = stringPreferencesKey("act_pt_types")
        val KEY_ACT_PT_ICONS = stringPreferencesKey("act_pt_icons")
        val KEY_ACT_PT_NAME  = stringPreferencesKey("act_pt_name")
        val KEY_ACT_DATE     = stringPreferencesKey("act_date")
        // Activity widget avatar paths
        val KEY_ACT_MY_AVATAR = stringPreferencesKey("act_my_avatar")
        val KEY_ACT_PT_AVATAR = stringPreferencesKey("act_pt_avatar")
    }

    /**
     * Pushes today's mood data for both the user and their partner to ALL mood widget sizes.
     * Called from [com.example.loveapp.viewmodel.MoodViewModel] after both loads complete.
     */
    suspend fun pushMoodUpdate(
        myType: String, myNote: String, myName: String?,
        ptType: String, ptNote: String, ptName: String?,
        myAvatarPath: String? = null, ptAvatarPath: String? = null
    ) = runCatching {
        val today = DateUtils.getTodayDateString()
        val mgr = GlanceAppWidgetManager(context)

        // Helper: update state + re-render for one widget class
        suspend fun <T : androidx.glance.appwidget.GlanceAppWidget> updateClass(
            widget: T, cls: Class<T>
        ) = mgr.getGlanceIds(cls).forEach { id ->
            updateAppWidgetState(context, PreferencesGlanceStateDefinition, id) { prefs: Preferences ->
                prefs.toMutablePreferences().apply {
                    this[KEY_MOOD_MY_TYPE] = myType
                    this[KEY_MOOD_MY_NOTE] = myNote
                    this[KEY_MOOD_MY_NAME] = myName ?: ""
                    this[KEY_MOOD_PT_TYPE] = ptType
                    this[KEY_MOOD_PT_NOTE] = ptNote
                    this[KEY_MOOD_PT_NAME] = ptName ?: ""
                    this[KEY_MOOD_DATE]    = today
                    this[KEY_MOOD_MY_AVATAR] = myAvatarPath ?: ""
                    this[KEY_MOOD_PT_AVATAR] = ptAvatarPath ?: ""
                }
            }
            widget.update(context, id)
        }

        updateClass(MoodWidgetSmall(), MoodWidgetSmall::class.java)
        updateClass(MoodDayWidget(),    MoodDayWidget::class.java)
        updateClass(MoodWidgetLarge(),  MoodWidgetLarge::class.java)
    }

    /**
     * Pushes today's activity data for both the user and their partner to ALL activity widget sizes.
     * Called from [com.example.loveapp.viewmodel.ActivityViewModel] after both loads complete.
     */
    suspend fun pushActivityUpdate(
        myCount: Int, myTypes: String, myIcons: String, myName: String?,
        ptCount: Int, ptTypes: String, ptIcons: String, ptName: String?,
        myAvatarPath: String? = null, ptAvatarPath: String? = null
    ) = runCatching {
        val today = DateUtils.getTodayDateString()
        val mgr = GlanceAppWidgetManager(context)

        suspend fun <T : androidx.glance.appwidget.GlanceAppWidget> updateClass(
            widget: T, cls: Class<T>
        ) = mgr.getGlanceIds(cls).forEach { id ->
            updateAppWidgetState(context, PreferencesGlanceStateDefinition, id) { prefs: Preferences ->
                prefs.toMutablePreferences().apply {
                    this[KEY_ACT_MY_COUNT] = myCount
                    this[KEY_ACT_MY_TYPES] = myTypes
                    this[KEY_ACT_MY_ICONS] = myIcons
                    this[KEY_ACT_MY_NAME]  = myName ?: ""
                    this[KEY_ACT_PT_COUNT] = ptCount
                    this[KEY_ACT_PT_TYPES] = ptTypes
                    this[KEY_ACT_PT_ICONS] = ptIcons
                    this[KEY_ACT_PT_NAME]  = ptName ?: ""
                    this[KEY_ACT_DATE]     = today
                    this[KEY_ACT_MY_AVATAR] = myAvatarPath ?: ""
                    this[KEY_ACT_PT_AVATAR] = ptAvatarPath ?: ""
                }
            }
            widget.update(context, id)
        }

        updateClass(ActivityWidgetSmall(),  ActivityWidgetSmall::class.java)
        updateClass(ActivityDayWidget(),    ActivityDayWidget::class.java)
        updateClass(ActivityWidgetLarge(),  ActivityWidgetLarge::class.java)
    }
}
