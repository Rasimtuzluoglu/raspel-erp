<template>
  <Dialog
    :visible="visible"
    modal
    :header="header"
    :style="{ width: '640px' }"
    @update:visible="$emit('update:visible', $event)"
  >
    <div
      v-if="yukleniyor"
      class="fg-bos"
    >
      <i class="pi pi-spin pi-spinner" /> {{ t('faturaGecmis.yukleniyor') }}
    </div>
    <div
      v-else-if="!kayitlar.length"
      class="fg-bos"
    >
      <i class="pi pi-history" />
      <p>{{ t('faturaGecmis.bos') }}</p>
    </div>
    <Timeline
      v-else
      :value="kayitlar"
      class="fg-timeline"
    >
      <template #marker="slotProps">
        <span
          class="fg-marker"
          :class="markerClass(slotProps.item.olay)"
        >
          <i :class="markerIcon(slotProps.item.olay)" />
        </span>
      </template>
      <template #content="slotProps">
        <div class="fg-kart">
          <div class="fg-ust">
            <span class="fg-baslik">{{ olayEtiket(slotProps.item.olay) }}</span>
            <span class="fg-tarih">{{ formatTarih(slotProps.item.tarih) }}</span>
          </div>
          <div class="fg-meta">
            <span><i class="pi pi-user" /> {{ slotProps.item.kullaniciAdi || t('faturaGecmis.sistem') }}</span>
            <span v-if="slotProps.item.ipAdresi"><i class="pi pi-globe" /> {{ slotProps.item.ipAdresi }}</span>
          </div>
          <div
            v-if="slotProps.item.olay === 'YAZDIR'"
            class="fg-yazdir"
          >
            <span class="fg-rozet format">{{ formatEtiket(slotProps.item.yazdirmaFormat) }}</span>
            <span
              v-if="slotProps.item.yaziciAdi"
              class="fg-rozet yazici"
            ><i class="pi pi-print" /> {{ slotProps.item.yaziciAdi }}</span>
            <span class="fg-rozet kopya">{{ t('faturaGecmis.kopyaNo', { n: slotProps.item.kopyaNo || 1 }) }}</span>
          </div>
          <p
            v-if="slotProps.item.aciklama"
            class="fg-aciklama"
          >
            {{ slotProps.item.aciklama }}
          </p>
          <div
            v-if="degisiklikler(slotProps.item).length"
            class="fg-degisiklik"
          >
            <div
              v-for="d in degisiklikler(slotProps.item)"
              :key="d.alan"
              class="fg-satir"
            >
              <span class="fg-alan">{{ alanEtiket(d.alan) }}</span>
              <span class="fg-eski">{{ d.onceki ?? '-' }}</span>
              <i class="pi pi-arrow-right fg-ok" />
              <span class="fg-yeni">{{ d.yeni ?? '-' }}</span>
            </div>
          </div>
        </div>
      </template>
    </Timeline>
    <template #footer>
      <Button
        :label="t('common.close')"
        icon="pi pi-times"
        class="p-button-text"
        @click="$emit('update:visible', false)"
      />
    </template>
  </Dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { faturaAPI } from '../api/index.js'
import { formatDateTime as formatTarih } from '../utils/format.js'

const props = defineProps({
  visible: { type: Boolean, default: false },
  fatura: { type: Object, default: null }
})
defineEmits(['update:visible'])

const { t } = useI18n()
const kayitlar = ref([])
const yukleniyor = ref(false)

const header = computed(() => {
  const no = props.fatura?.faturaNumarasi || ''
  return no ? `${t('faturaGecmis.title')} — ${no}` : t('faturaGecmis.title')
})

const yukle = async () => {
  if (!props.fatura?.id) {
    kayitlar.value = []
    return
  }
  yukleniyor.value = true
  try {
    const r = await faturaAPI.gecmis(props.fatura.id)
    kayitlar.value = r.data || []
  } catch {
    kayitlar.value = []
  } finally {
    yukleniyor.value = false
  }
}

watch(() => [props.visible, props.fatura?.id], ([v]) => {
  if (v) yukle()
})

