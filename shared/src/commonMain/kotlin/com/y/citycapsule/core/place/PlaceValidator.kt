package com.y.citycapsule.core.place

enum class PlaceValidationError {
    UNSUPPORTED_SCHEMA,
    INVALID_ID,
    NAME_REQUIRED,
    NAME_TOO_LONG,
    CITY_REQUIRED,
    CITY_TOO_LONG,
    DISTRICT_TOO_LONG,
    ADDRESS_TOO_LONG,
    TOO_MANY_TAGS,
    TAG_TOO_LONG,
    NOTE_TOO_LONG,
    INVALID_GEO_POINT,
    INVALID_VISUAL_REF,
    INVALID_CREATED_AT,
    INVALID_UPDATED_AT
}

data class PlaceValidationResult(
    val place: Place?,
    val errors: List<PlaceValidationError>
) {
    val isValid: Boolean
        get() = place != null && errors.isEmpty()
}

data class PlaceDraftValidationResult(
    val draft: PlaceDraft?,
    val errors: List<PlaceValidationError>
) {
    val isValid: Boolean
        get() = draft != null && errors.isEmpty()
}

object PlaceValidator {
    const val ID_MAX_LENGTH = 64
    const val NAME_MAX_LENGTH = 60
    const val CITY_MAX_LENGTH = 30
    const val DISTRICT_MAX_LENGTH = 30
    const val ADDRESS_MAX_LENGTH = 120
    const val TAG_MAX_COUNT = 8
    const val TAG_MAX_LENGTH = 16
    const val NOTE_MAX_LENGTH = 300
    const val VISUAL_REF_MAX_LENGTH = 512

    private val idPattern = Regex("[A-Za-z0-9_-]{1,$ID_MAX_LENGTH}")

    fun validate(place: Place): PlaceValidationResult {
        val normalized = place.copy(
            id = place.id.trim(),
            name = place.name.trim(),
            city = place.city.trim(),
            district = place.district.normalizedPlaceOptionalText(),
            address = place.address.normalizedPlaceOptionalText(),
            tags = normalizeTags(place.tags),
            note = place.note.normalizedPlaceOptionalText(),
            visualRef = place.visualRef?.copy(value = place.visualRef.value.trim())
        )
        val errors = validateFields(
            schemaVersion = normalized.schemaVersion,
            id = normalized.id,
            name = normalized.name,
            city = normalized.city,
            district = normalized.district,
            address = normalized.address,
            tags = normalized.tags,
            note = normalized.note,
            geoPoint = normalized.geoPoint,
            visualRef = normalized.visualRef,
            createdAtEpochMs = normalized.createdAtEpochMs,
            updatedAtEpochMs = normalized.updatedAtEpochMs
        )
        return PlaceValidationResult(
            place = normalized.takeIf { errors.isEmpty() },
            errors = errors
        )
    }

    fun validateDraft(draft: PlaceDraft): PlaceDraftValidationResult {
        val normalized = draft.copy(
            name = draft.name.trim(),
            city = draft.city.trim(),
            district = draft.district.normalizedPlaceOptionalText(),
            address = draft.address.normalizedPlaceOptionalText(),
            tags = normalizeTags(draft.tags),
            note = draft.note.normalizedPlaceOptionalText()
        )
        val errors = validateFields(
            schemaVersion = PlaceContract.SCHEMA_VERSION,
            id = VALID_DRAFT_ID,
            name = normalized.name,
            city = normalized.city,
            district = normalized.district,
            address = normalized.address,
            tags = normalized.tags,
            note = normalized.note,
            geoPoint = null,
            visualRef = null,
            createdAtEpochMs = 0L,
            updatedAtEpochMs = 0L
        )
        return PlaceDraftValidationResult(
            draft = normalized.takeIf { errors.isEmpty() },
            errors = errors
        )
    }

    fun normalizeOrNull(place: Place): Place? = validate(place).place

    fun normalizeDraftOrNull(draft: PlaceDraft): PlaceDraft? = validateDraft(draft).draft

    fun isValidId(id: String): Boolean = idPattern.matches(id)

