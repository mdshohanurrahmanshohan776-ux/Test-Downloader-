package com.shohan.pro.downloader.ui.components

import android.graphics.drawable.Drawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlin.math.roundToInt

class AndroidDrawablePainter(
    private val drawable: Drawable
) : Painter() {

    override val intrinsicSize: Size
        get() = if (drawable.intrinsicWidth > 0 && drawable.intrinsicHeight > 0) {
            Size(drawable.intrinsicWidth.toFloat(), drawable.intrinsicHeight.toFloat())
        } else {
            Size.Unspecified
        }

    override fun DrawScope.onDraw() {
        drawIntoCanvas { canvas ->
            drawable.setBounds(0, 0, size.width.roundToInt(), size.height.roundToInt())
            drawable.draw(canvas.nativeCanvas)
        }
    }
}

@Composable
fun rememberAndroidDrawablePainter(
    drawableResId: Int,
    isPressed: Boolean = false,
    isChecked: Boolean = false
): Painter {
    val context = LocalContext.current
    return remember(drawableResId, isPressed, isChecked) {
        val baseDrawable = ContextCompat.getDrawable(context, drawableResId)?.mutate()
            ?: error("Drawable $drawableResId could not be loaded")

        val stateList = mutableListOf<Int>()
        if (isPressed) {
            stateList.add(android.R.attr.state_pressed)
        }
        if (isChecked) {
            stateList.add(android.R.attr.state_checked)
        }
        baseDrawable.state = stateList.toIntArray()

        AndroidDrawablePainter(baseDrawable)
    }
}

@Composable
fun Modifier.androidDrawableBackground(
    drawableResId: Int,
    isPressed: Boolean = false,
    isChecked: Boolean = false
): Modifier {
    val context = LocalContext.current
    val drawable = remember(drawableResId, isPressed, isChecked) {
        val d = ContextCompat.getDrawable(context, drawableResId)?.mutate()
        val stateList = mutableListOf<Int>()
        if (isPressed) {
            stateList.add(android.R.attr.state_pressed)
        }
        if (isChecked) {
            stateList.add(android.R.attr.state_checked)
        }
        d?.state = stateList.toIntArray()
        d
    }

    return this.drawBehind {
        drawable?.let { d ->
            d.setBounds(0, 0, size.width.roundToInt(), size.height.roundToInt())
            d.draw(drawContext.canvas.nativeCanvas)
        }
    }
}
