<template>
  <div class="sohbet-sayfasi">
    <div class="sohbet-baslik">
      <h1>
        <i
          :class="aktifMod === 'ai' ? 'pi pi-sparkles' : 'pi pi-comments'"
          style="margin-right: 8px; color: #3b82f6"
        />
        {{ aktifMod === 'ai' ? t('sohbet.aiAsistan') : t('sohbet.ekipSohbeti') }}
      </h1>
      <div class="header-sag">
        <SelectButton
          v-if="aiErisimVar"
          v-model="aktifMod"
          :options="modSecenekleri"
          option-label="label"
          option-value="value"
          size="small"
        />
        <Tag
          v-if="aktifMod === 'ai' && aiYapilandirildi"
          :value="t('sohbet.gercekAi')"
          severity="success"
        />
        <Tag
          v-else-if="aktifMod === 'ai'"
          :value="t('sohbet.kuralTabanliAi')"
          severity="info"
        />
        <span
          v-if="aktifMod === 'ekip' && bagli"
          class="bagli-durum"
        ><i class="pi pi-circle-on" /> {{ t('sohbet.bagli') }}</span>
        <span
          v-else-if="aktifMod === 'ekip'"
          class="bagli-durum bagli-degil"
        ><i class="pi pi-circle-off" /> {{ t('sohbet.baglaniyor') }}</span>
      </div>
    </div>

    <!-- AI Asistan Hızlı Sorular -->
    <div
      v-if="aktifMod === 'ai'"
      class="ai-oneriler"
    >
      <span class="oneri-baslik"><i class="pi pi-bolt" /> {{ t('sohbet.hizliSorular') }}</span>
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
        <i class="pi pi-camera" /> {{ t('sohbet.faturaFisOku') }}
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
      <i class="pi pi-spin pi-spinner" /> {{ t('sohbet.ocrAnaliz') }}
    </div>

    <!-- Ekip Modu: oda listesi + mesaj alanı -->
    <div
      v-if="aktifMod === 'ekip'"
      class="ekip-duzen"
    >
      <nav
        class="oda-listesi"
        :aria-label="t('sohbet.sohbetKanallari')"
      >
        <div class="oda-listesi-baslik">
          <span>{{ t('sohbet.sohbetKanallari') }}</span>
          <Button
            icon="pi pi-plus"
            class="p-button-text p-button-sm"
            :title="t('sohbet.yeniOda')"
            :aria-label="t('sohbet.yeniOda')"
            @click="odadanOlusturAc"
          />
        </div>
        <button
          class="oda-ogesi"
          :class="{ aktif: seciliOdaId === null }"
          :aria-current="seciliOdaId === null ? 'true' : undefined"
          @click="genelSec"
        >
          <i class="pi pi-users" />
          <span>{{ t('sohbet.genelSohbet') }}</span>
          <span
            v-if="genelOkunmamis > 0 && seciliOdaId !== null"
            class="okunmamis-rozet"
          >{{ genelOkunmamis }}</span>
        </button>
        <div
          v-if="!odalar.length"
          class="oda-bos"
        >
          {{ t('sohbet.odaYok') }}
        </div>
        <button
          v-for="o in odalar"
          :key="o.id"
          class="oda-ogesi"
          :class="{ aktif: seciliOdaId === o.id }"
          :aria-current="seciliOdaId === o.id ? 'true' : undefined"
          @click="odaSec(o)"
        >
          <i :class="o.ozel ? 'pi pi-lock' : 'pi pi-hashtag'" />
          <span class="oda-ad">{{ o.ad }}</span>
          <span
            v-if="o.uyeMi && o.okunmamisSayisi > 0 && seciliOdaId !== o.id"
            class="okunmamis-rozet"
          >{{ o.okunmamisSayisi }}</span>
          <i
            v-if="!o.uyeMi"
            class="pi pi-sign-in oda-katil"
            :title="t('sohbet.katil')"
          />
        </button>
      </nav>

      <div class="oda-icerik">
        <div
          v-if="seciliOdaId !== null && seciliOda"
          class="oda-ust"
        >
          <span class="oda-baslik">
            <i class="pi pi-hashtag" /> {{ seciliOda.ad }}
            <Tag
              :value="t('sohbet.uyeSayisi', { n: seciliOda.uyeSayisi })"
              severity="info"
            />
          </span>
          <div class="oda-aksiyonlar">
            <Button
              v-if="authStore.isAdmin"
              :label="t('sohbet.uyeler')"
              icon="pi pi-users"
              class="p-button-sm p-button-outlined"
              @click="uyeYonetAc"
            />
            <Button
              v-if="seciliOda.uyeMi"
              :label="t('sohbet.ayril')"
              icon="pi pi-sign-out"
              class="p-button-sm p-button-text p-button-danger"
              @click="odadanAyril"
            />
            <Button
              v-if="odaYoneticisiMi(seciliOda)"
              icon="pi pi-pencil"
              class="p-button-sm p-button-text"
              :title="t('sohbet.odaDuzenle')"
              :aria-label="t('sohbet.odaDuzenle')"
              @click="odaDuzenleAc"
            />
            <Button
              v-if="odaYoneticisiMi(seciliOda)"
              icon="pi pi-trash"
              class="p-button-sm p-button-text p-button-danger"
              :title="t('sohbet.odaSil')"
              :aria-label="t('sohbet.odaSil')"
              @click="odayiSil"
            />
          </div>
        </div>

        <div
          ref="mesajKutusu"
          class="mesaj-kutusu"
          role="log"
          aria-live="polite"
          aria-relevant="additions"
          :aria-label="seciliOdaId ? t('sohbet.odaMesajlari') : t('sohbet.genelSohbet')"
        >
          <div
            v-if="yukleniyor || odaYukleniyor"
            class="bos"
          >
            {{ t('common.loading') }}
          </div>
          <div
            v-else-if="seciliOdaId !== null && !seciliOda?.uyeMi"
            class="bos"
          >
            {{ t('sohbet.odayaKatilin') }}
          </div>
          <div
            v-else-if="!aktifMesajlar.length"
            class="bos"
          >
            {{ t('sohbet.mesajYok') }}
          </div>
          <div
            v-if="dahaVar"
            class="daha-eski"
          >
            <Button
              :label="t('sohbet.dahaEski')"
              icon="pi pi-angle-up"
              class="p-button-sm p-button-text"
              :loading="eskiYukleniyor"
              @click="dahaEskiYukle"
            />
          </div>
          <div
            v-for="m in aktifMesajlar"
            :key="m.id"
            class="mesaj"
            :class="{ kendi: m.kullaniciId === kendiId }"
          >
            <div class="mesaj-ust">
              <strong>{{ m.kullaniciAd || t('sohbet.bilinmeyen') }}</strong>
              <span class="mesaj-zaman">{{ formatZaman(m.olusturmaTarihi) }}</span>
              <Button
                v-if="adminMi && m.id"
                icon="pi pi-trash"
                class="p-button-text p-button-rounded p-button-sm mesaj-sil-btn"
                :title="t('sohbet.mesajSil')"
                :aria-label="t('sohbet.mesajSil')"
                @click="mesajSil(m)"
              />
            </div>
            <div class="mesaj-icerik">
              <template v-if="guvenliDosyaUrl(m.dosyaUrl)">
                <a
                  :href="guvenliDosyaUrl(m.dosyaUrl)"
                  target="_blank"
                  rel="noopener noreferrer"
                  class="mesaj-dosya"
                >
                  <img
                    v-if="resimMi(m.dosyaUrl)"
                    :src="guvenliDosyaUrl(m.dosyaUrl)"
                    class="mesaj-gorsel"
                    :alt="t('sohbet.paylasilanGorsel')"
                  >
                  <span v-else><i class="pi pi-paperclip" /> {{ m.mesaj || t('sohbet.dosya') }}</span>
                </a>
              </template>
              <template v-if="m.mesaj">
                {{ m.mesaj }}
              </template>
            </div>
          </div>
        </div>

        <div
          class="yaziyor-gosterge"
          aria-live="polite"
        >
          <span v-if="yaziyorKullanici">{{ yaziyorKullanici }} {{ t('sohbet.yaziyor') }}</span>
        </div>
        <div class="mesaj-giris">
          <Button
            icon="pi pi-paperclip"
            class="p-button-text"
            :title="t('sohbet.dosyaGorselPaylas')"
            :aria-label="t('sohbet.dosyaGorselPaylas')"
            :disabled="gonderiliyor"
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
            :placeholder="t('sohbet.mesajPlaceholder')"
            class="mesaj-input"
            maxlength="4000"
            :disabled="gonderiliyor"
            @keyup.enter="gonder"
            @input="yaziyorGonder"
          />
          <Button
            icon="pi pi-send"
            :label="t('sohbet.gonder')"
            :loading="gonderiliyor"
            :disabled="gonderiliyor || !yeniMesaj.trim()"
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
          <h3>{{ t('sohbet.aiBaslik') }}</h3>
          <p>{{ t('sohbet.aiAciklama') }}</p>
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
              {{ m.rol === 'user' ? t('sohbet.siz') : t('sohbet.raspelAi') }}
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
              v-if="m.grafikTipi && m.grafikTipi !== 'none' && m.grafikVerisi"
              class="ai-grafik"
            >
              <component
                :is="grafikBileseni(m.grafikTipi)"
                :data="m.grafikVerisi"
                :options="aiGrafikOptions"
              />
            </div>

            <div
              v-else-if="m.grafikVerisi && m.grafikVerisi.labels"
              class="ai-rozet-grid"
            >
              <div
                v-for="(lbl, lIdx) in m.grafikVerisi.labels"
                :key="lIdx"
                class="ai-rozet"
              >
                <span>{{ lbl }}</span>
                <strong>{{ m.grafikVerisi.datasets?.[0]?.data?.[lIdx] }}{{ rozetBirimi(m) }}</strong>
              </div>
            </div>
          </div>
        </div>
        <div
          v-if="aiYukleniyor"
          class="ai-loading"
        >
          <i class="pi pi-spin pi-spinner" /> {{ t('sohbet.aiAnaliz') }}
        </div>
      </div>

      <div class="mesaj-giris">
        <InputText
          v-model="yeniMesaj"
          :placeholder="t('sohbet.aiPlaceholder')"
          class="mesaj-input"
          maxlength="4000"
          :disabled="aiYukleniyor"
          @keyup.enter="gonder"
        />
        <Button
          icon="pi pi-sparkles"
          :label="t('sohbet.sor')"
          :loading="gonderiliyor || aiYukleniyor"
          :disabled="aiYukleniyor || !yeniMesaj.trim()"
          @click="gonder"
        />
      </div>
    </template>

    <!-- Yeni Oda Dialog -->
    <Dialog
      v-model:visible="odaDialogAc"
      :header="t('sohbet.yeniOdaBaslik')"
      :modal="true"
      :style="{ width: '440px' }"
    >
      <div class="ajanda-form">
        <div class="field">
          <label>{{ t('sohbet.odaAdiZorunlu') }}</label>
          <InputText
            v-model="odaForm.ad"
            :placeholder="t('sohbet.odaAdiPlaceholder')"
            maxlength="100"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('common.description') }}</label>
          <InputText
            v-model="odaForm.aciklama"
            :placeholder="t('sohbet.aciklamaPlaceholder')"
            maxlength="300"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('sohbet.uyelerSec') }}</label>
          <MultiSelect
            v-model="odaForm.uyeIdleri"
            :options="kullanicilar"
            option-label="displayName"
            option-value="id"
            filter
            display="chip"
            :placeholder="t('sohbet.uyeEkle')"
            class="w-full"
          />
        </div>
        <div class="field ozel-kanal-alan">
          <InputSwitch v-model="odaForm.ozel" />
          <div>
            <label class="ozel-kanal-etiket">{{ t('sohbet.ozelKanal') }}</label>
            <small>{{ t('sohbet.ozelKanalAciklama') }}</small>
          </div>
        </div>
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          class="p-button-text"
          @click="odaDialogAc = false"
        />
        <Button
          :label="t('sohbet.olustur')"
          icon="pi pi-check"
          :loading="odaKaydediliyor"
          @click="odaOlustur"
        />
      </template>
    </Dialog>

    <!-- Oda Düzenle Dialog -->
    <Dialog
      v-model:visible="duzenleDialogAc"
      :header="t('sohbet.odaDuzenle')"
      :modal="true"
      :style="{ width: '440px' }"
    >
      <div class="ajanda-form">
        <div class="field">
          <label>{{ t('sohbet.odaAdiZorunlu') }}</label>
          <InputText
            v-model="duzenleForm.ad"
            maxlength="100"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('common.description') }}</label>
          <InputText
            v-model="duzenleForm.aciklama"
            maxlength="300"
            class="w-full"
          />
        </div>
        <div class="field ozel-kanal-alan">
          <InputSwitch v-model="duzenleForm.ozel" />
          <div>
            <label class="ozel-kanal-etiket">{{ t('sohbet.ozelKanal') }}</label>
            <small>{{ t('sohbet.ozelKanalAciklama') }}</small>
          </div>
        </div>
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          class="p-button-text"
          @click="duzenleDialogAc = false"
        />
        <Button
          :label="t('common.save')"
          icon="pi pi-check"
          :loading="duzenleKaydediliyor"
          @click="odaDuzenleKaydet"
        />
      </template>
    </Dialog>

    <!-- Üye Yönetim Dialog -->
    <Dialog
      v-model:visible="uyeDialogAc"
      :header="t('sohbet.odaUyeleri')"
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
            :title="t('sohbet.cikar')"
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
          filter
          :placeholder="t('sohbet.uyeEkle')"
          class="uye-dropdown"
        />
        <Button
          icon="pi pi-plus"
          class="p-button-sm"
          :title="t('sohbet.ekle')"
          :aria-label="t('sohbet.ekle')"
          @click="uyeEkle"
        />
      </div>
      <template #footer>
        <Button
          :label="t('common.close')"
          class="p-button-text"
          @click="uyeDialogAc = false"
        />
      </template>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { unwrapList } from '../api/utils/unwrap.js'
