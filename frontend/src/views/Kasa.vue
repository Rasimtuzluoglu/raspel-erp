<template>
  <div class="kasa-container">
    <PageHeader :title="t('kasa.title')">
      <template #actions>
        <Button
          v-if="yazabilir"
          :label="t('kasa.yeniKasa')"
          icon="pi pi-plus"
          @click="openKasaDialog"
        />
        <Button
          v-if="aktarabilir"
          :label="t('kasa.transferler')"
          icon="pi pi-arrow-right-arrow-left"
          class="p-button-outlined"
          aria-haspopup="true"
          aria-controls="kasa-transfer-menu"
          @click="transferMenu?.toggle($event)"
        />
        <Menu
          id="kasa-transfer-menu"
          ref="transferMenu"
          :model="transferMenuModel"
          :popup="true"
        />
        <Button
          :label="t('kasa.gunSonu')"
          icon="pi pi-calendar"
          class="p-button-outlined"
          @click="gunSonuRef?.open()"
        />
        <Button
          :icon="bakiyeGorunur ? 'pi pi-eye-slash' : 'pi pi-eye'"
          text
          rounded
          :title="bakiyeGorunur ? t('kasa.bakiyeGizle') : t('kasa.bakiyeGoster')"
          :aria-label="bakiyeGorunur ? t('kasa.bakiyeGizle') : t('kasa.bakiyeGoster')"
          @click="bakiyeGorunurlukDegistir"
        />
        <Button
          :label="t('kasa.excel')"
          icon="pi pi-file-excel"
          class="p-button-outlined"
          :loading="excelYukleniyor"
          @click="excelIndir"
        />
      </template>
    </PageHeader>

    <div class="kpi-grid">
      <KpiKart
        :baslik="t('kasa.kpiToplamKasa')"
        :deger="ozet.toplamKasa"
        ikon="pi pi-wallet"
        :para-birimi="false"
        renk="#3b82f6"
      />
      <KpiKart
        :baslik="t('kasa.kpiToplamBakiye')"
        :deger="ozet.toplamBakiye"
        ikon="pi pi-money-bill"
        renk="#10b981"
      >
        <template #deger>
          {{ bakiyeGorunur ? formatCurrency(ozet.toplamBakiye) : '••••' }}
        </template>
      </KpiKart>
      <KpiKart
        :baslik="t('kasa.kpiBugunGelir')"
        :deger="ozet.bugunGelir"
        ikon="pi pi-arrow-down-left"
        renk="#22c55e"
      >
        <template #deger>
          {{ bakiyeGorunur ? formatCurrency(ozet.bugunGelir) : '••••' }}
        </template>
      </KpiKart>
      <KpiKart
        :baslik="t('kasa.kpiBugunGider')"
        :deger="ozet.bugunGider"
        ikon="pi pi-arrow-up-right"
        renk="#ef4444"
      >
        <template #deger>
          {{ bakiyeGorunur ? formatCurrency(ozet.bugunGider) : '••••' }}
        </template>
      </KpiKart>
    </div>

    <div
      v-if="kasaStore.loading"
      class="loading"
    >
      <p><i class="pi pi-spin pi-spinner" /> {{ t('common.loading') }}</p>
    </div>

    <div
      v-if="!kasaStore.loading"
      class="kasa-cards"
    >
      <div
        v-for="kasa in kasaStore.kasalar"
        :key="kasa.id"
        class="kasa-card"
        :class="{ active: seciliKasaId === kasa.id }"
        @click="kasaSec(kasa)"
      >
        <div class="kasa-ust">
          <i class="pi pi-wallet" />
          <h3>{{ kasa.ad }}</h3>
          <i
            v-if="seciliKasaId === kasa.id"
            class="pi pi-check-circle secili-ikon"
            :title="t('kasa.secili')"
          />
        </div>
        <p
          v-if="bakiyeGorunur"
          class="kasa-bakiye gizli-veri"
          :class="kasa.bakiye >= 0 ? 'positive' : 'negative'"
        >
          {{ formatCurrency(kasa.bakiye) }}
        </p>
        <p
          v-else
          class="kasa-bakiye"
        >
          ••••
        </p>
        <div class="kasa-islem">
          <SatirEylemleri
            :gorunur="{ duzenle: yazabilir, sil: silebilir }"
            :baslik="kasa.ad"
            :items="kasaMenuItems(kasa)"
            @duzenle="editKasa(kasa)"
            @sil="confirmDel(kasa.id)"
          />
        </div>
      </div>
      <EmptyState
        v-if="kasaStore.kasalar.length === 0"
        :message="t('kasa.empty')"
        :sub-message="t('kasa.emptyHint')"
        icon="pi pi-wallet"
        :action-label="t('kasa.yeniKasa')"
        action-icon="pi pi-plus"
        class="full-width"
        @action="openKasaDialog"
      />
    </div>

    <Dialog
      v-model:visible="showKasaDialog"
      :header="editingKasaId ? t('kasa.kasaDuzenle') : t('kasa.yeniKasa')"
      :modal="true"
      style="width: 400px"
      :breakpoints="{ '575px': '92vw' }"
    >
      <div class="form-group">
        <label>{{ t('kasa.kasaAdi') }}</label>
        <InputText
          v-model="kasaForm.ad"
          :placeholder="t('kasa.kasaAdiPlaceholder')"
          class="w-full"
        />
      </div>
      <div
        v-if="!editingKasaId"
        class="form-group"
      >
        <label>{{ t('kasa.acilisBakiyesi') }}</label>
        <InputNumber
          v-model="kasaForm.bakiye"
          :min="0"
          :min-fraction-digits="2"
          :max-fraction-digits="2"
          class="w-full"
        />
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          icon="pi pi-times"
          class="p-button-text"
          @click="showKasaDialog = false"
        />
        <Button
          :label="editingKasaId ? t('kasa.guncelle') : t('common.save')"
          icon="pi pi-check"
          :loading="saving"
          @click="saveKasa"
        />
      </template>
    </Dialog>

    <div
      v-if="seciliKasa"
      class="hareket-bolumu"
    >
      <div class="hareket-header">
        <h2>{{ seciliKasa.ad }} {{ t('kasa.hareketler') }}</h2>
        <Button
          v-if="yazabilir"
          :label="t('kasa.gelirEkle')"
          icon="pi pi-plus-circle"
          class="p-button-success p-button-sm"
          @click="openHareketDialog('GELIR')"
        />
        <Button
          v-if="yazabilir"
          :label="t('kasa.giderEkle')"
          icon="pi pi-minus-circle"
          class="p-button-danger p-button-sm"
          @click="openHareketDialog('GIDER')"
        />
      </div>

      <div class="table-container">
        <div class="hareket-filtre">
          <Select
            v-model="hareketTurFiltre"
            :options="hareketTurSecenekleri"
            option-label="label"
            option-value="value"
            :placeholder="t('kasa.tumTurler')"
            show-clear
            class="hareket-filtre-alan"
          />
          <DatePicker
            v-model="hareketTarihFiltre"
            selection-mode="range"
            date-format="dd.mm.yy"
            :placeholder="t('kasa.tarihAraligi')"
            class="hareket-filtre-alan"
          />
          <span class="hareket-toplam">
            {{ t('kasa.hareketSayisi') }}: <strong>{{ filtreliHareketler.length }}</strong>
          </span>
        </div>
        <AppDataTable
          :value="filtreliHareketler"
          striped-rows
          :rows="10"
          :paginator="true"
          paginator-template="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink CurrentPageReport"
          :current-page-report-template="'{first} - {last} ({totalRecords} ' + $t('common.recordsWord') + ')'"
          gorunum-anahtari="kasa-hareketler"
        >
          <Column
            field="tarih"
            :header="t('common.date')"
            style="width: 100px"
            sortable
          >
            <template #body="s">
              {{ formatDate(s.data.hareketTarihi) }}
            </template>
          </Column>
          <Column
            field="tur"
            :header="t('kasa.tur')"
            style="width: 80px"
          >
            <template #body="s">
              <span :class="['badge', s.data.tur === 'GELIR' ? 'gelir' : 'gider']">
                {{ s.data.tur === 'GELIR' ? t('kasa.gelir') : t('kasa.gider') }}
              </span>
            </template>
          </Column>
          <Column
            field="tutar"
            :header="t('common.amount')"
            style="width: 120px"
            sortable
          >
            <template #body="s">
              <span
                v-if="bakiyeGorunur"
                class="gizli-veri"
                :class="s.data.tur === 'GELIR' ? 'positive' : 'negative'"
              >
                {{ formatCurrency(s.data.tutar) }}
              </span>
              <span v-else>••••</span>
            </template>
          </Column>
          <Column
            field="kategoriAd"
            :header="t('kasa.kategori')"
            style="width: 140px"
          >
            <template #body="s">
              {{ s.data.kategoriAd || '-' }}
            </template>
          </Column>
          <Column
            field="aciklama"
            :header="t('common.description')"
          />
          <Column
            header=""
            style="width: 60px"
          >
            <template #body="s">
              <Button
                v-if="silebilir"
                icon="pi pi-trash"
                :aria-label="$t('common.delete')"
                class="p-button-rounded p-button-danger p-button-sm"
                @click="delHareket(s.data.id)"
              />
            </template>
          </Column>
          <template #empty>
            <EmptyState
              icon="pi pi-wallet"
              :message="t('kasa.hareketYok')"
            />
          </template>
        </AppDataTable>
      </div>
    </div>

    <Dialog
      v-model:visible="showHareketDialog"
      :header="hareketBaslik"
      :modal="true"
      style="width: 500px"
      :breakpoints="{ '575px': '92vw' }"
    >
      <div class="form-group">
        <label>{{ t('kasa.tutar') }}</label>
        <InputNumber
          v-model="hareketForm.tutar"
          :min="0.01"
          :min-fraction-digits="2"
          :max-fraction-digits="2"
          :class="{ 'p-invalid': hareketHatalar.tutar }"
          class="w-full"
          @input="hareketHatalar.tutar = ''"
        />
        <small
          v-if="hareketHatalar.tutar"
          class="p-error"
        >{{ hareketHatalar.tutar }}</small>
      </div>
      <div class="form-group">
        <label>{{ t('kasa.tarihZorunlu') }}</label>
        <DatePicker
          v-model="hareketForm.hareketTarihi"
          date-format="dd.mm.yy"
          class="w-full"
        />
      </div>
      <div class="form-group">
        <label>{{ t('kasa.kategori') }}</label>
        <Dropdown
          v-model="hareketForm.kategoriId"
          :options="kategoriSecenekler"
          option-label="ad"
          option-value="id"
          :placeholder="t('faturalar.seciniz')"
          class="w-full"
        />
      </div>
      <div class="form-group">
        <label>{{ t('common.description') }}</label>
        <Textarea
          v-model="hareketForm.aciklama"
          rows="2"
          class="w-full"
        />
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          icon="pi pi-times"
          class="p-button-text"
          @click="showHareketDialog = false"
        />
        <Button
          :label="t('common.save')"
          icon="pi pi-check"
          :loading="saving"
          @click="saveHareket"
        />
      </template>
    </Dialog>

    <Dialog
      v-model:visible="showAktarDialog"
      :header="t('kasa.kasaAktar')"
      :modal="true"
      style="width: 480px"
      :breakpoints="{ '575px': '92vw' }"
    >
      <div class="form-group">
        <label>{{ t('kasa.kaynakKasa') }}</label>
        <Dropdown
          v-model="aktarForm.kaynakKasaId"
          :options="kasaStore.kasalar"
          option-label="ad"
          option-value="id"
          :placeholder="t('faturalar.seciniz')"
          class="w-full"
        />
      </div>
      <div class="form-group">
        <label>{{ t('kasa.hedefKasa') }}</label>
        <Dropdown
          v-model="aktarForm.hedefKasaId"
          :options="kasaStore.kasalar"
          option-label="ad"
          option-value="id"
          :placeholder="t('faturalar.seciniz')"
          class="w-full"
          :invalid="kasaAktarAyni"
        />
        <small
          v-if="kasaAktarAyni"
          class="alan-hata"
        >{{ t('kasa.kaynakHedefAyni') }}</small>
      </div>
      <div class="form-group">
        <label>{{ t('kasa.tutar') }}</label>
        <InputNumber
          v-model="aktarForm.tutar"
          :min="0.01"
          :min-fraction-digits="2"
          :max-fraction-digits="2"
          class="w-full"
        />
      </div>
      <div
        v-if="aktarForm.kaynakKasaId"
        class="transfer-etki"
      >
        <div class="etki-satir">
          <span>{{ t('kasa.mevcutBakiye') }}</span>
          <strong>{{ bakiyeGorunur ? formatCurrency(kasaAktarKaynakBakiye) : '••••' }}</strong>
        </div>
        <div class="etki-satir">
          <span>{{ t('kasa.transferSonrasi') }}</span>
          <strong
            class="gizli-veri"
            :class="{ negative: kasaAktarSonrasi < 0 }"
          >{{ bakiyeGorunur ? formatCurrency(kasaAktarSonrasi) : '••••' }}</strong>
        </div>
      </div>
      <div class="form-group">
        <label>{{ t('common.description') }}</label>
        <InputText
          v-model="aktarForm.aciklama"
          :placeholder="t('kasa.ornekSubeDevir')"
          class="w-full"
        />
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          icon="pi pi-times"
          class="p-button-text"
          @click="showAktarDialog = false"
        />
        <Button
          :label="t('kasa.aktar')"
          icon="pi pi-check"
          :loading="saving"
          :disabled="!kasaAktarGecerli"
          @click="saveAktar"
        />
      </template>
    </Dialog>

    <Dialog
      v-model:visible="showBankaAktarDialog"
      :header="t('kasa.kasadanBankaya')"
      :modal="true"
      style="width: 480px"
      :breakpoints="{ '575px': '92vw' }"
    >
      <div class="form-group">
        <label>{{ t('kasa.kaynakKasa') }}</label>
        <Dropdown
          v-model="bankaAktarForm.kasaId"
          :options="kasaStore.kasalar"
          option-label="ad"
          option-value="id"
          :placeholder="t('faturalar.seciniz')"
          class="w-full"
        />
      </div>
      <div class="form-group">
        <label>{{ t('kasa.hedefBanka') }}</label>
        <Dropdown
          v-model="bankaAktarForm.bankaId"
          :options="bankalar"
          option-label="ad"
          option-value="id"
          filter
          :placeholder="t('faturalar.seciniz')"
          class="w-full"
        />
      </div>
      <div class="form-group">
        <label>{{ t('kasa.tutar') }}</label>
        <InputNumber
          v-model="bankaAktarForm.tutar"
          :min="0.01"
          :min-fraction-digits="2"
          :max-fraction-digits="2"
          class="w-full"
        />
      </div>
      <div
        v-if="bankaAktarForm.kasaId"
        class="transfer-etki"
      >
        <div class="etki-satir">
          <span>{{ t('kasa.mevcutBakiye') }}</span>
          <strong>{{ bakiyeGorunur ? formatCurrency(bankaAktarKaynakBakiye) : '••••' }}</strong>
        </div>
        <div class="etki-satir">
          <span>{{ t('kasa.transferSonrasi') }}</span>
          <strong
            class="gizli-veri"
            :class="{ negative: bankaAktarSonrasi < 0 }"
          >{{ bakiyeGorunur ? formatCurrency(bankaAktarSonrasi) : '••••' }}</strong>
        </div>
      </div>
      <div class="form-group">
        <label>{{ t('common.description') }}</label>
        <InputText
          v-model="bankaAktarForm.aciklama"
          :placeholder="t('kasa.ornekGunSonu')"
          class="w-full"
        />
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          icon="pi pi-times"
          class="p-button-text"
          @click="showBankaAktarDialog = false"
        />
        <Button
          :label="t('kasa.aktar')"
          icon="pi pi-check"
          :loading="saving"
          :disabled="!bankaAktarGecerli"
          @click="saveBankaAktar"
        />
      </template>
    </Dialog>

    <Dialog
      v-model:visible="showBankadanAktarDialog"
      :header="t('kasa.bankadanKasaya')"
      :modal="true"
      style="width: 480px"
      :breakpoints="{ '575px': '92vw' }"
    >
      <div class="form-group">
        <label>{{ t('kasa.kaynakBanka') }}</label>
        <Dropdown
          v-model="bankadanAktarForm.bankaId"
          :options="bankalar"
          option-label="ad"
          option-value="id"
          filter
          :placeholder="t('faturalar.seciniz')"
          class="w-full"
        />
      </div>
      <div class="form-group">
        <label>{{ t('kasa.hedefKasa') }}</label>
        <Dropdown
          v-model="bankadanAktarForm.kasaId"
          :options="kasaStore.kasalar"
          option-label="ad"
          option-value="id"
          :placeholder="t('faturalar.seciniz')"
          class="w-full"
        />
      </div>
      <div class="form-group">
        <label>{{ t('kasa.tutar') }}</label>
        <InputNumber
          v-model="bankadanAktarForm.tutar"
          :min="0.01"
          :min-fraction-digits="2"
          :max-fraction-digits="2"
          class="w-full"
        />
      </div>
      <div
        v-if="bankadanAktarForm.bankaId"
        class="transfer-etki"
      >
        <div class="etki-satir">
          <span>{{ t('kasa.mevcutBakiye') }}</span>
          <strong>{{ bakiyeGorunur ? formatCurrency(bankadanAktarKaynakBakiye) : '••••' }}</strong>
        </div>
        <div class="etki-satir">
          <span>{{ t('kasa.transferSonrasi') }}</span>
          <strong
            class="gizli-veri"
            :class="{ negative: bankadanAktarSonrasi < 0 }"
          >{{ bakiyeGorunur ? formatCurrency(bankadanAktarSonrasi) : '••••' }}</strong>
        </div>
      </div>
      <div class="form-group">
        <label>{{ t('common.description') }}</label>
        <InputText
          v-model="bankadanAktarForm.aciklama"
          :placeholder="t('kasa.ornekGunSonu')"
          class="w-full"
        />
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          icon="pi pi-times"
          class="p-button-text"
          @click="showBankadanAktarDialog = false"
        />
        <Button
          :label="t('kasa.aktar')"
          icon="pi pi-check"
          :loading="saving"
          :disabled="!bankadanAktarGecerli"
          @click="saveBankadanAktar"
        />
      </template>
    </Dialog>

    <KasaGunSonuDialog ref="gunSonuRef" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { unwrapList } from '../api/utils/unwrap.js'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useConfirm } from 'primevue/useconfirm'
