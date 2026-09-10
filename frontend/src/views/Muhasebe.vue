<template>
  <div class="muhasebe-container">
    <div class="sayfa-baslik">
      <h1 class="page-title">
        {{ t('muhasebe.title') }}
      </h1>
      <Button
        v-if="aktifSekme === 0"
        :label="t('muhasebe.yeniHesap')"
        icon="pi pi-plus"
        @click="hesapDialogAc()"
      />
      <Button
        v-else-if="aktifSekme === 1"
        :label="t('muhasebe.yeniYevmiyeFisi')"
        icon="pi pi-plus"
        @click="fisDialogAc()"
      />
    </div>

    <IlkZiyaretIpuclari
      anahtar="muhasebe"
      :baslik="t('muhasebe.title')"
      :metin="t('muhasebe.ipucuMetin')"
    />

    <TabView
      v-model:active-index="aktifSekme"
      class="muhasebe-tabs"
    >
      <!-- HESAP PLANI -->
      <TabPanel :header="t('muhasebe.hesapPlani')">
        <AppDataTable
          :value="hesaplar"
          :loading="yukleniyor"
          arama-aktif
          :arama-placeholder="t('muhasebe.aramaPlaceholder')"
          gorunum-anahtari="muhasebe_hesap_plani"
        >
          <Column
            field="kod"
            :header="t('muhasebe.kod')"
            sortable
            style="width: 110px"
          />
          <Column
            field="ad"
            :header="t('muhasebe.hesapAdi')"
            sortable
          />
          <Column
            field="tip"
            :header="t('muhasebe.tip')"
          >
            <template #body="{ data }">
              <Tag
                :value="tipEtiketi(data.tip)"
                :severity="tipSeverity(data.tip)"
              />
            </template>
          </Column>
          <Column
            field="grup"
            :header="t('muhasebe.grup')"
          />
          <Column
            :header="t('muhasebe.islem')"
            style="width: 60px"
          >
            <template #body="{ data }">
              <SatirEylemleri
                :gorunur="{ duzenle: true, cogalt: true, sil: true }"
                @duzenle="hesapDialogAc(data)"
                @cogalt="hesapCogalt(data)"
                @sil="hesapSil(data)"
              />
            </template>
          </Column>
        </AppDataTable>
      </TabPanel>

      <!-- YEVMIYE FİŞLERİ -->
      <TabPanel :header="t('muhasebe.yevmiyeFisleri')">
        <div class="filtre-bar">
          <DatePicker
            v-model="filtreBaslangic"
            date-format="dd/mm/yy"
            :placeholder="t('muhasebe.baslangic')"
          />
          <DatePicker
            v-model="filtreBitis"
            date-format="dd/mm/yy"
            :placeholder="t('muhasebe.bitis')"
          />
          <Button
            icon="pi pi-refresh"
            :label="t('muhasebe.yenile')"
            class="p-button-sm p-button-text"
            @click="fisleriYukle"
          />
        </div>
        <DataTable
          :value="fisler"
          striped-rows
          :loading="fisYukleniyor"
        >
          <Column
            field="fisNo"
            :header="t('muhasebe.fisNo')"
            sortable
            style="width: 150px"
          />
          <Column
            field="tarih"
            :header="t('common.date')"
            sortable
          >
            <template #body="{ data }">
              {{ formatDate(data.tarih) }}
            </template>
          </Column>
          <Column
            field="aciklama"
            :header="t('common.description')"
          />
          <Column
            field="toplamBorc"
            :header="t('muhasebe.borc')"
          >
            <template #body="{ data }">
              {{ formatCurrency(data.toplamBorc) }}
            </template>
          </Column>
          <Column
            field="toplamAlacak"
            :header="t('muhasebe.alacak')"
          >
            <template #body="{ data }">
              {{ formatCurrency(data.toplamAlacak) }}
            </template>
          </Column>
          <Column
            field="durum"
            :header="t('common.status')"
          >
            <template #body="{ data }">
              <Tag
                :value="data.durum"
                :severity="data.durum === 'ONAYLANDI' ? 'success' : data.durum === 'IPTAL' ? 'danger' : 'info'"
              />
            </template>
          </Column>
          <Column
            :header="t('muhasebe.islem')"
            style="width: 140px"
          >
            <template #body="{ data }">
              <Button
                icon="pi pi-eye"
                class="p-button-rounded p-button-text"
                :title="t('muhasebe.goruntule')"
                @click="fisDetayAc(data)"
              />
              <Button
                v-if="data.durum !== 'IPTAL'"
                icon="pi pi-ban"
                class="p-button-rounded p-button-text p-button-danger"
                :title="t('muhasebe.iptalEt')"
                @click="fisIptal(data)"
              />
            </template>
          </Column>
        </DataTable>
      </TabPanel>

      <!-- MİZAN -->
      <TabPanel :header="t('muhasebe.mizan')">
        <div class="filtre-bar">
          <DatePicker
            v-model="mizanBaslangic"
            date-format="dd/mm/yy"
            :placeholder="t('muhasebe.baslangic')"
          />
          <DatePicker
            v-model="mizanBitis"
            date-format="dd/mm/yy"
            :placeholder="t('muhasebe.bitis')"
          />
          <Button
            icon="pi pi-refresh"
            :label="t('muhasebe.hesapla')"
            class="p-button-sm"
            @click="mizanYukle"
          />
        </div>
        <DataTable
          :value="mizan"
          striped-rows
          :loading="mizanYukleniyor"
        >
          <Column
            field="hesapKodu"
            :header="t('muhasebe.hesapKodu')"
            sortable
            style="width: 110px"
          />
          <Column
            field="hesapAdi"
            :header="t('muhasebe.hesapAdi')"
          />
          <Column
            field="borc"
            :header="t('muhasebe.borc')"
          >
            <template #body="{ data }">
              {{ formatCurrency(data.borc) }}
            </template>
          </Column>
          <Column
            field="alacak"
            :header="t('muhasebe.alacak')"
          >
            <template #body="{ data }">
              {{ formatCurrency(data.alacak) }}
            </template>
          </Column>
          <Column
            field="borcBakiye"
            :header="t('muhasebe.borcBakiye')"
          >
            <template #body="{ data }">
              <span class="pozitif">{{ formatCurrency(data.borcBakiye) }}</span>
            </template>
          </Column>
          <Column
            field="alacakBakiye"
            :header="t('muhasebe.alacakBakiye')"
          >
            <template #body="{ data }">
              <span class="negatif">{{ formatCurrency(data.alacakBakiye) }}</span>
            </template>
          </Column>
        </DataTable>
      </TabPanel>

      <!-- DEFTER-İ KEBİR -->
      <TabPanel :header="t('muhasebe.defteriKebir')">
        <div class="filtre-bar">
          <Select
            v-model="kebirHesap"
            :options="hesapSecenekleri"
            option-label="ad"
            option-value="kod"
            :placeholder="t('muhasebe.hesapSecin')"
            class="kebir-select"
            show-clear
          />
          <DatePicker
            v-model="kebirBaslangic"
            date-format="dd/mm/yy"
            :placeholder="t('muhasebe.baslangic')"
          />
          <DatePicker
            v-model="kebirBitis"
            date-format="dd/mm/yy"
            :placeholder="t('muhasebe.bitis')"
          />
          <Button
            icon="pi pi-refresh"
            :label="t('muhasebe.listele')"
            class="p-button-sm"
            @click="kebirYukle"
          />
        </div>
        <DataTable
          :value="kebir"
          striped-rows
          :loading="kebirYukleniyor"
        >
          <Column
            field="tarih"
            :header="t('common.date')"
          >
            <template #body="{ data }">
              {{ formatDate(data.tarih) }}
            </template>
          </Column>
          <Column
            field="fisNo"
            :header="t('muhasebe.fisNo')"
          />
          <Column
            field="aciklama"
            :header="t('common.description')"
          />
          <Column
            field="borc"
            :header="t('muhasebe.borc')"
          >
            <template #body="{ data }">
              {{ formatCurrency(data.borc) }}
            </template>
          </Column>
          <Column
            field="alacak"
            :header="t('muhasebe.alacak')"
          >
            <template #body="{ data }">
              {{ formatCurrency(data.alacak) }}
            </template>
          </Column>
          <Column
            field="bakiye"
            :header="t('muhasebe.bakiye')"
          >
            <template #body="{ data }">
              {{ formatCurrency(data.bakiye) }}
            </template>
          </Column>
        </DataTable>
      </TabPanel>

      <!-- BİLANÇO -->
      <TabPanel :header="t('muhasebe.bilanco')">
        <div class="filtre-bar">
          <Button
            icon="pi pi-refresh"
            :label="t('muhasebe.yenile')"
            class="p-button-sm"
            @click="bilancoYukle"
          />
          <Button
            icon="pi pi-file-excel"
            :label="t('muhasebe.excel')"
            class="p-button-sm p-button-outlined"
            @click="bilancoExcelIndir"
          />
        </div>
        <div class="finansal-grid">
          <div class="finansal-kolon">
            <h3 class="finansal-baslik">
              {{ t('muhasebe.aktifler') }}
            </h3>
            <DataTable
              :value="bilanco.aktifler || []"
              striped-rows
              size="small"
              :loading="bilancoYukleniyor"
            >
              <Column
                field="kod"
                :header="t('muhasebe.kod')"
              />
              <Column
                field="ad"
                :header="t('muhasebe.hesap')"
              />
              <Column :header="t('common.amount')">
                <template #body="{ data }">
                  {{ formatCurrency(data.tutar) }}
                </template>
              </Column>
            </DataTable>
            <div class="finansal-toplam">
              {{ t('muhasebe.toplamAktif') }} <strong>{{ formatCurrency(bilanco.aktifToplam) }}</strong>
            </div>
          </div>
          <div class="finansal-kolon">
            <h3 class="finansal-baslik">
              {{ t('muhasebe.pasifler') }}
            </h3>
            <DataTable
              :value="bilanco.pasifler || []"
              striped-rows
              size="small"
              :loading="bilancoYukleniyor"
            >
              <Column
                field="kod"
                :header="t('muhasebe.kod')"
              />
              <Column
                field="ad"
                :header="t('muhasebe.hesap')"
              />
              <Column :header="t('common.amount')">
                <template #body="{ data }">
                  {{ formatCurrency(data.tutar) }}
                </template>
              </Column>
            </DataTable>
            <div class="finansal-toplam">
              {{ t('muhasebe.toplamPasif') }} <strong>{{ formatCurrency(bilanco.pasifToplam) }}</strong>
            </div>
          </div>
        </div>
        <div class="grafik-kutu">
          <Doughnut
            :data="bilancoChartData"
            :options="{ responsive: true, maintainAspectRatio: false }"
            style="height: 220px"
          />
        </div>
      </TabPanel>

      <!-- KÂR / ZARAR -->
      <TabPanel :header="t('muhasebe.karZarar')">
        <div class="filtre-bar">
          <DatePicker
            v-model="karZararBaslangic"
            date-format="dd/mm/yy"
            :placeholder="t('muhasebe.baslangic')"
          />
          <DatePicker
            v-model="karZararBitis"
            date-format="dd/mm/yy"
            :placeholder="t('muhasebe.bitis')"
          />
          <Button
            icon="pi pi-refresh"
            :label="t('muhasebe.hesapla')"
            class="p-button-sm"
            @click="karZararYukle"
          />
          <Button
            icon="pi pi-file-excel"
            :label="t('muhasebe.excel')"
            class="p-button-sm p-button-outlined"
            @click="karZararExcelIndir"
          />
        </div>
        <div class="finansal-grid">
          <div class="finansal-kolon">
            <h3 class="finansal-baslik">
              {{ t('muhasebe.gelirler') }}
            </h3>
            <DataTable
              :value="karZarar.gelirler || []"
              striped-rows
              size="small"
              :loading="karZararYukleniyor"
            >
              <Column
                field="kod"
                :header="t('muhasebe.kod')"
              />
              <Column
                field="ad"
                :header="t('muhasebe.hesap')"
              />
              <Column :header="t('common.amount')">
                <template #body="{ data }">
                  {{ formatCurrency(data.tutar) }}
                </template>
              </Column>
            </DataTable>
          </div>
          <div class="finansal-kolon">
            <h3 class="finansal-baslik">
              {{ t('muhasebe.giderler') }}
            </h3>
            <DataTable
              :value="karZarar.giderler || []"
              striped-rows
              size="small"
              :loading="karZararYukleniyor"
            >
              <Column
                field="kod"
                :header="t('muhasebe.kod')"
              />
              <Column
                field="ad"
                :header="t('muhasebe.hesap')"
              />
              <Column :header="t('common.amount')">
                <template #body="{ data }">
                  {{ formatCurrency(data.tutar) }}
                </template>
              </Column>
            </DataTable>
          </div>
        </div>
        <div class="net-kar-kutusu">
          {{ t('muhasebe.net') }} {{ (karZarar.netKar || 0) >= 0 ? t('muhasebe.kar') : t('muhasebe.zarar') }}:
          <strong :class="(karZarar.netKar || 0) >= 0 ? 'kar' : 'zarar'">
            {{ formatCurrency(Math.abs(karZarar.netKar || 0)) }}
          </strong>
        </div>
        <div class="grafik-kutu">
          <Bar
            :data="karZararChartData"
            :options="{ responsive: true, maintainAspectRatio: false, plugins: { legend: { display: false } } }"
            style="height: 220px"
          />
        </div>
      </TabPanel>
    </TabView>

    <!-- HESAP DIALOG -->
    <Dialog
      v-model:visible="hesapDialog"
      :header="hesapDialogBaslik"
      modal
      :style="{ width: '480px' }"
    >
      <div class="form-grid">
        <div class="field">
          <label class="zorunlu">{{ t('muhasebe.hesapKodu') }}</label>
          <InputText
            v-model="hesapForm.kod"
            class="w-full"
            :placeholder="t('muhasebe.hesapKoduPlaceholder')"
            :class="{ 'p-invalid': hesapFormHatali.kod }"
          />
          <small
            v-if="hesapFormHatali.kod"
            class="hata-mesaj"
          >{{ t('muhasebe.hesapKoduZorunlu') }}</small>
        </div>
        <div class="field">
          <label class="zorunlu">{{ t('muhasebe.hesapAdi') }}</label>
          <InputText
            v-model="hesapForm.ad"
            class="w-full"
            :class="{ 'p-invalid': hesapFormHatali.ad }"
          />
          <small
            v-if="hesapFormHatali.ad"
            class="hata-mesaj"
          >{{ t('muhasebe.hesapAdiZorunlu') }}</small>
        </div>
        <div class="field">
          <label>{{ t('muhasebe.tipZorunlu') }}</label>
          <Select
            v-model="hesapForm.tip"
            :options="['AKTIF', 'PASIF', 'GELIR', 'GIDER']"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('muhasebe.grup') }}</label><InputText
            v-model="hesapForm.grup"
            class="w-full"
          />
        </div>
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          icon="pi pi-times"
          class="p-button-text"
          @click="hesapDialog = false"
        />
        <Button
          :label="t('common.save')"
          icon="pi pi-check"
          :loading="kaydediliyor"
          @click="hesapKaydet"
        />
      </template>
    </Dialog>

    <!-- YEVMIYE FİŞİ DIALOG -->
    <Dialog
      v-model:visible="fisDialog"
      :header="t('muhasebe.yeniYevmiyeFisi')"
      modal
      :style="{ width: '640px' }"
    >
      <div class="form-grid">
        <div class="field">
          <label>{{ t('muhasebe.tarihZorunlu') }}</label><DatePicker
            v-model="fisForm.tarih"
            date-format="dd/mm/yy"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('common.description') }}</label><InputText
            v-model="fisForm.aciklama"
            class="w-full"
          />
        </div>
      </div>
      <div class="fis-kalemler">
        <div class="fis-kalem-baslik">
          <span>{{ t('muhasebe.hesap') }}</span><span>{{ t('muhasebe.borc') }}</span><span>{{ t('muhasebe.alacak') }}</span><span />
        </div>
        <div
          v-for="(k, i) in fisForm.kalemler"
          :key="i"
          class="fis-kalem"
        >
          <Select
            v-model="k.hesapKodu"
            :options="hesapSecenekleri"
            option-label="ad"
            option-value="kod"
            :placeholder="t('muhasebe.hesapSecin')"
            class="kalem-hesap"
          />
          <InputNumber
            v-model="k.borc"
            mode="currency"
            currency="TRY"
            class="kalem-tutar"
          />
          <InputNumber
            v-model="k.alacak"
            mode="currency"
            currency="TRY"
            class="kalem-tutar"
          />
          <Button
            icon="pi pi-times"
            class="p-button-rounded p-button-text p-button-danger"
            @click="fisForm.kalemler.splice(i, 1)"
          />
        </div>
        <Button
          :label="t('muhasebe.kalemEkle')"
          icon="pi pi-plus"
          class="p-button-sm p-button-text"
          @click="kalemEkle"
        />
      </div>
      <div class="fis-toplam">
        {{ t('muhasebe.toplamBorc') }} <strong>{{ formatCurrency(fisToplamBorc) }}</strong> &nbsp;|&nbsp; {{ t('muhasebe.toplamAlacak') }}
        <strong>{{ formatCurrency(fisToplamAlacak) }}</strong>
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          icon="pi pi-times"
          class="p-button-text"
          @click="fisDialog = false"
        />
        <Button
          :label="t('muhasebe.fisiKaydet')"
          icon="pi pi-check"
          :loading="kaydediliyor"
          @click="fisKaydet"
        />
      </template>
    </Dialog>

    <!-- FİŞ DETAY DIALOG -->
    <Dialog
      v-model:visible="fisDetayDialog"
      :header="t('muhasebe.fisDetayi')"
      modal
      :style="{ width: '560px' }"
    >
      <div class="fis-detay-baslik">
        <strong>{{ fisDetay?.fisNo }}</strong> — {{ formatDate(fisDetay?.tarih) }}
        <Tag
          :value="fisDetay?.durum"
          :severity="fisDetay?.durum === 'ONAYLANDI' ? 'success' : fisDetay?.durum === 'IPTAL' ? 'danger' : 'info'"
        />
      </div>
      <p
        v-if="fisDetay?.aciklama"
        class="fis-detay-aciklama"
      >
        {{ fisDetay.aciklama }}
      </p>
      <DataTable
        :value="fisDetay?.kalemler || []"
        striped-rows
      >
        <Column
          field="hesapKodu"
          :header="t('muhasebe.hesap')"
          style="width: 90px"
        />
        <Column
          field="hesapAdi"
          :header="t('muhasebe.hesapAdi')"
        />
        <Column
          field="borc"
          :header="t('muhasebe.borc')"
        >
          <template #body="{ data }">
            {{ formatCurrency(data.borc) }}
          </template>
        </Column>
        <Column
          field="alacak"
          :header="t('muhasebe.alacak')"
        >
          <template #body="{ data }">
            {{ formatCurrency(data.alacak) }}
          </template>
        </Column>
      </DataTable>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useToast } from 'primevue/usetoast'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useConfirm } from 'primevue/useconfirm'
