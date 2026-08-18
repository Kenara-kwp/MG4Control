package com.mg4.control.update

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.mg4.control.debug.AppLogger
import java.io.File
import java.net.URI
import java.security.MessageDigest

/**
 * Contrôles de sécurité de la chaîne OTA.
 *
 * L'app tourne en `uid.system` : un APK d'origine non vérifiée installé sous cette
 * identité compromet le véhicule. Deux verrous, tous deux en "fail closed" :
 *   1. [ApkUrlPolicy]        — d'où l'APK a le droit de venir.
 *   2. [ApkSignatureVerifier] — que l'APK est bien signé par la même clé que nous.
 */

/** Origines autorisées pour un APK de mise à jour. */
object ApkUrlPolicy {

    private const val TAG = "MG4_UPDATE"

    /**
     * Hôtes autorisés. Les deux domaines `githubusercontent.com` sont les CDN vers
     * lesquels github.com redirige le téléchargement d'un asset de release ; sans eux
     * la redirection est refusée et la mise à jour échoue.
     */
    private val ALLOWED_HOSTS = setOf(
        "github.com",
        "api.github.com",
        "objects.githubusercontent.com",
        "release-assets.githubusercontent.com",
        "gitlab.com"
    )

    /**
     * Vrai si [url] est en https et pointe vers un hôte autorisé.
     *
     * Refuse tout le reste : http (y compris une rétrogradation https -> http en
     * cours de redirection), hôte inconnu, URL non parsable, et les sous-domaines
     * non listés explicitement (`evil-github.com`, `github.com.attacker.net`).
     */
    fun isAllowed(url: String): Boolean {
        val uri = try { URI(url) } catch (_: Exception) { return false }
        if (!uri.scheme.equals("https", ignoreCase = true)) return false
        val host = uri.host?.lowercase() ?: return false
        return host in ALLOWED_HOSTS
    }

    /** Comme [isAllowed], mais journalise le refus — pour les points d'entrée. */
    fun isAllowedLogged(url: String, where: String): Boolean {
        val ok = isAllowed(url)
        if (!ok) AppLogger.w(TAG, "$where : URL d'APK refusée (origine non autorisée) : $url")
        return ok
    }
}

/** Vérifie qu'un APK est signé par la même clé que l'app en cours d'exécution. */
object ApkSignatureVerifier {

    private const val TAG = "MG4_UPDATE"

    /**
     * Compare deux jeux d'empreintes de certificats.
     *
     * Fail closed : un jeu vide (archive illisible, signature absente, API qui a
     * échoué) ne correspond jamais, même face à un autre jeu vide.
     */
    fun certsMatch(archive: Set<String>, installed: Set<String>): Boolean =
        archive.isNotEmpty() && installed.isNotEmpty() && archive == installed

    /**
     * Vrai si [apk] est signé exactement par la même clé que l'app installée.
     * Toute erreur (archive corrompue, API indisponible) renvoie false.
     */
    fun matchesRunningApp(context: Context, apk: File): Boolean {
        val archive = fingerprintsOfArchive(context, apk)
        val installed = fingerprintsOfInstalled(context)
        val ok = certsMatch(archive, installed)
        if (!ok) {
            AppLogger.w(TAG, "Signature de l'APK non conforme — installation refusée " +
                    "(archive=${archive.size} cert(s), installée=${installed.size} cert(s))")
            // Les compteurs seuls ne permettent pas de trancher entre les trois causes réelles :
            // archive illisible (0 cert — typiquement une signature v2 sans v1, que
            // getPackageArchiveInfo ne remonte pas sur AAOS 9), clé différente, ou lecture de
            // notre propre signature en échec. Les empreintes le disent en une ligne.
            AppLogger.w(TAG, "  archive   : ${describe(archive)}")
            AppLogger.w(TAG, "  installée : ${describe(installed)}")
        }
        return ok
    }

