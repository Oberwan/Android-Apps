package com.example.shoppinglist.common

import android.text.Html
import androidx.appcompat.app.AlertDialog
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import com.example.shoppinglist.R
import com.example.shoppinglist.generateSnackbar

/**
 * Superclass of all fragments in the app. It is just a Fragment with an AlertDialog object
 * and an onDestroy which makes sure the dialog is cleanly dismissed
 */
abstract class FragmentWithDialog : Fragment(){

    protected var dialog: AlertDialog? = null


    fun displayHelp(message: String){
        this.context?.let {
            dialog = AlertDialog.Builder(it)
                .setMessage(HtmlCompat.fromHtml(
                    message,
                    HtmlCompat.FROM_HTML_SEPARATOR_LINE_BREAK_PARAGRAPH))
                .setNegativeButton(getString(R.string.ok), null)
                .create()
            dialog?.show()
        }
    }

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