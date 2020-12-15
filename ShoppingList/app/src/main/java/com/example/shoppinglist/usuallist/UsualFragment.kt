package com.example.shoppinglist.usuallist

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.EditText
import android.widget.Spinner
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
import com.example.shoppinglist.databinding.FragmentUsualListBinding
import com.example.shoppinglist.db.ItemDatabase
import com.example.shoppinglist.db.UsualItem
import java.util.ArrayList

class UsualFragment : BaseFragment(){

    private lateinit var usualViewModel: UsualViewModel
    private lateinit var adapter: UsualAdapter
    private var lastCategory = ""


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {

        val binding : FragmentUsualListBinding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_usual_list, container, false
        )

        // Set subtitle of screen
        (activity as AppCompatActivity?)!!.supportActionBar!!.subtitle = getString(R.string.usual_list_title)

        // Enable options menu creation
        setHasOptionsMenu(true)

        binding.lifecycleOwner = this

        val application = requireNotNull(this.activity).application
        val dataSource = ItemDatabase.getInstance(application).itemDatabaseDao

        val viewModelFactory = UsualViewModelFactory(dataSource, application)
        usualViewModel = ViewModelProvider(
                this, viewModelFactory).get(UsualViewModel::class.java)
        binding.usualViewModel = usualViewModel

        adapter = UsualAdapter(usualViewModel)
        binding.usualList.adapter = adapter

        setupObservers()

        //setup item touch helper
        @Suppress("UNCHECKED_CAST")
        val callback = BaseDragManageAdapter(usualViewModel, adapter as ListAdapter<Any, RecyclerView.ViewHolder>,
            0, ItemTouchHelper.RIGHT.or(ItemTouchHelper.LEFT))
        val helper = ItemTouchHelper(callback)
        helper.attachToRecyclerView(binding.usualList)

        // When phone back button is pressed, UI should behave as intended in the navigation
        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                usualViewModel.onBackClicked()
            }
        })

        return binding.root
    }

    private fun setupObservers() {

        // Mediator observer. Needed because otherwise the mediator won't update
        usualViewModel.mediator.observe(viewLifecycleOwner) {}

        // If the items in the database have changed, submit the new list for the adapter to display
        //        usualViewModel.items.observe(viewLifecycleOwner, Observer {
        //            it?.let {
        //                adapter.submitList(it)
        //            }
        //        })

        // Observe if item is to be added or edited
        usualViewModel.navigateToEditItem.observe(viewLifecycleOwner, Observer {
            if (it == true) {
                showEditItemDialog(usualViewModel.currentItem)
                usualViewModel.doneEditing()
            }
        })

        usualViewModel.lastDeleted.observe(viewLifecycleOwner, Observer {
            it?.let {
                generateUndoSnackbar(getString(R.string.item_deleted_info, it.name), usualViewModel)
            }
        })

        // Observe if ordered to go back to shopping list
        usualViewModel.navigateToShopping.observe(viewLifecycleOwner, Observer {
            if (it == true) {
                this.findNavController().navigate(
                        UsualFragmentDirections
                                .actionUsuallistToShoppinglist())
                // Change state not to navigate again when back
                usualViewModel.doneNavigatingToShopping()
            }
        })

        // Observe if user tried to propagate an update while the updated item already exists in the shopping list
        usualViewModel.showSnackCantPropagate.observe(viewLifecycleOwner, Observer {
            if (it == true) {
                generateSnackbar(getString(R.string.cant_propagate),
                        true)
                usualViewModel.doneShowingSnackCantPropagate()
            }
        })

        // Observe if item is added to the shopping list
        usualViewModel.showSnackAdded.observe(viewLifecycleOwner, Observer {
            if (it == true) {
                generateSnackbar(getString(R.string.art_put_in_slist),
                        false)
                usualViewModel.doneShowingSnackAdded()
            }
        })

        // Observe if item is removed from the shopping list
        usualViewModel.showSnackRemoved.observe(viewLifecycleOwner, Observer {
            if (it == true) {
                generateSnackbar(getString(R.string.art_del_from_slist),
                        false)
                usualViewModel.doneShowingSnackRemoved()
            }
        })
    }

    /**
     * Create options menu
     */
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.usual_art_menu, menu)
    }

    /**
     * Menu Options for the Usual list
     */
    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId){
            R.id.action_sort_by_cat -> {
                adapter.onSortUpdateUpcoming()
                usualViewModel.updateSorting(sortType, SORT_BY_CAT)
            }
            R.id.action_sort_by_item -> {
                adapter.onSortUpdateUpcoming()
                usualViewModel.updateSorting(sortType, SORT_BY_ITEM)
            }R.id.action_empty_list -> {
                showDelDialog()
                return true
            }
            R.id.action_add_all -> {
                usualViewModel.transferRemainingItems()
            }
        }
        return super.onOptionsItemSelected(menuItem)
    }

    /**
     * Dialog box asking the user to confirm deletion of all the usual items
     */
    private fun showDelDialog(){
        this.context?.let {
            dialog = AlertDialog.Builder(it)
                    .setTitle(R.string.del_all_title)
                    .setMessage(R.string.del_all_msg)
                    .setPositiveButton(R.string.confirm) { _, _ ->
                        usualViewModel.emptyList()
                    }
                    .setNegativeButton(getString(R.string.prmpt_cancel), null)
                    .create()
            dialog?.show()
        }
    }

    /**
     * Shows an alert dialog box to let the user create a new item or update an existing one
     */
    private fun showEditItemDialog(currentItem: UsualItem) {
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
                        usualViewModel.getAllCats()?.let { list ->
                            addAll(list.map { category -> category.categoryId })
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
                        when (usualViewModel.itemFormatState(newName, newCategory, newQuantity, newUnit)) {
                            VALID -> { // if valid, create, update, inform the user creation failed or ask to propagate
                                if (newUnit != "") newQuantity += " $newUnit"
                                val result = usualViewModel.createOrUpdate(isNew, currentItem,
                                    newName, newCategory, newQuantity)
                                result?.let {
                                    if (!result)
                                        generateSnackbar(getString(R.string.entry_already_exists), false)
                                }?: alertPropagate(currentItem,
                                        newName, newCategory, newQuantity) //if previous version was in the Shopping list, suggest to update matching item
                            }
                            EMPTY_STRING -> generateSnackbar(getString(R.string.pls_non_empty), false)//generateToast(it, getString(R.string.pls_non_empty), false)
                            INVALID_NUMBER -> generateSnackbar(getString(R.string.is_not_int), false)
                            ALREADY_EXISTS -> generateSnackbar(getString(R.string.entry_already_exists), false)
                        }
                    }
                    .setNegativeButton(getString(R.string.prmpt_cancel), null)
                    .setOnDismissListener {
                        //swipe can be re-enabled from here : it must just be prevented between Edit clicked and Dialog open
                        usualViewModel.enableSwipe()
                    }
                    .create()
            dialog?.show()
        }
    }

    /**
     * Alerts the user that the item to be updated also existed in the shopping list
     * and asks if the change to the usual item should be done for the shopping item too
     */
    private fun alertPropagate(currentItem: UsualItem,
                                name: String, category: String, quantity: String){

        dialog = AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.already_in_slist_title))
                .setMessage(getString(R.string.already_in_slist_msg))
                .setPositiveButton(getString(R.string.yes)) { _, _ ->
                    usualViewModel.updateItem(currentItem, name, category, quantity, true)
                }
                .setNegativeButton(getString(R.string.no)) { _, _ ->
                    usualViewModel.updateItem(currentItem, name, category, quantity, false)
                }
                .create()
        dialog?.show()
    }

}