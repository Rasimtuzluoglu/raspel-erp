<template>
  <div class="kategoriler-container">
    <PageHeader
      :title="t('kategoriler.title')"
      :subtitle="t('kategoriler.aciklama')"
      icon="pi pi-tags"
    >
      <template #actions>
        <Button
          :label="t('kategoriler.yeniKategori')"
          icon="pi pi-plus"
          @click="yeniAc()"
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
        :baslik="t('kategoriler.kpiToplam')"
        :deger="ozet.toplam"
        ikon="pi pi-tags"
        renk="#3b82f6"
        :para-birimi="false"
      />
      <KpiKart
        :baslik="t('kategoriler.kpiAktif')"
        :deger="ozet.aktif"
        ikon="pi pi-check-circle"
        renk="#10b981"
        :para-birimi="false"
      />
      <KpiKart
        :baslik="t('kategoriler.kpiPasif')"
        :deger="ozet.pasif"
        ikon="pi pi-times-circle"
        renk="#64748b"
        :para-birimi="false"
      />
      <KpiKart
        :baslik="t('kategoriler.kpiAltKategori')"
        :deger="ozet.altKategori"
        ikon="pi pi-sitemap"
        renk="#f59e0b"
        :para-birimi="false"
      />
    </div>

    <!-- Kapsam sekmeleri: Kasa (Gelir/Gider) · Stok · Masraf · Bütçe -->
    <div
      class="kapsam-cubugu"
      role="tablist"
      :aria-label="t('kategoriler.kapsamSecimi')"
      @keydown="tuslaSekme($event, anaKodlar, aktifAnaSekme, kapsamSec)"
    >
      <button
        v-for="s in anaSekmeler"
        :key="s.kod"
        type="button"
        role="tab"
        class="kapsam-sekme"
        :class="{ aktif: aktifAnaSekme === s.kod }"
        :aria-selected="aktifAnaSekme === s.kod"
        :tabindex="aktifAnaSekme === s.kod ? 0 : -1"
        :data-kapsam="s.kod"
        @click="kapsamSec(s.kod)"
      >
        <i
          :class="s.ikon"
          aria-hidden="true"
        />
        {{ s.ad }}
      </button>
    </div>

    <div
      v-if="kasaMi"
      class="kasa-alt-cubugu"
      role="tablist"
      :aria-label="t('kategoriler.kasaTuru')"
      @keydown="tuslaSekme($event, kasaKodlar, kapsam, kapsamSec)"
    >
      <button
        v-for="k in kasaTurleri"
        :key="k.kod"
        type="button"
        role="tab"
        class="kasa-alt-sekme"
        :class="{ aktif: kapsam === k.kod }"
        :aria-selected="kapsam === k.kod"
        :tabindex="kapsam === k.kod ? 0 : -1"
        @click="kapsamSec(k.kod)"
      >
        <i
          :class="k.ikon"
          aria-hidden="true"
        />
        {{ k.ad }}
      </button>
    </div>

    <div class="kategori-arac">
      <span class="arama-kutu">
        <i
          class="pi pi-search"
          aria-hidden="true"
        />
        <InputText
          v-model="arama"
          :placeholder="t('kategoriler.aramaPlaceholder')"
          :aria-label="t('kategoriler.aramaPlaceholder')"
          class="arama-input"
          @input="aramaGirdi"
        />
      </span>

      <div
        class="durum-cipleri"
        role="group"
        :aria-label="t('kategoriler.durumFiltresi')"
      >
        <button
          v-for="f in durumFiltreleri"
          :key="f.deger"
          type="button"
          class="durum-cip"
          :class="{ aktif: durumFiltre === f.deger }"
          :aria-pressed="durumFiltre === f.deger"
          @click="durumSec(f.deger)"
        >
          <i
            :class="f.ikon"
            aria-hidden="true"
          />
          {{ f.ad }}
        </button>
      </div>

      <Dropdown
        v-model="siralama"
        :options="siralamaSecenekleri"
        option-label="label"
        option-value="value"
        class="siralama-secim"
        :aria-label="t('kategoriler.siralama')"
      />

      <span class="arac-bosluk" />
      <span class="sonuc-rozet">{{ t('kategoriler.sonucSayisi', { n: filtreliUstler.length }) }}</span>
      <span
        class="sr-only"
        role="status"
        aria-live="polite"
      >{{ t('kategoriler.sonucSayisi', { n: filtreliUstler.length }) }}</span>
    </div>

    <!-- Agac tablo: ust kategoriler + ac/kapa ile girintili alt kategoriler -->
    <div
      v-if="tabloVar"
      class="kategori-masa"
      :aria-busy="yukleniyor"
    >
      <AppDataTable
        :value="tabloSatirlari"
        :paginator="false"
        :rows="sayfaBoyutu"
        striped-rows
        :loading="yukleniyor"
        :empty-message="filtreVar ? t('kategoriler.filtreBos') : t('kategoriler.empty')"
        gorunum-anahtari="kategoriler"
      >
        <Column
          field="ad"
          :header="t('kategoriler.kategoriAdi')"
          style="min-width: 260px"
        >
          <template #body="{ data }">
            <div
              class="kategori-hucre"
              :class="{ alt: data.derinlik === 1 }"
            >
              <button
                v-if="data.derinlik === 0 && altlar(data).length"
                type="button"
                class="agac-dugme"
                :aria-expanded="acikMi(data.id)"
                :aria-label="acikMi(data.id) ? t('kategoriler.daralt') : t('kategoriler.genislet')"
                @click="acKapa(data.id)"
              >
                <i
                  :class="acikMi(data.id) ? 'pi pi-chevron-down' : 'pi pi-chevron-right'"
                  aria-hidden="true"
                />
              </button>
              <span
                v-else
                class="agac-bosluk"
                aria-hidden="true"
              />
              <span
                class="renk-nokta"
                :style="{ background: data.renk || 'var(--text-muted)' }"
                aria-hidden="true"
              />
              <i
                v-if="data.ikon"
                :class="data.ikon"
                class="kategori-ikon"
                :style="{ color: data.renk || 'var(--text-secondary)' }"
                aria-hidden="true"
              />
              <span
                class="kategori-ad"
                :title="data.ad"
              >{{ data.ad }}</span>
              <span
                v-if="data.derinlik === 1 && data.ustKategoriAd"
                class="ust-ad"
              >· {{ data.ustKategoriAd }}</span>
              <Tag
                v-if="data.derinlik === 0 && altlar(data).length"
                :value="t('kategoriler.altSayisi', { n: altlar(data).length })"
                severity="secondary"
                class="alt-rozet"
              />
            </div>
          </template>
        </Column>

        <Column
          field="sira"
          :header="t('kategoriler.sira')"
          style="width: 80px"
        >
          <template #body="{ data }">
            <span class="sira-deger">{{ data.sira ?? 0 }}</span>
          </template>
        </Column>

        <Column
          field="kullanimSayisi"
          :header="t('kategoriler.kullanim')"
          style="width: 110px"
        >
          <template #body="{ data }">
            <button
              type="button"
              class="kullanim-rozet"
              :class="{ bos: !data.kullanimSayisi }"
              :aria-label="t('kategoriler.kullanimGor', { ad: data.ad })"
              :title="t('kategoriler.kullanimGor', { ad: data.ad })"
              @click="kullanimAc(data)"
            >
              <i
                class="pi pi-link"
                aria-hidden="true"
              />
              {{ data.kullanimSayisi || 0 }}
            </button>
          </template>
        </Column>

        <Column
          field="aktif"
          :header="t('common.status')"
          style="width: 96px"
        >
          <template #body="{ data }">
            <Tag
              :value="data.aktif === false ? t('status.passive') : t('status.active')"
              :severity="data.aktif === false ? 'danger' : 'success'"
            />
          </template>
        </Column>

        <Column
          :header="t('common.actions')"
          style="width: 220px"
        >
          <template #body="{ data }">
            <div
              class="satir-aksiyon"
              role="group"
              :aria-label="t('kategoriler.satirAksiyonlari', { ad: data.ad })"
            >
              <Button
                v-for="a in satirAksiyonlari(data)"
                :key="a.etiket"
                :icon="a.ikon"
                :severity="a.tehlike ? 'danger' : undefined"
                :disabled="a.devreDisi"
                :title="a.baslik"
                :aria-label="a.baslik || a.etiket"
                class="p-button-rounded p-button-text p-button-sm"
                @click.stop="a.islem()"
              />
            </div>
          </template>
        </Column>
      </AppDataTable>

      <div
        v-if="toplamSayfa > 1"
        class="sayfalama"
        role="navigation"
        :aria-label="t('kategoriler.sayfalama')"
      >
        <Button
          icon="pi pi-angle-left"
          class="p-button-text p-button-sm"
          :aria-label="t('kategoriler.oncekiSayfa')"
          :disabled="sayfa === 0"
          @click="oncekiSayfa"
        />
        <span class="sayfa-bilgi">{{ t('kategoriler.sayfaBilgisi', { sayfa: sayfa + 1, toplam: toplamSayfa }) }}</span>
        <Button
          icon="pi pi-angle-right"
          class="p-button-text p-button-sm"
          :aria-label="t('kategoriler.sonrakiSayfa')"
          :disabled="sayfa >= toplamSayfa - 1"
          @click="sonrakiSayfa"
        />
        <span class="sayfa-toplam">{{ t('kategoriler.toplamKayit', { n: filtreliUstler.length }) }}</span>
      </div>
    </div>

    <ListeDurumu
      v-else
      :yukleniyor="yukleniyor"
      :hata="hata"
      :bos="!yukleniyor && !hata && tabloSatirlari.length === 0"
      :bos-mesaj="filtreVar ? t('kategoriler.filtreBos') : t('kategoriler.empty')"
      :bos-ipucu="filtreVar ? '' : t('kategoriler.emptyHint')"
      :eylem-etiketi="filtreVar ? '' : t('kategoriler.yeniKategori')"
      :yeniden-dene="yukle"
      @eylem="yeniAc()"
    />

    <!-- Mobil kart gorunumu -->
    <div
      v-if="tabloVar"
      class="kategori-mobil"
      :aria-busy="yukleniyor"
    >
      <article
        v-for="k in tabloSatirlari"
        :key="k.id"
        class="kategori-mobil-kart"
        :class="{ alt: k.derinlik === 1 }"
      >
        <div class="kmk-ust">
          <span
            class="renk-nokta"
            :style="{ background: k.renk || 'var(--text-muted)' }"
            aria-hidden="true"
          />
          <i
            v-if="k.ikon"
            :class="k.ikon"
            :style="{ color: k.renk || 'var(--text-secondary)' }"
            aria-hidden="true"
          />
          <span class="kmk-ad">{{ k.ad }}</span>
          <Tag
            :value="k.aktif === false ? t('status.passive') : t('status.active')"
            :severity="k.aktif === false ? 'danger' : 'success'"
          />
        </div>
        <div class="kmk-satir">
          <span>{{ t('kategoriler.sira') }}: {{ k.sira ?? 0 }}</span>
          <span>{{ t('kategoriler.kullanim') }}: {{ k.kullanimSayisi || 0 }}</span>
        </div>
        <div class="satir-aksiyon">
          <Button
            v-for="a in satirAksiyonlari(k)"
            :key="a.etiket"
            :icon="a.ikon"
            :severity="a.tehlike ? 'danger' : undefined"
            :disabled="a.devreDisi"
            :title="a.baslik"
            :aria-label="a.baslik || a.etiket"
            class="p-button-rounded p-button-text p-button-sm"
            @click.stop="a.islem()"
          />
        </div>
      </article>
    </div>

    <!-- Sistem kategorileri (yalniz Masraf): salt-okunur personel masraf kodlari -->
    <section
      v-if="kapsam === 'MASRAF'"
      class="sistem-kart"
      :aria-label="t('kategoriler.sistemKategorileri')"
    >
      <header class="sistem-baslik">
        <i
          class="pi pi-lock"
          aria-hidden="true"
        />
        <div class="sistem-metin">
          <strong>{{ t('kategoriler.sistemKategorileri') }}</strong>
          <span>{{ t('kategoriler.sistemKategorileriAciklama') }}</span>
        </div>
        <Tag
          :value="t('kategoriler.sistemSaltOkunur')"
          severity="secondary"
        />
      </header>
      <div
        v-if="sistemYukleniyor"
        class="sistem-durum"
        role="status"
        aria-live="polite"
      >
        <i class="pi pi-spin pi-spinner" /> {{ t('common.loading') }}
      </div>
      <ul
        v-else-if="sistemKategorileri.length"
        class="sistem-liste"
      >
        <li
          v-for="s in sistemKategorileri"
          :key="s.kod"
          class="sistem-satir"
        >
          <code class="sistem-kod">{{ s.kod }}</code>
          <span class="sistem-ad">{{ s.ad }}</span>
          <Tag
            v-if="s.kapsam"
            :value="s.kapsam"
            severity="secondary"
          />
        </li>
      </ul>
      <p
        v-else
        class="sistem-bos"
      >
        {{ t('kategoriler.sistemKategoriYok') }}
      </p>
    </section>

    <KategoriFormDialog
      v-model:visible="formDialog"
      :kapsam="kapsam"
      :kategori="formKategori"
      :ust-kategoriler="ustKategoriler"
      :varsayilan-ust-kategori-id="formUstId"
      :kaydediliyor="kaydediliyor"
      @kaydet="kaydet"
    />

    <KategoriKullanimDrawer
      v-model:visible="kullanimDrawer"
      :kullanim="kullanimVeri"
      :yukleniyor="kullanimYukleniyor"
    />

    <!-- Birlestirme: kaynak arsivlenir, baglar hedefe tasinir -->
    <Dialog
      v-model:visible="birlestirDialog"
      :header="t('kategoriler.birlestirBaslik')"
      modal
      :style="{ width: '520px' }"
      :breakpoints="{ '575px': '92vw' }"
    >
      <div
        v-if="birlestirKaynak"
        class="birlestir"
      >
        <p class="birlestir-aciklama">
          {{ t('kategoriler.birlestirAciklama') }}
        </p>

        <div class="birlestir-kaynak">
          <span class="birlestir-etiket">{{ t('kategoriler.kaynakKategori') }}</span>
          <span class="birlestir-ad">
            <span
              class="renk-nokta"
              :style="{ background: birlestirKaynak.renk || 'var(--text-muted)' }"
              aria-hidden="true"
            />
            {{ birlestirKaynak.ad }}
          </span>
          <Tag
            :value="t('kategoriler.kullanimToplam') + ': ' + (birlestirKaynak.kullanimSayisi || 0)"
            severity="secondary"
          />
        </div>

        <div class="alan">
          <label for="birlestir-hedef">{{ t('kategoriler.hedefKategori') }}</label>
          <Dropdown
            id="birlestir-hedef"
            v-model="birlestirHedefId"
            :options="birlestirHedefleri"
            option-label="ad"
            option-value="id"
            :placeholder="t('kategoriler.hedefSecin')"
            class="w-full"
          />
        </div>

        <div
          v-if="birlestirHedef"
          class="birlestir-ozet"
        >
          <span>{{ t('kategoriler.birlestirHedefOzet', { ad: birlestirHedef.ad, n: birlestirHedef.kullanimSayisi || 0 }) }}</span>
        </div>

        <p
          class="birlestir-uyari"
          role="alert"
        >
          <i
            class="pi pi-exclamation-triangle"
            aria-hidden="true"
          />
          {{ t('kategoriler.birlestirUyari') }}
        </p>
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          icon="pi pi-times"
          class="p-button-text"
          @click="birlestirDialog = false"
        />
        <Button
          :label="t('kategoriler.birlestir')"
          icon="pi pi-clone"
          severity="danger"
          :loading="birlestiriyor"
          :disabled="!birlestirHedef || birlestiriyor"
          @click="birlestirKaydet"
        />
      </template>
    </Dialog>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useConfirm } from 'primevue/useconfirm'
