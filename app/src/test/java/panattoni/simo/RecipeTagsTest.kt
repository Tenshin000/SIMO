package panattoni.simo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import panattoni.simo.domain.RecipeTags

/**
 * Test unitari per la gestione delle categorie e dei tag delle ricette.
 */
class RecipeTagsTest{

    @Test
    fun defaultTags_containsPanini(){
        // Verifica che "Panini" sia presente nell'elenco delle categorie predefinite
        assertTrue("DEFAULT_TAGS deve contenere 'Panini'", RecipeTags.DEFAULT_TAGS.contains("Panini"))
    }

    @Test
    fun getAllSortedTags_includesPaniniAndSortsAlphabetically(){
        val tags = RecipeTags.getAllSortedTags()
        assertTrue(tags.contains("Panini"))

        // Verifica che l'elenco sia ordinato alfabeticamente (case-insensitive)
        val sortedCopy = tags.sortedWith(String.CASE_INSENSITIVE_ORDER)
        assertEquals(sortedCopy, tags)
    }

    @Test
    fun sanitizeTag_normalizesPaniniCasing(){
        assertEquals("Panini", RecipeTags.sanitizeTag("panini"))
        assertEquals("Panini", RecipeTags.sanitizeTag("PANINI"))
        assertEquals("Panini", RecipeTags.sanitizeTag("  Panini  "))
    }
}
