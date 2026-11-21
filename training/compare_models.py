"""
Yeni modeli uygulamadaki modelle adil biçimde karşılaştırır.

Tür sayısı değiştiğinde genel yüzde karşılaştırılamaz (daha çok tür = daha zor soru).
Bu yüzden iki modelin de bildiği ORTAK türlerde, tür başına doğrulukların ortalaması karşılaştırılır.

Kullanım:
    python training/compare_models.py --old training/rapor.md --new training/out/per_class.json
Çıkış kodu 0: yeni model daha iyi (veya eşit) → uygulamaya alınır; 1: alınmaz.
"""
import argparse
import json
import re
import sys

MIN_OVERALL = 0.50  # her durumda aranacak en düşük genel doğruluk


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--old", default="training/rapor.md")
    ap.add_argument("--new", default="training/out/per_class.json")
    ap.add_argument("--summary", default="training/out/karsilastirma.md")
    args = ap.parse_args()

    old = {}
    for line in open(args.old, encoding="utf-8"):
        m = re.match(r"\|\s*(\w+)\s*\|\s*\d+\s*\|\s*%(\d+)\s*\|", line)
        if m:
            old[m.group(1)] = int(m.group(2)) / 100
    new = json.load(open(args.new))
    common = sorted(set(old) & set(new["per_class"]))
    old_avg = sum(old[c] for c in common) / len(common) if common else 0.0
    new_avg = sum(new["per_class"][c] for c in common) / len(common) if common else 0.0
    ok = new["overall"] >= MIN_OVERALL and new_avg >= old_avg

    lines = [
        "## Eski modelle karşılaştırma", "",
        f"- Ortak tür sayısı: {len(common)} (eski modelde {len(old)}, yeni modelde {len(new['per_class'])})",
        f"- Ortak türlerde ortalama doğruluk: eski **%{old_avg * 100:.1f}** → yeni **%{new_avg * 100:.1f}**",
        f"- Yeni modelin tüm türlerdeki genel doğruluğu: %{new['overall'] * 100:.1f}",
        f"- Karar: {'uygulamaya alındı' if ok else 'uygulamaya ALINMADI (eski model korunuyor)'}",
    ]
    if common:
        diffs = sorted(((new["per_class"][c] - old[c], c) for c in common))
        lines += ["", "En çok gerileyen türler: " + ", ".join(f"{c} ({d * 100:+.0f})" for d, c in diffs[:5])]
        lines += ["En çok gelişen türler: " + ", ".join(f"{c} ({d * 100:+.0f})" for d, c in diffs[-5:][::-1])]
    text = "\n".join(lines) + "\n"
    open(args.summary, "w", encoding="utf-8").write(text)
    print(text)
    sys.exit(0 if ok else 1)


if __name__ == "__main__":
    main()
