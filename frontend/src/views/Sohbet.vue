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
      <button
        class="oneri-cip oneri-ocr"
        @click="dosyaInput.click()"
      >
        <i class="pi pi-camera" /> Fatura/Fiş Oku
      </button>
      <input
        ref="dosyaInput"
        type="file"
        accept="image/*"
        hidden
        @change="faturaOku"
      >
    </div>

    <!-- OCR Yükleniyor -->
    <div
      v-if="ocrYukleniyor"
      class="ocr-bilgi"
    >
      <i class="pi pi-spin pi-spinner" /> Fatura okunuyor, yapay zeka analiz ediyor...
    </div>

    <!-- Ekip Modu: oda listesi + mesaj alanı -->
    <div
      v-if="aktifMod === 'ekip'"
      class="ekip-duzen"
    >
      <div class="oda-listesi">
        <div class="oda-listesi-baslik">
          <span>Sohbet Kanalları</span>
          <Button
            icon="pi pi-plus"
            class="p-button-text p-button-sm"
            title="Yeni Oda"
            @click="odaDialogAc = true"
          />
        </div>
        <button
          class="oda-ogesi"
          :class="{ aktif: seciliOdaId === null }"
          @click="genelSec"
        >
          <i class="pi pi-users" />
          <span>Genel Sohbet</span>
        </button>
        <div
          v-if="!odalar.length"
          class="oda-bos"
        >
          Henüz oda yok. "+" ile oluşturun.
        </div>
        <button
          v-for="o in odalar"
          :key="o.id"
          class="oda-ogesi"
          :class="{ aktif: seciliOdaId === o.id }"
          @click="odaSec(o)"
        >
          <i class="pi pi-hashtag" />
          <span class="oda-ad">{{ o.ad }}</span>
          <span
            v-if="o.uyeMi && o.okunmamisSayisi > 0 && seciliOdaId !== o.id"
            class="okunmamis-rozet"
          >{{ o.okunmamisSayisi }}</span>
          <i
            v-if="!o.uyeMi"
            class="pi pi-sign-in oda-katil"
            title="Katıl"
          />
        </button>
      </div>

      <div class="oda-icerik">
        <div
          v-if="seciliOdaId !== null && seciliOda"
          class="oda-ust"
        >
          <span class="oda-baslik">
            <i class="pi pi-hashtag" /> {{ seciliOda.ad }}
            <Tag
              :value="seciliOda.uyeSayisi + ' üye'"
              severity="info"
            />
          </span>
          <div class="oda-aksiyonlar">
            <Button
              label="Üyeler"
              icon="pi pi-users"
              class="p-button-sm p-button-outlined"
              @click="uyeYonetAc"
            />
            <Button
              v-if="seciliOda.uyeMi"
              label="Ayrıl"
              icon="pi pi-sign-out"
              class="p-button-sm p-button-text p-button-danger"
              @click="odadanAyril"
            />
          </div>
        </div>

        <div
          ref="mesajKutusu"
          class="mesaj-kutusu"
        >
          <div
            v-if="yukleniyor || odaYukleniyor"
            class="bos"
          >
            Yükleniyor...
          </div>
          <div
            v-else-if="seciliOdaId !== null && !seciliOda?.uyeMi"
            class="bos"
          >
            Mesajları görmek için odaya katılın.
          </div>
          <div
            v-else-if="!aktifMesajlar.length"
            class="bos"
          >
            Henüz mesaj yok. İlk mesajı siz yazın.
          </div>
          <div
            v-for="m in aktifMesajlar"
            :key="m.id"
            class="mesaj"
            :class="{ kendi: m.kullaniciId === kendiId }"
          >
            <div class="mesaj-ust">
              <strong>{{ m.kullaniciAd || 'Bilinmeyen' }}</strong>
              <span class="mesaj-zaman">{{ formatZaman(m.olusturmaTarihi) }}</span>
            </div>
            <div class="mesaj-icerik">
              <template v-if="m.dosyaUrl">
                <a
                  :href="m.dosyaUrl"
                  target="_blank"
                  rel="noopener"
                  class="mesaj-dosya"
                >
                  <img
                    v-if="resimMi(m.dosyaUrl)"
                    :src="m.dosyaUrl"
                    class="mesaj-gorsel"
                    alt="Paylaşılan görsel"
                  >
                  <span v-else><i class="pi pi-paperclip" /> {{ m.mesaj || 'Dosya' }}</span>
                </a>
              </template>
              <template v-if="m.mesaj">
                {{ m.mesaj }}
              </template>
            </div>
          </div>
        </div>

        <div
          v-if="yaziyorKullanici"
          class="yaziyor-gosterge"
        >
          {{ yaziyorKullanici }} yazıyor...
        </div>
        <div class="mesaj-giris">
          <Button
            icon="pi pi-paperclip"
            class="p-button-text"
            title="Dosya/Görsel Paylaş"
            @click="sohbetDosyaInput.click()"
          />
          <input
            ref="sohbetDosyaInput"
            type="file"
            hidden
            @change="sohbetDosyaYukle"
          >
          <InputText
            v-model="yeniMesaj"
            placeholder="Mesajınızı yazın..."
            class="mesaj-input"
            @keyup.enter="gonder"
            @input="yaziyorGonder"
          />
          <Button
            icon="pi pi-send"
            label="Gönder"
            :loading="gonderiliyor"
            @click="gonder"
          />
        </div>
      </div>
    </div>

    <!-- AI Modu -->
    <template v-else>
      <div
        ref="mesajKutusu"
        class="mesaj-kutusu"
      >
        <div
          v-if="(!aiMesajlar || !aiMesajlar.length)"
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
      </div>

      <div class="mesaj-giris">
        <InputText
          v-model="yeniMesaj"
          placeholder="Yapay zekaya bir soru sorun (Örn: Bu ay en çok ciro yapan 3 müşteri kim?)..."
          class="mesaj-input"
          @keyup.enter="gonder"
        />
        <Button
          icon="pi pi-sparkles"
          label="Sor"
          :loading="gonderiliyor || aiYukleniyor"
          @click="gonder"
        />
      </div>
    </template>

    <!-- Yeni Oda Dialog -->
    <Dialog
      v-model:visible="odaDialogAc"
      header="Yeni Sohbet Odası"
      :modal="true"
      :style="{ width: '440px' }"
    >
      <div class="ajanda-form">
        <div class="field">
          <label>Oda Adı *</label>
          <InputText
            v-model="odaForm.ad"
            placeholder="Örn: Satış Ekibi"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>Açıklama</label>
          <InputText
            v-model="odaForm.aciklama"
            placeholder="Kısa açıklama"
            class="w-full"
          />
        </div>
      </div>
      <template #footer>
        <Button
          label="İptal"
          class="p-button-text"
          @click="odaDialogAc = false"
        />
        <Button
          label="Oluştur"
          icon="pi pi-check"
          :loading="odaKaydediliyor"
          @click="odaOlustur"
        />
      </template>
    </Dialog>

    <!-- Üye Yönetim Dialog -->
    <Dialog
      v-model:visible="uyeDialogAc"
      header="Oda Üyeleri"
      :modal="true"
      :style="{ width: '440px' }"
    >
      <div class="uye-listesi">
        <div
          v-for="u in (seciliOda?.uyeler || [])"
          :key="u.kullaniciId"
          class="uye-satir"
        >
          <i class="pi pi-user" />
          <span>{{ u.kullaniciAd || ('#' + u.kullaniciId) }}</span>
          <button
            v-if="u.kullaniciId !== kendiId"
            class="uye-cikar"
            title="Çıkar"
            @click="uyeCikar(u.kullaniciId)"
          >
            <i class="pi pi-times" />
          </button>
        </div>
      </div>
      <div class="uye-ekle">
        <Dropdown
          v-model="eklenecekKullaniciId"
          :options="eklenecekKullanicilar"
          option-label="displayName"
          option-value="id"
          placeholder="Üye ekle..."
          class="uye-dropdown"
        />
        <Button
          icon="pi pi-plus"
          class="p-button-sm"
          title="Ekle"
          @click="uyeEkle"
        />
      </div>
      <template #footer>
        <Button
          label="Kapat"
          class="p-button-text"
          @click="uyeDialogAc = false"
        />
      </template>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useAuthStore } from '../stores/authStore.js'
