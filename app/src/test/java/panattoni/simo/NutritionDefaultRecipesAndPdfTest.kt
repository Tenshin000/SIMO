package panattoni.simo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import panattoni.simo.data.local.PrepopulateData
import panattoni.simo.data.model.Item
import panattoni.simo.data.model.ItemType
import panattoni.simo.data.model.Recipe
import panattoni.simo.data.model.RecipeComponent
import panattoni.simo.data.model.RecipeComponentDetail
import panattoni.simo.data.model.RecipeWithComponents
import panattoni.simo.domain.MathEngine
import panattoni.simo.util.NumberUtils

/**
 * Test unitari di verifica per la Fase 4:
 * - Dati nutrizionali e modificatori peso in cottura delle 3 ricette di default (Cornetto, Crema, Pizza)
 * - Ricalcolo dinamico della resa in cottura e concentrazione nutrienti
 * - Formattazione pesi ed etichetta nutrizionale UE
 */
class NutritionDefaultRecipesAndPdfTest{

    @Test
    fun testDefaultRecipesHaveCookingWeightModifiers(){
        val crema = PrepopulateData.DEFAULT_RECIPES.first{ it.id == PrepopulateData.ID_RECIPE_CREMA }
        val cornetto = PrepopulateData.DEFAULT_RECIPES.first{ it.id == PrepopulateData.ID_RECIPE_CORNETTO }
        val pizza = PrepopulateData.DEFAULT_RECIPES.first{ it.id == PrepopulateData.ID_RECIPE_PIZZA }

        // Crema ha calo dell'8% (-8.0) per evaporazione a fuoco dolce
        assertEquals(-8.0, crema.cookingWeightModifier, 0.001)
        // Cornetto ha calo del 12% (-12.0) in cottura forno
        assertEquals(-12.0, cornetto.cookingWeightModifier, 0.001)
        // Pizza ha calo del 12% (-12.0) in forno pizza
        assertEquals(-12.0, pizza.cookingWeightModifier, 0.001)
    }

    @Test
    fun testDefaultItemsHaveCompleteNutritionalValues(){
        val latte = PrepopulateData.DEFAULT_ITEMS.first{ it.id == "ing_latte" }
        assertNotNull(latte.caloriesKcal)
        assertEquals(64.0, latte.caloriesKcal!!, 0.01)
        assertEquals(4.8, latte.carbohydratesG!!, 0.01)
        assertEquals(3.6, latte.fatsG!!, 0.01)
        assertEquals(3.3, latte.proteinsG!!, 0.01)
        assertEquals(87.5, latte.waterG!!, 0.01)
        assertEquals(44.0, latte.sodiumMg!!, 0.01)

        val farinaManitoba = PrepopulateData.DEFAULT_ITEMS.first{ it.id == "ing_farina_manitoba" }
        assertEquals(364.0, farinaManitoba.caloriesKcal!!, 0.01)
        assertEquals(72.0, farinaManitoba.carbohydratesG!!, 0.01)
        assertEquals(14.0, farinaManitoba.proteinsG!!, 0.01)

        val sale = PrepopulateData.DEFAULT_ITEMS.first{ it.id == "ing_sale" }
        assertEquals(0.0, sale.caloriesKcal!!, 0.01)
        assertEquals(38758.0, sale.sodiumMg!!, 0.01)
    }

