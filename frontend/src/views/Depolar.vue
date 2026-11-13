<template>
  <div class="depolar-container">
    <PageHeader :title="t('depolar.title')">
      <template #actions>
        <Button
          v-if="authStore.isAdmin"
          :label="t('depolar.depolarArasiTransfer')"
          icon="pi pi-sync"
          class="p-button-info p-button-outlined"
          :title="t('depolar.hizliTransferIpucu')"
          @click="transferDialog = true"
        />
        <Button
          :label="t('depolar.transferTalebiOlustur')"
          icon="pi pi-plus-circle"
          class="p-button-outlined"
          @click="talepOlusturAc"
        />
        <Button
          :label="t('depolar.transferTalepleri')"
          icon="pi pi-list"
          class="p-button-warning p-button-outlined"
          @click="talepDialogAc"
        />
        <Button
          :label="t('depolar.yeniDepo')"
          icon="pi pi-plus"
          @click="dialogAc()"
        />
        <Button
          :icon="gosterim === 'tablo' ? 'pi pi-th-large' : 'pi pi-list'"
          text
          rounded
          :title="gosterim === 'tablo' ? t('depolar.kartGorunumu') : t('depolar.tabloGorunumu')"
          :aria-label="gosterim === 'tablo' ? t('depolar.kartGorunumu') : t('depolar.tabloGorunumu')"
          @click="gosterimDegistir"
        />
      </template>
    </PageHeader>

    <DepoOzetKartlari
      :ozet="ozet"
      :geciken-talep="gecikenTalep"
      @kpi-sec="kpiSec"
    />

    <TabView v-model:active-index="aktifSekme">
      <TabPanel :header="t('depolar.depolar')">
        <DepoListePaneli
          :subeler="subeListesi"
          :admin-mi="authStore.isAdmin"
          :gosterim="gosterim"
          @gosterim-degistir="gosterimDegistir"
          @yeni="dialogAc()"
          @duzenle="dialogAc"
          @sil="sil"
          @detay="detayAc"
          @stoklar="stokGoruntule"
          @transfer="depodanTransfer"
        />
      </TabPanel>

      <TabPanel
        :header="t('depolar.depoStoklari')"
        :disabled="!seciliDepo"
      >
        <DepoStokPaneli
          v-if="seciliDepo"
          :depo="seciliDepo"
          :islem-yapabilir="islemYapabilir(seciliDepo)"
          @hareket-goster="hareketGoster"
          @ozet-yenile="ozetYukle"
        />
        <EmptyState
          v-else
          icon="pi pi-warehouse"
          :message="t('depolar.onceDepoSecin')"
        />
      </TabPanel>

      <TabPanel :header="t('depolar.transferler')">
        <DepoTransferPaneli
          :depolar="list"
          :admin-mi="authStore.isAdmin"
          @yenile="ozetYukle"
        />
      </TabPanel>

      <TabPanel
        :header="t('depolar.hareketler')"
        :disabled="!seciliDepo"
      >
        <DepoHareketPaneli
          v-if="seciliDepo"
          :depo="seciliDepo"
        />
        <EmptyState
          v-else
          icon="pi pi-history"
          :message="t('depolar.onceDepoSecin')"
        />
      </TabPanel>
    </TabView>

    <DepoFormDialog
      v-model:visible="dialog"
      :depo="duzenlenenDepo"
      :subeler="subeListesi"
      :kullanicilar="kullanicilar"
      :kaydediliyor="kaydediliyor"
      @kaydet="kaydet"
    />

    <Dialog
      v-model:visible="transferDialog"
      :header="t('depolar.depolarArasiTransfer')"
      modal
      :style="{ width: 'min(92vw, 460px)' }"
    >
      <div class="form-grid">
        <div class="field">
          <label>{{ t('depolar.kaynakDepoZorunlu') }}</label><Dropdown
            v-model="transferForm.kaynakDepoId"
            :options="list"
            option-label="ad"
            option-value="id"
            :placeholder="t('depolar.kaynakDepo')"
            class="w-full"
            @change="kaynakDegisti"
          />
        </div>
        <div class="field">
          <label>{{ t('depolar.hedefDepoZorunlu') }}</label><Dropdown
            v-model="transferForm.hedefDepoId"
            :options="list"
            option-label="ad"
            option-value="id"
            :placeholder="t('depolar.hedefDepo')"
            class="w-full"
            :invalid="hedefAyni"
          />
          <small
            v-if="hedefAyni"
            class="alan-hata"
          >{{ t('depolar.kaynakHedefAyni') }}</small>
        </div>
        <div class="field">
          <label>{{ t('depolar.urunZorunlu') }}</label><Dropdown
            v-model="transferForm.stokId"
            :options="stokOnerileri"
            option-label="ad"
            option-value="id"
            :placeholder="t('depolar.urunSec')"
            class="w-full"
            filter
            @filter="stokAra"
          />
        </div>
        <div class="field">
          <label>{{ t('depolar.miktarZorunlu') }}</label><InputNumber
            v-model="transferForm.miktar"
            :min="0"
            class="w-full"
          />
        </div>
        <div
          v-if="transferForm.kaynakDepoId && transferForm.stokId"
          class="transfer-etki"
        >
          <div class="etki-satir">
            <span>{{ t('depolar.mevcutStok') }}</span>
            <strong>{{ formatMiktar(seciliKaynakMiktar) }}</strong>
          </div>
          <div class="etki-satir">
            <span>{{ t('depolar.transferSonrasi') }}</span>
            <strong :class="{ 'text-danger': transferSonrasi < 0 }">{{ formatMiktar(transferSonrasi) }}</strong>
          </div>
        </div>
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          icon="pi pi-times"
          class="p-button-text"
          @click="transferDialog = false"
        />
        <Button
          :label="t('depolar.transferEt')"
          icon="pi pi-send"
          :loading="transferLoading"
          :disabled="!transferGecerli"
          @click="transferYap"
        />
      </template>
    </Dialog>

    <Dialog
      v-model:visible="talepOlusturDialog"
      :header="t('depolar.transferTalebiOlustur')"
      modal
      :style="{ width: 'min(92vw, 460px)' }"
    >
      <div class="form-grid">
        <div class="field">
          <label>{{ t('depolar.kaynakDepoZorunlu') }}</label><Dropdown
            v-model="talepForm.kaynakDepoId"
            :options="list"
            option-label="ad"
            option-value="id"
            :placeholder="t('depolar.kaynakDepo')"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('depolar.hedefDepoZorunlu') }}</label><Dropdown
            v-model="talepForm.hedefDepoId"
            :options="list"
            option-label="ad"
            option-value="id"
            :placeholder="t('depolar.hedefDepo')"
            class="w-full"
            :invalid="talepHedefAyni"
          />
          <small
            v-if="talepHedefAyni"
            class="alan-hata"
          >{{ t('depolar.kaynakHedefAyni') }}</small>
        </div>
        <div class="field">
          <label>{{ t('depolar.urunZorunlu') }}</label><Dropdown
            v-model="talepForm.stokId"
            :options="stokOnerileri"
            option-label="ad"
            option-value="id"
            :placeholder="t('depolar.urunSec')"
            class="w-full"
            filter
            @filter="stokAra"
          />
        </div>
        <div class="field">
          <label>{{ t('depolar.miktarZorunlu') }}</label><InputNumber
            v-model="talepForm.miktar"
            :min="0"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('common.description') }}</label><Textarea
            v-model="talepForm.aciklama"
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
          @click="talepOlusturDialog = false"
        />
        <Button
          :label="t('depolar.transferTalebiGonder')"
          icon="pi pi-send"
          :loading="talepOlusturuluyor"
          :disabled="!talepGecerli"
          @click="talepOlustur"
        />
      </template>
    </Dialog>

    <Dialog
      v-model:visible="talepDialog"
      :header="t('depolar.transferTalepleri')"
      :modal="true"
      :style="{ width: 'min(92vw, 620px)' }"
    >
      <div class="talep-arac">
        <Select
          v-model="talepDurumFiltre"
          :options="talepDurumSecenekleri"
          option-label="label"
          option-value="value"
          :placeholder="t('depolar.tumu')"
          show-clear
          class="talep-durum"
        />
        <span class="arama-kutu">
          <i class="pi pi-search" />
          <InputText
            v-model="talepArama"
            :placeholder="t('depolar.talepAra')"
            class="arama-input"
          />
        </span>
      </div>
      <DataTable
        :value="filtreliTalepler"
        striped-rows
        size="small"
        :loading="talepYukleniyor"
      >
        <template #empty>
          <EmptyState
            icon="pi pi-list"
            :message="t('depolar.talepYok')"
          />
        </template>
        <Column :header="t('depolar.kaynakDepo')">
          <template #body="{ data }">
            {{ data.kaynakDepoAd || data.kaynakDepoId }}
          </template>
        </Column>
        <Column :header="t('depolar.hedefDepo')">
          <template #body="{ data }">
            {{ data.hedefDepoAd || data.hedefDepoId }}
          </template>
        </Column>
        <Column :header="t('depolar.stok')">
          <template #body="{ data }">
            {{ data.stokAd || data.stokId }}
          </template>
        </Column>
        <Column :header="t('depolar.miktar')">
          <template #body="{ data }">
            {{ data.miktar }}
          </template>
        </Column>
        <Column :header="t('common.status')">
          <template #body="{ data }">
            <Tag
              :value="durumEtiket(data.durum)"
              :severity="durumSeverity(data.durum)"
            />
          </template>
        </Column>
        <Column
          header=""
          style="width: 100px"
        >
          <template #body="{ data }">
            <!-- Onay/red yalnızca ADMIN yetkisinde; aksi halde buton 403 üretiyordu. -->
            <template v-if="data.durum === 'BEKLIYOR' && authStore.isAdmin">
              <Button
                icon="pi pi-check"
                :aria-label="$t('common.onayla')"
                class="p-button-rounded p-button-success p-button-sm"
                style="margin-right: 6px"
                @click="talepOnayla(data)"
              />
              <Button
                icon="pi pi-times"
                :aria-label="$t('common.close')"
                class="p-button-rounded p-button-danger p-button-sm"
                @click="talepReddet(data)"
              />
            </template>
            <span
              v-else-if="data.durum === 'BEKLIYOR'"
              class="beklemede-metin"
            >{{ t('depolar.yoneticiOnayiBekliyor') }}</span>
          </template>
        </Column>
      </DataTable>
    </Dialog>

    <DepoDetayDialog
      v-model:visible="detayDialog"
      :depo="secili360"
      @duzenle="depodanDuzenle"
      @stoklari-yonet="depodanStoklariYonet"
      @transfer="depodanTransfer"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { unwrapList } from '../api/utils/unwrap.js'
