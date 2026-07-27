package com.mg4.control.hardware

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.mg4.control.R
import com.mg4.control.debug.AppLogger

/**
 * [T-904] Verrou d'écriture véhicule configurable (Réglages → « Sécurité conduite »).
 *
 * OFF par défaut : aucune restriction. Quand l'utilisateur l'active, une écriture de réglage
 * de conduite (AEB, ELK, ACC/TJA, mode de conduite…) n'est autorisée que jusqu'à la vitesse
 * maximale choisie (bornes incluses) ; au-dessus, refus. La vitesse illisible reste un refus
 * (fail closed), une vitesse inconnue pouvant être n'importe quelle vitesse.
 *
 * Le confort (sièges/volant chauffants, via CarHvacManager) n'est PAS concerné : ces
 * écritures ne changent pas le comportement routier.
 */
object VehicleWriteGate {

    private const val TAG = "MG4_GATE"

    /** Store partagé avec SettingsFragment. */
    const val PREFS_NAME = "mg4_settings"
    /** Clé bool : sécurité activée. Défaut false (aucune restriction). */
    const val KEY_ENABLED = "safety_speed_gate_enabled"
    /** Clé int : vitesse max (km/h) jusqu'à laquelle les écritures passent. Défaut 0. */
    const val KEY_MAX_KMH = "safety_speed_gate_max_kmh"
    /** Vitesse max saisissable. */
    const val MAX_SPEED_KMH = 250

    /** Anti-spam sur le message utilisateur : un refus par seconde au plus. */
    private const val TOAST_THROTTLE_MS = 1_000L

    @Volatile
    private var lastToastMs = 0L

    enum class Decision {
        /** Véhicule à l'arrêt — écriture autorisée. */
        ALLOWED,
        /** Véhicule en mouvement — écriture refusée. */
        REFUSED_MOVING,
        /** Vitesse illisible — écriture refusée (fail closed). */
        REFUSED_UNKNOWN_SPEED
    }

    /**
     * Décision pure. [enabled] false court-circuite tout (aucune restriction).
     * Sinon : autorisé jusqu'à [maxKmh] inclus ; vitesse null/NaN/négative = refus
     * (fail closed) ; au-dessus du seuil = refus.
     */
    fun decide(speedKmh: Float?, enabled: Boolean, maxKmh: Int): Decision = when {
        !enabled                             -> Decision.ALLOWED
        speedKmh == null || speedKmh.isNaN() -> Decision.REFUSED_UNKNOWN_SPEED
        speedKmh < 0f                        -> Decision.REFUSED_UNKNOWN_SPEED
        speedKmh <= maxKmh.toFloat()         -> Decision.ALLOWED
        else                                 -> Decision.REFUSED_MOVING
    }

    /** Clampe une saisie utilisateur dans [0, MAX_SPEED_KMH]. null/vide => 0. */
    fun clampSpeed(raw: Int?): Int = (raw ?: 0).coerceIn(0, MAX_SPEED_KMH)

    /** Décision + seuil + vitesse mesurée, lus en direct dans les prefs (sans effet de bord). */
    private data class Eval(val decision: Decision, val maxKmh: Int, val speedKmh: Float?)

    private fun evaluate(): Eval {
        val context = MG4Hardware.appContext() ?: return Eval(Decision.ALLOWED, 0, null)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_ENABLED, false)) return Eval(Decision.ALLOWED, 0, null)
        val maxKmh = prefs.getInt(KEY_MAX_KMH, 0)
        val speedKmh = MG4Hardware.getVehicleSpeedKmh()
        return Eval(decide(speedKmh, enabled = true, maxKmh = maxKmh), maxKmh, speedKmh)
    }

    /**
     * Vrai si l'écriture [operation] est permise maintenant. Lit la config en direct dans
     * les prefs. Sécurité OFF (défaut) ou contexte indisponible => autorisé. En cas de refus,
     * journalise et prévient l'utilisateur.
     */
    fun allow(operation: String): Boolean {
        val (decision, maxKmh, speedKmh) = evaluate()
        if (decision == Decision.ALLOWED) return true

        AppLogger.w(TAG, "Écriture refusée ($operation) : $decision (vitesse=${speedKmh ?: "?"} max=$maxKmh km/h)")
        notifyUser(decision, maxKmh, speedKmh)
        return false
    }

    /**
     * Comme [allow] mais silencieux (ni log ni toast) : pour les appelants qui veulent
     * seulement savoir si une écriture passerait maintenant (ex. affichage de l'overlay de
     * sélection de profil, qui applique un profil = une écriture).
     */
    fun isAllowedNow(): Boolean = evaluate().decision == Decision.ALLOWED

    private fun notifyUser(decision: Decision, maxKmh: Int, speedKmh: Float?) {
        val context: Context = MG4Hardware.appContext() ?: return
        val now = System.currentTimeMillis()
        if (now - lastToastMs < TOAST_THROTTLE_MS) return
        lastToastMs = now

        val message = when (decision) {
            Decision.REFUSED_MOVING        -> {
                // Vitesse actuelle affichée à côté de la limite : l'utilisateur voit d'un
                // coup d'œil ce que l'app mesure (et peut signaler un écart au compteur).
                val speedStr = String.format(java.util.Locale.getDefault(), "%.0f", speedKmh ?: 0f)
                context.getString(R.string.write_refused_moving, speedStr, maxKmh)
            }
            Decision.REFUSED_UNKNOWN_SPEED -> context.getString(R.string.write_refused_unknown_speed)
            Decision.ALLOWED               -> return
        }
        Handler(Looper.getMainLooper()).post {
            runCatching { Toast.makeText(context, message, Toast.LENGTH_SHORT).show() }
        }
    }
}
