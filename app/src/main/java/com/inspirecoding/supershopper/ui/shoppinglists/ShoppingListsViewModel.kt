package com.inspirecoding.supershopper.ui.shoppinglists

import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentChange.Type.*
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

    val currentUser = state.getLiveData<User>(ARG_KEY_USER)

    private val _shoppingListsFragmentsEventChannel = Channel<ShoppingListsFragmentsEvent>()
    val shoppingListsFragmentsEventChannel = _shoppingListsFragmentsEventChannel.receiveAsFlow()

    private var __shoppingLists = mutableListOf<ShoppingList>()
    private val _shoppingLists = MutableLiveData<Resource<List<ShoppingList>>>()
    val shoppingLists : LiveData<Resource<List<ShoppingList>>> = _shoppingLists


    fun getCurrentUserShoppingListsRealTime() {
        viewModelScope.launch {
            println("ShoppingLists - ViewModel -> viewModelScope.launch")
            currentUser.value?.let { _currentUser ->
                shoppingListRepository.getCurrentUserShoppingListsRealTime(_currentUser, viewModelScope).collect { result ->
                    println("ShoppingLists - ViewModel -> getCurrentUserShoppingListsRealTime")
                    println("ShoppingLists - ViewModel - result -> ${result.data?.size}")
                    when(result.status)
                    {
                        Status.LOADING -> {
                            _shoppingLists.postValue(Resource.Loading(result.isLoading))
                        }
                        Status.SUCCESS -> {
                            result.data?.let { list ->
                                println("ShoppingLists - ViewModel - list before -> ${list.size}")
                                println("ShoppingLists - ViewModel - list before -> ${__shoppingLists.size}")
                                updateShoppingList(list)
                                println("ShoppingLists - ViewModel - list after -> ${__shoppingLists.size}")
                                _shoppingLists.postValue(Resource.Success(__shoppingLists))
                            }
                        }
                        Status.ERROR -> {
                            result.message?.let {
                                _shoppingLists.postValue(Resource.Error(it))
                            }
                        }
                    }
//                    if(__shoppingLists.size > 0) {
//                        println("ShoppingLists - ViewModel - list after -> ${__shoppingLists.size}")
//                        _shoppingLists.postValue(Resource.Success(__shoppingLists))
//                    }
                }
            }
        }
    }

    private fun updateShoppingList(newShoppingLists: MutableList<Pair<DocumentChange, ShoppingList>>) {

        newShoppingLists.forEach { pair ->
            val shoppingList = pair.second
            when(pair.first.type)
            {
                ADDED -> {
                    val doesListAlreadyIncluded = __shoppingLists.find {
                        it.shoppingListId == shoppingList.shoppingListId
                    }
                    if(doesListAlreadyIncluded == null) {
                        __shoppingLists.add(shoppingList)
                        println("ShoppingLists - ViewModel - ADDED -> $shoppingList")
                    }
                }
                MODIFIED -> {
                    println("ShoppingLists - ViewModel - newShoppingLists.size -> ${newShoppingLists.size}")
                    val index = __shoppingLists.indexOfLast {
                        it.shoppingListId == shoppingList.shoppingListId
                    }
                    println("ShoppingLists - ViewModel - __shoppingLists.indexOfFirst -> $index")
                    println("ShoppingLists - ViewModel - __shoppingLists.indexOfFirst -> ${shoppingList.name}")
                    if(index != -1) __shoppingLists[index] = shoppingList
                }
                REMOVED -> {
                    __shoppingLists.remove(shoppingList)
                }
            }

            __shoppingLists.sortByDescending { it.dueDate }
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
            friendsSharedWith = mutableListOf((currentUser.value as User).id)
        )
    }


    fun updateCurrentUser(currentUser: User) {
        this.currentUser.postValue(currentUser)
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

    fun clearShoppingListResult() {
        shoppingListRepository.clearShoppingListResult()
    }
    override fun onCleared() {
        super.onCleared()
        clearShoppingListResult()
    }
}