<template>
  <section
    class="duzen-paneli"
    :aria-label="t('dashboard.duzenBaslik')"
    data-test="duzen-paneli"
  >
    <header class="duzen-ust">
      <div>
        <h2 class="duzen-baslik">
          <i
            class="pi pi-sliders-h"
            aria-hidden="true"
          />
          {{ t('dashboard.duzenBaslik') }}
        </h2>
        <p class="duzen-alt">
          {{ t('dashboard.duzenAlt') }}
        </p>
      </div>
    </header>

    <div
      v-for="grup in gruplar"
      :key="grup.bolge"
      class="duzen-grup"
      :data-bolge="grup.bolge"
    >
      <h3 class="duzen-grup-baslik">
        {{ grup.etiket }}
      </h3>
      <ul class="duzen-liste">
        <li
          v-for="(w, index) in grup.widgetlar"
          :key="w.key"
          class="duzen-satir"
          :class="{ 'duzen-satir--gizli': !w.gorunur, 'duzen-satir--sabit': w.sabit }"
          :data-widget="w.key"
        >
          <label class="duzen-gorunur">
            <input
              type="checkbox"
              :checked="w.gorunur"
              :aria-label="`${w.etiket} — ${t('dashboard.duzenGorunur')}`"
              @change="degistir(w.key, 'gorunur', $event.target.checked)"
            >
            <span class="duzen-etiket">{{ w.etiket }}</span>
          </label>
          <span
            v-if="w.sabit"
            class="duzen-sabit"
            :title="t('dashboard.widgetSabit')"
          >{{ t('dashboard.sabit') }}</span>
          <div
            v-else
            class="duzen-eylemler"
          >
            <button
              type="button"
              class="duzen-buton"
              :disabled="!tasinabilir(grup.widgetlar, index, -1)"
              :aria-label="`${w.etiket} — ${t('dashboard.widgetYukari')}`"
              :title="t('dashboard.widgetYukari')"
              @click="emit('tasi', { key: w.key, yon: -1 })"
            >
              <i
                class="pi pi-arrow-up"
                aria-hidden="true"
              />
            </button>
            <button
              type="button"
              class="duzen-buton"
              :disabled="!tasinabilir(grup.widgetlar, index, 1)"
              :aria-label="`${w.etiket} — ${t('dashboard.widgetAsagi')}`"
              :title="t('dashboard.widgetAsagi')"
              @click="emit('tasi', { key: w.key, yon: 1 })"
            >
              <i
                class="pi pi-arrow-down"
                aria-hidden="true"
              />
            </button>
          </div>
        </li>
      </ul>
    </div>

    <footer class="duzen-alt-bolum">
      <button
        type="button"
        class="duzen-eylem duzen-eylem--sifirla"
        @click="emit('sifirla')"
      >
        <i
          class="pi pi-replay"
          aria-hidden="true"
        />
        {{ t('dashboard.duzenSifirla') }}
      </button>
      <span class="duzen-bosluk" />
      <button
        type="button"
        class="duzen-eylem duzen-eylem--iptal"
        @click="emit('iptal')"
      >
        {{ t('dashboard.duzenIptal') }}
      </button>
      <button
        type="button"
        class="duzen-eylem duzen-eylem--kaydet"
        :disabled="kaydediliyor"
        @click="emit('kaydet')"
      >
        <i
          class="pi"
          :class="kaydediliyor ? 'pi-spin pi-spinner' : 'pi-check'"
          aria-hidden="true"
        />
        {{ t('dashboard.duzenKaydet') }}
      </button>
    </footer>
  </section>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

const props = defineProps({
  /** [{ key, etiket, gorunur, bolge: 'ana'|'ray', sabit }] */
  widgetlar: { type: Array, default: () => [] },
  kaydediliyor: { type: Boolean, default: false }
})

const emit = defineEmits(['degistir', 'tasi', 'sifirla', 'kaydet', 'iptal'])

const { t } = useI18n()

const gruplar = computed(() =>
  [
    { bolge: 'ana', etiket: t('dashboard.anaIcerik') },
    { bolge: 'ray', etiket: t('dashboard.sagRay') }
  ]
    .map((grup) => ({
      ...grup,
      widgetlar: props.widgetlar.filter((w) => (w.bolge === 'ray' ? 'ray' : 'ana') === grup.bolge)
    }))
    .filter((grup) => grup.widgetlar.length > 0)
)