import { useAuthStore } from '../stores/authStore.js'
import { sohbetAPI, sohbetOdaAPI, aiConfigAPI, kullaniciAPI } from '../api/index.js'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useConfirm } from 'primevue/useconfirm'
import { useI18n } from 'vue-i18n'
import { Bar, Doughnut, Line } from 'vue-chartjs'
import {
  Chart as ChartJS,
  Title,
  Tooltip,
  Legend,
  BarElement,
  CategoryScale,
  LinearScale,
  ArcElement,
  PointElement,
  LineElement
} from 'chart.js'

ChartJS.register(Title, Tooltip, Legend, BarElement, CategoryScale, LinearScale, ArcElement, PointElement, LineElement)

const aiGrafikOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: { legend: { position: 'bottom' } }
}
const grafikBileseni = (tip) => ({ bar: Bar, doughnut: Doughnut, line: Line }[tip] || Bar)

const authStore = useAuthStore()
const toastBildirim = useToastBildirim()
const confirm = useConfirm()
const { t, locale } = useI18n()

// Faz 8: AI finansal verileri okuyabildigi icin DRIVER rolune kapali.
// (Backend'de de ayni uc @PreAuthorize ile korunuyor.)
const kullaniciRolu = computed(() => String(authStore?.kullanici?.role || '').toUpperCase())
const aiErisimVar = computed(() => ['ADMIN', 'USER', 'MUHASEBE'].includes(kullaniciRolu.value))
const aktifMod = ref(aiErisimVar.value ? 'ai' : 'ekip')
const aiYapilandirildi = ref(false)
const modSecenekleri = computed(() =>
  aiErisimVar.value
    ? [
        { label: t('sohbet.aiAsistan'), value: 'ai' },
        { label: t('sohbet.ekipSohbeti'), value: 'ekip' }
      ]
    : [{ label: t('sohbet.ekipSohbeti'), value: 'ekip' }]
)

