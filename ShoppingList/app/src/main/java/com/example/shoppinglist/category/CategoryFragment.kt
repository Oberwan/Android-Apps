package com.example.shoppinglist.category

import android.os.Bundle
import android.view.*
import android.widget.EditText
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.example.shoppinglist.*
import com.example.shoppinglist.common.FragmentWithDialog
import com.example.shoppinglist.databinding.FragmentCategoriesBinding
import com.example.shoppinglist.db.Category
import com.example.shoppinglist.db.ItemDatabase


class CategoryFragment : FragmentWithDialog() {

    private lateinit var categoryViewModel: CategoryViewModel


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val binding : FragmentCategoriesBinding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_categories, container, false
        )

        // Set subtitle of screen
        (activity as AppCompatActivity?)!!.supportActionBar!!.subtitle = getString(R.string.category)

        // Enable options menu creation
        setHasOptionsMenu(true)

        binding.lifecycleOwner = this

        val application = requireNotNull(this.activity).application
        val dataSource = ItemDatabase.getInstance(application).itemDatabaseDao

        val viewModelFactory = CategoryViewModelFactory(dataSource, application)
        categoryViewModel = ViewModelProvider(
                this, viewModelFactory).get(CategoryViewModel::class.java)

        binding.catViewModel = categoryViewModel

        val adapter = CategoryAdapter(categoryViewModel)
        binding.categoryList.adapter = adapter

        fun RecyclerView.disableItemAnimator() {
            (itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
        }
        binding.categoryList.disableItemAnimator()
        //This was supposed to prevent blink after swapping item. animations still play though
        //(binding.categoryList.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false

        setupObservers(adapter)

        //setup item touch helper
        val callback = CatDragManageAdapter(categoryViewModel, adapter,
            ItemTouchHelper.UP.or(ItemTouchHelper.DOWN), ItemTouchHelper.RIGHT.or(ItemTouchHelper.LEFT))
        val helper = ItemTouchHelper(callback)
        helper.attachToRecyclerView(binding.categoryList)

        // When phone back button is pressed, UI should behave as intended in the navigation
        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                categoryViewModel.onBackClicked()
            }
        })


        return binding.root
    }

    private fun setupObservers(adapter: CategoryAdapter) {

        categoryViewModel.mediator.observe(viewLifecycleOwner) {}

        categoryViewModel.categories.observe(viewLifecycleOwner, Observer {
            it?.let {
                adapter.submitList(it)
            }
        })

        // Observe if item is to be added or edited
        categoryViewModel.navigateToEditItem.observe(viewLifecycleOwner, Observer {
            if (it == true) {
                showEditItemDialog(categoryViewModel.currentCat)
                categoryViewModel.doneEditing()
            }
        })

        categoryViewModel.lastDeleted.observe(viewLifecycleOwner, Observer {
            it?.let {
                generateUndoSnackbar(getString(R.string.cat_deleted_info, it.categoryId), categoryViewModel)
            }
        })

        // Observe order to go back to the shopping list
        categoryViewModel.navigateToShopping.observe(viewLifecycleOwner, Observer {
            if (it == true) {
                this.findNavController().navigate(
                        CategoryFragmentDirections
                                .actionCategoryFragmentToShoppingListFragment())
                // Change state not to navigate again when back
                categoryViewModel.doneNavigatingToShopping()
            }
        })

        // Observe if user tried to delete a used category
        categoryViewModel.showSnackBarDelForItem.observe(viewLifecycleOwner, Observer {
            if (it == true) {
                    adapter.notifyDataSetChanged()
                    generateSnackbar(getString(R.string.cant_del_used_cat),
                        false)
                    categoryViewModel.doneShowingSnackbarDel()
            }
        })

        //Observe if user ordered to delete all categories but all are used
        categoryViewModel.showSnackBarDelAll.observe(viewLifecycleOwner, Observer {
            if (it == true) {
                generateSnackbar(getString(R.string.cant_del_all_used),
                        false)
                categoryViewModel.doneShowingSnackbarDelAll()
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.category_menu, menu)
    }


    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId){
            R.id.action_sort_alpha -> categoryViewModel.updateSorting(catSortType, SORT_ALPHA)
            R.id.action_custom_sort -> categoryViewModel.updateSorting(catSortType, SORT_CUSTOM)
            R.id.action_empty_list -> {
                this.context?.let {
                    //Pros of asking confirm first: no database request if "no" is chosen
                    //Cons: if no category is unused, dialog box is pointless
                    dialog = AlertDialog.Builder(it)
                            .setTitle(getString(R.string.prmpt_del_unused_title))
                            .setMessage(getString(R.string.prmpt_del_unused_msg))
                            .setPositiveButton(R.string.confirm) { _, _ ->
                                categoryViewModel.deleteUnusedCats()
                            }
                            .setNegativeButton(getString(R.string.prmpt_cancel), null)
                            .create()
                    dialog?.show()
                }
                return true
            }
        }
        return super.onOptionsItemSelected(menuItem)
    }

    private fun showEditItemDialog(currentCat: Category) {
        this.context?.let {
            //Reference the dialog view to use its components
            val layoutView: View = layoutInflater.inflate(R.layout.cat_details, null)

            //Populate layout fields with current (or default - if new item) shopping item values
            layoutView.findViewById<EditText>(R.id.category).setText(currentCat.categoryId)

            // Prepare AlertDialog String values. If name is empty, it is a new item
            val isNew = currentCat.categoryId.isEmpty()
            val titleId: Int
            val messageId: Int
            val posButtonId: Int

            if (isNew){
                titleId = R.string.new_cat_title
                messageId = R.string.new_cat_msg
                posButtonId = R.string.add
            } else {
                titleId = R.string.edit_cat_title
                messageId = R.string.edit_cat_message
                posButtonId = R.string.confirm
            }


            dialog = AlertDialog.Builder(it)
                    .setTitle(titleId)
                    .setMessage(messageId)
                    .setView(layoutView)
                    .setPositiveButton(posButtonId) { _, _ ->
                        //Trim String to prevent apparent duplicates
                        val newCategory = layoutView.findViewById<EditText>(R.id.category).text.toString().trim()
                        if (newCategory.isEmpty()) generateSnackbar(getString(R.string.pls_non_empty), false)
                        else {
                            if (!categoryViewModel.createOrUpdate(isNew, currentCat, newCategory))
                                generateSnackbar(getString(R.string.entry_already_exists), false)
                        }
                    }
                    .setNegativeButton(getString(R.string.prmpt_cancel), null)
                    .setOnDismissListener {
                        //swipe can be re-enabled from here : it must just be prevented between Edit clicked and Dialog open
                        categoryViewModel.enableSwipe()
                    }
                    .create()
            dialog?.show()
        }
    }




}