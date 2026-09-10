<template>
  <div class="sirketler-sayfasi">
    <div class="sayfa-baslik">
      <h1 class="page-title">
        {{ t('sirketler.title') }}
      </h1>
      <Button
        :label="t('sirketler.yeniSirket')"
        icon="pi pi-plus"
        @click="dialogAc"
      />
    </div>

    <DataTable
      :value="sirketler"
      striped-rows
      responsive-layout="scroll"
      :loading="yukleniyor"
    >
      <Column
        field="id"
        header="#"
        style="width: 60px"
      />
      <Column
        field="ad"
        :header="t('sirketler.sirketAdi')"
        sortable
      />
      <Column
        field="vergiNo"
        :header="t('sirketler.vergiNo')"
      />
      <Column
        field="vergiDairesi"
        :header="t('sirketler.vergiDairesi')"
      />
      <Column
        field="telefon"
        :header="t('sirketler.telefon')"
      />
      <Column
        field="email"
        :header="t('sirketler.eposta')"
      />
      <Column
        field="tur"
        :header="t('sirketler.tur')"
      >
        <template #body="{ data }">
          <Tag
            :value="data.tur"
            :severity="data.tur === 'RESMI' ? 'success' : data.tur === 'GAYRIRESMI' ? 'warning' : 'info'"
          />
        </template>
      </Column>
      <Column
        field="yil"
        :header="t('sirketler.yil')"
      />
      <Column
        field="aktif"
        :header="t('common.status')"
      >
        <template #body="{ data }">
          <Tag
            :value="data.aktif ? t('sirketler.aktif') : t('sirketler.pasif')"
            :severity="data.aktif ? 'success' : 'danger'"
          />
        </template>
      </Column>
      <Column
        :header="t('sirketler.islem')"
        style="width: 120px"
      >
        <template #body="{ data }">
          <Button
            icon="pi pi-sitemap"
            class="p-button-rounded p-button-text p-button-info"
            :title="t('sirketler.grupKonsolideOzeti')"
            @click="konsolideGoster(data)"
          />
          <Button
            icon="pi pi-pencil"
            class="p-button-rounded p-button-text"
            :title="t('common.edit')"
            @click="dialogAc(data)"
          />
          <Button
            icon="pi pi-trash"
            class="p-button-rounded p-button-text p-button-danger"
            :title="t('common.delete')"
            @click="sil(data)"
          />
        </template>
      </Column>
    </DataTable>

    <Dialog
      v-model:visible="dialog"
      :header="duzenleme ? t('sirketler.sirketDuzenle') : t('sirketler.yeniSirket')"
      modal
      :style="{ width: '500px' }"
    >
      <Message
        v-if="uyariMesaji"
        severity="warn"
        :closable="false"
      >
        <i
          class="pi pi-exclamation-triangle"
          style="margin-right: 8px"
        />{{ uyariMesaji }}
      </Message>
      <div class="form-grid">
        <div class="field">
          <label>{{ t('sirketler.sirketAdiZorunlu') }}</label>
          <InputText
            v-model="form.ad"
            class="w-full"
          />
        </div>
        
        <div
          class="field-row"
          style="display: flex; gap: 16px;"
        >
          <div
            class="field"
            style="flex: 1;"
          >
            <label>{{ t('sirketler.sirketTuru') }}</label>
            <Dropdown
              v-model="form.tur"
              :options="[
                { label: t('sirketler.diger'), value: 'DIGER' },
                { label: t('sirketler.resmi'), value: 'RESMI' },
                { label: t('sirketler.gayriresmi'), value: 'GAYRIRESMI' }
              ]"
              option-label="label"
              option-value="value"
              class="w-full"
            />
          </div>
          <div
            class="field"
            style="flex: 1;"
          >
            <label>{{ t('sirketler.maliYil') }}</label>
            <InputNumber
              v-model="form.yil"
              :use-grouping="false"
              class="w-full"
            />
          </div>
        </div>

        <div class="field">
          <label>{{ t('sirketler.anaSirket') }}</label>
          <Dropdown
            v-model="form.parentId"
            :options="sirketler.filter(s => s.id !== form.id)"
            option-label="ad"
            option-value="id"
            show-clear
            :placeholder="t('sirketler.anaSirketSecin')"
            class="w-full"
          />
        </div>

        <div class="field">
          <label>{{ t('sirketler.vergiNo') }}</label>
          <InputText
            v-model="form.vergiNo"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('sirketler.vergiDairesi') }}</label>
          <InputText
            v-model="form.vergiDairesi"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('sirketler.telefon') }}</label>
          <InputText
            v-model="form.telefon"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('sirketler.eposta') }}</label>
          <InputText
            v-model="form.email"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('sirketler.webSitesi') }}</label>
          <InputText
            v-model="form.webSite"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('sirketler.logo') }}</label>
          <div class="logo-upload-row">
            <InputText
              v-model="form.logoUrl"
              class="w-full"
              :placeholder="t('sirketler.logoUrlPlaceholder')"
            />
            <Button
              icon="pi pi-upload"
              class="p-button-outlined"
              :loading="logoYukleniyor"
              @click="$refs.logoInput.click()"
            />
            <input
              ref="logoInput"
              type="file"
              accept="image/*"
              style="display: none"
              @change="logoSec"
            >
          </div>
          <img
            v-if="form.logoUrl"
            :src="form.logoUrl"
            class="logo-preview"
            :alt="t('sirketler.logoOnizleme')"
          >
        </div>
        <div class="field">
          <label>{{ t('sirketler.adres') }}</label>
          <Textarea
            v-model="form.adres"
            rows="3"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('sirketler.aktif') }}</label>
          <InputSwitch v-model="form.aktif" />
        </div>
        <div class="field">
          <label>{{ t('sirketler.negatifStokIzni') }}</label>
          <InputSwitch v-model="form.negatifStokIzni" />
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

    <!-- Grup Konsolidasyon Modal -->
    <Dialog
      v-model:visible="konsolideModal"
      :header="konsolideVeri?.anaSirketAdi + ' - ' + t('sirketler.grupKonsolideOzeti')"
      modal
      :style="{ width: '700px' }"
    >
      <div
        v-if="konsolideYukleniyor"
        class="text-center py-6"
      >
        <i class="pi pi-spin pi-spinner text-3xl text-primary" />
      </div>
      <div
        v-else-if="konsolideVeri"
        class="space-y-4"
      >
        <div class="grid grid-cols-3 gap-3">
          <div class="p-3 bg-blue-50 dark:bg-blue-950/40 rounded-lg border border-blue-100">
            <span class="text-xs text-accent font-bold block">{{ t('sirketler.toplamStokDegeri') }}</span>
            <span class="text-lg font-extrabold text-accent dark:text-blue-200">{{ formatPara(konsolideVeri.toplamStokDegeri) }}</span>
          </div>
          <div class="p-3 bg-emerald-50 dark:bg-emerald-950/40 rounded-lg border border-emerald-100">
            <span class="text-xs text-emerald-600 font-bold block">{{ t('sirketler.toplamAlacak') }}</span>
            <span class="text-lg font-extrabold text-emerald-800 dark:text-emerald-200">{{ formatPara(konsolideVeri.toplamAlacakBakiye) }}</span>
          </div>
          <div class="p-3 bg-amber-50 dark:bg-amber-950/40 rounded-lg border border-amber-100">
            <span class="text-xs text-amber-600 font-bold block">{{ t('sirketler.toplamBorc') }}</span>
            <span class="text-lg font-extrabold text-amber-800 dark:text-amber-200">{{ formatPara(konsolideVeri.toplamBorcBakiye) }}</span>
          </div>
        </div>

        <h4 class="font-bold text-sm text-secondary dark:text-gray-200 mt-3">
          {{ t('sirketler.grupAltSirketler') }} ({{ konsolideVeri.sirketler?.length || 0 }})
        </h4>
        <DataTable
          :value="konsolideVeri.sirketler"
          size="small"
          striped-rows
        >
          <Column
            field="sirketAdi"
            :header="t('sirketler.sirket')"
          />
          <Column
            field="tur"
            :header="t('sirketler.tur')"
            style="width: 100px"
          />
          <Column
            field="yil"
            :header="t('sirketler.yil')"
            style="width: 80px"
          />
          <Column
            field="stokSayisi"
            :header="t('sirketler.stokCesidi')"
            style="width: 100px"
          />
          <Column :header="t('sirketler.stokDegeri')">
            <template #body="{ data }">
              {{ formatPara(data.stokDegeri) }}
            </template>
          </Column>
          <Column :header="t('sirketler.cariBakiye')">
            <template #body="{ data }">
              <span :class="data.bakiye >= 0 ? 'text-emerald-600' : 'text-red-600'">
                {{ formatPara(data.bakiye) }}
              </span>
            </template>
          </Column>
        </DataTable>
      </div>
      <template #footer>
        <Button
          :label="t('stoklar.kapat')"
          class="p-button-text"
          @click="konsolideModal = false"
        />
      </template>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useConfirm } from 'primevue/useconfirm'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { sirketAPI, uploadAPI } from '../api/index.js'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