const hizliSorular = computed(() => [
  t('sohbet.soruCiro'),
  t('sohbet.soruVade'),
  t('sohbet.soruBakiye'),
  t('sohbet.soruKritikStok'),
  t('sohbet.soruKarliUrun')
])

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
// Faz 3.3: oda mesajlarında cursor tabanlı "daha eski yükle".
const mesajCursor = ref(null)
const dahaVar = ref(false)
const eskiYukleniyor = ref(false)

const odaDialogAc = ref(false)
const odaKaydediliyor = ref(false)
const odaForm = ref({ ad: '', aciklama: '', ozel: false, uyeIdleri: [] })
const uyeDialogAc = ref(false)
const kullanicilar = ref([])
const eklenecekKullaniciId = ref(null)

const kendiId = computed(() => authStore?.kullanici?.id)
const adminMi = computed(() => String(authStore?.kullanici?.role || '').toUpperCase() === 'ADMIN')

const seciliOda = computed(() => odalar.value.find((o) => o.id === seciliOdaId.value) || null)
const aktifMesajlar = computed(() => (seciliOdaId.value ? odaMesajlar.value : mesajlar.value))
const eklenecekKullanicilar = computed(() => {
  const mevcut = new Set((seciliOda.value?.uyeler || []).map((u) => u.kullaniciId))
  return kullanicilar.value.filter((k) => !mevcut.has(k.id))
})

