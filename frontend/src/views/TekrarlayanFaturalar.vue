<template>
  <div class="tf-page">
    <PageHeader
      title="Tekrarlayan Faturalar"
      subtitle="Abonelik, kira ve düzenli faturalandırmayı otomatikleştirin. Vadesi gelen tanımlar her gün otomatik fatura keser."
    >
      <template #actions>
        <Button
          label="Yeni Tekrarlayan Fatura"
          icon="pi pi-plus"
          class="p-button-success"
          @click="yeniKayit"
        />
      </template>
    </PageHeader>

    <Card>
      <template #content>
        <DataTable
          :value="kayitlar"
          :loading="yukleniyor"
          striped-rows
          size="small"
        >
          <Column
            field="cariHesapAd"
            header="Cari"
          >
            <template #body="s">
              {{ s.data.cariHesapAd || '-' }}
            </template>
          </Column>
          <Column
            field="tur"
            header="Tür"
            style="width: 90px"
          >
            <template #body="s">
              <Tag
                :value="s.data.tur === 'ALIS' ? 'Alış' : 'Satış'"
                :severity="s.data.tur === 'ALIS' ? 'warning' : 'success'"
              />
            </template>
          </Column>
          <Column
            field="periyot"
            header="Periyot"
            style="width: 100px"
          />
          <Column header="Başlangıç">
            <template #body="s">
              {{ formatDate(s.data.baslangicTarihi) }}
            </template>
          </Column>
          <Column header="Bitiş">
            <template #body="s">
              {{ s.data.bitisTarihi ? formatDate(s.data.bitisTarihi) : '-' }}
            </template>
          </Column>
          <Column header="Sonraki Çalıştırma">
            <template #body="s">
              {{ s.data.sonrakiCalistirma ? formatDate(s.data.sonrakiCalistirma) : '-' }}
            </template>
          </Column>
          <Column
            field="aktif"
            header="Durum"
            style="width: 90px"
          >
            <template #body="s">
              <Tag
                :value="s.data.aktif ? 'Aktif' : 'Pasif'"
                :severity="s.data.aktif ? 'success' : 'secondary'"
              />
            </template>
          </Column>
          <Column
            header="İşlem"
            style="width: 180px"
          >
            <template #body="s">
              <Button
                icon="pi pi-pencil"
                class="p-button-sm p-button-text"
                @click="duzenle(s.data)"
              />
              <Button
                icon="pi pi-file"
                class="p-button-sm p-button-text"
                title="Şimdi fatura üret"
                @click="suretiUret(s.data)"
              />
              <Button
                icon="pi pi-trash"
                class="p-button-sm p-button-text p-button-danger"
                @click="sil(s.data)"
              />
            </template>
          </Column>
        </DataTable>
        <div
          v-if="(!kayitlar || !kayitlar.length) && !yukleniyor"
          class="empty-state"
        >
          Henüz tekrarlayan fatura tanımı bulunmuyor.
        </div>
      </template>
    </Card>

    <Dialog
      v-model:visible="dialogAcik"
      :header="duzenlemeId ? 'Tekrarlayan Fatura Düzenle' : 'Yeni Tekrarlayan Fatura'"
      :modal="true"
      style="width: 680px"
      :maximizable="true"
    >
      <div class="form-grid">
        <div class="field">
          <label>Cari Hesap</label>
          <Select
            v-model="form.cariHesapId"
            :options="cariSecenekleri"
            option-label="ad"
            option-value="id"
            placeholder="Cari seçin"
            class="w-full"
            show-clear
            filter
          />
        </div>
        <div class="field">
          <label>Tür</label>
          <Select
            v-model="form.tur"
            :options="[{ label: 'Satış', value: 'SATIS' }, { label: 'Alış', value: 'ALIS' }]"
            option-label="label"
            option-value="value"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>Periyot</label>
          <Select
            v-model="form.periyot"
            :options="periyotlar"
            option-label="label"
            option-value="value"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>Başlangıç Tarihi</label>
          <DatePicker
            v-model="form.baslangicTarihi"
            date-format="dd/mm/yy"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>Bitiş Tarihi (opsiyonel)</label>
          <DatePicker
            v-model="form.bitisTarihi"
            date-format="dd/mm/yy"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>Açıklama</label>
          <InputText
            v-model="form.aciklama"
            class="w-full"
            placeholder="Örn: Aylık bakım ücreti"
          />
        </div>
        <div class="field">
          <label>Aktif</label>
          <ToggleSwitch v-model="form.aktif" />
        </div>
      </div>

      <div class="kalem-baslik">
        <span>Kalemler</span>
        <Button
          icon="pi pi-plus"
          class="p-button-sm p-button-outlined"
          label="Kalem Ekle"
          @click="kalemEkle"
        />
      </div>
      <div
        v-for="(k, i) in form.kalemler"
        :key="i"
        class="kalem-satir"
      >
        <InputText
          v-model="k.aciklama"
          placeholder="Açıklama"
          class="kalem-aciklama"
        />
        <InputNumber
          v-model="k.adet"
          placeholder="Adet"
          class="kalem-adet"
          :min="1"
        />
        <InputNumber
          v-model="k.birimFiyat"
          placeholder="Birim Fiyat"
          class="kalem-fiyat"
          mode="currency"
          currency="TRY"
        />
        <InputNumber
          v-model="k.kdvOrani"
          placeholder="KDV %"
          class="kalem-kdv"
          :min="0"
          :max="100"
        />
        <Button
          icon="pi pi-times"
          class="p-button-sm p-button-text p-button-danger"
          @click="form.kalemler.splice(i, 1)"
        />
      </div>

      <template #footer>
        <Button
          label="İptal"
          icon="pi pi-times"
          class="p-button-text"
          @click="dialogAcik = false"
        />
        <Button
          label="Kaydet"
          icon="pi pi-check"
          class="p-button-primary"
          :loading="kaydediliyor"
          @click="kaydet"
        />
      </template>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useConfirm } from 'primevue/useconfirm'
