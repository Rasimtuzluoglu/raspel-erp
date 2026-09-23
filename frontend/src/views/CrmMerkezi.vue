<template>
  <div class="crm-merkezi-sayfasi">
    <div class="sayfa-baslik">
      <h1 class="page-title">
        <i class="pi pi-bullseye" /> {{ t('crmMerkezi.title') }}
      </h1>
    </div>

    <TabView>
      <!-- LEAD -->
      <TabPanel :header="t('crmMerkezi.leadler')">
        <div class="sekme-aksiyon">
          <Button
            :label="t('crmMerkezi.yeniLead')"
            icon="pi pi-plus"
            @click="leadDialogAc"
          />
        </div>
        <DataTable
          :value="leadler"
          striped-rows
          responsive-layout="scroll"
          :loading="yukleniyor"
        >
          <template #empty>
            <EmptyState />
          </template>
          <Column
            field="ad"
            :header="t('crmMerkezi.ad')"
            sortable
          />
          <Column
            field="firma"
            :header="t('crmMerkezi.firma')"
          />
          <Column
            field="email"
            :header="t('crmMerkezi.email')"
          />
          <Column
            field="telefon"
            :header="t('crmMerkezi.telefon')"
          />
          <Column
            field="durum"
            :header="t('common.status')"
          >
            <template #body="{ data }">
              <Tag
                :value="data.durum"
                :severity="leadSeverity(data.durum)"
              />
            </template>
          </Column>
          <Column
            field="skor"
            :header="t('crmMerkezi.skor')"
            style="width: 80px"
          />
          <Column
            field="kampanyaAd"
            :header="t('crmMerkezi.kampanya')"
          />
          <Column
            :header="t('common.actions')"
            style="width: 150px"
          >
            <template #body="{ data }">
              <Button
                v-if="data.durum !== 'DONUSTURULDU'"
                icon="pi pi-user-plus"
                class="p-button-rounded p-button-text p-button-success"
                :title="t('crmMerkezi.donustur')"
                @click="leadDonustur(data)"
              />
              <Button
                icon="pi pi-pencil"
                :aria-label="$t('common.edit')"
                class="p-button-rounded p-button-text"
                @click="leadDialogAc(data)"
              />
              <Button
                icon="pi pi-trash"
                :aria-label="$t('common.delete')"
                class="p-button-rounded p-button-text p-button-danger"
                @click="leadSil(data)"
              />
            </template>
          </Column>
        </DataTable>
      </TabPanel>

      <!-- AKTIVITE -->
      <TabPanel :header="t('crmMerkezi.aktiviteler')">
        <div class="sekme-aksiyon">
          <Button
            :label="t('crmMerkezi.yeniAktivite')"
            icon="pi pi-plus"
            @click="aktiviteDialog = true"
          />
        </div>
        <DataTable
          :value="aktiviteler"
          striped-rows
          responsive-layout="scroll"
          :loading="yukleniyor"
        >
          <template #empty>
            <EmptyState />
          </template>
          <Column
            field="baslik"
            :header="t('crmMerkezi.baslik')"
          />
          <Column
            field="tur"
            :header="t('crmMerkezi.tur')"
          >
            <template #body="{ data }">
              <Tag
                :value="data.tur"
                severity="info"
              />
            </template>
          </Column>
          <Column
            field="cariHesapAd"
            :header="t('crmMerkezi.cari')"
          />
          <Column
            field="planlananTarih"
            :header="t('crmMerkezi.planlananTarih')"
          >
            <template #body="{ data }">
              {{ data.planlananTarih ? formatTarihSaat(data.planlananTarih) : '-' }}
            </template>
          </Column>
          <Column
            field="tamamlandi"
            :header="t('common.status')"
          >
            <template #body="{ data }">
              <Tag
                :value="data.tamamlandi ? t('crmMerkezi.tamamlandi') : t('crmMerkezi.bekliyor')"
                :severity="data.tamamlandi ? 'success' : 'warning'"
              />
            </template>
          </Column>
          <Column
            :header="t('common.actions')"
            style="width: 120px"
          >
            <template #body="{ data }">
              <Button
                :icon="data.tamamlandi ? 'pi pi-undo' : 'pi pi-check'"
                class="p-button-rounded p-button-text"
                :title="data.tamamlandi ? t('crmMerkezi.geriAl') : t('crmMerkezi.tamamla')"
                @click="aktiviteTamamla(data)"
              />
              <Button
                icon="pi pi-trash"
                :aria-label="$t('common.delete')"
                class="p-button-rounded p-button-text p-button-danger"
                @click="aktiviteSil(data)"
              />
            </template>
          </Column>
        </DataTable>
      </TabPanel>

      <!-- KAMPANYA -->
      <TabPanel :header="t('crmMerkezi.kampanyalar')">
        <div class="sekme-aksiyon">
          <Button
            :label="t('crmMerkezi.yeniKampanya')"
            icon="pi pi-plus"
            @click="kampanyaDialogAc"
          />
        </div>
        <DataTable
          :value="kampanyalar"
          striped-rows
          responsive-layout="scroll"
          :loading="yukleniyor"
        >
          <template #empty>
            <EmptyState />
          </template>
          <Column
            field="ad"
            :header="t('crmMerkezi.ad')"
          />
          <Column
            field="tur"
            :header="t('crmMerkezi.tur')"
          />
          <Column
            field="durum"
            :header="t('common.status')"
          >
            <template #body="{ data }">
              <Tag
                :value="data.durum"
                :severity="kampanyaSeverity(data.durum)"
              />
            </template>
          </Column>
          <Column
            field="baslangic"
            :header="t('crmMerkezi.baslangic')"
          />
          <Column
            field="bitis"
            :header="t('crmMerkezi.bitis')"
          />
          <Column
            field="butce"
            :header="t('crmMerkezi.butce')"
          >
            <template #body="{ data }">
              {{ data.butce ? formatCurrency(data.butce) : '-' }}
            </template>
          </Column>
          <Column
            field="leadSayisi"
            :header="t('crmMerkezi.leadSayisi')"
          />
          <Column
            :header="t('common.actions')"
            style="width: 110px"
          >
            <template #body="{ data }">
              <Button
                icon="pi pi-pencil"
                :aria-label="$t('common.edit')"
                class="p-button-rounded p-button-text"
                @click="kampanyaDialogAc(data)"
              />
              <Button
                icon="pi pi-trash"
                :aria-label="$t('common.delete')"
                class="p-button-rounded p-button-text p-button-danger"
                @click="kampanyaSil(data)"
              />
            </template>
          </Column>
        </DataTable>
      </TabPanel>
    </TabView>

    <!-- Lead Dialog -->
    <Dialog
      v-model:visible="leadDialog"
      :header="leadDuzenleme ? t('crmMerkezi.leadDuzenle') : t('crmMerkezi.yeniLead')"
      modal
      :style="{ width: '520px' }"
    >
      <div class="form-grid">
        <div class="field">
          <label>{{ t('crmMerkezi.adZorunlu') }}</label>
          <InputText
            v-model="leadForm.ad"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('crmMerkezi.firma') }}</label>
          <InputText
            v-model="leadForm.firma"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('crmMerkezi.email') }}</label>
          <InputText
            v-model="leadForm.email"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('crmMerkezi.telefon') }}</label>
          <InputText
            v-model="leadForm.telefon"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('crmMerkezi.durum') }}</label>
          <Dropdown
            v-model="leadForm.durum"
            :options="leadDurumlari"
            option-label="label"
            option-value="value"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('crmMerkezi.skor') }}</label>
          <InputNumber
            v-model="leadForm.skor"
            :min="0"
            :max="100"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('crmMerkezi.tahminiDeger') }}</label>
          <InputNumber
            v-model="leadForm.tahminiDeger"
            mode="currency"
            currency="TRY"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('crmMerkezi.kampanya') }}</label>
          <Dropdown
            v-model="leadForm.kampanyaId"
            :options="kampanyalar"
            option-label="ad"
            option-value="id"
            show-clear
            class="w-full"
          />
        </div>
        <div class="field full-width">
          <label>{{ t('crmMerkezi.aciklama') }}</label>
          <Textarea
            v-model="leadForm.aciklama"
            rows="2"
            class="w-full"
          />
        </div>
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          icon="pi pi-times"
          class="p-button-text"
          @click="leadDialog = false"
        />
        <Button
          :label="t('common.save')"
          icon="pi pi-check"
          :loading="kaydediliyor"
          @click="leadKaydet"
        />
      </template>
    </Dialog>

    <!-- Aktivite Dialog -->
    <Dialog
      v-model:visible="aktiviteDialog"
      :header="t('crmMerkezi.yeniAktivite')"
      modal
      :style="{ width: '480px' }"
    >
      <div class="form-grid">
        <div class="field">
          <label>{{ t('crmMerkezi.turZorunlu') }}</label>
          <Dropdown
            v-model="aktiviteForm.tur"
            :options="aktiviteTurleri"
            option-label="label"
            option-value="value"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('crmMerkezi.baslikZorunlu') }}</label>
          <InputText
            v-model="aktiviteForm.baslik"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('crmMerkezi.cari') }}</label>
          <Dropdown
            v-model="aktiviteForm.cariHesapId"
            :options="cariler"
            option-label="ad"
            option-value="id"
            show-clear
            filter
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('crmMerkezi.planlananTarih') }}</label>
          <DatePicker
            v-model="aktiviteForm.planlananTarih"
            show-time
            date-format="dd.mm.yy"
            class="w-full"
          />
        </div>
        <div class="field full-width">
          <label>{{ t('crmMerkezi.aciklama') }}</label>
          <Textarea
            v-model="aktiviteForm.aciklama"
            rows="2"
            class="w-full"
          />
        </div>
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          icon="pi pi-times"
          class="p-button-text"
          @click="aktiviteDialog = false"
        />
        <Button
          :label="t('common.save')"
          icon="pi pi-check"
          :loading="kaydediliyor"
          @click="aktiviteKaydet"
        />
      </template>
    </Dialog>

    <!-- Kampanya Dialog -->
    <Dialog
      v-model:visible="kampanyaDialog"
      :header="kampanyaDuzenleme ? t('crmMerkezi.kampanyaDuzenle') : t('crmMerkezi.yeniKampanya')"
      modal
      :style="{ width: '520px' }"
    >
      <div class="form-grid">
        <div class="field">
          <label>{{ t('crmMerkezi.adZorunlu') }}</label>
          <InputText
            v-model="kampanyaForm.ad"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('crmMerkezi.tur') }}</label>
          <Dropdown
            v-model="kampanyaForm.tur"
            :options="kampanyaTurleri"
            option-label="label"
            option-value="value"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('crmMerkezi.durum') }}</label>
          <Dropdown
            v-model="kampanyaForm.durum"
            :options="kampanyaDurumlari"
            option-label="label"
            option-value="value"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('crmMerkezi.butce') }}</label>
          <InputNumber
            v-model="kampanyaForm.butce"
            mode="currency"
            currency="TRY"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('crmMerkezi.baslangic') }}</label>
          <DatePicker
            v-model="kampanyaForm.baslangic"
            date-format="dd.mm.yy"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('crmMerkezi.bitis') }}</label>
          <DatePicker
            v-model="kampanyaForm.bitis"
            date-format="dd.mm.yy"
            class="w-full"
          />
        </div>
        <div class="field full-width">
          <label>{{ t('crmMerkezi.aciklama') }}</label>
          <Textarea
            v-model="kampanyaForm.aciklama"
            rows="2"
            class="w-full"
          />
        </div>
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          icon="pi pi-times"
          class="p-button-text"
          @click="kampanyaDialog = false"
        />
        <Button
          :label="t('common.save')"
          icon="pi pi-check"
          :loading="kaydediliyor"
          @click="kampanyaKaydet"
        />
      </template>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useConfirm } from 'primevue/useconfirm'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { crmAPI, cariHesapAPI } from '../api/index.js'
