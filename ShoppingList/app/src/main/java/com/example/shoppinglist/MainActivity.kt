package com.example.shoppinglist

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * @author OberwanKenobi
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Get the preferred sorting types, for shopping/usual items as well as categories
        val pref = getSharedPreferences("Share", MODE_PRIVATE)
        sortType = pref.getInt("sortBy", SORT_BY_CAT)
        catSortType = pref.getInt("catSortBy", SORT_CUSTOM)
    }

}