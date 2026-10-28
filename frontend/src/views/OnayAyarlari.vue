<template>
  <div class="onay-ayarlari-sayfasi">
    <PageHeader
      :title="t('onayAyarlari.title')"
      :description="t('onayAyarlari.altBaslik')"
      icon="pi pi-sliders-h"
    >
      <template #actions>
        <Button
          :label="t('onayAyarlari.geri')"
          icon="pi pi-arrow-left"
          text
          @click="router.push('/onaylar')"
        />
        <Button
          :label="t('onayAyarlari.yeniAyar')"
          icon="pi pi-plus"
          @click="yeniAc"
        />
      </template>
    </PageHeader>

    <div class="ayar-kart">
      <p class="ayar-ipucu">
        <i class="pi pi-info-circle" />
        {{ t('onayAyarlari.ipucu') }}
      </p>

      <SkeletonLoader
        v-if="yukleniyor && !ayarlar.length"
        :count="3"
        height="52px"
      />

      <AppDataTable
        v-else
        :value="ayarlar"
        data-key="modul"
        :paginator="false"
        striped-rows
        :loading="yukleniyor"
        gorunum-anahtari="onay-ayarlari"
        :empty-message="t('onayAyarlari.bos')"
      >
        <Column
          field="modul"
          :header="t('onayAyarlari.modul')"
        >
          <template #body="{ data }">
            <span class="modul-ad">{{ modulAdi(data.modul) }}</span>
          </template>
        </Column>
        <Column :header="t('onayAyarlari.esikTutar')">
          <template #body="{ data }">
            <InputNumber
              v-model="data.esikTutar"
              mode="currency"
              currency="TRY"
              locale="tr-TR"
              size="small"
              :aria-label="`${modulAdi(data.modul)} ${t('onayAyarlari.esikTutar')}`"
            />
          </template>
        </Column>
        <Column :header="t('onayAyarlari.otomatikOnay')">
          <template #body="{ data }">
            <ToggleSwitch
              v-model="data.otomatikOnay"
              :aria-label="`${modulAdi(data.modul)} ${t('onayAyarlari.otomatikOnay')}`"
            />
          </template>
        </Column>
        <Column
          :header="t('onayAyarlari.islem')"
          style="width: 190px"
        >
          <template #body="{ data }">
            <div class="ayar-aksiyonlar">
              <Button
                :label="t('onayAyarlari.kaydet')"
                icon="pi pi-save"
                size="small"
                :loading="kaydediliyor === data.modul"
                @click="kaydet(data)"
              />
              <Button
                :label="t('onayAyarlari.sil')"
                icon="pi pi-trash"
                size="small"
                severity="danger"
                text
                :aria-label="`${modulAdi(data.modul)} ${t('onayAyarlari.sil')}`"
                @click="silAc(data)"
              />
            </div>
          </template>
        </Column>
        <template #empty>
          <EmptyState
            icon="pi pi-sliders-h"
            :message="t('onayAyarlari.bos')"
            :sub-message="t('onayAyarlari.bosIpucu')"
          />
        </template>
      </AppDataTable>
    </div>

    <Dialog
      v-model:visible="yeniDialog"
      :header="t('onayAyarlari.yeniAyarBaslik')"
      modal
      :style="{ width: '440px' }"
      :breakpoints="{ '575px': '92vw' }"
      aria-modal="true"
    >
      <div class="ayar-form">
        <div class="field">
          <label for="ayar-modul">{{ t('onayAyarlari.modul') }}</label>
          <Select
            id="ayar-modul"
            v-model="yeniForm.modul"
            :options="modulSecenekleri"
            option-label="label"
            option-value="value"
            :placeholder="t('onayAyarlari.modulSec')"
            class="w-full"
          />
        </div>
        <div class="field">
          <label for="ayar-esik">{{ t('onayAyarlari.esikTutar') }}</label>
          <InputNumber
            id="ayar-esik"
            v-model="yeniForm.esikTutar"
            mode="currency"
            currency="TRY"
            locale="tr-TR"
            class="w-full"
          />
        </div>
        <div class="field field-satir">
          <label for="ayar-otomatik">{{ t('onayAyarlari.otomatikOnay') }}</label>
          <ToggleSwitch
            id="ayar-otomatik"
            v-model="yeniForm.otomatikOnay"
          />
        </div>
        <small
          v-if="yeniHata"
          class="hata-metin"
          role="alert"
        >{{ yeniHata }}</small>
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          icon="pi pi-times"
          text
          @click="yeniDialog = false"
        />
        <Button
          :label="t('onayAyarlari.ekle')"
          icon="pi pi-check"
          :loading="yeniKaydediliyor"
          @click="yeniKaydet"
        />
      </template>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { useConfirm } from 'primevue/useconfirm'
import { onayAyariAPI } from '../api/index.js'
import { useToastBildirim } from '../composables/useToastBildirim.js'

const { t } = useI18n()
const router = useRouter()
const confirm = useConfirm()
const toastBildirim = useToastBildirim()

