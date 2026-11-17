<template>
  <AppDialog
    :visible="visible"
    :header="t('sahaPortali.imzaTeslimBaslik')"
    width="560px"
    max-width="96vw"
    content-max-height="min(74vh, 680px)"
    class="saha-imza-dialog"
    @update:visible="emit('update:visible', $event)"
  >
    <div
      v-if="visible"
      class="saha-imza-sheet"
      data-test="saha-imza-sheet"
    >
      <p class="saha-imza-ozet">
        <strong>{{ teslimat?.faturaNumarasi || `#${teslimat?.id ?? '-'}` }}</strong>
        <span v-if="teslimat?.musteriAdi">· {{ teslimat.musteriAdi }}</span>
        <span
          v-if="teslimat?.teslimatAdresi"
          class="saha-imza-adres"
        >· {{ teslimat.teslimatAdresi }}</span>
      </p>

      <label class="saha-imza-alan">
        <span class="saha-imza-etiket">{{ t('sahaPortali.teslimAlanZorunlu') }}</span>
        <input
          v-model="teslimAlanAd"
          type="text"
          class="saha-imza-girdi"
          data-test="saha-imza-ad"
          autocomplete="name"
          required
          aria-required="true"
          :placeholder="t('sahaPortali.teslimAlanPlaceholder')"
        >
      </label>

      <label class="saha-imza-alan">
        <span class="saha-imza-etiket">{{ t('sahaPortali.teslimatNotu') }}</span>
        <textarea
          v-model="teslimNotu"
          class="saha-imza-girdi saha-imza-metin"
          data-test="saha-imza-not"
          rows="2"
          :placeholder="t('sahaPortali.teslimatNotuPlaceholder')"
        />
      </label>

      <div class="saha-imza-pad">
        <ImzaPad
          ref="imzaPadRef"
          :etiket="t('sahaPortali.dijitalImzaEtiket')"
          @imza-degisti="imzaDataUrl = $event"
        />
        <small
          v-if="!imzaDataUrl"
          class="saha-imza-uyari"
        >{{ t('sahaPortali.imzaZorunlu') }}</small>
      </div>

      <div class="saha-imza-foto">
        <span class="saha-imza-etiket">{{ t('sahaPortali.imzaFotoOpsiyonel') }}</span>
        <input
          ref="fotoGirdi"
          type="file"
          accept="image/*"
          capture="environment"
          class="saha-imza-dosya"
          data-test="saha-imza-foto-girdi"
          @change="fotografSecildi"
        >
        <button
          type="button"
          class="saha-imza-ikincil"
          data-test="saha-imza-foto-sec"
          @click="fotoGirdi?.click()"
        >
          <i
            class="pi pi-camera"
            aria-hidden="true"
          />
          <span>{{ t('sahaPortali.fotografSec') }}</span>
        </button>

        <div
          v-if="fotoDataUrl"
          class="saha-imza-foto-onizleme"
        >
          <img
            :src="fotoDataUrl"
            alt=""
            class="saha-imza-foto-gorsel"
          >
          <button
            type="button"
            class="saha-imza-ikincil"
            data-test="saha-imza-foto-kaldir"
            @click="fotografiKaldir"
          >
            {{ t('sahaPortali.fotografKaldir') }}
          </button>
        </div>

        <small
          v-if="uyari"
          class="saha-imza-uyari saha-imza-uyari--hata"
          role="alert"
        >{{ uyari }}</small>
      </div>

      <p
        v-if="!cevrimici"
        class="saha-imza-cevrimdisi"
        role="status"
      >
        <i
          class="pi pi-cloud"
          aria-hidden="true"
        />
        {{ t('sahaPortali.imzaCevrimdisiIpucu') }}
      </p>
    </div>

    <template #footer>
      <button
        type="button"
        class="saha-imza-vazgec"
        data-test="saha-imza-kapat"
        @click="kapat"
      >
        {{ t('common.vazgec') }}
      </button>
      <button
        v-if="cevrimici"
        type="button"
        class="saha-imza-birincil"
        data-test="saha-imza-gonder"
        :disabled="gonderilemez"
        :aria-busy="kaydediliyor ? 'true' : 'false'"
        @click="gonder"
      >
        <i
          :class="kaydediliyor ? 'pi pi-spin pi-spinner' : 'pi pi-check'"
          aria-hidden="true"
        />
        <span>{{ t('sahaPortali.gonder') }}</span>
      </button>
      <button
        v-else
        type="button"
        class="saha-imza-birincil"
        data-test="saha-imza-kuyruk"
        :disabled="gonderilemez"
        :aria-busy="kaydediliyor ? 'true' : 'false'"
        @click="kuyrukla"
      >
        <i
          class="pi pi-cloud-upload"
          aria-hidden="true"
        />
        <span>{{ t('sahaPortali.kuyrugaAl') }}</span>
      </button>
    </template>
  </AppDialog>
</template>

