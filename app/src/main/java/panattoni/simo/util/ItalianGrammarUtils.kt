package panattoni.simo.util

import java.util.Locale

object ItalianGrammarUtils{

    // Dizionario di unità e termini culinari frequenti (in minuscolo -> singolare)
    private val KNOWN_SINGULARS = mapOf(
        "cornetti" to "cornetto",
        "cornetto" to "cornetto",
        "porzioni" to "porzione",
        "porzione" to "porzione",
        "pizze" to "pizza",
        "pizza" to "pizza",
        "biscotti" to "biscotto",
        "biscotto" to "biscotto",
        "uova" to "uovo",
        "uovo" to "uovo",
        "fette" to "fetta",
        "fetta" to "fetta",
        "panini" to "panino",
        "panino" to "panino",
        "pezzi" to "pezzo",
        "pezzo" to "pezzo",
        "cucchiai" to "cucchiaio",
        "cucchiaio" to "cucchiaio",
        "cucchiaini" to "cucchiaino",
        "cucchiaino" to "cucchiaino",
        "bicchieri" to "bicchiere",
        "bicchiere" to "bicchiere",
        "torte" to "torta",
        "torta" to "torta",
        "croissant" to "croissant",
        "muffin" to "muffin",
        "muffins" to "muffin",
        "cupcake" to "cupcake",
        "cupcakes" to "cupcake",
        "toast" to "toast",
        "waffle" to "waffle",
        "crepes" to "crepe",
        "crepe" to "crepe",
        "crêpes" to "crêpe",
        "crêpe" to "crêpe",
        "focacce" to "focaccia",
        "focaccia" to "focaccia",
        "trecce" to "treccia",
        "treccia" to "treccia",
        "sfogliatine" to "sfogliatina",
        "sfogliatina" to "sfogliatina",
        "tortini" to "tortino",
        "tortino" to "tortino",
        "cannoli" to "cannolo",
        "cannolo" to "cannolo",
        "ciambelle" to "ciambella",
        "ciambella" to "ciambella",
        "barattoli" to "barattolo",
        "barattolo" to "barattolo",
        "pacchetti" to "pacchetto",
        "pacchetto" to "pacchetto",
        "vasetti" to "vasetto",
        "vasetto" to "vasetto",
        "bocconcini" to "bocconcino",
        "bocconcino" to "bocconcino",
        "tartellette" to "tartelletta",
        "tartelletta" to "tartelletta",
        "bignè" to "bignè",
        "babà" to "babà"
    )

    private val KNOWN_PLURALS = mapOf(
        "uovo" to "uova",
        "biscotto" to "biscotti",
        "cornetto" to "cornetti",
        "porzione" to "porzioni",
        "pizza" to "pizze",
        "fetta" to "fette",
        "panino" to "panini",
        "pezzo" to "pezzi",
        "cucchiaio" to "cucchiai",
        "cucchiaino" to "cucchiaini",
        "bicchiere" to "bicchieri",
        "torta" to "torte",
        "focaccia" to "focacce",
        "treccia" to "trecce",
        "croissant" to "croissant",
        "muffin" to "muffin",
        "toast" to "toast",
        "waffle" to "waffle",
        "bignè" to "bignè",
        "babà" to "babà"
    )

    // Unità invariabili
    private val INVARIABLE_UNITS = setOf(
        "g", "kg", "mg", "ml", "cl", "dl", "l", "q.b.", "qb"
    )

