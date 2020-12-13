package com.example.shoppinglist.category

import android.annotation.SuppressLint
import android.widget.TextView
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.shoppinglist.db.Category
import com.example.shoppinglist.db.ShoppingItem
import com.example.shoppinglist.mainlist.ItemAdapter


/**
 * Binder for the recyclerview
 */
//@BindingAdapter("listDataCat")
//fun bindRecyclerView(recyclerView: RecyclerView,
//                     data: List<Category>?) {
//    val adapter = recyclerView.adapter as CategoryAdapter
//    adapter.submitList(data)
//}


/**
 * Binder for the items of the list
 */
@BindingAdapter("categoryId")
fun TextView.setCategoryId(item: Category?){
    item?.let {
        text = item.categoryId
    }
}