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
        <div v-if="sonAramalar.length" class="qs-son-aramalar">
          <div class="qs-son-baslik">Son Aramalar</div>
          <div v-for="(a, i) in sonAramalar" :key="i" class="qs-son-item" @click="query = a">
            <i class="pi pi-history"></i>{{ a }}
            <i class="pi pi-times qs-son-sil" @click.stop="sonAramaSil(i)"></i>
          </div>
        </div>
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
import { cariHesapAPI, stokAPI, faturaAPI, personelAPI, projeAPI, siparisAPI, notAPI, bankaAPI, kasaAPI } from '../api/index.js'
import { safeGet, safeSet } from '../utils/safeStorage.js'

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
  siparis: { icon: 'pi pi-receipt', severity: 'info', route: '/siparisler' },
  not: { icon: 'pi pi-pen-to-square', severity: 'info', route: '/notlar' },
  banka: { icon: 'pi pi-building', severity: 'warn', route: '/bankalar' },
  kasa: { icon: 'pi pi-wallet', severity: 'info', route: '/kasa' }
}

watch(() => props.visible, (v) => {
  if (v) { ac() } else { query.value = ''; results.value = [] }
})

const ac = () => { query.value = ''; results.value = []; selectedIndex.value = 0; nextTick(() => inputRef.value?.focus()) }
const kapat = () => { emit('update:visible', false); query.value = ''; results.value = [] }

const SON_ARAMA_ANAHTAR = 'raspel_son_aramalar'
const sonAramalar = ref(safeGet(SON_ARAMA_ANAHTAR, []))

const sonAramaKaydet = (q) => {
  const temiz = (q || '').trim()
  if (!temiz) return
  const liste = sonAramalar.value.filter(x => x !== temiz)
  liste.unshift(temiz)
  sonAramalar.value = liste.slice(0, 8)
  safeSet(SON_ARAMA_ANAHTAR, sonAramalar.value)
}

const sonAramaSil = (i) => {
  sonAramalar.value.splice(i, 1)
  safeSet(SON_ARAMA_ANAHTAR, sonAramalar.value)
}

const handleKeydown = (e) => {
  if (e.key === 'Escape') return kapat()
  if (e.key === 'ArrowDown') { e.preventDefault(); selectedIndex.value = Math.min(selectedIndex.value + 1, results.value.length - 1) }
  if (e.key === 'ArrowUp') { e.preventDefault(); selectedIndex.value = Math.max(selectedIndex.value - 1, 0) }
  if (e.key === 'Enter' && results.value[selectedIndex.value]) navigate(results.value[selectedIndex.value])
}

const navigate = (item) => {
  sonAramaKaydet(query.value)
  kapat()
  if (item.type === 'cari' || item.type === 'fatura' || item.type === 'proje' || item.type === 'siparis') {
    router.push(`${typeConfig[item.type].route}/${item.id}`)
  } else {
    router.push(typeConfig[item.type].route)
  }
}

let searchTimer = null

const icindeAra = (val, q) => (val || '').toString().toLowerCase().includes(q)

