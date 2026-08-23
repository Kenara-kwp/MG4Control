package com.mg4.control.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.provider.Settings
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.mg4.control.debug.AppLogger
import com.mg4.control.service.MG4ControlService

/**
 * Interception des touches volant, AVANT le launcher.
 *
 * Pourquoi ce service existe alors que MG4Control reçoit déjà les touches : le chemin actuel est
 * un *broadcast* (`com.saic.keyevent.hardkey.report`), c'est-à-dire une notification émise APRÈS
 * coup par ce qui a déjà traité la touche. Un BroadcastReceiver ne rend rien au pipeline d'entrée,
 * il ne peut donc pas empêcher le launcher d'agir — d'où le double effet constaté. Seul un service
 * d'accessibilité portant [AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS] voit la touche
 * AVANT l'application au premier plan, et peut la consommer en renvoyant `true` depuis [onKeyEvent].
 *
 * ⚠️ PÉRIMÈTRE DE LA CONSOMMATION, à ne pas élargir à la légère. Deux cas seulement :
 *  • les touches EXPLICITEMENT enregistrées dans [AdvancedShortcuts], et uniquement si
 *    l'interrupteur des raccourcis avancés est actif ;
 *  • la touche pressée PENDANT un enregistrement, le temps d'un seul appui.
 * Tout le reste traverse. Avaler une
 * touche par erreur sur une voiture est autrement plus grave que le désagrément qu'on corrige,
 * d'où ce double verrou et le try/catch qui renvoie false en cas d'imprévu.
 */
