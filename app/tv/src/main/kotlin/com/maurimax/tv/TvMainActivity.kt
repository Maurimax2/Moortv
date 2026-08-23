package com.maurimax.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maurimax.core.designsystem.MaurimaxTvTheme
import com.maurimax.feature.home.HomeScreenTv
import com.maurimax.feature.home.HomeViewModel

class TvMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaurimaxTvTheme {
                HomeScreenTv(
                    viewModel = viewModel(factory = HomeViewModel.Factory),
                )
            }
        }
    }
}
