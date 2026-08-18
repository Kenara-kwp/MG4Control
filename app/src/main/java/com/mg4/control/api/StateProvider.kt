package com.mg4.control.api

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import com.mg4.control.debug.AppLogger
import com.mg4.control.hardware.MG4Hardware
import com.mg4.control.profile.ProfileManager
import com.mg4.control.util.FirmwareInfo

/**
 * Lecture de l'état véhicule par les applications tierces — `content://com.mg4.control.state/state`.
 *
 * Pourquoi un ContentProvider et pas un broadcast : un broadcast ne sait pas retourner de valeur,
 * et surtout il ne porte AUCUNE identité d'émetteur. Ici `callingPackage` est fiable (fourni par la
 * plateforme), ce qui permet à la fois de journaliser qui lit et d'appliquer une liste blanche.
 *
 * Lecture seule — aucune écriture véhicule ne passe par ce composant, [insert]/[update]/[delete]
 * sont volontairement inertes.
 *
 * Le format est un curseur d'UNE ligne, une colonne par valeur : c'est ce que savent consommer
 * Tasker et les outils d'automatisation. Une valeur illisible sort à `null` plutôt qu'à zéro — un
 * zéro se confondrait avec « siège éteint » ou « à l'arrêt ».
 */
class StateProvider : ContentProvider() {

    private companion object {
        val COLUMNS = arrayOf(
            "drive_mode", "regen", "seat_heat_left", "seat_heat_right", "steering_heat",
            "speed_kmh", "outside_temp_c", "tsr", "energy_saving", "aeb_enabled",
            "firmware", "profiles", "default_profile"
        )
    }

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri, projection: Array<out String>?, selection: String?,
        selectionArgs: Array<out String>?, sortOrder: String?
    ): Cursor? {
        val ctx = context ?: return null
        val caller = callingPackage

        if (!ExternalApi.isEnabled(ctx)) {
            AppLogger.i(ExternalApi.LOG_TAG, "LECTURE refusée (appelant=$caller) — API désactivée")
            return null
        }
        if (!ExternalApi.isCallerAllowed(ctx, caller)) {
            AppLogger.w(ExternalApi.LOG_TAG, "LECTURE refusée — $caller hors liste blanche")
            return null
        }

        val pm = ProfileManager(ctx)
        val profiles = pm.getAll()
        val cursor = MatrixCursor(COLUMNS)
        cursor.addRow(arrayOf(
            MG4Hardware.getDriveMode()?.name,
            MG4Hardware.getRegenLevel()?.name,
            MG4Hardware.getSeatHeatLeft().takeIf { it >= 0 },
            MG4Hardware.getSeatHeatRight().takeIf { it >= 0 },
            if (MG4Hardware.isSteeringHeatOn()) 1 else 0,
            MG4Hardware.getVehicleSpeedKmh(),
            MG4Hardware.getOutsideTempCelsius(),
            if (MG4Hardware.isTsrOn()) 1 else 0,
            if (MG4Hardware.isEnergySavingOn()) 1 else 0,
            if (MG4Hardware.isAebEnabled()) 1 else 0,
            FirmwareInfo.getGeneration().name,
            profiles.joinToString("|") { it.name },
            profiles.firstOrNull { it.id == pm.getDefaultId() }?.name
        ))
        AppLogger.i(ExternalApi.LOG_TAG, "LECTURE par $caller")
        return cursor
    }

    override fun getType(uri: Uri): String =
        "vnd.android.cursor.item/vnd.${context?.packageName}.${ExternalApi.PATH_STATE}"

    // Lecture seule, par conception : aucune écriture véhicule ne doit passer par un provider
    // exporté. Les écritures ont leur propre chemin (ExternalApiReceiver), journalisé et
    // soumis au verrou de vitesse.
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun update(uri: Uri, values: ContentValues?, selection: String?,
                        selectionArgs: Array<out String>?): Int = 0
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
