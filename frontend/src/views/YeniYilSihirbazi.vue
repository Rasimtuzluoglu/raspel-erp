<template>
  <div class="yeni-yil-sihirbazi p-4">
    <div class="flex justify-content-between align-items-center mb-4">
      <h1 class="text-2xl font-bold m-0">
        Yeni Yıl / Şirket Açılış Sihirbazı
      </h1>
    </div>

    <div class="card">
      <Steps
        :model="adımlar"
        :readonly="false"
        :active-step="aktifAdim"
      />

      <div class="mt-5 p-4 border-1 surface-border border-round">
        <!-- Adım 1: Temel Bilgiler -->
        <div
          v-if="aktifAdim === 0"
          class="step-content"
        >
          <h3 class="mb-4">
            1. Yeni Şirket Bilgileri
          </h3>
          <div class="grid formgrid p-fluid">
            <div class="field col-12 md:col-6">
              <label for="form-ad">Şirket Adı *</label>
              <InputText
                id="form-ad"
                v-model="form.ad"
                placeholder="Örn: RasPel 2026 Resmi"
              />
            </div>
            <div class="field col-12 md:col-3">
              <label for="form-yil">Mali Yıl *</label>
              <InputNumber
                id="form-yil"
                v-model="form.yil"
                :use-grouping="false"
              />
            </div>
            <div class="field col-12 md:col-3">
              <label for="form-tur">Şirket Türü</label>
              <Dropdown
                id="form-tur"
                v-model="form.tur"
                :options="[{label:'Resmi', value:'RESMI'}, {label:'Gayriresmi', value:'GAYRIRESMI'}, {label:'Diğer', value:'DIGER'}]"
                option-label="label"
                option-value="value"
              />
            </div>
            <div class="field col-12 md:col-6">
              <label for="form-parent">Ana Şirket (İsteğe Bağlı)</label>
              <Dropdown
                id="form-parent"
                v-model="form.parentId"
                :options="sirketler"
                option-label="ad"
                option-value="id"
                show-clear
                placeholder="Bu şirket hangi ana şirkete/müşteriye bağlı?"
              />
            </div>
            <div class="field col-12 md:col-6">
              <label for="form-vkn">Vergi Numarası</label>
              <InputText
                id="form-vkn"
                v-model="form.vergiNo"
              />
            </div>
          </div>
        </div>

        <!-- Adım 2: Veri Aktarım Seçenekleri -->
        <div
          v-else-if="aktifAdim === 1"
          class="step-content"
        >
          <h3 class="mb-4">
            2. Veri Aktarımı
          </h3>
          <div class="mb-4">
            <label
              for="kaynak-sirket"
              class="font-bold block mb-2"
            >Verilerin Çekileceği Kaynak Şirket</label>
            <Dropdown
              id="kaynak-sirket"
              v-model="aktarim.kaynakSirketId"
              :options="sirketler"
              option-label="ad"
              option-value="id"
              show-clear
              placeholder="Kaynak şirketi seçin (Veri aktarılmayacaksa boş bırakın)"
              class="w-full md:w-6"
            />
          </div>

          <div
            v-if="aktarim.kaynakSirketId"
            class="aktarim-secenekleri mt-4"
          >
            <div class="flex align-items-center mb-3">
              <Checkbox
                v-model="aktarim.stoklariAktar"
                :binary="true"
                input-id="stokAktar"
              />
              <label
                for="stokAktar"
                class="ml-2 font-semibold"
              >Stok Kartlarını Aktar</label>
            </div>
            <div class="ml-4 mb-4 text-color-secondary text-sm">
              <div class="flex align-items-center mb-2">
                <Checkbox
                  v-model="aktarim.fiyatlariKoru"
                  :binary="true"
                  input-id="fiyatKoru"
                  :disabled="!aktarim.stoklariAktar"
                />
                <label
                  for="fiyatKoru"
                  class="ml-2"
                >Stok satış ve alış fiyatlarını koru (Seçilmezse fiyatlar 0 olur)</label>
              </div>
              <i class="pi pi-info-circle mr-1" /> Stok miktarları yeni şirkette 0 olarak başlatılacaktır.
            </div>

            <div class="flex align-items-center mb-3">
              <Checkbox
                v-model="aktarim.carileriAktar"
                :binary="true"
                input-id="cariAktar"
              />
              <label
                for="cariAktar"
                class="ml-2 font-semibold"
              >Cari Hesapları Aktar</label>
            </div>
            <div class="ml-4 mb-4 text-color-secondary text-sm">
              <div class="flex align-items-center mb-2">
                <Checkbox
                  v-model="aktarim.bakiyeleriSifirla"
                  :binary="true"
                  input-id="bakiyeSifirla"
                  :disabled="!aktarim.carileriAktar"
                />
                <label
                  for="bakiyeSifirla"
                  class="ml-2"
                >Cari bakiyeleri sıfırla (Seçilmezse mevcut bakiyeler devir olarak aktarılır)</label>
              </div>
            </div>
          </div>
          <div
            v-else
            class="p-message p-message-info"
          >
            <div class="p-message-wrapper">
              <span class="p-message-icon pi pi-info-circle" />
              <div class="p-message-text">
                Kaynak şirket seçilmediği için yeni şirket tamamen boş oluşturulacaktır.
              </div>
            </div>
          </div>
        </div>

        <!-- Adım 3: Onay ve Özet -->
        <div
          v-else-if="aktifAdim === 2"
          class="step-content"
        >
          <h3 class="mb-4">
            3. Özet ve Onay
          </h3>
          <div class="surface-100 p-4 border-round mb-4">
            <h4 class="mt-0 mb-3 border-bottom-1 border-300 pb-2">
              Oluşturulacak Şirket
            </h4>
            <div class="grid">
              <div class="col-4 font-semibold">
                Şirket Adı:
              </div><div class="col-8">
                {{ form.ad }}
              </div>
              <div class="col-4 font-semibold">
                Tür / Yıl:
              </div><div class="col-8">
                {{ form.tur }} / {{ form.yil }}
              </div>
              <div class="col-4 font-semibold">
                Ana Şirket:
              </div><div class="col-8">
                {{ seciliAnaSirketAdi || '-' }}
              </div>
            </div>

            <h4 class="mt-4 mb-3 border-bottom-1 border-300 pb-2">
              Veri Aktarımı
            </h4>
            <div
              v-if="aktarim.kaynakSirketId"
              class="grid"
            >
              <div class="col-4 font-semibold">
                Kaynak Şirket:
              </div><div class="col-8">
                {{ seciliKaynakSirketAdi }}
              </div>
              <div class="col-4 font-semibold">
                Stok Kartları:
              </div>
              <div class="col-8">
                <span
                  v-if="aktarim.stoklariAktar"
                  class="text-green-600 font-bold"
                >Evet (Fiyatlar: {{ aktarim.fiyatlariKoru ? 'Korunacak' : 'Sıfırlanacak' }})</span>
                <span
                  v-else
                  class="text-red-500"
                >Hayır</span>
              </div>
              <div class="col-4 font-semibold">
                Cari Hesaplar:
              </div>
              <div class="col-8">
                <span
                  v-if="aktarim.carileriAktar"
                  class="text-green-600 font-bold"
                >Evet (Bakiye: {{ aktarim.bakiyeleriSifirla ? 'Sıfırlanacak' : 'Devredecek' }})</span>
                <span
                  v-else
                  class="text-red-500"
                >Hayır</span>
              </div>
            </div>
            <div
              v-else
              class="text-color-secondary font-italic"
            >
              Veri aktarımı yapılmayacak.
            </div>
          </div>

          <Message
            v-if="aktarim.kaynakSirketId && (aktarim.stoklariAktar || aktarim.carileriAktar)"
            severity="warn"
            :closable="false"
          >
            <i class="pi pi-exclamation-triangle mr-2" />
            Aktarım işlemi veritabanı boyutuna göre birkaç saniye sürebilir. Lütfen işlem bitene kadar sayfadan ayrılmayın.
          </Message>
        </div>
      </div>

      <div class="flex justify-content-between mt-4">
        <Button
          label="Geri"
          icon="pi pi-angle-left"
          :disabled="aktifAdim === 0"
          class="p-button-outlined"
          @click="geri"
        />
        <Button
          v-if="aktifAdim < 2"
          label="İleri"
          icon="pi pi-angle-right"
          icon-pos="right"
          :disabled="!ileriGidebilir"
          @click="ileri"
        />
        <Button
          v-else
          label="Şirketi Oluştur ve Aktarımı Başlat"
          icon="pi pi-check"
          icon-pos="right"
          severity="success"
          :loading="islemYapiliyor"
          @click="tamamla"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { sirketAPI, apiClient } from '../api/index.js'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useAuthStore } from '../stores/authStore.js'

