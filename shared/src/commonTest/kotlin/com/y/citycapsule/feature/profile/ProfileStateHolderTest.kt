package com.y.citycapsule.feature.profile

import com.y.citycapsule.core.storage.InMemoryKeyValueStore
import com.y.citycapsule.core.storage.StorageResult
import com.y.citycapsule.core.capsule.CapsuleDraft
import com.y.citycapsule.core.capsule.LocalCapsuleRepository
import com.y.citycapsule.core.favorite.LocalFavoriteRepository
import com.y.citycapsule.core.place.LocalPlaceRepository
import com.y.citycapsule.core.profile.AvatarPreset
import com.y.citycapsule.core.profile.LocalProfile
import com.y.citycapsule.core.profile.LocalProfileRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProfileOverviewStoreTest {

    @Test
    fun load_aggregatesPublishedCapsulesDistinctVisitedPlacesFavoritesAndCities() = runTest {
        val fixture = ProfileFixture()
        fixture.profileRepository.saveProfile(profile("Ophelia", "上海")) {}
        fixture.favoriteRepository.setFavorite("seed_shanghai_museum", true) {}
        fixture.publish(placeId = "seed_shanghai_museum", content = "博物馆一")
        fixture.publish(placeId = "seed_shanghai_museum", content = "博物馆二")
        fixture.publish(placeId = "seed_west_lake", content = "西湖")
        val store = fixture.overviewStore(this)

        store.dispatch(ProfileOverviewIntent.Load)
        advanceUntilIdle()

        val state = store.state.value
        assertEquals(ProfileOverviewStatus.READY, state.status)
        assertEquals("Ophelia", state.profile.displayName)
        assertEquals(3, state.memoryCount)
        assertEquals(2, state.visitedPlaceCount)
        assertEquals(1, state.wantToCount)
        assertEquals(listOf("seed_shanghai_museum"), state.wantToPlaces.map { it.id })
        assertEquals(
            listOf(
                ProfileCityFootprint(city = "上海", placeCount = 1, memoryCount = 2),
                ProfileCityFootprint(city = "杭州", placeCount = 1, memoryCount = 1),
            ),
            state.cityFootprints,
        )
        store.dispose()
    }

    @Test
    fun removeWantTo_updatesOverviewAndEmitsFavoritesChanged() = runTest {
        val fixture = ProfileFixture()
        fixture.favoriteRepository.setFavorite("seed_shanghai_museum", true) {}
        val store = fixture.overviewStore(this)
        store.dispatch(ProfileOverviewIntent.Load)
        advanceUntilIdle()
        val effect = async { store.effects.first() }

        store.dispatch(ProfileOverviewIntent.FavoriteToggled("seed_shanghai_museum"))
        advanceUntilIdle()

        assertEquals(0, store.state.value.wantToCount)
        assertTrue(store.state.value.wantToPlaces.isEmpty())
        assertIs<ProfileOverviewEffect.FavoritesChanged>(effect.await())
        store.dispose()
    }

    @Test
    fun unresolvedHistoricalPlace_countsAsVisitedButNotAsCityFootprint() = runTest {
        val fixture = ProfileFixture()
        fixture.publish(placeId = "removed_place", content = "旧地点")
        val store = fixture.overviewStore(this)

        store.dispatch(ProfileOverviewIntent.Load)
        advanceUntilIdle()

        assertEquals(1, store.state.value.memoryCount)
        assertEquals(1, store.state.value.visitedPlaceCount)
        assertEquals(1, store.state.value.unresolvedVisitedPlaceCount)
        assertTrue(store.state.value.cityFootprints.isEmpty())
        store.dispose()
    }

    @Test
    fun navigationIntents_areDeliveredAsOneShotEffects() = runTest {
        val fixture = ProfileFixture()
        val store = fixture.overviewStore(this)
        val editEffect = async { store.effects.first() }
        store.dispatch(ProfileOverviewIntent.EditProfileClicked)
        assertIs<ProfileOverviewEffect.NavigateToEdit>(editEffect.await())
        store.dispose()
    }
}