    /**
     * Converte un termine o unità al singolare secondo la lingua italiana.
     * Preserva il maiuscolo iniziale se presente.
     */
    fun toSingular(word: String): String{
        val trimmed = word.trim()
        if (trimmed.isEmpty()) return ""
        val lower = trimmed.lowercase(Locale.ITALIAN)

        if (lower in INVARIABLE_UNITS) return trimmed

        // Lookup nel dizionario culinario
        KNOWN_SINGULARS[lower]?.let{ sing ->
            return matchCase(trimmed, sing)
        }

        // Parole accentate sull'ultima sillaba o prestiti stranieri
        val lastChar = lower.last()
        if (lastChar in ACCENTED_VOWELS || isConsonant(lastChar)){
            if (lower.endsWith("s") && lower.length > 3){
                return matchCase(trimmed, lower.dropLast(1))
            }
            return trimmed
        }

        // Regole morfologiche italiane:
        // 1. -zioni / -sioni / -oni -> -zione / -sione / -one
        if (lower.endsWith("oni")){
            return matchCase(trimmed, lower.dropLast(1) + "e")
        }

        // 2. -cce / -gge -> -ccia / -ggia (focacce -> focaccia, trecce -> treccia)
        if (lower.endsWith("cce") || lower.endsWith("gge")){
            return matchCase(trimmed, lower.dropLast(1) + "ia")
        }

        // 3. -etti / -otti / -ini / -elli -> -etto / -otto / -ino / -ello
        if (lower.endsWith("etti") || lower.endsWith("otti") || lower.endsWith("ini") || lower.endsWith("elli")){
            return matchCase(trimmed, lower.dropLast(1) + "o")
        }

        // 4. -chi / -ghi -> -co / -go
        if (lower.endsWith("chi") || lower.endsWith("ghi")){
            return matchCase(trimmed, lower.dropLast(2) + "o")
        }

        // 5. -i -> -o (maschile plurale regolare)
        if (lower.endsWith("i") && lower.length > 2){
            return matchCase(trimmed, lower.dropLast(1) + "o")
        }

        // 6. -e -> -a (femminile plurale regolare)
        if (lower.endsWith("e") && lower.length > 2){
            return matchCase(trimmed, lower.dropLast(1) + "a")
        }

        return trimmed
    }

    /**
     * Converte un termine o unità al plurale secondo la lingua italiana.
     */
    fun toPlural(word: String): String{
        val trimmed = word.trim()
        if (trimmed.isEmpty()) return ""
        val lower = trimmed.lowercase(Locale.ITALIAN)

        if (lower in INVARIABLE_UNITS) return trimmed

        KNOWN_PLURALS[lower]?.let{ plur ->
            return matchCase(trimmed, plur)
        }

        val lastChar = lower.last()
        if (lastChar in ACCENTED_VOWELS || isConsonant(lastChar)){
            return trimmed
        }

        // -ccia / -ggia preceduta da consonante -> -cce / -gge
        if (lower.endsWith("ccia") || lower.endsWith("ggia")){
            return matchCase(trimmed, lower.dropLast(2) + "e")
        }

        // -o -> -i
        if (lower.endsWith("o")){
            return matchCase(trimmed, lower.dropLast(1) + "i")
        }

        // -a -> -e
        if (lower.endsWith("a")){
            return matchCase(trimmed, lower.dropLast(1) + "e")
        }

        // -e -> -i
        if (lower.endsWith("e")){
            return matchCase(trimmed, lower.dropLast(1) + "i")
        }

        return trimmed
    }

    /**
     * Genera l'etichetta del costo a porzione/pezzo per la schermata di dettaglio.
     * Esempi:
     * - "cornetti" -> "Costo a cornetto/porzione"
     * - "porzioni" -> "Costo a porzione"
     * - "pizze"    -> "Costo a pizza/porzione"
     * - "biscotti" -> "Costo a biscotto/porzione"
     * - "g" / "kg" -> "Costo al g" / "Costo al kg"
     */
    fun formatYieldCostLabel(yieldUnit: String?): String{
        if (yieldUnit.isNullOrBlank()) return "Costo a porzione"
        val trimmed = yieldUnit.trim()
        val lower = trimmed.lowercase(Locale.ITALIAN)

        if (lower == "g" || lower == "kg" || lower == "ml" || lower == "l"){
            return "Costo al $trimmed"
        }

        val singular = toSingular(trimmed).lowercase(Locale.ITALIAN)
        return if (singular == "porzione"){
            "Costo a porzione"
        } else{
            "Costo a $singular/porzione"
        }
    }

    private val ACCENTED_VOWELS = setOf('à', 'è', 'é', 'ì', 'ò', 'ù')
    private val VOWELS = setOf('a', 'e', 'i', 'o', 'u', 'y')

    private fun isConsonant(c: Char): Boolean{
        return c in 'a'..'z' && c !in VOWELS
    }

    private fun matchCase(original: String, target: String): String{
        if (original.isEmpty() || target.isEmpty()) return target
        return if (original[0].isUpperCase()){
            target.replaceFirstChar{ if (it.isLowerCase()) it.titlecase(Locale.ITALIAN) else it.toString() }
        } else{
            target
        }
    }
}