import { useToast } from 'primevue/usetoast'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useConfirm } from 'primevue/useconfirm'
import { useStokOnerileri } from '../composables/useStokOnerileri.js'
import { depoAPI, subeAPI, depoTransferAPI, kullaniciAPI } from '../api/index.js'
import { useAuthStore } from '../stores/authStore.js'
import { useDepoStore } from '../stores/depoStore.js'
import { useDepoYetki } from '../composables/useDepoYetki.js'
import EmptyState from '../components/EmptyState.vue'
import DepoDetayDialog from '../components/DepoDetayDialog.vue'
import DepoOzetKartlari from '../components/depo/DepoOzetKartlari.vue'
import DepoListePaneli from '../components/depo/DepoListePaneli.vue'
import DepoStokPaneli from '../components/depo/DepoStokPaneli.vue'
import DepoTransferPaneli from '../components/depo/DepoTransferPaneli.vue'
import DepoHareketPaneli from '../components/depo/DepoHareketPaneli.vue'
import DepoFormDialog from '../components/depo/DepoFormDialog.vue'

const { t } = useI18n()
const toast = useToast()
const toastBildirim = useToastBildirim()
const confirm = useConfirm()
const authStore = useAuthStore()
const depoStore = useDepoStore()
const { islemYapabilir } = useDepoYetki()
const list = ref([])
const subeListesi = ref([])
const kullanicilar = ref([])
const gecikenTalep = ref(0)
const duzenlenenDepo = ref(null)

