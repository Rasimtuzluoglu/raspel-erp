<template>
  <div class="donemler-sayfasi">
    <PageHeader
      :title="t('donemler.title')"
      :subtitle="t('donemler.aciklama')"
      icon="pi pi-calendar"
    >
      <template #actions>
        <Dropdown
          v-model="seciliSirketId"
          :options="sirketler"
          option-label="ad"
          option-value="id"
          :placeholder="t('donemler.sirketSecin')"
          class="sirket-dropdown"
          @change="sirketDegisti"
        />
        <Button
          :label="t('donemler.yilSonuKapat')"
          icon="pi pi-lock"
          severity="warn"
          outlined
          :disabled="!seciliSirketId"
          @click="kapanisDialog = true"
        />
        <Button
          :label="t('donemler.yeniDonem')"
          icon="pi pi-plus"
          :disabled="!seciliSirketId"
          @click="dialogAc"
        />
      </template>
    </PageHeader>

    <div class="kpi-serit">
      <div class="kpi-kart">
        <span class="kpi-ikon toplam"><i class="pi pi-calendar" /></span>
        <span class="kpi-metin">
          <small>{{ t('donemler.kpiToplam') }}</small>
          <strong>{{ donemler.length }}</strong>
        </span>
      </div>
      <div class="kpi-kart">
        <span class="kpi-ikon aktif"><i class="pi pi-check-circle" /></span>
        <span class="kpi-metin">
          <small>{{ t('donemler.kpiAktif') }}</small>
          <strong>{{ istatistik.aktifAdet }}</strong>
        </span>
      </div>
      <div class="kpi-kart">
        <span class="kpi-ikon kilitli"><i class="pi pi-lock" /></span>
        <span class="kpi-metin">
          <small>{{ t('donemler.kpiKilitli') }}</small>
          <strong>{{ istatistik.kilitliAdet }}</strong>
        </span>
      </div>
      <div class="kpi-kart">
        <span class="kpi-ikon kapanis"><i class="pi pi-history" /></span>
        <span class="kpi-metin">
          <small>{{ t('donemler.kpiSonKapanis') }}</small>
          <strong>{{ sonKapanisYili || '-' }}</strong>
        </span>
      </div>
    </div>

    <div
      v-if="seciliSirketId && !yukleniyor && donemler.length === 0"
      class="donem-bos"
    >
      <i class="pi pi-calendar-times" />
      <p>{{ t('donemler.kayitYok') }}</p>
    </div>

    <div
      v-else
      class="donem-izgara"
    >
      <article
        v-for="d in donemler"
        :key="d.id"
        class="donem-kart"
        :class="{ 'donem-kart-aktif': d.aktif, 'donem-kart-kilitli': d.kilitli }"
      >
        <header class="donem-kart-ust">
          <span class="donem-no">#{{ d.id }}</span>
          <div class="donem-rozetler">
            <Tag
              v-if="d.aktif"
              :value="t('donemler.aktif')"
              severity="success"
              icon="pi pi-check-circle"
            />
            <Tag
              v-else
              :value="t('donemler.pasif')"
              severity="danger"
            />
            <Tag
              v-if="d.kilitli"
              :value="t('donemler.kilitli')"
              severity="warn"
              icon="pi pi-lock"
            />
          </div>
        </header>

        <h3 class="donem-ad">
          {{ d.ad }}
        </h3>

        <div class="donem-aralik">
          <span class="donem-tarih">
            <i class="pi pi-calendar" /> {{ formatTarih(d.baslangic) }}
          </span>
          <span class="donem-tire">→</span>
          <span class="donem-tarih">
            <i class="pi pi-flag" /> {{ formatTarih(d.bitis) }}
          </span>
        </div>

        <div class="donem-kart-alt">
          <span class="donem-gun">
            <i class="pi pi-clock" /> {{ gunSayisi(d) }} {{ t('donemler.gun') }}
          </span>
          <div class="donem-eylemler">
            <Button
              v-if="!d.aktif"
              icon="pi pi-check-circle"
              class="p-button-rounded p-button-text p-button-success"
              :title="t('donemler.aktifYap')"
              :aria-label="t('donemler.aktifYap')"
              @click="aktifYap(d)"
            />
            <Button
              v-if="!d.kilitli"
              icon="pi pi-lock"
              class="p-button-rounded p-button-text p-button-warn"
              :title="t('donemler.kilitle')"
              :aria-label="t('donemler.kilitle')"
              @click="kilitle(d)"
            />
            <Button
              v-else
              icon="pi pi-lock-open"
              class="p-button-rounded p-button-text p-button-secondary"
              :title="t('donemler.kilidiAc')"
              :aria-label="t('donemler.kilidiAc')"
              @click="kilidiAc(d)"
            />
            <Button
              icon="pi pi-pencil"
              :aria-label="t('common.edit')"
              class="p-button-rounded p-button-text"
              :disabled="d.kilitli"
              @click="dialogAc(d)"
            />
            <Button
              icon="pi pi-trash"
              :aria-label="t('common.delete')"
              class="p-button-rounded p-button-text p-button-danger"
              :disabled="d.kilitli"
              @click="sil(d)"
            />
          </div>
        </div>
      </article>
    </div>

    <Dialog
      v-model:visible="kapanisDialog"
      :header="t('donemler.yilSonuKapat')"
      modal
      :style="{ width: '440px' }"
    >
      <Message
        severity="warn"
        :closable="false"
        class="kapanis-uyari"
      >
        {{ t('donemler.kapanisUyari') }}
      </Message>
      <div class="form-grid">
        <div class="field">
          <label>{{ t('donemler.maliYil') }}</label>
          <InputNumber
            v-model="kapanisForm.yil"
            :use-grouping="false"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('donemler.kapanisNotu') }}</label>
          <Textarea
            v-model="kapanisForm.ozet"
            rows="3"
            class="w-full"
          />
        </div>
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          icon="pi pi-times"
          class="p-button-text"
          @click="kapanisDialog = false"
        />
        <Button
          :label="t('donemler.kapat')"
          icon="pi pi-lock"
          severity="warning"
          :loading="kapanisYukleniyor"
          @click="yilSonuKapat"
        />
      </template>
    </Dialog>

    <Card
      v-if="kapanislar.length"
      class="kapanis-kart"
    >
      <template #title>
        <i class="pi pi-history" /> {{ t('donemler.kapanisGecmisi') }}
      </template>
      <template #content>
        <div
          v-for="k in kapanislar"
          :key="k.id"
          class="kapanis-satir"
        >
          <span class="kapanis-yil">{{ k.yil }}</span>
          <span class="kapanis-tarih">{{ formatTarihSaat(k.kapanisTarihi) }}</span>
          <span class="kapanis-not">{{ k.ozet || '-' }}</span>
        </div>
      </template>
    </Card>

    <Dialog
      v-model:visible="dialog"
      :header="duzenleme ? t('donemler.donemDuzenle') : t('donemler.yeniDonem')"
      modal
      :style="{ width: '440px' }"
    >
      <div class="form-grid">
        <div class="field">
          <label>{{ t('donemler.donemAdiZorunlu') }}</label>
          <InputText
            v-model="form.ad"
            class="w-full"
            :placeholder="t('donemler.donemAdiPlaceholder')"
          />
        </div>
        <div class="field">
          <label>{{ t('donemler.baslangicTarihiZorunlu') }}</label>
          <DatePicker
            v-model="form.baslangic"
            date-format="dd.mm.yy"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('donemler.bitisTarihiZorunlu') }}</label>
          <DatePicker
            v-model="form.bitis"
            date-format="dd.mm.yy"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('donemler.aktif') }}</label>
          <InputSwitch v-model="form.aktif" />
        </div>
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          icon="pi pi-times"
          class="p-button-text"
          @click="dialog = false"
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
import { ref, onMounted } from 'vue'
import { useConfirm } from 'primevue/useconfirm'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { donemAPI, sirketAPI } from '../api/index.js'
import { useI18n } from 'vue-i18n'
import { getLocalDateString, formatTarih, formatTarihSaat } from '../utils/format.js'
import { useAuthStore } from '../stores/authStore.js'
import PageHeader from '../components/PageHeader.vue'
import { computed } from 'vue'
const toastBildirim = useToastBildirim()
const confirm = useConfirm()
const { t } = useI18n()
const authStore = useAuthStore()