import { useKategoriStore } from '../stores/kategoriStore.js'
import { useAuthStore } from '../stores/authStore.js'
import PageHeader from '../components/PageHeader.vue'
import KpiKart from '../components/KpiKart.vue'
import AppDataTable from '../components/AppDataTable.vue'
import ListeDurumu from '../components/ListeDurumu.vue'
import KategoriFormDialog from '../components/KategoriFormDialog.vue'
import KategoriKullanimDrawer from '../components/KategoriKullanimDrawer.vue'

const { t } = useI18n()
const toastBildirim = useToastBildirim()
const confirm = useConfirm()
const kategoriStore = useKategoriStore()
const authStore = useAuthStore()

const KAPSAM_ANAHTAR = 'raspel_kategoriler_kapsam'
const GECERLI_KAPSAM = ['KASA_GELIR', 'KASA_GIDER', 'STOK', 'MASRAF', 'BUTCE']

const isAdmin = computed(() => authStore.isAdmin === true)

// ---- Hafiza: son secilen kapsam ----
const baslangicKapsam = () => {
  const kayitli = localStorage.getItem(KAPSAM_ANAHTAR)
  return GECERLI_KAPSAM.includes(kayitli) ? kayitli : 'KASA_GELIR'
}
const kapsam = ref(baslangicKapsam())

