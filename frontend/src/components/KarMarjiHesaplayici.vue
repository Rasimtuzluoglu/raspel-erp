<template>
  <Dialog
    :visible="visible"
    :header="t('marjHesap.baslik')"
    :modal="false"
    :style="{ width: '320px' }"
    :draggable="true"
    :closable="true"
    @update:visible="$emit('update:visible', $event)"
  >
    <div class="marj-form">
      <div class="form-satir">
        <label>{{ t('marjHesap.alisFiyati') }}</label><InputNumber
          v-model="alis"
          :min="0"
          class="w-full"
        />
      </div>
      <div class="form-satir">
        <label>{{ t('marjHesap.satisFiyati') }}</label><InputNumber
          v-model="satis"
          :min="0"
          class="w-full"
        />
      </div>
      <div class="form-satir">
        <label>{{ t('marjHesap.veyaMarj') }}</label>
        <InputNumber
          v-model="marj"
          :min="0"
          :max="1000"
          class="w-full"
          @update:model-value="marjdanHesapla"
        />
      </div>
      <div class="marj-sonuc">
        <div class="sonuc-satir">
          <span>{{ t('marjHesap.karTutari') }}</span><strong :class="kar >= 0 ? 'positive' : 'negative'">{{ format(kar) }}</strong>
        </div>
        <div class="sonuc-satir">
          <span>{{ t('marjHesap.karMarjYuzde') }}</span><strong>{{ yuzde.toFixed(2) }}%</strong>
        </div>
        <div class="sonuc-satir">
          <span>{{ t('marjHesap.satisMarjli') }}</span><strong>{{ format(satis || 0) }}</strong>
        </div>
      </div>
    </div>
  </Dialog>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
defineProps({ visible: Boolean })
defineEmits(['update:visible'])

const { t } = useI18n()

const alis = ref(100)
const satis = ref(150)
const marj = ref(50)

const kar = computed(() => (satis.value || 0) - (alis.value || 0))
const yuzde = computed(() => (alis.value ? (kar.value / alis.value) * 100 : 0))

const marjdanHesapla = () => {
  satis.value = (alis.value || 0) * (1 + (marj.value || 0) / 100)
}
const format = (v) => new Intl.NumberFormat('tr-TR', { style: 'currency', currency: 'TRY' }).format(v)
</script>

<style scoped>
.marj-form {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.form-satir label {
  display: block;
  font-size: 13px;
  color: var(--text-secondary);
  margin-bottom: 4px;
}
.marj-sonuc {
  background: var(--bg-secondary);
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 14px 16px;
}
.sonuc-satir {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid var(--border);
  font-size: 14px;
  color: var(--text-secondary);
}
.sonuc-satir:last-child {
  border-bottom: none;
}
.sonuc-satir strong {
  color: var(--text-primary);
  font-family: monospace;
}
.positive {
  color: #10b981;
}
.negative {
  color: #ef4444;
}
</style>
