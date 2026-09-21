<template>
  <Dialog
    v-model:visible="goster"
    :header="$t('guncellemeNotlari.baslik')"
    :modal="true"
    style="width: 520px"
    :closable="true"
    dismissable-mask
  >
    <div class="changelog">
      <div class="surum">
        <span class="surum-etiketi">v{{ SURUM }}</span>
        <ul>
          <li
            v-for="(madde, i) in guncelNotlar"
            :key="i"
          >
            <i class="pi pi-check-circle" /> {{ madde }}
          </li>
        </ul>
      </div>
      <div class="surum">
        <span class="surum-etiketi">v{{ SURUM_110 }}</span>
        <ul>
          <li
            v-for="(madde, i) in notlar110"
            :key="i"
          >
            <i class="pi pi-check-circle" /> {{ madde }}
          </li>
        </ul>
      </div>
      <div class="surum">
        <span class="surum-etiketi">v{{ SURUM_100 }}</span>
        <ul>
          <li><i class="pi pi-check-circle" /> {{ $t('guncellemeNotlari.v100') }}</li>
        </ul>
      </div>
    </div>
    <template #footer>
      <Button
        :label="$t('guncellemeNotlari.tamam')"
        icon="pi pi-check"
        @click="kapat"
      />
    </template>
  </Dialog>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'

const { tm } = useI18n()
const SURUM = typeof __APP_VERSION__ !== 'undefined' ? __APP_VERSION__ : '1.17.0'
const SURUM_110 = '1.1.0'
const SURUM_100 = '1.0.0'
const ANAHTAR = 'raspel_gorulen_surum'
const goster = ref(false)

function listeAl(anahtar) {
  const deger = tm(anahtar)
  return Array.isArray(deger) ? deger : []
}
const guncelNotlar = computed(() => listeAl('guncellemeNotlari.guncel'))
const notlar110 = computed(() => listeAl('guncellemeNotlari.v110'))

onMounted(() => {
  if (localStorage.getItem(ANAHTAR) !== SURUM) {
    goster.value = true
  }
})

// Pencere X ile veya maske ile kapatılsa bile bir daha gösterilmemesi için
// görünürlük false olduğunda görülen sürümü kalıcı olarak kaydet.
watch(goster, (acik) => {
  if (!acik) localStorage.setItem(ANAHTAR, SURUM)
})

const kapat = () => {
  goster.value = false
  localStorage.setItem(ANAHTAR, SURUM)
}
</script>

<style scoped>
.changelog {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.surum ul {
  list-style: none;
  margin: 8px 0 0;
  padding: 0;
}
.surum li {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 4px 0;
  font-size: 13px;
  color: var(--text-secondary);
}
.surum li i {
  color: #4ade80;
  font-size: 14px;
  margin-top: 2px;
}
.surum-etiketi {
  display: inline-block;
  background: var(--accent-soft-strong);
  color: var(--accent);
  padding: 2px 10px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 700;
}
</style>
