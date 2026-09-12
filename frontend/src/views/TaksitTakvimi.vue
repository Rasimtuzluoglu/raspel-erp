<template>
  <div class="taksit-takvimi">
    <PageHeader
      :title="t('taksitTakvimi.baslik')"
      icon="pi pi-calendar"
      :subtitle="t('taksitTakvimi.altBaslik')"
    >
      <template #actions>
        <Button
          :label="t('taksitTakvimi.yeniPlan')"
          icon="pi pi-plus"
          @click="planDialogAc"
        />
      </template>
    </PageHeader>

    <div class="ozet-kartlar">
      <div class="ozet-kart">
        <span class="ozet-baslik">{{ t('taksitTakvimi.bekleyen') }}</span>
        <strong class="ozet-deger">{{ formatCurrency(ozet.bekleyenToplam) }}</strong>
        <small class="ozet-alt">{{ ozet.bekleyenAdet || 0 }} {{ t('taksitTakvimi.adet') }}</small>
      </div>
      <div class="ozet-kart gecikmis">
        <span class="ozet-baslik">{{ t('taksitTakvimi.gecikmis') }}</span>
        <strong class="ozet-deger">{{ formatCurrency(ozet.gecikmisToplam) }}</strong>
        <small class="ozet-alt">{{ ozet.gecikmisAdet || 0 }} {{ t('taksitTakvimi.adet') }}</small>
      </div>
      <div class="ozet-kart bu-ay">
        <span class="ozet-baslik">{{ t('taksitTakvimi.buAy') }}</span>
        <strong class="ozet-deger">{{ formatCurrency(ozet.buAyToplam) }}</strong>
      </div>
    </div>

    <div class="takvim-kart">
      <div class="takvim-baslik">
        <Button
          icon="pi pi-chevron-left"
          text
          rounded
          :aria-label="t('taksitTakvimi.oncekiAy')"
          @click="ayDegistir(-1)"
        />
        <strong class="takvim-ay">{{ ayAdi }}</strong>
        <Button
          icon="pi pi-chevron-right"
          text
          rounded
          :aria-label="t('taksitTakvimi.sonrakiAy')"
          @click="ayDegistir(1)"
        />
      </div>
      <div class="takvim-grid">
        <div
          v-for="g in gunBasliklari"
          :key="g"
          class="gun-baslik"
        >
          {{ g }}
        </div>
        <div
          v-for="hucre in takvimHucreleri"
          :key="hucre.key"
          class="gun-hucre"
          :class="{ bugun: hucre.bugun, bos: !hucre.gun }"
        >
          <template v-if="hucre.gun">
            <span class="gun-no">{{ hucre.gun }}</span>
            <button
              v-for="k in hucre.kalemler"
              :key="k.id"
              type="button"
              class="kalem"
              :class="kalemSinif(k)"
              :title="`${k.cariAd} - ${formatCurrency(k.tutar)}`"
              @click="kalemDetay(k)"
            >
              {{ k.cariAd }} · {{ formatCurrency(k.tutar) }}
            </button>
          </template>
        </div>
      </div>
    </div>

    <div class="liste-kart">
      <h3 class="liste-baslik">
        {{ t('taksitTakvimi.yaklasan') }}
      </h3>
      <DataTable
        :value="yaklasan"
        data-key="id"
        :loading="yukleniyor"
        responsive-layout="scroll"
        paginator
        :rows="10"
        :empty-message="t('taksitTakvimi.kayitYok')"
      >
        <Column
          field="vadeTarihi"
          :header="t('taksitTakvimi.vade')"
          sortable
        >
          <template #body="{ data }">
            {{ formatDate(data.vadeTarihi) }}
          </template>
        </Column>
        <Column
          field="cariAd"
          :header="t('taksitTakvimi.cari')"
          sortable
        />
        <Column
          field="kurum"
          :header="t('taksitTakvimi.kurum')"
        >
          <template #body="{ data }">
            {{ data.kurum || '-' }}
          </template>
        </Column>
        <Column
          :header="t('taksitTakvimi.taksitNo')"
        >
          <template #body="{ data }">
            {{ data.taksitNo }}/{{ data.taksitSayisi }}
          </template>
        </Column>
        <Column
          field="tutar"
          :header="t('taksitTakvimi.tutar')"
        >
          <template #body="{ data }">
            {{ formatCurrency(data.tutar) }}
          </template>
        </Column>
        <Column :header="t('taksitTakvimi.durum')">
          <template #body="{ data }">
            <Tag
              :severity="durumSeverity(data)"
              :value="durumEtiket(data)"
            />
          </template>
        </Column>
        <Column :header="t('common.actions')">
          <template #body="{ data }">
            <Button
              v-if="data.odemeDurumu !== 'ODENDI'"
              icon="pi pi-check"
              text
              rounded
              :aria-label="t('taksitTakvimi.ode')"
              @click="ode(data)"
            />
            <Button
              icon="pi pi-trash"
              text
              rounded
              severity="danger"
              :aria-label="t('taksitTakvimi.sil')"
              @click="sil(data)"
            />
          </template>
        </Column>
      </DataTable>
    </div>

    <Dialog
      v-model:visible="planDialog"
      :header="t('taksitTakvimi.planBaslik')"
      modal
      :style="{ width: '480px' }"
    >
      <div class="plan-form">
        <div class="form-satir">
          <label>{{ t('taksitTakvimi.cariSec') }}</label>
          <Select
            v-model="form.cariId"
            :options="cariler"
            option-label="ad"
            option-value="id"
            filter
            :placeholder="t('taksitTakvimi.cariSec')"
            class="w-full"
          />
        </div>
        <div class="form-satir">
          <label>{{ t('taksitTakvimi.toplamTutar') }}</label>
          <InputNumber
            v-model="form.toplamTutar"
            mode="currency"
            currency="TRY"
            locale="tr-TR"
            class="w-full"
          />
        </div>
        <div class="form-ikili">
          <div class="form-satir">
            <label>{{ t('taksitTakvimi.taksitSayisi') }}</label>
            <InputNumber
              v-model="form.taksitSayisi"
              :min="1"
              :max="60"
              show-buttons
              class="w-full"
            />
          </div>
          <div class="form-satir">
            <label>{{ t('taksitTakvimi.periyot') }}</label>
            <InputNumber
              v-model="form.periyotAy"
              :min="1"
              :max="12"
              show-buttons
              class="w-full"
            />
          </div>
        </div>
        <div class="form-satir">
          <label>{{ t('taksitTakvimi.baslangic') }}</label>
          <DatePicker
            v-model="form.baslangicTarihi"
            date-format="dd.mm.yy"
            show-icon
            class="w-full"
          />
        </div>
        <div class="form-satir">
          <label>{{ t('taksitTakvimi.kurum') }}</label>
          <InputText
            v-model="form.kurum"
            class="w-full"
          />
        </div>
        <div class="form-satir">
          <label>{{ t('taksitTakvimi.aciklama') }}</label>
          <Textarea
            v-model="form.aciklama"
            rows="2"
            class="w-full"
          />
        </div>
      </div>
      <template #footer>
        <Button
          :label="t('taksitTakvimi.iptal')"
          severity="secondary"
          text
          @click="planDialog = false"
        />
        <Button
          :label="t('taksitTakvimi.kaydet')"
          icon="pi pi-check"
          :loading="kaydediliyor"
          @click="planKaydet"
        />
      </template>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useConfirm } from 'primevue/useconfirm'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { taksitAPI, cariHesapAPI } from '../api/index.js'
