<template>
  <div class="denetim-page">
    <PageHeader
      :title="t('denetim.title')"
      :subtitle="t('denetim.subtitle')"
    >
      <template #actions>
        <Button
          :icon="otomatikYenile ? 'pi pi-sync' : 'pi pi-play'"
          :label="t('denetim.otomatikYenile')"
          size="small"
          :severity="otomatikYenile ? 'success' : 'secondary'"
          :outlined="!otomatikYenile"
          :aria-pressed="otomatikYenile"
          :title="t('denetim.otomatikYenileIpucu')"
          @click="otomatikYenileDegistir"
        />
        <Button
          icon="pi pi-refresh"
          size="small"
          text
          rounded
          :aria-label="t('common.refresh')"
          :loading="yukleniyor"
          @click="yenile"
        />
        <Button
          :label="t('denetim.excel')"
          icon="pi pi-file-excel"
          class="p-button-sm p-button-outlined"
          :loading="excelYukleniyor"
          @click="excelIndir"
        />
      </template>
    </PageHeader>

    <div class="kpi-grid">
      <KpiKart
        :baslik="t('denetim.kpiToplam')"
        :deger="ozet.toplam"
        ikon="pi pi-list"
        :para-birimi="false"
        renk="#3b82f6"
      />
      <KpiKart
        :baslik="t('denetim.kpiOlustur')"
        :deger="ozetIslem('OLUSTUR')"
        ikon="pi pi-plus-circle"
        :para-birimi="false"
        renk="#10b981"
      />
      <KpiKart
        :baslik="t('denetim.kpiGuncelle')"
        :deger="ozetIslem('GUNCELLE')"
        ikon="pi pi-pencil"
        :para-birimi="false"
        renk="#f59e0b"
      />
      <KpiKart
        :baslik="t('denetim.kpiSil')"
        :deger="ozetIslem('SIL')"
        ikon="pi pi-trash"
        :para-birimi="false"
        renk="#ef4444"
      />
      <KpiKart
        :baslik="t('denetim.kpiHata')"
        :deger="ozetIslem('HATA')"
        ikon="pi pi-exclamation-triangle"
        :para-birimi="false"
        renk="#dc2626"
      />
    </div>

    <Card class="filtre-karti">
      <template #content>
        <div class="arac-satir">
          <span class="arama-kutu">
            <i class="pi pi-search" />
            <InputText
              v-model="aramaMetni"
              :placeholder="t('denetim.aramaPlaceholder')"
              class="arama-input"
              @keyup.enter="filtrele"
            />
          </span>
          <Button
            :label="t('denetim.filtreler')"
            icon="pi pi-filter"
            class="p-button-sm p-button-outlined"
            :badge="aktifFiltreSayisi ? String(aktifFiltreSayisi) : undefined"
            @click="filtrePanel?.toggle($event)"
          />
          <Button
            :label="t('denetim.sadeceRiskli')"
            icon="pi pi-exclamation-triangle"
            size="small"
            :severity="riskli ? 'danger' : 'secondary'"
            :outlined="!riskli"
            :aria-pressed="riskli"
            @click="riskliDegistir"
          />
          <Button
            v-if="aktifFiltreSayisi"
            :label="t('denetim.temizle')"
            icon="pi pi-filter-slash"
            class="p-button-sm p-button-text"
            @click="filtreTemizle"
          />
          <span class="arac-bosluk" />
          <Dropdown
            v-model="seciliKayitliFiltre"
            :options="kayitliFiltreler"
            option-label="ad"
            :placeholder="t('denetim.kayitliFiltreler')"
            class="kayitli-filtre"
            @change="kayitliFiltreYukle"
          />
          <Button
            :icon="gosterim === 'tablo' ? 'pi pi-th-large' : 'pi pi-list'"
            class="p-button-text p-button-sm"
            :title="gosterim === 'tablo' ? t('denetim.kartGorunumu') : t('denetim.tabloGorunumu')"
            :aria-label="gosterim === 'tablo' ? t('denetim.kartGorunumu') : t('denetim.tabloGorunumu')"
            @click="gosterimDegistir"
          />
        </div>
      </template>
    </Card>

    <Popover ref="filtrePanel">
      <div class="filtre-popover">
        <div class="filtre-grid">
          <div class="filtre-alan">
            <label>{{ t('denetim.islemTuru') }}</label>
            <Select
              v-model="filtre.islem"
              :options="islemTipleri"
              :placeholder="t('denetim.tumu')"
              class="w-full"
              show-clear
              clear-icon="pi pi-times"
              @change="filtrele"
            />
          </div>
          <div class="filtre-alan">
            <label>{{ t('denetim.entity') }}</label>
            <Select
              v-model="filtre.entityAdi"
              :options="entityListesi"
              :placeholder="t('denetim.tumu')"
              class="w-full"
              show-clear
              clear-icon="pi pi-times"
              @change="filtrele"
            />
          </div>
          <div class="filtre-alan">
            <label>{{ t('denetim.kullanici') }}</label>
            <Select
              v-model="filtre.kullaniciId"
              :options="kullanicilar"
              option-label="ad"
              option-value="id"
              :placeholder="t('denetim.tumu')"
              class="w-full"
              filter
              show-clear
              clear-icon="pi pi-times"
              @change="filtrele"
            />
          </div>
          <div class="filtre-alan">
            <label>{{ t('denetim.entityId') }}</label>
            <InputText
              v-model="filtre.entityId"
              type="number"
              :placeholder="t('denetim.entityIdPlaceholder')"
              class="w-full"
              @keyup.enter="filtrele"
            />
          </div>
          <div class="filtre-alan">
            <label>{{ t('denetim.tarihAraligi') }}</label>
            <TarihHizliSecim v-model="filtre.tarihAraligi" />
          </div>
          <div class="filtre-alan">
            <label>{{ t('denetim.ozelTarihAraligi') }}</label>
            <DatePicker
              v-model="filtre.tarihAraligi"
              selection-mode="range"
              date-format="dd.mm.yy"
              :placeholder="t('denetim.baslangicBitis')"
              class="w-full"
            />
          </div>
        </div>
        <div class="filtre-popover-aksiyon">
          <Button
            :label="t('denetim.filtreKaydet')"
            icon="pi pi-bookmark"
            class="p-button-sm p-button-text"
            @click="kaydetDialogAc"
          />
          <Button
            icon="pi pi-trash"
            class="p-button-sm p-button-text p-button-danger"
            :title="t('denetim.filtreSil')"
            :aria-label="t('denetim.filtreSil')"
            :disabled="!seciliKayitliFiltre"
            @click="kayitliFiltreSil"
          />
          <span class="arac-bosluk" />
          <Button
            :label="t('denetim.uygula')"
            icon="pi pi-check"
            class="p-button-sm"
            @click="filtreUygula"
          />
        </div>
      </div>
    </Popover>

    <Dialog
      v-model:visible="kayitliFiltreDialog"
      :header="t('denetim.filtreyiKaydet')"
      :modal="true"
      style="width: 380px"
    >
      <FormField
        :label="t('denetim.filtreAdi')"
        :required="true"
      >
        <InputText
          v-model="yeniFiltreAdi"
          :placeholder="t('denetim.filtreAdiPlaceholder')"
          class="w-full"
        />
      </FormField>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          icon="pi pi-times"
          class="p-button-text"
          @click="kayitliFiltreDialog = false"
        />
        <Button
          :label="t('common.save')"
          icon="pi pi-check"
          :disabled="!yeniFiltreAdi?.trim()"
          @click="filtreKaydet"
        />
      </template>
    </Dialog>

    <Card v-if="gosterim === 'tablo'">
      <template #content>
        <DataTable
          :value="logSatirlari"
          :loading="yukleniyor"
          :row-class="satirSinifi"
          striped-rows
          :rows="PAGE_SIZE"
          :paginator="true"
          :total-records="toplamKayit"
          lazy
          :first="sayfa * PAGE_SIZE"
          size="small"
          sort-field="tarih"
          :sort-order="-1"
          @page="sayfaDegisti"
        >
          <template #empty>
            <EmptyState :message="t('denetim.empty')" />
          </template>
          <Column
            field="tarih"
            :header="t('common.date')"
            style="width: 150px"
          >
            <template #body="s">
              {{ formatDate(s.data.tarih) }}
            </template>
          </Column>
          <Column
            field="kullaniciAdi"
            :header="t('denetim.kullanici')"
            style="width: 150px"
          >
            <template #body="s">
              {{ s.data.kullaniciAdi || (s.data.kullaniciId ? '#' + s.data.kullaniciId : '-') }}
            </template>
          </Column>
          <Column
            field="islem"
            :header="t('denetim.islem')"
            style="width: 110px"
          >
            <template #body="s">
              <Tag
                :value="islemEtiket(s.data.islem)"
                :severity="islemSeverity(s.data.islem)"
              />
            </template>
          </Column>
          <Column
            :header="t('denetim.entity')"
            style="width: 160px"
          >
            <template #body="s">
              <button
                v-if="s.data.entityAdi"
                type="button"
                class="entity-cip"
                @click="detayGoster(s.data)"
              >
                {{ s.data.entityAdi }}<span v-if="s.data.entityId"> #{{ s.data.entityId }}</span>
              </button>
              <span v-else>-</span>
            </template>
          </Column>
          <Column
            field="aciklama"
            :header="t('common.description')"
          />
          <Column
            field="detay"
            :header="t('denetim.detay')"
          >
            <template #body="s">
              <span
                v-if="s.data.detay"
                class="detay-metin"
                @click="detayGoster(s.data)"
              >
                {{ s.data.kisaDetay }}
              </span>
              <span v-else>-</span>
            </template>
          </Column>
          <Column
            field="ipAdresi"
            :header="t('denetim.ip')"
            style="width: 120px"
          />
        </DataTable>
      </template>
    </Card>

    <div
      v-else
      class="denetim-kartlar"
    >
      <EmptyState
        v-if="!logSatirlari.length && !yukleniyor"
        :message="t('denetim.empty')"
      />
      <div
        v-for="l in logSatirlari"
        :key="l.id"
        class="denetim-kart"
        :class="{ 'riskli-kart': riskliMi(l) }"
      >
        <div class="kart-ust">
          <Tag
            :value="islemEtiket(l.islem)"
            :severity="islemSeverity(l.islem)"
          />
          <span class="kart-tarih">{{ formatDate(l.tarih) }}</span>
        </div>
        <div class="kart-satir">
          <i class="pi pi-user" />
          <span>{{ l.kullaniciAdi || (l.kullaniciId ? '#' + l.kullaniciId : '-') }}</span>
        </div>
        <button
          v-if="l.entityAdi"
          type="button"
          class="entity-cip"
          @click="detayGoster(l)"
        >
          <i class="pi pi-box" /> {{ l.entityAdi }}<span v-if="l.entityId"> #{{ l.entityId }}</span>
        </button>
        <p
          v-if="l.aciklama"
          class="kart-aciklama"
        >
          {{ l.aciklama }}
        </p>
        <div class="kart-alt">
          <span
            v-if="l.ipAdresi"
            class="kart-ip"
          ><i class="pi pi-desktop" /> {{ l.ipAdresi }}</span>
          <span class="arac-bosluk" />
          <Button
            v-if="l.detay"
            :label="t('denetim.detay')"
            icon="pi pi-search"
            size="small"
            text
            @click="detayGoster(l)"
          />
        </div>
      </div>
      <Paginator
        v-if="toplamKayit > PAGE_SIZE"
        :rows="PAGE_SIZE"
        :total-records="toplamKayit"
        :first="sayfa * PAGE_SIZE"
        @page="sayfaDegisti"
      />
    </div>

    <Dialog
      v-model:visible="detayDialogAcik"
      :header="t('denetim.islemDetayi')"
      :modal="true"
      :style="{ width: '90%', maxWidth: '640px' }"
      :breakpoints="{ '575px': '95vw' }"
    >
      <div class="detay-dialog-icerik">
        <div class="detay-dialog-ust">
          <Tag
            :value="islemEtiket(seciliDetay?.islem)"
            :severity="islemSeverity(seciliDetay?.islem)"
          />
          <span class="detay-dialog-tarih">{{ seciliDetay?.tarih ? formatDate(seciliDetay.tarih) : '-' }}</span>
        </div>
        <div class="detay-dialog-satir">
          <span class="detay-dialog-etiket">{{ t('denetim.entity') }}</span>
          <button
            v-if="seciliDetay?.entityAdi"
            type="button"
            class="entity-cip"
            @click="kopyala(seciliDetay.entityAdi, t('denetim.entityKopyalandi'))"
          >
            {{ seciliDetay.entityAdi }} #{{ seciliDetay?.entityId ?? '-' }}
          </button>
          <span v-else>-</span>
        </div>
        <div class="detay-dialog-satir">
          <span class="detay-dialog-etiket">{{ t('denetim.kullanici') }}</span>
          <span>{{ seciliDetay?.kullaniciAdi || (seciliDetay?.kullaniciId ? '#' + seciliDetay.kullaniciId : '-') }}</span>
        </div>
        <div class="detay-dialog-satir">
          <span class="detay-dialog-etiket">{{ t('denetim.ipAdresi') }}</span>
          <span class="detay-dialog-deger">{{ seciliDetay?.ipAdresi || '-' }}</span>
        </div>
        <div
          v-if="seciliDetay?.aciklama"
          class="detay-dialog-satir"
        >
          <span class="detay-dialog-etiket">{{ t('denetim.aciklama') }}</span>
          <span class="detay-dialog-deger">{{ seciliDetay.aciklama }}</span>
        </div>

        <div
          v-if="seciliDetay?.detay"
          class="detay-json-blok"
        >
          <div class="detay-json-ust">
            <span class="detay-json-baslik"><i class="pi pi-code" /> {{ t('denetim.detayJson') }}</span>
            <span class="arac-bosluk" />
            <Button
              icon="pi pi-copy"
              :label="t('common.copy')"
              size="small"
              text
              @click="kopyala(detayFormatli, t('denetim.detayKopyalandi'))"
            />
            <Button
              :icon="detayAcik ? 'pi pi-chevron-up' : 'pi pi-chevron-down'"
              size="small"
              text
              rounded
              :aria-label="detayAcik ? t('denetim.detayGizle') : t('denetim.detayGoster')"
              @click="detayAcik = !detayAcik"
            />
          </div>
          <pre
            v-if="detayAcik"
            class="detay-json"
          >{{ detayFormatli }}</pre>
          <p
            v-else
            class="detay-json-ozet"
          >
            {{ detayOzet }}
          </p>
          <small class="detay-json-aciklama">{{ t('denetim.detayAciklama') }}</small>
        </div>
      </div>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { unwrapList } from '../api/utils/unwrap.js'
