<template>
  <div class="sohbet-sayfasi">
    <div class="sohbet-baslik">
      <h1>
        <i
          :class="aktifMod === 'ai' ? 'pi pi-sparkles' : 'pi pi-comments'"
          style="margin-right: 8px; color: #3b82f6"
        />
        {{ aktifMod === 'ai' ? 'Yapay Zeka (AI) Asistanı' : 'Ekip Sohbeti' }}
      </h1>
      <div class="header-sag">
        <SelectButton
          v-model="aktifMod"
          :options="modSecenekleri"
          option-label="label"
          option-value="value"
          size="small"
        />
        <Tag
          v-if="aktifMod === 'ai' && aiYapilandirildi"
          value="Gerçek AI (LLM)"
          severity="success"
        />
        <Tag
          v-else-if="aktifMod === 'ai'"
          value="Kural Tabanlı AI"
          severity="info"
        />
        <span
          v-if="aktifMod === 'ekip' && bagli"
          class="bagli-durum"
        ><i class="pi pi-circle-on" /> Bağlı</span>
        <span
          v-else-if="aktifMod === 'ekip'"
          class="bagli-durum bagli-degil"
        ><i class="pi pi-circle-off" /> Bağlanıyor...</span>
      </div>
    </div>

    <!-- AI Asistan Hızlı Sorular -->
    <div
      v-if="aktifMod === 'ai'"
      class="ai-oneriler"
    >
      <span class="oneri-baslik"><i class="pi pi-bolt" /> Hızlı Sorular:</span>
      <button
        v-for="oneri in hizliSorular"
        :key="oneri"
        class="oneri-cip"
        @click="hizliSoruSor(oneri)"
      >
        {{ oneri }}
      </button>
    </div>

    <!-- Mesaj Alanı -->
    <div
      ref="mesajKutusu"
      class="mesaj-kutusu"
    >
      <!-- Ekip Modu -->
      <template v-if="aktifMod === 'ekip'">
        <div
          v-if="yukleniyor"
          class="bos"
        >
          Yükleniyor...
        </div>
        <div
          v-else-if="!mesajlar.length"
          class="bos"
        >
          Henüz mesaj yok. İlk mesajı siz yazın.
        </div>
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
          <div class="mesaj-icerik">
            {{ m.mesaj }}
          </div>
        </div>
      </template>

      <!-- AI Modu -->
      <template v-else>
        <div
          v-if="!aiMesajlar.length"
          class="ai-bos-durum"
        >
          <i class="pi pi-sparkles ai-ikon-buyuk" />
          <h3>RasPel Yapay Zeka ERP Asistanı</h3>
          <p>Şirketinizin finans, ciro, stok, kasa ve vadesi gelen ödemeleri hakkında doğal dilde sorular sorabilirsiniz.</p>
        </div>
        <div
          v-for="(m, i) in aiMesajlar"
          :key="i"
          class="mesaj"
          :class="{ kendi: m.rol === 'user', 'ai-cevap': m.rol === 'ai' }"
        >
          <div class="mesaj-ust">
            <strong>
              <i :class="m.rol === 'user' ? 'pi pi-user' : 'pi pi-sparkles'" />
              {{ m.rol === 'user' ? 'Siz' : 'RasPel AI' }}
            </strong>
            <span class="mesaj-zaman">{{ formatZaman(m.zaman) }}</span>
          </div>
          <div class="mesaj-icerik">
            <p>{{ m.metin }}</p>

            <!-- Mini Tablo Çıktısı -->
            <div
              v-if="m.tabloVerisi && m.tabloVerisi.length"
              class="ai-tablo-wrapper"
            >
              <table class="ai-tablo">
                <thead>
                  <tr>
                    <th
                      v-for="(val, key) in m.tabloVerisi[0]"
                      :key="key"
                    >
                      {{ formatTabloBaslik(key) }}
                    </th>
                  </tr>
                </thead>
                <tbody>
                  <tr
                    v-for="(row, rIndex) in m.tabloVerisi"
                    :key="rIndex"
                  >
                    <td
                      v-for="(val, key) in row"
                      :key="key"
                    >
                      {{ val }}
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>

            <!-- Basit Veri Rozetleri -->
            <div
              v-if="m.grafikVerisi && m.grafikVerisi.labels"
              class="ai-rozet-grid"
            >
              <div
                v-for="(lbl, lIdx) in m.grafikVerisi.labels"
                :key="lIdx"
                class="ai-rozet"
              >
                <span>{{ lbl }}</span>
                <strong>{{ m.grafikVerisi.datasets?.[0]?.data?.[lIdx] }} ₺</strong>
              </div>
            </div>
          </div>
        </div>
        <div
          v-if="aiYukleniyor"
          class="ai-loading"
        >
          <i class="pi pi-spin pi-spinner" /> Yapay zeka verileri analiz ediyor...
        </div>
      </template>
    </div>

    <!-- Mesaj Giriş Alanı -->
    <div class="mesaj-giris">
      <InputText
        v-model="yeniMesaj"
        :placeholder="aktifMod === 'ai' ? 'Yapay zekaya bir soru sorun (Örn: Bu ay en çok ciro yapan 3 müşteri kim?)...' : 'Mesajınızı yazın...'"
        class="mesaj-input"
        @keyup.enter="gonder"
      />
      <Button
        :icon="aktifMod === 'ai' ? 'pi pi-sparkles' : 'pi pi-send'"
        :label="aktifMod === 'ai' ? 'Sor' : 'Gönder'"
        :loading="gonderiliyor || aiYukleniyor"
        @click="gonder"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useAuthStore } from '../stores/authStore.js'
