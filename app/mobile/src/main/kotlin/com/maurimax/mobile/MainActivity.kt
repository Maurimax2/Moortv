package com.maurimax.mobile

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maurimax.core.data.AppLocale
import com.maurimax.core.data.AppThemeStore
import com.maurimax.core.data.ThemeMode
import com.maurimax.core.designsystem.MaurimaxMobileTheme
import com.maurimax.feature.auth.LoginScreenMobile
import com.maurimax.feature.auth.LoginViewModel
import com.maurimax.feature.home.HomeScreenMobile
import com.maurimax.feature.home.HomeViewModel
import com.maurimax.feature.player.PlayerActivity

class MainActivity : ComponentActivity() {

    // Applied before any resource is read, so both the strings and the layout
    // direction come from the chosen language.
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocale.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Light is the default; the choice is remembered and applied
            // without recreating the activity.
            var mode by remember { mutableStateOf(AppThemeStore.load(this)) }

            // System bar icons invert with the theme; white glyphs on a light
            // status bar are invisible.
            val dark = mode == ThemeMode.DARK
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }

            MaurimaxMobileTheme(dark = dark) {
                MaurimaxMobileApp(
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

/**
 * Sign-in gate. Saved credentials are re-submitted on launch, so a returning
 * customer sees the login screen only long enough for that to come back.
 */
@Composable
private fun MaurimaxMobileApp(
    activity: ComponentActivity,
    theme: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
) {
    val loginViewModel: LoginViewModel = viewModel(factory = LoginViewModel.Factory)
    val login by loginViewModel.uiState.collectAsStateWithLifecycle()

    val credentials = login.credentials
    if (login.signedIn && credentials != null) {
        HomeScreenMobile(
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
        LoginScreenMobile(
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
