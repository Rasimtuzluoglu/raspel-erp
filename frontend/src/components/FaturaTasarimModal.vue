<template>
  <Dialog
    :visible="visible"
    :modal="true"
    :closable="true"
    :maximizable="true"
    :header="$t('faturaTasarim.title')"
    class="fatura-tasarim-dialog"
    :style="{ width: '96vw', maxWidth: '1400px', height: '92vh' }"
    @update:visible="$emit('update:visible', $event)"
  >
    <div class="tasarim-container">
      <!-- SOL PANEL: Tasarım & Görünüm Ayarları -->
      <div class="tasarim-sidebar no-print">
        <div class="sidebar-header">
          <h3><i class="pi pi-sliders-h" /> {{ $t('faturaTasarim.sablonAyarlari') }}</h3>
          <p class="sidebar-sub">
            {{ $t('faturaTasarim.sablonAyarlariAlt') }}
          </p>
        </div>

        <div class="ayar-gruplari">
          <!-- 1. Hazır Şablon Modeli -->
          <div class="ayar-kutu">
            <label class="ayar-baslik"><i class="pi pi-palette" /> {{ $t('faturaTasarim.sablonTemasi') }}</label>
            <div class="sablon-secici">
              <button
                v-for="s in sablonSecenekleri"
                :key="s.id"
                type="button"
                :class="['sablon-btn', { aktif: ayarlar.sablon === s.id }]"
                @click="sablonDegistir(s.id)"
              >
                <i :class="s.icon" />
                <span>{{ s.ad }}</span>
              </button>
            </div>
          </div>

          <!-- 2. Kağıt Boyutu & Sayfa Düzeni -->
          <div class="ayar-kutu">
            <label class="ayar-baslik"><i class="pi pi-file" /> {{ $t('faturaTasarim.kagitBoyutuDuzen') }}</label>
            <div class="form-satir">
              <label class="alt-etiket">{{ $t('faturaTasarim.sayfaFormati') }}</label>
              <Dropdown
                v-model="ayarlar.kagitBoyutu"
                :options="kagitBoyutSecenekleri"
                option-label="label"
                option-value="value"
                class="w-full"
              />
            </div>

            <div
              v-if="!ayarlar.kagitBoyutu.startsWith('termal')"
              class="form-satir mt-2"
            >
              <label class="alt-etiket">{{ $t('faturaTasarim.sayfaYonu') }}</label>
              <SelectButton
                v-model="ayarlar.sayfaYonu"
                :options="sayfaYonuSecenekleri"
                option-label="label"
                option-value="value"
                size="small"
                class="w-full"
              />
            </div>

            <div class="form-satir mt-2">
              <label class="alt-etiket">{{ $t('faturaTasarim.kenarBoslugu') }}</label>
              <SelectButton
                v-model="ayarlar.kenarBoslugu"
                :options="kenarBoslukSecenekleri"
                option-label="label"
                option-value="value"
                size="small"
                class="w-full"
              />
            </div>
          </div>

          <!-- 3. Vurgu Rengi & Yazı Tipi -->
          <div class="ayar-kutu">
            <label class="ayar-baslik"><i class="pi pi-sparkles" /> {{ $t('faturaTasarim.vurguRengi') }}</label>
            <div class="renk-paleti">
              <button
                v-for="r in renkSecenekleri"
                :key="r"
                type="button"
                :class="['renk-btn', { aktif: ayarlar.renk === r }]"
                :style="{ backgroundColor: r }"
                @click="ayarlar.renk = r"
              />
              <div class="ozel-renk-wrapper">
                <input
                  v-model="ayarlar.renk"
                  type="color"
                  :title="$t('faturaTasarim.ozelRenkSec')"
                  class="ozel-renk-input"
                >
              </div>
            </div>

            <div class="font-secici mt-3">
              <label class="alt-etiket">{{ $t('faturaTasarim.yaziTipi') }}</label>
              <Dropdown
                v-model="ayarlar.fontFamily"
                :options="fontSecenekleri"
                option-label="label"
                option-value="value"
                class="w-full"
              />
            </div>
          </div>

          <!-- 3. Başlık & Logo Ayarları -->
          <div class="ayar-kutu">
            <label class="ayar-baslik"><i class="pi pi-id-card" /> {{ $t('faturaTasarim.belgeBaslik') }}</label>
            <div class="form-satir">
              <label class="alt-etiket">{{ $t('faturaTasarim.faturaBasligi') }}</label>
              <InputText
                v-model="ayarlar.faturaBasligi"
                :placeholder="$t('faturaTasarim.faturaBasligiPlaceholder')"
                class="w-full"
              />
            </div>
            <div class="form-satir mt-2">
              <label class="alt-etiket">{{ $t('faturaTasarim.altBaslikSlogan') }}</label>
              <InputText
                v-model="ayarlar.altBaslik"
                :placeholder="$t('faturaTasarim.altBaslikPlaceholder')"
                class="w-full"
              />
            </div>

            <div class="toggle-satir mt-3">
              <span>{{ $t('faturaTasarim.sirketLogosuGoster') }}</span>
              <InputSwitch v-model="ayarlar.logoGoster" />
            </div>
            <div
              v-if="ayarlar.logoGoster"
              class="logo-ayarlari mt-2"
            >
              <div class="form-satir">
                <label class="alt-etiket">{{ $t('faturaTasarim.logoBoyutu') }}</label>
                <SelectButton
                  v-model="ayarlar.logoBoyutu"
                  :options="logoBoyutSecenekleri"
                  option-label="label"
                  option-value="value"
                  size="small"
                />
              </div>
              <div class="form-satir mt-2">
                <label class="alt-etiket">{{ $t('faturaTasarim.logoKonumu') }}</label>
                <SelectButton
                  v-model="ayarlar.logoKonumu"
                  :options="logoKonumSecenekleri"
                  option-label="label"
                  option-value="value"
                  size="small"
                />
              </div>
            </div>
          </div>

          <!-- 4. Şirket & Cari Bilgi Alanları -->
          <div class="ayar-kutu">
            <label class="ayar-baslik"><i class="pi pi-building" /> {{ $t('faturaTasarim.firmaMusteriBilgileri') }}</label>
            <div class="toggle-listesi">
              <div class="toggle-satir">
                <span>{{ $t('faturaTasarim.firmaVergiNoDairesi') }}</span>
                <InputSwitch v-model="ayarlar.firmaVergiBilgileri" />
              </div>
              <div class="toggle-satir">
                <span>{{ $t('faturaTasarim.firmaAdresiIletisim') }}</span>
                <InputSwitch v-model="ayarlar.firmaAdresBilgileri" />
              </div>
              <div class="toggle-satir">
                <span>{{ $t('faturaTasarim.mersisTicaretSicil') }}</span>
                <InputSwitch v-model="ayarlar.firmaMersisGoster" />
              </div>
              <div class="toggle-satir">
                <span>{{ $t('faturaTasarim.musteriCariDetaylari') }}</span>
                <InputSwitch v-model="ayarlar.cariDetayGoster" />
              </div>
            </div>
          </div>

          <!-- 5. Tablo Kolonları -->
          <div class="ayar-kutu">
            <label class="ayar-baslik"><i class="pi pi-table" /> {{ $t('faturaTasarim.faturaKalemKolonlari') }}</label>
            <div class="toggle-listesi">
              <div class="toggle-satir">
                <span>{{ $t('faturaTasarim.siraNo') }}</span>
                <InputSwitch v-model="ayarlar.kolonSiraNo" />
              </div>
              <div class="toggle-satir">
                <span>{{ $t('faturaTasarim.stokUrunKodu') }}</span>
                <InputSwitch v-model="ayarlar.kolonStokKodu" />
              </div>
              <div class="toggle-satir">
                <span>{{ $t('faturaTasarim.iskontoSutunu') }}</span>
                <InputSwitch v-model="ayarlar.kolonIskonto" />
              </div>
              <div class="toggle-satir">
                <span>{{ $t('faturaTasarim.kdvOraniSutunu') }}</span>
                <InputSwitch v-model="ayarlar.kolonKdvOrani" />
              </div>
              <div class="toggle-satir">
                <span>{{ $t('faturaTasarim.fiyatVeTutarlar') }}</span>
                <InputSwitch v-model="ayarlar.fiyatGoster" />
              </div>
            </div>
          </div>

          <!-- 6. Banka & İmza & Karekod -->
          <div class="ayar-kutu">
            <label class="ayar-baslik"><i class="pi pi-credit-card" /> {{ $t('faturaTasarim.bankaImzaEkstra') }}</label>
            <div class="toggle-listesi">
              <div class="toggle-satir">
                <span>{{ $t('faturaTasarim.bankaIbanBilgisi') }}</span>
                <InputSwitch v-model="ayarlar.bankaGoster" />
              </div>
              <div
                v-if="ayarlar.bankaGoster"
                class="banka-input-alani"
              >
                <InputText
                  v-model="ayarlar.bankaAdi"
                  :placeholder="$t('faturaTasarim.bankaAdiPlaceholder')"
                  class="w-full mb-1"
                />
                <InputText
                  v-model="ayarlar.ibanNo"
                  :placeholder="$t('faturaTasarim.ibanPlaceholder')"
                  class="w-full mb-1"
                />
                <InputText
                  v-model="ayarlar.hesapSahibi"
                  :placeholder="$t('faturaTasarim.hesapSahibiPlaceholder')"
                  class="w-full"
                />
              </div>

              <div class="toggle-satir mt-2">
                <span>{{ $t('faturaTasarim.karekodAlani') }}</span>
                <InputSwitch v-model="ayarlar.qrKodGoster" />
              </div>

              <div class="toggle-satir mt-2">
                <span>{{ $t('faturaTasarim.imzaKutusuAlani') }}</span>
                <InputSwitch v-model="ayarlar.imzaKutusuGoster" />
              </div>

              <div class="toggle-satir mt-2">
                <span>{{ $t('faturaTasarim.odemeDurumuRozeti') }}</span>
                <InputSwitch v-model="ayarlar.odemeDurumuGoster" />
              </div>
            </div>
          </div>

          <!-- 7. Dipnot & Hukuki Şartlar -->
          <div class="ayar-kutu">
            <label class="ayar-baslik"><i class="pi pi-align-left" /> {{ $t('faturaTasarim.dipnotHukuki') }}</label>
            <div class="toggle-satir mb-2">
              <span>{{ $t('faturaTasarim.dipnotAlaniniGoster') }}</span>
              <InputSwitch v-model="ayarlar.dipnotGoster" />
            </div>
            <div v-if="ayarlar.dipnotGoster">
              <Textarea
                v-model="ayarlar.dipnotMetni"
                rows="3"
                class="w-full"
                :placeholder="$t('faturaTasarim.dipnotPlaceholder')"
              />
            </div>
          </div>
        </div>

        <!-- Alt Aksiyon Butonları (Kaydet & Sıfırla) -->
        <div class="sidebar-footer">
          <button
            type="button"
            class="p-button p-button-success w-full mb-2"
            @click="sablonuKaydet"
          >
            <i
              class="pi pi-save"
              style="margin-right: 6px"
            /> {{ $t('faturaTasarim.varsayilanOlarakKaydet') }}
          </button>
          <button
            type="button"
            class="p-button p-button-outlined p-button-secondary w-full"
            @click="sablonuSifirla"
          >
            <i
              class="pi pi-refresh"
              style="margin-right: 6px"
            /> {{ $t('faturaTasarim.varsayilanaSifirla') }}
          </button>
        </div>
      </div>

      <!-- SAĞ PANEL: Canlı A4 Fatura Önizlemesi & Yazdırma Alanı -->
      <div class="tasarim-onizleme-alani">
        <div class="onizleme-toolbar no-print">
          <div class="zoom-kontroller">
            <button
              type="button"
              class="p-button p-button-text p-button-sm"
              :aria-label="$t('common.zoomOut')"
              :disabled="zoomOrani <= 0.5"
              @click="zoomOrani = Math.max(0.5, zoomOrani - 0.1)"
            >
              <i class="pi pi-search-minus" />
            </button>
            <span class="zoom-text">{{ Math.round(zoomOrani * 100) }}%</span>
            <button
              type="button"
              class="p-button p-button-text p-button-sm"
              :aria-label="$t('common.zoomIn')"
              :disabled="zoomOrani >= 1.3"
              @click="zoomOrani = Math.min(1.3, zoomOrani + 0.1)"
            >
              <i class="pi pi-search-plus" />
            </button>
          </div>
          <div class="toolbar-aksiyonlar">
            <button
              type="button"
              class="p-button p-button-primary"
              @click="yazdir"
            >
              <i
                class="pi pi-print"
                style="margin-right: 6px"
              /> {{ $t('faturaTasarim.hemenYazdirPdf') }}
            </button>
          </div>
        </div>

        <!-- Kağıt / Belge Önizleme Konteyneri -->
        <div class="a4-scroll-wrapper">
          <component :is="'style'">
            @media print {
            @page {
            size: {{ printPageSize }} !important;
            margin: {{ ayarlar.kenarBoslugu }} !important;
            }
            }
          </component>
          <div
            id="fatura-yazdirma-alani"
            class="fatura-a4-kagit"
            :class="[
              'tema-' + ayarlar.sablon,
              'font-' + ayarlar.fontFamily,
              'kagit-' + ayarlar.kagitBoyutu,
              'yon-' + ayarlar.sayfaYonu
            ]"
            :style="{
              '--vurgu-renk': ayarlar.renk,
              transform: `scale(${zoomOrani})`,
              transformOrigin: 'top center'
            }"
          >
            <!-- 1. ÜST BAŞLIK ALANI (HEADER) -->
            <div class="fatura-ust-header">
              <!-- Sol veya Konuma Göre Logo & Firma -->
              <div
                class="header-sol"
                :class="'logo-pos-' + ayarlar.logoKonumu"
              >
                <div
                  v-if="ayarlar.logoGoster && sirketLogo"
                  class="fatura-logo-kutu"
                  :class="'logo-boyut-' + ayarlar.logoBoyutu"
                >
                  <img
                    :src="sirketLogo"
                    alt="Logo"
                    class="fatura-logo-img"
                  >
                </div>
                <div class="firma-detaylari">
                  <h2 class="firma-unvani">
                    {{ sirket?.ad || authStore.sirketAdi || $t('faturaTasarim.firmaAdiFallback') }}
                  </h2>
                  <div
                    v-if="ayarlar.firmaVergiBilgileri"
                    class="firma-alt-bilgi"
                  >
                    <span v-if="sirket?.vergiDairesi"><strong>{{ $t('faturaTasarim.vdKisa') }}</strong> {{ sirket.vergiDairesi }}</span>
                    <span v-if="sirket?.vergiNo"><strong>{{ $t('faturaTasarim.vNoKisa') }}</strong> {{ sirket.vergiNo }}</span>
                  </div>
                  <div
                    v-if="ayarlar.firmaAdresBilgileri"
                    class="firma-alt-bilgi"
                  >
                    <p
                      v-if="sirket?.adres"
                      class="firma-adres-metin"
                    >
                      {{ sirket.adres }}
                    </p>
                    <span v-if="sirket?.telefon"><strong>{{ $t('faturaTasarim.telEtiket') }}</strong> {{ sirket.telefon }}</span>
                    <span v-if="sirket?.email"><strong>{{ $t('faturaTasarim.epostaEtiket') }}</strong> {{ sirket.email }}</span>
                    <span v-if="sirket?.webSite"><strong>{{ $t('faturaTasarim.webEtiket') }}</strong> {{ sirket.webSite }}</span>
                  </div>
                  <div
                    v-if="ayarlar.firmaMersisGoster"
                    class="firma-alt-bilgi"
                  >
                    <span><strong>{{ $t('faturaTasarim.mersisTicSicilEtiket') }}</strong> 012345678900001</span>
                  </div>
                </div>
              </div>

              <!-- Sağ: Fatura Belge Bilgileri -->
              <div class="header-sag">
                <div class="fatura-ana-baslik-band">
                  <h1>{{ ayarlar.faturaBasligi || (aktifFatura?.tur === 'ALIS' ? $t('faturaTasarim.alisFaturasi') : $t('faturaTasarim.satisFaturasi')) }}</h1>
                  <p
                    v-if="ayarlar.altBaslik"
                    class="fatura-alt-slogan"
                  >
                    {{ ayarlar.altBaslik }}
                  </p>
                </div>
                <div class="belge-meta-tablo">
                  <div class="meta-satir">
                    <span class="meta-label">{{ $t('faturaTasarim.faturaNoEtiket') }}</span>
                    <span class="meta-deger">{{ aktifFatura?.faturaNumarasi || 'FTR-2026-0001' }}</span>
                  </div>
                  <div class="meta-satir">
                    <span class="meta-label">{{ $t('faturaTasarim.duzenlemeTarihi') }}</span>
                    <span class="meta-deger">{{ formatDate(aktifFatura?.tarih) }}</span>
                  </div>
                  <div
                    v-if="aktifFatura?.vadeTarihi"
                    class="meta-satir"
                  >
                    <span class="meta-label">{{ $t('faturaTasarim.vadeTarihi') }}</span>
                    <span class="meta-deger">{{ formatDate(aktifFatura?.vadeTarihi) }}</span>
                  </div>
                  <div
                    v-if="ayarlar.odemeDurumuGoster && aktifFatura?.odemeDurumu"
                    class="meta-satir"
                  >
                    <span class="meta-label">{{ $t('faturaTasarim.odemeDurumuEtiket') }}</span>
                    <span class="meta-deger durum-etiket">{{ odemeDurumLabel(aktifFatura.odemeDurumu) }}</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- 2. CARİ / MÜŞTERİ BİLGİ KUTUSU -->
            <div
              v-if="ayarlar.cariDetayGoster"
              class="fatura-cari-kutusu"
            >
              <div class="cari-kutu-baslik">
                <span>{{ $t('faturaTasarim.sayinMusteriAlici') }}</span>
              </div>
              <div class="cari-kutu-icerik">
                <div class="cari-sol">
                  <h3 class="cari-unvan">
                    {{ aktifFatura?.cariHesapAd || $t('faturaTasarim.ornekCariAd') }}
                  </h3>
                  <p class="cari-adres">
                    {{ aktifFatura?.cariAdres || $t('faturaTasarim.ornekCariAdres') }}
                  </p>
                </div>
                <div class="cari-sag">
                  <div class="cari-meta-satir">
                    <span><strong>{{ $t('faturaTasarim.vergiDairesiEtiket') }}</strong> {{ aktifFatura?.cariVergiDairesi || 'Kadıköy' }}</span>
                  </div>
                  <div class="cari-meta-satir">
                    <span><strong>{{ $t('faturaTasarim.vergiTcNoEtiket') }}</strong> {{ aktifFatura?.cariVergiNo || '1234567890' }}</span>
                  </div>
                  <div
                    v-if="aktifFatura?.cariTelefon"
                    class="cari-meta-satir"
                  >
                    <span><strong>{{ $t('faturaTasarim.telefonEtiket') }}</strong> {{ aktifFatura.cariTelefon }}</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- 3. KALEM TABLOSU -->
            <table class="fatura-kalem-tablosu">
              <thead>
                <tr>
                  <th
                    v-if="ayarlar.kolonSiraNo"
                    style="width: 35px"
                  >
                    #
                  </th>
                  <th
                    v-if="ayarlar.kolonStokKodu"
                    style="width: 100px"
                  >
                    {{ $t('faturaTasarim.urunKodu') }}
                  </th>
                  <th>{{ $t('faturaTasarim.malHizmetAciklamasi') }}</th>
                  <th style="width: 65px; text-align: center">
                    {{ $t('faturaTasarim.miktar') }}
                  </th>
                  <th style="width: 55px; text-align: center">
                    {{ $t('faturaTasarim.birim') }}
                  </th>
                  <th
                    v-if="ayarlar.fiyatGoster"
                    style="width: 95px; text-align: right"
                  >
                    {{ $t('faturaTasarim.birimFiyat') }}
                  </th>
                  <th
                    v-if="ayarlar.kolonIskonto && ayarlar.fiyatGoster"
                    style="width: 65px; text-align: center"
                  >
                    {{ $t('faturaTasarim.iskontoKisa') }}
                  </th>
                  <th
                    v-if="ayarlar.kolonKdvOrani && ayarlar.fiyatGoster"
                    style="width: 60px; text-align: center"
                  >
                    {{ $t('faturaTasarim.kdvKisa') }}
                  </th>
                  <th
                    v-if="ayarlar.fiyatGoster"
                    style="width: 110px; text-align: right"
                  >
                    {{ $t('faturaTasarim.tutar') }}
                  </th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="(k, idx) in ornekKalemler"
                  :key="idx"
                >
                  <td
                    v-if="ayarlar.kolonSiraNo"
                    class="text-center"
                  >
                    {{ idx + 1 }}
                  </td>
                  <td
                    v-if="ayarlar.kolonStokKodu"
                    class="kod-td"
                  >
                    {{ k.stokKodu || 'STK-' + (100 + idx) }}
                  </td>
                  <td>
                    <span class="kalem-ad">{{ k.aciklama }}</span>
                  </td>
                  <td class="text-center font-bold">
                    {{ k.adet }}
                  </td>
                  <td class="text-center">
                    {{ k.birim || $t('faturaTasarim.adet') }}
                  </td>
                  <td
                    v-if="ayarlar.fiyatGoster"
                    class="text-right"
                  >
                    {{ formatCurrency(k.birimFiyat) }}
                  </td>
                  <td
                    v-if="ayarlar.kolonIskonto && ayarlar.fiyatGoster"
                    class="text-center"
                  >
                    {{ k.iskontoOrani ? `%${k.iskontoOrani}` : '-' }}
                  </td>
                  <td
                    v-if="ayarlar.kolonKdvOrani && ayarlar.fiyatGoster"
                    class="text-center"
                  >
                    %{{ kdvOrani(k) }}
                  </td>
                  <td
                    v-if="ayarlar.fiyatGoster"
                    class="text-right font-bold"
                  >
                    {{ formatCurrency(k.tutar) }}
                  </td>
                </tr>
              </tbody>
            </table>

            <!-- 4. ALT ÖZET & TOPLAMLAR ALANI -->
            <div class="fatura-alt-bolum">
              <!-- Sol Taraf: Banka Bilgisi, Karekod ve Not -->
              <div class="alt-sol-alani">
                <!-- Banka & IBAN Kutusu -->
                <div
                  v-if="ayarlar.bankaGoster"
                  class="banka-bilgi-karti"
                >
                  <div class="banka-kart-baslik">
                    <i class="pi pi-credit-card" /> <span>{{ $t('faturaTasarim.odemeBankaBilgileri') }}</span>
                  </div>
                  <div class="banka-kart-govde">
                    <p><strong>{{ $t('faturaTasarim.bankaEtiket') }}</strong> {{ ayarlar.bankaAdi || 'Ziraat Bankası - Ticari Şube' }}</p>
                    <p><strong>IBAN:</strong> <span class="iban-metin">{{ ayarlar.ibanNo || 'TR12 0001 0090 1234 5678 5001' }}</span></p>
                    <p v-if="ayarlar.hesapSahibi">
                      <strong>{{ $t('faturaTasarim.hesapSahibiEtiket') }}</strong> {{ ayarlar.hesapSahibi }}
                    </p>
                  </div>
                </div>

                <!-- Yazıyla Tutar -->
                <div
                  v-if="ayarlar.fiyatGoster"
                  class="yaziyla-tutar-kutusu"
                >
                  <span><strong>{{ $t('faturaTasarim.yalniz') }}</strong> {{ yaziylaTutar }}</span>
                </div>

                <!-- Fatura Açıklaması -->
                <div
                  v-if="aktifFatura?.aciklama"
                  class="fatura-not-kutusu"
                >
                  <strong>{{ $t('faturaTasarim.aciklamaEtiket') }}</strong> {{ aktifFatura.aciklama }}
                </div>
              </div>

              <!-- Sağ Taraf: Toplamlar & QR Kod -->
              <div
                v-if="ayarlar.fiyatGoster"
                class="alt-sag-alani"
              >
                <div class="toplamlar-tablosu">
                  <div class="toplam-satir">
                    <span>{{ $t('faturaTasarim.malHizmetAraToplam') }}</span>
                    <span>{{ formatCurrency(hesaplananAraToplam) }}</span>
                  </div>
                  <div
                    v-if="hesaplananIskonto > 0"
                    class="toplam-satir iskonto"
                  >
                    <span>{{ $t('faturaTasarim.toplamIskonto') }}</span>
                    <span>-{{ formatCurrency(hesaplananIskonto) }}</span>
                  </div>
                  <div class="toplam-satir">
                    <span>{{ $t('faturaTasarim.hesaplananKdv') }}</span>
                    <span>{{ formatCurrency(hesaplananKdv) }}</span>
                  </div>
                  <div class="toplam-satir genel-toplam">
                    <span>{{ $t('faturaTasarim.odenecekToplam') }}</span>
                    <span>{{ formatCurrency(hesaplananGenelToplam) }}</span>
                  </div>
                </div>

                <!-- QR Kod Alanı -->
                <div
                  v-if="ayarlar.qrKodGoster"
                  class="fatura-qr-alani"
                >
                  <div class="qr-placeholder">
                    <i class="pi pi-qrcode" />
                    <span>{{ $t('faturaTasarim.eBelgeDogrulama') }}</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- 5. İMZA / KAŞE ALANI -->
            <div
              v-if="ayarlar.imzaKutusuGoster"
              class="imza-alani-grid"
            >
              <div class="imza-kutusu">
                <span class="imza-baslik">{{ $t('faturaTasarim.teslimEdenDuzenleyen') }}</span>
                <p class="imza-alt-not">
                  {{ $t('faturaTasarim.kaseImza') }}
                </p>
              </div>
              <div class="imza-kutusu">
                <span class="imza-baslik">{{ $t('faturaTasarim.teslimAlanAlici') }}</span>
                <p class="imza-alt-not">
                  {{ $t('faturaTasarim.isimKaseImza') }}
                </p>
              </div>
            </div>

            <!-- 6. DİPNOT & HUKUKİ ŞARTLAR (FOOTER) -->
            <div
              v-if="ayarlar.dipnotGoster"
              class="fatura-dipnot-alani"
            >
              <p>{{ ayarlar.dipnotMetni || $t('faturaTasarim.dipnotVarsayilan') }}</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </Dialog>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useAuthStore } from '../stores/authStore.js'
