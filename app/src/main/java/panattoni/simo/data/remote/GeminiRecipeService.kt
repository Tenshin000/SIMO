package panattoni.simo.data.remote

import panattoni.simo.domain.Allergen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class GeminiIngredient(
    val name: String,
    val quantity: Double,
    val unit: String,
    val pricePerUnit: Double? = null,
    val priceUnit: String? = null,
    val allergens: List<Allergen> = emptyList()
)

data class GeminiRecipeResponse(
    val recipeName: String? = null,
    val description: String? = null,
    val category: String? = null,
    val baseYield: Double? = null,
    val yieldUnit: String? = null,
    val ingredients: List<GeminiIngredient> = emptyList(),
    val instructions: String? = null
)

enum class GeminiModel(
    val modelId: String,
    val displayName: String,
    val description: String
){
    GEMINI_FLASH_LITE_LATEST(
        "gemini-flash-lite-latest",
        "Gemini Flash Lite (Predefinito)",
        "Ultra-veloce, massima disponibilità per dosi, costi e allergeni"
    ),
    GEMINI_FLASH_LATEST(
        "gemini-flash-latest",
        "Gemini Flash (Consigliato per ricette complesse)",
        "Massima ricchezza descrittiva e precisione per preparazioni elaborate"
    );

    companion object{
        val DEFAULT = GEMINI_FLASH_LITE_LATEST

        fun fromModelId(id: String?): GeminiModel{
            return when (id){
                GEMINI_FLASH_LATEST.modelId -> GEMINI_FLASH_LATEST
                GEMINI_FLASH_LITE_LATEST.modelId -> GEMINI_FLASH_LITE_LATEST
                // Migrazione automatica da vecchie configurazioni o id obsoleti
                "gemini-3.8-flash" -> GEMINI_FLASH_LATEST
                "gemini-3.5-flash" -> GEMINI_FLASH_LATEST
                "gemini-3.5-flash-lite" -> GEMINI_FLASH_LITE_LATEST
                "gemini-3.1-flash-lite" -> GEMINI_FLASH_LITE_LATEST
                else -> DEFAULT
            }
        }

        /**
         * Ritorna la lista dei modelli a partire da quello selezionato per il fallback.
         * Se viene scelto GEMINI_FLASH_LATEST, in caso di saturazione scala su GEMINI_FLASH_LITE_LATEST.
         * Se viene scelto GEMINI_FLASH_LITE_LATEST, esegue direttamente quello.
         */
        fun getFallbackHierarchyStartingFrom(model: GeminiModel): List<GeminiModel>{
            return when (model){
                GEMINI_FLASH_LATEST -> listOf(GEMINI_FLASH_LATEST, GEMINI_FLASH_LITE_LATEST)
                GEMINI_FLASH_LITE_LATEST -> listOf(GEMINI_FLASH_LITE_LATEST)
            }
        }
    }
}

class GeminiRecipeService{

    companion object{
        private val SYSTEM_INSTRUCTION = """
            Sei un Executive Chef e Maestro Pasticcere di livello mondiale, esperto in cucina italiana, pasticceria e chimica degli alimenti.
            Il tuo compito è assistere gli utenti nella creazione e nel perfezionamento di ricette professionali per l'app "SIMO".
            
            REGOLE FONDAMENTALI:
            1. DOSI E PESI:
               - Usa grammature realistiche, tonde e calibrate (per grammi 'g' e millilitri 'ml' preferisci multipli di 5 o 10).
               - Per ingredienti discreti usa 'unità' o 'pz' con numeri interi (es. 2 unità).
               - Non usare dosi impossibili.
            2. FOOD COST (Costi unitari di mercato in Italia):
               - Assegna a ogni ingrediente un prezzo unitario realistico di mercato italiano (es. Farina 1.10-1.40 €/kg, Burro 8.00-10.00 €/kg, Latte 1.20-1.50 €/l, Uova 0.30 €/unità, Olio EVO 9.00-12.00 €/l).
               - L'unità di prezzo ('priceUnit') deve essere una tra: "kg", "g", "l", "ml", "unità", "pz".
            3. I 14 ALLERGENI UFFICIALI UE (Regolamento UE 1169/2011):
               Per ogni ingrediente identifica con precisione estrema se contiene uno o più dei 14 allergeni ufficiali.
               Puoi usare ESCLUSIVAMENTE questi ID testuali esatti:
               - GLUTINE (grano, segale, orzo, avena, farro, kamut, farine)
               - CROSTACEI (gamberi, scampi, aragoste)
               - UOVA (uova intere, tuorli, albumi)
               - PESCE
               - ARACHIDI
               - SOIA
               - LATTE (latte, burro, formaggi, panna, yogurt, lattosio)
               - FRUTTA_A_GUSCIO (mandorle, nocciole, noci, pistacchi, anacardi)
               - SEDANO
               - SENAPE
               - SEMI_DI_SESAMO
               - SOLFITI (vino, aceto con concentrazioni > 10mg)
               - LUPINI
               - MOLLUSCHI
               Se l'ingrediente non contiene nessuno di questi (es. zucchero, acqua, sale, frutta fresca semplice), la lista allergeni deve essere vuota [].
            4. PROCEDIMENTO NUMERATO:
               - I passaggi devono essere rigorosamente numerati ("1. ...", "2. ...", "3. ...").
               - I passaggi DEVONO essere strettamente relativi agli ingredienti definiti, citandoli esplicitamente nei passaggi corrispondenti.
            
            DEVI RISPONDERE ESCLUSIVAMENTE CON UN DOCUMENTO JSON VALIDO.
        """.trimIndent()
    }

