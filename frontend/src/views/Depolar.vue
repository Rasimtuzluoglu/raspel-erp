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

    <div class="kpi-grid">
      <KpiKart
        :baslik="t('depolar.kpiToplamDepo')"
        :deger="ozet.toplamDepo"
        ikon="pi pi-warehouse"
        :para-birimi="false"
        renk="#3b82f6"
      />
      <KpiKart
        :baslik="t('depolar.kpiAktifDepo')"
        :deger="ozet.aktifDepo"
        ikon="pi pi-check-circle"
        :para-birimi="false"
        renk="#10b981"
      />
      <KpiKart
        :baslik="t('depolar.kpiStokKalemi')"
        :deger="ozet.stokKalemiSayisi"
        ikon="pi pi-box"
        :para-birimi="false"
        renk="#8b5cf6"
      />
      <KpiKart
        :baslik="t('depolar.kpiSifirStok')"
        :deger="ozet.sifirStokluKalem"
        ikon="pi pi-exclamation-triangle"
        :para-birimi="false"
        renk="#ef4444"
      />
      <KpiKart
        :baslik="t('depolar.kpiBekleyenTalep')"
        :deger="ozet.bekleyenTalep"
        ikon="pi pi-list"
        :para-birimi="false"
        renk="#f59e0b"
      />
    </div>

    <TabView v-model:active-index="aktifSekme">
      <TabPanel :header="t('depolar.depolar')">
        <DataTable
          v-if="gosterim === 'tablo'"
          :value="list"
          striped-rows
          :loading="yukleniyor"
        >
          <template #empty>
            <EmptyState
              :message="t('depolar.empty')"
              :sub-message="t('depolar.emptyHint')"
              icon="pi pi-warehouse"
              :action-label="t('depolar.yeniDepo')"
              action-icon="pi pi-plus"
              @action="dialogAc()"
            />
          </template>
          <Column
            field="ad"
            :header="t('depolar.depoAdi')"
            sortable
          />
          <Column
            field="subeAdi"
            :header="t('depolar.bagliSube')"
          />
          <Column
            field="yetkili"
            :header="t('depolar.sorumlu')"
          />
          <Column
            field="aktif"
            :header="t('common.status')"
          >
            <template #body="{ data }">
              <Tag
                :value="data.aktif ? t('status.active') : t('status.passive')"
                :severity="data.aktif ? 'success' : 'danger'"
              />
            </template>
          </Column>
          <Column
            :header="t('common.actions')"
            style="width: 70px"
          >
            <template #body="{ data }">
              <SatirEylemleri
                :gorunur="{ duzenle: true, sil: authStore.isAdmin }"
                :baslik="data.ad"
                :items="depoMenuItems(data)"
                @duzenle="dialogAc(data)"
                @sil="sil(data)"
              />
            </template>
          </Column>
        </DataTable>

        <div
          v-else
          class="depo-kartlar"
        >
          <EmptyState
            v-if="!yukleniyor && list.length === 0"
            :message="t('depolar.empty')"
            :sub-message="t('depolar.emptyHint')"
            icon="pi pi-warehouse"
            :action-label="t('depolar.yeniDepo')"
            action-icon="pi pi-plus"
            @action="dialogAc()"
          />
          <div
            v-for="d in list"
            :key="d.id"
            class="depo-kart"
          >
            <div class="depo-kart-ust">
              <strong>{{ d.ad }}</strong>
              <Tag
                :value="d.aktif ? t('status.active') : t('status.passive')"
                :severity="d.aktif ? 'success' : 'danger'"
              />
            </div>
            <div class="depo-kart-satir">
              <i class="pi pi-building" /> {{ d.subeAdi || '-' }}
            </div>
            <div class="depo-kart-satir">
              <i class="pi pi-user" /> {{ d.yetkili || '-' }}
            </div>
            <div class="depo-kart-alt">
              <Button
                :label="t('depolar.stoklariGor')"
                icon="pi pi-box"
                size="small"
                text
                @click="stokGoruntule(d)"
              />
              <SatirEylemleri
                :gorunur="{ duzenle: true, sil: authStore.isAdmin }"
                :baslik="d.ad"
                :items="depoMenuItems(d)"
                @duzenle="dialogAc(d)"
                @sil="sil(d)"
              />
            </div>
          </div>
        </div>
      </TabPanel>

      <TabPanel
        :header="t('depolar.depoStoklari')"
        :disabled="!seciliDepo"
      >
        <div
          v-if="seciliDepo"
          class="stok-islemleri"
        >
          <div class="depo-basligi">
            <i class="pi pi-warehouse" />
            <span>{{ seciliDepo.ad }}</span>
            <Tag
              v-if="seciliDepo.subeAdi"
              :value="seciliDepo.subeAdi"
              severity="secondary"
            />
          </div>
          <div class="stok-ekle-form">
            <h3>{{ t('depolar.stokEkleCikar') }}</h3>
            <div class="form-row">
              <Dropdown
                v-model="stokForm.stokId"
                :options="stokOnerileri"
                option-label="ad"
                option-value="id"
                :placeholder="t('depolar.urunSec')"
                class="w-full"
                filter
                @filter="stokAra"
              />
              <InputNumber
                v-model="stokForm.miktar"
                :placeholder="t('depolar.miktar')"
                :min="0"
              />
              <Button
                :label="t('depolar.ekle')"
                icon="pi pi-plus"
                class="p-button-success"
                :loading="stokLoading"
                :disabled="!stokForm.stokId"
                @click="stokEkle"
              />
              <Button
                :label="t('depolar.cikar')"
                icon="pi pi-minus"
                class="p-button-warning"
                :loading="stokLoading"
                :disabled="!stokForm.stokId"
                @click="stokCikar"
              />
            </div>
            <small
              v-if="seciliStokMevcut"
              class="mevcut-stok-bilgi"
            >
              <i class="pi pi-info-circle" />
              {{ t('depolar.mevcutStok') }}: <strong>{{ formatMiktar(seciliStokMevcut.miktar) }}</strong>
              {{ seciliStokMevcut.birim || '' }}
            </small>
          </div>

          <div class="stok-tablo-ust">
            <span class="stok-ozet">
              {{ depoStoklari.length }} {{ t('depolar.kalem') }} ·
              {{ t('depolar.toplamMiktar') }}: {{ formatMiktar(depoStokToplam) }}
            </span>
            <span class="arama-kutu">
              <i class="pi pi-search" />
              <InputText
                v-model="depoStokArama"
                :placeholder="t('depolar.stokAra')"
                class="arama-input"
              />
            </span>
          </div>

          <DataTable
            :value="filtreliDepoStoklari"
            striped-rows
            size="small"
          >
            <template #empty>
              <EmptyState
                icon="pi pi-box"
                :message="depoStokArama ? t('common.noData') : t('depolar.stokYok')"
              />
            </template>
            <Column
              field="stokKodu"
              :header="t('depolar.stokKodu')"
              sortable
            />
            <Column
              field="stokAd"
              :header="t('depolar.urunAdi')"
              sortable
            />
            <Column
              field="birim"
              :header="t('depolar.birim')"
            />
            <Column
              field="miktar"
              :header="t('depolar.miktar')"
              sortable
            >
              <template #body="{ data }">
                <span :class="{ 'text-danger': Number(data.miktar) <= 0 }">{{ formatMiktar(data.miktar) }}</span>
                <Tag
                  v-if="Number(data.miktar) <= 0"
                  :value="t('depolar.kpiSifirStok')"
                  severity="danger"
                  class="sifir-etiket"
                />
              </template>
            </Column>
          </DataTable>
        </div>
      </TabPanel>
    </TabView>

    <Dialog
      v-model:visible="dialog"
      :header="dialogHeader"
      modal
      :style="{ width: '90%', maxWidth: '520px' }"
    >
      <div class="form-grid">
        <div class="field">
          <label>{{ t('depolar.depoAdiZorunlu') }}</label><InputText
            v-model="form.ad"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('depolar.bagliSubeZorunlu') }}</label><Dropdown
            v-model="form.subeId"
            :options="subeListesi"
            option-label="ad"
            option-value="id"
            :placeholder="t('depolar.subeSec')"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('depolar.sorumlu') }}</label><InputText
            v-model="form.yetkili"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('depolar.adres') }}</label><Textarea
            v-model="form.adres"
            rows="2"
            class="w-full"
          />
        </div>
        <div
          v-if="duzenleme"
          class="field"
        >
          <label>{{ t('status.active') }}</label><InputSwitch v-model="form.aktif" />
        </div>
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          icon="pi pi-times"
          class="p-button-text"
          @click="dialog = false"
        />
        <Button
          :label="t('common.save')"
          icon="pi pi-check"
          :loading="kaydediliyor"
          @click="kaydet"
        />
      </template>
    </Dialog>

    <Dialog
      v-model:visible="transferDialog"
      :header="t('depolar.depolarArasiTransfer')"
      modal
      :style="{ width: '90%', maxWidth: '520px' }"
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
      :style="{ width: '90%', maxWidth: '520px' }"
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
      :style="{ width: '90%', maxWidth: '720px' }"
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
import { depoAPI, subeAPI, depoTransferAPI } from '../api/index.js'
import { useAuthStore } from '../stores/authStore.js'
import EmptyState from '../components/EmptyState.vue'
import KpiKart from '../components/KpiKart.vue'
import DepoDetayDialog from '../components/DepoDetayDialog.vue'

