<template>
  <div v-if="visible" class="qs-overlay" @click.self="kapat">
    <div class="qs-modal">
      <div class="qs-input-wrapper">
        <i class="pi pi-search qs-icon"></i>
        <input
          ref="inputRef"
          v-model="query"
          type="text"
          class="qs-input"
          placeholder="Ara (cari, stok, fatura, personel, proje...)"
          @keydown="handleKeydown"
          autofocus
        />
        <kbd class="qs-esc">ESC</kbd>
      </div>
      <div v-if="!query" class="qs-hint">
        <p>Bir şey yazmaya başlayın...</p>
        <div class="qs-hint-items">
          <span><kbd>c</kbd> Cari Hesaplar</span>
          <span><kbd>s</kbd> Stoklar</span>
          <span><kbd>f</kbd> Faturalar</span>
          <span><kbd>p</kbd> Personel</span>
          <span><kbd>r</kbd> Projeler</span>
        </div>
      </div>
      <div v-if="loading" class="qs-loading"><i class="pi pi-spin pi-spinner"></i> Aranıyor...</div>
      <div v-if="error" class="qs-error">{{ error }}</div>
      <div v-if="results.length > 0 && !loading" class="qs-results">
        <div
          v-for="(item, idx) in results"
          :key="`${item.type}-${item.id}`"
          :class="['qs-item', { active: idx === selectedIndex }]"
          @click="navigate(item)"
          @mouseenter="selectedIndex = idx"
        >
          <i :class="item.icon" class="qs-item-icon"></i>
          <div class="qs-item-info">
            <span class="qs-item-title">{{ item.title }}</span>
            <span class="qs-item-sub">{{ item.subtitle }}</span>
          </div>
          <Tag :value="item.type" :severity="item.severity" />
        </div>
      </div>
      <div v-if="query && results.length === 0 && !loading" class="qs-empty">
        <i class="pi pi-search"></i>
        <p>"{{ query }}" için sonuç bulunamadı</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, nextTick, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { cariHesapAPI, stokAPI, faturaAPI, personelAPI, projeAPI, siparisAPI } from '../api/index.js'

const props = defineProps({ visible: Boolean })
const emit = defineEmits(['update:visible'])
const router = useRouter()
const query = ref('')
const results = ref([])
const selectedIndex = ref(0)
const loading = ref(false)
const error = ref(null)
const inputRef = ref(null)

const typeConfig = {
  cari: { icon: 'pi pi-users', severity: 'info', route: '/cari-hesaplar' },
  stok: { icon: 'pi pi-box', severity: 'success', route: '/stoklar' },
  fatura: { icon: 'pi pi-file', severity: 'warn', route: '/faturalar' },
  personel: { icon: 'pi pi-id-card', severity: 'help', route: '/personel' },
  proje: { icon: 'pi pi-folder', severity: 'contrast', route: '/projeler' },
  siparis: { icon: 'pi pi-receipt', severity: 'info', route: '/siparisler' }
}

watch(() => props.visible, (v) => {
  if (v) { ac() } else { query.value = ''; results.value = [] }
})

const ac = () => { query.value = ''; results.value = []; selectedIndex.value = 0; nextTick(() => inputRef.value?.focus()) }
const kapat = () => { emit('update:visible', false); query.value = ''; results.value = [] }

const handleKeydown = (e) => {
  if (e.key === 'Escape') return kapat()
  if (e.key === 'ArrowDown') { e.preventDefault(); selectedIndex.value = Math.min(selectedIndex.value + 1, results.value.length - 1) }
  if (e.key === 'ArrowUp') { e.preventDefault(); selectedIndex.value = Math.max(selectedIndex.value - 1, 0) }
  if (e.key === 'Enter' && results.value[selectedIndex.value]) navigate(results.value[selectedIndex.value])
}

const navigate = (item) => {
  kapat()
  if (item.type === 'cari' || item.type === 'fatura' || item.type === 'proje' || item.type === 'siparis') {
    router.push(`${typeConfig[item.type].route}/${item.id}`)
  } else {
    router.push(typeConfig[item.type].route)
  }
}

let searchTimer = null

