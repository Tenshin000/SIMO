package panattoni.simo.domain

import panattoni.simo.data.model.ItemType
import panattoni.simo.data.model.RecipeWithComponents
import panattoni.simo.data.model.ScaledComponent
import panattoni.simo.data.model.ScaledRecipeTree

object MathEngine{

    private const val MAX_RECURSION_DEPTH = 25
    private val DISCRETE_CODES = setOf("unità", "unita", "unit", "pz", "pezzi", "pezzo")

    /**
     * Calcola il fattore di scala k partendo dalla resa desiderata rispetto a quella base.
     * k = targetYield / baseYield
     * Protetto contro NaN, Infinity e divisioni per zero.
     */
    fun calculateScaleFactorByYield(baseYield: Double, targetYield: Double): Double{
        if (!baseYield.isFinite() || !targetYield.isFinite()) return 1.0
        if (baseYield <= 0.0 || targetYield <= 0.0) return 1.0
        val factor = targetYield / baseYield
        return if (factor.isFinite() && !factor.isNaN() && factor > 0.0) factor else 1.0
    }

    /**
     * Reverse Scaling: calcola il fattore di scala k partendo dalla quantità a disposizione
     * di un singolo ingrediente rispetto alla sua quantità base.
     * k = targetAmount / baseAmount
     * Protetto contro NaN, Infinity e divisioni per zero.
     */
    fun calculateScaleFactorByIngredient(baseAmount: Double, targetAmount: Double): Double{
        if (!baseAmount.isFinite() || !targetAmount.isFinite()) return 1.0
        if (baseAmount <= 0.0 || targetAmount <= 0.0) return 1.0
        val factor = targetAmount / baseAmount
        return if (factor.isFinite() && !factor.isNaN() && factor > 0.0) factor else 1.0
    }

    /**
     * Calcola il costo in euro per una determinata quantità di ingrediente
     * in base al prezzo unitario configurato (es. €1.20/kg per 250g -> 250/1000 * 1.20 = €0.30).
     */
    fun calculateComponentCost(
        quantity: Double,
        recipeUnit: String,
        pricePerUnit: Double?,
        priceUnit: String?,
        gramsPerUnit: Double? = null
    ): Double?{
        if (MeasurementUnit.isQb(recipeUnit)) return 0.0
        if (pricePerUnit == null || pricePerUnit <= 0.0 || !pricePerUnit.isFinite()) return null
        if (quantity <= 0.0 || !quantity.isFinite()) return 0.0
        val pUnit = priceUnit?.trim()?.lowercase() ?: return null
        val rUnit = recipeUnit.trim().lowercase()

        // 1. Stessa unità esatta
        if (pUnit == rUnit){
            return quantity * pricePerUnit
        }

        // 2. Peso: g vs kg
        if (rUnit == "g" && pUnit == "kg"){
            return (quantity / 1000.0) * pricePerUnit
        }
        if (rUnit == "kg" && pUnit == "g"){
            return (quantity * 1000.0) * pricePerUnit
        }

        // 3. Volume: ml vs l
        if (rUnit == "ml" && pUnit == "l"){
            return (quantity / 1000.0) * pricePerUnit
        }
        if (rUnit == "l" && pUnit == "ml"){
            return (quantity * 1000.0) * pricePerUnit
        }

        // 4. Se il prezzo è per kg/g ma l'ingrediente è in unità/pz ed ha gramsPerUnit definito
        val isRUnitDiscrete = MeasurementUnit.isIntegerUnit(rUnit) || rUnit == "unità" || rUnit == "unita" || rUnit == "pz"
        if (isRUnitDiscrete && gramsPerUnit != null && gramsPerUnit > 0.0){
            val totalGrams = quantity * gramsPerUnit
            if (pUnit == "kg"){
                return (totalGrams / 1000.0) * pricePerUnit
            }
            if (pUnit == "g"){
                return totalGrams * pricePerUnit
            }
        }

        // 5. Unità discrete equivalenti (es. unità, pz)
        if (rUnit in DISCRETE_CODES && pUnit in DISCRETE_CODES){
            return quantity * pricePerUnit
        }

        // 6. Cucchiai / cucchiaini / pizzichi: peso approssimativo comune
        if (pUnit == "kg" || pUnit == "g"){
            val approxGrams = when (rUnit){
                "cucchiaio", "cucchiai" -> 15.0
                "cucchiaino", "cucchiaini" -> 5.0
                "pizzico", "pizzichi" -> 1.0
                else -> null
            }
            if (approxGrams != null){
                val totalGrams = quantity * approxGrams
                return if (pUnit == "kg") (totalGrams / 1000.0) * pricePerUnit else totalGrams * pricePerUnit
            }
        }

        // Default proporzione diretta se non convertibile
        return quantity * pricePerUnit
    }