import { useToast } from 'primevue/usetoast'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { auditLogAPI, excelAPI, kullaniciAPI } from '../api/index.js'
import TarihHizliSecim from '../components/TarihHizliSecim.vue'
import FormField from '../components/FormField.vue'
import EmptyState from '../components/EmptyState.vue'
import KpiKart from '../components/KpiKart.vue'
import { usePanoyaKopyala } from '../composables/usePanoyaKopyala.js'
import { useI18n } from 'vue-i18n'
import { formatTarihSaat as formatDate, getLocalDateString } from '../utils/format.js'

const toast = useToast()
const toastBildirim = useToastBildirim()
const { kopyala } = usePanoyaKopyala()
const { t } = useI18n()

const PAGE_SIZE = 20
const MAKS_KAYITLI_FILTRE = 20

const logs = ref([])
const yukleniyor = ref(false)
const sayfa = ref(0)
const toplamKayit = ref(0)
const islemTipleri = ref([])
const entityListesi = ref([])
const kullanicilar = ref([])
const excelYukleniyor = ref(false)

// Serbest metin arama (aciklama/entity/IP) + riskli (SIL/HATA) filtresi.
const aramaMetni = ref('')
const riskli = ref(false)
const filtrePanel = ref(null)

// KPI ozeti (islem tipine gore sayimlar).
const ozet = ref({ toplam: 0, islemBazli: {} })
const ozetIslem = (kod) => ozet.value.islemBazli?.[kod] || 0

