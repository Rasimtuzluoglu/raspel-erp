<template>
  <div class="iletisim-page">
    <PageHeader
      :title="t('iletisim.title')"
      :subtitle="t('iletisim.subtitle')"
    />

    <div class="iletisim-grid">
      <Card class="iletisim-cari-kart">
        <template #content>
          <div class="form-group">
            <label>{{ t('iletisim.cari') }}</label>
            <AutoComplete
              v-model="seciliCari"
              :suggestions="oneriler"
              option-label="ad"
              class="w-full"
              :placeholder="t('iletisim.cariSec')"
              :force-selection="false"
              @complete="ara"
            >
              <template #option="slotProps">
                <div class="cari-opsiyon">
                  <span class="cari-opsiyon-ad">{{ slotProps.option.ad }}</span>
                  <span class="cari-opsiyon-alt">
                    {{ slotProps.option.telefon || slotProps.option.email || slotProps.option.vergiNumarasi || '' }}
                  </span>
                </div>
              </template>
            </AutoComplete>
          </div>

          <div
            v-if="seciliCari"
            class="cari-ozet"
          >
            <div class="cari-baslik">
              <span class="cari-avatar">{{ basHarfler }}</span>
              <div class="cari-baslik-metin">
                <strong>{{ seciliCari.ad }}</strong>
                <small v-if="seciliCari.vergiNumarasi">{{ seciliCari.vergiNumarasi }}</small>
              </div>
            </div>
            <div class="cari-bilgi">
              <div class="cari-bilgi-satir">
                <i class="pi pi-phone" />
                <span :class="{ 'veri-yok': !telefon }">{{ telefon || t('iletisim.telefonTanimsiz') }}</span>
              </div>
              <div class="cari-bilgi-satir">
                <i class="pi pi-envelope" />
                <span :class="{ 'veri-yok': !seciliCari.email }">{{ seciliCari.email || t('iletisim.emailTanimsiz') }}</span>
              </div>
              <div
                v-if="seciliCari.adres || seciliCari.il"
                class="cari-bilgi-satir"
              >
                <i class="pi pi-map-marker" />
                <span>{{ [seciliCari.adres, seciliCari.il].filter(Boolean).join(', ') }}</span>
              </div>
            </div>
          </div>
          <div
            v-else
            class="cari-bos"
          >
            <i class="pi pi-user" />
            <span>{{ t('iletisim.cariSeciniz') }}</span>
          </div>
        </template>
      </Card>

      <Card class="iletisim-mesaj-kart">
        <template #content>
          <div class="sablon-alani">
            <small class="sablon-baslik">{{ t('iletisim.hazirSablon') }}</small>
            <div class="sablon-cipler">
              <button
                v-for="s in sablonlar"
                :key="s.etiket"
                type="button"
                class="sablon-cip"
                @click="sablonUygula(s)"
              >
                {{ s.etiket }}
              </button>
            </div>
          </div>

          <div class="form-group">
            <label>{{ t('iletisim.konu') }}</label>
            <InputText
              v-model="konu"
              class="w-full"
              :placeholder="t('iletisim.konuPlaceholder')"
            />
          </div>
          <div class="form-group">
            <label>{{ t('iletisim.mesaj') }}</label>
            <Textarea
              v-model="mesaj"
              rows="6"
              class="w-full"
              :placeholder="t('iletisim.mesajPlaceholder')"
            />
            <small class="karakter-sayaci">{{ t('iletisim.karakter', { n: (mesaj || '').length }) }}</small>
          </div>

          <div class="iletisim-eylemler">
            <Button
              :label="t('iletisim.mailGonder')"
              icon="pi pi-envelope"
              :disabled="!seciliCari || !mesaj || !seciliCari.email"
              :loading="gonderiliyor"
              @click="mailGonder"
            />
            <Button
              :label="t('iletisim.whatsapp')"
              icon="pi pi-whatsapp"
              class="p-button-outlined"
              :disabled="!seciliCari || !whatsappMetni || !telefon"
              :loading="waYukleniyor"
              @click="whatsappAc"
            />
          </div>
          <small class="ipucu">{{ t('iletisim.ipucu') }}</small>
        </template>
      </Card>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useCariOnerileri } from '../composables/useCariOnerileri.js'
import { iletisimAPI } from '../api/index.js'

const { t } = useI18n()
const toastBildirim = useToastBildirim()
const { oneriler, ara } = useCariOnerileri()

const seciliCari = ref(null)
const konu = ref('')
const mesaj = ref('')
const gonderiliyor = ref(false)
const waYukleniyor = ref(false)

// Cari kartinda gosterilecek telefon: once kayitli telefon, yoksa yetkili telefon.
const telefon = computed(() => {
  const c = seciliCari.value
  if (!c) return ''
  return (c.telefon && c.telefon.trim()) || (c.yetkiliTelefon && c.yetkiliTelefon.trim()) || ''
})

const basHarfler = computed(() => {
  const ad = (seciliCari.value?.ad || '').trim()
  if (!ad) return '?'
  return ad
    .split(/\s+/)
    .slice(0, 2)
    .map((k) => k.charAt(0).toUpperCase())
    .join('')
})