import { formatCurrency, formatTarih as formatDate } from '../utils/format.js'

const { t, locale } = useI18n()
const confirm = useConfirm()
const toastBildirim = useToastBildirim()

const bugun = new Date()
const seciliYil = ref(bugun.getFullYear())
const seciliAy = ref(bugun.getMonth() + 1)

const kalemler = ref([])
const yaklasan = ref([])
const ozet = ref({})
const cariler = ref([])
const yukleniyor = ref(false)
const kaydediliyor = ref(false)
const planDialog = ref(false)

const form = ref({
  cariId: null,
  toplamTutar: null,
  taksitSayisi: 3,
  periyotAy: 1,
  baslangicTarihi: new Date(),
  kurum: '',
  aciklama: ''
})

const gunBasliklari = computed(() => {
  const isimler = [
    t('taksitTakvimi.pazartesi'), t('taksitTakvimi.sali'), t('taksitTakvimi.carsamba'),
    t('taksitTakvimi.persembe'), t('taksitTakvimi.cuma'), t('taksitTakvimi.cumartesi'), t('taksitTakvimi.pazar')
  ]
  return isimler
})

const ayAdi = computed(() => {
  const d = new Date(seciliYil.value, seciliAy.value - 1, 1)
  return new Intl.DateTimeFormat(locale.value === 'en' ? 'en-US' : 'tr-TR', {
    month: 'long', year: 'numeric'
  }).format(d)
})