// Opsiyonel periyodik yenileme (varsayilan kapali; kullanici acar).
const OTOMATIK_ANAHTAR = 'raspel_denetim_otomatik_yenile'
const otomatikYenile = ref(localStorage.getItem(OTOMATIK_ANAHTAR) === '1')
let otomatikTimer = null

// Gorunum: tablo / kart. Kucuk ekranda varsayilan kart (PWA/mobil).
const GORUNUM_ANAHTAR = 'raspel_denetim_gosterim'
const gosterim = ref(localStorage.getItem(GORUNUM_ANAHTAR) || 'tablo')
const gosterimDegistir = () => {
  gosterim.value = gosterim.value === 'tablo' ? 'kart' : 'tablo'
  localStorage.setItem(GORUNUM_ANAHTAR, gosterim.value)
}

// Riskli islem (silme/hata) satirlari gorsel olarak one cikarilir.
const riskliMi = (l) => l?.islem === 'SIL' || l?.islem === 'HATA'
const satirSinifi = (d) => (riskliMi(d) ? 'riskli-satir' : '')

const filtre = ref({
  islem: null,
  entityAdi: null,
  kullaniciId: null,
  entityId: null,
  tarihAraligi: []
})

const KAYITLI_ANAHTAR = 'raspel_kayitli_filtreler_denetim'
const kayitliFiltreler = ref([])
const seciliKayitliFiltre = ref(null)
const kayitliFiltreDialog = ref(false)
const yeniFiltreAdi = ref('')

