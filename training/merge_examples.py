"""make_examples.py çıktılarını birleştirir: parcalar/<paket>/ -> assets/examples + assets/examples.tsv.

Bir tür birden çok pakette varsa yama paketi (veri-yama*) önceliklidir; çünkü yama o türün verisini yeniler.
"""
import csv
import os
import shutil
import sys

src, assets = sys.argv[1], sys.argv[2]
best = {}  # id -> (öncelik, paket, satırlar)
for pk in sorted(os.listdir(src)):
    tsv = os.path.join(src, pk, "ornekler.tsv")
    if not os.path.exists(tsv):
        continue
    by_id = {}
    with open(tsv, encoding="utf-8") as f:
        for r in csv.DictReader(f, delimiter="\t"):
            by_id.setdefault(r["id"], []).append(r)
    prio = 1 if pk.startswith("veri-yama") else 0
    for sid, rows in by_id.items():
        if sid not in best or prio > best[sid][0]:
            best[sid] = (prio, pk, rows)

out_dir = os.path.join(assets, "examples")
shutil.rmtree(out_dir, ignore_errors=True)
os.makedirs(out_dir)
lines = []
for sid in sorted(best):
    _, pk, rows = best[sid]
    for r in sorted(rows, key=lambda r: int(r["n"])):
        shutil.copy(os.path.join(src, pk, r["file"]), os.path.join(out_dir, r["file"]))
        att = " ".join(r["attribution"].replace('"', "").split())  # satır sonu/sekme temizliği
        lines.append([sid, r["file"], r["license"], att])
with open(os.path.join(assets, "examples.tsv"), "w", newline="", encoding="utf-8") as f:
    w = csv.writer(f, delimiter="\t", quoting=csv.QUOTE_NONE, escapechar="\\")
    w.writerow(["id", "file", "license", "attribution"])
    w.writerows(lines)
size = sum(os.path.getsize(os.path.join(out_dir, x)) for x in os.listdir(out_dir))
print(f"{len(best)} tür, {len(lines)} fotoğraf, {size / 1048576:.1f} MB")
