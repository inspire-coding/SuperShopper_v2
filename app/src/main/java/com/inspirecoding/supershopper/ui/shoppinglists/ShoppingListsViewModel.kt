package com.inspirecoding.supershopper.ui.shoppinglists

import android.util.Log
import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import com.inspirecoding.supershopper.data.Resource
import com.inspirecoding.supershopper.data.ShoppingList
import com.inspirecoding.supershopper.data.User
import com.inspirecoding.supershopper.repository.local.ShopperRepository
import com.inspirecoding.supershopper.repository.shoppinglist.ShoppingListRepository
import com.inspirecoding.supershopper.repository.user.UserRepository
import com.inspirecoding.supershopper.utils.Status
import com.inspirecoding.supershopper.utils.ValidateMethods
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.util.*

class ShoppingListsViewModel @ViewModelInject constructor(
    private val shopperRepository: ShopperRepository,
    private val userRepository: UserRepository,
    private val shoppingListRepository: ShoppingListRepository,
    @Assisted private val state: SavedStateHandle
) : ViewModel() {

    // CONST
    private val TAG = this.javaClass.simpleName
    private val OPEN = "OPEN"
    companion object {
        const val  ARG_KEY_USER = "user"
    }

    var shoppingListName: String? = null

    val user = state.getLiveData<User>(ARG_KEY_USER)
    val currentUser = state.getLiveData<User>(ARG_KEY_USER)

    private val _shoppingListsFragmentsEventChannel = Channel<ShoppingListsFragmentsEvent>()
    val shoppingListsFragmentsEventChannel = _shoppingListsFragmentsEventChannel.receiveAsFlow()

    private val __shoppingLists = mutableListOf<ShoppingList>()
    private val _shoppingLists = MutableLiveData<Resource<List<ShoppingList>>>()
    val shoppingLists : LiveData<Resource<List<ShoppingList>>> = _shoppingLists


    fun getCurrentUserShoppingListsRealTime() {
        viewModelScope.launch {
            currentUser.value?.let { _currentUser ->
                shoppingListRepository.getCurrentUserShoppingListsRealTime(_currentUser, viewModelScope).collect { result ->
                    when(result.status)
                    {
                        Status.LOADING -> {
                            _shoppingLists.postValue(Resource.Loading(true))
                        }
                        Status.SUCCESS -> {
                            result.data?.map { _shoppingList ->
                                _shoppingList.friendsSharedWith.map { friendId ->
                                    userRepository.getUserFromFirestore(friendId).collect { userResult ->
                                        when(userResult.status)
                                        {
                                            Status.LOADING -> {
                                                _shoppingLists.postValue(Resource.Loading(true))
                                            }
                                            Status.SUCCESS -> {
                                                userResult.data?.let {
                                                    _shoppingList.usersSharedWith.add(it)
                                                }
                                            }
                                            Status.ERROR -> {
                                                result.message?.let {
                                                    _shoppingLists.postValue(Resource.Error(it))
                                                }
                                            }
                                        }
                                    }
                                }

                                updateShoppingList(_shoppingList)
                            }

                            _shoppingLists.postValue(Resource.Success(__shoppingLists))
                        }
                        Status.ERROR -> {
                            result.message?.let {
                                _shoppingLists.postValue(Resource.Error(it))
                            }
                        }
                    }
                }
            }
        }
    }

    private fun updateShoppingList(shoppingList: ShoppingList) {
        val index = __shoppingLists.indexOfFirst {
            shoppingList.shoppingListId == it.shoppingListId
        }

        if(index != -1) {
            __shoppingLists[index] = shoppingList
        } else {
            __shoppingLists.add(shoppingList)
        }
    }

    fun validateShoppingListName(): Boolean {
        val errorMessage = ValidateMethods.validateName(shoppingListName)

        if (errorMessage != "") onShowErrorMessage(errorMessage)

        return errorMessage.isEmpty()
    }

    fun insertShoppingList() {
        viewModelScope.launch {
            val shoppingList = createShoppingListObject()
            shoppingListRepository.insertShoppingList(shoppingList).collect { result ->
                when(result.status)
                {
                    Status.LOADING ->  {
                        _shoppingLists.postValue(Resource.Loading(true))
                    }
                    Status.SUCCESS ->  {
                        onOpenSelectedShoppingList(shoppingList)
                    }
                    Status.ERROR -> {
                        result.message?.let {
                            onShowErrorMessage(it)
                        }
                    }
                }
            }
        }
    }
    private fun createShoppingListObject(): ShoppingList {
        return ShoppingList(
            shoppingListId =  UUID.randomUUID().toString(),
            name = shoppingListName!!,
            dueDate = Date(),
            timeStamp = Date().time,
            shoppingListStatus = OPEN,
            friendsSharedWith = mutableListOf((user.value as User).id)
        )
    }





    /** Events **/
    fun onOpenSettings() {
        viewModelScope.launch {
            _shoppingListsFragmentsEventChannel.send(ShoppingListsFragmentsEvent.NavigateToSettingsFragment)
        }
    }
    fun onOpenFriends() {
        viewModelScope.launch {
            currentUser.value?.let {
                _shoppingListsFragmentsEventChannel.send(ShoppingListsFragmentsEvent.NavigateToFriendsFragment(it))
            }
        }
    }
    fun onOpenCurrentUserProfile() {
        viewModelScope.launch {
            currentUser.value?.let {
                _shoppingListsFragmentsEventChannel.send(ShoppingListsFragmentsEvent.NavigateToCurrentUserProfileFragment(it))
            }
        }
    }
    fun onOpenSelectedShoppingList(shoppingList: ShoppingList) {
        viewModelScope.launch {
            _shoppingListsFragmentsEventChannel.send(ShoppingListsFragmentsEvent.OpenSelectedShoppingList(shoppingList))
        }
    }
    fun onShowErrorMessage(message: String) {
        viewModelScope.launch {
            _shoppingListsFragmentsEventChannel.send(ShoppingListsFragmentsEvent.ShowErrorMessage(message))
        }
    }






    sealed class ShoppingListsFragmentsEvent {
        object NavigateToSettingsFragment : ShoppingListsFragmentsEvent()
        data class NavigateToFriendsFragment(val currentUser: User) : ShoppingListsFragmentsEvent()
        data class NavigateToCurrentUserProfileFragment(val currentUser: User) : ShoppingListsFragmentsEvent()
        data class OpenSelectedShoppingList(val shoppingList: ShoppingList) : ShoppingListsFragmentsEvent()
        data class ShowErrorMessage(val message: String) : ShoppingListsFragmentsEvent()
    }


    override fun onCleared() {
        super.onCleared()
        shoppingListRepository.clearShoppingListResult()
    }
}