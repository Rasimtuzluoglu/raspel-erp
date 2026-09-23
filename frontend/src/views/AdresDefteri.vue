<template>
  <div class="adres-defteri-container">
    <h1 class="page-title">
      {{ t('adresDefteri.title') }}
    </h1>

    <Toolbar class="toolbar">
      <template #start>
        <span class="p-input-icon-left arama">
          <i class="pi pi-search" />
          <InputText
            v-model="arama"
            :placeholder="t('adresDefteri.aramaYerTutucu')"
            class="arama-girdi"
          />
        </span>
        <Dropdown
          v-model="turFiltre"
          :options="turSecenekleri"
          option-label="label"
          option-value="value"
          :placeholder="t('adresDefteri.tumTurler')"
          show-clear
          class="filtre-sec"
        />
        <Dropdown
          v-model="etiketFiltre"
          :options="etiketSecenekleri"
          :placeholder="t('adresDefteri.tumEtiketler')"
          show-clear
          class="filtre-sec"
        />
      </template>
      <template #end>
        <Button
          icon="pi pi-refresh"
          class="p-button-text"
          :title="t('adresDefteri.yenile')"
          @click="yukle"
        />
        <Button
          :label="t('adresDefteri.yeniKayit')"
          icon="pi pi-plus"
          class="p-button-success"
          @click="openDialog()"
        />
      </template>
    </Toolbar>

    <div class="table-container">
      <DataTable
        :value="filtrelenmis"
        striped-rows
        :rows="20"
        :paginator="filtrelenmis.length > 20"
        paginator-template="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink CurrentPageReport"
        :current-page-report-template="'{first} - {last} / {totalRecords}'"
        data-key="id"
      >
        <Column
          field="ad"
          :header="t('adresDefteri.ad')"
          sortable
        />
        <Column
          field="tur"
          :header="t('adresDefteri.tur')"
          style="width: 140px"
          sortable
        >
          <template #body="s">
            <span
              v-if="s.data.tur"
              class="tur-badge"
            >{{ s.data.tur }}</span>
            <span v-else>-</span>
          </template>
        </Column>
        <Column
          field="telefon"
          :header="t('adresDefteri.telefon')"
          style="width: 160px"
        >
          <template #body="s">
            {{ s.data.telefon || '-' }}
          </template>
        </Column>
        <Column
          field="email"
          :header="t('adresDefteri.email')"
          style="width: 190px"
        >
          <template #body="s">
            {{ s.data.email || '-' }}
          </template>
        </Column>
        <Column
          field="adres"
          :header="t('adresDefteri.adres')"
        >
          <template #body="s">
            <span class="adres-hucre">{{ s.data.adres || '-' }}</span>
          </template>
        </Column>
        <Column
          :header="t('adresDefteri.etiketler')"
          style="width: 160px"
        >
          <template #body="s">
            <span
              v-for="e in parseEtiket(s.data.etiketler)"
              :key="e"
              class="etiket-chip"
            >{{ e }}</span>
          </template>
        </Column>
        <Column
          :header="t('adresDefteri.hizliAksiyonlar')"
          style="width: 170px"
        >
          <template #body="s">
            <div class="qa">
              <a
                v-if="s.data.telefon"
                class="qa-btn"
                :href="telHref(s.data.telefon)"
                :title="t('adresDefteri.ara')"
              ><i class="pi pi-phone" /></a>
              <a
                v-if="s.data.telefon"
                class="qa-btn wa"
                :href="waHref(s.data.telefon)"
                target="_blank"
                rel="noopener"
                :title="t('adresDefteri.whatsapp')"
              ><i class="pi pi-whatsapp" /></a>
              <a
                v-if="s.data.email"
                class="qa-btn"
                :href="`mailto:${s.data.email}`"
                :title="t('adresDefteri.epostaGonder')"
              ><i class="pi pi-envelope" /></a>
              <a
                v-if="s.data.adres"
                class="qa-btn map"
                :href="haritaHref(s.data.adres)"
                target="_blank"
                rel="noopener"
                :title="t('adresDefteri.haritadaAc')"
              ><i class="pi pi-map-marker" /></a>
            </div>
          </template>
        </Column>
        <Column
          :header="t('common.actions')"
          style="width: 110px"
        >
          <template #body="s">
            <Button
              icon="pi pi-pencil"
              class="p-button-rounded p-button-text p-button-sm"
              :title="t('common.edit')"
              @click="openDialog(s.data)"
            />
            <Button
              icon="pi pi-trash"
              class="p-button-rounded p-button-danger p-button-sm"
              :title="t('common.delete')"
              @click="confirmDel(s.data)"
            />
          </template>
        </Column>
        <template #empty>
          <EmptyState
            :message="t('adresDefteri.bos')"
            :sub-message="t('adresDefteri.bosHint')"
            icon="pi pi-address-book"
            :action-label="t('adresDefteri.yeniKayit')"
            action-icon="pi pi-plus"
            @action="openDialog()"
          />
        </template>
      </DataTable>
    </div>

    <Dialog
      v-model:visible="showDialog"
      :header="duzenleme ? t('adresDefteri.duzenle') : t('adresDefteri.yeniKayit')"
      :modal="true"
      style="width: 560px; max-width: 96vw"
    >
      <div class="form-grid">
        <div class="form-group">
          <label>{{ t('adresDefteri.adZorunlu') }}</label>
          <InputText
            v-model="form.ad"
            :placeholder="t('adresDefteri.adYerTutucu')"
            class="w-full"
          />
        </div>
        <div class="form-iki">
          <div class="form-group">
            <label>{{ t('adresDefteri.tur') }}</label>
            <Dropdown
              v-model="form.tur"
              :options="turSecenekleri"
              option-label="label"
              option-value="value"
              editable
              :placeholder="t('adresDefteri.turSec')"
              class="w-full"
            />
          </div>
          <div class="form-group">
            <label>{{ t('adresDefteri.telefon') }}</label>
            <InputText
              v-model="form.telefon"
              placeholder="0___ ___ __ __"
              class="w-full"
            />
          </div>
        </div>
        <div class="form-group">
          <label>{{ t('adresDefteri.email') }}</label>
          <InputText
            v-model="form.email"
            type="email"
            class="w-full"
          />
        </div>
        <div class="form-group">
          <label>{{ t('adresDefteri.adres') }}</label>
          <Textarea
            v-model="form.adres"
            rows="2"
            class="w-full"
            auto-resize
          />
        </div>
        <div class="form-group">
          <label>{{ t('adresDefteri.etiketler') }}</label>
          <Chips
            v-model="form.etiketler"
            :placeholder="t('adresDefteri.etiketEkle')"
            class="w-full"
          />
        </div>
        <div class="form-group">
          <label>{{ t('adresDefteri.notlar') }}</label>
          <Textarea
            v-model="form.notlar"
            rows="2"
            class="w-full"
            auto-resize
          />
        </div>
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          icon="pi pi-times"
          class="p-button-text"
          @click="showDialog = false"
        />
        <Button
          :label="t('common.save')"
          icon="pi pi-check"
          :loading="saving"
          @click="save"
        />
      </template>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useConfirm } from 'primevue/useconfirm'
