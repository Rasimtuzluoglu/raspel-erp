<template>
  <Drawer
    :visible="visible"
    position="right"
    :header="t('butceler.detayBaslik')"
    :style="{ width: '460px' }"
    :breakpoints="{ '575px': '92vw' }"
    @update:visible="$emit('update:visible', $event)"
  >
    <div
      v-if="kayit"
      class="detay"
    >
      <div class="detay-ust">
        <div class="detay-baslik">
          <i class="pi pi-wallet" />
          <span>{{ kayit.ad }}</span>
        </div>
        <div class="detay-rozetler">
          <Tag
            :value="durumAdi(kayit.durum)"
            :severity="durumSeverity(kayit.durum)"
          />
          <Tag
            :value="kayit.tur === 'GELIR' ? t('butceler.gelir') : t('butceler.gider')"
            :severity="kayit.tur === 'GELIR' ? 'success' : 'danger'"
          />
          <Tag
            :value="`v${kayit.revizyon || 1}`"
            severity="secondary"
          />
        </div>
      </div>

      <div class="detay-grid">
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('butceler.kategori') }}</span>
          <span>{{ kayit.kategori || '-' }}</span>
        </div>
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('butceler.donem') }}</span>
          <span>{{ kayit.ay }}/{{ kayit.yil }}</span>
        </div>
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('common.amount') }}</span>
          <strong>{{ formatCurrency(kayit.tutar) }}</strong>
        </div>
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('butceler.onaylayan') }}</span>
          <span>{{ kayit.onaylayanKullaniciId ? '#' + kayit.onaylayanKullaniciId : '-' }}</span>
        </div>
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('butceler.onayTarihi') }}</span>
          <span>{{ kayit.onayTarihi ? formatTarihSaat(kayit.onayTarihi) : '-' }}</span>
        </div>
        <div
          v-if="kayit.redGerekcesi"
          class="detay-alan genis"
        >
          <span class="detay-etiket">{{ t('butceler.redGerekcesi') }}</span>
          <span class="red-metin">{{ kayit.redGerekcesi }}</span>
        </div>
        <div
          v-if="kayit.aciklama"
          class="detay-alan genis"
        >
          <span class="detay-etiket">{{ t('common.description') }}</span>
          <span>{{ kayit.aciklama }}</span>
        </div>
      </div>

      <h4>{{ t('butceler.revizyonlar') }}</h4>
      <div
        v-if="revizyonYukleniyor"
        class="revizyon-yukleniyor"
        :aria-busy="true"
      >
        <SkeletonLoader
          :count="3"
          height="48px"
        />
      </div>
      <ol
        v-else-if="revizyonlar.length"
        class="zaman-cizelgesi"
      >
        <li
          v-for="r in revizyonlar"
          :key="r.id"
          class="zc-adim"
          :class="islemSinif(r.islem)"
        >
          <span class="zc-nokta"><i :class="islemIkon(r.islem)" /></span>
          <div class="zc-icerik">
            <div class="zc-baslik">
              <span>{{ islemAdi(r.islem) }} · v{{ r.revizyon }}</span>
              <span class="zc-tutar">{{ formatCurrency(r.tutar) }}</span>
            </div>
            <small
              v-if="r.aciklama"
              class="zc-alt"
            >{{ r.aciklama }}</small>
            <small class="zc-alt">{{ formatTarihSaat(r.olusturmaTarihi) }}</small>
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
          v-if="kayit && kayit.durum === 'TASLAK'"
          :label="t('butceler.onayla')"
          icon="pi pi-check"
          severity="success"
          @click="$emit('onayla', kayit)"
        />
        <Button
          v-if="kayit && kayit.durum === 'TASLAK'"
          :label="t('butceler.reddet')"
          icon="pi pi-times"
          severity="danger"
          outlined
          @click="$emit('reddet', kayit)"
        />
        <Button
          v-if="kayit"
          :label="t('common.edit')"
          icon="pi pi-pencil"
          text
          @click="$emit('duzenle', kayit)"
        />
      </div>
    </template>
  </Drawer>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import { formatCurrency, formatTarihSaat } from '../utils/format.js'

defineProps({
  visible: { type: Boolean, default: false },
  kayit: { type: Object, default: null },
  revizyonlar: { type: Array, default: () => [] },
  revizyonYukleniyor: { type: Boolean, default: false }
})

defineEmits(['update:visible', 'duzenle', 'onayla', 'reddet'])

const { t } = useI18n()

const durumAdi = (d) =>
  ({
    TASLAK: t('butceler.durumTaslak'),
    ONAYLANDI: t('butceler.durumOnaylandi'),
    REDDEDILDI: t('butceler.durumReddedildi')
  })[d] || d || '-'

const durumSeverity = (d) =>
  ({ TASLAK: 'warn', ONAYLANDI: 'success', REDDEDILDI: 'danger' })[d] || 'secondary'

const islemAdi = (i) =>
  ({
    OLUSTUR: t('butceler.islemOlustur'),
    GUNCELLE: t('butceler.islemGuncelle'),
    ONAY: t('butceler.islemOnay'),
    RED: t('butceler.islemRed')
  })[i] || i

const islemSinif = (i) =>
  ({ OLUSTUR: 'taslak', GUNCELLE: 'uretimde', ONAY: 'tamam', RED: 'iptal' })[i] || 'tamam'

const islemIkon = (i) =>
  ({
    OLUSTUR: 'pi pi-plus',
    GUNCELLE: 'pi pi-pencil',
    ONAY: 'pi pi-check',
    RED: 'pi pi-times'
  })[i] || 'pi pi-check'
</script>

<style scoped>
.detay {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.detay-ust {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.detay-baslik {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 1.05rem;
  font-weight: 700;
  color: var(--text-primary);
  word-break: break-word;
}
.detay-baslik i {
  color: var(--accent);
}
.detay-rozetler {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}
.detay-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 12px;
}
.detay-alan {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.detay-alan.genis {
  grid-column: 1 / -1;
}
.detay-etiket {
  font-size: 11.5px;
  text-transform: uppercase;
  letter-spacing: 0.03em;
  color: var(--text-muted);
}
.red-metin {
  color: var(--danger, #ef4444);
}
h4 {
  margin: 0;
  font-size: 15px;
}
.revizyon-yukleniyor {
  color: var(--text-muted);
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
.zc-adim.taslak .zc-nokta {
  background: var(--bg-muted);
  border-color: var(--border);
  color: var(--text-muted);
}
.zc-adim.uretimde .zc-nokta {
  background: rgba(59, 130, 246, 0.15);
  border-color: #3b82f6;
  color: #3b82f6;
}
.zc-adim.iptal .zc-nokta {
  background: rgba(239, 68, 68, 0.12);
  border-color: #ef4444;
  color: #ef4444;
}
.zc-icerik {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
  flex: 1;
}
.zc-baslik {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--text-primary);
}
.zc-tutar {
  font-size: 12px;
  color: var(--text-secondary);
}
.zc-alt {
  font-size: 0.75rem;
  color: var(--text-muted);
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
