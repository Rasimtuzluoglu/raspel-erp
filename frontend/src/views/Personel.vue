<template>
  <div class="personel-sayfasi">
    <div class="sayfa-baslik">
      <h1 class="page-title">
        {{ t('personel.title') }}
      </h1>
      <IlkZiyaretIpuclari
        anahtar="personel"
        :baslik="t('personel.ipucuBaslik')"
        :metin="t('personel.ipucuMetin')"
      />
      <Button
        :label="t('personel.yeniPersonel')"
        icon="pi pi-plus"
        @click="personelDialogAc()"
      />
    </div>

    <Toolbar class="toolbar">
      <template #end>
        <Button
          :label="t('personel.excelIndir')"
          icon="pi pi-file-excel"
          class="p-button-sm p-button-outlined"
          @click="excelIndir"
        />
      </template>
    </Toolbar>

    <TabView>
      <TabPanel :header="t('personel.personelListesi')">
        <AppDataTable
          :value="personeller"
          striped-rows
          :loading="yukleniyor"
          :paginator="false"
        >
          <Column
            field="ad"
            :header="t('personel.ad')"
            sortable
          />
          <Column
            field="soyad"
            :header="t('personel.soyad')"
            sortable
          />
          <Column
            field="departman"
            :header="t('personel.departman')"
          />
          <Column
            field="pozisyon"
            :header="t('personel.pozisyon')"
          />
          <Column
            field="rol"
            :header="t('personel.rol')"
            style="width: 110px"
          >
            <template #body="s">
              <span
                v-if="s.data.rol"
                class="rol-badge"
              >{{ rolEtiket(s.data.rol) }}</span>
              <span v-else>-</span>
            </template>
          </Column>
          <Column
            field="telefon"
            :header="t('personel.telefon')"
          />
          <Column
            field="email"
            :header="t('personel.eposta')"
          />
          <Column
            field="aktif"
            :header="t('common.status')"
          >
            <template #body="{ data }">
              <Tag
                :value="data.aktif ? t('personel.aktif') : t('personel.pasif')"
                :severity="data.aktif ? 'success' : 'danger'"
              />
            </template>
          </Column>
          <Column
            :header="t('personel.islem')"
            style="width: 140px"
          >
            <template #body="{ data }">
              <Button
                icon="pi pi-pencil"
                :aria-label="$t('common.edit')"
                class="p-button-rounded p-button-text"
                @click="personelDialogAc(data)"
              />
              <Button
                icon="pi pi-calendar-plus"
                class="p-button-rounded p-button-text p-button-info"
                :title="t('personel.izinEkle')"
                @click="izinDialogAc(data)"
              />
              <Button
                icon="pi pi-trash"
                :aria-label="$t('common.delete')"
                class="p-button-rounded p-button-text p-button-danger"
                @click="personelSil(data)"
              />
            </template>
          </Column>
          <template #empty>
            <EmptyState
              v-if="!yukleniyor && personeller.length === 0"
              :message="t('personel.empty')"
              :sub-message="t('personel.emptyHint')"
              icon="pi pi-users"
              :action-label="t('personel.yeniPersonel')"
              action-icon="pi pi-plus"
              @action="personelDialogAc()"
            />
          </template>
        </AppDataTable>
      </TabPanel>

      <TabPanel :header="t('personel.izinTalepleri')">
        <AppDataTable
          :value="tumIzinler"
          striped-rows
          :paginator="false"
        >
          <Column
            field="personelAdi"
            :header="t('personel.personel')"
          />
          <Column
            field="izinTuru"
            :header="t('personel.izinTuru')"
          >
            <template #body="{ data }">
              {{ izinTuruEtiket(data.izinTuru) }}
            </template>
          </Column>
          <Column
            field="baslangic"
            :header="t('personel.baslangic')"
          />
          <Column
            field="bitis"
            :header="t('personel.bitis')"
          />
          <Column
            field="gunSayisi"
            :header="t('personel.gun')"
          />
          <Column
            field="durum"
            :header="t('common.status')"
          >
            <template #body="{ data }">
              <Tag
                :value="durumEtiket(data.durum)"
                :severity="data.durum === 'ONAYLANDI' ? 'success' : data.durum === 'REDDEDILDI' ? 'danger' : 'warn'"
              />
            </template>
          </Column>
        </AppDataTable>
      </TabPanel>
    </TabView>

    <Dialog
      v-model:visible="personelDialog"
      :header="duzenleme ? t('personel.personelDuzenle') : t('personel.yeniPersonel')"
      modal
      :style="{ width: '600px' }"
    >
      <div class="form-grid">
        <div class="field-row">
          <div class="field">
            <label>{{ t('personel.adZorunlu') }}</label><InputText
              v-model="personelForm.ad"
              class="w-full"
            />
          </div>
          <div class="field">
            <label>{{ t('personel.soyadZorunlu') }}</label><InputText
              v-model="personelForm.soyad"
              class="w-full"
            />
          </div>
        </div>
        <div class="field-row">
          <div class="field">
            <label>{{ t('personel.tcKimlik') }}</label><InputText
              v-model="personelForm.tcKimlik"
              class="w-full"
            />
            <span
              v-if="(personelForm.tcKimlik || '').replace(/\D/g, '').length === 11"
              :style="{ color: tcGecerli ? '#22c55e' : '#ef4444', fontSize: '12px' }"
            >
              {{ tcGecerli ? t('personel.gecerliTC') : t('personel.gecersizTC') }}
            </span>
          </div>
          <div class="field">
            <label>{{ t('personel.dogumTarihi') }}</label><DatePicker
              v-model="personelForm.dogumTarihi"
              date-format="dd.mm.yy"
              class="w-full"
            />
          </div>
        </div>
        <div class="field-row">
          <div class="field">
            <label>{{ t('personel.departman') }}</label><InputText
              v-model="personelForm.departman"
              class="w-full"
            />
          </div>
          <div class="field">
            <label>{{ t('personel.pozisyon') }}</label><InputText
              v-model="personelForm.pozisyon"
              class="w-full"
            />
          </div>
        </div>
        <div class="field-row">
          <div class="field">
            <label>{{ t('personel.rol') }}</label>
            <Dropdown
              v-model="personelForm.rol"
              :options="rolSecenekleri"
              option-label="label"
              option-value="value"
              show-clear
              class="w-full"
            />
          </div>
          <div
            v-if="authStore.isAdmin"
            class="field"
          >
            <label>{{ t('personel.kullaniciHesabi') }}</label>
            <Dropdown
              v-model="personelForm.kullaniciId"
              :options="kullanicilar"
              option-label="displayName"
              option-value="id"
              show-clear
              filter
              :placeholder="personelForm.rol === 'SOFOR' ? t('personel.kullaniciSec') : t('personel.kullaniciOpsiyonel')"
              class="w-full"
            />
          </div>
        </div>
        <div class="field-row">
          <div class="field">
            <label>{{ t('personel.maas') }}</label><InputNumber
              v-model="personelForm.maas"
              mode="currency"
              currency="TRY"
              class="w-full"
            />
          </div>
          <div class="field">
            <label>{{ t('personel.iseGiris') }}</label><DatePicker
              v-model="personelForm.iseGirisTarihi"
              date-format="dd.mm.yy"
              class="w-full"
            />
          </div>
        </div>
        <div class="field-row">
          <div class="field">
            <label>{{ t('personel.telefon') }}</label><InputText
              v-model="personelForm.telefon"
              class="w-full"
            />
          </div>
          <div class="field">
            <label>{{ t('personel.eposta') }}</label><InputText
              v-model="personelForm.email"
              class="w-full"
            />
          </div>
        </div>
        <div class="field">
          <label>{{ t('personel.adres') }}</label><Textarea
            v-model="personelForm.adres"
            rows="2"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('personel.aktif') }}</label><InputSwitch v-model="personelForm.aktif" />
        </div>
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          icon="pi pi-times"
          class="p-button-text"
          @click="personelDialog = false"
        />
        <Button
          :label="t('common.save')"
          icon="pi pi-check"
          :loading="kaydediliyor"
          @click="personelKaydet"
        />
      </template>
    </Dialog>

    <Dialog
      v-model:visible="izinDialog"
      :header="t('personel.izinEkle')"
      modal
      :style="{ width: '450px' }"
    >
      <div class="form-grid">
        <div class="field">
          <label>{{ t('personel.personel') }}</label><InputText
            :value="izinPersonelAdi"
            disabled
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('personel.izinTuruZorunlu') }}</label>
          <Dropdown
            v-model="izinForm.izinTuru"
            :options="izinTuruSecenekleri"
            option-label="label"
            option-value="value"
            :placeholder="t('common.select')"
            class="w-full"
          />
        </div>
        <div class="field-row">
          <div class="field">
            <label>{{ t('personel.baslangic') }}</label><DatePicker
              v-model="izinForm.baslangic"
              date-format="dd.mm.yy"
              class="w-full"
            />
          </div>
          <div class="field">
            <label>{{ t('personel.bitis') }}</label><DatePicker
              v-model="izinForm.bitis"
              date-format="dd.mm.yy"
              class="w-full"
            />
          </div>
        </div>
        <div class="field">
          <label>{{ t('common.description') }}</label><Textarea
            v-model="izinForm.aciklama"
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
          @click="izinDialog = false"
        />
        <Button
          :label="t('common.save')"
          icon="pi pi-check"
          :loading="kaydediliyor"
          @click="izinKaydet"
        />
      </template>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { unwrapList } from '../api/utils/unwrap.js'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useAuthStore } from '../stores/authStore.js'
