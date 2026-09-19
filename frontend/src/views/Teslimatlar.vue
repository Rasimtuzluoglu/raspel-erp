<template>
  <div class="teslimat-sayfasi">
    <div class="teslimat-baslik">
      <h1>
        <i
          class="pi pi-truck"
          style="margin-right: 8px; color: #3b82f6"
        />{{ t('teslimatlar.title') }}
      </h1>
      <Button
        icon="pi pi-refresh"
        class="p-button-text p-button-sm"
        :loading="yukleniyor"
        @click="suruculeriYukle"
      />
    </div>

    <div class="nasil-kart">
      <button
        type="button"
        class="nasil-baslik"
        @click="rehberAcik = !rehberAcik"
      >
        <i class="pi pi-question-circle" />
        <span>{{ t('teslimatlar.nasilCalisir') }}</span>
        <i :class="rehberAcik ? 'pi pi-chevron-up' : 'pi pi-chevron-down'" />
      </button>
      <div
        v-if="rehberAcik"
        class="nasil-icerik"
      >
        <div class="nasil-adim">
          <strong>{{ t('teslimatlar.nasil1Baslik') }}</strong>
          <span>{{ t('teslimatlar.nasil1Metin') }}</span>
        </div>
        <div class="nasil-adim">
          <strong>{{ t('teslimatlar.nasil2Baslik') }}</strong>
          <span>{{ t('teslimatlar.nasil2Metin') }}</span>
        </div>
        <div class="nasil-adim">
          <strong>{{ t('teslimatlar.nasil3Baslik') }}</strong>
          <span>{{ t('teslimatlar.nasil3Metin') }}</span>
        </div>
        <div class="nasil-adim">
          <strong>{{ t('teslimatlar.nasil4Baslik') }}</strong>
          <span>{{ t('teslimatlar.nasil4Metin') }}</span>
        </div>
      </div>
    </div>

    <div class="teslimat-duzen">
      <!-- Şoför Listesi -->
      <div
        v-if="!soforMu"
        class="surucu-listesi"
      >
        <div
          v-if="yukleniyor"
          class="bos"
        >
          {{ t('common.loading') }}
        </div>
        <div
          v-else-if="!suruculer.length"
          class="bos"
        >
          {{ t('teslimatlar.soforYok') }}
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
          <p>{{ t('teslimatlar.soforSec') }}</p>
        </div>

        <template v-else>
          <div class="teslimat-icerik-baslik">
            <h2>
              <i class="pi pi-user" /> {{ seciliSurucu.ad }}
            </h2>
            <Tag
              :value="t('teslimatlar.bekleyen', { n: seciliSurucu.bekleyenTeslimatSayisi })"
              :severity="seciliSurucu.bekleyenTeslimatSayisi > 0 ? 'warn' : 'success'"
            />
          </div>

          <div class="filtre-sekmeleri">
            <button
              class="filtre-sekme"
              :class="{ aktif: filtre === 'TUMU' }"
              @click="filtre = 'TUMU'"
            >
              {{ t('teslimatlar.tumu') }}
            </button>
            <button
              class="filtre-sekme"
              :class="{ aktif: filtre === 'BUGUN' }"
              @click="filtre = 'BUGUN'"
            >
              {{ t('teslimatlar.bugun') }}
            </button>
            <button
              class="filtre-sekme"
              :class="{ aktif: filtre === 'GECIKEN' }"
              @click="filtre = 'GECIKEN'"
            >
              {{ t('teslimatlar.geciken') }}
            </button>
          </div>

          <div
            v-if="teslimatYukleniyor"
            class="bos"
          >
            {{ t('common.loading') }}
          </div>
          <div
            v-else-if="!teslimatlar.length"
            class="bos"
          >
            {{ t('teslimatlar.teslimatYok') }}
          </div>
          <div
            v-else-if="!filtreliTeslimatlar.length"
            class="bos"
          >
            {{ t('teslimatlar.filtreBos') }}
          </div>

          <div
            v-for="teslim in filtreliTeslimatlar"
            :key="teslim.id"
            class="teslimat-kart"
          >
            <div class="teslimat-ust">
              <span class="fatura-no">
                <i class="pi pi-file" /> {{ teslim.faturaNumarasi ? '#' + teslim.faturaNumarasi : t('teslimatlar.faturaNo', { id: teslim.faturaId }) }}
              </span>
              <span
                v-if="teslim.musteriAdi"
                class="musteri"
              >{{ teslim.musteriAdi }}</span>
              <Tag
                :value="durumAdi(teslim.durum)"
                :severity="durumSeverity(teslim.durum)"
              />
              <Tag
                v-if="teslim.gecikti"
                :value="t('teslimatlar.gecikti')"
                severity="danger"
              />
            </div>
            <div class="adres">
              <i class="pi pi-map-marker" />
              <span>{{ teslim.teslimatAdresi || '—' }}</span>
            </div>
            <div
              v-if="teslim.beklenenTeslimTarihi"
              class="beklenen"
            >
              <i class="pi pi-calendar" /> {{ t('teslimatlar.beklenenTeslim') }}: {{ formatTarih(teslim.beklenenTeslimTarihi) }}
            </div>
            <div
              v-if="teslim.notlar"
              class="not"
            >
              <i class="pi pi-comment" /> {{ teslim.notlar }}
            </div>
            <div
              v-if="teslim.siparisId"
              class="beklenen"
            >
              <i class="pi pi-receipt" /> {{ t('teslimatlar.siparisEtiketi') }} #{{ teslim.siparisId }}
            </div>
            <button
              type="button"
              class="gecmis-toggle"
              @click="gecmisToggle(teslim)"
            >
              <i class="pi pi-history" /> {{ t('teslimatlar.gecmis') }}
            </button>
            <div
              v-if="gecmisAcik[teslim.id]"
              class="gecmis-liste"
            >
              <div
                v-if="!(gecmisler[teslim.id] || []).length"
                class="gecmis-bos"
              >
                {{ t('teslimatlar.gecmisYok') }}
              </div>
              <div
                v-for="(g, i) in gecmisler[teslim.id]"
                :key="i"
                class="gecmis-satir"
              >
                <span class="g-nokta" />
                <span class="g-durum">{{ durumAdi(g.yeniDurum) }}</span>
                <span class="g-tarih">{{ formatTarih(g.olusturmaTarihi) }}</span>
              </div>
            </div>
            <div class="teslimat-aksiyonlar">
              <div class="foto-alan">
                <img
                  v-if="teslim.teslimatFoto"
                  :src="teslim.teslimatFoto"
                  class="teslimat-foto"
                  :alt="t('teslimatlar.teslimatFotografi')"
                >
                <label class="foto-yukle">
                  <i class="pi pi-camera" /> {{ t('teslimatlar.foto') }}
                  <input
                    type="file"
                    accept="image/*"
                    hidden
                    @change="(e) => fotoYukle(teslim, e)"
                  >
                </label>
              </div>
              <a
                class="yol-tarifi"
                :href="yolTarifiUrl(teslim)"
                target="_blank"
                rel="noopener"
              >
                <i class="pi pi-directions" /> {{ t('teslimatlar.yolTarifiAl') }}
              </a>
              <button
                v-if="teslim.durum !== 'TESLIM_EDILDI'"
                type="button"
                class="durum-btn imzala"
                @click="teslimModalAc(teslim)"
              >
                <i class="pi pi-pencil" /> {{ t('teslimatlar.teslimEtImzala') }}
              </button>
              <div
                v-if="teslim.durum === 'TESLIM_EDILDI'"
                class="teslim-bilgi"
              >
                <span
                  v-if="teslim.teslimImzaUrl"
                  class="teslim-imza-kutu"
                >
                  <img
                    :src="teslim.teslimImzaUrl"
                    class="teslim-imza"
                    :alt="t('teslimatlar.imza')"
                  >
                </span>
                <span class="teslim-alan">
                  <i class="pi pi-user" /> {{ teslim.teslimAlanAd || '—' }}
                </span>
                <button
                  type="button"
                  class="durum-btn fis"
                  @click="fisAc(teslim)"
                >
                  <i class="pi pi-file-pdf" /> {{ t('teslimatlar.teslimatFisi') }}
                </button>
                <button
                  type="button"
                  class="durum-btn paylas"
                  @click="teslimPaylas(teslim)"
                >
                  <i class="pi pi-whatsapp" /> {{ t('teslimatlar.paylas') }}
                </button>
              </div>
              <Dropdown
                :model-value="teslim.durum"
                :options="durumSecenekleri"
                option-label="label"
                option-value="value"
                class="durum-dropdown"
                @update:model-value="(d) => durumGuncelle(teslim, d)"
              />
              <div class="hizli-durum">
                <button
                  v-if="teslim.durum !== 'YOLDA' && teslim.durum !== 'TESLIM_EDILDI'"
                  type="button"
                  class="durum-btn yolda"
                  @click="durumGuncelle(teslim, 'YOLDA')"
                >
                  <i class="pi pi-truck" /> {{ t('teslimatlar.durumYolda') }}
                </button>
                <button
                  v-if="teslim.durum !== 'TESLIM_EDILDI'"
                  type="button"
                  class="durum-btn teslim"
                  @click="teslimModalAc(teslim)"
                >
                  <i class="pi pi-check" /> {{ t('teslimatlar.durumTeslimEdildi') }}
                </button>
              </div>
            </div>
          </div>
        </template>
      </div>
    </div>

    <Dialog
      v-model:visible="imzaModal"
      :modal="true"
      :header="t('teslimatlar.dijitalTeslimat')"
      :style="{ width: '94%', maxWidth: '480px' }"
    >
      <div class="imza-modal-icerik">
        <div class="imza-alan-grup">
          <label>{{ t('teslimatlar.teslimAlanZorunlu') }}</label>
          <InputText
            v-model="imzaForm.teslimAlanAd"
            :placeholder="t('teslimatlar.teslimAlanPlaceholder')"
            class="w-full"
          />
        </div>
        <div class="imza-alan-grup">
          <label>{{ t('teslimatlar.teslimNotu') }}</label>
          <Textarea
            v-model="imzaForm.teslimNotu"
            rows="2"
            :placeholder="t('teslimatlar.teslimNotuPlaceholder')"
            class="w-full"
          />
        </div>
        <div class="imza-alan-grup">
          <ImzaPad
            ref="imzaPadRef"
            :etiket="t('teslimatlar.dijitalImza')"
          />
        </div>
        <div class="imza-alan-grup imza-foto-satir">
          <label class="foto-sec-etiket">
            <i class="pi pi-camera" /> {{ t('teslimatlar.teslimFotografiOpsiyonel') }}
            <input
              type="file"
              accept="image/*"
              hidden
              @change="imzaFotoSec"
            >
          </label>
          <button
            type="button"
            class="konum-btn"
            @click="konumAl"
          >
            <i class="pi pi-map-marker" /> {{ t('teslimatlar.konumAl') }}
          </button>
        </div>
        <small
          v-if="imzaForm.teslimKonum"
          class="konum-bilgi"
        >
          <i class="pi pi-map-marker" /> {{ imzaForm.teslimKonum }}
        </small>
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          icon="pi pi-times"
          class="p-button-text"
          @click="imzaModal = false"
        />
        <Button
          :label="t('teslimatlar.teslimatiOnayla')"
          icon="pi pi-check"
          class="p-button-success"
          :loading="teslimEdiliyor"
          @click="teslimatiTamamla"
        />
      </template>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { teslimatAPI } from '../api/index.js'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useAuthStore } from '../stores/authStore.js'
