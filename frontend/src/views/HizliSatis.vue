<template>
  <div
    class="pos-container"
    :class="{ 'pos-buyuk': buyukYazi }"
  >
    <div class="pos-header">
      <div class="pos-marka">
        <span class="pos-marka-ikon"><i class="pi pi-bolt" /></span>
        <div class="pos-marka-metin">
          <strong>{{ t('hizliSatis.breadcrumb') }}</strong>
          <small>{{ authStore?.sirketAdi || 'RasPel ERP' }}</small>
        </div>
      </div>
      <div class="pos-header-sag">
        <div
          v-if="authStore?.kullanici"
          class="user-info"
        >
          <i class="pi pi-user" /> {{ authStore?.kullanici?.displayName || authStore?.kullanici?.username }}
        </div>
        <button
          type="button"
          class="pos-ikon-btn bugunku-btn"
          :title="t('hizliSatis.bugunkuSatislar', { n: gunlukSatislar.length })"
          :aria-label="t('hizliSatis.bugunkuSatislar', { n: gunlukSatislar.length })"
          @click="bugunkuDialog = true"
        >
          <i class="pi pi-clock" />
          <span
            v-if="gunlukSatislar.length > 0"
            class="bugunku-rozet"
          >{{ gunlukSatislar.length }}</span>
        </button>
        <button
          type="button"
          class="pos-tercih-btn"
          :class="{ aktif: buyukYazi || onayIste }"
          :title="t('hizliSatis.tercihler')"
          :aria-label="t('hizliSatis.tercihler')"
          @click="tercihPopover.toggle($event)"
        >
          <i class="pi pi-cog" />
        </button>
        <Popover ref="tercihPopover">
          <div class="tercih-panel">
            <div class="tercih-baslik">
              {{ t('hizliSatis.tercihler') }}
            </div>
            <label class="tercih-satir">
              <span class="tercih-metin">{{ t('hizliSatis.buyukYazi') }}</span>
              <ToggleSwitch
                v-model="buyukYazi"
                @change="buyukYaziKaydet"
              />
            </label>
            <label class="tercih-satir">
              <span class="tercih-metin">{{ t('hizliSatis.onayIste') }}</span>
              <ToggleSwitch
                v-model="onayIste"
                @change="onayIsteKaydet"
              />
            </label>
            <label class="tercih-satir">
              <span class="tercih-metin">{{ t('hizliSatis.otomatikYazdir') }}</span>
              <ToggleSwitch
                v-model="otomatikYazdir"
                @change="otomatikYazdirKaydet"
              />
            </label>
            <label class="tercih-satir">
              <span class="tercih-metin">{{ t('hizliSatis.kisayolIpucu') }}</span>
              <ToggleSwitch
                v-model="ipucuAcik"
                @change="ipucuKaydet"
              />
            </label>
          </div>
        </Popover>
        <button
          type="button"
          class="pos-ipucu-btn"
          :title="t('hizliSatis.kisayolIpucu')"
          :aria-label="t('hizliSatis.kisayolIpucu')"
          :aria-pressed="ipucuAcik"
          @click="ipucuToggle"
        >
          <i class="pi pi-question-circle" />
        </button>
      </div>
    </div>

    <div
      v-if="ipucuAcik"
      class="pos-ipucu"
    >
      <span><kbd>F1</kbd> {{ $t('hizliSatis.barkod') }}</span>
      <span><kbd>F2</kbd> {{ $t('hizliSatis.temizle') }}</span>
      <span><kbd>F3</kbd> {{ $t('hizliSatis.ipucuUrunAra') }}</span>
      <span><kbd>F4</kbd> {{ $t('hizliSatis.musteri') }}</span>
      <span><kbd>F5</kbd> {{ $t('hizliSatis.ipucuYeniMusteri') }}</span>
      <span><kbd>F6</kbd> {{ $t('hizliSatis.ipucuKamera') }}</span>
      <span><kbd>F9</kbd>/<kbd>F10</kbd> {{ $t('hizliSatis.odeme') }}</span>
      <span><kbd>N</kbd>/<kbd>K</kbd>/<kbd>H</kbd> {{ $t('hizliSatis.ipucuYontem') }}</span>
      <span><kbd>↑</kbd>/<kbd>↓</kbd> {{ $t('hizliSatis.ipucuSatir') }}</span>
      <span><kbd>Alt+↑/↓</kbd> {{ $t('hizliSatis.miktar') }}</span>
      <span><kbd>Del</kbd> {{ $t('hizliSatis.ipucuSil') }}</span>
      <button
        type="button"
        class="pos-ipucu-kapat"
        :aria-label="$t('common.close')"
        @click="ipucuKapat"
      >
        <i class="pi pi-times" />
      </button>
    </div>

    <div class="pos-body">
      <div class="pos-left">
        <div class="pos-arac-cubugu">
          <span class="p-input-icon-left arama-kutusu barkod-kutu">
            <i class="pi pi-barcode" />
            <InputText
              ref="barkodInputRef"
              v-model="globalBarkod"
              :placeholder="t('hizliSatis.barkodPlaceholder')"
              class="w-full"
              autofocus
              @keyup.enter="globalBarkodEkle"
            />
            <button
              type="button"
              class="alan-ikon-sag"
              :title="t('hizliSatis.barkodKamera')"
              :aria-label="t('hizliSatis.barkodKamera')"
              @click="scannerAcik = true"
            >
              <i class="pi pi-camera" />
            </button>
          </span>
          <span class="p-input-icon-left arama-kutusu">
            <i class="pi pi-search" />
            <InputText
              ref="aramaInputRef"
              v-model="seriNoArama"
              :placeholder="t('hizliSatis.aramaPlaceholder')"
              class="w-full"
            />
          </span>
          <button
            type="button"
            class="filtre-btn"
            :class="{ 'filtre-aktif': aktifFiltreSayisiPos > 0 }"
            :title="t('hizliSatis.filtreler')"
            :aria-label="t('hizliSatis.filtreler')"
            @click="filtrePopover.toggle($event)"
          >
            <i class="pi pi-filter" />
            <span class="filtre-btn-metin">{{ t('hizliSatis.filtreler') }}</span>
            <span
              v-if="aktifFiltreSayisiPos > 0"
              class="filtre-rozet"
            >{{ aktifFiltreSayisiPos }}</span>
          </button>
          <Popover ref="filtrePopover">
            <div class="filtre-panel">
              <div class="filtre-panel-baslik">
                {{ t('hizliSatis.filtreler') }}
              </div>
              <!-- Secenekler urun kartlarindaki degerlerden turetilir; ayrica
                   kategori/stok grubu tanimi yapmak gerekmez. -->
              <div class="filtre-alan">
                <label>{{ t('hizliSatis.kategori') }}</label>
                <Dropdown
                  v-model="filtreKategori"
                  :options="kategoriler"
                  :placeholder="t('hizliSatis.tumu')"
                  class="w-full"
                  show-clear
                />
              </div>
              <div class="filtre-alan">
                <label>{{ t('hizliSatis.marka') }}</label>
                <Dropdown
                  v-model="filtreMarka"
                  :options="markalar"
                  :placeholder="t('hizliSatis.tumu')"
                  class="w-full"
                  show-clear
                />
              </div>
              <div class="filtre-alan">
                <label>{{ t('hizliSatis.stokGrubu') }}</label>
                <Dropdown
                  v-model="filtreStokGrubu"
                  :options="stokGruplari"
                  :placeholder="t('hizliSatis.tumu')"
                  class="w-full"
                  show-clear
                />
              </div>
              <label class="filtre-alan filtre-alan-toggle">
                <span>{{ t('hizliSatis.sadeceStokta') }}</span>
                <ToggleSwitch v-model="sadeceStokta" />
              </label>
            </div>
          </Popover>
        </div>

        <!-- Aktif filtreler: tek tıkla temizlenebilir çipler -->
        <div
          v-if="aktifFiltreSayisiPos > 0"
          class="aktif-filtreler"
        >
          <span
            v-if="filtreKategori"
            class="aktif-filtre-cip"
          >
            <i class="pi pi-tag" /> {{ filtreKategori }}
            <button
              type="button"
              :aria-label="t('hizliSatis.filtreKaldir')"
              @click="filtreKategori = null"
            >
              <i class="pi pi-times" />
            </button>
          </span>
          <span
            v-if="filtreMarka"
            class="aktif-filtre-cip"
          >
            <i class="pi pi-star" /> {{ filtreMarka }}
            <button
              type="button"
              :aria-label="t('hizliSatis.filtreKaldir')"
              @click="filtreMarka = null"
            >
              <i class="pi pi-times" />
            </button>
          </span>
          <span
            v-if="filtreStokGrubu"
            class="aktif-filtre-cip"
          >
            <i class="pi pi-box" /> {{ filtreStokGrubu }}
            <button
              type="button"
              :aria-label="t('hizliSatis.filtreKaldir')"
              @click="filtreStokGrubu = null"
            >
              <i class="pi pi-times" />
            </button>
          </span>
          <button
            type="button"
            class="aktif-filtre-temizle"
            @click="filtreTemizle"
          >
            {{ t('hizliSatis.filtreTemizle') }}
          </button>
        </div>

        <div
          v-if="cokSatanlar.length > 0"
          class="cok-satanlar-section"
        >
          <div class="product-header">
            <h3><i class="pi pi-star-fill" /> {{ t('hizliSatis.cokSatanlar') }}</h3>
          </div>
          <div class="cok-satanlar-grid">
            <button
              v-for="u in cokSatanlar"
              :key="u.id"
              type="button"
              class="cok-satan-chip"
              :class="{ 'stok-yok': stokYokMu(u) }"
              :disabled="stokYokMu(u)"
              @click="sepeteEkle(u)"
            >
              <span class="cok-satan-ad">{{ u.ad }}</span>
              <span class="cok-satan-fiyat">{{ formatCurrency(satisFiyati(u)) }}</span>
            </button>
          </div>
        </div>

        <div class="product-section">
          <div class="product-header">
            <h3>
              <i class="pi pi-box" />
              {{ t('hizliSatis.mevcutUrunler') }}
              <span class="urun-sayaci">{{ filtrelenmisUrunler ? filtrelenmisUrunler.length : 0 }}</span>
            </h3>
            <div class="product-header-sag">
              <span class="siralama-etiket">{{ t('hizliSatis.sirala') }}</span>
              <Dropdown
                v-model="siralama"
                :options="siralamaSecenekleri"
                option-label="label"
                option-value="value"
                class="siralama-dropdown"
              />
            </div>
          </div>

          <!-- Kategori hızlı filtre çipleri -->
          <div
            v-if="kategoriCipleri.length > 1"
            class="kategori-cipler"
          >
            <button
              type="button"
              class="kategori-cip"
              :class="{ aktif: !filtreKategori }"
              @click="filtreKategori = null"
            >
              {{ t('hizliSatis.tumu') }}
            </button>
            <button
              v-for="k in kategoriCipleri"
              :key="k"
              type="button"
              class="kategori-cip"
              :class="{ aktif: filtreKategori === k }"
              @click="filtreKategori = k"
            >
              {{ k }}
            </button>
            <button
              v-if="gizliKategoriSayisi > 0 || tumKategorilerGoster"
              type="button"
              class="kategori-cip kategori-cip-daha"
              :class="{ aktif: tumKategorilerGoster }"
              @click="tumKategorilerGoster = !tumKategorilerGoster"
            >
              {{ tumKategorilerGoster ? t('hizliSatis.dahaAz') : '+' + gizliKategoriSayisi }}
            </button>
          </div>

          <div class="product-grid">
            <PosUrunKarti
              v-for="u in gorunenUrunler"
              :key="u.id"
              :urun="u"
              :cari-fiyat="cariFiyati(u.id)"
              :sepette-adet="sepetteAdet(u.id)"
              @sec="urunKartiTikla(u)"
            />
            <div
              v-if="filtrelenmisUrunler && filtrelenmisUrunler.length === 0"
              class="empty-products"
            >
              <i class="pi pi-inbox" />
              <p>{{ t('hizliSatis.urunBulunamadi') }}</p>
            </div>
          </div>
          <div
            v-if="filtrelenmisUrunler.length > gorunenUrunler.length"
            class="daha-fazla"
          >
            <Button
              :label="t('hizliSatis.dahaFazlaGoster', { n: Math.min(60, filtrelenmisUrunler.length - gorunenUrunler.length) })"
              icon="pi pi-angle-down"
              class="p-button-outlined"
              @click="gosterilenAdet += 60"
            />
          </div>
        </div>
      </div>

      <div class="pos-right">
        <Card class="siparis-kart">
          <template #content>
            <div class="pos-bolum">
              <div
                class="pos-bolum-baslik katlanir-baslik"
                :aria-expanded="musteriAcik"
                :title="musteriAcik ? t('hizliSatis.bolumKapat') : t('hizliSatis.bolumAc')"
                @click="musteriAcikDegistir"
              >
                <i class="pi pi-user" /> {{ t('hizliSatis.musteri') }}
                <i
                  class="pi katlanir-ok"
                  :class="musteriAcik ? 'pi-chevron-down' : 'pi-chevron-right'"
                />
              </div>
              <div
                v-show="musteriAcik"
                class="customer-field"
              >
                <SelectButton
                  v-model="musteriModu"
                  :options="musteriModlari"
                  option-label="label"
                  option-value="value"
                  :allow-empty="false"
                  class="w-full musteri-modu"
                />
                <template v-if="musteriModu === 'musteri'">
                  <AutoComplete
                    ref="musteriAutoRef"
                    v-model="musteriGiris"
                    :suggestions="musteriOnerileri"
                    option-label="ad"
                    :placeholder="t('hizliSatis.musteriAra')"
                    class="w-full"
                    @complete="musteriAra($event)"
                    @option-select="musteriSec"
                  >
                    <template #option="slotProps">
                      <div class="musteri-option">
                        {{ slotProps.option.ad }}
                        <span class="musteri-option-detay">{{
                          slotProps.option.vergiNo || slotProps.option.telefon
                        }}</span>
                      </div>
                    </template>
                  </AutoComplete>
                  <div
                    v-if="seciliMusteri"
                    class="secili-musteri-chip"
                  >
                    <i class="pi pi-user" />
                    <span class="secili-musteri-ad">{{ seciliMusteri.ad }}</span>
                    <button
                      type="button"
                      class="secili-musteri-sil"
                      :title="t('hizliSatis.musteriyiKaldir')"
                      @click="musteriTemizle"
                    >
                      <i class="pi pi-times" />
                    </button>
                  </div>
                  <div
                    v-if="musteriBakiyeUyarisi"
                    class="musteri-bakiye-uyari"
                    :class="musteriBakiyeUyarisi.seviye"
                  >
                    <i :class="musteriBakiyeUyarisi.seviye === 'danger' ? 'pi pi-exclamation-triangle' : 'pi pi-info-circle'" />
                    {{ musteriBakiyeUyarisi.mesaj }}
                  </div>
                  <div
                    v-if="degisimIadeId"
                    class="degisim-bilgi"
                  >
                    <i class="pi pi-shopping-cart" />
                    <span>{{ t('hizliSatis.degisimBilgi', { id: degisimIadeId }) }}</span>
                    <button
                      type="button"
                      class="degisim-kapat"
                      :title="t('common.close')"
                      @click="degisimIadeId = null"
                    >
                      <i class="pi pi-times" />
                    </button>
                  </div>
                  <Button
                    :label="t('hizliSatis.yeni')"
                    severity="secondary"
                    size="small"
                    @click="yeniMusteriDialog = true"
                  />
                </template>
              </div>
            </div>

            <!-- Teslimat (opsiyonel): sofor secilirse satistan sonra teslimat kaydi acilir -->
            <div class="pos-bolum">
              <button
                type="button"
                class="pos-bolum-baslik katlanir-baslik"
                :aria-expanded="teslimatAcik"
                :title="teslimatAcik ? t('hizliSatis.bolumKapat') : t('hizliSatis.bolumAc')"
                @click="teslimatAcikDegistir"
              >
                <span class="katlanir-sol">
                  <i
                    class="pi katlanir-ok"
                    :class="teslimatAcik ? 'pi-chevron-down' : 'pi-chevron-right'"
                  />
                  <i class="pi pi-truck" /> {{ t('hizliSatis.teslimat') }}
                </span>
                <span
                  v-if="!teslimatAcik && seciliSofor"
                  class="katlanir-rozet"
                >{{ teslimEdenEtiketi }}</span>
              </button>
              <template v-if="teslimatAcik">
                <p class="teslimat-ipucu">
                  {{ t('hizliSatis.teslimatIpucu') }}
                </p>
                <div class="teslim-eden-alan">
                  <label for="hizli-teslim-sofor">{{ t('hizliSatis.sofor') }}</label>
                  <Dropdown
                    id="hizli-teslim-sofor"
                    v-model="seciliSofor"
                    :options="soforler"
                    option-label="ad"
                    :loading="soforlerYukleniyor"
                    filter
                    :show-clear="true"
                    :placeholder="t('hizliSatis.soforSecin')"
                    class="w-full"
                  >
                    <template #option="s">
                      <div class="personel-opsiyon">
                        <i class="pi pi-user" />
                        <span>{{ s.option.ad }}</span>
                        <span
                          v-if="s.option.rol && s.option.rol !== 'DRIVER'"
                          class="sofor-rol-uyari"
                          :title="t('hizliSatis.soforRolUyari')"
                        >
                          <i class="pi pi-exclamation-triangle" />
                        </span>
                        <span
                          v-if="s.option.bekleyenTeslimatSayisi"
                          class="sofor-bekleyen"
                        >{{ s.option.bekleyenTeslimatSayisi }}</span>
                      </div>
                    </template>
                  </Dropdown>
                </div>
                <div
                  v-if="seciliSofor"
                  class="teslim-eden-alan"
                >
                  <label for="hizli-teslim-adres">
                    {{ t('hizliSatis.teslimatAdresi') }} <span class="zorunlu">*</span>
                  </label>
                  <InputText
                    id="hizli-teslim-adres"
                    v-model="teslimatAdresi"
                    :placeholder="t('hizliSatis.adresPlaceholder')"
                    class="w-full"
                  />
                </div>
                <div
                  v-if="seciliSofor"
                  class="teslim-eden-alan"
                >
                  <label>{{ t('hizliSatis.teslimDurumu') }}</label>
                  <SelectButton
                    v-model="teslimDurumu"
                    :options="teslimDurumSecenekleri"
                    option-label="label"
                    option-value="value"
                    :allow-empty="false"
                    class="w-full teslim-durum-secim"
                  />
                </div>
                <div
                  v-if="seciliSofor"
                  class="teslim-eden-alan"
                >
                  <InputText
                    v-model="teslimNotu"
                    :placeholder="t('hizliSatis.teslimNotuPlaceholder')"
                    class="w-full"
                  />
                </div>
              </template>
            </div>

            <div
              ref="sepetListeRef"
              class="pos-bolum sepet-bolum"
            >
              <div class="pos-bolum-baslik sepet-baslik">
                <button
                  type="button"
                  class="sepet-baslik-toggle"
                  :aria-expanded="sepetAcik"
                  @click="sepetAcikDegistir"
                >
                  {{ t('hizliSatis.siparisOzeti', { n: sepet ? sepet.length : 0 }) }}
                </button>
                <div class="sepet-baslik-btnler">
                  <Button
                    v-if="sepet && sepet.length"
                    icon="pi pi-save"
                    class="p-button-rounded p-button-text p-button-sm"
                    :title="t('hizliSatis.sepetiKaydet')"
                    @click="sepetKaydet"
                  />
                  <Button
                    v-if="kayitliSepetVar && sepet.length === 0"
                    icon="pi pi-folder-open"
                    class="p-button-rounded p-button-text p-button-sm"
                    :title="t('hizliSatis.kayitliSepetiYukle')"
                    @click="sepetYukle"
                  />
                  <Button
                    v-if="sepet && sepet.length"
                    :label="t('hizliSatis.temizle')"
                    icon="pi pi-trash"
                    severity="danger"
                    size="small"
                    @click.stop="sepetiGeriAlinabilirTemizle()"
                  />
                  <button
                    type="button"
                    class="katlanir-ikon-btn"
                    :aria-expanded="sepetAcik"
                    :title="sepetAcik ? t('hizliSatis.bolumKapat') : t('hizliSatis.bolumAc')"
                    :aria-label="sepetAcik ? t('hizliSatis.bolumKapat') : t('hizliSatis.bolumAc')"
                    @click="sepetAcikDegistir"
                  >
                    <i
                      class="pi katlanir-ok"
                      :class="sepetAcik ? 'pi-chevron-down' : 'pi-chevron-right'"
                    />
                  </button>
                </div>
              </div>
              <div
                v-if="geriAlSepet"
                class="geri-al-bar"
              >
                <span><i class="pi pi-trash" /> {{ t('hizliSatis.sepetTemizlendi') }}</span>
                <button
                  type="button"
                  @click="sepetGeriAl"
                >
                  <i class="pi pi-undo" /> {{ t('hizliSatis.geriAl') }}
                </button>
              </div>
              <div
                v-show="sepetAcik"
                class="sepet-icerik"
              >
                <div
                  v-if="sepet && sepet.length === 0"
                  class="sepet-bos"
                >
                  {{ t('hizliSatis.sepeteUrunEkle') }}
                </div>
                <div
                  v-for="(item, idx) in sepet"
                  :key="item.id"
                  class="sepet-item"
                  :class="{ 'aktif-satir': aktifSatir === idx, 'yeni-satir': vurguluId === item.id, 'surukleniyor': suruklenenIdx === idx }"
                  @click="aktifSatir = idx"
                  @dragover.prevent="suruklemeUzerine(idx)"
                  @drop.prevent="suruklemeBirak(idx)"
                >
                  <div class="sepet-ust">
                    <span
                      class="sepet-tutamac"
                      draggable="true"
                      :title="t('hizliSatis.siralaTutamac')"
                      @dragstart="suruklemeBasla(idx)"
                      @dragend="suruklemeBitir"
                    ><i class="pi pi-bars" /></span>
                    <span
                      class="sepet-kod"
                      :title="item.barkod"
                    >{{ item.barkod || item.stokKodu }}</span>
                    <span class="sepet-ad">{{ item.ad }}</span>
                    <span class="sepet-tutar">{{ formatCurrency(item.miktar * item.fiyat) }}</span>
                    <button
                      type="button"
                      class="sepet-sil"
                      :title="t('hizliSatis.kaldir')"
                      @click="sepetSil(idx)"
                    >
                      <i class="pi pi-times" />
                    </button>
                  </div>
                  <div class="sepet-kontroller">
                    <div class="sepet-adet-grup">
                      <button
                        type="button"
                        class="adet-btn"
                        :aria-label="t('hizliSatis.miktarAzalt')"
                        :title="t('hizliSatis.miktarAzalt')"
                        @click="miktarAzalt(idx)"
                      >
                        −
                      </button>
                      <input
                        v-model.number="item.miktar"
                        type="number"
                        min="1"
                        class="sepet-adet-input"
                        :aria-label="t('hizliSatis.adet')"
                        :title="t('hizliSatis.adet')"
                      >
                      <button
                        type="button"
                        class="adet-btn"
                        :aria-label="t('hizliSatis.miktarArtir')"
                        :title="t('hizliSatis.miktarArtir')"
                        @click="item.miktar++"
                      >
                        +
                      </button>
                    </div>
                    <select
                      v-model="item.fiyatTipi"
                      class="fiyat-tip-select"
                      @change="fiyatTipiDegisti(item)"
                    >
                      <option
                        v-for="f in item.fiyatlar"
                        :key="f.ad"
                        :value="f.ad"
                      >
                        {{ f.ad }}
                      </option>
                    </select>
                    <input
                      v-model.number="item.fiyat"
                      type="number"
                      step="0.01"
                      class="fiyat-giris-input"
                      :title="t('hizliSatis.birimFiyati')"
                    >
                  </div>
                  <div
                    v-if="item.sonAldigiFiyat"
                    class="sepet-son-alis"
                  >
                    <i class="pi pi-history" />
                    {{ seciliMusteri?.ad || $t('hizliSatis.musteri') }} {{ $t('hizliSatis.sonAlisOncesi') }}
                    <strong>{{ formatCurrency(item.sonAldigiFiyat) }}</strong>
                    {{ item.sonAldigiTarih ? '(' + formatDate(item.sonAldigiTarih) + ')' : '' }} {{ $t('hizliSatis.sonAlisSonrasi') }}
                  </div>
                </div>
                <hr class="ozet-ayrac">
                <button
                  type="button"
                  class="ozet-detay-btn"
                  :aria-expanded="detayAcik"
                  @click="detayAcikDegistir"
                >
                  <i
                    class="pi katlanir-ok"
                    :class="detayAcik ? 'pi-chevron-down' : 'pi-chevron-right'"
                  />
                  {{ detayAcik ? t('hizliSatis.detayGizle') : t('hizliSatis.detayGoster') }}
                </button>
                <div
                  v-show="detayAcik"
                  class="ozet-satir"
                >
                  <span>{{ t('hizliSatis.toplamFt3') }}</span>
                  <span>{{ toplamFt3.toFixed(2) }} ft³</span>
                </div>
                <div class="ozet-satir ozet-indirim-satir">
                  <span>{{ t('hizliSatis.indirim') }}</span>
                  <div class="ozet-indirim">
                    <SelectButton
                      v-model="indirimTipi"
                      :options="indirimTipleri"
                      option-label="label"
                      option-value="value"
                    />
                    <InputNumber
                      v-model="indirimDegeri"
                      :min="0"
                      :max="indirimTipi === 'yuzde' ? 100 : toplam"
                      :suffix="indirimTipi === 'yuzde' ? '%' : ' ₺'"
                      class="indirim-input"
                    />
                  </div>
                </div>
                <div class="ozet-satir ozet-genel">
                  <span>{{ t('hizliSatis.genelToplam') }}</span>
                  <span class="genel-toplam-deger">{{ formatCurrency(genelToplam) }}</span>
                </div>
              </div>
            </div>

            <div class="pos-bolum">
              <div
                class="pos-bolum-baslik katlanir-baslik"
                :aria-expanded="odemeAcik"
                :title="odemeAcik ? t('hizliSatis.bolumKapat') : t('hizliSatis.bolumAc')"
                @click="odemeAcikDegistir"
              >
                <i class="pi pi-wallet" /> {{ t('hizliSatis.odeme') }}
                <i
                  class="pi katlanir-ok"
                  :class="odemeAcik ? 'pi-chevron-down' : 'pi-chevron-right'"
                />
              </div>
              <div v-show="odemeAcik">
                <SelectButton
                  v-model="odemeDurumu"
                  :options="odemeTipleri"
                  option-label="label"
                  option-value="value"
                  class="w-full"
                />
                <div
                  v-if="odemeDurumu !== 'yok'"
                  class="odenen-satir"
                >
                  <label>{{ t('hizliSatis.odenenTutar') }}</label>
                  <InputNumber
                    v-model="odenenTutar"
                    :min="0"
                    :max="genelToplam"
                    mode="currency"
                    currency="TRY"
                    locale="tr-TR"
                    class="w-full"
                  />
                </div>

                <div
                  v-if="odemeDurumu !== 'yok' && odemeYontemi === 'NAKIT'"
                  class="odenen-satir"
                >
                  <label>{{ t('hizliSatis.alinanNakit') }}</label>
                  <InputNumber
                    v-model="alinanNakit"
                    :min="0"
                    mode="currency"
                    currency="TRY"
                    locale="tr-TR"
                    class="w-full"
                  />
                  <div
                    v-if="paraUstu > 0"
                    class="para-ustu"
                  >
                    <span>{{ t('hizliSatis.paraUstu') }}:</span>
                    <strong>{{ formatCurrency(paraUstu) }}</strong>
                  </div>
                  <div class="hizli-nakit">
                    <button
                      v-for="n in hizliNakit"
                      :key="n"
                      type="button"
                      class="hizli-nakit-btn"
                      @click="alinanNakit = (alinanNakit || 0) + n"
                    >
                      {{ n }}
                    </button>
                    <button
                      type="button"
                      class="hizli-nakit-btn tam"
                      @click="alinanNakit = genelToplam"
                    >
                      {{ t('hizliSatis.nakitTam') }}
                    </button>
                  </div>
                </div>

                <div
                  v-if="odemeDurumu !== 'yok'"
                  class="odenen-satir"
                >
                  <label>{{ t('hizliSatis.odemeYontemi') }}</label>
                  <div class="odeme-yontem-grid">
                    <button
                      v-for="y in odemeYontemleri"
                      :key="y.value"
                      type="button"
                      class="odeme-yontem-btn"
                      :class="{ active: odemeYontemi === y.value }"
                      @click="odemeYontemi = y.value"
                    >
                      <i :class="y.icon" />
                      {{ y.label }}
                    </button>
                  </div>
                </div>

                <div
                  v-if="odemeDurumu !== 'yok' && odemeYontemi === 'TAKSIT'"
                  class="taksit-panel"
                >
                  <div class="odenen-satir">
                    <label>{{ t('hizliSatis.taksitKurum') }}</label>
                    <InputText
                      v-model="taksitKurum"
                      :placeholder="t('hizliSatis.taksitKurumPlaceholder')"
                      class="w-full"
                    />
                  </div>
                  <div class="odenen-satir">
                    <label>{{ t('hizliSatis.taksitTutar') }}</label>
                    <InputNumber
                      v-model="taksitTutar"
                      :min="0"
                      :max="genelToplam"
                      mode="currency"
                      currency="TRY"
                      locale="tr-TR"
                      class="w-full"
                    />
                  </div>
                  <div class="odenen-satir">
                    <label>{{ t('hizliSatis.taksitSayisi') }}</label>
                    <InputNumber
                      v-model="taksitSayisi"
                      :min="1"
                      :max="60"
                      show-buttons
                      class="w-full"
                    />
                  </div>
                </div>

                <div class="odenen-satir">
                  <label>{{ t('hizliSatis.kasa') }}</label>
                  <Dropdown
                    v-model="seciliKasa"
                    :options="kasalar"
                    option-label="ad"
                    option-value="id"
                    :placeholder="t('hizliSatis.kasaSecin')"
                    class="w-full"
                  />
                </div>

                <div
                  v-if="odemeDurumu !== 'yok' && (odemeYontemi === 'KART' || odemeYontemi === 'HAVALE')"
                  class="odenen-satir"
                >
                  <label>{{ odemeYontemi === 'KART' ? t('hizliSatis.kartBankaAktar') : t('hizliSatis.havaleBanka') }}</label>
                  <Dropdown
                    v-model="seciliBanka"
                    :options="bankalar"
                    option-label="ad"
                    option-value="id"
                    :placeholder="t('hizliSatis.bankaSecin')"
                    show-clear
                    class="w-full"
                  />
                </div>

                <div
                  v-if="odemeDurumu !== 'yok' && odemeYontemi === 'KART'"
                  class="odenen-satir pos-secim"
                >
                  <label>{{ t('hizliSatis.posTerminali') }}</label>
                  <Dropdown
                    v-model="seciliPos"
                    :options="posTerminalleri"
                    option-label="ad"
                    option-value="id"
                    :placeholder="t('hizliSatis.posSecin')"
                    show-clear
                    class="w-full"
                  />
                  <small
                    v-if="seciliPosBilgi"
                    class="pos-komisyon-not"
                  >
                    {{ t('hizliSatis.posKomisyonNot', {
                      oran: seciliPosBilgi.komisyonOrani ?? 0,
                      komisyon: formatCurrency(hesaplananKomisyon)
                    }) }}
                  </small>
                </div>

                <div class="odeme-durum">
                  <Tag
                    :value="odemeDurumText"
                    :severity="odemeDurumSeverity"
                    class="w-full"
                  />
                </div>
                <div
                  v-if="kalanTutar > 0"
                  class="odeme-kalan"
                >
                  <span>{{ t('hizliSatis.kalan') }}:</span>
                  <span class="kalan-deger">{{ formatCurrency(kalanTutar) }}</span>
                </div>
              </div>
            </div>

            <div class="sticky-tamamla">
              <div class="fis-modu-satir">
                <span class="fis-modu-etiket"><i class="pi pi-print" /> {{ t('hizliSatis.fisModu') }}</span>
                <SelectButton
                  v-model="fisFiyatliGecici"
                  :options="fisModuSecenekleri"
                  option-label="label"
                  option-value="value"
                  size="small"
                  :allow-empty="false"
                />
              </div>
              <div class="sticky-tutar">
                <span>{{ t('hizliSatis.genelToplam') }}</span>
                <strong>{{ formatCurrency(genelToplam) }}</strong>
              </div>
              <Button
                :label="t('hizliSatis.satisiTamamla')"
                icon="pi pi-check"
                class="p-button-success w-full satis-buton"
                :loading="kaydediliyor"
                :disabled="sepet.length === 0 || (!anlikMusteri && !seciliMusteri)"
                @click="satisiTamamla"
              />
              <Button
                v-if="sonSatis"
                :label="t('hizliSatis.sonSatisiIptal')"
                icon="pi pi-undo"
                class="p-button-outlined p-button-danger w-full"
                @click="sonSatisiIptalEt"
              />
            </div>

            <div
              v-if="sepet.length > 0"
              class="pos-bolum"
            >
              <div class="pos-bolum-baslik fis-baslik-satir">
                <button
                  type="button"
                  class="katlanir-baslik katlanir-baslik-inline"
                  :aria-expanded="fisAcik"
                  :title="fisAcik ? t('hizliSatis.bolumKapat') : t('hizliSatis.bolumAc')"
                  @click="fisAcikDegistir"
                >
                  <span class="katlanir-sol">
                    <i
                      class="pi katlanir-ok"
                      :class="fisAcik ? 'pi-chevron-down' : 'pi-chevron-right'"
                    />
                    <i class="pi pi-print" /> {{ t('hizliSatis.fisOnizleme') }}
                  </span>
                </button>
                <div class="fis-ayarlar">
                  <Button
                    :label="t('hizliSatis.yazdirF9')"
                    icon="pi pi-print"
                    size="small"
                    @click="fisiYazdir(sonSatis?.faturaNumarasi, satisOzet?.fisModu)"
                  />
                  <Button
                    :label="t('hizliSatis.termal')"
                    icon="pi pi-send"
                    size="small"
                    severity="secondary"
                    outlined
                    @click="termalYazdir"
                  />
                </div>
              </div>
              <div
                v-show="fisAcik"
                class="fis-onizleme-kapsam"
              >
                <PosFisOnizleme
                  :sirket-logosu="sirketLogosu"
                  :sirket-adi="sirketAdi"
                  :simdiki-tarih="simdikiTarih"
                  :fis-no="fisNo"
                  :musteri-adi="musteriAdi"
                  :sepet="sepet"
                  :fis-fiyatli="fisFiyatli"
                  :toplam="toplam"
                  :indirim-degeri="indirimDegeri"
                  :indirim-tipi="indirimTipi"
                  :indirim-tutari="indirimTutari"
                  :genel-toplam="genelToplam"
                  :odenen-tutar="odenenTutar"
                  :kalan-tutar="kalanTutar"
                  :odeme-durum-text="odemeDurumText"
                  :fis-alt-notu="fisAltNotu"
                />
              </div>
            </div>
          </template>
        </Card>
      </div>
    </div>
  </div>


  <Dialog
    v-model:visible="yeniMusteriDialog"
    :header="t('hizliSatis.yeniCari')"
    :modal="true"
    :style="{ width: '520px' }"
    class="yeni-musteri-dialog"
  >
    <div class="ym-form-grid">
      <div class="field full-width">
        <label for="ym-ad">{{ t('hizliSatis.adFirma') }} <span class="required">*</span></label>
        <InputText
          id="ym-ad"
          v-model="yeniMusteri.ad"
          :placeholder="t('hizliSatis.adFirmaPlaceholder')"
          class="w-full"
        />
      </div>
      <div class="field">
        <label for="ym-telefon">{{ t('hizliSatis.telefon') }}</label>
        <InputText
          id="ym-telefon"
          v-model="yeniMusteri.telefon"
          placeholder="05XX XXX XX XX"
          class="w-full"
        />
      </div>
      <div class="field">
        <label for="ym-email">{{ t('hizliSatis.eposta') }}</label>
        <InputText
          id="ym-email"
          v-model="yeniMusteri.email"
          placeholder="ornek@domain.com"
          class="w-full"
        />
      </div>
      <div class="field">
        <label for="ym-vergi">{{ t('hizliSatis.vergiNo') }}</label>
        <InputText
          id="ym-vergi"
          v-model="yeniMusteri.vergiNo"
          :placeholder="t('hizliSatis.vergiPlaceholder')"
          class="w-full"
        />
      </div>
      <div class="field">
        <label for="ym-tur">{{ t('hizliSatis.cariTuru') }}</label>
        <Dropdown
          id="ym-tur"
          v-model="yeniMusteri.tur"
          :options="cariTurSecenekleri"
          option-label="label"
          option-value="value"
          :placeholder="$t('hizliSatis.musteriPlaceholder')"
          class="w-full"
        />
      </div>
      <div class="field full-width">
        <label for="ym-adres">{{ t('hizliSatis.adres') }}</label>
        <Textarea
          id="ym-adres"
          v-model="yeniMusteri.adres"
          rows="2"
          :placeholder="t('hizliSatis.adresPlaceholder')"
          class="w-full"
        />
      </div>
    </div>
    <template #footer>
      <div class="dialog-footer-btns">
        <Button
          :label="t('common.cancel')"
          icon="pi pi-times"
          class="p-button-text"
          @click="yeniMusteriDialog = false"
        />
        <Button
          :label="t('hizliSatis.kaydetSec')"
          icon="pi pi-check"
          class="p-button-primary"
          :loading="musteriKaydediliyor"
          @click="musteriKaydet"
        />
      </div>
    </template>
  </Dialog>

  <BarcodeScannerModal
    v-model:visible="scannerAcik"
    @scan="barkodTarandi"
  />

  <Dialog
    v-model:visible="onayDialog"
    :header="t('hizliSatis.onayBaslik')"
    :modal="true"
    style="width: 380px"
  >
    <div class="satis-onay">
      <i class="pi pi-question-circle satis-onay-ikon" />
      <p class="satis-onay-metin">
        {{ t('hizliSatis.onayMetin') }}
      </p>
      <p class="satis-onay-tutar">
        {{ t('hizliSatis.toplam') }}: <strong>{{ formatCurrency(genelToplam) }}</strong>
      </p>
    </div>
    <template #footer>
      <Button
        :label="t('hizliSatis.onayIptal')"
        severity="secondary"
        text
        @click="satisOnayIptal"
      />
      <Button
        :label="t('hizliSatis.onayOnayla')"
        icon="pi pi-check"
        @click="satisOnayla"
      />
    </template>
  </Dialog>

  <PosBugunkuSatislarDialog
    :visible="bugunkuDialog"
    :satislar="gunlukSatislar"
    @update:visible="bugunkuDialog = $event"
    @yenile="gunlukSatislariYukle"
    @goruntule="bugunkuSatisGoruntule"
  />

  <PosSatisOzetDialog
    :visible="satisOzetDialog"
    :satis-ozet="satisOzet"
    @update:visible="satisOzetDialog = $event"
    @yeni-satis="yeniSatisaBasla"
  />

  <Dialog
    v-model:visible="hizliUrunDialog"
    :header="t('hizliSatis.hizliUrunBaslik')"
    :modal="true"
    style="width: 420px"
  >
    <div class="form-grup">
      <label>{{ t('stoklar.barkod') }}</label>
      <InputText
        v-model="hizliUrun.barkod"
        class="w-full"
      />
    </div>
    <div class="form-grup">
      <label>{{ t('hizliSatis.urunAdi') }}</label>
      <InputText
        v-model="hizliUrun.ad"
        class="w-full"
        autofocus
      />
    </div>
    <div class="kurulum-iki-kolon">
      <div class="form-grup">
        <label>{{ t('stoklar.satisFiyati') }}</label>
        <InputNumber
          v-model="hizliUrun.fiyat"
          mode="currency"
          currency="TRY"
          locale="tr-TR"
          class="w-full"
        />
      </div>
      <div class="form-grup">
        <label>{{ t('hizliSatis.miktar') }}</label>
        <InputNumber
          v-model="hizliUrun.miktar"
          :min="0"
          class="w-full"
        />
      </div>
    </div>
    <template #footer>
      <Button
        :label="t('common.cancel')"
        class="p-button-text"
        @click="hizliUrunDialog = false"
      />
      <Button
        :label="t('hizliSatis.kaydetSec')"
        icon="pi pi-check"
        :loading="hizliUrunKaydediliyor"
        @click="hizliUrunKaydet"
      />
    </template>
  </Dialog>
