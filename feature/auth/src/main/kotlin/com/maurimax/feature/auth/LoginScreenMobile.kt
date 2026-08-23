package com.maurimax.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maurimax.core.designsystem.Brand
import com.maurimax.core.designsystem.Spacing

@Composable
fun LoginScreenMobile(
    viewModel: LoginViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LoginScreenMobile(
        state = state,
        onUsernameChange = viewModel::onUsernameChange,
        onPasswordChange = viewModel::onPasswordChange,
        onSubmit = viewModel::signIn,
        modifier = modifier,
    )
}

@Composable
fun LoginScreenMobile(
    state: LoginUiState,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brand.Ink)
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
            Text(
                text = "MAURIMAX",
                color = Brand.Accent,
                fontWeight = FontWeight.Black,
                fontSize = 34.sp,
                letterSpacing = 5.sp,
            )
            Text(
                text = "Sign in with the username and password from your subscription.",
                color = Brand.TextSecondary,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = Spacing.sm),
            )

            OutlinedTextField(
                value = state.username,
                onValueChange = onUsernameChange,
                label = { Text("Username") },
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
                label = { Text("Password") },
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
                            text = if (passwordVisible) "Hide" else "Show",
                            color = Brand.TextSecondary,
                            fontSize = 13.sp,
                        )
                    }
                },
                colors = fieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )

            if (state.error != null) {
                Text(text = state.error, color = Brand.AccentBright, fontSize = 14.sp)
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
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.signingIn) {
                    CircularProgressIndicator(
                        color = Brand.TextPrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.padding(vertical = Spacing.xs),
                    )
                } else {
                    Text("Sign in", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Brand.TextPrimary,
    unfocusedTextColor = Brand.TextPrimary,
    focusedBorderColor = Brand.Accent,
    unfocusedBorderColor = Brand.Outline,
    focusedLabelColor = Brand.Accent,
    unfocusedLabelColor = Brand.TextSecondary,
    cursorColor = Brand.Accent,
)
