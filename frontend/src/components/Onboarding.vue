<template>
  <div class="onboarding">
    <div class="onboard-kart">
      <div class="onboard-ust">
        <i class="pi pi-rocket onboard-ikon" />
        <h2>RasPel ERP'ye Hoş Geldiniz!</h2>
        <p>Sisteminiz şu anda boş görünüyor. Başlamak için birkaç adım:</p>
      </div>

      <div class="onboard-adimlar">
        <div
          v-for="(adim, i) in adimlar"
          :key="i"
          class="adim"
          :class="{ tamamlandi: adim.tamam }"
        >
          <span
            class="adim-no"
            :class="{ 'adim-yesil': adim.tamam }"
          >
            <i
              v-if="adim.tamam"
              class="pi pi-check"
            />
            <template v-else>{{ i + 1 }}</template>
          </span>
          <div class="adim-icerik">
            <strong>{{ adim.baslik }}</strong>
            <p>{{ adim.aciklama }}</p>
            <Button
              :label="adim.buton"
              :icon="adim.ikon"
              class="p-button-sm p-button-outlined"
              @click="adimTikla(adim)"
            />
          </div>
        </div>
      </div>

      <div class="onboard-alt">
        <div class="demo-secim">
          <InputSwitch v-model="demoVeriIsteniyor" />
          <span>Demo verileriyle doldurulsun</span>
        </div>
        <Button
          label="Demo Veriyi Yükle"
          icon="pi pi-download"
          class="p-button-success p-button-lg"
          :loading="demoYukleniyor"
          @click="demoYukle"
        />
        <a
          class="onboard-atla"
          @click="atla"
        >Atla, kendim kuracağım</a>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useToast } from 'primevue/usetoast'
import { useToastBildirim } from '../composables/useToastBildirim.js'

const router = useRouter()
const toast = useToast()
const toastBildirim = useToastBildirim()
const demoVeriIsteniyor = ref(true)
const demoYukleniyor = ref(false)
const emit = defineEmits(['demo-loaded'])

const adimlar = reactive([
  {
    baslik: 'Şirket Bilgileri',
    aciklama: 'Şirket adı, vergi no ve iletişim bilgilerinizi düzenleyin.',
    buton: 'Şirkete Git',
    ikon: 'pi pi-building',
    tamam: false,
    path: '/sirketler'
  },
  {
    baslik: 'İlk Cari Hesabını Ekleyin',
    aciklama: 'Müşteri ve tedarikçilerinizi sisteme ekleyin.',
    buton: 'Cari Ekle',
    ikon: 'pi pi-users',
    tamam: false,
    path: '/cari-hesaplar'
  },
  {
    baslik: 'Ürünlerinizi Tanımlayın',
    aciklama: 'Stok ürünlerinizi, fiyatlarını ve birimlerini ekleyin.',
    buton: 'Stok Ekle',
    ikon: 'pi pi-box',
    tamam: false,
    path: '/stoklar'
  },
  {
    baslik: 'İlk Faturanızı Kesin',
    aciklama: 'Satış veya alış faturanızı oluşturun.',
    buton: 'Fatura Kes',
    ikon: 'pi pi-file',
    tamam: false,
    path: '/faturalar'
  }
])

const adimTikla = (adim) => {
  router.push(adim.path)
}

const atla = () => {
  router.push('/')
}

const demoYukle = async () => {
  demoYukleniyor.value = true
  try {
    const { cariHesapAPI, stokAPI, kategoriAPI } = await import('../api/index.js')

    await kategoriAPI.create({ ad: 'Mobilya', tur: 'GIDER' })
    await kategoriAPI.create({ ad: 'Satış', tur: 'GELIR' })

    const ornekCariler = [
      {
        ad: 'Demo Müşteri A',
        vergiNo: '1111111111',
        telefon: '0532 111 11 11',
        email: 'musteria@demo.com',
        il: 'İstanbul'
      },
      {
        ad: 'Demo Müşteri B',
        vergiNo: '2222222222',
        telefon: '0532 222 22 22',
        email: 'musterib@demo.com',
        il: 'Ankara'
      },
      {
        ad: 'Demo Tedarikçi',
        vergiNo: '3333333333',
        telefon: '0532 333 33 33',
        email: 'tedarikci@demo.com',
        il: 'İzmir'
      }
    ]
    for (const c of ornekCariler) await cariHesapAPI.create(c)

    const ornekStoklar = [
      {
        ad: 'MDF 18mm',
        stokKodu: 'MDF-18',
        barkod: '8690001',
        birim: 'Adet',
        fiyat: 850,
        satisFiyati: 1050,
        miktar: 100,
        minMiktar: 10
      },
      {
        ad: 'PVC Bant Beyaz',
        stokKodu: 'PVC-B01',
        barkod: '8690002',
        birim: 'Rulo',
        fiyat: 45,
        satisFiyati: 75,
        miktar: 200,
        minMiktar: 20
      },
      {
        ad: 'Sunta 16mm',
        stokKodu: 'SUNTA-16',
        barkod: '8690003',
        birim: 'Adet',
        fiyat: 520,
        satisFiyati: 690,
        miktar: 80,
        minMiktar: 10
      }
    ]
    for (const s of ornekStoklar) await stokAPI.create(s)

    toast.add({
      severity: 'success',
      summary: 'Demo Veri Yüklendi',
      detail: 'Örnek cari, stok ve kategoriler eklendi.',
      life: 5000
    })
    adimlar.forEach((a) => {
      a.tamam = true
    })
    emit('demo-loaded')
  } catch (e) {
    toastBildirim.hata('Demo veri yüklenirken hata oluştu.')
  } finally {
    demoYukleniyor.value = false
  }
}
</script>

<style scoped>
.onboarding {
  display: flex;
  justify-content: center;
  padding: 2rem 0;
}
.onboard-kart {
  max-width: 560px;
  width: 100%;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 16px;
  padding: 2.5rem;
  box-shadow: var(--shadow);
}
.onboard-ust {
  text-align: center;
  margin-bottom: 2rem;
}
.onboard-ikon {
  font-size: 3rem;
  color: #3b82f6;
  margin-bottom: 0.5rem;
}
.onboard-ust h2 {
  margin: 0 0 0.5rem;
  font-size: 1.4rem;
}
.onboard-ust p {
  color: var(--text-secondary);
  font-size: 0.9rem;
  margin: 0;
}
.onboard-adimlar {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  margin-bottom: 2rem;
}
.adim {
  display: flex;
  gap: 1rem;
  align-items: flex-start;
}
.adim-no {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(59, 130, 246, 0.15);
  color: #60a5fa;
  font-weight: 700;
  font-size: 14px;
}
.adim-no.adim-yesil {
  background: rgba(34, 197, 94, 0.15);
  color: #4ade80;
}
.adim-icerik {
  flex: 1;
}
.adim-icerik strong {
  font-size: 0.95rem;
}
.adim-icerik p {
  font-size: 0.85rem;
  color: var(--text-secondary);
  margin: 2px 0 8px;
}
.onboard-alt {
  border-top: 1px solid var(--border);
  padding-top: 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
  align-items: center;
}
.demo-secim {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 0.85rem;
  color: var(--text-secondary);
}
.p-button-lg {
  font-size: 1.05rem;
  padding: 0.7rem 1.8rem;
}
.onboard-atla {
  font-size: 0.8rem;
  color: var(--text-secondary);
  text-decoration: underline;
  cursor: pointer;
  margin-top: 4px;
}
.onboard-atla:hover {
  color: var(--text-primary);
}
</style>
