package panattoni.simo.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import panattoni.simo.data.local.dao.ItemDao
import panattoni.simo.data.local.dao.RecipeDao
import panattoni.simo.data.local.entity.ItemEntity
import panattoni.simo.data.local.entity.RecipeComponentEntity
import panattoni.simo.data.local.entity.RecipeEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock

import panattoni.simo.data.local.dao.ShoppingDao
import panattoni.simo.data.local.entity.ShoppingItemEntity

/**
 * Database principale Room dell'applicazione SIMO.
 * Gestisce la persistenza locale di Item, Ricette, Componenti (DAG) e Lista della Spesa.
 */
@Database(
    entities = [
        ItemEntity::class,
        RecipeEntity::class,
        RecipeComponentEntity::class,
        ShoppingItemEntity::class
    ],
    version = 13,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase(){

    abstract fun itemDao(): ItemDao
    abstract fun recipeDao(): RecipeDao
    abstract fun shoppingDao(): ShoppingDao

    companion object{
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private const val PREFS_NAME = "simo_prefs"
        private const val KEY_PREPOPULATED = "db_prepopulated"
        private const val KEY_MAINTENANCE_DONE = "db_maintenance_v13_done"
        private val prepopulateMutex = kotlinx.coroutines.sync.Mutex()

        // Migrazione v9 -> v10: aggiunta colonna allergeni e popolamento per dati predefiniti
        val MIGRATION_9_10 = object : androidx.room.migration.Migration(9, 10){
            override fun migrate(db: SupportSQLiteDatabase){
                db.execSQL("ALTER TABLE items ADD COLUMN allergens TEXT NOT NULL DEFAULT ''")
                // Popola gli allergeni per gli ingredienti e ricette di default
                db.execSQL("UPDATE items SET allergens = 'LATTE' WHERE id = 'ing_latte'")
                db.execSQL("UPDATE items SET allergens = 'UOVA' WHERE id = 'ing_tuorli'")
                db.execSQL("UPDATE items SET allergens = 'GLUTINE' WHERE id = 'ing_farina_manitoba'")
                db.execSQL("UPDATE items SET allergens = 'GLUTINE' WHERE id = 'ing_farina_00'")
                db.execSQL("UPDATE items SET allergens = 'UOVA' WHERE id = 'ing_uova'")
                db.execSQL("UPDATE items SET allergens = 'LATTE' WHERE id = 'ing_burro'")
                db.execSQL("UPDATE items SET allergens = 'GLUTINE' WHERE id = 'ing_farina_0'")
                db.execSQL("UPDATE items SET allergens = 'LATTE' WHERE id = 'ing_mozzarella'")
                db.execSQL("UPDATE items SET allergens = 'UOVA,LATTE' WHERE id = 'recipe_crema_pasticcera'")
            }
        }

        // Migrazione v10 -> v11: aggiunta colonna unitPrice a shopping_items
        val MIGRATION_10_11 = object : androidx.room.migration.Migration(10, 11){
            override fun migrate(db: SupportSQLiteDatabase){
                db.execSQL("ALTER TABLE shopping_items ADD COLUMN unitPrice REAL NOT NULL DEFAULT 0.0")
            }
        }

        // Migrazione v11 -> v12: aggiunta colonna priceUnit a shopping_items
        val MIGRATION_11_12 = object : androidx.room.migration.Migration(11, 12){
            override fun migrate(db: SupportSQLiteDatabase){
                db.execSQL("ALTER TABLE shopping_items ADD COLUMN priceUnit TEXT DEFAULT NULL")
            }
        }

        // Migrazione v12 -> v13: creazione indici SQLite per velocizzare query e ordinamenti
        val MIGRATION_12_13 = object : androidx.room.migration.Migration(12, 13){
            override fun migrate(db: SupportSQLiteDatabase){
                db.execSQL("CREATE INDEX IF NOT EXISTS index_recipes_name ON recipes(name)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_recipes_isFavorite ON recipes(isFavorite)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_recipes_category ON recipes(category)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_recipes_createdAt ON recipes(createdAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_items_type ON items(type)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_items_name ON items(name)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_shopping_items_isChecked ON shopping_items(isChecked)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_shopping_items_createdAt ON shopping_items(createdAt)")
            }
        }

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase{
            return INSTANCE ?: synchronized(this){
                val appContext = context.applicationContext
                val instance = Room.databaseBuilder(
                    appContext,
                    AppDatabase::class.java,
                    "simo_recipe_database"
                )
                    .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
                    .addMigrations(MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13)
                    .addCallback(DatabaseCallback(appContext, scope))
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        suspend fun ensurePrepopulated(database: AppDatabase, context: Context){
            prepopulateMutex.withLock{
                try{
                    val count = database.recipeDao().getRecipeCount()
                    if (count == 0){
                        database.itemDao().insertItems(PrepopulateData.DEFAULT_ITEMS)
                        database.recipeDao().insertRecipes(PrepopulateData.DEFAULT_RECIPES)
                        database.recipeDao().insertComponents(PrepopulateData.DEFAULT_COMPONENTS)
                    }
                    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    prefs.edit().putBoolean(KEY_PREPOPULATED, true).apply()
                } catch (_: Exception){}
            }
        }

        private class DatabaseCallback(
            private val context: Context,
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback(){

            override fun onOpen(db: SupportSQLiteDatabase){
                super.onOpen(db)
                INSTANCE?.let{ database ->
                    scope.launch(Dispatchers.IO){
                        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        if (!prefs.getBoolean(KEY_MAINTENANCE_DONE, false)){
                            try{
                                db.execSQL("DELETE FROM recipe_components WHERE rowid NOT IN (SELECT MIN(rowid) FROM recipe_components GROUP BY parentRecipeId, childItemId)")
                                db.execSQL("UPDATE items SET allergens = 'LATTE' WHERE id = 'ing_latte' AND (allergens = '' OR allergens IS NULL)")
                                db.execSQL("UPDATE items SET allergens = 'UOVA' WHERE id = 'ing_tuorli' AND (allergens = '' OR allergens IS NULL)")
                                db.execSQL("UPDATE items SET allergens = 'GLUTINE' WHERE id = 'ing_farina_manitoba' AND (allergens = '' OR allergens IS NULL)")
                                db.execSQL("UPDATE items SET allergens = 'GLUTINE' WHERE id = 'ing_farina_00' AND (allergens = '' OR allergens IS NULL)")
                                db.execSQL("UPDATE items SET allergens = 'UOVA' WHERE id = 'ing_uova' AND (allergens = '' OR allergens IS NULL)")
                                db.execSQL("UPDATE items SET allergens = 'LATTE' WHERE id = 'ing_burro' AND (allergens = '' OR allergens IS NULL)")
                                db.execSQL("UPDATE items SET allergens = 'GLUTINE' WHERE id = 'ing_farina_0' AND (allergens = '' OR allergens IS NULL)")
                                db.execSQL("UPDATE items SET allergens = 'LATTE' WHERE id = 'ing_mozzarella' AND (allergens = '' OR allergens IS NULL)")
                                prefs.edit().putBoolean(KEY_MAINTENANCE_DONE, true).apply()
                            } catch (_: Exception){}
                        }
                        ensurePrepopulated(database, context)
                    }
                }
            }
        }
    }
}
