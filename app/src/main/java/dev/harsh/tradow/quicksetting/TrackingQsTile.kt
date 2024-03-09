package dev.harsh.tradow.quicksetting

import android.content.Intent
import android.service.quicksettings.TileService
import dev.harsh.tradow.service.BackgroundService
import dev.harsh.tradow.util.showShortToast

class TrackingQsTile : TileService() {

    override fun onClick() {
        super.onClick()
        startService(Intent(applicationContext, BackgroundService::class.java))
        applicationContext.showShortToast("Tradow is active")
    }

    override fun onDestroy() {
        super.onDestroy()
        stopService(Intent(applicationContext, BackgroundService::class.java))
        applicationContext.showShortToast("Tradow is inactive")
    }
}
