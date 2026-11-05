<template>
  <header class="ust-bar">
    <div class="ust-sol">
      <p class="ust-selam">
        {{ selamlama }},
        <strong>{{ kullaniciAd || '—' }}</strong>
      </p>
      <p class="ust-alt">
        <span
          v-if="sirketAd"
          class="ust-sirket"
        >{{ sirketAd }} · </span>
        <span>{{ bugunMetni }}</span>
        <span
          v-if="sonGuncelleme"
          class="ust-guncelleme"
        > · {{ t('dashboard.sonGuncellemeKisa', { zaman: sonGuncelleme }) }}</span>
        <span
          class="ust-canli"
          :class="canliDurumSinifi"
          data-test="canli-durum"
        >
          <span class="canli-nokta" />
          {{ canliMetni }}
        </span>
      </p>
    </div>

    <div class="ust-sag">
      <!-- Donem secici: KPI'lar secilen doneme gore yeniden yuklenir. -->
      <div class="ust-donem">
        <select
          class="ust-donem-secim"
          :value="donem"
          :aria-label="t('dashboard.donem')"
          :title="t('dashboard.donem')"
          data-test="donem-secici"
          @change="donemSec"
        >
          <option
            v-for="secenek in DONEM_SECENEKLERI"
            :key="secenek.kod"
            :value="secenek.kod"
          >
            {{ t(secenek.anahtar) }}
          </option>
        </select>
        <i
          class="pi pi-angle-down ust-donem-ikon"
          aria-hidden="true"
        />
      </div>

      <!-- Bildirim zili: gorev sayaclarinin toplami + ilgili ekran baglantilari. -->
      <div
        ref="zilKok"
        class="ust-zil-alan"
      >
        <button
          ref="zilButon"
          type="button"
          class="ust-buton ust-buton--ikon ust-zil"
          :class="{ 'ust-buton--secili': zilAcik }"
          :aria-label="zilEtiketi"
          :title="zilEtiketi"
          aria-haspopup="true"
          :aria-expanded="zilAcik ? 'true' : 'false'"
          aria-controls="db-bildirim-menusu"
          data-test="bildirim-zili"
          @click="zilDegistir"
        >
          <i
            class="pi pi-bell"
            aria-hidden="true"
          />
          <span
            v-if="toplamBildirim > 0"
            class="ust-zil-rozet"
            data-test="bildirim-rozet"
            aria-hidden="true"
          >{{ rozetMetni }}</span>
        </button>

        <div
          v-if="zilAcik"
          id="db-bildirim-menusu"
          class="ust-zil-menu"
          role="menu"
          :aria-label="t('dashboard.bildirimler')"
          data-test="bildirim-menusu"
          @keydown.esc.stop.prevent="zilKapat(true)"
        >
          <p class="ust-zil-baslik">
            {{ t('dashboard.bildirimler') }}
          </p>
          <router-link
            v-for="oge in bildirimOgeleri"
            :key="oge.kod"
            :to="oge.to"
            class="ust-zil-oge"
            role="menuitem"
            :data-test="`bildirim-${oge.kod}`"
            @click="zilKapat(false)"
          >
            <i
              :class="oge.ikon"
              aria-hidden="true"
            />
            <span class="ust-zil-oge-metin">{{ t(oge.anahtar) }}</span>
            <strong class="ust-zil-oge-adet">{{ oge.adet }}</strong>
          </router-link>
        </div>
      </div>

      <!-- Kisisel notlar: sunucu tarafli /api/notlar modulunu acar. -->
      <button
        type="button"
        class="ust-buton ust-buton--ikon ust-buton--masaustu"
        :aria-label="t('dashboard.notlarAc')"
        :title="t('dashboard.notlarAc')"
        data-test="not-buton"
        @click="emit('notlar')"
      >
        <i
          class="pi pi-clipboard"
          aria-hidden="true"
        />
      </button>

      <!-- Excel disa aktarma: secili donem ile /exports/dashboard -->
      <button
        type="button"
        class="ust-buton ust-buton--ikon ust-buton--masaustu"
        :aria-label="t('dashboard.excelIndir')"
        :title="t('dashboard.excelIndir')"
        :disabled="excelIndiriliyor"
        data-test="excel-indir"
        @click="excelIndir"
      >
        <i
          class="pi"
          :class="excelIndiriliyor ? 'pi-spin pi-spinner' : 'pi-file-excel'"
          aria-hidden="true"
        />
      </button>

      <!-- Yazdirma / PDF: tarayicinin kendi print akisi kullanilir. -->
      <button
        type="button"
        class="ust-buton ust-buton--ikon ust-buton--masaustu"
        :aria-label="t('dashboard.yazdirAc')"
        :title="t('dashboard.yazdirAc')"
        data-test="yazdir"
        @click="yazdir"
      >
        <i
          class="pi pi-print"
          aria-hidden="true"
        />
      </button>

      <!-- Kar analizi: secili donem icin urun/cari bazli kar-marj drawer'i -->
      <button
        type="button"
        class="ust-buton ust-buton--ikon ust-buton--masaustu"
        :aria-label="t('dashboard.karAnaliziAc')"
        :title="t('dashboard.karAnaliziAc')"
        data-test="kar-analizi-buton"
        @click="emit('kar-analizi')"
      >
        <i
          class="pi pi-chart-line"
          aria-hidden="true"
        />
      </button>

      <!-- Vade takvimi -->
      <button
        type="button"
        class="ust-buton ust-buton--ikon ust-buton--masaustu"
        :aria-label="t('dashboard.vadeTakvimiAc')"
        :title="t('dashboard.vadeTakvimiAc')"
        data-test="vade-takvimi-buton"
        @click="emit('takvim')"
      >
        <i
          class="pi pi-calendar"
          aria-hidden="true"
        />
      </button>

      <!-- Canli guncelleme anahtari: STOMP aboneligi; kopukken 60 sn yedek polling. -->
      <button
        type="button"
        class="ust-buton ust-buton--ikon ust-oto ust-buton--masaustu"
        :class="{ 'ust-buton--secili': otoYenile }"
        role="switch"
        :aria-checked="otoYenile ? 'true' : 'false'"
        :aria-label="otoYenile ? t('dashboard.otoYenilemeKapat') : t('dashboard.otoYenilemeAc')"
        :title="otoYenile ? t('dashboard.otoYenilemeKapat') : t('dashboard.otoYenilemeAc')"
        data-test="oto-yenileme"
        @click="emit('oto-degistir', !otoYenile)"
      >
        <i
          :class="otoYenile ? 'pi pi-sync' : 'pi pi-history'"
          aria-hidden="true"
        />
      </button>

      <button
        type="button"
        class="ust-buton ust-buton--ara"
        :title="t('dashboard.heroAra')"
        @click="emit('ara')"
      >
        <i
          class="pi pi-search"
          aria-hidden="true"
        />
        <span class="ust-ara-metin">{{ t('dashboard.heroAra') }}</span>
        <kbd aria-hidden="true">Ctrl K</kbd>
      </button>
      <button
        type="button"
        class="ust-buton ust-buton--ikon ust-buton--masaustu"
        :aria-label="t('dashboard.yenile')"
        :title="t('dashboard.yenile')"
        :disabled="yenileniyor"
        @click="emit('yenile')"
      >
        <i
          class="pi"
          :class="yenileniyor ? 'pi-spin pi-spinner' : 'pi-refresh'"
          aria-hidden="true"
        />
      </button>
      <button
        type="button"
        class="ust-buton ust-buton--ikon ust-buton--masaustu"
        :class="{ 'ust-buton--aktif': duzenModu }"
        :aria-label="duzenModu ? t('dashboard.duzenBitir') : t('dashboard.duzenle')"
        :title="duzenModu ? t('dashboard.duzenBitir') : t('dashboard.duzenle')"
        :aria-pressed="duzenModu"
        @click="emit('duzenle')"
      >
        <i
          class="pi pi-sliders-h"
          aria-hidden="true"
        />
      </button>

      <!--
        Mobil (<=760px): masaustu ikonlari CSS ile gizlenir; ayni eylemler
        "Daha fazla" popover'inda 44px satirlar olarak sunulur. Popover
        DOM'da her zaman yoktur; yalnizca acikken render edilir.
      -->
      <div
        ref="mobilAlan"
        class="ust-mobil-alan"
      >
        <button
          ref="mobilDaha"
          type="button"
          class="ust-buton ust-buton--ikon ust-mobil-daha"
          :class="{ 'ust-buton--secili': mobilMenuAcik }"
          :aria-label="t('dashboard.dahaFazlaIslem')"
          :title="t('dashboard.dahaFazlaIslem')"
          aria-haspopup="dialog"
          :aria-expanded="mobilMenuAcik ? 'true' : 'false'"
          aria-controls="db-mobil-menu"
          data-test="mobil-daha"
          @click="mobilMenuDegistir"
        >
          <i
            class="pi pi-ellipsis-h"
            aria-hidden="true"
          />
        </button>

        <div
          v-if="mobilMenuAcik"
          id="db-mobil-menu"
          ref="mobilMenu"
          class="ust-mobil-menu"
          role="dialog"
          aria-modal="false"
          :aria-label="t('dashboard.dahaFazlaIslem')"
          data-test="mobil-menu"
          @keydown.esc.stop.prevent="mobilMenuKapat(true)"
          @keydown.tab="mobilOdakDongusu"
        >
          <button
            type="button"
            class="ust-mobil-satir"
            data-test="mobil-aksiyon-notlar"
            @click="mobilEylem('notlar')"
          >
            <i
              class="pi pi-clipboard"
              aria-hidden="true"
            />
            <span class="ust-mobil-etiket">{{ t('dashboard.notlar') }}</span>
          </button>

          <button
            type="button"
            class="ust-mobil-satir"
            data-test="mobil-aksiyon-takvim"
            @click="mobilEylem('takvim')"
          >
            <i
              class="pi pi-calendar"
              aria-hidden="true"
            />
            <span class="ust-mobil-etiket">{{ t('dashboard.vadeTakvimi') }}</span>
          </button>

          <button
            type="button"
            class="ust-mobil-satir"
            data-test="mobil-aksiyon-kar"
            @click="mobilEylem('kar')"
          >
            <i
              class="pi pi-chart-line"
              aria-hidden="true"
            />
            <span class="ust-mobil-etiket">{{ t('dashboard.karAnalizi') }}</span>
          </button>

          <button
            type="button"
            class="ust-mobil-satir"
            :disabled="excelIndiriliyor"
            data-test="mobil-aksiyon-excel"
            @click="mobilExcel"
          >
            <i
              class="pi"
              :class="excelIndiriliyor ? 'pi-spin pi-spinner' : 'pi-file-excel'"
              aria-hidden="true"
            />
            <span class="ust-mobil-etiket">{{ t('dashboard.excelIndir') }}</span>
          </button>

          <button
            type="button"
            class="ust-mobil-satir"
            data-test="mobil-aksiyon-yazdir"
            @click="mobilEylem('yazdir')"
          >
            <i
              class="pi pi-print"
              aria-hidden="true"
            />
            <span class="ust-mobil-etiket">{{ t('dashboard.yazdirAc') }}</span>
          </button>

          <button
            type="button"
            class="ust-mobil-satir ust-mobil-satir--anahtar"
            role="switch"
            :aria-checked="otoYenile ? 'true' : 'false'"
            :aria-label="otoYenile ? t('dashboard.otoYenilemeKapat') : t('dashboard.otoYenilemeAc')"
            data-test="mobil-aksiyon-canli"
            @click="emit('oto-degistir', !otoYenile)"
          >
            <i
              class="pi pi-sync"
              aria-hidden="true"
            />
            <span class="ust-mobil-etiket">{{ t('dashboard.canliMod') }}</span>
            <span
              class="ust-mobil-anahtar"
              :class="{ 'ust-mobil-anahtar--acik': otoYenile }"
              aria-hidden="true"
            ><span class="ust-mobil-knob" /></span>
          </button>

          <button
            type="button"
            class="ust-mobil-satir"
            :disabled="yenileniyor"
            data-test="mobil-aksiyon-yenile"
            @click="mobilEylem('yenile')"
          >
            <i
              class="pi"
              :class="yenileniyor ? 'pi-spin pi-spinner' : 'pi-refresh'"
              aria-hidden="true"
            />
            <span class="ust-mobil-etiket">{{ t('dashboard.yenile') }}</span>
          </button>

          <button
            type="button"
            class="ust-mobil-satir"
            :class="{ 'ust-mobil-satir--aktif': duzenModu }"
            :aria-pressed="duzenModu"
            data-test="mobil-aksiyon-duzenle"
            @click="mobilEylem('duzenle')"
          >
            <i
              class="pi pi-sliders-h"
              aria-hidden="true"
            />
            <span class="ust-mobil-etiket">{{ duzenModu ? t('dashboard.duzenBitir') : t('dashboard.duzenle') }}</span>
          </button>
        </div>
      </div>
    </div>
  </header>
