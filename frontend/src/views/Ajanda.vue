<template>
  <div class="ajanda-sayfasi">
    <div class="ajanda-baslik">
      <h1>
        <i
          class="pi pi-calendar"
          style="margin-right: 8px"
        />Ajanda
      </h1>
      <div class="ay-gezinme">
        <Button
          icon="pi pi-chevron-left"
          class="p-button-text p-button-sm"
          @click="ayDegistir(-1)"
        />
        <span class="ay-etiket">{{ ayEtiket }}</span>
        <Button
          icon="pi pi-chevron-right"
          class="p-button-text p-button-sm"
          @click="ayDegistir(1)"
        />
        <Button
          label="Bugün"
          class="p-button-sm p-button-outlined"
          @click="buguneGit"
        />
        <Button
          label="Görev Ekle"
          icon="pi pi-plus"
          class="p-button-sm p-button-success"
          @click="gorevDialogAc = true"
        />
        <Button
          label="Hatırlatıcı"
          icon="pi pi-bell"
          class="p-button-sm p-button-outlined"
          @click="hatirlaticiDialogAc = true"
        />
      </div>
    </div>

    <div
      v-if="gorevler.length || hatirlaticilar.length"
      class="gorev-ozet"
    >
      <div class="gorev-ozet-baslik">
        <i class="pi pi-check-square" /> Görevlerim ({{ gorevler.length }})
      </div>
      <div
        v-for="g in gorevler"
        :key="g.id"
        class="gorev-satir"
        :class="{ tamamlandi: g.durum === 'TAMAMLANDI' }"
      >
        <Checkbox
          :model-value="g.durum === 'TAMAMLANDI'"
          :binary="true"
          @update:model-value="gorevDurumDegistir(g)"
        />
        <span class="gorev-baslik">{{ g.baslik }}</span>
        <span
          v-if="g.aciklama"
          class="gorev-aciklama"
        >{{ g.aciklama }}</span>
        <Tag
          :value="oncelikAdi(g.oncelik)"
          :severity="oncelikSeverity(g.oncelik)"
        />
        <span
          v-if="g.bitisTarihi"
          class="gorev-tarih"
        >{{ formatTarih(g.bitisTarihi) }}</span>
        <button
          class="gorev-sil"
          title="Sil"
          @click="gorevSil(g)"
        >
          <i class="pi pi-trash" />
        </button>
      </div>
      <div
        v-for="h in hatirlaticilar"
        :key="'h' + h.id"
        class="gorev-satir hatirlatici"
      >
        <i class="pi pi-bell" />
        <span class="gorev-baslik">{{ h.baslik }}</span>
        <span class="gorev-tarih">{{ formatZaman(h.hatirlatmaZamani) }}</span>
        <button
          class="gorev-sil"
          title="Sil"
          @click="hatirlaticiSil(h)"
        >
          <i class="pi pi-trash" />
        </button>
      </div>
    </div>

    <div class="takvim-grid">
      <div class="gun-basliklar">
        <span
          v-for="g in gunBasliklari"
          :key="g"
          class="gun-baslik"
        >{{ g }}</span>
      </div>
      <div class="gun-hucreleri">
        <div
          v-for="(gun, i) in gunler"
          :key="i"
          class="gun-hucre"
          :class="{ bos: !gun, bugun: gun && ayniGun(gun, bugun), secili: gun && ayniGun(gun, seciliGun) }"
          @click="gun && gunuSec(gun)"
        >
          <template v-if="gun">
            <span class="gun-no">{{ gun.getDate() }}</span>
            <div
              v-if="gunOlaylari(gun).length"
              class="olay-noktalar"
            >
              <span
                v-for="o in gunOlaylari(gun).slice(0, 3)"
                :key="o.tarih + o.tip + o.baslik"
                class="olay-nokta"
                :class="o.tip === 'VADE' ? 'vade' : 'gorev'"
              />
            </div>
          </template>
        </div>
      </div>
    </div>

    <div class="gun-detay">
      <h3 v-if="seciliGun">
        {{ seciliGun.toLocaleDateString('tr-TR', { weekday: 'long', day: 'numeric', month: 'long' }) }}
      </h3>
      <div
        v-if="(!seciliOlaylar || !seciliOlaylar.length)"
        class="bos"
      >
        Bu günde olay yok.
      </div>
      <div
        v-for="o in seciliOlaylar"
        :key="o.tarih + o.tip + o.baslik"
        class="olay-karti"
      >
        <i :class="olayIkon(o.tip)" />
        <div>
          <strong>{{ o.baslik }}</strong>
          <p>{{ o.aciklama }}</p>
        </div>
        <Tag
          :value="olayTipAdi(o.tip)"
          :severity="olayTipSeverity(o.tip)"
        />
      </div>
    </div>

    <!-- Görev Ekle Dialog -->
    <Dialog
      v-model:visible="gorevDialogAc"
      header="Görev Ekle"
      :modal="true"
      :style="{ width: '460px' }"
    >
      <div class="ajanda-form">
        <div class="field">
          <label>Başlık *</label>
          <InputText
            v-model="gorevForm.baslik"
            placeholder="Görev başlığı"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>Bitiş Tarihi</label>
          <DatePicker
            v-model="gorevForm.bitisTarihi"
            show-icon
            date-format="dd/mm/yy"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>Öncelik</label>
          <Dropdown
            v-model="gorevForm.oncelik"
            :options="oncelikSecenekleri"
            option-label="label"
            option-value="value"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>Açıklama</label>
          <Textarea
            v-model="gorevForm.aciklama"
            rows="2"
            class="w-full"
          />
        </div>
      </div>
      <template #footer>
        <Button
          label="İptal"
          class="p-button-text"
          @click="gorevDialogAc = false"
        />
        <Button
          label="Kaydet"
          icon="pi pi-check"
          :loading="kaydediliyor"
          @click="gorevKaydet"
        />
      </template>
    </Dialog>

    <!-- Hatırlatıcı Ekle Dialog -->
    <Dialog
      v-model:visible="hatirlaticiDialogAc"
      header="Hatırlatıcı Ekle"
      :modal="true"
      :style="{ width: '460px' }"
    >
      <div class="ajanda-form">
        <div class="field">
          <label>Başlık *</label>
          <InputText
            v-model="hatirlaticiForm.baslik"
            placeholder="Hatırlatıcı başlığı"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>Hatırlatma Zamanı *</label>
          <DatePicker
            v-model="hatirlaticiForm.hatirlatmaZamani"
            show-time
            hour-format="24"
            show-icon
            date-format="dd/mm/yy"
            class="w-full"
          />
        </div>
      </div>
      <template #footer>
        <Button
          label="İptal"
          class="p-button-text"
          @click="hatirlaticiDialogAc = false"
        />
        <Button
          label="Kaydet"
          icon="pi pi-check"
          :loading="kaydediliyor"
          @click="hatirlaticiKaydet"
        />
      </template>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ajandaAPI } from '../api/index.js'
