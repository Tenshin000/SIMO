package panattoni.simo

import panattoni.simo.util.ItalianGrammarUtils
import org.junit.Assert.assertEquals
import org.junit.Test

class ItalianGrammarUtilsTest {

    @Test
    fun testToSingular_regularAndCulinaryWords(){
        assertEquals("cornetto", ItalianGrammarUtils.toSingular("cornetti"))
        assertEquals("Cornetto", ItalianGrammarUtils.toSingular("Cornetti"))
        assertEquals("porzione", ItalianGrammarUtils.toSingular("porzioni"))
        assertEquals("pizza", ItalianGrammarUtils.toSingular("pizze"))
        assertEquals("biscotto", ItalianGrammarUtils.toSingular("biscotti"))
        assertEquals("uovo", ItalianGrammarUtils.toSingular("uova"))
        assertEquals("fetta", ItalianGrammarUtils.toSingular("fette"))
        assertEquals("panino", ItalianGrammarUtils.toSingular("panini"))
        assertEquals("pezzo", ItalianGrammarUtils.toSingular("pezzi"))
        assertEquals("cucchiaio", ItalianGrammarUtils.toSingular("cucchiai"))
        assertEquals("cucchiaino", ItalianGrammarUtils.toSingular("cucchiaini"))
        assertEquals("bicchiere", ItalianGrammarUtils.toSingular("bicchieri"))
        assertEquals("torta", ItalianGrammarUtils.toSingular("torte"))
        assertEquals("focaccia", ItalianGrammarUtils.toSingular("focacce"))
        assertEquals("treccia", ItalianGrammarUtils.toSingular("trecce"))
        assertEquals("cannolo", ItalianGrammarUtils.toSingular("cannoli"))
    }

    @Test
    fun testToSingular_foreignAndInvariableWords(){
        assertEquals("croissant", ItalianGrammarUtils.toSingular("croissant"))
        assertEquals("muffin", ItalianGrammarUtils.toSingular("muffin"))
        assertEquals("toast", ItalianGrammarUtils.toSingular("toast"))
        assertEquals("waffle", ItalianGrammarUtils.toSingular("waffle"))
        assertEquals("g", ItalianGrammarUtils.toSingular("g"))
        assertEquals("kg", ItalianGrammarUtils.toSingular("kg"))
        assertEquals("ml", ItalianGrammarUtils.toSingular("ml"))
        assertEquals("q.b.", ItalianGrammarUtils.toSingular("q.b."))
    }

    @Test
    fun testToSingular_alreadySingular(){
        assertEquals("cornetto", ItalianGrammarUtils.toSingular("cornetto"))
        assertEquals("porzione", ItalianGrammarUtils.toSingular("porzione"))
        assertEquals("pizza", ItalianGrammarUtils.toSingular("pizza"))
        assertEquals("uovo", ItalianGrammarUtils.toSingular("uovo"))
    }

    @Test
    fun testToPlural(){
        assertEquals("cornetti", ItalianGrammarUtils.toPlural("cornetto"))
        assertEquals("porzioni", ItalianGrammarUtils.toPlural("porzione"))
        assertEquals("pizze", ItalianGrammarUtils.toPlural("pizza"))
        assertEquals("uova", ItalianGrammarUtils.toPlural("uovo"))
        assertEquals("fette", ItalianGrammarUtils.toPlural("fetta"))
        assertEquals("panini", ItalianGrammarUtils.toPlural("panino"))
        assertEquals("focacce", ItalianGrammarUtils.toPlural("focaccia"))
        assertEquals("croissant", ItalianGrammarUtils.toPlural("croissant"))
    }

    @Test
    fun testFormatYieldCostLabel(){
        // User's specific issue: "Costo a cornett/porzione" -> "Costo a cornetto/porzione"
        assertEquals("Costo a cornetto/porzione", ItalianGrammarUtils.formatYieldCostLabel("cornetti"))
        assertEquals("Costo a cornetto/porzione", ItalianGrammarUtils.formatYieldCostLabel("cornetto"))

        assertEquals("Costo a porzione", ItalianGrammarUtils.formatYieldCostLabel("porzioni"))
        assertEquals("Costo a porzione", ItalianGrammarUtils.formatYieldCostLabel("porzione"))

        assertEquals("Costo a pizza/porzione", ItalianGrammarUtils.formatYieldCostLabel("pizze"))
        assertEquals("Costo a pizza/porzione", ItalianGrammarUtils.formatYieldCostLabel("pizza"))

        assertEquals("Costo a biscotto/porzione", ItalianGrammarUtils.formatYieldCostLabel("biscotti"))
        assertEquals("Costo a fetta/porzione", ItalianGrammarUtils.formatYieldCostLabel("fette"))

        assertEquals("Costo al g", ItalianGrammarUtils.formatYieldCostLabel("g"))
        assertEquals("Costo al kg", ItalianGrammarUtils.formatYieldCostLabel("kg"))
        assertEquals("Costo al ml", ItalianGrammarUtils.formatYieldCostLabel("ml"))
    }
}
