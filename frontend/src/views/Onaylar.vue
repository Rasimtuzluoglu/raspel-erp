<template>
  <div class="onaylar-sayfasi">
    <PageHeader
      :title="t('onaylar.title')"
      :description="t('onaylar.altBaslik')"
      icon="pi pi-check-circle"
    >
      <template #actions>
        <Button
          :label="t('onaylar.onayAyarlariBaglanti')"
          icon="pi pi-sliders-h"
          outlined
          size="small"
          @click="ayarlaraGit"
        />
        <Button
          :label="t('onaylar.excel')"
          icon="pi pi-file-excel"
          outlined
          size="small"
          :loading="excelYukleniyor"
          :disabled="gecmisMi"
          @click="excelIndir"
        />
        <Button
          :label="t('onaylar.yenile')"
          icon="pi pi-refresh"
          text
          size="small"
          :loading="yukleniyor || gecmisYukleniyor"
          @click="yenile"
        />
      </template>
    </PageHeader>

    <!-- KPI kartları -->
    <div class="kpi-satiri">
      <template v-if="ozetIskelet">
        <div
          v-for="i in 5"
          :key="i"
          class="kpi-iskelet"
        >
          <SkeletonLoader height="92px" />
        </div>
      </template>
      <template v-else>
        <button
          v-for="kpi in kpiKartlari"
          :key="kpi.key"
          type="button"
          class="kpi-tikla"
          :aria-pressed="kpiAktif(kpi)"
          @click="kpiTikla(kpi)"
        >
          <KpiKart
            :baslik="kpi.baslik"
            :deger="kpi.deger"
            :ikon="kpi.ikon"
            :renk="kpi.renk"
            :para-birimi="false"
          >
            <template #alt>
              {{ kpi.alt }}
            </template>
          </KpiKart>
        </button>
      </template>
    </div>

    <p
      class="sr-only"
      aria-live="polite"
    >
      {{ canliDuyuru }}
    </p>

    <!-- Sekmeler -->
    <div
      class="onay-sekmeleri"
      role="tablist"
      :aria-label="t('onaylar.sekmeler')"
    >
      <button
        v-for="(sekme, index) in sekmeler"
        :id="`onay-sekme-${sekme.key}`"
        :key="sekme.key"
        type="button"
        role="tab"
        class="onay-sekme"
        :class="{ aktif: aktifSekme === index }"
        :aria-selected="aktifSekme === index"
        :aria-controls="`onay-panel-${sekme.key}`"
        :tabindex="aktifSekme === index ? 0 : -1"
        @click="sekmeSec(index)"
        @keydown="sekmeTus($event, index)"
      >
        <i :class="sekme.ikon" />
        <span>{{ t(`onaylar.${sekme.i18n}`) }}</span>
        <span
          v-if="sekmeSayisi(sekme) > 0"
          class="sekme-rozet"
          :aria-label="t('onaylar.sekmeSayisi', { n: sekmeSayisi(sekme) })"
        >{{ sekmeSayisi(sekme) }}</span>
      </button>
    </div>

    <!-- Bekleyen onaylar -->
    <section
      v-if="!gecmisMi"
      :id="`onay-panel-${aktifSekmeKey}`"
      role="tabpanel"
      :aria-labelledby="`onay-sekme-${aktifSekmeKey}`"
      class="onay-panel"
    >
      <div class="onay-arac">
        <IconField class="filtre-arama">
          <InputIcon class="pi pi-search" />
          <InputText
            v-model="q"
            :placeholder="t('onaylar.aramaPlaceholder')"
            :aria-label="t('onaylar.aramaPlaceholder')"
            @input="aramaDegisti"
          />
        </IconField>
        <span class="arac-bosluk" />
        <small
          v-if="!turSecili"
          class="muted"
        >{{ t('onaylar.tumuIpucu') }}</small>
        <Button
          v-if="filtreVar"
          :label="t('onaylar.filtreTemizle')"
          icon="pi pi-filter-slash"
          text
          size="small"
          @click="filtreTemizle"
        />
      </div>

      <!-- Mobil toplu aksiyon çubuğu -->
      <div
        v-if="secim.length"
        class="toplu-cubuk mobil-toplu"
      >
        <span class="toplu-bilgi">
          <i class="pi pi-check-square" />
          {{ t('onaylar.seciliKayit', { n: secim.length }) }}
        </span>
        <div class="toplu-butonlar">
          <Button
            :label="t('onaylar.topluOnayla')"
            icon="pi pi-check"
            severity="success"
            size="small"
            :loading="islemde"
            @click="topluOnayla"
          />
          <Button
            :label="t('onaylar.topluReddet')"
            icon="pi pi-times"
            severity="danger"
            outlined
            size="small"
            :loading="islemde"
            @click="topluRetAc"
          />
          <Button
            icon="pi pi-times"
            text
            size="small"
            :aria-label="t('onaylar.secimiKaldir')"
            @click="secim = []"
          />
        </div>
      </div>

      <SkeletonLoader
        v-if="listeIskelet"
        class="tablo-iskelet"
        :count="6"
        height="46px"
      />

      <AppDataTable
        v-else
        :value="liste"
        :lazy="turSecili"
        data-key="anahtar"
        :total-records="turSecili ? toplam : liste.length"
        :rows="boyut"
        :loading="yukleniyor"
        striped-rows
        class="onay-masa"
        gorunum-anahtari="onaylar-bekleyen"
        :selection="secim"
        :is-row-selectable="satirSecilebilir"
        :empty-message="emptyMesaj"
        @update:selection="secimDegisti"
        @page="sayfaDegisti"
        @sort="siralaDegisti"
        @row-click="satirTiklandi"
      >
        <template #batch-actions>
          <Button
            :label="t('onaylar.topluOnayla')"
            icon="pi pi-check"
            severity="success"
            size="small"
            class="toplu-onay-btn"
            :loading="islemde"
            @click="topluOnayla"
          />
          <Button
            :label="t('onaylar.topluReddet')"
            icon="pi pi-times"
            severity="danger"
            outlined
            size="small"
            class="toplu-red-btn"
            :loading="islemde"
            @click="topluRetAc"
          />
        </template>

        <template #empty>
          <EmptyState
            v-if="!yukleniyor"
            :message="emptyMesaj"
            :sub-message="filtreVar ? null : t('onaylar.bekleyenYokIpucu')"
            icon="pi pi-check-circle"
          />
        </template>

        <Column
          selection-mode="multiple"
          style="width: 3rem"
        />
        <Column
          field="tur"
          :header="t('onaylar.tur')"
          style="width: 110px"
          sortable
        >
          <template #body="{ data }">
            <Tag
              :value="turAdi(data.tur)"
              :severity="turSeverity(data.tur)"
            />
          </template>
        </Column>
        <Column
          field="baslik"
          :header="t('onaylar.baslik')"
          sortable
        >
          <template #body="{ data }">
            <div class="kayit-baslik">
              {{ data.baslik || '-' }}
            </div>
            <small
              v-if="data.altBaslik"
              class="kayit-alt"
            >{{ data.altBaslik }}</small>
          </template>
        </Column>
        <Column
          field="tutar"
          :header="t('onaylar.tutar')"
          style="width: 140px"
          sortable
        >
          <template #body="{ data }">
            <span class="gizli-veri">{{ tutarMetni(data.tutar) }}</span>
          </template>
        </Column>
        <Column
          field="tarih"
          :header="t('onaylar.tarih')"
          style="width: 120px"
          sortable
        >
          <template #body="{ data }">
            {{ data.tarih ? formatDate(data.tarih) : '-' }}
          </template>
        </Column>
        <Column
          field="durum"
          :header="t('onaylar.durum')"
          style="width: 130px"
          sortable
        >
          <template #body="{ data }">
            <Tag
              :value="durumAdi(data.durum)"
              :severity="durumSeverity(data.durum)"
            />
          </template>
        </Column>
        <Column
          :header="t('onaylar.islemler')"
          style="width: 140px"
        >
          <template #body="{ data }">
            <div
              class="satir-aksiyonlar"
              @click.stop
            >
              <Button
                icon="pi pi-eye"
                severity="secondary"
                text
                rounded
                size="small"
                :aria-label="`${data.baslik || data.kaynakId} ${t('onaylar.detayiAc')}`"
                @click="detayAc(data)"
              />
              <Button
                icon="pi pi-check"
                severity="success"
                text
                rounded
                size="small"
                :aria-label="`${data.baslik || data.kaynakId} ${t('onaylar.onayla')}`"
                @click="tekOnayla(data)"
              />
              <Button
                icon="pi pi-times"
                severity="danger"
                text
                rounded
                size="small"
                :aria-label="`${data.baslik || data.kaynakId} ${t('onaylar.reddet')}`"
                @click="retAc(data)"
              />
            </div>
          </template>
        </Column>
      </AppDataTable>

      <!-- Mobil kart görünümü -->
      <div class="onay-mobil">
        <SkeletonLoader
          v-if="listeIskelet"
          :count="4"
          height="96px"
        />
        <EmptyState
          v-else-if="!liste.length"
          :message="emptyMesaj"
          :sub-message="filtreVar ? null : t('onaylar.bekleyenYokIpucu')"
          icon="pi pi-check-circle"
        />
        <template v-else>
          <div
            v-for="kayit in liste"
            :key="kayit.anahtar"
            class="onay-kart"
            @click="detayAc(kayit)"
          >
            <div class="kart-ust">
              <button
                type="button"
                class="kart-sec"
                :aria-pressed="seciliMi(kayit)"
                :aria-label="seciliMi(kayit) ? t('onaylar.secimiKaldir') : t('onaylar.sec')"
                @click.stop="secimToggle(kayit)"
              >
                <i :class="seciliMi(kayit) ? 'pi pi-check-square' : 'pi pi-square'" />
              </button>
              <div class="kart-baslik-alan">
                <strong>{{ kayit.baslik || '-' }}</strong>
                <small v-if="kayit.altBaslik">{{ kayit.altBaslik }}</small>
              </div>
              <strong class="gizli-veri">{{ tutarMetni(kayit.tutar) }}</strong>
            </div>
            <div class="kart-rozetler">
              <Tag
                :value="turAdi(kayit.tur)"
                :severity="turSeverity(kayit.tur)"
              />
              <Tag
                :value="durumAdi(kayit.durum)"
                :severity="durumSeverity(kayit.durum)"
              />
              <span class="kart-tarih">{{ kayit.tarih ? formatDate(kayit.tarih) : '-' }}</span>
            </div>
            <div
              class="kart-aksiyonlar"
              @click.stop
            >
              <Button
                icon="pi pi-eye"
                severity="secondary"
                text
                rounded
                size="small"
                :aria-label="t('onaylar.detayiAc')"
                @click="detayAc(kayit)"
              />
              <Button
                :label="t('onaylar.onayla')"
                icon="pi pi-check"
                severity="success"
                size="small"
                class="flex-1"
                @click="tekOnayla(kayit)"
              />
              <Button
                :label="t('onaylar.reddet')"
                icon="pi pi-times"
                severity="danger"
                outlined
                size="small"
                class="flex-1"
                @click="retAc(kayit)"
              />
            </div>
          </div>
        </template>
      </div>
    </section>

    <!-- Karar geçmişi -->
    <section
      v-else
      id="onay-panel-GECMIS"
      role="tabpanel"
      aria-labelledby="onay-sekme-GECMIS"
      class="onay-panel"
    >
      <div class="onay-arac">
        <IconField class="filtre-arama">
          <InputIcon class="pi pi-search" />
          <InputText
            v-model="gecmisQ"
            :placeholder="t('onaylar.gecmisAra')"
            :aria-label="t('onaylar.gecmisAra')"
            @input="gecmisAramaDegisti"
          />
        </IconField>
        <Select
          v-model="gecmisTur"
          :options="gecmisTurSecenekleri"
          option-label="label"
          option-value="value"
          show-clear
          class="arac-secim"
          :placeholder="t('onaylar.gecmisTur')"
          :aria-label="t('onaylar.gecmisTur')"
          @change="gecmisFiltreDegisti"
        />
        <Select
          v-model="gecmisKarar"
          :options="gecmisKararSecenekleri"
          option-label="label"
          option-value="value"
          show-clear
          class="arac-secim"
          :placeholder="t('onaylar.gecmisKarar')"
          :aria-label="t('onaylar.gecmisKarar')"
          @change="gecmisFiltreDegisti"
        />
        <DatePicker
          v-model="gecmisAralik"
          selection-mode="range"
          date-format="dd.mm.yy"
          show-button-bar
          show-icon
          class="arac-tarih"
          :placeholder="t('onaylar.gecmisTarih')"
          :aria-label="t('onaylar.gecmisTarih')"
          @update:model-value="gecmisFiltreDegisti"
        />
        <span class="arac-bosluk" />
        <Button
          icon="pi pi-refresh"
          text
          size="small"
          :aria-label="t('onaylar.yenile')"
          :loading="gecmisYukleniyor"
          @click="gecmisYukle"
        />
      </div>

      <SkeletonLoader
        v-if="gecmisIskelet"
        class="tablo-iskelet"
        :count="5"
        height="46px"
      />

      <AppDataTable
        v-else
        :value="gecmisListe"
        lazy
        data-key="anahtar"
        :total-records="gecmisToplam"
        :rows="gecmisBoyut"
        :loading="gecmisYukleniyor"
        striped-rows
        class="onay-masa"
        gorunum-anahtari="onaylar-gecmis"
        :empty-message="t('onaylar.gecmisYok')"
        @page="gecmisSayfaDegisti"
      >
        <template #empty>
          <EmptyState
            v-if="!gecmisYukleniyor"
            :message="t('onaylar.gecmisYok')"
            icon="pi pi-history"
          />
        </template>
        <Column
          field="tur"
          :header="t('onaylar.tur')"
          style="width: 110px"
        >
          <template #body="{ data }">
            <Tag
              :value="turAdi(data.tur)"
              :severity="turSeverity(data.tur)"
            />
          </template>
        </Column>
        <Column
          :header="t('onaylar.kayit')"
          style="width: 110px"
        >
          <template #body="{ data }">
            #{{ data.kaynakId }}
          </template>
        </Column>
        <Column
          field="karar"
          :header="t('onaylar.gecmisKarar')"
          style="width: 110px"
        >
          <template #body="{ data }">
            <Tag
              :value="kararAdi(data.karar)"
              :severity="data.karar === 'ONAY' ? 'success' : 'danger'"
            />
          </template>
        </Column>
        <Column
          field="kararVerenAd"
          :header="t('onaylar.kararVeren')"
        >
          <template #body="{ data }">
            {{ data.kararVerenAd || (data.kararVerenId ? `#${data.kararVerenId}` : '-') }}
          </template>
        </Column>
        <Column
          field="olusturmaTarihi"
          :header="t('onaylar.tarih')"
          style="width: 170px"
        >
          <template #body="{ data }">
            {{ formatTarihSaat(data.olusturmaTarihi) }}
          </template>
        </Column>
        <Column
          field="gerekce"
          :header="t('onaylar.gerekce')"
        >
          <template #body="{ data }">
            {{ data.gerekce || '-' }}
          </template>
        </Column>
      </AppDataTable>

      <!-- Mobil geçmiş kartları -->
      <div class="onay-mobil">
        <SkeletonLoader
          v-if="gecmisIskelet"
          :count="4"
          height="96px"
        />
        <EmptyState
          v-else-if="!gecmisListe.length"
          :message="t('onaylar.gecmisYok')"
          icon="pi pi-history"
        />
        <template v-else>
          <div
            v-for="kayit in gecmisListe"
            :key="kayit.anahtar"
            class="onay-kart"
          >
            <div class="kart-ust">
              <div class="kart-baslik-alan">
                <strong>{{ turAdi(kayit.tur) }} #{{ kayit.kaynakId }}</strong>
                <small>{{ kayit.kararVerenAd || '-' }}</small>
              </div>
              <Tag
                :value="kararAdi(kayit.karar)"
                :severity="kayit.karar === 'ONAY' ? 'success' : 'danger'"
              />
            </div>
            <p
              v-if="kayit.gerekce"
              class="kart-gerekce"
            >
              {{ kayit.gerekce }}
            </p>
            <span class="kart-tarih">{{ formatTarihSaat(kayit.olusturmaTarihi) }}</span>
          </div>
        </template>
      </div>
    </section>

    <OnayDetayDrawer
      v-model:visible="detayAcik"
      :kayit="detayKayit"
      :yukleniyor="detayYukleniyor"
      @onayla="tekOnayla"
      @reddet="retAc"
    />

    <!-- Ret gerekçesi (tek / toplu) -->
    <Dialog
      v-model:visible="retDialog"
      :header="retBaslik"
      modal
      :style="{ width: '460px' }"
      :breakpoints="{ '575px': '92vw' }"
      aria-modal="true"
      @show="gerekceOdakla"
    >
      <div class="ret-form">
        <p class="modal-aciklama">
          {{ retAciklama }}
        </p>
        <div class="field">
          <label for="onay-ret-gerekce">{{ t('onaylar.retGerekceEtiket') }}</label>
          <Textarea
            id="onay-ret-gerekce"
            ref="gerekceRef"
            v-model="gerekce"
            rows="3"
            maxlength="500"
            class="w-full"
            :placeholder="t('onaylar.retGerekcePlaceholder')"
            :aria-invalid="!!retHata"
            aria-describedby="onay-ret-ipucu onay-ret-hata"
            @keydown="gerekceTus"
          />
          <small
            id="onay-ret-ipucu"
            class="ipucu"
          >{{ t('onaylar.retIpucu') }}</small>
          <small
            v-if="retHata"
            id="onay-ret-hata"
            class="hata-metin"
            role="alert"
          >{{ retHata }}</small>
        </div>
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          icon="pi pi-times"
          text
          @click="retDialog = false"
        />
        <Button
          :label="t('onaylar.retGonder')"
          icon="pi pi-times"
          severity="danger"
          :loading="islemde"
          :disabled="!retGecerli"
          @click="retGonder"
        />
      </template>
    </Dialog>

    <!-- Toplu işlem sonucu -->
    <Dialog
      v-model:visible="sonucDialog"
      :header="t('onaylar.sonucBaslik')"
      modal
      :style="{ width: '520px' }"
      :breakpoints="{ '575px': '92vw' }"
      aria-modal="true"
    >
      <div
        v-if="sonuc"
        class="sonuc-ozet"
      >
        <p class="sonuc-basari">
          <i class="pi pi-check-circle" />
          {{ t('onaylar.sonucBasarili', { n: sonuc.basarili }) }}
        </p>
        <div v-if="sonuc.atlanan.length">
          <h4>{{ t('onaylar.atlananlar') }}</h4>
          <ul class="atlanan-liste">
            <li
              v-for="atlanan in sonuc.atlanan"
              :key="atlanan.kaynakId"
            >
              <strong>{{ t('onaylar.atlananKayit', { id: atlanan.kaynakId }) }}</strong>
              <span>{{ atlanan.mesaj }}</span>
            </li>
          </ul>
        </div>
      </div>
      <template #footer>
        <Button
          :label="t('onaylar.kapat')"
          icon="pi pi-check"
          @click="sonucDialog = false"
        />
      </template>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { onaylarAPI, excelAPI } from '../api/index.js'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import OnayDetayDrawer from '../components/OnayDetayDrawer.vue'
