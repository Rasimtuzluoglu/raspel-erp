<template>
  <div class="kitaplik-sayfasi">
    <div class="sayfa-baslik">
      <h1>
        <i class="pi pi-bookmark" />
        {{ t('kitaplik.title') }}
      </h1>
      <Button
        :label="t('kitaplik.yeniKayit')"
        icon="pi pi-plus"
        @click="yeniKayit"
      />
    </div>

    <div class="kitaplik-filtre">
      <div class="sekme-grubu">
        <Button
          :label="t('kitaplik.yerImleriSekme')"
          icon="pi pi-link"
          :severity="tur === 'YER_IMI' ? 'primary' : 'secondary'"
          :outlined="tur !== 'YER_IMI'"
          size="small"
          @click="sekmeDegistir('YER_IMI')"
        />
        <Button
          :label="t('kitaplik.snippetSekme')"
          icon="pi pi-code"
          :severity="tur === 'SNIPPET' ? 'primary' : 'secondary'"
          :outlined="tur !== 'SNIPPET'"
          size="small"
          @click="sekmeDegistir('SNIPPET')"
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
        :placeholder="t('kitaplik.kategori')"
        class="filtre-alani"
        @change="yukle"
      />
      <InputText
        v-model="arama"
        :placeholder="t('kitaplik.aramaPlaceholder')"
        class="arama-kutu"
        @input="aramaGecikmeli"
      />
    </div>

    <div class="kitaplik-icerik">
      <DataTable
        v-if="!yukleniyor && !hata && kayitlar.length > 0"
        :value="kayitlar"
        striped-rows
        responsive-layout="scroll"
        data-key="id"
      >
        <Column
          field="baslik"
          :header="t('kitaplik.baslik')"
          sortable
        />
        <Column :header="tur === 'YER_IMI' ? t('kitaplik.url') : t('kitaplik.icerik')">
          <template #body="s">
            <div
              v-if="tur === 'YER_IMI'"
              class="satir-icerik"
            >
              <a
                v-if="guvenliUrl(s.data.url)"
                :href="guvenliUrl(s.data.url)"
                target="_blank"
                rel="noopener noreferrer"
                class="baglanti"
              >{{ s.data.url }}</a>
              <span v-else>{{ s.data.url || '-' }}</span>
              <Button
                icon="pi pi-copy"
                :aria-label="t('kitaplik.kopyala')"
                :title="t('kitaplik.kopyala')"
                severity="secondary"
                text
                rounded
                size="small"
                @click="kopyala(s.data.url || '', t('kitaplik.kopyalandi'))"
              />
            </div>
            <div
              v-else
              class="satir-icerik"
            >
              <code class="snippet-onizleme">{{ snippetOnizleme(s.data.icerik) }}</code>
              <Button
                icon="pi pi-copy"
                :aria-label="t('kitaplik.kopyala')"
                :title="t('kitaplik.kopyala')"
                severity="secondary"
                text
                rounded
                size="small"
                @click="kopyala(s.data.icerik || '', t('kitaplik.kopyalandi'))"
              />
            </div>
          </template>
        </Column>
        <Column :header="t('kitaplik.kategori')">
          <template #body="s">
            <Tag
              v-if="s.data.kategori"
              :value="kategoriEtiketi(s.data.kategori)"
              severity="secondary"
            />
            <span v-else>-</span>
          </template>
        </Column>
        <Column :header="t('kitaplik.kapsam')">
          <template #body="s">
            <Tag
              :value="kapsamEtiketi(s.data)"
              :severity="s.data.kapsam === 'GLOBAL' ? 'info' : 'secondary'"
            />
          </template>
        </Column>
        <Column :header="t('common.actions')">
          <template #body="s">
            <div class="islem-butonlari">
              <Button
                icon="pi pi-external-link"
                :aria-label="t('kitaplik.ac')"
                :title="t('kitaplik.ac')"
                severity="secondary"
                text
                rounded
                size="small"
                :disabled="!guvenliUrl(s.data.url)"
                @click="ac(s.data)"
              />
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
                :aria-label="t('kitaplik.arsivle')"
                :title="t('kitaplik.arsivle')"
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
                :aria-label="t('kitaplik.geriAl')"
                :title="t('kitaplik.geriAl')"
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
        :bos-mesaj="tur === 'YER_IMI' ? t('kitaplik.yerImiEmpty') : t('kitaplik.snippetEmpty')"
        :bos-ipucu="t('kitaplik.emptyHint')"
        :eylem-etiketi="t('kitaplik.yeniKayit')"
        :yeniden-dene="yukle"
        @eylem="yeniKayit"
      />
    </div>

    <Dialog
      v-model:visible="dialog"
      :header="duzenlenen ? t('kitaplik.duzenle') : t('kitaplik.yeniKayit')"
      :modal="true"
      :style="{ width: '600px' }"
    >
      <div class="form-alani">
        <label>{{ t('kitaplik.tur') }}</label>
        <div class="sekme-grubu">
          <Button
            :label="t('kitaplik.yerImi')"
            :severity="form.tur === 'YER_IMI' ? 'primary' : 'secondary'"
            :outlined="form.tur !== 'YER_IMI'"
            size="small"
            :disabled="!!duzenlenen"
            @click="form.tur = 'YER_IMI'"
          />
          <Button
            :label="t('kitaplik.snippet')"
            :severity="form.tur === 'SNIPPET' ? 'primary' : 'secondary'"
            :outlined="form.tur !== 'SNIPPET'"
            size="small"
            :disabled="!!duzenlenen"
            @click="form.tur = 'SNIPPET'"
          />
        </div>
      </div>

      <div class="form-alani">
        <label>{{ t('kitaplik.baslik') }} <span class="zorunlu">*</span></label>
        <InputText
          v-model="form.baslik"
          class="w-full"
        />
      </div>

      <div
        v-if="form.tur === 'YER_IMI'"
        class="form-alani"
      >
        <label>{{ t('kitaplik.url') }} <span class="zorunlu">*</span></label>
        <InputText
          v-model="form.url"
          placeholder="https://..."
          class="w-full"
        />
      </div>

      <div
        v-else
        class="form-alani"
      >
        <label>{{ t('kitaplik.icerik') }} <span class="zorunlu">*</span></label>
        <Textarea
          v-model="form.icerik"
          rows="6"
          class="w-full"
          auto-resize
        />
      </div>

      <div class="form-ikili">
        <div class="form-alani">
          <label>{{ t('kitaplik.kategori') }}</label>
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
          <label>{{ t('kitaplik.kapsam') }}</label>
          <div class="sekme-grubu">
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
      </div>

      <div
        v-if="form.kapsam === 'GLOBAL'"
        class="form-alani"
      >
        <label>{{ t('kitaplik.gorunurluk') }}</label>
        <div class="sekme-grubu">
          <Button
            :label="t('kitaplik.gorunurlukSahis')"
            :severity="form.gorunurluk === 'SAHIS' ? 'primary' : 'secondary'"
            :outlined="form.gorunurluk !== 'SAHIS'"
            size="small"
            @click="form.gorunurluk = 'SAHIS'"
          />
          <Button
            :label="t('kitaplik.gorunurlukTumu')"
            :severity="form.gorunurluk === 'TUMU' ? 'primary' : 'secondary'"
            :outlined="form.gorunurluk !== 'TUMU'"
            size="small"
            @click="form.gorunurluk = 'TUMU'"
          />
        </div>
        <small class="ipucu">{{ t('kitaplik.gorunurlukIpucu') }}</small>
      </div>

      <div class="form-alani">
        <label>{{ t('kitaplik.notlar') }}</label>
        <Textarea
          v-model="form.notlar"
          rows="2"
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
import { kitaplikAPI } from '../api/index.js'
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

