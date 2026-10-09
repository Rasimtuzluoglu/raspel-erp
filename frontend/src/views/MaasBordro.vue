<template>
  <div class="maas-container">
    <PageHeader
      :title="t('maasBordro.title')"
      :subtitle="t('maasBordro.subtitle')"
    >
      <template #actions>
        <Button
          :label="t('maasBordro.yeniBordro')"
          icon="pi pi-plus"
          @click="dialogAc()"
        />
        <Button
          :label="t('maasBordro.bordroAyarlari')"
          icon="pi pi-cog"
          class="p-button-outlined"
          @click="ayarDialogAc"
        />
        <Button
          :label="t('maasBordro.topluUret')"
          icon="pi pi-users"
          class="p-button-outlined"
          @click="topluDialogAc"
        />
      </template>
    </PageHeader>

    <!-- KPI şeridi -->
    <div class="kpi-serit">
      <div class="kpi-kart">
        <span class="kpi-etiket">{{ t('maasBordro.kpiAdet') }}</span>
        <strong class="kpi-deger gizli-veri">{{ kpi.adet || 0 }}</strong>
      </div>
      <div class="kpi-kart">
        <span class="kpi-etiket">{{ t('maasBordro.kpiBrut') }}</span>
        <strong class="kpi-deger gizli-veri">{{ formatCurrency(kpi.toplamBrut) }}</strong>
      </div>
      <div class="kpi-kart">
        <span class="kpi-etiket">{{ t('maasBordro.kpiKesinti') }}</span>
        <strong class="kpi-deger gizli-veri">{{ formatCurrency(kpi.toplamKesinti) }}</strong>
      </div>
      <div class="kpi-kart">
        <span class="kpi-etiket">{{ t('maasBordro.kpiNet') }}</span>
        <strong class="kpi-deger gizli-veri">{{ formatCurrency(kpi.toplamNet) }}</strong>
      </div>
      <div class="kpi-kart">
        <span class="kpi-etiket">{{ t('maasBordro.kpiNetOdenen') }}</span>
        <strong class="kpi-deger gizli-veri">{{ formatCurrency(kpi.odenenNet) }}</strong>
      </div>
    </div>

    <!-- Filtre çubuğu -->
    <div class="filtre-cubugu">
      <Dropdown
        v-model="filtre.yil"
        :options="yilSecenekleri"
        :placeholder="t('maasBordro.yil')"
        show-clear
        class="filtre-alan"
        @change="filtrele"
      />
      <Dropdown
        v-model="filtre.ay"
        :options="aySecenekleri"
        option-label="ad"
        option-value="deger"
        :placeholder="t('maasBordro.ay')"
        show-clear
        class="filtre-alan"
        @change="filtrele"
      />
      <Dropdown
        v-model="filtre.durum"
        :options="durumSecenekleri"
        option-label="ad"
        option-value="deger"
        :placeholder="t('maasBordro.durum')"
        show-clear
        class="filtre-alan"
        @change="filtrele"
      />
      <Dropdown
        v-model="filtre.odemeDurumu"
        :options="odemeSecenekleri"
        option-label="ad"
        option-value="deger"
        :placeholder="t('maasBordro.odemeDurumu')"
        show-clear
        class="filtre-alan"
        @change="filtrele"
      />
      <IconField class="filtre-arama">
        <InputIcon class="pi pi-search" />
        <InputText
          v-model="filtre.q"
          :placeholder="t('maasBordro.filtrePersonel')"
          @input="filtrele"
        />
      </IconField>
      <Button
        :label="t('maasBordro.temizle')"
        icon="pi pi-filter-slash"
        class="p-button-text p-button-sm"
        @click="filtreTemizle"
      />
    </div>

    <DataTable
      :value="list"
      striped-rows
      :loading="yukleniyor"
      lazy
      :paginator="true"
      :rows="satirSayisi"
      :total-records="toplam"
      :first="sayfa * satirSayisi"
      @page="sayfaDegisti"
    >
      <template #empty>
        <EmptyState />
      </template>
      <Column
        field="personelAdi"
        :header="t('maasBordro.personel')"
        sortable
      />
      <Column
        field="yil"
        :header="t('maasBordro.yil')"
        sortable
        style="width: 80px"
      />
      <Column
        field="ay"
        :header="t('maasBordro.ay')"
        sortable
        style="width: 70px"
      />
      <Column
        field="brutMaas"
        :header="t('maasBordro.brut')"
      >
        <template #body="{ data }">
          <span class="gizli-veri sayisal">{{ formatCurrency(data.brutMaas) }}</span>
        </template>
      </Column>
      <Column
        field="kesintiler"
        :header="t('maasBordro.kesintiler')"
      >
        <template #body="{ data }">
          <span class="gizli-veri sayisal">{{ formatCurrency(data.kesintiler) }}</span>
        </template>
      </Column>
      <Column
        field="netMaas"
        :header="t('maasBordro.net')"
      >
        <template #body="{ data }">
          <span class="gizli-veri sayisal">{{ formatCurrency(data.netMaas) }}</span>
        </template>
      </Column>
      <Column
        field="durum"
        :header="t('maasBordro.durum')"
        style="width: 120px"
      >
        <template #body="{ data }">
          <span
            class="durum-rozet"
            :class="data.durum === 'ONAYLANDI' ? 'durum-onayli' : 'durum-taslak'"
          >
            {{ data.durum === 'ONAYLANDI' ? t('maasBordro.onaylandi') : t('maasBordro.taslak') }}
          </span>
        </template>
      </Column>
      <Column
        field="odemeDurumu"
        :header="t('maasBordro.odemeDurumu')"
        style="width: 110px"
      >
        <template #body="{ data }">
          <span
            class="durum-rozet"
            :class="data.odemeDurumu === 'ODENDI' ? 'durum-onayli' : 'durum-taslak'"
          >
            {{ data.odemeDurumu === 'ODENDI' ? t('maasBordro.odendi') : t('maasBordro.odenmedi') }}
          </span>
        </template>
      </Column>
      <Column
        :header="t('maasBordro.islem')"
        style="width: 230px"
      >
        <template #body="{ data }">
          <div class="islem-hucre">
            <Button
              icon="pi pi-print"
              :aria-label="t('maasBordro.fisYazdir')"
              :title="t('maasBordro.fisYazdir')"
              class="p-button-rounded p-button-text p-button-sm"
              @click="fisYazdir(data)"
            />
            <Button
              v-if="data.durum !== 'ONAYLANDI'"
              icon="pi pi-pencil"
              :aria-label="t('common.edit')"
              :title="t('common.edit')"
              class="p-button-rounded p-button-text p-button-sm"
              @click="dialogAc(data)"
            />
            <Button
              v-if="data.durum !== 'ONAYLANDI'"
              icon="pi pi-check"
              :aria-label="t('maasBordro.onayla')"
              :title="t('maasBordro.onayla')"
              class="p-button-rounded p-button-text p-button-sm"
              @click="onayla(data)"
            />
            <Button
              v-if="data.durum === 'ONAYLANDI' && data.odemeDurumu !== 'ODENDI'"
              icon="pi pi-wallet"
              :aria-label="t('maasBordro.ode')"
              :title="t('maasBordro.ode')"
              class="p-button-rounded p-button-text p-button-sm"
              @click="odemeDialogAc(data)"
            />
            <Button
              v-if="data.durum === 'ONAYLANDI'"
              icon="pi pi-lock-open"
              :aria-label="t('maasBordro.onayKaldir')"
              :title="t('maasBordro.onayKaldir')"
              class="p-button-rounded p-button-text p-button-sm"
              @click="onayKaldir(data)"
            />
            <Button
              v-if="data.durum !== 'ONAYLANDI'"
              icon="pi pi-trash"
              :aria-label="$t('common.delete')"
              :title="$t('common.delete')"
              class="p-button-rounded p-button-text p-button-sm"
              @click="sil(data)"
            />
          </div>
        </template>
      </Column>
    </DataTable>

    <!-- Bordro ödemesi: kasa seçimi -->
    <Dialog
      v-model:visible="odemeDialog"
      :header="t('maasBordro.ode')"
      modal
      :style="{ width: '440px' }"
    >
      <div class="form-grid">
        <div class="field">
          <label>{{ t('maasBordro.odemeKasa') }}</label>
          <Dropdown
            v-model="odemeKasaId"
            :options="kasaListesi"
            option-label="ad"
            option-value="id"
            :placeholder="t('maasBordro.kasaSec')"
            class="w-full"
          />
        </div>
        <p class="odeme-bilgi">
          {{ t('maasBordro.odemeAciklama', { tutar: formatCurrency(odemeBordro?.netMaas || 0) }) }}
        </p>
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          class="p-button-text"
          @click="odemeDialog = false"
        />
        <Button
          :label="t('maasBordro.ode')"
          icon="pi pi-wallet"
          :loading="odemeGonderiliyor"
          :disabled="!odemeKasaId"
          @click="odemeYap"
        />
      </template>
    </Dialog>

    <Dialog
      v-model:visible="dialog"
      :header="dialogHeader"
      modal
      :style="{ width: '620px' }"
    >
      <div class="form-grid">
        <div class="field">
          <label>{{ t('maasBordro.personelZorunlu') }}</label>
          <Dropdown
            v-model="form.personelId"
            :options="personelListesi"
            option-label="displayName"
            option-value="id"
            :placeholder="t('maasBordro.personelSec')"
            class="w-full"
            filter
          />
        </div>
        <div class="field-row">
          <div class="field">
            <label>{{ t('maasBordro.yil') }}</label>
            <InputNumber
              v-model="form.yil"
              class="w-full"
              :min="2000"
              :max="2100"
              :use-grouping="false"
            />
          </div>
          <div class="field">
            <label>{{ t('maasBordro.ay') }}</label>
            <InputNumber
              v-model="form.ay"
              class="w-full"
              :min="1"
              :max="12"
            />
          </div>
          <div class="field">
            <label>{{ t('maasBordro.odemeTarihi') }}</label>
            <DatePicker
              v-model="form.odemeTarihi"
              date-format="dd.mm.yy"
              class="w-full"
            />
          </div>
        </div>
        <div class="field">
          <label>{{ t('maasBordro.brutMaasZorunlu') }}</label>
          <InputNumber
            v-model="form.brutMaas"
            mode="currency"
            currency="TRY"
            locale="tr-TR"
            class="w-full"
          />
        </div>

        <!-- Canlı hesaplama dökümü -->
        <div class="hesap-kutu">
          <div class="hesap-baslik">
            <i class="pi pi-calculator" /> {{ t('maasBordro.hesaplama') }}
            <small v-if="hesaplaniyor">{{ t('maasBordro.hesaplaniyor') }}</small>
          </div>
          <div
            v-if="hesaplama"
            class="hesap-satirlar"
          >
            <div class="hesap-satir">
              <span>{{ t('maasBordro.sgkKesintisi') }}</span><span>{{ formatCurrency(hesaplama.sgkIsciKesintisi) }}</span>
            </div>
            <div class="hesap-satir">
              <span>{{ t('maasBordro.gelirVergisi') }}</span><span>{{ formatCurrency(hesaplama.gelirVergisi) }}</span>
            </div>
            <div class="hesap-satir">
              <span>{{ t('maasBordro.damgaVergisi') }}</span><span>{{ formatCurrency(hesaplama.damgaVergisi) }}</span>
            </div>
            <div class="hesap-satir">
              <span>{{ t('maasBordro.gelirVergisiMatrahi') }}</span><span>{{ formatCurrency(hesaplama.gelirVergisiMatrahi) }}</span>
            </div>
            <div class="hesap-satir">
              <span>{{ t('maasBordro.isverenMaliyeti') }}</span><span>{{ formatCurrency(hesaplama.isverenMaliyeti) }}</span>
            </div>
            <p
              v-if="hesaplama.aciklama"
              class="hesap-uyari"
            >
              <i class="pi pi-exclamation-triangle" /> {{ hesaplama.aciklama }}
            </p>
          </div>
          <p
            v-else
            class="hesap-bos"
          >
            {{ t('maasBordro.hesapBekliyor') }}
          </p>
        </div>

        <div class="field-row">
          <div class="field">
            <label>{{ t('maasBordro.kesintiler') }}</label>
            <InputNumber
              v-model="form.kesintiler"
              mode="currency"
              currency="TRY"
              locale="tr-TR"
              class="w-full"
              @input="kesintiElle = true"
            />
          </div>
          <div class="field">
            <label>{{ t('maasBordro.netMaasHesaplanan') }}</label>
            <span class="net-deger">{{ formatCurrency(netOnizleme) }}</span>
          </div>
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
      v-model:visible="ayarDialog"
      :header="t('maasBordro.bordroAyarlari')"
      modal
      :style="{ width: '620px' }"
    >
      <div class="form-grid">
        <div class="field-row">
          <div class="field">
            <label>{{ t('maasBordro.yil') }}</label>
            <InputNumber
              v-model="ayarForm.yil"
              :use-grouping="false"
              class="w-full"
            />
          </div>
          <div class="field">
            <label>{{ t('maasBordro.asgariUcret') }}</label>
            <InputNumber
              v-model="ayarForm.asgariUcret"
              mode="currency"
              currency="TRY"
              locale="tr-TR"
              class="w-full"
            />
          </div>
        </div>
        <div class="field-row">
          <div class="field">
            <label>{{ t('maasBordro.sgkIsciOrani') }}</label>
            <InputNumber
              v-model="ayarForm.sgkIsciOrani"
              :min="0"
              :max="100"
              suffix=" %"
              class="w-full"
            />
          </div>
          <div class="field">
            <label>{{ t('maasBordro.issizlikIsciOrani') }}</label>
            <InputNumber
              v-model="ayarForm.issizlikIsciOrani"
              :min="0"
              :max="100"
              suffix=" %"
              class="w-full"
            />
          </div>
        </div>
        <div class="field-row">
          <div class="field">
            <label>{{ t('maasBordro.sgkIsverenOrani') }}</label>
            <InputNumber
              v-model="ayarForm.sgkIsverenOrani"
              :min="0"
              :max="100"
              suffix=" %"
              class="w-full"
            />
          </div>
          <div class="field">
            <label>{{ t('maasBordro.issizlikIsverenOrani') }}</label>
            <InputNumber
              v-model="ayarForm.issizlikIsverenOrani"
              :min="0"
              :max="100"
              suffix=" %"
              class="w-full"
            />
          </div>
          <div class="field">
            <label>{{ t('maasBordro.damgaOrani') }}</label>
            <InputNumber
              v-model="ayarForm.damgaOrani"
              :min="0"
              :max="100"
              :max-fraction-digits="3"
              suffix=" %"
              class="w-full"
            />
          </div>
        </div>

        <!-- Gelir vergisi dilimleri: tablo editörü (JSON yerine) -->
        <div class="field">
          <label>{{ t('maasBordro.vergiDilimleri') }}</label>
          <div class="dilim-baslik">
            <span>{{ t('maasBordro.dilimLimit') }}</span>
            <span>{{ t('maasBordro.dilimOran') }}</span>
            <span />
          </div>
          <div
            v-for="(d, i) in dilimler"
            :key="i"
            class="dilim-satir"
          >
            <InputNumber
              v-model="d.limit"
              :min="0"
              :use-grouping="false"
              :placeholder="t('maasBordro.dilimSinirsiz')"
              class="dilim-limit"
            />
            <InputNumber
              v-model="d.oran"
              :min="0"
              :max="100"
              suffix=" %"
              class="dilim-oran"
            />
            <Button
              icon="pi pi-trash"
              :aria-label="$t('common.delete')"
              class="p-button-text p-button-danger p-button-sm"
              @click="dilimler.splice(i, 1)"
            />
          </div>
          <Button
            :label="t('maasBordro.dilimEkle')"
            icon="pi pi-plus"
            class="p-button-sm p-button-text"
            @click="dilimler.push({ limit: null, oran: 15 })"
          />
          <small class="dilim-not">{{ t('maasBordro.dilimNot') }}</small>
        </div>
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          class="p-button-text"
          @click="ayarDialog = false"
        />
        <Button
          :label="t('common.save')"
          icon="pi pi-check"
          :loading="kaydediliyor"
          @click="ayarKaydet"
        />
      </template>
    </Dialog>

    <Dialog
      v-model:visible="topluDialog"
      :header="t('maasBordro.topluUret')"
      modal
      :style="{ width: '680px' }"
    >
      <div class="form-grid">
        <div class="field-row">
          <div class="field">
            <label>{{ t('maasBordro.yil') }}</label>
            <InputNumber
              v-model="topluForm.yil"
              :use-grouping="false"
              class="w-full"
              @update:model-value="topluOnizle"
            />
          </div>
          <div class="field">
            <label>{{ t('maasBordro.ay') }}</label>
            <InputNumber
              v-model="topluForm.ay"
              :min="1"
              :max="12"
              show-buttons
              class="w-full"
              @update:model-value="topluOnizle"
            />
          </div>
        </div>
        <small class="toplu-not">{{ t('maasBordro.topluNot') }}</small>

        <div
          v-if="onizlemeYukleniyor"
          class="toplu-bos"
        >
          {{ t('common.loading') }}
        </div>
        <template v-else>
          <div class="onizleme-ozet">
            {{ t('maasBordro.onizlemeOzet', { uygun: uygunSayisi, atlanan: atlananSayisi }) }}
          </div>
          <DataTable
            :value="onizleme"
            size="small"
            :rows="8"
            :paginator="onizleme.length > 8"
            scrollable
            scroll-height="260px"
          >
            <Column style="width: 44px">
              <template #body="{ data }">
                <Checkbox
                  v-if="data.durum === 'UYGUN'"
                  v-model="data.secili"
                  binary
                />
              </template>
            </Column>
            <Column
              field="personelAdi"
              :header="t('maasBordro.personel')"
            />
            <Column :header="t('maasBordro.brut')">
              <template #body="{ data }">
                <span class="sayisal">{{ formatCurrency(data.brutMaas) }}</span>
              </template>
            </Column>
            <Column :header="t('maasBordro.net')">
              <template #body="{ data }">
                <span class="sayisal">{{ data.netMaas != null ? formatCurrency(data.netMaas) : '-' }}</span>
              </template>
            </Column>
            <Column :header="t('maasBordro.durum')">
              <template #body="{ data }">
                <span
                  class="durum-rozet"
                  :class="data.durum === 'UYGUN' ? 'durum-onayli' : 'durum-taslak'"
                >{{ topluDurumEtiket(data.durum) }}</span>
              </template>
            </Column>
          </DataTable>
        </template>
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          class="p-button-text"
          @click="topluDialog = false"
        />
        <Button
          :label="t('maasBordro.uret')"
          icon="pi pi-users"
          :loading="kaydediliyor"
          :disabled="uygunSayisi === 0"
          @click="topluUret"
        />
      </template>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import { unwrapList } from '../api/utils/unwrap.js'
