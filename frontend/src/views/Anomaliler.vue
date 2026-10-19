<template>
  <div class="anomaliler-page">
    <PageHeader
      :title="t('anomaliler.title')"
      :subtitle="t('anomaliler.subtitle')"
    >
      <template #actions>
        <Button
          v-if="aktifSekme === 'anomali'"
          :label="t('anomaliler.yenidenTara')"
          icon="pi pi-refresh"
          class="p-button-primary"
          :loading="yukleniyor"
          @click="anomalileriYukle"
        />
        <Button
          v-else
          :label="t('anomaliler.yeniIpEkle')"
          icon="pi pi-plus"
          class="p-button-success"
          @click="ipDialogAc()"
        />
      </template>
    </PageHeader>

    <div class="kpi-satiri">
      <KpiKart
        :baslik="t('anomaliler.kpiToplam')"
        :deger="anomaliler.length"
        ikon="pi pi-shield"
        renk="#3b82f6"
        :para-birimi="false"
      />
      <KpiKart
        :baslik="t('anomaliler.kpiKritik')"
        :deger="kritikYuksekSayisi"
        ikon="pi pi-exclamation-triangle"
        renk="#ef4444"
        :para-birimi="false"
      />
      <KpiKart
        :baslik="t('anomaliler.kpiOrta')"
        :deger="ortaSayisi"
        ikon="pi pi-exclamation-circle"
        renk="#f59e0b"
        :para-birimi="false"
      />
      <KpiKart
        :baslik="t('anomaliler.kpiDusuk')"
        :deger="dusukSayisi"
        ikon="pi pi-info-circle"
        renk="#64748b"
        :para-birimi="false"
      />
      <KpiKart
        :baslik="t('anomaliler.kpiIp')"
        :deger="ipListesi.length"
        ikon="pi pi-lock"
        renk="#8b5cf6"
        :para-birimi="false"
      />
    </div>

    <TabView v-model:active-index="aktifIndex">
      <TabPanel>
        <template #header>
          <span class="flex items-center gap-1.5">
            <i class="pi pi-shield" />
            {{ t('anomaliler.anomaliRisk') }}
          </span>
        </template>

        <div class="anomali-arac">
          <IconField class="filtre-arama">
            <InputIcon class="pi pi-search" />
            <InputText
              v-model="filtreQ"
              :placeholder="t('anomaliler.aramaPlaceholder')"
            />
          </IconField>
          <Dropdown
            v-model="filtreTur"
            :options="turSecenekleri"
            option-label="ad"
            option-value="deger"
            show-clear
            class="filtre-select"
            :placeholder="t('anomaliler.turFiltre')"
          />
          <span class="arac-bosluk" />
          <span
            v-if="sonTarama"
            class="tarama-zamani"
          ><i class="pi pi-clock" /> {{ t('anomaliler.sonTarama') }}: {{ formatTarih(sonTarama) }}</span>
          <Button
            :label="t('anomaliler.excel')"
            icon="pi pi-file-excel"
            outlined
            size="small"
            :loading="excelYukleniyor"
            @click="excelIndir"
          />
        </div>

        <div
          class="anomali-cipler"
          role="group"
          :aria-label="t('common.status')"
        >
          <button
            v-for="c in seviyeCipleri"
            :key="c.deger"
            type="button"
            class="anomali-cip"
            :class="{ aktif: filtreSeviye === c.deger }"
            :aria-pressed="filtreSeviye === c.deger"
            @click="filtreSeviye = c.deger"
          >
            <i :class="c.ikon" /> {{ c.ad }}
          </button>
          <label class="yok-say-toggle">
            <Checkbox
              v-model="yokSayilanlariGoster"
              binary
            />
            <span>{{ t('anomaliler.yokSayilanlariGoster') }}</span>
          </label>
        </div>

        <div
          v-if="yukleniyor"
          class="p-4"
          role="status"
          :aria-label="t('common.loading')"
        >
          <SkeletonLoader :count="3" />
        </div>

        <EmptyState
          v-else-if="anomaliler.length === 0"
          icon="pi pi-check-circle"
          :message="t('anomaliler.bosBaslik')"
          :sub-message="t('anomaliler.bosAciklama')"
        />

        <EmptyState
          v-else-if="filtreliAnomaliler.length === 0"
          icon="pi pi-filter-slash"
          :message="t('anomaliler.filtreBos')"
          :action-label="t('anomaliler.temizle')"
          action-icon="pi pi-filter-slash"
          @action="filtreTemizle"
        />

        <div
          v-else
          class="anomali-grid"
        >
          <div
            v-for="item in filtreliAnomaliler"
            :key="item.id"
            class="anomali-card"
            :class="(item.seviye || '').toLowerCase()"
          >
            <div class="card-header">
              <div class="header-left">
                <span
                  class="badge"
                  :class="(item.seviye || '').toLowerCase()"
                >{{ seviyeLabel(item.seviye) }} {{ t('anomaliler.oncelik') }}</span>
                <span class="tur-label">{{ turLabel(item.tur) }}</span>
                <Tag
                  v-if="item.durum === 'INCELENDI'"
                  :value="t('anomaliler.incelendi')"
                  severity="info"
                  icon="pi pi-eye"
                />
                <Tag
                  v-if="item.durum === 'YOK_SAYILDI'"
                  :value="t('anomaliler.yokSayildi')"
                  severity="secondary"
                  icon="pi pi-eye-slash"
                />
              </div>
              <div class="header-right">
                <span class="tarih">{{ formatTarih(item.tespitTarihi) }}</span>
                <SatirEylemleri
                  :gorunur="{ duzenle: false, cogalt: false, sil: false }"
                  :items="anomaliAksiyonlari(item)"
                />
              </div>
            </div>
            <h4 class="card-title">
              <i class="pi pi-exclamation-triangle" /> {{ item.baslik }}
            </h4>
            <p class="card-desc">
              {{ item.aciklama }}
            </p>
            <div class="oneri-box">
              <strong><i class="pi pi-lightbulb" /> {{ t('anomaliler.oneri') }}:</strong> {{ item.oneri }}
            </div>
          </div>
        </div>
      </TabPanel>

      <TabPanel v-if="isAdmin">
        <template #header>
          <span class="flex items-center gap-1.5">
            <i class="pi pi-lock" />
            {{ t('anomaliler.ipBeyazListesi') }}
          </span>
        </template>

        <div class="ip-info-box">
          <i class="pi pi-shield" />
          <div>
            <strong>{{ t('anomaliler.ipPolitika') }}</strong>
            <p>{{ t('anomaliler.ipPolitikaAciklama') }}</p>
          </div>
        </div>

        <div class="ip-arac">
          <span class="arac-bosluk" />
          <Button
            :label="t('anomaliler.excel')"
            icon="pi pi-file-excel"
            outlined
            size="small"
            :loading="ipExcelYukleniyor"
            @click="ipExcelIndir"
          />
        </div>

        <DataTable
          :value="ipListesi"
          class="ip-masa"
          striped-rows
          :loading="ipYukleniyor"
        >
          <template #empty>
            <EmptyState
              :message="t('anomaliler.ipYok')"
              icon="pi pi-lock"
            />
          </template>
          <Column
            field="ipAdresi"
            :header="t('anomaliler.izinVerilenIp')"
            style="width: 220px"
          >
            <template #body="{ data }">
              <code>{{ data.ipAdresi }}</code>
            </template>
          </Column>
          <Column
            field="aciklama"
            :header="t('anomaliler.aciklamaLokasyon')"
          />
          <Column
            field="eklemeTarihi"
            :header="t('anomaliler.tanimlamaTarihi')"
            style="width: 160px"
          />
          <Column
            field="durum"
            :header="t('common.status')"
            style="width: 120px"
          >
            <template #body="{ data }">
              <Tag
                :value="data.durum === 'PASIF' ? t('anomaliler.ipPasif') : t('anomaliler.ipAktif')"
                :severity="data.durum === 'PASIF' ? 'secondary' : 'success'"
              />
            </template>
          </Column>
          <Column
            :header="t('anomaliler.islem')"
            style="width: 80px"
          >
            <template #body="{ data }">
              <SatirEylemleri
                :gorunur="{ duzenle: false, cogalt: false, sil: false }"
                :items="ipAksiyonlari(data)"
              />
            </template>
          </Column>
        </DataTable>

        <!-- Mobil IP kartları -->
        <div class="ip-mobil">
          <div
            v-if="ipYukleniyor && !ipListesi.length"
            class="ip-skeleton"
            role="status"
            :aria-label="t('common.loading')"
          >
            <SkeletonLoader
              :count="3"
              height="64px"
            />
          </div>
          <EmptyState
            v-else-if="!ipListesi.length"
            :message="t('anomaliler.ipYok')"
            icon="pi pi-lock"
          />
          <div
            v-for="k in ipListesi"
            :key="k.id"
            class="ip-mobil-kart"
          >
            <div class="ip-mk-ust">
              <code>{{ k.ipAdresi }}</code>
              <SatirEylemleri
                :gorunur="{ duzenle: false, cogalt: false, sil: false }"
                :items="ipAksiyonlari(k)"
              />
            </div>
            <div class="ip-mk-satir">
              <span>{{ k.aciklama || '-' }}</span>
              <Tag
                :value="k.durum === 'PASIF' ? t('anomaliler.ipPasif') : t('anomaliler.ipAktif')"
                :severity="k.durum === 'PASIF' ? 'secondary' : 'success'"
              />
            </div>
            <span class="ip-mk-tarih">{{ k.eklemeTarihi || '-' }}</span>
          </div>
        </div>

        <!-- IP Ekleme / Düzenleme Modalı -->
        <Dialog
          v-model:visible="ipModalAcik"
          :header="ipDuzenleme ? t('anomaliler.ipDuzenle') : t('anomaliler.yeniIpBaslik')"
          :modal="true"
          :style="{ width: '440px' }"
          :breakpoints="{ '575px': '92vw' }"
        >
          <div class="p-fluid">
            <div class="field mb-3">
              <label for="ipAdresi">{{ t('anomaliler.ipAdresi') }}</label>
              <InputText
                id="ipAdresi"
                v-model="yeniIp.ipAdresi"
                :placeholder="t('anomaliler.ipAdresiPlaceholder')"
              />
            </div>
            <div class="field mb-3">
              <label for="ipAciklama">{{ t('anomaliler.aciklamaOfis') }}</label>
              <InputText
                id="ipAciklama"
                v-model="yeniIp.aciklama"
                :placeholder="t('anomaliler.ipAciklamaPlaceholder')"
              />
            </div>
            <div class="field mb-3">
              <label>{{ t('common.status') }}</label>
              <div class="switch-satir">
                <InputSwitch v-model="ipAktifMi" />
                <span>{{ ipAktifMi ? t('anomaliler.ipAktif') : t('anomaliler.ipPasif') }}</span>
              </div>
            </div>
          </div>
          <template #footer>
            <Button
              :label="t('common.cancel')"
              icon="pi pi-times"
              class="p-button-text"
              @click="ipModalAcik = false"
            />
            <Button
              :label="t('common.save')"
              icon="pi pi-check"
              class="p-button-primary"
              :loading="ipKaydediliyor"
              @click="ipKaydet"
            />
          </template>
        </Dialog>
      </TabPanel>
    </TabView>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useConfirm } from 'primevue/useconfirm'