    /**
     * Costruisce ricorsivamente l'albero delle dosi scalate della ricetta e di tutte le sotto-ricette annidate,
     * calcolando anche il Food Cost in tempo reale (costo componente, costo totale impasto e costo porzione).
     *
     * @param recipeWithComponents la ricetta genitore con i suoi componenti
     * @param scalingFactor il moltiplicatore k
     * @param allRecipesMap mappa di tutte le ricette con componenti disponibili per la risoluzione a cascata
     * @param visitedRecipeIds set di ID per prevenzione di cicli o ricorsione infinita
     * @param currentDepth profondità ricorsiva corrente per prevenire stack overflow
     */
    fun buildScaledTree(
        recipeWithComponents: RecipeWithComponents,
        scalingFactor: Double,
        allRecipesMap: Map<String, RecipeWithComponents>,
        visitedRecipeIds: Set<String> = emptySet(),
        currentDepth: Int = 0
    ): ScaledRecipeTree{
        val recipe = recipeWithComponents.recipe
        val safeFactor = if (!scalingFactor.isFinite() || scalingFactor <= 0.0) 1.0 else scalingFactor
        val targetYield = recipe.baseYieldQuantity * safeFactor

        val currentVisited = visitedRecipeIds + recipe.id

        var hasAnyCost = false
        var accumulatedCost = 0.0

        val scaledComponents = recipeWithComponents.components.map{ detail ->
            val comp = detail.component
            val safeBaseQty = if (comp.baseQuantity.isFinite() && comp.baseQuantity >= 0.0) comp.baseQuantity else 0.0
            val scaledQuantity = safeBaseQty * safeFactor
            val formatted = MeasurementUnit.formatAmount(scaledQuantity, comp.unit)

            var subTree: ScaledRecipeTree? = null
            var componentCost: Double? = null

            if (detail.item.type == ItemType.RECIPE && currentDepth < MAX_RECURSION_DEPTH){
                val subRecipeWithComps = allRecipesMap[detail.item.id]
                if (subRecipeWithComps != null && !currentVisited.contains(subRecipeWithComps.recipe.id)){
                    val subBaseYield = subRecipeWithComps.recipe.baseYieldQuantity
                    val subFactor = if (subBaseYield.isFinite() && subBaseYield > 0.0 && scaledQuantity.isFinite()){
                        scaledQuantity / subBaseYield
                    } else 1.0

                    val calculatedSubTree = buildScaledTree(
                        recipeWithComponents = subRecipeWithComps,
                        scalingFactor = subFactor,
                        allRecipesMap = allRecipesMap,
                        visitedRecipeIds = currentVisited,
                        currentDepth = currentDepth + 1
                    )
                    subTree = calculatedSubTree

                    // Se la sotto-ricetta ha dati di costo, il costo di questa componente è il totale calcolato dalla sotto-ricetta
                    if (calculatedSubTree.hasCostData){
                        componentCost = calculatedSubTree.totalCost
                    }
                }
            }

            // Se è materia prima oppure sotto-ricetta senza albero calcolato
            if (componentCost == null){
                componentCost = if (MeasurementUnit.isQb(comp.unit)){
                    0.0
                } else if (detail.item.pricePerUnit != null){
                    calculateComponentCost(
                        quantity = scaledQuantity,
                        recipeUnit = comp.unit,
                        pricePerUnit = detail.item.pricePerUnit,
                        priceUnit = detail.item.priceUnit,
                        gramsPerUnit = detail.item.gramsPerUnit
                    )
                } else null
            }

            if (componentCost != null && componentCost > 0.0){
                hasAnyCost = true
                accumulatedCost += componentCost
            }

            ScaledComponent(
                componentDetail = detail,
                scaledQuantity = scaledQuantity,
                formattedScaledQuantity = formatted,
                subRecipeTree = subTree,
                componentCost = componentCost
            )
        }

        val costPerPortion = if (targetYield > 0.0) accumulatedCost / targetYield else 0.0

        val allCollectedAllergens = mutableSetOf<Allergen>()
        for (comp in scaledComponents){
            if (comp.subRecipeTree != null){
                allCollectedAllergens.addAll(comp.subRecipeTree.allergens)
            } else{
                allCollectedAllergens.addAll(comp.componentDetail.item.allergensList)
            }
        }
        val sortedAllergens = Allergen.entries.filter{ allCollectedAllergens.contains(it) }

        return ScaledRecipeTree(
            recipe = recipe,
            targetYieldQuantity = targetYield,
            scalingFactor = safeFactor,
            components = scaledComponents,
            totalCost = accumulatedCost,
            costPerPortion = costPerPortion,
            hasCostData = hasAnyCost,
            allergens = sortedAllergens
        )
    }

    /**
     * Rileva se aggiungere la sotto-ricetta 'candidateChildId' dentro 'parentRecipeId'
     * creerebbe una dipendenza circolare (Ciclo nel grafo orientato / Violazione DAG).
     *
     * @param parentRecipeId ID della ricetta genitore
     * @param candidateChildId ID della ricetta figlia che si vorrebbe inserire
     * @param existingEdges lista di coppie (genitoreId -> figlioId) attualmente esistenti
     * @return true se causerebbe un ciclo, false altrimenti
     */
    fun wouldCreateCycle(
        parentRecipeId: String,
        candidateChildId: String,
        existingEdges: List<Pair<String, String>>
    ): Boolean{
        if (parentRecipeId.isBlank() || candidateChildId.isBlank()) return false
        if (parentRecipeId == candidateChildId) return true

        // Controlla se candidateChildId può raggiungere parentRecipeId seguendo le relazioni esistenti
        val adjacency = mutableMapOf<String, MutableList<String>>()
        for ((parent, child) in existingEdges){
            if (parent.isNotBlank() && child.isNotBlank()){
                adjacency.getOrPut(parent){ mutableListOf() }.add(child)
            }
        }

        val queue = ArrayDeque<String>()
        queue.add(candidateChildId)
        val visited = mutableSetOf<String>()

        while (queue.isNotEmpty()){
            val curr = queue.removeFirst()
            if (curr == parentRecipeId) return true
            if (visited.add(curr)){
                adjacency[curr]?.forEach{ next ->
                    if (!visited.contains(next)){
                        queue.add(next)
                    }
                }
            }
        }

        return false
    }
}