    @Test
    fun testCremaPasticceraDynamicNutritionCalculation(){
        val cremaEntity = PrepopulateData.DEFAULT_RECIPES.first{ it.id == PrepopulateData.ID_RECIPE_CREMA }
        val cremaRecipe = Recipe(
            id = cremaEntity.id,
            name = cremaEntity.name,
            description = cremaEntity.description,
            category = cremaEntity.category,
            baseYieldQuantity = cremaEntity.baseYieldQuantity,
            yieldUnit = cremaEntity.yieldUnit,
            instructions = cremaEntity.instructions,
            isDefault = true,
            createdAt = cremaEntity.createdAt,
            cookingWeightModifier = cremaEntity.cookingWeightModifier
        )

        val itemsMap = PrepopulateData.DEFAULT_ITEMS.associateBy{ it.id }.mapValues{ (_, entity) ->
            Item(
                id = entity.id,
                name = entity.name,
                type = ItemType.valueOf(entity.type),
                defaultUnit = entity.defaultUnit,
                pricePerUnit = entity.pricePerUnit,
                priceUnit = entity.priceUnit,
                gramsPerUnit = entity.gramsPerUnit,
                allergens = entity.allergens,
                caloriesKcal = entity.caloriesKcal,
                carbohydratesG = entity.carbohydratesG,
                fatsG = entity.fatsG,
                proteinsG = entity.proteinsG,
                waterG = entity.waterG,
                sodiumMg = entity.sodiumMg
            )
        }

        val cremaComponents = PrepopulateData.DEFAULT_COMPONENTS
            .filter{ it.parentRecipeId == cremaRecipe.id }
            .map{ compEntity ->
                val item = itemsMap[compEntity.childItemId]!!
                RecipeComponentDetail(
                    component = RecipeComponent(
                        id = 0L,
                        parentRecipeId = compEntity.parentRecipeId,
                        childItemId = compEntity.childItemId,
                        baseQuantity = compEntity.baseQuantity,
                        unit = compEntity.unit,
                        sortOrder = compEntity.sortOrder
                    ),
                    item = item
                )
            }

        val tree = MathEngine.buildScaledTree(
            recipeWithComponents = RecipeWithComponents(cremaRecipe, cremaComponents),
            scalingFactor = 1.0,
            allRecipesMap = emptyMap()
        )

        val nutrition = tree.nutrition
        assertTrue("La crema pasticcera deve avere dati nutrizionali", nutrition.hasNutritionData)
        assertTrue("Il peso crudo totale deve essere maggiore di 500g", nutrition.totalRawWeightGrams > 500.0)

        // Con calo peso -8%, il peso cotto deve essere il 92% del peso crudo
        val expectedCooked = nutrition.totalRawWeightGrams * 0.92
        assertEquals(expectedCooked, nutrition.totalCookedWeightGrams, 0.5)

        // Valori per 100g devono essere realistici per una crema pasticcera (130-180 kcal/100g)
        assertTrue("Calorie per 100g tra 120 e 200 kcal", nutrition.per100g.caloriesKcal in 120.0..200.0)
        assertTrue("Carboidrati per 100g tra 15 e 30 g", nutrition.per100g.carbohydratesG in 15.0..30.0)
        assertTrue("Grassi per 100g tra 3 e 10 g", nutrition.per100g.fatsG in 3.0..10.0)
        assertTrue("Sale equivalente calcolato correttamente", nutrition.per100g.saltG > 0.0)
        assertEquals(nutrition.per100g.saltG, nutrition.per100g.saltEquivalentG, 0.001)
        assertEquals(nutrition.per100g.caloriesKcal * 4.184, nutrition.per100g.energyKj, 0.01)
    }

    @Test
    fun testCornettoWithSubRecipeTreeNutrition(){
        val cremaEntity = PrepopulateData.DEFAULT_RECIPES.first{ it.id == PrepopulateData.ID_RECIPE_CREMA }
        val cornettoEntity = PrepopulateData.DEFAULT_RECIPES.first{ it.id == PrepopulateData.ID_RECIPE_CORNETTO }

        val itemsMap = PrepopulateData.DEFAULT_ITEMS.associateBy{ it.id }.mapValues{ (_, entity) ->
            Item(
                id = entity.id,
                name = entity.name,
                type = ItemType.valueOf(entity.type),
                defaultUnit = entity.defaultUnit,
                pricePerUnit = entity.pricePerUnit,
                priceUnit = entity.priceUnit,
                gramsPerUnit = entity.gramsPerUnit,
                allergens = entity.allergens,
                caloriesKcal = entity.caloriesKcal,
                carbohydratesG = entity.carbohydratesG,
                fatsG = entity.fatsG,
                proteinsG = entity.proteinsG,
                waterG = entity.waterG,
                sodiumMg = entity.sodiumMg
            )
        }

        fun resolveRecipe(id: String): RecipeWithComponents?{
            val recEntity = PrepopulateData.DEFAULT_RECIPES.firstOrNull{ it.id == id } ?: return null
            val rec = Recipe(
                id = recEntity.id,
                name = recEntity.name,
                description = recEntity.description,
                category = recEntity.category,
                baseYieldQuantity = recEntity.baseYieldQuantity,
                yieldUnit = recEntity.yieldUnit,
                instructions = recEntity.instructions,
                isDefault = true,
                createdAt = recEntity.createdAt,
                cookingWeightModifier = recEntity.cookingWeightModifier
            )
            val comps = PrepopulateData.DEFAULT_COMPONENTS.filter{ it.parentRecipeId == id }.map{ c ->
                RecipeComponentDetail(
                    component = RecipeComponent(
                        id = 0L,
                        parentRecipeId = c.parentRecipeId,
                        childItemId = c.childItemId,
                        baseQuantity = c.baseQuantity,
                        unit = c.unit,
                        sortOrder = c.sortOrder
                    ),
                    item = itemsMap[c.childItemId]!!
                )
            }
            return RecipeWithComponents(rec, comps)
        }

        val cornettoRecipeWithComponents = resolveRecipe(cornettoEntity.id)!!
        val cremaRecipeWithComponents = resolveRecipe(cremaEntity.id)!!

        val allRecipesMap = mapOf(
            cremaEntity.id to cremaRecipeWithComponents,
            cornettoEntity.id to cornettoRecipeWithComponents
        )

        val tree = MathEngine.buildScaledTree(
            recipeWithComponents = cornettoRecipeWithComponents,
            scalingFactor = 1.0,
            allRecipesMap = allRecipesMap
        )

        val nutrition = tree.nutrition
        assertTrue("Il cornetto deve avere valori nutrizionali calcolati ricorsivamente", nutrition.hasNutritionData)
        // Cornetto ha calo cottura del 12% (-12.0)
        assertEquals(-12.0, nutrition.cookingWeightModifier, 0.001)
        assertTrue("Peso a porzione (singolo cornetto) compreso tra 80g e 140g", nutrition.weightPerPortionGrams in 80.0..140.0)
        // Calorie per singolo cornetto farcito alla crema
        assertTrue("Calorie singolo cornetto alla crema tra 250 e 450 kcal", nutrition.perPortion.caloriesKcal in 250.0..450.0)
    }