import { sohbetAPI, sohbetOdaAPI, aiConfigAPI, kullaniciAPI } from '../api/index.js'
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
  'Kasa ve banka toplam bakiyemiz nedir?',
  'Kritik seviyede stoklarım hangileri?',
  'En kârlı ürünlerim hangileri?'
]

const mesajlar = ref([])
const odaMesajlar = ref([])
const odalar = ref([])
const seciliOdaId = ref(null)
const aiMesajlar = ref([])
const yeniMesaj = ref('')
const gonderiliyor = ref(false)
const aiYukleniyor = ref(false)
const ocrYukleniyor = ref(false)
const yukleniyor = ref(false)
const odaYukleniyor = ref(false)
const bagli = ref(false)
const mesajKutusu = ref(null)
const dosyaInput = ref(null)
const sohbetDosyaInput = ref(null)
const yaziyorKullanici = ref('')

const odaDialogAc = ref(false)
const odaKaydediliyor = ref(false)
const odaForm = ref({ ad: '', aciklama: '' })
const uyeDialogAc = ref(false)
const kullanicilar = ref([])
const eklenecekKullaniciId = ref(null)

const kendiId = computed(() => authStore?.kullanici?.id)

const seciliOda = computed(() => odalar.value.find((o) => o.id === seciliOdaId.value) || null)
const aktifMesajlar = computed(() => (seciliOdaId.value ? odaMesajlar.value : mesajlar.value))
const eklenecekKullanicilar = computed(() => {
  const mevcut = new Set((seciliOda.value?.uyeler || []).map((u) => u.kullaniciId))
  return kullanicilar.value.filter((k) => !mevcut.has(k.id))
})