import {
  formatCurrency,
  formatDate,
  formatTarihSaat,
  getLocalDateString
} from '../utils/format.js'

const { t } = useI18n()
const router = useRouter()
const toastBildirim = useToastBildirim()

// ---- Hafıza ----
const TAB_ANAHTAR = 'raspel_onaylar_tab'
const SEKMELER = [
  { key: 'TUMU', i18n: 'tabTumu', ikon: 'pi pi-list', tur: null },
  { key: 'IZIN', i18n: 'tabIzin', ikon: 'pi pi-calendar', tur: 'IZIN' },
  { key: 'MASRAF', i18n: 'tabMasraf', ikon: 'pi pi-wallet', tur: 'MASRAF' },
  { key: 'SATINALMA', i18n: 'tabSatinalma', ikon: 'pi pi-shopping-bag', tur: 'SATINALMA' },
  { key: 'SIPARIS', i18n: 'tabSiparis', ikon: 'pi pi-receipt', tur: 'SIPARIS' },
  { key: 'GECMIS', i18n: 'tabGecmis', ikon: 'pi pi-history', tur: null }
]

const sekmeOku = () => {
  const sayi = Number(localStorage.getItem(TAB_ANAHTAR))
  return Number.isInteger(sayi) && sayi >= 0 && sayi < SEKMELER.length ? sayi : 0
}