import { useI18n } from 'vue-i18n'
import { formatTarih } from '../utils/format.js'
import ImzaPad from '../components/ImzaPad.vue'

const toastBildirim = useToastBildirim()
const authStore = useAuthStore()
const { t } = useI18n()

const suruculer = ref([])
const teslimatlar = ref([])
const seciliSurucu = ref(null)
const yukleniyor = ref(false)
const teslimatYukleniyor = ref(false)
const filtre = ref('TUMU')
const rehberAcik = ref(false)
const gecmisAcik = ref({})
const gecmisler = ref({})

// Dijital imza / teslim
const imzaModal = ref(false)
const imzaHedef = ref(null)
const imzaForm = ref({ teslimAlanAd: '', teslimNotu: '', teslimKonum: '' })
const teslimEdiliyor = ref(false)
const imzaPadRef = ref(null)
const imzaFoto = ref(null)

const soforMu = computed(() => authStore?.kullanici?.role === 'DRIVER')

const gecmisYukle = async (teslim) => {
  try {
    const r = await teslimatAPI.gecmis(teslim.id)
    gecmisler.value = { ...gecmisler.value, [teslim.id]: r.data || [] }
  } catch {
    gecmisler.value = { ...gecmisler.value, [teslim.id]: [] }
  }
}