import { muhasebeAPI, excelAPI } from '../api/index.js'
import SatirEylemleri from '../components/SatirEylemleri.vue'
import IlkZiyaretIpuclari from '../components/IlkZiyaretIpuclari.vue'
import { useGeriAl } from '../composables/useGeriAl.js'
import { formatCurrency } from '../utils/format.js'
import { Doughnut, Bar } from 'vue-chartjs'
import { Chart as ChartJS, ArcElement, Tooltip, Legend, CategoryScale, LinearScale, BarElement } from 'chart.js'
import { useI18n } from 'vue-i18n'

ChartJS.register(ArcElement, Tooltip, Legend, CategoryScale, LinearScale, BarElement)

const toast = useToast()
const toastBildirim = useToastBildirim()
const confirm = useConfirm()
const { silVeGeriAl } = useGeriAl()
const { t } = useI18n()

const aktifSekme = ref(0)
const yukleniyor = ref(false)
const kaydediliyor = ref(false)

// Hesap planı
const hesaplar = ref([])
const hesapDialog = ref(false)
const hesapDuzenleme = ref(false)
const hesapForm = ref({ kod: '', ad: '', tip: 'AKTIF', grup: '' })
const hesapFormHatali = ref({ kod: false, ad: false })
const hesapDialogBaslik = computed(() => (hesapDuzenleme.value ? t('muhasebe.hesapDuzenle') : t('muhasebe.yeniHesap')))

