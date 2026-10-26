package panattoni.simo.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import panattoni.simo.data.local.entity.ShoppingItemEntity
import kotlinx.coroutines.flow.Flow

/**
 * Accesso ai dati per gli articoli della Lista della Spesa.
 */
@Dao
interface ShoppingDao{
    // --- Query di lettura e ricerca articoli attivi ---
    @Query("SELECT * FROM shopping_items ORDER BY isChecked ASC, createdAt DESC")
    fun getAllShoppingItems(): Flow<List<ShoppingItemEntity>>

    @Query("SELECT * FROM shopping_items WHERE isChecked = 0")
    suspend fun getActiveShoppingItems(): List<ShoppingItemEntity>

    @Query("SELECT * FROM shopping_items WHERE LOWER(TRIM(itemName)) = LOWER(TRIM(:name)) AND LOWER(TRIM(unit)) = LOWER(TRIM(:unit)) AND isChecked = 0 LIMIT 1")
    suspend fun findActiveItemByNameAndUnit(name: String, unit: String): ShoppingItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingItem(item: ShoppingItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingItems(items: List<ShoppingItemEntity>)

    @Update
    suspend fun updateShoppingItem(item: ShoppingItemEntity)

    @Delete
    suspend fun deleteShoppingItem(item: ShoppingItemEntity)

    @Query("DELETE FROM shopping_items WHERE id = :id")
    suspend fun deleteShoppingItemById(id: Long)

    @Query("DELETE FROM shopping_items WHERE isChecked = 1")
    suspend fun deleteCheckedItems(): Int

    @Query("DELETE FROM shopping_items")
    suspend fun clearAll()

    @Query("SELECT * FROM shopping_items WHERE id = :id LIMIT 1")
    suspend fun getItemById(id: Long): ShoppingItemEntity?

    @Query("UPDATE shopping_items SET isChecked = :isChecked WHERE id = :id")
    suspend fun setChecked(id: Long, isChecked: Boolean)

    @Query("UPDATE shopping_items SET quantity = :newQuantity WHERE id = :id")
    suspend fun updateQuantity(id: Long, newQuantity: Double)

    @Query("UPDATE shopping_items SET quantity = :newQuantity, unitPrice = :unitPrice, priceUnit = :priceUnit WHERE id = :id")
    suspend fun updateQuantityAndPrice(id: Long, newQuantity: Double, unitPrice: Double, priceUnit: String?)

    @Query("UPDATE shopping_items SET quantity = :newQuantity, unit = :newUnit, unitPrice = :unitPrice, priceUnit = :priceUnit WHERE id = :id")
    suspend fun updateItemDetails(id: Long, newQuantity: Double, newUnit: String, unitPrice: Double, priceUnit: String?)
}
