package com.inspirecoding.supershopper.ui.openedshoppinglist

import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import com.inspirecoding.supershopper.data.ListItem
import com.inspirecoding.supershopper.data.Resource
import com.inspirecoding.supershopper.data.ShoppingList
import com.inspirecoding.supershopper.data.User
import com.inspirecoding.supershopper.repository.shoppinglist.ShoppingListRepository
import com.inspirecoding.supershopper.repository.user.UserRepository
import com.inspirecoding.supershopper.ui.shoppinglists.ShoppingListsViewModel
import com.inspirecoding.supershopper.utils.Status
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.util.*

class OpenedShoppingListViewModel @ViewModelInject constructor(
    @Assisted private val state: SavedStateHandle,
    private val shoppingListRepository: ShoppingListRepository
): ViewModel() {

    // CONST
    private val TAG = this.javaClass.simpleName
    companion object {
        const val ARG_KEY_OPENEDSHOPPINGLIST = "openedShoppingList"
        const val ARG_KEY_DUEDATE = "dueDate"
    }

    private val _listItemEventChannel = Channel<ListItemEvent>()
    val listItemEventChannel = _listItemEventChannel.receiveAsFlow()

    val openedShoppingList = state.getLiveData<ShoppingList>(ARG_KEY_OPENEDSHOPPINGLIST)

    val currentUser = state.getLiveData<User>(ShoppingListsViewModel.ARG_KEY_USER)


    fun updateShoppingListDueDate(dueDate: Long) {
        openedShoppingList.value?.let { shoppingList ->
            shoppingList.dueDate = Date(dueDate)
            viewModelScope.launch {
                shoppingListRepository.updateShoppingListDueDate(
                    shoppingListId = shoppingList.shoppingListId,
                    dueDate = shoppingList.dueDate
                ).collect()
            }
        }
    }




    /** Events **/
    fun onShowErrorMessage(message: String) {
        viewModelScope.launch {
            _listItemEventChannel.send(ListItemEvent.ShowErrorMessage(message))
        }
    }


    sealed class ListItemEvent {
        data class ShowErrorMessage(val message: String) : ListItemEvent()
    }

}