// Tarih araligi degistiginde (hizli secim, ozel aralik veya kayitli filtre
// yuklemesi) listeyi otomatik yenile. ONCEDEN yalnizca iki Select ve
// DatePicker'in `@date-select`'i yeniliyordu; "Bu Ay" gibi hizli secimler
// tabloyu GUNCELLEMIYORDU.
watch(
  () => filtre.value.tarihAraligi,
  () => filtrele()
)

// --- Kayitli filtreler: ISO-guvenli serilestirme ---
// ONCEDEN Date nesneleri JSON.stringify ile ISO string'e donusuyor, geri
// yuklenirken DatePicker'a string geliyor ve aralik bos gorunuyordu.
const isoToDate = (s) => (typeof s === 'string' && s ? new Date(`${s}T00:00:00`) : null)

const seriFiltre = () => ({
  islem: filtre.value.islem || null,
  entityAdi: filtre.value.entityAdi || null,
  kullaniciId: filtre.value.kullaniciId || null,
  entityId: filtre.value.entityId || null,
  tarihAraligi:
    filtre.value.tarihAraligi?.length === 2 && filtre.value.tarihAraligi[0]
      ? [getLocalDateString(filtre.value.tarihAraligi[0]), getLocalDateString(filtre.value.tarihAraligi[1])]
      : []
})

