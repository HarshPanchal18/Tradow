package com.example.geofencing

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startForegroundService
import com.example.geofencing.ui.theme.GeofencingTheme
import java.lang.Double.doubleToRawLongBits
import java.lang.Double.longBitsToDouble

@Suppress("DEPRECATION")
class MainActivity : ComponentActivity() {
    private lateinit var sharedPref: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPref = getSharedPreferences("", Context.MODE_PRIVATE)
        setContent {
            GeofencingTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HomeLayout()
                }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Service Started Successfully!", Toast.LENGTH_SHORT).show()
            startForegroundService(this, Intent(this, BackgroundService::class.java))
        } else {
            // show an toast message asking for the permission
            Toast.makeText(
                this,
                "Do I really need to tell, why you should give me location access??",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    @Composable
    fun HomeLayout() {
        val isServiceActivated = false
        val context = LocalContext.current
        val prevLat = sharedPref.getDouble("latitude", 0.0).toString()
        val prevLon = sharedPref.getDouble("longitude", 0.0).toString()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            //verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val latitude by remember { mutableStateOf(prevLat) }
            val longitude by remember { mutableStateOf(prevLon) }

            Row {
                Button(
                    onClick = {
                        if (ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            ActivityCompat.requestPermissions(
                                context as Activity,
                                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                                1
                            )
                        } else {
                            if (!isServiceActivated) {
                                val backgroundServiceIntent =
                                    Intent(context, BackgroundService::class.java)
                                backgroundServiceIntent.putExtra("Latitude", latitude.toDouble())
                                backgroundServiceIntent.putExtra("Longitude", longitude.toDouble())

                                sharedPref.edit()
                                    .putDouble("latitude", latitude.toDouble())
                                    .putDouble("latitude", latitude.toDouble())
                                    .apply()

                                // start the service
                                startForegroundService(context, backgroundServiceIntent)
                                Toast.makeText(
                                    context,
                                    "Service Started Successfully!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    },
                    enabled = latitude.isNotEmpty() && longitude.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        disabledContainerColor = Color.LightGray,
                        disabledContentColor = Color.DarkGray
                    ),
                    modifier = Modifier.weight(1f)
                ) { Text("Start Service") }
                Spacer(Modifier.width(4.dp))
                Button(
                    onClick = {
                        // call an intent to stop the service
                        context.stopService(Intent(context, BackgroundService::class.java))
                        Toast.makeText(context, "Service Stopped!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Stop Service") }
            } // Row
        } // Column
    }

    private fun SharedPreferences.Editor.putDouble(
        key: String,
        double: Double
    ): SharedPreferences.Editor = putLong(key, doubleToRawLongBits(double))

    private fun SharedPreferences.getDouble(key: String, default: Double) =
        longBitsToDouble(getLong(key, doubleToRawLongBits(default)))

}
