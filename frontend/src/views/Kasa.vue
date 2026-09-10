<template>
  <div class="kasa-container">
    <h1>{{ t('kasa.title') }}</h1>

    <Toolbar class="toolbar">
      <template #start>
        <Button
          :label="t('kasa.yeniKasa')"
          icon="pi pi-plus"
          class="p-button-success"
          @click="openKasaDialog"
        />
      </template>
      <template #end>
        <Button
          :label="t('kasa.bankayaAktar')"
          icon="pi pi-building"
          class="p-button-sm p-button-outlined mr-2"
          @click="openBankaAktarDialog"
        />
        <Button
          :label="t('kasa.kasaAktar')"
          icon="pi pi-arrow-right-arrow-left"
          class="p-button-sm p-button-outlined mr-2"
          @click="openAktarDialog"
        />
        <Button
          :label="t('kasa.gunSonu')"
          icon="pi pi-calendar"
          class="p-button-sm p-button-outlined mr-2"
          @click="gunSonuAc"
        />
        <Button
          :label="t('kasa.excel')"
          icon="pi pi-file-excel"
          class="p-button-sm p-button-outlined"
          @click="excelIndir"
        />
      </template>
    </Toolbar>

    <div
      v-if="kasaStore.loading"
      class="loading"
    >
      <p><i class="pi pi-spin pi-spinner" /> Yükleniyor...</p>
    </div>

    <div
      v-if="!kasaStore.loading"
      class="kasa-cards"
    >
      <div
        v-for="kasa in kasaStore.kasalar"
        :key="kasa.id"
        class="kasa-card"
        :class="{ active: seciliKasaId === kasa.id }"
        @click="kasaSec(kasa)"
      >
        <div class="kasa-ust">
          <i class="pi pi-wallet" />
          <h3>{{ kasa.ad }}</h3>
        </div>
        <p
          class="kasa-bakiye"
          :class="kasa.bakiye >= 0 ? 'positive' : 'negative'"
        >
          {{ formatCurrency(kasa.bakiye) }}
        </p>
        <div class="kasa-islem">
          <Button
            icon="pi pi-pencil"
            class="p-button-rounded p-button-info p-button-sm"
            @click.stop="editKasa(kasa)"
          />
          <Button
            icon="pi pi-trash"
            class="p-button-rounded p-button-danger p-button-sm"
            @click.stop="confirmDel(kasa.id)"
          />
        </div>
      </div>
      <EmptyState
        v-if="kasaStore.kasalar.length === 0"
        :message="t('kasa.empty')"
        :sub-message="t('kasa.emptyHint')"
        icon="pi pi-wallet"
        :action-label="t('kasa.yeniKasa')"
        action-icon="pi pi-plus"
        class="full-width"
        @action="openKasaDialog"
      />
    </div>

    <Dialog
      v-model:visible="showKasaDialog"
      :header="editingKasaId ? t('kasa.kasaDuzenle') : t('kasa.yeniKasa')"
      :modal="true"
      style="width: 400px"
    >
      <div class="form-group">
        <label>{{ t('kasa.kasaAdi') }}</label>
        <InputText
          v-model="kasaForm.ad"
          :placeholder="t('kasa.kasaAdiPlaceholder')"
          class="w-full"
        />
      </div>
      <div
        v-if="!editingKasaId"
        class="form-group"
      >
        <label>{{ t('kasa.acilisBakiyesi') }}</label>
        <InputNumber
          v-model="kasaForm.bakiye"
          :min="0"
          :min-fraction-digits="2"
          :max-fraction-digits="2"
          class="w-full"
        />
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          icon="pi pi-times"
          class="p-button-text"
          @click="showKasaDialog = false"
        />
        <Button
          :label="editingKasaId ? t('kasa.guncelle') : t('common.save')"
          icon="pi pi-check"
          :loading="saving"
          @click="saveKasa"
        />
      </template>
    </Dialog>

    <div
      v-if="seciliKasa"
      class="hareket-bolumu"
    >
      <div class="hareket-header">
        <h2>{{ seciliKasa.ad }} {{ t('kasa.hareketler') }}</h2>
        <Button
          :label="t('kasa.gelirEkle')"
          icon="pi pi-plus-circle"
          class="p-button-success p-button-sm"
          @click="openHareketDialog('GELIR')"
        />
        <Button
          :label="t('kasa.giderEkle')"
          icon="pi pi-minus-circle"
          class="p-button-danger p-button-sm"
          @click="openHareketDialog('GIDER')"
        />
      </div>

      <div class="table-container">
        <DataTable
          :value="kasaHareketler"
          striped-rows
          :rows="10"
          :paginator="true"
          paginator-template="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink CurrentPageReport"
          current-page-report-template="{first} - {last} ({totalRecords} kayıt)"
        >
          <Column
            field="tarih"
            :header="t('common.date')"
            style="width: 100px"
          >
            <template #body="s">
              {{ formatDate(s.data.hareketTarihi) }}
            </template>
          </Column>
          <Column
            field="tur"
            :header="t('kasa.tur')"
            style="width: 80px"
          >
            <template #body="s">
              <span :class="['badge', s.data.tur === 'GELIR' ? 'gelir' : 'gider']">
                {{ s.data.tur === 'GELIR' ? t('kasa.gelir') : t('kasa.gider') }}
              </span>
            </template>
          </Column>
          <Column
            field="tutar"
            :header="t('common.amount')"
            style="width: 120px"
          >
            <template #body="s">
              <span :class="s.data.tur === 'GELIR' ? 'positive' : 'negative'">
                {{ formatCurrency(s.data.tutar) }}
              </span>
            </template>
          </Column>
          <Column
            field="kategoriAd"
            :header="t('kasa.kategori')"
            style="width: 140px"
          >
            <template #body="s">
              {{ s.data.kategoriAd || '-' }}
            </template>
          </Column>
          <Column
            field="aciklama"
            :header="t('common.description')"
          />
          <Column
            header=""
            style="width: 60px"
          >
            <template #body="s">
              <Button
                icon="pi pi-trash"
                class="p-button-rounded p-button-danger p-button-sm"
                @click="delHareket(s.data.id)"
              />
            </template>
          </Column>
        </DataTable>
        <Message
          v-if="kasaHareketler && kasaHareketler.length === 0"
          severity="info"
          :text="t('kasa.hareketYok')"
        />
      </div>
    </div>

    <Dialog
      v-model:visible="showHareketDialog"
      :header="hareketBaslik"
      :modal="true"
      style="width: 500px"
    >
      <div class="form-group">
        <label>{{ t('kasa.tutar') }}</label>
        <InputNumber
          v-model="hareketForm.tutar"
          :min="0.01"
          :min-fraction-digits="2"
          :max-fraction-digits="2"
          class="w-full"
        />
      </div>
      <div class="form-group">
        <label>{{ t('kasa.tarihZorunlu') }}</label>
        <DatePicker
          v-model="hareketForm.hareketTarihi"
          date-format="dd.mm.yy"
          class="w-full"
        />
      </div>
      <div class="form-group">
        <label>{{ t('kasa.kategori') }}</label>
        <Dropdown
          v-model="hareketForm.kategoriId"
          :options="kategoriSecenekler"
          option-label="ad"
          option-value="id"
          :placeholder="t('faturalar.seciniz')"
          class="w-full"
        />
      </div>
      <div class="form-group">
        <label>{{ t('common.description') }}</label>
        <Textarea
          v-model="hareketForm.aciklama"
          rows="2"
          class="w-full"
        />
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          icon="pi pi-times"
          class="p-button-text"
          @click="showHareketDialog = false"
        />
        <Button
          :label="t('common.save')"
          icon="pi pi-check"
          :loading="saving"
          @click="saveHareket"
        />
      </template>
    </Dialog>

    <Dialog
      v-model:visible="showAktarDialog"
      :header="t('kasa.kasaAktar')"
      :modal="true"
      style="width: 480px"
    >
      <div class="form-group">
        <label>{{ t('kasa.kaynakKasa') }}</label>
        <Dropdown
          v-model="aktarForm.kaynakKasaId"
          :options="kasaStore.kasalar"
          option-label="ad"
          option-value="id"
          :placeholder="t('faturalar.seciniz')"
          class="w-full"
        />
      </div>
      <div class="form-group">
        <label>{{ t('kasa.hedefKasa') }}</label>
        <Dropdown
          v-model="aktarForm.hedefKasaId"
          :options="kasaStore.kasalar"
          option-label="ad"
          option-value="id"
          :placeholder="t('faturalar.seciniz')"
          class="w-full"
        />
      </div>
      <div class="form-group">
        <label>{{ t('kasa.tutar') }}</label>
        <InputNumber
          v-model="aktarForm.tutar"
          :min="0.01"
          :min-fraction-digits="2"
          :max-fraction-digits="2"
          class="w-full"
        />
      </div>
      <div class="form-group">
        <label>{{ t('common.description') }}</label>
        <InputText
          v-model="aktarForm.aciklama"
          :placeholder="t('kasa.ornekSubeDevir')"
          class="w-full"
        />
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          icon="pi pi-times"
          class="p-button-text"
          @click="showAktarDialog = false"
        />
        <Button
          :label="t('kasa.aktar')"
          icon="pi pi-check"
          :loading="saving"
          @click="saveAktar"
        />
      </template>
    </Dialog>

    <Dialog
      v-model:visible="showBankaAktarDialog"
      :header="t('kasa.kasadanBankaya')"
      :modal="true"
      style="width: 480px"
    >
      <div class="form-group">
        <label>{{ t('kasa.kaynakKasa') }}</label>
        <Dropdown
          v-model="bankaAktarForm.kasaId"
          :options="kasaStore.kasalar"
          option-label="ad"
          option-value="id"
          :placeholder="t('faturalar.seciniz')"
          class="w-full"
        />
      </div>
      <div class="form-group">
        <label>{{ t('kasa.hedefBanka') }}</label>
        <Dropdown
          v-model="bankaAktarForm.bankaId"
          :options="bankalar"
          option-label="ad"
          option-value="id"
          filter
          :placeholder="t('faturalar.seciniz')"
          class="w-full"
        />
      </div>
      <div class="form-group">
        <label>{{ t('kasa.tutar') }}</label>
        <InputNumber
          v-model="bankaAktarForm.tutar"
          :min="0.01"
          :min-fraction-digits="2"
          :max-fraction-digits="2"
          class="w-full"
        />
      </div>
      <div class="form-group">
        <label>{{ t('common.description') }}</label>
        <InputText
          v-model="bankaAktarForm.aciklama"
          :placeholder="t('kasa.ornekGunSonu')"
          class="w-full"
        />
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          icon="pi pi-times"
          class="p-button-text"
          @click="showBankaAktarDialog = false"
        />
        <Button
          :label="t('kasa.aktar')"
          icon="pi pi-check"
          :loading="saving"
          @click="saveBankaAktar"
        />
      </template>
    </Dialog>

    <Dialog
      v-model:visible="gunSonuDialog"
      :header="t('kasa.gunSonuBaslik')"
      :modal="true"
      style="width: 480px"
    >
      <div
        v-if="gunSonuVerisi"
        class="gun-sonu"
      >
        <div class="gun-sonu-tarih">
          {{ formatDate(gunSonuVerisi.tarih) }}
        </div>
        <div class="gun-sonu-satir">
          <span>{{ t('kasa.satisAdedi') }}</span>
          <strong>{{ gunSonuVerisi.satisAdedi }}</strong>
        </div>
        <div class="gun-sonu-satir">
          <span>{{ t('kasa.toplamSatis') }}</span>
          <strong>{{ formatCurrency(gunSonuVerisi.toplamSatis) }}</strong>
        </div>
        <div class="gun-sonu-satir">
          <span>{{ t('kasa.nakit') }}</span>
          <strong>{{ formatCurrency(gunSonuVerisi.nakitSatis) }}</strong>
        </div>
        <div class="gun-sonu-satir">
          <span>{{ t('kasa.kart') }}</span>
          <strong>{{ formatCurrency(gunSonuVerisi.kartSatis) }}</strong>
        </div>
        <div class="gun-sonu-satir">
          <span>{{ t('kasa.havale') }}</span>
          <strong>{{ formatCurrency(gunSonuVerisi.havaleSatis) }}</strong>
        </div>
        <div class="gun-sonu-kasalar">
          <div class="gun-sonu-alt-baslik">
            {{ t('kasa.kasaBakiyeleri') }}
          </div>
          <div
            v-for="k in gunSonuVerisi.kasalar"
            :key="k.ad"
            class="gun-sonu-satir"
          >
            <span>{{ k.ad }}</span>
            <strong>{{ formatCurrency(k.bakiye) }}</strong>
          </div>
        </div>
      </div>
      <template #footer>
        <Button
          :label="t('kasa.kapat')"
          icon="pi pi-times"
          class="p-button-text"
          @click="gunSonuDialog = false"
        />
      </template>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useConfirm } from 'primevue/useconfirm'