const router = useRouter()
const toast = useToastBildirim()
const authStore = useAuthStore()

const aktifAdim = ref(0)
const adımlar = ref([
  { label: 'Şirket Bilgileri' },
  { label: 'Veri Aktarımı' },
  { label: 'Onay' }
])

const sirketler = ref([])
const islemYapiliyor = ref(false)

const form = ref({
  ad: '',
  tur: 'RESMI',
  yil: new Date().getFullYear(),
  parentId: null,
  vergiNo: '',
  aktif: true
})

const aktarim = ref({
  kaynakSirketId: null,
  stoklariAktar: true,
  carileriAktar: true,
  bakiyeleriSifirla: true,
  fiyatlariKoru: true
})

const seciliAnaSirketAdi = computed(() => {
  if (!form.value.parentId) return ''
  const s = sirketler.value.find(x => x.id === form.value.parentId)
  return s ? s.ad : ''
})

const seciliKaynakSirketAdi = computed(() => {
  if (!aktarim.value.kaynakSirketId) return ''
  const s = sirketler.value.find(x => x.id === aktarim.value.kaynakSirketId)
  return s ? s.ad : ''
})

const ileriGidebilir = computed(() => {
  if (aktifAdim.value === 0) {
    return form.value.ad && form.value.ad.trim().length > 0 && form.value.yil
  }
  return true
})

