package com.maurimax.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maurimax.core.designsystem.Corners
import com.maurimax.core.designsystem.MaurimaxTheme
import com.maurimax.core.data.PortalFailure
import com.maurimax.core.data.ThemeMode
import com.maurimax.core.designsystem.BrandLockup
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

@Composable
fun LoginScreenMobile(
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
    var passwordVisible by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaurimaxTheme.colors.ground)
            .imePadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .widthIn(max = 420.dp)
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            BrandLockup(fontSize = 28.sp, markHeight = 44.dp)
            Text(
                text = stringResource(R.string.brand_tagline),
                color = MaurimaxTheme.colors.accentText,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 22.sp,
            )
            Text(
                text = stringResource(R.string.auth_subtitle),
                color = MaurimaxTheme.colors.textSecondary,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                modifier = Modifier.padding(bottom = Spacing.sm),
            )

            OutlinedTextField(
                value = state.username,
                onValueChange = onUsernameChange,
                label = { Text(stringResource(R.string.auth_username)) },
                singleLine = true,
                enabled = !state.signingIn,
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
                            color = MaurimaxTheme.colors.textSecondary,
                            fontSize = 13.sp,
                        )
                    }
                },
                colors = fieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )

            state.error?.let { failure ->
                Text(
                    text = failure.message(),
                    color = MaurimaxTheme.colors.accentText,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
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
                    .height(52.dp),
            ) {
                if (state.signingIn) {
                    CircularProgressIndicator(
                        color = MaurimaxTheme.colors.textPrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.padding(vertical = Spacing.xs),
                    )
                } else {
                    Text(
                        text = stringResource(R.string.auth_sign_in),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                    )
                }
            }

            LanguageSwitch(
                current = language,
                onSelect = onLanguageChange,
                modifier = Modifier.padding(top = Spacing.sm),
            )
            ThemeSwitch(current = theme, onSelect = onThemeChange)
        }
    }
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
