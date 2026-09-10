<template>
  <div class="duzeltme-sayfasi">
    <div class="sayfa-baslik">
      <h1><i class="pi pi-sliders-h" /> {{ t('stokDuzeltmeler.title') }}</h1>
    </div>

    <div class="bolum">
      <div class="bolum-baslik">
        <h2>{{ t('stokDuzeltmeler.stokDuzelt') }}</h2>
      </div>
      <div class="form-satir">
        <Dropdown
          v-model="form.stokId"
          :options="stoklar"
          option-label="ad"
          option-value="id"
          filter
          class="w-full"
          :placeholder="t('stokDuzeltmeler.stokSecin')"
        />
        <InputNumber
          v-model="form.yeniMiktar"
          :min="0"
          :placeholder="t('stokDuzeltmeler.yeniMiktar')"
          class="miktar-input"
        />
        <InputText
          v-model="form.neden"
          :placeholder="t('stokDuzeltmeler.nedenOpsiyonel')"
          class="w-full"
        />
        <Button
          :label="t('stokDuzeltmeler.duzelt')"
          icon="pi pi-check"
          :loading="kaydediliyor"
          @click="duzelt"
        />
      </div>
    </div>

    <div class="bolum">
      <div class="bolum-baslik">
        <h2>{{ t('stokDuzeltmeler.duzeltmeGecmisi') }}</h2>
        <Button
          icon="pi pi-refresh"
          class="p-button-text p-button-sm"
          @click="yukle"
        />
      </div>
      <div
        v-if="!gecmis.length"
        class="bos"
      >
        {{ t('stokDuzeltmeler.bos') }}
      </div>
      <div
        v-for="d in gecmis"
        :key="d.id"
        class="satir"
      >
        <div class="satir-bilgi">
          <strong>{{ d.stokAd }}</strong>
          <span class="muted">{{ d.eskiMiktar }} → {{ d.yeniMiktar }}</span>
          <span
            v-if="d.neden"
            class="muted"
          >{{ d.neden }}</span>
          <span class="muted">{{ d.olusturmaTarihi ? new Date(d.olusturmaTarihi).toLocaleString('tr-TR') : '' }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { stokDuzeltmeAPI, stokAPI } from '../api/index.js'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useI18n } from 'vue-i18n'

const toastBildirim = useToastBildirim()
const { t } = useI18n()

const stoklar = ref([])
const gecmis = ref([])
const kaydediliyor = ref(false)
const form = ref({ stokId: null, yeniMiktar: null, neden: '' })

const yukle = async () => {
  try {
    const [g, s] = await Promise.all([stokDuzeltmeAPI.gecmis(), stokAPI.getAll({ size: 500 })])
    gecmis.value = g.data || []
    stoklar.value = s.data?.content || s.data || []
  } catch (err) {
    toastBildirim.hata(t('stokDuzeltmeler.hataYukleme'))
  }
}

const duzelt = async () => {
  if (!form.value.stokId || form.value.yeniMiktar == null) {
    toastBildirim.uyari(t('stokDuzeltmeler.zorunlu'))
    return
  }
  kaydediliyor.value = true
  try {
    await stokDuzeltmeAPI.duzelt({
      stokId: form.value.stokId,
      yeniMiktar: form.value.yeniMiktar,
      neden: form.value.neden || null
    })
    toastBildirim.basarili(t('stokDuzeltmeler.duzeltildi'))
    form.value = { stokId: null, yeniMiktar: null, neden: '' }
    yukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('stokDuzeltmeler.islemBasarisiz'))
  } finally {
    kaydediliyor.value = false
  }
}

onMounted(yukle)
</script>

<style scoped>
.duzeltme-sayfasi {
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
.form-satir {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}
.miktar-input {
  width: 140px;
}
.w-full {
  flex: 1;
  min-width: 160px;
}
.satir {
  padding: 10px 0;
  border-bottom: 1px solid var(--border);
}
.satir:last-child {
  border-bottom: none;
}
.satir-bilgi {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  align-items: center;
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
</style>
