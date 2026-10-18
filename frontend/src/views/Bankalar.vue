<template>
  <div class="bankalar-container">
    <PageHeader :title="t('bankalar.title')">
      <template #actions>
        <Button
          :icon="bakiyeGorunur ? 'pi pi-eye-slash' : 'pi pi-eye'"
          text
          rounded
          :title="bakiyeGorunur ? t('bankalar.bakiyeGizle') : t('bankalar.bakiyeGoster')"
          :aria-label="bakiyeGorunur ? t('bankalar.bakiyeGizle') : t('bankalar.bakiyeGoster')"
          @click="bakiyeGorunurlukDegistir"
        />
        <Button
          :icon="gosterim === 'tablo' ? 'pi pi-th-large' : 'pi pi-list'"
          text
          rounded
          :title="gosterim === 'tablo' ? t('bankalar.kartGorunumu') : t('bankalar.tabloGorunumu')"
          :aria-label="gosterim === 'tablo' ? t('bankalar.kartGorunumu') : t('bankalar.tabloGorunumu')"
          @click="gosterimDegistir"
        />
        <Button
          :label="t('bankalar.excel')"
          icon="pi pi-file-excel"
          class="p-button-sm p-button-outlined"
          :loading="excelYukleniyor"
          @click="excelIndir"
        />
        <Button
          :label="t('bankalar.yeniBanka')"
          icon="pi pi-plus"
          @click="openDialog"
        />
      </template>
    </PageHeader>

    <div class="kpi-grid">
      <KpiKart
        :baslik="t('bankalar.kpiToplamBanka')"
        :deger="ozet.toplamBanka"
        ikon="pi pi-building"
        :para-birimi="false"
        renk="#3b82f6"
      />
      <KpiKart
        :baslik="t('bankalar.kpiToplamBakiye')"
        :deger="ozet.toplamBakiye"
        ikon="pi pi-wallet"
        renk="#10b981"
      >
        <template #deger>
          {{ bakiyeGorunur ? formatCurrency(ozet.toplamBakiye) : '••••' }}
        </template>
      </KpiKart>
      <KpiKart
        :baslik="t('bankalar.kpiPozitifBakiye')"
        :deger="ozet.pozitifBakiye"
        ikon="pi pi-arrow-up-right"
        renk="#0ea5e9"
      >
        <template #deger>
          {{ bakiyeGorunur ? formatCurrency(ozet.pozitifBakiye) : '••••' }}
        </template>
      </KpiKart>
      <KpiKart
        :baslik="t('bankalar.kpiEslesmemisHareket')"
        :deger="ozet.eslesmemisHareket"
        ikon="pi pi-exclamation-circle"
        :para-birimi="false"
        renk="#f59e0b"
      />
    </div>

    <div
      v-if="loading"
      class="loading"
    >
      <p><i class="pi pi-spin pi-spinner" /> {{ t('common.loading') }}</p>
    </div>

    <div
      v-else-if="gosterim === 'tablo'"
      class="table-container"
    >
      <AppDataTable
        :value="bankaStore.bankalar"
        striped-rows
        :rows="10"
        :paginator="true"
        paginator-template="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink CurrentPageReport RowsPerPageDropdown"
        :rows-per-page-options="[10, 20, 50]"
        :current-page-report-template="'{first} - {last} ({totalRecords} ' + $t('common.recordsWord') + ')'"
        gorunum-anahtari="bankalar"
        arama-aktif
        :arama-placeholder="t('bankalar.aramaPlaceholder')"
      >
        <Column
          field="ad"
          :header="t('bankalar.colBankaAdi')"
          style="width: 200px"
          sortable
        />
        <Column
          field="hesapNo"
          :header="t('bankalar.colHesapNo')"
          style="width: 150px"
        >
          <template #body="s">
            {{ s.data.hesapNo || '-' }}
          </template>
        </Column>
        <Column
          field="iban"
          :header="t('bankalar.iban')"
          style="width: 230px"
        >
          <template #body="s">
            <button
              v-if="s.data.iban"
              type="button"
              class="kopyalanabilir"
              :aria-label="t('bankalar.ibanKopyalandi')"
              @click="kopyala(s.data.iban, t('bankalar.ibanKopyalandi'))"
            >
              {{ s.data.iban }} <i class="pi pi-copy kopyala-ikon" />
            </button>
            <span v-else>-</span>
          </template>
        </Column>
        <Column
          field="bakiye"
          :header="t('bankalar.colBakiye')"
          style="width: 130px"
          sortable
        >
          <template #body="s">
            <span
              v-if="bakiyeGorunur"
              class="gizli-veri"
              :class="s.data.bakiye >= 0 ? 'positive' : 'negative'"
            >{{ formatCurrency(s.data.bakiye) }}</span>
            <span v-else>••••</span>
          </template>
        </Column>
        <Column
          :header="t('common.actions')"
          style="width: 70px"
        >
          <template #body="s">
            <SatirEylemleri
              :gorunur="{ duzenle: true, sil: authStore.isAdmin }"
              :baslik="s.data.ad"
              :items="bankaMenuItems(s.data)"
              @duzenle="editBanka(s.data)"
              @sil="confirmDelete(s.data.id)"
            />
          </template>
        </Column>
        <template #empty>
          <EmptyState
            v-if="bankaStore.bankalar.length === 0"
            :message="t('bankalar.empty')"
            :sub-message="t('bankalar.emptyHint')"
            icon="pi pi-building"
            :action-label="t('bankalar.yeniBanka')"
            action-icon="pi pi-plus"
            @action="openDialog"
          />
        </template>
      </AppDataTable>
    </div>

    <div
      v-else
      class="banka-kartlar"
    >
      <EmptyState
        v-if="bankaStore.bankalar.length === 0"
        :message="t('bankalar.empty')"
        :sub-message="t('bankalar.emptyHint')"
        icon="pi pi-building"
        :action-label="t('bankalar.yeniBanka')"
        action-icon="pi pi-plus"
        @action="openDialog"
      />
      <div
        v-for="b in bankaStore.bankalar"
        :key="b.id"
        class="banka-kart"
      >
        <div class="banka-kart-ust">
          <strong>{{ b.ad }}</strong>
          <SatirEylemleri
            :gorunur="{ duzenle: true, sil: authStore.isAdmin }"
            :baslik="b.ad"
            :items="bankaMenuItems(b)"
            @duzenle="editBanka(b)"
            @sil="confirmDelete(b.id)"
          />
        </div>
        <div class="banka-kart-satir">
          <i class="pi pi-hashtag" /> {{ b.hesapNo || '-' }}
        </div>
        <button
          v-if="b.iban"
          type="button"
          class="kopyalanabilir banka-kart-iban"
          :aria-label="t('bankalar.ibanKopyalandi')"
          @click="kopyala(b.iban, t('bankalar.ibanKopyalandi'))"
        >
          <i class="pi pi-copy kopyala-ikon" /> {{ b.iban }}
        </button>
        <div class="banka-kart-bakiye">
          <span
            v-if="bakiyeGorunur"
            :class="b.bakiye >= 0 ? 'positive' : 'negative'"
          >{{ formatCurrency(b.bakiye) }}</span>
          <span v-else>••••</span>
          <Button
            :label="t('bankalar.hareketler')"
            icon="pi pi-list"
            size="small"
            text
            @click="detayAc(b)"
          />
        </div>
      </div>
    </div>
    <Dialog
      v-model:visible="showDialog"
      :header="editingId ? t('bankalar.duzenle') : t('bankalar.yeniBanka')"
      :modal="true"
      style="width: 500px"
      :breakpoints="{ '575px': '92vw' }"
    >
      <div class="form-group">
        <label>{{ t('bankalar.bankaAdi') }}</label>
        <InputText
          v-model="form.ad"
          :placeholder="t('bankalar.bankaAdiPlaceholder')"
          class="w-full"
        />
      </div>
      <div class="form-group">
        <label>{{ t('bankalar.hesapNo') }}</label>
        <InputText
          v-model="form.hesapNo"
          :placeholder="t('bankalar.hesapNoPlaceholder')"
          class="w-full"
        />
      </div>
      <div class="form-group">
        <label>{{ t('bankalar.iban') }}</label>
        <InputText
          v-model="form.iban"
          :placeholder="t('bankalar.ibanPlaceholder')"
          class="w-full"
          :invalid="!ibanGecerli"
        />
        <small
          v-if="!ibanGecerli"
          class="alan-hata"
        >{{ t('bankalar.ibanGecersiz') }}</small>
      </div>
      <div
        v-if="!editingId"
        class="form-group"
      >
        <label>{{ t('bankalar.acilisBakiyesi') }}</label>
        <InputNumber
          v-model="form.bakiye"
          :min="0"
          :min-fraction-digits="2"
          :max-fraction-digits="2"
          class="w-full"
        />
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          icon="pi pi-times"
          class="p-button-text"
          @click="closeDialog"
        />
        <Button
          :label="editingId ? t('bankalar.guncelle') : t('common.save')"
          icon="pi pi-check"
          :loading="saving"
          :disabled="!ibanGecerli"
          @click="saveBanka"
        />
      </template>
    </Dialog>

    <!-- Banka detayı: bakiye kartı ve hareket (mutabakat) geçmişi -->
    <Drawer
      v-model:visible="detayDialog"
      position="right"
      :header="detayBanka?.ad || t('bankalar.title')"
      :style="{ width: '640px', maxWidth: '96vw' }"
    >
      <div
        v-if="detayBanka"
        class="detay-bilgi"
      >
        <div class="detay-kutu">
          <span>{{ t('bankalar.colBakiye') }}</span>
          <strong
            v-if="bakiyeGorunur"
            :class="detayBanka.bakiye >= 0 ? 'positive' : 'negative'"
          >
            {{ formatCurrency(detayBanka.bakiye) }}
          </strong>
          <strong v-else>••••</strong>
        </div>
        <div class="detay-kutu">
          <span>{{ t('bankalar.colHesapNo') }}</span>
          <strong>{{ detayBanka.hesapNo || '-' }}</strong>
        </div>
        <div class="detay-kutu genis">
          <span>IBAN</span>
          <strong
            class="kopyalanabilir"
            @click="detayBanka.iban && kopyala(detayBanka.iban, t('bankalar.ibanKopyalandi'))"
          >
            {{ detayBanka.iban || '-' }} <i
              v-if="detayBanka.iban"
              class="pi pi-copy kopyala-ikon"
            />
          </strong>
        </div>
      </div>

      <div class="detay-baslik">
        <span><i class="pi pi-list" /> {{ t('bankalar.hareketler') }}</span>
        <Button
          :label="t('bankalar.mutabakataGit')"
          icon="pi pi-link"
          class="p-button-sm p-button-outlined"
          @click="mutabakataGit"
        />
      </div>

      <div class="detay-ozet">
        <div class="detay-ozet-kutu">
          <span>{{ t('bankalar.borcToplam') }}</span>
          <strong class="negative">{{ formatCurrency(hareketBorcToplam) }}</strong>
        </div>
        <div class="detay-ozet-kutu">
          <span>{{ t('bankalar.alacakToplam') }}</span>
          <strong class="positive">{{ formatCurrency(hareketAlacakToplam) }}</strong>
        </div>
      </div>

      <div class="detay-filtre">
        <Select
          v-model="hareketDurum"
          :options="hareketDurumSecenekleri"
          option-label="label"
          option-value="value"
          :placeholder="t('bankalar.tumu')"
          show-clear
          class="detay-filtre-alan"
        />
        <DatePicker
          v-model="hareketTarih"
          selection-mode="range"
          date-format="dd.mm.yy"
          :placeholder="t('bankalar.tarihAraligi')"
          class="detay-filtre-alan"
        />
      </div>

      <DataTable
        :value="filtreliHareketler"
        :loading="hareketYukleniyor"
        striped-rows
        size="small"
        scrollable
        scroll-height="52vh"
        :paginator="filtreliHareketler.length > 20"
        :rows="20"
      >
        <template #empty>
          <EmptyState :message="t('bankalar.hareketYok')" />
        </template>
        <Column
          field="tarih"
          :header="t('common.date')"
          style="width: 110px"
        />
        <Column
          field="aciklama"
          :header="t('common.description')"
        />
        <Column
          field="borc"
          :header="t('bankalar.borc')"
          style="width: 110px"
        >
          <template #body="s">
            <span v-if="bakiyeGorunur && s.data.borc">{{ formatCurrency(s.data.borc) }}</span>
          </template>
        </Column>
        <Column
          field="alacak"
          :header="t('bankalar.alacak')"
          style="width: 110px"
        >
          <template #body="s">
            <span v-if="bakiyeGorunur && s.data.alacak">{{ formatCurrency(s.data.alacak) }}</span>
          </template>
        </Column>
        <Column
          field="bakiye"
          :header="t('bankalar.colBakiye')"
          style="width: 120px"
        >
          <template #body="s">
            <span
              v-if="bakiyeGorunur"
              class="gizli-veri"
            >{{ formatCurrency(s.data.bakiye) }}</span>
            <span v-else>••••</span>
          </template>
        </Column>
        <Column
          field="eslestirildi"
          :header="t('bankalar.durum')"
          style="width: 120px"
        >
          <template #body="s">
            <Tag
              v-if="s.data.eslestirildi"
              severity="success"
              :value="t('bankalar.eslesti')"
            />
            <Tag
              v-else
              severity="warn"
              :value="t('bankalar.bekliyor')"
            />
          </template>
        </Column>
      </DataTable>
    </Drawer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useConfirm } from 'primevue/useconfirm'
