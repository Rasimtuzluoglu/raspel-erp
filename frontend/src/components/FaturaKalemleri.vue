<template>
  <div class="kalem-bilesen">
    <!-- Kalem listesi: genis ekranda tablo, dar ekranda kart -->
    <div
      v-if="!darEkran"
      class="kalem-tablo-kapsam"
    >
      <DataTable
        :value="kalemler"
        striped-rows
      >
        <template #empty>
          <EmptyState />
        </template>
        <Column
          header="#"
          style="width: 40px"
        >
          <template #body="s">
            {{ s.index + 1 }}
          </template>
        </Column>
        <Column :header="$t('faturaKalemleri.aciklamaZorunlu')">
          <template #body="s">
            <!-- Stok arama destegi verilirse AutoComplete ile stok kodu/adi aranir;
                 secilince satir stok bilgisiyle (emit) doldurulur. -->
            <AutoComplete
              v-if="stokArama"
              v-model="s.data.aciklama"
              :suggestions="oneriler"
              option-label="etiket"
              :placeholder="$t('faturaKalemleri.aciklamaPlaceholder')"
              class="w-full"
              :force-selection="false"
              dropdown
              @complete="onAra($event)"
              @option-select="(e) => $emit('stok-sec', { index: s.index, stok: e.value.stok })"
            />
            <InputText
              v-else
              v-model="s.data.aciklama"
              :placeholder="$t('faturaKalemleri.aciklamaPlaceholder')"
              class="w-full"
            />
          </template>
        </Column>
        <Column
          :header="$t('faturaKalemleri.adetZorunlu')"
          style="width: 110px"
        >
          <template #body="s">
            <InputNumber
              v-model="s.data.adet"
              :min="1"
              class="w-full sayi-girdi"
              @focus="sec($event)"
            />
          </template>
        </Column>
        <Column
          :header="$t('faturaKalemleri.birimFiyatZorunlu')"
          style="width: 150px"
        >
          <template #body="s">
            <InputNumber
              v-model="s.data.birimFiyat"
              :min="0"
              :min-fraction-digits="2"
              :max-fraction-digits="2"
              class="w-full sayi-girdi"
              @focus="sec($event)"
            />
          </template>
        </Column>
        <Column
          :header="$t('faturaKalemleri.iskonto')"
          style="width: 100px"
        >
          <template #body="s">
            <InputNumber
              v-model="s.data.iskontoOrani"
              :min="0"
              :max="100"
              :min-fraction-digits="0"
              class="w-full sayi-girdi"
              @focus="sec($event)"
            />
          </template>
        </Column>
        <Column
          :header="$t('faturaKalemleri.kdv')"
          style="width: 90px"
        >
          <template #body="s">
            <Dropdown
              v-model="s.data.kdvOrani"
              :options="kdvSecenekleri"
              class="w-full"
            />
          </template>
        </Column>
        <Column
          :header="$t('faturaKalemleri.tutar')"
          style="width: 140px"
          body-class="tutar-hucre"
        >
          <template #body="s">
            {{ formatCurrency(kalemTutar(s.data)) }}
          </template>
        </Column>
        <Column
          header=""
          style="width: 96px"
        >
          <template #body="s">
            <div class="satir-aksiyon">
              <Button
                icon="pi pi-copy"
                class="p-button-rounded p-button-text p-button-sm"
                :aria-label="$t('faturaKalemleri.cogalt')"
                :title="$t('faturaKalemleri.cogalt')"
                @click="cogalt(s.data)"
              />
              <Button
                icon="pi pi-trash"
                class="p-button-rounded p-button-danger p-button-sm"
                :aria-label="$t('common.delete')"
                :title="$t('common.delete')"
                @click="$emit('remove', s.index)"
              />
            </div>
          </template>
        </Column>
      </DataTable>
    </div>

    <!-- Dar ekran: kart duzeni -->
    <div
      v-else
      class="kalem-kartlar"
    >
      <div
        v-if="!kalemler.length"
        class="kalem-yok"
      >
        <i class="pi pi-inbox" />
        <span>{{ $t('faturaKalemleri.bosKalem') }}</span>
      </div>
      <div
        v-for="(k, idx) in kalemler"
        :key="idx"
        class="kalem-kart"
      >
        <div class="kalem-kart-ust">
          <AutoComplete
            v-if="stokArama"
            v-model="k.aciklama"
            :suggestions="oneriler"
            option-label="etiket"
            :placeholder="$t('faturaKalemleri.aciklamaPlaceholder')"
            class="w-full"
            :force-selection="false"
            dropdown
            @complete="onAra($event)"
            @option-select="(e) => $emit('stok-sec', { index: idx, stok: e.value.stok })"
          />
          <InputText
            v-else
            v-model="k.aciklama"
            :placeholder="$t('faturaKalemleri.aciklamaPlaceholder')"
            class="w-full"
          />
          <div class="kalem-kart-aksiyon">
            <Button
              icon="pi pi-copy"
              class="p-button-rounded p-button-text p-button-sm"
              :title="$t('faturaKalemleri.cogalt')"
              @click="cogalt(k)"
            />
            <Button
              icon="pi pi-trash"
              class="p-button-rounded p-button-danger p-button-sm"
              :title="$t('common.delete')"
              @click="$emit('remove', idx)"
            />
          </div>
        </div>
        <div class="kalem-kart-grid">
          <label>
            <small>{{ $t('faturaKalemleri.adetZorunlu') }}</small>
            <InputNumber
              v-model="k.adet"
              :min="1"
              class="w-full sayi-girdi"
              @focus="sec($event)"
            />
          </label>
          <label>
            <small>{{ $t('faturaKalemleri.birimFiyatZorunlu') }}</small>
            <InputNumber
              v-model="k.birimFiyat"
              :min="0"
              :min-fraction-digits="2"
              :max-fraction-digits="2"
              class="w-full sayi-girdi"
              @focus="sec($event)"
            />
          </label>
          <label>
            <small>{{ $t('faturaKalemleri.iskonto') }}</small>
            <InputNumber
              v-model="k.iskontoOrani"
              :min="0"
              :max="100"
              :min-fraction-digits="0"
              class="w-full sayi-girdi"
              @focus="sec($event)"
            />
          </label>
          <label>
            <small>{{ $t('faturaKalemleri.kdv') }}</small>
            <Dropdown
              v-model="k.kdvOrani"
              :options="kdvSecenekleri"
              class="w-full"
            />
          </label>
        </div>
        <div class="kalem-kart-tutar">
          <span>{{ $t('faturaKalemleri.tutar') }}</span>
          <strong>{{ formatCurrency(kalemTutar(k)) }}</strong>
        </div>
      </div>
    </div>

    <!-- Tek ekleme yolu: hizli kalem ekleme satiri -->
    <div class="hizli-kalem">
      <div class="hizli-kalem-baslik">
        <i class="pi pi-plus-circle" /> {{ $t('faturaKalemleri.hizliEkle') }}
      </div>
      <div class="hizli-kalem-grid">
        <AutoComplete
          v-model="yeni.stok"
          :suggestions="oneriler"
          option-label="etiket"
          :placeholder="$t('faturaKalemleri.stokAra')"
          class="hizli-stok"
          :force-selection="false"
          dropdown
          @complete="onAra($event)"
          @option-select="stokSecildi"
        >
          <template #option="slotProps">
            <div class="stok-opsiyon">
              <span class="stok-opsiyon-kod">{{ slotProps.option.stok.stokKodu || slotProps.option.stok.barkod }}</span>
              <span class="stok-opsiyon-ad">{{ slotProps.option.stok.ad }}</span>
              <span class="stok-opsiyon-fiyat">{{ formatCurrency(slotProps.option.stok.satisFiyati || slotProps.option.stok.fiyat) }}</span>
            </div>
          </template>
        </AutoComplete>
        <InputNumber
          v-model="yeni.adet"
          :min="1"
          :placeholder="$t('faturaKalemleri.adetZorunlu')"
          class="hizli-adet sayi-girdi"
          @focus="sec($event)"
          @keyup.enter="kalemEkle"
        />
        <InputNumber
          v-model="yeni.fiyat"
          :min="0"
          :min-fraction-digits="2"
          :max-fraction-digits="2"
          :placeholder="$t('faturaKalemleri.birimFiyatZorunlu')"
          class="hizli-fiyat sayi-girdi"
          @focus="sec($event)"
          @keyup.enter="kalemEkle"
        />
        <Dropdown
          v-model="yeni.kdv"
          :options="kdvSecenekleri"
          class="hizli-kdv"
        />
        <Button
          :label="$t('faturaKalemleri.ekle')"
          icon="pi pi-plus"
          class="p-button-success hizli-ekle-btn"
          @click="kalemEkle"
        />
      </div>
      <small class="hizli-kalem-ipucu">{{ $t('faturaKalemleri.hizliEkleIpucu') }}</small>
    </div>

    <div class="summary-box">
      <div class="summary-row">
        <span>{{ $t('faturaKalemleri.araToplam') }}</span><span>{{ formatCurrency(araToplam) }}</span>
      </div>
      <div class="summary-row">
        <span>{{ $t('faturaKalemleri.kdvLabel') }}</span><span>{{ formatCurrency(kdvToplam) }}</span>
      </div>
      <div class="summary-row total">
        <span>{{ $t('faturaKalemleri.genelToplam') }}</span><span>{{ formatCurrency(genelToplam) }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { formatCurrency } from '../utils/format.js'
import { kalemTutar } from '../utils/faturaHesapla.js'
import { stokAPI } from '../api/index.js'

const props = defineProps({
  kalemler: { type: Array, required: true },
  araToplam: { type: Number, default: 0 },
  kdvToplam: { type: Number, default: 0 },
  genelToplam: { type: Number, default: 0 },
  /** KDV orani secenekleri (varsayilan [0,1,8,10,18,20]). */
  kdvSecenekleri: { type: Array, default: () => [0, 1, 8, 10, 18, 20] },
  /** true ise aciklama alani satir-ici stok arama (AutoComplete) olur. */
  stokArama: { type: Boolean, default: false },
  /**
   * Yeni kalemde KDV yoksa kullanilacak varsayilan oran. Varsayilan 0'dir;
   * stok secilirse stogun KDV'si (tanimliysa) uygulanir.
   */
  kdvVarsayilan: { type: Number, default: 0 }
})

const emit = defineEmits(['add', 'remove', 'stok-sec'])

const oneriler = ref([])
let aramaZamanlayici = null

// Aktif kalem listesinin (props) ilk gecerli KDV varsayilaniyla baslamasi icin.
const yeni = reactive({ stok: null, adet: 1, fiyat: 0, kdv: props.kdvVarsayilan })

// Sayisal girdide odaklaninca mevcut degeri sec (hizli ustune yazma).
const sec = (event) => {
  try {
    event?.target?.select?.()
  } catch {
    /* yoksay */
  }
}

// Dar ekran tespiti (<=720px) icin kart duzeni.
const darEkran = ref(false)
let mq = null
const mqGuncelle = (e) => {
  darEkran.value = e.matches
}
onMounted(() => {
  try {
    mq = window.matchMedia('(max-width: 720px)')
    darEkran.value = mq.matches
    mq.addEventListener('change', mqGuncelle)
  } catch {
    darEkran.value = false
  }
})
onUnmounted(() => {
  try {
    mq?.removeEventListener('change', mqGuncelle)
  } catch {
    /* yoksay */
  }
})

// Sunucu tarafi stok arama (debounce). Oneri etiketi stok kodu + ad.
const onAra = (event) => {
  const q = (event?.query || '').trim()
  if (aramaZamanlayici) clearTimeout(aramaZamanlayici)
  if (q.length < 2) {
    oneriler.value = []
    return
  }
  aramaZamanlayici = setTimeout(async () => {
    try {
      const r = await stokAPI.ara(q)
      const liste = Array.isArray(r.data) ? r.data : (r.data?.content || [])
      oneriler.value = liste.map((s) => ({
        etiket: `${s.stokKodu ? '[' + s.stokKodu + '] ' : ''}${s.ad}`,
        stok: s
      }))
    } catch {
      oneriler.value = []
    }
  }, 250)
}

// Hizli ekleme satirinda stok secilince adet/fiyat/KDV otomatik dolar.
const stokSecildi = (e) => {
  const s = e?.value?.stok
  if (!s) return
  yeni.stok = s
  yeni.fiyat = Number(s.satisFiyati || s.fiyat || 0)
  yeni.kdv = s.kdvOrani != null ? Number(s.kdvOrani) : props.kdvVarsayilan
}

const kalemEkle = () => {
  const s = yeni.stok
  emit('add', {
    stokId: s?.id ?? null,
    aciklama: s?.ad ?? '',
    adet: yeni.adet || 1,
    birimFiyat: yeni.fiyat || 0,
    iskontoOrani: 0,
    kdvOrani: yeni.kdv ?? props.kdvVarsayilan
  })
  // Satir ekleme hizli olsun: stok temizlenir, adet 1'e doner, KDV varsayilan kalir.
  yeni.stok = null
  yeni.adet = 1
  yeni.fiyat = 0
}

// Satiri kopyala: mevcut degerler korunur, kayit kimligi tasinmaz (yeni satir).
const cogalt = (kalem) => {
  const kopya = { ...kalem }
  delete kopya.id
  emit('add', kopya)
}
</script>

<style scoped>
.kalem-tablo-kapsam {
  overflow-x: auto;
}
.satir-aksiyon {
  display: flex;
  gap: 2px;
  justify-content: flex-end;
}
:deep(.sayi-girdi .p-inputnumber-input) {
  text-align: right;
}
:deep(.tutar-hucre) {
  text-align: right;
  font-variant-numeric: tabular-nums;
}
:deep(.p-autocomplete),
:deep(.p-dropdown) {
  width: 100%;
}

/* Hizli kalem ekleme */
.hizli-kalem {
  margin-top: 12px;
  padding: 12px;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: var(--bg-primary);
}
.hizli-kalem-baslik {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 700;
  color: var(--text-secondary);
  margin-bottom: 10px;
}
.hizli-kalem-baslik i {
  color: var(--accent);
}
.hizli-kalem-grid {
  display: grid;
  grid-template-columns: minmax(200px, 1fr) 90px 130px 90px auto;
  gap: 8px;
  align-items: center;
}
.hizli-ekle-btn {
  white-space: nowrap;
}
.hizli-kalem-ipucu {
  display: block;
  margin-top: 8px;
  color: var(--text-muted);
  font-size: 11.5px;
}
.stok-opsiyon {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}
.stok-opsiyon-kod {
  font-size: 11px;
  color: var(--text-muted);
  font-variant-numeric: tabular-nums;
}
.stok-opsiyon-ad {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.stok-opsiyon-fiyat {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
}

/* Dar ekran kart duzeni */
.kalem-kartlar {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.kalem-yok {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 18px;
  color: var(--text-muted);
  font-size: 13px;
  border: 1px dashed var(--border);
  border-radius: 12px;
}
.kalem-kart {
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 10px;
  background: var(--bg-primary);
}
.kalem-kart-ust {
  display: flex;
  align-items: center;
  gap: 8px;
}
.kalem-kart-aksiyon {
  display: flex;
  gap: 2px;
  flex-shrink: 0;
}
.kalem-kart-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  margin-top: 8px;
}
.kalem-kart-grid label {
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.kalem-kart-grid small {
  color: var(--text-muted);
  font-size: 11px;
}
.kalem-kart-tutar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid var(--border);
  font-size: 13px;
  color: var(--text-secondary);
}
.kalem-kart-tutar strong {
  color: var(--text-primary);
  font-variant-numeric: tabular-nums;
}

@media (max-width: 720px) {
  .hizli-kalem-grid {
    grid-template-columns: 1fr 1fr;
  }
  .hizli-stok {
    grid-column: 1 / -1;
  }
  .hizli-ekle-btn {
    grid-column: 1 / -1;
  }
}

.summary-box {
  background: var(--border);
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 15px;
  margin-top: 15px;
}
.summary-row {
  display: flex;
  justify-content: space-between;
  padding: 5px 0;
  font-size: 14px;
  color: var(--text-secondary);
}
.summary-row.total {
  font-weight: 700;
  font-size: 18px;
  border-top: 2px solid var(--accent);
  margin-top: 5px;
  padding-top: 10px;
  color: var(--text-primary);
}
</style>