const { t } = useI18n()
const toast = useToast()
const toastBildirim = useToastBildirim()
const confirm = useConfirm()
const authStore = useAuthStore()
const list = ref([])
const subeListesi = ref([])

// Depo stok girişi ve transfer dropdown'ları sunucu aramalı. Önceden
// parametresiz `stokAPI.getAll()` çağrılıyordu ve backend'in 50 kayıtlık
// varsayılan tavanına takılıyordu: 50. stoktan sonrası hiçbir depoya
// giriş/transfer yapılamıyordu, kullanıcı sessizce eksik listede kalıyordu.
const { oneriler: stokOnerileri, ara: stokAra, hemenAra: stokOnerileriYukle } = useStokOnerileri()
const depoStoklari = ref([])
const seciliDepo = ref(null)
const yukleniyor = ref(false)
const kaydediliyor = ref(false)
const stokLoading = ref(false)
const transferLoading = ref(false)
const dialog = ref(false)
const duzenleme = ref(false)
const transferDialog = ref(false)
const form = ref({ ad: '', subeId: null, yetkili: '', adres: '', aktif: true })
const stokForm = ref({ stokId: null, miktar: 0 })
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

const depoMenuItems = (d) => [
  { etiket: t('depolar.detayGor'), ikon: 'pi pi-eye', grup: t('depolar.depo'), islem: () => detayAc(d) },
  { etiket: t('depolar.stoklariGor'), ikon: 'pi pi-box', grup: t('depolar.depo'), islem: () => stokGoruntule(d) }
]

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
const depoStokArama = ref('')
const filtreliDepoStoklari = computed(() => {
  const q = depoStokArama.value.trim().toLowerCase()
  if (!q) return depoStoklari.value
  return depoStoklari.value.filter((s) =>
    (s.stokAd || '').toLowerCase().includes(q) || (s.stokKodu || '').toLowerCase().includes(q)
  )
})
const depoStokToplam = computed(() =>
  depoStoklari.value.reduce((acc, s) => acc + (Number(s?.miktar) || 0), 0)
)
const seciliStokMevcut = computed(() =>
  depoStoklari.value.find((s) => s.stokId === stokForm.value.stokId) || null
)

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

