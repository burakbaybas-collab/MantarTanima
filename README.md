# FungiLens (MantarTanima)

Yapay Zekâ Destekli Mantar Tanımlama Sistemi — Doğayı Keşfet, Mantarları Tanı.

Fotoğraftan mantar türü tanıyan Android uygulaması (Kotlin + Jetpack Compose).

> **Uyarı:** Uygulamanın sonuçları yalnızca bilgi amaçlıdır. Bir mantarın yenilebilirliğine asla tek başına bu uygulamaya bakarak karar verilmemelidir.

## Durum

- [x] Kamera ile fotoğraf çekme / galeriden seçme
- [x] Sonuç ekranı: en olası 3 tür, güven oranı, yenilebilirlik etiketi, karışabilen türler
- [x] Tür bilgi tablosu (`app/src/main/assets/species.tsv`)
- [x] TensorFlow Lite model altyapısı (model eklenince otomatik devreye girer)
- [x] Tanıma modeli eğitim altyapısı (`training/`)

## Veri tabanı ve model

- Tür tablosu: `app/src/main/assets/species.tsv` (114 tür; Türkçe ad, yenilebilirlik, açıklama, karışabilen türler). Uygulama ve eğitim aynı tabloyu kullanır; GitHub'da dosyayı açıp ✏️ ile düzenleyebilirsiniz.
- Fotoğraflar: iNaturalist'teki doğrulanmış (research grade) gözlemler, yalnızca CC0 / CC-BY / CC-BY-NC lisanslı
- Kendi fotoğraflarınız: `training/own/<tür_id>/`
- Eğitim: GitHub → **Actions → Modeli eğit (mantar modeli temelli) → Run workflow**. Yeni model yalnızca mevcut modelden daha iyiyse uygulamaya eklenir.
- Sonuç raporu: `training/rapor.md`

Model yokken uygulama **demo modunda** çalışır: fotoğraf akışı çalışır, tahmin yapılmaz.

## Bilgisayarda çalıştırma

1. Android Studio → **File → New → Project from Version Control**
2. Adres: `https://github.com/burakbaybas-collab/MantarTanima.git`
3. Gradle senkronizasyonu bitince telefonu USB ile bağlayın (Geliştirici seçenekleri → USB hata ayıklama açık) ve ▶ **Run**.

## Proje yapısı

```
app/src/main/java/com/burakbaybas/mantartanima/
├── MainActivity.kt          Ekranlar arası geçiş
├── MainViewModel.kt         Fotoğrafı açma ve sınıflandırma
├── classifier/              Model arayüzü, demo ve TFLite uygulaması
├── data/SpeciesCatalog.kt   Türlerin Türkçe adı, yenilebilirliği, açıklaması
└── ui/                      Ana ekran, sonuç ekranı, tema
```

## Mantar Çiftliği oyunu

`oyun/mantar-ciftligi.html`: tarayıcıda çalışan mantar yetiştiriciliği simülasyonu. Kompost (Faz I), pastörizasyon (Faz II), misel ekimi, kuluçka odası, üretim odası (örtü toprağı ve şoklama), 3 flaş hasat ve satış aşamalarından oluşur. Dosyayı tarayıcıda açmanız yeterli.