let stompClient = null
let subscription = null
let silSubscription = null
let rozetSubscription = null
let odaSilBildirimSubscription = null
let odaSubscription = null
let odaSilSubscription = null
let yaziyorSubscription = null
let yaziyorZamanlayici = null
let yaziyorGizlemeZamanlayici = null

const formatZaman = (t) => {
  if (!t) return ''
  const d = new Date(t)
  if (isNaN(d.getTime())) return ''
  // Faz 8: tarih de arayuz diline uyar (onceden sabit tr-TR idi).
  return d.toLocaleString(locale.value === 'en' ? 'en-GB' : 'tr-TR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

/** Yerel listeye mesajı (id'ye göre tekrarı önleyerek) ekler ve kaydırır. */
const mesajEkle = (hedefRef, m, zorla = false) => {
  if (!hedefRef || !hedefRef.value || !m) return
  if (m.id != null && hedefRef.value.some((x) => x.id === m.id)) return
  hedefRef.value.push(m)
  kaydir(zorla)
}

/** Yalnızca ADMIN mesaj silebilir. Genel sohbet veya oda mesajı silinir. */
const mesajSil = (m) => {
  if (!m?.id) return
  confirm.require({
    message: t('common.silmeOnayMesaji'),
    header: t('common.silmeOnayi'),
    icon: 'pi pi-exclamation-triangle',
    rejectProps: { label: t('common.vazgec'), severity: 'secondary', outlined: true, size: 'small' },
    acceptProps: { label: t('common.evetSil'), severity: 'danger', size: 'small' },
    accept: async () => {
      try {
        if (seciliOdaId.value) {
          await sohbetOdaAPI.mesajSil(seciliOdaId.value, m.id)
          odaMesajlar.value = odaMesajlar.value.filter((x) => x.id !== m.id)
        } else {
          await sohbetAPI.mesajSil(m.id)
          mesajlar.value = mesajlar.value.filter((x) => x.id !== m.id)
        }
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || t('sohbet.mesajSilinemedi'))
      }
    }
  })
}

const formatTabloBaslik = (key) => {  const map = {
    sira: '#',
    musteri: t('sohbet.musteri'),
    ciro: t('sohbet.toplamCiro'),
    faturaNo: t('sohbet.faturaNo'),
    cari: t('sohbet.cariHesap'),
    vade: t('sohbet.vadeTarihi'),
    tur: t('sohbet.islemTuru'),
    tutar: t('common.amount'),
    hesap: t('sohbet.hesapAdi'),
    bakiye: t('sohbet.bakiye'),
    stok: t('sohbet.urun'),
    miktar: t('sohbet.miktar'),
    durum: t('common.status'),
    maliyet: t('sohbet.maliyet'),
    satis: t('sohbet.satisFiyati'),
    marj: t('sohbet.karMarji'),
    // Faz 8: backend `marjYuzde` gonderiyordu; baslikta ham anahtar gorunuyordu.
    marjYuzde: t('sohbet.karMarji')
  }
  return map[key] || key
}

/**
 * Rozet birimi. Onceden her deger `₺` ile gosteriliyordu; stok adetleri ve
 * yuzdeler de para birimi gibi gorunuyordu. Intent'e gore birim secilir.
 */
const rozetBirimi = (m) => {
  if (m?.intent === 'KARLILIK') return ' %'
  if (m?.intent === 'STOK_DURUM') return ''
  return ' ₺'
}

/**
 * Mesaj kutusunu kaydırır.
 *
 * Faz 8: Önce HER yeni mesajda dibe atılıyordu; kullanıcı geçmişi okurken
 * ekran zıplıyordu. Artık yalnızca kullanıcı zaten dipteyse (veya kendi
 * mesajını gönderdiyse) otomatik kaydırılır.
 */
const kaydir = (zorla = false) => {
  nextTick(() => {
    const el = mesajKutusu.value
    if (!el) return
    const dipteMi = el.scrollHeight - el.scrollTop - el.clientHeight < 80
    if (zorla || dipteMi) el.scrollTop = el.scrollHeight
  })
}

/** Rozet tazelemesini layout'a bildirir (sidebar 30 sn'lik yoklamayi beklemez). */
const rozetYenile = () => {
  window.dispatchEvent(new CustomEvent('raspel:sohbet-guncelle'))
}

const hizliSoruSor = (soru) => {
  if (aiYukleniyor.value) return
  yeniMesaj.value = soru
  gonder()
}

// Faz 4.2: AI yapılandırılmışsa token token (SSE) akıtır; başarısız olursa
// yapısal (kural tabanlı, grafik/tablo üreten) uca düşer. Kural tabanlı uçta
// LLM olmadığı için akış yalnızca AI yapılandırıldığında anlamlıdır.
const aiSseAkis = (metin, aiMesaj) =>
  new Promise((resolve) => {
    let alindi = false
    let bitti = false
    const base = import.meta.env.VITE_API_BASE_URL || '/api'
    const es = new EventSource(`${base}/sohbet/ai-sorgu-stream?soru=${encodeURIComponent(metin)}`)
    const kapat = (ok) => {
      if (bitti) return
      bitti = true
      es.close()
      resolve(ok)
    }
    es.addEventListener('token', (e) => {
      try {
        aiMesaj.metin += JSON.parse(e.data)
        alindi = true
        kaydir()
      } catch {
        /* tek token atlanır */
      }
    })
    // Sunucu hata olayı ve bağlantı kapanması aynı isimde gelir; token alındıysa başarı.
    es.onerror = () => kapat(alindi)
  })

const aiStreamGonder = async (metin) => {
  // Faz 8: paralel akislar cevaplari karistiriyordu.
  if (aiYukleniyor.value) return
  aiMesajlar.value.push({ rol: 'user', metin, zaman: new Date() })
  yeniMesaj.value = ''
  const aiMesaj = { rol: 'ai', metin: '', zaman: new Date() }
  aiMesajlar.value.push(aiMesaj)
  kaydir()
  aiYukleniyor.value = true

  if (aiYapilandirildi.value && typeof EventSource !== 'undefined') {
    const basarili = await aiSseAkis(metin, aiMesaj)
    if (basarili) {
      aiYukleniyor.value = false
      kaydir()
      return
    }
    // SSE başarısız: yapısal uca düşmek için metni sıfırla.
    aiMesaj.metin = ''
  }

  try {
    const res = await sohbetAPI.aiSorgu(metin)
    const data = res.data || {}
    aiMesaj.metin = data.cevapMetni || t('sohbet.hataAi')
    aiMesaj.grafikTipi = data.grafikTipi
    aiMesaj.grafikVerisi = data.grafikVerisi
    aiMesaj.tabloVerisi = data.tabloVerisi
    aiMesaj.intent = data.intent
  } catch {
    if (!aiMesaj.metin) aiMesaj.metin = t('sohbet.hataAi')
  } finally {
    aiYukleniyor.value = false
    kaydir()
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
      metin: '📄 ' + t('sohbet.okunanFatura') + '\n' + sonuc,
      zaman: new Date()
    })
    kaydir()
  } catch (err) {
    const mesaj = err?.response?.data?.message || t('sohbet.faturaOkunamadi')
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
    // Faz 8: AI istegi surerken ikinci istek baslatilmaz.
    if (aiYukleniyor.value) return
    aiStreamGonder(metin)
    return
  }

  // Faz 8: Enter'a iki kez basmak iki mesaj gonderiyordu (buton loading olsa
  // da tus isleyicisi korunmuyordu).
  if (gonderiliyor.value) return
  gonderiliyor.value = true
  try {
    let gonderilen
    if (seciliOdaId.value) {
      const r = await sohbetOdaAPI.mesajGonder(seciliOdaId.value, { mesaj: metin })
      gonderilen = r.data
    } else {
      const r = await sohbetAPI.gonder({ mesaj: metin })
      gonderilen = r.data
    }
    mesajEkle(seciliOdaId.value ? odaMesajlar : mesajlar, gonderilen, true)
    yeniMesaj.value = ''
    if (!seciliOdaId.value) genelOkunduIsaretle()
    rozetYenile()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('sohbet.mesajGonderilemedi'))
  } finally {
    gonderiliyor.value = false
  }
}

