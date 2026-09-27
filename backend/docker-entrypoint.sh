#!/bin/sh
# RasPel backend baslatma betigi.
#
# Neden var: -XX:MaxRAMPercentage bazi ortamlarda (ozellikle Docker Desktop/WSL2)
# konteyner cgroup limiti yerine HOST RAM'ini kullanabiliyor; bu da 2GB limitli
# konteynerde ~10GB heap rezerve edilmesine ve OOM-kill'e yol aciyordu. Burada
# limit dogrudan cgroup dosyasindan okunup heap acikca hesaplanir; boylece hem
# Linux VPS hem Docker Desktop'ta davranis aynidir.
set -e

# Oncelik: JAVA_MAX_HEAP env (or. "1500m" veya "1024") > cgroup limitinin %65'i.
HEAP_MB=""

if [ -n "${JAVA_MAX_HEAP:-}" ]; then
  case "$JAVA_MAX_HEAP" in
    *m|*M) HEAP_MB=$(printf '%s' "$JAVA_MAX_HEAP" | tr -d 'mM') ;;
    *g|*G) HEAP_MB=$(( $(printf '%s' "$JAVA_MAX_HEAP" | tr -d 'gG') * 1024 )) ;;
    *[!0-9]*) HEAP_MB="" ;;
    *) HEAP_MB="$JAVA_MAX_HEAP" ;;
  esac
fi

if [ -z "$HEAP_MB" ]; then
  LIMIT_BYTES=""
  # cgroup v2
  if [ -f /sys/fs/cgroup/memory.max ]; then
    v=$(cat /sys/fs/cgroup/memory.max 2>/dev/null || true)
    case "$v" in ''|*[!0-9]*) ;; *) LIMIT_BYTES="$v" ;; esac
  fi
  # cgroup v1 (eski cekirdekler)
  if [ -z "$LIMIT_BYTES" ] && [ -f /sys/fs/cgroup/memory/memory.limit_in_bytes ]; then
    v=$(cat /sys/fs/cgroup/memory/memory.limit_in_bytes 2>/dev/null || true)
    case "$v" in ''|*[!0-9]*) ;; *) LIMIT_BYTES="$v" ;; esac
  fi
  if [ -n "$LIMIT_BYTES" ]; then
    HEAP_MB=$(( LIMIT_BYTES / 1048576 * 65 / 100 ))
  fi
fi

# Guvenlik sinirlari: min 512m, max 4096m (asiri rezervasyonu engeller).
if [ -n "$HEAP_MB" ]; then
  [ "$HEAP_MB" -lt 512 ] && HEAP_MB=512
  [ "$HEAP_MB" -gt 4096 ] && HEAP_MB=4096
  XMS_MB=$(( HEAP_MB / 3 ))
  echo "[entrypoint] Bellek limiti algilandi; heap -Xmx${HEAP_MB}m -Xms${XMS_MB}m"
  exec java -Xmx${HEAP_MB}m -Xms${XMS_MB}m -Xss512k \
    -XX:MaxMetaspaceSize=160m -XX:MaxDirectMemorySize=96m -XX:ReservedCodeCacheSize=96m \
    -XX:+UseG1GC -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/heapdump.hprof \
    -XX:+ExitOnOutOfMemoryError -jar app.jar
fi

# Limit okunamadiysa yuzde tabanli geri donus (en azindan bir sinir koyar).
echo "[entrypoint] Bellek limiti okunamadi; MaxRAMPercentage=65 kullaniliyor"
exec java -XX:MaxRAMPercentage=65 -XX:InitialRAMPercentage=25 -Xss512k \
  -XX:MaxMetaspaceSize=160m -XX:MaxDirectMemorySize=96m -XX:ReservedCodeCacheSize=96m \
  -XX:+UseG1GC -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/heapdump.hprof \
  -XX:+ExitOnOutOfMemoryError -jar app.jar
