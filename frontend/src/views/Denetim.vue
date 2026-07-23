<template>
  <div class="denetim-sayfasi">
    <h1 class="page-title">Denetim Kayıtları</h1>
    <DataTable :value="list" stripedRows :loading="yukleniyor">
      <Column field="tarih" header="Tarih" sortable>
        <template #body="{ data }">{{ new Date(data.tarih).toLocaleString('tr-TR') }}</template>
      </Column>
      <Column field="kullaniciId" header="Kullanıcı ID" sortable />
      <Column field="islem" header="İşlem" sortable>
        <template #body="{ data }">
          <Tag :value="data.islem" :severity="data.islem === 'SIL' ? 'danger' : data.islem === 'OLUSTUR' ? 'success' : 'info'" />
        </template>
      </Column>
      <Column field="entityAdi" header="Entity" sortable />
      <Column field="entityId" header="Entity ID" sortable />
      <Column field="aciklama" header="Açıklama" />
      <Column field="ipAdresi" header="IP" sortable />
    </DataTable>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useToast } from 'primevue/usetoast'
import { auditLogAPI } from '../api/index.js'
const toast = useToast()

const list = ref([])
const yukleniyor = ref(false)

onMounted(async () => {
  yukleniyor.value = true
  try { list.value = (await auditLogAPI.getAll()).data } catch (err) {
    toast.add({ severity: 'error', summary: 'Hata', detail: err?.response?.data?.message || err?.message || 'Denetim kayıtları yüklenirken hata oluştu', life: 5000 })
  }
  yukleniyor.value = false
})
</script>