const toastBildirim = useToastBildirim()
const confirm = useConfirm()

const sirketler = ref([])
const yukleniyor = ref(false)
const dialog = ref(false)
const duzenleme = ref(false)
const kaydediliyor = ref(false)
const seciliId = ref(null)
const form = ref({
  ad: '',
  vergiNo: '',
  vergiDairesi: '',
  adres: '',
  telefon: '',
  email: '',
  webSite: '',
  logoUrl: '',
  aktif: true,
  negatifStokIzni: false
})
const uyariMesaji = ref('')
const logoYukleniyor = ref(false)

const konsolideModal = ref(false)
const konsolideYukleniyor = ref(false)
const konsolideVeri = ref(null)

const formatPara = (v) => {
  if (v == null || isNaN(v)) return '0,00 ₺'
  return new Intl.NumberFormat('tr-TR', { style: 'currency', currency: 'TRY' }).format(v)
}

const konsolideGoster = async (sirket) => {
  konsolideYukleniyor.value = true
  konsolideModal.value = true
  try {
    const res = await sirketAPI.getKonsolideOzet(sirket.id)
    konsolideVeri.value = res.data
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('sirketler.konsolideAlinamadi'))
  } finally {
    konsolideYukleniyor.value = false
  }
}

onMounted(async () => {
  yukleniyor.value = true
  try {
    const r = await sirketAPI.getAll()
    sirketler.value = r.data?.content || r.data || []
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('sirketler.yuklemeHatasi'))
  }
  yukleniyor.value = false
})