import { useToastBildirim } from '../composables/useToastBildirim.js'

const toastBildirim = useToastBildirim()
const olaylar = ref([])
const gorevler = ref([])
const hatirlaticilar = ref([])
const aktifAy = ref(new Date())
const bugun = new Date()
const seciliGun = ref(null)
const gorevDialogAc = ref(false)
const hatirlaticiDialogAc = ref(false)
const kaydediliyor = ref(false)

const gorevForm = ref({ baslik: '', bitisTarihi: null, oncelik: 'ORTA', aciklama: '' })
const hatirlaticiForm = ref({ baslik: '', hatirlatmaZamani: null })

const oncelikSecenekleri = [
  { label: 'Düşük', value: 'DUSUK' },
  { label: 'Orta', value: 'ORTA' },
  { label: 'Yüksek', value: 'YUKSEK' }
]

const gunBasliklari = ['Pzt', 'Sal', 'Çar', 'Per', 'Cum', 'Cmt', 'Paz']

const ayEtiket = computed(() => aktifAy.value.toLocaleDateString('tr-TR', { month: 'long', year: 'numeric' }))

const gunler = computed(() => {
  const yil = aktifAy.value.getFullYear()
  const ay = aktifAy.value.getMonth()
  const ilkGun = new Date(yil, ay, 1)
  let baslangicHaftasi = ilkGun.getDay() - 1
  if (baslangicHaftasi < 0) baslangicHaftasi = 6
  const gunSayisi = new Date(yil, ay + 1, 0).getDate()
  const liste = []
  for (let i = 0; i < baslangicHaftasi; i++) liste.push(null)
  for (let d = 1; d <= gunSayisi; d++) liste.push(new Date(yil, ay, d))
  return liste
})

const ayniGun = (a, b) => a && b && a.toDateString() === b.toDateString()