import { unwrapList } from '../api/utils/unwrap.js'
import { useI18n } from 'vue-i18n'
import { formatCurrency, formatTarihSaat, getLocalDateString } from '../utils/format.js'

const { t } = useI18n()
const toastBildirim = useToastBildirim()
const confirm = useConfirm()

const leadler = ref([])
const aktiviteler = ref([])
const kampanyalar = ref([])
const cariler = ref([])
const yukleniyor = ref(false)
const kaydediliyor = ref(false)

const leadDialog = ref(false)
const leadDuzenleme = ref(false)
const leadId = ref(null)
const leadForm = ref(bosLead())

const aktiviteDialog = ref(false)
const aktiviteForm = ref(bosAktivite())

const kampanyaDialog = ref(false)
const kampanyaDuzenleme = ref(false)
const kampanyaId = ref(null)
const kampanyaForm = ref(bosKampanya())

const leadDurumlari = computed(() => [
  { label: t('crmMerkezi.leadYeni'), value: 'YENI' },
  { label: t('crmMerkezi.leadNitelikli'), value: 'NITELIKLI' },
  { label: t('crmMerkezi.leadDonusturuldu'), value: 'DONUSTURULDU' },
  { label: t('crmMerkezi.leadKaybedildi'), value: 'KAYBEDILDI' }
])
const aktiviteTurleri = computed(() => [
  { label: t('crmMerkezi.aktiviteArama'), value: 'ARAMA' },
  { label: t('crmMerkezi.aktiviteToplanti'), value: 'TOPLANTI' },
  { label: t('crmMerkezi.aktiviteEposta'), value: 'EMAIL' },
  { label: t('crmMerkezi.aktiviteNot'), value: 'NOT' },
  { label: t('crmMerkezi.aktiviteGorev'), value: 'GOREV' }
])
const kampanyaTurleri = computed(() => [
  { label: t('crmMerkezi.aktiviteEposta'), value: 'EMAIL' },
  { label: t('crmMerkezi.kampanyaSms'), value: 'SMS' },
  { label: t('crmMerkezi.kampanyaSosyal'), value: 'SOSYAL' },
  { label: t('crmMerkezi.kampanyaEtkinlik'), value: 'ETKINLIK' },
  { label: t('crmMerkezi.kampanyaDiger'), value: 'DIGER' }
])
const kampanyaDurumlari = computed(() => [
  { label: t('crmMerkezi.kampanyaPlanlandi'), value: 'PLANLANDI' },
  { label: t('crmMerkezi.kampanyaAktif'), value: 'AKTIF' },
  { label: t('crmMerkezi.kampanyaTamamlandi'), value: 'TAMAMLANDI' },
  { label: t('crmMerkezi.kampanyaIptal'), value: 'IPTAL' }
])

