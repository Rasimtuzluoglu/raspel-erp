#!/bin/sh
# REDTEAM/Faz3 canli dogrulama (CSS): PrimeVue 4 ikon siniflari tema CSS'inde
# mevcut, PrimeVue 3 ikon siniflari YOK.
cd /usr/share/nginx/html/assets || exit 1
for f in *.css; do
  iconfield=$(grep -o '\.p-iconfield' "$f" 2>/dev/null | wc -l)
  inputicon=$(grep -o '\.p-inputicon' "$f" 2>/dev/null | wc -l)
  eski=$(grep -o 'p-input-icon-left' "$f" 2>/dev/null | wc -l)
  echo "$f  .p-iconfield=$iconfield  .p-inputicon=$inputicon  p-input-icon-left=$eski"
done