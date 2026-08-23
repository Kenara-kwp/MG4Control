package com.mg4.control.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * Cycle de régénération du raccourci volant — logique pure, aucun accès véhicule ni Android.
 *
 * Ce qui est réellement à risque ici, c'est l'ORDRE : celui de déclaration de [RegenLevel] n'est
 * pas l'ordre d'usage, `OFF(5)` étant coincé entre `ADAPTIVE(3)` et `ONE_PEDAL(6)`. Un cycle
 * arithmétique sur les valeurs traverserait donc « Off » puis « 1 Pédale » à chaque tour, ce qui
 * ne se verrait qu'au volant.
 */
class RegenCycleTest {

    @Test
    fun `le cycle suit l ordre du launcher et reboucle`() {
        assertEquals(RegenLevel.MEDIUM,   RegenLevel.nextInCycle(RegenLevel.LOW))
        assertEquals(RegenLevel.HIGH,     RegenLevel.nextInCycle(RegenLevel.MEDIUM))
        assertEquals(RegenLevel.ADAPTIVE, RegenLevel.nextInCycle(RegenLevel.HIGH))
        assertEquals(RegenLevel.LOW,      RegenLevel.nextInCycle(RegenLevel.ADAPTIVE))
    }

    @Test
    fun `un niveau hors cycle entre par le premier cran`() {
        // Off et 1 Pédale ne font pas partie du cycle : ne rien faire passerait pour une panne
        // du raccourci, alors que la voiture est simplement dans un état qu'il ne parcourt pas.
        assertEquals(RegenLevel.LOW, RegenLevel.nextInCycle(RegenLevel.OFF))
        assertEquals(RegenLevel.LOW, RegenLevel.nextInCycle(RegenLevel.ONE_PEDAL))
    }

    @Test
    fun `le cycle ne traverse jamais Off ni 1 Pedale`() {
        // Un tour complet, plus une marge : c'est exactement ce qu'un cycle arithmétique sur les
        // valeurs de l'enum ferait échouer.
        var niveau = RegenLevel.LOW
        repeat(RegenLevel.CYCLE_ORDER.size * 2 + 1) {
            niveau = RegenLevel.nextInCycle(niveau)
            assertFalse("Off ne doit pas apparaître dans le cycle", niveau == RegenLevel.OFF)
            assertFalse("1 Pédale a son propre raccourci", niveau == RegenLevel.ONE_PEDAL)
        }
    }

    @Test
    fun `1 Pedale reste hors du cycle`() {
        assertFalse(RegenLevel.ONE_PEDAL in RegenLevel.CYCLE_ORDER)
        assertFalse(RegenLevel.OFF in RegenLevel.CYCLE_ORDER)
    }
}
