<template>
  <Drawer
    :visible="visible"
    position="right"
    :header="t('cekSenet.detayBaslik')"
    :style="{ width: '520px' }"
    :breakpoints="{ '575px': '92vw' }"
    @update:visible="$emit('update:visible', $event)"
  >
    <div
      v-if="yukleniyor"
      class="detay-iskelet"
      aria-busy="true"
    >
      <SkeletonLoader
        :count="6"
        height="42px"
      />
    </div>

    <div
      v-else-if="kayit"
      class="detay"
    >
      <div class="detay-ust">
        <div class="detay-baslik">
          <i :class="kayit.tur === 'CEK' ? 'pi pi-credit-card' : 'pi pi-file-edit'" />
          <span>{{ kayit.cekNo || t('cekSenet.detayBaslik') }}</span>
        </div>
        <div class="detay-rozetler">
          <Tag
            :value="turAdi(kayit.tur)"
            :severity="kayit.tur === 'CEK' ? 'info' : 'warn'"
          />
          <Tag
            :value="durumAdi(kayit.durum)"
            :severity="durumSeverity(kayit.durum)"
          />
        </div>
      </div>

      <section class="detay-bolum">
        <h4>{{ t('cekSenet.bilgiler') }}</h4>
        <div class="detay-grid">
          <div class="detay-alan">
            <span class="detay-etiket">{{ t('cekSenet.cariHesap') }}</span>
            <span>{{ kayit.cariHesapAdi || '-' }}</span>
          </div>
          <div class="detay-alan">
            <span class="detay-etiket">{{ t('cekSenet.banka') }}</span>
            <span>{{ kayit.bankaAdi || '-' }}</span>
          </div>
          <div class="detay-alan">
            <span class="detay-etiket">{{ t('cekSenet.sube') }}</span>
            <span>{{ kayit.sube || '-' }}</span>
          </div>
          <div class="detay-alan">
            <span class="detay-etiket">{{ t('cekSenet.cekNo') }}</span>
            <span>{{ kayit.cekNo || '-' }}</span>
          </div>
          <div class="detay-alan">
            <span class="detay-etiket">{{ t('cekSenet.hesapNo') }}</span>
            <span>{{ kayit.hesapNo || '-' }}</span>
          </div>
          <div class="detay-alan">
            <span class="detay-etiket">{{ t('cekSenet.vade') }}</span>
            <span>{{ formatTarih(kayit.vadeTarihi) }}</span>
          </div>
          <div class="detay-alan">
            <span class="detay-etiket">{{ t('cekSenet.kesinmeTarihi') }}</span>
            <span>{{ kayit.kesinmeTarihi ? formatTarih(kayit.kesinmeTarihi) : '-' }}</span>
          </div>
          <div class="detay-alan">
            <span class="detay-etiket">{{ t('common.amount') }}</span>
            <strong class="gizli-veri">{{ formatCurrency(kayit.tutar) }}</strong>
          </div>
          <div
            v-if="kayit.aciklama"
            class="detay-alan genis"
          >
            <span class="detay-etiket">{{ t('common.description') }}</span>
            <span>{{ kayit.aciklama }}</span>
          </div>
        </div>
      </section>

      <section class="detay-bolum">
        <h4>{{ t('cekSenet.cariFatura') }}</h4>
        <div class="detay-grid">
          <div class="detay-alan">
            <span class="detay-etiket">{{ t('cekSenet.cariHesap') }}</span>
            <span>{{ kayit.cariHesapAdi || '-' }}</span>
          </div>
          <div class="detay-alan">
            <span class="detay-etiket">{{ t('cekSenet.fatura') }}</span>
            <span>{{ kayit.faturaNo || '-' }}</span>
          </div>
        </div>
      </section>

      <section
        v-if="ciroTeminatVar"
        class="detay-bolum"
      >
        <h4>{{ t('cekSenet.ciroTeminat') }}</h4>
        <div class="detay-grid">
          <div
            v-if="kayit.ciroCariAd"
            class="detay-alan"
          >
            <span class="detay-etiket">{{ t('cekSenet.ciroCari') }}</span>
            <span>{{ kayit.ciroCariAd }}</span>
          </div>
          <div
            v-if="kayit.ciroTarihi"
            class="detay-alan"
          >
            <span class="detay-etiket">{{ t('cekSenet.ciroTarihi') }}</span>
            <span>{{ formatTarih(kayit.ciroTarihi) }}</span>
          </div>
          <div
            v-if="kayit.ciroNotu"
            class="detay-alan genis"
          >
            <span class="detay-etiket">{{ t('cekSenet.ciroNotu') }}</span>
            <span>{{ kayit.ciroNotu }}</span>
          </div>
          <div
            v-if="kayit.teminatTarihi"
            class="detay-alan"
          >
            <span class="detay-etiket">{{ t('cekSenet.teminatTarihi') }}</span>
            <span>{{ formatTarih(kayit.teminatTarihi) }}</span>
          </div>
          <div
            v-if="kayit.teminatAciklama"
            class="detay-alan genis"
          >
            <span class="detay-etiket">{{ t('cekSenet.teminatAciklama') }}</span>
            <span>{{ kayit.teminatAciklama }}</span>
          </div>
          <div
            v-if="kayit.protestoTarihi"
            class="detay-alan"
          >
            <span class="detay-etiket">{{ t('cekSenet.protestoTarihi') }}</span>
            <span>{{ formatTarih(kayit.protestoTarihi) }}</span>
          </div>
        </div>
      </section>

      <section
        v-if="tahsilVar"
        class="detay-bolum"
      >
        <h4>{{ t('cekSenet.tahsilBaglari') }}</h4>
        <div class="detay-grid">
          <div class="detay-alan">
            <span class="detay-etiket">{{ t('cekSenet.tahsilatHareketi') }}</span>
            <span>{{ kayit.tahsilatHareketId ? '#' + kayit.tahsilatHareketId : '-' }}</span>
          </div>
          <div class="detay-alan">
            <span class="detay-etiket">{{ t('cekSenet.kasa') }}</span>
            <span>{{ kayit.kasaAd || '-' }}</span>
          </div>
          <div class="detay-alan">
            <span class="detay-etiket">{{ t('cekSenet.bankaHesabi') }}</span>
            <span>{{ kayit.bankaAd || '-' }}</span>
          </div>
        </div>
      </section>

      <section class="detay-bolum">
        <h4>{{ t('cekSenet.zamanCizelgesi') }}</h4>
        <ol
          v-if="gecmis.length"
          class="zaman-cizelgesi"
        >
          <li
            v-for="(olay, i) in gecmis"
            :key="olay.id || i"
            class="zc-adim"
            :class="islemSinif(olay.islem)"
          >
            <span class="zc-nokta"><i :class="islemIkon(olay.islem)" /></span>
            <div class="zc-icerik">
              <div class="zc-baslik">
                <span>{{ islemAdi(olay.islem) }}</span>
                <small
                  v-if="olay.eskiDurum || olay.yeniDurum"
                  class="zc-gecis"
                >
                  {{ olay.eskiDurum ? durumAdi(olay.eskiDurum) : '—' }}
                  <i class="pi pi-arrow-right" />
                  {{ olay.yeniDurum ? durumAdi(olay.yeniDurum) : '—' }}
                </small>
              </div>
              <small
                v-if="olay.aciklama"
                class="zc-alt"
              >{{ olay.aciklama }}</small>
              <small class="zc-alt">{{ formatTarihSaat(olay.olusturmaTarihi) }}</small>
            </div>
          </li>
        </ol>
        <p
          v-else
          class="muted"
        >
          {{ t('common.noData') }}
        </p>
      </section>
    </div>

    <template #footer>
      <div
        v-if="kayit"
        class="detay-aksiyonlar"
      >
        <Button
          v-if="aksiyonVar('tahsil')"
          :label="t('cekSenet.tahsilEt')"
          icon="pi pi-check"
          severity="success"
          size="small"
          @click="$emit('tahsil', kayit)"
        />
        <Button
          v-if="aksiyonVar('ciro')"
          :label="t('cekSenet.ciroEt')"
          icon="pi pi-sync"
          size="small"
          @click="$emit('ciro', kayit)"
        />
        <Button
          v-if="aksiyonVar('teminat')"
          :label="t('cekSenet.teminatVer')"
          icon="pi pi-shield"
          size="small"
          outlined
          @click="$emit('teminat', kayit)"
        />
        <Button
          v-if="aksiyonVar('teminatIade')"
          :label="t('cekSenet.teminatIade')"
          icon="pi pi-shield"
          size="small"
          outlined
          @click="$emit('teminatIade', kayit)"
        />
        <Button
          v-if="aksiyonVar('protesto')"
          :label="t('cekSenet.protestoEt')"
          icon="pi pi-exclamation-triangle"
          severity="danger"
          size="small"
          outlined
          @click="$emit('protesto', kayit)"
        />
        <Button
          v-if="aksiyonVar('ciroIade')"
          :label="t('cekSenet.ciroIade')"
          icon="pi pi-undo"
          size="small"
          outlined
          @click="$emit('ciroIade', kayit)"
        />
        <Button
          v-if="aksiyonVar('iadeAl')"
          :label="t('cekSenet.iadeAl')"
          icon="pi pi-inbox"
          size="small"
          outlined
          @click="$emit('iadeAl', kayit)"
        />
        <Button
          v-if="aksiyonVar('tahsilIptal')"
          :label="t('cekSenet.tahsilIptal')"
          icon="pi pi-times"
          severity="danger"
          size="small"
          @click="$emit('tahsilIptal', kayit)"
        />
        <Button
          v-if="aksiyonVar('duzenle')"
          :label="t('common.edit')"
          icon="pi pi-pencil"
          size="small"
          text
          @click="$emit('duzenle', kayit)"
        />
        <Button
          v-if="aksiyonVar('sil')"
          :label="t('common.delete')"
          icon="pi pi-trash"
          severity="danger"
          size="small"
          text
          @click="$emit('sil', kayit)"
        />
      </div>
    </template>
  </Drawer>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { formatCurrency, formatTarih, formatTarihSaat } from '../utils/format.js'