let stompClient = null
let subscription = null
let odaSubscription = null
let yaziyorSubscription = null
let yaziyorZamanlayici = null
let yaziyorGizlemeZamanlayici = null

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
    bakiye: 'Bakiye',
    stok: 'Ürün',
    miktar: 'Miktar',
    durum: 'Durum',
    maliyet: 'Maliyet',
    satis: 'Satış Fiyatı',
    marj: 'Kâr Marjı'
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

// AI yanıtını SSE (akış) ile getirir; başarısız olursa klasik uca geri düşer.
const aiStreamGonder = async (metin) => {
  aiMesajlar.value.push({ rol: 'user', metin, zaman: new Date() })
  yeniMesaj.value = ''
  const aiMesaj = { rol: 'ai', metin: '', zaman: new Date() }
  aiMesajlar.value.push(aiMesaj)
  kaydir()
  aiYukleniyor.value = true

  try {
    let token = ''
    try {
      token = authStore.token || ''
    } catch {
      /* empty */
    }
    const base = import.meta.env.VITE_API_BASE_URL || '/api'
    const url = `${base}/sohbet/ai-sorgu-stream?` + new URLSearchParams({ soru: metin }).toString()
    const res = await fetch(url, {
      headers: token ? { Authorization: `Bearer ${token}` } : {},
      credentials: 'include'
    })
    if (!res.ok || !res.body) throw new Error('akış yok')

    const okuyucu = res.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    let bosYanit = true
    for (;;) {
      const { done, value } = await okuyucu.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      let idx
      while ((idx = buffer.indexOf('\n\n')) >= 0) {
        const event = buffer.slice(0, idx)
        buffer = buffer.slice(idx + 2)
        const dataLines = event.split('\n').filter((l) => l.startsWith('data:'))
        if (dataLines.length) {
          const payload = dataLines.map((l) => l.slice(5).trim()).join('\n')
          if (payload) {
            try {
              aiMesaj.metin += JSON.parse(payload)
            } catch {
              aiMesaj.metin += payload
            }
            bosYanit = false
            kaydir()
          }
        }
      }
    }
    if (bosYanit) {
      await aiYanitGeriDus(aiMesaj, metin)
    }
  } catch {
    await aiYanitGeriDus(aiMesaj, metin)
  } finally {
    aiYukleniyor.value = false
    kaydir()
  }
}

