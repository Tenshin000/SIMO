package panattoni.simo

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import panattoni.simo.data.local.entity.ItemEntity
import panattoni.simo.data.local.entity.RecipeEntity
import panattoni.simo.data.model.Item
import panattoni.simo.data.model.ItemType
import panattoni.simo.data.model.Recipe

/**
 * Test unitari per la persistenza e il backup dei 6 valori nutrizionali
 * e del modificatore peso in cottura (calo / resa pasta e riso).
 */
class NutritionDatabaseAndBackupTest{

    @Test
    fun testItemNutritionMapping(){
        // Verifica il mapping bidirezionale tra Item e ItemEntity con i 6 valori nutrizionali
        val domainItem = Item(
            id = "ing_pasta",
            name = "Pasta di semola",
            type = ItemType.RAW_INGREDIENT,
            defaultUnit = "g",
            pricePerUnit = 1.50,
            priceUnit = "kg",
            allergens = "GLUTINE",
            caloriesKcal = 356.0,
            carbohydratesG = 71.2,
            fatsG = 1.5,
            proteinsG = 12.5,
            waterG = 12.5,
            sodiumMg = 5.0
        )

        val entity = ItemEntity.fromDomain(domainItem)
        assertEquals("ing_pasta", entity.id)
        assertEquals(356.0, entity.caloriesKcal!!, 0.001)
        assertEquals(71.2, entity.carbohydratesG!!, 0.001)
        assertEquals(1.5, entity.fatsG!!, 0.001)
        assertEquals(12.5, entity.proteinsG!!, 0.001)
        assertEquals(12.5, entity.waterG!!, 0.001)
        assertEquals(5.0, entity.sodiumMg!!, 0.001)

        val reconstructed = entity.toDomain()
        assertEquals(domainItem.id, reconstructed.id)
        assertEquals(domainItem.caloriesKcal, reconstructed.caloriesKcal)
        assertEquals(domainItem.carbohydratesG, reconstructed.carbohydratesG)
        assertEquals(domainItem.fatsG, reconstructed.fatsG)
        assertEquals(domainItem.proteinsG, reconstructed.proteinsG)
        assertEquals(domainItem.waterG, reconstructed.waterG)
        assertEquals(domainItem.sodiumMg, reconstructed.sodiumMg)
    }

    @Test
    fun testItemNutritionDefaultsNull(){
        // Verifica che per ingredienti senza dati nutrizionali i valori siano null di default
        val emptyItem = Item(
            id = "ing_test",
            name = "Test",
            type = ItemType.RAW_INGREDIENT
        )
        assertNull(emptyItem.caloriesKcal)
        assertNull(emptyItem.carbohydratesG)
        assertNull(emptyItem.fatsG)
        assertNull(emptyItem.proteinsG)
        assertNull(emptyItem.waterG)
        assertNull(emptyItem.sodiumMg)

        val entity = ItemEntity.fromDomain(emptyItem)
        assertNull(entity.caloriesKcal)
        assertNull(entity.carbohydratesG)
        assertNull(entity.fatsG)
        assertNull(entity.proteinsG)
        assertNull(entity.waterG)
        assertNull(entity.sodiumMg)
    }

    @Test
    fun testRecipeCookingWeightModifierMapping(){
        // Verifica il mapping del modificatore peso cottura (calo negativo e aumento positivo)
        val shrinkageRecipe = Recipe(
            id = "recipe_pane",
            name = "Pane Casereccio",
            baseYieldQuantity = 1.0,
            yieldUnit = "kg",
            cookingWeightModifier = -15.0 // Calo del 15% per evaporazione in forno
        )
        val entityShrink = RecipeEntity.fromDomain(shrinkageRecipe)
        assertEquals(-15.0, entityShrink.cookingWeightModifier, 0.001)
        assertEquals(-15.0, entityShrink.toDomain().cookingWeightModifier, 0.001)

        val hydrationRecipe = Recipe(
            id = "recipe_pasta_cotta",
            name = "Pasta Lessata",
            baseYieldQuantity = 1.0,
            yieldUnit = "porzione",
            cookingWeightModifier = 130.0 // Aumento del 130% (massa più che raddoppiata per assorbimento acqua)
        )
        val entityHydrate = RecipeEntity.fromDomain(hydrationRecipe)
        assertEquals(130.0, entityHydrate.cookingWeightModifier, 0.001)
        assertEquals(130.0, entityHydrate.toDomain().cookingWeightModifier, 0.001)
    }

