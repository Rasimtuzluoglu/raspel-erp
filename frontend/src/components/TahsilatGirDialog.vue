<template>
  <Dialog
    :visible="visible"
    modal
    header="Tahsilat Gir"
    :style="{ width: '520px', maxWidth: 'calc(100vw - 24px)' }"
    :dismissable-mask="false"
    @update:visible="$emit('update:visible', $event)"
  >
    <div class="tahsilat-form">
      <div class="form-panel">
        <div class="form-section-title">
          <i class="pi pi-user" /> Cari Hesap
        </div>
        <FormField label="Cari Hesap">
          <Select
            v-model="form.cariId"
            :options="efektifCariler"
            option-label="ad"
            option-value="cariId"
            :filter="true"
            :disabled="!!props.cari"
            placeholder="Cari seçin"
            class="w-full"
            @change="cariDegisti"
          />
        </FormField>
      </div>

      <div class="form-panel">
        <div class="form-section-title">
          <i class="pi pi-wallet" /> Ödeme
        </div>
        <div class="form-grid-2">
          <FormField label="Ödenecek Tutar (₺)">
            <InputNumber
              v-model="form.tutar"
              :min="0"
              :min-fraction-digits="2"
              :max-fraction-digits="2"
              :disabled="!form.cariId"
              placeholder="Tutar"
              class="w-full"
            />
          </FormField>
          <FormField label="Tahsilat Tarihi">
            <DatePicker
              v-model="form.hareketTarihi"
              show-icon
              date-format="dd/mm/yy"
              class="w-full"
            />
          </FormField>
        </div>

        <FormField label="Ödeme Yöntemi">
          <div class="yontem-btns">
            <button
              v-for="y in yontemler"
              :key="y.value"
              type="button"
              class="yontem-btn"
              :class="{ aktif: form.odemeYontemi === y.value }"
              @click="form.odemeYontemi = y.value"
            >
              <i :class="y.icon" />
              {{ y.label }}
            </button>
          </div>
        </FormField>

        <div
          v-if="form.odemeYontemi === 'TAKSIT'"
          class="taksit-panel"
        >
          <div class="form-grid-2">
            <FormField label="Taksit Çekilen Kurum">
              <Select
                v-model="form.taksitKurum"
                :options="kurumlar"
                :filter="true"
                editable
                placeholder="Banka / finans kurumu"
                option-label="ad"
                option-value="ad"
                class="w-full"
              />
            </FormField>
            <FormField label="Çekilen Taksit Tutarı (₺)">
              <InputNumber
                v-model="form.taksitTutar"
                :min="0"
                :min-fraction-digits="2"
                :max-fraction-digits="2"
                placeholder="Taksit tutarı"
                class="w-full"
              />
            </FormField>
          </div>
        </div>

        <FormField label="Açıklama">
          <Textarea
            v-model="form.aciklama"
            rows="2"
            placeholder="İsteğe bağlı not"
            class="w-full"
          />
        </FormField>
      </div>

      <div
        v-if="seciliCari"
        class="acik-faturalar"
      >
        <div class="form-section-title">
          <i class="pi pi-file" /> Açık Faturalar
        </div>
        <div class="acik-listesi">
          <div
            v-for="f in seciliCari.faturalar"
            :key="f.faturaId"
            class="acik-satir"
          >
            <div class="acik-bilgi">
              <strong>{{ f.faturaNumarasi }}</strong>
              <span>Vade: {{ formatDate(f.vadeTarihi) }}</span>
            </div>
            <div class="acik-tutar">
              {{ formatCurrency(f.kalanTutar) }}
            </div>
          </div>
          <div
            v-if="!seciliCari.faturalar?.length"
            class="acik-bos"
          >
            Açık fatura yok.
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <Button
        label="Vazgeç"
        icon="pi pi-times"
        class="p-button-text"
        @click="$emit('update:visible', false)"
      />
      <Button
        label="Tahsilatı Kaydet"
        icon="pi pi-check"
        class="p-button-success"
        :loading="kaydediliyor"
        :disabled="!gecerli"
        @click="kaydet"
      />
    </template>
  </Dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { tahsilatAPI, bankaAPI } from '../api/index.js'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { formatCurrency, formatDate } from '../utils/format.js'
import FormField from './FormField.vue'

const props = defineProps({
  visible: { type: Boolean, default: false },
  /** Cari özet listesi (Tahsilat sayfasından gelen ozet.cariler) */
  cariler: { type: Array, default: () => [] },
  /** İlk seçili cari id */
  baslangicCariId: { type: Number, default: null },
  /** Tek cari detayından geçilirse bu listeye eklenir (cari detayı kullanımı) */
  cari: { type: Object, default: null }
})

const efektifCariler = computed(() => {
  let liste = props.cariler.map((c) => ({ ...c, ad: c.ad || c.cariAd }))
  if (props.cari && !liste.find((c) => c.cariId === props.cari.id)) {
    const faturalar = props.cari.acikFaturalar || []
    const toplam = faturalar.length
      ? faturalar.reduce((s, f) => s + (Number(f.kalanTutar) || 0), 0)
      : Math.abs(Number(props.cari.bakiye) || 0)
    liste = [...liste, { cariId: props.cari.id, ad: props.cari.ad, toplamAlacak: toplam, faturalar }]
  }
  return liste
})

const emit = defineEmits(['update:visible', 'kaydedildi'])

const toastBildirim = useToastBildirim()

