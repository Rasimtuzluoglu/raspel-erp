<template>
  <div
    v-if="visible"
    class="db-drawer-katman"
    data-test="vade-takvimi-katman"
    @click.self="kapat"
  >
    <aside
      ref="panel"
      class="db-drawer db-takvim-drawer"
      role="dialog"
      aria-modal="true"
      :aria-labelledby="baslikId"
      tabindex="-1"
      :aria-busy="yukleniyor ? 'true' : 'false'"
      data-test="vade-takvimi"
      @keydown="tusYonet"
    >
      <header class="db-drawer-ust">
        <div class="db-drawer-baslik-alan">
          <h2
            :id="baslikId"
            class="db-drawer-baslik"
          >
            {{ t('dashboard.vadeTakvimi') }}
          </h2>
          <p class="db-drawer-alt">
            {{ t('dashboard.vadeTakvimiAlt') }}
          </p>
        </div>
        <button
          type="button"
          class="db-drawer-kapat"
          :aria-label="t('common.close')"
          :title="t('common.close')"
          @click="kapat"
        >
          <i
            class="pi pi-times"
            aria-hidden="true"
          />
        </button>
      </header>

      <div class="takvim-ust">
        <button
          type="button"
          class="takvim-ay-buton"
          :aria-label="t('dashboard.oncekiAy')"
          :title="t('dashboard.oncekiAy')"
          data-test="takvim-onceki-ay"
          @click="ayDegistir(-1)"
        >
          <i
            class="pi pi-chevron-left"
            aria-hidden="true"
          />
        </button>
        <span
          class="takvim-ay"
          aria-live="polite"
          data-test="takvim-ay"
        >{{ ayEtiketi }}</span>
        <button
          type="button"
          class="takvim-ay-buton"
          :aria-label="t('dashboard.sonrakiAy')"
          :title="t('dashboard.sonrakiAy')"
          data-test="takvim-sonraki-ay"
          @click="ayDegistir(1)"
        >
          <i
            class="pi pi-chevron-right"
            aria-hidden="true"
          />
        </button>
      </div>

      <p
        v-if="yukleniyor && gunBilgisi.size"
        class="takvim-yukleniyor"
        role="status"
      >
        {{ t('dashboard.takvimYukleniyor') }}
      </p>

      <div
        v-if="yukleniyor && !gunBilgisi.size"
        class="takvim-iskelet"
        aria-hidden="true"
      >
        <span
          v-for="i in 35"
          :key="`takvim-iskelet-${i}`"
          class="takvim-iskelet-hucre"
        />
      </div>

      <div
        v-else-if="hata"
        class="takvim-durum takvim-durum--hata"
        role="alert"
        data-test="takvim-hata"
      >
        <span>
          <i
            class="pi pi-exclamation-circle"
            aria-hidden="true"
          />
          {{ t('dashboard.takvimHata') }}
        </span>
        <button
          type="button"
          class="takvim-dene"
          @click="yukle"
        >
          {{ t('dashboard.gorevYenidenDene') }}
        </button>
      </div>

      <template v-else>
        <div
          class="takvim-izgara"
          role="group"
          :aria-label="t('dashboard.vadeTakvimi')"
        >
          <div
            class="takvim-gun-adlari"
            aria-hidden="true"
          >
            <span
              v-for="ad in gunAdlari"
              :key="ad"
              class="takvim-gun-adi"
            >{{ ad }}</span>
          </div>
          <div class="takvim-hucreler">
            <template
              v-for="hucre in hucreler"
              :key="hucre.anahtar"
            >
              <span
                v-if="hucre.bos"
                class="takvim-hucre takvim-hucre--bos"
                aria-hidden="true"
              />
              <button
                v-else
                type="button"
                class="takvim-hucre"
                :class="hucreSinifi(hucre)"
                :aria-pressed="seciliGun === hucre.kod"
                :aria-label="hucreEtiketi(hucre)"
                :data-test="`takvim-gun-${hucre.kod}`"
                @click="gunSec(hucre)"
              >
                <span class="takvim-gun-no">{{ hucre.gunNo }}</span>
                <span
                  v-if="hucre.adet"
                  class="takvim-gun-tutar"
                >{{ kisaTutar(hucre.tutar) }}</span>
                <span
                  v-if="hucre.adet"
                  class="takvim-gun-adet"
                >{{ hucre.adet }}</span>
              </button>
            </template>
          </div>
        </div>

        <p
          v-if="!ayKayitVar"
          class="takvim-bos"
          data-test="takvim-bos"
        >
          <i
            class="pi pi-check-circle"
            aria-hidden="true"
          />
          {{ t('dashboard.takvimBos') }}
        </p>

        <section
          v-if="seciliGun"
          class="takvim-liste"
          data-test="takvim-liste"
        >
          <header class="takvim-liste-ust">
            <h3 class="takvim-liste-baslik">
              {{ formatDate(seciliGun) }}
            </h3>
            <router-link
              to="/tahsilat"
              class="takvim-liste-link"
            >
              {{ t('dashboard.tahsilataGit') }}
              <i
                class="pi pi-arrow-right"
                aria-hidden="true"
              />
            </router-link>
          </header>

          <ul
            v-if="seciliKayitlar.length"
            class="takvim-kayitlar"
          >
            <li
              v-for="kayit in seciliKayitlar"
              :key="`${kayit.faturaId}-${kayit.faturaNumarasi}-${kayit.vadeTarihi}`"
              class="takvim-kayit"
              :class="{ 'takvim-kayit--gecikmis': kayit.geciktiMi }"
              :data-test="`takvim-kayit-${kayit.faturaId}`"
            >
              <span class="takvim-kayit-icerik">
                <strong class="takvim-kayit-no">{{ kayit.faturaNumarasi }}</strong>
                <span class="takvim-kayit-cari">{{ kayit.cariAd }}</span>
              </span>
              <strong class="takvim-kayit-tutar gizli-veri">{{ formatCurrency(kayit.kalanTutar) }}</strong>
            </li>
          </ul>
          <p
            v-else
            class="takvim-bos"
          >
            {{ t('dashboard.vadeListeBos') }}
          </p>
        </section>
      </template>
    </aside>
  </div>
