<template>
  <div
    class="adet-grup"
    :class="{ 'adet-uyari': uyariGoster }"
  >
    <button
      type="button"
      class="adet-btn"
      :disabled="azaltilamaz"
      :aria-label="t('hizliSatis.miktarAzalt')"
      :title="t('hizliSatis.miktarAzalt')"
      @click="$emit('azalt')"
    >
      −
    </button>
    <input
      ref="girdiRef"
      v-model="yerelDeger"
      class="adet-girdi"
      type="number"
      inputmode="decimal"
      :min="min"
      :max="enFazla"
      :step="adim"
      :aria-label="t('hizliSatis.adet')"
      :aria-invalid="uyariGoster"
      :title="baslik"
      @focus="$event.target.select()"
      @change="onCommit"
      @blur="onCommit"
      @keydown.enter.stop.prevent="onCommit"
      @keydown.esc.stop.prevent="onVazgec"
      @keydown.up.stop.prevent="artir"
      @keydown.down.stop.prevent="azalt"
    >
    <button
      type="button"
      class="adet-btn"
      :disabled="artirilamaz"
      :aria-label="t('hizliSatis.miktarArtir')"
      :title="t('hizliSatis.miktarArtir')"
      @click="$emit('artir')"
    >
      +
    </button>
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'
import { adimBirimIcin, minMiktarBirimIcin } from '../utils/posAdet.js'

/**
 * Sepet satiri adet (miktar) kontrolu.
 *
 * SORUN: Once `−` butonu, `<input type="number" min="1">` ve `+` butonu ayri
 * ayri duruyordu ve DORT farkli davranislari vardi:
 *   - `+` butonu geri-al kaydi yazmiyor, satiri vurgulamiyor, stok kontrolu
 *     yapmiyordu;
 *   - `−` butonu miktari 1'de dusunce satiri KAYBEDIYORdu;
 *   - girdi dogrudan state'e yaziyordu (negatif adet mumkun, Enter ise yok);
 *   - her buton adimi sabit 1 idi, kg/m2 urunlerde 0.5 satilamiyordu.
 *
 * BURADA: uc kontrol tek yerde toplandi ve HER YOL AYNI OLAYI YAYAR:
 *   `azalt` / `artir` / `commit(yeniDeger)`
 * Dogrulama ve stok tavani ust tarafta (`miktarDogrula`) yapilir; bu yuzden
 * bu bilesen bilerek KENDI ADET DEGERINI DEGISTIRMEZ — gecerli degeri
 * `miktar` prop'undan okur. Uygulanamayan bir degisiklik (orn. stok asimi)
 * ust taraf reddederse girdi kendiliginden eski degerine doner; boylece
 * "yazdigim deger kaydedilmedi" durumu GORUNUR olur.
 *
 * KLAVYE: girdi odaktayken ↑/↓ adim, Enter commit, Escape vazgeç.
 */
const props = defineProps({
  miktar: { type: [Number, String], default: 1 },
  /** Depodaki mevcut miktar. null/undefined = bilinmiyor (tavan uygulanmaz). */
  stokMiktari: { type: [Number, String], default: null },
  birim: { type: String, default: 'adet' },
  disabled: { type: Boolean, default: false }
})

const emit = defineEmits(['azalt', 'artir', 'commit'])

const { t } = useI18n()

const adim = computed(() => adimBirimIcin(props.birim))
const min = computed(() => minMiktarBirimIcin(props.birim))
const enFazla = computed(() => {
  const s = Number(props.stokMiktari)
  return props.stokMiktari === null || props.stokMiktari === undefined || !Number.isFinite(s)
    ? undefined
    : s
})

const mevcut = computed(() => Number(props.miktar) || 0)

/** Tavana vardiginda `+` devre disi kalir (tasinmayi engelle). */
const artirilamaz = computed(
  () => props.disabled || (enFazla.value !== undefined && mevcut.value >= Number(enFazla.value))
)
/** En kucuk adetteyken `−` devre disi kalir (silme bu butonun isi degil). */
const azaltilamaz = computed(() => props.disabled || mevcut.value <= min.value)

/** Kullanici yazarken gosterilen gecici deger. Prop değişince senkronlanır.
 *
 *  SAYI TUTARLIGI: deger daima SAYIDIR. `input[type=number]` uzerinde Vue
 *  v-model degerini otomatik sayiya cevirir; ilk deger de String degil
 *  Number olarak kurulur, aksi halde "degistirmeden Enter" ile commit edilen
 *  deger `"2"` (string), yazilip Enter ile commit edilen deger `2` (number)
 *  olurdu. Ust tarafin `miktarDogrula` string'i de kabul etmesi bizi bu
 *  tutarsizliktan kurtarmaz — cikan sonuc ayni ama sozlesme belirsiz olur.
 */
const yerelDeger = ref(Number(props.miktar) || 0)
const uyariGoster = ref(false)

watch(
  () => props.miktar,
  (y) => {
    yerelDeger.value = Number(y) || 0
    uyariGoster.value = false
  }
)

/** Stok tavanına dayanırsa girişi kırmızıya çevirip kısaca uyar. */
const baslik = computed(() => {
  if (enFazla.value === undefined) return t('hizliSatis.adet')
  return t('hizliSatis.adetEnFazla', { n: enFazla.value })
})

const girdiRef = ref(null)

/** Girdiyi seçili hale getirir (klavyeden Enter sonrası hızlı yazma için). */
const sec = () => girdiRef.value?.select?.()

function onCommit() {
  if (props.disabled) return
  emit('commit', yerelDeger.value)
  // Ust taraf reddedebilir (stok asimı). Deger değişmezse işaret kaldırılır.
  nextTick(() => {
    if (mevcut.value !== Number(yerelDeger.value)) {
      uyariGoster.value = true
      yerelDeger.value = mevcut.value
    } else {
      uyariGoster.value = false
    }
  })
}

function onVazgec() {
  yerelDeger.value = mevcut.value
  uyariGoster.value = false
}

const artir = () => emit('artir')
const azalt = () => emit('azalt')

defineExpose({ sec })
</script>

<style scoped>
.adet-grup {
  display: inline-flex;
  align-items: center;
  gap: 2px;
}
.adet-btn {
  width: 28px;
  height: 28px;
  border: 1px solid var(--border);
  border-radius: 6px;
  background: var(--bg-muted);
  color: var(--text-primary);
  font-size: 16px;
  line-height: 1;
  cursor: pointer;
}
.adet-btn:hover:not(:disabled) {
  background: var(--accent);
  color: #fff;
  border-color: var(--accent);
}
.adet-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
.adet-girdi {
  width: 56px;
  height: 28px;
  padding: 0 4px;
  border: 1px solid var(--border);
  border-radius: 6px;
  background: var(--bg-card, #fff);
  color: var(--text-primary);
  font-size: 13px;
  text-align: center;
  font-variant-numeric: tabular-nums;
}
/* Depodaki miktardan fazla girildi: sessizce kabul edilmez. */
.adet-uyari .adet-girdi {
  border-color: var(--danger, #dc2626);
  background: color-mix(in srgb, var(--danger, #dc2626) 8%, transparent);
}
</style>