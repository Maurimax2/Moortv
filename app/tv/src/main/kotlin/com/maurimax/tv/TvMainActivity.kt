package com.maurimax.tv

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maurimax.core.data.AppLocale
import com.maurimax.core.data.AppThemeStore
import com.maurimax.core.data.ThemeMode
import com.maurimax.core.designsystem.MaurimaxTvTheme
import com.maurimax.core.model.MediaItem
import com.maurimax.feature.auth.LoginScreenTv
import com.maurimax.feature.auth.LoginViewModel
import com.maurimax.feature.home.DetailScreenTv
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
            // Dark is the default — channel logos arrive baked on black, so a
            // light page frames every one of them in a grey box. The choice is
            // remembered and applied without recreating the activity.
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
        var opened by remember { mutableStateOf<MediaItem?>(null) }

        val homeViewModel: HomeViewModel = viewModel(
            key = "home-${credentials.username}",
            factory = HomeViewModel.factory(credentials),
        )

        // Playback runs in another activity, so progress lands while this one
        // is stopped. Without re-reading on resume, the continue-watching row
        // would still show what the customer just finished.
        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) homeViewModel.refreshLibrary()
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        // Observed rather than read once: a download's progress has to reach
        // the page it is showing on.
        val homeState by homeViewModel.uiState.collectAsStateWithLifecycle()

        val play: (MediaItem) -> Unit = { item ->
            if (item.isPlayable) {
                activity.startActivity(PlayerActivity.intent(activity, item))
            }
        }

        val chosen = opened
        if (chosen != null) {
            BackHandler { opened = null }
            DetailScreenTv(
                item = chosen,
                onPlay = play,
                isFavourite = homeViewModel.isFavourite(chosen),
                onToggleFavourite = { homeViewModel.toggleFavourite(chosen) },
                download = homeState.downloads.firstOrNull { it.item.id == chosen.id },
                onDownload = { homeViewModel.download(chosen) },
                onRemoveDownload = { homeViewModel.removeDownload(chosen) },
                onRefresh = homeViewModel::refreshLibrary,
                onBack = { opened = null },
            )
        } else {
            HomeScreenTv(
                viewModel = homeViewModel,
                account = credentials.username,
                // Signing out keeps the account on the device, so this is a
                // switch back to the list rather than a goodbye.
                onSwitchAccount = loginViewModel::signOut,
                // A channel plays on OK — nobody wants a page about a channel.
                // Films and series open their own page first.
                onItemClick = { item -> if (item.isLive) play(item) else opened = item },
            )
        }
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