</template>

<script setup>
import { ref, computed, watch, nextTick, onMounted, onUnmounted } from 'vue'
import { unwrapList } from '../api/utils/unwrap.js'
import { useToast } from 'primevue/usetoast'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useAuthStore } from '../stores/authStore.js'
import { useCariHesapStore } from '../stores/cariHesapStore.js'
import { useStokStore } from '../stores/stokStore.js'
import { useMarka } from '../composables/useMarka.js'
import { useI18n } from 'vue-i18n'
import BarcodeScannerModal from '../components/BarcodeScannerModal.vue'
import PosFisOnizleme from '../components/PosFisOnizleme.vue'
import PosSatisOzetDialog from '../components/PosSatisOzetDialog.vue'
import PosBugunkuSatislarDialog from '../components/PosBugunkuSatislarDialog.vue'
import PosUrunKarti from '../components/PosUrunKarti.vue'
import { faturaAPI, cariHesapAPI, stokAPI, kasaAPI, bankaAPI, sirketAPI, posAPI, teslimatAPI } from '../api/index.js'
import { useOfflineSatisKuyrugu } from '../composables/useOfflineSatisKuyrugu.js'
import AutoComplete from 'primevue/autocomplete'
import SelectButton from 'primevue/selectbutton'
import { useKisayollar } from '../composables/useKisayollar.js'
import { formatCurrency, formatDate, formatDateTime, getLocalDateString } from '../utils/format.js'
import { escPosFisiUret, escPosYazdir } from '../utils/escpos.js'
import { escapeHtml } from '../utils/escapeHtml.js'
import { fisPenceresiAcVeYazdir } from '../utils/fisYazdir.js'
import { satisPayloadUret } from '../utils/satisPayload.js'
import { useRoute, useRouter } from 'vue-router'
import { useConfirm } from 'primevue/useconfirm'

