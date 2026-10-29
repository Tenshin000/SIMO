package panattoni.simo

import panattoni.simo.data.remote.GeminiRecipeService
import panattoni.simo.domain.Allergen
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiRecipeServiceTest {

    private val service = GeminiRecipeService()

    @Test
    fun testParseFullRecipeResponse(){
        val mockJsonResponse = """
        {
          "name": "Pan di Spagna Classico",
          "description": "Base soffice della pasticceria italiana senza lievito.",
          "category": "Basi Pasticceria",
          "baseYield": 1.0,
          "yieldUnit": "torta",
          "ingredients": [
            {
              "name": "Uova intere",
              "quantity": 5.0,
              "unit": "pz",
              "pricePerUnit": 0.30,
              "priceUnit": "pz",
              "allergens": ["UOVA"]
            },
            {
              "name": "Zucchero semolato",
              "quantity": 150.0,
              "unit": "g",
              "pricePerUnit": 1.20,
              "priceUnit": "kg",
              "allergens": []
            },
            {
              "name": "Farina 00",
              "quantity": 150.0,
              "unit": "g",
              "pricePerUnit": 1.10,
              "priceUnit": "kg",
              "allergens": ["GLUTINE"]
            },
            {
              "name": "Scorza di limone",
              "quantity": 0.0,
              "unit": "q.b.",
              "pricePerUnit": 0.0,
              "priceUnit": "",
              "allergens": []
            }
          ],
          "instructions": "1. Montare le uova a temperatura ambiente con lo zucchero per almeno 15 minuti.\n2. Setacciare la farina 00 due volte.\n3. Incorporare la farina delicatamente con movimenti dal basso verso l'alto.\n4. Aggiungere la scorza di limone grattugiata.\n5. Infornare a 170°C per 35 minuti in forno statico."
        }
        """.trimIndent()

        val parsed = service.parseRecipeJson(JSONObject(mockJsonResponse))

        assertEquals("Pan di Spagna Classico", parsed.recipeName)
        assertEquals("Basi Pasticceria", parsed.category)
        assertEquals(4, parsed.ingredients.size)

        val uova = parsed.ingredients[0]
        assertEquals("Uova intere", uova.name)
        assertEquals(5.0, uova.quantity, 0.001)
        assertEquals("pz", uova.unit)
        assertEquals(0.30, uova.pricePerUnit!!, 0.001)
        assertEquals("pz", uova.priceUnit)
        assertEquals(listOf(Allergen.UOVA), uova.allergens)

        val farina = parsed.ingredients[2]
        assertEquals("Farina 00", farina.name)
        assertEquals(150.0, farina.quantity, 0.001)
        assertEquals(listOf(Allergen.GLUTINE), farina.allergens)

        assertNotNull(parsed.instructions)
        assertTrue(parsed.instructions!!.startsWith("1. "))
        assertTrue(parsed.instructions!!.contains("2. "))
        assertTrue(parsed.instructions!!.contains("3. "))
        assertTrue(parsed.instructions!!.contains("4. "))
        assertTrue(parsed.instructions!!.contains("5. "))
        assertEquals("torta", parsed.yieldUnit)
    }

    @Test
    fun testParseRecipeJsonWithCustomYieldUnit(){
        // Verifica che l'unità personalizzata sensata come "bicchieri" o "torta" venga preservata
        val mockJsonResponse = """
        {
          "name": "Negroni Classico",
          "category": "Alcolici",
          "baseYield": 1.0,
          "yieldUnit": "bicchieri",
          "ingredients": [
            { "name": "Gin", "quantity": 30.0, "unit": "ml", "pricePerUnit": 15.0, "priceUnit": "l", "allergens": [] }
          ]
        }
        """.trimIndent()
        val parsed = service.parseRecipeJson(JSONObject(mockJsonResponse))
        assertEquals("bicchieri", parsed.yieldUnit)
    }

    @Test
    fun testParseAllergensFlexibleMatching(){
        val jsonArray = org.json.JSONArray().apply {
            put("GLUTINE")
            put("latte")
            put("FRUTTA_A_GUSCIO")
            put("uova")
        }

        val allergens = service.parseAllergens(jsonArray)
        assertTrue(allergens.contains(Allergen.GLUTINE))
        assertTrue(allergens.contains(Allergen.LATTE))
        assertTrue(allergens.contains(Allergen.FRUTTA_A_GUSCIO))
        assertTrue(allergens.contains(Allergen.UOVA))
    }

    @Test
    fun testNumberedStepsAreFormattedProperly(){
        val steps = "1. Montare il burro con lo zucchero a velo.\n2. Aggiungere i tuorli a filo.\n3. Incorporare la farina di mandorle e la farina debole."
        val lines = steps.lines()
        assertEquals(3, lines.size)
        assertTrue(lines[0].startsWith("1. "))
        assertTrue(lines[1].startsWith("2. "))
        assertTrue(lines[2].startsWith("3. "))
    }

    @Test
    fun testParseInstructionsAsJsonArray(){
        val json = JSONObject("""
        {
          "name": "Test Ricetta",
          "instructions": [
            "Primo passaggio di preparazione",
            "Secondo passaggio di preparazione"
          ]
        }
        """)
        val parsed = service.parseRecipeJson(json)
        assertNotNull(parsed.instructions)
        assertTrue(parsed.instructions!!.contains("1. Primo passaggio"))
        assertTrue(parsed.instructions!!.contains("2. Secondo passaggio"))
    }

    @Test
    fun testGeminiModelHierarchyAndDefault(){
        val defaultModel = panattoni.simo.data.remote.GeminiModel.DEFAULT
        assertEquals(panattoni.simo.data.remote.GeminiModel.GEMINI_FLASH_LITE_LATEST, defaultModel)
        assertEquals("gemini-flash-lite-latest", defaultModel.modelId)

        // Test from Flash Latest: should cascade to Flash Latest then Flash Lite Latest
        val fromFlash = panattoni.simo.data.remote.GeminiModel.getFallbackHierarchyStartingFrom(
            panattoni.simo.data.remote.GeminiModel.GEMINI_FLASH_LATEST
        )
        assertEquals(
            listOf(
                panattoni.simo.data.remote.GeminiModel.GEMINI_FLASH_LATEST,
                panattoni.simo.data.remote.GeminiModel.GEMINI_FLASH_LITE_LATEST
            ),
            fromFlash
        )

        // Test from Flash Lite Latest: only that one
        val fromLite = panattoni.simo.data.remote.GeminiModel.getFallbackHierarchyStartingFrom(
            panattoni.simo.data.remote.GeminiModel.GEMINI_FLASH_LITE_LATEST
        )
        assertEquals(
            listOf(panattoni.simo.data.remote.GeminiModel.GEMINI_FLASH_LITE_LATEST),
            fromLite
        )

        // Test migration of old preferences
        assertEquals(
            panattoni.simo.data.remote.GeminiModel.GEMINI_FLASH_LITE_LATEST,
            panattoni.simo.data.remote.GeminiModel.fromModelId("gemini-3.1-flash-lite")
        )
        assertEquals(
            panattoni.simo.data.remote.GeminiModel.GEMINI_FLASH_LITE_LATEST,
            panattoni.simo.data.remote.GeminiModel.fromModelId("gemini-3.5-flash-lite")
        )
        assertEquals(
            panattoni.simo.data.remote.GeminiModel.GEMINI_FLASH_LATEST,
            panattoni.simo.data.remote.GeminiModel.fromModelId("gemini-3.8-flash")
        )

        // Test unknown model id
        val unknown = panattoni.simo.data.remote.GeminiModel.fromModelId("unknown-model-xyz")
        assertEquals(panattoni.simo.data.remote.GeminiModel.GEMINI_FLASH_LITE_LATEST, unknown)
    }
}
