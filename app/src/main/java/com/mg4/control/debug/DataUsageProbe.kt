package com.mg4.control.debug

import android.app.usage.NetworkStatsManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.TrafficStats
import com.mg4.control.util.DataUsage
import java.io.File
import java.util.Calendar

/**
 * Sonde « consommation de données » — LECTURE SEULE.
 *
 * Ces compteurs sont ceux d'Android, pas du véhicule : le noyau compte les octets par interface
 * et par UID, `NetworkStatsService` les agrège. L'app Réglages SAIC ne fait que les relire (sa
 * décompilation ne référence que des classes `android.net.*`). Il n'y a donc aucun dispatch par
 * firmware à prévoir ici, contrairement à tout le reste du projet.
 *
 * ⚠️ MESURÉ SUR VÉHICULE, ne pas re-supposer :
 *  • le trafic passe par **Ethernet** (mobile = 0, total = 5,1 Mo depuis le démarrage) ;
 *  • `/sys/class/net` est **illisible** — SELinux ; la voie par interface est morte ;
 *  • la permission est accordée : MOBILE et WIFI rendent 0 SANS SecurityException ;
 *  • `querySummaryForDevice(TYPE_ETHERNET, …)` rend **null**, il ne lève PAS d'exception.
 *    Sur API 28 cette surcharge attrape en interne l'IllegalArgumentException de
 *    `createTemplate()` et retourne null — d'où un NPE côté appelant si on ne teste pas ;
 *  • `NetworkTemplate.buildTemplateEthernet()` et `INetworkStatsService` répondent.
 *
 * La sonde reste utile comme non-régression, et sa dernière section essaie de bout en bout la
 * voie que [DataUsage] emploie réellement.
 */
object DataUsageProbe {

    private const val TAG = "MG4_DATA"

    fun run(context: Context) {
        AppLogger.i(TAG, "── DIAG consommation de données ──")
        interfacesSysfs()
        trafficStats()
        networkStatsPublic(context)
        gabaritEthernetCache()
        voieRetenue(context)
        AppLogger.i(TAG, "── fin DIAG données ──")
    }

    /** Essai de bout en bout de la voie que DataUsage utilisera reellement. */
    private fun voieRetenue(context: Context) {
        AppLogger.i(TAG, "— voie retenue (DataUsage) —")
        val fin = System.currentTimeMillis()
        listOf(
            "mois courant" to DataUsage.startOfMonth(),
            "30 jours"     to fin - 30L * 24 * 3600 * 1000
        ).forEach { (libelle, debut) ->
            val u = DataUsage.ethernet(context, debut, fin)
            if (u == null) {
                AppLogger.w(TAG, "  Ethernet / $libelle : ÉCHEC — repli TrafficStats nécessaire")
            } else {
                AppLogger.i(TAG, "  Ethernet / $libelle : ${DataUsage.format(u.total)} " +
                    "(rx=${DataUsage.format(u.rx)} tx=${DataUsage.format(u.tx)}) via ${u.source}")
            }
        }
        val boot = DataUsage.sinceBoot()
        AppLogger.i(TAG, "  repli : ${DataUsage.format(boot.total)} via ${boot.source}")
    }

    private fun fmt(octets: Long): String = when {
        octets < 0                -> "n/a"
        octets < 1024             -> "$octets o"
        octets < 1024 * 1024      -> "%.1f Ko".format(octets / 1024.0)
        octets < 1024L * 1024 * 1024 -> "%.1f Mo".format(octets / 1048576.0)
        else                      -> "%.2f Go".format(octets / 1073741824.0)
    }

    /**
     * Compteurs par interface, lus dans sysfs. C'est la voie la plus robuste : aucune permission,
     * et surtout elle NOMME les interfaces — c'est elle qui dira si le trafic passe par `eth0`,
     * `rmnet*`, `wlan0` ou autre chose. Compteurs depuis le dernier démarrage.
     */
    private fun interfacesSysfs() {
        val racine = File("/sys/class/net")
        val ifaces = racine.listFiles()?.sortedBy { it.name }
        if (ifaces.isNullOrEmpty()) {
            AppLogger.w(TAG, "sysfs : /sys/class/net illisible")
            return
        }
        AppLogger.i(TAG, "— interfaces (depuis le démarrage) —")
        ifaces.forEach { iface ->
            val rx = lireCompteur(iface, "rx_bytes")
            val tx = lireCompteur(iface, "tx_bytes")
            // Une interface à 0 des deux côtés n'apprend rien : on ne garde que ce qui a servi.
            if (rx > 0 || tx > 0) {
                AppLogger.i(TAG, "  ${iface.name.padEnd(10)} rx=${fmt(rx)}  tx=${fmt(tx)}  " +
                    "total=${fmt(rx + tx)}")
            }
        }
        val muettes = ifaces.filter { lireCompteur(it, "rx_bytes") <= 0 && lireCompteur(it, "tx_bytes") <= 0 }
        if (muettes.isNotEmpty()) {
            AppLogger.i(TAG, "  (sans trafic : ${muettes.joinToString(", ") { it.name }})")
        }
    }

