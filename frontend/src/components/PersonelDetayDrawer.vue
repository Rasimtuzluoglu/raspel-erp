<template>
  <Drawer
    :visible="visible"
    position="right"
    :header="baslik"
    :style="{ width: '520px', maxWidth: '96vw' }"
    @update:visible="$emit('update:visible', $event)"
  >
    <div
      v-if="yukleniyor && !detay"
      class="detay-yukleniyor"
      role="status"
      :aria-label="t('common.loading')"
    >
      <SkeletonLoader
        :count="5"
        height="64px"
      />
    </div>

    <div
      v-else-if="detay && detay.personel"
      class="detay"
    >
      <div class="detay-ust">
        <div class="detay-baslik">
          <i class="pi pi-user" />
          <span>{{ detay.personel.ad }} {{ detay.personel.soyad }}</span>
        </div>
        <div class="detay-taglar">
          <Tag
            :value="detay.personel.aktif ? t('personel.aktif') : t('personel.pasif')"
            :severity="detay.personel.aktif ? 'success' : 'danger'"
          />
          <Tag
            v-if="detay.bugunIzinli"
            :value="t('personelDetay.bugunIzinli')"
            severity="warn"
          />
        </div>
      </div>

      <div class="detay-grid">
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('personel.departman') }}</span>
          <span>{{ detay.personel.departman || '-' }}</span>
        </div>
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('personel.pozisyon') }}</span>
          <span>{{ detay.personel.pozisyon || '-' }}</span>
        </div>
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('personel.rol') }}</span>
          <span>{{ rolEtiket(detay.personel.rol) }}</span>
        </div>
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('personel.tcKimlik') }}</span>
          <span>{{ detay.personel.tcKimlik || '-' }}</span>
        </div>
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('personel.telefon') }}</span>
          <span>{{ detay.personel.telefon || '-' }}</span>
        </div>
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('personel.eposta') }}</span>
          <span class="detay-email">{{ detay.personel.email || '-' }}</span>
        </div>
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('personel.dogumTarihi') }}</span>
          <span>{{ tarih(detay.personel.dogumTarihi) }}</span>
        </div>
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('personel.iseGiris') }}</span>
          <span>{{ tarih(detay.personel.iseGirisTarihi) }}</span>
        </div>
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('personel.cikisTarihi') }}</span>
          <span>{{ tarih(detay.personel.cikisTarihi) }}</span>
        </div>
        <div
          v-if="detay.personel.maas !== null && detay.personel.maas !== undefined"
          class="detay-alan"
        >
          <span class="detay-etiket">{{ t('personel.maas') }}</span>
          <strong>{{ para(detay.personel.maas) }}</strong>
        </div>
        <div class="detay-alan genis">
          <span class="detay-etiket">{{ t('personel.adres') }}</span>
          <span>{{ detay.personel.adres || '-' }}</span>
        </div>
      </div>

      <div class="detay-bolum">
        <div class="bolum-baslik">
          <span>{{ t('personelDetay.puantajOzet') }}</span>
          <Tag
            :value="t('personelDetay.son30Gun')"
            severity="secondary"
          />
        </div>
        <div class="puantaj-kutular">
          <div class="pk pk-geldi">
            <strong>{{ detay.geldi }}</strong>
            <span>{{ t('puantaj.durumGeldi') }}</span>
          </div>
          <div class="pk pk-gelmedi">
            <strong>{{ detay.gelmedi }}</strong>
            <span>{{ t('puantaj.durumGelmedi') }}</span>
          </div>
          <div class="pk pk-izinli">
            <strong>{{ detay.izinli }}</strong>
            <span>{{ t('puantaj.durumIzinli') }}</span>
          </div>
          <div class="pk pk-mazeretli">
            <strong>{{ detay.mazeretli }}</strong>
            <span>{{ t('puantaj.durumMazeretli') }}</span>
          </div>
        </div>
        <div class="puantaj-alt">
          {{ t('personelDetay.toplamKayit', { sayi: detay.puantajKayitSayisi }) }}
          <span v-if="detay.bekleyenIzin"> · {{ t('personelDetay.bekleyenIzinSayisi', { sayi: detay.bekleyenIzin }) }}</span>
        </div>
      </div>

      <div class="detay-bolum">
        <div class="bolum-baslik">
          <span>{{ t('personelDetay.sonIzinler') }}</span>
          <Tag
            :value="String(detay.sonIzinler?.length || 0)"
            severity="secondary"
          />
        </div>
        <ul
          v-if="detay.sonIzinler?.length"
          class="izin-liste"
          :aria-label="t('personelDetay.sonIzinler')"
        >
          <li
            v-for="i in detay.sonIzinler"
            :key="i.id"
            class="izin-satir"
          >
            <div class="izin-sol">
              <strong>{{ izinTuruEtiket(i.izinTuru) }}</strong>
              <span class="izin-tarih">{{ tarih(i.baslangic) }} – {{ tarih(i.bitis) }} · {{ t('personel.gun') }}: {{ i.gunSayisi }}</span>
            </div>
            <Tag
              :value="durumEtiket(i.durum)"
              :severity="i.durum === 'ONAYLANDI' ? 'success' : i.durum === 'REDDEDILDI' ? 'danger' : 'warn'"
            />
          </li>
        </ul>
        <div
          v-else
          class="bolum-bos"
        >
          {{ t('personelDetay.izinYok') }}
        </div>
      </div>
    </div>

    <template #footer>
      <div class="detay-aksiyonlar">
        <Button
          v-if="detay?.personel && duzenlenebilir"
          :label="t('common.edit')"
          icon="pi pi-pencil"
          @click="$emit('duzenle', detay.personel)"
        />
        <Button
          v-if="detay?.personel && duzenlenebilir"
          :label="t('personel.izinEkle')"
          icon="pi pi-calendar-plus"
          class="p-button-outlined"
          @click="$emit('izin-ekle', detay.personel)"
        />
      </div>
    </template>
  </Drawer>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import SkeletonLoader from './SkeletonLoader.vue'
