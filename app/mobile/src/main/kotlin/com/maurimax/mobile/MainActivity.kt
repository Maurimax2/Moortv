package com.maurimax.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maurimax.core.designsystem.MaurimaxMobileTheme
import com.maurimax.feature.home.HomeScreenMobile
import com.maurimax.feature.home.HomeViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaurimaxMobileTheme {
                HomeScreenMobile(
                    viewModel = viewModel(factory = HomeViewModel.Factory),
                )
            }
        }
    }
}
