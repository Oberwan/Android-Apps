package com.example.shoppinglist.common

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.Log
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import javax.security.auth.login.LoginException

/**
 * Manager for swiping Shopping items in order to delete them
 */
class BaseDragManageAdapter(var viewModel: BaseViewModel, var adapter: ListAdapter<Any, RecyclerView.ViewHolder>, dragDirs: Int, swipeDirs: Int)
    : ItemTouchHelper.SimpleCallback(dragDirs, swipeDirs){
    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        return false
    }

    /**
     * Prevent categories drag and drop if sorting is set to alphabetically
     */
    override fun isLongPressDragEnabled(): Boolean {
        return false
    }

    override fun isItemViewSwipeEnabled(): Boolean {
        return viewModel.isSwipeEnabled()
    }


    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        super.onSelectedChanged(viewHolder, actionState)

        with(viewHolder?.itemView?.background){
            if (this is ColorDrawable && this.color == Color.WHITE)
                viewHolder?.itemView?.setBackgroundColor(Color.RED)
        }
    }

    /**
     * indicates the viewModel an object has been swiped, and the adapter a visual update will be needed
     */
    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        viewModel.onSwiped(adapter.currentList[viewHolder.adapterPosition])
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)

        with(viewHolder.itemView.background){
            if (this is ColorDrawable && this.color == Color.RED)
                viewHolder.itemView.setBackgroundColor(Color.WHITE)
        }
    }

}