import { useConfirm } from 'primevue/useconfirm'
import { useFormKorumasi } from '../composables/useFormKorumasi.js'
import { personelAPI, personelIzinAPI, excelAPI, kullaniciAPI } from '../api/index.js'
import EmptyState from '../components/EmptyState.vue'
import IlkZiyaretIpuclari from '../components/IlkZiyaretIpuclari.vue'
import { useI18n } from 'vue-i18n'
import { getLocalDateString } from '../utils/format.js'
const toastBildirim = useToastBildirim()
const confirm = useConfirm()
const authStore = useAuthStore()
const { t } = useI18n()

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
const kullanicilar = ref([])
const rolSecenekleri = computed(() => [
  { label: t('personel.rolSofor'), value: 'SOFOR' },
  { label: t('personel.rolDepocu'), value: 'DEPOCU' },
  { label: t('personel.rolDiger'), value: 'DIGER' }
])
const rolEtiket = (rol) => ({ SOFOR: t('personel.rolSofor'), DEPOCU: t('personel.rolDepocu'), DIGER: t('personel.rolDiger') }[rol] || rol)
const { temizle: formTemizle } = useFormKorumasi(personelForm)
const izinForm = ref({ izinTuru: '', baslangic: null, bitis: null, aciklama: '' })
const tcGecerli = computed(() => {
  const val = (personelForm.value.tcKimlik || '').replace(/\D/g, '')
  if (val.length !== 11 || val[0] === '0') return false
  const d = val.split('').map(Number)
  const tek = d[0] + d[2] + d[4] + d[6] + d[8]
  const cift = d[1] + d[3] + d[5] + d[7]
  if ((tek * 7 - cift) % 10 !== d[9]) return false
  if (d.slice(0, 10).reduce((s, x) => s + x, 0) % 10 !== d[10]) return false
  return true
})

