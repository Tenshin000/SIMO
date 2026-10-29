package panattoni.simo

import panattoni.simo.data.remote.AiProvider
import panattoni.simo.data.remote.OpenAiModel
import panattoni.simo.data.remote.OpenAiRecipeService
import panattoni.simo.domain.Allergen
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Test unitari per il servizio OpenAI ChatGPT: verifica parsing JSON,
 * mappatura allergeni, gerarchia fallback modelli e provider.
 */
class OpenAiRecipeServiceTest{

    private val service = OpenAiRecipeService()

    @Test
    fun testOpenAiModelsAndFallback(){
        assertEquals(OpenAiModel.GPT_4O_MINI, OpenAiModel.DEFAULT)
        assertEquals(OpenAiModel.GPT_4O, OpenAiModel.fromModelId("gpt-4o"))
        assertEquals(OpenAiModel.GPT_4O_MINI, OpenAiModel.fromModelId("gpt-4o-mini"))
        assertEquals(OpenAiModel.GPT_4O_MINI, OpenAiModel.fromModelId("unknown-model"))

        val fallbackGpt4o = OpenAiModel.getFallbackHierarchyStartingFrom(OpenAiModel.GPT_4O)
        assertEquals(2, fallbackGpt4o.size)
        assertEquals(OpenAiModel.GPT_4O, fallbackGpt4o[0])
        assertEquals(OpenAiModel.GPT_4O_MINI, fallbackGpt4o[1])

        val fallbackMini = OpenAiModel.getFallbackHierarchyStartingFrom(OpenAiModel.GPT_4O_MINI)
        assertEquals(1, fallbackMini.size)
        assertEquals(OpenAiModel.GPT_4O_MINI, fallbackMini[0])
    }

    @Test
    fun testAiProviderEnum(){
        assertEquals(AiProvider.GEMINI, AiProvider.DEFAULT)
        assertEquals(AiProvider.OPENAI, AiProvider.fromId("openai"))
        assertEquals(AiProvider.OPENAI, AiProvider.fromId("OpenAI"))
        assertEquals(AiProvider.GEMINI, AiProvider.fromId("gemini"))
        assertEquals(AiProvider.GEMINI, AiProvider.fromId(null))
    }

    @Test
    fun testParseFullRecipeResponseFromOpenAi(){
        val mockOpenAiApiResponse = """
        {
          "id": "chatcmpl-12345",
          "object": "chat.completion",
          "choices": [
            {
              "index": 0,
              "message": {
                "role": "assistant",
                "content": "{\n  \"name\": \"Torta Margherita Soffice\",\n  \"description\": \"Dolce da colazione classico della tradizione italiana.\",\n  \"category\": \"Dolci\",\n  \"baseYield\": 8.0,\n  \"yieldUnit\": \"porzioni\",\n  \"ingredients\": [\n    {\n      \"name\": \"Farina 00\",\n      \"quantity\": 200.0,\n      \"unit\": \"g\",\n      \"pricePerUnit\": 1.20,\n      \"priceUnit\": \"kg\",\n      \"allergens\": [\"GLUTINE\"]\n    },\n    {\n      \"name\": \"Fecola di patate\",\n      \"quantity\": 100.0,\n      \"unit\": \"g\",\n      \"pricePerUnit\": 2.00,\n      \"priceUnit\": \"kg\",\n      \"allergens\": []\n    },\n    {\n      \"name\": \"Uova medie\",\n      \"quantity\": 4.0,\n      \"unit\": \"pz\",\n      \"pricePerUnit\": 0.35,\n      \"priceUnit\": \"pz\",\n      \"allergens\": [\"uova\"]\n    },\n    {\n      \"name\": \"Burro fuso\",\n      \"quantity\": 100.0,\n      \"unit\": \"g\",\n      \"pricePerUnit\": 9.50,\n      \"priceUnit\": \"kg\",\n      \"allergens\": [\"latte\"]\n    }\n  ],\n  \"instructions\": \"1. Montare le uova con lo zucchero fino a renderle chiare e spumose.\\n2. Aggiungere le farine setacciate.\\n3. Unire a filo il burro fuso a temperatura ambiente.\\n4. Cuocere a 180°C per 35 minuti.\"\n}"
              }
            }
          ]
        }
        """.trimIndent()

        val parsed = service.parseOpenAiResponse(mockOpenAiApiResponse)

        assertEquals("Torta Margherita Soffice", parsed.recipeName)
        assertEquals("Dolci", parsed.category)
        assertEquals(8.0, parsed.baseYield!!, 0.001)
        assertEquals("porzioni", parsed.yieldUnit)
        assertEquals(4, parsed.ingredients.size)

        val uova = parsed.ingredients[2]
        assertEquals("Uova medie", uova.name)
        assertEquals(4.0, uova.quantity, 0.001)
        assertEquals(0.35, uova.pricePerUnit!!, 0.001)
        assertTrue(uova.allergens.contains(Allergen.UOVA))

        val burro = parsed.ingredients[3]
        assertTrue(burro.allergens.contains(Allergen.LATTE))

        assertNotNull(parsed.instructions)
        assertTrue(parsed.instructions!!.contains("1. "))
        assertTrue(parsed.instructions!!.contains("4. "))
    }

    @Test
    fun testParseRecipeJsonWithCustomYieldUnit(){
        // Verifica che un'unità personalizzata sensata come "bicchieri" o "torta" venga preservata e ripulita correttamente
        val json = JSONObject("""
        {
          "name": "Spritz Veneziano",
          "category": "Alcolici",
          "baseYield": 2.0,
          "yieldUnit": "bicchieri (2 drink)",
          "ingredients": [
            { "name": "Prosecco", "quantity": 180.0, "unit": "ml", "pricePerUnit": 6.0, "priceUnit": "l", "allergens": ["SOLFITI"] }
          ],
          "instructions": "1. Riempire i bicchieri di ghiaccio.\n2. Versare il prosecco."
        }
        """.trimIndent())

        val parsed = service.parseRecipeJson(json)
        assertEquals("bicchieri", parsed.yieldUnit)
        assertEquals(2.0, parsed.baseYield!!, 0.001)
    }
}
