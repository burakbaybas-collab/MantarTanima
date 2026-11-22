"""Türkiye için aday tür listesi çıkarır (GitHub Actions'ta çalışır; internet gerekir).

Kaynaklar:
  * FungiTastic meta verisi  -> tür başına fotoğraf sayısı (uzman doğrulamalı, Kuzey Avrupa)
  * GBIF                     -> Türkiye'deki mantar kayıtları (country=TR)
  * iNaturalist              -> Türkiye'deki araştırma düzeyi gözlemler, Avrupa'daki fotoğraflı gözlemler

Çıktı: training/aday_turler.csv
"""
import csv
import io
import os
import sys
import time
import zipfile
from collections import Counter, defaultdict

import requests

UA = {"User-Agent": "MantarTanima/0.2 (tur-secimi; github.com/burakbaybas-collab/MantarTanima)"}
FT_META = "https://cmp.felk.cvut.cz/datagrid/FungiTastic/shared/download/metadata.zip"
INAT = "https://api.inaturalist.org/v1"
GBIF = "https://api.gbif.org/v1"
# Makromantar sınıfları (likenler ve mikrofunguslar hariç)
MACRO_CLASSES = {"Agaricomycetes", "Pezizomycetes", "Dacrymycetes", "Tremellomycetes", "Leotiomycetes",
                 "Sordariomycetes", "Geoglossomycetes"}
LICHEN_ORDERS = {"Lecanorales", "Peltigerales", "Teloschistales", "Caliciales", "Ostropales", "Pertusariales",
                 "Lichinales", "Verrucariales", "Gyalectales", "Baeomycetales", "Umbilicariales", "Acarosporales",
                 "Candelariales", "Rhizocarpales", "Arthoniales", "Lecideales"}


def get(url, params=None, tries=6):
    for i in range(tries):
        try:
            r = requests.get(url, params=params, headers=UA, timeout=120)
            if r.status_code == 200:
                return r
            print("HTTP", r.status_code, url, file=sys.stderr)
        except requests.RequestException as e:
            print("hata", e, file=sys.stderr)
        time.sleep(min(60, 3 * 2 ** i))
    raise RuntimeError(url)


def fungitastic_counts():
    print("FungiTastic meta verisi indiriliyor...", flush=True)
    r = get(FT_META)
    z = zipfile.ZipFile(io.BytesIO(r.content))
    counts, meta = Counter(), {}
    seen = set()
    for name in z.namelist():
        if not name.endswith(".csv") or "FewShot" in name or "Mini" in name:
            continue
        rows = list(csv.DictReader(io.TextIOWrapper(z.open(name), encoding="utf-8")))
        print(f"  {name}: {len(rows)} satır", flush=True)
        for row in rows:
            sp = (row.get("species") or "").strip()
            fn = row.get("filename")
            if not sp or (sp, fn) in seen:
                continue
            seen.add((sp, fn))
            counts[sp] += 1
            meta.setdefault(sp, {k: row.get(k, "") for k in ("phylum", "class", "order", "family", "genus", "poisonous")})
    print(f"FungiTastic: {len(counts)} tür, {sum(counts.values())} fotoğraf", flush=True)
    return counts, meta


def gbif_turkey():
    print("GBIF Türkiye kayıtları...", flush=True)
    r = get(f"{GBIF}/occurrence/search", {"country": "TR", "kingdomKey": 5, "limit": 0,
                                         "facet": "speciesKey", "facetLimit": 4000})
    facets = r.json()["facets"][0]["counts"]
    out = {}
    for i, f in enumerate(facets):
        sp = get(f"{GBIF}/species/{f['name']}").json()
        if sp.get("rank") != "SPECIES":
            continue
        out[sp.get("canonicalName") or sp.get("species")] = {
            "gbif_tr": f["count"], "class": sp.get("class", ""), "order": sp.get("order", ""),
            "family": sp.get("family", ""), "phylum": sp.get("phylum", ""),
        }
        if i % 200 == 0:
            print(f"  {i}/{len(facets)}", flush=True)
    print(f"GBIF Türkiye: {len(out)} tür", flush=True)
    return out


def inat_place(place_id):
    p = get(f"{INAT}/places/{place_id}").json()["results"][0]
    print(f"iNat yer {place_id}: {p['display_name']}", flush=True)
    return place_id


def inat_species_counts(place_id):
    out, page = {}, 1
    while True:
        r = get(f"{INAT}/observations/species_counts", {
            "place_id": place_id, "taxon_id": 47170, "quality_grade": "research", "per_page": 500, "page": page})
        res = r.json()["results"]
        for x in res:
            t = x["taxon"]
            if t.get("rank") == "species":
                out[t["name"]] = x["count"]
        if len(res) < 500 or page >= 20:
            break
        page += 1
        time.sleep(1)
    return out


def main():
    ft, ft_meta = fungitastic_counts()
    gb = gbif_turkey()
    tr_place = inat_place(7183)  # Türkiye
    eu_place = inat_place(97391)  # Avrupa
    inat_tr = inat_species_counts(tr_place)
    print(f"iNat Türkiye: {len(inat_tr)} tür", flush=True)
    inat_eu = inat_species_counts(eu_place)
    print(f"iNat Avrupa: {len(inat_eu)} tür", flush=True)

    current = set()
    with open("app/src/main/assets/species.tsv", encoding="utf-8") as f:
        for row in csv.DictReader(f, delimiter="\t"):
            current.add(row["taxon"])

    names = set(gb) | set(inat_tr) | current
    rows = []
    for n in names:
        g = gb.get(n, {})
        m = ft_meta.get(n, {})
        cls = g.get("class") or m.get("class", "")
        order = g.get("order") or m.get("order", "")
        rows.append({
            "taxon": n, "mevcut": int(n in current),
            "gbif_tr": g.get("gbif_tr", 0), "inat_tr": inat_tr.get(n, 0),
            "fungitastic": ft.get(n, 0), "inat_avrupa": inat_eu.get(n, 0),
            "class": cls, "order": order, "family": g.get("family") or m.get("family", ""),
            "makro": int(cls in MACRO_CLASSES and order not in LICHEN_ORDERS),
            "ft_poisonous": m.get("poisonous", ""),
        })
    rows.sort(key=lambda r: (-r["mevcut"], -(r["gbif_tr"] + 5 * r["inat_tr"])))
    with open("training/aday_turler.csv", "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=list(rows[0]))
        w.writeheader()
        w.writerows(rows)
    with open("training/fungitastic_turler.csv", "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["species", "photos", "class", "family", "poisonous", "inat_avrupa"])
        for sp, c in ft.most_common():
            m = ft_meta[sp]
            w.writerow([sp, c, m.get("class"), m.get("family"), m.get("poisonous"), inat_eu.get(sp, 0)])
    print(f"Yazıldı: {len(rows)} aday", flush=True)


if __name__ == "__main__":
    main()