watch(query, (val) => {
  if (searchTimer) clearTimeout(searchTimer)
  if (!val || val.length < 1) { results.value = []; return }
  searchTimer = setTimeout(async () => {
    loading.value = true; error.value = null; selectedIndex.value = 0
    const q = val.trim().toLowerCase()
    try {
      const [cariler, stoklar, faturalar, personeller, projeler, siparisler, notlar, bankalar, kasalar] = await Promise.all([
        cariHesapAPI.search(q).then(r => r.data.map(d => ({ ...d, type: 'cari', ...typeConfig.cari, title: d.ad, subtitle: `Vergi: ${d.vergiNumarasi || '-'} | Bakiye: ${formatCur(d.bakiye)}` }))).catch(() => []),
        stokAPI.ara(q).then(r => r.data.map(d => ({ ...d, type: 'stok', ...typeConfig.stok, title: d.ad, subtitle: `Kod: ${d.stokKodu || '-'} | Miktar: ${d.miktar || 0} ${d.birim || ''}` }))).catch(() => []),
        faturaAPI.getAll().then(r => (r.data?.content || r.data || []).filter(f => icindeAra(f.faturaNumarasi, q) || icindeAra(f.cariHesapAd, q)).slice(0, 5).map(d => ({ ...d, type: 'fatura', ...typeConfig.fatura, title: `#${d.faturaNumarasi || d.id}`, subtitle: `${d.cariHesapAd || '-'} | ${formatCur(d.genelToplam)}` }))).catch(() => []),
        personelAPI.getAll().then(r => (r.data?.content || r.data || []).filter(p => icindeAra(p.ad, q) || icindeAra(p.soyad, q) || icindeAra(p.pozisyon, q)).slice(0, 5).map(d => ({ ...d, type: 'personel', ...typeConfig.personel, title: `${d.ad || ''} ${d.soyad || ''}`, subtitle: `${d.pozisyon || '-'}` }))).catch(() => []),
        projeAPI.getAll().then(r => (r.data?.content || r.data || []).filter(p => icindeAra(p.ad, q) || icindeAra(p.kod, q)).slice(0, 5).map(d => ({ ...d, type: 'proje', ...typeConfig.proje, title: d.ad, subtitle: `Durum: ${d.durum || '-'}` }))).catch(() => []),
        siparisAPI.getAll().then(r => (r.data?.content || r.data || []).filter(s => icindeAra(s.siparisNo, q) || icindeAra(s.durum, q)).slice(0, 5).map(d => ({ ...d, type: 'siparis', ...typeConfig.siparis, title: `#${d.siparisNo || d.id}`, subtitle: `Durum: ${d.durum || '-'}` }))).catch(() => []),
        notAPI.getAll().then(r => (r.data?.content || r.data || []).filter(n => icindeAra(n.baslik, q)).slice(0, 3).map(d => ({ ...d, type: 'not', ...typeConfig.not, title: d.baslik, subtitle: `Önem: ${d.onemDerecesi || 'NORMAL'}` }))).catch(() => []),
        bankaAPI.getAll().then(r => (r.data?.content || r.data || []).filter(b => icindeAra(b.ad, q) || icindeAra(b.iban, q)).slice(0, 3).map(d => ({ ...d, type: 'banka', ...typeConfig.banka, title: d.ad, subtitle: `IBAN: ${d.iban || '-'}` }))).catch(() => []),
        kasaAPI.getAll().then(r => (r.data?.content || r.data || []).filter(k => icindeAra(k.ad, q)).slice(0, 3).map(d => ({ ...d, type: 'kasa', ...typeConfig.kasa, title: d.ad, subtitle: `Bakiye: ${formatCur(d.bakiye)}` }))).catch(() => [])
      ])
      const birlesik = [...cariler, ...stoklar, ...faturalar, ...personeller, ...projeler, ...siparisler, ...notlar, ...bankalar, ...kasalar]
      const sirali = birlesik.filter(i => i.title?.toLowerCase().startsWith(q)).concat(birlesik.filter(i => !i.title?.toLowerCase().startsWith(q)))
      results.value = sirali.slice(0, 15)
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
.qs-son-aramalar { text-align: left; margin-bottom: 14px; }
.qs-son-baslik { font-size: 11px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.5px; color: #64748b; margin-bottom: 6px; }
.qs-son-item {
  display: flex; align-items: center; gap: 8px;
  padding: 6px 10px; border-radius: 6px; cursor: pointer;
  font-size: 13px; color: #94a3b8;
}
.qs-son-item:hover { background: rgba(148,163,184,0.08); color: #e2e8f0; }
.qs-son-item i:first-child { font-size: 12px; color: #475569; }
.qs-son-sil { margin-left: auto; font-size: 11px; opacity: 0; }
.qs-son-item:hover .qs-son-sil { opacity: 0.7; }
.qs-son-sil:hover { opacity: 1 !important; color: #f87171; }
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
