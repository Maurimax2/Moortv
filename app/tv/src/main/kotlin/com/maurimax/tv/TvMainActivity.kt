package com.maurimax.tv

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maurimax.core.data.AppLocale
import com.maurimax.core.designsystem.MaurimaxTvTheme
import com.maurimax.feature.auth.LoginScreenTv
import com.maurimax.feature.auth.LoginViewModel
import com.maurimax.feature.home.HomeScreenTv
import com.maurimax.feature.home.HomeViewModel

class TvMainActivity : ComponentActivity() {

    // Applied before any resource is read, so both the strings and the layout
    // direction come from the chosen language.
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocale.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaurimaxTvTheme {
                MaurimaxTvApp(activity = this)
            }
        }
    }
}

@Composable
private fun MaurimaxTvApp(activity: ComponentActivity) {
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
        LoginScreenTv(
            viewModel = loginViewModel,
            language = AppLocale.resolve(activity),
            onLanguageChange = { language ->
                AppLocale.set(activity, language)
                // Resources and layout direction are bound at attachBaseContext,
                // so the activity has to be rebuilt for the switch to take hold.
                activity.recreate()
            },
        )
    }
}
