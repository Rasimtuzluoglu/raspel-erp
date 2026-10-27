<template>
  <div class="ajanda-sayfasi">
    <PageHeader
      :title="t('ajanda.title')"
      :subtitle="t('ajanda.altBaslik')"
      icon="pi pi-calendar"
    >
      <template #actions>
        <Button
          :label="t('ajanda.gorevEkle')"
          icon="pi pi-plus"
          @click="gorevDialogAc = true"
        />
        <Button
          :label="t('ajanda.hatirlatici')"
          icon="pi pi-bell"
          outlined
          @click="hatirlaticiDialogAc = true"
        />
        <Button
          icon="pi pi-refresh"
          :aria-label="t('common.refresh')"
          class="p-button-text p-button-sm"
          :loading="yukleniyor"
          @click="yukle"
        />
      </template>
    </PageHeader>

    <div class="kpi-satiri">
      <KpiKart
        :baslik="t('ajanda.kpiBugun')"
        :deger="ozet.bugunOlay"
        ikon="pi pi-sun"
        renk="#3b82f6"
        :para-birimi="false"
      />
      <KpiKart
        :baslik="t('ajanda.kpiBuAyVade')"
        :deger="ozet.buAyVadeToplam"
        ikon="pi pi-money-bill"
        renk="#f59e0b"
      />
      <KpiKart
        :baslik="t('ajanda.kpiBekleyen')"
        :deger="ozet.bekleyenGorev"
        ikon="pi pi-check-square"
        renk="#8b5cf6"
        :para-birimi="false"
      />
      <KpiKart
        :baslik="t('ajanda.kpiGecikmis')"
        :deger="ozet.gecikmisGorev"
        ikon="pi pi-exclamation-circle"
        renk="#ef4444"
        :para-birimi="false"
      />
    </div>

    <TabView v-model:active-index="aktifTab">
      <TabPanel :header="t('ajanda.tabTakvim')">
        <div class="takvim-arac">
          <div class="ay-gezinme">
            <Button
              icon="pi pi-chevron-left"
              :aria-label="t('common.previousMonth')"
              class="p-button-text p-button-sm"
              @click="ayDegistir(-1)"
            />
            <span class="ay-etiket">{{ ayEtiket }}</span>
            <Button
              icon="pi pi-chevron-right"
              :aria-label="t('common.nextMonth')"
              class="p-button-text p-button-sm"
              @click="ayDegistir(1)"
            />
            <Button
              :label="t('ajanda.bugun')"
              class="p-button-sm p-button-outlined"
              @click="buguneGit"
            />
          </div>
          <div
            class="takvim-lejant"
            role="group"
            :aria-label="t('ajanda.tipFiltre')"
          >
            <button
              v-for="c in takvimTipleri"
              :key="c.tip"
              type="button"
              class="tip-cip"
              :class="{ pasif: takvimTipFiltre !== 'TUMU' && takvimTipFiltre !== c.tip }"
              :aria-pressed="takvimTipFiltre === c.tip"
              @click="takvimTipSec(c.tip)"
            >
              <span
                class="cip-nokta"
                :class="c.sinif"
              />
              {{ c.label }}
              <strong>{{ c.sayi }}</strong>
            </button>
          </div>
        </div>

        <div
          v-if="yukleniyor && !olaylar.length"
          class="takvim-yukleniyor"
          :aria-busy="true"
        >
          <SkeletonLoader
            :count="3"
            height="42px"
          />
        </div>
        <div
          v-else
          class="takvim-grid"
          role="grid"
          :aria-label="t('ajanda.tabTakvim')"
          :aria-busy="yukleniyor"
        >
          <div
            class="gun-basliklar"
            role="row"
          >
            <span
              v-for="g in gunBasliklari"
              :key="g"
              role="columnheader"
              class="gun-baslik"
            >{{ g }}</span>
          </div>
          <div
            class="gun-hucreleri"
            role="rowgroup"
          >
            <div
              v-for="(hafta, hi) in haftalar"
              :key="hi"
              class="takvim-satir"
              role="row"
            >
              <div
                v-for="(gun, i) in hafta"
                :key="i"
                class="gun-hucre"
                :class="{ bos: !gun, bugun: gun && ayniGun(gun, bugun), secili: gun && ayniGun(gun, seciliGun) }"
                :role="gun ? 'gridcell' : 'presentation'"
                :tabindex="gun ? 0 : -1"
                :aria-label="gun ? gunAriaEtiket(gun) : undefined"
                :aria-selected="gun ? ayniGun(gun, seciliGun) : undefined"
                @click="gun && gunuSec(gun)"
                @keydown.enter.prevent="gun && gunuSec(gun)"
                @keydown.space.prevent="gun && gunuSec(gun)"
              >
                <template v-if="gun">
                  <span class="gun-no">{{ gun.getDate() }}</span>
                  <div
                    v-if="gunOlaylari(gun).length"
                    class="olay-noktalar"
                  >
                    <span
                      v-for="o in gunOlaylari(gun).slice(0, 3)"
                      :key="o.tarih + o.tip + o.baslik"
                      class="olay-nokta"
                      :class="o.tip === 'VADE' ? 'vade' : 'gorev'"
                    />
                  </div>
                </template>
              </div>
            </div>
          </div>
        </div>

        <div class="gun-detay">
          <h3 v-if="seciliGun">
            {{ seciliGun.toLocaleDateString(intlLocale, { weekday: 'long', day: 'numeric', month: 'long' }) }}
          </h3>
          <p
            class="sr-only"
            aria-live="polite"
          >
            {{ seciliGunOzeti }}
          </p>
          <div
            v-if="(!seciliOlaylar || !seciliOlaylar.length)"
            class="bos"
          >
            {{ t('ajanda.buGundeOlayYok') }}
          </div>
          <div
            v-for="o in seciliOlaylar"
            :key="o.tarih + o.tip + o.baslik"
            class="olay-karti"
          >
            <i :class="olayIkon(o.tip)" />
            <div class="olay-icerik">
              <strong>{{ o.baslik }}</strong>
              <p>{{ o.aciklama }}</p>
            </div>
            <Tag
              :value="olayTipAdi(o.tip)"
              :severity="olayTipSeverity(o.tip)"
            />
            <Button
              v-if="o.kaynakTip"
              icon="pi pi-arrow-right"
              :aria-label="t('ajanda.olayaGit')"
              :title="t('ajanda.olayaGit')"
              class="p-button-text p-button-sm"
              @click="olayaGit(o)"
            />
          </div>
        </div>
      </TabPanel>

      <TabPanel :header="t('ajanda.tabYaklasan')">
        <div class="liste-kart">
          <div class="yaklasan-arac">
            <Dropdown
              v-model="yaklasanGun"
              :options="gunSecenekleri"
              option-label="label"
              option-value="value"
              class="arac-secim"
              @change="yaklasanYukle"
            />
            <Dropdown
              v-model="yaklasanTip"
              :options="yaklasanTipSecenekleri"
              option-label="label"
              option-value="value"
              class="arac-secim"
              @change="yaklasanYukle"
            />
            <span class="arac-bosluk" />
            <Button
              :label="t('ajanda.excel')"
              icon="pi pi-file-excel"
              outlined
              size="small"
              :loading="excelYukleniyor"
              @click="excelIndir"
            />
            <Button
              icon="pi pi-refresh"
              :aria-label="t('common.refresh')"
              class="p-button-text p-button-sm"
              :loading="yaklasanYukleniyor"
              @click="yaklasanYukle"
            />
          </div>

          <div :aria-busy="yaklasanYukleniyor">
            <AppDataTable
              :value="yaklasanOlaylar"
              :loading="yaklasanYukleniyor"
              :rows="10"
              :empty-message="t('ajanda.yaklasanYok')"
              data-key="tarih"
              striped-rows
              gorunum-anahtari="ajanda_yaklasan"
            >
              <Column
                :header="t('ajanda.tarih')"
                style="width: 120px"
              >
                <template #body="{ data }">
                  {{ formatTarih(data.tarih) }}
                </template>
              </Column>
              <Column
                :header="t('ajanda.tip')"
                style="width: 140px"
              >
                <template #body="{ data }">
                  <Tag
                    :value="olayTipAdi(data.tip)"
                    :severity="olayTipSeverity(data.tip)"
                  />
                </template>
              </Column>
              <Column
                field="baslik"
                :header="t('ajanda.baslik')"
              />
              <Column
                field="aciklama"
                :header="t('ajanda.aciklama')"
              />
              <Column
                :header="t('ajanda.islem')"
                style="width: 90px"
              >
                <template #body="{ data }">
                  <Button
                    icon="pi pi-arrow-right"
                    :aria-label="t('ajanda.olayaGit')"
                    :title="t('ajanda.olayaGit')"
                    class="p-button-text p-button-sm"
                    @click="olayaGit(data)"
                  />
                </template>
              </Column>
            </AppDataTable>
          </div>
        </div>
      </TabPanel>

      <TabPanel :header="t('ajanda.tabGorevler')">
        <div class="liste-kart">
          <div class="gorev-arac">
            <span class="arac-arama">
              <i class="pi pi-search" />
              <InputText
                v-model="gorevArama"
                :placeholder="t('ajanda.gorevAra')"
                class="arama-input"
              />
            </span>
            <Dropdown
              v-model="gorevDurumFiltre"
              :options="gorevDurumSecenekleri"
              option-label="label"
              option-value="value"
              class="arac-secim"
            />
            <span class="arac-bosluk" />
            <span class="gorev-sayac">
              {{ t('ajanda.gorevSayisi', { gorunen: filtrelenmisGorevler.length, toplam: gorevler.length }) }}
            </span>
          </div>

          <EmptyState
            v-if="!filtrelenmisGorevler.length"
            icon="pi pi-check-square"
            :message="gorevler.length ? t('ajanda.filtreSonucYok') : t('ajanda.gorevYok')"
          />

          <div
            v-else
            class="gorev-liste"
          >
            <div
              v-for="g in filtrelenmisGorevler"
              :key="g.id"
              class="gorev-satir"
              :class="{ tamamlandi: g.durum === 'TAMAMLANDI', gecikmis: gorevGecikmis(g) }"
            >
              <Checkbox
                class="gorev-sec"
                :model-value="g.durum === 'TAMAMLANDI'"
                :binary="true"
                :aria-label="g.baslik"
                @update:model-value="gorevDurumDegistir(g)"
              />
              <button
                class="gorev-icerik"
                :title="t('ajanda.detay')"
                @click="gorevDetayAc(g)"
              >
                <span class="gorev-baslik">{{ g.baslik }}</span>
                <span
                  v-if="g.aciklama"
                  class="gorev-aciklama"
                >{{ g.aciklama }}</span>
              </button>
              <Tag
                class="gorev-rozet"
                :value="oncelikAdi(g.oncelik)"
                :severity="oncelikSeverity(g.oncelik)"
              />
              <span
                v-if="g.bitisTarihi"
                class="gorev-tarih"
                :class="{ 'metin-gecikmis': gorevGecikmis(g) }"
              >{{ formatTarih(g.bitisTarihi) }}</span>
              <SatirEylemleri
                class="gorev-eylem"
                :baslik="g.baslik"
                :gorunur="{ duzenle: true, sil: true }"
                :items="gorevEylemleri(g)"
                @duzenle="gorevDuzenleAc(g)"
                @sil="gorevSil(g)"
              />
            </div>
          </div>
        </div>
      </TabPanel>

      <TabPanel :header="t('ajanda.tabHatirlaticilar')">
        <div class="liste-kart">
          <div class="gorev-arac">
            <div class="gorev-ozet-baslik">
              <i class="pi pi-bell" /> {{ t('ajanda.hatirlaticilar') }} ({{ hatirlaticilar.length }})
            </div>
          </div>
          <EmptyState
            v-if="!hatirlaticilar.length"
            icon="pi pi-bell"
            :message="t('ajanda.hatirlaticiYok')"
          />
          <div
            v-else
            class="gorev-liste"
          >
            <div
              v-for="h in hatirlaticilar"
              :key="'h' + h.id"
              class="gorev-satir hatirlatici"
            >
              <i class="pi pi-bell gorev-ikon-alan" />
              <button
                class="gorev-icerik"
                :title="t('ajanda.detay')"
                @click="hatirlaticiDetayAc(h)"
              >
                <span class="gorev-baslik">{{ h.baslik }}</span>
              </button>
              <span class="gorev-tarih">{{ formatZaman(h.hatirlatmaZamani) }}</span>
              <SatirEylemleri
                :baslik="h.baslik"
                :gorunur="{ duzenle: false, sil: true }"
                @sil="hatirlaticiSil(h)"
              />
            </div>
          </div>
        </div>
      </TabPanel>
    </TabView>

    <AjandaGorevDetayDrawer
      v-model:visible="detayDrawerAc"
      :kayit="detayKayit"
      :kayit-turu="detayKayitTuru"
      @duzenle="gorevDuzenleAc"
      @tamamla="gorevDurumDegistir"
      @sil="detaySil"
    />

    <!-- Görev Ekle Dialog -->
    <Dialog
      v-model:visible="gorevDialogAc"
      :header="duzenlenenGorevId ? t('ajanda.gorevDuzenleBaslik') : t('ajanda.gorevEkle')"
      :modal="true"
      :style="{ width: '440px' }"
      :breakpoints="{ '575px': '92vw' }"
      @hide="gorevDialogSifirla"
    >
      <div class="ajanda-form">
        <div class="field">
          <label>{{ t('ajanda.baslikZorunlu') }}</label>
          <InputText
            v-model="gorevForm.baslik"
            :placeholder="t('ajanda.gorevBasligiPlaceholder')"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('ajanda.bitisTarihi') }}</label>
          <DatePicker
            v-model="gorevForm.bitisTarihi"
            show-icon
            date-format="dd.mm.yy"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('ajanda.oncelik') }}</label>
          <Dropdown
            v-model="gorevForm.oncelik"
            :options="oncelikSecenekleri"
            option-label="label"
            option-value="value"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('ajanda.aciklama') }}</label>
          <Textarea
            v-model="gorevForm.aciklama"
            rows="2"
            class="w-full"
          />
        </div>
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          class="p-button-text"
          @click="gorevDialogAc = false"
        />
        <Button
          :label="t('common.save')"
          icon="pi pi-check"
          :loading="kaydediliyor"
          @click="gorevKaydet"
        />
      </template>
    </Dialog>

    <!-- Hatırlatıcı Ekle Dialog -->
    <Dialog
      v-model:visible="hatirlaticiDialogAc"
      :header="t('ajanda.hatirlaticiEkle')"
      :modal="true"
      :style="{ width: '440px' }"
      :breakpoints="{ '575px': '92vw' }"
    >
      <div class="ajanda-form">
        <div class="field">
          <label>{{ t('ajanda.baslikZorunlu') }}</label>
          <InputText
            v-model="hatirlaticiForm.baslik"
            :placeholder="t('ajanda.hatirlaticiBasligiPlaceholder')"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('ajanda.hatirlatmaZamani') }}</label>
          <DatePicker
            v-model="hatirlaticiForm.hatirlatmaZamani"
            show-time
            hour-format="24"
            show-icon
            date-format="dd.mm.yy"
            class="w-full"
          />
        </div>
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          class="p-button-text"
          @click="hatirlaticiDialogAc = false"
        />
        <Button
          :label="t('common.save')"
          icon="pi pi-check"
          :loading="kaydediliyor"
          @click="hatirlaticiKaydet"
        />
      </template>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { ajandaAPI, excelAPI } from '../api/index.js'
