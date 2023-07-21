package com.example.geofencing

import android.Manifest
import android.annotation.SuppressLint
import java.lang.Double.longBitsToDouble
import java.lang.Double.doubleToRawLongBits
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startForegroundService
import com.example.geofencing.ui.theme.GeofencingTheme
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.CameraPosition

@Suppress("DEPRECATION")
class MainActivity : ComponentActivity(), OnMapReadyCallback, GoogleMap.OnInfoWindowClickListener,
    GoogleMap.OnMarkerDragListener {
    private lateinit var sharedPref: SharedPreferences
    private var position = LatLng(34.6767, 33.04455)

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

            MapsScreen(modifier = Modifier.weight(1F))
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

    @Composable
    fun MapsScreen(modifier: Modifier = Modifier) {
        val initialPosition = LatLng(34.6767, 33.04455)
        var currentPosition by rememberSaveable { mutableStateOf(initialPosition) }

        Surface(
            color = Color.White,
            modifier = modifier.fillMaxWidth()
        ) {
            MapViewContainer(
                modifier = Modifier.fillMaxSize(),
                initialPosition = initialPosition,
                onMarkerDrag = { newPosition -> currentPosition = newPosition }
            )

            IconButton(
                onClick = {
                    Toast.makeText(
                        baseContext,
                        "Position: ${currentPosition.latitude}:${currentPosition.longitude}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            ) { Text(text = "Return Back") }
        }
    }

    @SuppressLint("PotentialBehaviorOverride")
    @Composable
    fun MapViewContainer(
        modifier: Modifier = Modifier,
        initialPosition: LatLng,
        onMarkerDrag: (LatLng) -> Unit
    ) {
        var mapView: MapView? by remember { mutableStateOf(null) }

        DisposableEffect(Unit) {
            mapView?.onCreate(Bundle())
            onDispose { mapView?.onDestroy() }
        }

        AndroidView(
            factory = { context ->
                mapView = MapView(context)
                mapView!!
            },
            modifier = modifier
        )

        LaunchedEffect(mapView) {
            mapView!!.getMapAsync { googleMap ->
                googleMap.apply {
                    // Set map style
                    setMapStyle(
                        MapStyleOptions.loadRawResourceStyle(baseContext, R.raw.map_style)
                    )

                    // Add a marker to the map
                    val markerOptions = MarkerOptions()
                        .position(initialPosition)
                        .title("Marker Title")
                        .draggable(true)
                    val marker = addMarker(markerOptions)

                    // Set the initial camera position
                    val cameraPosition = CameraPosition.Builder()
                        .target(initialPosition)
                        .zoom(12f)
                        .build()
                    moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))

                    // Set marker drag listener
                    setOnMarkerDragListener(object : GoogleMap.OnMarkerDragListener {
                        override fun onMarkerDragStart(marker: Marker) {}

                        override fun onMarkerDrag(marker: Marker) {}

                        override fun onMarkerDragEnd(marker: Marker) {
                            val newPosition = marker.position
                            onMarkerDrag(newPosition)
                        }
                    })
                } // apply{}
            }
        } // LaunchedEffect()
    }

    @SuppressLint("PotentialBehaviorOverride")
    override fun onMapReady(map: GoogleMap) {
        if (
            (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED)
            &&
            (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED)
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            ActivityCompat.requestPermissions(
                baseContext as Activity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
        }

        map.isMyLocationEnabled = true
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 13F))
        val zoom = CameraUpdateFactory.zoomTo(15F)
        map.animateCamera(zoom)
        map.addMarker(
            MarkerOptions()
                .title("Shop")
                .snippet("Is this the right location?")
                .position(position)
                .draggable(true)
        )

        map.setOnInfoWindowClickListener(this)
        map.setOnMarkerDragListener(this)
    }

    override fun onInfoWindowClick(marker: Marker) {
        Toast.makeText(this, marker.title, Toast.LENGTH_LONG).show()
    }

    override fun onMarkerDrag(marker: Marker) {
        val position0: LatLng = marker.position
        Log.d(localClassName, "Drag from ${position0.latitude} : ${position0.longitude}")
    }

    override fun onMarkerDragEnd(marker: Marker) {
        position = marker.position
        Log.d(localClassName, "Drag from ${position.latitude} : ${position.longitude}")
    }

    override fun onMarkerDragStart(marker: Marker) {
        val position0: LatLng = marker.position
        Log.d(localClassName, "Drag from ${position0.latitude} : ${position0.longitude}")
    }
}