import { useKasaStore } from '../stores/kasaStore.js'
import { useKategoriStore } from '../stores/kategoriStore.js'
import { useAuthStore } from '../stores/authStore.js'
import { kasaAPI, excelAPI, bankaAPI } from '../api/index.js'
import EmptyState from '../components/EmptyState.vue'
import KpiKart from '../components/KpiKart.vue'
import KasaGunSonuDialog from '../components/KasaGunSonuDialog.vue'
import { formatCurrency, getLocalDateString, formatTarih as formatDate } from '../utils/format.js'
import { useI18n } from 'vue-i18n'

const toastBildirim = useToastBildirim()
const confirm = useConfirm()
const { t } = useI18n()
const kasaStore = useKasaStore()
const kategoriStore = useKategoriStore()
const authStore = useAuthStore()

const seciliKasaId = ref(null)
const seciliKasa = ref(null)
const kasaHareketler = ref([])
const saving = ref(false)
const excelYukleniyor = ref(false)
const transferMenu = ref(null)

// Yetki kapilari — backend ile hizali (403 onlenir).
const yazabilir = computed(() =>
  authStore.isAdmin || authStore.kullanici?.role === 'MUHASEBE' || authStore.hasPermission('FINANS_WRITE'))
const silebilir = computed(() => authStore.isAdmin || authStore.hasPermission('FINANS_DELETE'))
const aktarabilir = computed(() => authStore.isAdmin || authStore.kullanici?.role === 'MUHASEBE')

