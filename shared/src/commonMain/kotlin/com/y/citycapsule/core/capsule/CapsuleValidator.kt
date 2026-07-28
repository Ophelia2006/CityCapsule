package com.y.citycapsule.core.capsule

object CapsuleValidator {
    fun normalizeDraft(value: CapsuleDraft): CapsuleDraft? {
        if (value.schemaVersion != CapsuleContract.SCHEMA_VERSION) return null
        val content = value.content.trim()
        if (content.isEmpty() || content.length > CapsuleContract.CONTENT_MAX_LENGTH) return null
        val placeId = value.placeId?.trim()?.takeIf(String::isNotEmpty) ?: return null
        val capsuleId = value.capsuleId?.trim()?.takeIf(String::isNotEmpty)
        val tags = value.tags.map(String::trim).filter(String::isNotEmpty).distinct()
        if (tags.size > CapsuleContract.TAG_MAX_COUNT ||
            tags.any { it.length > CapsuleContract.TAG_MAX_LENGTH }
        ) return null
        val imagePaths = value.imagePaths
            .map(String::trim)
            .filter(String::isNotEmpty)
            .distinct()
        if (imagePaths.size > CapsuleContract.IMAGE_MAX_COUNT ||
            imagePaths.any { it.length > CapsuleContract.IMAGE_PATH_MAX_LENGTH }
        ) return null
        return value.copy(
            capsuleId = capsuleId,
            content = content,
            tags = tags,
            placeId = placeId,
            imagePaths = imagePaths
        )
    }

    fun normalize(value: CityCapsule): CityCapsule? {
        val draft = normalizeDraft(
            CapsuleDraft(
                capsuleId = value.id,
                content = value.content,
                mood = value.mood,
                tags = value.tags,
                placeId = value.placeId,
                imagePaths = value.imagePaths,
                updatedAtEpochMs = value.updatedAtEpochMs
            )
        ) ?: return null
        if (value.id.isBlank() || value.createdAtEpochMs < 0L ||
            value.updatedAtEpochMs < value.createdAtEpochMs
        ) return null
        return value.copy(
            id = value.id.trim(),
            content = draft.content,
            tags = draft.tags,
            placeId = requireNotNull(draft.placeId),
            imagePaths = draft.imagePaths
        )
    }
}
