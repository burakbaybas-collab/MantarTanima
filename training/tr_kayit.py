"""Türkçe adı olmayan türlerin Türkiye kayıt sayıları (GBIF + iNaturalist) -> tr_kayit.tsv"""
import csv, sys, time, requests
sys.path.insert(0, "training")
from fetch_gbif import resolve, get
S = requests.Session(); S.headers["User-Agent"] = "FungiLens/1.0"
rows = [r for r in csv.DictReader(open("app/src/main/assets/species.tsv", encoding="utf-8"), delimiter="\t")
        if not r["turkish_name"].strip()]
w = csv.writer(open("tr_kayit.tsv", "w", encoding="utf-8", newline=""), delimiter="\t")
w.writerow(["id", "taxon", "gbif_tr", "inat_tr"])
for r in rows:
    g = i = 0
    try:
        k, _ = resolve(r["taxon"])
        if k:
            g = get("occurrence/search", {"taxonKey": k, "country": "TR", "limit": 0}).get("count", 0)
    except Exception as e:
        print("gbif", r["taxon"], e)
    for _ in range(3):
        try:
            j = S.get("https://api.inaturalist.org/v1/observations", params={"taxon_name": r["taxon"], "place_id": 7183,
                      "per_page": 0}, timeout=30).json()
            i = j.get("total_results", 0); break
        except Exception:
            time.sleep(5)
    w.writerow([r["id"], r["taxon"], g, i]); print(r["taxon"], g, i, flush=True); time.sleep(0.5)
