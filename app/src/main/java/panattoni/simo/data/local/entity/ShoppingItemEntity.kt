package panattoni.simo.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import panattoni.simo.data.model.ShoppingItem

@Entity(
    tableName = "shopping_items",
    indices = [
        Index("isChecked"),
        Index("createdAt")
    ]
)
data class ShoppingItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val itemName: String,
    val quantity: Double,
    val unit: String,
    val source: String = "Manuale",
    val isChecked: Boolean = false,
    @androidx.room.ColumnInfo(defaultValue = "0.0")
    val unitPrice: Double = 0.0,
    val priceUnit: String? = null,
    val createdAt: Long = System.currentTimeMillis()
){
    fun toDomain(): ShoppingItem = ShoppingItem(
        id = id,
        itemName = itemName,
        quantity = quantity,
        unit = unit,
        source = source,
        isChecked = isChecked,
        unitPrice = unitPrice,
        priceUnit = priceUnit,
        createdAt = createdAt
    )

    companion object{
        fun fromDomain(item: ShoppingItem): ShoppingItemEntity = ShoppingItemEntity(
            id = item.id,
            itemName = item.itemName,
            quantity = item.quantity,
            unit = item.unit,
            source = item.source,
            isChecked = item.isChecked,
            unitPrice = item.unitPrice,
            priceUnit = item.priceUnit,
            createdAt = item.createdAt
        )
    }
}
