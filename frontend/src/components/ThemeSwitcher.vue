<template>
  <div
    ref="wrapperRef"
    class="theme-switcher-wrapper"
  >
    <button
      class="theme-toggle-btn"
      :title="`Tema & Renk Ayarları (${isDark ? 'Koyu' : 'Açık'})`"
      @click="menuAc"
    >
      <i
        :class="isDark ? 'pi pi-moon' : 'pi pi-sun'"
        class="theme-icon"
      />
    </button>

    <div
      v-if="menuAcik"
      ref="menuRef"
      class="theme-menu"
      :style="menuStil"
      @click.stop
    >
      <div class="menu-header">
        <span>{{ $t('theme.settings') }}</span>
        <i
          class="pi pi-times close-btn"
          @click="menuAcik = false"
        />
      </div>

      <div class="menu-section">
        <label class="section-label">{{ $t('theme.mode') }}</label>
        <div class="mode-toggle-group">
          <button
            class="mode-btn"
            :class="{ active: !isDark }"
            @click="applyMode('light')"
          >
            <i class="pi pi-sun" /> {{ $t('theme.lightShort') }}
          </button>
          <button
            class="mode-btn"
            :class="{ active: isDark }"
            @click="applyMode('dark')"
          >
            <i class="pi pi-moon" /> {{ $t('theme.darkShort') }}
          </button>
        </div>
      </div>

      <div class="menu-section">
        <label class="section-label">{{ $t('theme.accent') }}</label>
        <div class="color-options">
          <button
            v-for="c in colors"
            :key="c.name"
            class="color-dot"
            :class="{ active: accentColor === c.value }"
            :style="{ background: c.value }"
            :title="c.name"
            @click="applyColor(c.value)"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted, onUnmounted } from 'vue'
import { useTheme } from '../composables/useTheme.js'

const menuAcik = ref(false)
const wrapperRef = ref(null)
const menuRef = ref(null)
const menuStil = ref({})
const { isDark, accentColor, applyMode, applyColor, initTheme } = useTheme()

const colors = [
  { name: 'Okyanus Mavisi', value: '#3b82f6' },
  { name: 'Zümrüt Yeşil', value: '#10b981' },
  { name: 'Asil Mor', value: '#8b5cf6' },
  { name: 'Sıcak Amber', value: '#f59e0b' }
]

const menuAc = () => {
  menuAcik.value = !menuAcik.value
  if (menuAcik.value) {
    nextTick(() => {
      const rect = wrapperRef.value?.getBoundingClientRect()
      if (!rect) return
      const mh = menuRef.value ? menuRef.value.offsetHeight : 220
      menuStil.value = {
        position: 'fixed',
        top: Math.max(8, rect.top - mh - 8) + 'px',
        left: Math.max(8, rect.left) + 'px',
        bottom: 'auto'
      }
    })
  }
}

const disTiklaKapat = (e) => {
  if (!e.target.closest('.theme-switcher-wrapper')) {
    menuAcik.value = false
  }
}

onMounted(() => {
  initTheme()
  window.addEventListener('click', disTiklaKapat)
})

onUnmounted(() => {
  window.removeEventListener('click', disTiklaKapat)
})
</script>

<style scoped>
.theme-switcher-wrapper {
  position: relative;
  display: inline-block;
}

.theme-toggle-btn {
  background: rgba(148, 163, 184, 0.08);
  border: 1px solid rgba(148, 163, 184, 0.15);
  color: var(--text-secondary, #cbd5e1);
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
}

.theme-toggle-btn:hover {
  background: rgba(59, 130, 246, 0.15);
  border-color: #3b82f6;
}

.theme-icon {
  font-size: 16px;
  color: #f59e0b;
}

.theme-menu {
  width: 240px;
  background: var(--bg-card, #1e293b);
  border: 1px solid var(--border, rgba(255, 255, 255, 0.15));
  border-radius: 12px;
  padding: 12px 16px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.4);
  z-index: 99999;
}

.menu-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--border, rgba(255, 255, 255, 0.1));
}

.close-btn {
  cursor: pointer;
  font-size: 12px;
  opacity: 0.6;
}

.close-btn:hover {
  opacity: 1;
}

.menu-section {
  margin-bottom: 12px;
}

.section-label {
  display: block;
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  color: var(--text-secondary, #94a3b8);
  margin-bottom: 6px;
}

.mode-toggle-group {
  display: flex;
  gap: 6px;
  background: rgba(0, 0, 0, 0.2);
  padding: 4px;
  border-radius: 8px;
}

.mode-btn {
  flex: 1;
  padding: 6px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  transition: all 0.15s;
}

.mode-btn.active {
  background: var(--accent, #3b82f6);
  color: #ffffff;
}

.color-options {
  display: flex;
  gap: 10px;
  justify-content: space-between;
  padding-top: 4px;
}

.color-dot {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: 2px solid transparent;
  cursor: pointer;
  transition: all 0.15s;
}

.color-dot:hover {
  transform: scale(1.15);
}

.color-dot.active {
  border-color: #ffffff;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.4);
}
</style>
