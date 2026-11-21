"""
Eğitimde kullanılan fotoğraflardan örnek galeri sayfaları üretir (GitHub'da görüntülenir).

Kullanım:
    python training/make_gallery.py --per-species 12

Çıktı: galeri/README.md (tür listesi) ve galeri/<id>.md (her tür için örnek fotoğraflar).
Fotoğraflar fetch_inat.py ile aynı sorgu ve sırayla alınır, yani eğitime giren ilk fotoğraflardır.
"""
import argparse
import csv
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
from fetch_inat import collect_photos, resolve_taxon  # noqa: E402

EDIBILITY_TR = {
    "EDIBLE": "🟢 Yenir",
    "EDIBLE_WITH_CAUTION": "🟡 Dikkatle yenir",
    "INEDIBLE": "⚪ Yenmez",
    "POISONOUS": "🟠 Zehirli",
    "DEADLY": "🔴 Ölümcül zehirli",
}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--species", default="app/src/main/assets/species.tsv")
    ap.add_argument("--labels", default="app/src/main/assets/labels.txt")
    ap.add_argument("--out", default="galeri")
    ap.add_argument("--per-species", type=int, default=12)
    args = ap.parse_args()

    with open(args.species, encoding="utf-8") as f:
        species = list(csv.DictReader(f, delimiter="\t"))
    trained = set()
    if os.path.exists(args.labels):
        trained = {l.strip() for l in open(args.labels, encoding="utf-8") if l.strip()}

    os.makedirs(args.out, exist_ok=True)
    index = [
        "# Eğitim fotoğrafları galerisi", "",
        f"Her türün sayfasında modeli eğitmek için kullanılan ilk {args.per_species} fotoğraf var "
        "(iNaturalist, doğrulanmış gözlemler). Model tür başına en fazla 500 fotoğrafla eğitildi.", "",
        "| Tür | Türkçe adı | Durum | Modelde |", "|---|---|---|---|",
    ]
    for sp in sorted(species, key=lambda s: s["taxon"]):
        in_model = "✅" if sp["id"] in trained else "❌ (yetersiz fotoğraf)"
        index.append(f"| [*{sp['taxon']}*]({sp['id']}.md) | {sp['turkish_name'] or '–'} | "
                     f"{EDIBILITY_TR.get(sp['edibility'], sp['edibility'])} | {in_model} |")
        try:
            taxon_id, name, _ = resolve_taxon(sp["taxon"])
            photos = collect_photos(taxon_id, args.per_species)
        except SystemExit:
            photos, name, taxon_id = [], sp["taxon"], None

        title = f"{sp['turkish_name']} (*{sp['taxon']}*)" if sp["turkish_name"] else f"*{sp['taxon']}*"
        page = [f"# {title}", "",
                f"**{EDIBILITY_TR.get(sp['edibility'], '')}** · Modelde: {in_model}", ""]
        if sp["description"]:
            page += [sp["description"], ""]
        if name != sp["taxon"]:
            page += [f"iNaturalist'teki güncel adı: *{name}*", ""]
        if taxon_id:
            page += [f"[iNaturalist'teki tüm gözlemler](https://www.inaturalist.org/observations?"
                     f"taxon_id={taxon_id}&quality_grade=research)", ""]
        page += ["[← Tüm türler](README.md)", ""]
        if not photos:
            page.append("Bu tür için fotoğraf bulunamadı.")
        for i in range(0, len(photos), 3):
            row = photos[i:i + 3]
            page.append("| " + " | ".join(f"[![]({p['url']})]({p['observation']})" for p in row) + " |")
            if i == 0:
                page.append("|" + "---|" * len(row))
        if photos:
            page += ["", "### Fotoğraf sahipleri", ""]
            page += [f"- {p['attribution']} ([gözlem]({p['observation']}))" for p in photos]
        with open(os.path.join(args.out, f"{sp['id']}.md"), "w", encoding="utf-8") as f:
            f.write("\n".join(page) + "\n")
        print(sp["id"], len(photos), flush=True)

    with open(os.path.join(args.out, "README.md"), "w", encoding="utf-8") as f:
        f.write("\n".join(index) + "\n")


if __name__ == "__main__":
    main()
