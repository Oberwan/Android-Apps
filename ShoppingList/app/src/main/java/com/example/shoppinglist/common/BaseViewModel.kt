package com.example.shoppinglist.common

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shoppinglist.SORT_CUSTOM
import com.example.shoppinglist.SorterMediatorLiveData
import com.example.shoppinglist.db.Category
import com.example.shoppinglist.db.ItemDatabaseDao
import com.example.shoppinglist.db.ShoppingItem
import com.example.shoppinglist.sortType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

/**
 * BaseViewModel could contain a lot more functions if data classes extending a non-data class
 * didn't cause a problem with Room database
 */
abstract class BaseViewModel(
        protected val database: ItemDatabaseDao,
        protected val application: Application
) : ViewModel() {

    //not protected because it needs to be observed by the Fragment
    val mediator = SorterMediatorLiveData<Unit>()

    /**
     * This value will be asked by the DragManageAdapter. It should be temporarily disabled
     * if the user taps the edit button, because they still have time to accidentally swipe
     * before the dialog appears, which is undesirable
     */
    protected var swipeEnabled = true

    init {
        swipeEnabled = true
    }

    fun enableSwipe(){
        swipeEnabled = true
    }
    fun isSwipeEnabled(): Boolean{
        return swipeEnabled
    }

    /**
     * get every [Category] from table categories. Main Thread must wait for them to be retrieved
     */
    fun getAllCats(): List<Category>?{
        return runBlocking {
            database.getAllCategories()
        }
    }

//    protected fun getCatSortType() : Int{
//        return application.getSharedPreferences("Share", AppCompatActivity.MODE_PRIVATE)
//            .getInt("catSortBy", SORT_CUSTOM)
//    }

    protected open fun editSharedPref(sortBy: Int) {
        val pref: SharedPreferences = application.getSharedPreferences("Share", Context.MODE_PRIVATE)
        val edit = pref.edit()
        edit.putInt("sortBy", sortBy)
        edit.apply()
        sortType = sortBy
    }

    fun updateSorting(sortValue: Int, newSort: Int){
        if (sortValue != newSort){
            editSharedPref(newSort)
            assignSortedItems()
        }
    }

    /**
     * This could be cleaner with item being of "BaseItem" type if ShoppingItem, UsualItem and Category
     * could extend a same parent class without conflict with Room
     */
    abstract fun onSwiped(item: Any)

    abstract fun assignSortedItems()

    abstract fun undoDelete()

    abstract fun abortUndo()


}