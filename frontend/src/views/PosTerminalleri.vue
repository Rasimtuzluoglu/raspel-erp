<template>
  <div class="pos-sayfasi">
    <PageHeader
      :title="t('posTerminalleri.title')"
      icon="pi pi-credit-card"
    >
      <template #actions>
        <Button
          v-if="yonetebilir"
          :label="t('posTerminalleri.gunSonuRaporu')"
          icon="pi pi-file"
          class="p-button-outlined"
          @click="raporDialog = true"
        />
        <Button
          v-if="yonetebilir"
          :label="t('posTerminalleri.gunSonuIsle')"
          icon="pi pi-calendar"
          class="p-button-outlined"
          :loading="gunSonuYukleniyor"
          @click="gunSonuIsle"
        />
        <Button
          v-if="yonetebilir"
          :label="t('posTerminalleri.yeniPos')"
          icon="pi pi-plus"
          @click="dialogAc(null)"
        />
      </template>
    </PageHeader>

    <!-- KPI ozeti -->
    <div class="kpi-grid">
      <KpiKart
        :baslik="t('posTerminalleri.kpiToplamPos')"
        :deger="kpi.toplamPos"
        ikon="pi pi-credit-card"
        :para-birimi="false"
        renk="#3b82f6"
      />
      <KpiKart
        :baslik="t('posTerminalleri.kpiAktifPos')"
        :deger="kpi.aktifPos"
        ikon="pi pi-check-circle"
        :para-birimi="false"
        renk="#10b981"
      />
      <KpiKart
        :baslik="t('posTerminalleri.kpiBugunCiro')"
        :deger="kpi.bugunCiro"
        ikon="pi pi-money-bill"
        renk="#8b5cf6"
      />
      <KpiKart
        :baslik="t('posTerminalleri.kpiBugunKomisyon')"
        :deger="kpi.bugunKomisyon"
        ikon="pi pi-percentage"
        renk="#f59e0b"
      />
    </div>

    <!-- POS Özeti -->
    <div class="pos-ozet-grid">
      <div
        v-for="o in ozetler"
        :key="o.posId"
        class="pos-ozet-kart poz-clickable"
        :class="{ vurgulu: vurguluPosId === o.posId }"
        @click="ozeteTikla(o)"
      >
        <div class="ozet-ust">
          <i class="pi pi-credit-card" />
          <div>
            <strong>{{ o.posAd }}</strong>
            <small v-if="o.bankaAd">{{ o.bankaAd }}</small>
          </div>
        </div>
        <div class="ozet-degerler">
          <span>{{ t('posTerminalleri.bugun') }} <strong>{{ formatPara(o.bugunTutar) }}</strong></span>
          <span>{{ t('posTerminalleri.toplam') }} <strong>{{ formatPara(o.toplamTutar) }}</strong></span>
        </div>
        <div class="ozet-komisyon">
          {{ t('posTerminalleri.komisyonBugun') }} {{ formatPara(o.bugunKomisyon) }} · {{ t('posTerminalleri.toplam') }} {{ formatPara(o.toplamKomisyon) }}
        </div>
      </div>
      <EmptyState
        v-if="!ozetler.length && !yukleniyor"
        icon="pi pi-chart-bar"
        :message="t('posTerminalleri.ozetYok')"
        class="full-width"
      />
    </div>

    <!-- POS Listesi -->
    <div class="pos-listesi">
      <EmptyState
        v-if="!terminaller.length && !yukleniyor"
        icon="pi pi-credit-card"
        :message="t('posTerminalleri.empty')"
        :action-label="yonetebilir ? t('posTerminalleri.yeniPos') : ''"
        action-icon="pi pi-plus"
        class="full-width"
        @action="dialogAc(null)"
      />
      <div
        v-for="p in terminaller"
        :key="p.id"
        :ref="(el) => (kartRefler[p.id] = el)"
        class="pos-kart"
        :class="{ vurgulu: vurguluPosId === p.id }"
      >
        <div class="pos-kart-bilgi">
          <strong>{{ p.ad }}</strong>
          <span
            v-if="p.bankaAd"
            class="pos-banka"
          >{{ p.bankaAd }}</span>
          <span class="pos-komisyon">{{ t('posTerminalleri.komisyon') }} %{{ p.komisyonOrani ?? 0 }}</span>
        </div>
        <Tag
          :value="p.aktif ? t('posTerminalleri.aktif') : t('posTerminalleri.pasif')"
          :severity="p.aktif ? 'success' : 'danger'"
        />
        <SatirEylemleri
          :gorunur="{ duzenle: yonetebilir, sil: silebilir }"
          :baslik="p.ad"
          :items="posMenuItems(p)"
          @duzenle="dialogAc(p)"
          @sil="sil(p)"
        />
      </div>
    </div>

    <Dialog
      v-model:visible="dialog"
      :header="duzenlenenId ? t('posTerminalleri.posDuzenle') : t('posTerminalleri.yeniPosTerminali')"
      :modal="true"
      :style="{ width: '440px' }"
      :breakpoints="{ '575px': '92vw' }"
    >
      <div class="pos-form">
        <div class="field">
          <label>{{ t('posTerminalleri.posAdiZorunlu') }}</label>
          <InputText
            v-model="form.ad"
            :placeholder="t('posTerminalleri.posAdiPlaceholder')"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('posTerminalleri.banka') }}</label>
          <Dropdown
            v-model="form.bankaId"
            :options="bankalar"
            option-label="ad"
            option-value="id"
            filter
            :placeholder="t('posTerminalleri.bankaSecin')"
            class="w-full"
            :invalid="!form.bankaId"
          />
          <small
            v-if="!bankalar.length"
            class="alan-uyari"
          >{{ t('posTerminalleri.onceBankaEkle') }}</small>
        </div>
        <div class="field">
          <label>{{ t('posTerminalleri.komisyonOrani') }}</label>
          <InputNumber
            v-model="form.komisyonOrani"
            :min="0"
            :max="100"
            :min-fraction-digits="2"
            :max-fraction-digits="2"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('posTerminalleri.aktif') }}</label>
          <InputSwitch v-model="form.aktif" />
        </div>
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          class="p-button-text"
          @click="dialog = false"
        />
        <Button
          :label="t('common.save')"
          icon="pi pi-check"
          :loading="kaydediliyor"
          :disabled="!form.ad?.trim() || !form.bankaId"
          @click="kaydet"
        />
      </template>
    </Dialog>

    <Dialog
      v-model:visible="musteriDialog"
      :header="t('posTerminalleri.musteriDetayi') + ' — ' + (seciliPos?.ad || '')"
      :modal="true"
      :style="{ width: '480px' }"
      :breakpoints="{ '575px': '92vw' }"
    >
      <div class="musteri-arac">
        <span class="arama-kutu">
          <i class="pi pi-search" />
          <InputText
            v-model="musteriArama"
            :placeholder="t('posTerminalleri.musteriAra')"
            class="arama-input"
          />
        </span>
        <span class="musteri-toplam">
          {{ t('posTerminalleri.toplam') }} <strong>{{ formatPara(musteriToplam) }}</strong>
        </span>
      </div>
      <EmptyState
        v-if="!filtreliMusteriler.length"
        icon="pi pi-users"
        :message="t('posTerminalleri.cekimYok')"
      />
      <div
        v-for="m in filtreliMusteriler"
        :key="m.cariId"
        class="musteri-satir"
      >
        <span>{{ m.cariAd }}</span>
        <strong>{{ formatPara(m.toplamTutar) }}</strong>
      </div>
    </Dialog>

    <PosGunSonuRaporDialog
      v-model:visible="raporDialog"
      :terminaller="terminaller"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { unwrapList } from '../api/utils/unwrap.js'