// Depo stok girişi ve transfer dropdown'ları sunucu aramalı. Önceden
// parametresiz `stokAPI.getAll()` çağrılıyordu ve backend'in 50 kayıtlık
// varsayılan tavanına takılıyordu: 50. stoktan sonrası hiçbir depoya
// giriş/transfer yapılamıyordu, kullanıcı sessizce eksik listede kalıyordu.
const { oneriler: stokOnerileri, ara: stokAra, hemenAra: stokOnerileriYukle } = useStokOnerileri()
const seciliDepo = ref(null)
const yukleniyor = ref(false)
const kaydediliyor = ref(false)
const transferLoading = ref(false)
const dialog = ref(false)
const duzenleme = ref(false)
const transferDialog = ref(false)
const form = ref({ ad: '', subeId: null, yetkili: '', adres: '', aktif: true })
const transferForm = ref({ kaynakDepoId: null, hedefDepoId: null, stokId: null, miktar: 0 })

// Sekme + gorunum tercihleri. "Stoklari gor" depo stoklari sekmesini acar.
const aktifSekme = ref(0)
const GORUNUM_ANAHTAR = 'raspel_depolar_gosterim'
const gosterim = ref(localStorage.getItem(GORUNUM_ANAHTAR) || 'tablo')
const gosterimDegistir = () => {
  gosterim.value = gosterim.value === 'tablo' ? 'kart' : 'tablo'
  localStorage.setItem(GORUNUM_ANAHTAR, gosterim.value)
}

