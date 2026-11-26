<template>
  <Dialog
    :visible="visible"
    :header="cariId ? t('cariHesaplar.duzenle') : t('cariHesaplar.yeniCariHesap')"
    :modal="true"
    style="width: 780px"
    :draggable="false"
    @update:visible="kapat"
  >
    <p
      v-if="formHatalariVar"
      class="form-hata-ozet"
      role="alert"
    >
      <i
        class="pi pi-exclamation-triangle"
        aria-hidden="true"
      />
      {{ t('cariHesaplar.formHatalariVar') }}
    </p>

    <div class="dialog-form">
      <!-- Sol kolon: genel bilgiler -->
      <div class="form-kolon">
        <div class="form-section">
          <div class="form-section-title">
            {{ t('cariHesaplar.genelBilgiler') }}
          </div>
          <div class="form-group">
            <label for="ad">{{ t('cariHesaplar.cariAdi') }} <span class="required">*</span></label>
            <InputText
              id="ad"
              v-model="form.ad"
              :placeholder="t('cariHesaplar.adPlaceholder')"
              class="w-full"
              required
              :aria-invalid="alanHatalari.ad ? 'true' : 'false'"
              :aria-describedby="alanHatalari.ad ? 'ad-hata' : undefined"
              @input="hataTemizle('ad')"
            />
            <small
              v-if="alanHatalari.ad"
              id="ad-hata"
              class="alan-hata"
            >{{ alanHatalari.ad }}</small>
          </div>
          <div class="form-group">
            <label>{{ t('cariHesaplar.fotograf') }}</label>
            <div class="foto-satir">
              <img
                v-if="form.fotoUrl || form.fotoThumbUrl"
                :src="form.fotoThumbUrl || form.fotoUrl"
                class="foto-onizle"
                alt="foto"
                loading="lazy"
                decoding="async"
              >
              <input
                ref="fotoInput"
                type="file"
                accept="image/*"
                hidden
                @change="fotoSec"
              >
              <Button
                :label="t('cariHesaplar.fotografYukle')"
                icon="pi pi-image"
                class="p-button-outlined"
                @click="$refs.fotoInput.click()"
              />
              <Button
                v-if="form.fotoUrl || form.fotoThumbUrl"
                :label="t('stoklar.kaldir')"
                icon="pi pi-times"
                class="p-button-text p-button-danger"
                @click="form.fotoUrl = ''; form.fotoThumbUrl = ''"
              />
            </div>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label for="tur">{{ t('cariHesaplar.cariTuru') }}</label>
              <Dropdown
                v-model="form.tur"
                :options="cariTurSecenekleri"
                option-label="label"
                option-value="value"
                :placeholder="t('faturalar.seciniz')"
                class="w-full"
              />
            </div>
            <div class="form-group">
              <label for="vergiNumarasi">{{ t('cariHesaplar.vergiNoTc') }}</label>
              <InputText
                id="vergiNumarasi"
                v-model="form.vergiNumarasi"
                :placeholder="t('cariHesaplar.vergiNoPlaceholder')"
                class="w-full"
                inputmode="numeric"
                maxlength="11"
                :aria-invalid="alanHatalari.vergiNumarasi ? 'true' : 'false'"
                :aria-describedby="alanHatalari.vergiNumarasi ? 'vergiNumarasi-hata' : undefined"
                @input="hataTemizle('vergiNumarasi')"
              />
              <small
                v-if="alanHatalari.vergiNumarasi"
                id="vergiNumarasi-hata"
                class="alan-hata"
              >{{ alanHatalari.vergiNumarasi }}</small>
            </div>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label for="telefon">{{ t('cariHesaplar.telefon') }}</label>
              <InputText
                id="telefon"
                v-model="form.telefon"
                placeholder="05XX XXX XX XX"
                class="w-full"
                inputmode="tel"
                maxlength="20"
                :aria-invalid="alanHatalari.telefon ? 'true' : 'false'"
                :aria-describedby="alanHatalari.telefon ? 'telefon-hata' : undefined"
                @input="hataTemizle('telefon')"
              />
              <small
                v-if="alanHatalari.telefon"
                id="telefon-hata"
                class="alan-hata"
              >{{ alanHatalari.telefon }}</small>
            </div>
            <div class="form-group">
              <label for="email">{{ t('cariHesaplar.eposta') }}</label>
              <InputText
                id="email"
                v-model="form.email"
                placeholder="ornek@firma.com"
                class="w-full"
                inputmode="email"
                autocomplete="email"
                :aria-invalid="alanHatalari.email ? 'true' : 'false'"
                :aria-describedby="alanHatalari.email ? 'email-hata' : undefined"
                @input="hataTemizle('email')"
              />
              <small
                v-if="alanHatalari.email"
                id="email-hata"
                class="alan-hata"
              >{{ alanHatalari.email }}</small>
            </div>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label for="vergiDairesi">{{ t('cariHesaplar.vergiDairesi') }}</label>
              <InputText
                id="vergiDairesi"
                v-model="form.vergiDairesi"
                :placeholder="t('cariHesaplar.vergiDairesiPlaceholder')"
                class="w-full"
              />
            </div>
            <div class="form-group">
              <label for="iban">{{ t('cariHesaplar.iban') }}</label>
              <InputText
                id="iban"
                v-model="form.iban"
                placeholder="TR..."
                class="w-full"
                maxlength="34"
                :aria-invalid="alanHatalari.iban ? 'true' : 'false'"
                :aria-describedby="alanHatalari.iban ? 'iban-hata' : 'iban-yardim'"
                @input="hataTemizle('iban')"
              />
              <small
                v-if="alanHatalari.iban"
                id="iban-hata"
                class="alan-hata"
              >{{ alanHatalari.iban }}</small>
              <small
                v-else-if="ibanGecerli === true"
                class="iban-gecerli"
              >&#x2713; {{ t('cariHesaplar.ibanGecerli') }}</small>
              <small
                v-else-if="ibanGecerli === false"
                class="iban-gecersiz"
              >&#x2717; {{ t('cariHesaplar.ibanGecersiz') }}</small>
              <small
                id="iban-yardim"
                class="iban-yardim"
              >{{ t('cariHesaplar.ibanYardim') }}</small>
            </div>
          </div>
        </div>
      </div>

      <!-- Sag kolon: adres, yetkili, kredi/vade -->
      <div class="form-kolon">
        <div class="form-section">
          <div class="form-section-title">
            {{ t('cariHesaplar.adresBilgileri') }}
          </div>
          <div class="form-row">
            <div class="form-group">
              <label for="il">{{ t('cariHesaplar.il') }}</label>
              <InputText
                id="il"
                v-model="form.il"
                :placeholder="t('cariHesaplar.il')"
                class="w-full"
              />
            </div>
            <div class="form-group">
              <label for="ilce">{{ t('cariHesaplar.ilce') }}</label>
              <InputText
                id="ilce"
                v-model="form.ilce"
                :placeholder="t('cariHesaplar.ilce')"
                class="w-full"
              />
            </div>
          </div>
          <div class="form-group">
            <label for="adres">{{ t('cariHesaplar.adres') }}</label>
            <Textarea
              id="adres"
              v-model="form.adres"
              :placeholder="t('cariHesaplar.adresPlaceholder')"
              rows="2"
              class="w-full"
            />
          </div>
        </div>

        <div class="form-section">
          <div class="form-section-title">
            {{ t('cariHesaplar.yetkiliKisi') }}
          </div>
          <div class="form-row">
            <div class="form-group">
              <label for="yetkiliKisi">{{ t('cariHesaplar.adSoyad') }}</label>
              <InputText
                id="yetkiliKisi"
                v-model="form.yetkiliKisi"
                :placeholder="t('cariHesaplar.adSoyadPlaceholder')"
                class="w-full"
              />
            </div>
            <div class="form-group">
              <label for="yetkiliTelefon">{{ t('cariHesaplar.telefon') }}</label>
              <InputText
                id="yetkiliTelefon"
                v-model="form.yetkiliTelefon"
                placeholder="0XXX XXX XX XX"
                class="w-full"
              />
            </div>
          </div>
        </div>

        <div class="form-section">
          <div class="form-section-title">
            {{ t('cariHesaplar.krediVade') }}
          </div>
          <div class="form-row">
            <div class="form-group">
              <label for="krediLimiti">{{ t('cariHesaplar.krediLimitiTL') }}</label>
              <InputNumber
                id="krediLimiti"
                v-model="form.krediLimiti"
                :min="0"
                :min-fraction-digits="2"
                class="w-full"
              />
            </div>
            <div class="form-group">
              <label for="odemeVadesi">{{ t('cariHesaplar.odemeVadesiGun') }}</label>
              <InputNumber
                id="odemeVadesi"
                v-model="form.odemeVadesi"
                :min="0"
                :min-fraction-digits="0"
                class="w-full"
              />
            </div>
            <div class="form-group">
              <label for="paraBirimi">{{ t('cariHesaplar.paraBirimi') }}</label>
              <Dropdown
                id="paraBirimi"
                v-model="form.paraBirimi"
                :options="paraBirimiSecenekleri"
                option-label="label"
                option-value="value"
                class="w-full"
              />
            </div>
          </div>
        </div>
      </div>

      <!-- Tam genislik: ek bilgiler -->
      <div class="form-section form-section-genis">
        <div class="form-section-title">
          {{ t('cariHesaplar.ekBilgiler') }}
        </div>
        <!-- Satis temsilcisi: temsilci performans raporu bu alana gore gruplar.
             Once yazma yolu yoktu, alan her zaman null kaldi ve rapor tek
             satirda "temsilci atanmamis" donuyordu. -->
        <div class="form-row">
          <div class="form-group">
            <label for="temsilciId">{{ t('cariHesaplar.satisTemsilcisi') }}</label>
            <Dropdown
              id="temsilciId"
              v-model="form.temsilciId"
              :options="temsilciSecenekleri"
              option-label="ad"
              option-value="id"
              :placeholder="t('cariHesaplar.temsilciSeciniz')"
              class="w-full"
              show-clear
              filter
              filter-by="ad,email"
              :loading="temsilcilerYukleniyor"
            />
          </div>
          <div class="form-group">
            <label for="etiketler">{{ t('cariHesaplar.etiketler') }}</label>
            <InputText
              id="etiketler"
              v-model="form.etiketler"
              :placeholder="t('cariHesaplar.etiketlerPlaceholder')"
              class="w-full"
            />
          </div>
        </div>
        <div class="form-group">
          <label for="notlar">{{ t('cariHesaplar.notlar') }}</label>
          <Textarea
            id="notlar"
            v-model="form.notlar"
            :placeholder="t('cariHesaplar.notlarPlaceholder')"
            rows="3"
            class="w-full"
          />
        </div>
        <div
          v-if="cariId"
          class="form-group"
        >
          <label>{{ t('cariHesaplar.aktif') }}</label>
          <InputSwitch v-model="form.aktif" />
        </div>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <Button
          :label="t('common.cancel')"
          icon="pi pi-times"
          class="p-button-text"
          @click="kapat"
        />
        <Button
          :label="cariId ? t('cariHesaplar.guncelle') : t('common.save')"
          icon="pi pi-check"
          :loading="saving"
          @click="kaydet"
        />
      </div>
    </template>
  </Dialog>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '../../stores/authStore.js'