const gecmisToggle = async (teslim) => {
  const acik = !gecmisAcik.value[teslim.id]
  gecmisAcik.value = { ...gecmisAcik.value, [teslim.id]: acik }
  if (acik && !gecmisler.value[teslim.id]) await gecmisYukle(teslim)
}

const bugunStr = () => {
  const bugun = new Date()
  const ay = String(bugun.getMonth() + 1).padStart(2, '0')
  const gun = String(bugun.getDate()).padStart(2, '0')
  return `${bugun.getFullYear()}-${ay}-${gun}`
}

const filtreliTeslimatlar = computed(() => {
  if (filtre.value === 'BUGUN') {
    return teslimatlar.value.filter((t) => t.beklenenTeslimTarihi === bugunStr())
  }
  if (filtre.value === 'GECIKEN') {
    return teslimatlar.value.filter((t) => t.gecikti)
  }
  return teslimatlar.value
})

const durumSecenekleri = computed(() => [
  { label: t('teslimatlar.durumBeklemede'), value: 'BEKLEMEDE' },
  { label: t('teslimatlar.durumYolda'), value: 'YOLDA' },
  { label: t('teslimatlar.durumTeslimEdildi'), value: 'TESLIM_EDILDI' },
  { label: t('teslimatlar.durumIptal'), value: 'IPTAL' }
])

