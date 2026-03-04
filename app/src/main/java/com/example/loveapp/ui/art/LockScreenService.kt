package com.example.loveapp.ui.art

import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.loveapp.R
import dagger.hilt.android.AndroidEntryPoint

/**
 * Foreground service that overlays a **lock-screen widget panel** at the bottom
 * of the screen using TYPE_APPLICATION_OVERLAY.
 *
 * The panel is sized to occupy only the bottom ~180dp, so the lock screen clock
 * and notifications above it are completely untouched (FLAG_NOT_TOUCH_MODAL).
 *
 * Auto-shows on ACTION_SCREEN_OFF (device locked).
 * Auto-hides on ACTION_USER_PRESENT (device unlocked).
 *
 * Requires: android.permission.SYSTEM_ALERT_WINDOW
 */
@AndroidEntryPoint
class LockScreenService : Service() {

    companion object {
        private const val CHANNEL_ID      = "lock_screen_canvas_silent"
        private const val NOTIFICATION_ID = 2001
        private const val EXTRA_CANVAS_ID = "canvas_id"
        /** Height of the bottom widget panel in dp */
        private const val PANEL_HEIGHT_DP = 120

        fun start(context: Context, canvasId: Int = -1) {
            val intent = Intent(context, LockScreenService::class.java)
                .putExtra(EXTRA_CANVAS_ID, canvasId)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, LockScreenService::class.java))
        }
    }

    private var windowManager: WindowManager? = null
    private var panelView: View?              = null
    private var storedCanvasId: Int           = -1

    // ── Screen lock / unlock receiver ────────────────────────────────────────

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF    -> showPanel()
                Intent.ACTION_USER_PRESENT  -> hidePanel()   // fully unlocked
                Intent.ACTION_SCREEN_ON     -> {
                    // Screen on but still locked → keep panel visible
                    val km = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
                    if (!km.isKeyguardLocked) hidePanel()
                }
            }
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildSilentNotification())
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        registerReceiver(screenReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        })
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        storedCanvasId = intent?.getIntExtra(EXTRA_CANVAS_ID, -1) ?: -1
        // If screen is already locked when service starts — show panel immediately
        val km = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        if (km.isKeyguardLocked) showPanel() else hidePanel()
        return START_STICKY
    }

    override fun onDestroy() {
        unregisterReceiver(screenReceiver)
        hidePanel()
        windowManager = null
        super.onDestroy()
    }

    // ── Panel visibility ──────────────────────────────────────────────────────

    private fun showPanel() {
        if (panelView != null) return
        panelView = buildPanelView()
        try {
            windowManager?.addView(panelView, buildWindowParams())
        } catch (e: Exception) {
            panelView = null
        }
    }

    private fun hidePanel() {
        panelView?.let {
            try { windowManager?.removeView(it) } catch (_: Exception) {}
        }
        panelView = null
    }

    // ── Panel view (programmatic) ─────────────────────────────────────────────

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density + 0.5f).toInt()

    private fun buildPanelView(): View {
        val corner = dpToPx(20).toFloat()

        // Root padding wrapper
        val root = FrameLayout(this).apply {
            setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(16))
        }

        // Card background — dark glass
        val cardBg = GradientDrawable().apply {
            shape       = GradientDrawable.RECTANGLE
            setColor(Color.argb(215, 15, 12, 22))
            cornerRadii = floatArrayOf(corner, corner, corner, corner,
                                       corner, corner, corner, corner)
        }

        // Top accent strip: pink → purple, only rounded at top
        val accentBg = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(0xFFE91E63.toInt(), 0xFF9C27B0.toInt())
        ).apply {
            shape       = GradientDrawable.RECTANGLE
            cornerRadii = floatArrayOf(corner, corner, corner, corner, 0f, 0f, 0f, 0f)
        }

        val card = FrameLayout(this).apply {
            background = cardBg
            elevation  = dpToPx(8).toFloat()
        }

        // Accent line (5dp tall)
        val accent = View(this).apply {
            background   = accentBg
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, dpToPx(5)
            )
        }

        // Content row (below accent)
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity     = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ).apply { topMargin = dpToPx(5) }
        }

        // Circle icon: pink→purple gradient
        val iconCircleBg = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(0xFFE91E63.toInt(), 0xFF9C27B0.toInt())
        ).apply {
            shape        = GradientDrawable.OVAL
            cornerRadius = dpToPx(24).toFloat()
        }
        val iconSize = dpToPx(48)
        val iconCircle = FrameLayout(this).apply {
            background   = iconCircleBg
            layoutParams = LinearLayout.LayoutParams(iconSize, iconSize)
        }
        val iconEmoji = TextView(this).apply {
            text     = "\uD83C\uDFA8"   // 🎨
            textSize = 20f
            gravity  = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        iconCircle.addView(iconEmoji)

        // Text column
        val textCol = LinearLayout(this).apply {
            orientation  = LinearLayout.VERTICAL
            gravity      = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                .apply { marginStart = dpToPx(14) }
        }
        val title = TextView(this).apply {
            text      = "Совместный холст"
            textSize  = 15f
            typeface  = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        }
        val subtitle = TextView(this).apply {
            text      = "Нажмите, чтобы рисовать"
            textSize  = 12f
            setTextColor(Color.argb(170, 255, 255, 255))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dpToPx(3) }
        }
        textCol.addView(title)
        textCol.addView(subtitle)

        // Arrow chevron
        val arrow = TextView(this).apply {
            text      = "›"
            textSize  = 30f
            typeface  = Typeface.DEFAULT_BOLD
            setTextColor(Color.argb(140, 255, 255, 255))
            gravity   = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { marginStart = dpToPx(8) }
        }

        row.addView(iconCircle)
        row.addView(textCol)
        row.addView(arrow)

        card.addView(accent)
        card.addView(row)
        card.setOnClickListener { launchDrawing(storedCanvasId) }

        root.addView(card)
        return root
    }

    private fun launchDrawing(canvasId: Int) {
        startActivity(Intent(this, LockScreenDrawingActivity::class.java).apply {
            putExtra("canvas_id", canvasId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        })
    }

    @Suppress("DEPRECATION")
    private fun buildWindowParams() = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        dpToPx(PANEL_HEIGHT_DP),
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or    // clock area stays interactive
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,     // visible over keyguard
        PixelFormat.TRANSLUCENT
    ).apply { gravity = Gravity.BOTTOM }

    // ── Silent foreground notification ────────────────────────────────────────

    private fun buildSilentNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Рисование активно")
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setShowWhen(false)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .build()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel(
                CHANNEL_ID,
                "Рисование (системный сервис)",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
                setSound(null, null)
            }.also {
                getSystemService(NotificationManager::class.java).createNotificationChannel(it)
            }
        }
    }
}
