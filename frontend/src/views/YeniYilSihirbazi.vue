<template>
  <div class="yeni-yil-sihirbazi">
    <div class="sayfa-baslik">
      <h1 class="page-title">
        <i class="pi pi-sparkles" /> Yeni Yıl / Şirket Açılış Sihirbazı
      </h1>
      <p class="aciklama">
        Yeni mali yıl için şirket oluşturun ve isterseniz mevcut şirketinizden stok/cari verilerini aktarın.
      </p>
    </div>

    <div class="card">
      <Steps
        :model="adımlar"
        :readonly="false"
        :active-step="aktifAdim"
      />

      <div class="adim-icerik">
        <!-- Adım 1: Temel Bilgiler -->
        <div
          v-if="aktifAdim === 0"
          class="step-content"
        >
          <h3 class="adim-baslik">
            <i class="pi pi-building" /> 1. Yeni Şirket Bilgileri
          </h3>
          <div class="form-grid">
            <div class="field">
              <label for="form-ad">Şirket Adı *</label>
              <InputText
                id="form-ad"
                v-model="form.ad"
                placeholder="Örn: RasPel 2026 Resmi"
              />
            </div>
            <div class="field">
              <label for="form-yil">Mali Yıl *</label>
              <InputNumber
                id="form-yil"
                v-model="form.yil"
                :use-grouping="false"
              />
            </div>
            <div class="field">
              <label for="form-tur">Şirket Türü</label>
              <Dropdown
                id="form-tur"
                v-model="form.tur"
                :options="[{label:'Resmi', value:'RESMI'}, {label:'Gayriresmi', value:'GAYRIRESMI'}, {label:'Diğer', value:'DIGER'}]"
                option-label="label"
                option-value="value"
              />
            </div>
            <div class="field">
              <label for="form-vkn">Vergi Numarası</label>
              <InputText
                id="form-vkn"
                v-model="form.vergiNo"
              />
            </div>
            <div class="field full-width">
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
          </div>
        </div>

        <!-- Adım 2: Veri Aktarım Seçenekleri -->
        <div
          v-else-if="aktifAdim === 1"
          class="step-content"
        >
          <h3 class="adim-baslik">
            <i class="pi pi-database" /> 2. Veri Aktarımı
          </h3>
          <div class="field mb-4">
            <label for="kaynak-sirket">Verilerin Çekileceği Kaynak Şirket</label>
            <Dropdown
              id="kaynak-sirket"
              v-model="aktarim.kaynakSirketId"
              :options="sirketler"
              option-label="ad"
              option-value="id"
              show-clear
              placeholder="Kaynak şirketi seçin (Veri aktarılmayacaksa boş bırakın)"
            />
          </div>

          <div
            v-if="aktarim.kaynakSirketId"
            class="aktarim-secenekleri"
          >
            <div class="aktarim-kutu">
              <div class="aktarim-satir">
                <Checkbox
                  v-model="aktarim.stoklariAktar"
                  :binary="true"
                  input-id="stokAktar"
                />
                <label
                  for="stokAktar"
                  class="aktarim-etiket"
                >Stok Kartlarını Aktar</label>
              </div>
              <div class="aktarim-alt">
                <div class="aktarim-satir">
                  <Checkbox
                    v-model="aktarim.fiyatlariKoru"
                    :binary="true"
                    input-id="fiyatKoru"
                    :disabled="!aktarim.stoklariAktar"
                  />
                  <label
                    for="fiyatKoru"
                    class="aktarim-etiket"
                  >Stok satış ve alış fiyatlarını koru</label>
                </div>
                <small class="ipucu">
                  <i class="pi pi-info-circle" /> Fiyatlar korunmazsa 0 olur. Miktarlar yeni şirkette 0'dan başlar.
                </small>
              </div>
            </div>

            <div class="aktarim-kutu">
              <div class="aktarim-satir">
                <Checkbox
                  v-model="aktarim.carileriAktar"
                  :binary="true"
                  input-id="cariAktar"
                />
                <label
                  for="cariAktar"
                  class="aktarim-etiket"
                >Cari Hesapları Aktar</label>
              </div>
              <div class="aktarim-alt">
                <div class="aktarim-satir">
                  <Checkbox
                    v-model="aktarim.bakiyeleriSifirla"
                    :binary="true"
                    input-id="bakiyeSifirla"
                    :disabled="!aktarim.carileriAktar"
                  />
                  <label
                    for="bakiyeSifirla"
                    class="aktarim-etiket"
                  >Cari bakiyeleri sıfırla</label>
                </div>
                <small class="ipucu">
                  <i class="pi pi-info-circle" /> Sıfırlanmazsa mevcut bakiyeler devir olarak aktarılır.
                </small>
              </div>
            </div>
          </div>
          <div
            v-else
            class="bos-uyari"
          >
            <i class="pi pi-info-circle" />
            <span>Kaynak şirket seçilmediği için yeni şirket tamamen boş oluşturulacaktır.</span>
          </div>
        </div>

        <!-- Adım 3: Onay ve Özet -->
        <div
          v-else-if="aktifAdim === 2"
          class="step-content"
        >
          <h3 class="adim-baslik">
            <i class="pi pi-check-circle" /> 3. Özet ve Onay
          </h3>
          <div class="ozet-kutu">
            <h4 class="ozet-baslik">
              Oluşturulacak Şirket
            </h4>
            <div class="ozet-satir">
              <span>Şirket Adı</span>
              <strong>{{ form.ad }}</strong>
            </div>
            <div class="ozet-satir">
              <span>Tür / Yıl</span>
              <strong>{{ form.tur }} / {{ form.yil }}</strong>
            </div>
            <div class="ozet-satir">
              <span>Ana Şirket</span>
              <strong>{{ seciliAnaSirketAdi || '-' }}</strong>
            </div>

            <h4 class="ozet-baslik mt-4">
              Veri Aktarımı
            </h4>
            <div
              v-if="aktarim.kaynakSirketId"
            >
              <div class="ozet-satir">
                <span>Kaynak Şirket</span>
                <strong>{{ seciliKaynakSirketAdi }}</strong>
              </div>
              <div class="ozet-satir">
                <span>Stok Kartları</span>
                <strong
                  :class="aktarim.stoklariAktar ? 'pozitif' : 'negatif'"
                >{{ aktarim.stoklariAktar ? 'Evet' : 'Hayır' }}</strong>
              </div>
              <div class="ozet-satir">
                <span>Cari Hesaplar</span>
                <strong
                  :class="aktarim.carileriAktar ? 'pozitif' : 'negatif'"
                >{{ aktarim.carileriAktar ? 'Evet' : 'Hayır' }}</strong>
              </div>
            </div>
            <div
              v-else
              class="ipucu"
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

      <div class="adim-butonlar">
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
.yeni-yil-sihirbazi {
  padding: 0;
}
.sayfa-baslik {
  margin-bottom: 20px;
}
.sayfa-baslik .page-title {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0 0 6px;
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--text-primary);
}
.sayfa-baslik .page-title i {
  color: var(--accent);
}
.sayfa-baslik .aciklama {
  margin: 0;
  color: var(--text-muted);
  font-size: 13px;
}