const izinTurleri = [
  'YILLIK_IZIN',
  'HASTA_IZNI',
  'MAZERET_IZNI',
  'DOGUM_IZNI',
  'BABALIK_IZNI',
  'EVLILIK_IZNI',
  'UCRETSIZ_IZIN'
]

// Ham enum kodlari yerine yerellestirilmis etiketler.
const izinTuruEtiket = (kod) => ({
  YILLIK_IZIN: t('personel.izinYillik'),
  HASTA_IZNI: t('personel.izinHasta'),
  MAZERET_IZNI: t('personel.izinMazeret'),
  DOGUM_IZNI: t('personel.izinDogum'),
  BABALIK_IZNI: t('personel.izinBabalik'),
  EVLILIK_IZNI: t('personel.izinEvlilik'),
  UCRETSIZ_IZIN: t('personel.izinUcretsiz')
}[kod] || kod)

const durumEtiket = (durum) => ({
  BEKLEMEDE: t('personel.durumBeklemede'),
  ONAYLANDI: t('personel.durumOnaylandi'),
  REDDEDILDI: t('personel.durumReddedildi')
}[durum] || durum)

const izinTuruSecenekleri = computed(() => izinTurleri.map((k) => ({ label: izinTuruEtiket(k), value: k })))

function defaultForm() {
  return {
    ad: '',
    soyad: '',
    tcKimlik: '',
    dogumTarihi: null,
    iseGirisTarihi: new Date(),
    departman: '',
    pozisyon: '',
    rol: '',
    kullaniciId: null,
    maas: null,
    telefon: '',
    email: '',
    adres: '',
    aktif: true
  }
}

