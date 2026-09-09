<template>
  <div class="uretim-sayfasi">
    <div class="sayfa-baslik">
      <h1><i class="pi pi-cog" /> Üretim</h1>
    </div>

    <!-- Reçeteler -->
    <div class="bolum">
      <div class="bolum-baslik">
        <h2>Reçeteler (Ürün Ağacı)</h2>
        <Button
          label="Yeni Reçete"
          icon="pi pi-plus"
          class="p-button-sm"
          @click="receteDialogAc(null)"
        />
      </div>
      <div
        v-if="!receteler.length"
        class="bos"
      >
        Henüz reçete yok.
      </div>
      <div
        v-for="r in receteler"
        :key="r.id"
        class="satir"
      >
        <div class="satir-bilgi">
          <strong>{{ r.ad }}</strong>
          <span class="muted">Mamul: {{ r.urunAd || '#' + r.urunId }}</span>
          <span class="muted">Kalem: {{ (r.kalemler || []).length }}</span>
        </div>
        <Button
          icon="pi pi-trash"
          class="p-button-rounded p-button-text p-button-danger p-button-sm"
          @click="receteSil(r)"
        />
      </div>
    </div>

    <!-- Üretim Emirleri -->
    <div class="bolum">
      <div class="bolum-baslik">
        <h2>Üretim Emirleri</h2>
        <Button
          label="Yeni Emir"
          icon="pi pi-plus"
          class="p-button-sm"
          @click="emirDialog = true"
        />
      </div>
      <div
        v-if="!emirler.length"
        class="bos"
      >
        Henüz üretim emri yok.
      </div>
      <div
        v-for="e in emirler"
        :key="e.id"
        class="satir"
      >
        <div class="satir-bilgi">
          <strong>{{ e.urunAd || '#' + e.urunId }}</strong>
          <span class="muted">Miktar: {{ e.miktar }}</span>
          <Tag
            :value="durumAdi(e.durum)"
            :severity="durumSeverity(e.durum)"
          />
        </div>
        <Button
          v-if="e.durum !== 'TAMAMLANDI' && e.durum !== 'IPTAL'"
          label="Tamamla"
          icon="pi pi-check"
          class="p-button-sm p-button-success"
          @click="emirTamamla(e)"
        />
      </div>
    </div>

    <!-- Reçete Dialog -->
    <Dialog
      v-model:visible="receteDialog"
      header="Reçete"
      :modal="true"
      :style="{ width: '520px' }"
    >
      <div class="form">
        <div class="field">
          <label>Reçete Adı *</label>
          <InputText
            v-model="receteForm.ad"
            class="w-full"
            placeholder="Örn: Masa A Reçetesi"
          />
        </div>
        <div class="field">
          <label>Üretilecek Ürün (Mamul) *</label>
          <Dropdown
            v-model="receteForm.urunId"
            :options="stoklar"
            option-label="ad"
            option-value="id"
            filter
            class="w-full"
            placeholder="Mamul seçin"
          />
        </div>
        <div class="field">
          <label>Hammaddeler</label>
          <div
            v-for="(k, i) in receteForm.kalemler"
            :key="i"
            class="kalem-satir"
          >
            <Dropdown
              v-model="k.hammaddeId"
              :options="stoklar"
              option-label="ad"
              option-value="id"
              filter
              class="w-full"
              placeholder="Hammadde"
            />
            <InputNumber
              v-model="k.miktar"
              :min="0"
              class="kalem-miktar"
              placeholder="Miktar"
            />
            <Button
              icon="pi pi-trash"
              class="p-button-text p-button-danger p-button-sm"
              @click="receteForm.kalemler.splice(i, 1)"
            />
          </div>
          <Button
            label="Kalem Ekle"
            icon="pi pi-plus"
            class="p-button-sm p-button-text"
            @click="receteForm.kalemler.push({ hammaddeId: null, miktar: 1 })"
          />
        </div>
      </div>
      <template #footer>
        <Button
          label="İptal"
          class="p-button-text"
          @click="receteDialog = false"
        />
        <Button
          label="Kaydet"
          icon="pi pi-check"
          :loading="kaydediliyor"
          @click="receteKaydet"
        />
      </template>
    </Dialog>

    <!-- Emir Dialog -->
    <Dialog
      v-model:visible="emirDialog"
      header="Yeni Üretim Emri"
      :modal="true"
      :style="{ width: '420px' }"
    >
      <div class="form">
        <div class="field">
          <label>Ürün *</label>
          <Dropdown
            v-model="emirForm.urunId"
            :options="stoklar"
            option-label="ad"
            option-value="id"
            filter
            class="w-full"
            placeholder="Ürün seçin"
          />
        </div>
        <div class="field">
          <label>Miktar *</label>
          <InputNumber
            v-model="emirForm.miktar"
            :min="0"
            class="w-full"
          />
        </div>
      </div>
      <template #footer>
        <Button
          label="İptal"
          class="p-button-text"
          @click="emirDialog = false"
        />
        <Button
          label="Oluştur"
          icon="pi pi-check"
          :loading="kaydediliyor"
          @click="emirKaydet"
        />
      </template>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { uretimAPI, stokAPI } from '../api/index.js'