/** Genel sohbette okunan son mesajı SUNUCUYA bildirir (cihazdan bağımsız). */
const genelOkunmamis = ref(0)
let sonBildirilenOkumaId = 0
const genelRozetYukle = async () => {
  try {
    const r = await sohbetAPI.okunmamis()
    genelOkunmamis.value = Number(r.data?.okunmamis || 0)
  } catch {
    genelOkunmamis.value = 0
  }
}
const genelOkunduIsaretle = () => {
  const son = mesajlar.value[mesajlar.value.length - 1]
  if (son?.id == null) return
  genelOkunmamis.value = 0
  if (son.id <= sonBildirilenOkumaId) return
  sonBildirilenOkumaId = son.id
  sohbetAPI.okunduGenel(son.id).catch(() => {
    // Sunucuya yazılamazsa bir sonraki açılışta tekrar denenir.
    sonBildirilenOkumaId = 0
  })
  rozetYenile()
}

const yukle = async () => {
  yukleniyor.value = true
  try {
    const r = await sohbetAPI.sonMesajlar({ limit: MESAJ_SAYFA })
    mesajlar.value = r.data || []
    // Faz 8: genel sohbet de cursor ile sayfalanir.
    mesajCursor.value = mesajlar.value.length ? mesajlar.value[0].id : null
    dahaVar.value = mesajlar.value.length === MESAJ_SAYFA
    kaydir(true)
    genelOkunduIsaretle()
  } catch {
    mesajlar.value = []
    mesajCursor.value = null
    dahaVar.value = false
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
    const r = await kullaniciAPI.getAll({ size: 500 })
    const tumu = unwrapList(r)
    kullanicilar.value = tumu.filter((k) => k.sirketId === authStore.sirketId)
  } catch {
    kullanicilar.value = []
  }
}

