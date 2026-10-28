<template>
  <Dialog
    :visible="visible"
    modal
    :draggable="false"
    :header="dialogBaslik"
    :style="{ width: '560px' }"
    :breakpoints="{ '575px': '92vw' }"
    class="kategori-form-dialog"
    @update:visible="$emit('update:visible', $event)"
    @show="odakla"
  >
    <form
      class="kategori-form"
      novalidate
      @submit.prevent="kaydet"
    >
      <!-- Canli onizleme: secilen renk/ikon/ad aninda gorunur. -->
      <div
        class="onizleme"
        :aria-label="t('kategoriler.onizleme')"
      >
        <span class="onizleme-etiket">{{ t('kategoriler.onizleme') }}</span>
        <span
          class="onizleme-rozet"
          :style="{ background: renkYumusak, color: form.renk || 'var(--text-primary)' }"
          aria-live="polite"
        >
          <i
            v-if="form.ikon"
            :class="form.ikon"
            aria-hidden="true"
          />
          <span class="onizleme-ad">{{ onizlemeAd }}</span>
        </span>
      </div>

      <div class="alan">
        <label for="kategori-ad">{{ t('kategoriler.kategoriAdiZorunlu') }}</label>
        <InputText
          id="kategori-ad"
          ref="adRef"
          v-model="form.ad"
          class="w-full"
          :class="{ 'p-invalid': hatalar.ad }"
          maxlength="100"
          required
          aria-required="true"
          :aria-invalid="!!hatalar.ad"
          :placeholder="t('kategoriler.kategoriAdiPlaceholder')"
          @input="hatalar.ad = ''"
        />
        <small
          v-if="hatalar.ad"
          class="p-error"
          role="alert"
        >{{ hatalar.ad }}</small>
        <small
          v-else-if="buyukHarfKapsami"
          class="ipucu"
        >{{ t('kategoriler.buyukHarfIpucu') }}</small>
      </div>

      <div class="alan">
        <label for="kategori-ust">{{ t('kategoriler.ustKategori') }}</label>
        <Dropdown
          id="kategori-ust"
          v-model="form.ustKategoriId"
          :options="ustSecenekleri"
          option-label="ad"
          option-value="id"
          show-clear
          class="w-full"
          :disabled="ustKilitli"
          :placeholder="t('kategoriler.altKategoriYok')"
        />
        <small
          v-if="ustKilitli"
          class="ipucu"
        >{{ t('kategoriler.ustKategoriKilitli') }}</small>
        <small
          v-else
          class="ipucu"
        >{{ t('kategoriler.ustKategoriIpucu') }}</small>
      </div>

      <div class="alan">
        <span
          id="kategori-renk-etiket"
          class="alan-etiket"
        >{{ t('kategoriler.renk') }}</span>
        <div
          class="renk-paleti"
          role="group"
          aria-labelledby="kategori-renk-etiket"
        >
          <button
            v-for="renk in RENK_PALETI"
            :key="renk"
            type="button"
            class="renk-nokta"
            :class="{ secili: form.renk === renk }"
            :style="{ background: renk }"
            :aria-pressed="form.renk === renk"
            :aria-label="t('kategoriler.renkSec', { renk })"
            :title="renk"
            @click="form.renk = renk"
          >
            <i
              v-if="form.renk === renk"
              class="pi pi-check"
              aria-hidden="true"
            />
          </button>
        </div>
      </div>

      <div class="alan">
        <span
          id="kategori-ikon-etiket"
          class="alan-etiket"
        >{{ t('kategoriler.ikon') }}</span>
        <div
          class="ikon-izgara"
          role="group"
          aria-labelledby="kategori-ikon-etiket"
        >
          <button
            v-for="ikon in IKONLAR"
            :key="ikon"
            type="button"
            class="ikon-secim"
            :class="{ secili: form.ikon === ikon }"
            :aria-pressed="form.ikon === ikon"
            :aria-label="t('kategoriler.ikonSec', { ikon: ikon.replace('pi ', '') })"
            :title="ikon.replace('pi ', '')"
            @click="form.ikon = ikon"
          >
            <i
              :class="ikon"
              aria-hidden="true"
            />
          </button>
        </div>
      </div>

      <div class="alan-satiri">
        <div class="alan">
          <label for="kategori-sira">{{ t('kategoriler.sira') }}</label>
          <InputNumber
            id="kategori-sira"
            v-model="form.sira"
            :min="0"
            :use-grouping="false"
            show-buttons
            class="w-full"
          />
        </div>
        <div class="alan">
          <label for="kategori-aktif">{{ t('kategoriler.aktif') }}</label>
          <InputSwitch
            id="kategori-aktif"
            v-model="form.aktif"
            :aria-label="t('kategoriler.aktif')"
          />
        </div>
      </div>

      <div class="alan">
        <label for="kategori-aciklama">{{ t('common.description') }}</label>
        <Textarea
          id="kategori-aciklama"
          v-model="form.aciklama"
          rows="2"
          maxlength="255"
          class="w-full"
        />
      </div>
    </form>

    <template #footer>
      <Button
        :label="t('common.cancel')"
        icon="pi pi-times"
        class="p-button-text"
        @click="$emit('update:visible', false)"
      />
      <Button
        :label="t('common.save')"
        icon="pi pi-check"
        :loading="kaydediliyor"
        :disabled="kaydediliyor"
        @click="kaydet"
      />
    </template>
  </Dialog>
