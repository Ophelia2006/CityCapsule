package com.y.citycapsule.core.city

import com.y.citycapsule.core.place.GeoPoint

data class CityDefinition(
    val id: String,
    val displayName: String,
    val centerPoint: GeoPoint,
    val supported: Boolean,
    val contentPackVersion: Int
)

object CityRegistry {
    const val DEFAULT_CITY_ID = "cn-shanghai"

    val cities: List<CityDefinition> = listOf(
        CityDefinition(
            id = DEFAULT_CITY_ID,
            displayName = "上海",
            centerPoint = GeoPoint(31.2304, 121.4737),
            supported = true,
            contentPackVersion = 3
        ),
        CityDefinition(
            id = "cn-hangzhou",
            displayName = "杭州",
            centerPoint = GeoPoint(30.2741, 120.1551),
            supported = true,
            contentPackVersion = 2
        )
    )

    val supportedCities: List<CityDefinition> = cities.filter(CityDefinition::supported)

    fun byId(id: String?): CityDefinition? = cities.firstOrNull { it.id == id }

    fun byDisplayName(name: String?): CityDefinition? = cities.firstOrNull {
        it.displayName.equals(name?.trim(), ignoreCase = true)
    }
}

data class ExploreCitySelection(
    val schemaVersion: Int = SCHEMA_VERSION,
    val selectedCityId: String = CityRegistry.DEFAULT_CITY_ID,
    val recentCityIds: List<String> = listOf(CityRegistry.DEFAULT_CITY_ID)
) {
    companion object {
        const val SCHEMA_VERSION = 1
        const val MAX_RECENT_CITIES = 5
        val DEFAULT = ExploreCitySelection()
    }
}
