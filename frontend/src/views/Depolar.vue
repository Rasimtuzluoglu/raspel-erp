<template>
  <div class="depolar-container">
    <div class="sayfa-baslik">
      <h1 class="page-title">
        Depolar & Stok Yönetimi
      </h1>
      <div class="toolbar-end">
        <Button
          label="Depolar Arası Transfer"
          icon="pi pi-exchange"
          class="p-button-info p-button-outlined"
          @click="transferDialog = true"
        />
        <Button
          label="Yeni Depo"
          icon="pi pi-plus"
          @click="dialogAc()"
        />
      </div>
    </div>

    <TabView>
      <TabPanel header="Depolar">
        <DataTable
          :value="list"
          striped-rows
          :loading="yukleniyor"
        >
          <Column
            field="ad"
            header="Depo Adı"
            sortable
          />
          <Column
            field="subeAdi"
            header="Bağlı Şube"
          />
          <Column
            field="yetkili"
            header="Sorumlu"
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
            style="width:160px"
          >
            <template #body="{ data }">
              <Button
                icon="pi pi-box"
                class="p-button-rounded p-button-text"
                title="Stokları Gör"
                @click="stokGoruntule(data)"
              />
              <Button
                icon="pi pi-pencil"
                class="p-button-rounded p-button-text"
                @click="dialogAc(data)"
              />
              <Button
                icon="pi pi-trash"
                class="p-button-rounded p-button-text"
                @click="sil(data)"
              />
            </template>
          </Column>
        </DataTable>
        <EmptyState
          v-if="!yukleniyor && list.length === 0"
          message="Henüz depo bulunamadı"
          sub-message="İlk deponuzu eklemek için Yeni Depo butonuna tıklayın"
          icon="pi pi-warehouse"
          action-label="Yeni Depo"
          action-icon="pi pi-plus"
          @action="dialogAc()"
        />
      </TabPanel>

      <TabPanel
        :header="seciliDepo ? seciliDepo.ad + ' - Stoklar' : 'Depo Stokları'"
        :disabled="!seciliDepo"
      >
        <div
          v-if="seciliDepo"
          class="stok-islemleri"
        >
          <div class="stok-ekle-form">
            <h3>Stok Ekle/Çıkar</h3>
            <div class="form-row">
              <Dropdown
                v-model="stokForm.stokId"
                :options="stokListesi"
                option-label="ad"
                option-value="id"
                placeholder="Ürün Seç"
                class="w-full"
                filter
              />
              <InputNumber
                v-model="stokForm.miktar"
                placeholder="Miktar"
                :min="0"
              />
              <Button
                label="Ekle"
                icon="pi pi-plus"
                class="p-button-success"
                :loading="stokLoading"
                @click="stokEkle"
              />
              <Button
                label="Çıkar"
                icon="pi pi-minus"
                class="p-button-warning"
                :loading="stokLoading"
                @click="stokCikar"
              />
            </div>
          </div>
          <DataTable
            :value="depoStoklari"
            striped-rows
            size="small"
          >
            <Column
              field="stokKodu"
              header="Stok Kodu"
            />
            <Column
              field="stokAd"
              header="Ürün Adı"
            />
            <Column
              field="birim"
              header="Birim"
            />
            <Column
              field="miktar"
              header="Miktar"
              sortable
            >
              <template #body="{ data }">
                <span :class="{ 'text-danger': data.miktar <= 0 }">{{ formatCurrency(data.miktar) }}</span>
              </template>
            </Column>
          </DataTable>
        </div>
      </TabPanel>
    </TabView>

    <Dialog
      v-model:visible="dialog"
      :header="dialogHeader"
      modal
      :style="{ width: '500px' }"
    >
      <div class="form-grid">
        <div class="field">
          <label>Depo Adı *</label><InputText
            v-model="form.ad"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>Bağlı Şube *</label><Dropdown
            v-model="form.subeId"
            :options="subeListesi"
            option-label="ad"
            option-value="id"
            placeholder="Şube Seç"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>Sorumlu</label><InputText
            v-model="form.yetkili"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>Adres</label><Textarea
            v-model="form.adres"
            rows="2"
            class="w-full"
          />
        </div>
        <div
          v-if="duzenleme"
          class="field"
        >
          <label>Aktif</label><InputSwitch v-model="form.aktif" />
        </div>
      </div>
      <template #footer>
        <Button
          label="İptal"
          icon="pi pi-times"
          class="p-button-text"
          @click="dialog = false"
        />
        <Button
          label="Kaydet"
          icon="pi pi-check"
          :loading="kaydediliyor"
          @click="kaydet"
        />
      </template>
    </Dialog>

    <Dialog
      v-model:visible="transferDialog"
      header="Depolar Arası Transfer"
      modal
      :style="{ width: '500px' }"
    >
      <div class="form-grid">
        <div class="field">
          <label>Kaynak Depo *</label><Dropdown
            v-model="transferForm.kaynakDepoId"
            :options="list"
            option-label="ad"
            option-value="id"
            placeholder="Kaynak Depo"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>Hedef Depo *</label><Dropdown
            v-model="transferForm.hedefDepoId"
            :options="list"
            option-label="ad"
            option-value="id"
            placeholder="Hedef Depo"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>Ürün *</label><Dropdown
            v-model="transferForm.stokId"
            :options="stokListesi"
            option-label="ad"
            option-value="id"
            placeholder="Ürün Seç"
            class="w-full"
            filter
          />
        </div>
        <div class="field">
          <label>Miktar *</label><InputNumber
            v-model="transferForm.miktar"
            :min="0"
            class="w-full"
          />
        </div>
      </div>
      <template #footer>
        <Button
          label="İptal"
          icon="pi pi-times"
          class="p-button-text"
          @click="transferDialog = false"
        />
        <Button
          label="Transfer Et"
          icon="pi pi-send"
          :loading="transferLoading"
          @click="transferYap"
        />
      </template>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useToast } from 'primevue/usetoast'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useConfirm } from 'primevue/useconfirm'