const TUM_MODULLER = ['MASRAF', 'SATINALMA', 'IZIN']
const MODUL_ANAHTAR = {
  IZIN: 'turIzin',
  MASRAF: 'turMasraf',
  SATINALMA: 'turSatinalma',
  SIPARIS: 'turSiparis'
}

const ayarlar = ref([])
const yukleniyor = ref(false)
const kaydediliyor = ref(null)

const yeniDialog = ref(false)
const yeniKaydediliyor = ref(false)
const yeniHata = ref('')
const yeniForm = ref({ modul: null, esikTutar: 0, otomatikOnay: false })

const modulAdi = (modul) =>
  MODUL_ANAHTAR[modul] ? t(`onaylar.${MODUL_ANAHTAR[modul]}`) : (modul || '-')

const modulSecenekleri = computed(() =>
  TUM_MODULLER.map((modul) => ({ value: modul, label: modulAdi(modul) }))
)

const yukle = async () => {
  yukleniyor.value = true
  try {
    const r = await onayAyariAPI.listele()
    ayarlar.value = Array.isArray(r.data) ? r.data : []
  } catch (err) {
    ayarlar.value = []
    toastBildirim.hata(err?.response?.data?.message || t('onayAyarlari.yuklemeHatasi'))
  } finally {
    yukleniyor.value = false
  }
}

const kaydet = async (ayar) => {
  if (!ayar?.modul) return
  kaydediliyor.value = ayar.modul
  try {
    await onayAyariAPI.kaydet({
      modul: ayar.modul,
      esikTutar: ayar.esikTutar ?? 0,
      otomatikOnay: !!ayar.otomatikOnay
    })
    toastBildirim.basarili(t('onayAyarlari.kaydedildi', { modul: modulAdi(ayar.modul) }))
    await yukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('onayAyarlari.kaydetHatasi'))
  } finally {
    kaydediliyor.value = null
  }
}

const yeniAc = () => {
  yeniHata.value = ''
  yeniForm.value = {
    modul: modulSecenekleri.value[0].value,
    esikTutar: 0,
    otomatikOnay: false
  }
  yeniDialog.value = true
}

const yeniKaydet = async () => {
  if (!yeniForm.value.modul) {
    yeniHata.value = t('onayAyarlari.modulSec')
    return
  }
  yeniKaydediliyor.value = true
  yeniHata.value = ''
  try {
    await onayAyariAPI.kaydet({
      modul: yeniForm.value.modul,
      esikTutar: yeniForm.value.esikTutar ?? 0,
      otomatikOnay: !!yeniForm.value.otomatikOnay
    })
    toastBildirim.basarili(t('onayAyarlari.kaydedildi', { modul: modulAdi(yeniForm.value.modul) }))
    yeniDialog.value = false
    await yukle()
  } catch (err) {
    yeniHata.value = err?.response?.data?.message || t('onayAyarlari.kaydetHatasi')
  } finally {
    yeniKaydediliyor.value = false
  }
}

const silAc = (ayar) => {
  if (!ayar?.modul) return
  confirm.require({
    header: t('onayAyarlari.silBaslik'),
    message: t('onayAyarlari.silAciklama', { modul: modulAdi(ayar.modul) }),
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: t('onayAyarlari.sil'),
    rejectLabel: t('common.cancel'),
    acceptClass: 'p-button-danger',
    accept: () => silOnayla(ayar)
  })
}

const silOnayla = async (ayar) => {
  try {
    // Backend'de silme ucu yok; ayar varsayilana sifirlanarak kalici hale getirilir.
    await onayAyariAPI.kaydet({ modul: ayar.modul, esikTutar: 0, otomatikOnay: false })
    toastBildirim.basarili(t('onayAyarlari.silindi', { modul: modulAdi(ayar.modul) }))
    await yukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('onayAyarlari.kaydetHatasi'))
  }
}

onMounted(yukle)
</script>

<style scoped>
.onay-ayarlari-sayfasi {
  padding: 0;
  max-width: 100%;
}
.ayar-kart {
  border: 1px solid var(--border);
  border-radius: var(--radius-lg, 14px);
  background: var(--bg-card);
  padding: 16px;
}
.ayar-ipucu {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin: 0 0 14px;
  font-size: 12.5px;
  color: var(--text-secondary);
}
.ayar-ipucu i {
  color: var(--accent);
  margin-top: 2px;
}
.modul-ad {
  font-weight: 600;
}
.ayar-aksiyonlar {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}
.ayar-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.field label {
  font-size: 12.5px;
  color: var(--text-secondary);
}
.field-satir {
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
}
.hata-metin {
  color: var(--danger, #ef4444);
  font-size: 12px;
}
@media (max-width: 575px) {
  .ayar-kart {
    padding: 12px;
  }
  .ayar-aksiyonlar {
    flex-direction: column;
    align-items: stretch;
  }
}
@media (prefers-reduced-motion: reduce) {
  .ayar-kart {
    transition: none;
  }
}
</style>
