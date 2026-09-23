<template>
  <div class="donemler-sayfasi">
    <div class="sayfa-baslik">
      <h1 class="page-title">
        {{ t('donemler.title') }}
      </h1>
      <div class="baslik-aksiyon">
        <Dropdown
          v-model="seciliSirketId"
          :options="sirketler"
          option-label="ad"
          option-value="id"
          :placeholder="t('donemler.sirketSecin')"
          class="sirket-dropdown"
          @change="donemleriYukle"
        />
        <Button
          :label="t('donemler.yilSonuKapat')"
          icon="pi pi-lock"
          severity="warning"
          :disabled="!seciliSirketId"
          @click="kapanisDialog = true"
        />
        <Button
          :label="t('donemler.yeniDonem')"
          icon="pi pi-plus"
          :disabled="!seciliSirketId"
          @click="dialogAc"
        />
      </div>
    </div>

    <DataTable
      :value="donemler"
      striped-rows
      responsive-layout="scroll"
      :loading="yukleniyor"
    >
      <template #empty>
        <EmptyState />
      </template>
      <Column
        field="id"
        header="#"
        style="width: 60px"
      />
      <Column
        field="ad"
        :header="t('donemler.donemAdi')"
        sortable
      />
      <Column
        field="baslangic"
        :header="t('donemler.baslangic')"
      >
        <template #body="{ data }">
          {{ data.baslangic }}
        </template>
      </Column>
      <Column
        field="bitis"
        :header="t('donemler.bitis')"
      >
        <template #body="{ data }">
          {{ data.bitis }}
        </template>
      </Column>
      <Column
        field="aktif"
        :header="t('common.status')"
      >
        <template #body="{ data }">
          <div class="durum-hucre">
            <Tag
              :value="data.aktif ? t('donemler.aktif') : t('donemler.pasif')"
              :severity="data.aktif ? 'success' : 'danger'"
            />
            <Tag
              v-if="data.kilitli"
              :value="t('donemler.kilitli')"
              severity="warning"
              icon="pi pi-lock"
            />
          </div>
        </template>
      </Column>
      <Column
        :header="t('donemler.islem')"
        style="width: 170px"
      >
        <template #body="{ data }">
          <Button
            v-if="!data.aktif"
            icon="pi pi-check-circle"
            class="p-button-rounded p-button-text p-button-success"
            :title="t('donemler.aktifYap')"
            @click="aktifYap(data)"
          />
          <Button
            v-if="!data.kilitli"
            icon="pi pi-lock"
            class="p-button-rounded p-button-text p-button-warning"
            :title="t('donemler.kilitle')"
            @click="kilitle(data)"
          />
          <Button
            v-else
            icon="pi pi-lock-open"
            class="p-button-rounded p-button-text p-button-secondary"
            :title="t('donemler.kilidiAc')"
            @click="kilidiAc(data)"
          />
          <Button
            icon="pi pi-pencil"
            :aria-label="$t('common.edit')"
            class="p-button-rounded p-button-text"
            :disabled="data.kilitli"
            @click="dialogAc(data)"
          />
          <Button
            icon="pi pi-trash"
            :aria-label="$t('common.delete')"
            class="p-button-rounded p-button-text p-button-danger"
            :disabled="data.kilitli"
            @click="sil(data)"
          />
        </template>
      </Column>
    </DataTable>

    <Dialog
      v-model:visible="kapanisDialog"
      :header="t('donemler.yilSonuKapat')"
      modal
      :style="{ width: '460px' }"
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
      :style="{ width: '450px' }"
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
import { getLocalDateString, formatTarihSaat } from '../utils/format.js'
import { useAuthStore } from '../stores/authStore.js'
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
.sayfa-baslik {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  gap: 16px;
  flex-wrap: wrap;
}
.sayfa-baslik h1 {
  margin: 0;
}
.baslik-aksiyon {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}
.sirket-dropdown {
  min-width: min(200px, 100%);
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
.w-full {
  width: 100%;
}
.durum-hucre {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
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
}
</style>