function bosLead() {
  return {
    ad: '', firma: '', email: '', telefon: '', kaynak: '',
    durum: 'YENI', skor: 0, tahminiDeger: null, kampanyaId: null, aciklama: ''
  }
}
function bosAktivite() {
  return { tur: 'ARAMA', baslik: '', cariHesapId: null, planlananTarih: null, aciklama: '' }
}
function bosKampanya() {
  return { ad: '', tur: 'EMAIL', durum: 'PLANLANDI', butce: null, baslangic: null, bitis: null, aciklama: '' }
}

const leadSeverity = (d) => ({ YENI: 'info', NITELIKLI: 'success', DONUSTURULDU: 'contrast', KAYBEDILDI: 'danger' }[d] || 'info')
const kampanyaSeverity = (d) => ({ PLANLANDI: 'info', AKTIF: 'success', TAMAMLANDI: 'contrast', IPTAL: 'danger' }[d] || 'info')

const yukle = async () => {
  yukleniyor.value = true
  try {
    const [l, a, k] = await Promise.all([
      crmAPI.getLeadler({ size: 200 }),
      crmAPI.getAktiviteler({ size: 200 }),
      crmAPI.getKampanyalar({ size: 200 })
    ])
    leadler.value = unwrapList(l)
    aktiviteler.value = unwrapList(a)
    kampanyalar.value = unwrapList(k)
  } catch {
    toastBildirim.hata(t('crmMerkezi.hataYukleme'))
  } finally {
    yukleniyor.value = false
  }
}