</template>

<script setup>
import { computed, nextTick, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { dashboardAPI } from '../../api/index.js'
import { formatCurrency, formatDate, getLocalDateString } from '../../utils/format.js'

const props = defineProps({
  visible: { type: Boolean, default: false }
})

const emit = defineEmits(['update:visible'])

const { t } = useI18n()

const baslikId = 'db-vade-takvimi-baslik'
const panel = ref(null)
let oncekiOdak = null

const GUN_ADI_ANAHTARLARI = [
  'dashboard.gunKisaPzt',
  'dashboard.gunKisaSal',
  'dashboard.gunKisaCar',
  'dashboard.gunKisaPer',
  'dashboard.gunKisaCum',
  'dashboard.gunKisaCmt',
  'dashboard.gunKisaPaz'
]
const gunAdlari = computed(() => GUN_ADI_ANAHTARLARI.map((anahtar) => t(anahtar)))

const bugunKodu = getLocalDateString(new Date())
const yediGunSonra = getLocalDateString(new Date(Date.now() + 7 * 24 * 60 * 60 * 1000))

const simdi = new Date()
const ay = ref(new Date(simdi.getFullYear(), simdi.getMonth(), 1))
const gunBilgisi = ref(new Map())
const yukleniyor = ref(false)
const hata = ref('')
const seciliGun = ref('')

const ayEtiketi = computed(() =>
  new Intl.DateTimeFormat('tr-TR', { month: 'long', year: 'numeric' }).format(ay.value)
)

const ayAraligi = computed(() => {
  const yil = ay.value.getFullYear()
  const ayNo = ay.value.getMonth()
  return {
    baslangic: getLocalDateString(new Date(yil, ayNo, 1)),
    bitis: getLocalDateString(new Date(yil, ayNo + 1, 0))
  }
})

/** Pzt–Paz hafta baslangicli aylik izgara (bos hucreler `bos: true`). */
const hucreler = computed(() => {
  const yil = ay.value.getFullYear()
  const ayNo = ay.value.getMonth()
  const ilkGun = new Date(yil, ayNo, 1)
  const kaydirma = (ilkGun.getDay() + 6) % 7
  const gunSayisi = new Date(yil, ayNo + 1, 0).getDate()
  const toplam = Math.ceil((kaydirma + gunSayisi) / 7) * 7
  const harita = gunBilgisi.value

  return Array.from({ length: toplam }, (_, sira) => {
    const gunNo = sira - kaydirma + 1
    if (gunNo < 1 || gunNo > gunSayisi) return { bos: true, anahtar: `bos-${sira}` }
    const kod = getLocalDateString(new Date(yil, ayNo, gunNo))
    const bilgi = harita.get(kod) || null
    return {
      bos: false,
      anahtar: kod,
      kod,
      gunNo,
      tutar: bilgi?.tutar || 0,
      adet: bilgi?.adet || 0,
      gecikmis: (bilgi?.kayitlar || []).some((k) => k.geciktiMi === true),
      yaklasan:
        (bilgi?.kayitlar || []).length > 0 &&
        !(bilgi?.kayitlar || []).some((k) => k.geciktiMi === true) &&
        kod >= bugunKodu &&
        kod <= yediGunSonra
    }
  })
})

const ayKayitVar = computed(() => gunBilgisi.value.size > 0)

const seciliKayitlar = computed(() => {
  if (!seciliGun.value) return []
  return gunBilgisi.value.get(seciliGun.value)?.kayitlar || []
})

const kisaTutar = (deger) =>
  new Intl.NumberFormat('tr-TR', {
    style: 'currency',
    currency: 'TRY',
    notation: 'compact',
    maximumFractionDigits: 1
  }).format(Number(deger) || 0)

const hucreSinifi = (hucre) => ({
  'takvim-hucre--dolu': hucre.adet > 0,
  'takvim-hucre--gecikmis': hucre.gecikmis,
  'takvim-hucre--yaklasan': hucre.yaklasan,
  'takvim-hucre--secili': seciliGun.value === hucre.kod,
  'takvim-hucre--bugun': hucre.kod === bugunKodu
})

const hucreEtiketi = (hucre) =>
  t('dashboard.takvimHucreEtiket', {
    tarih: formatDate(hucre.kod),
    adet: hucre.adet,
    tutar: formatCurrency(hucre.tutar)
  })

const gunSec = (hucre) => {
  seciliGun.value = seciliGun.value === hucre.kod ? '' : hucre.kod
}

const ayDegistir = (yon) => {
  seciliGun.value = ''
  ay.value = new Date(ay.value.getFullYear(), ay.value.getMonth() + yon, 1)
}

const yukle = async () => {
  const { baslangic, bitis } = ayAraligi.value
  yukleniyor.value = true
  hata.value = ''
  try {
    const cevap = await dashboardAPI.vadeler({ baslangic, bitis })
    const veri = cevap?.data
    const liste = Array.isArray(veri) ? veri : veri?.content || []
    const harita = new Map()
    for (const kayit of liste) {
      const kod = String(kayit?.vadeTarihi || '').slice(0, 10)
      if (!kod) continue
      const mevcut = harita.get(kod) || { tutar: 0, adet: 0, kayitlar: [] }
      mevcut.tutar += Number(kayit.kalanTutar) || 0
      mevcut.adet += 1
      mevcut.kayitlar.push(kayit)
      harita.set(kod, mevcut)
    }
    gunBilgisi.value = harita
  } catch (err) {
    hata.value = err?.response?.data?.message || err?.message || t('dashboard.takvimHata')
    gunBilgisi.value = new Map()
  } finally {
    yukleniyor.value = false
  }
}

const kapat = () => emit('update:visible', false)

const tusYonet = (olay) => {
  if (olay.key === 'Escape') {
    kapat()
    return
  }
  if (olay.key !== 'Tab') return
  const ogeler = Array.from(
    panel.value?.querySelectorAll('button:not(:disabled), [href], [tabindex]:not([tabindex="-1"])') || []
  )
  if (!ogeler.length) return
  const ilk = ogeler[0]
  const son = ogeler[ogeler.length - 1]
  if (olay.shiftKey && document.activeElement === ilk) {
    olay.preventDefault()
    son.focus()
  } else if (!olay.shiftKey && document.activeElement === son) {
    olay.preventDefault()
    ilk.focus()
  }
}

watch(
  () => props.visible,
  (acik) => {
    if (acik) {
      oncekiOdak = typeof document !== 'undefined' ? document.activeElement : null
      seciliGun.value = ''
      yukle()
      nextTick(() => panel.value?.focus?.())
    } else if (oncekiOdak && typeof oncekiOdak.focus === 'function') {
      oncekiOdak.focus?.()
      oncekiOdak = null
    }
  },
  { immediate: true }
)

watch(ay, () => {
  if (props.visible) yukle()
})
</script>

<style scoped>
.takvim-ust {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.takvim-ay {
  font-size: 13px;
  font-weight: 700;
  color: var(--text-primary);
  text-transform: capitalize;
}
.takvim-ay-buton {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--bg-muted);
  color: var(--text-secondary);
  cursor: pointer;
}
.takvim-ay-buton:hover {
  border-color: var(--accent-border);
  color: var(--accent);
}
.takvim-ay-buton:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}
.takvim-ay-buton i {
  font-size: 10px;
}
.takvim-yukleniyor {
  margin: 0;
  font-size: 11px;
  color: var(--text-muted);
}