const toast = useToast()
const toastBildirim = useToastBildirim()
const authStore = useAuthStore()
const cariHesapStore = useCariHesapStore()
const stokStore = useStokStore()
const offlineKuyruk = useOfflineSatisKuyrugu()
const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const confirm = useConfirm()

// Değişim akışı: İadeler ekranından "Yeni Satışa Geç" ile gelindiğinde gösterilir.
const degisimIadeId = ref(null)
const { sirketLogosu } = useMarka()

const offlineKuyruguSenkronizeEt = async () => {
  try {
    const gonderilen = await offlineKuyruk.senkronizeEt(
      (s, anahtar) => faturaAPI.create(s, anahtar),
      (meta, yanit) => teslimatKaydiniOlustur(yanit?.data?.id, meta)
    )
    if (gonderilen > 0) {
      toast.add({ severity: 'success', summary: t('hizliSatis.senkronizeEdildi'), detail: t('hizliSatis.satisGonderildi', { n: gonderilen }), life: 3000 })
    }
  } catch {
    /* empty */
  }
}

useKisayollar({
  kaydet: () => satisiTamamla(),
  iptal: () => {
    if (yeniMusteriDialog.value) yeniMusteriDialog.value = false
    else if (scannerAcik.value) scannerAcik.value = false
    else if (satisOzetDialog.value) satisOzetDialog.value = false
  },
  yazdir: () => fisiYazdir()
})

const girdideMi = (e) => {
  const el = e.target
  return !!el && (el.tagName === 'INPUT' || el.tagName === 'TEXTAREA' || el.tagName === 'SELECT' || el.isContentEditable)
}

const odakla = (r) => {
  r.value?.$el?.focus?.() || r.value?.focus?.()
}

// Hizli Satis klavye kisayollari. Capture fazinda calisir; boylece global F2/F4
// (App.vue) preventDefault sayesinde devreye girmez.
const handlePosKeys = (e) => {
  if (e.ctrlKey || e.metaKey) return

  if (e.key === 'F1') {
    e.preventDefault(); odakla(barkodInputRef); return
  }
  if (e.key === 'F2') {
    e.preventDefault()
    sepetiGeriAlinabilirTemizle()
    toast.add({ severity: 'info', summary: t('common.toastInfo'), detail: t('hizliSatis.sepetTemizlendi'), life: 2000 })
    return
  }
  if (e.key === 'F3') {
    e.preventDefault(); odakla(aramaInputRef); return
  }
  if (e.key === 'F4') {
    e.preventDefault()
    musteriModu.value = 'musteri'
    nextTick(() => odakla(musteriAutoRef))
    return
  }
  if (e.key === 'F5') {
    e.preventDefault(); yeniMusteriDialog.value = true; return
  }
  if (e.key === 'F6') {
    e.preventDefault(); scannerAcik.value = true; return
  }
  if (e.key === 'F9' || e.key === 'F10') {
    e.preventDefault()
    odemeDurumu.value = e.key === 'F9' ? 'tam' : 'yarim'
    if (sepet.value.length) satisiTamamla()
    return
  }
  if (e.altKey && (e.key === 'ArrowUp' || e.key === 'ArrowDown')) {
    if (aktifSatir.value >= 0 && sepet.value[aktifSatir.value]) {
      e.preventDefault()
      if (e.key === 'ArrowUp') sepet.value[aktifSatir.value].miktar++
      else miktarAzalt(aktifSatir.value)
    }
    return
  }

  // Sepette satirlar arasi gezinme (yazarken degil)
  if (!girdideMi(e) && sepet.value.length && (e.key === 'ArrowUp' || e.key === 'ArrowDown')) {
    e.preventDefault()
    const yon = e.key === 'ArrowDown' ? 1 : -1
    let idx = aktifSatir.value
    if (idx < 0) idx = yon > 0 ? 0 : sepet.value.length - 1
    else idx = Math.min(sepet.value.length - 1, Math.max(0, idx + yon))
    aktifSatir.value = idx
    return
  }

  // Aktif satirin miktar alanina odaklan
  if (!girdideMi(e) && e.key === 'Enter') {
    if (aktifSatir.value >= 0 && sepet.value[aktifSatir.value]) {
      e.preventDefault()
      odaklaAktifAdet()
    }
    return
  }

  // Harf kisayollari yazarken tetiklenmesin
  if (girdideMi(e)) return

  if (e.key === 'Delete' && aktifSatir.value >= 0 && sepet.value[aktifSatir.value]) {
    e.preventDefault(); sepetSil(aktifSatir.value); return
  }

  const k = e.key.toLowerCase()
  if (k === 'n') {
    e.preventDefault(); odemeYontemi.value = 'NAKIT'
  } else if (k === 'k') {
    e.preventDefault(); odemeYontemi.value = 'KART'
  } else if (k === 'h') {
    e.preventDefault(); odemeYontemi.value = 'HAVALE'
  }
}

const odaklaAktifAdet = () => {
  const el = sepetListeRef.value?.querySelector?.('.sepet-item.aktif-satir .sepet-adet-input')
  el?.focus?.()
  el?.select?.()
}

const ipucuToggle = () => {
  ipucuAcik.value = !ipucuAcik.value
  localStorage.setItem('raspel_pos_ipucu_acik', String(ipucuAcik.value))
}

const ipucuKaydet = () => {
  localStorage.setItem('raspel_pos_ipucu_acik', String(ipucuAcik.value))
}

const ipucuKapat = () => {
  ipucuAcik.value = false
  localStorage.setItem('raspel_pos_ipucu_acik', 'false')
}

// POS kullanılabilirlik tercihleri (varsayılan: kapalı). Yaşlı/uzak mesafeden
// kullanan personel için büyük yazı; kazara satışı önlemek için onay adımı.
const buyukYazi = ref(localStorage.getItem('raspel_pos_buyuk_yazi') === 'true')
const onayIste = ref(localStorage.getItem('raspel_pos_onay_iste') === 'true')
// Satış sonrası fişin otomatik yazdırılması (varsayılan açık).
const otomatikYazdir = ref(localStorage.getItem('raspel_pos_otomatik_yazdir') !== 'false')
const otomatikYazdirKaydet = () => {
  localStorage.setItem('raspel_pos_otomatik_yazdir', String(otomatikYazdir.value))
}

// Katlanabilir bolum tercihleri (varsayilan: yalnizca musteri ACik; sepet/odeme/
// teslimat/fis KAPALI; kullanici secimi hatirlanir).
const musteriAcik = ref(localStorage.getItem('raspel_pos_musteri_acik') !== 'false')
// Varsayilan ACIK: kasiyer taradigi urunleri ve secili odeme yontemini gormeli.
const sepetAcik = ref(localStorage.getItem('raspel_pos_sepet_acik') !== 'false')
const odemeAcik = ref(localStorage.getItem('raspel_pos_odeme_acik') !== 'false')
const teslimatAcik = ref(localStorage.getItem('raspel_pos_teslimat_acik') === 'true')
const fisAcik = ref(localStorage.getItem('raspel_pos_fis_acik') === 'true')
const detayAcik = ref(localStorage.getItem('raspel_pos_detay_acik') === 'true')

// Bugunku satislar: sag sutunda yer kaplamasin diye dialog'da gosterilir.
const bugunkuDialog = ref(false)

const musteriAcikDegistir = () => {
  musteriAcik.value = !musteriAcik.value
  localStorage.setItem('raspel_pos_musteri_acik', String(musteriAcik.value))
}
const sepetAcikDegistir = () => {
  sepetAcik.value = !sepetAcik.value
  localStorage.setItem('raspel_pos_sepet_acik', String(sepetAcik.value))
}
const odemeAcikDegistir = () => {
  odemeAcik.value = !odemeAcik.value
  localStorage.setItem('raspel_pos_odeme_acik', String(odemeAcik.value))
}
const teslimatAcikDegistir = () => {
  teslimatAcik.value = !teslimatAcik.value
  localStorage.setItem('raspel_pos_teslimat_acik', String(teslimatAcik.value))
}
const fisAcikDegistir = () => {
  fisAcik.value = !fisAcik.value
  localStorage.setItem('raspel_pos_fis_acik', String(fisAcik.value))
}
const detayAcikDegistir = () => {
  detayAcik.value = !detayAcik.value
  localStorage.setItem('raspel_pos_detay_acik', String(detayAcik.value))
}

const buyukYaziKaydet = () => {
  localStorage.setItem('raspel_pos_buyuk_yazi', String(buyukYazi.value))
}

const onayIsteKaydet = () => {
  localStorage.setItem('raspel_pos_onay_iste', String(onayIste.value))
}

const onayDialog = ref(false)
const onayBekleyenSatis = ref(false)

onMounted(() => {
  window.addEventListener('keydown', handlePosKeys, true)
  window.addEventListener('online', offlineKuyruguSenkronizeEt)
  if (navigator.onLine) offlineKuyruguSenkronizeEt()
})

onUnmounted(() => {
  window.removeEventListener('keydown', handlePosKeys, true)
  window.removeEventListener('online', offlineKuyruguSenkronizeEt)
})

const sirketAdi = computed(() => authStore.sirketAdi || '')

const seriNoArama = ref('')
const siralama = ref('ad')
const gosterilenAdet = ref(60)
const globalBarkod = ref('')
const scannerAcik = ref(false)
// Araç çubuğu popover'ları (filtreler + tercihler)
const filtrePopover = ref(null)
const tercihPopover = ref(null)
const barkodInputRef = ref(null)
const aramaInputRef = ref(null)
const musteriAutoRef = ref(null)
const aktifSatir = ref(-1)
const sepetListeRef = ref(null)
const ipucuAcik = ref(localStorage.getItem('raspel_pos_ipucu_acik') === 'true')
const hizliUrunDialog = ref(false)
const hizliUrun = ref({ barkod: '', ad: '', fiyat: 0, miktar: 1 })
const hizliUrunKaydediliyor = ref(false)

const globalBarkodEkle = () => {
  const barkod = globalBarkod.value.trim()
  if (!barkod) return
  barkodTarandi(barkod)
  globalBarkod.value = ''
  // Odak tekrar bu inputa gelsin ki USB tarayıcı kesintisiz okusun
  barkodInputRef.value?.$el?.focus?.() || barkodInputRef.value?.focus?.()
}

