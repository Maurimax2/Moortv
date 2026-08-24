package com.maurimax.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maurimax.core.designsystem.Corners
import com.maurimax.core.designsystem.MaurimaxTheme
import com.maurimax.core.designsystem.MaurimaxFormColors
import com.maurimax.core.data.ThemeMode
import com.maurimax.core.designsystem.BrandLockup
import com.maurimax.core.designsystem.Spacing

@Composable
fun LoginScreenTv(
    viewModel: LoginViewModel,
    language: String,
    onLanguageChange: (String) -> Unit,
    theme: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LoginScreenTv(
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

@Composable
fun LoginScreenTv(
    state: LoginUiState,
    language: String = com.maurimax.core.data.AppLocale.ARABIC,
    onLanguageChange: (String) -> Unit = {},
    theme: ThemeMode = ThemeMode.LIGHT,
    onThemeChange: (ThemeMode) -> Unit = {},
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val usernameField = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { usernameField.requestFocus() } }

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(MaurimaxTheme.colors.ground)
            .padding(Spacing.tvOverscan),
    ) {
        // Left half is brand, right half is the form. Splitting them keeps the
        // focusable controls in one column so D-pad travel is a straight line.
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
        ) {
            BrandLockup(fontSize = 40.sp, markHeight = 62.dp)
            Text(
                text = stringResource(R.string.brand_tagline),
                color = MaurimaxTheme.colors.accentText,
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 27.sp,
                modifier = Modifier
                    .padding(top = Spacing.sm)
                    .width(400.dp),
            )
            Text(
                text = stringResource(R.string.auth_subtitle),
                color = MaurimaxTheme.colors.textSecondary,
                fontSize = 17.sp,
                lineHeight = 25.sp,
                modifier = Modifier
                    .padding(top = Spacing.md)
                    .width(400.dp),
            )
            LanguageSwitch(
                current = language,
                onSelect = onLanguageChange,
                fontSize = 15.sp,
                modifier = Modifier.padding(top = Spacing.lg),
            )
            ThemeSwitch(
                current = theme,
                onSelect = onThemeChange,
                fontSize = 15.sp,
                modifier = Modifier.padding(top = Spacing.sm),
            )
        }

        MaurimaxFormColors {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
            ) {
                OutlinedTextField(
                    value = state.username,
                    onValueChange = onUsernameChange,
                    label = { Text(stringResource(R.string.auth_username)) },
                    singleLine = true,
                    enabled = !state.signingIn,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    colors = tvFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(usernameField),
                )

                OutlinedTextField(
                    value = state.password,
                    onValueChange = onPasswordChange,
                    label = { Text(stringResource(R.string.auth_password)) },
                    singleLine = true,
                    enabled = !state.signingIn,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                    colors = tvFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.md),
                )

                state.error?.let { failure ->
                    Text(
                        text = failure.message(),
                        color = MaurimaxTheme.colors.accentText,
                        fontSize = 16.sp,
                        lineHeight = 23.sp,
                        modifier = Modifier.padding(top = Spacing.md),
                    )
                }

                Button(
                    onClick = onSubmit,
                    enabled = state.canSubmit,
                    shape = RoundedCornerShape(Corners.control),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaurimaxTheme.colors.accent,
                        contentColor = MaurimaxTheme.colors.textPrimary,
                        disabledContainerColor = MaurimaxTheme.colors.surfaceRaised,
                        disabledContentColor = MaurimaxTheme.colors.textTertiary,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(top = Spacing.lg),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(
                                if (state.signingIn) R.string.auth_signing_in else R.string.auth_sign_in,
                            ),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 17.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun tvFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = MaurimaxTheme.colors.textPrimary,
    unfocusedTextColor = MaurimaxTheme.colors.textPrimary,
    focusedBorderColor = MaurimaxTheme.colors.accent,
    unfocusedBorderColor = MaurimaxTheme.colors.outline,
    focusedLabelColor = MaurimaxTheme.colors.accent,
    unfocusedLabelColor = MaurimaxTheme.colors.textSecondary,
    cursorColor = MaurimaxTheme.colors.accent,
)
