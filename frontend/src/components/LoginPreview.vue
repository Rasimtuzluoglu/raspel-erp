<template>
  <div
    ref="kokEl"
    class="login-preview"
  >
    <!-- Sahte uygulama penceresi (tarayici cercevesi) -->
    <div class="preview-window">
      <div class="preview-chrome">
        <span class="chrome-dot dot-red" />
        <span class="chrome-dot dot-yellow" />
        <span class="chrome-dot dot-green" />
        <span class="chrome-url">
          <i class="pi pi-lock" />
          raspel-erp.app
        </span>
      </div>

      <div class="preview-body">
        <!-- Mini kenar menu -->
        <aside class="preview-side">
          <span class="side-logo"><img
            :src="markaLogo"
            alt=""
          ></span>
          <span class="side-item aktif"><i class="pi pi-chart-line" /></span>
          <span class="side-item"><i class="pi pi-file-check" /></span>
          <span class="side-item"><i class="pi pi-box" /></span>
          <span class="side-item"><i class="pi pi-users" /></span>
          <span class="side-item"><i class="pi pi-sparkles" /></span>
        </aside>

        <main class="preview-main">
          <transition
            name="preview-fade"
            mode="out-in"
          >
            <!-- Panel -->
            <div
              v-if="aktif === 'dashboard'"
              key="dashboard"
              class="mock mock-dash"
            >
              <div class="mock-kpi-row">
                <div class="mock-kpi">
                  <span class="kpi-label" />
                  <span class="kpi-value" />
                </div>
                <div class="mock-kpi">
                  <span class="kpi-label" />
                  <span class="kpi-value" />
                </div>
                <div class="mock-kpi">
                  <span class="kpi-label" />
                  <span class="kpi-value" />
                </div>
              </div>
              <svg
                class="mock-chart"
                viewBox="0 0 300 90"
                preserveAspectRatio="none"
                aria-hidden="true"
              >
                <defs>
                  <linearGradient
                    id="lpGrad"
                    x1="0"
                    y1="0"
                    x2="0"
                    y2="1"
                  >
                    <stop
                      offset="0%"
                      stop-color="var(--giris-aksan, #10b981)"
                      stop-opacity="0.35"
                    />
                    <stop
                      offset="100%"
                      stop-color="var(--giris-aksan, #10b981)"
                      stop-opacity="0"
                    />
                  </linearGradient>
                </defs>
                <path
                  d="M0,72 C40,52 62,60 92,40 C122,20 152,46 182,30 C212,14 244,36 300,12 L300,90 L0,90 Z"
                  fill="url(#lpGrad)"
                />
                <path
                  class="mock-chart-line"
                  d="M0,72 C40,52 62,60 92,40 C122,20 152,46 182,30 C212,14 244,36 300,12"
                  fill="none"
                  stroke="var(--giris-aksan, #10b981)"
                  stroke-width="2.5"
                  stroke-linecap="round"
                />
              </svg>
              <div class="mock-rows">
                <div class="mock-row">
                  <span class="mock-avatar" />
                  <span class="mock-line w55" />
                  <span class="mock-pill" />
                </div>
                <div class="mock-row">
                  <span class="mock-avatar" />
                  <span class="mock-line w40" />
                  <span class="mock-pill" />
                </div>
              </div>
            </div>

            <!-- e-Fatura -->
            <div
              v-else-if="aktif === 'invoice'"
              key="invoice"
              class="mock mock-invoice"
            >
              <div class="mock-inv-head">
                <span class="mock-line w45" />
                <span class="mock-badge" />
              </div>
              <div class="mock-inv-rows">
                <div class="mock-inv-row">
                  <span class="mock-line w65" />
                  <span class="mock-line w15" />
                </div>
                <div class="mock-inv-row">
                  <span class="mock-line w50" />
                  <span class="mock-line w15" />
                </div>
                <div class="mock-inv-row">
                  <span class="mock-line w60" />
                  <span class="mock-line w15" />
                </div>
                <div class="mock-inv-row total">
                  <span class="mock-line w30" />
                  <span class="mock-line w20 accent" />
                </div>
              </div>
            </div>

            <!-- Stok -->
            <div
              v-else-if="aktif === 'stock'"
              key="stock"
              class="mock mock-stock"
            >
              <div class="mock-stock-row">
                <span class="mock-thumb" />
                <div class="mock-stock-info">
                  <span class="mock-line w60" />
                  <span class="mock-bar"><i style="width: 82%" /></span>
                </div>
                <span class="mock-qty">82</span>
              </div>
              <div class="mock-stock-row">
                <span class="mock-thumb" />
                <div class="mock-stock-info">
                  <span class="mock-line w45" />
                  <span class="mock-bar"><i
                    class="low"
                    style="width: 24%"
                  /></span>
                </div>
                <span class="mock-qty low">24</span>
              </div>
              <div class="mock-stock-row">
                <span class="mock-thumb" />
                <div class="mock-stock-info">
                  <span class="mock-line w50" />
                  <span class="mock-bar"><i style="width: 61%" /></span>
                </div>
                <span class="mock-qty">61</span>
              </div>
            </div>

            <!-- Üretim -->
            <div
              v-else-if="aktif === 'production'"
              key="production"
              class="mock mock-prod"
            >
              <div class="mock-prod-head">
                <span class="mock-line w45" />
                <span class="mock-badge" />
              </div>
              <div class="mock-prod-row">
                <span class="mock-prod-icon"><i class="pi pi-cog" /></span>
                <div class="mock-prod-info">
                  <span class="mock-line w60" />
                  <span class="mock-bar"><i style="width: 68%" /></span>
                </div>
                <span class="mock-qty">68%</span>
              </div>
              <div class="mock-prod-row">
                <span class="mock-prod-icon"><i class="pi pi-cog" /></span>
                <div class="mock-prod-info">
                  <span class="mock-line w50" />
                  <span class="mock-bar"><i style="width: 32%" /></span>
                </div>
                <span class="mock-qty">32%</span>
              </div>
              <div class="mock-prod-row">
                <span class="mock-prod-icon"><i class="pi pi-check" /></span>
                <div class="mock-prod-info">
                  <span class="mock-line w55" />
                  <span class="mock-bar"><i style="width: 92%" /></span>
                </div>
                <span class="mock-qty">92%</span>
              </div>
            </div>

            <!-- Kârlılık -->
            <div
              v-else-if="aktif === 'profit'"
              key="profit"
              class="mock mock-profit"
            >
              <div class="mock-profit-head">
                <span class="mock-line w40" />
                <span class="mock-line w20 accent" />
              </div>
              <div class="mock-bars">
                <span style="height: 42%" />
                <span style="height: 66%" />
                <span style="height: 54%" />
                <span style="height: 88%" />
                <span style="height: 72%" />
                <span style="height: 95%" />
              </div>
            </div>

            <!-- AI Asistan -->
            <div
              v-else
              key="ai"
              class="mock mock-ai"
            >
              <div class="mock-bubble user">
                <span class="mock-line w70" />
              </div>
              <div class="mock-bubble bot">
                <span class="mock-line w85" />
                <span class="mock-line w60" />
              </div>
              <div class="mock-insight">
                <i class="pi pi-sparkles" />
                <span class="mock-line w55" />
              </div>
            </div>
          </transition>
        </main>
      </div>
    </div>

    <!-- Sekmeler -->
    <div
      class="preview-tabs"
      role="tablist"
    >
      <button
        type="button"
        role="tab"
        class="preview-tab"
        :class="{ aktif: aktif === 'dashboard' }"
        :aria-selected="aktif === 'dashboard'"
        @click="sec('dashboard')"
      >
        <i class="pi pi-chart-line" /> {{ $t('giris.previewTabDashboard') }}
      </button>
      <button
        type="button"
        role="tab"
        class="preview-tab"
        :class="{ aktif: aktif === 'invoice' }"
        :aria-selected="aktif === 'invoice'"
        @click="sec('invoice')"
      >
        <i class="pi pi-file-check" /> {{ $t('giris.previewTabInvoice') }}
      </button>
      <button
        type="button"
        role="tab"
        class="preview-tab"
        :class="{ aktif: aktif === 'stock' }"
        :aria-selected="aktif === 'stock'"
        @click="sec('stock')"
      >
        <i class="pi pi-box" /> {{ $t('giris.previewTabStock') }}
      </button>
      <button
        type="button"
        role="tab"
        class="preview-tab"
        :class="{ aktif: aktif === 'production' }"
        :aria-selected="aktif === 'production'"
        @click="sec('production')"
      >
        <i class="pi pi-cog" /> {{ $t('giris.previewTabProduction') }}
      </button>
      <button
        type="button"
        role="tab"
        class="preview-tab"
        :class="{ aktif: aktif === 'profit' }"
        :aria-selected="aktif === 'profit'"
        @click="sec('profit')"
      >
        <i class="pi pi-chart-bar" /> {{ $t('giris.previewTabProfit') }}
      </button>
      <button
        type="button"
        role="tab"
        class="preview-tab"
        :class="{ aktif: aktif === 'ai' }"
        :aria-selected="aktif === 'ai'"
        @click="sec('ai')"
      >
        <i class="pi pi-sparkles" /> {{ $t('giris.previewTabAi') }}
      </button>
    </div>

    <div
      class="preview-progress"
      aria-hidden="true"
    >
      <span :style="{ width: ilerleme + '%' }" />
    </div>

    <transition
      name="preview-fade"
      mode="out-in"
    >
      <p
        :key="aktif"
        class="preview-caption"
      >
        <span v-if="aktif === 'dashboard'">{{ $t('giris.previewCapDashboard') }}</span>
        <span v-else-if="aktif === 'invoice'">{{ $t('giris.previewCapInvoice') }}</span>
        <span v-else-if="aktif === 'stock'">{{ $t('giris.previewCapStock') }}</span>
        <span v-else-if="aktif === 'production'">{{ $t('giris.previewCapProduction') }}</span>
        <span v-else-if="aktif === 'profit'">{{ $t('giris.previewCapProfit') }}</span>
        <span v-else>{{ $t('giris.previewCapAi') }}</span>
      </p>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, watchEffect } from 'vue'