// Fişler
const fisler = ref([])
const fisYukleniyor = ref(false)
const fisDialog = ref(false)
const fisDetayDialog = ref(false)
const fisDetay = ref(null)
const filtreBaslangic = ref(null)
const filtreBitis = ref(null)
const fisForm = ref({ tarih: new Date(), aciklama: '', kalemler: [{ hesapKodu: null, borc: 0, alacak: 0 }] })

// Mizan
const mizan = ref([])
const mizanYukleniyor = ref(false)
const mizanBaslangic = ref(null)
const mizanBitis = ref(null)

// Defter-i kebir
const kebir = ref([])
const kebirYukleniyor = ref(false)
const kebirHesap = ref(null)
const kebirBaslangic = ref(null)
const kebirBitis = ref(null)

// Bilanço
const bilanco = ref({})
const bilancoYukleniyor = ref(false)

// Kâr / Zarar
const karZarar = ref({})
const karZararYukleniyor = ref(false)
const karZararBaslangic = ref(null)
const karZararBitis = ref(null)

const hesapSecenekleri = computed(() => hesaplar.value.map((h) => ({ ad: `${h.kod} - ${h.ad}`, kod: h.kod })))
const fisToplamBorc = computed(() => (fisForm.value.kalemler || []).reduce((t, k) => t + (Number(k.borc) || 0), 0))
const fisToplamAlacak = computed(() => (fisForm.value.kalemler || []).reduce((t, k) => t + (Number(k.alacak) || 0), 0))

