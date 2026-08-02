<template>
  <div class="bildirim-zili">
    <Button icon="pi pi-bell" class="p-button-rounded p-button-text" @click="panelAcik = !panelAcik" title="Bildirimler">
      <span v-if="filtrelenmisBildirimler.length" class="zil-sayac">{{ filtrelenmisBildirimler.length }}</span>
    </Button>

    <transition name="panel">
      <div v-if="panelAcik" class="bildirim-panel">
        <div class="panel-baslik">
          <strong>Bildirimler</strong>
          <div class="panel-ayarlar">
            <Button icon="pi pi-cog" class="p-button-sm p-button-text" @click="tercihPaneli = !tercihPaneli" title="Bildirim Tercihleri" />
            <Button icon="pi pi-check" class="p-button-sm p-button-text" @click="temizle" title="Temizle" />
          </div>
        </div>
        <div v-if="tercihPaneli" class="tercih-paneli">
          <div class="tercih-baslik">Bildirim Türleri</div>
          <label v-for="t in tercihListesi" :key="t.tur" class="tercih-satir">
            <Checkbox :model-value="tercihler[t.tur] !== false" :binary="true" @update:model-value="tercihDegistir(t.tur, $event)" />
            <span>{{ t.etiket }}</span>
          </label>
        </div>
        <div v-if="filtrelenmisBildirimler.length === 0 && !tercihPaneli" class="panel-bos">
          <i class="pi pi-inbox"></i>
          <p>Bildirim yok</p>
        </div>
        <div v-else-if="!tercihPaneli" class="panel-liste">
          <div v-for="(b, i) in filtrelenmisBildirimler" :key="i" class="panel-item" @click="bildirimTikla(b)">
            <div class="item-ikon" :class="ikonSinifi(b.tur)">
              <i :class="ikonSinifi(b.tur)"></i>
            </div>
            <div class="item-icerik">
              <strong>{{ b.baslik }}</strong>
              <p>{{ b.mesaj }}</p>
              <small>{{ formatTarih(b.tarih) }}</small>
            </div>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useWebSocket } from '../composables/useWebSocket.js'

const router = useRouter()
const panelAcik = ref(false)
const tercihPaneli = ref(false)
const bildirimler = ref([])
const { sonBildirim } = useWebSocket()

const TERCIH_ANAHTAR = 'raspel_bildirim_tercihleri'
const tercihListesi = [
  { tur: 'STOK', etiket: 'Stok Uyarıları' },
  { tur: 'SIPARIS', etiket: 'Siparişler' },
  { tur: 'FATURA', etiket: 'Faturalar' },
  { tur: 'ODEME', etiket: 'Ödemeler' },
  { tur: 'TAKSiLAT', etiket: 'Tahsilatlar' },
  { tur: 'UYARI', etiket: 'Uyarılar' },
  { tur: 'INFO', etiket: 'Bilgilendirme' }
]
const tercihler = ref(JSON.parse(localStorage.getItem(TERCIH_ANAHTAR) || '{}'))

const filtrelenmisBildirimler = computed(() =>
  bildirimler.value.filter(b => tercihler.value[b.tur] !== false)
)

const tercihDegistir = (tur, val) => {
  tercihler.value[tur] = val
  localStorage.setItem(TERCIH_ANAHTAR, JSON.stringify(tercihler.value))
}

watch(sonBildirim, (yeni) => {
  if (yeni) bildirimler.value.unshift(yeni)
})

const ikonSinifi = (tur) => {
  const ikonlar = {
    STOK: 'pi pi-box',
    SIPARIS: 'pi pi-receipt',
    FATURA: 'pi pi-file',
    ODEME: 'pi pi-credit-card',
    TAKSILAT: 'pi pi-arrow-down',
    UYARI: 'pi pi-exclamation-triangle',
    INFO: 'pi pi-info-circle'
  }
  return ikonlar[tur] || ikonlar.INFO
}

const formatTarih = (t) => {
  if (!t) return ''
  return new Date(t).toLocaleString('tr-TR', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' })
}

const bildirimTikla = (b) => {
  if (b.yonlendirme) router.push(b.yonlendirme)
  panelAcik.value = false
}

const temizle = () => { bildirimler.value = [] }
</script>

<style scoped>
.bildirim-zili { position: relative; }
.zil-sayac {
  position: absolute; top: -2px; right: -2px;
  background: #ef4444; color: white; border-radius: 50%;
  min-width: 18px; height: 18px; font-size: 11px;
  display: flex; align-items: center; justify-content: center;
  font-weight: 700; padding: 0 4px;
}
.bildirim-panel {
  position: absolute; top: 48px; right: 0; z-index: 1000;
  width: 340px; max-height: 420px; overflow-y: auto;
  background: var(--bg-card); border: 1px solid var(--border);
  border-radius: 12px; box-shadow: var(--shadow);
}
.panel-baslik {
  display: flex; justify-content: space-between; align-items: center;
  padding: 12px 16px; border-bottom: 1px solid var(--border);
  position: sticky; top: 0; background: var(--bg-card);
}
.panel-ayarlar { display: flex; align-items: center; }
.tercih-paneli { padding: 12px 16px; }
.tercih-baslik { font-size: 11px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.5px; color: var(--text-muted); margin-bottom: 8px; }
.tercih-satir { display: flex; align-items: center; gap: 8px; padding: 4px 0; font-size: 13px; cursor: pointer; }
.panel-baslik strong { font-size: 14px; }
.panel-bos { text-align: center; padding: 32px; color: var(--text-muted); }
.panel-bos i { font-size: 32px; display: block; margin-bottom: 8px; }
.panel-bos p { margin: 0; font-size: 13px; }
.panel-liste { padding: 4px 0; }
.panel-item {
  display: flex; gap: 12px; padding: 10px 16px; cursor: pointer;
  border-bottom: 1px solid var(--border);
  transition: background 0.15s;
}
.panel-item:last-child { border-bottom: none; }
.panel-item:hover { background: rgba(148,163,184,0.06); }
.item-ikon {
  width: 34px; height: 34px; border-radius: 8px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  font-size: 16px; color: #60a5fa; background: rgba(59,130,246,0.15);
}
.item-ikon.pi-exclamation-triangle { color: #fbbf24; background: rgba(245,158,11,0.15); }
.item-icerik { flex: 1; min-width: 0; }
.item-icerik strong { display: block; font-size: 13px; }
.item-icerik p { margin: 2px 0; font-size: 12px; color: var(--text-secondary); }
.item-icerik small { font-size: 11px; color: var(--text-muted); }
.panel-enter-active, .panel-leave-active { transition: all 0.2s ease; }
.panel-enter-from, .panel-leave-to { opacity: 0; transform: translateY(-8px); }
</style>
