package com.example.sensus

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SensusApp : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
