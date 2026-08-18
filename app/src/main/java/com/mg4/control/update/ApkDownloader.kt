package com.mg4.control.update

import com.mg4.control.debug.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Télécharge un fichier APK depuis une URL vers un fichier local.
 * Gère les redirections HTTP/HTTPS (GitHub CDN).
 * La progression (0–100) est remontée via [onProgress] sur le thread appelant.
 */
object ApkDownloader {

    private const val TAG = "MG4_UPDATE"

    /** Plafond de taille : l'APK fait ~10 Mo, au-delà on refuse plutôt que remplir le disque. */
    private const val MAX_APK_BYTES = 100L * 1024 * 1024

    suspend fun download(
        url: String,
        destFile: File,
        onProgress: suspend (Int) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // Supprimer un éventuel fichier précédent
            if (destFile.exists()) destFile.delete()

            if (!ApkUrlPolicy.isAllowedLogged(url, "Téléchargement")) return@withContext false

            val conn = openConnection(url) ?: return@withContext false
            if (conn.responseCode != 200) {
                conn.disconnect()
                return@withContext false
            }

            val totalBytes = conn.contentLengthLong
            if (totalBytes > MAX_APK_BYTES) {
                conn.disconnect()
                return@withContext false
            }
            var downloaded = 0L

            conn.inputStream.use { input ->
                destFile.outputStream().use { output ->
                    val buffer = ByteArray(8_192)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloaded += read
                        // Le Content-Length est déclaratif : on coupe aussi sur les octets réels.
                        if (downloaded > MAX_APK_BYTES) {
                            destFile.delete()
                            return@withContext false
                        }
                        if (totalBytes > 0) {
                            val pct = (downloaded * 100L / totalBytes).toInt()
                            withContext(Dispatchers.Main) { onProgress(pct) }
                        }
                    }
                }
            }

            conn.disconnect()

            // ⚠️ Un transfert peut s'arreter SANS lever d'exception : connexion coupee, keep-alive
            // rompu, reseau de la voiture qui decroche. La boucle se termine alors normalement et
            // laisse un fichier court mais non vide, que `length() > 0` laissait passer.
            //
            // Cet APK tronque partait ensuite au verificateur de signature, qui n'y lisait aucun
            // certificat et annoncait « Signature de l'APK non conforme ». Le symptome accusait la
            // cle de signature alors que la cause etait le reseau : compter les octets evite
            // d'envoyer enqueter sur la mauvaise piste.
            if (totalBytes > 0 && downloaded != totalBytes) {
                AppLogger.w(TAG, "Telechargement incomplet : $downloaded/$totalBytes octets " +
                    "— APK rejete (reseau interrompu, ce n'est PAS un probleme de signature)")
                destFile.delete()
                return@withContext false
            }
            if (downloaded == 0L) {
                AppLogger.w(TAG, "Telechargement vide — APK rejete")
                destFile.delete()
                return@withContext false
            }
            // sha256 + taille : a comparer avec les lignes publiees par la CI (etape
            // "Verify signature schemes" de beta.yml). Deux empreintes identiques prouvent que
            // l'octet a octet est intact ; une divergence designe le transfert, pas la signature.
            AppLogger.i(TAG, "APK telecharge : $downloaded octets, sha256=${sha256(destFile)}")
            destFile.exists() && destFile.length() > 0

        } catch (_: Exception) {
            destFile.delete()
            false
        }
    }

    /**
     * Ouvre une connexion en suivant manuellement les redirections (GitHub → CDN).
     * Chaque saut est revalidé : sans cela un `Location:` en http suffit à faire
     * accepter une rétrogradation https → http ou un hôte arbitraire.
     * Renvoie null si un saut sort de la liste d'origines autorisées.
     */
    /** Empreinte du fichier recu, pour la confronter a celle publiee par la CI. */
    private fun sha256(f: File): String = try {
        val md = java.security.MessageDigest.getInstance("SHA-256")
        f.inputStream().use { input ->
            val buf = ByteArray(8_192)
            var read: Int
            while (input.read(buf).also { read = it } != -1) md.update(buf, 0, read)
        }
        md.digest().joinToString("") { "%02x".format(it) }
    } catch (e: Exception) {
        "(illisible: ${e.message})"
    }

    private fun openConnection(urlStr: String, depth: Int = 0): HttpURLConnection? {
        if (!ApkUrlPolicy.isAllowedLogged(urlStr, "Redirection")) return null
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            // Les redirections sont suivies à la main pour pouvoir contrôler chaque saut.
            instanceFollowRedirects = false
            setRequestProperty("User-Agent", "MG4Control-Android")
            connectTimeout = 15_000
            readTimeout    = 120_000
            connect()
        }
        val code = conn.responseCode
        if ((code == 301 || code == 302 || code == 303 || code == 307 || code == 308) && depth < 5) {
            val location = conn.getHeaderField("Location") ?: return conn
            conn.disconnect()
            return openConnection(location, depth + 1)
        }
        return conn
    }
}