const eskiAd = ref('')
const dialogAc = (data) => {
  duzenleme.value = !!data
  seciliId.value = data?.id || null
  form.value = data
    ? { ...data }
    : {
        ad: '',
        tur: 'DIGER',
        yil: new Date().getFullYear(),
        parentId: null,
        vergiNo: '',
        vergiDairesi: '',
        adres: '',
        telefon: '',
        email: '',
        webSite: '',
        logoUrl: '',
        aktif: true,
        negatifStokIzni: false
      }
  eskiAd.value = data?.ad || ''
  uyariMesaji.value = ''
  if (data?.sonAdGuncellemeTarihi) {
    const son = new Date(data.sonAdGuncellemeTarihi)
    const simdi = new Date()
    const fark = Math.ceil((son.getTime() + 30 * 24 * 60 * 60 * 1000 - simdi.getTime()) / (24 * 60 * 60 * 1000))
    if (fark > 0) {
      uyariMesaji.value = t('sirketler.adDegistirmeUyari', { fark, tarih: son.toLocaleDateString('tr-TR') })
    }
  }
  dialog.value = true
}

const logoSec = async (event) => {
  const file = event.target.files?.[0]
  if (!file) return
  logoYukleniyor.value = true
  try {
    const res = await uploadAPI.uploadSirketLogo(file)
    form.value.logoUrl = res.data.url
  } catch (err) {
    toastBildirim.hata(t('sirketler.logoYuklenemedi'))
  }
  logoYukleniyor.value = false
}

const kaydet = async () => {
  if (duzenleme.value && form.value.ad !== eskiAd.value && uyariMesaji.value) {
    confirm.require({
      message: `${uyariMesaji.value}\n\n${t('sirketler.degisiklikOnayi')}`,
      header: t('sirketler.otuzGunKurali'),
      icon: 'pi pi-exclamation-triangle',
      accept: () => kaydetAction()
    })
    return
  }
  await kaydetAction()
}

const kaydetAction = async () => {
  kaydediliyor.value = true
  try {
    if (duzenleme.value) {
      await sirketAPI.update(seciliId.value, form.value)
      toastBildirim.basarili(t('sirketler.guncellendi'))
    } else {
      await sirketAPI.create(form.value)
      toastBildirim.basarili(t('sirketler.olusturuldu'))
    }
    dialog.value = false
    const r = await sirketAPI.getAll()
    sirketler.value = r.data?.content || r.data || []
  } catch (err) {
    toastBildirim.hata(err.response?.data?.message || t('sirketler.islemBasarisiz'))
  }
  kaydediliyor.value = false
}

const sil = async (data) => {
  try {
    await sirketAPI.delete(data.id)
    sirketler.value = sirketler.value.filter((s) => s.id !== data.id)
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('sirketler.silmeHatasi'))
  }
}
</script>

<style scoped>
.sirketler-sayfasi {
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
.logo-upload-row {
  display: flex;
  gap: 8px;
  align-items: center;
}
.logo-preview {
  max-width: 120px;
  max-height: 60px;
  margin-top: 8px;
  border-radius: 6px;
  object-fit: contain;
}
</style>
