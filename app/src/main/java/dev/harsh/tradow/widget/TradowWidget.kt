package dev.harsh.tradow.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.geofencing.R
import dev.harsh.tradow.model.Spot
import dev.harsh.tradow.util.SharedPreferencesHelper

class TradowWidget : GlanceAppWidget() {

    /** GlanceStateDefinition - Telling GlanceState how to store-retrieve data.
    - Once the object is created, the data is updating using the state directly.
     * PreferencesGlanceStateDefinition - For creating a widget using datastore preference
     */
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition
    private var activeSpot: Spot? = null

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {

            activeSpot = SharedPreferencesHelper.getActiveSpot(context)

            LaunchedEffect(activeSpot) {
                activeSpot = SharedPreferencesHelper.getActiveSpot(context)
            }

            GlanceTheme {
                WidgetCard(context, activeSpot)
            }
        }
    }

    @Composable
    fun WidgetCard(context: Context, spot: Spot?) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(12.dp)
                .background(Color.White),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (spot != null) {

                Text(
                    text = spot.title,
                    modifier = GlanceModifier.padding(4.dp),
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = ColorProvider(Color.Green.copy(0.8F))
                    )
                )

                Box(
                    modifier = GlanceModifier
                        .padding(horizontal = 8.dp)
                        .clickable {
                            activeSpot = SharedPreferencesHelper.getActiveSpot(context)
                        }
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.refresh), contentDescription = ""
                    )
                }

            } else {
                Row {
                    Text(
                        text = "Add/Select spot to continue",
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    )
                    Box(
                        modifier = GlanceModifier
                            .clickable {
                                activeSpot = SharedPreferencesHelper.getActiveSpot(context)
                            }
                    ) {
                        Image(
                            provider = ImageProvider(R.drawable.location), contentDescription = ""
                        )
                    }
                }
            }

        }
    }

}
