<template>
  <div class="raporlar-container">
    <h1>Raporlar</h1>

    <TabView>
      <TabPanel header="Cari Ekstre">
        <div class="rapor-filtre">
          <div class="form-group">
            <label>Cari Hesap</label>
            <Dropdown v-model="ekstreCariId" :options="cariHesapStore.cariHesaplar"
              option-label="ad" option-value="id" placeholder="Seçiniz" class="w-full" />
          </div>
          <div class="form-group">
            <label>Başlangıç</label>
            <DatePicker v-model="ekstreBas" date-format="dd.mm.yy" class="w-full" />
          </div>
          <div class="form-group">
            <label>Bitiş</label>
            <DatePicker v-model="ekstreBit" date-format="dd.mm.yy" class="w-full" />
          </div>
          <div class="form-group filtre-btn">
            <label>&nbsp;</label>
            <Button label="Rapor Getir" icon="pi pi-search" @click="getCariEkstre" :loading="ekstreLoading" />
          </div>
        </div>

        <div v-if="ekstreData" class="rapor-sonuc">
          <div class="rapor-bilgi">
            <h3>{{ ekstreData.cariAd }}</h3>
            <p>Dönem Başı Bakiye: <strong :class="ekstreData.donemBasBakiye >= 0 ? 'positive' : 'negative'">{{ formatCurrency(ekstreData.donemBasBakiye) }}</strong></p>
            <p>Dönem Sonu Bakiye: <strong :class="ekstreData.donemSonBakiye >= 0 ? 'positive' : 'negative'">{{ formatCurrency(ekstreData.donemSonBakiye) }}</strong></p>
          </div>
          <DataTable :value="ekstreData.hareketler" striped-rows :rows="10" :paginator="true"
            paginator-template="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink CurrentPageReport">
            <Column field="hareketTarihi" header="Tarih" style="width:100px">
              <template #body="s">{{ formatDate(s.data.hareketTarihi) }}</template>
            </Column>
            <Column field="tur" header="Tür" style="width:90px">
              <template #body="s">
                <span :class="['badge', s.data.tur === 'TAHSILAT' ? 'tahsilat' : 'odeme']">
                  {{ s.data.tur === 'TAHSILAT' ? 'Tahsilat' : 'Ödeme' }}
                </span>
              </template>
            </Column>
            <Column field="tutar" header="Tutar" style="width:120px">
              <template #body="s">
                <span :class="s.data.tur === 'TAHSILAT' ? 'positive' : 'negative'">{{ formatCurrency(s.data.tutar) }}</span>
              </template>
            </Column>
            <Column field="aciklama" header="Açıklama"></Column>
          </DataTable>
          <Message v-if="ekstreData.hareketler.length === 0" severity="info" text="Bu dönemde hareket bulunmamaktadır." />
        </div>
      </TabPanel>

      <TabPanel header="Gelir/Gider Özeti">
        <div class="rapor-filtre">
          <div class="form-group">
            <label>Başlangıç</label>
            <DatePicker v-model="ggBas" date-format="dd.mm.yy" class="w-full" />
          </div>
          <div class="form-group">
            <label>Bitiş</label>
            <DatePicker v-model="ggBit" date-format="dd.mm.yy" class="w-full" />
          </div>
          <div class="form-group filtre-btn">
            <label>&nbsp;</label>
            <Button label="Rapor Getir" icon="pi pi-search" @click="getGelirGider" :loading="ggLoading" />
          </div>
        </div>

        <div v-if="ggData" class="rapor-sonuc">
          <div class="ozet-kartlar">
            <div class="ozet-kart gelir"><span>Toplam Gelir</span><strong>{{ formatCurrency(ggData.toplamGelir) }}</strong></div>
            <div class="ozet-kart gider"><span>Toplam Gider</span><strong>{{ formatCurrency(ggData.toplamGider) }}</strong></div>
            <div class="ozet-kart" :class="ggData.netKarZarar >= 0 ? 'kar' : 'zarar'">
              <span>Net Kar/Zarar</span><strong>{{ formatCurrency(ggData.netKarZarar) }}</strong>
            </div>
          </div>

          <h3 style="margin-top:25px">Aylık Dağılım</h3>
          <DataTable :value="ggData.aylikDagilim" striped-rows>
            <Column field="ay" header="Ay"></Column>
            <Column field="net" header="Net Tutar">
              <template #body="s">
                <span :class="s.data.net >= 0 ? 'positive' : 'negative'">{{ formatCurrency(s.data.net) }}</span>
              </template>
            </Column>
          </DataTable>
        </div>
      </TabPanel>

      <TabPanel header="KDV Raporu">
        <div class="rapor-filtre">
          <div class="form-group">
            <label>Başlangıç</label>
            <DatePicker v-model="kdvBas" date-format="dd.mm.yy" class="w-full" />
          </div>
          <div class="form-group">
            <label>Bitiş</label>
            <DatePicker v-model="kdvBit" date-format="dd.mm.yy" class="w-full" />
          </div>
          <div class="form-group filtre-btn">
            <label>&nbsp;</label>
            <Button label="Rapor Getir" icon="pi pi-search" @click="getKdv" :loading="kdvLoading" />
          </div>
        </div>

        <div v-if="kdvData" class="rapor-sonuc">
          <div class="ozet-kartlar">
            <div class="ozet-kart gelir"><span>Çıkış KDV (Satış)</span><strong>{{ formatCurrency(kdvData.toplamKdvCikis) }}</strong></div>
            <div class="ozet-kart gider"><span>Giriş KDV (Alış)</span><strong>{{ formatCurrency(kdvData.toplamKdvGiris) }}</strong></div>
            <div class="ozet-kart" :class="kdvData.kdvFarki >= 0 ? 'kar' : 'zarar'">
              <span>KDV Farkı</span><strong>{{ formatCurrency(kdvData.kdvFarki) }}</strong>
            </div>
          </div>
        </div>
      </TabPanel>

      <TabPanel header="Yaşlandırma">
        <div class="rapor-filtre">
          <Button label="Rapor Getir" icon="pi pi-search" @click="getYaslandirma" :loading="yasLoading" />
        </div>
        <div v-if="yasData" class="rapor-sonuc">
          <DataTable :value="yasData" striped-rows :rows="10" :paginator="true"
            paginator-template="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink CurrentPageReport">
            <Column field="cariAd" header="Cari Hesap"></Column>
            <Column field="bakiye" header="Borç Bakiyesi" style="width:140px">
              <template #body="s"><span class="negative">{{ formatCurrency(s.data.bakiye) }}</span></template>
            </Column>
            <Column field="gun" header="Gün" style="width:80px"></Column>
            <Column field="aralik" header="Vade Aralığı" style="width:130px">
              <template #body="s">
                <span :class="['vade-badge', vadeClass(s.data.aralik)]">{{ s.data.aralik }}</span>
              </template>
            </Column>
          </DataTable>
          <Message v-if="yasData.length === 0" severity="info" text="Borçlu cari hesap bulunmamaktadır." />
        </div>
      </TabPanel>
    </TabView>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useToast } from 'primevue/usetoast'