import { anomaliAPI, excelAPI } from '../api/index.js'
import { useAuthStore } from '../stores/authStore.js'
import PageHeader from '../components/PageHeader.vue'
import SkeletonLoader from '../components/SkeletonLoader.vue'
import KpiKart from '../components/KpiKart.vue'
import EmptyState from '../components/EmptyState.vue'
import SatirEylemleri from '../components/SatirEylemleri.vue'
import { useI18n } from 'vue-i18n'
import { formatTarihSaat as formatTarih } from '../utils/format.js'

const { t } = useI18n()
const toastBildirim = useToastBildirim()
const confirm = useConfirm()
const router = useRouter()
const authStore = useAuthStore()
const isAdmin = computed(() => authStore?.kullanici?.role === 'ADMIN')

const AKTIF_SEKME_ANAHTAR = 'raspel_anomaliler_tab'
const aktifIndex = ref(Number(localStorage.getItem(AKTIF_SEKME_ANAHTAR)) === 1 ? 1 : 0)
watch(aktifIndex, (v) => localStorage.setItem(AKTIF_SEKME_ANAHTAR, String(v)))
const aktifSekme = computed(() => (aktifIndex.value === 0 ? 'anomali' : 'ip'))
if (!isAdmin.value) aktifIndex.value = 0