.adim-icerik {
  margin-top: 20px;
  padding: 20px;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: var(--bg-card);
}
.step-content {
  min-height: 320px;
}
.adim-baslik {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 16px;
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
}
.adim-baslik i {
  color: var(--accent);
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
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
  text-transform: uppercase;
  letter-spacing: 0.4px;
}
.field.full-width {
  grid-column: 1 / -1;
}
.field :deep(.p-inputtext),
.field :deep(.p-inputnumber),
.field :deep(.p-dropdown) {
  width: 100%;
}

.aktarim-secenekleri {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.aktarim-kutu {
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 12px 14px;
}
.aktarim-satir {
  display: flex;
  align-items: center;
  gap: 8px;
}
.aktarim-etiket {
  font-size: 13.5px;
  font-weight: 600;
  color: var(--text-primary);
  cursor: pointer;
}
.aktarim-alt {
  margin-top: 8px;
  padding-left: 28px;
}
.ipucu {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 6px;
}
.bos-uyari {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 16px;
  background: rgba(59, 130, 246, 0.1);
  border: 1px solid rgba(59, 130, 246, 0.25);
  border-radius: 10px;
  font-size: 13px;
  color: var(--text-secondary);
}
.bos-uyari i {
  color: #60a5fa;
}

.ozet-kutu {
  background: var(--bg-secondary);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 18px;
  margin-bottom: 16px;
}
.ozet-baslik {
  margin: 0 0 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--border);
  font-size: 14px;
  font-weight: 700;
  color: var(--text-primary);
}
.ozet-satir {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 6px 0;
  font-size: 13.5px;
}
.ozet-satir span {
  color: var(--text-muted);
}
.ozet-satir strong {
  color: var(--text-primary);
  text-align: right;
}
.ozet-satir strong.pozitif {
  color: #10b981;
}
.ozet-satir strong.negatif {
  color: #ef4444;
}
.mt-4 {
  margin-top: 16px;
}

.adim-butonlar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  margin-top: 20px;
}

@media (max-width: 700px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
  .field.full-width {
    grid-column: auto;
  }
}
</style>