const bilancoChartData = computed(() => ({
  labels: [t('muhasebe.aktif'), t('muhasebe.pasif')],
  datasets: [{
    data: [Number(bilanco.value.aktifToplam) || 0, Number(bilanco.value.pasifToplam) || 0],
    backgroundColor: ['#3b82f6', '#f59e0b']
  }]
}))

const karZararChartData = computed(() => ({
  labels: [t('muhasebe.gelir'), t('muhasebe.gider'), t('muhasebe.netKar')],
  datasets: [{
    label: t('muhasebe.tutar'),
    data: [
      Number(karZarar.value.gelirToplam) || 0,
      Number(karZarar.value.giderToplam) || 0,
      Number(karZarar.value.netKar) || 0
    ],
    backgroundColor: ['#10b981', '#ef4444', '#3b82f6']
  }]
}))

import { formatTarih as formatDate } from '../utils/format.js'

const tipEtiketi = (tip) => ({ AKTIF: t('muhasebe.tipAktif'), PASIF: t('muhasebe.tipPasif'), GELIR: t('muhasebe.tipGelir'), GIDER: t('muhasebe.tipGider') })[tip] || tip
const tipSeverity = (tip) => ({ AKTIF: 'info', PASIF: 'warning', GELIR: 'success', GIDER: 'danger' })[tip] || 'secondary'

