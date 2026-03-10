package com.example.loveapp.data.repository

import com.example.loveapp.data.api.LoveAppApiService
import com.example.loveapp.data.api.models.*
import com.tencent.mmkv.MMKV
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShopRepository @Inject constructor(
    private val api: LoveAppApiService
) {
    private fun token() = "Bearer ${MMKV.defaultMMKV().decodeString("auth_token", "")}"

    suspend fun getBalance(): Result<CoinBalanceResponse> = runCatching {
        api.getCoinBalance(token())
    }

    suspend fun getShopItems(category: String? = null): Result<List<ShopItem>> = runCatching {
        api.getShopItems(token(), category)
    }

    suspend fun buyItem(id: Int): Result<ShopPurchaseResult> = runCatching {
        api.buyShopItem(token(), id)
    }

    suspend fun getDailyDeals(): Result<List<DailyDeal>> = runCatching {
        api.getDailyDeals(token())
    }

    suspend fun buyDeal(id: Int): Result<DealPurchaseResult> = runCatching {
        api.buyDailyDeal(token(), id)
    }

    suspend fun getMissions(): Result<List<DailyMission>> = runCatching {
        api.getDailyMissions(token())
    }

    suspend fun progressMission(missionType: String): Result<MissionProgressResult> = runCatching {
        api.progressMission(token(), MissionProgressRequest(missionType))
    }

    suspend fun claimMission(id: Int): Result<MissionClaimResult> = runCatching {
        api.claimMission(token(), id)
    }

    suspend fun getTransactions(limit: Int? = null, offset: Int? = null): Result<TransactionsResponse> = runCatching {
        api.getCoinTransactions(token(), limit, offset)
    }

    suspend fun getSummary(): Result<EconomySummary> = runCatching {
        api.getEconomySummary(token())
    }
}