import { faturaAPI, sirketAPI } from '../api/index.js'
import { formatCurrency, formatDate } from '../utils/format.js'
import { kdvOrani } from '../utils/faturaHesapla.js'
import { useToast } from 'primevue/usetoast'
import { useI18n } from 'vue-i18n'

const props = defineProps({
  visible: { type: Boolean, default: false },
  faturaId: { type: [Number, String], default: null },
  faturaData: { type: Object, default: null }
})

defineEmits(['update:visible'])
const authStore = useAuthStore()
const toast = useToast()
const { t } = useI18n()

const zoomOrani = ref(0.9)
const sirket = ref(null)
const sirketLogo = ref('')
const aktifFatura = ref(props.faturaData || null)

// Varsayılan Şablon Ayarları
const varsayilanAyarlar = {
  sablon: 'modern', // modern, klasik, minimal, kompakt
  kagitBoyutu: 'a4', // a4, a5, termal80, termal58, letter
  sayfaYonu: 'portrait', // portrait, landscape
  kenarBoslugu: '8mm', // 5mm, 8mm, 12mm
  renk: '#1e40af', // lacivert
  fontFamily: 'sans', // sans, roboto, serif, mono
  faturaBasligi: t('faturaTasarim.satisFaturasi'),
  altBaslik: t('faturaTasarim.resmiFaturaSureti'),
  logoGoster: true,
  logoBoyutu: 'orta',
  logoKonumu: 'sol',
  firmaVergiBilgileri: true,
  firmaAdresBilgileri: true,
  firmaMersisGoster: false,
  cariDetayGoster: true,
  kolonSiraNo: true,
  kolonStokKodu: true,
  kolonIskonto: true,
  kolonKdvOrani: true,
  fiyatGoster: true,
  bankaGoster: true,
  bankaAdi: t('faturaTasarim.ornekBankaAdi'),
  ibanNo: 'TR12 0006 2000 0001 2345 6789 01',
  hesapSahibi: t('faturaTasarim.ornekHesapSahibi'),
  qrKodGoster: true,
  imzaKutusuGoster: true,
  odemeDurumuGoster: true,
  dipnotGoster: true,
  dipnotMetni: t('faturaTasarim.dipnotVarsayilanUzun')
}

