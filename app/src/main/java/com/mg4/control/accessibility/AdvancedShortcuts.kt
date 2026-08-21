package com.mg4.control.accessibility

import android.content.Context
import com.mg4.control.shortcut.ShortcutAction

/**
 * Raccourcis « avancés » : couples (code de touche, type d'appui) → action.
 *
 * Distincts des raccourcis classiques, qui reposent sur le broadcast `hardkey.report` du
 * véhicule. Ceux-ci passent par [KeyCaptureService], qui voit la touche AVANT le launcher et
 * peut la consommer.
 *
 * ⚠️ CONSÉQUENCE À CONNAÎTRE : une touche enregistrée ici est réclamée **en bloc**. Pour
 * intercepter un appui long il faut consommer le DOWN, or à cet instant on ne sait pas encore
 * si l'appui sera court ou long — et on ne peut pas « dé-consommer » après coup. Le type
 * d'appui non attribué ne retombe donc PAS sur le launcher : il ne fait simplement rien.
 * C'est ce que [isClaimed] exprime, et c'est pour ça qu'elle ignore le type d'appui.
 */
object AdvancedShortcuts {

    const val PREFS = "mg4_settings"

    /** Interrupteur maître de la fonctionnalité. Défaut false : la voie classique reste la norme. */
    const val KEY_ENABLED = "advanced_shortcuts_enabled"

    /**
     * Seuil de l'appui long, en millisecondes.
     *
     * Le chemin classique n'a pas ce problème : le véhicule lui livre un extra
     * `android.intent.extra.hardkey.longpress` déjà calculé. L'accessibilité ne fournit que des
     * KeyEvent bruts, on mesure donc DOWN→UP nous-mêmes. 500 ms est la valeur Android par
     * défaut ; le seuil du véhicule est inconnu, un écart de classement entre les deux systèmes
     * reste donc possible.
     */
    const val LONG_PRESS_MS = 500L

    private const val PREFIXE = "adv_sc_"

    private fun cle(keyCode: Int, long: Boolean) =
        "$PREFIXE$keyCode${if (long) "_long" else "_single"}"

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, on: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ENABLED, on).apply()
    }

    data class Mapping(val keyCode: Int, val longPress: Boolean, val action: ShortcutAction)

    /** Toutes les attributions, triées par touche puis par type d'appui. */
    fun all(context: Context): List<Mapping> {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return p.all.keys
            .filter { it.startsWith(PREFIXE) }
            .mapNotNull { cle ->
                val reste = cle.removePrefix(PREFIXE)
                val long = reste.endsWith("_long")
                val code = reste.removeSuffix("_long").removeSuffix("_single").toIntOrNull()
                    ?: return@mapNotNull null
                val id = runCatching { p.getInt(cle, 0) }.getOrDefault(0)
                val action = ShortcutAction.fromId(id)
                if (action == ShortcutAction.NONE) null else Mapping(code, long, action)
            }
            .sortedWith(compareBy({ it.keyCode }, { it.longPress }))
    }

    fun set(context: Context, keyCode: Int, longPress: Boolean, action: ShortcutAction) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt(cle(keyCode, longPress), action.id).apply()
    }

    fun remove(context: Context, keyCode: Int, longPress: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().remove(cle(keyCode, longPress)).apply()
    }

    /** Action attribuée à ce couple, ou null. */
    fun actionFor(context: Context, keyCode: Int, longPress: Boolean): ShortcutAction? {
        val id = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(cle(keyCode, longPress), 0)
        return ShortcutAction.fromId(id).takeIf { it != ShortcutAction.NONE }
    }

    /**
     * Vrai si CETTE TOUCHE est réclamée, quel que soit le type d'appui — voir l'avertissement
     * en tête de fichier. C'est cette méthode qui décide de la consommation.
     */
    fun isClaimed(context: Context, keyCode: Int): Boolean =
        actionFor(context, keyCode, false) != null || actionFor(context, keyCode, true) != null

    /**
     * Nom lisible d'une touche du volant, ou null si inconnue.
     *
     * Les codes viennent du système classique et des relevés faits sur véhicule. Une touche
     * inconnue reste utilisable — elle s'affiche par son code, ce qui suffit aux remontées.
     */
    fun nomTouche(keyCode: Int): String? = when (keyCode) {
        17            -> "Étoile gauche"
        286, 18       -> "Étoile droite"
        297           -> "Joystick haut"
        298           -> "Joystick bas"
        299           -> "Joystick gauche"
        300           -> "Joystick droite"
        301           -> "Joystick centre"
        else          -> null
    }
}
