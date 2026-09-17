<template>
  <div class="tahsilat-container">
    <div class="sayfa-baslik">
      <h1><i class="pi pi-money-bill" /> {{ t('tahsilat.title') }}</h1>
      <p class="aciklama">
        {{ t('tahsilat.aciklama') }}
      </p>
      <Button
        :label="t('tahsilat.tumBorclularaWhatsapp')"
        icon="pi pi-whatsapp"
        class="p-button-success"
        :disabled="!borcluCariler.length"
        @click="tumuWhatsapp"
      />
    </div>

    <div
      v-if="yukleniyor"
      class="yukleniyor"
    >
      <i class="pi pi-spin pi-spinner" /> {{ t('common.loading') }}
    </div>

    <template v-else>
      <div class="ozet-grid">
        <div class="ozet-kart">
          <div class="ozet-ikon toplam">
            <i class="pi pi-wallet" />
          </div>
          <div>
            <span class="ozet-etiket">{{ t('tahsilat.toplamAlacak') }}</span>
            <strong class="ozet-deger"><span class="gizli-veri">{{ formatCurrency(ozet?.toplamAlacak || 0) }}</span></strong>
          </div>
        </div>
        <div class="ozet-kart">
          <div class="ozet-ikon gecmis">
            <i class="pi pi-exclamation-triangle" />
          </div>
          <div>
            <span class="ozet-etiket">{{ t('tahsilat.vadesiGecmis') }}</span>
            <strong class="ozet-deger text-red-600"><span class="gizli-veri">{{ formatCurrency(ozet?.vadesiGecmisToplam || 0) }}</span></strong>
          </div>
        </div>
        <div class="ozet-kart">
          <div class="ozet-ikon yaklasan">
            <i class="pi pi-calendar-clock" />
          </div>
          <div>
            <span class="ozet-etiket">{{ t('tahsilat.otuzGunIcinde') }}</span>
            <strong class="ozet-deger text-amber-600"><span class="gizli-veri">{{ formatCurrency(ozet?.vadesiYaklasanToplam || 0) }}</span></strong>
          </div>
        </div>
        <div class="ozet-kart">
          <div class="ozet-ikon cari">
            <i class="pi pi-users" />
          </div>
          <div>
            <span class="ozet-etiket">{{ t('tahsilat.gecikmisCariAcikFatura') }}</span>
            <strong class="ozet-deger">{{ ozet?.gecikmisCariSayisi || 0 }} / {{ ozet?.acikFaturaSayisi || 0 }}</strong>
          </div>
        </div>
      </div>

      <Toolbar class="toolbar">
        <template #start>
          <span class="filtre-etiket">{{ t('tahsilat.yaslandirmaSirali') }}</span>
        </template>
        <template #end>
          <Button
            icon="pi pi-money-bill"
            :label="t('tahsilat.tahsilatGir')"
            class="p-button-success p-button-sm"
            :disabled="!(ozet?.cariler || []).length"
            @click="tahsilatGir()"
          />
          <Button
            icon="pi pi-refresh"
            :label="t('tahsilat.yenile')"
            class="p-button-outlined p-button-sm"
            @click="yukle"
          />
        </template>
      </Toolbar>

      <div class="table-container">
        <!-- eslint-disable vue/attribute-hyphenation -->
        <AppDataTable
          v-model:expandedRows="genisletilenler"
          :value="ozet?.cariler || []"
          data-key="cariId"
          striped-rows
          :paginator="false"
        >
          <Column
            expander
            style="width: 3rem"
          />
          <Column
            field="cariAd"
            :header="t('tahsilat.cariHesap')"
            sortable
          >
            <template #body="{ data }">
              <div class="cari-hucre">
                <strong>{{ data.cariAd }}</strong>
                <span class="cari-alt">{{ t('tahsilat.acikFatura', { n: data.faturaSayisi }) }}</span>
              </div>
            </template>
          </Column>
          <Column
            :header="t('tahsilat.toplamAlacak')"
            sortable
          >
            <template #body="{ data }">
              <strong class="text-primary dark:text-gray-100"><span class="gizli-veri">{{ formatCurrency(data.toplamAlacak) }}</span></strong>
            </template>
          </Column>
          <Column
            :header="t('tahsilat.gecikmis')"
            sortable
          >
            <template #body="{ data }">
              <span
                :class="data.gecikmisAlacak > 0 ? 'text-red-600 font-bold' : 'text-muted'"
              ><span class="gizli-veri">{{ formatCurrency(data.gecikmisAlacak) }}</span></span>
            </template>
          </Column>
          <Column
            :header="t('tahsilat.yaslandirma')"
            sortable
          >
            <template #body="{ data }">
              <Tag
                :value="data.aralik"
                :severity="aralikSeverity(data.aralik)"
              />
            </template>
          </Column>
          <Column
            :header="t('tahsilat.islem')"
            style="width: 90px"
          >
            <template #body="{ data }">
              <div class="islem-grup">
                <Button
                  icon="pi pi-money-bill"
                  class="p-button-rounded p-button-text p-button-success"
                  :title="t('tahsilat.tahsilatGir')"
                  @click="tahsilatGir(data)"
                />
                <SatirEylemleri
                  :gorunur="{ duzenle: false, cogalt: false, sil: false }"
                  :items="tahsilatEylemleri(data)"
                />
              </div>
            </template>
          </Column>

          <template #expansion="slotProps">
            <div class="fatura-liste">
              <div
                v-for="f in slotProps.data.faturalar"
                :key="f.faturaId"
                class="fatura-satir"
              >
                <div class="fatura-no">
                  <i class="pi pi-file" /> {{ f.faturaNumarasi }}
                </div>
                <div class="fatura-vade">
                  {{ t('tahsilat.vade') }}: {{ formatDate(f.vadeTarihi) }}
                  <span
                    v-if="f.gecikmeGunu > 0"
                    class="gecikme-pill"
                  >{{ t('tahsilat.gunGecikti', { n: f.gecikmeGunu }) }}</span>
                  <span
                    v-else
                    class="vade-pill"
                  >{{ t('tahsilat.gunKaldi', { n: Math.abs(f.gecikmeGunu) }) }}</span>
                </div>
                <div class="fatura-tutar">
                  <span class="gizli-veri">{{ formatCurrency(f.kalanTutar) }}</span>
                </div>
              </div>
              <div
                v-if="!slotProps.data.faturalar?.length"
                class="bos-satir"
              >
                {{ t('tahsilat.faturaDetayiYok') }}
              </div>
            </div>
          </template>
          <template #empty>
            <EmptyState
              v-if="!(ozet?.cariler || []).length"
              icon="pi pi-check-circle"
              :message="t('tahsilat.acikAlacakYok')"
              :sub-message="t('tahsilat.acikAlacakYokHint')"
            />
          </template>
        </AppDataTable>
        <!-- eslint-enable vue/attribute-hyphenation -->
      </div>
    </template>

    <div class="gecmis-bolum">
      <div class="gecmis-baslik">
        <span><i class="pi pi-history" /> {{ t('tahsilat.tahsilatGecmisi') }}</span>
        <Button
          icon="pi pi-refresh"
          class="p-button-sm p-button-text"
          :loading="gecmisYukleniyor"
          @click="gecmisYukle"
        />
      </div>
      <div class="table-container">
        <AppDataTable
          :value="gecmis"
          :loading="gecmisYukleniyor"
          striped-rows
          :paginator="true"
          :rows="10"
          paginator-template="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink CurrentPageReport"
          gorunum-anahtari="tahsilat-gecmis"
        >
          <Column :header="t('common.date')">
            <template #body="s">
              {{ formatDate(s.data.hareketTarihi) }}
            </template>
          </Column>
          <Column
            field="cariHesapAd"
            :header="t('tahsilat.cariHesap')"
            sortable
          />
          <Column :header="t('common.amount')">
            <template #body="s">
              <strong class="text-green-500"><span class="gizli-veri">{{ formatCurrency(s.data.tutar) }}</span></strong>
            </template>
          </Column>
          <Column :header="t('tahsilat.odemeYontemi')">
            <template #body="s">
              <Tag
                v-if="s.data.odemeYontemi"
                :value="odemeYontemiLabel(s.data.odemeYontemi)"
                :severity="odemeYontemiSeverity(s.data.odemeYontemi)"
              />
              <span
                v-else
                class="text-muted"
              >-</span>
            </template>
          </Column>
          <Column :header="t('tahsilat.taksitBilgisi')">
            <template #body="s">
              <span v-if="s.data.odemeYontemi === 'TAKSIT'">
                {{ s.data.taksitKurum || '-' }}
                <small
                  v-if="s.data.taksitTutar"
                  class="text-muted"
                > (<span class="gizli-veri">{{ formatCurrency(s.data.taksitTutar) }}</span>)</small>
              </span>
              <span
                v-else-if="s.data.posAd"
                class="pos-bilgi"
              >
                <i class="pi pi-credit-card" /> {{ s.data.posAd }}
                <small
                  v-if="s.data.komisyonTutar"
                  class="text-muted"
                > · {{ t('tahsilat.kom') }}: <span class="gizli-veri">{{ formatCurrency(s.data.komisyonTutar) }}</span></small>
                <small
                  v-if="s.data.valorTarihi"
                  class="text-muted"
                > · {{ t('tahsilat.valor') }}: {{ s.data.valorTarihi }}</small>
              </span>
              <span
                v-else
                class="text-muted"
              >-</span>
            </template>
          </Column>
          <Column :header="t('common.description')">
            <template #body="s">
              <span class="text-muted">{{ s.data.aciklama || '-' }}</span>
            </template>
          </Column>
          <template #empty>
            <EmptyState
              v-if="!gecmisYukleniyor && !gecmis.length"
              icon="pi pi-history"
              :message="t('tahsilat.gecmisYok')"
              :sub-message="t('tahsilat.gecmisYokHint')"
            />
          </template>
        </AppDataTable>
      </div>
    </div>

    <TahsilatGirDialog
      v-model:visible="tahsilatDialogAcik"
      :cariler="dialogCariler"
      :baslangic-cari-id="seciliCariId"
      @kaydedildi="kaydetSonrasi"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { unwrapList } from '../api/utils/unwrap.js'
