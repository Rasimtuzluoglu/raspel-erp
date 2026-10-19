<template>
  <div class="donemler-sayfasi">
    <PageHeader
      :title="t('donemler.title')"
      :subtitle="t('donemler.aciklama')"
      icon="pi pi-calendar"
    >
      <template #actions>
        <Dropdown
          v-model="seciliSirketId"
          :options="sirketler"
          option-label="ad"
          option-value="id"
          :placeholder="t('donemler.sirketSecin')"
          class="sirket-dropdown"
          @change="sirketDegisti"
        />
        <Button
          :label="t('donemler.yilSonuKapat')"
          icon="pi pi-lock"
          severity="warn"
          outlined
          :disabled="!seciliSirketId"
          @click="kapanisDialog = true"
        />
        <Button
          :label="t('donemler.yeniDonem')"
          icon="pi pi-plus"
          :disabled="!seciliSirketId"
          @click="dialogAc()"
        />
        <Button
          icon="pi pi-refresh"
          :aria-label="t('common.refresh')"
          class="p-button-text p-button-sm"
          :loading="yukleniyor"
          @click="yenile"
        />
      </template>
    </PageHeader>

    <div class="kpi-serit">
      <KpiKart
        :baslik="t('donemler.kpiToplam')"
        :deger="donemler.length"
        ikon="pi pi-calendar"
        renk="#3b82f6"
        :para-birimi="false"
      />
      <KpiKart
        :baslik="t('donemler.kpiAktif')"
        :deger="istatistik.aktifAdet"
        ikon="pi pi-check-circle"
        renk="#10b981"
        :para-birimi="false"
      />
      <KpiKart
        :baslik="t('donemler.kpiKilitli')"
        :deger="istatistik.kilitliAdet"
        ikon="pi pi-lock"
        renk="#f59e0b"
        :para-birimi="false"
      />
      <KpiKart
        :baslik="t('donemler.kpiGuncel')"
        :deger="guncelDonem ? 1 : 0"
        ikon="pi pi-flag"
        renk="#8b5cf6"
        :para-birimi="false"
        class="kpi-guncel"
      >
        <template #deger>
          {{ guncelDonem?.ad || '-' }}
        </template>
      </KpiKart>
      <KpiKart
        :baslik="t('donemler.kpiSonKapanis')"
        :deger="sonKapanisYili || 0"
        ikon="pi pi-history"
        renk="#64748b"
        :para-birimi="false"
      >
        <template #deger>
          {{ sonKapanisYili || '-' }}
        </template>
      </KpiKart>
    </div>

    <div
      v-if="seciliSirketId && donemler.length > 0"
      class="donem-arac"
    >
      <IconField class="filtre-arama">
        <InputIcon class="pi pi-search" />
        <InputText
          v-model="arama"
          :placeholder="t('donemler.aramaPlaceholder')"
        />
      </IconField>
      <Dropdown
        v-model="sirala"
        :options="siralamaSecenekleri"
        option-label="ad"
        option-value="deger"
        class="filtre-select"
      />
      <span class="arac-bosluk" />
      <Button
        :label="t('donemler.excel')"
        icon="pi pi-file-excel"
        outlined
        size="small"
        :loading="excelYukleniyor"
        @click="excelIndir"
      />
    </div>

    <div
      v-if="seciliSirketId && donemler.length > 0"
      class="donem-cipler"
      role="group"
      :aria-label="t('common.status')"
    >
      <button
        v-for="c in durumSecenekleri"
        :key="c.deger"
        type="button"
        class="donem-cip"
        :class="{ aktif: durumFiltresi === c.deger }"
        :aria-pressed="durumFiltresi === c.deger"
        @click="durumFiltresi = c.deger"
      >
        <i :class="c.ikon" /> {{ c.etiket }}
      </button>
    </div>

    <div
      v-if="seciliSirketId && yukleniyor && donemler.length === 0"
      class="donem-izgara"
      role="status"
      :aria-label="t('common.loading')"
    >
      <SkeletonLoader
        :count="3"
        height="150px"
      />
    </div>

    <EmptyState
      v-else-if="seciliSirketId && !yukleniyor && donemler.length === 0"
      :message="t('donemler.kayitYok')"
      icon="pi pi-calendar-times"
      :action-label="t('donemler.yeniDonem')"
      action-icon="pi pi-plus"
      @action="dialogAc()"
    />

    <EmptyState
      v-else-if="seciliSirketId && !yukleniyor && filtreliDonemler.length === 0"
      :message="t('donemler.filtreBos')"
      icon="pi pi-filter-slash"
      :action-label="t('donemler.temizle')"
      action-icon="pi pi-filter-slash"
      @action="filtreTemizle"
    />

    <div
      v-else
      class="donem-izgara"
    >
      <article
        v-for="d in filtreliDonemler"
        :key="d.id"
        class="donem-kart"
        :class="{ 'donem-kart-aktif': d.aktif, 'donem-kart-kilitli': d.kilitli }"
      >
        <header class="donem-kart-ust">
          <span class="donem-no">#{{ d.id }}</span>
          <div class="donem-rozetler">
            <Tag
              v-if="guncelDonem && d.id === guncelDonem.id"
              :value="t('donemler.guncel')"
              severity="info"
              icon="pi pi-flag"
            />
            <Tag
              v-if="d.aktif"
              :value="t('donemler.aktif')"
              severity="success"
              icon="pi pi-check-circle"
            />
            <Tag
              v-else
              :value="t('donemler.pasif')"
              severity="danger"
            />
            <Tag
              v-if="d.kilitli"
              :value="t('donemler.kilitli')"
              severity="warn"
              icon="pi pi-lock"
            />
          </div>
        </header>

        <h3 class="donem-ad">
          {{ d.ad }}
        </h3>

        <div class="donem-aralik">
          <span class="donem-tarih">
            <i class="pi pi-calendar" /> {{ formatTarih(d.baslangic) }}
          </span>
          <span class="donem-tire">→</span>
          <span class="donem-tarih">
            <i class="pi pi-flag" /> {{ formatTarih(d.bitis) }}
          </span>
        </div>

        <div class="donem-kart-alt">
          <span class="donem-gun">
            <i class="pi pi-clock" /> {{ gunSayisi(d) }} {{ t('donemler.gun') }}
          </span>
          <div class="donem-eylemler">
            <SatirEylemleri
              :gorunur="{ duzenle: false, cogalt: false, sil: false }"
              :items="donemAksiyonlari(d)"
            />
          </div>
        </div>
      </article>
    </div>

    <DonemDetayDrawer
      v-model:visible="detayDrawer"
      :donem="seciliDonem"
      :guncel="!!guncelDonem && seciliDonem?.id === guncelDonem.id"
      @aktif="drawerAktif"
      @kilitle="drawerKilitle"
      @kilidi-ac="drawerKilidiAc"
      @duzenle="drawerDuzenle"
    />

    <Dialog
      v-model:visible="kapanisDialog"
      :header="t('donemler.yilSonuKapat')"
      modal
      :style="{ width: '440px' }"
      :breakpoints="{ '575px': '92vw' }"
    >
      <Message
        severity="warn"
        :closable="false"
        class="kapanis-uyari"
      >
        {{ t('donemler.kapanisUyari') }}
      </Message>
      <div class="form-grid">
        <div class="field">
          <label>{{ t('donemler.maliYil') }}</label>
          <InputNumber
            v-model="kapanisForm.yil"
            :use-grouping="false"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('donemler.kapanisNotu') }}</label>
          <Textarea
            v-model="kapanisForm.ozet"
            rows="3"
            class="w-full"
          />
        </div>
      </div>

      <div class="kapanis-onizleme">
        <div class="kapanis-onizleme-ust">
          <span>{{ t('donemler.etkilenecekDonemler') }}</span>
          <Tag
            :value="String(kapanisOnizleme.adet)"
            severity="info"
          />
        </div>
        <ul
          v-if="kapanisOnizleme.donemler.length"
          class="kapanis-onizleme-liste"
        >
          <li
            v-for="d in kapanisOnizleme.donemler"
            :key="d.id"
          >
            <strong>{{ d.ad }}</strong>
            <span class="muted">{{ formatTarih(d.baslangic) }} → {{ formatTarih(d.bitis) }}</span>
          </li>
        </ul>
        <p
          v-else
          class="muted"
        >
          {{ t('donemler.etkilenecekDonemYok') }}
        </p>
        <Message
          v-if="kapanisOnizleme.zatenKapali"
          severity="error"
          :closable="false"
          class="kapanis-uyari"
        >
          {{ t('donemler.yilZatenKapali') }}
        </Message>
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          icon="pi pi-times"
          class="p-button-text"
          @click="kapanisDialog = false"
        />
        <Button
          :label="t('donemler.kapat')"
          icon="pi pi-lock"
          severity="warning"
          :loading="kapanisYukleniyor"
          :disabled="!kapanisYapilabilir"
          @click="yilSonuKapat"
        />
      </template>
    </Dialog>

    <Card
      v-if="kapanislar.length"
      class="kapanis-kart"
    >
      <template #title>
        <div class="kapanis-baslik">
          <span><i class="pi pi-history" /> {{ t('donemler.kapanisGecmisi') }}</span>
          <Button
            :label="t('donemler.excel')"
            icon="pi pi-file-excel"
            outlined
            size="small"
            :loading="kapanisExcelYukleniyor"
            @click="kapanisExcelIndir"
          />
        </div>
      </template>
      <template #content>
        <div class="kapanis-satir kapanis-baslik-satir">
          <span>{{ t('donemler.maliYil') }}</span>
          <span>{{ t('donemler.kapanisTarihi') }}</span>
          <span>{{ t('donemler.kullanici') }}</span>
          <span>{{ t('donemler.kilitlenenAdet') }}</span>
          <span>{{ t('donemler.kapanisNotu') }}</span>
        </div>
        <div
          v-for="k in kapanislar"
          :key="k.id"
          class="kapanis-satir"
        >
          <span class="kapanis-yil">{{ k.yil }}</span>
          <span class="kapanis-tarih">{{ formatTarihSaat(k.kapanisTarihi) }}</span>
          <span class="kapanis-kullanici">{{ k.kullaniciId != null ? '#' + k.kullaniciId : '-' }}</span>
          <span class="kapanis-adet">{{ k.kilitlenenDonemSayisi ?? '-' }}</span>
          <span class="kapanis-not">{{ k.ozet || '-' }}</span>
        </div>
      </template>
    </Card>

    <Dialog
      v-model:visible="dialog"
      :header="duzenleme ? t('donemler.donemDuzenle') : t('donemler.yeniDonem')"
      modal
      :style="{ width: '440px' }"
      :breakpoints="{ '575px': '92vw' }"
    >
      <div class="form-grid">
        <div class="field">
          <label>{{ t('donemler.donemAdiZorunlu') }}</label>
          <InputText
            v-model="form.ad"
            class="w-full"
            :placeholder="t('donemler.donemAdiPlaceholder')"
          />
        </div>
        <div class="field">
          <label>{{ t('donemler.baslangicTarihiZorunlu') }}</label>
          <DatePicker
            v-model="form.baslangic"
            date-format="dd.mm.yy"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('donemler.bitisTarihiZorunlu') }}</label>
          <DatePicker
            v-model="form.bitis"
            date-format="dd.mm.yy"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('donemler.aktif') }}</label>
          <InputSwitch v-model="form.aktif" />
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
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useConfirm } from 'primevue/useconfirm'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { donemAPI, sirketAPI, excelAPI } from '../api/index.js'
import { useI18n } from 'vue-i18n'
import { getLocalDateString, formatTarih, formatTarihSaat } from '../utils/format.js'
import { useAuthStore } from '../stores/authStore.js'
import PageHeader from '../components/PageHeader.vue'
import KpiKart from '../components/KpiKart.vue'
import EmptyState from '../components/EmptyState.vue'
import SatirEylemleri from '../components/SatirEylemleri.vue'
import DonemDetayDrawer from '../components/DonemDetayDrawer.vue'
import SkeletonLoader from '../components/SkeletonLoader.vue'
const toastBildirim = useToastBildirim()
const confirm = useConfirm()
const { t } = useI18n()
const authStore = useAuthStore()