const aktifSekme = ref(sekmeOku())
const sekmeler = SEKMELER
const aktifSekmeKey = computed(() => SEKMELER[aktifSekme.value]?.key || 'TUMU')
const gecmisMi = computed(() => aktifSekme.value === 5)
const turParam = computed(() => SEKMELER[aktifSekme.value]?.tur || null)
const turSecili = computed(() => !!turParam.value)

// ---- Özet + liste ----
const ozet = ref({ toplam: 0, izin: 0, masraf: 0, satinalma: 0, siparis: 0 })
const ozetYuklendi = ref(false)
const liste = ref([])
const toplam = ref(0)
const yukleniyor = ref(false)
const excelYukleniyor = ref(false)
const islemde = ref(false)

const q = ref('')
const sayfa = ref(0)
const boyut = ref(25)
const siralama = ref(null)
let aramaZaman = null

const ozetIskelet = computed(() => !ozetYuklendi.value)
const listeIskelet = computed(() => yukleniyor.value && !liste.value.length)
const filtreVar = computed(() => !!q.value.trim())
const emptyMesaj = computed(() =>
  filtreVar.value ? t('onaylar.filtreYok') : t('onaylar.bekleyenYok')
)

const kpiKartlari = computed(() => [
  {
    key: 'TOPLAM',
    index: 0,
    baslik: t('onaylar.kpiToplam'),
    deger: ozet.value?.toplam || 0,
    ikon: 'pi pi-inbox',
    renk: '#3b82f6',
    alt: t('onaylar.kpiToplamAlt')
  },
  {
    key: 'IZIN',
    index: 1,
    baslik: t('onaylar.kpiIzin'),
    deger: ozet.value?.izin || 0,
    ikon: 'pi pi-calendar',
    renk: '#8b5cf6',
    alt: t('onaylar.kpiIzinAlt')
  },
  {
    key: 'MASRAF',
    index: 2,
    baslik: t('onaylar.kpiMasraf'),
    deger: ozet.value?.masraf || 0,
    ikon: 'pi pi-wallet',
    renk: '#10b981',
    alt: t('onaylar.kpiMasrafAlt')
  },
  {
    key: 'SATINALMA',
    index: 3,
    baslik: t('onaylar.kpiSatinalma'),
    deger: ozet.value?.satinalma || 0,
    ikon: 'pi pi-shopping-bag',
    renk: '#f59e0b',
    alt: t('onaylar.kpiSatinalmaAlt')
  },
  {
    key: 'SIPARIS',
    index: 4,
    baslik: t('onaylar.kpiSiparis'),
    deger: ozet.value?.siparis || 0,
    ikon: 'pi pi-receipt',
    renk: '#14b8a6',
    alt: t('onaylar.kpiSiparisAlt')
  }
])

