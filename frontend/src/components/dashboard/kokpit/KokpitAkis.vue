<template>
  <section
    class="kokpit-akis kart"
    data-test="kokpit-akis"
    :aria-label="t('dashboard.sonIslemler')"
  >
    <header class="akis-baslik">
      <h2 class="akis-baslik-metin">
        {{ t('dashboard.sonIslemler') }}
      </h2>
      <span
        v-if="canli"
        class="akis-canli"
        :title="t('dashboard.kokpitCanli')"
        aria-hidden="true"
      />
    </header>

    <div
      v-if="turler.length > 1"
      class="akis-filtreler"
      role="group"
      :aria-label="t('dashboard.akisFiltreTumu')"
    >
      <button
        type="button"
        class="akis-filtre"
        :class="{ 'akis-filtre--aktif': tur === 'TUMU' }"
        :aria-pressed="tur === 'TUMU'"
        data-test="akis-filtre-TUMU"
        @click="turSec('TUMU')"
      >
        {{ t('dashboard.akisFiltreTumu') }}
      </button>
      <button
        v-for="secenek in turler"
        :key="secenek"
        type="button"
        class="akis-filtre"
        :class="{ 'akis-filtre--aktif': tur === secenek }"
        :aria-pressed="tur === secenek"
        :data-test="`akis-filtre-${secenek}`"
        @click="turSec(secenek)"
      >
        {{ turEtiketi(secenek) }}
      </button>
    </div>

    <div
      v-if="yukleniyor && olaylar.length === 0"
      class="akis-iskeletler"
      aria-hidden="true"
    >
      <span
        v-for="n in 4"
        :key="`akis-iskelet-${n}`"
        class="akis-iskelet"
      />
    </div>

    <p
      v-else-if="hata"
      class="akis-hata"
      role="alert"
      data-test="kokpit-akis-hata"
    >
      <i
        class="pi pi-exclamation-triangle"
        aria-hidden="true"
      />
      <span>{{ t('dashboard.akisHata') }}</span>
      <button
        type="button"
        class="akis-yenile"
        @click="emit('yenile')"
      >
        {{ t('dashboard.yenidenDene') }}
      </button>
    </p>

    <p
      v-else-if="gorunen.length === 0"
      class="akis-bos"
      data-test="kokpit-akis-bos"
    >
      <i
        class="pi pi-inbox"
        aria-hidden="true"
      />
      {{ t('dashboard.akisBos') }}
    </p>

    <div
      v-else
      class="akis-gruplar"
    >
      <div
        v-for="grup in gruplar"
        :key="grup.anahtar"
        class="akis-grup"
        :data-test="`akis-grup-${grup.anahtar}`"
      >
        <span
          class="akis-grup-baslik"
          role="heading"
          aria-level="3"
        >{{ grup.etiket }}</span>
        <ul class="akis-liste">
          <li
            v-for="{ olay, index } in grup.ogeler"
            :key="`${olay.tur || 'olay'}-${olay.tarih || index}-${index}`"
          >
            <button
              type="button"
              class="akis-satir"
              :data-test="`akis-oge-${index}`"
              @click="emit('sec', olay)"
            >
              <span
                class="akis-ikon"
                :style="ikonStili(olay)"
                aria-hidden="true"
              >
                <i :class="olay.ikon || 'pi pi-circle'" />
              </span>
              <span class="akis-metin">
                <strong class="akis-metin-baslik">{{ olay.baslik || olay.tur }}</strong>
                <small
                  v-if="olay.aciklama"
                  class="akis-metin-aciklama"
                >{{ olay.aciklama }}</small>
              </span>
              <span class="akis-sag">
                <span
                  v-if="olay.tutar !== null && olay.tutar !== undefined"
                  class="akis-tutar"
                >{{ formatPara(olay.tutar, paraBirimi) }}</span>
                <small class="akis-zaman">{{ zamanMetni(olay.tarih) }}</small>
              </span>
            </button>
          </li>
        </ul>
      </div>
    </div>

    <button
      v-if="filtreli.length > varsayilanAdet"
      type="button"
      class="akis-tumu"
      :aria-expanded="genisletildi"
      data-test="akis-tumunu-gor"
      @click="genisletildi = !genisletildi"
    >
      <template v-if="genisletildi">
        {{ t('dashboard.akisDahaAz') }}
      </template>
      <template v-else>
        {{ t('dashboard.akisTumunuGor', { n: filtreli.length }) }}
      </template>
      <i
        class="pi"
        :class="genisletildi ? 'pi-angle-up' : 'pi-angle-down'"
        aria-hidden="true"
      />
    </button>
  </section>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { formatPara } from '../../../utils/format.js'