const donemler = ref([])
const sirketler = ref([])
const seciliSirketId = ref(null)
const yukleniyor = ref(false)
const dialog = ref(false)
const duzenleme = ref(false)
const kaydediliyor = ref(false)
const seciliId = ref(null)
const form = ref({ ad: '', baslangic: null, bitis: null, aktif: true })
const kapanisDialog = ref(false)
const kapanisYukleniyor = ref(false)
const kapanisExcelYukleniyor = ref(false)
const kapanisForm = ref({ yil: new Date().getFullYear(), ozet: '' })
const kapanislar = ref([])

// Arama / durum çipleri / sıralama (istemci taraflı; dönem sayısı küçüktür).
const arama = ref('')
const durumFiltresi = ref('TUMU')
const sirala = ref('baslangic,desc')
const excelYukleniyor = ref(false)

// Detay çekmecesi.
const detayDrawer = ref(false)
const seciliDonem = ref(null)

onMounted(async () => {
  try {
    const r = await sirketAPI.getAktif()
    sirketler.value = r.data
    if (sirketler.value.length > 0) {
      // Aktif JWT şirketini önceliklendir; listeyi körlemesine ilk elemandan alma
      // (aksi halde başka şirkete geçilmişse "Dönem bu şirkete ait değil" hatası oluşur).
      const aktifId = authStore.sirketId
      const aktifVar = aktifId != null && sirketler.value.some((s) => s.id === aktifId)
      seciliSirketId.value = aktifVar ? aktifId : sirketler.value[0].id
      await donemleriYukle()
      await kapanislariYukle()
    }
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('donemler.hataSirketler'))
  }
})