const yukleniyor = ref(false)
const anomaliler = ref([])

const ipListesi = ref([])
const ipYukleniyor = ref(false)
const ipModalAcik = ref(false)
const ipKaydediliyor = ref(false)
const yeniIp = ref({ ipAdresi: '', aciklama: '' })
const ipDuzenleme = ref(false)
const ipSeciliId = ref(null)
const yokSayilanlariGoster = ref(false)

const ipAktifMi = computed({
  get: () => (yeniIp.value.durum || 'AKTIF') !== 'PASIF',
  set: (v) => {
    yeniIp.value.durum = v ? 'AKTIF' : 'PASIF'
  }
})

// Faz 2: istemci taraflı filtreler + Excel.
const sonTarama = ref(null)
const filtreSeviye = ref('TUMU')
const filtreTur = ref(null)
const filtreQ = ref('')
const excelYukleniyor = ref(false)
const ipExcelYukleniyor = ref(false)

const kritikYuksekSayisi = computed(
  () => anomaliler.value.filter((a) => ['KRITIK', 'YUKSEK'].includes(a.seviye)).length
)
const ortaSayisi = computed(() => anomaliler.value.filter((a) => a.seviye === 'ORTA').length)
const dusukSayisi = computed(() => anomaliler.value.filter((a) => a.seviye === 'DUSUK').length)

