# MantarTanima

Fotoğraftan mantar türü tanıyan Android uygulaması (Kotlin + Jetpack Compose).

> **Uyarı:** Uygulamanın sonuçları yalnızca bilgi amaçlıdır. Bir mantarın yenilebilirliğine asla tek başına bu uygulamaya bakarak karar verilmemelidir.

## Durum

- [x] Kamera ile fotoğraf çekme / galeriden seçme
- [x] Sonuç ekranı: en olası 3 tür, güven oranı, yenilebilirlik etiketi, karışabilen türler
- [x] Tür bilgi kataloğu (`app/src/main/java/.../data/SpeciesCatalog.kt`)
- [x] TensorFlow Lite model altyapısı (model eklenince otomatik devreye girer)
- [x] Tanıma modeli eğitim altyapısı (`training/`)

## Veri tabanı ve model

- Tür listesi: `training/species.csv` (17 tür; yenen türler ve karışabilen zehirli türler)
- Fotoğraflar: iNaturalist'teki doğrulanmış (research grade) gözlemler, yalnızca CC0 / CC-BY / CC-BY-NC lisanslı
- Kendi fotoğraflarınız: `training/own/<tür_id>/`
- Eğitim: GitHub → **Actions → Modeli eğit → Run workflow**. `species.csv` veya `training/own/` değişince otomatik başlar.
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