const barkodTarandi = async (barkod) => {
  if (!barkod) return
  // Etiketlerde barkod alani bos urunlerde stok kodu kodlanir; arama her ikisini
  // ve seri numarasini kapsar.
  const eslesir = (s) => s.barkod === barkod || s.seriNo === barkod || s.stokKodu === barkod
  let urun = stokStore.stoklar.find(eslesir)
  if (urun) {
    sepeteEkle(urun)
    toast.add({ severity: 'success', summary: t('hizliSatis.urunEklendi'), detail: urun.ad, life: 2000 })
  } else {
    // Sunucuda ara (büyük envanterde tümü yüklenmemiş olabilir)
    try {
      const r = await stokAPI.ara(barkod)
      const bulunan = (r.data || []).find(eslesir)
      if (bulunan) {
        sepeteEkle(bulunan)
        toast.add({ severity: 'success', summary: t('hizliSatis.urunEklendi'), detail: bulunan.ad, life: 2000 })
        return
      }
    } catch {
      /* sunucu araması başarısız olabilir */
    }
    toast.add({ severity: 'warn', summary: t('hizliSatis.bulunamadi'), detail: t('hizliSatis.barkodluUrunBulunamadi', { barkod }), life: 3000 })
    hizliUrun.value = { barkod, ad: '', fiyat: 0, miktar: 1 }
    hizliUrunDialog.value = true
  }
}

const filtreKategori = ref(null)
const filtreMarka = ref(null)
const filtreStokGrubu = ref(null)
const sadeceStokta = ref(false)
const tumKategorilerGoster = ref(false)

const filtreTemizle = () => {
  filtreKategori.value = null
  filtreMarka.value = null
  filtreStokGrubu.value = null
}

// Araç çubuğundaki filtre rozeti: kaç filtre aktif?
const aktifFiltreSayisiPos = computed(
  () => (filtreKategori.value ? 1 : 0) + (filtreMarka.value ? 1 : 0) + (filtreStokGrubu.value ? 1 : 0)
)

const seciliMusteri = ref(null)
const musteriGiris = ref('')
const seciliSofor = ref(null)
const soforler = ref([])
const soforlerYukleniyor = ref(false)
const teslimatAdresi = ref('')
const teslimDurumu = ref('BEKLIYOR')
const teslimNotu = ref('')
const musteriModu = ref('perakende')
const cariTurSecenekleri = computed(() => [
  { label: t('cariTur.musteri'), value: 'Musteri' },
  { label: t('cariTur.tedarikci'), value: 'Tedarikci' },
  { label: t('cariTur.herIkisi'), value: 'Her Ikisi' }
])
const musteriModlari = computed(() => [
  { label: t('hizliSatis.perakende'), value: 'perakende', icon: 'pi pi-shopping-cart' },
  { label: t('hizliSatis.musteri'), value: 'musteri', icon: 'pi pi-users' }
])
const anlikMusteri = computed(() => musteriModu.value === 'perakende')

watch(musteriModu, (mod) => {
  if (mod === 'perakende') {
    seciliMusteri.value = null
    musteriGiris.value = ''
  }
})
const musteriOnerileri = ref([])
const yeniMusteriDialog = ref(false)
const yeniMusteri = ref({ ad: '', telefon: '', email: '', adres: '', vergiNo: '', tur: 'Musteri' })
const musteriKaydediliyor = ref(false)

const sepet = ref([])
const kaydediliyor = ref(false)
const fisNo = ref('')
const fisFiyatli = ref(localStorage.getItem('raspel_fis_fiyatli') !== 'false')
// Fiş fiyatlı/fiyatsız seçimi satış başına geçicidir; satış tamamlanınca sunucu ayarına döner.
const fisFiyatliGecici = ref(fisFiyatli.value)
const fisAltNotu = ref(localStorage.getItem('raspel_fis_notu') || t('hizliSatis.fisAltNotVarsayilan'))
const fisGenislik = ref(localStorage.getItem('raspel_fis_genislik') || '80')

// Sekmeler arası canlı senkron: Ayarlar'da değişince POS'a anında yansır
const dinleyici = (e) => {
  if (e.key === 'raspel_fis_fiyatli' && e.newValue !== null) {
    fisFiyatli.value = e.newValue !== 'false'
    // Kullanıcı bu satış için farklı bir mod seçmediyse varsayılan güncellenir.
    if (fisFiyatliGecici.value === !fisFiyatli.value) fisFiyatliGecici.value = fisFiyatli.value
  } else if (e.key === 'raspel_fis_notu' && e.newValue !== null) {
    fisAltNotu.value = e.newValue
  } else if (e.key === 'raspel_fis_genislik' && e.newValue !== null) {
    fisGenislik.value = e.newValue
  }
}
onMounted(() => window.addEventListener('storage', dinleyici))
onUnmounted(() => window.removeEventListener('storage', dinleyici))

const indirimTipi = ref('tutar')
const indirimTipleri = ref([
  { label: '₺', value: 'tutar' },
  { label: '%', value: 'yuzde' }
])
const indirimDegeri = ref(0)

const odemeDurumu = ref('tam')
const odemeTipleri = computed(() => [
  { label: t('hizliSatis.odemeTipTam'), value: 'tam' },
  { label: t('hizliSatis.odemeTipYarim'), value: 'yarim' },
  { label: t('hizliSatis.odemeTipYok'), value: 'yok' }
])
const odenenTutar = ref(0)

// Ödeme yöntemi (Nakit/Kart/Havale/Taksit)
const odemeYontemi = ref('NAKIT')
const odemeYontemleri = computed(() => [
  { label: t('hizliSatis.nakit'), value: 'NAKIT', icon: 'pi pi-money-bill' },
  { label: t('hizliSatis.kart'), value: 'KART', icon: 'pi pi-credit-card' },
  { label: t('hizliSatis.havale'), value: 'HAVALE', icon: 'pi pi-send' },
  { label: t('hizliSatis.taksit'), value: 'TAKSIT', icon: 'pi pi-calendar' }
])

// Taksit bilgisi
const taksitKurum = ref('')
const taksitTutar = ref(0)
const taksitSayisi = ref(1)

// Kasa seçimi
const seciliKasa = ref(null)
const kasalar = ref([])

// Kart için doğrudan banka aktarımı
const seciliBanka = ref(null)
const bankalar = ref([])

// Kart için POS terminali (perakende satış; gün sonunda komisyon düşülerek bankaya geçer)
const seciliPos = ref(null)
const posTerminalleri = ref([])

const seciliPosBilgi = computed(
  () => posTerminalleri.value.find((p) => p.id === seciliPos.value) || null
)
const hesaplananKomisyon = computed(() => {
  const oran = Number(seciliPosBilgi.value?.komisyonOrani || 0)
  if (!oran || !odenenTutar.value) return 0
  return Math.round(odenenTutar.value * oran) / 100
})

// Para üstü
const alinanNakit = ref(0)
const paraUstu = computed(() => {
  if (odemeYontemi.value !== 'NAKIT' || odemeDurumu.value === 'yok') return 0
  return Math.max(0, (alinanNakit.value || 0) - (odenenTutar.value || 0))
})

// Satış sonrası özet
const satisOzet = ref(null)
const satisOzetDialog = ref(false)

// Günlük satış geçmişi
const gunlukSatislar = ref([])
const sonSatis = ref(null)

const kasalariYukle = async () => {
  try {
    const r = await kasaAPI.getAllKasalar()
    kasalar.value = unwrapList(r)
    if (kasalar.value.length > 0 && !seciliKasa.value) {
      seciliKasa.value = kasalar.value[0].id
    }
  } catch {
    kasalar.value = []
  }
}

const bankalariYukle = async () => {
  try {
    const r = await bankaAPI.getAll()
    bankalar.value = unwrapList(r)
  } catch {
    bankalar.value = []
  }
}

const poslariYukle = async () => {
  try {
    const r = await posAPI.aktif()
    posTerminalleri.value = Array.isArray(r.data) ? r.data : unwrapList(r)
  } catch {
    posTerminalleri.value = []
  }
}

const gunlukSatislariYukle = async () => {
  try {
    const bugun = getLocalDateString()
    // Sunucu tarafli filtre: bugun + SATIS. Istemci filtrelemesine gerek yok.
    const r = await faturaAPI.getAll({ size: 200, tur: 'SATIS', bas: bugun, bit: bugun })
    gunlukSatislar.value = unwrapList(r)
  } catch {
    gunlukSatislar.value = []
  }
}

// Gunluk satis listesinden faturayi acar (detay/yeniden yazdirma icin).
const bugunkuSatisGoruntule = (s) => {
  bugunkuDialog.value = false
  if (s?.id) router.push(`/faturalar/${s.id}`)
}

// POS filtre secenekleri ayri tanim tablolarindan degil, dogrudan urun
// kartlarindaki degerlerden turetilir (kategori/marka/stok grubu tanimi gerekmez).
const benzersizDegerler = (alan) => {
  const degerler = new Set()
  ;(stokStore.stoklar || []).forEach((s) => {
    const d = (s?.[alan] || '').trim()
    if (d) degerler.add(d)
  })
  return [...degerler].sort((a, b) => a.localeCompare(b, 'tr'))
}
const kategoriler = computed(() => benzersizDegerler('kategori'))
const markalar = computed(() => benzersizDegerler('marka'))
const stokGruplari = computed(() => benzersizDegerler('stokGrubu'))
const cokSatanlar = ref([])

// Ürün başına çoklu fiyat listesi (stok fiyatları endpoint'inden)
const urunFiyatlari = ref({})
// Cariye özel fiyatlar (stokId -> fiyat)
const cariOzelFiyatlar = ref({})

// Müşterinin ürüne özel fiyatını döndürür
const cariFiyati = (urunId) => cariOzelFiyatlar.value[urunId] || null

// Görünen ürünlerin fiyat listelerini topluca çeker
const urunFiyatlariniYukle = async (urunler) => {
  const yeni = { ...urunFiyatlari.value }
  const eksik = (urunler || []).filter((u) => u?.id != null && !yeni[u.id]).map((u) => u.id)
  if (!eksik.length) return
  try {
    // Ürün başına ayrı istek yerine tek toplu istek.
    const r = await stokAPI.getFiyatlarToplu(eksik)
    const harita = r.data || {}
    for (const id of eksik) {
      const liste = harita[id] || []
      if (liste.length) {
        yeni[id] = liste.map((f) => ({ ad: f.ad || f.fiyatTipi || f.tip || t('hizliSatis.fiyat'), fiyat: Number(f.fiyat) }))
      }
    }
    urunFiyatlari.value = yeni
  } catch {
    /* fiyat listesi alınamadı */
  }
}

// Sepete ürün eklenirken çoklu fiyat listesini de getirir
const urunFiyatlariniYukleTek = async (urun) => {
  if (urunFiyatlari.value[urun.id]) return urunFiyatlari.value[urun.id]
  try {
    const r = await stokAPI.getFiyatlar(urun.id)
    const liste = r.data || []
    if (liste.length) {
      const map = liste.map((f) => ({ ad: f.ad || f.fiyatTipi || 'Fiyat', fiyat: Number(f.fiyat) }))
      urunFiyatlari.value = { ...urunFiyatlari.value, [urun.id]: map }
      return map
    }
  } catch {
    /* */
  }
  return []
}

const cokSatanlariYukle = async () => {
  try {
    const r = await stokAPI.enCokSatanlar(12)
    cokSatanlar.value = r.data || []
    urunFiyatlariniYukle(cokSatanlar.value)
  } catch {
    cokSatanlar.value = []
  }
}

// Sepet kaydet / yükle
const SEPET_KEY = 'raspel_kayitli_sepet'
const kayitliSepetVar = ref(false)

const sepetKaydet = () => {
  if (!sepet.value.length) return
  localStorage.setItem(SEPET_KEY, JSON.stringify(sepet.value))
  kayitliSepetVar.value = true
  toast.add({ severity: 'success', summary: t('hizliSatis.sepetKaydedildi'), detail: t('hizliSatis.sepetKaydedildiDetay'), life: 3000 })
}

const sepetYukle = () => {
  try {
    const kayit = JSON.parse(localStorage.getItem(SEPET_KEY) || '[]')
    sepet.value = kayit
    kayitliSepetVar.value = false
    localStorage.removeItem(SEPET_KEY)
    toast.add({ severity: 'info', summary: t('hizliSatis.sepetYuklendi'), detail: t('hizliSatis.sepetYuklendiDetay'), life: 3000 })
  } catch {
    /* empty */
  }
}

const toplam = computed(() => sepet.value.reduce((t, i) => t + i.miktar * i.fiyat, 0))

const toplamFt3 = computed(() =>
  sepet.value.reduce((t, i) => {
    const hacim = i.birimHacim || 1
    return t + i.miktar * hacim
  }, 0)
)

const toplamAgirlik = computed(() =>
  sepet.value.reduce((t, i) => t + (Number(i.agirlik) || 0) * (Number(i.miktar) || 0), 0)
)

const agirlikVarMi = computed(() => toplamAgirlik.value > 0)

const agirlikMetni = (kg) => {
  const deger = Number(kg) || 0
  return deger >= 1000 ? (deger / 1000).toFixed(3) + ' ton' : deger.toFixed(2) + ' kg'
}

const indirimTutari = computed(() => {
  if (indirimDegeri.value <= 0) return 0
  if (indirimTipi.value === 'yuzde') return toplam.value * (Math.min(indirimDegeri.value, 100) / 100)
  return Math.min(indirimDegeri.value, toplam.value)
})

const genelToplam = computed(() => Math.max(0, toplam.value - indirimTutari.value))

watch(odemeDurumu, (v) => {
  if (v === 'tam') odenenTutar.value = genelToplam.value
  else if (v === 'yarim') odenenTutar.value = genelToplam.value / 2
  else odenenTutar.value = 0
})

watch(genelToplam, () => {
  if (odemeDurumu.value === 'tam') odenenTutar.value = genelToplam.value
  else if (odemeDurumu.value === 'yarim') odenenTutar.value = genelToplam.value / 2
})

const kalanTutar = computed(() => Math.max(0, genelToplam.value - odenenTutar.value))

const odemeDurumText = computed(() => {
  if (odemeDurumu.value === 'yok' || odenenTutar.value === 0) return t('hizliSatis.odemedi')
  if (odenenTutar.value >= genelToplam.value) return t('hizliSatis.tamamenOdendi')
  return t('hizliSatis.kismiOdedi')
})

const odemeDurumEnum = computed(() => {
  if (odemeDurumu.value === 'yok' || odenenTutar.value === 0) return 'ODENMEDI'
  if (odenenTutar.value >= genelToplam.value) return 'ODENDI'
  return 'KISMI_ODENDI'
})

const odemeDurumSeverity = computed(() => {
  if (odemeDurumu.value === 'yok' || odenenTutar.value === 0) return 'danger'
  if (odenenTutar.value >= genelToplam.value) return 'success'
  return 'warning'
})

const musteriAdi = computed(() => {
  if (anlikMusteri.value) return t('hizliSatis.anlikMusteri')
  return seciliMusteri.value?.ad || ''
})

const musteriBakiyeUyarisi = computed(() => {
  if (!seciliMusteri.value) return null
  const bakiye = seciliMusteri.value.bakiye
  const krediLimiti = seciliMusteri.value.krediLimiti
  if (krediLimiti != null && bakiye != null && bakiye < 0 && Math.abs(bakiye) >= krediLimiti) {
    return { seviye: 'danger', mesaj: t('hizliSatis.krediLimitiAsildi', { tutar: formatCurrency(Math.abs(bakiye)) }) }
  }
  if (bakiye != null && bakiye < 0) {
    return { seviye: 'warn', mesaj: t('hizliSatis.borc', { tutar: formatCurrency(Math.abs(bakiye)) }) }
  }
  if (bakiye != null && bakiye > 0) {
    return { seviye: 'info', mesaj: t('hizliSatis.alacak', { tutar: formatCurrency(bakiye) }) }
  }
  return null
})

const filtrelenmisUrunler = computed(() => {
  let list = stokStore.stoklar || []

  if (filtreKategori.value) {
    list = list.filter((u) => (u.kategori || '').trim() === filtreKategori.value)
  }

  if (filtreMarka.value) {
    list = list.filter((u) => (u.marka || '').trim() === filtreMarka.value)
  }

  if (filtreStokGrubu.value) {
    list = list.filter((u) => (u.stokGrubu || '').trim() === filtreStokGrubu.value)
  }

  if (sadeceStokta.value) {
    list = list.filter((u) => Number(u.miktar || 0) > 0)
  }

  if (seriNoArama.value) {
    const q = seriNoArama.value.toLowerCase()
    list = list.filter(
      (u) =>
        u.ad?.toLowerCase().includes(q) ||
        u.stokKodu?.toLowerCase().includes(q) ||
        u.barkod?.toLowerCase().includes(q) ||
        u.seriNo?.toLowerCase().includes(q) ||
        u.stokGrubu?.toLowerCase().includes(q)
    )
  }

  return [...list].sort((a, b) => {
    if (siralama.value === 'fiyat') return (satisFiyati(b) || 0) - (satisFiyati(a) || 0)
    if (siralama.value === 'stok') return (b.miktar || 0) - (a.miktar || 0)
    return (a.ad || '').localeCompare(b.ad || '', 'tr')
  })
})

// Kademeli gösterim: binlerce üründe ilk 60 kart çizilir, "Daha fazla" ile artırılır.
const gorunenUrunler = computed(() => filtrelenmisUrunler.value.slice(0, gosterilenAdet.value))

