package com.maurimax.core.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * The MAURIMAX wordmark.
 *
 * A brand name is not translated and not mirrored, so this forces left-to-right
 * text direction — without it the Latin letters reorder inside an Arabic layout.
 * BasicText is used so it needs no Material theme and reads the same on phone
 * and TV.
 */
@Composable
fun Wordmark(
    fontSize: TextUnit = 22.sp,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        BasicText(
            text = "MAURIMAX",
            style = TextStyle(
                color = Brand.TextPrimary,
                fontSize = fontSize,
                fontWeight = FontWeight.Black,
                letterSpacing = fontSize * 0.18f,
                textDirection = TextDirection.Ltr,
            ),
        )
    }
}
