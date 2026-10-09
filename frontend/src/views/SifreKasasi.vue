<template>
  <div class="sifre-kasa-sayfasi">
    <div class="sayfa-baslik">
      <h1>
        <i class="pi pi-lock" />
        {{ t('sifreKasasi.title') }}
      </h1>
      <Button
        :label="t('sifreKasasi.yeniKayit')"
        icon="pi pi-plus"
        @click="yeniKayit"
      />
    </div>

    <Message
      severity="warn"
      :closable="false"
      class="kasa-uyari"
    >
      {{ t('sifreKasasi.guvenlikNotu') }}
    </Message>

    <div class="kasa-filtre">
      <div class="kapsam-sekmeleri">
        <Button
          :label="t('sifreKasasi.kisiselSekme')"
          :severity="kapsam === 'KISISEL' ? 'primary' : 'secondary'"
          :outlined="kapsam !== 'KISISEL'"
          size="small"
          @click="kapsamDegistir('KISISEL')"
        />
        <Button
          :label="t('sifreKasasi.sirketSekme')"
          :severity="kapsam === 'GLOBAL' ? 'primary' : 'secondary'"
          :outlined="kapsam !== 'GLOBAL'"
          size="small"
          @click="kapsamDegistir('GLOBAL')"
        />
      </div>

      <Dropdown
        v-model="durum"
        :options="durumSecenekleri"
        option-label="label"
        option-value="value"
        class="filtre-alani"
        @change="yukle"
      />
      <Dropdown
        v-model="kategori"
        :options="kategoriSecenekleri"
        option-label="label"
        option-value="value"
        :show-clear="true"
        :placeholder="t('sifreKasasi.kategori')"
        class="filtre-alani"
        @change="yukle"
      />
      <span class="arama-kutu">
        <InputText
          v-model="arama"
          :placeholder="t('sifreKasasi.aramaPlaceholder')"
          @input="aramaGecikmeli"
        />
      </span>
    </div>

    <div class="kasa-icerik">
      <DataTable
        v-if="!yukleniyor && !hata && kayitlar.length > 0"
        :value="kayitlar"
        striped-rows
        responsive-layout="scroll"
        data-key="id"
      >
        <Column
          field="baslik"
          :header="t('sifreKasasi.baslik')"
          sortable
        />
        <Column
          field="kullaniciAdi"
          :header="t('sifreKasasi.kullaniciAdi')"
        >
          <template #body="s">
            <span
              v-if="s.data.kullaniciAdi"
              class="kopyalanabilir"
              @click="kopyala(s.data.kullaniciAdi, t('sifreKasasi.kullaniciAdiKopyalandi'))"
            >
              {{ s.data.kullaniciAdi }}
              <i class="pi pi-copy" />
            </span>
            <span v-else>-</span>
          </template>
        </Column>
        <Column :header="t('sifreKasasi.sifre')">
          <template #body="s">
            <div class="sifre-hucre">
              <code class="sifre-metin">{{ acilanId === s.data.id && acilanSifre ? acilanSifre : '••••••••' }}</code>
              <Button
                :icon="acilanId === s.data.id && acilanSifre ? 'pi pi-eye-slash' : 'pi pi-eye'"
                :aria-label="t('sifreKasasi.sifreyiGoster')"
                :title="t('sifreKasasi.sifreyiGoster')"
                severity="secondary"
                text
                rounded
                size="small"
                @click="sifreToggle(s.data)"
              />
              <Button
                icon="pi pi-copy"
                :aria-label="t('sifreKasasi.sifreyiKopyala')"
                :title="t('sifreKasasi.sifreyiKopyala')"
                severity="secondary"
                text
                rounded
                size="small"
                @click="sifreyiKopyala(s.data)"
              />
            </div>
          </template>
        </Column>
        <Column :header="t('sifreKasasi.kategori')">
          <template #body="s">
            <Tag
              v-if="s.data.kategori"
              :value="kategoriEtiketi(s.data.kategori)"
              severity="secondary"
            />
            <span v-else>-</span>
          </template>
        </Column>
        <Column :header="t('sifreKasasi.sure')">
          <template #body="s">
            <Tag
              :value="sureEtiketi(s.data)"
              :severity="sureSeverity(s.data)"
            />
          </template>
        </Column>
        <Column :header="t('common.actions')">
          <template #body="s">
            <div class="islem-butonlari">
              <Button
                icon="pi pi-pencil"
                :aria-label="t('common.edit')"
                :title="t('common.edit')"
                severity="info"
                text
                rounded
                size="small"
                :disabled="arsivMi(s.data) || (s.data.kapsam === 'GLOBAL' && !authStore.isAdmin)"
                @click="duzenle(s.data)"
              />
              <Button
                v-if="!arsivMi(s.data)"
                icon="pi pi-inbox"
                :aria-label="t('sifreKasasi.arsivle')"
                :title="t('sifreKasasi.arsivle')"
                severity="warn"
                text
                rounded
                size="small"
                :disabled="s.data.kapsam === 'GLOBAL' && !authStore.isAdmin"
                @click="arsivleOnay(s.data)"
              />
              <Button
                v-else
                icon="pi pi-replay"
                :aria-label="t('sifreKasasi.geriAl')"
                :title="t('sifreKasasi.geriAl')"
                severity="success"
                text
                rounded
                size="small"
                :disabled="s.data.kapsam === 'GLOBAL' && !authStore.isAdmin"
                @click="geriAlOnay(s.data)"
              />
            </div>
          </template>
        </Column>
      </DataTable>

      <ListeDurumu
        v-else
        :yukleniyor="yukleniyor"
        :hata="hata"
        :bos="!yukleniyor && !hata"
        :bos-mesaj="t('sifreKasasi.empty')"
        :bos-ipucu="t('sifreKasasi.emptyHint')"
        :eylem-etiketi="t('sifreKasasi.yeniKayit')"
        :yeniden-dene="yukle"
        @eylem="yeniKayit"
      />
    </div>

    <Dialog
      v-model:visible="dialog"
      :header="duzenlenen ? t('sifreKasasi.duzenle') : t('sifreKasasi.yeniKayit')"
      :modal="true"
      :style="{ width: '560px' }"
    >
      <div class="form-alani">
        <label>{{ t('sifreKasasi.baslik') }} <span class="zorunlu">*</span></label>
        <InputText
          v-model="form.baslik"
          class="w-full"
        />
      </div>

      <div class="form-alani">
        <label>{{ t('sifreKasasi.kullaniciAdi') }}</label>
        <InputText
          v-model="form.kullaniciAdi"
          class="w-full"
        />
      </div>

      <div class="form-alani">
        <label>{{ t('sifreKasasi.sifre') }} <span
          v-if="!duzenlenen"
          class="zorunlu"
        >*</span></label>
        <div class="p-inputgroup w-full">
          <InputText
            v-model="form.sifre"
            :type="sifreGorunur ? 'text' : 'password'"
            :placeholder="duzenlenen ? t('sifreKasasi.sifreDegistirIpucu') : ''"
            autocomplete="new-password"
          />
          <Button
            :icon="sifreGorunur ? 'pi pi-eye-slash' : 'pi pi-eye'"
            :aria-label="t('common.show')"
            severity="secondary"
            outlined
            @click="sifreGorunur = !sifreGorunur"
          />
        </div>
      </div>

      <div class="form-alani">
        <label>{{ t('sifreKasasi.url') }}</label>
        <InputText
          v-model="form.url"
          class="w-full"
        />
      </div>

      <div class="form-ikili">
        <div class="form-alani">
          <label>{{ t('sifreKasasi.kategori') }}</label>
          <Dropdown
            v-model="form.kategori"
            :options="kategoriSecenekleri"
            option-label="label"
            option-value="value"
            :show-clear="true"
            class="w-full"
          />
        </div>
        <div class="form-alani">
          <label>{{ t('sifreKasasi.gecerlilikGun') }}</label>
          <InputNumber
            v-model="form.gecerlilikGun"
            :min="0"
            :max="3650"
            :suffix="' ' + t('sifreKasasi.gun')"
            class="w-full"
          />
        </div>
      </div>

      <div class="form-alani">
        <label>{{ t('sifreKasasi.kapsam') }}</label>
        <div class="kapsam-secimi">
          <Button
            :label="t('sifreKasasi.kisiselSekme')"
            :severity="form.kapsam === 'KISISEL' ? 'primary' : 'secondary'"
            :outlined="form.kapsam !== 'KISISEL'"
            size="small"
            :disabled="!!duzenlenen"
            @click="kapsamSec('KISISEL')"
          />
          <Button
            :label="t('sifreKasasi.sirketSekme')"
            :severity="form.kapsam === 'GLOBAL' ? 'primary' : 'secondary'"
            :outlined="form.kapsam !== 'GLOBAL'"
            size="small"
            :disabled="!!duzenlenen || !authStore.isAdmin"
            @click="kapsamSec('GLOBAL')"
          />
        </div>
      </div>

      <div
        v-if="form.kapsam === 'GLOBAL'"
        class="form-alani"
      >
        <label>{{ t('sifreKasasi.gorunurluk') }}</label>
        <div class="kapsam-secimi">
          <Button
            :label="t('sifreKasasi.gorunurlukSahis')"
            :severity="form.sifreGorunurlugu === 'SAHIS' ? 'primary' : 'secondary'"
            :outlined="form.sifreGorunurlugu !== 'SAHIS'"
            size="small"
            @click="form.sifreGorunurlugu = 'SAHIS'"
          />
          <Button
            :label="t('sifreKasasi.gorunurlukTumu')"
            :severity="form.sifreGorunurlugu === 'TUMU' ? 'primary' : 'secondary'"
            :outlined="form.sifreGorunurlugu !== 'TUMU'"
            size="small"
            @click="form.sifreGorunurlugu = 'TUMU'"
          />
        </div>
        <small class="ipucu">{{ t('sifreKasasi.gorunurlukIpucu') }}</small>
      </div>

      <div class="form-alani">
        <label>{{ t('sifreKasasi.notlar') }}</label>
        <Textarea
          v-model="form.notlar"
          rows="3"
          class="w-full"
        />
      </div>

      <template #footer>
        <Button
          :label="t('common.cancel')"
          icon="pi pi-times"
          severity="secondary"
          text
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
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useConfirm } from 'primevue/useconfirm'
import { sifreKasaAPI } from '../api/index.js'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { usePanoyaKopyala } from '../composables/usePanoyaKopyala.js'
import { useAuthStore } from '../stores/authStore.js'
import ListeDurumu from '../components/ListeDurumu.vue'