const ayarlar = ref({ ...varsayilanAyarlar })

// Kağıt Boyutu ve Düzen Seçenekleri
const kagitBoyutSecenekleri = computed(() => [
  { label: t('faturaTasarim.kagitA4'), value: 'a4' },
  { label: t('faturaTasarim.kagitA5'), value: 'a5' },
  { label: t('faturaTasarim.kagitTermal80'), value: 'termal80' },
  { label: t('faturaTasarim.kagitTermal58'), value: 'termal58' },
  { label: t('faturaTasarim.kagitLetter'), value: 'letter' }
])

const sayfaYonuSecenekleri = computed(() => [
  { label: t('faturaTasarim.dikeyPortrait'), value: 'portrait' },
  { label: t('faturaTasarim.yatayLandscape'), value: 'landscape' }
])

const kenarBoslukSecenekleri = computed(() => [
  { label: t('faturaTasarim.dar5mm'), value: '5mm' },
  { label: t('faturaTasarim.normal8mm'), value: '8mm' },
  { label: t('faturaTasarim.genis12mm'), value: '12mm' }
])

const printPageSize = computed(() => {
  if (ayarlar.value.kagitBoyutu === 'a4') {
    return `A4 ${ayarlar.value.sayfaYonu}`
  }
  if (ayarlar.value.kagitBoyutu === 'a5') {
    return `A5 ${ayarlar.value.sayfaYonu}`
  }
  if (ayarlar.value.kagitBoyutu === 'termal80') {
    return '80mm auto'
  }
  if (ayarlar.value.kagitBoyutu === 'termal58') {
    return '58mm auto'
  }
  if (ayarlar.value.kagitBoyutu === 'letter') {
    return `letter ${ayarlar.value.sayfaYonu}`
  }
  return 'A4 portrait'
})

