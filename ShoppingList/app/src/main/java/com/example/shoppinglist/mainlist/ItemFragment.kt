package com.example.shoppinglist.mainlist

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.shoppinglist.*
import com.example.shoppinglist.common.BaseDragManageAdapter
import com.example.shoppinglist.common.BaseFragment
import com.example.shoppinglist.databinding.FragmentShoppingListBinding
import com.example.shoppinglist.db.ItemDatabase
import com.example.shoppinglist.db.ShoppingItem
import java.util.*

class ItemFragment : BaseFragment() {

    private lateinit var itemViewModel: ItemViewModel
    private lateinit var adapter: ItemAdapter
    private var lastCategory = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {

        val binding : FragmentShoppingListBinding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_shopping_list, container, false
        )
        binding.lifecycleOwner = this

        // Set subtitle of screen
        (activity as AppCompatActivity?)!!.supportActionBar!!.subtitle = ""

        // Enable options menu creation
        setHasOptionsMenu(true)

        val application = requireNotNull(this.activity).application
        val dataSource = ItemDatabase.getInstance(application).itemDatabaseDao

        val viewModelFactory = ItemViewModelFactory(dataSource, application)
        itemViewModel = ViewModelProvider(
                this, viewModelFactory).get(ItemViewModel::class.java)

        binding.itemViewModel = itemViewModel

        adapter = ItemAdapter(itemViewModel)
        binding.shoppingList.adapter = adapter

        setupObservers()

        //setup item touch helper
        @Suppress("UNCHECKED_CAST")
        val callback = BaseDragManageAdapter(itemViewModel, adapter as ListAdapter<Any, RecyclerView.ViewHolder>,
            0, ItemTouchHelper.RIGHT.or(ItemTouchHelper.LEFT))
        val helper = ItemTouchHelper(callback)
        helper.attachToRecyclerView(binding.shoppingList)

