package br.com.gotuner.tuner.domain.tuning

import br.com.gotuner.tuner.domain.model.StandardTuning
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NoteScaleTest {
    @Test fun everyStringSitsOnItsOwnNote() {
        StandardTuning.strings.forEach { string ->
            val position = NoteScale.position(string.frequency)!!
            val semitone = NoteScale.closestSemitone(string.frequency)!!

            assertEquals(string.note, NoteScale.name(semitone))
            // String frequencies are rounded to two decimals, hence the small tolerance.
            assertEquals(semitone.toDouble(), position, 0.001)
            assertEquals(string.frequency, NoteScale.frequency(semitone), 0.01)
        }
    }

    @Test fun namesNeighboursAndOctaves() {
        assertEquals("D#2", NoteScale.name(39))
        assertEquals("E2", NoteScale.name(40))
        assertEquals("F2", NoteScale.name(41))
        assertEquals("B3", NoteScale.name(59))
        assertEquals("C4", NoteScale.name(60))
        assertEquals("A4", NoteScale.name(69))
        assertEquals("C-1", NoteScale.name(0))
        assertEquals("B-2", NoteScale.name(-1))
    }

    @Test fun positionMovesOneUnitPerSemitone() {
        assertEquals(69.0, NoteScale.position(440.0)!!, 1e-9)
        assertEquals(81.0, NoteScale.position(880.0)!!, 1e-9)
        // 30 cents above A2.
        assertEquals(45.3, NoteScale.position(110.0 * 2.0.pow(30.0 / 1200))!!, 1e-9)
    }

    @Test fun closestSemitoneSwitchesHalfwayBetweenNotes() {
        val e2 = NoteScale.frequency(40)

        assertEquals(40, NoteScale.closestSemitone(e2 * 2.0.pow(49.0 / 1200)))
        assertEquals(41, NoteScale.closestSemitone(e2 * 2.0.pow(51.0 / 1200)))
        assertEquals(39, NoteScale.closestSemitone(e2 * 2.0.pow(-51.0 / 1200)))
    }

    @Test fun invalidFrequenciesHaveNoPosition() {
        listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY).forEach {
            assertNull(NoteScale.position(it), "$it")
            assertNull(NoteScale.closestSemitone(it), "$it")
        }
    }
}
