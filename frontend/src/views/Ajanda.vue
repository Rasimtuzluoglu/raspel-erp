<template>
  <div class="ajanda-sayfasi">
    <div class="ajanda-baslik">
      <h1><i class="pi pi-calendar" style="margin-right:8px" />Ajanda</h1>
      <div class="ay-gezinme">
        <Button icon="pi pi-chevron-left" class="p-button-text p-button-sm" @click="ayDegistir(-1)" />
        <span class="ay-etiket">{{ ayEtiket }}</span>
        <Button icon="pi pi-chevron-right" class="p-button-text p-button-sm" @click="ayDegistir(1)" />
        <Button label="Bugün" class="p-button-sm p-button-outlined" @click="buguneGit" />
      </div>
    </div>

    <div class="takvim-grid">
      <div class="gun-basliklar">
        <span v-for="g in gunBasliklari" :key="g" class="gun-baslik">{{ g }}</span>
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
            <div v-if="gunOlaylari(gun).length" class="olay-noktalar">
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
      <h3 v-if="seciliGun">{{ seciliGun.toLocaleDateString('tr-TR', { weekday: 'long', day: 'numeric', month: 'long' }) }}</h3>
      <div v-if="!seciliOlaylar.length" class="bos">Bu günde olay yok.</div>
      <div v-for="o in seciliOlaylar" :key="o.tarih + o.tip + o.baslik" class="olay-karti">
        <i :class="o.tip === 'VADE' ? 'pi pi-money-bill vade-ikon' : 'pi pi-check-square gorev-ikon'" />
        <div>
          <strong>{{ o.baslik }}</strong>
          <p>{{ o.aciklama }}</p>
        </div>
        <Tag :value="o.tip === 'VADE' ? 'Vade' : 'Görev'" :severity="o.tip === 'VADE' ? 'warn' : 'info'" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ajandaAPI } from '../api/index.js'
import { useToastBildirim } from '../composables/useToastBildirim.js'

const toastBildirim = useToastBildirim()
const olaylar = ref([])
const aktifAy = ref(new Date())
const bugun = new Date()
const seciliGun = ref(null)

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

const gunuSec = (gun) => { seciliGun.value = gun }

const gunOlaylari = (gun) => olaylar.value.filter(o => ayniGun(new Date(o.tarih), gun))

const seciliOlaylar = computed(() => seciliGun.value ? gunOlaylari(seciliGun.value) : [])

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

onMounted(() => { seciliGun.value = new Date(); yukle() })
</script>

<style scoped>
.ajanda-sayfasi { padding: 0; }
.ajanda-baslik { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.ajanda-baslik h1 { margin: 0; }
.ay-gezinme { display: flex; align-items: center; gap: 8px; }
.ay-etiket { font-weight: 700; min-width: 120px; text-align: center; text-transform: capitalize; }
.takvim-grid { background: var(--bg-card); border: 1px solid var(--border); border-radius: 12px; overflow: hidden; }
.gun-basliklar { display: grid; grid-template-columns: repeat(7, 1fr); background: var(--bg-primary); }
.gun-baslik { padding: 8px; text-align: center; font-size: 12px; font-weight: 600; color: var(--text-secondary); }
.gun-hucreleri { display: grid; grid-template-columns: repeat(7, 1fr); }
.gun-hucre { min-height: 70px; border: 1px solid var(--border); padding: 6px; cursor: pointer; }
.gun-hucre.bos { background: rgba(0,0,0,0.02); cursor: default; }
.gun-hucre.bugun { background: rgba(59,130,246,0.08); }
.gun-hucre.secili { outline: 2px solid #3b82f6; }
.gun-no { font-size: 13px; font-weight: 600; }
.olay-noktalar { display: flex; gap: 4px; margin-top: 6px; }
.olay-nokta { width: 8px; height: 8px; border-radius: 50%; }
.olay-nokta.vade { background: #f59e0b; }
.olay-nokta.gorev { background: #3b82f6; }
.gun-detay { margin-top: 16px; background: var(--bg-card); border: 1px solid var(--border); border-radius: 12px; padding: 16px; }
.gun-detay h3 { margin: 0 0 12px; text-transform: capitalize; }
.bos { color: var(--text-muted); padding: 12px 0; }
.olay-karti { display: flex; align-items: center; gap: 12px; padding: 10px 0; border-bottom: 1px solid var(--border); }
.olay-karti:last-child { border-bottom: none; }
.olay-karti i { font-size: 18px; }
.vade-ikon { color: #f59e0b; }
.gorev-ikon { color: #3b82f6; }
.olay-karti div { flex: 1; }
.olay-karti strong { font-size: 14px; }
.olay-karti p { margin: 2px 0 0; font-size: 12px; color: var(--text-secondary); }
</style>
