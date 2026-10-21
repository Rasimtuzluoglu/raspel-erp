<template>
  <div class="personel-sayfasi">
    <PageHeader
      :title="t('personel.title')"
      :subtitle="t('personel.aciklama')"
      icon="pi pi-users"
    >
      <template #actions>
        <Button
          v-if="authStore.isAdmin"
          :label="t('personel.yeniPersonel')"
          icon="pi pi-plus"
          @click="personelDialogAc()"
        />
        <Button
          icon="pi pi-refresh"
          :aria-label="t('common.refresh')"
          class="p-button-text p-button-sm"
          :loading="yukleniyor"
          @click="yukle"
        />
      </template>
    </PageHeader>

    <IlkZiyaretIpuclari
      anahtar="personel"
      :baslik="t('personel.ipucuBaslik')"
      :metin="t('personel.ipucuMetin')"
    />

    <div class="kpi-satiri">
      <KpiKart
        :baslik="t('personel.kpiToplam')"
        :deger="ozet.toplam"
        ikon="pi pi-users"
        renk="#3b82f6"
        :para-birimi="false"
      />
      <KpiKart
        :baslik="t('personel.kpiAktif')"
        :deger="ozet.aktif"
        ikon="pi pi-check-circle"
        renk="#10b981"
        :para-birimi="false"
      />
      <KpiKart
        :baslik="t('personel.kpiPasif')"
        :deger="ozet.pasif"
        ikon="pi pi-user-minus"
        renk="#64748b"
        :para-birimi="false"
      />
      <KpiKart
        :baslik="t('personel.kpiBekleyenIzin')"
        :deger="ozet.bekleyenIzin"
        ikon="pi pi-calendar-clock"
        renk="#f59e0b"
        :para-birimi="false"
      />
    </div>

    <TabView v-model:active-index="aktifTab">
      <TabPanel :header="t('personel.personelListesi')">
        <div class="personel-arac">
          <span class="arac-arama">
            <i class="pi pi-search" />
            <InputText
              v-model="filtre.q"
              :placeholder="t('personel.aramaPlaceholder')"
              class="arama-input"
              @input="aramaGirdi"
            />
          </span>
          <Dropdown
            v-model="filtre.departman"
            :options="departmanlar"
            :placeholder="t('personel.departman')"
            show-clear
            class="arac-secim"
            @change="filtreDegisti"
          />
          <Dropdown
            v-model="filtre.rol"
            :options="rolSecenekleri"
            option-label="label"
            option-value="value"
            :placeholder="t('personel.rol')"
            show-clear
            class="arac-secim"
            @change="filtreDegisti"
          />
          <Dropdown
            v-model="filtre.durum"
            :options="durumSecenekleri"
            option-label="label"
            option-value="value"
            class="arac-secim"
            @change="filtreDegisti"
          />
          <span class="arac-bosluk" />
          <Button
            :label="t('personel.excelIndir')"
            icon="pi pi-file-excel"
            outlined
            size="small"
            :loading="excelYukleniyor"
            @click="excelIndir"
          />
        </div>

        <div
          class="personel-masa"
          :aria-busy="yukleniyor"
        >
          <AppDataTable
            :value="personeller"
            lazy
            :total-records="toplam"
            :rows="sayfaBoyutu"
            striped-rows
            :loading="yukleniyor"
            :empty-message="filtreVar ? t('personel.filtreBos') : t('personel.empty')"
            gorunum-anahtari="personel-personel"
            @page="sayfaDegisti"
            @sort="siralaDegisti"
          >
            <Column
              field="ad"
              :header="t('personel.ad')"
              sortable
            />
            <Column
              field="soyad"
              :header="t('personel.soyad')"
              sortable
            />
            <Column
              field="departman"
              :header="t('personel.departman')"
            />
            <Column
              field="pozisyon"
              :header="t('personel.pozisyon')"
            />
            <Column
              field="rol"
              :header="t('personel.rol')"
              style="width: 110px"
            >
              <template #body="s">
                <span
                  v-if="s.data.rol"
                  class="rol-badge"
                >{{ rolEtiket(s.data.rol) }}</span>
                <span v-else>-</span>
              </template>
            </Column>
            <Column
              field="telefon"
              :header="t('personel.telefon')"
            />
            <Column
              field="email"
              :header="t('personel.eposta')"
            />
            <Column
              field="aktif"
              :header="t('common.status')"
            >
              <template #body="{ data }">
                <Tag
                  :value="data.aktif ? t('personel.aktif') : t('personel.pasif')"
                  :severity="data.aktif ? 'success' : 'danger'"
                />
              </template>
            </Column>
            <Column
              :header="t('personel.islem')"
              style="width: 72px"
            >
              <template #body="{ data }">
                <SatirEylemleri
                  :gorunur="{ duzenle: false, cogalt: false, sil: false }"
                  :items="personelAksiyonlari(data)"
                  :baslik="`${data.ad} ${data.soyad}`"
                />
              </template>
            </Column>
            <template #empty>
              <EmptyState
                v-if="!yukleniyor && personeller.length === 0"
                :message="filtreVar ? t('personel.filtreBos') : t('personel.empty')"
                :sub-message="filtreVar ? '' : t('personel.emptyHint')"
                icon="pi pi-users"
                :action-label="filtreVar || !authStore.isAdmin ? '' : t('personel.yeniPersonel')"
                action-icon="pi pi-plus"
                @action="personelDialogAc()"
              />
            </template>
          </AppDataTable>
        </div>

        <!-- Mobil kart görünümü -->
        <div
          class="personel-mobil"
          :aria-busy="yukleniyor"
        >
          <span
            class="sr-only"
            role="status"
            aria-live="polite"
          >{{ t('personel.sonucSayisi', { sayi: toplam }) }}</span>
          <div
            v-if="yukleniyor && !personeller.length"
            class="personel-skeleton"
            role="status"
            :aria-label="t('common.loading')"
          >
            <SkeletonLoader
              :count="4"
              height="104px"
            />
          </div>
          <EmptyState
            v-else-if="!personeller.length"
            :message="filtreVar ? t('personel.filtreBos') : t('personel.empty')"
            icon="pi pi-users"
          />
          <article
            v-for="p in personeller"
            :key="p.id"
            class="personel-mobil-kart"
          >
            <div class="pmk-ust">
              <button
                type="button"
                class="pmk-ad"
                :title="t('personel.detay')"
                @click="detayAc(p)"
              >
                <i class="pi pi-user" />
                <span>{{ p.ad }} {{ p.soyad }}</span>
              </button>
              <SatirEylemleri
                :gorunur="{ duzenle: false, cogalt: false, sil: false }"
                :items="personelAksiyonlari(p)"
                :baslik="`${p.ad} ${p.soyad}`"
              />
            </div>
            <div class="pmk-satir">
              <Tag
                :value="p.aktif ? t('personel.aktif') : t('personel.pasif')"
                :severity="p.aktif ? 'success' : 'danger'"
              />
              <span
                v-if="p.rol"
                class="rol-badge"
              >{{ rolEtiket(p.rol) }}</span>
              <span
                v-if="p.departman"
                class="pmk-alt"
              >{{ p.departman }}</span>
            </div>
            <div
              v-if="p.pozisyon"
              class="pmk-satir pmk-alt"
            >
              {{ p.pozisyon }}
            </div>
            <div class="pmk-satir pmk-iletisim">
              <span v-if="p.telefon"><i class="pi pi-phone" /> {{ p.telefon }}</span>
              <span v-if="p.email"><i class="pi pi-envelope" /> {{ p.email }}</span>
            </div>
          </article>
        </div>
      </TabPanel>

      <TabPanel :header="t('personel.izinTalepleri')">
        <div class="personel-arac">
          <span class="arac-bosluk" />
          <Button
            :label="t('personel.izinlereGit')"
            icon="pi pi-arrow-right"
            icon-pos="right"
            outlined
            size="small"
            @click="router.push('/izinler')"
          />
        </div>
        <AppDataTable
          :value="tumIzinler"
          striped-rows
          :paginator="false"
        >
          <Column
            field="personelAdi"
            :header="t('personel.personel')"
          />
          <Column
            field="izinTuru"
            :header="t('personel.izinTuru')"
          >
            <template #body="{ data }">
              {{ izinTuruEtiket(data.izinTuru) }}
            </template>
          </Column>
          <Column
            field="baslangic"
            :header="t('personel.baslangic')"
          />
          <Column
            field="bitis"
            :header="t('personel.bitis')"
          />
          <Column
            field="gunSayisi"
            :header="t('personel.gun')"
          />
          <Column
            field="durum"
            :header="t('common.status')"
          >
            <template #body="{ data }">
              <Tag
                :value="durumEtiket(data.durum)"
                :severity="data.durum === 'ONAYLANDI' ? 'success' : data.durum === 'REDDEDILDI' ? 'danger' : 'warn'"
              />
            </template>
          </Column>
        </AppDataTable>
      </TabPanel>
    </TabView>

    <PersonelDetayDrawer
      v-model:visible="detayDrawer"
      :detay="detay"
      :yukleniyor="detayYukleniyor"
      :duzenlenebilir="authStore.isAdmin"
      @duzenle="drawerDuzenle"
      @izin-ekle="drawerIzinEkle"
    />

    <Dialog
      v-model:visible="personelDialog"
      :header="duzenleme ? t('personel.personelDuzenle') : t('personel.yeniPersonel')"
      modal
      :style="{ width: '720px' }"
    >
      <div class="form-grid">
        <div class="field-row">
          <div class="field">
            <label for="personel-ad">{{ t('personel.adZorunlu') }}</label><InputText
              id="personel-ad"
              v-model="personelForm.ad"
              class="w-full"
              required
              aria-required="true"
              maxlength="50"
            />
          </div>
          <div class="field">
            <label for="personel-soyad">{{ t('personel.soyadZorunlu') }}</label><InputText
              id="personel-soyad"
              v-model="personelForm.soyad"
              class="w-full"
              required
              aria-required="true"
              maxlength="50"
            />
          </div>
        </div>
        <div class="field-row">
          <div class="field">
            <label for="personel-tc">{{ t('personel.tcKimlik') }}</label><InputText
              id="personel-tc"
              v-model="personelForm.tcKimlik"
              class="w-full"
              inputmode="numeric"
              maxlength="11"
            />
            <span
              v-if="(personelForm.tcKimlik || '').replace(/\D/g, '').length === 11"
              :style="{ color: tcGecerli ? '#22c55e' : '#ef4444', fontSize: '12px' }"
            >
              {{ tcGecerli ? t('personel.gecerliTC') : t('personel.gecersizTC') }}
            </span>
          </div>
          <div class="field">
            <label>{{ t('personel.dogumTarihi') }}</label><DatePicker
              v-model="personelForm.dogumTarihi"
              date-format="dd.mm.yy"
              class="w-full"
            />
          </div>
        </div>
        <div class="field-row">
          <div class="field">
            <label>{{ t('personel.departman') }}</label><InputText
              v-model="personelForm.departman"
              class="w-full"
            />
          </div>
          <div class="field">
            <label>{{ t('personel.pozisyon') }}</label><InputText
              v-model="personelForm.pozisyon"
              class="w-full"
            />
          </div>
        </div>
        <div class="field-row">
          <div class="field">
            <label>{{ t('personel.rol') }}</label>
            <Dropdown
              v-model="personelForm.rol"
              :options="rolSecenekleri"
              option-label="label"
              option-value="value"
              show-clear
              class="w-full"
            />
          </div>
          <div
            v-if="authStore.isAdmin"
            class="field"
          >
            <label>{{ t('personel.kullaniciHesabi') }}</label>
            <Dropdown
              v-model="personelForm.kullaniciId"
              :options="kullanicilar"
              option-label="displayName"
              option-value="id"
              show-clear
              filter
              :placeholder="personelForm.rol === 'SOFOR' ? t('personel.kullaniciSec') : t('personel.kullaniciOpsiyonel')"
              class="w-full"
            />
          </div>
        </div>
        <div class="field-row">
          <div
            v-if="canSalary"
            class="field"
          >
            <label>{{ t('personel.maas') }}</label><InputNumber
              v-model="personelForm.maas"
              mode="currency"
              currency="TRY"
              :min="0"
              class="w-full"
            />
          </div>
          <div class="field">
            <label>{{ t('personel.iseGiris') }}</label><DatePicker
              v-model="personelForm.iseGirisTarihi"
              date-format="dd.mm.yy"
              class="w-full"
            />
          </div>
          <div class="field">
            <label>{{ t('personel.cikisTarihi') }}</label><DatePicker
              v-model="personelForm.cikisTarihi"
              date-format="dd.mm.yy"
              show-button-bar
              class="w-full"
            />
          </div>
        </div>
        <div class="field-row">
          <div class="field">
            <label>{{ t('personel.telefon') }}</label><InputText
              v-model="personelForm.telefon"
              class="w-full"
            />
          </div>
          <div class="field">
            <label>{{ t('personel.eposta') }}</label><InputText
              v-model="personelForm.email"
              class="w-full"
            />
          </div>
        </div>
        <div class="field">
          <label>{{ t('personel.adres') }}</label><Textarea
            v-model="personelForm.adres"
            rows="2"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('personel.aktif') }}</label><InputSwitch v-model="personelForm.aktif" />
        </div>
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          icon="pi pi-times"
          class="p-button-text"
          @click="personelDialog = false"
        />
        <Button
          :label="t('common.save')"
          icon="pi pi-check"
          :loading="kaydediliyor"
          @click="personelKaydet"
        />
      </template>
    </Dialog>

    <Dialog
      v-model:visible="izinDialog"
      :header="t('personel.izinEkle')"
      modal
      :style="{ width: '440px' }"
    >
      <div class="form-grid">
        <div class="field">
          <label>{{ t('personel.personel') }}</label><InputText
            :value="izinPersonelAdi"
            disabled
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('personel.izinTuruZorunlu') }}</label>
          <Dropdown
            v-model="izinForm.izinTuru"
            :options="izinTuruSecenekleri"
            option-label="label"
            option-value="value"
            :placeholder="t('common.select')"
            class="w-full"
          />
        </div>
        <div class="field-row">
          <div class="field">
            <label>{{ t('personel.baslangic') }}</label><DatePicker
              v-model="izinForm.baslangic"
              date-format="dd.mm.yy"
              class="w-full"
            />
          </div>
          <div class="field">
            <label>{{ t('personel.bitis') }}</label><DatePicker
              v-model="izinForm.bitis"
              date-format="dd.mm.yy"
              class="w-full"
            />
          </div>
        </div>
        <div class="field">
          <label>{{ t('common.description') }}</label><Textarea
            v-model="izinForm.aciklama"
            rows="2"
            class="w-full"
          />
        </div>
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          icon="pi pi-times"
          class="p-button-text"
          @click="izinDialog = false"
        />
        <Button
          :label="t('common.save')"
          icon="pi pi-check"
          :loading="kaydediliyor"
          @click="izinKaydet"
        />
      </template>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { unwrapList } from '../api/utils/unwrap.js'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useAuthStore } from '../stores/authStore.js'
