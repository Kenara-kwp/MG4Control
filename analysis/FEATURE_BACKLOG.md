# T6 — Backlog d'idées de fonctionnalités

Classé par risque. Règle : **read-only d'abord**, toute écriture véhicule passe la revue sécurité (T4)
et est désactivée/gardée dans la variante offline si non essentielle.

## A. Read-only / faible risque (prioritaires)
| Idée | Description | Dépend de |
|---|---|---|
| **Tuile SOH** | Santé batterie + SOC dans le Dashboard | T5 (signal trouvé) |
| **Dashboard batterie** | SOH, temp. pack, deltas cellules, nb cycles (si exposés) | sonde vendor (T5) |
| **Journal de charge** | Log des sessions de charge + export CSV local | stockage local |
| **Stats trajet / efficacité** | Wh/km, énergie récupérée (regen), conso moyenne | lecture CPM |
| **Backup/restore profils** | Export/import des `DrivingProfile` en JSON | Gson déjà présent — **idéal offline** |
| **Auto-bascule de profil** | Profil selon heure / conducteur (clé BT déjà gérée) | `bluetooth/` existant |
| **Audit-check intégrité (offline)** | L'APK offline vérifie sa propre signature au boot | variante offline (T3) |

## B. Écriture véhicule / risque modéré (gate sécurité obligatoire)
| Idée | Description | Garde-fou |
|---|---|---|
| **Contrôle audio** (port depuis 2.7.0) | Bose / loudness / fader / balance / 3D / tone | feature déjà écrite dans 2.7.0 — porter avec revue T4 |
| **Pré-conditionnement** | Chauffer/refroidir l'habitacle avant départ (planifié) | écriture climatisation → gate T4 |
| **Macros de profil** | Enchaîner plusieurs réglages en un tap | confirmer chaque write |

## C. Plateforme / qualité (hors-véhicule)
| Idée | Description |
|---|---|
| **Tests unitaires** `ProfileManager`/`FirmwareInfo` | aucun test actuellement → filet pour les refactors |
| **Détekt + ktlint en CI** | qualité Kotlin, complète la CI sécurité (T4) |
| **Renovate/Dependabot** | mises à jour de dépendances surveillées |
| **Page de release auto-générée** | changelog depuis les commits |

## Recommandation d'ordre
1. **T5 → Tuile SOH** (si signal) — valeur user immédiate, read-only.
2. **Port audio depuis 2.7.0** — feature déjà faite, juste à réintégrer proprement + gate T4.
3. **Backup/restore profils JSON** — petit, sûr, parfait pour la variante offline.
4. Le reste selon retours utilisateurs.