    /**
     * Journalise le verdict de signature SANS rien bloquer.
     *
     * Le contrôle bloquant a été désactivé à la demande du propriétaire (voir [ApkInstaller]),
     * mais garder la MESURE permet de trancher, log en main, entre « signature réellement
     * différente » et « archive illisible parce que le téléchargement a été coupé ». Les deux
     * produisaient le même message auparavant, et c'est précisément ce qui a fait chercher une
     * panne de clé là où le réseau était en cause.
     *
     * Ne renvoie volontairement RIEN : le résultat ne doit pas pouvoir être réutilisé comme
     * condition, sans quoi le contrôle redeviendrait bloquant par inadvertance.
     */
    fun logVerdict(context: Context, apk: File) {
        val taille = runCatching { apk.length() }.getOrDefault(-1L)
        val archive = fingerprintsOfArchive(context, apk)
        val installed = fingerprintsOfInstalled(context)
        if (certsMatch(archive, installed)) {
            AppLogger.i(TAG, "[AUDIT] Signature CONFORME (non bloquant) — ${apk.name}, $taille octets")
        } else {
            AppLogger.w(TAG, "[AUDIT] Signature NON CONFORME (non bloquant, installation poursuivie) — " +
                "${apk.name}, $taille octets, archive=${archive.size} cert(s), installee=${installed.size} cert(s)")
            AppLogger.w(TAG, "[AUDIT]   archive   : ${describe(archive)}")
            AppLogger.w(TAG, "[AUDIT]   installee : ${describe(installed)}")
            if (archive.isEmpty()) AppLogger.w(TAG, "[AUDIT]   archive=0 cert : archive illisible " +
                "(telechargement tronque, ou v1 absente) — ce n'est PAS la preuve d'une cle differente")
        }
    }

    /** Empreintes tronquées, suffisantes pour identifier une clé dans un log. */
    private fun describe(certs: Set<String>): String =
        if (certs.isEmpty()) "(aucune — archive illisible, non signée, ou signée v2 sans v1)"
        else certs.joinToString(", ") { it.take(16) + "…" }

    /** Empreintes SHA-256 des certificats signant le fichier APK [apk]. */
    private fun fingerprintsOfArchive(context: Context, apk: File): Set<String> = try {
        // ⚠️ CAUSE RACINE du « archive=0 cert » observé le 18/08 sur un APK pourtant complet et
        // signé v1+v2+v3 : sur API 28, getPackageArchiveInfo() ne renseigne PAS `signingInfo`
        // pour une ARCHIVE — seul le champ déprécié `signatures` l'est, et uniquement si
        // GET_SIGNATURES est demandé. Ne demander que GET_SIGNING_CERTIFICATES rendait donc
        // zéro certificat, alors que la même lecture sur le paquet INSTALLÉ fonctionnait : d'où
        // le « archive=0, installee=1 » qui a fait accuser tour à tour la clé puis le réseau.
        // On demande les DEUX drapeaux et [signatureDigests] prend celui qui est rempli.
        @Suppress("DEPRECATION")
        val flags = PackageManager.GET_SIGNATURES or
            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                PackageManager.GET_SIGNING_CERTIFICATES else 0)
        val info = context.packageManager.getPackageArchiveInfo(apk.absolutePath, flags)
        signatureDigests(info)
    } catch (e: Exception) {
        AppLogger.w(TAG, "Lecture de la signature de l'archive impossible : ${e.message}")
        emptySet()
    }

    /** Empreintes SHA-256 des certificats signant l'app en cours d'exécution. */
    private fun fingerprintsOfInstalled(context: Context): Set<String> = try {
        @Suppress("DEPRECATION")
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            PackageManager.GET_SIGNING_CERTIFICATES
        else
            PackageManager.GET_SIGNATURES
        val info = context.packageManager.getPackageInfo(context.packageName, flags)
        signatureDigests(info)
    } catch (e: Exception) {
        AppLogger.w(TAG, "Lecture de notre propre signature impossible : ${e.message}")
        emptySet()
    }

    private fun signatureDigests(info: android.content.pm.PackageInfo?): Set<String> {
        if (info == null) return emptySet()
        val moderne = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.signingInfo?.let { si ->
                if (si.hasMultipleSigners()) si.apkContentsSigners else si.signingCertificateHistory
            }
        } else null
        // Repli volontaire sur l'API dépréciée : pour une archive sur API 28, `signatures` est le
        // SEUL champ renseigné. Ne pas « moderniser » ce repli sans l'avoir testé sur véhicule.
        @Suppress("DEPRECATION")
        val signatures = moderne ?: info.signatures ?: return emptySet()

        val md = MessageDigest.getInstance("SHA-256")
        return signatures.mapNotNull { sig ->
            sig?.toByteArray()?.let { md.digest(it).joinToString("") { b -> "%02x".format(b) } }
        }.toSet()
    }
}
