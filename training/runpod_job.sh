#!/usr/bin/env bash
# RunPod GPU sunucusunda çalışan eğitim işi.
# GitHub iş akışı (train-gpu.yml) sunucuyu bu betikle başlatır. Ortam değişkenleri:
#   REPO, RUN_TAG, GH_TOKEN, RP_KEY, RUNPOD_POD_ID (RunPod verir),
#   PER_SPECIES, MAX_EPOCHS, MODEL, IMG, BATCH, HOURS, LIMIT_SPECIES (deneme için)
# Sonuçlar GitHub'da "$RUN_TAG" adlı geçici bir sürüme yüklenir; iş bitince sunucu kendini kapatır.

LOG=/work/egitim.log
mkdir -p /work
exec > >(tee -a "$LOG") 2>&1

API="https://api.github.com/repos/$REPO"
AUTH="Authorization: Bearer $GH_TOKEN"

upload() {  # upload <dosya> <ad>  (ağ/DNS kesintilerine karşı 10 deneme)
  [ -f "$1" ] || return 0
  for i in 1 2 3 4 5 6 7 8 9 10; do
    curl -sS --retry 3 -X POST -H "$AUTH" -H "Content-Type: application/octet-stream" \
      --data-binary @"$1" "https://uploads.github.com/repos/$REPO/releases/$RELEASE_ID/assets?name=$2" >/dev/null && return 0
    echo "Yükleme denemesi $i başarısız: $2"; sleep $((i * 20))
  done
  echo "Yükleme başarısız: $2"
}

finish() {
  status=$1
  echo "=== Bitiş: $status ($(date -u)) ==="
  upload /work/repo/training/out/best_model.pt best_model.pt
  upload /work/repo/training/rapor_fungi.md rapor_fungi.md
  upload /work/repo/training/out/per_class.json per_class.json
  echo "$status" > /work/durum.txt
  upload "$LOG" egitim.log
  upload /work/durum.txt "$status.txt"
  # Sunucuyu kapat (para harcamaya devam etmesin)
  curl -sS -X DELETE -H "Authorization: Bearer $RP_KEY" "https://api.runpod.io/v2/pods/$RUNPOD_POD_ID" || true
  sleep 600
}

echo "=== Başlangıç: $(date -u), GPU: $(nvidia-smi --query-gpu=name,memory.total --format=csv,noheader) ==="
nproc; free -g | head -2

RELEASE_ID=$(curl -sS -H "$AUTH" "$API/releases/tags/$RUN_TAG" | python -c "import sys,json; print(json.load(sys.stdin)['id'])")
echo "Sonuç sürümü: $RUN_TAG ($RELEASE_ID)"

# Her 10 dakikada bir günlüğün sonunu "ilerleme.log" olarak yükle (canlı takip için)
progress_loop() {
  while true; do
    sleep 600
    tail -n 120 "$LOG" > /work/ilerleme.log
    OLD=$(curl -sS -H "$AUTH" "$API/releases/$RELEASE_ID/assets" | python -c "import sys,json; print(' '.join(str(a['id']) for a in json.load(sys.stdin) if a['name']=='ilerleme.log'))")
    for ID in $OLD; do curl -sS -X DELETE -H "$AUTH" "$API/releases/assets/$ID" >/dev/null; done
    upload /work/ilerleme.log ilerleme.log
  done
}
progress_loop &

# Sağlam indirme: HTTP/1.1, her hatada yeniden dene, kaldığı yerden sürdür
dl() {
  for i in 1 2 3 4 5 6 7 8; do
    curl -sSL --http1.1 --retry 5 --retry-all-errors --retry-delay 5 -C - \
      -H "$AUTH" -H "Accept: application/octet-stream" "$1" -o "$2" && return 0
    echo "indirme kesildi ($i): $2"; sleep 10
  done
  return 1
}

