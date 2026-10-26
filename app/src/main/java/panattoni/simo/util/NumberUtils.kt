package panattoni.simo.util

import java.util.Locale

object NumberUtils{

    /**
     * Converte in modo sicuro una stringa numerica in Double, supportando sia virgola ',' che punto '.'.
     * Elimina spazi, valida che sia un numero finito e non NaN.
     */
    fun parseDoubleSafe(input: String?, default: Double = 0.0): Double{
        if (input.isNullOrBlank()) return default
        return try{
            val sanitized = input.trim().replace(',', '.')
            val value = sanitized.toDouble()
            if (value.isFinite() && !value.isNaN()) value else default
        } catch (_: Exception){
            default
        }
    }

    /**
     * Converte in modo sicuro una stringa numerica in Double?, ritornando null se non è valido.
     */
    fun parseDoubleOrNull(input: String?): Double?{
        if (input.isNullOrBlank()) return null
        return try{
            val sanitized = input.trim().replace(',', '.')
            val value = sanitized.toDouble()
            if (value.isFinite() && !value.isNaN()) value else null
        } catch (_: Exception){
            null
        }
    }

    /**
     * Verifica se la stringa rappresenta un numero positivo valido (> 0 e finito).
     */
    fun isValidPositiveNumber(input: String?): Boolean{
        val parsed = parseDoubleOrNull(input) ?: return false
        return parsed > 0.0 && parsed.isFinite()
    }

    /**
     * Formatta un numero Double per l'interfaccia utente:
     * - Se intero (es. 2.0), mostra "2"
     * - Se decimale (es. 1.5000), mostra al massimo 2 cifre decimali senza zeri finali superflui
     */
    fun formatAmount(amount: Double): String{
        if (!amount.isFinite() || amount.isNaN()) return "0"
        val rounded = Math.round(amount * 100.0) / 100.0
        return if (Math.abs(rounded - Math.round(rounded)) < 0.001){
            Math.round(rounded).toInt().toString()
        } else{
            // Formattazione con 1 o 2 decimali
            val formatted = String.format(Locale.US, "%.2f", rounded)
            if (formatted.endsWith("0")){
                formatted.dropLast(1)
            } else{
                formatted
            }
        }
    }

    /**
     * Formatta la dose in base all'unità:
     * - "q.b." non ha numero
     * - unità intere (unità, pezzi, cucchiai, cucchiaini) vengono approssimate per difetto se < .5, per eccesso se >= .5 e mai con la virgola
     * - altre unità mantengono decimali se necessario
     */
    fun formatQuantityForUnit(amount: Double, unitCode: String): String{
        if (panattoni.simo.domain.MeasurementUnit.isQb(unitCode)){
            return ""
        }
        if (panattoni.simo.domain.MeasurementUnit.isIntegerUnit(unitCode)){
            return panattoni.simo.domain.MeasurementUnit.roundIntegerAmount(amount).toString()
        }
        return formatAmount(amount)
    }
}
