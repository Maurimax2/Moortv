package com.maurimax.core.designsystem

import androidx.compose.ui.unit.dp

/** Spacing scale. TV needs more breathing room than a phone, hence two values. */
object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 40.dp

    /** Safe-area inset for TV: broadcast overscan eats the outer 5% of the panel. */
    val tvOverscan = 48.dp
}

/** Corner radii. Tight rather than pill-shaped — key art should read as a poster. */
object Corners {
    val tile = 8.dp
    val card = 12.dp
    val control = 12.dp
}
