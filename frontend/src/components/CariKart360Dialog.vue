<template>
  <Dialog
    :visible="visible"
    :header="t('cariKart.baslik')"
    modal
    :style="{ width: '900px', maxWidth: '95vw' }"
    @update:visible="emit('update:visible', $event)"
  >
    <div
      v-if="yukleniyor"
      class="kart-yukleniyor"
    >
      <i class="pi pi-spin pi-spinner" /> {{ t('cariKart.yukleniyor') }}
    </div>

    <div v-else-if="kart">
      <div class="kart-ust">
        <div class="kart-kimlik">
          <h3>{{ kart.cariAd }}</h3>
          <div class="kart-meta">
            <span v-if="kart.telefon"><i class="pi pi-phone" /> {{ kart.telefon }}</span>
            <span v-if="kart.email"><i class="pi pi-envelope" /> {{ kart.email }}</span>
            <span v-if="kart.temsilciAd"><i class="pi pi-user" /> {{ kart.temsilciAd }}</span>
          </div>
        </div>
      </div>

      <div class="kredi-kart">
        <div class="kredi-satir">
          <div class="kredi-alan">
            <span>{{ t('cariKart.krediLimiti') }}</span>
            <strong>{{ formatCurrency(kart.kredi?.krediLimiti) }}</strong>
          </div>
          <div class="kredi-alan">
            <span>{{ t('cariKart.bakiye') }}</span>
            <strong>{{ formatCurrency(kart.kredi?.bakiye) }}</strong>
          </div>
          <div class="kredi-alan">
            <span>{{ t('cariKart.kullanilabilirKredi') }}</span>
            <strong :class="{ 'limit-asimi': kart.kredi?.limitAsimi }">
              {{ formatCurrency(kart.kredi?.kullanilabilirKredi) }}
            </strong>
          </div>
          <div class="kredi-alan">
            <span>{{ t('cariKart.riskOrani') }}</span>
            <strong>{{ kart.kredi?.riskOrani != null ? kart.kredi.riskOrani + '%' : '-' }}</strong>
          </div>
        </div>
        <Tag
          v-if="kart.kredi?.limitAsimi"
          severity="danger"
          :value="t('cariKart.limitAsimi')"
        />
      </div>

      <TabView>
        <TabPanel :header="t('cariKart.ozet')">
          <DataTable
            :value="ozetSatirlari"
            responsive-layout="scroll"
          >
            <Column
              field="etiket"
              :header="t('cariKart.gosterge')"
            />
            <Column
              field="deger"
              :header="t('cariKart.deger')"
            />
          </DataTable>
        </TabPanel>

        <TabPanel :header="t('cariKart.faturalar')">
          <DataTable
            :value="kart.sonFaturalar"
            responsive-layout="scroll"
            :empty-message="t('cariKart.kayitYok')"
          >
            <Column
              field="faturaNumarasi"
              :header="t('cariKart.faturaNo')"
            />
            <Column
              field="tarih"
              :header="t('cariKart.tarih')"
            >
              <template #body="{ data }">
                {{ formatDate(data.tarih) }}
              </template>
            </Column>
            <Column
              field="durum"
              :header="t('cariKart.durum')"
            />
            <Column
              field="genelToplam"
              :header="t('cariKart.tutar')"
            >
              <template #body="{ data }">
                {{ formatCurrency(data.genelToplam) }}
              </template>
            </Column>
            <Column
              field="kalanTutar"
              :header="t('cariKart.kalan')"
            >
              <template #body="{ data }">
                {{ formatCurrency(data.kalanTutar) }}
              </template>
            </Column>
          </DataTable>
        </TabPanel>

        <TabPanel :header="t('cariKart.siparisler')">
          <DataTable
            :value="kart.sonSiparisler"
            responsive-layout="scroll"
            :empty-message="t('cariKart.kayitYok')"
          >
            <Column
              field="siparisNo"
              :header="t('cariKart.siparisNo')"
            />
            <Column
              field="tarih"
              :header="t('cariKart.tarih')"
            >
              <template #body="{ data }">
                {{ formatDate(data.tarih) }}
              </template>
            </Column>
            <Column
              field="durum"
              :header="t('cariKart.durum')"
            />
            <Column
              field="genelToplam"
              :header="t('cariKart.tutar')"
            >
              <template #body="{ data }">
                {{ formatCurrency(data.genelToplam) }}
              </template>
            </Column>
          </DataTable>
        </TabPanel>

        <TabPanel :header="t('cariKart.iadeler')">
          <DataTable
            :value="kart.sonIadeler"
            responsive-layout="scroll"
            :empty-message="t('cariKart.kayitYok')"
          >
            <Column
              field="tarih"
              :header="t('cariKart.tarih')"
            >
              <template #body="{ data }">
                {{ formatDate(data.tarih) }}
              </template>
            </Column>
            <Column
              field="tur"
              :header="t('cariKart.tur')"
            />
            <Column
              field="durum"
              :header="t('cariKart.durum')"
            />
            <Column
              field="tutar"
              :header="t('cariKart.tutar')"
            >
              <template #body="{ data }">
                {{ formatCurrency(data.tutar) }}
              </template>
            </Column>
          </DataTable>
        </TabPanel>

        <TabPanel :header="t('cariKart.firsatlar')">
          <DataTable
            :value="kart.firsatlar"
            responsive-layout="scroll"
            :empty-message="t('cariKart.kayitYok')"
          >
            <Column
              field="ad"
              :header="t('cariKart.firsatAd')"
            />
            <Column
              field="durum"
              :header="t('cariKart.durum')"
            />
            <Column
              field="kaynak"
              :header="t('cariKart.kaynak')"
            />
            <Column
              field="deger"
              :header="t('cariKart.degerBaslik')"
            >
              <template #body="{ data }">
                {{ formatCurrency(data.deger) }}
              </template>
            </Column>
            <Column
              field="tahminiKapanis"
              :header="t('cariKart.tahminiKapanis')"
            >
              <template #body="{ data }">
                {{ formatDate(data.tahminiKapanis) }}
              </template>
            </Column>
          </DataTable>
        </TabPanel>

        <TabPanel :header="t('cariKart.notlar')">
          <DataTable
            :value="kart.notlar"
            responsive-layout="scroll"
            :empty-message="t('cariKart.kayitYok')"
          >
            <Column
              field="baslik"
              :header="t('cariKart.notBaslik')"
            />
            <Column
              field="icerik"
              :header="t('cariKart.icerik')"
            />
            <Column
              field="olusturmaTarihi"
              :header="t('cariKart.tarih')"
            >
              <template #body="{ data }">
                {{ formatDateTime(data.olusturmaTarihi) }}
              </template>
            </Column>
          </DataTable>
        </TabPanel>

        <TabPanel :header="t('cariKart.ozelFiyatlar')">
          <DataTable
            :value="kart.ozelFiyatlar"
            responsive-layout="scroll"
            :empty-message="t('cariKart.kayitYok')"
          >
            <Column
              field="stokKodu"
              :header="t('cariKart.stokKodu')"
            />
            <Column
              field="stokAd"
              :header="t('cariKart.stokAd')"
            />
            <Column
              field="fiyat"
              :header="t('cariKart.fiyat')"
            >
              <template #body="{ data }">
                {{ formatCurrency(data.fiyat) }}
              </template>
            </Column>
          </DataTable>
        </TabPanel>

        <TabPanel :header="t('cariKart.taksitler')">
          <DataTable
            :value="kart.taksitler"
            responsive-layout="scroll"
            :empty-message="t('cariKart.kayitYok')"
          >
            <Column
              :header="t('cariKart.taksitNo')"
            >
              <template #body="{ data }">
                {{ data.taksitNo }}/{{ data.taksitSayisi }}
              </template>
            </Column>
            <Column
              field="vadeTarihi"
              :header="t('cariKart.vade')"
            >
              <template #body="{ data }">
                {{ formatDate(data.vadeTarihi) }}
              </template>
            </Column>
            <Column
              field="kurum"
              :header="t('cariKart.kurum')"
            >
              <template #body="{ data }">
                {{ data.kurum || '-' }}
              </template>
            </Column>
            <Column
              field="tutar"
              :header="t('cariKart.tutar')"
            >
              <template #body="{ data }">
                {{ formatCurrency(data.tutar) }}
              </template>
            </Column>
            <Column :header="t('cariKart.durum')">
              <template #body="{ data }">
                <Tag
                  :severity="taksitSeverity(data)"
                  :value="taksitEtiket(data)"
                />
              </template>
            </Column>
          </DataTable>
        </TabPanel>
      </TabView>
    </div>
  </Dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { cariHesapAPI } from '../api/index.js'