import { useKasaStore } from '../stores/kasaStore.js'
import { useKategoriStore } from '../stores/kategoriStore.js'
import { kasaAPI, excelAPI, faturaAPI, bankaAPI } from '../api/index.js'
import EmptyState from '../components/EmptyState.vue'
import { formatCurrency } from '../utils/format.js'
import { useI18n } from 'vue-i18n'

const toastBildirim = useToastBildirim()
const confirm = useConfirm()
const { t } = useI18n()
const kasaStore = useKasaStore()
const kategoriStore = useKategoriStore()

const seciliKasaId = ref(null)
const seciliKasa = ref(null)
const kasaHareketler = ref([])
const saving = ref(false)

const showKasaDialog = ref(false)
const editingKasaId = ref(null)
const kasaForm = ref({ ad: '', bakiye: 0 })

const showHareketDialog = ref(false)
const hareketTur = ref('GELIR')
const hareketForm = ref({ tutar: null, hareketTarihi: new Date(), kategoriId: null, aciklama: '' })

const showAktarDialog = ref(false)
const aktarForm = ref({ kaynakKasaId: null, hedefKasaId: null, tutar: null, aciklama: '' })

const showBankaAktarDialog = ref(false)
const bankaAktarForm = ref({ kasaId: null, bankaId: null, tutar: null, aciklama: '' })
const bankalar = ref([])

