package com.maurimax.core.designsystem

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The MAURIMAX lockup: the mark beside the wordmark.
 *
 * Two details make it survive an Arabic build. The name is forced
 * left-to-right, because a brand is not mirrored and without it the Latin
 * letters reorder inside an RTL layout. And the row is laid out explicitly
 * rather than inheriting direction, so the mark stays on the same side of the
 * name in both languages — a logo that flips is a different logo.
 */
@Composable
fun BrandLockup(
    fontSize: TextUnit = 20.sp,
    markHeight: Dp = 28.dp,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(markHeight * 0.32f),
        modifier = modifier,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_mark),
            contentDescription = null,
            modifier = Modifier.height(markHeight),
        )
        Wordmark(fontSize = fontSize)
    }
}

/** The name alone, for places too tight for the mark. */
@Composable
fun Wordmark(
    fontSize: TextUnit = 20.sp,
    modifier: Modifier = Modifier,
) {
    BasicText(
        text = "MAURIMAX",
        style = TextStyle(
            color = Brand.TextPrimary,
            fontSize = fontSize,
            fontWeight = FontWeight.Black,
            letterSpacing = fontSize * 0.16f,
            textDirection = TextDirection.Ltr,
        ),
        modifier = modifier,
    )
}
