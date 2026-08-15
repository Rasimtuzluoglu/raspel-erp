<template>
  <div class="kritik-stok-container">
    <div class="sayfa-baslik">
      <h1 class="page-title">
        Kritik Stok & Yeniden Sipariş
      </h1>
      <Button
        icon="pi pi-refresh"
        label="Yenile"
        class="p-button-text"
        :loading="yukleniyor"
        @click="yukle"
      />
    </div>

    <IlkZiyaretIpuclari
      anahtar="kritik-stok"
      baslik="Kritik Stok & Yeniden Sipariş"
      metin="Kritik seviyeye (min. miktar) düşen ürünler otomatik listelenir. 'Önerilen Sipariş' sütunu, güvenli stoğa dönmek için önerilen sipariş miktarını gösterir."
    />

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
      :value="list"
      striped-rows
      :loading="yukleniyor"
    >
      <Column
        field="stokKodu"
        header="Stok Kodu"
        style="width:110px"
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
        style="width:150px"
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
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { stokAPI, satinalmaTalepAPI } from '../api/index.js'
import IlkZiyaretIpuclari from '../components/IlkZiyaretIpuclari.vue'

const toastBildirim = useToastBildirim()
const list = ref([])
const yukleniyor = ref(false)

onMounted(yukle)

const yukle = async () => {
  yukleniyor.value = true
  try {
    const r = await stokAPI.kritik()
    list.value = r.data || []
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || 'Kritik stoklar yüklenemedi')
  }
  yukleniyor.value = false
}

const talepOlustur = async (data) => {
  try {
    await satinalmaTalepAPI.create({
      talepNo: 'TAL-' + Date.now(),
      tarih: new Date().toISOString().split('T')[0],
      talepEden: 'Sistem',
      departman: 'Stok Yönetimi',
      aciklama: `Kritik stok - ${data.ad} (${data.stokKodu || '-'}): mevcut ${data.miktar}, kritik seviye ${data.minMiktar}, önerilen sipariş ${data.onerilenSiparisMiktari} ${data.birim}. Tedarikçi: ${data.tedarikciAd || 'Belirtilmemiş'}.`
    })
    toastBildirim.basarili(`${data.ad} için tedarik talebi oluşturuldu.`)
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || 'Tedarik talebi oluşturulamadı')
  }
}
</script>

<style scoped>
.kritik-stok-container { padding: 0; }
.sayfa-baslik { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.onem-uyari {
  display: flex; align-items: center; gap: 10px; padding: 12px 16px; margin-bottom: 18px;
  background: rgba(245,158,11,0.12); border: 1px solid rgba(245,158,11,0.3); border-radius: 10px;
  color: #fbbf24; font-size: 14px;
}
.onem-uyari i { font-size: 18px; }
.bos-durum {
  text-align: center; padding: 60px 20px; color: var(--text-muted);
  background: var(--bg-card); border: 1px dashed var(--border); border-radius: 12px;
}
.bos-durum i { font-size: 40px; color: #10b981; display: block; margin-bottom: 12px; }
.kritik-miktar { color: #ef4444; }
</style>
