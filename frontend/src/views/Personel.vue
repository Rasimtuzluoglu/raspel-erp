<template>
  <div class="personel-sayfasi">
    <div class="sayfa-baslik">
      <h1 class="page-title">
        İnsan Kaynakları
      </h1>
      <Button
        label="Yeni Personel"
        icon="pi pi-plus"
        @click="personelDialogAc()"
      />
    </div>

    <Toolbar class="toolbar">
      <template #end>
        <Button
          label="Excel"
          icon="pi pi-file-excel"
          class="p-button-sm p-button-outlined"
          @click="excelIndir"
        />
      </template>
    </Toolbar>

    <TabView>
      <TabPanel header="Personel Listesi">
        <DataTable
          :value="personeller"
          striped-rows
          :loading="yukleniyor"
        >
          <Column
            field="ad"
            header="Ad"
            sortable
          />
          <Column
            field="soyad"
            header="Soyad"
            sortable
          />
          <Column
            field="departman"
            header="Departman"
          />
          <Column
            field="pozisyon"
            header="Pozisyon"
          />
          <Column
            field="telefon"
            header="Telefon"
          />
          <Column
            field="email"
            header="E-posta"
          />
          <Column
            field="aktif"
            header="Durum"
          >
            <template #body="{ data }">
              <Tag
                :value="data.aktif ? 'Aktif' : 'Pasif'"
                :severity="data.aktif ? 'success' : 'danger'"
              />
            </template>
          </Column>
          <Column
            header="İşlem"
            style="width:140px"
          >
            <template #body="{ data }">
              <Button
                icon="pi pi-pencil"
                class="p-button-rounded p-button-text"
                @click="personelDialogAc(data)"
              />
              <Button
                icon="pi pi-calendar-plus"
                class="p-button-rounded p-button-text p-button-info"
                title="İzin Ekle"
                @click="izinDialogAc(data)"
              />
              <Button
                icon="pi pi-trash"
                class="p-button-rounded p-button-text p-button-danger"
                @click="personelSil(data)"
              />
            </template>
          </Column>
        </DataTable>
      </TabPanel>

      <TabPanel header="İzin Talepleri">
        <DataTable
          :value="tumIzinler"
          striped-rows
        >
          <Column
            field="personelAdi"
            header="Personel"
          />
          <Column
            field="izinTuru"
            header="İzin Türü"
          />
          <Column
            field="baslangic"
            header="Başlangıç"
          />
          <Column
            field="bitis"
            header="Bitiş"
          />
          <Column
            field="gunSayisi"
            header="Gün"
          />
          <Column
            field="durum"
            header="Durum"
          >
            <template #body="{ data }">
              <Tag
                :value="data.durum"
                :severity="data.durum === 'ONAYLANDI' ? 'success' : data.durum === 'REDDEDILDI' ? 'danger' : 'warn'"
              />
            </template>
          </Column>
        </DataTable>
      </TabPanel>
    </TabView>

    <Dialog
      v-model:visible="personelDialog"
      :header="duzenleme ? 'Personel Düzenle' : 'Yeni Personel'"
      modal
      :style="{ width: '600px' }"
    >
      <div class="form-grid">
        <div class="field-row">
          <div class="field">
            <label>Ad *</label><InputText
              v-model="personelForm.ad"
              class="w-full"
            />
          </div>
          <div class="field">
            <label>Soyad *</label><InputText
              v-model="personelForm.soyad"
              class="w-full"
            />
          </div>
        </div>
        <div class="field-row">
          <div class="field">
            <label>TC Kimlik</label><InputText
              v-model="personelForm.tcKimlik"
              class="w-full"
            />
          </div>
          <div class="field">
            <label>Doğum Tarihi</label><DatePicker
              v-model="personelForm.dogumTarihi"
              date-format="dd/mm/yy"
              class="w-full"
            />
          </div>
        </div>
        <div class="field-row">
          <div class="field">
            <label>Departman</label><InputText
              v-model="personelForm.departman"
              class="w-full"
            />
          </div>
          <div class="field">
            <label>Pozisyon</label><InputText
              v-model="personelForm.pozisyon"
              class="w-full"
            />
          </div>
        </div>
        <div class="field-row">
          <div class="field">
            <label>Maaş</label><InputNumber
              v-model="personelForm.maas"
              mode="currency"
              currency="TRY"
              class="w-full"
            />
          </div>
          <div class="field">
            <label>İşe Giriş</label><DatePicker
              v-model="personelForm.iseGirisTarihi"
              date-format="dd/mm/yy"
              class="w-full"
            />
          </div>
        </div>
        <div class="field-row">
          <div class="field">
            <label>Telefon</label><InputText
              v-model="personelForm.telefon"
              class="w-full"
            />
          </div>
          <div class="field">
            <label>E-posta</label><InputText
              v-model="personelForm.email"
              class="w-full"
            />
          </div>
        </div>
        <div class="field">
          <label>Adres</label><Textarea
            v-model="personelForm.adres"
            rows="2"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>Aktif</label><InputSwitch v-model="personelForm.aktif" />
        </div>
      </div>
      <template #footer>
        <Button
          label="İptal"
          icon="pi pi-times"
          class="p-button-text"
          @click="personelDialog = false"
        />
        <Button
          label="Kaydet"
          icon="pi pi-check"
          :loading="kaydediliyor"
          @click="personelKaydet"
        />
      </template>
    </Dialog>

    <Dialog
      v-model:visible="izinDialog"
      header="İzin Ekle"
      modal
      :style="{ width: '450px' }"
    >
      <div class="form-grid">
        <div class="field">
          <label>Personel</label><InputText
            :value="izinPersonelAdi"
            disabled
            class="w-full"
          />
        </div>
        <div class="field">
          <label>İzin Türü *</label>
          <Dropdown
            v-model="izinForm.izinTuru"
            :options="izinTurleri"
            placeholder="Seçin"
            class="w-full"
          />
        </div>
        <div class="field-row">
          <div class="field">
            <label>Başlangıç</label><DatePicker
              v-model="izinForm.baslangic"
              date-format="dd/mm/yy"
              class="w-full"
            />
          </div>
          <div class="field">
            <label>Bitiş</label><DatePicker
              v-model="izinForm.bitis"
              date-format="dd/mm/yy"
              class="w-full"
            />
          </div>
        </div>
        <div class="field">
          <label>Açıklama</label><Textarea
            v-model="izinForm.aciklama"
            rows="2"
            class="w-full"
          />
        </div>
      </div>
      <template #footer>
        <Button
          label="İptal"
          icon="pi pi-times"
          class="p-button-text"
          @click="izinDialog = false"
        />
        <Button
          label="Kaydet"
          icon="pi pi-check"
          :loading="kaydediliyor"
          @click="izinKaydet"
        />
      </template>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useConfirm } from 'primevue/useconfirm'
