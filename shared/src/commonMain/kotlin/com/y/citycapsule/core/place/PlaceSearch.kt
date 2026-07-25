package com.y.citycapsule.core.place

data class PlaceFilter(
    val categories: Set<PlaceCategory> = emptySet(),
    val city: String? = null,
    val district: String? = null,
    val favoritesOnly: Boolean = false
) {
    fun normalized(): PlaceFilter = copy(
        city = city.normalizedPlaceOptionalText(),
        district = district.normalizedPlaceOptionalText()
    )
}

data class PlaceSearchResult(
    val places: List<Place>,
    val normalizedQuery: String,
    val appliedFilter: PlaceFilter
)

object PlaceSearchEngine {
    fun search(
        places: List<Place>,
        favoriteIds: Set<String> = emptySet(),
        query: String = "",
        filter: PlaceFilter = PlaceFilter()
    ): PlaceSearchResult {
        val normalizedQuery = normalizeSearchText(query)
        val terms = normalizedQuery.split(' ').filter(String::isNotEmpty)
        val appliedFilter = filter.normalized()
        val matches = places.mapNotNull { place ->
            if (!matchesFilter(place, favoriteIds, appliedFilter)) {
                return@mapNotNull null
            }
            match(place, normalizedQuery, terms)
        }
        return PlaceSearchResult(
            places = matches.sortedWith(
                compareBy<PlaceMatch> { it.rank }
                    .thenByDescending { it.place.updatedAtEpochMs }
                    .thenBy { normalizeSearchText(it.place.name) }
                    .thenBy { it.place.id }
            ).map(PlaceMatch::place),
            normalizedQuery = normalizedQuery,
            appliedFilter = appliedFilter
        )
    }

    private fun matchesFilter(
        place: Place,
        favoriteIds: Set<String>,
        filter: PlaceFilter
    ): Boolean {
        if (filter.categories.isNotEmpty() && place.category !in filter.categories) {
            return false
        }
        if (filter.city != null &&
            normalizeSearchText(place.city) != normalizeSearchText(filter.city)
        ) {
            return false
        }
        if (filter.district != null &&
            normalizeSearchText(place.district.orEmpty()) != normalizeSearchText(filter.district)
        ) {
            return false
        }
        return !filter.favoritesOnly || place.id in favoriteIds
    }

    private fun match(
        place: Place,
        normalizedQuery: String,
        terms: List<String>
    ): PlaceMatch? {
        if (normalizedQuery.isEmpty()) {
            return PlaceMatch(place, RANK_EMPTY_QUERY)
        }
        val name = normalizeSearchText(place.name)
        val tags = place.tags.map(::normalizeSearchText)
        val locationFields = listOf(
            place.city,
            place.district.orEmpty(),
            place.address.orEmpty(),
            place.note.orEmpty()
        ).map(::normalizeSearchText)
        val allFields = listOf(name) + tags + locationFields
        if (!terms.all { term -> allFields.any { field -> term in field } }) {
            return null
        }
        val rank = when {
            name == normalizedQuery -> RANK_EXACT_NAME
            name.startsWith(normalizedQuery) -> RANK_NAME_PREFIX
            normalizedQuery in name -> RANK_NAME_CONTAINS
            tags.any { normalizedQuery in it } -> RANK_TAG
            locationFields.any { normalizedQuery in it } -> RANK_LOCATION
            else -> RANK_MULTI_TERM
        }
        return PlaceMatch(place, rank)
    }

    private fun normalizeSearchText(value: String): String =
        value.trim().lowercase().replace(WHITESPACE, " ")

    private data class PlaceMatch(
        val place: Place,
        val rank: Int
    )

    private val WHITESPACE = Regex("\\s+")
    private const val RANK_EXACT_NAME = 0
    private const val RANK_NAME_PREFIX = 1
    private const val RANK_NAME_CONTAINS = 2
    private const val RANK_TAG = 3
    private const val RANK_LOCATION = 4
    private const val RANK_MULTI_TERM = 5
    private const val RANK_EMPTY_QUERY = 6
}
