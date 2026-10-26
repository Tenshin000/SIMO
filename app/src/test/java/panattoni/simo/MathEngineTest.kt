package panattoni.simo

import panattoni.simo.data.model.Item
import panattoni.simo.data.model.ItemType
import panattoni.simo.data.model.Recipe
import panattoni.simo.data.model.RecipeComponent
import panattoni.simo.data.model.RecipeComponentDetail
import panattoni.simo.data.model.RecipeWithComponents
import panattoni.simo.domain.MathEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MathEngineTest {

    @Test
    fun testCornettoAllaCremaUseCases(){
        // Setup Item Crema
        val cremaItem = Item(id = "crema_id", name = "Crema Pasticcera", type = ItemType.RECIPE, defaultUnit = "g")
        val cremaRecipe = Recipe(
            id = "crema_id",
            name = "Crema Pasticcera",
            baseYieldQuantity = 600.0,
            yieldUnit = "g"
        )
        val cremaComponents = listOf(
            RecipeComponentDetail(
                component = RecipeComponent(parentRecipeId = "crema_id", childItemId = "latte_crema", baseQuantity = 500.0, unit = "ml"),
                item = Item(id = "latte_crema", name = "Latte Intero", type = ItemType.RAW_INGREDIENT, defaultUnit = "ml")
            ),
            RecipeComponentDetail(
                component = RecipeComponent(parentRecipeId = "crema_id", childItemId = "tuorli", baseQuantity = 4.0, unit = "pz"),
                item = Item(id = "tuorli", name = "Tuorli", type = ItemType.RAW_INGREDIENT, defaultUnit = "pz")
            ),
            RecipeComponentDetail(
                component = RecipeComponent(parentRecipeId = "crema_id", childItemId = "zucchero", baseQuantity = 120.0, unit = "g"),
                item = Item(id = "zucchero", name = "Zucchero", type = ItemType.RAW_INGREDIENT, defaultUnit = "g")
            ),
            RecipeComponentDetail(
                component = RecipeComponent(parentRecipeId = "crema_id", childItemId = "amido", baseQuantity = 40.0, unit = "g"),
                item = Item(id = "amido", name = "Amido di Mais", type = ItemType.RAW_INGREDIENT, defaultUnit = "g")
            )
        )
        val cremaWithComponents = RecipeWithComponents(cremaRecipe, cremaComponents)

        // Setup Cornetto Recipe
        val cornettoRecipe = Recipe(
            id = "cornetto_id",
            name = "Cornetto alla Crema",
            baseYieldQuantity = 12.0,
            yieldUnit = "cornetti"
        )
        val cornettoComponents = listOf(
            RecipeComponentDetail(
                component = RecipeComponent(parentRecipeId = "cornetto_id", childItemId = "farina_00", baseQuantity = 250.0, unit = "g"),
                item = Item(id = "farina_00", name = "Farina 00", type = ItemType.RAW_INGREDIENT, defaultUnit = "g")
            ),
            RecipeComponentDetail(
                component = RecipeComponent(parentRecipeId = "cornetto_id", childItemId = "latte_cornetto", baseQuantity = 180.0, unit = "ml"),
                item = Item(id = "latte_cornetto", name = "Latte", type = ItemType.RAW_INGREDIENT, defaultUnit = "ml")
            ),
            RecipeComponentDetail(
                component = RecipeComponent(parentRecipeId = "cornetto_id", childItemId = "crema_id", baseQuantity = 400.0, unit = "g"),
                item = cremaItem,
                subRecipe = cremaRecipe
            )
        )
        val cornettoWithComponents = RecipeWithComponents(cornettoRecipe, cornettoComponents)

        val allRecipes = mapOf(
            "crema_id" to cremaWithComponents,
            "cornetto_id" to cornettoWithComponents
        )

        // TEST 1: Reverse scaling su Farina 00 (ho 180g invece di 250g)
        val factorReverse = MathEngine.calculateScaleFactorByIngredient(baseAmount = 250.0, targetAmount = 180.0)
        assertEquals(0.72, factorReverse, 0.001)

        val scaledTreeReverse = MathEngine.buildScaledTree(cornettoWithComponents, factorReverse, allRecipes)
        // Cornetti ricalcolati
        assertEquals(8.64, scaledTreeReverse.targetYieldQuantity, 0.01)

        // Farina scalata a 180g
        val scaledFarina = scaledTreeReverse.components.first { it.componentDetail.item.id == "farina_00" }
        assertEquals(180.0, scaledFarina.scaledQuantity, 0.01)

        // Crema scalata a 400 * 0.72 = 288g
        val scaledCremaComp = scaledTreeReverse.components.first { it.componentDetail.item.id == "crema_id" }
        assertEquals(288.0, scaledCremaComp.scaledQuantity, 0.01)

        // Sotto-albero della crema ricalcolato per esattamente 288g!
        val cremaSubTree = scaledCremaComp.subRecipeTree
        assertTrue(cremaSubTree != null)
        assertEquals(288.0, cremaSubTree!!.targetYieldQuantity, 0.01)
        // Latte nella crema: 500 * (288 / 600) = 240 ml
        val latteInCrema = cremaSubTree.components.first { it.componentDetail.item.id == "latte_crema" }
        assertEquals(240.0, latteInCrema.scaledQuantity, 0.01)

        // TEST 2: Scaling per resa (Voglio fare 24 cornetti invece di 12)
        val factorYield = MathEngine.calculateScaleFactorByYield(baseYield = 12.0, targetYield = 24.0)
        assertEquals(2.0, factorYield, 0.001)

        val scaledTreeYield = MathEngine.buildScaledTree(cornettoWithComponents, factorYield, allRecipes)
        assertEquals(24.0, scaledTreeYield.targetYieldQuantity, 0.01)
        val cremaYieldComp = scaledTreeYield.components.first { it.componentDetail.item.id == "crema_id" }
        assertEquals(800.0, cremaYieldComp.scaledQuantity, 0.01)
        val cremaYieldSubTree = cremaYieldComp.subRecipeTree!!
        assertEquals(800.0, cremaYieldSubTree.targetYieldQuantity, 0.01)
        // Latte nella crema raddoppiato per 800g: 500 * (800 / 600) = 666.66 ml
        val latteDouble = cremaYieldSubTree.components.first { it.componentDetail.item.id == "latte_crema" }
        assertEquals(666.67, latteDouble.scaledQuantity, 0.1)
    }

    @Test
    fun testCycleDetectionDAG(){
        // Cornetto -> Crema
        val existingRelations = listOf("cornetto" to "crema")

        // Crema NON può contenere Cornetto (creerebbe ciclo)
        val createsCycle = MathEngine.wouldCreateCycle("crema", "cornetto", existingRelations)
        assertTrue(createsCycle)

        // Una ricetta non può contenere se stessa
        assertTrue(MathEngine.wouldCreateCycle("crema", "crema", existingRelations))

        // Altro elemento indipendente NON crea ciclo
        assertFalse(MathEngine.wouldCreateCycle("cornetto", "glassa", existingRelations))
        assertFalse(MathEngine.wouldCreateCycle("glassa", "zucchero_a_velo", existingRelations))
    }

    @Test
    fun testFoodCostCalculation(){
        // Farina: 250g con prezzo €1.20/kg -> €0.30
        val costFarina = MathEngine.calculateComponentCost(
            quantity = 250.0,
            recipeUnit = "g",
            pricePerUnit = 1.20,
            priceUnit = "kg"
        )
        assertEquals(0.30, costFarina!!, 0.001)

        // Burro: 175g con prezzo €8.00/kg -> €1.40
        val costBurro = MathEngine.calculateComponentCost(
            quantity = 175.0,
            recipeUnit = "g",
            pricePerUnit = 8.00,
            priceUnit = "kg"
        )
        assertEquals(1.40, costBurro!!, 0.001)

        // Uova: 2 unità a €0.30/unità -> €0.60
        val costUova = MathEngine.calculateComponentCost(
            quantity = 2.0,
            recipeUnit = "unità",
            pricePerUnit = 0.30,
            priceUnit = "unità"
        )
        assertEquals(0.60, costUova!!, 0.001)

        // Latte: 500 ml a €1.40/l -> €0.70
        val costLatte = MathEngine.calculateComponentCost(
            quantity = 500.0,
            recipeUnit = "ml",
            pricePerUnit = 1.40,
            priceUnit = "l"
        )
        assertEquals(0.70, costLatte!!, 0.001)

        // Test albero scalato con Food Cost
        val farinaItem = Item(id = "farina", name = "Farina", type = ItemType.RAW_INGREDIENT, defaultUnit = "g", pricePerUnit = 1.20, priceUnit = "kg")
        val burroItem = Item(id = "burro", name = "Burro", type = ItemType.RAW_INGREDIENT, defaultUnit = "g", pricePerUnit = 8.00, priceUnit = "kg")
        val biscottiRecipe = Recipe(id = "biscotti", name = "Biscotti", baseYieldQuantity = 10.0, yieldUnit = "biscotti")
        val biscottiWithComps = RecipeWithComponents(
            biscottiRecipe,
            listOf(
                RecipeComponentDetail(RecipeComponent(parentRecipeId = "biscotti", childItemId = "farina", baseQuantity = 250.0, unit = "g"), farinaItem),
                RecipeComponentDetail(RecipeComponent(parentRecipeId = "biscotti", childItemId = "burro", baseQuantity = 175.0, unit = "g"), burroItem)
            )
        )
        // Base: 250g farina (€0.30) + 175g burro (€1.40) = €1.70 per 10 biscotti -> €0.17 a biscotto
        val treeBase = MathEngine.buildScaledTree(biscottiWithComps, 1.0, emptyMap())
        assertTrue(treeBase.hasCostData)
        assertEquals(1.70, treeBase.totalCost, 0.001)
        assertEquals(0.17, treeBase.costPerPortion, 0.001)

        // Raddoppio a 20 biscotti (scalingFactor = 2.0): totale €3.40, a biscotto sempre €0.17
        val treeDouble = MathEngine.buildScaledTree(biscottiWithComps, 2.0, emptyMap())
        assertEquals(3.40, treeDouble.totalCost, 0.001)
        assertEquals(0.17, treeDouble.costPerPortion, 0.001)

        // Ingrediente in q.b. con prezzo unitario (es. Sale a €0.50/kg): nella ricetta il costo totale deve essere 0.0
        val costSaleQb = MathEngine.calculateComponentCost(
            quantity = 1.0,
            recipeUnit = "q.b.",
            pricePerUnit = 0.50,
            priceUnit = "kg"
        )
        assertEquals(0.0, costSaleQb!!, 0.001)

        val saleItem = Item(id = "sale", name = "Sale fino", type = ItemType.RAW_INGREDIENT, defaultUnit = "g", pricePerUnit = 0.50, priceUnit = "kg")
        val recipeWithSale = RecipeWithComponents(
            biscottiRecipe,
            listOf(
                RecipeComponentDetail(RecipeComponent(parentRecipeId = "biscotti", childItemId = "farina", baseQuantity = 250.0, unit = "g"), farinaItem),
                RecipeComponentDetail(RecipeComponent(parentRecipeId = "biscotti", childItemId = "sale", baseQuantity = 1.0, unit = "q.b."), saleItem)
            )
        )
        val treeWithSale = MathEngine.buildScaledTree(recipeWithSale, 1.0, emptyMap())
        assertEquals(0.0, treeWithSale.components.first{ it.componentDetail.item.id == "sale" }.componentCost!!, 0.001)
        assertEquals(0.30, treeWithSale.totalCost, 0.001)
    }
}