const durumAdi = (d) => durumSecenekleri.value.find((x) => x.value === d)?.label || d || '—'

const durumSeverity = (d) => {
  const map = { BEKLEMEDE: 'warn', YOLDA: 'info', TESLIM_EDILDI: 'success', IPTAL: 'danger' }
  return map[d] || 'secondary'
}

const yolTarifiUrl = (teslim) =>
  `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(teslim.teslimatAdresi || '')}`

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
    } else if (suruculer.value.length === 1) {
      await surucuSec(suruculer.value[0])
    }
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('teslimatlar.hataSoforYukleme'))
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
    toastBildirim.hata(err?.response?.data?.message || t('teslimatlar.hataTeslimatYukleme'))
    teslimatlar.value = []
  } finally {
    teslimatYukleniyor.value = false
  }
}

const surucuSec = async (s) => {
  seciliSurucu.value = s
  await teslimatlarYukle(s.id)
}

const durumGuncelle = async (teslim, yeniDurum) => {
  try {
    await teslimatAPI.durumGuncelle(teslim.id, yeniDurum)
    teslim.durum = yeniDurum
    toastBildirim.basarili(t('teslimatlar.durumGuncellendi'))
    if (seciliSurucu.value) {
      await suruculeriYukle()
    }
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('teslimatlar.hataDurumGuncelleme'))
  }
}