import { useMobilEkran } from '../../../composables/useMobilEkran.js'

const props = defineProps({
  olaylar: { type: Array, default: () => [] },
  yukleniyor: { type: Boolean, default: false },
  hata: { type: [String, Boolean, null], default: null },
  paraBirimi: { type: String, default: 'TRY' },
  canli: { type: Boolean, default: false },
  /** İlk görünür kayıt sayısı (masaüstü). */
  baslangicAdet: { type: Number, default: 8 },
  /** "Tümünü gör" ile açılan üst sınır. */
  maksimumAdet: { type: Number, default: 20 }
})

const emit = defineEmits(['sec', 'yenile'])
const { t, te } = useI18n()

const mobil = useMobilEkran()
const tur = ref('TUMU')
const genisletildi = ref(false)

const TUR_SIRA = ['FATURA', 'TAHSILAT', 'ODEME', 'SIPARIS', 'STOK', 'ONAY']
const TUR_ANAHTAR = {
  FATURA: 'dashboard.akisTurFatura',
  TAHSILAT: 'dashboard.akisTurTahsilat',
  ODEME: 'dashboard.akisTurOdeme',
  SIPARIS: 'dashboard.akisTurSiparis',
  STOK: 'dashboard.akisTurStok',
  ONAY: 'dashboard.akisTurOnay'
}

const turEtiketi = (kod) => {
  const anahtar = TUR_ANAHTAR[kod]
  return anahtar && te(anahtar) ? t(anahtar) : kod
}

/** Yalnızca veride geçen türler, sabit sırayla (filtre çubuğu şişmesin). */
const turler = computed(() => {
  const mevcut = new Set(props.olaylar.map((olay) => olay.tur).filter(Boolean))
  return TUR_SIRA.filter((kod) => mevcut.has(kod))
})

const filtreli = computed(() => (
  tur.value === 'TUMU'
    ? props.olaylar
    : props.olaylar.filter((olay) => olay.tur === tur.value)
))

/** Mobilde ilk 5, masaüstünde 8; "Tümünü gör" ile maksimuma çıkar. */
const varsayilanAdet = computed(() => {
  const taban = mobil.value ? 5 : props.baslangicAdet
  return Math.max(1, Math.min(taban, props.maksimumAdet))
})

const gorunen = computed(() => {
  const adet = genisletildi.value ? props.maksimumAdet : varsayilanAdet.value
  return filtreli.value.slice(0, Math.max(0, adet))
})

const turSec = (kod) => {
  tur.value = kod
  genisletildi.value = false
}

const GRUP_SIRASI = ['BUGUN', 'DUN', 'BU_HAFTA', 'DAHA_ESKI']
const GRUP_ETIKETI = {
  BUGUN: 'dashboard.gunBugun',
  DUN: 'dashboard.gunDun',
  BU_HAFTA: 'dashboard.gunBuHafta',
  DAHA_ESKI: 'dashboard.gunDahaEski'
}

