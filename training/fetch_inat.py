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
import threading
from concurrent.futures import ThreadPoolExecutor

import requests

API = "https://api.inaturalist.org/v1"
HEADERS = {"User-Agent": "MantarTanima/0.1 (egitim verisi; github.com/burakbaybas-collab/MantarTanima)"}
LICENSES = "cc0,cc-by,cc-by-nc"


_api_lock = threading.Lock()
_last_call = [0.0]
API_INTERVAL = 1.0  # iNaturalist'in istediği nazik hız: saniyede en fazla ~1 istek (tüm iş parçacıkları toplamı)


def api_get(path, params, retries=8):
    last_err = None
    for attempt in range(retries):
        with _api_lock:
            wait = _last_call[0] + API_INTERVAL - time.time()
            if wait > 0:
                time.sleep(wait)
            _last_call[0] = time.time()
        try:
            r = requests.get(f"{API}{path}", params=params, headers=HEADERS, timeout=60)
            if r.status_code == 200:
                return r.json()
            last_err = f"HTTP {r.status_code}"
        except (requests.RequestException, ValueError) as e:  # bağlantı kopması, bozuk yanıt
            last_err = repr(e)
        time.sleep(min(120, 5 * 2 ** attempt))
    raise RuntimeError(f"iNaturalist yanıt vermedi ({path}): {last_err}")


def resolve_taxon(name):
    """Adı iNaturalist'teki geçerli taksona çevirir; eski/eş anlamlı adları da tanır
    (ör. Lepista nuda -> Collybia nuda)."""
    data = api_get("/taxa", {"q": name, "per_page": 30, "is_active": "true"})
    for t in data["results"]:
        names = {t["name"].lower(), (t.get("matched_term") or "").lower()}
        if name.lower() in names:
            return t["id"], t["name"], t["rank"]
    raise SystemExit(f"Takson bulunamadı: {name}")


# Önce Avrupa + Türkiye gözlemleri, yetmezse dünyanın geri kalanı
REGIONS = ["97391,7183", None]


def collect_photos(taxon_id, limit):
    photos, seen, seen_obs = [], set(), set()
    for region in REGIONS:
        page = 1
        while len(photos) < limit and page <= 20:
            params = {
                "taxon_id": taxon_id,
                "quality_grade": "research",
                "photos": "true",
                "photo_license": LICENSES,
                "per_page": 200,
                "page": page,
                "order_by": "votes",
            }
            if region:
                params["place_id"] = region
            data = api_get("/observations", params)
            results = data["results"]
            if not results:
                break
            for obs in results:
                if obs["id"] in seen_obs:
                    continue
                seen_obs.add(obs["id"])
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
        if len(photos) >= limit:
            break
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
    ap.add_argument("--workers", type=int, default=6, help="Eşzamanlı fotoğraf indirme sayısı")
    ap.add_argument("--species-workers", type=int, default=1, help="Aynı anda işlenen tür sayısı")
    ap.add_argument("--shard", type=int, default=0, help="Paralel indirmede bu parçanın sırası")
    ap.add_argument("--shards", type=int, default=1, help="Toplam parça sayısı")
    args = ap.parse_args()

    with open(args.species, encoding="utf-8") as f:
        species = list(csv.DictReader(f, delimiter="\t"))
    species = [sp for i, sp in enumerate(species) if i % args.shards == args.shard]
    print(f"Parça {args.shard + 1}/{args.shards}: {len(species)} tür", flush=True)

    dl_pool = ThreadPoolExecutor(max_workers=args.workers)

    def one(sp):
        try:
            taxon_id, name, rank = resolve_taxon(sp["taxon"])
            photos = collect_photos(taxon_id, args.per_species)
        except (SystemExit, RuntimeError) as e:  # bir türdeki sorun tüm işi durdurmasın
            print(f"ATLANDI {sp['id']}: {e}", flush=True)
            return sp["id"], 0
        folder = os.path.join(args.out, sp["id"])
        os.makedirs(folder, exist_ok=True)
        ok = sum(dl_pool.map(lambda it: download(it, folder), photos))
        with open(os.path.join(folder, "_kaynaklar.csv"), "w", newline="", encoding="utf-8") as f:
            w = csv.DictWriter(f, fieldnames=["photo_id", "url", "license", "attribution", "observation"])
            w.writeheader()
            w.writerows(photos)
        print(f"{sp['id']:<24} {name} ({rank}, takson {taxon_id}): {ok} fotoğraf", flush=True)
        return sp["id"], ok

    # Birkaç tür aynı anda: API sırası tek, fotoğraf indirmeleri paralel ilerler
    with ThreadPoolExecutor(max_workers=args.species_workers) as ex:
        summary = list(ex.map(one, species))
    dl_pool.shutdown()
    print(f"Toplam: {sum(n for _, n in summary)} fotoğraf, {sum(1 for _, n in summary if n)} tür", flush=True)

    low = [s for s, n in summary if n < 50]
    if low:
        print("UYARI: Az fotoğrafı olan türler:", ", ".join(low), file=sys.stderr)


if __name__ == "__main__":
    main()
