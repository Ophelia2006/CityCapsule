package com.y.citycapsule.core.storage

/**
 * Frozen cross-platform migration contract.
 *
 * The ArkTS implementation mirrors these wire values because platform migration runs before
 * the Kuikly bridge is available. Business code must never access the META keys directly.
 */
object StorageMigrationContract {
    const val CURRENT_SCHEMA_VERSION = 1
    const val MAX_ATTEMPTS = 3

    const val META_SCHEMA_VERSION = "storage.schema_version"
    const val META_STATE = "storage.migration_state"
    const val META_ATTEMPTS = "storage.migration_attempts"
    const val META_LAST_ERROR = "storage.migration_last_error"

    const val TYPE_METADATA_PREFIX = "__cc_type__."

    const val LEGACY_SETTINGS_STORE = "city_capsule_settings"
    const val LEGACY_THEME_MODE = "theme_mode"

    const val TARGET_THEME_MODE = "settings.theme_mode"
    const val TARGET_THEME_MODE_TYPE = "string"

    fun normalizeLegacyThemeMode(value: String?): String? = value
        ?.trim()
        ?.lowercase()
        ?.takeIf { candidate -> ThemeMode.entries.any { it.wireValue == candidate } }
}

enum class StorageMigrationState(val wireValue: String) {
    NOT_STARTED("not_started"),
    RUNNING("running"),
    COMPLETED("completed"),
    FAILED("failed");

    companion object {
        fun fromWireValue(value: String?): StorageMigrationState = entries.firstOrNull {
            it.wireValue == value
        } ?: NOT_STARTED
    }
}