import { formatCurrency, formatTarih as formatDate, formatTarihSaat as formatDateTime } from '../utils/format.js'

const props = defineProps({
  visible: { type: Boolean, default: false },
  cariId: { type: Number, default: null }
})
const emit = defineEmits(['update:visible'])

const { t } = useI18n()
const kart = ref(null)
const yukleniyor = ref(false)

const ozetSatirlari = computed(() => {
  const o = kart.value?.ozet
  if (!o) return []
  return [
    { etiket: t('cariKart.faturaSayisi'), deger: o.faturaSayisi },
    { etiket: t('cariKart.faturaToplam'), deger: formatCurrency(o.faturaToplam) },
    { etiket: t('cariKart.kalan'), deger: formatCurrency(o.kalanTutar) },
    { etiket: t('cariKart.siparisSayisi'), deger: o.siparisSayisi },
    { etiket: t('cariKart.siparisToplam'), deger: formatCurrency(o.siparisToplam) },
    { etiket: t('cariKart.iadeSayisi'), deger: o.iadeSayisi },
    { etiket: t('cariKart.iadeToplam'), deger: formatCurrency(o.iadeToplam) },
    { etiket: t('cariKart.tahsilatToplam'), deger: formatCurrency(o.tahsilatToplam) }
  ]
})

