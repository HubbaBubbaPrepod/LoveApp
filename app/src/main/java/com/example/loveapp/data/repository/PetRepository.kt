package com.example.loveapp.data.repository

import com.example.loveapp.data.api.LoveAppApiService
import com.example.loveapp.data.api.models.*
import javax.inject.Inject

class PetRepository @Inject constructor(
    private val apiService: LoveAppApiService,
    private val authRepository: AuthRepository
) {
    private suspend fun token() = authRepository.getToken() ?: throw Exception("No token")

    // Core pet
    suspend fun getPet(): Result<PetResponse> = try {
        val resp = apiService.getPet("Bearer ${token()}")
        if (resp.success && resp.data != null) Result.success(resp.data) else Result.failure(Exception(resp.message ?: "Failed"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun updatePet(name: String? = null, petType: String? = null): Result<PetResponse> = try {
        val resp = apiService.updatePet("Bearer ${token()}", PetUpdateRequest(name, petType))
        if (resp.success && resp.data != null) Result.success(resp.data) else Result.failure(Exception(resp.message ?: "Failed"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun feed(): Result<CareActionResult> = try {
        val resp = apiService.feedPet("Bearer ${token()}")
        if (resp.success && resp.data != null) Result.success(resp.data) else Result.failure(Exception(resp.message ?: "Failed"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun play(): Result<CareActionResult> = try {
        val resp = apiService.playWithPet("Bearer ${token()}")
        if (resp.success && resp.data != null) Result.success(resp.data) else Result.failure(Exception(resp.message ?: "Failed"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun clean(): Result<CareActionResult> = try {
        val resp = apiService.cleanPet("Bearer ${token()}")
        if (resp.success && resp.data != null) Result.success(resp.data) else Result.failure(Exception(resp.message ?: "Failed"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun getHistory(limit: Int = 20): Result<List<PetActionResponse>> = try {
        val resp = apiService.getPetHistory("Bearer ${token()}", limit)
        if (resp.success && resp.data != null) Result.success(resp.data.items) else Result.failure(Exception(resp.message ?: "Failed"))
    } catch (e: Exception) { Result.failure(e) }

    // Pet types
    suspend fun getPetTypes(): Result<List<PetTypeInfo>> = try {
        val resp = apiService.getPetTypes("Bearer ${token()}")
        if (resp.success && resp.data != null) Result.success(resp.data.items) else Result.failure(Exception(resp.message ?: "Failed"))
    } catch (e: Exception) { Result.failure(e) }

    // Eggs
    suspend fun getEggs(): Result<List<PetEgg>> = try {
        val resp = apiService.getPetEggs("Bearer ${token()}")
        if (resp.success && resp.data != null) Result.success(resp.data.items) else Result.failure(Exception(resp.message ?: "Failed"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun hatchEgg(eggId: Long): Result<EggHatchResult> = try {
        val resp = apiService.hatchEgg("Bearer ${token()}", eggId)
        if (resp.success && resp.data != null) Result.success(resp.data) else Result.failure(Exception(resp.message ?: "Failed"))
    } catch (e: Exception) { Result.failure(e) }

    // Furniture
    suspend fun getFurnitureShop(): Result<List<FurnitureItem>> = try {
        val resp = apiService.getFurnitureShop("Bearer ${token()}")
        if (resp.success && resp.data != null) Result.success(resp.data.items) else Result.failure(Exception(resp.message ?: "Failed"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun getOwnedFurniture(): Result<List<OwnedFurniture>> = try {
        val resp = apiService.getOwnedFurniture("Bearer ${token()}")
        if (resp.success && resp.data != null) Result.success(resp.data.items) else Result.failure(Exception(resp.message ?: "Failed"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun buyFurniture(furnitureId: Int): Result<FurnitureItem> = try {
        val resp = apiService.buyFurniture("Bearer ${token()}", furnitureId)
        if (resp.success && resp.data != null) Result.success(resp.data) else Result.failure(Exception(resp.message ?: "Failed"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun placeFurniture(ownedId: Long, isPlaced: Boolean, x: Int = 0, y: Int = 0): Result<OwnedFurniture> = try {
        val resp = apiService.placeFurniture("Bearer ${token()}", ownedId, FurniturePlaceRequest(isPlaced, x, y))
        if (resp.success && resp.data != null) Result.success(resp.data) else Result.failure(Exception(resp.message ?: "Failed"))
    } catch (e: Exception) { Result.failure(e) }

    // Adventures
    suspend fun getAdventures(): Result<AdventuresData> = try {
        val resp = apiService.getAdventures("Bearer ${token()}")
        if (resp.success && resp.data != null) Result.success(resp.data) else Result.failure(Exception(resp.message ?: "Failed"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun startAdventure(adventureId: Int): Result<AdventureStartResult> = try {
        val resp = apiService.startAdventure("Bearer ${token()}", adventureId)
        if (resp.success && resp.data != null) Result.success(resp.data) else Result.failure(Exception(resp.message ?: "Failed"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun claimAdventure(activeId: Long): Result<AdventureClaimResult> = try {
        val resp = apiService.claimAdventure("Bearer ${token()}", activeId)
        if (resp.success && resp.data != null) Result.success(resp.data) else Result.failure(Exception(resp.message ?: "Failed"))
    } catch (e: Exception) { Result.failure(e) }

    // Wishes
    suspend fun getWishes(): Result<List<PetWish>> = try {
        val resp = apiService.getPetWishes("Bearer ${token()}")
        if (resp.success && resp.data != null) Result.success(resp.data.items) else Result.failure(Exception(resp.message ?: "Failed"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun createWish(text: String, emoji: String = "⭐"): Result<PetWish> = try {
        val resp = apiService.createPetWish("Bearer ${token()}", PetWishRequest(text, emoji))
        if (resp.success && resp.data != null) Result.success(resp.data) else Result.failure(Exception(resp.message ?: "Failed"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun fulfillWish(wishId: Long): Result<WishFulfillResult> = try {
        val resp = apiService.fulfillPetWish("Bearer ${token()}", wishId)
        if (resp.success && resp.data != null) Result.success(resp.data) else Result.failure(Exception(resp.message ?: "Failed"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun deleteWish(wishId: Long): Result<Unit> = try {
        val resp = apiService.deletePetWish("Bearer ${token()}", wishId)
        if (resp.success) Result.success(Unit) else Result.failure(Exception(resp.message ?: "Failed"))
    } catch (e: Exception) { Result.failure(e) }

    // Check-in
    suspend fun getCheckinStatus(): Result<CheckinStatus> = try {
        val resp = apiService.getCheckinStatus("Bearer ${token()}")
        if (resp.success && resp.data != null) Result.success(resp.data) else Result.failure(Exception(resp.message ?: "Failed"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun doCheckin(): Result<CheckinResult> = try {
        val resp = apiService.doCheckin("Bearer ${token()}")
        if (resp.success && resp.data != null) Result.success(resp.data) else Result.failure(Exception(resp.message ?: "Failed"))
    } catch (e: Exception) { Result.failure(e) }

    // Spin
    suspend fun doSpin(): Result<SpinResult> = try {
        val resp = apiService.doSpin("Bearer ${token()}")
        if (resp.success && resp.data != null) Result.success(resp.data) else Result.failure(Exception(resp.message ?: "Failed"))
    } catch (e: Exception) { Result.failure(e) }

    // Collections & Passport
    suspend fun getCollections(): Result<CollectionsData> = try {
        val resp = apiService.getPetCollections("Bearer ${token()}")
        if (resp.success && resp.data != null) Result.success(resp.data) else Result.failure(Exception(resp.message ?: "Failed"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun getPassport(): Result<PetPassport> = try {
        val resp = apiService.getPetPassport("Bearer ${token()}")
        if (resp.success && resp.data != null) Result.success(resp.data) else Result.failure(Exception(resp.message ?: "Failed"))
    } catch (e: Exception) { Result.failure(e) }
}