import { sohbetAPI, aiConfigAPI } from '../api/index.js'
import { useToastBildirim } from '../composables/useToastBildirim.js'

const authStore = useAuthStore()
const toastBildirim = useToastBildirim()

const aktifMod = ref('ai')
const aiYapilandirildi = ref(false)
const modSecenekleri = [
  { label: 'AI Asistan', value: 'ai' },
  { label: 'Ekip Sohbeti', value: 'ekip' }
]

const hizliSorular = [
  'Bu ay en çok ciro yaptığımız 3 müşteri kim?',
  'Gelecek hafta vadesi gelen ödemelerim neler?',
  'Kasa ve banka toplam bakiyemiz nedir?'
]

const mesajlar = ref([])
const aiMesajlar = ref([])
const yeniMesaj = ref('')
const gonderiliyor = ref(false)
const aiYukleniyor = ref(false)
const yukleniyor = ref(false)
const bagli = ref(false)
const mesajKutusu = ref(null)

const kendiId = computed(() => authStore?.kullanici?.id)

let stompClient = null
let subscription = null

const formatZaman = (t) => {
  if (!t) return ''
  const d = new Date(t)
  return d.toLocaleTimeString('tr-TR', { hour: '2-digit', minute: '2-digit' })
}

const formatTabloBaslik = (key) => {
  const map = {
    sira: '#',
    musteri: 'Müşteri',
    ciro: 'Toplam Ciro',
    faturaNo: 'Fatura No',
    cari: 'Cari Hesap',
    vade: 'Vade Tarihi',
    tur: 'İşlem Türü',
    tutar: 'Tutar',
    hesap: 'Hesap Adı',
    bakiye: 'Bakiye'
  }
  return map[key] || key
}

const kaydir = () => {
  nextTick(() => {
    if (mesajKutusu.value) mesajKutusu.value.scrollTop = mesajKutusu.value.scrollHeight
  })
}

const hizliSoruSor = (soru) => {
  yeniMesaj.value = soru
  gonder()
}

const gonder = async () => {
  const metin = yeniMesaj.value.trim()
  if (!metin) return

  if (aktifMod.value === 'ai') {
    aiMesajlar.value.push({ rol: 'user', metin, zaman: new Date() })
    yeniMesaj.value = ''
    kaydir()
    aiYukleniyor.value = true

    try {
      const res = await sohbetAPI.aiSorgu(metin)
      const data = res.data
      aiMesajlar.value.push({
        rol: 'ai',
        metin: data.cevapMetni,
        grafikTipi: data.grafikTipi,
        grafikVerisi: data.grafikVerisi,
        tabloVerisi: data.tabloVerisi,
        zaman: new Date()
      })
    } catch {
      aiMesajlar.value.push({
        rol: 'ai',
        metin: 'Üzgünüm, sorunuzu işlerken bir hata oluştu. Lütfen tekrar deneyin.',
        zaman: new Date()
      })
    } finally {
      aiYukleniyor.value = false
      kaydir()
    }
    return
  }

  // Ekip Sohbeti
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
  } catch {
    mesajlar.value = []
  }
  yukleniyor.value = false
}