/* --- Izgara -------------------------------------------------------------- */
.takvim-izgara {
  min-width: 0;
}
.takvim-gun-adlari,
.takvim-hucreler {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 4px;
}
.takvim-gun-adi {
  padding: 2px 0;
  text-align: center;
  font-size: 9.5px;
  font-weight: 700;
  letter-spacing: 0.02em;
  text-transform: uppercase;
  color: var(--text-muted);
}
.takvim-hucre {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-start;
  gap: 1px;
  min-height: 52px;
  padding: 4px 2px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--bg-card);
  color: var(--text-secondary);
  font-size: 11px;
  cursor: pointer;
  transition:
    border-color var(--dur-base, 0.2s) var(--ease-standard, ease),
    background var(--dur-base, 0.2s) var(--ease-standard, ease);
}
.takvim-hucre:hover {
  border-color: var(--accent-border);
  background: var(--bg-muted);
}
.takvim-hucre:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}
.takvim-hucre--bos {
  border-color: transparent;
  background: transparent;
  cursor: default;
  pointer-events: none;
}
.takvim-hucre--bugun {
  border-color: var(--accent-border);
}
.takvim-hucre--dolu {
  background: var(--bg-muted);
}
.takvim-hucre--secili {
  border-color: var(--accent);
  box-shadow: 0 0 0 1px var(--accent) inset;
}
.takvim-hucre--gecikmis {
  border-color: var(--danger-border, var(--danger));
  background: var(--danger-soft);
}
.takvim-hucre--yaklasan {
  border-color: var(--warning-border, var(--warning));
  background: var(--warning-soft);
}
.takvim-gun-no {
  font-size: 11px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  color: var(--text-primary);
}
.takvim-gun-tutar {
  font-size: 8.5px;
  line-height: 1.2;
  font-weight: 600;
  color: var(--text-muted);
  font-variant-numeric: tabular-nums;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.takvim-hucre--gecikmis .takvim-gun-tutar {
  color: var(--danger);
}
.takvim-gun-adet {
  position: absolute;
  top: 2px;
  right: 3px;
  min-width: 13px;
  height: 13px;
  border-radius: 999px;
  background: var(--accent-soft);
  color: var(--accent);
  font-size: 8.5px;
  font-weight: 800;
  line-height: 13px;
  text-align: center;
}
.takvim-hucre--gecikmis .takvim-gun-adet {
  background: var(--danger);
  color: #fff;
}
.takvim-hucre--yaklasan .takvim-gun-adet {
  background: var(--warning);
  color: #1f2937;
}

/* --- Durumlar ------------------------------------------------------------ */
.takvim-iskelet {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 4px;
}
.takvim-iskelet-hucre {
  height: 52px;
  border-radius: 8px;
  background: var(--bg-muted);
  animation: takvimParlama 1.4s ease infinite;
}
.takvim-durum {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  padding: 14px 4px;
  font-size: 12.5px;
  color: var(--text-muted);
}
.takvim-durum--hata {
  color: var(--danger);
  justify-content: space-between;
}
.takvim-dene {
  min-height: 30px;
  padding: 0 10px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--bg-card);
  color: var(--text-secondary);
  font-size: 11.5px;
  font-weight: 600;
  cursor: pointer;
}
.takvim-dene:hover {
  color: var(--accent);
  border-color: var(--accent-border);
}
.takvim-dene:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}
.takvim-bos {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 8px 0 0;
  font-size: 11.5px;
  color: var(--text-muted);
}
.takvim-bos i {
  color: var(--success);
}