const arama = ref('')
const durumFiltre = ref('TUMU')
const siralama = ref('sira')
const sayfa = ref(0)
const sayfaBoyutu = 25
const yukleniyor = ref(false)
const hata = ref(null)
const acikKumeler = ref([])

const formDialog = ref(false)
const formKategori = ref(null)
const formUstId = ref(null)
const kaydediliyor = ref(false)

const kullanimDrawer = ref(false)
const kullanimVeri = ref(null)
const kullanimYukleniyor = ref(false)

const birlestirDialog = ref(false)
const birlestirKaynak = ref(null)
const birlestirHedefId = ref(null)
const birlestiriyor = ref(false)

const sistemKategorileri = computed(() => kategoriStore.sistemKategoriler || [])
const sistemYukleniyor = ref(false)
const sistemYuklendi = ref(false)

// ---- Kapsam sekmeleri ----
const anaSekmeler = computed(() => [
  { kod: 'KASA', ad: t('kategoriler.kapsamKasa'), ikon: 'pi pi-wallet' },
  { kod: 'STOK', ad: t('kategoriler.kapsamStok'), ikon: 'pi pi-box' },
  { kod: 'MASRAF', ad: t('kategoriler.kapsamMasraf'), ikon: 'pi pi-receipt' },
  { kod: 'BUTCE', ad: t('kategoriler.kapsamButce'), ikon: 'pi pi-chart-pie' }
])
const anaKodlar = computed(() => anaSekmeler.value.map((s) => s.kod))

