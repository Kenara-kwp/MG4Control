package com.mg4.control.model

enum class RegenLevel(val value: Int, val label: String) {
    LOW(0, "Low"),
    MEDIUM(1, "Medium"),
    HIGH(2, "High"),
    ADAPTIVE(3, "Adaptive"),
    OFF(5, "Off"),
    ONE_PEDAL(6, "One Pedal");

    companion object {
        fun fromValue(v: Int): RegenLevel = values().firstOrNull { it.value == v } ?: MEDIUM

        /**
         * Ordre d'USAGE des niveaux — celui du launcher d'origine, et surtout PAS l'ordre de
         * déclaration ci-dessus, où [OFF] (5) est coincé entre [ADAPTIVE] (3) et [ONE_PEDAL]
         * (6) : un cycle arithmétique traverserait Off puis 1 Pédale à chaque tour.
         *
         * [ONE_PEDAL] reste volontairement hors du cycle. C'est un mode de conduite à part
         * entière, il a déjà son propre raccourci, et l'y inclure imposerait de le traverser
         * à chaque tour de molette.
         */
        val CYCLE_ORDER = listOf(LOW, MEDIUM, HIGH, ADAPTIVE)

        /**
         * Niveau suivant dans [CYCLE_ORDER], en rebouclant à la fin.
         *
         * Un niveau hors cycle ([OFF] ou [ONE_PEDAL]) fait entrer par le premier cran : sur une
         * touche de volant, ne rien faire passerait pour une panne.
         */
        fun nextInCycle(current: RegenLevel): RegenLevel {
            val idx = CYCLE_ORDER.indexOf(current)
            return if (idx < 0) CYCLE_ORDER.first()
                   else CYCLE_ORDER[(idx + 1) % CYCLE_ORDER.size]
        }
    }
}
