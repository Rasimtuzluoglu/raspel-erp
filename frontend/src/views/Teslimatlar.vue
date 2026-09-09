<template>
  <div class="teslimat-sayfasi">
    <div class="teslimat-baslik">
      <h1>
        <i
          class="pi pi-truck"
          style="margin-right: 8px; color: #3b82f6"
        />Teslimatlar
      </h1>
      <Button
        icon="pi pi-refresh"
        class="p-button-text p-button-sm"
        :loading="yukleniyor"
        @click="suruculeriYukle"
      />
    </div>

    <div class="teslimat-duzen">
      <!-- Şoför Listesi -->
      <div class="surucu-listesi">
        <div
          v-if="yukleniyor"
          class="bos"
        >
          Yükleniyor...
        </div>
        <div
          v-else-if="!suruculer.length"
          class="bos"
        >
          Henüz şoför (DRIVER rolü) tanımlanmamış.
        </div>
        <button
          v-for="s in suruculer"
          :key="s.id"
          class="surucu-kart"
          :class="{ aktif: seciliSurucu?.id === s.id }"
          @click="surucuSec(s)"
        >
          <i class="pi pi-user" />
          <span class="surucu-ad">{{ s.ad }}</span>
          <span
            class="bekleyen-rozet"
            :class="{ var: s.bekleyenTeslimatSayisi > 0 }"
          >{{ s.bekleyenTeslimatSayisi }}</span>
        </button>
      </div>

      <!-- Teslimat Noktaları -->
      <div class="teslimat-icerik">
        <div
          v-if="!seciliSurucu"
          class="bos buyuk"
        >
          <i class="pi pi-user" />
          <p>Bir şoför seçin; o şoförün teslimat noktaları burada listelensin.</p>
        </div>

        <template v-else>
          <div class="teslimat-icerik-baslik">
            <h2>
              <i class="pi pi-user" /> {{ seciliSurucu.ad }}
            </h2>
            <Tag
              :value="seciliSurucu.bekleyenTeslimatSayisi + ' bekleyen'"
              :severity="seciliSurucu.bekleyenTeslimatSayisi > 0 ? 'warn' : 'success'"
            />
          </div>

          <div
            v-if="teslimatYukleniyor"
            class="bos"
          >
            Yükleniyor...
          </div>
          <div
            v-else-if="!teslimatlar.length"
            class="bos"
          >
            Bu şoförün atanmış teslimatı yok.
          </div>

          <div
            v-for="t in teslimatlar"
            :key="t.id"
            class="teslimat-kart"
          >
            <div class="teslimat-ust">
              <span class="fatura-no">
                <i class="pi pi-file" /> {{ t.faturaNumarasi ? '#' + t.faturaNumarasi : 'Fatura #' + t.faturaId }}
              </span>
              <span
                v-if="t.musteriAdi"
                class="musteri"
              >{{ t.musteriAdi }}</span>
              <Tag
                :value="durumAdi(t.durum)"
                :severity="durumSeverity(t.durum)"
              />
              <Tag
                v-if="t.gecikti"
                value="Gecikti"
                severity="danger"
              />
            </div>
            <div class="adres">
              <i class="pi pi-map-marker" />
              <span>{{ t.teslimatAdresi || '—' }}</span>
            </div>
            <div
              v-if="t.beklenenTeslimTarihi"
              class="beklenen"
            >
              <i class="pi pi-calendar" /> Beklenen teslim: {{ t.beklenenTeslimTarihi }}
            </div>
            <div
              v-if="t.notlar"
              class="not"
            >
              <i class="pi pi-comment" /> {{ t.notlar }}
            </div>
            <div class="teslimat-aksiyonlar">
              <div class="foto-alan">
                <img
                  v-if="t.teslimatFoto"
                  :src="t.teslimatFoto"
                  class="teslimat-foto"
                  alt="Teslimat fotoğrafı"
                >
                <label class="foto-yukle">
                  <i class="pi pi-camera" /> Foto
                  <input
                    type="file"
                    accept="image/*"
                    hidden
                    @change="(e) => fotoYukle(t, e)"
                  >
                </label>
              </div>
              <a
                class="yol-tarifi"
                :href="yolTarifiUrl(t)"
                target="_blank"
                rel="noopener"
              >
                <i class="pi pi-directions" /> Yol Tarifi Al
              </a>
              <Dropdown
                :model-value="t.durum"
                :options="durumSecenekleri"
                option-label="label"
                option-value="value"
                class="durum-dropdown"
                @update:model-value="(d) => durumGuncelle(t, d)"
              />
            </div>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { teslimatAPI } from '../api/index.js'
import { useToastBildirim } from '../composables/useToastBildirim.js'

const toastBildirim = useToastBildirim()

const suruculer = ref([])
const teslimatlar = ref([])
const seciliSurucu = ref(null)
const yukleniyor = ref(false)
const teslimatYukleniyor = ref(false)

const durumSecenekleri = [
  { label: 'Beklemede', value: 'BEKLEMEDE' },
  { label: 'Yolda', value: 'YOLDA' },
  { label: 'Teslim Edildi', value: 'TESLIM_EDILDI' },
  { label: 'İptal', value: 'IPTAL' }
]

const durumAdi = (d) => durumSecenekleri.find((x) => x.value === d)?.label || d || '—'

const durumSeverity = (d) => {
  const map = { BEKLEMEDE: 'warn', YOLDA: 'info', TESLIM_EDILDI: 'success', IPTAL: 'danger' }
  return map[d] || 'secondary'
}

