package com.mg4.control.api

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mg4.control.debug.AppLogger
import com.mg4.control.service.MG4ControlService

/**
 * Point d'entrée des applications tierces (KeyMapper, Tasker…) — issue #79.
 *
 * Déclaré dans le Manifest, donc joignable application fermée : on ne fait ici QUE le contrôle
 * d'accès et le relais. Le travail réel part dans [MG4ControlService], pour deux raisons — un
 * receiver ne dispose que de dix secondes, et le service détient déjà l'état des bascules et le
 * répartiteur d'actions.
 *
 * Voir [ExternalApi] pour la discussion sécurité : le verrou est l'interrupteur des Réglages,
 * désactivé par défaut.
 */
class ExternalApiReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val direct = ExternalApi.directActionName(action)
        if (direct == null &&
            action != ExternalApi.ACTION_EXECUTE && action != ExternalApi.ACTION_SET) return

        // On journalise AUSSI les refus : « l'API ne répond pas » doit être diagnosticable sans
        // avoir à deviner si c'est l'interrupteur ou l'intent qui est en cause.
        if (!ExternalApi.isEnabled(context)) {
            AppLogger.i(ExternalApi.LOG_TAG, "REFUS $action — API externe désactivée dans Réglages")
            return
        }

        AppLogger.i(ExternalApi.LOG_TAG, "REÇU $action " +
            "action=${intent.getStringExtra(ExternalApi.EXTRA_ACTION)} " +
            "key=${intent.getStringExtra(ExternalApi.EXTRA_KEY)} " +
            "value=${intent.extras?.get(ExternalApi.EXTRA_VALUE)} " +
            "profile=${intent.getStringExtra(ExternalApi.EXTRA_PROFILE)}")

        val relay = Intent(context, MG4ControlService::class.java).apply {
            // Une action directe est convertie en forme riche : le service n'a ainsi qu'un seul
            // chemin de traitement, quel que soit le vocabulaire employé par l'appelant.
            setAction(if (direct != null) ExternalApi.ACTION_EXECUTE else action)
            putExtra(ExternalApi.EXTRA_ACTION, direct ?: intent.getStringExtra(ExternalApi.EXTRA_ACTION))
            putExtra(ExternalApi.EXTRA_KEY,     intent.getStringExtra(ExternalApi.EXTRA_KEY))
            putExtra(ExternalApi.EXTRA_PROFILE, intent.getStringExtra(ExternalApi.EXTRA_PROFILE))
            // La valeur arrive en texte (adb, Tasker) ou en entier (KeyMapper) : on relaie la forme
            // texte, seule commune aux deux, et le service se charge de l'interpréter.
            putExtra(ExternalApi.EXTRA_VALUE, intent.extras?.get(ExternalApi.EXTRA_VALUE)?.toString())
        }
        runCatching { context.startForegroundService(relay) }
            .onFailure { AppLogger.w(ExternalApi.LOG_TAG, "relais vers le service impossible : ${it.message}") }
    }
}
