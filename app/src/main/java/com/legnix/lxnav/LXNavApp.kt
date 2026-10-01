package com.legnix.lxnav

import android.app.Application
import com.legnix.lxnav.data.Prefs

class LXNavApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        Prefs.load(this)
    }

    companion object {
        lateinit var instance: LXNavApp
            private set
    }
}
