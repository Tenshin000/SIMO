package panattoni.simo

import panattoni.simo.util.NumberUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NumberUtilsTest {

    @Test
    fun testParseDoubleSafe(){
        assertEquals(1.5, NumberUtils.parseDoubleSafe("1.5"), 0.001)
        assertEquals(1.5, NumberUtils.parseDoubleSafe("1,5"), 0.001)
        assertEquals(250.0, NumberUtils.parseDoubleSafe("  250  "), 0.001)
        assertEquals(0.0, NumberUtils.parseDoubleSafe(""), 0.001)
        assertEquals(0.0, NumberUtils.parseDoubleSafe(null), 0.001)
        assertEquals(0.0, NumberUtils.parseDoubleSafe("abc"), 0.001)
        assertEquals(10.0, NumberUtils.parseDoubleSafe("invalid", 10.0), 0.001)
        assertEquals(0.0, NumberUtils.parseDoubleSafe("NaN"), 0.001)
        assertEquals(0.0, NumberUtils.parseDoubleSafe("Infinity"), 0.001)
    }

    @Test
    fun testIsValidPositiveNumber(){
        assertTrue(NumberUtils.isValidPositiveNumber("1"))
        assertTrue(NumberUtils.isValidPositiveNumber("0.5"))
        assertTrue(NumberUtils.isValidPositiveNumber("0,5"))
        assertTrue(NumberUtils.isValidPositiveNumber("  12.34 "))
        assertFalse(NumberUtils.isValidPositiveNumber("0"))
        assertFalse(NumberUtils.isValidPositiveNumber("0,0"))
        assertFalse(NumberUtils.isValidPositiveNumber("-1"))
        assertFalse(NumberUtils.isValidPositiveNumber("-0.5"))
        assertFalse(NumberUtils.isValidPositiveNumber("abc"))
        assertFalse(NumberUtils.isValidPositiveNumber(""))
        assertFalse(NumberUtils.isValidPositiveNumber(null))
    }

    @Test
    fun testFormatAmount(){
        assertEquals("2", NumberUtils.formatAmount(2.0))
        assertEquals("1.5", NumberUtils.formatAmount(1.5))
        assertEquals("33.3", NumberUtils.formatAmount(33.3))
        assertEquals("0.2", NumberUtils.formatAmount(0.2))
        assertEquals("0", NumberUtils.formatAmount(0.0))
        assertEquals("0", NumberUtils.formatAmount(Double.NaN))
        assertEquals("0", NumberUtils.formatAmount(Double.POSITIVE_INFINITY))
    }

    @Test
    fun testRoundIntegerAmount(){
        // Se decimali < .5 approssima per difetto
        assertEquals(1L, panattoni.simo.domain.MeasurementUnit.roundIntegerAmount(1.4))
        assertEquals(1L, panattoni.simo.domain.MeasurementUnit.roundIntegerAmount(1.49))
        assertEquals(1L, panattoni.simo.domain.MeasurementUnit.roundIntegerAmount(1.0))
        assertEquals(0L, panattoni.simo.domain.MeasurementUnit.roundIntegerAmount(0.49))

        // Se decimali >= .5 approssima per eccesso
        assertEquals(2L, panattoni.simo.domain.MeasurementUnit.roundIntegerAmount(1.5))
        assertEquals(2L, panattoni.simo.domain.MeasurementUnit.roundIntegerAmount(1.51))
        assertEquals(2L, panattoni.simo.domain.MeasurementUnit.roundIntegerAmount(1.99))
        assertEquals(1L, panattoni.simo.domain.MeasurementUnit.roundIntegerAmount(0.5))
        assertEquals(3L, panattoni.simo.domain.MeasurementUnit.roundIntegerAmount(2.5))
    }

    @Test
    fun testIntegerUnitsAndQb(){
        // Integer units: unità, pezzi, cucchiai, cucchiaini
        assertTrue(panattoni.simo.domain.MeasurementUnit.isIntegerUnit("unità"))
        assertTrue(panattoni.simo.domain.MeasurementUnit.isIntegerUnit("unita"))
        assertTrue(panattoni.simo.domain.MeasurementUnit.isIntegerUnit("pz"))
        assertTrue(panattoni.simo.domain.MeasurementUnit.isIntegerUnit("pezzi"))
        assertTrue(panattoni.simo.domain.MeasurementUnit.isIntegerUnit("cucchiaio"))
        assertTrue(panattoni.simo.domain.MeasurementUnit.isIntegerUnit("cucchiai"))
        assertTrue(panattoni.simo.domain.MeasurementUnit.isIntegerUnit("cucchiaino"))
        assertTrue(panattoni.simo.domain.MeasurementUnit.isIntegerUnit("cucchiaini"))
        assertFalse(panattoni.simo.domain.MeasurementUnit.isIntegerUnit("g"))
        assertFalse(panattoni.simo.domain.MeasurementUnit.isIntegerUnit("ml"))
        assertFalse(panattoni.simo.domain.MeasurementUnit.isIntegerUnit("q.b."))

        // q.b.
        assertTrue(panattoni.simo.domain.MeasurementUnit.isQb("q.b."))
        assertTrue(panattoni.simo.domain.MeasurementUnit.isQb("q.b"))
        assertTrue(panattoni.simo.domain.MeasurementUnit.isQb("qb"))
        assertTrue(panattoni.simo.domain.MeasurementUnit.isQb("quanto basta"))
        assertFalse(panattoni.simo.domain.MeasurementUnit.isQb("unità"))
        assertFalse(panattoni.simo.domain.MeasurementUnit.isQb("g"))
    }

    @Test
    fun testFormatQuantityForUnit(){
        assertEquals("1", NumberUtils.formatQuantityForUnit(1.4, "unità"))
        assertEquals("2", NumberUtils.formatQuantityForUnit(1.5, "unità"))
        assertEquals("1", NumberUtils.formatQuantityForUnit(1.49, "cucchiai"))
        assertEquals("2", NumberUtils.formatQuantityForUnit(1.50, "cucchiai"))
        assertEquals("2", NumberUtils.formatQuantityForUnit(2.4, "pezzi"))
        assertEquals("3", NumberUtils.formatQuantityForUnit(2.5, "pezzi"))
        assertEquals("", NumberUtils.formatQuantityForUnit(1.0, "q.b."))

        // Format amount in MeasurementUnit
        assertEquals("q.b.", panattoni.simo.domain.MeasurementUnit.formatAmount(1.0, "q.b."))
        assertEquals("1 unità", panattoni.simo.domain.MeasurementUnit.formatAmount(1.4, "unità"))
        assertEquals("2 unità", panattoni.simo.domain.MeasurementUnit.formatAmount(1.5, "unità"))
        assertEquals("2 cucchiai", panattoni.simo.domain.MeasurementUnit.formatAmount(2.4, "cucchiai"))
        assertEquals("3 cucchiai", panattoni.simo.domain.MeasurementUnit.formatAmount(2.5, "cucchiai"))
        assertEquals("100 g", panattoni.simo.domain.MeasurementUnit.formatAmount(100.0, "g"))
    }
}