import KpiKart from '../components/KpiKart.vue'
import SatirEylemleri from '../components/SatirEylemleri.vue'
import AjandaGorevDetayDrawer from '../components/AjandaGorevDetayDrawer.vue'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { formatGunAy as formatTarih, getLocalDateString } from '../utils/format.js'

const { t, locale } = useI18n()
const router = useRouter()
const intlLocale = computed(() => (locale.value === 'en' ? 'en-US' : 'tr-TR'))
const toastBildirim = useToastBildirim()
const olaylar = ref([])
const gorevler = ref([])
const hatirlaticilar = ref([])
const aktifAy = ref(new Date())
const bugun = new Date()
const seciliGun = ref(null)
const gorevDialogAc = ref(false)
const hatirlaticiDialogAc = ref(false)
const kaydediliyor = ref(false)
const yukleniyor = ref(false)
const ozet = ref({ bugunOlay: 0, buAyVadeToplam: 0, bekleyenGorev: 0, gecikmisGorev: 0 })
const yaklasanVeri = ref([])
const yaklasanOlaylar = computed(() =>
  yaklasanTip.value === 'TUMU'
    ? yaklasanVeri.value
    : yaklasanVeri.value.filter((o) => o.tip === yaklasanTip.value)
)
const yaklasanGun = ref(30)
const yaklasanTip = ref('TUMU')
const yaklasanYukleniyor = ref(false)
const excelYukleniyor = ref(false)
const duzenlenenGorevId = ref(null)
const gorevArama = ref('')
const gorevDurumFiltre = ref('TUMU')
const detayDrawerAc = ref(false)
const detayKayit = ref(null)
const detayKayitTuru = ref('gorev')