import { useToast } from 'primevue/usetoast'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useConfirm } from 'primevue/useconfirm'
import { maasBordroAPI, personelAPI, kasaAPI } from '../api/index.js'
import { formatCurrency, getLocalDateString } from '../utils/format.js'
import { fisPenceresiAcVeYazdir } from '../utils/fisYazdir.js'
import { useI18n } from 'vue-i18n'

const toast = useToast()
const toastBildirim = useToastBildirim()
const confirm = useConfirm()
const { t, locale } = useI18n()

const list = ref([])
const personelListesi = ref([])
const kasaListesi = ref([])
const yukleniyor = ref(false)
const kaydediliyor = ref(false)
const dialog = ref(false)
const duzenleme = ref(false)
const sayfa = ref(0)
const satirSayisi = ref(25)
const toplam = ref(0)
const kpi = ref({ adet: 0, toplamBrut: 0, toplamKesinti: 0, toplamNet: 0, odenenNet: 0 })

const filtre = ref({ yil: null, ay: null, durum: null, odemeDurumu: null, q: '' })
const buYil = new Date().getFullYear()

const yilSecenekleri = computed(() => {
  const yillar = []
  for (let y = buYil + 1; y >= buYil - 6; y--) yillar.push(y)
  return yillar
})
const aySecenekleri = computed(() =>
  Array.from({ length: 12 }, (_, i) => ({
    deger: i + 1,
    ad: new Date(2000, i, 1).toLocaleDateString(locale.value === 'en' ? 'en-US' : 'tr-TR', { month: 'long' })
  }))
)
const durumSecenekleri = computed(() => [
  { deger: 'TASLAK', ad: t('maasBordro.taslak') },
  { deger: 'ONAYLANDI', ad: t('maasBordro.onaylandi') }
])
const odemeSecenekleri = computed(() => [
  { deger: 'ODENMEDI', ad: t('maasBordro.odenmedi') },
  { deger: 'ODENDI', ad: t('maasBordro.odendi') }
])