const cozFiltre = (f = {}) => ({
  islem: f.islem || null,
  entityAdi: f.entityAdi || null,
  kullaniciId: f.kullaniciId || null,
  entityId: f.entityId || null,
  tarihAraligi:
    Array.isArray(f.tarihAraligi) && f.tarihAraligi.length === 2
      ? [isoToDate(f.tarihAraligi[0]), isoToDate(f.tarihAraligi[1])]
      : []
})

const kayitliFiltreleriKaydet = () => {
  try {
    localStorage.setItem(KAYITLI_ANAHTAR, JSON.stringify(kayitliFiltreler.value))
  } catch {
    /* yoksay */
  }
}

const kayitliFiltreleriYukle = () => {
  try {
    kayitliFiltreler.value = JSON.parse(localStorage.getItem(KAYITLI_ANAHTAR) || '[]')
  } catch {
    kayitliFiltreler.value = []
  }
}

const kaydetDialogAc = () => {
  yeniFiltreAdi.value = seciliKayitliFiltre.value?.ad || ''
  kayitliFiltreDialog.value = true
}

const filtreKaydet = () => {
  const ad = yeniFiltreAdi.value.trim()
  if (!ad) return
  const kayit = { ad, filtre: seriFiltre() }
  // Ayni ad varsa guncelle (mukerrer kayit olusmasin).
  const idx = kayitliFiltreler.value.findIndex((f) => f.ad.toLowerCase() === ad.toLowerCase())
  if (idx >= 0) {
    kayitliFiltreler.value[idx] = kayit
  } else {
    kayitliFiltreler.value.push(kayit)
    if (kayitliFiltreler.value.length > MAKS_KAYITLI_FILTRE) kayitliFiltreler.value.shift()
  }
  kayitliFiltreleriKaydet()
  kayitliFiltreDialog.value = false
  yeniFiltreAdi.value = ''
  toast.add({ severity: 'success', summary: t('denetim.kaydedildi'), detail: t('denetim.filtreKaydedildi'), life: 3000 })
}

