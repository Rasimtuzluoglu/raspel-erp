<template>
  <div class="notlar-page">
    <PageHeader
      :title="t('notlar.title')"
      :subtitle="t('notlar.subtitle')"
    >
      <template #actions>
        <Button
          :label="t('notlar.yeniNot')"
          icon="pi pi-plus"
          class="p-button-primary"
          @click="dialogAc"
        />
      </template>
    </PageHeader>

    <div
      v-if="geriAlGoster"
      class="geri-al-banner"
    >
      <i class="pi pi-history" />
      <span>"{{ silinenSon?.baslik }}" {{ t('notlar.silindiBanner') }}</span>
      <Button
        :label="t('notlar.geriAl')"
        icon="pi pi-undo"
        size="small"
        @click="geriAl"
      />
      <Button
        icon="pi pi-times"
        class="p-button-text p-button-sm"
        @click="geriAlGoster = false"
      />
    </div>

    <div
      v-if="store.loading"
      class="p-4"
    >
      <SkeletonLoader :count="3" />
    </div>

    <div
      v-else-if="store.notlar.length === 0"
      class="empty-box"
    >
      <i class="pi pi-pen-to-square empty-icon" />
      <h3>{{ t('notlar.emptyTitle') }}</h3>
      <p style="margin-bottom: 1rem">
        {{ t('notlar.emptyHint') }}
      </p>
      <Button
        :label="t('notlar.yeniNotEkle')"
        icon="pi pi-plus"
        class="p-button-primary"
        @click="dialogAc"
      />
    </div>

    <div
      v-else
      class="not-grid"
    >
      <div
        v-for="item in store.notlar"
        :key="item.id"
        class="not-card"
        :class="[item.onemDerecesi?.toLowerCase(), renkSinif(item.renk)]"
      >
        <div class="not-card-header">
          <span
            class="onem-badge"
            :class="item.onemDerecesi?.toLowerCase()"
          >{{ onemAdi(item.onemDerecesi || 'NORMAL') }}</span>
          <span class="not-tarih">{{ formatTarih(item.olusturmaTarihi) }}</span>
        </div>
        <h4 class="not-baslik">
          {{ item.baslik }}
        </h4>
        <p class="not-icerik">
          {{ item.icerik || t('notlar.aciklamaYok') }}
        </p>
        <div class="not-card-actions">
          <Button
            icon="pi pi-pencil"
            class="p-button-rounded p-button-text p-button-sm"
            :title="t('common.edit')"
            @click="dialogDuzenle(item)"
          />
          <Button
            icon="pi pi-trash"
            class="p-button-rounded p-button-text p-button-danger p-button-sm"
            :title="t('common.delete')"
            @click="sil(item.id)"
          />
        </div>
      </div>
    </div>

    <Dialog
      v-model:visible="dialogGoster"
      :header="dialogBaslik"
      :modal="true"
      style="width: 500px"
    >
      <FormField
        :label="t('notlar.baslik')"
        :required="true"
        :error="gonderildi && !form.baslik?.trim() ? t('notlar.baslikZorunlu') : ''"
      >
        <InputText
          v-model="form.baslik"
          :placeholder="t('notlar.baslikPlaceholder')"
          class="w-full"
          :class="{ 'p-invalid': gonderildi && !form.baslik?.trim() }"
        />
      </FormField>
      <FormField :label="t('notlar.icerik')">
        <Textarea
          v-model="form.icerik"
          :placeholder="t('notlar.icerikPlaceholder')"
          rows="5"
          class="w-full"
        />
      </FormField>
      <FormField :label="t('notlar.onemDerecesi')">
        <Dropdown
          v-model="form.onemDerecesi"
          :options="onemSecenek"
          option-label="label"
          option-value="value"
          :placeholder="t('faturalar.seciniz')"
          class="w-full"
        />
      </FormField>
      <FormField :label="t('notlar.renk')">
        <div class="renk-secici">
          <button
            v-for="r in renkSecenekler"
            :key="r.deger"
            type="button"
            class="renk-nokta"
            :class="{ secili: form.renk === r.deger }"
            :style="{ background: r.renk }"
            :title="r.etiket"
            @click="form.renk = r.deger"
          />
        </div>
      </FormField>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          icon="pi pi-times"
          class="p-button-text"
          @click="dialogGoster = false"
        />
        <Button
          :label="t('common.save')"
          icon="pi pi-check"
          :loading="kaydediliyor"
          @click="kaydet"
        />
      </template>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useToast } from 'primevue/usetoast'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useNotStore } from '../stores/notStore.js'
