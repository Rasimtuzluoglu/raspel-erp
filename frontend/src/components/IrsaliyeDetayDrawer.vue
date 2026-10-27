<template>
  <Drawer
    :visible="visible"
    position="right"
    :header="t('irsaliyeler.detayBaslik')"
    :style="{ width: '560px' }"
    :breakpoints="{ '575px': '92vw' }"
    @update:visible="$emit('update:visible', $event)"
  >
    <div
      v-if="kayit"
      class="detay"
    >
      <div class="detay-ust">
        <div class="detay-baslik">
          <i class="pi pi-truck" />
          <span>{{ kayit.irsaliyeNo || '-' }}</span>
        </div>
        <div class="detay-rozetler">
          <Tag
            :value="durumAdi(kayit.durum)"
            :severity="durumSeverity(kayit.durum)"
          />
          <Tag
            :value="kayit.tur === 'ALIS' ? t('irsaliyeler.turAlis') : t('irsaliyeler.turSatis')"
            :severity="kayit.tur === 'ALIS' ? 'warn' : 'info'"
          />
          <Tag
            v-if="kayit.teslimatDurum"
            :value="teslimatDurumAdi(kayit.teslimatDurum)"
            :severity="teslimatDurumSeverity(kayit.teslimatDurum)"
          />
        </div>
      </div>

      <div
        v-if="kayit.faturaId || kayit.teslimatId || kayit.siparisNo"
        class="baglantilar"
      >
        <router-link
          v-if="kayit.faturaId"
          class="baglanti"
          :to="`/faturalar/${kayit.faturaId}`"
        >
          <i class="pi pi-file" />
          {{ t('irsaliyeler.faturaBaglantisi') }}:
          {{ kayit.faturaNumarasi || `#${kayit.faturaId}` }}
        </router-link>
        <router-link
          v-if="kayit.teslimatId"
          class="baglanti"
          to="/teslimatlar"
        >
          <i class="pi pi-map-marker" />
          {{ t('irsaliyeler.teslimatKaydi') }}: #{{ kayit.teslimatId }}
        </router-link>
        <span
          v-if="kayit.siparisNo"
          class="baglanti"
        >
          <i class="pi pi-shopping-cart" />
          {{ t('irsaliyeler.siparis') }}: {{ kayit.siparisNo }}
        </span>
      </div>

      <h4>{{ t('irsaliyeler.temelBilgiler') }}</h4>
      <div class="detay-grid">
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('common.date') }}</span>
          <span>{{ tarih(kayit.tarih) }}</span>
        </div>
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('irsaliyeler.cariHesap') }}</span>
          <span>{{ kayit.cariHesapAdi || '-' }}</span>
        </div>
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('irsaliyeler.tur') }}</span>
          <span>{{ kayit.tur === 'ALIS' ? t('irsaliyeler.turAlis') : t('irsaliyeler.turSatis') }}</span>
        </div>
        <div
          v-if="kayit.aciklama"
          class="detay-alan genis"
        >
          <span class="detay-etiket">{{ t('common.description') }}</span>
          <span>{{ kayit.aciklama }}</span>
        </div>
      </div>

      <h4>{{ t('irsaliyeler.sevkTeslimBilgileri') }}</h4>
      <div class="detay-grid">
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('irsaliyeler.plaka') }}</span>
          <span>{{ kayit.plaka || '-' }}</span>
        </div>
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('irsaliyeler.sofor') }}</span>
          <span>{{ kayit.soforAd || '-' }}</span>
        </div>
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('irsaliyeler.tasiyiciFirma') }}</span>
          <span>{{ kayit.tasiyiciFirma || '-' }}</span>
        </div>
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('irsaliyeler.sevkTarihi') }}</span>
          <span>{{ kayit.sevkTarihi ? tarihSaat(kayit.sevkTarihi) : t('irsaliyeler.sevkEdilmemis') }}</span>
        </div>
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('irsaliyeler.koliAdedi') }}</span>
          <span>{{ kayit.koliAdedi ?? '-' }}</span>
        </div>
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('irsaliyeler.desi') }}</span>
          <span>{{ kayit.desi ?? '-' }}</span>
        </div>
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('irsaliyeler.brutKg') }}</span>
          <span>{{ kayit.brutKg ?? '-' }}</span>
        </div>
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('irsaliyeler.teslimAlan') }}</span>
          <span>{{ kayit.teslimAlan || '-' }}</span>
        </div>
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('irsaliyeler.teslimTarihi') }}</span>
          <span>{{ kayit.teslimTarihi ? tarihSaat(kayit.teslimTarihi) : '-' }}</span>
        </div>
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('irsaliyeler.teslimatDurumu') }}</span>
          <span>{{ kayit.teslimatDurum ? teslimatDurumAdi(kayit.teslimatDurum) : '-' }}</span>
        </div>
        <div
          v-if="kayit.teslimatAdresi"
          class="detay-alan genis"
        >
          <span class="detay-etiket">{{ t('irsaliyeler.teslimatAdresi') }}</span>
          <span>{{ kayit.teslimatAdresi }}</span>
        </div>
      </div>

      <h4>{{ t('irsaliyeler.kalemler') }}</h4>
      <div class="kalem-sarmal">
        <table
          v-if="kalemler.length"
          class="kalem-tablo"
        >
          <thead>
            <tr>
              <th>{{ t('irsaliyeler.stok') }}</th>
              <th class="sag">
                {{ t('irsaliyeler.miktar') }}
              </th>
              <th>{{ t('irsaliyeler.birim') }}</th>
              <th>{{ t('common.description') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="(k, i) in kalemler"
              :key="k.id ?? i"
            >
              <td>{{ k.stokAdi || k.stokAd || `#${k.stokId}` }}</td>
              <td class="sag">
                {{ k.miktar }}
              </td>
              <td>{{ k.birim || '-' }}</td>
              <td>{{ k.aciklama || '-' }}</td>
            </tr>
          </tbody>
        </table>
        <p
          v-else
          class="muted"
        >
          {{ t('irsaliyeler.kalemYok') }}
        </p>
      </div>

      <h4>{{ t('irsaliyeler.durumZamanCizelgesi') }}</h4>
      <div
        v-if="gecmisYukleniyor"
        class="gecmis-yukleniyor"
        :aria-busy="true"
      >
        <SkeletonLoader
          :count="3"
          height="52px"
        />
      </div>
      <ol
        v-else-if="gecmisListesi.length"
        class="zaman-cizelgesi"
      >
        <li
          v-for="(log, i) in gecmisListesi"
          :key="log.id ?? i"
          class="zc-adim"
          :class="durumSinif(log.yeniDurum)"
        >
          <span class="zc-nokta">
            <i :class="durumIkon(log.yeniDurum)" />
          </span>
          <div class="zc-icerik">
            <div class="zc-baslik">
              <span>{{ durumAdi(log.yeniDurum) }}</span>
            </div>
            <small
              v-if="log.aciklama"
              class="zc-alt"
            >{{ log.aciklama }}</small>
            <small class="zc-alt">{{ tarihSaat(log.olusturmaTarihi) }}</small>
          </div>
        </li>
      </ol>
      <p
        v-else
        class="muted"
      >
        {{ t('common.noData') }}
      </p>
    </div>

    <template #footer>
      <div class="detay-aksiyonlar">
        <Button
          v-if="duzenlenebilir"
          :label="t('common.edit')"
          icon="pi pi-pencil"
          class="btn"
          text
          @click="$emit('duzenle', kayit)"
        />
        <Button
          v-if="kayit && kayit.durum === 'TASLAK'"
          :label="t('irsaliyeler.hazirla')"
          icon="pi pi-box"
          class="btn"
          @click="$emit('hazirla', kayit)"
        />
        <Button
          v-if="kayit && kayit.durum === 'HAZIRLANDI'"
          :label="t('irsaliyeler.sevkEt')"
          icon="pi pi-send"
          class="btn"
          @click="$emit('sevk', kayit)"
        />
        <Button
          v-if="kayit && kayit.durum === 'SEVK_EDILDI'"
          :label="t('irsaliyeler.teslimEt')"
          icon="pi pi-check"
          class="btn"
          severity="success"
          @click="$emit('teslim', kayit)"
        />
        <Button
          v-if="kayit && kayit.durum === 'SEVK_EDILDI'"
          :label="t('irsaliyeler.sevkiGeriAl')"
          icon="pi pi-undo"
          class="btn"
          outlined
          @click="$emit('sevki-geri-al', kayit)"
        />
        <Button
          v-if="faturalanabilir"
          :label="t('irsaliyeler.faturayaDonustur')"
          icon="pi pi-file"
          class="btn"
          severity="warn"
          outlined
          @click="$emit('faturaya-donustur', kayit)"
        />
        <Button
          v-if="kayit && kayit.durum !== 'IPTAL'"
          :label="t('irsaliyeler.iptalEt')"
          icon="pi pi-times"
          class="btn"
          text
          severity="danger"
          @click="$emit('iptal', kayit)"
        />
      </div>
    </template>
  </Drawer>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { formatDate as tarih, formatDateTime as tarihSaat } from '../utils/format.js'

const props = defineProps({
  visible: { type: Boolean, default: false },
  kayit: { type: Object, default: null },
  gecmis: { type: Array, default: () => [] },
  gecmisYukleniyor: { type: Boolean, default: false }
})

defineEmits([
  'update:visible',
  'duzenle',
  'hazirla',
  'sevk',
  'teslim',
  'sevki-geri-al',
  'iptal',
  'faturaya-donustur'
])

const { t } = useI18n()

const kalemler = computed(() => props.kayit?.kalemler || [])
const gecmisListesi = computed(() => {
  if (props.gecmis?.length) return props.gecmis
  return props.kayit?.gecmis || []
})
const duzenlenebilir = computed(
  () => props.kayit && ['TASLAK', 'HAZIRLANDI'].includes(props.kayit.durum)
)
const faturalanabilir = computed(
  () => props.kayit && props.kayit.durum !== 'IPTAL' && !props.kayit.faturaId
)

const durumAdi = (d) =>
  ({
    TASLAK: t('irsaliyeler.durumTaslak'),
    HAZIRLANDI: t('irsaliyeler.durumHazirlandi'),
    SEVK_EDILDI: t('irsaliyeler.durumSevkEdildi'),
    KESILDI: t('irsaliyeler.durumSevkEdildi'),
    TESLIM_EDILDI: t('irsaliyeler.durumTeslimEdildi'),
    IPTAL: t('irsaliyeler.durumIptal')
  })[d] || d || '-'

const durumSeverity = (d) =>
  ({
    TASLAK: 'secondary',
    HAZIRLANDI: 'warn',
    SEVK_EDILDI: 'info',
    KESILDI: 'info',
    TESLIM_EDILDI: 'success',
    IPTAL: 'danger'
  })[d] || 'secondary'

const teslimatDurumAdi = (d) =>
  ({
    HAZIRLANIYOR: t('irsaliyeler.durumHazirlandi'),
    YOLDA: t('irsaliyeler.durumSevkEdildi'),
    TESLIM_EDILDI: t('irsaliyeler.durumTeslimEdildi'),
    IPTAL: t('irsaliyeler.durumIptal')
  })[d] || d || '-'

const teslimatDurumSeverity = (d) =>
  ({ HAZIRLANIYOR: 'warn', YOLDA: 'info', TESLIM_EDILDI: 'success', IPTAL: 'danger' })[d] || 'secondary'

const durumSinif = (d) =>
  ({
    TASLAK: 'taslak',
    HAZIRLANDI: 'uretimde',
    SEVK_EDILDI: 'uretimde',
    KESILDI: 'uretimde',
    TESLIM_EDILDI: 'tamam',
    IPTAL: 'iptal'
  })[d] || 'tamam'

const durumIkon = (d) =>
  ({
    TASLAK: 'pi pi-file-edit',
    HAZIRLANDI: 'pi pi-box',
    SEVK_EDILDI: 'pi pi-send',
    KESILDI: 'pi pi-send',
    TESLIM_EDILDI: 'pi pi-check',
    IPTAL: 'pi pi-times'
  })[d] || 'pi pi-check'
</script>

<style scoped>
.detay {
  display: flex;
  flex-direction: column;
  gap: 16px;
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
}
.detay-rozetler {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}
.baglantilar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.baglanti {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  border: 1px solid var(--border);
  border-radius: 999px;
  background: var(--bg-card);
  color: var(--text-secondary);
  font-size: 12px;
  text-decoration: none;
}
.baglanti:hover {
  color: var(--accent);
  border-color: var(--accent-border, var(--accent));
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
  overflow-wrap: anywhere;
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
h4 {
  margin: 0;
  font-size: 15px;
}
.kalem-sarmal {
  overflow-x: auto;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: var(--bg-card);
}
.kalem-tablo {
  width: 100%;
  border-collapse: collapse;
  font-size: 12.5px;
}
.kalem-tablo th,
.kalem-tablo td {
  padding: 8px 10px;
  border-bottom: 1px solid var(--border);
  text-align: left;
  white-space: nowrap;
}
.kalem-tablo th {
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 0.03em;
  color: var(--text-muted);
  background: var(--bg-muted);
}
.kalem-tablo tbody tr:last-child td {
  border-bottom: none;
}
.kalem-tablo .sag {
  text-align: right;
}
.zaman-cizelgesi {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.zc-adim {
  display: flex;
  gap: 12px;
  padding: 8px 0;
  position: relative;
}
.zc-adim:not(:last-child)::before {
  content: '';
  position: absolute;
  left: 15px;
  top: 34px;
  bottom: -4px;
  width: 2px;
  background: var(--border);
}
.zc-nokta {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--success-soft, rgba(16, 185, 129, 0.15));
  border: 1px solid var(--success, #10b981);
  color: var(--success, #10b981);
  flex-shrink: 0;
  z-index: 1;
}
.zc-adim.taslak .zc-nokta {
  background: var(--bg-muted);
  border-color: var(--border);
  color: var(--text-muted);
}
.zc-adim.uretimde .zc-nokta {
  background: rgba(59, 130, 246, 0.15);
  border-color: #3b82f6;
  color: #3b82f6;
}
.zc-adim.iptal .zc-nokta {
  background: rgba(239, 68, 68, 0.12);
  border-color: #ef4444;
  color: #ef4444;
}
.zc-icerik {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
  flex: 1;
}
.zc-baslik {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--text-primary);
}
.zc-alt {
  font-size: 0.75rem;
  color: var(--text-muted);
}
.gecmis-yukleniyor {
  color: var(--text-muted);
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
</style>
