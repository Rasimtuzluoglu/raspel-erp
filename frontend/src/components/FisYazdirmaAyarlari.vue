<template>
  <Card class="ayar-kart">
    <template #title>
      <div class="baslik-ic">
        <i class="pi pi-print" />{{ t('hesapAyarlari.fisYazdirmaAyarlari') }}
      </div>
    </template>
    <template #content>
      <p class="ai-aciklama">
        {{ t('hesapAyarlari.fisAciklama') }}
      </p>
      <div class="fis-ayar-satir">
        <label>{{ t('hesapAyarlari.fisAltNotu') }}</label>
        <InputText
          v-model="fisAltNotu"
          :placeholder="t('hesapAyarlari.fisAltiMesajPlaceholder')"
          class="w-full"
        />
      </div>
      <div class="fis-ayar-satir">
        <label>{{ t('hesapAyarlari.fisteFiyatGoster') }}</label>
        <SelectButton
          v-model="fisFiyatli"
          :options="fisSecenekleri"
          option-label="label"
          option-value="value"
        />
      </div>
      <div class="fis-ayar-satir">
        <label>{{ t('hesapAyarlari.fisGenisligi') }}</label>
        <SelectButton
          v-model="fisGenislik"
          :options="fisGenislikSecenekleri"
          option-label="label"
          option-value="value"
        />
      </div>
    </template>
  </Card>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '../stores/authStore.js'
import { sirketAPI } from '../api/index.js'

const { t } = useI18n()
const authStore = useAuthStore()

// Fiş yazdırma ayarları (sunucuda saklanır; localStorage hızlı önbellek)
const fisAltNotu = ref(localStorage.getItem('raspel_fis_notu') || t('hesapAyarlari.fisVarsayilanNot'))
const fisFiyatli = ref(localStorage.getItem('raspel_fis_fiyatli') !== 'false')
const fisGenislik = ref(localStorage.getItem('raspel_fis_genislik') || '80')
const fisSecenekleri = [
  { label: t('hesapAyarlari.fiyatli'), value: true },
  { label: t('hesapAyarlari.fiyatsiz'), value: false }
]
const fisGenislikSecenekleri = [
  { label: '80mm', value: '80' },
  { label: '58mm', value: '58' }
]

const fisAyarlariYukle = async () => {
  const sirketId = authStore?.sirketId
  if (!sirketId) return
  try {
    const res = await sirketAPI.getPosFisAyarlari(sirketId)
    if (res.data?.ayarlar) {
      const a = JSON.parse(res.data.ayarlar)
      if (a.fisAltNotu != null) {
        fisAltNotu.value = a.fisAltNotu
        localStorage.setItem('raspel_fis_notu', a.fisAltNotu)
      }
      if (a.fisFiyatli != null) {
        fisFiyatli.value = a.fisFiyatli
        localStorage.setItem('raspel_fis_fiyatli', String(a.fisFiyatli))
      }
      if (a.fisGenislik != null) {
        fisGenislik.value = String(a.fisGenislik)
        localStorage.setItem('raspel_fis_genislik', String(a.fisGenislik))
      }
    }
  } catch {
    /* sunucu yoksa yerel önbellek kullanılır */
  }
}

const fisAyarlariKaydet = async () => {
  const ayarlar = { fisAltNotu: fisAltNotu.value || '', fisFiyatli: fisFiyatli.value, fisGenislik: fisGenislik.value }
  localStorage.setItem('raspel_fis_notu', ayarlar.fisAltNotu)
  localStorage.setItem('raspel_fis_fiyatli', String(ayarlar.fisFiyatli))
  localStorage.setItem('raspel_fis_genislik', String(ayarlar.fisGenislik))
  const sirketId = authStore?.sirketId
  if (sirketId) {
    try {
      await sirketAPI.savePosFisAyarlari(sirketId, JSON.stringify(ayarlar))
    } catch {
      /* sunucu yoksa yerel önbellek yeterli */
    }
  }
}

watch([fisAltNotu, fisFiyatli, fisGenislik], () => { fisAyarlariKaydet() })

const fisAyariDinleyici = (e) => {
  if (e.key === 'raspel_fis_fiyatli' && e.newValue !== null) {
    fisFiyatli.value = e.newValue !== 'false'
  } else if (e.key === 'raspel_fis_notu' && e.newValue !== null) {
    fisAltNotu.value = e.newValue
  }
}
onMounted(() => window.addEventListener('storage', fisAyariDinleyici))
onUnmounted(() => window.removeEventListener('storage', fisAyariDinleyici))
onMounted(() => fisAyarlariYukle())
</script>

<style scoped>
.ayar-kart {
  display: flex;
  flex-direction: column;
}
.ayar-kart :deep(.p-card-content) {
  padding-top: 8px;
  flex: 1;
}
.ayar-kart :deep(.p-card-title) {
  display: flex;
  align-items: center;
}
.ayar-kart :deep(.p-card-title i) {
  margin-right: 8px;
}
.baslik-ic {
  display: inline-flex;
  align-items: center;
}
.baslik-ic i {
  margin-right: 8px;
}
.ai-aciklama {
  color: var(--text-secondary);
  margin-bottom: 8px;
  font-size: 13px;
  line-height: 1.5;
}
.fis-ayar-satir {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 14px;
}
.fis-ayar-satir label {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
}
</style>