const yolTarifiUrl = (t) =>
  `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(t.teslimatAdresi || '')}`

const suruculeriYukle = async () => {
  yukleniyor.value = true
  try {
    const r = await teslimatAPI.byDriver()
    suruculer.value = r.data || []
    if (seciliSurucu.value) {
      const guncel = suruculer.value.find((s) => s.id === seciliSurucu.value.id)
      if (guncel) {
        seciliSurucu.value = guncel
        await teslimatlarYukle(guncel.id)
      } else {
        seciliSurucu.value = null
        teslimatlar.value = []
      }
    }
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || 'Şoförler yüklenemedi')
    suruculer.value = []
  } finally {
    yukleniyor.value = false
  }
}

const teslimatlarYukle = async (driverId) => {
  teslimatYukleniyor.value = true
  try {
    const r = await teslimatAPI.teslimatlar(driverId)
    teslimatlar.value = r.data || []
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || 'Teslimatlar yüklenemedi')
    teslimatlar.value = []
  } finally {
    teslimatYukleniyor.value = false
  }
}

const surucuSec = async (s) => {
  seciliSurucu.value = s
  await teslimatlarYukle(s.id)
}

const durumGuncelle = async (t, yeniDurum) => {
  try {
    await teslimatAPI.durumGuncelle(t.id, yeniDurum)
    t.durum = yeniDurum
    toastBildirim.basarili('Durum güncellendi')
    if (seciliSurucu.value) {
      await suruculeriYukle()
    }
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || 'Durum güncellenemedi')
  }
}

const fotoYukle = async (t, event) => {
  const dosya = event.target.files?.[0]
  if (!dosya) return
  try {
    const res = await teslimatAPI.fotoYukle(t.id, dosya)
    t.teslimatFoto = res.data?.teslimatFoto || t.teslimatFoto
    toastBildirim.basarili('Fotoğraf yüklendi')
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || 'Fotoğraf yüklenemedi')
  } finally {
    event.target.value = ''
  }
}

onMounted(() => {
  suruculeriYukle()
})
</script>

<style scoped>
.teslimat-sayfasi {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.teslimat-baslik {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.teslimat-baslik h1 {
  margin: 0;
  font-size: 20px;
  display: flex;
  align-items: center;
}
.teslimat-duzen {
  display: flex;
  gap: 16px;
  min-height: 0;
  align-items: flex-start;
}
.surucu-listesi {
  width: 260px;
  min-width: 220px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 10px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.surucu-kart {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  border: none;
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
  font-size: 13px;
  text-align: left;
  width: 100%;
}
.surucu-kart:hover {
  background: var(--bg-primary);
}
.surucu-kart.aktif {
  background: rgba(59, 130, 246, 0.12);
  color: var(--accent, #3b82f6);
  font-weight: 600;
}
.surucu-ad {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.bekleyen-rozet {
  min-width: 22px;
  height: 22px;
  border-radius: 11px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 700;
  background: rgba(148, 163, 184, 0.2);
  color: var(--text-muted);
}
.bekleyen-rozet.var {
  background: rgba(245, 158, 11, 0.2);
  color: #fbbf24;
}
.teslimat-icerik {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.teslimat-icerik-baslik {
  display: flex;
  align-items: center;
  gap: 10px;
}
.teslimat-icerik-baslik h2 {
  margin: 0;
  font-size: 16px;
  display: flex;
  align-items: center;
  gap: 6px;
}
.teslimat-kart {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.teslimat-ust {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.fatura-no {
  font-weight: 700;
  font-size: 13px;
  color: var(--accent, #3b82f6);
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.musteri {
  font-size: 13px;
  color: var(--text-secondary);
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.adres {
  display: flex;
  gap: 8px;
  font-size: 13px;
  color: var(--text-primary);
}
.adres i {
  color: #ef4444;
  margin-top: 2px;
}
.not {
  font-size: 12px;
  color: var(--text-muted);
  display: flex;
  gap: 6px;
}
.teslimat-aksiyonlar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 4px;
}
.yol-tarifi {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 8px;
  background: rgba(59, 130, 246, 0.12);
  color: var(--accent, #3b82f6);
  font-size: 12px;
  font-weight: 600;
  text-decoration: none;
}
.yol-tarifi:hover {
  background: rgba(59, 130, 246, 0.22);
}
.durum-dropdown {
  min-width: 150px;
}
.beklenen {
  font-size: 12px;
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  gap: 6px;
}
.foto-alan {
  display: flex;
  align-items: center;
  gap: 8px;
}
.teslimat-foto {
  width: 48px;
  height: 48px;
  object-fit: cover;
  border-radius: 8px;
  border: 1px solid var(--border);
}
.foto-yukle {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 8px;
  background: var(--bg-primary);
  border: 1px solid var(--border);
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
}
.foto-yukle:hover {
  border-color: var(--accent, #3b82f6);
  color: var(--accent, #3b82f6);
}
.bos {
  text-align: center;
  color: var(--text-muted);
  padding: 24px 12px;
  font-size: 13px;
}
.bos.buyuk {
  padding: 60px 20px;
}
.bos.buyuk i {
  font-size: 40px;
  display: block;
  margin-bottom: 12px;
  color: var(--text-muted);
}
@media (max-width: 900px) {
  .teslimat-duzen {
    flex-direction: column;
  }
  .surucu-listesi {
    width: 100%;
    flex-direction: row;
    flex-wrap: wrap;
  }
  .surucu-kart {
    width: auto;
    flex: 1;
    min-width: 140px;
  }
}
</style>
