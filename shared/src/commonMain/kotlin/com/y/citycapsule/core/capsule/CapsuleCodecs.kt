package com.y.citycapsule.core.capsule

import com.tencent.kuikly.core.nvi.serialization.json.JSONArray
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.y.citycapsule.core.storage.StorageCodec
import com.y.citycapsule.core.storage.StorageValueType

object CapsuleCatalogCodec : StorageCodec<CapsuleCatalog> {
    override val valueType = StorageValueType.JSON_OBJECT

    override fun encode(value: CapsuleCatalog): String {
        require(value.schemaVersion == CapsuleContract.SCHEMA_VERSION)
        require(value.capsules.size <= CapsuleContract.MAX_CATALOG_SIZE)
        val normalized = value.capsules.map { requireNotNull(CapsuleValidator.normalize(it)) }
        require(normalized.map(CityCapsule::id).distinct().size == normalized.size)
        return JSONObject().apply {
            put("schemaVersion", value.schemaVersion)
            put("capsules", JSONArray().also { array ->
                normalized.forEach { array.put(it.toJson()) }
            })
        }.toString()
    }

    override fun decode(encoded: String): CapsuleCatalog? {
        return try {
            val json = JSONObject(encoded)
            val schemaVersion = json.optInt("schemaVersion", -1)
            if (schemaVersion !in setOf(CapsuleContract.LEGACY_SCHEMA_VERSION, CapsuleContract.SCHEMA_VERSION)) return null
            val array = json.optJSONArray("capsules") ?: return null
            if (array.length() > CapsuleContract.MAX_CATALOG_SIZE) return null
            val capsules = buildList {
                for (index in 0 until array.length()) {
                    add(array.optJSONObject(index)?.toCapsule() ?: return null)
                }
            }
            if (capsules.map(CityCapsule::id).distinct().size != capsules.size) return null
            CapsuleCatalog(capsules = capsules.map { it.copy(schemaVersion = CapsuleContract.SCHEMA_VERSION) })
        } catch (_: Throwable) {
            null
        }
    }
}

object CapsuleDraftCodec : StorageCodec<CapsuleDraft> {
    override val valueType = StorageValueType.JSON_OBJECT

    override fun encode(value: CapsuleDraft): String = JSONObject().apply {
        put("schemaVersion", value.schemaVersion)
        value.capsuleId?.let { put("capsuleId", it) }
        put("content", value.content)
        value.mood?.let { put("mood", it.wireValue) }
        put("tags", JSONArray().also { array -> value.tags.forEach(array::put) })
        value.placeId?.let { put("placeId", it) }
        value.roamingSessionId?.let { put("roamingSessionId", it) }
        put("imagePaths", JSONArray().also { array -> value.imagePaths.forEach(array::put) })
        put("updatedAtEpochMs", value.updatedAtEpochMs.toString())
    }.toString()

    override fun decode(encoded: String): CapsuleDraft? {
        return try {
            val json = JSONObject(encoded)
            val schemaVersion = json.optInt("schemaVersion", -1)
            if (schemaVersion !in setOf(CapsuleContract.LEGACY_SCHEMA_VERSION, CapsuleContract.SCHEMA_VERSION)) return null
            val tagsJson = json.optJSONArray("tags") ?: return null
            val tags = buildList {
                for (index in 0 until tagsJson.length()) add(tagsJson.optString(index) ?: return null)
            }
            val moodWire = json.optString("mood")
            val mood = if (moodWire.isBlank()) null else CapsuleMood.fromWireValue(moodWire) ?: return null
            val imagesJson = json.optJSONArray("imagePaths") ?: JSONArray()
            val images = buildList {
                for (index in 0 until imagesJson.length()) add(imagesJson.optString(index) ?: return null)
            }
            val draft = CapsuleDraft(
                capsuleId = json.optString("capsuleId").takeIf(String::isNotBlank),
                content = json.optString("content"),
                mood = mood,
                tags = tags,
                placeId = json.optString("placeId").takeIf(String::isNotBlank),
                roamingSessionId = json.optString("roamingSessionId").takeIf(String::isNotBlank),
                imagePaths = images,
                updatedAtEpochMs = json.optString("updatedAtEpochMs").toLongOrNull() ?: return null
            )
            if (draft.content.length > CapsuleContract.CONTENT_MAX_LENGTH ||
                draft.tags.size > CapsuleContract.TAG_MAX_COUNT ||
                draft.tags.any { it.isBlank() || it.length > CapsuleContract.TAG_MAX_LENGTH } ||
                draft.imagePaths.size > CapsuleContract.IMAGE_MAX_COUNT ||
                draft.imagePaths.any { it.isBlank() || it.length > CapsuleContract.IMAGE_PATH_MAX_LENGTH }
            ) null else draft
        } catch (_: Throwable) {
            null
        }
    }
}

private fun CityCapsule.toJson(): JSONObject = JSONObject().apply {
    put("schemaVersion", schemaVersion)
    put("id", id)
    put("content", content)
    mood?.let { put("mood", it.wireValue) }
    put("tags", JSONArray().also { array -> tags.forEach(array::put) })
    put("placeId", placeId)
    roamingSessionId?.let { put("roamingSessionId", it) }
    put("imagePaths", JSONArray().also { array -> imagePaths.forEach(array::put) })
    put("createdAtEpochMs", createdAtEpochMs.toString())
    put("updatedAtEpochMs", updatedAtEpochMs.toString())
}

private fun JSONObject.toCapsule(): CityCapsule? {
    val schemaVersion = optInt("schemaVersion", -1)
    if (schemaVersion !in setOf(CapsuleContract.LEGACY_SCHEMA_VERSION, CapsuleContract.SCHEMA_VERSION)) return null
    val tagsJson = optJSONArray("tags") ?: return null
    val tags = buildList {
        for (index in 0 until tagsJson.length()) add(tagsJson.optString(index) ?: return null)
    }
    val moodWire = optString("mood")
    val mood = if (moodWire.isBlank()) null else CapsuleMood.fromWireValue(moodWire) ?: return null
    val imagesJson = optJSONArray("imagePaths") ?: JSONArray()
    val images = buildList {
        for (index in 0 until imagesJson.length()) add(imagesJson.optString(index) ?: return null)
    }
    return CapsuleValidator.normalize(
        CityCapsule(
            id = optString("id"),
            content = optString("content"),
            mood = mood,
            tags = tags,
            placeId = optString("placeId"),
            roamingSessionId = optString("roamingSessionId").takeIf(String::isNotBlank),
            imagePaths = images,
            createdAtEpochMs = optString("createdAtEpochMs").toLongOrNull() ?: return null,
            updatedAtEpochMs = optString("updatedAtEpochMs").toLongOrNull() ?: return null
        )
    )
}