// Şablon Seçenekleri
const sablonSecenekleri = computed(() => [
  { id: 'modern', ad: t('faturaTasarim.sablonModern'), icon: 'pi pi-star' },
  { id: 'klasik', ad: t('faturaTasarim.sablonKlasik'), icon: 'pi pi-file' },
  { id: 'minimal', ad: t('faturaTasarim.sablonMinimal'), icon: 'pi pi-minus' },
  { id: 'kompakt', ad: t('faturaTasarim.sablonKompakt'), icon: 'pi pi-receipt' }
])

// Renk Seçenekleri
const renkSecenekleri = ['#1e40af', '#059669', '#991b1b', '#1e293b', '#7c3aed', '#b45309']

// Font Seçenekleri
const fontSecenekleri = computed(() => [
  { label: t('faturaTasarim.fontSans'), value: 'sans' },
  { label: t('faturaTasarim.fontRoboto'), value: 'roboto' },
  { label: t('faturaTasarim.fontSerif'), value: 'serif' },
  { label: t('faturaTasarim.fontMono'), value: 'mono' }
])

const logoBoyutSecenekleri = computed(() => [
  { label: t('faturaTasarim.kucuk'), value: 'kucuk' },
  { label: t('faturaTasarim.orta'), value: 'orta' },
  { label: t('faturaTasarim.buyuk'), value: 'buyuk' }
])