const hareketBaslik = computed(() => (hareketTur.value === 'GELIR' ? t('kasa.gelirEkleBaslik') : t('kasa.giderEkleBaslik')))

const kategoriSecenekler = computed(() => kategoriStore.kategoriler.filter((k) => k.tur === hareketTur.value))

onMounted(async () => {
  await Promise.all([kasaStore.getAllKasalar(), kategoriStore.getAllKategoriler()])
  try {
    const r = await bankaAPI.getAll({ size: 500 })
    bankalar.value = r.data?.content || r.data || []
  } catch {
    bankalar.value = []
  }
})

const kasaSec = async (kasa) => {
  seciliKasaId.value = kasa.id
  seciliKasa.value = kasa
  try {
    const r = await kasaAPI.getHareketler(kasa.id)
    kasaHareketler.value = r.data
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('kasa.hareketYuklenemedi'))
  }
}

const openKasaDialog = () => {
  editingKasaId.value = null
  kasaForm.value = { ad: '', bakiye: 0 }
  showKasaDialog.value = true
}

const editKasa = (kasa) => {
  editingKasaId.value = kasa.id
  kasaForm.value = { ad: kasa.ad, bakiye: 0 }
  showKasaDialog.value = true
}

const saveKasa = async () => {
  if (!kasaForm.value.ad.trim()) {
    toastBildirim.uyari(t('kasa.kasaAdiGiriniz'))
    return
  }
  saving.value = true
  try {
    if (editingKasaId.value) {
      await kasaStore.updateKasa(editingKasaId.value, kasaForm.value)
      toastBildirim.basarili(t('kasa.kasaGuncellendi'))
    } else {
      await kasaStore.addKasa(kasaForm.value)
      toastBildirim.basarili(t('kasa.kasaOlusturuldu'))
    }
    showKasaDialog.value = false
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('kasa.islemBasarisiz'))
  } finally {
    saving.value = false
  }
}

