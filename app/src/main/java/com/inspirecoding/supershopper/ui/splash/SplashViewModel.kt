package com.inspirecoding.supershopper.ui.splash

import androidx.appcompat.app.AppCompatDelegate
import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import com.inspirecoding.supershopper.data.User
import com.inspirecoding.supershopper.repository.datastore.DataStoreRepository
import com.inspirecoding.supershopper.repository.local.ShopperRepository
import com.inspirecoding.supershopper.repository.user.UserRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.switchMap
import kotlinx.coroutines.launch

class SplashViewModel @ViewModelInject constructor(
    private val shopperRepository: ShopperRepository,
    private val userRepository: UserRepository,
    private val dataStoreRepository: DataStoreRepository,
    @Assisted private val state: SavedStateHandle
): ViewModel() {

    private val _splashEventChannel = Channel<SplashEvent>()
    val splashEventChannel = _splashEventChannel.receiveAsFlow()

    val userResource = userRepository.userResource

    val notificationsSettingsFromDataStore = dataStoreRepository.readNotificationsSettingFromDataStore.asLiveData()
    val nightModeSettingsFromDataStore = dataStoreRepository.readNightModeSettingFromDataStore.asLiveData()


    fun saveNotificationsSettingsToDataStore(areTurnedOn: Boolean) = viewModelScope.launch {
        dataStoreRepository.saveNotificationsSettingToDataStore(areTurnedOn = areTurnedOn)
    }
    fun saveNightModeSettingsToDataStore(isTurnedOn: Boolean) = viewModelScope.launch {
        dataStoreRepository.saveNightModeSettingToDataStore(isTurnedOn = isTurnedOn)
    }



    private fun getListOfCategories() {
        viewModelScope.launch {
            shopperRepository.getCategoriesSuspend()
        }
    }

    fun checkUserLoggedIn() {
        viewModelScope.launch {
            userRepository.checkUserLoggedIn(this)
        }
    }

    fun updateFirebaseInstanceTokenOFUserInFirestore(
        user: User
    ) {
        viewModelScope.launch {
            userRepository.getFirebaseInstanceToken().collect { token ->
                if(user.firebaseInstanceToken.contains(token)) {
                    onNavigateToShoppingListsFragment(user)
                } else {
                    userRepository.updateFirebaseInstanceTokenOFUserInFirestore(user, viewModelScope).collect()
                }
            }
        }
    }

    /** Events **/
    fun onLoginSelected() {
        viewModelScope.launch {
            _splashEventChannel.send(SplashEvent.NavigateToLoginFragment)
        }
    }
    fun onRegistrationSelected() {
        viewModelScope.launch {
            _splashEventChannel.send(SplashEvent.NavigateToRegisterFragment)
        }
    }
    fun onNavigateToShoppingListsFragment(user: User) {
        viewModelScope.launch {
            _splashEventChannel.send(SplashEvent.NavigateToShoppingListsFragment(user))
        }
    }
    fun onTurnNightMode(isTurnedOn: Boolean) {
        viewModelScope.launch {
            println("onTurnNightMode -> $isTurnedOn")
            _splashEventChannel.send(SplashEvent.TurnNightMode(isTurnedOn))
        }
    }
    fun onShowErrorMessage(message: String) {
        viewModelScope.launch {
            _splashEventChannel.send(SplashEvent.ShowErrorMessage(message))
        }
    }

    sealed class SplashEvent {
        object NavigateToLoginFragment : SplashEvent()
        object NavigateToRegisterFragment : SplashEvent()
        data class TurnNightMode(val isTurnedOn: Boolean) : SplashEvent()
        data class ShowErrorMessage(val message: String) : SplashEvent()
        data class NavigateToShoppingListsFragment(val user: User) : SplashEvent()
    }
}