const filtreParams = () => {
  const p = {}
  if (filtre.value.yil != null) p.yil = filtre.value.yil
  if (filtre.value.ay != null) p.ay = filtre.value.ay
  if (filtre.value.durum) p.durum = filtre.value.durum
  if (filtre.value.odemeDurumu) p.odemeDurumu = filtre.value.odemeDurumu
  if (filtre.value.q?.trim()) p.q = filtre.value.q.trim()
  return p
}

const listele = async () => {
  yukleniyor.value = true
  try {
    const params = { ...filtreParams(), page: sayfa.value, size: satirSayisi.value }
    const [lR, oR] = await Promise.all([maasBordroAPI.getAll(params), maasBordroAPI.ozet(filtreParams())])
    list.value = unwrapList(lR)
    toplam.value = lR.data?.totalElements ?? list.value.length
    kpi.value = { ...kpi.value, ...(oR.data || {}) }
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('maasBordro.hataYukleme'))
  } finally {
    yukleniyor.value = false
  }
}

let filtreZaman = null
const filtrele = () => {
  if (filtreZaman) clearTimeout(filtreZaman)
  filtreZaman = setTimeout(() => {
    sayfa.value = 0
    listele()
  }, 250)
}

const filtreTemizle = () => {
  filtre.value = { yil: null, ay: null, durum: null, odemeDurumu: null, q: '' }
  sayfa.value = 0
  listele()
}

