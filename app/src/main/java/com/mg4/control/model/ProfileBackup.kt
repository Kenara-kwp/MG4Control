package com.mg4.control.model

/**
 * Format sérialisé de la sauvegarde des profils (fichier dans la mémoire de la voiture).
 *
 * [schemaVersion] permet de faire évoluer le format sans casser d'anciennes sauvegardes
 * (Gson ignore les champs inconnus et applique les valeurs par défaut des champs manquants).
 */
data class ProfileBackup(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val defaultId: String? = null,
    val profiles: List<DrivingProfile> = emptyList()
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
    }
}
