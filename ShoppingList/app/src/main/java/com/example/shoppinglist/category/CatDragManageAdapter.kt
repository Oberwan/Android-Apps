package com.example.shoppinglist.category


import android.content.Context
import android.graphics.Color
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.example.shoppinglist.RED_COLOR
import com.example.shoppinglist.TEAL_COLOR
import com.example.shoppinglist.db.Category
import java.util.*

const val INVALID_POSITION = -2
/**
 * Category items are draggable, so that the user can put them in the same order as their usual store
 */
class CatDragManageAdapter(var categoryViewModel: CategoryViewModel, var adapter: CategoryAdapter, dragDirs: Int, swipeDirs: Int)
    : ItemTouchHelper.SimpleCallback(dragDirs, swipeDirs){


    /**
     * Object meant to implement a smooth scroll when it has to be done programmatically.
     * Inner class is created instead of "object" on the fly because this is used a couple of times
     */
    inner class CustomLinearSmoothScroller(context : Context)
        : LinearSmoothScroller(context) {
        //scrolling is a bit quick: slow it down a notch
        override fun calculateTimeForScrolling(dx: Int): Int {
            return super.calculateTimeForScrolling(dx) * 15
        }
        //get to the top of the list item
        override fun getVerticalSnapPreference(): Int {
            return LinearSmoothScroller.SNAP_TO_START;
        }
    }

    /**
     * This list is used to store the initial list at beginning of a drag.
     * Its sole goal is to circumvent the fact that we can't count on the last onMove
     * to give us the final target : in A - B - C, if user moves A through B and C AND BACK to B,
     * we want B - A - C, so swap with B. But here its last target is C,
     * yet we don't want to tell the DB to move A past C
     */
    private val initialList = mutableListOf<Category>()

    private var initialPosition = INVALID_POSITION
    private var targetPosition = INVALID_POSITION

    /**
     * Sometimes, onMove method is called more than once for the "same" move.
     * So, in order not to mess up the targetList, we need to make sure current move is not a duplicate
     */
    private var lastFrom = INVALID_POSITION
    private var lastTo = INVALID_POSITION



    /**
     * Inform the adapter that an item is dragged
     */
    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {

        val fromPosition = viewHolder.adapterPosition
        val toPosition = target.adapterPosition


        //Sometimes method is called more than once for same move (...why?)
        if (fromPosition == lastFrom && toPosition == lastTo)
            return false

        if (initialPosition == INVALID_POSITION){
            initialPosition = fromPosition
            initialList.addAll(adapter.currentList)
        }
        targetPosition = toPosition


        val items = adapter.currentList.toMutableList()

        if (fromPosition < toPosition){
            for (i in fromPosition until toPosition){
                //Swap UI order
                Collections.swap(items, i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition+1){
                //Swap UI order
                Collections.swap(items, i, i - 1)
            }
        }

        //submitList with ListAdapter seems COMPULSORY to avoid visual bugs
        adapter.submitList(items)

        lastFrom = fromPosition
        lastTo = toPosition
        return true
    }

    override fun onMoved(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, fromPos: Int, target: RecyclerView.ViewHolder, toPos: Int, x: Int, y: Int) {
        // When item at start of list is dragged, an ugly scroll happens (Google, pls!!). Prevent that
        if (fromPos < 2){
            recyclerView.layoutManager?.startSmoothScroll(
                    CustomLinearSmoothScroller(recyclerView.context).apply { targetPosition = 0 })
        }
    }


    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        categoryViewModel.onSwiped(adapter.currentList[viewHolder.adapterPosition])
    }


    override fun isItemViewSwipeEnabled(): Boolean {
        return categoryViewModel.isSwipeEnabled()
    }

    /**
     * Prevent categories drag and drop if sorting is set to alphabetically
     */
    override fun isLongPressDragEnabled(): Boolean {
        return categoryViewModel.isDragPossible()
    }


    /**
     * Dragged item is colored
     */
    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        viewHolder?.let {
            if (actionState == ItemTouchHelper.ACTION_STATE_DRAG)
                it.itemView.setBackgroundColor(TEAL_COLOR)
            if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE)
                it.itemView.setBackgroundColor(RED_COLOR)
        }
        super.onSelectedChanged(viewHolder, actionState)
    }


    /**
     * If the item was dragged, this method handles the drop and reverts background color to white
     * If the item was swiped, just revert background color to white (deletion of Category might be illegal)
     */
    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)

        // If item is dropped at start of the list, manually make UI focus smoothly on it
        if (viewHolder.adapterPosition == 0) {
            recyclerView.layoutManager?.startSmoothScroll(
                    CustomLinearSmoothScroller(recyclerView.context).apply { targetPosition = 0 })
        }

        viewHolder.itemView.setBackgroundColor(Color.WHITE)

//        try {
//            Log.i(
//                    "----------------DMA : ",
//                    "initial position : $initialPosition, ${initialList[initialPosition]} \n" +
//                            "last from, last to : $lastFrom, $lastTo \n" +
//                            "target position : $targetPosition, ${initialList[targetPosition]} \n" +
//                            " \nnames/new Ranks ${adapter.currentList.map { listOf(it.categoryId, it.rank) }}")
//        } catch (e : ArrayIndexOutOfBoundsException){}

        //Check targetPosition because LongPressed without onMove will still trigger clearView
        if (targetPosition != INVALID_POSITION){
            val catFrom = initialList[initialPosition]
            val catTo = initialList[targetPosition]
            if (initialPosition != targetPosition)
                categoryViewModel.updateCatRank(catFrom, catTo)
            targetPosition = INVALID_POSITION
        }

        //Reinitialize positions and list
        initialPosition = INVALID_POSITION
        lastFrom = INVALID_POSITION
        lastTo = INVALID_POSITION
        initialList.clear()
    }
}