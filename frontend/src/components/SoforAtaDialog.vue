<template>
  <Dialog
    v-model:visible="soforDialog"
    :header="t('faturalar.soforAtaBaslik')"
    :modal="true"
    :style="{ width: '480px', maxWidth: 'calc(100vw - 24px)' }"
  >
    <div class="sofor-ata-bilgi">
      <span class="sofor-ata-ikon"><i class="pi pi-file" /></span>
      <span class="sofor-ata-metin">
        <span class="sofor-ata-fatura">#{{ soforForm.faturaNumarasi }}</span>
        <span
          v-if="soforForm.cariHesapAd"
          class="sofor-ata-cari"
        >{{ soforForm.cariHesapAd }}</span>
      </span>
      <span class="sofor-ata-rozet"><i class="pi pi-truck" /></span>
    </div>
    <div class="form-grup">
      <label>{{ t('hizliSatis.sofor') }} <span class="zorunlu">*</span></label>
      <Dropdown
        v-model="soforForm.driverId"
        :options="soforSecenekleri"
        option-label="ad"
        option-value="id"
        filter
        :loading="soforYukleniyor"
        :placeholder="t('hizliSatis.soforSecin')"
        class="w-full"
      >
        <template #value="s">
          <div
            v-if="s.value"
            class="sofor-opsiyon"
          >
            <i class="pi pi-user" />
            <span class="sofor-opsiyon-ad">{{ s.value.ad }}</span>
            <i
              v-if="s.value.rol && s.value.rol !== 'DRIVER'"
              class="pi pi-exclamation-triangle sofor-rol-uyari"
              :title="t('hizliSatis.soforRolUyari')"
            />
            <span
              v-if="s.value.bekleyenTeslimatSayisi"
              class="sofor-bekleyen"
            >{{ s.value.bekleyenTeslimatSayisi }}</span>
          </div>
          <span
            v-else
            class="sofor-bos"
          >{{ t('hizliSatis.soforSecin') }}</span>
        </template>
        <template #option="s">
          <div class="sofor-opsiyon">
            <i class="pi pi-user" />
            <span class="sofor-opsiyon-ad">{{ s.option.ad }}</span>
            <i
              v-if="s.option.rol && s.option.rol !== 'DRIVER'"
              class="pi pi-exclamation-triangle sofor-rol-uyari"
              :title="t('hizliSatis.soforRolUyari')"
            />
            <span
              v-if="s.option.bekleyenTeslimatSayisi"
              class="sofor-bekleyen"
            >{{ s.option.bekleyenTeslimatSayisi }}</span>
          </div>
        </template>
      </Dropdown>
    </div>
    <div class="form-grup">
      <label>{{ t('hizliSatis.teslimatAdresi') }} <span class="zorunlu">*</span></label>
      <Textarea
        v-model="soforForm.teslimatAdresi"
        rows="2"
        :placeholder="t('hizliSatis.adresPlaceholder')"
        class="w-full"
      />
    </div>
    <div class="form-grup">
      <label>{{ t('faturalar.beklenenTeslimTarihi') }}</label>
      <DatePicker
        v-model="soforForm.beklenenTeslimTarihi"
        date-format="dd.mm.yy"
        show-icon
        class="w-full"
      />
    </div>
    <div class="form-grup">
      <label>{{ t('hizliSatis.teslimNotu') }}</label>
      <InputText
        v-model="soforForm.notlar"
        :placeholder="t('hizliSatis.teslimNotuPlaceholder')"
        class="w-full"
      />
    </div>
    <template #footer>
      <Button
        :label="t('common.cancel')"
        icon="pi pi-times"
        class="p-button-text"
        @click="soforDialog = false"
      />
      <Button
        :label="t('faturalar.soforAta')"
        icon="pi pi-truck"
        class="p-button-success"
        :loading="soforKaydediliyor"
        @click="soforAtaKaydet"
      />
    </template>
  </Dialog>
</template>

<script setup>
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { teslimatAPI, cariHesapAPI } from '../api/index.js'
import { unwrapList } from '../api/utils/unwrap.js'
import { getLocalDateString } from '../utils/format.js'
import { useToastBildirim } from '../composables/useToastBildirim.js'

const { t } = useI18n()
const toastBildirim = useToastBildirim()
const emit = defineEmits(['saved'])

