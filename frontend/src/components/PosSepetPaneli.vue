<template>
  <div
    ref="listeRef"
    class="pos-bolum sepet-bolum"
  >
    <div class="pos-bolum-baslik sepet-baslik">
      <button
        type="button"
        class="sepet-baslik-toggle"
        :aria-expanded="acik"
        @click="$emit('toggle')"
      >
        {{ t('hizliSatis.siparisOzeti', { n: sepet ? sepet.length : 0 }) }}
      </button>
      <div class="sepet-baslik-btnler">
        <Button
          v-if="sepet && sepet.length"
          icon="pi pi-save"
          class="p-button-rounded p-button-text p-button-sm"
          :title="t('hizliSatis.sepetiKaydet')"
          @click="$emit('kaydet')"
        />
        <Button
          v-if="kayitliSepetVar && sepet.length === 0"
          icon="pi pi-folder-open"
          class="p-button-rounded p-button-text p-button-sm"
          :title="t('hizliSatis.kayitliSepetiYukle')"
          @click="$emit('yukle')"
        />
        <Button
          v-if="sepet && sepet.length"
          :label="t('hizliSatis.temizle')"
          icon="pi pi-trash"
          severity="danger"
          size="small"
          @click.stop="$emit('temizle')"
        />
        <button
          type="button"
          class="katlanir-ikon-btn"
          :aria-expanded="acik"
          :title="acik ? t('hizliSatis.bolumKapat') : t('hizliSatis.bolumAc')"
          :aria-label="acik ? t('hizliSatis.bolumKapat') : t('hizliSatis.bolumAc')"
          @click="$emit('toggle')"
        >
          <i
            class="pi katlanir-ok"
            :class="acik ? 'pi-chevron-down' : 'pi-chevron-right'"
          />
        </button>
      </div>
    </div>
    <div
      v-if="geriAlSepet"
      class="geri-al-bar"
    >
      <span><i class="pi pi-trash" /> {{ t('hizliSatis.sepetTemizlendi') }}</span>
      <button
        type="button"
        @click="$emit('geri-al')"
      >
        <i class="pi pi-undo" /> {{ t('hizliSatis.geriAl') }}
      </button>
    </div>
    <div
      v-show="acik"
      class="sepet-icerik"
    >
      <div
        v-if="sepet && sepet.length === 0"
        class="sepet-bos"
      >
        {{ t('hizliSatis.sepeteUrunEkle') }}
      </div>
      <div
        v-for="(item, idx) in sepet"
        :key="item.id"
        class="sepet-item"
        :class="{ 'aktif-satir': aktifSatir === idx, 'yeni-satir': vurguluId === item.id, 'surukleniyor': suruklenenIdx === idx }"
        @click="$emit('satir-sec', idx)"
        @dragover.prevent="$emit('surukleme-uzerine', idx)"
        @drop.prevent="$emit('surukleme-birak', idx)"
      >
        <div class="sepet-ust">
          <span
            class="sepet-tutamac"
            draggable="true"
            :title="t('hizliSatis.siralaTutamac')"
            @dragstart="$emit('surukleme-basla', idx)"
            @dragend="$emit('surukleme-bitir')"
          ><i class="pi pi-bars" /></span>
          <span
            class="sepet-kod"
            :title="item.barkod"
          >{{ item.barkod || item.stokKodu }}</span>
          <span class="sepet-ad">{{ item.ad }}</span>
          <span class="sepet-tutar">{{ formatCurrency(item.miktar * item.fiyat) }}</span>
          <button
            type="button"
            class="sepet-sil"
            :title="t('hizliSatis.kaldir')"
            @click="$emit('sil', idx)"
          >
            <i class="pi pi-times" />
          </button>
        </div>
        <div class="sepet-kontroller">
          <div class="sepet-adet-grup">
            <button
              type="button"
              class="adet-btn"
              :aria-label="t('hizliSatis.miktarAzalt')"
              :title="t('hizliSatis.miktarAzalt')"
              @click="$emit('miktar-azalt', idx)"
            >
              −
            </button>
            <input
              v-model.number="item.miktar"
              type="number"
              min="1"
              class="sepet-adet-input"
              :aria-label="t('hizliSatis.adet')"
              :title="t('hizliSatis.adet')"
            >
            <button
              type="button"
              class="adet-btn"
              :aria-label="t('hizliSatis.miktarArtir')"
              :title="t('hizliSatis.miktarArtir')"
              @click="$emit('miktar-artir', idx)"
            >
              +
            </button>
          </div>
          <select
            v-model="item.fiyatTipi"
            class="fiyat-tip-select"
            @change="$emit('fiyat-tipi-degisti', item)"
          >
            <option
              v-for="f in item.fiyatlar"
              :key="f.ad"
              :value="f.ad"
            >
              {{ f.ad }}
            </option>
          </select>
          <input
            v-model.number="item.fiyat"
            type="number"
            step="0.01"
            class="fiyat-giris-input"
            :title="t('hizliSatis.birimFiyati')"
          >
        </div>
        <div
          v-if="item.sonAldigiFiyat"
          class="sepet-son-alis"
        >
          <i class="pi pi-history" />
          {{ musteriAdi || $t('hizliSatis.musteri') }} {{ $t('hizliSatis.sonAlisOncesi') }}
          <strong>{{ formatCurrency(item.sonAldigiFiyat) }}</strong>
          {{ item.sonAldigiTarih ? '(' + formatDate(item.sonAldigiTarih) + ')' : '' }} {{ $t('hizliSatis.sonAlisSonrasi') }}
        </div>
      </div>
      <hr class="ozet-ayrac">
      <button
        type="button"
        class="ozet-detay-btn"
        :aria-expanded="detayAcik"
        @click="$emit('detay-toggle')"
      >
        <i
          class="pi katlanir-ok"
          :class="detayAcik ? 'pi-chevron-down' : 'pi-chevron-right'"
        />
        {{ detayAcik ? t('hizliSatis.detayGizle') : t('hizliSatis.detayGoster') }}
      </button>
      <div
        v-show="detayAcik"
        class="ozet-satir"
      >
        <span>{{ t('hizliSatis.toplamFt3') }}</span>
        <span>{{ toplamFt3.toFixed(2) }} ft³</span>
      </div>
      <div class="ozet-satir ozet-indirim-satir">
        <span>{{ t('hizliSatis.indirim') }}</span>
        <div class="ozet-indirim">
          <SelectButton
            :model-value="indirimTipi"
            :options="indirimTipleri"
            option-label="label"
            option-value="value"
            @update:model-value="$emit('update:indirimTipi', $event)"
          />
          <InputNumber
            :model-value="indirimDegeri"
            :min="0"
            :max="indirimTipi === 'yuzde' ? 100 : toplam"
            :suffix="indirimTipi === 'yuzde' ? '%' : ' ₺'"
            class="indirim-input"
            @update:model-value="$emit('update:indirimDegeri', $event)"
          />
        </div>
      </div>
      <div class="ozet-satir ozet-genel">
        <span>{{ t('hizliSatis.genelToplam') }}</span>
        <span class="genel-toplam-deger">{{ formatCurrency(genelToplam) }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'
import { formatCurrency, formatDate } from '../utils/format.js'

const props = defineProps({
  sepet: { type: Array, required: true },
  acik: { type: Boolean, default: true },
  aktifSatir: { type: Number, default: -1 },
  vurguluId: { type: [Number, String, null], default: null },
  suruklenenIdx: { type: Number, default: -1 },
  geriAlSepet: { type: Boolean, default: false },
  kayitliSepetVar: { type: Boolean, default: false },
  detayAcik: { type: Boolean, default: false },
  toplamFt3: { type: Number, default: 0 },
  toplam: { type: Number, default: 0 },
  genelToplam: { type: Number, default: 0 },
  musteriAdi: { type: String, default: '' },
  indirimTipi: { type: String, default: 'tutar' },
  indirimDegeri: { type: Number, default: 0 },
  indirimTipleri: { type: Array, default: () => [] }
})

defineEmits([
  'toggle', 'kaydet', 'yukle', 'temizle', 'geri-al',
  'sil', 'miktar-azalt', 'miktar-artir', 'satir-sec',
  'surukleme-basla', 'surukleme-uzerine', 'surukleme-birak', 'surukleme-bitir',
  'fiyat-tipi-degisti', 'detay-toggle',
  'update:indirimTipi', 'update:indirimDegeri'
])

void props
const { t } = useI18n()
const listeRef = ref(null)

// Klavye ile aktif satira kaydirma icin liste elemani disariya acilir.
defineExpose({ listeRef })

onMounted(() => nextTick(() => {}))
</script>
