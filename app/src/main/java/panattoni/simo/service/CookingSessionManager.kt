package panattoni.simo.service

import android.content.Context
import android.os.Build
import panattoni.simo.data.model.ScaledRecipeTree
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Modello di dati per un timer da cucina con temporizzazione basata su timestamp reale (endTimeMillis).
 * Questo garantisce che il tempo trascorra con precisione assoluta anche quando l'app è in background,
 * con lo schermo spento o mentre l'utente guarda video su YouTube.
 */
data class ActiveKitchenTimer(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val totalSeconds: Int,
    val remainingSeconds: Int = totalSeconds,
    val isRunning: Boolean = true,
    val isFinished: Boolean = false,
    val endTimeMillis: Long = System.currentTimeMillis() + (totalSeconds * 1000L),
    val pausedRemainingSeconds: Int? = null
){
    fun currentRemainingSeconds(): Int{
        if (isFinished) return 0
        return if (!isRunning){
            pausedRemainingSeconds ?: remainingSeconds
        } else{
            val remaining = ((endTimeMillis - System.currentTimeMillis()) / 1000).toInt()
            Math.max(0, remaining)
        }
    }
}

/**
 * Stato persistente della sessione di cottura attiva (Cooking Mode).
 */
data class CookingSessionState(
    val recipeId: String,
    val recipeName: String,
    val targetYieldQuantity: Double,
    val targetYieldUnit: String,
    val scalingFactor: Double = 1.0,
    val selectedTab: Int = 0,
    val checkedIngredients: Set<String> = emptySet(),
    val checkedSteps: Set<Int> = emptySet(),
    val activeTimers: List<ActiveKitchenTimer> = emptyList()
)

/**
 * Singleton centrale per la gestione resiliente della sessione di Cooking Mode.
 * Salva lo stato in SharedPreferences ad ogni modifica e coordina il Foreground Service,
 * impedendo che il sistema operativo Android scarichi la memoria o cancelli i timer in background.
 */
object CookingSessionManager{

    private const val PREFS_NAME = "simo_cooking_session_prefs"
    private const val KEY_SESSION_JSON = "active_session_json"

    private val _sessionState = MutableStateFlow<CookingSessionState?>(null)
    val sessionState: StateFlow<CookingSessionState?> = _sessionState.asStateFlow()

    // Cache in memoria dell'albero scalato per evitare ricalcoli inutili
    var activeTree: ScaledRecipeTree? = null
        private set

    fun init(context: Context){
        loadSessionFromPrefs(context)
        // Se c'erano timer in esecuzione prima della chiusura/kill, avvia il service per continuare
        if (hasRunningTimers()){
            CookingTimerService.start(context)
        }
    }

    fun hasActiveSession(): Boolean{
        return _sessionState.value != null
    }

    fun hasRunningTimers(): Boolean{
        val timers = _sessionState.value?.activeTimers ?: return false
        return timers.any{ it.isRunning && !it.isFinished }
    }

    fun startSession(context: Context, tree: ScaledRecipeTree, scalingFactor: Double){
        activeTree = tree
        val existing = _sessionState.value
        val newState = if (existing != null && existing.recipeId == tree.recipe.id){
            // Mantiene i timer e le spunte già attive se si tratta della stessa ricetta
            existing.copy(
                scalingFactor = scalingFactor,
                targetYieldQuantity = tree.targetYieldQuantity,
                targetYieldUnit = tree.recipe.yieldUnit
            )
        } else{
            CookingSessionState(
                recipeId = tree.recipe.id,
                recipeName = tree.recipe.name,
                targetYieldQuantity = tree.targetYieldQuantity,
                targetYieldUnit = tree.recipe.yieldUnit,
                scalingFactor = scalingFactor
            )
        }
        _sessionState.value = newState
        saveSessionToPrefs(context)
    }