import { posAPI, bankaAPI } from '../api/index.js'
import { useAuthStore } from '../stores/authStore.js'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useConfirm } from 'primevue/useconfirm'
import KpiKart from '../components/KpiKart.vue'
import EmptyState from '../components/EmptyState.vue'
import PosGunSonuRaporDialog from '../components/PosGunSonuRaporDialog.vue'
import { formatPara } from '../utils/format.js'
import { useI18n } from 'vue-i18n'

const toastBildirim = useToastBildirim()
const confirm = useConfirm()
const { t } = useI18n()
const authStore = useAuthStore()

const terminaller = ref([])
const ozetler = ref([])
const bankalar = ref([])
const dialog = ref(false)
const duzenlenenId = ref(null)
const musteriDialog = ref(false)
const seciliPos = ref(null)
const musteriler = ref([])
const gunSonuYukleniyor = ref(false)
const kaydediliyor = ref(false)
const yukleniyor = ref(false)
const raporDialog = ref(false)
const musteriArama = ref('')

// Yetki kapilari (backend ile hizali).
const yonetebilir = computed(() => authStore.isAdmin || authStore.kullanici?.role === 'MUHASEBE')
const silebilir = computed(() => authStore.isAdmin)

// KPI: /ozet verisinden client-side toplanir.
const kpi = computed(() => ({
  toplamPos: terminaller.value.length,
  aktifPos: terminaller.value.filter((p) => p.aktif).length,
  bugunCiro: ozetler.value.reduce((a, o) => a + (Number(o.bugunTutar) || 0), 0),
  bugunKomisyon: ozetler.value.reduce((a, o) => a + (Number(o.bugunKomisyon) || 0), 0)
}))

