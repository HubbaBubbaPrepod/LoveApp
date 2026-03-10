package com.example.loveapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loveapp.data.api.models.*
import com.example.loveapp.data.repository.PetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PetViewModel @Inject constructor(
    private val petRepository: PetRepository
) : ViewModel() {

    private val _pet = MutableStateFlow<PetResponse?>(null)
    val pet: StateFlow<PetResponse?> = _pet.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isActing = MutableStateFlow(false)
    val isActing: StateFlow<Boolean> = _isActing.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _actionResult = MutableStateFlow<String?>(null)
    val actionResult: StateFlow<String?> = _actionResult.asStateFlow()

    // Pet types
    private val _petTypes = MutableStateFlow<List<PetTypeInfo>>(emptyList())
    val petTypes: StateFlow<List<PetTypeInfo>> = _petTypes.asStateFlow()

    // Eggs
    private val _eggs = MutableStateFlow<List<PetEgg>>(emptyList())
    val eggs: StateFlow<List<PetEgg>> = _eggs.asStateFlow()

    // Furniture
    private val _furnitureShop = MutableStateFlow<List<FurnitureItem>>(emptyList())
    val furnitureShop: StateFlow<List<FurnitureItem>> = _furnitureShop.asStateFlow()

    private val _ownedFurniture = MutableStateFlow<List<OwnedFurniture>>(emptyList())
    val ownedFurniture: StateFlow<List<OwnedFurniture>> = _ownedFurniture.asStateFlow()

    // Adventures
    private val _adventuresData = MutableStateFlow<AdventuresData?>(null)
    val adventuresData: StateFlow<AdventuresData?> = _adventuresData.asStateFlow()

    // Wishes
    private val _wishes = MutableStateFlow<List<PetWish>>(emptyList())
    val wishes: StateFlow<List<PetWish>> = _wishes.asStateFlow()

    // Check-in
    private val _checkinStatus = MutableStateFlow<CheckinStatus?>(null)
    val checkinStatus: StateFlow<CheckinStatus?> = _checkinStatus.asStateFlow()

    // Spin result
    private val _spinResult = MutableStateFlow<SpinResult?>(null)
    val spinResult: StateFlow<SpinResult?> = _spinResult.asStateFlow()

    // Collections
    private val _collections = MutableStateFlow<CollectionsData?>(null)
    val collections: StateFlow<CollectionsData?> = _collections.asStateFlow()

    // Passport
    private val _passport = MutableStateFlow<PetPassport?>(null)
    val passport: StateFlow<PetPassport?> = _passport.asStateFlow()

    // Level reward popup
    private val _levelReward = MutableStateFlow<LevelReward?>(null)
    val levelReward: StateFlow<LevelReward?> = _levelReward.asStateFlow()

    // Hatch result popup
    private val _hatchResult = MutableStateFlow<EggHatchResult?>(null)
    val hatchResult: StateFlow<EggHatchResult?> = _hatchResult.asStateFlow()

    init {
        loadPet()
    }

    fun loadPet() {
        viewModelScope.launch {
            _isLoading.value = true
            petRepository.getPet()
                .onSuccess { _pet.value = it }
                .onFailure { _errorMessage.value = it.message }
            _isLoading.value = false
        }
    }

    // === Care Actions ===
    fun feed() { doCareAction("Покормлен! 🍖") { petRepository.feed() } }
    fun play() { doCareAction("Поиграл! 🎾") { petRepository.play() } }
    fun clean() { doCareAction("Помыт! 🛁") { petRepository.clean() } }

    private fun doCareAction(label: String, action: suspend () -> Result<CareActionResult>) {
        viewModelScope.launch {
            _isActing.value = true
            action()
                .onSuccess {
                    _pet.value = it.pet
                    _actionResult.value = label
                    if (it.levelReward != null) _levelReward.value = it.levelReward
                }
                .onFailure { _errorMessage.value = it.message }
            _isActing.value = false
        }
    }

    fun renamePet(name: String) {
        if (_isActing.value) return
        viewModelScope.launch {
            _isActing.value = true
            petRepository.updatePet(name = name)
                .onSuccess { _pet.value = it }
                .onFailure { _errorMessage.value = it.message }
            _isActing.value = false
        }
    }

    // === Pet Types ===
    fun loadPetTypes() {
        viewModelScope.launch {
            petRepository.getPetTypes()
                .onSuccess { _petTypes.value = it }
        }
    }

    // === Eggs ===
    fun loadEggs() {
        viewModelScope.launch {
            petRepository.getEggs()
                .onSuccess { _eggs.value = it }
        }
    }

    fun hatchEgg(eggId: Long) {
        viewModelScope.launch {
            _isActing.value = true
            petRepository.hatchEgg(eggId)
                .onSuccess {
                    _hatchResult.value = it
                    loadEggs()
                    loadPet()
                }
                .onFailure { _errorMessage.value = it.message }
            _isActing.value = false
        }
    }

    // === Furniture ===
    fun loadFurnitureShop() {
        viewModelScope.launch {
            petRepository.getFurnitureShop()
                .onSuccess { _furnitureShop.value = it }
        }
    }

    fun loadOwnedFurniture() {
        viewModelScope.launch {
            petRepository.getOwnedFurniture()
                .onSuccess { _ownedFurniture.value = it }
        }
    }

    fun buyFurniture(furnitureId: Int) {
        viewModelScope.launch {
            _isActing.value = true
            petRepository.buyFurniture(furnitureId)
                .onSuccess {
                    _actionResult.value = "Куплено: ${it.name} ${it.emoji}"
                    loadOwnedFurniture()
                    loadPet()
                }
                .onFailure { _errorMessage.value = it.message }
            _isActing.value = false
        }
    }

    fun placeFurniture(ownedId: Long, isPlaced: Boolean) {
        viewModelScope.launch {
            petRepository.placeFurniture(ownedId, isPlaced)
                .onSuccess { loadOwnedFurniture() }
                .onFailure { _errorMessage.value = it.message }
        }
    }

    // === Adventures ===
    fun loadAdventures() {
        viewModelScope.launch {
            petRepository.getAdventures()
                .onSuccess { _adventuresData.value = it }
        }
    }

    fun startAdventure(adventureId: Int) {
        viewModelScope.launch {
            _isActing.value = true
            petRepository.startAdventure(adventureId)
                .onSuccess {
                    _actionResult.value = "Приключение началось! 🗺️"
                    loadAdventures()
                    loadPet()
                }
                .onFailure { _errorMessage.value = it.message }
            _isActing.value = false
        }
    }

    fun claimAdventure(activeId: Long) {
        viewModelScope.launch {
            _isActing.value = true
            petRepository.claimAdventure(activeId)
                .onSuccess {
                    _actionResult.value = "Награда: +${it.xpGained} XP, +${it.coinsGained} монет! 🎉"
                    loadAdventures()
                    loadPet()
                }
                .onFailure { _errorMessage.value = it.message }
            _isActing.value = false
        }
    }

    // === Wishes ===
    fun loadWishes() {
        viewModelScope.launch {
            petRepository.getWishes()
                .onSuccess { _wishes.value = it }
        }
    }

    fun createWish(text: String, emoji: String = "⭐") {
        viewModelScope.launch {
            _isActing.value = true
            petRepository.createWish(text, emoji)
                .onSuccess {
                    _actionResult.value = "Желание добавлено! ⭐"
                    loadWishes()
                }
                .onFailure { _errorMessage.value = it.message }
            _isActing.value = false
        }
    }

    fun fulfillWish(wishId: Long) {
        viewModelScope.launch {
            _isActing.value = true
            petRepository.fulfillWish(wishId)
                .onSuccess {
                    _actionResult.value = "Желание исполнено! +${it.xpGained} XP ✨"
                    loadWishes()
                    loadPet()
                }
                .onFailure { _errorMessage.value = it.message }
            _isActing.value = false
        }
    }

    fun deleteWish(wishId: Long) {
        viewModelScope.launch {
            petRepository.deleteWish(wishId)
                .onSuccess { loadWishes() }
                .onFailure { _errorMessage.value = it.message }
        }
    }

    // === Check-in ===
    fun loadCheckinStatus() {
        viewModelScope.launch {
            petRepository.getCheckinStatus()
                .onSuccess { _checkinStatus.value = it }
        }
    }

    fun doCheckin() {
        viewModelScope.launch {
            _isActing.value = true
            petRepository.doCheckin()
                .onSuccess {
                    _actionResult.value = "Отмечено! Серия: ${it.streak} 🔥 +${it.coinsEarned} монет, +${it.xpEarned} XP"
                    loadCheckinStatus()
                    loadPet()
                }
                .onFailure { _errorMessage.value = it.message }
            _isActing.value = false
        }
    }

    // === Spin ===
    fun doSpin() {
        viewModelScope.launch {
            _isActing.value = true
            petRepository.doSpin()
                .onSuccess {
                    _spinResult.value = it
                    _actionResult.value = "🎰 ${it.rewardDetail}"
                    loadPet()
                    loadEggs()
                }
                .onFailure { _errorMessage.value = it.message }
            _isActing.value = false
        }
    }

    // === Collections & Passport ===
    fun loadCollections() {
        viewModelScope.launch {
            petRepository.getCollections()
                .onSuccess { _collections.value = it }
        }
    }

    fun loadPassport() {
        viewModelScope.launch {
            petRepository.getPassport()
                .onSuccess { _passport.value = it }
        }
    }

    fun refresh() { loadPet() }

    fun clearMessages() {
        _errorMessage.value = null
        _actionResult.value = null
    }

    fun clearLevelReward() { _levelReward.value = null }
    fun clearHatchResult() { _hatchResult.value = null }
    fun clearSpinResult() { _spinResult.value = null }
}
