# Génération de la liste des lieux

Interroge Wikidata pour chaque catégorie définie dans `categories.py`, filtre (célébrité, doublons < 5 km, max 5 % par pays) et écrit `data/locations.json`.

```bash
pip install requests
cd tools/locations
python3 build_locations.py --check-classes   # vérifier que chaque QID correspond bien à la classe voulue
python3 build_locations.py --stats
```

Les seuils `min_sitelinks` de `categories.py` règlent le nombre de lieux par catégorie.
Les réponses brutes sont mises en cache dans `cache/` : supprimer ce dossier pour repartir de zéro.
