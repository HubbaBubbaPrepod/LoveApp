package com.example.loveapp.widget

import android.content.Context
import android.content.Intent
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
import androidx.glance.layout.*
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.loveapp.MainActivity
import com.example.loveapp.widget.WidgetUpdater.Companion.KEY_ART_CANVAS_ID
import com.example.loveapp.widget.WidgetUpdater.Companion.KEY_ART_CANVAS_TITLE
import com.example.loveapp.widget.WidgetUpdater.Companion.KEY_ART_THUMB_PATH

/**
 * Small (2×2) art widget – shows partner's latest canvas thumbnail.
 */
class ArtWidgetSmall : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { SmallContent(context) }
    }

    @Composable
    private fun SmallContent(context: Context) {
        val prefs     = currentState<Preferences>()
        val thumbPath = prefs[KEY_ART_THUMB_PATH] ?: ""
        val title     = prefs[KEY_ART_CANVAS_TITLE] ?: "Художества"
        val canvasId  = prefs[KEY_ART_CANVAS_ID]  ?: -1
        val bitmap    = loadWidgetIconBitmap(thumbPath)

        val open = actionStartActivity(
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("destination", "art_gallery")
                if (canvasId > 0) putExtra("canvas_id", canvasId)
            }
        )

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(Color(0xFFF3E5F5)))
                .cornerRadius(16.dp)
                .clickable(open),
            contentAlignment = Alignment.BottomCenter
        ) {
            if (bitmap != null) {
                Image(
                    provider = ImageProvider(bitmap),
                    contentDescription = title,
                    modifier = GlanceModifier.fillMaxSize().cornerRadius(16.dp)
                )
            } else {
                // Placeholder
                Box(
                    modifier = GlanceModifier.fillMaxSize().background(ColorProvider(Color(0xFFE1BEE7))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✏️",
                        style = TextStyle(fontSize = 36.sp)
                    )
                }
            }

            // Title overlay
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(ColorProvider(Color(0x99000000)))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = title,
                    style = TextStyle(
                        color     = ColorProvider(Color.White),
                        fontSize  = 12.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1
                )
            }
        }
    }
}

class ArtWidgetSmallReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = ArtWidgetSmall()
}
