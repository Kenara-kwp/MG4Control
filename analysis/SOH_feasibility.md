# T5 — Faisabilité : lecture du SOH (State of Health) batterie MG4

> **Contrainte verrouillée : LECTURE SEULE. Aucune écriture véhicule.**
> Statut : spike de faisabilité — pas une garantie de feature.

## Définition
SOH = santé batterie = capacité utile actuelle ÷ capacité nominale d'origine, en %.
Aucun signal `SOH` n'existe aujourd'hui dans le code (ni MG4Control ni 2.7.0) ni dans
`MG4Control_SWI_Reference.md`. Seul signal énergie présent : `PROP_ENERGY_SAVING = 0x5030007`
(mode éco, sans rapport avec la santé).

## Couches de lecture disponibles (existantes dans MG4Hardware)
- **CPM** — `CarPropertyManager` (AAOS standard, async via ServiceConnection). Permission `CAR_ENERGY` déjà détenue.
- **VPM / VSM** — managers vendor SAIC, par firmware (SWI133 = VPM, SWI68/165 = VSM, SWI69/131/132 = CarVehicleSettingClient).
- **Katman5** — `VehicleConditionManager` (SWI133/68/165) / `ICarGeneralService` (SWI69/131/132) → lecture de *conditions* véhicule. **Piste la plus probable pour des signaux batterie vendor.**

## Pistes candidates, classées par effort/risque

### Piste A — Propriétés AAOS standard (à tenter en premier, risque ~nul)
`CarPropertyManager` expose des `VehiclePropertyIds` batterie EV (permission `Car.PERMISSION_ENERGY`, déjà couverte par `CAR_ENERGY`) :
| Signal | VehiclePropertyId (symbolique) | Usage |
|---|---|---|
| Capacité nominale | `INFO_EV_BATTERY_CAPACITY` | dénominateur SOH (kWh d'origine) |
| Capacité utile actuelle | `EV_CURRENT_BATTERY_CAPACITY` | numérateur SOH (kWh réels) |
| Niveau charge | `EV_BATTERY_LEVEL` | SOC instantané (≠ SOH) |
> **SOH ≈ `EV_CURRENT_BATTERY_CAPACITY` / `INFO_EV_BATTERY_CAPACITY` × 100.**
> Valeurs int exactes à confirmer depuis `android.car.VehiclePropertyIds` sur l'appareil.
> Risque : MG/SAIC peut ne pas implémenter `EV_CURRENT_BATTERY_CAPACITY` (souvent absent) → SOH non dérivable par cette voie seule.

### Piste B — Propriété vendor SOH directe (reverse, effort moyen)
Sonder l'espace vendor (`CAR_VENDOR_EXTENSION`, IDs type `0x2140xxxx` / `0x30xxxxx` déjà utilisés)
et `VehicleConditionManager` pour un champ "battery health" / "SOH" exposé par SAIC.
Méthode : énumérer en lecture les propriétés vendor et logger valeur+plage, corréler avec le SOH
affiché par un outil OBD/diag de référence.

### Piste C — Estimation logicielle (fallback)
Si ni A ni B : estimer le SOH côté app à partir de l'historique capacité pleine charge vs nominale
(nécessite plusieurs cycles → imprécis, hors scope d'un spike).

## Plan de probe (read-only)
1. Ajouter une commande **debug** dans `ConsoleFragment` (déjà présent) : dump des props candidates
   via `getIntPropertyCPM` / `getFloatProperty` — **aucun setter**.
2. Lire Piste A (`INFO_EV_BATTERY_CAPACITY`, `EV_CURRENT_BATTERY_CAPACITY`, `EV_BATTERY_LEVEL`).
3. Si `EV_CURRENT_BATTERY_CAPACITY` absent/0 → énumérer Piste B via `VehicleConditionManager`.
4. Comparer à une valeur SOH de référence (outil OBD) pour valider l'unité/l'échelle.

## Conclusion
- **Faisable en lecture seule à tester** ; coût d'investigation faible (réutilise CPM + Console existants).
- **Confiance moyenne** : dépend de l'implémentation MG de `EV_CURRENT_BATTERY_CAPACITY` (Piste A) ou
  de l'exposition d'un champ santé par `VehicleConditionManager` (Piste B).
- **Garde-fou** : tout reste en lecture ; aucune écriture véhicule, conforme à la décision T5.
- **Livrable feature** (si signal trouvé) : tuile SOH read-only dans le Dashboard (cf. T6).
