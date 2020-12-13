package com.example.shoppinglist.usuallist

import android.app.Application
import androidx.lifecycle.*
import com.example.shoppinglist.*
import com.example.shoppinglist.common.BaseViewModel
import com.example.shoppinglist.db.Category
import com.example.shoppinglist.db.ItemDatabaseDao
import com.example.shoppinglist.db.ShoppingItem
import com.example.shoppinglist.db.UsualItem
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


class UsualViewModel(
        dataSource: ItemDatabaseDao,
        application: Application
) : BaseViewModel(dataSource, application) {



    /**
     * [LiveData] variables. ViewModel has no access to View,
     * but lets the observing View know what's up
     */

    /**
     * Current item must be known if user taps the edit button.
     * Until then, it's set to a default (non db valid) UsualItem
     */
    private var _currentItem = UsualItem()
    val currentItem: UsualItem
        get() = _currentItem

    /**
     * Takes value of the last deleted item for undo,
     * or null if no item was deleted/if deletion is complete
     */
    private var _lastDeleted = MutableLiveData<UsualItem?>()
    val lastDeleted: LiveData<UsualItem?>
        get() = _lastDeleted

    private val _navigateToEditItem = MutableLiveData<Boolean?>()
    val navigateToEditItem: LiveData<Boolean?>
        get() = _navigateToEditItem

    private val _navigateToShopping = MutableLiveData<Boolean?>()
    val navigateToShopping: LiveData<Boolean?>
        get() = _navigateToShopping

    /**
        If the user updates a usual item, wants to update the shopping item as well
        but the updated version of the usual item is already in the shopping list,
        a message is shown
     */
    private val _showSnackCantPropagate = MutableLiveData<Boolean?>()
    val showSnackCantPropagate: LiveData<Boolean?>
    get() = _showSnackCantPropagate


    private val _showSnackAdded = MutableLiveData<Boolean?>()
    val showSnackAdded : LiveData<Boolean?>
        get() = _showSnackAdded

    private val _showSnackRemoved = MutableLiveData<Boolean?>()
    val showSnackRemoved : LiveData<Boolean?>
        get() = _showSnackRemoved

    /**
     * This is the best I've found so far to be able to account both for Live data AND Sorting
     * The idea is to have a MutableLiveData observed by the view. That way, when sorting pref
     * is modified, the MutableLiveData switches from one LiveData to the other, thanks to
     * the MediatorLiveData. The latter is custom, as the last assigned item has to be known to be removed
     * Maybe just one of the list order will be called, hence lazy declaration
     */
    private val itemsSortedByItem : LiveData<List<UsualItem>> by lazy {
        database.getAllUsualItemsByItem()
    }
    private val itemsSortedByCat : LiveData<List<UsualItem>> by lazy {
        if (catSortType == SORT_CUSTOM) database.getAllUsualItemsByCatSortRank()
        else database.getAllUsualItemsByCatSortName()
    }
    val items = MutableLiveData<List<UsualItem>>()



    init {
        // usual_items table is only updated at the last moment, when fragment is accessed
        viewModelScope.launch {
            database.updateTableUsualItems()
        }
        assignSortedItems()
    }

    /**
     * On clicks
     */


    /**
     * When checked, item is added to the shopping list. When unchecked, it is removed from shopping list
     */
    fun onItemClicked(item: UsualItem, isChecked: Boolean) {
        viewModelScope.launch {
            if (isChecked) {
                database.insertItem(
                        ShoppingItem(name = item.name, category = item.category, quantity = item.quantity))
                _showSnackAdded.value = true
            } else {
                database.deleteItem(item.name, item.category)
                _showSnackRemoved.value = true
            }
            database.updateInShoppingList(item.itemId, isChecked)
        }
    }


    /**
     * When + button is clicked, set _navigateToNewItem to true so that the Observer reacts.
     * As an extreme precaution, prevent swipe so that the user can't accidentally delete
     * an item while the dialog is loading. Otherwise, worst case would be the user presses FAB,
     * deletes an article, adds an article of the same name an category, and undoes the deletion
     */
    fun onFabClicked(){
        swipeEnabled = false
        _navigateToEditItem.value = true
    }


    /**
     * When Edit button is clicked, set _navigateToNewItem to true so that the Observer reacts
     */
    fun onEditClicked(item: UsualItem){
        //prevent accidental swipe to be done while AlertDialog is loading
        swipeEnabled = false
        _currentItem = item
        _navigateToEditItem.value = true
    }



    /**
     * When either back or back UI button is pressed, go back to the shopping list
     */
    fun onBackClicked(){
        _navigateToShopping.value = true
    }

    /**
     * Delete a usual item from the database
     */
    override fun onSwiped(item: Any){
        viewModelScope.launch {
            database.deleteUsualItem(item as UsualItem)
            _lastDeleted.value = item
        }
    }

    /**
     * Re-add into database the last item that was deleted
     */
    override fun undoDelete() {
        with(_lastDeleted.value){
            this?.let {
                addItem(name, category, quantity)
            }
        }
        _lastDeleted.value = null
    }

    /**
     * When this is called, user cannot revert delete action anymore
     */
    override fun abortUndo() {
        _lastDeleted.value = null
    }

    /**
     * Remove all items from the shopping list
     */
    fun emptyList(){
        viewModelScope.launch {
            database.deleteAllUsualItems()
        }
    }

    /**
     * The Recycler view is determined by the chosen sorted order
     */
    override fun assignSortedItems(){
        mediator.currentSource?.let {
            mediator.removeSource(mediator.currentSource as LiveData<*>)
        }
        mediator.addSource(if (sortType == SORT_BY_ITEM) itemsSortedByItem
            else itemsSortedByCat) { items.value = it }
    }

    /**
     * Insert a [UsualItem] in the database.
     * As category String is a foreign key, it must be created in table categories if nonexistent
     */
    private fun addItem(name: String, category: String, quantity: String){
        viewModelScope.launch {
            if (database.getFromCategories(category) == null){
                database.insertCategoryByName(category)
            }
            //User might have added an item that is already in the shopping list
            database.insertUsualItem(UsualItem(name = name, category = category, quantity = quantity,
            inShoppingList = if (database.getFromItems(name, category) != null) 1 else 0))

        }
    }

    /**
     * Put into the shopping list every item of the usual list that are not already there
     */
    fun transferRemainingItems() {
        viewModelScope.launch {
            database.transferAndUpdateRemainingUsualItems()
        }
    }

    /**
     *  tries to create (if toCreate) or update the item
     *  @return true if success, false if tried to create but already exist,
     *  null if item was to be created but is already in the shopping list
     */
    fun createOrUpdate(toCreate: Boolean, item: UsualItem,
                       newName: String, newCategory: String, newQuantity:String): Boolean?{
        // If name or category have changed AND target values already exist, add/update can't be done
        if ( (item.name != newName || item.category != newCategory)
                && runBlocking { database.getFromUsualItems(newName, newCategory) != null })
            return false
        if (toCreate) { // Couple (name, category) must be unique in the table
            addItem(newName, newCategory, newQuantity)
        } else { // item existed and must be updated
            if (item.inShoppingList == 1) {
                return null
            }
            else updateItem(item,
                    newName, newCategory, newQuantity,
                    null)
        }
        return true
    }

    /**
     * If item properties have changed, create the item's category - if nonexistent -
     * and update usual_items entry. If propagation is ordered, update matching item from articles
     */
    fun updateItem(oldItem: UsualItem,
                   newName: String, newCategory: String, newQuantity: String,
                   propagate: Boolean?) {
        // Update only if properties changed
        if (oldItem.name != newName
                || oldItem.category != newCategory
                || oldItem.quantity != newQuantity) {
            viewModelScope.launch {
                if (database.getFromCategories(newCategory) == null) {
                    database.insertCategoryByName(newCategory)
                }
                // If propagate is not null, it means the user has been suggested to propagate,
                // So the oldItem was in the shopping list
                propagate?.let {
                    if (oldItem.name == newName && oldItem.category == newCategory) { // Only the quantity has changed
                        if(propagate){ //update the new quantity in shopping item table as well
                            database.updateUsualItemAndItem(oldItem,
                                    newName, newCategory, newQuantity)
                            return@launch
                        } else { //don't update shopping item, but new usual item is still in the shopping list
                            database.updateUsualItem(UsualItem(itemId = oldItem.itemId,
                                    name = newName,
                                    category = newCategory,
                                    quantity = newQuantity,
                                    inShoppingList = 1))
                            return@launch
                        }
                    } else { // name and/or category have changed
                        if (propagate) { // propagate only if NEW labels aren't already in the shopping list
                            database.getFromItems(newName, newCategory)?.let {
                                _showSnackCantPropagate.value = true //no return here: usual item will still be updated
                            } ?: run {
                                database.updateUsualItemAndItem(oldItem,
                                        newName, newCategory, newQuantity)
                                return@launch
                            }
                        } // else option is handled after let {}
                    }
                }
                // Remaining cases: the usual item is new and propagation was declined ("else" from the last "if")
                // OR the usual item is new and propagation was not an option (null)
                // In both cases, the item might happen to be in the shopping list:
                database.updateUsualItem(UsualItem(itemId = oldItem.itemId,
                        name = newName,
                        category = newCategory,
                        quantity = newQuantity,
                        inShoppingList = if (database.getFromItems(newName, newCategory) == null) 0
                        else 1))

            }
        }
    }



    /**
     * When the item is added/edited or the editing is cancelled, don't navigate again
     */
    fun doneEditing() {
        _navigateToEditItem.value = false
        _currentItem = UsualItem()
    }


    /**
     * Indicator that navigation to the shopping list has been done
     */
    fun doneNavigatingToShopping() {
        _navigateToShopping.value = false
    }


    fun doneShowingSnackCantPropagate() {
        _showSnackCantPropagate.value = false
    }


    fun doneShowingSnackAdded() {
        _showSnackAdded.value = false
    }

    fun doneShowingSnackRemoved() {
        _showSnackRemoved.value = false
    }


    /**
     * Check if data is eligible for building a [UsualItem]
     * It uses access to the database, so it's not in Utils
     */
    fun itemFormatState(name: String, category: String, quantity: String, unit: String): Int {
        val foundItem = runBlocking {
            database.getFromUsualItems(name, category, if (unit == "") quantity else "$quantity $unit")
        }
        return formatState(foundItem != null, name, category, quantity)
    }


}