const kayitliFiltreYukle = () => {
  if (!seciliKayitliFiltre.value) return
  filtre.value = cozFiltre(seciliKayitliFiltre.value.filtre)
  toast.add({ severity: 'info', summary: t('denetim.filtreUygulandi'), detail: seciliKayitliFiltre.value.ad, life: 3000 })
}

const kayitliFiltreSil = () => {
  if (!seciliKayitliFiltre.value) return
  const ad = seciliKayitliFiltre.value.ad
  kayitliFiltreler.value = kayitliFiltreler.value.filter((f) => f.ad !== ad)
  kayitliFiltreleriKaydet()
  seciliKayitliFiltre.value = null
  toast.add({ severity: 'success', summary: t('denetim.filtreSilindi'), life: 3000 })
}

// Liste ve ozet sorgularinin paylastigi filtre parametreleri.
// `q` yalnizca 2+ karakterde gonderilir (AramaTemizleyici kisa girdide eslesme
// uretmez; istemciyi de tutarli davranmaya zorlar).
const filtreParams = () => {
  const params = {}
  if (filtre.value.entityAdi) params.entityAdi = filtre.value.entityAdi
  if (filtre.value.kullaniciId) params.kullaniciId = filtre.value.kullaniciId
  if (filtre.value.entityId) params.entityId = filtre.value.entityId
  const q = aramaMetni.value.trim()
  if (q.length >= 2) params.q = q
  if (filtre.value.tarihAraligi?.length === 2 && filtre.value.tarihAraligi[0]) {
    params.baslangicTarih = formatISODate(filtre.value.tarihAraligi[0])
    params.bitisTarih = formatISODate(filtre.value.tarihAraligi[1])
  }
  return params
}

const yukle = async (page = 0) => {
  yukleniyor.value = true
  try {
    const params = { page, size: PAGE_SIZE, ...filtreParams() }
    if (filtre.value.islem) params.islem = filtre.value.islem
    if (riskli.value) params.riskli = true
    const r = await auditLogAPI.getAll(params)
    logs.value = unwrapList(r)
    toplamKayit.value = r.data?.totalElements || logs.value.length
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('denetim.hataYukleme'))
  } finally {
    yukleniyor.value = false
  }
}

const ozetYukle = async () => {
  try {
    const r = await auditLogAPI.ozet(filtreParams())
    ozet.value = {
      toplam: r.data?.toplam || 0,
      islemBazli: r.data?.islemBazli || {}
    }
  } catch {
    /* ozet alinamazsa KPI'lar sifirda kalir; liste akisini engellemez */
  }
}

// Manuel + periyodik yenileme: liste ve KPI birlikte tazelenir.
const yenile = () => {
  yukle(sayfa.value)
  ozetYukle()
}

const filtrele = () => {
  sayfa.value = 0
  yukle(0)
  ozetYukle()
}

const filtreUygula = () => {
  filtrele()
  filtrePanel.value?.hide()
}

const riskliDegistir = () => {
  riskli.value = !riskli.value
  filtrele()
}

const otomatikZamanlayici = () => {
  if (otomatikTimer) {
    clearInterval(otomatikTimer)
    otomatikTimer = null
  }
  if (otomatikYenile.value) {
    otomatikTimer = setInterval(yenile, 30000)
  }
}

const otomatikYenileDegistir = () => {
  otomatikYenile.value = !otomatikYenile.value
  localStorage.setItem(OTOMATIK_ANAHTAR, otomatikYenile.value ? '1' : '0')
  otomatikZamanlayici()
}

// Filtre popover'indaki "badge" ve "Temizle" icin aktif filtre sayisi.
const aktifFiltreSayisi = computed(() => {
  let n = 0
  if (filtre.value.islem) n++
  if (filtre.value.entityAdi) n++
  if (filtre.value.kullaniciId) n++
  if (filtre.value.entityId) n++
  if (filtre.value.tarihAraligi?.length === 2 && filtre.value.tarihAraligi[0]) n++
  if (aramaMetni.value.trim().length >= 2) n++
  if (riskli.value) n++
  return n
})

