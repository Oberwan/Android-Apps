package com.example.shoppinglist.mainlist

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.shoppinglist.*
import com.example.shoppinglist.common.BaseViewModel
import com.example.shoppinglist.db.ItemDatabaseDao
import com.example.shoppinglist.db.ShoppingItem
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class ItemViewModel(
    dataSource: ItemDatabaseDao, application: Application
) : BaseViewModel(dataSource, application) {

    /**
     * [LiveData] variables. ViewModel has no access to View,
     * but lets the observing View know what's up
     */

    // Current item must be known if user taps the edit button.
    // Until then, it's set to a default (non db valid) ShoppingItem
    private var _currentItem = ShoppingItem()
    val currentItem: ShoppingItem
        get() = _currentItem

    /**
     * Value to be observed by the view. Takes value of the last deleted item for undo,
     * or null if no item was deleted/if deletion is complete
     */
    private var _lastDeleted = MutableLiveData<ShoppingItem?>()
    val lastDeleted: LiveData<ShoppingItem?>
        get() = _lastDeleted

    private val _navigateToEditItem = MutableLiveData<Boolean?>()
    val navigateToEditItem: LiveData<Boolean?>
        get() = _navigateToEditItem

    private val _navigateToCategories = MutableLiveData<Boolean?>()
    val navigateToCategories: LiveData<Boolean?>
        get() = _navigateToCategories

    private val _navigateToUsual = MutableLiveData<Boolean?>()
    val navigateToUsual: LiveData<Boolean?>
        get() = _navigateToUsual

    private val _smsToSend = MutableLiveData<String?>()
    val smsToSend: LiveData<String?>
        get() = _smsToSend

    // Live list from table articles
    /**
     * This is the best I've found so far to be able to account both for Live data AND Sorting
     * The idea is to have a MutableLiveData observed by the view. That way, when sorting pref
     * is modified, the MutableLiveData switches from one LiveData to the other, thanks to
     * the MediatorLiveData. The latter is custom, as the last assigned item has to be known to be removed
     * Maybe just one of the list order will be called, hence lazy declaration
     */
    private val itemsSortedByItem : LiveData<List<ShoppingItem>> by lazy {
        database.getAllItemsByItem()
    }
    private val itemsSortedByCat : LiveData<List<ShoppingItem>> by lazy {
        // According to the cat sort, items should be displayed by cat name or cat rank
        if (catSortType == SORT_CUSTOM) database.getAllItemsByCatSortRank()
        else database.getAllItemsByCatSortName()
    }
    val items = MutableLiveData<List<ShoppingItem>>()


    init {
        assignSortedItems()
    }

    /**
     * On clicks
     */

    /**
     * If item checkbox is checked, it is in cart/bought.
     */
    fun onItemClicked(itemId: Long, isChecked: Boolean) {
    viewModelScope.launch {
        database.updateInCart(itemId, isChecked)
        }
    }


    /**
     * Let the View observer know it can access the Category Fragment
     */
    fun onCatOptionMenuClicked(){
        _navigateToCategories.value = true
    }

    /**
     * Let the View observer know it can access the Usual Fragment
     */
    fun onUsualOptionMenuClicked(){
        _navigateToUsual.value = true
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
    fun onEditClicked(item: ShoppingItem){
        //prevent accidental swipe to be done while AlertDialog is loading
        swipeEnabled = false
        _currentItem = item
        _navigateToEditItem.value = true
    }

    /**
     * Delete an item from the database
     */
    override fun onSwiped(item: Any){
        viewModelScope.launch {
            database.deleteItem(item as ShoppingItem)
            _lastDeleted.value = item
        }
    }

    /**
     * Re-add into database the last item that was deleted
     */
    override fun undoDelete() {
        with(_lastDeleted.value){
            this?.let {
                addItem(name, category, quantity, inCart)
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
     * Various database actions
     */

    /**
     * Remove checked items from the shopping list
     */
    fun clearChecked(){
        viewModelScope.launch {
            database.deleteInCart()
        }
    }



    /**
     * Remove all items from the shopping list
     */
    fun emptyList(){
        viewModelScope.launch {
            database.deleteAllItems()
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
     * Insert an item in the database.
     * As category String is a foreign key, it must be created in table categories if nonexistent
     */
    private fun addItem(name: String, category: String, quantity: String, inCart:Int = 0){
        viewModelScope.launch {
            if (database.getFromCategories(category) == null){
                database.insertCategoryByName(category)
            }
            database.insertItem(ShoppingItem(name = name, category = category, quantity = quantity, inCart = inCart))
        }
    }

    /**
     * Orders the creation or update of an item, if it is not illegal
     */
    fun createOrUpdate(toCreate: Boolean, item: ShoppingItem,
                       newName: String, newCategory: String, newQuantity: String): Boolean {
        // If name or category have changed AND target values already exist, add/update can't be done
        if ( (item.name != newName || item.category != newCategory)
                && runBlocking { database.getFromItems(newName, newCategory) != null })
            return false
        if (toCreate) { // Couple (name, category) must be unique in the table
            addItem(newName, newCategory, newQuantity)
        } else { // Item existed and must be updated
            updateItem(item,
                    newName, newCategory, newQuantity)
        }
        return true
    }

    /**
     * Updates a shopping item. If the new category doesn't exist, create it
     */
    private fun updateItem(oldItem: ShoppingItem, newName: String, newCategory: String, newQuantity: String) {
        // Update only if properties changed
        if (oldItem.name != newName
                || oldItem.category != newCategory
                || oldItem.quantity != newQuantity) {
            viewModelScope.launch {
                if (database.getFromCategories(newCategory) == null) {
                    database.insertCategoryByName(newCategory)
                }
                database.updateItem(ShoppingItem(itemId = oldItem.itemId,
                        name = newName,
                        category = newCategory,
                        quantity = newQuantity,
                        inCart = oldItem.inCart))
            }
        }
    }


    /**
     * Display Send list sorted by categories by SMS
     */
    fun sendViaSMS() : Boolean {
        val listToSend = runBlocking {
            if (catSortType == SORT_CUSTOM) database.getAllItemsByCatSortRankSus()
            else database.getAllItemsByCatSortNameSus()
        }
        listToSend?.let {
            if (listToSend.isEmpty()) return false
            _smsToSend.value = shapeSMS(it)
            return true
        }
        return false
    }


    /**
     * Navigation [LiveData] updates
     */



    /**
     * When the item is added/edited or the editing is cancelled, don't navigate again
     */
    fun doneEditing() {
        _navigateToEditItem.value = false
        _currentItem = ShoppingItem()
    }

    /**
     * When navigation to categories is done, don't navigate again
     */
    fun doneNavigatingToCat(){
        _navigateToCategories.value = false
    }

    /**
     * When navigation to usual items is done, don't navigate again
     */
    fun doneNavigatingToUsual(){
        _navigateToUsual.value = false
    }

    fun doneSendingSMS(){
        _smsToSend.value = null
    }


    /**
     * Check if data is eligible for building a [ShoppingItem]
     * It uses access to the database, so it's not in Utils
     */
    fun itemFormatState(name: String, category: String, quantity: String, unit: String): Int {
        val foundItem = runBlocking {
            database.getFromItems(name, category, if (unit == "") quantity else "$quantity $unit")
        }
        return formatState(foundItem != null, name, category, quantity)
    }



}