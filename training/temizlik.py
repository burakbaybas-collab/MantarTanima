"""FungiLens eğitim verisi temizliği (Windows bilgisayarda, GPU ile).

Ne yapar:
  1. GitHub'daki eğitim paketlerini (iNaturalist + FungiTastic, ~17 GB) indirir;
     yarıda kalırsa kaldığı yerden devam eder.
  2. Son eğitilen modeli (best_model.pt) indirir.
  3. Paketleri diske açmadan, her fotoğrafı doğrudan okuyup modelden geçirir.
  4. Şüpheli fotoğrafları işaretler:
       - Model fotoğrafın etiketine çok düşük, başka bir türe çok yüksek olasılık veriyorsa
         (yanlış etiket ya da mantar olmayan fotoğraf olabilir),
       - Aynı fotoğraf iki farklı türün klasöründe varsa (sinek mantarı / gemmata hatası gibi).
  5. Sonuçları C:\\FungiLens\\temizlik klasörüne yazar:
       temizlik_sonuc.csv.gz  -> bunu sohbete gönderin
       ozet.txt               -> kısa özet
       ornekler\\             -> en çok şüpheli fotoğrafı olan türlerden küçük örnek sayfalar

Çalıştırma (sunucunun sanal ortamında):
    pip install timm pillow numpy requests
    python temizlik.py
"""
import csv
import gzip
import hashlib
import io
import os
import sys
import tarfile
import time
from collections import Counter, defaultdict
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path

import numpy as np
import requests
from PIL import Image, ImageDraw, ImageFile

ImageFile.LOAD_TRUNCATED_IMAGES = True

REPO = "burakbaybas-collab/MantarTanima"
DATA_TAG = "egitim-verisi"
MODEL_URL = f"https://github.com/{REPO}/releases/download/son-egitim/best_model.pt"
ROOT = Path(os.environ.get("FUNGILENS_KOK", r"C:\FungiLens"))
DATA_DIR = ROOT / "veri"
OUT_DIR = ROOT / "temizlik"
BATCH = 64
THREADS = max(4, min(12, (os.cpu_count() or 8) - 2))

# Şüphe eşikleri
OWN_MAX = 0.05      # kendi etiketine verilen olasılık bundan düşük
OTHER_MIN = 0.80    # ve başka bir türe bundan yüksek


# ---------------------------------------------------------------- indirme
def download(url, dest, retries=10):
    dest.parent.mkdir(parents=True, exist_ok=True)
    for attempt in range(1, retries + 1):
        have = dest.stat().st_size if dest.exists() else 0
        headers = {"Range": f"bytes={have}-"} if have else {}
        try:
            with requests.get(url, headers=headers, stream=True, timeout=60) as r:
                if r.status_code == 416:  # zaten tamamı inmiş
                    return
                if r.status_code == 404:
                    raise SystemExit(f"Bulunamadı: {url}")
                r.raise_for_status()
                total = int(r.headers.get("Content-Length", 0)) + (have if r.status_code == 206 else 0)
                mode = "ab" if r.status_code == 206 else "wb"
                done = have if r.status_code == 206 else 0
                last = time.time()
                with open(dest, mode) as f:
                    for chunk in r.iter_content(1 << 20):
                        f.write(chunk)
                        done += len(chunk)
                        if time.time() - last > 5:
                            pct = f"{done / total * 100:5.1f}%" if total else ""
                            print(f"\r  {dest.name}: {done / 1e9:.2f} GB {pct}   ", end="", flush=True)
                            last = time.time()
                print(f"\r  {dest.name}: tamam ({done / 1e9:.2f} GB)            ", flush=True)
                if total and done < total:
                    raise IOError("eksik indi")
                return
        except SystemExit:
            raise
        except Exception as e:
            print(f"\n  indirme kesildi ({attempt}/{retries}): {e}; tekrar deneniyor", flush=True)
            time.sleep(10)
    raise SystemExit(f"İndirilemedi: {url}")


def release_assets():
    r = requests.get(f"https://api.github.com/repos/{REPO}/releases/tags/{DATA_TAG}", timeout=60)
    r.raise_for_status()
    return [(a["name"], a["browser_download_url"], a["size"]) for a in r.json()["assets"]]


class ConcatReader(io.RawIOBase):
    """Birden çok parça dosyasını tek akış gibi okur (FungiTastic parçaları için)."""

    def __init__(self, paths):
        self.paths = list(paths)
        self.f = None

    def readable(self):
        return True

    def readinto(self, b):
        while True:
            if self.f is None:
                if not self.paths:
                    return 0
                self.f = open(self.paths.pop(0), "rb")
            n = self.f.readinto(b)
            if n:
                return n
            self.f.close()
            self.f = None


