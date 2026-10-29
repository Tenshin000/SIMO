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
    val allergens: List<Allergen> = emptyList(),
    // Valori nutrizionali per 100g di ingrediente crudo
    val caloriesKcal: Double? = null,
    val carbohydratesG: Double? = null,
    val fatsG: Double? = null,
    val proteinsG: Double? = null,
    val waterG: Double? = null,
    val sodiumMg: Double? = null
)

data class GeminiRecipeResponse(
    val recipeName: String? = null,
    val description: String? = null,
    val category: String? = null,
    val baseYield: Double? = null,
    val yieldUnit: String? = null,
    val ingredients: List<GeminiIngredient> = emptyList(),
    val instructions: String? = null,
    // Modificatore peso in cottura in percentuale (es. -12.0 per calo del 12%, +120.0 per pasta/riso)
    val cookingWeightModifier: Double? = null
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
               Se l'ingrediente non contiene nessuno di questi (es. zucchero, acqua, sale, frutta fresca semplice), la lista allergeni deve essere vuota [].
            4. PROCEDIMENTO NUMERATO:
               - I passaggi devono essere rigorosamente numerati ("1. ...", "2. ...", "3. ...").
               - I passaggi DEVONO essere strettamente relativi agli ingredienti definiti, citandoli esplicitamente nei passaggi corrispondenti.
            5. VALORI NUTRIZIONALI PER 100g E MODIFICATORE COTTURA:
               - Per ciascun ingrediente, stima con la massima fedeltà e realismo i 6 valori nutrizionali per 100g di materia prima:
                 'caloriesKcal' (calorie kcal), 'carbohydratesG' (carboidrati g), 'fatsG' (grassi g), 'proteinsG' (proteine g), 'waterG' (acqua g), 'sodiumMg' (sodio mg).
               - Per la ricetta completa, stima 'cookingWeightModifier' (% di calo peso o resa in cottura):
                 es. -12.0 per evaporazione/calo in forno, +120.0 per pasta o riso che raddoppiano, 0.0 per piatti a freddo.
            6. UNITÀ DELLA RESA ('yieldUnit') E SENSO GASTRONOMICO:
               - Valuta con attenzione l'unità di resa richiesta dall'utente (es. "bicchieri", "torta", "crostata", "muffin", "panini", "fette", "bottiglie", "flûte", ecc.).
               - Se ha senso gastronomico o di servizio per il tipo di preparazione (es. "bicchieri" o "flûte" per bevande e cocktail; "torta" o "crostata" per dolci e torte intere; "panini" o "burger" per panini; "cornetti" per cornetti), DEVI MANTENERE esattamente l'unità scelta dall'utente (es. "bicchieri", "torta") e bilanciare sia le dosi che il 'baseYield' su di essa (es. 1 torta, 2 bicchieri).
               - Se invece l'unità indicata dall'utente NON HA ALCUN SENSO culinario per la ricetta richiesta (es. "bicchieri" per una pizza o una lasagna, o "torta" per un cocktail o un risotto), oppure se è generica/assente, usa come fallback "porzioni".
            
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
            val userUnit = yieldUnit?.trim()?.takeIf{ it.isNotBlank() } ?: "porzioni"
            val userYieldNum = baseYield?.trim()?.takeIf{ it.isNotBlank() } ?: "4"
            append("Genera la ricetta completa e professionale per: \"$recipeName\".\n")
            if (!description.isNullOrBlank()){
                append("Descrizione / note dell'utente: \"$description\".\n")
            }
            if (!category.isNullOrBlank()){
                append("Categoria desiderata: \"$category\".\n")
            }
            append("Resa base e Unità richiesta: $userYieldNum $userUnit.\n")
            append("""
                IMPORTANTE SULL'UNITÀ DELLA RESA ('yieldUnit'):
                L'utente ha impostato come unità di misura: "$userUnit".
                - Se "$userUnit" ha senso culinario o di somministrazione per la ricetta "$recipeName" (es. "torta" per un dolce/torta, "bicchieri" per una bevanda/cocktail/frullato, "panini" per panini, "crostata" per crostate, "cornetti" per cornetti), DEVI MANTENERE TASSATIVAMENTE "$userUnit" nel campo 'yieldUnit' del JSON e calcolare e bilanciare sia le dosi degli ingredienti che il valore di 'baseYield' per questa unità.
                - Se invece "$userUnit" NON ha alcun senso culinario per la ricetta "$recipeName" (es. "bicchieri" per pasta o pizza, o "torta" per una bevanda o un risotto), imposta 'yieldUnit' a "porzioni".
            """.trimIndent())
            append("\n")
            if (!userCustomPrompt.isNullOrBlank()){
                append("Istruzioni speciali dell'utente: \"$userCustomPrompt\".\n")
            }
            append("""
                Rispondi con un JSON che rispetti questo schema esatto:
               {
                  "recipeName": "$recipeName",
                  "description": "Breve descrizione professionale accattivante",
                  "category": "Una tra Dolci, Pizze, Panetteria, Panini, Antipasti, Primi, Secondi, Contorni, Zuppe, Alcolici, Analcolici, Caffetteria, Altro",
                  "baseYield": $userYieldNum,
                  "yieldUnit": "$userUnit",
                  "cookingWeightModifier": -10.0,
                  "ingredients": [
                   {
                      "name": "Nome ingrediente",
                      "quantity": 250.0,
                      "unit": "g" o "ml" o "unità" o "pz" o "q.b.",
                      "pricePerUnit": 1.20,
                      "priceUnit": "kg" o "l" o "unità",
                      "allergens": ["GLUTINE"],
                      "caloriesKcal": 364.0,
                      "carbohydratesG": 76.3,
                      "fatsG": 1.0,
                      "proteinsG": 10.3,
                      "waterG": 14.0,
                      "sodiumMg": 2.0
                    }
                  ],
                  "instructions": "1. Primo passaggio...\n2. Secondo passaggio..."
                }
            """.trimIndent())
        }

        callGeminiApi(trimmedKey, prompt, preferredModel)
    }

    /**
     * Completa gli ingredienti mancanti o parziali (pesature, costi, allergeni, valori nutrizionali)
     * e genera le istruzioni, SENZA aggiungere né rimuovere ingredienti rispetto a quelli passati dall'utente.
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
            append("Completa e perfeziona gli ingredienti per la ricetta: \"$recipeName\" (Resa richiesta: $baseYield $yieldUnit).\n")
            if (!description.isNullOrBlank()){
                append("Descrizione: \"$description\".\n")
            }
            append("Ingredienti forniti dall'utente:\n$ingredientsDescription\n\n")
            append("COMPITI RICHIESTI:\n")
            append("1. NON aggiungere nuovi ingredienti e NON rimuovere nessuno degli ingredienti forniti. Devi completare ed elaborare ESCLUSIVAMENTE la lista fornita dall'utente.\n")
            append("2. Per ogni ingrediente con quantità 'MANCANTE' o pari a zero, calcola la pesatura ('quantity') e unità ('unit') ideale, bilanciata e realistica per la resa di $baseYield $yieldUnit.\n")
            append("3. Se un ingrediente ha già una quantità specificata e valida (> 0), mantienila intatta.\n")
            append("4. Se mancano costo unitario ('pricePerUnit' / 'priceUnit'), assegna prezzi realistici di mercato in euro.\n")
            append("5. Rileva e assegna gli allergeni UE per ciascun ingrediente (tra i 14 allergeni ufficiali UE).\n")
            append("6. Stima con il massimo realismo e fedeltà scientifica i 6 valori nutrizionali per 100g ('caloriesKcal', 'carbohydratesG', 'fatsG', 'proteinsG', 'waterG', 'sodiumMg') per ogni ingrediente.\n")
            append("7. Scrivi il procedimento di preparazione passo-passo numerato ('1. ...', '2. ...') menzionando gli ingredienti.\n")
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

        callGeminiApi(trimmedKey, prompt, preferredModel)
    }

    /**
     * Stima con precisione scientifica i 6 valori nutrizionali per 100g per ciascun ingrediente fornito.
     * Non altera né calcola il modificatore calo peso (riservato alla ricetta completa).
     */
    suspend fun enrichNutritionalValues(
        apiKey: String,
        recipeName: String,
        ingredients: List<GeminiIngredient>,
        preferredModel: String = GeminiModel.DEFAULT.modelId
    ): Result<GeminiRecipeResponse> = withContext(Dispatchers.IO){
        val trimmedKey = apiKey.trim()
        if (trimmedKey.isBlank()){
            return@withContext Result.failure(IllegalArgumentException("Inserisci la tua API Key di Google Gemini."))
        }

        val ingredientsListText = ingredients.mapIndexed{ idx, ing ->
            "${idx + 1}. ${ing.name} (dose attuale: ${ing.quantity} ${ing.unit})"
        }.joinToString("\n")

        val prompt = buildString{
            append("Sei un esperto nutrizionista e chimico alimentare per l'app SIMO.\n")
            append("Per la ricetta \"$recipeName\", analizza i seguenti ingredienti ed assegna a ciascuno valori nutrizionali il più realistici possibile, basandoti sulle tabelle nutrizionali ufficiali (es. CREA / USDA):\n")
            append(ingredientsListText)
            append("\n\nCOMPITI:\n")
            append("1. Per ciascun ingrediente determina con la massima accuratezza i 6 valori nutrizionali per 100g di materia prima: caloriesKcal, carbohydratesG, fatsG, proteinsG, waterG (acqua in g), sodiumMg (sodio in mg).\n")
            append("2. Restituisci ESATTAMENTE gli stessi ingredienti forniti, senza aggiungerne né ometterne alcuno.\n\n")
            append("""
                Rispondi esclusivamente con un JSON valido nel seguente formato:
                {
                  "recipeName": "$recipeName",
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
        val yieldUnit = recipeJson.optString("yieldUnit")
            .replace(Regex("""\(.*\)"""), "")
            .trim()
            .takeIf{ it.isNotBlank() }

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

                val caloriesKcal = if (ingObj.has("caloriesKcal") && !ingObj.isNull("caloriesKcal")) ingObj.optDouble("caloriesKcal", 0.0).takeIf{ it >= 0.0 } else null
                val carbohydratesG = if (ingObj.has("carbohydratesG") && !ingObj.isNull("carbohydratesG")) ingObj.optDouble("carbohydratesG", 0.0).takeIf{ it >= 0.0 } else null
                val fatsG = if (ingObj.has("fatsG") && !ingObj.isNull("fatsG")) ingObj.optDouble("fatsG", 0.0).takeIf{ it >= 0.0 } else null
                val proteinsG = if (ingObj.has("proteinsG") && !ingObj.isNull("proteinsG")) ingObj.optDouble("proteinsG", 0.0).takeIf{ it >= 0.0 } else null
                val waterG = if (ingObj.has("waterG") && !ingObj.isNull("waterG")) ingObj.optDouble("waterG", 0.0).takeIf{ it >= 0.0 } else null
                val sodiumMg = if (ingObj.has("sodiumMg") && !ingObj.isNull("sodiumMg")) ingObj.optDouble("sodiumMg", 0.0).takeIf{ it >= 0.0 } else null

                ingredientsList.add(
                    GeminiIngredient(
                        name = name,
                        quantity = safeQty,
                        unit = unit,
                        pricePerUnit = price,
                        priceUnit = cleanPriceUnit,
                        allergens = allergens.distinct(),
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
            recipeName = recipeName,
            description = description,
            category = category,
            baseYield = baseYield,
            yieldUnit = yieldUnit,
            ingredients = ingredientsList,
            instructions = instructions,
            cookingWeightModifier = cookingWeightModifier
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
