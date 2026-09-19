<template>
  <div class="depolar-container">
    <div class="sayfa-baslik">
      <h1 class="page-title">
        {{ t('depolar.title') }}
      </h1>
      <div class="toolbar-end">
        <Button
          :label="t('depolar.depolarArasiTransfer')"
          icon="pi pi-sync"
          class="p-button-info p-button-outlined"
          @click="transferDialog = true"
        />
        <Button
          :label="t('depolar.transferTalepleri')"
          icon="pi pi-list"
          class="p-button-warning p-button-outlined"
          @click="talepDialogAc"
        />
        <Button
          :label="t('depolar.yeniDepo')"
          icon="pi pi-plus"
          @click="dialogAc()"
        />
      </div>
    </div>

    <TabView>
      <TabPanel :header="t('depolar.depolar')">
        <DataTable
          :value="list"
          striped-rows
          :loading="yukleniyor"
        >
          <Column
            field="ad"
            :header="t('depolar.depoAdi')"
            sortable
          />
          <Column
            field="subeAdi"
            :header="t('depolar.bagliSube')"
          />
          <Column
            field="yetkili"
            :header="t('depolar.sorumlu')"
          />
          <Column
            field="aktif"
            :header="t('common.status')"
          >
            <template #body="{ data }">
              <Tag
                :value="data.aktif ? t('status.active') : t('status.passive')"
                :severity="data.aktif ? 'success' : 'danger'"
              />
            </template>
          </Column>
          <Column
            :header="t('common.actions')"
            style="width: 160px"
          >
            <template #body="{ data }">
              <Button
                icon="pi pi-box"
                class="p-button-rounded p-button-text"
                :title="t('depolar.stoklariGor')"
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
          :message="t('depolar.empty')"
          :sub-message="t('depolar.emptyHint')"
          icon="pi pi-warehouse"
          :action-label="t('depolar.yeniDepo')"
          action-icon="pi pi-plus"
          @action="dialogAc()"
        />
      </TabPanel>

      <TabPanel
        :header="t('depolar.depoStoklari')"
        :disabled="!seciliDepo"
      >
        <div
          v-if="seciliDepo"
          class="stok-islemleri"
        >
          <div class="depo-basligi">
            <i class="pi pi-warehouse" />
            <span>{{ seciliDepo.ad }}</span>
            <Tag
              v-if="seciliDepo.subeAdi"
              :value="seciliDepo.subeAdi"
              severity="secondary"
            />
          </div>
          <div class="stok-ekle-form">
            <h3>{{ t('depolar.stokEkleCikar') }}</h3>
            <div class="form-row">
              <Dropdown
                v-model="stokForm.stokId"
                :options="stokListesi"
                option-label="ad"
                option-value="id"
                :placeholder="t('depolar.urunSec')"
                class="w-full"
                filter
              />
              <InputNumber
                v-model="stokForm.miktar"
                :placeholder="t('depolar.miktar')"
                :min="0"
              />
              <Button
                :label="t('depolar.ekle')"
                icon="pi pi-plus"
                class="p-button-success"
                :loading="stokLoading"
                @click="stokEkle"
              />
              <Button
                :label="t('depolar.cikar')"
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
              :header="t('depolar.stokKodu')"
            />
            <Column
              field="stokAd"
              :header="t('depolar.urunAdi')"
            />
            <Column
              field="birim"
              :header="t('depolar.birim')"
            />
            <Column
              field="miktar"
              :header="t('depolar.miktar')"
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
          <label>{{ t('depolar.depoAdiZorunlu') }}</label><InputText
            v-model="form.ad"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('depolar.bagliSubeZorunlu') }}</label><Dropdown
            v-model="form.subeId"
            :options="subeListesi"
            option-label="ad"
            option-value="id"
            :placeholder="t('depolar.subeSec')"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('depolar.sorumlu') }}</label><InputText
            v-model="form.yetkili"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('depolar.adres') }}</label><Textarea
            v-model="form.adres"
            rows="2"
            class="w-full"
          />
        </div>
        <div
          v-if="duzenleme"
          class="field"
        >
          <label>{{ t('status.active') }}</label><InputSwitch v-model="form.aktif" />
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

    <Dialog
      v-model:visible="transferDialog"
      :header="t('depolar.depolarArasiTransfer')"
      modal
      :style="{ width: '500px' }"
    >
      <div class="form-grid">
        <div class="field">
          <label>{{ t('depolar.kaynakDepoZorunlu') }}</label><Dropdown
            v-model="transferForm.kaynakDepoId"
            :options="list"
            option-label="ad"
            option-value="id"
            :placeholder="t('depolar.kaynakDepo')"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('depolar.hedefDepoZorunlu') }}</label><Dropdown
            v-model="transferForm.hedefDepoId"
            :options="list"
            option-label="ad"
            option-value="id"
            :placeholder="t('depolar.hedefDepo')"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('depolar.urunZorunlu') }}</label><Dropdown
            v-model="transferForm.stokId"
            :options="stokListesi"
            option-label="ad"
            option-value="id"
            :placeholder="t('depolar.urunSec')"
            class="w-full"
            filter
          />
        </div>
        <div class="field">
          <label>{{ t('depolar.miktarZorunlu') }}</label><InputNumber
            v-model="transferForm.miktar"
            :min="0"
            class="w-full"
          />
        </div>
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          icon="pi pi-times"
          class="p-button-text"
          @click="transferDialog = false"
        />
        <Button
          :label="t('depolar.transferEt')"
          icon="pi pi-send"
          :loading="transferLoading"
          @click="transferYap"
        />
      </template>
    </Dialog>

    <Dialog
      v-model:visible="talepDialog"
      :header="t('depolar.transferTalepleri')"
      :modal="true"
      style="width: 720px"
    >
      <DataTable
        :value="transferTalepleri"
        striped-rows
        size="small"
        :loading="talepYukleniyor"
      >
        <Column :header="t('depolar.kaynakDepo')">
          <template #body="{ data }">
            {{ data.kaynakDepoAd || data.kaynakDepoId }}
          </template>
        </Column>
        <Column :header="t('depolar.hedefDepo')">
          <template #body="{ data }">
            {{ data.hedefDepoAd || data.hedefDepoId }}
          </template>
        </Column>
        <Column :header="t('depolar.stok')">
          <template #body="{ data }">
            {{ data.stokAd || data.stokId }}
          </template>
        </Column>
        <Column :header="t('depolar.miktar')">
          <template #body="{ data }">
            {{ data.miktar }}
          </template>
        </Column>
        <Column :header="t('common.status')">
          <template #body="{ data }">
            <Tag
              :value="data.durum"
              :severity="data.durum === 'BEKLIYOR' ? 'warning' : data.durum === 'ONAYLANDI' ? 'success' : 'danger'"
            />
          </template>
        </Column>
        <Column
          header=""
          style="width: 100px"
        >
          <template #body="{ data }">
            <template v-if="data.durum === 'BEKLIYOR'">
              <Button
                icon="pi pi-check"
                class="p-button-rounded p-button-success p-button-sm"
                style="margin-right: 6px"
                @click="talepOnayla(data)"
              />
              <Button
                icon="pi pi-times"
                class="p-button-rounded p-button-danger p-button-sm"
                @click="talepReddet(data)"
              />
            </template>
          </template>
        </Column>
      </DataTable>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { unwrapList } from '../api/utils/unwrap.js'