const aiYanitGeriDus = async (aiMesaj, metin) => {
  try {
    const res2 = await sohbetAPI.aiSorgu(metin)
    const data = res2.data
    aiMesaj.metin = data.cevapMetni
    aiMesaj.grafikTipi = data.grafikTipi
    aiMesaj.grafikVerisi = data.grafikVerisi
    aiMesaj.tabloVerisi = data.tabloVerisi
  } catch {
    if (!aiMesaj.metin) {
      aiMesaj.metin = 'Üzgünüm, sorunuzu işlerken bir hata oluştu. Lütfen tekrar deneyin.'
    }
  }
}

const faturaOku = async (event) => {
  const dosya = event.target.files?.[0]
  if (!dosya) return
  ocrYukleniyor.value = true
  try {
    const base64 = await dosyaBase64(dosya)
    const res = await sohbetAPI.aiOcr(base64, dosya.type || 'image/jpeg')
    const sonuc = res.data?.sonuc || ''
    aiMesajlar.value.push({
      rol: 'ai',
      metin: '📄 Okunan fatura bilgisi:\n' + sonuc,
      zaman: new Date()
    })
    kaydir()
  } catch (err) {
    const mesaj = err?.response?.data?.message || 'Fatura okunamadı. AI yapılandırmasını kontrol edin.'
    aiMesajlar.value.push({
      rol: 'ai',
      metin: mesaj,
      zaman: new Date()
    })
    kaydir()
  } finally {
    ocrYukleniyor.value = false
    if (event.target) event.target.value = ''
  }
}

const dosyaBase64 = (dosya) =>
  new Promise((resolve, reject) => {
    const okuyucu = new FileReader()
    okuyucu.onload = () => {
      const sonuc = okuyucu.result || ''
      const virgul = sonuc.indexOf(',')
      resolve(virgul >= 0 ? sonuc.substring(virgul + 1) : sonuc)
    }
    okuyucu.onerror = reject
    okuyucu.readAsDataURL(dosya)
  })

const gonder = async () => {
  const metin = yeniMesaj.value.trim()
  if (!metin) return

  if (aktifMod.value === 'ai') {
    aiStreamGonder(metin)
    return
  }

  // Ekip Sohbeti
  gonderiliyor.value = true
  try {
    if (seciliOdaId.value) {
      await sohbetOdaAPI.mesajGonder(seciliOdaId.value, { mesaj: metin })
    } else {
      await sohbetAPI.gonder({ mesaj: metin })
    }
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

const odalariYukle = async () => {
  try {
    const r = await sohbetOdaAPI.odalar()
    odalar.value = r.data || []
  } catch {
    odalar.value = []
  }
}

const kullanicilariYukle = async () => {
  try {
    const r = await kullaniciAPI.getAll()
    const tumu = r.data?.content || r.data || []
    kullanicilar.value = tumu.filter((k) => k.sirketId === authStore.sirketId)
  } catch {
    kullanicilar.value = []
  }
}

const genelSec = () => {
  seciliOdaId.value = null
  odaAboneligiYenile()
  yukle()
}

const odaSec = async (o) => {
  if (!o.uyeMi) {
    try {
      await sohbetOdaAPI.katil(o.id)
      await odalariYukle()
    } catch (err) {
      toastBildirim.hata(err?.response?.data?.message || 'Odaya katılınamadı')
      return
    }
  }
  seciliOdaId.value = o.id
  odaAboneligiYenile()
  odaMesajlariYukle(o.id)
  o.okunmamisSayisi = 0
  sohbetOdaAPI.okundu(o.id).catch(() => {})
}

const odaMesajlariYukle = async (odaId) => {
  odaYukleniyor.value = true
  try {
    const r = await sohbetOdaAPI.mesajlar(odaId)
    odaMesajlar.value = r.data || []
    kaydir()
  } catch {
    odaMesajlar.value = []
  }
  odaYukleniyor.value = false
}

const odaOlustur = async () => {
  if (!odaForm.value.ad.trim()) {
    toastBildirim.uyari('Oda adı zorunludur')
    return
  }
  odaKaydediliyor.value = true
  try {
    const r = await sohbetOdaAPI.olustur({ ad: odaForm.value.ad, aciklama: odaForm.value.aciklama })
    odalar.value.push(r.data)
    odaDialogAc.value = false
    odaForm.value = { ad: '', aciklama: '' }
    seciliOdaId.value = r.data.id
    odaAboneligiYenile()
    odaMesajlariYukle(r.data.id)
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || 'Oda oluşturulamadı')
  } finally {
    odaKaydediliyor.value = false
  }
}

const odadanAyril = async () => {
  if (!seciliOdaId.value) return
  try {
    await sohbetOdaAPI.ayril(seciliOdaId.value)
    toastBildirim.basarili('Odadan ayrıldınız')
    seciliOdaId.value = null
    await odalariYukle()
    odaAboneligiYenile()
    yukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || 'İşlem başarısız')
  }
}