import { useBankaStore } from '../stores/bankaStore.js'
import { useAuthStore } from '../stores/authStore.js'
import { usePanoyaKopyala } from '../composables/usePanoyaKopyala.js'
import { useFormKorumasi } from '../composables/useFormKorumasi.js'
import { excelAPI, bankaMutabakatAPI, bankaAPI } from '../api/index.js'
import EmptyState from '../components/EmptyState.vue'
import KpiKart from '../components/KpiKart.vue'
import { formatCurrency, getLocalDateString } from '../utils/format.js'
import { useI18n } from 'vue-i18n'

const toastBildirim = useToastBildirim()
const { t } = useI18n()
const confirm = useConfirm()
const router = useRouter()
const bankaStore = useBankaStore()
const authStore = useAuthStore()
const { kopyala } = usePanoyaKopyala()

// Gorunum: tablo / kart (kucuk ekranda varsayilan kart).
const GORUNUM_ANAHTAR = 'raspel_banka_gosterim'
const gosterim = ref(localStorage.getItem(GORUNUM_ANAHTAR) || 'tablo')
const gosterimDegistir = () => {
  gosterim.value = gosterim.value === 'tablo' ? 'kart' : 'tablo'
  localStorage.setItem(GORUNUM_ANAHTAR, gosterim.value)
}