const form = ref({ ad: '', bankaId: null, komisyonOrani: 0, aktif: true })

// Müşteri detayı: arama + ciroya göre sıralama + toplam.
const filtreliMusteriler = computed(() => {
  const q = musteriArama.value.trim().toLowerCase()
  const list = q ? musteriler.value.filter((m) => (m.cariAd || '').toLowerCase().includes(q)) : musteriler.value.slice()
  return list.sort((a, b) => (Number(b.toplamTutar) || 0) - (Number(a.toplamTutar) || 0))
})
const musteriToplam = computed(() => musteriler.value.reduce((a, m) => a + (Number(m.toplamTutar) || 0), 0))

const yukle = async () => {
  yukleniyor.value = true
  try {
    const [t, o] = await Promise.all([posAPI.liste(), posAPI.ozet()])
    terminaller.value = t.data || []
    ozetler.value = o.data || []
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('posTerminalleri.hataYukleme'))
  }
  yukleniyor.value = false
}

const bankalariYukle = async () => {
  try {
    const r = await bankaAPI.getAll()
    bankalar.value = unwrapList(r)
  } catch {
    bankalar.value = []
  }
}

const dialogAc = (p) => {
  duzenlenenId.value = p?.id || null
  form.value = p
    ? { ad: p.ad, bankaId: p.bankaId, komisyonOrani: p.komisyonOrani ?? 0, aktif: p.aktif !== false }
    : { ad: '', bankaId: null, komisyonOrani: 0, aktif: true }
  dialog.value = true
}

// Kart ⋮ menusu: musteri detayi; duzenle/sil yerlesik gelir.
const posMenuItems = (p) => [
  { etiket: t('posTerminalleri.musteriDetayi'), ikon: 'pi pi-users', islem: () => musteriDetayAc(p) }
]

// Ozet kartindan ilgili POS kartina odaklan (vurgula + kaydir).
const kartRefler = {}
const vurguluPosId = ref(null)
let vurguTimer = null
const ozeteTikla = (o) => {
  const el = kartRefler[o.posId]
  if (el?.scrollIntoView) el.scrollIntoView({ behavior: 'smooth', block: 'center' })
  vurguluPosId.value = o.posId
  clearTimeout(vurguTimer)
  vurguTimer = setTimeout(() => (vurguluPosId.value = null), 1600)
}

const kaydet = async () => {
  if (!form.value.ad?.trim()) {
    toastBildirim.uyari(t('posTerminalleri.posAdiZorunluUyari'))
    return
  }
  if (!form.value.bankaId) {
    toastBildirim.uyari(t('posTerminalleri.bankaZorunluUyari'))
    return
  }
  kaydediliyor.value = true
  try {
    if (duzenlenenId.value) {
      await posAPI.guncelle(duzenlenenId.value, form.value)
      toastBildirim.basarili(t('posTerminalleri.guncellendi'))
    } else {
      await posAPI.olustur(form.value)
      toastBildirim.basarili(t('posTerminalleri.olusturuldu'))
    }
    dialog.value = false
    yukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('posTerminalleri.kaydedilemedi'))
  } finally {
    kaydediliyor.value = false
  }
}

