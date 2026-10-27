# SatGuess

Jeu Android inspiré d'EarthGuessr : une image satellite s'affiche, placez-la sur la carte du monde. 5 manches, 5 000 points maximum par manche.

- Spécifications : [`docs/SPECS.md`](docs/SPECS.md)
- Liste des lieux : `data/locations.json`, générée par [`tools/locations`](tools/locations/README.md)

## Compiler

Prérequis : JDK 17+ et le SDK Android (plateforme 35). Indiquer le SDK dans `local.properties` :

```
sdk.dir=/chemin/vers/android-sdk
# Facultatif : imagerie Mapbox Satellite (sinon Sentinel-2 cloudless d'EOX, sans clé)
MAPBOX_TOKEN=pk....
```

```bash
./gradlew testDebugUnitTest   # tests unitaires (seed, tirage, score)
./gradlew assembleDebug       # APK : app/build/outputs/apk/debug/app-debug.apk
```