import { useConfirm } from 'primevue/useconfirm'
import { useFormKorumasi } from '../composables/useFormKorumasi.js'
import { personelAPI, personelIzinAPI, excelAPI, kullaniciAPI } from '../api/index.js'
import EmptyState from '../components/EmptyState.vue'
import IlkZiyaretIpuclari from '../components/IlkZiyaretIpuclari.vue'
import PageHeader from '../components/PageHeader.vue'
import KpiKart from '../components/KpiKart.vue'
import SatirEylemleri from '../components/SatirEylemleri.vue'
import PersonelDetayDrawer from '../components/PersonelDetayDrawer.vue'
import SkeletonLoader from '../components/SkeletonLoader.vue'
import { useI18n } from 'vue-i18n'
import { getLocalDateString } from '../utils/format.js'
const toastBildirim = useToastBildirim()
const confirm = useConfirm()
const authStore = useAuthStore()
const router = useRouter()
const { t } = useI18n()

const SAYFA_ANAHTAR = 'raspel_personel_rows'
const TAB_ANAHTAR = 'raspel_personel_tab'

const aktifTab = ref(Number(localStorage.getItem(TAB_ANAHTAR)) || 0)
watch(aktifTab, (v) => localStorage.setItem(TAB_ANAHTAR, String(v)))

