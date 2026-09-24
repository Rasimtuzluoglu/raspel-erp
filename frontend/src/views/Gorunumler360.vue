<template>
  <div class="gorunum-360">
    <PageHeader
      :title="t('gorunum360.title')"
      :subtitle="t('gorunum360.subtitle')"
    />

    <TabView>
      <!-- Stok Kâr 360 -->
      <TabPanel>
        <template #header>
          <span class="tab-baslik"><i class="pi pi-chart-line" /> {{ t('gorunum360.stokKar') }}</span>
        </template>
        <div class="kpi-grid">
          <KpiKart
            :baslik="t('gorunum360.toplamCiro')"
            :deger="stokKar.toplamCiro"
            para-birimi
          />
          <KpiKart
            :baslik="t('gorunum360.toplamKar')"
            :deger="stokKar.toplamKar"
            para-birimi
          />
          <KpiKart
            :baslik="t('gorunum360.genelMarj')"
            :deger="stokKar.genelMarj"
            suffix="%"
          />
          <KpiKart
            :baslik="t('gorunum360.negatifMarj')"
            :deger="stokKar.negatifMarjAdet"
            :trend="stokKar.negatifMarjAdet > 0 ? -1 : 0"
          />
        </div>
        <AppDataTable
          :value="stokKar.urunler"
          striped-rows
          :loading="yukleniyor"
          :rows="25"
        >
          <Column
            field="ad"
            :header="t('gorunum360.urun')"
          />
          <Column
            field="stokKodu"
            :header="t('gorunum360.stokKodu')"
            style="width: 130px"
          />
          <Column
            field="satisAdet"
            :header="t('gorunum360.satisAdet')"
            style="width: 110px"
          />
          <Column
            field="ciro"
            :header="t('gorunum360.ciro')"
            style="width: 140px"
          >
            <template #body="{ data }">
              {{ formatCurrency(data.ciro) }}
            </template>
          </Column>
          <Column
            field="maliyet"
            :header="t('gorunum360.maliyet')"
            style="width: 140px"
          >
            <template #body="{ data }">
              {{ formatCurrency(data.maliyet) }}
            </template>
          </Column>
          <Column
            field="brutKar"
            :header="t('gorunum360.brutKar')"
            style="width: 140px"
          >
            <template #body="{ data }">
              <span :class="data.brutKar >= 0 ? 'pozitif' : 'negatif'">{{ formatCurrency(data.brutKar) }}</span>
            </template>
          </Column>
          <Column
            field="marj"
            :header="t('gorunum360.marj')"
            style="width: 90px"
          >
            <template #body="{ data }">
              <Tag
                :value="`${data.marj}%`"
                :severity="data.brutKar >= 0 ? 'success' : 'danger'"
              />
            </template>
          </Column>
          <Column
            field="stokMiktar"
            :header="t('gorunum360.stokMiktar')"
            style="width: 120px"
          />
          <template #empty>
            <EmptyState
              :message="t('gorunum360.veriYok')"
              icon="pi pi-chart-line"
            />
          </template>
        </AppDataTable>
      </TabPanel>

      <!-- Çalışan Performans 360 -->
      <TabPanel>
        <template #header>
          <span class="tab-baslik"><i class="pi pi-users" /> {{ t('gorunum360.calisanPerformans') }}</span>
        </template>
        <div class="kpi-grid">
          <KpiKart
            :baslik="t('gorunum360.toplamTeslimat')"
            :deger="calisan.toplamTeslimat"
          />
          <KpiKart
            :baslik="t('gorunum360.tamamlananTeslimat')"
            :deger="calisan.tamamlananTeslimat"
          />
          <KpiKart
            :baslik="t('gorunum360.calisanSayisi')"
            :deger="calisan.calisanlar.length"
          />
        </div>
        <div class="kart-grid">
          <div
            v-for="c in calisan.calisanlar"
            :key="c.id"
            class="calisan-kart"
          >
            <div class="calisan-avatar">
              {{ (c.ad || '?').charAt(0) }}
            </div>
            <div class="calisan-bilgi">
              <strong>{{ c.ad }}</strong>
              <span class="calisan-rol">{{ c.rol }}</span>
            </div>
            <div class="calisan-sayilar">
              <div class="sayi-blok">
                <span class="sayi">{{ c.toplamTeslimat }}</span>
                <small>{{ t('gorunum360.teslimat') }}</small>
              </div>
              <div class="sayi-blok">
                <span class="sayi pozitif">{{ c.tamamlananTeslimat }}</span>
                <small>{{ t('gorunum360.tamamlandi') }}</small>
              </div>
              <div class="sayi-blok">
                <span class="sayi uyari">{{ c.bekleyenTeslimat }}</span>
                <small>{{ t('gorunum360.bekleyen') }}</small>
              </div>
            </div>
          </div>
          <EmptyState
            v-if="!yukleniyor && calisan.calisanlar.length === 0"
            :message="t('gorunum360.veriYok')"
            icon="pi pi-users"
          />
        </div>
      </TabPanel>

      <!-- Müşteri Segmentasyonu -->
      <TabPanel>
        <template #header>
          <span class="tab-baslik"><i class="pi pi-tags" /> {{ t('gorunum360.musteriSegment') }}</span>
        </template>
        <div class="segment-ozet-grid">
          <div
            v-for="s in segment.segmentOzeti"
            :key="s.segment"
            class="segment-kart"
            :class="segmentSinif(s.segment)"
          >
            <span class="segment-ad">{{ t('gorunum360.segment_' + s.segment) || s.segment }}</span>
            <strong class="segment-adet">{{ s.adet }}</strong>
            <small>{{ formatCurrency(s.toplamCiro) }}</small>
          </div>
        </div>
        <AppDataTable
          :value="segment.musteriler"
          striped-rows
          :loading="yukleniyor"
          :rows="25"
        >
          <Column
            field="ad"
            :header="t('gorunum360.musteri')"
          />
          <Column
            field="segment"
            :header="t('gorunum360.segment')"
            style="width: 150px"
          >
            <template #body="{ data }">
              <span
                v-tooltip.top="data.gerekce"
                :class="['segment-rozet', segmentSinif(data.segment)]"
              >
                {{ t('gorunum360.segment_' + data.segment) }}
              </span>
            </template>
          </Column>
          <Column
            field="ciro"
            :header="t('gorunum360.ciro')"
            style="width: 140px"
          >
            <template #body="{ data }">
              {{ formatCurrency(data.ciro) }}
            </template>
          </Column>
          <Column
            field="kalanTutar"
            :header="t('gorunum360.kalan')"
            style="width: 130px"
          >
            <template #body="{ data }">
              <span :class="data.kalanTutar > 0 ? 'negatif' : ''">{{ formatCurrency(data.kalanTutar) }}</span>
            </template>
          </Column>
          <Column
            field="faturaAdet"
            :header="t('gorunum360.faturaAdet')"
            style="width: 110px"
          />
          <Column
            field="sonFaturaGunOnce"
            :header="t('gorunum360.sonFatura')"
            style="width: 140px"
          >
            <template #body="{ data }">
              {{ data.sonFaturaGunOnce != null ? t('gorunum360.gunOnce', { n: data.sonFaturaGunOnce }) : '-' }}
            </template>
          </Column>
          <template #empty>
            <EmptyState
              :message="t('gorunum360.veriYok')"
              icon="pi pi-tags"
            />
          </template>
        </AppDataTable>
      </TabPanel>
    </TabView>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { raporAPI } from '../api/index.js'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { formatCurrency } from '../utils/format.js'