const kasaTurleri = computed(() => [
  { kod: 'KASA_GELIR', ad: t('kategoriler.gelir'), ikon: 'pi pi-arrow-down-left' },
  { kod: 'KASA_GIDER', ad: t('kategoriler.gider'), ikon: 'pi pi-arrow-up-right' }
])
const kasaKodlar = computed(() => kasaTurleri.value.map((k) => k.kod))

const kasaMi = computed(() => kapsam.value === 'KASA_GELIR' || kapsam.value === 'KASA_GIDER')
const aktifAnaSekme = computed(() => (kasaMi.value ? 'KASA' : kapsam.value))

const kapsamSec = (kod) => {
  if (kod === 'KASA') {
    if (!kasaMi.value) kapsam.value = 'KASA_GELIR'
    return
  }
  if (kapsam.value !== kod) kapsam.value = kod
}

/** role=tablist klavye destegi (ok tuslari / Home / End). */
const tuslaSekme = (olay, kodlar, aktifKod, secFn) => {
  const indeks = kodlar.indexOf(aktifKod)
  if (indeks < 0) return
  let hedef = -1
  if (olay.key === 'ArrowRight' || olay.key === 'ArrowDown') hedef = (indeks + 1) % kodlar.length
  else if (olay.key === 'ArrowLeft' || olay.key === 'ArrowUp') hedef = (indeks - 1 + kodlar.length) % kodlar.length
  else if (olay.key === 'Home') hedef = 0
  else if (olay.key === 'End') hedef = kodlar.length - 1
  if (hedef < 0) return
  olay.preventDefault()
  const dugumler = [...(olay.currentTarget?.parentElement?.querySelectorAll('[role="tab"]') || [])]
  secFn(kodlar[hedef])
  nextTick(() => dugumler[hedef]?.focus?.())
}

// ---- Filtre / siralama ----
const durumFiltreleri = computed(() => [
  { deger: 'TUMU', ad: t('kategoriler.filtreTumu'), ikon: 'pi pi-list' },
  { deger: 'AKTIF', ad: t('kategoriler.filtreAktif'), ikon: 'pi pi-check-circle' },
  { deger: 'PASIF', ad: t('kategoriler.filtrePasif'), ikon: 'pi pi-times-circle' }
])

const siralamaSecenekleri = computed(() => [
  { label: t('kategoriler.siralaSiraya'), value: 'sira' },
  { label: t('kategoriler.siralaAdArtan'), value: 'ad-asc' },
  { label: t('kategoriler.siralaAdAzalan'), value: 'ad-desc' },
  { label: t('kategoriler.siralaKullanim'), value: 'kullanim-desc' }
])

const filtreVar = computed(() => !!arama.value.trim() || durumFiltre.value !== 'TUMU')

const aktifParam = () => {
  if (durumFiltre.value === 'AKTIF') return true
  if (durumFiltre.value === 'PASIF') return false
  return undefined
}

const durumUygun = (k) => {
  if (!k) return false
  if (durumFiltre.value === 'AKTIF') return k.aktif !== false
  if (durumFiltre.value === 'PASIF') return k.aktif === false
  return true
}

const karsilastir = (a, b) => {
  if (siralama.value === 'ad-asc') return String(a.ad || '').localeCompare(String(b.ad || ''), 'tr')
  if (siralama.value === 'ad-desc') return String(b.ad || '').localeCompare(String(a.ad || ''), 'tr')
  if (siralama.value === 'kullanim-desc') return Number(b.kullanimSayisi || 0) - Number(a.kullanimSayisi || 0)
  return (Number(a.sira) || 0) - (Number(b.sira) || 0) || String(a.ad || '').localeCompare(String(b.ad || ''), 'tr')
}

// ---- Agac ----
const agac = computed(() => {
  const ham = Array.isArray(kategoriStore.agac) ? kategoriStore.agac : []
  const idKumesi = new Set(ham.map((k) => k.id))
  // Filtre nedeniyle ust kategorisi sonucta olmayan dugumler kok olarak gelir.
  const kokler = ham.filter((k) => !k.ustKategoriId || !idKumesi.has(k.ustKategoriId))
  if (ham.some((k) => Array.isArray(k.altKategoriler))) {
    return kokler.map((k) => ({ ...k, altKategoriler: Array.isArray(k.altKategoriler) ? k.altKategoriler : [] }))
  }
  // Duz liste geldiyse (bazi kurulumlar) istemci tarafinda agaca cevir.
  return kokler.map((k) => ({
    ...k,
    altKategoriler: ham.filter((c) => c.ustKategoriId === k.id)
  }))
})