# ---------------------------------------------------------------- model
def load_model(path):
    import timm
    import torch

    ck = torch.load(path, map_location="cpu", weights_only=False)
    dev = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    model = timm.create_model(ck["model"], pretrained=False, num_classes=len(ck["classes"]))
    model.load_state_dict(ck["state_dict"])
    model.eval().to(dev)
    if dev.type == "cuda":
        model = model.half()
    mean = torch.tensor(ck["mean"]).view(1, 3, 1, 1).to(dev)
    std = torch.tensor(ck["std"]).view(1, 3, 1, 1).to(dev)
    img = int(ck["img"])
    print(f"Model: {len(ck['classes'])} tür, {img} piksel, cihaz: "
          f"{torch.cuda.get_device_name(0) if dev.type == 'cuda' else 'CPU (yavaş!)'}", flush=True)

    @torch.inference_mode()
    def predict(arrays):
        x = torch.from_numpy(np.stack(arrays)).to(dev).permute(0, 3, 1, 2).float() / 255.0
        x = (x - mean) / std
        if dev.type == "cuda":
            x = x.half()
        return torch.softmax(model(x).float(), dim=-1).cpu().numpy()

    return predict, ck["classes"], img


def prep(data, img):
    """JPEG bayt -> (img x img x 3) uint8 dizi; kısa kenar ölçekle + ortadan kırp."""
    try:
        im = Image.open(io.BytesIO(data))
        im.draft("RGB", (img * 2, img * 2))
        im = im.convert("RGB")
        w, h = im.size
        s = img / min(w, h)
        im = im.resize((max(img, round(w * s)), max(img, round(h * s))), Image.BILINEAR)
        w, h = im.size
        l, t = (w - img) // 2, (h - img) // 2
        return np.asarray(im.crop((l, t, l + img, t + img)), dtype=np.uint8)
    except Exception:
        return None


# ---------------------------------------------------------------- tarama
def iter_tar(fileobj):
    """Paketteki (kaynak, tür, dosya, bayt) dörtlülerini sırayla verir."""
    with tarfile.open(fileobj=fileobj, mode="r|") as tf:
        for m in tf:
            if not m.isfile():
                continue
            parts = m.name.replace("\\", "/").split("/")
            if len(parts) < 3 or parts[0] not in ("data", "data_ft"):
                continue
            if not parts[-1].lower().endswith((".jpg", ".jpeg", ".png", ".webp")):
                continue
            src = "inat" if parts[0] == "data" else "ft"
            yield src, parts[1], parts[-1], tf.extractfile(m).read()


