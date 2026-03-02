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
import com.example.loveapp.widget.WidgetUpdater.Companion.KEY_ART_MY_THUMB_PATH
import com.example.loveapp.widget.WidgetUpdater.Companion.KEY_ART_MY_CANVAS_TITLE

/**
 * Large (4×4) art widget – shows partner's latest drawing prominently,
 * with a smaller thumbnail of the user's own latest canvas below.
 */
class ArtWidgetLarge : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { LargeContent(context) }
    }

    @Composable
    private fun LargeContent(context: Context) {
        val prefs          = currentState<Preferences>()
        val thumbPath      = prefs[KEY_ART_THUMB_PATH]      ?: ""
        val title          = prefs[KEY_ART_CANVAS_TITLE]    ?: "Художества"
        val canvasId       = prefs[KEY_ART_CANVAS_ID]       ?: -1
        val partnerName    = prefs[KEY_ART_PARTNER_NAME]    ?: "Партнёр"
        val updatedAt      = prefs[KEY_ART_UPDATED_AT]      ?: ""
        val myThumbPath    = prefs[KEY_ART_MY_THUMB_PATH]   ?: ""
        val myTitle        = prefs[KEY_ART_MY_CANVAS_TITLE] ?: ""

        val partnerBitmap  = loadWidgetIconBitmap(thumbPath)
        val myBitmap       = loadWidgetIconBitmap(myThumbPath)

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
        ) {
            // ── Header ──────────────────────────────────────────────────────
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(ColorProvider(Color(0xFF7B1FA2)))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    "🎨 Художества",
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            // ── Partner's drawing (dominant) ─────────────────────────────
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .defaultWeight(),
                contentAlignment = Alignment.BottomStart
            ) {
                if (partnerBitmap != null) {
                    Image(
                        provider = ImageProvider(partnerBitmap),
                        contentDescription = title,
                        modifier = GlanceModifier.fillMaxSize()
                    )
                } else {
                    Box(
                        GlanceModifier.fillMaxSize().background(ColorProvider(Color(0xFFE1BEE7))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✏️", style = TextStyle(fontSize = 48.sp))
                    }
                }

                // Name + title overlay
                Box(
                    GlanceModifier
                        .fillMaxWidth()
                        .background(ColorProvider(Color(0xAA000000)))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Column {
                        Text(
                            title,
                            style = TextStyle(color = ColorProvider(Color.White), fontSize = 13.sp, fontWeight = FontWeight.Bold),
                            maxLines = 1
                        )
                        if (updatedAt.isNotBlank()) {
                            Text(
                                "$partnerName · $updatedAt",
                                style = TextStyle(color = ColorProvider(Color(0xCCFFFFFF)), fontSize = 11.sp),
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // ── User's own latest canvas (mini strip) ────────────────────
            if (myBitmap != null || myTitle.isNotBlank()) {
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .background(ColorProvider(Color(0xDD7B1FA2)))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (myBitmap != null) {
                        Image(
                            provider = ImageProvider(myBitmap),
                            contentDescription = myTitle,
                            modifier = GlanceModifier.size(40.dp).cornerRadius(8.dp)
                        )
                        Spacer(GlanceModifier.width(8.dp))
                    }
                    Text(
                        text = if (myTitle.isNotBlank()) "Мой: $myTitle" else "Мои работы",
                        style = TextStyle(color = ColorProvider(Color.White), fontSize = 11.sp),
                        maxLines = 1,
                        modifier = GlanceModifier.defaultWeight()
                    )
                }
            }
        }
    }
}

class ArtWidgetLargeReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = ArtWidgetLarge()
}