const sayfaDegisti = (e) => {
  sayfa.value = e.page
  satirSayisi.value = e.rows
  listele()
}

const form = ref({
  personelId: null,
  yil: buYil,
  ay: new Date().getMonth() + 1,
  brutMaas: 0,
  kesintiler: 0,
  odemeTarihi: new Date()
})
const hesaplama = ref(null)
const hesaplaniyor = ref(false)
const kesintiElle = ref(false)

const dialogHeader = computed(() => (duzenleme.value ? t('maasBordro.bordroDuzenle') : t('maasBordro.yeniBordro')))

const netOnizleme = computed(() => {
  if (!kesintiElle.value && hesaplama.value?.netMaas != null) return Number(hesaplama.value.netMaas)
  return Math.max(0, (Number(form.value.brutMaas) || 0) - (Number(form.value.kesintiler) || 0))
})

// Brüt/personel/dönem değişince hesaplama motorundan canlı döküm al.
let hesapZaman = null
const hesaplaTetikle = () => {
  hesaplaniyor.value = true
  const govde = {
    personelId: form.value.personelId,
    yil: form.value.yil,
    ay: form.value.ay,
    brutMaas: Number(form.value.brutMaas) || 0
  }
  return maasBordroAPI.hesapla(govde)
    .then((r) => {
      hesaplama.value = r.data || null
      if (!kesintiElle.value && hesaplama.value?.toplamKesinti != null) {
        form.value.kesintiler = Number(hesaplama.value.toplamKesinti)
      }
    })
    .catch(() => { hesaplama.value = null })
    .finally(() => { hesaplaniyor.value = false })
}

