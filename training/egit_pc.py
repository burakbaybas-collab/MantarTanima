"""FungiLens modelini kendi bilgisayarında (Windows, NVIDIA GPU) eğit.

Ne yapar:
  1. GitHub'dan güncel eğitim kodunu, tür listesini ve temizlik listesini (haric.csv) indirir.
  2. temizlik.py'nin indirdiği paketleri (C:\\FungiLens\\veri) klasörlere açar
     ve yeniden indirilen 7 türün yamasını uygular.
  3. Büyük fotoğrafları bir kez 448 piksele küçültür (eğitim hızlanır).
  4. Eğitimi başlatır (384 piksel, RTX 5060 ile ~12-18 saat). Kapanırsa yeniden çalıştırın:
     kaldığı yerden devam eder.
  5. Sonuç: C:\\FungiLens\\egitim\\out384\\best_model.pt ve rapor_fungi.md

Çalıştırma (C:\\FungiLens\\sunucu klasöründe):
    venv\\Scripts\\python egit_pc.py --deneme     (yaklaşık 20 dk hız denemesi)
    venv\\Scripts\\python egit_pc.py              (tam eğitim)
"""
import io
import os
import shutil
import subprocess
import sys
import tarfile
import time
import zipfile
from pathlib import Path

import requests

REPO = "burakbaybas-collab/MantarTanima"
ROOT = Path(os.environ.get("FUNGILENS_KOK", r"C:\FungiLens"))
VERI = ROOT / "veri"
WORK = ROOT / "egitim"
CODE = WORK / "kod"
DATA = WORK / "data"
DATA_FT = WORK / "data_ft"
DATA_GBIF = WORK / "data_gbif"
OUT = WORK / "out384"
FOTOLAR = ROOT / "fotograflar"   # kendi fotoğraflarınız: fotograflar\Amanita virosa\*.jpg

MODEL = "hf-hub:BVRA/tf_efficientnet_b3.in1k_ft_df20_299"
IMG, BATCH, PER_SPECIES, PER_EPOCH, EPOCHS, HOURS = 384, 20, 1000, 450, 9, 60.0
DENEME = "--deneme" in sys.argv


class ConcatReader(io.RawIOBase):
    def __init__(self, paths):
        self.paths, self.f = list(paths), None

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


BASES = {"data": DATA, "data_ft": DATA_FT, "data_gbif": DATA_GBIF}


def extract(fileobj, label):
    """Paketi açar: data/... -> DATA, data_ft/... -> DATA_FT, data_gbif/... -> DATA_GBIF."""
    n, t0 = 0, time.time()
    with tarfile.open(fileobj=fileobj, mode="r|") as tf:
        for m in tf:
            if not m.isfile():
                continue
            parts = m.name.replace("\\", "/").split("/")
            if len(parts) < 3 or parts[0] not in BASES or ".." in parts:
                continue
            base = BASES[parts[0]]
            dst = base.joinpath(*parts[1:])
            dst.parent.mkdir(parents=True, exist_ok=True)
            with tf.extractfile(m) as src, open(dst, "wb") as out:
                shutil.copyfileobj(src, out)
            n += 1
            if n % 5000 == 0:
                print(f"\r  {label}: {n} dosya ({n / (time.time() - t0):.0f}/sn)   ", end="", flush=True)
    print(f"\r  {label}: {n} dosya açıldı                    ", flush=True)


def step_code():
    print("== 1) Güncel eğitim kodu indiriliyor ==", flush=True)
    r = requests.get(f"https://github.com/{REPO}/archive/refs/heads/main.zip", timeout=120)
    r.raise_for_status()
    if CODE.exists():
        shutil.rmtree(CODE)
    with zipfile.ZipFile(io.BytesIO(r.content)) as z:
        for info in z.infolist():
            name = info.filename.split("/", 1)[-1]
            if not (name.startswith("training/") or name == "app/src/main/assets/species.tsv"):
                continue
            if name.startswith(("training/data", "training/own/")) and not name.startswith("training/own/"):
                continue
            dst = CODE / name
            if info.is_dir():
                dst.mkdir(parents=True, exist_ok=True)
            else:
                dst.parent.mkdir(parents=True, exist_ok=True)
                dst.write_bytes(z.read(info))
    print("  tamam", flush=True)