import { tahsilatAPI, cariHesapAPI } from '../api/index.js'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { formatCurrency, formatDate } from '../utils/format.js'
import TahsilatGirDialog from '../components/TahsilatGirDialog.vue'
import { useI18n } from 'vue-i18n'

const toastBildirim = useToastBildirim()
const { t } = useI18n()
const yukleniyor = ref(false)
const ozet = ref(null)
const genisletilenler = ref([])
const tahsilatDialogAcik = ref(false)
const seciliCariId = ref(null)
const tumCariler = ref([])

// Faturası olmayan cariler için de (avans/genel tahsilat) seçim listesi.
const dialogCariler = computed(() => {
  const mevcut = (ozet.value?.cariler || []).map((c) => ({ ...c, cariId: c.cariId || c.id }))
  const idler = new Set(mevcut.map((c) => c.cariId))
  const ek = tumCariler.value
    .filter((c) => !idler.has(c.id))
    .map((c) => ({ cariId: c.id, ad: c.ad, faturalar: [], toplamAlacak: 0 }))
  return [...mevcut, ...ek]
})

const gecmis = ref([])
const gecmisYukleniyor = ref(false)

const tahsilatEylemleri = (d) => {
  const items = []
  if (d.email) {
    items.push({ etiket: t('tahsilat.epostaIleHatirlat'), ikon: 'pi pi-envelope', islem: () => hatirlat(d) })
  }
  if (d.telefon) {
    items.push({ etiket: t('tahsilat.whatsappAc'), ikon: 'pi pi-whatsapp', islem: () => whatsappAc(d) })
    items.push({ etiket: t('tahsilat.ara'), ikon: 'pi pi-phone', islem: () => ara(d) })
  }
  return items
}

