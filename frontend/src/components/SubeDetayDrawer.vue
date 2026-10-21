<template>
  <Drawer
    :visible="visible"
    position="right"
    :header="sube?.ad || t('subeler.title')"
    :style="{ width: '480px', maxWidth: '96vw' }"
    @update:visible="$emit('update:visible', $event)"
  >
    <div
      v-if="sube"
      class="detay"
    >
      <div class="detay-ust">
        <div class="detay-baslik">
          <i class="pi pi-building" />
          <span>{{ sube.ad || '-' }}</span>
        </div>
        <Tag
          :value="sube.aktif ? t('status.active') : t('status.passive')"
          :severity="sube.aktif ? 'success' : 'danger'"
        />
      </div>

      <div class="detay-grid">
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('subeler.yetkili') }}</span>
          <span>{{ sube.yetkili || '-' }}</span>
        </div>
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('subeler.telefon') }}</span>
          <span>{{ sube.telefon || '-' }}</span>
        </div>
        <div class="detay-alan genis">
          <span class="detay-etiket">{{ t('subeler.adres') }}</span>
          <span>{{ sube.adres || '-' }}</span>
        </div>
        <div class="detay-alan">
          <span class="detay-etiket">{{ t('subeler.olusturma') }}</span>
          <span>{{ tarihSaat(sube.olusturmaTarihi) }}</span>
        </div>
      </div>

      <div class="depo-bolum">
        <div class="depo-baslik">
          <span>{{ t('subeler.bagliDepolar') }}</span>
          <Tag
            :value="String(depolar.length)"
            severity="secondary"
          />
        </div>
        <div
          v-if="depolarYukleniyor"
          class="depo-bos"
          role="status"
          aria-live="polite"
        >
          <i class="pi pi-spin pi-spinner" /> {{ t('common.loading') }}
        </div>
        <ul
          v-else-if="depolar.length"
          class="depo-liste"
          :aria-label="t('subeler.bagliDepolar')"
        >
          <li
            v-for="d in depolar"
            :key="d.id"
            class="depo-satir"
          >
            <i class="pi pi-warehouse" />
            <span class="depo-ad">{{ d.ad }}</span>
            <Tag
              :value="d.aktif ? t('status.active') : t('status.passive')"
              :severity="d.aktif ? 'success' : 'danger'"
            />
          </li>
        </ul>
        <div
          v-else
          class="depo-bos"
        >
          {{ t('subeler.bagliDepoYok') }}
        </div>
      </div>
    </div>

    <template #footer>
      <div class="detay-aksiyonlar">
        <Button
          v-if="sube && duzenlenebilir"
          :label="t('common.edit')"
          icon="pi pi-pencil"
          @click="$emit('duzenle', sube)"
        />
      </div>
    </template>
  </Drawer>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import { formatTarihSaat as tarihSaat } from '../utils/format.js'

defineProps({
  visible: { type: Boolean, default: false },
  sube: { type: Object, default: null },
  depolar: { type: Array, default: () => [] },
  depolarYukleniyor: { type: Boolean, default: false },
  duzenlenebilir: { type: Boolean, default: false }
})

defineEmits(['update:visible', 'duzenle'])

const { t } = useI18n()
</script>

<style scoped>
.detay {
  display: flex;
  flex-direction: column;
  gap: 16px;
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
.depo-bolum {
  border-top: 1px solid var(--border);
  padding-top: 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.depo-baslik {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12.5px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.03em;
  color: var(--text-secondary);
}
.depo-liste {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.depo-satir {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border: 1px solid var(--border);
  border-radius: var(--radius-md, 10px);
  background: var(--bg-muted);
}
.depo-satir i {
  color: var(--text-muted);
}
.depo-ad {
  flex: 1 1 auto;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--text-primary);
}
.depo-bos {
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
