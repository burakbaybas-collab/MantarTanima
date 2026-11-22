"""
GPU sunucusunda eğitilen modeli (best_model.pt) Android için TFLite'a dönüştürür.

Kullanım:
    python training/export_model.py --ckpt training/out/best_model.pt --out training/out

Girdi: [1, H, W, 3] float32 (0..1)   Çıktı: tür olasılıkları (softmax)
"""
import argparse
import os

import numpy as np
import timm
import torch
import torch.nn as nn


class ExportWrapper(nn.Module):
    def __init__(self, model, mean, std):
        super().__init__()
        self.model = model
        self.register_buffer("mean", torch.tensor(mean).view(1, 3, 1, 1))
        self.register_buffer("std", torch.tensor(std).view(1, 3, 1, 1))

    def forward(self, x):
        x = x.permute(0, 3, 1, 2)
        x = (x - self.mean) / self.std
        return torch.softmax(self.model(x), dim=-1)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--ckpt", default="training/out/best_model.pt")
    ap.add_argument("--out", default="training/out")
    args = ap.parse_args()

    ck = torch.load(args.ckpt, map_location="cpu", weights_only=False)
    classes, img = ck["classes"], ck["img"]
    print(f"Model: {ck['model']}, {len(classes)} tür, {img} piksel, doğruluk %{ck['accuracy'] * 100:.1f}", flush=True)
    model = timm.create_model(ck["model"], pretrained=False, num_classes=len(classes))
    model.load_state_dict(ck["state_dict"])
    wrapper = ExportWrapper(model.eval(), ck["mean"], ck["std"]).eval()

    import litert_torch as converter_lib
    sample = (torch.rand(1, img, img, 3),)
    edge, quantized = None, False
    try:
        import tensorflow as tf
        edge = converter_lib.convert(
            wrapper, sample, _ai_edge_converter_flags={"optimizations": [tf.lite.Optimize.DEFAULT]})
        quantized = True
    except Exception as e:
        print("Sıkıştırmasız aktarım:", repr(e)[:200], flush=True)
        edge = converter_lib.convert(wrapper, sample)

    # Doğrulama: TFLite çıktıları PyTorch çıktılarıyla uyuşuyor mu? (gerçekçi renk dağılımlı rastgele girdiler)
    torch.manual_seed(0)
    corrs, agree = [], 0
    for _ in range(30):
        base = torch.rand(1, 1, 1, 3)
        x = (base + 0.25 * torch.rand(1, img, img, 3)).clamp(0, 1)
        with torch.no_grad():
            ref = wrapper(x).numpy().reshape(-1)
        out = np.asarray(edge(x)).reshape(-1)
        corrs.append(np.corrcoef(ref, out)[0, 1])
        agree += int(ref.argmax() == out.argmax())
    corr = float(np.nanmean(corrs))
    print(f"TFLite/PyTorch: korelasyon {corr:.4f}, aynı en olası tür {agree}/30, sıkıştırılmış={quantized}", flush=True)
    if corr < (0.97 if quantized else 0.999):
        raise SystemExit("Dışa aktarılan model PyTorch modeliyle uyuşmuyor.")

    os.makedirs(args.out, exist_ok=True)
    path = os.path.join(args.out, "model.tflite")
    edge.export(path)
    with open(os.path.join(args.out, "labels.txt"), "w", encoding="utf-8") as f:
        f.write("\n".join(classes) + "\n")
    print(f"model.tflite: {os.path.getsize(path) / 1e6:.1f} MB", flush=True)


if __name__ == "__main__":
    main()