const SEVIYE_ANAHTARLARI = {
  KRITIK: 'anomaliler.seviyeKritik',
  YUKSEK: 'anomaliler.seviyeYuksek',
  ORTA: 'anomaliler.seviyeOrta',
  DUSUK: 'anomaliler.seviyeDusuk'
}
const seviyeLabel = (s) => (SEVIYE_ANAHTARLARI[s] ? t(SEVIYE_ANAHTARLARI[s]) : s || '-')

const TUR_ANAHTARLARI = {
  MUKERRER_FATURA: 'anomaliler.turMukerrerFatura',
  MUKERRER_ODEME: 'anomaliler.turMukerrerOdeme',
  YUKSEK_TUTAR: 'anomaliler.turYuksekTutar',
  ANORMAL_STOK_CIKISI: 'anomaliler.turAnormalStokCikisi'
}
const turLabel = (tur) => (TUR_ANAHTARLARI[tur] ? t(TUR_ANAHTARLARI[tur]) : tur || '-')

const seviyeCipleri = computed(() => [
  { deger: 'TUMU', ad: t('anomaliler.cipTumu'), ikon: 'pi pi-list' },
  { deger: 'KRITIK_YUKSEK', ad: t('anomaliler.kpiKritik'), ikon: 'pi pi-exclamation-triangle' },
  { deger: 'ORTA', ad: t('anomaliler.seviyeOrta'), ikon: 'pi pi-exclamation-circle' },
  { deger: 'DUSUK', ad: t('anomaliler.seviyeDusuk'), ikon: 'pi pi-info-circle' }
])

const turSecenekleri = computed(() =>
  Object.keys(TUR_ANAHTARLARI).map((k) => ({ deger: k, ad: t(TUR_ANAHTARLARI[k]) }))
)

