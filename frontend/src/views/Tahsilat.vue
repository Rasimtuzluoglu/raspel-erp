<template>
  <div class="tahsilat-container">
    <div class="sayfa-baslik">
      <h1><i class="pi pi-money-bill" /> Tahsilat Merkezi</h1>
      <p class="aciklama">
        Ödenmemiş alacaklarınızı tek ekrandan takip edin, yaşlandırma analizi yapın ve müşterilerinize tek tıkla
        hatırlatma gönderin.
      </p>
    </div>

    <div
      v-if="yukleniyor"
      class="yukleniyor"
    >
      <i class="pi pi-spin pi-spinner" /> Yükleniyor...
    </div>

    <template v-else>
      <div class="ozet-grid">
        <div class="ozet-kart">
          <div class="ozet-ikon toplam">
            <i class="pi pi-wallet" />
          </div>
          <div>
            <span class="ozet-etiket">Toplam Alacak</span>
            <strong class="ozet-deger">{{ formatCurrency(ozet?.toplamAlacak || 0) }}</strong>
          </div>
        </div>
        <div class="ozet-kart">
          <div class="ozet-ikon gecmis">
            <i class="pi pi-exclamation-triangle" />
          </div>
          <div>
            <span class="ozet-etiket">Vadesi Geçmiş</span>
            <strong class="ozet-deger text-red-600">{{ formatCurrency(ozet?.vadesiGecmisToplam || 0) }}</strong>
          </div>
        </div>
        <div class="ozet-kart">
          <div class="ozet-ikon yaklasan">
            <i class="pi pi-calendar-clock" />
          </div>
          <div>
            <span class="ozet-etiket">30 Gün İçinde Vadesi Gelecek</span>
            <strong class="ozet-deger text-amber-600">{{ formatCurrency(ozet?.vadesiYaklasanToplam || 0) }}</strong>
          </div>
        </div>
        <div class="ozet-kart">
          <div class="ozet-ikon cari">
            <i class="pi pi-users" />
          </div>
          <div>
            <span class="ozet-etiket">Gecikmiş Cari / Açık Fatura</span>
            <strong class="ozet-deger">{{ ozet?.gecikmisCariSayisi || 0 }} / {{ ozet?.acikFaturaSayisi || 0 }}</strong>
          </div>
        </div>
      </div>

      <Toolbar class="toolbar">
        <template #start>
          <span class="filtre-etiket">Alacak yaşlandırmasına göre sıralı</span>
        </template>
        <template #end>
          <Button
            icon="pi pi-money-bill"
            label="Tahsilat Gir"
            class="p-button-success p-button-sm"
            :disabled="!(ozet?.cariler || []).length"
            @click="tahsilatGir()"
          />
          <Button
            icon="pi pi-refresh"
            label="Yenile"
            class="p-button-outlined p-button-sm"
            @click="yukle"
          />
        </template>
      </Toolbar>

      <div class="table-container">
        <!-- eslint-disable vue/attribute-hyphenation -->
        <DataTable
          v-model:expandedRows="genisletilenler"
          :value="ozet?.cariler || []"
          data-key="cariId"
          striped-rows
        >
          <Column
            expander
            style="width: 3rem"
          />
          <Column
            field="cariAd"
            header="Cari Hesap"
            sortable
          >
            <template #body="{ data }">
              <div class="cari-hucre">
                <strong>{{ data.cariAd }}</strong>
                <span class="cari-alt">{{ data.faturaSayisi }} açık fatura</span>
              </div>
            </template>
          </Column>
          <Column
            header="Toplam Alacak"
            sortable
          >
            <template #body="{ data }">
              <strong class="text-primary dark:text-gray-100">{{ formatCurrency(data.toplamAlacak) }}</strong>
            </template>
          </Column>
          <Column
            header="Gecikmiş"
            sortable
          >
            <template #body="{ data }">
              <span
                :class="data.gecikmisAlacak > 0 ? 'text-red-600 font-bold' : 'text-muted'"
              >{{ formatCurrency(data.gecikmisAlacak) }}</span>
            </template>
          </Column>
          <Column
            header="Yaşlandırma"
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
            header="İşlem"
            style="width: 200px"
          >
            <template #body="{ data }">
              <div class="islem-grup">
                <Button
                  icon="pi pi-money-bill"
                  class="p-button-rounded p-button-text p-button-success"
                  title="Tahsilat Gir"
                  @click="tahsilatGir(data)"
                />
                <Button
                  icon="pi pi-envelope"
                  class="p-button-rounded p-button-text p-button-primary"
                  :title="data.email ? 'E-posta ile hatırlat' : 'E-posta tanımlı değil'"
                  :disabled="!data.email"
                  @click="hatirlat(data)"
                />
                <Button
                  v-if="data.telefon"
                  icon="pi pi-whatsapp"
                  class="p-button-rounded p-button-text p-button-success"
                  title="WhatsApp'ta aç"
                  @click="whatsappAc(data)"
                />
                <Button
                  v-if="data.telefon"
                  icon="pi pi-phone"
                  class="p-button-rounded p-button-text p-button-info"
                  title="Ara"
                  @click="ara(data)"
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
                  Vade: {{ formatDate(f.vadeTarihi) }}
                  <span
                    v-if="f.gecikmeGunu > 0"
                    class="gecikme-pill"
                  >{{ f.gecikmeGunu }} gün gecikti</span>
                  <span
                    v-else
                    class="vade-pill"
                  >{{ Math.abs(f.gecikmeGunu) }} gün kaldı</span>
                </div>
                <div class="fatura-tutar">
                  {{ formatCurrency(f.kalanTutar) }}
                </div>
              </div>
              <div
                v-if="!slotProps.data.faturalar?.length"
                class="bos-satir"
              >
                Fatura detayı bulunmuyor.
              </div>
            </div>
          </template>
        </DataTable>
        <!-- eslint-enable vue/attribute-hyphenation -->
      </div>

      <EmptyState
        v-if="!(ozet?.cariler || []).length"
        icon="pi pi-check-circle"
        message="Açık alacak yok"
        sub-message="Şu anda ödenmemiş alacağınız bulunmuyor. Harika!"
      />
    </template>

    <div class="gecmis-bolum">
      <div class="gecmis-baslik">
        <span><i class="pi pi-history" /> Tahsilat Geçmişi</span>
        <Button
          icon="pi pi-refresh"
          class="p-button-sm p-button-text"
          :loading="gecmisYukleniyor"
          @click="gecmisYukle"
        />
      </div>
      <div class="table-container">
        <DataTable
          :value="gecmis"
          :loading="gecmisYukleniyor"
          striped-rows
          :paginator="true"
          :rows="10"
          paginator-template="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink CurrentPageReport"
        >
          <Column header="Tarih">
            <template #body="s">
              {{ formatDate(s.data.hareketTarihi) }}
            </template>
          </Column>
          <Column
            field="cariHesapAd"
            header="Cari Hesap"
            sortable
          />
          <Column header="Tutar">
            <template #body="s">
              <strong class="text-green-500">{{ formatCurrency(s.data.tutar) }}</strong>
            </template>
          </Column>
          <Column header="Ödeme Yöntemi">
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
          <Column header="Taksit Bilgisi">
            <template #body="s">
              <span v-if="s.data.odemeYontemi === 'TAKSIT'">
                {{ s.data.taksitKurum || '-' }}
                <small
                  v-if="s.data.taksitTutar"
                  class="text-muted"
                > ({{ formatCurrency(s.data.taksitTutar) }})</small>
              </span>
              <span
                v-else-if="s.data.posAd"
                class="pos-bilgi"
              >
                <i class="pi pi-credit-card" /> {{ s.data.posAd }}
                <small
                  v-if="s.data.komisyonTutar"
                  class="text-muted"
                > · Kom: {{ formatCurrency(s.data.komisyonTutar) }}</small>
                <small
                  v-if="s.data.valorTarihi"
                  class="text-muted"
                > · Valör: {{ s.data.valorTarihi }}</small>
              </span>
              <span
                v-else
                class="text-muted"
              >-</span>
            </template>
          </Column>
          <Column header="Açıklama">
            <template #body="s">
              <span class="text-muted">{{ s.data.aciklama || '-' }}</span>
            </template>
          </Column>
        </DataTable>
        <EmptyState
          v-if="!gecmisYukleniyor && !gecmis.length"
          icon="pi pi-history"
          message="Tahsilat geçmişi yok"
          sub-message="Henüz tahsilat kaydı yapılmamış."
        />
      </div>
    </div>

    <TahsilatGirDialog
      v-model:visible="tahsilatDialogAcik"
      :cariler="ozet?.cariler || []"
      :baslangic-cari-id="seciliCariId"
      @kaydedildi="kaydetSonrasi"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { tahsilatAPI } from '../api/index.js'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { formatCurrency, formatDate } from '../utils/format.js'
