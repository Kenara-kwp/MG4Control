![image info](mg4control_github_banner.svg)

[![Security](https://github.com/SliDeeN/MG4Control/actions/workflows/security.yml/badge.svg)](https://github.com/SliDeeN/MG4Control/actions/workflows/security.yml)
[![Release](https://github.com/SliDeeN/MG4Control/actions/workflows/release.yml/badge.svg)](https://github.com/SliDeeN/MG4Control/actions/workflows/release.yml)

> Application Android Automotive pour le contrôle avancé des paramètres de conduite du MG4 électrique.
> Android Automotive app for advanced driving settings control on the MG4 electric vehicle.

> Vous appréciez MG4Control et souhaitez soutenir son développement ?  
You enjoy MG4Control and want to support its development ?  
[![PayPal](https://img.shields.io/badge/Donate-PayPal-blue?logo=paypal)](https://www.paypal.com/paypalme/pfauquembergue)
[![Buy Me a Coffee](https://img.shields.io/badge/Buy%20Me%20a%20Coffee-ffdd00?logo=buy-me-a-coffee&logoColor=black)](https://buymeacoffee.com/slideen)
---

<details open>
<summary><strong>🇫🇷 Français</strong></summary>

## Table des matières
1. [Présentation](#présentation)
2. [Fonctionnalités](#fonctionnalités)
3. [Compatibilité](#compatibilité)
4. [Architecture](#architecture)
5. [Structure du projet](#structure-du-projet)
6. [Couches matérielles](#couches-matérielles)
7. [Système de profils](#système-de-profils)
8. [Interface utilisateur](#interface-utilisateur)
9. [API externe](#api-externe-keymapper-tasker)
10. [Compilation et installation](#compilation-et-installation)
11. [Permissions requises](#permissions-requises)

---

## Présentation

**MG4Control** est une application système conçue pour Android Automotive OS, destinée à fonctionner sur les écrans de bord des véhicules MG4 équipés du SoC **SAIC MT2712**. Elle offre un accès direct et unifié aux réglages de conduite qui ne sont pas accessibles — ou difficilement accessibles — via l'interface constructeur.

L'application communique avec le véhicule via le SDK propriétaire SAIC, en accédant aux services Android Automotive (`CarPropertyManager`, `CarHvacManager`) ainsi qu'aux services de bas niveau exposés par le firmware du véhicule.

> **Important :** Cette application nécessite des privilèges système (`sharedUserId="android.uid.system"`) et doit être signée avec la clé de la ROM. Elle ne peut pas fonctionner sur un appareil standard débloqué.

> [!WARNING]
> **MG4Control est un projet communautaire indépendant. Il n'est en aucun cas affilié, approuvé ou soutenu par MG Motor, SAIC Motor ou l'une de leurs filiales.**
> L'utilisation de cette application se fait entièrement à vos risques. Des réglages incorrects peuvent affecter le comportement du véhicule. Procédez avec précaution.

---

## Fonctionnalités

### Paramètres de conduite
- **Mode de conduite** : ECO / NORMAL / SPORT / SNOW / CUSTOM
- **Régénération** : Off / Faible / Moyen / Fort / Adaptatif / 1 Pédale

### Confort
- **Volant chauffant** : On / Off
- **Sièges chauffants gauche et droit** : Off / Niveau 1 / 2 / 3
- **Climatisation** : consigne de température, ventilation, marche/arrêt, A/C, AUTO,
  recirculation (intérieur / extérieur / auto), dégivrage avant et arrière

### ADAS (Assistance à la conduite)
- **SWI133** : Off / Limiteur / Auto / ACC / ICA + alertes excès de vitesse / changement de limite
- **SWI68** : Désactiver / ACC / TJA + avertissement sonore On / Off
- **SWI69 / SWI131** : Anti-collision avant (AEB) — On / Off + mode Alerte uniquement / Alerte + Freinage
- **SWI165** : Désactiver / ACC / TJA + Anti-collision avant (AEB) On/Off + mode Alerte / Alerte+Freinage + avertissement sonore

### Raccourcis volant
- Configuration des **boutons ★ gauche et droit**, en appui **simple** ou **long**
- Actions disponibles : 1 Pédale, cycle ADAS, cycle anticollision, alertes sonores,
  reconnaissance des panneaux, économie d'énergie, lancer un profil, sélecteur de profil,
  ouvrir MG4Control, lancer une application, éteindre le véhicule
- Réglages associés affichés **uniquement si l'action correspondante est attribuée**
  (niveau de repli du mode 1 Pédale, crans du cycle ADAS)
- Activation / désactivation des raccourcis avec **dialog d'avertissement**

### Automatisation
- **Application d'un profil selon la température extérieure** : seuil, sens
  (inférieure/supérieure), profil à appliquer, exécution directe ou popup de confirmation
- **Déclenchement A/C via la température** : deux règles indépendantes (température supérieure /
  inférieure), chacune avec son seuil, sa consigne, sa ventilation et ses dégivrages
- Chaque automatisation est dépliable indépendamment de son interrupteur d'activation

### Gestion de profils
- Sauvegarde jusqu'à **5 profils** personnalisés
- Application instantanée d'un profil en un clic
- Application automatique du profil par défaut **au démarrage du véhicule**

### Réglages
Écran organisé en **quatre onglets** :
- **Langues** : français, anglais, allemand, espagnol, italien, portugais
- **Interface** : écran affiché au démarrage, apparence (auto / sombre / clair)
- **Réglages avancés** : application automatique du profil, vérification des mises à jour au
  lancement, extinction du véhicule écran allumé, blocage des réglages de conduite au-delà d'une
  vitesse donnée, **API externe** (cf. section dédiée)
- **Infos** : vérification des mises à jour, nettoyage des APK, dialog « À propos » (version de
  l'app, firmware, QR codes), indicateur de firmware, et bouton Diagnostic révélé par 5 clics
  sur le logo

### Profils
- Liste des profils avec application, définition par défaut, modification, suppression
- **Éditeur en plein écran** organisé en trois catégories : Conduite, Sécurité, Confort
- Le nom du profil et le réglage « profil par défaut » restent visibles sur les trois onglets
- Volant et sièges chauffants disposent d'un interrupteur de **prise en compte** : décoché, le
  profil ne touche pas au réglage au lieu de l'éteindre

### Compatibilité firmware inconnue (UNKNOWN)
- Dialog d'avertissement au démarrage si le firmware n'est ni SWI133 ni SWI68
- L'utilisateur peut fermer l'application ou continuer
- En mode "Continuer", les pastilles de firmware (*Réglages → Infos*) deviennent cliquables pour
  forcer un mode de compatibilité
- Le choix forcé est persisté en SharedPreferences et survit aux redémarrages de l'app

---

## Compatibilité

| Élément | Valeur |
|---------|--------|
| Véhicule cible | MG4 Electric (SAIC) |
| OS | Android Automotive 9+ (API 28+) |
| SoC | SAIC MT2712 |
| Résolution d'écran | 1280 × 480 (orientation paysage forcée) |
| Firmware SWI133 | Compatible ✅ |
| Firmware SWI131 | Compatible ✅ |
| Firmware SWI132 | Compatible ✅ |
| Firmware SWI68 | Compatible ✅ |
| Firmware SWI69 | Compatible ✅ |
| Firmware SWI165 | Compatible ✅ |
| Firmware UNKNOWN | Mode forcé SWI133/SWI132/SWI68/SWI69/SWI131/SWI165 disponible ⚠️ |

---

## Architecture

### Vue d'ensemble

```
┌──────────────────────────────────────────────────────┐
│                    INTERFACE                          │
│  MainActivity ─── NavController ─── Fragment Host   │
│  ┌─────────────┐  ┌──────────────┐  ┌─────────────┐ │
│  │  Dashboard  │  │   Profils    │  │  Réglages   │ │
│  └─────────────┘  └──────────────┘  └─────────────┘ │
└──────────────────────────────────────────────────────┘
                         │
┌──────────────────────────────────────────────────────┐
│                 COUCHE MÉTIER                         │
│  ProfileManager  ─  ProfileApplier  ─  FirmwareInfo  │
└──────────────────────────────────────────────────────┘
                         │
┌──────────────────────────────────────────────────────┐
│            ABSTRACTION MATÉRIELLE (MG4Hardware)       │
│  Katman1 (Car API) → Katman2 (Binder) → Katman4      │
│                      (ADAS / SWI133 / SWI68)          │
└──────────────────────────────────────────────────────┘
                         │
┌──────────────────────────────────────────────────────┐
│              SERVICES SYSTÈME & BOOT                  │
│      MG4ControlService  ─────  BootReceiver          │
└──────────────────────────────────────────────────────┘
```

### Démarrage de l'application

```
Démarrage véhicule
       │
       ▼
BootReceiver.onReceive()
       │
       ▼
MG4ControlService.onCreate()
  └─ MG4Hardware.init()
  └─ Découverte des services Katman1 / Katman4
  └─ Application du profil par défaut (si activé)
       │
       ▼
MainActivity (IHM)
  └─ FirmwareInfo.initWithContext()     ← charge mode forcé (SharedPreferences)
  └─ Détection du firmware (SWI133 / SWI68 / UNKNOWN)
  └─ Configuration de la top bar (chips firmware)
  └─ checkUnknownFirmware()             ← dialog si UNKNOWN et non forcé
  └─ Navigation vers DashboardFragment
```

---

## Structure du projet

```
MG4Control/
├── app/src/main/
│   ├── java/com/mg4/control/
│   │   ├── MG4App.kt                  # Application — mode nuit, locale
│   │   ├── MainActivity.kt            # Activité principale, top bar, navigation
│   │   │
│   │   ├── model/
│   │   │   ├── DrivingProfile.kt      # Modèle de données d'un profil
│   │   │   ├── DriveMode.kt           # Enum modes de conduite (ECO/NORMAL/SPORT/SNOW/CUSTOM)
│   │   │   └── RegenLevel.kt          # Enum niveaux de régénération
│   │   │
│   │   ├── profile/
│   │   │   ├── ProfileManager.kt      # CRUD profils (SharedPreferences + Gson)
│   │   │   └── ProfileApplier.kt      # Application des réglages au véhicule (async)
│   │   │
│   │   ├── hardware/
│   │   │   └── MG4Hardware.kt         # Abstraction matérielle (4 couches)
│   │   │
│   │   ├── api/
│   │   │   ├── ExternalApi.kt         # Contrat de l'API externe (actions, clés, verrous)
│   │   │   ├── ExternalApiReceiver.kt # Réception des intents tiers
│   │   │   └── StateProvider.kt       # Lecture de l'état (ContentProvider)
│   │   │
│   │   ├── automation/
│   │   │   ├── AutomationSettings.kt        # Profil selon la température
│   │   │   ├── AutomationDecision.kt        # Décision pure (testable hors Android)
│   │   │   ├── ClimateAutomationSettings.kt # Déclenchement A/C
│   │   │   └── ClimateAutomationDecision.kt
│   │   │
│   │   ├── ui/
│   │   │   ├── DashboardFragment.kt   # Écran principal (rail 3 catégories)
│   │   │   ├── ProfileFragment.kt     # Liste des profils
│   │   │   ├── ProfileEditFragment.kt # Éditeur plein écran (rail 3 catégories)
│   │   │   ├── SettingsFragment.kt    # Réglages (rail 4 onglets)
│   │   │   ├── ShortcutsFragment.kt   # Raccourcis volant (rail 2 onglets)
│   │   │   ├── AutomationFragment.kt  # Automatisations
│   │   │   ├── AudioFragment.kt       # Audio (A9 uniquement)
│   │   │   ├── ProfileAdapter.kt      # Adaptateur RecyclerView profils
│   │   │   ├── ConsoleFragment.kt     # Journal de debug en temps réel
│   │   │   ├── DriveRegenFragment.kt  # Héritage (non utilisé en v2)
│   │   │   ├── ClimateFragment.kt     # Héritage (non utilisé en v2)
│   │   │   └── AdasFragment.kt        # Héritage (non utilisé en v2)
│   │   │
│   │   ├── service/
│   │   │   └── MG4ControlService.kt   # Service de premier plan (boot + auto-apply)
│   │   │
│   │   ├── receiver/
│   │   │   └── BootReceiver.kt        # Récepteur de démarrage système
│   │   │
│   │   ├── util/
│   │   │   ├── FirmwareInfo.kt        # Détection firmware (SWI133/SWI68/UNKNOWN) + mode forcé
│   │   │   ├── FirmwareHelper.kt      # Lecture version firmware complète (async)
│   │   │   └── LocaleHelper.kt        # Gestion de la langue (FR / EN)
│   │   │
│   │   └── update/
│   │       ├── UpdateChecker.kt       # Vérification dernière release GitHub (API)
│   │       ├── UpdateDialogManager.kt # Dialog MAJ + DownloadManager + ouverture dossier
│   │   │
│   │   └── debug/
│   │       └── AppLogger.kt           # Buffer de logs en mémoire (400 entrées)
│   │
│   ├── res/
│   │   ├── layout/
│   │   │   ├── activity_main.xml      # Top bar + NavHostFragment
│   │   │   ├── fragment_dashboard.xml # Écran principal (conduite + climat + alertes)
│   │   │   ├── fragment_profile.xml   # Liste des profils
│   │   │   ├── fragment_settings.xml  # Réglages
│   │   │   ├── item_profile.xml       # Item liste de profil
│   │   │   ├── dialog_profile_edit.xml       # Dialog création / édition de profil
│   │   │   ├── dialog_app_info.xml           # Dialog "À propos"
│   │   │   └── dialog_unknown_firmware.xml   # Dialog firmware inconnu (UNKNOWN)
│   │   ├── navigation/nav_graph.xml   # Dashboard → Profils / Réglages
│   │   ├── values/strings.xml         # Chaînes FR
│   │   ├── values-en/strings.xml      # Chaînes EN
│   │   └── values/colors.xml          # Palette dash_* (dark theme)
│   │
│   └── AndroidManifest.xml
│
└── mockup/
    └── index.html                     # Maquette interactive HTML 1280×480
```

---

## Couches matérielles

`MG4Hardware` est organisé en **4 couches d'accès**, du plus haut niveau au plus bas, avec repli automatique en cas d'échec.

### Katman1 — Android Automotive Car API
Couche principale. Utilise les APIs officielles Android Automotive :
- `CarPropertyManager` → modes de conduite, régénération, pédale unique
- `CarHvacManager` → siège chauffant, volant chauffant

La connexion est initialisée par réflexion sur `Car.createCar()` avec plusieurs surcharges tentées successivement. Les actions en attente sont mises en file d'attente et exécutées dès que le service est prêt.

### Katman2 — Raw Binder (fallback)
Repli sur `ServiceManager.getService("vehiclesetting")` avec appels `binderTransact()` directs. Souvent bloqué par SELinux en production.

### Katman4 — Services ADAS (firmware-specific)
Couche dédiée aux fonctions ADAS, chargée dynamiquement selon la génération de firmware :

| Firmware | Service | Mécanisme |
|----------|---------|-----------|
| **SWI133** | `VehiclePropertyManager` | Chargé depuis l'APK launcher via `ClassLoader` + réflexion sur `mIVehiclePropertyService`. Utilise `getMixProperty()` / `setMixProperty()` |
| **SWI68** | `VehicleSettingManager` | Singleton statique chargé via réflexion. Utilise `setAccTjaMode()` / `setLaneKeepingWarningSound()` |
| **SWI69 / SWI131** | `VehicleSettingManager` | Même singleton que SWI68. Utilise `setFcwState()` / `getFcwState()` / `setFcwAutoBrakeMode()` / `setFcwSensitivity()` pour l'AEB. Valeurs confirmées empiriquement sur véhicule réel : `setFcwState(1)` = DÉSACTIVER, `setFcwState(2)` = ACTIVER. |
| **SWI165** | `VehicleSettingManager` | Même SDK que SWI68 (`com.saicmotor.sdk.vehiclesettings`). ADAS via `setAccTjaMode()`. AEB via `setAutoEmergencyBraking(1/2)` comme toggle principal + `setFcwAlarmMode(1/2)` + `setFcwAutoBrakeMode(1/2)`. Modes : 1=OFF, 2=ON. |

### Détection du firmware

```kotlin
// util/FirmwareInfo.kt
FirmwareInfo.initWithContext(context)   // Charge le mode forcé depuis SharedPreferences
val gen = FirmwareInfo.getGeneration()  // Lit ro.build.mt2712.version
// → Gen.SWI133 | Gen.SWI68 | Gen.UNKNOWN

// Si firmware inconnu, l'utilisateur peut forcer un mode :
FirmwareInfo.forceGeneration(context, FirmwareInfo.Gen.SWI133)
FirmwareInfo.isForced(context)          // true si mode forcé actif
FirmwareInfo.getDetectedString()        // Ex : "SWI69-12345" (brut)
```

Le résultat est mis en cache. Si le firmware est `UNKNOWN` et aucun mode forcé, un dialog d'avertissement s'affiche au démarrage. L'utilisateur peut choisir de continuer et forcer SWI133 ou SWI68 via les chips de la top bar.

---

## Système de profils

### Modèle `DrivingProfile`

```kotlin
data class DrivingProfile(
    val id: String,             // UUID unique
    val name: String,           // Nom affiché
    val driveMode: DriveMode,   // ECO / NORMAL / SPORT / SNOW / CUSTOM
    val regenLevel: RegenLevel, // OFF / LOW / MEDIUM / HIGH / ADAPTIVE / ONE_PEDAL
    val steeringHeat: Boolean,
    val seatHeatLeft: Int,      // 0–3
    val seatHeatRight: Int,     // 0–3
    // SWI133 uniquement :
    val overspeedAlarm: Boolean,
    val speedLimitTone: Boolean,
    val adasMode: Int,          // 0=Off 1=Lim 2=Auto 3=ACC 4=ICA
    // SWI68 uniquement :
    val soundWarning: Boolean,
    val swi68AdasMode: Int      // Swi68Mode.OFF / ACC / TJA
)
```

### Persistance

Les profils sont sérialisés en JSON via **Gson** et stockés dans `SharedPreferences`. Maximum **5 profils** par appareil.

### Application d'un profil

`ProfileApplier.apply()` exécute les appels matériels dans l'ordre suivant sur `Dispatchers.IO` :
1. Mode de conduite (rapide — binder)
2. Niveau de régénération (rapide — binder)
3. Volant chauffant (~2 s — polling de confirmation d'état)
4. Siège gauche (~7 s — polling par toggle)
5. Siège droit (~7 s — polling par toggle)
6. Attente Katman4 → ADAS (selon firmware)

---

## Interface utilisateur

### Navigation
L'application utilise un **NavController** avec **7 destinations** :

```
DashboardFragment (départ)
    ├──► ProfileFragment ──► ProfileEditFragment  (création / édition, plein écran)
    ├──► SettingsFragment
    ├──► ShortcutsFragment
    ├──► AudioFragment        (A9 uniquement)
    └──► AutomationFragment
```

Les boutons de la barre du haut fonctionnent en bascule : un second appui revient au dashboard.

### Rail de catégories
Quatre écrans partagent le même motif : un **rail vertical à gauche** sélectionne une catégorie,
le contenu défile à droite, et ce qui n'appartient à aucune catégorie reste dans un bandeau
persistant (nom du profil, interrupteur maître) ou en pied de page (Annuler / Enregistrer / Fermer).

| Écran | Onglets |
|---|---|
| Dashboard | Conduite · Sécurité · Confort |
| Éditeur de profil | Conduite · Sécurité · Confort |
| Réglages | Langues · Interface · Réglages avancés · Infos |
| Raccourcis | Boutons · Actions |

Un onglet dont la page n'a plus aucune section visible sur le firmware courant est **masqué** —
mieux vaut pas d'onglet qu'un onglet qui ouvre une page vide.

### Dimensionnement
Valeurs communes aux écrans refondus, calées sur la lisibilité au volant : titres **20sp**,
en-têtes de section **13sp**, libellés et boutons **16sp**, hauteur de bouton **52dp**, onglets du
rail **64dp**, rail **180dp**, padding de carte **14dp**.

### Palette de couleurs

L'application suit le thème clair ou sombre. Les valeurs claires sont dans
`res/values/colors.xml`, les sombres dans `res/values-night/colors.xml` — **mêmes noms de token
des deux côtés**, c'est la seule règle à respecter en ajoutant une couleur.

| Token | Clair | Sombre | Usage |
|-------|-------|--------|-------|
| `dash_bg` | `#F2F2F7` | `#0C0C0E` | Fond général |
| `dash_card` | `#FFFFFF` | `#141416` | Cartes |
| `dash_section` | `#F2F2F7` | `#1C1C1F` | Sections internes |
| `dash_border` | `#D1D1D6` | `#2A2A2E` | Bordures et séparateurs |
| `dash_btn` | `#E5E5EA` | `#222226` | Fond de bouton inactif |
| `dash_text_lo` | `#8E8E93` | `#52525B` | En-têtes de section |
| `dash_accent` | `#0284C7` | `#38BDF8` | Sélection active (bleu) |
| `dash_accent_dim` | `#E0F2FE` | `#0C4A6E` | Fond de la sélection active |
| `dash_eco` | `#16A34A` | `#22C55E` | Mode ECO (vert) |
| `dash_warn` | `#D97706` | `#F59E0B` | Avertissement (orange) |
| `dash_danger` | `#E11D48` | `#F43F5E` | Suppression / danger |
| `text_primary` | `#1C1C1E` | `#FFFFFF` | Texte principal |
| `text_secondary` | `#6C6C70` | `#B0B0B0` | Texte secondaire |

Chaque couleur `*_dim` est le fond associé à sa couleur vive : `dash_eco_dim`, `dash_warn_dim` et
`dash_danger_dim` suivent le même principe que `dash_accent_dim`.

> **Piège de nommage :** `bg_dark` vaut `#FFFFFF` en thème clair. Le nom date d'une époque où
> l'application n'avait qu'un thème sombre ; il désigne le fond général, pas une couleur foncée.

---

## API externe (KeyMapper, Tasker…)

Permet à une application tierce de déclencher les fonctions de MG4Control (issue #79).

> **Désactivée par défaut.** Elle s'active dans *Réglages → Réglages avancés → « API externe »*,
> avec une confirmation explicite. Tant qu'elle est désactivée, toute commande reçue est refusée
> et journalisée. Une fois activée, **n'importe quelle application installée** peut envoyer ces
> intents : ils ne sont protégés par aucune permission, car KeyMapper et Tasker viennent du Play
> Store et ne peuvent pas en détenir une de niveau `signature`.

### Actions directes — une action d'intent par commande

Aucun extra requis : c'est la forme utilisable depuis **KeyMapper**, dont l'éditeur d'intent ne
propose que le type (*Broadcast receiver*) et la chaîne d'action.

| Action | Effet |
|---|---|
| `com.mg4.control.action.ONE_PEDAL` | Bascule 1 pédale ↔ niveau de repli |
| `com.mg4.control.action.ENERGY_SAVING_TOGGLE` | Économie d'énergie |
| `com.mg4.control.action.PROFILE_PICKER` | Ouvre le sélecteur de profil à l'écran |
| `com.mg4.control.action.OPEN_APP` | Ouvre MG4Control |

Ce sont des **bascules** : chaque envoi inverse l'état, il n'existe pas de « mettre à ON ».

> **Commandes volontairement hors API.** `VEHICLE_POWER_OFF`, `ADAS_CYCLE`, `AEB_CYCLE`,
> `TSR_TOGGLE`, `OVERSPEED_ALARM`, `SPEED_LIMIT_TONE` et `SOUND_WARNING` ne sont **pas** exposées :
> elles touchent à la sécurité active ou coupent le véhicule. Le refus s'applique aussi à
> `EXECUTE` — les retirer des seules actions directes n'aurait rien protégé. Elles restent
> pilotables depuis l'application et les raccourcis volant.

**Dans KeyMapper** : ajouter une action → *Intent* (version 2.3.0 minimum) → type
**Broadcast receiver** → coller la chaîne dans le champ *Action*.

### `EXECUTE` — pour Tasker, adb, scripts

`com.mg4.control.action.EXECUTE` avec un extra texte `action` valant l'un des noms ci-dessus, plus
deux commandes que les actions directes ne peuvent pas couvrir :

- `APPLY_PROFILE` — exige un extra `profile` : le nom du profil, insensible à la casse
- `OPEN_CUSTOM_APP` — ouvre l'application configurée dans les raccourcis

```bash
adb shell am broadcast -a com.mg4.control.action.EXECUTE \
  --es action APPLY_PROFILE --es profile "Trajet domicile"
```

### `SET` — écriture directe d'une valeur

`com.mg4.control.action.SET` avec les extras `key` et `value` :

| `key` | `value` accepté |
|---|---|
| `drive_mode` | `ECO` `NORMAL` `SPORT` `SNOW` `CUSTOM` |
| `regen` | `OFF` `LOW` `MEDIUM` `HIGH` `ADAPTIVE` `ONE_PEDAL` |
| `seat_heat_left` | `0` à `3` |
| `seat_heat_right` | `0` à `3` |
| `steering_heat` | `0`/`1` ou `false`/`true` |
| `profile` | nom du profil |
| `hvac_power` | `0`/`1` — marche/arrêt de la clim |
| `ac` | `0`/`1` — compresseur A/C |
| `hvac_auto` | `0`/`1` — mode automatique |
| `hvac_temp` | °C, clampé aux bornes réelles du véhicule |
| `hvac_fan` | niveau de ventilation, clampé aux bornes réelles |
| `hvac_recirc` | `INNER` `OUTSIDE` `AUTO` (ou `0` `1` `2`) |
| `defrost_front` | `0`/`1` |
| `defrost_rear` | `0`/`1` |

Les clés `hvac_*` et `defrost_*` sont ignorées si le firmware n'expose pas la climatisation.
Consigne et ventilation sont clampées aux bornes **lues sur le véhicule**, qui diffèrent d'un
firmware à l'autre. Ces commandes sont des bascules matérielles qui avancent d'un cran à la fois :
comptez quelques secondes avant que l'état final soit atteint.

```bash
adb shell am broadcast -a com.mg4.control.action.SET --es key drive_mode --es value SPORT
```

#### `NEXT` / `PREV` / `TOGGLE` — cycler sans connaître l'état

À la place d'une consigne, `value` accepte **`NEXT`** (cran suivant), **`PREV`** (cran précédent)
ou **`TOGGLE`** (alias de `NEXT`, plus lisible sur un booléen). La nouvelle valeur est calculée à
partir de l'état lu sur le véhicule, ce qui permet d'assigner « siège chauffant +1 » à un seul
bouton de volant. Le cycle **reboucle** : au maximum, le cran suivant revient au minimum.

```bash
adb shell am broadcast -a com.mg4.control.action.SET --es key seat_heat_left --es value NEXT
adb shell am broadcast -a com.mg4.control.action.SET --es key ac --es value TOGGLE
```

Clés cyclables : `seat_heat_left`, `seat_heat_right`, `steering_heat`, `hvac_power`, `ac`,
`hvac_auto`, `hvac_temp`, `hvac_fan`, `hvac_recirc`, `defrost_front`, `defrost_rear`.

`drive_mode`, `regen` et `profile` en sont **volontairement exclus** : l'énumération des modes de
conduite n'est pas filtrée par firmware (on cyclerait vers un mode absent du véhicule), la
disponibilité de la régénération dépend de l'état courant (aucun niveau en mode Neige, One Pedal
seul quand Éco énergie est actif), et il n'existe pas de notion de « profil courant ».

Si l'état courant est illisible, la commande est **refusée sans rien écrire** plutôt que de partir
d'une valeur supposée — un point de départ deviné ferait descendre la valeur alors que vous
appuyez pour la monter. Le refus est journalisé (`adb logcat -s MG4_API`).

### Lecture de l'état — ContentProvider

`content://com.mg4.control.state/state` (ou `com.mg4.control.offline.state` pour la variante
offline — l'authority suit l'applicationId). Un curseur d'**une** ligne :

`drive_mode`, `regen`, `seat_heat_left`, `seat_heat_right`, `steering_heat`, `speed_kmh`,
`outside_temp_c`, `tsr`, `energy_saving`, `aeb_enabled`, `firmware`, `profiles` (noms séparés
par `|`), `default_profile`.

Une valeur illisible vaut `null`, jamais `0` — un zéro se confondrait avec « siège éteint » ou
« véhicule à l'arrêt ». Tasker sait interroger un ContentProvider, KeyMapper non.

Contrairement aux broadcasts, un provider connaît son appelant : chaque lecture est journalisée
nominativement, et la préférence `external_api_allowlist` (liste de paquets séparés par des
virgules, vide = tous acceptés) est réellement appliquée.

### Sécurité et diagnostic

Le **verrou de vitesse** (*Réglages → « Bloquer les réglages de conduite au-delà d'une certaine
vitesse »*) s'applique aussi à l'API, puisqu'il est posé dans les primitives d'écriture. Attention :
il est lui-même **désactivé par défaut** — s'il ne l'est pas, aucune limite de vitesse ne
s'applique aux commandes externes. Le confort (sièges, volant chauffants) n'est jamais concerné.

Toute commande, acceptée ou refusée, est tracée au tag **`MG4_API`** (visible via le bouton
Diagnostic). Pour tester l'application indépendamment de KeyMapper :

```bash
adb shell am broadcast -a com.mg4.control.action.PROFILE_PICKER
```

Silence complet = APK pas à jour ou service arrêté. `REFUS … API externe désactivée` =
l'interrupteur des Réglages n'a pas été confirmé.

---

## Compilation et installation

Vous pouvez directement télécharger la dernière version de MG4Control via les releases : https://github.com/SliDeeN/MG4Control/releases
Il ne vous faut qu'une clé USB et l'accès aux paramètres AAOS afin d'installer l'APK.


Vous pouvez aussi compiler vous même le projet :

### Prérequis
- Android Studio Hedgehog (2023.1) ou supérieur
- JDK 17+
- Android SDK API 34

### Build debug

```bash
# Avec le JDK d'Android Studio
JAVA_HOME="/path/to/Android Studio/jbr" ./gradlew assembleDebug
```

L'APK se trouve dans :
```
app/build/outputs/apk/debug/app-debug.apk
```

### Installation sur le véhicule

L'application nécessite d'être signée avec la clé système de la ROM. Sur un système de développement :

```bash
adb push app-debug.apk /sdcard/
adb shell pm install -r --system /sdcard/app-debug.apk
```

> Sur une ROM de production, l'APK doit être incluse dans le build système ou installée via un mécanisme OEM spécifique.

---

## Permissions requises

| Permission | Justification |
|-----------|---------------|
| `FOREGROUND_SERVICE` | Service de premier plan pour l'auto-apply |
| `WAKE_LOCK` | Empêche le sleep pendant l'application des réglages |
| `RECEIVE_BOOT_COMPLETED` | Démarrage automatique au boot |
| `CAR_POWERTRAIN` | Contrôle du mode de conduite et de la régénération |
| `CONTROL_CAR_CLIMATE` | Contrôle des sièges et du volant chauffants |
| `CAR_VENDOR_EXTENSION` | Extensions propriétaires SAIC |
| `CAR_ENERGY` | Informations batterie / motorisation |
| `INTERNET` | Vérification des mises à jour (GitHub API) |
| `DOWNLOAD_WITHOUT_NOTIFICATION` | Téléchargement silencieux de l'APK de mise à jour |
| `WRITE_EXTERNAL_STORAGE` | Enregistrement APK dans le dossier Téléchargements |

</details>

---

<details open>
<summary><strong>🇬🇧 English</strong></summary>

## Table of Contents
1. [Overview](#overview)
2. [Features](#features)
3. [Compatibility](#compatibility)
4. [Architecture](#architecture)
5. [Project Structure](#project-structure)
6. [Hardware Layers](#hardware-layers)
7. [Profile System](#profile-system)
8. [User Interface](#user-interface)
9. [External API](#external-api-keymapper-tasker)
10. [Build & Installation](#build--installation)
11. [Required Permissions](#required-permissions)

---

## Overview

**MG4Control** is a system-level application designed for Android Automotive OS, intended to run on the head unit of MG4 electric vehicles equipped with the **SAIC MT2712** SoC. It provides direct, unified access to driving settings that are unavailable — or poorly accessible — through the stock manufacturer interface.

The app communicates with the vehicle through the proprietary SAIC SDK, accessing Android Automotive services (`CarPropertyManager`, `CarHvacManager`) as well as low-level services exposed by the vehicle's firmware.

> **Important:** This application requires system privileges (`sharedUserId="android.uid.system"`) and must be signed with the ROM's platform key. It cannot run on a standard unlocked device.

> [!WARNING]
> **MG4Control is an independent community project. It is in no way affiliated with, endorsed by, or supported by MG Motor, SAIC Motor, or any of their subsidiaries.**
> Use this application entirely at your own risk. Incorrect settings may affect vehicle behaviour. Proceed with caution.

---

## Features

### Driving Settings
- **Drive mode**: ECO / NORMAL / SPORT / SNOW / CUSTOM
- **Regenerative braking**: Off / Low / Medium / High / Adaptive / One Pedal

### Comfort
- **Heated steering wheel**: On / Off
- **Heated seats (left & right)**: Off / Level 1 / 2 / 3
- **Climate control**: temperature setpoint, fan speed, power, A/C, AUTO, recirculation
  (inner / outside / auto), front and rear defrost

### ADAS (Advanced Driver Assistance)
- **SWI133**: Off / Speed Limiter / Auto / ACC / ICA + overspeed alert / speed limit change alert
- **SWI68**: Disable / ACC / TJA + audible warning On / Off
- **SWI69 / SWI131**: Forward Collision Warning (AEB) — On / Off + mode Alert only / Alert + Emergency Braking

### Steering Wheel Shortcuts
- Configure **4 steering wheel buttons** (left/right side buttons)
- Available actions: Drive mode / Regeneration / ADAS / **Open app**
- Enable/disable shortcuts with a **warning dialog**

### Profile Management
- Save up to **5 custom profiles**
- Instant one-tap profile application
- Automatic default profile application **on vehicle startup**

### Settings
- Language selection (French / English)
- Enable/disable automatic profile application
- **Auto-update**: GitHub release check + APK download to Downloads folder
- **APK cleanup**: removes old `MGControl*.apk` files from Downloads folder
- "About" dialog showing app version, firmware version, and GitHub QR code

---


## Compatibility

| Item | Value |
|------|-------|
| Target vehicle | MG4 Electric (SAIC) |
| OS | Android Automotive 9+ (API 28+) |
| SoC | SAIC MT2712 |
| Screen resolution | 1280 × 480 (forced landscape) |
| Firmware SWI133 | Compatible ✅ |
| Firmware SWI131 | Compatible ✅ |
| Firmware SWI132 | Compatible ✅ |
| Firmware SWI68 | Compatible ✅ |
| Firmware SWI69 | Compatible ✅ |
| Firmware SWI165 | Compatible ✅ |
| UNKNOWN firmware | Forced SWI133/SWI132/SWI68/SWI69/SWI131/SWI165 mode available ⚠️ |

---

## Architecture

### Overview

```
┌──────────────────────────────────────────────────────┐
│                      UI LAYER                         │
│  MainActivity ─── NavController ─── Fragment Host    │
│  ┌─────────────┐  ┌──────────────┐  ┌─────────────┐ │
│  │  Dashboard  │  │   Profiles   │  │  Settings   │ │
│  └─────────────┘  └──────────────┘  └─────────────┘ │
└──────────────────────────────────────────────────────┘
                         │
┌──────────────────────────────────────────────────────┐
│                  BUSINESS LOGIC                       │
│  ProfileManager  ─  ProfileApplier  ─  FirmwareInfo  │
└──────────────────────────────────────────────────────┘
                         │
┌──────────────────────────────────────────────────────┐
│           HARDWARE ABSTRACTION (MG4Hardware)          │
│  Katman1 (Car API) → Katman2 (Binder) → Katman4      │
│                      (ADAS / SWI133 / SWI68)          │
└──────────────────────────────────────────────────────┘
                         │
┌──────────────────────────────────────────────────────┐
│               SYSTEM SERVICES & BOOT                  │
│       MG4ControlService  ─────  BootReceiver         │
└──────────────────────────────────────────────────────┘
```

### Startup Sequence

```
Vehicle boot
       │
       ▼
BootReceiver.onReceive()
       │
       ▼
MG4ControlService.onCreate()
  └─ MG4Hardware.init()
  └─ Katman1 / Katman4 service discovery
  └─ Apply default profile (if enabled)
       │
       ▼
MainActivity (UI)
  └─ Firmware detection (SWI133 / SWI68)
  └─ Top bar setup
  └─ Navigate to DashboardFragment
```

---

## Project Structure

```
MG4Control/
├── app/src/main/
│   ├── java/com/mg4/control/
│   │   ├── MG4App.kt                  # Application — night mode, locale
│   │   ├── MainActivity.kt            # Main activity, top bar, navigation
│   │   │
│   │   ├── model/
│   │   │   ├── DrivingProfile.kt      # Profile data model
│   │   │   ├── DriveMode.kt           # Drive mode enum (ECO/NORMAL/SPORT/SNOW/CUSTOM)
│   │   │   └── RegenLevel.kt          # Regen level enum
│   │   │
│   │   ├── profile/
│   │   │   ├── ProfileManager.kt      # Profile CRUD (SharedPreferences + Gson)
│   │   │   └── ProfileApplier.kt      # Applies settings to vehicle (async)
│   │   │
│   │   ├── hardware/
│   │   │   └── MG4Hardware.kt         # Hardware abstraction (4 layers)
│   │   │
│   │   ├── ui/
│   │   │   ├── DashboardFragment.kt   # Unified main screen
│   │   │   ├── ProfileFragment.kt     # Profile management
│   │   │   ├── SettingsFragment.kt    # Settings & About
│   │   │   ├── ProfileAdapter.kt      # Profile list RecyclerView adapter
│   │   │   ├── ConsoleFragment.kt     # Real-time debug log viewer
│   │   │   ├── DriveRegenFragment.kt  # Legacy (unused in v2)
│   │   │   ├── ClimateFragment.kt     # Legacy (unused in v2)
│   │   │   └── AdasFragment.kt        # Legacy (unused in v2)
│   │   │
│   │   ├── service/
│   │   │   └── MG4ControlService.kt   # Foreground service (boot + auto-apply)
│   │   │
│   │   ├── receiver/
│   │   │   └── BootReceiver.kt        # System boot receiver
│   │   │
│   │   ├── util/
│   │   │   ├── FirmwareInfo.kt        # Firmware generation detection (SWI133 / SWI68)
│   │   │   ├── FirmwareHelper.kt      # Full firmware version string reader (async)
│   │   │   └── LocaleHelper.kt        # Language management (FR / EN)
│   │   │
│   │   └── debug/
│   │       └── AppLogger.kt           # In-memory log ring buffer (400 entries)
│   │
│   ├── res/
│   │   ├── layout/
│   │   │   ├── activity_main.xml      # Top bar + NavHostFragment
│   │   │   ├── fragment_dashboard.xml # Main screen (drive + climate + alerts)
│   │   │   ├── fragment_profile.xml   # Profile list
│   │   │   ├── fragment_settings.xml  # Settings screen
│   │   │   ├── item_profile.xml       # Profile list item
│   │   │   ├── dialog_profile_edit.xml# Profile create / edit dialog
│   │   │   └── dialog_app_info.xml    # About dialog
│   │   ├── navigation/nav_graph.xml   # Dashboard → Profiles / Settings
│   │   ├── values/strings.xml         # French strings
│   │   ├── values-en/strings.xml      # English strings
│   │   └── values/colors.xml          # dash_* color palette (dark theme)
│   │
│   └── AndroidManifest.xml
│
└── mockup/
    └── index.html                     # Interactive HTML mockup (1280×480)
```

---

## Hardware Layers

`MG4Hardware` is organized into **4 access layers**, from highest to lowest level, with automatic fallback on failure.

### Katman1 — Android Automotive Car API
Primary layer. Uses official Android Automotive APIs:
- `CarPropertyManager` → drive modes, regeneration, one-pedal
- `CarHvacManager` → seat heating, steering wheel heating

The connection is initialized via reflection on `Car.createCar()` with multiple overloads tried in sequence. Pending actions are queued and executed once the service is ready, with exponential backoff retry (2 s → 60 s).

### Katman2 — Raw Binder (fallback)
Falls back to `ServiceManager.getService("vehiclesetting")` with direct `binderTransact()` calls. Usually blocked by SELinux in production builds.

### Katman4 — ADAS Services (firmware-specific)
Dedicated layer for ADAS functions, dynamically loaded according to the detected firmware generation:

| Firmware | Service | Mechanism |
|----------|---------|-----------|
| **SWI133** | `VehiclePropertyManager` | Loaded from the launcher APK via `ClassLoader` + reflection on `mIVehiclePropertyService`. Uses `getMixProperty()` / `setMixProperty()` |
| **SWI68** | `VehicleSettingManager` | Static singleton loaded via reflection. Uses `setAccTjaMode()` / `setLaneKeepingWarningSound()` |
| **SWI69 / SWI131** | `VehicleSettingManager` | Same singleton as SWI68. Uses `setFcwState()` / `getFcwState()` / `setFcwAutoBrakeMode()` / `setFcwSensitivity()` for AEB. Values confirmed empirically on real hardware: `setFcwState(1)` = DISABLE, `setFcwState(2)` = ENABLE. |
| **SWI165** | `VehicleSettingManager` | Same SDK as SWI68 (`com.saicmotor.sdk.vehiclesettings`). ADAS via `setAccTjaMode()`. AEB via `setAutoEmergencyBraking(1/2)` as the main toggle + `setFcwAlarmMode(1/2)` + `setFcwAutoBrakeMode(1/2)`. Values: 1=OFF, 2=ON. |

### Firmware Detection

```kotlin
// util/FirmwareInfo.kt
val gen = FirmwareInfo.getGeneration()  // Reads ro.build.mt2712.version
// → Gen.SWI133 | Gen.SWI68 | Gen.UNKNOWN
```

The result is cached and used throughout the app to branch firmware-specific code paths.

---

## Profile System

### `DrivingProfile` Model

```kotlin
data class DrivingProfile(
    val id: String,             // Unique UUID
    val name: String,           // Display name
    val driveMode: DriveMode,   // ECO / NORMAL / SPORT / SNOW / CUSTOM
    val regenLevel: RegenLevel, // OFF / LOW / MEDIUM / HIGH / ADAPTIVE / ONE_PEDAL
    val steeringHeat: Boolean,
    val seatHeatLeft: Int,      // 0–3
    val seatHeatRight: Int,     // 0–3
    // SWI133 only:
    val overspeedAlarm: Boolean,
    val speedLimitTone: Boolean,
    val adasMode: Int,          // 0=Off 1=Limiter 2=Auto 3=ACC 4=ICA
    // SWI68 only:
    val soundWarning: Boolean,
    val swi68AdasMode: Int      // Swi68Mode.OFF / ACC / TJA
)
```

### Persistence

Profiles are serialized to JSON via **Gson** and stored in `SharedPreferences`. Maximum **5 profiles** per device.

### Applying a Profile

`ProfileApplier.apply()` executes hardware calls in the following order on `Dispatchers.IO`:
1. Drive mode (fast — binder call)
2. Regen level (fast — binder call)
3. Heated steering wheel (~2 s — state confirmation polling)
4. Left seat heating (~7 s — toggle polling)
5. Right seat heating (~7 s — toggle polling)
6. Wait for Katman4 → ADAS (firmware-dependent)

---

## User Interface

### Navigation
The app uses a **NavController** with **7 destinations**:

```
DashboardFragment (start)
    ├──► ProfileFragment ──► ProfileEditFragment  (create / edit, full screen)
    ├──► SettingsFragment
    ├──► ShortcutsFragment
    ├──► AudioFragment        (A9 only)
    └──► AutomationFragment
```

Top-bar buttons act as toggles: a second press returns to the dashboard.

### Category rail
Four screens share the same pattern: a **vertical rail on the left** selects a category, the
content scrolls on the right, and whatever belongs to no category stays in a persistent header
(profile name, master switch) or footer (Cancel / Save / Close).

| Screen | Tabs |
|---|---|
| Dashboard | Driving · Safety · Comfort |
| Profile editor | Driving · Safety · Comfort |
| Settings | Languages · Interface · Advanced · Info |
| Shortcuts | Buttons · Actions |

A tab whose page has no visible section left on the current firmware is **hidden** — better no tab
than a tab opening an empty page.

### Sizing
Values shared by the reworked screens, tuned for readability while driving: titles **20sp**,
section headers **13sp**, labels and buttons **16sp**, button height **52dp**, rail tabs **64dp**,
rail width **180dp**, card padding **14dp**.

### Color Palette

The app follows the light or dark theme. Light values live in `res/values/colors.xml`, dark ones in
`res/values-night/colors.xml` — **same token names on both sides**, which is the only rule to
follow when adding a colour.

| Token | Light | Dark | Usage |
|-------|-------|------|-------|
| `dash_bg` | `#F2F2F7` | `#0C0C0E` | App background |
| `dash_card` | `#FFFFFF` | `#141416` | Cards |
| `dash_section` | `#F2F2F7` | `#1C1C1F` | Inner sections |
| `dash_border` | `#D1D1D6` | `#2A2A2E` | Borders and dividers |
| `dash_btn` | `#E5E5EA` | `#222226` | Inactive button background |
| `dash_text_lo` | `#8E8E93` | `#52525B` | Section headers |
| `dash_accent` | `#0284C7` | `#38BDF8` | Active selection (blue) |
| `dash_accent_dim` | `#E0F2FE` | `#0C4A6E` | Active selection background |
| `dash_eco` | `#16A34A` | `#22C55E` | ECO mode (green) |
| `dash_warn` | `#D97706` | `#F59E0B` | Warning (amber) |
| `dash_danger` | `#E11D48` | `#F43F5E` | Delete / danger actions |
| `text_primary` | `#1C1C1E` | `#FFFFFF` | Primary text |
| `text_secondary` | `#6C6C70` | `#B0B0B0` | Secondary text |

Every `*_dim` colour is the background paired with its vivid counterpart: `dash_eco_dim`,
`dash_warn_dim` and `dash_danger_dim` follow the same principle as `dash_accent_dim`.

> **Naming pitfall:** `bg_dark` is `#FFFFFF` in the light theme. The name dates back to when the
> app only had a dark theme; it means the general background, not a dark colour.

---

## External API (KeyMapper, Tasker…)

Lets a third-party app trigger MG4Control functions (issue #79).

> **Disabled by default.** Turn it on in *Settings → Advanced settings → "External API"*, with an
> explicit confirmation. While disabled, every incoming command is refused and logged. Once
> enabled, **any installed app** can send these intents: they are protected by no permission,
> because KeyMapper and Tasker ship from the Play Store and can never hold a `signature` one.

### Direct actions — one intent action per command

No extras required. This is the form usable from **KeyMapper**, whose intent editor only offers
the type (*Broadcast receiver*) and the action string.

| Action | Effect |
|---|---|
| `com.mg4.control.action.ONE_PEDAL` | Toggle 1-pedal ↔ fallback regen level |
| `com.mg4.control.action.ENERGY_SAVING_TOGGLE` | Energy saving |
| `com.mg4.control.action.PROFILE_PICKER` | Show the on-screen profile picker |
| `com.mg4.control.action.OPEN_APP` | Open MG4Control |

These are **toggles**: each send flips the state, there is no "set to ON".

> **Deliberately out of the API.** `VEHICLE_POWER_OFF`, `ADAS_CYCLE`, `AEB_CYCLE`, `TSR_TOGGLE`,
> `OVERSPEED_ALARM`, `SPEED_LIMIT_TONE` and `SOUND_WARNING` are **not** exposed: they affect active
> safety or shut the vehicle down. The refusal also covers `EXECUTE` — removing them from the direct
> actions alone would have protected nothing. They remain available from the app and the steering
> wheel shortcuts.

**In KeyMapper**: add an action → *Intent* (version 2.3.0 minimum) → type **Broadcast receiver** →
paste the string into the *Action* field.

### `EXECUTE` — for Tasker, adb, scripts

`com.mg4.control.action.EXECUTE` with a string extra `action` holding one of the names above, plus
two commands the direct actions cannot cover:

- `APPLY_PROFILE` — requires a `profile` extra: the profile name, case-insensitive
- `OPEN_CUSTOM_APP` — opens the app configured in the shortcuts screen

```bash
adb shell am broadcast -a com.mg4.control.action.EXECUTE \
  --es action APPLY_PROFILE --es profile "Home commute"
```

### `SET` — write a value directly

`com.mg4.control.action.SET` with the `key` and `value` extras:

| `key` | accepted `value` |
|---|---|
| `drive_mode` | `ECO` `NORMAL` `SPORT` `SNOW` `CUSTOM` |
| `regen` | `OFF` `LOW` `MEDIUM` `HIGH` `ADAPTIVE` `ONE_PEDAL` |
| `seat_heat_left` | `0` to `3` |
| `seat_heat_right` | `0` to `3` |
| `steering_heat` | `0`/`1` or `false`/`true` |
| `profile` | profile name |
| `hvac_power` | `0`/`1` — climate on/off |
| `ac` | `0`/`1` — A/C compressor |
| `hvac_auto` | `0`/`1` — automatic mode |
| `hvac_temp` | °C, clamped to the vehicle's real bounds |
| `hvac_fan` | fan level, clamped to the real bounds |
| `hvac_recirc` | `INNER` `OUTSIDE` `AUTO` (or `0` `1` `2`) |
| `defrost_front` | `0`/`1` |
| `defrost_rear` | `0`/`1` |

The `hvac_*` and `defrost_*` keys are ignored when the firmware exposes no climate control.
Setpoint and fan are clamped to bounds **read from the vehicle**, which differ across firmwares.
These are hardware toggles that step one notch at a time: expect a few seconds before the final
state is reached.

```bash
adb shell am broadcast -a com.mg4.control.action.SET --es key drive_mode --es value SPORT
```

#### `NEXT` / `PREV` / `TOGGLE` — cycling without knowing the state

Instead of a setpoint, `value` accepts **`NEXT`** (next notch), **`PREV`** (previous notch) or
**`TOGGLE`** (an alias of `NEXT`, easier to read on a boolean). The new value is computed from the
state read on the vehicle, so a single steering-wheel button can mean "seat heat +1". The cycle
**wraps around**: past the maximum, the next notch returns to the minimum.

```bash
adb shell am broadcast -a com.mg4.control.action.SET --es key seat_heat_left --es value NEXT
adb shell am broadcast -a com.mg4.control.action.SET --es key ac --es value TOGGLE
```

Cyclable keys: `seat_heat_left`, `seat_heat_right`, `steering_heat`, `hvac_power`, `ac`,
`hvac_auto`, `hvac_temp`, `hvac_fan`, `hvac_recirc`, `defrost_front`, `defrost_rear`.

`drive_mode`, `regen` and `profile` are **deliberately excluded**: the drive-mode enum is not
filtered per firmware (cycling could select a mode the car does not have), regen availability
depends on the current state (no level at all in Snow mode, One Pedal only while Energy Saving is
on), and there is no notion of a "current profile".

When the current state cannot be read, the command is **refused without writing anything** rather
than assuming a starting point — a guessed origin would move the value down while you press to
move it up. Refusals are logged (`adb logcat -s MG4_API`).

### Reading state — ContentProvider

`content://com.mg4.control.state/state` (or `com.mg4.control.offline.state` for the offline
variant — the authority follows the applicationId). A **single**-row cursor:

`drive_mode`, `regen`, `seat_heat_left`, `seat_heat_right`, `steering_heat`, `speed_kmh`,
`outside_temp_c`, `tsr`, `energy_saving`, `aeb_enabled`, `firmware`, `profiles` (names separated
by `|`), `default_profile`.

An unreadable value is `null`, never `0` — a zero would be indistinguishable from "seat off" or
"vehicle stopped". Tasker can query a ContentProvider, KeyMapper cannot.

Unlike broadcasts, a provider knows its caller: every read is logged by package name, and the
`external_api_allowlist` preference (comma-separated packages, empty = all allowed) is actually
enforced.

### Security and diagnostics

The **speed lock** (*Settings → "Block driving settings above a given speed"*) also covers the API,
since it sits in the write primitives. Note that it is itself **disabled by default** — if you have
not enabled it, no speed limit applies to external commands. Comfort settings (seats, steering
wheel heating) are never affected.

Every command, accepted or refused, is traced under the **`MG4_API`** tag (visible via the
Diagnostic button). To test the app independently of KeyMapper:

```bash
adb shell am broadcast -a com.mg4.control.action.PROFILE_PICKER
```

Complete silence = stale APK or service not running. `REFUS … API externe désactivée` = the
Settings toggle was never confirmed.

---

## Build & Installation

You can download the latest version of MG4Control directly from the releases page: https://github.com/SliDeeN/MG4Control/releases
All you need is a USB drive and access to the AAOS settings to install the APK.


You can also compile the project yourself:

### Prerequisites
- Android Studio Hedgehog (2023.1) or later
- JDK 17+
- Android SDK API 34

### Debug Build

```bash
# Using Android Studio's bundled JDK
JAVA_HOME="/path/to/Android Studio/jbr" ./gradlew assembleDebug
```

Output APK location:
```
app/build/outputs/apk/debug/app-debug.apk
```

### Installing on the Vehicle

The application must be signed with the ROM's system key. On a development system:

```bash
adb push app-debug.apk /sdcard/
adb shell pm install -r --system /sdcard/app-debug.apk
```

> On a production ROM, the APK must be included in the system build or installed through an OEM-specific mechanism.

---

## Required Permissions

| Permission | Reason |
|-----------|--------|
| `FOREGROUND_SERVICE` | Persistent foreground service for auto-apply |
| `WAKE_LOCK` | Prevents sleep during settings application |
| `RECEIVE_BOOT_COMPLETED` | Auto-start on vehicle boot |
| `CAR_POWERTRAIN` | Drive mode and regeneration control |
| `CONTROL_CAR_CLIMATE` | Seat and steering wheel heating control |
| `CAR_VENDOR_EXTENSION` | SAIC proprietary extensions |
| `CAR_ENERGY` | Battery / powertrain information |

---

## Credits

Made with ❤ by **SliDeeN** and **Claude IA**

Basé sur l'application **DriveHub Dort** développée par **Merth4n** & **hotboy_ist**

Remerciements spéciaux à **confor1max** pour les tests approfondis du firmware SWI68 🙏

[![GitHub](https://img.shields.io/badge/GitHub-SliDeeN%2FMG4Control-181717?logo=github)](https://github.com/SliDeeN/MG4Control)

</details>
