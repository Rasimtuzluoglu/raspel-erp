<template>
  <div class="subeler-container">
    <div class="sayfa-baslik">
      <h1 class="page-title">
        {{ t('subeler.title') }}
      </h1>
      <Button
        :label="t('subeler.yeniSube')"
        icon="pi pi-plus"
        @click="dialogAc()"
      />
    </div>

    <DataTable
      :value="list"
      striped-rows
      :loading="yukleniyor"
    >
      <Column
        field="ad"
        :header="t('subeler.subeAdi')"
        sortable
      />
      <Column
        field="yetkili"
        :header="t('subeler.yetkili')"
      />
      <Column
        field="telefon"
        :header="t('subeler.telefon')"
      />
      <Column
        field="adres"
        :header="t('subeler.adres')"
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
        style="width: 120px"
      >
        <template #body="{ data }">
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
      :message="t('subeler.empty')"
      :sub-message="t('subeler.emptyHint')"
      icon="pi pi-map-marker"
      :action-label="t('subeler.yeniSube')"
      action-icon="pi pi-plus"
      @action="dialogAc()"
    />

    <Dialog
      v-model:visible="dialog"
      :header="dialogHeader"
      modal
      :style="{ width: '500px' }"
    >
      <div class="form-grid">
        <div class="field">
          <label>{{ t('subeler.subeAdiZorunlu') }}</label><InputText
            v-model="form.ad"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('subeler.yetkili') }}</label><InputText
            v-model="form.yetkili"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('subeler.telefon') }}</label><InputText
            v-model="form.telefon"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('subeler.adres') }}</label><Textarea
            v-model="form.adres"
            rows="3"
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
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { unwrapList } from '../api/utils/unwrap.js'
import { useToast } from 'primevue/usetoast'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useConfirm } from 'primevue/useconfirm'
import { subeAPI } from '../api/index.js'
import EmptyState from '../components/EmptyState.vue'

const { t } = useI18n()
const toast = useToast()
const toastBildirim = useToastBildirim()
const confirm = useConfirm()
const list = ref([])
const yukleniyor = ref(false)
const kaydediliyor = ref(false)
const dialog = ref(false)
const duzenleme = ref(false)
const form = ref({ ad: '', yetkili: '', telefon: '', adres: '', aktif: true })

const dialogHeader = computed(() => (duzenleme.value ? t('subeler.subeDuzenle') : t('subeler.yeniSube')))

onMounted(async () => {
  yukleniyor.value = true
  try {
    const r = await subeAPI.getAll()
    list.value = unwrapList(r)
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('subeler.hataYukleme'))
  }
  yukleniyor.value = false
})

const dialogAc = (data) => {
  duzenleme.value = !!data
  form.value = data ? { ...data } : { ad: '', yetkili: '', telefon: '', adres: '', aktif: true }
  dialog.value = true
}

const kaydet = async () => {
  kaydediliyor.value = true
  try {
    if (duzenleme.value) {
      await subeAPI.update(form.value.id, form.value)
      toastBildirim.basarili(t('subeler.guncellendi'))
    } else {
      await subeAPI.create(form.value)
      toastBildirim.basarili(t('subeler.olusturuldu'))
    }
    dialog.value = false
    const r = await subeAPI.getAll()
    list.value = unwrapList(r)
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('subeler.islemBasarisiz'))
  }
  kaydediliyor.value = false
}

const sil = (data) => {
  confirm.require({
    message: t('subeler.silOnayMesaj', { ad: data.ad }),
    header: t('subeler.silmeOnayi'),
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: t('subeler.evetSil'),
    rejectLabel: t('common.cancel'),
    accept: async () => {
      try {
        await subeAPI.delete(data.id)
        list.value = list.value.filter((x) => x.id !== data.id)
        toast.add({ severity: 'success', summary: t('subeler.silindi'), detail: t('subeler.subeSilindi'), life: 3000 })
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || t('subeler.silmeBasarisiz'))
      }
    }
  })
}
</script>

<style scoped>
.subeler-container {
  padding: 0;
}
.sayfa-baslik {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
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
</style>
