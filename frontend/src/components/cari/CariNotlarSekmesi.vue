<template>
  <div class="cari-notlar">
    <div class="cari-not-baslik">
      <h4>
        <i
          class="pi pi-pen-to-square"
          style="margin-right: 6px"
        />{{ t('cariHesaplar.gorusmeNotlari') }}
      </h4>
    </div>
    <div class="cari-not-ekle">
      <Dropdown
        v-model="onem"
        :options="onemSecenekleri"
        option-label="label"
        option-value="value"
        class="not-onem-select"
      />
      <InputText
        v-model="metin"
        :placeholder="t('cariHesaplar.yeniNotPlaceholder')"
        @keyup.enter="ekle"
      />
      <Button
        icon="pi pi-plus"
        :aria-label="t('common.add')"
        class="p-button-sm"
        @click="ekle"
      />
    </div>
    <div
      v-if="!notlar || !notlar.length"
      class="cari-not-bos"
    >
      {{ t('cariHesaplar.notYok') }}
    </div>
    <div
      v-for="n in notlar"
      :key="n.id"
      class="cari-not-satir"
    >
      <div class="cari-not-icerik">
        <div class="cari-not-baslik-satir">
          <strong>{{ n.baslik }}</strong>
          <span
            v-if="n.onemDerecesi && n.onemDerecesi !== 'NORMAL'"
            class="not-onem-rozet"
            :class="n.onemDerecesi.toLowerCase()"
          >
            {{ n.onemDerecesi === 'YUKSEK' ? t('cariHesaplar.yuksek') : t('cariHesaplar.kritik') }}
          </span>
        </div>
        <p>{{ n.icerik }}</p>
        <small>{{ formatTarihSaat(n.olusturmaTarihi, '') }}</small>
      </div>
      <Button
        icon="pi pi-trash"
        :aria-label="t('common.delete')"
        class="p-button-rounded p-button-text p-button-danger p-button-sm"
        @click="emit('sil', n)"
      />
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { formatTarihSaat } from '../../utils/format.js'

/**
 * Cari 360 kartı > Görüşme Notları sekmesi. Not listesi parent'tan gelir;
 * ekleme/silme istekleri olayla bildirilir.
 */
defineProps({
  notlar: { type: Array, default: () => [] }
})

const emit = defineEmits(['ekle', 'sil'])
const { t } = useI18n()

const metin = ref('')
const onem = ref('NORMAL')

const onemSecenekleri = computed(() => [
  { label: t('cariHesaplar.normal'), value: 'NORMAL' },
  { label: t('cariHesaplar.yuksek'), value: 'YUKSEK' },
  { label: t('cariHesaplar.kritik'), value: 'KRITIK' }
])

const ekle = () => {
  const temiz = metin.value.trim()
  if (!temiz) return
  emit('ekle', { baslik: temiz, onem: onem.value })
  metin.value = ''
  onem.value = 'NORMAL'
}
</script>

<style scoped>
.cari-notlar {
  margin-top: 20px;
  border-top: 1px solid var(--border);
  padding-top: 14px;
}

.cari-not-baslik h4 {
  margin: 0 0 10px;
  font-size: 14px;
}

.cari-not-ekle {
  display: flex;
  gap: 8px;
  align-items: center;
}

.not-onem-select {
  width: 110px;
  flex-shrink: 0;
}

.cari-not-bos {
  color: var(--text-muted);
  font-size: 13px;
  padding: 8px 0;
}

.cari-not-satir {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid var(--border);
}

.cari-not-baslik-satir {
  display: flex;
  align-items: center;
  gap: 8px;
}

.not-onem-rozet {
  padding: 1px 8px;
  border-radius: 20px;
  font-size: 10px;
  font-weight: 700;
}

.not-onem-rozet.yuksek {
  background: var(--warning-soft);
  color: var(--warning);
}

.not-onem-rozet.kritik {
  background: var(--danger-soft);
  color: var(--danger);
}

.cari-not-icerik strong {
  font-size: 13px;
}

.cari-not-icerik p {
  margin: 2px 0;
  font-size: 13px;
}

.cari-not-icerik small {
  color: var(--text-muted);
  font-size: 11px;
}
</style>