const kapanislariYukle = async () => {
  if (!seciliSirketId.value) {
    kapanislar.value = []
    return
  }
  try {
    const r = await donemAPI.kapanislar()
    kapanislar.value = r.data || []
  } catch {
    kapanislar.value = []
  }
}

const kilitle = (data) => {
  confirm.require({
    message: t('donemler.kilitleOnay', { ad: data.ad }),
    header: t('donemler.kilitle'),
    icon: 'pi pi-lock',
    acceptLabel: t('donemler.evet'),
    rejectLabel: t('common.vazgec'),
    accept: async () => {
      try {
        await donemAPI.kilitle(data.id)
        toastBildirim.basarili(t('donemler.kilitlendi'))
        await donemleriYukle()
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || err?.message || t('donemler.islemBasarisiz'))
      }
    },
    reject: () => {}
  })
}

const kilidiAc = (data) => {
  confirm.require({
    message: t('donemler.kilidiAcOnay'),
    header: t('donemler.kilidiAc'),
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: t('donemler.evet'),
    rejectLabel: t('common.vazgec'),
    accept: async () => {
      try {
        await donemAPI.kilidiAc(data.id)
        toastBildirim.basarili(t('donemler.kilidiAcildi'))
        await donemleriYukle()
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || err?.message || t('donemler.islemBasarisiz'))
      }
    },
    reject: () => {}
  })
}

