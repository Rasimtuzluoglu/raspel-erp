<template>
  <div
    class="fatura-detay"
    :class="{ 'print-mode': printMode }"
  >
    <div class="detay-header">
      <Button
        :label="t('faturaDetay.geri')"
        icon="pi pi-arrow-left"
        class="p-button-text no-print"
        @click="$router.push('/faturalar')"
      />
      <div class="detay-ayarlar no-print">
        <SelectButton
          v-model="faturaFiyatli"
          :options="fiyatSecenekleri"
          option-label="label"
          option-value="value"
          size="small"
        />
        <Button
          :label="t('faturaDetay.epostaGonder')"
          icon="pi pi-envelope"
          :loading="emailGonderiliyor"
          :disabled="!fatura?.cariHesapAd"
          @click="gonderEmail"
        />
        <Button
          :label="t('faturaDetay.sablonTasarlaYazdir')"
          icon="pi pi-palette"
          class="p-button-primary"
          @click="tasarimModalAcik = true"
        />
      </div>
    </div>

    <FaturaTasarimModal
      v-model:visible="tasarimModalAcik"
      :fatura-data="fatura"
    />

    <div
      v-if="loading"
      class="loading"
    >
      <p><i class="pi pi-spin pi-spinner" /> {{ t('common.loading') }}</p>
    </div>

    <div
      v-if="fatura && !loading"
      class="fatura-kagit"
    >
      <div class="fatura-baslik">
        <div class="firma-bilgi">
          <h2>{{ t('faturaDetay.onMuhasebe') }}</h2>
          <p>{{ sirket?.ad || authStore.sirketAdi || 'RasPel ERP' }}</p>
          <p v-if="sirket?.vergiDairesi">
            {{ t('faturaDetay.vergiDairesi') }} {{ sirket.vergiDairesi }}
          </p>
          <p v-if="sirket?.vergiNo">
            {{ t('faturaDetay.vergiNo') }} {{ sirket.vergiNo }}
          </p>
          <p v-if="sirket?.adres">
            {{ sirket.adres }}
          </p>
        </div>
        <div class="fatura-bilgi">
          <h1>{{ fatura.tur === 'SATIS' ? t('faturaDetay.satisFaturasi') : t('faturaDetay.alisFaturasi') }}</h1>
          <p><strong>{{ t('faturaDetay.faturaNo') }}</strong> {{ fatura.faturaNumarasi }}</p>
          <p><strong>{{ t('faturaDetay.tarihLabel') }}</strong> {{ formatDate(fatura.tarih) }}</p>
          <p>
            <strong>{{ t('faturaDetay.durumBaslik') }}</strong>
            <span :class="['durum-badge', (fatura.durum || '').toLowerCase()]">{{ durumLabel(fatura.durum) }}</span>
          </p>
          <p><strong>{{ t('faturaDetay.islemiYapan') }}</strong> {{ fatura.olusturanKullaniciAdi }}</p>
          <p v-if="fatura.teslimEden">
            <strong>{{ t('faturaDetay.teslimEden') }}</strong> {{ fatura.teslimEden }}
          </p>
          <p v-if="fatura.teslimDurumu">
            <strong>{{ t('faturaDetay.teslimDurumu') }}</strong>
            <span :class="['teslim-badge', (fatura.teslimDurumu || '').toLowerCase()]">{{
              teslimDurumLabel(fatura.teslimDurumu)
            }}</span>
          </p>
          <p v-if="fatura.teslimNotu">
            <strong>{{ t('faturaDetay.teslimNotu') }}</strong> {{ fatura.teslimNotu }}
          </p>
          <img
            v-if="fatura.teslimFotograf"
            :src="fatura.teslimFotograf"
            class="teslim-fotograf"
            :alt="t('faturaDetay.teslimatFotografi')"
          >
        </div>
      </div>

      <div
        v-if="fatura.cariHesapAd"
        class="cari-bilgi"
      >
        <h3>{{ t('faturaDetay.musteriTedarikciBilgisi') }}</h3>
        <p>
          <strong>{{ fatura.cariHesapAd }}</strong>
        </p>
      </div>

      <table class="fatura-tablo">
        <thead>
          <tr>
            <th>#</th>
            <th>{{ t('common.description') }}</th>
            <th>{{ t('faturaDetay.adet') }}</th>
            <th v-if="faturaFiyatli">
              {{ t('faturaDetay.birimFiyat') }}
            </th>
            <th v-if="faturaFiyatli">
              {{ t('faturaDetay.iskonto') }}
            </th>
            <th v-if="faturaFiyatli">
              {{ t('faturaDetay.kdv') }}
            </th>
            <th v-if="faturaFiyatli">
              {{ t('faturaDetay.toplam') }}
            </th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="(k, i) in fatura.kalemler"
            :key="k.id"
          >
            <td>{{ i + 1 }}</td>
            <td>{{ k.aciklama }}</td>
            <td>{{ k.adet }}</td>
            <td v-if="faturaFiyatli">
              {{ formatCurrency(k.birimFiyat) }}
            </td>
            <td v-if="faturaFiyatli">
              {{ k.iskontoOrani || 0 }}%
            </td>
            <td v-if="faturaFiyatli">
              {{ k.kdvOrani }}%
            </td>
            <td
              v-if="faturaFiyatli"
              class="text-right"
            >
              {{ formatCurrency(k.tutar) }}
            </td>
          </tr>
        </tbody>
      </table>

      <div
        v-if="faturaFiyatli"
        class="fatura-ozet"
      >
        <div class="ozet-row">
          <span>{{ t('faturaDetay.araToplam') }}</span><span>{{ formatCurrency(fatura.araToplam) }}</span>
        </div>
        <div class="ozet-row">
          <span>{{ t('faturaDetay.kdvToplam') }}</span><span>{{ formatCurrency(fatura.kdv) }}</span>
        </div>
        <div
          v-if="fatura.genelIskontoTutari > 0"
          class="ozet-row"
        >
          <span>{{ t('faturaDetay.genelIskonto') }}</span><span class="negative">-{{ formatCurrency(fatura.genelIskontoTutari) }}</span>
        </div>
        <div class="ozet-row total">
          <span>{{ t('faturaDetay.genelToplam') }}</span><span>{{ formatCurrency(fatura.genelToplam) }}</span>
        </div>
        <div
          v-if="fatura.toplamAgirlik > 0"
          class="ozet-row total"
        >
          <span>{{ t('faturaDetay.toplamAgirlik') }}</span><span>{{ fatura.toplamAgirlik }} kg</span>
        </div>
        <div
          v-if="fatura.odemeDurumu"
          class="ozet-row odeme"
        >
          <span>{{ t('faturaDetay.odemeDurumu') }}</span><span :class="fatura.odemeDurumu === 'ODENDI' ? 'positive' : 'negative'">{{
            odemeDurumLabel(fatura.odemeDurumu)
          }}</span>
        </div>
        <div
          v-if="fatura.odenenTutar > 0"
          class="ozet-row odeme"
        >
          <span>{{ t('faturaDetay.odenen') }}</span><span>{{ formatCurrency(fatura.odenenTutar) }}</span>
        </div>
        <div
          v-if="fatura.kalanTutar > 0"
          class="ozet-row odeme"
        >
          <span>{{ t('faturaDetay.kalan') }}</span><span class="negative">{{ formatCurrency(fatura.kalanTutar) }}</span>
        </div>
      </div>

      <div
        v-if="fatura.aciklama"
        class="fatura-yazi"
      >
        <p><strong>{{ t('faturaDetay.aciklamaBaslik') }}</strong> {{ fatura.aciklama }}</p>
      </div>

      <div class="belgeler no-print">
        <div class="belgeler-baslik">
          <h3><i class="pi pi-paperclip" /> {{ t('faturaDetay.belgeler') }}</h3>
          <div class="belge-yukleme">
            <input
              ref="dosyaInput"
              type="file"
              hidden
              @change="dosyaSecildi"
            >
            <Button
              :label="t('faturaDetay.belgeEkle')"
              icon="pi pi-plus"
              size="small"
              class="p-button-sm p-button-outlined"
              :loading="belgeYukleniyor"
              @click="dosyaInput.click()"
            />
          </div>
        </div>
        <div
          v-if="belgeler && belgeler.length === 0"
          class="belge-bos"
        >
          {{ t('faturaDetay.belgeBos') }}
        </div>
        <div
          v-else
          class="belge-liste"
        >
          <div
            v-for="b in belgeler"
            :key="b.id"
            class="belge-item"
          >
            <i class="pi pi-file belge-ikon" />
            <span class="belge-ad">{{ b.dosyaAdi }}</span>
            <span class="belge-tarih">{{ formatDateTime(b.olusturmaTarihi) }}</span>
            <Button
              icon="pi pi-download"
              class="p-button-rounded p-button-text p-button-sm"
              :title="t('faturaDetay.indir')"
              @click="belgeIndir(b)"
            />
            <Button
              icon="pi pi-trash"
              class="p-button-rounded p-button-text p-button-danger p-button-sm"
              :title="t('common.delete')"
              @click="belgeSil(b.id)"
            />
          </div>
        </div>
      </div>

      <div class="fatura-alt">
        <p>{{ t('faturaDetay.olusturma') }} {{ formatDateTime(fatura.olusturmaTarihi) }}</p>
      </div>
    </div>

    <div
      v-if="error && !loading"
      class="error-card"
    >
      <Message
        severity="error"
        :text="error"
      />
      <Button
        :label="t('faturaDetay.faturalaraDon')"
        icon="pi pi-arrow-left"
        @click="$router.push('/faturalar')"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useToast } from 'primevue/usetoast'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useFaturaStore } from '../stores/faturaStore.js'