import { useElementHover, useIntervalFn, usePreferredReducedMotion } from '@vueuse/core'

const markaLogo = '/logo-icon.png'

const sekmeler = ['dashboard', 'invoice', 'stock', 'production', 'profit', 'ai']
const aktif = ref('dashboard')
const kokEl = ref(null)

const aktifIndex = computed(() => Math.max(0, sekmeler.indexOf(aktif.value)))
const ilerleme = computed(() => ((aktifIndex.value + 1) / sekmeler.length) * 100)

const hover = useElementHover(kokEl)
const azHareket = usePreferredReducedMotion()
const hareketAzalt = computed(() => azHareket.value === 'reduce')

const { pause, resume } = useIntervalFn(() => {
  const i = sekmeler.indexOf(aktif.value)
  aktif.value = sekmeler[(i + 1) % sekmeler.length]
}, 4500)

// Fare uzerindeyken veya hareket azaltma tercihinde otomatik gezinmeyi durdur.
watchEffect(() => {
  if (hareketAzalt.value || hover.value) pause()
  else resume()
})

const sec = (id) => {
  aktif.value = id
}
</script>

<style scoped>
.login-preview {
  width: 100%;
  max-width: 460px;
  margin: 0 auto;
}

/* Pencere */
.preview-window {
  border-radius: 14px;
  background: rgba(2, 6, 23, 0.72);
  border: 1px solid rgba(148, 163, 184, 0.22);
  box-shadow: 0 18px 44px -18px rgba(0, 0, 0, 0.7);
  overflow: hidden;
  backdrop-filter: blur(6px);
}
[data-theme='light'] .preview-window {
  background: rgba(255, 255, 255, 0.92);
  border-color: rgba(203, 213, 225, 0.9);
  box-shadow: 0 16px 36px -18px rgba(15, 23, 42, 0.25);
}