const yilSonuKapat = async () => {
  if (!kapanisForm.value.yil) {
    toastBildirim.uyari(t('donemler.maliYilZorunlu'))
    return
  }
  if (!kapanisYapilabilir.value) {
    toastBildirim.uyari(t('donemler.kapanisYapilamaz'))
    return
  }
  kapanisYukleniyor.value = true
  try {
    await donemAPI.yilSonuKapat({
      sirketId: seciliSirketId.value,
      yil: kapanisForm.value.yil,
      ozet: kapanisForm.value.ozet
    })
    toastBildirim.basarili(t('donemler.kapanisBasarili'))
    kapanisDialog.value = false
    kapanisForm.value = { yil: new Date().getFullYear(), ozet: '' }
    await donemleriYukle()
    await kapanislariYukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('donemler.kapanisBasarisiz'))
  } finally {
    kapanisYukleniyor.value = false
  }
}

const kapanisExcelIndir = async () => {
  kapanisExcelYukleniyor.value = true
  try {
    const res = await excelAPI.donemKapanislari()
    const url = window.URL.createObjectURL(
      new Blob([res.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    )
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', `donem-kapanislar-${new Date().toISOString().slice(0, 10)}.xlsx`)
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)
    toastBildirim.basarili(t('donemler.excelIndirildi'))
  } catch {
    toastBildirim.hata(t('donemler.excelHata'))
  } finally {
    kapanisExcelYukleniyor.value = false
  }
}

const donemleriYukle = async () => {
  if (!seciliSirketId.value) {
    donemler.value = []
    return
  }
  yukleniyor.value = true
  try {
    const r = await donemAPI.getBySirket(seciliSirketId.value)
    donemler.value = r.data
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('donemler.hataYukleme'))
  }
  yukleniyor.value = false
}

const yenile = async () => {
  await Promise.all([donemleriYukle(), kapanislariYukle()])
}