const logoKonumSecenekleri = computed(() => [
  { label: t('faturaTasarim.sol'), value: 'sol' },
  { label: t('faturaTasarim.orta'), value: 'orta' },
  { label: t('faturaTasarim.sag'), value: 'sag' }
])

onMounted(async () => {
  sablonuYukle()
  await firmaBilgisiGetir()
  if (props.visible) {
    await faturaYukle()
  }
})

watch(() => props.visible, async (val) => {
  if (val) {
    sablonuYukle()
    await faturaYukle()
  }
})

watch(() => props.faturaData, (val) => {
  if (val) aktifFatura.value = val
})

const firmaBilgisiGetir = async () => {
  try {
    const sirketId = authStore.sirketId
    if (sirketId) {
      const res = await sirketAPI.getById(sirketId)
      sirket.value = res.data
      sirketLogo.value = res.data?.logoUrl || ''
    }
  } catch {
    /* fallback to authStore */
  }
}

const faturaYukle = async () => {
  if (props.faturaData) {
    aktifFatura.value = props.faturaData
    return
  }
  if (props.faturaId) {
    try {
      const res = await faturaAPI.getById(props.faturaId)
      aktifFatura.value = res.data
    } catch {
      toast.add({ severity: 'error', summary: t('faturaTasarim.hata'), detail: t('faturaTasarim.faturaBilgileriYuklenemedi'), life: 3000 })
    }
  }
}

// Şablon Değiştirme
const sablonDegistir = (sablonId) => {
  ayarlar.value.sablon = sablonId
  if (sablonId === 'minimal') {
    ayarlar.value.renk = '#1e293b'
  } else if (sablonId === 'klasik') {
    ayarlar.value.renk = '#1e40af'
    ayarlar.value.imzaKutusuGoster = true
  }
}

