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
      @click.stop="acToggle($event)"
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
          <div
            v-if="baslik"
            class="eylem-baslik"
            :title="baslik"
          >
            {{ baslik }}
          </div>
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

          <template
            v-for="(it, i) in items"
            :key="i"
          >
            <div
              v-if="it.grup"
              class="eylem-grup"
            >
              {{ it.grup }}
            </div>
            <div
              v-else-if="it.ayrac"
              class="eylem-ayrac"
            />
            <button
              v-else
              class="eylem-item"
              :class="it.sinif"
              :title="it.aciklama || it.etiket"
              @click="calistirItem(it)"
            >
              <i :class="it.ikon" />
              <span class="eylem-metin">
                <span class="eylem-etiket">{{ it.etiket }}</span>
                <span
                  v-if="it.aciklama"
                  class="eylem-aciklama"
                >{{ it.aciklama }}</span>
              </span>
            </button>
          </template>

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

<script>
// Modul duzeyi (setup DISINDA): tum SatirEylemleri ornekleri ayni kapatıcıyı
// paylasir. `<script setup>` icindeki degiskenler ornek-basina olurdu; bu
// yuzden burada tanimlanir. Yeni menu acilirken onceki kapatilir.
let aktifKapat = null
</script>

<script setup>
import { ref, nextTick, onMounted, onUnmounted } from 'vue'

const props = defineProps({
  gorunur: { type: Object, default: () => ({ duzenle: true, cogalt: false, sil: true }) },
  /** Ek aksiyonlar: [{ etiket, ikon, islem, sinif?, aciklama?, grup?, ayrac? }] */
  items: { type: Array, default: () => [] },
  /** Menu ustunde gosterilen kayit adi basligi. */
  baslik: { type: String, default: '' },
  /** true ise menu satira degil, liste alaninin sag ustune sabit konumlanir. */
  sabitKonum: { type: Boolean, default: false }
})

const emit = defineEmits(['duzenle', 'cogalt', 'sil', 'eylem'])

const btnRef = ref(null)
const rootEl = ref(null)
const menuRef = ref(null)
const menuStil = ref({ position: 'fixed', left: '-9999px', top: '0', visibility: 'hidden' })
const acik = ref(false)

const kapat = () => {
  acik.value = false
  if (aktifKapat === kapat) aktifKapat = null
}

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
    kapat()
    ;(btnRef.value?.$el || btnRef.value)?.focus?.()
  }
}

const acToggle = async (e) => {
  if (acik.value) {
    kapat()
    return
  }
  // Baska bir satirin menusu aciksa once onu kapat.
  if (aktifKapat && aktifKapat !== kapat) aktifKapat()
  aktifKapat = kapat
  acik.value = true
  // Menu olculene kadar ekran disinda ve gizli tut (body'ye teleport edildigi
  // icin varsayilan konum sol ust olurdu - "en solda aciliyor" bug'i).
  menuStil.value = { position: 'fixed', left: '-9999px', top: '0', visibility: 'hidden' }
  await nextTick()

  // Tiklanan elemani esas al; bilesen cagrilarinda currentTarget null olabilir.
  const el = e?.currentTarget || e?.target || btnRef.value?.$el || btnRef.value
  const bosluk = 8

  const hesapla = () => {
    //Sabit konum: menu, satira degil listeye (tablo konteyneri) gore sag ustte acilir.
    if (props.sabitKonum) {
      const konteyner = rootEl.value?.closest?.('.table-container, .app-datatable-wrapper')
      const kr = konteyner?.getBoundingClientRect?.()
      if (kr && (kr.width || kr.height)) {
        const mw = menuRef.value?.offsetWidth || 220
        let left = kr.right - mw - 8
        left = Math.max(8, Math.min(left, window.innerWidth - mw - 8))
        const top = Math.max(8, kr.top + 8)
        menuStil.value = {
          position: 'fixed',
          left: `${left}px`,
          top: `${top}px`,
          visibility: 'visible',
          maxHeight: `${Math.min(360, window.innerHeight - 2 * bosluk)}px`,
          maxWidth: `calc(100vw - ${2 * bosluk}px)`
        }
        return true
      }
    }
    const r = el?.getBoundingClientRect?.()
    // Eleman henuz yerlesmediyse (0 rect) hesaplamayi ertele
    if (!r || (!r.width && !r.height)) return false
    const mw = menuRef.value?.offsetWidth || 176
    const mh = menuRef.value?.offsetHeight || 160
    // Sag kenara hizala; tasarsa sola kaydir, ekran disina cikarma
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
      visibility: 'visible',
      maxHeight: `${Math.min(320, window.innerHeight - 2 * bosluk)}px`,
      maxWidth: `calc(100vw - ${2 * bosluk}px)`
    }
    return true
  }

  for (let deneme = 0; deneme < 3 && !hesapla(); deneme++) {
    await new Promise((resolve) => requestAnimationFrame(() => resolve()))
  }
  await nextTick()
  odaklanIlk()
}

const calistir = (eylem) => {
  kapat()
  emit(eylem)
}

const calistirItem = (it) => {
  kapat()
  if (typeof it.islem === 'function') it.islem()
  emit('eylem', it.etiket)
}

const disariTikla = (e) => {
  // Capture fazinda calisir; boylece buton `@click.stop` ile bubbles'i
  // durdursa bile baska satirin ⋮'ine tiklandiginda ONCEKI menu kapanir.
  // Kendi tetik butonu (rootEl) ve acik menu (menuRef, body'ye teleport) haric.
  if (!acik.value) return
  if (rootEl.value?.contains(e.target)) return
  if (menuRef.value?.contains(e.target)) return
  kapat()
}

