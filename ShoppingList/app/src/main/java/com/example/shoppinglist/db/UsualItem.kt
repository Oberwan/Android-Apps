package com.example.shoppinglist.db

import androidx.room.*
import androidx.room.ForeignKey.CASCADE
import androidx.room.ForeignKey.RESTRICT
import com.example.shoppinglist.DEFAULT_CATEGORY_EN
import com.example.shoppinglist.DEFAULT_CATEGORY_FR
import java.util.*

/**
 * Item the user will want to regularly put in the shopping list
 */
@Entity(tableName = "usual_items",
    indices = [Index(value = ["category"])],
    foreignKeys = [ForeignKey(entity = Category::class,
        parentColumns = arrayOf("category_id"),
        childColumns = arrayOf("category"),
        onDelete = RESTRICT, onUpdate = CASCADE)])
data class UsualItem(
        @PrimaryKey(autoGenerate = true)
    var itemId: Long = 0L,

        @ColumnInfo(name = "name")
    var name: String = "",

        @ColumnInfo(name = "category")
    var category: String = if (Locale.getDefault().language == Locale("fr").language)
            DEFAULT_CATEGORY_FR
        else DEFAULT_CATEGORY_EN,

        @ColumnInfo(name = "quantity")
    var quantity: String = "1",

    //Boolean disguised as Int, values 0 or 1
        @ColumnInfo(name = "in_shopping_list")
    var inShoppingList: Int = 0
)//: ItemTemplate(itemId, name, category, quantity)