// KPI ozeti.
const ozet = ref({ toplamKasa: 0, toplamBakiye: 0, bugunGelir: 0, bugunGider: 0 })
const ozetYukle = async () => {
  try {
    const r = await kasaAPI.ozet()
    ozet.value = { ...ozet.value, ...(r.data || {}) }
  } catch {
    /* ozet alinamazsa KPI sifirda kalir */
  }
}

// Bakiye gizle/goster (memur ekrani icin); tercih hatirlanir.
const BAKIYE_ANAHTAR = 'raspel_kasa_bakiye_gorunur'
const bakiyeGorunur = ref(localStorage.getItem(BAKIYE_ANAHTAR) !== '0')
const bakiyeGorunurlukDegistir = () => {
  bakiyeGorunur.value = !bakiyeGorunur.value
  localStorage.setItem(BAKIYE_ANAHTAR, bakiyeGorunur.value ? '1' : '0')
}

// Aktarimlar tek menude toplanir.
const transferMenuModel = computed(() => [
  { label: t('kasa.bankadanAktar'), icon: 'pi pi-building-columns', command: () => openBankadanAktarDialog() },
  { label: t('kasa.bankayaAktar'), icon: 'pi pi-building', command: () => openBankaAktarDialog() },
  { label: t('kasa.kasaAktar'), icon: 'pi pi-arrow-right-arrow-left', command: () => openAktarDialog() }
])