const kpiAktif = (kpi) => aktifSekme.value === kpi.index
const kpiTikla = (kpi) => sekmeSec(kpi.index)

const sekmeSayisi = (sekme) => {
  if (sekme.key === 'GECMIS') return 0
  if (sekme.key === 'TUMU') return Number(ozet.value?.toplam) || 0
  const anahtar = sekme.key.toLowerCase()
  return Number(ozet.value?.[anahtar]) || 0
}

const canliDuyuru = computed(() => {
  if (yukleniyor.value || gecmisYukleniyor.value) return t('onaylar.yukleniyor')
  if (gecmisMi.value) return t('onaylar.gecmisDuyuru', { n: gecmisToplam.value })
  if (secim.value.length) return t('onaylar.secilenDuyuru', { n: secim.value.length })
  return t('onaylar.kayitDuyuru', { gorunen: liste.value.length, toplam: toplam.value })
})

// ---- Etiketler ----
const TUR_BILGI = {
  IZIN: { i18n: 'turIzin', severity: 'info' },
  MASRAF: { i18n: 'turMasraf', severity: 'success' },
  SATINALMA: { i18n: 'turSatinalma', severity: 'warn' },
  SIPARIS: { i18n: 'turSiparis', severity: 'secondary' }
}

const DURUM_ANAHTAR = {
  BEKLEMEDE: 'durumBeklemede',
  BEKLIYOR: 'durumBekliyor',
  TEKLIF: 'durumTeklif',
  TASLAK: 'durumTaslak',
  ONAYLANDI: 'durumOnaylandi',
  REDDEDILDI: 'durumReddedildi',
  IPTAL: 'durumIptal',
  HAZIRLANIYOR: 'durumHazirlaniyor',
  TAMAMLANDI: 'durumTamamlandi'
}

