package com.maurimax.feature.auth

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maurimax.core.data.AppLocale
import com.maurimax.core.data.ThemeMode
import com.maurimax.core.designsystem.BrandLockup
import com.maurimax.core.designsystem.MaurimaxDisplayFamily
import com.maurimax.core.designsystem.MaurimaxTheme
import com.maurimax.core.designsystem.Showcase
import com.maurimax.core.designsystem.Spacing
import com.maurimax.core.model.Credentials
import kotlinx.coroutines.delay

@Composable
fun LoginScreenMobile(
    viewModel: LoginViewModel,
    language: String,
    onLanguageChange: (String) -> Unit,
    theme: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LoginScreenMobile(
        state = state,
        language = language,
        onLanguageChange = onLanguageChange,
        theme = theme,
        onThemeChange = onThemeChange,
        onUsernameChange = viewModel::onUsernameChange,
        onPasswordChange = viewModel::onPasswordChange,
        onSubmit = viewModel::signIn,
        onUseAccount = viewModel::useAccount,
        onForgetAccount = viewModel::forgetAccount,
        onAddAccount = viewModel::addAccount,
        onCancelAdd = viewModel::cancelAdd,
        modifier = modifier,
    )
}

/**
 * Sign-in.
 *
 * One image, one lockup, two fields and a button, laid out from the bottom so
 * the artwork owns the top of the screen and the keyboard pushes the form up
 * without disturbing it. There is no card: a form boxed inside a panel reads as
 * a settings page, and this is the first impression the product gets to make.
 */
@Composable
fun LoginScreenMobile(
    state: LoginUiState,
    language: String = AppLocale.ARABIC,
    onLanguageChange: (String) -> Unit = {},
    theme: ThemeMode = ThemeMode.DARK,
    onThemeChange: (ThemeMode) -> Unit = {},
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onUseAccount: (Credentials) -> Unit = {},
    onForgetAccount: (Credentials) -> Unit = {},
    onAddAccount: () -> Unit = {},
    onCancelAdd: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = MaurimaxTheme.colors

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.ground)
            .imePadding(),
    ) {
        Backdrop()

        Column(
            verticalArrangement = Arrangement.Bottom,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = Spacing.lg,
                    end = Spacing.lg,
                    top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 96.dp,
                    bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + Spacing.lg,
                ),
        ) {
            // The lockup is set as large as the narrowest phone can carry it.
            // At 34sp it runs past both margins on a 320dp screen, and a
            // clipped wordmark is a broken logo.
            BoxWithConstraints {
                val roomy = maxWidth >= 340.dp
                BrandLockup(
                    fontSize = if (roomy) 34.sp else 27.sp,
                    markHeight = if (roomy) 54.dp else 44.dp,
                )
            }

            Text(
                text = stringResource(R.string.brand_tagline),
                fontFamily = MaurimaxDisplayFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                lineHeight = 28.sp,
                color = colors.textSecondary,
                modifier = Modifier
                    .padding(top = Spacing.md)
                    .widthIn(max = 300.dp),
            )

            Spacer(Modifier.height(Spacing.xl))

            if (state.showingPicker) {
                AccountList(
                    accounts = state.accounts,
                    signingIn = state.signingIn,
                    error = state.error,
                    onUse = onUseAccount,
                    onForget = onForgetAccount,
                    onAdd = onAddAccount,
                )
            } else {
                SignInForm(
                    state = state,
                    onUsernameChange = onUsernameChange,
                    onPasswordChange = onPasswordChange,
                    onSubmit = onSubmit,
                    // Only offered when there is a list to go back to.
                    onCancel = onCancelAdd.takeIf { state.accounts.isNotEmpty() },
                )
            }

            Spacer(Modifier.height(Spacing.xl))

            Preferences(
                language = language,
                onLanguageChange = onLanguageChange,
                theme = theme,
                onThemeChange = onThemeChange,
            )
        }
    }
}

/**
 * The artwork behind the form.
 *
 * It changes on a slow cycle — football, film, football, film — and opens on
 * the football, because that is what most of this audience came for and the
 * first frame is the only one guaranteed to be seen. Seven seconds a frame
 * with a long dissolve: fast enough to notice on the way in, slow enough that
 * nothing moves while somebody is typing a password.
 *
 * A gradient carries it down to solid ground before the first field, so the
 * image stays atmosphere rather than something the copy has to fight.
 */