    /**
     * Compila un'intera ricetta partendo dal nome (ed eventuale descrizione/istruzioni fornite dall'utente).
     */
    suspend fun generateFullRecipe(
        apiKey: String,
        recipeName: String,
        description: String? = null,
        category: String? = null,
        baseYield: String? = null,
        yieldUnit: String? = null,
        userCustomPrompt: String? = null,
        preferredModel: String = GeminiModel.DEFAULT.modelId
    ): Result<GeminiRecipeResponse> = withContext(Dispatchers.IO){
        val trimmedKey = apiKey.trim()
        if (trimmedKey.isBlank()){
            return@withContext Result.failure(IllegalArgumentException("Inserisci la tua API Key di Google Gemini. Puoi ottenerla gratuitamente in 1 minuto su Google AI Studio."))
        }

        val prompt = buildString{
            append("Genera la ricetta completa e professionale per: \"$recipeName\".\n")
            if (!description.isNullOrBlank()){
                append("Descrizione / note dell'utente: \"$description\".\n")
            }
            if (!category.isNullOrBlank()){
                append("Categoria desiderata: \"$category\".\n")
            }
            if (!baseYield.isNullOrBlank() && !yieldUnit.isNullOrBlank()){
                append("Resa base richiesta: $baseYield $yieldUnit.\n")
            }
            if (!userCustomPrompt.isNullOrBlank()){
                append("Istruzioni speciali dell'utente: \"$userCustomPrompt\".\n")
            }
            append("""
                Rispondi con un JSON che rispetti questo schema esatto:
               {
                  "recipeName": "$recipeName",
                  "description": "Breve descrizione professionale accattivante",
                  "category": "Una tra Dolci, Pizze, Panetteria, Antipasti, Primi, Secondi, Contorni, Zuppe, Alcolici, Analcolici, Caffetteria, Altro",
                  "baseYield": 4.0,
                  "yieldUnit": "porzioni" o "cornetti" o "pizze" o "g",
                  "ingredients": [
                   {
                      "name": "Nome ingrediente",
                      "quantity": 250.0,
                      "unit": "g" o "ml" o "unità" o "pz" o "q.b.",
                      "pricePerUnit": 1.20,
                      "priceUnit": "kg" o "l" o "unità",
                      "allergens": ["GLUTINE"]
                    }
                  ],
                  "instructions": "1. Primo passaggio...\n2. Secondo passaggio..."
                }
            """.trimIndent())
        }

        callGeminiApi(trimmedKey, prompt, preferredModel)
    }