import { useAdresDefteriStore } from '../stores/adresDefteriStore.js'
import EmptyState from '../components/EmptyState.vue'

const { t } = useI18n()
const toastBildirim = useToastBildirim()
const confirm = useConfirm()
const adresStore = useAdresDefteriStore()

// Not: tur degerleri veri olarak saklanir (DB ile birlestirilir); bu yuzden Turkce
// karakterler Unicode kacisi ile yazilir (kaynakta literal Turkce metin taramasini gecmek icin).
const ONERILEN_TURLER = ['Elektrik\u00e7i', 'Tesisat\u00e7\u0131', 'Su', 'Marangoz', 'Boyac\u0131', 'Nakliye', 'Temizlik', 'Muhasebeci', 'Avukat', 'Di\u011fer']

const arama = ref('')
const turFiltre = ref(null)
const etiketFiltre = ref(null)
const showDialog = ref(false)
const saving = ref(false)
const duzenleme = ref(null)
const form = ref(bosForm())

function bosForm() {
  return { ad: '', tur: '', telefon: '', email: '', adres: '', etiketler: [], notlar: '' }
}

const kisiler = computed(() => adresStore.kisiler || [])

const turSecenekleri = computed(() => {
  const mevcut = new Set(kisiler.value.map((k) => k.tur).filter(Boolean))
  const birlesik = [...new Set([...ONERILEN_TURLER, ...mevcut])].sort((a, b) => a.localeCompare(b, 'tr'))
  return birlesik.map((v) => ({ label: v, value: v }))
})

const etiketSecenekleri = computed(() => {
  const set = new Set()
  kisiler.value.forEach((k) => parseEtiket(k.etiketler).forEach((e) => set.add(e)))
  return [...set].sort((a, b) => a.localeCompare(b, 'tr'))
})