<script setup>
import { computed, nextTick, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import AppDialog from '../AppDialog.vue'
import ImzaPad from '../ImzaPad.vue'

const props = defineProps({
  visible: { type: Boolean, default: false },
  teslimat: { type: Object, default: null },
  cevrimici: { type: Boolean, default: true },
  kaydediliyor: { type: Boolean, default: false }
})

const emit = defineEmits(['update:visible', 'gonder', 'kuyrukla'])
const { t } = useI18n()

const AZAMI_FOTO_BAYT = 6 * 1024 * 1024

const imzaPadRef = ref(null)
const fotoGirdi = ref(null)
const teslimAlanAd = ref('')
const teslimNotu = ref('')
const imzaDataUrl = ref(null)
const fotoDataUrl = ref(null)
const uyari = ref('')

const gonderilemez = computed(
  () => props.kaydediliyor || !teslimAlanAd.value.trim() || !imzaDataUrl.value
)

function govde() {
  return {
    teslimAlanAd: teslimAlanAd.value.trim(),
    teslimNotu: teslimNotu.value.trim(),
    imzaDataUrl: imzaDataUrl.value,
    fotoDataUrl: fotoDataUrl.value
  }
}

function gonder() {
  if (gonderilemez.value) return
  emit('gonder', govde())
}

function kuyrukla() {
  if (gonderilemez.value) return
  emit('kuyrukla', govde())
}

function kapat() {
  emit('update:visible', false)
}

function sifirla() {
  teslimAlanAd.value = ''
  teslimNotu.value = ''
  imzaDataUrl.value = null
  fotoDataUrl.value = null
  uyari.value = ''
  if (fotoGirdi.value) fotoGirdi.value.value = ''
}

function fotografSecildi(olay) {
  const dosya = olay?.target?.files?.[0]
  if (!dosya) return
  uyari.value = ''
  if (dosya.size > AZAMI_FOTO_BAYT) {
    uyari.value = t('sahaPortali.fotografCokBuyuk')
    if (fotoGirdi.value) fotoGirdi.value.value = ''
    return
  }
  const okuyucu = new FileReader()
  okuyucu.onload = () => {
    fotoDataUrl.value = typeof okuyucu.result === 'string' ? okuyucu.result : null
  }
  okuyucu.onerror = () => {
    fotoDataUrl.value = null
  }
  okuyucu.readAsDataURL(dosya)
}

function fotografiKaldir() {
  fotoDataUrl.value = null
  if (fotoGirdi.value) fotoGirdi.value.value = ''
}

watch(
  () => props.visible,
  async (acik) => {
    if (!acik) {
      sifirla()
      return
    }
    await nextTick()
    imzaPadRef.value?.hazirla?.()
  }
)
</script>

<style scoped>
.saha-imza-sheet {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-width: 0;
}
.saha-imza-ozet {
  margin: 0;
  font-size: 12.5px;
  color: var(--text-secondary);
  overflow-wrap: anywhere;
}
.saha-imza-adres {
  color: var(--text-muted);
}
.saha-imza-alan {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}
.saha-imza-etiket {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
}
.saha-imza-girdi {
  width: 100%;
  min-height: 44px;
  padding: 8px 10px;
  border: 1px solid var(--border);
  border-radius: var(--radius-md, 10px);
  background: var(--bg-card);
  color: var(--text-primary);
  font-size: 14px;
  font-family: inherit;
}
.saha-imza-metin {
  resize: vertical;
  min-height: 56px;
}
.saha-imza-pad {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.saha-imza-uyari {
  font-size: 11.5px;
  color: var(--text-muted);
}
.saha-imza-uyari--hata {
  color: var(--danger);
  font-weight: 600;
}
.saha-imza-foto {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
}
.saha-imza-dosya {
  display: none;
}
.saha-imza-foto-onizleme {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}
.saha-imza-foto-gorsel {
  width: 72px;
  height: 72px;
  object-fit: cover;
  border-radius: var(--radius-md, 10px);
  border: 1px solid var(--border);
}
.saha-imza-ikincil,
.saha-imza-vazgec,
.saha-imza-birincil {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-height: 44px;
  padding: 0 14px;
  border-radius: var(--radius-md, 10px);
  border: 1px solid var(--border);
  background: var(--bg-card);
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}
.saha-imza-birincil {
  background: var(--accent);
  border-color: var(--accent);
  color: var(--accent-contrast, #fff);
}
.saha-imza-birincil:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}
.saha-imza-cevrimdisi {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  margin: 0;
  padding: 8px 10px;
  border-radius: var(--radius-md, 10px);
  background: var(--warning-soft, var(--bg-secondary));
  color: var(--text-secondary);
  font-size: 11.5px;
  line-height: 1.4;
}
.saha-imza-ikincil:focus-visible,
.saha-imza-vazgec:focus-visible,
.saha-imza-birincil:focus-visible,
.saha-imza-girdi:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}
/* Mobil: sheet tam ekran; klavye açılınca içerik kaydırılabilir kalır. */
@media (max-width: 640px) {
  .saha-imza-dialog :deep(.p-dialog) {
    width: 100vw;
    max-width: 100vw;
    height: 100dvh;
    max-height: 100dvh;
    margin: 0;
    border-radius: 0;
  }
  .saha-imza-dialog :deep(.p-dialog-content) {
    max-height: none;
    flex: 1 1 auto;
    -webkit-overflow-scrolling: touch;
  }
}
</style>