const altlar = (ust) => (Array.isArray(ust?.altKategoriler) ? ust.altKategoriler : []).filter(durumUygun).slice().sort(karsilastir)

const eslesir = (k, q) => String(k?.ad || '').toLocaleLowerCase('tr-TR').includes(q)

const filtreliUstler = computed(() => {
  const q = arama.value.trim().toLocaleLowerCase('tr-TR')
  const sonuc = []
  for (const ust of agac.value) {
    const altKategoriler = altlar(ust)
    const kendiUygun = durumUygun(ust)
    let altEslesen = altKategoriler
    if (q) {
      const kendiEsles = eslesir(ust, q)
      altEslesen = altKategoriler.filter((c) => eslesir(c, q))
      if (!kendiEsles && !altEslesen.length) continue
    }
    if (!kendiUygun && !altEslesen.length) continue
    sonuc.push({ ...ust, altKategoriler: altEslesen })
  }
  return sonuc.sort(karsilastir)
})

const toplamSayfa = computed(() => Math.max(1, Math.ceil(filtreliUstler.value.length / sayfaBoyutu)))

const sayfaliUstler = computed(() => {
  const bas = sayfa.value * sayfaBoyutu
  return filtreliUstler.value.slice(bas, bas + sayfaBoyutu)
})

const tabloSatirlari = computed(() => {
  const satirlar = []
  for (const ust of sayfaliUstler.value) {
    const altKategoriler = altlar(ust)
    satirlar.push({ ...ust, derinlik: 0, altKategoriler })
    if (acikKumeler.value.includes(ust.id)) {
      for (const alt of altKategoriler) satirlar.push({ ...alt, derinlik: 1 })
    }
  }
  return satirlar
})

const tabloVar = computed(() => !yukleniyor.value && !hata.value && tabloSatirlari.value.length > 0)

const acikMi = (id) => acikKumeler.value.includes(id)
const acKapa = (id) => {
  if (acikMi(id)) acikKumeler.value = acikKumeler.value.filter((x) => x !== id)
  else acikKumeler.value = [...acikKumeler.value, id]
}

const sayfaSinirla = () => {
  if (sayfa.value > toplamSayfa.value - 1) sayfa.value = Math.max(0, toplamSayfa.value - 1)
}
const oncekiSayfa = () => {
  if (sayfa.value > 0) sayfa.value -= 1
}
const sonrakiSayfa = () => {
  if (sayfa.value < toplamSayfa.value - 1) sayfa.value += 1
}

const ozet = computed(() => ({
  toplam: Number(kategoriStore.ozet?.toplam || 0),
  aktif: Number(kategoriStore.ozet?.aktif || 0),
  pasif: Number(kategoriStore.ozet?.pasif || 0),
  altKategori: Number(kategoriStore.ozet?.altKategori || 0)
}))

// ---- Yukleme ----
const yukle = async () => {
  yukleniyor.value = true
  hata.value = null
  try {
    const sonuclar = await Promise.allSettled([
      kategoriStore.agacGetir({
        kapsam: kapsam.value,
        q: arama.value.trim() || undefined,
        aktif: aktifParam()
      }),
      kategoriStore.ozetGetir({ kapsam: kapsam.value })
    ])
    if (sonuclar[0].status === 'rejected') throw sonuclar[0].reason
    // Ust kategoriler varsayilan olarak acik gelir; kullanici kapatabilir.
    acikKumeler.value = agac.value
      .filter((u) => altlar(u).length)
      .map((u) => u.id)
    sayfaSinirla()
  } catch (err) {
    hata.value = err?.response?.data?.message || err?.message || t('kategoriler.hataYukleme')
  } finally {
    yukleniyor.value = false
  }
}

let aramaZamanlayici = null
const aramaGirdi = () => {
  if (aramaZamanlayici) clearTimeout(aramaZamanlayici)
  aramaZamanlayici = setTimeout(() => {
    sayfa.value = 0
    yukle()
  }, 300)
}

const durumSec = (deger) => {
  if (durumFiltre.value === deger) return
  durumFiltre.value = deger
  sayfa.value = 0
  yukle()
}

const sistemYukle = async () => {
  if (sistemYuklendi.value || sistemYukleniyor.value) return
  sistemYukleniyor.value = true
  try {
    await kategoriStore.sistemKategorileriGetir()
    sistemYuklendi.value = true
  } catch {
    /* kart bos kalir; tablo yuklemesi etkilenmez */
  } finally {
    sistemYukleniyor.value = false
  }
}

watch(kapsam, (yeni) => {
  try {
    localStorage.setItem(KAPSAM_ANAHTAR, yeni)
  } catch {
    /* yoksay */
  }
  sayfa.value = 0
  acikKumeler.value = []
  yukle()
  if (yeni === 'MASRAF') sistemYukle()
})

onMounted(async () => {
  await yukle()
  if (kapsam.value === 'MASRAF') sistemYukle()
})

onUnmounted(() => {
  if (aramaZamanlayici) clearTimeout(aramaZamanlayici)
})

// ---- Form ----
const ustKategoriler = computed(() => agac.value.filter((k) => !k.ustKategoriId))

const yeniAc = () => {
  formKategori.value = null
  formUstId.value = null
  formDialog.value = true
}

const duzenleAc = (k) => {
  formKategori.value = { ...k }
  formUstId.value = null
  formDialog.value = true
}

const altEkle = (ust) => {
  formKategori.value = null
  formUstId.value = ust.id
  formDialog.value = true
}

const payloadOlustur = (k, ek = {}) => ({
  ad: k.ad,
  kapsam: k.kapsam || kapsam.value,
  ustKategoriId: k.ustKategoriId ?? null,
  renk: k.renk || null,
  ikon: k.ikon || null,
  sira: Number(k.sira) || 0,
  aciklama: k.aciklama || null,
  aktif: k.aktif !== false,
  ...ek
})

