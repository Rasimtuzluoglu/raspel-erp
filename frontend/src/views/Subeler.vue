<template>
  <div class="subeler-container">
    <PageHeader
      :title="t('subeler.title')"
      :subtitle="t('subeler.aciklama')"
      icon="pi pi-building"
    >
      <template #actions>
        <Button
          v-if="isAdmin"
          :label="t('subeler.yeniSube')"
          icon="pi pi-plus"
          @click="dialogAc()"
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
        :baslik="t('subeler.kpiToplam')"
        :deger="ozet.toplam"
        ikon="pi pi-building"
        renk="#3b82f6"
        :para-birimi="false"
      />
      <KpiKart
        :baslik="t('subeler.kpiAktif')"
        :deger="ozet.aktif"
        ikon="pi pi-check-circle"
        renk="#10b981"
        :para-birimi="false"
      />
      <KpiKart
        :baslik="t('subeler.kpiPasif')"
        :deger="ozet.pasif"
        ikon="pi pi-times-circle"
        renk="#64748b"
        :para-birimi="false"
      />
      <KpiKart
        :baslik="t('subeler.kpiDepo')"
        :deger="ozet.depo"
        ikon="pi pi-warehouse"
        renk="#f59e0b"
        :para-birimi="false"
      />
    </div>

    <div class="sube-arac">
      <div
        class="sube-cipler"
        role="group"
        :aria-label="t('common.status')"
      >
        <button
          v-for="c in durumCipleri"
          :key="c.deger"
          type="button"
          class="sube-cip"
          :class="{ aktif: filtreDurum === c.deger }"
          :aria-pressed="filtreDurum === c.deger"
          @click="filtreDurum = c.deger"
        >
          <i :class="c.ikon" /> {{ c.ad }}
        </button>
      </div>
      <span class="arac-bosluk" />
      <Button
        :label="t('subeler.excel')"
        icon="pi pi-file-excel"
        outlined
        size="small"
        :loading="excelYukleniyor"
        @click="excelIndir"
      />
    </div>

    <div
      class="sube-masa"
      :aria-busy="yukleniyor"
    >
      <AppDataTable
        :value="filtreliListe"
        striped-rows
        arama-aktif
        :arama-placeholder="t('subeler.aramaPlaceholder')"
        :loading="yukleniyor"
        :empty-message="filtreDurum !== 'TUMU' ? t('subeler.filtreBos') : t('subeler.empty')"
        gorunum-anahtari="subeler"
      >
        <Column
          field="ad"
          :header="t('subeler.subeAdi')"
          sortable
        />
        <Column
          field="yetkili"
          :header="t('subeler.yetkili')"
          sortable
        />
        <Column
          field="telefon"
          :header="t('subeler.telefon')"
        />
        <Column
          field="adres"
          :header="t('subeler.adres')"
        />
        <Column
          field="aktif"
          :header="t('common.status')"
        >
          <template #body="{ data }">
            <Tag
              :value="data.aktif ? t('status.active') : t('status.passive')"
              :severity="data.aktif ? 'success' : 'danger'"
            />
          </template>
        </Column>
        <Column
          :header="t('common.actions')"
          style="width: 72px"
        >
          <template #body="{ data }">
            <SatirEylemleri
              :gorunur="{ duzenle: false, cogalt: false, sil: false }"
              :items="subeAksiyonlari(data)"
              :baslik="data.ad"
            />
          </template>
        </Column>
        <template #empty>
          <EmptyState
            v-if="!yukleniyor"
            :message="filtreDurum !== 'TUMU' ? t('subeler.filtreBos') : t('subeler.empty')"
            :sub-message="filtreDurum !== 'TUMU' ? '' : t('subeler.emptyHint')"
            icon="pi pi-map-marker"
            :action-label="isAdmin && filtreDurum === 'TUMU' ? t('subeler.yeniSube') : ''"
            action-icon="pi pi-plus"
            @action="dialogAc()"
          />
        </template>
      </AppDataTable>
    </div>

    <!-- Mobil kart görünümü -->
    <div
      class="sube-mobil"
      :aria-busy="yukleniyor"
    >
      <span
        class="sr-only"
        role="status"
        aria-live="polite"
      >{{ t('subeler.sonucSayisi', { sayi: filtreliListe.length }) }}</span>
      <div
        v-if="yukleniyor && !list.length"
        class="sube-skeleton"
        role="status"
        :aria-label="t('common.loading')"
      >
        <SkeletonLoader
          :count="4"
          height="96px"
        />
      </div>
      <EmptyState
        v-else-if="!filtreliListe.length"
        :message="filtreDurum !== 'TUMU' ? t('subeler.filtreBos') : t('subeler.empty')"
        icon="pi pi-map-marker"
      />
      <article
        v-for="s in filtreliListe"
        :key="s.id"
        class="sube-mobil-kart"
      >
        <div class="smk-ust">
          <button
            type="button"
            class="smk-ad"
            :title="t('subeler.detay')"
            @click="detayAc(s)"
          >
            <i class="pi pi-building" />
            <span>{{ s.ad }}</span>
          </button>
          <SatirEylemleri
            :gorunur="{ duzenle: false, cogalt: false, sil: false }"
            :items="subeAksiyonlari(s)"
            :baslik="s.ad"
          />
        </div>
        <div class="smk-satir">
          <Tag
            :value="s.aktif ? t('status.active') : t('status.passive')"
            :severity="s.aktif ? 'success' : 'danger'"
          />
          <span
            v-if="s.yetkili"
            class="smk-alt"
          >{{ s.yetkili }}</span>
        </div>
        <div class="smk-satir">
          <span v-if="s.telefon"><i class="pi pi-phone" /> {{ s.telefon }}</span>
          <span
            v-if="s.adres"
            class="smk-adres"
          ><i class="pi pi-map-marker" /> {{ s.adres }}</span>
        </div>
      </article>
    </div>

    <Dialog
      v-model:visible="dialog"
      :header="dialogHeader"
      modal
      :style="{ width: '520px' }"
      :breakpoints="{ '575px': '92vw' }"
    >
      <div class="form-grid">
        <div class="field">
          <label for="sube-ad">{{ t('subeler.subeAdiZorunlu') }} <span
            class="zorunlu"
            aria-hidden="true"
          >*</span></label>
          <InputText
            id="sube-ad"
            v-model="form.ad"
            class="w-full"
            required
            aria-required="true"
            maxlength="200"
          />
        </div>
        <div class="field">
          <label for="sube-yetkili">{{ t('subeler.yetkili') }}</label><InputText
            id="sube-yetkili"
            v-model="form.yetkili"
            class="w-full"
            maxlength="100"
          />
        </div>
        <div class="field">
          <label for="sube-telefon">{{ t('subeler.telefon') }}</label><InputText
            id="sube-telefon"
            v-model="form.telefon"
            class="w-full"
            type="tel"
            inputmode="tel"
            autocomplete="tel"
            maxlength="20"
          />
        </div>
        <div class="field">
          <label for="sube-adres">{{ t('subeler.adres') }}</label><Textarea
            id="sube-adres"
            v-model="form.adres"
            rows="3"
            class="w-full"
            maxlength="500"
          />
        </div>
        <div class="field">
          <label>{{ t('status.active') }}</label><InputSwitch v-model="form.aktif" />
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

    <SubeDetayDrawer
      v-model:visible="detayDrawer"
      :sube="seciliSube"
      :depolar="depolar"
      :depolar-yukleniyor="depolarYukleniyor"
      :duzenlenebilir="isAdmin"
      @duzenle="drawerDuzenle"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { unwrapList } from '../api/utils/unwrap.js'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useConfirm } from 'primevue/useconfirm'
