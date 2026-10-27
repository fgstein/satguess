#!/usr/bin/env python3
"""Génère la liste des lieux SatGuess depuis Wikidata.

Usage :
  python3 build_locations.py --check-classes   # affiche le libellé de chaque classe Wikidata utilisée
  python3 build_locations.py                   # interroge Wikidata, filtre, écrit le JSON
  python3 build_locations.py --stats           # idem + statistiques détaillées

Les réponses brutes sont mises en cache dans ./cache (supprimer le dossier pour
forcer une nouvelle interrogation).
"""

import argparse
import hashlib
import json
import math
import re
import sys
import time
from collections import Counter, defaultdict
from pathlib import Path

import requests

from categories import CATEGORIES, COORD_DEFAULT

ENDPOINT = "https://query.wikidata.org/sparql"
USER_AGENT = "SatGuessLocationBuilder/0.1 (https://github.com/fgstein/satguess)"
HERE = Path(__file__).resolve().parent
CACHE_DIR = HERE / "cache"
DEFAULT_OUTPUT = HERE.parent.parent / "data" / "locations.json"

DEDUP_KM = 5.0
MAX_COUNTRY_SHARE = 0.05

POINT_RE = re.compile(r"^Point\(\s*(-?[\d.eE+-]+)\s+(-?[\d.eE+-]+)\s*\)$")


def build_query(source):
    coord = source.get("coord", COORD_DEFAULT)
    return f"""
SELECT ?item (SAMPLE(?coord) AS ?c) (MAX(?sl) AS ?sitelinks) (SAMPLE(?cc) AS ?country)
       (SAMPLE(?fr) AS ?name_fr) (SAMPLE(?en) AS ?name_en)
       (SAMPLE(?dfr) AS ?desc_fr) (SAMPLE(?den) AS ?desc_en) WHERE {{
  {source["where"]}
  ?item wikibase:sitelinks ?sl .
  FILTER(?sl >= {source["min_sitelinks"]})
  {coord}
  OPTIONAL {{ ?item wdt:P17 ?ctry . ?ctry wdt:P297 ?cc . }}
  OPTIONAL {{ ?item rdfs:label ?fr . FILTER(LANG(?fr) = "fr") }}
  OPTIONAL {{ ?item rdfs:label ?en . FILTER(LANG(?en) = "en") }}
  OPTIONAL {{ ?item schema:description ?dfr . FILTER(LANG(?dfr) = "fr") }}
  OPTIONAL {{ ?item schema:description ?den . FILTER(LANG(?den) = "en") }}
}} GROUP BY ?item
"""


def run_query(query, retries=5):
    CACHE_DIR.mkdir(exist_ok=True)
    cache_file = CACHE_DIR / (hashlib.sha1(query.encode()).hexdigest() + ".json")
    if cache_file.exists():
        return json.loads(cache_file.read_text())

    delay = 5
    for attempt in range(retries):
        resp = requests.get(
            ENDPOINT,
            params={"query": query, "format": "json"},
            headers={"User-Agent": USER_AGENT, "Accept": "application/sparql-results+json"},
            timeout=90,
        )
        if resp.status_code == 200:
            rows = resp.json()["results"]["bindings"]
            cache_file.write_text(json.dumps(rows))
            time.sleep(1)  # politesse envers le service public
            return rows
        if resp.status_code in (429, 500, 502, 503, 504) and attempt < retries - 1:
            wait = int(resp.headers.get("Retry-After", delay))
            print(f"    HTTP {resp.status_code}, nouvel essai dans {wait}s", file=sys.stderr)
            time.sleep(wait)
            delay *= 2
            continue
        raise RuntimeError(f"Requête en échec (HTTP {resp.status_code}) :\n{resp.text[:500]}\n{query}")


def parse_point(wkt):
    """Renvoie (lat, lon) pour une coordonnée terrestre, sinon None.

    Les coordonnées sur d'autres astres sont préfixées par l'URI du globe
    (ex. « <http://www.wikidata.org/entity/Q405> Point(...) ») et sont rejetées.
    """
    m = POINT_RE.match(wkt.strip())
    if not m:
        return None
    lon, lat = float(m.group(1)), float(m.group(2))
    if not (-90 <= lat <= 90 and -180 <= lon <= 180):
        return None
    return lat, lon


def haversine_km(a, b):
    lat1, lon1 = map(math.radians, a)
    lat2, lon2 = map(math.radians, b)
    h = math.sin((lat2 - lat1) / 2) ** 2 + math.cos(lat1) * math.cos(lat2) * math.sin((lon2 - lon1) / 2) ** 2
    return 2 * 6371.0088 * math.asin(math.sqrt(h))


