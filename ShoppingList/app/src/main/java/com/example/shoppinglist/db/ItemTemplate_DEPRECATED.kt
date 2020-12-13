package com.example.shoppinglist.db

/**
 * TODO there is no data class inheritance in Kotlin
 *  but I encountered a problem with Room when data classes extended a super-non-data-class.
 *  This means I have to double code some common features for UsualItem and ShoppingItem...
 */



//abstract class ItemTemplate (
//    open var itemId: Long,
//    open var name: String,
//    open var category: String,
//    open var quantity: String
//)