watch(
  () => [form.value.personelId, form.value.yil, form.value.ay, form.value.brutMaas],
  () => {
    if (!dialog.value) return
    if (hesapZaman) clearTimeout(hesapZaman)
    hesapZaman = setTimeout(() => {
      if (form.value.personelId && Number(form.value.brutMaas) > 0) hesaplaTetikle()
    }, 350)
  }
)

// Bordro ayarları
const ayarDialog = ref(false)
const topluDialog = ref(false)
const dilimler = ref([])
const ayarForm = ref({
  yil: buYil, asgariUcret: 0,
  sgkIsciOrani: 14, issizlikIsciOrani: 1,
  sgkIsverenOrani: 20.5, issizlikIsverenOrani: 2,
  damgaOrani: 0.759, gelirVergisiDilimleri: ''
})
const topluForm = ref({ yil: buYil, ay: new Date().getMonth() + 1 })

// Toplu üretim önizlemesi: hangi personel üretilecek/atlanacak.
const onizleme = ref([])
const onizlemeYukleniyor = ref(false)
const uygunSayisi = computed(() => onizleme.value.filter((o) => o.durum === 'UYGUN').length)
const atlananSayisi = computed(() => onizleme.value.length - uygunSayisi.value)
const topluDurumEtiket = (d) =>
  d === 'UYGUN' ? t('maasBordro.durumUygun') : d === 'MAAS_YOK' ? t('maasBordro.durumMaasYok') : t('maasBordro.durumZatenVar')

