<template>
  <article
    class="saha-teslimat-kart"
    :class="{ 'saha-teslimat-kart--ilk': ilkMi, 'saha-teslimat-kart--gecikti': teslimat?.gecikti }"
    data-test="saha-teslimat-karti"
  >
    <header class="saha-teslimat-ust">
      <span
        v-if="sira"
        class="saha-teslimat-sira"
        aria-hidden="true"
      >{{ sira }}</span>
      <span
        class="saha-teslimat-rozet"
        :class="`saha-teslimat-rozet--${rozetSinifi}`"
      >
        {{ durumEtiketi }}
      </span>
      <span
        v-if="teslimat?.gecikti"
        class="saha-teslimat-gecikti"
      >
        <i
          class="pi pi-clock"
          aria-hidden="true"
        />
        {{ t('sahaPortali.gecikti') }}
      </span>
      <span
        v-if="ilkMi"
        class="saha-teslimat-siradaki"
        data-test="saha-teslimat-siradaki"
      >{{ t('sahaPortali.siradaki') }}</span>
    </header>

    <h3 class="saha-teslimat-baslik">
      {{ teslimat?.faturaNumarasi || `#${teslimat?.id ?? '-'}` }}
    </h3>

    <p
      v-if="musteri"
      class="saha-teslimat-musteri"
    >
      <i
        class="pi pi-user"
        aria-hidden="true"
      />
      {{ musteri }}
    </p>

    <p
      v-if="adres"
      class="saha-teslimat-adres"
    >
      <i
        class="pi pi-map-marker"
        aria-hidden="true"
      />
      {{ adres }}
    </p>

    <p
      v-if="teslimat?.beklenenTeslimTarihi"
      class="saha-teslimat-tarih"
    >
      <i
        class="pi pi-calendar"
        aria-hidden="true"
      />
      {{ formatTarih(teslimat.beklenenTeslimTarihi) }}
    </p>

    <div class="saha-teslimat-eylemler">
      <SahaNavigasyonButton
        v-if="adres"
        :adres="adres"
      />

      <a
        v-if="telefonUrl"
        class="saha-teslimat-eylem"
        data-test="saha-teslimat-telefon"
        :href="telefonUrl"
        @click="emit('telefon', teslimat)"
      >
        <i
          class="pi pi-phone"
          aria-hidden="true"
        />
        <span>{{ t('sahaPortali.ara') }}</span>
      </a>

      <a
        v-if="waUrl"
        class="saha-teslimat-eylem"
        data-test="saha-teslimat-whatsapp"
        :href="waUrl"
        target="_blank"
        rel="noopener noreferrer"
      >
        <i
          class="pi pi-whatsapp"
          aria-hidden="true"
        />
        <span>{{ t('sahaPortali.whatsapp') }}</span>
      </a>

      <button
        type="button"
        class="saha-teslimat-eylem"
        data-test="saha-teslimat-durum"
        @click="emit('durum-sec', teslimat)"
      >
        <i
          class="pi pi-sync"
          aria-hidden="true"
        />
        <span>{{ t('sahaPortali.durumSec') }}</span>
      </button>

      <button
        type="button"
        class="saha-teslimat-imza"
        data-test="saha-teslimat-imza"
        :disabled="teslimEdildi"
        @click="emit('imza', teslimat)"
      >
        <i
          class="pi pi-pencil"
          aria-hidden="true"
        />
        <span>{{ t('sahaPortali.imzalaTeslim') }}</span>
      </button>
    </div>
  </article>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import SahaNavigasyonButton from './SahaNavigasyonButton.vue'
import { formatTarih } from '../../utils/format.js'
import { telUrl, whatsappUrl } from '../../utils/sahaBaglantilar.js'

const props = defineProps({
  teslimat: { type: Object, default: null },
  sira: { type: Number, default: null },
  ilkMi: { type: Boolean, default: false }
})

const emit = defineEmits(['imza', 'durum-sec', 'telefon'])
const { t } = useI18n()