const ayDegistir = (delta) => {
  aktifAy.value = new Date(aktifAy.value.getFullYear(), aktifAy.value.getMonth() + delta, 1)
  yukle()
}

const buguneGit = () => {
  aktifAy.value = new Date()
  seciliGun.value = new Date()
  yukle()
}

const gunuSec = (gun) => {
  seciliGun.value = gun
}

const gunOlaylari = (gun) => olaylar.value.filter((o) => ayniGun(new Date(o.tarih), gun))

const seciliOlaylar = computed(() => (seciliGun.value ? gunOlaylari(seciliGun.value) : []))

const oncelikAdi = (o) => oncelikSecenekleri.find((x) => x.value === o)?.label || 'Orta'
const oncelikSeverity = (o) => (o === 'YUKSEK' ? 'danger' : o === 'DUSUK' ? 'secondary' : 'warn')

const olayIkon = (tip) => {
  const map = { VADE: 'pi pi-money-bill vade-ikon', GOREV: 'pi pi-check-square gorev-ikon', KISISEL_GOREV: 'pi pi-check-square gorev-ikon', HATIRLATICI: 'pi pi-bell hatirlatici-ikon' }
  return map[tip] || 'pi pi-info-circle gorev-ikon'
}
const olayTipAdi = (tip) => {
  const map = { VADE: 'Vade', GOREV: 'Görev', KISISEL_GOREV: 'Görevim', HATIRLATICI: 'Hatırlatıcı' }
  return map[tip] || tip
}
const olayTipSeverity = (tip) => {
  const map = { VADE: 'warn', GOREV: 'info', KISISEL_GOREV: 'success', HATIRLATICI: 'danger' }
  return map[tip] || 'info'
}

const formatTarih = (d) => (d ? new Date(d).toLocaleDateString('tr-TR', { day: '2-digit', month: '2-digit' }) : '')
const formatZaman = (t) => (t ? new Date(t).toLocaleString('tr-TR', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' }) : '')

const yukle = async () => {
  const yil = aktifAy.value.getFullYear()
  const ay = aktifAy.value.getMonth()
  const baslangic = new Date(yil, ay, 1)
  const bitis = new Date(yil, ay + 1, 0)
  const fmt = (d) => d.toISOString().split('T')[0]
  try {
    const r = await ajandaAPI.olaylar({ baslangic: fmt(baslangic), bitis: fmt(bitis) })
    olaylar.value = r.data || []
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || 'Ajanda yüklenemedi')
  }
}

const gorevleriYukle = async () => {
  try {
    const r = await ajandaAPI.gorevler()
    gorevler.value = r.data || []
  } catch {
    gorevler.value = []
  }
}

const hatirlaticilariYukle = async () => {
  try {
    const r = await ajandaAPI.hatirlaticilar()
    hatirlaticilar.value = r.data || []
  } catch {
    hatirlaticilar.value = []
  }
}

const gorevKaydet = async () => {
  if (!gorevForm.value.baslik.trim()) {
    toastBildirim.uyari('Görev başlığı zorunludur')
    return
  }
  kaydediliyor.value = true
  try {
    await ajandaAPI.gorevOlustur({
      ...gorevForm.value,
      bitisTarihi: gorevForm.value.bitisTarihi ? gorevForm.value.bitisTarihi.toISOString().slice(0, 10) : null
    })
    toastBildirim.basarili('Görev oluşturuldu')
    gorevDialogAc.value = false
    gorevForm.value = { baslik: '', bitisTarihi: null, oncelik: 'ORTA', aciklama: '' }
    gorevleriYukle()
    yukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || 'Görev oluşturulamadı')
  } finally {
    kaydediliyor.value = false
  }
}

const hatirlaticiKaydet = async () => {
  if (!hatirlaticiForm.value.baslik.trim() || !hatirlaticiForm.value.hatirlatmaZamani) {
    toastBildirim.uyari('Başlık ve zaman zorunludur')
    return
  }
  kaydediliyor.value = true
  try {
    await ajandaAPI.hatirlaticiOlustur({
      baslik: hatirlaticiForm.value.baslik,
      hatirlatmaZamani: hatirlaticiForm.value.hatirlatmaZamani.toISOString()
    })
    toastBildirim.basarili('Hatırlatıcı oluşturuldu')
    hatirlaticiDialogAc.value = false
    hatirlaticiForm.value = { baslik: '', hatirlatmaZamani: null }
    hatirlaticilariYukle()
    yukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || 'Hatırlatıcı oluşturulamadı')
  } finally {
    kaydediliyor.value = false
  }
}

