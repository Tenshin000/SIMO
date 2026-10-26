package panattoni.simo

import panattoni.simo.domain.MathEngine
import org.junit.Assert.assertEquals
import org.junit.Test

class MathEngineSafetyTest {

    @Test
    fun testDivisionByZeroAndInvalidNumbers(){
        // Yield base zero
        assertEquals(1.0, MathEngine.calculateScaleFactorByYield(0.0, 10.0), 0.001)
        // Target yield zero
        assertEquals(1.0, MathEngine.calculateScaleFactorByYield(10.0, 0.0), 0.001)
        // Valori negativi
        assertEquals(1.0, MathEngine.calculateScaleFactorByYield(-5.0, 10.0), 0.001)
        assertEquals(1.0, MathEngine.calculateScaleFactorByYield(10.0, -5.0), 0.001)
        // NaN ed Infinity
        assertEquals(1.0, MathEngine.calculateScaleFactorByYield(Double.NaN, 10.0), 0.001)
        assertEquals(1.0, MathEngine.calculateScaleFactorByYield(10.0, Double.POSITIVE_INFINITY), 0.001)

        // Reverse scaling ingredient
        assertEquals(1.0, MathEngine.calculateScaleFactorByIngredient(0.0, 100.0), 0.001)
        assertEquals(1.0, MathEngine.calculateScaleFactorByIngredient(100.0, 0.0), 0.001)
        assertEquals(1.0, MathEngine.calculateScaleFactorByIngredient(-10.0, 100.0), 0.001)
        assertEquals(1.0, MathEngine.calculateScaleFactorByIngredient(100.0, -10.0), 0.001)
        assertEquals(1.0, MathEngine.calculateScaleFactorByIngredient(Double.NaN, 100.0), 0.001)
        assertEquals(1.0, MathEngine.calculateScaleFactorByIngredient(100.0, Double.POSITIVE_INFINITY), 0.001)
    }

    @Test
    fun testCycleDetectionEdgeCases(){
        // Stringhe vuote
        assertEquals(false, MathEngine.wouldCreateCycle("", "", emptyList()))
        assertEquals(false, MathEngine.wouldCreateCycle("a", "", emptyList()))
        assertEquals(false, MathEngine.wouldCreateCycle("", "b", emptyList()))

        // Ciclo a 3 nodi: A -> B -> C -> A
        val edges = listOf("A" to "B", "B" to "C")
        assertEquals(true, MathEngine.wouldCreateCycle("C", "A", edges))
        assertEquals(false, MathEngine.wouldCreateCycle("A", "D", edges))
    }
}
