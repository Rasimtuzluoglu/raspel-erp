<template>
  <Drawer
    :visible="visible"
    position="right"
    :header="satir?.siparisNo || t('siparisTakip.title')"
    :style="{ width: '480px', maxWidth: '96vw' }"
    @update:visible="$emit('update:visible', $event)"
  >
    <div
      v-if="satir"
      class="detay"
    >
      <div class="detay-ust">
        <div class="detay-cari">
          <i class="pi pi-building" />
          <span>{{ satir.cariAd || '-' }}</span>
        </div>
        <Tag
          v-if="satir.driverAd"
          :value="t('siparisTakip.soforEtiket', { ad: satir.driverAd })"
          severity="info"
        />
      </div>

      <!-- Adim timeline -->
      <ol class="zaman-cizelgesi">
        <li
          v-for="(adim, i) in adimlar"
          :key="i"
          class="zc-adim"
          :class="{ tamam: adim.tamam, aktif: adim.aktif }"
        >
          <span class="zc-nokta">
            <i :class="adim.tamam ? 'pi pi-check' : adim.ikon" />
          </span>
          <div class="zc-icerik">
            <div class="zc-baslik">
              <span>{{ adim.ad }}</span>
              <Tag
                :value="adim.durumEtiket"
                :severity="adim.severity"
              />
              <span
                v-if="adim.sayi > 1"
                class="adet-rozet"
              >×{{ adim.sayi }}</span>
            </div>
            <small
              v-if="adim.altBilgi"
              class="zc-alt"
            >{{ adim.altBilgi }}</small>
          </div>
        </li>
      </ol>

      <div
        v-if="satir.teslimatGecikti"
        class="gecikme-uyari"
      >
        <i class="pi pi-exclamation-triangle" />
        <span>{{ t('siparisTakip.gecikmeUyari') }}</span>
      </div>
    </div>

    <template #footer>
      <div class="detay-aksiyonlar">
        <Button
          v-if="satir && !satir.uretimDurum && satir.siparisDurum !== 'IPTAL'"
          :label="t('uretim.siparistenEmir')"
          icon="pi pi-cog"
          class="p-button-outlined"
          @click="$emit('uretim-emri', satir)"
        />
        <Button
          :label="t('siparisTakip.siparislereGit')"
          icon="pi pi-shopping-cart"
          text
          @click="$emit('git', 'Siparisler')"
        />
        <Button
          :label="t('siparisTakip.teslimatlaraGit')"
          icon="pi pi-truck"
          text
          @click="$emit('git', 'Teslimatlar')"
        />
      </div>
    </template>
  </Drawer>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { formatTarih } from '../utils/format.js'

const props = defineProps({
  visible: { type: Boolean, default: false },
  satir: { type: Object, default: null }
})

defineEmits(['update:visible', 'uretim-emri', 'git'])

const { t } = useI18n()

const durumEtiket = (kod) => {
  if (!kod) return ''
  const anahtar = `siparisTakip.durum.${kod}`
  const ceviri = t(anahtar)
  return ceviri === anahtar ? kod : ceviri
}
const durumSeverity = (d) => {
  if (!d) return 'secondary'
  if (['IPTAL', 'REDDEDILDI', 'HATA'].includes(d)) return 'danger'
  if (['TAMAMLANDI', 'KESILDI', 'TESLIM_EDILDI', 'ONAYLANDI'].includes(d)) return 'success'
  if (['URETIMDE', 'BEKLEMEDE', 'YOLDA', 'HAZIRLANIYOR'].includes(d)) return 'info'
  return 'warn'
}

const adimlar = computed(() => {
  const s = props.satir
  if (!s) return []
  const siparisTamam = !['TEKLIF', 'TASLAK', 'IPTAL'].includes(s.siparisDurum) && !!s.siparisDurum
  const uretimTamam = s.uretimDurum === 'TAMAMLANDI'
  const sevkTamam = ['KESILDI', 'TAMAMLANDI'].includes(s.sevkDurum)
  const teslimTamam = s.teslimatDurum === 'TESLIM_EDILDI'
  return [
    {
      ad: t('siparisTakip.siparis'),
      ikon: 'pi pi-file',
      durumEtiket: durumEtiket(s.siparisDurum) || t('siparisTakip.yok'),
      severity: durumSeverity(s.siparisDurum),
      sayi: 1,
      tamam: siparisTamam,
      aktif: false,
      altBilgi: ''
    },
    {
      ad: t('siparisTakip.uretim'),
      ikon: 'pi pi-cog',
      durumEtiket: durumEtiket(s.uretimDurum) || t('siparisTakip.yok'),
      severity: durumSeverity(s.uretimDurum),
      sayi: s.uretimSayisi || 0,
      tamam: uretimTamam,
      aktif: !!s.uretimDurum && !uretimTamam,
      altBilgi: ''
    },
    {
      ad: t('siparisTakip.sevk'),
      ikon: 'pi pi-truck',
      durumEtiket: durumEtiket(s.sevkDurum) || t('siparisTakip.yok'),
      severity: durumSeverity(s.sevkDurum),
      sayi: s.sevkSayisi || 0,
      tamam: sevkTamam,
      aktif: !!s.sevkDurum && !sevkTamam,
      altBilgi: ''
    },
    {
      ad: t('siparisTakip.teslimat'),
      ikon: 'pi pi-map-marker',
      durumEtiket: durumEtiket(s.teslimatDurum) || t('siparisTakip.yok'),
      severity: durumSeverity(s.teslimatDurum),
      sayi: s.teslimatSayisi || 0,
      tamam: teslimTamam,
      aktif: !!s.teslimatDurum && !teslimTamam,
      altBilgi: s.beklenenTeslimTarihi
        ? t('siparisTakip.beklenen') + ': ' + formatTarih(s.beklenenTeslimTarihi)
        : ''
    }
  ]
})
</script>

<style scoped>
.detay {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.detay-ust {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
}
.detay-cari {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: var(--text-primary);
}
.zaman-cizelgesi {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.zc-adim {
  display: flex;
  gap: 12px;
  padding: 8px 0;
  position: relative;
  opacity: 0.6;
}
.zc-adim.tamam,
.zc-adim.aktif {
  opacity: 1;
}
.zc-adim:not(:last-child)::before {
  content: '';
  position: absolute;
  left: 15px;
  top: 34px;
  bottom: -4px;
  width: 2px;
  background: var(--border);
}
.zc-nokta {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-secondary);
  border: 1px solid var(--border);
  color: var(--text-muted);
  flex-shrink: 0;
  z-index: 1;
}
.zc-adim.tamam .zc-nokta {
  background: var(--success-soft, rgba(16, 185, 129, 0.15));
  border-color: var(--success, #10b981);
  color: var(--success, #10b981);
}
.zc-adim.aktif .zc-nokta {
  background: var(--accent-soft);
  border-color: var(--accent);
  color: var(--accent);
}
.zc-icerik {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}
.zc-baslik {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--text-primary);
}
.zc-alt {
  font-size: 0.75rem;
  color: var(--text-muted);
}
.gecikme-uyari {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border-radius: 10px;
  border: 1px solid rgba(239, 68, 68, 0.3);
  background: rgba(239, 68, 68, 0.1);
  color: var(--danger, #ef4444);
  font-size: 0.82rem;
  font-weight: 600;
}
.detay-aksiyonlar {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
</style>