const confirmDel = (id) => {
  confirm.require({
    message: t('kasa.silOnayMesaj'),
    header: t('kasa.onay'),
    icon: 'pi pi-exclamation-triangle',
    accept: async () => {
      try {
        await kasaStore.deleteKasa(id)
        if (seciliKasaId.value === id) {
          seciliKasaId.value = null
          seciliKasa.value = null
          kasaHareketler.value = []
        }
        toastBildirim.basarili(t('kasa.kasaSilindi'))
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || err?.message || t('kasa.silmeBasarisiz'))
      }
    }
  })
}

const openHareketDialog = (tur) => {
  hareketTur.value = tur
  hareketForm.value = { tutar: null, hareketTarihi: new Date(), kategoriId: null, aciklama: '' }
  showHareketDialog.value = true
}

const saveHareket = async () => {
  if (!hareketForm.value.tutar || hareketForm.value.tutar <= 0) {
    toastBildirim.uyari(t('kasa.gecerliTutar'))
    return
  }
  saving.value = true
  try {
    await kasaAPI.addHareket(seciliKasaId.value, {
      tur: hareketTur.value,
      tutar: hareketForm.value.tutar,
      hareketTarihi: hareketForm.value.hareketTarihi.toISOString().split('T')[0],
      kategoriId: hareketForm.value.kategoriId,
      aciklama: hareketForm.value.aciklama
    })
    const r = await kasaAPI.getHareketler(seciliKasaId.value)
    kasaHareketler.value = r.data
    await kasaStore.getAllKasalar()
    const guncel = kasaStore.kasalar.find((k) => k.id === seciliKasaId.value)
    if (guncel) seciliKasa.value = guncel
    showHareketDialog.value = false
    toastBildirim.basarili(t('kasa.hareketEklendi'))
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('kasa.islemBasarisiz'))
  } finally {
    saving.value = false
  }
}

