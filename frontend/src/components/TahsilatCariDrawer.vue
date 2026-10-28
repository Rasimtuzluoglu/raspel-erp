<template>
  <Drawer
    :visible="visible"
    position="right"
    :header="t('tahsilat.cariDetayBaslik')"
    :style="{ width: '460px' }"
    :breakpoints="{ '575px': '92vw' }"
    @update:visible="$emit('update:visible', $event)"
  >
    <div
      v-if="cari"
      class="cari-detay"
    >
      <div class="detay-ust">
        <div class="detay-baslik">
          <i class="pi pi-user" />
          <span>{{ cari.cariAd }}</span>
        </div>
        <div class="detay-rozetler">
          <Tag
            v-if="cari.aralik"
            :value="cari.aralik"
            :severity="aralikSeverity(cari.aralik)"
          />
          <Tag
            v-if="Number(cari.gecikmisAlacak) > 0"
            :value="t('tahsilat.gecikmis')"
            severity="danger"
          />
        </div>
      </div>

      <div class="detay-grid">
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('tahsilat.toplamAlacak') }}</span>
          <strong class="gizli-veri">{{ formatCurrency(cari.toplamAlacak) }}</strong>
        </div>
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('tahsilat.vadesiGecmis') }}</span>
          <strong
            class="gizli-veri"
            :class="Number(cari.gecikmisAlacak) > 0 ? 'negatif' : ''"
          >{{ formatCurrency(cari.gecikmisAlacak) }}</strong>
        </div>
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('tahsilat.yasAraligi') }}</span>
          <span>{{ cari.aralik || '-' }}</span>
        </div>
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('tahsilat.acikFaturaSayisi') }}</span>
          <span>{{ cari.faturaSayisi ?? (cari.faturalar || []).length }}</span>
        </div>
      </div>

      <section class="detay-bolum">
        <h4><i class="pi pi-phone" /> {{ t('tahsilat.iletisim') }}</h4>
        <div
          v-if="cari.telefon || cari.email"
          class="iletisim-liste"
        >
          <a
            v-if="cari.telefon"
            :href="`tel:${cari.telefon}`"
            class="iletisim-satir"
          >
            <i class="pi pi-phone" />
            <span>{{ cari.telefon }}</span>
          </a>
          <a
            v-if="cari.email"
            :href="`mailto:${cari.email}`"
            class="iletisim-satir"
          >
            <i class="pi pi-envelope" />
            <span>{{ cari.email }}</span>
          </a>
        </div>
        <p
          v-else
          class="muted"
        >
          {{ t('tahsilat.iletisimYok') }}
        </p>
      </section>

      <section class="detay-bolum">
        <h4><i class="pi pi-file" /> {{ t('tahsilat.acikFaturalar') }}</h4>
        <ul
          v-if="(cari.faturalar || []).length"
          class="fatura-liste"
        >
          <li
            v-for="f in cari.faturalar"
            :key="f.faturaId"
            class="fatura-satir"
          >
            <div class="fatura-ust">
              <strong>{{ f.faturaNumarasi }}</strong>
              <span class="gizli-veri">{{ formatCurrency(f.kalanTutar) }}</span>
            </div>
            <div class="fatura-alt">
              <span>{{ t('tahsilat.vade') }}: {{ formatDate(f.vadeTarihi) }}</span>
              <span
                v-if="f.gecikmeGunu > 0"
                class="gecikme-pill"
              >{{ t('tahsilat.gunGecikti', { n: f.gecikmeGunu }) }}</span>
              <span
                v-else
                class="vade-pill"
              >{{ t('tahsilat.gunKaldi', { n: Math.abs(f.gecikmeGunu || 0) }) }}</span>
            </div>
          </li>
        </ul>
        <p
          v-else
          class="muted"
        >
          {{ t('tahsilat.acikFaturaYok') }}
        </p>
      </section>

      <section class="detay-bolum">
        <h4><i class="pi pi-history" /> {{ t('tahsilat.sonTahsilatlar') }}</h4>
        <div
          v-if="yukleniyor"
          :aria-busy="true"
        >
          <SkeletonLoader
            :count="3"
            height="44px"
          />
        </div>
        <ul
          v-else-if="sonTahsilatlar.length"
          class="tahsilat-liste"
        >
          <li
            v-for="s in sonTahsilatlar"
            :key="s.id"
            class="tahsilat-satir"
          >
            <div class="tahsilat-ust">
              <span>{{ formatDate(s.tarih) }}</span>
              <strong class="gizli-veri">{{ formatCurrency(s.tutar) }}</strong>
            </div>
            <div class="tahsilat-alt">
              <Tag
                v-if="s.odemeYontemi"
                :value="odemeYontemiLabel(s.odemeYontemi)"
                :severity="s.odemeYontemi === 'NAKIT' ? 'success' : 'info'"
              />
              <Tag
                :value="s.durum === 'IPTAL' ? t('tahsilat.durumIptal') : t('tahsilat.durumAktif')"
                :severity="s.durum === 'IPTAL' ? 'danger' : 'secondary'"
              />
            </div>
          </li>
        </ul>
        <p
          v-else
          class="muted"
        >
          {{ t('tahsilat.sonTahsilatYok') }}
        </p>
      </section>
    </div>

    <p
      v-else
      class="muted"
    >
      {{ t('common.noData') }}
    </p>

    <template #footer>
      <div
        v-if="cari"
        class="detay-aksiyonlar"
      >
        <Button
          :label="t('tahsilat.tahsilatAl')"
          icon="pi pi-money-bill"
          severity="success"
          @click="$emit('tahsilat', cari)"
        />
        <Button
          v-if="cari.telefon"
          :label="t('tahsilat.whatsapp')"
          icon="pi pi-whatsapp"
          class="p-button-success p-button-outlined"
          @click="$emit('whatsapp', cari)"
        />
        <Button
          v-if="cari.email"
          :label="t('tahsilat.epostaGonder')"
          icon="pi pi-envelope"
          class="p-button-outlined"
          @click="$emit('eposta', cari)"
        />
      </div>
    </template>
  </Drawer>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import { formatCurrency, formatDate } from '../utils/format.js'

