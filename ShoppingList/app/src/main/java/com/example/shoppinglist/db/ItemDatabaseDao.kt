package com.example.shoppinglist.db

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface ItemDatabaseDao {

    /**
     * Shopping items
     */


    @Insert(entity = ShoppingItem::class)
    suspend fun insertItem(item: ShoppingItem)

    @Query("UPDATE articles SET in_cart = :isChecked WHERE itemId = :itemId")
    suspend fun updateInCart(itemId: Long, isChecked: Boolean)

    // Updates the ShoppingItem with matching itemId with new property values
    @Update
    suspend fun updateItem(item: ShoppingItem)

    // Note: the following SELECTs are extra safe (LIMIT 1), as only one item should be returned

    @Query("SELECT * FROM articles WHERE itemId = :key LIMIT 1")
    suspend fun getFromItems(key: Long): ShoppingItem?

    @Query("SELECT * FROM articles WHERE category = :category LIMIT 1")
    suspend fun getFromItems(category: String): ShoppingItem?

    @Query("SELECT * FROM articles WHERE name = :name AND category = :category LIMIT 1")
    suspend fun getFromItems(name: String, category: String): ShoppingItem?

    @Query("SELECT * FROM articles WHERE name = :name AND category = :cat AND quantity = :qty LIMIT 1")
    suspend fun getFromItems(name: String, cat: String, qty: String): ShoppingItem?

    @Query("SELECT in_cart FROM articles WHERE itemId = :itemId")
    suspend fun isInCart(itemId: Long): Boolean

    //@Query("SELECT * from articles ORDER BY category, name ASC")
    @Query("SELECT * FROM articles AS a JOIN categories AS c ON a.category = c.category_id ORDER BY c.rank, name ASC")
    fun getAllItemsByCatSortRank(): LiveData<List<ShoppingItem>>

    @Query("SELECT * FROM articles ORDER BY category, name ASC")
    fun getAllItemsByCatSortName(): LiveData<List<ShoppingItem>>

    @Query("SELECT * FROM articles ORDER BY name ASC")
    fun getAllItemsByItem(): LiveData<List<ShoppingItem>>

    /**
     * non live data versions for getting the list to send as SMS
     */
    @Query("SELECT * FROM articles AS a JOIN categories AS c ON a.category = c.category_id WHERE a.in_cart = 0 ORDER BY c.rank, name ASC")
    suspend fun getAllItemsByCatSortRankSus(): List<ShoppingItem>?

    @Query("SELECT * FROM articles WHERE in_cart = 0 ORDER BY category, name ASC")
    suspend fun getAllItemsByCatSortNameSus(): List<ShoppingItem>?

//    @Query("SELECT * from articles ORDER BY name ASC")
//    suspend fun getAllItemsByItemSus(): List<ShoppingItem>?

    @Delete(entity = ShoppingItem::class)
    suspend fun deleteItem(item: ShoppingItem)

    @Query("DELETE FROM articles WHERE name = :name AND category = :category")
    suspend fun deleteItem(name: String, category: String)

    @Query("DELETE FROM articles WHERE in_cart = \"1\"")
    suspend fun deleteInCart()

    @Query("DELETE FROM articles")
    suspend fun deleteAllItems()


    /**
     * Categories
     */


    /**
     * Should only be used to re-insert a category that was deleted, because DB should
     * always decide of the category ID and rank
     */
    @Insert(entity = Category::class)
    suspend fun insertCategory(category: Category)

    /**
     * Preferred way to insert a category. Rank should be superior to all other ranks, or 1 if table is empty
     */
    @Query("INSERT INTO categories (category_id, rank) VALUES (:category, coalesce((SELECT MAX(rank) FROM categories),0)+1)")
    suspend fun insertCategoryByName(category: String)

    @Update(entity = Category::class)
    fun updateCategory(cat: Category)

//    @Update(entity = Category::class)
//    suspend fun updateCategories(catList: List<Category>)

    @Query("UPDATE categories SET category_id = :newCat WHERE category_id = :oldCat ")
    suspend fun updateCatName(oldCat: String, newCat:String)

    @Query("SELECT * FROM categories WHERE category_id = :category LIMIT 1")
    suspend fun getFromCategories(category: String): Category?

    @Query("SELECT * FROM categories WHERE rank = :rank LIMIT 1")
    suspend fun getCatFromRank(rank: Int): Category?

    @Query("SELECT * FROM categories ORDER BY category_id")
    suspend fun getAllCategories(): List<Category>?

    // Live versions for observer in CategoryFragment
    @Query("SELECT * FROM categories ORDER BY rank ASC") // ORDER BY category_id ASC")
    fun getAllCatsLiveByRank(): LiveData<List<Category>>

    @Query("SELECT * FROM categories ORDER BY category_id")
    fun getAllCatsLiveAlpha(): LiveData<List<Category>>

//    @Query("DELETE FROM categories")
//    suspend fun deleteAllCats()

    /**
     * Categories might only be deleted if they are not used in any other table
     */
    @Query("DELETE FROM categories WHERE " +
            "categories.category_id NOT IN (SELECT category FROM articles) " +
            "AND categories.category_id NOT IN (SELECT category FROM usual_items)")
    suspend fun deleteUnusedCats(): Int

    @Delete(entity = Category::class)
    suspend fun deleteCategory(cat: Category)



    /**
     * Usual Items
     */


    //@Query("select u.*, case a.count when null then 0 else a.count end as in_shopping_list from usual_items as u left join (select category, name, count(itemId) from articles group by category, name) as a on u.name=a.name and u.category=a.category")

    /**
     * Update boolean in_shopping_list, according to whether the couple (name, category)
     * is in both tables
     */
    @Query("UPDATE usual_items SET in_shopping_list = (SELECT count (*) FROM articles as a WHERE usual_items.category=a.category AND usual_items.name=a.name)>0")
    suspend fun updateTableUsualItems()

    @Insert(entity = UsualItem::class)
    suspend fun insertUsualItem(item: UsualItem)

    @Update(entity = UsualItem::class)
    suspend fun updateUsualItem(item: UsualItem)

    @Query("SELECT * FROM usual_items WHERE name = :name AND category = :cat LIMIT 1")
    suspend fun getFromUsualItems(name: String, cat: String): UsualItem?

    @Query("SELECT * FROM usual_items WHERE name = :name AND category = :cat AND quantity = :qty LIMIT 1")
    suspend fun getFromUsualItems(name: String, cat: String, qty: String): UsualItem?

    @Query("SELECT * FROM usual_items ORDER BY category, name ASC")
    fun getAllUsualItemsByCatSortName(): LiveData<List<UsualItem>>

    @Query("SELECT * FROM usual_items AS u JOIN categories AS c ON u.category = c.category_id ORDER BY c.rank, name ASC")
    fun getAllUsualItemsByCatSortRank(): LiveData<List<UsualItem>>


    @Query("SELECT * FROM usual_items ORDER BY name ASC")
    fun getAllUsualItemsByItem(): LiveData<List<UsualItem>>

    @Query("UPDATE usual_items SET in_shopping_list = :isChecked WHERE itemId = :itemId")
    suspend fun updateInShoppingList(itemId: Long, isChecked: Boolean)

    @Delete(entity = UsualItem::class)
    suspend fun deleteUsualItem(item: UsualItem)

    @Query("DELETE FROM usual_items")
    suspend fun deleteAllUsualItems()


    /**
     * Transactions
     */



//    @Query("UPDATE categories SET rank = :newRank WHERE category_id = :category")
//    fun updateCatRank(category: String, newRank: Int)


   /* @Query("SELECT * FROM categories where pk = :id")
    fun getCatFromId(id: Int): Category?*/

 /*   @Transaction
    suspend fun swapCategoriesFromId(fromId: Int, toId: Int){
        val catFrom = getCatFromId(fromId)
        val catTarget = getCatFromId(toId)
        catFrom?.let {
            catTarget?.let {
                val fromRank = catFrom.rank
                val toRank = catTarget.rank
                updateCategory(catFrom.apply { rank = -1 }) //Temporary "non valid" value
                if (fromRank < toRank) //Decrease values in between
                    updateCategoriesAsc(fromRank, toRank)
                else //Increase values in between
                    updateCategoriesDesc(fromRank, toRank)
                updateCategory(catFrom.apply { rank = toRank }) // set target value
            }
        }
    }*/

    //Part of the next transaction :

    /**
     * from 2 to 5 : 3, 4, 5 -> 2, 3, 4 ; from 2 to 3 : 3 -> 2
     */
    @Query("UPDATE categories SET rank = rank-1  WHERE rank > :fromRank AND rank <= :toRank")
    fun updateCategoriesAsc(fromRank: Int, toRank: Int)

    //from 5 to 2 : 2, 3, 4 -> 3, 4, 5
    @Query("UPDATE categories SET rank = rank+1  WHERE rank >= :toRank AND rank < :fromRank")
    fun updateCategoriesDesc(fromRank: Int, toRank: Int)

    @Transaction
    suspend fun updateCatRanks(catFrom: Category, catTarget: Category){
        val fromRank = catFrom.rank
        val toRank = catTarget.rank
        updateCategory(catFrom.apply { rank = -1 }) //Temporary "non valid" value
        if (fromRank < toRank) //Decrease values in between
            updateCategoriesAsc(fromRank, toRank)
        else //Increase values in between
            updateCategoriesDesc(fromRank, toRank)
        updateCategory(catFrom.apply { rank = toRank }) // set target value
    }



    /**
     * if
     * from < to : store to.rk as TO, from.rk = -1, from < rk <= to --, from.rk= TO
     * from > to : store to.rk as TO, from.rk = -1, to <= rk < from ++, from.rk= TO
     */
    //if from < to, for  from<rank<=to, getCatFromRank?.let
   /* @Transaction
    suspend fun swapCatRanks(cat1: Category, cat2: Category, betweenList: List<Category>){
        //updateCatRank(cat1.categoryId, -1)
        val fromRank = cat1.rank
        val toRank = cat2.rank

        updateCategory(cat1.apply { rank = -1 }) //temporary "non valid" rank
        Log.i(
            "DB",
            "TEMP MOD : catFrom: $cat1, catTo: $cat2")
        if (fromRank < toRank){
            if (betweenList.isNotEmpty()){
                for (cat in betweenList){
                    cat.rank--
                    updateCategory(cat)
                    Log.i(
                        "DB-TO HIGHER RANK",
                        "FOUND & decremented : $cat")
                }
            }
            cat2.rank--
            updateCategory(cat2)
            Log.i(
                "DB-TO HIGHER RANK",
                "Last : cat2, became : $cat2")
        } else {
            if (betweenList.isNotEmpty()){
                for (cat in betweenList){
                    cat.rank++
                    updateCategory(cat)
                    Log.i(
                        "DB-TO LOWER RANK",
                        "FOUND & incremented : $cat")
                }
            }
            cat2.rank++
            updateCategory(cat2)
            Log.i(
                "DB-TO LOWER RANK",
                "Last : cat2, became : $cat2")
        }


        updateCategory(cat1.apply { rank = toRank })
        Log.i(
            "DB-FINALLY",
            "cat1, became : $cat1")
    }*/


    //Part of the next transaction :
    //As it happens, transferred usual items are not in cart, so in_cart = in_shopping_list = 0
    @Query("INSERT INTO articles (name, category, quantity, in_cart) SELECT name, category, quantity, 0 FROM usual_items WHERE in_shopping_list = 0 ")
    fun transferRemainingUsualItems()

    @Query("UPDATE usual_items SET in_shopping_list = 1 WHERE in_shopping_list = 0")
    fun updateRemainingUsualItems()

    /**
     * Put into the shopping list the usual items that are not already in the shopping list
     */
    @Transaction
    suspend fun transferAndUpdateRemainingUsualItems(){
        transferRemainingUsualItems()
        updateRemainingUsualItems()
    }


//    @Query("SELECT * from articles WHERE name = :name AND category = :category")
//    fun getFromItemsUnsuspended(name: String, category: String): ShoppingItem?

    //Part of the next transaction :

    @Query("UPDATE articles SET name= :newName, category=:newCat, quantity=:newQty WHERE name= :oldName AND category=:oldCat")
    fun updateItemUnsuspended(oldName: String, oldCat: String, newName: String, newCat: String, newQty: String)

    @Update(entity = UsualItem::class)
    fun updateUsualItemUnsuspended(item: UsualItem)

    /**
     * Transaction is called when usual item must be updated AND its old version was also in articles
     * AND the user wishes to update both of them as a result
     */
    @Transaction
    suspend fun updateUsualItemAndItem(oldItem: UsualItem, newName: String, newCategory: String, newQuantity: String){
        updateItemUnsuspended(oldItem.name, oldItem.category , newName, newCategory, newQuantity)
        updateUsualItemUnsuspended(UsualItem(oldItem.itemId, newName, newCategory, newQuantity, 1))
    }



}