const tarihParam = (d) => (d ? (d.toISOString?.().split('T')[0] ?? d) : null)

onMounted(() => {
  hesaplariYukle()
  fisleriYukle()
})

const hesaplariYukle = async () => {
  yukleniyor.value = true
  try {
    const r = await muhasebeAPI.getHesapPlani()
    hesaplar.value = r.data || []
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('muhasebe.hataHesapPlani'))
  }
  yukleniyor.value = false
}

const hesapDialogAc = (data) => {
  hesapDuzenleme.value = !!data
  hesapFormHatali.value = { kod: false, ad: false }
  hesapForm.value = data ? { ...data } : { kod: '', ad: '', tip: 'AKTIF', grup: '' }
  hesapDialog.value = true
}

const hesapCogalt = (data) => {
  hesapFormHatali.value = { kod: false, ad: false }
  hesapForm.value = { ...data, id: null, kod: '', ad: data.ad + ' ' + t('muhasebe.kopya') }
  hesapDuzenleme.value = false
  hesapDialog.value = true
}

const hesapKaydet = async () => {
  const hatali = { kod: !hesapForm.value.kod?.trim(), ad: !hesapForm.value.ad?.trim() }
  hesapFormHatali.value = hatali
  if (hatali.kod || hatali.ad) {
    toastBildirim.uyari(t('muhasebe.kodAdZorunlu'))
    return
  }
  kaydediliyor.value = true
  try {
    if (hesapDuzenleme.value) await muhasebeAPI.hesapGuncelle(hesapForm.value.id, hesapForm.value)
    else await muhasebeAPI.hesapOlustur(hesapForm.value)
    toastBildirim.basarili(t('muhasebe.hesapKaydedildi'))
    hesapDialog.value = false
    hesaplariYukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('muhasebe.islemBasarisiz'))
  }
  kaydediliyor.value = false
}