const excelIndir = async () => {
  excelYukleniyor.value = true
  try {
    const res = await excelAPI.donemler()
    const url = window.URL.createObjectURL(
      new Blob([res.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    )
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', `donemler-${new Date().toISOString().slice(0, 10)}.xlsx`)
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)
    toastBildirim.basarili(t('donemler.excelIndirildi'))
  } catch {
    toastBildirim.hata(t('donemler.excelHata'))
  } finally {
    excelYukleniyor.value = false
  }
}

const donemAksiyonlari = (d) => {
  const items = []
  if (!d.aktif) {
    items.push({ etiket: t('donemler.aktifYap'), ikon: 'pi pi-check-circle', islem: () => aktifYap(d) })
  }
  if (!d.kilitli) {
    items.push({ etiket: t('donemler.kilitle'), ikon: 'pi pi-lock', islem: () => kilitle(d) })
    items.push({ etiket: t('common.edit'), ikon: 'pi pi-pencil', islem: () => dialogAc(d) })
    items.push({ etiket: t('common.delete'), ikon: 'pi pi-trash', sinif: 'eylem-sil', islem: () => sil(d) })
  } else {
    items.push({ etiket: t('donemler.kilidiAc'), ikon: 'pi pi-lock-open', islem: () => kilidiAc(d) })
  }
  items.push({ etiket: t('donemler.detay'), ikon: 'pi pi-search', islem: () => detayAc(d) })
  return items
}

const detayAc = (d) => {
  seciliDonem.value = d
  detayDrawer.value = true
}
const drawerAktif = (d) => {
  detayDrawer.value = false
  aktifYap(d)
}
const drawerKilitle = (d) => {
  detayDrawer.value = false
  kilitle(d)
}
const drawerKilidiAc = (d) => {
  detayDrawer.value = false
  kilidiAc(d)
}
const drawerDuzenle = (d) => {
  detayDrawer.value = false
  dialogAc(d)
}

const aktifYap = async (data) => {
  try {
    await donemAPI.aktifYap(data.id)
    toastBildirim.basarili(t('donemler.aktifYapildi'))
    await donemleriYukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('donemler.islemBasarisiz'))
  }
}

const dialogAc = (data) => {
  duzenleme.value = !!data
  seciliId.value = data?.id || null
  form.value = data
    ? {
        ...data,
        baslangic: data.baslangic ? new Date(data.baslangic) : null,
        bitis: data.bitis ? new Date(data.bitis) : null
      }
    : { ad: '', baslangic: null, bitis: null, sirketId: seciliSirketId.value, aktif: true }
  dialog.value = true
}

const kaydet = async () => {
  if (!form.value.ad?.trim()) {
    toastBildirim.uyari(t('donemler.donemAdiZorunlu'))
    return
  }
  if (!form.value.baslangic || !form.value.bitis) {
    toastBildirim.uyari(t('donemler.tarihZorunlu'))
    return
  }
  if (new Date(form.value.bitis) < new Date(form.value.baslangic)) {
    toastBildirim.uyari(t('donemler.bitisOnceOlamaz'))
    return
  }
  kaydediliyor.value = true
  try {
    const payload = {
      ...form.value,
      sirketId: seciliSirketId.value,
      baslangic: getLocalDateString(form.value.baslangic),
      bitis: getLocalDateString(form.value.bitis)
    }
    if (duzenleme.value) {
      await donemAPI.update(seciliId.value, payload)
    } else {
      await donemAPI.create(payload)
    }
    dialog.value = false
    await donemleriYukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('donemler.hataKaydet'))
  }
  kaydediliyor.value = false
}

const sirketDegisti = async () => {
  kapanisForm.value.yil = new Date().getFullYear()
  kapanisForm.value.ozet = ''
  await donemleriYukle()
  await kapanislariYukle()
}

// Dönem listesinden türeyen özetler (KPI şeridi).
const istatistik = computed(() => ({
  aktifAdet: donemler.value.filter((d) => d.aktif).length,
  kilitliAdet: donemler.value.filter((d) => d.kilitli).length
}))

// Bugünü kapsayan dönem (varsa). ISO tarih karşılaştırması ile saat dilimi sorunu önlenir.
const guncelDonem = computed(() => {
  const bugun = getLocalDateString(new Date())
  return (
    donemler.value.find((d) => {
      const b = (d.baslangic || '').slice(0, 10)
      const s = (d.bitis || '').slice(0, 10)
      return Boolean(b && s) && bugun >= b && bugun <= s
    }) || null
  )
})

