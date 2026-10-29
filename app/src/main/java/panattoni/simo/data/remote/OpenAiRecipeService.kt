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

/**
 * Provider AI selezionabili dall'utente per la generazione delle ricette.
 */
enum class AiProvider(
    val id: String,
    val displayName: String
){
    GEMINI("gemini", "Google Gemini"),
    OPENAI("openai", "ChatGPT (OpenAI)");

    companion object{
        val DEFAULT = GEMINI

        fun fromId(id: String?): AiProvider{
            return when (id?.lowercase()){
                OPENAI.id -> OPENAI
                else -> GEMINI
            }
        }
    }
}

/**
 * Modelli ufficiali OpenAI supportati: dal più leggero ed economico al più avanzato.
 */
enum class OpenAiModel(
    val modelId: String,
    val displayName: String,
    val description: String
){
    GPT_4O_MINI(
        "gpt-4o-mini",
        "GPT-4o Mini (Predefinito e leggero)",
        "Ultra-veloce ed efficiente, ideale per pesi, costi e allergeni"
    ),
    GPT_4O(
        "gpt-4o",
        "GPT-4o (Avanzato per ricette complesse)",
        "Massima precisione e intelligenza culinaria per preparazioni elaborate"
    );

    companion object{
        val DEFAULT = GPT_4O_MINI

        fun fromModelId(id: String?): OpenAiModel{
            return when (id){
                GPT_4O.modelId -> GPT_4O
                GPT_4O_MINI.modelId -> GPT_4O_MINI
                else -> DEFAULT
            }
        }

        /**
         * Ritorna la gerarchia di fallback a cascata: se gpt-4o fallisce o è saturo, ripiega su gpt-4o-mini.
         */
        fun getFallbackHierarchyStartingFrom(model: OpenAiModel): List<OpenAiModel>{
            return when (model){
                GPT_4O -> listOf(GPT_4O, GPT_4O_MINI)
                GPT_4O_MINI -> listOf(GPT_4O_MINI)
            }
        }
    }
}

/**
 * Servizio di integrazione REST per OpenAI ChatGPT (gpt-4o-mini e gpt-4o).
 * Permette la generazione di ricette complete, il completamento di dosi mancanti e la stesura del procedimento.
 */
class OpenAiRecipeService{

    companion object{
        private const val OPENAI_API_ENDPOINT = "https://api.openai.com/v1/chat/completions"

        // Prompt di sistema per istruire il modello sulle regole di SIMO
        private val SYSTEM_INSTRUCTION = """
            Sei un Executive Chef e Maestro Pasticcere di livello mondiale, esperto in cucina italiana, pasticceria e chimica degli alimenti.
            Il tuo compito è assistere gli utenti nella creazione e nel perfezionamento di ricette professionali per l'app "SIMO".
            
            REGOLE FONDAMENTALI:
            0. VALORI IL PIÙ REALISTICI POSSIBILE:
               - Ogni singolo dato (grammature, costi unitari di mercato italiano, valori nutrizionali per 100g, umidità residua, calo o resa in cottura) DEVE essere il più realistico, autentico e affidabile possibile.
               - Basati rigorosamente sulla realtà gastronomica professionale e sui dati scientifici ufficiali (tabelle CREA / USDA).
               - Evita tassativamente cifre esagerate, arbitrarie, sproporzionate o non calibrate.
            1. DOSI E PESI:
               - Usa grammature realistiche, tonde e calibrate (per grammi 'g' e millilitri 'ml' preferisci multipli di 5 o 10).
               - Per ingredienti discreti usa 'unità' o 'pz' con numeri interi (es. 2 unità).
               - Non usare dosi impossibili.
            2. FOOD COST E COERENZA UNITÀ DI PREZZO:
               - Assegna a ogni ingrediente un prezzo unitario realistico di mercato italiano (es. Farina 1.10-1.40 €/kg, Burro 8.00-10.00 €/kg, Latte 1.20-1.50 €/l, Uova 0.30 €/unità, Olio EVO 9.00-12.00 €/l).
               - L'unità di prezzo ('priceUnit') deve essere una tra: "kg", "g", "l", "ml", "unità", "pz".
               - COERENZA RIGOROSA: Per i liquidi dosati in 'ml' o 'l' (latte, acqua, caffè, succhi, olio, panna) usa TASSATIVAMENTE 'l' come 'priceUnit' (es. 1.20 €/l). Non usare mai 'kg' per liquidi o bevande dosate in millilitri! Per farine e solidi in 'g' o 'kg' usa 'kg'. Per discreti usa 'unità' o 'pz'.
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
            4. FORMATO DI RISPOSTA:
               Rispondi ESCLUSIVAMENTE con un oggetto JSON valido, senza testo introduttivo né blocchi markdown.
            5. UNITÀ DELLA RESA ('yieldUnit') E SENSO GASTRONOMICO:
               - Valuta con attenzione l'unità di resa richiesta dall'utente (es. "bicchieri", "torta", "crostata", "muffin", "panini", "fette", "bottiglie", "flûte", ecc.).
               - Se ha senso gastronomico o di servizio per il tipo di preparazione (es. "bicchieri" o "flûte" per bevande e cocktail; "torta" o "crostata" per dolci e torte intere; "panini" o "burger" per panini; "cornetti" per cornetti), DEVI MANTENERE esattamente l'unità scelta dall'utente (es. "bicchieri", "torta") e bilanciare sia le dosi che il 'baseYield' su di essa (es. 1 torta, 2 bicchieri).
               - Se invece l'unità indicata dall'utente NON HA ALCUN SENSO culinario per la ricetta richiesta (es. "bicchieri" per una pizza o una lasagna, o "torta" per un cocktail o un risotto), oppure se è generica/assente, usa come fallback "porzioni".
        """.trimIndent()
    }