const hesapSil = (data) => {
  confirm.require({
    message: t('muhasebe.hesapSilOnay', { hesap: `${data.kod} - ${data.ad}` }),
    header: t('masraflar.silmeOnayi'),
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: t('masraflar.evetSil'),
    rejectLabel: t('common.cancel'),
    accept: async () => {
      try {
        await muhasebeAPI.hesapSil(data.id)
        hesaplar.value = hesaplar.value.filter((h) => h.id !== data.id)
        silVeGeriAl({
          veri: data,
          metin: t('muhasebe.hesapKodSilindi', { kod: data.kod }),
          geriYukle: async (kayit) => {
            await muhasebeAPI.hesapOlustur({ kod: kayit.kod, ad: kayit.ad, tip: kayit.tip, grup: kayit.grup })
            hesaplariYukle()
          }
        })
        toast.add({ severity: 'success', summary: t('muhasebe.silindi'), detail: t('muhasebe.hesapSilindi'), life: 3000 })
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || t('muhasebe.silmeBasarisiz'))
      }
    }
  })
}

const fisleriYukle = async () => {
  fisYukleniyor.value = true
  try {
    const params = {}
    if (filtreBaslangic.value) params.baslangic = tarihParam(filtreBaslangic.value)
    if (filtreBitis.value) params.bitis = tarihParam(filtreBitis.value)
    const r = await muhasebeAPI.getFisler(params)
    fisler.value = r.data || []
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('muhasebe.hataFisler'))
  }
  fisYukleniyor.value = false
}

