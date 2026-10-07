<template>
  <div>
    <!-- Adim 1: Musteri -->
    <div class="pos-bolum">
      <button
        type="button"
        class="pos-bolum-baslik katlanir-baslik"
        :aria-expanded="musteriAcik"
        :title="musteriAcik ? t('hizliSatis.bolumKapat') : t('hizliSatis.bolumAc')"
        @click="$emit('update:musteriAcik', !musteriAcik)"
      >
        <span class="katlanir-sol">
          <i class="pi pi-user" /> {{ t('hizliSatis.musteri') }}
        </span>
        <i
          class="pi katlanir-ok"
          :class="musteriAcik ? 'pi-chevron-down' : 'pi-chevron-right'"
        />
      </button>
      <div
        v-show="musteriAcik"
        class="customer-field"
      >
        <SelectButton
          :model-value="musteriModu"
          :options="musteriModlari"
          option-label="label"
          option-value="value"
          :allow-empty="false"
          class="w-full musteri-modu"
          @update:model-value="$emit('update:musteriModu', $event)"
        />
        <template v-if="musteriModu === 'musteri'">
          <AutoComplete
            ref="musteriAutoRef"
            :model-value="musteriGiris"
            :suggestions="musteriOnerileri"
            option-label="ad"
            :placeholder="t('hizliSatis.musteriAra')"
            class="w-full"
            @update:model-value="$emit('update:musteriGiris', $event)"
            @complete="$emit('musteri-ara', $event)"
            @option-select="$emit('musteri-sec', $event)"
          >
            <template #option="slotProps">
              <div class="musteri-option">
                {{ slotProps.option.ad }}
                <span class="musteri-option-detay">{{
                  slotProps.option.vergiNo || slotProps.option.telefon
                }}</span>
              </div>
            </template>
          </AutoComplete>
          <div
            v-if="seciliMusteri"
            class="secili-musteri-chip"
          >
            <i class="pi pi-user" />
            <span class="secili-musteri-ad">{{ seciliMusteri.ad }}</span>
            <button
              type="button"
              class="secili-musteri-sil"
              :title="t('hizliSatis.musteriyiKaldir')"
              @click="$emit('musteri-temizle')"
            >
              <i class="pi pi-times" />
            </button>
          </div>
          <div
            v-if="musteriBakiyeUyarisi"
            class="musteri-bakiye-uyari"
            :class="musteriBakiyeUyarisi.seviye"
          >
            <i :class="musteriBakiyeUyarisi.seviye === 'danger' ? 'pi pi-exclamation-triangle' : 'pi pi-info-circle'" />
            {{ musteriBakiyeUyarisi.mesaj }}
          </div>
          <div
            v-if="degisimIadeId"
            class="degisim-bilgi"
          >
            <i class="pi pi-shopping-cart" />
            <span>{{ t('hizliSatis.degisimBilgi', { id: degisimIadeId }) }}</span>
            <button
              type="button"
              class="degisim-kapat"
              :title="t('common.close')"
              @click="$emit('degisim-kapat')"
            >
              <i class="pi pi-times" />
            </button>
          </div>
          <Button
            :label="t('hizliSatis.yeni')"
            severity="secondary"
            size="small"
            @click="$emit('yeni-musteri')"
          />
        </template>
      </div>
    </div>

    <!-- Adim 2 (opsiyonel): Teslimat -->
    <div class="pos-bolum">
      <button
        type="button"
        class="pos-bolum-baslik katlanir-baslik"
        :aria-expanded="teslimatAcik"
        :title="teslimatAcik ? t('hizliSatis.bolumKapat') : t('hizliSatis.bolumAc')"
        @click="$emit('update:teslimatAcik', !teslimatAcik)"
      >
        <span class="katlanir-sol">
          <i
            class="pi katlanir-ok"
            :class="teslimatAcik ? 'pi-chevron-down' : 'pi-chevron-right'"
          />
          <i class="pi pi-truck" /> {{ t('hizliSatis.teslimat') }}
        </span>
        <span
          v-if="!teslimatAcik && seciliSofor"
          class="katlanir-rozet"
        >{{ seciliSofor.ad }}</span>
      </button>
      <template v-if="teslimatAcik">
        <p class="teslimat-ipucu">
          {{ t('hizliSatis.teslimatIpucu') }}
        </p>
        <div class="teslim-eden-alan">
          <label for="hizli-teslim-sofor">{{ t('hizliSatis.sofor') }}</label>
          <Dropdown
            id="hizli-teslim-sofor"
            :model-value="seciliSofor"
            :options="soforler"
            option-label="ad"
            :loading="soforlerYukleniyor"
            filter
            :show-clear="true"
            :placeholder="t('hizliSatis.soforSecin')"
            class="w-full"
            @update:model-value="$emit('update:seciliSofor', $event)"
          >
            <template #option="s">
              <div class="personel-opsiyon">
                <i class="pi pi-user" />
                <span>{{ s.option.ad }}</span>
                <span
                  v-if="s.option.rol && s.option.rol !== 'DRIVER'"
                  class="sofor-rol-uyari"
                  :title="t('hizliSatis.soforRolUyari')"
                >
                  <i class="pi pi-exclamation-triangle" />
                </span>
                <span
                  v-if="s.option.bekleyenTeslimatSayisi"
                  class="sofor-bekleyen"
                >{{ s.option.bekleyenTeslimatSayisi }}</span>
              </div>
            </template>
          </Dropdown>
        </div>
        <div
          v-if="seciliSofor"
          class="teslim-eden-alan"
          :class="{ 'alan-hata': hatalar && hatalar.teslimatAdresi }"
        >
          <label for="hizli-teslim-adres">
            {{ t('hizliSatis.teslimatAdresi') }} <span class="zorunlu">*</span>
          </label>
          <InputText
            id="hizli-teslim-adres"
            :model-value="teslimatAdresi"
            :placeholder="t('hizliSatis.adresPlaceholder')"
            :invalid="!!(hatalar && hatalar.teslimatAdresi)"
            class="w-full"
            @update:model-value="$emit('update:teslimatAdresi', $event)"
          />
          <!-- Dogrulama hatasi satir ici gosterilir. Once yalnizca toast
               cikiyordu; kasiyer hangi alanin eksik oldugunu ekranda
               goremiyordu (panel kapaliyken alan HIC gorunmuyordu). -->
          <small
            v-if="hatalar && hatalar.teslimatAdresi"
            class="alan-hata-metin"
          >
            <i class="pi pi-exclamation-circle" />
            {{ t('hizliSatis.teslimatAdresiGerekli') }}
          </small>
        </div>
        <div
          v-if="seciliSofor"
          class="teslim-eden-alan"
        >
          <label>{{ t('hizliSatis.teslimDurumu') }}</label>
          <SelectButton
            :model-value="teslimDurumu"
            :options="teslimDurumSecenekleri"
            option-label="label"
            option-value="value"
            :allow-empty="false"
            class="w-full teslim-durum-secim"
            @update:model-value="$emit('update:teslimDurumu', $event)"
          />
        </div>
        <div
          v-if="seciliSofor"
          class="teslim-eden-alan"
        >
          <InputText
            :model-value="teslimNotu"
            :placeholder="t('hizliSatis.teslimNotuPlaceholder')"
            class="w-full"
            @update:model-value="$emit('update:teslimNotu', $event)"
          />
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'

