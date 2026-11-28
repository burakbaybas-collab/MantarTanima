"""Eğitim fotoğraflarını küçültür (uzun kenar en fazla 448 piksel) ve bozuk dosyaları siler.
Eğitim 299 pikselde yapıldığı için doğruluğu etkilemez; paket boyutunu ~4 kat azaltır."""
import os
import sys
from concurrent.futures import ProcessPoolExecutor

from PIL import Image

MAX = 448


def shrink(path):
    try:
        with Image.open(path) as im:
            if max(im.size) <= MAX * 1.25:  # zaten küçük: dokunma (yeniden sıkıştırma kaliteyi düşürür)
                return 1
            im.draft("RGB", (MAX, MAX))
            im = im.convert("RGB")
            im.thumbnail((MAX, MAX), Image.LANCZOS)
            im.save(path, "JPEG", quality=88)
        return 1
    except Exception:
        os.remove(path)
        return 0


def main(roots):
    files = [os.path.join(d, f) for root in roots for d, _, fs in os.walk(root) for f in fs
             if f.lower().endswith((".jpg", ".jpeg"))]
    print(f"{len(files)} fotoğraf kontrol ediliyor (büyükler {MAX} piksele küçültülür)...", flush=True)
    with ProcessPoolExecutor() as ex:
        ok = sum(ex.map(shrink, files, chunksize=256))
    print(f"{ok}/{len(files)} fotoğraf hazır", flush=True)


if __name__ == "__main__":
    main(sys.argv[1:])
