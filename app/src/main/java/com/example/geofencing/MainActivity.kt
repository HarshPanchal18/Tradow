package com.example.geofencing

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startForegroundService
import com.example.geofencing.model.Spot
import com.example.geofencing.service.BackgroundService
import com.example.geofencing.ui.theme.GeofencingTheme
import com.example.geofencing.util.LATITUDE
import com.example.geofencing.util.LONGITUDE
import com.example.geofencing.util.SharedPreferencesHelper.PREF_NAME
import com.example.geofencing.util.SharedPreferencesHelper.loadSpots
import com.example.geofencing.util.SharedPreferencesHelper.saveSpots
import com.example.geofencing.util.SharedPreferencesHelper.updateSpots
import com.example.geofencing.util.noRippleClickable
import com.example.geofencing.util.showLongToast
import com.example.geofencing.util.showShortToast
import java.util.Locale

@Suppress("DEPRECATION")
class MainActivity : ComponentActivity() {
    private lateinit var sharedPref: SharedPreferences
    private lateinit var spots: MutableState<Array<Spot>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedPref = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        setContent {
            GeofencingTheme {

                spots = remember { mutableStateOf(loadSpots(this)) }
                var showBottomSheet by remember { mutableStateOf(false) }
                if (showBottomSheet)
                    BottomSheet(
                        onDismiss = {
                            stopService(Intent(this, BackgroundService::class.java))
                            showBottomSheet = false
                        }
                    )

                var pressedTime: Long = 0
                BackHandler(enabled = true) {
                    if (pressedTime + 1800 > System.currentTimeMillis())
                        finish()
                    else
                        this.showShortToast("Press again to exit")
                    pressedTime = System.currentTimeMillis()
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row {
                                Button(
                                    onClick = {
                                        if (ContextCompat.checkSelfPermission(
                                                /* context = */ this@MainActivity,
                                                /* permission = */
                                                Manifest.permission.ACCESS_FINE_LOCATION
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
                                                spots.value.isEmpty() -> showBottomSheet = true

                                                spots.value.any { it.isSelected } -> {
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
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF5CB34D),
                                    )
                                ) { Text(text = "Start") }

                                Spacer(modifier = Modifier.width(8.dp))

                                Button(
                                    onClick = {
                                        // call an intent to stop the service
                                        stopService(
                                            Intent(
                                                this@MainActivity,
                                                BackgroundService::class.java
                                            )
                                        )
                                        finish()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFDD564D),
                                    )
                                ) { Text("Stop & Exit") }
                            }

                            SmallFloatingActionButton(
                                onClick = { showBottomSheet = true },
                                containerColor = MaterialTheme.colorScheme.background,
                                shape = CircleShape
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AddCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    }
                ) {
                    HomeLayout(modifier = Modifier.padding(it))
                }
            }
        }
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

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun BottomSheet(onDismiss: () -> Unit) {
        val modalBottomSheetState = rememberModalBottomSheetState()

        ModalBottomSheet(
            onDismissRequest = { onDismiss() },
            sheetState = modalBottomSheetState,
            dragHandle = {
                BottomSheetDefaults.DragHandle(
                    color = MaterialTheme.colorScheme.onBackground.copy(0.9F)
                )
            },
        ) {
            BottomSheetLayout { newSpot ->
                spots.value.plus(newSpot)
                spots.value = loadSpots(this@MainActivity)
                onDismiss()
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun BottomSheetLayout(onAdd: (Array<Spot>) -> Unit) {
        var spotTitle by remember { mutableStateOf("") }
        var latitude by remember { mutableStateOf("") }
        var longitude by remember { mutableStateOf("") }
        val focusManager = LocalFocusManager.current
        val keyboardManager = LocalSoftwareKeyboardController.current
        val focusedIndicatorColor = MaterialTheme.colorScheme.onBackground.copy(0.9F)

        startService(Intent(this@MainActivity, BackgroundService::class.java))

        Column(
            modifier = Modifier
                .padding(8.dp)
                .padding(bottom = 20.dp)
        ) {

            Row {
                Image(painter = painterResource(R.drawable.location), contentDescription = null)

                Text(
                    text = "Add new track spot",
                    fontWeight = FontWeight.Bold,
                    fontSize = MaterialTheme.typography.titleLarge.fontSize,
                    style = TextStyle(fontStyle = FontStyle.Italic),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp)
                )
            }

            Button(
                onClick = {
                    if (ContextCompat.checkSelfPermission(
                            this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        ActivityCompat.requestPermissions(
                            this@MainActivity as Activity,
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1
                        )
                    } else {
                        latitude = sharedPref.getString(LATITUDE, "") ?: ""
                        longitude = sharedPref.getString(LONGITUDE, "") ?: ""
                        spotTitle = "Spot"
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp)
                    .padding(top = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onBackground.copy(0.9F),
                    contentColor = MaterialTheme.colorScheme.background
                )
            ) {
                Text(
                    text = "Fill current address",
                    textAlign = TextAlign.Center
                )
            }

            Text(
                text = "OR",
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
            )

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(5.dp)
            ) {
                OutlinedTextField(
                    value = spotTitle,
                    onValueChange = { spotTitle = it },
                    label = { Text(text = "Title") },
                    placeholder = { Text(text = "Title") },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next,
                        keyboardType = KeyboardType.Text
                    ),
                    keyboardActions = KeyboardActions(onNext = {
                        focusManager.moveFocus(FocusDirection.Down)
                    }),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = focusedIndicatorColor
                    )
                )
                Button(
                    onClick = {
                        val newSpot = arrayOf(
                            Spot(
                                title = spotTitle.trim(),
                                latitude = latitude.trim().toDouble(),
                                longitude = longitude.trim().toDouble(),
                                isSelected = false
                            )
                        )
                        saveSpots(
                            context = this@MainActivity,
                            spotArray = newSpot
                        )
                        onAdd(newSpot)
                    },
                    enabled = spotTitle.isNotEmpty() && latitude.isNotEmpty() && longitude.isNotEmpty(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.onBackground.copy(0.9F),
                        contentColor = MaterialTheme.colorScheme.background
                    )
                ) { Text(text = "Add") }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = latitude, onValueChange = { latitude = it },
                    modifier = Modifier
                        .padding(horizontal = 5.dp)
                        .weight(1F),
                    label = { Text(LATITUDE.capitalize(Locale.ROOT)) },
                    placeholder = { Text(LATITUDE.capitalize(Locale.ROOT)) },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next,
                        keyboardType = KeyboardType.Number
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Next) }
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = focusedIndicatorColor
                    )
                )

                OutlinedTextField(
                    value = longitude, onValueChange = { longitude = it },
                    modifier = Modifier
                        .padding(horizontal = 5.dp)
                        .weight(1F),
                    label = { Text(LONGITUDE.capitalize(Locale.ROOT)) },
                    placeholder = { Text(text = LONGITUDE.capitalize(Locale.ROOT)) },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done,
                        keyboardType = KeyboardType.Number
                    ),
                    keyboardActions = KeyboardActions(onDone = { keyboardManager?.hide() }),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = focusedIndicatorColor
                    )
                )
            }

        }
    }

    @Composable
    fun HomeLayout(modifier: Modifier) {

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(10.dp)
        ) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(color = Color.White)
                )

                Text(
                    text = "While on site, your phone automatically goes silent!", // Your phone will automatically go silent while you're on campus!
                    modifier = Modifier.padding(10.dp),
                    fontSize = MaterialTheme.typography.titleLarge.fontSize,
                )
            }

            if (spots.value.isNotEmpty()) {
                Card {
                    LazyColumn(modifier = Modifier.padding(10.dp)) {
                        items(spots.value) { spot ->
                            SpotItem(spot = spot) {
                                spots.value = spots.value.map { it.copy(isSelected = it == spot) }
                                    .toTypedArray()
                                updateSpots(this@MainActivity, spots.value)
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun SpotItem(spot: Spot, onItemSelected: () -> Unit) {
        var selection by remember { mutableStateOf(spot.isSelected) }

        Row(verticalAlignment = Alignment.CenterVertically) {

            IconButton(onClick = {
                selection = !selection
                spot.isSelected = !spot.isSelected
                onItemSelected()
            }) {
                Icon(
                    Icons.Default.Circle,
                    contentDescription = "Selected option",
                    modifier = Modifier.size(20.dp),
                    tint = if (spot.isSelected) Color(0xFF6FE05C) else Color.Gray
                )
            }

            Column(
                modifier = Modifier
                    .padding(10.dp)
                    .weight(1F)
                    .noRippleClickable {
                        selection = !selection
                        spot.isSelected = !spot.isSelected
                        onItemSelected()
                    },
            ) {
                Text(
                    text = spot.title.trim(),
                    fontSize = MaterialTheme.typography.titleLarge.fontSize,
                    fontFamily = FontFamily.SansSerif
                )

                if (spot.isSelected)
                    Text(
                        text = "${spot.latitude} : ${spot.longitude}",
                        fontSize = MaterialTheme.typography.titleSmall.fontSize,
                        color = MaterialTheme.colorScheme.onBackground.copy(0.75F),
                        fontStyle = FontStyle.Italic
                    )
            }

            if (!spot.isSelected)
                IconButton(onClick = {
                    val spotList = spots.value.toMutableList()
                    spotList.remove(spot)
                    spots.value = spotList.toTypedArray()

                    updateSpots(this@MainActivity, spots.value)
                }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.padding(8.dp)
                    )
                }
        }
    }
}