const personeller = ref([])
const tumIzinler = ref([])
const yukleniyor = ref(false)
const kaydediliyor = ref(false)
const excelYukleniyor = ref(false)
const personelDialog = ref(false)
const izinDialog = ref(false)
const duzenleme = ref(false)
const izinPersonelId = ref(null)
const izinPersonelAdi = ref('')
const personelForm = ref(defaultForm())
const kullanicilar = ref([])
const ozet = ref({ toplam: 0, aktif: 0, pasif: 0, bekleyenIzin: 0 })
const detayDrawer = ref(false)
const detay = ref(null)
const detayYukleniyor = ref(false)

const canSalary = computed(() => ['ADMIN', 'MUHASEBE'].includes(authStore?.kullanici?.role))

const toplam = ref(0)
const sayfa = ref(0)
const sayfaBoyutu = ref(Number(localStorage.getItem(SAYFA_ANAHTAR)) || 25)
const sirala = ref('ad,asc')
const departmanlar = ref([])
const filtre = ref({ q: '', departman: null, rol: null, durum: 'TUMU' })
let aramaZamanlayici = null

const filtreVar = computed(
  () => !!filtre.value.q?.trim() || !!filtre.value.departman || !!filtre.value.rol || filtre.value.durum !== 'TUMU'
)

