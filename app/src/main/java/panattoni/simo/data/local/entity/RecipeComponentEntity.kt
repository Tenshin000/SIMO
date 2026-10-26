package panattoni.simo.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import panattoni.simo.data.model.RecipeComponent

@Entity(
    tableName = "recipe_components",
    foreignKeys = [
        ForeignKey(
            entity = RecipeEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentRecipeId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["childItemId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["parentRecipeId", "childItemId"], unique = true),
        Index("childItemId")
    ]
)
data class RecipeComponentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val parentRecipeId: String,
    val childItemId: String,
    val baseQuantity: Double,
    val unit: String,
    val sortOrder: Int = 0
){
    fun toDomain(): RecipeComponent = RecipeComponent(
        id = id,
        parentRecipeId = parentRecipeId,
        childItemId = childItemId,
        baseQuantity = baseQuantity,
        unit = unit,
        sortOrder = sortOrder
    )

    companion object{
        fun fromDomain(comp: RecipeComponent): RecipeComponentEntity = RecipeComponentEntity(
            id = comp.id,
            parentRecipeId = comp.parentRecipeId,
            childItemId = comp.childItemId,
            baseQuantity = comp.baseQuantity,
            unit = comp.unit,
            sortOrder = comp.sortOrder
        )
    }
}