    fun stopSession(context: Context){
        activeTree = null
        _sessionState.value = null
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_SESSION_JSON).apply()
        CookingTimerService.stop(context)
    }

    fun setSelectedTab(context: Context, tab: Int){
        val current = _sessionState.value ?: return
        _sessionState.value = current.copy(selectedTab = tab)
        saveSessionToPrefs(context)
    }

    fun toggleIngredient(context: Context, key: String){
        val current = _sessionState.value ?: return
        val updated = if (current.checkedIngredients.contains(key)){
            current.checkedIngredients - key
        } else{
            current.checkedIngredients + key
        }
        _sessionState.value = current.copy(checkedIngredients = updated)
        saveSessionToPrefs(context)
    }

    fun toggleStep(context: Context, stepIndex: Int){
        val current = _sessionState.value ?: return
        val updated = if (current.checkedSteps.contains(stepIndex)){
            current.checkedSteps - stepIndex
        } else{
            current.checkedSteps + stepIndex
        }
        _sessionState.value = current.copy(checkedSteps = updated)
        saveSessionToPrefs(context)
    }

    fun addTimer(context: Context, label: String, totalSeconds: Int){
        val current = _sessionState.value ?: return
        val newTimer = ActiveKitchenTimer(
            label = label.ifBlank{ "Timer ${current.activeTimers.size + 1}" },
            totalSeconds = totalSeconds,
            remainingSeconds = totalSeconds,
            isRunning = true,
            isFinished = false,
            endTimeMillis = System.currentTimeMillis() + (totalSeconds * 1000L),
            pausedRemainingSeconds = null
        )
        _sessionState.value = current.copy(activeTimers = current.activeTimers + newTimer)
        saveSessionToPrefs(context)
        CookingTimerService.start(context)
    }

    fun togglePauseTimer(context: Context, timerId: String){
        val current = _sessionState.value ?: return
        val updated = current.activeTimers.map{ timer ->
            if (timer.id == timerId){
                if (timer.isRunning){
                    // Mette in pausa: congela i secondi rimanenti attuali
                    val rem = timer.currentRemainingSeconds()
                    timer.copy(
                        isRunning = false,
                        remainingSeconds = rem,
                        pausedRemainingSeconds = rem
                    )
                } else{
                    // Riprende: imposta il nuovo endTimeMillis partendo da adesso
                    val rem = timer.pausedRemainingSeconds ?: timer.remainingSeconds
                    timer.copy(
                        isRunning = true,
                        remainingSeconds = rem,
                        endTimeMillis = System.currentTimeMillis() + (rem * 1000L),
                        pausedRemainingSeconds = null
                    )
                }
            } else{
                timer
            }
        }
        _sessionState.value = current.copy(activeTimers = updated)
        saveSessionToPrefs(context)
        if (hasRunningTimers()){
            CookingTimerService.start(context)
        }
    }

    fun resetTimer(context: Context, timerId: String){
        val current = _sessionState.value ?: return
        val updated = current.activeTimers.map{ timer ->
            if (timer.id == timerId){
                timer.copy(
                    remainingSeconds = timer.totalSeconds,
                    isRunning = false,
                    isFinished = false,
                    endTimeMillis = System.currentTimeMillis() + (timer.totalSeconds * 1000L),
                    pausedRemainingSeconds = timer.totalSeconds
                )
            } else{
                timer
            }
        }
        _sessionState.value = current.copy(activeTimers = updated)
        saveSessionToPrefs(context)
    }

    fun deleteTimer(context: Context, timerId: String){
        val current = _sessionState.value ?: return
        val updated = current.activeTimers.filterNot{ it.id == timerId }
        _sessionState.value = current.copy(activeTimers = updated)
        saveSessionToPrefs(context)
        if (!hasRunningTimers() && updated.none{ it.isFinished }){
            CookingTimerService.stop(context)
        }
    }

    fun addSecondsToTimer(context: Context, timerId: String, addedSeconds: Int){
        val current = _sessionState.value ?: return
        val updated = current.activeTimers.map{ timer ->
            if (timer.id == timerId){
                val currentRem = timer.currentRemainingSeconds()
                val newRem = currentRem + addedSeconds
                val newTotal = timer.totalSeconds + addedSeconds
                val newEndTime = if (timer.isRunning){
                    System.currentTimeMillis() + (newRem * 1000L)
                } else{
                    timer.endTimeMillis + (addedSeconds * 1000L)
                }
                timer.copy(
                    remainingSeconds = newRem,
                    totalSeconds = newTotal,
                    endTimeMillis = newEndTime,
                    pausedRemainingSeconds = if (!timer.isRunning) newRem else null,
                    isFinished = false
                )
            } else{
                timer
            }
        }
        _sessionState.value = current.copy(activeTimers = updated)
        saveSessionToPrefs(context)
        CookingTimerService.start(context)
    }

    fun dismissAlarm(context: Context, timerId: String){
        val current = _sessionState.value ?: return
        val updated = current.activeTimers.map{ timer ->
            if (timer.id == timerId){
                timer.copy(isFinished = false)
            } else{
                timer
            }
        }
        _sessionState.value = current.copy(activeTimers = updated)
        saveSessionToPrefs(context)
        CookingTimerService.stopAlarmSound(context)
    }

    /**
     * Esegue il ricalcolo al secondo basato sul clock reale.
     * Ritorna true se almeno un timer è appena terminato (ha suonato).
     */
    fun tick(context: Context): Boolean{
        val current = _sessionState.value ?: return false
        var newlyFinished = false
        var changed = false

        val updated = current.activeTimers.map{ timer ->
            if (timer.isRunning && !timer.isFinished){
                val rem = timer.currentRemainingSeconds()
                if (rem <= 0){
                    newlyFinished = true
                    changed = true
                    timer.copy(
                        remainingSeconds = 0,
                        isRunning = false,
                        isFinished = true,
                        pausedRemainingSeconds = 0
                    )
                } else if (rem != timer.remainingSeconds){
                    changed = true
                    timer.copy(remainingSeconds = rem)
                } else{
                    timer
                }
            } else{
                timer
            }
        }

        if (changed){
            _sessionState.value = current.copy(activeTimers = updated)
            saveSessionToPrefs(context)
        }
        return newlyFinished
    }

    private fun saveSessionToPrefs(context: Context){
        val session = _sessionState.value ?: return
        try{
            val json = JSONObject().apply{
                put("recipeId", session.recipeId)
                put("recipeName", session.recipeName)
                put("targetYieldQuantity", session.targetYieldQuantity)
                put("targetYieldUnit", session.targetYieldUnit)
                put("scalingFactor", session.scalingFactor)
                put("selectedTab", session.selectedTab)

                val ingArray = JSONArray()
                session.checkedIngredients.forEach{ ingArray.put(it) }
                put("checkedIngredients", ingArray)

                val stepArray = JSONArray()
                session.checkedSteps.forEach{ stepArray.put(it) }
                put("checkedSteps", stepArray)

                val timersArray = JSONArray()
                session.activeTimers.forEach{ timer ->
                    val tObj = JSONObject().apply{
                        put("id", timer.id)
                        put("label", timer.label)
                        put("totalSeconds", timer.totalSeconds)
                        put("remainingSeconds", timer.currentRemainingSeconds())
                        put("isRunning", timer.isRunning)
                        put("isFinished", timer.isFinished)
                        put("endTimeMillis", timer.endTimeMillis)
                        if (timer.pausedRemainingSeconds != null){
                            put("pausedRemainingSeconds", timer.pausedRemainingSeconds)
                        }
                    }
                    timersArray.put(tObj)
                }
                put("activeTimers", timersArray)
            }

            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_SESSION_JSON, json.toString()).apply()
        } catch (_: Exception){}
    }

    private fun loadSessionFromPrefs(context: Context){
        try{
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val jsonStr = prefs.getString(KEY_SESSION_JSON, null) ?: return
            val json = JSONObject(jsonStr)

            val recipeId = json.getString("recipeId")
            val recipeName = json.optString("recipeName", "")
            val targetYieldQuantity = json.optDouble("targetYieldQuantity", 1.0)
            val targetYieldUnit = json.optString("targetYieldUnit", "porzioni")
            val scalingFactor = json.optDouble("scalingFactor", 1.0)
            val selectedTab = json.optInt("selectedTab", 0)

            val checkedIngredients = mutableSetOf<String>()
            val ingArray = json.optJSONArray("checkedIngredients")
            if (ingArray != null){
                for (i in 0 until ingArray.length()){
                    checkedIngredients.add(ingArray.getString(i))
                }
            }

            val checkedSteps = mutableSetOf<Int>()
            val stepArray = json.optJSONArray("checkedSteps")
            if (stepArray != null){
                for (i in 0 until stepArray.length()){
                    checkedSteps.add(stepArray.getInt(i))
                }
            }

            val timers = mutableListOf<ActiveKitchenTimer>()
            val timersArray = json.optJSONArray("activeTimers")
            if (timersArray != null){
                val now = System.currentTimeMillis()
                for (i in 0 until timersArray.length()){
                    val tObj = timersArray.getJSONObject(i)
                    val id = tObj.optString("id", UUID.randomUUID().toString())
                    val label = tObj.optString("label", "Timer")
                    val totalSecs = tObj.optInt("totalSeconds", 300)
                    val isRunning = tObj.optBoolean("isRunning", false)
                    val wasFinished = tObj.optBoolean("isFinished", false)
                    val endTimeMillis = tObj.optLong("endTimeMillis", now)
                    val pausedRemaining = if (tObj.has("pausedRemainingSeconds")) tObj.getInt("pausedRemainingSeconds") else null

                    val currentRem = if (wasFinished){
                        0
                    } else if (!isRunning){
                        pausedRemaining ?: tObj.optInt("remainingSeconds", totalSecs)
                    } else{
                        Math.max(0, ((endTimeMillis - now) / 1000).toInt())
                    }
                    val isFinished = wasFinished || (isRunning && currentRem <= 0)

                    timers.add(
                        ActiveKitchenTimer(
                            id = id,
                            label = label,
                            totalSeconds = totalSecs,
                            remainingSeconds = currentRem,
                            isRunning = isRunning && !isFinished,
                            isFinished = isFinished,
                            endTimeMillis = endTimeMillis,
                            pausedRemainingSeconds = pausedRemaining
                        )
                    )
                }
            }

            _sessionState.value = CookingSessionState(
                recipeId = recipeId,
                recipeName = recipeName,
                targetYieldQuantity = targetYieldQuantity,
                targetYieldUnit = targetYieldUnit,
                scalingFactor = scalingFactor,
                selectedTab = selectedTab,
                checkedIngredients = checkedIngredients,
                checkedSteps = checkedSteps,
                activeTimers = timers
            )
        } catch (_: Exception){
            _sessionState.value = null
        }
    }
}