    /**
     * Genera una ricetta completa da zero tramite OpenAI ChatGPT.
     */
    suspend fun generateRecipe(
        apiKey: String,
        recipeName: String,
        baseYield: String? = null,
        yieldUnit: String? = null,
        customPrompt: String? = null,
        preferredModel: String = OpenAiModel.DEFAULT.modelId
    ): Result<GeminiRecipeResponse> = withContext(Dispatchers.IO){
        val trimmedKey = apiKey.trim()
        if (trimmedKey.isBlank()){
            return@withContext Result.failure(IllegalArgumentException("Inserisci la tua API Key di OpenAI (ChatGPT)."))
        }

        val prompt = buildString{
            val userUnit = yieldUnit?.trim()?.takeIf{ it.isNotBlank() } ?: "porzioni"
            val userYieldNum = baseYield?.trim()?.takeIf{ it.isNotBlank() } ?: "4"
            append("Genera una ricetta professionale e bilanciata per: \"$recipeName\".\n")
            append("Resa base e Unità richiesta: $userYieldNum $userUnit.\n")
            append("""
                IMPORTANTE SULL'UNITÀ DELLA RESA ('yieldUnit'):
                L'utente ha impostato come unità di misura: "$userUnit".
                - Se "$userUnit" ha senso culinario o di somministrazione per la ricetta "$recipeName" (es. "torta" per un dolce/torta, "bicchieri" per un cocktail/bevanda/frullato, "panini" per panini, "crostata" per crostate, "cornetti" per cornetti), DEVI MANTENERE TASSATIVAMENTE "$userUnit" nel campo 'yieldUnit' del JSON e calcolare e bilanciare sia le dosi degli ingredienti che il valore di 'baseYield' per questa unità.
                - Se invece "$userUnit" NON ha alcun senso culinario per la ricetta "$recipeName" (es. "bicchieri" per pasta o pizza, o "torta" per una bevanda o un risotto), imposta 'yieldUnit' a "porzioni".
            """.trimIndent())
            append("\n")
            if (!customPrompt.isNullOrBlank()){
                append("Istruzioni speciali dell'utente: $customPrompt\n")
            }
            append("""
                Devi compilare:
                - 'name': nome formale della ricetta
                - 'description': breve descrizione invitante (1-2 frasi)
                - 'category': categoria appropriata (es. Primi, Secondi, Dolci, Lievitati, Panini, Pasticceria, Bevande, Antipasti)
                - 'baseYield': numero resa (es. $userYieldNum)
                - 'yieldUnit': "$userUnit" (o "porzioni" se "$userUnit" non ha senso per la ricetta)
                - 'ingredients': lista di tutti gli ingredienti con 'name', 'quantity' (numero), 'unit' (es. g, ml, pz, unità, cucchiaio, q.b.), 'pricePerUnit' (costo unitario di mercato), 'priceUnit' (kg, l, pz o unità), 'allergens' (array con ID degli allergeni UE)
                - 'instructions': testo con i passaggi del procedimento numerati (1., 2., 3...)
                
                Rispondi con un JSON conforme alla struttura:
                {
                  "name": "Nome",
                  "description": "Descrizione",
                  "category": "Dolci",
                  "baseYield": $userYieldNum,
                  "yieldUnit": "$userUnit",
                  "ingredients": [
                    { "name": "Farina 00", "quantity": 200.0, "unit": "g", "pricePerUnit": 1.20, "priceUnit": "kg", "allergens": ["GLUTINE"] }
                  ],
                  "instructions": "1. ...\n2. ..."
                }
            """.trimIndent())
        }

        callOpenAiApi(trimmedKey, prompt, preferredModel)
    }

