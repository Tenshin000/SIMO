package panattoni.simo

import android.app.Application
import android.util.Log

/**
 * Classe Application globale dell'app SIMO.
 * Inizializza l'intercettazione globale dei crash non gestiti per la diagnostica locale.
 */
class SimoApplication : Application(){
    override fun onCreate(){
        super.onCreate()
        setupGlobalExceptionHandler()
    }

    /**
     * Cattura eventuali eccezioni impreviste a runtime, salvando i dettagli
     * nelle SharedPreferences private per consentire l'ispezione degli errori.
     */
    private fun setupGlobalExceptionHandler(){
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler{ thread, throwable ->
            try{
                Log.e("SIMO_CRASH", "Eccezione non intercettata su thread ${thread.name}: ${throwable.message}", throwable)
                val prefs = getSharedPreferences("simo_crash_log", MODE_PRIVATE)
                prefs.edit()
                    .putString("last_crash_message", throwable.message ?: "Eccezione sconosciuta")
                    .putString("last_crash_type", throwable.javaClass.simpleName)
                    .putLong("last_crash_time", System.currentTimeMillis())
                    .apply()
            } catch (_: Exception){
            } finally{
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }
}
