package com.example.shoppinglist.category

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.shoppinglist.db.ItemDatabaseDao
import com.example.shoppinglist.mainlist.ItemViewModel


class CategoryViewModelFactory (
        private val dataSource: ItemDatabaseDao, private val application: Application
        ) : ViewModelProvider.Factory {
    @Suppress("unchecked_cast")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CategoryViewModel::class.java)) {
            return CategoryViewModel(dataSource, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}