// Sabit konumlu menu scroll/resize'da kaymasin: kapat (menu kendi icinde
// kaydirilirken kapatma)
const kaydirincaKapat = (e) => {
  if (e && e.target && menuRef.value?.contains(e.target)) return
  if (acik.value) kapat()
}

onMounted(() => {
  document.addEventListener('click', disariTikla, true)
  window.addEventListener('scroll', kaydirincaKapat, true)
  window.addEventListener('resize', kaydirincaKapat)
})
onUnmounted(() => {
  document.removeEventListener('click', disariTikla, true)
  window.removeEventListener('scroll', kaydirincaKapat, true)
  window.removeEventListener('resize', kaydirincaKapat)
  if (aktifKapat === kapat) aktifKapat = null
})
</script>

<style scoped>
.satir-eylemler {
  position: relative;
  display: inline-flex;
  /* REDTEAM/Faz5: Satir eylem dugmesi flex satirlarda (POS sepet satiri) yer
     aliyor. `flex-shrink` verilmedigi icin dar panelde ezilip dugme metni
     kuculuyor/ekranda ust uste biniyordu. */
  flex-shrink: 0;
}
.eylem-menu {
  /* z-index (1250) < modal (1300): bir Dialog icinde acildiginda maskinin
     altinda kalir; dropdown/overlay'larin (1200) uzerinde kalmaya devam eder. */
  z-index: var(--z-menu);
  min-width: 220px;
  max-height: min(360px, 70vh);
  overflow-y: auto;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg, 14px);
  box-shadow: var(--elev-2, 0 12px 32px rgba(0, 0, 0, 0.35));
  padding: 6px;
  scrollbar-width: thin;
}
.eylem-menu::-webkit-scrollbar {
  width: 8px;
}
.eylem-menu::-webkit-scrollbar-thumb {
  background: rgba(148, 163, 184, 0.25);
  border-radius: 999px;
  border: 2px solid transparent;
  background-clip: content-box;
}
.eylem-baslik {
  padding: 6px 10px 8px;
  font-size: 13px;
  font-weight: 700;
  color: var(--text-primary);
  border-bottom: 1px solid var(--border);
  margin-bottom: 6px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.eylem-grup {
  padding: 8px 10px 4px;
  font-size: 10.5px;
  font-weight: 700;
  letter-spacing: 0.6px;
  text-transform: uppercase;
  color: var(--text-muted);
}
.eylem-ayrac {
  height: 1px;
  background: var(--border);
  margin: 5px 6px;
}
.eylem-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  min-height: 40px;
  padding: 8px 10px;
  border: none;
  background: transparent;
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 500;
  border-radius: var(--radius-md, 10px);
  cursor: pointer;
  text-align: left;
  white-space: nowrap;
  transition:
    background var(--dur-fast, 0.15s) var(--ease-standard, ease),
    color var(--dur-fast, 0.15s) var(--ease-standard, ease);
}
.eylem-item:hover {
  background: var(--accent-soft);
  color: var(--accent);
}
.eylem-item:active {
  transform: translateY(1px);
}
.eylem-item:focus-visible {
  outline: none;
  box-shadow: 0 0 0 3px var(--accent-soft-strong);
}
/* Ikon rozeti: yuvarlak-kare tintli zemin. */
.eylem-item > i {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  flex-shrink: 0;
  border-radius: 8px;
  background: var(--bg-muted);
  color: var(--text-secondary);
  font-size: 14px;
  transition:
    background var(--dur-fast, 0.15s) var(--ease-standard, ease),
    color var(--dur-fast, 0.15s) var(--ease-standard, ease);
}
.eylem-item:hover > i {
  background: var(--accent-soft-strong);
  color: var(--accent);
}
.eylem-metin {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}
.eylem-aciklama {
  font-size: 11.5px;
  font-weight: 400;
  color: var(--text-muted);
  white-space: normal;
  line-height: 1.25;
  transition: color var(--dur-fast, 0.15s) var(--ease-standard, ease);
}
.eylem-item:hover .eylem-aciklama {
  color: var(--accent);
  opacity: 0.9;
}
/* Renk kodlama: finansal aksiyonlar bir bakista ayirt edilir. */
.eylem-item.eylem-pozitif > i {
  background: var(--success-soft);
  color: var(--success, #22c55e);
}
.eylem-item.eylem-pozitif:hover {
  background: var(--success-soft);
  color: var(--success, #22c55e);
}
.eylem-item.eylem-pozitif:hover > i {
  background: var(--success-soft);
  color: var(--success, #22c55e);
}
.eylem-item.eylem-uyari > i {
  background: var(--warning-soft);
  color: var(--warning, #f59e0b);
}
.eylem-item.eylem-uyari:hover {
  background: var(--warning-soft);
  color: var(--warning, #f59e0b);
}
.eylem-item.eylem-uyari:hover > i {
  background: var(--warning-soft);
  color: var(--warning, #f59e0b);
}
/* Yikici aksiyon: ayrac + kirmizi. */
.eylem-item.eylem-sil {
  color: var(--danger, #ef4444);
  margin-top: 6px;
  border-top: 1px solid var(--border);
  border-radius: 0 0 var(--radius-md, 10px) var(--radius-md, 10px);
}
.eylem-item.eylem-sil > i {
  background: var(--danger-soft);
  color: var(--danger, #ef4444);
}
.eylem-item.eylem-sil:hover {
  background: var(--danger-soft);
  color: var(--danger, #ef4444);
}
.eylem-item.eylem-sil:hover > i {
  background: var(--danger-soft);
  color: var(--danger, #ef4444);
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
  transform: translateY(-4px) scale(0.98);
}
@media (prefers-reduced-motion: reduce) {
  .fade-enter-active,
  .fade-leave-active {
    transition: none;
  }
}
</style>
