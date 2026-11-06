<template>
  <div
    class="mini-tablo"
    :data-test="dataTest"
  >
    <table>
      <caption class="mini-tablo-baslik-gizli">
        {{ baslik }}
      </caption>
      <thead>
        <tr>
          <th
            v-for="kolon in kolonlar"
            :key="kolon.anahtar"
            scope="col"
            :class="[hizaSinifi(kolon), { 'mini-tablo-gizle-mobil': kolon.mobilGizle }]"
          >
            {{ kolon.etiket }}
          </th>
        </tr>
      </thead>
      <tbody v-if="!yukleniyor && satirlar.length > 0">
        <tr
          v-for="(satir, index) in satirlar"
          :key="satirAnahtariFn(satir, index)"
          :tabindex="secilebilir ? 0 : undefined"
          :class="{ 'mini-tablo-satir--secilebilir': secilebilir }"
          @click="sec(satir)"
          @keydown.enter.prevent="sec(satir)"
          @keydown.space.prevent="sec(satir)"
        >
          <td
            v-for="kolon in kolonlar"
            :key="kolon.anahtar"
            :class="[hizaSinifi(kolon), { 'mini-tablo-gizle-mobil': kolon.mobilGizle }]"
          >
            <slot
              :name="`hucre-${kolon.anahtar}`"
              :satir="satir"
              :deger="satir[kolon.anahtar]"
              :kolon="kolon"
            >
              {{ hucreMetni(satir, kolon) }}
            </slot>
          </td>
        </tr>
      </tbody>
    </table>

    <div
      v-if="yukleniyor"
      class="mini-tablo-iskelet"
      aria-hidden="true"
    >
      <span
        v-for="n in 3"
        :key="`iskelet-${n}`"
        class="mini-tablo-iskelet-satir"
      />
    </div>

    <p
      v-else-if="satirlar.length === 0"
      class="mini-tablo-bos"
      data-test="mini-tablo-bos"
    >
      {{ bosMetin }}
    </p>
  </div>
</template>

<script setup>
import { formatDate, formatPara } from '../../../utils/format.js'

const props = defineProps({
  kolonlar: { type: Array, default: () => [] },
  satirlar: { type: Array, default: () => [] },
  bosMetin: { type: String, default: 'Veri yok' },
  yukleniyor: { type: Boolean, default: false },
  secilebilir: { type: Boolean, default: true },
  paraBirimi: { type: String, default: 'TRY' },
  satirAnahtari: { type: [String, Function], default: 'id' },
  baslik: { type: String, default: 'Finans tablosu' },
  dataTest: { type: String, default: 'mini-tablo' }
})

const emit = defineEmits(['satir-sec'])

const hizaSinifi = (kolon) => (kolon.hiza === 'sag' ? 'mini-tablo-sag' : 'mini-tablo-sol')

const satirAnahtariFn = (satir, index) => {
  if (typeof props.satirAnahtari === 'function') {
    return props.satirAnahtari(satir, index)
  }
  return satir?.[props.satirAnahtari] ?? index
}

const hucreMetni = (satir, kolon) => {
  const deger = satir?.[kolon.anahtar]
  if (deger === null || deger === undefined || deger === '') return '—'
  if (typeof kolon.bicim === 'function') {
    return kolon.bicim(deger, satir)
  }
  switch (kolon.bicim) {
    case 'para':
      return formatPara(deger, props.paraBirimi)
    case 'yuzde':
      return `${Number(deger).toLocaleString('tr-TR', { maximumFractionDigits: 1 })}%`
    case 'sayi':
      return Number(deger).toLocaleString('tr-TR', { maximumFractionDigits: 0 })
    case 'tarih':
      return formatDate(deger, '—')
    default:
      return String(deger)
  }
}

const sec = (satir) => {
  if (props.secilebilir) emit('satir-sec', satir)
}
</script>

<style scoped>
.mini-tablo {
  min-width: 0;
  overflow: hidden;
}
.mini-tablo table {
  width: 100%;
  table-layout: fixed;
  border-collapse: collapse;
}
.mini-tablo-baslik-gizli {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}
.mini-tablo th {
  padding: 4px 7px 6px;
  border-bottom: 1px solid var(--border);
  font-size: 10.5px;
  font-weight: 600;
  letter-spacing: 0.03em;
  text-transform: uppercase;
  color: var(--text-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.mini-tablo td {
  height: 33px;
  padding: 0 7px;
  border-bottom: 1px solid var(--border);
  font-size: 12px;
  color: var(--text-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.mini-tablo tbody tr:last-child td {
  border-bottom: 0;
}
.mini-tablo-sag {
  text-align: right;
  font-variant-numeric: tabular-nums;
}
.mini-tablo-sol {
  text-align: left;
}
.mini-tablo-satir--secilebilir {
  cursor: pointer;
}
.mini-tablo-satir--secilebilir:hover td {
  background: var(--bg-muted);
  color: var(--text-primary);
}
.mini-tablo-satir--secilebilir:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: -2px;
}
.mini-tablo-bos {
  margin: 0;
  padding: 14px 8px;
  text-align: center;
  font-size: 12px;
  color: var(--text-muted);
}
.mini-tablo-iskelet {
  display: flex;
  flex-direction: column;
  gap: 7px;
  padding: 8px 0;
}
.mini-tablo-iskelet-satir {
  display: block;
  height: 11px;
  border-radius: var(--radius-pill);
  background: var(--bg-muted);
}
.mini-tablo-iskelet-satir:nth-child(2) {
  width: 82%;
}
.mini-tablo-iskelet-satir:nth-child(3) {
  width: 64%;
}
@media (max-width: 720px) {
  .mini-tablo-gizle-mobil {
    display: none;
  }
  .mini-tablo td {
    height: 32px;
    padding: 0 5px;
  }
  .mini-tablo th {
    padding: 3px 5px 5px;
  }
}
</style>
