package com.satya.uno

import android.app.Application
import com.satya.uno.data.Auth
import com.satya.uno.data.Cloud
import com.satya.uno.data.Net

class UnoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Net.init(this)
        Cloud.init(this)
        Auth.init()
    }
}
