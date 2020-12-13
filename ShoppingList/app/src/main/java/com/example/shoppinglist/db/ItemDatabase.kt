package com.example.shoppinglist.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.shoppinglist.R
import java.util.*


@Database(entities = [Category::class, ShoppingItem::class, UsualItem::class],
    version = 1,
    exportSchema = false)
abstract class ItemDatabase : RoomDatabase() {

    abstract val itemDatabaseDao: ItemDatabaseDao

    companion object {
        @Volatile
        private var INSTANCE: ItemDatabase? = null

        fun getInstance(context: Context): ItemDatabase {
            synchronized(this) {
                var instance = INSTANCE
                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        ItemDatabase::class.java,
                        "shopping_database"
                    ).addCallback(object :
                        RoomDatabase.Callback() { //First creation : create categories and an example entry
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
//                            if (Locale.getDefault().language == Locale("fr").language)
                            val catPrePopValues =
                                context.resources.getStringArray(R.array.pre_pop_cat)
                                    .apply { sort() }
                            val catList = catPrePopValues.mapIndexed() { index, catName ->
                                Category(1, catName, index+1) }
                            db.execSQL("INSERT INTO categories (category_id, rank) " +
                                    "VALUES ${
                                        catList.map { listOf(it.categoryId, it.rank) } //for each list item, get the couple pk/name
                                            .joinToString { "(\"${it[0]}\", ${it[1]})" } //convert couple into (pk, "name")
                                    }")

                            val itemPrePopValues =
                                context.resources.getStringArray(R.array.pre_pop_item)
                            db.execSQL("INSERT INTO usual_items (name, category, quantity, in_shopping_list) " +
                                    "VALUES (${itemPrePopValues.joinToString { "\"$it\"" }}, 0)"
                            )
                            db.execSQL("INSERT INTO articles (name, category, quantity, in_cart) " +
                                    "VALUES (${itemPrePopValues.joinToString { "\"$it\"" }}, 0)"
                            )
                        }
                    })
                            .fallbackToDestructiveMigration()
                        .build()
                    INSTANCE = instance
                }
                return instance
            }
        }
    }
}