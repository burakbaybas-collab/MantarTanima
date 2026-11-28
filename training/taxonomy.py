"""Her tür için GBIF'ten üst sınıflandırmayı (alem, bölüm, sınıf, takım, aile) alır.

Çıktı: app/src/main/assets/taxonomy.tsv  (id, kingdom, phylum, class, order, family)
"""
import csv
import os
import sys
from concurrent.futures import ThreadPoolExecutor

sys.path.insert(0, os.path.dirname(__file__))
from fetch_gbif import OVERRIDES, get  # noqa: E402

KEYS = ["kingdom", "phylum", "class", "order", "family"]


def lookup(name):
    tries = []
    for q in OVERRIDES.get(name, [name]):
        tries += [{"name": q, "strict": "true"}, {"name": q}]
    for p in tries:
        m = get("species/match", p)
        if m.get("matchType") in ("EXACT", "FUZZY") and m.get("phylum"):
            return {k: m.get(k, "") for k in KEYS}
    # Yalnız cins adıyla (tür eşleşmezse aile ve üstü yine bulunur)
    m = get("species/match", {"name": name.split()[0], "rank": "GENUS"})
    return {k: m.get(k, "") for k in KEYS} if m.get("phylum") else None


def main():
    with open("app/src/main/assets/species.tsv", encoding="utf-8") as f:
        species = [(r["id"], r["taxon"]) for r in csv.DictReader(f, delimiter="\t")]
    with ThreadPoolExecutor(8) as ex:
        results = list(ex.map(lambda s: lookup(s[1]), species))
    missing = []
    with open("app/src/main/assets/taxonomy.tsv", "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f, delimiter="\t")
        w.writerow(["id"] + KEYS)
        for (sid, name), r in zip(species, results):
            if r:
                w.writerow([sid] + [r[k] for k in KEYS])
            else:
                missing.append(name)
    print(f"{len(species) - len(missing)}/{len(species)} tür bulundu")
    for (sid, name), r in zip(species, results):
        if r and not all(r.values()):
            print("Eksik alan:", name, r)
    if missing:
        print("Bulunamayan:", ", ".join(missing))
    from collections import Counter
    print("Alemler:", Counter(r["kingdom"] for r in results if r))
    print("Bölümler:", Counter(r["phylum"] for r in results if r))


if __name__ == "__main__":
    main()