import SelectButton from 'primevue/selectbutton'
import { useYakinZamanda } from '../composables/useYakinZamanda.js'
import { belgeAPI, faturaAPI, sirketAPI } from '../api/index.js'
import { useAuthStore } from '../stores/authStore.js'
import FaturaTasarimModal from '../components/FaturaTasarimModal.vue'
import { formatCurrency } from '../utils/format.js'
import { useI18n } from 'vue-i18n'

const route = useRoute()
const router = useRouter()
const faturaStore = useFaturaStore()
const authStore = useAuthStore()
const win = window
const { t } = useI18n()

const fatura = ref(null)
const sirket = ref(null)
const loading = ref(true)
const error = ref(null)
const printMode = ref(route.query.print === 'true')
const tasarimModalAcik = ref(false)

const escListener = (e) => {
  if (e.key === 'Escape') router.push('/faturalar')
}

onMounted(() => {
  window.addEventListener('keydown', escListener)
})

onUnmounted(() => {
  window.removeEventListener('keydown', escListener)
})

const toast = useToast()
const toastBildirim = useToastBildirim()
const dosyaInput = ref(null)
const belgeler = ref([])
const belgeYukleniyor = ref(false)
const emailGonderiliyor = ref(false)

const gonderEmail = async () => {
  if (!fatura.value) return
  emailGonderiliyor.value = true
  try {
    await faturaAPI.gonderEmail(fatura.value.id)
    toast.add({ severity: 'success', summary: t('faturaDetay.gonderildi'), detail: t('faturaDetay.faturaPdfGonderildi'), life: 3000 })
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('faturaDetay.epostaGonderilemedi'))
  }
  emailGonderiliyor.value = false
}

