<template>
  <Drawer
    :visible="visible"
    position="right"
    :header="t('kategoriler.kullanimBaslik')"
    :style="{ width: '480px', maxWidth: '92vw' }"
    @update:visible="$emit('update:visible', $event)"
  >
    <div
      v-if="yukleniyor"
      class="kullanim-durum"
      role="status"
      aria-live="polite"
    >
      <i class="pi pi-spin pi-spinner" /> {{ t('kategoriler.kullanimYukleniyor') }}
    </div>

    <div
      v-else-if="kullanim"
      class="kullanim"
    >
      <div class="kullanim-ust">
        <div class="kullanim-baslik">
          <span
            class="renk-nokta"
            :style="{ background: renk || 'var(--text-muted)' }"
            aria-hidden="true"
          />
          <i
            v-if="kullanim.ikon"
            :class="kullanim.ikon"
            :style="{ color: renk || 'var(--text-secondary)' }"
            aria-hidden="true"
          />
          <span>{{ kullanim.ad }}</span>
        </div>
        <span class="kullanim-toplam">
          <i
            class="pi pi-link"
            aria-hidden="true"
          />
          {{ t('kategoriler.kullanimToplam') }}:
          <strong>{{ kullanim.toplam || 0 }}</strong>
        </span>
      </div>

      <div
        v-if="detaylar.length"
        class="kullanim-bolum"
      >
        <h4 class="kullanim-alt-baslik">
          {{ t('kategoriler.kullanimDetay') }}
        </h4>
        <ul
          class="kullanim-liste"
          :aria-label="t('kategoriler.kullanimDetay')"
        >
          <li
            v-for="d in detaylar"
            :key="d.modul"
            class="kullanim-satir"
          >
            <i
              :class="modulIkon(d.modul)"
              aria-hidden="true"
            />
            <span class="kullanim-modul">{{ modulAd(d.modul) }}</span>
            <span class="kullanim-adet">{{ d.adet || 0 }}</span>
            <Button
              v-if="modulYol(d.modul)"
              :label="t('kategoriler.ilgiliKaydaGit')"
              icon="pi pi-arrow-right"
              class="p-button-text p-button-sm"
              :aria-label="t('kategoriler.ilgiliKaydaGitModul', { modul: modulAd(d.modul) })"
              @click="git(d.modul)"
            />
          </li>
        </ul>
      </div>

      <div
        v-else
        class="kullanim-bos"
      >
        <i
          class="pi pi-inbox"
          aria-hidden="true"
        />
        <p>{{ t('kategoriler.kullanimYok') }}</p>
      </div>
    </div>
  </Drawer>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'

const props = defineProps({
  visible: { type: Boolean, default: false },
  kullanim: { type: Object, default: null },
  yukleniyor: { type: Boolean, default: false }
})

const emit = defineEmits(['update:visible', 'git'])

const { t } = useI18n()
const router = useRouter()

const MODUL_YOL = {
  stok: '/stoklar',
  kasa: '/kasa',
  masraf: '/masraflar',
  butce: '/butceler',
  iskonto: '/iskonto-kurallari'
}

/** Backend modul anahtarini (STOK, KASA, MASRAF...) normalize eder. */
const anahtar = (modul) => String(modul || '').toLowerCase()

const detaylar = computed(() =>
  Array.isArray(props.kullanim?.detaylar) ? props.kullanim.detaylar : []
)

const renk = computed(() => props.kullanim?.renk || null)

const modulYol = (modul) => {
  const anahtarlar = Object.keys(MODUL_YOL)
  const bulunan = anahtarlar.find((a) => anahtar(modul).includes(a))
  return bulunan ? MODUL_YOL[bulunan] : ''
}

const modulAd = (modul) => {
  const a = anahtar(modul)
  if (a.includes('stok')) return t('kategoriler.modulStok')
  if (a.includes('kasa')) return t('kategoriler.modulKasa')
  if (a.includes('masraf')) return t('kategoriler.modulMasraf')
  if (a.includes('butce')) return t('kategoriler.modulButce')
  if (a.includes('iskonto')) return t('kategoriler.modulIskonto')
  return modul
}

const modulIkon = (modul) => {
  const a = anahtar(modul)
  if (a.includes('stok')) return 'pi pi-box'
  if (a.includes('kasa')) return 'pi pi-wallet'
  if (a.includes('masraf')) return 'pi pi-receipt'
  if (a.includes('butce')) return 'pi pi-chart-pie'
  if (a.includes('iskonto')) return 'pi pi-percentage'
  return 'pi pi-link'
}

const git = (modul) => {
  const yol = modulYol(modul)
  if (!yol) return
  emit('git', { modul, yol })
  router?.push?.(yol)
}

defineExpose({ modulAd, modulIkon, modulYol, git })
</script>

<style scoped>
.kullanim {
  display: flex;
  flex-direction: column;
  gap: 18px;
}
.kullanim-durum {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 28px 12px;
  color: var(--text-secondary);
  font-size: 13px;
}
.kullanim-ust {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border);
}
.kullanim-baslik {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 1.05rem;
  font-weight: 700;
  color: var(--text-primary);
  min-width: 0;
}
.kullanim-baslik > span:last-child {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.renk-nokta {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  flex-shrink: 0;
}
.kullanim-toplam {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12.5px;
  color: var(--text-secondary);
  background: var(--bg-muted);
  border: 1px solid var(--border);
  border-radius: 999px;
  padding: 4px 12px;
}
.kullanim-toplam strong {
  color: var(--text-primary);
  font-variant-numeric: tabular-nums;
}
.kullanim-bolum {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.kullanim-alt-baslik {
  margin: 0;
  font-size: 11.5px;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  color: var(--text-muted);
}
.kullanim-liste {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.kullanim-satir {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 12px;
  border: 1px solid var(--border);
  border-radius: var(--radius-md, 10px);
  background: var(--bg-muted);
  flex-wrap: wrap;
}
.kullanim-satir > i {
  color: var(--text-muted);
}
.kullanim-modul {
  flex: 1 1 auto;
  min-width: 0;
  font-size: 13px;
  color: var(--text-primary);
}
.kullanim-adet {
  font-size: 13px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  color: var(--text-primary);
}
.kullanim-bos {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 32px 12px;
  text-align: center;
  color: var(--text-secondary);
  font-size: 13px;
}
.kullanim-bos i {
  font-size: 2rem;
  color: var(--text-muted);
}
.kullanim-bos p {
  margin: 0;
}
@media (prefers-reduced-motion: reduce) {
  .kullanim-satir {
    transition: none;
  }
}
</style>
