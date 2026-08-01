package com.gotimer.ui.widgets

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
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
import com.gotimer.ui.MainActivity
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

    /**
     * Recompute the widget layout whenever the user resizes it so the font
     * can scale to the available space.
     */
    override val sizeMode: SizeMode = SizeMode.Exact

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
 * Widget layout: dice count on top, then the full pool status and the
 * refill/gift countdowns. Fonts scale with the widget width so text is
 * legible when expanded and still fits when compact. Every line is
 * single-line so the widget never grows mid-line.
 */
@Composable
private fun GoTimerWidgetContent(model: GoTimerWidgetModel) {
    val width = LocalSize.current.width
    val titleSize = titleFontSizeFor(width)
    val bodySize = bodyFontSizeFor(width)
    GlanceTheme {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .padding(10.dp)
                .clickable(actionStartActivity<MainActivity>()),
            horizontalAlignment = Alignment.Horizontal.Start,
            verticalAlignment = Alignment.Vertical.Top,
        ) {
            Text(
                text = "GO! ${model.diceText}",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = titleSize,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
            )
            Text(
                text = model.fullProjectionText,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = bodySize,
                ),
                modifier = GlanceModifier.padding(top = 2.dp),
                maxLines = 1,
            )
            Text(
                text = "Refill ${model.nextRefillText} \u00b7 Gift ${model.giftText}",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = bodySize,
                ),
                modifier = GlanceModifier.padding(top = 2.dp),
                maxLines = 1,
            )
        }
    }
}

/**
 * Scales the title font continuously with the widget width so it grows to
 * fill the available space, while never shrinking below the compact size.
 */
@Composable
private fun titleFontSizeFor(width: Dp): TextUnit =
    (width.value * TITLE_WIDTH_SCALE).coerceAtLeast(TITLE_MIN_SIZE_SP).sp

/**
 * Scales the body font continuously with the width, kept proportionally
 * below the title so the hierarchy is preserved.
 */
@Composable
private fun bodyFontSizeFor(width: Dp): TextUnit =
    (width.value * BODY_WIDTH_SCALE).coerceAtLeast(BODY_MIN_SIZE_SP).sp

/** Dp of widget width per sp of title text. */
private const val TITLE_WIDTH_SCALE = 0.1f

/** Dp of widget width per sp of body text. */
private const val BODY_WIDTH_SCALE = 0.075f

private const val TITLE_MIN_SIZE_SP = 24f
private const val BODY_MIN_SIZE_SP = 18f
