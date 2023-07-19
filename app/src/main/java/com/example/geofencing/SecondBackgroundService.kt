package com.example.geofencing

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class SecondBackgroundService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var audioManager: AudioManager
    private var gotOutOfCampus = false
    private val radiusToCheck = 100.0 // meter

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val listOfIntents = arrayOfNulls<Intent>(1)
        listOfIntents[0] = notificationIntent
        val pendingIntent = PendingIntent.getActivities(
            this,
            0,
            listOfIntents,
            PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, App.CHANNEL_ID)
            .setContentTitle("Background service active")
            .setContentText("Tap to return")
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(1, notification)
        requestLocationUpdates()
        return START_STICKY
    }

    private fun requestLocationUpdates() {
        val locationRequest = LocationRequest.create()
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            .setInterval(5000) // 5 Seconds
            .setFastestInterval(1000) // 1 second

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)

                val location = locationResult.lastLocation
                val latitude = location?.latitude
                val longitude = location?.longitude

                // if the location inside the defined fence, do the following
                if (isInCampus(latitude!!, longitude!!)) {
                    gotOutOfCampus = true

                    try {
                        // phone is in the campus, switch to silence mode,
                        // if not already try to put the phone to vibrate mode
                        if (audioManager.ringerMode != AudioManager.RINGER_MODE_VIBRATE)
                            audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                    } catch (e: Exception) {
                        Log.d("Vibrate mode", e.message.toString())
                    }

                    try {
                        // try to put the phone to silent mode
                        if (audioManager.ringerMode != AudioManager.RINGER_MODE_SILENT)
                            audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                    } catch (e: Exception) {
                        Log.d("Silent mode", e.message.toString())
                    }

                } else {
                    // put phone into general mode, if not already (only once)
                    /*
                    * The user can keep either on silent or general,
                    * but for the first time, the user gets out of campus,
                    * the phone is set to general mode!
                    * */
                    if (gotOutOfCampus && audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL) {
                        audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                        gotOutOfCampus = false
                    }
                }
            }
        }
    }

    private fun getDistance(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val R = 6371 // radius of the earth

        val latDistance = Math.toRadians(abs(lat2 - lat1))
        val lonDistance = Math.toRadians(abs(lon2 - lon1))

        val a = (sin(latDistance / 2) * sin(latDistance / 2)
                + (cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2))
                * sin(lonDistance / 2) * sin(lonDistance / 2)))

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        var distance = R * c * 1000 // distance in meter
        distance = distance.pow(2.0)
        return sqrt(distance)

    }

    private fun isInCampus(latitude: Double, longitude: Double): Boolean {
        val lat = 21.1279366//22.9611167
        val lon = 72.8597302 // 88.4335215 // 21.127936652631288, 72.85973029155082

        // radius up to 200m is checked
        return getDistance(lat, lon, latitude, longitude) <= radiusToCheck
    }
}