// WhatsApp'a gonderilecek metin. KOK NEDEN DUZELTMESI: once yalnizca cariId
// gonderiliyordu ve backend ciplak `https://wa.me/<no>` donuyordu; konu/mesaj
// hic eklenmedigi icin WhatsApp BOS mesaj kutusu aciyordu.
const whatsappMetni = computed(() => {
  const k = (konu.value || '').trim()
  const m = (mesaj.value || '').trim()
  return [k ? `*${k}*` : '', m].filter(Boolean).join('\n\n')
})

const sablonlar = computed(() => [
  {
    etiket: t('iletisim.sablonOdeme'),
    konu: t('iletisim.sablonOdemeKonu'),
    mesaj: t('iletisim.sablonOdemeMesaj', { ad: seciliCari.value?.ad || '' })
  },
  {
    etiket: t('iletisim.sablonBilgi'),
    konu: t('iletisim.sablonBilgiKonu'),
    mesaj: t('iletisim.sablonBilgiMesaj', { ad: seciliCari.value?.ad || '' })
  },
  {
    etiket: t('iletisim.sablonTesekkur'),
    konu: t('iletisim.sablonTesekkurKonu'),
    mesaj: t('iletisim.sablonTesekkurMesaj', { ad: seciliCari.value?.ad || '' })
  }
])

const sablonUygula = (s) => {
  konu.value = s.konu
  mesaj.value = s.mesaj
}

const mailGonder = async () => {
  if (!seciliCari.value || !mesaj.value) return
  gonderiliyor.value = true
  try {
    await iletisimAPI.mailGonder({
      cariHesapId: seciliCari.value.id,
      konu: konu.value,
      mesaj: mesaj.value
    })
    toastBildirim.basarili(t('iletisim.gonderildi'))
    mesaj.value = ''
    konu.value = ''
  } catch (e) {
    toastBildirim.hata(e?.response?.data?.message || t('common.error'))
  } finally {
    gonderiliyor.value = false
  }
}

const whatsappAc = async () => {
  if (!seciliCari.value) return
  waYukleniyor.value = true
  try {
    const r = await iletisimAPI.whatsapp(seciliCari.value.id)
    const link = r?.data?.link
    if (link) {
      // Konu + mesaji `?text=` olarak ekle (wa.me destekli format).
      const metin = whatsappMetni.value
      const url = metin ? `${link}${link.includes('?') ? '&' : '?'}text=${encodeURIComponent(metin)}` : link
      window.open(url, '_blank', 'noopener')
    }
  } catch (e) {
    toastBildirim.hata(e?.response?.data?.message || t('common.error'))
  } finally {
    waYukleniyor.value = false
  }
}
</script>

<style scoped>
.iletisim-page {
  padding: 0;
}
.iletisim-grid {
  display: grid;
  grid-template-columns: minmax(280px, 360px) minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}
.form-group {
  margin-bottom: 1rem;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.w-full {
  width: 100% !important;
}

/* Cari kartı */
.cari-ozet {
  margin-top: 5px;
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 14px;
  background: color-mix(in srgb, var(--accent) 4%, transparent);
}
.cari-baslik {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}
.cari-avatar {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 15px;
  color: var(--accent-contrast, #04211d);
  background: linear-gradient(135deg, var(--accent), var(--accent-hover));
  flex-shrink: 0;
}
.cari-baslik-metin {
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.cari-baslik-metin strong {
  font-size: 14px;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.cari-baslik-metin small {
  color: var(--text-muted);
  font-size: 11.5px;
}
.cari-bilgi {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.cari-bilgi-satir {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12.5px;
  color: var(--text-secondary);
}
.cari-bilgi-satir i {
  color: var(--accent);
  font-size: 13px;
  flex-shrink: 0;
}
.veri-yok {
  color: var(--text-muted);
  font-style: italic;
}
.cari-bos {
  margin-top: 5px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 28px 16px;
  border: 1px dashed var(--border);
  border-radius: 12px;
  color: var(--text-muted);
  font-size: 12.5px;
}
.cari-bos i {
  font-size: 22px;
}

/* Öneri satırı */
.cari-opsiyon {
  display: flex;
  flex-direction: column;
  line-height: 1.3;
}
.cari-opsiyon-ad {
  font-weight: 600;
}
.cari-opsiyon-alt {
  font-size: 11.5px;
  color: var(--text-muted);
}

/* Mesaj kartı */
.sablon-alani {
  margin-bottom: 12px;
}
.sablon-baslik {
  display: block;
  color: var(--text-muted);
  margin-bottom: 6px;
}
.sablon-cipler {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.sablon-cip {
  border: 1px solid var(--border);
  background: transparent;
  color: var(--text-secondary);
  border-radius: var(--radius-pill, 999px);
  padding: 5px 12px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.15s ease;
}
.sablon-cip:hover {
  border-color: var(--accent);
  color: var(--accent);
  background: var(--accent-soft);
}
.karakter-sayaci {
  align-self: flex-end;
  color: var(--text-muted);
  font-size: 11px;
}
.iletisim-eylemler {
  display: flex;
  gap: 12px;
  margin-top: 8px;
}
.ipucu {
  display: block;
  margin-top: 12px;
  color: var(--text-muted);
}

@media (max-width: 820px) {
  .iletisim-grid {
    grid-template-columns: 1fr;
  }
}
</style>