import { depoAPI, subeAPI, stokAPI } from '../api/index.js'
import EmptyState from '../components/EmptyState.vue'

const toast = useToast()
const toastBildirim = useToastBildirim()
const confirm = useConfirm()
const list = ref([])
const subeListesi = ref([])
const stokListesi = ref([])
const depoStoklari = ref([])
const seciliDepo = ref(null)
const yukleniyor = ref(false)
const kaydediliyor = ref(false)
const stokLoading = ref(false)
const transferLoading = ref(false)
const dialog = ref(false)
const duzenleme = ref(false)
const transferDialog = ref(false)
const form = ref({ ad: '', subeId: null, yetkili: '', adres: '', aktif: true })
const stokForm = ref({ stokId: null, miktar: 0 })
const transferForm = ref({ kaynakDepoId: null, hedefDepoId: null, stokId: null, miktar: 0 })

const dialogHeader = computed(() => duzenleme.value ? 'Depo Düzenle' : 'Yeni Depo')

const formatCurrency = (v) => {
  if (v === null || v === undefined) return '0,00'
  return new Intl.NumberFormat('tr-TR', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(v)
}

onMounted(async () => {
  yukleniyor.value = true
  try {
    const [depoRes, subeRes, stokRes] = await Promise.all([
      depoAPI.getAll(), subeAPI.getAll(), stokAPI.getAll()
    ])
    list.value = depoRes.data?.content || depoRes.data || []
    subeListesi.value = subeRes.data
    stokListesi.value = stokRes.data.content || stokRes.data
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || 'Veriler yüklenemedi')
  }
  yukleniyor.value = false
})

const dialogAc = (data) => {
  duzenleme.value = !!data
  form.value = data ? { ...data } : { ad: '', subeId: null, yetkili: '', adres: '', aktif: true }
  dialog.value = true
}

const kaydet = async () => {
  kaydediliyor.value = true
  try {
    if (duzenleme.value) {
      await depoAPI.update(form.value.id, form.value)
      toastBildirim.basarili('Depo güncellendi')
    } else {
      await depoAPI.create(form.value)
      toastBildirim.basarili('Depo oluşturuldu')
    }
    dialog.value = false
    const r = await depoAPI.getAll(); list.value = r.data?.content || r.data || []
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || 'İşlem başarısız')
  }
  kaydediliyor.value = false
}

const sil = (data) => {
  confirm.require({
    message: `${data.ad} deposunu silmek istediğinize emin misiniz?`,
    header: 'Silme Onayı',
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: 'Evet, Sil',
    rejectLabel: 'İptal',
    accept: async () => {
      try {
        await depoAPI.delete(data.id)
        list.value = list.value.filter(x => x.id !== data.id)
        toast.add({ severity: 'success', summary: 'Silindi', detail: 'Depo silindi', life: 3000 })
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || 'Silme başarısız')
      }
    }
  })
}

const stokGoruntule = async (depo) => {
  seciliDepo.value = depo
  try {
    const r = await depoAPI.getStoklar(depo.id)
    depoStoklari.value = r.data
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || 'Stoklar yüklenemedi')
  }
}

const stokEkle = async () => {
  if (!stokForm.value.stokId || !stokForm.value.miktar) return
  stokLoading.value = true
  try {
    await depoAPI.stokEkle(seciliDepo.value.id, { stokId: stokForm.value.stokId, miktar: stokForm.value.miktar })
    const r = await depoAPI.getStoklar(seciliDepo.value.id)
    depoStoklari.value = r.data
    toastBildirim.basarili('Stok eklendi')
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || 'Stok ekleme başarısız')
  }
  stokLoading.value = false
}

const stokCikar = async () => {
  if (!stokForm.value.stokId || !stokForm.value.miktar) return
  stokLoading.value = true
  try {
    await depoAPI.stokCikar(seciliDepo.value.id, { stokId: stokForm.value.stokId, miktar: stokForm.value.miktar })
    const r = await depoAPI.getStoklar(seciliDepo.value.id)
    depoStoklari.value = r.data
    toastBildirim.basarili('Stok çıkarıldı')
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || 'Stok çıkarma başarısız')
  }
  stokLoading.value = false
}

const transferYap = async () => {
  if (!transferForm.value.kaynakDepoId || !transferForm.value.hedefDepoId || !transferForm.value.stokId || !transferForm.value.miktar) return
  transferLoading.value = true
  try {
    await depoAPI.transfer(transferForm.value)
    transferDialog.value = false
    toastBildirim.basarili('Transfer tamamlandı')
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || 'Transfer başarısız')
  }
  transferLoading.value = false
}
</script>

<style scoped>
.depolar-container { padding: 0; }
.sayfa-baslik { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; flex-wrap: wrap; gap: 12px; }
.toolbar-end { display: flex; gap: 8px; }
.form-grid { display: flex; flex-direction: column; gap: 16px; }
.form-row { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
.field { display: flex; flex-direction: column; gap: 6px; }
.field label { font-size: 13px; font-weight: 600; color: var(--text-secondary); }
.w-full { width: 100%; }
.stok-islemleri { display: flex; flex-direction: column; gap: 16px; }
.stok-ekle-form { background: var(--bg-secondary); padding: 16px; border-radius: 8px; }
.stok-ekle-form h3 { margin: 0 0 12px; font-size: 15px; }
.text-danger { color: #f87171; }
</style>