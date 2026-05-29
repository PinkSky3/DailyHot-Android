package com.example.data.model

data class OilPriceResponse(
    val code: Int? = null,
    val data: OilPriceData? = null,
    val msg: String? = null
)

data class OilPriceData(
    val province: String? = null,
    val name: String? = null,
    val `92`: String? = null,
    val `92h`: String? = null,
    val `95`: String? = null,
    val `95h`: String? = null,
    val `98`: String? = null,
    val `98h`: String? = null,
    val `0`: String? = null,
    val `0h`: String? = null,
    val `-10`: String? = null,
    val `-10h`: String? = null,
    val `-20`: String? = null,
    val `-20h`: String? = null
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

data class OilPriceEntry(
    val label: String,
    val price: String
)
