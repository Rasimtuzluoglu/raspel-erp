<template>
  <Toast />
  <div class="proje-sayfasi">
    <div class="sayfa-baslik">
      <h1 class="page-title">
        Projeler & Görevler
      </h1>
      <Button
        label="Yeni Proje"
        icon="pi pi-plus"
        @click="dialogAc()"
      />
    </div>

    <DataTable
      :value="list"
      striped-rows
      :loading="yukleniyor"
      :expanded-row-keys="expanded"
      data-key="id"
      @row-expand="rowExpand"
      @row-collapse="rowCollapse"
    >
      <Column
        :expander="true"
        style="width:40px"
      />
      <Column
        field="ad"
        header="Proje Adı"
        sortable
      />
      <Column
        field="sorumlu"
        header="Sorumlu"
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
        field="durum"
        header="Durum"
      >
        <template #body="{ data }">
          <Tag
            :value="data.durum"
            :severity="data.durum === 'TAMAMLANDI' ? 'success' : data.durum === 'IPTAL' ? 'danger' : 'info'"
          />
        </template>
      </Column>
      <Column
        header="İşlem"
        style="width:120px"
      >
        <template #body="{ data }">
          <Button
            v-if="data.durum === 'DEVAM_EDIYOR'"
            icon="pi pi-check-circle"
            class="p-button-rounded p-button-text p-button-success"
            title="Tamamla"
            @click="durumGuncelle(data, 'TAMAMLANDI')"
          />
          <Button
            icon="pi pi-trash"
            class="p-button-rounded p-button-text"
            @click="sil(data)"
          />
        </template>
      </Column>
      <template #expansion="{ data }">
        <div class="gorevler">
          <div class="gorev-baslik">
            <h3>Görevler</h3>
            <Button
              label="Görev Ekle"
              icon="pi pi-plus"
              size="small"
              @click="gorevDialogAc(data)"
            />
          </div>
          <DataTable
            :value="data.gorevler || []"
            striped-rows
            size="small"
          >
            <Column
              field="ad"
              header="Görev"
            />
            <Column
              field="atanan"
              header="Atanan"
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
              field="durum"
              header="Durum"
            >
              <template #body="{ data: g }">
                <Tag
                  :value="g.durum"
                  :severity="g.durum === 'TAMAMLANDI' ? 'success' : g.durum === 'DEVAM_EDIYOR' ? 'info' : 'warn'"
                />
              </template>
            </Column>
            <Column
              header="İşlem"
              style="width:80px"
            >
              <template #body="{ data: g }">
                <Button
                  v-if="g.durum !== 'TAMAMLANDI'"
                  icon="pi pi-check"
                  class="p-button-rounded p-button-text p-button-success"
                  size="small"
                  title="Tamamla"
                  @click="gorevTamamla(g)"
                />
              </template>
            </Column>
          </DataTable>
        </div>
      </template>
    </DataTable>

    <EmptyState
      v-if="!yukleniyor && list.length === 0"
      message="Henüz proje bulunamadı"
      sub-message="İlk projenizi eklemek için Yeni Proje butonuna tıklayın"
      icon="pi pi-folder"
      action-label="Yeni Proje"
      action-icon="pi pi-plus"
      @action="dialogAc()"
    />

    <Dialog
      v-model:visible="dialog"
      header="Yeni Proje"
      modal
      :style="{ width: '500px' }"
    >
      <div class="form-grid">
        <div class="field">
          <label>Proje Adı *</label><InputText
            v-model="form.ad"
            class="w-full"
          />
        </div>
        <div class="field-row">
          <div class="field">
            <label>Sorumlu</label><InputText
              v-model="form.sorumlu"
              class="w-full"
            />
          </div>
        </div>
        <div class="field-row">
          <div class="field">
            <label>Başlangıç</label><DatePicker
              v-model="form.baslangic"
              date-format="dd/mm/yy"
              class="w-full"
            />
          </div>
          <div class="field">
            <label>Bitiş</label><DatePicker
              v-model="form.bitis"
              date-format="dd/mm/yy"
              class="w-full"
            />
          </div>
        </div>
        <div class="field">
          <label>Açıklama</label><Textarea
            v-model="form.aciklama"
            rows="3"
            class="w-full"
          />
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
      v-model:visible="gorevDialog"
      header="Görev Ekle"
      modal
      :style="{ width: '450px' }"
    >
      <div class="form-grid">
        <div class="field">
          <label>Görev Adı *</label><InputText
            v-model="gorevForm.ad"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>Atanan</label><InputText
            v-model="gorevForm.atanan"
            class="w-full"
          />
        </div>
        <div class="field-row">
          <div class="field">
            <label>Başlangıç</label><DatePicker
              v-model="gorevForm.baslangic"
              date-format="dd/mm/yy"
              class="w-full"
            />
          </div>
          <div class="field">
            <label>Bitiş</label><DatePicker
              v-model="gorevForm.bitis"
              date-format="dd/mm/yy"
              class="w-full"
            />
          </div>
        </div>
        <div class="field">
          <label>Açıklama</label><Textarea
            v-model="gorevForm.aciklama"
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
          @click="gorevDialog = false"
        />
        <Button
          label="Kaydet"
          icon="pi pi-check"
          :loading="kaydediliyor"
          @click="gorevKaydet"
        />
      </template>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useToast } from 'primevue/usetoast'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useConfirm } from 'primevue/useconfirm'
