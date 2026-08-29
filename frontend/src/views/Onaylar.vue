<template>
  <div class="onaylar-sayfasi">
    <div class="sayfa-baslik flex justify-between items-center flex-wrap gap-3 mb-4">
      <div>
        <h1 class="page-title text-xl font-bold">
          <i
            class="pi pi-check-circle text-primary mr-2"
          />Yönetici & Muhasebe Onay Merkezi
        </h1>
        <p class="text-xs text-muted">
          Personel izinleri, saha masrafları ve satınalma taleplerini tek merkezden inceleyip onaylayın.
        </p>
      </div>
      <Button
        icon="pi pi-refresh"
        label="Yenile"
        class="p-button-outlined p-button-sm"
        :loading="yukleniyor"
        @click="yukle"
      />
    </div>

    <!-- PrimeVue TabView Sekmeleri -->
    <TabView>
      <TabPanel>
        <template #header>
          <span class="flex items-center gap-1.5">
            <i class="pi pi-calendar" />
            İzin Talepleri
            <span
              v-if="bekleyenIzinler && bekleyenIzinler.length > 0"
              class="badge-sayi bg-blue-600"
            >{{ bekleyenIzinler ? bekleyenIzinler.length : 0 }}</span>
          </span>
        </template>

        <div class="onay-icerik">
          <div
            v-if="bekleyenIzinler && bekleyenIzinler.length > 0"
            class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4"
          >
            <div
              v-for="i in bekleyenIzinler"
              :key="i.id"
              class="onay-kart p-4 rounded-xl border bg-white dark:bg-gray-800 shadow-sm flex flex-col justify-between"
            >
              <div>
                <div class="flex justify-between items-start mb-2">
                  <div class="flex items-center gap-2">
                    <div class="user-avatar bg-blue-100 dark:bg-blue-900/60 text-blue-700 dark:text-blue-300">
                      <i class="pi pi-user" />
                    </div>
                    <div>
                      <h4 class="font-bold text-sm text-gray-900 dark:text-gray-100">
                        {{ i.personelAdi || 'Personel #' + i.personelId }}
                      </h4>
                      <span class="text-xs text-muted">{{ i.izinTuru }}</span>
                    </div>
                  </div>
                  <Tag
                    value="Bekliyor"
                    severity="warn"
                  />
                </div>
                <div class="bg-gray-50 dark:bg-gray-700/50 p-2.5 rounded-lg text-xs space-y-1 mb-3">
                  <div class="flex justify-between">
                    <span>Tarih Aralığı:</span>
                    <strong>{{ formatDate(i.baslangic) }} → {{ formatDate(i.bitis) }}</strong>
                  </div>
                  <div class="flex justify-between text-primary font-semibold">
                    <span>İzin Süresi:</span>
                    <span>{{ i.gunSayisi }} Gün</span>
                  </div>
                  <div
                    v-if="i.aciklama"
                    class="pt-1 text-muted border-t mt-1"
                  >
                    {{ i.aciklama }}
                  </div>
                </div>
              </div>
              <div class="flex gap-2 pt-2 border-t">
                <Button
                  label="Onayla"
                  icon="pi pi-check"
                  class="p-button-success p-button-sm flex-1"
                  @click="izinOnay(i, 'ONAYLANDI')"
                />
                <Button
                  label="Reddet"
                  icon="pi pi-times"
                  class="p-button-danger p-button-outlined p-button-sm flex-1"
                  @click="izinOnay(i, 'REDDEDILDI')"
                />
              </div>
            </div>
          </div>
          <div
            v-else
            class="text-center py-12 text-muted"
          >
            <i class="pi pi-calendar text-4xl text-gray-400 block mb-2" />
            Şu anda bekleyen izin talebi bulunmuyor.
          </div>
        </div>
      </TabPanel>

      <TabPanel>
        <template #header>
          <span class="flex items-center gap-1.5">
            <i class="pi pi-wallet" />
            Saha Masraf & Avans
            <span
              v-if="bekleyenMasraflar && bekleyenMasraflar.length > 0"
              class="badge-sayi bg-emerald-600"
            >{{ bekleyenMasraflar ? bekleyenMasraflar.length : 0 }}</span>
          </span>
        </template>

        <div class="onay-icerik">
          <div
            v-if="bekleyenMasraflar && bekleyenMasraflar.length > 0"
            class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4"
          >
            <div
              v-for="m in bekleyenMasraflar"
              :key="m.id"
              class="onay-kart p-4 rounded-xl border bg-white dark:bg-gray-800 shadow-sm flex flex-col justify-between"
            >
              <div>
                <div class="flex justify-between items-start mb-2">
                  <div class="flex items-center gap-2">
                    <div class="user-avatar bg-emerald-100 dark:bg-emerald-900/60 text-emerald-700 dark:text-emerald-300">
                      <i class="pi pi-receipt" />
                    </div>
                    <div>
                      <h4 class="font-bold text-sm text-gray-900 dark:text-gray-100">
                        {{ m.personelAdi || m.kullaniciAdi || 'Saha Personeli' }}
                      </h4>
                      <span class="text-xs px-2 py-0.5 rounded bg-gray-100 dark:bg-gray-700 font-semibold">{{ m.tur === 'AVANS' ? 'Avans' : m.kategori }}</span>
                    </div>
                  </div>
                  <div class="text-right font-extrabold text-base text-emerald-600">
                    {{ formatCurrency(m.tutar) }} {{ m.paraBirimi || 'TRY' }}
                  </div>
                </div>
                <div class="bg-gray-50 dark:bg-gray-700/50 p-2.5 rounded-lg text-xs space-y-1 mb-3">
                  <div class="flex justify-between text-muted">
                    <span>Fiş / Talep Tarihi:</span>
                    <strong>{{ formatDate(m.tarih) }}</strong>
                  </div>
                  <div class="text-gray-800 dark:text-gray-200">
                    {{ m.aciklama }}
                  </div>
                </div>
              </div>
              <div class="flex gap-2 pt-2 border-t">
                <Button
                  label="Onayla (Gidere İşle)"
                  icon="pi pi-check"
                  class="p-button-success p-button-sm flex-1"
                  @click="masrafOnayla(m)"
                />
                <Button
                  label="Reddet"
                  icon="pi pi-times"
                  class="p-button-danger p-button-outlined p-button-sm flex-1"
                  @click="masrafReddet(m)"
                />
              </div>
            </div>
          </div>
          <div
            v-else
            class="text-center py-12 text-muted"
          >
            <i class="pi pi-wallet text-4xl text-gray-400 block mb-2" />
            Şu anda onay bekleyen saha masrafı veya avans talebi bulunmuyor.
          </div>
        </div>
      </TabPanel>

      <TabPanel>
        <template #header>
          <span class="flex items-center gap-1.5">
            <i class="pi pi-shopping-bag" />
            Satın Alma Talepleri
            <span
              v-if="bekleyenTalepler && bekleyenTalepler.length > 0"
              class="badge-sayi bg-purple-600"
            >{{ bekleyenTalepler ? bekleyenTalepler.length : 0 }}</span>
          </span>
        </template>

        <div class="onay-icerik">
          <div
            v-if="bekleyenTalepler && bekleyenTalepler.length > 0"
            class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4"
          >
            <div
              v-for="t in bekleyenTalepler"
              :key="t.id"
              class="onay-kart p-4 rounded-xl border bg-white dark:bg-gray-800 shadow-sm flex flex-col justify-between"
            >
              <div>
                <div class="flex justify-between items-start mb-2">
                  <span class="font-bold text-sm text-primary">#{{ t.talepNo }}</span>
                  <Tag
                    value="Onay Bekliyor"
                    severity="warning"
                  />
                </div>
                <h4 class="font-bold text-sm text-gray-900 dark:text-gray-100 mb-1">
                  {{ t.talepEden || '-' }} · {{ t.departman || 'Genel' }}
                </h4>
                <p class="text-xs text-muted mb-3">
                  {{ t.aciklama }}
                </p>
              </div>
              <div class="flex gap-2 pt-2 border-t">
                <Button
                  label="Onayla"
                  icon="pi pi-check"
                  class="p-button-success p-button-sm flex-1"
                  @click="talepOnay(t, 'ONAYLANDI')"
                />
                <Button
                  label="Reddet"
                  icon="pi pi-times"
                  class="p-button-danger p-button-outlined p-button-sm flex-1"
                  @click="talepOnay(t, 'REDDEDILDI')"
                />
              </div>
            </div>
          </div>
          <div
            v-else
            class="text-center py-12 text-muted"
          >
            <i class="pi pi-shopping-bag text-4xl text-gray-400 block mb-2" />
            Şu anda bekleyen satın alma talebi bulunmuyor.
          </div>
        </div>
      </TabPanel>

      <TabPanel>
        <template #header>
          <span class="flex items-center gap-1.5">
            <i class="pi pi-receipt" />
            Saha Siparişleri
            <span
              v-if="bekleyenSiparisler && bekleyenSiparisler.length > 0"
              class="badge-sayi bg-blue-600"
            >{{ bekleyenSiparisler ? bekleyenSiparisler.length : 0 }}</span>
          </span>
        </template>

        <div class="onay-icerik">
          <div
            v-if="bekleyenSiparisler && bekleyenSiparisler.length > 0"
            class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4"
          >
            <div
              v-for="s in bekleyenSiparisler"
              :key="s.id"
              class="onay-kart p-4 rounded-xl border bg-white dark:bg-gray-800 shadow-sm flex flex-col justify-between"
            >
              <div>
                <div class="flex justify-between items-start mb-2">
                  <span class="font-bold text-sm text-primary">#{{ s.siparisNo || s.id }}</span>
                  <Tag
                    value="Onay Bekliyor"
                    severity="warning"
                  />
                </div>
                <h4 class="font-bold text-sm text-gray-900 dark:text-gray-100 mb-1">
                  {{ s.cariHesapAd || s.musteriAd || s.cariHesapAdi || 'Müşteri' }}
                </h4>
                <p class="text-xs text-muted mb-1">
                  {{ s.aciklama || 'Saha Siparişi' }}
                </p>
                <p class="text-xs font-semibold text-primary mb-3">
                  {{ formatCurrency(s.genelToplam || s.toplamTutar || 0) }}
                </p>
              </div>
              <div class="flex gap-2 pt-2 border-t">
                <Button
                  label="Onayla"
                  icon="pi pi-check"
                  class="p-button-success p-button-sm flex-1"
                  @click="siparisOnay(s, 'HAZIRLANIYOR')"
                />
                <Button
                  label="Reddet"
                  icon="pi pi-times"
                  class="p-button-danger p-button-outlined p-button-sm flex-1"
                  @click="siparisOnay(s, 'IPTAL')"
                />
              </div>
            </div>
          </div>
          <div
            v-else
            class="text-center py-12 text-muted"
          >
            <i class="pi pi-receipt text-4xl text-gray-400 block mb-2" />
            Şu anda onay bekleyen saha siparişi bulunmuyor.
          </div>
        </div>
      </TabPanel>
    </TabView>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { personelIzinAPI, personelMasrafTalepAPI, satinalmaTalepAPI, siparisAPI } from '../api/index.js'
