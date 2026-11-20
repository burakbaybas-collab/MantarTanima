# Model dosyaları

Bu klasöre iki dosya konduğunda uygulama gerçek modeli kullanır:

- `model.tflite` — görüntü sınıflandırma modeli
  - Girdi: `[1, 224, 224, 3]`, float32, piksel değerleri 0–1 aralığında
  - Çıktı: `[1, N]`, float32 (her tür için olasılık veya logit)
- `labels.txt` — her satırda bir tür, çıktı sırasıyla aynı. Satırdaki ad,
  `data/SpeciesCatalog.kt` içindeki `id` ile eşleşirse tür bilgi kartı gösterilir
  (örn. `amanita_phalloides`).

Dosyalar yoksa uygulama "demo" modunda çalışır ve tahmin yapmaz.