const durumSecenekleri = computed(() => [
  { label: t('personel.filtreDurumTumu'), value: 'TUMU' },
  { label: t('personel.aktif'), value: 'AKTIF' },
  { label: t('personel.pasif'), value: 'PASIF' }
])
const rolSecenekleri = computed(() => [
  { label: t('personel.rolSofor'), value: 'SOFOR' },
  { label: t('personel.rolDepocu'), value: 'DEPOCU' },
  { label: t('personel.rolDiger'), value: 'DIGER' }
])
const rolEtiket = (rol) => ({ SOFOR: t('personel.rolSofor'), DEPOCU: t('personel.rolDepocu'), DIGER: t('personel.rolDiger') }[rol] || rol)
const { temizle: formTemizle } = useFormKorumasi(personelForm)
const izinForm = ref({ izinTuru: '', baslangic: null, bitis: null, aciklama: '' })
const tcGecerli = computed(() => {
  const val = (personelForm.value.tcKimlik || '').replace(/\D/g, '')
  if (val.length !== 11 || val[0] === '0') return false
  const d = val.split('').map(Number)
  const tek = d[0] + d[2] + d[4] + d[6] + d[8]
  const cift = d[1] + d[3] + d[5] + d[7]
  if (((tek * 7 - cift) % 10 + 10) % 10 !== d[9]) return false
  if (d.slice(0, 10).reduce((s, x) => s + x, 0) % 10 !== d[10]) return false
  return true
})