// Filtre/sıralama değişince kademeli gösterim baştan başlar.
watch([seriNoArama, filtreKategori, filtreMarka, filtreStokGrubu, sadeceStokta, siralama], () => {
  gosterilenAdet.value = 60
})

// Görünen ürünler değiştiğinde çoklu fiyat listelerini besle
watch(gorunenUrunler, (list) => {
  if (list && list.length) urunFiyatlariniYukle(list)
}, { immediate: true })

const siralamaSecenekleri = computed(() => [
  { label: t('hizliSatis.siralaAd'), value: 'ad' },
  { label: t('hizliSatis.siralaFiyat'), value: 'fiyat' },
  { label: t('hizliSatis.siralaStok'), value: 'stok' }
])

const fisModuSecenekleri = computed(() => [
  { label: t('hizliSatis.fiyatliFis'), value: true },
  { label: t('hizliSatis.fiyatsizFis'), value: false }
])

const kategoriCipleri = computed(() =>
  tumKategorilerGoster.value ? kategoriler.value : kategoriler.value.slice(0, 12)
)
const gizliKategoriSayisi = computed(() => Math.max(0, kategoriler.value.length - 12))

// POS'ta satış fiyatı önceliklidir; tanımlı değilse alış fiyatına düşülür.
const satisFiyati = (u) => Number(u?.satisFiyati || u?.fiyat || 0)

const stokYokMu = (u) => Number(u?.miktar || 0) <= 0

// Sepetteki adet: kart üzerinde rozet olarak gösterilir.
const sepetteAdet = (id) => sepet.value.find((i) => i.id === id)?.miktar || 0

const urunKartiTikla = (u) => {
  if (stokYokMu(u)) {
    toastBildirim.uyari(t('hizliSatis.stokYokUyari', { ad: u.ad }))
    return
  }
  sepeteEkle(u)
}


const simdikiTarih = computed(() => formatDateTime(new Date()))


onMounted(async () => {
  try {
    await Promise.all([
      cariHesapStore.getAllCariHesaplar(),
      stokStore.getAll(),
      soforleriYukle(),
      cokSatanlariYukle(),
      kasalariYukle(),
      bankalariYukle(),
      poslariYukle(),
      gunlukSatislariYukle()
    ])
    kayitliSepetVar.value = !!localStorage.getItem('raspel_kayitli_sepet')
    await fisAyarlariSunucudanYukle()
    degisimSorgusunuUygula()
  } catch (e) {
    // Kullanici bos urun listesi gorup "urun yok" sanmasin; yukleme hatasini bildir.
    toastBildirim.hata(e?.response?.data?.message || t('hizliSatis.yuklemeHatasi'))
  }
})

const degisimSorgusunuUygula = () => {
  const qCari = route.query.cariHesapId
  if (qCari) {
    const c = cariHesapStore.cariHesaplar?.find((x) => String(x.id) === String(qCari))
    if (c) seciliMusteri.value = c
  }
  if (route.query.degisim) {
    degisimIadeId.value = route.query.degisim
    // Sorgu temizlenir; sayfa yenilenince banner tekrar açılmasın.
    router.replace({ path: route.path })
  }
}

const fisAyarlariSunucudanYukle = async () => {
  const sirketId = authStore?.sirketId
  if (!sirketId) return
  try {
    const res = await sirketAPI.getPosFisAyarlari(sirketId)
    if (res.data?.ayarlar) {
      const a = JSON.parse(res.data.ayarlar)
      if (a.fisAltNotu != null) {
        fisAltNotu.value = a.fisAltNotu
        localStorage.setItem('raspel_fis_notu', a.fisAltNotu)
      }
      if (a.fisFiyatli != null) {
        fisFiyatli.value = a.fisFiyatli
        localStorage.setItem('raspel_fis_fiyatli', String(a.fisFiyatli))
      }
      if (a.fisGenislik != null) {
        fisGenislik.value = String(a.fisGenislik)
        localStorage.setItem('raspel_fis_genislik', String(a.fisGenislik))
      }
    }
  } catch {
    /* sunucu yoksa yerel önbellek kullanılır */
  }
}

const teslimDurumSecenekleri = computed(() => [
  { label: t('faturalar.durumBekliyor'), value: 'BEKLIYOR' },
  { label: t('faturalar.durumYolda'), value: 'YOLDA' },
  { label: t('faturalar.durumTeslimEdildi'), value: 'TESLIM_EDILDI' }
])

// POS'ta secilen soforun adi; fis/fatura uzerine "Teslim Eden" olarak yazilir.
const teslimEden = computed(() => seciliSofor.value?.ad || '')

// Teslimat bolumu katliyken secili soforu rozet olarak gosterir.
const teslimEdenEtiketi = computed(() => seciliSofor.value?.ad || '')

const soforleriYukle = async () => {
  soforlerYukleniyor.value = true
  try {
    const r = await teslimatAPI.suruculer()
    soforler.value = unwrapList(r)
  } catch {
    soforler.value = []
  } finally {
    soforlerYukleniyor.value = false
  }
}

// POS teslim durumu -> Teslimat kaydi durumu eslemesi.
const TESLIMAT_DURUM_ESLEME = { BEKLIYOR: 'BEKLEMEDE', YOLDA: 'YOLDA', TESLIM_EDILDI: 'TESLIM_EDILDI' }

const teslimatMetaUret = () => {
  if (!seciliSofor.value?.id) return null
  return {
    driverId: seciliSofor.value.id,
    teslimatAdresi: teslimatAdresi.value.trim(),
    notlar: teslimNotu.value?.trim() || null,
    durum: TESLIMAT_DURUM_ESLEME[teslimDurumu.value] || 'BEKLEMEDE'
  }
}

const teslimatKaydiniOlustur = async (faturaId, meta = null) => {
  const veri = meta || teslimatMetaUret()
  if (!veri || !faturaId) return
  try {
    await teslimatAPI.olustur({ faturaId, ...veri })
  } catch (e) {
    // Satis tamamlandi; teslimat kaydi acilamazsa kullanici bilgilendirilir.
    toastBildirim.uyari(e?.response?.data?.message || t('hizliSatis.teslimatKaydedilemedi'))
  }
}

// Sofor secilince cari adresi otomatik dolar (kullanici elle degistirebilir).
watch([seciliSofor, seciliMusteri], () => {
  if (!seciliSofor.value || teslimatAdresi.value.trim()) return
  const adres = seciliMusteri.value?.adres
  if (adres) teslimatAdresi.value = adres
})

const musteriAra = (event) => {
  const query = event.query
  const kaynak = cariHesapStore?.cariHesaplar || []
  if (!query) {
    musteriOnerileri.value = kaynak.slice(0, 20)
    return
  }
  const q = query.toLowerCase()
  musteriOnerileri.value = kaynak.filter(
      (c) => c.ad?.toLowerCase().includes(q) || c.vergiNo?.toLowerCase().includes(q) || c.telefon?.includes(query)
    )
    .slice(0, 20)
}

const musteriSec = (event) => {
  seciliMusteri.value = event.value
  musteriGiris.value = ''
  sepeteCariFiyatUygula()
  cariOzelFiyatlariYukle()
}

const musteriTemizle = () => {
  seciliMusteri.value = null
  musteriGiris.value = ''
  cariOzelFiyatlar.value = {}
}

// Cariye özel tanımlı fiyatları yükler (stokId -> fiyat)
const cariOzelFiyatlariYukle = async () => {
  const cariId = seciliMusteri.value?.id
  if (!cariId) return
  try {
    const r = await cariHesapAPI.getFiyatlar(cariId)
    const liste = r.data || []
    const map = {}
    liste.forEach((f) => {
      if (f.stokId != null) map[f.stokId] = f.fiyat
    })
    cariOzelFiyatlar.value = map
  } catch {
    cariOzelFiyatlar.value = {}
  }
}

const musteriKaydet = async () => {
  if (!yeniMusteri.value.ad) {
    toastBildirim.uyari(t('hizliSatis.adFirmaZorunlu'))
    return
  }
  musteriKaydediliyor.value = true
  try {
    const r = await cariHesapAPI.create(yeniMusteri.value)
    seciliMusteri.value = r.data
    musteriGiris.value = ''
    yeniMusteriDialog.value = false
    yeniMusteri.value = { ad: '', telefon: '', email: '', adres: '', vergiNo: '', tur: 'Musteri' }
    toastBildirim.basarili(t('hizliSatis.cariHesapOlusturuldu'))
  } catch (e) {
    toastBildirim.hata(e?.response?.data?.message || t('hizliSatis.kayitBasarisiz'))
  }
  musteriKaydediliyor.value = false
}

// Sepete eklenen satırı kısa süre vurgular (görsel geri bildirim).
const vurguluId = ref(null)
const satiriVurgula = (id) => {
  vurguluId.value = id
  setTimeout(() => {
    if (vurguluId.value === id) vurguluId.value = null
  }, 700)
}

// Hizli nakit: kasa icin sik kullanilan banknot/kagit degerleri.
const hizliNakit = [50, 100, 200, 500]

// Sepet satirlarini surukle-birak ile siralama (tutamactan).
const suruklenenIdx = ref(null)
const surklenenUzerinde = ref(null)
const suruklemeBasla = (idx) => { suruklenenIdx.value = idx }
const suruklemeUzerine = (idx) => { surklenenUzerinde.value = idx }
const suruklemeBitir = () => { suruklenenIdx.value = null; surklenenUzerinde.value = null }
const suruklemeBirak = (hedefIdx) => {
  const kaynak = suruklenenIdx.value
  surklenenUzerinde.value = null
  if (kaynak === null || kaynak === hedefIdx) {
    suruklenenIdx.value = null
    return
  }
  const arr = sepet.value
  const [tasinan] = arr.splice(kaynak, 1)
  arr.splice(hedefIdx, 0, tasinan)
  aktifSatir.value = hedefIdx
  suruklenenIdx.value = null
}

const sepeteEkle = async (u) => {
  const varOlan = sepet.value.find((i) => i.id === u.id)
  if (varOlan) {
    varOlan.miktar++
    satiriVurgula(u.id)
    return
  }
  const stdFiyat = Number(u.satisFiyati || u.fiyat || 0)
  const cokluFiyatVar = !!(u.fiyatlar && u.fiyatlar.length > 0)
  const temelFiyatlar = cokluFiyatVar
    ? u.fiyatlar.map((f) => ({ ad: f.ad, fiyat: f.fiyat }))
    : [
        { ad: t('hizliSatis.fiyatPerakende'), fiyat: stdFiyat },
        { ad: t('hizliSatis.fiyatToptan'), fiyat: Math.round(stdFiyat * 0.9 * 100) / 100 },
        { ad: t('hizliSatis.fiyatOzel'), fiyat: Math.round(stdFiyat * 0.8 * 100) / 100 }
      ]

  // İYİMSER EKLEME: satır anında sepete girer (gecikme/çift tıklama sorunu yok);
  // fiyat listesi ve cari geçmişi arka planda zenginleştirilir.
  const yeniItem = {
    id: u.id,
    ad: u.ad,
    stokKodu: u.stokKodu,
    barkod: u.barkod,
    miktar: 1,
    fiyat: temelFiyatlar[0]?.fiyat ?? stdFiyat,
    fiyatlar: temelFiyatlar,
    fiyatTipi: temelFiyatlar[0]?.ad ?? t('hizliSatis.fiyatPerakende'),
    birim: u.birim || 'adet',
    birimHacim: u.birimHacim || 1,
    agirlik: Number(u.agirlik) || 0,
    sonAldigiFiyat: null,
    sonAldigiTarih: null,
    sonAldigiBilgisiYukleniyor: false
  }
  sepet.value.push(yeniItem)
  satiriVurgula(u.id)

  try {
    let fiyatlar = temelFiyatlar
    if (!cokluFiyatVar) {
      const tckilen = await urunFiyatlariniYukleTek(u)
      if (tckilen && tckilen.length > 0) fiyatlar = tckilen
    }
    // Kullanıcı bu arada satırı sildiyse dokunma.
    const guncel = sepet.value.find((i) => i.id === u.id)
    if (!guncel) return
    guncel.fiyatlar = fiyatlar
    if (!fiyatlar.some((f) => f.ad === guncel.fiyatTipi)) {
      guncel.fiyat = fiyatlar[0]?.fiyat ?? stdFiyat
      guncel.fiyatTipi = fiyatlar[0]?.ad ?? t('hizliSatis.fiyatPerakende')
    }
    // Seçili müşteri varsa ürünü en son hangi fiyata aldığını sor.
    if (seciliMusteri.value?.id) {
      guncel.sonAldigiBilgisiYukleniyor = true
      try {
        const r = await faturaAPI.cariUrunFiyatGecmisi(seciliMusteri.value.id, u.id)
        const data = r.data
        if (data && data.sonFiyat != null) {
          guncel.sonAldigiFiyat = data.sonFiyat
          const enSon = (data.gecmis || [])[0]
          guncel.sonAldigiTarih = enSon?.tarih || null
          guncel.fiyat = data.sonFiyat
          if (!guncel.fiyatlar.some((f) => f.ad === t('hizliSatis.fiyatSonAldigi'))) {
            guncel.fiyatlar.unshift({ ad: t('hizliSatis.fiyatSonAldigi'), fiyat: data.sonFiyat })
          }
          guncel.fiyatTipi = guncel.fiyatlar[0]?.ad ?? t('hizliSatis.fiyatPerakende')
        }
      } catch {
        /* cari fiyat geçmişi alınamadı */
      } finally {
        guncel.sonAldigiBilgisiYukleniyor = false
      }
    }
  } catch {
    /* zenginleştirme opsiyonel; temel fiyatla devam */
  }
}

const fiyatTipiDegisti = (item) => {
  const secili = item.fiyatlar?.find((f) => f.ad === item.fiyatTipi)
  if (secili) item.fiyat = secili.fiyat
}

// Müşteri seçilince sepetteki tüm ürünlere cari bazlı fiyatı uygular
const sepeteCariFiyatUygula = async () => {
  const cariId = seciliMusteri.value?.id
  if (!cariId || !sepet.value.length) return
  await Promise.all(sepet.value.map(async (item) => {
    try {
      const r = await faturaAPI.cariUrunFiyatGecmisi(cariId, item.id)
      const data = r.data
      if (data && data.sonFiyat != null) {
        item.sonAldigiFiyat = data.sonFiyat
        const enSon = (data.gecmis || [])[0]
        item.sonAldigiTarih = enSon?.tarih || null
        item.fiyat = data.sonFiyat
        if (!item.fiyatlar.some((f) => f.ad === t('hizliSatis.fiyatSonAldigi'))) {
          item.fiyatlar.unshift({ ad: t('hizliSatis.fiyatSonAldigi'), fiyat: data.sonFiyat })
        }
        item.fiyatTipi = t('hizliSatis.fiyatSonAldigi')
      }
    } catch {
      /* cari fiyat geçmişi alınamadı */
    }
  }))
}

const miktarAzalt = (idx) => {
  if (sepet.value[idx].miktar > 1) sepet.value[idx].miktar--
  else sepetSil(idx)
}

const sepetSil = (idx) => {
  sepet.value.splice(idx, 1)
}