    /**
     * Completa ingredienti mancanti o senza dose bilanciandoli, SENZA aggiungere né rimuovere ingredienti.
     */
    suspend fun completeMissingIngredients(
        apiKey: String,
        recipeName: String,
        baseYield: String = "4",
        yieldUnit: String = "porzioni",
        existingIngredients: List<GeminiIngredient>,
        customPrompt: String? = null,
        preferredModel: String = OpenAiModel.DEFAULT.modelId
    ): Result<GeminiRecipeResponse> = withContext(Dispatchers.IO){
        val trimmedKey = apiKey.trim()
        if (trimmedKey.isBlank()){
            return@withContext Result.failure(IllegalArgumentException("Inserisci la tua API Key di OpenAI (ChatGPT)."))
        }

        val prompt = buildString{
            append("Completa e perfeziona gli ingredienti per la ricetta: \"$recipeName\" (Resa: $baseYield $yieldUnit).\n")
            append("Ecco gli ingredienti attualmente inseriti dall'utente:\n")
            existingIngredients.forEach{ ing ->
                val qtyStr = if (ing.quantity <= 0.0) "MANCANTE" else "${ing.quantity} ${ing.unit}"
                append("- ${ing.name} (Quantità: $qtyStr)\n")
            }
            if (!customPrompt.isNullOrBlank()){
                append("Istruzioni speciali dell'utente: $customPrompt\n")
            }
            append("""
                COMPITI RICHIESTI:
                1. NON aggiungere nuovi ingredienti e NON rimuovere alcuno degli ingredienti inseriti. Devi elaborare ESCLUSIVAMENTE la lista fornita.
                2. Se la quantità era 'MANCANTE' o pari a zero, calcola la quantità ('quantity') e unità ('unit') ideale e perfettamente bilanciata per una resa di $baseYield $yieldUnit.
                3. Se la quantità era già presente e valida (> 0), mantienila intatta.
                4. Assegna a OGNI ingrediente un prezzo unitario realistico di mercato ('pricePerUnit' e 'priceUnit').
                5. Identifica con precisione assoluta gli allergeni UE per ogni ingrediente.
                6. Stima con precisione scientifica i 6 valori nutrizionali per 100g ('caloriesKcal', 'carbohydratesG', 'fatsG', 'proteinsG', 'waterG', 'sodiumMg') per ogni ingrediente.
                7. Genera i passaggi numerati del procedimento (1., 2...) menzionando gli ingredienti presenti.
                
                Rispondi con un JSON conforme alla struttura:
                {
                  "name": "$recipeName",
                  "baseYield": ${baseYield.toDoubleOrNull() ?: 4.0},
                  "yieldUnit": "$yieldUnit",
                  "ingredients": [
                    {
                      "name": "Nome ingrediente",
                      "quantity": 100.0,
                      "unit": "g",
                      "pricePerUnit": 1.50,
                      "priceUnit": "kg",
                      "allergens": ["GLUTINE"],
                      "caloriesKcal": 364.0,
                      "carbohydratesG": 76.3,
                      "fatsG": 1.0,
                      "proteinsG": 10.3,
                      "waterG": 14.0,
                      "sodiumMg": 2.0
                    }
                  ],
                  "instructions": "1. ...\n2. ..."
                }
            """.trimIndent())
        }

        callOpenAiApi(trimmedKey, prompt, preferredModel)
    }