// Depo 360 detay modali.
const detayDialog = ref(false)
const secili360 = ref(null)

const detayAc = (d) => {
  secili360.value = d
  detayDialog.value = true
}

const depodanDuzenle = (d) => {
  detayDialog.value = false
  dialogAc(d)
}

const depodanStoklariYonet = (d) => {
  detayDialog.value = false
  stokGoruntule(d)
}

const depodanTransfer = (d) => {
  detayDialog.value = false
  transferForm.value = { kaynakDepoId: d.id, hedefDepoId: null, stokId: null, miktar: 0 }
  transferDialog.value = true
  kaynakDegisti()
}

// --- Depo stoklari sekmesi: arama + ozet ---
// --- Transfer: kaynak depo stoklari + sonrasi onizleme ---
const kaynakStoklari = ref([])
const kaynakDegisti = async () => {
  const id = transferForm.value.kaynakDepoId
  if (!id) {
    kaynakStoklari.value = []
    return
  }
  try {
    const r = await depoAPI.getStoklar(id)
    kaynakStoklari.value = r.data || []
  } catch {
    kaynakStoklari.value = []
  }
}
const seciliKaynakMiktar = computed(() => {
  const s = kaynakStoklari.value.find((x) => x.stokId === transferForm.value.stokId)
  return s ? Number(s.miktar) || 0 : 0
})
const transferSonrasi = computed(() => seciliKaynakMiktar.value - (Number(transferForm.value.miktar) || 0))
const hedefAyni = computed(
  () =>
    transferForm.value.kaynakDepoId != null &&
    transferForm.value.hedefDepoId != null &&
    transferForm.value.kaynakDepoId === transferForm.value.hedefDepoId
)
const transferGecerli = computed(
  () =>
    !!transferForm.value.kaynakDepoId &&
    !!transferForm.value.hedefDepoId &&
    !hedefAyni.value &&
    !!transferForm.value.stokId &&
    Number(transferForm.value.miktar) > 0 &&
    Number(transferForm.value.miktar) <= seciliKaynakMiktar.value
)

// --- Transfer talebi olusturma + listeleme ---
const talepOlusturDialog = ref(false)
const talepOlusturuluyor = ref(false)
const talepForm = ref({ kaynakDepoId: null, hedefDepoId: null, stokId: null, miktar: 0, aciklama: '' })
const talepHedefAyni = computed(
  () =>
    talepForm.value.kaynakDepoId != null &&
    talepForm.value.hedefDepoId != null &&
    talepForm.value.kaynakDepoId === talepForm.value.hedefDepoId
)
const talepGecerli = computed(
  () =>
    !!talepForm.value.kaynakDepoId &&
    !!talepForm.value.hedefDepoId &&
    !talepHedefAyni.value &&
    !!talepForm.value.stokId &&
    Number(talepForm.value.miktar) > 0
)

const talepOlusturAc = () => {
  talepForm.value = { kaynakDepoId: null, hedefDepoId: null, stokId: null, miktar: 0, aciklama: '' }
  talepOlusturDialog.value = true
}

const talepOlustur = async () => {
  if (!talepGecerli.value) return
  talepOlusturuluyor.value = true
  try {
    await depoTransferAPI.create(talepForm.value)
    toastBildirim.basarili(t('depolar.talepOlusturuldu'))
    talepOlusturDialog.value = false
    ozetYukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('depolar.talepOlusturulamadi'))
  } finally {
    talepOlusturuluyor.value = false
  }
}

// Talep listesi: durum filtresi + arama.
const talepDurumFiltre = ref(null)
const talepArama = ref('')
const talepDurumSecenekleri = computed(() => [
  { label: t('depolar.durumBekliyor'), value: 'BEKLIYOR' },
  { label: t('depolar.durumOnaylandi'), value: 'ONAYLANDI' },
  { label: t('depolar.durumReddedildi'), value: 'REDDEDILDI' }
])
const filtreliTalepler = computed(() => {
  const q = talepArama.value.trim().toLowerCase()
  return transferTalepleri.value.filter((x) => {
    if (talepDurumFiltre.value && x.durum !== talepDurumFiltre.value) return false
    if (!q) return true
    const metin = `${x.kaynakDepoAd || ''} ${x.hedefDepoAd || ''} ${x.stokAd || ''}`.toLowerCase()
    return metin.includes(q)
  })
})

