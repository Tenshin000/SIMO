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

        // Idempotency: passare un termine già plurale non deve corromperlo (es. pizze non deve diventare pizzi)
        assertEquals("pizze", ItalianGrammarUtils.toPlural("pizze"))
        assertEquals("panini", ItalianGrammarUtils.toPlural("panini"))
        assertEquals("porzioni", ItalianGrammarUtils.toPlural("porzioni"))
        assertEquals("cornetti", ItalianGrammarUtils.toPlural("cornetti"))
        assertEquals("fette", ItalianGrammarUtils.toPlural("fette"))
        assertEquals("torte", ItalianGrammarUtils.toPlural("torte"))
    }

    @Test
    fun testFormatUnitByQuantity(){
        // Caso "Panino Big Tasty": 1 panino, 2 panini
        assertEquals("panino", ItalianGrammarUtils.formatUnitByQuantity("panino", 1.0))
        assertEquals("panini", ItalianGrammarUtils.formatUnitByQuantity("panino", 2.0))
        assertEquals("panino", ItalianGrammarUtils.formatUnitByQuantity("panini", 1.0))
        assertEquals("panini", ItalianGrammarUtils.formatUnitByQuantity("panini", 2.0))

        // Caso "Maiale alla Birra": 4 porzioni, 1 porzione
        assertEquals("porzione", ItalianGrammarUtils.formatUnitByQuantity("porzioni", 1.0))
        assertEquals("porzioni", ItalianGrammarUtils.formatUnitByQuantity("porzioni", 4.0))
        assertEquals("porzione", ItalianGrammarUtils.formatUnitByQuantity("porzione", 1.0))
        assertEquals("porzioni", ItalianGrammarUtils.formatUnitByQuantity("porzione", 4.0))

        // Altri casi culinari
        assertEquals("pizza", ItalianGrammarUtils.formatUnitByQuantity("pizze", 1.0))
        assertEquals("pizze", ItalianGrammarUtils.formatUnitByQuantity("pizza", 3.0))
        assertEquals("uovo", ItalianGrammarUtils.formatUnitByQuantity("uova", 1.0))
        assertEquals("uova", ItalianGrammarUtils.formatUnitByQuantity("uovo", 2.0))
        assertEquals("cucchiaio", ItalianGrammarUtils.formatUnitByQuantity("cucchiai", 1.0))
        assertEquals("cucchiai", ItalianGrammarUtils.formatUnitByQuantity("cucchiaio", 2.0))

        // Unità invariabili
        assertEquals("g", ItalianGrammarUtils.formatUnitByQuantity("g", 1.0))
        assertEquals("g", ItalianGrammarUtils.formatUnitByQuantity("g", 500.0))
        assertEquals("kg", ItalianGrammarUtils.formatUnitByQuantity("kg", 1.0))
        assertEquals("kg", ItalianGrammarUtils.formatUnitByQuantity("kg", 2.0))
        assertEquals("ml", ItalianGrammarUtils.formatUnitByQuantity("ml", 1.0))
        assertEquals("q.b.", ItalianGrammarUtils.formatUnitByQuantity("q.b.", 1.0))
    }

    @Test
    fun testFormatQuantityWithUnit(){
        assertEquals("1 panino", ItalianGrammarUtils.formatQuantityWithUnit(1.0, "panino"))
        assertEquals("2 panini", ItalianGrammarUtils.formatQuantityWithUnit(2.0, "panino"))
        assertEquals("1 panino", ItalianGrammarUtils.formatQuantityWithUnit(1.0, "panini"))
        assertEquals("2 panini", ItalianGrammarUtils.formatQuantityWithUnit(2.0, "panini"))

        assertEquals("1 porzione", ItalianGrammarUtils.formatQuantityWithUnit(1.0, "porzioni"))
        assertEquals("4 porzioni", ItalianGrammarUtils.formatQuantityWithUnit(4.0, "porzioni"))

        assertEquals("500 g", ItalianGrammarUtils.formatQuantityWithUnit(500.0, "g"))
        assertEquals("1 g", ItalianGrammarUtils.formatQuantityWithUnit(1.0, "g"))
        assertEquals("q.b.", ItalianGrammarUtils.formatQuantityWithUnit(1.0, "q.b."))
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