const showKasaDialog = ref(false)
const editingKasaId = ref(null)
const kasaForm = ref({ ad: '', bakiye: 0 })

const showHareketDialog = ref(false)
const hareketTur = ref('GELIR')
const hareketForm = ref({ tutar: null, hareketTarihi: new Date(), kategoriId: null, aciklama: '' })
const hareketHatalar = ref({ tutar: '' })

const showAktarDialog = ref(false)
const aktarForm = ref({ kaynakKasaId: null, hedefKasaId: null, tutar: null, aciklama: '' })

const showBankaAktarDialog = ref(false)
const bankaAktarForm = ref({ kasaId: null, bankaId: null, tutar: null, aciklama: '' })
const showBankadanAktarDialog = ref(false)
const bankadanAktarForm = ref({ bankaId: null, kasaId: null, tutar: null, aciklama: '' })
const bankalar = ref([])

// Aktarim sonrasi secili kasanin bakiyesini ve hareket listesini tazele.
const hareketleriTazele = async () => {
  await kasaStore.getAllKasalar()
  const guncel = kasaStore.kasalar.find((k) => k.id === seciliKasaId.value)
  if (guncel) seciliKasa.value = guncel
  if (seciliKasaId.value) {
    const r = await kasaAPI.getHareketler(seciliKasaId.value)
    kasaHareketler.value = r.data
  }
  ozetYukle()
}

