"""GBIF'ten (Observation.org, Mushroom Observer, İskandinav atlasları, herbaryumlar...) tür fotoğrafı indirir.

- iNaturalist ve Danimarka Mantar Atlası kayıtları ATLANIR (zaten iNat ve FungiTastic verisinde var).
- Yalnızca açık lisanslı fotoğraflar alınır (CC0, CC-BY, CC-BY-SA, CC-BY-NC, CC-BY-NC-SA).
- Önce Türkiye, sonra Avrupa, sonra dünya kayıtları; gözlem başına en fazla 2 fotoğraf.
- Fotoğraflar 448 piksele küçültülerek kaydedilir: <out>/<tür_id>/gbif_<kayıt>_<n>.jpg
- Her tür klasörüne kaynak/lisans/hak sahibi listesi yazılır (_kaynaklar.csv).
"""
import argparse
import csv
import io
import os
import sys
import time
from concurrent.futures import ThreadPoolExecutor

import requests
from PIL import Image

API = "https://api.gbif.org/v1"
MAX_PX = 448
PER_OCC = 2

# iNaturalist research-grade ve Danimarka Mantar Atlası (FungiTastic/DF20 kaynağı)
SKIP_DATASETS = {
    "50c9509d-22c7-4a22-a47d-8c48425ef4a7",
    "84d26682-f762-11e1-a439-00145eb45e9a",
}
SKIP_URL_PARTS = ("inaturalist", "svampe", "svampeatlas")
OK_LICENSES = ("publicdomain/zero", "licenses/by/", "licenses/by-sa/", "licenses/by-nc/", "licenses/by-nc-sa/",
               "CC0_1_0", "CC_BY_4_0", "CC_BY_NC_4_0")

OVERRIDES = {"Amanita gemmata": ["Amanita gemmata", "Amanita junquillea"]}

S = requests.Session()
S.headers["User-Agent"] = "FungiLens-egitim/1.0 (github.com/burakbaybas-collab/MantarTanima)"


def get(path, params, retries=6):
    for i in range(retries):
        try:
            r = S.get(f"{API}/{path}", params=params, timeout=60)
            if r.status_code == 200:
                return r.json()
            if r.status_code in (429, 500, 502, 503, 504):
                time.sleep(5 * (i + 1))
                continue
            r.raise_for_status()
        except requests.RequestException:
            time.sleep(5 * (i + 1))
    raise RuntimeError(f"GBIF yanıt vermedi: {path} {params}")


def resolve(name):
    """Tür adını GBIF taksonuna eşler; eş anlamlıysa kabul edilen adı kullanır."""
    for q in OVERRIDES.get(name, [name]):
        m = get("species/match", {"name": q, "kingdom": "Fungi", "strict": "true"})
        if m.get("matchType") != "EXACT" or m.get("rank") not in ("SPECIES", "SUBSPECIES", "VARIETY", "FORM"):
            continue
        key = m.get("acceptedUsageKey") or m.get("usageKey")
        if key:
            return int(key), m.get("species") or m.get("scientificName")
    return None, None


def license_ok(lic):
    return bool(lic) and any(p in lic for p in OK_LICENSES) and "-nd" not in lic.lower()


def candidates(taxon_key, want):
    """Fotoğraflı kayıtları öncelik sırasıyla toplar: Türkiye > Avrupa > dünya."""
    seen, out = set(), []
    for scope in ({"country": "TR"}, {"continent": "EUROPE"}, {}):
        offset = 0
        while len(out) < want and offset < 5000:
            page = get("occurrence/search", dict(scope, taxonKey=taxon_key, mediaType="StillImage",
                                                  limit=300, offset=offset))
            for occ in page.get("results", []):
                k = occ["key"]
                if k in seen or occ.get("datasetKey") in SKIP_DATASETS:
                    continue
                seen.add(k)
                n = 0
                for i, m in enumerate(occ.get("media", [])):
                    url = m.get("identifier") or ""
                    if m.get("type") != "StillImage" or not url.startswith("http"):
                        continue
                    if any(p in url.lower() for p in SKIP_URL_PARTS):
                        break
                    lic = m.get("license") or occ.get("license") or ""
                    if not license_ok(lic):
                        continue
                    out.append({"occ": k, "i": i, "url": url, "license": lic,
                                "rights": m.get("rightsHolder") or m.get("creator") or occ.get("recordedBy") or "",
                                "dataset": occ.get("datasetName") or occ.get("datasetKey", ""),
                                "country": occ.get("countryCode", "")})
                    n += 1
                    if n >= PER_OCC:
                        break
            if page.get("endOfRecords", True):
                break
            offset += 300
        if len(out) >= want:
            break
    return out


