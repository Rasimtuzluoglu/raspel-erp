<template>
  <Dialog :visible="visible" @update:visible="$emit('update:visible', $event)"
    header="Hesap Makinesi" :modal="false" :style="{ width: '280px' }" :draggable="true" :closable="true">
    <div class="calc-display">
      <div class="calc-gecmis">{{ gecmis }}</div>
      <div class="calc-sonuc">{{ display }}</div>
    </div>
    <div class="calc-keypad">
      <button class="calc-btn clear" @click="temizle">C</button>
      <button class="calc-btn clear" @click="sil">CE</button>
      <button class="calc-btn operator" @click="yuzde">%</button>
      <button class="calc-btn operator" @click="islem('/')">&divide;</button>
      <button class="calc-btn" @click="rakam('7')">7</button>
      <button class="calc-btn" @click="rakam('8')">8</button>
      <button class="calc-btn" @click="rakam('9')">9</button>
      <button class="calc-btn operator" @click="islem('*')">&times;</button>
      <button class="calc-btn" @click="rakam('4')">4</button>
      <button class="calc-btn" @click="rakam('5')">5</button>
      <button class="calc-btn" @click="rakam('6')">6</button>
      <button class="calc-btn operator" @click="islem('-')">&minus;</button>
      <button class="calc-btn" @click="rakam('1')">1</button>
      <button class="calc-btn" @click="rakam('2')">2</button>
      <button class="calc-btn" @click="rakam('3')">3</button>
      <button class="calc-btn operator" @click="islem('+')">+</button>
      <button class="calc-btn" @click="rakam('0')" style="grid-column:span 2">0</button>
      <button class="calc-btn" @click="rakam('.')">.</button>
      <button class="calc-btn equals" @click="hesapla">=</button>
    </div>
  </Dialog>
</template>

<script setup>
import { ref } from 'vue'
defineProps({ visible: Boolean })
defineEmits(['update:visible'])

const display = ref('0')
const gecmis = ref('')
let current = ''
let previous = ''
let op = ''
let yeniSayi = true

const rakam = (n) => {
  if (yeniSayi) { current = n; yeniSayi = false }
  else { current += n }
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

const temizle = () => { current = ''; previous = ''; op = ''; display.value = '0'; gecmis.value = ''; yeniSayi = true }
const sil = () => { if (!yeniSayi && current.length > 1) current = current.slice(0, -1); else { current = ''; display.value = '0' }; display.value = current || '0' }
const yuzde = () => { if (current) { display.value = String(parseFloat(current) / 100); current = display.value } }
</script>

<style scoped>
.calc-display { background: var(--surface-ground); border-radius: 8px; padding: 12px 16px; text-align: right; margin-bottom: 10px; min-height: 60px; }
.calc-gecmis { font-size: 12px; color: var(--text-muted); min-height: 18px; }
.calc-sonuc { font-size: 26px; font-weight: 700; font-family: monospace; }
.calc-keypad { display: grid; grid-template-columns: repeat(4, 1fr); gap: 6px; }
.calc-btn { padding: 12px 8px; font-size: 16px; border: 1px solid var(--surface-border); border-radius: 8px; background: var(--surface-card); cursor: pointer; font-weight: 600; transition: all 0.15s; }
.calc-btn:hover { background: var(--surface-hover); }
.calc-btn.operator { background: var(--primary-100); color: var(--primary-700); border-color: var(--primary-200); }
.calc-btn.equals { background: var(--green-500); color: white; border-color: var(--green-500); }
.calc-btn.equals:hover { background: var(--green-600); }
.calc-btn.clear { background: var(--surface-hover); color: var(--text-secondary); }
</style>
