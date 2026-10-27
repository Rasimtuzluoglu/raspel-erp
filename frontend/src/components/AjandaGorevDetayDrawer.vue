<template>
  <Drawer
    :visible="visible"
    position="right"
    :header="baslik"
    :style="{ width: '440px' }"
    :breakpoints="{ '575px': '92vw' }"
    @update:visible="$emit('update:visible', $event)"
  >
    <div
      v-if="kayit"
      class="detay"
    >
      <div class="detay-ust">
        <div class="detay-baslik">
          <i :class="kayitTuru === 'hatirlatici' ? 'pi pi-bell' : 'pi pi-check-square'" />
          <span>{{ kayit.baslik }}</span>
        </div>
        <div class="detay-rozetler">
          <Tag
            :value="kayitTuru === 'hatirlatici' ? t('ajanda.hatirlatici') : t('ajanda.gorevlerim')"
            :severity="kayitTuru === 'hatirlatici' ? 'danger' : 'success'"
          />
          <Tag
            v-if="kayitTuru === 'gorev'"
            :value="oncelikAdi(kayit.oncelik)"
            :severity="oncelikSeverity(kayit.oncelik)"
          />
          <Tag
            v-if="kayitTuru === 'gorev'"
            :value="kayit.durum === 'TAMAMLANDI' ? t('ajanda.durumTamamlanan') : t('ajanda.durumBekleyen')"
            :severity="kayit.durum === 'TAMAMLANDI' ? 'success' : 'info'"
          />
          <Tag
            v-if="gecikmis"
            :value="t('ajanda.durumGecikmis')"
            severity="danger"
          />
        </div>
      </div>

      <div class="detay-grid">
        <div
          v-if="kayitTuru === 'gorev'"
          class="detay-alan"
        >
          <span class="detay-etiket">{{ t('ajanda.bitisTarihi') }}</span>
          <span :class="{ 'metin-gecikmis': gecikmis }">{{ kayit.bitisTarihi ? formatTarih(kayit.bitisTarihi) : '-' }}</span>
        </div>
        <div
          v-else
          class="detay-alan"
        >
          <span class="detay-etiket">{{ t('ajanda.hatirlatmaZamani') }}</span>
          <span>{{ formatTarihSaat(kayit.hatirlatmaZamani) }}</span>
        </div>
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('ajanda.aciklama') }}</span>
          <span>{{ kayit.aciklama || '-' }}</span>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="detay-aksiyonlar">
        <template v-if="kayitTuru === 'gorev'">
          <Button
            :label="kayit?.durum === 'TAMAMLANDI' ? t('ajanda.geriAl') : t('ajanda.tamamla')"
            :icon="kayit?.durum === 'TAMAMLANDI' ? 'pi pi-replay' : 'pi pi-check'"
            :severity="kayit?.durum === 'TAMAMLANDI' ? 'secondary' : 'success'"
            @click="$emit('tamamla', kayit)"
          />
          <Button
            :label="t('ajanda.duzenle')"
            icon="pi pi-pencil"
            text
            @click="$emit('duzenle', kayit)"
          />
        </template>
        <Button
          :label="t('common.delete')"
          icon="pi pi-trash"
          severity="danger"
          text
          @click="$emit('sil', kayit)"
        />
      </div>
    </template>
  </Drawer>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { formatTarih, formatTarihSaat } from '../utils/format.js'

const props = defineProps({
  visible: { type: Boolean, default: false },
  kayit: { type: Object, default: null },
  /** 'gorev' | 'hatirlatici' */
  kayitTuru: { type: String, default: 'gorev' }
})

defineEmits(['update:visible', 'duzenle', 'tamamla', 'sil'])

const { t } = useI18n()

const baslik = computed(() =>
  props.kayitTuru === 'hatirlatici' ? t('ajanda.hatirlaticiDetay') : t('ajanda.gorevDetay')
)

const oncelikAdi = (o) => {
  const map = { DUSUK: t('ajanda.dusuk'), YUKSEK: t('ajanda.yuksek') }
  return map[o] || t('ajanda.orta')
}
const oncelikSeverity = (o) => (o === 'YUKSEK' ? 'danger' : o === 'DUSUK' ? 'secondary' : 'warn')

const gecikmis = computed(() => {
  if (props.kayitTuru !== 'gorev' || !props.kayit?.bitisTarihi) return false
  if (props.kayit.durum === 'TAMAMLANDI') return false
  const bitis = String(props.kayit.bitisTarihi).slice(0, 10)
  const bugun = new Date()
  const p = (n) => String(n).padStart(2, '0')
  return bitis < `${bugun.getFullYear()}-${p(bugun.getMonth() + 1)}-${p(bugun.getDate())}`
})
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
.detay-etiket {
  font-size: 11.5px;
  text-transform: uppercase;
  letter-spacing: 0.03em;
  color: var(--text-muted);
}
.metin-gecikmis {
  color: var(--danger, #ef4444);
  font-weight: 600;
}
.detay-aksiyonlar {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
</style>
