<template>
  <div class="onaylar-sayfasi">
    <div class="sayfa-baslik flex justify-between items-center flex-wrap gap-3 mb-4">
      <div>
        <h1 class="page-title text-xl font-bold">
          <i
            class="pi pi-check-circle text-primary mr-2"
          />{{ t('onaylar.title') }}
        </h1>
        <p class="text-xs text-muted">
          {{ t('onaylar.subtitle') }}
        </p>
      </div>
      <Button
        icon="pi pi-refresh"
        :label="t('onaylar.yenile')"
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
            {{ t('onaylar.izinTalepleri') }}
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
                    <div class="user-avatar bg-blue-100 dark:bg-blue-900/60 text-accent dark:text-blue-300">
                      <i class="pi pi-user" />
                    </div>
                    <div>
                      <h4 class="font-bold text-sm text-primary dark:text-gray-100">
                        {{ i.personelAdi || `${t('onaylar.personel')} #${i.personelId}` }}
                      </h4>
                      <span class="text-xs text-muted">{{ i.izinTuru }}</span>
                    </div>
                  </div>
                  <Tag
                    :value="t('onaylar.bekliyor')"
                    severity="warn"
                  />
                </div>
                <div class="bg-secondary dark:bg-gray-700/50 p-2.5 rounded-lg text-xs space-y-1 mb-3">
                  <div class="flex justify-between">
                    <span>{{ t('onaylar.tarihAraligi') }}:</span>
                    <strong>{{ formatDate(i.baslangic) }} → {{ formatDate(i.bitis) }}</strong>
                  </div>
                  <div class="flex justify-between text-primary font-semibold">
                    <span>{{ t('onaylar.izinSuresi') }}:</span>
                    <span>{{ i.gunSayisi }} {{ t('onaylar.gun') }}</span>
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
                  :label="t('onaylar.onayla')"
                  icon="pi pi-check"
                  class="p-button-success p-button-sm flex-1"
                  @click="izinOnay(i, 'ONAYLANDI')"
                />
                <Button
                  :label="t('onaylar.reddet')"
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
            {{ t('onaylar.bekleyenIzinYok') }}
          </div>
        </div>
      </TabPanel>

      <TabPanel>
        <template #header>
          <span class="flex items-center gap-1.5">
            <i class="pi pi-wallet" />
            {{ t('onaylar.sahaMasrafAvans') }}
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
                      <h4 class="font-bold text-sm text-primary dark:text-gray-100">
                        {{ m.personelAdi || m.kullaniciAdi || t('onaylar.sahaPersoneli') }}
                      </h4>
                      <span class="text-xs px-2 py-0.5 rounded bg-gray-100 dark:bg-gray-700 font-semibold">{{ m.tur === 'AVANS' ? t('onaylar.avans') : m.kategori }}</span>
                    </div>
                  </div>
                  <div class="text-right font-extrabold text-base text-emerald-600">
                    {{ formatCurrency(m.tutar) }} {{ m.paraBirimi || 'TRY' }}
                  </div>
                </div>
                <div class="bg-secondary dark:bg-gray-700/50 p-2.5 rounded-lg text-xs space-y-1 mb-3">
                  <div class="flex justify-between text-muted">
                    <span>{{ t('onaylar.fisTarihi') }}:</span>
                    <strong>{{ formatDate(m.tarih) }}</strong>
                  </div>
                  <div class="text-primary dark:text-gray-200">
                    {{ m.aciklama }}
                  </div>
                </div>
              </div>
              <div class="flex gap-2 pt-2 border-t">
                <Button
                  :label="t('onaylar.onaylaGidereIsle')"
                  icon="pi pi-check"
                  class="p-button-success p-button-sm flex-1"
                  @click="masrafOnayla(m)"
                />
                <Button
                  :label="t('onaylar.reddet')"
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
            {{ t('onaylar.bekleyenMasrafYok') }}
          </div>
        </div>
      </TabPanel>

      <TabPanel>
        <template #header>
          <span class="flex items-center gap-1.5">
            <i class="pi pi-shopping-bag" />
            {{ t('onaylar.satinAlmaTalepleri') }}
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
              v-for="talep in bekleyenTalepler"
              :key="talep.id"
              class="onay-kart p-4 rounded-xl border bg-white dark:bg-gray-800 shadow-sm flex flex-col justify-between"
            >
              <div>
                <div class="flex justify-between items-start mb-2">
                  <span class="font-bold text-sm text-primary">#{{ talep.talepNo }}</span>
                  <Tag
                    :value="t('onaylar.onayBekliyor')"
                    severity="warning"
                  />
                </div>
                <h4 class="font-bold text-sm text-primary dark:text-gray-100 mb-1">
                  {{ talep.talepEden || '-' }} · {{ talep.departman || t('onaylar.genel') }}
                </h4>
                <p class="text-xs text-muted mb-3">
                  {{ talep.aciklama }}
                </p>
              </div>
              <div class="flex gap-2 pt-2 border-t">
                <Button
                  :label="t('onaylar.onayla')"
                  icon="pi pi-check"
                  class="p-button-success p-button-sm flex-1"
                  @click="talepOnay(talep, 'ONAYLANDI')"
                />
                <Button
                  :label="t('onaylar.reddet')"
                  icon="pi pi-times"
                  class="p-button-danger p-button-outlined p-button-sm flex-1"
                  @click="talepOnay(talep, 'REDDEDILDI')"
                />
              </div>
            </div>
          </div>
          <div
            v-else
            class="text-center py-12 text-muted"
          >
            <i class="pi pi-shopping-bag text-4xl text-gray-400 block mb-2" />
            {{ t('onaylar.bekleyenTalepYok') }}
          </div>
        </div>
      </TabPanel>

      <TabPanel>
        <template #header>
          <span class="flex items-center gap-1.5">
            <i class="pi pi-receipt" />
            {{ t('onaylar.sahaSiparisleri') }}
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
                    :value="t('onaylar.onayBekliyor')"
                    severity="warning"
                  />
                </div>
                <h4 class="font-bold text-sm text-primary dark:text-gray-100 mb-1">
                  {{ s.cariHesapAd || s.musteriAd || s.cariHesapAdi || t('onaylar.musteri') }}
                </h4>
                <p class="text-xs text-muted mb-1">
                  {{ s.aciklama || t('onaylar.sahaSiparisi') }}
                </p>
                <p class="text-xs font-semibold text-primary mb-3">
                  {{ formatCurrency(s.genelToplam || s.toplamTutar || 0) }}
                </p>
              </div>
              <div class="flex gap-2 pt-2 border-t">
                <Button
                  :label="t('onaylar.onayla')"
                  icon="pi pi-check"
                  class="p-button-success p-button-sm flex-1"
                  @click="siparisOnay(s, 'HAZIRLANIYOR')"
                />
                <Button
                  :label="t('onaylar.reddet')"
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
            {{ t('onaylar.bekleyenSiparisYok') }}
          </div>
        </div>
      </TabPanel>
      <TabPanel>
        <template #header>
          <span class="flex items-center gap-1.5">
            <i class="pi pi-sliders-h" />
            {{ t('onaylar.onayAyarlari') }}
          </span>
        </template>
        <div class="onay-icerik p-4">
          <p class="text-xs text-muted mb-4">
            {{ t('onaylar.ayarAciklama') }}
          </p>
          <DataTable
            :value="onayAyarlari"
            size="small"
            striped-rows
            :loading="ayarYukleniyor"
          >
            <Column
              field="modul"
              :header="t('onaylar.modul')"
            />
            <Column :header="t('onaylar.esikTutar')">
              <template #body="{ data }">
                <InputNumber
                  v-model="data.esikTutar"
                  mode="currency"
                  currency="TRY"
                  locale="tr-TR"
                  size="small"
                />
              </template>
            </Column>
            <Column :header="t('onaylar.otomatikOnay')">
              <template #body="{ data }">
                <ToggleSwitch
                  v-model="data.otomatikOnay"
                />
              </template>
            </Column>
            <Column :header="t('onaylar.islem')">
              <template #body="{ data }">
                <Button
                  icon="pi pi-save"
                  size="small"
                  :label="t('common.save')"
                  @click="ayarKaydet(data)"
                />
              </template>
            </Column>
          </DataTable>
        </div>
      </TabPanel>
    </TabView>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { personelIzinAPI, personelMasrafTalepAPI, satinalmaTalepAPI, siparisAPI, onayAyariAPI } from '../api/index.js'