const genelSec = () => {
  seciliOdaId.value = null
  dahaVar.value = false
  eskiYukleniyor.value = false
  odaAboneligiYenile()
  yukle()
}

const odaSec = async (o) => {
  if (!o.uyeMi) {
    try {
      await sohbetOdaAPI.katil(o.id)
      await odalariYukle()
    } catch (err) {
      toastBildirim.hata(err?.response?.data?.message || t('sohbet.odayaKatilnamadi'))
      return
    }
  }
  seciliOdaId.value = o.id
  odaAboneligiYenile()
  odaMesajlariYukle(o.id)
  o.okunmamisSayisi = 0
  sohbetOdaAPI.okundu(o.id).catch(() => {})
  genelRozetYukle()
  rozetYenile()
}

const MESAJ_SAYFA = 50

const odaMesajlariYukle = async (odaId) => {
  odaYukleniyor.value = true
  mesajCursor.value = null
  dahaVar.value = false
  try {
    const r = await sohbetOdaAPI.mesajlar(odaId, { limit: MESAJ_SAYFA })
    const liste = r.data || []
    odaMesajlar.value = liste
    // Dönen liste eskiden yeniye sıralıdır; en eski (ilk) mesajın id'si cursordur.
    mesajCursor.value = liste.length ? liste[0].id : null
    dahaVar.value = liste.length === MESAJ_SAYFA
    kaydir()
  } catch {
    odaMesajlar.value = []
  }
  odaYukleniyor.value = false
}

// Faz 3.3: cursor'dan önceki (daha eski) mesajları yükler ve başa ekler.
// Faz 8: genel sohbet için de çalışır; ekleme sonrası kaydırma konumu korunur.
const dahaEskiYukle = async () => {
  if (!dahaVar.value || !mesajCursor.value || eskiYukleniyor.value) return
  eskiYukleniyor.value = true
  const el = mesajKutusu.value
  const oncekiYukseklik = el?.scrollHeight || 0
  const oncekiUst = el?.scrollTop || 0
  try {
    const r = seciliOdaId.value
      ? await sohbetOdaAPI.mesajlar(seciliOdaId.value, { cursor: mesajCursor.value, limit: MESAJ_SAYFA })
      : await sohbetAPI.sonMesajlar({ cursor: mesajCursor.value, limit: MESAJ_SAYFA })
    const eski = r.data || []
    if (eski.length) {
      if (seciliOdaId.value) odaMesajlar.value = [...eski, ...odaMesajlar.value]
      else mesajlar.value = [...eski, ...mesajlar.value]
      mesajCursor.value = eski[0].id
    }
    dahaVar.value = eski.length === MESAJ_SAYFA
    // Başa ekleme görünümü aşağı kaydırır; kullanıcı aynı mesajda kalsın.
    nextTick(() => {
      if (el) el.scrollTop = oncekiUst + (el.scrollHeight - oncekiYukseklik)
    })
  } catch {
    /* sessiz: kullanıcı tekrar deneyebilir */
  } finally {
    eskiYukleniyor.value = false
  }
}

const odaOlustur = async () => {
  if (!odaForm.value.ad.trim()) {
    toastBildirim.uyari(t('sohbet.odaAdiZorunluUyari'))
    return
  }
  odaKaydediliyor.value = true
  try {
    const r = await sohbetOdaAPI.olustur({
      ad: odaForm.value.ad.trim(),
      aciklama: odaForm.value.aciklama,
      ozel: odaForm.value.ozel,
      uyeIdleri: odaForm.value.uyeIdleri
    })
    odalar.value.push(r.data)
    odaDialogAc.value = false
    odaForm.value = { ad: '', aciklama: '', ozel: false, uyeIdleri: [] }
    seciliOdaId.value = r.data.id
    odaAboneligiYenile()
    odaMesajlariYukle(r.data.id)
    rozetYenile()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('sohbet.odaOlusturulamadi'))
  } finally {
    odaKaydediliyor.value = false
  }
}

/** Oda yöneticisi mi? (global ADMIN, kurucu veya OWNER/ADMIN üyelik) */
const odaYoneticisiMi = (oda) => {
  if (!oda) return false
  if (adminMi.value) return true
  if (oda.olusturanKullaniciId === kendiId.value) return true
  return (oda.uyeler || []).some(
    (u) => u.kullaniciId === kendiId.value && ['OWNER', 'ADMIN'].includes(String(u.rol || '').toUpperCase())
  )
}

const odadanOlusturAc = async () => {
  odaDialogAc.value = true
  if (!kullanicilar.value.length) await kullanicilariYukle()
}

/**
 * Faz 8: onaysiz ayrilma tek tiklama ile kanali silebiliyordu. Kurucu ayrilirsa
 * yonetim en eski uyeye devredilir; son uye ayrilirsa oda silinir.
 */
