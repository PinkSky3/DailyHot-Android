package com.example.data.model

data class OilPriceRootResponse(
    val code: Int? = null,
    val msg: String? = null,
    val data: OilPriceDataResponse? = null
)

data class OilPriceDataResponse(
    val time: String? = null,
    val next_update_time: String? = null,
    val province: OilPriceProvince? = null
)

data class OilPriceProvince(
    val pri_name: String? = null,
    val gasoline_92: String? = null,
    val gasoline_95: String? = null,
    val gasoline_98: String? = null,
    val diesel_0: String? = null
)

data class OilPriceEntry(
    val label: String,
    val price: String
)

val PROVINCES = listOf(
    "北京", "上海", "天津", "重庆",
    "河北", "山西", "辽宁", "吉林", "黑龙江",
    "江苏", "浙江", "安徽", "福建", "江西", "山东",
    "河南", "湖北", "湖南",
    "广东", "广西", "海南",
    "四川", "贵州", "云南", "西藏",
    "陕西", "甘肃", "青海", "宁夏", "新疆",
    "内蒙古"
)