// Şablonu Kaydetme & Sıfırlama
const sablonuKaydet = async () => {
  const sirketId = authStore.sirketId
  const key = `raspel_fatura_sablon_${sirketId || 'genel'}`
  const json = JSON.stringify(ayarlar.value)
  // Önce sunucuya yaz (kalıcı, PDF üretiminde kullanılır), başarısızsa yerelde tut.
  if (sirketId) {
    try {
      await sirketAPI.saveFaturaSablonu(sirketId, json)
      localStorage.removeItem(key)
    } catch {
      localStorage.setItem(key, json)
    }
  } else {
    localStorage.setItem(key, json)
  }
  toast.add({
    severity: 'success',
    summary: t('faturaTasarim.kaydedildi'),
    detail: t('faturaTasarim.sablonKaydedildiDetay'),
    life: 3500
  })
}

const sablonuYukle = async () => {
  const sirketId = authStore.sirketId
  const key = `raspel_fatura_sablon_${sirketId || 'genel'}`
  if (sirketId) {
    try {
      const res = await sirketAPI.getFaturaSablonu(sirketId)
      if (res.data?.sablon) {
        ayarlar.value = { ...varsayilanAyarlar, ...JSON.parse(res.data.sablon) }
        return
      }
    } catch {
      /* sunucu yoksa yerel yedeğe düş */
    }
  }
  const kayitli = localStorage.getItem(key)
  if (kayitli) {
    try {
      ayarlar.value = { ...varsayilanAyarlar, ...JSON.parse(kayitli) }
    } catch {
      ayarlar.value = { ...varsayilanAyarlar }
    }
  }
}

const sablonuSifirla = async () => {
  ayarlar.value = { ...varsayilanAyarlar }
  const sirketId = authStore.sirketId
  const key = `raspel_fatura_sablon_${sirketId || 'genel'}`
  localStorage.removeItem(key)
  if (sirketId) {
    try {
      await sirketAPI.saveFaturaSablonu(sirketId, '')
    } catch {
      /* sunucu yoksa yerel temizlik yeterli */
    }
  }
  toast.add({
    severity: 'info',
    summary: t('faturaTasarim.sifirlandi'),
    detail: t('faturaTasarim.sablonSifirlandiDetay'),
    life: 3000
  })
}

// Kalem Listesi (Örnek veya Gerçek)
const ornekKalemler = computed(() => {
  if (aktifFatura.value?.kalemler && aktifFatura.value.kalemler.length > 0) {
    return aktifFatura.value.kalemler
  }
  return [
    { stokKodu: 'STK-001', aciklama: t('faturaTasarim.ornekKalem1'), adet: 1, birim: t('faturaTasarim.birimAdet'), birimFiyat: 15000, iskontoOrani: 0, kdvOrani: 20, tutar: 15000 },
    { stokKodu: 'STK-002', aciklama: t('faturaTasarim.ornekKalem2'), adet: 12, birim: t('faturaTasarim.birimAy'), birimFiyat: 1200, iskontoOrani: 10, kdvOrani: 20, tutar: 12960 },
    { stokKodu: 'STK-003', aciklama: t('faturaTasarim.ornekKalem3'), adet: 1, birim: t('faturaTasarim.birimYil'), birimFiyat: 6500, iskontoOrani: 0, kdvOrani: 20, tutar: 6500 }
  ]
})

const hesaplananAraToplam = computed(() => {
  if (aktifFatura.value?.araToplam != null) return aktifFatura.value.araToplam
  return ornekKalemler.value.reduce((sum, k) => sum + (Number(k.tutar) || 0), 0)
})

const hesaplananIskonto = computed(() => {
  if (aktifFatura.value?.genelIskontoTutari != null) return aktifFatura.value.genelIskontoTutari
  return 0
})

const hesaplananKdv = computed(() => {
  if (aktifFatura.value?.kdv != null) return aktifFatura.value.kdv
  return hesaplananAraToplam.value * 0.20
})

const hesaplananGenelToplam = computed(() => {
  if (aktifFatura.value?.genelToplam != null) return aktifFatura.value.genelToplam
  return (hesaplananAraToplam.value - hesaplananIskonto.value) + hesaplananKdv.value
})

const yaziylaTutar = computed(() => {
  const tutar = Math.round(Number(hesaplananGenelToplam.value) || 0)
  return t('faturaTasarim.yaziylaTutar', { tutar: tutar.toLocaleString('tr-TR') })
})

const odemeDurumLabel = (durum) => {
  const map = {
    ODENDI: t('faturaTasarim.odendi'),
    ODENMEDI: t('faturaTasarim.odenmedi'),
    BEKLIYOR: t('faturaTasarim.bekliyor'),
    KISMI_ODENDI: t('faturaTasarim.kismiOdendi')
  }
  return map[durum] || durum || t('faturaTasarim.odenmedi')
}

// Tarayıcı Yazdırma
const yazdir = async () => {
  const id = aktifFatura.value?.id || props.faturaId
  if (id) {
    try {
      await faturaAPI.yazdirmaKaydet(id, { format: (ayarlar.value.kagitBoyutu || 'a4').toUpperCase() })
    } catch {
      /* yazdırma kaydı başarısız olsa da yazdırma engellenmez */
    }
  }
  window.print()
}
</script>

<style scoped>
.tasarim-container {
  display: flex;
  height: calc(92vh - 90px);
  gap: 20px;
}

/* Sol Sidebar */
.tasarim-sidebar {
  width: 380px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 16px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

/* Dar ekranlarda yan panel alt alta iner; ic yatay scroll olusmaz */
@media (max-width: 1100px) {
  .tasarim-container {
    flex-direction: column;
    height: auto;
  }
  .tasarim-sidebar {
    width: 100%;
  }
}
.sidebar-header {
  padding: 16px 20px;
  border-bottom: 1px solid var(--border);
}
.sidebar-header h3 {
  font-size: 16px;
  font-weight: 700;
  margin: 0 0 4px;
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-primary);
}
.sidebar-header h3 i {
  color: var(--primary-color);
}
.sidebar-sub {
  font-size: 12px;
  color: var(--text-muted);
  margin: 0;
}