const filtreliAnomaliler = computed(() => {
  const q = filtreQ.value.trim().toLowerCase()
  return anomaliler.value.filter((a) => {
    if (a.durum === 'YOK_SAYILDI' && !yokSayilanlariGoster.value) return false
    if (filtreSeviye.value === 'KRITIK_YUKSEK' && !['KRITIK', 'YUKSEK'].includes(a.seviye)) return false
    if (['ORTA', 'DUSUK'].includes(filtreSeviye.value) && a.seviye !== filtreSeviye.value) return false
    if (filtreTur.value && a.tur !== filtreTur.value) return false
    if (q) {
      const metin = `${a.baslik || ''} ${a.aciklama || ''} ${a.oneri || ''} ${a.tur || ''}`.toLowerCase()
      if (!metin.includes(q)) return false
    }
    return true
  })
})

const filtreTemizle = () => {
  filtreSeviye.value = 'TUMU'
  filtreTur.value = null
  filtreQ.value = ''
}

const excelParams = () => {
  const p = {}
  if (filtreSeviye.value === 'KRITIK_YUKSEK') p.seviye = 'KRITIK,YUKSEK'
  else if (filtreSeviye.value !== 'TUMU') p.seviye = filtreSeviye.value
  if (filtreTur.value) p.tur = filtreTur.value
  if (filtreQ.value.trim()) p.q = filtreQ.value.trim()
  return p
}

const indirBlob = async (istek, dosyaAdi, yukleniyorRef) => {
  yukleniyorRef.value = true
  try {
    const res = await istek()
    const url = window.URL.createObjectURL(
      new Blob([res.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    )
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', `${dosyaAdi}-${new Date().toISOString().slice(0, 10)}.xlsx`)
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)
    toastBildirim.basarili(t('anomaliler.excelIndirildi'))
  } catch {
    toastBildirim.hata(t('anomaliler.excelHata'))
  } finally {
    yukleniyorRef.value = false
  }
}

const excelIndir = () =>
  indirBlob(() => excelAPI.anomaliler(excelParams()), 'anomaliler', excelYukleniyor)
const ipExcelIndir = () =>
  indirBlob(() => excelAPI.ipWhitelist(), 'ip-whitelist', ipExcelYukleniyor)

const ilgiliKaydaGit = (item) => {
  if (item.ilgiliKayitNo) {
    router.push({ name: 'Faturalar', query: { no: item.ilgiliKayitNo } })
  }
}

const durumGuncelle = async (item, durum) => {
  try {
    await anomaliAPI.durumGuncelle(item.id, durum)
    item.durum = durum === 'ACIK' ? null : durum
    toastBildirim.basarili(t('anomaliler.durumGuncellendi'))
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('anomaliler.durumHata'))
  }
}

const anomaliAksiyonlari = (item) => {
  const items = []
  if (item.ilgiliKayitNo) {
    items.push({
      etiket: t('anomaliler.ilgiliKaydaGit'),
      ikon: 'pi pi-external-link',
      islem: () => ilgiliKaydaGit(item)
    })
  }
  if (item.durum !== 'INCELENDI') {
    items.push({
      etiket: t('anomaliler.incelendi'),
      ikon: 'pi pi-eye',
      islem: () => durumGuncelle(item, 'INCELENDI')
    })
  }
  if (item.durum !== 'YOK_SAYILDI') {
    items.push({
      etiket: t('anomaliler.yokSay'),
      ikon: 'pi pi-eye-slash',
      islem: () => durumGuncelle(item, 'YOK_SAYILDI')
    })
  }
  if (item.durum) {
    items.push({
      etiket: t('anomaliler.geriAl'),
      ikon: 'pi pi-undo',
      islem: () => durumGuncelle(item, 'ACIK')
    })
  }
  return items
}

const anomalileriYukle = async () => {
  yukleniyor.value = true
  try {
    const res = await anomaliAPI.tara()
    anomaliler.value = res.data || []
    sonTarama.value = new Date()
    if (anomaliler.value.length > 0) {
      toastBildirim.uyari(t('anomaliler.supheliDurum', { sayi: anomaliler.value.length }))
    }
  } catch {
    toastBildirim.hata(t('anomaliler.taramaHatasi'))
  } finally {
    yukleniyor.value = false
  }
}

const ipListesiYukle = async () => {
  ipYukleniyor.value = true
  try {
    const res = await anomaliAPI.getIpWhitelist()
    ipListesi.value = res.data || []
  } catch {
    /* empty */
  } finally {
    ipYukleniyor.value = false
  }
}