const tahsilatGir = (cari) => {
  seciliCariId.value = cari?.cariId || null
  tahsilatDialogAcik.value = true
}

const odemeYontemiLabel = (y) => ({
  NAKIT: t('tahsilat.nakit'),
  KART: t('tahsilat.kart'),
  TAKSIT: t('tahsilat.taksit'),
  HAVALE: t('tahsilat.havale')
}[y] || y)

const odemeYontemiSeverity = (y) => ({
  NAKIT: 'success',
  KART: 'info',
  TAKSIT: 'warn',
  HAVALE: 'secondary'
}[y] || 'info')

const gecmisYukle = async () => {
  gecmisYukleniyor.value = true
  try {
    const r = await tahsilatAPI.gecmis({ size: 50 })
    gecmis.value = unwrapList(r)
  } catch {
    gecmis.value = []
  } finally {
    gecmisYukleniyor.value = false
  }
}

const kaydetSonrasi = () => {
  yukle()
  gecmisYukle()
}

const aralikSeverity = (aralik) => {
  if (!aralik) return 'info'
  if (aralik === 'Vadesi Gelmemiş') return 'success'
  if (aralik === '90+ Gün') return 'danger'
  if (aralik === '61-90 Gün') return 'danger'
  return 'warning'
}

const yukle = async () => {
  yukleniyor.value = true
  try {
    const r = await tahsilatAPI.ozet()
    ozet.value = r.data
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('tahsilat.ozetYuklenemedi'))
  } finally {
    yukleniyor.value = false
  }
}

const hatirlat = async (cari) => {
  try {
    const r = await tahsilatAPI.hatirlat(cari.cariId)
    toastBildirim.basarili(t('tahsilat.hatirlatmaGonderildi', { ad: cari.cariAd, n: r.data?.gonderilen || 0 }))
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('tahsilat.hatirlatmaGonderilemedi'))
  }
}