const kalemEkle = () => fisForm.value.kalemler.push({ hesapKodu: null, borc: 0, alacak: 0 })

const fisDialogAc = () => {
  fisForm.value = { tarih: new Date(), aciklama: '', kalemler: [{ hesapKodu: null, borc: 0, alacak: 0 }] }
  fisDialog.value = true
}

const fisKaydet = async () => {
  if (!fisForm.value.kalemler?.length) {
    toastBildirim.uyari(t('muhasebe.enAzBirKalem'))
    return
  }
  if (fisToplamBorc.value !== fisToplamAlacak.value) {
    toast.add({
      severity: 'error',
      summary: t('muhasebe.fisDenkDegil'),
      detail: t('muhasebe.fisDenkDegilDetay'),
      life: 5000
    })
    return
  }
  kaydediliyor.value = true
  try {
    const payload = {
      tarih: tarihParam(fisForm.value.tarih),
      aciklama: fisForm.value.aciklama,
      kalemler: fisForm.value.kalemler.map((k) => ({
        hesapKodu: k.hesapKodu,
        hesapAdi: hesaplar.value.find((h) => h.kod === k.hesapKodu)?.ad,
        borc: Number(k.borc) || 0,
        alacak: Number(k.alacak) || 0
      }))
    }
    await muhasebeAPI.fisOlustur(payload)
    toastBildirim.basarili(t('muhasebe.fisOlusturuldu'))
    fisDialog.value = false
    fisleriYukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('muhasebe.fisKaydedilemedi'))
  }
  kaydediliyor.value = false
}

const fisDetayAc = async (data) => {
  try {
    const r = await muhasebeAPI.getFis(data.id)
    fisDetay.value = r.data
    fisDetayDialog.value = true
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('muhasebe.hataFisDetay'))
  }
}

const fisIptal = (data) => {
  confirm.require({
    message: t('muhasebe.fisIptalOnay', { fisNo: data.fisNo }),
    header: t('muhasebe.iptalOnayi'),
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: t('muhasebe.evetIptalEt'),
    rejectLabel: t('muhasebe.vazgec'),
    accept: async () => {
      try {
        await muhasebeAPI.fisIptal(data.id)
        toast.add({ severity: 'success', summary: t('muhasebe.iptalEdildi'), detail: t('muhasebe.fisIptalEdildi'), life: 3000 })
        fisleriYukle()
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || t('muhasebe.iptalBasarisiz'))
      }
    }
  })
}

const mizanYukle = async () => {
  mizanYukleniyor.value = true
  try {
    const params = {}
    if (mizanBaslangic.value) params.baslangic = tarihParam(mizanBaslangic.value)
    if (mizanBitis.value) params.bitis = tarihParam(mizanBitis.value)
    const r = await muhasebeAPI.getMizan(params)
    mizan.value = r.data || []
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('muhasebe.hataMizan'))
  }
  mizanYukleniyor.value = false
}