const OLAY_ANAHTAR = { OLUSTUR: 'olustur', GUNCELLE: 'guncelle', DURUM: 'durumDegisikligi', SIL: 'sil', YAZDIR: 'yazdir' }
const ALAN_ANAHTAR = {
  genelToplam: 'genelToplam', durum: 'durum', vadeTarihi: 'vadeTarihi', araToplam: 'araToplam',
  kdv: 'kdv', odemeDurumu: 'odemeDurumu', odenenTutar: 'odenenTutar', kalanTutar: 'kalanTutar',
  aciklama: 'aciklama', tarih: 'tarih', kalemSayisi: 'kalemSayisi', toplamAdet: 'toplamAdet',
  cariHesap: 'cariHesap', genelIskontoTutari: 'genelIskontoTutari', taksitKurum: 'taksitKurum',
  taksitTutar: 'taksitTutar', odemeYontemi: 'odemeYontemi', depoId: 'depoId', paraBirimi: 'paraBirimi'
}
const FORMAT_ANAHTAR = { A4: 'a4', A5: 'a5', LETTER: 'letter', TERMAL80: 'termal80', TERMAL58: 'termal58', TERMAL: 'termal' }

const olayEtiket = (olay) => t(`faturaGecmis.olay.${OLAY_ANAHTAR[olay] || 'diger'}`)
const alanEtiket = (alan) => (ALAN_ANAHTAR[alan] ? t(`faturaGecmis.alan.${ALAN_ANAHTAR[alan]}`) : alan)
const formatEtiket = (f) => (FORMAT_ANAHTAR[f] ? t(`faturaGecmis.format.${FORMAT_ANAHTAR[f]}`) : (f || '-'))

const markerIcon = (olay) => ({
  OLUSTUR: 'pi pi-plus', GUNCELLE: 'pi pi-pencil', DURUM: 'pi pi-sync',
  SIL: 'pi pi-trash', YAZDIR: 'pi pi-print'
}[olay] || 'pi pi-info-circle')

const markerClass = (olay) => ({
  OLUSTUR: 'm-success', GUNCELLE: 'm-info', DURUM: 'm-warning',
  SIL: 'm-danger', YAZDIR: 'm-print'
}[olay] || 'm-info')

const degisiklikler = (kayit) => {
  if (kayit.olay !== 'GUNCELLE' || !kayit.oncekiDeger || !kayit.yeniDeger) return []
  try {
    const onceki = JSON.parse(kayit.oncekiDeger)
    const yeni = JSON.parse(kayit.yeniDeger)
    const sonuc = []
    for (const alan of Object.keys(yeni)) {
      const o = onceki[alan]
      const y = yeni[alan]
      if ((o ?? '') === (y ?? '')) continue
      sonuc.push({ alan, onceki: o, yeni: y })
    }
    return sonuc
  } catch {
    return []
  }
}
</script>

<style scoped>
.fg-bos {
  text-align: center;
  padding: 2rem;
  color: var(--text-muted);
}
.fg-bos i {
  font-size: 2rem;
  display: block;
  margin-bottom: 0.5rem;
}
.fg-marker {
  display: flex;
  width: 2rem;
  height: 2rem;
  align-items: center;
  justify-content: center;
  color: #fff;
  border-radius: 50%;
  z-index: 1;
}
.m-success { background: #10b981; }
.m-info { background: #3b82f6; }
.m-warning { background: #f59e0b; }
.m-danger { background: #ef4444; }
.m-print { background: #8b5cf6; }
.fg-kart {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 10px 14px;
  margin-bottom: 0.75rem;
}
.fg-ust {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 8px;
}
.fg-baslik {
  font-weight: 600;
  color: var(--text-primary);
  font-size: 0.92rem;
}
.fg-tarih {
  font-size: 0.75rem;
  color: var(--text-muted);
}
.fg-meta {
  display: flex;
  gap: 1rem;
  font-size: 0.78rem;
  color: var(--text-muted);
  margin-top: 2px;
}
.fg-meta span {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
}
.fg-yazdir {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  margin-top: 6px;
}
.fg-rozet {
  font-size: 0.72rem;
  padding: 2px 8px;
  border-radius: 10px;
  background: rgba(139, 92, 246, 0.15);
  color: #8b5cf6;
  font-weight: 600;
}
.fg-rozet.yazici {
  background: rgba(59, 130, 246, 0.15);
  color: #3b82f6;
}
.fg-rozet.kopya {
  background: rgba(100, 116, 139, 0.18);
  color: var(--text-secondary);
}
.fg-aciklama {
  margin: 6px 0 0;
  font-size: 0.82rem;
  color: var(--text-secondary);
  white-space: pre-wrap;
}
.fg-degisiklik {
  margin-top: 8px;
  border-top: 1px dashed var(--border);
  padding-top: 6px;
}
.fg-satir {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.78rem;
  padding: 2px 0;
}
.fg-alan {
  min-width: 110px;
  color: var(--text-muted);
}
.fg-eski {
  color: #f87171;
  text-decoration: line-through;
}
.fg-ok {
  font-size: 0.65rem;
  color: var(--text-muted);
}
.fg-yeni {
  color: #4ade80;
  font-weight: 600;
}
</style>