const uyeYonetAc = async () => {
  uyeDialogAc.value = true
  kullanicilariYukle()
}

const uyeEkle = async () => {
  if (!eklenecekKullaniciId.value || !seciliOdaId.value) return
  try {
    await sohbetOdaAPI.uyeEkle(seciliOdaId.value, eklenecekKullaniciId.value)
    eklenecekKullaniciId.value = null
    await odalariYukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || 'Üye eklenemedi')
  }
}

const uyeCikar = async (kullaniciId) => {
  if (!seciliOdaId.value) return
  try {
    await sohbetOdaAPI.uyeCikar(seciliOdaId.value, kullaniciId)
    await odalariYukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || 'Üye çıkarılamadı')
  }
}

const baglan = () => {
  const SOCKET_URL = import.meta.env.VITE_WS_URL || window.location.origin + '/ws'
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
            odaAboneligiYenile()
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

const odaAboneligiYenile = () => {
  if (odaSubscription) {
    odaSubscription.unsubscribe()
    odaSubscription = null
  }
  if (yaziyorSubscription) {
    yaziyorSubscription.unsubscribe()
    yaziyorSubscription = null
  }
  yaziyorKullanici.value = ''
  if (stompClient && stompClient.connected && seciliOdaId.value) {
    const sirketId = authStore.sirketId
    odaSubscription = stompClient.subscribe(`/topic/sohbet/oda/${sirketId}/${seciliOdaId.value}`, (msg) => {
      try {
        const m = JSON.parse(msg.body)
        if (!odaMesajlar.value.some((x) => x.id === m.id)) {
          odaMesajlar.value.push(m)
          kaydir()
        }
      } catch {
        /* empty */
      }
    })
    yaziyorSubscription = stompClient.subscribe(`/topic/sohbet/oda/${sirketId}/${seciliOdaId.value}/yaziyor`, (msg) => {
      try {
        const y = JSON.parse(msg.body)
        if (y.kullaniciAd && y.kullaniciAd !== authStore?.kullanici?.displayName) {
          yaziyorKullanici.value = y.kullaniciAd
          if (yaziyorGizlemeZamanlayici) clearTimeout(yaziyorGizlemeZamanlayici)
          yaziyorGizlemeZamanlayici = setTimeout(() => {
            yaziyorKullanici.value = ''
          }, 2500)
        }
      } catch {
        /* empty */
      }
    })
  }
}

const yaziyorGonder = () => {
  if (!stompClient || !stompClient.connected || !seciliOdaId.value) return
  if (yaziyorZamanlayici) clearTimeout(yaziyorZamanlayici)
  yaziyorZamanlayici = setTimeout(() => {
    stompClient.publish({
      destination: '/app/sohbet/oda/yaziyor',
      body: JSON.stringify({
        sirketId: authStore.sirketId,
        odaId: seciliOdaId.value,
        kullaniciAd: authStore?.kullanici?.displayName
      })
    })
  }, 400)
}

const resimMi = (url) => /\.(png|jpe?g|gif|webp|bmp)$/i.test(url || '')

const sohbetDosyaYukle = async (event) => {
  const dosya = event.target.files?.[0]
  if (!dosya || !seciliOdaId.value) return
  gonderiliyor.value = true
  try {
    const res = await sohbetOdaAPI.dosyaYukle(seciliOdaId.value, dosya)
    const url = res.data?.url
    if (url) {
      await sohbetOdaAPI.mesajGonder(seciliOdaId.value, { mesaj: dosya.name, dosyaUrl: url })
    }
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || 'Dosya yüklenemedi')
  } finally {
    gonderiliyor.value = false
    event.target.value = ''
  }
}

