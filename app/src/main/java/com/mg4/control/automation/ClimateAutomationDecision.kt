package com.mg4.control.automation

/** Décision pure de l'automatisation climatisation (testable sans Android). */
object ClimateAutomationDecision {

    /** Règle retenue, ou NONE si rien ne s'applique. */
    enum class Outcome { NONE, HOT, COLD }

    /**
     * Choisit la règle à appliquer d'après la température extérieure.
     *
     * Conditions inclusives : chaud si `temp >= seuilChaud`, froid si `temp <= seuilFroid`.
     * Si les deux se déclenchent (seuils qui se chevauchent — configuration incohérente), on
     * retient **CHAUD** de façon déterministe plutôt que de dépendre d'un ordre implicite.
     */
    fun evaluate(config: ClimateAutomationSettings.Config, temp: Float?): Outcome = when {
        !config.enabled              -> Outcome.NONE
        temp == null || temp.isNaN() -> Outcome.NONE
        config.hot.active  && temp >= config.hot.threshold.toFloat()  -> Outcome.HOT
        config.cold.active && temp <= config.cold.threshold.toFloat() -> Outcome.COLD
        else                         -> Outcome.NONE
    }
}