const soforDialog = ref(false)
const soforKaydediliyor = ref(false)
const soforYukleniyor = ref(false)
const soforSecenekleri = ref([])
const soforForm = ref({
  faturaId: null,
  faturaNumarasi: '',
  cariHesapAd: '',
  driverId: null,
  teslimatAdresi: '',
  beklenenTeslimTarihi: null,
  notlar: ''
})

const soforlariYukle = async () => {
  soforYukleniyor.value = true
  try {
    const r = await teslimatAPI.suruculer()
    soforSecenekleri.value = unwrapList(r)
  } catch {
    soforSecenekleri.value = []
  } finally {
    soforYukleniyor.value = false
  }
}

const open = async (f) => {
  soforForm.value = {
    faturaId: f.id,
    faturaNumarasi: f.faturaNumarasi || f.id,
    cariHesapAd: f.cariHesapAd || '',
    driverId: null,
    teslimatAdresi: '',
    beklenenTeslimTarihi: null,
    notlar: ''
  }
  soforDialog.value = true
  if (!soforSecenekleri.value.length) soforlariYukle()
  // Cari adresi varsa on doldur; kullanici degistirebilir.
  if (f.cariHesapId) {
    try {
      const r = await cariHesapAPI.getById(f.cariHesapId)
      const adres = r.data?.adres
      if (adres) soforForm.value.teslimatAdresi = adres
    } catch {
      /* adres opsiyonel */
    }
  }
}

const soforAtaKaydet = async () => {
  if (!soforForm.value.driverId) {
    toastBildirim.uyari(t('faturalar.soforSecilmedi'))
    return
  }
  if (!soforForm.value.teslimatAdresi?.trim()) {
    toastBildirim.uyari(t('hizliSatis.teslimatAdresiGerekli'))
    return
  }
  soforKaydediliyor.value = true
  try {
    await teslimatAPI.olustur({
      faturaId: soforForm.value.faturaId,
      driverId: soforForm.value.driverId,
      teslimatAdresi: soforForm.value.teslimatAdresi.trim(),
      beklenenTeslimTarihi: soforForm.value.beklenenTeslimTarihi
        ? getLocalDateString(soforForm.value.beklenenTeslimTarihi)
        : null,
      notlar: soforForm.value.notlar?.trim() || null
    })
    toastBildirim.basarili(t('faturalar.soforAtandi'))
    soforDialog.value = false
    emit('saved')
  } catch (e) {
    toastBildirim.hata(e?.response?.data?.message || t('faturalar.teslimatOlusturulamadi'))
  } finally {
    soforKaydediliyor.value = false
  }
}

defineExpose({ open })
</script>

<style scoped>
.sofor-ata-bilgi {
  display: flex;
  align-items: center;
  gap: 11px;
  margin-bottom: 16px;
  padding: 11px 13px;
  border: 1px solid var(--accent-soft-strong);
  border-radius: 12px;
  background: var(--accent-soft);
}
.sofor-ata-ikon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  flex-shrink: 0;
  border-radius: 9px;
  background: var(--bg-card);
  border: 1px solid var(--accent-soft-strong);
  color: var(--accent);
  font-size: 14px;
}
.sofor-ata-metin {
  display: flex;
  flex-direction: column;
  gap: 1px;
  flex: 1;
  min-width: 0;
}
.sofor-ata-rozet {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  flex-shrink: 0;
  border-radius: 8px;
  background: var(--accent);
  color: var(--accent-contrast, #04211d);
  font-size: 13px;
}
.sofor-ata-fatura {
  font-weight: 800;
  color: var(--text-primary);
  font-size: 14px;
  font-variant-numeric: tabular-nums;
}
.sofor-ata-cari {
  color: var(--text-secondary);
  font-size: 12.5px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.sofor-bos {
  color: var(--text-muted);
}
.form-grup {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.form-grup + .form-grup {
  margin-top: 14px;
}
.form-grup > label {
  font-size: 11.5px;
  font-weight: 600;
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.zorunlu {
  color: var(--danger);
}
.sofor-opsiyon {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  min-width: 0;
}
.sofor-opsiyon-ad {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.sofor-opsiyon .sofor-rol-uyari {
  margin-left: auto;
  color: var(--warning);
}
.sofor-opsiyon .sofor-bekleyen {
  margin-left: auto;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 999px;
  background: var(--warning-soft);
  color: var(--warning);
  font-size: 11px;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.sofor-opsiyon .sofor-rol-uyari + .sofor-bekleyen {
  margin-left: 0;
}
</style>