import { projeAPI } from '../api/index.js'
import EmptyState from '../components/EmptyState.vue'

const toast = useToast()
const toastBildirim = useToastBildirim()
const confirm = useConfirm()
const list = ref([])
const yukleniyor = ref(false)
const kaydediliyor = ref(false)
const dialog = ref(false)
const gorevDialog = ref(false)
const seciliProje = ref(null)
const form = ref({ ad: '', aciklama: '', baslangic: new Date(), bitis: null, sorumlu: '' })
const gorevForm = ref({ ad: '', aciklama: '', atanan: '', baslangic: new Date(), bitis: null })

const hataGoster = (err) => {
  toastBildirim.hata(err?.response?.data?.message || err?.message || 'Bir hata oluştu')
}

onMounted(async () => {
  yukleniyor.value = true
  try { const r = await projeAPI.getAll(); list.value = r.data?.content || r.data || [] } catch (e) { hataGoster(e) }
  yukleniyor.value = false
})

const dialogAc = () => { form.value = { ad: '', aciklama: '', baslangic: new Date(), bitis: null, sorumlu: '' }; dialog.value = true }
const kaydet = async () => {
  kaydediliyor.value = true
  try {
    await projeAPI.create({ ...form.value, baslangic: form.value.baslangic?.toISOString().split('T')[0], bitis: form.value.bitis?.toISOString().split('T')[0] })
    dialog.value = false
    const r = await projeAPI.getAll(); list.value = r.data?.content || r.data || []
    toastBildirim.basarili('Proje oluşturuldu')
  } catch (e) { hataGoster(e) }
  kaydediliyor.value = false
}
const durumGuncelle = async (data, durum) => {
  try {
    await projeAPI.durumGuncelle(data.id, durum)
    const r = await projeAPI.getAll(); list.value = r.data?.content || r.data || []
    toastBildirim.basarili('Durum güncellendi')
  } catch (e) { hataGoster(e) }
}
const sil = (data) => {
  confirm.require({
    message: 'Bu kaydı silmek istediğinize emin misiniz?',
    header: 'Silme Onayı',
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: 'Evet, Sil',
    rejectLabel: 'İptal',
    accept: async () => {
      try {
        await projeAPI.delete(data.id)
        list.value = list.value.filter(x => x.id !== data.id)
        toast.add({ severity: 'success', summary: 'Silindi', detail: 'Proje silindi', life: 3000 })
      } catch (e) { hataGoster(e) }
    },
    reject: () => {}
  })
}

const gorevDialogAc = (data) => { seciliProje.value = data; gorevForm.value = { ad: '', aciklama: '', atanan: '', baslangic: new Date(), bitis: null }; gorevDialog.value = true }
const gorevKaydet = async () => {
  kaydediliyor.value = true
  try {
    const g = { ...gorevForm.value, baslangic: gorevForm.value.baslangic?.toISOString().split('T')[0], bitis: gorevForm.value.bitis?.toISOString().split('T')[0] }
    await projeAPI.gorevEkle(seciliProje.value.id, g)
    gorevDialog.value = false; const r = await projeAPI.getAll(); list.value = r.data?.content || r.data || []
    toastBildirim.basarili('Görev eklendi')
  } catch (e) { hataGoster(e) } kaydediliyor.value = false
}
const gorevTamamla = async (g) => {
  try {
    await projeAPI.gorevDurumGuncelle(g.id, 'TAMAMLANDI')
    const r = await projeAPI.getAll(); list.value = r.data?.content || r.data || []
    toastBildirim.basarili('Görev tamamlandı')
  } catch (e) { hataGoster(e) }
}

const expanded = ref({})
const rowExpand = (e) => { expanded.value[e.id] = true }
const rowCollapse = (e) => { delete expanded.value[e.id] }
</script>

<style scoped>
.proje-sayfasi { padding: 0; }
.sayfa-baslik { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.sayfa-baslik h1 { margin: 0; }
.gorevler { padding: 16px; background: rgba(0,0,0,0.1); border-radius: 8px; }
.gorev-baslik { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.gorev-baslik h3 { margin: 0; font-size: 15px; color: var(--text-secondary); }
.form-grid { display: flex; flex-direction: column; gap: 16px; }
.field-row { display: flex; gap: 16px; }
.field-row .field { flex: 1; }
.field { display: flex; flex-direction: column; gap: 6px; }
.field label { font-size: 13px; font-weight: 600; color: var(--text-secondary); }
.w-full { width: 100%; }
</style>