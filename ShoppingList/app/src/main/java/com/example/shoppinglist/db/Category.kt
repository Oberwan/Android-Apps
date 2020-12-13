package com.example.shoppinglist.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "categories", indices = [Index(value = ["category_id"], unique = true)])
data class Category (
    @PrimaryKey(autoGenerate = true)
    var pk: Long = 1L,

    @ColumnInfo(name = "category_id")
    var categoryId: String = "",

    @ColumnInfo(name = "rank")
    var rank: Int = 1
){
    constructor(categoryId: String) : this(1, categoryId, 1)
}