import { personelAPI, personelIzinAPI, excelAPI } from '../api/index.js'
const toastBildirim = useToastBildirim()
const confirm = useConfirm()

const personeller = ref([])
const tumIzinler = ref([])
const yukleniyor = ref(false)
const kaydediliyor = ref(false)
const personelDialog = ref(false)
const izinDialog = ref(false)
const duzenleme = ref(false)
const izinPersonelId = ref(null)
const izinPersonelAdi = ref('')
const personelForm = ref(defaultForm())
const izinForm = ref({ izinTuru: '', baslangic: null, bitis: null, aciklama: '' })
const izinTurleri = ['YILLIK_IZIN', 'HASTA_IZNI', 'MAZERET_IZNI', 'DOGUM_IZNI', 'BABALIK_IZNI', 'EVLILIK_IZNI', 'UCRETSIZ_IZIN']

function defaultForm() { return { ad: '', soyad: '', tcKimlik: '', dogumTarihi: null, iseGirisTarihi: new Date(), departman: '', pozisyon: '', maas: null, telefon: '', email: '', adres: '', aktif: true } }

onMounted(async () => {
  yukleniyor.value = true
  try {
    const [pR, iR] = await Promise.all([personelAPI.getAll(), personelIzinAPI.getAll()])
    personeller.value = pR.data?.content || pR.data || []
    tumIzinler.value = iR.data?.content || iR.data || []
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || 'Personel verileri yüklenirken hata oluştu')
  }
  yukleniyor.value = false
})