const DURUM_SEVERITY = {
  BEKLEMEDE: 'warn',
  BEKLIYOR: 'warn',
  TEKLIF: 'warn',
  TASLAK: 'warn',
  ONAYLANDI: 'success',
  REDDEDILDI: 'danger',
  IPTAL: 'danger',
  HAZIRLANIYOR: 'info',
  TAMAMLANDI: 'success'
}

const turAdi = (tur) =>
  TUR_BILGI[tur] ? t(`onaylar.${TUR_BILGI[tur].i18n}`) : (tur || '-')
const turSeverity = (tur) => TUR_BILGI[tur]?.severity || 'secondary'
const durumAdi = (durum) =>
  DURUM_ANAHTAR[durum] ? t(`onaylar.${DURUM_ANAHTAR[durum]}`) : (durum || '-')
const durumSeverity = (durum) => DURUM_SEVERITY[durum] || 'secondary'
const kararAdi = (karar) =>
  karar === 'ONAY' ? t('onaylar.kararOnay') : karar === 'RED' ? t('onaylar.kararRed') : (karar || '-')
const tutarMetni = (tutar) => (tutar === null || tutar === undefined ? '-' : formatCurrency(tutar))

// ---- Veri yükleme ----
const satirHazirla = (kayit) => ({
  ...kayit,
  anahtar: `${kayit.tur}:${kayit.kaynakId}`
})

const listeParams = () => {
  const params = {}
  if (turSecili.value) {
    params.tur = turParam.value
    params.page = sayfa.value
    params.size = boyut.value
    if (siralama.value) params.sort = siralama.value
  }
  if (q.value.trim()) params.q = q.value.trim()
  return params
}

const listeYukle = async () => {
  yukleniyor.value = true
  try {
    const r = await onaylarAPI.bekleyen(listeParams())
    const veri = r.data || {}
    const icerik = Array.isArray(veri) ? veri : veri.content || []
    liste.value = icerik.map(satirHazirla)
    toplam.value = Array.isArray(veri) ? liste.value.length : (veri.totalElements ?? liste.value.length)
    // Sayfa/filtre değişiminde görünmeyen seçimler tutulmaz.
    secim.value = secim.value.filter((secili) =>
      liste.value.some((kayit) => kayit.anahtar === secili.anahtar)
    )
  } catch (err) {
    liste.value = []
    toplam.value = 0
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('onaylar.yuklemeHatasi'))
  } finally {
    yukleniyor.value = false
  }
}

const ozetYukle = async () => {
  try {
    const r = await onaylarAPI.ozet()
    ozet.value = { ...ozet.value, ...(r.data || {}) }
  } catch {
    // Özet yüklenemezse KPI'lar sıfır gösterilir; liste yine çalışır.
  } finally {
    ozetYuklendi.value = true
  }
}

const aktifListeyiYukle = () => (gecmisMi.value ? gecmisYukle() : listeYukle())
const yenile = () => {
  ozetYukle()
  aktifListeyiYukle()
}
const yenileListeleri = async () => {
  await Promise.all([listeYukle(), ozetYukle()])
  if (gecmisYuklendi.value) await gecmisYukle()
}

// ---- Sekme / filtre ----
const sekmeSec = (index) => {
  if (index === aktifSekme.value || index < 0 || index >= SEKMELER.length) return
  aktifSekme.value = index
  localStorage.setItem(TAB_ANAHTAR, String(index))
  sayfa.value = 0
  gecmisSayfa.value = 0
  secim.value = []
  siralama.value = null
  if (index === 5) gecmisYukle()
  else listeYukle()
}

