<template>
  <div class="empty-state">
    <div class="empty-state-icon-wrapper">
      <div class="empty-state-glow" />
      <!-- İllüstrasyon: opsiyonel, ikon yerine çizim gösterir (makyaj katmanı). -->
      <svg
        v-if="illustrasyon"
        class="empty-state-cizim"
        viewBox="0 0 96 96"
        fill="none"
        aria-hidden="true"
      >
        <template v-if="illustrasyon === 'liste'">
          <rect
            x="20"
            y="16"
            width="56"
            height="64"
            rx="8"
            stroke="currentColor"
            stroke-width="2.5"
            opacity="0.55"
          />
          <rect
            x="20"
            y="16"
            width="56"
            height="14"
            rx="8"
            fill="currentColor"
            opacity="0.12"
          />
          <path
            d="M32 42h32M32 52h24M32 62h18"
            stroke="currentColor"
            stroke-width="2.5"
            stroke-linecap="round"
            opacity="0.7"
          />
          <circle
            cx="70"
            cy="68"
            r="12"
            fill="currentColor"
            opacity="0.12"
          />
          <path
            d="m65 68 4 4 7-8"
            stroke="currentColor"
            stroke-width="2.5"
            stroke-linecap="round"
            stroke-linejoin="round"
          />
        </template>

        <template v-else-if="illustrasyon === 'depo'">
          <path
            d="M18 40 48 22l30 18v38H18V40Z"
            stroke="currentColor"
            stroke-width="2.5"
            stroke-linejoin="round"
            opacity="0.6"
          />
          <path
            d="M18 40h60"
            stroke="currentColor"
            stroke-width="2.5"
            opacity="0.4"
          />
          <rect
            x="30"
            y="52"
            width="36"
            height="8"
            rx="2"
            stroke="currentColor"
            stroke-width="2"
            opacity="0.7"
          />
          <rect
            x="30"
            y="66"
            width="36"
            height="12"
            rx="2"
            fill="currentColor"
            opacity="0.12"
          />
          <rect
            x="30"
            y="66"
            width="36"
            height="12"
            rx="2"
            stroke="currentColor"
            stroke-width="2"
            opacity="0.6"
          />
        </template>

        <template v-else-if="illustrasyon === 'arama'">
          <circle
            cx="44"
            cy="44"
            r="22"
            stroke="currentColor"
            stroke-width="2.5"
            opacity="0.6"
          />
          <circle
            cx="44"
            cy="44"
            r="22"
            fill="currentColor"
            opacity="0.08"
          />
          <path
            d="m60 60 16 16"
            stroke="currentColor"
            stroke-width="3"
            stroke-linecap="round"
            opacity="0.8"
          />
          <path
            d="M34 44h20M44 34v20"
            stroke="currentColor"
            stroke-width="2.5"
            stroke-linecap="round"
            opacity="0.55"
          />
        </template>

        <template v-else-if="illustrasyon === 'rapor'">
          <path
            d="M24 72V26"
            stroke="currentColor"
            stroke-width="2.5"
            stroke-linecap="round"
            opacity="0.4"
          />
          <path
            d="M24 72h50"
            stroke="currentColor"
            stroke-width="2.5"
            stroke-linecap="round"
            opacity="0.4"
          />
          <rect
            x="32"
            y="54"
            width="10"
            height="18"
            rx="2"
            fill="currentColor"
            opacity="0.18"
          />
          <rect
            x="48"
            y="44"
            width="10"
            height="28"
            rx="2"
            fill="currentColor"
            opacity="0.28"
          />
          <rect
            x="64"
            y="34"
            width="10"
            height="38"
            rx="2"
            fill="currentColor"
            opacity="0.4"
          />
          <path
            d="m30 40 14-8 12 6 14-12"
            stroke="currentColor"
            stroke-width="2.5"
            stroke-linecap="round"
            stroke-linejoin="round"
            opacity="0.75"
          />
        </template>
      </svg>

      <i
        v-else
        :class="icon"
        class="empty-state-icon"
      />
    </div>
    <p class="empty-state-text">
      {{ message || $t('common.noData') }}
    </p>
    <p
      v-if="subMessage"
      class="empty-state-sub"
    >
      {{ subMessage }}
    </p>
    <Button
      v-if="actionLabel"
      :label="actionLabel"
      :icon="actionIcon"
      class="p-button-sm p-button-outlined empty-state-action"
      @click="$emit('action')"
    />
  </div>
</template>

<script setup>
defineProps({
  message: { type: String, default: '' },
  subMessage: { type: String, default: '' },
  icon: { type: String, default: 'pi pi-inbox' },
  actionLabel: { type: String, default: '' },
  actionIcon: { type: String, default: 'pi pi-plus' },
  /** Opsiyonel illüstrasyon: 'liste' | 'depo' | 'arama' | 'rapor'. */
  illustrasyon: {
    type: String,
    default: '',
    validator: (deger) => ['', 'liste', 'depo', 'arama', 'rapor'].includes(deger)
  }
})
defineEmits(['action'])
</script>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
  text-align: center;
}
.empty-state-icon-wrapper {
  position: relative;
  width: 88px;
  height: 88px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
}
.empty-state-glow {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: radial-gradient(circle, var(--accent-soft) 0%, transparent 70%);
  animation: empty-pulse 2.5s ease-in-out infinite;
}
.empty-state-icon {
  font-size: 40px;
  color: var(--text-secondary);
  position: relative;
  z-index: 1;
  text-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
}
.empty-state-cizim {
  position: relative;
  z-index: 1;
  width: 64px;
  height: 64px;
  color: var(--text-secondary);
}
@keyframes empty-pulse {
  0%,
  100% {
    transform: scale(1);
    opacity: 0.8;
  }
  50% {
    transform: scale(1.08);
    opacity: 1;
  }
}
.empty-state-text {
  font-size: 15px;
  color: var(--text-secondary, #94a3b8);
  margin: 0;
}
.empty-state-sub {
  font-size: 13px;
  color: var(--text-muted, #64748b);
  margin: 6px 0 0;
  max-width: 320px;
  line-height: 1.5;
}
.empty-state-action {
  margin-top: 16px;
}
</style>