</template>

<script setup>
import { computed, nextTick, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'

/** Sabit renk paleti (backend ile ayni 12 renk siniri). */
const RENK_PALETI = [
  '#ef4444', '#f97316', '#f59e0b', '#eab308',
  '#84cc16', '#10b981', '#14b8a6', '#06b6d4',
  '#3b82f6', '#6366f1', '#a855f7', '#ec4899'
]

/** PrimeIcons izgarasi: kategori icin yaygin kullanilan ikonlar. */
const IKONLAR = [
  'pi pi-tag', 'pi pi-tags', 'pi pi-box', 'pi pi-shopping-cart',
  'pi pi-shopping-bag', 'pi pi-wallet', 'pi pi-money-bill', 'pi pi-credit-card',
  'pi pi-chart-line', 'pi pi-chart-pie', 'pi pi-home', 'pi pi-building',
  'pi pi-truck', 'pi pi-car', 'pi pi-wrench', 'pi pi-cog',
  'pi pi-users', 'pi pi-user', 'pi pi-briefcase', 'pi pi-file',
  'pi pi-receipt', 'pi pi-percentage', 'pi pi-gift', 'pi pi-star'
]

const props = defineProps({
  visible: { type: Boolean, default: false },
  kapsam: { type: String, default: 'KASA_GELIR' },
  kategori: { type: Object, default: null },
  /** Yalniz ayni kapsamin UST kategorileri beklenir. */
  ustKategoriler: { type: Array, default: () => [] },
  varsayilanUstKategoriId: { type: [Number, String], default: null },
  kaydediliyor: { type: Boolean, default: false }
})

const emit = defineEmits(['update:visible', 'kaydet'])

const { t } = useI18n()
const adRef = ref(null)
const hatalar = ref({ ad: '' })

const form = ref(bosForm())

function bosForm() {
  return {
    ad: '',
    ustKategoriId: props.varsayilanUstKategoriId ?? null,
    renk: RENK_PALETI[0],
    ikon: 'pi pi-tag',
    sira: 0,
    aciklama: '',
    aktif: true
  }
}

const duzenleme = computed(() => !!props.kategori?.id)
const dialogBaslik = computed(() => {
  if (duzenleme.value) return t('kategoriler.kategoriDuzenle')
  if (props.varsayilanUstKategoriId) return t('kategoriler.altKategoriEkle')
  return t('kategoriler.yeniKategori')
})

const buyukHarfKapsami = computed(() => props.kapsam === 'MASRAF' || props.kapsam === 'BUTCE')

const ustSecenekleri = computed(() =>
  (props.ustKategoriler || []).filter((k) => {
    if (!k || k.aktif === false) return false
    if (props.kategori?.id && k.id === props.kategori.id) return false
    // Yalniz 2 seviye: yalniz kok kategoriler ust kategori olabilir.
    return !k.ustKategoriId
  })
)

// Alt kategorisi olan bir kayit ust kategoriye tasinamaz (dongu/3. seviye).
const ustKilitli = computed(
  () => duzenleme.value && Number(props.kategori?.altKategoriSayisi || 0) > 0
)

const onizlemeAd = computed(() => {
  const ad = (form.value.ad || '').trim()
  if (!ad) return t('kategoriler.onizlemeAd')
  return buyukHarfKapsami.value ? ad.toLocaleUpperCase('tr-TR') : ad
})

const renkYumusak = computed(() => (form.value.renk ? `${form.value.renk}22` : 'var(--bg-muted)'))

watch(
  () => [props.visible, props.kategori, props.varsayilanUstKategoriId, props.kapsam],
  () => {
    if (!props.visible) return
    const k = props.kategori
    form.value = {
      ad: k?.ad || '',
      ustKategoriId: k?.ustKategoriId ?? props.varsayilanUstKategoriId ?? null,
      renk: k?.renk || RENK_PALETI[0],
      ikon: k?.ikon || 'pi pi-tag',
      sira: k?.sira ?? 0,
      aciklama: k?.aciklama || '',
      aktif: k?.aktif !== false
    }
    hatalar.value = { ad: '' }
  },
  { immediate: true }
)

const odakla = () => {
  nextTick(() => {
    const el = adRef.value?.$el || adRef.value
    el?.focus?.()
  })
}

const kaydet = () => {
  if (props.kaydediliyor) return
  const ad = (form.value.ad || '').trim()
  if (!ad) {
    hatalar.value = { ad: t('kategoriler.adZorunlu') }
    const el = adRef.value?.$el || adRef.value
    el?.focus?.()
    return
  }
  hatalar.value = { ad: '' }
  emit('kaydet', {
    ad: buyukHarfKapsami.value ? ad.toLocaleUpperCase('tr-TR') : ad,
    kapsam: props.kapsam,
    ustKategoriId: form.value.ustKategoriId ?? null,
    renk: form.value.renk || null,
    ikon: form.value.ikon || null,
    sira: Number(form.value.sira) || 0,
    aciklama: (form.value.aciklama || '').trim() || null,
    aktif: form.value.aktif !== false
  })
}

defineExpose({ RENK_PALETI, IKONLAR, form, hatalar, kaydet })
</script>

<style scoped>
.kategori-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.onizleme {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border: 1px dashed var(--border);
  border-radius: var(--radius-md, 10px);
  background: var(--bg-muted);
}
.onizleme-etiket {
  font-size: 11.5px;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  color: var(--text-muted);
  font-weight: 700;
}
.onizleme-rozet {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 5px 12px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 700;
  max-width: 100%;
}
.onizleme-ad {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.alan {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}
.alan-satiri {
  display: flex;
  gap: 16px;
  align-items: flex-end;
}
.alan-satiri > .alan:first-child {
  flex: 1 1 auto;
}
.alan label,
.alan-etiket {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
}
.ipucu {
  font-size: 11.5px;
  color: var(--text-muted);
}
.renk-paleti {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.renk-nokta {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: 2px solid transparent;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 11px;
  box-shadow: 0 0 0 1px rgba(148, 163, 184, 0.3);
  transition: transform var(--dur-fast, 0.15s) var(--ease-standard, ease);
}
.renk-nokta:hover {
  transform: scale(1.1);
}
.renk-nokta.secili {
  border-color: var(--text-primary);
  box-shadow: 0 0 0 2px var(--bg-card), 0 0 0 4px var(--accent);
}
.ikon-izgara {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(38px, 1fr));
  gap: 6px;
  max-height: 148px;
  overflow-y: auto;
  padding: 4px;
  border: 1px solid var(--border);
  border-radius: var(--radius-md, 10px);
}
.ikon-secim {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 34px;
  border-radius: 8px;
  border: 1px solid transparent;
  background: var(--bg-muted);
  color: var(--text-secondary);
  cursor: pointer;
  font-size: 14px;
  transition: all var(--dur-fast, 0.15s) var(--ease-standard, ease);
}
.ikon-secim:hover {
  color: var(--accent);
  border-color: var(--accent-border);
}
.ikon-secim.secili {
  background: var(--accent-soft);
  border-color: var(--accent-border);
  color: var(--accent);
}
.ikon-secim:focus-visible,
.renk-nokta:focus-visible {
  outline: none;
  box-shadow: 0 0 0 3px var(--accent-soft-strong);
}
.w-full {
  width: 100%;
}
@media (prefers-reduced-motion: reduce) {
  .renk-nokta,
  .ikon-secim {
    transition: none;
  }
  .renk-nokta:hover {
    transform: none;
  }
}
</style>
