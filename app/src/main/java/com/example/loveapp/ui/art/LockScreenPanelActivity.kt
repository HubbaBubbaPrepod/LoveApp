package com.example.loveapp.ui.art

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

/**
 * Transparent full-screen Activity displayed **over the lock screen**.
 *
 * Uses [setShowWhenLocked] (API 27) which is the only reliable way on modern Android
 * to place UI above the keyguard from a non-system app.
 *
 * The Activity covers the full screen but only the bottom panel is visible;
 * everything else is transparent, so the lock screen clock and wallpaper show through.
 *
 * Lifecycle:
 * - Started by [LockScreenService] on ACTION_SCREEN_ON when keyguard is locked.
 * - Finishes itself on ACTION_USER_PRESENT (device unlocked).
 * - Finishes itself on [ACTION_CLOSE] broadcast (screen off / service stopped).
 */
class LockScreenPanelActivity : ComponentActivity() {

    companion object {
        const val ACTION_CLOSE = "com.example.loveapp.LOCK_PANEL_CLOSE"
    }

    private val closeReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_USER_PRESENT -> finish()  // fully unlocked
                ACTION_CLOSE               -> finish()  // service requested close
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ── Show above the keyguard without turning the screen on ──────────
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(false)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        }

        // Register close triggers
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(ACTION_CLOSE)
        }
        ContextCompat.registerReceiver(
            this, closeReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED
        )

        val canvasId = intent.getIntExtra("canvas_id", -1)

        setContent {
            LockScreenPanelOverlay(
                onDraw = {
                    startActivity(Intent(this, LockScreenDrawingActivity::class.java).apply {
                        putExtra("canvas_id", canvasId)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    })
                    finish()
                },
                onDismiss = { finish() }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(closeReceiver) }
        super.onDestroy()
    }
}

// ── UI ─────────────────────────────────────────────────────────────────────────

@Composable
private fun LockScreenPanelOverlay(
    onDraw: () -> Unit,
    onDismiss: () -> Unit
) {
    val gradient = Brush.horizontalGradient(listOf(Color(0xFFE91E63), Color(0xFF9C27B0)))
    val panelBg  = Color(0xD7_0F0C16.toInt())     // ~85% opaque dark

    Box(
        modifier = Modifier
            .fillMaxSize()
            // tap anywhere outside the card → dismiss
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
    ) {
        // ── Bottom panel card ────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(panelBg)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDraw
                )
        ) {
            // Pink→purple top accent strip
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .align(Alignment.TopCenter)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(gradient)
            )

            // Content row
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon circle
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(gradient),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "\uD83C\uDFA8", fontSize = 20.sp) // 🎨
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 14.dp)
                ) {
                    Text(
                        text       = "Совместный холст",
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color      = Color.White
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text     = "Нажмите, чтобы рисовать вместе",
                        fontSize = 12.sp,
                        color    = Color.White.copy(alpha = 0.65f)
                    )
                }

                Text(
                    text       = "\u203A",  // ›
                    fontSize   = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color.White.copy(alpha = 0.5f)
                )
            }
        }
    }
}