const sekmeTus = (olay, index) => {
  const tuslar = { ArrowRight: 1, ArrowLeft: -1 }
  let hedef = index
  if (olay.key === 'Home') hedef = 0
  else if (olay.key === 'End') hedef = SEKMELER.length - 1
  else if (tuslar[olay.key]) {
    hedef = (index + tuslar[olay.key] + SEKMELER.length) % SEKMELER.length
  } else return
  olay.preventDefault()
  sekmeSec(hedef)
  nextTick(() => document.getElementById(`onay-sekme-${SEKMELER[hedef].key}`)?.focus())
}

const aramaDegisti = () => {
  if (aramaZaman) clearTimeout(aramaZaman)
  aramaZaman = setTimeout(() => {
    sayfa.value = 0
    listeYukle()
  }, 300)
}

const filtreTemizle = () => {
  q.value = ''
  sayfa.value = 0
  listeYukle()
}

const sayfaDegisti = (olay) => {
  if (!turSecili.value) return
  sayfa.value = olay.page
  boyut.value = olay.rows
  listeYukle()
}

const siralaDegisti = (olay) => {
  if (!turSecili.value) return
  siralama.value = olay.sortField
    ? `${olay.sortField},${olay.sortOrder === -1 ? 'desc' : 'asc'}`
    : null
  sayfa.value = 0
  listeYukle()
}

const ayarlaraGit = () => router.push('/onay-ayarlari')

// ---- Seçim + toplu işlem ----
const secim = ref([])
const secimTuru = computed(() => secim.value[0]?.tur || null)
const seciliMi = (kayit) => secim.value.some((secili) => secili.anahtar === kayit.anahtar)
const satirSecilebilir = (kayit) => !secimTuru.value || kayit.tur === secimTuru.value

const secimDegisti = (yeni) => {
  const secili = Array.isArray(yeni) ? yeni.filter(Boolean) : []
  if (!secili.length) {
    secim.value = []
    return
  }
  const turler = new Set(secili.map((kayit) => kayit.tur))
  if (turler.size > 1) {
    // Farklı tür karışımı kabul edilmez; önceki tutarlı seçim korunur.
    // Yeni dizi referansı, kontrollü seçimin tabloda geri çizilmesini garanti eder.
    secim.value = [...secim.value]
    toastBildirim.uyari(t('onaylar.farkliTurSecimUyari'))
    return
  }
  secim.value = secili
}

const secimToggle = (kayit) => {
  if (seciliMi(kayit)) {
    secim.value = secim.value.filter((secili) => secili.anahtar !== kayit.anahtar)
    return
  }
  if (!satirSecilebilir(kayit)) {
    toastBildirim.uyari(t('onaylar.farkliTurSecimUyari'))
    return
  }
  secim.value = [...secim.value, kayit]
}

// ---- Karar ----
const tekKarar = async (kayit, karar, gerekce = '') => {
  if (!kayit?.tur || kayit.kaynakId == null) return
  islemde.value = true
  try {
    await onaylarAPI.karar({
      tur: kayit.tur,
      kaynakId: kayit.kaynakId,
      karar,
      gerekce
    })
    toastBildirim.basarili(
      karar === 'ONAY' ? t('onaylar.tekOnayBasarili') : t('onaylar.tekRedBasarili')
    )
    detayAcik.value = false
    retDialog.value = false
    secim.value = []
    await yenileListeleri()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('onaylar.islemBasarisiz'))
  } finally {
    islemde.value = false
  }
}

const tekOnayla = (kayit) => tekKarar(kayit, 'ONAY', '')

const topluOnayla = () => topluKarar('ONAY', '', secim.value)

const topluKarar = async (karar, gerekce, secili) => {
  if (!secili?.length) return
  const tur = secili[0].tur
  islemde.value = true
  try {
    const r = await onaylarAPI.toplu({
      tur,
      kaynakIds: secili.map((kayit) => kayit.kaynakId),
      karar,
      gerekce
    })
    retDialog.value = false
    secim.value = []
    sonucuIsle(r.data || {}, karar)
    await yenileListeleri()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('onaylar.islemBasarisiz'))
  } finally {
    islemde.value = false
  }
}

const sonuc = ref(null)
const sonucDialog = ref(false)

const sonucuIsle = (veri, karar) => {
  const basarili = Number(veri?.basarili) || 0
  const atlanan = Array.isArray(veri?.atlanan) ? veri.atlanan : []
  sonuc.value = { karar, basarili, atlanan }
  if (atlanan.length) {
    sonucDialog.value = true
    toastBildirim.uyari(
      t(karar === 'ONAY' ? 'onaylar.topluOnayKismi' : 'onaylar.topluRedKismi', {
        basarili,
        atlanan: atlanan.length
      })
    )
  } else {
    toastBildirim.basarili(
      t(karar === 'ONAY' ? 'onaylar.topluOnayBasarili' : 'onaylar.topluRedBasarili', {
        n: basarili
      })
    )
  }
}

// ---- Ret gerekçesi ----
const retDialog = ref(false)
const retMod = ref('tek')
const retHedef = ref(null)
const retSecim = ref([])
const gerekce = ref('')
const retHata = ref('')
const gerekceRef = ref(null)

const retGecerli = computed(() => gerekce.value.trim().length >= 3)
const retBaslik = computed(() =>
  retMod.value === 'toplu' ? t('onaylar.retTopluBaslik') : t('onaylar.retBaslik')
)
const retAciklama = computed(() =>
  retMod.value === 'toplu'
    ? t('onaylar.retTopluAciklama', { n: retSecim.value.length })
    : t('onaylar.retAciklama')
)

const modaliAc = (mod, hedef = null, secili = []) => {
  retMod.value = mod
  retHedef.value = hedef
  retSecim.value = [...secili]
  gerekce.value = ''
  retHata.value = ''
  retDialog.value = true
}