def step_data():
    marker = WORK / ".veri_acildi"
    if marker.exists():
        print("== 2) Veri zaten açılmış, atlanıyor ==", flush=True)
    else:
        print("== 2) Eğitim paketleri açılıyor (20-40 dk sürebilir) ==", flush=True)
        for d in (DATA, DATA_FT):
            if d.exists():
                shutil.rmtree(d)
        inat = sorted(VERI.glob("veri-inat-*.tar"), key=lambda p: int(p.stem.split("-")[2]))
        ft = sorted(VERI.glob("veri-ft.tar.part-*"))
        if len(inat) < 12 or not ft:
            raise SystemExit("C:\\FungiLens\\veri içinde paketler eksik; önce temizlik.py'yi çalıştırın.")
        for p in inat:
            with open(p, "rb") as f:
                extract(f, p.name)
        extract(io.BufferedReader(ConcatReader(ft), 1 << 20), "FungiTastic")
        marker.write_text("ok")

    yama_marker = WORK / ".yama_uygulandi"
    r = requests.get(f"https://api.github.com/repos/{REPO}/releases/tags/egitim-verisi", timeout=60)
    r.raise_for_status()
    yama = [a for a in r.json()["assets"] if a["name"].startswith("veri-yama")]
    stamp = ",".join(f'{a["name"]}:{a["updated_at"]}' for a in yama)
    if yama and (not yama_marker.exists() or yama_marker.read_text() != stamp):
        print("  Yama paketi uygulanıyor (yeniden indirilen türler)", flush=True)
        for a in yama:
            data = requests.get(a["browser_download_url"], timeout=600).content
            with tarfile.open(fileobj=io.BytesIO(data)) as tf:
                species = {m.name.split("/")[1] for m in tf.getmembers() if m.name.startswith("data/") and m.name.count("/") >= 2}
            for s in species:
                shutil.rmtree(DATA / s, ignore_errors=True)
                print(f"    {s} yenilendi", flush=True)
            extract(io.BytesIO(data), a["name"])
        yama_marker.write_text(stamp)

    # GBIF fotoğrafları (gbif-data.yml ile hazırlanır); paket değişince yeniden indirilir
    gbif = [a for a in r.json()["assets"] if a["name"].startswith("gbif-veri-")]
    g_stamp = ",".join(f'{a["name"]}:{a["updated_at"]}' for a in gbif)
    g_marker = WORK / ".gbif_acildi"
    if gbif and (not g_marker.exists() or g_marker.read_text() != g_stamp):
        print(f"  GBIF fotoğrafları indiriliyor ({len(gbif)} paket)", flush=True)
        shutil.rmtree(DATA_GBIF, ignore_errors=True)
        for a in gbif:
            dst = VERI / a["name"]
            if not dst.exists() or dst.stat().st_size != a["size"]:
                with requests.get(a["browser_download_url"], stream=True, timeout=600) as resp:
                    resp.raise_for_status()
                    with open(dst, "wb") as f:
                        for chunk in resp.iter_content(1 << 20):
                            f.write(chunk)
            with open(dst, "rb") as f:
                extract(f, a["name"])
        g_marker.write_text(g_stamp)


def step_shrink():
    marker = WORK / ".kucultuldu"
    if marker.exists():
        return
    print("== Büyük fotoğraflar küçültülüyor (bir kez, 20-60 dk) ==", flush=True)
    dirs = [str(d) for d in (DATA, DATA_FT, DATA_GBIF) if d.exists()]
    rc = subprocess.call([sys.executable, "training/shrink_images.py", *dirs], cwd=CODE)
    if rc == 0:
        marker.write_text("ok")


def step_own():
    """C:\\FungiLens\\fotograflar altındaki tür klasörlerini eğitime ekler (en yüksek öncelikli kaynak)."""
    FOTOLAR.mkdir(parents=True, exist_ok=True)
    import csv
    with open(CODE / "app/src/main/assets/species.tsv", encoding="utf-8") as f:
        rows = list(csv.DictReader(f, delimiter="\t"))
    by_name = {}
    for r in rows:
        by_name[r["id"].lower()] = r["id"]
        by_name[r["taxon"].lower()] = r["id"]
        by_name[r["taxon"].lower().replace(" ", "_")] = r["id"]
    own = CODE / "training" / "own"
    added, unknown = 0, []
    for d in sorted(p for p in FOTOLAR.iterdir() if p.is_dir()):
        sid = by_name.get(d.name.strip().lower())
        if not sid:
            unknown.append(d.name)
            continue
        dst = own / sid
        dst.mkdir(parents=True, exist_ok=True)
        for f in d.iterdir():
            if f.suffix.lower() in (".jpg", ".jpeg", ".png"):
                shutil.copyfile(f, dst / f"pc_{f.name}")
                added += 1
    print(f"== Kendi fotoğraflarınız: {added} fotoğraf eklendi ==", flush=True)
    if unknown:
        print(f"  Tanınmayan klasör adları (Latince adı kontrol edin): {unknown}", flush=True)


def step_train():
    print("== 3) Eğitim başlıyor ==", flush=True)
    OUT.mkdir(parents=True, exist_ok=True)
    env = dict(os.environ, PYTHONUTF8="1", HF_HUB_DISABLE_SYMLINKS_WARNING="1")
    ckpt = OUT / "ckpt.pt"
    cmd = [
        sys.executable, "training/train_fungi.py",
        "--species", "app/src/main/assets/species.tsv",
        "--data", str(DATA), "--df20", str(DATA_FT), "--own", "training/own", "--gbif", str(DATA_GBIF),
        "--exclude", "training/haric.csv",
        "--model", MODEL, "--img", str(IMG), "--batch", str(BATCH),
        "--limit-per-class", str(PER_SPECIES), "--head-epochs", "1",
        "--max-epochs", str(EPOCHS), "--patience", "3", "--hours", str(HOURS), "--per-epoch", str(PER_EPOCH),
        "--out", str(OUT), "--report", str(OUT / "rapor_fungi.md"), "--ckpt", str(ckpt),
        "--final", "--no-export",
    ]
    if DENEME:
        cmd += ["--bench", "400"]
        print("  HIZ DENEMESİ: 400 adım eğitilip tahmini süre yazılacak", flush=True)
    elif ckpt.exists():
        cmd.append("--resume")
        print("  Önceki eğitimin kaldığı yerden devam ediliyor", flush=True)
    rc = subprocess.call(cmd, cwd=CODE, env=env)
    if rc != 0:
        raise SystemExit(f"Eğitim hata ile bitti (kod {rc}). Ekranın fotoğrafını gönderin.")
    if DENEME:
        print("\nDeneme bitti. Ekranın fotoğrafını gönderin; tam eğitim için --deneme olmadan çalıştırın.", flush=True)
        return
    print(f"\nBitti. Model: {OUT / 'best_model.pt'}", flush=True)
    print(f"Rapor: {OUT / 'rapor_fungi.md'}", flush=True)


if __name__ == "__main__":
    t0 = time.time()
    step_code()
    step_data()
    step_shrink()
    step_own()
    step_train()
    print(f"Toplam süre: {(time.time() - t0) / 3600:.1f} saat")
