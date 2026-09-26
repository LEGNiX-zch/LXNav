package com.legnix.lxnav

import android.app.Application

class LXNavApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: LXNavApp
            private set
    }
}
