package panattoni.simo.data.repository

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import panattoni.simo.data.local.AppDatabase
import panattoni.simo.data.local.entity.ItemEntity
import panattoni.simo.data.local.entity.RecipeComponentEntity
import panattoni.simo.data.local.entity.RecipeEntity
import panattoni.simo.domain.MathEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

/**
 * Gestore dell'esportazione e importazione dei backup JSON del ricettario.
 * Include gestione dei conflitti, preservazione integrità relazionale e validazione anti-cicli.
 */
class BackupManager(private val database: AppDatabase){

    data class ConflictItem(
        val importedRecipe: RecipeEntity,
        val existingRecipe: RecipeEntity,
        var shouldReplace: Boolean = true
    )

    data class ParsedBackup(
        val items: List<ItemEntity>,
        val recipes: List<RecipeEntity>,
        val components: List<RecipeComponentEntity>,
        val conflicts: List<ConflictItem>,
        val newRecipes: List<RecipeEntity>
    )

    suspend fun exportToJson(context: Context, uri: Uri): Result<Int> = withContext(Dispatchers.IO){
        try{
            val fullDump = generateFullExportJson()
            context.contentResolver.openOutputStream(uri)?.use{ outputStream ->
                OutputStreamWriter(outputStream, Charsets.UTF_8).use{ writer ->
                    writer.write(fullDump)
                    writer.flush()
                }
            } ?: return@withContext Result.failure(IllegalStateException("Impossibile aprire il file per la scrittura. Verifica i permessi di archiviazione."))

            val recipeCount = database.recipeDao().getRecipeCount()
            Result.success(recipeCount)
        } catch (e: SecurityException){
            Result.failure(Exception("Permesso negato per il salvataggio del file: ${e.message}"))
        } catch (e: Exception){
            Result.failure(Exception("Errore durante l'esportazione: ${e.message}"))
        }
    }

    suspend fun generateFullExportJson(): String = withContext(Dispatchers.IO){
        val rootJson = JSONObject()
        rootJson.put("version", 2)
        rootJson.put("appName", "SIMO")
        rootJson.put("timestamp", System.currentTimeMillis())

        // 1. Items (Materie prime, valori nutrizionali e riferimenti ricette)
        val itemsList = database.itemDao().getAllItemsDirect()
        val itemsJson = JSONArray()
        for (item in itemsList){
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("name", item.name)
            obj.put("type", item.type)
            obj.put("defaultUnit", item.defaultUnit)
            if (item.gramsPerUnit != null){
                obj.put("gramsPerUnit", item.gramsPerUnit)
            }
            if (item.pricePerUnit != null){
                obj.put("pricePerUnit", item.pricePerUnit)
            }
            if (!item.priceUnit.isNullOrBlank()){
                obj.put("priceUnit", item.priceUnit)
            }
            if (item.allergens.isNotBlank()){
                obj.put("allergens", item.allergens)
            }
            // Nuovi 6 valori nutrizionali per 100g (Versione 2)
            if (item.caloriesKcal != null){
                obj.put("caloriesKcal", item.caloriesKcal)
            }
            if (item.carbohydratesG != null){
                obj.put("carbohydratesG", item.carbohydratesG)
            }
            if (item.fatsG != null){
                obj.put("fatsG", item.fatsG)
            }
            if (item.proteinsG != null){
                obj.put("proteinsG", item.proteinsG)
            }
            if (item.waterG != null){
                obj.put("waterG", item.waterG)
            }
            if (item.sodiumMg != null){
                obj.put("sodiumMg", item.sodiumMg)
            }
            itemsJson.put(obj)
        }
        rootJson.put("items", itemsJson)

        // 2. Ricette (con modificatore peso cottura)
        val recipesList = database.recipeDao().getAllRecipesDirect()
        val recipesJson = JSONArray()
        for (recipe in recipesList){
            val obj = JSONObject()
            obj.put("id", recipe.id)
            obj.put("name", recipe.name)
            obj.put("description", recipe.description)
            obj.put("baseYieldQuantity", recipe.baseYieldQuantity)
            obj.put("yieldUnit", recipe.yieldUnit)
            obj.put("instructions", recipe.instructions)
            obj.put("isDefault", recipe.isDefault)
            obj.put("isFavorite", recipe.isFavorite)
            obj.put("category", recipe.category)
            obj.put("createdAt", recipe.createdAt)
            // Modificatore resa/peso in cottura (Versione 2)
            obj.put("cookingWeightModifier", recipe.cookingWeightModifier)
            recipesJson.put(obj)
        }
        rootJson.put("recipes", recipesJson)

        // 3. Componenti (Collegamento ricette-ingredienti)
        val compList = database.recipeDao().getAllComponentsDirect()
        val compsJson = JSONArray()
        for (c in compList){
            val obj = JSONObject()
            obj.put("parentRecipeId", c.parentRecipeId)
            obj.put("childItemId", c.childItemId)
            obj.put("baseQuantity", c.baseQuantity)
            obj.put("unit", c.unit)
            obj.put("sortOrder", c.sortOrder)
            compsJson.put(obj)
        }
        rootJson.put("components", compsJson)

        return@withContext rootJson.toString(2)
    }

