<template>
  <Dialog
    :visible="visible"
    :header="t('cariKart.baslik')"
    modal
    :style="{ width: '960px', maxWidth: '95vw' }"
    @update:visible="emit('update:visible', $event)"
  >
    <div
      v-if="yukleniyor"
      class="kart-yukleniyor"
    >
      <i class="pi pi-spin pi-spinner" /> {{ t('cariKart.yukleniyor') }}
    </div>

    <div v-else-if="kart">
      <div class="kart-kimlik-kart">
        <div class="kart-kimlik-sol">
          <img
            v-if="avatarUrl"
            :src="avatarUrl"
            class="kart-avatar"
            alt=""
          >
          <span
            v-else
            class="kart-avatar-yok"
          ><i class="pi pi-user" /></span>
          <div class="kart-kimlik-metin">
            <div class="kart-ad-satir">
              <h3>{{ kart.cariAd }}</h3>
              <Tag
                v-if="kart.tur"
                :severity="turSeverity(kart.tur)"
                :value="kart.tur"
              />
              <Tag
                v-if="kart.aktif === false"
                severity="secondary"
                :value="t('common.passive')"
              />
            </div>
            <div class="kart-meta">
              <span v-if="kart.telefon"><i class="pi pi-phone" /> {{ kart.telefon }}</span>
              <span v-if="kart.email"><i class="pi pi-envelope" /> {{ kart.email }}</span>
              <span v-if="kart.temsilciAd"><i class="pi pi-user" /> {{ kart.temsilciAd }}</span>
            </div>
          </div>
        </div>
        <div class="kart-islemler">
          <Button
            :label="t('cariHesaplar.tahsilat')"
            icon="pi pi-money-bill"
            class="p-button-sm"
            data-test="cari-360-tahsilat"
            @click="emit('tahsilat', props.cari)"
          />
          <Button
            :label="t('cariHesaplar.yeniFatura')"
            icon="pi pi-file-plus"
            class="p-button-sm p-button-outlined"
            data-test="cari-360-fatura"
            @click="emit('fatura', props.cari)"
          />
          <Button
            :label="t('cariHesaplar.ekstreYazdir')"
            icon="pi pi-print"
            class="p-button-sm p-button-outlined"
            @click="emit('ekstre')"
          />
          <Button
            :label="t('cariHesaplar.adresler')"
            icon="pi pi-map-marker"
            class="p-button-sm p-button-outlined"
            @click="emit('adresler')"
          />
        </div>
      </div>

      <div class="kart-kpi">
        <div class="kpi-hucre">
          <span>{{ t('cariKart.krediLimiti') }}</span>
          <strong class="gizli-veri">{{ formatCurrency(kart.kredi?.krediLimiti) }}</strong>
        </div>
        <div class="kpi-hucre">
          <span>{{ t('cariKart.bakiye') }}</span>
          <strong
            class="gizli-veri"
            :class="{ 'bakiye-alacak': Number(kart.kredi?.bakiye) < 0, 'bakiye-odenecek': Number(kart.kredi?.bakiye) > 0 }"
          >{{ formatCurrency(kart.kredi?.bakiye) }}</strong>
        </div>
        <div class="kpi-hucre">
          <span>{{ t('cariKart.kullanilabilirKredi') }}</span>
          <strong
            class="gizli-veri"
            :class="{ 'limit-asimi': kart.kredi?.limitAsimi }"
          >{{ formatCurrency(kart.kredi?.kullanilabilirKredi) }}</strong>
        </div>
        <div class="kpi-hucre">
          <span>{{ t('cariKart.riskOrani') }}</span>
          <strong class="gizli-veri">{{ kart.kredi?.riskOrani != null ? kart.kredi.riskOrani + '%' : '-' }}</strong>
        </div>
        <div class="kpi-hucre">
          <span>{{ t('cariKart.sonFatura') }}</span>
          <strong>{{ sonFaturaTarihi }}</strong>
        </div>
        <Tag
          v-if="kart.kredi?.limitAsimi"
          severity="danger"
          :value="t('cariKart.limitAsimi')"
          class="kart-limit-tag"
        />
      </div>

      <TabView scrollable>
        <TabPanel :header="t('cariKart.ozet')">
          <div class="kart-ozet">
            <section class="kart-ozet-kutu">
              <h4>{{ t('cariKart.finansalOzet') }}</h4>
              <div class="kart-ozet-statlar">
                <div
                  v-for="s in ozetKutulari"
                  :key="s.etiket"
                  class="kart-stat"
                >
                  <span>{{ s.etiket }}</span>
                  <strong :class="{ negatif: s.negatif }">{{ s.deger }}</strong>
                </div>
              </div>
            </section>

            <section class="kart-ozet-kutu">
              <h4>{{ t('cariKart.sonHareketler') }}</h4>
              <ul
                v-if="(kart.sonHareketler || []).length > 0"
                class="kart-mini-liste"
              >
                <li
                  v-for="h in kart.sonHareketler"
                  :key="h.id"
                >
                  <span class="kart-mini-tarih">{{ formatDate(h.tarih) }}</span>
                  <span class="kart-mini-metin">{{ hareketTuruEtiketi(h.tur) }} · {{ h.aciklama || '-' }}</span>
                  <strong :class="h.tur === 'TAHSILAT' ? 'pozitif' : 'negatif'">
                    {{ h.tur === 'TAHSILAT' ? '+' : '-' }}{{ formatCurrency(h.tutar) }}
                  </strong>
                </li>
              </ul>
              <p
                v-else
                class="kart-bos"
              >
                {{ t('cariKart.kayitYok') }}
              </p>
            </section>

            <section class="kart-ozet-kutu">
              <h4>{{ t('cariKart.sonFaturalar') }}</h4>
              <ul
                v-if="(kart.sonFaturalar || []).length > 0"
                class="kart-mini-liste"
              >
                <li
                  v-for="f in kart.sonFaturalar"
                  :key="f.id"
                >
                  <span class="kart-mini-tarih">{{ formatDate(f.tarih) }}</span>
                  <span class="kart-mini-metin">{{ f.faturaNumarasi }} · {{ f.durum }}</span>
                  <strong>{{ formatCurrency(f.genelToplam) }}</strong>
                </li>
              </ul>
              <p
                v-else
                class="kart-bos"
              >
                {{ t('cariKart.kayitYok') }}
              </p>
            </section>

            <section class="kart-ozet-kutu">
              <h4>{{ t('cariKart.notlar') }}</h4>
              <ul
                v-if="(kart.notlar || []).length > 0"
                class="kart-mini-liste"
              >
                <li
                  v-for="n in kart.notlar.slice(0, 4)"
                  :key="n.id"
                >
                  <span class="kart-mini-tarih">{{ formatDate(n.olusturmaTarihi) }}</span>
                  <span class="kart-mini-metin">{{ n.baslik || n.icerik }}</span>
                </li>
              </ul>
              <p
                v-else
                class="kart-bos"
              >
                {{ t('cariKart.kayitYok') }}
              </p>
            </section>
          </div>
        </TabPanel>

        <TabPanel :header="t('cariKart.sonHareketler')">
          <p class="kart-liste-notu">
            {{ t('cariKart.sonKayitNotu') }}
          </p>
          <AppDataTable
            :value="kart.sonHareketler"
            :paginator="false"
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
              :header="t('cariKart.hareketTuru')"
            >
              <template #body="{ data }">
                <span :class="['badge', data.tur === 'TAHSILAT' ? 'tahsilat' : 'odeme']">
                  {{ hareketTuruEtiketi(data.tur) }}
                </span>
              </template>
            </Column>
            <Column
              field="tutar"
              :header="t('cariKart.tutar')"
            >
              <template #body="{ data }">
                <span :class="data.tur === 'TAHSILAT' ? 'positive' : 'negative'">
                  {{ data.tur === 'TAHSILAT' ? '+' : '-' }}{{ formatCurrency(data.tutar) }}
                </span>
              </template>
            </Column>
            <Column
              field="aciklama"
              :header="t('cariKart.aciklama')"
            />
          </AppDataTable>
        </TabPanel>

        <TabPanel :header="t('cariKart.sonFaturalar')">
          <p class="kart-liste-notu">
            {{ t('cariKart.sonKayitNotu') }}
          </p>
          <AppDataTable
            :value="kart.sonFaturalar"
            :paginator="false"
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
          </AppDataTable>
        </TabPanel>

        <TabPanel :header="t('cariKart.sonSiparisler')">
          <p class="kart-liste-notu">
            {{ t('cariKart.sonKayitNotu') }}
          </p>
          <AppDataTable
            :value="kart.sonSiparisler"
            :paginator="false"
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
          </AppDataTable>
        </TabPanel>

        <TabPanel :header="t('cariKart.sonIadeler')">
          <p class="kart-liste-notu">
            {{ t('cariKart.sonKayitNotu') }}
          </p>
          <AppDataTable
            :value="kart.sonIadeler"
            :paginator="false"
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
          </AppDataTable>
        </TabPanel>

        <TabPanel :header="t('cariKart.firsatlar')">
          <AppDataTable
            :value="kart.firsatlar"
            :paginator="false"
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
          </AppDataTable>
        </TabPanel>

        <TabPanel :header="t('cariKart.notlar')">
          <AppDataTable
            :value="kart.notlar"
            :paginator="false"
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
          </AppDataTable>
        </TabPanel>

        <TabPanel :header="t('cariKart.ozelFiyatlar')">
          <AppDataTable
            :value="kart.ozelFiyatlar"
            :paginator="false"
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
          </AppDataTable>
        </TabPanel>

        <TabPanel :header="t('cariKart.taksitler')">
          <AppDataTable
            :value="kart.taksitler"
            :paginator="false"
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
          </AppDataTable>
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
  cariId: { type: Number, default: null },
  /** Liste satiri (avatar/tur icin); opsiyonel. */
  cari: { type: Object, default: null }
})
const emit = defineEmits(['update:visible', 'ekstre', 'adresler', 'tahsilat', 'fatura'])

