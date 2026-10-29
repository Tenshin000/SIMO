package panattoni.simo.domain

import panattoni.simo.data.model.Item
import panattoni.simo.data.model.ItemType
import panattoni.simo.data.model.NutritionalValues
import panattoni.simo.data.model.RecipeNutritionSummary
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
    // Insieme delle unità di massa e volume con equivalenza culinaria 1:1 (1g = 1ml, 1kg = 1l)
    private val MASS_VOLUME_UNITS = setOf(
        "g", "grammi", "grammo",
        "kg", "chilogrammi", "chilogrammo", "chili", "chilo",
        "ml", "millilitri", "millilitro",
        "l", "litri", "litro"
    )

    private fun isKiloOrLiter(u: String): Boolean{
        return u in setOf("kg", "chilogrammi", "chilogrammo", "chili", "chilo", "l", "litri", "litro")
    }

    /**
     * Calcola il costo di un componente gestendo in modo rigoroso le equivalenze tra unità.
     * In cucina vale l'equivalenza standard 1:1 tra massa e volume per densità vicina all'acqua:
     * - 1 g = 1 ml
     * - 1 kg = 1 l (e 1 kg/l = 1000 g/ml)
     * Il prezzo al kg o al litro corrisponde a 1/1000 per g o ml (es. 25 €/kg su 300 ml = 0.025 €/ml * 300 = 7.50 €).
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

        // 2. Conversioni dirette e incrociate tra massa e volume (g, kg, ml, l)
        // Corrispondenza 1 a 1: 1 g = 1 ml, 1 kg = 1 l; 1 kg/l = 1000 g/ml
        if (rUnit in MASS_VOLUME_UNITS && pUnit in MASS_VOLUME_UNITS){
            val quantityInBase = if (isKiloOrLiter(rUnit)) quantity * 1000.0 else quantity
            val pricePerBase = if (isKiloOrLiter(pUnit)) pricePerUnit / 1000.0 else pricePerUnit
            return quantityInBase * pricePerBase
        }

        // 3. Misure culinarie approssimate (cucchiai, cucchiaini, pizzichi) su prezzo a peso o volume
        val approxGrams = when (rUnit){
            "cucchiaio", "cucchiai" -> 15.0
            "cucchiaino", "cucchiaini" -> 5.0
            "pizzico", "pizzichi" -> 1.0
            else -> null
        }
        if (approxGrams != null && pUnit in MASS_VOLUME_UNITS){
            val quantityInBase = quantity * approxGrams
            val pricePerBase = if (isKiloOrLiter(pUnit)) pricePerUnit / 1000.0 else pricePerUnit
            return quantityInBase * pricePerBase
        }

        // 4. Se l'ingrediente è in unità/pz ed ha gramsPerUnit definito con prezzo a peso o volume
        val isRUnitDiscrete = MeasurementUnit.isIntegerUnit(rUnit) || rUnit in DISCRETE_CODES
        if (isRUnitDiscrete && gramsPerUnit != null && gramsPerUnit > 0.0 && pUnit in MASS_VOLUME_UNITS){
            val quantityInBase = quantity * gramsPerUnit
            val pricePerBase = if (isKiloOrLiter(pUnit)) pricePerUnit / 1000.0 else pricePerUnit
            return quantityInBase * pricePerBase
        }

        // 5. Unità discrete equivalenti (es. unità, pz)
        if (rUnit in DISCRETE_CODES && pUnit in DISCRETE_CODES){
            return quantity * pricePerUnit
        }

        // Default proporzione diretta se non convertibile
        return quantity * pricePerUnit
    }

    /**
     * Converte una quantità in grammi in base all'unità di misura specificata.
     * Supporta tutte le unità di misura dell'app (g, kg, ml, l, unità, pz, cucchiai, cucchiaini, pizzichi, q.b.).
     */
    fun convertToGrams(quantity: Double, unit: String, gramsPerUnit: Double? = null): Double{
        if (!quantity.isFinite() || quantity <= 0.0) return 0.0
        if (MeasurementUnit.isQb(unit)) return 0.0

        val u = unit.trim().lowercase()
        return when{
            u in setOf("g", "grammi", "grammo") -> quantity
            u in setOf("kg", "chilogrammi", "chilogrammo", "chili", "chilo") -> quantity * 1000.0
            u in setOf("ml", "millilitri", "millilitro") -> quantity // Densità standard 1.0 g/ml per liquidi alimentari
            u in setOf("l", "litri", "litro") -> quantity * 1000.0
            u in setOf("cucchiaio", "cucchiai") -> quantity * 15.0 // Peso medio standard 1 cucchiaio
            u in setOf("cucchiaino", "cucchiaini") -> quantity * 5.0 // Peso medio standard 1 cucchiaino
            u in setOf("pizzico", "pizzichi") -> quantity * 1.0 // Peso medio standard 1 pizzico
            gramsPerUnit != null && gramsPerUnit > 0.0 -> quantity * gramsPerUnit
            u in DISCRETE_CODES -> quantity * 50.0 // Default prudenziale per unità intera non calibrata
            else -> quantity // Fallback 1:1
        }
    }

    /**
     * Calcola il peso in grammi e i 6 valori nutrizionali per una determinata quantità di ingrediente
     * partendo dai suoi valori nutrizionali di riferimento per 100g.
     */
    fun calculateIngredientNutrients(item: Item, quantity: Double, unit: String): Pair<Double, NutritionalValues>{
        val grams = convertToGrams(quantity, unit, item.gramsPerUnit)
        if (grams <= 0.0) return Pair(0.0, NutritionalValues())

        val factor = grams / 100.0
        val nutrients = NutritionalValues(
            caloriesKcal = (item.caloriesKcal ?: 0.0) * factor,
            carbohydratesG = (item.carbohydratesG ?: 0.0) * factor,
            fatsG = (item.fatsG ?: 0.0) * factor,
            proteinsG = (item.proteinsG ?: 0.0) * factor,
            waterG = (item.waterG ?: 0.0) * factor,
            sodiumMg = (item.sodiumMg ?: 0.0) * factor
        )
        return Pair(grams, nutrients)
    }

    /**
     * Costruisce ricorsivamente l'albero delle dosi scalate della ricetta e di tutte le sotto-ricette annidate,
     * calcolando anche il Food Cost in tempo reale (costo componente, costo totale impasto e costo porzione)
     * e il profilo nutrizionale completo (totale, per 100g e a porzione) con modificatore di peso in cottura.
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
        var accumulatedRawWeight = 0.0
        var accumulatedNutrients = NutritionalValues()
        var hasAnyNutrition = false

        val scaledComponents = recipeWithComponents.components.map{ detail ->
            val comp = detail.component
            val safeBaseQty = if (comp.baseQuantity.isFinite() && comp.baseQuantity >= 0.0) comp.baseQuantity else 0.0
            val scaledQuantity = safeBaseQty * safeFactor
            val formatted = MeasurementUnit.formatAmount(scaledQuantity, comp.unit)

            var subTree: ScaledRecipeTree? = null
            var componentCost: Double? = null
            var compRawWeight = 0.0
            var compNutrients = NutritionalValues()

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

                    // Se la sotto-ricetta ha dati nutrizionali calcolati ricorsivamente:
                    if (calculatedSubTree.nutrition.hasNutritionData){
                        val fraction = if (calculatedSubTree.targetYieldQuantity > 0.0){
                            scaledQuantity / calculatedSubTree.targetYieldQuantity
                        } else 1.0
                        compNutrients = calculatedSubTree.nutrition.totalNutrients * fraction
                        // Il peso della sotto-ricetta che entra nella ricetta padre è il suo peso cotto finito proporzionale
                        compRawWeight = if (calculatedSubTree.nutrition.totalCookedWeightGrams > 0.0){
                            calculatedSubTree.nutrition.totalCookedWeightGrams * fraction
                        } else {
                            convertToGrams(scaledQuantity, comp.unit, detail.item.gramsPerUnit)
                        }
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

            // Se i valori nutrizionali non provengono da una sotto-ricetta ricorsiva, calcolali dall'Item
            if (!compNutrients.hasValues){
                val (g, n) = calculateIngredientNutrients(detail.item, scaledQuantity, comp.unit)
                compRawWeight = g
                compNutrients = n
            }

            if (componentCost != null && componentCost > 0.0){
                hasAnyCost = true
                accumulatedCost += componentCost
            }

            if (compNutrients.hasValues){
                hasAnyNutrition = true
            }

            accumulatedRawWeight += compRawWeight
            accumulatedNutrients = accumulatedNutrients + compNutrients

            ScaledComponent(
                componentDetail = detail,
                scaledQuantity = scaledQuantity,
                formattedScaledQuantity = formatted,
                subRecipeTree = subTree,
                componentCost = componentCost,
                rawWeightGrams = compRawWeight,
                nutrients = compNutrients
            )
        }

        val costPerPortion = if (targetYield > 0.0) accumulatedCost / targetYield else 0.0

        // --- Calcolo Nutrizionale con Modificatore Peso Cottura (Yield Loss / Gain) ---
        val cookingModifier = recipe.cookingWeightModifier
        // Moltiplicatore di resa ponderale in cottura (es. -15% -> 0.85; +120% -> 2.20; protetto contro valori <= 0)
        val cookingMultiplier = (1.0 + (cookingModifier / 100.0)).coerceAtLeast(0.01)
        val totalCookedWeightGrams = accumulatedRawWeight * cookingMultiplier

        // Adattamento dell'acqua nei nutrienti totali cotti:
        val cookedNutrients = when{
            cookingModifier < 0.0 -> {
                // Calo peso: perdita di vapore acqueo per evaporazione
                val waterLost = accumulatedRawWeight - totalCookedWeightGrams
                val remainingWater = (accumulatedNutrients.waterG - waterLost).coerceAtLeast(0.0)
                accumulatedNutrients.copy(waterG = remainingWater)
            }
            cookingModifier > 0.0 -> {
                // Aumento peso: assorbimento d'acqua in cottura (es. pasta o riso)
                val waterGained = totalCookedWeightGrams - accumulatedRawWeight
                val totalWater = accumulatedNutrients.waterG + waterGained
                accumulatedNutrients.copy(waterG = totalWater)
            }
            else -> accumulatedNutrients
        }

        // Calcolo per 100g di prodotto finito (concentrazione dei nutrienti post-cottura, a norma Reg. UE 1169/2011)
        val per100g = if (totalCookedWeightGrams > 0.0){
            val factor100 = 100.0 / totalCookedWeightGrams
            cookedNutrients * factor100
        } else{
            NutritionalValues()
        }

        // Calcolo per porzione e peso della singola porzione
        val isWeightYield = recipe.yieldUnit.trim().lowercase() in setOf("g", "kg", "grammi", "grammo", "chilogrammi", "chilo")
        val (perPortion, weightPerPortionGrams) = if (targetYield > 0.0){
            if (isWeightYield){
                // Se la resa base è espressa in grammi o kg, la porzione nutrizionale di riferimento standard è 100g
                Pair(per100g, 100.0)
            } else{
                // Resa a pezzi o porzioni (es. 10 cornetti, 4 porzioni): dividi per il numero totale di porzioni prodotte
                val portionFactor = 1.0 / targetYield
                val nutPortion = cookedNutrients * portionFactor
                val portionWeight = totalCookedWeightGrams / targetYield
                Pair(nutPortion, portionWeight)
            }
        } else{
            Pair(NutritionalValues(), 0.0)
        }

        val nutritionSummary = RecipeNutritionSummary(
            totalRawWeightGrams = accumulatedRawWeight,
            totalCookedWeightGrams = totalCookedWeightGrams,
            cookingWeightModifier = cookingModifier,
            totalNutrients = cookedNutrients,
            per100g = per100g,
            perPortion = perPortion,
            weightPerPortionGrams = weightPerPortionGrams,
            portionUnit = recipe.yieldUnit,
            targetYieldQuantity = targetYield,
            hasNutritionData = hasAnyNutrition
        )

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
            allergens = sortedAllergens,
            nutrition = nutritionSummary
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
