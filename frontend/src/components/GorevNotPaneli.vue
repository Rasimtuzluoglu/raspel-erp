<template>
  <div class="fade-in-section gorev-not-grid">
    <div class="form-container-card">
      <div class="form-header">
        <h3><i class="pi pi-check-square text-primary mr-2" />{{ t('sahaPortali.gorevEkle') }}</h3>
      </div>
      <div class="form-body">
        <div class="form-field">
          <label>{{ t('sahaPortali.gorevBasligi') }}</label>
          <InputText
            v-model="gorevForm.baslik"
            class="w-full"
          />
        </div>
        <div class="form-row-2">
          <div class="form-field">
            <label>{{ t('sahaPortali.bitisTarihi') }}</label>
            <input
              v-model="gorevForm.bitisTarihi"
              type="date"
              class="p-inputtext w-full"
            >
          </div>
          <div class="form-field">
            <label>{{ t('sahaPortali.oncelik') }}</label>
            <Dropdown
              v-model="gorevForm.oncelik"
              :options="oncelikSecenekleri"
              option-label="label"
              option-value="value"
              class="w-full"
            />
          </div>
        </div>
        <div class="form-field">
          <label>{{ t('common.description') }}</label>
          <Textarea
            v-model="gorevForm.aciklama"
            rows="2"
            class="w-full"
          />
        </div>
        <Button
          :label="t('sahaPortali.gorevKaydet')"
          icon="pi pi-plus"
          class="p-button-primary w-full"
          :loading="gorevGonderiliyor"
          @click="gorevKaydet"
        />
      </div>
    </div>

    <div class="form-container-card">
      <div class="form-header">
        <h3><i class="pi pi-pen-to-square text-primary mr-2" />{{ t('sahaPortali.hizliNot') }}</h3>
      </div>
      <div class="form-body">
        <div class="form-field">
          <label>{{ t('sahaPortali.notBasligi') }}</label>
          <InputText
            v-model="notForm.baslik"
            class="w-full"
          />
        </div>
        <div class="form-field">
          <label>{{ t('common.description') }}</label>
          <Textarea
            v-model="notForm.icerik"
            rows="3"
            class="w-full"
          />
        </div>
        <Button
          :label="t('sahaPortali.notKaydet')"
          icon="pi pi-save"
          class="p-button-primary w-full"
          :loading="notGonderiliyor"
          @click="notKaydet"
        />
      </div>
    </div>

    <div class="form-container-card gorev-liste">
      <div class="form-header">
        <h3><i class="pi pi-list text-primary mr-2" />{{ t('sahaPortali.gorevlerim') }}</h3>
      </div>
      <div v-if="gorevler.length">
        <div
          v-for="g in gorevler"
          :key="g.id"
          class="gorev-satir"
        >
          <Checkbox
            :model-value="g.durum === 'TAMAMLANDI'"
            :binary="true"
            @update:model-value="gorevTamamla(g)"
          />
          <span :class="{ tamam: g.durum === 'TAMAMLANDI' }">{{ g.baslik }}</span>
          <small v-if="g.bitisTarihi">{{ formatTarih(g.bitisTarihi) }}</small>
        </div>
      </div>
      <div
        v-else
        class="empty-box"
      >
        <p>{{ t('sahaPortali.gorevYok') }}</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useToast } from 'primevue/usetoast'
import { ajandaAPI, notAPI } from '../api/index.js'
import { unwrapList } from '../api/utils/unwrap.js'
import { formatTarih } from '../utils/format.js'

const { t } = useI18n()
const toast = useToast()
const emit = defineEmits(['not-kaydedildi'])

const gorevForm = ref({ baslik: '', bitisTarihi: '', oncelik: 'ORTA', aciklama: '' })
const gorevler = ref([])
const gorevGonderiliyor = ref(false)
const oncelikSecenekleri = computed(() => [
  { label: t('sahaPortali.oncelikDusuk'), value: 'DUSUK' },
  { label: t('sahaPortali.oncelikOrta'), value: 'ORTA' },
  { label: t('sahaPortali.oncelikYuksek'), value: 'YUKSEK' }
])
const notForm = ref({ baslik: '', icerik: '' })
const notGonderiliyor = ref(false)

const gorevleriYukle = async () => {
  try { gorevler.value = unwrapList(await ajandaAPI.gorevler()) } catch { /* yoksay */ }
}

