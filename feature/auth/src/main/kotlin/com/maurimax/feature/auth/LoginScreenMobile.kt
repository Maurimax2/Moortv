package com.maurimax.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maurimax.core.data.AppLocale
import com.maurimax.core.data.Graph
import com.maurimax.core.data.PortalFailure
import com.maurimax.core.data.ThemeMode
import com.maurimax.core.designsystem.BrandLockup
import com.maurimax.core.designsystem.Corners
import com.maurimax.core.designsystem.MaurimaxTheme
import com.maurimax.core.designsystem.PosterWall
import com.maurimax.core.designsystem.Scrims
import com.maurimax.core.designsystem.Spacing

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
        modifier = modifier,
    )
}

/**
 * Sign-in.
 *
 * The screen is deliberately one column with a lot of air: it asks for two
 * things, and a form that looks busy for two fields reads as bureaucracy. The
 * brand sits at the top at full size because this is the only screen where it
 * gets to, and the language and appearance switches sit at the very bottom
 * where they are findable without competing with the one action that matters.
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
    modifier: Modifier = Modifier,
) {
    val colors = MaurimaxTheme.colors
    var passwordVisible by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.ground)
            .imePadding(),
    ) {
        // The catalogue's own artwork, dimmed, standing in for a stock hero.
        PosterWall(
            posters = remember { Graph.rememberedPosters() },
            modifier = Modifier.fillMaxSize(),
        )

        // A violet bloom over it, so the top of the screen still belongs to the
        // brand rather than to whichever posters happen to be cached.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp)
                .background(Scrims.signInGlow()),
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg),
        ) {
            Spacer(Modifier.height(72.dp))

            BrandLockup(fontSize = 30.sp, markHeight = 48.dp)

            Text(
                text = stringResource(R.string.brand_tagline),
                color = colors.accentText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 24.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = Spacing.md),
            )

            Text(
                text = stringResource(R.string.auth_subtitle),
                color = colors.textSecondary,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = Spacing.sm)
                    .widthIn(max = 340.dp),
            )

            Spacer(Modifier.height(Spacing.xl))

            // The form sits on a raised card so the two fields read as one object.
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Corners.card))
                    .background(colors.surface.copy(alpha = 0.96f))
                    .border(1.dp, colors.outline, RoundedCornerShape(Corners.card))
                    .padding(Spacing.lg),
            ) {
                OutlinedTextField(
                    value = state.username,
                    onValueChange = onUsernameChange,
                    label = { Text(stringResource(R.string.auth_username)) },
                    singleLine = true,
                    enabled = !state.signingIn,
                    shape = RoundedCornerShape(Corners.control),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next,
                    ),
                    colors = fieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = state.password,
                    onValueChange = onPasswordChange,
                    label = { Text(stringResource(R.string.auth_password)) },
                    singleLine = true,
                    enabled = !state.signingIn,
                    shape = RoundedCornerShape(Corners.control),
                    visualTransformation = if (passwordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                    trailingIcon = {
                        TextButton(onClick = { passwordVisible = !passwordVisible }) {
                            Text(
                                text = stringResource(
                                    if (passwordVisible) R.string.auth_hide else R.string.auth_show,
                                ),
                                color = colors.textSecondary,
                                fontSize = 13.sp,
                            )
                        }
                    },
                    colors = fieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )

                state.error?.let { failure ->
                    ErrorNote(failure)
                }

                Button(
                    onClick = onSubmit,
                    enabled = state.canSubmit,
                    shape = RoundedCornerShape(Corners.control),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accent,
                        contentColor = colors.onAccent,
                        disabledContainerColor = colors.surfaceRaised,
                        disabledContentColor = colors.textTertiary,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                ) {
                    if (state.signingIn) {
                        CircularProgressIndicator(
                            color = colors.onAccent,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(22.dp),
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.auth_sign_in),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                        )
                    }
                }
            }

            Spacer(Modifier.height(Spacing.xl))

            LanguageSwitch(current = language, onSelect = onLanguageChange)
            Spacer(Modifier.height(Spacing.sm))
            ThemeSwitch(current = theme, onSelect = onThemeChange)

            Spacer(Modifier.height(Spacing.xl))
        }
    }
}

/** A failure is tinted and boxed, so it does not read as another label. */
@Composable
private fun ErrorNote(failure: PortalFailure) {
    val colors = MaurimaxTheme.colors
    Text(
        text = failure.message(),
        color = colors.accentText,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Corners.tile))
            .background(colors.surfaceRaised)
            .padding(Spacing.md),
    )
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = MaurimaxTheme.colors.textPrimary,
    unfocusedTextColor = MaurimaxTheme.colors.textPrimary,
    focusedBorderColor = MaurimaxTheme.colors.accent,
    unfocusedBorderColor = MaurimaxTheme.colors.outline,
    focusedLabelColor = MaurimaxTheme.colors.accent,
    unfocusedLabelColor = MaurimaxTheme.colors.textSecondary,
    cursorColor = MaurimaxTheme.colors.accent,
)