defineProps({
  visible: { type: Boolean, default: false },
  cari: { type: Object, default: null },
  sonTahsilatlar: { type: Array, default: () => [] },
  yukleniyor: { type: Boolean, default: false }
})

defineEmits(['update:visible', 'tahsilat', 'whatsapp', 'eposta'])

const { t } = useI18n()

const aralikSeverity = (aralik) => {
  if (!aralik) return 'info'
  if (aralik.startsWith('Vadesi')) return 'success'
  if (aralik.startsWith('61-90') || aralik.startsWith('90+')) return 'danger'
  return 'warning'
}

const odemeYontemiLabel = (y) =>
  ({
    NAKIT: t('tahsilat.nakit'),
    KART: t('tahsilat.kart'),
    TAKSIT: t('tahsilat.taksit'),
    HAVALE: t('tahsilat.havale')
  })[y] || y
</script>

<style scoped>
.cari-detay {
  display: flex;
  flex-direction: column;
  gap: 18px;
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
.detay-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 12px;
}
.detay-alan {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.detay-etiket {
  font-size: 11.5px;
  text-transform: uppercase;
  letter-spacing: 0.03em;
  color: var(--text-muted);
}
.negatif {
  color: var(--danger, #ef4444);
}
.detay-bolum h4 {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 0 0 8px;
  font-size: 14px;
  color: var(--text-primary);
}
.detay-bolum h4 i {
  color: var(--accent);
}
.iletisim-liste {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.iletisim-satir {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--text-secondary);
  text-decoration: none;
  font-size: 13px;
}
.iletisim-satir:hover {
  color: var(--accent);
}
.fatura-liste,
.tahsilat-liste {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.fatura-satir,
.tahsilat-satir {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 10px 12px;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: var(--bg-card);
  font-size: 12.5px;
}
.fatura-ust,
.tahsilat-ust {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.fatura-alt,
.tahsilat-alt {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  color: var(--text-muted);
}
.gecikme-pill {
  background: rgba(239, 68, 68, 0.15);
  color: #ef4444;
  font-size: 11px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 10px;
}
.vade-pill {
  background: rgba(16, 185, 129, 0.15);
  color: #10b981;
  font-size: 11px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 10px;
}
.muted {
  margin: 0;
  font-size: 12px;
  color: var(--text-muted);
}
.detay-aksiyonlar {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
@media (prefers-reduced-motion: reduce) {
  .iletisim-satir {
    transition: none;
  }
}
</style>