const showDialog = ref(false)
const loading = ref(false)
const saving = ref(false)
const editingId = ref(null)
const excelYukleniyor = ref(false)

// KPI ozeti (hesap/bakiye/eslesmemis hareket).
const ozet = ref({ toplamBanka: 0, toplamBakiye: 0, pozitifBakiye: 0, eslesmemisHareket: 0 })
const ozetYukle = async () => {
  try {
    const r = await bankaAPI.ozet()
    ozet.value = { ...ozet.value, ...(r.data || {}) }
  } catch {
    /* ozet alinamazsa KPI sifirda kalir */
  }
}

// Bakiye gizle/goster (memur ekrani icin); tercih hatirlanir.
const BAKIYE_ANAHTAR = 'raspel_banka_bakiye_gorunur'
const bakiyeGorunur = ref(localStorage.getItem(BAKIYE_ANAHTAR) !== '0')
const bakiyeGorunurlukDegistir = () => {
  bakiyeGorunur.value = !bakiyeGorunur.value
  localStorage.setItem(BAKIYE_ANAHTAR, bakiyeGorunur.value ? '1' : '0')
}

// Banka detayı: hareket (mutabakat) geçmişi
const detayDialog = ref(false)
const detayBanka = ref(null)
const hareketler = ref([])
const hareketYukleniyor = ref(false)

