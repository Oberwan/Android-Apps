package com.example.shoppinglist.usuallist

import android.annotation.SuppressLint
import android.util.Log
import android.widget.TextView
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.shoppinglist.db.ShoppingItem
import com.example.shoppinglist.db.UsualItem
import com.example.shoppinglist.mainlist.ItemAdapter


/**
 * Binder for the recyclerview
 */
@BindingAdapter("listDataUsual")
fun bindRecyclerView(recyclerView: RecyclerView,
                     data: List<UsualItem>?) {
    val adapter = recyclerView.adapter as UsualAdapter
    adapter.submitList(data)
}



/**
 * Binders for the items of the list
 */


@BindingAdapter("itemCategory")
fun TextView.setItemCategory(item: UsualItem?){
    item?.let {
        text = item.category
    }
}

@BindingAdapter("itemName")
fun TextView.setItemName(item: UsualItem?){
    item?.let {
        text = item.name
    }
}

@SuppressLint("SetTextI18n") // to prevent complaint about string concatenation
@BindingAdapter("itemQuantity")
fun TextView.setItemQuantity(item: UsualItem?){
    item?.let {
        text = "x" + item.quantity
    }
}


//
///**
// * ViewHolder that holds a single [TextView].
// *
// * A ViewHolder holds a view for the [RecyclerView] as well as providing additional information
// * to the RecyclerView such as where on the screen it was last drawn during scrolling.
// */
//class TextItemViewHolder(val textView: TextView): RecyclerView.ViewHolder(textView)