import { useAuthStore } from '../stores/authStore.js'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { formatCurrency, formatDate } from '../utils/format.js'

const authStore = useAuthStore()
const toastBildirim = useToastBildirim()

const yukleniyor = ref(false)
const bekleyenIzinler = ref([])
const bekleyenMasraflar = ref([])
const bekleyenTalepler = ref([])
const bekleyenSiparisler = ref([])

const yukle = async () => {
  yukleniyor.value = true
  try {
    const [iRes, mRes, tRes, sRes] = await Promise.allSettled([
      personelIzinAPI.getAll(),
      personelMasrafTalepAPI.getBekleyenler(),
      satinalmaTalepAPI.getAll(),
      siparisAPI.getAll({ size: 100 })
    ])
    if (iRes.status === 'fulfilled') {
      const allIzin = iRes.value.data?.content || iRes.value.data || []
      bekleyenIzinler.value = allIzin.filter((i) => i.durum === 'BEKLEMEDE')
    }
    if (mRes.status === 'fulfilled') {
      bekleyenMasraflar.value = mRes.value.data || []
    }
    if (tRes.status === 'fulfilled') {
      const allTalep = tRes.value.data?.content || tRes.value.data || []
      bekleyenTalepler.value = allTalep.filter((t) => t.durum === 'TASLAK')
    }
    if (sRes.status === 'fulfilled') {
      const allSiparis = sRes.value.data?.content || sRes.value.data || []
      bekleyenSiparisler.value = allSiparis.filter((s) => s.durum === 'BEKLIYOR')
    }
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || 'Onaylar yüklenemedi')
  } finally {
    yukleniyor.value = false
  }
}

