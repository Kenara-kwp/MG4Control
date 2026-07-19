package com.mg4.control.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Comparaison de versions — logique pure, aucun accès réseau ni Android.
 *
 * Les versions à suffixe ("2.6.4-beta") ne sont pas couvertes ici : leur
 * traitement actuel est incorrect et fait l'objet de T-907.
 */
class UpdateCheckerTest {

    @Test
    fun `remote patch superieur est plus recent`() {
        assertTrue(UpdateChecker.isNewer("2.6.5", "2.6.4"))
    }

    @Test
    fun `remote minor superieur est plus recent`() {
        assertTrue(UpdateChecker.isNewer("2.7.0", "2.6.9"))
    }

    @Test
    fun `remote major superieur est plus recent`() {
        assertTrue(UpdateChecker.isNewer("3.0.0", "2.99.99"))
    }

    @Test
    fun `versions egales ne sont pas plus recentes`() {
        assertFalse(UpdateChecker.isNewer("2.6.4", "2.6.4"))
    }

    @Test
    fun `remote inferieur n est pas plus recent`() {
        assertFalse(UpdateChecker.isNewer("2.6.3", "2.6.4"))
    }

    @Test
    fun `prefixe v est ignore des deux cotes`() {
        assertTrue(UpdateChecker.isNewer("v2.6.5", "v2.6.4"))
        assertFalse(UpdateChecker.isNewer("v2.6.4", "2.6.4"))
    }

    @Test
    fun `segments manquants comptent comme zero`() {
        // "2.7" == "2.7.0" : pas une mise à jour.
        assertFalse(UpdateChecker.isNewer("2.7", "2.7.0"))
        // "2.7.1" > "2.7".
        assertTrue(UpdateChecker.isNewer("2.7.1", "2.7"))
        // "2.7" < "2.7.1" : le segment absent vaut 0.
        assertFalse(UpdateChecker.isNewer("2.7", "2.7.1"))
    }

    @Test
    fun `versionHops somme les increments par segment`() {
        assertEquals(1, UpdateChecker.versionHops("2.6.4", "2.6.5"))
        assertEquals(3, UpdateChecker.versionHops("2.6.4", "2.7.6"))
    }

    @Test
    fun `versionHops ignore les regressions de segment`() {
        // Un segment qui recule ne retire pas de sauts : 2.6.9 -> 2.7.0 = 1 saut.
        assertEquals(1, UpdateChecker.versionHops("2.6.9", "2.7.0"))
        assertEquals(0, UpdateChecker.versionHops("2.6.4", "2.6.4"))
    }
}