const props = defineProps({
  visible: { type: Boolean, default: false },
  kayit: { type: Object, default: null },
  yukleniyor: { type: Boolean, default: false }
})

defineEmits([
  'update:visible',
  'duzenle',
  'sil',
  'tahsil',
  'tahsilIptal',
  'ciro',
  'ciroIade',
  'iadeAl',
  'teminat',
  'teminatIade',
  'protesto'
])

const { t } = useI18n()

const gecmis = computed(() => props.kayit?.gecmis || [])

const ciroTeminatVar = computed(() => {
  const k = props.kayit
  if (!k) return false
  return !!(k.ciroCariAd || k.ciroTarihi || k.ciroNotu || k.teminatTarihi || k.teminatAciklama || k.protestoTarihi)
})

const tahsilVar = computed(() => {
  const k = props.kayit
  if (!k) return false
  return !!(k.tahsilatHareketId || k.kasaAd || k.bankaAd)
})

const DURUM_ANAHTAR = {
  PORTFOY: 'durumPortfoy',
  TEMINATTA: 'durumTeminatta',
  CIRO_EDILDI: 'durumCiroEdildi',
  TAHSIL_EDILDI: 'durumTahsilEdildi',
  PROTESTO: 'durumProtesto',
  IADE: 'durumIade',
  // Legacy kayitlar
  ODENDI: 'durumTahsilEdildi'
}