let onizlemeZaman = null
const topluOnizle = () => {
  if (onizlemeZaman) clearTimeout(onizlemeZaman)
  onizlemeZaman = setTimeout(async () => {
    onizlemeYukleniyor.value = true
    try {
      const r = await maasBordroAPI.topluOnizleme(topluForm.value.yil, topluForm.value.ay)
      onizleme.value = (r.data || []).map((o) => ({ ...o, secili: o.durum === 'UYGUN' }))
    } catch {
      onizleme.value = []
    } finally {
      onizlemeYukleniyor.value = false
    }
  }, 300)
}

const topluDialogAc = () => {
  topluDialog.value = true
  topluOnizle()
}

const dilimleriCoz = (json) => {
  try {
    const arr = typeof json === 'string' ? JSON.parse(json) : json
    if (Array.isArray(arr) && arr.length) {
      return arr.map((d) => ({ limit: d.limit ?? null, oran: Number(d.oran) || 0 }))
    }
  } catch {
    /* bozuk JSON: varsayılan dilimler */
  }
  return [{ limit: 158000, oran: 15 }, { limit: null, oran: 20 }]
}

const ayarDialogAc = async () => {
  ayarDialog.value = true
  try {
    const r = await maasBordroAPI.ayar(buYil)
    if (r.data) {
      ayarForm.value = { ...ayarForm.value, ...r.data }
      dilimler.value = dilimleriCoz(r.data.gelirVergisiDilimleri)
    }
  } catch {
    dilimler.value = dilimleriCoz(ayarForm.value.gelirVergisiDilimleri)
  }
}

const ayarKaydet = async () => {
  // Cift gonderim engeli: ayni ayar iki kez kaydedilmesin.
  if (kaydediliyor.value) return
  kaydediliyor.value = true
  try {
    const govde = {
      ...ayarForm.value,
      gelirVergisiDilimleri: JSON.stringify(
        dilimler.value
          .filter((d) => d.oran != null)
          .map((d) => ({ limit: d.limit === '' || d.limit == null ? null : d.limit, oran: d.oran }))
      )
    }
    await maasBordroAPI.ayarKaydet(govde)
    toastBildirim.basarili(t('maasBordro.ayarKaydedildi'))
    ayarDialog.value = false
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('maasBordro.islemBasarisiz'))
  } finally {
    kaydediliyor.value = false
  }
}