import { subeAPI, excelAPI } from '../api/index.js'
import { useAuthStore } from '../stores/authStore.js'
import PageHeader from '../components/PageHeader.vue'
import KpiKart from '../components/KpiKart.vue'
import EmptyState from '../components/EmptyState.vue'
import SatirEylemleri from '../components/SatirEylemleri.vue'
import SubeDetayDrawer from '../components/SubeDetayDrawer.vue'
import SkeletonLoader from '../components/SkeletonLoader.vue'

const { t } = useI18n()
const toastBildirim = useToastBildirim()
const confirm = useConfirm()
const authStore = useAuthStore()
const isAdmin = computed(() => authStore?.kullanici?.role === 'ADMIN')
const list = ref([])
const yukleniyor = ref(false)
const kaydediliyor = ref(false)
const dialog = ref(false)
const duzenleme = ref(false)
const form = ref({ ad: '', yetkili: '', telefon: '', adres: '', aktif: true })

const detayDrawer = ref(false)
const seciliSube = ref(null)
const depolar = ref([])
const depolarYukleniyor = ref(false)

const TELEFON_DESENI = /^[0-9+()\s-]{7,20}$/

const filtreDurum = ref('TUMU')
const excelYukleniyor = ref(false)

const durumCipleri = computed(() => [
  { deger: 'TUMU', ad: t('subeler.cipTumu'), ikon: 'pi pi-list' },
  { deger: 'AKTIF', ad: t('status.active'), ikon: 'pi pi-check-circle' },
  { deger: 'PASIF', ad: t('status.passive'), ikon: 'pi pi-times-circle' }
])

