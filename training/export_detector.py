"""
"Fotoğrafta mantar var mı?" kontrolü için ImageNet'te eğitilmiş MobileNetV2'yi TFLite'a aktarır.

ImageNet'in 1000 sınıfı arasında mantar sınıfları da vardır (mantar, agaric, bolet, gyromitra,
stinkhorn, earthstar, hen-of-the-woods, coral fungus). Uygulama bu sınıfların toplam olasılığına
bakarak boş oda, insan, eşya gibi mantar olmayan fotoğrafları ayırır.

Kullanım:
    python training/export_detector.py --out app/src/main/assets
Çıktı: detector.tflite (girdi [1,224,224,3] float32 0..1, çıktı 1000 ImageNet olasılığı)
"""
import argparse
import os

import tensorflow as tf


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default="app/src/main/assets")
    args = ap.parse_args()

    base = tf.keras.applications.MobileNetV2(input_shape=(224, 224, 3), include_top=True, weights="imagenet")
    inputs = tf.keras.Input(shape=(224, 224, 3))                 # 0..1 (uygulama böyle verir)
    x = tf.keras.layers.Rescaling(2.0, offset=-1.0)(inputs)     # MobileNetV2 -1..1 bekler
    model = tf.keras.Model(inputs, base(x))

    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    tflite = converter.convert()
    os.makedirs(args.out, exist_ok=True)
    with open(os.path.join(args.out, "detector.tflite"), "wb") as f:
        f.write(tflite)
    print(f"detector.tflite: {len(tflite) / 1e6:.1f} MB")


if __name__ == "__main__":
    main()
