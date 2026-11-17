<template>
  <a
    v-if="temizAdres"
    class="saha-navigasyon"
    data-test="saha-navigasyon"
    :href="url"
    target="_blank"
    rel="noopener noreferrer"
    :aria-label="`${t('sahaPortali.navigasyon')}: ${temizAdres}`"
    @click.prevent="ac"
  >
    <i
      class="pi pi-directions"
      aria-hidden="true"
    />
    <span>{{ t('sahaPortali.navigasyon') }}</span>
  </a>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { haritaUrl } from '../../utils/sahaBaglantilar.js'

const props = defineProps({
  adres: { type: String, default: '' }
})

const { t } = useI18n()

const temizAdres = computed(() => (props.adres || '').trim())
const url = computed(() => haritaUrl(temizAdres.value))

function ac() {
  window.open(url.value, '_blank')
}
</script>

<style scoped>
.saha-navigasyon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-height: 44px;
  padding: 0 12px;
  border: 1px solid var(--accent-border, var(--border));
  border-radius: var(--radius-md, 10px);
  background: var(--bg-card);
  color: var(--accent);
  font-size: 13px;
  font-weight: 600;
  text-decoration: none;
}
.saha-navigasyon:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}
</style>
