<template>
  <div class="sohbet-sayfasi">
    <div class="sohbet-baslik">
      <h1><i class="pi pi-comments" style="margin-right:8px" />Ekip Sohbeti</h1>
      <span v-if="bagli" class="bagli-durum"><i class="pi pi-circle-on" /> Bağlı</span>
      <span v-else class="bagli-durum bagli-degil"><i class="pi pi-circle-off" /> Bağlanıyor...</span>
    </div>

    <div ref="mesajKutusu" class="mesaj-kutusu">
      <div v-if="yukleniyor" class="bos">Yükleniyor...</div>
      <div v-else-if="!mesajlar.length" class="bos">Henüz mesaj yok. İlk mesajı siz yazın.</div>
      <div
        v-for="m in mesajlar"
        :key="m.id"
        class="mesaj"
        :class="{ kendi: m.kullaniciId === kendiId }"
      >
        <div class="mesaj-ust">
          <strong>{{ m.kullaniciAd || 'Bilinmeyen' }}</strong>
          <span class="mesaj-zaman">{{ formatZaman(m.olusturmaTarihi) }}</span>
        </div>
        <div class="mesaj-icerik">{{ m.mesaj }}</div>
      </div>
    </div>

    <div class="mesaj-giris">
      <InputText
        v-model="yeniMesaj"
        placeholder="Mesajınızı yazın..."
        class="mesaj-input"
        @keyup.enter="gonder"
      />
      <Button
        icon="pi pi-send"
        label="Gönder"
        :loading="gonderiliyor"
        @click="gonder"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useAuthStore } from '../stores/authStore.js'
import { sohbetAPI } from '../api/index.js'
import { useToastBildirim } from '../composables/useToastBildirim.js'

const authStore = useAuthStore()
const toastBildirim = useToastBildirim()

const mesajlar = ref([])
const yeniMesaj = ref('')
const gonderiliyor = ref(false)
const yukleniyor = ref(false)
const bagli = ref(false)
const mesajKutusu = ref(null)

const kendiId = authStore.kullanici?.id

let stompClient = null
let subscription = null

const formatZaman = (t) => {
  if (!t) return ''
  const d = new Date(t)
  return d.toLocaleTimeString('tr-TR', { hour: '2-digit', minute: '2-digit' }) + ' · ' + d.toLocaleDateString('tr-TR')
}

const kaydir = () => {
  nextTick(() => {
    if (mesajKutusu.value) mesajKutusu.value.scrollTop = mesajKutusu.value.scrollHeight
  })
}

const gonder = async () => {
  const metin = yeniMesaj.value.trim()
  if (!metin) return
  gonderiliyor.value = true
  try {
    await sohbetAPI.gonder({ mesaj: metin })
    yeniMesaj.value = ''
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || 'Mesaj gönderilemedi')
  } finally {
    gonderiliyor.value = false
  }
}

const yukle = async () => {
  yukleniyor.value = true
  try {
    const r = await sohbetAPI.sonMesajlar()
    mesajlar.value = r.data || []
    kaydir()
  } catch { mesajlar.value = [] }
  yukleniyor.value = false
}

const baglan = () => {
  const token = authStore.token || ''
  const SOCKET_URL = import.meta.env.VITE_WS_URL || (window.location.origin + '/ws' + (token ? '?token=' + encodeURIComponent(token) : ''))
  import('sockjs-client').then(SockJS => {
    import('@stomp/stompjs').then(({ Client }) => {
      const socket = new SockJS.default(SOCKET_URL)
      stompClient = new Client({
        webSocketFactory: () => socket,
        reconnectDelay: 5000,
        onConnect: () => {
          bagli.value = true
          const sirketId = authStore.sirketId
          subscription = stompClient.subscribe(`/topic/sohbet/${sirketId}`, (msg) => {
            try {
              mesajlar.value.push(JSON.parse(msg.body))
              kaydir()
            } catch { /* empty */ }
          })
        },
        onDisconnect: () => { bagli.value = false }
      })
      stompClient.activate()
    })
  }).catch(() => {})
}

onMounted(() => {
  yukle()
  baglan()
})

onUnmounted(() => {
  if (subscription) subscription.unsubscribe()
  if (stompClient) stompClient.deactivate()
})
</script>

<style scoped>
.sohbet-sayfasi { display: flex; flex-direction: column; height: calc(100vh - 120px); padding: 0; }
.sohbet-baslik { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
.sohbet-baslik h1 { margin: 0; }
.bagli-durum { font-size: 12px; color: #22c55e; display: flex; align-items: center; gap: 4px; }
.bagli-durum.bagli-degil { color: #f59e0b; }
.mesaj-kutusu { flex: 1; overflow-y: auto; background: var(--bg-card); border: 1px solid var(--border); border-radius: 12px; padding: 16px; display: flex; flex-direction: column; gap: 12px; }
.bos { text-align: center; color: var(--text-muted); padding: 40px 0; }
.mesaj { max-width: 70%; align-self: flex-start; background: var(--bg-primary); border: 1px solid var(--border); border-radius: 10px; padding: 10px 14px; }
.mesaj.kendi { align-self: flex-end; background: rgba(59,130,246,0.12); border-color: rgba(59,130,246,0.3); }
.mesaj-ust { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
.mesaj-ust strong { font-size: 13px; }
.mesaj-zaman { font-size: 11px; color: var(--text-muted); }
.mesaj-icerik { font-size: 14px; white-space: pre-wrap; word-break: break-word; }
.mesaj-giris { display: flex; gap: 8px; margin-top: 12px; }
.mesaj-input { flex: 1; }
</style>
