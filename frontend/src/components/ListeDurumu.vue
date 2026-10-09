<template>
  <!--
    Ust uc durumu: yukleniyor / hata / bos liste.
    DataTable'in `empty` slot'unun gorsel olarak data ile ayni
    yukseklikte gorunmesini saglar; boylece "kayit yok" ile
    "yukleniyor" birbirine karismaz.
  -->
  <div
    class="liste-durumu"
    role="status"
    aria-live="polite"
  >
    <div
      v-if="yukleniyor"
      class="liste-durumu__iskelet"
      :aria-label="$t('common.loading')"
    >
      <SkeletonLoader
        :count="iskeletSatir"
        height="44px"
      />
    </div>

    <div
      v-else-if="hata"
      class="liste-durumu__hata"
      role="alert"
    >
      <i class="pi pi-exclamation-triangle" />
      <div class="liste-durumu__metin">
        <strong>{{ $t('common.yuklenemedi') }}</strong>
        <span v-if="hataMetni">{{ hataMetni }}</span>
      </div>
      <Button
        v-if="yenidenDene"
        :label="$t('common.tekrarDene')"
        icon="pi pi-refresh"
        class="p-button-sm p-button-outlined"
        @click="yenidenDene"
      />
    </div>

    <div
      v-else-if="bos"
      class="liste-durumu__bos"
    >
      <i
        class="pi pi-inbox"
        :style="{ fontSize: '2.5rem', color: 'var(--text-color-secondary, #94a3b8)' }"
      />
      <div class="liste-durumu__metin">
        <strong>{{ bosMesaj }}</strong>
        <span v-if="bosIpucu">{{ bosIpucu }}</span>
      </div>
      <Button
        v-if="eylemEtiketi"
        :label="eylemEtiketi"
        icon="pi pi-plus"
        class="p-button-sm"
        @click="$emit('eylem')"
      />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import Button from 'primevue/button'
import SkeletonLoader from './SkeletonLoader.vue'

const props = defineProps({
  yukleniyor: { type: Boolean, default: false },
  hata: { type: [String, Error], default: null },
  bos: { type: Boolean, default: false },
  bosMesaj: { type: String, default: '' },
  bosIpucu: { type: String, default: '' },
  eylemEtiketi: { type: String, default: '' },
  iskeletSatir: { type: Number, default: 6 },
  yenidenDene: { type: Function, default: null }
})

defineEmits(['eylem'])

const hataMesaji = (h) => {
  if (!h) return ''
  if (typeof h === 'string') return h
  return h.message || ''
}

const hataMetni = computed(() => hataMesaji(props.hata))
</script>

<style scoped>
.liste-durumu {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.liste-durumu__bos,
.liste-durumu__hata {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 32px 16px;
  text-align: center;
}

.liste-durumu__hata {
  color: var(--red-500, #ef4444);
}

.liste-durumu__metin {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.liste-durumu__metin span {
  color: var(--text-color-secondary, #64748b);
  font-size: 0.875rem;
}
</style>