const takvimHucreleri = computed(() => {
  const ilk = new Date(seciliYil.value, seciliAy.value - 1, 1)
  const gunSayisi = new Date(seciliYil.value, seciliAy.value, 0).getDate()
  let offset = ilk.getDay() - 1
  if (offset < 0) offset = 6

  const hucreler = []
  for (let i = 0; i < offset; i++) {
    hucreler.push({ key: `bos-${i}`, gun: null, kalemler: [] })
  }
  const simdi = new Date()
  for (let g = 1; g <= gunSayisi; g++) {
    const gunKalemleri = kalemler.value.filter((k) => {
      const d = new Date(k.vadeTarihi)
      return d.getDate() === g
    })
    hucreler.push({
      key: `gun-${g}`,
      gun: g,
      bugun: simdi.getFullYear() === seciliYil.value
        && simdi.getMonth() === seciliAy.value - 1
        && simdi.getDate() === g,
      kalemler: gunKalemleri
    })
  }
  return hucreler
})

const kalemSinif = (k) => {
  if (k.odemeDurumu === 'ODENDI') return 'kalem-odendi'
  if (k.gecikmeGunu > 0) return 'kalem-gecikmis'
  return 'kalem-bekleyen'
}

const durumSeverity = (k) => {
  if (k.odemeDurumu === 'ODENDI') return 'success'
  if (k.gecikmeGunu > 0) return 'danger'
  return 'warning'
}

const durumEtiket = (k) => {
  if (k.odemeDurumu === 'ODENDI') return t('taksitTakvimi.odendi')
  if (k.gecikmeGunu > 0) return t('taksitTakvimi.gecikmis')
  return t('taksitTakvimi.bekliyor')
}

const yukle = async () => {
  yukleniyor.value = true
  try {
    const [takvimRes, ozetRes, yaklasanRes] = await Promise.all([
      taksitAPI.takvim(seciliYil.value, seciliAy.value),
      taksitAPI.ozet(),
      taksitAPI.yaklasan(30)
    ])
    kalemler.value = takvimRes.data || []
    ozet.value = ozetRes.data || {}
    yaklasan.value = yaklasanRes.data || []
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('taksitTakvimi.hata'))
  }
  yukleniyor.value = false
}

const ayDegistir = (delta) => {
  let ay = seciliAy.value + delta
  let yil = seciliYil.value
  if (ay < 1) { ay = 12; yil -= 1 }
  if (ay > 12) { ay = 1; yil += 1 }
  seciliAy.value = ay
  seciliYil.value = yil
  yukle()
}

const planDialogAc = async () => {
  form.value = {
    cariId: null,
    toplamTutar: null,
    taksitSayisi: 3,
    periyotAy: 1,
    baslangicTarihi: new Date(),
    kurum: '',
    aciklama: ''
  }
  if (!cariler.value.length) {
    try {
      const r = await cariHesapAPI.getAll({ size: 500 })
      cariler.value = r.data?.content || r.data || []
    } catch (err) {
      toastBildirim.hata(err?.response?.data?.message || t('taksitTakvimi.hata'))
    }
  }
  planDialog.value = true
}

const planKaydet = async () => {
  if (!form.value.cariId || !form.value.toplamTutar || form.value.toplamTutar <= 0) {
    toastBildirim.uyari(t('taksitTakvimi.formEksik'))
    return
  }
  kaydediliyor.value = true
  try {
    await taksitAPI.planOlustur({
      cariId: form.value.cariId,
      toplamTutar: form.value.toplamTutar,
      taksitSayisi: form.value.taksitSayisi,
      periyotAy: form.value.periyotAy,
      baslangicTarihi: formatYyyyMmDd(form.value.baslangicTarihi),
      kurum: form.value.kurum || null,
      aciklama: form.value.aciklama || null
    })
    planDialog.value = false
    toastBildirim.basarili(t('taksitTakvimi.planOlusturuldu'))
    await yukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('taksitTakvimi.hata'))
  }
  kaydediliyor.value = false
}

