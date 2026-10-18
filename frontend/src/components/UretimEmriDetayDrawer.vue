<template>
  <Drawer
    :visible="visible"
    position="right"
    :header="emir ? `${t('uretim.title')} #${emir.id}` : t('uretim.emirDetay')"
    :style="{ width: '520px', maxWidth: '96vw' }"
    @update:visible="$emit('update:visible', $event)"
  >
    <div
      v-if="emir"
      class="detay"
    >
      <div class="detay-ust">
        <div class="detay-mamul">
          <i class="pi pi-box" />
          <span>{{ emir.urunAd || '-' }}</span>
        </div>
        <Tag
          :value="durumAdi(emir.durum)"
          :severity="durumSeverity(emir.durum)"
        />
      </div>

      <div
        v-if="emir.gecikti"
        class="gecikme-uyari"
      >
        <i class="pi pi-exclamation-triangle" />
        <span>{{ t('uretim.gecikti') }}</span>
      </div>

      <div class="detay-grid">
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('uretim.miktar') }}</span>
          <span>{{ emir.miktar }}</span>
        </div>
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('uretim.uretilen') }}</span>
          <span>{{ emir.uretilenMiktar ?? '-' }}</span>
        </div>
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('uretim.fire') }}</span>
          <span>{{ emir.fireMiktar ?? '-' }}</span>
        </div>
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('uretim.oncelik') }}</span>
          <Tag
            :value="oncelikAdi(emir.oncelik)"
            :severity="oncelikSeverity(emir.oncelik)"
          />
        </div>
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('uretim.planlananBaslangic') }}</span>
          <span>{{ tarih(emir.planlananBaslangic) }}</span>
        </div>
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('uretim.planlananBitis') }}</span>
          <span>{{ tarih(emir.planlananBitis) }}</span>
        </div>
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('uretim.baslama') }}</span>
          <span>{{ emir.baslamaTarihi ? tarihSaat(emir.baslamaTarihi) : '-' }}</span>
        </div>
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('uretim.tamamlanma') }}</span>
          <span>{{ emir.tamamlanmaTarihi ? tarihSaat(emir.tamamlanmaTarihi) : '-' }}</span>
        </div>
      </div>

      <h4>{{ t('uretim.maliyetBilgisi') }}</h4>
      <div class="detay-grid">
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('uretim.hammaddeMaliyeti') }}</span>
          <span>{{ para(emir.hammaddeMaliyeti) }}</span>
        </div>
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('uretim.iscilikMaliyeti') }}</span>
          <span>{{ para(emir.iscilikMaliyeti) }}</span>
        </div>
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('uretim.toplamMaliyet') }}</span>
          <strong>{{ para(emir.toplamMaliyet) }}</strong>
        </div>
      </div>

      <div
        v-if="emir.aciklama"
        class="detay-aciklama"
      >
        {{ emir.aciklama }}
      </div>

      <h4>{{ t('uretim.gecmis') }}</h4>
      <div
        v-if="gecmisYukleniyor"
        class="gecmis-yukleniyor"
      >
        <i class="pi pi-spin pi-spinner" /> {{ t('common.loading') }}
      </div>
      <ol
        v-else-if="gecmis.length"
        class="zaman-cizelgesi"
      >
        <li
          v-for="log in gecmis"
          :key="log.id"
          class="zc-adim tamam"
        >
          <span class="zc-nokta">
            <i class="pi pi-check" />
          </span>
          <div class="zc-icerik">
            <div class="zc-baslik">
              <span>{{ durumAdi(log.yeniDurum) }}</span>
            </div>
            <small
              v-if="log.aciklama"
              class="zc-alt"
            >{{ log.aciklama }}</small>
            <small class="zc-alt">{{ tarihSaat(log.olusturmaTarihi) }}</small>
          </div>
        </li>
      </ol>
      <p
        v-else
        class="muted"
      >
        {{ t('common.noData') }}
      </p>
    </div>

    <template #footer>
      <div class="detay-aksiyonlar">
        <Button
          v-if="emir && emir.durum === 'TASLAK'"
          :label="t('uretim.baslat')"
          icon="pi pi-play"
          @click="$emit('baslat', emir)"
        />
        <Button
          v-if="emir && (emir.durum === 'TASLAK' || emir.durum === 'URETIMDE')"
          :label="t('uretim.tamamla')"
          icon="pi pi-check"
          class="p-button-success"
          @click="$emit('tamamla', emir)"
        />
        <Button
          v-if="emir && (emir.durum === 'TASLAK' || emir.durum === 'URETIMDE')"
          :label="t('uretim.iptalEt')"
          icon="pi pi-times"
          class="p-button-text p-button-danger"
          @click="$emit('iptal', emir)"
        />
      </div>
    </template>
  </Drawer>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import { formatCurrency as para, formatDate as tarih, formatDateTime as tarihSaat } from '../utils/format.js'

defineProps({
  visible: { type: Boolean, default: false },
  emir: { type: Object, default: null },
  gecmis: { type: Array, default: () => [] },
  gecmisYukleniyor: { type: Boolean, default: false }
})

defineEmits(['update:visible', 'baslat', 'tamamla', 'iptal'])

const { t } = useI18n()

const durumAdi = (d) =>
  ({
    TASLAK: t('uretim.taslak'),
    URETIMDE: t('uretim.uretimde'),
    TAMAMLANDI: t('uretim.tamamlandi'),
    IPTAL: t('uretim.iptal')
  })[d] || d

const durumSeverity = (d) =>
  ({ TASLAK: 'secondary', URETIMDE: 'info', TAMAMLANDI: 'success', IPTAL: 'danger' })[d] || 'secondary'

const oncelikAdi = (o) =>
  ({ DUSUK: t('uretim.oncelikDusuk'), NORMAL: t('uretim.oncelikNormal'), YUKSEK: t('uretim.oncelikYuksek') })[o] ||
  o ||
  '-'

const oncelikSeverity = (o) => ({ DUSUK: 'secondary', NORMAL: 'info', YUKSEK: 'warn' })[o] || 'secondary'
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
.detay-mamul {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: var(--text-primary);
}
.detay-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 12px;
}
.detay-alan {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.detay-etiket {
  font-size: 11.5px;
  text-transform: uppercase;
  letter-spacing: 0.03em;
  color: var(--text-muted);
}
.detay-aciklama {
  padding: 10px;
  border-radius: 8px;
  background: var(--bg-hover, rgba(148, 163, 184, 0.1));
  font-size: 13px;
}
h4 {
  margin: 0;
  font-size: 15px;
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
  background: var(--success-soft, rgba(16, 185, 129, 0.15));
  border: 1px solid var(--success, #10b981);
  color: var(--success, #10b981);
  flex-shrink: 0;
  z-index: 1;
}
.zc-icerik {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}
.zc-baslik {
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--text-primary);
}
.zc-alt {
  font-size: 0.75rem;
  color: var(--text-muted);
}
.gecmis-yukleniyor {
  color: var(--text-muted);
  font-size: 13px;
}
.muted {
  font-size: 12px;
  color: var(--text-muted);
}
.detay-aksiyonlar {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
</style>
