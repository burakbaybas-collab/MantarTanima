"""
iNaturalist'ten araştırma düzeyinde (research grade) mantar fotoğrafları indirir.

Kullanım:
    python training/fetch_inat.py --per-species 300

Her tür için training/data/<id>/ klasörüne JPEG fotoğraflar indirilir.
Yalnızca yeniden kullanıma izin veren lisanslar alınır (CC0, CC-BY, CC-BY-NC)
ve kaynak bilgisi training/data/<id>/_kaynaklar.csv dosyasına yazılır.

Tür listesi: app/src/main/assets/species.tsv (uygulama da aynı tabloyu kullanır).
Kendi fotoğraflarınızı eklemek için: training/own/<id>/ klasörüne koyun;
train.py bunları da otomatik kullanır.
"""
import argparse
import csv
import os
import sys
import time
from concurrent.futures import ThreadPoolExecutor

import requests

API = "https://api.inaturalist.org/v1"
HEADERS = {"User-Agent": "MantarTanima/0.1 (egitim verisi; github.com/burakbaybas-collab/MantarTanima)"}
LICENSES = "cc0,cc-by,cc-by-nc"


def api_get(path, params, retries=4):
    for attempt in range(retries):
        r = requests.get(f"{API}{path}", params=params, headers=HEADERS, timeout=60)
        if r.status_code == 200:
            time.sleep(1.0)  # iNaturalist'in istediği nazik hız
            return r.json()
        time.sleep(5 * (attempt + 1))
    r.raise_for_status()


def resolve_taxon(name):
    """Adı iNaturalist'teki geçerli taksona çevirir; eski/eş anlamlı adları da tanır
    (ör. Lepista nuda -> Collybia nuda)."""
    data = api_get("/taxa", {"q": name, "per_page": 30, "is_active": "true"})
    for t in data["results"]:
        names = {t["name"].lower(), (t.get("matched_term") or "").lower()}
        if name.lower() in names:
            return t["id"], t["name"], t["rank"]
    raise SystemExit(f"Takson bulunamadı: {name}")


def collect_photos(taxon_id, limit):
    photos, seen, page = [], set(), 1
    while len(photos) < limit and page <= 20:
        data = api_get("/observations", {
            "taxon_id": taxon_id,
            "quality_grade": "research",
            "photos": "true",
            "photo_license": LICENSES,
            "per_page": 200,
            "page": page,
            "order_by": "votes",
        })
        results = data["results"]
        if not results:
            break
        for obs in results:
            # Her gözlemden en fazla 3 fotoğraf: genelde farklı açılardır (üst, yan, alt)
            for p in obs.get("photos", [])[:3]:
                if p["id"] in seen or p.get("license_code") not in LICENSES.split(","):
                    continue
                seen.add(p["id"])
                photos.append({
                    "photo_id": p["id"],
                    "url": p["url"].replace("square", "medium"),
                    "license": p.get("license_code"),
                    "attribution": p.get("attribution", ""),
                    "observation": f"https://www.inaturalist.org/observations/{obs['id']}",
                })
        page += 1
    return photos[:limit]


def download(item, folder):
    path = os.path.join(folder, f"{item['photo_id']}.jpg")
    if os.path.exists(path) and os.path.getsize(path) > 0:
        return True
    for attempt in range(3):
        try:
            r = requests.get(item["url"], headers=HEADERS, timeout=60)
            if r.status_code == 200 and r.content[:3] == b"\xff\xd8\xff":  # geçerli JPEG
                with open(path, "wb") as f:
                    f.write(r.content)
                return True
        except requests.RequestException:
            pass
        time.sleep(2 * (attempt + 1))
    return False


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--species", default="app/src/main/assets/species.tsv")
    ap.add_argument("--out", default="training/data")
    ap.add_argument("--per-species", type=int, default=300)
    args = ap.parse_args()

    with open(args.species, encoding="utf-8") as f:
        species = list(csv.DictReader(f, delimiter="\t"))

    summary = []
    for sp in species:
        try:
            taxon_id, name, rank = resolve_taxon(sp["taxon"])
        except SystemExit as e:
            print(f"ATLANDI: {e}", flush=True)
            continue
        photos = collect_photos(taxon_id, args.per_species)
        folder = os.path.join(args.out, sp["id"])
        os.makedirs(folder, exist_ok=True)
        with ThreadPoolExecutor(max_workers=6) as ex:
            ok = sum(ex.map(lambda it: download(it, folder), photos))
        with open(os.path.join(folder, "_kaynaklar.csv"), "w", newline="", encoding="utf-8") as f:
            w = csv.DictWriter(f, fieldnames=["photo_id", "url", "license", "attribution", "observation"])
            w.writeheader()
            w.writerows(photos)
        print(f"{sp['id']:<24} {name} ({rank}, takson {taxon_id}): {ok} fotoğraf", flush=True)
        summary.append((sp["id"], ok))

    low = [s for s, n in summary if n < 50]
    if low:
        print("UYARI: Az fotoğrafı olan türler:", ", ".join(low), file=sys.stderr)


if __name__ == "__main__":
    main()