const kaydet = async (payload) => {
  kaydediliyor.value = true
  try {
    if (formKategori.value?.id) {
      await kategoriStore.updateKategori(formKategori.value.id, payload)
      toastBildirim.basarili(t('kategoriler.guncellendi'))
    } else {
      await kategoriStore.addKategori(payload)
      toastBildirim.basarili(t('kategoriler.olusturuldu'))
    }
    kategoriStore.onbellekTemizle()
    formDialog.value = false
    await yukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('kategoriler.islemBasarisiz'))
  } finally {
    kaydediliyor.value = false
  }
}

// ---- Satir aksiyonlari ----
const silEngel = (k) => {
  if (!isAdmin.value) return t('kategoriler.adminGerekli')
  const altSayisi = Number(k?.altKategoriSayisi ?? altlar(k).length ?? 0)
  if (altSayisi > 0) return t('kategoriler.silAltKategoriEngel', { n: altSayisi })
  const kullanim = Number(k?.kullanimSayisi || 0)
  if (kullanim > 0) return t('kategoriler.silKullanimEngel', { n: kullanim })
  return ''
}

const kardesSirali = (k) => {
  if (k.ustKategoriId) {
    const ust = agac.value.find((u) => u.id === k.ustKategoriId)
    return altlar(ust).slice().sort((a, b) => (Number(a.sira) || 0) - (Number(b.sira) || 0))
  }
  return agac.value.slice().sort((a, b) => (Number(a.sira) || 0) - (Number(b.sira) || 0))
}

const yukariVar = (k) => kardesSirali(k).findIndex((x) => x.id === k.id) > 0
const asagiVar = (k) => {
  const liste = kardesSirali(k)
  const i = liste.findIndex((x) => x.id === k.id)
  return i >= 0 && i < liste.length - 1
}

const siraDegistir = async (k, yon) => {
  const liste = kardesSirali(k)
  const i = liste.findIndex((x) => x.id === k.id)
  const hedef = liste[i + yon]
  if (i < 0 || !hedef) return
  const aSira = Number(k.sira) || 0
  const bSira = Number(hedef.sira) || 0
  const yeniA = aSira === bSira ? i + yon : bSira
  const yeniB = aSira === bSira ? i : aSira
  try {
    siralama.value = 'sira'
    await Promise.all([
      kategoriStore.updateKategori(k.id, payloadOlustur(k, { sira: Math.max(0, yeniA) })),
      kategoriStore.updateKategori(hedef.id, payloadOlustur(hedef, { sira: Math.max(0, yeniB) }))
    ])
    kategoriStore.onbellekTemizle()
    toastBildirim.basarili(t('kategoriler.siraGuncellendi'))
    await yukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('kategoriler.islemBasarisiz'))
  }
}

const arsivToggle = async (k) => {
  if (!isAdmin.value) {
    toastBildirim.uyari(t('kategoriler.adminGerekli'))
    return
  }
  try {
    if (k.aktif === false) {
      await kategoriStore.aktiflestir(k.id)
      toastBildirim.basarili(t('kategoriler.aktiflestirildi'))
    } else {
      await kategoriStore.arsivle(k.id)
      toastBildirim.basarili(t('kategoriler.arsivlendi'))
    }
    await yukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('kategoriler.islemBasarisiz'))
  }
}

const sil = (k) => {
  const engel = silEngel(k)
  if (engel) {
    toastBildirim.uyari(engel)
    return
  }
  confirm.require({
    message: t('kategoriler.silOnayMesajAd', { ad: k.ad }),
    header: t('common.silmeOnayi'),
    icon: 'pi pi-exclamation-triangle',
    rejectProps: { label: t('common.vazgec'), severity: 'secondary', outlined: true, size: 'small' },
    acceptProps: { label: t('common.evetSil'), severity: 'danger', size: 'small' },
    accept: async () => {
      try {
        await kategoriStore.deleteKategori(k.id)
        kategoriStore.onbellekTemizle()
        toastBildirim.basarili(t('kategoriler.kategoriSilindi'))
        await yukle()
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || t('kategoriler.silmeBasarisiz'))
      }
    }
  })
}

// ---- Birlestirme ----
const tumKategoriler = computed(() =>
  agac.value.flatMap((u) => [u, ...(Array.isArray(u.altKategoriler) ? u.altKategoriler : [])])
)

const birlestirHedefleri = computed(() => {
  const kaynak = birlestirKaynak.value
  if (!kaynak) return []
  return tumKategoriler.value.filter(
    (k) => k.id !== kaynak.id && k.ustKategoriId !== kaynak.id && k.aktif !== false
  )
})

const birlestirHedef = computed(
  () => birlestirHedefleri.value.find((k) => k.id === birlestirHedefId.value) || null
)

const birlestirAc = (k) => {
  if (!isAdmin.value) {
    toastBildirim.uyari(t('kategoriler.adminGerekli'))
    return
  }
  birlestirKaynak.value = { ...k }
  birlestirHedefId.value = null
  birlestirDialog.value = true
}

const birlestirKaydet = async () => {
  if (!birlestirKaynak.value || !birlestirHedef.value) {
    toastBildirim.uyari(t('kategoriler.hedefSecin'))
    return
  }
  birlestiriyor.value = true
  try {
    await kategoriStore.birlestir({
      kaynakId: birlestirKaynak.value.id,
      hedefId: birlestirHedef.value.id
    })
    toastBildirim.basarili(t('kategoriler.birlestirildi'))
    birlestirDialog.value = false
    await yukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('kategoriler.birlestirHata'))
  } finally {
    birlestiriyor.value = false
  }
}