def download(item, folder):
    path = os.path.join(folder, f"gbif_{item['occ']}_{item['i']}.jpg")
    if os.path.exists(path):
        return True
    for _ in range(3):
        try:
            r = S.get(item["url"], timeout=40)
            if r.status_code != 200 or len(r.content) < 5000:
                return False
            im = Image.open(io.BytesIO(r.content)).convert("RGB")
            if min(im.size) < 200:
                return False
            im.thumbnail((MAX_PX, MAX_PX), Image.LANCZOS)
            im.save(path, "JPEG", quality=85, optimize=True)
            return True
        except Exception:
            time.sleep(2)
    return False


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--species", default="app/src/main/assets/species.tsv")
    ap.add_argument("--out", default="training/data_gbif")
    ap.add_argument("--per-species", type=int, default=200)
    ap.add_argument("--workers", type=int, default=24)
    ap.add_argument("--shard", type=int, default=0)
    ap.add_argument("--shards", type=int, default=1)
    ap.add_argument("--only", default="")
    ap.add_argument("--max-minutes", type=float, default=0, help="Süre dolunca kalan türleri atla")
    args = ap.parse_args()

    with open(args.species, encoding="utf-8") as f:
        species = list(csv.DictReader(f, delimiter="\t"))
    only = {s.strip() for s in args.only.split(",") if s.strip()}

    # Önce tüm eşleşmeler: iki listedeki tür aynı GBIF taksonuna düşerse ikisi de atlanır
    keys = {}
    for sp in species:
        if only and sp["id"] not in only:
            continue
        keys[sp["id"]] = resolve(sp["taxon"])
    by_key = {}
    for sid, (k, _) in keys.items():
        if k:
            by_key.setdefault(k, []).append(sid)
    clash = {sid for ids in by_key.values() if len(ids) > 1 for sid in ids}

    todo = [sp for i, sp in enumerate(s for s in species if s["id"] in keys) if i % args.shards == args.shard]
    total = 0
    with ThreadPoolExecutor(args.workers) as ex:
        t0 = time.time()
        for sp in todo:
            sid = sp["id"]
            if args.max_minutes and time.time() - t0 > args.max_minutes * 60:
                print(f"Süre doldu; {sid} ve sonrası atlandı", flush=True)
                break
            key, name = keys[sid]
            if not key:
                print(f"{sid}: GBIF'te bulunamadı", flush=True)
                continue
            if sid in clash:
                print(f"{sid}: başka bir türle aynı GBIF taksonu ({name}), atlandı", flush=True)
                continue
            items = candidates(key, int(args.per_species * 1.3))
            folder = os.path.join(args.out, sid)
            os.makedirs(folder, exist_ok=True)
            results = list(ex.map(lambda it: download(it, folder), items))
            ok_items = [it for it, ok in zip(items, results) if ok][:args.per_species]
            keep = {f"gbif_{it['occ']}_{it['i']}.jpg" for it in ok_items}
            for f in os.listdir(folder):
                if f.endswith(".jpg") and f not in keep:
                    os.remove(os.path.join(folder, f))
            with open(os.path.join(folder, "_kaynaklar.csv"), "w", newline="", encoding="utf-8") as f:
                w = csv.writer(f)
                w.writerow(["dosya", "gbif_kayit", "lisans", "hak_sahibi", "veri_seti", "ulke", "url"])
                for it in ok_items:
                    w.writerow([f"gbif_{it['occ']}_{it['i']}.jpg", it["occ"], it["license"], it["rights"],
                                it["dataset"], it["country"], it["url"]])
            tr = sum(1 for it in ok_items if it["country"] == "TR")
            total += len(ok_items)
            print(f"{sid} ({name}): {len(ok_items)} fotoğraf (Türkiye: {tr})", flush=True)
    print(f"Toplam {total} fotoğraf", flush=True)


if __name__ == "__main__":
    sys.exit(main())
