package panattoni.simo.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import panattoni.simo.data.local.entity.ItemEntity
import kotlinx.coroutines.flow.Flow

/**
 * Accesso ai dati per gli Item (ingredienti base e ricette referenziabili).
 */
@Dao
interface ItemDao{
    @Query("SELECT * FROM items ORDER BY name ASC")
    fun getAllItems(): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items ORDER BY name ASC")
    suspend fun getAllItemsDirect(): List<ItemEntity>

    @Query("SELECT * FROM items WHERE id = :id LIMIT 1")
    suspend fun getItemById(id: String): ItemEntity?

    @Query("SELECT * FROM items WHERE type = :type ORDER BY name ASC")
    fun getItemsByType(type: String): Flow<List<ItemEntity>>

    @Upsert
    suspend fun upsertItem(item: ItemEntity)

    @Upsert
    suspend fun upsertItems(items: List<ItemEntity>)

    @Upsert
    suspend fun insertItem(item: ItemEntity)

    @Upsert
    suspend fun insertItems(items: List<ItemEntity>)

    @Update
    suspend fun updateItem(item: ItemEntity)

    @Delete
    suspend fun deleteItem(item: ItemEntity)

    @Query("DELETE FROM items WHERE id = :id")
    suspend fun deleteItemById(id: String)

    @Query("UPDATE items SET type = :newType WHERE id = :id")
    suspend fun updateItemType(id: String, newType: String)
}