const fisiYazdir = (gercekFaturaNo, fiyatliOverride = null) => {
  if (!sepet.value.length) return
  // Gerçek fatura numarası varsa fişe o yazılır (fişten faturaya ulaşılabilir).
  // Yalnızca çevrimdışı/henüz oluşmamış satışlarda geçici numara üretilir.
  fisNo.value = gercekFaturaNo || ('F-' + Date.now().toString(36).toUpperCase())
  yazdirmaKaydet('TERMAL80')

  const fiyatli = fiyatliOverride !== null ? fiyatliOverride : fisFiyatliGecici.value
  const kalemHtml = sepet.value
    .map((i) => {
      const ad = escapeHtml(i.ad || '')
      const satir = `<div class="satir"><span class="ad">${ad} x${i.miktar}</span>${fiyatli ? `<span class="tutar">${formatCurrency(i.miktar * i.fiyat)}</span>` : ''}</div>`
      return satir
    })
    .join('')

  const ozetHtml = fiyatli
    ? `
    <div class="ayrac">- - - - - - - - - - - - - -</div>
    <div class="satir"><span class="ad">${t('hizliSatis.araToplam')}</span><span class="tutar">${formatCurrency(toplam.value)}</span></div>
    ${indirimDegeri.value > 0 ? `<div class="satir"><span class="ad">${t('hizliSatis.indirim')}${indirimTipi.value === 'yuzde' ? ' (' + indirimDegeri.value + '%)' : ''}</span><span class="tutar">-${formatCurrency(indirimTutari.value)}</span></div>` : ''}
    <div class="satir genel"><span class="ad">${t('hizliSatis.fisGenelToplam')}</span><span class="tutar">${formatCurrency(genelToplam.value)}</span></div>
    <div class="ayrac">- - - - - - - - - - - - - -</div>
    <div class="satir"><span class="ad">${t('hizliSatis.fisOdenen')}</span><span class="tutar">${formatCurrency(odenenTutar.value)}</span></div>
    ${kalanTutar.value > 0 ? `<div class="satir"><span class="ad">${t('hizliSatis.fisKalan')}</span><span class="tutar">${formatCurrency(kalanTutar.value)}</span></div>` : ''}
  `
    : ''

  const musteriHtml = musteriAdi.value ? `<div class="musteri">${t('hizliSatis.fisMusteri')} ${escapeHtml(musteriAdi.value)}</div>` : ''

  const html = `<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>${t('hizliSatis.fisOnizleme')}</title>
<style>
  * { margin: 0; padding: 0; box-sizing: border-box; }
  body { font-family: 'Courier New', monospace; width: ${fisGenislik.value === '58' ? '58mm' : '80mm'}; margin: 0 auto; color: #000; font-size: 12px; }
  .aracubuk {
    position: fixed; top: 0; left: 0; right: 0; z-index: 10;
    width: 100%; padding: 10px; text-align: center;
    background: #1e293b; box-shadow: 0 2px 8px rgba(0,0,0,0.2);
  }
  .aracubuk button {
    font-family: Arial, sans-serif; font-size: 14px; font-weight: 600;
    padding: 10px 24px; border: none; border-radius: 6px; cursor: pointer;
    background: var(--accent); color: #fff; margin: 0 4px;
  }
  .aracubuk button.iptal { background: #475569; }
  .fis { padding: 6px 4px; margin-top: 52px; }
  .baslik { text-align: center; font-size: 14px; font-weight: bold; margin-bottom: 4px; }
.logo { display: block; max-height: 48px; max-width: 140px; margin: 0 auto 4px; object-fit: contain; }
  .tarih, .fisno { text-align: center; font-size: 10px; margin-top: 2px; }
  .musteri { margin-top: 6px; font-size: 11px; }
  .ayrac { text-align: center; color: #555; margin: 4px 0; letter-spacing: 1px; }
  .satir { display: flex; justify-content: space-between; padding: 2px 0; }
  .satir .ad { flex: 1; white-space: pre-wrap; word-break: break-word; padding-right: 6px; }
  .satir .tutar { white-space: nowrap; }
  .satir.genel { border-top: 2px solid #000; font-weight: bold; padding-top: 4px; margin-top: 4px; }
  .tesekkur { text-align: center; margin-top: 8px; font-size: 10px; }
  @media print {
    .aracubuk { display: none !important; }
    .fis { margin-top: 0; }
  }
</style>
</head>
<body>
  <div class="aracubuk">
    <button onclick="window.print()">${t('hizliSatis.yazdir')}</button>
    <button class="iptal" onclick="window.close()">${t('hizliSatis.kapat')}</button>
  </div>
  <div class="fis">
    ${sirketLogosu.value ? `<img class="logo" src="${escapeHtml(sirketLogosu.value)}" alt="logo" />` : ''}
    <div class="baslik">${escapeHtml(sirketAdi.value || 'RASPEL ERP')}</div>
    <div class="tarih">${simdikiTarih.value}</div>
    <div class="fisno">${t('hizliSatis.fisNo')} ${escapeHtml(fisNo.value || '')}</div>
    ${musteriHtml}
    ${teslimEden.value ? `<div class="musteri">${t('hizliSatis.teslimEden')}: ${escapeHtml(teslimEden.value)}</div>` : ''}
    ${teslimDurumu.value && teslimDurumu.value !== 'BEKLIYOR' ? `<div class="musteri">${t('hizliSatis.teslimEtiketi')}: ${teslimDurumEtiketi(teslimDurumu.value)}</div>` : ''}
    ${teslimNotu.value ? `<div class="musteri">${t('hizliSatis.not')}: ${escapeHtml(teslimNotu.value)}</div>` : ''}
    <div class="ayrac">- - - - - - - - - - - - - -</div>
    ${kalemHtml}
    ${agirlikVarMi.value ? `<div class="satir"><span class="ad">${t('hizliSatis.toplamAgirlik')}</span><span class="tutar">${agirlikMetni(toplamAgirlik.value)}</span></div>` : ''}
    ${ozetHtml}
    <div class="satir"><span class="ad">${t('hizliSatis.toplamUrun')}</span><span class="tutar">${sepet.value.length}</span></div>
    <div class="satir"><span class="ad">${t('common.status')}</span><span class="tutar">${odemeDurumText.value}</span></div>
    <div class="ayrac">- - - - - - - - - - - - - -</div>
    <div class="tesekkur">${t('hizliSatis.islemYapan')} ${escapeHtml(authStore?.kullanici?.displayName || '-')}</div>
    <div class="tesekkur">${t('hizliSatis.iyiGunlerDileriz')}</div>
  </div>
</body>
</html>`

  const pencere = fisPenceresiAcVeYazdir(html)
  if (!pencere) {
    toastBildirim.hata(t('hizliSatis.pencereEngellendi'))
  }
}

const termalYazdir = async () => {
  if (!sepet.value.length) return
  const bytes = escPosFisiUret({
    baslik: sirketAdi.value || 'RASPEL ERP',
    tarih: simdikiTarih.value,
    fisNo: fisNo.value || undefined,
    genislik: fisGenislik.value,
    kalemler: sepet.value.map((i) => ({ ad: i.ad, adet: i.miktar, tutar: i.miktar * i.fiyat })),
    toplamAgirlik: agirlikVarMi.value ? toplamAgirlik.value : undefined,
    toplam: genelToplam.value,
    altNot: fisAltNotu.value || undefined
  })
  try {
    const gonderildi = await escPosYazdir(bytes)
    if (!gonderildi) {
      // WebUSB desteklenmiyorsa browser print fallback
      fisiYazdir()
    } else {
      yazdirmaKaydet('TERMAL', gonderildi)
    }
  } catch {
    toastBildirim.hata(t('hizliSatis.termalGonderilemedi'))
  }
}

/** Son oluşturulan faturaya yazdırma izi kaydeder (fatura id yoksa sessizce atlanır). */
const yazdirmaKaydet = (format, yaziciAdi) => {
  const id = sonSatis.value?.id
  if (!id) return
  faturaAPI.yazdirmaKaydet(id, { format, yaziciAdi }).catch(() => {})
}

const teslimDurumEtiketi = (d) => ({ BEKLIYOR: t('faturalar.durumBekliyor'), YOLDA: t('faturalar.durumYolda'), TESLIM_EDILDI: t('faturalar.durumTeslimEdildi') })[d] || d

const hizliUrunKaydet = async () => {
  const u = hizliUrun.value
  if (!u.ad || !u.ad.trim()) {
    toast.add({ severity: 'warn', summary: t('hizliSatis.eksikBilgi'), detail: t('hizliSatis.urunAdiZorunlu'), life: 2500 })
    return
  }
  hizliUrunKaydediliyor.value = true
  try {
    const r = await stokAPI.create({
      ad: u.ad.trim(),
      barkod: u.barkod || null,
      fiyat: Number(u.fiyat) || 0,
      satisFiyati: Number(u.fiyat) || 0,
      miktar: Number(u.miktar) || 0,
      birim: 'Adet'
    })
    const olusan = r.data || { ...u }
    hizliUrunDialog.value = false
    toast.add({ severity: 'success', summary: t('hizliSatis.urunEklendi'), detail: u.ad, life: 2500 })
    await sepeteEkle(olusan)
    if (Number(u.miktar) > 1 && olusan?.id) {
      const item = sepet.value.find((i) => i.id === olusan.id)
      if (item) item.miktar = Number(u.miktar)
    }
  } catch (e) {
    toast.add({
      severity: 'error',
      summary: t('hizliSatis.kaydedilemedi'),
      detail: e?.response?.data?.message || t('hizliSatis.urunOlusturulamadi'),
      life: 3000
    })
  } finally {
    hizliUrunKaydediliyor.value = false
  }
}

const satisiTamamla = async () => {
  if (!anlikMusteri.value && !seciliMusteri.value) {
    toast.add({
      severity: 'warn',
      summary: t('hizliSatis.musteriGerekli'),
      detail: t('hizliSatis.musteriSecin'),
      life: 3000
    })
    return
  }
  if (sepet.value.length === 0) {
    toast.add({ severity: 'warn', summary: t('hizliSatis.sepetBos'), detail: t('hizliSatis.onceUrunEkleyin'), life: 2500 })
    return
  }
  // Kazara satışı önleme: tercih açıksa kısa onay adımı göster.
  if (onayIste.value) {
    onayBekleyenSatis.value = true
    onayDialog.value = true
    return
  }
  await satisiTamamlaOnaysiz()
}

const satisOnayla = async () => {
  onayDialog.value = false
  onayBekleyenSatis.value = false
  await satisiTamamlaOnaysiz()
}

const satisOnayIptal = () => {
  onayDialog.value = false
  onayBekleyenSatis.value = false
}

const satisiTamamlaOnaysiz = async () => {
  if (odemeYontemi.value === 'TAKSIT' && (!taksitKurum.value.trim() || !taksitTutar.value || taksitTutar.value <= 0)) {
    toastBildirim.uyari(t('hizliSatis.taksitZorunlu'))
    return
  }
  // Sofor secildiyse teslimat adresi zorunlu; soforsuz satista adres sorulmaz.
  if (seciliSofor.value && !teslimatAdresi.value.trim()) {
    teslimatAcik.value = true
    toastBildirim.uyari(t('hizliSatis.teslimatAdresiGerekli'))
    return
  }
  kaydediliyor.value = true
  const satisVerisi = satisPayloadUret({
    cariHesapId: anlikMusteri.value ? null : seciliMusteri.value.id,
    cariHesapAdi: anlikMusteri.value ? t('hizliSatis.perakendeMusteri') : seciliMusteri.value.ad,
    tur: 'SATIS',
    durum: 'KESILDI',
    tarih: getLocalDateString(),
    aciklama: t('hizliSatis.hizliSatisAciklama'),
    indirim: indirimTutari.value,
    odenenTutar: odenenTutar.value,
    odemeDurumu: odemeDurumEnum.value,
    odemeYontemi: odemeYontemi.value,
    kalemler: sepet.value.map((i) => ({ stokId: i.id, aciklama: i.ad, adet: i.miktar, birimFiyat: i.fiyat })),
    ekstra: {
      teslimEden: teslimEden.value || null,
      teslimDurumu: teslimDurumu.value || 'BEKLIYOR',
      teslimNotu: teslimNotu.value || null,
      taksitKurum: odemeYontemi.value === 'TAKSIT' ? taksitKurum.value : null,
      taksitTutar: odemeYontemi.value === 'TAKSIT' ? taksitTutar.value : null,
      taksitSayisi: odemeYontemi.value === 'TAKSIT' ? taksitSayisi.value : null,
      kasaId: odemeYontemi.value === 'NAKIT' ? (seciliKasa.value || null) : null,
      bankaId: odemeYontemi.value === 'HAVALE' || (odemeYontemi.value === 'KART' && !seciliPos.value)
        ? (seciliBanka.value || null)
        : null,
      kartaBankaAktar: odemeYontemi.value === 'KART' && !seciliPos.value && !!seciliBanka.value,
      // POS terminali seçilirse tutar anında bankaya yazılmaz; POS gün sonunda komisyon
      // düşülerek terminalin bankasına aktarılır (çift giriş önlenir).
      posTerminaliId: odemeYontemi.value === 'KART' ? (seciliPos.value || null) : null,
      komisyonTutar: odemeYontemi.value === 'KART' && seciliPos.value ? hesaplananKomisyon.value : null
    }
  })
  try {
    const yanit = await faturaAPI.create(satisVerisi)
    satisBasarili(yanit)
  } catch (e) {
    // Ağ yoksa satışı kuyruğa al (offline satış). Şoför ataması da kuyrukla
    // taşınır; bağlantı gelince fatura oluşup teslimat kaydı otomatik açılır.
    if (!e?.response) {
      offlineKuyruk.ekle(satisVerisi, teslimatMetaUret())
      toast.add({
        severity: 'warn',
        summary: t('hizliSatis.cevrimdisiKuyruk'),
        detail: t('hizliSatis.baglantiGelince'),
        life: 4000
      })
      try {
        fisiYazdir()
      } catch {
        /* empty */
      }
      sepetiTemizle()
    } else if ((e?.response?.data?.message || '').includes('Kredi limiti')) {
      // Kredi limiti sunucu tarafında engellendi; açık onayla bayrak gönderilerek tekrar denenir.
      confirm.require({
        message: e.response.data.message + ' ' + t('faturalar.krediLimitiDevam'),
        header: t('faturalar.krediLimitiBaslik'),
        icon: 'pi pi-exclamation-triangle',
        acceptLabel: t('faturalar.krediLimitiOnayla'),
        rejectLabel: t('common.vazgec'),
        accept: async () => {
          try {
            satisBasarili(await faturaAPI.create({ ...satisVerisi, krediLimitiGormezdenGel: true }))
          } catch (e2) {
            toastBildirim.hata(e2?.response?.data?.message || t('hizliSatis.satisBasarisiz'))
          }
        }
      })
    } else {
      toastBildirim.hata(e?.response?.data?.message || t('hizliSatis.satisBasarisiz'))
    }
  }
  kaydediliyor.value = false
}

// Başarılı satış sonrası ortak işlemler (normal ve kredi limiti onaylı akış).
const satisBasarili = (yanit) => {
  sonSatis.value = yanit.data
  satisOzet.value = {
    faturaNo: yanit.data?.faturaNumarasi,
    toplam: genelToplam.value,
    odenen: odenenTutar.value,
    kalan: kalanTutar.value,
    yontem: odemeYontemi.value,
    paraUstu: paraUstu.value,
    fisModu: fisFiyatliGecici.value
  }
  toastBildirim.basarili(t('hizliSatis.satisTamamlandi') + ' - ' + formatCurrency(genelToplam.value))
  if (otomatikYazdir.value) {
    try {
      fisiYazdir(yanit.data?.faturaNumarasi)
    } catch {
      /* empty */
    }
  }
  // Sofor secildiyse teslimat kaydi acilir (fatura olustuktan sonra).
  teslimatKaydiniOlustur(yanit.data?.id, teslimatMetaUret())
  sepetiTemizle()
  alinanNakit.value = 0
  // Fiş modu satış başına geçiciydi: sunucu ayarına dön.
  fisFiyatliGecici.value = fisFiyatli.value
  satisOzetDialog.value = true
  gunlukSatislariYukle()
  kasalariYukle()
}

// Son satışı iptal et (stok geri alınır)
const sonSatisiIptalEt = async () => {
  if (!sonSatis.value?.id) {
    toastBildirim.uyari(t('hizliSatis.geriAlinacakSatisYok'))
    return
  }
  try {
    await faturaAPI.updateDurum(sonSatis.value.id, 'IPTAL')
    toastBildirim.basarili(t('hizliSatis.sonSatisIptalEdildi'))
    sonSatis.value = null
    gunlukSatislariYukle()
    stokStore.getAll()
    kasalariYukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('hizliSatis.iptalBasarisiz'))
  }
}

// Sepet temizleme: F2 ve "Temizle" için geri alınabilir (kısa süre).
const geriAlSepet = ref(null)
let geriAlZamanlayici = null
const sepetiGeriAlinabilirTemizle = () => {
  if (sepet.value.length) {
    geriAlSepet.value = sepet.value.map((i) => ({ ...i }))
    clearTimeout(geriAlZamanlayici)
    geriAlZamanlayici = setTimeout(() => { geriAlSepet.value = null }, 8000)
  }
  sepet.value = []
  aktifSatir.value = -1
}
const sepetGeriAl = () => {
  if (!geriAlSepet.value) return
  sepet.value = geriAlSepet.value.map((i) => ({ ...i }))
  geriAlSepet.value = null
  clearTimeout(geriAlZamanlayici)
}

// Satış özeti kapatılıp yeni satışa hazırlanır: barkod alanı odaklanır.
const yeniSatisaBasla = () => {
  satisOzetDialog.value = false
  satisOzet.value = null
  nextTick(() => odakla(barkodInputRef))
}

// Satış tamamlandıktan sonra: temizle (geri alınamaz) ve perakende moduna dön.
const sepetiTemizle = () => {
  sepet.value = []
  geriAlSepet.value = null
  seciliMusteri.value = null
  musteriGiris.value = ''
  seciliSofor.value = null
  teslimatAdresi.value = ''
  teslimDurumu.value = 'BEKLIYOR'
  teslimNotu.value = ''
  musteriModu.value = 'perakende'
  indirimDegeri.value = 0
  odemeDurumu.value = 'tam'
  odenenTutar.value = 0
}
</script>