    /**
     * Completa gli ingredienti mancanti o parziali (dosi, unità, prezzi, allergeni) e genera le istruzioni.
     */
    suspend fun completeMissingIngredients(
        apiKey: String,
        recipeName: String,
        description: String? = null,
        baseYield: Double = 1.0,
        yieldUnit: String = "porzioni",
        existingIngredients: List<GeminiIngredient>,
        userCustomPrompt: String? = null,
        preferredModel: String = GeminiModel.DEFAULT.modelId
    ): Result<GeminiRecipeResponse> = withContext(Dispatchers.IO){
        val trimmedKey = apiKey.trim()
        if (trimmedKey.isBlank()){
            return@withContext Result.failure(IllegalArgumentException("Inserisci la tua API Key di Google Gemini."))
        }

        val ingredientsDescription = JSONArray().apply{
            existingIngredients.forEach{ ing ->
                val obj = JSONObject()
                obj.put("name", ing.name)
                obj.put("quantity", if (ing.quantity > 0.0) ing.quantity else "MANCANTE")
                obj.put("unit", ing.unit.ifBlank{ "MANCANTE" })
                if (ing.pricePerUnit != null) obj.put("pricePerUnit", ing.pricePerUnit)
                if (ing.priceUnit != null) obj.put("priceUnit", ing.priceUnit)
                obj.put("allergens", JSONArray(ing.allergens.map{ it.id }))
                put(obj)
            }
        }.toString()

        val prompt = buildString{
            append("Completa e perfeziona gli ingredienti per la ricetta: \"$recipeName\" (Resa: $baseYield $yieldUnit).\n")
            if (!description.isNullOrBlank()){
                append("Descrizione: \"$description\".\n")
            }
            append("Ingredienti parziali inseriti dall'utente:\n$ingredientsDescription\n\n")
            append("COMPITI RICHIESTI:\n")
            append("1. Mantieni tutti gli ingredienti inseriti dall'utente.\n")
            append("2. Se la quantità era 'MANCANTE' o pari a zero, calcola la quantità ('quantity') e unità ('unit') ideale e perfettamente bilanciata per una resa di $baseYield $yieldUnit.\n")
            append("3. Se mancano costo unitario ('pricePerUnit' / 'priceUnit'), stimali con precisione in euro.\n")
            append("4. Rileva e assegna gli allergeni UE per ciascun ingrediente (tra i 14 allergeni consentiti).\n")
            append("5. Se mancano ingredienti essenziali per la riuscita della ricetta, aggiungili con dosi, costi e allergeni completi.\n")
            append("6. Scrivi il procedimento di preparazione passo-passo numerato ('1. ...', '2. ...') menzionando tutti gli ingredienti.\n")
            if (!userCustomPrompt.isNullOrBlank()){
                append("Istruzioni extra dell'utente: \"$userCustomPrompt\".\n")
            }
            append("""
                Rispondi con un JSON che rispetti questo schema esatto:
               {
                  "recipeName": "$recipeName",
                  "description": "Descrizione",
                  "category": "Dolci",
                  "baseYield": $baseYield,
                  "yieldUnit": "$yieldUnit",
                  "ingredients": [
                   {
                      "name": "Nome ingrediente",
                      "quantity": 200.0,
                      "unit": "g",
                      "pricePerUnit": 1.20,
                      "priceUnit": "kg",
                      "allergens": ["GLUTINE"]
                    }
                  ],
                  "instructions": "1. ...\n2. ..."
                }
            """.trimIndent())
        }

        callGeminiApi(trimmedKey, prompt, preferredModel)
    }

    /**
     * Genera solo i passaggi di preparazione numerati coerenti con gli ingredienti specificati.
     */
    suspend fun generateInstructionsOnly(
        apiKey: String,
        recipeName: String,
        ingredients: List<GeminiIngredient>,
        userCustomPrompt: String? = null,
        preferredModel: String = GeminiModel.DEFAULT.modelId
    ): Result<String> = withContext(Dispatchers.IO){
        val trimmedKey = apiKey.trim()
        if (trimmedKey.isBlank()){
            return@withContext Result.failure(IllegalArgumentException("Inserisci la tua API Key di Google Gemini."))
        }

        val ingredientsListText = ingredients.joinToString("\n"){ ing ->
            "- ${ing.name}: ${if (ing.quantity > 0) "${ing.quantity} ${ing.unit}" else ing.unit}"
        }

        val prompt = buildString{
            append("Scrivi il procedimento di preparazione passo-passo numerato per la ricetta: \"$recipeName\".\n")
            append("Ingredienti disponibili e dosi:\n$ingredientsListText\n\n")
            append("REQUISITI:\n")
            append("- I passaggi DEVONO essere numerati: '1. ', '2. ', '3. ', ecc.\n")
            append("- I passaggi DEVONO essere rigorosamente relativi a TUTTI gli ingredienti elencati sopra.\n")
            append("- Descrivi temperature, tempi e consistenze ideali.\n")
            if (!userCustomPrompt.isNullOrBlank()){
                append("- Istruzioni extra: \"$userCustomPrompt\".\n")
            }
            append("""
                Rispondi con un JSON che contenga unicamente il campo "instructions":
               {
                  "instructions": "1. Primo passaggio...\n2. Secondo passaggio..."
                }
            """.trimIndent())
        }

        val response = callGeminiApi(trimmedKey, prompt, preferredModel)
        response.map{ it.instructions ?: "" }
    }

