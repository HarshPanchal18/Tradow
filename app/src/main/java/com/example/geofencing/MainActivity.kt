package com.example.geofencing

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startForegroundService
import com.example.geofencing.ui.theme.GeofencingTheme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.LocationSource
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.BuildConfig
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.lang.Double.doubleToRawLongBits
import java.lang.Double.longBitsToDouble
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

/*@Suppress("DEPRECATION")
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
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val latitude by remember { mutableStateOf(prevLat) }
            val longitude by remember { mutableStateOf(prevLon) }

            MapScreen(modifier = Modifier.weight(1F))
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
                Spacer(Modifier.width(4.dp))
                /*Button(onClick = { MapWindow() }) {
                    Text("Open map")
                }*/
            } // Row
            //MapWindow()
        } // Column
    }

    @Composable
    private fun MapWindow() {
        val hydePark = LatLng(51.508610, -0.163611)
        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(hydePark, 10F)
        }
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState
        ) {
            Marker(
                state = MarkerState(position = hydePark),
                title = "Hyde Park",
                snippet = "Marker in Hyde park"
            )
        }
    }

    private fun SharedPreferences.Editor.putDouble(
        key: String,
        double: Double
    ): SharedPreferences.Editor = putLong(key, doubleToRawLongBits(double))

    private fun SharedPreferences.getDouble(key: String, default: Double) =
        longBitsToDouble(getLong(key, doubleToRawLongBits(default)))

    @Composable
    fun MapScreen(modifier: Modifier = Modifier) {
        val initialPosition = LatLng(34.6767, 33.04455)
        var currentPosition by rememberSaveable { mutableStateOf(initialPosition) }

        Surface(
            color = Color.White,
            modifier = modifier.fillMaxWidth()
        ) {
            Box {
                MapViewContainer(
                    modifier = Modifier.fillMaxSize(),
                    initialPosition = initialPosition,
                    onMarkerDrag = {}
                )
                IconButton(
                    onClick = {
                        Toast.makeText(
                            baseContext,
                            "Position: ${currentPosition.latitude}:${currentPosition.longitude}",
                            Toast.LENGTH_LONG
                        ).show()
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) { Text(text = "Return Back") }
            }

        }
    }

    @Composable
    private fun MapViewContainer(
        modifier: Modifier,
        initialPosition: LatLng,
        onMarkerDrag: () -> Unit
    ) {
        var mapView: MapView? by remember { mutableStateOf(null) }

        DisposableEffect(key1 = Unit) {
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
                            onMarkerDrag(marker)
                        }
                    })
                }
            }
        }
    }

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

    @Preview
    @Composable
    fun prev() {
        HomeLayout()
    }
}
*/

private const val TAG = "Map App"
private val locationSource = MyLocationSource()
var currentLocation: MutableState<Location?> = mutableStateOf(null)
private var shouldShowMap: MutableState<Boolean> = mutableStateOf(false)

class MainActivity : ComponentActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: com.google.android.gms.location.LocationRequest
    private lateinit var locationCallback: LocationCallback

    @Composable
    fun RequestPerms() {
        val requestPermissionLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
                ) {
                    Log.i("kilo", "Permission granted")
                    setupFusedLocation()
                    shouldShowMap.value = true
                } else {
                    Log.i("kilo", "Permission denied")
                }
            }

        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                Log.i("kilo", "Permission previously granted")
                shouldShowMap.value = true
            }

            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.CAMERA
            ) -> Log.i("kilo", "Show camera permissions dialog")

            else -> SideEffect {
                requestPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }


    // ON CREATE
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            //DirectionWithLocationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ScaffoldWithTopBar()
                    RequestPerms()
                }
            //}
        }

    }

    // Bug in fusedLocation
    @SuppressLint("MissingPermission")
    private fun setupFusedLocation() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                for (location in p0.locations) {
                    currentLocation.value = location
                    locationSource.onLocationChanged(location)
                }
            }
        }
        locationRequest = com.google.android.gms.location.LocationRequest.create().apply {
            interval = TimeUnit.SECONDS.toMillis(10)
            fastestInterval = TimeUnit.SECONDS.toMillis(5)
            maxWaitTime = TimeUnit.SECONDS.toMillis(10)
            priority = Priority.PRIORITY_HIGH_ACCURACY

        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.myLooper()
        )
    }


    /*    @SuppressLint("MissingPermission")
        private fun startLocationUpdates() {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }

        override fun onPause() {
            super.onPause()
            stopLocationUpdates()
        }

        private fun stopLocationUpdates() {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }

        override fun onResume() {
            super.onResume()
            startLocationUpdates()
        }*/
}

private class MyLocationSource : LocationSource {

    private var listener: LocationSource.OnLocationChangedListener? = null

    override fun activate(listener: LocationSource.OnLocationChangedListener) {
        this.listener = listener
    }

    override fun deactivate() {
        listener = null
    }

