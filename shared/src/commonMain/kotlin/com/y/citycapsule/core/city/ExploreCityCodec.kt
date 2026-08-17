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
        }.toString()
    }

    override fun decode(encoded: String): ExploreCitySelection? {
        return try {
            val json = JSONObject(encoded)
            if (json.optInt("schemaVersion", -1) != ExploreCitySelection.SCHEMA_VERSION) return null
            val recentJson = json.optJSONArray("recentCityIds") ?: return null
            val recent = buildList {
                for (index in 0 until recentJson.length()) add(recentJson.optString(index) ?: return null)
            }
            normalize(
                ExploreCitySelection(
                    selectedCityId = json.optString("selectedCityId"),
                    recentCityIds = recent
                )
            )
        } catch (_: Throwable) {
            null
        }
    }

    private fun normalize(value: ExploreCitySelection): ExploreCitySelection? {
        if (value.schemaVersion != ExploreCitySelection.SCHEMA_VERSION) return null
        val selected = value.selectedCityId.trim()
        if (CityRegistry.byId(selected)?.supported != true) return null
        val recent = (listOf(selected) + value.recentCityIds)
            .map(String::trim)
            .distinct()
            .filter { CityRegistry.byId(it)?.supported == true }
            .take(ExploreCitySelection.MAX_RECENT_CITIES)
        return ExploreCitySelection(selectedCityId = selected, recentCityIds = recent)
    }
}
