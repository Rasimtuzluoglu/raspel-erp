<template>
  <section
    v-if="toplam > 0"
    class="saha-kuyruk"
    data-test="saha-kuyruk-cubugu"
    role="status"
    aria-live="polite"
  >
    <div class="saha-kuyruk-bilgi">
      <i
        class="pi pi-cloud-upload saha-kuyruk-ikon"
        aria-hidden="true"
      />
      <div class="saha-kuyruk-metin">
        <span class="saha-kuyruk-baslik">
          {{ t('sahaPortali.kuyrukBaslik', { n: toplam }) }}
        </span>
        <span class="saha-kuyruk-kirilim">{{ kirilim }}</span>
        <span
          v-if="hataSayisi > 0"
          class="saha-kuyruk-hata"
        >
          <i
            class="pi pi-exclamation-triangle"
            aria-hidden="true"
          />
          {{ t('sahaPortali.kuyrukHata', { n: hataSayisi }) }}
          <template v-if="sonHata">
            · {{ t('sahaPortali.kuyrukSonHata', { mesaj: sonHata }) }}
          </template>
        </span>
      </div>
    </div>

    <div class="saha-kuyruk-eylem">
      <button
        v-for="kayit in hataliKayitlar"
        :key="`sil-${kayit.id}`"
        type="button"
        class="saha-kuyruk-sil"
        data-test="saha-kuyruk-sil"
        :aria-label="t('sahaPortali.kuyrukSil')"
        :title="t('sahaPortali.kuyrukSil')"
        @click="emit('sil', kayit.id)"
      >
        <i
          class="pi pi-trash"
          aria-hidden="true"
        />
      </button>
      <button
        type="button"
        class="saha-kuyruk-gonder"
        data-test="saha-kuyruk-gonder"
        :disabled="senkronizeEdiliyor"
        :aria-busy="senkronizeEdiliyor ? 'true' : 'false'"
        @click="emit('gonder')"
      >
        <i
          :class="senkronizeEdiliyor ? 'pi pi-spin pi-spinner' : 'pi pi-send'"
          aria-hidden="true"
        />
        <span>
          {{ senkronizeEdiliyor ? t('sahaPortali.kuyrukGonderiliyor') : t('sahaPortali.kuyrukSimdiGonder') }}
        </span>
      </button>
    </div>
  </section>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

const props = defineProps({
  kuyruk: { type: Array, default: () => [] },
  senkronizeEdiliyor: { type: Boolean, default: false }
})

const emit = defineEmits(['gonder', 'sil'])
const { t } = useI18n()

const TUR_ANAHTAR = {
  TESLIMAT: 'sahaPortali.kuyrukTeslimat',
  ZIYARET: 'sahaPortali.kuyrukZiyaret',
  MASRAF: 'sahaPortali.kuyrukMasraf',
  IZIN: 'sahaPortali.kuyrukIzin',
  TAHSILAT: 'sahaPortali.kuyrukTahsilat'
}

const toplam = computed(() => props.kuyruk.length)

const hataliKayitlar = computed(() => props.kuyruk.filter((k) => k.durum === 'HATA'))
const hataSayisi = computed(() => hataliKayitlar.value.length)

const sonHata = computed(() => {
  const kayit = [...hataliKayitlar.value].reverse().find((k) => k.sonHata)
  return kayit?.sonHata || ''
})

const kirilim = computed(() => {
  const sayilar = props.kuyruk.reduce((acc, k) => {
    acc[k.tur] = (acc[k.tur] || 0) + 1
    return acc
  }, {})
  return Object.entries(TUR_ANAHTAR)
    .filter(([tur]) => sayilar[tur])
    .map(([tur, anahtar]) => t(anahtar, { n: sayilar[tur] }))
    .join(' · ')
})
</script>

<style scoped>
.saha-kuyruk {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 8px 12px;
  padding: 10px 12px;
  border: 1px solid var(--warning-border, var(--border));
  border-radius: var(--radius-lg, 14px);
  background: var(--warning-soft, var(--bg-secondary));
  min-width: 0;
}
.saha-kuyruk-bilgi {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  min-width: 0;
  flex: 1 1 200px;
}
.saha-kuyruk-ikon {
  color: var(--warning, var(--accent));
  font-size: 14px;
  margin-top: 2px;
}
.saha-kuyruk-metin {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
  font-size: 12px;
  line-height: 1.35;
}
.saha-kuyruk-baslik {
  font-weight: 700;
  color: var(--text-primary);
}
.saha-kuyruk-kirilim {
  color: var(--text-secondary);
}
.saha-kuyruk-hata {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: var(--danger);
  font-weight: 600;
  overflow-wrap: anywhere;
}
.saha-kuyruk-eylem {
  display: flex;
  align-items: center;
  gap: 6px;
  flex: 0 0 auto;
}
.saha-kuyruk-gonder,
.saha-kuyruk-sil {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-height: 44px;
  min-width: 44px;
  padding: 0 14px;
  border-radius: var(--radius-md, 10px);
  border: 1px solid var(--accent);
  background: var(--accent);
  color: var(--accent-contrast, #fff);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}
.saha-kuyruk-sil {
  padding: 0;
  background: transparent;
  color: var(--danger);
  border-color: var(--danger-border, var(--border));
}
.saha-kuyruk-gonder:disabled {
  opacity: 0.65;
  cursor: progress;
}
.saha-kuyruk-gonder:focus-visible,
.saha-kuyruk-sil:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}
@media (max-width: 520px) {
  .saha-kuyruk-eylem {
    width: 100%;
    justify-content: flex-end;
  }
}
</style>