<style scoped>
.pos-container {
  padding: 0;
  min-height: 0;
  max-width: 100%;
  /* Yatay tasmalara karsi emniyet: icerik kolonlari kendi icinde kaydirilir. */
  overflow-x: clip;
}
/* Buyuk yazi modu: yasli/uzak mesafeden kullanan personel icin olcekler */
.pos-buyuk {
  font-size: 18px;
}
.pos-buyuk .breadcrumb,
.pos-buyuk .user-info {
  font-size: 16px;
}
/* Urun karti */
.pos-buyuk .urun-sayaci {
  font-size: 13px;
}
.pos-buyuk .cok-satan-ad {
  font-size: 16px;
}
.pos-buyuk .cok-satan-fiyat {
  font-size: 15px;
}
.pos-buyuk .cok-satan-chip {
  padding: 11px 15px;
}
/* Bolum basliklari */
.pos-buyuk .pos-bolum-baslik,
.pos-buyuk .katlanir-baslik {
  font-size: 16px;
}
.pos-buyuk .product-header h3 {
  font-size: 18px;
}
/* Sepet */
.pos-buyuk .sepet-tutar {
  font-size: 16px;
}
.pos-buyuk .sepet-son-alis {
  font-size: 15px;
}
.pos-buyuk .sepet-item {
  padding: 14px 0;
}
/* Ozet ve tutarlar */
.pos-buyuk .ozet-satir,
.pos-buyuk .odeme-kalan {
  font-size: 16px;
}
.pos-buyuk .odenen-satir label {
  font-size: 15px;
}
.pos-buyuk .genel-toplam-deger {
  font-size: 24px;
}
.pos-buyuk .kalan-deger {
  font-size: 17px;
}
/* Odeme yontemleri */
.pos-buyuk .odeme-yontem-btn {
  font-size: 14px;
  padding: 11px 6px;
}
.pos-buyuk .odeme-yontem-btn i {
  font-size: 20px;
}
/* Musteri */
.pos-buyuk .musteri-option,
.pos-buyuk .secili-musteri-ad {
  font-size: 15px;
}
.pos-buyuk .musteri-option-detay {
  font-size: 13px;
}
/* Gunluk satislar */
.pos-buyuk :deep(.gunluk-satis-cari) {
  font-size: 14px;
}
.pos-buyuk :deep(.gunluk-satis-tutar) {
  font-size: 16px;
}
.pos-buyuk :deep(.gunluk-satis-no) {
  font-size: 15px;
}
/* Kisa yol ipucu */
.pos-buyuk .pos-ipucu {
  font-size: 14px;
}
/* Sticky tamamla */
.pos-buyuk .sticky-tutar {
  font-size: 16px;
}
.pos-buyuk .sticky-tutar strong {
  font-size: 24px;
}
.pos-tercih-btn,
.pos-ikon-btn {
  border: 1px solid var(--border);
  background: var(--bg-secondary);
  color: var(--text-secondary);
  width: 34px;
  height: 34px;
  border-radius: 8px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  transition: background 0.15s, border-color 0.15s, color 0.15s;
}
.pos-tercih-btn:hover,
.pos-ikon-btn:hover {
  background: var(--bg-primary);
  color: var(--text-primary);
}
.pos-tercih-btn.aktif {
  background: var(--accent, var(--primary-color));
  border-color: var(--accent, var(--primary-color));
  color: var(--accent-contrast, #ffffff);
}
/* Bugunku satislar butonu + sayac rozeti */
.bugunku-btn {
  position: relative;
}
.bugunku-rozet {
  position: absolute;
  top: -6px;
  right: -6px;
  min-width: 17px;
  height: 17px;
  padding: 0 4px;
  border-radius: 999px;
  background: var(--accent);
  color: var(--accent-contrast, #04211d);
  font-size: 10.5px;
  font-weight: 800;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.satis-onay {
  text-align: center;
  padding: 8px 0;
}
.satis-onay-ikon {
  font-size: 2.4rem;
  color: var(--accent, var(--primary-color));
  margin-bottom: 10px;
}
.satis-onay-metin {
  font-size: 15px;
  color: var(--text-primary);
  margin: 0 0 8px;
}
.satis-onay-tutar {
  font-size: 18px;
  color: var(--text-primary);
  margin: 0;
}
.pos-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  gap: 12px;
  flex-wrap: wrap;
}
.breadcrumb {
  font-size: 13px;
  color: var(--text-muted);
}
.breadcrumb i {
  margin-right: 4px;
}
.user-info {
  font-size: 13px;
  color: var(--text-secondary);
}
.user-info i {
  margin-right: 4px;
}
.pos-header-sag {
  display: flex;
  align-items: center;
  gap: 10px;
}
.pos-ipucu-btn {
  border: 1px solid var(--border);
  background: var(--bg-secondary);
  color: var(--text-secondary);
  width: 30px;
  height: 30px;
  border-radius: 8px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.pos-ipucu-btn:hover {
  color: var(--accent);
  border-color: var(--accent);
}
.pos-ipucu {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  margin-bottom: 12px;
  border: 1px dashed var(--border);
  border-radius: 10px;
  background: var(--bg-secondary);
  font-size: 11.5px;
  color: var(--text-secondary);
}
.pos-ipucu span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  white-space: nowrap;
}
.pos-ipucu kbd {
  background: var(--bg-primary);
  border: 1px solid var(--border);
  border-bottom-width: 2px;
  border-radius: 5px;
  padding: 1px 6px;
  font-family: monospace;
  font-size: 11px;
  color: var(--text-primary);
}
.pos-ipucu-kapat {
  margin-left: auto;
  border: none;
  background: transparent;
  color: var(--text-muted);
  cursor: pointer;
  padding: 2px 6px;
}
.pos-ipucu-kapat:hover {
  color: var(--text-primary);
}
.form-grup {
  margin-bottom: 14px;
}
.form-grup label {
  display: block;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
  margin-bottom: 4px;
}
.kurulum-iki-kolon {
  display: flex;
  gap: 12px;
}
.kurulum-iki-kolon .form-grup {
  flex: 1;
  min-width: 0;
}
@media (max-width: 600px) {
  .kurulum-iki-kolon {
    flex-direction: column;
    gap: 0;
  }
}

.pos-body {
  display: grid;
  grid-template-columns: minmax(0, 1fr) clamp(340px, 30vw, 420px);
  gap: 16px;
  align-items: stretch;
}
.pos-left {
  min-width: 0;
}
.pos-right {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.siparis-kart :deep(.p-card-content) {
  display: flex;
  flex-direction: column;
  gap: 18px;
}
.pos-bolum {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.pos-bolum + .pos-bolum {
  border-top: 1px solid var(--border);
  padding-top: 18px;
}
.pos-bolum-baslik {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 700;
  color: var(--text-primary);
  text-transform: uppercase;
  letter-spacing: 0.4px;
}
.pos-bolum-baslik i {
  color: var(--accent);
  font-size: 14px;
}
.pos-bolum-baslik.sepet-baslik {
  justify-content: space-between;
}

/* Katlanabilir bolum basligi */
.katlanir-baslik {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  width: 100%;
  min-height: 40px;
  padding: 6px 4px;
  border: none;
  background: transparent;
  color: var(--text-primary);
  font: inherit;
  font-weight: 700;
  font-size: inherit;
  text-transform: uppercase;
  letter-spacing: 0.4px;
  cursor: pointer;
  border-radius: 8px;
  transition: background 0.15s;
}
.katlanir-baslik:hover {
  background: var(--bg-secondary);
}
.katlanir-baslik:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}
.katlanir-baslik-inline {
  width: auto;
  flex: 1;
  min-width: 0;
  justify-content: flex-start;
}
.katlanir-sol {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}
.katlanir-sol i {
  color: var(--accent);
  font-size: 14px;
}
.katlanir-ok {
  color: var(--text-secondary) !important;
  font-size: 12px !important;
}
.katlanir-rozet {
  font-size: 12px;
  font-weight: 600;
  color: var(--accent);
  background: var(--accent-soft);
  padding: 2px 8px;
  border-radius: 10px;
  text-transform: none;
  letter-spacing: 0;
  max-width: 140px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* Sticky Satisi Tamamla */
.sticky-tamamla {
  position: sticky;
  bottom: 0;
  z-index: 5;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 10px 0 6px;
  margin-top: 4px;
  background: var(--bg-card);
  border-top: 2px solid var(--border);
}
.sticky-tutar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-secondary);
}
.sticky-tutar strong {
  font-size: 20px;
  font-weight: 800;
  color: var(--accent);
}
.fis-modu-satir {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.fis-modu-etiket {
  font-size: 12px;
  color: var(--text-muted);
  display: inline-flex;
  align-items: center;
  gap: 5px;
  white-space: nowrap;
}
.fis-modu-satir :deep(.p-selectbutton) {
  flex: 1;
  justify-content: flex-end;
}
.fis-modu-satir :deep(.p-selectbutton .p-button) {
  padding: 4px 10px;
  font-size: 12px;
}

.filter-card :deep(.p-card-content) {
  padding-top: 0;
}
.filter-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.filter-row {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.filter-select {
  flex: 1;
}

.pos-arac-cubugu {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(0, 1.4fr) auto;
  gap: 8px;
  align-items: stretch;
  margin-bottom: 12px;
}
.pos-arac-cubugu .arama-kutusu {
  display: block;
  position: relative;
}
.pos-arac-cubugu .arama-kutusu .p-inputtext {
  width: 100%;
  height: 42px;
  padding-left: 2.75rem !important;
}
.pos-arac-cubugu .barkod-kutu .p-inputtext {
  padding-right: 2.75rem !important;
}
.pos-arac-cubugu .arama-kutusu > i {
  position: absolute;
  left: 0.9rem;
  top: 50%;
  transform: translateY(-50%);
  margin: 0;
  font-size: 15px;
  color: var(--text-muted);
  z-index: 1;
}
/* Barkod alanı içindeki kamera düğmesi (ayrı buton kalabalığını önler) */
.alan-ikon-sag {
  position: absolute;
  right: 0.4rem;
  top: 50%;
  transform: translateY(-50%);
  border: none;
  background: var(--bg-muted, rgba(148, 163, 184, 0.12));
  color: var(--text-secondary);
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all var(--dur-fast, 0.15s) var(--ease-standard, ease);
}
.alan-ikon-sag:hover {
  background: var(--accent-soft-strong);
  color: var(--accent);
}
/* Filtreler popover düğmesi */
.filtre-btn {
  position: relative;
  height: 42px;
  padding: 0 14px;
  border-radius: 10px;
  border: 1px solid var(--border);
  background: var(--bg-card);
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  cursor: pointer;
  transition: all var(--dur-fast, 0.15s) var(--ease-standard, ease);
}
.filtre-btn:hover {
  border-color: var(--accent-border);
  color: var(--text-primary);
}
.filtre-btn.filtre-aktif {
  border-color: var(--accent-border);
  background: var(--accent-soft);
  color: var(--accent);
}
.filtre-rozet {
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 999px;
  background: var(--accent);
  color: var(--accent-contrast, #04211d);
  font-size: 11px;
  font-weight: 800;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.filtre-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 260px;
}
.filtre-panel-baslik {
  font-size: 13px;
  font-weight: 700;
  color: var(--text-primary);
}
.filtre-alan label {
  display: block;
  font-size: 11px;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 4px;
}
/* Aktif filtre çipleri */
.aktif-filtreler {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin: -4px 0 10px;
}
.aktif-filtre-cip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 3px 8px 3px 10px;
  border-radius: 999px;
  background: var(--accent-soft);
  border: 1px solid var(--accent-border);
  color: var(--accent);
  font-size: 12px;
  font-weight: 600;
}
.aktif-filtre-cip button {
  border: none;
  background: transparent;
  color: inherit;
  cursor: pointer;
  display: inline-flex;
  padding: 2px;
}
.aktif-filtre-temizle {
  border: none;
  background: transparent;
  color: var(--text-muted);
  font-size: 12px;
  text-decoration: underline;
  cursor: pointer;
}
/* Tercihler popover paneli */
.tercih-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 240px;
}
.tercih-baslik {
  font-size: 13px;
  font-weight: 700;
  color: var(--text-primary);
}
.tercih-satir {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  cursor: pointer;
}
.tercih-metin {
  font-size: 13px;
  color: var(--text-secondary);
}
.katlanir-ikon-btn {
  border: none;
  background: transparent;
  color: var(--text-muted);
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 4px;
  border-radius: 6px;
}
.katlanir-ikon-btn:hover {
  color: var(--accent);
  background: var(--accent-soft);
}
.pos-arac-cubugu .arac-dropdown {
  height: 42px;
  width: 100%;
}
.pos-arac-cubugu .arac-dropdown :deep(.p-select),
.pos-arac-cubugu .arac-dropdown :deep(.p-dropdown) {
  width: 100%;
  height: 42px;
}
.pos-arac-cubugu .arac-dropdown :deep(.p-select-label),
.pos-arac-cubugu .arac-dropdown :deep(.p-dropdown-label) {
  line-height: 42px;
  padding-top: 0;
  padding-bottom: 0;
}
.pos-arac-cubugu .p-button {
  height: 42px;
}

@media (max-width: 900px) {
  .pos-arac-cubugu {
    grid-template-columns: 1fr 1fr;
  }
  .filtre-btn {
    grid-column: 1 / -1;
    justify-content: center;
  }
}
@media (max-width: 520px) {
  .pos-arac-cubugu {
    grid-template-columns: 1fr;
  }
  .filtre-btn-metin {
    display: none;
  }
}

.product-section {
  margin-top: 4px;
}
.product-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 10px;
}
.product-header h3 {
  font-size: 14px;
  font-weight: 600;
  margin: 0;
  color: var(--text-primary);
  display: flex;
  align-items: center;
  gap: 6px;
}
.product-header h3 i {
  color: var(--accent);
}
.product-header-sag {
  display: flex;
  align-items: center;
  gap: 8px;
}
.siralama-etiket {
  font-size: 11.5px;
  color: var(--text-muted);
}
.siralama-dropdown {
  min-width: 130px;
}
.urun-sayaci {
  font-size: 11px;
  font-weight: 700;
  color: var(--accent);
  background: var(--accent-soft);
  padding: 1px 8px;
  border-radius: 10px;
}
.kategori-cipler {
  display: flex;
  gap: 6px;
  overflow-x: auto;
  padding-bottom: 6px;
  margin-bottom: 8px;
  scrollbar-width: thin;
}
.kategori-cip {
  flex: 0 0 auto;
  border: 1px solid var(--border);
  background: var(--bg-card);
  color: var(--text-secondary);
  border-radius: 999px;
  padding: 4px 12px;
  font-size: 12px;
  cursor: pointer;
  transition: all var(--dur-fast, 0.15s) var(--ease-standard, ease);
}
.kategori-cip:hover {
  border-color: var(--accent-border);
  color: var(--text-primary);
}
.kategori-cip.aktif {
  background: var(--accent-soft-strong);
  border-color: var(--accent-border);
  color: var(--accent);
  font-weight: 700;
}
.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(178px, 1fr));
  gap: 12px;
  max-height: calc(100vh - 230px);
  max-height: calc(100dvh - 230px);
  overflow-y: auto;
  padding: 2px 2px 10px;
}
.daha-fazla {
  display: flex;
  justify-content: center;
  padding: 12px 0 4px;
}
.empty-products {
  grid-column: 1 / -1;
  text-align: center;
  padding: 40px;
  color: var(--text-muted);
}
.cok-satanlar-section {
  margin-top: 8px;
}
.cok-satanlar-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}
.cok-satan-chip {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
  padding: 8px 12px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.15s;
  text-align: left;
}
.cok-satan-chip:hover {
  border-color: var(--accent);
  transform: translateY(-1px);
}
.cok-satan-chip.stok-yok {
  opacity: 0.5;
  cursor: not-allowed;
}
.cok-satan-chip.stok-yok:hover {
  border-color: var(--border);
  transform: none;
}
.cok-satan-ad {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
}
.cok-satan-fiyat {
  font-size: 12px;
  font-weight: 700;
  color: var(--accent);
}
.empty-products i {
  font-size: 36px;
  display: block;
  margin-bottom: 8px;
}

.customer-field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.teslim-eden-alan {
  margin-bottom: 12px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}