    private fun validateFields(
        schemaVersion: Int,
        id: String,
        name: String,
        city: String,
        district: String?,
        address: String?,
        tags: List<String>,
        note: String?,
        geoPoint: GeoPoint?,
        visualRef: PlaceVisualRef?,
        createdAtEpochMs: Long,
        updatedAtEpochMs: Long
    ): List<PlaceValidationError> = buildList {
        if (schemaVersion != PlaceContract.SCHEMA_VERSION) {
            add(PlaceValidationError.UNSUPPORTED_SCHEMA)
        }
        if (!isValidId(id)) {
            add(PlaceValidationError.INVALID_ID)
        }
        if (name.isEmpty()) {
            add(PlaceValidationError.NAME_REQUIRED)
        } else if (name.length > NAME_MAX_LENGTH) {
            add(PlaceValidationError.NAME_TOO_LONG)
        }
        if (city.isEmpty()) {
            add(PlaceValidationError.CITY_REQUIRED)
        } else if (city.length > CITY_MAX_LENGTH) {
            add(PlaceValidationError.CITY_TOO_LONG)
        }
        if ((district?.length ?: 0) > DISTRICT_MAX_LENGTH) {
            add(PlaceValidationError.DISTRICT_TOO_LONG)
        }
        if ((address?.length ?: 0) > ADDRESS_MAX_LENGTH) {
            add(PlaceValidationError.ADDRESS_TOO_LONG)
        }
        if (tags.size > TAG_MAX_COUNT) {
            add(PlaceValidationError.TOO_MANY_TAGS)
        }
        if (tags.any { it.length > TAG_MAX_LENGTH }) {
            add(PlaceValidationError.TAG_TOO_LONG)
        }
        if ((note?.length ?: 0) > NOTE_MAX_LENGTH) {
            add(PlaceValidationError.NOTE_TOO_LONG)
        }
        if (geoPoint != null && (
                !geoPoint.latitude.isFinite() ||
                    !geoPoint.longitude.isFinite() ||
                    geoPoint.latitude !in -90.0..90.0 ||
                    geoPoint.longitude !in -180.0..180.0
                )
        ) {
            add(PlaceValidationError.INVALID_GEO_POINT)
        }
        if (visualRef != null &&
            (visualRef.value.isEmpty() || visualRef.value.length > VISUAL_REF_MAX_LENGTH)
        ) {
            add(PlaceValidationError.INVALID_VISUAL_REF)
        }
        if (createdAtEpochMs < 0L) {
            add(PlaceValidationError.INVALID_CREATED_AT)
        }
        if (updatedAtEpochMs < createdAtEpochMs) {
            add(PlaceValidationError.INVALID_UPDATED_AT)
        }
    }

    private fun normalizeTags(tags: List<String>): List<String> {
        val seen = mutableSetOf<String>()
        return buildList {
            tags.forEach { source ->
                val tag = source.trim()
                val identity = tag.lowercase()
                if (tag.isNotEmpty() && seen.add(identity)) {
                    add(tag)
                }
            }
        }
    }

    private const val VALID_DRAFT_ID = "draft"
}

internal fun String?.normalizedPlaceOptionalText(): String? =
    this?.trim()?.takeIf(String::isNotEmpty)

enum class PlaceCatalogValidationError {
    UNSUPPORTED_SCHEMA,
    INVALID_SEED_VERSION,
    TOO_MANY_PLACES,
    INVALID_PLACE,
    DUPLICATE_PLACE_ID
}

data class PlaceCatalogValidationResult(
    val catalog: PlaceCatalog?,
    val errors: List<PlaceCatalogValidationError>
) {
    val isValid: Boolean
        get() = catalog != null && errors.isEmpty()
}

object PlaceCatalogValidator {
    fun validate(catalog: PlaceCatalog): PlaceCatalogValidationResult {
        val normalizedPlaces = mutableListOf<Place>()
        var hasInvalidPlace = false
        catalog.places.forEach { place ->
            val normalized = PlaceValidator.normalizeOrNull(place)
            if (normalized == null) {
                hasInvalidPlace = true
            } else {
                normalizedPlaces += normalized
            }
        }
        val duplicateIds = normalizedPlaces
            .groupingBy(Place::id)
            .eachCount()
            .any { it.value > 1 }
        val errors = buildList {
            if (catalog.schemaVersion != PlaceContract.SCHEMA_VERSION) {
                add(PlaceCatalogValidationError.UNSUPPORTED_SCHEMA)
            }
            if (catalog.seedVersion < 0) {
                add(PlaceCatalogValidationError.INVALID_SEED_VERSION)
            }
            if (catalog.places.size > PlaceContract.MAX_CATALOG_SIZE) {
                add(PlaceCatalogValidationError.TOO_MANY_PLACES)
            }
            if (hasInvalidPlace) {
                add(PlaceCatalogValidationError.INVALID_PLACE)
            }
            if (duplicateIds) {
                add(PlaceCatalogValidationError.DUPLICATE_PLACE_ID)
            }
        }
        return PlaceCatalogValidationResult(
            catalog = catalog.copy(places = normalizedPlaces).takeIf { errors.isEmpty() },
            errors = errors
        )
    }

    fun normalizeOrNull(catalog: PlaceCatalog): PlaceCatalog? = validate(catalog).catalog
}
