package panattoni.simo.data.model

import panattoni.simo.domain.Allergen

/**
 * Distingue gli item tra materie prime fisiche (RAW_INGREDIENT) e preparazioni composte (RECIPE).
 */
enum class ItemType{
    RAW_INGREDIENT,
    RECIPE
}

/**
 * Modello anagrafico di un ingrediente o sotto-ricetta con unità, costo, lista allergeni e valori nutrizionali.
 */
data class Item(
    val id: String,
    val name: String,
    val type: ItemType,
    val defaultUnit: String = "g",
    val gramsPerUnit: Double? = null, // Per ingredienti discreti (es. 1 uovo = 50g)
    val pricePerUnit: Double? = null, // Costo unitario (es. 1.20 per €1.20)
    val priceUnit: String? = null, // Unità commerciale di costo (es. "kg", "l", "unità")
    val allergens: String = "", // Stringa CSV con gli ID degli allergeni (es. "GLUTINE,LATTE")
    // Valori nutrizionali per 100g di ingrediente crudo/base
    val caloriesKcal: Double? = null, // Calorie (kcal per 100g)
    val carbohydratesG: Double? = null, // Carboidrati totali (g per 100g)
    val fatsG: Double? = null, // Grassi totali (g per 100g)
    val proteinsG: Double? = null, // Proteine totali (g per 100g)
    val waterG: Double? = null, // Acqua/umidità (g per 100g)
    val sodiumMg: Double? = null // Sodio (mg per 100g)
){
    val allergensList: List<Allergen>
        get() = Allergen.parseAllergensString(allergens)
}

/**
 * Modello principale della ricetta con metadati, resa base, modificatore peso in cottura e istruzioni.
 */
data class Recipe(
    val id: String,
    val name: String,
    val description: String = "",
    val baseYieldQuantity: Double,
    val yieldUnit: String, // es. "cornetti", "porzioni", "pizze", "g"
    val instructions: String = "",
    val isDefault: Boolean = false, // True se inserita con il pre-popolamento iniziale
    val isFavorite: Boolean = false,
    val category: String = "Altro",
    val createdAt: Long = System.currentTimeMillis(),
    // Modificatore percentuale del peso in cottura (es. -15.0 per calo/evaporazione del 15%, +120.0 per idratazione pasta/riso)
    val cookingWeightModifier: Double = 0.0
)

/**
 * Rappresenta un arco orientato del grafo (DAG) tra una ricetta genitore e un ingrediente/sotto-ricetta.
 */
data class RecipeComponent(
    val id: Long = 0,
    val parentRecipeId: String,
    val childItemId: String,
    val baseQuantity: Double,
    val unit: String,
    val sortOrder: Int = 0
)

/**
 * Associazione completa di un componente con i dettagli dell'Item referenziato ed eventuale sotto-ricetta.
 */
data class RecipeComponentDetail(
    val component: RecipeComponent,
    val item: Item,
    val subRecipe: Recipe? = null
)

/**
 * Ricetta aggregata con la lista di tutti i suoi componenti di primo livello.
 */
data class RecipeWithComponents(
    val recipe: Recipe,
    val components: List<RecipeComponentDetail>
)

/**
 * Singolo ingrediente scalato con quantità ricalcolata, costo parziale ed eventuale sotto-albero ricorsivo.
 */
data class ScaledComponent(
    val componentDetail: RecipeComponentDetail,
    val scaledQuantity: Double,
    val formattedScaledQuantity: String,
    val subRecipeTree: ScaledRecipeTree? = null,
    val componentCost: Double? = null // Costo per questo componente scalato in Euro
)

/**
 * Albero completo della ricetta scalata con totale Food Cost, costo porzione e allergeni accumulati.
 */
data class ScaledRecipeTree(
    val recipe: Recipe,
    val targetYieldQuantity: Double,
    val scalingFactor: Double,
    val components: List<ScaledComponent>,
    val totalCost: Double = 0.0,
    val costPerPortion: Double = 0.0,
    val hasCostData: Boolean = false,
    val allergens: List<Allergen> = emptyList()
)

/**
 * Articolo all'interno della lista della spesa con quantità, costo unitario e stato di spunta.
 */
data class ShoppingItem(
    val id: Long = 0,
    val itemName: String,
    val quantity: Double,
    val unit: String,
    val source: String = "Manuale", // "Manuale" o nome della ricetta sorgente
    val isChecked: Boolean = false,
    val unitPrice: Double = 0.0,
    val priceUnit: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

