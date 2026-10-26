package panattoni.simo.util

import java.util.regex.Pattern

data class ParsedTimerPrompt(
    val totalSeconds: Int,
    val displayLabel: String,
    val contextNote: String = ""
)

/**
 * Estrattore automatico dei tempi di cottura e lievitazione dal testo delle istruzioni.
 * Riconosce durate in ore, minuti e secondi per generare pulsanti timer interattivi.
 */
object CookingTimerExtractor{

    // Regex per individuare espressioni come:
    // "15 minuti", "15 min", "1 ora e 30 minuti", "90 secondi", "35-40 minuti", "35 m", "30''", "45'"
    private val TIME_REGEX = Pattern.compile(
        """(?i)(?:per|circa|almeno|massimo)?\s*(\d+(?:[.,]\d+)?)\s*(?:-|a|/)?\s*(\d+(?:[.,]\d+)?)?\s*(ore|ora|h|minuti|minuto|min|m|secondi|secondo|sec|s)\b"""
    )

    /**
     * Analizza una riga di testo/istruzione e restituisce tutti i timer rilevati nel testo.
     */
    fun extractTimersFromStep(text: String): List<ParsedTimerPrompt>{
        val results = mutableListOf<ParsedTimerPrompt>()
        val matcher = TIME_REGEX.matcher(text)

        while (matcher.find()){
            val numStr1 = matcher.group(1)?.replace(',', '.') ?: continue
            val numStr2 = matcher.group(2)?.replace(',', '.')
            val unitStr = matcher.group(3)?.lowercase() ?: continue

            val val1 = numStr1.toDoubleOrNull() ?: continue
            // Se c'è un intervallo "35-40 minuti", prendiamo il valore iniziale o medio (es. 35)
            val effectiveValue = val1

            val seconds = when{
                unitStr.startsWith("ora") || unitStr.startsWith("ore") || unitStr == "h" -> (effectiveValue * 3600).toInt()
                unitStr.startsWith("min") || unitStr == "m" -> (effectiveValue * 60).toInt()
                unitStr.startsWith("sec") || unitStr == "s" -> effectiveValue.toInt()
                else -> 0
            }

            if (seconds in 5..86400){ // Tra 5 secondi e 24 ore
                val displayLabel = formatDurationShort(seconds)
                results.add(
                    ParsedTimerPrompt(
                        totalSeconds = seconds,
                        displayLabel = displayLabel,
                        contextNote = matcher.group(0)?.trim() ?: displayLabel
                    )
                )
            }
        }
        return results
    }

    fun formatDurationShort(seconds: Int): String{
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return when{
            h > 0 && m > 0 -> "${h}h ${m}m"
            h > 0 -> "${h}h"
            m > 0 && s > 0 -> "${m}m ${s}s"
            m > 0 -> "${m}m"
            else -> "${s}s"
        }
    }
}