const hareketBaslik = computed(() => (hareketTur.value === 'GELIR' ? t('kasa.gelirEkleBaslik') : t('kasa.giderEkleBaslik')))

// Hareket tablosu filtreleri (tur + tarih araligi).
const hareketTurFiltre = ref(null)
const hareketTarihFiltre = ref(null)
const hareketTurSecenekleri = computed(() => [
  { label: t('kasa.gelir'), value: 'GELIR' },
  { label: t('kasa.gider'), value: 'GIDER' }
])
const filtreliHareketler = computed(() =>
  kasaHareketler.value.filter((h) => {
    if (hareketTurFiltre.value && h.tur !== hareketTurFiltre.value) return false
    if (Array.isArray(hareketTarihFiltre.value) && hareketTarihFiltre.value.length === 2 && hareketTarihFiltre.value[0]) {
      const tr = h.hareketTarihi || ''
      if (tr < getLocalDateString(hareketTarihFiltre.value[0]) || tr > getLocalDateString(hareketTarihFiltre.value[1])) return false
    }
    return true
  })
)

const kategoriSecenekler = computed(() => kategoriStore.kategoriler.filter((k) => k.tur === hareketTur.value))

// Aktarim dogrulama + bakiye onizlemesi.
const kasaAktarKaynakBakiye = computed(() => Number(kasaStore.kasalar.find((k) => k.id === aktarForm.value.kaynakKasaId)?.bakiye) || 0)
const kasaAktarAyni = computed(() => !!aktarForm.value.kaynakKasaId && aktarForm.value.kaynakKasaId === aktarForm.value.hedefKasaId)
const kasaAktarSonrasi = computed(() => kasaAktarKaynakBakiye.value - (Number(aktarForm.value.tutar) || 0))
const kasaAktarGecerli = computed(() =>
  !!aktarForm.value.kaynakKasaId && !!aktarForm.value.hedefKasaId && !kasaAktarAyni.value
  && Number(aktarForm.value.tutar) > 0 && Number(aktarForm.value.tutar) <= kasaAktarKaynakBakiye.value)