const { t } = useI18n()
const toastBildirim = useToastBildirim()
const confirm = useConfirm()
const { kopyala } = usePanoyaKopyala()
const authStore = useAuthStore()

const kayitlar = ref([])
const yukleniyor = ref(false)
const hata = ref(null)

const kapsam = ref('KISISEL')
const durum = ref('AKTIF')
const kategori = ref(null)
const arama = ref('')

const dialog = ref(false)
const duzenlenen = ref(null)
const form = ref(bosForm())
const sifreGorunur = ref(false)
const kaydediliyor = ref(false)

// Acilan sifre yalnizca bellekte tutulur ve kisa sure sonra gizlenir.
const acilanId = ref(null)
const acilanSifre = ref('')
let gizlemeZamanlayici = null
let aramaZamanlayici = null

const kategoriSecenekleri = computed(() => [
  { label: t('sifreKasasi.katSistem'), value: 'SISTEM' },
  { label: t('sifreKasasi.katBanka'), value: 'BANKA' },
  { label: t('sifreKasasi.katEposta'), value: 'EPOSTA' },
  { label: t('sifreKasasi.katSosyal'), value: 'SOSYAL' },
  { label: t('sifreKasasi.katDiger'), value: 'DIGER' }
])

const durumSecenekleri = computed(() => [
  { label: t('sifreKasasi.durumAktif'), value: 'AKTIF' },
  { label: t('sifreKasasi.durumArsiv'), value: 'ARSIV' },
  { label: t('sifreKasasi.durumTumu'), value: 'TUMU' }
])