const TELEFON_DESENI = /^[0-9+()\s-]{7,20}$/
const EMAIL_DESENI = /^[^@\s]+@[^@\s]+\.[^@\s]+$/

const izinTurleri = [
  'YILLIK_IZIN',
  'HASTA_IZNI',
  'MAZERET_IZNI',
  'DOGUM_IZNI',
  'BABALIK_IZNI',
  'EVLILIK_IZNI',
  'UCRETSIZ_IZIN'
]

// Ham enum kodlari yerine yerellestirilmis etiketler.
const izinTuruEtiket = (kod) => ({
  YILLIK_IZIN: t('personel.izinYillik'),
  HASTA_IZNI: t('personel.izinHasta'),
  MAZERET_IZNI: t('personel.izinMazeret'),
  DOGUM_IZNI: t('personel.izinDogum'),
  BABALIK_IZNI: t('personel.izinBabalik'),
  EVLILIK_IZNI: t('personel.izinEvlilik'),
  UCRETSIZ_IZIN: t('personel.izinUcretsiz')
}[kod] || kod)

const durumEtiket = (durum) => ({
  BEKLEMEDE: t('personel.durumBeklemede'),
  ONAYLANDI: t('personel.durumOnaylandi'),
  REDDEDILDI: t('personel.durumReddedildi')
}[durum] || durum)

