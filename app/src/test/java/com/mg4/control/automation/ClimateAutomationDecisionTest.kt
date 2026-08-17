package com.mg4.control.automation

import com.mg4.control.automation.ClimateAutomationDecision.Outcome
import org.junit.Assert.assertEquals
import org.junit.Test

class ClimateAutomationDecisionTest {

    private fun rule(active: Boolean, threshold: Int) =
        ClimateAutomationSettings.Rule(active, threshold, 21, 4, false, false)

    private fun cfg(
        enabled: Boolean = true,
        hotOn: Boolean = true, hotT: Int = 28,
        coldOn: Boolean = true, coldT: Int = 5
    ) = ClimateAutomationSettings.Config(enabled, rule(hotOn, hotT), rule(coldOn, coldT))

    @Test fun `desactive - aucune regle`() {
        assertEquals(Outcome.NONE, ClimateAutomationDecision.evaluate(cfg(enabled = false), 35f))
    }

    @Test fun `temp illisible - aucune regle`() {
        assertEquals(Outcome.NONE, ClimateAutomationDecision.evaluate(cfg(), null))
        assertEquals(Outcome.NONE, ClimateAutomationDecision.evaluate(cfg(), Float.NaN))
    }

    @Test fun `au dessus du seuil chaud - regle chaud`() {
        assertEquals(Outcome.HOT, ClimateAutomationDecision.evaluate(cfg(), 30f))
    }

    @Test fun `au seuil chaud - borne incluse`() {
        assertEquals(Outcome.HOT, ClimateAutomationDecision.evaluate(cfg(), 28f))
    }

    @Test fun `sous le seuil froid - regle froid`() {
        assertEquals(Outcome.COLD, ClimateAutomationDecision.evaluate(cfg(), 2f))
    }

    @Test fun `au seuil froid - borne incluse`() {
        assertEquals(Outcome.COLD, ClimateAutomationDecision.evaluate(cfg(), 5f))
    }

    @Test fun `entre les deux seuils - aucune regle`() {
        assertEquals(Outcome.NONE, ClimateAutomationDecision.evaluate(cfg(), 18f))
    }

    @Test fun `regle chaud desactivee - ignoree`() {
        assertEquals(Outcome.NONE, ClimateAutomationDecision.evaluate(cfg(hotOn = false), 35f))
    }

    @Test fun `regle froid desactivee - ignoree`() {
        assertEquals(Outcome.NONE, ClimateAutomationDecision.evaluate(cfg(coldOn = false), 0f))
    }

    @Test fun `seuils qui se chevauchent - chaud gagne de facon deterministe`() {
        // Configuration incohérente (chaud>=10 ET froid<=30) : 20 satisfait les deux.
        assertEquals(Outcome.HOT, ClimateAutomationDecision.evaluate(cfg(hotT = 10, coldT = 30), 20f))
    }
}
