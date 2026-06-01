package com.example.data.model

data class GoldMarketEntry(
    val name: String,
    val sellPrice: String,
    val openPrice: String,
    val highPrice: String,
    val lowPrice: String,
    val unit: String,
    val changeRate: String
)

data class GoldBrandEntry(
    val brand: String,
    val goldPrice: String,
    val bullionPrice: String,
    val platinumPrice: String,
    val updated: String
)

data class GoldBankRecycleEntry(
    val name: String,
    val price: String,
    val type: String,
    val updated: String
)

data class GoldPriceSnapshot(
    val domesticMarkets: List<GoldMarketEntry>,
    val internationalMarkets: List<GoldMarketEntry>,
    val brands: List<GoldBrandEntry>,
    val bankRecycle: List<GoldBankRecycleEntry>,
    val source: String,
    val updateTime: String
)