const formatYyyyMmDd = (d) => {
  if (!d) return null
  const t2 = d instanceof Date ? d : new Date(d)
  const ay = String(t2.getMonth() + 1).padStart(2, '0')
  const gun = String(t2.getDate()).padStart(2, '0')
  return `${t2.getFullYear()}-${ay}-${gun}`
}

const kalemDetay = (k) => {
  if (k.odemeDurumu !== 'ODENDI') {
    ode(k)
  }
}

const ode = (k) => {
  confirm.require({
    message: t('taksitTakvimi.odendiOnay'),
    header: t('taksitTakvimi.ode'),
    icon: 'pi pi-check-circle',
    acceptLabel: t('taksitTakvimi.evet'),
    rejectLabel: t('taksitTakvimi.iptal'),
    accept: async () => {
      try {
        await taksitAPI.ode(k.id)
        toastBildirim.basarili(t('taksitTakvimi.odendiIsaretlendi'))
        await yukle()
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || t('taksitTakvimi.hata'))
      }
    }
  })
}

const sil = (k) => {
  confirm.require({
    message: t('taksitTakvimi.silOnay'),
    header: t('taksitTakvimi.sil'),
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: t('taksitTakvimi.evet'),
    rejectLabel: t('taksitTakvimi.iptal'),
    accept: async () => {
      try {
        await taksitAPI.sil(k.id)
        toastBildirim.basarili(t('taksitTakvimi.silindi'))
        await yukle()
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || t('taksitTakvimi.hata'))
      }
    }
  })
}

onMounted(yukle)
</script>

<style scoped>
.taksit-takvimi {
  padding: 0;
}

.ozet-kartlar {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.ozet-kart {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 16px 20px;
  border-radius: 12px;
  background: var(--surface-card, #1e293b);
  border-left: 4px solid #3b82f6;
}

.ozet-kart.gecikmis {
  border-left-color: #ef4444;
}

.ozet-kart.bu-ay {
  border-left-color: #f59e0b;
}

.ozet-baslik {
  font-size: 13px;
  color: var(--text-secondary, #94a3b8);
}

.ozet-deger {
  font-size: 22px;
  font-weight: 600;
}

.ozet-alt {
  font-size: 12px;
  color: var(--text-secondary, #94a3b8);
}

.takvim-kart,
.liste-kart {
  padding: 16px 20px;
  border-radius: 12px;
  background: var(--surface-card, #1e293b);
  margin-bottom: 20px;
}

.takvim-baslik {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-bottom: 12px;
}

.takvim-ay {
  font-size: 16px;
  text-transform: capitalize;
}

.takvim-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 6px;
}

.gun-baslik {
  text-align: center;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary, #94a3b8);
  padding-bottom: 4px;
}

.gun-hucre {
  min-height: 84px;
  border: 1px solid var(--surface-border, #334155);
  border-radius: 8px;
  padding: 4px;
  display: flex;
  flex-direction: column;
  gap: 3px;
  overflow: hidden;
}

.gun-hucre.bos {
  opacity: 0.35;
}

.gun-hucre.bugun {
  border-color: #3b82f6;
  box-shadow: 0 0 0 1px #3b82f6 inset;
}

.gun-no {
  font-size: 12px;
  font-weight: 600;
}

.kalem {
  border: none;
  border-radius: 6px;
  padding: 2px 5px;
  font-size: 11px;
  text-align: left;
  cursor: pointer;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  color: #fff;
}

.kalem-bekleyen {
  background: #f59e0b;
}

.kalem-gecikmis {
  background: #ef4444;
}

.kalem-odendi {
  background: #10b981;
}

.liste-baslik {
  font-size: 16px;
  margin: 0 0 12px 0;
}

.form-satir {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 14px;
  flex: 1;
}

.form-satir label {
  font-size: 13px;
  color: var(--text-secondary, #94a3b8);
}

.form-ikili {
  display: flex;
  gap: 12px;
}

.w-full {
  width: 100%;
}
</style>
