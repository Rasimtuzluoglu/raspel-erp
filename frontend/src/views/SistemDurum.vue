<template>
  <div class="sistem-durum">
    <div class="sayfa-baslik">
      <h1>
        <i
          class="pi pi-server"
          style="margin-right: 8px"
        />{{ t('sistemDurum.title') }}
      </h1>
      <Button
        icon="pi pi-refresh"
        :label="t('sistemDurum.refresh')"
        class="p-button-outlined"
        :loading="yukleniyor"
        @click="yukle"
      />
    </div>

    <div
      v-if="!durum && !yukleniyor"
      class="bos"
    >
      {{ t('sistemDurum.durumAlinamadi') }}
    </div>

    <div
      v-else
      class="icerik"
    >
      <div class="kart-grid">
        <div class="durum-kart">
          <span class="kart-etiket">{{ t('sistemDurum.genelDurum') }}</span>
          <strong :class="durum?.durum === 'UP' ? 'iyi' : 'kotu'">
            <i :class="durum?.durum === 'UP' ? 'pi pi-check-circle' : 'pi pi-exclamation-triangle'" />
            {{ durum?.durum || '...' }}
          </strong>
        </div>
        <div class="durum-kart">
          <span class="kart-etiket">{{ t('sistemDurum.uptime') }}</span>
          <strong>{{ formatSure(durum?.uptimeMs) }}</strong>
        </div>
        <div class="durum-kart">
          <span class="kart-etiket">{{ t('sistemDurum.surum') }}</span>
          <strong>{{ durum?.surum || '...' }}</strong>
        </div>
        <div class="durum-kart">
          <span class="kart-etiket">{{ t('sistemDurum.toplamHata') }}</span>
          <strong :class="durum?.hataSayisi > 0 ? 'kotu' : 'iyi'">{{ durum?.hataSayisi ?? '...' }}</strong>
        </div>
      </div>

      <div class="bolum">
        <h2>{{ t('sistemDurum.bilesenler') }}</h2>
        <div class="bilesenler">
          <div
            v-for="(v, k) in durum?.bilesenler || {}"
            :key="k"
            class="bilesen"
          >
            <span>{{ bilesenAdi(k) }}</span>
            <i :class="v === 'UP' ? 'pi pi-check-circle iyi' : 'pi pi-exclamation-circle kotu'" />
          </div>
        </div>
      </div>

      <div class="bolum">
        <h2>{{ t('sistemDurum.kaynakKullanimi') }}</h2>
        <div class="kaynaklar">
          <div class="kaynak">
            <span>{{ t('sistemDurum.bellek') }}</span>
            <ProgressBar :value="bellekYuzde" />
            <small>{{ formatByte(durum?.bellek?.toplam - durum?.bellek?.bos) }} /
              {{ formatByte(durum?.bellek?.toplam) }}</small>
          </div>
          <div class="kaynak">
            <span>{{ t('sistemDurum.disk') }}</span>
            <ProgressBar :value="diskYuzde" />
            <small>{{ formatByte(durum?.disk?.toplam - durum?.disk?.kullanilabilir) }} /
              {{ formatByte(durum?.disk?.toplam) }}</small>
          </div>
        </div>
      </div>

      <div class="bolum">
        <h2>{{ t('sistemDurum.depolama') }}</h2>
        <div class="yedek-ozet">
          <div>
            <span>{{ t('sistemDurum.tip') }}</span><strong>{{ durum?.depolama?.tip === 'minio' ? 'MinIO (S3)' : t('sistemDurum.yerelDisk') }}</strong>
          </div>
          <div>
            <span>{{ t('sistemDurum.nesneSayisi') }}</span><strong>{{ durum?.depolama?.nesneSayisi ?? 0 }}</strong>
          </div>
          <div>
            <span>{{ t('sistemDurum.toplamBoyut') }}</span><strong>{{ formatByte(durum?.depolama?.toplamBoyut) }}</strong>
          </div>
        </div>
      </div>

      <div class="bolum">
        <h2>{{ t('sistemDurum.yedekleme') }}</h2>
        <div class="yedek-ozet">
          <div>
            <span>{{ t('sistemDurum.toplamYedek') }}</span><strong>{{ durum?.yedekleme?.totalBackups ?? 0 }}</strong>
          </div>
          <div>
            <span>{{ t('sistemDurum.toplamBoyut') }}</span><strong>{{ formatByte(durum?.yedekleme?.totalSize) }}</strong>
          </div>
          <div>
            <span>{{ t('sistemDurum.sonYedek') }}</span><strong class="kucuk">{{
              durum?.yedekleme?.lastBackup ? new Date(durum.yedekleme.lastBackup).toLocaleString('tr-TR') : '-'
            }}</strong>
          </div>
          <Button
            :label="t('sistemDurum.yedekAl')"
            icon="pi pi-save"
            class="p-button-success p-button-sm"
            :loading="yedekleniyor"
            @click="yedekAl"
          />
        </div>
      </div>

      <div class="bolum">
        <div class="bolum-baslik">
          <h2>{{ t('sistemDurum.sonHatalar') }}</h2>
          <Button
            v-if="authStore.isAdmin && sonHatalar?.length"
            :label="t('sistemDurum.hatalariTemizle')"
            icon="pi pi-trash"
            class="p-button-sm p-button-text p-button-danger"
            :loading="temizleniyor"
            @click="hataTemizle"
          />
        </div>
        <div
          v-if="(!sonHatalar || !sonHatalar.length)"
          class="bos-kucuk"
        >
          <i class="pi pi-check-circle" /> {{ t('sistemDurum.hataYok') }}
        </div>
        <div
          v-for="h in sonHatalar"
          :key="h.id"
          class="hata-satir"
        >
          <div class="hata-bilgi">
            <strong>{{ h.tur }}</strong>
            <p>{{ h.mesaj }}</p>
            <small>{{ h.endpoint }} ·
              {{ h.olusturmaTarihi ? new Date(h.olusturmaTarihi).toLocaleString('tr-TR') : '' }}</small>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { sistemDurumAPI, backupAPI } from '../api/index.js'