const dialogHeader = computed(() => (duzenleme.value ? t('depolar.depoDuzenle') : t('depolar.yeniDepo')))

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
    // Dropdown ilk açılışta boş görünmesin diye ilk sayfa bir kez çekilir.
    stokOnerileriYukle()
    ozetYukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('depolar.hataVeri'))
  }
  yukleniyor.value = false
})

const dialogAc = (data) => {
  duzenleme.value = !!data
  form.value = data ? { ...data } : { ad: '', subeId: null, yetkili: '', adres: '', aktif: true }
  dialog.value = true
}

const kaydet = async () => {
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
        toast.add({ severity: 'success', summary: t('depolar.silindi'), detail: t('depolar.depoSilindi'), life: 3000 })
        ozetYukle()
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || t('depolar.silmeBasarisiz'))
      }
    }
  })
}

const stokGoruntule = async (depo) => {
  seciliDepo.value = depo
  detayDialog.value = false
  // Depo secilince "Depo Stoklari" sekmesi otomatik acilir.
  aktifSekme.value = 1
  try {
    const r = await depoAPI.getStoklar(depo.id)
    depoStoklari.value = r.data
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('depolar.stoklarYuklenemedi'))
  }
}

const stokEkle = async () => {
  if (!stokForm.value.stokId || !stokForm.value.miktar) return
  stokLoading.value = true
  try {
    await depoAPI.stokEkle(seciliDepo.value.id, { stokId: stokForm.value.stokId, miktar: stokForm.value.miktar })
    const r = await depoAPI.getStoklar(seciliDepo.value.id)
    depoStoklari.value = r.data
    toastBildirim.basarili(t('depolar.stokEklendi'))
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('depolar.stokEklemeBasarisiz'))
  }
  stokLoading.value = false
  ozetYukle()
}