const filtreliListe = computed(() => {
  if (filtreDurum.value === 'AKTIF') return list.value.filter((s) => s.aktif)
  if (filtreDurum.value === 'PASIF') return list.value.filter((s) => !s.aktif)
  return list.value
})

const ozet = ref({ toplam: 0, aktif: 0, pasif: 0, depo: 0 })
const ozetYukle = async () => {
  try {
    const r = await subeAPI.ozet()
    ozet.value = { ...ozet.value, ...(r.data || {}) }
  } catch {
    /* özet alınamazsa KPI sıfır kalır */
  }
}

const dialogHeader = computed(() => (duzenleme.value ? t('subeler.subeDuzenle') : t('subeler.yeniSube')))

const yukle = async () => {
  yukleniyor.value = true
  try {
    const r = await subeAPI.getAll()
    list.value = unwrapList(r)
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('subeler.hataYukleme'))
  } finally {
    yukleniyor.value = false
  }
  ozetYukle()
}

onMounted(yukle)

const dialogAc = (data) => {
  duzenleme.value = !!data
  form.value = data ? { ...data } : { ad: '', yetkili: '', telefon: '', adres: '', aktif: true }
  dialog.value = true
}

const kaydet = async () => {
  const ad = (form.value.ad || '').trim()
  if (!ad) {
    toastBildirim.uyari(t('subeler.adZorunlu'))
    return
  }
  const telefon = (form.value.telefon || '').trim()
  if (telefon && !TELEFON_DESENI.test(telefon)) {
    toastBildirim.uyari(t('subeler.telefonGecersiz'))
    return
  }
  kaydediliyor.value = true
  try {
    const payload = { ...form.value, ad, telefon }
    if (duzenleme.value) {
      await subeAPI.update(payload.id, payload)
      toastBildirim.basarili(t('subeler.guncellendi'))
    } else {
      await subeAPI.create(payload)
      toastBildirim.basarili(t('subeler.olusturuldu'))
    }
    dialog.value = false
    await yukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('subeler.islemBasarisiz'))
  }
  kaydediliyor.value = false
}

const subeAksiyonlari = (d) => {
  const items = [{ etiket: t('subeler.detay'), ikon: 'pi pi-search', islem: () => detayAc(d) }]
  if (isAdmin.value) {
    items.push({ etiket: t('common.edit'), ikon: 'pi pi-pencil', islem: () => dialogAc(d) })
    items.push({ etiket: t('common.delete'), ikon: 'pi pi-trash', sinif: 'eylem-sil', islem: () => sil(d) })
  }
  return items
}