.ayar-gruplari {
  flex: 1;
  overflow-y: auto;
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.ayar-kutu {
  background: var(--bg-primary);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 14px;
}
.ayar-baslik {
  font-size: 12px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 12px;
}
.ayar-baslik i {
  color: var(--primary-color);
}
.alt-etiket {
  font-size: 11.5px;
  color: var(--text-muted);
  font-weight: 600;
  display: block;
  margin-bottom: 4px;
}

/* Şablon Kart Butonları */
.sablon-secici {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}
.sablon-btn {
  padding: 10px 8px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 10px;
  color: var(--text-primary);
  font-size: 12px;
  font-weight: 600;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  transition: all 0.2s;
}
.sablon-btn:hover {
  border-color: var(--primary-color);
  background: rgba(59, 130, 246, 0.05);
}
.sablon-btn.aktif {
  border-color: var(--primary-color);
  background: var(--accent-soft);
  color: var(--primary-color);
}
.sablon-btn i {
  font-size: 16px;
}

/* Renk Paleti */
.renk-paleti {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.renk-btn {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: 2px solid transparent;
  cursor: pointer;
  transition: transform 0.15s;
}
.renk-btn:hover {
  transform: scale(1.15);
}
.renk-btn.aktif {
  border-color: var(--text-primary);
  box-shadow: 0 0 0 2px var(--primary-color);
}
.ozel-renk-wrapper {
  position: relative;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  overflow: hidden;
  border: 1px dashed var(--text-muted);
}
.ozel-renk-input {
  position: absolute;
  top: -10px;
  left: -10px;
  width: 50px;
  height: 50px;
  cursor: pointer;
}

/* Toggle Listesi */
.toggle-listesi {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.toggle-satir {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12.5px;
  color: var(--text-primary);
}

.sidebar-footer {
  padding: 14px 20px;
  border-top: 1px solid var(--border);
  background: var(--bg-card);
}

/* Sağ Önizleme Alanı */
.tasarim-onizleme-alani {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: var(--bg-secondary);
  border: 1px solid var(--border);
  border-radius: 16px;
  overflow: hidden;
}
.onizleme-toolbar {
  padding: 10px 18px;
  background: var(--bg-card);
  border-bottom: 1px solid var(--border);
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.zoom-kontroller {
  display: flex;
  align-items: center;
  gap: 6px;
}
.zoom-text {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-muted);
  min-width: 40px;
  text-align: center;
}

.a4-scroll-wrapper {
  flex: 1;
  overflow: auto;
  padding: 24px;
  display: flex;
  justify-content: center;
}

/* Kağıt / Fatura Belgesi Temel Stilleri */
.fatura-a4-kagit {
  background: #ffffff !important;
  color: #1e293b !important;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.25);
  border-radius: 4px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  box-sizing: border-box;
  font-size: 12px;
  line-height: 1.4;
  transition: width 0.2s, min-height 0.2s, padding 0.2s;
}

/* Kağıt Formatları ve Yönleri */
.kagit-a4.yon-portrait {
  width: 210mm;
  min-height: 297mm;
  padding: 16mm 18mm;
}
.kagit-a4.yon-landscape {
  width: 297mm;
  min-height: 210mm;
  padding: 14mm 16mm;
}

.kagit-a5.yon-portrait {
  width: 148mm;
  min-height: 210mm;
  padding: 10mm 12mm;
  font-size: 10.5px;
}
.kagit-a5.yon-portrait .fatura-ust-header h1 { font-size: 16px; }
.kagit-a5.yon-portrait .firma-unvani { font-size: 15px; }
.kagit-a5.yon-portrait .fatura-kalem-tablosu th,
.kagit-a5.yon-portrait .fatura-kalem-tablosu td { padding: 4px 6px; font-size: 9.5px; }

.kagit-a5.yon-landscape {
  width: 210mm;
  min-height: 148mm;
  padding: 8mm 12mm;
  font-size: 10px;
}
.kagit-a5.yon-landscape .fatura-ust-header h1 { font-size: 16px; }
.kagit-a5.yon-landscape .firma-unvani { font-size: 14px; }
.kagit-a5.yon-landscape .fatura-kalem-tablosu th,
.kagit-a5.yon-landscape .fatura-kalem-tablosu td { padding: 3px 5px; font-size: 9px; }

.kagit-letter.yon-portrait {
  width: 216mm;
  min-height: 279mm;
  padding: 16mm 18mm;
}
.kagit-letter.yon-landscape {
  width: 279mm;
  min-height: 216mm;
  padding: 14mm 16mm;
}

/* Termal Fiş / POS Formatları */
.kagit-termal80 {
  width: 80mm;
  min-height: auto;
  padding: 6mm 6mm;
  font-size: 9.5px;
}
.kagit-termal80 .fatura-ust-header {
  flex-direction: column;
  align-items: center;
  text-align: center;
}
.kagit-termal80 .header-sol,
.kagit-termal80 .header-sag {
  text-align: center;
  align-items: center;
  width: 100%;
}
.kagit-termal80 .fatura-alt-bolum {
  flex-direction: column;
}
.kagit-termal80 .imza-alani-grid {
  grid-template-columns: 1fr;
  gap: 16px;
}

.kagit-termal58 {
  width: 58mm;
  min-height: auto;
  padding: 4mm 4mm;
  font-size: 8.5px;
}
.kagit-termal58 .fatura-ust-header {
  flex-direction: column;
  align-items: center;
  text-align: center;
}
.kagit-termal58 .header-sol,
.kagit-termal58 .header-sag {
  text-align: center;
  align-items: center;
  width: 100%;
}
.kagit-termal58 .fatura-alt-bolum {
  flex-direction: column;
}
.kagit-termal58 .imza-alani-grid {
  grid-template-columns: 1fr;
  gap: 12px;
}

/* Font Ailesi Sınıfları */
.font-sans { font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif; }
.font-roboto { font-family: 'Roboto', sans-serif; }
.font-serif { font-family: 'Georgia', serif; }
.font-mono { font-family: 'Courier New', monospace; }

/* 1. ÜST HEADER */
.fatura-ust-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding-bottom: 16px;
  border-bottom: 2px solid var(--vurgu-renk, #1e40af);
  gap: 20px;
}
.header-sol {
  flex: 1.2;
  display: flex;
  gap: 14px;
}
.logo-pos-orta {
  flex-direction: column;
  align-items: center;
  text-align: center;
}
.logo-pos-sag {
  flex-direction: row-reverse;
}

.fatura-logo-kutu {
  flex-shrink: 0;
  display: flex;
  align-items: center;
}
.logo-boyut-kucuk { width: 50px; height: 50px; }
.logo-boyut-orta { width: 75px; height: 75px; }
.logo-boyut-buyuk { width: 100px; height: 100px; }

.fatura-logo-img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.firma-unvani {
  font-size: 18px;
  font-weight: 800;
  color: var(--vurgu-renk, #1e40af);
  margin: 0 0 4px;
}
.firma-alt-bilgi {
  font-size: 10.5px;
  color: #475569;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 2px;
}
.firma-adres-metin {
  margin: 0;
  width: 100%;
}

.header-sag {
  flex: 0.9;
  text-align: right;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}
.fatura-ana-baslik-band h1 {
  font-size: 20px;
  font-weight: 900;
  color: var(--vurgu-renk, #1e40af);
  margin: 0;
  letter-spacing: 0.5px;
}
.fatura-alt-slogan {
  font-size: 10px;
  color: #64748b;
  margin: 2px 0 8px;
}
.belge-meta-tablo {
  font-size: 11px;
  display: flex;
  flex-direction: column;
  gap: 3px;
  margin-top: 6px;
}
.meta-satir {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
.meta-label {
  color: #64748b;
  font-weight: 600;
}
.meta-deger {
  color: #0f172a;
  font-weight: 700;
}
.durum-etiket {
  padding: 1px 6px;
  background: #e0f2fe;
  color: #0369a1;
  border-radius: 4px;
  font-size: 10px;
}

/* 2. CARİ KUTUSU */
.fatura-cari-kutusu {
  margin: 14px 0;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  overflow: hidden;
}
.cari-kutu-baslik {
  background: #f1f5f9;
  padding: 4px 10px;
  font-size: 10px;
  font-weight: 700;
  color: var(--vurgu-renk, #1e40af);
  border-bottom: 1px solid #e2e8f0;
  letter-spacing: 0.5px;
}
.cari-kutu-icerik {
  padding: 8px 12px;
  display: flex;
  justify-content: space-between;
  gap: 16px;
}
.cari-unvan {
  font-size: 13px;
  font-weight: 700;
  color: #0f172a;
  margin: 0 0 2px;
}
.cari-adres {
  font-size: 11px;
  color: #475569;
  margin: 0;
}
.cari-sag {
  font-size: 10.5px;
  color: #334155;
  text-align: right;
}

/* 3. KALEM TABLOSU */
.fatura-kalem-tablosu {
  width: 100%;
  border-collapse: collapse;
  margin: 12px 0;
  font-size: 11px;
}
.fatura-kalem-tablosu th {
  background: var(--vurgu-renk, #1e40af);
  color: #ffffff;
  font-weight: 700;
  padding: 7px 8px;
  text-align: left;
  font-size: 10.5px;
}
.fatura-kalem-tablosu td {
  padding: 6px 8px;
  border-bottom: 1px solid #e2e8f0;
  color: #1e293b;
}
.fatura-kalem-tablosu tr:nth-child(even) td {
  background: #f8fafc;
}
.kod-td {
  font-family: monospace;
  font-size: 10px;
  color: #475569;
}
.kalem-ad {
  font-weight: 600;
}

/* 4. ALT BÖLÜM (ÖZET & BANKA) */
.fatura-alt-bolum {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  margin-top: 10px;
  padding-top: 10px;
}
.alt-sol-alani {
  flex: 1.1;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.banka-bilgi-karti {
  border: 1px dashed #cbd5e1;
  border-radius: 6px;
  padding: 8px 10px;
  font-size: 10.5px;
  background: #fafafa;
}
.banka-kart-baslik {
  font-weight: 700;
  color: var(--vurgu-renk, #1e40af);
  margin-bottom: 4px;
  display: flex;
  align-items: center;
  gap: 5px;
}
.banka-kart-govde p {
  margin: 1px 0;
}
.iban-metin {
  font-family: monospace;
  font-weight: 700;
}

.yaziyla-tutar-kutusu {
  font-size: 10.5px;
  font-style: italic;
  color: #334155;
  padding: 4px 8px;
  background: #f1f5f9;
  border-left: 3px solid var(--vurgu-renk, #1e40af);
}
.fatura-not-kutusu {
  font-size: 10.5px;
  color: #475569;
}

.alt-sag-alani {
  flex: 0.9;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}
.toplamlar-tablosu {
  width: 100%;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  overflow: hidden;
  font-size: 11px;
}
.toplam-satir {
  display: flex;
  justify-content: space-between;
  padding: 5px 10px;
  border-bottom: 1px solid #f1f5f9;
}
.toplam-satir.iskonto {
  color: #dc2626;
}
.toplam-satir.genel-toplam {
  background: var(--vurgu-renk, #1e40af);
  color: #ffffff;
  font-weight: 800;
  font-size: 12px;
  border-bottom: none;
}

.fatura-qr-alani {
  margin-top: 10px;
}
.qr-placeholder {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-size: 10px;
  color: #64748b;
}
.qr-placeholder i {
  font-size: 24px;
  color: #0f172a;
}

/* 5. İMZA ALANI */
.imza-alani-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 40px;
  margin-top: 24px;
}
.imza-kutusu {
  border-top: 1px solid #cbd5e1;
  padding-top: 6px;
  text-align: center;
}
.imza-baslik {
  font-size: 11px;
  font-weight: 700;
  color: #334155;
  display: block;
}
.imza-alt-not {
  font-size: 9.5px;
  color: #94a3b8;
  margin-top: 25px;
}

/* 6. DİPNOT */
.fatura-dipnot-alani {
  margin-top: 16px;
  padding-top: 8px;
  border-top: 1px solid #e2e8f0;
  font-size: 9.5px;
  color: #64748b;
  text-align: center;
}

/* TEMALAR */
/* Klasik Resmi Tema */
.tema-klasik .fatura-ust-header {
  border-bottom: 3px double var(--vurgu-renk, #1e40af);
}
.tema-klasik .fatura-kalem-tablosu th {
  border: 1px solid #cbd5e1;
}
.tema-klasik .fatura-kalem-tablosu td {
  border: 1px solid #e2e8f0;
}

/* Minimalist Tema */
.tema-minimal .fatura-ust-header {
  border-bottom: 1px solid #0f172a;
}
.tema-minimal .fatura-kalem-tablosu th {
  background: transparent;
  color: #0f172a;
  border-bottom: 2px solid #0f172a;
  border-top: 1px solid #0f172a;
}
.tema-minimal .toplam-satir.genel-toplam {
  background: #0f172a;
}

/* Kompakt Fiş Tema */
.tema-kompakt {
  padding: 10mm 12mm;
  font-size: 10.5px;
}
.tema-kompakt .fatura-kalem-tablosu th,
.tema-kompakt .fatura-kalem-tablosu td {
  padding: 4px 6px;
}

/* YAZDIRMA (@media print) STİLLERİ */
@media print {
  body * {
    visibility: hidden;
  }
  .no-print,
  .p-dialog-header,
  .p-dialog-mask,
  .tasarim-sidebar,
  .onizleme-toolbar {
    display: none !important;
  }
  .fatura-tasarim-dialog {
    position: static !important;
    width: 100% !important;
    height: auto !important;
    max-width: 100% !important;
    margin: 0 !important;
    padding: 0 !important;
    box-shadow: none !important;
    border: none !important;
  }
  .tasarim-container {
    height: auto !important;
    display: block !important;
  }
  .tasarim-onizleme-alani {
    background: transparent !important;
    border: none !important;
  }
  .a4-scroll-wrapper {
    padding: 0 !important;
    overflow: visible !important;
  }
  #fatura-yazdirma-alani,
  #fatura-yazdirma-alani * {
    visibility: visible !important;
  }
  #fatura-yazdirma-alani {
    position: absolute !important;
    left: 0 !important;
    top: 0 !important;
    width: 100% !important;
    min-height: 100% !important;
    margin: 0 !important;
    box-shadow: none !important;
    transform: none !important;
    padding: 10mm 14mm !important;
    -webkit-print-color-adjust: exact !important;
    print-color-adjust: exact !important;
  }
  @page {
    size: A4 portrait;
    margin: 8mm;
  }
}
</style>
