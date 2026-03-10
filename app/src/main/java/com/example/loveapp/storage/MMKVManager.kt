package com.example.loveapp.storage

import android.content.Context
import android.util.Log
import com.tencent.mmkv.MMKV

/**
 * Wrapper around Tencent MMKV – a fast, persistent key-value store backed by mmap.
 * Call [init] once from Application.onCreate().
 */
object MMKVManager {

    private const val TAG = "MMKVManager"

    @Volatile
    private var initialized = false

    /** Initialise MMKV. Must be called before any [defaultInstance] usage. */
    fun init(context: Context) {
        if (initialized) return
        val rootDir = MMKV.initialize(context.applicationContext)
        initialized = true
        Log.d(TAG, "MMKV initialised, root=$rootDir")
    }

    /** Default MMKV instance (general purpose). */
    fun defaultInstance(): MMKV {
        check(initialized) { "MMKVManager.init() not called" }
        return MMKV.defaultMMKV()
    }

    /** Named MMKV instance for scoped storage. */
    fun getInstance(name: String): MMKV {
        check(initialized) { "MMKVManager.init() not called" }
        return MMKV.mmkvWithID(name)
    }
}