import { useCariHesapStore } from '../../stores/cariHesapStore.js'
import { useToastBildirim } from '../../composables/useToastBildirim.js'
import { useFormKorumasi } from '../../composables/useFormKorumasi.js'
import { cariFormDogrula } from '../../composables/cariFormDogrulama.js'
import { cariHesapAPI, kullaniciAPI, uploadAPI } from '../../api/index.js'
import { unwrapList } from '../../api/utils/unwrap.js'
import { resimDogrula } from '../../utils/dosyaDogrula.js'
import { resimSikistir } from '../../utils/resimSikistir.js'

const props = defineProps({
  visible: { type: Boolean, default: false },
  cariId: { type: [Number, String], default: null },
  baslangicVerisi: { type: Object, default: null }
})

const emit = defineEmits(['update:visible', 'kaydedildi'])
const { t } = useI18n()
const authStore = useAuthStore()
const cariHesapStore = useCariHesapStore()
const toastBildirim = useToastBildirim()

const saving = ref(false)
const submitted = ref(false)
const alanHatalari = ref({})
const formHatalariVar = computed(() => Object.keys(alanHatalari.value).length > 0)
const fotoInput = ref(null)

const cariTurSecenekleri = computed(() => [
  { label: t('cariTur.musteri'), value: 'Musteri' },
  { label: t('cariTur.tedarikci'), value: 'Tedarikci' },
  { label: t('cariTur.herIkisi'), value: 'Her Ikisi' }
])

