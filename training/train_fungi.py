"""
Mantar fotoğraflarıyla önceden eğitilmiş bir modeli (Danish Fungi 2020, 1.604 tür) temel alarak
kendi türlerimiz için ince ayar yapar ve Android için TFLite olarak dışa aktarır.

Temel model: BVRA/tf_efficientnet_b0.in1k_ft_df20_224 (Hugging Face, lisans CC-BY-NC-4.0)
  Picek ve ark., "Danish Fungi 2020" / "FungiTastic" (arXiv:2408.13632)

Kullanım:
    python training/train_fungi.py --hours 3.5

Çıktılar (yalnızca mevcut modelden daha iyiyse uygulamaya kopyalanır, workflow karar verir):
  training/out/model.tflite, training/out/labels.txt, training/rapor_fungi.md
  training/out/accuracy.txt (genel doğruluk, 0..1)
"""
import argparse
import csv
import os
import random
import tempfile
import time

import numpy as np
import timm
import torch
import torch.nn as nn
from timm.data import resolve_data_config
from torch.utils.data import DataLoader
from PIL import Image, ImageFile
from torchvision import datasets, transforms

# Yarım inmiş / bozuk birkaç fotoğraf yüzünden eğitim durmasın
ImageFile.LOAD_TRUNCATED_IMAGES = True


def safe_loader(path):
    try:
        with open(path, "rb") as f:
            return Image.open(f).convert("RGB")
    except Exception:
        return Image.new("RGB", (320, 320), (127, 127, 127))

BASE_MODEL = "hf-hub:BVRA/tf_efficientnet_b0.in1k_ft_df20_224"
IMG = 224
SEED = 42
MIN_PHOTOS = 40


def merge_sources(species_ids, data_dir, own_dir, tmp, val_frac=0.15, limit=None, df20_dir=None):
    """Her türü ayrı ayrı eğitim/doğrulama olarak böler (train.py ile aynı bölme)."""
    rng = random.Random(SEED)
    classes, counts, skipped = [], {}, []
    for c in sorted(species_ids):
        by_src = {"inat": [], "df20": [], "own": []}
        for src_root, prefix in ((data_dir, "inat"), (df20_dir, "df20"), (own_dir, "own")):
            src = os.path.join(src_root, c) if src_root else None
            if not src or not os.path.isdir(src):
                continue
            for f in sorted(os.listdir(src)):
                if f.lower().endswith((".jpg", ".jpeg", ".png")):
                    by_src[prefix].append((os.path.abspath(os.path.join(src, f)), f"{prefix}_{f}"))
        total = sum(len(v) for v in by_src.values())
        if total < MIN_PHOTOS:
            skipped.append(f"{c} ({total})")
            continue
        classes.append(c)
        # Öncelik: kendi fotoğraflarınız > uzman doğrulamalı (FungiTastic/DF20) > iNaturalist
        for v in by_src.values():
            rng.shuffle(v)
        own, ft, inat = by_src["own"], by_src["df20"], by_src["inat"]
        if limit:
            # Uzman fotoğrafları en fazla %70; kalanı iNaturalist'ten (çeşitlilik için),
            # iNaturalist yetmezse yine uzman fotoğraflarıyla tamamlanır.
            room = max(0, limit - len(own))
            n_ft = min(len(ft), int(room * 0.7))
            n_in = min(len(inat), room - n_ft)
            n_ft = min(len(ft), room - n_in)
            files = own + ft[:n_ft] + inat[:n_in]
        else:
            files = own + ft + inat
        rng.shuffle(files)
        n_val = max(1, int(len(files) * val_frac))
        for i, (path, name) in enumerate(files):
            split = "val" if i < n_val else "train"
            dst = os.path.join(tmp, split, c)
            os.makedirs(dst, exist_ok=True)
            os.symlink(path, os.path.join(dst, name))
        counts[c] = len(files)
    return classes, counts, skipped


class ExportWrapper(nn.Module):
    """Uygulamanın verdiği girdiyi (NHWC, 0..1) modele uygun hale getirir, olasılık döndürür."""

    def __init__(self, model, mean, std):
        super().__init__()
        self.model = model
        self.register_buffer("mean", torch.tensor(mean).view(1, 3, 1, 1))
        self.register_buffer("std", torch.tensor(std).view(1, 3, 1, 1))

    def forward(self, x):
        x = x.permute(0, 3, 1, 2)
        x = (x - self.mean) / self.std
        return torch.softmax(self.model(x), dim=-1)


DEVICE = torch.device("cuda" if torch.cuda.is_available() else "cpu")


