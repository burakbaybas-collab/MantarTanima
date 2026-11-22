"""FungiTastic'ten (uzman doğrulamalı, Kuzey Avrupa) bizim türlerimize ait fotoğrafları alır.

Görüntüler büyük zip dosyaları halinde gelir (train/val/test, 300 piksel). Her zip indirilir,
yalnızca seçilen dosyalar çıkarılır ve zip silinir. GPU sunucusunda çalışır (disk ~40 GB yeter).

Lisans: FungiTastic / Danish Fungi verisi araştırma ve ticari olmayan kullanım içindir.
"""
import argparse
import csv
import io
import os
import random
import subprocess
import sys
import time
import zipfile
from collections import defaultdict

ROOT = "https://cmp.felk.cvut.cz/datagrid/FungiTastic/shared/download"
SPLITS = ["train", "val", "test"]


def fetch(url, dst, tries=6):
    for i in range(tries):
        r = subprocess.run(["curl", "-fL", "--retry", "5", "--retry-delay", "10", "-C", "-", "-o", dst, url])
        if r.returncode == 0 and os.path.getsize(dst) > 0:
            return dst
        print(f"İndirme hatası ({r.returncode}), tekrar {i + 1}", flush=True)
        time.sleep(30)
    raise RuntimeError(url)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--species", default="app/src/main/assets/species.tsv")
    ap.add_argument("--out", default="training/data_ft")
    ap.add_argument("--per-species", type=int, default=1000)
    ap.add_argument("--size", default="300")
    ap.add_argument("--tmp", default="/work/ft_tmp")
    ap.add_argument("--splits", default="train,val,test", help="Hızlı deneme için yalnızca val")
    ap.add_argument("--stream", action="store_true",
                    help="Zip'i diske indirmeden akış halinde aç (az diskli sunucular için)")
    args = ap.parse_args()
    os.makedirs(args.tmp, exist_ok=True)

    with open(args.species, encoding="utf-8") as f:
        species = list(csv.DictReader(f, delimiter="\t"))
    by_name = {s["taxon"].strip().lower(): s["id"] for s in species}

    meta_zip = fetch(f"{ROOT}/metadata.zip", os.path.join(args.tmp, "metadata.zip"))
    z = zipfile.ZipFile(meta_zip)
    pool = defaultdict(list)          # tür kimliği -> [(dosya adı, gözlem)]
    for name in z.namelist():
        if not name.endswith(".csv") or "FewShot" in name or "Mini" in name:
            continue
        for row in csv.DictReader(io.TextIOWrapper(z.open(name), encoding="utf-8")):
            sp = (row.get("species") or "").strip().lower()
            sid = by_name.get(sp)
            if sid:
                pool[sid].append((row["filename"], row.get("observationID", "")))
    rng = random.Random(42)
    wanted = {}                         # dosya adı -> tür kimliği
    for sid, items in pool.items():
        items = sorted(set(items))
        rng.shuffle(items)
        # Farklı gözlemlere öncelik: önce her gözlemden bir fotoğraf, sonra kalanlar
        first, rest, seen = [], [], set()
        for fn, obs in items:
            (rest if obs in seen else first).append(fn)
            seen.add(obs)
        for fn in (first + rest)[:args.per_species]:
            wanted[fn] = sid
    print(f"FungiTastic eşleşen tür: {len(pool)}/{len(species)}, seçilen fotoğraf: {len(wanted)}", flush=True)
    missing = [s["id"] for s in species if s["id"] not in pool]
    print("FungiTastic'te olmayan türler:", ", ".join(missing), flush=True)

    got = 0
    if args.stream:
        import requests
        from stream_unzip import stream_unzip
        for split in args.splits.split(","):
            url = f"{ROOT}/FungiTastic-{split}-{args.size}p.zip"
            print(f"Akış: {url}", flush=True)
            t = time.time()
            with requests.get(url, stream=True, timeout=600) as r:
                r.raise_for_status()
                for name, _size, chunks in stream_unzip(r.iter_content(chunk_size=1 << 20)):
                    base = os.path.basename(name.decode("utf-8", "replace"))
                    sid = wanted.get(base)
                    if not sid:
                        for _ in chunks:
                            pass
                        continue
                    d = os.path.join(args.out, sid)
                    os.makedirs(d, exist_ok=True)
                    with open(os.path.join(d, f"ft_{base}"), "wb") as out:
                        for c in chunks:
                            out.write(c)
                    got += 1
            print(f"  {split}: toplam {got} fotoğraf ({time.time() - t:.0f} sn)", flush=True)
        print(f"Bitti: {got} fotoğraf", flush=True)
        return
    for split in args.splits.split(","):
        url = f"{ROOT}/FungiTastic-{split}-{args.size}p.zip"
        zpath = os.path.join(args.tmp, os.path.basename(url))
        print(f"İndiriliyor: {url}", flush=True)
        t = time.time()
        try:
            fetch(url, zpath)
        except RuntimeError as e:
            print("ATLANDI:", e, flush=True)
            continue
        print(f"  {os.path.getsize(zpath) / 1e9:.1f} GB, {time.time() - t:.0f} sn", flush=True)
        with zipfile.ZipFile(zpath) as zz:
            for info in zz.infolist():
                base = os.path.basename(info.filename)
                sid = wanted.get(base)
                if not sid or info.is_dir():
                    continue
                d = os.path.join(args.out, sid)
                os.makedirs(d, exist_ok=True)
                dst = os.path.join(d, f"ft_{base}")
                if not os.path.exists(dst):
                    with zz.open(info) as src, open(dst, "wb") as out:
                        out.write(src.read())
                    got += 1
        os.remove(zpath)
        print(f"  {split}: toplam {got} fotoğraf çıkarıldı", flush=True)

    counts = {sid: len(os.listdir(os.path.join(args.out, sid))) for sid in os.listdir(args.out)} if os.path.isdir(args.out) else {}
    print(f"Bitti: {got} fotoğraf, {len(counts)} tür", flush=True)
    if got == 0:
        sys.exit("FungiTastic'ten fotoğraf alınamadı")


if __name__ == "__main__":
    main()