    /**
     * Genera esclusivamente il procedimento numerato passo-passo.
     */
    suspend fun generateInstructions(
        apiKey: String,
        recipeName: String,
        baseYield: String? = null,
        yieldUnit: String? = null,
        ingredients: List<GeminiIngredient>,
        customPrompt: String? = null,
        preferredModel: String = OpenAiModel.DEFAULT.modelId
    ): Result<GeminiRecipeResponse> = withContext(Dispatchers.IO){
        val trimmedKey = apiKey.trim()
        if (trimmedKey.isBlank()){
            return@withContext Result.failure(IllegalArgumentException("Inserisci la tua API Key di OpenAI (ChatGPT)."))
        }

        val prompt = buildString{
            append("Scrivi il procedimento professionale passo-passo per la ricetta: \"$recipeName\".\n")
            if (!baseYield.isNullOrBlank() && !yieldUnit.isNullOrBlank()){
                append("Resa: $baseYield $yieldUnit.\n")
            }
            append("Ingredienti disponibili e dosi:\n")
            ingredients.forEach{ ing ->
                append("- ${ing.name}: ${ing.quantity} ${ing.unit}\n")
            }
            if (!customPrompt.isNullOrBlank()){
                append("Istruzioni speciali dell'utente: $customPrompt\n")
            }
            append("""
                REGOLE PER IL PROCEDIMENTO:
                - Deve essere numerato: 1., 2., 3., 4...
                - Chiaro, conciso ma ricco di dettagli tecnici (temperature, tempi, consistenze).
                - Usa solo gli ingredienti elencati sopra.
                
                Rispondi con un JSON:
                {
                  "name": "$recipeName",
                  "instructions": "1. ...\n2. ...\n3. ..."
                }
            """.trimIndent())
        }

        val response = callOpenAiApi(trimmedKey, prompt, preferredModel)
        response.map{ it.copy(ingredients = ingredients) }
    }

    /**
     * Stima con precisione scientifica i 6 valori nutrizionali per 100g per ciascun ingrediente fornito.
     * Non altera né calcola il modificatore calo peso (riservato alla ricetta completa).
     */
    suspend fun enrichNutritionalValues(
        apiKey: String,
        recipeName: String,
        ingredients: List<GeminiIngredient>,
        preferredModel: String = OpenAiModel.DEFAULT.modelId
    ): Result<GeminiRecipeResponse> = withContext(Dispatchers.IO){
        val trimmedKey = apiKey.trim()
        if (trimmedKey.isBlank()){
            return@withContext Result.failure(IllegalArgumentException("Inserisci la tua API Key di OpenAI (ChatGPT)."))
        }

        val ingredientsListText = ingredients.mapIndexed{ idx, ing ->
            "${idx + 1}. ${ing.name} (dose: ${ing.quantity} ${ing.unit})"
        }.joinToString("\n")

        val prompt = buildString{
            append("Sei un esperto nutrizionista e chimico alimentare per l'app SIMO.\n")
            append("Per la ricetta \"$recipeName\", analizza i seguenti ingredienti ed assegna a ciascuno valori nutrizionali realistici basati su tabelle ufficiali (es. CREA / USDA):\n")
            append(ingredientsListText)
            append("\n\nCOMPITI:\n")
            append("1. Per ciascun ingrediente determina con la massima accuratezza i 6 valori nutrizionali per 100g di materia prima: caloriesKcal, carbohydratesG, fatsG, proteinsG, waterG (acqua in g), sodiumMg (sodio in mg).\n")
            append("2. Restituisci ESATTAMENTE gli stessi ingredienti forniti, senza aggiungerne né ometterne alcuno.\n\n")
            append("""
                Rispondi esclusivamente con un JSON valido nel seguente formato:
                {
                  "name": "$recipeName",
                  "ingredients": [
                    {
                      "name": "Nome ingrediente",
                      "caloriesKcal": 364.0,
                      "carbohydratesG": 76.3,
                      "fatsG": 1.0,
                      "proteinsG": 10.3,
                      "waterG": 14.0,
                      "sodiumMg": 2.0
                    }
                  ]
                }
            """.trimIndent())
        }

        callOpenAiApi(trimmedKey, prompt, preferredModel)
    }