import { tekrarlayanFaturaAPI, cariHesapAPI } from '../api/index.js'
import PageHeader from '../components/PageHeader.vue'

const toastBildirim = useToastBildirim()
const confirm = useConfirm()

const kayitlar = ref([])
const yukleniyor = ref(false)
const dialogAcik = ref(false)
const kaydediliyor = ref(false)
const duzenlemeId = ref(null)

const cariSecenekleri = ref([])

const periyotlar = [
  { label: 'Günlük', value: 'GUNLUK' },
  { label: 'Haftalık', value: 'HAFTALIK' },
  { label: 'Aylık', value: 'AYLIK' },
  { label: 'Yıllık', value: 'YILLIK' }
]

const bosForm = () => ({
  cariHesapId: null,
  tur: 'SATIS',
  periyot: 'AYLIK',
  baslangicTarihi: new Date(),
  bitisTarihi: null,
  aciklama: '',
  aktif: true,
  kalemler: [{ aciklama: '', adet: 1, birimFiyat: 0, kdvOrani: 20, iskontoOrani: 0, stokId: null }]
})

const form = ref(bosForm())

const yukle = async () => {
  yukleniyor.value = true
  try {
    const r = await tekrarlayanFaturaAPI.getAll()
    kayitlar.value = r.data || []
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || 'Tekrarlayan faturalar yüklenemedi')
  } finally {
    yukleniyor.value = false
  }
}

const carileriYukle = async () => {
  try {
    const r = await cariHesapAPI.getAll()
    kayitlar.value
    cariSecenekleri.value = r.data?.content || r.data || []
  } catch {
    cariSecenekleri.value = []
  }
}

const yeniKayit = () => {
  duzenlemeId.value = null
  form.value = bosForm()
  dialogAcik.value = true
}

