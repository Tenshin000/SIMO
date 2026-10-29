package panattoni.simo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import panattoni.simo.data.model.Item
import panattoni.simo.data.model.ItemType
import panattoni.simo.data.model.Recipe
import panattoni.simo.data.model.RecipeComponent
import panattoni.simo.data.model.RecipeComponentDetail
import panattoni.simo.data.model.RecipeWithComponents
import panattoni.simo.domain.MathEngine

/**
 * Test di verifica per il motore di calcolo nutrizionale ricorsivo (MathEngine):
 * - Conversione ponderale per tutte le unità di misura (g, kg, ml, l, pz, cucchiai, cucchiaini, pizzichi, qb)
 * - Calcolo macronutrienti per materie prime
 * - Concentrazione nutrienti post-cottura per calo peso (Yield Loss / Shrinkage)
 * - Diluizione nutrienti per idratazione/aumento peso (pasta/riso)
 * - Calcolo ricorsivo su grafi ad albero con sotto-ricette annidate (DAG)
 * - Dichiarazione nutrizionale UE: calcolo per 100g, per porzione e sale equivalente
 */
class NutritionMathEngineTest{

    @Test
    fun testConvertToGramsForAllUnits(){
        // Grammi
        assertEquals(250.0, MathEngine.convertToGrams(250.0, "g"), 0.001)
        assertEquals(100.0, MathEngine.convertToGrams(100.0, "grammi"), 0.001)

        // Chilogrammi
        assertEquals(1500.0, MathEngine.convertToGrams(1.5, "kg"), 0.001)
        assertEquals(2000.0, MathEngine.convertToGrams(2.0, "chilogrammi"), 0.001)

        // Millilitri (densità cucina standard ~1.0 g/ml)
        assertEquals(300.0, MathEngine.convertToGrams(300.0, "ml"), 0.001)

        // Litri
        assertEquals(1000.0, MathEngine.convertToGrams(1.0, "l"), 0.001)

        // Cucchiai (15g ciascuno)
        assertEquals(30.0, MathEngine.convertToGrams(2.0, "cucchiai"), 0.001)
        assertEquals(15.0, MathEngine.convertToGrams(1.0, "cucchiaio"), 0.001)

        // Cucchiaini (5g ciascuno)
        assertEquals(15.0, MathEngine.convertToGrams(3.0, "cucchiaini"), 0.001)

        // Pizzichi (1g ciascuno)
        assertEquals(2.0, MathEngine.convertToGrams(2.0, "pizzichi"), 0.001)

        // Quanto basta (0g)
        assertEquals(0.0, MathEngine.convertToGrams(1.0, "q.b."), 0.001)

        // Unità discrete con gramsPerUnit (es. 3 uova da 50g)
        assertEquals(150.0, MathEngine.convertToGrams(3.0, "unità", gramsPerUnit = 50.0), 0.001)
        assertEquals(40.0, MathEngine.convertToGrams(2.0, "pz", gramsPerUnit = 20.0), 0.001)

        // Unità discrete senza gramsPerUnit (default prudenziale 50g)
        assertEquals(100.0, MathEngine.convertToGrams(2.0, "pezzi"), 0.001)
    }

    @Test
    fun testCalculateIngredientNutrients(){
        val farina = Item(
            id = "farina",
            name = "Farina 00",
            type = ItemType.RAW_INGREDIENT,
            defaultUnit = "g",
            caloriesKcal = 364.0,
            carbohydratesG = 76.3,
            fatsG = 1.0,
            proteinsG = 10.3,
            waterG = 14.0,
            sodiumMg = 2.0
        )

        // 200g di farina -> nutrienti esattamente raddoppiati
        val (grams, nutrients) = MathEngine.calculateIngredientNutrients(farina, 200.0, "g")
        assertEquals(200.0, grams, 0.001)
        assertEquals(728.0, nutrients.caloriesKcal, 0.01)
        assertEquals(152.6, nutrients.carbohydratesG, 0.01)
        assertEquals(2.0, nutrients.fatsG, 0.01)
        assertEquals(20.6, nutrients.proteinsG, 0.01)
        assertEquals(28.0, nutrients.waterG, 0.01)
        assertEquals(4.0, nutrients.sodiumMg, 0.01)
        // Sale equivalente UE = (4.0 / 1000) * 2.5 = 0.01g
        assertEquals(0.01, nutrients.saltG, 0.001)
    }