    /**
     * Esegue la chiamata HTTP all'API di OpenAI gestendo la gerarchia di fallback tra modelli.
     */
    private fun callOpenAiApi(
        apiKey: String,
        prompt: String,
        preferredModel: String = OpenAiModel.DEFAULT.modelId
    ): Result<GeminiRecipeResponse>{
        val startModel = OpenAiModel.fromModelId(preferredModel)
        val modelsToTry = OpenAiModel.getFallbackHierarchyStartingFrom(startModel)

        var lastError: Exception? = null

        for (openAiModel in modelsToTry){
            val modelName = openAiModel.modelId
            println("[OpenAiRecipeService] Tentativo richiesta con modello: $modelName")

            for (attempt in 1..2){
                try{
                    val url = URL(OPENAI_API_ENDPOINT)
                    val conn = (url.openConnection() as HttpURLConnection).apply{
                        requestMethod = "POST"
                        connectTimeout = 30000
                        readTimeout = 60000
                        doOutput = true
                        doInput = true
                        setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                        setRequestProperty("Authorization", "Bearer $apiKey")
                    }

                    // Costruzione del body JSON in formato Chat Completions OpenAI
                    val requestJson = JSONObject().apply{
                        put("model", modelName)
                        val messagesArr = JSONArray().apply{
                            put(JSONObject().apply{
                                put("role", "system")
                                put("content", SYSTEM_INSTRUCTION)
                            })
                            put(JSONObject().apply{
                                put("role", "user")
                                put("content", prompt)
                            })
                        }
                        put("messages", messagesArr)
                        put("temperature", 0.3)
                        put("response_format", JSONObject().put("type", "json_object"))
                    }

                    OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use{ writer ->
                        writer.write(requestJson.toString())
                        writer.flush()
                    }

                    val responseCode = conn.responseCode
                    println("[OpenAiRecipeService] Modello $modelName (tentativo $attempt) codice HTTP $responseCode")

                    if (responseCode == HttpURLConnection.HTTP_OK){
                        val responseText = BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use{ it.readText() }
                        val parsed = parseOpenAiResponse(responseText)
                        return Result.success(parsed)
                    } else{
                        val errorText = try{
                            BufferedReader(InputStreamReader(conn.errorStream ?: conn.inputStream, Charsets.UTF_8)).use{ it.readText() }
                        } catch (_: Exception){ "" }
                        println("[OpenAiRecipeService] Modello $modelName errore: HTTP $responseCode - $errorText")

                        val errorMsg = when (responseCode){
                            401 -> "API Key di OpenAI non valida o non autorizzata. Controlla la chiave generata su platform.openai.com."
                            429 -> "Limite di richieste o crediti esauriti su OpenAI (Quota superata). Verifica il tuo piano OpenAI."
                            500, 503 -> "I server di OpenAI ($modelName) sono temporaneamente sovraccarichi."
                            else -> "Errore OpenAI (HTTP $responseCode): $errorText"
                        }
                        lastError = RuntimeException(errorMsg)

                        // Se errore 401 (chiave errata), non serve ritentare con altri modelli
                        if (responseCode == 401){
                            return Result.failure(lastError)
                        }
                    }
                } catch (e: Exception){
                    println("[OpenAiRecipeService] Eccezione per modello $modelName (tentativo $attempt): ${e.message}")
                    lastError = e
                }
            }
        }

        return Result.failure(lastError ?: RuntimeException("Impossibile contattare i server di OpenAI. Verifica la connessione internet."))
    }

    /**
     * Estrae il contenuto JSON dalla risposta Chat Completions di OpenAI.
     */
    fun parseOpenAiResponse(jsonText: String): GeminiRecipeResponse{
        val root = JSONObject(jsonText)
        val choices = root.optJSONArray("choices") ?: throw IllegalArgumentException("Risposta OpenAI priva di 'choices'.")
        if (choices.length() == 0) throw IllegalArgumentException("Nessuna risposta ricevuta da OpenAI.")

        val firstChoice = choices.getJSONObject(0)
        val message = firstChoice.optJSONObject("message") ?: throw IllegalArgumentException("Nessun messaggio nella risposta OpenAI.")
        val content = message.optString("content").trim()
        if (content.isEmpty()) throw IllegalArgumentException("Contenuto vuoto nella risposta OpenAI.")

        val sanitized = sanitizeJson(content)
        val recipeJson = JSONObject(sanitized)
        return parseRecipeJson(recipeJson)
    }

    private fun sanitizeJson(raw: String): String{
        var cleaned = raw.trim()
        if (cleaned.startsWith("```json")){
            cleaned = cleaned.removePrefix("```json").trim()
        } else if (cleaned.startsWith("```")){
            cleaned = cleaned.removePrefix("```").trim()
        }
        if (cleaned.endsWith("```")){
            cleaned = cleaned.removeSuffix("```").trim()
        }
        return cleaned
    }

