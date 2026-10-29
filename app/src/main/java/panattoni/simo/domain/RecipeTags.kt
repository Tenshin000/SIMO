package panattoni.simo.domain

object RecipeTags{

    // Categorie predefinite dell'applicazione per le ricette
    val DEFAULT_TAGS = listOf(
        "Dolci",
        "Pizze",
        "Panetteria",
        "Panini",
        "Antipasti",
        "Primi",
        "Secondi",
        "Contorni",
        "Zuppe",
        "Alcolici",
        "Analcolici",
        "Caffetteria"
    )

    private val DEFAULT_TAGS_MAP: Map<String, String> = DEFAULT_TAGS.associateBy{ it.lowercase() }

    /**
     * Unisce i tag di default con i tag personalizzati creati nelle ricette,
     * rimuovendo i duplicati (case-insensitive) e ordinandoli in rigoroso ordine alfabetico.
     */
    fun getAllSortedTags(customTagsFromRecipes: Collection<String> = emptyList()): List<String>{
        if (customTagsFromRecipes.isEmpty()){
            return DEFAULT_TAGS.sortedWith(String.CASE_INSENSITIVE_ORDER)
        }

        val tagsMap = LinkedHashMap(DEFAULT_TAGS_MAP)
        for (rawTag in customTagsFromRecipes){
            val trimmed = rawTag.trim()
            if (trimmed.isNotEmpty()){
                val lower = trimmed.lowercase()
                if (!tagsMap.containsKey(lower)){
                    tagsMap[lower] = trimmed
                }
            }
        }

        return tagsMap.values.sortedWith(String.CASE_INSENSITIVE_ORDER)
    }

    /**
     * Pulisce e normalizza un tag (se corrisponde a uno di default ne usa il casing corretto, altrimenti capitalizza).
     */
    fun sanitizeTag(rawTag: String): String{
        val trimmed = rawTag.trim()
        if (trimmed.isEmpty()) return ""
        val matching = DEFAULT_TAGS.firstOrNull{ it.equals(trimmed, ignoreCase = true) }
        return matching ?: trimmed.replaceFirstChar{ if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}