// ---- Kullanim drawer ----
const kullanimAc = async (k) => {
  kullanimVeri.value = {
    kategoriId: k.id,
    ad: k.ad,
    toplam: k.kullanimSayisi || 0,
    detaylar: [],
    renk: k.renk || null,
    ikon: k.ikon || null
  }
  kullanimDrawer.value = true
  kullanimYukleniyor.value = true
  try {
    const veri = await kategoriStore.kullanimGetir(k.id)
    if (veri) {
      kullanimVeri.value = {
        ...kullanimVeri.value,
        ...veri,
        renk: k.renk || null,
        ikon: k.ikon || null
      }
    }
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('kategoriler.kullanimYuklenemedi'))
  } finally {
    kullanimYukleniyor.value = false
  }
}

const satirAksiyonlari = (k) => {
  const aksiyonlar = [
    { etiket: t('common.edit'), ikon: 'pi pi-pencil', baslik: t('common.edit'), islem: () => duzenleAc(k) }
  ]

  if (!k.ustKategoriId) {
    aksiyonlar.push({
      etiket: t('kategoriler.altKategoriEkle'),
      ikon: 'pi pi-plus',
      baslik: t('kategoriler.altKategoriEkle'),
      islem: () => altEkle(k)
    })
  }

  const siraKapali = siralama.value !== 'sira'
  aksiyonlar.push(
    {
      etiket: t('kategoriler.yukariTasi'),
      ikon: 'pi pi-arrow-up',
      baslik: t('kategoriler.yukariTasi'),
      devreDisi: siraKapali || !yukariVar(k),
      islem: () => siraDegistir(k, -1)
    },
    {
      etiket: t('kategoriler.asagiTasi'),
      ikon: 'pi pi-arrow-down',
      baslik: t('kategoriler.asagiTasi'),
      devreDisi: siraKapali || !asagiVar(k),
      islem: () => siraDegistir(k, 1)
    },
    {
      etiket: k.aktif === false ? t('kategoriler.aktiflestir') : t('kategoriler.arsivle'),
      ikon: k.aktif === false ? 'pi pi-replay' : 'pi pi-inbox',
      baslik: isAdmin.value
        ? (k.aktif === false ? t('kategoriler.aktiflestir') : t('kategoriler.arsivle'))
        : t('kategoriler.adminGerekli'),
      devreDisi: !isAdmin.value,
      islem: () => arsivToggle(k)
    },
    {
      etiket: t('kategoriler.birlestir'),
      ikon: 'pi pi-clone',
      baslik: isAdmin.value ? t('kategoriler.birlestir') : t('kategoriler.adminGerekli'),
      devreDisi: !isAdmin.value,
      islem: () => birlestirAc(k)
    }
  )

  const engel = silEngel(k)
  aksiyonlar.push({
    etiket: t('common.delete'),
    ikon: 'pi pi-trash',
    baslik: engel || t('common.delete'),
    devreDisi: !!engel,
    tehlike: true,
    islem: () => sil(k)
  })

  return aksiyonlar
}

defineExpose({
  kapsam,
  arama,
  durumFiltre,
  siralama,
  sayfa,
  yukle,
  aramaGirdi,
  kapsamSec,
  durumSec,
  acKapa,
  acikMi,
  yeniAc,
  duzenleAc,
  altEkle,
  kaydet,
  sil,
  silEngel,
  arsivToggle,
  birlestirAc,
  birlestirKaydet,
  kullanimAc,
  satirAksiyonlari,
  oncekiSayfa,
  sonrakiSayfa,
  formDialog,
  formKategori,
  formUstId,
  kullanimDrawer,
  kullanimVeri,
  kullanimYukleniyor,
  birlestirDialog,
  birlestirKaynak,
  birlestirHedefId,
  tabloSatirlari,
  filtreliUstler,
  toplamSayfa,
  sistemKategorileri
})
</script>