const izinTuruSecenekleri = computed(() => izinTurleri.map((k) => ({ label: izinTuruEtiket(k), value: k })))

function defaultForm() {
  return {
    ad: '',
    soyad: '',
    tcKimlik: '',
    dogumTarihi: null,
    iseGirisTarihi: new Date(),
    cikisTarihi: null,
    departman: '',
    pozisyon: '',
    rol: '',
    kullaniciId: null,
    maas: null,
    telefon: '',
    email: '',
    adres: '',
    aktif: true
  }
}

const ozetYukle = async () => {
  try {
    const r = await personelAPI.ozet()
    ozet.value = { ...ozet.value, ...(r.data || {}) }
  } catch {
    /* özet alınamazsa KPI sıfır kalır */
  }
}

const filtreParams = () => {
  const p = { page: sayfa.value, size: sayfaBoyutu.value, sort: sirala.value }
  if (filtre.value.q?.trim()) p.q = filtre.value.q.trim()
  if (filtre.value.departman) p.departman = filtre.value.departman
  if (filtre.value.rol) p.rol = filtre.value.rol
  if (filtre.value.durum !== 'TUMU') p.durum = filtre.value.durum
  return p
}

const yukle = async () => {
  yukleniyor.value = true
  try {
    const r = await personelAPI.filtreli(filtreParams())
    personeller.value = unwrapList(r)
    toplam.value = r.data?.totalElements ?? personeller.value.length
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('personel.hataYukleme'))
  }
  yukleniyor.value = false
}

const izinleriYukle = async () => {
  try {
    const r = await personelIzinAPI.getAll({ size: 500 })
    tumIzinler.value = unwrapList(r)
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('personel.hataYukleme'))
  }
}

const kullanicilariYukle = async () => {
  try {
    const r = await kullaniciAPI.getAll({ size: 500 })
    kullanicilar.value = unwrapList(r)
  } catch {
    kullanicilar.value = []
  }
}

const departmanlariYukle = async () => {
  try {
    const r = await personelAPI.departmanlar()
    departmanlar.value = Array.isArray(r.data) ? r.data : []
  } catch {
    departmanlar.value = []
  }
}

onMounted(async () => {
  await Promise.all([izinleriYukle(), kullanicilariYukle(), departmanlariYukle()])
  await yukle()
  ozetYukle()
})

const filtreDegisti = () => {
  sayfa.value = 0
  yukle()
}

const aramaGirdi = () => {
  clearTimeout(aramaZamanlayici)
  aramaZamanlayici = setTimeout(filtreDegisti, 300)
}

const sayfaDegisti = (e) => {
  sayfa.value = e.page
  sayfaBoyutu.value = e.rows
  localStorage.setItem(SAYFA_ANAHTAR, String(e.rows))
  yukle()
}

const siralaDegisti = (e) => {
  sirala.value = `${e.sortField},${e.sortOrder === 1 ? 'asc' : 'desc'}`
  sayfa.value = 0
  yukle()
}

