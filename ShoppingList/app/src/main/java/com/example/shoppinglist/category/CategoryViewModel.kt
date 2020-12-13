package com.example.shoppinglist.category

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.database.sqlite.SQLiteConstraintException
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.shoppinglist.*
import com.example.shoppinglist.common.BaseViewModel
import com.example.shoppinglist.db.Category
import com.example.shoppinglist.db.ItemDatabaseDao
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class CategoryViewModel(
    dataSource: ItemDatabaseDao, application: Application
) : BaseViewModel(dataSource, application) {



    private val _navigateToEditItem = MutableLiveData<Boolean?>()
    val navigateToEditItem: LiveData<Boolean?>
        get() = _navigateToEditItem

    private val _navigateToShopping = MutableLiveData<Boolean?>()
    val navigateToShopping: LiveData<Boolean?>
        get() = _navigateToShopping

    private var _showSnackbarDelForItem = MutableLiveData<Boolean?>()
    val showSnackBarDelForItem: LiveData<Boolean?>
        get() = _showSnackbarDelForItem

    private var _showSnackbarDelAll = MutableLiveData<Boolean?>()
    val showSnackBarDelAll: LiveData<Boolean?>
        get() = _showSnackbarDelAll

    /**
     * Listened by category_fragment to determine if warning about drag and drop being disabled
     * should be displayed (it will be if sorting order is alpha)
     */
    private var _isWarningVisible = MutableLiveData<Boolean?>()
    val isWarningVisible: LiveData<Boolean?>
        get() = _isWarningVisible

    /**
     * Current category must be known if user taps the edit button.
     * Until then, it's set to a default (non db valid) Category
     */
    private var _currentCat = Category()
    val currentCat: Category
        get() = _currentCat

    /**
     * Value to be observed by the view. Takes value of the last deleted item for undo,
     * or null if no item was deleted/if deletion is complete
     */
    private var _lastDeleted = MutableLiveData<Category?>()
    val lastDeleted: LiveData<Category?>
        get() = _lastDeleted

    /**
     * cf explanation in other view models
     */
    private val catSortedAlpha : LiveData<List<Category>> by lazy {
        dataSource.getAllCatsLiveAlpha()
    }
    private val catSortedCustom : LiveData<List<Category>> by lazy {
        dataSource.getAllCatsLiveByRank()
    }
    val categories = MutableLiveData<List<Category>>()



    init {
        _isWarningVisible.value = catSortType == SORT_ALPHA
        assignSortedItems()
    }

    /**
     * When Edit button is clicked, set _navigateToNewItem to true so that the Observer reacts
     */
    fun onEditClicked(category: Category){
        //prevent accidental swipe to be done while AlertDialog is loading
        swipeEnabled = false
        _currentCat = category
        _navigateToEditItem.value = true
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

    fun onBackClicked(){
        _navigateToShopping.value = true
    }


    override fun onSwiped(item: Any){
        deleteItem(item as Category)
    }

    /**
     * Re-add into database the last item that was deleted.
     * As categories can be dragged, it is possible that a user deletes a category
     * then drags another category which takes the rank of the deleted category,
     * and then undoes the deletion, which would cause the categories to have the same rank
     */
    override fun undoDelete() {
        with(_lastDeleted.value){
            this?.let {
                viewModelScope.launch {
                    when {
                        database.getCatFromRank(it.rank) == null -> addItem(it)
                        database.getCatFromRank(it.rank-1) == null -> addItem(it.apply { rank-- })
                        database.getCatFromRank(it.rank+1) == null -> addItem(it.apply { rank++ })
                        else -> addItem(it.categoryId) //last resort : no place was found, insert at the end
                    }
                }

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
     * The Recycler view is determined by the chosen sorted order
     */
    override fun assignSortedItems(){
        mediator.currentSource?.let {
            mediator.removeSource(mediator.currentSource as LiveData<*>)
        }
        mediator.addSource(if (catSortType == SORT_ALPHA) catSortedAlpha
        else catSortedCustom) { categories.value = it }
    }

    /**
     * Delete every entry in the categories table.
     * If all are used, none are deleted: let the user know
     */
    fun deleteUnusedCats() {
        viewModelScope.launch {
            if (database.deleteUnusedCats() == 0) _showSnackbarDelAll.value = true
        }
    }

    fun createOrUpdate(isNew: Boolean, oldItem: Category, newName: String): Boolean{
        // If target values already exist, add/update can't be done
        if (runBlocking { database.getFromCategories(newName) != null })
            return false
        if (isNew)
            addItem(newName)
        else
            updateCategory(oldItem.categoryId, newName)
        return true
    }

    private fun addItem(categoryName: String) {
        viewModelScope.launch {
            database.insertCategoryByName(categoryName)
        }
    }

    /**
     * Dedicated for the case where category deletion is undone, as otherwise ID and rank should
     * be determined by the DB
     */
    private fun addItem(newCategory: Category) {
        viewModelScope.launch {
            database.insertCategory(newCategory)
        }
    }

    private fun updateCategory(oldCat: String, newCat: String) {
        viewModelScope.launch {
            database.updateCatName(oldCat, newCat)
        }
    }

//    fun updateCategories(catList: List<Category>, adapter: CategoryAdapter) {
//        if (catList.isNotEmpty()) {
//            viewModelScope.launch {
//                database.updateCategories(catList)
//            }
//        }
//    }

    private fun deleteItem(category: Category) {
        viewModelScope.launch {
            try {
                database.deleteCategory(category)
                _lastDeleted.value = category
            } catch (e: SQLiteConstraintException) {
                _showSnackbarDelForItem.value = true
            }
        }
    }


    /**
     * Moves a category rank from current to target
     */
    fun updateCatRank(catFrom: Category?, catTarget: Category?) {
        catFrom?.let {
            catTarget?.let {
                viewModelScope.launch {
                    database.updateCatRanks(catFrom, catTarget)
                }
            }
        }
    }


    fun doneEditing(){
        _navigateToEditItem.value = false
        _currentCat = Category()
    }

    fun doneShowingSnackbarDel() {
        _showSnackbarDelForItem.value = false
    }

    fun doneShowingSnackbarDelAll() {
        _showSnackbarDelAll.value = false
    }

    fun doneNavigatingToShopping() {
        _navigateToShopping.value = false
    }


    override fun editSharedPref(sortBy: Int) {
        val pref: SharedPreferences = application.getSharedPreferences("Share", Context.MODE_PRIVATE)
        val edit = pref.edit()
        edit.putInt("catSortBy", sortBy)
        edit.apply()
        catSortType = sortBy
        _isWarningVisible.value = catSortType == SORT_ALPHA
    }

    fun isDragPossible(): Boolean {
        return catSortType == SORT_CUSTOM
    }


}