// Drawer hareket filtreleri + özet.
const hareketDurum = ref(null)
const hareketTarih = ref(null)
const hareketDurumSecenekleri = computed(() => [
  { label: t('bankalar.eslesti'), value: 'ESLESTI' },
  { label: t('bankalar.bekliyor'), value: 'BEKLIYOR' }
])
const filtreliHareketler = computed(() =>
  hareketler.value.filter((h) => {
    if (hareketDurum.value === 'ESLESTI' && !h.eslestirildi) return false
    if (hareketDurum.value === 'BEKLIYOR' && h.eslestirildi) return false
    if (Array.isArray(hareketTarih.value) && hareketTarih.value.length === 2 && hareketTarih.value[0]) {
      const t = h.tarih || ''
      if (t < getLocalDateString(hareketTarih.value[0]) || t > getLocalDateString(hareketTarih.value[1])) return false
    }
    return true
  })
)
const hareketBorcToplam = computed(() => hareketler.value.reduce((a, h) => a + (Number(h.borc) || 0), 0))
const hareketAlacakToplam = computed(() => hareketler.value.reduce((a, h) => a + (Number(h.alacak) || 0), 0))

const detayAc = async (banka) => {
  detayBanka.value = banka
  detayDialog.value = true
  hareketDurum.value = null
  hareketTarih.value = null
  hareketYukleniyor.value = true
  hareketler.value = []
  try {
    const r = await bankaMutabakatAPI.listele(banka.id)
    hareketler.value = Array.isArray(r.data) ? r.data : []
  } catch {
    hareketler.value = []
  } finally {
    hareketYukleniyor.value = false
  }
}