import { useCariHesapStore } from '../stores/cariHesapStore.js'
import { raporAPI } from '../api/index.js'
const toast = useToast()

const cariHesapStore = useCariHesapStore()

const ekstreCariId = ref(null)
const ekstreBas = ref(new Date(new Date().getFullYear(), 0, 1))
const ekstreBit = ref(new Date())
const ekstreData = ref(null)
const ekstreLoading = ref(false)

const ggBas = ref(new Date(new Date().getFullYear(), 0, 1))
const ggBit = ref(new Date())
const ggData = ref(null)
const ggLoading = ref(false)

const kdvBas = ref(new Date(new Date().getFullYear(), 0, 1))
const kdvBit = ref(new Date())
const kdvData = ref(null)
const kdvLoading = ref(false)

const yasData = ref(null)
const yasLoading = ref(false)

onMounted(async () => {
  await cariHesapStore.getAllCariHesaplar()
})

const getCariEkstre = async () => {
  if (!ekstreCariId.value) return
  ekstreLoading.value = true
  try {
    const r = await raporAPI.cariEkstre({
      cariHesapId: ekstreCariId.value,
      baslangic: ekstreBas.value.toISOString().split('T')[0],
      bitis: ekstreBit.value.toISOString().split('T')[0]
    })
    ekstreData.value = r.data
  } catch (err) { toast.add({ severity: 'error', summary: 'Hata', detail: err?.response?.data?.message || err?.message || 'Cari ekstre yüklenirken hata oluştu', life: 5000 }); ekstreData.value = null }
  finally { ekstreLoading.value = false }
}