const excelIndir = async () => {
  try {
    const res = await excelAPI.personel()
    const url = window.URL.createObjectURL(new Blob([res.data]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', 'Personel.xlsx')
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)
  } catch { /* silent */ }
}

const personelDialogAc = (data) => {
  duzenleme.value = !!data
  personelForm.value = data ? { ...data, dogumTarihi: data.dogumTarihi ? new Date(data.dogumTarihi) : null, iseGirisTarihi: data.iseGirisTarihi ? new Date(data.iseGirisTarihi) : new Date() } : defaultForm()
  personelDialog.value = true
}

const personelKaydet = async () => {
  kaydediliyor.value = true
  try {
    const p = { ...personelForm.value, dogumTarihi: personelForm.value.dogumTarihi?.toISOString().split('T')[0], iseGirisTarihi: personelForm.value.iseGirisTarihi?.toISOString().split('T')[0] }
    if (duzenleme.value) await personelAPI.update(personelForm.value.id, p)
    else await personelAPI.create(p)
    personelDialog.value = false
    const r2 = await personelAPI.getAll(); personeller.value = r2.data?.content || r2.data || []
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || 'Personel kaydedilirken hata oluştu')
  }
  kaydediliyor.value = false
}

const personelSil = (data) => {
  confirm.require({
    message: 'Bu kaydı silmek istediğinize emin misiniz?',
    header: 'Silme Onayı',
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: 'Evet, Sil',
    rejectLabel: 'İptal',
    accept: async () => {
      try {
        await personelAPI.delete(data.id)
        personeller.value = personeller.value.filter(p => p.id !== data.id)
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || err?.message || 'Personel silinirken hata oluştu')
      }
    },
    reject: () => {}
  })
}

const izinDialogAc = (data) => {
  izinPersonelId.value = data.id
  izinPersonelAdi.value = `${data.ad} ${data.soyad}`
  izinForm.value = { izinTuru: '', baslangic: new Date(), bitis: new Date(), aciklama: '' }
  izinDialog.value = true
}

const izinKaydet = async () => {
  kaydediliyor.value = true
  try {
    const gunSayisi = Math.ceil((izinForm.value.bitis - izinForm.value.baslangic) / (1000 * 60 * 60 * 24)) + 1
    await personelIzinAPI.create({ personelId: izinPersonelId.value, izinTuru: izinForm.value.izinTuru, baslangic: izinForm.value.baslangic?.toISOString().split('T')[0], bitis: izinForm.value.bitis?.toISOString().split('T')[0], gunSayisi, aciklama: izinForm.value.aciklama })
    izinDialog.value = false
    const r3 = await personelIzinAPI.getAll(); tumIzinler.value = r3.data?.content || r3.data || []
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || 'İzin kaydedilirken hata oluştu')
  }
  kaydediliyor.value = false
}
</script>

<style scoped>
.personel-sayfasi { padding: 0; }
.sayfa-baslik { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.sayfa-baslik h1 { margin: 0; }
.form-grid { display: flex; flex-direction: column; gap: 16px; }
.field-row { display: flex; gap: 16px; }
.field-row .field { flex: 1; }
.field { display: flex; flex-direction: column; gap: 6px; }
.field label { font-size: 13px; font-weight: 600; color: var(--text-secondary); }
.w-full { width: 100%; }
</style>