const bankaAktarKaynakBakiye = computed(() => Number(kasaStore.kasalar.find((k) => k.id === bankaAktarForm.value.kasaId)?.bakiye) || 0)
const bankaAktarSonrasi = computed(() => bankaAktarKaynakBakiye.value - (Number(bankaAktarForm.value.tutar) || 0))
const bankaAktarGecerli = computed(() =>
  !!bankaAktarForm.value.kasaId && !!bankaAktarForm.value.bankaId
  && Number(bankaAktarForm.value.tutar) > 0 && Number(bankaAktarForm.value.tutar) <= bankaAktarKaynakBakiye.value)

const bankadanAktarKaynakBakiye = computed(() => Number(bankalar.value.find((b) => b.id === bankadanAktarForm.value.bankaId)?.bakiye) || 0)
const bankadanAktarSonrasi = computed(() => bankadanAktarKaynakBakiye.value - (Number(bankadanAktarForm.value.tutar) || 0))
const bankadanAktarGecerli = computed(() =>
  !!bankadanAktarForm.value.bankaId && !!bankadanAktarForm.value.kasaId
  && Number(bankadanAktarForm.value.tutar) > 0 && Number(bankadanAktarForm.value.tutar) <= bankadanAktarKaynakBakiye.value)

onMounted(async () => {
  // Store cagrilari hata firlatabilir; bir hata diger yuklemeyi durdurmasin.
  await Promise.allSettled([kasaStore.getAllKasalar(), kategoriStore.getAllKategoriler()])
  try {
    const r = await bankaAPI.getAll({ size: 500 })
    bankalar.value = unwrapList(r)
  } catch {
    bankalar.value = []
  }
  ozetYukle()
})

const kasaSec = async (kasa) => {
  seciliKasaId.value = kasa.id
  seciliKasa.value = kasa
  hareketTurFiltre.value = null
  hareketTarihFiltre.value = null
  try {
    const r = await kasaAPI.getHareketler(kasa.id)
    kasaHareketler.value = r.data
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('kasa.hareketYuklenemedi'))
  }
}

const openKasaDialog = () => {
  editingKasaId.value = null
  kasaForm.value = { ad: '', bakiye: 0 }
  showKasaDialog.value = true
}

const editKasa = (kasa) => {
  editingKasaId.value = kasa.id
  kasaForm.value = { ad: kasa.ad, bakiye: 0 }
  showKasaDialog.value = true
}

// Kart ⋮ menusu: hareketleri gor (sec); duzenle/sil yerlesik gelir.
const kasaMenuItems = (kasa) => [
  { etiket: t('kasa.hareketleriGor'), ikon: 'pi pi-list', islem: () => kasaSec(kasa) }
]

const saveKasa = async () => {
  // Cift gonderim engeli: ayni kasa kaydi iki kez olusmasin.
  if (saving.value) return
  if (!kasaForm.value.ad.trim()) {
    toastBildirim.uyari(t('kasa.kasaAdiGiriniz'))
    return
  }
  saving.value = true
  try {
    if (editingKasaId.value) {
      await kasaStore.updateKasa(editingKasaId.value, kasaForm.value)
      toastBildirim.basarili(t('kasa.kasaGuncellendi'))
    } else {
      await kasaStore.addKasa(kasaForm.value)
      toastBildirim.basarili(t('kasa.kasaOlusturuldu'))
    }
    showKasaDialog.value = false
    ozetYukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('kasa.islemBasarisiz'))
  } finally {
    saving.value = false
  }
}

