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
        "babà" to "babà",
        "persone" to "persona",
        "persona" to "persona",
        "piatti" to "piatto",
        "piatto" to "piatto",
        "ciotole" to "ciotola",
        "ciotola" to "ciotola",
        "vasi" to "vaso",
        "vaso" to "vaso",
        "bustine" to "bustina",
        "bustina" to "bustina",
        "foglie" to "foglia",
        "foglia" to "foglia",
        "spicchi" to "spicchio",
        "spicchio" to "spicchio",
        "rametti" to "rametto",
        "rametto" to "rametto",
        "rotoli" to "rotolo",
        "rotolo" to "rotolo",
        "cubetti" to "cubetto",
        "cubetto" to "cubetto",
        "gocce" to "goccia",
        "goccia" to "goccia",
        "pizzichi" to "pizzico",
        "pizzico" to "pizzico",
        "unità" to "unità",
        "unita" to "unita"
    )

    private val KNOWN_PLURALS = mapOf(
        "uovo" to "uova",
        "uova" to "uova",
        "biscotto" to "biscotti",
        "biscotti" to "biscotti",
        "cornetto" to "cornetti",
        "cornetti" to "cornetti",
        "porzione" to "porzioni",
        "porzioni" to "porzioni",
        "pizza" to "pizze",
        "pizze" to "pizze",
        "fetta" to "fette",
        "fette" to "fette",
        "panino" to "panini",
        "panini" to "panini",
        "pezzo" to "pezzi",
        "pezzi" to "pezzi",
        "cucchiaio" to "cucchiai",
        "cucchiai" to "cucchiai",
        "cucchiaino" to "cucchiaini",
        "cucchiaini" to "cucchiaini",
        "bicchiere" to "bicchieri",
        "bicchieri" to "bicchieri",
        "torta" to "torte",
        "torte" to "torte",
        "focaccia" to "focacce",
        "focacce" to "focacce",
        "treccia" to "trecce",
        "trecce" to "trecce",
        "croissant" to "croissant",
        "muffin" to "muffin",
        "muffins" to "muffin",
        "toast" to "toast",
        "waffle" to "waffle",
        "bignè" to "bignè",
        "babà" to "babà",
        "persona" to "persone",
        "persone" to "persone",
        "piatto" to "piatti",
        "piatti" to "piatti",
        "ciotola" to "ciotole",
        "ciotole" to "ciotole",
        "vaso" to "vasi",
        "vasi" to "vasi",
        "bustina" to "bustine",
        "bustine" to "bustine",
        "foglia" to "foglie",
        "foglie" to "foglie",
        "spicchio" to "spicchi",
        "spicchi" to "spicchi",
        "rametto" to "rametti",
        "rametti" to "rametti",
        "rotolo" to "rotoli",
        "rotoli" to "rotoli",
        "cubetto" to "cubetti",
        "cubetti" to "cubetti",
        "goccia" to "gocce",
        "gocce" to "gocce",
        "pizzico" to "pizzichi",
        "pizzichi" to "pizzichi",
        "barattolo" to "barattoli",
        "barattoli" to "barattoli",
        "pacchetto" to "pacchetti",
        "pacchetti" to "pacchetti",
        "vasetto" to "vasetti",
        "vasetti" to "vasetti",
        "bocconcino" to "bocconcini",
        "bocconcini" to "bocconcini",
        "tartelletta" to "tartellette",
        "tartellette" to "tartellette",
        "cannolo" to "cannoli",
        "cannoli" to "cannoli",
        "ciambella" to "ciambelle",
        "ciambelle" to "ciambelle",
        "sfogliatina" to "sfogliatine",
        "sfogliatine" to "sfogliatine",
        "tortino" to "tortini",
        "tortino" to "tortini"
    )

    // Unità invariabili
    private val INVARIABLE_UNITS = setOf(
        "g", "kg", "mg", "ml", "cl", "dl", "l", "q.b.", "qb", "unità", "unita", "pz"
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
     * È idempotente: se la parola è già al plurale, la restituisce invariata senza distorcerla.
     */
    fun toPlural(word: String): String{
        val trimmed = word.trim()
        if (trimmed.isEmpty()) return ""
        val lower = trimmed.lowercase(Locale.ITALIAN)

        if (lower in INVARIABLE_UNITS) return trimmed

        KNOWN_PLURALS[lower]?.let{ plur ->
            return matchCase(trimmed, plur)
        }

        // Se la parola è già un plurale registrato o ha una forma singolare diversa
        val singular = KNOWN_SINGULARS[lower] ?: toSingular(trimmed).lowercase(Locale.ITALIAN)
        if (singular != lower){
            KNOWN_PLURALS[singular]?.let{ plur ->
                return matchCase(trimmed, plur)
            }
            if (lower.endsWith("i") || lower.endsWith("e")){
                return trimmed
            }
        }

        val lastChar = singular.last()
        if (lastChar in ACCENTED_VOWELS || isConsonant(lastChar)){
            return trimmed
        }

        // -ccia / -ggia preceduta da consonante -> -cce / -gge
        if (singular.endsWith("ccia") || singular.endsWith("ggia")){
            return matchCase(trimmed, singular.dropLast(2) + "e")
        }

        // -io atono -> -i (es. cucchiaio -> cucchiai, spicchio -> spicchi)
        if (singular.endsWith("io")){
            return matchCase(trimmed, singular.dropLast(1))
        }

        // -o -> -i
        if (singular.endsWith("o")){
            return matchCase(trimmed, singular.dropLast(1) + "i")
        }

        // -a -> -e
        if (singular.endsWith("a")){
            return matchCase(trimmed, singular.dropLast(1) + "e")
        }

        // -e -> -i
        if (singular.endsWith("e")){
            return matchCase(trimmed, singular.dropLast(1) + "i")
        }

        return trimmed
    }

    /**
     * Restituisce l'unità flessa correttamente al singolare o al plurale in base alla quantità numerica.
     * Es:
     * - (unit = "porzioni", quantity = 1.0) -> "porzione"
     * - (unit = "panino", quantity = 2.0) -> "panini"
     * - (unit = "g", quantity = 1.0) -> "g"
     * - (unit = "cornetti", quantity = 1.0) -> "cornetto"
     */
    fun formatUnitByQuantity(unit: String?, quantity: Double): String{
        if (unit.isNullOrBlank()) return ""
        val trimmed = unit.trim()
        val lower = trimmed.lowercase(Locale.ITALIAN)
        if (lower in INVARIABLE_UNITS) return trimmed

        return if (Math.abs(quantity - 1.0) < 0.001){
            toSingular(trimmed)
        } else{
            toPlural(trimmed)
        }
    }

    /**
     * Formatta quantità e unità insieme con accordo grammaticale corretto (singolare con 1, plurale con >1).
     * Gestisce correttamente casi particolari come 'q.b.' (senza numero).
     * Es:
     * - (1.0, "panini") -> "1 panino"
     * - (2.0, "panino") -> "2 panini"
     * - (1.0, "porzioni") -> "1 porzione"
     * - (4.0, "porzioni") -> "4 porzioni"
     * - (500.0, "g") -> "500 g"
     * - (1.0, "q.b.") -> "q.b."
     */
    fun formatQuantityWithUnit(quantity: Double, unit: String?): String{
        if (unit.isNullOrBlank()) return NumberUtils.formatAmount(quantity)
        if (panattoni.simo.domain.MeasurementUnit.isQb(unit)) return "q.b."
        val formattedQty = NumberUtils.formatAmount(quantity)
        val displayUnit = formatUnitByQuantity(unit, quantity)
        return if (displayUnit.isEmpty()) formattedQty else "$formattedQty $displayUnit"
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
