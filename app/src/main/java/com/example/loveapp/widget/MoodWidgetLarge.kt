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
 * Large 4×3 mood widget – shows each person's mood in a full card with
 * emoji, label and the full note text.
 */
class MoodWidgetLarge : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { LargeContent(context) }
    }

    @Composable
    private fun LargeContent(context: Context) {
        val prefs  = currentState<Preferences>()
        val myType = prefs[KEY_MOOD_MY_TYPE] ?: ""
        val myNote = prefs[KEY_MOOD_MY_NOTE] ?: ""
        val myName = (prefs[KEY_MOOD_MY_NAME]?.takeIf { it.isNotBlank() } ?: "Я")
        val ptType = prefs[KEY_MOOD_PT_TYPE] ?: ""
        val ptNote = prefs[KEY_MOOD_PT_NOTE] ?: ""
        val ptName = (prefs[KEY_MOOD_PT_NAME]?.takeIf { it.isNotBlank() } ?: "Партнёр")
        val date   = prefs[KEY_MOOD_DATE]    ?: ""
        val myAvatarBmp = loadWidgetIconBitmap(prefs[KEY_MOOD_MY_AVATAR] ?: "")
        val ptAvatarBmp = loadWidgetIconBitmap(prefs[KEY_MOOD_PT_AVATAR] ?: "")

        val open = actionStartActivity(
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("destination", "mood_tracker")
            }
        )

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(Color(0xFFFFF4F8)))
                .cornerRadius(22.dp)
                .clickable(open)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            contentAlignment = Alignment.TopStart
        ) {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.Top,
                horizontalAlignment = Alignment.Start
            ) {
                // ── Title row ─────────────────────────────────────────────────
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text  = "💭  Настроение за день",
                        style = TextStyle(
                            color      = ColorProvider(Color(0xFFFF6B9D)),
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                if (date.isNotEmpty()) {
                    Text(
                        text  = date,
                        style = TextStyle(
                            color    = ColorProvider(Color(0xFFAEAEB2)),
                            fontSize = 10.sp
                        )
                    )
                }

                Spacer(GlanceModifier.height(12.dp))

                // ── My full card ───────────────────────────────────────────────
                LargeMoodCard(
                    name     = myName,
                    moodType = myType,
                    note     = myNote,
                    bgColor  = Color(0x1FFF6B9D),
                    nameColor = Color(0xFFFF6B9D),
                    noteMax  = 80,
                    avatarBitmap = myAvatarBmp
                )

                Spacer(GlanceModifier.height(10.dp))

                // ── Partner full card ──────────────────────────────────────────
                LargeMoodCard(
                    name     = ptName,
                    moodType = ptType,
                    note     = ptNote,
                    bgColor  = Color(0x158E8E93),
                    nameColor = Color(0xFF636366),
                    noteMax  = 80,
                    avatarBitmap = ptAvatarBmp
                )
            }
        }
    }

    @Composable
    private fun LargeMoodCard(
        name: String,
        moodType: String,
        note: String,
        bgColor: Color,
        nameColor: Color,
        noteMax: Int,
        avatarBitmap: Bitmap? = null
    ) {
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(ColorProvider(bgColor))
                .cornerRadius(14.dp)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Column(horizontalAlignment = Alignment.Start) {
                // Name + emoji in a row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (avatarBitmap != null) {
                        Image(
                            provider = ImageProvider(avatarBitmap),
                            contentDescription = name,
                            modifier = GlanceModifier.size(32.dp).cornerRadius(16.dp)
                        )
                        Spacer(GlanceModifier.width(8.dp))
                    } else {
                        Text(
                            text  = moodEmoji(moodType),
                            style = TextStyle(fontSize = 30.sp)
                        )
                        Spacer(GlanceModifier.width(10.dp))
                    }
                    Column {
                        Text(
                            text  = name,
                            style = TextStyle(
                                color      = ColorProvider(nameColor),
                                fontSize   = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text  = moodLabel(moodType),
                            style = TextStyle(
                                color      = ColorProvider(moodTextColor(moodType)),
                                fontSize   = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
                // Note
                if (note.isNotEmpty()) {
                    Spacer(GlanceModifier.height(6.dp))
                    Text(
                        text  = if (note.length > noteMax) note.take(noteMax - 3) + "…" else note,
                        style = TextStyle(
                            color    = ColorProvider(Color(0xFF636366)),
                            fontSize = 11.sp
                        )
                    )
                } else if (moodType.isEmpty()) {
                    Spacer(GlanceModifier.height(4.dp))
                    Text(
                        text  = "Нет записи",
                        style = TextStyle(
                            color    = ColorProvider(Color(0xFFAEAEB2)),
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }
    }

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
        else    -> "—"
    }

    private fun moodTextColor(type: String) = when (type.lowercase()) {
        "rad"   -> Color(0xFFFF375F)
        "good"  -> Color(0xFF30D158)
        "meh"   -> Color(0xFFB8960A)
        "bad"   -> Color(0xFFFF9F0A)
        "awful" -> Color(0xFF5E5CE6)
        else    -> Color(0xFF8E8E93)
    }
}

class MoodWidgetLargeReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = MoodWidgetLarge()

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        WidgetSyncWorker.runImmediately(context)
    }
}