const IPV4_CIDR =
  /^(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3}(\/(3[0-2]|[12]?\d))?$/

const ipDialogAc = (data) => {
  ipDuzenleme.value = !!data
  ipSeciliId.value = data?.id || null
  yeniIp.value = data
    ? { ipAdresi: data.ipAdresi || '', aciklama: data.aciklama || '', durum: data.durum || 'AKTIF' }
    : { ipAdresi: '', aciklama: '', durum: 'AKTIF' }
  ipModalAcik.value = true
}

const ipKaydet = async () => {
  const ip = (yeniIp.value.ipAdresi || '').trim()
  if (!ip) {
    toastBildirim.uyari(t('anomaliler.ipGiriniz'))
    return
  }
  if (!IPV4_CIDR.test(ip)) {
    toastBildirim.uyari(t('anomaliler.ipGecersiz'))
    return
  }
  ipKaydediliyor.value = true
  try {
    const payload = { ipAdresi: ip, aciklama: yeniIp.value.aciklama, durum: yeniIp.value.durum || 'AKTIF' }
    const res = ipDuzenleme.value
      ? await anomaliAPI.updateIpWhitelist(ipSeciliId.value, payload)
      : await anomaliAPI.addIpWhitelist(payload)
    ipListesi.value = res.data || []
    ipModalAcik.value = false
    toastBildirim.basarili(
      ipDuzenleme.value ? t('anomaliler.ipGuncellendi') : t('anomaliler.ipTanimlandi')
    )
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('anomaliler.ipEklenemedi'))
  } finally {
    ipKaydediliyor.value = false
  }
}

const ipDurumDegistir = async (data) => {
  try {
    const res = await anomaliAPI.updateIpWhitelist(data.id, {
      ipAdresi: data.ipAdresi,
      aciklama: data.aciklama,
      durum: data.durum === 'PASIF' ? 'AKTIF' : 'PASIF'
    })
    ipListesi.value = res.data || []
    toastBildirim.basarili(t('anomaliler.ipGuncellendi'))
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('anomaliler.ipEklenemedi'))
  }
}

const ipSil = (data) => {
  confirm.require({
    message: t('anomaliler.ipSilOnay', { ip: data.ipAdresi }),
    header: t('anomaliler.ipSil'),
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: t('common.evetSil'),
    rejectLabel: t('common.vazgec'),
    accept: async () => {
      try {
        const res = await anomaliAPI.deleteIpWhitelist(data.id)
        ipListesi.value = res.data || []
        toastBildirim.basarili(t('anomaliler.ipKaldirildi'))
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || t('anomaliler.ipSilinemedi'))
      }
    }
  })
}

const ipAksiyonlari = (data) => [
  { etiket: t('common.edit'), ikon: 'pi pi-pencil', islem: () => ipDialogAc(data) },
  {
    etiket: data.durum === 'PASIF' ? t('anomaliler.ipAktifYap') : t('anomaliler.ipPasifYap'),
    ikon: 'pi pi-power-off',
    islem: () => ipDurumDegistir(data)
  },
  { etiket: t('anomaliler.ipSil'), ikon: 'pi pi-trash', sinif: 'eylem-sil', islem: () => ipSil(data) }
]

onMounted(() => {
  anomalileriYukle()
  if (isAdmin.value) ipListesiYukle()
})
</script>

