import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import AppDialog from '../AppDialog.vue'

/**
 * AppDialog — paylasilan diyalog sarmalayicisi.
 *
 * 3 ekran (`Satis`, `Faturalar`, `Teklifler`) bunu kullanir. Degisiklikler
 * varsayilanlari BOZMADAN yapilmalidir; bu testler sozlesmeyi sabitler:
 *   - `visible` / `update:visible` iki yonlu kullanim
 *   - baslik, genislik ve icerik yuksekligi prop'lari
 *   - footer slotu
 *   - Esc ile kapanma kontrolu (`closeOnEscape`)
 *   - ICERIK KAYDIRMASI: once `max-height` veriliyor ama `overflow` yoktu;
 *     icerik kirpiliyordu. Stil kaynaginda `overflow-y: auto` olmali.
 */

import { readFileSync } from 'node:fs'
import { join } from 'node:path'

const DialogStub = {
  name: 'Dialog',
  props: ['visible', 'header', 'modal', 'closable', 'closeOnEscape'],
  emits: ['update:visible'],
  template:
    '<div class="p-dialog" :data-visible="String(visible)" :data-closable="String(closable)" :data-esc="String(closeOnEscape)"><div class="p-dialog-header">{{ header }}</div><div class="p-dialog-content"><slot /></div><div class="p-dialog-footer"><slot name="footer" /></div></div>'
}

const kur = (props = {}, slots = {}) =>
  mount(AppDialog, {
    props: { visible: true, header: 'Başlık', ...props },
    slots,
    global: { stubs: { Dialog: DialogStub, teleport: true } }
  })

describe('AppDialog', () => {
  it('basligi vevarsayilan slotu gosterir', () => {
    const w = kur({}, { default: '<p class="icerik">Merhaba</p>' })
    expect(w.find('.p-dialog-header').text()).toBe('Başlık')
    expect(w.find('.icerik').text()).toBe('Merhaba')
  })

  it('footer slotu yalnizca verilirse render edilir', () => {
    const bos = kur()
    expect(bos.find('.p-dialog-footer button').exists()).toBe(false)

    const dolu = kur({}, { footer: '<button class="kaydet">Kaydet</button>' })
    expect(dolu.find('.p-dialog-footer .kaydet').exists()).toBe(true)
  })

  it('gorunurluk Dialog bilesenine iletilir', () => {
    expect(kur({ visible: true }).find('.p-dialog').attributes('data-visible')).toBe('true')
    expect(kur({ visible: false }).find('.p-dialog').attributes('data-visible')).toBe('false')
  })

  it('closable ve closeOnEscape prop`lari Dialog`a iletilir', () => {
    const w = kur({ closable: false, closeOnEscape: false })
    expect(w.find('.p-dialog').attributes('data-closable')).toBe('false')
    expect(w.find('.p-dialog').attributes('data-esc')).toBe('false')
  })

  it('varsayilanlar korunur (closable true, Esc true)', () => {
    const w = kur()
    expect(w.find('.p-dialog').attributes('data-closable')).toBe('true')
    expect(w.find('.p-dialog').attributes('data-esc')).toBe('true')
  })

  it('genislik ve icerik yuksekligi stil degiskenine yazilir', () => {
    const w = kur({ width: '1280px', contentMaxHeight: 'min(84vh, 900px)' })
    const stil = w.find('.p-dialog').attributes('style') || ''
    expect(stil).toContain('1280px')
    expect(stil).toContain('--app-dialog-ch')
  })

  it('KRITIK: icerik alani kaydirilabilir (overflow-y: auto)', () => {
    // Once `max-height` verilip `overflow` verilmedigi icin icerik KIRPILIYORDU.
    const kaynak = readFileSync(join(process.cwd(), 'src/components/AppDialog.vue'), 'utf8')
    expect(kaynak).toMatch(/\.p-dialog-content[\s\S]*?overflow-y:\s*auto/)
  })

  it('update:visible olayi disari aktarilir', async () => {
    const w = kur()
    w.findComponent(DialogStub).vm.$emit('update:visible', false)
    await w.vm.$nextTick()
    expect(w.emitted('update:visible')).toEqual([[false]])
  })
})