const gorevKaydet = async () => {
  if (!gorevForm.value.baslik?.trim()) {
    toast.add({ severity: 'warn', summary: t('sahaPortali.eksikBilgi'), detail: t('sahaPortali.gorevBasligiZorunlu'), life: 3000 })
    return
  }
  gorevGonderiliyor.value = true
  try {
    await ajandaAPI.gorevOlustur({
      baslik: gorevForm.value.baslik.trim(),
      bitisTarihi: gorevForm.value.bitisTarihi || null,
      oncelik: gorevForm.value.oncelik,
      aciklama: gorevForm.value.aciklama
    })
    toast.add({ severity: 'success', summary: t('sahaPortali.basarili'), detail: t('sahaPortali.gorevKaydedildi'), life: 3000 })
    gorevForm.value = { baslik: '', bitisTarihi: '', oncelik: 'ORTA', aciklama: '' }
    await gorevleriYukle()
  } catch (err) {
    toast.add({ severity: 'error', summary: t('sahaPortali.hata'), detail: err?.response?.data?.message || err.message, life: 3000 })
  } finally {
    gorevGonderiliyor.value = false
  }
}

const gorevTamamla = async (g) => {
  if (!g?.id) return
  try {
    if (g.durum === 'TAMAMLANDI') {
      await ajandaAPI.gorevGuncelle(g.id, { ...g, durum: 'BEKLIYOR' })
      g.durum = 'BEKLIYOR'
    } else {
      await ajandaAPI.gorevTamamla(g.id)
      g.durum = 'TAMAMLANDI'
    }
  } catch (err) {
    toast.add({ severity: 'error', summary: t('sahaPortali.hata'), detail: err?.response?.data?.message || err.message, life: 3000 })
  }
}

const notKaydet = async () => {
  if (!notForm.value.baslik?.trim() || !notForm.value.icerik?.trim()) {
    toast.add({ severity: 'warn', summary: t('sahaPortali.eksikBilgi'), detail: t('sahaPortali.notZorunlu'), life: 3000 })
    return
  }
  notGonderiliyor.value = true
  try {
    await notAPI.create({ baslik: notForm.value.baslik.trim(), icerik: notForm.value.icerik.trim(), kategori: 'SAHA_NOT' })
    toast.add({ severity: 'success', summary: t('sahaPortali.basarili'), detail: t('sahaPortali.notKaydedildi'), life: 3000 })
    notForm.value = { baslik: '', icerik: '' }
    emit('not-kaydedildi')
  } catch (err) {
    toast.add({ severity: 'error', summary: t('sahaPortali.hata'), detail: err?.response?.data?.message || err.message, life: 3000 })
  } finally {
    notGonderiliyor.value = false
  }
}

onMounted(() => gorevleriYukle())
</script>

<style scoped>
.fade-in-section {
  animation: fadeIn 0.25s ease-in-out;
}
.form-container-card {
  max-width: 600px;
  margin: 0 auto;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 1rem;
  padding: 1.5rem;
}
.form-header h3 {
  font-size: 1.15rem;
  font-weight: 700;
  margin: 0 0 0.25rem 0;
}
.form-header p {
  font-size: 0.8rem;
  color: var(--text-secondary);
  margin-bottom: 1.25rem;
}
.form-body {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}
.form-field label {
  display: block;
  font-size: 0.8rem;
  font-weight: 600;
  margin-bottom: 0.35rem;
  color: var(--text-secondary);
}
.form-row-2 {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.75rem;
}
.empty-box {
  text-align: center;
  padding: 3.5rem 1rem;
  background: var(--bg-card);
  border: 1px dashed var(--border);
  border-radius: 1rem;
  color: var(--text-secondary);
}
.gorev-not-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(320px, 100%), 1fr));
  gap: 1rem;
  align-items: start;
}
.gorev-liste .gorev-satir {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 4px;
  border-bottom: 1px solid var(--border);
  font-size: 0.85rem;
}
.gorev-liste .gorev-satir:last-child {
  border-bottom: none;
}
.gorev-liste .gorev-satir span {
  flex: 1;
  min-width: 0;
}
.gorev-liste .gorev-satir .tamam {
  text-decoration: line-through;
  color: var(--text-muted);
}
.gorev-liste .gorev-satir small {
  color: var(--text-muted);
  white-space: nowrap;
}
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(4px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
