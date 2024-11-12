package com.iiitl.locateme

import android.app.Application
import com.iiitl.locateme.utils.DeviceUuidManager

class BeaconifyApplication : Application() {
    lateinit var deviceUuidManager: DeviceUuidManager
        private set

    override fun onCreate() {
        super.onCreate()
        deviceUuidManager = DeviceUuidManager(this)
    }

    companion object {
        private lateinit var instance: BeaconifyApplication

        fun getInstance(): BeaconifyApplication {
            return instance
        }
    }

    init {
        instance = this
    }
}