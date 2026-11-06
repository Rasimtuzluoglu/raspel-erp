<template>
  <div
    v-if="visible"
    class="hizli-katman"
    data-test="hizli-tahsilat-katman"
    @click.self="kapat"
  >
    <section
      ref="panel"
      class="hizli-panel"
      role="dialog"
      aria-modal="true"
      aria-labelledby="hizli-tahsilat-baslik"
      tabindex="-1"
      @keydown.esc.stop.prevent="kapat"
    >
      <header class="hizli-ust">
        <div>
          <h2
            id="hizli-tahsilat-baslik"
            class="hizli-baslik"
          >
            {{ baslik }}
          </h2>
          <p class="hizli-alt">
            {{ t('dashboard.kokpitHizliCari') }} · {{ t('dashboard.kokpitHizliTutar') }}
          </p>
        </div>
        <button
          type="button"
          class="hizli-kapat"
          :aria-label="t('dashboard.kokpitOnayKapat')"
          data-test="hizli-tahsilat-kapat"
          @click="kapat"
        >
          <i
            class="pi pi-times"
            aria-hidden="true"
          />
        </button>
      </header>

      <div class="hizli-govde">
        <div class="hizli-alan">
          <label
            class="hizli-etiket"
            for="hizli-cari-arama"
          >{{ t('dashboard.kokpitHizliCari') }}</label>

          <div
            v-if="seciliCari"
            class="hizli-secili-cari"
          >
            <span>{{ seciliCari.ad }}</span>
            <button
              type="button"
              class="hizli-temizle"
              :aria-label="t('dashboard.kokpitTemizle')"
              @click="cariTemizle"
            >
              <i
                class="pi pi-times"
                aria-hidden="true"
              />
            </button>
          </div>

          <template v-else>
            <input
              id="hizli-cari-arama"
              ref="cariGirdi"
              v-model="cariArama"
              type="text"
              class="hizli-girdi"
              :placeholder="t('dashboard.kokpitHizliCariAra')"
              autocomplete="off"
              data-test="hizli-cari-arama"
              @focus="aramaAcik = true"
              @input="cariAra($event.target.value)"
            >
            <ul
              v-if="aramaAcik && (oneriler.length > 0 || cariYukleniyor)"
              class="hizli-oneriler"
              role="listbox"
            >
              <li
                v-for="cari in oneriler"
                :key="cari.id"
                role="option"
                :aria-selected="false"
              >
                <button
                  type="button"
                  class="hizli-oneri"
                  :data-test="`hizli-cari-${cari.id}`"
                  @click="cariSec(cari)"
                >
                  {{ cari.ad }}
                </button>
              </li>
              <li
                v-if="cariYukleniyor && oneriler.length === 0"
                class="hizli-oneri-bos"
              >
                {{ t('dashboard.kokpitYukleniyor') }}
              </li>
            </ul>
          </template>
        </div>

        <div class="hizli-satir">
          <div class="hizli-alan">
            <label
              class="hizli-etiket"
              for="hizli-tutar"
            >{{ t('dashboard.kokpitHizliTutar') }}</label>
            <input
              id="hizli-tutar"
              v-model="tutar"
              type="number"
              min="0.01"
              step="0.01"
              class="hizli-girdi"
              data-test="hizli-tutar"
            >
          </div>
          <div class="hizli-alan">
            <label
              class="hizli-etiket"
              for="hizli-tarih"
            >{{ t('dashboard.kokpitHizliTarih') }}</label>
            <input
              id="hizli-tarih"
              v-model="tarih"
              type="date"
              class="hizli-girdi"
              data-test="hizli-tarih"
            >
          </div>
        </div>

        <div class="hizli-alan">
          <label
            class="hizli-etiket"
            for="hizli-odeme-sekli"
          >{{ t('dashboard.kokpitHizliOdemeSekli') }}</label>
          <select
            id="hizli-odeme-sekli"
            v-model="odemeSekli"
            class="hizli-girdi"
            data-test="hizli-odeme-sekli"
          >
            <option
              v-for="secenek in odemeSekilleri"
              :key="secenek.value"
              :value="secenek.value"
            >
              {{ secenek.label }}
            </option>
          </select>
        </div>

        <div class="hizli-alan">
          <label
            class="hizli-etiket"
            for="hizli-aciklama"
          >{{ t('dashboard.kokpitHizliAciklama') }}</label>
          <input
            id="hizli-aciklama"
            v-model="aciklama"
            type="text"
            maxlength="250"
            class="hizli-girdi"
            data-test="hizli-aciklama"
          >
        </div>

        <p
          v-if="hata"
          class="hizli-hata"
          role="alert"
        >
          {{ hata }}
        </p>
      </div>

      <footer class="hizli-eylemler">
        <button
          type="button"
          class="hizli-buton"
          @click="kapat"
        >
          {{ t('common.cancel') }}
        </button>
        <button
          type="button"
          class="hizli-buton hizli-buton--ana"
          :disabled="kaydediliyor"
          data-test="hizli-kaydet"
          @click="gonder"
        >
          <i
            v-if="kaydediliyor"
            class="pi pi-spin pi-spinner"
            aria-hidden="true"
          />
          {{ t('dashboard.kokpitHizliKaydet') }}
        </button>
      </footer>
    </section>
  </div>