import PageHeader from '../components/PageHeader.vue'
import KpiKart from '../components/KpiKart.vue'
import EmptyState from '../components/EmptyState.vue'

const { t } = useI18n()
const toastBildirim = useToastBildirim()
const yukleniyor = ref(false)

const stokKar = ref({ urunler: [], toplamCiro: 0, toplamKar: 0, genelMarj: 0, negatifMarjAdet: 0, toplamUrun: 0 })
const calisan = ref({ calisanlar: [], toplamTeslimat: 0, tamamlananTeslimat: 0 })
const segment = ref({ segmentOzeti: [], musteriler: [] })

const segmentSinif = (s) => ({
  VIP: 'segment-vip',
  DUZENLI: 'segment-duzenli',
  YENI: 'segment-yeni',
  RISKLI: 'segment-riskli',
  PASIF: 'segment-pasif',
  GELISMEDE: 'segment-gelismede'
})[s] || ''

const yukle = async () => {
  yukleniyor.value = true
  const [s, c, m] = await Promise.allSettled([
    raporAPI.stokKar360(),
    raporAPI.calisanPerformans360(),
    raporAPI.musteriSegment()
  ])
  if (s.status === 'fulfilled') stokKar.value = s.value.data || stokKar.value
  if (c.status === 'fulfilled') calisan.value = c.value.data || calisan.value
  if (m.status === 'fulfilled') segment.value = m.value.data || segment.value
  if ([s, c, m].some((r) => r.status === 'rejected')) {
    toastBildirim.hata(t('gorunum360.hataYukleme'))
  }
  yukleniyor.value = false
}

onMounted(yukle)
</script>

<style scoped>
.gorunum-360 {
  padding: 0;
}
.tab-baslik {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 14px;
  margin-bottom: 16px;
}
.kart-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 14px;
}
.calisan-kart {
  display: flex;
  align-items: center;
  gap: 14px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 16px;
}
.calisan-avatar {
  width: 46px;
  height: 46px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--accent), var(--accent-hover));
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 18px;
  flex-shrink: 0;
}
.calisan-bilgi {
  display: flex;
  flex-direction: column;
  min-width: 0;
  flex: 1;
}
.calisan-rol {
  font-size: 11px;
  color: var(--text-muted);
}
.calisan-sayilar {
  display: flex;
  gap: 16px;
}
.sayi-blok {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.sayi {
  font-size: 20px;
  font-weight: 800;
  color: var(--text-primary);
}
.sayi.pozitif {
  color: #4ade80;
}
.sayi.uyari {
  color: #fb923c;
}
.sayi-blok small {
  font-size: 10px;
  color: var(--text-muted);
}
.segment-ozet-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}
.segment-kart {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 14px;
  border-radius: 12px;
  border: 1px solid var(--border);
  background: var(--bg-card);
}
.segment-ad {
  font-size: 12px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.segment-adet {
  font-size: 24px;
}
.segment-rozet {
  display: inline-block;
  padding: 3px 10px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 700;
  cursor: help;
}
.segment-vip { background: rgba(168, 85, 247, 0.15); color: #c084fc; }
.segment-duzenli { background: rgba(34, 197, 94, 0.15); color: #4ade80; }
.segment-yeni { background: rgba(96, 165, 250, 0.15); color: #60a5fa; }
.segment-riskli { background: rgba(239, 68, 68, 0.15); color: #f87171; }
.segment-pasif { background: rgba(148, 163, 184, 0.15); color: #94a3b8; }
.segment-gelismede { background: rgba(251, 191, 36, 0.15); color: #fbbf24; }
.pozitif { color: #4ade80; }
.negatif { color: #f87171; }
</style>