</template>

<script setup>
import { computed, inject, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { PrimeVueToastSymbol } from 'primevue/usetoast'
import { excelAPI } from '../../api/index.js'

const props = defineProps({
  kullaniciAd: { type: String, default: '' },
  sirketAd: { type: String, default: '' },
  canli: { type: Boolean, default: true },
  bagli: { type: Boolean, default: true },
  sonGuncelleme: { type: String, default: '' },
  yenileniyor: { type: Boolean, default: false },
  duzenModu: { type: Boolean, default: false },
  donem: { type: String, default: 'BU_AY' },
  gorevSayilari: { type: Object, default: () => ({}) },
  otoYenile: { type: Boolean, default: false }
})

const emit = defineEmits([
  'yenile',
  'ara',
  'duzenle',
  'donem-degistir',
  'takvim',
  'kar-analizi',
  'oto-degistir',
  'notlar',
  'excel-indirildi'
])

const { t } = useI18n()

// Toast opsiyoneldir: saglayici yoksa (test/izole kullanim) sessiz gecer.
const toast = inject(PrimeVueToastSymbol, null)
const bildir = (severity, detay) => {
  if (!toast || typeof toast.add !== 'function') return
  toast.add({ severity, summary: t('dashboard.excelIndir'), detail: detay, life: 3000 })
}

/** Backend donem kodlari; sirasi secicide gorunen siradir. */
const DONEM_SECENEKLERI = [
  { kod: 'BU_AY', anahtar: 'dashboard.donemBuAy' },
  { kod: 'GECEN_AY', anahtar: 'dashboard.donemGecenAy' },
  { kod: 'BU_CEYREK', anahtar: 'dashboard.donemBuCeyrek' },
  { kod: 'BU_YIL', anahtar: 'dashboard.donemBuYil' }
]

// Canli mod gostergesi: bagliyken yesil nabiz, kopukken amber "yeniden baglaniyor".
const canliDurumSinifi = computed(() => {
  if (!props.canli) return 'ust-canli--kapali'
  return props.bagli ? '' : 'ust-canli--bekliyor'
})

const canliMetni = computed(() => {
  if (!props.canli) return t('dashboard.cevrimdisiVeri')
  return props.bagli ? t('dashboard.canli') : t('dashboard.yenidenBaglaniyor')
})

const selamlama = computed(() => {  const saat = new Date().getHours()
  if (saat < 6) return t('dashboard.iyiGeceler')
  if (saat < 12) return t('dashboard.gunaydin')
  if (saat < 18) return t('dashboard.iyiGunler')
  return t('dashboard.iyiAksamlar')
})

// "28 Ekim 2026 Çarşamba"
const bugunMetni = computed(() =>
  new Intl.DateTimeFormat('tr-TR', {
    day: 'numeric',
    month: 'long',
    year: 'numeric',
    weekday: 'long'
  }).format(new Date())
)

const donemSec = (olay) => emit('donem-degistir', olay.target.value)

// --- Bildirim zili ---------------------------------------------------------
const sayi = (deger) => {
  const n = Number(deger)
  return Number.isFinite(n) && n > 0 ? Math.floor(n) : 0
}

const bildirimOgeleri = computed(() => [
  {
    kod: 'onayBekleyen',
    anahtar: 'dashboard.bildirimOnayBekleyen',
    to: '/onaylar',
    ikon: 'pi pi-check-square',
    adet: sayi(props.gorevSayilari?.onayBekleyen)
  },
  {
    kod: 'vadesiGecen',
    anahtar: 'dashboard.bildirimVadesiGecen',
    to: '/tahsilat',
    ikon: 'pi pi-clock',
    adet: sayi(props.gorevSayilari?.vadesiGecen)
  },
  {
    kod: 'kritikStok',
    anahtar: 'dashboard.bildirimKritikStok',
    to: '/stoklar?kritik=1',
    ikon: 'pi pi-exclamation-triangle',
    adet: sayi(props.gorevSayilari?.kritikStok)
  }
])

const toplamBildirim = computed(() =>
  bildirimOgeleri.value.reduce((toplam, oge) => toplam + oge.adet, 0)
)

const rozetMetni = computed(() => (toplamBildirim.value > 99 ? '99+' : String(toplamBildirim.value)))

const zilEtiketi = computed(() => `${t('dashboard.bildirimler')} (${toplamBildirim.value})`)

const zilAcik = ref(false)
const zilKok = ref(null)
const zilButon = ref(null)

const zilKapat = (odakla = false) => {
  zilAcik.value = false
  if (odakla) zilButon.value?.focus?.()
}

const zilDegistir = () => {
  zilAcik.value = !zilAcik.value
}

const disariTikla = (olay) => {
  const kok = zilKok.value
  if (!kok || kok.contains?.(olay.target)) return
  zilKapat(false)
}

const tusKacisi = (olay) => {
  if (olay.key === 'Escape') zilKapat(true)
}

// Dinleyiciler yalnizca menu acikken baglanir; kapaninca/kaldirilinca temizlenir.
watch(zilAcik, (acik) => {
  if (typeof document === 'undefined') return
  if (acik) {
    document.addEventListener('click', disariTikla, true)
    document.addEventListener('keydown', tusKacisi)
  } else {
    document.removeEventListener('click', disariTikla, true)
    document.removeEventListener('keydown', tusKacisi)
  }
})

// --- Mobil "Daha fazla" menusu ---------------------------------------------
// Masaustu ikonlari CSS ile gizlenir; mobilde ayni emit'ler buradan tetiklenir.
const mobilMenuAcik = ref(false)
const mobilAlan = ref(null)
const mobilMenu = ref(null)
const mobilDaha = ref(null)

const mobilMenuKapat = (odakla = false) => {
  mobilMenuAcik.value = false
  if (odakla) mobilDaha.value?.focus?.()
}

const mobilMenuDegistir = () => {
  mobilMenuAcik.value = !mobilMenuAcik.value
}

const mobilDisariTikla = (olay) => {
  const kok = mobilAlan.value
  if (!kok || kok.contains?.(olay.target)) return
  mobilMenuKapat(false)
}

const mobilTusKacisi = (olay) => {
  if (olay.key === 'Escape') mobilMenuKapat(true)
}

/** Popover acikken Tab ilk/son oge arasinda dongusel kalir. */
const mobilOdakDongusu = (olay) => {
  const kok = mobilMenu.value
  if (!kok) return
  const odaklanabilir = [...kok.querySelectorAll('button:not([disabled])')]
  if (odaklanabilir.length === 0) return
  const ilk = odaklanabilir[0]
  const son = odaklanabilir[odaklanabilir.length - 1]
  const aktif = document.activeElement
  if (olay.shiftKey && (aktif === ilk || !kok.contains(aktif))) {
    olay.preventDefault()
    son.focus()
  } else if (!olay.shiftKey && (aktif === son || !kok.contains(aktif))) {
    olay.preventDefault()
    ilk.focus()
  }
}

const mobilEylem = (ad) => {
  mobilMenuKapat()
  if (ad === 'notlar') emit('notlar')
  else if (ad === 'takvim') emit('takvim')
  else if (ad === 'kar') emit('kar-analizi')
  else if (ad === 'yazdir') yazdir()
  else if (ad === 'yenile') emit('yenile')
  else if (ad === 'duzenle') emit('duzenle')
}

const mobilExcel = async () => {
  await excelIndir()
  mobilMenuKapat()
}

watch(mobilMenuAcik, async (acik) => {
  if (typeof document === 'undefined') return
  if (acik) {
    document.addEventListener('click', mobilDisariTikla, true)
    document.addEventListener('keydown', mobilTusKacisi)
    // Odak menuye tasinir: klavye kullanicisi ilk satirdan devam eder.
    await nextTick()
    mobilMenu.value?.querySelector('button:not([disabled])')?.focus?.()
  } else {
    document.removeEventListener('click', mobilDisariTikla, true)
    document.removeEventListener('keydown', mobilTusKacisi)
  }
})

// --- Excel disa aktarma ----------------------------------------------------
const excelIndiriliyor = ref(false)

const excelDosyaAdi = () => {
  const tarih = new Date()
  const yil = tarih.getFullYear()
  const ay = String(tarih.getMonth() + 1).padStart(2, '0')
  const gun = String(tarih.getDate()).padStart(2, '0')
  return `dashboard-${yil}${ay}${gun}.xlsx`
}

const blobIndir = (blob, dosyaAdi) => {
  if (typeof document === 'undefined' || typeof URL === 'undefined') return
  if (typeof URL.createObjectURL !== 'function') return
  const url = URL.createObjectURL(blob)
  try {
    const baglanti = document.createElement('a')
    baglanti.href = url
    baglanti.download = dosyaAdi
    baglanti.rel = 'noopener'
    document.body.appendChild(baglanti)
    baglanti.click()
    baglanti.remove()
  } finally {
    if (typeof URL.revokeObjectURL === 'function') URL.revokeObjectURL(url)
  }
}

const excelIndir = async () => {
  if (excelIndiriliyor.value) return
  excelIndiriliyor.value = true
  try {
    const cevap = await excelAPI.dashboard({ donem: props.donem })
    const veri = cevap?.data
    const blob = typeof Blob !== 'undefined' && veri instanceof Blob ? veri : new Blob([veri ?? ''])
    blobIndir(blob, excelDosyaAdi())
    bildir('success', t('dashboard.excelIndirildi'))
    emit('excel-indirildi')
  } catch {
    bildir('error', t('dashboard.excelHata'))
  } finally {
    excelIndiriliyor.value = false
  }
}

// --- Yazdirma / PDF --------------------------------------------------------
const yazdir = () => {
  if (typeof window !== 'undefined' && typeof window.print === 'function') window.print()
}

onBeforeUnmount(() => {
  if (typeof document === 'undefined') return
  document.removeEventListener('click', disariTikla, true)
  document.removeEventListener('keydown', tusKacisi)
  document.removeEventListener('click', mobilDisariTikla, true)
  document.removeEventListener('keydown', mobilTusKacisi)
})

</script>

<style scoped>
/*
 * Ust bar sade bir baslik alanidir: kart degil, cerceve/arka plan/golge yok.
 * Yukseklik ~64px; sol tarafta selamlama + tek satir baglam, sagda eylemler.
 */
.ust-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  min-height: 64px;
  padding: 6px 0;
  min-width: 0;
}
.ust-sol {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}
.ust-selam {
  margin: 0;
  font-size: 17px;
  font-weight: 600;
  line-height: 1.3;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.ust-selam strong {
  font-weight: 700;
}
.ust-alt {
  display: flex;
  align-items: center;
  gap: 0;
  flex-wrap: wrap;
  margin: 0;
  font-size: 12px;
  line-height: 1.4;
  color: var(--text-muted);
  min-width: 0;
}
.ust-canli {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  margin-left: 10px;
  font-size: 11px;
  font-weight: 600;
  color: var(--success);
}
.ust-canli--kapali {
  color: var(--warning);
}
.ust-canli--bekliyor {
  color: var(--warning);
}
.ust-canli--bekliyor .canli-nokta {
  animation: canliNabizBekliyor 1.8s infinite;
}
@keyframes canliNabizBekliyor {
  0% {
    box-shadow: 0 0 0 0 rgba(245, 158, 11, 0.45);
  }
  70% {
    box-shadow: 0 0 0 6px rgba(245, 158, 11, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(245, 158, 11, 0);
  }
}
.canli-nokta {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentcolor;
  animation: canliNabiz 1.8s infinite;
}
@keyframes canliNabiz {
  0% {
    box-shadow: 0 0 0 0 var(--success-soft);
  }
  70% {
    box-shadow: 0 0 0 6px rgba(16, 185, 129, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(16, 185, 129, 0);
  }
}
.ust-sag {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  flex-wrap: wrap;
  min-width: 0;
}
.ust-buton {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 34px;
  padding: 0 10px;
  border-radius: 9px;
  border: 1px solid transparent;
  background: transparent;
  color: var(--text-secondary);
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
  transition:
    background var(--dur-base, 0.2s) var(--ease-standard, ease),
    color var(--dur-base, 0.2s) var(--ease-standard, ease),
    border-color var(--dur-base, 0.2s) var(--ease-standard, ease);
}
.ust-buton:hover:not(:disabled) {
  color: var(--text-primary);
  border-color: var(--border);
  background: var(--bg-muted);
}
.ust-buton:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}
.ust-buton:disabled {
  opacity: 0.65;
  cursor: progress;
}
.ust-buton--ikon {
  width: 34px;
  padding: 0;
}
.ust-buton--aktif {
  color: var(--accent-contrast);
  background: var(--accent);
  border-color: var(--accent);
}
.ust-buton--secili {
  color: var(--accent);
  background: var(--accent-soft);
  border-color: var(--accent-border);
}
.ust-buton--ara {
  border-color: var(--border);
}
.ust-buton kbd {
  padding: 1px 6px;
  border-radius: 6px;
  border: 1px solid var(--border);
  background: var(--bg-muted);
  font-size: 10px;
  font-weight: 700;
  color: var(--text-muted);
}

/* --- Donem secici -------------------------------------------------------- */
.ust-donem {
  position: relative;
  display: inline-flex;
  align-items: center;
  flex-shrink: 0;
}
.ust-donem-secim {
  appearance: none;
  -webkit-appearance: none;
  min-height: 34px;
  padding: 0 26px 0 10px;
  border-radius: 9px;
  border: 1px solid var(--border);
  background: var(--bg-muted);
  color: var(--text-secondary);
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
  transition:
    border-color var(--dur-base, 0.2s) var(--ease-standard, ease),
    color var(--dur-base, 0.2s) var(--ease-standard, ease);
}
.ust-donem-secim:hover {
  color: var(--text-primary);
  border-color: var(--accent-border);
}
.ust-donem-secim:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}
.ust-donem-secim option {
  background: var(--bg-card);
  color: var(--text-primary);
}
.ust-donem-ikon {
  position: absolute;
  right: 9px;
  font-size: 10px;
  color: var(--text-muted);
  pointer-events: none;
}

/* --- Bildirim zili ------------------------------------------------------- */
.ust-zil-alan {
  position: relative;
  display: inline-flex;
  flex-shrink: 0;
}
.ust-zil-rozet {
  position: absolute;
  top: -3px;
  right: -3px;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  border-radius: 999px;
  border: 1px solid var(--bg-card);
  /* Koyu kirmizi zemin: beyaz metinle 4.5:1 uzeri kontrast. */
  background: #b91c1c;
  color: #fff;
  font-size: 9.5px;
  font-weight: 800;
  line-height: 14px;
  text-align: center;
  font-variant-numeric: tabular-nums;
}
.ust-zil-menu {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  z-index: 90;
  width: 248px;
  padding: 6px;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: var(--bg-card);
  box-shadow: var(--elev-3, var(--elev-2));
}
.ust-zil-baslik {
  margin: 0;
  padding: 4px 8px 6px;
  font-size: 10.5px;
  font-weight: 700;
  letter-spacing: 0.03em;
  text-transform: uppercase;
  color: var(--text-muted);
}
.ust-zil-oge {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 34px;
  padding: 0 8px;
  border-radius: 8px;
  color: var(--text-secondary);
  font-size: 12.5px;
  font-weight: 600;
  text-decoration: none;
}
.ust-zil-oge:hover {
  background: var(--bg-muted);
  color: var(--text-primary);
}
.ust-zil-oge:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: -2px;
}
.ust-zil-oge i {
  font-size: 12px;
  color: var(--accent);
}
.ust-zil-oge-metin {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.ust-zil-oge-adet {
  flex-shrink: 0;
  min-width: 22px;
  padding: 1px 6px;
  border-radius: 999px;
  background: var(--bg-muted);
  color: var(--text-primary);
  font-size: 11px;
  font-weight: 800;
  text-align: center;
  font-variant-numeric: tabular-nums;
}

@media (prefers-reduced-motion: reduce) {
  .canli-nokta {
    animation: none;
  }
  .ust-buton,
  .ust-donem-secim {
    transition: none;
  }
}
/* --- Mobil "Daha fazla" menusu ------------------------------------------ */
/*
 * Popover yalnizca mobilde gorunur. Masaustunde tetikleyici ve menu DOM'da
 * olabilir ama CSS ile gizlenir; masaustu ikon seti birebir korunur.
 */
.ust-mobil-alan {
  position: relative;
  display: none;
  flex-shrink: 0;
}
.ust-mobil-menu {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  z-index: 95;
  display: flex;
  flex-direction: column;
  gap: 2px;
  width: min(300px, calc(100vw - 24px));
  padding: 6px;
  border: 1px solid var(--border);
  border-radius: 14px;
  background: var(--bg-card);
  box-shadow: var(--elev-3, var(--elev-2));
}
.ust-mobil-satir {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  min-height: 44px;
  padding: 0 10px;
  border: 1px solid transparent;
  border-radius: 10px;
  background: transparent;
  color: var(--text-secondary);
  font-family: inherit;
  font-size: 13.5px;
  font-weight: 600;
  text-align: left;
  cursor: pointer;
  transition:
    background var(--dur-base, 0.2s) var(--ease-standard, ease),
    color var(--dur-base, 0.2s) var(--ease-standard, ease);
}
.ust-mobil-satir:hover:not(:disabled) {
  background: var(--bg-muted);
  color: var(--text-primary);
}
.ust-mobil-satir:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: -2px;
}
.ust-mobil-satir:disabled {
  opacity: 0.6;
  cursor: progress;
}
.ust-mobil-satir > i {
  width: 18px;
  flex-shrink: 0;
  text-align: center;
  font-size: 13px;
  color: var(--accent);
}
.ust-mobil-etiket {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.ust-mobil-satir--aktif {
  background: var(--accent-soft);
  color: var(--accent);
}
/* Canli mod anahtari: satir butonunun saginda gorsel switch. */
.ust-mobil-anahtar {
  position: relative;
  display: inline-flex;
  align-items: center;
  flex-shrink: 0;
  width: 38px;
  height: 22px;
  padding: 2px;
  border-radius: 999px;
  background: var(--bg-muted);
  border: 1px solid var(--border);
  transition: background var(--dur-base, 0.2s) var(--ease-standard, ease);
}
.ust-mobil-anahtar--acik {
  background: var(--accent);
  border-color: var(--accent);
}
.ust-mobil-knob {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: var(--bg-card);
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.25);
  transform: translateX(0);
  transition: transform var(--dur-base, 0.2s) var(--ease-standard, ease);
}
.ust-mobil-anahtar--acik .ust-mobil-knob {
  transform: translateX(16px);
}

