package com.y.citycapsule.core.profile

enum class LocalProfileValidationError {
    UNSUPPORTED_SCHEMA,
    DISPLAY_NAME_REQUIRED,
    DISPLAY_NAME_TOO_LONG,
    HOME_CITY_TOO_LONG,
    BIO_TOO_LONG
}

data class LocalProfileValidationResult(
    val profile: LocalProfile?,
    val errors: List<LocalProfileValidationError>
) {
    val isValid: Boolean
        get() = profile != null && errors.isEmpty()
}

object LocalProfileValidator {
    const val DISPLAY_NAME_MAX_LENGTH = 20
    const val HOME_CITY_MAX_LENGTH = 30
    const val BIO_MAX_LENGTH = 80

    fun validate(profile: LocalProfile): LocalProfileValidationResult {
        val normalized = profile.copy(
            displayName = profile.displayName.trim(),
            homeCity = profile.homeCity.normalizedOptionalText(),
            bio = profile.bio.normalizedOptionalText()
        )
        val errors = buildList {
            if (normalized.schemaVersion != LocalProfileContract.SCHEMA_VERSION) {
                add(LocalProfileValidationError.UNSUPPORTED_SCHEMA)
            }
            if (normalized.displayName.isEmpty()) {
                add(LocalProfileValidationError.DISPLAY_NAME_REQUIRED)
            } else if (normalized.displayName.length > DISPLAY_NAME_MAX_LENGTH) {
                add(LocalProfileValidationError.DISPLAY_NAME_TOO_LONG)
            }
            if ((normalized.homeCity?.length ?: 0) > HOME_CITY_MAX_LENGTH) {
                add(LocalProfileValidationError.HOME_CITY_TOO_LONG)
            }
            if ((normalized.bio?.length ?: 0) > BIO_MAX_LENGTH) {
                add(LocalProfileValidationError.BIO_TOO_LONG)
            }
        }
        return LocalProfileValidationResult(
            profile = normalized.takeIf { errors.isEmpty() },
            errors = errors
        )
    }

    fun normalizeOrNull(profile: LocalProfile): LocalProfile? = validate(profile).profile
}

internal fun String?.normalizedOptionalText(): String? =
    this?.trim()?.takeIf(String::isNotEmpty)