import { useToast } from 'primevue/usetoast'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useConfirm } from 'primevue/useconfirm'
import { depoAPI, subeAPI, stokAPI, depoTransferAPI } from '../api/index.js'
import EmptyState from '../components/EmptyState.vue'

const { t } = useI18n()
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

const dialogHeader = computed(() => (duzenleme.value ? t('depolar.depoDuzenle') : t('depolar.yeniDepo')))

const formatCurrency = (v) => {
  if (v === null || v === undefined) return '0,00'
  return new Intl.NumberFormat('tr-TR', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(v)
}

onMounted(async () => {
  yukleniyor.value = true
  try {
    const [depoRes, subeRes, stokRes] = await Promise.all([depoAPI.getAll(), subeAPI.getAll(), stokAPI.getAll()])
    list.value = unwrapList(depoRes)
    subeListesi.value = unwrapList(subeRes)
    stokListesi.value = unwrapList(stokRes)
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('depolar.hataVeri'))
  }
  yukleniyor.value = false
})

const dialogAc = (data) => {
  duzenleme.value = !!data
  form.value = data ? { ...data } : { ad: '', subeId: null, yetkili: '', adres: '', aktif: true }
  dialog.value = true
}

const kaydet = async () => {
  if (!form.value.ad || !form.value.ad.trim()) {
    toastBildirim.uyari(t('depolar.adZorunlu'))
    return
  }
  if (!form.value.subeId) {
    toastBildirim.uyari(t('depolar.subeZorunlu'))
    return
  }
  kaydediliyor.value = true
  try {
    if (duzenleme.value) {
      await depoAPI.update(form.value.id, form.value)
      toastBildirim.basarili(t('depolar.guncellendi'))
    } else {
      await depoAPI.create(form.value)
      toastBildirim.basarili(t('depolar.olusturuldu'))
    }
    dialog.value = false
    const r = await depoAPI.getAll()
    list.value = unwrapList(r)
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('depolar.islemBasarisiz'))
  }
  kaydediliyor.value = false
}