const durumSecenekleri = computed(() => [
  { deger: 'TUMU', etiket: t('donemler.cipTumu'), ikon: 'pi pi-list' },
  { deger: 'AKTIF', etiket: t('donemler.aktif'), ikon: 'pi pi-check-circle' },
  { deger: 'KILITLI', etiket: t('donemler.kilitli'), ikon: 'pi pi-lock' },
  { deger: 'PASIF', etiket: t('donemler.pasif'), ikon: 'pi pi-circle' }
])

const siralamaSecenekleri = computed(() => [
  { deger: 'baslangic,desc', ad: t('donemler.siralaBaslangicDesc') },
  { deger: 'baslangic,asc', ad: t('donemler.siralaBaslangicAsc') },
  { deger: 'ad,asc', ad: t('donemler.siralaAdAsc') }
])

const filtreliDonemler = computed(() => {
  let liste = [...donemler.value]
  const q = arama.value.trim().toLowerCase()
  if (q) liste = liste.filter((d) => (d.ad || '').toLowerCase().includes(q))
  if (durumFiltresi.value === 'AKTIF') liste = liste.filter((d) => d.aktif)
  else if (durumFiltresi.value === 'KILITLI') liste = liste.filter((d) => d.kilitli)
  else if (durumFiltresi.value === 'PASIF') liste = liste.filter((d) => !d.aktif && !d.kilitli)

  const [alan, yon] = sirala.value.split(',')
  liste.sort((a, b) => {
    let av = a[alan]
    let bv = b[alan]
    if (alan === 'ad') {
      av = (av || '').toLowerCase()
      bv = (bv || '').toLowerCase()
    }
    if (av === bv) return 0
    const sonuc = av > bv ? 1 : -1
    return yon === 'asc' ? sonuc : -sonuc
  })
  return liste
})

const filtreTemizle = () => {
  arama.value = ''
  durumFiltresi.value = 'TUMU'
}

const sonKapanisYili = computed(() => {
  if (!kapanislar.value.length) return null
  return kapanislar.value
    .map((k) => Number(k.yil))
    .filter((y) => Number.isFinite(y))
    .sort((a, b) => b - a)[0] ?? null
})

// Yıl sonu kapanış önizlemesi: seçilen mali yılı kapsayan dönemler + zaten kapalı mı?
const kapanisOnizleme = computed(() => {
  const yil = Number(kapanisForm.value.yil)
  if (!Number.isInteger(yil) || yil < 1900) {
    return { donemler: [], adet: 0, zatenKapali: false }
  }
  const bas = `${yil}-01-01`
  const bit = `${yil}-12-31`
  const liste = (donemler.value || []).filter((d) => {
    const b = (d.baslangic || '').slice(0, 10)
    const s = (d.bitis || '').slice(0, 10)
    return Boolean(b && s) && s >= bas && b <= bit
  })
  const zatenKapali = (kapanislar.value || []).some((k) => Number(k.yil) === yil)
  return { donemler: liste, adet: liste.length, zatenKapali }
})

const kapanisYapilabilir = computed(
  () => kapanisOnizleme.value.adet > 0 && !kapanisOnizleme.value.zatenKapali
)

// Dönemin kaç gün sürdüğü (aralık günleri dahil). Geçersiz tarih → null.
const gunSayisi = (d) => {
  const b = new Date(d.baslangic)
  const s = new Date(d.bitis)
  if (Number.isNaN(b.getTime()) || Number.isNaN(s.getTime()) || s < b) return '—'
  return Math.round((s - b) / 86400000) + 1
}

const sil = (data) => {
  confirm.require({
    message: t('common.confirmDelete'),
    header: t('common.silmeOnayi'),
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: t('common.evetSil'),
    rejectLabel: t('common.vazgec'),
    accept: async () => {
      try {
        await donemAPI.delete(data.id)
        await donemleriYukle()
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || err?.message || t('donemler.hataSil'))
      }
    },
    reject: () => {}
  })
}
</script>