    @Test
    fun testCookingShrinkageAndNutrientConcentration(){
        // Test Pane Casereccio: impasto crudo da 1000g, calo peso del 20% in cottura (800g cotto)
        val farina = Item(
            id = "farina_pane",
            name = "Farina",
            type = ItemType.RAW_INGREDIENT,
            defaultUnit = "g",
            caloriesKcal = 360.0,
            carbohydratesG = 75.0,
            fatsG = 1.0,
            proteinsG = 10.0,
            waterG = 14.0,
            sodiumMg = 2.0
        )
        val acqua = Item(
            id = "acqua_pane",
            name = "Acqua",
            type = ItemType.RAW_INGREDIENT,
            defaultUnit = "ml",
            caloriesKcal = 0.0,
            carbohydratesG = 0.0,
            fatsG = 0.0,
            proteinsG = 0.0,
            waterG = 100.0,
            sodiumMg = 5.0
        )

        val recipe = Recipe(
            id = "recipe_pane",
            name = "Pane Casereccio",
            baseYieldQuantity = 1.0,
            yieldUnit = "pagnotta",
            cookingWeightModifier = -20.0 // Calo peso del 20% in forno
        )

        // 600g farina + 400ml acqua = 1000g crudi
        val components = listOf(
            RecipeComponentDetail(
                component = RecipeComponent(parentRecipeId = "recipe_pane", childItemId = "farina_pane", baseQuantity = 600.0, unit = "g"),
                item = farina
            ),
            RecipeComponentDetail(
                component = RecipeComponent(parentRecipeId = "recipe_pane", childItemId = "acqua_pane", baseQuantity = 400.0, unit = "ml"),
                item = acqua
            )
        )

        val tree = MathEngine.buildScaledTree(
            recipeWithComponents = RecipeWithComponents(recipe, components),
            scalingFactor = 1.0,
            allRecipesMap = emptyMap()
        )

        val n = tree.nutrition
        assertTrue(n.hasNutritionData)
        // Peso crudo = 600 + 400 = 1000g
        assertEquals(1000.0, n.totalRawWeightGrams, 0.01)
        // Peso cotto = 1000 * (1 - 0.20) = 800g
        assertEquals(800.0, n.totalCookedWeightGrams, 0.01)

        // Calorie totali impasto = 600g farina * 3.6 = 2160 kcal
        assertEquals(2160.0, n.totalNutrients.caloriesKcal, 0.01)
        // Carboidrati totali = 600g farina * 0.75 = 450g
        assertEquals(450.0, n.totalNutrients.carbohydratesG, 0.01)

        // CONCENTRAZIONE PER 100G:
        // A crudo le calorie per 100g sarebbero state 2160 / 1000 * 100 = 216 kcal
        // A cotto (800g), le calorie per 100g si concentrano: 2160 / 800 * 100 = 270 kcal/100g!
        assertEquals(270.0, n.per100g.caloriesKcal, 0.01)
        // Carboidrati per 100g: 450 / 800 * 100 = 56.25 g/100g!
        assertEquals(56.25, n.per100g.carbohydratesG, 0.01)

        // Acqua: a crudo c'erano (600*0.14) + (400*1.0) = 84 + 400 = 484g
        // Con calo di 200g in cottura, acqua cotta residua = 484 - 200 = 284g
        assertEquals(284.0, n.totalNutrients.waterG, 0.01)
        // Acqua per 100g = 284 / 800 * 100 = 35.5 g/100g
        assertEquals(35.5, n.per100g.waterG, 0.01)
    }

    @Test
    fun testPastaCookingHydrationGain(){
        // Test Pasta: 100g pasta cruda con aumento di peso in cottura del +120% (idratazione)
        val pasta = Item(
            id = "pasta_secca",
            name = "Pasta di Semola",
            type = ItemType.RAW_INGREDIENT,
            defaultUnit = "g",
            caloriesKcal = 350.0,
            carbohydratesG = 70.0,
            fatsG = 1.5,
            proteinsG = 12.0,
            waterG = 12.5,
            sodiumMg = 5.0
        )

        val recipe = Recipe(
            id = "recipe_spaghetti",
            name = "Spaghetti Cotti",
            baseYieldQuantity = 1.0,
            yieldUnit = "piatto",
            cookingWeightModifier = 120.0 // +120% per assorbimento acqua
        )

        val components = listOf(
            RecipeComponentDetail(
                component = RecipeComponent(parentRecipeId = "recipe_spaghetti", childItemId = "pasta_secca", baseQuantity = 100.0, unit = "g"),
                item = pasta
            )
        )

        val tree = MathEngine.buildScaledTree(
            recipeWithComponents = RecipeWithComponents(recipe, components),
            scalingFactor = 1.0,
            allRecipesMap = emptyMap()
        )

        val n = tree.nutrition
        assertEquals(100.0, n.totalRawWeightGrams, 0.01)
        // Peso cotto = 100 * (1 + 1.20) = 220g
        assertEquals(220.0, n.totalCookedWeightGrams, 0.01)

        // Calorie totali conservate = 350 kcal
        assertEquals(350.0, n.totalNutrients.caloriesKcal, 0.01)

        // DILUIZIONE PER 100G COTTO:
        // Calorie per 100g cotto = 350 / 220 * 100 = 159.09 kcal/100g
        assertEquals(159.09, n.per100g.caloriesKcal, 0.1)

        // Per porzione (1 piatto):
        assertEquals(350.0, n.perPortion.caloriesKcal, 0.01)
        assertEquals(220.0, n.weightPerPortionGrams, 0.01)
    }