const odadanAyril = () => {
  if (!seciliOdaId.value || !seciliOda.value) return
  const oda = seciliOda.value
  const kurucu = oda.olusturanKullaniciId === kendiId.value
  confirm.require({
    message: kurucu ? t('sohbet.ayrilKurucuOnay') : t('sohbet.ayrilOnay'),
    header: t('sohbet.ayrilOnayBaslik'),
    icon: 'pi pi-exclamation-triangle',
    rejectProps: { label: t('common.vazgec'), severity: 'secondary', outlined: true, size: 'small' },
    acceptProps: { label: t('sohbet.ayril'), severity: 'danger', size: 'small' },
    accept: async () => {
      try {
        await sohbetOdaAPI.ayril(seciliOdaId.value)
        toastBildirim.basarili(t('sohbet.odadanAyrildiniz'))
        seciliOdaId.value = null
        odaMesajlar.value = []
        await odalariYukle()
        odaAboneligiYenile()
        yukle()
        rozetYenile()
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || t('sohbet.islemBasarisiz'))
      }
    }
  })
}

/** Odayı (ve mesajlarını) siler; yalnızca yönetici. */
const odayiSil = () => {
  if (!seciliOdaId.value || !seciliOda.value) return
  const odaId = seciliOdaId.value
  confirm.require({
    message: t('sohbet.odaSilOnay', { ad: seciliOda.value.ad }),
    header: t('sohbet.odaSil'),
    icon: 'pi pi-exclamation-triangle',
    rejectProps: { label: t('common.vazgec'), severity: 'secondary', outlined: true, size: 'small' },
    acceptProps: { label: t('sohbet.odaSil'), severity: 'danger', size: 'small' },
    accept: async () => {
      try {
        await sohbetOdaAPI.sil(odaId)
        toastBildirim.basarili(t('sohbet.odaSilindi'))
        seciliOdaId.value = null
        odaMesajlar.value = []
        await odalariYukle()
        odaAboneligiYenile()
        yukle()
        rozetYenile()
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || t('sohbet.islemBasarisiz'))
      }
    }
  })
}

// --- Oda düzenleme (ad/açıklama/özel kanal) -------------------------------
const duzenleDialogAc = ref(false)
const duzenleKaydediliyor = ref(false)
const duzenleForm = ref({ ad: '', aciklama: '', ozel: false })

const odaDuzenleAc = () => {
  const oda = seciliOda.value
  if (!oda) return
  duzenleForm.value = {
    ad: oda.ad || '',
    aciklama: oda.aciklama || '',
    ozel: oda.ozel === true
  }
  duzenleDialogAc.value = true
}

const odaDuzenleKaydet = async () => {
  if (!seciliOdaId.value) return
  if (!duzenleForm.value.ad.trim()) {
    toastBildirim.uyari(t('sohbet.odaAdiZorunluUyari'))
    return
  }
  duzenleKaydediliyor.value = true
  try {
    const r = await sohbetOdaAPI.guncelle(seciliOdaId.value, {
      ad: duzenleForm.value.ad.trim(),
      aciklama: duzenleForm.value.aciklama,
      ozel: duzenleForm.value.ozel
    })
    const idx = odalar.value.findIndex((o) => o.id === r.data.id)
    if (idx >= 0) odalar.value[idx] = r.data
    duzenleDialogAc.value = false
    toastBildirim.basarili(t('sohbet.odaDuzenlendi'))
    rozetYenile()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('sohbet.islemBasarisiz'))
  } finally {
    duzenleKaydediliyor.value = false
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
    toastBildirim.hata(err?.response?.data?.message || t('sohbet.uyeEklenemedi'))
  }
}

