package com.y.citycapsule

import android.app.Application
import android.util.Log
import com.y.citycapsule.storage.AndroidMmkvStorage
import com.y.citycapsule.storage.AndroidSharedPreferencesLegacySettingsSource
import com.y.citycapsule.storage.AndroidStorageMigrator

class KRApplication : Application() {

    init {
        application = this
    }

    override fun onCreate() {
        super.onCreate()
        val initialization = AndroidMmkvStorage.initialize(this)
        if (initialization.success) {
            runCatching {
                AndroidStorageMigrator(
                    AndroidMmkvStorage,
                    AndroidSharedPreferencesLegacySettingsSource(this)
                ).migrate()
            }.onSuccess { migration ->
                Log.i(
                    "CityCapsuleStorage",
                    "Storage migration outcome=${migration.outcome}, attempt=${migration.attempt}, " +
                        "migratedTheme=${migration.migratedTheme}, reason=${migration.reason.orEmpty()}"
                )
            }.onFailure {
                Log.e("CityCapsuleStorage", "Storage migration skipped safely.")
            }
        }
    }

    companion object {
        lateinit var application: Application
    }
}