const excelIndir = async () => {
  excelYukleniyor.value = true
  try {
    const params = filtreParams()
    delete params.page
    delete params.size
    delete params.sort
    const res = await excelAPI.personel(params)
    const url = window.URL.createObjectURL(new Blob([res.data]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', `Personel-${new Date().toISOString().slice(0, 10)}.xlsx`)
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)
    toastBildirim.basarili(t('personel.excelIndirildi'))
  } catch {
    toastBildirim.hata(t('personel.excelHata'))
  } finally {
    excelYukleniyor.value = false
  }
}

const personelDialogAc = (data) => {
  duzenleme.value = !!data
  personelForm.value = data
    ? {
        ...data,
        dogumTarihi: data.dogumTarihi ? new Date(data.dogumTarihi) : null,
        iseGirisTarihi: data.iseGirisTarihi ? new Date(data.iseGirisTarihi) : new Date(),
        cikisTarihi: data.cikisTarihi ? new Date(data.cikisTarihi) : null
      }
    : defaultForm()
  formTemizle()
  personelDialog.value = true
}

const dogrula = () => {
  if (!(personelForm.value.ad || '').trim()) {
    toastBildirim.uyari(t('personel.adZorunluUyari'))
    return false
  }
  if (!(personelForm.value.soyad || '').trim()) {
    toastBildirim.uyari(t('personel.soyadZorunluUyari'))
    return false
  }
  const tc = (personelForm.value.tcKimlik || '').replace(/\D/g, '')
  if (tc && !tcGecerli.value) {
    toastBildirim.uyari(t('personel.tcGecersizUyari'))
    return false
  }
  const telefon = (personelForm.value.telefon || '').trim()
  if (telefon && !TELEFON_DESENI.test(telefon)) {
    toastBildirim.uyari(t('personel.telefonGecersiz'))
    return false
  }
  const email = (personelForm.value.email || '').trim()
  if (email && !EMAIL_DESENI.test(email)) {
    toastBildirim.uyari(t('personel.emailGecersiz'))
    return false
  }
  if (personelForm.value.maas !== null && personelForm.value.maas !== undefined && Number(personelForm.value.maas) < 0) {
    toastBildirim.uyari(t('personel.maasNegatifOlamaz'))
    return false
  }
  const giris = personelForm.value.iseGirisTarihi
  const cikis = personelForm.value.cikisTarihi
  if (giris && cikis && new Date(cikis) < new Date(giris)) {
    toastBildirim.uyari(t('personel.cikisGiristenOnce'))
    return false
  }
  return true
}

const personelKaydet = async () => {
  if (!dogrula()) return
  kaydediliyor.value = true
  try {
    const p = {
      ...personelForm.value,
      ad: personelForm.value.ad.trim(),
      soyad: personelForm.value.soyad.trim(),
      dogumTarihi: getLocalDateString(personelForm.value.dogumTarihi),
      iseGirisTarihi: getLocalDateString(personelForm.value.iseGirisTarihi),
      cikisTarihi: getLocalDateString(personelForm.value.cikisTarihi)
    }
    if (duzenleme.value) await personelAPI.update(personelForm.value.id, p)
    else await personelAPI.create(p)
    formTemizle()
    personelDialog.value = false
    await yukle()
    await departmanlariYukle()
    ozetYukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('personel.hataKaydet'))
  }
  kaydediliyor.value = false
}

const personelAksiyonlari = (d) => {
  const items = [{ etiket: t('personel.detay'), ikon: 'pi pi-search', islem: () => detayAc(d) }]
  if (authStore.isAdmin) {
    items.push({ etiket: t('common.edit'), ikon: 'pi pi-pencil', islem: () => personelDialogAc(d) })
  }
  items.push({ etiket: t('personel.izinEkle'), ikon: 'pi pi-calendar-plus', islem: () => izinDialogAc(d) })
  if (authStore.isAdmin) {
    items.push({ etiket: t('common.delete'), ikon: 'pi pi-trash', sinif: 'eylem-sil', islem: () => personelSil(d) })
  }
  return items
}

const detayAc = async (d) => {
  detay.value = null
  detayDrawer.value = true
  detayYukleniyor.value = true
  try {
    const r = await personelAPI.detay(d.id)
    detay.value = r.data || null
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('personel.detayYuklenemedi'))
  } finally {
    detayYukleniyor.value = false
  }
}

const drawerDuzenle = (p) => {
  detayDrawer.value = false
  personelDialogAc(p)
}

const drawerIzinEkle = (p) => {
  detayDrawer.value = false
  izinDialogAc(p)
}

const personelSil = (data) => {
  confirm.require({
    message: t('common.confirmDelete'),
    header: t('common.silmeOnayi'),
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: t('common.evetSil'),
    rejectLabel: t('common.vazgec'),
    accept: async () => {
      try {
        await personelAPI.delete(data.id)
        await yukle()
        await departmanlariYukle()
        ozetYukle()
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || err?.message || t('personel.hataSil'))
      }
    },
    reject: () => {}
  })
}

