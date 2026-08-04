<template>
  <div class="mutabakat-container">
    <div class="sayfa-baslik">
      <h1 class="page-title">
        Banka Mutabakatı
      </h1>
    </div>

    <IlkZiyaretIpuclari
      anahtar="banka-mutabakat"
      baslik="Banka Mutabakatı"
      metin="Bankanızdan indirdiğiniz hesap özetini (CSV/Excel) yükleyin; sistem hareketleri faturalarla otomatik eşleştirir. Eşleşmeyenleri elle bağlayabilirsiniz."
    />

    <div class="mutabakat-ust">
      <div class="banka-secim">
        <label>Banka</label>
        <Select
          v-model="seciliBanka"
          :options="bankalar"
          option-label="ad"
          option-value="id"
          placeholder="Banka seçin"
          class="w-full"
          filter
          show-clear
          @change="bankaDegisti"
        />
      </div>
      <div class="ust-butonlar">
        <input
          ref="dosyaInput"
          type="file"
          accept=".csv,.xlsx,.txt"
          hidden
          @change="dosyaSecildi"
        >
        <Button
          label="Hesap Özeti Yükle"
          icon="pi pi-upload"
          :disabled="!seciliBanka"
          :loading="yukleniyor"
          @click="dosyaInput.click()"
        />
        <Button
          label="Otomatik Eşleştir"
          icon="pi pi-link"
          severity="secondary"
          outlined
          :disabled="!seciliBanka"
          @click="otomatikEslestir"
        />
      </div>
    </div>

    <div
      v-if="eslesenSayisi !== null"
      class="mutabakat-ozet"
    >
      <div class="ozet-kutu">
        <span>Toplam Hareket</span><strong>{{ hareketler.length }}</strong>
      </div>
      <div class="ozet-kutu">
        <span>Eşleşen</span><strong class="pozitif">{{ eslesenSayisi }}</strong>
      </div>
      <div class="ozet-kutu">
        <span>Eşleşmeyen</span><strong class="negatif">{{ eslesmeyenSayisi }}</strong>
      </div>
      <div class="ozet-kutu">
        <span>Eşleşme Oranı</span><strong>{{ eslesmeOrani }}</strong>
      </div>
    </div>

    <AppDataTable
      :value="hareketler"
      :loading="yukleniyor"
      arama-aktif
      arama-placeholder="Hareketlerde ara..."
      gorunum-anahtari="banka_mutabakat"
      empty-message="Hesap özeti yüklenmedi"
    >
      <Column
        field="tarih"
        header="Tarih"
        sortable
      >
        <template #body="{ data }">
          {{ formatDate(data.tarih) }}
        </template>
      </Column>
      <Column
        field="aciklama"
        header="Açıklama"
      />
      <Column
        field="borc"
        header="Borç"
      >
        <template #body="{ data }">
          <span class="negatif">{{ formatCurrency(data.borc) }}</span>
        </template>
      </Column>
      <Column
        field="alacak"
        header="Alacak"
      >
        <template #body="{ data }">
          <span class="pozitif">{{ formatCurrency(data.alacak) }}</span>
        </template>
      </Column>
      <Column
        field="bakiye"
        header="Bakiye"
      >
        <template #body="{ data }">
          {{ data.bakiye != null ? formatCurrency(data.bakiye) : '-' }}
        </template>
      </Column>
      <Column
        field="eslestirildi"
        header="Eşleşme"
        sortable
      >
        <template #body="{ data }">
          <Tag
            :value="data.eslestirildi ? 'Eşleşti' : 'Eşleşmedi'"
            :severity="data.eslestirildi ? 'success' : 'danger'"
          />
          <div
            v-if="data.eslesenFaturaNo"
            class="eslesen-fatura"
          >
            #{{ data.eslesenFaturaNo }}
          </div>
        </template>
      </Column>
      <Column
        header="İşlem"
        style="width:70px"
      >
        <template #body="{ data }">
          <div
            v-if="!data.eslestirildi"
            class="eylem-btns"
          >
            <Select
              v-model="data.eslesenFaturaId"
              :options="faturalar"
              option-label="etiket"
              option-value="id"
              placeholder="Fatura bağla"
              filter
              class="fatura-bagla"
              @change="manuelEslestir(data)"
            />
          </div>
          <Button
            v-else
            icon="pi pi-times"
            class="p-button-rounded p-button-text"
            title="Eşleştirmeyi kaldır"
            @click="eslestirmeyiKaldir(data)"
          />
        </template>
      </Column>
    </AppDataTable>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useToast } from 'primevue/usetoast'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useConfirm } from 'primevue/useconfirm'
import { bankaAPI, bankaMutabakatAPI, faturaAPI } from '../api/index.js'
import IlkZiyaretIpuclari from '../components/IlkZiyaretIpuclari.vue'

const toast = useToast()
const toastBildirim = useToastBildirim()
const confirm = useConfirm()

const bankalar = ref([])
const faturalar = ref([])
const seciliBanka = ref(null)
const hareketler = ref([])
const yukleniyor = ref(false)
const dosyaInput = ref(null)