import FormField from '../components/FormField.vue'
import { useI18n } from 'vue-i18n'

const toast = useToast()
const toastBildirim = useToastBildirim()
const store = useNotStore()
const { t } = useI18n()

const onemSecenek = [
  { label: t('notlar.dusuk'), value: 'DUSUK' },
  { label: t('notlar.normal'), value: 'NORMAL' },
  { label: t('notlar.yuksek'), value: 'YUKSEK' },
  { label: t('notlar.kritik'), value: 'KRITIK' }
]

const renkSecenekler = [
  { deger: 'MAVI', renk: '#3b82f6', etiket: t('notlar.mavi') },
  { deger: 'YESIL', renk: '#22c55e', etiket: t('notlar.yesil') },
  { deger: 'SARI', renk: '#f59e0b', etiket: t('notlar.sari') },
  { deger: 'KIRMIZI', renk: '#ef4444', etiket: t('notlar.kirmizi') },
  { deger: 'MOR', renk: '#8b5cf6', etiket: t('notlar.mor') },
  { deger: 'PEMBE', renk: '#ec4899', etiket: t('notlar.pembe') },
  { deger: 'GRİ', renk: '#64748b', etiket: t('notlar.gri') }
]

const renkSinif = (r) => `renk-${(r || 'MAVI').toLowerCase()}`

const onemAdi = (v) =>
  ({ DUSUK: t('notlar.dusuk'), NORMAL: t('notlar.normal'), YUKSEK: t('notlar.yuksek'), KRITIK: t('notlar.kritik') })[v] || v

const dialogGoster = ref(false)
const duzenlemeModu = ref(false)
const kaydediliyor = ref(false)
const gonderildi = ref(false)
const form = ref({ baslik: '', icerik: '', onemDerecesi: 'NORMAL' })
const geriAlGoster = ref(false)
const silinenSon = ref(null)
let geriAlZamanlayici = null

const dialogBaslik = computed(() => (duzenlemeModu.value ? t('notlar.duzenle') : t('notlar.yeniNot')))

onMounted(() => store.getAllNotlar())

const dialogAc = () => {
  duzenlemeModu.value = false
  form.value = { baslik: '', icerik: '', onemDerecesi: 'NORMAL', renk: 'MAVI' }
  gonderildi.value = false
  dialogGoster.value = true
}

const dialogDuzenle = (item) => {
  duzenlemeModu.value = true
  form.value = { ...item }
  gonderildi.value = false
  dialogGoster.value = true
}

const kaydet = async () => {
  gonderildi.value = true
  if (!form.value.baslik?.trim()) return
  kaydediliyor.value = true
  try {
    if (duzenlemeModu.value) {
      await store.updateNot(form.value.id, form.value)
      toast.add({ severity: 'success', summary: t('notlar.guncellendi'), detail: t('notlar.guncellemeBasarili'), life: 3000 })
    } else {
      await store.addNot(form.value)
      toast.add({ severity: 'success', summary: t('notlar.eklendi'), detail: t('notlar.eklemeBasarili'), life: 3000 })
    }
    dialogGoster.value = false
  } catch {
    toastBildirim.hata(t('notlar.islemBasarisiz'))
  } finally {
    kaydediliyor.value = false
  }
}

const sil = async (id) => {
  const silinen = store.notlar.find((n) => n.id === id)
  try {
    await store.deleteNot(id)
    toast.add({ severity: 'success', summary: 'Silindi', detail: 'Not silindi.', life: 3000 })
    if (silinen) {
      silinenSon.value = silinen
      geriAlGoster.value = true
      if (geriAlZamanlayici) clearTimeout(geriAlZamanlayici)
      geriAlZamanlayici = setTimeout(() => {
        geriAlGoster.value = false
        silinenSon.value = null
      }, 8000)
    }
  } catch {
    toastBildirim.hata('Silme başarısız.')
  }
}

