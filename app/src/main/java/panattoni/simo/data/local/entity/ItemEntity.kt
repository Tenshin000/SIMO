package panattoni.simo.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import panattoni.simo.data.model.Item
import panattoni.simo.data.model.ItemType

@Entity(
    tableName = "items",
    indices = [
        Index("type"),
        Index("name")
    ]
)
data class ItemEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val type: String, // "RAW_INGREDIENT" or "RECIPE"
    val defaultUnit: String = "g",
    val gramsPerUnit: Double? = null,
    val pricePerUnit: Double? = null,
    val priceUnit: String? = null,
    val allergens: String = ""
){
    fun toDomain(): Item = Item(
        id = id,
        name = name,
        type = if (type == "RECIPE") ItemType.RECIPE else ItemType.RAW_INGREDIENT,
        defaultUnit = defaultUnit,
        gramsPerUnit = gramsPerUnit,
        pricePerUnit = pricePerUnit,
        priceUnit = priceUnit,
        allergens = allergens
    )

    companion object{
        fun fromDomain(item: Item): ItemEntity = ItemEntity(
            id = item.id,
            name = item.name,
            type = item.type.name,
            defaultUnit = item.defaultUnit,
            gramsPerUnit = item.gramsPerUnit,
            pricePerUnit = item.pricePerUnit,
            priceUnit = item.priceUnit,
            allergens = item.allergens
        )
    }
}