const retAc = (kayit) => modaliAc('tek', kayit)

const topluRetAc = () => {
  if (!secim.value.length) return
  modaliAc('toplu', null, secim.value)
}

const gerekceOdakla = () => {
  nextTick(() => {
    const alan = gerekceRef.value?.$el || gerekceRef.value
    alan?.focus?.()
  })
}

const gerekceTus = (olay) => {
  if (olay.key !== 'Enter') return
  // Shift+Enter yeni satır; Enter ve Ctrl/Cmd+Enter gönderir.
  if (olay.shiftKey && !olay.ctrlKey && !olay.metaKey) return
  olay.preventDefault()
  retGonder()
}

const retGonder = async () => {
  if (!retGecerli.value) {
    retHata.value = t('onaylar.retMinHata')
    return
  }
  retHata.value = ''
  if (retMod.value === 'toplu') {
    await topluKarar('RED', gerekce.value.trim(), retSecim.value)
  } else {
    const hedef = retHedef.value
    retDialog.value = false
    await tekKarar(hedef, 'RED', gerekce.value.trim())
  }
}

// ---- Detay drawer ----
const detayAcik = ref(false)
const detayKayit = ref(null)
const detayYukleniyor = ref(false)

const detayAc = (kayit) => {
  if (!kayit) return
  detayKayit.value = kayit
  detayYukleniyor.value = false
  detayAcik.value = true
}

const satirTiklandi = (olay) => {
  if (olay?.data) detayAc(olay.data)
}

// ---- Excel ----
const indir = (veri, dosyaAdi) => {
  const blob = new Blob([veri])
  const url = URL.createObjectURL(blob)
  const baglanti = document.createElement('a')
  baglanti.href = url
  baglanti.download = dosyaAdi
  document.body.appendChild(baglanti)
  baglanti.click()
  baglanti.remove()
  setTimeout(() => URL.revokeObjectURL(url), 60000)
}

const excelIndir = async () => {
  excelYukleniyor.value = true
  try {
    const params = {}
    if (turParam.value) params.tur = turParam.value
    if (q.value.trim()) params.q = q.value.trim()
    const r = await excelAPI.onaylar(params)
    indir(r.data, 'onaylar.xlsx')
    toastBildirim.basarili(t('onaylar.excelIndirildi'))
  } catch {
    toastBildirim.hata(t('onaylar.excelHata'))
  } finally {
    excelYukleniyor.value = false
  }
}

// ---- Geçmiş ----
const gecmisListe = ref([])
const gecmisToplam = ref(0)
const gecmisYukleniyor = ref(false)
const gecmisYuklendi = ref(false)
const gecmisSayfa = ref(0)
const gecmisBoyut = ref(25)
const gecmisTur = ref(null)
const gecmisKarar = ref(null)
const gecmisQ = ref('')
const gecmisAralik = ref(null)
let gecmisAramaZaman = null

const gecmisIskelet = computed(() => gecmisYukleniyor.value && !gecmisListe.value.length)
const gecmisTurSecenekleri = computed(() =>
  SEKMELER.filter((sekme) => sekme.tur).map((sekme) => ({
    value: sekme.tur,
    label: t(`onaylar.${sekme.i18n}`)
  }))
)
const gecmisKararSecenekleri = computed(() => [
  { value: 'ONAY', label: t('onaylar.kararOnay') },
  { value: 'RED', label: t('onaylar.kararRed') }
])

const gecmisParams = () => {
  const params = { page: gecmisSayfa.value, size: gecmisBoyut.value }
  if (gecmisTur.value) params.tur = gecmisTur.value
  if (gecmisKarar.value) params.karar = gecmisKarar.value
  if (gecmisQ.value.trim()) params.q = gecmisQ.value.trim()
  const bas = gecmisAralik.value?.[0]
  const bit = gecmisAralik.value?.[1]
  if (bas) params.baslangic = getLocalDateString(bas)
  if (bit) params.bitis = getLocalDateString(bit)
  return params
}

const gecmisYukle = async () => {
  gecmisYukleniyor.value = true
  try {
    const r = await onaylarAPI.gecmis(gecmisParams())
    const veri = r.data || {}
    const icerik = Array.isArray(veri) ? veri : veri.content || []
    gecmisListe.value = icerik.map((kayit) => ({
      ...kayit,
      anahtar: kayit.id ?? `${kayit.tur}:${kayit.kaynakId}:${kayit.olusturmaTarihi}`
    }))
    gecmisToplam.value = Array.isArray(veri)
      ? gecmisListe.value.length
      : (veri.totalElements ?? gecmisListe.value.length)
    gecmisYuklendi.value = true
  } catch (err) {
    gecmisListe.value = []
    gecmisToplam.value = 0
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('onaylar.gecmisYuklemeHatasi'))
  } finally {
    gecmisYukleniyor.value = false
  }
}

const gecmisFiltreDegisti = () => {
  gecmisSayfa.value = 0
  gecmisYukle()
}

const gecmisAramaDegisti = () => {
  if (gecmisAramaZaman) clearTimeout(gecmisAramaZaman)
  gecmisAramaZaman = setTimeout(() => {
    gecmisSayfa.value = 0
    gecmisYukle()
  }, 300)
}

const gecmisSayfaDegisti = (olay) => {
  gecmisSayfa.value = olay.page
  gecmisBoyut.value = olay.rows
  gecmisYukle()
}

onMounted(() => {
  ozetYukle()
  if (gecmisMi.value) gecmisYukle()
  else listeYukle()
})

onBeforeUnmount(() => {
  if (aramaZaman) clearTimeout(aramaZaman)
  if (gecmisAramaZaman) clearTimeout(gecmisAramaZaman)
})
</script>

<style scoped>
.onaylar-sayfasi {
  padding: 0;
  max-width: 100%;
}