onMounted(async () => {
  try {
    const res = await sirketAPI.getAll()
    sirketler.value = res.data?.content || res.data || []
    
    if (authStore.sirketId) {
      aktarim.value.kaynakSirketId = authStore.sirketId
    }
  } catch (err) {
    toast.hata('Şirketler yüklenemedi')
  }
})

const ileri = () => {
  if (aktifAdim.value < 2) aktifAdim.value++
}

const geri = () => {
  if (aktifAdim.value > 0) aktifAdim.value--
}

const tamamla = async () => {
  islemYapiliyor.value = true
  try {
    const sirketRes = await sirketAPI.create(form.value)
    const yeniSirketId = sirketRes.data.id

    if (aktarim.value.kaynakSirketId && (aktarim.value.stoklariAktar || aktarim.value.carileriAktar)) {
      const payload = {
        kaynakSirketId: aktarim.value.kaynakSirketId,
        hedefSirketId: yeniSirketId,
        stoklariAktar: aktarim.value.stoklariAktar,
        carileriAktar: aktarim.value.carileriAktar,
        bakiyeleriSifirla: aktarim.value.bakiyeleriSifirla,
        fiyatlariKoru: aktarim.value.fiyatlariKoru
      }
      
      const aktarimRes = await apiClient.post('/veri-aktarim/sirketler-arasi', payload)
      const sonuc = aktarimRes.data
      
      toast.basarili(`Şirket oluşturuldu ve veriler aktarıldı! ${sonuc.aktarilanStokSayisi} Stok, ${sonuc.aktarilanCariSayisi} Cari kopyalandı.`)
    } else {
      toast.basarili('Yeni şirket başarıyla oluşturuldu.')
    }

    setTimeout(() => {
      router.push('/sistem-durumu')
    }, 1500)

  } catch (err) {
    toast.hata(err.response?.data?.message || 'İşlem sırasında bir hata oluştu.')
  } finally {
    islemYapiliyor.value = false
  }
}
</script>

<style scoped>
.step-content {
  min-height: 350px;
}
</style>
