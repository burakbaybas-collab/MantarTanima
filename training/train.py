"""
Mantar tanıma modelini eğitir ve Android uygulaması için TFLite olarak dışa aktarır.

Kullanım:
    python training/train.py --data training/data --own training/own --out app/src/main/assets

Yöntem: ImageNet'te eğitilmiş MobileNetV2 üzerine transfer öğrenme.
  1. aşama: gövde dondurulur, yalnızca sınıflandırma katmanı eğitilir
  2. aşama: gövdenin son katmanları düşük öğrenme hızıyla ince ayarlanır

Çıktılar:
  model.tflite  -> girdi [1,224,224,3] float32 (0..1), çıktı tür olasılıkları
  labels.txt    -> çıktı sırasıyla tür kimlikleri
  training/rapor.md -> doğrulama başarımı (genel ve tür bazında)
"""
import argparse
import os
import random
import shutil
import tempfile

import numpy as np
import tensorflow as tf

IMG = 224
SEED = 42


def merge_sources(data_dir, own_dir, tmp, val_frac=0.15):
    """iNaturalist ve kendi fotoğraflarınızı birleştirir; her türü ayrı ayrı eğitim/doğrulama
    olarak böler (sembolik bağlarla). Doğrulama fotoğrafları eğitimde asla kullanılmaz."""
    rng = random.Random(SEED)
    classes = sorted(d for d in os.listdir(data_dir) if os.path.isdir(os.path.join(data_dir, d)))
    counts = {}
    for c in classes:
        files = []
        for src_root, prefix in ((data_dir, "inat"), (own_dir, "own")):
            src = os.path.join(src_root, c) if src_root else None
            if not src or not os.path.isdir(src):
                continue
            for f in sorted(os.listdir(src)):
                if f.lower().endswith((".jpg", ".jpeg", ".png")):
                    files.append((os.path.abspath(os.path.join(src, f)), f"{prefix}_{f}"))
        rng.shuffle(files)
        n_val = max(1, int(len(files) * val_frac))
        for i, (path, name) in enumerate(files):
            split = "val" if i < n_val else "train"
            dst = os.path.join(tmp, split, c)
            os.makedirs(dst, exist_ok=True)
            os.symlink(path, os.path.join(dst, name))
        counts[c] = len(files)
    return classes, counts