const mutabakataGit = () => {
  detayDialog.value = false
  router.push({ name: 'BankaMutabakat' })
}

// Satir ⋮ menusu: hareketler (detay); duzenle/sil yerlesik gelir.
const bankaMenuItems = (b) => [
  { etiket: t('bankalar.hareketler'), ikon: 'pi pi-list', islem: () => detayAc(b) }
]

const form = ref({ ad: '', hesapNo: '', iban: '', bakiye: 0 })
const { temizle: formTemizle } = useFormKorumasi(form)

// TR IBAN: "TR" + 24 rakam (26 karakter). Bos birakilabilir.
const IBAN_TR_RE = /^TR\d{24}$/
const ibanNormalize = (v) => (v || '').replace(/\s+/g, '').toUpperCase()
const ibanGecerli = computed(() => {
  const s = ibanNormalize(form.value.iban)
  return s === '' || IBAN_TR_RE.test(s)
})

onMounted(async () => {
  // Kayitli tercih yoksa kucuk ekranda kart gorunumu varsayilan.
  if (!localStorage.getItem(GORUNUM_ANAHTAR)
      && typeof window.matchMedia === 'function'
      && window.matchMedia('(max-width: 768px)').matches) {
    gosterim.value = 'kart'
  }
  loading.value = true
  try {
    await bankaStore.getAllBankalar()
    ozetYukle()
  } catch {
    toastBildirim.hata(t('bankalar.hataYukleme'))
  } finally {
    loading.value = false
  }
})

const openDialog = () => {
  editingId.value = null
  form.value = { ad: '', hesapNo: '', iban: '', bakiye: 0 }
  formTemizle()
  showDialog.value = true
}

const closeDialog = () => {
  showDialog.value = false
}

const editBanka = (banka) => {
  editingId.value = banka.id
  form.value = { ad: banka.ad, hesapNo: banka.hesapNo || '', iban: banka.iban || '', bakiye: 0 }
  formTemizle()
  showDialog.value = true
}

const saveBanka = async () => {
  if (!form.value.ad.trim()) {
    toastBildirim.uyari(t('bankalar.adBosOlamaz'))
    return
  }
  if (!ibanGecerli.value) {
    toastBildirim.uyari(t('bankalar.ibanGecersiz'))
    return
  }
  const iban = ibanNormalize(form.value.iban)
  saving.value = true
  try {
    if (editingId.value) {
      await bankaStore.updateBanka(editingId.value, {
        ad: form.value.ad,
        hesapNo: form.value.hesapNo,
        iban
      })
      toastBildirim.basarili(t('bankalar.guncellendi'))
    } else {
      await bankaStore.addBanka({ ...form.value, iban })
      toastBildirim.basarili(t('bankalar.olusturuldu'))
    }
    formTemizle()
    closeDialog()
    ozetYukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('bankalar.islemBasarisiz'))
  } finally {
    saving.value = false
  }
}