const detayAc = async (d) => {
  seciliSube.value = d
  depolar.value = []
  detayDrawer.value = true
  depolarYukleniyor.value = true
  try {
    const r = await subeAPI.depolar(d.id)
    depolar.value = Array.isArray(r.data) ? r.data : []
  } catch {
    depolar.value = []
  } finally {
    depolarYukleniyor.value = false
  }
}

const drawerDuzenle = (d) => {
  detayDrawer.value = false
  dialogAc(d)
}

const sil = (data) => {
  confirm.require({
    message: t('subeler.silOnayMesaj', { ad: data.ad }),
    header: t('common.silmeOnayi'),
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: t('common.evetSil'),
    rejectLabel: t('common.vazgec'),
    accept: async () => {
      try {
        await subeAPI.delete(data.id)
        await yukle()
        toastBildirim.basarili(t('subeler.silindi'))
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || t('subeler.silmeBasarisiz'))
      }
    }
  })
}

const excelIndir = async () => {
  excelYukleniyor.value = true
  try {
    const params = {}
    if (filtreDurum.value !== 'TUMU') params.durum = filtreDurum.value
    const res = await excelAPI.subeler(params)
    const url = window.URL.createObjectURL(
      new Blob([res.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    )
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', `subeler-${new Date().toISOString().slice(0, 10)}.xlsx`)
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)
    toastBildirim.basarili(t('subeler.excelIndirildi'))
  } catch {
    toastBildirim.hata(t('subeler.excelHata'))
  } finally {
    excelYukleniyor.value = false
  }
}
</script>

<style scoped>
.subeler-container {
  padding: 0;
}
.kpi-satiri {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}
.sube-arac {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}
.arac-bosluk {
  flex: 1 1 auto;
}
.sube-cipler {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.sube-cip {
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
.sube-cip:hover {
  border-color: var(--accent-border);
  color: var(--text-primary);
}
.sube-cip.aktif {
  background: var(--accent-soft, rgba(245, 158, 11, 0.16));
  border-color: var(--accent-border, rgba(245, 158, 11, 0.5));
  color: var(--accent);
}
.sube-cip i {
  font-size: 12px;
}
.sube-mobil {
  display: none;
}
.sube-skeleton {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.sube-mobil-kart {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px 14px;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: var(--bg-card);
}
.smk-ust {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.smk-ad {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  padding: 0;
  border: none;
  background: transparent;
  color: var(--text-primary);
  font-size: 13.5px;
  font-weight: 700;
  text-align: left;
  cursor: pointer;
}
.smk-ad:hover span {
  color: var(--accent);
}
.smk-ad i {
  color: var(--accent);
  flex-shrink: 0;
}
.smk-ad span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.smk-satir {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  font-size: 12.5px;
  color: var(--text-secondary);
}
.smk-satir > span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}
.smk-satir i {
  font-size: 11.5px;
  color: var(--text-muted);
}
.smk-alt {
  font-size: 12px;
  color: var(--text-muted);
}
.smk-adres {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
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
.w-full {
  width: 100%;
}
.zorunlu {
  color: var(--danger, #ef4444);
}
@media (max-width: 768px) {
  .sube-masa {
    display: none;
  }
  .sube-mobil {
    display: flex;
    flex-direction: column;
    gap: 10px;
  }
  .arac-bosluk {
    display: none;
  }
  .sube-cipler {
    flex: 1 1 100%;
    flex-wrap: nowrap;
    overflow-x: auto;
    padding-bottom: 2px;
  }
  .sube-cip {
    flex-shrink: 0;
  }
}
@media (prefers-reduced-motion: reduce) {
  .sube-cip,
  .smk-ad span {
    transition: none;
  }
}
</style>