const TAB_ANAHTAR = 'raspel_ajanda_tab'
const aktifTab = ref(Number(localStorage.getItem(TAB_ANAHTAR)) || 0)
watch(aktifTab, (v) => {
  localStorage.setItem(TAB_ANAHTAR, String(v))
  if (v === 1) yaklasanYukle()
})

const gorevForm = ref({ baslik: '', bitisTarihi: null, oncelik: 'ORTA', aciklama: '' })
const hatirlaticiForm = ref({ baslik: '', hatirlatmaZamani: null })

const oncelikSecenekleri = computed(() => [
  { label: t('ajanda.dusuk'), value: 'DUSUK' },
  { label: t('ajanda.orta'), value: 'ORTA' },
  { label: t('ajanda.yuksek'), value: 'YUKSEK' }
])

const gunSecenekleri = computed(() => [
  { label: t('ajanda.gun7'), value: 7 },
  { label: t('ajanda.gun30'), value: 30 },
  { label: t('ajanda.gun60'), value: 60 },
  { label: t('ajanda.gun90'), value: 90 }
])

const yaklasanTipSecenekleri = computed(() => [
  { label: t('ajanda.tumu'), value: 'TUMU' },
  { label: t('ajanda.olayTipVade'), value: 'VADE' },
  { label: t('ajanda.olayTipGorev'), value: 'GOREV' },
  { label: t('ajanda.olayTipGorevim'), value: 'KISISEL_GOREV' },
  { label: t('ajanda.olayTipHatirlatici'), value: 'HATIRLATICI' }
])

