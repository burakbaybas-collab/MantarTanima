"""GBIF'ten (Observation.org, Mushroom Observer, İskandinav atlasları, herbaryumlar...) tür fotoğrafı indirir.

- iNaturalist ve Danimarka Mantar Atlası kayıtları ATLANIR (zaten iNat ve FungiTastic verisinde var).
- Yalnızca açık lisanslı fotoğraflar alınır (CC0, CC-BY, CC-BY-SA, CC-BY-NC, CC-BY-NC-SA).
- Önce Türkiye, sonra Avrupa, sonra dünya kayıtları; gözlem başına en fazla 2 fotoğraf.
- Fotoğraflar 448 piksele küçültülerek kaydedilir: <out>/<tür_id>/gbif_<kayıt>_<n>.jpg
- Her tür klasörüne kaynak/lisans/hak sahibi listesi yazılır (_kaynaklar.csv).
"""
import argparse
import csv
import gzip
import json
import io
import os
import sys
import time
from concurrent.futures import ThreadPoolExecutor

import requests
from PIL import Image

Image.MAX_IMAGE_PIXELS = 120_000_000

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
S.mount("https://", requests.adapters.HTTPAdapter(pool_connections=64, pool_maxsize=128))
S.mount("http://", requests.adapters.HTTPAdapter(pool_connections=64, pool_maxsize=128))
S.headers["User-Agent"] = "FungiLens-egitim/1.0 (github.com/burakbaybas-collab/MantarTanima)"


def get(path, params, retries=7):
    for i in range(retries):
        try:
            r = S.get(f"{API}/{path}", params=params, timeout=90)
            if r.status_code == 200:
                return r.json()
            if i == 0:
                print(f"  GBIF {r.status_code} {r.headers.get('Retry-After')} {r.text[:120]!r}", flush=True)
            if r.status_code in (429, 500, 502, 503, 504):
                wait = r.headers.get("Retry-After")
                back = min(60, 3 * 2 ** i)
                time.sleep(max(back, float(wait)) if wait and wait.isdigit() else back)
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
    # Tek sorguda veri setlerini bul (iNat/Danimarka hariç), sonra yalnız onları sayfala.
    f = get("occurrence/search", dict(taxonKey=taxon_key, mediaType="StillImage", limit=0,
                                      facet="datasetKey", **{"datasetKey.facetLimit": 40}))
    dsets = [c["name"] for c in (f.get("facets") or [{}])[0].get("counts", []) if c["name"] not in SKIP_DATASETS]
    for ds in dsets:
        offset = 0
        while len(out) < want * 2 and offset < 3000:
            try:
                page = get("occurrence/search", dict(taxonKey=taxon_key, mediaType="StillImage",
                                                      datasetKey=ds, limit=300, offset=offset))
            except RuntimeError as e:
                print(f"  uyarı: {e}", flush=True)
                break
            for occ in page.get("results", []):
                k = occ["key"]
                if k in seen:
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
                                "country": occ.get("countryCode", ""), "continent": occ.get("continent", "")})
                    n += 1
                    if n >= PER_OCC:
                        break
            if page.get("endOfRecords", True):
                break
            offset += 300
        if len(out) >= want * 2:
            break
    # Öncelik: Türkiye > Avrupa > diğer
    out.sort(key=lambda it: 0 if it["country"] == "TR" else (1 if it["continent"] == "EUROPE" else 2))
    return out[:want]


T0 = 0
HOST_FAIL = {}


