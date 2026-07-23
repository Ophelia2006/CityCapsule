package com.y.citycapsule.core.theme

/**
 * User-facing theme preference shared by storage, the design system, and both native hosts.
 *
 * [wireValue] is part of storage protocol v1 and must not be renamed without a migration.
 */
enum class ThemeMode(val wireValue: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromWireValue(value: String): ThemeMode? = entries.firstOrNull {
            it.wireValue == value
        }
    }
}