const donemler = ref([])
const sirketler = ref([])
const seciliSirketId = ref(null)
const yukleniyor = ref(false)
const dialog = ref(false)
const duzenleme = ref(false)
const kaydediliyor = ref(false)
const seciliId = ref(null)
const form = ref({ ad: '', baslangic: null, bitis: null, aktif: true })
const kapanisDialog = ref(false)
const kapanisYukleniyor = ref(false)
const kapanisForm = ref({ yil: new Date().getFullYear(), ozet: '' })
const kapanislar = ref([])

onMounted(async () => {
  try {
    const r = await sirketAPI.getAktif()
    sirketler.value = r.data
    if (sirketler.value.length > 0) {
      // Aktif JWT şirketini önceliklendir; listeyi körlemesine ilk elemandan alma
      // (aksi halde başka şirkete geçilmişse "Dönem bu şirkete ait değil" hatası oluşur).
      const aktifId = authStore.sirketId
      const aktifVar = aktifId != null && sirketler.value.some((s) => s.id === aktifId)
      seciliSirketId.value = aktifVar ? aktifId : sirketler.value[0].id
      await donemleriYukle()
      await kapanislariYukle()
    }
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('donemler.hataSirketler'))
  }
})

const kapanislariYukle = async () => {
  if (!seciliSirketId.value) {
    kapanislar.value = []
    return
  }
  try {
    const r = await donemAPI.kapanislar()
    kapanislar.value = r.data || []
  } catch {
    kapanislar.value = []
  }
}

