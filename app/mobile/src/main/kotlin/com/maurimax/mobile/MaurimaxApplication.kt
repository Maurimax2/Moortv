package com.maurimax.mobile

import android.app.Application
import com.maurimax.core.data.Graph

class MaurimaxApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Graph.init(this)
    }
}
