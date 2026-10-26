package panattoni.simo

import panattoni.simo.util.CookingTimerExtractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CookingTimerExtractorTest {

    @Test
    fun testExtractTimersFromTypicalRecipeSentences(){
        // Test "infornare a 180°C per 35 minuti"
        val step1 = "Infornare a 180°C in forno statico per 35 minuti fino a doratura."
        val timers1 = CookingTimerExtractor.extractTimersFromStep(step1)
        assertEquals(1, timers1.size)
        assertEquals(35 * 60, timers1[0].totalSeconds)
        assertEquals("35m", timers1[0].displayLabel)

        // Test ore
        val step2 = "Lasciare lievitare coperto per 2 ore a temperatura ambiente."
        val timers2 = CookingTimerExtractor.extractTimersFromStep(step2)
        assertEquals(1, timers2.size)
        assertEquals(2 * 3600, timers2[0].totalSeconds)
        assertEquals("2h", timers2[0].displayLabel)

        // Test secondi
        val step3 = "Frullare a massima velocità per 45 secondi."
        val timers3 = CookingTimerExtractor.extractTimersFromStep(step3)
        assertEquals(1, timers3.size)
        assertEquals(45, timers3[0].totalSeconds)
        assertEquals("45s", timers3[0].displayLabel)

        // Test abbreviazione min
        val step4 = "Mescolare a bagnomaria per circa 10 min."
        val timers4 = CookingTimerExtractor.extractTimersFromStep(step4)
        assertEquals(1, timers4.size)
        assertEquals(600, timers4[0].totalSeconds)

        // Test senza tempi
        val step5 = "Setacciare la farina con un colino a maglie fini."
        val timers5 = CookingTimerExtractor.extractTimersFromStep(step5)
        assertTrue(timers5.isEmpty())
    }
}
