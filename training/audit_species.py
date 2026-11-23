"""Belirli türlerin eğitim fotoğraflarını denetler.

Her fotoğraf için: modelin tahmini, kendi etiketinin olasılığı ve renk ölçüleri
(kırmızı / sarı piksel oranı). Şüpheli fotoğraflar için temas sayfaları üretir.
Ayrıca test klasöründeki fotoğrafların ilk 5 tahminini yazar.
"""
import argparse
import csv
import colorsys
import os

import numpy as np
import timm
import torch
from PIL import Image, ImageDraw, ImageFile

ImageFile.LOAD_TRUNCATED_IMAGES = True


def color_fracs(im):
    small = np.asarray(im.convert("HSV").resize((96, 96)), dtype=np.float32) / 255.0
    h, s, v = small[..., 0] * 360, small[..., 1], small[..., 2]
    vivid = (s > 0.45) & (v > 0.35)
    red = vivid & ((h < 18) | (h > 340))
    yellow = vivid & (h >= 35) & (h < 65)
    return float(red.mean()), float(yellow.mean())


def sheet(rows, path, title, max_n=120):
    rows = rows[:max_n]
    if not rows:
        return
    t, cols = 180, 10
    n = len(rows)
    W, H = cols * t, ((n + cols - 1) // cols) * (t + 30) + 40
    canvas = Image.new("RGB", (W, H), "white")
    d = ImageDraw.Draw(canvas)
    d.text((8, 10), title, fill="black")
    for i, r in enumerate(rows):
        try:
            im = Image.open(r["path"]).convert("RGB")
            im.thumbnail((t, t))
        except Exception:
            continue
        x, y = (i % cols) * t, 40 + (i // cols) * (t + 30)
        canvas.paste(im, (x, y))
        d.text((x + 2, y + t + 2), f"{i}: {r['pred'][:16]} {r['pred_p']:.2f}", fill="black")
        d.text((x + 2, y + t + 15), f"kendi {r['own_p']:.2f} R{r['red']:.2f} Y{r['yellow']:.2f}", fill="gray")
    canvas.save(path, quality=85)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--ckpt", required=True)
    ap.add_argument("--dirs", nargs="+", required=True, help="Tür klasörlerini içeren kök klasörler")
    ap.add_argument("--species", nargs="+", required=True)
    ap.add_argument("--test", default=None)
    ap.add_argument("--out", default="denetim")
    args = ap.parse_args()
    os.makedirs(args.out, exist_ok=True)

    ck = torch.load(args.ckpt, map_location="cpu", weights_only=False)
    classes, img = ck["classes"], ck["img"]
    model = timm.create_model(ck["model"], pretrained=False, num_classes=len(classes))
    model.load_state_dict(ck["state_dict"])
    model.eval()
    mean = torch.tensor(ck["mean"]).view(3, 1, 1)
    std = torch.tensor(ck["std"]).view(3, 1, 1)
    torch.set_num_threads(os.cpu_count() or 2)

    def predict(paths):
        batch, ok = [], []
        for p in paths:
            try:
                im = Image.open(p).convert("RGB")
            except Exception:
                continue
            x = torch.from_numpy(np.asarray(im.resize((img, img)), dtype=np.float32) / 255.0).permute(2, 0, 1)
            batch.append((x - mean) / std)
            ok.append((p, im))
        if not batch:
            return []
        with torch.no_grad():
            pr = torch.softmax(model(torch.stack(batch)), dim=-1).numpy()
        return [(p, im, pr[i]) for i, (p, im) in enumerate(ok)]

    if args.test and os.path.isdir(args.test):
        with open(os.path.join(args.out, "test_tahminleri.txt"), "w") as f:
            files = sorted(os.listdir(args.test))
            allp = []
            for p, im, pr in predict([os.path.join(args.test, x) for x in files]):
                top = np.argsort(-pr)[:5]
                line = f"{os.path.basename(p)}: " + ", ".join(f"{classes[i]} {pr[i]:.2f}" for i in top)
                print(line, flush=True)
                f.write(line + "\n")
                allp.append((os.path.basename(p).split("_")[0], pr))
            for grp in sorted({g for g, _ in allp}):
                m = np.mean([pr for g, pr in allp if g == grp], axis=0)
                top = np.argsort(-m)[:5]
                line = f"ORTALAMA {grp}: " + ", ".join(f"{classes[i]} {m[i]:.2f}" for i in top)
                print(line, flush=True)
                f.write(line + "\n")

    summary = []
    for sid in args.species:
        own = classes.index(sid) if sid in classes else None
        paths = []
        for root in args.dirs:
            d = os.path.join(root, sid)
            if os.path.isdir(d):
                paths += [(root, os.path.join(d, f)) for f in sorted(os.listdir(d))]
        rows = []
        for k in range(0, len(paths), 32):
            chunk = paths[k:k + 32]
            src = {p: r for r, p in chunk}
            for p, im, pr in predict([p for _, p in chunk]):
                red, yel = color_fracs(im)
                top = int(np.argmax(pr))
                rows.append({
                    "path": p, "source": os.path.basename(src[p].rstrip("/")), "file": os.path.basename(p),
                    "pred": classes[top], "pred_p": float(pr[top]),
                    "own_p": float(pr[own]) if own is not None else -1, "red": red, "yellow": yel,
                })
        with open(os.path.join(args.out, f"{sid}.csv"), "w", newline="") as f:
            w = csv.DictWriter(f, fieldnames=list(rows[0].keys()) if rows else ["path"])
            w.writeheader()
            w.writerows(rows)
        wrong = sorted([r for r in rows if r["pred"] != sid], key=lambda r: -r["pred_p"])
        by_red = sorted(rows, key=lambda r: -r["red"])
        by_yel = sorted(rows, key=lambda r: -r["yellow"])
        sheet(wrong, os.path.join(args.out, f"{sid}_yanlis_tahmin.jpg"), f"{sid}: modelin baska tur dedigi fotograflar")
        sheet(by_red, os.path.join(args.out, f"{sid}_en_kirmizi.jpg"), f"{sid}: en kirmizi fotograflar")
        sheet(by_yel, os.path.join(args.out, f"{sid}_en_sari.jpg"), f"{sid}: en sari fotograflar")
        src_counts = {}
        for r in rows:
            src_counts[r["source"]] = src_counts.get(r["source"], 0) + 1
        confus = {}
        for r in wrong:
            confus[r["pred"]] = confus.get(r["pred"], 0) + 1
        line = (f"{sid}: {len(rows)} fotoğraf {src_counts}, yanlış tahmin {len(wrong)}, "
                f"kırmızı>%10: {sum(r['red'] > 0.10 for r in rows)}, sarı>%10: {sum(r['yellow'] > 0.10 for r in rows)}, "
                f"karıştığı: {sorted(confus.items(), key=lambda x: -x[1])[:6]}")
        print(line, flush=True)
        summary.append(line)
    with open(os.path.join(args.out, "ozet.txt"), "w") as f:
        f.write("\n".join(summary) + "\n")


if __name__ == "__main__":
    main()