    @Test
    fun testBackupJsonV2SerializationAndDeserialization(){
        // Simula la struttura JSON prodotta da BackupManager v2
        val root = JSONObject()
        root.put("version", 2)
        root.put("appName", "SIMO")
        root.put("timestamp", 1700000000000L)

        val itemsArr = JSONArray()
        val itemObj = JSONObject()
        itemObj.put("id", "ing_riso")
        itemObj.put("name", "Riso Carnaroli")
        itemObj.put("type", "RAW_INGREDIENT")
        itemObj.put("defaultUnit", "g")
        itemObj.put("pricePerUnit", 3.20)
        itemObj.put("priceUnit", "kg")
        itemObj.put("caloriesKcal", 350.0)
        itemObj.put("carbohydratesG", 78.0)
        itemObj.put("fatsG", 0.6)
        itemObj.put("proteinsG", 6.8)
        itemObj.put("waterG", 14.0)
        itemObj.put("sodiumMg", 3.0)
        itemsArr.put(itemObj)
        root.put("items", itemsArr)

        val recipesArr = JSONArray()
        val recipeObj = JSONObject()
        recipeObj.put("id", "recipe_risotto")
        recipeObj.put("name", "Risotto allo Zafferano")
        recipeObj.put("baseYieldQuantity", 4.0)
        recipeObj.put("yieldUnit", "porzioni")
        recipeObj.put("cookingWeightModifier", 110.0)
        recipesArr.put(recipeObj)
        root.put("recipes", recipesArr)

        // Verifica parsing JSON per l'ingrediente
        val parsedItemObj = root.getJSONArray("items").getJSONObject(0)
        val parsedItem = ItemEntity(
            id = parsedItemObj.getString("id"),
            name = parsedItemObj.getString("name"),
            type = parsedItemObj.getString("type"),
            caloriesKcal = if (parsedItemObj.has("caloriesKcal")) parsedItemObj.optDouble("caloriesKcal", 0.0) else null,
            carbohydratesG = if (parsedItemObj.has("carbohydratesG")) parsedItemObj.optDouble("carbohydratesG", 0.0) else null,
            fatsG = if (parsedItemObj.has("fatsG")) parsedItemObj.optDouble("fatsG", 0.0) else null,
            proteinsG = if (parsedItemObj.has("proteinsG")) parsedItemObj.optDouble("proteinsG", 0.0) else null,
            waterG = if (parsedItemObj.has("waterG")) parsedItemObj.optDouble("waterG", 0.0) else null,
            sodiumMg = if (parsedItemObj.has("sodiumMg")) parsedItemObj.optDouble("sodiumMg", 0.0) else null
        )

        assertEquals(350.0, parsedItem.caloriesKcal!!, 0.001)
        assertEquals(78.0, parsedItem.carbohydratesG!!, 0.001)
        assertEquals(0.6, parsedItem.fatsG!!, 0.001)
        assertEquals(6.8, parsedItem.proteinsG!!, 0.001)
        assertEquals(14.0, parsedItem.waterG!!, 0.001)
        assertEquals(3.0, parsedItem.sodiumMg!!, 0.001)

        // Verifica parsing JSON per la ricetta
        val parsedRecipeObj = root.getJSONArray("recipes").getJSONObject(0)
        val parsedRecipe = RecipeEntity(
            id = parsedRecipeObj.getString("id"),
            name = parsedRecipeObj.getString("name"),
            baseYieldQuantity = parsedRecipeObj.getDouble("baseYieldQuantity"),
            yieldUnit = parsedRecipeObj.getString("yieldUnit"),
            cookingWeightModifier = if (parsedRecipeObj.has("cookingWeightModifier")) parsedRecipeObj.optDouble("cookingWeightModifier", 0.0) else 0.0
        )
        assertEquals(110.0, parsedRecipe.cookingWeightModifier, 0.001)
    }

    @Test
    fun testBackupJsonV1BackwardCompatibility(){
        // Simula un backup JSON generato dalla Versione 1 (senza campi nutrizionali e senza modificatore cottura)
        val v1JsonString = """
            {
              "version": 1,
              "appName": "SIMO",
              "timestamp": 1690000000000,
              "items": [
                {
                  "id": "ing_zucchero",
                  "name": "Zucchero semolato",
                  "type": "RAW_INGREDIENT",
                  "defaultUnit": "g",
                  "pricePerUnit": 1.20,
                  "priceUnit": "kg"
                }
              ],
              "recipes": [
                {
                  "id": "recipe_torta",
                  "name": "Torta Margherita",
                  "baseYieldQuantity": 8.0,
                  "yieldUnit": "porzioni"
                }
              ],
              "components": []
            }
        """.trimIndent()

        val json = JSONObject(v1JsonString)
        assertEquals(1, json.getInt("version"))

        // Parsing item v1
        val itemObj = json.getJSONArray("items").getJSONObject(0)
        val item = ItemEntity(
            id = itemObj.getString("id"),
            name = itemObj.getString("name"),
            type = itemObj.getString("type"),
            caloriesKcal = if (itemObj.has("caloriesKcal") && !itemObj.isNull("caloriesKcal")) itemObj.optDouble("caloriesKcal", 0.0) else null,
            carbohydratesG = if (itemObj.has("carbohydratesG") && !itemObj.isNull("carbohydratesG")) itemObj.optDouble("carbohydratesG", 0.0) else null,
            fatsG = if (itemObj.has("fatsG") && !itemObj.isNull("fatsG")) itemObj.optDouble("fatsG", 0.0) else null,
            proteinsG = if (itemObj.has("proteinsG") && !itemObj.isNull("proteinsG")) itemObj.optDouble("proteinsG", 0.0) else null,
            waterG = if (itemObj.has("waterG") && !itemObj.isNull("waterG")) itemObj.optDouble("waterG", 0.0) else null,
            sodiumMg = if (itemObj.has("sodiumMg") && !itemObj.isNull("sodiumMg")) itemObj.optDouble("sodiumMg", 0.0) else null
        )
        assertNull(item.caloriesKcal)
        assertNull(item.carbohydratesG)
        assertNull(item.fatsG)
        assertNull(item.proteinsG)
        assertNull(item.waterG)
        assertNull(item.sodiumMg)

        // Parsing recipe v1
        val recipeObj = json.getJSONArray("recipes").getJSONObject(0)
        val recipe = RecipeEntity(
            id = recipeObj.getString("id"),
            name = recipeObj.getString("name"),
            baseYieldQuantity = recipeObj.getDouble("baseYieldQuantity"),
            yieldUnit = recipeObj.getString("yieldUnit"),
            cookingWeightModifier = if (recipeObj.has("cookingWeightModifier")) recipeObj.optDouble("cookingWeightModifier", 0.0) else 0.0
        )
        assertEquals(0.0, recipeObj.optDouble("cookingWeightModifier", 0.0), 0.001)
        assertEquals(0.0, recipe.cookingWeightModifier, 0.001)
    }
}