// Talep durumunu kullaniciya cevirir (ham BEKLIYOR/ONAYLANDI gorunmesin).
const durumEtiket = (d) => {
  if (d === 'BEKLIYOR') return t('depolar.durumBekliyor')
  if (d === 'ONAYLANDI') return t('depolar.durumOnaylandi')
  if (d === 'REDDEDILDI') return t('depolar.durumReddedildi')
  return d || '-'
}
const durumSeverity = (d) => (d === 'BEKLIYOR' ? 'warning' : d === 'ONAYLANDI' ? 'success' : 'danger')

// KPI ozeti (depo/stok/talep sayilari).
const ozet = ref({
  toplamDepo: 0,
  aktifDepo: 0,
  pasifDepo: 0,
  stokKalemiSayisi: 0,
  sifirStokluKalem: 0,
  bekleyenTalep: 0
})

const ozetYukle = async () => {
  try {
    const r = await depoAPI.ozet()
    ozet.value = { ...ozet.value, ...(r.data || {}) }
  } catch {
    /* ozet alinamazsa KPI'lar sifirda kalir; liste akisini engellemez */
  }
  try {
    const r = await depoTransferAPI.sayfali({ durum: 'BEKLIYOR', page: 0, size: 100 })
    gecikenTalep.value = (r.data?.content || []).filter((t) => t.slaDurumu === 'GECIKTI').length
  } catch {
    gecikenTalep.value = 0
  }
}

const kpiSec = ({ anahtar }) => {
  if (anahtar === 'talep' || anahtar === 'geciken') aktifSekme.value = 2
  else aktifSekme.value = 0
}

const hareketGoster = ({ stokId }) => {
  if (!seciliDepo.value) return
  void stokId
  aktifSekme.value = 3
}

// Miktar (adet) bicimi: para birimi DEGIL. "1.250" / "1.250,5".
const formatMiktar = (v) => {
  if (v === null || v === undefined || isNaN(v)) return '0'
  return new Intl.NumberFormat('tr-TR', { maximumFractionDigits: 2 }).format(v)
}

onMounted(async () => {
  // Kayitli tercih yoksa kucuk ekranda kart gorunumu varsayilan.
  if (!localStorage.getItem(GORUNUM_ANAHTAR)
      && typeof window.matchMedia === 'function'
      && window.matchMedia('(max-width: 768px)').matches) {
    gosterim.value = 'kart'
  }
  yukleniyor.value = true
  try {
    const [depoRes, subeRes] = await Promise.all([depoAPI.getAll(), subeAPI.getAll()])
    list.value = unwrapList(depoRes)
    subeListesi.value = unwrapList(subeRes)
    try {
      const k = await kullaniciAPI.getAll({ size: 500 })
      kullanicilar.value = unwrapList(k)
    } catch {
      kullanicilar.value = []
    }
    // Dropdown ilk açılışta boş görünmesin diye ilk sayfa bir kez çekilir.
    stokOnerileriYukle()
    await depoStore.yukle()
    ozetYukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('depolar.hataVeri'))
  }
  yukleniyor.value = false
})

const dialogAc = (data) => {
  duzenlenenDepo.value = data || null
  duzenleme.value = !!data
  form.value = data ? { ...data } : { ad: '', subeId: null, yetkili: '', adres: '', aktif: true }
  dialog.value = true
}

const kaydet = async (gelen) => {
  if (gelen) form.value = { ...form.value, ...gelen }
  if (!form.value.ad || !form.value.ad.trim()) {
    toastBildirim.uyari(t('depolar.adZorunlu'))
    return
  }
  if (!form.value.subeId) {
    toastBildirim.uyari(t('depolar.subeZorunlu'))
    return
  }
  kaydediliyor.value = true
  try {
    if (duzenleme.value) {
      await depoAPI.update(form.value.id, form.value)
      toastBildirim.basarili(t('depolar.guncellendi'))
    } else {
      await depoAPI.create(form.value)
      toastBildirim.basarili(t('depolar.olusturuldu'))
    }
    dialog.value = false
    const r = await depoAPI.getAll()
    list.value = unwrapList(r)
    await depoStore.yukle()
    ozetYukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('depolar.islemBasarisiz'))
  }
  kaydediliyor.value = false
}

