package com.example.shoppinglist.mainlist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.shoppinglist.SORT_BY_CAT
import com.example.shoppinglist.SORT_BY_ITEM
import com.example.shoppinglist.databinding.ShoppingItemBinding
import com.example.shoppinglist.db.ShoppingItem
import com.example.shoppinglist.sortType

class ItemAdapter(private val viewModel: ItemViewModel)
    : ListAdapter<ShoppingItem, ItemAdapter.ViewHolder>(ItemDiffCallback()){


    /**
     * Notifies the adapter next update will refresh all the list (sort will change)
     */
    private var notifyNextUpdate = false


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent)
    }

    /**
     * Bind each item to the viewModel and display category only once per group of the same category,
     * or not at all if items are sorted by name
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, viewModel)
        if (sortType != SORT_BY_ITEM){
            if (position > 0 && getItem(position - 1).category == item.category)
                holder.binding.catInList.visibility = View.GONE
            else
                holder.binding.catInList.visibility = View.VISIBLE
        } else holder.binding.catInList.visibility = View.GONE
    }


    private fun notifyWholeChange(){
        notifyItemRangeChanged(0, currentList.size)
    }

    /**
     * As we want categories to display only once per group under the same category,
     * a problem arise when the user changes sorting type. Indeed, when going from sorting by item
     * to sorting by categories, the viewholders update and THEN switch places, so the same
     * category might be displayed many times as a result. So, in the case of a sorting update,
     * we have to notify the adapter AFTER the update is done. We use notifyItemRangeChanged
     * for all the list, because notifyDataSetChanged botches the animation and is not recommended
     */
    override fun onCurrentListChanged(previousList: MutableList<ShoppingItem>, currentList: MutableList<ShoppingItem>) {
        super.onCurrentListChanged(previousList, currentList)
        if (notifyNextUpdate) {
            if (currentList.size > 0) {
                notifyWholeChange()
            }
            notifyNextUpdate = false
        } else if (currentList.size > 0 && sortType == SORT_BY_CAT)
            updateAccordingToChange(currentList, previousList)
    }


    /**
     * If an item has been added, maybe notify its neighbour below
     * If an item has been deleted, maybe notify its former neighbour
     * If an item has been updated, notify the whole list
     */
    private fun updateAccordingToChange(
        currentList: MutableList<ShoppingItem>,
        previousList: MutableList<ShoppingItem>
    ) {
        when {
            //Look for a new item and its index, notify its current neighbour below it shan't display category
            currentList.size > previousList.size -> {
                val toMinusFrom = currentList.withIndex().minus(previousList.withIndex())
                notifyItemBelow(toMinusFrom, currentList, 1)
            }
            //Look for a deleted item and its index, notify its former neighbour below it shall display category
            currentList.size < previousList.size -> {
                val fromMinusTo = previousList.withIndex().minus(currentList.withIndex())
                notifyItemBelow(fromMinusTo, previousList, 0)
            }
            //sizes are equal, it is an update
            else -> notifyWholeChange()
        }
    }

    /**
     * If an item has been added or deleted, its current resp. former neighbour might have to be
     * notified to stop displaying resp. display its category
     * @param offset Must be 1 if item is added, 0 if item has been deleted (its index is now used by
     * the following item)
     */
    private fun notifyItemBelow(
        indexedItem: List<IndexedValue<ShoppingItem>>,
        refList: MutableList<ShoppingItem>,
        offset: Int
    ) {
        if (indexedItem.isNotEmpty()) {
            val itemIndex = indexedItem[0].index
            //check item is not last, otherwise there is no item to notify
            if (itemIndex != refList.lastIndex) {
                val item = indexedItem[0].value
                // is the item the first / first of its category?
                if (itemIndex == 0 || refList[itemIndex-1].category != item.category) {
                    //if the (current resp. former) following item has the same category, notify it
                    if (refList[itemIndex+1].category == item.category) {
                        notifyItemChanged(itemIndex + offset)
                    }
                }
            }
        }
    }


    /**
     * Notifies the adapter that an extra notification will be needed after current list is changed
     */
    fun onSortUpdateUpcoming(){
        notifyNextUpdate = true
    }


    class ViewHolder private constructor(val binding: ShoppingItemBinding):
        RecyclerView.ViewHolder(binding.root){

        fun bind(item: ShoppingItem, viewModel: ItemViewModel/*clickListener: ItemListener*/){
            binding.item = item
            binding.itemViewModel = viewModel
            binding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup): ViewHolder{
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ShoppingItemBinding.inflate(layoutInflater, parent, false)
                return ViewHolder(binding)
            }
        }

    }


}


class ItemDiffCallback : DiffUtil.ItemCallback<ShoppingItem>() {

    override fun areItemsTheSame(oldItem: ShoppingItem, newItem: ShoppingItem): Boolean {
        return oldItem.itemId == newItem.itemId
    }

    override fun areContentsTheSame(oldItem: ShoppingItem, newItem: ShoppingItem): Boolean {
        return oldItem == newItem
    }
}
