package com.example.geofencing.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.view.animation.OvershootInterpolator
import androidx.activity.ComponentActivity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.geofencing.R
import com.example.geofencing.model.Spot
import com.example.geofencing.service.BackgroundService
import com.example.geofencing.util.LATITUDE
import com.example.geofencing.util.LONGITUDE
import com.example.geofencing.util.SharedPreferencesHelper
import com.example.geofencing.util.SharedPreferencesHelper.PREF_NAME
import com.example.geofencing.util.noRippleClickable
import com.example.geofencing.util.showShortToast
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheet(context: Context, spots: MutableState<Array<Spot>>, onDismiss: () -> Unit) {
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
        BottomSheetLayout(context) { newSpot ->
            spots.value.plus(newSpot)
            spots.value = SharedPreferencesHelper.loadSpots(context)
            onDismiss()
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun BottomSheetLayout(context: Context, onAdd: (Array<Spot>) -> Unit) {
    var spotTitle by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf("") }
    var longitude by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val keyboardManager = LocalSoftwareKeyboardController.current
    val focusedIndicatorColor = MaterialTheme.colorScheme.onBackground.copy(0.9F)

    context.startService(Intent(context, BackgroundService::class.java))
    val sharedPref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    Column(
        modifier = Modifier
            .padding(8.dp)
            .padding(bottom = 20.dp)
    ) {

        Row {
            Image(painter = painterResource(R.drawable.location), contentDescription = null)

            Text(
                text = "Add new spot",
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
                        context, Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        context as Activity,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1
                    )
                } else {
                    val manager =
                        context.getSystemService(ComponentActivity.LOCATION_SERVICE) as LocationManager
                    if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                        context.showShortToast("Please turn on location to get started")
                    } else {
                        latitude = sharedPref.getString(LATITUDE, "") ?: ""
                        longitude = sharedPref.getString(LONGITUDE, "") ?: ""
                        spotTitle = "Spot"
                    }
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
                    SharedPreferencesHelper.saveSpots(
                        context = context,
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
                label = { Text(text = "Latitude") },
                placeholder = { Text(text = "Latitude") },
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
                label = { Text(text = "Longitude") },
                placeholder = { Text("Longitude") },
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
fun BottomRow(
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
    onAddSpotClick: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row {
            Button(
                onClick = { onStartClick() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5CB34D))
            ) { Text(text = "Start") }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = { onStopClick() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDD564D))
            ) { Text("Stop & Exit") }
        }

        SmallFloatingActionButton(
            onClick = { onAddSpotClick() },
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

@Composable
fun SpotItem(spot: Spot, onClear: () -> Unit, onItemSelected: () -> Unit) {
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
            IconButton(onClick = { onClear() }) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.padding(8.dp)
                )
            }
    }
}

@Composable
fun SplashScreen(navController: NavController) = Box(
    Modifier
        .fillMaxSize()
        .background(
            color = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primaryContainer
            else Color.White
        )
) {
    val scale = remember { Animatable(0.0F) }

    LaunchedEffect(key1 = true) {
        scale.animateTo(
            targetValue = 0.7F,
            animationSpec = tween(durationMillis = 800, easing = {
                OvershootInterpolator(/* tension = */ 4F).getInterpolation(it)
            })
        )
        delay(1000)
        navController.navigate(Screens.Home) {
            popUpTo(Screens.Splash) { inclusive = true }
        }
    }

    Image(
        painter = painterResource(id = R.mipmap.ic_launcher_foreground),
        contentDescription = null,
        alignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp)
            .scale(scale.value)
    )

    Text(
        text = stringResource(id = R.string.app_name),
        textAlign = TextAlign.Center,
        fontSize = MaterialTheme.typography.headlineLarge.fontSize,
        fontWeight = FontWeight.ExtraBold,
        fontFamily = FontFamily.Serif,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(16.dp)
    )
}

@Preview(showBackground = true)
@Composable
fun Prevs() {
    SplashScreen(navController = rememberNavController())
}