    @Test
    fun testRecursiveSubRecipeNutritionalCalculation(){
        // Sotto-ricetta: Crema Pasticcera (Resa 500g)
        val latte = Item(
            id = "latte", name = "Latte", type = ItemType.RAW_INGREDIENT, defaultUnit = "ml",
            caloriesKcal = 64.0, carbohydratesG = 4.8, fatsG = 3.6, proteinsG = 3.3, waterG = 87.5, sodiumMg = 44.0
        )
        val zucchero = Item(
            id = "zucchero", name = "Zucchero", type = ItemType.RAW_INGREDIENT, defaultUnit = "g",
            caloriesKcal = 387.0, carbohydratesG = 99.9, fatsG = 0.0, proteinsG = 0.0, waterG = 0.1, sodiumMg = 1.0
        )

        val cremaRecipe = Recipe(
            id = "crema",
            name = "Crema Pasticcera",
            baseYieldQuantity = 500.0,
            yieldUnit = "g",
            cookingWeightModifier = 0.0
        )
        val cremaItem = Item(id = "crema", name = "Crema Pasticcera", type = ItemType.RECIPE, defaultUnit = "g")

        // 400ml latte (256 kcal, 19.2g carb, 14.4g fat, 13.2g prot) + 100g zucchero (387 kcal, 99.9g carb) = 500g
        // Totali crema = 643 kcal, 119.1g carb, 14.4g fat, 13.2g prot
        val cremaComponents = listOf(
            RecipeComponentDetail(
                component = RecipeComponent(parentRecipeId = "crema", childItemId = "latte", baseQuantity = 400.0, unit = "ml"),
                item = latte
            ),
            RecipeComponentDetail(
                component = RecipeComponent(parentRecipeId = "crema", childItemId = "zucchero", baseQuantity = 100.0, unit = "g"),
                item = zucchero
            )
        )
        val cremaWithComponents = RecipeWithComponents(cremaRecipe, cremaComponents)

        // Ricetta Padre: Cornetti alla Crema (Resa 10 cornetti)
        // Usa 250g di Crema (esattamente metà della dose base della crema, quindi 50% dei suoi nutrienti!)
        val farina = Item(
            id = "farina", name = "Farina", type = ItemType.RAW_INGREDIENT, defaultUnit = "g",
            caloriesKcal = 360.0, carbohydratesG = 75.0, fatsG = 1.0, proteinsG = 10.0, waterG = 14.0, sodiumMg = 2.0
        )
        val cornettiRecipe = Recipe(
            id = "cornetti",
            name = "Cornetti alla Crema",
            baseYieldQuantity = 10.0,
            yieldUnit = "cornetti",
            cookingWeightModifier = -10.0 // Calo 10% in cottura
        )
        // 500g farina (1800 kcal) + 250g crema (321.5 kcal)
        val cornettiComponents = listOf(
            RecipeComponentDetail(
                component = RecipeComponent(parentRecipeId = "cornetti", childItemId = "farina", baseQuantity = 500.0, unit = "g"),
                item = farina
            ),
            RecipeComponentDetail(
                component = RecipeComponent(parentRecipeId = "cornetti", childItemId = "crema", baseQuantity = 250.0, unit = "g"),
                item = cremaItem,
                subRecipe = cremaRecipe
            )
        )
        val cornettiWithComponents = RecipeWithComponents(cornettiRecipe, cornettiComponents)

        val allRecipes = mapOf(
            "crema" to cremaWithComponents,
            "cornetti" to cornettiWithComponents
        )

        val tree = MathEngine.buildScaledTree(
            recipeWithComponents = cornettiWithComponents,
            scalingFactor = 1.0,
            allRecipesMap = allRecipes
        )

        val n = tree.nutrition
        assertTrue(n.hasNutritionData)

        // Peso crudo = 500g farina + 250g crema = 750g
        assertEquals(750.0, n.totalRawWeightGrams, 0.01)
        // Peso cotto = 750 * 0.90 = 675g
        assertEquals(675.0, n.totalCookedWeightGrams, 0.01)

        // Calorie farina = 500 * 3.6 = 1800 kcal
        // Calorie crema = 643 * (250/500) = 321.5 kcal
        // Totale calorie cornetti = 1800 + 321.5 = 2121.5 kcal
        assertEquals(2121.5, n.totalNutrients.caloriesKcal, 0.1)

        // Calorie per Porzione (1 cornetto sui 10 prodotti):
        assertEquals(212.15, n.perPortion.caloriesKcal, 0.1)
        // Peso di 1 cornetto cotto = 675g / 10 = 67.5g
        assertEquals(67.5, n.weightPerPortionGrams, 0.01)

        // Calorie per 100g prodotto finito:
        // 2121.5 / 675 * 100 = 314.3 kcal/100g
        assertEquals(314.3, n.per100g.caloriesKcal, 0.2)
    }
}
