package com.y.citycapsule.core.storage

import com.y.citycapsule.core.onboarding.OnboardingDraft
import com.y.citycapsule.core.onboarding.OnboardingDraftCodec
import com.y.citycapsule.core.favorite.FavoritePlaceIds
import com.y.citycapsule.core.favorite.FavoritePlaceIdsCodec
import com.y.citycapsule.core.place.PlaceCatalog
import com.y.citycapsule.core.place.PlaceCatalogCodec
import com.y.citycapsule.core.profile.LocalProfile
import com.y.citycapsule.core.profile.LocalProfileCodec

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

    object Profile {
        val LOCAL_PROFILE = StorageKey(
            store = StorageStore.PREFERENCES,
            namespace = "profile",
            name = "local_profile",
            defaultValue = LocalProfile.DEFAULT,
            codec = LocalProfileCodec
        )
    }

    object Onboarding {
        val COMPLETED_VERSION = StorageKey(
            store = StorageStore.PREFERENCES,
            namespace = "onboarding",
            name = "completed_version",
            defaultValue = 0L,
            codec = StorageCodecs.LONG
        )

        val DRAFT = StorageKey(
            store = StorageStore.CACHE,
            namespace = "onboarding",
            name = "draft",
            defaultValue = OnboardingDraft.EMPTY,
            codec = OnboardingDraftCodec
        )
    }

    object Places {
        val CATALOG = StorageKey(
            store = StorageStore.PREFERENCES,
            namespace = "places",
            name = "catalog",
            defaultValue = PlaceCatalog.EMPTY,
            codec = PlaceCatalogCodec
        )
    }

    object Favorites {
        val PLACE_IDS = StorageKey(
            store = StorageStore.PREFERENCES,
            namespace = "favorites",
            name = "place_ids",
            defaultValue = FavoritePlaceIds.EMPTY,
            codec = FavoritePlaceIdsCodec
        )
    }

    val all: List<StorageKey<*>> = listOf(
        Settings.THEME_MODE,
        Profile.LOCAL_PROFILE,
        Onboarding.COMPLETED_VERSION,
        Onboarding.DRAFT,
        Places.CATALOG,
        Favorites.PLACE_IDS
    )
}