const uyeCikar = async (kullaniciId) => {
  if (!seciliOdaId.value) return
  try {
    await sohbetOdaAPI.uyeCikar(seciliOdaId.value, kullaniciId)
    await odalariYukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('sohbet.uyeCikarilamadi'))
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
            globalAbonelikleriYenile()
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

/**
 * Şirket geneli abonelikler: genel sohbet, silme, rozet ve oda silme.
 * Yeniden bağlanmada eski abonelikler kapatılır (çift mesaj olmasın).
 */
const globalAbonelikleriYenile = () => {
  if (!stompClient || !stompClient.connected) return
  const sirketId = authStore.sirketId
  ;[subscription, silSubscription, rozetSubscription, odaSilBildirimSubscription].forEach((s) => s?.unsubscribe?.())
  subscription = stompClient.subscribe(`/topic/sohbet/${sirketId}`, (msg) => {
    try {
      const m = JSON.parse(msg.body)
      mesajEkle(mesajlar, m)
      // Genel sohbet ekrandaysa yeni mesaj okunmuş sayılır.
      if (seciliOdaId.value === null) genelOkunduIsaretle()
      else {
        genelRozetYukle()
        rozetYenile()
      }
    } catch {
      /* empty */
    }
  })
  silSubscription = stompClient.subscribe(`/topic/sohbet/${sirketId}/sil`, (msg) => {
    const id = Number(msg.body)
    mesajlar.value = mesajlar.value.filter((x) => x.id !== id)
  })
  // Faz 8: seçili olmayan odalarda gelen mesaj rozeti anında tazeler.
  rozetSubscription = stompClient.subscribe(`/topic/sohbet/${sirketId}/rozet`, (msg) => {
    try {
      const veri = JSON.parse(msg.body)
      if (veri?.odaId !== seciliOdaId.value) {
        odalariYukle()
        rozetYenile()
      }
    } catch {
      /* empty */
    }
  })
  // Faz 8: silinen oda listede kalmasın.
  odaSilBildirimSubscription = stompClient.subscribe(`/topic/sohbet/${sirketId}/oda-sil`, (msg) => {
    const odaId = Number(msg.body)
    odalar.value = odalar.value.filter((o) => o.id !== odaId)
    if (seciliOdaId.value === odaId) {
      seciliOdaId.value = null
      odaMesajlar.value = []
      odaAboneligiYenile()
      yukle()
    }
    rozetYenile()
  })
  odaAboneligiYenile()
}

const odaAboneligiYenile = () => {
  if (odaSubscription) {
    odaSubscription.unsubscribe()
    odaSubscription = null
  }
  if (odaSilSubscription) {
    odaSilSubscription.unsubscribe()
    odaSilSubscription = null
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
    odaSilSubscription = stompClient.subscribe(`/topic/sohbet/oda/${sirketId}/${seciliOdaId.value}/sil`, (msg) => {
      const id = Number(msg.body)
      odaMesajlar.value = odaMesajlar.value.filter((x) => x.id !== id)
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

/** XSS/gizlilik koruması: yalnızca kendi yükleme yolumuz ve blob URL'ler.
 *  Harici http(s) adresler (eski mesajlarda kalmış olabilir) ve `//evil.com`
 *  gibi protokol-göreli adresler RENDER EDİLMEZ; aksi halde oda üyelerinin
 *  tarayıcısı dış siteye istek atıyordu (IP/tracking sızıntısı). */
const guvenliDosyaUrl = (url) => {
  const u = String(url || '').trim()
  return u.startsWith('/api/uploads/') || u.startsWith('blob:') ? u : null
}

const sohbetDosyaYukle = async (event) => {
  const dosya = event.target.files?.[0]
  if (!dosya) return
  gonderiliyor.value = true
  try {
    let url
    if (seciliOdaId.value) {
      const res = await sohbetOdaAPI.dosyaYukle(seciliOdaId.value, dosya)
      url = res.data?.url
    } else {
      const res = await sohbetAPI.dosya(dosya)
      url = res.data?.url
    }
    if (!url) throw new Error(t('sohbet.dosyaYuklenemedi'))
    let gonderilen
    if (seciliOdaId.value) {
      const r = await sohbetOdaAPI.mesajGonder(seciliOdaId.value, { mesaj: dosya.name, dosyaUrl: url })
      gonderilen = r.data
    } else {
      const r = await sohbetAPI.gonder({ mesaj: dosya.name, dosyaUrl: url })
      gonderilen = r.data
    }
    mesajEkle(seciliOdaId.value ? odaMesajlar : mesajlar, gonderilen, true)
    if (!seciliOdaId.value) genelOkunduIsaretle()
    rozetYenile()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('sohbet.dosyaYuklenemedi'))
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
  genelRozetYukle()
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
  if (silSubscription) silSubscription.unsubscribe()
  if (rozetSubscription) rozetSubscription.unsubscribe()
  if (odaSilBildirimSubscription) odaSilBildirimSubscription.unsubscribe()
  if (odaSubscription) odaSubscription.unsubscribe()
  if (odaSilSubscription) odaSilSubscription.unsubscribe()
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
  height: calc(100dvh / var(--uyg-olcek) - 140px);
  max-height: calc(100dvh / var(--uyg-olcek) - 140px);
  min-height: 0;
  padding: 0;
  max-width: 100%;
}
@media (max-width: 900px) {
  .sohbet-sayfasi {
    height: calc(100dvh / var(--uyg-olcek) - 200px);
    max-height: calc(100dvh / var(--uyg-olcek) - 200px);
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
  color: var(--success);
  display: flex;
  align-items: center;
  gap: 4px;
}
.bagli-durum.bagli-degil {
  color: var(--warning);
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
  background: var(--accent-soft);
  border-color: var(--accent, var(--accent));
  color: var(--accent, var(--accent));
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
  background: var(--accent-soft);
  color: var(--accent, var(--accent));
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
  background: var(--accent, var(--accent));
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
  color: var(--accent);
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
  background: var(--accent-soft);
  border-color: var(--accent-border);
}
.mesaj.ai-cevap {
  background: var(--success-soft);
  border-color: var(--success-border);
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
.mesaj-sil-btn {
  margin-left: auto;
  width: 1.6rem;
  height: 1.6rem;
  color: var(--text-muted);
}
.mesaj-sil-btn:hover {
  color: var(--danger, #ef4444);
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
.ai-grafik {
  position: relative;
  height: 260px;
  margin-top: 12px;
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
  color: var(--success);
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
  display: flex;
  align-items: center;
  min-height: 20px;
  font-size: 12px;
  color: var(--text-muted);
  padding: 0 4px;
}
.mesaj-dosya {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--accent, var(--accent));
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
.ozel-kanal-alan {
  flex-direction: row !important;
  align-items: flex-start;
  gap: 10px !important;
}
.ozel-kanal-etiket {
  display: block;
  font-weight: 600;
}
.ozel-kanal-alan small {
  color: var(--text-muted);
  font-size: 12px;
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
  color: var(--danger);
  background: var(--danger-soft);
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
