#!/bin/sh
# REDTEAM/Faz3 canli dogrulama: uretilen bundle'da kaldirilmis PrimeVue 3
# sinifi KALMAMALI, PrimeVue 4 ikon sinifi (`p-iconfield`) OLMALI.
cd /usr/share/nginx/html/assets || exit 1
for f in *.js; do
  eski=$(grep -o 'p-input-icon-left' "$f" 2>/dev/null | wc -l)
  yeni=$(grep -o 'p-iconfield' "$f" 2>/dev/null | wc -l)
  # Ekran metni olarak basilmis yonerge kaliplari (3.1)
  metin=$(grep -o "v-permission=.\{0,25\}" "$f" 2>/dev/null | wc -l)
  echo "$f  p-input-icon-left=$eski  p-iconfield=$yeni"
done
echo "--- toplam"
echo "p-input-icon-left : $(grep -o 'p-input-icon-left' *.js 2>/dev/null | wc -l)"
echo "p-iconfield       : $(grep -o 'p-iconfield' *.js 2>/dev/null | wc -l)"
echo "pi-inputicon      : $(grep -o 'pi-inputicon' *.js 2>/dev/null | wc -l)"
echo "v-permission      : $(grep -o 'v-permission' *.js 2>/dev/null | wc -l)"