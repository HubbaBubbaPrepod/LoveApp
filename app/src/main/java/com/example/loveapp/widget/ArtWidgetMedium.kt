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
import com.example.loveapp.widget.WidgetUpdater.Companion.KEY_ART_PARTNER_NAME
import com.example.loveapp.widget.WidgetUpdater.Companion.KEY_ART_THUMB_PATH
import com.example.loveapp.widget.WidgetUpdater.Companion.KEY_ART_UPDATED_AT

/**
 * Medium (3×3) art widget – thumbnail + partner name + update time.
 */
class ArtWidgetMedium : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { MediumContent(context) }
    }

    @Composable
    private fun MediumContent(context: Context) {
        val prefs       = currentState<Preferences>()
        val thumbPath   = prefs[KEY_ART_THUMB_PATH]    ?: ""
        val title       = prefs[KEY_ART_CANVAS_TITLE]  ?: "Художества"
        val canvasId    = prefs[KEY_ART_CANVAS_ID]     ?: -1
        val partnerName = prefs[KEY_ART_PARTNER_NAME]  ?: "Партнёр"
        val updatedAt   = prefs[KEY_ART_UPDATED_AT]    ?: ""
        val bitmap      = loadWidgetIconBitmap(thumbPath)

        val open = actionStartActivity(
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("destination", "art_gallery")
                if (canvasId > 0) putExtra("canvas_id", canvasId)
            }
        )

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(Color(0xFFF3E5F5)))
                .cornerRadius(16.dp)
                .clickable(open)
                .padding(0.dp)
        ) {
            // Thumbnail (takes most of the space)
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .defaultWeight(),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        provider = ImageProvider(bitmap),
                        contentDescription = title,
                        modifier = GlanceModifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .background(ColorProvider(Color(0xFFE1BEE7))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✏️", style = TextStyle(fontSize = 40.sp))
                    }
                }
            }

            // Info row
            Column(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(ColorProvider(Color(0xCC7B1FA2)))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = title,
                    style = TextStyle(color = ColorProvider(Color.White), fontSize = 13.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1
                )
                Spacer(GlanceModifier.height(2.dp))
                Text(
                    text = if (updatedAt.isNotBlank()) "✏️ $partnerName · $updatedAt" else "✏️ $partnerName",
                    style = TextStyle(color = ColorProvider(Color(0xCCFFFFFF)), fontSize = 11.sp),
                    maxLines = 1
                )
            }
        }
    }
}

class ArtWidgetMediumReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = ArtWidgetMedium()
}