function bosForm() {
  return {
    baslik: '',
    kullaniciAdi: '',
    sifre: '',
    url: '',
    kategori: null,
    gecerlilikGun: null,
    kapsam: 'KISISEL',
    sifreGorunurlugu: 'SAHIS',
    notlar: ''
  }
}

const arsivMi = (k) => k.arsiv === true

const yukle = async () => {
  yukleniyor.value = true
  hata.value = null
  sifreTemizle()
  try {
    const params = { kapsam: kapsam.value, durum: durum.value }
    if (kategori.value) params.kategori = kategori.value
    if (arama.value && arama.value.trim()) params.q = arama.value.trim()
    const r = await sifreKasaAPI.getAll(params)
    kayitlar.value = Array.isArray(r.data) ? r.data : []
  } catch (err) {
    hata.value = err?.response?.data?.message || err?.message || t('sifreKasasi.yuklemeHatasi')
    kayitlar.value = []
  } finally {
    yukleniyor.value = false
  }
}

const kapsamDegistir = (yeni) => {
  if (kapsam.value === yeni) return
  kapsam.value = yeni
  durum.value = 'AKTIF'
  yukle()
}

const aramaGecikmeli = () => {
  clearTimeout(aramaZamanlayici)
  aramaZamanlayici = setTimeout(yukle, 300)
}