const gorevDurumSecenekleri = computed(() => [
  { label: t('ajanda.tumDurumlar'), value: 'TUMU' },
  { label: t('ajanda.durumBekleyen'), value: 'BEKLIYOR' },
  { label: t('ajanda.durumTamamlanan'), value: 'TAMAMLANDI' },
  { label: t('ajanda.durumGecikmis'), value: 'GECIKMIS' }
])

const takvimTipFiltre = ref('TUMU')
const takvimTipleri = computed(() => {
  const say = (tip) => olaylar.value.filter((o) => o.tip === tip).length
  return [
    { tip: 'VADE', sinif: 'vade', label: t('ajanda.olayTipVade'), sayi: say('VADE') },
    { tip: 'GOREV', sinif: 'gorev', label: t('ajanda.olayTipGorev'), sayi: say('GOREV') },
    { tip: 'KISISEL_GOREV', sinif: 'kisisel', label: t('ajanda.olayTipGorevim'), sayi: say('KISISEL_GOREV') },
    { tip: 'HATIRLATICI', sinif: 'hatirlatici', label: t('ajanda.olayTipHatirlatici'), sayi: say('HATIRLATICI') }
  ]
})
const takvimTipSec = (tip) => {
  takvimTipFiltre.value = takvimTipFiltre.value === tip ? 'TUMU' : tip
}

const gorevGecikmis = (g) =>
  g?.durum !== 'TAMAMLANDI' &&
  !!g?.bitisTarihi &&
  String(g.bitisTarihi).slice(0, 10) < getLocalDateString(new Date())

const filtrelenmisGorevler = computed(() => {
  let liste = [...gorevler.value]
  const q = gorevArama.value.trim().toLowerCase()
  if (q) {
    liste = liste.filter((g) => `${g.baslik || ''} ${g.aciklama || ''}`.toLowerCase().includes(q))
  }
  if (gorevDurumFiltre.value === 'BEKLIYOR') liste = liste.filter((g) => g.durum !== 'TAMAMLANDI')
  else if (gorevDurumFiltre.value === 'TAMAMLANDI') liste = liste.filter((g) => g.durum === 'TAMAMLANDI')
  else if (gorevDurumFiltre.value === 'GECIKMIS') liste = liste.filter((g) => gorevGecikmis(g))

  const anahtarTarih = (g) => (g.bitisTarihi ? String(g.bitisTarihi).slice(0, 10) : '9999-12-31')
  return liste.sort((a, b) => {
    const ag = gorevGecikmis(a) ? 0 : 1
    const bg = gorevGecikmis(b) ? 0 : 1
    if (ag !== bg) return ag - bg
    const at = anahtarTarih(a)
    const bt = anahtarTarih(b)
    if (at !== bt) return at < bt ? -1 : 1
    return (a.baslik || '').localeCompare(b.baslik || '', 'tr')
  })
})

const gunBasliklari = computed(() => [
  t('ajanda.pzt'),
  t('ajanda.sal'),
  t('ajanda.car'),
  t('ajanda.per'),
  t('ajanda.cum'),
  t('ajanda.cmt'),
  t('ajanda.paz')
])