const paraBirimiSecenekleri = [
  { label: 'TRY (₺)', value: 'TRY' },
  { label: 'USD ($)', value: 'USD' },
  { label: 'EUR (€)', value: 'EUR' },
  { label: 'GBP (£)', value: 'GBP' }
]

const bosForm = () => ({
  ad: '',
  tur: '',
  vergiNumarasi: '',
  vergiDairesi: '',
  telefon: '',
  email: '',
  iban: '',
  il: '',
  ilce: '',
  adres: '',
  yetkiliKisi: '',
  yetkiliTelefon: '',
  krediLimiti: null,
  odemeVadesi: 0,
  temsilciId: null,
  temsilciAd: null,
  notlar: '',
  etiketler: '',
  fotoUrl: '',
  fotoThumbUrl: '',
  aktif: true,
  paraBirimi: 'TRY'
})

const form = ref(bosForm())
const { temizle: formTemizle } = useFormKorumasi(form)

const ibanGecerli = computed(() => {
  const val = (form.value.iban || '').replace(/\s/g, '').toUpperCase()
  if (!val || val.length < 5) return null
  if (!val.startsWith('TR') || val.length !== 26) return false
  const rearranged = val.slice(4) + val.slice(0, 4)
  const numeric = rearranged.replace(/[A-Z]/g, (c) => String(c.charCodeAt(0) - 55))
  try {
    return BigInt(numeric) % 97n === 1n
  } catch {
    return false
  }
})

