<template>
  <div class="takip-sayfasi">
    <div class="sayfa-baslik">
      <h1><i class="pi pi-sitemap" /> {{ t('siparisTakip.title') }}</h1>
      <div class="baslik-aksiyonlar">
        <Dropdown
          v-model="secilenSofor"
          :options="soforSecenekleri"
          option-label="label"
          option-value="value"
          :placeholder="t('siparisTakip.tumSoforler')"
          :show-clear="true"
          class="sofor-filtre"
        />
        <Button
          icon="pi pi-refresh"
          class="p-button-text p-button-sm"
          @click="yukle"
        />
      </div>
    </div>

    <div
      v-if="yukleniyor"
      class="bos"
    >
      {{ t('common.loading') }}
    </div>
    <div
      v-else-if="!zincir.length"
      class="bos"
    >
      {{ t('siparisTakip.bos') }}
    </div>
    <div
      v-else-if="!filtreliZincir.length"
      class="bos"
    >
      {{ t('siparisTakip.filtreBos') }}
    </div>

    <div
      v-for="s in filtreliZincir"
      :key="s.siparisId"
      class="takip-kart"
    >
      <div class="kart-ust">
        <strong>{{ s.siparisNo }}</strong>
        <span
          v-if="s.cariAd"
          class="muted"
        >{{ s.cariAd }}</span>
        <Tag
          v-if="s.driverAd"
          :value="t('siparisTakip.soforEtiket', { ad: s.driverAd })"
          severity="info"
        />
      </div>
      <div class="adimlar">
        <div
          class="adim"
          :class="{ tamam: !bosDurum(s.siparisDurum) && s.siparisDurum !== 'IPTAL' }"
        >
          <i class="pi pi-file" />
          <span>{{ t('siparisTakip.siparis') }}</span>
          <Tag
            :value="s.siparisDurum"
            :severity="durumSeverity(s.siparisDurum)"
          />
        </div>
        <div class="ok">
          <i class="pi pi-arrow-right" />
        </div>
        <div
          class="adim"
          :class="{ tamam: !!s.uretimDurum }"
        >
          <i class="pi pi-cog" />
          <span>{{ t('siparisTakip.uretim') }}</span>
          <Tag
            v-if="s.uretimDurum"
            :value="s.uretimDurum"
            :severity="durumSeverity(s.uretimDurum)"
          />
          <Tag
            v-else
            :value="t('siparisTakip.yok')"
            severity="secondary"
          />
        </div>
        <div class="ok">
          <i class="pi pi-arrow-right" />
        </div>
        <div
          class="adim"
          :class="{ tamam: !!s.sevkDurum }"
        >
          <i class="pi pi-truck" />
          <span>{{ t('siparisTakip.sevk') }}</span>
          <Tag
            v-if="s.sevkDurum"
            :value="s.sevkDurum"
            :severity="durumSeverity(s.sevkDurum)"
          />
          <Tag
            v-else
            :value="t('siparisTakip.yok')"
            severity="secondary"
          />
        </div>
        <div class="ok">
          <i class="pi pi-arrow-right" />
        </div>
        <div
          class="adim"
          :class="{ tamam: s.teslimatDurum === 'TESLIM_EDILDI' }"
        >
          <i class="pi pi-map-marker" />
          <span>{{ t('siparisTakip.teslimat') }}</span>
          <Tag
            v-if="s.teslimatDurum"
            :value="s.teslimatDurum"
            :severity="durumSeverity(s.teslimatDurum)"
          />
          <Tag
            v-else
            :value="t('siparisTakip.yok')"
            severity="secondary"
          />
          <Tag
            v-if="s.teslimatGecikti"
            :value="t('siparisTakip.gecikti')"
            severity="danger"
          />
          <span
            v-if="s.beklenenTeslimTarihi"
            class="beklenen-tarih"
          >{{ formatTarih(s.beklenenTeslimTarihi) }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { siparisTakipAPI } from '../api/index.js'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useI18n } from 'vue-i18n'
import { formatTarih } from '../utils/format.js'

const toastBildirim = useToastBildirim()
const { t } = useI18n()
const zincir = ref([])
const yukleniyor = ref(false)
const secilenSofor = ref(null)

const soforSecenekleri = computed(() => {
  const kume = new Set(zincir.value.map((s) => s.driverAd).filter(Boolean))
  return [...kume].map((ad) => ({ label: ad, value: ad }))
})

const filtreliZincir = computed(() => {
  if (!secilenSofor.value) return zincir.value
  return zincir.value.filter((s) => s.driverAd === secilenSofor.value)
})

const bosDurum = (d) => !d || d === 'TEKLIF' || d === 'TASLAK'

const durumSeverity = (d) => {
  if (!d) return 'secondary'
  if (['IPTAL', 'REDDEDILDI'].includes(d)) return 'danger'
  if (['TAMAMLANDI', 'KESILDI', 'TESLIM_EDILDI', 'ONAYLANDI'].includes(d)) return 'success'
  if (['URETIMDE', 'BEKLEMEDE', 'YOLDA'].includes(d)) return 'info'
  return 'warn'
}

const yukle = async () => {
  yukleniyor.value = true
  try {
    const r = await siparisTakipAPI.zincir()
    zincir.value = r.data || []
  } catch (err) {
    toastBildirim.hata(t('siparisTakip.hataYukleme'))
  }
  yukleniyor.value = false
}

onMounted(yukle)
</script>

<style scoped>
.takip-sayfasi {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.sayfa-baslik {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.sayfa-baslik h1 {
  margin: 0;
  font-size: 20px;
  display: flex;
  align-items: center;
}
.baslik-aksiyonlar {
  display: flex;
  align-items: center;
  gap: 8px;
}
.sofor-filtre {
  min-width: 180px;
}
.beklenen-tarih {
  font-size: 11px;
  color: var(--text-muted);
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.bos {
  text-align: center;
  color: var(--text-muted);
  padding: 32px;
}
.takip-kart {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 14px 16px;
}
.kart-ust {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}
.muted {
  font-size: 12px;
  color: var(--text-muted);
}
.adimlar {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.adim {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  border-radius: 8px;
  background: var(--bg-primary);
  border: 1px solid var(--border);
  font-size: 13px;
  opacity: 0.75;
}
.adim.tamam {
  opacity: 1;
  border-color: rgba(16, 185, 129, 0.4);
}
.adim i {
  color: var(--accent, #3b82f6);
}
.ok {
  color: var(--text-muted);
}
</style>
