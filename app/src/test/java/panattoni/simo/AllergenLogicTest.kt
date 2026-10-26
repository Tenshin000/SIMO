package panattoni.simo

import panattoni.simo.data.model.Item
import panattoni.simo.data.model.ItemType
import panattoni.simo.data.model.Recipe
import panattoni.simo.data.model.RecipeComponent
import panattoni.simo.data.model.RecipeComponentDetail
import panattoni.simo.data.model.RecipeWithComponents
import panattoni.simo.domain.Allergen
import panattoni.simo.domain.MathEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AllergenLogicTest {

    @Test
    fun testAll14OfficialAllergensPresent(){
        assertEquals("Devono esserci esattamente i 14 allergeni ufficiali UE", 14, Allergen.entries.size)

        val expectedIds = setOf(
            "GLUTINE", "CROSTACEI", "UOVA", "PESCE", "ARACHIDI",
            "SOIA", "LATTE", "FRUTTA_A_GUSCIO", "SEDANO", "SENAPE",
            "SEMI_DI_SESAMO", "SOLFITI", "LUPINI", "MOLLUSCHI"
        )
        val actualIds = Allergen.entries.map { it.id }.toSet()
        assertEquals(expectedIds, actualIds)

        for (allergen in Allergen.entries){
            assertTrue("Il nome visualizzato non deve essere vuoto", allergen.displayName.isNotBlank())
            assertTrue("La descrizione legale non deve essere vuota", allergen.description.isNotBlank())
            assertTrue("L'emoji non deve essere vuota", allergen.emoji.isNotBlank())
            assertNotNull(Allergen.fromId(allergen.id))
        }
    }

    @Test
    fun testAllergenParsingAndFormatting(){
        val parsed = Allergen.parseAllergensString("GLUTINE, LATTE , uova, non_esistente")
        assertEquals(listOf(Allergen.GLUTINE, Allergen.LATTE, Allergen.UOVA), parsed)

        val formatted = Allergen.formatAllergensString(listOf(Allergen.GLUTINE, Allergen.LATTE))
        assertEquals("GLUTINE,LATTE", formatted)

        assertTrue(Allergen.parseAllergensString("").isEmpty())
        assertTrue(Allergen.parseAllergensString(null).isEmpty())
        assertTrue(Allergen.parseAllergensString("   ").isEmpty())
    }

    @Test
    fun testCascadingAllergensThroughSubRecipes(){
        // 1. Sotto-ricetta: Crema Pasticcera (Latte -> LATTE, Tuorli -> UOVA, Zucchero -> nessun allergene)
        val itemLatte = Item(id = "latte", name = "Latte intero", type = ItemType.RAW_INGREDIENT, defaultUnit = "ml", allergens = "LATTE")
        val itemTuorli = Item(id = "tuorli", name = "Tuorli d'uovo", type = ItemType.RAW_INGREDIENT, defaultUnit = "pz", allergens = "UOVA")
        val itemZucchero = Item(id = "zucchero", name = "Zucchero", type = ItemType.RAW_INGREDIENT, defaultUnit = "g", allergens = "")

        val cremaRecipe = Recipe(id = "recipe_crema", name = "Crema Pasticcera", baseYieldQuantity = 500.0, yieldUnit = "g")
        val cremaWithComps = RecipeWithComponents(
            recipe = cremaRecipe,
            components = listOf(
                RecipeComponentDetail(RecipeComponent(parentRecipeId = "recipe_crema", childItemId = "latte", baseQuantity = 500.0, unit = "ml"), itemLatte),
                RecipeComponentDetail(RecipeComponent(parentRecipeId = "recipe_crema", childItemId = "tuorli", baseQuantity = 4.0, unit = "pz"), itemTuorli),
                RecipeComponentDetail(RecipeComponent(parentRecipeId = "recipe_crema", childItemId = "zucchero", baseQuantity = 100.0, unit = "g"), itemZucchero)
            )
        )

        // 2. Ricetta Genitore: Cornetto alla Crema (Farina -> GLUTINE, Burro -> LATTE, e Sotto-ricetta Crema)
        val itemFarina = Item(id = "farina", name = "Farina 00", type = ItemType.RAW_INGREDIENT, defaultUnit = "g", allergens = "GLUTINE")
        val itemBurro = Item(id = "burro", name = "Burro", type = ItemType.RAW_INGREDIENT, defaultUnit = "g", allergens = "LATTE")
        val itemCremaAsRecipe = Item(id = "recipe_crema", name = "Crema Pasticcera", type = ItemType.RECIPE, defaultUnit = "g")

        val cornettoRecipe = Recipe(id = "recipe_cornetto", name = "Cornetto alla Crema", baseYieldQuantity = 10.0, yieldUnit = "cornetti")
        val cornettoWithComps = RecipeWithComponents(
            recipe = cornettoRecipe,
            components = listOf(
                RecipeComponentDetail(RecipeComponent(parentRecipeId = "recipe_cornetto", childItemId = "farina", baseQuantity = 300.0, unit = "g"), itemFarina),
                RecipeComponentDetail(RecipeComponent(parentRecipeId = "recipe_cornetto", childItemId = "burro", baseQuantity = 150.0, unit = "g"), itemBurro),
                RecipeComponentDetail(RecipeComponent(parentRecipeId = "recipe_cornetto", childItemId = "recipe_crema", baseQuantity = 200.0, unit = "g"), itemCremaAsRecipe, subRecipe = cremaRecipe)
            )
        )

        val allRecipesMap = mapOf(
            "recipe_crema" to cremaWithComps,
            "recipe_cornetto" to cornettoWithComps
        )

        // Costruzione albero per Crema
        val cremaTree = MathEngine.buildScaledTree(cremaWithComps, 1.0, allRecipesMap)
        assertEquals(2, cremaTree.allergens.size)
        assertTrue("La crema deve contenere UOVA", cremaTree.allergens.contains(Allergen.UOVA))
        assertTrue("La crema deve contenere LATTE", cremaTree.allergens.contains(Allergen.LATTE))
        assertFalse("La crema NON contiene GLUTINE", cremaTree.allergens.contains(Allergen.GLUTINE))

        // Costruzione albero per Cornetto: riceve a cascata gli allergeni della Crema (LATTE, UOVA) + ingredienti propri (GLUTINE, LATTE)
        val cornettoTree = MathEngine.buildScaledTree(cornettoWithComps, 1.0, allRecipesMap)
        assertEquals("Il cornetto deve ereditare a cascata GLUTINE, UOVA, LATTE (distinti)", 3, cornettoTree.allergens.size)
        assertTrue("Il cornetto contiene GLUTINE", cornettoTree.allergens.contains(Allergen.GLUTINE))
        assertTrue("Il cornetto eredita UOVA dalla crema", cornettoTree.allergens.contains(Allergen.UOVA))
        assertTrue("Il cornetto contiene LATTE (da burro e crema)", cornettoTree.allergens.contains(Allergen.LATTE))
    }

    @Test
    fun testRecipeWithoutAllergens(){
        val itemAcqua = Item(id = "acqua", name = "Acqua", type = ItemType.RAW_INGREDIENT, defaultUnit = "ml", allergens = "")
        val itemZucchero = Item(id = "zucchero", name = "Zucchero", type = ItemType.RAW_INGREDIENT, defaultUnit = "g", allergens = "")

        val sciroppoRecipe = Recipe(id = "sciroppo", name = "Sciroppo di Zucchero", baseYieldQuantity = 500.0, yieldUnit = "ml")
        val sciroppoWithComps = RecipeWithComponents(
            recipe = sciroppoRecipe,
            components = listOf(
                RecipeComponentDetail(RecipeComponent(parentRecipeId = "sciroppo", childItemId = "acqua", baseQuantity = 250.0, unit = "ml"), itemAcqua),
                RecipeComponentDetail(RecipeComponent(parentRecipeId = "sciroppo", childItemId = "zucchero", baseQuantity = 250.0, unit = "g"), itemZucchero)
            )
        )

        val tree = MathEngine.buildScaledTree(sciroppoWithComps, 1.0, mapOf("sciroppo" to sciroppoWithComps))
        assertTrue("Lo sciroppo non deve contenere allergeni", tree.allergens.isEmpty())
    }

    @Test
    fun testDefaultPrepopulatedRecipesHaveCorrectAllergens(){
        val defaultItemsMap = panattoni.simo.data.local.PrepopulateData.DEFAULT_ITEMS.associateBy { it.id }

        // Mappa delle ricette con componenti
        val componentsByParent = panattoni.simo.data.local.PrepopulateData.DEFAULT_COMPONENTS.groupBy { it.parentRecipeId }
        val recipeEntities = panattoni.simo.data.local.PrepopulateData.DEFAULT_RECIPES.associateBy { it.id }

        val allRecipesMap = recipeEntities.mapValues { (recipeId, recipeEntity) ->
            val compDetails = (componentsByParent[recipeId] ?: emptyList()).map { compEntity ->
                val itemEntity = defaultItemsMap[compEntity.childItemId]!!
                val subRecipeEntity = recipeEntities[itemEntity.id]
                RecipeComponentDetail(
                    component = compEntity.toDomain(),
                    item = itemEntity.toDomain(),
                    subRecipe = subRecipeEntity?.toDomain()
                )
            }
            RecipeWithComponents(recipeEntity.toDomain(), compDetails)
        }

        // 1. Crema Pasticcera Classica: deve contenere LATTE e UOVA
        val cremaWithComps = allRecipesMap[panattoni.simo.data.local.PrepopulateData.ID_RECIPE_CREMA]!!
        val cremaTree = MathEngine.buildScaledTree(cremaWithComps, 1.0, allRecipesMap)
        assertEquals(listOf(Allergen.UOVA, Allergen.LATTE), cremaTree.allergens)

        // 2. Cornetto alla Crema: deve ereditare GLUTINE, UOVA, LATTE
        val cornettoWithComps = allRecipesMap[panattoni.simo.data.local.PrepopulateData.ID_RECIPE_CORNETTO]!!
        val cornettoTree = MathEngine.buildScaledTree(cornettoWithComps, 1.0, allRecipesMap)
        assertEquals(listOf(Allergen.GLUTINE, Allergen.UOVA, Allergen.LATTE), cornettoTree.allergens)

        // 3. Pizza Margherita: deve contenere GLUTINE e LATTE
        val pizzaWithComps = allRecipesMap[panattoni.simo.data.local.PrepopulateData.ID_RECIPE_PIZZA]!!
        val pizzaTree = MathEngine.buildScaledTree(pizzaWithComps, 1.0, allRecipesMap)
        assertEquals(listOf(Allergen.GLUTINE, Allergen.LATTE), pizzaTree.allergens)
    }
}
