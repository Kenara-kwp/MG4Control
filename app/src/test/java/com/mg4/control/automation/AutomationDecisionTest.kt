package com.mg4.control.automation

import com.mg4.control.automation.AutomationDecision.Outcome
import org.junit.Assert.assertEquals
import org.junit.Test

class AutomationDecisionTest {

    @Test fun `desactive - non applicable`() {
        assertEquals(Outcome.NOT_APPLICABLE, AutomationDecision.evaluate(false, 30f, 25, true))
    }

    @Test fun `temp illisible - non applicable`() {
        assertEquals(Outcome.NOT_APPLICABLE, AutomationDecision.evaluate(true, null, 25, true))
        assertEquals(Outcome.NOT_APPLICABLE, AutomationDecision.evaluate(true, Float.NaN, 25, true))
    }

    @Test fun `profil absent - non applicable`() {
        assertEquals(Outcome.NOT_APPLICABLE, AutomationDecision.evaluate(true, 30f, 25, false))
    }

    @Test fun `sous le seuil - applique`() {
        assertEquals(Outcome.APPLY, AutomationDecision.evaluate(true, 24.9f, 25, true))
    }

    @Test fun `au seuil (borne incluse) - applique`() {
        assertEquals(Outcome.APPLY, AutomationDecision.evaluate(true, 25f, 25, true))
    }

    @Test fun `au dessus du seuil - non applicable`() {
        assertEquals(Outcome.NOT_APPLICABLE, AutomationDecision.evaluate(true, 31.5f, 25, true))
    }

    @Test fun `clampTemp borne 0 a 60, defaut si null`() {
        assertEquals(25, AutomationSettings.clampTemp(null))
        assertEquals(0, AutomationSettings.clampTemp(-5))
        assertEquals(60, AutomationSettings.clampTemp(120))
        assertEquals(18, AutomationSettings.clampTemp(18))
    }
}