set -o pipefail
(
  set -e
  for i in 1 2 3 4 5 6; do
    rm -rf /work/repo
    git clone --depth 1 "https://github.com/$REPO" /work/repo && break
    echo "git clone başarısız ($i), tekrar deneniyor"; sleep 20
  done
  [ -d /work/repo/training ]
  cd /work/repo
  for i in 1 2 3; do pip install -q timm requests && break; sleep 15; done
  python -c "import torch, timm; print('torch', torch.__version__, 'cuda', torch.cuda.is_available(), 'timm', timm.__version__)"
  if ! python -c "import torch,sys; sys.exit(0 if torch.cuda.is_available() else 1)"; then
    echo "GPU kullanılamıyor (sürücü uyumsuz); başka sunucu istenecek."
    exit 3
  fi

  SPECIES=app/src/main/assets/species.tsv
  if [ -n "$LIMIT_SPECIES" ]; then   # hızlı deneme: yalnızca ilk N tür
    head -n $((LIMIT_SPECIES + 1)) "$SPECIES" > /work/species_deneme.tsv
    SPECIES=/work/species_deneme.tsv
  fi

  echo "=== Fotoğraflar indiriliyor ($(date -u)) ==="
  # FungiTastic (uzman doğrulamalı) ve iNaturalist aynı anda indirilir
  FT_SPLITS=train,val,test
  [ -n "$LIMIT_SPECIES" ] && FT_SPLITS=val
  FT_PARTS=""
  if [ -n "$DATA_TAG" ]; then
    FT_PARTS=$(curl -sS --http1.1 --retry 5 --retry-all-errors -H "$AUTH" "$API/releases/tags/$DATA_TAG" | python -c "
import sys,json
a=sorted((x['name'],x['id']) for x in json.load(sys.stdin)['assets'] if x['name'].startswith('veri-ft.tar.part-'))
print(' '.join(str(i) for _,i in a))")
  fi
  if [ -n "$FT_PARTS" ]; then
    # Önceden hazırlanmış FungiTastic parçaları (sırayla birleştirilip açılır)
    # Her parça önce dosyaya indirilir (kopan bağlantı kaldığı yerden sürdürülür), sonra açılır
    ( set -e
      N=0
      for ID in $FT_PARTS; do
        N=$((N+1)); F="/work/ft_part_$(printf %02d $N)"
        dl "$API/releases/assets/$ID" "$F"
      done
      cat /work/ft_part_* | tar -xf - -C training
      rm -f /work/ft_part_*
      echo "FungiTastic paketleri açıldı: $(ls training/data_ft | wc -l) tür" ) > /work/ft.log 2>&1 &
  else
    python training/fetch_fungitastic.py --species "$SPECIES" --per-species "$PER_SPECIES" --splits "$FT_SPLITS" \
      > /work/ft.log 2>&1 &
  fi
  FT_PID=$!
  if [ -n "$DATA_TAG" ]; then
    # iNaturalist fotoğrafları önceden hazırlanmış paketlerden (GitHub sürümü) alınır
    curl -sS --http1.1 --retry 5 --retry-all-errors -H "$AUTH" "$API/releases/tags/$DATA_TAG" | python -c "
import sys,json
for a in json.load(sys.stdin)['assets']:
    if a['name'].startswith('veri-inat-'): print(a['id'], a['name'])" > /work/paketler.txt
    echo "Hazır paket sayısı: $(wc -l < /work/paketler.txt)"
    while read -r ID NAME; do
      dl "$API/releases/assets/$ID" "/work/$NAME"
      tar -xf "/work/$NAME" -C training && rm -f "/work/$NAME"
      echo "  $NAME açıldı"
    done < /work/paketler.txt
    # Yama paketleri: içindeki türlerin eski klasörleri silinip yenisiyle değiştirilir
    curl -sS --http1.1 --retry 5 --retry-all-errors -H "$AUTH" "$API/releases/tags/$DATA_TAG" | python -c "
import sys,json
for a in json.load(sys.stdin)['assets']:
    if a['name'].startswith('veri-yama'): print(a['id'], a['name'])" > /work/yamalar.txt
    while read -r ID NAME; do
      dl "$API/releases/assets/$ID" "/work/$NAME"
      for D in $(tar -tf "/work/$NAME" | awk -F/ 'NF>2 && $1=="data" {print $2}' | sort -u); do
        rm -rf "training/data/$D"; echo "  yama: $D yenilendi"
      done
      tar -xf "/work/$NAME" -C training && rm -f "/work/$NAME"
    done < /work/yamalar.txt
    echo "iNaturalist türleri: $(ls training/data | wc -l)"
  else
    python training/fetch_inat.py --species "$SPECIES" --per-species "$PER_SPECIES" --workers 64 --species-workers 6
  fi
  wait $FT_PID || echo "FungiTastic indirmesi başarısız; yalnızca iNaturalist kullanılacak"
  cat /work/ft.log
  df -h /work | tail -1
  du -sh training/data training/data_ft 2>/dev/null || true

  # Eğitim süresini iş akışının kapanma saatine göre ayarla (sonuç yüklemeye 30 dk pay bırak)
  if [ -n "$DEADLINE" ]; then
    HOURS=$(python -c "import time; print(round(max(0.3, min(float('$HOURS'), ($DEADLINE - time.time() - 1800) / 3600)), 2))")
    echo "Eğitim bütçesi: $HOURS saat (kapanma: $(date -u -d @$DEADLINE))"
  fi
  echo "=== Eğitim başlıyor ($(date -u)) ==="
  tail -n 120 "$LOG" > /work/ilerleme.log; upload /work/ilerleme.log ilerleme-baslangic.log
  python training/train_fungi.py --species "$SPECIES" --df20 training/data_ft --model "$MODEL" --img "$IMG" --batch "$BATCH" \
    --limit-per-class "$PER_SPECIES" --head-epochs 1 --max-epochs "$MAX_EPOCHS" --patience 4 \
    --hours "$HOURS" --final --no-export
)
RC=$?
if [ $RC -eq 0 ]; then finish basarili
elif [ $RC -eq 3 ]; then finish gpuyok
else finish hata; fi