const delHareket = async (id) => {
  try {
    await kasaAPI.deleteHareket(id)
    kasaHareketler.value = kasaHareketler.value.filter((h) => h.id !== id)
    await kasaStore.getAllKasalar()
    const guncel = kasaStore.kasalar.find((k) => k.id === seciliKasaId.value)
    if (guncel) seciliKasa.value = guncel
    toastBildirim.basarili(t('kasa.hareketSilindi'))
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('kasa.silmeBasarisiz'))
  }
}

const openAktarDialog = () => {
  aktarForm.value = { kaynakKasaId: null, hedefKasaId: null, tutar: null, aciklama: '' }
  showAktarDialog.value = true
}

const openBankaAktarDialog = () => {
  bankaAktarForm.value = { kasaId: null, bankaId: null, tutar: null, aciklama: '' }
  showBankaAktarDialog.value = true
}

const saveBankaAktar = async () => {
  if (!bankaAktarForm.value.kasaId || !bankaAktarForm.value.bankaId) {
    toastBildirim.uyari(t('kasa.kaynakHedefSecin'))
    return
  }
  if (!bankaAktarForm.value.tutar || bankaAktarForm.value.tutar <= 0) {
    toastBildirim.uyari(t('kasa.gecerliTutarGiriniz'))
    return
  }
  saving.value = true
  try {
    await kasaAPI.bankayaAktar(bankaAktarForm.value)
    showBankaAktarDialog.value = false
    toastBildirim.basarili(t('kasa.bankayaAktarildi'))
    await kasaStore.getAllKasalar()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('kasa.aktarimBasarisiz'))
  } finally {
    saving.value = false
  }
}

const gunSonuDialog = ref(false)
const gunSonuVerisi = ref(null)

const gunSonuAc = async () => {
  gunSonuDialog.value = true
  try {
    const bugun = new Date().toISOString().split('T')[0]
    const r = await faturaAPI.getAll({ size: 200, sort: 'tarih,desc' })
    const faturalar = r.data?.content || r.data || []
    const bugunSatislar = faturalar.filter((f) => f.tur === 'SATIS' && f.tarih === bugun)
    const toplamSatis = bugunSatislar.reduce((t, f) => t + (f.genelToplam || 0), 0)
    const nakitSatis = bugunSatislar.filter((f) => f.odemeYontemi === 'NAKIT').reduce((t, f) => t + (f.odenenTutar || 0), 0)
    const kartSatis = bugunSatislar.filter((f) => f.odemeYontemi === 'KART').reduce((t, f) => t + (f.odenenTutar || 0), 0)
    const havaleSatis = bugunSatislar.filter((f) => f.odemeYontemi === 'HAVALE').reduce((t, f) => t + (f.odenenTutar || 0), 0)
    gunSonuVerisi.value = {
      tarih: bugun,
      satisAdedi: bugunSatislar.length,
      toplamSatis,
      nakitSatis,
      kartSatis,
      havaleSatis,
      kasalar: kasaStore.kasalar.map((k) => ({ ad: k.ad, bakiye: k.bakiye }))
    }
  } catch {
    gunSonuVerisi.value = null
  }
}

