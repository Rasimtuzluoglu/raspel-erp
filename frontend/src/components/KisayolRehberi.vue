<template>
  <transition name="dialog-fade">
    <div
      v-if="goster"
      class="rehber-overlay"
      @click.self="kapat"
    >
      <div class="rehber-kutu">
        <div class="rehber-baslik">
          <strong>{{ $t('shortcuts.title') }}</strong>
          <Button
            icon="pi pi-times"
            class="p-button-rounded p-button-text"
            @click="kapat"
          />
        </div>
        <div class="rehber-icerik">
          <div
            v-for="k in kisayollar"
            :key="k.kod"
            class="rehber-satir"
          >
            <span class="kisa-tuslar">
              <kbd
                v-for="t in k.tuslar"
                :key="t"
              >{{ t }}</kbd>
            </span>
            <span class="kisa-aciklama">{{ $t(k.aciklamaAnahtar) }}</span>
          </div>
        </div>
        <div class="rehber-alt">
          <small>{{ $t('shortcuts.closeHint') }}</small>
        </div>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { onMounted, onUnmounted } from 'vue'

const props = defineProps({ goster: { type: Boolean, default: false } })
const emit = defineEmits(['update:goster'])

const kapat = () => emit('update:goster', false)

const kisayollar = [
  { kod: 'ara', tuslar: ['Ctrl', 'K'], aciklamaAnahtar: 'shortcuts.search' },
  { kod: 'kaydet', tuslar: ['Ctrl', 'S'], aciklamaAnahtar: 'shortcuts.save' },
  { kod: 'yazdir', tuslar: ['Ctrl', 'P'], aciklamaAnahtar: 'shortcuts.print' },
  { kod: 'yeni', tuslar: ['F2'], aciklamaAnahtar: 'shortcuts.new' },
  { kod: 'iptal', tuslar: ['Esc'], aciklamaAnahtar: 'shortcuts.close' },
  { kod: 'rehber', tuslar: ['?'], aciklamaAnahtar: 'shortcuts.guide' }
]

const tusHandler = (e) => {
  if (e.key === 'Escape' && props.goster) {
    kapat()
  }
  if (e.key === '?' && !e.ctrlKey && !e.metaKey && !e.altKey) {
    const aktif = document.activeElement
    const girdi = aktif && (aktif.tagName === 'INPUT' || aktif.tagName === 'TEXTAREA' || aktif.isContentEditable)
    if (!girdi) {
      e.preventDefault()
      emit('update:goster', !props.goster)
    }
  }
}

onMounted(() => window.addEventListener('keydown', tusHandler))
onUnmounted(() => window.removeEventListener('keydown', tusHandler))
</script>

<style scoped>
.rehber-overlay {
  position: fixed; inset: 0; z-index: 100000;
  background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center;
}
.rehber-kutu {
  width: 440px; max-width: 92vw; background: var(--bg-card);
  border: 1px solid var(--border); border-radius: 14px; box-shadow: 0 20px 60px rgba(0,0,0,0.5);
}
.rehber-baslik {
  display: flex; justify-content: space-between; align-items: center;
  padding: 14px 18px; border-bottom: 1px solid var(--border); font-size: 15px;
}
.rehber-icerik { padding: 12px 18px; }
.rehber-satir {
  display: flex; align-items: center; gap: 14px; padding: 9px 0;
  border-bottom: 1px solid rgba(148,163,184,0.08);
}
.rehber-satir:last-child { border-bottom: none; }
.kisa-tuslar { display: flex; gap: 4px; min-width: 90px; }
kbd {
  background: rgba(148,163,184,0.15); border: 1px solid rgba(148,163,184,0.25);
  border-bottom-width: 2px; border-radius: 5px; padding: 2px 7px;
  font-size: 12px; font-family: inherit; color: var(--text-primary);
}
.kisa-aciklama { font-size: 13px; color: var(--text-secondary); }
.rehber-alt { padding: 10px 18px; border-top: 1px solid var(--border); color: var(--text-muted); }
.dialog-fade-enter-active, .dialog-fade-leave-active { transition: opacity 0.2s; }
.dialog-fade-enter-from, .dialog-fade-leave-to { opacity: 0; }
</style>
