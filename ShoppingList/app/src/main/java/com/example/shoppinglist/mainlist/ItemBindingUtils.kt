package com.example.shoppinglist.mainlist

import android.annotation.SuppressLint
import android.graphics.Paint
import android.util.Log
import android.widget.TextView
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.shoppinglist.db.ShoppingItem

/**
 * Binder for the recyclerview
 */
@BindingAdapter("listData")
fun bindRecyclerView(recyclerView: RecyclerView,
                     data: List<ShoppingItem>?) {
    val adapter = recyclerView.adapter as ItemAdapter
    adapter.submitList(data)
}


/**
 * Binders for the items of the list
 */



@BindingAdapter("itemCategory")
fun TextView.setItemCategory(item: ShoppingItem?){
    item?.let {
        text = item.category
    }
}

@BindingAdapter("itemName")
fun TextView.setItemName(item: ShoppingItem?){
    item?.let {
        text = item.name
        if (item.inCart == 1) {
            paintFlags = paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            paintFlags = paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        }
    }
}

@SuppressLint("SetTextI18n") // to prevent complaint about string concatenation
@BindingAdapter("itemQuantity")
fun TextView.setItemQuantity(item: ShoppingItem?){
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