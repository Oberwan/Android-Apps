package com.example.shoppinglist.mainlist

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.shoppinglist.db.ItemDatabaseDao

/**
 * Boiler plate code for a ViewModel Factory.
 * Provides the ItemDatabaseDao and context to the ViewModel.
 */
class ItemViewModelFactory(
    private val dataSource: ItemDatabaseDao,
    private val application: Application
) : ViewModelProvider.Factory {
    @Suppress("unchecked_cast")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ItemViewModel::class.java)) {
            return ItemViewModel(dataSource, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}