const duzenle = (k) => {
  duzenlemeId.value = k.id
  form.value = {
    cariHesapId: k.cariHesapId,
    tur: k.tur,
    periyot: k.periyot,
    baslangicTarihi: k.baslangicTarihi ? new Date(k.baslangicTarihi) : new Date(),
    bitisTarihi: k.bitisTarihi ? new Date(k.bitisTarihi) : null,
    aciklama: k.aciklama || '',
    aktif: k.aktif !== false,
    kalemler: (k.kalemler || []).map((c) => ({ ...c }))
  }
  if (!form.value.kalemler.length) form.value.kalemler = bosForm().kalemler
  dialogAcik.value = true
}

const kalemEkle = () => {
  form.value.kalemler.push({ aciklama: '', adet: 1, birimFiyat: 0, kdvOrani: 20, iskontoOrani: 0, stokId: null })
}

const tarihParam = (d) => (d ? (d.toISOString?.().split('T')[0] ?? d) : null)

const kaydet = async () => {
  if (!form.value.cariHesapId) {
    toastBildirim.uyari('Cari hesap seçin')
    return
  }
  if (!form.value.kalemler.length || form.value.kalemler.some((k) => !k.aciklama)) {
    toastBildirim.uyari('Kalemler eksik veya açıklamasız')
    return
  }
  kaydediliyor.value = true
  const gonderilecek = {
    cariHesapId: form.value.cariHesapId,
    tur: form.value.tur,
    periyot: form.value.periyot,
    baslangicTarihi: tarihParam(form.value.baslangicTarihi),
    bitisTarihi: tarihParam(form.value.bitisTarihi),
    aciklama: form.value.aciklama,
    aktif: form.value.aktif,
    kalemler: form.value.kalemler
  }
  try {
    if (duzenlemeId.value) {
      await tekrarlayanFaturaAPI.update(duzenlemeId.value, gonderilecek)
      toastBildirim.basarili('Tekrarlayan fatura güncellendi')
    } else {
      await tekrarlayanFaturaAPI.create(gonderilecek)
      toastBildirim.basarili('Tekrarlayan fatura oluşturuldu')
    }
    dialogAcik.value = false
    yukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || 'Kaydedilemedi')
  } finally {
    kaydediliyor.value = false
  }
}

const sil = (k) => {
  confirm.require({
    message: 'Bu tekrarlayan fatura tanımı silinecek. Emin misiniz?',
    header: 'Silme Onayı',
    icon: 'pi pi-exclamation-triangle',
    accept: async () => {
      try {
        await tekrarlayanFaturaAPI.delete(k.id)
        toastBildirim.basarili('Silindi')
        yukle()
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || 'Silinemedi')
      }
    }
  })
}

const suretiUret = (k) => {
  confirm.require({
    message: 'Bu tanımdan şimdi bir fatura üretilecek. Emin misiniz?',
    header: 'Fatura Üret',
    icon: 'pi pi-file',
    accept: async () => {
      try {
        await tekrarlayanFaturaAPI.uret(k.id)
        toastBildirim.basarili('Fatura üretildi')
        yukle()
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || 'Fatura üretilemedi')
      }
    }
  })
}

const formatDate = (d) => (d ? new Date(d).toLocaleDateString('tr-TR') : '-')

onMounted(() => {
  yukle()
  carileriYukle()
})
</script>

<style scoped>
.tf-page {
  padding: 1.5rem;
}
.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-bottom: 16px;
}
.field label {
  display: block;
  margin-bottom: 4px;
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--text-secondary);
}
.w-full {
  width: 100% !important;
}
.kalem-baslik {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 700;
  margin-bottom: 8px;
}
.kalem-satir {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 8px;
}
.kalem-aciklama {
  flex: 1;
  min-width: 140px;
}
.kalem-adet {
  width: 80px;
}
.kalem-fiyat {
  width: 130px;
}
.kalem-kdv {
  width: 90px;
}
.empty-state {
  text-align: center;
  padding: 2rem;
  color: var(--text-muted);
}
</style>
