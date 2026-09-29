<template>
  <div class="giris-hero-alani">
    <div class="hero-brand">
      <div class="hero-logo-box">
        <img
          :src="markaLogoIcon"
          class="hero-logo-icon"
          alt="RasPel"
        >
      </div>
      <div class="hero-brand-text">
        <span class="hero-brand-name">RasPel ERP</span>
        <span class="hero-badge">v1.36.0 Enterprise</span>
      </div>
    </div>

    <span class="hero-eyebrow"><i class="pi pi-bolt" /> {{ t('giris.heroEyebrow') }}</span>
    <h1 class="hero-title">
      {{ t('giris.heroTitle') }}
    </h1>
    <div class="hero-rotate">
      <transition
        name="rotate-fade"
        mode="out-in"
      >
        <span
          :key="rotateIndex"
          class="hero-rotate-item"
        >{{ rotateItems[rotateIndex] }}</span>
      </transition>
    </div>
    <p class="hero-desc">
      {{ t('giris.heroSubtitle') }}
    </p>

    <!-- Canli urun vitrini -->
    <div class="hero-preview-wrap">
      <LoginPreview />
    </div>

    <!-- Ozellik listesi -->
    <div class="hero-features">
      <span class="hero-features-title">{{ t('giris.featuresTitle') }}</span>
      <div class="hero-features-grid">
        <div class="feature-item">
          <span class="feature-ikon"><i class="pi pi-sparkles" /></span>
          <div class="feature-metin">
            <strong>{{ t('giris.featureAiTitle') }}</strong>
            <span>{{ t('giris.featureAiDesc') }}</span>
          </div>
        </div>
        <div class="feature-item">
          <span class="feature-ikon"><i class="pi pi-file-check" /></span>
          <div class="feature-metin">
            <strong>{{ t('giris.featureEInvoiceTitle') }}</strong>
            <span>{{ t('giris.featureEInvoiceDesc') }}</span>
          </div>
        </div>
        <div class="feature-item">
          <span class="feature-ikon"><i class="pi pi-box" /></span>
          <div class="feature-metin">
            <strong>{{ t('giris.featureStockTitle') }}</strong>
            <span>{{ t('giris.featureStockDesc') }}</span>
          </div>
        </div>
        <div class="feature-item">
          <span class="feature-ikon"><i class="pi pi-chart-bar" /></span>
          <div class="feature-metin">
            <strong>{{ t('giris.featureProfitTitle') }}</strong>
            <span>{{ t('giris.featureProfitDesc') }}</span>
          </div>
        </div>
        <div class="feature-item">
          <span class="feature-ikon"><i class="pi pi-compass" /></span>
          <div class="feature-metin">
            <strong>{{ t('giris.featureFieldTitle') }}</strong>
            <span>{{ t('giris.featureFieldDesc') }}</span>
          </div>
        </div>
        <div class="feature-item">
          <span class="feature-ikon"><i class="pi pi-wallet" /></span>
          <div class="feature-metin">
            <strong>{{ t('giris.featureCollectionTitle') }}</strong>
            <span>{{ t('giris.featureCollectionDesc') }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Guven rozetleri (notr) -->
    <div class="hero-trust">
      <span class="trust-badge"><i class="pi pi-shield" /> {{ t('giris.trustKvkk') }}</span>
      <span class="trust-badge"><i class="pi pi-lock" /> {{ t('giris.trustSsl') }}</span>
      <span class="trust-badge"><i class="pi pi-key" /> {{ t('giris.trust2fa') }}</span>
      <span class="trust-badge"><i class="pi pi-cloud" /> {{ t('giris.trustCloud') }}</span>
    </div>
  </div>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import LoginPreview from './LoginPreview.vue'

defineProps({
  rotateIndex: { type: Number, default: 0 },
  rotateItems: { type: Array, default: () => [] }
})
const { t } = useI18n()

const markaLogoIcon = '/logo-icon.png'
</script>

<style scoped>
/* Acik temada aksan vurgulari koyu tonla (okunabilirlik) */
[data-theme='light'] .hero-eyebrow {
  color: var(--giris-aksan-koyu);
}
[data-theme='light'] .hero-rotate-item {
  background: linear-gradient(90deg, var(--giris-aksan), var(--giris-aksan-koyu));
}
[data-theme='light'] .feature-ikon {
  color: var(--giris-aksan-koyu);
}
[data-theme='light'] .trust-badge i {
  color: var(--giris-aksan-koyu);
}
[data-theme='light'] .feature-item {
  background: rgba(255, 255, 255, 0.72);
  border-color: rgba(203, 213, 225, 0.7);
}
[data-theme='light'] .hero-features-title {
  color: var(--text-secondary);
}

.hero-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}
.hero-logo-box {
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.hero-logo-icon {
  width: 44px;
  height: 44px;
  object-fit: contain;
}
.hero-brand-name {
  font-size: 22px;
  font-weight: 800;
  color: var(--text-primary);
  letter-spacing: -0.5px;
}
.hero-badge {
  font-size: 11px;
  font-weight: 700;
  padding: 3px 8px;
  background: var(--giris-tint-15);
  color: var(--giris-aksan-parlak);
  border: 1px solid var(--giris-tint-30);
  border-radius: 20px;
  margin-left: 8px;
}

.hero-title {
  font-size: 24px;
  font-weight: 800;
  color: var(--text-primary);
  line-height: 1.22;
  margin: 0 0 8px;
  letter-spacing: -0.5px;
}
.hero-desc {
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.45;
  margin: 0 0 4px;
}
.hero-eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 11.5px;
  font-weight: 700;
  letter-spacing: 0.6px;
  text-transform: uppercase;
  color: var(--giris-aksan-parlak);
  margin-bottom: 4px;
}
.hero-eyebrow i {
  font-size: 11px;
}

