<template>
  <Drawer
    :visible="visible"
    position="right"
    :header="t('masraflar.detay')"
    :style="{ width: '460px', maxWidth: '96vw' }"
    @update:visible="$emit('update:visible', $event)"
  >
    <div
      v-if="masraf"
      class="detay"
    >
      <div class="detay-ust">
        <div class="detay-baslik">
          <i class="pi pi-receipt" />
          <span>{{ kategoriEtiket(masraf.kategori) }}</span>
        </div>
        <strong class="detay-tutar">{{ para(masraf.tutar) }}</strong>
      </div>

      <div class="detay-grid">
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('common.date') }}</span>
          <span>{{ tarih(masraf.tarih) }}</span>
        </div>
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('masraflar.belgeNo') }}</span>
          <span>{{ masraf.belgeNo || '-' }}</span>
        </div>
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('masraflar.matrah') }}</span>
          <span>{{ para(masraf.matrah) }}</span>
        </div>
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('masraflar.kdvTutar') }}</span>
          <span>{{ para(masraf.kdvTutar) }} ({{ masraf.kdvOrani ?? 0 }}%)</span>
        </div>
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('masraflar.odemeYontemi') }}</span>
          <span>{{ odemeEtiket(masraf.odemeYontemi) }}</span>
        </div>
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('masraflar.odemeHesabi') }}</span>
          <span>{{ hesapAdi || '-' }}</span>
        </div>
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('masraflar.cariHesap') }}</span>
          <span>{{ cariAdi || '-' }}</span>
        </div>
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('masraflar.olusturma') }}</span>
          <span>{{ tarihSaat(masraf.olusturmaTarihi) }}</span>
        </div>
        <div class="detay-alan genis">
          <span class="detay-etiket">{{ t('common.description') }}</span>
          <span>{{ masraf.aciklama || '-' }}</span>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="detay-aksiyonlar">
        <Button
          v-if="masraf && duzenlenebilir"
          :label="t('common.edit')"
          icon="pi pi-pencil"
          @click="$emit('duzenle', masraf)"
        />
        <Button
          v-if="masraf && silinebilir"
          :label="t('common.delete')"
          icon="pi pi-trash"
          severity="danger"
          outlined
          @click="$emit('sil', masraf)"
        />
      </div>
    </template>
  </Drawer>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import { formatTarih as tarih, formatTarihSaat as tarihSaat, formatCurrency as para } from '../utils/format.js'

defineProps({
  visible: { type: Boolean, default: false },
  masraf: { type: Object, default: null },
  cariAdi: { type: String, default: '' },
  hesapAdi: { type: String, default: '' },
  duzenlenebilir: { type: Boolean, default: false },
  silinebilir: { type: Boolean, default: false }
})

defineEmits(['update:visible', 'duzenle', 'sil'])

const { t } = useI18n()

const KATEGORI_ANAHTARLARI = {
  YAKIT: 'kategoriYakit',
  YEMEK: 'kategoriYemek',
  KONAKLAMA: 'kategoriKonaklama',
  ULASIM: 'kategoriUlasim',
  MALZEME: 'kategoriMalzeme',
  DIGER: 'kategoriDiger'
}

const kategoriEtiket = (kod) => {
  if (!kod) return '-'
  const anahtar = KATEGORI_ANAHTARLARI[kod]
  return anahtar ? t(`masraflar.${anahtar}`) : kod
}

const odemeEtiket = (kod) => {
  if (!kod) return t('masraflar.odemeYok')
  return { NAKIT: t('masraflar.odemeNakit'), HAVALE: t('masraflar.odemeHavale'), KART: t('masraflar.odemeKart') }[kod] || kod
}
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
.detay-baslik {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 1.05rem;
  font-weight: 700;
  color: var(--text-primary);
}
.detay-baslik i {
  color: var(--accent);
}
.detay-tutar {
  font-size: 1.1rem;
  color: var(--accent);
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
.detay-aksiyonlar {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
</style>
