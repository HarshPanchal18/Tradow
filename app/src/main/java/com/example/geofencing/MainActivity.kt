package com.example.geofencing

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startForegroundService
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.geofencing.model.Spot
import com.example.geofencing.service.BackgroundService
import com.example.geofencing.ui.BottomRow
import com.example.geofencing.ui.BottomSheet
import com.example.geofencing.ui.Screens
import com.example.geofencing.ui.SplashScreen
import com.example.geofencing.ui.SpotItem
import com.example.geofencing.ui.theme.GeofencingTheme
import com.example.geofencing.util.SharedPreferencesHelper.PREF_NAME
import com.example.geofencing.util.SharedPreferencesHelper.loadSpots
import com.example.geofencing.util.SharedPreferencesHelper.updateSpots
import com.example.geofencing.util.showLongToast
import com.example.geofencing.util.showShortToast
import com.google.accompanist.systemuicontroller.rememberSystemUiController

@Suppress("DEPRECATION")
class MainActivity : ComponentActivity() {
    private lateinit var sharedPref: SharedPreferences
    private lateinit var spots: MutableState<Array<Spot>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedPref = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        setContent {
            GeofencingTheme {

                val isDarkMode = isSystemInDarkTheme()
                val systemUiController = rememberSystemUiController()
                val statusBarColor = MaterialTheme.colorScheme.surface
                val navigationBarColor = MaterialTheme.colorScheme.primary

                DisposableEffect(isDarkMode) {
                    systemUiController.setNavigationBarColor(
                        color = statusBarColor,
                        darkIcons = isDarkMode
                    )
                    systemUiController.setStatusBarColor(
                        color = navigationBarColor,
                        darkIcons = isDarkMode
                    )

                    onDispose { }
                }

                spots = remember { mutableStateOf(loadSpots(this)) }
                var showBottomSheet by remember { mutableStateOf(false) }
                if (showBottomSheet) {
                    BottomSheet(
                        context = this,
                        spots = spots,
                        onDismiss = {
                            stopService(Intent(this, BackgroundService::class.java))
                            showBottomSheet = false
                        }
                    )
                }

                var pressedTime: Long = 0
                BackHandler(enabled = true) {
                    if (pressedTime + 1800 > System.currentTimeMillis())
                        finish()
                    else
                        this.showShortToast("Press again to exit")
                    pressedTime = System.currentTimeMillis()
                }

                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = Screens.Splash) {
                    composable(Screens.Splash) {
                        SplashScreen(navController = navController)
                    }
                    composable(Screens.Home) {
                        HomeScreen(forOpenBottomSheet = { showBottomSheet = true })
                    }
                }

            }
        }

        /*ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { view, insets ->
            val bottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            view.updatePadding(bottom = bottom)
            insets
        }*/

    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            this.showShortToast("Monitoring started successfully!")
            startForegroundService(this, Intent(this, BackgroundService::class.java))
        } else {
            // show an toast message asking for the permission
            this.showLongToast("Do I really need to tell, why you should give me location access??")
        }
    }

    override fun onPause() {
        super.onPause()
        moveTaskToBack(true)
    }

    @Composable
    fun HomeScreen(forOpenBottomSheet: () -> Unit) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                BottomRow(
                    onStartClick = {
                        if (ContextCompat.checkSelfPermission(
                                this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            ActivityCompat.requestPermissions(
                                /* activity = */ this@MainActivity as Activity,
                                /* permissions = */
                                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                                /* requestCode = */
                                1
                            )
                        } else {
                            when {
                                spots.value.isEmpty() -> forOpenBottomSheet()

                                spots.value.any { it.isSelected } -> {
                                    val manager =
                                        getSystemService(LOCATION_SERVICE) as LocationManager
                                    if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER))
                                        this.showShortToast("Please turn on location to get started")
                                    //startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))

                                    val backgroundServiceIntent =
                                        Intent(
                                            this@MainActivity,
                                            BackgroundService::class.java
                                        )
                                    startService(backgroundServiceIntent)
                                    //this@MainActivity.showShortToast("Service Started Successfully!")
                                }

                                else ->
                                    this@MainActivity.showShortToast("Kindly choose a site to monitor!")

                            }
                        }
                    },
                    onStopClick = {
                        // call an intent to stop the service
                        stopService(
                            Intent(this@MainActivity, BackgroundService::class.java)
                        )
                        finish()
                    },
                    onAddSpotClick = { forOpenBottomSheet() }
                )
            }
        ) {
            HomeLayout(modifier = Modifier.padding(it))
        }
    }

    @Composable
    fun HomeLayout(modifier: Modifier) {

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp)
        ) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(color = MaterialTheme.colorScheme.primaryContainer)
                )

                Text(
                    text = "While on site, your phone automatically goes on vibrate!", // Your phone will automatically go silent while you're on campus!
                    modifier = Modifier.padding(10.dp),
                    fontSize = MaterialTheme.typography.titleLarge.fontSize,
                )
            }

            if (spots.value.isNotEmpty()) {
                Card {
                    LazyColumn(modifier = Modifier.padding(10.dp)) {
                        items(spots.value) { spot ->
                            SpotItem(
                                spot = spot,
                                onClear = {
                                    val spotList = spots.value.toMutableList()
                                    spotList.remove(spot)
                                    spots.value = spotList.toTypedArray()

                                    updateSpots(this@MainActivity, spots.value)
                                }
                            ) {
                                spots.value = spots.value.map { it.copy(isSelected = it == spot) }
                                    .toTypedArray()
                                updateSpots(this@MainActivity, spots.value)
                            }
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "Add new spot by pressing the 'Plus' button below!",
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }
    }
}
