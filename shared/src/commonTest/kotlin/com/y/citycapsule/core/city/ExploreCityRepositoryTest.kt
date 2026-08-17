package com.y.citycapsule.core.city

import com.y.citycapsule.core.place.GeoPoint
import com.y.citycapsule.core.storage.InMemoryKeyValueStore
import com.y.citycapsule.core.storage.StorageResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ExploreCityRepositoryTest {
    @Test
    fun initializesShanghaiAndPersistsRecentSelections() {
        val repository = LocalExploreCityRepository(InMemoryKeyValueStore())
        var first: ExploreCitySelection? = null
        repository.get { first = (it as? StorageResult.Success)?.value }
        assertEquals("cn-shanghai", first?.selectedCityId)

        repository.select("cn-hangzhou") { }
        var restored: ExploreCitySelection? = null
        repository.get { restored = (it as? StorageResult.Success)?.value }
        assertEquals("cn-hangzhou", restored?.selectedCityId)
        assertEquals(listOf("cn-hangzhou", "cn-shanghai"), restored?.recentCityIds)
    }

    @Test
    fun rejectsUnsupportedCityWithoutOverwritingSelection() {
        val repository = LocalExploreCityRepository(InMemoryKeyValueStore())
        var result: StorageResult<ExploreCitySelection>? = null
        repository.select("unknown") { result = it }
        assertIs<StorageResult.Failure>(result)
        repository.get { result = it }
        assertEquals("cn-shanghai", (result as StorageResult.Success).value.selectedCityId)
    }

    @Test
    fun knownCityWithoutContentCanBeSelectedForHonestEmptyState() {
        val repository = LocalExploreCityRepository(InMemoryKeyValueStore())
        var result: StorageResult<ExploreCitySelection>? = null
        repository.select("cn-beijing") { result = it }
        assertEquals("cn-beijing", (result as StorageResult.Success).value.selectedCityId)
        assertEquals(false, CityRegistry.byId("cn-beijing")?.supported)
    }

    @Test
    fun offlineReverseGeocoderOnlyReturnsSupportedNearbyCity() {
        var shanghai: ReverseGeocodeResult? = null
        SupportedCityReverseGeocoder.resolve(GeoPoint(31.2304, 121.4737)) { shanghai = it }
        assertEquals("cn-shanghai", (shanghai as ReverseGeocodeResult.SupportedCity).city.id)

        var elsewhere: ReverseGeocodeResult? = null
        SupportedCityReverseGeocoder.resolve(GeoPoint(39.9042, 116.4074)) { elsewhere = it }
        assertIs<ReverseGeocodeResult.UnsupportedCity>(elsewhere)
    }
}
