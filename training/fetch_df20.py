"""
Danish Fungi 2020 (DF20) veri setinin 300 piksellik fotoğraflarından yalnızca bizim türlerimize
ait olanları indirir. Arşiv (~6,5 GB) diske kaydedilmeden akış halinde okunur; yalnızca seçilen
fotoğraflar yazılır.

Kaynak: Picek ve ark., "Danish Fungi 2020", https://github.com/BohemianVRA/DanishFungiDataset
Lisans: yalnızca ticari olmayan araştırma amaçlı kullanım.

Kullanım:
    python training/fetch_df20.py --per-species 200 --out training/data_df20
"""
import argparse
import csv
import io
import os
import re
import tarfile
import urllib.request
import zipfile

META_URL = "http://ptak.felk.cvut.cz/plants/DanishFungiDataset/DF20-metadata.zip"
IMG_URL = "http://ptak.felk.cvut.cz/plants/DanishFungiDataset/DF20-300px.tar.gz"


def norm_name(name):
    """'Amanita muscaria (L.) Lam.' -> 'amanita muscaria'"""
    parts = re.sub(r"[^A-Za-z\- ]", " ", name or "").split()
    return " ".join(parts[:2]).lower()


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--species", default="app/src/main/assets/species.tsv")
    ap.add_argument("--out", default="training/data_df20")
    ap.add_argument("--per-species", type=int, default=200)
    args = ap.parse_args()

    with open(args.species, encoding="utf-8") as f:
        wanted = {norm_name(r["taxon"]): r["id"] for r in csv.DictReader(f, delimiter="\t")}

    print("Metadata indiriliyor...", flush=True)
    raw = urllib.request.urlopen(META_URL, timeout=300).read()
    by_file = {}
    per_id = {}
    with zipfile.ZipFile(io.BytesIO(raw)) as z:
        for name in z.namelist():
            if not name.lower().endswith(".csv"):
                continue
            rows = list(csv.DictReader(io.TextIOWrapper(z.open(name), encoding="utf-8", errors="replace")))
            if not rows:
                continue
            cols = rows[0].keys()
            name_col = next((c for c in ("scientificName", "species", "Species") if c in cols), None)
            img_col = next((c for c in ("image_path", "ImageUniqueID", "image") if c in cols), None)
            print(f"{name}: {len(rows)} satır, ad sütunu={name_col}, görüntü sütunu={img_col}", flush=True)
            if not name_col or not img_col:
                continue
            for r in rows:
                sid = wanted.get(norm_name(r[name_col]))
                if not sid:
                    continue
                fname = os.path.basename(r[img_col])
                if fname in by_file or per_id.get(sid, 0) >= args.per_species:
                    continue
                by_file[fname] = sid
                per_id[sid] = per_id.get(sid, 0) + 1
    print(f"Eşleşen tür: {len(per_id)}, seçilen fotoğraf: {len(by_file)}", flush=True)

    # Daha önce indirilmiş olanları atla
    todo = {f: s for f, s in by_file.items()
            if not os.path.exists(os.path.join(args.out, s, f"df20_{f}"))}
    if not todo:
        print("Tüm fotoğraflar zaten var.")
        return
    print(f"Arşiv akış halinde okunuyor ({len(todo)} fotoğraf aranıyor)...", flush=True)
    written = 0
    with urllib.request.urlopen(IMG_URL, timeout=600) as resp:
        with tarfile.open(fileobj=resp, mode="r|gz") as tar:
            for m in tar:
                if not m.isfile():
                    continue
                base = os.path.basename(m.name)
                sid = todo.get(base)
                if not sid:
                    continue
                dst = os.path.join(args.out, sid)
                os.makedirs(dst, exist_ok=True)
                with open(os.path.join(dst, f"df20_{base}"), "wb") as f:
                    f.write(tar.extractfile(m).read())
                written += 1
                if written % 2000 == 0:
                    print(f"  {written} fotoğraf yazıldı", flush=True)
    print(f"Tamamlandı: {written} fotoğraf.", flush=True)
    for sid in sorted(per_id):
        print(f"  {sid}: {per_id[sid]}")


if __name__ == "__main__":
    main()