defineProps({
  musteriAcik: { type: Boolean, default: true },
  musteriModu: { type: String, default: 'perakende' },
  musteriModlari: { type: Array, default: () => [] },
  musteriGiris: { type: String, default: '' },
  musteriOnerileri: { type: Array, default: () => [] },
  seciliMusteri: { type: Object, default: null },
  musteriBakiyeUyarisi: { type: Object, default: null },
  degisimIadeId: { type: [Number, String, null], default: null },
  teslimatAcik: { type: Boolean, default: false },
  seciliSofor: { type: Object, default: null },
  soforler: { type: Array, default: () => [] },
  soforlerYukleniyor: { type: Boolean, default: false },
  teslimatAdresi: { type: String, default: '' },
  teslimDurumu: { type: String, default: 'BEKLIYOR' },
  teslimDurumSecenekleri: { type: Array, default: () => [] },
  teslimNotu: { type: String, default: '' },
  /** Alan bazli dogrulama hatalari (orn. `teslimatAdresi: true`). */
  hatalar: { type: Object, default: null }
})

defineEmits([
  'update:musteriAcik', 'update:musteriModu', 'update:musteriGiris',
  'update:teslimatAcik', 'update:seciliSofor', 'update:teslimatAdresi',
  'update:teslimDurumu', 'update:teslimNotu',
  'musteri-ara', 'musteri-sec', 'musteri-temizle', 'degisim-kapat', 'yeni-musteri'
])

const { t } = useI18n()
const musteriAutoRef = ref(null)
defineExpose({ musteriAutoRef })
</script>