</template>

<script setup>
import { computed, nextTick, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { hareketAPI } from '../../../api/index.js'
import { useCariOnerileri, cariHesapCoz } from '../../../composables/useCariOnerileri.js'
import { useToastBildirim } from '../../../composables/useToastBildirim.js'
import { getLocalDateString } from '../../../utils/format.js'

const props = defineProps({
  visible: { type: Boolean, default: false },
  tur: { type: String, default: 'TAHSILAT' },
  cariId: { type: Number, default: null }
})

const emit = defineEmits(['update:visible', 'kaydedildi'])
const { t } = useI18n()
const toastBildirim = useToastBildirim()
const { oneriler, yukleniyor: cariYukleniyor, ara: cariAra, hemenAra } = useCariOnerileri()

const panel = ref(null)
const cariGirdi = ref(null)
const cariArama = ref('')
const seciliCari = ref(null)
const aramaAcik = ref(false)
const tutar = ref('')
const tarih = ref(getLocalDateString(new Date()))
const odemeSekli = ref('NAKIT')
const aciklama = ref('')
const kaydediliyor = ref(false)
const hata = ref('')

const baslik = computed(() =>
  props.tur === 'ODEME'
    ? t('dashboard.kokpitHizliIslemBaslikOdeme')
    : t('dashboard.kokpitHizliIslemBaslikTahsilat')
)

const odemeSekilleri = computed(() => [
  { label: t('hareketler.nakit'), value: 'NAKIT' },
  { label: t('hareketler.krediKarti'), value: 'KREDI_KARTI' },
  { label: t('hareketler.havaleEft'), value: 'HAVALE_EFT' },
  { label: t('hareketler.cek'), value: 'CEK' },
  { label: t('hareketler.senet'), value: 'SENET' },
  { label: t('hareketler.banka'), value: 'BANKA' }
])

const sifirla = () => {
  cariArama.value = ''
  seciliCari.value = null
  aramaAcik.value = false
  tutar.value = ''
  tarih.value = getLocalDateString(new Date())
  odemeSekli.value = 'NAKIT'
  aciklama.value = ''
  hata.value = ''
}

const cariSec = (cari) => {
  seciliCari.value = cari
  cariArama.value = cari?.ad || ''
  aramaAcik.value = false
}

const cariTemizle = () => {
  seciliCari.value = null
  cariArama.value = ''
  aramaAcik.value = true
  nextTick(() => cariGirdi.value?.focus?.())
}

const kapat = () => {
  emit('update:visible', false)
}

const gonder = async () => {
  if (!seciliCari.value?.id) {
    hata.value = t('dashboard.kokpitHizliCariZorunlu')
    return
  }
  const tutarSayisi = Number(tutar.value)
  if (!Number.isFinite(tutarSayisi) || tutarSayisi <= 0) {
    hata.value = t('dashboard.kokpitHizliTutarGecerli')
    return
  }
  kaydediliyor.value = true
  hata.value = ''
  try {
    await hareketAPI.create({
      cariHesapId: seciliCari.value.id,
      tur: props.tur,
      odemeSekli: odemeSekli.value || null,
      tutar: tutarSayisi,
      hareketTarihi: tarih.value || null,
      aciklama: aciklama.value || null
    })
    toastBildirim.basarili(t('dashboard.kokpitHizliKaydedildi'))
    emit('kaydedildi', {
      tur: props.tur,
      tutar: tutarSayisi,
      cariId: seciliCari.value.id
    })
    kapat()
  } catch {
    hata.value = t('dashboard.kokpitHizliKaydedilemedi')
    toastBildirim.hata(hata.value)
  } finally {
    kaydediliyor.value = false
  }
}

watch(
  () => props.visible,
  async (acik) => {
    if (!acik) return
    sifirla()
    if (props.cariId != null) {
      const cari = await cariHesapCoz(props.cariId)
      if (cari) {
        cariSec(cari)
      } else {
        aramaAcik.value = true
        await hemenAra('')
      }
    } else {
      aramaAcik.value = true
      await hemenAra('')
    }
    nextTick(() => {
      panel.value?.focus?.()
      cariGirdi.value?.focus?.()
    })
  },
  { immediate: true }
)
</script>

<style scoped>
.hizli-katman {
  position: fixed;
  inset: 0;
  z-index: var(--z-modal);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  background: rgba(2, 6, 12, 0.55);
  backdrop-filter: blur(2px);
}
.hizli-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: min(520px, 100%);
  max-height: calc(100vh - 32px);
  padding: 16px;
  overflow-y: auto;
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  background: var(--bg-card);
  box-shadow: var(--elev-2);
}
.hizli-ust {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--border);
}
.hizli-baslik {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
}
.hizli-alt {
  margin: 2px 0 0;
  font-size: 11px;
  color: var(--text-muted);
}
.hizli-kapat {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 30px;
  height: 30px;
  margin-left: auto;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
}
.hizli-kapat:hover,
.hizli-kapat:focus-visible {
  border-color: var(--accent-border);
  color: var(--accent);
}
.hizli-kapat:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 1px;
}
.hizli-govde {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.hizli-alan {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}
.hizli-satir {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}
.hizli-etiket {
  font-size: 11px;
  font-weight: 600;
  color: var(--text-muted);
}
.hizli-girdi {
  width: 100%;
  height: 34px;
  padding: 0 9px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: var(--bg-secondary);
  color: var(--text-primary);
  font-size: 12px;
}
.hizli-girdi:focus-visible {
  border-color: var(--accent-border);
  outline: 2px solid var(--accent);
  outline-offset: 1px;
}
.hizli-oneriler {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  z-index: 5;
  max-height: 180px;
  margin: 2px 0 0;
  padding: 3px;
  overflow-y: auto;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: var(--bg-card);
  box-shadow: var(--elev-2);
  list-style: none;
}
.hizli-oneri {
  width: 100%;
  padding: 6px 7px;
  border: 0;
  border-radius: var(--radius-xs);
  background: transparent;
  color: var(--text-secondary);
  font-size: 12px;
  text-align: left;
  cursor: pointer;
}
.hizli-oneri:hover,
.hizli-oneri:focus-visible {
  background: var(--bg-muted);
  color: var(--text-primary);
}
.hizli-oneri:focus-visible {
  outline: 2px solid var(--accent);
}
.hizli-oneri-bos {
  padding: 6px 7px;
  font-size: 11px;
  color: var(--text-muted);
}
.hizli-secili-cari {
  display: flex;
  align-items: center;
  gap: 7px;
  min-height: 34px;
  padding: 0 9px;
  border: 1px solid var(--accent-border);
  border-radius: var(--radius-sm);
  background: var(--accent-soft);
  color: var(--text-primary);
  font-size: 12px;
}
.hizli-secili-cari span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.hizli-temizle {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 22px;
  height: 22px;
  margin-left: auto;
  border: 0;
  border-radius: 50%;
  background: transparent;
  color: var(--text-muted);
  cursor: pointer;
}
.hizli-temizle:hover,
.hizli-temizle:focus-visible {
  color: var(--danger);
}
.hizli-hata {
  margin: 0;
  padding: 7px 9px;
  border: 1px solid var(--danger-border);
  border-radius: var(--radius-sm);
  background: var(--danger-soft);
  color: var(--danger);
  font-size: 11px;
}
.hizli-eylemler {
  display: flex;
  justify-content: flex-end;
  gap: 7px;
  padding-top: 10px;
  border-top: 1px solid var(--border);
}
.hizli-buton {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 6px 12px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--text-secondary);
  font-size: 11.5px;
  font-weight: 600;
  cursor: pointer;
}
.hizli-buton:hover:not(:disabled),
.hizli-buton:focus-visible:not(:disabled) {
  border-color: var(--accent-border);
  color: var(--accent);
}
.hizli-buton:disabled {
  opacity: 0.6;
  cursor: default;
}
.hizli-buton--ana {
  border-color: var(--accent-border);
  background: var(--accent);
  color: var(--accent-contrast);
}
.hizli-buton--ana:hover:not(:disabled),
.hizli-buton--ana:focus-visible:not(:disabled) {
  background: var(--accent-hover);
  color: var(--accent-contrast);
}
.hizli-buton:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}
@media (max-width: 520px) {
  .hizli-panel {
    padding: 13px;
  }
  .hizli-satir {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
