package com.maurimax.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maurimax.core.designsystem.MaurimaxMobileTheme
import com.maurimax.feature.auth.LoginScreenMobile
import com.maurimax.feature.auth.LoginViewModel
import com.maurimax.feature.home.HomeScreenMobile
import com.maurimax.feature.home.HomeViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaurimaxMobileTheme {
                MaurimaxMobileApp()
            }
        }
    }
}

/**
 * Sign-in gate. Saved credentials are re-submitted on launch, so a returning
 * customer sees the login screen only long enough for that to come back.
 */
@Composable
private fun MaurimaxMobileApp() {
    val loginViewModel: LoginViewModel = viewModel(factory = LoginViewModel.Factory)
    val login by loginViewModel.uiState.collectAsStateWithLifecycle()

    val credentials = login.credentials
    if (login.signedIn && credentials != null) {
        HomeScreenMobile(
            viewModel = viewModel(
                key = "home-${credentials.username}",
                factory = HomeViewModel.factory(credentials),
            ),
        )
    } else {
        LoginScreenMobile(viewModel = loginViewModel)
    }
}
