package com.maurimax.tv

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maurimax.core.data.AppLocale
import com.maurimax.core.data.AppThemeStore
import com.maurimax.core.data.ThemeMode
import com.maurimax.core.designsystem.MaurimaxTvTheme
import com.maurimax.feature.auth.LoginScreenTv
import com.maurimax.feature.auth.LoginViewModel
import com.maurimax.feature.home.HomeScreenTv
import com.maurimax.feature.home.HomeViewModel
import com.maurimax.feature.player.PlayerActivity

class TvMainActivity : ComponentActivity() {

    // Applied before any resource is read, so both the strings and the layout
    // direction come from the chosen language.
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocale.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Light is the default; the choice is remembered and applied
            // without recreating the activity.
            var mode by remember { mutableStateOf(AppThemeStore.load(this)) }

            MaurimaxTvTheme(dark = mode == ThemeMode.DARK) {
                MaurimaxTvApp(
                    activity = this,
                    theme = mode,
                    onThemeChange = { chosen ->
                        mode = chosen
                        AppThemeStore.save(this, chosen)
                    },
                )
            }
        }
    }
}

@Composable
private fun MaurimaxTvApp(
    activity: ComponentActivity,
    theme: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
) {
    val loginViewModel: LoginViewModel = viewModel(factory = LoginViewModel.Factory)
    val login by loginViewModel.uiState.collectAsStateWithLifecycle()

    val credentials = login.credentials
    if (login.signedIn && credentials != null) {
        HomeScreenTv(
            viewModel = viewModel(
                key = "home-${credentials.username}",
                factory = HomeViewModel.factory(credentials),
            ),
            onItemClick = { item ->
                // A series is a container, not a stream; its episode list comes next.
                if (item.isPlayable) {
                    activity.startActivity(
                        PlayerActivity.intent(
                            context = activity,
                            url = item.playbackUrl,
                            title = item.title,
                            isLive = item.isLive,
                        ),
                    )
                }
            },
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
            theme = theme,
            onThemeChange = onThemeChange,
        )
    }
}
