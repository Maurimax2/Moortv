package com.maurimax.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maurimax.core.designsystem.MaurimaxTvTheme
import com.maurimax.feature.auth.LoginScreenTv
import com.maurimax.feature.auth.LoginViewModel
import com.maurimax.feature.home.HomeScreenTv
import com.maurimax.feature.home.HomeViewModel

class TvMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaurimaxTvTheme {
                MaurimaxTvApp()
            }
        }
    }
}

@Composable
private fun MaurimaxTvApp() {
    val loginViewModel: LoginViewModel = viewModel(factory = LoginViewModel.Factory)
    val login by loginViewModel.uiState.collectAsStateWithLifecycle()

    val credentials = login.credentials
    if (login.signedIn && credentials != null) {
        HomeScreenTv(
            viewModel = viewModel(
                key = "home-${credentials.username}",
                factory = HomeViewModel.factory(credentials),
            ),
        )
    } else {
        LoginScreenTv(viewModel = loginViewModel)
    }
}
