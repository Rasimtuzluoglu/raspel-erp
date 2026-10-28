<template>
  <Drawer
    :visible="visible"
    position="right"
    :header="t('onaylar.detayBaslik')"
    :style="{ width: '520px' }"
    :breakpoints="{ '575px': '92vw' }"
    @update:visible="$emit('update:visible', $event)"
  >
    <div
      v-if="yukleniyor"
      class="detay-iskelet"
      aria-busy="true"
    >
      <SkeletonLoader
        :count="4"
        height="44px"
      />
    </div>

    <div
      v-else-if="kayit"
      class="onay-detay"
    >
      <div class="detay-ust">
        <div class="detay-baslik">
          <i :class="turIkonu" />
          <span>{{ kayit.baslik || '-' }}</span>
        </div>
        <div class="detay-rozetler">
          <Tag
            :value="turAdi"
            :severity="turSeverity"
          />
          <Tag
            :value="durumAdi"
            :severity="durumSeverity"
          />
        </div>
        <p
          v-if="kayit.altBaslik"
          class="detay-alt"
        >
          {{ kayit.altBaslik }}
        </p>
      </div>

      <div class="detay-grid">
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('onaylar.tutar') }}</span>
          <strong class="gizli-veri">{{ tutarMetni }}</strong>
        </div>
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('onaylar.tarih') }}</span>
          <span>{{ kayit.tarih ? formatDate(kayit.tarih) : '-' }}</span>
        </div>
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('onaylar.olusturmaTarihi') }}</span>
          <span>{{ kayit.olusturmaTarihi ? formatTarihSaat(kayit.olusturmaTarihi) : '-' }}</span>
        </div>
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('onaylar.kaynakKayit') }}</span>
          <span>#{{ kayit.kaynakId ?? '-' }}</span>
        </div>
        <div
          v-if="kayit.aciklama"
          class="detay-alan genis"
        >
          <span class="detay-etiket">{{ t('onaylar.detayAciklama') }}</span>
          <span>{{ kayit.aciklama }}</span>
        </div>
      </div>

      <section
        v-if="metaSatirlari.length"
        class="meta-bolum"
      >
        <h4>{{ t('onaylar.meta') }}</h4>
        <dl class="meta-liste">
          <template
            v-for="satir in metaSatirlari"
            :key="satir.anahtar"
          >
            <dt>{{ satir.etiket }}</dt>
            <dd>{{ satir.deger }}</dd>
          </template>
        </dl>
      </section>

      <RouterLink
        v-if="ilgiliRota"
        :to="ilgiliRota"
        class="ilgili-link"
      >
        <i class="pi pi-arrow-right" />
        {{ t('onaylar.ilgiliKaydaGit') }}
      </RouterLink>
      <p
        v-else
        class="muted"
      >
        {{ t('onaylar.ilgiliKayitYok') }}
      </p>
    </div>

    <template #footer>
      <div class="detay-aksiyonlar">
        <Button
          v-if="kayit && aksiyonGoster"
          :label="t('onaylar.onayla')"
          icon="pi pi-check"
          severity="success"
          @click="$emit('onayla', kayit)"
        />
        <Button
          v-if="kayit && aksiyonGoster"
          :label="t('onaylar.reddet')"
          icon="pi pi-times"
          severity="danger"
          outlined
          @click="$emit('reddet', kayit)"
        />
        <Button
          :label="t('onaylar.kapat')"
          icon="pi pi-times"
          text
          @click="$emit('update:visible', false)"
        />
      </div>
    </template>
  </Drawer>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { formatCurrency, formatDate, formatTarihSaat } from '../utils/format.js'

const props = defineProps({
  visible: { type: Boolean, default: false },
  kayit: { type: Object, default: null },
  yukleniyor: { type: Boolean, default: false }
})

defineEmits(['update:visible', 'onayla', 'reddet'])

const { t } = useI18n()

const TUR = {
  IZIN: { anahtar: 'turIzin', ikon: 'pi pi-calendar', severity: 'info' },
  MASRAF: { anahtar: 'turMasraf', ikon: 'pi pi-wallet', severity: 'success' },
  SATINALMA: { anahtar: 'turSatinalma', ikon: 'pi pi-shopping-bag', severity: 'warn' },
  SIPARIS: { anahtar: 'turSiparis', ikon: 'pi pi-receipt', severity: 'secondary' }
}

const DURUM_ANAHTAR = {
  BEKLEMEDE: 'durumBeklemede',
  BEKLIYOR: 'durumBekliyor',
  TEKLIF: 'durumTeklif',
  TASLAK: 'durumTaslak',
  ONAYLANDI: 'durumOnaylandi',
  REDDEDILDI: 'durumReddedildi',
  IPTAL: 'durumIptal',
  HAZIRLANIYOR: 'durumHazirlaniyor',
  TAMAMLANDI: 'durumTamamlandi'
}

const DURUM_SEVERITY = {
  BEKLEMEDE: 'warn',
  BEKLIYOR: 'warn',
  TEKLIF: 'warn',
  TASLAK: 'warn',
  ONAYLANDI: 'success',
  REDDEDILDI: 'danger',
  IPTAL: 'danger',
  HAZIRLANIYOR: 'info',
  TAMAMLANDI: 'success'
}

// Nihai durumdaki kayitlarda onay/red aksiyonu gosterilmez.
const KAPALI_DURUMLAR = ['ONAYLANDI', 'REDDEDILDI', 'IPTAL', 'TAMAMLANDI', 'TESLIM_EDILDI']

const ROTALAR = {
  IZIN: '/izinler',
  MASRAF: '/masraflar',
  SATINALMA: '/satinalma',
  SIPARIS: '/siparisler'
}