const DURUM_ETIKET = {
  BEKLEMEDE: 'sahaPortali.durumBeklemede',
  YOLDA: 'sahaPortali.durumYolda',
  TESLIM_EDILDI: 'sahaPortali.durumTeslimEdildi',
  IPTAL: 'sahaPortali.durumIptal'
}

const durumEtiketi = computed(() => {
  const anahtar = DURUM_ETIKET[props.teslimat?.durum]
  return anahtar ? t(anahtar) : (props.teslimat?.durum || '-')
})

const rozetSinifi = computed(() => (props.teslimat?.durum || 'beklemede').toLowerCase())

const teslimEdildi = computed(() => props.teslimat?.durum === 'TESLIM_EDILDI')
const adres = computed(() => (props.teslimat?.teslimatAdresi || '').trim())
const musteri = computed(() => (props.teslimat?.musteriAdi || '').trim())
const telefonUrl = computed(() => telUrl(props.teslimat?.musteriTelefon))
const waUrl = computed(() => whatsappUrl(props.teslimat?.musteriTelefon))
</script>

<style scoped>
.saha-teslimat-kart {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 12px;
  border: 1px solid var(--border);
  border-radius: var(--radius-lg, 14px);
  background: var(--bg-card);
  min-width: 0;
}
.saha-teslimat-kart--ilk {
  border-color: var(--accent-border);
  box-shadow: inset 3px 0 0 var(--accent);
}
.saha-teslimat-kart--gecikti {
  border-color: var(--danger-border, var(--danger));
}
.saha-teslimat-ust {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  min-width: 0;
}
.saha-teslimat-sira {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: var(--bg-secondary);
  color: var(--text-secondary);
  font-size: 11px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}
.saha-teslimat-rozet {
  padding: 2px 8px;
  border-radius: 999px;
  border: 1px solid var(--border);
  font-size: 11px;
  font-weight: 700;
  color: var(--text-secondary);
  background: var(--bg-secondary);
}
.saha-teslimat-rozet--yolda {
  color: var(--accent);
  border-color: var(--accent-border);
  background: var(--accent-soft, var(--bg-secondary));
}
.saha-teslimat-rozet--teslim_edildi {
  color: var(--success);
  border-color: var(--success-border, var(--border));
}
.saha-teslimat-rozet--iptal {
  color: var(--text-muted);
}
.saha-teslimat-gecikti {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  font-weight: 700;
  color: var(--danger);
}
.saha-teslimat-siradaki {
  margin-left: auto;
  padding: 2px 8px;
  border-radius: 999px;
  background: var(--accent);
  color: var(--accent-contrast, #fff);
  font-size: 10.5px;
  font-weight: 800;
  letter-spacing: 0.02em;
  text-transform: uppercase;
}
.saha-teslimat-baslik {
  margin: 0;
  font-size: 14px;
  font-weight: 700;
  color: var(--text-primary);
  overflow-wrap: anywhere;
}
.saha-teslimat-musteri,
.saha-teslimat-adres,
.saha-teslimat-tarih {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  margin: 0;
  font-size: 12px;
  color: var(--text-secondary);
  overflow-wrap: anywhere;
}
.saha-teslimat-musteri i,
.saha-teslimat-adres i,
.saha-teslimat-tarih i {
  font-size: 11px;
  margin-top: 3px;
  color: var(--text-muted);
  flex: 0 0 auto;
}
.saha-teslimat-eylemler {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 4px;
}
.saha-teslimat-eylem,
.saha-teslimat-imza {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-height: 44px;
  padding: 0 12px;
  border: 1px solid var(--border);
  border-radius: var(--radius-md, 10px);
  background: var(--bg-card);
  color: var(--text-primary);
  font-size: 12.5px;
  font-weight: 600;
  text-decoration: none;
  cursor: pointer;
}
.saha-teslimat-imza {
  flex: 1 1 150px;
  background: var(--accent);
  border-color: var(--accent);
  color: var(--accent-contrast, #fff);
}
.saha-teslimat-imza:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}
.saha-teslimat-eylem:focus-visible,
.saha-teslimat-imza:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}
</style>
