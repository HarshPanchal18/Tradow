package com.example.geofencing

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startForegroundService
import com.example.geofencing.model.Spot
import com.example.geofencing.service.BackgroundService
import com.example.geofencing.ui.theme.GeofencingTheme
import com.example.geofencing.util.LATITUDE
import com.example.geofencing.util.LONGITUDE
import com.example.geofencing.util.SharedPreferencesHelper
import com.example.geofencing.util.showLongToast
import com.example.geofencing.util.showShortToast
import java.util.Locale

@Suppress("DEPRECATION")
class MainActivity : ComponentActivity() {
    private lateinit var sharedPref: SharedPreferences
    private lateinit var spots: Array<Spot>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedPref = getSharedPreferences("", Context.MODE_PRIVATE)

        setContent {
            GeofencingTheme {

                val isServiceActivated = false
                val context = LocalContext.current
                spots = SharedPreferencesHelper.loadArray(context)
                var showBottomSheet by remember { mutableStateOf(false) }
                if (showBottomSheet)
                    BottomSheet { showBottomSheet = false }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    floatingActionButton = {
                        ExtendedFloatingActionButton(
                            text = { Text(text = "Add spot") },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.AddLocationAlt,
                                    contentDescription = null
                                )
                            },
                            onClick = { showBottomSheet = true }
                        )
                    },
                    bottomBar = {
                        Row(
                            horizontalArrangement = Arrangement.SpaceAround,
                            modifier = Modifier.fillMaxWidth()
                        ) {
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
                                        if (!isServiceActivated) {
                                            val backgroundServiceIntent =
                                                Intent(
                                                    this@MainActivity,
                                                    BackgroundService::class.java
                                                )

                                            // start the service
                                            startForegroundService(context, backgroundServiceIntent)
                                            this@MainActivity.showShortToast("Service Started Successfully!")
                                        }
                                    }
                                },
                            ) { Text("Start Service") }

                            Button(
                                onClick = {
                                    // call an intent to stop the service
                                    stopService(Intent(context, BackgroundService::class.java))
                                    this@MainActivity.showShortToast("Service Stopped!")
                                },
                            ) { Text("Stop Service") }
                        }
                    }
                ) {
                    @Suppress("UNUSED_EXPRESSION") it
                    HomeLayout()
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
            this.showShortToast("Service Started Successfully!")
            startForegroundService(this, Intent(this, BackgroundService::class.java))
        } else {
            // show an toast message asking for the permission
            this.showLongToast("Do I really need to tell, why you should give me location access??")
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun BottomSheet(onDismiss: () -> Unit) {
        val modalBottomSheetState = rememberModalBottomSheetState()

        ModalBottomSheet(onDismissRequest = { onDismiss() },
            sheetState = modalBottomSheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            BottomSheetLayout { newSpot ->
                spots.plus(newSpot)
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

        Column(
            modifier = Modifier
                .padding(12.dp)
                .padding(bottom = 20.dp)
        ) {
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
                )
                Button(
                    onClick = {
                        val newSpot = arrayOf(
                            Spot(
                                title = spotTitle,
                                latitude = latitude.toDouble(),
                                longitude = longitude.toDouble()
                            )
                        )
                        SharedPreferencesHelper.saveArray(
                            context = this@MainActivity,
                            spotArray = newSpot
                        )
                        onAdd(newSpot)
                    },
                    enabled = spotTitle.isNotEmpty() && latitude.isNotEmpty() && longitude.isNotEmpty()
                ) { Text(text = "Add") }
            }

            Spacer(modifier = Modifier.height(10.dp))

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
                )
            }
        }
    }

    @Composable
    fun HomeLayout() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
        ) {

            Text(
                text = "Automatically make your phone go Silent while in Campus!",
                modifier = Modifier.padding(10.dp),
                fontSize = MaterialTheme.typography.titleMedium.fontSize
            )

            Card {
                LazyColumn(modifier = Modifier.padding(10.dp)) {
                    items(spots) { spot ->
                        SpotItem(spot = spot)
                    }
                }
            }
        }
    }

    @Composable
    fun SpotItem(spot: Spot) {
        Row(verticalAlignment = Alignment.CenterVertically) {

            RadioButton(selected = true, onClick = { })

            Column(
                modifier = Modifier
                    .padding(10.dp)
                    .fillMaxWidth(),
            ) {

                Text(
                    text = spot.title,
                    fontSize = MaterialTheme.typography.titleLarge.fontSize,
                    color = Color.Black.copy(0.9F)
                )

                Text(
                    text = "${spot.latitude} : ${spot.longitude}",
                    fontSize = MaterialTheme.typography.titleMedium.fontSize,
                    color = Color.DarkGray
                )
            }
        }
    }
}