const eslesenSayisi = computed(() => hareketler.value.filter(h => h.eslestirildi).length)
const eslesmeyenSayisi = computed(() => hareketler.value.length - eslesenSayisi.value)
const eslesmeOrani = computed(() => {
  if (!hareketler.value.length) return '—'
  return Math.round((eslesenSayisi.value / hareketler.value.length) * 100) + '%'
})

const formatCurrency = (v) => v == null || Number(v) === 0 ? '0,00 ₺' : new Intl.NumberFormat('tr-TR', { style: 'currency', currency: 'TRY' }).format(v)
const formatDate = (d) => d ? new Intl.DateTimeFormat('tr-TR', { year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date(d)) : '-'

onMounted(async () => {
  try {
    const r = await bankaAPI.getAll()
    bankalar.value = r.data || []
  } catch { /* empty */ }
  try {
    const rf = await faturaAPI.getAll()
    const data = rf.data?.content || rf.data || []
    faturalar.value = data.map(f => ({ ...f, etiket: `${f.faturaNumarasi} (${formatCurrency(f.genelToplam)})` }))
  } catch { /* empty */ }
})

const bankaDegisti = () => {
  hareketler.value = []
  if (seciliBanka.value) yukle()
}

const yukle = async () => {
  yukleniyor.value = true
  try {
    const r = await bankaMutabakatAPI.listele(seciliBanka.value)
    hareketler.value = r.data || []
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || 'Hareketler yüklenemedi')
  }
  yukleniyor.value = false
}

const dosyaSecildi = async (e) => {
  const file = e.target.files[0]
  if (!file) return
  yukleniyor.value = true
  try {
    await bankaMutabakatAPI.yukle(seciliBanka.value, file)
    toast.add({ severity: 'success', summary: 'Yüklendi', detail: 'Hesap özeti yüklendi ve eşleştirildi', life: 3000 })
    await yukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || 'Yükleme başarısız')
  }
  yukleniyor.value = false
  e.target.value = ''
}

const otomatikEslestir = async () => {
  yukleniyor.value = true
  try {
    await bankaMutabakatAPI.otomatikEslestir(seciliBanka.value)
    toast.add({ severity: 'success', summary: 'Eşleştirildi', detail: 'Otomatik eşleştirme tamamlandı', life: 3000 })
    await yukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || 'Eşleştirme başarısız')
  }
  yukleniyor.value = false
}

const manuelEslestir = async (hareket) => {
  if (!hareket.eslesenFaturaId) return
  try {
    await bankaMutabakatAPI.eslestir(seciliBanka.value, hareket.id, hareket.eslesenFaturaId)
    toast.add({ severity: 'success', summary: 'Eşleşti', detail: 'Hareket fatura ile eşleştirildi', life: 3000 })
    await yukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || 'Eşleştirme başarısız')
  }
}

const eslestirmeyiKaldir = (hareket) => {
  confirm.require({
    message: 'Bu eşleştirmeyi kaldırmak istediğinize emin misiniz?',
    header: 'Onay', icon: 'pi pi-exclamation-triangle',
    acceptLabel: 'Evet', rejectLabel: 'İptal',
    accept: async () => {
      try {
        await bankaMutabakatAPI.eslestirmeyiKaldir(seciliBanka.value, hareket.id)
        toast.add({ severity: 'success', summary: 'Kaldırıldı', detail: 'Eşleştirme kaldırıldı', life: 3000 })
        await yukle()
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || 'İşlem başarısız')
      }
    }
  })
}
</script>

<style scoped>
.mutabakat-container { padding: 0; }
.sayfa-baslik { margin-bottom: 20px; }
.mutabakat-ust { display: flex; align-items: flex-end; justify-content: space-between; gap: 16px; margin-bottom: 20px; flex-wrap: wrap; }
.banka-secim { min-width: 280px; display: flex; flex-direction: column; gap: 6px; }
.banka-secim label { font-size: 12px; font-weight: 600; color: var(--text-secondary); text-transform: uppercase; }
.ust-butonlar { display: flex; gap: 8px; }
.mutabakat-ozet { display: flex; gap: 14px; margin-bottom: 18px; flex-wrap: wrap; }
.ozet-kutu {
  flex: 1; min-width: 140px; background: var(--bg-card); border: 1px solid var(--border);
  border-radius: 12px; padding: 14px 16px; display: flex; flex-direction: column; gap: 4px;
}
.ozet-kutu span { font-size: 12px; color: var(--text-muted); font-weight: 600; text-transform: uppercase; }
.ozet-kutu strong { font-size: 18px; }
.pozitif { color: #10b981; font-weight: 600; }
.negatif { color: #ef4444; font-weight: 600; }
.eslesen-fatura { font-size: 11px; color: #10b981; margin-top: 4px; font-weight: 600; }
.fatura-bagla { min-width: 160px; }
.eylem-btns { display: flex; align-items: center; }
.w-full { width: 100%; }
</style>
