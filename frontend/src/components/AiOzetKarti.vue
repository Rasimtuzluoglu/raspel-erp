<template>
  <!--
    Gömülü (inline) blok: Yönetici Kokpiti'ndeki AI insight kartının içinde
    kullanılır. Önceden Dashboard'da ayrı bir Card olarak duruyordu; kullanıcı
    isteğiyle kokpite taşındı ve tek "AI" kartı olacak şekilde birleştirildi.
  -->
  <div class="ai-ozet-bolum">
    <div class="ai-ozet-baslik">
      <i class="pi pi-sparkles" /> {{ t('dashboard.aiOzet') }}
    </div>
    <p
      v-if="ozet"
      class="ai-ozet-metin"
    >
      {{ ozet }}
    </p>
    <p
      v-else
      class="ai-ozet-bos"
    >
      {{ t('dashboard.aiOzetBekliyor') }}
    </p>
    <Button
      :label="ozet ? t('dashboard.aiOzetYenile') : t('dashboard.aiOzetAl')"
      icon="pi pi-bolt"
      class="p-button-sm ai-ozet-btn"
      :loading="yukleniyor"
      @click="ozetAl"
    />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { sohbetAPI } from '../api/index.js'

const { t } = useI18n()
const toastBildirim = useToastBildirim()
const ozet = ref('')
const yukleniyor = ref(false)

const ozetAl = async () => {
  yukleniyor.value = true
  try {
    const r = await sohbetAPI.aiDashboardOzet()
    ozet.value = r?.data?.ozet || ''
  } catch (e) {
    // AI yapılandırılmadıysa (400) sessiz kal; diğer hatalarda bilgi ver.
    const durum = e?.response?.status
    if (durum !== 400) {
      toastBildirim.hata(e?.response?.data?.message || t('common.error'))
    } else {
      toastBildirim.uyari(t('dashboard.aiYapilandirilmadi'))
    }
  } finally {
    yukleniyor.value = false
  }
}
</script>

<style scoped>
.ai-ozet-bolum {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}
.ai-ozet-baslik {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 0.8rem;
  color: var(--text-primary);
}
.ai-ozet-metin {
  margin: 0;
  line-height: 1.5;
  font-size: 0.8rem;
  color: var(--text-secondary);
}
.ai-ozet-bos {
  margin: 0;
  color: var(--text-muted);
  font-size: 0.8rem;
}
.ai-ozet-btn {
  align-self: flex-start;
}
</style>
