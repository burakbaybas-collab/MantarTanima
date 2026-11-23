"""Ad listesindeki türlerin iNaturalist araştırma düzeyi gözlem sayılarını yazar (Avrupa+Türkiye ve dünya)."""
import csv, sys, time
import requests
sys.path.insert(0, "training")
import fetch_inat as fi

names = [l.strip() for l in open(sys.argv[1]) if l.strip()]
out = csv.writer(open(sys.argv[2], "w", newline=""))
out.writerow(["taxon", "inat_adi", "takson", "tr", "avrupa_tr", "dunya"])
for n in names:
    try:
        tid, nm, rank = fi.resolve_taxon(n)
    except SystemExit:
        out.writerow([n, "", "", 0, 0, 0]); continue
    def cnt(extra):
        p = {"taxon_id": tid, "quality_grade": "research", "photos": "true", "per_page": 0}
        p.update(extra)
        return fi.api_get("/observations", p)["total_results"]
    row = [n, nm, tid, cnt({"place_id": 7183}), cnt({"place_id": "97391,7183"}), cnt({})]
    print(*row, flush=True)
    out.writerow(row)
