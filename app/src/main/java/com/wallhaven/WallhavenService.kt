package com.wallhaven

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.wallhaven.Functions.Companion.t

const val reviveTime = 10
var reviver: Runnable = object : Runnable {
    override fun run() {
        if (allSet)
            if (!handler.hasCallbacks(refresher)) {
                handler.post(refresher)
            }
        t("Reviving")
        handler.postDelayed(this, (reviveTime * 1000).toLong())
    }
}

class WallhavenService : Service() {
    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): WallhavenService = this@WallhavenService
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        handler.post(reviver)
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }
}