    private fun callGeminiApi(
        apiKey: String,
        prompt: String,
        preferredModel: String = GeminiModel.DEFAULT.modelId
    ): Result<GeminiRecipeResponse>{
        val startModel = GeminiModel.fromModelId(preferredModel)
        val modelsToTry = GeminiModel.getFallbackHierarchyStartingFrom(startModel)

        var lastError: Exception? = null

        for (gemModel in modelsToTry){
            val modelName = gemModel.modelId
            val urlStr = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
            println("[GeminiRecipeService] Attempting request with model: $modelName")

            for (attempt in 1..2){
                try{
                    val url = URL(urlStr)
                    val conn = (url.openConnection() as HttpURLConnection).apply{
                        requestMethod = "POST"
                        connectTimeout = 30000
                        readTimeout = 60000
                        doOutput = true
                        doInput = true
                        setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    }

                    val requestJson = JSONObject().apply{
                        val contentsArr = JSONArray().apply{
                            val contentObj = JSONObject().apply{
                                val partsArr = JSONArray().apply{
                                    put(JSONObject().put("text", prompt))
                                }
                                put("parts", partsArr)
                            }
                            put(contentObj)
                        }
                        put("contents", contentsArr)

                        val generationConfig = JSONObject().apply{
                            put("response_mime_type", "application/json")
                            put("temperature", 0.3)
                        }
                        put("generationConfig", generationConfig)

                        val systemInstructionObj = JSONObject().apply{
                            val partsArr = JSONArray().apply{
                                put(JSONObject().put("text", SYSTEM_INSTRUCTION))
                            }
                            put("parts", partsArr)
                        }
                        put("systemInstruction", systemInstructionObj)
                    }

                    OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use{ writer ->
                        writer.write(requestJson.toString())
                        writer.flush()
                    }

                    val responseCode = conn.responseCode
                    println("[GeminiRecipeService] Model $modelName (attempt $attempt) returned HTTP $responseCode")

                    if (responseCode == HttpURLConnection.HTTP_OK){
                        val responseText = BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use{ it.readText() }
                        val parsed = parseGeminiResponse(responseText)
                        return Result.success(parsed)
                    } else{
                        val errorText = try{
                            BufferedReader(InputStreamReader(conn.errorStream ?: conn.inputStream, Charsets.UTF_8)).use{ it.readText() }
                        } catch (_: Exception){ "" }
                        println("[GeminiRecipeService] Model $modelName returned error: HTTP $responseCode - $errorText")

                        val errorMsg = when (responseCode){
                            400 -> "Richiesta non valida. Verifica la tua API Key di Gemini."
                            403 -> "API Key non autorizzata o non valida. Controlla la chiave generata su Google AI Studio."
                            429 -> "Limite di richieste temporaneamente superato (Quota Google AI Studio). Attendi qualche secondo e riprova."
                            500, 503 -> "I server di Google Gemini ($modelName) sono temporaneamente occupati."
                            else -> "Errore HTTP $responseCode: $errorText"
                        }
                        lastError = RuntimeException(errorMsg)

                        if (responseCode == 400 || responseCode == 403){
                            // Don't retry with invalid API key
                            return Result.failure(lastError)
                        }

                        if (attempt < 2){
                            try{ Thread.sleep(800) } catch (_: InterruptedException){}
                        }
                    }
                } catch (e: Exception){
                    println("[GeminiRecipeService] Exception for model $modelName (attempt $attempt): ${e.message}")
                    lastError = e
                    if (attempt < 2){
                        try{ Thread.sleep(800) } catch (_: InterruptedException){}
                    }
                }
            }
        }

        return Result.failure(lastError ?: RuntimeException("Impossibile contattare il servizio Gemini."))
    }

    private fun parseGeminiResponse(jsonString: String): GeminiRecipeResponse{
        val root = JSONObject(jsonString)
        val candidates = root.optJSONArray("candidates")
            ?: throw IllegalStateException("Risposta Gemini priva di candidati.")
        if (candidates.length() == 0) throw IllegalStateException("Nessun contenuto restituito da Gemini.")

        val firstCandidate = candidates.getJSONObject(0)
        val content = firstCandidate.optJSONObject("content")
            ?: throw IllegalStateException("Contenuto non presente nel candidato Gemini.")
        val parts = content.optJSONArray("parts")
            ?: throw IllegalStateException("Nessun frammento di testo nella risposta Gemini.")
        if (parts.length() == 0) throw IllegalStateException("Testo della risposta vuoto.")

        val rawText = parts.getJSONObject(0).optString("text", "{}")
        val recipeJson = JSONObject(rawText)
        return parseRecipeJson(recipeJson)
    }