const gorevDurumDegistir = async (g) => {
  try {
    if (g.durum === 'TAMAMLANDI') {
      await ajandaAPI.gorevGuncelle(g.id, { durum: 'BEKLIYOR' })
    } else {
      await ajandaAPI.gorevTamamla(g.id)
    }
    gorevleriYukle()
    yukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || 'İşlem başarısız')
  }
}

const gorevSil = async (g) => {
  try {
    await ajandaAPI.gorevSil(g.id)
    gorevleriYukle()
    yukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || 'Silinemedi')
  }
}

const hatirlaticiSil = async (h) => {
  try {
    await ajandaAPI.hatirlaticiSil(h.id)
    hatirlaticilariYukle()
    yukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || 'Silinemedi')
  }
}

onMounted(() => {
  seciliGun.value = new Date()
  yukle()
  gorevleriYukle()
  hatirlaticilariYukle()
})
</script>

<style scoped>
.ajanda-sayfasi {
  padding: 0;
}
.ajanda-baslik {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.ajanda-baslik h1 {
  margin: 0;
}
.ay-gezinme {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.ay-etiket {
  font-weight: 700;
  min-width: 120px;
  text-align: center;
  text-transform: capitalize;
}
.takvim-grid {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  overflow: hidden;
}
.gun-basliklar {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  background: var(--bg-primary);
}
.gun-baslik {
  padding: 8px;
  text-align: center;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
}
.gun-hucreleri {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
}
.gun-hucre {
  min-height: 70px;
  border: 1px solid var(--border);
  padding: 6px;
  cursor: pointer;
}
.gun-hucre.bos {
  background: rgba(0, 0, 0, 0.02);
  cursor: default;
}
.gun-hucre.bugun {
  background: rgba(59, 130, 246, 0.08);
}
.gun-hucre.secili {
  outline: 2px solid #3b82f6;
}
.gun-no {
  font-size: 13px;
  font-weight: 600;
}
.olay-noktalar {
  display: flex;
  gap: 4px;
  margin-top: 6px;
}
.olay-nokta {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}
.olay-nokta.vade {
  background: #f59e0b;
}
.olay-nokta.gorev {
  background: #3b82f6;
}
.gun-detay {
  margin-top: 16px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 16px;
}
.gun-detay h3 {
  margin: 0 0 12px;
  text-transform: capitalize;
}
.bos {
  color: var(--text-muted);
  padding: 12px 0;
}
.olay-karti {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid var(--border);
}
.olay-karti:last-child {
  border-bottom: none;
}
.olay-karti i {
  font-size: 18px;
}
.vade-ikon {
  color: #f59e0b;
}
.gorev-ikon {
  color: #3b82f6;
}
.hatirlatici-ikon {
  color: #ef4444;
}
.gorev-ozet {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 14px 16px;
  margin-bottom: 16px;
}
.gorev-ozet-baslik {
  font-size: 13px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 6px;
}
.gorev-ozet-baslik i {
  color: var(--accent);
}
.gorev-satir {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 0;
  border-bottom: 1px solid var(--border);
  font-size: 13px;
}
.gorev-satir:last-child {
  border-bottom: none;
}
.gorev-satir.tamamlandi .gorev-baslik {
  text-decoration: line-through;
  color: var(--text-muted);
}
.gorev-satir.hatirlatici {
  color: var(--text-secondary);
}
.gorev-baslik {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.gorev-aciklama {
  flex: 0 1 auto;
  max-width: 40%;
  font-size: 11px;
  color: var(--text-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.gorev-tarih {
  font-size: 11px;
  color: var(--text-muted);
  white-space: nowrap;
}
.gorev-sil {
  background: transparent;
  border: none;
  color: var(--text-muted);
  cursor: pointer;
  font-size: 13px;
  padding: 2px 4px;
  border-radius: 4px;
}
.gorev-sil:hover {
  color: #ef4444;
  background: rgba(239, 68, 68, 0.1);
}
.ajanda-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.ajanda-form .field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.ajanda-form .field label {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
}
.ajanda-form .w-full {
  width: 100%;
}
.olay-karti div {
  flex: 1;
}
.olay-karti strong {
  font-size: 14px;
}
.olay-karti p {
  margin: 2px 0 0;
  font-size: 12px;
  color: var(--text-secondary);
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