<style scoped>
.anomaliler-page {
  padding: 0;
}
.kpi-satiri {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}
.anomali-arac,
.ip-arac {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}
.filtre-arama {
  min-width: 220px;
  flex: 1 1 220px;
  max-width: 340px;
}
.filtre-select {
  min-width: 180px;
}
.arac-bosluk {
  flex: 1 1 auto;
}
.tarama-zamani {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--text-muted);
}
.anomali-cipler {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}
.anomali-cip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 999px;
  border: 1px solid var(--border);
  background: var(--bg-card);
  color: var(--text-secondary);
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--dur-base, 0.2s) var(--ease-standard, ease);
}
.anomali-cip:hover {
  border-color: var(--accent-border);
  color: var(--text-primary);
}
.anomali-cip.aktif {
  background: var(--accent-soft, rgba(245, 158, 11, 0.16));
  border-color: var(--accent-border, rgba(245, 158, 11, 0.5));
  color: var(--accent);
}
.anomali-cip i {
  font-size: 12px;
}
.ip-info-box {
  background: var(--accent-soft);
  border: 1px solid var(--accent-border);
  border-radius: 10px;
  padding: 14px 18px;
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 20px;
}
.ip-info-box i {
  font-size: 28px;
  color: var(--accent);
}
.ip-info-box strong {
  display: block;
  font-size: 14px;
  color: var(--text-primary);
  margin-bottom: 2px;
}
.ip-info-box p {
  margin: 0;
  font-size: 13px;
  color: var(--text-secondary);
}
.empty-box {
  background: var(--bg-card, #1e293b);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 3rem;
  text-align: center;
  margin-top: 1.5rem;
}
.success-icon {
  font-size: 3.5rem;
  color: #22c55e;
  margin-bottom: 1rem;
}
.anomali-grid {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  margin-top: 1.5rem;
}
.anomali-card {
  background: var(--bg-card, #1e293b);
  border: 1px solid var(--border);
  border-left: 6px solid var(--accent);
  border-radius: 10px;
  padding: 1.25rem;
}
.anomali-card.kritik, .anomali-card.yuksek {
  border-left-color: #ef4444;
}
.anomali-card.orta {
  border-left-color: #f59e0b;
}
.anomali-card.dusuk {
  border-left-color: var(--accent);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.75rem;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}
.yok-say-toggle {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-left: auto;
  font-size: 12.5px;
  color: var(--text-secondary);
  cursor: pointer;
}
.switch-satir {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}
.ip-mobil {
  display: none;
}
.ip-skeleton {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.ip-mobil-kart {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 12px 14px;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: var(--bg-card);
}
.ip-mk-ust {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.ip-mk-satir {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  font-size: 12.5px;
  color: var(--text-secondary);
}
.ip-mk-tarih {
  font-size: 12px;
  color: var(--text-muted);
}

@media (max-width: 768px) {
  .ip-masa {
    display: none;
  }
  .ip-mobil {
    display: flex;
    flex-direction: column;
    gap: 10px;
  }
  .anomali-arac .filtre-arama,
  .anomali-arac .filtre-select {
    min-width: 0;
    max-width: none;
    flex: 1 1 100%;
  }
  .card-header {
    flex-wrap: wrap;
    gap: 8px;
  }
  .header-left,
  .header-right {
    flex-wrap: wrap;
  }
}

@media (prefers-reduced-motion: reduce) {
  .anomali-cip {
    transition: none;
  }
}
.badge {
  padding: 0.25rem 0.6rem;
  border-radius: 4px;
  font-size: 0.75rem;
  font-weight: 700;
  text-transform: uppercase;
}
.badge.kritik, .badge.yuksek {
  background: rgba(239, 68, 68, 0.25);
  color: #f87171;
}
.badge.orta {
  background: rgba(245, 158, 11, 0.25);
  color: #fbbf24;
}
.badge.dusuk {
  background: var(--accent-soft-strong);
  color: var(--accent);
}

.tur-label {
  margin-left: 0.5rem;
  font-size: 0.8rem;
  color: var(--text-secondary, #94a3b8);
  font-weight: 600;
}
.tarih {
  font-size: 0.8rem;
  color: var(--text-muted, #64748b);
}
.card-title {
  margin: 0 0 0.5rem 0;
  font-size: 1.1rem;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}
.card-desc {
  color: var(--text-secondary, #cbd5e1);
  margin-bottom: 0.75rem;
  font-size: 0.95rem;
}
.oneri-box {
  background: rgba(56, 189, 248, 0.08);
  border: 1px solid rgba(56, 189, 248, 0.15);
  border-radius: 6px;
  padding: 0.6rem 0.8rem;
  font-size: 0.875rem;
  color: #38bdf8;
}
</style>
