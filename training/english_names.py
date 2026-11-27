"""iNaturalist'ten türlerin İngilizce yaygın adlarını çeker -> english_names.tsv (id, taxon, english_name)."""
import csv, time, requests
S = requests.Session(); S.headers["User-Agent"] = "FungiLens/1.0"
rows = list(csv.DictReader(open("app/src/main/assets/species.tsv", encoding="utf-8"), delimiter="\t"))
out = open("english_names.tsv", "w", encoding="utf-8", newline="")
w = csv.writer(out, delimiter="\t"); w.writerow(["id", "taxon", "english_name"])
for i, r in enumerate(rows):
    name = ""
    for _ in range(4):
        try:
            j = S.get("https://api.inaturalist.org/v1/taxa", params={"q": r["taxon"], "rank": "species,subspecies,variety",
                      "locale": "en", "per_page": 5}, timeout=30).json()
            for t in j.get("results", []):
                if t.get("name", "").lower() == r["taxon"].lower() or r["taxon"].lower() in [s.lower() for s in t.get("names", [])]:
                    name = t.get("preferred_common_name") or ""
                    break
            else:
                t = (j.get("results") or [{}])[0]
                if t.get("matched_term", "").lower() == r["taxon"].lower():
                    name = t.get("preferred_common_name") or ""
            break
        except Exception:
            time.sleep(5)
    w.writerow([r["id"], r["taxon"], name]); time.sleep(0.6)
    if i % 100 == 0: print(i, r["taxon"], name, flush=True)
out.close()
