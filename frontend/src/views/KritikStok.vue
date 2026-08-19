<template>
  <div class="kritik-stok-container">
    <div class="sayfa-baslik">
      <h1 class="page-title">
        Kritik Stok & Akıllı Talep Tahmini
      </h1>
      <div class="header-actions">
        <SelectButton
          v-model="gorunumTipi"
          :options="gorunumSecenekleri"
          option-label="label"
          option-value="value"
          class="mr-2"
        />
        <Button
          icon="pi pi-refresh"
          label="Yenile"
          class="p-button-text"
          :loading="yukleniyor"
          @click="yukle"
        />
      </div>
    </div>

    <IlkZiyaretIpuclari
      anahtar="kritik-stok"
      baslik="Kritik Stok & Akıllı Talep Tahmini"
      metin="Kritik seviyedeki ürünleri listeleyin veya 'Akıllı Talep Tahmini' sekmesinden yapay zekanın tüketim trendlerine göre hesapladığı tahmini tükenme günlerini ve proaktif sipariş önerilerini inceleyin."
    />

    <!-- 1. Akıllı Talep Tahmini Görünümü -->
    <template v-if="gorunumTipi === 'tahmin'">
      <div
        v-if="tahminList.length"
        class="tahmin-ozet-grid"
      >
        <div class="ozet-kart acil">
          <i class="pi pi-exclamation-circle" />
          <div>
            <span>Acil Sipariş Gereken</span>
            <strong>{{ tahminList.filter(t => t.durum === 'KRITIK').length }} Ürün</strong>
          </div>
        </div>
        <div class="ozet-kart dikkat">
          <i class="pi pi-clock" />
          <div>
            <span>15 Gün İçinde Bitecek</span>
            <strong>{{ tahminList.filter(t => t.durum === 'DIKKAT').length }} Ürün</strong>
          </div>
        </div>
        <div class="ozet-kart guvenli">
          <i class="pi pi-check-circle" />
          <div>
            <span>Güvenli Seviyede</span>
            <strong>{{ tahminList.filter(t => t.durum === 'GUVENLI').length }} Ürün</strong>
          </div>
        </div>
      </div>

      <DataTable
        state-storage="session"
        state-key="stok-tahmin-table-state"
        :value="tahminList"
        striped-rows
        :loading="yukleniyor"
      >
        <Column
          field="stokKodu"
          header="Stok Kodu"
          style="width: 110px"
        />
        <Column
          field="ad"
          header="Ürün"
          sortable
        />
        <Column
          field="mevcutMiktar"
          header="Mevcut Stok"
          sortable
        >
          <template #body="{ data }">
            <strong>{{ data.mevcutMiktar }} {{ data.birim }}</strong>
          </template>
        </Column>
        <Column
          field="gunlukOrtalamaTuketim"
          header="Günlük Tüketim"
          sortable
        >
          <template #body="{ data }">
            {{ data.gunlukOrtalamaTuketim }} {{ data.birim }}/gün
          </template>
        </Column>
        <Column
          field="tahminiTukenmeGunu"
          header="Tahmini Tükenme"
          sortable
        >
          <template #body="{ data }">
            <Tag
              :value="data.tahminiTukenmeGunu >= 999 ? '999+ gün' : `${data.tahminiTukenmeGunu} gün`"
              :severity="data.durum === 'KRITIK' ? 'danger' : data.durum === 'DIKKAT' ? 'warn' : 'success'"
            />
          </template>
        </Column>
        <Column
          field="onerilenSiparisMiktari"
          header="Önerilen Sipariş"
          sortable
        >
          <template #body="{ data }">
            <Tag
              :value="`${data.onerilenSiparisMiktari} ${data.birim}`"
              severity="info"
            />
          </template>
        </Column>
        <Column
          field="proaktifOneri"
          header="Yapay Zeka Önerisi"
        >
          <template #body="{ data }">
            <span :class="['ai-oneri', data.durum.toLowerCase()]">
              <i class="pi pi-sparkles" /> {{ data.proaktifOneri }}
            </span>
          </template>
        </Column>
        <Column
          header="İşlem"
          style="width: 100px"
        >
          <template #body="{ data }">
            <Button
              icon="pi pi-cart-plus"
              class="p-button-rounded p-button-text p-button-warning"
              title="Tedarik Talebi Oluştur"
              @click="talepOlustur(data)"
            />
          </template>
        </Column>
      </DataTable>
    </template>

    <!-- 2. Klasik Kritik Stok Görünümü -->
    <template v-else>
      <div
        v-if="!yukleniyor && list.length === 0"
        class="bos-durum"
      >
        <i class="pi pi-check-circle" />
        <p>Kritik seviyede stok yok. Tüm ürünler güvenli seviyede.</p>
      </div>

      <div
        v-if="list.length"
        class="onem-uyari"
      >
        <i class="pi pi-exclamation-triangle" />
        <strong>{{ list.length }} ürün</strong> kritik seviyede — yeniden sipariş önerisi oluşturuldu.
      </div>

      <DataTable
        state-storage="session"
        state-key="kritikstok-table-state"
        :value="list"
        striped-rows
        :loading="yukleniyor"
      >
        <Column
          field="stokKodu"
          header="Stok Kodu"
          style="width: 110px"
        />
        <Column
          field="ad"
          header="Ürün"
          sortable
        />
        <Column
          field="miktar"
          header="Mevcut Stok"
          sortable
        >
          <template #body="{ data }">
            <strong class="kritik-miktar">{{ data.miktar }} {{ data.birim }}</strong>
          </template>
        </Column>
        <Column
          field="minMiktar"
          header="Kritik Seviye"
        >
          <template #body="{ data }">
            {{ data.minMiktar }} {{ data.birim }}
          </template>
        </Column>
        <Column
          field="onerilenSiparisMiktari"
          header="Önerilen Sipariş"
          sortable
        >
          <template #body="{ data }">
            <Tag
              :value="`${data.onerilenSiparisMiktari} ${data.birim}`"
              severity="warning"
            />
          </template>
        </Column>
        <Column
          field="marka"
          header="Marka"
        />
        <Column
          field="tedarikciAd"
          header="Tedarikçi"
        >
          <template #body="{ data }">
            {{ data.tedarikciAd || '-' }}
          </template>
        </Column>
        <Column
          header="İşlem"
          style="width: 150px"
        >
          <template #body="{ data }">
            <Button
              icon="pi pi-cart-plus"
              class="p-button-rounded p-button-text p-button-warning"
              title="Tedarik Talebi Oluştur"
              @click="talepOlustur(data)"
            />
            <router-link
              v-slot="{ navigate }"
              :to="`/stoklar`"
              custom
            >
              <Button
                icon="pi pi-box"
                class="p-button-rounded p-button-text"
                title="Stoklara Git"
                @click="navigate"
              />
            </router-link>
          </template>
        </Column>
      </DataTable>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { stokAPI, satinalmaTalepAPI } from '../api/index.js'