import TahsilatGirDialog from '../components/TahsilatGirDialog.vue'

const toastBildirim = useToastBildirim()
const yukleniyor = ref(false)
const ozet = ref(null)
const genisletilenler = ref([])
const tahsilatDialogAcik = ref(false)
const seciliCariId = ref(null)

const gecmis = ref([])
const gecmisYukleniyor = ref(false)

const tahsilatGir = (cari) => {
  seciliCariId.value = cari?.cariId || null
  tahsilatDialogAcik.value = true
}

const odemeYontemiLabel = (y) => ({
  NAKIT: 'Nakit',
  KART: 'Kart',
  TAKSIT: 'Taksit',
  HAVALE: 'Havale'
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
    gecmis.value = r.data?.content || r.data || []
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
    toastBildirim.hata(err?.response?.data?.message || 'Tahsilat özeti yüklenemedi')
  } finally {
    yukleniyor.value = false
  }
}

const hatirlat = async (cari) => {
  try {
    const r = await tahsilatAPI.hatirlat(cari.cariId)
    toastBildirim.basarili(`${cari.cariAd} için ${r.data?.gonderilen || 0} hatırlatma e-postası gönderildi`)
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || 'Hatırlatma gönderilemedi')
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
    `Sayın ${cari.cariAd}, hesabınızda ${formatCurrency(cari.toplamAlacak)} tutarında ödenmemiş bakiye bulunmaktadır. Ödeme konusunda bilgi almak için bize ulaşabilirsiniz.`
  )
  window.open(`https://wa.me/${no}?text=${mesaj}`, '_blank')
}

const ara = (cari) => {
  window.location.href = `tel:${cari.telefon}`
}

onMounted(() => {
  yukle()
  gecmisYukle()
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
  background: rgba(59, 130, 246, 0.15);
  color: #3b82f6;
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