import { formatTarih as tarih } from '../utils/format.js'

const props = defineProps({
  visible: { type: Boolean, default: false },
  detay: { type: Object, default: null },
  yukleniyor: { type: Boolean, default: false },
  duzenlenebilir: { type: Boolean, default: false }
})

defineEmits(['update:visible', 'duzenle', 'izin-ekle'])

const { t } = useI18n()

const baslik = computed(() => {
  const p = props.detay?.personel
  return p ? `${p.ad} ${p.soyad}` : t('personel.detay')
})

const rolEtiket = (rol) => ({
  SOFOR: t('personel.rolSofor'),
  DEPOCU: t('personel.rolDepocu'),
  DIGER: t('personel.rolDiger')
}[rol] || (rol || '-'))

const izinTuruEtiket = (kod) => ({
  YILLIK_IZIN: t('personel.izinYillik'),
  HASTA_IZNI: t('personel.izinHasta'),
  MAZERET_IZNI: t('personel.izinMazeret'),
  DOGUM_IZNI: t('personel.izinDogum'),
  BABALIK_IZNI: t('personel.izinBabalik'),
  EVLILIK_IZNI: t('personel.izinEvlilik'),
  UCRETSIZ_IZIN: t('personel.izinUcretsiz')
}[kod] || kod)

const durumEtiket = (durum) => ({
  BEKLEMEDE: t('personel.durumBeklemede'),
  ONAYLANDI: t('personel.durumOnaylandi'),
  REDDEDILDI: t('personel.durumReddedildi')
}[durum] || durum)

const para = (v) => Number(v).toLocaleString('tr-TR', { style: 'currency', currency: 'TRY' })
</script>

<style scoped>
.detay {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.detay-yukleniyor {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.detay-ust {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
}
.detay-baslik {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 1.05rem;
  font-weight: 700;
  color: var(--text-primary);
}
.detay-baslik i {
  color: var(--accent);
}
.detay-taglar {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}
.detay-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 12px;
}
.detay-alan {
  display: flex;
  flex-direction: column;
  gap: 4px;
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
.detay-email {
  overflow-wrap: anywhere;
}
.detay-bolum {
  border-top: 1px solid var(--border);
  padding-top: 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.bolum-baslik {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12.5px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.03em;
  color: var(--text-secondary);
}
.puantaj-kutular {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(90px, 1fr));
  gap: 8px;
}
.pk {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 8px 10px;
  border: 1px solid var(--border);
  border-radius: var(--radius-md, 10px);
  background: var(--bg-muted);
}
.pk strong {
  font-size: 1.05rem;
  color: var(--text-primary);
}
.pk span {
  font-size: 11.5px;
  color: var(--text-muted);
}
.pk-geldi {
  border-color: rgba(16, 185, 129, 0.35);
}
.pk-gelmedi {
  border-color: rgba(239, 68, 68, 0.35);
}
.pk-izinli {
  border-color: rgba(245, 158, 11, 0.35);
}
.puantaj-alt {
  font-size: 12px;
  color: var(--text-muted);
}
.izin-liste {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.izin-satir {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 8px 10px;
  border: 1px solid var(--border);
  border-radius: var(--radius-md, 10px);
  background: var(--bg-muted);
}
.izin-sol {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}
.izin-tarih {
  font-size: 11.5px;
  color: var(--text-muted);
}
.bolum-bos {
  padding: 12px;
  text-align: center;
  font-size: 13px;
  color: var(--text-muted);
}
.detay-aksiyonlar {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
</style>
