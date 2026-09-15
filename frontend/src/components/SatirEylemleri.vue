<template>
  <div
    ref="rootEl"
    class="satir-eylemler"
  >
    <Button
      ref="btnRef"
      icon="pi pi-ellipsis-v"
      class="p-button-rounded p-button-text"
      :title="$t('common.actions')"
      :aria-label="$t('common.actions')"
      aria-haspopup="true"
      :aria-expanded="acik"
      @click="acToggle($event)"
    />

    <!-- body'ye teleport: tablo scroll konteyneri icinde kirpilmayi onler -->
    <Teleport to="body">
      <transition name="fade">
        <div
          v-if="acik"
          ref="menuRef"
          class="eylem-menu"
          role="menu"
          :aria-label="$t('common.actions')"
          :style="menuStil"
          @click.stop
          @keydown="menuKeydown"
        >
          <button
            v-if="gorunur.duzenle"
            class="eylem-item"
            @click="calistir('duzenle')"
          >
            <i class="pi pi-pencil" /> {{ $t('common.edit') }}
          </button>
          <button
            v-if="gorunur.cogalt"
            class="eylem-item"
            @click="calistir('cogalt')"
          >
            <i class="pi pi-copy" /> {{ $t('common.duplicate') }}
          </button>

          <button
            v-for="(it, i) in items"
            :key="i"
            class="eylem-item"
            :class="it.sinif"
            @click="calistirItem(it)"
          >
            <i :class="it.ikon" /> {{ it.etiket }}
          </button>

          <slot />

          <button
            v-if="gorunur.sil"
            class="eylem-item eylem-sil"
            @click="calistir('sil')"
          >
            <i class="pi pi-trash" /> {{ $t('common.delete') }}
          </button>
        </div>
      </transition>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted, onUnmounted } from 'vue'

defineProps({
  gorunur: { type: Object, default: () => ({ duzenle: true, cogalt: false, sil: true }) },
  /** Ek aksiyonlar: [{ etiket, ikon, islem, sinif? }] */
  items: { type: Array, default: () => [] }
})

const emit = defineEmits(['duzenle', 'cogalt', 'sil', 'eylem'])

const btnRef = ref(null)
const rootEl = ref(null)
const menuRef = ref(null)
const menuStil = ref({})
const acik = ref(false)

const odaklanIlk = () => {
  menuRef.value?.querySelector?.('.eylem-item')?.focus?.()
}

const menuKeydown = (e) => {
  const ogeler = [...(menuRef.value?.querySelectorAll('.eylem-item') || [])]
  if (!ogeler.length) return
  const aktifIdx = ogeler.indexOf(document.activeElement)
  if (e.key === 'ArrowDown') {
    e.preventDefault()
    ogeler[(aktifIdx + 1 + ogeler.length) % ogeler.length].focus()
  } else if (e.key === 'ArrowUp') {
    e.preventDefault()
    ogeler[(aktifIdx - 1 + ogeler.length) % ogeler.length].focus()
  } else if (e.key === 'Home') {
    e.preventDefault()
    ogeler[0].focus()
  } else if (e.key === 'End') {
    e.preventDefault()
    ogeler[ogeler.length - 1].focus()
  } else if (e.key === 'Escape') {
    e.preventDefault()
    acik.value = false
    ;(btnRef.value?.$el || btnRef.value)?.focus?.()
  }
}

const acToggle = async (e) => {
  acik.value = !acik.value
  if (!acik.value) return
  await nextTick()
  // Tıklanan butonu esas al (ref bazı durumlarda 0 rect döndürebilir)
  const el = e?.currentTarget || btnRef.value?.$el || btnRef.value

  const hesapla = () => {
    const r = el?.getBoundingClientRect?.()
    // Eleman henüz yerleşmediyse (0 rect) hesaplamayı ertele
    if (!r || (!r.width && !r.height && !r.top && !r.left && !r.right)) return false
    const mw = menuRef.value?.offsetWidth || 176
    const mh = menuRef.value?.offsetHeight || 160
    const bosluk = 8
    // Sağ kenara hizala; taşarsa sola kaydır, ekran dışına çıkarma
    let left = r.right - mw
    if (left < bosluk) left = r.left
    if (left + mw > window.innerWidth - bosluk) left = window.innerWidth - mw - bosluk
    left = Math.max(bosluk, left)
    const alttaYerVar = r.bottom + mh + bosluk <= window.innerHeight
    const top = alttaYerVar ? r.bottom + 4 : Math.max(bosluk, r.top - mh - 4)
    menuStil.value = {
      position: 'fixed',
      left: `${left}px`,
      top: `${top}px`,
      maxHeight: `${Math.min(320, window.innerHeight - 2 * bosluk)}px`,
      maxWidth: `calc(100vw - ${2 * bosluk}px)`
    }
    return true
  }

  if (!hesapla()) {
    await new Promise((resolve) => requestAnimationFrame(() => resolve()))
    hesapla()
  }
  await nextTick()
  odaklanIlk()
}

const calistir = (eylem) => {
  acik.value = false
  emit(eylem)
}

const calistirItem = (it) => {
  acik.value = false
  if (typeof it.islem === 'function') it.islem()
  emit('eylem', it.etiket)
}

const disariTikla = (e) => {
  // Menu body'ye teleport edildigi icin tiklamanin bu ornege ait olup olmadigi
  // rootEl uzerinden kontrol edilir; boylece baska satirin menusu acilirsa kapanir.
  if (acik.value && !rootEl.value?.contains(e.target)) acik.value = false
}

// Sabit konumlu menu scroll/resize'da kaymasin: kapat (menu kendi icinde
// kaydirilirken kapatma)
const kaydirincaKapat = (e) => {
  if (e && e.target && menuRef.value?.contains(e.target)) return
  if (acik.value) acik.value = false
}

onMounted(() => {
  document.addEventListener('click', disariTikla)
  window.addEventListener('scroll', kaydirincaKapat, true)
  window.addEventListener('resize', kaydirincaKapat)
})
onUnmounted(() => {
  document.removeEventListener('click', disariTikla)
  window.removeEventListener('scroll', kaydirincaKapat, true)
  window.removeEventListener('resize', kaydirincaKapat)
})
</script>

<style scoped>
.satir-eylemler {
  position: relative;
  display: inline-flex;
}
.eylem-menu {
  z-index: 1200;
  min-width: 170px;
  max-height: min(320px, 70vh);
  overflow-y: auto;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 10px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.3);
  padding: 4px;
}
.eylem-item {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 8px 12px;
  border: none;
  background: transparent;
  color: var(--text-primary);
  font-size: 13px;
  border-radius: 7px;
  cursor: pointer;
  text-align: left;
  white-space: nowrap;
}
.eylem-item:hover {
  background: rgba(148, 163, 184, 0.1);
}
.eylem-item i {
  font-size: 13px;
  color: var(--text-muted);
  width: 14px;
}
.eylem-item.eylem-sil {
  color: #ef4444;
}
.eylem-item.eylem-sil i {
  color: #ef4444;
}
.fade-enter-active,
.fade-leave-active {
  transition:
    opacity 0.15s,
    transform 0.15s;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}
</style>