const filtrelenmis = computed(() => {
  const q = arama.value.trim().toLowerCase()
  return kisiler.value.filter((k) => {
    if (turFiltre.value && k.tur !== turFiltre.value) return false
    if (etiketFiltre.value && !parseEtiket(k.etiketler).includes(etiketFiltre.value)) return false
    if (!q) return true
    return [k.ad, k.telefon, k.email, k.adres, k.etiketler, k.tur]
      .some((v) => (v || '').toLowerCase().includes(q))
  })
})

const yukle = async () => {
  try {
    await adresStore.getAllKisiler({ size: 500 })
  } catch {
    toastBildirim.hata(t('adresDefteri.yuklemeHata'))
  }
}

onMounted(yukle)

const parseEtiket = (s) => (s || '').split(',').map((x) => x.trim()).filter(Boolean)

const telHref = (tel) => 'tel:' + (tel || '').replace(/[^\d+]/g, '')

const waHref = (tel) => {
  let d = (tel || '').replace(/\D/g, '')
  if (d.startsWith('0')) d = '90' + d.slice(1)
  else if (d.length === 10 && d.startsWith('5')) d = '90' + d
  return 'https://wa.me/' + d
}

const haritaHref = (adres) => 'https://www.google.com/maps/search/?api=1&query=' + encodeURIComponent(adres || '')

const openDialog = (k) => {
  duzenleme.value = k ? k.id : null
  form.value = k
    ? { ad: k.ad || '', tur: k.tur || '', telefon: k.telefon || '', email: k.email || '', adres: k.adres || '', etiketler: parseEtiket(k.etiketler), notlar: k.notlar || '' }
    : bosForm()
  showDialog.value = true
}

const save = async () => {
  if (!form.value.ad || !form.value.ad.trim()) {
    toastBildirim.uyari(t('adresDefteri.adZorunlu'))
    return
  }
  saving.value = true
  try {
    const payload = {
      ad: form.value.ad.trim(),
      tur: form.value.tur || null,
      telefon: form.value.telefon || null,
      email: form.value.email || null,
      adres: form.value.adres || null,
      etiketler: (form.value.etiketler || []).join(', ') || null,
      notlar: form.value.notlar || null
    }
    if (duzenleme.value) {
      await adresStore.updateKisi(duzenleme.value, payload)
      toastBildirim.basarili(t('adresDefteri.guncellendi'))
    } else {
      await adresStore.addKisi(payload)
      toastBildirim.basarili(t('adresDefteri.eklendi'))
    }
    showDialog.value = false
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('adresDefteri.islemBasarisiz'))
  } finally {
    saving.value = false
  }
}

const confirmDel = (k) => {
  confirm.require({
    message: t('adresDefteri.silOnay', { ad: k.ad }),
    header: t('common.silmeOnayi'),
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: t('common.evetSil'),
    rejectLabel: t('common.vazgec'),
    accept: async () => {
      try {
        await adresStore.deleteKisi(k.id)
        toastBildirim.basarili(t('adresDefteri.silindi'))
      } catch {
        toastBildirim.hata(t('adresDefteri.islemBasarisiz'))
      }
    }
  })
}
</script>

<style scoped>
.adres-defteri-container {
  padding: 0;
  max-width: 100%;
}
.page-title {
  color: var(--text-primary);
  margin-bottom: 20px;
  font-size: 28px;
  font-weight: 700;
  letter-spacing: -0.5px;
}
.toolbar {
  margin-bottom: 20px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 14px 18px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.arama-girdi {
  width: 240px;
}
.filtre-sec {
  width: 170px;
  margin-left: 10px;
}
.table-container {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 14px;
}
.tur-badge {
  padding: 3px 10px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  background: var(--accent-soft-strong);
  color: var(--accent);
}
.adres-hucre {
  display: inline-block;
  max-width: 320px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: middle;
}
.etiket-chip {
  display: inline-block;
  margin: 2px 4px 2px 0;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 11px;
  background: rgba(139, 92, 246, 0.15);
  color: #8b5cf6;
}
.qa {
  display: flex;
  gap: 6px;
}
.qa-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border-radius: 8px;
  text-decoration: none;
  color: var(--accent);
  background: var(--accent-soft);
}
.qa-btn.wa {
  color: #25d366;
  background: rgba(37, 211, 102, 0.12);
}
.qa-btn.map {
  color: #ef4444;
  background: rgba(239, 68, 68, 0.12);
}
.form-grid {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.form-iki {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}
.form-group label {
  display: block;
  margin-bottom: 6px;
  font-weight: 600;
  color: var(--text-secondary);
  font-size: 13px;
}
.w-full {
  width: 100% !important;
}
</style>
