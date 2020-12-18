package com.example.shoppinglist

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.shoppinglist.common.BaseViewModel
import com.example.shoppinglist.db.ShoppingItem
import com.google.android.material.snackbar.Snackbar
import java.lang.IllegalStateException
import java.lang.StringBuilder
import java.util.*

const val ALREADY_EXISTS = 2
const val VALID = 1
const val EMPTY_STRING = 0
const val INVALID_NUMBER = -1

const val DEFAULT_CATEGORY_EN = "Misc."
const val DEFAULT_CATEGORY_FR = "Divers"
const val SMS_HEADER_EN = "Shopping List:"
const val SMS_HEADER_FR = "Liste des courses :"

//items and usual items are sorted by category or alphabetically
const val SORT_BY_CAT = 0
const val SORT_BY_ITEM = 1
var sortType = SORT_BY_CAT

//Categories are sorted alphabetically or in a custom manner
const val SORT_CUSTOM = 0
const val SORT_ALPHA = 1
var catSortType = SORT_CUSTOM


const val RED_COLOR = 0xFFCF0000.toInt()
const val TEAL_COLOR = 0xAA03DAC5.toInt()

//Replaced with Snackbar
fun generateToast(context: Context, message: CharSequence?, longDuration: Boolean) {
    val duration = if (longDuration) {
        Toast.LENGTH_LONG
    } else {
        Toast.LENGTH_SHORT
    }
    val toast = Toast.makeText(context, message, duration)
    toast.show()
}

/**
 * Extension function so that fragments can display Snackbar messages
 */
fun Fragment.generateSnackbar(message: String, longDuration: Boolean){
    try {
        Snackbar.make(
            this.requireActivity().findViewById(android.R.id.content),
            message,
            if (longDuration) Snackbar.LENGTH_LONG else Snackbar.LENGTH_SHORT
        ).setAction(getString(R.string.dismiss_snackbar)){}
            .show()
    } catch (e: IllegalStateException){}
}

/**
 * Extension function so that fragments can display Snackbar messages
 */
fun Fragment.generateUndoSnackbar(message: String, viewModel: BaseViewModel){
    try {
        Snackbar.make(
                this.requireActivity().findViewById(android.R.id.content),
                message,
                4000//Snackbar.LENGTH_LONG
        ).setAction(getString(R.string.undo)){ //if undo clicked, re-add the item in the list
                    viewModel.undoDelete()
        }.addCallback(object : Snackbar.Callback() {
            //however snackbar is removed (other than actions and snackbar replaced), undo is aborted
            override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                if (event != DISMISS_EVENT_ACTION && event != DISMISS_EVENT_CONSECUTIVE)
                    viewModel.abortUndo()
            }
        }).show()
    } catch (e: IllegalStateException){}
}




fun formatState(foundItem: Boolean, name:String, category: String, quantity:String): Int{
    if (foundItem) {
        return ALREADY_EXISTS
    }
    if (name.isEmpty() || category.isEmpty()) return EMPTY_STRING
    if (quantity.toDoubleOrNull() == null //quantity might be in european format :
            && quantity.replace(",", ".").toDoubleOrNull() == null)
        return INVALID_NUMBER
    return VALID
}

/**
 * Format sms text according to the list. It will be formatted similarly to user's current display
 */
fun shapeSMS(itemList: List<ShoppingItem>?) : String{
    val sms = StringBuilder()
    val smsHeader = if (Locale.getDefault().language == Locale("fr").language)
        SMS_HEADER_FR else SMS_HEADER_EN
    itemList?.let { wholeList ->
        sms.appendLine(smsHeader)
        if (sortType == SORT_BY_CAT) {
            wholeList.groupBy { it.category }.forEach { (cat, list) ->
                sms.appendLine("** $cat **")
                list.forEach { item ->
                    sms.appendLine("${item.name} x${item.quantity}")
                }
            }
        } else { // If list is sorted by item, categories should not be displayed
            for (item in wholeList) sms.appendLine("${item.name} x${item.quantity}")
        }
    }
    return sms.toString()
}


/**
 * MediatorLiveData which keeps track of its last assigned source. Used by Item/UsualViewModel
 */
class SorterMediatorLiveData<Unit> : MediatorLiveData<Unit>(){
    var currentSource : Any? = null
    override fun <S : Any?> addSource(source: LiveData<S>, onChanged: Observer<in S>) {
        currentSource = source
        super.addSource(source, onChanged)
    }
}

