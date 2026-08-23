<template>
  <div>
    <!-- Global Command Palette Modal -->
    <Dialog
      v-model:visible="visible"
      :modal="true"
      :closable="false"
      :style="{ width: '90%', maxWidth: '580px' }"
      class="command-palette-dialog"
    >
      <template #header>
        <div class="w-full flex items-center gap-2">
          <i class="pi pi-search text-gray-400 text-lg" />
          <input
            ref="searchInput"
            v-model="query"
            type="text"
            placeholder="Sayfa, modül veya işlem ara... (Örn: Fatura, Stok, Rapor, POS)"
            class="w-full bg-transparent border-none outline-none text-base font-medium text-gray-800 dark:text-gray-100 placeholder-gray-400"
            @keydown.down.prevent="navigateResults(1)"
            @keydown.up.prevent="navigateResults(-1)"
            @keydown.enter.prevent="selectResult"
            @keydown.esc="visible = false"
          >
          <kbd class="text-xs bg-gray-100 dark:bg-gray-700 px-2 py-1 rounded text-gray-500 font-mono">ESC</kbd>
        </div>
      </template>

      <div class="palette-results max-h-80 overflow-y-auto py-2 space-y-1">
        <div
          v-for="(item, idx) in filteredItems"
          :key="item.path || idx"
          :class="['result-item flex items-center justify-between p-2.5 rounded-lg cursor-pointer transition-colors', idx === selectedIndex ? 'bg-primary text-white' : 'hover:bg-gray-50 dark:hover:bg-gray-800 text-gray-700 dark:text-gray-200']"
          @click="executeItem(item)"
        >
          <div class="flex items-center gap-3">
            <i :class="[item.icon, 'text-base', idx === selectedIndex ? 'text-white' : 'text-primary']" />
            <div>
              <div class="font-semibold text-sm">
                {{ item.title }}
              </div>
              <div :class="['text-xs', idx === selectedIndex ? 'text-blue-100' : 'text-gray-400']">
                {{ item.group }}
              </div>
            </div>
          </div>
          <span :class="['text-xs font-mono px-1.5 py-0.5 rounded', idx === selectedIndex ? 'bg-white/20 text-white' : 'bg-gray-100 dark:bg-gray-700 text-gray-500']">
            {{ item.shortcut || '↵ Git' }}
          </span>
        </div>

        <div
          v-if="filteredItems.length === 0"
          class="text-center py-8 text-gray-400 text-sm"
        >
          <i class="pi pi-search text-2xl mb-2 block opacity-50" />
          "{{ query }}" için sonuç bulunamadı.
        </div>
      </div>

      <template #footer>
        <div class="flex justify-between items-center text-xs text-gray-400 w-full pt-2">
          <span><kbd class="font-mono">↑↓</kbd> Gezin · <kbd class="font-mono">↵</kbd> Seç</span>
          <span><kbd class="font-mono">Ctrl+K</kbd> ile her zaman açabilirsiniz</span>
        </div>
      </template>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const visible = ref(false)
const query = ref('')
const selectedIndex = ref(0)
const searchInput = ref(null)

const menuItems = [
  { title: 'Dashboard / Genel Bakış', group: 'Genel', path: '/dashboard', icon: 'pi pi-home' },
  { title: 'Yönetici Kokpiti', group: 'Yönetim', path: '/yonetici-kokpiti', icon: 'pi pi-bolt' },
  { title: 'Saha Portalı', group: 'Operasyon', path: '/saha-portali', icon: 'pi pi-compass' },
  { title: 'Hızlı Satış (POS)', group: 'Satış', path: '/hizli-satis', icon: 'pi pi-desktop', shortcut: 'F2' },
  { title: 'Faturalar', group: 'Ticaret', path: '/faturalar', icon: 'pi pi-file' },
  { title: 'Siparişler', group: 'Ticaret', path: '/siparisler', icon: 'pi pi-shopping-bag' },
  { title: 'Teklifler', group: 'Ticaret', path: '/teklifler', icon: 'pi pi-tag' },
  { title: 'İrsaliyeler', group: 'Ticaret', path: '/irsaliyeler', icon: 'pi pi-truck' },
  { title: 'Stok Listesi', group: 'Envanter', path: '/stoklar', icon: 'pi pi-box', shortcut: 'F4' },
  { title: 'Stok Sayım', group: 'Envanter', path: '/stok-sayim', icon: 'pi pi-check-square' },
  { title: 'Depo Transfer', group: 'Envanter', path: '/depolar', icon: 'pi pi-building' },
  { title: 'Cari Hesaplar', group: 'Finans', path: '/cari-hesaplar', icon: 'pi pi-users' },
  { title: 'Kasa & Banka', group: 'Finans', path: '/kasalar', icon: 'pi pi-wallet' },
  { title: 'Masraflar & Avans', group: 'Finans', path: '/masraflar', icon: 'pi pi-receipt' },
  { title: 'Çek / Senet', group: 'Finans', path: '/cek-senet', icon: 'pi pi-credit-card' },
  { title: 'Personeller & İK', group: 'İnsan Kaynakları', path: '/personeller', icon: 'pi pi-id-card' },
  { title: 'İzin Talepleri', group: 'İnsan Kaynakları', path: '/izinler', icon: 'pi pi-calendar' },
  { title: 'Raporlar & Analiz', group: 'Raporlama', path: '/raporlar', icon: 'pi pi-chart-line' },
  { title: 'Şirketler & Grup', group: 'Sistem', path: '/sirketler', icon: 'pi pi-sitemap' },
  { title: 'Yeni Yıl Devir Sihirbazı', group: 'Sistem', path: '/yeni-yil-sihirbazi', icon: 'pi pi-sync' },
  { title: 'Ayarlar & Profil', group: 'Kullanıcı', path: '/hesap-ayarlari', icon: 'pi pi-cog' }
]

const filteredItems = computed(() => {
  if (!query.value.trim()) return menuItems
  const q = query.value.toLowerCase()
  return menuItems.filter(item =>
    item.title.toLowerCase().includes(q) || item.group.toLowerCase().includes(q)
  )
})

const navigateResults = (step) => {
  const total = filteredItems.value.length
  if (total === 0) return
  selectedIndex.value = (selectedIndex.value + step + total) % total
}

const selectResult = () => {
  const item = filteredItems.value[selectedIndex.value]
  if (item) executeItem(item)
}

const executeItem = (item) => {
  visible.value = false
  if (item.path) {
    router.push(item.path)
  }
}

const handleKeyDown = (e) => {
  // Ctrl + K or Cmd + K
  if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') {
    e.preventDefault()
    openPalette()
  } else if (e.key === 'F2') {
    e.preventDefault()
    router.push('/hizli-satis')
  } else if (e.key === 'F4') {
    e.preventDefault()
    router.push('/stoklar')
  }
}

const openPalette = () => {
  visible.value = true
  query.value = ''
  selectedIndex.value = 0
  nextTick(() => {
    if (searchInput.value) searchInput.value.focus()
  })
}

onMounted(() => {
  window.addEventListener('keydown', handleKeyDown)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeyDown)
})

defineExpose({ openPalette })
</script>

<style scoped>
.command-palette-dialog :deep(.p-dialog-content) {
  padding: 0.5rem 1rem;
}
.command-palette-dialog :deep(.p-dialog-header) {
  border-bottom: 1px solid var(--border);
  padding: 1rem 1.25rem;
}
.command-palette-dialog :deep(.p-dialog-footer) {
  border-top: 1px solid var(--border);
  padding: 0.75rem 1.25rem;
}
</style>