const tur = ref('YER_IMI')
const durum = ref('AKTIF')
const kategori = ref(null)
const arama = ref('')

const dialog = ref(false)
const duzenlenen = ref(null)
const form = ref(bosForm())
const kaydediliyor = ref(false)

let aramaZamanlayici = null

const kategoriSecenekleri = computed(() => [
  { label: t('kitaplik.katSistem'), value: 'SISTEM' },
  { label: t('kitaplik.katArac'), value: 'ARAC' },
  { label: t('kitaplik.katDokuman'), value: 'DOKUMAN' },
  { label: t('kitaplik.katKod'), value: 'KOD' },
  { label: t('kitaplik.katDiger'), value: 'DIGER' }
])

const durumSecenekleri = computed(() => [
  { label: t('sifreKasasi.durumAktif'), value: 'AKTIF' },
  { label: t('sifreKasasi.durumArsiv'), value: 'ARSIV' },
  { label: t('sifreKasasi.durumTumu'), value: 'TUMU' }
])

function bosForm() {
  return {
    tur: tur.value,
    baslik: '',
    url: '',
    icerik: '',
    kategori: null,
    notlar: '',
    kapsam: 'KISISEL',
    gorunurluk: 'SAHIS'
  }
}

const arsivMi = (k) => k.arsiv === true

