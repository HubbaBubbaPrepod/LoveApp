package com.example.loveapp.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
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
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.layout.wrapContentWidth
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.loveapp.MainActivity
import com.example.loveapp.widget.WidgetUpdater.Companion.KEY_MOOD_MY_NAME
import com.example.loveapp.widget.WidgetUpdater.Companion.KEY_MOOD_MY_TYPE
import com.example.loveapp.widget.WidgetUpdater.Companion.KEY_MOOD_PT_NAME
import com.example.loveapp.widget.WidgetUpdater.Companion.KEY_MOOD_PT_TYPE

/**
 * Compact 2×1 mood widget – shows both users' emoji + name side by side.
 * Perfect for a narrow slot on the home screen.
 */
class MoodWidgetSmall : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { SmallContent(context) }
    }

    @Composable
    private fun SmallContent(context: Context) {
        val prefs  = currentState<Preferences>()
        val myType = prefs[KEY_MOOD_MY_TYPE] ?: ""
        val myName = (prefs[KEY_MOOD_MY_NAME]?.takeIf { it.isNotBlank() } ?: "Я").take(6)
        val ptType = prefs[KEY_MOOD_PT_TYPE] ?: ""
        val ptName = (prefs[KEY_MOOD_PT_NAME]?.takeIf { it.isNotBlank() } ?: "Партн.").take(6)

        val open = actionStartActivity(
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("destination", "mood_tracker")
            }
        )

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(Color(0xFFFFF0F5)))
                .cornerRadius(16.dp)
                .clickable(open)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ── My badge ─────────────────────────────────────────────────
                MoodBadge(
                    name     = myName,
                    moodType = myType,
                    bgColor  = Color(0x33FF6B9D),
                    nameColor = Color(0xFFFF6B9D),
                    modifier  = GlanceModifier.defaultWeight()
                )
                Spacer(GlanceModifier.width(6.dp))
                // ── Partner badge ─────────────────────────────────────────────
                MoodBadge(
                    name     = ptName,
                    moodType = ptType,
                    bgColor  = Color(0x228E8E93),
                    nameColor = Color(0xFF636366),
                    modifier  = GlanceModifier.defaultWeight()
                )
            }
        }
    }

    @Composable
    private fun MoodBadge(
        name: String,
        moodType: String,
        bgColor: Color,
        nameColor: Color,
        modifier: GlanceModifier
    ) {
        val emoji = moodEmoji(moodType)
        Box(
            modifier = modifier
                .background(ColorProvider(bgColor))
                .cornerRadius(10.dp)
                .padding(horizontal = 6.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text  = emoji,
                    style = TextStyle(fontSize = 22.sp)
                )
                Text(
                    text  = name,
                    style = TextStyle(
                        color      = ColorProvider(nameColor),
                        fontSize   = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
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
}

class MoodWidgetSmallReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = MoodWidgetSmall()

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        WidgetSyncWorker.runImmediately(context)
    }
}
