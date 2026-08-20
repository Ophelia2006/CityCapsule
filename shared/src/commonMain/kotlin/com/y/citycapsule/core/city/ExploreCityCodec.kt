package com.y.citycapsule.core.city

import com.tencent.kuikly.core.nvi.serialization.json.JSONArray
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.y.citycapsule.core.storage.StorageCodec
import com.y.citycapsule.core.storage.StorageValueType

object ExploreCityCodec : StorageCodec<ExploreCitySelection> {
    override val valueType = StorageValueType.JSON_OBJECT

    override fun encode(value: ExploreCitySelection): String {
        val normalized = requireNotNull(normalize(value))
        return JSONObject().apply {
            put("schemaVersion", ExploreCitySelection.SCHEMA_VERSION)
            put("selectedCityId", normalized.selectedCityId)
            put("recentCityIds", JSONArray().apply { normalized.recentCityIds.forEach(::put) })
            normalized.selectedCityOverride?.let { city ->
                put("selectedCityOverride", JSONObject().apply {
                    put("id", city.id)
                    put("displayName", city.displayName)
                    put("latitude", city.centerPoint.latitude)
                    put("longitude", city.centerPoint.longitude)
                    put("supported", city.supported)
                    put("contentPackVersion", city.contentPackVersion)
                })
            }
        }.toString()
    }

    override fun decode(encoded: String): ExploreCitySelection? {
        return try {
            val json = JSONObject(encoded)
            val schemaVersion = json.optInt("schemaVersion", -1)
            if (schemaVersion !in 1..ExploreCitySelection.SCHEMA_VERSION) return null
            val recentJson = json.optJSONArray("recentCityIds") ?: return null
            val recent = buildList {
                for (index in 0 until recentJson.length()) add(recentJson.optString(index) ?: return null)
            }
            normalize(
                ExploreCitySelection(
                    selectedCityId = json.optString("selectedCityId"),
                    recentCityIds = recent,
                    selectedCityOverride = if (schemaVersion >= 2) {
                        json.optJSONObject("selectedCityOverride")?.let { city ->
                            val name = city.optString("displayName").trim()
                            val id = city.optString("id").trim()
                            val latitude = city.optDouble("latitude", Double.NaN)
                            val longitude = city.optDouble("longitude", Double.NaN)
                            if (name.isEmpty() || id.isEmpty() || !latitude.isFinite() || !longitude.isFinite()) null
                            else CityDefinition(
                                id = id,
                                displayName = name,
                                centerPoint = com.y.citycapsule.core.place.GeoPoint(latitude, longitude),
                                supported = city.optBoolean("supported", false),
                                contentPackVersion = city.optInt("contentPackVersion", 0)
                            )
                        }
                    } else null
                )
            )
        } catch (_: Throwable) {
            null
        }
    }

    private fun normalize(value: ExploreCitySelection): ExploreCitySelection? {
        val selected = value.selectedCityId.trim()
        val override = value.selectedCityOverride?.takeIf {
            it.id == selected && it.displayName.isNotBlank()
        }
        if (CityRegistry.byId(selected) == null && override == null) return null
        val recent = (listOf(selected) + value.recentCityIds)
            .map(String::trim)
            .distinct()
            .filter { CityRegistry.byId(it) != null }
            .take(ExploreCitySelection.MAX_RECENT_CITIES)
        return ExploreCitySelection(
            selectedCityId = selected,
            recentCityIds = recent,
            selectedCityOverride = override
        )
    }
}