const { t } = useI18n()
const kart = ref(null)
const yukleniyor = ref(false)

/** Liste satirindan avatar; kart API'sinde foto alani yok. */
const avatarUrl = computed(() => props.cari?.fotoThumbUrl || props.cari?.fotoUrl || null)

/** Cari turu -> Tag rengi (liste ekraniyla ayni kural). */
const turSeverity = (tur) =>
  tur === 'Musteri' ? 'info' : tur === 'Tedarikci' ? 'warn' : 'secondary'

const hareketTuruEtiketi = (tur) =>
  ({ TAHSILAT: t('hareketler.tahsilat'), ODEME: t('hareketler.odeme'), BORC: t('hareketler.borclandirma') }[tur] || tur)

const sonFaturaTarihi = computed(() => {
  const tarih = kart.value?.sonFaturalar?.[0]?.tarih
  return tarih ? formatDate(tarih) : '—'
})

const ozetKutulari = computed(() => {
  const o = kart.value?.ozet
  if (!o) return []
  return [
    { etiket: t('cariKart.faturaSayisi'), deger: o.faturaSayisi ?? 0 },
    { etiket: t('cariKart.faturaToplam'), deger: formatCurrency(o.faturaToplam) },
    { etiket: t('cariKart.kalan'), deger: formatCurrency(o.kalanTutar), negatif: Number(o.kalanTutar) > 0 },
    { etiket: t('cariKart.siparisSayisi'), deger: o.siparisSayisi ?? 0 },
    { etiket: t('cariKart.siparisToplam'), deger: formatCurrency(o.siparisToplam) },
    { etiket: t('cariKart.iadeSayisi'), deger: o.iadeSayisi ?? 0 },
    { etiket: t('cariKart.iadeToplam'), deger: formatCurrency(o.iadeToplam), negatif: Number(o.iadeToplam) > 0 },
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

.kart-kimlik-kart {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  padding: 16px;
  border: 1px solid var(--border);
  border-radius: 14px;
  background: var(--bg-secondary);
  margin-bottom: 14px;
}
.kart-kimlik-sol {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}
.kart-avatar {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid var(--border);
  flex: 0 0 auto;
}
.kart-avatar-yok {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  border: 1px dashed var(--border);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--text-muted);
  font-size: 20px;
  flex: 0 0 auto;
}
.kart-kimlik-metin {
  min-width: 0;
}
.kart-ad-satir {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.kart-ad-satir h3 {
  margin: 0;
  font-size: 18px;
}
.kart-meta {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  font-size: 13px;
  color: var(--text-secondary, #94a3b8);
  margin-top: 6px;
}
.kart-islemler {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.kart-kpi {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 10px;
  margin-bottom: 16px;
}
.kpi-hucre {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 12px 14px;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: var(--bg-primary);
}
.kpi-hucre span {
  font-size: 11px;
  font-weight: 600;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.kpi-hucre strong {
  font-size: 17px;
  font-weight: 800;
  color: var(--text-primary);
  font-variant-numeric: tabular-nums;
}
.kart-limit-tag {
  grid-column: 1 / -1;
  justify-self: start;
}
.limit-asimi {
  color: #ef4444;
}
.kpi-hucre strong.bakiye-alacak {
  color: var(--success);
}
.kpi-hucre strong.bakiye-odenecek {
  color: var(--danger);
}
.kart-ozet {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}
.kart-ozet-kutu {
  min-width: 0;
  padding: 12px 14px;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: var(--bg-primary);
}
.kart-ozet-kutu h4 {
  margin: 0 0 10px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  color: var(--text-muted);
}
.kart-ozet-statlar {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}
.kart-stat {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 7px 9px;
  border: 1px solid var(--border);
  border-radius: 9px;
  background: var(--bg-card);
}
.kart-stat span {
  font-size: 10.5px;
  color: var(--text-muted);
}
.kart-stat strong {
  font-size: 13px;
  color: var(--text-primary);
  font-variant-numeric: tabular-nums;
}
.kart-stat strong.negatif {
  color: var(--danger);
}
.kart-mini-liste {
  display: flex;
  flex-direction: column;
  gap: 7px;
  margin: 0;
  padding: 0;
  list-style: none;
}
.kart-mini-liste li {
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr) auto;
  align-items: center;
  gap: 8px;
  min-width: 0;
  font-size: 12px;
}
.kart-mini-tarih {
  color: var(--text-muted);
  font-variant-numeric: tabular-nums;
}
.kart-mini-metin {
  overflow: hidden;
  color: var(--text-secondary);
  text-overflow: ellipsis;
  white-space: nowrap;
}
.kart-mini-liste strong {
  font-variant-numeric: tabular-nums;
  color: var(--text-primary);
  white-space: nowrap;
}
.kart-mini-liste strong.pozitif {
  color: var(--success);
}
.kart-mini-liste strong.negatif {
  color: var(--danger);
}
.kart-bos {
  margin: 0;
  padding: 8px 0;
  font-size: 12px;
  color: var(--text-muted);
}
.kart-liste-notu {
  margin: 0 0 8px;
  font-size: 11px;
  color: var(--text-muted);
}
@media (max-width: 700px) {
  .kart-kimlik-kart {
    flex-direction: column;
  }
  .kart-ozet {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
