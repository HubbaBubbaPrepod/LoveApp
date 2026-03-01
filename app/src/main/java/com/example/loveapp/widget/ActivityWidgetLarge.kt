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
 * Large 4×3 activity widget – shows each person's full activity breakdown:
 * large count number, all type emojis, and Russian labels beneath.
 */
class ActivityWidgetLarge : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { LargeContent(context) }
    }

    @Composable
    private fun LargeContent(context: Context) {
        val prefs      = currentState<Preferences>()
        val myCount    = prefs[KEY_ACT_MY_COUNT] ?: 0
        val myTypesRaw = prefs[KEY_ACT_MY_TYPES] ?: ""
        val myIconsRaw = prefs[KEY_ACT_MY_ICONS] ?: ""
        val myName     = (prefs[KEY_ACT_MY_NAME]?.takeIf { it.isNotBlank() } ?: "Я")
        val ptCount    = prefs[KEY_ACT_PT_COUNT] ?: 0
        val ptTypesRaw = prefs[KEY_ACT_PT_TYPES] ?: ""
        val ptIconsRaw = prefs[KEY_ACT_PT_ICONS] ?: ""
        val ptName     = (prefs[KEY_ACT_PT_NAME]?.takeIf { it.isNotBlank() } ?: "Партнёр")
        val date       = prefs[KEY_ACT_DATE]     ?: ""

        val myTypes = parseTypes(myTypesRaw)
        val myIcons = parseTypes(myIconsRaw)
        val ptTypes = parseTypes(ptTypesRaw)
        val ptIcons = parseTypes(ptIconsRaw)

        val open = actionStartActivity(
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("destination", "activity_feed")
            }
        )

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(Color(0xFFF2F7FF)))
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
                        text  = "🏃  Активности за день",
                        style = TextStyle(
                            color      = ColorProvider(Color(0xFF1E90FF)),
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

                // ── My card ────────────────────────────────────────────────────
                LargeActivityCard(
                    name      = myName,
                    count     = myCount,
                    types     = myTypes,
                    icons     = myIcons,
                    bgColor   = Color(0x1C1E90FF),
                    nameColor = Color(0xFF1E90FF),
                    cntColor  = Color(0xFF1E90FF)
                )

                Spacer(GlanceModifier.height(10.dp))

                // ── Partner card ──────────────────────────────────────────────
                LargeActivityCard(
                    name      = ptName,
                    count     = ptCount,
                    types     = ptTypes,
                    icons     = ptIcons,
                    bgColor   = Color(0x128E8E93),
                    nameColor = Color(0xFF636366),
                    cntColor  = Color(0xFF48484A)
                )
            }
        }
    }

    @Composable
    private fun LargeActivityCard(
        name: String,
        count: Int,
        types: List<String>,
        icons: List<String>,
        bgColor: Color,
        nameColor: Color,
        cntColor: Color
    ) {
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(ColorProvider(bgColor))
                .cornerRadius(14.dp)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Column(horizontalAlignment = Alignment.Start) {
                // Name + big count in a row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text  = if (count == 0) "0" else count.toString(),
                        style = TextStyle(
                            color      = ColorProvider(cntColor),
                            fontSize   = 34.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(GlanceModifier.width(10.dp))
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
                            text  = countLabel(count),
                            style = TextStyle(
                                color    = ColorProvider(Color(0xFF8E8E93)),
                                fontSize = 11.sp
                            )
                        )
                    }
                }
                // Type emojis + labels
                if (types.isNotEmpty() && count > 0) {
                    Spacer(GlanceModifier.height(6.dp))
                    val iconList = if (icons.isNotEmpty()) icons else types.map { activityEmoji(it) }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        iconList.forEachIndexed { idx, iconStr ->
                            if (idx > 0) Spacer(GlanceModifier.width(4.dp))
                            val bmp = loadWidgetIconBitmap(iconStr)
                            if (bmp != null) {
                                Image(
                                    provider = ImageProvider(bmp),
                                    contentDescription = null,
                                    modifier = GlanceModifier.size(26.dp)
                                )
                            } else {
                                Text(iconStr, style = TextStyle(fontSize = 18.sp))
                            }
                        }
                    }
                    Spacer(GlanceModifier.height(2.dp))
                    val labels = types.joinToString(" · ") { activityLabel(it) }
                    Text(
                        text  = if (labels.length > 44) labels.take(41) + "…" else labels,
                        style = TextStyle(
                            color    = ColorProvider(Color(0xFF636366)),
                            fontSize = 10.sp
                        )
                    )
                } else if (count == 0) {
                    Spacer(GlanceModifier.height(4.dp))
                    Text(
                        text  = "Нет активностей",
                        style = TextStyle(
                            color    = ColorProvider(Color(0xFFAEAEB2)),
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }
    }

    private fun parseTypes(raw: String): List<String> =
        if (raw.isBlank()) emptyList()
        else raw.split(",").filter { it.isNotBlank() }.take(5)

    private fun countLabel(n: Int) = when {
        n == 0                    -> "ничего"
        n % 100 in 11..14         -> "активностей"
        n % 10 == 1               -> "активность"
        n % 10 in 2..4            -> "активности"
        else                      -> "активностей"
    }

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

    private fun activityLabel(type: String) = when (type.lowercase()) {
        "work"     -> "Работа"
        "computer" -> "Компьютер"
        "sport"    -> "Спорт"
        "food"     -> "Еда"
        "walk"     -> "Прогулка"
        "sleep"    -> "Сон"
        "reading"  -> "Чтение"
        "social"   -> "Общение"
        "relax"    -> "Отдых"
        else       -> type.let { if (it.length > 14) it.take(13) + "…" else it }
    }
}

class ActivityWidgetLargeReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = ActivityWidgetLarge()

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        WidgetSyncWorker.runImmediately(context)
    }
}
