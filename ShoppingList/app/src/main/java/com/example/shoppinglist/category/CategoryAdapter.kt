package com.example.shoppinglist.category

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.shoppinglist.databinding.CategoryItemBinding
import com.example.shoppinglist.db.Category
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.runBlocking

class CategoryAdapter(private val viewModel: CategoryViewModel)
    : ListAdapter<Category, CategoryAdapter.ViewHolder>(CategoryDiffCallback()){


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent)
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, viewModel)
    }

    class ViewHolder private constructor(val binding: CategoryItemBinding):
            RecyclerView.ViewHolder(binding.root){

        fun bind(category: Category, viewModel: CategoryViewModel/*clickListener: ItemListener*/){
            binding.cat = category
            binding.catViewModel = viewModel
            binding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup): ViewHolder{
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = CategoryItemBinding.inflate(layoutInflater, parent, false)
                return ViewHolder(binding)
            }
        }

    }




}


class CategoryDiffCallback : DiffUtil.ItemCallback<Category>() {

    override fun areItemsTheSame(oldItem: Category, newItem: Category): Boolean {
        return oldItem.pk == newItem.pk
    }

    override fun areContentsTheSame(oldItem: Category, newItem: Category): Boolean {
        return oldItem == newItem
    }
}