const saveAktar = async () => {
  if (!aktarForm.value.kaynakKasaId || !aktarForm.value.hedefKasaId) {
    toastBildirim.uyari(t('kasa.kaynakHedefKasa'))
    return
  }
  if (!aktarForm.value.tutar || aktarForm.value.tutar <= 0) {
    toastBildirim.uyari(t('kasa.gecerliTutar'))
    return
  }
  saving.value = true
  try {
    await kasaAPI.aktar(aktarForm.value)
    showAktarDialog.value = false
    await kasaStore.getAllKasalar()
    toastBildirim.basarili(t('kasa.kasaAktarildi'))
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('kasa.aktarimBasarisiz'))
  } finally {
    saving.value = false
  }
}

const excelIndir = async () => {
  try {
    const res = await excelAPI.kasalar()
    const url = window.URL.createObjectURL(new Blob([res.data]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', 'Kasalar.xlsx')
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)
  } catch {
    /* silent */
  }
}


import { formatTarih as formatDate } from '../utils/format.js'
</script>

<style scoped>
.kasa-container {
  padding: 0;
  max-width: 100%;
}
h1 {
  color: var(--text-primary);
  margin-bottom: 20px;
  font-size: 28px;
  font-weight: 700;
  letter-spacing: -0.5px;
}
h2 {
  color: var(--text-primary);
  font-size: 20px;
  margin: 0;
}
.toolbar {
  margin-bottom: 20px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 14px 18px;
}
.loading {
  text-align: center;
  padding: 40px;
  color: #666;
}
.kasa-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(min(280px, 100%), 1fr));
  gap: 15px;
  margin-bottom: 30px;
}
.kasa-card {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 14px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.2);
}
.kasa-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.3);
  border-color: rgba(59, 130, 246, 0.25);
}
.kasa-card.active {
  border-color: #3b82f6;
  background: rgba(59, 130, 246, 0.08);
}
.kasa-ust {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 15px;
}
.kasa-ust i {
  font-size: 28px;
  color: #1976d2;
}
.kasa-ust h3 {
  margin: 0;
  font-size: 18px;
}
.kasa-bakiye {
  font-size: 24px;
  font-weight: bold;
  margin: 0 0 15px 0;
}
.kasa-islem {
  display: flex;
  gap: 8px;
}
.positive {
  color: #4caf50;
}
.negative {
  color: #f44336;
}
.hareket-bolumu {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 14px;
  padding: 20px;
}
.hareket-header {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}
.table-container {
  overflow-x: auto;
}
.form-group {
  margin-bottom: 20px;
}
.form-group label {
  display: block;
  margin-bottom: 6px;
  font-weight: bold;
  color: #333;
  font-size: 13px;
}
.badge {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: bold;
}
.badge.gelir {
  background: #e8f5e9;
  color: #2e7d32;
}
.badge.gider {
  background: #ffebee;
  color: #c62828;
}
.w-full {
  width: 100% !important;
}
.gun-sonu {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.gun-sonu-tarih {
  font-size: 13px;
  color: var(--text-muted);
  margin-bottom: 8px;
  text-align: center;
}
.gun-sonu-satir {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid var(--border);
  font-size: 14px;
}
.gun-sonu-satir span {
  color: var(--text-secondary);
}
.gun-sonu-kasalar {
  margin-top: 10px;
}
.gun-sonu-alt-baslik {
  font-size: 13px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 4px;
}
.full-width {
  grid-column: 1 / -1;
}
</style>