const degistir = (key, alan, deger) => {
  emit('degistir', { key, alan, deger })
}

/** Ayni grup icinde, sabit satirlari atlayarak tasinabilecek komsu var mi? */
const tasinabilir = (liste, index, yon) => {
  const adim = yon < 0 ? -1 : 1
  let i = index + adim
  while (i >= 0 && i < liste.length) {
    if (!liste[i].sabit) return true
    i += adim
  }
  return false
}
</script>

<style scoped>
/*
 * Duzen paneli panonun icinde bir banttir (kendi golgesi/yaricapi yok);
 * iki grup halinde ana icerik ve sag ray bolumlerini listeler.
 */
.duzen-paneli {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 16px 24px;
  border-bottom: 1px solid var(--border);
  background: var(--bg-muted);
}
.duzen-ust {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
}
.duzen-baslik {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  font-size: 15px;
  font-weight: 800;
  color: var(--text-primary);
}
.duzen-baslik i {
  color: var(--accent);
}
.duzen-alt {
  margin: 2px 0 0;
  font-size: 12px;
  color: var(--text-secondary);
}
.duzen-grup {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.duzen-grup-baslik {
  margin: 0;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.5px;
  text-transform: uppercase;
  color: var(--text-muted);
}
.duzen-liste {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}
.duzen-satir {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 6px 10px;
  border-radius: 10px;
  border: 1px solid var(--border);
  background: var(--bg-card);
  min-width: 0;
}
.duzen-satir--gizli {
  opacity: 0.6;
}
.duzen-satir--sabit {
  background: transparent;
  border-style: dashed;
}
.duzen-gorunur {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  cursor: pointer;
  font-size: 12.5px;
  color: var(--text-primary);
}
.duzen-gorunur input {
  width: 16px;
  height: 16px;
  accent-color: var(--accent);
  cursor: pointer;
}
.duzen-etiket {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.duzen-sabit {
  flex-shrink: 0;
  padding: 2px 8px;
  border-radius: 999px;
  background: var(--bg-muted);
  color: var(--text-muted);
  font-size: 10.5px;
  font-weight: 700;
  letter-spacing: 0.3px;
  text-transform: uppercase;
}
.duzen-eylemler {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}
.duzen-buton {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 30px;
  height: 30px;
  padding: 0 6px;
  border-radius: 7px;
  border: 1px solid var(--border);
  background: var(--bg-muted);
  color: var(--text-secondary);
  font-size: 10.5px;
  font-weight: 700;
  cursor: pointer;
}
.duzen-buton:hover:not(:disabled) {
  color: var(--accent);
  border-color: var(--accent-border);
}
.duzen-buton:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}
.duzen-buton:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
.duzen-alt-bolum {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.duzen-bosluk {
  flex: 1;
}
.duzen-eylem {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 38px;
  padding: 0 14px;
  border-radius: 9px;
  border: 1px solid var(--border);
  background: var(--bg-card);
  color: var(--text-secondary);
  font-size: 12.5px;
  font-weight: 700;
  cursor: pointer;
}
.duzen-eylem:hover:not(:disabled) {
  color: var(--text-primary);
  border-color: var(--accent-border);
}
.duzen-eylem:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}
.duzen-eylem--kaydet {
  background: var(--accent);
  border-color: var(--accent);
  color: var(--accent-contrast);
}
.duzen-eylem--kaydet:hover:not(:disabled) {
  background: var(--accent-hover);
  border-color: var(--accent-hover);
  color: var(--accent-contrast);
}
.duzen-eylem:disabled {
  opacity: 0.6;
  cursor: progress;
}
.duzen-eylem--sifirla i {
  font-size: 11px;
}
@media (max-width: 760px) {
  .duzen-liste {
    grid-template-columns: 1fr;
  }
  .duzen-bosluk {
    display: none;
  }
  .duzen-eylem {
    flex: 1;
    justify-content: center;
  }
}
@media (max-width: 600px) {
  .duzen-paneli {
    padding: 14px 16px;
  }
}
</style>