const telefonNormalle = (telefon) => {
  let no = String(telefon || '').replace(/\D/g, '')
  if (no.startsWith('0')) no = no.slice(1)
  if (no.length === 10) no = '90' + no
  return no
}

const whatsappAc = (cari) => {
  const no = telefonNormalle(cari.telefon)
  const mesaj = encodeURIComponent(
    t('tahsilat.whatsappMesaj', { ad: cari.cariAd, tutar: formatCurrency(cari.toplamAlacak) })
  )
  window.open(`https://wa.me/${no}?text=${mesaj}`, '_blank')
}

const borcluCariler = computed(() => (ozet.value?.cariler || []).filter((c) => c.telefon && c.toplamAlacak > 0))

const tumuWhatsapp = () => {
  borcluCariler.value.forEach((c, i) => {
    setTimeout(() => whatsappAc(c), i * 400)
  })
  toastBildirim.bilgi(t('tahsilat.whatsappAciliyor', { n: borcluCariler.value.length }))
}

const ara = (cari) => {
  window.location.href = `tel:${cari.telefon}`
}

onMounted(async () => {
  yukle()
  gecmisYukle()
  try {
    const r = await cariHesapAPI.getAll({ size: 1000 })
    tumCariler.value = unwrapList(r)
  } catch {
    tumCariler.value = []
  }
})
</script>

<style scoped>
.tahsilat-container {
  padding: 0.5rem 0;
}
.gecmis-bolum {
  margin-top: 28px;
}
.gecmis-baslik {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  font-size: 15px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 12px;
}
.gecmis-baslik span {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}
.gecmis-baslik i {
  color: var(--accent);
}
.sayfa-baslik h1 {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0 0 4px;
  font-size: 1.4rem;
}
.sayfa-baslik .aciklama {
  color: var(--text-muted);
  font-size: 13px;
  margin: 0 0 16px;
}
.yukleniyor {
  padding: 40px;
  text-align: center;
  color: var(--text-muted);
}
.ozet-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(220px, 100%), 1fr));
  gap: 14px;
  margin-bottom: 16px;
}
.ozet-kart {
  display: flex;
  align-items: center;
  gap: 14px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 16px;
  box-shadow: var(--shadow);
}
.ozet-ikon {
  width: 46px;
  height: 46px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  flex-shrink: 0;
}
.ozet-ikon.toplam {
  background: var(--accent-soft-strong);
  color: var(--accent);
}
.ozet-ikon.gecmis {
  background: rgba(239, 68, 68, 0.15);
  color: #ef4444;
}
.ozet-ikon.yaklasan {
  background: rgba(245, 158, 11, 0.15);
  color: #f59e0b;
}
.ozet-ikon.cari {
  background: rgba(16, 185, 129, 0.15);
  color: #10b981;
}
.ozet-etiket {
  display: block;
  font-size: 12px;
  color: var(--text-muted);
  margin-bottom: 2px;
}
.ozet-deger {
  font-size: 1.25rem;
  font-weight: 700;
}
.toolbar {
  margin-bottom: 14px;
}
.filtre-etiket {
  font-size: 13px;
  color: var(--text-muted);
}
.cari-hucre {
  display: flex;
  flex-direction: column;
}
.cari-alt {
  font-size: 12px;
  color: var(--text-muted);
}
.islem-grup {
  display: flex;
  flex-wrap: wrap;
  gap: 2px;
}
.fatura-liste {
  padding: 8px 16px;
  background: var(--bg-secondary, #f8fafc);
  border-radius: 8px;
}
.fatura-satir {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 8px 0;
  border-bottom: 1px solid var(--border);
  font-size: 13px;
  flex-wrap: wrap;
}
.fatura-satir:last-child {
  border-bottom: none;
}
.fatura-no {
  font-weight: 600;
  min-width: min(140px, 100%);
}
.fatura-vade {
  flex: 1;
  color: var(--text-muted);
}
.fatura-tutar {
  font-weight: 700;
  color: var(--text-primary);
}
.gecikme-pill {
  margin-left: 8px;
  background: rgba(239, 68, 68, 0.15);
  color: #ef4444;
  font-size: 11px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 10px;
}
.vade-pill {
  margin-left: 8px;
  background: rgba(16, 185, 129, 0.15);
  color: #10b981;
  font-size: 11px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 10px;
}
.bos-satir {
  color: var(--text-muted);
  padding: 8px 0;
}

@media (max-width: 600px) {
  .fatura-satir {
    flex-wrap: wrap;
    gap: 6px;
  }
  .fatura-no {
    min-width: 100%;
  }
}
</style>
