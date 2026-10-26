package panattoni.simo.domain

/**
 * I 14 allergeni ufficiali secondo la normativa europea (Regolamento UE 1169/2011).
 */
enum class Allergen(
    val id: String,
    val displayName: String,
    val description: String,
    val emoji: String
){
    GLUTINE(
        id = "GLUTINE",
        displayName = "Glutine",
        description = "grano, segale, orzo, avena, farro, kamut o ceppi ibridati e prodotti derivati",
        emoji = "🌾"
    ),
    CROSTACEI(
        id = "CROSTACEI",
        displayName = "Crostacei",
        description = "crostacei e prodotti a base di crostacei",
        emoji = "🦐"
    ),
    UOVA(
        id = "UOVA",
        displayName = "Uova",
        description = "uova e prodotti a base di uova",
        emoji = "🥚"
    ),
    PESCE(
        id = "PESCE",
        displayName = "Pesce",
        description = "pesce e prodotti a base di pesce",
        emoji = "🐟"
    ),
    ARACHIDI(
        id = "ARACHIDI",
        displayName = "Arachidi",
        description = "arachidi e prodotti a base di arachidi",
        emoji = "🥜"
    ),
    SOIA(
        id = "SOIA",
        displayName = "Soia",
        description = "soia e prodotti a base di soia",
        emoji = "🌱"
    ),
    LATTE(
        id = "LATTE",
        displayName = "Latte",
        description = "latte e prodotti a base di latte (incluso lattosio)",
        emoji = "🥛"
    ),
    FRUTTA_A_GUSCIO(
        id = "FRUTTA_A_GUSCIO",
        displayName = "Frutta a guscio",
        description = "mandorle, nocciole, noci, anacardi, noci pecan, noci del Brasile, pistacchi, noci macadamia",
        emoji = "🌰"
    ),
    SEDANO(
        id = "SEDANO",
        displayName = "Sedano",
        description = "sedano e prodotti a base di sedano",
        emoji = "🥬"
    ),
    SENAPE(
        id = "SENAPE",
        displayName = "Senape",
        description = "senape e prodotti a base di senape",
        emoji = "🟡"
    ),
    SEMI_DI_SESAMO(
        id = "SEMI_DI_SESAMO",
        displayName = "Semi di sesamo",
        description = "semi di sesamo e prodotti a base di semi di sesamo",
        emoji = "⚪"
    ),
    SOLFITI(
        id = "SOLFITI",
        displayName = "Anidride solforosa e solfiti",
        description = "in concentrazioni superiori a 10 mg/kg o 10 mg/l",
        emoji = "🍷"
    ),
    LUPINI(
        id = "LUPINI",
        displayName = "Lupini",
        description = "lupini e prodotti a base di lupini",
        emoji = "🌿"
    ),
    MOLLUSCHI(
        id = "MOLLUSCHI",
        displayName = "Molluschi",
        description = "molluschi e prodotti a base di molluschi",
        emoji = "🦪"
    );

    companion object{
        fun fromId(id: String?): Allergen?{
            if (id.isNullOrBlank()) return null
            return entries.firstOrNull{ it.id.equals(id.trim(), ignoreCase = true) }
        }

        fun parseAllergensString(raw: String?): List<Allergen>{
            if (raw.isNullOrBlank()) return emptyList()
            return raw.split(",")
                .mapNotNull{ fromId(it.trim()) }
                .distinct()
        }

        fun formatAllergensString(allergens: Collection<Allergen>): String{
            return allergens.map{ it.id }.distinct().joinToString(",")
        }
    }
}