const yeniKayit = () => {
  duzenlenen.value = null
  form.value = bosForm()
  // Kullanici "Sirket" sekmesindeyse ve adminse, varsayilan kapsam global olsun.
  if (kapsam.value === 'GLOBAL' && authStore.isAdmin) {
    form.value.kapsam = 'GLOBAL'
  }
  sifreGorunur.value = false
  dialog.value = true
}

const duzenle = (kayit) => {
  duzenlenen.value = kayit
  form.value = {
    baslik: kayit.baslik || '',
    kullaniciAdi: kayit.kullaniciAdi || '',
    sifre: '', // bos: mevcut sifre korunur
    url: kayit.url || '',
    kategori: kayit.kategori || null,
    gecerlilikGun: kayit.gecerlilikGun || null,
    kapsam: kayit.kapsam || 'KISISEL',
    sifreGorunurlugu: kayit.sifreGorunurlugu || 'SAHIS',
    notlar: kayit.notlar || ''
  }
  sifreGorunur.value = false
  dialog.value = true
}

const kapsamSec = (yeni) => {
  form.value.kapsam = yeni
  if (yeni !== 'GLOBAL') form.value.sifreGorunurlugu = 'SAHIS'
}

const kaydet = async () => {
  // Cift gonderim engeli: ayni kaydin iki kez olusmasini onler.
  if (kaydediliyor.value) return
  if (!form.value.baslik || !form.value.baslik.trim()) {
    toastBildirim.uyari(t('sifreKasasi.baslikZorunlu'))
    return
  }
  if (!duzenlenen.value && !form.value.sifre) {
    toastBildirim.uyari(t('sifreKasasi.sifreZorunlu'))
    return
  }

  kaydediliyor.value = true
  try {
    const payload = {
      baslik: form.value.baslik.trim(),
      kullaniciAdi: form.value.kullaniciAdi || null,
      url: form.value.url || null,
      kategori: form.value.kategori || null,
      gecerlilikGun: form.value.gecerlilikGun || null,
      kapsam: form.value.kapsam,
      sifreGorunurlugu: form.value.kapsam === 'GLOBAL' ? form.value.sifreGorunurlugu : 'SAHIS',
      notlar: form.value.notlar || null
    }
    // Sifre yalniz gercekten girildiyse gonderilir; boylece duzenlemede
    // bos birakilirsa mevcut sifre korunur.
    if (form.value.sifre) payload.sifre = form.value.sifre

    if (duzenlenen.value) {
      await sifreKasaAPI.guncelle(duzenlenen.value.id, payload)
      toastBildirim.basarili(t('sifreKasasi.guncellendi'))
    } else {
      await sifreKasaAPI.olustur(payload)
      toastBildirim.basarili(t('sifreKasasi.olusturuldu'))
    }
    // Duz metni bellekten/DOM'dan temizle.
    form.value.sifre = ''
    dialog.value = false
    await yukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('sifreKasasi.kaydetmeHatasi'))
  } finally {
    kaydediliyor.value = false
  }
}

const sifreToggle = async (kayit) => {
  if (acilanId.value === kayit.id && acilanSifre.value) {
    sifreTemizle()
    return
  }
  await sifreAc(kayit, false)
}

const sifreyiKopyala = async (kayit) => {
  const duz = await sifreAc(kayit, true)
  if (duz) await kopyala(duz, t('sifreKasasi.sifreKopyalandi'))
}

