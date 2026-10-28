<template>
  <section
    class="dw"
    :class="{ 'dw--duzen': duzenModu, 'dw--surukleniyor': surukleniyor }"
    :draggable="duzenModu"
    :data-widget="anahtar"
    @dragstart="suruklemeyeBasla"
    @dragend="suruklemeBitti"
    @dragover.prevent="suruklemeUzerinde"
    @drop.prevent="birakildi"
  >
    <div
      v-if="duzenModu"
      class="dw-arac"
      role="toolbar"
      :aria-label="etiket"
    >
      <span
        class="dw-tutamac"
        :title="t('dashboard.widgetSurukle')"
        aria-hidden="true"
      >
        <i class="pi pi-bars" />
      </span>
      <span class="dw-etiket">{{ etiket }}</span>
      <span class="dw-butonlar">
        <button
          type="button"
          class="dw-buton"
          :disabled="sira <= 0"
          :aria-label="`${etiket} — ${t('dashboard.widgetYukari')}`"
          :title="t('dashboard.widgetYukari')"
          @click="emit('yukari')"
        >
          <i
            class="pi pi-arrow-up"
            aria-hidden="true"
          />
        </button>
        <button
          type="button"
          class="dw-buton"
          :disabled="sira >= uzunluk - 1"
          :aria-label="`${etiket} — ${t('dashboard.widgetAsagi')}`"
          :title="t('dashboard.widgetAsagi')"
          @click="emit('asagi')"
        >
          <i
            class="pi pi-arrow-down"
            aria-hidden="true"
          />
        </button>
        <button
          type="button"
          class="dw-buton dw-buton--gizle"
          :aria-label="`${etiket} — ${t('dashboard.widgetGizle')}`"
          :title="t('dashboard.widgetGizle')"
          @click="emit('gizle')"
        >
          <i
            class="pi pi-eye-slash"
            aria-hidden="true"
          />
        </button>
      </span>
    </div>
    <div class="dw-icerik">
      <slot />
    </div>
  </section>
</template>

<script setup>
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'

const props = defineProps({
  anahtar: { type: String, required: true },
  etiket: { type: String, default: '' },
  duzenModu: { type: Boolean, default: false },
  sira: { type: Number, default: 0 },
  uzunluk: { type: Number, default: 1 }
})

const emit = defineEmits(['yukari', 'asagi', 'gizle', 'surukle-baslat', 'birak'])

const { t } = useI18n()

const surukleniyor = ref(false)

const suruklemeyeBasla = (event) => {
  surukleniyor.value = true
  if (event?.dataTransfer) {
    event.dataTransfer.effectAllowed = 'move'
    // Firefox suruklemeyi baslatmak icin veri istiyor.
    try {
      event.dataTransfer.setData('text/plain', props.anahtar)
    } catch {
      /* bazi tarayicilar izin vermez */
    }
  }
  emit('surukle-baslat', props.anahtar)
}

const suruklemeBitti = () => {
  surukleniyor.value = false
}

const suruklemeUzerinde = (event) => {
  if (!props.duzenModu) return
  if (event?.dataTransfer) event.dataTransfer.dropEffect = 'move'
}

const birakildi = () => {
  surukleniyor.value = false
  emit('birak', props.anahtar)
}
</script>

<style scoped>
/*
 * Normal modda widget bir cerceve CIZMEZ: bolum, panonun icinde duz bir
 * bolumdur (cerceve/arka plan/golge/yaricap yok). Yalnizca duzen modunda
 * kesikli cerceve + arac cubugu gorunur.
 */
.dw {
  min-width: 0;
}
.dw--duzen {
  outline: 1px dashed var(--border);
  outline-offset: -1px;
}
.dw--duzen:hover,
.dw--duzen:focus-within {
  outline-color: var(--accent-border);
  background: color-mix(in srgb, var(--accent-soft) 40%, transparent);
}
.dw--surukleniyor {
  opacity: 0.55;
}
.dw-arac {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 12px 24px 0;
  padding: 4px 8px;
  border-radius: 10px;
  background: var(--bg-muted);
  border: 1px solid var(--border);
  flex-wrap: wrap;
}
.dw-tutamac {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border-radius: 7px;
  color: var(--text-muted);
  cursor: grab;
  flex-shrink: 0;
}
.dw-tutamac:active {
  cursor: grabbing;
}
.dw-etiket {
  font-size: 12px;
  font-weight: 700;
  color: var(--text-secondary);
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.dw-butonlar {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}
.dw-buton {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 30px;
  height: 30px;
  padding: 0 8px;
  border-radius: 8px;
  border: 1px solid var(--border);
  background: var(--bg-card);
  color: var(--text-secondary);
  font-size: 11px;
  font-weight: 700;
  cursor: pointer;
}
.dw-buton:hover:not(:disabled) {
  color: var(--accent);
  border-color: var(--accent-border);
}
.dw-buton:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}
.dw-buton:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
.dw-buton--gizle:hover {
  color: var(--danger);
  border-color: var(--danger-border);
}
@media (max-width: 600px) {
  .dw-arac {
    margin: 12px 16px 0;
  }
}
</style>