def download(item, folder):
    path = os.path.join(folder, f"gbif_{item['occ']}_{item['i']}.jpg")
    if os.path.exists(path):
        return True
    host = item["url"].split("/")[2]
    if HOST_FAIL.get(host, 0) > 30:   # sürekli yanıt vermeyen sunucuyu atla
        return False
    for _ in range(2):
        try:
            r = S.get(item["url"], timeout=(10, 25))
            if r.status_code != 200 or len(r.content) < 5000:
                return False
            if len(r.content) > 25_000_000:
                return False
            im = Image.open(io.BytesIO(r.content))
            im.draft("RGB", (MAX_PX * 2, MAX_PX * 2))   # JPEG'i küçük çözerek bellek tasarrufu
            im = im.convert("RGB")
            if min(im.size) < 200:
                return False
            im.thumbnail((MAX_PX, MAX_PX), Image.LANCZOS)
            im.save(path, "JPEG", quality=85, optimize=True)
            return True
        except Exception:
            time.sleep(1)
    HOST_FAIL[host] = HOST_FAIL.get(host, 0) + 1
    return False


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--species", default="app/src/main/assets/species.tsv")
    ap.add_argument("--out", default="training/data_gbif")
    ap.add_argument("--per-species", type=int, default=200)
    ap.add_argument("--workers", type=int, default=48)
    ap.add_argument("--shard", type=int, default=0)
    ap.add_argument("--shards", type=int, default=1)
    ap.add_argument("--only", default="")
    ap.add_argument("--plan", help="1. aşama: yalnızca GBIF sorguları; fotoğraf listesini bu dosyaya yaz")
    ap.add_argument("--from-plan", help="2. aşama: listedeki fotoğrafları indir")
    ap.add_argument("--api-workers", type=int, default=4)
    ap.add_argument("--skip-file", help="Bu dosyadaki türler zaten indirilmiş; atla")
    args = ap.parse_args()

    global T0
    T0 = time.time()
    if args.from_plan:
        return download_plan(args)

    with open(args.species, encoding="utf-8") as f:
        species = list(csv.DictReader(f, delimiter="\t"))
    only = {s.strip() for s in args.only.split(",") if s.strip()}
    skip = set()
    if args.skip_file and os.path.exists(args.skip_file):
        skip = {l.strip() for l in open(args.skip_file, encoding="utf-8") if l.strip()}
    sel = [sp for sp in species if (not only or sp["id"] in only) and sp["id"] not in skip]
    # plan aşaması da parçalanabilir (farklı makineler = GBIF sınırına daha az takılma)
    sel = [sp for i, sp in enumerate(sel) if i % args.shards == args.shard]
    print(f"Planlanacak {len(sel)} tür (atlanan: {len(skip)})", flush=True)
    with ThreadPoolExecutor(args.api_workers) as ex:
        keys = dict(zip([sp["id"] for sp in sel], ex.map(lambda sp: resolve(sp["taxon"]), sel)))
    by_key = {}
    for sid, (k, _) in keys.items():
        if k:
            by_key.setdefault(k, []).append(sid)
    clash = {sid for ids in by_key.values() if len(ids) > 1 for sid in ids}

    def plan_one(sp):
        sid = sp["id"]
        key, name = keys[sid]
        if not key or sid in clash:
            print(f"{sid}: {'GBIF eşleşmesi yok' if not key else 'başka türle aynı takson'}, atlandı", flush=True)
            return sid, name, []
        try:
            items = candidates(key, int(args.per_species * 1.3))
        except Exception as e:
            print(f"{sid}: GBIF hatası ({e})", flush=True)
            items = []
        print(f"{sid}: {len(items)} aday fotoğraf [{(time.time() - T0) / 60:.0f} dk]", flush=True)
        return sid, name, items

    plan = {}

    def save():
        with gzip.open(args.plan + ".tmp", "wt", encoding="utf-8") as f:
            json.dump(plan, f)
        os.replace(args.plan + ".tmp", args.plan)

    # Süre sınırına takılırsa o ana kadarki plan kaybolmasın diye düzenli kaydedilir
    with ThreadPoolExecutor(args.api_workers) as ex:
        for n, (sid, name, items) in enumerate(ex.map(plan_one, sel), 1):
            plan[sid] = {"name": name, "items": items}
            if n % 20 == 0:
                save()
                print(f"--- {n}/{len(sel)} tür planlandı", flush=True)
    save()
    print(f"Plan: {len(plan)} tür, {sum(len(v['items']) for v in plan.values())} aday fotoğraf", flush=True)


def download_plan(args):
    plan = {}
    for path in args.from_plan.split(","):
        with gzip.open(path, "rt", encoding="utf-8") as f:
            plan.update(json.load(f))
    todo = [sid for i, sid in enumerate(sorted(plan)) if i % args.shards == args.shard]
    total = 0
    t0 = time.time()
    with ThreadPoolExecutor(args.workers) as dl:
        for sid in todo:
            items, name = plan[sid]["items"], plan[sid]["name"]
            if not items:
                continue
            folder = os.path.join(args.out, sid)
            os.makedirs(folder, exist_ok=True)
            results = list(dl.map(lambda it: download(it, folder), items))
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
            print(f"{sid} ({name}): {len(ok_items)} fotoğraf (Türkiye: {tr}) [{(time.time() - t0) / 60:.0f} dk]",
                  flush=True)
    print(f"Toplam {total} fotoğraf", flush=True)


if __name__ == "__main__":
    sys.exit(main())
