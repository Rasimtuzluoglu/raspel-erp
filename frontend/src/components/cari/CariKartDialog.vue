<template>
  <Dialog
    :visible="visible"
    :modal="true"
    :style="{ width: '820px', maxWidth: '96vw' }"
    :draggable="false"
    @update:visible="kapat"
  >
    <template #header>
      <div class="hareket-baslik">
        <i class="pi pi-list" />
        <span>{{ t('cariHesaplar.hareketlerBaslik') }}</span>
      </div>
    </template>
    <CariHareketBasligi
      :cari="cari"
      :ozet="ozet"
      @ekstre="ekstreEmit"
    />

    <CariHareketTablosu
      :hareketler="hareketler"
      :bakiye="bakiye"
      :yukleniyor="yukleniyor"
    >
      <TabView
        scrollable
        class="cari-sekmeler"
      >
        <TabPanel :header="t('cariHesaplar.gecmisFaturalarTab')">
          <CariFaturalarSekmesi
            :faturalar="faturalar"
            @fatura-detay="faturaDetayEmit"
          />
        </TabPanel>

        <TabPanel :header="t('cariHesaplar.ozelFiyatlar')">
          <CariOzelFiyatlarSekmesi
            :ozel-fiyatlar="ozelFiyatlar"
            @ekle="ozelFiyatEkleEmit"
            @sil="ozelFiyatSilEmit"
          />
        </TabPanel>

        <TabPanel :header="t('cariHesaplar.gecmisteAldigiUrunler')">
          <CariUrunGecmisiSekmesi
            :son-urunler="sonUrunler"
            :secili="seciliUrun"
            @goster="urunGosterEmit"
            @kapat="urunKapatEmit"
          />
        </TabPanel>

        <TabPanel :header="t('cariHesaplar.gorusmeNotlari')">
          <CariNotlarSekmesi
            :notlar="notlar"
            @ekle="notEkleEmit"
            @sil="notSilEmit"
          />
        </TabPanel>
      </TabView>
    </CariHareketTablosu>

    <template #footer>
      <Button
        :label="t('stoklar.kapat')"
        icon="pi pi-times"
        @click="kapat"
      />
    </template>
  </Dialog>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import CariHareketBasligi from './CariHareketBasligi.vue'
import CariHareketTablosu from './CariHareketTablosu.vue'
import CariFaturalarSekmesi from './CariFaturalarSekmesi.vue'
import CariOzelFiyatlarSekmesi from './CariOzelFiyatlarSekmesi.vue'
import CariUrunGecmisiSekmesi from './CariUrunGecmisiSekmesi.vue'
import CariNotlarSekmesi from './CariNotlarSekmesi.vue'

defineProps({
  visible: { type: Boolean, default: false },
  cari: { type: Object, default: null },
  ozet: { type: Object, default: null },
  hareketler: { type: Array, default: () => [] },
  bakiye: { type: Array, default: () => [] },
  yukleniyor: { type: Boolean, default: false },
  faturalar: { type: Array, default: () => [] },
  ozelFiyatlar: { type: Array, default: () => [] },
  sonUrunler: { type: Array, default: () => [] },
  seciliUrun: { type: Object, default: null },
  notlar: { type: Array, default: () => [] }
})

const emit = defineEmits([
  'update:visible',
  'ekstre',
  'fatura-detay',
  'ozel-fiyat-ekle',
  'ozel-fiyat-sil',
  'urun-goster',
  'urun-kapat',
  'not-ekle',
  'not-sil'
])

const { t } = useI18n()

const kapat = () => emit('update:visible', false)
const ekstreEmit = (aralik) => emit('ekstre', aralik)
const faturaDetayEmit = (fatura) => emit('fatura-detay', fatura)
const ozelFiyatEkleEmit = (deger) => emit('ozel-fiyat-ekle', deger)
const ozelFiyatSilEmit = (fiyat) => emit('ozel-fiyat-sil', fiyat)
const urunGosterEmit = (urun) => emit('urun-goster', urun)
const urunKapatEmit = () => emit('urun-kapat')
const notEkleEmit = (not) => emit('not-ekle', not)
const notSilEmit = (not) => emit('not-sil', not)
</script>

<style scoped>
.hareket-baslik {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 700;
  font-size: 15px;
}
.hareket-baslik i {
  color: var(--accent);
}
</style>
