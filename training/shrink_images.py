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
            im = im.convert("RGB")
            if max(im.size) > MAX:
                im.thumbnail((MAX, MAX), Image.LANCZOS)
            im.save(path, "JPEG", quality=85, optimize=True)
        return 1
    except Exception:
        os.remove(path)
        return 0


def main(root):
    files = [os.path.join(d, f) for d, _, fs in os.walk(root) for f in fs if f.lower().endswith((".jpg", ".jpeg"))]
    with ProcessPoolExecutor() as ex:
        ok = sum(ex.map(shrink, files, chunksize=64))
    print(f"{ok}/{len(files)} fotoğraf küçültüldü")


if __name__ == "__main__":
    main(sys.argv[1])