    suspend fun parseBackup(context: Context, uri: Uri): Result<ParsedBackup> = withContext(Dispatchers.IO){
        try{
            val content = StringBuilder()
            val openRes = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(IllegalStateException("Impossibile accedere al file selezionato."))

            openRes.use{ inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use{ reader ->
                    var line = reader.readLine()
                    while (line != null){
                        content.append(line).append("\n")
                        line = reader.readLine()
                    }
                }
            }

            val rawString = content.toString().trim()
            if (rawString.isEmpty()){
                return@withContext Result.failure(IllegalArgumentException("Il file selezionato è vuoto."))
            }

            val json = try{
                JSONObject(rawString)
            } catch (e: JSONException){
                return@withContext Result.failure(IllegalArgumentException("Il file non è un documento JSON valido o è danneggiato."))
            }

            val appIdentifier = when{
                json.has("appName") -> json.getString("appName")
                json.has("app") -> json.getString("app")
                else -> ""
            }

            if (appIdentifier != "SIMO"){
                return@withContext Result.failure(IllegalArgumentException("File non compatibile: non è stato generato dall'app SIMO."))
            }

            // Parsing Items
            val itemsMap = mutableMapOf<String, ItemEntity>()
            if (json.has("items")){
                val itemsArr = json.getJSONArray("items")
                for (i in 0 until itemsArr.length()){
                    val obj = itemsArr.optJSONObject(i) ?: continue
                    val id = obj.optString("id", "").trim()
                    val name = obj.optString("name", "").trim()
                    if (id.isNotEmpty() && name.isNotEmpty()){
                        val item = ItemEntity(
                            id = id,
                            name = name,
                            type = obj.optString("type", "RAW_INGREDIENT"),
                            defaultUnit = obj.optString("defaultUnit", "g"),
                            gramsPerUnit = if (obj.has("gramsPerUnit")) obj.optDouble("gramsPerUnit", 0.0).takeIf{ it > 0.0 } else null,
                            pricePerUnit = if (obj.has("pricePerUnit")) obj.optDouble("pricePerUnit", 0.0).takeIf{ it > 0.0 } else null,
                            priceUnit = if (obj.has("priceUnit")) obj.optString("priceUnit", "").trim().ifEmpty{ null } else null,
                            allergens = obj.optString("allergens", "").trim(),
                            caloriesKcal = if (obj.has("caloriesKcal") && !obj.isNull("caloriesKcal")) obj.optDouble("caloriesKcal", 0.0).takeIf{ it >= 0.0 } else null,
                            carbohydratesG = if (obj.has("carbohydratesG") && !obj.isNull("carbohydratesG")) obj.optDouble("carbohydratesG", 0.0).takeIf{ it >= 0.0 } else null,
                            fatsG = if (obj.has("fatsG") && !obj.isNull("fatsG")) obj.optDouble("fatsG", 0.0).takeIf{ it >= 0.0 } else null,
                            proteinsG = if (obj.has("proteinsG") && !obj.isNull("proteinsG")) obj.optDouble("proteinsG", 0.0).takeIf{ it >= 0.0 } else null,
                            waterG = if (obj.has("waterG") && !obj.isNull("waterG")) obj.optDouble("waterG", 0.0).takeIf{ it >= 0.0 } else null,
                            sodiumMg = if (obj.has("sodiumMg") && !obj.isNull("sodiumMg")) obj.optDouble("sodiumMg", 0.0).takeIf{ it >= 0.0 } else null
                        )
                        itemsMap[id] = item
                    }
                }
            }

            // Parsing Ricette
            val recipesToInsert = mutableListOf<RecipeEntity>()
            if (json.has("recipes")){
                val recipesArr = json.getJSONArray("recipes")
                for (i in 0 until recipesArr.length()){
                    val obj = recipesArr.optJSONObject(i) ?: continue
                    val id = obj.optString("id", "").trim()
                    val name = obj.optString("name", "").trim()
                    if (id.isNotEmpty() && name.isNotEmpty()){
                        val rawYield = obj.optDouble("baseYieldQuantity", 1.0)
                        val safeYield = if (rawYield.isFinite() && rawYield > 0.0) rawYield else 1.0
                            val isDefaultRecipe = obj.optBoolean("isDefault", false)
                            val recipe = RecipeEntity(
                                id = id,
                                name = name,
                                description = obj.optString("description", "").trim(),
                                baseYieldQuantity = safeYield,
                                yieldUnit = obj.optString("yieldUnit", "porzioni").trim().ifEmpty{ "porzioni" },
                                instructions = obj.optString("instructions", "").trim(),
                                isDefault = isDefaultRecipe,
                                isFavorite = obj.optBoolean("isFavorite", false),
                                category = obj.optString("category", "Altro").trim().ifEmpty{ "Altro" },
                                createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                                cookingWeightModifier = if (obj.has("cookingWeightModifier")) obj.optDouble("cookingWeightModifier", 0.0) else 0.0
                            )
                        recipesToInsert.add(recipe)

                        // Assicura che per ogni ricetta esista il relativo Item di tipo RECIPE nella mappa degli items
                        if (!itemsMap.containsKey(id)){
                            itemsMap[id] = ItemEntity(
                                id = id,
                                name = name,
                                type = "RECIPE",
                                defaultUnit = recipe.yieldUnit
                            )
                        }
                    }
                }
            }

            // Parsing Componenti
            val compsToInsert = mutableListOf<RecipeComponentEntity>()
            if (json.has("components")){
                val compsArr = json.getJSONArray("components")
                for (i in 0 until compsArr.length()){
                    val obj = compsArr.optJSONObject(i) ?: continue
                    val parentId = obj.optString("parentRecipeId", "").trim()
                    val childId = obj.optString("childItemId", "").trim()
                    if (parentId.isNotEmpty() && childId.isNotEmpty()){
                        val rawBaseQty = obj.optDouble("baseQuantity", 1.0)
                        val safeBaseQty = if (rawBaseQty.isFinite() && rawBaseQty >= 0.0) rawBaseQty else 0.0
                        compsToInsert.add(
                            RecipeComponentEntity(
                                parentRecipeId = parentId,
                                childItemId = childId,
                                baseQuantity = safeBaseQty,
                                unit = obj.optString("unit", "g").trim().ifEmpty{ "g" },
                                sortOrder = obj.optInt("sortOrder", i)
                            )
                        )

                        // Protezione Foreign Key: se childId non è tra gli items importati, sintetizza un placeholder
                        if (!itemsMap.containsKey(childId)){
                            itemsMap[childId] = ItemEntity(
                                id = childId,
                                name = childId.replace('_', ' ').replaceFirstChar{ if (it.isLowerCase()) it.titlecase(java.util.Locale.ROOT) else it.toString() },
                                type = "RAW_INGREDIENT",
                                defaultUnit = obj.optString("unit", "g")
                            )
                        }
                    }
                }
            }

            // Validazione Cicli nel Grafo (prevenzione loop infiniti nel backup)
            val edges = compsToInsert.map{ it.parentRecipeId to it.childItemId }
            for (comp in compsToInsert){
                if (MathEngine.wouldCreateCycle(comp.parentRecipeId, comp.childItemId, edges.filterNot{ it == (comp.parentRecipeId to comp.childItemId) })){
                    return@withContext Result.failure(
                        IllegalArgumentException("Il backup contiene una dipendenza circolare ciclica tra le ricette e non può essere importato in modo sicuro.")
                    )
                }
            }

            // Confronto con le ricette già esistenti nel database
            val existingRecipes = database.recipeDao().getAllRecipesDirect()
            val conflicts = mutableListOf<ConflictItem>()
            val newRecipes = mutableListOf<RecipeEntity>()

            for (imported in recipesToInsert){
                val match = existingRecipes.find{
                    it.id == imported.id || it.name.trim().equals(imported.name.trim(), ignoreCase = true)
                }
                if (match != null){
                    conflicts.add(ConflictItem(importedRecipe = imported, existingRecipe = match, shouldReplace = true))
                } else{
                    newRecipes.add(imported)
                }
            }

            Result.success(
                ParsedBackup(
                    items = itemsMap.values.toList(),
                    recipes = recipesToInsert,
                    components = compsToInsert,
                    conflicts = conflicts,
                    newRecipes = newRecipes
                )
            )
        } catch (e: Exception){
            Result.failure(Exception("Errore durante l'analisi del backup: ${e.message}"))
        }
    }