@Composable
private fun Backdrop() {
    val colors = MaurimaxTheme.colors
    var index by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(7_000)
            index = (index + 1) % Showcase.backdrops.size
        }
    }

    Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.72f)) {
        Crossfade(
            targetState = index,
            animationSpec = tween(durationMillis = 1_400),
            label = "sign-in backdrop",
            modifier = Modifier.fillMaxSize(),
        ) { current ->
            Image(
                painter = painterResource(Showcase.backdrops[current]),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to colors.ground.copy(alpha = 0.55f),
                        0.45f to colors.ground.copy(alpha = 0.86f),
                        0.82f to colors.ground,
                        1f to colors.ground,
                    ),
                ),
        )
    }
}

@Composable
private fun ColumnScope.SignInForm(
    state: LoginUiState,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onCancel: (() -> Unit)?,
) {
    val colors = MaurimaxTheme.colors
    var passwordVisible by remember { mutableStateOf(false) }

    Field(
        label = stringResource(R.string.auth_username),
        value = state.username,
        onValueChange = onUsernameChange,
        enabled = !state.signingIn,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
    )

    Spacer(Modifier.height(Spacing.md))

    Field(
        label = stringResource(R.string.auth_password),
        value = state.password,
        onValueChange = onPasswordChange,
        enabled = !state.signingIn,
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onSubmit() }),
        trailing = {
            Text(
                text = stringResource(if (passwordVisible) R.string.auth_hide else R.string.auth_show),
                style = MaterialTheme.typography.labelMedium,
                color = colors.textTertiary,
                modifier = Modifier.clickable { passwordVisible = !passwordVisible },
            )
        },
    )

    state.error?.let { failure ->
        Text(
            text = failure.message(),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.accentText,
            modifier = Modifier.padding(top = Spacing.md),
        )
    }

    Spacer(Modifier.height(Spacing.lg))

    PrimaryButton(
        label = stringResource(if (state.signingIn) R.string.auth_signing_in else R.string.auth_sign_in),
        enabled = state.canSubmit,
        onClick = onSubmit,
    )

    if (onCancel != null) {
        Text(
            text = stringResource(R.string.accounts_cancel),
            style = MaterialTheme.typography.labelMedium,
            color = colors.textTertiary,
            modifier = Modifier
                .padding(top = Spacing.md)
                .align(Alignment.CenterHorizontally)
                .clickable(onClick = onCancel),
        )
    }
}

/**
 * A labelled field.
 *
 * The label sits above rather than floating inside: a floating label that
 * animates into the border is a Material signature, and it reads as a form
 * built from a toolkit rather than drawn for this product.
 */
@Composable
private fun Field(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = MaurimaxTheme.colors
    var focused by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = colors.textTertiary,
            modifier = Modifier.padding(bottom = Spacing.xs, start = 2.dp),
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.surface)
                .border(
                    width = 1.dp,
                    // Focus is the one place orange earns its keep here.
                    color = if (focused) colors.accent else colors.outline,
                    shape = RoundedCornerShape(10.dp),
                )
                .padding(horizontal = Spacing.md),
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(
                    color = colors.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                cursorBrush = SolidColor(colors.accent),
                visualTransformation = visualTransformation,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { focused = it.isFocused },
            )
            trailing?.invoke()
        }
    }
}

/** White on near-black. The only thing on the screen that looks pressable. */
@Composable
internal fun PrimaryButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaurimaxTheme.colors
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (enabled) colors.primaryFill else colors.surfaceRaised)
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) colors.onPrimaryFill else colors.textTertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Language and appearance, as plain words at the foot of the screen.
 *
 * Deliberately the quietest thing here: a customer changes these once, and
 * chips or switches would give them the same weight as signing in.
 */
@Composable
private fun Preferences(
    language: String,
    onLanguageChange: (String) -> Unit,
    theme: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
) {
    val colors = MaurimaxTheme.colors

    @Composable
    fun Option(text: String, selected: Boolean, onClick: () -> Unit) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) colors.textPrimary else colors.textTertiary,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.clickable(onClick = onClick),
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        modifier = Modifier.fillMaxWidth(),
    ) {
        // Each language is written in its own, so somebody who cannot read the
        // one currently showing can still find theirs.
        Option("العربية", language == AppLocale.ARABIC) { onLanguageChange(AppLocale.ARABIC) }
        Option("Français", language == AppLocale.FRENCH) { onLanguageChange(AppLocale.FRENCH) }

        Spacer(Modifier.weight(1f))

        Option(
            text = stringResource(if (theme == ThemeMode.DARK) R.string.theme_dark else R.string.theme_light),
            selected = false,
        ) {
            onThemeChange(if (theme == ThemeMode.DARK) ThemeMode.LIGHT else ThemeMode.DARK)
        }
    }
}