/** Takvim günü farkına göre grup; geçersiz/eksik tarih "daha eski" sayılır. */
const gunGrubu = (tarih) => {
  if (!tarih) return 'DAHA_ESKI'
  const zaman = new Date(tarih).getTime()
  if (!Number.isFinite(zaman)) return 'DAHA_ESKI'
  const bugun = new Date()
  bugun.setHours(0, 0, 0, 0)
  const gun = new Date(zaman)
  gun.setHours(0, 0, 0, 0)
  const fark = Math.round((bugun.getTime() - gun.getTime()) / 86400000)
  // Geleceğe tarihlenmiş kayıtlar "daha eski" görünmemeli; göreli zaman
  // "az önce" derken eski grubuna düşmek çelişkiliydi.
  if (fark <= 0) return 'BUGUN'
  if (fark === 1) return 'DUN'
  if (fark >= 2 && fark <= 7) return 'BU_HAFTA'
  return 'DAHA_ESKI'
}

/** Görünen (filtre + "Tümünü gör" uygulanmış) olayları sabit sıralı gün gruplarına böler. */
const gruplar = computed(() => {
  const kova = new Map()
  gorunen.value.forEach((olay, index) => {
    const anahtar = gunGrubu(olay?.tarih)
    if (!kova.has(anahtar)) kova.set(anahtar, [])
    kova.get(anahtar).push({ olay, index })
  })
  return GRUP_SIRASI
    .filter((anahtar) => kova.has(anahtar))
    .map((anahtar) => ({
      anahtar,
      etiket: t(GRUP_ETIKETI[anahtar]),
      ogeler: kova.get(anahtar)
    }))
})

watch(() => props.olaylar, () => {
  // Yeni veri geldiğinde seçili tür artık yoksa "Tümü"ye dön.
  if (tur.value !== 'TUMU' && !turler.value.includes(tur.value)) tur.value = 'TUMU'
})

const ikonStili = (olay) => {
  const renk = olay?.renk || 'var(--accent)'
  return {
    background: `color-mix(in srgb, ${renk} 16%, transparent)`,
    color: renk
  }
}

/** Göreli zaman: az önce / N dk / N sa / N gün; eski kayıtlarda tarih. */
const zamanMetni = (tarih) => {
  if (!tarih) return ''
  const zaman = new Date(tarih).getTime()
  if (!Number.isFinite(zaman)) return ''
  const farkDk = Math.max(0, Math.round((Date.now() - zaman) / 60000))
  if (farkDk < 1) return t('dashboard.akisSimdi')
  if (farkDk < 60) return t('dashboard.akisDakikaOnce', { n: farkDk })
  const farkSa = Math.round(farkDk / 60)
  if (farkSa < 24) return t('dashboard.akisSaatOnce', { n: farkSa })
  const farkGun = Math.round(farkSa / 24)
  if (farkGun < 8) return t('dashboard.akisGunOnce', { n: farkGun })
  return new Date(zaman).toLocaleDateString('tr-TR')
}
</script>