const confirmDelete = (id) => {
  confirm.require({
    message: t('bankalar.silOnayMesaj'),
    header: t('common.silmeOnayi'),
    icon: 'pi pi-exclamation-triangle',
    rejectProps: { label: t('common.vazgec'), severity: 'secondary', outlined: true, size: 'small' },
    acceptProps: { label: t('common.evetSil'), severity: 'danger', size: 'small' },
    accept: async () => {
      try {
        await bankaStore.deleteBanka(id)
        toastBildirim.basarili(t('bankalar.silindi'))
        ozetYukle()
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || t('bankalar.silmeBasarisiz'))
      }
    }
  })
}

const excelIndir = async () => {
  excelYukleniyor.value = true
  try {
    const res = await excelAPI.bankalar()
    const url = window.URL.createObjectURL(new Blob([res.data]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', 'Bankalar.xlsx')
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)
    toastBildirim.basarili(t('bankalar.excelIndirildi'))
  } catch {
    toastBildirim.hata(t('bankalar.excelHata'))
  } finally {
    excelYukleniyor.value = false
  }
}

</script>

<style scoped>
.bankalar-container {
  padding: 0;
  max-width: 100%;
}
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(170px, 100%), 1fr));
  gap: 12px;
  margin-bottom: 1rem;
}
.table-container {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 14px;
}
.loading {
  text-align: center;
  padding: 40px;
  color: var(--text-muted);
}
.form-group {
  margin-bottom: 20px;
}
.form-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: bold;
  color: var(--text-secondary);
}
.positive {
  color: var(--success);
  font-weight: bold;
}
.negative {
  color: var(--danger);
  font-weight: bold;
}
.kopyalanabilir {
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: none;
  background: transparent;
  padding: 0;
  font: inherit;
  color: inherit;
}
.detay-bilgi {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin-bottom: 16px;
}
.detay-kutu {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 10px 12px;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: var(--bg-card);
}
.detay-kutu span {
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  color: var(--text-muted);
}
.detay-kutu.genis {
  grid-column: 1 / -1;
}
.detay-baslik {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 10px;
  font-weight: 600;
  color: var(--text-primary);
}
.kopyalanabilir:hover {
  color: var(--accent);
}
.kopyala-ikon {
  font-size: 11px;
  opacity: 0.5;
}
.kopyalanabilir:hover .kopyala-ikon {
  opacity: 1;
}
.w-full {
  width: 100% !important;
}
.banka-kartlar {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(min(280px, 100%), 1fr));
  gap: 12px;
}
.banka-kart {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 14px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 1rem;
}
.banka-kart-ust {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.banka-kart-ust strong {
  font-size: 1rem;
  color: var(--text-primary);
  min-width: 0;
}
.banka-kart-satir {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.82rem;
  color: var(--text-secondary);
}
.banka-kart-iban {
  font-size: 0.82rem;
  color: var(--text-secondary);
}
.banka-kart-bakiye {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-top: auto;
  padding-top: 8px;
  border-top: 1px solid var(--border);
  font-weight: 700;
}
.detay-ozet {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin-bottom: 10px;
}
.detay-ozet-kutu {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 10px 12px;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: var(--bg-card);
}
.detay-ozet-kutu span {
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  color: var(--text-muted);
}
.detay-ozet-kutu strong {
  font-size: 1rem;
}
.detay-filtre {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
  flex-wrap: wrap;
}
.detay-filtre-alan {
  flex: 1 1 180px;
  min-width: 0;
}
.alan-hata {
  display: block;
  margin-top: 6px;
  color: var(--danger, #ef4444);
  font-size: 0.75rem;
}

/* Mobil: detay ozeti ve filtreler tek kolona iner. */
@media (max-width: 560px) {
  .detay-ozet {
    grid-template-columns: 1fr;
  }
  .detay-filtre-alan {
    flex: 1 1 100%;
  }
}
</style>