const excelIndir = async () => {
  excelYukleniyor.value = true
  try {
    const params = { ...filtreParams() }
    if (filtre.value.islem) params.islem = filtre.value.islem
    if (riskli.value) params.riskli = true
    const res = await excelAPI.denetimLog(params)
    const url = window.URL.createObjectURL(
      new Blob([res.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    )
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', `denetim-log-${getLocalDateString()}.xlsx`)
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)
  } catch {
    toastBildirim.hata(t('denetim.hataExcel'))
  } finally {
    excelYukleniyor.value = false
  }
}

const filtreTemizle = () => {
  filtre.value = { islem: null, entityAdi: null, kullaniciId: null, entityId: null, tarihAraligi: [] }
  aramaMetni.value = ''
  riskli.value = false
  seciliKayitliFiltre.value = null
  filtrele()
}
const sayfaDegisti = (e) => {
  sayfa.value = e.page
  yukle(e.page)
}

const formatISODate = (d) => {
  if (!d) return null
  return getLocalDateString(d)
}

// Islem kodlarini kullaniciya cevirir (OLUSTUR/SIL/... ham gorunmesin).
const islemEtiketleri = computed(() => ({
  OLUSTUR: t('denetim.islemOlustur'),
  GUNCELLE: t('denetim.islemGuncelle'),
  SIL: t('denetim.islemSil'),
  ISLEM: t('denetim.islemIslem'),
  HATA: t('denetim.islemHata'),
  ARSIVLE: t('denetim.islemArsivle'),
  ARSIVLE_GERI_AL: t('denetim.islemArsivleGeriAl')
}))
const islemEtiket = (v) => islemEtiketleri.value[v] || v || '-'

const islemSeverity = (islem) => {
  if (islem === 'SIL' || islem === 'HATA') return 'danger'
  if (islem === 'OLUSTUR') return 'success'
  if (islem === 'GUNCELLE') return 'warn'
  if (islem === 'ARSIVLE' || islem === 'ARSIVLE_GERI_AL') return 'secondary'
  return 'info'
}

const kisaDetay = (detay) => {
  if (!detay) return '-'
  try {
    const obj = typeof detay === 'string' ? JSON.parse(detay) : detay
    const s = JSON.stringify(obj)
    return s.length > 60 ? s.slice(0, 60) + '…' : s
  } catch {
    return detay.length > 60 ? detay.slice(0, 60) + '…' : detay
  }
}

const logSatirlari = computed(() => logs.value.map((l) => ({ ...l, kisaDetay: kisaDetay(l.detay) })))

const detayDialogAcik = ref(false)
const seciliDetay = ref(null)
const detayFormatli = ref('')
const detayAcik = ref(true)
const detayOzet = computed(() => {
  const s = detayFormatli.value || ''
  const tekSatir = s.replace(/\s+/g, ' ').trim()
  return tekSatir.length > 140 ? tekSatir.slice(0, 140) + '…' : tekSatir
})

const detayGoster = (log) => {
  seciliDetay.value = log
  try {
    const obj = typeof log.detay === 'string' ? JSON.parse(log.detay) : log.detay
    detayFormatli.value = JSON.stringify(obj, null, 2)
  } catch {
    detayFormatli.value = log.detay || '-'
  }
  // Uzun JSON varsayilan acik; cok uzunsa kullanici katlayabilir.
  detayAcik.value = detayFormatli.value.length < 1500
  detayDialogAcik.value = true
}

const filtreSecenekleriniYukle = async () => {
  try {
    const [islemRes, entityRes, kullaniciRes] = await Promise.all([
      auditLogAPI.getIslemTipleri(),
      auditLogAPI.getEntityListesi(),
      kullaniciAPI.getAll({ page: 0, size: 200 })
    ])
    islemTipleri.value = islemRes.data || []
    entityListesi.value = entityRes.data || []
    kullanicilar.value = (unwrapList(kullaniciRes) || []).map((u) => ({
      id: u.id,
      ad: u.displayName || u.username || `#${u.id}`
    }))
  } catch {
    /* empty */
  }
}

onMounted(() => {
  // Kayitli tercih yoksa kucuk ekranda kart gorunumu varsayilan.
  if (!localStorage.getItem(GORUNUM_ANAHTAR)
      && typeof window.matchMedia === 'function'
      && window.matchMedia('(max-width: 768px)').matches) {
    gosterim.value = 'kart'
  }
  filtreSecenekleriniYukle()
  kayitliFiltreleriYukle()
  yukle()
  ozetYukle()
  otomatikZamanlayici()
})

onUnmounted(() => {
  if (otomatikTimer) clearInterval(otomatikTimer)
})
</script>

<style scoped>
.denetim-page {
  padding: 0;
  max-width: 100%;
}
.filtre-karti {
  margin-bottom: 1rem;
}
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(170px, 100%), 1fr));
  gap: 12px;
  margin-bottom: 1rem;
}
.arac-satir {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.arama-kutu {
  position: relative;
  display: inline-flex;
  align-items: center;
  flex: 1 1 240px;
  min-width: min(240px, 100%);
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
.arac-bosluk {
  flex: 1 1 auto;
}
.filtre-popover {
  min-width: min(420px, 90vw);
}
.filtre-popover-aksiyon {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px solid var(--border);
}
.filtre-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(180px, 100%), 1fr));
  gap: 12px;
  align-items: end;
}
.filtre-alan label {
  display: block;
  margin-bottom: 4px;
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--text-secondary);
}
.w-full {
  width: 100% !important;
}
.kayitli-filtre {
  min-width: min(170px, 100%) !important;
}
/* Riskli (silme/hata) satirlari ve kartlari one cikar. */
:deep(.riskli-satir) > td {
  background: rgba(239, 68, 68, 0.06) !important;
}
:deep(.riskli-satir) {
  box-shadow: inset 3px 0 0 var(--danger, #ef4444);
}
.entity-cip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  max-width: 100%;
  padding: 2px 10px;
  border: 1px solid var(--border);
  border-radius: 9999px;
  background: var(--bg-secondary, transparent);
  color: var(--text-primary);
  font-size: 0.78rem;
  cursor: pointer;
}
.entity-cip:hover {
  border-color: var(--accent);
  color: var(--accent);
}
.denetim-kartlar {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(min(300px, 100%), 1fr));
  gap: 12px;
}
.denetim-kart {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 14px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 1rem;
}
.riskli-kart {
  box-shadow: inset 4px 0 0 var(--danger, #ef4444);
}
.kart-ust {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.kart-tarih {
  font-size: 0.75rem;
  color: var(--text-muted);
  white-space: nowrap;
}
.kart-satir {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.85rem;
  color: var(--text-secondary);
}
.kart-aciklama {
  margin: 0;
  font-size: 0.85rem;
  color: var(--text-primary);
}
.kart-alt {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: auto;
}
.kart-ip {
  font-size: 0.75rem;
  color: var(--text-muted);
}
.detay-metin {
  font-family: monospace;
  font-size: 0.75rem;
  color: var(--text-secondary);
  cursor: pointer;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 200px;
  display: inline-block;
}
.detay-metin:hover {
  color: var(--primary-color, var(--accent));
  text-decoration: underline;
}
.detay-dialog-icerik {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.detay-dialog-ust {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 4px;
}
.detay-dialog-tarih {
  font-size: 0.8rem;
  color: var(--text-muted);
}
.detay-dialog-satir {
  display: flex;
  gap: 12px;
  font-size: 0.9rem;
}
.detay-dialog-etiket {
  min-width: 90px;
  color: var(--text-muted);
  font-weight: 600;
}
.detay-dialog-deger {
  flex: 1;
  min-width: 0;
  word-break: break-word;
}
.detay-json-blok {
  margin-top: 6px;
}
.detay-json-ust {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 6px;
}
.detay-json-baslik {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--text-secondary);
}
.detay-json {
  background: rgba(0, 0, 0, 0.05);
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 12px;
  font-size: 0.8rem;
  font-family: monospace;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 320px;
  overflow: auto;
  margin: 0;
}
.detay-json-ozet {
  margin: 0;
  padding: 10px 12px;
  border: 1px dashed var(--border);
  border-radius: 8px;
  font-family: monospace;
  font-size: 0.78rem;
  color: var(--text-secondary);
}
.detay-json-aciklama {
  display: block;
  margin-top: 6px;
  font-size: 0.72rem;
  color: var(--text-muted);
}

/* Mobil: arama tam genislik, detay satirlari alt alta. */
@media (max-width: 560px) {
  .arama-kutu {
    flex: 1 1 100%;
    min-width: 0;
  }
  .detay-dialog-satir {
    flex-direction: column;
    gap: 2px;
  }
  .detay-dialog-etiket {
    min-width: 0;
  }
}
</style>
