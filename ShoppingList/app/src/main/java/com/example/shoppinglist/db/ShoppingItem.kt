package com.example.shoppinglist.db

import androidx.room.*
import androidx.room.ForeignKey.CASCADE
import androidx.room.ForeignKey.RESTRICT
import com.example.shoppinglist.DEFAULT_CATEGORY

/**
 * Item in the Shopping list
 */
@Entity(tableName = "articles",
    indices = [Index(value = ["category"])],
    foreignKeys = [ForeignKey(entity = Category::class,
        parentColumns = arrayOf("category_id"),
        childColumns = arrayOf("category"),
        onDelete = RESTRICT, onUpdate = CASCADE)])
data class ShoppingItem(
    @PrimaryKey(autoGenerate = true)
    var itemId: Long = 0L,

    @ColumnInfo(name = "name")
    var name: String = "",

    @ColumnInfo(name = "category")
    var category: String = DEFAULT_CATEGORY,

    @ColumnInfo(name = "quantity")
    var quantity: String = "1",

    //Boolean disguised as Int, values 0 or 1
    @ColumnInfo(name = "in_cart")
    var inCart: Int = 0
)
