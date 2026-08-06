<template>
  <Dialog :visible="visible" @update:visible="$emit('update:visible', $event)"
    header="Hesap Makinesi" :modal="false" :style="{ width: '300px' }" :draggable="true" :closable="true">
    <div class="calc-display">{{ display }}</div>
    <div class="calc-keypad">
      <button v-for="btn in buttons" :key="btn" 
        :class="['calc-btn', { operator: '+-*/='.includes(btn), clear: btn === 'C', equals: btn === '=' }]"
        @click="press(btn)">{{ btn }}</button>
    </div>
  </Dialog>
</template>

<script setup>
import { ref } from 'vue'
defineProps({ visible: Boolean })
defineEmits(['update:visible'])

const display = ref('0')
let current = ''
let op = ''
let previous = ''

const buttons = ['7','8','9','/','4','5','6','*','1','2','3','-','0','.','=','+','C']

const press = (btn) => {
  if (btn === 'C') { current = ''; op = ''; previous = ''; display.value = '0'; return }
  if ('+-*/'.includes(btn)) {
    if (current) { previous = current; current = '' }
    op = btn; return
  }
  if (btn === '=') {
    if (previous && current && op) {
      const a = parseFloat(previous), b = parseFloat(current)
      const result = op === '+' ? a+b : op === '-' ? a-b : op === '*' ? a*b : b !== 0 ? a/b : 'Hata'
      display.value = String(Number(result.toFixed(10)))
      current = display.value; previous = ''; op = ''
    }
    return
  }
  if (btn === '.' && current.includes('.')) return
  current += btn
  display.value = current || '0'
}
</script>

<style scoped>
.calc-display { background: var(--surface-ground); border-radius: 8px; padding: 16px; text-align: right; font-size: 24px; font-weight: 700; margin-bottom: 12px; font-family: monospace; }
.calc-keypad { display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px; }
.calc-btn { padding: 14px; font-size: 18px; border: 1px solid var(--surface-border); border-radius: 8px; background: var(--surface-card); cursor: pointer; font-weight: 600; }
.calc-btn:hover { background: var(--surface-hover); }
.calc-btn.operator { background: var(--primary-color); color: white; border-color: var(--primary-color); }
.calc-btn.clear { background: var(--red-500); color: white; }
.calc-btn.equals { background: var(--green-500); color: white; }
</style>
