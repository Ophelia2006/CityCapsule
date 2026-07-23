package com.y.citycapsule.core.storage

/** Source-compatible alias; the neutral model now lives outside the storage package. */
typealias ThemeMode = com.y.citycapsule.core.theme.ThemeMode

private object ThemeModeCodec : StorageCodec<ThemeMode> {
    override val valueType: StorageValueType = StorageValueType.STRING

    override fun encode(value: ThemeMode): String = value.wireValue

    override fun decode(encoded: String): ThemeMode? = ThemeMode.fromWireValue(encoded)
}

/**
 * The only registry from which shared business code may obtain persistent keys.
 * Additions require matching Android/HarmonyOS migration review and a documentation update.
 */
object AppStorageKeys {
    object Settings {
        val THEME_MODE = StorageKey(
            store = StorageStore.PREFERENCES,
            namespace = "settings",
            name = "theme_mode",
            defaultValue = ThemeMode.SYSTEM,
            codec = ThemeModeCodec
        )
    }

    val all: List<StorageKey<*>> = listOf(
        Settings.THEME_MODE
    )
}
