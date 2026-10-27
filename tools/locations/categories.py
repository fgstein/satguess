"""Catégories de lieux et requêtes Wikidata associées.

Chaque catégorie regroupe une ou plusieurs sources. Une source est un fragment
SPARQL qui lie ?item, plus un seuil minimal de sitelinks (nombre d'articles
Wikipédia, utilisé comme mesure de célébrité).

Variables disponibles dans les fragments :
  ?item   l'objet Wikidata
  ?coord  doit être lié par le fragment si COORD_DEFAULT n'est pas utilisé

Les seuils sont réglés pour viser plusieurs milliers de lieux au total ;
ajuster après un premier passage (`build_locations.py --stats`).
"""

# Coordonnée par défaut : la propriété P625 de l'objet.
COORD_DEFAULT = "?item wdt:P625 ?coord ."


def instance_of(*qids, subclasses=True):
    """Fragment « ?item est une instance de l'une de ces classes »."""
    values = " ".join(f"wd:{q}" for q in qids)
    if not subclasses:
        return f"VALUES ?cls {{ {values} }} ?item wdt:P31 ?cls ."
    # Sous-classes d'abord, puis instances : avec l'optimiseur désactivé
    # (cf. build_query), cet ordre évite les dépassements de délai.
    return f"VALUES ?root {{ {values} }} ?cls wdt:P279* ?root . ?item wdt:P31 ?cls ."


CATEGORIES = [
    {
        "code": "A",
        "name": "Grandes villes",
        "view_km": 15,
        "sources": [
            # city, big city, million city, metropolis, capital
            {"where": instance_of("Q515", "Q1549591", "Q1637706", "Q200250", "Q5119", subclasses=False),
             "min_sitelinks": 40},
        ],
    },
    {
        "code": "B",
        "name": "Aéroports",
        "view_km": 5,
        "sources": [
            # airport, avec un code IATA (P238) pour écarter les petits terrains
            {"where": instance_of("Q1248784") + " ?item wdt:P238 ?iata .",
             "min_sitelinks": 15},
        ],
    },
    {
        "code": "C",
        "name": "Bases militaires",
        "view_km": 5,
        "sources": [
            # military base, air base, naval base
            {"where": instance_of("Q245016", "Q695850", "Q1324633"),
             "min_sitelinks": 8},
        ],
    },
    {
        "code": "D",
        "name": "Ports et canaux",
        "view_km": 8,
        "sources": [
            {"where": instance_of("Q44782"), "min_sitelinks": 10},  # port
            {"where": instance_of("Q12284"), "min_sitelinks": 20},  # canal
        ],
    },
    {
        "code": "E",
        "name": "Sites industriels / énergie",
        "view_km": 5,
        "sources": [
            {"where": instance_of("Q159719"), "min_sitelinks": 10},  # power station
            {"where": instance_of("Q12323"), "min_sitelinks": 15},   # dam
            {"where": instance_of("Q12353044"), "min_sitelinks": 5}, # oil refinery
        ],
    },
    {
        "code": "F",
        "name": "Mines",
        "view_km": 5,
        "sources": [
            {"where": instance_of("Q820477"), "min_sitelinks": 5},  # mine
        ],
    },
    {
        "code": "G",
        "name": "Bases de lancement spatial",
        "view_km": 8,
        "sources": [
            {"where": instance_of("Q194188"), "min_sitelinks": 3},  # spaceport
        ],
    },
    {
        "code": "H",
        "name": "Fleuves : deltas et embouchures",
        "view_km": 30,
        "sources": [
            {"where": instance_of("Q43197"), "min_sitelinks": 5},  # river delta
            # Fleuves connus : on prend la coordonnée qualifiée « embouchure » (P518 = river mouth)
            {"where": instance_of("Q4022"),
             "coord": "?item p:P625 ?coordSt . ?coordSt ps:P625 ?coord ; pq:P518 wd:Q1233637 .",
             "min_sitelinks": 40},
        ],
    },
    {
        "code": "I",
        "name": "Volcans, montagnes, cratères",
        "view_km": 15,
        "sources": [
            {"where": instance_of("Q8072"), "min_sitelinks": 15},   # volcano
            {"where": instance_of("Q8502"), "min_sitelinks": 40},   # mountain
            {"where": instance_of("Q55818"), "min_sitelinks": 10},  # impact crater
        ],
    },
    {
        "code": "J",
        "name": "Îles et atolls",
        "view_km": 10,
        "sources": [
            {"where": instance_of("Q23442"), "min_sitelinks": 30},  # island
            {"where": instance_of("Q42523"), "min_sitelinks": 8},   # atoll
        ],
    },
    {
        "code": "K",
        "name": "Patrimoine mondial UNESCO",
        "view_km": 3,
        "sources": [
            # heritage designation = World Heritage Site
            {"where": "?item wdt:P1435 wd:Q9259 .", "min_sitelinks": 15},
        ],
    },
    {
        "code": "L",
        "name": "Circuits et grands stades",
        "view_km": 3,
        "sources": [
            {"where": instance_of("Q2338524"), "min_sitelinks": 10},  # motorsport racing track
            # stade de plus de 40 000 places (P1083 = capacité)
            {"where": instance_of("Q483110") + " ?item wdt:P1083 ?cap . FILTER(?cap >= 40000)",
             "min_sitelinks": 20},
        ],
    },
    {
        "code": "M",
        "name": "Paysages remarquables",
        "view_km": 20,
        "sources": [
            {"where": instance_of("Q150784"), "min_sitelinks": 10},  # canyon
            {"where": instance_of("Q45776"), "min_sitelinks": 10},   # fjord
            {"where": instance_of("Q23397"), "min_sitelinks": 40},   # lake
            {"where": instance_of("Q35666"), "min_sitelinks": 15},   # glacier
            {"where": instance_of("Q935277", "Q3497768"), "min_sitelinks": 5},  # salt pan, dry lake
        ],
    },
]
