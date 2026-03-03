package com.example.loveapp.ui.art

import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

data class DrawPoint(val x: Float, val y: Float)

data class DrawActionEvent(
    val canvasId: Int,
    val action: String,   // "start" | "move" | "end" | "clear"
    val points: List<DrawPoint>,
    val color: String,
    val strokeWidth: Float,
    val senderId: Int
)

@Singleton
class ArtSocketManager @Inject constructor() {

    private var socket: Socket? = null

    private val _drawActions = MutableSharedFlow<DrawActionEvent>(extraBufferCapacity = 256)
    val drawActions: SharedFlow<DrawActionEvent> = _drawActions

    fun connect(token: String) {
        if (socket?.connected() == true) return
        try {
            val opts = IO.Options().apply {
                extraHeaders = mapOf("Authorization" to listOf("Bearer $token"))
            }
            socket = IO.socket("https://love-app.ru?token=$token", opts)
            socket?.connect()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun joinCanvas(canvasId: Int) {
        socket?.off("draw-action")
        socket?.emit("join-canvas", canvasId)
        socket?.on("draw-action") { args ->
            val data = args.getOrNull(0) as? JSONObject ?: return@on
            val event = DrawActionEvent(
                canvasId    = data.optInt("canvasId"),
                action      = data.optString("action", "move"),
                points      = parsePoints(data.optJSONArray("points")),
                color       = data.optString("color", "#000000"),
                strokeWidth = data.optDouble("strokeWidth", 5.0).toFloat(),
                senderId    = data.optInt("senderId")
            )
            _drawActions.tryEmit(event)
        }
    }

    fun leaveCanvas(canvasId: Int) {
        socket?.off("draw-action")
        socket?.emit("leave-canvas", canvasId)
    }

    fun sendDrawAction(
        canvasId: Int,
        action: String,
        points: List<DrawPoint>,
        color: String,
        strokeWidth: Float
    ) {
        val json = JSONObject().apply {
            put("canvasId", canvasId)
            put("action", action)
            put("points", JSONArray().also { arr ->
                points.forEach { p ->
                    arr.put(JSONObject().apply { put("x", p.x); put("y", p.y) })
                }
            })
            put("color", color)
            put("strokeWidth", strokeWidth)
        }
        socket?.emit("draw-action", json)
    }

    fun sendClear(canvasId: Int) {
        val json = JSONObject().apply {
            put("canvasId", canvasId)
            put("action", "clear")
            put("points", JSONArray())
            put("color", "#FFFFFF")
            put("strokeWidth", 1.0)
        }
        socket?.emit("draw-action", json)
    }

    fun disconnect() {
        socket?.off()
        socket?.disconnect()
        socket = null
    }

    fun isConnected() = socket?.connected() == true

    private fun parsePoints(arr: JSONArray?): List<DrawPoint> {
        if (arr == null) return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            val obj = arr.optJSONObject(i) ?: return@mapNotNull null
            DrawPoint(obj.optDouble("x").toFloat(), obj.optDouble("y").toFloat())
        }
    }
}