/* Ozellik listesi */
.hero-features {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.hero-features-title {
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.6px;
  text-transform: uppercase;
  color: var(--text-muted);
}
.hero-features-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 6px;
}
.feature-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 7px 9px;
  border-radius: 11px;
  background: rgba(148, 163, 184, 0.06);
  border: 1px solid rgba(148, 163, 184, 0.12);
  transition: transform 0.18s ease, border-color 0.18s ease;
}
.feature-item:hover {
  transform: translateY(-2px);
  border-color: var(--giris-tint-35);
}
.feature-ikon {
  width: 26px;
  height: 26px;
  flex-shrink: 0;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--giris-tint-14);
  color: var(--giris-aksan-parlak);
  font-size: 12px;
}
.feature-metin {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}
.feature-metin strong {
  font-size: 12px;
  color: var(--text-primary);
  font-weight: 700;
}
.feature-metin span {
  font-size: 10.5px;
  color: var(--text-muted);
  line-height: 1.35;
}

/* Guven rozetleri */
.hero-trust {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
  padding-top: 10px;
  border-top: 1px solid rgba(148, 163, 184, 0.12);
}
.trust-badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 5px 10px;
  border-radius: 20px;
  font-size: 10.5px;
  font-weight: 600;
  color: var(--text-secondary);
  background: rgba(148, 163, 184, 0.08);
  border: 1px solid rgba(148, 163, 184, 0.16);
}
.trust-badge i {
  color: var(--giris-aksan);
  font-size: 11px;
}

/* Hero donen baslik */
.hero-rotate {
  min-height: 22px;
  margin: 0 0 8px;
}
.hero-rotate-item {
  font-size: 15.5px;
  font-weight: 700;
  background: linear-gradient(90deg, var(--giris-aksan-parlak), var(--giris-aksan-parlak));
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}
.rotate-fade-enter-active,
.rotate-fade-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}
.rotate-fade-enter-from {
  opacity: 0;
  transform: translateY(8px);
}
.rotate-fade-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

/* Canli urun vitrini */
.hero-preview-wrap {
  margin: 2px 0;
  transform: translate3d(var(--pxs, 0), var(--pys, 0), 0);
  transition: transform 0.25s ease-out;
  will-change: transform;
}

@media (max-width: 1180px) {
  .hero-features-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-height: 900px) {
  .giris-hero-alani {
    padding: 20px 28px;
    gap: 8px;
  }
  .hero-brand {
    margin-bottom: 10px;
  }
  .hero-title {
    font-size: 21px;
  }
  .hero-desc {
    font-size: 12.5px;
  }
  .feature-metin span {
    display: none;
  }
  .feature-item {
    padding: 6px 9px;
  }
  .trust-badge {
    font-size: 10px;
    padding: 4px 9px;
  }
}

@media (max-height: 760px) {
  .giris-hero-alani {
    padding: 16px 24px;
    gap: 6px;
  }
  .hero-brand,
  .hero-rotate,
  .hero-eyebrow {
    display: none;
  }
  .hero-title {
    font-size: 19px;
    margin-bottom: 6px;
  }
  .hero-desc {
    display: none;
  }
  .hero-features {
    gap: 7px;
  }
  .hero-trust {
    padding-top: 8px;
  }
}

@media (max-height: 620px) {
  .hero-features {
    display: none;
  }
  .mobil-preview {
    display: none;
  }
}
</style>