const yukle = async () => {
  yukleniyor.value = true
  hata.value = null
  try {
    const params = { tur: tur.value, durum: durum.value }
    if (kategori.value) params.kategori = kategori.value
    if (arama.value && arama.value.trim()) params.q = arama.value.trim()
    const r = await kitaplikAPI.getAll(params)
    kayitlar.value = Array.isArray(r.data) ? r.data : []
  } catch (err) {
    hata.value = err?.response?.data?.message || err?.message || t('kitaplik.yuklemeHatasi')
    kayitlar.value = []
  } finally {
    yukleniyor.value = false
  }
}

const sekmeDegistir = (yeni) => {
  if (tur.value === yeni) return
  tur.value = yeni
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
  dialog.value = true
}

const duzenle = (kayit) => {
  duzenlenen.value = kayit
  form.value = {
    tur: kayit.tur,
    baslik: kayit.baslik || '',
    url: kayit.url || '',
    icerik: kayit.icerik || '',
    kategori: kayit.kategori || null,
    notlar: kayit.notlar || '',
    kapsam: kayit.kapsam || 'KISISEL',
    gorunurluk: kayit.gorunurluk || 'SAHIS'
  }
  dialog.value = true
}

const kapsamSec = (yeni) => {
  form.value.kapsam = yeni
  if (yeni !== 'GLOBAL') form.value.gorunurluk = 'SAHIS'
}

const kaydet = async () => {
  // Cift gonderim engeli.
  if (kaydediliyor.value) return
  if (!form.value.baslik || !form.value.baslik.trim()) {
    toastBildirim.uyari(t('kitaplik.baslikZorunlu'))
    return
  }
  if (form.value.tur === 'YER_IMI' && (!form.value.url || !form.value.url.trim())) {
    toastBildirim.uyari(t('kitaplik.urlZorunlu'))
    return
  }
  if (form.value.tur === 'SNIPPET' && (!form.value.icerik || !form.value.icerik.trim())) {
    toastBildirim.uyari(t('kitaplik.icerikZorunlu'))
    return
  }

  kaydediliyor.value = true
  try {
    const payload = {
      tur: form.value.tur,
      baslik: form.value.baslik.trim(),
      url: form.value.tur === 'YER_IMI' ? form.value.url.trim() : null,
      icerik: form.value.tur === 'SNIPPET' ? form.value.icerik : null,
      kategori: form.value.kategori || null,
      notlar: form.value.notlar || null,
      kapsam: form.value.kapsam,
      gorunurluk: form.value.kapsam === 'GLOBAL' ? form.value.gorunurluk : 'SAHIS'
    }
    if (duzenlenen.value) {
      await kitaplikAPI.guncelle(duzenlenen.value.id, payload)
      toastBildirim.basarili(t('kitaplik.guncellendi'))
    } else {
      await kitaplikAPI.olustur(payload)
      toastBildirim.basarili(t('kitaplik.olusturuldu'))
    }
    dialog.value = false
    await yukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('kitaplik.kaydetmeHatasi'))
  } finally {
    kaydediliyor.value = false
  }
}

