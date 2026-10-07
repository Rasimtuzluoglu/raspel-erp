<template>
  <div class="pos-bolum">
    <button
      type="button"
      class="pos-bolum-baslik katlanir-baslik"
      :aria-expanded="acik"
      :title="acik ? t('hizliSatis.bolumKapat') : t('hizliSatis.bolumAc')"
      @click="$emit('toggle')"
    >
      <span class="katlanir-sol">
        <i class="pi pi-wallet" /> {{ t('hizliSatis.odeme') }}
      </span>
      <i
        class="pi katlanir-ok"
        :class="acik ? 'pi-chevron-down' : 'pi-chevron-right'"
      />
    </button>
    <div
      v-show="acik"
      class="odeme-icerik"
    >
      <SelectButton
        :model-value="odemeDurumu"
        :options="odemeTipleri"
        option-label="label"
        option-value="value"
        class="w-full"
        @update:model-value="$emit('update:odemeDurumu', $event)"
      />
      <div
        v-if="odemeAktif"
        class="odenen-satir"
      >
        <label>{{ t('hizliSatis.odenenTutar') }}</label>
        <InputNumber
          :model-value="odenenTutar"
          :min="0"
          :max="genelToplam"
          mode="currency"
          currency="TRY"
          locale="tr-TR"
          class="w-full"
          @update:model-value="$emit('update:odenenTutar', $event)"
        />
      </div>

      <div
        v-if="odemeAktif && odemeYontemi === 'NAKIT'"
        class="odenen-satir"
      >
        <label>{{ t('hizliSatis.alinanNakit') }}</label>
        <InputNumber
          :model-value="alinanNakit"
          :min="0"
          mode="currency"
          currency="TRY"
          locale="tr-TR"
          class="w-full"
          @update:model-value="$emit('update:alinanNakit', $event)"
        />
        <div
          v-if="paraUstu > 0"
          class="para-ustu"
        >
          <span>{{ t('hizliSatis.paraUstu') }}:</span>
          <strong>{{ formatCurrency(paraUstu) }}</strong>
        </div>
        <div class="hizli-nakit">
          <button
            v-for="n in hizliNakit"
            :key="n"
            type="button"
            class="hizli-nakit-btn"
            @click="$emit('update:alinanNakit', (alinanNakit || 0) + n)"
          >
            {{ n }}
          </button>
          <button
            type="button"
            class="hizli-nakit-btn tam"
            @click="$emit('update:alinanNakit', genelToplam)"
          >
            {{ t('hizliSatis.nakitTam') }}
          </button>
        </div>
      </div>

      <div
        v-if="odemeAktif"
        class="odenen-satir"
      >
        <label>{{ t('hizliSatis.odemeYontemi') }}</label>
        <div class="odeme-yontem-grid">
          <button
            v-for="y in odemeYontemleri"
            :key="y.value"
            type="button"
            class="odeme-yontem-btn"
            :class="{ active: odemeYontemi === y.value }"
            @click="$emit('update:odemeYontemi', y.value)"
          >
            <i :class="y.icon" />
            {{ y.label }}
          </button>
        </div>
      </div>

      <div
        v-if="odemeAktif && odemeYontemi === 'TAKSIT'"
        class="taksit-panel"
        :class="{ 'alan-hata': hatalar && hatalar.taksit }"
      >
        <!-- Taksit dogrulamasi basarisizsa alanlar kirmiziya doner ve
             aciklama satir ici cikar; once yalniz toast vardi. -->
        <small
          v-if="hatalar && hatalar.taksit"
          class="alan-hata-metin"
        >
          <i class="pi pi-exclamation-circle" />
          {{ t('hizliSatis.taksitZorunlu') }}
        </small>
        <div class="odenen-satir">
          <label>{{ t('hizliSatis.taksitKurum') }}</label>
          <InputText
            :model-value="taksitKurum"
            :placeholder="t('hizliSatis.taksitKurumPlaceholder')"
            class="w-full"
            @update:model-value="$emit('update:taksitKurum', $event)"
          />
        </div>
        <div class="odenen-satir">
          <label>{{ t('hizliSatis.taksitTutar') }}</label>
          <InputNumber
            :model-value="taksitTutar"
            :min="0"
            :max="genelToplam"
            mode="currency"
            currency="TRY"
            locale="tr-TR"
            class="w-full"
            @update:model-value="$emit('update:taksitTutar', $event)"
          />
        </div>
        <div class="odenen-satir">
          <label>{{ t('hizliSatis.taksitSayisi') }}</label>
          <InputNumber
            :model-value="taksitSayisi"
            :min="1"
            :max="60"
            show-buttons
            class="w-full"
            @update:model-value="$emit('update:taksitSayisi', $event)"
          />
        </div>
      </div>

      <div class="odenen-satir">
        <label>{{ t('hizliSatis.kasa') }}</label>
        <Dropdown
          :model-value="seciliKasa"
          :options="kasalar"
          option-label="ad"
          option-value="id"
          :placeholder="t('hizliSatis.kasaSecin')"
          class="w-full"
          @update:model-value="$emit('update:seciliKasa', $event)"
        />
      </div>

      <div
        v-if="odemeAktif && (odemeYontemi === 'KART' || odemeYontemi === 'HAVALE')"
        class="odenen-satir"
      >
        <label>{{ odemeYontemi === 'KART' ? t('hizliSatis.kartBankaAktar') : t('hizliSatis.havaleBanka') }}</label>
        <Dropdown
          :model-value="seciliBanka"
          :options="bankalar"
          option-label="ad"
          option-value="id"
          :placeholder="t('hizliSatis.bankaSecin')"
          show-clear
          class="w-full"
          @update:model-value="$emit('update:seciliBanka', $event)"
        />
      </div>

      <div
        v-if="odemeAktif && odemeYontemi === 'KART'"
        class="odenen-satir pos-secim"
      >
        <label>{{ t('hizliSatis.posTerminali') }}</label>
        <Dropdown
          :model-value="seciliPos"
          :options="posTerminalleri"
          option-label="ad"
          option-value="id"
          :placeholder="t('hizliSatis.posSecin')"
          show-clear
          class="w-full"
          @update:model-value="$emit('update:seciliPos', $event)"
        />
        <small
          v-if="seciliPosBilgi"
          class="pos-komisyon-not"
        >
          {{ t('hizliSatis.posKomisyonNot', {
            oran: seciliPosBilgi.komisyonOrani ?? 0,
            komisyon: formatCurrency(hesaplananKomisyon)
          }) }}
        </small>
      </div>

      <div class="odeme-durum">
        <Tag
          :value="odemeDurumText"
          :severity="odemeDurumSeverity"
          class="w-full"
        />
      </div>
      <div
        v-if="kalanTutar > 0"
        class="odeme-kalan"
      >
        <span>{{ t('hizliSatis.kalan') }}:</span>
        <span class="kalan-deger">{{ formatCurrency(kalanTutar) }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { formatCurrency } from '../utils/format.js'

const props = defineProps({
  acik: { type: Boolean, default: true },
  odemeDurumu: { type: String, default: 'tam' },
  odemeTipleri: { type: Array, default: () => [] },
  odemeYontemi: { type: String, default: 'NAKIT' },
  odemeYontemleri: { type: Array, default: () => [] },
  odenenTutar: { type: Number, default: 0 },
  alinanNakit: { type: Number, default: 0 },
  paraUstu: { type: Number, default: 0 },
  hizliNakit: { type: Array, default: () => [] },
  taksitKurum: { type: String, default: '' },
  taksitTutar: { type: Number, default: 0 },
  taksitSayisi: { type: Number, default: 1 },
  seciliKasa: { type: [Number, String, null], default: null },
  kasalar: { type: Array, default: () => [] },
  seciliBanka: { type: [Number, String, null], default: null },
  bankalar: { type: Array, default: () => [] },
  seciliPos: { type: [Number, String, null], default: null },
  posTerminalleri: { type: Array, default: () => [] },
  seciliPosBilgi: { type: Object, default: null },
  hesaplananKomisyon: { type: Number, default: 0 },
  genelToplam: { type: Number, default: 0 },
  kalanTutar: { type: Number, default: 0 },
  odemeDurumText: { type: String, default: '' },
  odemeDurumSeverity: { type: String, default: 'secondary' },
  /** Alan bazli dogrulama hatalari (orn. `{ taksit: true }`). */
  hatalar: { type: Object, default: null }
})

// REDTEAM/Faz5: ayni kosul ("odeme yok degil") alti yerde tekrarlaniyordu.
// Adlandirilmis kosul okunurlugu artirir; DAVRANIS AYNIDIR.
const odemeAktif = computed(() => props.odemeDurumu !== 'yok')

defineEmits([
  'toggle',
  'update:odemeDurumu',
  'update:odemeYontemi',
  'update:odenenTutar',
  'update:alinanNakit',
  'update:taksitKurum',
  'update:taksitTutar',
  'update:taksitSayisi',
  'update:seciliKasa',
  'update:seciliBanka',
  'update:seciliPos'
])

const { t } = useI18n()
</script>