def build_model(num_classes):
    base = tf.keras.applications.MobileNetV2(
        input_shape=(IMG, IMG, 3), include_top=False, weights="imagenet"
    )
    base.trainable = False

    augment = tf.keras.Sequential([
        tf.keras.layers.RandomFlip("horizontal"),
        tf.keras.layers.RandomRotation(0.08),
        tf.keras.layers.RandomZoom(0.15),
        tf.keras.layers.RandomContrast(0.15),
    ], name="cogaltma")

    inputs = tf.keras.Input(shape=(IMG, IMG, 3))          # 0..1 aralığında (uygulama böyle verir)
    x = augment(inputs)
    x = tf.keras.layers.Rescaling(2.0, offset=-1.0)(x)    # MobileNetV2 -1..1 bekler
    x = base(x, training=False)
    x = tf.keras.layers.GlobalAveragePooling2D()(x)
    x = tf.keras.layers.Dropout(0.3)(x)
    outputs = tf.keras.layers.Dense(num_classes, activation="softmax")(x)
    return tf.keras.Model(inputs, outputs), base


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--data", default="training/data")
    ap.add_argument("--own", default="training/own")
    ap.add_argument("--out", default="app/src/main/assets")
    ap.add_argument("--epochs-head", type=int, default=8)
    ap.add_argument("--epochs-fine", type=int, default=6)
    ap.add_argument("--report", default="training/rapor.md")
    args = ap.parse_args()

    tmp = tempfile.mkdtemp()
    classes, counts = merge_sources(args.data, args.own if os.path.isdir(args.own) else None, tmp)
    print("Tür başına fotoğraf:", counts, flush=True)

    common = dict(labels="inferred", label_mode="int", class_names=classes,
                  image_size=(IMG, IMG), batch_size=32, seed=SEED)
    train_ds = tf.keras.utils.image_dataset_from_directory(os.path.join(tmp, "train"), **common)
    val_ds = tf.keras.utils.image_dataset_from_directory(os.path.join(tmp, "val"), shuffle=False, **common)

    scale = lambda x, y: (x / 255.0, y)
    train_ds = train_ds.map(scale).prefetch(tf.data.AUTOTUNE)
    val_ds = val_ds.map(scale).cache().prefetch(tf.data.AUTOTUNE)

    # Sınıf dengesizliğini telafi et
    total = sum(counts.values())
    class_weight = {i: total / (len(classes) * max(counts[c], 1)) for i, c in enumerate(classes)}

    model, base = build_model(len(classes))
    model.compile(optimizer=tf.keras.optimizers.Adam(1e-3),
                  loss="sparse_categorical_crossentropy", metrics=["accuracy"])
    print("1. aşama: sınıflandırıcı katman", flush=True)
    model.fit(train_ds, validation_data=val_ds, epochs=args.epochs_head,
              class_weight=class_weight, verbose=2)

    print("2. aşama: ince ayar", flush=True)
    base.trainable = True
    for layer in base.layers[:-40]:
        layer.trainable = False
    model.compile(optimizer=tf.keras.optimizers.Adam(1e-5),
                  loss="sparse_categorical_crossentropy", metrics=["accuracy"])
    model.fit(train_ds, validation_data=val_ds, epochs=args.epochs_fine,
              class_weight=class_weight, verbose=2,
              callbacks=[tf.keras.callbacks.EarlyStopping(patience=2, restore_best_weights=True)])

    # Doğrulama raporu
    y_true = np.concatenate([y.numpy() for _, y in val_ds])
    y_pred = np.argmax(model.predict(val_ds, verbose=0), axis=1)
    acc = float((y_true == y_pred).mean())
    lines = [
        "# Model doğrulama raporu", "",
        f"Genel doğruluk (tek fotoğraf): **%{acc * 100:.1f}**  ",
        f"Doğrulama fotoğrafı: {len(y_true)}, tür sayısı: {len(classes)}", "",
        "| Tür | Fotoğraf (toplam) | Doğruluk | En çok karıştığı tür |",
        "|---|---|---|---|",
    ]
    for i, c in enumerate(classes):
        mask = y_true == i
        if not mask.any():
            lines.append(f"| {c} | {counts[c]} | – | – |")
            continue
        cacc = float((y_pred[mask] == i).mean())
        wrong = y_pred[mask][y_pred[mask] != i]
        confused = classes[np.bincount(wrong).argmax()] if len(wrong) else "–"
        lines.append(f"| {c} | {counts[c]} | %{cacc * 100:.0f} | {confused} |")
    lines += ["", "Not: Uygulama üç fotoğrafın ortalamasını aldığı için gerçek kullanımda başarım genelde daha yüksektir."]
    with open(args.report, "w", encoding="utf-8") as f:
        f.write("\n".join(lines) + "\n")
    print("\n".join(lines), flush=True)

    # TFLite'a dönüştür (ağırlıklar 8-bit: ~4 kat küçük, hız/başarım kaybı çok az)
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    tflite = converter.convert()
    os.makedirs(args.out, exist_ok=True)
    with open(os.path.join(args.out, "model.tflite"), "wb") as f:
        f.write(tflite)
    with open(os.path.join(args.out, "labels.txt"), "w", encoding="utf-8") as f:
        f.write("\n".join(classes) + "\n")
    print(f"Model kaydedildi: {len(tflite) / 1e6:.1f} MB", flush=True)
    shutil.rmtree(tmp, ignore_errors=True)


if __name__ == "__main__":
    main()