import IlkZiyaretIpuclari from '../components/IlkZiyaretIpuclari.vue'

const toastBildirim = useToastBildirim()
const list = ref([])
const tahminList = ref([])
const yukleniyor = ref(false)

const gorunumTipi = ref('tahmin')
const gorunumSecenekleri = [
  { label: 'Akıllı Talep Tahmini (AI)', value: 'tahmin' },
  { label: 'Kritik Seviyedeki Stoklar', value: 'kritik' }
]

onMounted(yukle)

async function yukle() {
  yukleniyor.value = true
  try {
    const [kritikRes, tahminRes] = await Promise.all([
      stokAPI.kritik(),
      stokAPI.talepTahmini()
    ])
    list.value = kritikRes.data || []
    tahminList.value = tahminRes.data || []
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || 'Stok verileri yüklenemedi')
  }
  yukleniyor.value = false
}

const talepOlustur = async (data) => {
  try {
    const miktar = data.onerilenSiparisMiktari || 100
    await satinalmaTalepAPI.create({
      talepNo: 'TAL-' + Date.now(),
      tarih: new Date().toISOString().split('T')[0],
      talepEden: 'Sistem (AI Tahmin)',
      departman: 'Stok Yönetimi',
      aciklama: `Otomatik talep - ${data.ad} (${data.stokKodu || '-'}): mevcut ${data.mevcutMiktar || data.miktar}, önerilen sipariş: ${miktar} ${data.birim || 'Adet'}. Tedarikçi: ${data.tedarikciAd || 'Belirtilmemiş'}.`
    })
    toastBildirim.basarili(`${data.ad} için tedarik talebi oluşturuldu.`)
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || 'Tedarik talebi oluşturulamadı')
  }
}
</script>

<style scoped>
.kritik-stok-container {
  padding: 0;
}
.sayfa-baslik {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 10px;
}
.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}
.tahmin-ozet-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}
.ozet-kart {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 14px;
}
.ozet-kart i {
  font-size: 28px;
}
.ozet-kart.acil {
  border-left: 5px solid #ef4444;
}
.ozet-kart.acil i {
  color: #ef4444;
}
.ozet-kart.dikkat {
  border-left: 5px solid #f59e0b;
}
.ozet-kart.dikkat i {
  color: #f59e0b;
}
.ozet-kart.guvenli {
  border-left: 5px solid #10b981;
}
.ozet-kart.guvenli i {
  color: #10b981;
}
.ozet-kart span {
  display: block;
  font-size: 12px;
  color: var(--text-muted);
  font-weight: 600;
  text-transform: uppercase;
}
.ozet-kart strong {
  font-size: 18px;
  color: var(--text-primary);
}
.ai-oneri {
  font-size: 13px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.ai-oneri.kritik {
  color: #ef4444;
  font-weight: 600;
}
.ai-oneri.dikkat {
  color: #f59e0b;
}
.ai-oneri.guvenli {
  color: var(--text-secondary);
}
.onem-uyari {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  margin-bottom: 18px;
  background: rgba(245, 158, 11, 0.12);
  border: 1px solid rgba(245, 158, 11, 0.3);
  border-radius: 10px;
  color: #fbbf24;
  font-size: 14px;
}
.onem-uyari i {
  font-size: 18px;
}
.bos-durum {
  text-align: center;
  padding: 60px 20px;
  color: var(--text-muted);
  background: var(--bg-card);
  border: 1px dashed var(--border);
  border-radius: 12px;
}
.bos-durum i {
  font-size: 40px;
  color: #10b981;
  display: block;
  margin-bottom: 12px;
}
.kritik-miktar {
  color: #ef4444;
}
</style>