const yontemler = [
  { value: 'NAKIT', label: 'Nakit', icon: 'pi pi-money-bill' },
  { value: 'KART', label: 'Kart', icon: 'pi pi-credit-card' },
  { value: 'TAKSIT', label: 'Taksit', icon: 'pi pi-calendar' },
  { value: 'HAVALE', label: 'Havale', icon: 'pi pi-send' }
]

const kurumlar = ref([])
const kaydediliyor = ref(false)

const form = ref({
  cariId: props.baslangicCariId,
  tutar: null,
  hareketTarihi: new Date(),
  odemeYontemi: 'NAKIT',
  taksitKurum: null,
  taksitTutar: null,
  aciklama: ''
})

const seciliCari = computed(() => {
  const bulunan = efektifCariler.value.find((c) => c.cariId === form.value.cariId)
  if (bulunan) {
    // Cari detayından gelen açık faturalar varsa özet listesine bağla
    if (props.cari?.id === form.value.cariId && props.cari?.acikFaturalar?.length) {
      return { ...bulunan, faturalar: props.cari.acikFaturalar }
    }
    return bulunan
  }
  return props.cari && props.cari.id === form.value.cariId ? props.cari : null
})

const gecerli = computed(() => {
  const t = form.value.tutar
  const tutarOk = t && t > 0
  const kurumOk = form.value.odemeYontemi !== 'TAKSIT' ||
    (form.value.taksitKurum && form.value.taksitTutar && form.value.taksitTutar > 0)
  return !!(form.value.cariId && tutarOk && kurumOk)
})

watch(
  () => props.visible,
  async (open) => {
    if (open) {
      const hedefCariId = props.cari?.id || props.baslangicCariId
      if (hedefCariId) {
        form.value.cariId = hedefCariId
        const acikToplam = (props.cari?.acikFaturalar || [])
          .reduce((s, f) => s + (Number(f.kalanTutar) || 0), 0)
        form.value.tutar = acikToplam > 0 ? acikToplam
          : (props.cari?.bakiye != null ? Math.abs(Number(props.cari.bakiye)) : null)
      }
      if (!kurumlar.value.length) {
        try {
          const r = await bankaAPI.getAll()
          kurumlar.value = (r.data?.content || r.data || []).map((b) => ({
            ad: b.ad || b.adi || b.bankaAdi || String(b.id)
          }))
        } catch {
          kurumlar.value = []
        }
      }
    } else {
      form.value = {
        cariId: props.cari?.id || props.baslangicCariId,
        tutar: null,
        hareketTarihi: new Date(),
        odemeYontemi: 'NAKIT',
        taksitKurum: null,
        taksitTutar: null,
        aciklama: ''
      }
    }
  }
)

const cariDegisti = () => {
  form.value.tutar = null
  if (form.value.cariId && seciliCari.value?.toplamAlacak) {
    form.value.tutar = seciliCari.value.toplamAlacak
  }
}

const kaydet = async () => {
  if (!gecerli.value) return
  kaydediliyor.value = true
  try {
    await tahsilatAPI.gir({
      cariId: form.value.cariId,
      tutar: form.value.tutar,
      odemeYontemi: form.value.odemeYontemi,
      taksitKurum: form.value.odemeYontemi === 'TAKSIT' ? form.value.taksitKurum : null,
      taksitTutar: form.value.odemeYontemi === 'TAKSIT' ? form.value.taksitTutar : null,
      aciklama: form.value.aciklama || null,
      hareketTarihi: form.value.hareketTarihi ? form.value.hareketTarihi.toISOString().slice(0, 10) : null
    })
    toastBildirim.basarili('Tahsilat kaydedildi')
    emit('kaydedildi')
    emit('update:visible', false)
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || 'Tahsilat kaydedilemedi')
  } finally {
    kaydediliyor.value = false
  }
}
</script>

<style scoped>
.tahsilat-form {
  display: flex;
  flex-direction: column;
  gap: 18px;
}
.form-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.form-section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.4px;
  color: var(--text-secondary);
  padding-bottom: 8px;
  border-bottom: 1px solid var(--border);
}
.form-section-title i {
  color: var(--accent);
}
.form-grid-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
.w-full {
  width: 100%;
}
.yontem-btns {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
}
.yontem-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 10px;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: var(--bg-card);
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
}
.yontem-btn:hover {
  border-color: var(--accent);
  color: var(--text-primary);
}
.yontem-btn.aktif {
  background: rgba(59, 130, 246, 0.12);
  border-color: var(--accent);
  color: var(--accent);
}
.taksit-panel {
  background: rgba(139, 92, 246, 0.08);
  border: 1px solid rgba(139, 92, 246, 0.25);
  border-radius: 12px;
  padding: 12px;
}
.acik-faturalar {
  display: flex;
  flex-direction: column;
}
.acik-listesi {
  background: var(--bg-secondary);
  border-radius: 10px;
  padding: 8px;
  max-height: 180px;
  overflow-y: auto;
}
.acik-satir {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border-bottom: 1px solid var(--border);
  font-size: 13px;
}
.acik-satir:last-child {
  border-bottom: none;
}
.acik-bilgi {
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.acik-bilgi span {
  font-size: 11px;
  color: var(--text-muted);
}
.acik-tutar {
  font-weight: 700;
  white-space: nowrap;
}
.acik-bos {
  text-align: center;
  color: var(--text-muted);
  padding: 16px;
  font-size: 13px;
}
</style>