def fetch_candidates():
    """Interroge chaque source ; un objet présent dans plusieurs catégories garde la première."""
    by_id = {}
    for cat in CATEGORIES:
        count = 0
        for source in cat["sources"]:
            rows = run_query(build_query(source))
            for row in rows:
                qid = row["item"]["value"].rsplit("/", 1)[-1]
                point = parse_point(row["c"]["value"])
                if point is None or qid in by_id:
                    continue
                name_en = row.get("name_en", {}).get("value")
                name_fr = row.get("name_fr", {}).get("value") or name_en
                if not name_en and not name_fr:
                    continue
                by_id[qid] = {
                    "id": qid,
                    "lat": round(point[0], 5),
                    "lon": round(point[1], 5),
                    "cat": cat["code"],
                    "name_fr": name_fr,
                    "name_en": name_en or name_fr,
                    "desc_fr": row.get("desc_fr", {}).get("value"),
                    "desc_en": row.get("desc_en", {}).get("value"),
                    "country": row.get("country", {}).get("value"),
                    "sitelinks": int(row["sitelinks"]["value"]),
                    "view_km": cat["view_km"],
                }
                count += 1
        print(f"  {cat['code']} {cat['name']:<35} {count:>5} candidats")
    return list(by_id.values())


def dedupe(locations):
    """Supprime les lieux à moins de DEDUP_KM d'un lieu plus connu."""
    cell = 0.1  # degrés ; DEDUP_KM reste bien inférieur à la taille d'une cellule
    grid = defaultdict(list)
    kept = []
    for loc in sorted(locations, key=lambda l: -l["sitelinks"]):
        p = (loc["lat"], loc["lon"])
        gx, gy = int(math.floor(p[0] / cell)), int(math.floor(p[1] / cell))
        near = (
            other
            for dx in (-1, 0, 1)
            for dy in (-1, 0, 1)
            for other in grid[(gx + dx, gy + dy)]
        )
        if any(haversine_km(p, o) < DEDUP_KM for o in near):
            continue
        grid[(gx, gy)].append(p)
        kept.append(loc)
    return kept


def cap_countries(locations):
    """Limite chaque pays à MAX_COUNTRY_SHARE du total, en gardant les plus connus."""
    limit = max(1, int(len(locations) * MAX_COUNTRY_SHARE))
    per_country = Counter()
    kept = []
    for loc in sorted(locations, key=lambda l: -l["sitelinks"]):
        country = loc["country"]
        if country is not None:
            if per_country[country] >= limit:
                continue
            per_country[country] += 1
        kept.append(loc)
    return kept


def check_classes():
    qids = sorted({q for cat in CATEGORIES for s in cat["sources"]
                   for q in re.findall(r"wd:(Q\d+)", s["where"] + s.get("coord", ""))})
    values = " ".join(f"wd:{q}" for q in qids)
    rows = run_query(f"""
SELECT ?q ?en ?fr WHERE {{
  VALUES ?q {{ {values} }}
  OPTIONAL {{ ?q rdfs:label ?en . FILTER(LANG(?en) = "en") }}
  OPTIONAL {{ ?q rdfs:label ?fr . FILTER(LANG(?fr) = "fr") }}
}}""")
    labels = {r["q"]["value"].rsplit("/", 1)[-1]: (r.get("en", {}).get("value"), r.get("fr", {}).get("value"))
              for r in rows}
    for q in qids:
        en, fr = labels.get(q, (None, None))
        print(f"{q:<10} {en or '?':<35} {fr or '?'}")


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--check-classes", action="store_true", help="affiche les libellés des classes et quitte")
    parser.add_argument("--stats", action="store_true", help="affiche des statistiques détaillées")
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT)
    parser.add_argument("--version", type=int, help="version du fichier (défaut : version existante + 1)")
    args = parser.parse_args()

    if args.check_classes:
        check_classes()
        return

    print("Interrogation de Wikidata…")
    candidates = fetch_candidates()
    print(f"Candidats : {len(candidates)}")
    deduped = dedupe(candidates)
    print(f"Après dédoublonnage ({DEDUP_KM} km) : {len(deduped)}")
    final = cap_countries(deduped)
    print(f"Après plafond par pays ({MAX_COUNTRY_SHARE:.0%}) : {len(final)}")

    version = args.version
    if version is None:
        version = 1
        if args.output.exists():
            version = json.loads(args.output.read_text()).get("version", 0) + 1
    if not 0 <= version <= 127:
        sys.exit("La version doit tenir sur 7 bits (0–127), cf. specs §16.")

    final.sort(key=lambda l: l["id"])
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps({"version": version, "locations": final},
                                      ensure_ascii=False, separators=(",", ":")))
    print(f"Écrit {args.output} (version {version}, {len(final)} lieux)")

    if args.stats:
        print("\nPar catégorie :")
        for code, n in sorted(Counter(l["cat"] for l in final).items()):
            print(f"  {code} {n:>5}")
        print("\nTop 15 pays :")
        for country, n in Counter(l["country"] for l in final).most_common(15):
            print(f"  {country or '—':<4} {n:>5}")


if __name__ == "__main__":
    main()