const fotoYukle = async (teslim, event) => {
  const dosya = event.target.files?.[0]
  if (!dosya) return
  try {
    const res = await teslimatAPI.fotoYukle(teslim.id, dosya)
    teslim.teslimatFoto = res.data?.teslimatFoto || teslim.teslimatFoto
    toastBildirim.basarili(t('teslimatlar.fotografYuklendi'))
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('teslimatlar.fotografYuklenemedi'))
  } finally {
    event.target.value = ''
  }
}

const teslimModalAc = (teslim) => {
  imzaHedef.value = teslim
  imzaForm.value = { teslimAlanAd: '', teslimNotu: '', teslimKonum: '' }
  imzaFoto.value = null
  imzaModal.value = true
  nextTick(() => imzaPadRef.value?.hazirla())
}

const imzaFotoSec = (event) => {
  imzaFoto.value = event.target.files?.[0] || null
}

const konumAl = () => {
  if (!navigator.geolocation) return
  navigator.geolocation.getCurrentPosition(
    (pos) => {
      imzaForm.value.teslimKonum = `${pos.coords.latitude.toFixed(5)}, ${pos.coords.longitude.toFixed(5)}`
    },
    () => {},
    { enableHighAccuracy: true, timeout: 10000 }
  )
}

const teslimatiTamamla = async () => {
  if (!imzaHedef.value?.id) return
  if (!imzaForm.value.teslimAlanAd?.trim()) {
    toastBildirim.uyari(t('teslimatlar.teslimAlanZorunlu'))
    return
  }
  if (imzaPadRef.value?.bosMu?.() !== false) {
    toastBildirim.uyari(t('teslimatlar.imzaZorunlu'))
    return
  }
  teslimEdiliyor.value = true
  try {
    const blob = await imzaPadRef.value.toBlob()
    if (!blob) {
      toastBildirim.uyari(t('teslimatlar.imzaZorunlu'))
      return
    }
    const dosya = new File([blob], `imza-${imzaHedef.value.id}.png`, { type: 'image/png' })
    const res = await teslimatAPI.teslimEt(imzaHedef.value.id, imzaForm.value, dosya)
    // Teslim fotoğrafı varsa ayrıca yükle (opsiyonel).
    if (imzaFoto.value) {
      try {
        await teslimatAPI.fotoYukle(imzaHedef.value.id, imzaFoto.value)
      } catch {
        /* fotoğraf yüklenemedi; teslim yine de tamam */
      }
    }
    const guncel = res.data
    const idx = teslimatlar.value.findIndex((x) => x.id === guncel.id)
    if (idx >= 0) teslimatlar.value[idx] = guncel
    toastBildirim.basarili(t('teslimatlar.teslimEdildi'))
    imzaModal.value = false
    await suruculeriYukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('teslimatlar.teslimEdilemedi'))
  } finally {
    teslimEdiliyor.value = false
  }
}

const fisAc = (teslim) => {
  window.open(`/api/deliveries/${teslim.id}/fis`, '_blank')
}