const ISLEM_ANAHTAR = {
  OLUSTUR: 'islemOlustur',
  GUNCELLE: 'islemGuncelle',
  TEMINAT: 'islemTeminat',
  TEMINAT_IADE: 'islemTeminatIade',
  CIRO: 'islemCiro',
  CIRO_IADE: 'islemCiroIade',
  IADE_AL: 'islemIadeAl',
  TAHSIL: 'islemTahsil',
  TAHSIL_IPTAL: 'islemTahsilIptal',
  PROTESTO: 'islemProtesto'
}

const durumAdi = (d) => (DURUM_ANAHTAR[d] ? t(`cekSenet.${DURUM_ANAHTAR[d]}`) : d || '-')

const durumSeverity = (d) =>
  ({
    PORTFOY: 'info',
    TEMINATTA: 'warn',
    CIRO_EDILDI: 'secondary',
    TAHSIL_EDILDI: 'success',
    PROTESTO: 'danger',
    IADE: 'warn',
    ODENDI: 'success'
  })[d] || 'secondary'

const turAdi = (tur) => (tur === 'CEK' ? t('cekSenet.cek') : t('cekSenet.senet'))

const islemAdi = (i) => (ISLEM_ANAHTAR[i] ? t(`cekSenet.${ISLEM_ANAHTAR[i]}`) : i || '-')