.preview-chrome {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 9px 12px;
  background: rgba(148, 163, 184, 0.08);
  border-bottom: 1px solid rgba(148, 163, 184, 0.14);
}
.chrome-dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  flex-shrink: 0;
}
.dot-red { background: #f87171; }
.dot-yellow { background: #fbbf24; }
.dot-green { background: #34d399; }
.chrome-url {
  margin-left: 8px;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 10.5px;
  color: var(--text-muted, #94a3b8);
  background: rgba(148, 163, 184, 0.1);
  border-radius: 8px;
  padding: 2px 10px;
}
.chrome-url i {
  font-size: 9px;
  color: #34d399;
}

/* Govde */
.preview-body {
  display: flex;
  min-height: 164px;
}
.preview-side {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 12px 9px;
  background: rgba(148, 163, 184, 0.05);
  border-right: 1px solid rgba(148, 163, 184, 0.12);
}
.side-logo {
  width: 26px;
  height: 26px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 6px;
}
.side-logo img {
  width: 26px;
  height: 26px;
  object-fit: contain;
}
.side-item {
  width: 26px;
  height: 26px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-muted, #94a3b8);
  font-size: 12px;
  background: rgba(148, 163, 184, 0.08);
}
.side-item.aktif {
  background: var(--giris-tint-18, rgba(16, 185, 129,0.18));
  color: var(--giris-aksan-parlak, #34d399);
}

.preview-main {
  flex: 1;
  min-width: 0;
  padding: 12px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

/* Ortak mock parcalari */
.mock {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.mock-line,
.kpi-label,
.kpi-value,
.mock-pill,
.mock-badge {
  display: block;
  height: 8px;
  border-radius: 5px;
  background: rgba(148, 163, 184, 0.22);
}
.mock-line {
  height: 7px;
}
.w15 { width: 15%; }
.w20 { width: 20%; }
.w30 { width: 30%; }
.w40 { width: 40%; }
.w45 { width: 45%; }
.w50 { width: 50%; }
.w55 { width: 55%; }
.w60 { width: 60%; }
.w65 { width: 65%; }
.w70 { width: 70%; }
.w85 { width: 85%; }
.accent { background: var(--giris-tint-55, rgba(16, 185, 129,0.55)); }
.low { background: rgba(16, 185, 129, 0.75) !important; }

/* Panel */
.mock-kpi-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}
.mock-kpi {
  background: rgba(148, 163, 184, 0.08);
  border: 1px solid rgba(148, 163, 184, 0.12);
  border-radius: 9px;
  padding: 8px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.kpi-label { width: 60%; background: rgba(148, 163, 184, 0.18); }
.kpi-value { width: 85%; height: 11px; background: var(--giris-tint-50, rgba(16, 185, 129,0.5)); }
.mock-chart {
  width: 100%;
  height: 46px;
  display: block;
}
.mock-chart-line {
  stroke-dasharray: 620;
  stroke-dashoffset: 620;
  animation: cizgi 1.6s ease forwards;
}
@keyframes cizgi {
  to { stroke-dashoffset: 0; }
}
.mock-rows {
  display: flex;
  flex-direction: column;
  gap: 7px;
}
.mock-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.mock-avatar {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: rgba(148, 163, 184, 0.25);
  flex-shrink: 0;
}
.mock-pill {
  width: 34px;
  height: 12px;
  border-radius: 8px;
  background: var(--giris-tint-30, rgba(16, 185, 129,0.3));
  margin-left: auto;
}

/* e-Fatura */
.mock-inv-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 8px;
  border-bottom: 1px dashed rgba(148, 163, 184, 0.2);
}
.mock-badge {
  width: 46px;
  height: 14px;
  border-radius: 8px;
  background: var(--giris-tint-28, rgba(16, 185, 129,0.28));
}
.mock-inv-rows {
  display: flex;
  flex-direction: column;
  gap: 9px;
}
.mock-inv-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.mock-inv-row.total {
  padding-top: 8px;
  border-top: 1px solid rgba(148, 163, 184, 0.18);
}
.mock-inv-row.total .mock-line {
  height: 10px;
}

/* Stok */
.mock-stock-row {
  display: flex;
  align-items: center;
  gap: 9px;
}
.mock-thumb {
  width: 26px;
  height: 26px;
  border-radius: 7px;
  background: rgba(148, 163, 184, 0.16);
  flex-shrink: 0;
}
.mock-stock-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 5px;
}
.mock-bar {
  display: block;
  height: 6px;
  border-radius: 4px;
  background: rgba(148, 163, 184, 0.16);
  overflow: hidden;
}
.mock-bar > i {
  display: block;
  height: 100%;
  border-radius: 4px;
  background: linear-gradient(90deg, var(--giris-aksan, #10b981), var(--giris-aksan-parlak, #34d399));
}
.mock-qty {
  font-size: 11px;
  font-weight: 700;
  color: var(--text-secondary, #cbd5e1);
  min-width: 22px;
  text-align: right;
}
.mock-qty.low {
  color: #f59e0b;
  background: none;
}

/* AI */
.mock-ai {
  gap: 8px;
}
.mock-bubble {
  border-radius: 11px;
  padding: 9px 11px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.mock-bubble.user {
  align-self: flex-end;
  width: 72%;
  background: var(--giris-tint-16, rgba(16, 185, 129,0.16));
  border: 1px solid var(--giris-tint-25, rgba(16, 185, 129,0.25));
}
.mock-bubble.bot {
  align-self: flex-start;
  width: 88%;
  background: rgba(148, 163, 184, 0.1);
  border: 1px solid rgba(148, 163, 184, 0.16);
}
.mock-insight {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 11px;
  border-radius: 11px;
  background: var(--giris-tint-14, rgba(16, 185, 129,0.14));
  border: 1px solid var(--giris-tint-28, rgba(16, 185, 129,0.28));
}
.mock-insight i {
  color: var(--giris-aksan-parlak, #34d399);
  font-size: 13px;
}
.mock-insight .mock-line {
  flex: 1;
}

/* Sekmeler */
.preview-tabs {
  display: flex;
  gap: 5px;
  margin-top: 10px;
  flex-wrap: wrap;
}
.preview-tab {
  flex: 1;
  min-width: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  padding: 5px 7px;
  border-radius: 9px;
  background: rgba(148, 163, 184, 0.07);
  border: 1px solid rgba(148, 163, 184, 0.16);
  color: var(--text-secondary, #cbd5e1);
  font-size: 11px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.18s ease;
  white-space: nowrap;
  touch-action: manipulation;
}
.preview-tab i {
  font-size: 11px;
}
.preview-tab:hover {
  border-color: var(--giris-tint-40, rgba(16, 185, 129,0.4));
  color: var(--text-primary, #f1f5f9);
}
.preview-tab.aktif {
  background: var(--giris-tint-16, rgba(16, 185, 129,0.16));
  border-color: var(--giris-tint-50, rgba(16, 185, 129,0.5));
  color: var(--giris-aksan-parlak, #34d399);
}

.preview-caption {
  margin: 8px 2px 0;
  font-size: 12px;
  color: var(--text-secondary, #cbd5e1);
  line-height: 1.4;
  min-height: 30px;
}

/* Gecisler */
.preview-fade-enter-active,
.preview-fade-leave-active {
  transition:
    opacity 0.25s ease,
    transform 0.25s ease;
}
.preview-fade-enter-from,
.preview-fade-leave-to {
  opacity: 0;
  transform: translateY(6px);
}

/* Üretim */
.mock-prod-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 8px;
  border-bottom: 1px dashed rgba(148, 163, 184, 0.2);
}
.mock-prod-row {
  display: flex;
  align-items: center;
  gap: 9px;
}
.mock-prod-icon {
  width: 26px;
  height: 26px;
  border-radius: 8px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--giris-tint-16, rgba(16, 185, 129,0.16));
  color: var(--giris-aksan-parlak, #34d399);
  font-size: 12px;
}
.mock-prod-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 5px;
}

/* Kârlılık */
.mock-profit-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.mock-bars {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  height: 84px;
  padding: 8px 4px 0;
  border-bottom: 1px solid rgba(148, 163, 184, 0.18);
}
.mock-bars span {
  flex: 1;
  min-height: 10px;
  border-radius: 6px 6px 0 0;
  background: linear-gradient(180deg, var(--giris-aksan-parlak, #34d399), var(--giris-tint-25, rgba(16, 185, 129,0.25)));
  transform-origin: bottom;
  animation: barYuksekligi 0.6s ease;
}
@keyframes barYuksekligi {
  from { transform: scaleY(0); }
  to { transform: scaleY(1); }
}

/* İlerleme göstergesi */
.preview-progress {
  height: 3px;
  border-radius: 3px;
  margin-top: 10px;
  background: rgba(148, 163, 184, 0.15);
  overflow: hidden;
}
.preview-progress span {
  display: block;
  height: 100%;
  border-radius: 3px;
  background: linear-gradient(90deg, var(--giris-aksan, #10b981), var(--giris-aksan-parlak, #34d399));
  transition: width 0.4s ease;
}

@media (max-width: 520px) {
  .preview-tab span,
  .preview-tab {
    font-size: 10.5px;
  }
}

/* Kisa ekranlar: vitrin daha da kompakt */
@media (max-height: 900px) {
  .preview-body {
    min-height: 148px;
  }
  .mock-chart {
    height: 40px;
  }
  .mock-bars {
    height: 68px;
  }
  .preview-caption {
    min-height: 0;
  }
}

@media (max-height: 760px) {
  .preview-body {
    min-height: 132px;
  }
  .preview-caption {
    display: none;
  }
}

/* Hareket azaltma tercihi */
@media (prefers-reduced-motion: reduce) {
  .mock-chart-line {
    stroke-dashoffset: 0;
    animation: none;
  }
  .mock-bars span {
    animation: none;
  }
  .preview-progress span {
    transition: none;
  }
  .preview-fade-enter-active,
  .preview-fade-leave-active {
    transition: none;
  }
}
</style>