import { useAuthStore } from '../stores/authStore.js'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { formatCurrency, formatDate } from '../utils/format.js'

const { t } = useI18n()
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
    toastBildirim.hata(err?.response?.data?.message || t('onaylar.yuklemeHatasi'))
  } finally {
    yukleniyor.value = false
  }
}

const izinOnay = async (i, durum) => {
  try {
    await personelIzinAPI.durumGuncelle(i.id, durum, authStore?.kullanici?.displayName || authStore?.kullanici?.username)
    toastBildirim.basarili(durum === 'ONAYLANDI' ? t('onaylar.izinOnaylandi') : t('onaylar.izinReddedildi'))
    await yukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('onaylar.islemBasarisiz'))
  }
}

const masrafOnayla = async (m) => {
  try {
    await personelMasrafTalepAPI.onayla(m.id, t('onaylar.muhasebeOnayiNotu'))
    toastBildirim.basarili(t('onaylar.masrafOnaylandi'))
    await yukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('onaylar.islemBasarisiz'))
  }
}

const masrafReddet = async (m) => {
  const not = prompt(t('onaylar.retGerekcesi'), t('onaylar.belgeFisYetersiz'))
  if (not === null) return
  try {
    await personelMasrafTalepAPI.reddet(m.id, not)
    toastBildirim.basarili(t('onaylar.masrafReddedildi'))
    await yukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('onaylar.islemBasarisiz'))
  }
}