const izinDialogAc = (data) => {
  izinPersonelId.value = data.id
  izinPersonelAdi.value = `${data.ad} ${data.soyad}`
  izinForm.value = { izinTuru: '', baslangic: new Date(), bitis: new Date(), aciklama: '' }
  izinDialog.value = true
}

const izinKaydet = async () => {
  if (!izinForm.value.izinTuru) {
    toastBildirim.uyari(t('personel.izinTuruZorunluUyari'))
    return
  }
  if (!izinForm.value.baslangic || !izinForm.value.bitis) {
    toastBildirim.uyari(t('personel.izinTarihZorunlu'))
    return
  }
  if (new Date(izinForm.value.bitis) < new Date(izinForm.value.baslangic)) {
    toastBildirim.uyari(t('personel.izinBitisOnce'))
    return
  }
  kaydediliyor.value = true
  try {
    await personelIzinAPI.create({
      personelId: izinPersonelId.value,
      izinTuru: izinForm.value.izinTuru,
      baslangic: getLocalDateString(izinForm.value.baslangic),
      bitis: getLocalDateString(izinForm.value.bitis),
      aciklama: izinForm.value.aciklama
    })
    izinDialog.value = false
    await izinleriYukle()
    ozetYukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('personel.izinHataKaydet'))
  }
  kaydediliyor.value = false
}
</script>

<style scoped>
.personel-sayfasi {
  padding: 0;
}
.kpi-satiri {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}
.personel-arac {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}
.arac-bosluk {
  flex: 1 1 auto;
}
.arac-arama {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 0 10px;
  border: 1px solid var(--border);
  border-radius: var(--radius-md, 10px);
  background: var(--bg-card);
  min-width: 220px;
  flex: 1 1 240px;
  max-width: 360px;
}
.arac-arama i {
  color: var(--text-muted);
  font-size: 13px;
}
.arac-arama :deep(.arama-input) {
  border: none;
  background: transparent;
  box-shadow: none;
  width: 100%;
  padding-left: 0;
}
.arac-secim {
  min-width: 150px;
}
.personel-mobil {
  display: none;
}
.personel-skeleton {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.personel-mobil-kart {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px 14px;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: var(--bg-card);
  transition: border-color var(--dur-fast, 0.15s) var(--ease-standard, ease);
}
.pmk-ust {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.pmk-ad {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  padding: 0;
  border: none;
  background: transparent;
  color: var(--text-primary);
  font-size: 13.5px;
  font-weight: 700;
  text-align: left;
  cursor: pointer;
}
.pmk-ad:hover span {
  color: var(--accent);
}
.pmk-ad i {
  color: var(--accent);
  flex-shrink: 0;
}
.pmk-ad span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.pmk-satir {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  font-size: 12.5px;
  color: var(--text-secondary);
}
.pmk-iletisim span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  min-width: 0;
  overflow-wrap: anywhere;
}
.pmk-iletisim i {
  font-size: 11.5px;
  color: var(--text-muted);
}
.pmk-alt {
  font-size: 12px;
  color: var(--text-muted);
}
.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}
.form-grid {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.field-row {
  display: flex;
  gap: 16px;
}
.field-row .field {
  flex: 1;
}
.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.field label {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
}
.w-full {
  width: 100%;
}
.rol-badge {
  padding: 3px 10px;
  border-radius: 20px;
  font-size: 11px;
  font-weight: 600;
  background: var(--accent-soft-strong);
  color: var(--accent);
}
@media (max-width: 768px) {
  .personel-masa {
    display: none;
  }
  .personel-mobil {
    display: flex;
    flex-direction: column;
    gap: 10px;
  }
  .arac-bosluk {
    display: none;
  }
  .arac-arama {
    flex: 1 1 100%;
    max-width: none;
  }
  .arac-secim {
    flex: 1 1 45%;
    min-width: 0;
  }
  .field-row {
    flex-wrap: wrap;
  }
}
@media (prefers-reduced-motion: reduce) {
  .personel-mobil-kart,
  .pmk-ad span {
    transition: none;
  }
}
</style>
