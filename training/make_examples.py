"""Her tür için uygulamada gösterilecek 3 örnek fotoğraf seçer.

Bir türün iNaturalist fotoğraflarından (lisans bilgisi _kaynaklar.csv'de) rastgele adaylar alınır,
uygulamadaki model ile puanlanır; modelin o türe en yüksek olasılık verdiği, farklı gözlemlere ait
3 fotoğraf küçültülüp (WebP) kaydedilir.

Kullanım:
    python training/make_examples.py --data training/data --out out_ornek --kaynak inat-0
Çıktı: out_ornek/<id>_<n>.webp ve out_ornek/ornekler.tsv
"""
import argparse
import csv
import os
import random

import numpy as np
from PIL import Image

LICENSE_TEXT = {"cc0": "CC0", "cc-by": "CC BY", "cc-by-nc": "CC BY-NC", "cc-by-sa": "CC BY-SA",
                "cc-by-nc-sa": "CC BY-NC-SA"}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--data", required=True)
    ap.add_argument("--model", default="app/src/main/assets/model.tflite")
    ap.add_argument("--labels", default="app/src/main/assets/labels.txt")
    ap.add_argument("--out", required=True)
    ap.add_argument("--kaynak", default="", help="Birleştirmede öncelik için paket adı")
    ap.add_argument("--per", type=int, default=3)
    ap.add_argument("--cand", type=int, default=24)
    ap.add_argument("--size", type=int, default=256)
    ap.add_argument("--quality", type=int, default=62)
    args = ap.parse_args()

    from ai_edge_litert.interpreter import Interpreter
    interp = Interpreter(model_path=args.model, num_threads=os.cpu_count() or 4)
    interp.allocate_tensors()
    inp = interp.get_input_details()[0]
    out = interp.get_output_details()[0]
    _, h, w, _ = inp["shape"]
    labels = [l.strip() for l in open(args.labels, encoding="utf-8") if l.strip()]
    index = {l: i for i, l in enumerate(labels)}

    def probs(img):
        x = np.asarray(img.resize((w, h), Image.BILINEAR), dtype=np.float32)[None] / 255.0
        interp.set_tensor(inp["index"], x)
        interp.invoke()
        v = interp.get_tensor(out["index"])[0].astype(np.float64)
        if not (v.min() >= 0 and abs(v.sum() - 1) < 0.02):
            v = np.exp(v - v.max())
            v /= v.sum()
        return v

    os.makedirs(args.out, exist_ok=True)
    rows = []
    for sp in sorted(os.listdir(args.data)):
        folder = os.path.join(args.data, sp)
        src = os.path.join(folder, "_kaynaklar.csv")
        if sp not in index or not os.path.exists(src):
            continue
        meta = []
        with open(src, encoding="utf-8") as f:
            for r in csv.DictReader(f):
                p = os.path.join(folder, f"{r['photo_id']}.jpg")
                if r.get("license") in LICENSE_TEXT and os.path.exists(p):
                    meta.append((p, r))
        if not meta:
            continue
        rnd = random.Random(sp)
        rnd.shuffle(meta)
        # Önce farklı gözlemlerden adaylar
        cands, seen_obs = [], set()
        for p, r in meta:
            if r["observation"] not in seen_obs:
                cands.append((p, r)); seen_obs.add(r["observation"])
            if len(cands) >= args.cand:
                break
        scored = []
        for p, r in cands:
            try:
                img = Image.open(p).convert("RGB")
            except Exception:
                continue
            if min(img.size) < 200:
                continue
            v = probs(img)
            k = index[sp]
            scored.append((float(v[k]), int(v.argmax()) == k, p, r))
        scored.sort(key=lambda t: (t[1], t[0]), reverse=True)
        n = 0
        for score, top1, p, r in scored:
            if n >= args.per:
                break
            n += 1
            img = Image.open(p).convert("RGB")
            img.thumbnail((args.size, args.size), Image.LANCZOS)
            name = f"{sp}_{n}.webp"
            img.save(os.path.join(args.out, name), "WEBP", quality=args.quality, method=6)
            rows.append({"id": sp, "n": n, "file": name, "score": f"{score:.3f}", "top1": int(top1),
                         "license": LICENSE_TEXT[r["license"]], "attribution": r.get("attribution", ""),
                         "observation": r.get("observation", ""), "kaynak": args.kaynak})
        print(f"{sp:<30} {n} örnek (en iyi %{scored[0][0] * 100:.0f})" if scored else f"{sp:<30} aday yok", flush=True)

    with open(os.path.join(args.out, "ornekler.tsv"), "w", newline="", encoding="utf-8") as f:
        wr = csv.DictWriter(f, fieldnames=["id", "n", "file", "score", "top1", "license", "attribution",
                                           "observation", "kaynak"], delimiter="\t")
        wr.writeheader()
        wr.writerows(rows)
    print(f"Toplam {len(rows)} fotoğraf, {len({r['id'] for r in rows})} tür", flush=True)


if __name__ == "__main__":
    main()
