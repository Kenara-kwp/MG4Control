package com.mg4.control.api

import android.content.Context

/**
 * Contrat de l'API externe — celle qu'utilisent KeyMapper, Tasker et consorts (issue #79).
 *
 * ⚠️ SÉCURITÉ — à lire avant de toucher à ce fichier.
 *
 * T-902 avait fermé le receiver hardkey parce que n'importe quelle application installée pouvait
 * forger son broadcast et piloter mode de conduite, régen, ADAS et AEB. Cette API rouvre
 * délibérément cette voie, parce que c'est la seule façon de servir des applications du Play Store :
 * KeyMapper et Tasker ne seront JAMAIS signées avec la clé plateforme, donc une permission
 * `signature` les exclurait. Il n'existe pas non plus de moyen fiable de connaître l'émetteur d'un
 * broadcast — une liste blanche est impossible de ce côté-là.
 *
 * Le garde-fou est donc ailleurs, et il est double :
 *  1. **[isEnabled] — interrupteur dans Réglages, DÉSACTIVÉ PAR DÉFAUT.** Tant que le propriétaire
 *     ne l'a pas activé sciemment, rien n'est joignable. C'est le vrai verrou.
 *  2. Le **verrou de vitesse** est hérité gratuitement : `VehicleWriteGate.allow()` est posé dans
 *     les primitives d'écriture de `MG4Hardware`, donc en dessous de tout point d'entrée. Aucun
 *     appelant externe ne peut écrire en roulant, quoi qu'il envoie.
 *
 * Toute écriture passant par ici est journalisée (tag [LOG_TAG]) : en cas de comportement
 * inattendu sur un véhicule, on doit pouvoir dire qui a demandé quoi.
 */
object ExternalApi {

    const val LOG_TAG = "MG4_API"

    private const val PREFS = "mg4_settings"

    /** Interrupteur maître, Réglages → Réglages avancés. Défaut : false, volontairement. */
    const val KEY_ENABLED = "external_api_enabled"

    /**
     * Liste blanche d'appelants, séparée par des virgules. Vide = tous acceptés.
     *
     * Uniquement exploitable par le ContentProvider : lui seul peut vérifier son appelant
     * (`getCallingPackage()`). Un broadcast ne porte pas d'identité d'émetteur.
     */
    const val KEY_ALLOWLIST = "external_api_allowlist"

    // ── Broadcasts entrants ──────────────────────────────────────────────────

    /**
     * Préfixe des actions « une commande = une action d'intent » :
     * `com.mg4.control.action.ONE_PEDAL`, `…ADAS_CYCLE`, `…PROFILE_PICKER`…
     *
     * ⚠️ RAISON D'ÊTRE, ne pas fusionner avec [ACTION_EXECUTE] en croyant simplifier :
     * KeyMapper ne sait saisir QUE la chaîne d'action, son éditeur d'intent n'a pas de champ
     * « extras ». Une API qui exige un extra lui est inutilisable. Cette forme-là ne demande
     * rien d'autre que l'action, donc elle marche avec n'importe quel outil capable d'émettre
     * un intent.
     */
    const val ACTION_PREFIX = "com.mg4.control.action."

    /** Commandes exposées en action directe (celles qui ne réclament aucun paramètre). */
    val DIRECT_ACTIONS = listOf(
        "ONE_PEDAL", "ENERGY_SAVING_TOGGLE", "PROFILE_PICKER", "OPEN_APP"
    )

    /**
     * Commandes VOLONTAIREMENT hors API, quelle que soit la forme d'appel.
     *
     * Ces sept-là touchent à la sécurité active ou coupent le véhicule ; les exposer à toute
     * application installée n'est pas un risque acceptable. Le filtre s'applique aussi à
     * [ACTION_EXECUTE] : les retirer des seules actions directes n'aurait rien protégé, puisque
     * l'extra `action` y donnait le même accès sans authentification supplémentaire.
     *
     * Elles restent évidemment pilotables depuis l'application et les raccourcis volant.
     */
    val BLOCKED_ACTIONS = setOf(
        "VEHICLE_POWER_OFF", "SOUND_WARNING", "OVERSPEED_ALARM", "SPEED_LIMIT_TONE",
        "ADAS_CYCLE", "AEB_CYCLE", "TSR_TOGGLE"
    )

