package com.example.shoppinglist.common

import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import com.example.shoppinglist.R
import java.util.ArrayList

abstract class BaseFragment : FragmentWithDialog() {


    /**
     * Populate a spinner with values existing in the Category table
     */
    protected fun generateSpinner(layoutView: View, categoryList: ArrayList<String> ){
        val dropDown = layoutView.findViewById<Spinner>(R.id.cat_dropdown)

        // Nothing to adapt if there is no entry in categories table
        if (categoryList.size > 1) {
            val adapter: ArrayAdapter<String> = ArrayAdapter<String>(this.requireContext(), android.R.layout.simple_spinner_item, categoryList)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            dropDown.adapter = adapter
            dropDown.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                    val dropText = parent.getItemAtPosition(position).toString()
                    if (dropText.isEmpty()) {
                        return
                    }
                    val catView = layoutView.findViewById<View>(R.id.category) as TextView
                    catView.text = dropText
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
    }

    protected fun getAlertStringIDs(isNew: Boolean) : List<Int>{
        return if (isNew)
            listOf(R.string.add_art_prmpt_title,
            R.string.add_art_prmpt_msg,
            R.string.add)
         else
            listOf( R.string.edit_art_prmpt_title,
             R.string.edit_art_prmpt_msg,
             R.string.confirm)
    }


}