const islemSinif = (i) => {
  if (i === 'TAHSIL') return 'tamam'
  if (i === 'TAHSIL_IPTAL' || i === 'PROTESTO') return 'iptal'
  if (i === 'CIRO' || i === 'CIRO_IADE' || i === 'TEMINAT' || i === 'TEMINAT_IADE' || i === 'IADE_AL') {
    return 'uretimde'
  }
  return 'taslak'
}

const islemIkon = (i) =>
  ({
    OLUSTUR: 'pi pi-plus',
    GUNCELLE: 'pi pi-pencil',
    TEMINAT: 'pi pi-shield',
    TEMINAT_IADE: 'pi pi-shield',
    CIRO: 'pi pi-sync',
    CIRO_IADE: 'pi pi-undo',
    IADE_AL: 'pi pi-inbox',
    TAHSIL: 'pi pi-check',
    TAHSIL_IPTAL: 'pi pi-times',
    PROTESTO: 'pi pi-exclamation-triangle'
  })[i] || 'pi pi-circle'

// Duruma göre hangi aksiyon gösterilir?
const AKSIYONLAR = {
  tahsil: ['PORTFOY', 'TEMINATTA'],
  ciro: ['PORTFOY'],
  teminat: ['PORTFOY'],
  teminatIade: ['TEMINATTA'],
  protesto: ['PORTFOY', 'TEMINATTA'],
  tahsilIptal: ['TAHSIL_EDILDI'],
  ciroIade: ['CIRO_EDILDI'],
  iadeAl: ['IADE'],
  duzenle: ['PORTFOY', 'TEMINATTA', 'CIRO_EDILDI', 'IADE'],
  sil: ['PORTFOY', 'TEMINATTA', 'CIRO_EDILDI', 'IADE']
}

const aksiyonVar = (ad) => {
  const durum = props.kayit?.durum
  if (!durum) return false
  return (AKSIYONLAR[ad] || []).includes(durum)
}
</script>

<style scoped>
.detay {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.detay-iskelet {
  color: var(--text-muted);
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
.detay-bolum {
  display: flex;
  flex-direction: column;
  gap: 10px;
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
  min-width: 0;
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
h4 {
  margin: 0;
  font-size: 15px;
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
  background: var(--bg-muted);
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
  flex-wrap: wrap;
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--text-primary);
}
.zc-gecis {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 11.5px;
  font-weight: 500;
  color: var(--text-muted);
}
.zc-gecis i {
  font-size: 9px;
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
