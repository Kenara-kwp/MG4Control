package com.mg4.control.util

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.net.TrafficStats
import com.mg4.control.debug.AppLogger
import java.util.Calendar

/**
 * Consommation de données du système — lecture seule.
 *
 * Ces compteurs sont ceux d'Android (le noyau compte par interface, `NetworkStatsService`
 * agrège), pas ceux du véhicule : aucun dispatch par firmware ici, contrairement au reste du
 * projet. L'app Réglages SAIC ne fait que relire les mêmes.
 *
 * ⚠️ CE QUE LA SONDE A MESURÉ SUR VÉHICULE, et qui dicte l'implémentation :
 *  • le trafic passe par **Ethernet** — `TrafficStats` mobile rend 0 alors que le total monte ;
 *  • `/sys/class/net` est **illisible** (SELinux), la voie par interface est donc morte ;
 *  • la permission est bien accordée : MOBILE et WIFI répondent 0 sans SecurityException ;
 *  • `querySummaryForDevice(TYPE_ETHERNET, …)` rend **null**. Sur API 28 cette surcharge
 *    attrape l'IllegalArgumentException de `createTemplate()` en interne et retourne null au
 *    lieu de la propager — d'où un NPE côté appelant, pas une exception parlante ;
 *  • en revanche `NetworkTemplate.buildTemplateEthernet()` répond, et `INetworkStatsService`
 *    est atteignable.
 *
 * D'où la cascade ci-dessous : la surcharge **cachée** qui accepte un NetworkTemplate, puis un
 * repli sur `TrafficStats` (depuis le démarrage seulement). [lastSource] dit toujours laquelle
 * a servi, pour qu'un chiffre affiché ne soit jamais d'origine ambiguë.
 */
object DataUsage {

    private const val TAG = "MG4_DATA"

    data class Usage(val rx: Long, val tx: Long, val source: String) {
        val total: Long get() = rx + tx
    }

    /** Début du mois courant, minuit — la borne qu'utilise l'écran Réglages par défaut. */
    fun startOfMonth(): Long = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    /**
     * Consommation Ethernet sur l'intervalle, ou `null` si aucune voie ne répond.
     *
     * Retourne null plutôt qu'un zéro : « rien consommé » et « impossible de lire » ne doivent
     * pas s'afficher pareil, sinon un défaut de permission passe pour une absence de trafic.
     */
    fun ethernet(context: Context, start: Long, end: Long): Usage? {
        val nsm = try {
            context.getSystemService(Context.NETWORK_STATS_SERVICE) as? NetworkStatsManager
        } catch (_: Exception) { null } ?: return null

        val template = ethernetTemplate() ?: return null
        return try {
            // Surcharge @hide : c'est la seule qui accepte un gabarit Ethernet. La variante
            // publique à base d'int ne sait construire que MOBILE et WIFI.
            val m = nsm.javaClass.getMethod(
                "querySummaryForDevice", template.javaClass,
                Long::class.javaPrimitiveType, Long::class.javaPrimitiveType
            )
            val bucket = m.invoke(nsm, template, start, end) as? NetworkStats.Bucket
            if (bucket == null) {
                AppLogger.w(TAG, "querySummaryForDevice(template) → bucket null")
                null
            } else {
                Usage(bucket.rxBytes, bucket.txBytes, "NetworkStatsManager/Ethernet")
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "voie cachée indisponible : ${e.javaClass.simpleName} ${e.message}")
            null
        }
    }

    /** Repli universel : toutes interfaces, mais uniquement depuis le dernier démarrage. */
    fun sinceBoot(): Usage = Usage(
        TrafficStats.getTotalRxBytes().coerceAtLeast(0),
        TrafficStats.getTotalTxBytes().coerceAtLeast(0),
        "TrafficStats/depuis le démarrage"
    )

    private fun ethernetTemplate(): Any? = try {
        Class.forName("android.net.NetworkTemplate")
            .getMethod("buildTemplateEthernet").invoke(null)
    } catch (e: Exception) {
        AppLogger.w(TAG, "buildTemplateEthernet indisponible : ${e.javaClass.simpleName}")
        null
    }

    /** Formatage lisible, unités décimales comme l'écran Réglages. */
    fun format(octets: Long): String = when {
        octets < 0                   -> "n/a"
        octets < 1024                -> "$octets o"
        octets < 1024 * 1024         -> "%.1f Ko".format(octets / 1024.0)
        octets < 1024L * 1024 * 1024 -> "%.1f Mo".format(octets / 1048576.0)
        else                         -> "%.2f Go".format(octets / 1073741824.0)
    }
}