const belgeleriYukle = async () => {
  try {
    const r = await belgeAPI.kayitBelgeleri('FATURA', route.params.id)
    belgeler.value = r.data || []
  } catch {
    /* empty */
  }
}

const dosyaSecildi = async (e) => {
  const file = e.target.files[0]
  if (!file) return
  belgeYukleniyor.value = true
  try {
    await belgeAPI.yukle('FATURA', route.params.id, file)
    toast.add({ severity: 'success', summary: t('faturaDetay.eklendi'), detail: t('faturaDetay.belgeYuklendi'), life: 3000 })
    await belgeleriYukle()
  } catch {
    toastBildirim.hata(t('faturaDetay.belgeYuklenemedi'))
  } finally {
    belgeYukleniyor.value = false
    e.target.value = ''
  }
}

const belgeIndir = async (b) => {
  try {
    const filename = b.url.split('/').pop()
    const res = await belgeAPI.indir(filename)
    const blobUrl = window.URL.createObjectURL(new Blob([res.data]))
    const link = document.createElement('a')
    link.href = blobUrl
    link.setAttribute('download', b.dosyaAdi)
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(blobUrl)
  } catch {
    toastBildirim.hata(t('faturaDetay.belgeIndirilemedi'))
  }
}

const belgeSil = async (id) => {
  try {
    await belgeAPI.sil(id)
    belgeler.value = belgeler.value.filter((b) => b.id !== id)
    toast.add({ severity: 'success', summary: t('faturaDetay.silindi'), detail: t('faturaDetay.belgeSilindi'), life: 3000 })
  } catch {
    toastBildirim.hata(t('faturaDetay.silmeBasarisiz'))
  }
}