/* --- Secili gun listesi -------------------------------------------------- */
.takvim-liste {
  margin-top: 4px;
  padding-top: 10px;
  border-top: 1px solid var(--border);
}
.takvim-liste-ust {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.takvim-liste-baslik {
  margin: 0;
  font-size: 12.5px;
  font-weight: 700;
  color: var(--text-primary);
}
.takvim-liste-link {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 2px 4px;
  border-radius: 6px;
  color: var(--accent);
  font-size: 11px;
  font-weight: 700;
  text-decoration: none;
  white-space: nowrap;
}
.takvim-liste-link:hover {
  color: var(--accent-hover);
}
.takvim-liste-link:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}
.takvim-liste-link i {
  font-size: 8.5px;
}
.takvim-kayitlar {
  list-style: none;
  margin: 6px 0 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.takvim-kayit {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 9px;
  border: 1px solid var(--border);
  border-radius: 9px;
  background: var(--bg-muted);
  min-width: 0;
}
.takvim-kayit--gecikmis {
  border-color: var(--danger-border, var(--danger));
  background: var(--danger-soft);
}
.takvim-kayit-icerik {
  display: flex;
  flex-direction: column;
  gap: 1px;
  flex: 1;
  min-width: 0;
}
.takvim-kayit-no {
  font-size: 11.5px;
  font-weight: 700;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.takvim-kayit-cari {
  font-size: 10.5px;
  color: var(--text-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.takvim-kayit-tutar {
  flex-shrink: 0;
  font-size: 11.5px;
  font-weight: 800;
  color: var(--text-primary);
  font-variant-numeric: tabular-nums;
}
.takvim-kayit--gecikmis .takvim-kayit-tutar {
  color: var(--danger);
}

@keyframes takvimParlama {
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
  .takvim-hucre,
  .takvim-iskelet-hucre {
    transition: none;
    animation: none;
  }
}
@media (max-width: 600px) {
  .takvim-hucre {
    min-height: 46px;
  }
  .takvim-gun-tutar {
    font-size: 8px;
  }
}
</style>