const hataTemizle = (alan) => {
  if (!alanHatalari.value[alan]) return
  const yeni = { ...alanHatalari.value }
  delete yeni[alan]
  alanHatalari.value = yeni
}

const formuDogrula = () => {
  alanHatalari.value = cariFormDogrula({
    form: form.value,
    ibanGecerli: ibanGecerli.value,
    t
  })
  return Object.keys(alanHatalari.value).length === 0
}

const formDegeri = (c) => ({
  ad: c.ad || '',
  tur: c.tur || '',
  vergiNumarasi: c.vergiNumarasi || '',
  vergiDairesi: c.vergiDairesi || '',
  telefon: c.telefon || '',
  email: c.email || '',
  iban: c.iban || '',
  il: c.il || '',
  ilce: c.ilce || '',
  adres: c.adres || '',
  yetkiliKisi: c.yetkiliKisi || '',
  yetkiliTelefon: c.yetkiliTelefon || '',
  krediLimiti: c.krediLimiti ?? null,
  odemeVadesi: c.odemeVadesi ?? 0,
  temsilciId: c.temsilciId ?? null,
  temsilciAd: c.temsilciAd || null,
  notlar: c.notlar || '',
  etiketler: c.etiketler || '',
  fotoUrl: c.fotoUrl || '',
  fotoThumbUrl: c.fotoThumbUrl || '',
  aktif: c.aktif !== false,
  paraBirimi: c.paraBirimi || 'TRY'
})

const formuHazirla = async () => {
  temsilcileriYukle()
  let kaynak = props.baslangicVerisi
  if (!kaynak && props.cariId != null) {
    const liste = Array.isArray(cariHesapStore.cariHesaplar) ? cariHesapStore.cariHesaplar : []
    kaynak = liste.find((c) => String(c.id) === String(props.cariId)) || null
    if (!kaynak) {
      try {
        const r = await cariHesapAPI.getById(props.cariId)
        kaynak = r?.data || null
      } catch {
        kaynak = null
      }
    }
  }
  form.value = kaynak ? formDegeri(kaynak) : bosForm()
  submitted.value = false
  alanHatalari.value = {}
  formTemizle()
}

const kapat = () => {
  submitted.value = false
  alanHatalari.value = {}
  emit('update:visible', false)
}

const fotoSec = async (e) => {
  const file = e.target.files[0]
  if (!file) return
  const hata = resimDogrula(file)
  if (hata) {
    toastBildirim.hata(t(hata.key, hata.params))
    e.target.value = ''
    return
  }
  try {
    const kucuk = await resimSikistir(file)
    const r = await uploadAPI.foto(kucuk)
    form.value.fotoUrl = r.data?.url || ''
    form.value.fotoThumbUrl = r.data?.thumbUrl || ''
    toastBildirim.basarili(t('cariHesaplar.fotografYuklendi'))
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.error || err?.response?.data?.message || t('cariHesaplar.fotografYuklenemedi'))
  } finally {
    e.target.value = ''
  }
}

// Satis temsilcisi secenekleri. Kullanici listesi cari ekraninda zaten
// yuklenmiyordu; dropdown ilk acilista bos kalsin diye forma girildiginde bir
// kez cekilir. Raporlar bu alana gore grupladigi icin secim zorunlu degil
// ama atanmayan cariler raporda ayri satirda gosterilir.
const temsilciSecenekleri = ref([])
const temsilcilerYukleniyor = ref(false)
let temsilcilerYuklendi = false

const temsilcileriYukle = async () => {
  if (temsilcilerYuklendi || temsilcilerYukleniyor.value) return
  temsilcilerYukleniyor.value = true
  try {
    const r = await kullaniciAPI.getAll({ size: 500 })
    const liste = unwrapList(r)
    // API sirket filtresini uyguluyor olsa da liste kendi sirketine gore
    // daraltilir: yanlis sirketten temsilci secilmemeli.
    const sirketId = authStore?.sirketId
    temsilciSecenekleri.value = sirketId ? liste.filter((k) => k.sirketId === sirketId) : liste
    temsilcilerYuklendi = true
  } catch {
    temsilciSecenekleri.value = []
  } finally {
    temsilcilerYukleniyor.value = false
  }
}