const faturaFiyatli = ref(true)
const fiyatSecenekleri = computed(() => [
  { label: t('faturaDetay.fiyatli'), value: true },
  { label: t('faturaDetay.fiyatsiz'), value: false }
])

onMounted(async () => {
  try {
    fatura.value = await faturaStore.getFaturaById(route.params.id)
    useYakinZamanda().kaydet(
      'fatura',
      fatura.value.id,
      `#${fatura.value.faturaNumarasi || fatura.value.id}`,
      fatura.value.cariHesapAd
    )
    belgeleriYukle()
  } catch (err) {
    error.value = err.response?.data?.message || t('faturaDetay.faturaBulunamadi')
  } finally {
    loading.value = false
  }
  try {
    if (authStore.sirketId) {
      const r = await sirketAPI.getById(authStore.sirketId)
      sirket.value = r.data || null
    }
  } catch {
    /* empty */
  }
  if (printMode.value)
    setTimeout(() => {
      try {
        win.focus()
        win.print()
      } catch (e) {
        console.error('Yazdırma hatası:', e)
      }
    }, 300)
})

const durumLabel = (d) => ({ TASLAK: t('faturalar.durumTaslak'), TEKLIF: t('faturalar.durumTeklif'), KESILDI: t('faturalar.durumKesildi'), IPTAL: t('faturalar.durumIptal') })[d] || d
const odemeDurumLabel = (d) => ({ ODENMEDI: t('faturaDetay.odenmedi'), KISMI_ODENDI: t('faturaDetay.kismiOdedi'), ODENDI: t('faturaDetay.odendi') })[d] || d
const teslimDurumLabel = (d) => ({ BEKLIYOR: t('faturalar.durumBekliyor'), YOLDA: t('faturalar.durumYolda'), TESLIM_EDILDI: t('faturalar.durumTeslimEdildi') })[d] || d


import { formatTarih as formatDate, formatTarihKisa as formatDateTime } from '../utils/format.js'
</script>

