package com.y.citycapsule.core.place

/** Stable, offline fixtures written only when the catalog key is missing. */
object PlaceSeedData {
    val CATALOG = PlaceCatalog(
        places = listOf(
            seed(
                id = "seed_shanghai_museum",
                name = "上海博物馆",
                city = "上海",
                district = "黄浦区",
                category = PlaceCategory.CULTURE,
                address = "人民大道 201 号",
                tags = listOf("博物馆", "历史"),
                note = "适合安静地了解城市历史。",
                order = 1
            ),
            seed(
                id = "seed_wukang_building",
                name = "武康大楼",
                city = "上海",
                district = "徐汇区",
                category = PlaceCategory.LANDMARK,
                address = "淮海中路 1850 号",
                tags = listOf("建筑", "街区"),
                note = "适合步行观察周边街道。",
                order = 2
            ),
            seed(
                id = "seed_west_bund_museum",
                name = "西岸美术馆",
                city = "上海",
                district = "徐汇区",
                category = PlaceCategory.CULTURE,
                address = "龙腾大道 2600 号",
                tags = listOf("美术馆", "滨江"),
                note = "可以和滨江散步安排在同一天。",
                order = 3
            ),
            seed(
                id = "seed_gongqing_forest",
                name = "共青森林公园",
                city = "上海",
                district = "杨浦区",
                category = PlaceCategory.NATURE,
                address = "军工路 2000 号",
                tags = listOf("公园", "散步"),
                note = "适合留出半天慢慢游览。",
                order = 4
            ),
            seed(
                id = "seed_west_lake",
                name = "西湖",
                city = "杭州",
                district = "西湖区",
                category = PlaceCategory.NATURE,
                address = "湖滨路",
                tags = listOf("湖泊", "散步"),
                note = "清晨和傍晚更适合步行。",
                order = 5
            ),
            seed(
                id = "seed_china_tea_museum",
                name = "中国茶叶博物馆",
                city = "杭州",
                district = "西湖区",
                category = PlaceCategory.CULTURE,
                address = "龙井路 88 号",
                tags = listOf("博物馆", "茶文化"),
                note = "室内外空间都值得预留时间。",
                order = 6
            ),
            seed(
                id = "seed_hefang_street",
                name = "河坊街",
                city = "杭州",
                district = "上城区",
                category = PlaceCategory.SHOPPING,
                address = "河坊街",
                tags = listOf("街区", "小店"),
                note = "适合顺路体验本地小吃和店铺。",
                order = 7
            ),
            seed(
                id = "seed_hangzhou_food_market",
                name = "杭州城市厨房",
                city = "杭州",
                district = "拱墅区",
                category = PlaceCategory.FOOD,
                address = "霞湾巷",
                tags = listOf("美食", "市集"),
                note = "适合多人一起探索不同风味。",
                order = 8
            )
        ).sortedBy(Place::id)
    )

    val IDS: Set<String> = CATALOG.places.mapTo(mutableSetOf(), Place::id)

    private fun seed(
        id: String,
        name: String,
        city: String,
        district: String,
        category: PlaceCategory,
        address: String,
        tags: List<String>,
        note: String,
        order: Int
    ): Place {
        val timestamp = BASE_TIMESTAMP + order
        return Place(
            id = id,
            name = name,
            city = city,
            district = district,
            category = category,
            address = address,
            tags = tags,
            note = note,
            source = PlaceSource.SEED,
            createdAtEpochMs = timestamp,
            updatedAtEpochMs = timestamp
        )
    }

    private const val BASE_TIMESTAMP = 1_700_000_000_000L
}