@media (prefers-reduced-motion: reduce) {
  .ust-mobil-satir,
  .ust-mobil-anahtar,
  .ust-mobil-knob {
    transition: none;
  }
}

/* --- Mobil yerlesim (<=760px) ------------------------------------------- */
@media (max-width: 900px) {
  .ust-bar {
    flex-direction: column;
    align-items: stretch;
    gap: 8px;
    padding-bottom: 10px;
  }
  .ust-sag {
    justify-content: flex-start;
  }
}

@media (max-width: 760px) {
  .ust-bar {
    gap: 6px;
    padding-bottom: 12px;
  }
  /* Canli/cevrimdisi durumu kendi satirinda kalir (tasma yok). */
  .ust-canli {
    flex-basis: 100%;
    margin-left: 0;
    margin-top: 2px;
  }
  .ust-sag {
    flex: 1 1 100%;
    width: 100%;
    flex-wrap: wrap;
    gap: 6px;
    justify-content: flex-start;
  }
  /* Donem secici tam satir. */
  .ust-donem {
    flex: 1 1 100%;
    order: 0;
  }
  .ust-donem-secim {
    width: 100%;
    max-width: none;
    min-height: 44px;
    /* iOS otomatik yakinlastirmasini engellemek icin 16px. */
    font-size: 16px;
  }
  .ust-buton {
    min-height: 44px;
  }
  .ust-buton--ikon {
    width: 44px;
  }
  .ust-buton--ara {
    order: 1;
    width: 44px;
    min-height: 44px;
    margin-left: auto;
    padding: 0;
    border-color: transparent;
  }
  .ust-ara-metin,
  .ust-buton--ara kbd {
    display: none;
  }
  .ust-zil-alan {
    order: 2;
  }
  .ust-zil {
    width: 44px;
    min-height: 44px;
  }
  .ust-zil-menu {
    width: min(300px, calc(100vw - 24px));
  }
  .ust-mobil-alan {
    display: inline-flex;
    order: 3;
  }
  .ust-mobil-daha {
    width: 44px;
    min-height: 44px;
  }
  /* Masaustu ikonlari DOM'da kalir; yalnizca mobilde gizlenir. */
  .ust-buton--masaustu {
    display: none;
  }
}

@media (max-width: 560px) {
  .ust-selam {
    font-size: 16px;
  }
  .ust-donem-secim {
    max-width: none;
  }
}
</style>
