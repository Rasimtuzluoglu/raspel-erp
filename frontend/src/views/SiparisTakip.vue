<template>
  <div class="takip-sayfasi">
    <div class="sayfa-baslik">
      <h1><i class="pi pi-sitemap" /> Sipariş Takibi</h1>
      <Button
        icon="pi pi-refresh"
        class="p-button-text p-button-sm"
        @click="yukle"
      />
    </div>

    <div
      v-if="yukleniyor"
      class="bos"
    >
      Yükleniyor...
    </div>
    <div
      v-else-if="!zincir.length"
      class="bos"
    >
      Henüz sipariş yok.
    </div>

    <div
      v-for="s in zincir"
      :key="s.siparisId"
      class="takip-kart"
    >
      <div class="kart-ust">
        <strong>{{ s.siparisNo }}</strong>
        <span
          v-if="s.cariAd"
          class="muted"
        >{{ s.cariAd }}</span>
      </div>
      <div class="adimlar">
        <div
          class="adim"
          :class="{ tamam: !bosDurum(s.siparisDurum) && s.siparisDurum !== 'IPTAL' }"
        >
          <i class="pi pi-file" />
          <span>Sipariş</span>
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
          <span>Üretim</span>
          <Tag
            v-if="s.uretimDurum"
            :value="s.uretimDurum"
            :severity="durumSeverity(s.uretimDurum)"
          />
          <Tag
            v-else
            value="Yok"
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
          <span>Sevk</span>
          <Tag
            v-if="s.sevkDurum"
            :value="s.sevkDurum"
            :severity="durumSeverity(s.sevkDurum)"
          />
          <Tag
            v-else
            value="Yok"
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
          <span>Teslimat</span>
          <Tag
            v-if="s.teslimatDurum"
            :value="s.teslimatDurum"
            :severity="durumSeverity(s.teslimatDurum)"
          />
          <Tag
            v-else
            value="Yok"
            severity="secondary"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { siparisTakipAPI } from '../api/index.js'
import { useToastBildirim } from '../composables/useToastBildirim.js'

const toastBildirim = useToastBildirim()
const zincir = ref([])
const yukleniyor = ref(false)

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
    toastBildirim.hata('Sipariş takibi yüklenemedi')
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