watch(
  () => props.visible,
  (gorunur) => {
    if (gorunur) formuHazirla()
  },
  { immediate: true }
)

const kaydet = async () => {
  if (!props.visible) return
  submitted.value = true
  if (!formuDogrula()) return

  // Temsilcinin adi denormalize olarak saklanir (temsilci performans raporu
  // JOIN yapmadan bu alana gore gruplar). Dropdown yalnizca id dondurdugu icin
  // ad burada cozulur; temsilci temizlendiyse alanlar da temizlenir.
  const seciliTemsilci = temsilciSecenekleri.value.find((k) => k.id === form.value.temsilciId)
  const temsilciAd = form.value.temsilciId ? seciliTemsilci?.ad || null : null
  const payload = { ...form.value, temsilciAd }

  saving.value = true
  try {
    if (props.cariId != null) {
      await cariHesapStore.updateCariHesap(props.cariId, payload)
      toastBildirim.basarili(t('cariHesaplar.guncellendi'))
    } else {
      await cariHesapStore.addCariHesap(payload)
      toastBildirim.basarili(t('cariHesaplar.olusturuldu'))
    }
    formTemizle()
    alanHatalari.value = {}
    emit('kaydedildi')
    emit('update:visible', false)
  } catch (error) {
    const mesaj = error?.response?.data?.message || ''
    if (/vergi numaras/i.test(mesaj)) {
      alanHatalari.value = { ...alanHatalari.value, vergiNumarasi: mesaj }
    } else {
      toastBildirim.hata(mesaj || t('cariHesaplar.islemBasarisiz'))
    }
  } finally {
    saving.value = false
  }
}

defineExpose({ kaydet })
</script>

<style scoped>
.dialog-form {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}
.form-kolon {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-width: 0;
}
.form-section {
  padding: 16px;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: var(--bg-secondary);
}
.form-section-genis {
  grid-column: 1 / -1;
}
.form-section-title {
  font-size: 13px;
  font-weight: 700;
  color: var(--accent);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 14px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--border);
}
.form-group {
  margin-bottom: 14px;
}
.form-group:last-child {
  margin-bottom: 0;
}
.form-group label {
  display: block;
  margin-bottom: 6px;
  font-weight: 700;
  color: var(--text-primary) !important;
  font-size: 13px;
}
.form-group .required {
  color: var(--danger);
}
.alan-hata {
  display: block;
  margin-top: 4px;
  font-size: 11.5px;
  line-height: 1.4;
  color: var(--danger);
}
.form-hata-ozet {
  display: flex;
  align-items: center;
  gap: 7px;
  margin: 0 0 14px;
  padding: 9px 12px;
  border: 1px solid var(--danger-border);
  border-radius: 10px;
  background: var(--danger-soft);
  color: var(--danger);
  font-size: 12.5px;
}
.form-group :deep(.p-inputtext[aria-invalid='true']),
.form-group :deep(.p-textarea[aria-invalid='true']) {
  border-color: var(--danger) !important;
  box-shadow: 0 0 0 3px var(--danger-soft) !important;
}
.form-group :deep(.p-inputtext),
.form-group :deep(.p-textarea) {
  width: 100%;
  background: var(--bg-primary) !important;
  border: 1px solid var(--border) !important;
  border-radius: 8px;
  padding: 10px 12px !important;
  color: var(--text-primary) !important;
  font-size: 14px;
}
.form-group :deep(.p-inputtext:enabled:focus),
.form-group :deep(.p-textarea:enabled:focus) {
  border-color: var(--accent) !important;
  box-shadow: 0 0 0 3px var(--accent-soft-strong) !important;
}
.form-group :deep(.p-inputtext.p-invalid),
.form-group :deep(.p-textarea.p-invalid) {
  border-color: var(--danger) !important;
}
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
.foto-satir {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.foto-onizle {
  width: 56px;
  height: 56px;
  object-fit: cover;
  border-radius: 10px;
  border: 1px solid var(--border);
}
.iban-gecerli {
  display: block;
  color: var(--success);
  font-size: 11px;
  margin-top: 2px;
}
.iban-gecersiz {
  display: block;
  color: var(--danger);
  font-size: 11px;
  margin-top: 2px;
}
.iban-yardim {
  display: block;
  color: var(--text-secondary);
  font-size: 11px;
  margin-top: 2px;
}
@media (max-width: 900px) {
  .dialog-form {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
