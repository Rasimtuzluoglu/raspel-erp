<template>
  <div class="onaylar-sayfasi">
    <div class="sayfa-baslik">
      <h1>
        <i
          class="pi pi-check-circle"
          style="margin-right: 8px"
        />Bekleyen Onaylar
      </h1>
      <Button
        icon="pi pi-refresh"
        label="Yenile"
        class="p-button-text"
        :loading="yukleniyor"
        @click="yukle"
      />
    </div>

    <div class="ozet">
      <div class="ozet-kart">
        <span>Bekleyen İzin</span><strong>{{ bekleyenIzinler.length }}</strong>
      </div>
      <div class="ozet-kart">
        <span>Bekleyen Talep</span><strong>{{ bekleyenTalepler.length }}</strong>
      </div>
    </div>

    <Card>
      <template #title>
        <i
          class="pi pi-calendar"
          style="margin-right: 8px"
        />İzin Talepleri
      </template>
      <template #content>
        <div
          v-if="!bekleyenIzinler.length"
          class="bos"
        >
          Bekleyen izin talebi yok.
        </div>
        <div
          v-for="i in bekleyenIzinler"
          :key="i.id"
          class="onay-satir"
        >
          <div class="onay-bilgi">
            <strong>{{ i.personelAdi || '#' + i.personelId }}</strong>
            <p>{{ i.izinTuru }} · {{ i.baslangic }} → {{ i.bitis }} ({{ i.gunSayisi }} gün)</p>
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

    <Card>
      <template #title>
        <i
          class="pi pi-shopping-bag"
          style="margin-right: 8px"
        />Satın Alma Talepleri
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
import { personelIzinAPI, satinalmaTalepAPI } from '../api/index.js'
import { useAuthStore } from '../stores/authStore.js'
import { useToastBildirim } from '../composables/useToastBildirim.js'

const authStore = useAuthStore()
const toastBildirim = useToastBildirim()
const izinler = ref([])
const talepler = ref([])
const yukleniyor = ref(false)

const bekleyenIzinler = ref([])
const bekleyenTalepler = ref([])

const yukle = async () => {
  yukleniyor.value = true
  try {
    const [iRes, tRes] = await Promise.all([personelIzinAPI.getAll(), satinalmaTalepAPI.getAll()])
    izinler.value = iRes.data?.content || iRes.data || []
    talepler.value = tRes.data?.content || tRes.data || []
    bekleyenIzinler.value = izinler.value.filter((i) => i.durum === 'BEKLEMEDE')
    bekleyenTalepler.value = talepler.value.filter((t) => t.durum === 'TASLAK')
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || 'Onaylar yüklenemedi')
  }
  yukleniyor.value = false
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

const talepOnay = async (t, durum) => {
  try {
    await satinalmaTalepAPI.durumGuncelle(t.id, durum)
    toastBildirim.basarili(`Talep ${durum === 'ONAYLANDI' ? 'onaylandı' : 'reddedildi'}`)
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
.ozet {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}
.ozet-kart {
  flex: 1;
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
}
</style>
