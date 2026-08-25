package com.maurimax.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp

/** Drawn rather than imported, so the lockup needs no icon dependency. */
@Composable
fun PlayGlyph(color: Color, size: androidx.compose.ui.unit.Dp = 13.dp) {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(size)) {
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(0f, 0f)
            lineTo(this@Canvas.size.width, this@Canvas.size.height / 2f)
            lineTo(0f, this@Canvas.size.height)
            close()
        }
        drawPath(path, color)
    }
}