/* KPI */
.kpi-satiri {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 18px;
}
.kpi-iskelet {
  min-width: 0;
}
.kpi-tikla {
  display: block;
  width: 100%;
  padding: 0;
  border: 0;
  background: transparent;
  text-align: left;
  cursor: pointer;
  border-radius: var(--radius-lg, 14px);
  min-width: 0;
}
.kpi-tikla:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}

/* Sekmeler */
.onay-sekmeleri {
  display: flex;
  align-items: stretch;
  gap: 2px;
  overflow-x: auto;
  border-bottom: 1px solid var(--border);
  margin-bottom: 14px;
  scrollbar-width: thin;
}
.onay-sekme {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 44px;
  padding: 8px 12px;
  border: 0;
  border-bottom: 2px solid transparent;
  background: transparent;
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 500;
  white-space: nowrap;
  cursor: pointer;
  transition: color var(--dur-base, 0.2s) var(--ease-standard, ease),
    border-color var(--dur-base, 0.2s) var(--ease-standard, ease);
}
.onay-sekme:hover {
  color: var(--text-primary);
}
.onay-sekme.aktif {
  color: var(--accent);
  border-bottom-color: var(--accent);
  font-weight: 700;
}
.onay-sekme:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: -2px;
}
.onay-sekme i {
  font-size: 13px;
}
.sekme-rozet {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 999px;
  background: var(--accent);
  color: #042f2e;
  font-size: 10px;
  font-weight: 800;
  line-height: 1;
}

/* Araç çubuğu */
.onay-arac {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}
.filtre-arama {
  min-width: 220px;
  max-width: 340px;
  flex: 1 1 240px;
}
.arac-secim {
  min-width: 140px;
}
.arac-tarih {
  min-width: 200px;
}
.arac-bosluk {
  flex: 1;
}
.muted {
  color: var(--text-muted);
  font-size: 12px;
}

/* Toplu çubuk (mobil) */
.toplu-cubuk {
  display: none;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
  padding: 10px 12px;
  margin-bottom: 12px;
  border: 1px solid var(--accent-border, rgba(20, 184, 166, 0.35));
  border-radius: var(--radius-md, 10px);
  background: var(--accent-soft, rgba(20, 184, 166, 0.12));
}
.toplu-bilgi {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
}
.toplu-bilgi i {
  color: var(--accent);
}
.toplu-butonlar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
}

/* Tablo */
.kayit-baslik {
  font-weight: 600;
  color: var(--text-primary);
}
.kayit-alt {
  display: block;
  margin-top: 2px;
  color: var(--text-muted);
  font-size: 12px;
}
.satir-aksiyonlar {
  display: inline-flex;
  align-items: center;
  gap: 2px;
}

/* Mobil kartlar */
.onay-mobil {
  display: none;
  flex-direction: column;
  gap: 10px;
}
.onay-kart {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 12px;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: var(--bg-card);
  cursor: pointer;
  transition: border-color var(--dur-base, 0.2s) var(--ease-standard, ease);
}
.onay-kart:focus-within,
.onay-kart:hover {
  border-color: var(--accent-border, rgba(20, 184, 166, 0.35));
}
.kart-ust {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}
.kart-baslik-alan {
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex: 1;
  min-width: 0;
}
.kart-baslik-alan strong {
  color: var(--text-primary);
  overflow-wrap: anywhere;
}
.kart-baslik-alan small {
  color: var(--text-muted);
  font-size: 12px;
  overflow-wrap: anywhere;
}
.kart-sec {
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--bg-card);
  color: var(--text-secondary);
  width: 40px;
  height: 40px;
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}
.kart-sec[aria-pressed='true'] {
  color: var(--accent);
  border-color: var(--accent);
}
.kart-rozetler {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
}
.kart-tarih {
  color: var(--text-muted);
  font-size: 12px;
}
.kart-gerekce {
  margin: 0;
  font-size: 12.5px;
  color: var(--text-secondary);
  overflow-wrap: anywhere;
}
.kart-aksiyonlar {
  display: flex;
  gap: 8px;
  padding-top: 8px;
  border-top: 1px solid var(--border);
}
.kart-aksiyonlar :deep(.p-button) {
  min-height: 42px;
}

/* Ret + sonuç */
.ret-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.modal-aciklama {
  margin: 0;
  font-size: 13px;
  color: var(--text-secondary);
}
.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.field label {
  font-size: 12.5px;
  color: var(--text-secondary);
}
.ipucu {
  color: var(--text-muted);
  font-size: 11.5px;
}
.hata-metin {
  color: var(--danger, #ef4444);
  font-size: 12px;
}
.sonuc-ozet {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.sonuc-basari {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  font-weight: 600;
  color: var(--success, #10b981);
}
.sonuc-ozet h4 {
  margin: 0 0 8px;
  font-size: 13px;
  color: var(--text-primary);
}
.atlanan-liste {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 240px;
  overflow-y: auto;
}
.atlanan-liste li {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 8px 10px;
  border: 1px solid var(--border);
  border-radius: 8px;
  font-size: 12.5px;
}
.atlanan-liste span {
  color: var(--text-secondary);
}

@media (max-width: 1100px) {
  .kpi-satiri {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
@media (max-width: 768px) {
  .kpi-satiri {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .onay-masa {
    display: none;
  }
  .onay-mobil {
    display: flex;
  }
  .mobil-toplu {
    display: flex;
  }
  .filtre-arama {
    max-width: none;
  }
  .arac-secim,
  .arac-tarih {
    flex: 1 1 140px;
    min-width: 0;
  }
  .arac-bosluk {
    display: none;
  }
}
@media (max-width: 420px) {
  .kpi-satiri {
    grid-template-columns: 1fr;
  }
}
@media (prefers-reduced-motion: reduce) {
  .onay-sekme,
  .onay-kart {
    transition: none;
  }
}
</style>