const sifreAc = async (kayit, kopyalaModu) => {
  try {
    const r = await sifreKasaAPI.sifreAc(kayit.id)
    const duz = r.data?.sifre || ''
    if (!duz) {
      toastBildirim.uyari(t('sifreKasasi.sifreYok'))
      return ''
    }
    if (!kopyalaModu) {
      acilanId.value = kayit.id
      acilanSifre.value = duz
      // 30 sn sonra otomatik gizle.
      clearTimeout(gizlemeZamanlayici)
      gizlemeZamanlayici = setTimeout(sifreTemizle, 30000)
    }
    return duz
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('sifreKasasi.sifreAcilamadi'))
    return ''
  }
}

const sifreTemizle = () => {
  acilanId.value = null
  acilanSifre.value = ''
  clearTimeout(gizlemeZamanlayici)
}

const arsivleOnay = (kayit) => {
  confirm.require({
    message: t('sifreKasasi.arsivOnay', { ad: kayit.baslik }),
    header: t('sifreKasasi.arsivOnayBaslik'),
    icon: 'pi pi-exclamation-triangle',
    rejectProps: { label: t('common.vazgec'), severity: 'secondary', outlined: true, size: 'small' },
    acceptProps: { label: t('sifreKasasi.arsivle'), severity: 'danger', size: 'small' },
    accept: async () => {
      try {
        await sifreKasaAPI.arsivle(kayit.id)
        toastBildirim.basarili(t('sifreKasasi.arsivlendi'))
        await yukle()
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || t('sifreKasasi.arsivHatasi'))
      }
    },
    reject: () => {}
  })
}

const geriAlOnay = (kayit) => {
  confirm.require({
    message: t('sifreKasasi.geriAlOnay', { ad: kayit.baslik }),
    header: t('sifreKasasi.geriAl'),
    icon: 'pi pi-replay',
    rejectProps: { label: t('common.vazgec'), severity: 'secondary', outlined: true, size: 'small' },
    acceptProps: { label: t('sifreKasasi.geriAl'), severity: 'success', size: 'small' },
    accept: async () => {
      try {
        await sifreKasaAPI.geriAl(kayit.id)
        toastBildirim.basarili(t('sifreKasasi.geriAlindi'))
        await yukle()
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || t('sifreKasasi.geriAlHatasi'))
      }
    },
    reject: () => {}
  })
}

const kategoriEtiketi = (kod) => {
  const bulunan = kategoriSecenekleri.value.find((k) => k.value === kod)
  return bulunan ? bulunan.label : kod
}

const sureEtiketi = (k) => {
  if (k.durum === 'SURESIZ') return t('sifreKasasi.sureSuresiz')
  if (k.durum === 'SURESI_BITTI') return t('sifreKasasi.sureBitti')
  if (k.durum === 'SURE_YAKLASTI') return t('sifreKasasi.sureYaklasti', { gun: k.kalanGun })
  return t('sifreKasasi.sureGecerli')
}

const sureSeverity = (k) => {
  if (k.durum === 'SURESI_BITTI') return 'danger'
  if (k.durum === 'SURE_YAKLASTI') return 'warn'
  if (k.durum === 'GECERLI') return 'success'
  return 'secondary'
}

onMounted(yukle)
onUnmounted(() => {
  clearTimeout(gizlemeZamanlayici)
  clearTimeout(aramaZamanlayici)
})
</script>

<style scoped>
.sifre-kasa-sayfasi {
  padding: 0;
}
.sayfa-baslik {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.sayfa-baslik h1 {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
}
.kasa-uyari {
  margin-bottom: 16px;
}
.kasa-filtre {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
.kapsam-sekmeleri,
.kapsam-secimi {
  display: flex;
  gap: 8px;
}
.filtre-alani {
  min-width: 160px;
}
.arama-kutu {
  min-width: 220px;
}
.kasa-icerik {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 14px;
}
.sifre-hucre {
  display: flex;
  align-items: center;
  gap: 4px;
}
.sifre-metin {
  font-family: monospace;
  letter-spacing: 1px;
}
.islem-butonlari {
  display: flex;
  gap: 2px;
}
.kopyalanabilir {
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.kopyalanabilir:hover {
  color: var(--primary-color, #14b8a6);
}
.form-alani {
  margin-bottom: 16px;
}
.form-alani label {
  display: block;
  margin-bottom: 6px;
  font-weight: 600;
  font-size: 13px;
}
.form-ikili {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
.zorunlu {
  color: var(--red-500, #ef4444);
}
.ipucu {
  display: block;
  margin-top: 6px;
  color: var(--text-color-secondary, #64748b);
}
.w-full {
  width: 100%;
}
</style>