const talepOnay = async (talep, durum) => {
  try {
    await satinalmaTalepAPI.durumGuncelle(talep.id, durum)
    toastBildirim.basarili(durum === 'ONAYLANDI' ? t('onaylar.satinalmaOnaylandi') : t('onaylar.satinalmaReddedildi'))
    await yukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('onaylar.islemBasarisiz'))
  }
}

const siparisOnay = async (s, durum) => {
  try {
    await siparisAPI.durumGuncelle(s.id, durum)
    toastBildirim.basarili(durum === 'HAZIRLANIYOR' ? t('onaylar.siparisOnaylandi') : t('onaylar.siparisReddedildi'))
    await yukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('onaylar.islemBasarisiz'))
  }
}

const onayAyarlari = ref([])
const ayarYukleniyor = ref(false)

const ayarlariYukle = async () => {
  ayarYukleniyor.value = true
  try {
    const r = await onayAyariAPI.listele()
    onayAyarlari.value = r.data || []
  } catch {
    onayAyarlari.value = []
  } finally {
    ayarYukleniyor.value = false
  }
}

const ayarKaydet = async (ayar) => {
  try {
    await onayAyariAPI.kaydet({ modul: ayar.modul, esikTutar: ayar.esikTutar, otomatikOnay: ayar.otomatikOnay })
    toastBildirim.basarili(`${ayar.modul} ${t('onaylar.ayarKaydedildi')}`)
    await ayarlariYukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('onaylar.ayarKaydetHatasi'))
  }
}

onMounted(() => {
  yukle()
  ayarlariYukle()
})
</script>

<style scoped>
.onaylar-sayfasi {
  padding: 0;
  max-width: 100%;
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
