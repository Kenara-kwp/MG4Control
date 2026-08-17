package com.mg4.control.automation

import android.content.Context

/**
 * Réglages de l'automatisation « Déclenchement A/C via la température ».
 *
 * Deux règles INDÉPENDANTES : une quand il fait chaud (temp ≥ seuil), une quand il fait froid
 * (temp ≤ seuil). Chacune porte ses propres réglages clim — les mêmes valeurs n'auraient aucun
 * sens à 35 °C et à −5 °C.
 *
 * Volontairement séparée d'[AutomationSettings] : ce n'est pas une application de profil, elle
 * n'est donc PAS soumise au toggle « application auto du profil » et a son propre interrupteur.
 */
object ClimateAutomationSettings {

    const val PREFS = "mg4_settings"

    const val KEY_ENABLED = "ac_auto_enabled"

    // Une règle = un préfixe ; les clés sont dérivées pour éviter douze constantes quasi jumelles.
    private const val HOT  = "ac_auto_hot_"
    private const val COLD = "ac_auto_cold_"

    const val DEFAULT_HOT_THRESHOLD  = 28
    const val DEFAULT_COLD_THRESHOLD = 5
    const val DEFAULT_HOT_TARGET     = 20
    const val DEFAULT_COLD_TARGET    = 24
    const val DEFAULT_FAN            = 4

    /** Bornes de saisie — larges à dessein, les vraies limites du véhicule sont lues au moment
     *  d'appliquer (getClimateState) et la consigne y est clampée. */
    const val MIN_TEMP = -20
    const val MAX_TEMP = 60
    const val MIN_TARGET = 15
    const val MAX_TARGET = 33
    const val MIN_FAN = 1
    const val MAX_FAN = 10

    /** Une règle : sa condition de déclenchement et les réglages clim à appliquer. */
    data class Rule(
        val active: Boolean,
        val threshold: Int,
        val targetTemp: Int,
        val fanLevel: Int,
        val defrostFront: Boolean,
        val defrostRear: Boolean
    )

    data class Config(
        val enabled: Boolean,
        val hot: Rule,
        val cold: Rule
    )

    fun read(context: Context): Config {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        fun rule(prefix: String, defThreshold: Int, defTarget: Int) = Rule(
            active       = p.getBoolean(prefix + "on", false),
            threshold    = p.getInt(prefix + "threshold", defThreshold),
            targetTemp   = p.getInt(prefix + "target", defTarget),
            fanLevel     = p.getInt(prefix + "fan", DEFAULT_FAN),
            defrostFront = p.getBoolean(prefix + "def_front", false),
            defrostRear  = p.getBoolean(prefix + "def_rear", false)
        )
        return Config(
            enabled = p.getBoolean(KEY_ENABLED, false),
            hot     = rule(HOT,  DEFAULT_HOT_THRESHOLD,  DEFAULT_HOT_TARGET),
            cold    = rule(COLD, DEFAULT_COLD_THRESHOLD, DEFAULT_COLD_TARGET)
        )
    }

    // ── Écriture (utilisée par l'UI) ─────────────────────────────────────────
    fun keyOn(hot: Boolean)        = (if (hot) HOT else COLD) + "on"
    fun keyThreshold(hot: Boolean) = (if (hot) HOT else COLD) + "threshold"
    fun keyTarget(hot: Boolean)    = (if (hot) HOT else COLD) + "target"
    fun keyFan(hot: Boolean)       = (if (hot) HOT else COLD) + "fan"
    fun keyDefFront(hot: Boolean)  = (if (hot) HOT else COLD) + "def_front"
    fun keyDefRear(hot: Boolean)   = (if (hot) HOT else COLD) + "def_rear"

    fun clampThreshold(raw: Int?, hot: Boolean): Int =
        (raw ?: if (hot) DEFAULT_HOT_THRESHOLD else DEFAULT_COLD_THRESHOLD)
            .coerceIn(MIN_TEMP, MAX_TEMP)

    fun clampTarget(raw: Int?, hot: Boolean): Int =
        (raw ?: if (hot) DEFAULT_HOT_TARGET else DEFAULT_COLD_TARGET)
            .coerceIn(MIN_TARGET, MAX_TARGET)

    fun clampFan(raw: Int?): Int = (raw ?: DEFAULT_FAN).coerceIn(MIN_FAN, MAX_FAN)
}
