<template>
  <Dialog
    :visible="visible"
    :header="t('hizliSatis.satisTamamlandi')"
    :modal="true"
    style="width: 420px"
    @update:visible="emit('update:visible', $event)"
  >
    <div
      v-if="satisOzet"
      class="satis-ozet"
    >
      <div class="satis-ozet-baslik">
        <i class="pi pi-check-circle" />
        <span>{{ t('hizliSatis.islemBasarili') }}</span>
      </div>
      <div class="satis-ozet-satir">
        <span>{{ t('hizliSatis.faturaNo') }}</span>
        <strong>{{ satisOzet.faturaNo }}</strong>
      </div>
      <div class="satis-ozet-satir">
        <span>{{ t('hizliSatis.toplam') }}</span>
        <strong>{{ formatCurrency(satisOzet.toplam) }}</strong>
      </div>
      <div class="satis-ozet-satir">
        <span>{{ t('hizliSatis.fisOdenen') }}</span>
        <strong>{{ formatCurrency(satisOzet.odenen) }}</strong>
      </div>
      <div
        v-if="satisOzet.kalan > 0"
        class="satis-ozet-satir"
      >
        <span>{{ t('hizliSatis.fisKalan') }}</span>
        <strong class="borc">{{ formatCurrency(satisOzet.kalan) }}</strong>
      </div>
      <div
        v-if="satisOzet.paraUstu > 0"
        class="satis-ozet-satir"
      >
        <span>{{ t('hizliSatis.paraUstu') }}</span>
        <strong class="para">{{ formatCurrency(satisOzet.paraUstu) }}</strong>
      </div>
      <div class="satis-ozet-satir">
        <span>{{ t('hizliSatis.fisModu') }}</span>
        <strong>{{ satisOzet.fisModu === false ? t('hizliSatis.fiyatsizFis') : t('hizliSatis.fiyatliFis') }}</strong>
      </div>
    </div>
    <template #footer>
      <Button
        :label="t('hizliSatis.kapat')"
        icon="pi pi-times"
        class="p-button-text"
        @click="emit('update:visible', false)"
      />
      <Button
        :label="t('satis.yeniSatis')"
        icon="pi pi-plus"
        class="p-button-success"
        autofocus
        @click="emit('yeni-satis')"
      />
    </template>
  </Dialog>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import { formatCurrency } from '../utils/format.js'

defineProps({
  visible: { type: Boolean, default: false },
  satisOzet: { type: Object, default: null }
})
const emit = defineEmits(['update:visible', 'yeni-satis'])
const { t } = useI18n()
</script>

<style scoped>
.satis-ozet {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.satis-ozet-baslik {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 700;
  color: var(--success);
  margin-bottom: 8px;
}
.satis-ozet-baslik i {
  font-size: 22px;
}
.satis-ozet-satir {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid var(--border);
  font-size: 14px;
}
.satis-ozet-satir span {
  color: var(--text-muted);
}
.satis-ozet-satir .borc {
  color: var(--danger);
}
.satis-ozet-satir .para {
  color: var(--success);
}
</style>
