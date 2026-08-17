package com.y.citycapsule.core.storage

import com.y.citycapsule.core.onboarding.OnboardingDraft
import com.y.citycapsule.core.onboarding.OnboardingDraftCodec
import com.y.citycapsule.core.capsule.CapsuleCatalog
import com.y.citycapsule.core.capsule.CapsuleCatalogCodec
import com.y.citycapsule.core.capsule.CapsuleDraft
import com.y.citycapsule.core.capsule.CapsuleDraftCodec
import com.y.citycapsule.core.favorite.FavoritePlaceIds
import com.y.citycapsule.core.favorite.FavoritePlaceIdsCodec
import com.y.citycapsule.core.place.PlaceCatalog
import com.y.citycapsule.core.place.PlaceCatalogCodec
import com.y.citycapsule.core.profile.LocalProfile
import com.y.citycapsule.core.profile.LocalProfileCodec
import com.y.citycapsule.core.route.LocalRouteCatalog
import com.y.citycapsule.core.route.LocalRouteCatalogCodec
import com.y.citycapsule.core.roaming.RoamingSession
import com.y.citycapsule.core.roaming.RoamingSessionCodec
import com.y.citycapsule.core.track.TrackMetadata
import com.y.citycapsule.core.track.TrackMetadataCodec
import com.y.citycapsule.core.checkin.CheckInCatalog
import com.y.citycapsule.core.checkin.CheckInCodec

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

    object Capsules {
        val CATALOG = StorageKey(
            store = StorageStore.PREFERENCES,
            namespace = "capsules",
            name = "catalog",
            defaultValue = CapsuleCatalog.EMPTY,
            codec = CapsuleCatalogCodec
        )

        val DRAFT = StorageKey(
            store = StorageStore.CACHE,
            namespace = "capsules",
            name = "draft",
            defaultValue = CapsuleDraft.EMPTY,
            codec = CapsuleDraftCodec
        )
    }

    object Routes {
        val CATALOG = StorageKey(
            store = StorageStore.PREFERENCES,
            namespace = "routes",
            name = "catalog",
            defaultValue = LocalRouteCatalog.EMPTY,
            codec = LocalRouteCatalogCodec
        )
    }

    object Roaming {
        val SESSION = StorageKey<RoamingSession>(
            store = StorageStore.PREFERENCES,
            namespace = "roaming",
            name = "session",
            defaultValue = RoamingSession(startedAtEpochMs = 0L, endedAtEpochMs = 0L, status = com.y.citycapsule.core.roaming.RoamingStatus.ENDED),
            codec = RoamingSessionCodec
        )
        val TRACK = StorageKey(
            store = StorageStore.PREFERENCES,
            namespace = "roaming",
            name = "track",
            defaultValue = TrackMetadata(0L),
            codec = TrackMetadataCodec
        )
        val CHECK_INS = StorageKey(store = StorageStore.PREFERENCES, namespace = "roaming", name = "check_ins", defaultValue = CheckInCatalog(sessionStartedAtEpochMs = 0L), codec = CheckInCodec)
    }

    val all: List<StorageKey<*>> = listOf(
        Settings.THEME_MODE,
        Profile.LOCAL_PROFILE,
        Onboarding.COMPLETED_VERSION,
        Onboarding.DRAFT,
        Places.CATALOG,
        Favorites.PLACE_IDS,
        Capsules.CATALOG,
        Capsules.DRAFT,
        Routes.CATALOG,
        Roaming.SESSION,
        Roaming.TRACK,
        Roaming.CHECK_INS
    )
}