const getGelirGider = async () => {
  ggLoading.value = true
  try {
    const r = await raporAPI.gelirGider({
      baslangic: ggBas.value.toISOString().split('T')[0],
      bitis: ggBit.value.toISOString().split('T')[0]
    })
    ggData.value = r.data
  } catch (err) { toast.add({ severity: 'error', summary: 'Hata', detail: err?.response?.data?.message || err?.message || 'Gelir/gider raporu yüklenirken hata oluştu', life: 5000 }); ggData.value = null }
  finally { ggLoading.value = false }
}

const getKdv = async () => {
  kdvLoading.value = true
  try {
    const r = await raporAPI.kdv({
      baslangic: kdvBas.value.toISOString().split('T')[0],
      bitis: kdvBit.value.toISOString().split('T')[0]
    })
    kdvData.value = r.data
  } catch (err) { toast.add({ severity: 'error', summary: 'Hata', detail: err?.response?.data?.message || err?.message || 'KDV raporu yüklenirken hata oluştu', life: 5000 }); kdvData.value = null }
  finally { kdvLoading.value = false }
}

const getYaslandirma = async () => {
  yasLoading.value = true
  try { const r = await raporAPI.yaslandirma(); yasData.value = r.data }
  catch (err) { toast.add({ severity: 'error', summary: 'Hata', detail: err?.response?.data?.message || err?.message || 'Yaşlandırma raporu yüklenirken hata oluştu', life: 5000 }); yasData.value = null }
  finally { yasLoading.value = false }
}

const vadeClass = (aralik) => {
  if (aralik.startsWith('0')) return 'risk-yok'
  if (aralik.startsWith('31')) return 'risk-az'
  if (aralik.startsWith('61')) return 'risk-orta'
  return 'risk-yuksek'
}

const formatCurrency = (v) => v ?? 0
  ? new Intl.NumberFormat('tr-TR', { style: 'currency', currency: 'TRY' }).format(v)
  : '0,00 ₺'

const formatDate = (d) => d
  ? new Intl.DateTimeFormat('tr-TR', { year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date(d))
  : '-'
</script>

<style scoped>
.raporlar-container { padding: 20px; }
h1 { color: var(--text-primary); margin-bottom: 20px; font-size: 28px; font-weight: 700; letter-spacing: -0.5px; }
.rapor-filtre { display: flex; gap: 15px; align-items: flex-end; flex-wrap: wrap; margin-bottom: 20px; background: var(--bg-card); border: 1px solid var(--border); padding: 20px; border-radius: 12px; }
.form-group { min-width: 200px; }
.form-group label { display: block; margin-bottom: 6px; font-weight: bold; color: #333; font-size: 13px; }
.filtre-btn { min-width: auto; }
.rapor-sonuc { margin-top: 20px; }
.rapor-bilgi { background: #f8f9fa; padding: 15px; border-radius: 8px; margin-bottom: 15px; }
.rapor-bilgi h3 { margin: 0 0 10px 0; color: #1976d2; }
.rapor-bilgi p { margin: 5px 0; }
.ozet-kartlar { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 15px; }
.ozet-kart { background: var(--bg-card); padding: 20px; border-radius: 14px; border: 1px solid var(--border); text-align: center; }
.ozet-kart span { display: block; font-size: 13px; color: #666; margin-bottom: 8px; }
.ozet-kart strong { font-size: 22px; }
.ozet-kart.gelir strong { color: #4caf50; }
.ozet-kart.gider strong { color: #f44336; }
.ozet-kart.kar strong { color: #4caf50; }
.ozet-kart.zarar strong { color: #f44336; }
.badge { padding: 4px 12px; border-radius: 20px; font-size: 12px; font-weight: bold; }
.badge.tahsilat { background: #e8f5e9; color: #2e7d32; }
.badge.odeme { background: #ffebee; color: #c62828; }
.positive { color: #4caf50; font-weight: bold; }
.negative { color: #f44336; font-weight: bold; }
.vade-badge { padding: 4px 12px; border-radius: 20px; font-size: 12px; font-weight: bold; }
.risk-yok { background: #e8f5e9; color: #2e7d32; }
.risk-az { background: #fff3e0; color: #e65100; }
.risk-orta { background: #ffebee; color: #c62828; }
.risk-yuksek { background: #fce4ec; color: #880e4f; }
.w-full { width: 100% !important; }
</style>
