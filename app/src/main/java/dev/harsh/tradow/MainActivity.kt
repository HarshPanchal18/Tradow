package dev.harsh.tradow

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.geofencing.R
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import dev.harsh.tradow.model.Spot
import dev.harsh.tradow.service.BackgroundService
import dev.harsh.tradow.ui.BottomRow
import dev.harsh.tradow.ui.BottomSheet
import dev.harsh.tradow.ui.Screens
import dev.harsh.tradow.ui.SplashScreen
import dev.harsh.tradow.ui.SpotItem
import dev.harsh.tradow.ui.theme.GeofencingTheme
import dev.harsh.tradow.util.RADIUS
import dev.harsh.tradow.util.SharedPreferencesHelper.PREF_NAME
import dev.harsh.tradow.util.SharedPreferencesHelper.loadSpots
import dev.harsh.tradow.util.SharedPreferencesHelper.updateSpots
import dev.harsh.tradow.util.showLongToast
import dev.harsh.tradow.util.showShortToast

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

    // Move the task containing this activity to the back of the activity stack.
    override fun onPause() {
        super.onPause()

        if (checkLocationPermission())
            moveTaskToBack(true)
    }

    @Composable
    fun HomeScreen(forOpenBottomSheet: () -> Unit) {
        var hasLocationPermission = false
        val isLocationEnabled = checkLocationState()
        val locationPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted)
                hasLocationPermission = true
            else
                this.showLongToast("Do I really need to tell, why you should give me location access??")
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                BottomRow(
                    onStartClick = {
                        if (hasLocationPermission) {
                            when {
                                spots.value.isEmpty() -> forOpenBottomSheet()

                                spots.value.any { it.isSelected } -> {

                                    if (isLocationEnabled) {
                                        val backgroundServiceIntent =
                                            Intent(this@MainActivity, BackgroundService::class.java)
                                        startService(backgroundServiceIntent)
                                    } else {
                                        this.showShortToast("Please turn on location to get started")
                                    }
                                }

                                else -> this@MainActivity.showShortToast("Kindly choose a site to monitor!")
                            }
                        } else {
                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
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

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {

                        val state =
                            remember { mutableFloatStateOf(sharedPref.getFloat(RADIUS, 50F)) }

                        Slider(
                            value = state.floatValue,
                            onValueChange = {
                                state.floatValue = it
                                sharedPref.edit().apply { putFloat(RADIUS, it) }.apply()
                            },
                            steps = 2,
                            valueRange = 50F..200F,
                            modifier = Modifier
                                .weight(1F)
                                .padding(end = 8.dp),
                            colors = SliderDefaults.colors(
                                activeTrackColor = MaterialTheme.colorScheme.onSurface,
                                thumbColor = MaterialTheme.colorScheme.inverseSurface
                            )
                        )

                        Text(
                            text = "Radius: ${state.floatValue.toInt()}M",
                            fontWeight = FontWeight.SemiBold
                        )
                    }

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
                                    spots.value =
                                        spots.value.map { it.copy(isSelected = it == spot) }
                                            .toTypedArray()
                                    updateSpots(this@MainActivity, spots.value)
                                }
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

    private fun checkLocationPermission(): Boolean {
        // Check if the location permission is granted
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkLocationState(): Boolean {
        val manager = getSystemService(LOCATION_SERVICE) as LocationManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            manager.isLocationEnabled
        } else {
            manager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        }
    }
}
