<template>
  <div class="pos-sayfasi">
    <div class="pos-baslik">
      <h1><i class="pi pi-credit-card" /> POS Terminalleri</h1>
      <Button
        label="Yeni POS"
        icon="pi pi-plus"
        class="p-button-sm"
        @click="dialogAc(null)"
      />
    </div>

    <!-- POS Özeti -->
    <div class="pos-ozet-grid">
      <div
        v-for="o in ozetler"
        :key="o.posId"
        class="pos-ozet-kart"
      >
        <div class="ozet-ust">
          <i class="pi pi-credit-card" />
          <div>
            <strong>{{ o.posAd }}</strong>
            <small v-if="o.bankaAd">{{ o.bankaAd }}</small>
          </div>
        </div>
        <div class="ozet-degerler">
          <span>Bugün: <strong>{{ formatPara(o.bugunTutar) }}</strong></span>
          <span>Toplam: <strong>{{ formatPara(o.toplamTutar) }}</strong></span>
        </div>
        <div class="ozet-komisyon">
          Komisyon (bugün): {{ formatPara(o.bugunKomisyon) }} · Toplam: {{ formatPara(o.toplamKomisyon) }}
        </div>
      </div>
      <div
        v-if="!ozetler.length && !yukleniyor"
        class="bos"
      >
        Henüz POS terminali yok.
      </div>
    </div>

    <!-- POS Listesi -->
    <div class="pos-listesi">
      <div
        v-for="p in terminaller"
        :key="p.id"
        class="pos-kart"
      >
        <div class="pos-kart-bilgi">
          <strong>{{ p.ad }}</strong>
          <span class="pos-banka">{{ p.bankaAd || 'Banka atanmamış' }}</span>
          <span class="pos-komisyon">Komisyon: %{{ p.komisyonOrani ?? 0 }}</span>
        </div>
        <Tag
          :value="p.aktif ? 'Aktif' : 'Pasif'"
          :severity="p.aktif ? 'success' : 'danger'"
        />
        <Button
          icon="pi pi-users"
          class="p-button-rounded p-button-text p-button-sm"
          title="Müşteri Detayı"
          @click="musteriDetayAc(p)"
        />
        <Button
          icon="pi pi-pencil"
          class="p-button-rounded p-button-text p-button-sm"
          @click="dialogAc(p)"
        />
        <Button
          icon="pi pi-trash"
          class="p-button-rounded p-button-text p-button-sm p-button-danger"
          @click="sil(p)"
        />
      </div>
    </div>

    <Dialog
      v-model:visible="dialog"
      :header="duzenlenenId ? 'POS Düzenle' : 'Yeni POS Terminali'"
      :modal="true"
      :style="{ width: '440px' }"
    >
      <div class="pos-form">
        <div class="field">
          <label>POS Adı *</label>
          <InputText
            v-model="form.ad"
            placeholder="Örn: Halkbank POS"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>Banka</label>
          <Dropdown
            v-model="form.bankaId"
            :options="bankalar"
            option-label="ad"
            option-value="id"
            filter
            placeholder="Banka seçin"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>Komisyon Oranı (%)</label>
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
          <label>Aktif</label>
          <InputSwitch v-model="form.aktif" />
        </div>
      </div>
      <template #footer>
        <Button
          label="İptal"
          class="p-button-text"
          @click="dialog = false"
        />
        <Button
          label="Kaydet"
          icon="pi pi-check"
          :loading="kaydediliyor"
          @click="kaydet"
        />
      </template>
    </Dialog>

    <Dialog
      v-model:visible="musteriDialog"
      :header="'Müşteri Detayı — ' + (seciliPos?.ad || '')"
      :modal="true"
      :style="{ width: '440px' }"
    >
      <div
        v-if="!musteriler.length"
        class="bos"
      >
        Bu POS'tan henüz çekim yapılmamış.
      </div>
      <div
        v-for="m in musteriler"
        :key="m.cariId"
        class="musteri-satir"
      >
        <span>{{ m.cariAd }}</span>
        <strong>{{ formatPara(m.toplamTutar) }}</strong>
      </div>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { posAPI, bankaAPI } from '../api/index.js'
import { useToastBildirim } from '../composables/useToastBildirim.js'

const toastBildirim = useToastBildirim()

const terminaller = ref([])
const ozetler = ref([])
const bankalar = ref([])
const dialog = ref(false)
const duzenlenenId = ref(null)
const musteriDialog = ref(false)
const seciliPos = ref(null)
const musteriler = ref([])
const kaydediliyor = ref(false)
const yukleniyor = ref(false)

const form = ref({ ad: '', bankaId: null, komisyonOrani: 0, aktif: true })

const formatPara = (v) => new Intl.NumberFormat('tr-TR', { style: 'currency', currency: 'TRY' }).format(v || 0)

const yukle = async () => {
  yukleniyor.value = true
  try {
    const [t, o] = await Promise.all([posAPI.liste(), posAPI.ozet()])
    terminaller.value = t.data || []
    ozetler.value = o.data || []
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || 'POS listesi yüklenemedi')
  }
  yukleniyor.value = false
}

const bankalariYukle = async () => {
  try {
    const r = await bankaAPI.getAll()
    bankalar.value = r.data?.content || r.data || []
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

const kaydet = async () => {
  if (!form.value.ad?.trim()) {
    toastBildirim.uyari('POS adı zorunludur')
    return
  }
  kaydediliyor.value = true
  try {
    if (duzenlenenId.value) {
      await posAPI.guncelle(duzenlenenId.value, form.value)
      toastBildirim.basarili('POS güncellendi')
    } else {
      await posAPI.olustur(form.value)
      toastBildirim.basarili('POS oluşturuldu')
    }
    dialog.value = false
    yukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || 'Kaydedilemedi')
  } finally {
    kaydediliyor.value = false
  }
}

const sil = async (p) => {
  try {
    await posAPI.sil(p.id)
    toastBildirim.basarili('POS silindi')
    yukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || 'Silinemedi')
  }
}

const musteriDetayAc = async (p) => {
  seciliPos.value = p
  musteriDialog.value = true
  try {
    const r = await posAPI.musteriler(p.id)
    musteriler.value = r.data || []
  } catch {
    musteriler.value = []
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
.pos-baslik {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.pos-baslik h1 {
  margin: 0;
  font-size: 20px;
  display: flex;
  align-items: center;
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
  color: var(--accent, #3b82f6);
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
.bos {
  grid-column: 1 / -1;
  text-align: center;
  color: var(--text-muted);
  padding: 32px;
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
</style>
