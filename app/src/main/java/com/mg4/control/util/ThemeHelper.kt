package com.mg4.control.util

import android.app.UiModeManager
import android.content.Context
import android.provider.Settings
import androidx.appcompat.app.AppCompatDelegate
import com.mg4.control.debug.AppLogger

/**
 * Gestion du thème de l'application (sombre / clair / auto-sync launcher).
 *
 * Le mode "auto" est disponible sur TOUS les firmwares, mais fonctionne différemment :
 *
 * ─ SWI69 / SWI131 / SWI132 ──────────────────────────────────────────────────────
 *   Clé Settings.System : "SKIN_THEME_CONFIG"  (0 = sombre, 1 = clair)
 *   Broadcast launcher  : "com.saicmotor.changeSkin" (sans extra → relire Settings.System)
 *   → On lit la valeur explicitement et on passe MODE_NIGHT_YES/NO + recreate().
 *
 * ─ SWI133 / SWI68 ───────────────────────────────────────────────────────────────
 *   Le launcher appelle UiModeManager.setNightMode() qui change le uiMode Android
 *   global (0x13 = clair, 0x23 = sombre). Le SkinManager SWI133 lit isNightMode()
 *   depuis Configuration.uiMode.
 *   → On utilise MODE_NIGHT_FOLLOW_SYSTEM : AppCompat suit automatiquement le
 *     uiMode système et recrée l'activité lorsqu'il change.
 *
 * Préférence stockée : "theme_mode" dans "mg4_settings"
 *   "auto"  → sync avec le launcher MG (mécanisme adapté au firmware)
 *   "dark"  → toujours sombre
 *   "light" → toujours clair
 */
object ThemeHelper {

    private const val SKIN_THEME_KEY  = "SKIN_THEME_CONFIG"
    const val ACTION_SKIN_CHANGE      = "com.saicmotor.changeSkin"
    const val PREF_THEME_MODE         = "theme_mode"
    private const val PREFS_NAME      = "mg4_settings"

    /**
     * Callback invoqué (sur le thread principal) lorsque le launcher change de thème
     * sur SWI69/131/132. MainActivity l'utilise pour déclencher recreate().
     * Sur SWI133/68, AppCompat appelle recreate() automatiquement via FOLLOW_SYSTEM.
     */
    @Volatile var onThemeChanged: (() -> Unit)? = null

    // ── Détection du mécanisme ───────────────────────────────────────────────

    /**
     * Retourne true si ce firmware expose SKIN_THEME_CONFIG dans Settings.System
     * (SWI69 / SWI131 / SWI132). Sur SWI68/133, utilise FOLLOW_SYSTEM à la place.
     */
    fun hasSkinThemeConfig(context: Context): Boolean {
        return try {
            Settings.System.getInt(context.contentResolver, SKIN_THEME_KEY, -1) != -1
        } catch (e: Exception) {
            false
        }
    }

    // ── Lecture du thème launcher (SWI69/131/132 uniquement) ─────────────────

    /**
     * Lit SKIN_THEME_CONFIG et retourne MODE_NIGHT_YES ou MODE_NIGHT_NO.
     * À n'appeler que si hasSkinThemeConfig() == true.
     */
    fun getLauncherNightMode(context: Context): Int {
        return try {
            val value = Settings.System.getInt(context.contentResolver, SKIN_THEME_KEY, 0)
            if (value == 1) AppCompatDelegate.MODE_NIGHT_NO else AppCompatDelegate.MODE_NIGHT_YES
        } catch (e: Exception) {
            AppCompatDelegate.MODE_NIGHT_YES
        }
    }

    // ── Résolution du mode à appliquer ───────────────────────────────────────

