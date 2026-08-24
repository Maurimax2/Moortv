package com.maurimax.tv

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.maurimax.core.data.Graph
import com.maurimax.core.designsystem.maurimaxImageLoader

class MaurimaxTvApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        Graph.init(this)
    }

    /** Coil picks this up for every AsyncImage in the app. */
    override fun newImageLoader(): ImageLoader = maurimaxImageLoader(this)
}
