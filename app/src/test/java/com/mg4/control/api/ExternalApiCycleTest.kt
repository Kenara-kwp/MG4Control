package com.mg4.control.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Cycle NEXT/PREV de l'API externe — logique pure, aucun accès véhicule ni Android.
 *
 * Ce qui est réellement à risque ici, c'est le rebouclage : Kotlin rend `(-1) % 4 == -1`, donc
 * un modulo naïf sortirait des bornes sur PREV au lieu de revenir au maximum.
 */
class ExternalApiCycleTest {

    // ── Vocabulaire ──────────────────────────────────────────────────────────

    @Test
    fun `NEXT et TOGGLE avancent, PREV recule`() {
        assertEquals(1,  ExternalApi.cycleDirection("NEXT"))
        assertEquals(1,  ExternalApi.cycleDirection("TOGGLE"))
        assertEquals(-1, ExternalApi.cycleDirection("PREV"))
    }

    @Test
    fun `la casse et les espaces sont tolerés`() {
        // Tasker et KeyMapper laissent facilement passer une espace en fin de champ.
        assertEquals(1,  ExternalApi.cycleDirection(" next "))
        assertEquals(-1, ExternalApi.cycleDirection("Prev"))
    }

    @Test
    fun `une consigne ordinaire n est pas un cycle`() {
        // Sans quoi une valeur legitime serait detournee en cycle.
        assertNull(ExternalApi.cycleDirection("2"))
        assertNull(ExternalApi.cycleDirection("AUTO"))
        assertNull(ExternalApi.cycleDirection(""))
    }

    // ── Rebouclage ───────────────────────────────────────────────────────────

    @Test
    fun `NEXT avance d un cran`() {
        assertEquals(2, ExternalApi.cycleStep(1, 0, 3, 1))
    }

    @Test
    fun `NEXT reboucle du maximum vers le minimum`() {
        assertEquals(0, ExternalApi.cycleStep(3, 0, 3, 1))
    }

    @Test
    fun `PREV reboucle du minimum vers le maximum`() {
        // Le cas qui casse avec un modulo naïf : (-1 % 4) vaut -1 en Kotlin.
        assertEquals(3, ExternalApi.cycleStep(0, 0, 3, -1))
    }

    @Test
    fun `PREV recule d un cran`() {
        assertEquals(2, ExternalApi.cycleStep(3, 0, 3, -1))
    }

    @Test
    fun `bornes ne commencant pas a zero - ventilation 1 a 10`() {
        assertEquals(2,  ExternalApi.cycleStep(1, 1, 10, 1))
        assertEquals(1,  ExternalApi.cycleStep(10, 1, 10, 1))
        assertEquals(10, ExternalApi.cycleStep(1, 1, 10, -1))
    }

    @Test
    fun `bornes de temperature 16 a 32`() {
        assertEquals(17, ExternalApi.cycleStep(16, 16, 32, 1))
        assertEquals(16, ExternalApi.cycleStep(32, 16, 32, 1))
        assertEquals(32, ExternalApi.cycleStep(16, 16, 32, -1))
    }

    @Test
    fun `une valeur hors bornes retombe dans les bornes`() {
        // Une propriete peut rendre une valeur inattendue ; le cycle ne doit pas la propager.
        val r = ExternalApi.cycleStep(99, 0, 3, 1)
        assertTrue("resultat hors bornes : $r", r in 0..3)
    }

    @Test
    fun `plage degeneree ne boucle pas dans le vide`() {
        // max <= min : une division par zero serait le pire resultat possible ici.
        assertEquals(5, ExternalApi.cycleStep(5, 5, 5, 1))
        assertEquals(5, ExternalApi.cycleStep(9, 5, 4, -1))
    }

    // ── Liste blanche ────────────────────────────────────────────────────────

    @Test
    fun `les cles non cyclables restent exclues`() {
        // Exclusions raisonnees, pas des oublis : voir la doc de CYCLABLE_KEYS.
        assertTrue(ExternalApi.SET_DRIVE_MODE !in ExternalApi.CYCLABLE_KEYS)
        assertTrue(ExternalApi.SET_REGEN      !in ExternalApi.CYCLABLE_KEYS)
        assertTrue(ExternalApi.SET_PROFILE    !in ExternalApi.CYCLABLE_KEYS)
    }

    @Test
    fun `les onze cles annoncees sont cyclables`() {
        listOf(
            ExternalApi.SET_SEAT_HEAT_LEFT, ExternalApi.SET_SEAT_HEAT_RIGHT,
            ExternalApi.SET_STEERING_HEAT, ExternalApi.SET_HVAC_POWER,
            ExternalApi.SET_HVAC_AC, ExternalApi.SET_HVAC_AUTO, ExternalApi.SET_HVAC_TEMP,
            ExternalApi.SET_HVAC_FAN, ExternalApi.SET_HVAC_RECIRC,
            ExternalApi.SET_DEFROST_FRONT, ExternalApi.SET_DEFROST_REAR
        ).forEach { assertTrue("$it devrait etre cyclable", it in ExternalApi.CYCLABLE_KEYS) }
        assertEquals(11, ExternalApi.CYCLABLE_KEYS.size)
    }

    @Test
    fun `aucune cle cyclable n est une action bloquee`() {
        // Le cycle ne doit surtout pas rouvrir une porte fermee par BLOCKED_ACTIONS.
        ExternalApi.CYCLABLE_KEYS.forEach {
            assertTrue("$it est bloquee", it.uppercase() !in ExternalApi.BLOCKED_ACTIONS)
        }
    }
}