const ayEtiket = computed(() => aktifAy.value.toLocaleDateString(intlLocale.value, { month: 'long', year: 'numeric' }))

const gunler = computed(() => {
  const yil = aktifAy.value.getFullYear()
  const ay = aktifAy.value.getMonth()
  const ilkGun = new Date(yil, ay, 1)
  let baslangicHaftasi = ilkGun.getDay() - 1
  if (baslangicHaftasi < 0) baslangicHaftasi = 6
  const gunSayisi = new Date(yil, ay + 1, 0).getDate()
  const liste = []
  for (let i = 0; i < baslangicHaftasi; i++) liste.push(null)
  for (let d = 1; d <= gunSayisi; d++) liste.push(new Date(yil, ay, d))
  return liste
})

const haftalar = computed(() => {
  const liste = gunler.value
  const sonuc = []
  for (let i = 0; i < liste.length; i += 7) sonuc.push(liste.slice(i, i + 7))
  return sonuc
})

// "YYYY-MM-DD" (LocalDate) değerini yerel gece yarısına çevirir; UTC kaymasını önler.
const yerelTarihe = (v) => {
  if (!v) return null
  if (v instanceof Date) return v
  const s = String(v).slice(0, 10)
  const [y, m, d] = s.split('-').map(Number)
  return new Date(y, (m || 1) - 1, d || 1)
}
const ayniGun = (a, b) => a && b && a.toDateString() === b.toDateString()

const ayDegistir = (delta) => {
  aktifAy.value = new Date(aktifAy.value.getFullYear(), aktifAy.value.getMonth() + delta, 1)
  yukle()
}

const buguneGit = () => {
  aktifAy.value = new Date()
  seciliGun.value = new Date()
  yukle()
}

const gunuSec = (gun) => {
  seciliGun.value = gun
}

const gunOlaylari = (gun) =>
  olaylar.value.filter(
    (o) =>
      (takvimTipFiltre.value === 'TUMU' || o.tip === takvimTipFiltre.value) &&
      ayniGun(yerelTarihe(o.tarih), gun)
  )

const seciliOlaylar = computed(() => (seciliGun.value ? gunOlaylari(seciliGun.value) : []))

const gunAriaEtiket = (gun) => {
  const tarih = gun.toLocaleDateString(intlLocale.value, {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
    year: 'numeric'
  })
  return t('ajanda.gunAria', { tarih, sayi: gunOlaylari(gun).length })
}

const seciliGunOzeti = computed(() => {
  if (!seciliGun.value) return ''
  return gunAriaEtiket(seciliGun.value)
})

const oncelikAdi = (o) => oncelikSecenekleri.value.find((x) => x.value === o)?.label || t('ajanda.orta')
const oncelikSeverity = (o) => (o === 'YUKSEK' ? 'danger' : o === 'DUSUK' ? 'secondary' : 'warn')

const olayIkon = (tip) => {
  const map = { VADE: 'pi pi-money-bill vade-ikon', GOREV: 'pi pi-check-square gorev-ikon', KISISEL_GOREV: 'pi pi-check-square gorev-ikon', HATIRLATICI: 'pi pi-bell hatirlatici-ikon' }
  return map[tip] || 'pi pi-info-circle gorev-ikon'
}
const olayTipAdi = (tip) => {
  const map = {
    VADE: t('ajanda.olayTipVade'),
    GOREV: t('ajanda.olayTipGorev'),
    KISISEL_GOREV: t('ajanda.olayTipGorevim'),
    HATIRLATICI: t('ajanda.olayTipHatirlatici')
  }
  return map[tip] || tip
}
const olayTipSeverity = (tip) => {
  const map = { VADE: 'warn', GOREV: 'info', KISISEL_GOREV: 'success', HATIRLATICI: 'danger' }
  return map[tip] || 'info'
}