const sil = (p) => {
  confirm.require({
    message: t('posTerminalleri.silmeOnayi', { ad: p.ad || '' }),
    header: t('common.silmeOnayi'),
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: t('common.evetSil'),
    rejectLabel: t('common.vazgec'),
    accept: async () => {
      try {
        await posAPI.sil(p.id)
        toastBildirim.basarili(t('posTerminalleri.silindi'))
        yukle()
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || t('posTerminalleri.silinemedi'))
      }
    },
    reject: () => {}
  })
}

const musteriDetayAc = async (p) => {
  seciliPos.value = p
  musteriDialog.value = true
  musteriArama.value = ''
  musteriler.value = []
  try {
    const r = await posAPI.musteriler(p.id)
    musteriler.value = r.data || []
  } catch {
    musteriler.value = []
  }
}

const gunSonuIsle = async () => {
  gunSonuYukleniyor.value = true
  try {
    const r = await posAPI.gunSonu()
    const islenen = r.data || []
    if (islenen.length) {
      toastBildirim.basarili(t('posTerminalleri.gunSonuAktarildi', { n: islenen.length }))
    } else {
      toastBildirim.bilgi(t('posTerminalleri.gunSonuTutarYok'))
    }
    yukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('posTerminalleri.gunSonuBasarisiz'))
  } finally {
    gunSonuYukleniyor.value = false
  }
}

onMounted(() => {
  yukle()
  bankalariYukle()
})
</script>

<style scoped>
.pos-sayfasi {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(170px, 100%), 1fr));
  gap: 12px;
}
.full-width {
  grid-column: 1 / -1;
}
.pos-ozet-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(min(260px, 100%), 1fr));
  gap: 12px;
}
.pos-ozet-kart {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 14px 16px;
}
.ozet-ust {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}
.ozet-ust i {
  font-size: 20px;
  color: var(--accent, var(--accent));
}
.ozet-ust strong {
  display: block;
  font-size: 14px;
}
.ozet-ust small {
  color: var(--text-muted);
}
.ozet-degerler {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 13px;
}
.ozet-degerler strong {
  color: #10b981;
}
.ozet-komisyon {
  margin-top: 8px;
  font-size: 12px;
  color: var(--text-muted);
  border-top: 1px solid var(--border);
  padding-top: 8px;
}
.pos-listesi {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.pos-kart {
  display: flex;
  align-items: center;
  gap: 12px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 10px 14px;
}
.pos-kart-bilgi {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.pos-banka,
.pos-komisyon {
  font-size: 12px;
  color: var(--text-muted);
}
.pos-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.pos-form .field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.pos-form .field label {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
}
.pos-form .w-full {
  width: 100%;
}
.musteri-satir {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid var(--border);
  font-size: 13px;
}
.musteri-satir:last-child {
  border-bottom: none;
}
.poz-clickable {
  cursor: pointer;
}
.pos-ozet-kart.vurgulu,
.pos-kart.vurgulu {
  border-color: var(--accent);
  box-shadow: 0 0 0 2px var(--accent-soft-strong, rgba(20, 184, 166, 0.35));
}
.alan-uyari {
  display: block;
  margin-top: 6px;
  color: var(--danger, #ef4444);
  font-size: 0.75rem;
}
.musteri-arac {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
  flex-wrap: wrap;
}
.arama-kutu {
  position: relative;
  display: inline-flex;
  align-items: center;
  flex: 1 1 200px;
  min-width: 0;
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
.musteri-toplam {
  font-size: 0.82rem;
  color: var(--text-secondary);
  white-space: nowrap;
}

/* Mobil: POS kart bilgisi tam genişlik, dikey. */
@media (max-width: 640px) {
  .pos-kart {
    flex-wrap: wrap;
  }
  .pos-kart-bilgi {
    flex: 1 1 100%;
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }
}
</style>