    @Test
    fun testPizzaMargheritaRealisticWeight(){
        val pizzaEntity = PrepopulateData.DEFAULT_RECIPES.first{ it.id == PrepopulateData.ID_RECIPE_PIZZA }
        val itemsMap = PrepopulateData.DEFAULT_ITEMS.associateBy{ it.id }.mapValues{ (_, entity) ->
            Item(
                id = entity.id,
                name = entity.name,
                type = ItemType.valueOf(entity.type),
                defaultUnit = entity.defaultUnit,
                pricePerUnit = entity.pricePerUnit,
                priceUnit = entity.priceUnit,
                gramsPerUnit = entity.gramsPerUnit,
                allergens = entity.allergens,
                caloriesKcal = entity.caloriesKcal,
                carbohydratesG = entity.carbohydratesG,
                fatsG = entity.fatsG,
                proteinsG = entity.proteinsG,
                waterG = entity.waterG,
                sodiumMg = entity.sodiumMg
            )
        }

        val pizzaRecipe = Recipe(
            id = pizzaEntity.id,
            name = pizzaEntity.name,
            description = pizzaEntity.description,
            category = pizzaEntity.category,
            baseYieldQuantity = pizzaEntity.baseYieldQuantity,
            yieldUnit = pizzaEntity.yieldUnit,
            instructions = pizzaEntity.instructions,
            isDefault = true,
            createdAt = pizzaEntity.createdAt,
            cookingWeightModifier = pizzaEntity.cookingWeightModifier
        )

        val pizzaComponents = PrepopulateData.DEFAULT_COMPONENTS
            .filter{ it.parentRecipeId == pizzaRecipe.id }
            .map{ compEntity ->
                val item = itemsMap[compEntity.childItemId]!!
                RecipeComponentDetail(
                    component = RecipeComponent(
                        id = 0L,
                        parentRecipeId = compEntity.parentRecipeId,
                        childItemId = compEntity.childItemId,
                        baseQuantity = compEntity.baseQuantity,
                        unit = compEntity.unit,
                        sortOrder = compEntity.sortOrder
                    ),
                    item = item
                )
            }

        val tree = MathEngine.buildScaledTree(
            recipeWithComponents = RecipeWithComponents(pizzaRecipe, pizzaComponents),
            scalingFactor = 1.0,
            allRecipesMap = emptyMap()
        )

        val nutrition = tree.nutrition
        assertTrue("La pizza deve avere dati nutrizionali", nutrition.hasNutritionData)
        // Peso crudo totale pizza: circa 455g (panetto ~275g + pomodoro 90g + mozzarella 90g)
        assertTrue("Peso crudo totale realistico tra 430g e 480g (era ~800g)", nutrition.totalRawWeightGrams in 430.0..480.0)
        // Peso cotto finito con calo cottura -12%: circa 400g
        assertTrue("Peso cotto finito esattamente attorno a 400g", nutrition.totalCookedWeightGrams in 380.0..420.0)
        assertTrue("Peso a porzione esattamente attorno a 400g", nutrition.weightPerPortionGrams in 380.0..420.0)
    }

    @Test
    fun testFormatWeightHelper(){
        assertEquals("0 g", NumberUtils.formatWeight(0.0))
        assertEquals("250 g", NumberUtils.formatWeight(250.0))
        assertEquals("999 g", NumberUtils.formatWeight(999.0))
        assertEquals("1 kg", NumberUtils.formatWeight(1000.0))
        assertEquals("1.25 kg", NumberUtils.formatWeight(1250.0))
        assertEquals("2.5 kg", NumberUtils.formatWeight(2500.0))
    }
}
