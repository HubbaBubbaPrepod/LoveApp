package com.example.loveapp.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.loveapp.MainActivity
import com.example.loveapp.widget.WidgetUpdater.Companion.KEY_MOOD_DATE
import com.example.loveapp.widget.WidgetUpdater.Companion.KEY_MOOD_MY_AVATAR
import com.example.loveapp.widget.WidgetUpdater.Companion.KEY_MOOD_MY_NAME
import com.example.loveapp.widget.WidgetUpdater.Companion.KEY_MOOD_MY_NOTE
import com.example.loveapp.widget.WidgetUpdater.Companion.KEY_MOOD_MY_TYPE
import com.example.loveapp.widget.WidgetUpdater.Companion.KEY_MOOD_PT_AVATAR
import com.example.loveapp.widget.WidgetUpdater.Companion.KEY_MOOD_PT_NAME
import com.example.loveapp.widget.WidgetUpdater.Companion.KEY_MOOD_PT_NOTE
import com.example.loveapp.widget.WidgetUpdater.Companion.KEY_MOOD_PT_TYPE

/**
 * Home-screen widget showing today's mood for both the user and their partner.
 * Tapping the widget opens the Mood Tracker screen directly.
 * Data is pushed via [WidgetUpdater.pushMoodUpdate] from [com.example.loveapp.viewmodel.MoodViewModel].
 */
class MoodDayWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { MoodContent(context) }
    }

    @Composable
    private fun MoodContent(context: Context) {
        val prefs  = currentState<Preferences>()
        val myType = prefs[KEY_MOOD_MY_TYPE] ?: ""
        val myNote = prefs[KEY_MOOD_MY_NOTE] ?: ""
        val myName = (prefs[KEY_MOOD_MY_NAME]?.takeIf { it.isNotBlank() } ?: "Я").take(8)
        val ptType = prefs[KEY_MOOD_PT_TYPE] ?: ""
        val ptNote = prefs[KEY_MOOD_PT_NOTE] ?: ""
        val ptName = (prefs[KEY_MOOD_PT_NAME]?.takeIf { it.isNotBlank() } ?: "Партнёр").take(8)
        val date   = prefs[KEY_MOOD_DATE]    ?: ""
        val myAvatarBmp = loadWidgetIconBitmap(prefs[KEY_MOOD_MY_AVATAR] ?: "")
        val ptAvatarBmp = loadWidgetIconBitmap(prefs[KEY_MOOD_PT_AVATAR] ?: "")

        val openMood = actionStartActivity(
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("destination", "mood_tracker")
            }
        )

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(Color(0xFFFFF8FB)))
                .cornerRadius(20.dp)
                .clickable(openMood)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            contentAlignment = Alignment.TopStart
        ) {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.Top,
                horizontalAlignment = Alignment.Start
            ) {
                // ── Header ───────────────────────────────────────────────────
                val header = if (date.isNotEmpty()) "💭  Настроение · $date"
                             else "💭  Настроение сегодня"
                Text(
                    text  = header,
                    style = TextStyle(
                        color      = ColorProvider(Color(0xFF8E8E93)),
                        fontSize   = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
                Spacer(GlanceModifier.height(8.dp))

                // ── My mood ──────────────────────────────────────────────────
                MoodRow(name = myName, moodType = myType, note = myNote, isMe = true, avatarBitmap = myAvatarBmp)

                Spacer(GlanceModifier.height(6.dp))

                // ── Partner mood ─────────────────────────────────────────────
                MoodRow(name = ptName, moodType = ptType, note = ptNote, isMe = false, avatarBitmap = ptAvatarBmp)
            }
        }
    }

    @Composable
    private fun MoodRow(name: String, moodType: String, note: String, isMe: Boolean, avatarBitmap: Bitmap? = null) {
        val rowBg   = if (isMe) Color(0x1AFF6B9D) else Color(0x1A8E8E93)
        val nameClr = if (isMe) Color(0xFFFF6B9D) else Color(0xFF636366)

        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(ColorProvider(rowBg))
                .cornerRadius(10.dp)
                .padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (avatarBitmap != null) {
                Image(
                    provider = ImageProvider(avatarBitmap),
                    contentDescription = name,
                    modifier = GlanceModifier.size(18.dp).cornerRadius(9.dp)
                )
                Spacer(GlanceModifier.width(4.dp))
            }
            Text(
                text     = name,
                style    = TextStyle(
                    color      = ColorProvider(nameClr),
                    fontSize   = 10.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = GlanceModifier.width(if (avatarBitmap != null) 40.dp else 58.dp)
            )
            if (moodType.isEmpty()) {
                Text(
                    text  = "—",
                    style = TextStyle(
                        color    = ColorProvider(Color(0xFFAEAEB2)),
                        fontSize = 16.sp
                    )
                )
            } else {
                Text(
                    text  = moodEmoji(moodType),
                    style = TextStyle(fontSize = 18.sp)
                )
                Spacer(GlanceModifier.width(6.dp))
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text  = moodLabel(moodType),
                        style = TextStyle(
                            color      = ColorProvider(moodTextColor(moodType)),
                            fontSize   = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    if (note.isNotEmpty()) {
                        Text(
                            text  = if (note.length > 20) note.take(17) + "…" else note,
                            style = TextStyle(
                                color    = ColorProvider(Color(0xFF8E8E93)),
                                fontSize = 9.sp
                            )
                        )
                    }
                }
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun moodEmoji(type: String) = when (type.lowercase()) {
        "rad"   -> "😄"
        "good"  -> "😊"
        "meh"   -> "😐"
        "bad"   -> "😔"
        "awful" -> "😞"
        else    -> "💭"
    }

    private fun moodLabel(type: String) = when (type.lowercase()) {
        "rad"   -> "Отлично"
        "good"  -> "Хорошо"
        "meh"   -> "Нейтрально"
        "bad"   -> "Плохо"
        "awful" -> "Ужасно"
        else    -> "Нет записи"
    }

    private fun moodTextColor(type: String) = when (type.lowercase()) {
        "rad"   -> Color(0xFFFF375F)
        "good"  -> Color(0xFF30D158)
        "meh"   -> Color(0xFFB8960A)
        "bad"   -> Color(0xFFFF9F0A)
        "awful" -> Color(0xFF5E5CE6)
        else    -> Color(0xFF1C1C1E)
    }
}

class MoodDayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = MoodDayWidget()

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        WidgetSyncWorker.runImmediately(context)
    }
}
