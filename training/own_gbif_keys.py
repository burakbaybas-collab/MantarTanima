"""Uygulamadaki türlerin GBIF kabul edilen anahtarları (aday listesinden çıkarmak için)."""
import csv, sys
from concurrent.futures import ThreadPoolExecutor
sys.path.insert(0, "training")
from fetch_gbif import resolve

with open("app/src/main/assets/species.tsv", encoding="utf-8") as f:
    sp = list(csv.DictReader(f, delimiter="\t"))
extra = ["Inosperma erubescens", "Inocybe erubescens", "Morchella deliciosa", "Morchella vulgaris",
         "Morchella steppicola", "Morchella semilibera", "Morchella tridentina", "Morchella rufobrunnea",
         "Morchella eximia", "Morchella conica", "Morchella eohespera", "Morchella dunalii", "Morchella anatolica"]
with ThreadPoolExecutor(8) as ex:
    keys = list(ex.map(lambda s: resolve(s)[0], [s["taxon"] for s in sp]))
    ek = list(ex.map(lambda n: resolve(n), extra))
with open("own_keys.csv", "w", newline="", encoding="utf-8") as f:
    w = csv.writer(f); w.writerow(["id", "taxon", "gbif_key"])
    for s, k in zip(sp, keys): w.writerow([s["id"], s["taxon"], k or ""])
    for n, (k, acc) in zip(extra, ek): w.writerow(["EXTRA", f"{n}|{acc}", k or ""])