onMounted(async () => {
  await yukle()
  try {
    const c = await cariHesapAPI.getAll({ size: 500 })
    cariler.value = unwrapList(c)
  } catch {
    /* opsiyonel */
  }
})

// Lead
const leadDialogAc = (data) => {
  leadDuzenleme.value = !!data
  leadId.value = data?.id || null
  leadForm.value = data ? { ...bosLead(), ...data } : bosLead()
  leadDialog.value = true
}
const leadKaydet = async () => {
  if (!leadForm.value.ad?.trim()) {
    toastBildirim.uyari(t('crmMerkezi.adGerekli'))
    return
  }
  kaydediliyor.value = true
  try {
    if (leadDuzenleme.value) await crmAPI.leadGuncelle(leadId.value, leadForm.value)
    else await crmAPI.leadOlustur(leadForm.value)
    leadDialog.value = false
    await yukle()
    toastBildirim.basarili(t('crmMerkezi.kaydedildi'))
  } catch (err) {
    toastBildirim.hata(err.response?.data?.message || t('crmMerkezi.kaydetHatasi'))
  } finally {
    kaydediliyor.value = false
  }
}
const leadSil = (data) => {
  confirm.require({
    message: t('common.confirmDelete'),
    header: t('common.silmeOnayi'),
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: t('common.evetSil'),
    rejectLabel: t('common.vazgec'),
    accept: async () => {
      try {
        await crmAPI.leadSil(data.id)
        await yukle()
      } catch (err) {
        toastBildirim.hata(err.response?.data?.message || t('crmMerkezi.silmeHatasi'))
      }
    },
    reject: () => {}
  })
}
const leadDonustur = (data) => {
  if (!data.cariHesapId) {
    toastBildirim.uyari(t('crmMerkezi.donusturmekIcinCari'))
    return
  }
  confirm.require({
    message: t('crmMerkezi.donusturOnay'),
    header: t('crmMerkezi.donustur'),
    icon: 'pi pi-question-circle',
    acceptLabel: t('common.save'),
    rejectLabel: t('common.vazgec'),
    accept: async () => {
      try {
        await crmAPI.leadDonustur(data.id, data.cariHesapId)
        await yukle()
        toastBildirim.basarili(t('crmMerkezi.donusturuldu'))
      } catch (err) {
        toastBildirim.hata(err.response?.data?.message || t('crmMerkezi.islemHatasi'))
      }
    },
    reject: () => {}
  })
}

