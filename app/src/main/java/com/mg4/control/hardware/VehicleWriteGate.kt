package com.mg4.control.hardware

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.mg4.control.R
import com.mg4.control.debug.AppLogger

/**
 * [T-904] Politique décidée : une écriture de réglage véhicule n'est autorisée QU'À L'ARRÊT.
 *
 * Changer l'AEB, l'ELK, l'ACC/TJA ou le mode de conduite en roulant modifie le comportement
 * du véhicule sous le conducteur. La règle est donc : 0 km/h, sinon refus — et refus AUSSI
 * quand la vitesse est illisible (fail closed), parce qu'une vitesse inconnue peut être
 * n'importe quelle vitesse.
 *
 * Le confort (sièges/volant chauffants, via CarHvacManager) n'est PAS concerné : ces
 * écritures ne changent pas le comportement routier.
 */
object VehicleWriteGate {

    private const val TAG = "MG4_GATE"

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
     * Décision pure à partir d'une vitesse en km/h, [speedKmh] à null si illisible.
     *
     * Une vitesse négative est traitée comme illisible : le VHAL ne produit pas de vitesse
     * négative en marche avant, et une valeur aberrante ne doit jamais ouvrir la porte.
     */
    fun decide(speedKmh: Float?): Decision = when {
        speedKmh == null || speedKmh.isNaN() -> Decision.REFUSED_UNKNOWN_SPEED
        speedKmh < 0f                        -> Decision.REFUSED_UNKNOWN_SPEED
        speedKmh == 0f                       -> Decision.ALLOWED
        else                                 -> Decision.REFUSED_MOVING
    }

    /**
     * Vrai si l'écriture [operation] est permise maintenant. En cas de refus, journalise et
     * prévient l'utilisateur — un refus silencieux ferait croire que le réglage a été pris.
     */
    fun allow(operation: String): Boolean {
        val decision = decide(MG4Hardware.getVehicleSpeedKmh())
        if (decision == Decision.ALLOWED) return true

        AppLogger.w(TAG, "Écriture refusée ($operation) : $decision")
        notifyUser(decision)
        return false
    }

    private fun notifyUser(decision: Decision) {
        val context: Context = MG4Hardware.appContext() ?: return
        val now = System.currentTimeMillis()
        if (now - lastToastMs < TOAST_THROTTLE_MS) return
        lastToastMs = now

        val message = when (decision) {
            Decision.REFUSED_MOVING        -> R.string.write_refused_moving
            Decision.REFUSED_UNKNOWN_SPEED -> R.string.write_refused_unknown_speed
            Decision.ALLOWED               -> return
        }
        Handler(Looper.getMainLooper()).post {
            runCatching { Toast.makeText(context, message, Toast.LENGTH_SHORT).show() }
        }
    }
}