const kilitle = async (data) => {
  try {
    await donemAPI.kilitle(data.id)
    toastBildirim.basarili(t('donemler.kilitlendi'))
    await donemleriYukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('donemler.islemBasarisiz'))
  }
}

const kilidiAc = (data) => {
  confirm.require({
    message: t('donemler.kilidiAcOnay'),
    header: t('donemler.kilidiAc'),
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: t('donemler.evet'),
    rejectLabel: t('common.vazgec'),
    accept: async () => {
      try {
        await donemAPI.kilidiAc(data.id)
        toastBildirim.basarili(t('donemler.kilidiAcildi'))
        await donemleriYukle()
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || err?.message || t('donemler.islemBasarisiz'))
      }
    },
    reject: () => {}
  })
}

const yilSonuKapat = async () => {
  if (!kapanisForm.value.yil) {
    toastBildirim.uyari(t('donemler.maliYilZorunlu'))
    return
  }
  kapanisYukleniyor.value = true
  try {
    await donemAPI.yilSonuKapat({
      sirketId: seciliSirketId.value,
      yil: kapanisForm.value.yil,
      ozet: kapanisForm.value.ozet
    })
    toastBildirim.basarili(t('donemler.kapanisBasarili'))
    kapanisDialog.value = false
    kapanisForm.value = { yil: new Date().getFullYear(), ozet: '' }
    await donemleriYukle()
    await kapanislariYukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('donemler.kapanisBasarisiz'))
  } finally {
    kapanisYukleniyor.value = false
  }
}

const donemleriYukle = async () => {
  if (!seciliSirketId.value) {
    donemler.value = []
    return
  }
  yukleniyor.value = true
  try {
    const r = await donemAPI.getBySirket(seciliSirketId.value)
    donemler.value = r.data
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('donemler.hataYukleme'))
  }
  yukleniyor.value = false
}

const aktifYap = async (data) => {
  try {
    await donemAPI.aktifYap(data.id)
    toastBildirim.basarili(t('donemler.aktifYapildi'))
    await donemleriYukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('donemler.islemBasarisiz'))
  }
}

const dialogAc = (data) => {  duzenleme.value = !!data
  seciliId.value = data?.id || null
  form.value = data
    ? {
        ...data,
        baslangic: data.baslangic ? new Date(data.baslangic) : null,
        bitis: data.bitis ? new Date(data.bitis) : null
      }
    : { ad: '', baslangic: null, bitis: null, sirketId: seciliSirketId.value, aktif: true }
  dialog.value = true
}

const kaydet = async () => {
  kaydediliyor.value = true
  try {
    const payload = {
      ...form.value,
      sirketId: seciliSirketId.value,
      baslangic: getLocalDateString(form.value.baslangic),
      bitis: getLocalDateString(form.value.bitis)
    }
    if (duzenleme.value) {
      await donemAPI.update(seciliId.value, payload)
    } else {
      await donemAPI.create(payload)
    }
    dialog.value = false
    await donemleriYukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('donemler.hataKaydet'))
  }
  kaydediliyor.value = false
}

const sirketDegisti = async () => {
  kapanisForm.value.yil = new Date().getFullYear()
  kapanisForm.value.ozet = ''
  await donemleriYukle()
  await kapanislariYukle()
}

// Dönem listesinden türeyen özetler (KPI şeridi).
const istatistik = computed(() => ({
  aktifAdet: donemler.value.filter((d) => d.aktif).length,
  kilitliAdet: donemler.value.filter((d) => d.kilitli).length
}))

const sonKapanisYili = computed(() => {
  if (!kapanislar.value.length) return null
  return kapanislar.value
    .map((k) => Number(k.yil))
    .filter((y) => Number.isFinite(y))
    .sort((a, b) => b - a)[0] ?? null
})