const stokCikar = async () => {
  if (!stokForm.value.stokId || !stokForm.value.miktar) return
  stokLoading.value = true
  try {
    await depoAPI.stokCikar(seciliDepo.value.id, { stokId: stokForm.value.stokId, miktar: stokForm.value.miktar })
    const r = await depoAPI.getStoklar(seciliDepo.value.id)
    depoStoklari.value = r.data
    toastBildirim.basarili(t('depolar.stokCikarildi'))
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('depolar.stokCikarmaBasarisiz'))
  }
  stokLoading.value = false
  ozetYukle()
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
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(170px, 100%), 1fr));
  gap: 12px;
  margin-bottom: 1rem;
}
.form-grid {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.form-row {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
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
.stok-islemleri {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.depo-basligi {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
}
.depo-basligi i {
  color: var(--accent);
}
.stok-ekle-form {
  background: var(--bg-secondary);
  padding: 16px;
  border-radius: 8px;
}
.stok-ekle-form h3 {
  margin: 0 0 12px;
  font-size: 15px;
}
.text-danger {
  color: #f87171;
}
.depo-kartlar {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(min(280px, 100%), 1fr));
  gap: 12px;
}
.depo-kart {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 14px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 1rem;
}
.depo-kart-ust {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.depo-kart-ust strong {
  font-size: 1rem;
  color: var(--text-primary);
  min-width: 0;
}
.depo-kart-satir {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.82rem;
  color: var(--text-secondary);
}
.depo-kart-alt {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-top: auto;
  padding-top: 8px;
  border-top: 1px solid var(--border);
}
.mevcut-stok-bilgi {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-top: 10px;
  font-size: 0.8rem;
  color: var(--text-secondary);
}
.mevcut-stok-bilgi strong {
  color: var(--primary-color, var(--accent));
}
.stok-tablo-ust {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
}
.stok-ozet {
  font-size: 0.82rem;
  color: var(--text-secondary);
  font-weight: 600;
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
  color: var(--text-muted);
  pointer-events: none;
}
.arama-input {
  width: 100%;
  padding-left: 32px;
}
.sifir-etiket {
  margin-left: 8px;
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
  .form-row {
    flex-direction: column;
    align-items: stretch;
  }
  .stok-tablo-ust,
  .talep-arac {
    flex-direction: column;
    align-items: stretch;
  }
  .arama-kutu {
    min-width: 0;
  }
  .stok-ekle-form {
    padding: 12px;
  }
}
</style>
