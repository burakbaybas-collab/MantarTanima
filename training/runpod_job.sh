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

upload() {  # upload <dosya> <ad>
  [ -f "$1" ] || return 0
  curl -sS -X POST -H "$AUTH" -H "Content-Type: application/octet-stream" \
    --data-binary @"$1" "https://uploads.github.com/repos/$REPO/releases/$RELEASE_ID/assets?name=$2" >/dev/null \
    || echo "Yükleme başarısız: $2"
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

set -o pipefail
(
  set -e
  git clone --depth 1 "https://github.com/$REPO" /work/repo
  cd /work/repo
  pip install -q timm requests
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
  python training/fetch_inat.py --species "$SPECIES" --per-species "$PER_SPECIES" --workers 64 --species-workers 6
  if [ -z "$LIMIT_SPECIES" ]; then
    python training/fetch_df20.py --species "$SPECIES" --per-species 300 || echo "DF20 atlandı"
  fi
  du -sh training/data training/data_df20 2>/dev/null || true

  echo "=== Eğitim başlıyor ($(date -u)) ==="
  python training/train_fungi.py --species "$SPECIES" --model "$MODEL" --img "$IMG" --batch "$BATCH" \
    --limit-per-class "$PER_SPECIES" --head-epochs 1 --max-epochs "$MAX_EPOCHS" --patience 4 \
    --hours "$HOURS" --final --no-export
)
RC=$?
if [ $RC -eq 0 ]; then finish basarili
elif [ $RC -eq 3 ]; then finish gpuyok
else finish hata; fi
