package com.mg4.control.automation

/** Décision pure de l'automatisation température (testable sans Android). */
object AutomationDecision {

    enum class Outcome { NOT_APPLICABLE, APPLY }

    /**
     * APPLY ssi : [enabled] ET [temp] lisible (non null/NaN) ET [profileExists]
     * ET [temp] <= [threshold] (borne incluse — déclenchement quand il fait ≤ seuil).
     * Sinon NOT_APPLICABLE.
     */
    fun evaluate(enabled: Boolean, temp: Float?, threshold: Int, profileExists: Boolean): Outcome = when {
        !enabled                     -> Outcome.NOT_APPLICABLE
        temp == null || temp.isNaN() -> Outcome.NOT_APPLICABLE
        !profileExists               -> Outcome.NOT_APPLICABLE
        temp <= threshold.toFloat()  -> Outcome.APPLY
        else                         -> Outcome.NOT_APPLICABLE
    }
}