<style scoped>
.kokpit-akis {
  display: flex;
  flex-direction: column;
  min-width: 0;
  gap: 8px;
}
.akis-baslik {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 2px;
}
.akis-baslik-metin {
  margin: 0;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.02em;
  color: var(--text-secondary);
  text-transform: uppercase;
}
.akis-canli {
  width: 7px;
  height: 7px;
  border-radius: 999px;
  background: var(--success);
  box-shadow: 0 0 0 3px var(--success-soft);
}
.akis-filtreler {
  display: flex;
  gap: 5px;
  min-width: 0;
  overflow-x: auto;
  padding-bottom: 2px;
  scrollbar-width: thin;
}
.akis-filtre {
  flex-shrink: 0;
  padding: 3px 9px;
  border: 1px solid var(--border);
  border-radius: 999px;
  background: transparent;
  color: var(--text-muted);
  font-size: 10.5px;
  font-weight: 600;
  cursor: pointer;
  transition:
    border-color var(--dur-fast) var(--ease-standard),
    color var(--dur-fast) var(--ease-standard),
    background var(--dur-fast) var(--ease-standard);
}
.akis-filtre:hover,
.akis-filtre:focus-visible {
  border-color: var(--accent-border);
  color: var(--accent);
}
.akis-filtre--aktif {
  border-color: var(--accent-border);
  background: var(--accent-soft);
  color: var(--accent);
}
.akis-filtre:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 1px;
}
.akis-gruplar {
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.akis-grup + .akis-grup {
  margin-top: 6px;
}
.akis-grup-baslik {
  display: block;
  padding: 6px 6px 2px;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: var(--text-muted);
}
.akis-liste {
  display: flex;
  flex-direction: column;
  margin: 0;
  padding: 0;
  list-style: none;
}
.akis-satir {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 8px 6px;
  border: none;
  border-bottom: 1px solid var(--border);
  background: transparent;
  color: inherit;
  text-align: left;
  cursor: pointer;
  border-radius: 8px;
  transition: background var(--dur-fast) var(--ease-standard);
}
.akis-liste li:last-child .akis-satir {
  border-bottom: none;
}
.akis-satir:hover,
.akis-satir:focus-visible {
  background: var(--bg-secondary);
}
.akis-satir:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 1px;
}
.akis-ikon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  flex-shrink: 0;
  border-radius: 9px;
  font-size: 13px;
}
.akis-metin {
  display: flex;
  flex-direction: column;
  gap: 1px;
  min-width: 0;
  flex: 1;
}
.akis-metin-baslik {
  font-size: 12.5px;
  font-weight: 600;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.akis-metin-aciklama {
  font-size: 11px;
  color: var(--text-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.akis-sag {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 1px;
  flex-shrink: 0;
}
.akis-tutar {
  font-size: 12.5px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  color: var(--text-primary);
}
.akis-zaman {
  font-size: 10.5px;
  color: var(--text-muted);
}
.akis-bos,
.akis-hata {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 6px 2px;
  color: var(--text-muted);
  font-size: 12px;
}
.akis-hata {
  color: var(--danger-text);
}
.akis-yenile {
  margin-left: auto;
  padding: 2px 8px;
  border: 1px solid var(--border);
  border-radius: 999px;
  background: transparent;
  color: var(--text-secondary);
  font-size: 11px;
  cursor: pointer;
}
.akis-tumu {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  width: 100%;
  padding: 7px 10px;
  border: 1px solid var(--border);
  border-radius: 9px;
  background: transparent;
  color: var(--text-secondary);
  font-size: 11.5px;
  font-weight: 600;
  cursor: pointer;
}
.akis-tumu:hover,
.akis-tumu:focus-visible {
  border-color: var(--accent-border);
  color: var(--accent);
}
.akis-tumu:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}
.akis-iskeletler {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.akis-iskelet {
  height: 40px;
  border-radius: 9px;
  background: var(--bg-secondary);
  animation: akis-nabiz 1.4s ease-in-out infinite;
}
@keyframes akis-nabiz {
  0%,
  100% {
    opacity: 0.55;
  }
  50% {
    opacity: 0.9;
  }
}
@media (max-width: 640px) {
  .akis-satir {
    padding: 7px 4px;
  }
  .akis-ikon {
    width: 26px;
    height: 26px;
    font-size: 12px;
  }
  .akis-metin-aciklama {
    display: none;
  }
}
/* Dokunmatik isaretci: filtre cipleri ve akis satirlari 44 px hedefe cikar. */
@media (pointer: coarse) {
  .akis-filtre {
    min-height: 44px;
    padding-left: 12px;
    padding-right: 12px;
  }
  .akis-satir {
    min-height: 44px;
    padding-left: 6px;
    padding-right: 6px;
  }
}
@media (prefers-reduced-motion: reduce) {
  .akis-iskelet {
    animation: none;
  }
  .akis-satir,
  .akis-filtre {
    transition: none;
  }
}
</style>