// Dönemin kaç gün sürdüğü (aralık günleri dahil). Geçersiz tarih → null.
const gunSayisi = (d) => {
  const b = new Date(d.baslangic)
  const s = new Date(d.bitis)
  if (Number.isNaN(b.getTime()) || Number.isNaN(s.getTime()) || s < b) return '—'
  return Math.round((s - b) / 86400000) + 1
}

const sil = (data) => {
  confirm.require({
    message: t('common.confirmDelete'),
    header: t('common.silmeOnayi'),
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: t('common.evetSil'),
    rejectLabel: t('common.vazgec'),
    accept: async () => {
      try {
        await donemAPI.delete(data.id)
        await donemleriYukle()
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || err?.message || t('donemler.hataSil'))
      }
    },
    reject: () => {}
  })
}
</script>

<style scoped>
.donemler-sayfasi {
  padding: 0;
}
.sirket-dropdown {
  min-width: min(200px, 100%);
}
.w-full {
  width: 100%;
}

/* KPI şeridi: toplam / aktif / kilitli / son kapanış */
.kpi-serit {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 12px;
  margin-bottom: 20px;
}
.kpi-kart {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  border: 1px solid var(--border);
  border-radius: 14px;
  background: var(--bg-card);
  min-width: 0;
}
.kpi-ikon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  flex-shrink: 0;
  border-radius: 11px;
  font-size: 16px;
}
.kpi-ikon.toplam {
  background: var(--accent-soft);
  color: var(--accent);
  border: 1px solid var(--accent-soft-strong);
}
.kpi-ikon.aktif {
  background: var(--success-soft);
  color: var(--success);
  border: 1px solid var(--success-border);
}
.kpi-ikon.kilitli {
  background: var(--warning-soft);
  color: var(--warning);
  border: 1px solid var(--warning-border);
}
.kpi-ikon.kapanis {
  background: var(--bg-secondary);
  color: var(--text-secondary);
  border: 1px solid var(--border);
}
.kpi-metin {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}
.kpi-metin small {
  color: var(--text-muted);
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.4px;
}
.kpi-metin strong {
  color: var(--text-primary);
  font-size: 20px;
  font-weight: 800;
  line-height: 1.1;
  font-variant-numeric: tabular-nums;
}

/* Dönem kart ızgarası */
.donem-izgara {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 14px;
}
.donem-kart {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 16px;
  border: 1px solid var(--border);
  border-radius: 14px;
  background: var(--bg-card);
  transition: border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
}
.donem-kart:hover {
  border-color: var(--accent-border);
  box-shadow: var(--elev-1, 0 2px 10px rgba(0, 0, 0, 0.12));
  transform: translateY(-2px);
}
.donem-kart-aktif {
  border-color: var(--accent);
  background: linear-gradient(180deg, var(--accent-soft) 0%, var(--bg-card) 55%);
}
.donem-kart-kilitli {
  opacity: 0.82;
}
.donem-kart-ust {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.donem-no {
  color: var(--text-muted);
  font-size: 12px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}
.donem-rozetler {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}
.donem-ad {
  margin: 0;
  color: var(--text-primary);
  font-size: 16px;
  font-weight: 700;
  line-height: 1.3;
  overflow-wrap: anywhere;
}
.donem-aralik {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  color: var(--text-secondary);
  font-size: 12.5px;
}
.donem-tarih {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  white-space: nowrap;
}
.donem-tarih i {
  color: var(--text-muted);
  font-size: 11.5px;
}
.donem-tire {
  color: var(--text-muted);
}
.donem-kart-alt {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-top: auto;
  padding-top: 10px;
  border-top: 1px solid var(--border);
}
.donem-gun {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  color: var(--text-muted);
  font-size: 12px;
  font-variant-numeric: tabular-nums;
}
.donem-eylemler {
  display: flex;
  align-items: center;
  gap: 2px;
}
.donem-bos {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 48px 20px;
  border: 1px dashed var(--border);
  border-radius: 14px;
  color: var(--text-muted);
  text-align: center;
}
.donem-bos i {
  font-size: 2.2rem;
}
.donem-bos p {
  margin: 0;
  font-size: 14px;
}

.form-grid {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.field label {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
}
.kapanis-uyari {
  margin-bottom: 16px;
}
.kapanis-kart {
  margin-top: 20px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
}
.kapanis-satir {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0;
  border-bottom: 1px solid var(--border);
  font-size: 13px;
}
.kapanis-satir:last-child {
  border-bottom: none;
}
.kapanis-yil {
  font-weight: 700;
  color: var(--accent);
  min-width: 48px;
}
.kapanis-tarih {
  color: var(--text-muted);
  min-width: 150px;
}
.kapanis-not {
  color: var(--text-secondary);
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