const sil = (data) => {
  confirm.require({
    message: t('depolar.silOnayMesaj', { ad: data.ad }),
    header: t('common.silmeOnayi'),
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: t('common.evetSil'),
    rejectLabel: t('common.vazgec'),
    accept: async () => {
      try {
        await depoAPI.delete(data.id)
        list.value = list.value.filter((x) => x.id !== data.id)
        await depoStore.yukle()
        toast.add({ severity: 'success', summary: t('depolar.silindi'), detail: t('depolar.depoSilindi'), life: 3000 })
        ozetYukle()
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || t('depolar.silmeBasarisiz'))
      }
    }
  })
}

const stokGoruntule = (depo) => {
  seciliDepo.value = depo
  detayDialog.value = false
  // Depo secilince "Depo Stoklari" sekmesi otomatik acilir.
  aktifSekme.value = 1
}

const transferYap = async () => {
  if (hedefAyni.value) {
    toastBildirim.uyari(t('depolar.kaynakHedefAyni'))
    return
  }
  if (Number(transferForm.value.miktar) <= 0) {
    toastBildirim.uyari(t('depolar.miktarPozitif'))
    return
  }
  if (Number(transferForm.value.miktar) > seciliKaynakMiktar.value) {
    toastBildirim.uyari(t('depolar.yetersizStok'))
    return
  }
  if (!transferGecerli.value) return
  transferLoading.value = true
  try {
    await depoAPI.transfer(transferForm.value)
    transferDialog.value = false
    toastBildirim.basarili(t('depolar.transferTamamlandi'))
    ozetYukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('depolar.transferBasarisiz'))
  }
  transferLoading.value = false
}

const talepDialog = ref(false)
const transferTalepleri = ref([])
const talepYukleniyor = ref(false)

const talepDialogAc = async () => {
  talepDialog.value = true
  await talepYukle()
}

const talepYukle = async () => {
  talepYukleniyor.value = true
  try {
    const r = await depoTransferAPI.getAll()
    transferTalepleri.value = r.data || []
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('depolar.taleplerYuklenemedi'))
  } finally {
    talepYukleniyor.value = false
  }
}

const talepOnayla = async (t) => {
  try {
    await depoTransferAPI.onayla(t.id)
    toastBildirim.basarili(t('depolar.transferOnaylandi'))
    talepYukle()
    ozetYukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('depolar.onaylamaBasarisiz'))
  }
}

const talepReddet = async (t) => {
  try {
    await depoTransferAPI.reddet(t.id)
    toastBildirim.basarili(t('depolar.transferReddedildi'))
    talepYukle()
    ozetYukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('depolar.reddetmeBasarisiz'))
  }
}
</script>

<style scoped>
.depolar-container {
  padding: 0;
}
.depolar-container :deep(.p-tabview-tablist) {
  scrollbar-width: none;
}
.depolar-container :deep(.p-tabview-tablist-scroll-container) {
  scrollbar-width: none;
}
.depolar-container :deep(.p-tabview-tab-header) {
  white-space: nowrap;
}
.form-grid {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.field {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
/* Diyaloglar: varsayilan 48px input yuksekligi formu gereksiz sisiriyordu. */
.field :deep(.p-inputtext),
.field :deep(.p-select),
.field :deep(.p-inputnumber-input) {
  padding: 0.5rem 0.7rem;
  font-size: 0.875rem;
}
.field small {
  font-size: 11.5px;
  color: var(--text-muted);
}
.field label {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
}
.w-full {
  width: 100%;
}
.text-danger {
  color: #f87171;
}
.arama-kutu {
  position: relative;
  display: inline-flex;
  align-items: center;
  min-width: min(220px, 100%);
}
.arama-kutu > i {
  position: absolute;
  left: 10px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--text-muted);
  pointer-events: none;
}
.arama-input {
  width: 100%;
  padding-left: 32px;
}
.transfer-etki {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 10px 12px;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: var(--bg-secondary);
}
.etki-satir {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  font-size: 0.85rem;
  color: var(--text-secondary);
}
.alan-hata {
  display: block;
  margin-top: 4px;
  color: var(--danger, #ef4444);
  font-size: 0.75rem;
}
.talep-arac {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}
.talep-durum {
  min-width: min(180px, 100%);
}

/* Mobil: form satirlari ve arac cubuklari alt alta. */
@media (max-width: 640px) {
  .talep-arac {
    flex-direction: column;
    align-items: stretch;
  }
  .arama-kutu {
    min-width: 0;
  }
}
</style>
