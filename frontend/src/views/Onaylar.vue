<template>
  <div class="onaylar-sayfasi">
    <div class="sayfa-baslik">
      <h1>
        <i
          class="pi pi-check-circle"
          style="margin-right: 8px"
        />Yönetici & Muhasebe Onay Merkezi
      </h1>
      <Button
        icon="pi pi-refresh"
        label="Yenile"
        class="p-button-text"
        :loading="yukleniyor"
        @click="yukle"
      />
    </div>

    <!-- Özet Sayaç Kartları -->
    <div class="grid grid-cols-1 md:grid-cols-3 gap-4 mb-4">
      <div class="ozet-kart">
        <span>Bekleyen İzin Talebi</span>
        <strong class="text-blue-600">{{ bekleyenIzinler.length }}</strong>
      </div>
      <div class="ozet-kart">
        <span>Bekleyen Masraf / Avans</span>
        <strong class="text-emerald-600">{{ bekleyenMasraflar.length }}</strong>
      </div>
      <div class="ozet-kart">
        <span>Bekleyen Satınalma</span>
        <strong class="text-purple-600">{{ bekleyenTalepler.length }}</strong>
      </div>
    </div>

    <!-- 1. İZİN TALEPLERİ -->
    <Card class="mb-4">
      <template #title>
        <div class="flex justify-between items-center">
          <span><i
            class="pi pi-calendar"
            style="margin-right: 8px"
          />Personel İzin Talepleri</span>
          <Badge
            :value="bekleyenIzinler.length"
            severity="info"
          />
        </div>
      </template>
      <template #content>
        <div
          v-if="!bekleyenIzinler.length"
          class="bos"
        >
          Bekleyen izin talebi bulunmuyor.
        </div>
        <div
          v-for="i in bekleyenIzinler"
          :key="i.id"
          class="onay-satir"
        >
          <div class="onay-bilgi">
            <strong>{{ i.personelAdi || '#' + i.personelId }}</strong>
            <p>{{ i.izinTuru }} · {{ formatDate(i.baslangic) }} → {{ formatDate(i.bitis) }} ({{ i.gunSayisi }} gün)</p>
            <small
              v-if="i.aciklama"
              class="text-muted block mt-0.5"
            >Açıklama: {{ i.aciklama }}</small>
          </div>
          <div class="onay-aksiyon">
            <Button
              label="Onayla"
              icon="pi pi-check"
              class="p-button-sm p-button-success"
              @click="izinOnay(i, 'ONAYLANDI')"
            />
            <Button
              label="Reddet"
              icon="pi pi-times"
              class="p-button-sm p-button-danger p-button-outlined"
              @click="izinOnay(i, 'REDDEDILDI')"
            />
          </div>
        </div>
      </template>
    </Card>

    <!-- 2. SAHA MASRAF & AVANS TALEPLERİ -->
    <Card class="mb-4">
      <template #title>
        <div class="flex justify-between items-center">
          <span><i
            class="pi pi-wallet"
            style="margin-right: 8px"
          />Saha Masraf & Avans Talepleri</span>
          <Badge
            :value="bekleyenMasraflar.length"
            severity="success"
          />
        </div>
      </template>
      <template #content>
        <div
          v-if="!bekleyenMasraflar.length"
          class="bos"
        >
          Onay bekleyen saha masrafı veya avans talebi yok.
        </div>
        <div
          v-for="m in bekleyenMasraflar"
          :key="m.id"
          class="onay-satir"
        >
          <div class="onay-bilgi">
            <div class="flex items-center gap-2">
              <strong>{{ m.personelAdi || m.kullaniciAdi || 'Personel' }}</strong>
              <span class="text-xs px-2 py-0.5 rounded bg-gray-100 font-semibold">{{ m.tur === 'AVANS' ? 'Avans' : m.kategori }}</span>
            </div>
            <p class="text-sm font-bold text-emerald-600">
              {{ formatCurrency(m.tutar) }} {{ m.paraBirimi || 'TRY' }}
            </p>
            <p>{{ formatDate(m.tarih) }} · {{ m.aciklama }}</p>
          </div>
          <div class="onay-aksiyon">
            <Button
              label="Onayla (Gidere İşle)"
              icon="pi pi-check"
              class="p-button-sm p-button-success"
              @click="masrafOnayla(m)"
            />
            <Button
              label="Reddet"
              icon="pi pi-times"
              class="p-button-sm p-button-danger p-button-outlined"
              @click="masrafReddet(m)"
            />
          </div>
        </div>
      </template>
    </Card>

    <!-- 3. SATINALMA TALEPLERİ -->
    <Card>
      <template #title>
        <div class="flex justify-between items-center">
          <span><i
            class="pi pi-shopping-bag"
            style="margin-right: 8px"
          />Satın Alma Talepleri</span>
          <Badge
            :value="bekleyenTalepler.length"
            severity="warning"
          />
        </div>
      </template>
      <template #content>
        <div
          v-if="!bekleyenTalepler.length"
          class="bos"
        >
          Bekleyen satın alma talebi yok.
        </div>
        <div
          v-for="t in bekleyenTalepler"
          :key="t.id"
          class="onay-satir"
        >
          <div class="onay-bilgi">
            <strong>{{ t.talepNo }}</strong>
            <p>{{ t.talepEden || '-' }} · {{ t.departman || '-' }} · {{ t.aciklama }}</p>
          </div>
          <div class="onay-aksiyon">
            <Button
              label="Onayla"
              icon="pi pi-check"
              class="p-button-sm p-button-success"
              @click="talepOnay(t, 'ONAYLANDI')"
            />
            <Button
              label="Reddet"
              icon="pi pi-times"
              class="p-button-sm p-button-danger p-button-outlined"
              @click="talepOnay(t, 'REDDEDILDI')"
            />
          </div>
        </div>
      </template>
    </Card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { personelIzinAPI, personelMasrafTalepAPI, satinalmaTalepAPI } from '../api/index.js'