onMounted(async () => {
  yukleniyor.value = true
  try {
    const [pR, iR, kR] = await Promise.all([personelAPI.getAll({ size: 500 }), personelIzinAPI.getAll({ size: 500 }), kullaniciAPI.getAll({ size: 500 }).catch(() => ({ data: [] }))])
    personeller.value = unwrapList(pR)
    tumIzinler.value = unwrapList(iR)
    kullanicilar.value = unwrapList(kR)
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('personel.hataYukleme'))
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
  } catch {
    /* silent */
  }
}

const personelDialogAc = (data) => {
  duzenleme.value = !!data
  personelForm.value = data
    ? {
        ...data,
        dogumTarihi: data.dogumTarihi ? new Date(data.dogumTarihi) : null,
        iseGirisTarihi: data.iseGirisTarihi ? new Date(data.iseGirisTarihi) : new Date()
      }
    : defaultForm()
  formTemizle()
  personelDialog.value = true
}

const personelKaydet = async () => {
  kaydediliyor.value = true
  try {
    const p = {
      ...personelForm.value,
      dogumTarihi: getLocalDateString(personelForm.value.dogumTarihi),
      iseGirisTarihi: getLocalDateString(personelForm.value.iseGirisTarihi)
    }
    if (duzenleme.value) await personelAPI.update(personelForm.value.id, p)
    else await personelAPI.create(p)
    formTemizle()
    personelDialog.value = false
    const r2 = await personelAPI.getAll()
    personeller.value = unwrapList(r2)
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('personel.hataKaydet'))
  }
  kaydediliyor.value = false
}

const personelSil = (data) => {
  confirm.require({
    message: t('common.confirmDelete'),
    header: t('common.silmeOnayi'),
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: t('common.evetSil'),
    rejectLabel: t('common.vazgec'),
    accept: async () => {
      try {
        await personelAPI.delete(data.id)
        personeller.value = personeller.value.filter((p) => p.id !== data.id)
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || err?.message || t('personel.hataSil'))
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
    await personelIzinAPI.create({
      personelId: izinPersonelId.value,
      izinTuru: izinForm.value.izinTuru,
      baslangic: getLocalDateString(izinForm.value.baslangic),
      bitis: getLocalDateString(izinForm.value.bitis),
      gunSayisi,
      aciklama: izinForm.value.aciklama
    })
    izinDialog.value = false
    const r3 = await personelIzinAPI.getAll()
    tumIzinler.value = unwrapList(r3)
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('personel.izinHataKaydet'))
  }
  kaydediliyor.value = false
}
</script>

<style scoped>
.personel-sayfasi {
  padding: 0;
}
.sayfa-baslik {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}
.sayfa-baslik h1 {
  margin: 0;
}
.form-grid {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.field-row {
  display: flex;
  gap: 16px;
}
.field-row .field {
  flex: 1;
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
.rol-badge {
  padding: 3px 10px;
  border-radius: 20px;
  font-size: 11px;
  font-weight: 600;
  background: var(--accent-soft-strong);
  color: var(--accent);
}
</style>