const izinOnay = async (i, durum) => {
  try {
    await personelIzinAPI.durumGuncelle(i.id, durum, authStore?.kullanici?.displayName || authStore?.kullanici?.username)
    toastBildirim.basarili(`İzin ${durum === 'ONAYLANDI' ? 'onaylandı' : 'reddedildi'}`)
    await yukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || 'İşlem başarısız')
  }
}

const masrafOnayla = async (m) => {
  try {
    await personelMasrafTalepAPI.onayla(m.id, 'Muhasebe tarafından onaylandı')
    toastBildirim.basarili('Masraf talebi onaylandı ve şirket giderlerine işlendi')
    await yukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || 'İşlem başarısız')
  }
}

const masrafReddet = async (m) => {
  const not = prompt('Ret gerekçesi:', 'Belge veya fiş yetersiz')
  if (not === null) return
  try {
    await personelMasrafTalepAPI.reddet(m.id, not)
    toastBildirim.basarili('Masraf talebi reddedildi')
    await yukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || 'İşlem başarısız')
  }
}

const talepOnay = async (t, durum) => {
  try {
    await satinalmaTalepAPI.durumGuncelle(t.id, durum)
    toastBildirim.basarili(`Satınalma talebi ${durum === 'ONAYLANDI' ? 'onaylandı' : 'reddedildi'}`)
    await yukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || 'İşlem başarısız')
  }
}

const siparisOnay = async (s, durum) => {
  try {
    await siparisAPI.durumGuncelle(s.id, durum)
    toastBildirim.basarili(durum === 'HAZIRLANIYOR' ? 'Saha siparişi onaylandı' : 'Saha siparişi reddedildi')
    await yukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || 'İşlem başarısız')
  }
}

onMounted(yukle)
</script>

<style scoped>
.onaylar-sayfasi {
  padding: 1rem;
}

.badge-sayi {
  color: #fff;
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 10px;
  margin-left: 6px;
}

.user-avatar {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
}

.onay-kart {
  min-height: 190px;
}
</style>
