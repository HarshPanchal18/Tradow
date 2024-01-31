package com.example.geofencing.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ServiceRestarterBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        context?.startService(Intent(context, BackgroundService::class.java))
    }
}