const topluUret = async () => {
  // Yalnızca işaretli ve uygun personel üretilir.
  const seciliIds = onizleme.value
    .filter((o) => o.durum === 'UYGUN' && o.secili)
    .map((o) => o.personelId)
  if (!seciliIds.length) return
  kaydediliyor.value = true
  try {
    const r = await maasBordroAPI.topluUret(topluForm.value.yil, topluForm.value.ay, seciliIds)
    toastBildirim.basarili(t('maasBordro.topluSonuc', {
      uretilen: r.data?.uretilen ?? 0,
      atlanan: r.data?.atlanan ?? 0
    }))
    topluDialog.value = false
    sayfa.value = 0
    await listele()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('maasBordro.islemBasarisiz'))
  } finally {
    kaydediliyor.value = false
  }
}

// Bordro ödemesi (kasa seçimi)
const odemeDialog = ref(false)
const odemeBordro = ref(null)
const odemeKasaId = ref(null)
const odemeGonderiliyor = ref(false)

const odemeDialogAc = (data) => {
  odemeBordro.value = data
  odemeKasaId.value = null
  odemeDialog.value = true
}

const odemeYap = async () => {
  if (!odemeKasaId.value) return
  odemeGonderiliyor.value = true
  try {
    await maasBordroAPI.ode(odemeBordro.value.id, odemeKasaId.value)
    toastBildirim.basarili(t('maasBordro.odemeBasarili'))
    odemeDialog.value = false
    await listele()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('maasBordro.islemBasarisiz'))
  } finally {
    odemeGonderiliyor.value = false
  }
}

const fisYazdir = (data) => {
  const html = `<!DOCTYPE html><html lang="tr"><head><meta charset="utf-8"><title>${t('maasBordro.fisBaslik')}</title>
    <style>
      @page { size: A4; margin: 15mm; }
      * { box-sizing: border-box; }
      body { font-family: -apple-system, 'Segoe UI', Roboto, sans-serif; color: #111827; margin: 0; }
      h1 { font-size: 20px; margin: 0 0 4px; }
      .alt { color: #6b7280; font-size: 12px; margin: 0 0 20px; }
      .satir { display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px solid #e5e7eb; font-size: 14px; }
      .satir.toplam { font-weight: 700; font-size: 16px; border-bottom: 2px solid #111827; }
      .etiket { color: #4b5563; }
      @media print { body { margin: 0; } }
    </style></head><body>
      <h1>${t('maasBordro.fisBaslik')}</h1>
      <p class="alt">${t('maasBordro.personel')}: ${esc(data.personelAdi || '')} — ${data.yil}/${String(data.ay).padStart(2, '0')}</p>
      <div class="satir"><span class="etiket">${t('maasBordro.brut')}</span><span>${formatCurrency(data.brutMaas || 0)}</span></div>
      <div class="satir"><span class="etiket">${t('maasBordro.gelirVergisiMatrahi')}</span><span>${formatCurrency(data.gelirVergisiMatrahi || 0)}</span></div>
      <div class="satir"><span class="etiket">${t('maasBordro.kesintiler')}</span><span>${formatCurrency(data.kesintiler || 0)}</span></div>
      <div class="satir toplam"><span>${t('maasBordro.net')}</span><span>${formatCurrency(data.netMaas || 0)}</span></div>
    </body></html>`
  const pencere = fisPenceresiAcVeYazdir(html)
  if (!pencere) toastBildirim.uyari(t('maasBordro.fisPopupEngel'))
}

const esc = (s) => String(s).replace(/[&<>"]/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]))

onMounted(async () => {
  yukleniyor.value = true
  try {
    const [pR, kR] = await Promise.all([
      personelAPI.getAll({ size: 200 }),
      kasaAPI.getAll({ size: 200 })
    ])
    personelListesi.value = unwrapList(pR).map((p) => ({
      ...p,
      displayName: p.ad && p.soyad ? `${p.ad} ${p.soyad}` : p.ad || p.id
    }))
    kasaListesi.value = unwrapList(kR)
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('maasBordro.hataYukleme'))
  } finally {
    yukleniyor.value = false
  }
  await listele()
})

const dialogAc = (data) => {
  duzenleme.value = !!data
  kesintiElle.value = !!data
  hesaplama.value = null
  form.value = data
    ? { ...data, odemeTarihi: data.odemeTarihi ? new Date(data.odemeTarihi) : new Date() }
    : {
        personelId: null,
        yil: buYil,
        ay: new Date().getMonth() + 1,
        brutMaas: 0,
        kesintiler: 0,
        odemeTarihi: new Date()
      }
  dialog.value = true
  if (data) hesaplaTetikle()
}

const kaydet = async () => {
  // Cift gonderim engeli: ayni bordro kaydi iki kez olusmasin.
  if (kaydediliyor.value) return
  kaydediliyor.value = true
  try {
    const payload = {
      ...form.value,
      odemeTarihi: form.value.odemeTarihi ? getLocalDateString(form.value.odemeTarihi) : null
    }
    if (duzenleme.value) {
      await maasBordroAPI.update(form.value.id, payload)
      toastBildirim.basarili(t('maasBordro.guncellendi'))
    } else {
      await maasBordroAPI.create(payload)
      toastBildirim.basarili(t('maasBordro.olusturuldu'))
    }
    dialog.value = false
    await listele()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('maasBordro.islemBasarisiz'))
  }
  kaydediliyor.value = false
}