const formatZaman = (z) => (z ? new Date(z).toLocaleString(intlLocale.value, { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' }) : '')

const ozetYukle = async () => {
  try {
    const r = await ajandaAPI.ozet()
    ozet.value = { ...ozet.value, ...(r.data || {}) }
  } catch {
    /* özet alınamazsa KPI sıfır kalır */
  }
}

const yukle = async () => {
  const yil = aktifAy.value.getFullYear()
  const ay = aktifAy.value.getMonth()
  const baslangic = new Date(yil, ay, 1)
  const bitis = new Date(yil, ay + 1, 0)
  const fmt = (d) => getLocalDateString(d)
  yukleniyor.value = true
  try {
    const r = await ajandaAPI.olaylar({ baslangic: fmt(baslangic), bitis: fmt(bitis) })
    olaylar.value = r.data || []
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('ajanda.hataAjanda'))
  }
  yukleniyor.value = false
  ozetYukle()
}

const yaklasanYukle = async () => {
  yaklasanYukleniyor.value = true
  try {
    const r = await ajandaAPI.yaklasan(yaklasanGun.value)
    yaklasanVeri.value = r.data || []
  } catch (err) {
    yaklasanVeri.value = []
    toastBildirim.hata(err?.response?.data?.message || t('ajanda.hataAjanda'))
  } finally {
    yaklasanYukleniyor.value = false
  }
}

const excelIndir = async () => {
  excelYukleniyor.value = true
  try {
    const params = { gun: yaklasanGun.value }
    if (yaklasanTip.value !== 'TUMU') params.tip = yaklasanTip.value
    const res = await excelAPI.ajanda(params)
    const url = window.URL.createObjectURL(new Blob([res.data]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', `Ajanda-${getLocalDateString(new Date())}.xlsx`)
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)
    toastBildirim.basarili(t('ajanda.excelIndirildi'))
  } catch {
    toastBildirim.hata(t('ajanda.excelHata'))
  } finally {
    excelYukleniyor.value = false
  }
}

const olayaGit = (o) => {
  switch (o.kaynakTip) {
    case 'FATURA':
      if (o.kaynakNo) router.push({ path: '/faturalar', query: { no: o.kaynakNo } })
      else router.push('/faturalar')
      break
    case 'PROJE_GOREVI':
      router.push('/projeler')
      break
    case 'KISISEL_GOREV': {
      const g = gorevler.value.find((x) => x.id === o.kaynakId)
      if (g) gorevDetayAc(g)
      else aktifTab.value = 2
      break
    }
    case 'HATIRLATICI': {
      const h = hatirlaticilar.value.find((x) => x.id === o.kaynakId)
      if (h) hatirlaticiDetayAc(h)
      else aktifTab.value = 3
      break
    }
    default:
      break
  }
}

const gorevleriYukle = async () => {
  try {
    const r = await ajandaAPI.gorevler()
    gorevler.value = r.data || []
  } catch {
    gorevler.value = []
  }
}

const hatirlaticilariYukle = async () => {
  try {
    const r = await ajandaAPI.hatirlaticilar()
    hatirlaticilar.value = r.data || []
  } catch {
    hatirlaticilar.value = []
  }
}

const gorevDuzenleAc = (g) => {
  if (!g) return
  duzenlenenGorevId.value = g.id
  gorevForm.value = {
    baslik: g.baslik || '',
    bitisTarihi: g.bitisTarihi ? yerelTarihe(g.bitisTarihi) : null,
    oncelik: g.oncelik || 'ORTA',
    aciklama: g.aciklama || ''
  }
  detayDrawerAc.value = false
  gorevDialogAc.value = true
}

const gorevDialogSifirla = () => {
  duzenlenenGorevId.value = null
  gorevForm.value = { baslik: '', bitisTarihi: null, oncelik: 'ORTA', aciklama: '' }
}

const gorevEylemleri = (g) => [
  {
    etiket: t('ajanda.detay'),
    ikon: 'pi pi-eye',
    islem: () => gorevDetayAc(g)
  },
  {
    etiket: g.durum === 'TAMAMLANDI' ? t('ajanda.geriAl') : t('ajanda.tamamla'),
    ikon: g.durum === 'TAMAMLANDI' ? 'pi pi-replay' : 'pi pi-check',
    sinif: g.durum === 'TAMAMLANDI' ? '' : 'eylem-pozitif',
    islem: () => gorevDurumDegistir(g)
  }
]

const gorevDetayAc = (g) => {
  detayKayitTuru.value = 'gorev'
  detayKayit.value = g
  detayDrawerAc.value = true
  aktifTab.value = 2
}

const hatirlaticiDetayAc = (h) => {
  detayKayitTuru.value = 'hatirlatici'
  detayKayit.value = h
  detayDrawerAc.value = true
  aktifTab.value = 3
}

const detaySil = async (kayit) => {
  if (!kayit) return
  detayDrawerAc.value = false
  if (detayKayitTuru.value === 'hatirlatici') await hatirlaticiSil(kayit)
  else await gorevSil(kayit)
}

const gorevKaydet = async () => {
  if (!gorevForm.value.baslik.trim()) {
    toastBildirim.uyari(t('ajanda.gorevBasligiZorunlu'))
    return
  }
  kaydediliyor.value = true
  const govde = {
    ...gorevForm.value,
    bitisTarihi: gorevForm.value.bitisTarihi ? getLocalDateString(gorevForm.value.bitisTarihi) : null
  }
  try {
    if (duzenlenenGorevId.value) {
      await ajandaAPI.gorevGuncelle(duzenlenenGorevId.value, govde)
      toastBildirim.basarili(t('ajanda.gorevDuzenlendi'))
    } else {
      await ajandaAPI.gorevOlustur(govde)
      toastBildirim.basarili(t('ajanda.gorevOlusturuldu'))
    }
    gorevDialogAc.value = false
    gorevDialogSifirla()
    gorevleriYukle()
    yukle()
    yaklasanYukle()
  } catch (err) {
    toastBildirim.hata(
      err?.response?.data?.message ||
        t(duzenlenenGorevId.value ? 'ajanda.gorevGuncellenemedi' : 'ajanda.gorevOlusturulamadi')
    )
  } finally {
    kaydediliyor.value = false
  }
}

// LocalDateTime backend'e yerel saat diliminde gonderilir (toISOString UTC'ye
// kaydirip hatirlaticinin erken calismasina yol acardi).
const yerelDateTimeIso = (d) => {
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}T${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

const hatirlaticiKaydet = async () => {
  if (!hatirlaticiForm.value.baslik.trim() || !hatirlaticiForm.value.hatirlatmaZamani) {
    toastBildirim.uyari(t('ajanda.baslikZamanZorunlu'))
    return
  }
  kaydediliyor.value = true
  try {
    await ajandaAPI.hatirlaticiOlustur({
      baslik: hatirlaticiForm.value.baslik,
      hatirlatmaZamani: yerelDateTimeIso(hatirlaticiForm.value.hatirlatmaZamani)
    })
    toastBildirim.basarili(t('ajanda.hatirlaticiOlusturuldu'))
    hatirlaticiDialogAc.value = false
    hatirlaticiForm.value = { baslik: '', hatirlatmaZamani: null }
    hatirlaticilariYukle()
    yukle()
    yaklasanYukle()
  } catch (err) {
    const alanHatalari = err?.response?.data?.errors
    const detay = alanHatalari
      ? Object.values(alanHatalari).join(' ')
      : err?.response?.data?.message || t('ajanda.hatirlaticiOlusturulamadi')
    toastBildirim.hata(detay)
  } finally {
    kaydediliyor.value = false
  }
}

const gorevDurumDegistir = async (g) => {
  const yeniDurum = g.durum === 'TAMAMLANDI' ? 'BEKLIYOR' : 'TAMAMLANDI'
  try {
    if (g.durum === 'TAMAMLANDI') {
      await ajandaAPI.gorevGuncelle(g.id, { durum: 'BEKLIYOR' })
    } else {
      await ajandaAPI.gorevTamamla(g.id)
    }
    if (detayKayitTuru.value === 'gorev' && detayKayit.value?.id === g.id) {
      detayKayit.value = { ...detayKayit.value, durum: yeniDurum }
    }
    gorevleriYukle()
    yukle()
    yaklasanYukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('ajanda.islemBasarisiz'))
  }
}

const gorevSil = async (g) => {
  try {
    await ajandaAPI.gorevSil(g.id)
    gorevleriYukle()
    yukle()
    yaklasanYukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('ajanda.silinemedi'))
  }
}

const hatirlaticiSil = async (h) => {
  try {
    await ajandaAPI.hatirlaticiSil(h.id)
    hatirlaticilariYukle()
    yukle()
    yaklasanYukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('ajanda.silinemedi'))
  }
}

onMounted(() => {
  seciliGun.value = new Date()
  yukle()
  gorevleriYukle()
  hatirlaticilariYukle()
  yaklasanYukle()
})
</script>

<style scoped>
.ajanda-sayfasi {
  padding: 0;
}
.kpi-satiri {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}
.takvim-arac {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}
.liste-kart {
  padding: 16px 20px;
  border-radius: 12px;
  background: var(--bg-card);
  border: 1px solid var(--border);
}
.yaklasan-arac {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}
.yaklasan-arac .arac-secim {
  min-width: 170px;
}
.yaklasan-arac .arac-bosluk {
  flex: 1;
}
.ay-gezinme {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.ay-etiket {
  font-weight: 700;
  min-width: 120px;
  text-align: center;
  text-transform: capitalize;
}
.takvim-grid {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  overflow: hidden;
}
.takvim-satir {
  display: contents;
}
.takvim-yukleniyor {
  padding: 16px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  margin-bottom: 12px;
}
.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0 0 0 0);
  white-space: nowrap;
  border: 0;
}
.gun-basliklar {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  background: var(--bg-primary);
}
.gun-baslik {
  padding: 8px;
  text-align: center;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
}
.gun-hucreleri {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
}
.gun-hucre {
  min-height: 70px;
  border: 1px solid var(--border);
  padding: 6px;
  cursor: pointer;
}
.gun-hucre.bos {
  background: rgba(0, 0, 0, 0.02);
  cursor: default;
}
.gun-hucre.bugun {
  background: rgba(59, 130, 246, 0.08);
}
.gun-hucre.secili {
  outline: 2px solid var(--accent);
}
.gun-no {
  font-size: 13px;
  font-weight: 600;
}
.olay-noktalar {
  display: flex;
  gap: 4px;
  margin-top: 6px;
}
.olay-nokta {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}
.olay-nokta.vade {
  background: #f59e0b;
}
.olay-nokta.gorev {
  background: var(--accent);
}
.gun-detay {
  margin-top: 16px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 16px;
}
.gun-detay h3 {
  margin: 0 0 12px;
  text-transform: capitalize;
}
.bos {
  color: var(--text-muted);
  padding: 12px 0;
}
.olay-karti {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid var(--border);
}
.olay-karti:last-child {
  border-bottom: none;
}
.olay-karti i {
  font-size: 18px;
}
.olay-icerik {
  flex: 1;
  min-width: 0;
}
.olay-icerik strong {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.olay-icerik p {
  margin: 2px 0 0;
  font-size: 12px;
  color: var(--text-muted);
  word-break: break-word;
}
.takvim-lejant {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.tip-cip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border-radius: 999px;
  border: 1px solid var(--border);
  background: var(--bg-card);
  color: var(--text-secondary);
  font-size: 12px;
  cursor: pointer;
  transition: opacity 0.15s ease, border-color 0.15s ease;
}
.tip-cip:hover {
  border-color: var(--accent);
  color: var(--accent);
}
.tip-cip.pasif {
  opacity: 0.45;
}
.tip-cip[aria-pressed='true'] {
  border-color: var(--accent);
  background: var(--accent-soft);
  color: var(--accent);
}
.tip-cip strong {
  font-weight: 700;
}
.cip-nokta {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--text-muted);
}
.cip-nokta.vade {
  background: #f59e0b;
}
.cip-nokta.gorev {
  background: var(--accent);
}
.cip-nokta.kisisel {
  background: #22c55e;
}
.cip-nokta.hatirlatici {
  background: #ef4444;
}
.vade-ikon {
  color: #f59e0b;
}
.gorev-ikon {
  color: var(--accent);
}
.hatirlatici-ikon {
  color: #ef4444;
}
.gorev-ozet {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 14px 16px;
  margin-bottom: 16px;
}
.gorev-arac {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 10px;
}
.gorev-arac .arac-bosluk {
  flex: 1;
}
.arac-arama {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-width: 200px;
  max-width: 320px;
  flex: 1;
  background: var(--bg-muted);
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 0 8px;
}
.arac-arama > i {
  color: var(--text-muted);
  font-size: 13px;
}
.arac-arama .arama-input {
  border: none;
  background: transparent;
  box-shadow: none;
}
.gorev-arac .arac-secim {
  min-width: 160px;
}
.gorev-sayac {
  font-size: 12px;
  color: var(--text-muted);
  white-space: nowrap;
}
.gorev-liste {
  display: flex;
  flex-direction: column;
}
.gorev-icerik {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
  border: none;
  background: transparent;
  color: inherit;
  text-align: left;
  cursor: pointer;
  padding: 2px 0;
}
.gorev-icerik:hover .gorev-baslik {
  color: var(--accent);
}
.metin-gecikmis {
  color: var(--danger, #ef4444) !important;
  font-weight: 600;
}
.gorev-ozet-baslik {
  font-size: 13px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 6px;
}
.gorev-ozet-baslik i {
  color: var(--accent);
}
.gorev-satir {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 0;
  border-bottom: 1px solid var(--border);
  font-size: 13px;
}
.gorev-satir:last-child {
  border-bottom: none;
}
.gorev-satir.tamamlandi .gorev-baslik {
  text-decoration: line-through;
  color: var(--text-muted);
}
.gorev-satir.hatirlatici {
  color: var(--text-secondary);
}
.gorev-baslik {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.gorev-icerik .gorev-baslik {
  flex: none;
  width: 100%;
}
.gorev-aciklama {
  flex: 0 1 auto;
  max-width: 40%;
  font-size: 11px;
  color: var(--text-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.gorev-icerik .gorev-aciklama {
  max-width: 100%;
}
.gorev-tarih {
  font-size: 11px;
  color: var(--text-muted);
  white-space: nowrap;
}
.gorev-sil {
  background: transparent;
  border: none;
  color: var(--text-muted);
  cursor: pointer;
  font-size: 13px;
  padding: 2px 4px;
  border-radius: 4px;
}
.gorev-sil:hover {
  color: #ef4444;
  background: rgba(239, 68, 68, 0.1);
}
.ajanda-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.ajanda-form .field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.ajanda-form .field label {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
}
.ajanda-form .w-full {
  width: 100%;
}
.olay-karti div {
  flex: 1;
}
.olay-karti strong {
  font-size: 14px;
}
.olay-karti p {
  margin: 2px 0 0;
  font-size: 12px;
  color: var(--text-secondary);
  white-space: pre-wrap;
  word-break: break-word;
}

@media (max-width: 900px) {
  .ay-gezinme {
    justify-content: space-between;
  }
  .takvim-grid {
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
  }
  .gun-basliklar,
  .gun-hucreleri {
    min-width: 560px;
  }
  .gun-hucre {
    min-height: 56px;
  }
  .yaklasan-arac .arac-bosluk,
  .gorev-arac .arac-bosluk {
    display: none;
  }
}

@media (max-width: 640px) {
  .kpi-satiri {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 8px;
  }
  .liste-kart {
    padding: 12px;
    border-radius: 10px;
  }
  .yaklasan-arac,
  .gorev-arac {
    gap: 8px;
  }
  .arac-arama {
    flex: 1 1 100%;
    max-width: none;
  }
  .yaklasan-arac .arac-secim,
  .gorev-arac .arac-secim {
    flex: 1 1 140px;
    min-width: 0;
  }
  .gorev-sayac {
    display: none;
  }
  .takvim-lejant {
    overflow-x: auto;
    flex-wrap: nowrap;
    padding-bottom: 2px;
    -webkit-overflow-scrolling: touch;
  }
  .tip-cip {
    flex-shrink: 0;
    min-height: 32px;
  }
  .gorev-satir {
    display: grid;
    grid-template-columns: auto minmax(0, 1fr) auto;
    grid-template-areas:
      'sec icerik eylem'
      'sec rozet rozet'
      'sec tarih tarih';
    align-items: center;
    gap: 6px 10px;
    padding: 10px 12px;
    border: 1px solid var(--border);
    border-radius: 10px;
    margin-bottom: 8px;
    font-size: 13px;
  }
  .gorev-satir:last-child {
    margin-bottom: 0;
  }
  .gorev-sec {
    grid-area: sec;
    align-self: start;
  }
  .gorev-ikon-alan {
    grid-area: sec;
    align-self: start;
  }
  .gorev-icerik {
    grid-area: icerik;
  }
  .gorev-rozet {
    grid-area: rozet;
    justify-self: start;
  }
  .gorev-tarih {
    grid-area: tarih;
  }
  .gorev-eylem {
    grid-area: eylem;
    align-self: start;
  }
  .olay-karti {
    flex-wrap: wrap;
  }
  .olay-icerik {
    flex: 1 1 60%;
  }
  .gun-basliklar,
  .gun-hucreleri {
    min-width: 420px;
  }
  .gun-hucre {
    min-height: 44px;
    padding: 4px;
  }
  .gun-no {
    font-size: 12px;
  }
  .ay-etiket {
    font-size: 14px;
  }
}

@media (max-width: 600px) {
  .gun-basliklar,
  .gun-hucreleri {
    min-width: 380px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .tip-cip,
  .gorev-icerik .gorev-baslik {
    transition: none;
  }
}
</style>