    fun onLocationChanged(location: Location) {
        listener?.onLocationChanged(location)
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScaffoldWithTopBar() {
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("") },
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }) { pad ->
        val topPad = pad.calculateTopPadding()

        if (currentLocation.value != null && shouldShowMap.value) {
            MapScreen(
                newLocation = currentLocation.value!!,
                topPad
            )
        } else {
            AnimatedVisibility(
                modifier = Modifier.fillMaxSize(),
                visible = currentLocation.value == null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.background)
                        .wrapContentSize()
                )
            }
        }

    }
}


@Composable
private fun MapScreen(
    newLocation: Location,
    topPad: Dp
) {

    val loc = LatLng(newLocation.latitude, newLocation.longitude)
    locationSource.onLocationChanged(newLocation)
    var showMarker by remember { mutableStateOf(false) }
    var destinationLocation by remember { mutableStateOf(loc) }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(loc, 14f)
    }

    var isMapLoaded by remember { mutableStateOf(false) }

    val uiSettings by remember { mutableStateOf(MapUiSettings()) }
    var properties by remember {
        mutableStateOf(MapProperties(mapType = MapType.NORMAL, isMyLocationEnabled = true))
    }


    // Update blue dot and camera when the location changes
    LaunchedEffect(newLocation) {
        Log.d(TAG, "Updating blue dot on map...")
        locationSource.onLocationChanged(newLocation)

        Log.d(TAG, "Updating camera position...")
        val cameraPosition = CameraPosition.fromLatLngZoom(
            LatLng(
                newLocation.latitude,
                newLocation.longitude
            ), cameraPositionState.position.zoom
        )
        cameraPositionState.animate(CameraUpdateFactory.newCameraPosition(cameraPosition), 1_000)
    }


    Box(Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier
                .matchParentSize()
                .padding(top = topPad),
            properties = properties,
            uiSettings = uiSettings,
            cameraPositionState = cameraPositionState,
            onMapLoaded = {
                isMapLoaded = true
            },
            onMapClick = {
                destinationLocation = it
                showMarker = true
                val url = getDirectionsUrl(
                    LatLng(newLocation.latitude, newLocation.longitude),
                    destinationLocation
                )
                println(url)
                /*
                {   "error_message" : "You must enable Billing on the Google Cloud Project at
                 https://console.cloud.google.com/project/_/billing/enable
                 Learn more at https://developers.google.com/maps/gmp-get-started",
                   "routes" : [],   "status" : "REQUEST_DENIED"}
                 */

                // Does not work because ABOVE, check logcat if u want
                CoroutineScope(Dispatchers.IO).launch {
                    val data = downloadUrl(url)
                    withContext(Dispatchers.Main) {
                        println(data)
                    }
                }


            },
            locationSource = locationSource
        ) {
            if (showMarker) {
                Marker(
                    state = MarkerState(position = destinationLocation),
                    title = "Destination",
                    snippet = "${destinationLocation.latitude}, ${destinationLocation.longitude}"
                )
                Polyline(
                    points = listOf(
                        LatLng(newLocation.latitude, newLocation.longitude),
                        destinationLocation
                    )
                )
            }

        }

        // Buttons
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 8.dp),
            horizontalAlignment = Alignment.End
        ) {
            Button(onClick = {
                properties = if (properties == properties.copy(mapType = MapType.NORMAL)) {
                    properties.copy(mapType = MapType.SATELLITE)
                } else {
                    properties.copy(mapType = MapType.NORMAL)
                }
            }
            ) {
                Text(text = "Toggle SATELLITE")
            }
        }

        if (!isMapLoaded) {
            AnimatedVisibility(
                modifier = Modifier
                    .matchParentSize(),
                visible = !isMapLoaded,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.background)
                        .wrapContentSize()
                )
            }
        }
    }
}

private fun getDirectionsUrl(origin: LatLng, dest: LatLng): String {

    // Origin of route
    val strOrigin = "origin=" + origin.latitude + "," + origin.longitude

    // Destination of route
    val strDest = "destination=" + dest.latitude + "," + dest.longitude

    // Sensor enabled
    val sensor = "sensor=false"
    val mode = "mode=driving"
    // Building the parameters to the web service
    val parameters = "$strOrigin&$strDest&$sensor&$mode"

    // Output format
    val output = "json"

    // Building the url to the web service
    return "https://maps.googleapis.com/maps/api/directions/$output?$parameters&key=AIzaSyBrwz5Cl9o8PNBEur3CtK6QRX_-Sij_SjY"
}

private fun downloadUrl(strUrl: String): String {
    val iStream: InputStream
    val urlConnection: HttpURLConnection

    val url = URL(strUrl)

    urlConnection = url.openConnection() as HttpURLConnection
    urlConnection.connect()

    iStream = urlConnection.inputStream

    val br = BufferedReader(InputStreamReader(iStream))
    val sb = StringBuffer()

    var line = br.readLine()
    while (line != null) {
        sb.append(line)
        line = br.readLine()
    }

    val data = sb.toString()

    br.close()
    Log.d("data", data)
    iStream.close()
    urlConnection.disconnect()

    return data
}
