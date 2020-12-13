package com.example.shoppinglist.common

import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment

/**
 * Superclass of all fragments in the app. It is just a Fragment with an AlertDialog object
 * and an onDestroy which makes sure the dialog is cleanly dismissed
 */
abstract class FragmentWithDialog : Fragment(){

    protected var dialog: AlertDialog? = null

    /**
     * This override is especially useful for cleanly dismissing any dialog trying to show
     * and above all to let the swipe action be re-enabled on dismiss. The reason it is
     * disabled beforehand is that a user could press the Edit button, then swipe just before
     * the alert dialog is shown, which is undesirable
     */
    override fun onDestroy() {
        super.onDestroy()
        dialog?.let {
            it.dismiss()
            dialog = null
        }
    }

}