import { useToastBildirim } from '../composables/useToastBildirim.js'

const toastBildirim = useToastBildirim()

const receteler = ref([])
const emirler = ref([])
const stoklar = ref([])
const receteDialog = ref(false)
const emirDialog = ref(false)
const kaydediliyor = ref(false)

const receteForm = ref({ ad: '', urunId: null, kalemler: [] })
const emirForm = ref({ urunId: null, miktar: null })

const durumAdi = (d) => ({ TASLAK: 'Taslak', URETIMDE: 'Üretimde', TAMAMLANDI: 'Tamamlandı', IPTAL: 'İptal' })[d] || d
const durumSeverity = (d) => ({ TASLAK: 'secondary', URETIMDE: 'info', TAMAMLANDI: 'success', IPTAL: 'danger' })[d] || 'secondary'

const yukle = async () => {
  try {
    const [r, e, s] = await Promise.all([uretimAPI.receteler(), uretimAPI.emirler(), stokAPI.getAll({ size: 500 })])
    receteler.value = r.data || []
    emirler.value = e.data || []
    stoklar.value = s.data?.content || s.data || []
  } catch (err) {
    toastBildirim.hata('Üretim verileri yüklenemedi')
  }
}

const receteDialogAc = () => {
  receteForm.value = { ad: '', urunId: null, kalemler: [] }
  receteDialog.value = true
}

const receteKaydet = async () => {
  if (!receteForm.value.ad?.trim() || !receteForm.value.urunId) {
    toastBildirim.uyari('Reçete adı ve mamul zorunludur')
    return
  }
  kaydediliyor.value = true
  try {
    await uretimAPI.receteOlustur({
      ad: receteForm.value.ad,
      urunId: receteForm.value.urunId,
      kalemler: receteForm.value.kalemler.filter((k) => k.hammaddeId && k.miktar > 0)
    })
    toastBildirim.basarili('Reçete kaydedildi')
    receteDialog.value = false
    yukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || 'Reçete kaydedilemedi')
  } finally {
    kaydediliyor.value = false
  }
}

const receteSil = async (r) => {
  try {
    await uretimAPI.receteSil(r.id)
    toastBildirim.basarili('Reçete silindi')
    yukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || 'Silinemedi')
  }
}

const emirKaydet = async () => {
  if (!emirForm.value.urunId || !emirForm.value.miktar) {
    toastBildirim.uyari('Ürün ve miktar zorunludur')
    return
  }
  kaydediliyor.value = true
  try {
    await uretimAPI.emirOlustur(emirForm.value)
    toastBildirim.basarili('Üretim emri oluşturuldu')
    emirDialog.value = false
    emirForm.value = { urunId: null, miktar: null }
    yukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || 'Emir oluşturulamadı')
  } finally {
    kaydediliyor.value = false
  }
}

const emirTamamla = async (e) => {
  try {
    await uretimAPI.emirTamamla(e.id)
    toastBildirim.basarili('Üretim tamamlandı, stoklar güncellendi')
    yukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || 'Üretim tamamlanamadı')
  }
}

onMounted(yukle)
</script>

<style scoped>
.uretim-sayfasi {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.sayfa-baslik h1 {
  margin: 0;
  font-size: 20px;
  display: flex;
  align-items: center;
}
.bolum {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 16px;
}
.bolum-baslik {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.bolum-baslik h2 {
  margin: 0;
  font-size: 15px;
}
.satir {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px solid var(--border);
}
.satir:last-child {
  border-bottom: none;
}
.satir-bilgi {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.muted {
  font-size: 12px;
  color: var(--text-muted);
}
.bos {
  text-align: center;
  color: var(--text-muted);
  padding: 24px;
}
.form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.field label {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
}
.w-full {
  width: 100%;
}
.kalem-satir {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 8px;
}
.kalem-miktar {
  width: 120px;
}
</style>
