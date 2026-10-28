<template>
  <div
    class="kritik-stok"
    data-test="kritik-stok"
  >
    <div
      v-if="(yukleniyor || yerelYukleniyor) && !satirlar.length"
      class="kritik-iskelet"
      aria-hidden="true"
    >
      <span
        v-for="i in 3"
        :key="`kritik-iskelet-${i}`"
        class="iskelet"
      />
    </div>

    <ul
      v-else-if="satirlar.length"
      class="kritik-liste"
    >
      <li
        v-for="(s, index) in satirlar"
        :key="s.id ?? s.stokKodu ?? `${index}`"
      >
        <router-link
          to="/stoklar?kritik=1"
          class="kritik-satir"
        >
          <span class="kritik-ad">{{ s.ad || s.stokKodu }}</span>
          <span class="kritik-miktar gizli-veri">
            {{ t('dashboard.kritikStokMiktarMin', { miktar: sayi(s.miktar), min: sayi(s.minMiktar) }) }}
          </span>
        </router-link>
      </li>
    </ul>

    <p
      v-else
      class="kritik-bos"
    >
      <i
        class="pi pi-check-circle"
        aria-hidden="true"
      />
      {{ t('dashboard.kritikStokYok') }}
    </p>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { stokAPI } from '../../api/index.js'

const props = defineProps({
  // Ust bilesen liste verirse istek atilmaz (test/senkron kullanim).
  stoklar: { type: Array, default: null },
  yukleniyor: { type: Boolean, default: false }
})

const { t } = useI18n()

const yerelVeri = ref([])
const yerelYukleniyor = ref(false)

// En fazla 4 satir; detayi icin kart basligindaki "Tumunu gor" baglantisi var.
const satirlar = computed(() => {
  const liste = props.stoklar || yerelVeri.value
  return Array.isArray(liste) ? liste.slice(0, 4) : []
})

const sayi = (deger) => Number(deger || 0).toLocaleString('tr-TR')

onMounted(async () => {
  if (props.stoklar || !stokAPI?.kritik) return
  yerelYukleniyor.value = true
  try {
    const cevap = await stokAPI.kritik()
    yerelVeri.value = Array.isArray(cevap?.data) ? cevap.data : []
  } catch {
    // Bolum bos durum gosterir; sayfa akisi bozulmaz.
    yerelVeri.value = []
  } finally {
    yerelYukleniyor.value = false
  }
})
</script>

<style scoped>
.kritik-stok {
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.kritik-liste {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
}
.kritik-liste li + li {
  border-top: 1px solid var(--border);
}
.kritik-satir {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 7px 4px;
  border-radius: 8px;
  min-width: 0;
  color: inherit;
  text-decoration: none;
  transition: background var(--dur-base, 0.2s) var(--ease-standard, ease);
}
.kritik-satir:hover {
  background: var(--bg-muted);
}
.kritik-satir:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}
.kritik-ad {
  font-size: 12.5px;
  font-weight: 600;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  min-width: 0;
}
.kritik-miktar {
  font-size: 11.5px;
  font-variant-numeric: tabular-nums;
  color: var(--danger);
  white-space: nowrap;
  flex-shrink: 0;
}
.kritik-bos {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  padding: 6px 0;
  color: var(--text-muted);
  font-size: 12.5px;
}
.kritik-bos i {
  color: var(--success);
}
.kritik-iskelet {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.iskelet {
  display: block;
  height: 20px;
  border-radius: 7px;
  background: var(--bg-muted);
  animation: iskeletParlama 1.4s ease infinite;
}
@keyframes iskeletParlama {
  0% {
    opacity: 0.45;
  }
  50% {
    opacity: 0.85;
  }
  100% {
    opacity: 0.45;
  }
}
@media (prefers-reduced-motion: reduce) {
  .kritik-satir,
  .iskelet {
    transition: none;
    animation: none;
  }
}
</style>