watch(query, (val) => {
  if (searchTimer) clearTimeout(searchTimer)
  if (!val || val.length < 1) { results.value = []; return }
  searchTimer = setTimeout(async () => {
    loading.value = true; error.value = null; selectedIndex.value = 0
    const q = val.trim().toLowerCase()
    const firstChar = q[0]
    const allCategories = !['c', 's', 'f', 'p', 'r'].includes(firstChar)
    try {
      const promises = []
      if (firstChar === 'c' || allCategories) {
        promises.push(cariHesapAPI.search(q).then(r => r.data.map(d => ({ ...d, type: 'cari', ...typeConfig.cari, title: d.ad, subtitle: `Vergi: ${d.vergiNumarasi || '-'} | Bakiye: ${formatCur(d.bakiye)}` }))).catch(() => []))
      }
      if (firstChar === 's' || allCategories) {
        promises.push(stokAPI.ara(q).then(r => r.data.map(d => ({ ...d, type: 'stok', ...typeConfig.stok, title: d.ad, subtitle: `Kod: ${d.stokKodu} | Miktar: ${d.miktar} ${d.birim}` }))).catch(() => []))
      }
      if (firstChar === 'f' || allCategories) {
        promises.push(faturaAPI.getAll().then(r => r.data.filter(f => f.faturaNumarasi?.toLowerCase().includes(q) || f.cariHesapAd?.toLowerCase().includes(q)).map(d => ({ ...d, type: 'fatura', ...typeConfig.fatura, title: `#${d.faturaNumarasi || d.id}`, subtitle: `${d.cariHesapAd} | ${formatCur(d.genelToplam)}` }))).catch(() => []))
      }
      if (firstChar === 'p' || allCategories) {
        promises.push(personelAPI.search(q).then(r => r.data.map(d => ({ ...d, type: 'personel', ...typeConfig.personel, title: d.ad, subtitle: `${d.pozisyon || ''}` }))).catch(() => []))
      }
      if (firstChar === 'r' || allCategories) {
        promises.push(projeAPI.search(q).then(r => r.data.map(d => ({ ...d, type: 'proje', ...typeConfig.proje, title: d.ad, subtitle: `${d.durum || ''}` }))).catch(() => []))
      }
      const all = await Promise.all(promises)
      results.value = all.flat().slice(0, 12)
    } catch (e) { error.value = 'Arama sırasında hata oluştu' }
    finally { loading.value = false }
  }, 300)
})

const formatCur = (v) => { if (v == null) return '0,00 ₺'; return new Intl.NumberFormat('tr-TR', { style: 'currency', currency: 'TRY' }).format(v) }

const handler = (e) => { if ((e.ctrlKey || e.metaKey) && e.key === 'k') { e.preventDefault(); props.visible ? kapat() : ac(); emit('update:visible', !props.visible) } }
onMounted(() => document.addEventListener('keydown', handler))
onUnmounted(() => {
  document.removeEventListener('keydown', handler)
  if (searchTimer) clearTimeout(searchTimer)
})
</script>

<style scoped>
.qs-overlay {
  position: fixed; inset: 0; z-index: 9999;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(4px);
  display: flex; align-items: flex-start; justify-content: center;
  padding-top: 12vh;
}
.qs-modal {
  width: 580px; max-width: 90vw;
  background: var(--bg-card, #1e293b);
  border: 1px solid rgba(148, 163, 184, 0.15);
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.5);
  overflow: hidden;
}
.qs-input-wrapper { display: flex; align-items: center; gap: 10px; padding: 16px 20px; border-bottom: 1px solid rgba(148,163,184,0.1); }
.qs-icon { font-size: 18px; color: #64748b; flex-shrink: 0; }
.qs-input { flex: 1; border: none; outline: none; background: transparent; color: var(--text-primary, #f1f5f9); font-size: 16px; font-family: inherit; }
.qs-input::placeholder { color: #475569; }
.qs-esc { background: rgba(148,163,184,0.1); color: #64748b; padding: 2px 8px; border-radius: 4px; font-size: 11px; font-family: inherit; flex-shrink: 0; }
.qs-hint { padding: 24px 20px; text-align: center; color: #64748b; font-size: 13px; }
.qs-hint p { margin: 0 0 12px; }
.qs-hint-items { display: flex; flex-wrap: wrap; gap: 8px; justify-content: center; }
.qs-hint-items span { font-size: 12px; color: #94a3b8; }
.qs-hint-items kbd { background: rgba(148,163,184,0.1); padding: 2px 6px; border-radius: 4px; font-size: 11px; color: #cbd5e1; font-family: inherit; border: 1px solid rgba(148,163,184,0.15); }
.qs-loading, .qs-empty, .qs-error { padding: 24px; text-align: center; color: #64748b; font-size: 14px; }
.qs-empty i { font-size: 32px; display: block; margin-bottom: 8px; color: #475569; }
.qs-results { max-height: 360px; overflow-y: auto; padding: 8px; }
.qs-item { display: flex; align-items: center; gap: 12px; padding: 10px 12px; border-radius: 10px; cursor: pointer; transition: all 0.1s; }
.qs-item.active, .qs-item:hover { background: rgba(59,130,246,0.12); }
.qs-item-icon { font-size: 16px; color: #64748b; width: 24px; text-align: center; flex-shrink: 0; }
.qs-item-info { flex: 1; min-width: 0; }
.qs-item-title { display: block; font-size: 14px; color: var(--text-primary, #f1f5f9); font-weight: 500; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.qs-item-sub { display: block; font-size: 12px; color: #64748b; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
</style>