const META_ANAHTAR = {
  personel: 'metaPersonel',
  personelAdi: 'metaPersonel',
  departman: 'metaDepartman',
  cari: 'metaCari',
  cariHesap: 'metaCari',
  cariHesapAdi: 'metaCari',
  cariHesapId: 'metaCariHesapId',
  tedarikci: 'metaTedarikci',
  kategori: 'metaKategori',
  izinTuru: 'metaIzinTuru',
  gunSayisi: 'metaGunSayisi',
  bitisTarihi: 'metaBitisTarihi',
  avansMi: 'metaAvansMi',
  talepTarihi: 'metaTalepTarihi',
  talepEden: 'metaTalepEden',
  siparisTarihi: 'metaSiparisTarihi',
  siparisNo: 'metaSiparisNo',
  talepNo: 'metaTalepNo',
  sube: 'metaSube',
  donem: 'metaDonem',
  paraBirimi: 'metaParaBirimi'
}

const turAdi = computed(() =>
  props.kayit?.tur && TUR[props.kayit.tur] ? t(`onaylar.${TUR[props.kayit.tur].anahtar}`) : (props.kayit?.tur || '-')
)
const turIkonu = computed(() => TUR[props.kayit?.tur]?.ikon || 'pi pi-check-circle')
const turSeverity = computed(() => TUR[props.kayit?.tur]?.severity || 'secondary')
const durumAdi = computed(() => {
  const durum = props.kayit?.durum
  return durum && DURUM_ANAHTAR[durum] ? t(`onaylar.${DURUM_ANAHTAR[durum]}`) : (durum || '-')
})
const durumSeverity = computed(() => DURUM_SEVERITY[props.kayit?.durum] || 'secondary')
const aksiyonGoster = computed(() => !!props.kayit && !KAPALI_DURUMLAR.includes(props.kayit.durum))
const ilgiliRota = computed(() => ROTALAR[props.kayit?.tur] || null)
const tutarMetni = computed(() =>
  props.kayit?.tutar == null ? '-' : formatCurrency(props.kayit.tutar)
)

const metaEtiketi = (anahtar) => {
  const i18nAnahtari = META_ANAHTAR[anahtar]
  if (i18nAnahtari) return t(`onaylar.${i18nAnahtari}`)
  return String(anahtar)
    .replace(/[_-]+/g, ' ')
    .replace(/([a-z])([A-Z])/g, '$1 $2')
    .replace(/^./, (harf) => harf.toUpperCase())
}

// Meta değerleri türüne göre biçimlenir: tarih alanları gün olarak,
// boolean alanlar Evet/Hayır olarak gösterilir; aksi halde ham değer yazılır.
const metaDegeri = (anahtar, deger) => {
  if (typeof deger === 'boolean') {
    return deger ? t('onaylar.metaEvet') : t('onaylar.metaHayir')
  }
  if (/(Tarihi|Tarih)$/.test(String(anahtar)) && typeof deger === 'string' && /^\d{4}-\d{2}-\d{2}/.test(deger)) {
    return formatDate(deger)
  }
  return String(deger)
}

const metaSatirlari = computed(() => {
  const meta = props.kayit?.meta
  if (!meta) return []
  if (Array.isArray(meta)) {
    return meta.map((satir, index) => {
      const anahtar = satir?.anahtar ?? satir?.key ?? index
      return {
        anahtar,
        etiket: metaEtiketi(satir?.etiket ?? anahtar),
        deger: metaDegeri(anahtar, satir?.deger ?? satir?.value ?? '-')
      }
    })
  }
  if (typeof meta === 'object') {
    return Object.entries(meta)
      .filter(([, deger]) => deger !== null && deger !== undefined && deger !== '')
      .map(([anahtar, deger]) => ({
        anahtar,
        etiket: metaEtiketi(anahtar),
        deger: Array.isArray(deger) ? deger.join(', ') : metaDegeri(anahtar, deger)
      }))
  }
  return [{ anahtar: 'meta', etiket: t('onaylar.meta'), deger: String(meta) }]
})
</script>

<style scoped>
.onay-detay {
  display: flex;
  flex-direction: column;
  gap: 18px;
}
.detay-iskelet {
  color: var(--text-muted);
}
.detay-ust {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.detay-baslik {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 1.05rem;
  font-weight: 700;
  color: var(--text-primary);
  word-break: break-word;
}
.detay-baslik i {
  color: var(--accent);
  flex-shrink: 0;
}
.detay-rozetler {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}
.detay-alt {
  margin: 0;
  font-size: 13px;
  color: var(--text-secondary);
  overflow-wrap: anywhere;
}
.detay-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 12px;
}
.detay-alan {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}
.detay-alan.genis {
  grid-column: 1 / -1;
}
.detay-etiket {
  font-size: 11.5px;
  text-transform: uppercase;
  letter-spacing: 0.03em;
  color: var(--text-muted);
}
.meta-bolum h4 {
  margin: 0 0 8px;
  font-size: 14px;
  color: var(--text-primary);
}
.meta-liste {
  margin: 0;
  display: grid;
  grid-template-columns: minmax(110px, auto) 1fr;
  gap: 6px 12px;
  font-size: 13px;
}
.meta-liste dt {
  color: var(--text-muted);
}
.meta-liste dd {
  margin: 0;
  color: var(--text-primary);
  overflow-wrap: anywhere;
}
.ilgili-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: var(--accent);
  text-decoration: none;
  min-height: 44px;
}
.ilgili-link:hover {
  text-decoration: underline;
}
.muted {
  font-size: 12px;
  color: var(--text-muted);
}
.detay-aksiyonlar {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
@media (prefers-reduced-motion: reduce) {
  .ilgili-link {
    transition: none;
  }
}
</style>
