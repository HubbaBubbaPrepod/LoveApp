package com.example.loveapp.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
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
import com.example.loveapp.widget.WidgetUpdater.Companion.KEY_ACT_DATE
import com.example.loveapp.widget.WidgetUpdater.Companion.KEY_ACT_MY_COUNT
import com.example.loveapp.widget.WidgetUpdater.Companion.KEY_ACT_MY_ICONS
import com.example.loveapp.widget.WidgetUpdater.Companion.KEY_ACT_MY_NAME
import com.example.loveapp.widget.WidgetUpdater.Companion.KEY_ACT_MY_TYPES
import com.example.loveapp.widget.WidgetUpdater.Companion.KEY_ACT_PT_COUNT
import com.example.loveapp.widget.WidgetUpdater.Companion.KEY_ACT_PT_ICONS
import com.example.loveapp.widget.WidgetUpdater.Companion.KEY_ACT_PT_NAME
import com.example.loveapp.widget.WidgetUpdater.Companion.KEY_ACT_PT_TYPES

/**
 * Home-screen widget showing today's activities for both the user and their partner.
 * Tapping the widget opens the Activity Feed screen directly.
 * Data is pushed via [WidgetUpdater.pushActivityUpdate] from [com.example.loveapp.viewmodel.ActivityViewModel].
 */
class ActivityDayWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { ActivityContent(context) }
    }

    @Composable
    private fun ActivityContent(context: Context) {
        val prefs      = currentState<Preferences>()
        val myCount    = prefs[KEY_ACT_MY_COUNT] ?: 0
        val myTypesRaw = prefs[KEY_ACT_MY_TYPES] ?: ""
        val myIconsRaw = prefs[KEY_ACT_MY_ICONS] ?: ""
        val myName     = (prefs[KEY_ACT_MY_NAME]?.takeIf { it.isNotBlank() } ?: "Я").take(8)
        val ptCount    = prefs[KEY_ACT_PT_COUNT] ?: 0
        val ptTypesRaw = prefs[KEY_ACT_PT_TYPES] ?: ""
        val ptIconsRaw = prefs[KEY_ACT_PT_ICONS] ?: ""
        val ptName     = (prefs[KEY_ACT_PT_NAME]?.takeIf { it.isNotBlank() } ?: "Партнёр").take(8)
        val date       = prefs[KEY_ACT_DATE]     ?: ""

        val myTypes = if (myTypesRaw.isBlank()) emptyList()
                      else myTypesRaw.split(",").filter { it.isNotBlank() }.take(4)
        val myIcons = if (myIconsRaw.isBlank()) emptyList()
                      else myIconsRaw.split(",").filter { it.isNotBlank() }.take(4)
        val ptTypes = if (ptTypesRaw.isBlank()) emptyList()
                      else ptTypesRaw.split(",").filter { it.isNotBlank() }.take(4)
        val ptIcons = if (ptIconsRaw.isBlank()) emptyList()
                      else ptIconsRaw.split(",").filter { it.isNotBlank() }.take(4)

        val openActivity = actionStartActivity(
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("destination", "activity_feed")
            }
        )

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(Color(0xFFF0F6FF)))
                .cornerRadius(20.dp)
                .clickable(openActivity)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            contentAlignment = Alignment.TopStart
        ) {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.Top,
                horizontalAlignment = Alignment.Start
            ) {
                // ── Header ───────────────────────────────────────────────────
                val header = if (date.isNotEmpty()) "🏃  Активности · $date"
                             else "🏃  Активности сегодня"
                Text(
                    text  = header,
                    style = TextStyle(
                        color      = ColorProvider(Color(0xFF8E8E93)),
                        fontSize   = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
                Spacer(GlanceModifier.height(8.dp))

                // ── My activities ────────────────────────────────────────────
                ActivityRow(name = myName, count = myCount, types = myTypes, icons = myIcons, isMe = true)

                Spacer(GlanceModifier.height(6.dp))

                // ── Partner activities ────────────────────────────────────────
                ActivityRow(name = ptName, count = ptCount, types = ptTypes, icons = ptIcons, isMe = false)
            }
        }
    }

    @Composable
    private fun ActivityRow(name: String, count: Int, types: List<String>, icons: List<String>, isMe: Boolean) {
        val rowBg   = if (isMe) Color(0x1A1E90FF) else Color(0x1A8E8E93)
        val nameClr = if (isMe) Color(0xFF1E90FF) else Color(0xFF636366)
        val cntClr  = if (isMe) Color(0xFF1E90FF) else Color(0xFF48484A)

        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(ColorProvider(rowBg))
                .cornerRadius(10.dp)
                .padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text     = name,
                style    = TextStyle(
                    color      = ColorProvider(nameClr),
                    fontSize   = 10.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = GlanceModifier.width(58.dp)
            )
            if (count == 0) {
                Text(
                    text  = "—",
                    style = TextStyle(
                        color    = ColorProvider(Color(0xFFAEAEB2)),
                        fontSize = 16.sp
                    )
                )
            } else {
                Text(
                    text  = count.toString(),
                    style = TextStyle(
                        color      = ColorProvider(cntClr),
                        fontSize   = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(GlanceModifier.width(4.dp))
                Text(
                    text  = "акт.",
                    style = TextStyle(
                        color    = ColorProvider(Color(0xFF8E8E93)),
                        fontSize = 10.sp
                    )
                )
                if (types.isNotEmpty()) {
                    Spacer(GlanceModifier.width(6.dp))
                    val iconList = if (icons.isNotEmpty()) icons else types.map { activityEmoji(it) }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        iconList.forEachIndexed { idx, iconStr ->
                            if (idx > 0) Spacer(GlanceModifier.width(3.dp))
                            val bmp = loadWidgetIconBitmap(iconStr)
                            if (bmp != null) {
                                Image(
                                    provider = ImageProvider(bmp),
                                    contentDescription = null,
                                    modifier = GlanceModifier.size(20.dp)
                                )
                            } else {
                                Text(iconStr, style = TextStyle(fontSize = 14.sp))
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun activityEmoji(type: String) = when (type.lowercase()) {
        "work"     -> "💼"
        "computer" -> "💻"
        "sport"    -> "🏃"
        "food"     -> "🍽️"
        "walk"     -> "🚶"
        "sleep"    -> "😴"
        "reading"  -> "📚"
        "social"   -> "👥"
        "relax"    -> "🧘"
        else       -> "✨"
    }
}

class ActivityDayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = ActivityDayWidget()

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        WidgetSyncWorker.runImmediately(context)
    }
}
