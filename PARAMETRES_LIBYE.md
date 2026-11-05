# Paramètres géodésiques Libye intégrés à SubTopo-Geo

Source : base EPSG (consultée via epsg.io, novembre 2026). À valider par le Département des Levés de Libye / votre organisme avant tout travail contractuel.

## Transformations vers WGS84 (translations géocentriques, ellipsoïde International 1924 / Hayford)
| Datum | dX (m) | dY (m) | dZ (m) | Précision | Réf. EPSG |
|---|---|---|---|---|---|
| ELD79 (European Libyan Datum 1979) | -115.8543 | -99.0583 | -152.4616 | 5 m, Libye onshore, 29 stations, mai 2006, source : Survey Department of Libya | 15909 |
| LGD2006 (Libyan Geodetic Datum 2006) | -208.4058 | -109.8777 | -2.5764 | 0,1 m, Libye onshore et offshore | 15908 (valeurs lues dans les définitions EPSG:3177 / 3201) |
| ED50 générique (Europe occidentale) | -87 | -98 | -121 | 3 à 8 m en Europe, **pas pour la Libye** | 1133 |

Aucune transformation ED50 -> WGS84 propre à la Libye n'a été trouvée dans EPSG. EPSG liste aussi des transformations locales ELD79 pour certains champs pétroliers (Mabruk, Murzuq, Amal, Sirte NC192, Cyrénaïque) : non intégrées.

## Projections
| Système | Méridien central | Échelle k0 | Faux Est (m) | Faux Nord | Vérifié sur |
|---|---|---|---|---|---|
| LGD2006 / Libya TM (EPSG:3177) | 17° E | 0,9965 | 1 000 000 | 0 | EPSG:3177 |
| LGD2006 / Libya TM zone N | 2N - 1 | 0,99995 | 200 000 | 0 | zones 7 (13° E) et 13 (25° E) |
| ELD79 / Libya zone N | 2N - 1 | 0,9999 | 200 000 | 0 | zone 9 (17° E) |
| LGD2006 ou ELD79 / UTM 32N, 33N, 34N | 9, 15, 21° E | 0,9996 | 500 000 | 0 | via « UTM · datum » + préréglage datum |

Les zones 5 à 13 non citées ci-dessus suivent la règle « méridien central = 2N - 1 » : à confirmer. Attention : k0 diffère entre ELD79 (0,9999) et LGD2006 (0,99995).