        // When back is pressed in this fragment, application should be left
        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                requireActivity().finish()
            }
        })


        return binding.root
    }


    private fun setupObservers() {
        itemViewModel.mediator.observe(viewLifecycleOwner) {}
        // If the items in the database have changed, submit the new list for the adapter to display
        //        itemViewModel.items.observe(viewLifecycleOwner, Observer {
        //            it?.let {
        //                adapter.submitList(it)
        //            }
        //        })

        // Observe if item is to be added or edited
        itemViewModel.navigateToEditItem.observe(viewLifecycleOwner, Observer {
            if (it == true) {
                showEditItemDialog(itemViewModel.currentItem)
                itemViewModel.doneEditing()
            }
        })

        itemViewModel.lastDeleted.observe(viewLifecycleOwner, Observer {
            it?.let {
                generateUndoSnackbar("Item ${it.name} has been deleted", itemViewModel)
            }
        })

        itemViewModel.navigateToCategories.observe(viewLifecycleOwner, Observer {
            if (it == true) {
                this.findNavController().navigate(
                        ItemFragmentDirections
                                .actionShoppinglistToCategories())
                // Change state not to navigate again when back
                itemViewModel.doneNavigatingToCat()
            }
        })
        itemViewModel.navigateToUsual.observe(viewLifecycleOwner, Observer {
            if (it == true) {
                this.findNavController().navigate(
                        ItemFragmentDirections
                                .actionShoppinglistToUsuallist())
                // Change state not to navigate again when back
                itemViewModel.doneNavigatingToUsual()
            }
        })

        itemViewModel.smsToSend.observe(viewLifecycleOwner, Observer {
            it?.let {
                sendSMS(it)
                itemViewModel.doneSendingSMS()
            }
        })
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.shopping_menu, menu)
    }



    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId){
            R.id.action_sort_by_cat -> {
                adapter.onSortUpdateUpcoming()
                itemViewModel.updateSorting(sortType, SORT_BY_CAT)
            }
            R.id.action_sort_by_item -> {
                adapter.onSortUpdateUpcoming()
                itemViewModel.updateSorting(sortType, SORT_BY_ITEM)
            }
            R.id.action_usual_items -> {
                itemViewModel.onUsualOptionMenuClicked()
                return true
            }
            R.id.action_manage_cat -> {
                itemViewModel.onCatOptionMenuClicked()
                return true
            }
            R.id.action_sens_sms ->{
                showSendDialog()
            }
            R.id.action_empty_list -> {
                showDelDialog(true, R.string.del_all_title, R.string.del_all_msg)
                return true
            }
            R.id.action_clear_checked -> {
                showDelDialog(false, R.string.del_chk_title, R.string.del_chk_msg)
                return true
            }
        }
        return super.onOptionsItemSelected(menuItem)
    }

    /**
     * Dialog to let the user confirm the list is to be sent via SMS
     */
    private fun showSendDialog() {
        this.context?.let {
            dialog = AlertDialog.Builder(it)
                    .setTitle(getString(R.string.sens_sms_title))
                    .setMessage(getString(R.string.sens_sms_msg))
                    .setPositiveButton(R.string.confirm) { _, _ ->
                        if (!itemViewModel.sendViaSMS())
                            generateSnackbar(getString(R.string.sms_no_data_to_send), false)
                    }
                    .setNegativeButton(getString(R.string.prmpt_cancel), null)
                    .create()
            dialog?.show()
        }
    }

    /**
     * The dialog box for deleting the whole list or only checked items is pretty much the same
     */
    private fun showDelDialog(delAll: Boolean, titleId: Int, messageId: Int){
        this.context?.let {
            dialog = AlertDialog.Builder(it)
                    .setTitle(titleId)
                    .setMessage(messageId)
                    .setPositiveButton(R.string.confirm) { _, _ ->
                        if (delAll) itemViewModel.emptyList()
                        else itemViewModel.clearChecked()
                    }
                    .setNegativeButton(getString(R.string.prmpt_cancel), null)
                    .create()
            dialog?.show()
        }
    }

    private fun showEditItemDialog(currentItem: ShoppingItem) {
        this.context?.let {
            //Reference the dialog view to use its components
            val layoutView: View = layoutInflater.inflate(R.layout.shop_item_details, null)

            //if current quantity contains a space, it has a unit, and can be split into 2 values
            val splitQty = currentItem.quantity.split(" ")
            val currentQuantity = splitQty[0]
            val unit = if (splitQty.size > 1) splitQty[1] else ""

            //If name is empty, it is a new item
            val isNew = currentItem.name.isEmpty()

            // Populate layout fields with current (or default - if new item) shopping item values
            layoutView.findViewById<EditText>(R.id.art_choice).setText(currentItem.name)
            //If item is to be updated, it already has a category. Otherwise, pick the last
            //category that was picked, out of convenience
            layoutView.findViewById<EditText>(R.id.category).setText(
                    if (isNew && lastCategory.isNotEmpty()) lastCategory
                    else currentItem.category
            )
            layoutView.findViewById<EditText>(R.id.numberPicker).setText(currentQuantity)
            layoutView.findViewById<Spinner>(R.id.qty_dropdown).setSelection(
                    resources.getStringArray(R.array.unit_array).indexOf(unit)
            )

            // Populate dropdown list
            // Category list contains blank item, so that no item is selected in dropdown in the first place
            // followed by entries from categories table if they exist
            generateSpinner(layoutView,
                    ArrayList<String>().apply {
                        add("")
                        itemViewModel.getAllCats()?.let { list ->
                            addAll(list.map { category ->  category.categoryId })
                        }
                    })

            // Prepare AlertDialog String values.
            val (titleId, messageId, posButtonId) = getAlertStringIDs(isNew)


            dialog = AlertDialog.Builder(it)
                    .setTitle(titleId)
                    .setMessage(messageId)
                    .setView(layoutView)
                    .setPositiveButton(posButtonId) { _, _ ->
                        //Values are trimmed to prevent unwanted spaces
                        val newName = layoutView.findViewById<EditText>(R.id.art_choice).text.toString().trim()
                        val newCategory = layoutView.findViewById<EditText>(R.id.category).text.toString().trim()
                        var newQuantity = layoutView.findViewById<EditText>(R.id.numberPicker).text.toString()
                        val newUnit = layoutView.findViewById<Spinner>(R.id.qty_dropdown).selectedItem.toString()
                        //Update the last category propriety out of convenience for next time
                        lastCategory = newCategory
                        when (itemViewModel.itemFormatState(newName, newCategory, newQuantity, newUnit)) {
                            VALID -> { // valid format: entry is created or updated
                                if (newUnit != "") newQuantity += " $newUnit"
//                                if (sortType == SORT_BY_CAT) adapter.onUpdateUpcoming()
                                if (!itemViewModel.createOrUpdate(isNew, currentItem,
                                        newName, newCategory, newQuantity)){
//                                    if (sortType == SORT_BY_CAT) adapter.cancelOnUpdateUpcoming() //entry won't be created, so no need to notify adapter
                                    generateSnackbar(getString(R.string.entry_already_exists), false)
                                }
                            }
                            EMPTY_STRING -> generateSnackbar(getString(R.string.pls_non_empty), false)
                            INVALID_NUMBER -> generateSnackbar(getString(R.string.is_not_int), false)
                            ALREADY_EXISTS -> generateSnackbar(getString(R.string.entry_already_exists), false)
                        }
                    }
                    .setNegativeButton(getString(R.string.prmpt_cancel), null)
                    .setOnDismissListener {
                        //swipe can be re-enabled from here : it must just be prevented between Edit clicked and Dialog open
                        itemViewModel.enableSwipe()
                    }
                    .create()
            dialog?.show()


        }
    }

    private fun sendSMS(sms: String){
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("smsto:")
            putExtra("sms_body", sms);
        }
        startActivity(intent)
    }



}