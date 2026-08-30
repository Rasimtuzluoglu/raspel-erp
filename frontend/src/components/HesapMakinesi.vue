<template>
  <Dialog
    :visible="visible"
    header="Hesap Makinesi"
    :modal="false"
    :style="{ width: '280px' }"
    :draggable="true"
    :closable="true"
    @update:visible="$emit('update:visible', $event)"
  >
    <div class="calc-display">
      <div class="calc-display-ust">
        <div class="calc-gecmis">
          {{ gecmis }}
        </div>
        <button
          class="calc-kopyala"
          title="Sonucu kopyala"
          @click="kopyala"
        >
          <i class="pi pi-copy" />
        </button>
      </div>
      <div class="calc-sonuc">
        {{ display }}
      </div>
    </div>
    <div class="calc-keypad">
      <button
        class="calc-btn clear"
        @click="temizle"
      >
        C
      </button>
      <button
        class="calc-btn clear"
        @click="sil"
      >
        CE
      </button>
      <button
        class="calc-btn operator"
        @click="yuzde"
      >
        %
      </button>
      <button
        class="calc-btn operator"
        @click="islem('/')"
      >
        &divide;
      </button>
      <button
        class="calc-btn"
        @click="rakam('7')"
      >
        7
      </button>
      <button
        class="calc-btn"
        @click="rakam('8')"
      >
        8
      </button>
      <button
        class="calc-btn"
        @click="rakam('9')"
      >
        9
      </button>
      <button
        class="calc-btn operator"
        @click="islem('*')"
      >
        &times;
      </button>
      <button
        class="calc-btn"
        @click="rakam('4')"
      >
        4
      </button>
      <button
        class="calc-btn"
        @click="rakam('5')"
      >
        5
      </button>
      <button
        class="calc-btn"
        @click="rakam('6')"
      >
        6
      </button>
      <button
        class="calc-btn operator"
        @click="islem('-')"
      >
        &minus;
      </button>
      <button
        class="calc-btn"
        @click="rakam('1')"
      >
        1
      </button>
      <button
        class="calc-btn"
        @click="rakam('2')"
      >
        2
      </button>
      <button
        class="calc-btn"
        @click="rakam('3')"
      >
        3
      </button>
      <button
        class="calc-btn operator"
        @click="islem('+')"
      >
        +
      </button>
      <button
        class="calc-btn"
        style="grid-column: span 2"
        @click="rakam('0')"
      >
        0
      </button>
      <button
        class="calc-btn"
        @click="rakam('.')"
      >
        .
      </button>
      <button
        class="calc-btn equals"
        @click="hesapla"
      >
        =
      </button>
    </div>
  </Dialog>
</template>

<script setup>
import { ref, watch, onUnmounted } from 'vue'
const props = defineProps({ visible: Boolean })
defineEmits(['update:visible'])

const display = ref('0')
const gecmis = ref('')
let current = ''
let previous = ''
let op = ''
let yeniSayi = true

const rakam = (n) => {
  if (yeniSayi) {
    current = n
    yeniSayi = false
  } else {
    current += n
  }
  display.value = current
}

const islem = (o) => {
  if (op && !yeniSayi) hesapla()
  previous = current
  op = o
  yeniSayi = true
  gecmis.value = previous + ' ' + op
}

const hesapla = () => {
  if (!op || !previous) return
  const a = parseFloat(previous)
  const b = parseFloat(current || previous)
  let sonuc = 0
  if (op === '+') sonuc = a + b
  else if (op === '-') sonuc = a - b
  else if (op === '*') sonuc = a * b
  else if (op === '/') sonuc = b !== 0 ? a / b : 0
  gecmis.value = previous + ' ' + op + ' ' + (current || previous) + ' ='
  display.value = String(Number(sonuc.toFixed(10)))
  current = display.value
  previous = ''
  op = ''
  yeniSayi = true
}

const temizle = () => {
  current = ''
  previous = ''
  op = ''
  display.value = '0'
  gecmis.value = ''
  yeniSayi = true
}
const sil = () => {
  if (!yeniSayi && current.length > 1) current = current.slice(0, -1)
  else {
    current = ''
    display.value = '0'
  }
  display.value = current || '0'
}
const yuzde = () => {
  if (current) {
    display.value = String(parseFloat(current) / 100)
    current = display.value
  }
}

const kopyala = async () => {
  try {
    await navigator.clipboard.writeText(display.value)
  } catch {
    /* ignore */
  }
}

const klavyeTusu = (e) => {
  if (!props.visible) return
  const k = e.key
  if (k >= '0' && k <= '9') {
    e.preventDefault()
    rakam(k)
  } else if (k === '.') {
    e.preventDefault()
    rakam('.')
  } else if (k === '+') {
    e.preventDefault()
    islem('+')
  } else if (k === '-') {
    e.preventDefault()
    islem('-')
  } else if (k === '*') {
    e.preventDefault()
    islem('*')
  } else if (k === '/') {
    e.preventDefault()
    islem('/')
  } else if (k === 'Enter' || k === '=') {
    e.preventDefault()
    hesapla()
  } else if (k === 'Backspace') {
    e.preventDefault()
    sil()
  } else if (k === 'Escape' || k === 'c' || k === 'C') {
    temizle()
  } else if (k === '%') {
    e.preventDefault()
    yuzde()
  }
}

watch(
  () => props.visible,
  (v) => {
    if (v) {
      document.addEventListener('keydown', klavyeTusu)
    } else {
      document.removeEventListener('keydown', klavyeTusu)
    }
  }
)

onUnmounted(() => {
  document.removeEventListener('keydown', klavyeTusu)
})
</script>

<style scoped>
.calc-display {
  background: var(--bg-secondary);
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 12px 16px;
  text-align: right;
  margin-bottom: 12px;
  min-height: 64px;
}
.calc-display-ust {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.calc-gecmis {
  font-size: 12px;
  color: var(--text-muted);
  min-height: 18px;
  flex: 1;
}
.calc-kopyala {
  background: transparent;
  border: none;
  color: var(--text-muted);
  cursor: pointer;
  padding: 2px;
  font-size: 14px;
  transition: color 0.15s;
}
.calc-kopyala:hover {
  color: var(--accent);
}
.calc-sonuc {
  font-size: 28px;
  font-weight: 700;
  font-family: monospace;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.calc-keypad {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 6px;
}
.calc-btn {
  padding: 12px 8px;
  font-size: 16px;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: var(--bg-secondary);
  color: var(--text-primary);
  cursor: pointer;
  font-weight: 600;
  transition: all 0.15s;
}
.calc-btn:hover {
  background: rgba(148, 163, 184, 0.12);
}
.calc-btn.operator {
  background: rgba(59, 130, 246, 0.15);
  color: var(--accent);
  border-color: rgba(59, 130, 246, 0.3);
}
.calc-btn.operator:hover {
  background: rgba(59, 130, 246, 0.25);
}
.calc-btn.equals {
  background: var(--accent);
  color: white;
  border-color: var(--accent);
}
.calc-btn.equals:hover {
  background: var(--accent-hover);
}
.calc-btn.clear {
  background: var(--bg-secondary);
  color: var(--text-muted);
}
</style>