const kebirYukle = async () => {
  kebirYukleniyor.value = true
  try {
    const params = { hesapKodu: kebirHesap.value || undefined }
    if (kebirBaslangic.value) params.baslangic = tarihParam(kebirBaslangic.value)
    if (kebirBitis.value) params.bitis = tarihParam(kebirBitis.value)
    const r = await muhasebeAPI.getDefteriKebir(params)
    kebir.value = r.data || []
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('muhasebe.hataKebir'))
  }
  kebirYukleniyor.value = false
}

const bilancoYukle = async () => {
  bilancoYukleniyor.value = true
  try {
    const r = await muhasebeAPI.getBilanco()
    bilanco.value = r.data || {}
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('muhasebe.hataBilanco'))
  } finally {
    bilancoYukleniyor.value = false
  }
}

const karZararYukle = async () => {
  karZararYukleniyor.value = true
  try {
    const params = {}
    if (karZararBaslangic.value) params.baslangic = tarihParam(karZararBaslangic.value)
    if (karZararBitis.value) params.bitis = tarihParam(karZararBitis.value)
    const r = await muhasebeAPI.getKarZarar(params)
    karZarar.value = r.data || {}
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('muhasebe.hataKarZarar'))
  } finally {
    karZararYukleniyor.value = false
  }
}

const excelIndir = (res, dosyaAdi) => {
  const url = window.URL.createObjectURL(new Blob([res.data]))
  const link = document.createElement('a')
  link.href = url
  link.setAttribute('download', `${dosyaAdi}-${new Date().toISOString().split('T')[0]}.xlsx`)
  document.body.appendChild(link)
  link.click()
  link.remove()
  window.URL.revokeObjectURL(url)
}

const bilancoExcelIndir = async () => {
  try {
    const res = await excelAPI.bilanco()
    excelIndir(res, 'bilanco')
  } catch {
    toastBildirim.hata(t('muhasebe.hataBilancoExcel'))
  }
}

const karZararExcelIndir = async () => {
  try {
    const params = {}
    if (karZararBaslangic.value) params.baslangic = tarihParam(karZararBaslangic.value)
    if (karZararBitis.value) params.bitis = tarihParam(karZararBitis.value)
    const res = await excelAPI.karZarar(params)
    excelIndir(res, 'kar-zarar')
  } catch {
    toastBildirim.hata(t('muhasebe.hataKarZararExcel'))
  }
}
</script>

<style scoped>
.muhasebe-container {
  padding: 0;
}
.sayfa-baslik {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}
.filtre-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.form-grid {
  display: flex;
  flex-direction: column;
  gap: 14px;
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
.zorunlu::after {
  content: ' *';
  color: #ef4444;
}
.hata-mesaj {
  color: #ef4444;
  font-size: 12px;
}
:deep(.p-invalid) {
  border-color: #ef4444 !important;
}
.fis-kalemler {
  margin-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.fis-kalem-baslik,
.fis-kalem {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(80px, 140px) minmax(80px, 140px) 40px;
  gap: 8px;
  align-items: center;
}
.fis-kalem-baslik span {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-muted);
}
.fis-toplam {
  margin-top: 14px;
  padding: 10px 14px;
  background: rgba(59, 130, 246, 0.08);
  border-radius: 8px;
  font-size: 13px;
}
.fis-detay-baslik {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
  font-size: 14px;
}
.fis-detay-aciklama {
  color: var(--text-secondary);
  margin-bottom: 12px;
}
.kebir-select {
  min-width: min(240px, 100%);
}
.pozitif {
  color: #10b981;
  font-weight: 600;
}
.negatif {
  color: #ef4444;
  font-weight: 600;
}
.finansal-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}
.finansal-kolon {
  min-width: 0;
}
.finansal-baslik {
  font-size: 1rem;
  font-weight: 700;
  margin: 0 0 8px;
  color: var(--text-primary);
}
.finansal-toplam {
  margin-top: 8px;
  font-size: 0.9rem;
  color: var(--text-secondary);
}
.net-kar-kutusu {
  margin-top: 16px;
  padding: 12px 16px;
  border-radius: 8px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  font-size: 1rem;
  color: var(--text-primary);
}
.net-kar-kutusu .kar {
  color: #10b981;
}
.net-kar-kutusu .zarar {
  color: #ef4444;
}
.grafik-kutu {
  margin-top: 16px;
  padding: 16px;
  border-radius: 8px;
  background: var(--bg-card);
  border: 1px solid var(--border);
}
@media (max-width: 768px) {
  .finansal-grid {
    grid-template-columns: 1fr;
  }
}
</style>
