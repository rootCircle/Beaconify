package com.iiitl.locateme.utils

import android.content.Context
import com.iiitl.locateme.BeaconifyApplication

fun Context.getDeviceUuid(): String {
    return BeaconifyApplication.getInstance().deviceUuidManager.getDeviceUuid()
}