<template>
  <div class="satir-eylemler">
    <Button
      icon="pi pi-ellipsis-v"
      class="p-button-rounded p-button-text"
      title="İşlemler"
      aria-haspopup="true"
      @click="acik = !acik"
    />
    <transition name="fade">
      <div
        v-if="acik"
        class="eylem-menu"
        @click.stop
      >
        <button
          v-if="gorunur.duzenle"
          class="eylem-item"
          @click="calistir('duzenle')"
        >
          <i class="pi pi-pencil" /> Düzenle
        </button>
        <button
          v-if="gorunur.cogalt"
          class="eylem-item"
          @click="calistir('cogalt')"
        >
          <i class="pi pi-copy" /> Çoğalt
        </button>
        <slot />
        <button
          v-if="gorunur.sil"
          class="eylem-item eylem-sil"
          @click="calistir('sil')"
        >
          <i class="pi pi-trash" /> Sil
        </button>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

defineProps({
  gorunur: { type: Object, default: () => ({ duzenle: true, cogalt: false, sil: true }) }
})

const emit = defineEmits(['duzenle', 'cogalt', 'sil'])

const acik = ref(false)

const calistir = (eylem) => {
  acik.value = false
  emit(eylem)
}

const disariTikla = (e) => {
  if (acik.value && !e.target.closest('.satir-eylemler')) acik.value = false
}

onMounted(() => document.addEventListener('click', disariTikla))
onUnmounted(() => document.removeEventListener('click', disariTikla))
</script>

<style scoped>
.satir-eylemler {
  position: relative;
}
.eylem-menu {
  position: absolute;
  right: 0;
  top: 36px;
  z-index: 50;
  min-width: 140px;
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
}
.eylem-item:hover {
  background: rgba(148, 163, 184, 0.1);
}
.eylem-item i {
  font-size: 13px;
  color: var(--text-muted);
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