import { useAuthStore } from '../stores/authStore.js'
import { useToastBildirim } from '../composables/useToastBildirim.js'

const { t } = useI18n()
const toastBildirim = useToastBildirim()
const authStore = useAuthStore()
const durum = ref(null)
const sonHatalar = ref([])
const yukleniyor = ref(false)
const yedekleniyor = ref(false)
const temizleniyor = ref(false)

const bilesenAdi = (k) =>
  ({ db: t('sistemDurum.veritabani'), redis: 'Redis', rabbit: 'RabbitMQ', diskSpace: 'Disk', ping: 'Ping', mail: t('sistemDurum.eposta') })[k] || k

const bellekYuzde = computed(() => {
  if (!durum.value?.bellek?.toplam) return 0
  const kullanilan = durum.value.bellek.toplam - durum.value.bellek.bos
  return Math.round((kullanilan / durum.value.bellek.toplam) * 100)
})

const diskYuzde = computed(() => {
  if (!durum.value?.disk?.toplam) return 0
  const kullanilan = durum.value.disk.toplam - durum.value.disk.kullanilabilir
  return Math.round((kullanilan / durum.value.disk.toplam) * 100)
})

const formatByte = (b) => {
  if (b == null || isNaN(b)) return '-'
  const kb = 1024,
    mb = kb * 1024,
    gb = mb * 1024
  if (b >= gb) return (b / gb).toFixed(2) + ' GB'
  if (b >= mb) return (b / mb).toFixed(1) + ' MB'
  if (b >= kb) return (b / kb).toFixed(0) + ' KB'
  return b + ' B'
}

const formatSure = (ms) => {
  if (ms == null) return '-'
  const sn = Math.floor(ms / 1000)
  const g = Math.floor(sn / 3600)
  const dk = Math.floor((sn % 3600) / 60)
  const gun = Math.floor(g / 24)
  if (gun > 0) return `${gun}g ${g % 24}s`
  return `${g}s ${dk}dk`
}

const yukle = async () => {
  yukleniyor.value = true
  try {
    const [d, h] = await Promise.all([sistemDurumAPI.durum(), sistemDurumAPI.hataLog()])
    durum.value = d.data
    sonHatalar.value = h.data || []
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('sistemDurum.durumAlinamadi'))
  }
  yukleniyor.value = false
}

const yedekAl = async () => {
  yedekleniyor.value = true
  try {
    await backupAPI.manual('DAILY')
    toastBildirim.basarili(t('sistemDurum.yedeklemeBaslatildi'))
    await yukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('sistemDurum.yedeklemeBasarisiz'))
  }
  yedekleniyor.value = false
}

const hataTemizle = async () => {
  temizleniyor.value = true
  try {
    await sistemDurumAPI.hataLogTemizle()
    toastBildirim.basarili(t('sistemDurum.hataLoglariTemizlendi'))
    await yukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('sistemDurum.hataLoglariTemizlenemedi'))
  }
  temizleniyor.value = false
}

onMounted(yukle)
</script>

<style scoped>
.sistem-durum {
  padding: 0;
}
.sayfa-baslik {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.sayfa-baslik h1 {
  margin: 0;
}
.bos {
  text-align: center;
  color: var(--text-muted);
  padding: 40px 0;
}
.kart-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(180px, 100%), 1fr));
  gap: 14px;
  margin-bottom: 20px;
}
.durum-kart {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 16px;
  text-align: center;
}
.kart-etiket {
  display: block;
  font-size: 12px;
  color: var(--text-secondary);
  margin-bottom: 6px;
}
.durum-kart strong {
  font-size: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}
.iyi {
  color: #22c55e;
}
.kotu {
  color: #ef4444;
}
.bolum {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 16px;
}
.bolum h2 {
  margin: 0 0 12px;
  font-size: 15px;
}
.bolum-baslik {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.bolum-baslik h2 {
  margin: 0;
  font-size: 15px;
}
.bilesenler {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.bilesen {
  display: flex;
  align-items: center;
  gap: 8px;
  background: var(--bg-primary);
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 8px 12px;
  font-size: 13px;
}
.kaynaklar {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.kaynak span {
  display: block;
  font-size: 13px;
  margin-bottom: 6px;
}
.kaynak small {
  color: var(--text-muted);
}
.yedek-ozet {
  display: flex;
  flex-wrap: wrap;
  gap: 24px;
  align-items: center;
}
.yedek-ozet div {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.yedek-ozet span {
  font-size: 12px;
  color: var(--text-secondary);
}
.yedek-ozet strong {
  font-size: 16px;
}
.yedek-ozet strong.kucuk {
  font-size: 13px;
}
.bos-kucuk {
  color: var(--text-muted);
  padding: 12px 0;
}
.hata-satir {
  padding: 10px 0;
  border-bottom: 1px solid var(--border);
}
.hata-satir:last-child {
  border-bottom: none;
}
.hata-bilgi strong {
  font-size: 13px;
  color: #f87171;
}
.hata-bilgi p {
  margin: 2px 0;
  font-size: 13px;
}
.hata-bilgi small {
  color: var(--text-muted);
  font-size: 11px;
}
</style>