// Aktivite
const aktiviteKaydet = async () => {
  if (!aktiviteForm.value.baslik?.trim()) {
    toastBildirim.uyari(t('crmMerkezi.baslikGerekli'))
    return
  }
  kaydediliyor.value = true
  try {
    await crmAPI.aktiviteOlustur(aktiviteForm.value)
    aktiviteDialog.value = false
    aktiviteForm.value = bosAktivite()
    await yukle()
    toastBildirim.basarili(t('crmMerkezi.kaydedildi'))
  } catch (err) {
    toastBildirim.hata(err.response?.data?.message || t('crmMerkezi.kaydetHatasi'))
  } finally {
    kaydediliyor.value = false
  }
}
const aktiviteTamamla = async (data) => {
  try {
    await crmAPI.aktiviteTamamla(data.id, !data.tamamlandi)
    await yukle()
  } catch (err) {
    toastBildirim.hata(err.response?.data?.message || t('crmMerkezi.islemHatasi'))
  }
}
const aktiviteSil = (data) => {
  confirm.require({
    message: t('common.confirmDelete'),
    header: t('common.silmeOnayi'),
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: t('common.evetSil'),
    rejectLabel: t('common.vazgec'),
    accept: async () => {
      try {
        await crmAPI.aktiviteSil(data.id)
        await yukle()
      } catch (err) {
        toastBildirim.hata(err.response?.data?.message || t('crmMerkezi.silmeHatasi'))
      }
    },
    reject: () => {}
  })
}

// Kampanya
const kampanyaDialogAc = (data) => {
  kampanyaDuzenleme.value = !!data
  kampanyaId.value = data?.id || null
  kampanyaForm.value = data
    ? {
        ...bosKampanya(),
        ...data,
        baslangic: data.baslangic ? new Date(data.baslangic) : null,
        bitis: data.bitis ? new Date(data.bitis) : null
      }
    : bosKampanya()
  kampanyaDialog.value = true
}
const kampanyaKaydet = async () => {
  if (!kampanyaForm.value.ad?.trim()) {
    toastBildirim.uyari(t('crmMerkezi.adGerekli'))
    return
  }
  kaydediliyor.value = true
  try {
    const payload = {
      ...kampanyaForm.value,
      baslangic: getLocalDateString(kampanyaForm.value.baslangic),
      bitis: getLocalDateString(kampanyaForm.value.bitis)
    }
    if (kampanyaDuzenleme.value) await crmAPI.kampanyaGuncelle(kampanyaId.value, payload)
    else await crmAPI.kampanyaOlustur(payload)
    kampanyaDialog.value = false
    await yukle()
    toastBildirim.basarili(t('crmMerkezi.kaydedildi'))
  } catch (err) {
    toastBildirim.hata(err.response?.data?.message || t('crmMerkezi.kaydetHatasi'))
  } finally {
    kaydediliyor.value = false
  }
}
const kampanyaSil = (data) => {
  confirm.require({
    message: t('common.confirmDelete'),
    header: t('common.silmeOnayi'),
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: t('common.evetSil'),
    rejectLabel: t('common.vazgec'),
    accept: async () => {
      try {
        await crmAPI.kampanyaSil(data.id)
        await yukle()
      } catch (err) {
        toastBildirim.hata(err.response?.data?.message || t('crmMerkezi.silmeHatasi'))
      }
    },
    reject: () => {}
  })
}
</script>

<style scoped>
.crm-merkezi-sayfasi {
  padding: 0;
}
.sayfa-baslik {
  margin-bottom: 20px;
}
.page-title {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0;
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--text-primary);
}
.page-title i {
  color: var(--accent);
}
.sekme-aksiyon {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 12px;
}
.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}
.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.field.full-width {
  grid-column: 1 / -1;
}
.field label {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.4px;
}
.w-full {
  width: 100%;
}
@media (max-width: 700px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
  .field.full-width {
    grid-column: auto;
  }
}
</style>