const taksitSeverity = (k) => {
  if (k.odemeDurumu === 'ODENDI') return 'success'
  if (k.gecikmeGunu > 0) return 'danger'
  return 'warning'
}

const taksitEtiket = (k) => {
  if (k.odemeDurumu === 'ODENDI') return t('cariKart.odendi')
  if (k.gecikmeGunu > 0) return t('cariKart.gecikmis')
  return t('cariKart.bekliyor')
}

const yukle = async () => {
  if (!props.cariId) return
  yukleniyor.value = true
  try {
    const r = await cariHesapAPI.kart(props.cariId)
    kart.value = r.data
  } catch {
    kart.value = null
  }
  yukleniyor.value = false
}

watch(
  () => props.visible,
  (v) => {
    if (v) yukle()
  }
)
</script>

<style scoped>
.kart-yukleniyor {
  padding: 40px;
  text-align: center;
  color: var(--text-secondary, #94a3b8);
}

.kart-ust {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.kart-kimlik h3 {
  margin: 0 0 6px 0;
}

.kart-meta {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  font-size: 13px;
  color: var(--text-secondary, #94a3b8);
}

.kredi-kart {
  padding: 14px 18px;
  border-radius: 12px;
  background: var(--surface-ground, #0f172a);
  margin-bottom: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.kredi-satir {
  display: flex;
  gap: 32px;
  flex-wrap: wrap;
}

.kredi-alan {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.kredi-alan span {
  font-size: 12px;
  color: var(--text-secondary, #94a3b8);
}

.kredi-alan strong {
  font-size: 16px;
}

.limit-asimi {
  color: #ef4444;
}
</style>