const arsivleOnay = (kayit) => {
  confirm.require({
    message: t('kitaplik.arsivOnay', { ad: kayit.baslik }),
    header: t('kitaplik.arsivOnayBaslik'),
    icon: 'pi pi-exclamation-triangle',
    rejectProps: { label: t('common.vazgec'), severity: 'secondary', outlined: true, size: 'small' },
    acceptProps: { label: t('kitaplik.arsivle'), severity: 'danger', size: 'small' },
    accept: async () => {
      try {
        await kitaplikAPI.arsivle(kayit.id)
        toastBildirim.basarili(t('kitaplik.arsivlendi'))
        await yukle()
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || t('kitaplik.arsivHatasi'))
      }
    },
    reject: () => {}
  })
}

const geriAlOnay = (kayit) => {
  confirm.require({
    message: t('kitaplik.geriAlOnay', { ad: kayit.baslik }),
    header: t('kitaplik.geriAl'),
    icon: 'pi pi-replay',
    rejectProps: { label: t('common.vazgec'), severity: 'secondary', outlined: true, size: 'small' },
    acceptProps: { label: t('kitaplik.geriAl'), severity: 'success', size: 'small' },
    accept: async () => {
      try {
        await kitaplikAPI.geriAl(kayit.id)
        toastBildirim.basarili(t('kitaplik.geriAlindi'))
        await yukle()
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || t('kitaplik.geriAlHatasi'))
      }
    },
    reject: () => {}
  })
}

const ac = (kayit) => {
  const url = guvenliUrl(kayit.url)
  if (url) window.open(url, '_blank', 'noopener')
}

// Guvenlik: yalnizca http/https adreslerine izin ver (javascript:/data: engellenir).
const guvenliUrl = (url) => {
  if (!url || typeof url !== 'string') return null
  const t = url.trim()
  return /^https?:\/\//i.test(t) ? t : null
}

const snippetOnizleme = (icerik) => {
  if (!icerik) return '-'
  const tek = icerik.replace(/\s+/g, ' ').trim()
  return tek.length > 80 ? tek.slice(0, 80) + '…' : tek
}

const kategoriEtiketi = (kod) => {
  const bulunan = kategoriSecenekleri.value.find((k) => k.value === kod)
  return bulunan ? bulunan.label : kod
}

const kapsamEtiketi = (k) => {
  if (k.kapsam === 'GLOBAL') {
    return k.gorunurluk === 'TUMU' ? t('kitaplik.kapsamSirketTumu') : t('kitaplik.kapsamSirketSahis')
  }
  return t('kitaplik.kapsamKisisel')
}

onMounted(yukle)
onUnmounted(() => clearTimeout(aramaZamanlayici))
</script>

<style scoped>
.kitaplik-sayfasi {
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
.kitaplik-filtre {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
.sekme-grubu {
  display: flex;
  gap: 8px;
}
.filtre-alani {
  min-width: 150px;
}
.arama-kutu {
  min-width: 220px;
}
.kitaplik-icerik {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 14px;
}
.satir-icerik {
  display: flex;
  align-items: center;
  gap: 6px;
}
.baglanti {
  color: var(--primary-color, #14b8a6);
  text-decoration: none;
  word-break: break-all;
}
.baglanti:hover {
  text-decoration: underline;
}
.snippet-onizleme {
  font-family: monospace;
  font-size: 12px;
}
.islem-butonlari {
  display: flex;
  gap: 2px;
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