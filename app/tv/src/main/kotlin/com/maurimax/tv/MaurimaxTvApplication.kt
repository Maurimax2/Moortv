package com.maurimax.tv

import android.app.Application
import com.maurimax.core.data.Graph

class MaurimaxTvApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Graph.init(this)
    }
}
