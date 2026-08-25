package com.maurimax.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Cairo, bundled.
 *
 * Arabic is this product's first language, and leaving the family to the
 * platform meant it was set in whatever each manufacturer happened to ship —
 * so the same screen looked considered on one phone and like a system dialog
 * on another. A Latin family would have been worse: Arabic would fall back
 * silently and the primary language would end up the afterthought.
 *
 * Cairo draws both scripts from one design, so a French line and an Arabic
 * line beside it belong together. Four weights, subset to Latin and Arabic:
 * about 320KB for a typeface that is on every screen.
 */
private val Cairo = FontFamily(
    Font(R.font.cairo_regular, FontWeight.Normal),
    Font(R.font.cairo_semibold, FontWeight.SemiBold),
    Font(R.font.cairo_bold, FontWeight.Bold),
    Font(R.font.cairo_black, FontWeight.Black),
)

/**
 * One scale for the whole product.
 *
 * Deliberately few steps and a wide gap between them. A catalogue is mostly
 * artwork with a little text on it, so the type either titles something or
 * annotates it — anything in between just makes a screen look busy.
 *
 * Arabic sits lower and taller in its line than Latin, so line heights are
 * generous: the tight leading that flatters a Latin headline clips Arabic
 * descenders.
 */
val MaurimaxTypography = Typography(
    // Hero title. Black weight, set tight, never more than two lines.
    displayLarge = TextStyle(
        fontFamily = Cairo,
        fontSize = 34.sp,
        lineHeight = 42.sp,
        fontWeight = FontWeight.Black,
    ),
    headlineLarge = TextStyle(
        fontFamily = Cairo,
        fontSize = 26.sp,
        lineHeight = 34.sp,
        fontWeight = FontWeight.Bold,
    ),
    headlineMedium = TextStyle(
        fontFamily = Cairo,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.Bold,
    ),
    // Rail headings.
    titleLarge = TextStyle(
        fontFamily = Cairo,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Bold,
    ),
    titleMedium = TextStyle(
        fontFamily = Cairo,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    bodyLarge = TextStyle(fontFamily = Cairo, fontSize = 15.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = Cairo, fontSize = 13.sp, lineHeight = 20.sp),
    // Buttons.
    labelLarge = TextStyle(
        fontFamily = Cairo,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Bold,
    ),
    // Metadata beside a title: year, duration, category.
    labelMedium = TextStyle(fontFamily = Cairo, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = Cairo, fontSize = 11.sp, lineHeight = 15.sp),
)

/** For the few places that set a family directly rather than taking a style. */
val MaurimaxFontFamily: FontFamily = Cairo
