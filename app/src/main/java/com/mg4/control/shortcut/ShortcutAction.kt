package com.mg4.control.shortcut

enum class ShortcutAction(val id: Int) {
    NONE(0),
    ONE_PEDAL(1),
    AEB_CYCLE(2),
    SOUND_WARNING(3),
    OVERSPEED_ALARM(4),
    SPEED_LIMIT_TONE(5),
    ADAS_CYCLE(6),
    OPEN_APP(7),
    OPEN_CUSTOM_APP(8),
    ENERGY_SAVING_TOGGLE(9),
    TSR_TOGGLE(10),
    APPLY_PROFILE(11),
    PROFILE_PICKER(12),
    VEHICLE_POWER_OFF(13),

    // Actions « lues puis écrites » : elles interrogent le véhicule à chaque pression plutôt
    // que de suivre un état mémorisé. Indispensable ici — l'utilisateur peut agir sur la clim
    // ou l'ESC depuis l'écran d'origine, un état gardé en mémoire dériverait aussitôt.
    ESC_TOGGLE(14),
    DROWSINESS_TOGGLE(15),
    DROWSINESS_SEN_CYCLE(16),
    HVAC_TOGGLE(17),
    HVAC_TEMP_UP(18),
    HVAC_TEMP_DOWN(19),
    HVAC_FAN_UP(20),
    HVAC_FAN_DOWN(21);

    companion object {
        fun fromId(id: Int) = values().firstOrNull { it.id == id } ?: NONE
    }
}

enum class PressType(val key: String) {
    SINGLE("single"),
    LONG("long"),
    DOUBLE("double");

    companion object {
        fun fromKey(key: String) = values().firstOrNull { it.key == key } ?: LONG
    }
}
