package dev.zwander.common.data

import androidx.compose.animation.animateColorAsState
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import dev.icerock.moko.resources.StringResource
import dev.icerock.moko.resources.compose.stringResource
import dev.zwander.common.components.FormatText

typealias InfoMap = Map<StringResource, InfoItem<*>?>
typealias MutableInfoMap = MutableMap<StringResource, InfoItem<*>?>

@Composable
fun generateInfoList(vararg dataKeys: Any?, block: MutableInfoMap.() -> Unit): InfoMap {
    val mapState = remember(dataKeys) {
        LinkedHashMap<StringResource, InfoItem<*>?>().also(block)
    }

    return mapState
}

operator fun MutableInfoMap.set(key: StringResource, details: StringResource? = null, value: String?) {
    this[key] = value?.let { InfoItem.StringItem(label = key, value = it, details = details) }
}

operator fun MutableInfoMap.set(key: StringResource, details: StringResource? = null, value: StringResource?) {
    this[key] = value?.let { InfoItem.StringResourceItem(label = key, value = it, details = details) }
}

// Triple: <value, min, max>
operator fun MutableInfoMap.set(key: StringResource, details: StringResource?, value: Triple<Int?, Int, Int>) {
    this[key] = value.first?.let { InfoItem.ColorGradientItem(
        label = key,
        value = it,
        details = details,
        minValue = value.second,
        maxValue = value.third,
    ) }
}

sealed interface InfoItem<T: Any?> {
    val label: StringResource
    val value: T
    val details: StringResource?

    @Composable
    fun Render(modifier: Modifier)

    data class StringItem(
        override val label: StringResource,
        override val value: String,
        override val details: StringResource?,
    ) : InfoItem<String> {
        @Composable
        override fun Render(modifier: Modifier) {
            FormatText(
                text = stringResource(label),
                value = value,
                modifier = modifier,
                detailsText = details?.let { stringResource(it) },
            )
        }
    }

    data class StringResourceItem(
        override val label: StringResource,
        override val value: StringResource,
        override val details: StringResource?,
    ) : InfoItem<StringResource> {
        @Composable
        override fun Render(modifier: Modifier) {
            FormatText(
                text = stringResource(label),
                value = stringResource(value),
                modifier = modifier,
                detailsText = details?.let { stringResource(it) },
            )
        }
    }

    data class ColorGradientItem(
        override val label: StringResource,
        override val value: Int,
        override val details: StringResource?,
        val minValue: Int,
        val maxValue: Int,
    ) : InfoItem<Int> {
        // https://www.astrouxds.com/patterns/status-system/
        companion object {
            private val darkModeRed = Color(0xFFFF3838)
            private val darkModeGreen = Color(0xFF56F000)

            private val lightModeRed = Color(0xFFFF2A04)
            private val lightModeGreen = Color(0xFF00E200)
        }

        @Composable
        override fun Render(modifier: Modifier) {
            val shouldUseDarkColor = LocalContentColor.current.luminance() > 0.5f

            val scaledFraction = ((value - minValue).toFloat() / (maxValue - minValue).toFloat())
                .coerceAtLeast(0f)
                .coerceAtMost(1f)

            val color = lerp(
                start = if (shouldUseDarkColor) darkModeRed else lightModeRed,
                stop = if (shouldUseDarkColor) darkModeGreen else lightModeGreen,
                fraction = scaledFraction,
            )
            val colorState by animateColorAsState(color)

            FormatText(
                text = stringResource(label),
                value = value.toString(),
                modifier = modifier,
                valueColor = colorState,
                detailsText = details?.let { stringResource(it) },
            )
        }
    }
}