<style scoped>
.fatura-detay {
  padding: 0;
  max-width: 100%;
}
.detay-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 20px;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.detay-ayarlar {
  display: flex;
  align-items: center;
  gap: 8px;
}
.loading {
  text-align: center;
  padding: 60px;
  color: #666;
}
.fatura-kagit {
  background: var(--bg-card);
  padding: 40px;
  border: 1px solid var(--border);
  border-radius: 14px;
  max-width: 800px;
  margin: 0 auto;
}
.fatura-baslik {
  display: flex;
  justify-content: space-between;
  margin-bottom: 30px;
  border-bottom: 2px solid #1976d2;
  padding-bottom: 20px;
}
.fatura-baslik h1 {
  color: #1976d2;
  font-size: 22px;
  margin: 0 0 10px 0;
}
.fatura-baslik h2 {
  color: #333;
  margin: 0 0 10px 0;
}
.firma-bilgi p,
.fatura-bilgi p {
  margin: 3px 0;
  font-size: 13px;
}
.cari-bilgi {
  background: #f5f5f5;
  padding: 15px;
  border-radius: 4px;
  margin-bottom: 20px;
}
.cari-bilgi h3 {
  margin: 0 0 8px 0;
  font-size: 14px;
  color: #666;
}
.cari-bilgi p {
  margin: 0;
  font-size: 16px;
}
.fatura-tablo {
  width: 100%;
  border-collapse: collapse;
  margin-bottom: 20px;
}
.fatura-tablo th {
  background: #1976d2;
  color: white;
  padding: 10px;
  text-align: left;
  font-size: 13px;
}
.fatura-tablo td {
  padding: 10px;
  border-bottom: 1px solid #eee;
  font-size: 13px;
}
.fatura-tablo tr:nth-child(even) {
  background: #f9f9f9;
}
.text-right {
  text-align: right;
}
.fatura-ozet {
  margin-left: auto;
  width: 300px;
  background: #f8f9fa;
  padding: 15px;
  border-radius: 8px;
}
.ozet-row {
  display: flex;
  justify-content: space-between;
  padding: 5px 0;
  font-size: 14px;
}
.ozet-row.total {
  font-weight: bold;
  font-size: 18px;
  border-top: 2px solid #1976d2;
  margin-top: 5px;
  padding-top: 10px;
}
.ozet-row.odeme {
  font-size: 13px;
  color: #666;
}
.ozet-row .negative {
  color: #f44336;
}
.ozet-row .positive {
  color: #4caf50;
}
.fatura-yazi {
  margin-top: 20px;
  padding: 15px;
  background: #fff8e1;
  border-radius: 4px;
}
.belgeler {
  margin-top: 20px;
  padding: 15px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 8px;
}
.belgeler-baslik {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}
.belgeler-baslik h3 {
  margin: 0;
  font-size: 15px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.belge-bos {
  font-size: 13px;
  color: var(--text-muted);
}
.belge-liste {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.belge-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border: 1px solid var(--border);
  border-radius: 6px;
}
.belge-ikon {
  color: #60a5fa;
  font-size: 16px;
}
.belge-ad {
  flex: 1;
  font-size: 13px;
  word-break: break-all;
}
.belge-tarih {
  font-size: 11px;
  color: var(--text-muted);
  white-space: nowrap;
}
@media print {
  .belgeler {
    display: none !important;
  }
}
.fatura-alt {
  margin-top: 30px;
  text-align: center;
  color: #999;
  font-size: 12px;
  border-top: 1px solid #eee;
  padding-top: 15px;
}
.durum-badge {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: bold;
}
.teslim-badge {
  padding: 2px 10px;
  border-radius: 20px;
  font-size: 11px;
  font-weight: 600;
}
.teslim-badge.bekliyor {
  background: rgba(255, 152, 0, 0.15);
  color: #fb923c;
}
.teslim-badge.yolda {
  background: rgba(96, 165, 250, 0.15);
  color: #60a5fa;
}
.teslim-badge.teslim_edildi {
  background: rgba(34, 197, 94, 0.15);
  color: #4ade80;
}
.teslim-fotograf {
  margin-top: 10px;
  max-width: 260px;
  max-height: 180px;
  border-radius: 10px;
  border: 1px solid var(--border);
}
.durum-badge.taslak {
  background: #fff3e0;
  color: #e65100;
}
.durum-badge.teklif {
  background: #e3f2fd;
  color: #1565c0;
}
.durum-badge.kesildi {
  background: #e8f5e9;
  color: #2e7d32;
}
.durum-badge.iptal {
  background: #f5f5f5;
  color: #9e9e9e;
}
.error-card {
  text-align: center;
  padding: 60px;
}

@media print {
  .no-print {
    display: none !important;
  }
  body * {
    visibility: hidden;
  }
  .fatura-kagit,
  .fatura-kagit * {
    visibility: visible;
  }
  .fatura-kagit {
    position: absolute;
    left: 0;
    top: 0;
    box-shadow: none;
    padding: 20px;
    max-width: 100%;
    border: none;
    border-radius: 0;
  }
  .fatura-detay {
    padding: 0;
    background: white;
  }
  .durum-badge.kesildi {
    border: 1px solid #2e7d32;
  }
}
</style>
