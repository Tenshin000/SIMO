package panattoni.simo.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import panattoni.simo.data.local.entity.RecipeComponentEntity
import panattoni.simo.data.local.entity.RecipeEntity
import kotlinx.coroutines.flow.Flow

/**
 * Accesso ai dati per Ricette e Componenti (relazioni gerarchiche DAG).
 */
@Dao
interface RecipeDao{
    // --- Query di lettura ricette e componenti ---
    @Query("SELECT * FROM recipes ORDER BY name ASC")
    fun getAllRecipes(): Flow<List<RecipeEntity>>

    @Query("SELECT * FROM recipes ORDER BY name ASC")
    suspend fun getAllRecipesDirect(): List<RecipeEntity>

    @Query("SELECT * FROM recipes WHERE id = :id LIMIT 1")
    suspend fun getRecipeById(id: String): RecipeEntity?

    @Query("SELECT * FROM recipes WHERE id = :id LIMIT 1")
    fun getRecipeFlowById(id: String): Flow<RecipeEntity?>

    @Query("SELECT * FROM recipe_components WHERE parentRecipeId = :recipeId ORDER BY sortOrder ASC, id ASC")
    fun getComponentsForRecipe(recipeId: String): Flow<List<RecipeComponentEntity>>

    @Query("SELECT * FROM recipe_components WHERE parentRecipeId = :recipeId ORDER BY sortOrder ASC, id ASC")
    suspend fun getComponentsForRecipeDirect(recipeId: String): List<RecipeComponentEntity>

    @Query("SELECT * FROM recipe_components")
    suspend fun getAllComponentsDirect(): List<RecipeComponentEntity>

    @Query("SELECT * FROM recipe_components")
    fun getAllComponentsFlow(): Flow<List<RecipeComponentEntity>>

    @Query("SELECT COUNT(*) FROM recipes")
    suspend fun getRecipeCount(): Int

    // --- Inserimento, aggiornamento ed eliminazione ---
    @Upsert
    suspend fun upsertRecipe(recipe: RecipeEntity)

    @Upsert
    suspend fun upsertRecipes(recipes: List<RecipeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipe(recipe: RecipeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipes(recipes: List<RecipeEntity>)

    @Update
    suspend fun updateRecipe(recipe: RecipeEntity)

    @Delete
    suspend fun deleteRecipe(recipe: RecipeEntity)

    @Query("UPDATE recipes SET isFavorite = :isFavorite WHERE id = :recipeId")
    suspend fun setFavorite(recipeId: String, isFavorite: Boolean)

    @Query("DELETE FROM recipes WHERE id = :id")
    suspend fun deleteRecipeById(id: String)

    @Query("SELECT DISTINCT r.name FROM recipes r INNER JOIN recipe_components c ON r.id = c.parentRecipeId WHERE c.childItemId = :recipeId")
    suspend fun getParentRecipeNamesUsingChild(recipeId: String): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComponents(components: List<RecipeComponentEntity>)

    @Query("DELETE FROM recipe_components WHERE parentRecipeId = :recipeId")
    suspend fun deleteComponentsForRecipe(recipeId: String)

    // Sostituisce ricetta e componenti atomicamente in un'unica transazione SQLite
    @Transaction
    suspend fun replaceRecipeWithComponents(
        recipe: RecipeEntity,
        components: List<RecipeComponentEntity>
    ){
        insertRecipe(recipe)
        deleteComponentsForRecipe(recipe.id)
        insertComponents(components)
    }
}