const sil = (data) => {
  confirm.require({
    message: t('depolar.silOnayMesaj', { ad: data.ad }),
    header: t('depolar.silmeOnayi'),
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: t('depolar.evetSil'),
    rejectLabel: t('common.cancel'),
    accept: async () => {
      try {
        await depoAPI.delete(data.id)
        list.value = list.value.filter((x) => x.id !== data.id)
        toast.add({ severity: 'success', summary: t('depolar.silindi'), detail: t('depolar.depoSilindi'), life: 3000 })
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || t('depolar.silmeBasarisiz'))
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
    toastBildirim.hata(err?.response?.data?.message || t('depolar.stoklarYuklenemedi'))
  }
}

const stokEkle = async () => {
  if (!stokForm.value.stokId || !stokForm.value.miktar) return
  stokLoading.value = true
  try {
    await depoAPI.stokEkle(seciliDepo.value.id, { stokId: stokForm.value.stokId, miktar: stokForm.value.miktar })
    const r = await depoAPI.getStoklar(seciliDepo.value.id)
    depoStoklari.value = r.data
    toastBildirim.basarili(t('depolar.stokEklendi'))
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('depolar.stokEklemeBasarisiz'))
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
    toastBildirim.basarili(t('depolar.stokCikarildi'))
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('depolar.stokCikarmaBasarisiz'))
  }
  stokLoading.value = false
}

const transferYap = async () => {
  if (
    !transferForm.value.kaynakDepoId ||
    !transferForm.value.hedefDepoId ||
    !transferForm.value.stokId ||
    !transferForm.value.miktar
  )
    return
  transferLoading.value = true
  try {
    await depoAPI.transfer(transferForm.value)
    transferDialog.value = false
    toastBildirim.basarili(t('depolar.transferTamamlandi'))
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('depolar.transferBasarisiz'))
  }
  transferLoading.value = false
}

const talepDialog = ref(false)
const transferTalepleri = ref([])
const talepYukleniyor = ref(false)

const talepDialogAc = async () => {
  talepDialog.value = true
  await talepYukle()
}

const talepYukle = async () => {
  talepYukleniyor.value = true
  try {
    const r = await depoTransferAPI.getAll()
    transferTalepleri.value = r.data || []
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('depolar.taleplerYuklenemedi'))
  } finally {
    talepYukleniyor.value = false
  }
}

const talepOnayla = async (t) => {
  try {
    await depoTransferAPI.onayla(t.id)
    toastBildirim.basarili(t('depolar.transferOnaylandi'))
    talepYukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('depolar.onaylamaBasarisiz'))
  }
}

const talepReddet = async (t) => {
  try {
    await depoTransferAPI.reddet(t.id)
    toastBildirim.basarili(t('depolar.transferReddedildi'))
    talepYukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('depolar.reddetmeBasarisiz'))
  }
}
</script>

<style scoped>
.depolar-container {
  padding: 0;
}
.depolar-container :deep(.p-tabview-tablist) {
  overflow-x: visible;
  scrollbar-width: none;
}
.depolar-container :deep(.p-tabview-tablist::-webkit-scrollbar) {
  display: none;
}
.depolar-container :deep(.p-tabview-nav-link) {
  white-space: nowrap;
}
.sayfa-baslik {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  flex-wrap: wrap;
  gap: 12px;
}
.toolbar-end {
  display: flex;
  gap: 8px;
}
.form-grid {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.form-row {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
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
.stok-islemleri {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.depo-basligi {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
}
.depo-basligi i {
  color: var(--accent);
}
.stok-ekle-form {
  background: var(--bg-secondary);
  padding: 16px;
  border-radius: 8px;
}
.stok-ekle-form h3 {
  margin: 0 0 12px;
  font-size: 15px;
}
.text-danger {
  color: #f87171;
}
</style>
