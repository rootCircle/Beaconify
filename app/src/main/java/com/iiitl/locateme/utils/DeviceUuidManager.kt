// utils/DeviceUuidManager.kt
package com.iiitl.locateme.utils

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

class DeviceUuidManager(context: Context) {
    private val preferences: SharedPreferences = context.getSharedPreferences(
        PREF_NAME,
        Context.MODE_PRIVATE
    )

    fun getDeviceUuid(): String {
        var uuid = preferences.getString(KEY_UUID, null)

        if (uuid == null) {
            uuid = UUID.randomUUID().toString()
            preferences.edit()
                .putString(KEY_UUID, uuid)
                .apply()
        }

        return uuid
    }

    companion object {
        private const val PREF_NAME = "device_uuid_prefs"
        private const val KEY_UUID = "device_uuid"
    }
}