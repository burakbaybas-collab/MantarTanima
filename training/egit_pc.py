"""FungiLens modelini kendi bilgisayarında (Windows, NVIDIA GPU) eğit.

Ne yapar:
  1. GitHub'dan güncel eğitim kodunu, tür listesini ve temizlik listesini (haric.csv) indirir.
  2. temizlik.py'nin indirdiği paketleri (C:\\FungiLens\\veri) klasörlere açar
     ve yeniden indirilen 7 türün yamasını uygular.
  3. Eğitimi başlatır (RTX 5060 ile ~7-9 saat). Kapanırsa yeniden çalıştırın:
     açılmış veriyi tekrar açmaz.
  4. Sonuç: C:\\FungiLens\\egitim\\out\\best_model.pt ve rapor_fungi.md

Çalıştırma (C:\\FungiLens\\sunucu klasöründe):
    venv\\Scripts\\python egit_pc.py
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
OUT = WORK / "out"

MODEL = "hf-hub:BVRA/tf_efficientnet_b3.in1k_ft_df20_299"
IMG, BATCH, PER_SPECIES, EPOCHS, HOURS = 299, 32, 1000, 12, 9.0


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


def extract(fileobj, label):
    """Paketi açar: data/... -> DATA, data_ft/... -> DATA_FT."""
    n, t0 = 0, time.time()
    with tarfile.open(fileobj=fileobj, mode="r|") as tf:
        for m in tf:
            if not m.isfile():
                continue
            parts = m.name.replace("\\", "/").split("/")
            if len(parts) < 3 or parts[0] not in ("data", "data_ft") or ".." in parts:
                continue
            base = DATA if parts[0] == "data" else DATA_FT
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


def step_train():
    print("== 3) Eğitim başlıyor ==", flush=True)
    OUT.mkdir(parents=True, exist_ok=True)
    env = dict(os.environ, PYTHONUTF8="1", HF_HUB_DISABLE_SYMLINKS_WARNING="1")
    ckpt = OUT / "ckpt.pt"
    cmd = [
        sys.executable, "training/train_fungi.py",
        "--species", "app/src/main/assets/species.tsv",
        "--data", str(DATA), "--df20", str(DATA_FT), "--own", "training/own",
        "--exclude", "training/haric.csv",
        "--model", MODEL, "--img", str(IMG), "--batch", str(BATCH),
        "--limit-per-class", str(PER_SPECIES), "--head-epochs", "1",
        "--max-epochs", str(EPOCHS), "--patience", "4", "--hours", str(HOURS),
        "--out", str(OUT), "--report", str(OUT / "rapor_fungi.md"), "--ckpt", str(ckpt),
        "--final", "--no-export",
    ]
    if ckpt.exists():
        cmd.append("--resume")
        print("  Önceki eğitimin kaldığı yerden devam ediliyor", flush=True)
    rc = subprocess.call(cmd, cwd=CODE, env=env)
    if rc != 0:
        raise SystemExit(f"Eğitim hata ile bitti (kod {rc}). Ekranın fotoğrafını gönderin.")
    print(f"\nBitti. Model: {OUT / 'best_model.pt'}", flush=True)
    print(f"Rapor: {OUT / 'rapor_fungi.md'}", flush=True)


if __name__ == "__main__":
    t0 = time.time()
    step_code()
    step_data()
    step_train()
    print(f"Toplam süre: {(time.time() - t0) / 3600:.1f} saat")