<style scoped>
.donemler-sayfasi {
  padding: 0;
}
.sirket-dropdown {
  min-width: min(200px, 100%);
}
.w-full {
  width: 100%;
}
.donem-arac {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}
.filtre-arama {
  min-width: 220px;
  flex: 1 1 220px;
  max-width: 320px;
}
.filtre-select {
  min-width: 200px;
}
.arac-bosluk {
  flex: 1 1 auto;
}
.donem-cipler {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}
.donem-cip {
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
.donem-cip:hover {
  border-color: var(--accent-border);
  color: var(--text-primary);
}
.donem-cip.aktif {
  background: var(--accent-soft, rgba(245, 158, 11, 0.16));
  border-color: var(--accent-border, rgba(245, 158, 11, 0.5));
  color: var(--accent);
}
.donem-cip i {
  font-size: 12px;
}

/* KPI şeridi: toplam / aktif / kilitli / güncel dönem / son kapanış */
.kpi-serit {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
  gap: 12px;
  margin-bottom: 20px;
}
.kpi-guncel :deep(.kpi-deger) {
  font-size: 1.05rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* Dönem kart ızgarası */
.donem-izgara {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 14px;
}
.donem-kart {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 16px;
  border: 1px solid var(--border);
  border-radius: 14px;
  background: var(--bg-card);
  transition: border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
}
.donem-kart:hover {
  border-color: var(--accent-border);
  box-shadow: var(--elev-1, 0 2px 10px rgba(0, 0, 0, 0.12));
  transform: translateY(-2px);
}
.donem-kart-aktif {
  border-color: var(--accent);
  background: linear-gradient(180deg, var(--accent-soft) 0%, var(--bg-card) 55%);
}
.donem-kart-kilitli {
  opacity: 0.82;
}
.donem-kart-ust {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.donem-no {
  color: var(--text-muted);
  font-size: 12px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}
.donem-rozetler {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}
.donem-ad {
  margin: 0;
  color: var(--text-primary);
  font-size: 16px;
  font-weight: 700;
  line-height: 1.3;
  overflow-wrap: anywhere;
}
.donem-aralik {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  color: var(--text-secondary);
  font-size: 12.5px;
}
.donem-tarih {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  white-space: nowrap;
}
.donem-tarih i {
  color: var(--text-muted);
  font-size: 11.5px;
}
.donem-tire {
  color: var(--text-muted);
}
.donem-kart-alt {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-top: auto;
  padding-top: 10px;
  border-top: 1px solid var(--border);
}
.donem-gun {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  color: var(--text-muted);
  font-size: 12px;
  font-variant-numeric: tabular-nums;
}
.donem-eylemler {
  display: flex;
  align-items: center;
  gap: 2px;
}

.form-grid {
  display: flex;
  flex-direction: column;
  gap: 16px;
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
.kapanis-uyari {
  margin-bottom: 16px;
}
.kapanis-kart {
  margin-top: 20px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
}
.kapanis-satir {
  display: grid;
  grid-template-columns: 80px 160px 110px 110px 1fr;
  align-items: center;
  gap: 12px;
  padding: 8px 0;
  border-bottom: 1px solid var(--border);
  font-size: 13px;
}
.kapanis-baslik-satir {
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.4px;
  color: var(--text-muted);
}
.kapanis-baslik {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}
.kapanis-kullanici,
.kapanis-adet {
  color: var(--text-secondary);
  font-variant-numeric: tabular-nums;
}
.kapanis-onizleme {
  margin-top: 16px;
  padding: 12px;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: var(--bg-secondary);
}
.kapanis-onizleme-ust {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  font-size: 12.5px;
  font-weight: 700;
  color: var(--text-secondary);
  margin-bottom: 8px;
}
.kapanis-onizleme-liste {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.kapanis-onizleme-liste li {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  font-size: 12.5px;
}
.muted {
  color: var(--text-muted);
  font-size: 12px;
}
.kapanis-satir:last-child {
  border-bottom: none;
}
.kapanis-yil {
  font-weight: 700;
  color: var(--accent);
  min-width: 48px;
}
.kapanis-tarih {
  color: var(--text-muted);
  min-width: 150px;
}
.kapanis-not {
  color: var(--text-secondary);
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 640px) {
  .donem-izgara {
    grid-template-columns: 1fr;
  }
  .filtre-arama,
  .filtre-select {
    min-width: 0;
    max-width: none;
    flex: 1 1 100%;
  }
  .kapanis-baslik-satir {
    display: none;
  }
  .kapanis-satir {
    grid-template-columns: 1fr 1fr;
    gap: 4px 12px;
    padding: 10px 0;
  }
  .kapanis-not {
    grid-column: 1 / -1;
    white-space: normal;
    overflow: visible;
    text-overflow: clip;
  }
}

@media (prefers-reduced-motion: reduce) {
  .donem-kart,
  .donem-kart:hover,
  .donem-cip {
    transition: none;
    transform: none;
  }
}
</style>
