"""Yeni tür adayları: GBIF'te Avrupa'da fotoğraflı kaydı çok olan ve/veya Türkiye kaydı olan,
uygulamada henüz bulunmayan makromantar türleri. Çıktı: adaylar.csv"""
import csv
import sys
import time
from concurrent.futures import ThreadPoolExecutor

import requests

API = "https://api.gbif.org/v1"
FUNGI = 5
CLASSES = {"Agaricomycetes", "Pezizomycetes", "Dacrymycetes", "Tremellomycetes", "Leotiomycetes", "Sordariomycetes"}
S = requests.Session()


def get(path, params=None):
    for i in range(6):
        try:
            r = S.get(f"{API}/{path}", params=params, timeout=90)
            if r.status_code == 200:
                return r.json()
        except requests.RequestException:
            pass
        time.sleep(3 * (i + 1))
    raise RuntimeError(path)


def facet(params, limit):
    p = dict(params, taxonKey=FUNGI, facet="speciesKey", limit=0, **{"speciesKey.facetLimit": limit})
    return {int(c["name"]): c["count"] for c in get("occurrence/search", p)["facets"][0]["counts"]}


def main():
    with open("app/src/main/assets/species.tsv", encoding="utf-8") as f:
        have = {r["taxon"].lower() for r in csv.DictReader(f, delimiter="\t")}
    eu_img = facet({"continent": "EUROPE", "mediaType": "StillImage"}, 5000)
    all_img = facet({"mediaType": "StillImage"}, 8000)
    tr = facet({"country": "TR"}, 4000)
    keys = set(list(eu_img)[:3500]) | set(tr)
    print(len(eu_img), len(tr), len(keys), flush=True)

    def info(k):
        try:
            return _info(k)
        except Exception as e:
            print('hata', k, e, flush=True)
            return None

    def _info(k):
        s = get(f"species/{k}")
        if s.get("class") not in CLASSES or s.get("taxonomicStatus") not in ("ACCEPTED", None):
            return None
        name = s.get("canonicalName") or s.get("species")
        if not name or name.lower() in have or len(name.split()) != 2:
            return None
        m = get("occurrence/search", {"taxonKey": k, "continent": "EUROPE", "facet": "month", "limit": 0,
                                       "month.facetLimit": 12})
        months = {int(c["name"]): c["count"] for c in m["facets"][0]["counts"]} if m.get("facets") else {}
        tot = sum(months.values()) or 1
        spring = sum(months.get(i, 0) for i in (3, 4, 5)) / tot
        syn = get(f"species/{k}/synonyms", {"limit": 50}).get("results", [])
        syn_hit = [x["canonicalName"] for x in syn if (x.get("canonicalName") or "").lower() in have]
        return {"gbif_key": k, "taxon": name, "family": s.get("family", ""), "order": s.get("order", ""),
                "class": s.get("class", ""), "tr_kayit": tr.get(k, 0), "avrupa_fotolu": eu_img.get(k, 0),
                "dunya_fotolu": all_img.get(k, 0), "ilkbahar_orani": round(spring, 2),
                "listedeki_es_anlamli": ";".join(syn_hit)}

    with ThreadPoolExecutor(8) as ex:
        rows = [r for r in ex.map(info, sorted(keys)) if r]
    rows.sort(key=lambda r: (-r["tr_kayit"], -r["avrupa_fotolu"]))
    with open("adaylar.csv", "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=list(rows[0]))
        w.writeheader()
        w.writerows(rows)
    print(len(rows), "aday", flush=True)


if __name__ == "__main__":
    sys.exit(main())
