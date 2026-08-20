package com.y.citycapsule.core.capsule

object CapsuleContract {
    const val SCHEMA_VERSION = 2
    const val LEGACY_SCHEMA_VERSION = 1
    const val MAX_CATALOG_SIZE = 500
    const val CONTENT_MAX_LENGTH = 2000
    const val TAG_MAX_COUNT = 8
    const val TAG_MAX_LENGTH = 20
    const val IMAGE_MAX_COUNT = 9
    const val IMAGE_PATH_MAX_LENGTH = 2048
}

enum class CapsuleMood(val wireValue: String) {
    HAPPY("happy"),
    CALM("calm"),
    SURPRISED("surprised"),
    MELANCHOLY("melancholy"),
    ENERGETIC("energetic");

    companion object {
        fun fromWireValue(value: String): CapsuleMood? = entries.firstOrNull {
            it.wireValue == value
        }
    }
}

data class CityCapsule(
    val schemaVersion: Int = CapsuleContract.SCHEMA_VERSION,
    val id: String,
    val content: String,
    val mood: CapsuleMood? = null,
    val tags: List<String> = emptyList(),
    val placeId: String,
    val roamingSessionId: String? = null,
    val imagePaths: List<String> = emptyList(),
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
)

data class CapsuleDraft(
    val schemaVersion: Int = CapsuleContract.SCHEMA_VERSION,
    val capsuleId: String? = null,
    val content: String = "",
    val mood: CapsuleMood? = null,
    val tags: List<String> = emptyList(),
    val placeId: String? = null,
    val roamingSessionId: String? = null,
    val imagePaths: List<String> = emptyList(),
    val updatedAtEpochMs: Long = 0L
) {
    companion object {
        val EMPTY = CapsuleDraft()
    }
}

data class CapsuleCatalog(
    val schemaVersion: Int = CapsuleContract.SCHEMA_VERSION,
    val capsules: List<CityCapsule> = emptyList()
) {
    companion object {
        val EMPTY = CapsuleCatalog()
    }
}