import { useAuthStore } from '../stores/authStore.js'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { formatCurrency, formatDate } from '../utils/format.js'

const authStore = useAuthStore()
const toastBildirim = useToastBildirim()

const yukleniyor = ref(false)
const bekleyenIzinler = ref([])
const bekleyenMasraflar = ref([])
const bekleyenTalepler = ref([])

const yukle = async () => {
  yukleniyor.value = true
  try {
    const [iRes, mRes, tRes] = await Promise.allSettled([
      personelIzinAPI.getAll(),
      personelMasrafTalepAPI.getBekleyenler(),
      satinalmaTalepAPI.getAll()
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
      bekleyenTalepler.value = allTalep.filter((t) => t.durum === 'TASLAK' || t.durum === 'BEKLEMEDE')
    }
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || 'Onaylar yüklenemedi')
  } finally {
    yukleniyor.value = false
  }
}

const izinOnay = async (i, durum) => {
  try {
    await personelIzinAPI.durumGuncelle(i.id, durum, authStore.kullanici?.displayName || authStore.kullanici?.username)
    toastBildirim.basarili(`İzin ${durum === 'ONAYLANDI' ? 'onaylandı' : 'reddedildi'}`)
    await yukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || 'İşlem başarısız')
  }
}

const masrafOnayla = async (m) => {
  try {
    await personelMasrafTalepAPI.onayla(m.id, 'Muhasebe tarafından onaylandı')
    toastBildirim.basarili(`Masraf talebi onaylandı ve şirket giderlerine işlendi`)
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
    toastBildirim.basarili(`Masraf talebi reddedildi`)
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

onMounted(yukle)
</script>

<style scoped>
.onaylar-sayfasi {
  padding: 0;
}
.sayfa-baslik {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.sayfa-baslik h1 {
  margin: 0;
}
.ozet-kart {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 16px;
  text-align: center;
}
.ozet-kart span {
  display: block;
  font-size: 13px;
  color: var(--text-secondary);
}
.ozet-kart strong {
  font-size: 24px;
}
.bos {
  color: var(--text-muted);
  padding: 12px 0;
}
.onay-satir {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 0;
  border-bottom: 1px solid var(--border);
  gap: 16px;
}
.onay-satir:last-child {
  border-bottom: none;
}
.onay-bilgi strong {
  font-size: 14px;
}
.onay-bilgi p {
  margin: 2px 0 0;
  font-size: 12px;
  color: var(--text-secondary);
}
.onay-aksiyon {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}
</style>
