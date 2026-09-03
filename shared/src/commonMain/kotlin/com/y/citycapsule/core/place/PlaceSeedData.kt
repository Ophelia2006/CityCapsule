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
                category = PlaceCategory.MUSEUM,
                address = "人民大道 201 号",
                tags = listOf("博物馆", "历史"),
                description = "适合安静地了解城市历史。",
                geoPoint = GeoPoint(31.2303, 121.4703),
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
                description = "适合步行观察周边街道。",
                geoPoint = GeoPoint(31.1990, 121.4374),
                order = 2
            ),
            seed(
                id = "seed_west_bund_museum",
                name = "西岸美术馆",
                city = "上海",
                district = "徐汇区",
                category = PlaceCategory.ART_SPACE,
                address = "龙腾大道 2600 号",
                tags = listOf("美术馆", "滨江"),
                description = "可以和滨江散步安排在同一天。",
                geoPoint = GeoPoint(31.1773, 121.4630),
                order = 3
            ),
            seed(
                id = "seed_gongqing_forest",
                name = "共青森林公园",
                city = "上海",
                district = "杨浦区",
                category = PlaceCategory.PARK,
                address = "军工路 2000 号",
                tags = listOf("公园", "散步"),
                description = "适合留出半天慢慢游览。",
                geoPoint = GeoPoint(31.3228, 121.5481),
                order = 4
            ),
            seed(
                id = "seed_the_bund",
                name = "外滩",
                city = "上海",
                district = "黄浦区",
                category = PlaceCategory.LANDMARK,
                address = "中山东一路",
                tags = listOf("建筑", "滨江", "夜景"),
                description = "沿黄浦江观察城市天际线与近代建筑群，傍晚到入夜最能感受两岸变化。",
                geoPoint = GeoPoint(31.2400, 121.4900),
                order = 5
            ),
            seed(
                id = "seed_yu_garden",
                name = "豫园",
                city = "上海",
                district = "黄浦区",
                category = PlaceCategory.HISTORIC_SITE,
                address = "福佑路 168 号",
                tags = listOf("园林", "古建", "街区"),
                description = "从古典园林进入老城厢的空间层次，适合与周边街巷安排为一次步行探索。",
                geoPoint = GeoPoint(31.2272, 121.4921),
                order = 6
            ),
            seed(
                id = "seed_shanghai_tower",
                name = "上海中心大厦",
                city = "上海",
                district = "浦东新区",
                category = PlaceCategory.LANDMARK,
                address = "银城中路 501 号",
                tags = listOf("天际线", "建筑", "城市景观"),
                description = "从陆家嘴高层建筑群理解上海的现代城市尺度，周边步行可观察多种建筑视角。",
                geoPoint = GeoPoint(31.2335, 121.5055),
                order = 7
            ),
            seed(
                id = "seed_natural_history_museum",
                name = "上海自然博物馆",
                city = "上海",
                district = "静安区",
                category = PlaceCategory.MUSEUM,
                address = "北京西路 510 号",
                tags = listOf("博物馆", "自然", "亲子"),
                description = "展览从生命演化延伸到人与自然，建筑和静安雕塑公园也构成连续的公共空间。",
                geoPoint = GeoPoint(31.2367, 121.4623),
                order = 8
            ),
            seed(
                id = "seed_power_station_of_art",
                name = "上海当代艺术博物馆",
                city = "上海",
                district = "黄浦区",
                category = PlaceCategory.ART_SPACE,
                address = "苗江路 678 号",
                tags = listOf("当代艺术", "工业建筑", "滨江"),
                description = "由工业建筑转化而来的当代艺术空间，适合连同南外滩滨水区域一起探索。",
                geoPoint = GeoPoint(31.2056, 121.4973),
                order = 9
            ),
            seed(
                id = "seed_shanghai_astronomy_museum",
                name = "上海天文馆",
                city = "上海",
                district = "浦东新区",
                category = PlaceCategory.MUSEUM,
                address = "临港大道 380 号",
                tags = listOf("天文", "博物馆", "建筑"),
                description = "以天体运行和宇宙尺度为线索的专题场馆，建筑本身也提供鲜明的空间体验。",
                geoPoint = GeoPoint(30.9148, 121.9250),
                order = 10
            ),
            seed(
                id = "seed_m50",
                name = "M50 创意园",
                city = "上海",
                district = "普陀区",
                category = PlaceCategory.ART_SPACE,
                address = "莫干山路 50 号",
                tags = listOf("艺术", "工业遗存", "小店"),
                description = "由旧厂房形成的艺术街区，画廊、工作室与沿河城市空间适合慢慢步行。",
                geoPoint = GeoPoint(31.2471, 121.4491),
                order = 11
            ),
            seed(
                id = "seed_1933_old_millfun",
                name = "1933 老场坊",
                city = "上海",
                district = "虹口区",
                category = PlaceCategory.HISTORIC_SITE,
                address = "溧阳路 611 号",
                tags = listOf("工业建筑", "街区", "摄影"),
                description = "独特的混凝土结构、坡道和廊桥形成复杂空间，适合观察建筑光影。",
                geoPoint = GeoPoint(31.2596, 121.4910),
                order = 12
            ),
            seed(
                id = "seed_fuxing_park",
                name = "复兴公园",
                city = "上海",
                district = "黄浦区",
                category = PlaceCategory.PARK,
                address = "雁荡路 105 号",
                tags = listOf("公园", "散步", "梧桐"),
                description = "城市中心适合停留和观察日常生活的公园，也可以串联周边历史街区散步。",
                geoPoint = GeoPoint(31.2183, 121.4690),
                order = 13
            ),
            seed(
                id = "seed_xuhui_riverside",
                name = "徐汇滨江公共开放空间",
                city = "上海",
                district = "徐汇区",
                category = PlaceCategory.WATERFRONT,
                address = "龙腾大道沿线",
                tags = listOf("滨江", "散步", "夕阳"),
                description = "沿江步道串联工业遗存、艺术场馆与开放绿地，适合傍晚进行较长距离漫步。",
                geoPoint = GeoPoint(31.1848, 121.4662),
                order = 14
            ),
            seed(
                id = "seed_tianzifang",
                name = "田子坊",
                city = "上海",
                district = "黄浦区",
                category = PlaceCategory.NEIGHBORHOOD,
                address = "泰康路 210 弄",
                tags = listOf("里弄", "小店", "街区"),
                description = "在里弄尺度中穿行，观察居住空间、工作室与小店混合形成的街区肌理。",
                geoPoint = GeoPoint(31.2101, 121.4685),
                order = 15
            ),
            seed(
                id = "seed_west_lake",
                name = "西湖",
                city = "杭州",
                district = "西湖区",
                category = PlaceCategory.WATERFRONT,
                address = "湖滨路",
                tags = listOf("湖泊", "散步"),
                description = "清晨和傍晚更适合步行。",
                geoPoint = GeoPoint(30.2507, 120.1536),
                order = 16
            ),
            seed(
                id = "seed_china_tea_museum",
                name = "中国茶叶博物馆",
                city = "杭州",
                district = "西湖区",
                category = PlaceCategory.MUSEUM,
                address = "龙井路 88 号",
                tags = listOf("博物馆", "茶文化"),
                description = "室内外空间都值得预留时间。",
                geoPoint = GeoPoint(30.2294, 120.1188),
                order = 17
            ),
            seed(
                id = "seed_hefang_street",
                name = "河坊街",
                city = "杭州",
                district = "上城区",
                category = PlaceCategory.MARKET,
                address = "河坊街",
                tags = listOf("街区", "小店"),
                description = "适合顺路体验本地小吃和店铺。",
                geoPoint = GeoPoint(30.2372, 120.1704),
                order = 18
            ),
            seed(
                id = "seed_hangzhou_food_market",
                name = "杭州城市厨房",
                city = "杭州",
                district = "拱墅区",
                category = PlaceCategory.RESTAURANT,
                address = "霞湾巷",
                tags = listOf("美食", "市集"),
                description = "适合多人一起探索不同风味。",
                geoPoint = GeoPoint(30.3150, 120.1510),
                order = 19
            )
        ).sortedBy(Place::id)
    )

    val IDS: Set<String> = CATALOG.places.mapTo(mutableSetOf(), Place::id)
    val BY_ID: Map<String, Place> = CATALOG.places.associateBy(Place::id)

    private fun seed(
        id: String,
        name: String,
        city: String,
        district: String,
        category: PlaceCategory,
        address: String,
        tags: List<String>,
        description: String,
        geoPoint: GeoPoint,
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
            description = description,
            contentSource = "CityCapsule 内置城市内容包",
            source = PlaceSource.SEED,
            geoPoint = geoPoint,
            createdAtEpochMs = timestamp,
            updatedAtEpochMs = timestamp
        )
    }

    private const val BASE_TIMESTAMP = 1_700_000_000_000L
}