const sil = (data) => {
  const personelAd = data.personelAdi || data.id
  confirm.require({
    message: t('maasBordro.silOnayMesaj', { ad: personelAd }),
    header: t('common.silmeOnayi'),
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: t('common.evetSil'),
    rejectLabel: t('common.vazgec'),
    accept: async () => {
      try {
        await maasBordroAPI.delete(data.id)
        await listele()
        toast.add({ severity: 'success', summary: t('maasBordro.silindi'), detail: t('maasBordro.bordroSilindi'), life: 3000 })
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || t('maasBordro.silmeBasarisiz'))
      }
    }
  })
}

const onayla = async (data) => {
  try {
    await maasBordroAPI.onayla(data.id)
    toast.add({ severity: 'success', summary: t('maasBordro.onaylandi'), detail: t('maasBordro.onayBasarili'), life: 3000 })
    await listele()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('maasBordro.islemBasarisiz'))
  }
}

const onayKaldir = async (data) => {
  try {
    await maasBordroAPI.onayKaldir(data.id)
    toast.add({ severity: 'success', summary: t('maasBordro.onayKaldir'), detail: t('maasBordro.onayKaldirildi'), life: 3000 })
    await listele()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('maasBordro.islemBasarisiz'))
  }
}
</script>

<style scoped>
.maas-container {
  padding: 0;
}
.kpi-serit {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 12px;
  margin-bottom: 14px;
}
.kpi-kart {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 12px 14px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.kpi-etiket {
  font-size: 11.5px;
  text-transform: uppercase;
  letter-spacing: 0.4px;
  color: var(--text-muted);
  font-weight: 600;
}
.kpi-deger {
  font-size: 18px;
  color: var(--text-primary);
  font-variant-numeric: tabular-nums;
}
.filtre-cubugu {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}
.filtre-alan {
  min-width: 130px;
}
.filtre-arama {
  min-width: 200px;
  flex: 1;
}
.islem-hucre {
  display: flex;
  gap: 2px;
  flex-wrap: wrap;
}
.sayisal {
  text-align: right;
  display: inline-block;
  width: 100%;
  font-variant-numeric: tabular-nums;
}
.durum-rozet {
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 0.8rem;
  font-weight: 600;
}
.durum-onayli {
  background: var(--green-100, #dcfce7);
  color: var(--green-700, #15803d);
}
.durum-taslak {
  background: var(--yellow-100, #fef9c3);
  color: var(--yellow-700, #a16207);
}
.form-grid {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.field-row {
  display: flex;
  gap: 12px;
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
.net-deger {
  font-weight: 700;
  font-size: 18px;
  color: var(--green-600, #16a34a);
}
.hesap-kutu {
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 12px 14px;
  background: color-mix(in srgb, var(--accent) 4%, transparent);
}
.hesap-baslik {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 13px;
  color: var(--text-primary);
  margin-bottom: 8px;
}
.hesap-baslik small {
  color: var(--text-muted);
  font-weight: 400;
}
.hesap-satirlar {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.hesap-satir {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: var(--text-secondary);
}
.hesap-uyari {
  margin: 6px 0 0;
  font-size: 12px;
  color: var(--red-500, #ef4444);
}
.hesap-bos {
  margin: 0;
  font-size: 12.5px;
  color: var(--text-muted);
}
.dilim-baslik,
.dilim-satir {
  display: grid;
  grid-template-columns: 1fr 1fr 40px;
  gap: 8px;
  align-items: center;
}
.dilim-baslik {
  font-size: 11px;
  text-transform: uppercase;
  color: var(--text-muted);
  font-weight: 600;
}
.dilim-satir {
  margin-bottom: 6px;
}
.dilim-limit,
.dilim-oran {
  width: 100%;
}
.dilim-not {
  display: block;
  margin-top: 6px;
  color: var(--text-muted);
  font-size: 11.5px;
}
.odeme-bilgi {
  font-size: 13px;
  color: var(--text-secondary);
}
.toplu-not {
  font-size: 12px;
  color: var(--text-muted);
  line-height: 1.5;
}
.onizleme-ozet {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
}
.toplu-bos {
  text-align: center;
  color: var(--text-muted);
  padding: 16px;
}
</style>
