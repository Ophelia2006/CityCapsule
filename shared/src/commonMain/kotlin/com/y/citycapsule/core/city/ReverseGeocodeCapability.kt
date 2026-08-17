package com.y.citycapsule.core.city

import com.y.citycapsule.core.location.GeoDistance
import com.y.citycapsule.core.place.GeoPoint

sealed interface ReverseGeocodeResult {
    data class SupportedCity(val city: CityDefinition) : ReverseGeocodeResult
    data object UnsupportedCity : ReverseGeocodeResult
    data class Failure(val message: String) : ReverseGeocodeResult
}

fun interface ReverseGeocodeCapability {
    fun resolve(point: GeoPoint, callback: (ReverseGeocodeResult) -> Unit)
}

/**
 * Offline city resolver for the bundled content cities. It intentionally does not claim to
 * resolve an address or arbitrary administrative area.
 */
object SupportedCityReverseGeocoder : ReverseGeocodeCapability {
    override fun resolve(point: GeoPoint, callback: (ReverseGeocodeResult) -> Unit) {
        val nearest = CityRegistry.supportedCities
            .map { city -> city to GeoDistance.meters(point, city.centerPoint) }
            .minByOrNull { it.second }
        callback(
            if (nearest != null && nearest.second <= SUPPORTED_CITY_RADIUS_METERS) {
                ReverseGeocodeResult.SupportedCity(nearest.first)
            } else {
                ReverseGeocodeResult.UnsupportedCity
            }
        )
    }

    private const val SUPPORTED_CITY_RADIUS_METERS = 80_000.0
}
