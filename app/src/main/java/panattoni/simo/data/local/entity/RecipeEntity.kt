package panattoni.simo.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import panattoni.simo.data.model.Recipe

@Entity(
    tableName = "recipes",
    indices = [
        Index("name"),
        Index("isFavorite"),
        Index("category"),
        Index("createdAt")
    ]
)
data class RecipeEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String = "",
    val baseYieldQuantity: Double,
    val yieldUnit: String,
    val instructions: String = "",
    val isDefault: Boolean = false,
    val isFavorite: Boolean = false,
    val category: String = "Altro",
    val createdAt: Long = System.currentTimeMillis(),
    // Modificatore percentuale del peso in cottura (es. -15.0 per calo del 15%, +120.0 per resa pasta/riso)
    val cookingWeightModifier: Double = 0.0
){
    fun toDomain(): Recipe = Recipe(
        id = id,
        name = name,
        description = description,
        baseYieldQuantity = baseYieldQuantity,
        yieldUnit = yieldUnit,
        instructions = instructions,
        isDefault = isDefault,
        isFavorite = isFavorite,
        category = category,
        createdAt = createdAt,
        cookingWeightModifier = cookingWeightModifier
    )

    companion object{
        fun fromDomain(recipe: Recipe): RecipeEntity = RecipeEntity(
            id = recipe.id,
            name = recipe.name,
            description = recipe.description,
            baseYieldQuantity = recipe.baseYieldQuantity,
            yieldUnit = recipe.yieldUnit,
            instructions = recipe.instructions,
            isDefault = recipe.isDefault,
            isFavorite = recipe.isFavorite,
            category = recipe.category,
            createdAt = recipe.createdAt,
            cookingWeightModifier = recipe.cookingWeightModifier
        )
    }
}
