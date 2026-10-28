<template>
  <section
    class="pano-bolum"
    data-test="pano-bolum"
  >
    <header class="pano-bolum-ust">
      <h2 class="pano-bolum-baslik">
        <span class="pano-bolum-ikon">
          <i
            :class="ikon"
            aria-hidden="true"
          />
        </span>
        <span class="pano-bolum-baslik-metin">{{ baslik }}</span>
      </h2>
      <router-link
        v-if="linkTo"
        :to="linkTo"
        class="pano-bolum-link"
        :aria-label="`${baslik} — ${t('dashboard.detayaGit')}`"
      >
        {{ t('dashboard.detayaGit') }}
        <i
          class="pi pi-arrow-right"
          aria-hidden="true"
        />
      </router-link>
    </header>

    <div class="pano-bolum-govde">
      <slot />
    </div>
  </section>
</template>

<script setup>
import { useI18n } from 'vue-i18n'

defineProps({
  baslik: { type: String, required: true },
  ikon: { type: String, default: 'pi pi-chart-bar' },
  linkTo: { type: String, default: null }
})

const { t } = useI18n()
</script>