    fun parseRecipeJson(recipeJson: JSONObject): GeminiRecipeResponse{
        val name = recipeJson.optString("name").trim().takeIf{ it.isNotBlank() }
        val description = recipeJson.optString("description").trim().takeIf{ it.isNotBlank() }
        val category = recipeJson.optString("category").trim().takeIf{ it.isNotBlank() }
        val baseYield = recipeJson.optDouble("baseYield").takeIf{ !it.isNaN() && it > 0.0 }
        val rawYieldUnit = recipeJson.optString("yieldUnit")
        val yieldUnit = rawYieldUnit
            .replace(Regex("""\(.*\)"""), "")
            .trim()
            .takeIf{ it.isNotBlank() }
        val instructions = recipeJson.optString("instructions").trim().takeIf{ it.isNotBlank() }

        val ingredientsList = mutableListOf<GeminiIngredient>()
        val ingsArray = recipeJson.optJSONArray("ingredients")
        if (ingsArray != null){
            for (i in 0 until ingsArray.length()){
                val ingObj = ingsArray.getJSONObject(i)
                val ingName = ingObj.optString("name").trim()
                if (ingName.isBlank()) continue

                val rawQty = ingObj.optDouble("quantity", 0.0)
                val qty = if (rawQty.isNaN() || rawQty < 0.0) 0.0 else rawQty
                val unit = ingObj.optString("unit", "g").trim().ifEmpty{ "g" }

                val pricePerUnit = ingObj.optDouble("pricePerUnit").takeIf{ !it.isNaN() && it > 0.0 }
                val priceUnit = ingObj.optString("priceUnit").trim().takeIf{ it.isNotBlank() }

                val allergensArr = ingObj.optJSONArray("allergens")
                val allergens = parseAllergens(allergensArr)

                val caloriesKcal = if (ingObj.has("caloriesKcal") && !ingObj.isNull("caloriesKcal")) ingObj.optDouble("caloriesKcal", 0.0).takeIf{ it >= 0.0 } else null
                val carbohydratesG = if (ingObj.has("carbohydratesG") && !ingObj.isNull("carbohydratesG")) ingObj.optDouble("carbohydratesG", 0.0).takeIf{ it >= 0.0 } else null
                val fatsG = if (ingObj.has("fatsG") && !ingObj.isNull("fatsG")) ingObj.optDouble("fatsG", 0.0).takeIf{ it >= 0.0 } else null
                val proteinsG = if (ingObj.has("proteinsG") && !ingObj.isNull("proteinsG")) ingObj.optDouble("proteinsG", 0.0).takeIf{ it >= 0.0 } else null
                val waterG = if (ingObj.has("waterG") && !ingObj.isNull("waterG")) ingObj.optDouble("waterG", 0.0).takeIf{ it >= 0.0 } else null
                val sodiumMg = if (ingObj.has("sodiumMg") && !ingObj.isNull("sodiumMg")) ingObj.optDouble("sodiumMg", 0.0).takeIf{ it >= 0.0 } else null

                ingredientsList.add(
                    GeminiIngredient(
                        name = ingName,
                        quantity = qty,
                        unit = unit,
                        pricePerUnit = pricePerUnit,
                        priceUnit = priceUnit,
                        allergens = allergens,
                        caloriesKcal = caloriesKcal,
                        carbohydratesG = carbohydratesG,
                        fatsG = fatsG,
                        proteinsG = proteinsG,
                        waterG = waterG,
                        sodiumMg = sodiumMg
                    )
                )
            }
        }

        val cookingWeightModifier = if (recipeJson.has("cookingWeightModifier") && !recipeJson.isNull("cookingWeightModifier")){
            val mod = recipeJson.optDouble("cookingWeightModifier", 0.0)
            if (mod.isFinite()) mod else null
        } else null

        return GeminiRecipeResponse(
            recipeName = name,
            description = description,
            category = category,
            baseYield = baseYield,
            yieldUnit = yieldUnit,
            ingredients = ingredientsList,
            instructions = instructions,
            cookingWeightModifier = cookingWeightModifier
        )
    }

    fun parseAllergens(jsonArray: JSONArray?): List<Allergen>{
        if (jsonArray == null) return emptyList()
        val result = mutableSetOf<Allergen>()
        for (i in 0 until jsonArray.length()){
            val raw = jsonArray.optString(i).trim().uppercase()
            if (raw.isBlank()) continue

            val matched = Allergen.entries.firstOrNull{ allergen ->
                allergen.name == raw ||
                allergen.name.replace("_", "") == raw.replace("_", "").replace(" ", "") ||
                allergen.displayName.uppercase() == raw ||
                allergen.displayName.uppercase().contains(raw) ||
                raw.contains(allergen.displayName.uppercase())
            }
            if (matched != null){
                result.add(matched)
            }
        }
        return result.toList()
    }
}
