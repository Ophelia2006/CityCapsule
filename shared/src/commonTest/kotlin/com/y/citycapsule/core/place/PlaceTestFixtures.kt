package com.y.citycapsule.core.place

internal fun placeFixture(
    id: String = "place_1",
    name: String = "上海博物馆",
    city: String = "上海",
    district: String? = "黄浦区",
    category: PlaceCategory = PlaceCategory.CULTURE,
    address: String? = "人民大道 201 号",
    tags: List<String> = listOf("博物馆", "历史"),
    note: String? = "适合雨天参观",
    createdAtEpochMs: Long = 100L,
    updatedAtEpochMs: Long = 200L
): Place = Place(
    id = id,
    name = name,
    city = city,
    district = district,
    category = category,
    address = address,
    tags = tags,
    note = note,
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs
)