class KeyCaptureService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        // Le XML déclare la CAPACITÉ (canRequestFilterKeyEvents) ; le drapeau, lui, doit être
        // posé ici. Déclarer la capacité sans demander le drapeau ne filtre rien du tout.
        serviceInfo = serviceInfo?.apply {
            flags = flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        }
        AppLogger.i(TAG, "service connecté — filtrage actif ; " +
            "${AdvancedShortcuts.all(this).size} raccourci(s) avancé(s) enregistré(s), " +
            "interrupteur=${AdvancedShortcuts.isEnabled(this)}")
    }

    /** Instant du DOWN par touche — sert à mesurer la durée de l'appui. */
    private val debutAppui = mutableMapOf<Int, Long>()

    /** Touche en cours d'apprentissage : sert à avaler aussi la fin de son appui. */
    private var codeEnregistre: Int? = null

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        // Tout est encapsulé : une exception qui remonterait d'ici déciderait à notre place du
        // sort de la touche. On ne laisse jamais une erreur avaler une commande du volant.
        try {
            event ?: return false
            val code = event.keyCode

            if (event.action == KeyEvent.ACTION_DOWN) {
                AppLogger.i(TAG, "TOUCHE keycode=$code (${KeyEvent.keyCodeToString(code)}) " +
                    "source=${event.source} repeat=${event.repeatCount}")
            }

            // ── Mode enregistrement : on RÉCLAME la touche le temps de l'apprendre ──
            //
            // Sans ça, la touche partait au système pendant qu'on la captait : appuyer sur
            // « Accueil » basculait vers le launcher MG et l'utilisateur quittait l'écran avant
            // d'avoir pu terminer. Autrement dit, on ne pouvait enregistrer que les touches qui
            // ne font rien — l'inverse du besoin.
            //
            // Le risque de rester coincé est nul : l'enregistrement est à USAGE UNIQUE, le
            // listener se détache dès la première touche. Une seule pression est avalée.
            if (listener != null && event.action == KeyEvent.ACTION_DOWN) {
                codeEnregistre = code
                listener?.invoke(code)
                return true
            }
            // Fin de l'appui en cours d'enregistrement : le listener est déjà détaché, mais il
            // reste les répétitions et le UP. Les laisser passer livrerait au système un UP
            // orphelin — voire une action sur la touche qu'on vient justement de capturer.
            if (codeEnregistre == code) {
                if (event.action == KeyEvent.ACTION_UP) codeEnregistre = null
                return true
            }

            // Seules les touches explicitement enregistrées sont interceptées. Tout le reste
            // traverse : c'est ce qui garantit qu'un bug ici ne peut pas paralyser le volant.
            if (!AdvancedShortcuts.isEnabled(this) || !AdvancedShortcuts.isClaimed(this, code)) {
                return false
            }

            when (event.action) {
                KeyEvent.ACTION_DOWN -> {
                    // Ne mémoriser que le PREMIER down : la répétition automatique en enverrait
                    // d'autres et écraserait l'instant de départ, rendant tout appui « court ».
                    if (event.repeatCount == 0) debutAppui[code] = System.currentTimeMillis()
                }
                KeyEvent.ACTION_UP -> {
                    val debut = debutAppui.remove(code)
                    val duree = if (debut == null) 0L else System.currentTimeMillis() - debut
                    val long = duree >= AdvancedShortcuts.LONG_PRESS_MS
                    val action = AdvancedShortcuts.actionFor(this, code, long)
                    AppLogger.i(TAG, "touche $code réclamée — durée=${duree}ms " +
                        "type=${if (long) "long" else "simple"} action=${action?.name ?: "aucune"}")
                    if (action != null) declencher(action, code, long)
                }
            }
            return true   // touche réclamée : le launcher ne la verra pas
        } catch (e: Exception) {
            AppLogger.w(TAG, "onKeyEvent exception : ${e.message}")
            return false
        }
    }

    /**
     * Relaie l'action au service principal, qui détient déjà tout le répartiteur et l'état des
     * bascules. On ne réimplémente rien ici — un second chemin d'exécution finirait par diverger.
     */
    private fun declencher(action: com.mg4.control.shortcut.ShortcutAction, code: Int, long: Boolean) {
        val i = android.content.Intent(this, MG4ControlService::class.java).apply {
            setAction(MG4ControlService.ACTION_ADV_SHORTCUT)
            putExtra(MG4ControlService.EXTRA_ADV_ACTION, action.name)
            // Clé de bascule propre aux raccourcis avancés : sans elle, une action à deux états
            // partagerait son état avec le bouton classique du même nom.
            putExtra(MG4ControlService.EXTRA_ADV_SLOT, AdvancedShortcuts.slotKey(code, long))
        }
        runCatching { startForegroundService(i) }
            .onFailure { AppLogger.w(TAG, "relais impossible : ${it.message}") }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) { /* inutilisé */ }

    override fun onInterrupt() { /* inutilisé */ }

    override fun onDestroy() {
        AppLogger.i(TAG, "service déconnecté")
        super.onDestroy()
    }

    companion object {
        private const val TAG = "MG4_KEYCAP"

        /**
         * Destinataire des touches captées, posé par l'écran d'enregistrement.
         *
         * Volontairement statique : un service d'accessibilité est instancié par le système, on
         * ne peut pas lui passer de référence. Toujours le remettre à null en quittant l'écran,
         * sinon on retiendrait un Fragment détruit.
         */
        @Volatile
        var listener: ((Int) -> Unit)? = null

        /**
         * Vrai si l'utilisateur a activé notre service dans les réglages d'accessibilité.
         *
         * Lu dans Settings.Secure plutôt que via AccessibilityManager : on veut savoir si NOTRE
         * composant précis est dans la liste, pas si un service quelconque tourne.
         */
        fun isEnabled(context: Context): Boolean = try {
            val actifs = Settings.Secure.getString(
                context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ).orEmpty()
            val nous = "${context.packageName}/${KeyCaptureService::class.java.name}"
            actifs.split(':').any { it.equals(nous, ignoreCase = true) }
        } catch (e: Exception) {
            AppLogger.w(TAG, "état du service illisible : ${e.message}")
            false
        }
    }
}
