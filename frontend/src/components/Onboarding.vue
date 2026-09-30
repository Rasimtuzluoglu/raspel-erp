<template>
  <div class="onboarding">
    <div class="onboard-kart">
      <div class="onboard-ust">
        <i class="pi pi-bolt onboard-ikon" />
        <h2>{{ $t('onboarding.hosGeldiniz') }}</h2>
        <p>{{ $t('onboarding.bosSistem') }}</p>
      </div>

      <div class="onboard-adimlar">
        <div
          v-for="(adim, i) in adimlar"
          :key="i"
          class="adim"
          :class="{ tamamlandi: adim.tamam }"
        >
          <span
            class="adim-no"
            :class="{ 'adim-yesil': adim.tamam }"
          >
            <i
              v-if="adim.tamam"
              class="pi pi-check"
            />
            <template v-else>{{ i + 1 }}</template>
          </span>
          <div class="adim-icerik">
            <strong>{{ adim.baslik }}</strong>
            <p>{{ adim.aciklama }}</p>
            <Button
              :label="adim.buton"
              :icon="adim.ikon"
              class="p-button-sm p-button-outlined"
              @click="adimTikla(adim)"
            />
          </div>
        </div>
      </div>

      <div class="onboard-alt">
        <a
          class="onboard-atla"
          @click="atla"
        >{{ $t('onboarding.atla') }}</a>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'

const router = useRouter()
const { t } = useI18n()
const emit = defineEmits(['atla'])

const adimlar = reactive([
  {
    baslik: t('onboarding.adim1Baslik'),
    aciklama: t('onboarding.adim1Aciklama'),
    buton: t('onboarding.adim1Buton'),
    ikon: 'pi pi-building',
    tamam: false,
    path: '/sirketler'
  },
  {
    baslik: t('onboarding.adim2Baslik'),
    aciklama: t('onboarding.adim2Aciklama'),
    buton: t('onboarding.adim2Buton'),
    ikon: 'pi pi-users',
    tamam: false,
    path: '/cari-hesaplar'
  },
  {
    baslik: t('onboarding.adim3Baslik'),
    aciklama: t('onboarding.adim3Aciklama'),
    buton: t('onboarding.adim3Buton'),
    ikon: 'pi pi-box',
    tamam: false,
    path: '/stoklar'
  },
  {
    baslik: t('onboarding.adim4Baslik'),
    aciklama: t('onboarding.adim4Aciklama'),
    buton: t('onboarding.adim4Buton'),
    ikon: 'pi pi-file',
    tamam: false,
    path: '/faturalar'
  }
])

const adimTikla = (adim) => {
  router.push(adim.path)
}

const atla = () => {
  emit('atla')
}
</script>

<style scoped>
.onboarding {
  display: flex;
  justify-content: center;
  padding: 2rem 0;
}
.onboard-kart {
  max-width: 560px;
  width: 100%;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 16px;
  padding: 2.5rem;
  box-shadow: var(--shadow);
}
.onboard-ust {
  text-align: center;
  margin-bottom: 2rem;
}
.onboard-ikon {
  font-size: 3rem;
  color: var(--accent);
  margin-bottom: 0.5rem;
}
.onboard-ust h2 {
  margin: 0 0 0.5rem;
  font-size: 1.4rem;
}
.onboard-ust p {
  color: var(--text-secondary);
  font-size: 0.9rem;
  margin: 0;
}
.onboard-adimlar {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  margin-bottom: 2rem;
}
.adim {
  display: flex;
  gap: 1rem;
  align-items: flex-start;
}
.adim-no {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--accent-soft-strong);
  color: var(--accent);
  font-weight: 700;
  font-size: 14px;
}
.adim-no.adim-yesil {
  background: rgba(34, 197, 94, 0.15);
  color: #4ade80;
}
.adim-icerik {
  flex: 1;
}
.adim-icerik strong {
  font-size: 0.95rem;
}
.adim-icerik p {
  font-size: 0.85rem;
  color: var(--text-secondary);
  margin: 2px 0 8px;
}
.onboard-alt {
  border-top: 1px solid var(--border);
  padding-top: 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
  align-items: center;
}
.onboard-atla {
  font-size: 0.8rem;
  color: var(--text-secondary);
  text-decoration: underline;
  cursor: pointer;
  margin-top: 4px;
}
.onboard-atla:hover {
  color: var(--text-primary);
}
</style>
