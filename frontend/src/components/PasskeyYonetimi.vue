<template>
  <Card
    class="ayar-kart"
    data-test="passkey-kart"
  >
    <template #title>
      <i class="pi pi-key" />{{ t('passkey.baslik') }}
    </template>
    <template #content>
      <p class="passkey-aciklama">
        {{ t('passkey.aciklama') }}
      </p>

      <p
        v-if="!destek"
        class="passkey-uyari"
        role="status"
      >
        <i class="pi pi-info-circle" />
        {{ t('passkey.desteklenmiyor') }}
      </p>

      <template v-else>
        <DataTable
          :value="liste"
          :loading="yukleniyor"
          striped-rows
          size="small"
          data-test="passkey-liste"
        >
          <Column
            field="etiket"
            :header="t('passkey.etiket')"
          />
          <Column
            field="olusturmaTarihi"
            :header="t('passkey.olusturma')"
          >
            <template #body="{ data }">
              {{ tarih(data.olusturmaTarihi) }}
            </template>
          </Column>
          <Column
            field="sonKullanmaTarihi"
            :header="t('passkey.sonKullanma')"
          >
            <template #body="{ data }">
              {{ data.sonKullanmaTarihi ? tarih(data.sonKullanmaTarihi) : '—' }}
            </template>
          </Column>
          <Column style="width: 64px">
            <template #body="{ data }">
              <Button
                icon="pi pi-trash"
                severity="danger"
                text
                rounded
                :aria-label="t('passkey.sil')"
                :data-test="`passkey-sil-${data.id}`"
                @click="sil(data)"
              />
            </template>
          </Column>
          <template #empty>
            <span class="passkey-bos">{{ t('passkey.yok') }}</span>
          </template>
        </DataTable>

        <Button
          :label="t('passkey.ekle')"
          icon="pi pi-plus"
          class="passkey-ekle"
          data-test="passkey-ekle"
          @click="dialogAc"
        />
      </template>

      <Dialog
        v-model:visible="dialog"
        :header="t('passkey.ekleBaslik')"
        modal
        :style="{ width: '420px' }"
      >
        <div class="field">
          <label for="passkey-parola">{{ t('passkey.parola') }}</label>
          <InputText
            id="passkey-parola"
            v-model="parola"
            type="password"
            autocomplete="current-password"
            class="w-full"
            data-test="passkey-parola"
          />
          <small>{{ t('passkey.parolaIpucu') }}</small>
        </div>
        <div class="field">
          <label for="passkey-etiket">{{ t('passkey.etiket') }}</label>
          <InputText
            id="passkey-etiket"
            v-model="etiket"
            class="w-full"
            :placeholder="t('passkey.etiketIpucu')"
            data-test="passkey-etiket"
          />
        </div>

        <p
          v-if="hata"
          class="passkey-hata"
          role="alert"
        >
          {{ hata }}
        </p>

        <template #footer>
          <Button
            :label="t('passkey.iptal')"
            text
            @click="dialog = false"
          />
          <Button
            :label="t('passkey.kaydet')"
            icon="pi pi-check"
            :loading="kaydediliyor"
            data-test="passkey-kaydet"
            @click="ekle"
          />
        </template>
      </Dialog>
    </template>
  </Card>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { passkeyAPI } from '../api/index.js'
import { passkeyDestekleniyor, passkeyOlustur } from '../utils/webauthn.js'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { formatDate } from '../utils/format.js'

const { t } = useI18n()
const toastBildirim = useToastBildirim()

const destek = passkeyDestekleniyor()
const liste = ref([])
const yukleniyor = ref(false)
const dialog = ref(false)
const parola = ref('')
const etiket = ref('')
const kaydediliyor = ref(false)
const hata = ref('')

const tarih = (deger) => (deger ? formatDate(deger) : '—')

const yukle = async () => {
  if (!destek) return
  yukleniyor.value = true
  try {
    const cevap = await passkeyAPI.listele()
    liste.value = Array.isArray(cevap.data) ? cevap.data : []
  } catch (err) {
    toastBildirim.hata(err.response?.data?.message || t('passkey.hata'))
  } finally {
    yukleniyor.value = false
  }
}

const dialogAc = () => {
  parola.value = ''
  etiket.value = ''
  hata.value = ''
  dialog.value = true
}

const ekle = async () => {
  hata.value = ''
  if (!parola.value.trim()) {
    hata.value = t('passkey.parolaZorunlu')
    return
  }
  kaydediliyor.value = true
  try {
    const secenek = await passkeyAPI.kayitSecenekleri(parola.value)
    const credential = await passkeyOlustur(secenek.data?.publicKey)
    if (!credential) return
    await passkeyAPI.kaydet({
      challengeId: secenek.data.challengeId,
      etiket: etiket.value.trim() || t('passkey.baslik'),
      credential
    })
    dialog.value = false
    toastBildirim.basarili(t('passkey.eklendi'))
    await yukle()
  } catch (err) {
    if (err?.name === 'NotAllowedError' || err?.name === 'AbortError') return
    hata.value = err.response?.data?.message || t('passkey.hata')
  } finally {
    kaydediliyor.value = false
  }
}

const sil = async (kayit) => {
  if (typeof window !== 'undefined' && typeof window.confirm === 'function'
      && !window.confirm(t('passkey.silOnay'))) {
    return
  }
  try {
    await passkeyAPI.sil(kayit.id)
    toastBildirim.basarili(t('passkey.silindi'))
    await yukle()
  } catch (err) {
    toastBildirim.hata(err.response?.data?.message || t('passkey.hata'))
  }
}

onMounted(yukle)
</script>

<style scoped>
.passkey-aciklama {
  margin: 0 0 12px;
  font-size: 13px;
  color: var(--text-secondary);
}

.passkey-uyari {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin: 0;
  padding: 10px 12px;
  border: 1px solid var(--border);
  border-radius: 9px;
  background: var(--bg-muted);
  font-size: 12.5px;
  color: var(--text-secondary);
}

.passkey-bos {
  font-size: 12.5px;
  color: var(--text-muted);
}

.passkey-ekle {
  margin-top: 12px;
}

.passkey-hata {
  margin: 8px 0 0;
  font-size: 12.5px;
  color: var(--danger);
}
</style>