watch(seciliOdaId, () => {
  odaAboneligiYenile()
})

onMounted(async () => {
  yukle()
  odalariYukle()
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
  if (odaSubscription) odaSubscription.unsubscribe()
  if (yaziyorSubscription) yaziyorSubscription.unsubscribe()
  if (yaziyorZamanlayici) clearTimeout(yaziyorZamanlayici)
  if (yaziyorGizlemeZamanlayici) clearTimeout(yaziyorGizlemeZamanlayici)
  if (stompClient) stompClient.deactivate()
})
</script>

<style scoped>
.sohbet-sayfasi {
  display: flex;
  flex-direction: column;
  height: calc(100dvh - 140px);
  max-height: calc(100dvh - 140px);
  min-height: 360px;
  padding: 0;
  max-width: 100%;
}
@media (max-width: 900px) {
  .sohbet-sayfasi {
    height: calc(100dvh - 200px);
    max-height: calc(100dvh - 200px);
  }
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
.oneri-cip.oneri-ocr {
  background: rgba(139, 92, 246, 0.12);
  border-color: rgba(139, 92, 246, 0.35);
  color: #a78bfa;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.oneri-cip.oneri-ocr:hover {
  background: rgba(139, 92, 246, 0.22);
  border-color: #8b5cf6;
  color: #c4b5fd;
}
.ocr-bilgi {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  margin-bottom: 10px;
  background: rgba(139, 92, 246, 0.1);
  border: 1px solid rgba(139, 92, 246, 0.3);
  border-radius: 10px;
  font-size: 13px;
  color: #a78bfa;
}
.ekip-duzen {
  flex: 1;
  display: flex;
  gap: 12px;
  min-height: 0;
}
.oda-listesi {
  width: 220px;
  min-width: 200px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 10px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.oda-listesi-baslik {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  font-weight: 700;
  color: var(--text-muted);
  padding: 0 4px 6px;
}
.oda-ogesi {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 10px;
  border-radius: 8px;
  border: none;
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
  font-size: 13px;
  text-align: left;
  width: 100%;
}
.oda-ogesi:hover {
  background: var(--bg-primary);
}
.oda-ogesi.aktif {
  background: rgba(59, 130, 246, 0.12);
  color: var(--accent, #3b82f6);
  font-weight: 600;
}
.oda-ad {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.oda-katil {
  font-size: 12px;
  opacity: 0.7;
}
.okunmamis-rozet {
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  border-radius: 10px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 700;
  background: var(--accent, #3b82f6);
  color: #fff;
}
.oda-bos {
  font-size: 12px;
  color: var(--text-muted);
  padding: 10px;
  text-align: center;
}
.oda-icerik {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-width: 0;
}
.oda-ust {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}
.oda-baslik {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
  font-size: 14px;
}
.oda-aksiyonlar {
  display: flex;
  align-items: center;
  gap: 8px;
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
}
.mesaj-input {
  flex: 1;
}
.yaziyor-gosterge {
  font-size: 12px;
  color: var(--text-muted);
  padding: 2px 4px;
  min-height: 18px;
}
.mesaj-dosya {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--accent, #3b82f6);
  text-decoration: none;
}
.mesaj-gorsel {
  max-width: 220px;
  max-height: 220px;
  border-radius: 10px;
  display: block;
  margin: 4px 0;
}
.ajanda-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.ajanda-form .field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.ajanda-form .field label {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
}
.ajanda-form .w-full {
  width: 100%;
}
.uye-listesi {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 14px;
  max-height: 260px;
  overflow-y: auto;
}
.uye-satir {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border: 1px solid var(--border);
  border-radius: 8px;
  font-size: 13px;
}
.uye-satir span {
  flex: 1;
}
.uye-cikar {
  background: transparent;
  border: none;
  color: var(--text-muted);
  cursor: pointer;
  font-size: 13px;
  border-radius: 4px;
  padding: 2px 6px;
}
.uye-cikar:hover {
  color: #ef4444;
  background: rgba(239, 68, 68, 0.1);
}
.uye-ekle {
  display: flex;
  gap: 8px;
  align-items: center;
}
.uye-dropdown {
  flex: 1;
}
</style>