const geriAl = async () => {
  if (!silinenSon.value) return
  try {
    await store.addNot({
      baslik: silinenSon.value.baslik,
      icerik: silinenSon.value.icerik,
      onemDerecesi: silinenSon.value.onemDerecesi,
      renk: silinenSon.value.renk
    })
    toast.add({ severity: 'success', summary: 'Geri Alındı', detail: 'Not geri yüklendi.', life: 3000 })
  } catch {
    toastBildirim.hata('Geri alma başarısız.')
  } finally {
    geriAlGoster.value = false
    silinenSon.value = null
  }
}

const formatTarih = (t) => {
  if (!t) return ''
  return new Date(t).toLocaleString('tr-TR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}
</script>

<style scoped>
.notlar-page {
  padding: 0;
  max-width: 100%;
}
.geri-al-banner {
  display: flex;
  align-items: center;
  gap: 10px;
  background: rgba(245, 158, 11, 0.12);
  border: 1px solid rgba(245, 158, 11, 0.3);
  border-radius: 10px;
  padding: 10px 16px;
  margin-bottom: 1rem;
  font-size: 0.9rem;
  color: #fbbf24;
}
.geri-al-banner i {
  font-size: 16px;
}
.empty-box {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 3rem;
  text-align: center;
  margin-top: 1.5rem;
}
.empty-icon {
  font-size: 3.5rem;
  color: #94a3b8;
  margin-bottom: 1rem;
}
.not-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(min(320px, 100%), 1fr));
  gap: 1rem;
  margin-top: 1.5rem;
}
.not-card {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 1.25rem;
  transition: all 0.2s;
  display: flex;
  flex-direction: column;
}
.not-card:hover {
  border-color: rgba(59, 130, 246, 0.3);
}
.not-card.yuksek {
  border-left: 4px solid #f59e0b;
}
.not-card.kritik {
  border-left: 4px solid #ef4444;
}
.not-card.dusuk {
  border-left: 4px solid #3b82f6;
}
.not-card.normal {
  border-left: 4px solid #64748b;
}
[data-theme='light'] .not-card {
  background: #ffffff;
}
.not-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.75rem;
}
.onem-badge {
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
  font-size: 0.7rem;
  font-weight: 700;
  text-transform: uppercase;
}
.onem-badge.dusuk {
  background: rgba(59, 130, 246, 0.2);
  color: #60a5fa;
}
.onem-badge.normal {
  background: rgba(100, 116, 139, 0.2);
  color: #94a3b8;
}
.onem-badge.yuksek {
  background: rgba(245, 158, 11, 0.2);
  color: #fbbf24;
}
.onem-badge.kritik {
  background: rgba(239, 68, 68, 0.2);
  color: #f87171;
}
.not-card.renk-mavi {
  border-left: 4px solid #3b82f6;
}
.not-card.renk-yesil {
  border-left: 4px solid #22c55e;
}
.not-card.renk-sari {
  border-left: 4px solid #f59e0b;
}
.not-card.renk-kirmizi {
  border-left: 4px solid #ef4444;
}
.not-card.renk-mor {
  border-left: 4px solid #8b5cf6;
}
.not-card.renk-pembe {
  border-left: 4px solid #ec4899;
}
.not-card.renk-gri {
  border-left: 4px solid #64748b;
}
.renk-secici {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.renk-nokta {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  border: 2px solid transparent;
  cursor: pointer;
  transition: all 0.15s;
}
.renk-nokta:hover {
  transform: scale(1.15);
}
.renk-nokta.secili {
  border-color: var(--text-primary);
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.3);
}
.not-tarih {
  font-size: 0.75rem;
  color: var(--text-muted);
}
.not-baslik {
  margin: 0 0 0.5rem;
  font-size: 1rem;
  color: var(--text-primary);
  font-weight: 600;
}
.not-icerik {
  font-size: 0.875rem;
  color: var(--text-secondary);
  flex: 1;
  margin-bottom: 0.75rem;
  white-space: pre-wrap;
  word-break: break-word;
}
.not-card-actions {
  display: flex;
  gap: 0.25rem;
  justify-content: flex-end;
  border-top: 1px solid var(--border);
  padding-top: 0.5rem;
}
.w-full {
  width: 100% !important;
}
[data-theme='light'] .onem-badge.normal {
  background: rgba(100, 116, 139, 0.1);
  color: #475569;
}
</style>