    private fun lireCompteur(iface: File, nom: String): Long =
        try { File(iface, "statistics/$nom").readText().trim().toLong() } catch (_: Exception) { -1L }

    /** TrafficStats : aucune permission, mais uniquement depuis le dernier démarrage. */
    private fun trafficStats() {
        AppLogger.i(TAG, "— TrafficStats (depuis le démarrage) —")
        val totalRx = TrafficStats.getTotalRxBytes()
        val totalTx = TrafficStats.getTotalTxBytes()
        val mobRx   = TrafficStats.getMobileRxBytes()
        val mobTx   = TrafficStats.getMobileTxBytes()
        AppLogger.i(TAG, "  total  rx=${fmt(totalRx)} tx=${fmt(totalTx)} → ${fmt(totalRx + totalTx)}")
        AppLogger.i(TAG, "  mobile rx=${fmt(mobRx)} tx=${fmt(mobTx)} → ${fmt(mobRx + mobTx)}")
        AppLogger.i(TAG, "  (total inclut TOUTES les interfaces, Ethernet compris ; " +
            "mobile ne compte que le modem)")
    }

    /**
     * Voie publique. On teste MOBILE, WIFI **et** ETHERNET : l'échec attendu sur ETHERNET est une
     * information, pas un incident — il confirme qu'il faudra la voie cachée.
     */
    private fun networkStatsPublic(context: Context) {
        AppLogger.i(TAG, "— NetworkStatsManager (voie publique) —")
        val nsm = try {
            context.getSystemService(Context.NETWORK_STATS_SERVICE) as? NetworkStatsManager
        } catch (e: Exception) {
            AppLogger.w(TAG, "  service indisponible : ${e.message}"); null
        } ?: return

        val fin = System.currentTimeMillis()
        val debutMois = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val fenetres = listOf(
            "30 jours"    to fin - 30L * 24 * 3600 * 1000,
            "mois courant" to debutMois,
            "24 h"        to fin - 24L * 3600 * 1000
        )
        val types = listOf(
            "MOBILE"   to ConnectivityManager.TYPE_MOBILE,
            "WIFI"     to ConnectivityManager.TYPE_WIFI,
            "ETHERNET" to ConnectivityManager.TYPE_ETHERNET
        )
        types.forEach { (nom, type) ->
            fenetres.forEach { (libelle, debut) ->
                try {
                    // API 28 : cette surcharge attrape l IllegalArgumentException de
                    // createTemplate() et rend NULL au lieu de la propager. Un type non
                    // supporté (ETHERNET) arrive donc ici en bucket null, pas en exception.
                    val b = nsm.querySummaryForDevice(type, null, debut, fin)
                    if (b == null) {
                        AppLogger.w(TAG, "  $nom / $libelle : bucket null — type non supporté " +
                            "par la surcharge publique")
                    } else {
                        AppLogger.i(TAG, "  $nom / $libelle : rx=${fmt(b.rxBytes)} tx=${fmt(b.txBytes)} " +
                            "→ ${fmt(b.rxBytes + b.txBytes)}")
                    }
                } catch (e: SecurityException) {
                    // Distinguer explicitement le refus de permission d'un simple zéro : les deux
                    // se ressembleraient à l'écran alors que les causes n'ont rien à voir.
                    AppLogger.w(TAG, "  $nom / $libelle : REFUS permission " +
                        "(READ_NETWORK_USAGE_HISTORY manquante ?) — ${e.message}")
                } catch (e: IllegalArgumentException) {
                    AppLogger.w(TAG, "  $nom / $libelle : type non supporté par l'API publique " +
                        "— ${e.message}")
                } catch (e: Exception) {
                    AppLogger.w(TAG, "  $nom / $libelle : ${e.javaClass.simpleName} ${e.message}")
                }
            }
        }
    }

    /**
     * Voie cachée, celle qu'emploie l'app Réglages SAIC. On ne fait que vérifier son
     * ACCESSIBILITÉ ici : si `buildTemplateEthernet()` répond, la fonctionnalité est réalisable
     * pour l'Ethernet ; sinon il faudra se rabattre sur les compteurs sysfs.
     */
    private fun gabaritEthernetCache() {
        AppLogger.i(TAG, "— NetworkTemplate (voie cachée) —")
        try {
            val cls = Class.forName("android.net.NetworkTemplate")
            val m = cls.getMethod("buildTemplateEthernet")
            val tpl = m.invoke(null)
            AppLogger.i(TAG, "  buildTemplateEthernet() ✓ → $tpl")
        } catch (e: Exception) {
            AppLogger.w(TAG, "  buildTemplateEthernet() indisponible : " +
                "${e.javaClass.simpleName} ${e.message}")
        }
        try {
            Class.forName("android.net.INetworkStatsService")
            AppLogger.i(TAG, "  INetworkStatsService atteignable ✓")
        } catch (e: Exception) {
            AppLogger.w(TAG, "  INetworkStatsService : ${e.javaClass.simpleName}")
        }
    }
}
