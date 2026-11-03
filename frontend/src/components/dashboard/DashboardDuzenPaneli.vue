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

    <!-- Sabit bantlar: yalnizca gizlenebilir, tasinamaz. -->
    <ul
      v-if="sabitler.length"
      class="duzen-liste duzen-liste--sabit"
      data-bolge="SABIT"
    >
      <li
        v-for="w in sabitler"
        :key="w.key"
        class="duzen-satir duzen-satir--sabit"
        :class="{ 'duzen-satir--gizli': !w.gorunur }"
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
          class="duzen-sabit"
          :title="t('dashboard.widgetSabit')"
        >{{ t('dashboard.sabit') }}</span>
      </li>
    </ul>

    <!-- Iki bolge: grafik duvari (SOL) ve aksiyon paneli (SAG). -->
    <section
      v-for="grup in gruplar"
      :key="grup.key"
      class="duzen-grup"
      :data-bolge="grup.key"
    >
      <h3 class="duzen-grup-baslik">
        {{ grup.baslik }}
      </h3>
      <ul class="duzen-liste">
        <li
          v-for="(w, index) in grup.liste"
          :key="w.key"
          class="duzen-satir"
          :class="{ 'duzen-satir--gizli': !w.gorunur }"
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
          <div class="duzen-eylemler">
            <button
              type="button"
              class="duzen-buton"
              :disabled="index === 0"
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
              :disabled="index === grup.liste.length - 1"
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
    </section>

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
import { BOLGE_SAG, BOLGE_SOL, widgetBolgesi } from '../../utils/dashboardDuzen.js'

const props = defineProps({
  /** [{ key, etiket, gorunur, sabit, bolge? }] */
  widgetlar: { type: Array, default: () => [] },
  kaydediliyor: { type: Boolean, default: false }
})

const emit = defineEmits(['degistir', 'tasi', 'sifirla', 'kaydet', 'iptal'])

const { t } = useI18n()

const degistir = (key, alan, deger) => {
  emit('degistir', { key, alan, deger })
}

const sabitler = computed(() => props.widgetlar.filter((w) => w.sabit))

const bolgeAl = (w) => w.bolge || widgetBolgesi(w.key)

const gruplar = computed(() =>
  [
    {
      key: BOLGE_SOL,
      baslik: t('dashboard.bolgeGrafikler'),
      liste: props.widgetlar.filter((w) => !w.sabit && bolgeAl(w) === BOLGE_SOL)
    },
    {
      key: BOLGE_SAG,
      baslik: t('dashboard.bolgeAksiyon'),
      liste: props.widgetlar.filter((w) => !w.sabit && bolgeAl(w) === BOLGE_SAG)
    }
  ].filter((g) => g.liste.length)
)
</script>

<style scoped>
/*
 * Duzen paneli: sabit bantlar + iki bolge grubu. Her grupta gorunurluk ve
 * kendi icinde sira oklari vardir; kart boyutu kontrolu yoktur.
 */
.duzen-paneli {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 12px 14px;
  margin: 8px 0 12px;
  border: 1px solid var(--border);
  border-radius: 13px;
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
  font-size: 14px;
  font-weight: 800;
  color: var(--text-primary);
}
.duzen-baslik i {
  color: var(--accent);
}
.duzen-alt {
  margin: 2px 0 0;
  font-size: 11.5px;
  color: var(--text-secondary);
}
.duzen-grup {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}
.duzen-grup-baslik {
  margin: 0;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.4px;
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
.duzen-liste--sabit {
  border-bottom: 1px solid var(--border);
  padding-bottom: 10px;
}
.duzen-satir {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 6px 10px;
  border-radius: 9px;
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
  min-height: 36px;
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
    padding: 12px;
  }
}
</style>