def evaluate(model, loader, n_classes):
    model.eval()
    ys, ps = [], []
    with torch.no_grad():
        for x, y in loader:
            with torch.autocast(DEVICE.type, enabled=DEVICE.type == "cuda"):
                out = model(x.to(DEVICE, non_blocking=True))
            ps.append(out.argmax(1).cpu().numpy())
            ys.append(y.numpy())
    y, p = np.concatenate(ys), np.concatenate(ps)
    return float((y == p).mean()), y, p


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--species", default="app/src/main/assets/species.tsv")
    ap.add_argument("--data", default="training/data")
    ap.add_argument("--own", default="training/own")
    ap.add_argument("--df20", default="training/data_df20")
    ap.add_argument("--out", default="training/out")
    ap.add_argument("--report", default="training/rapor_fungi.md")
    ap.add_argument("--hours", type=float, default=3.5, help="İnce ayar için en fazla süre")
    ap.add_argument("--head-epochs", type=int, default=1)
    ap.add_argument("--max-epochs", type=int, default=4)
    ap.add_argument("--limit-per-class", type=int, default=None, help="Hızlı deneme için")
    ap.add_argument("--ckpt", default="training/out/ckpt.pt", help="Aşamalar arası devir dosyası")
    ap.add_argument("--resume", action="store_true", help="Önceki aşamanın kaldığı yerden devam et")
    ap.add_argument("--final", action="store_true", help="Son aşama: eğitimi bitir ve modeli dışa aktar")
    ap.add_argument("--patience", type=int, default=3, help="Bu kadar epoch iyileşme olmazsa dur")
    ap.add_argument("--model", default=BASE_MODEL, help="timm / Hugging Face temel model adı")
    ap.add_argument("--img", type=int, default=224, help="Girdi çözünürlüğü (piksel)")
    ap.add_argument("--batch", type=int, default=48)
    ap.add_argument("--lr", type=float, default=1.5e-4, help="Tam ince ayar öğrenme hızı")
    ap.add_argument("--no-export", action="store_true",
                    help="TFLite dışa aktarmayı atla (GPU sunucusunda; dışa aktarma export_model.py ile yapılır)")
    args = ap.parse_args()

    torch.manual_seed(SEED)
    ncpu = len(os.sched_getaffinity(0)) if hasattr(os, 'sched_getaffinity') else (os.cpu_count() or 4)
    torch.set_num_threads(min(ncpu, 16))
    start = time.time()

    with open(args.species, encoding="utf-8") as f:
        species_ids = [row["id"] for row in csv.DictReader(f, delimiter="\t")]
    use_df20 = os.path.isdir(args.df20)
    if args.resume and os.path.exists(args.ckpt):
        # Devam ederken önceki aşamayla aynı tür listesi ve aynı veri kaynakları kullanılır;
        # aksi halde eğitim/doğrulama bölmesi değişir ve sonuçlar karşılaştırılamaz.
        pre = torch.load(args.ckpt, map_location="cpu", weights_only=False)
        species_ids = pre["classes"]
        use_df20 = use_df20 and pre.get("used_df20", False)
        print(f"Devam modu: {len(species_ids)} tür, DF20 {'açık' if use_df20 else 'kapalı'}", flush=True)
        del pre
    tmp = tempfile.mkdtemp()
    classes, counts, skipped = merge_sources(
        species_ids, args.data, args.own if os.path.isdir(args.own) else None, tmp,
        limit=args.limit_per_class, df20_dir=args.df20 if use_df20 else None)
    print(f"{len(classes)} tür, {sum(counts.values())} fotoğraf; atlanan: {skipped}", flush=True)

    IMG = args.img
    print(f"Cihaz: {DEVICE}, model: {args.model}, çözünürlük: {IMG}", flush=True)
    model = timm.create_model(args.model, pretrained=True)
    cfg = resolve_data_config({}, model=model)
    mean, std = list(cfg["mean"]), list(cfg["std"])
    print("Temel model normalizasyonu:", mean, std, flush=True)
    model.reset_classifier(len(classes))
    model.to(DEVICE)

    norm = transforms.Normalize(mean, std)
    train_tf = transforms.Compose([
        transforms.RandomResizedCrop(IMG, scale=(0.45, 1.0)),
        transforms.RandomHorizontalFlip(),
        transforms.RandomRotation(15),
        transforms.ColorJitter(0.25, 0.25, 0.2, 0.03),
        transforms.ToTensor(), norm,
    ])
    # Uygulama fotoğrafı kırpmadan 224x224'e küçültür; doğrulama da aynısını yapar
    val_tf = transforms.Compose([transforms.Resize((IMG, IMG)), transforms.ToTensor(), norm])

    train_ds = datasets.ImageFolder(os.path.join(tmp, "train"), transform=train_tf, loader=safe_loader)
    val_ds = datasets.ImageFolder(os.path.join(tmp, "val"), transform=val_tf, loader=safe_loader)
    assert train_ds.classes == classes == val_ds.classes
    workers = max(1, min(ncpu - 1, 12))
    pin = DEVICE.type == "cuda"
    train_dl = DataLoader(train_ds, batch_size=args.batch, shuffle=True, num_workers=workers,
                          persistent_workers=True, pin_memory=pin, drop_last=True)
    val_dl = DataLoader(val_ds, batch_size=args.batch * 2, shuffle=False, num_workers=workers,
                        persistent_workers=True, pin_memory=pin)
    scaler = torch.amp.GradScaler(DEVICE.type, enabled=DEVICE.type == "cuda")

    total = sum(counts.values())
    weights = torch.tensor([total / (len(classes) * counts[c]) for c in classes], dtype=torch.float32)
    loss_fn = nn.CrossEntropyLoss(weight=weights.to(DEVICE), label_smoothing=0.1)

    def make_opt(phase):
        head = phase == "head"
        for name, p in model.named_parameters():
            p.requires_grad = (not head) or name.startswith("classifier")
        o = torch.optim.AdamW([p for p in model.parameters() if p.requires_grad],
                              lr=1e-3 if head else args.lr, weight_decay=1e-4)
        n_epochs = args.head_epochs if head else args.max_epochs - args.head_epochs
        return o, torch.optim.lr_scheduler.CosineAnnealingLR(o, T_max=max(1, len(train_dl) * n_epochs))

    start_epoch, best_acc, best_state, since_best, done = 0, -1.0, None, 0, False
    opt = sched = None
    phase = None
    if args.resume and os.path.exists(args.ckpt):
        ck = torch.load(args.ckpt, map_location="cpu", weights_only=False)
        assert ck["classes"] == classes, "Tür listesi aşamalar arasında değişmiş"
        model.load_state_dict(ck["model"])
        model.to(DEVICE)
        start_epoch, best_acc, best_state = ck["epoch"], ck["best_acc"], ck["best_state"]
        since_best, done, phase = ck["since_best"], ck["done"], ck["phase"]
        opt, sched = make_opt(phase)
        opt.load_state_dict(ck["opt"])
        sched.load_state_dict(ck["sched"])
        print(f"Devam: epoch {start_epoch}, en iyi doğrulama %{best_acc * 100:.1f}", flush=True)

    def save_ckpt(next_epoch):
        os.makedirs(os.path.dirname(args.ckpt), exist_ok=True)
        torch.save({"classes": classes, "used_df20": use_df20, "model": model.state_dict(), "epoch": next_epoch,
                    "best_acc": best_acc, "best_state": best_state, "since_best": since_best,
                    "done": done, "phase": phase, "opt": opt.state_dict(), "sched": sched.state_dict()},
                   args.ckpt)

    budget = args.hours * 3600
    epoch = start_epoch
    while not done and epoch < args.max_epochs:
        want = "head" if epoch < args.head_epochs else "full"
        if want != phase:
            phase = want
            opt, sched = make_opt(phase)
        model.train()
        t0, seen, run_loss = time.time(), 0, 0.0
        for x, y in train_dl:
            x, y = x.to(DEVICE, non_blocking=True), y.to(DEVICE, non_blocking=True)
            opt.zero_grad(set_to_none=True)
            with torch.autocast(DEVICE.type, enabled=DEVICE.type == "cuda"):
                loss = loss_fn(model(x), y)
            scaler.scale(loss).backward()
            scaler.step(opt)
            scaler.update()
            sched.step()
            seen += len(y)
            run_loss += loss.item() * len(y)
        acc, _, _ = evaluate(model, val_dl, len(classes))
        dt = time.time() - t0
        print(f"Epoch {epoch + 1}/{args.max_epochs} ({'sınıflandırıcı' if phase == 'head' else 'tam ince ayar'}): "
              f"kayıp {run_loss / seen:.3f}, doğrulama %{acc * 100:.1f}, {dt / 60:.1f} dk", flush=True)
        if acc > best_acc:
            best_acc, best_state, since_best = acc, {k: v.detach().cpu().clone() for k, v in model.state_dict().items()}, 0
        else:
            since_best += 1
            if phase == "full" and since_best >= args.patience:
                print("Doğruluk artmıyor; eğitim tamamlandı.", flush=True)
                done = True
        epoch += 1
        save_ckpt(epoch)
        if time.time() - start + dt * 1.15 > budget:
            print("Bu aşamanın süresi doldu.", flush=True)
            break
    if epoch >= args.max_epochs:
        done = True
    if opt is not None:
        save_ckpt(epoch)

    if not args.final:
        print(f"Aşama bitti (epoch {epoch}, en iyi %{best_acc * 100:.1f}); devir dosyası kaydedildi.", flush=True)
        return

    model.load_state_dict(best_state)
    acc, y_true, y_pred = evaluate(model, val_dl, len(classes))

    lines = [
        "# Model doğrulama raporu (mantar modeli temelli)", "",
        f"Temel model: `{args.model.replace('hf-hub:', '')}` (Danish Fungi 2020, CC-BY-NC-4.0), {IMG} piksel  ",
        f"Genel doğruluk (tek fotoğraf): **%{acc * 100:.1f}**  ",
        f"Doğrulama fotoğrafı: {len(y_true)}, tür sayısı: {len(classes)}", "",
        "| Tür | Fotoğraf (toplam) | Doğruluk | En çok karıştığı tür |", "|---|---|---|---|",
    ]
    for i, c in enumerate(classes):
        mask = y_true == i
        cacc = float((y_pred[mask] == i).mean()) if mask.any() else 0.0
        wrong = y_pred[mask][y_pred[mask] != i]
        confused = classes[np.bincount(wrong).argmax()] if len(wrong) else "–"
        lines.append(f"| {c} | {counts[c]} | %{cacc * 100:.0f} | {confused} |")
    if skipped:
        lines += ["", "Az fotoğraf nedeniyle modele alınmayan türler: " + ", ".join(skipped)]
    with open(args.report, "w", encoding="utf-8") as f:
        f.write("\n".join(lines) + "\n")
    import json
    os.makedirs(args.out, exist_ok=True)
    per_class = {c: float((y_pred[y_true == i] == i).mean()) for i, c in enumerate(classes) if (y_true == i).any()}
    with open(os.path.join(args.out, "per_class.json"), "w") as f:
        json.dump({"overall": acc, "per_class": per_class}, f)
    print("\n".join(lines[:6]), flush=True)

    # En iyi model ağırlıkları (dışa aktarma ayrı makinede de yapılabilsin diye)
    os.makedirs(args.out, exist_ok=True)
    torch.save({"state_dict": {k: v.cpu() for k, v in model.state_dict().items()}, "classes": classes,
                "model": args.model, "img": IMG, "mean": mean, "std": std, "accuracy": acc},
               os.path.join(args.out, "best_model.pt"))
    with open(os.path.join(args.out, "labels.txt"), "w", encoding="utf-8") as f:
        f.write("\n".join(classes) + "\n")
    with open(os.path.join(args.out, "accuracy.txt"), "w") as f:
        f.write(f"{acc:.4f}\n")
    if args.no_export:
        print("Dışa aktarma atlandı (best_model.pt kaydedildi).", flush=True)
        return
    model.cpu()

    # --- TFLite'a dışa aktar ---
    os.makedirs(args.out, exist_ok=True)
    wrapper = ExportWrapper(model.eval(), mean, std).eval()
    sample = (torch.rand(1, IMG, IMG, 3),)
    import litert_torch as converter_lib  # eski adı: ai_edge_torch
    try:
        import tensorflow as tf
        edge = converter_lib.convert(
            wrapper, sample, _ai_edge_converter_flags={"optimizations": [tf.lite.Optimize.DEFAULT]})
        print("TFLite: ağırlıklar 8-bit sıkıştırıldı", flush=True)
    except Exception as e:  # sıkıştırma desteklenmezse tam hassasiyetle aktar
        print("Sıkıştırmasız aktarım:", e, flush=True)
        edge = converter_lib.convert(wrapper, sample)
    path = os.path.join(args.out, "model.tflite")
    edge.export(path)

    # Dışa aktarılan model PyTorch ile aynı sonucu veriyor mu? (doğrulama örneklerinden 200 tanesi)
    agree, checked = 0, 0
    raw_tf = transforms.Compose([transforms.Resize((IMG, IMG)), transforms.ToTensor()])
    for i in range(0, len(val_ds), max(1, len(val_ds) // 200)):
        img_path, _ = val_ds.samples[i]
        x = raw_tf(datasets.folder.default_loader(img_path)).permute(1, 2, 0).unsqueeze(0)
        with torch.no_grad():
            ref = wrapper(x).argmax(1).item()
        out = np.asarray(edge(x)).reshape(-1)
        agree += int(out.argmax() == ref)
        checked += 1
    print(f"TFLite/PyTorch uyumu: {agree}/{checked}", flush=True)
    if agree < 0.97 * checked:
        raise SystemExit("Dışa aktarılan model PyTorch modeliyle uyuşmuyor; uygulamaya eklenmeyecek.")

    with open(os.path.join(args.out, "labels.txt"), "w", encoding="utf-8") as f:
        f.write("\n".join(classes) + "\n")
    with open(os.path.join(args.out, "accuracy.txt"), "w") as f:
        f.write(f"{acc:.4f}\n")
    print(f"Model: {os.path.getsize(path) / 1e6:.1f} MB, toplam süre {(time.time() - start) / 60:.0f} dk", flush=True)


if __name__ == "__main__":
    main()