const teslimPaylas = (teslim) => {
  const satirlar = [
    t('teslimatlar.fisBaslik'),
    `${t('teslimatlar.faturaNo', { id: teslim.faturaId })}: ${teslim.faturaNumarasi || '#' + teslim.faturaId}`,
    `${t('teslimatlar.musteri')}: ${teslim.musteriAdi || '-'}`,
    `${t('teslimatlar.teslimAlanEtiket')}: ${teslim.teslimAlanAd || '-'}`,
    teslim.teslimTarihi ? `${t('teslimatlar.teslimTarihiEtiket')}: ${formatTarih(teslim.teslimTarihi)}` : null,
    `${t('teslimatlar.teslimEdenEtiket')}: ${teslim.teslimEdenAd || seciliSurucu.value?.ad || '-'}`
  ].filter(Boolean)
  window.open(`https://api.whatsapp.com/send?text=${encodeURIComponent(satirlar.join('\n'))}`, '_blank')
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
  background: var(--accent-soft);
  color: var(--accent, var(--accent));
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
.filtre-sekmeleri {
  display: flex;
  gap: 6px;
}
.filtre-sekme {
  padding: 5px 12px;
  border-radius: 999px;
  border: 1px solid var(--border);
  background: var(--bg-primary);
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
}
.filtre-sekme:hover {
  border-color: var(--accent, var(--accent));
  color: var(--accent, var(--accent));
}
.filtre-sekme.aktif {
  background: var(--accent-soft);
  border-color: var(--accent, var(--accent));
  color: var(--accent, var(--accent));
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
  color: var(--accent, var(--accent));
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
  background: var(--accent-soft);
  color: var(--accent, var(--accent));
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
  border-color: var(--accent, var(--accent));
  color: var(--accent, var(--accent));
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
.nasil-kart {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  overflow: hidden;
}
.nasil-baslik {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  border: none;
  background: transparent;
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  text-align: left;
}
.nasil-baslik i:last-child {
  margin-left: auto;
}
.nasil-icerik {
  padding: 0 16px 14px;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 10px;
}
.nasil-adim {
  display: flex;
  flex-direction: column;
  gap: 2px;
  font-size: 12px;
  color: var(--text-secondary);
}
.nasil-adim strong {
  color: var(--text-primary);
  font-size: 12.5px;
}
.gecmis-toggle {
  align-self: flex-start;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border-radius: 8px;
  border: 1px solid var(--border);
  background: var(--bg-primary);
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
}
.gecmis-toggle:hover {
  border-color: var(--accent, var(--accent));
  color: var(--accent, var(--accent));
}
.gecmis-liste {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 6px 0 0;
}
.gecmis-satir {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--text-secondary);
}
.g-nokta {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--accent, var(--accent));
  flex-shrink: 0;
}
.g-durum {
  font-weight: 600;
  color: var(--text-primary);
}
.g-tarih {
  margin-left: auto;
  color: var(--text-muted);
}
.gecmis-bos {
  font-size: 12px;
  color: var(--text-muted);
}
.hizli-durum {
  display: flex;
  gap: 8px;
}
.durum-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  border-radius: 8px;
  border: none;
  cursor: pointer;
  font-size: 13px;
  font-weight: 600;
  color: #fff;
}
.durum-btn.yolda {
  background: var(--accent);
}
.durum-btn.teslim {
  background: #10b981;
}
.durum-btn.imzala {
  background: #10b981;
}
.durum-btn.fis {
  background: #ef4444;
}
.durum-btn.paylas {
  background: #25d366;
}
.durum-btn:hover {
  filter: brightness(1.08);
}
.teslim-bilgi {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.teslim-imza-kutu {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  border: 1px solid var(--border);
  border-radius: 6px;
  padding: 2px 6px;
}
.teslim-imza {
  height: 34px;
  max-width: 120px;
  object-fit: contain;
}
.teslim-alan {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-primary);
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.imza-modal-icerik {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.imza-alan-grup label {
  display: block;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
  margin-bottom: 4px;
}
.imza-foto-satir {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.foto-sec-etiket,
.konum-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 8px;
  border: 1px solid var(--border);
  background: var(--bg-primary);
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
}
.foto-sec-etiket:hover,
.konum-btn:hover {
  border-color: var(--accent);
  color: var(--accent);
}
.konum-bilgi {
  font-size: 11px;
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
  .hizli-durum {
    width: 100%;
  }
  .durum-btn {
    flex: 1;
    justify-content: center;
    padding: 12px;
    font-size: 14px;
  }
}
</style>
