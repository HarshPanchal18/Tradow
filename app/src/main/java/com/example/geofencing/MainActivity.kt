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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startForegroundService
import com.example.geofencing.service.BackgroundService
import com.example.geofencing.ui.theme.GeofencingTheme
import com.example.geofencing.util.LATITUDE
import com.example.geofencing.util.LONGITUDE
import com.example.geofencing.util.getDouble
import com.example.geofencing.util.putDouble
import com.example.geofencing.util.showLongToast
import com.example.geofencing.util.showShortToast
import java.util.Locale

@Suppress("DEPRECATION")
class MainActivity : ComponentActivity() {
    private lateinit var sharedPref: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedPref = getSharedPreferences("", Context.MODE_PRIVATE)

        setContent {
            GeofencingTheme {

                var showBottomSheet by remember { mutableStateOf(false) }
                if (showBottomSheet) {
                    BottomSheet {
                        showBottomSheet = false
                    }
                }

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
                    }
                ) { it
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
            BottomSheetLayout()
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Preview(showBackground = true)
    @Composable
    fun BottomSheetLayout() {
        var spotTitle by remember { mutableStateOf("") }
        var latitude by remember { mutableStateOf("") }
        var longitude by remember { mutableStateOf("") }
        val focusManager = LocalFocusManager.current
        val keyboardManager = LocalSoftwareKeyboardController.current

        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment =Alignment.CenterVertically,
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
                    onClick = {},
                    enabled = spotTitle.isNotEmpty() && latitude.isNotEmpty() && longitude.isNotEmpty()
                ) {
                    Text(text = "Add")
                }
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
                    keyboardActions = KeyboardActions(onNext = {
                        focusManager.moveFocus(FocusDirection.Next)
                    }),
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

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun HomeLayout() {
        val isServiceActivated = false
        val context = LocalContext.current
        val focusManager = LocalFocusManager.current
        val keyboardManager = LocalSoftwareKeyboardController.current
        val prevLatitude = sharedPref.getDouble(LATITUDE, 0.0).toString()
        val prevLongitude = sharedPref.getDouble(LONGITUDE, 0.0).toString()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            var latitude by remember { mutableStateOf(prevLatitude) }
            var longitude by remember { mutableStateOf(prevLongitude) }

            Text("Automatically make your phone go Silent while in Campus!")

            Row(modifier = Modifier.fillMaxWidth()) {
                TextField(
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
                    keyboardActions = KeyboardActions(onNext = {
                        focusManager.moveFocus(FocusDirection.Next)
                    }),
                    trailingIcon = {
                        if (latitude.isNotEmpty()) {
                            IconButton(
                                onClick = { latitude = "" }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close, contentDescription = null,
                                    tint = Color.Black
                                )
                            }
                        }
                    },
                )
                TextField(
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
                    trailingIcon = {
                        if (longitude.isNotEmpty()) {
                            IconButton(
                                onClick = { longitude = "" }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close, contentDescription = null,
                                    tint = Color.Black
                                )
                            }
                        }
                    },
                )
            }

            Row(
                horizontalArrangement = Arrangement.SpaceAround,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        if (ContextCompat.checkSelfPermission(
                                /* context = */ this@MainActivity,
                                /* permission = */ Manifest.permission.ACCESS_FINE_LOCATION
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
                                    Intent(this@MainActivity, BackgroundService::class.java).apply {
                                        putExtra(LATITUDE, latitude.toDouble())
                                        putExtra(LONGITUDE, longitude.toDouble())
                                    }

                                sharedPref.edit()
                                    .putDouble(LATITUDE, latitude.toDouble())
                                    .putDouble(LONGITUDE, longitude.toDouble())
                                    .apply()

                                // start the service
                                startForegroundService(context, backgroundServiceIntent)
                                this@MainActivity.showShortToast("Service Started Successfully!")
                            }
                        }
                    },
                    enabled = (latitude.isNotEmpty().and(longitude.isNotEmpty())) ||
                            (latitude.toDouble() != 0.0).and(longitude.toDouble() != 0.0),
                    colors = ButtonDefaults.buttonColors(
                        disabledContainerColor = Color.LightGray,
                        disabledContentColor = Color.DarkGray
                    ),
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
    }
}