.teslim-eden-alan label {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.teslim-eden-alan .zorunlu {
  color: var(--danger);
}
.teslimat-ipucu {
  margin: 0 0 4px;
  font-size: 12px;
  line-height: 1.4;
  color: var(--text-muted);
}
.teslim-durum-secim :deep(.p-selectbutton) {
  display: flex;
  flex-wrap: wrap;
}
.personel-opsiyon {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  min-width: 0;
}
.personel-opsiyon > span:not(.sofor-bekleyen):not(.sofor-rol-uyari) {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.personel-opsiyon i {
  font-size: 12px;
  color: var(--text-muted);
}
.personel-opsiyon .sofor-bekleyen {
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
.personel-opsiyon .sofor-rol-uyari {
  margin-left: auto;
  color: var(--warning);
  font-size: 12px;
}
.personel-opsiyon .sofor-rol-uyari + .sofor-bekleyen {
  margin-left: 0;
}
.anlik-musteri {
  margin-bottom: 4px;
}
.musteri-modu {
  display: flex;
}
.musteri-modu .p-selectbutton .p-button {
  flex: 1;
  justify-content: center;
}
.musteri-option {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.musteri-option-detay {
  font-size: 11px;
  color: var(--text-muted);
}
.secili-musteri-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  background: var(--accent-soft);
  border: 1px solid var(--accent-soft-strong);
  border-radius: 8px;
  padding: 6px 10px;
  font-size: 13px;
}
.secili-musteri-chip i {
  color: var(--accent);
  font-size: 14px;
}
.secili-musteri-ad {
  flex: 1;
  color: var(--text-primary);
  font-weight: 500;
}
.secili-musteri-sil {
  background: none;
  border: none;
  color: var(--text-muted);
  cursor: pointer;
  font-size: 13px;
  padding: 2px;
}
.secili-musteri-sil:hover {
  color: var(--danger);
}

.sepet-bolum {
  max-height: 350px;
  overflow-y: auto;
}
.sepet-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.sepet-baslik-btnler {
  display: flex;
  align-items: center;
  gap: 4px;
}
.sepet-bos {
  text-align: center;
  padding: 20px;
  color: var(--text-muted);
  font-size: 13px;
}
.sepet-item {
  padding: 10px 0;
  border-bottom: 1px solid var(--border);
}
.sepet-item.aktif-satir {
  background: var(--info-soft);
  box-shadow: inset 3px 0 0 var(--accent, var(--accent));
  border-radius: 8px;
}
.sepet-item:last-child {
  border-bottom: none;
}
.sepet-ust {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 6px;
}
.sepet-kod {
  font-size: 11.5px;
  font-weight: 700;
  color: var(--text-secondary);
  background: var(--bg-secondary);
  padding: 2px 6px;
  border-radius: 5px;
  white-space: nowrap;
  max-width: 90px;
  overflow: hidden;
  text-overflow: ellipsis;
  flex-shrink: 0;
}
.sepet-ad {
  flex: 1;
  font-size: 13.5px;
  font-weight: 600;
  color: var(--text-primary);
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.sepet-sil {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: var(--text-secondary);
  font-size: 16px;
  cursor: pointer;
  transition: all 0.15s;
}
.pos-buyuk .sepet-kod {
  font-size: 13px;
}
.pos-buyuk .sepet-ad {
  font-size: 16px;
}
.pos-buyuk .sepet-sil {
  width: 42px;
  height: 42px;
  font-size: 19px;
}
.sepet-sil:hover {
  background: var(--danger-soft);
  color: var(--danger);
}
.sepet-kontroller {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  justify-content: flex-end;
  min-width: 0;
}
.sepet-adet-grup {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}
.adet-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--bg-secondary);
  color: var(--text-secondary);
  font-size: 16px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s;
}
.adet-btn:hover {
  border-color: var(--accent);
  color: var(--accent);
}
.sepet-adet-input {
  width: clamp(44px, 7vw, 52px);
  text-align: center;
  font-weight: 700;
  font-size: 14px;
  height: 34px;
  background: var(--bg-primary);
  color: var(--text-primary);
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 0 4px;
  outline: none;
}
/* Buyuk yazi modu: adet/fiyat kontrolleri daha da buyuk */
.pos-buyuk .adet-btn {
  width: 42px;
  height: 42px;
  font-size: 19px;
}
.pos-buyuk .sepet-adet-input {
  width: clamp(52px, 8vw, 62px);
  height: 42px;
  font-size: 17px;
}
/* Mobil: POS adet/fiyat kontrolleri dokunma hedefi >=40px */
@media (max-width: 900px) {
  .adet-btn {
    width: 40px;
    height: 40px;
  }
  .sepet-adet-input,
  .fiyat-giris-input {
    height: 40px;
    font-size: 14px;
  }
}
.odeme-yontem-grid {
  display: flex;
  gap: 6px;
}
.taksit-panel {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 10px;
  border: 1px solid rgba(139, 92, 246, 0.3);
  border-radius: 10px;
  background: rgba(139, 92, 246, 0.08);
  margin-top: 8px;
}
.odeme-yontem-btn {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 8px 4px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--bg-primary);
  color: var(--text-secondary);
  cursor: pointer;
  font-size: 11px;
  font-weight: 600;
  transition: all 0.15s;
}
.odeme-yontem-btn:hover {
  border-color: var(--accent);
}
.odeme-yontem-btn.active {
  border-color: var(--accent);
  background: var(--accent-soft);
  color: var(--accent);
}
.odeme-yontem-btn i {
  font-size: 16px;
}
.para-ustu {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
  padding: 8px 12px;
  background: var(--success-soft);
  border: 1px solid var(--success-border);
  border-radius: 8px;
  font-size: 14px;
}
.para-ustu strong {
  color: var(--success);
  font-size: 16px;
}
.musteri-bakiye-uyari {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 600;
}
.musteri-bakiye-uyari.danger {
  background: var(--danger-soft);
  color: var(--danger);
  border: 1px solid var(--danger-border);
}
.musteri-bakiye-uyari.warn {
  background: var(--warning-soft);
  color: var(--warning);
  border: 1px solid rgba(245, 158, 11, 0.25);
}
.musteri-bakiye-uyari.info {
  background: var(--accent-soft);
  color: var(--accent);
  border: 1px solid var(--accent-soft-strong);
}
.degisim-bilgi {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 6px;
  padding: 8px 10px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 600;
  background: var(--accent-soft);
  color: var(--accent);
  border: 1px solid var(--accent-soft-strong);
}
.degisim-bilgi span {
  flex: 1;
}
.degisim-kapat {
  background: none;
  border: none;
  color: inherit;
  cursor: pointer;
  padding: 2px;
  display: inline-flex;
}
.gunluk-baslik {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 10px;
}
.sepet-birimfiyat {
  font-size: 11px;
  color: var(--text-muted);
  margin-left: auto;
}
.sepet-tutar {
  font-size: 13px;
  font-weight: 700;
  min-width: 60px;
  text-align: right;
}
.sepet-son-alis {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 4px;
  padding: 5px 8px;
  font-size: 12px;
  color: var(--text-secondary);
  background: var(--warning-soft);
  border: 1px solid var(--warning-border);
  border-radius: 8px;
}
.sepet-son-alis i {
  font-size: 12px;
  color: var(--warning);
}
.sepet-son-alis strong {
  color: var(--accent);
}

.ozet-satir {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 0;
  font-size: 13px;
}
.ozet-indirim {
  display: flex;
  align-items: center;
  gap: 6px;
}
.indirim-input {
  width: 100px;
}
.ozet-ayrac {
  border: none;
  border-top: 1px solid var(--border);
  margin: 6px 0;
}
.ozet-detay-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 34px;
  padding: 4px 6px;
  border: none;
  background: transparent;
  color: var(--text-secondary);
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
  border-radius: 6px;
  transition: color 0.15s, background 0.15s;
}
.ozet-detay-btn:hover {
  color: var(--text-primary);
  background: var(--bg-secondary);
}
.pos-buyuk .ozet-detay-btn {
  font-size: 15px;
  min-height: 40px;
}
.ozet-genel {
  border-top: 2px solid var(--border);
  margin-top: 4px;
  padding-top: 8px;
}
.genel-toplam-deger {
  font-size: 18px;
  font-weight: 800;
  color: var(--accent);
}

.siparis-kart :deep(.p-selectbutton) {
  display: flex;
}
.siparis-kart :deep(.p-selectbutton .p-button) {
  flex: 1;
  font-size: 12px;
}
.odenen-satir {
  margin-top: 8px;
}
.odenen-satir label {
  display: block;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
  margin-bottom: 4px;
}
.pos-komisyon-not {
  display: block;
  margin-top: 4px;
  font-size: 11px;
  color: var(--text-muted);
  line-height: 1.4;
}
.odeme-durum {
  margin-top: 8px;
}
.odeme-durum :deep(.p-tag) {
  justify-content: center;
}
.odeme-kalan {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 0;
  font-size: 13px;
}
.kalan-deger {
  font-weight: 700;
  color: var(--accent);
}

.fis-card :deep(.p-card-content) {
  padding: 0;
}
.fis-baslik-satir {
  justify-content: space-between;
}
.fis-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  gap: 8px;
  flex-wrap: wrap;
}
.fis-ayarlar {
  display: flex;
  align-items: center;
  gap: 8px;
}
.fis-ayarlar .p-selectbutton .p-button {
  padding: 4px 10px;
  font-size: 11px;
}
.fis-onizleme-kapsam {
  overflow-x: auto;
  padding: 12px;
  background: var(--bg-secondary);
  border-radius: 0 0 8px 8px;
}


.satis-buton {
  margin-top: 4px;
}

.ym-form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}
.ym-form-grid .full-width {
  grid-column: span 2;
}
.dialog-footer-btns {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
  width: 100%;
}

.field {
  margin-bottom: 12px;
}
.field label {
  display: block;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 6px;
}
.required {
  color: var(--danger);
}

.fiyat-tip-select {
  flex: 1;
  min-width: 0;
  height: 26px;
  background: var(--bg-primary);
  color: var(--text-primary);
  border: 1px solid var(--border);
  border-radius: 6px;
  font-size: 11px;
  padding: 0 4px;
  outline: none;
}
.fiyat-giris-input {
  width: 68px;
  height: 26px;
  background: var(--bg-primary);
  color: var(--text-primary);
  border: 1px solid var(--border);
  border-radius: 6px;
  font-size: 12px;
  padding: 0 4px;
  text-align: right;
  outline: none;
}

@media (max-width: 1280px) {
  /* Dar dizustu/tablet: sag sutun alta iner; sabit 400px yerine tam genislik. */
  .pos-body {
    grid-template-columns: minmax(0, 1fr);
  }
}
@media (max-width: 1100px) {
  .product-grid {
    grid-template-columns: repeat(auto-fill, minmax(min(160px, 100%), 1fr));
  }
}

/* ======================= POS TASARIM YENILEME =======================
   Görsel iyileştirme katmanı; işlev/akış değişmez, tümü tema değişkenli. */
.pos-container {
  gap: 14px;
}
.pos-header {
  position: sticky;
  top: 0;
  z-index: 20;
  padding: 10px 16px;
  background: var(--bg-header, var(--bg-card));
  border: 1px solid var(--border);
  border-radius: 14px;
  backdrop-filter: blur(10px);
}
.breadcrumb {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
  color: var(--text-secondary);
}
.pos-header-sag {
  gap: 8px;
}
.pos-ikon-btn,
.pos-tercih-btn,
.pos-ipucu-btn {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  border: 1px solid var(--border);
  background: var(--bg-card);
  color: var(--text-secondary);
  transition: color var(--dur-fast, 0.15s) ease, border-color var(--dur-fast, 0.15s) ease, background var(--dur-fast, 0.15s) ease;
}
.pos-ikon-btn:hover,
.pos-tercih-btn:hover,
.pos-ipucu-btn:hover {
  color: var(--accent);
  border-color: var(--accent-border);
}
.pos-tercih-btn.aktif {
  color: var(--accent);
  border-color: var(--accent-border);
  background: var(--accent-soft);
}
.pos-body {
  gap: 14px;
  align-items: start;
}
.pos-arac-cubugu {
  gap: 10px;
  padding: 12px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 14px;
}
.product-grid {
  gap: 12px;
}
.siparis-kart {
  border-radius: 14px;
  border: 1px solid var(--border);
}
:deep(.siparis-kart .p-card-body) {
  padding: 14px;
}
:deep(.siparis-kart .p-card-content) {
  padding: 0;
}
.sepet-item {
  border-radius: 10px;
}
.sepet-item.aktif-satir {
  background: var(--accent-soft);
  box-shadow: inset 3px 0 0 var(--accent);
}
.odeme-yontem-btn {
  border-radius: 12px;
  border: 1px solid var(--border);
  transition: border-color var(--dur-fast, 0.15s) ease, background var(--dur-fast, 0.15s) ease, transform var(--dur-fast, 0.15s) ease;
}
.odeme-yontem-btn:hover {
  border-color: var(--accent-border);
  transform: translateY(-1px);
}
.sticky-tamamla {
  border-radius: 12px;
  box-shadow: var(--elev-2, 0 8px 24px rgba(0, 0, 0, 0.35));
}
.empty-products {
  color: var(--text-muted);
}

/* Marka + baslik */
.pos-marka {
  display: flex;
  align-items: center;
  gap: 10px;
}
.pos-marka-ikon {
  width: 38px;
  height: 38px;
  border-radius: 11px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  color: #fff;
  background: linear-gradient(135deg, var(--accent), var(--accent-hover));
  box-shadow: var(--elev-1, 0 2px 10px rgba(0, 0, 0, 0.25));
}
.pos-marka-metin {
  display: flex;
  flex-direction: column;
  line-height: 1.15;
}
.pos-marka-metin strong {
  font-size: 15px;
  color: var(--text-primary);
}
.pos-marka-metin small {
  font-size: 11.5px;
  color: var(--text-muted);
}

/* Komut cubugu */
.barkod-kutu :deep(.p-inputtext) {
  font-weight: 600;
}
.arama-kutusu :deep(.p-inputtext) {
  width: 100%;
}
.filtre-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  border-radius: 10px;
  border: 1px solid var(--border);
  background: var(--bg-card);
  color: var(--text-secondary);
  cursor: pointer;
  transition: all var(--dur-fast, 0.15s) ease;
}
.filtre-btn:hover {
  color: var(--accent);
  border-color: var(--accent-border);
}
.filtre-btn.filtre-aktif {
  color: var(--accent);
  border-color: var(--accent-border);
  background: var(--accent-soft);
}
.filtre-rozet {
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 999px;
  background: var(--accent);
  color: var(--accent-contrast, #04211d);
  font-size: 11px;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

/* Kategori + cok satan cipleri */
.kategori-cip {
  border-radius: 999px;
  transition: all var(--dur-fast, 0.15s) ease;
}
.kategori-cip.aktif {
  background: var(--accent);
  color: var(--accent-contrast, #04211d);
  border-color: var(--accent);
}
.cok-satan-chip {
  border-radius: 12px;
  transition: transform var(--dur-fast, 0.15s) ease, border-color var(--dur-fast, 0.15s) ease;
}
.cok-satan-chip:hover:not(:disabled) {
  transform: translateY(-2px);
  border-color: var(--accent-border);
}

/* Sepet satiri */
.sepet-icerik {
  gap: 8px;
}
.sepet-item {
  padding: 10px 12px;
  border: 1px solid transparent;
}
.sepet-item:hover {
  border-color: var(--border);
}
.sepet-tutar {
  font-variant-numeric: tabular-nums;
  font-weight: 700;
}

/* Odeme yontemi secili */
.odeme-yontem-btn.aktif {
  border-color: var(--accent);
  background: var(--accent-soft);
  color: var(--accent);
  font-weight: 700;
}

/* ======================= C1: VIEWPORT'A BAGLI KASA =======================
   Sayfa scroll'u kaldirilir; sol urun alani ve sag siparis paneli kendi
   scroll'une sahip; toplam/odeme/tamamla sagda sabit (sticky) kalir. */
.pos-container {
  display: flex;
  flex-direction: column;
  height: calc(100dvh - 52px);
  min-height: 520px;
  overflow: hidden;
}
.pos-header,
.pos-ipucu {
  flex: 0 0 auto;
}
.pos-body {
  flex: 1 1 auto;
  min-height: 0;
  overflow: hidden;
  align-items: stretch;
}
.pos-left {
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}
.pos-arac-cubugu,
.aktif-filtreler,
.cok-satanlar-section {
  flex: 0 0 auto;
}
.product-section {
  flex: 1 1 auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.product-grid {
  flex: 1 1 auto;
  min-height: 0;
  max-height: none !important;
  overflow-y: auto;
  align-content: start;
}
.pos-right {
  min-height: 0;
  overflow: hidden;
  display: flex;
}
.pos-right :deep(.p-card),
.siparis-kart {
  width: 100%;
  display: flex;
  flex-direction: column;
  min-height: 0;
}
.siparis-kart :deep(.p-card-body) {
  display: flex;
  flex-direction: column;
  min-height: 0;
  flex: 1 1 auto;
}
.siparis-kart :deep(.p-card-content) {
  display: flex;
  flex-direction: column;
  min-height: 0;
  flex: 1 1 auto;
  overflow-y: auto;
}
.sepet-bolum {
  max-height: none !important;
}
.sepet-icerik {
  max-height: none !important;
  overflow: visible;
}
.sticky-tamamla {
  position: sticky;
  bottom: 0;
  z-index: 5;
  background: var(--bg-card);
}
@media (max-width: 1280px) {
  /* Tek sutuna dusunce viewport kilidini kaldir; sayfa normal kaydirilsin. */
  .pos-container {
    height: auto;
    min-height: 0;
    overflow: visible;
  }
  .pos-body,
  .pos-left,
  .product-section,
  .pos-right,
  .siparis-kart :deep(.p-card-content) {
    overflow: visible;
  }
  .product-grid {
    overflow: visible;
  }
}

/* C2: sepete yeni eklenen satir vurgusu */
.sepet-item.yeni-satir {
  animation: sepetPulse 0.7s ease;
}
@keyframes sepetPulse {
  0% {
    background: var(--accent-soft-strong);
  }
  100% {
    background: transparent;
  }
}
@media (prefers-reduced-motion: reduce) {
  .sepet-item.yeni-satir {
    animation: none;
  }
}

/* C3: dokunma hedefleri + sepet basligi + geri al */
.sepet-baslik-toggle {
  background: none;
  border: none;
  color: var(--text-primary);
  font-weight: 700;
  cursor: pointer;
  padding: 4px 6px;
  border-radius: 8px;
  font-size: 14px;
  text-align: left;
}
.sepet-baslik-toggle:hover {
  background: var(--bg-muted, rgba(148, 163, 184, 0.08));
}
.geri-al-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 8px 12px;
  margin: 6px 0;
  border-radius: 10px;
  background: var(--warning-soft);
  border: 1px solid var(--warning-border);
  color: var(--warning);
  font-size: 12.5px;
  font-weight: 600;
}
.geri-al-bar button {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  border-radius: 8px;
  border: 1px solid var(--accent-border);
  background: var(--bg-card);
  color: var(--accent);
  font-weight: 700;
  cursor: pointer;
}
.adet-btn,
.sepet-adet-input,
.fiyat-tip-select,
.fiyat-giris-input {
  min-height: 40px;
}
.adet-btn {
  min-width: 40px;
}
.filtre-alan-toggle {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  cursor: pointer;
}
.kategori-cip-daha {
  border-style: dashed;
}

/* Hizli nakit + surukle-birak */
.hizli-nakit {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}
.hizli-nakit-btn {
  flex: 1 1 auto;
  min-width: 52px;
  min-height: 38px;
  border-radius: 10px;
  border: 1px solid var(--border);
  background: var(--bg-card);
  color: var(--text-primary);
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  cursor: pointer;
  transition: all var(--dur-fast, 0.15s) ease;
}
.hizli-nakit-btn:hover {
  border-color: var(--accent-border);
  color: var(--accent);
}
.hizli-nakit-btn.tam {
  background: var(--accent-soft);
  color: var(--accent);
  border-color: var(--accent-border);
}
.sepet-tutamac {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  flex-shrink: 0;
  color: var(--text-muted);
  cursor: grab;
}
.sepet-tutamac:active {
  cursor: grabbing;
}
.sepet-item.surukleniyor {
  opacity: 0.5;
}
</style>

<style>
@media print {
  body * {
    visibility: hidden;
  }
  #fisOnizleme,
  #fisOnizleme * {
    visibility: visible;
  }
  #fisOnizleme {
    position: fixed;
    top: 0;
    left: 0;
    width: 80mm;
    padding: 10mm;
    background: white;
    color: black;
    font-size: 12px;
    font-family: 'Courier New', monospace;
  }
}
</style>