const baglan = () => {
  const token = authStore.token || ''
  const SOCKET_URL =
    import.meta.env.VITE_WS_URL || window.location.origin + '/ws' + (token ? '?token=' + encodeURIComponent(token) : '')
  import('sockjs-client')
    .then((SockJS) => {
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
              } catch {
                /* empty */
              }
            })
          },
          onDisconnect: () => {
            bagli.value = false
          }
        })
        stompClient.activate()
      })
    })
    .catch(() => {})
}

onMounted(async () => {
  yukle()
  baglan()
  try {
    const res = await aiConfigAPI.getConfig()
    if (res.data && res.data.durum === 'AKTIF') {
      aiYapilandirildi.value = true
    }
  } catch {
    /* empty */
  }
})

onUnmounted(() => {
  if (subscription) subscription.unsubscribe()
  if (stompClient) stompClient.deactivate()
})
</script>

<style scoped>
.sohbet-sayfasi {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 120px);
  padding: 0;
}
.sohbet-baslik {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  flex-wrap: wrap;
  gap: 10px;
}
.sohbet-baslik h1 {
  margin: 0;
  font-size: 20px;
  display: flex;
  align-items: center;
}
.header-sag {
  display: flex;
  align-items: center;
  gap: 12px;
}
.bagli-durum {
  font-size: 12px;
  color: #22c55e;
  display: flex;
  align-items: center;
  gap: 4px;
}
.bagli-durum.bagli-degil {
  color: #f59e0b;
}
.ai-oneriler {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  overflow-x: auto;
  padding-bottom: 4px;
}
.oneri-baslik {
  font-size: 12px;
  font-weight: 700;
  color: var(--text-muted);
  white-space: nowrap;
}
.oneri-cip {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 20px;
  padding: 6px 14px;
  font-size: 12px;
  color: var(--text-secondary);
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s ease;
}
.oneri-cip:hover {
  background: rgba(59, 130, 246, 0.1);
  border-color: var(--accent, #3b82f6);
  color: var(--accent, #3b82f6);
}
.mesaj-kutusu {
  flex: 1;
  overflow-y: auto;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.bos {
  text-align: center;
  color: var(--text-muted);
  padding: 40px 0;
}
.ai-bos-durum {
  text-align: center;
  padding: 60px 20px;
  color: var(--text-muted);
  margin: auto;
}
.ai-ikon-buyuk {
  font-size: 48px;
  color: #3b82f6;
  margin-bottom: 16px;
  display: block;
}
.ai-bos-durum h3 {
  color: var(--text-primary);
  margin: 0 0 8px 0;
}
.ai-bos-durum p {
  max-width: 500px;
  margin: 0 auto;
  font-size: 14px;
}
.mesaj {
  max-width: 75%;
  align-self: flex-start;
  background: var(--bg-primary);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 12px 16px;
}
.mesaj.kendi {
  align-self: flex-end;
  background: rgba(59, 130, 246, 0.12);
  border-color: rgba(59, 130, 246, 0.3);
}
.mesaj.ai-cevap {
  background: rgba(16, 185, 129, 0.08);
  border-color: rgba(16, 185, 129, 0.25);
  max-width: 85%;
}
.mesaj-ust {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}
.mesaj-ust strong {
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 5px;
}
.mesaj-zaman {
  font-size: 11px;
  color: var(--text-muted);
}
.mesaj-icerik {
  font-size: 14px;
  white-space: pre-wrap;
  word-break: break-word;
}
.ai-tablo-wrapper {
  margin-top: 10px;
  overflow-x: auto;
}
.ai-tablo {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
}
.ai-tablo th {
  background: rgba(0, 0, 0, 0.1);
  padding: 6px 10px;
  text-align: left;
  border-bottom: 1px solid var(--border);
}
.ai-tablo td {
  padding: 6px 10px;
  border-bottom: 1px solid var(--border);
}
.ai-rozet-grid {
  display: flex;
  gap: 8px;
  margin-top: 10px;
  flex-wrap: wrap;
}
.ai-rozet {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 6px 12px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.ai-rozet span {
  font-size: 11px;
  color: var(--text-muted);
}
.ai-rozet strong {
  font-size: 13px;
  color: #10b981;
}
.ai-loading {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-muted);
  font-size: 13px;
  padding: 8px 12px;
}
.mesaj-giris {
  display: flex;
  gap: 8px;
  margin-top: 12px;
}
.mesaj-input {
  flex: 1;
}
</style>
