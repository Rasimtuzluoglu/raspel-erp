<template>
  <div
    v-if="!ilkYukleme"
    class="table-container"
  >
    <AppDataTable
      :selection="secili"
      :value="cariHesaplar"
      data-key="id"
      striped-rows
      :loading="yukleniyor"
      :size="yogunluk === 'compact' ? 'small' : 'normal'"
      :lazy="true"
      :total-records="toplamKayit"
      :rows="sayfaBoyutu"
      :paginator="true"
      :sort-field="siralama.field"
      :sort-order="siralama.order"
      paginator-template="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink CurrentPageReport RowsPerPageDropdown"
      :rows-per-page-options="[10, 20, 50]"
      :current-page-report-template="'{first} - {last} ({totalRecords} ' + t('common.recordsWord') + ')'"
      gorunum-anahtari="cari-hesaplar"
      @page="sayfaDegistiEmit"
      @sort="siralaEmit"
      @update:selection="seciliGuncelle"
    >
      <Column
        selection-mode="multiple"
        header-style="width: 3rem"
        class="cari-secim-kolon"
      />
      <Column
        v-if="kolonlar[0].visible"
        field="id"
        header="ID"
        style="width: 60px"
      />
      <Column
        v-if="kolonlar[1].visible"
        field="ad"
        :header="t('cariHesaplar.ad')"
        sortable
        class="cari-ad-kolon"
        style="width: 240px"
      >
        <template #body="s">
          <div class="cari-ad-hucre">
            <img
              v-if="s.data.fotoThumbUrl || s.data.fotoUrl"
              :src="s.data.fotoThumbUrl || s.data.fotoUrl"
              class="cari-thumb"
              alt=""
              loading="lazy"
              decoding="async"
            >
            <span
              v-else
              class="cari-thumb-yok"
            ><i class="pi pi-user" /></span>
            <span>{{ s.data.ad }}</span>
          </div>
        </template>
      </Column>
      <Column
        v-if="kolonlar[2].visible"
        field="tur"
        :header="t('cariHesaplar.tur')"
        sortable
        style="width: 100px"
      >
        <template #body="s">
          <Tag
            :value="s.data.tur || '-'"
            :severity="s.data.tur === 'Musteri' ? 'info' : s.data.tur === 'Tedarikci' ? 'warn' : 'secondary'"
          />
        </template>
      </Column>
      <Column
        v-if="kolonlar[3].visible"
        field="yetkiliKisi"
        :header="t('cariHesaplar.yetkili')"
        style="width: 130px"
      />
      <Column
        v-if="kolonlar[4].visible"
        field="telefon"
        :header="t('cariHesaplar.telefon')"
        style="width: 130px"
      >
        <template #body="s">
          <span
            v-if="s.data.telefon"
            class="kopyalanabilir"
            @click="kopyala(s.data.telefon, t('cariHesaplar.telefonKopyalandi'))"
          >
            {{ s.data.telefon }} <i class="pi pi-copy kopyala-ikon" />
          </span>
          <span v-else>-</span>
        </template>
      </Column>
      <Column
        v-if="kolonlar[5].visible"
        field="krediLimiti"
        :header="t('cariHesaplar.krediLimiti')"
        sortable
        style="width: 120px"
      >
        <template #body="s">
          {{ s.data.krediLimiti ? formatCurrency(s.data.krediLimiti) : '-' }}
        </template>
      </Column>
      <Column
        v-if="kolonlar[6].visible"
        field="odemeVadesi"
        :header="t('cariHesaplar.vadeGun')"
        sortable
        style="width: 80px"
      />
      <Column
        v-if="kolonlar[7].visible"
        field="temsilciAd"
        :header="t('cariHesaplar.satisTemsilcisi')"
        style="width: 140px"
      >
        <template #body="slotProps">
          <!-- Temsilci performans raporu bu alana gÃ¶re gruplar; atanmamÄ±ÅŸ
                 cariler raporda tek satÄ±rda toplanÄ±yor. -->
          <span v-if="slotProps.data.temsilciAd">{{ slotProps.data.temsilciAd }}</span>
          <span
            v-else
            class="gizli-veri"
          >-</span>
        </template>
      </Column>
      <Column
        v-if="kolonlar[8].visible"
        field="bakiye"
        :header="t('cariHesaplar.bakiye')"
        class="sayisal"
        sortable
        style="width: 140px"
      >
        <template #body="slotProps">
          <!-- Bakiye iÅŸareti kuralÄ±: negatif = cari bize borÃ§lu (ALACAK).
                 Ã–nceden `>= 0` kontrolÃ¼ borÃ§lu mÃ¼ÅŸteriyi kÄ±rmÄ±zÄ± "alacak"
                 rozetiyle gÃ¶steriyordu. -->
          <span
            class="bakiye-rozet gizli-veri"
            :class="odenecekMi(slotProps.data.bakiye) ? 'borc' : 'alacak'"
            :title="odenecekMi(slotProps.data.bakiye) ? t('cariHesaplar.bizeBorclu') : t('cariHesaplar.bizeAlacakli')"
          >
            <i :class="odenecekMi(slotProps.data.bakiye) ? 'pi pi-arrow-up-right' : 'pi pi-arrow-down-left'" />
            {{ formatCurrency(Math.abs(slotProps.data.bakiye || 0)) }}
          </span>
        </template>
      </Column>
      <Column
        v-if="kolonlar[9].visible"
        field="etiketler"
        :header="t('cariHesaplar.etiketler')"
        style="width: 160px"
      >
        <template #body="slotProps">
          <span
            v-if="slotProps.data.etiketler"
            class="etiket-rozet"
          >{{ slotProps.data.etiketler }}</span>
          <span
            v-else
            class="gizli-veri"
          >-</span>
        </template>
      </Column>
      <Column
        :header="t('common.actions')"
        class="cari-islem-kolon"
        style="width: 110px"
      >
        <template #body="slotProps">
          <div class="satir-islemler">
            <Button
              icon="pi pi-money-bill"
              class="p-button-rounded p-button-success p-button-sm"
              :title="t('cariHesaplar.tahsilat')"
              :aria-label="t('cariHesaplar.tahsilat')"
              data-test="cari-tahsilat"
              @click="tahsilatEmit(slotProps.data)"
            />
            <SatirEylemleri
              :gorunur="{ duzenle: false, cogalt: false, sil: false }"
              :items="eylemler(slotProps.data)"
              :baslik="slotProps.data.ad"
            />
          </div>
        </template>
      </Column>
      <template #batch-actions>
        <Button
          :label="t('cariHesaplar.topluEposta')"
          icon="pi pi-envelope"
          class="p-button-sm p-button-info"
          @click="topluEmailEmit"
        />
        <!--
              REDTEAM/Faz3.2: `v-permission="'CARI_DELETE'"` burada
              GEREKSIZDI ve yanlisti: backend `CariHesapController:160`
              toplu silmeyi `hasRole('ADMIN')` ile koruyor (tekil silme ise
              `CARI_DELETE` kabul ediyor). `v-if="isAdmin"` zaten vardi ve
              dogru kontrol bu; iki kontrol yan yana birakildiginda biri
              sessizce etkisiz kaliyordu. Yanlis olan silindi.
            -->
        <Button
          v-if="isAdmin"
          :label="t('cariHesaplar.topluSil')"
          icon="pi pi-trash"
          class="p-button-sm p-button-danger"
          @click="batchSilEmit"
        />
        <Button
          :label="t('cariHesaplar.csvAktar')"
          icon="pi pi-download"
          class="p-button-sm p-button-outlined"
          @click="csvEmit"
        />
      </template>
      <template #empty>
        <EmptyState
          v-if="cariHesaplar.length === 0"
          :message="t('cariHesaplar.empty')"
          :sub-message="t('cariHesaplar.emptyHint')"
          illustrasyon="liste"
          icon="pi pi-users"
          :action-label="t('cariHesaplar.emptyAction')"
          action-icon="pi pi-plus"
          @action="yeniEmit"
        />
      </template>
    </AppDataTable>
  </div>

  <!-- Kucuk ekran: tablo yerine kart listesi (CSS ile <768px'te gosterilir) -->
  <div
    v-if="!ilkYukleme"
    class="cari-kart-liste"
  >
    <div
      v-for="c in (cariHesaplar)"
      :key="c.id"
      class="cari-kart"
    >
      <div class="cari-kart-ust">
        <div class="cari-ad-hucre">
          <img
            v-if="c.fotoThumbUrl || c.fotoUrl"
            :src="c.fotoThumbUrl || c.fotoUrl"
            class="cari-thumb"
            alt=""
            loading="lazy"
            decoding="async"
          >
          <span
            v-else
            class="cari-thumb-yok"
          ><i class="pi pi-user" /></span>
          <div class="cari-kart-baslik">
            <strong>{{ c.ad }}</strong>
            <Tag
              :value="c.tur || '-'"
              :severity="c.tur === 'Musteri' ? 'info' : c.tur === 'Tedarikci' ? 'warn' : 'secondary'"
            />
          </div>
        </div>
        <span
          class="bakiye-rozet"
          :class="odenecekMi(c.bakiye) ? 'borc' : 'alacak'"
        >
          <i :class="odenecekMi(c.bakiye) ? 'pi pi-arrow-up-right' : 'pi pi-arrow-down-left'" />
          {{ formatCurrency(Math.abs(c.bakiye || 0)) }}
        </span>
      </div>
      <div class="cari-kart-alt">
        <span
          v-if="c.telefon"
          class="kopyalanabilir"
          @click="kopyala(c.telefon, t('cariHesaplar.telefonKopyalandi'))"
        >
          <i class="pi pi-phone" /> {{ c.telefon }}
        </span>
        <span v-if="c.yetkiliKisi"><i class="pi pi-user" /> {{ c.yetkiliKisi }}</span>
        <span v-if="c.temsilciAd"><i class="pi pi-briefcase" /> {{ c.temsilciAd }}</span>
      </div>
      <div class="cari-kart-islemler">
        <Button
          icon="pi pi-money-bill"
          class="p-button-rounded p-button-success p-button-sm"
          :title="t('cariHesaplar.tahsilat')"
          :aria-label="t('cariHesaplar.tahsilat')"
          data-test="cari-tahsilat"
          @click="tahsilatEmit(c)"
        />
        <SatirEylemleri
          :gorunur="{ duzenle: false, cogalt: false, sil: false }"
          :items="eylemler(c)"
          :baslik="c.ad"
        />
      </div>
    </div>

    <EmptyState
      v-if="(cariHesaplar).length === 0"
      :message="t('cariHesaplar.empty')"
      :sub-message="t('cariHesaplar.emptyHint')"
      icon="pi pi-users"
      :action-label="t('cariHesaplar.emptyAction')"
      action-icon="pi pi-plus"
      @action="yeniEmit"
    />

    <Paginator
      v-if="(toplamKayit || 0) > sayfaBoyutu"
      :first="sayfa * sayfaBoyutu"
      :rows="sayfaBoyutu"
      :total-records="toplamKayit"
      :rows-per-page-options="[10, 20, 50]"
      class="cari-kart-sayfalama"
      @page="sayfaDegistiEmit"
    />
  </div>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import { formatCurrency } from '../../utils/format.js'

defineProps({
  cariHesaplar: { type: Array, default: () => [] },
  toplamKayit: { type: [Number, String], default: 0 },
  secili: { type: Array, default: () => [] },
  yukleniyor: { type: Boolean, default: false },
  ilkYukleme: { type: Boolean, default: false },
  yogunluk: { type: String, default: 'comfortable' },
  sayfa: { type: Number, default: 0 },
  sayfaBoyutu: { type: Number, default: 10 },
  siralama: { type: Object, default: () => ({ field: 'ad', order: 1 }) },
  kolonlar: { type: Array, default: () => [] },
  isAdmin: { type: Boolean, default: false },
  eylemler: { type: Function, default: () => [] },
  kopyala: { type: Function, default: () => {} }
})

const emit = defineEmits([
  'update:secili',
  'sayfa-degisti',
  'sirala',
  'tahsilat',
  'toplu-email',
  'toplu-sil',
  'csv',
  'yeni'
])

const { t } = useI18n()

const odenecekMi = (bakiye) => Number(bakiye || 0) > 0
const seciliGuncelle = (deger) => emit('update:secili', deger)
const sayfaDegistiEmit = (olay) => emit('sayfa-degisti', olay)
const siralaEmit = (olay) => emit('sirala', olay)
const tahsilatEmit = (cari) => emit('tahsilat', cari)
const batchSilEmit = () => emit('toplu-sil')
const csvEmit = () => emit('csv')
const topluEmailEmit = () => emit('toplu-email')
const yeniEmit = () => emit('yeni')
</script>

<style scoped>
.cari-ad-hucre {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}
.cari-ad-hucre > span:last-child {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.cari-thumb {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid var(--border);
  flex: 0 0 auto;
}
.cari-thumb-yok {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 1px dashed var(--border);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--text-muted);
  font-size: 13px;
  flex: 0 0 auto;
}
.bakiye-rozet {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 3px 10px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 700;
}
.bakiye-rozet.alacak {
  background: var(--success-soft);
  color: var(--success);
}
.bakiye-rozet.borc {
  background: var(--danger-soft);
  color: var(--danger);
}
.cari-kart-liste {
  display: none;
}
.cari-kart {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 14px;
  margin-bottom: 10px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
}
.cari-kart-ust {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}
.cari-kart-baslik {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}
.cari-kart-baslik strong {
  color: var(--text-primary);
}
.cari-kart-alt {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  font-size: 13px;
  color: var(--text-secondary);
}
.cari-kart-alt i {
  color: var(--text-muted);
  margin-right: 2px;
}
.cari-kart-islemler {
  display: flex;
  align-items: center;
  gap: 6px;
  padding-top: 10px;
  border-top: 1px solid var(--border);
}
.cari-kart-sayfalama {
  justify-content: center;
  margin-top: 8px;
}
@media (max-width: 768px) {
  .table-container {
    display: none;
  }
  .cari-kart-liste {
    display: block;
  }
}
.table-container {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 14px;
}
.table-container :deep(.cari-secim-kolon),
.table-container :deep(.cari-ad-kolon),
.table-container :deep(.cari-islem-kolon) {
  position: sticky;
  z-index: 2;
  background: var(--bg-card);
}
.table-container :deep(.cari-secim-kolon) {
  left: 0;
  width: 56px;
  min-width: 56px;
  max-width: 56px;
  padding: 8px;
}
.table-container :deep(.cari-ad-kolon) {
  left: 56px;
  box-shadow: 8px 0 12px -12px rgba(0, 0, 0, 0.8);
}
.table-container :deep(.cari-islem-kolon) {
  right: 0;
  box-shadow: -8px 0 12px -12px rgba(0, 0, 0, 0.8);
}
.table-container :deep(thead .cari-secim-kolon),
.table-container :deep(thead .cari-ad-kolon),
.table-container :deep(thead .cari-islem-kolon) {
  z-index: 3;
}
.table-container :deep(.cari-secim-kolon .p-checkbox) {
  width: 40px;
  height: 40px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.table-container :deep(.cari-secim-kolon .p-checkbox-box) {
  width: 20px;
  height: 20px;
}
.table-container :deep(.cari-islem-kolon .satir-islemler) {
  flex-wrap: nowrap;
}
.satir-islemler {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: nowrap;
}
.kopyalanabilir {
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  white-space: nowrap;
}
.kopyalanabilir:hover {
  color: var(--accent);
}
.kopyala-ikon {
  font-size: 11px;
  opacity: 0.5;
}
.kopyalanabilir:hover .kopyala-ikon {
  opacity: 1;
}
</style>
