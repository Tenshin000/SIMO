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
    version = 14,
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
        private const val KEY_MAINTENANCE_V14_DONE = "db_maintenance_v14_done"
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

        // Migrazione v13 -> v14: aggiunta 6 valori nutrizionali su items e modificatore peso cottura su recipes
        val MIGRATION_13_14 = object : androidx.room.migration.Migration(13, 14){
            override fun migrate(db: SupportSQLiteDatabase){
                // 1. Aggiunta 6 colonne per i valori nutrizionali per 100g negli ingredienti
                db.execSQL("ALTER TABLE items ADD COLUMN caloriesKcal REAL DEFAULT NULL")
                db.execSQL("ALTER TABLE items ADD COLUMN carbohydratesG REAL DEFAULT NULL")
                db.execSQL("ALTER TABLE items ADD COLUMN fatsG REAL DEFAULT NULL")
                db.execSQL("ALTER TABLE items ADD COLUMN proteinsG REAL DEFAULT NULL")
                db.execSQL("ALTER TABLE items ADD COLUMN waterG REAL DEFAULT NULL")
                db.execSQL("ALTER TABLE items ADD COLUMN sodiumMg REAL DEFAULT NULL")

                // 2. Aggiunta colonna per il modificatore peso in cottura (calo o incremento resa)
                db.execSQL("ALTER TABLE recipes ADD COLUMN cookingWeightModifier REAL NOT NULL DEFAULT 0.0")

                // 3. Popolamento valori nutrizionali predefiniti per gli ingredienti di base esistenti
                db.execSQL("UPDATE items SET caloriesKcal = 64.0, carbohydratesG = 4.8, fatsG = 3.6, proteinsG = 3.3, waterG = 87.5, sodiumMg = 44.0 WHERE id = 'ing_latte'")
                db.execSQL("UPDATE items SET caloriesKcal = 322.0, carbohydratesG = 3.6, fatsG = 26.5, proteinsG = 15.9, waterG = 51.0, sodiumMg = 48.0 WHERE id = 'ing_tuorli'")
                db.execSQL("UPDATE items SET caloriesKcal = 387.0, carbohydratesG = 99.9, fatsG = 0.0, proteinsG = 0.0, waterG = 0.1, sodiumMg = 1.0 WHERE id = 'ing_zucchero'")
                db.execSQL("UPDATE items SET caloriesKcal = 381.0, carbohydratesG = 91.0, fatsG = 0.1, proteinsG = 0.3, waterG = 8.5, sodiumMg = 9.0 WHERE id = 'ing_amido_mais'")
                db.execSQL("UPDATE items SET caloriesKcal = 47.0, carbohydratesG = 16.0, fatsG = 0.3, proteinsG = 1.5, waterG = 81.6, sodiumMg = 6.0 WHERE id = 'ing_scorza_limone'")
                db.execSQL("UPDATE items SET caloriesKcal = 288.0, carbohydratesG = 12.6, fatsG = 0.1, proteinsG = 0.1, waterG = 52.6, sodiumMg = 9.0 WHERE id = 'ing_vaniglia'")
                db.execSQL("UPDATE items SET caloriesKcal = 364.0, carbohydratesG = 72.0, fatsG = 1.5, proteinsG = 14.0, waterG = 12.0, sodiumMg = 2.0 WHERE id = 'ing_farina_manitoba'")
                db.execSQL("UPDATE items SET caloriesKcal = 364.0, carbohydratesG = 76.3, fatsG = 1.0, proteinsG = 10.3, waterG = 14.0, sodiumMg = 2.0 WHERE id = 'ing_farina_00'")
                db.execSQL("UPDATE items SET caloriesKcal = 143.0, carbohydratesG = 0.7, fatsG = 9.5, proteinsG = 12.6, waterG = 76.0, sodiumMg = 142.0 WHERE id = 'ing_uova'")
                db.execSQL("UPDATE items SET caloriesKcal = 717.0, carbohydratesG = 0.1, fatsG = 81.1, proteinsG = 0.9, waterG = 16.0, sodiumMg = 11.0 WHERE id = 'ing_burro'")
                db.execSQL("UPDATE items SET caloriesKcal = 105.0, carbohydratesG = 18.0, fatsG = 1.9, proteinsG = 8.4, waterG = 69.0, sodiumMg = 30.0 WHERE id = 'ing_lievito_birra'")
                db.execSQL("UPDATE items SET caloriesKcal = 0.0, carbohydratesG = 0.0, fatsG = 0.0, proteinsG = 0.0, waterG = 0.2, sodiumMg = 38758.0 WHERE id = 'ing_sale'")
                db.execSQL("UPDATE items SET caloriesKcal = 364.0, carbohydratesG = 74.0, fatsG = 1.2, proteinsG = 11.5, waterG = 13.0, sodiumMg = 2.0 WHERE id = 'ing_farina_0'")
                db.execSQL("UPDATE items SET caloriesKcal = 0.0, carbohydratesG = 0.0, fatsG = 0.0, proteinsG = 0.0, waterG = 100.0, sodiumMg = 5.0 WHERE id = 'ing_acqua'")
                db.execSQL("UPDATE items SET caloriesKcal = 884.0, carbohydratesG = 0.0, fatsG = 100.0, proteinsG = 0.0, waterG = 0.0, sodiumMg = 2.0 WHERE id = 'ing_olio_evo'")
                db.execSQL("UPDATE items SET caloriesKcal = 22.0, carbohydratesG = 3.5, fatsG = 0.2, proteinsG = 1.2, waterG = 93.5, sodiumMg = 12.0 WHERE id = 'ing_pomodori_pelati'")
                db.execSQL("UPDATE items SET caloriesKcal = 280.0, carbohydratesG = 2.2, fatsG = 22.0, proteinsG = 18.0, waterG = 55.0, sodiumMg = 400.0 WHERE id = 'ing_mozzarella'")
                db.execSQL("UPDATE items SET caloriesKcal = 23.0, carbohydratesG = 2.7, fatsG = 0.6, proteinsG = 3.2, waterG = 92.0, sodiumMg = 4.0 WHERE id = 'ing_basilico'")

                // 4. Modificatori peso cottura predefiniti per le ricette standard
                db.execSQL("UPDATE recipes SET cookingWeightModifier = -8.0 WHERE id = 'recipe_crema_pasticcera'")
                db.execSQL("UPDATE recipes SET cookingWeightModifier = -12.0 WHERE id = 'recipe_cornetto_crema'")
                db.execSQL("UPDATE recipes SET cookingWeightModifier = -10.0 WHERE id = 'recipe_pizza_margherita'")
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
                    .addMigrations(MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14)
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

                        // Manutenzione v14: compila automaticamente le 3 ricette di default e tutti i loro ingredienti
                        if (!prefs.getBoolean(KEY_MAINTENANCE_V14_DONE, false)){
                            try{
                                for (item in PrepopulateData.DEFAULT_ITEMS){
                                    if (item.caloriesKcal != null){
                                        db.execSQL(
                                            "UPDATE items SET caloriesKcal = ?, carbohydratesG = ?, fatsG = ?, proteinsG = ?, waterG = ?, sodiumMg = ? WHERE id = ?",
                                            arrayOf(item.caloriesKcal, item.carbohydratesG, item.fatsG, item.proteinsG, item.waterG, item.sodiumMg, item.id)
                                        )
                                    }
                                }
                                for (rec in PrepopulateData.DEFAULT_RECIPES){
                                    db.execSQL(
                                        "UPDATE recipes SET cookingWeightModifier = ? WHERE id = ?",
                                        arrayOf(rec.cookingWeightModifier, rec.id)
                                    )
                                }
                                prefs.edit().putBoolean(KEY_MAINTENANCE_V14_DONE, true).apply()
                            } catch (_: Exception){}
                        }
                        ensurePrepopulated(database, context)
                    }
                }
            }
        }
    }
}