    /**
     * Night mode réglé par le launcher, lu sur **`UiModeManager`** et NON sur
     * `Configuration.uiMode`.
     *
     * ⚠️ MESURÉ SUR SWI133 (2026-08-16), ne pas « simplifier » en revenant à FOLLOW_SYSTEM :
     * quand on change le thème depuis l'écran voiture, `UiModeManager.getNightMode()` passe bien
     * de 2 (YES) à 1 (NO) — le launcher appelle donc `setNightMode()` — mais le système **ne
     * propage pas** le changement dans la Configuration, qui reste figée à `0x23` (TYPE_CAR |
     * NIGHT_YES) dans les deux relevés. Or `MODE_NIGHT_FOLLOW_SYSTEM` se base sur la
     * Configuration : il est donc aveugle ici, et l'app restait bloquée en sombre.
     *
     * MODE_NIGHT_AUTO / CUSTOM (0 / 3) : pas de valeur exploitable → on rend la main à AppCompat.
     */
    fun getSystemNightMode(context: Context): Int = try {
        val umm = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        when (umm.nightMode) {
            UiModeManager.MODE_NIGHT_YES -> AppCompatDelegate.MODE_NIGHT_YES
            UiModeManager.MODE_NIGHT_NO  -> AppCompatDelegate.MODE_NIGHT_NO
            else                         -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
    } catch (e: Exception) {
        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    }

    /**
     * Retourne le night mode AppCompat à appliquer selon la préférence "theme_mode".
     *
     * "auto" sur SWI69/131/132 → YES ou NO selon SKIN_THEME_CONFIG
     * "auto" sur SWI133/68/165 → YES ou NO selon [getSystemNightMode]
     */
    fun resolveNightMode(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return when (prefs.getString(PREF_THEME_MODE, "auto")) {
            "light" -> AppCompatDelegate.MODE_NIGHT_NO
            "auto"  -> if (hasSkinThemeConfig(context)) getLauncherNightMode(context)
                       else getSystemNightMode(context)
            else    -> AppCompatDelegate.MODE_NIGHT_YES   // "dark" + fallback
        }
    }

    // ── Notification de changement (SWI69/131/132) ───────────────────────────

    /** Appelé depuis le service (déjà sur le main thread) pour déclencher recreate(). */
    fun notifyThemeChanged() {
        onThemeChanged?.invoke()
    }

    // ── Sonde diagnostic ─────────────────────────────────────────────────────

    private const val DIAG_TAG = "MG4_THEME"

    /** URI du fournisseur de thème SAIC (voie SWI133 : le launcher, SystemUI et VehicleSettings
     *  y posent un ContentObserver — c'est leur seul mécanisme de synchro). */
    private const val SKIN_PROVIDER_URI = "content://com.saicmotor.skinProvider/current"

    private fun nightLabel(uiMode: Int): String =
        when (uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) {
            android.content.res.Configuration.UI_MODE_NIGHT_YES -> "NUIT"
            android.content.res.Configuration.UI_MODE_NIGHT_NO  -> "JOUR"
            else -> "INDÉFINI"
        }

    private fun modeLabel(mode: Int): String = when (mode) {
        AppCompatDelegate.MODE_NIGHT_NO            -> "NO (clair)"
        AppCompatDelegate.MODE_NIGHT_YES           -> "YES (sombre)"
        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> "FOLLOW_SYSTEM"
        else                                       -> "autre($mode)"
    }

    /**
     * Sonde du bouton Diagnostic. Lecture seule.
     *
     * Objectif : identifier, sur un firmware donné, QUELLE source de thème répond réellement.
     * Manip côté testeur : cliquer Diagnostic, changer le thème depuis l'écran voiture, recliquer
     * Diagnostic, et comparer les deux relevés — c'est la ligne qui bouge qui désigne le mécanisme.
     *
     * [context] doit être le contexte d'ACTIVITÉ : c'est sa configuration qui décide des ressources
     * réellement affichées, et elle peut différer de celle du contexte applicatif.
     */
    fun runDiagnostic(context: Context) {
        AppLogger.i(DIAG_TAG, "── DIAG thème ──")

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        AppLogger.i(DIAG_TAG, "pref theme_mode=${prefs.getString(PREF_THEME_MODE, "(absent)")} " +
            "| defaultNightMode=${modeLabel(AppCompatDelegate.getDefaultNightMode())} " +
            "| resolveNightMode=${modeLabel(resolveNightMode(context))}")

        val actUi = context.resources.configuration.uiMode
        val appUi = context.applicationContext.resources.configuration.uiMode
        AppLogger.i(DIAG_TAG, "uiMode activité=0x${Integer.toHexString(actUi)} (${nightLabel(actUi)}) " +
            "| application=0x${Integer.toHexString(appUi)} (${nightLabel(appUi)})" +
            if (actUi != appUi) "  ⚠ DIVERGENTS" else "")

        // Voie SWI68/165 : le uiMode Android, piloté par le launcher ET par CarNightService.
        try {
            val umm = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
            AppLogger.i(DIAG_TAG, "UiModeManager nightMode=${umm.nightMode} (1=NO/jour, 2=YES/nuit) " +
                "currentModeType=${umm.currentModeType} → mappé ${modeLabel(getSystemNightMode(context))}")
        } catch (e: Exception) {
            AppLogger.w(DIAG_TAG, "UiModeManager indisponible : ${e.message}")
        }

        // Voie A9 (SWI69/131/132) : clé Settings.System + broadcast com.saicmotor.changeSkin.
        val skinCfg = try {
            Settings.System.getInt(context.contentResolver, SKIN_THEME_KEY, -1)
        } catch (e: Exception) { -2 }
        AppLogger.i(DIAG_TAG, "Settings.System.$SKIN_THEME_KEY=$skinCfg " +
            "(-1=absent, -2=erreur, 0=sombre, 1=clair)")

        // Voie SWI133 : ContentProvider du launcher. Détermine s'il est lisible par MG4Control
        // ET quelle valeur porte le thème courant (inconnue jusqu'ici).
        try {
            context.contentResolver.query(
                android.net.Uri.parse(SKIN_PROVIDER_URI), null, null, null, null
            )?.use { c ->
                AppLogger.i(DIAG_TAG, "skinProvider : ${c.count} ligne(s), colonnes=${c.columnNames.joinToString()}")
                var n = 0
                while (c.moveToNext() && n++ < 10) {
                    val row = (0 until c.columnCount).joinToString(" | ") { i ->
                        "${c.getColumnName(i)}=${runCatching { c.getString(i) }.getOrNull()}"
                    }
                    AppLogger.i(DIAG_TAG, "  $row")
                }
            } ?: AppLogger.i(DIAG_TAG, "skinProvider : query a renvoyé null (provider absent ?)")
        } catch (e: Exception) {
            AppLogger.w(DIAG_TAG, "skinProvider illisible : ${e.javaClass.simpleName} ${e.message}")
        }
    }
}