class ProfileEditorStoreTest {

    @Test
    fun save_trimsFieldsPersistsProfileAndEmitsSaved() = runTest {
        val repository = LocalProfileRepository(InMemoryKeyValueStore())
        val store = ProfileEditorStore(repository, this)
        store.dispatch(ProfileEditorIntent.Load)
        advanceUntilIdle()
        store.dispatch(ProfileEditorIntent.DisplayNameChanged("  Ophelia  "))
        store.dispatch(ProfileEditorIntent.HomeCityChanged("  上海  "))
        val effect = async { store.effects.first() }

        store.dispatch(ProfileEditorIntent.SaveClicked)
        advanceUntilIdle()

        assertIs<ProfileEditorEffect.SavedAndNavigateBack>(effect.await())
        repository.getProfile { result ->
            assertEquals(
                profile("Ophelia", "上海"),
                assertIs<StorageResult.Success<LocalProfile>>(result).value,
            )
        }
        assertFalse(store.state.value.isDirty)
        store.dispose()
    }

    @Test
    fun backWithUnsavedChanges_requiresConfirmation() = runTest {
        val repository = LocalProfileRepository(InMemoryKeyValueStore())
        val store = ProfileEditorStore(repository, this)
        store.dispatch(ProfileEditorIntent.Load)
        advanceUntilIdle()
        store.dispatch(ProfileEditorIntent.DisplayNameChanged("新的昵称"))
        advanceUntilIdle()

        store.dispatch(ProfileEditorIntent.BackClicked)
        advanceUntilIdle()

        assertTrue(store.state.value.showDiscardConfirmation)
        store.dispose()
    }

    @Test
    fun emptyNickname_isRejectedWithoutPersisting() = runTest {
        val repository = LocalProfileRepository(InMemoryKeyValueStore())
        val store = ProfileEditorStore(repository, this)
        store.dispatch(ProfileEditorIntent.Load)
        advanceUntilIdle()
        store.dispatch(ProfileEditorIntent.DisplayNameChanged("   "))

        store.dispatch(ProfileEditorIntent.SaveClicked)
        advanceUntilIdle()

        assertTrue(store.state.value.validationMessage != null)
        assertEquals(ProfileEditorStatus.READY, store.state.value.status)
        repository.getProfile { result ->
            assertIs<com.y.citycapsule.core.storage.StorageResult.Missing>(result)
        }
        store.dispose()
    }

    @Test
    fun load_doesNotCreateDirtyState() = runTest {
        val repository = LocalProfileRepository(InMemoryKeyValueStore())
        repository.saveProfile(profile("Ophelia", "上海")) {}
        val store = ProfileEditorStore(repository, this)

        store.dispatch(ProfileEditorIntent.Load)
        advanceUntilIdle()

        assertEquals("Ophelia", store.state.value.profile.displayName)
        assertEquals("上海", store.state.value.profile.homeCity)
        assertFalse(store.state.value.isDirty)
        assertNull(store.state.value.notice)
        store.dispose()
    }
}

private class ProfileFixture {
    private val storage = InMemoryKeyValueStore()
    val profileRepository = LocalProfileRepository(storage)
    val placeRepository = LocalPlaceRepository(storage)
    val favoriteRepository = LocalFavoriteRepository(storage, placeRepository)
    val capsuleRepository = LocalCapsuleRepository(storage)

    fun overviewStore(scope: CoroutineScope) = ProfileOverviewStore(
        profileRepository = profileRepository,
        placeRepository = placeRepository,
        favoriteRepository = favoriteRepository,
        capsuleRepository = capsuleRepository,
        parentScope = scope,
    )

    fun publish(placeId: String, content: String) {
        capsuleRepository.publish(
            CapsuleDraft(
                placeId = placeId,
                content = content,
            ),
        ) { result ->
            assertIs<StorageResult.Success<*>>(result)
        }
    }
}

private fun profile(name: String, city: String) = LocalProfile(
    displayName = name,
    avatarPreset = AvatarPreset.SKY,
    homeCity = city,
)
