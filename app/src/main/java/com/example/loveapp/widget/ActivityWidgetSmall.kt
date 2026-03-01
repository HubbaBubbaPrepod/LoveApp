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
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.loveapp.MainActivity
import com.example.loveapp.widget.WidgetUpdater.Companion.KEY_ACT_MY_COUNT
import com.example.loveapp.widget.WidgetUpdater.Companion.KEY_ACT_MY_ICONS
import com.example.loveapp.widget.WidgetUpdater.Companion.KEY_ACT_MY_NAME
import com.example.loveapp.widget.WidgetUpdater.Companion.KEY_ACT_MY_TYPES
import com.example.loveapp.widget.WidgetUpdater.Companion.KEY_ACT_PT_COUNT
import com.example.loveapp.widget.WidgetUpdater.Companion.KEY_ACT_PT_ICONS
import com.example.loveapp.widget.WidgetUpdater.Companion.KEY_ACT_PT_NAME
import com.example.loveapp.widget.WidgetUpdater.Companion.KEY_ACT_PT_TYPES

/**
 * Compact 2×1 activity widget – shows both users' activity count + emojis side by side.
 */
class ActivityWidgetSmall : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { SmallContent(context) }
    }

    @Composable
    private fun SmallContent(context: Context) {
        val prefs      = currentState<Preferences>()
        val myCount    = prefs[KEY_ACT_MY_COUNT] ?: 0
        val myTypesRaw = prefs[KEY_ACT_MY_TYPES] ?: ""
        val myIconsRaw = prefs[KEY_ACT_MY_ICONS] ?: ""
        val myName     = (prefs[KEY_ACT_MY_NAME]?.takeIf { it.isNotBlank() } ?: "Я").take(6)
        val ptCount    = prefs[KEY_ACT_PT_COUNT] ?: 0
        val ptTypesRaw = prefs[KEY_ACT_PT_TYPES] ?: ""
        val ptIconsRaw = prefs[KEY_ACT_PT_ICONS] ?: ""
        val ptName     = (prefs[KEY_ACT_PT_NAME]?.takeIf { it.isNotBlank() } ?: "Партн.").take(6)

        val myIcons = parseIconList(myIconsRaw, myTypesRaw, max = 3)
        val ptIcons = parseIconList(ptIconsRaw, ptTypesRaw, max = 3)

        val open = actionStartActivity(
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("destination", "activity_feed")
            }
        )

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(Color(0xFFEEF4FF)))
                .cornerRadius(16.dp)
                .clickable(open)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ActivityBadge(
                    name      = myName,
                    count     = myCount,
                    icons     = myIcons,
                    bgColor   = Color(0x331E90FF),
                    nameColor = Color(0xFF1E90FF),
                    cntColor  = Color(0xFF1E90FF),
                    modifier  = GlanceModifier.defaultWeight()
                )
                Spacer(GlanceModifier.width(6.dp))
                ActivityBadge(
                    name      = ptName,
                    count     = ptCount,
                    icons     = ptIcons,
                    bgColor   = Color(0x228E8E93),
                    nameColor = Color(0xFF636366),
                    cntColor  = Color(0xFF48484A),
                    modifier  = GlanceModifier.defaultWeight()
                )
            }
        }
    }

    @Composable
    private fun ActivityBadge(
        name: String,
        count: Int,
        icons: List<String>,
        bgColor: Color,
        nameColor: Color,
        cntColor: Color,
        modifier: GlanceModifier
    ) {
        Box(
            modifier = modifier
                .background(ColorProvider(bgColor))
                .cornerRadius(10.dp)
                .padding(horizontal = 6.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text  = if (count == 0) "—" else count.toString(),
                    style = TextStyle(
                        color      = ColorProvider(cntColor),
                        fontSize   = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                if (icons.isNotEmpty() && count > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        icons.forEachIndexed { idx, iconStr ->
                            if (idx > 0) Spacer(GlanceModifier.width(2.dp))
                            val bmp = loadWidgetIconBitmap(iconStr)
                            if (bmp != null) {
                                Image(
                                    provider = ImageProvider(bmp),
                                    contentDescription = null,
                                    modifier = GlanceModifier.size(18.dp)
                                )
                            } else {
                                Text(iconStr, style = TextStyle(fontSize = 12.sp))
                            }
                        }
                    }
                }
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

    private fun parseIconList(iconsRaw: String, typesRaw: String, max: Int): List<String> {
        val src = if (iconsRaw.isNotBlank()) iconsRaw else typesRaw
        if (src.isBlank()) return emptyList()
        return src.split(",").filter { it.isNotBlank() }.take(max)
    }
}

class ActivityWidgetSmallReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = ActivityWidgetSmall()

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        WidgetSyncWorker.runImmediately(context)
    }
}
