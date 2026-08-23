package com.maurimax.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maurimax.core.designsystem.Brand
import com.maurimax.core.designsystem.MaurimaxFormColors
import com.maurimax.core.designsystem.Spacing

@Composable
fun LoginScreenTv(
    viewModel: LoginViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LoginScreenTv(
        state = state,
        onUsernameChange = viewModel::onUsernameChange,
        onPasswordChange = viewModel::onPasswordChange,
        onSubmit = viewModel::signIn,
        modifier = modifier,
    )
}

@Composable
fun LoginScreenTv(
    state: LoginUiState,
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
            .background(Brand.Ink)
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
            Text(
                text = "MAURIMAX",
                color = Brand.Accent,
                fontWeight = FontWeight.Black,
                fontSize = 52.sp,
                letterSpacing = 8.sp,
            )
            Text(
                text = "Sign in with the username and password from your subscription.",
                color = Brand.TextSecondary,
                fontSize = 17.sp,
                modifier = Modifier
                    .padding(top = Spacing.md)
                    .width(380.dp),
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
                    label = { Text("Username") },
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
                    label = { Text("Password") },
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

                if (state.error != null) {
                    Text(
                        text = state.error,
                        color = Brand.AccentBright,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(top = Spacing.md),
                    )
                }

                Button(
                    onClick = onSubmit,
                    enabled = state.canSubmit,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Brand.Accent,
                        contentColor = Brand.TextPrimary,
                        disabledContainerColor = Brand.SurfaceRaised,
                        disabledContentColor = Brand.TextSecondary,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.lg),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = if (state.signingIn) "Signing in…" else "Sign in",
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
    focusedTextColor = Brand.TextPrimary,
    unfocusedTextColor = Brand.TextPrimary,
    focusedBorderColor = Brand.Accent,
    unfocusedBorderColor = Brand.Outline,
    focusedLabelColor = Brand.Accent,
    unfocusedLabelColor = Brand.TextSecondary,
    cursorColor = Brand.Accent,
)