    /** Nom de ShortcutAction porté par une action directe, ou null si ce n'en est pas une. */
    fun directActionName(action: String?): String? {
        val a = action ?: return null
        if (!a.startsWith(ACTION_PREFIX)) return null
        val name = a.removePrefix(ACTION_PREFIX)
        return name.takeIf { it in DIRECT_ACTIONS }
    }

    /**
     * Forme riche, pour les appelants qui savent joindre des extras (Tasker, adb, scripts).
     * Extra [EXTRA_ACTION] = nom de ShortcutAction, [EXTRA_PROFILE] pour APPLY_PROFILE.
     */
    const val ACTION_EXECUTE = "com.mg4.control.action.EXECUTE"

    /** Écrit un réglage. Extras [EXTRA_KEY] et [EXTRA_VALUE]. */
    const val ACTION_SET = "com.mg4.control.action.SET"

    const val EXTRA_ACTION  = "action"
    const val EXTRA_KEY     = "key"
    const val EXTRA_VALUE   = "value"
    /** Nom OU identifiant du profil, pour `action=APPLY_PROFILE` et `key=profile`. */
    const val EXTRA_PROFILE = "profile"

    // ── Clés acceptées par ACTION_SET ────────────────────────────────────────
    const val SET_DRIVE_MODE      = "drive_mode"       // ECO|NORMAL|SPORT|SNOW|CUSTOM
    const val SET_REGEN           = "regen"            // OFF|LOW|MEDIUM|HIGH|ADAPTIVE|ONE_PEDAL
    const val SET_SEAT_HEAT_LEFT  = "seat_heat_left"   // 0..3
    const val SET_SEAT_HEAT_RIGHT = "seat_heat_right"  // 0..3
    const val SET_STEERING_HEAT   = "steering_heat"    // 0|1 (ou false|true)
    const val SET_PROFILE         = "profile"          // nom ou id

    // ── Climatisation ────────────────────────────────────────────────────────
    // Réglages de confort : ils ne changent pas le comportement routier, contrairement aux
    // commandes de [BLOCKED_ACTIONS]. Ignorés si le firmware n'expose pas la clim.
    const val SET_HVAC_POWER   = "hvac_power"     // 0|1
    const val SET_HVAC_AC      = "ac"             // 0|1
    const val SET_HVAC_AUTO    = "hvac_auto"      // 0|1
    const val SET_HVAC_TEMP    = "hvac_temp"      // °C, clampé aux bornes réelles du véhicule
    const val SET_HVAC_FAN     = "hvac_fan"       // niveau, clampé aux bornes réelles
    const val SET_HVAC_RECIRC  = "hvac_recirc"    // INNER|OUTSIDE|AUTO (ou 0|1|2)
    const val SET_DEFROST_FRONT = "defrost_front" // 0|1
    const val SET_DEFROST_REAR  = "defrost_rear"  // 0|1

    // ── Lecture (ContentProvider) ────────────────────────────────────────────

    /**
     * `content://<applicationId>.state/state` → une ligne, une colonne par valeur.
     *
     * ⚠️ L'authority suit l'applicationId, elle n'est donc PAS une constante : la variante
     * offline s'installe à côté de l'online et deux paquets ne peuvent pas déclarer la même
     * (INSTALL_FAILED_CONFLICTING_PROVIDER). Les intégrateurs doivent viser
     * `com.mg4.control.state` ou `com.mg4.control.offline.state` selon la variante installée.
     */
    fun authority(context: Context): String = context.packageName + ".state"

    const val PATH_STATE = "state"

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, false)

    /**
     * Vrai si [caller] est autorisé. Liste vide = pas de filtrage (l'interrupteur maître reste le
     * verrou). Un appelant inconnu de la plateforme (`null`) est refusé dès que la liste est posée.
     */
    fun isCallerAllowed(context: Context, caller: String?): Boolean {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_ALLOWLIST, "").orEmpty().trim()
        if (raw.isEmpty()) return true
        val allowed = raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        return caller != null && allowed.any { it.equals(caller, ignoreCase = true) }
    }
}