<style scoped>
.kategoriler-container {
  padding: 0;
}
.kpi-satiri {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}
.kapsam-cubugu {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  padding: 4px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg, 14px);
  margin-bottom: 10px;
}
.kapsam-sekme {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 9px 16px;
  border: 1px solid transparent;
  border-radius: var(--radius-md, 10px);
  background: transparent;
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--dur-base, 0.2s) var(--ease-standard, ease);
}
.kapsam-sekme:hover {
  color: var(--text-primary);
  background: var(--bg-muted);
}
.kapsam-sekme.aktif {
  background: var(--accent-soft, rgba(20, 184, 166, 0.16));
  border-color: var(--accent-border, rgba(20, 184, 166, 0.5));
  color: var(--accent);
}
.kapsam-sekme i {
  font-size: 13px;
}
.kasa-alt-cubugu {
  display: flex;
  gap: 6px;
  margin-bottom: 12px;
}
.kasa-alt-sekme {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 12px;
  border-radius: 999px;
  border: 1px solid var(--border);
  background: var(--bg-card);
  color: var(--text-secondary);
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--dur-base, 0.2s) var(--ease-standard, ease);
}
.kasa-alt-sekme:hover {
  color: var(--text-primary);
  border-color: var(--accent-border);
}
.kasa-alt-sekme.aktif {
  background: var(--accent-soft);
  border-color: var(--accent-border);
  color: var(--accent);
}
.kategori-arac {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}
.arama-kutu {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  min-width: 220px;
  max-width: 320px;
  flex: 1 1 220px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: var(--radius-md, 10px);
  padding: 2px 8px;
}
.arama-kutu:focus-within {
  border-color: var(--accent-border);
  box-shadow: 0 0 0 3px var(--accent-soft);
}
.arama-kutu > i {
  color: var(--text-muted);
  font-size: 14px;
  margin-left: 4px;
}
.arama-input {
  border: none;
  background: transparent;
  box-shadow: none !important;
}
.arama-input:focus {
  box-shadow: none !important;
}
.durum-cipleri {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}
.durum-cip {
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
.durum-cip:hover {
  border-color: var(--accent-border);
  color: var(--text-primary);
}
.durum-cip.aktif {
  background: var(--accent-soft);
  border-color: var(--accent-border);
  color: var(--accent);
}
.durum-cip i {
  font-size: 12px;
}
.siralama-secim {
  min-width: 150px;
}
.arac-bosluk {
  flex: 1 1 auto;
}
.sonuc-rozet {
  font-size: 12.5px;
  color: var(--text-secondary);
  background: var(--bg-muted);
  border: 1px solid var(--border);
  border-radius: 999px;
  padding: 4px 12px;
}
.kategori-masa {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 12px;
}
.kategori-hucre {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}
.kategori-hucre.alt {
  padding-left: 26px;
}
.agac-dugme {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  flex-shrink: 0;
  border: 1px solid var(--border);
  border-radius: 7px;
  background: var(--bg-muted);
  color: var(--text-secondary);
  cursor: pointer;
  font-size: 11px;
}
.agac-dugme:hover {
  color: var(--accent);
  border-color: var(--accent-border);
}
.agac-dugme:focus-visible {
  outline: none;
  box-shadow: 0 0 0 3px var(--accent-soft-strong);
}
.agac-bosluk {
  width: 24px;
  flex-shrink: 0;
}
.renk-nokta {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}
.kategori-ikon {
  font-size: 14px;
  flex-shrink: 0;
}
.kategori-ad {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--text-primary);
  font-weight: 600;
}
.kategori-hucre.alt .kategori-ad {
  font-weight: 500;
}
.ust-ad {
  font-size: 11.5px;
  color: var(--text-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.alt-rozet {
  flex-shrink: 0;
}
.sira-deger {
  font-variant-numeric: tabular-nums;
  color: var(--text-secondary);
}
.kullanim-rozet {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 3px 10px;
  border-radius: 999px;
  border: 1px solid var(--border);
  background: var(--bg-muted);
  color: var(--text-primary);
  font-size: 12px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  cursor: pointer;
}
.kullanim-rozet.bos {
  color: var(--text-muted);
  font-weight: 500;
}
.kullanim-rozet:hover {
  border-color: var(--accent-border);
  color: var(--accent);
}
.satir-aksiyon {
  display: flex;
  align-items: center;
  gap: 2px;
  flex-wrap: wrap;
}
.sayfalama {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 10px;
  flex-wrap: wrap;
}
.sayfa-bilgi {
  font-size: 12.5px;
  font-weight: 600;
  color: var(--text-primary);
}
.sayfa-toplam {
  font-size: 12px;
  color: var(--text-muted);
  margin-left: 4px;
}
.kategori-mobil {
  display: none;
}
.kategori-mobil-kart {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px 14px;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: var(--bg-card);
}
.kategori-mobil-kart.alt {
  margin-left: 18px;
  border-left: 3px solid var(--accent-soft-strong);
}
.kmk-ust {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}
.kmk-ad {
  flex: 1 1 auto;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13.5px;
  font-weight: 700;
  color: var(--text-primary);
}
.kmk-satir {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
  font-size: 12.5px;
  color: var(--text-secondary);
}
.sistem-kart {
  margin-top: 16px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 14px 16px;
}
.sistem-baslik {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--border);
}
.sistem-baslik > i {
  color: var(--text-muted);
}
.sistem-metin {
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex: 1 1 220px;
  min-width: 0;
}
.sistem-metin strong {
  font-size: 13.5px;
  color: var(--text-primary);
}
.sistem-metin span {
  font-size: 12px;
  color: var(--text-secondary);
}
.sistem-liste {
  list-style: none;
  margin: 10px 0 0;
  padding: 0;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 8px;
}
.sistem-satir {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 10px;
  border: 1px solid var(--border);
  border-radius: var(--radius-md, 10px);
  background: var(--bg-muted);
  min-width: 0;
}
.sistem-kod {
  font-size: 11.5px;
  font-weight: 700;
  color: var(--accent);
  background: var(--accent-soft);
  border-radius: 6px;
  padding: 2px 6px;
}
.sistem-ad {
  flex: 1 1 auto;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12.5px;
  color: var(--text-primary);
}
.sistem-durum,
.sistem-bos {
  margin: 12px 0 0;
  font-size: 12.5px;
  color: var(--text-secondary);
  text-align: center;
}
.birlestir {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.birlestir-aciklama {
  margin: 0;
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.5;
}
.birlestir-kaynak {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  padding: 10px 12px;
  border: 1px solid var(--border);
  border-radius: var(--radius-md, 10px);
  background: var(--bg-muted);
}
.birlestir-etiket {
  font-size: 11.5px;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  color: var(--text-muted);
  font-weight: 700;
}
.birlestir-ad {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  font-size: 13px;
  font-weight: 700;
  color: var(--text-primary);
}
.birlestir-ozet {
  font-size: 12.5px;
  color: var(--text-secondary);
}
.birlestir-uyari {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin: 0;
  padding: 10px 12px;
  border-radius: var(--radius-md, 10px);
  background: var(--warning-soft, rgba(245, 158, 11, 0.12));
  color: var(--warning, #f59e0b);
  font-size: 12.5px;
  line-height: 1.45;
}
.alan {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.alan label {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
}
.w-full {
  width: 100%;
}
.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}
@media (max-width: 768px) {
  .kategori-masa {
    display: none;
  }
  .kategori-mobil {
    display: flex;
    flex-direction: column;
    gap: 10px;
  }
  .arac-bosluk {
    display: none;
  }
  .arama-kutu {
    max-width: 100%;
    flex-basis: 100%;
  }
  .durum-cipleri {
    flex: 1 1 100%;
    flex-wrap: nowrap;
    overflow-x: auto;
    padding-bottom: 2px;
  }
  .durum-cip {
    flex-shrink: 0;
  }
  .kapsam-cubugu {
    flex-wrap: nowrap;
    overflow-x: auto;
  }
  .kapsam-sekme {
    flex-shrink: 0;
  }
}
@media (prefers-reduced-motion: reduce) {
  .kapsam-sekme,
  .kasa-alt-sekme,
  .durum-cip,
  .agac-dugme,
  .kullanim-rozet {
    transition: none;
  }
}
</style>