const confirmDel = (id) => {
  confirm.require({
    message: t('kasa.silOnayMesaj'),
    header: t('common.silmeOnayi'),
    icon: 'pi pi-exclamation-triangle',
    rejectProps: { label: t('common.vazgec'), severity: 'secondary', outlined: true, size: 'small' },
    acceptProps: { label: t('common.evetSil'), severity: 'danger', size: 'small' },
    accept: async () => {
      try {
        await kasaStore.deleteKasa(id)
        if (seciliKasaId.value === id) {
          seciliKasaId.value = null
          seciliKasa.value = null
          kasaHareketler.value = []
        }
        toastBildirim.basarili(t('kasa.kasaSilindi'))
        ozetYukle()
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || err?.message || t('kasa.silmeBasarisiz'))
      }
    }
  })
}

const openHareketDialog = (tur) => {
  hareketTur.value = tur
  hareketForm.value = { tutar: null, hareketTarihi: new Date(), kategoriId: null, aciklama: '' }
  showHareketDialog.value = true
}

const saveHareket = async () => {
  // Cift gonderim engeli: ayni kasa hareketi iki kez yazilmasin.
  if (saving.value) return
  hareketHatalar.value = { tutar: '' }
  if (!hareketForm.value.tutar || hareketForm.value.tutar <= 0) {
    hareketHatalar.value.tutar = t('kasa.gecerliTutar')
    return
  }
  saving.value = true
  try {
    await kasaAPI.addHareket(seciliKasaId.value, {
      tur: hareketTur.value,
      tutar: hareketForm.value.tutar,
      hareketTarihi: getLocalDateString(hareketForm.value.hareketTarihi),
      kategoriId: hareketForm.value.kategoriId,
      aciklama: hareketForm.value.aciklama
    })
    const r = await kasaAPI.getHareketler(seciliKasaId.value)
    kasaHareketler.value = r.data
    await kasaStore.getAllKasalar()
    const guncel = kasaStore.kasalar.find((k) => k.id === seciliKasaId.value)
    if (guncel) seciliKasa.value = guncel
    showHareketDialog.value = false
    toastBildirim.basarili(t('kasa.hareketEklendi'))
    ozetYukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('kasa.islemBasarisiz'))
  } finally {
    saving.value = false
  }
}

const delHareket = (id) => {
  confirm.require({
    message: t('common.silmeOnayMesaji'),
    header: t('common.silmeOnayi'),
    icon: 'pi pi-exclamation-triangle',
    rejectProps: { label: t('common.vazgec'), severity: 'secondary', outlined: true, size: 'small' },
    acceptProps: { label: t('common.evetSil'), severity: 'danger', size: 'small' },
    accept: async () => {
      try {
        await kasaAPI.deleteHareket(id)
        kasaHareketler.value = kasaHareketler.value.filter((h) => h.id !== id)
        await kasaStore.getAllKasalar()
        const guncel = kasaStore.kasalar.find((k) => k.id === seciliKasaId.value)
        if (guncel) seciliKasa.value = guncel
        toastBildirim.basarili(t('kasa.hareketSilindi'))
        ozetYukle()
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || err?.message || t('kasa.silmeBasarisiz'))
      }
    }
  })
}

const openAktarDialog = () => {
  aktarForm.value = { kaynakKasaId: null, hedefKasaId: null, tutar: null, aciklama: '' }
  showAktarDialog.value = true
}

const openBankaAktarDialog = () => {
  bankaAktarForm.value = { kasaId: null, bankaId: null, tutar: null, aciklama: '' }
  showBankaAktarDialog.value = true
}

const openBankadanAktarDialog = () => {
  bankadanAktarForm.value = { bankaId: null, kasaId: null, tutar: null, aciklama: '' }
  showBankadanAktarDialog.value = true
}

const saveBankadanAktar = async () => {
  if (!bankadanAktarForm.value.bankaId || !bankadanAktarForm.value.kasaId) {
    toastBildirim.uyari(t('kasa.kaynakHedefSecin'))
    return
  }
  if (!bankadanAktarForm.value.tutar || bankadanAktarForm.value.tutar <= 0) {
    toastBildirim.uyari(t('kasa.gecerliTutarGiriniz'))
    return
  }
  if (Number(bankadanAktarForm.value.tutar) > bankadanAktarKaynakBakiye.value) {
    toastBildirim.uyari(t('kasa.yetersizBakiye'))
    return
  }
  saving.value = true
  try {
    await kasaAPI.bankadanAktar(bankadanAktarForm.value)
    showBankadanAktarDialog.value = false
    toastBildirim.basarili(t('kasa.bankadanAktarildi'))
    await hareketleriTazele()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('kasa.aktarimBasarisiz'))
  } finally {
    saving.value = false
  }
}