    suspend fun applyImport(
        backup: ParsedBackup,
        replaceAllConflicts: Boolean? = null,
        specificReplacements: Map<String, Boolean>? = null
    ): Result<Int> = withContext(Dispatchers.IO){
        try{
            val recipesToCommit = mutableListOf<RecipeEntity>()
            val recipeIdsToCleanOld = mutableSetOf<String>()

            // 1. Le nuove ricette vengono sempre aggiunte
            recipesToCommit.addAll(backup.newRecipes)

            // 2. Risoluzione dei conflitti
            for (c in backup.conflicts){
                val replace = when{
                    replaceAllConflicts != null -> replaceAllConflicts
                    specificReplacements != null -> specificReplacements[c.importedRecipe.id] ?: c.shouldReplace
                    else -> c.shouldReplace
                }

                if (replace){
                    // Sostituisce la ricetta esistente
                    if (c.existingRecipe.id != c.importedRecipe.id){
                        recipeIdsToCleanOld.add(c.existingRecipe.id)
                    }
                    recipesToCommit.add(c.importedRecipe)
                }
            }

            val allowedRecipeIds = recipesToCommit.map{ it.id }.toSet()
            val filteredComponents = backup.components.filter{ allowedRecipeIds.contains(it.parentRecipeId) }

            database.withTransaction{
                try{
                    database.openHelper.writableDatabase.execSQL("PRAGMA defer_foreign_keys = ON")
                } catch (_: Exception){}

                // Rimuove eventuali ricette vecchie con ID differente ma stesso nome che l'utente ha scelto di sostituire
                for (oldId in recipeIdsToCleanOld){
                    database.recipeDao().deleteComponentsForRecipe(oldId)
                    database.recipeDao().deleteRecipeById(oldId)
                }

                // Rimuove i componenti precedenti per le ricette che verranno sovrascritte o inserite
                for (r in recipesToCommit){
                    database.recipeDao().deleteComponentsForRecipe(r.id)
                }

                // Upsert degli Items (tutti gli ingredienti e riferimenti)
                if (backup.items.isNotEmpty()){
                    database.itemDao().upsertItems(backup.items)
                }

                // Upsert delle Ricette
                if (recipesToCommit.isNotEmpty()){
                    database.recipeDao().upsertRecipes(recipesToCommit)
                }

                // Inserimento componenti aggiornati
                if (filteredComponents.isNotEmpty()){
                    database.recipeDao().insertComponents(filteredComponents)
                }
            }

            Result.success(recipesToCommit.size)
        } catch (e: Exception){
            Result.failure(Exception("Errore durante l'importazione nel database: ${e.message}"))
        }
    }

    suspend fun importFromJson(context: Context, uri: Uri): Result<Int> = withContext(Dispatchers.IO){
        val parseRes = parseBackup(context, uri)
        if (parseRes.isFailure){
            return@withContext Result.failure(parseRes.exceptionOrNull() ?: Exception("Errore di lettura"))
        }
        applyImport(parseRes.getOrThrow(), replaceAllConflicts = true)
    }
}