    internal fun parseRecipeJson(recipeJson: JSONObject): GeminiRecipeResponse{
        val recipeName = recipeJson.optString("recipeName").ifBlank{ recipeJson.optString("name") }.trim().takeIf{ it.isNotBlank() }
        val description = recipeJson.optString("description").trim().takeIf{ it.isNotBlank() }
        val category = recipeJson.optString("category").trim().takeIf{ it.isNotBlank() }
        val baseYield = if (recipeJson.has("baseYield")) recipeJson.optDouble("baseYield", 1.0) else null
        val yieldUnit = recipeJson.optString("yieldUnit").trim().takeIf{ it.isNotBlank() }

        val instructions = if (recipeJson.has("instructions")){
            val instObj = recipeJson.opt("instructions")
            when (instObj){
                is org.json.JSONArray ->{
                    val sb = StringBuilder()
                    for (i in 0 until instObj.length()){
                        val step = instObj.optString(i, "").trim()
                        if (step.isNotBlank()){
                            if (sb.isNotEmpty()) sb.append("\n")
                            if (!step.matches(Regex("""^\d+\..*"""))){
                                sb.append("${i + 1}. $step")
                            } else{
                                sb.append(step)
                            }
                        }
                    }
                    sb.toString().takeIf{ it.isNotBlank() }
                }
                is String -> instObj.takeIf{ it.isNotBlank() }
                else -> recipeJson.optString("instructions").takeIf{ it.isNotBlank() }
            }
        } else null

        val ingredientsList = mutableListOf<GeminiIngredient>()
        val ingArray = recipeJson.optJSONArray("ingredients")
        if (ingArray != null){
            for (i in 0 until ingArray.length()){
                val ingObj = ingArray.optJSONObject(i) ?: continue
                val name = ingObj.optString("name", "").trim()
                if (name.isBlank()) continue

                val rawQty = ingObj.optDouble("quantity", 0.0)
                val safeQty = if (rawQty.isFinite() && rawQty > 0.0) rawQty else 0.0
                val unit = ingObj.optString("unit", "g").trim().ifBlank{ "g" }

                val price = if (ingObj.has("pricePerUnit")){
                    val p = ingObj.optDouble("pricePerUnit", 0.0)
                    if (p.isFinite() && p > 0.0) p else null
                } else null

                val rawPriceUnit = ingObj.optString("priceUnit", "").trim().ifBlank{ null }
                val cleanPriceUnit = rawPriceUnit
                    ?.replace("EUR/", "", ignoreCase = true)
                    ?.replace("€/", "")
                    ?.replace("pezzo", "pz", ignoreCase = true)
                    ?.replace("mazzetto", "pz", ignoreCase = true)
                    ?.trim()
                    ?.ifBlank{ null }

                val allergens = mutableListOf<Allergen>()
                val allergensArr = ingObj.optJSONArray("allergens")
                if (allergensArr != null){
                    allergens.addAll(parseAllergens(allergensArr))
                }

                ingredientsList.add(
                    GeminiIngredient(
                        name = name,
                        quantity = safeQty,
                        unit = unit,
                        pricePerUnit = price,
                        priceUnit = cleanPriceUnit,
                        allergens = allergens.distinct()
                    )
                )
            }
        }

        return GeminiRecipeResponse(
            recipeName = recipeName,
            description = description,
            category = category,
            baseYield = baseYield,
            yieldUnit = yieldUnit,
            ingredients = ingredientsList,
            instructions = instructions
        )
    }

    internal fun parseAllergens(allergensArr: org.json.JSONArray): List<Allergen>{
        val list = mutableListOf<Allergen>()
        for (j in 0 until allergensArr.length()){
            val allergenStr = allergensArr.optString(j, "").trim()
            Allergen.fromId(allergenStr)?.let{ list.add(it) } ?: run{
                val matched = Allergen.entries.firstOrNull{ 
                    it.id.equals(allergenStr, ignoreCase = true) || 
                    it.displayName.equals(allergenStr, ignoreCase = true) ||
                    it.name.equals(allergenStr, ignoreCase = true)
                }
                if (matched != null) list.add(matched)
            }
        }
        return list.distinct()
    }
}
