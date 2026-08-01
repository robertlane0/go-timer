package com.gotimer.ui.widgets

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.gotimer.datastore.appDataStore
import com.gotimer.repository.DiceRepository
import kotlinx.coroutines.flow.first

/**
 * Home screen widget showing current dice, full pool projection, next
 * refill, and Free Gift countdown.
 *
 * State is read straight from DataStore on every update, so the widget
 * always shows the latest persisted timestamps. Updates arrive from the
 * launcher refresh, the periodic provider interval, a device boot, or the
 * app being opened.
 */
class GoTimerWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = DiceRepository(context.appDataStore).appState.first()
        val model = GoTimerWidgetModel.from(state, System.currentTimeMillis())
        provideContent {
            GoTimerWidgetContent(model)
        }
    }
}

/**
 * Receiver that makes [GoTimerWidget] available as an app widget.
 */
class GoTimerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GoTimerWidget()
}

/**
 * Compact widget layout: dice count on top, then the full pool status and
 * the refill/gift countdowns.
 */
@Composable
private fun GoTimerWidgetContent(model: GoTimerWidgetModel) {
    GlanceTheme {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .padding(12.dp),
            horizontalAlignment = Alignment.Horizontal.Start,
            verticalAlignment = Alignment.Vertical.Top,
        ) {
            Text(
                text = "GO! ${model.diceText}",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Text(
                text = model.fullProjectionText,
                style = TextStyle(color = GlanceTheme.colors.onSurface),
                modifier = GlanceModifier.padding(top = 4.dp),
            )
            Text(
                text = "Refill ${model.nextRefillText} \u00b7 Gift ${model.giftText}",
                style = TextStyle(color = GlanceTheme.colors.onSurface),
                modifier = GlanceModifier.padding(top = 4.dp),
            )
        }
    }
}