const saveBankaAktar = async () => {
  if (!bankaAktarForm.value.kasaId || !bankaAktarForm.value.bankaId) {
    toastBildirim.uyari(t('kasa.kaynakHedefSecin'))
    return
  }
  if (!bankaAktarForm.value.tutar || bankaAktarForm.value.tutar <= 0) {
    toastBildirim.uyari(t('kasa.gecerliTutarGiriniz'))
    return
  }
  if (Number(bankaAktarForm.value.tutar) > bankaAktarKaynakBakiye.value) {
    toastBildirim.uyari(t('kasa.yetersizBakiye'))
    return
  }
  saving.value = true
  try {
    await kasaAPI.bankayaAktar(bankaAktarForm.value)
    showBankaAktarDialog.value = false
    toastBildirim.basarili(t('kasa.bankayaAktarildi'))
    await hareketleriTazele()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('kasa.aktarimBasarisiz'))
  } finally {
    saving.value = false
  }
}

const gunSonuRef = ref(null)

const saveAktar = async () => {
  if (!aktarForm.value.kaynakKasaId || !aktarForm.value.hedefKasaId) {
    toastBildirim.uyari(t('kasa.kaynakHedefKasa'))
    return
  }
  if (kasaAktarAyni.value) {
    toastBildirim.uyari(t('kasa.kaynakHedefAyni'))
    return
  }
  if (!aktarForm.value.tutar || aktarForm.value.tutar <= 0) {
    toastBildirim.uyari(t('kasa.gecerliTutar'))
    return
  }
  if (Number(aktarForm.value.tutar) > kasaAktarKaynakBakiye.value) {
    toastBildirim.uyari(t('kasa.yetersizBakiye'))
    return
  }
  saving.value = true
  try {
    await kasaAPI.aktar(aktarForm.value)
    showAktarDialog.value = false
    await hareketleriTazele()
    toastBildirim.basarili(t('kasa.kasaAktarildi'))
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('kasa.aktarimBasarisiz'))
  } finally {
    saving.value = false
  }
}

const excelIndir = async () => {
  excelYukleniyor.value = true
  try {
    const res = await excelAPI.kasalar()
    const url = window.URL.createObjectURL(new Blob([res.data]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', 'Kasalar.xlsx')
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)
    toastBildirim.basarili(t('kasa.excelIndirildi'))
  } catch {
    toastBildirim.hata(t('kasa.excelHata'))
  } finally {
    excelYukleniyor.value = false
  }
}
</script>

<style scoped>
.kasa-container {
  padding: 0;
  max-width: 100%;
}
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(170px, 100%), 1fr));
  gap: 12px;
  margin-bottom: 1rem;
}
h2 {
  color: var(--text-primary);
  font-size: 20px;
  margin: 0;
}
.loading {
  text-align: center;
  padding: 40px;
  color: var(--text-muted);
}
.kasa-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(min(280px, 100%), 1fr));
  gap: 15px;
  margin-bottom: 30px;
}
.kasa-card {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 14px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.2);
}
.kasa-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.3);
  border-color: var(--accent-soft-strong);
}
.kasa-card.active {
  border-color: var(--accent);
  background: rgba(59, 130, 246, 0.08);
}
.kasa-ust {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 15px;
}
.kasa-ust i {
  font-size: 28px;
  color: var(--accent);
}
.kasa-ust h3 {
  margin: 0;
  font-size: 18px;
}
.secili-ikon {
  margin-left: auto;
  color: var(--accent);
  font-size: 20px;
}
.kasa-bakiye {
  font-size: 24px;
  font-weight: bold;
  margin: 0 0 15px 0;
}
.kasa-islem {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
.positive {
  color: var(--success);
}
.negative {
  color: var(--danger);
}
.hareket-bolumu {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 14px;
  padding: 20px;
}
.hareket-header {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}
.form-group {
  margin-bottom: 20px;
}
.form-group label {
  display: block;
  margin-bottom: 6px;
  font-weight: bold;
  color: var(--text-secondary);
  font-size: 13px;
}
.badge {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: bold;
}
.badge.gelir {
  background: var(--success-soft);
  color: var(--success);
}
.badge.gider {
  background: var(--danger-soft);
  color: var(--danger);
}
.w-full {
  width: 100% !important;
}
.gun-sonu {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.full-width {
  grid-column: 1 / -1;
}
.hareket-filtre {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}
.hareket-filtre-alan {
  flex: 1 1 180px;
  min-width: 0;
}
.hareket-toplam {
  font-size: 0.82rem;
  color: var(--text-secondary);
}
.transfer-etki {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 10px 12px;
  margin-bottom: 16px;
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
  margin-top: 6px;
  color: var(--danger, #ef4444);
  font-size: 0.75rem;
}

/* Mobil: hareket filtreleri alt alta, kart aksiyonlari sola. */
@media (max-width: 640px) {
  .hareket-filtre {
    flex-direction: column;
    align-items: stretch;
  }
  .hareket-filtre-alan {
    flex: 1 1 100%;
  }
  .kasa-islem {
    justify-content: flex-start;
  }
}
</style>