def main():
    t_start = time.time()
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    (OUT_DIR / "ornekler").mkdir(exist_ok=True)

    print("== 1) Model indiriliyor ==", flush=True)
    model_path = ROOT / "model_temizlik" / "best_model.pt"
    download(MODEL_URL, model_path)

    print("== 2) Eğitim paketleri indiriliyor (~17 GB, yarıda kalırsa kaldığı yerden sürer) ==", flush=True)
    assets = release_assets()
    inat = sorted([a for a in assets if a[0].startswith("veri-inat-")], key=lambda a: int(a[0].split("-")[2].split(".")[0]))
    ft = sorted([a for a in assets if a[0].startswith("veri-ft.tar.part-")])
    for name, url, size in inat + ft:
        p = DATA_DIR / name
        if p.exists() and p.stat().st_size == size:
            print(f"  {name}: zaten var", flush=True)
            continue
        download(url, p)

    print("== 3) Fotoğraflar taranıyor ==", flush=True)
    predict, classes, img = load_model(model_path)
    cls_index = {c: i for i, c in enumerate(classes)}

    sources = [(n, lambda n=n: open(DATA_DIR / n, "rb")) for n, _, _ in inat]
    if ft:
        sources.append(("FungiTastic", lambda: io.BufferedReader(ConcatReader([DATA_DIR / n for n, _, _ in ft]), 1 << 20)))

    rows = []                       # şüpheli fotoğraflar
    per_species = defaultdict(lambda: [0, 0])   # tür -> [toplam, doğru]
    hashes = {}                     # md5 -> (kaynak, tür, dosya)
    dup_rows = []
    thumbs = defaultdict(list)      # tür -> [(küçük resim, açıklama)]
    skipped = Counter()
    n_total = 0
    pool = ThreadPoolExecutor(THREADS)

    def flush(batch):
        nonlocal n_total
        arrays = list(pool.map(lambda b: prep(b[3], img), batch))
        keep = [(b, a) for b, a in zip(batch, arrays) if a is not None]
        skipped["okunamadı"] += len(batch) - len(keep)
        if not keep:
            return
        probs = predict([a for _, a in keep])
        for ((src, sid, fname, data), a), p in zip(keep, probs):
            n_total += 1
            own = cls_index.get(sid)
            if own is None:
                skipped["modelde olmayan tür"] += 1
                continue
            top = int(p.argmax())
            per_species[sid][0] += 1
            per_species[sid][1] += int(top == own)
            if p[own] < OWN_MAX and top != own and p[top] >= OTHER_MIN:
                rows.append([src, sid, fname, classes[top], f"{p[top]:.3f}", f"{p[own]:.3f}", "baska_tur"])
                if len(thumbs[sid]) < 40:
                    t = Image.fromarray(a).resize((150, 150))
                    thumbs[sid].append((t, f"{classes[top][:20]} {p[top]:.2f}"))

    batch = []
    n_batches = 0
    for name, opener in sources:
        t0, before = time.time(), n_total
        with opener() as fobj:
            for src, sid, fname, data in iter_tar(fobj):
                h = hashlib.md5(data).hexdigest()
                prev = hashes.get(h)
                if prev is None:
                    hashes[h] = (src, sid, fname)
                elif prev[1] != sid:
                    dup_rows.append([src, sid, fname, prev[1], "", "", "ayni_foto_baska_turde"])
                batch.append((src, sid, fname, data))
                if len(batch) >= BATCH:
                    flush(batch)
                    batch = []
                    n_batches += 1
                    if n_batches % 50 == 0:
                        rate = (n_total - before) / max(1e-6, time.time() - t0)
                        print(f"\r  {name}: {n_total - before} fotoğraf ({rate:.0f}/sn), "
                              f"şüpheli toplam: {len(rows)}   ", end="", flush=True)
        if batch:
            flush(batch)
            batch = []
        print(f"\r  {name}: {n_total - before} fotoğraf tarandı, şüpheli toplam: {len(rows)}          ", flush=True)

    print("== 4) Sonuçlar yazılıyor ==", flush=True)
    with gzip.open(OUT_DIR / "temizlik_sonuc.csv.gz", "wt", encoding="utf-8", newline="") as f:
        w = csv.writer(f)
        w.writerow(["kaynak", "tur", "dosya", "modelin_dedigi", "olasilik", "kendi_olasiligi", "neden"])
        w.writerows(rows + dup_rows)
        w.writerow([])
        w.writerow(["#tur_ozeti", "tur", "toplam", "dogru"])
        for sid, (tot, ok) in sorted(per_species.items()):
            w.writerow(["#tur_ozeti", sid, tot, ok])

    flagged = Counter(r[1] for r in rows)
    lines = [
        f"Taranan fotoğraf: {n_total}",
        f"Başka türe benzeyen şüpheli fotoğraf: {len(rows)} (%{len(rows) / max(1, n_total) * 100:.2f})",
        f"Aynı fotoğraf iki farklı türde: {len(dup_rows)}",
        f"Atlanan: {dict(skipped)}",
        f"Süre: {(time.time() - t_start) / 60:.0f} dk",
        "",
        "En çok şüpheli fotoğrafı olan türler:",
    ]
    for sid, n in flagged.most_common(40):
        tot = per_species[sid][0]
        lines.append(f"  {sid}: {n} / {tot} (%{n / max(1, tot) * 100:.1f})")
    (OUT_DIR / "ozet.txt").write_text("\n".join(lines), encoding="utf-8")
    print("\n".join(lines), flush=True)

    for sid, n in flagged.most_common(30):
        items = thumbs[sid]
        cols = 8
        sheet = Image.new("RGB", (cols * 150, ((len(items) + cols - 1) // cols) * 170 + 30), "white")
        d = ImageDraw.Draw(sheet)
        d.text((6, 8), f"{sid}: modelin baska tur dedigi fotograflar ({n})", fill="black")
        for i, (t, cap) in enumerate(items):
            x, y = (i % cols) * 150, 30 + (i // cols) * 170
            sheet.paste(t, (x, y))
            d.text((x + 2, y + 152), cap, fill="black")
        sheet.save(OUT_DIR / "ornekler" / f"{sid}.jpg", quality=85)

    print(f"\nBitti. Şu dosyayı sohbete gönderin: {OUT_DIR / 'temizlik_sonuc.csv.gz'}", flush=True)
    print(f"Örnek sayfalar: {OUT_DIR / 'ornekler'}", flush=True)
    print("İndirilen paketleri (C:\\FungiLens\\veri) bir sonraki taramaya kadar saklayabilir ya da silebilirsiniz.")


if __name__ == "__main__":
    main()
