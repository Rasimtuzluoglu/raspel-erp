<template>
  <div
    class="dashboard-container"
    :class="{ 'icerik-hazir': !loading }"
  >
    <Message
      v-if="dashboardStore.veriEksik"
      severity="warn"
      :closable="false"
      class="veri-eksik-uyari"
      :text="t('dashboard.veriEksikUyari')"
    />
    <div class="dashboard-header">
      <div class="dashboard-baslik-blok">
        <h1>RasPel ERP</h1>
        <p class="karsilama-mesaji">
          {{ karsilamaMetni }},
          <strong>{{ authStore?.kullanici?.displayName || authStore?.kullanici?.username || '' }}</strong>
          <span
            v-if="authStore?.sirketAdi"
            class="karsilama-sirket"
          >
            <i class="pi pi-building" /> {{ authStore?.sirketAdi }}
          </span>
        </p>
        <div class="dashboard-canli">
          <span class="canli-rozet">
            <span class="canli-nokta" />{{ t('dashboard.canli') }}
          </span>
          <span
            v-if="sonGuncellemeZamani"
            class="canli-zaman"
          >
            {{ t('dashboard.sonGuncelleme', { zaman: sonGuncellemeZamani }) }}
          </span>
        </div>
      </div>
      <div class="header-sag">
        <DovizTickerCompact />
        <div class="dashboard-datetime">
          <SaatGostergesi />
        </div>
        <Button
          icon="pi pi-refresh"
          class="p-button-rounded p-button-text"
          :loading="loading"
          :title="t('dashboard.yenile')"
          @click="refresh"
        />
        <Button
          icon="pi pi-cog"
          class="p-button-rounded p-button-text"
          :title="t('dashboard.widgetAyarlari')"
          @click="widgetAyarlariGoster = true"
        />
      </div>
    </div>

    <Card
      v-if="widgetAyarlariGoster"
      class="widget-ayarlari"
    >
      <template #title>
        <i
          class="pi pi-sliders-h"
          style="margin-right: 8px"
        />{{ t('dashboard.gosterilecekWidgetlar') }}
      </template>
      <template #content>
        <div class="widget-togglar">
          <label
            v-for="w in widgetListesi"
            :key="w.key"
            class="widget-toggle"
          >
            <InputSwitch v-model="w.gorunur" />
            <span>{{ w.etiket }}</span>
          </label>
        </div>
        <div style="margin-top: 16px; display: flex; gap: 8px; justify-content: flex-end">
          <Button
            :label="t('dashboard.uygula')"
            icon="pi pi-check"
            class="p-button-sm"
            @click="kaydetWidget"
          />
          <Button
            :label="t('common.cancel')"
            icon="pi pi-times"
            class="p-button-sm p-button-text"
            @click="iptalWidget"
          />
        </div>
      </template>
    </Card>

    <div
      v-if="loading"
      class="skeleton-grid"
    >
      <div
        v-for="i in 7"
        :key="i"
        class="skeleton-card"
      >
        <Skeleton
          width="100%"
          height="90px"
        />
      </div>
      <div style="grid-column: 1/-1">
        <Skeleton
          width="100%"
          height="200px"
        />
      </div>
      <div style="grid-column: 1/-1">
        <Skeleton
          width="100%"
          height="120px"
        />
      </div>
      <div class="skeleton-bottom">
        <Skeleton
          width="100%"
          height="220px"
        /><Skeleton
          width="100%"
          height="220px"
        />
      </div>
    </div>

    <Onboarding
      v-if="!loading && bosSistem && onboardingGoster"
      @atla="onboardingAtla"
    />

    <template v-if="!loading && (!bosSistem || !onboardingGoster)">
      <!-- 0. GÜNLÜK ÖZET -->
      <Card
        v-if="dashboardStore?.ozet"
        class="ozet-kart"
      >
        <template #title>
          <i
            class="pi pi-sparkles ozet-ikon"
          />{{ t('dashboard.gununOzeti') }}
        </template>
        <template #content>
          <p class="ozet-metin">
            {{ dashboardStore.ozet }}
          </p>
        </template>
      </Card>

      <!-- 1. TEMEL 4 KPI KARTI -->
      <h2
        v-if="widgets.istatistikler.gorunur"
        class="section-title"
      >
        <i class="pi pi-chart-pie" /> {{ t('dashboard.finansalOzet') }}
      </h2>
      <div
        v-if="widgets.istatistikler.gorunur"
        class="stats-grid"
      >
        <KpiKart
          :baslik="t('dashboard.toplamCari')"
          :deger="dashboardStore?.toplamCariSayisi || 0"
          :para-birimi="false"
          icon="pi pi-users"
          renk="#3b82f6"
        >
          <template #alt>
            {{ t('dashboard.bakiye') }} <strong>{{ formatCurrency(dashboardStore?.toplamBakiye || 0) }}</strong>
          </template>
        </KpiKart>
        <KpiKart
          :baslik="t('dashboard.toplamLikiditeKart')"
          :deger="toplamLikidite"
          icon="pi pi-wallet"
          renk="#10b981"
          :trend="nakitTrend"
          :sparkline="sparkNet"
        >
          <template #alt>
            {{ t('dashboard.kasaLabel') }} {{ formatCurrency(toplamKasaBakiye) }} · {{ t('dashboard.bankaLabel') }} {{ formatCurrency(toplamBankaBakiye) }}
          </template>
        </KpiKart>
        <KpiKart
          :baslik="t('dashboard.faturaDurumu')"
          :deger="toplamFatura"
          :para-birimi="false"
          icon="pi pi-file"
          renk="#f59e0b"
          :trend="tahsilatTrend"
          :sparkline="sparkGelir"
        >
          <template #alt>
            {{ t('dashboard.kesilen') }} <strong>{{ kesilenFatura }}</strong> · {{ t('dashboard.bugunkuTahsilat') }} <strong>{{ formatCurrency(dashboardStore?.bugunkuTahsilat || 0) }}</strong>
          </template>
        </KpiKart>
        <KpiKart
          :baslik="t('dashboard.stokCesidi')"
          :deger="toplamStok"
          :para-birimi="false"
          icon="pi pi-box"
          renk="#8b5cf6"
        >
          <template #alt>
            {{ t('dashboard.stokDegeri') }} <strong>{{ formatCurrency(dashboardStore?.toplamStokDegeri || 0) }}</strong>
            <span
              v-if="dusukStokAdet > 0"
              class="critical-hint"
            >
              <i class="pi pi-exclamation-triangle" /> {{ t('dashboard.kritikStokSayisi', { n: dusukStokAdet }) }}
            </span>
            <span
              v-else
              class="text-emerald-600"
            >{{ t('dashboard.stokSeviyeleriYeterli') }}</span>
          </template>
        </KpiKart>
      </div>

      <!-- 1a. BUGÜNÜN ÖZETİ + HEDEF İLERLEMESİ -->
      <template v-if="widgets.bugunOzet.gorunur">
        <h2 class="section-title">
          <i class="pi pi-sun" /> {{ t('dashboard.bugununOzetiHedefler') }}
        </h2>
        <div class="bugun-ozet-grid">
          <div class="bugun-kart tahsilat">
            <i class="pi pi-arrow-down-left" />
            <div>
              <span>{{ t('dashboard.bugunkuTahsilatKart') }}</span>
              <strong>{{ formatCurrency(dashboardStore?.bugunkuTahsilat || 0) }}</strong>
            </div>
          </div>
          <div class="bugun-kart odeme">
            <i class="pi pi-arrow-up-right" />
            <div>
              <span>{{ t('dashboard.bugunkuOdemeKart') }}</span>
              <strong>{{ formatCurrency(dashboardStore?.bugunkuOdeme || 0) }}</strong>
            </div>
          </div>
          <div class="bugun-kart siparis">
            <i class="pi pi-shopping-cart" />
            <div>
              <span>{{ t('dashboard.bugunkuSiparis') }}</span>
              <strong>{{ dashboardStore?.bugunkuSiparis || 0 }}</strong>
            </div>
          </div>
          <div class="bugun-kart teslimat">
            <i class="pi pi-truck" />
            <div>
              <span>{{ t('dashboard.bekleyenTeslimat') }}</span>
              <strong>{{ dashboardStore?.bekleyenTeslimat || 0 }}</strong>
            </div>
          </div>
        </div>

        <div class="hedef-grid">
          <div class="hedef-kart">
            <div class="hedef-baslik">
              <span><i class="pi pi-bullseye" /> {{ t('dashboard.aylikCiroHedefi') }}</span>
              <strong>{{ formatCurrency(dashboardStore?.gerceklesenCiro || 0) }} / {{ formatCurrency(dashboardStore?.hedefCiro || 0) }}</strong>
            </div>
            <div class="hedef-track">
              <div
                class="hedef-fill ciro"
                :style="{ width: Math.min(100, dashboardStore?.ciroIlerlemeYuzdesi || 0) + '%' }"
              />
            </div>
            <span class="hedef-yuzde">%{{ (dashboardStore?.ciroIlerlemeYuzdesi || 0).toFixed(1) }}</span>
          </div>
          <div class="hedef-kart">
            <div class="hedef-baslik">
              <span><i class="pi pi-chart-line" /> {{ t('dashboard.netKarBuAy') }}</span>
              <strong :class="(dashboardStore?.gerceklesenKar || 0) >= 0 ? 'positive' : 'negative'">{{ formatCurrency(dashboardStore?.gerceklesenKar || 0) }}</strong>
            </div>
            <div class="alacak-borc-satir">
              <span class="alacak">{{ t('dashboard.alacakLabel') }} {{ formatCurrency(dashboardStore?.pozitifBakiye || 0) }}</span>
              <span class="borc">{{ t('dashboard.borcLabel') }} {{ formatCurrency(Math.abs(dashboardStore?.negatifBakiye || 0)) }}</span>
            </div>
          </div>
          <div class="hedef-kart">
            <div class="hedef-baslik">
              <span><i class="pi pi-trophy" /> {{ t('dashboard.karHedefi') }}</span>
              <strong>{{ formatCurrency(dashboardStore?.gerceklesenKar || 0) }} / {{ formatCurrency(dashboardStore?.hedefKar || 0) }}</strong>
            </div>
            <div class="hedef-track">
              <div
                class="hedef-fill kar"
                :style="{ width: karIlerleme + '%' }"
              />
            </div>
            <span class="hedef-yuzde">%{{ karIlerleme.toFixed(1) }}</span>
          </div>
        </div>
      </template>

      <!-- 1b. OPERASYONEL METRİKLER -->
      <DashboardMiniIstatistikler
        v-if="widgets.istatistikler.gorunur"
      />

      <!-- 1c. CARİ ÖZET & TAHSİLAT TAKİBİ -->
      <template v-if="widgets.cariOzet.gorunur">
        <h2 class="section-title">
          <i class="pi pi-users" /> {{ t('dashboard.cariOzetTahsilat') }}
        </h2>
        <div class="cari-ozet-grid">
          <div class="cari-ozet-kart alacak">
            <i class="pi pi-arrow-down-left" />
            <div>
              <span>{{ t('dashboard.toplamAlacak') }}</span>
              <strong>{{ formatCurrency(dashboardStore?.pozitifBakiye || 0) }}</strong>
            </div>
          </div>
          <div class="cari-ozet-kart borc">
            <i class="pi pi-arrow-up-right" />
            <div>
              <span>{{ t('dashboard.toplamBorc') }}</span>
              <strong>{{ formatCurrency(Math.abs(dashboardStore?.negatifBakiye || 0)) }}</strong>
            </div>
          </div>
          <div class="cari-ozet-kart enborc">
            <i class="pi pi-user-minus" />
            <div>
              <span>{{ t('dashboard.enBorcluCari') }}</span>
              <strong>{{ (dashboardStore?.enCokBorcCariler || [])[0]?.cariAd || '—' }}</strong>
              <small v-if="(dashboardStore?.enCokBorcCariler || [])[0]">{{ formatCurrency(dashboardStore.enCokBorcCariler[0].tutar) }}</small>
            </div>
          </div>
        </div>
      </template>

      <!-- 2. HIZLI İŞLEMLER ÇUBUĞU -->
      <h2
        v-if="widgets.istatistikler.gorunur"
        class="section-title"
      >
        <i class="pi pi-bolt" /> {{ t('dashboard.hizliIslemler') }}
      </h2>
      <div
        v-if="widgets.istatistikler.gorunur"
        class="quick-actions"
      >
        <router-link
          v-if="authStore.isAdmin"
          to="/yonetici-kokpiti"
          class="action-card kokpit"
        >
          <i class="pi pi-bolt" /><span>{{ t('dashboard.yoneticiKokpiti') }}</span>
        </router-link>
        <router-link
          to="/teklifler"
          class="action-card teklif"
        >
          <i class="pi pi-file-edit" /><span>{{ t('dashboard.satisTeklifleri') }}</span>
        </router-link>
        <router-link
          to="/saha-portali"
          class="action-card saha"
        >
          <i class="pi pi-compass" /><span>{{ t('dashboard.sahaPortali') }}</span>
        </router-link>
        <router-link
          to="/faturalar"
          class="action-card fatura"
        >
          <i class="pi pi-file" /><span>{{ t('dashboard.yeniFatura') }}</span>
        </router-link>
        <router-link
          to="/hizli-satis"
          class="action-card satis"
        >
          <i class="pi pi-shopping-bag" /><span>{{ t('dashboard.hizliSatis') }}</span>
        </router-link>
        <router-link
          to="/cari-hesaplar"
          class="action-card cari"
        >
          <i class="pi pi-user-plus" /><span>{{ t('dashboard.yeniCari') }}</span>
        </router-link>
        <router-link
          to="/hareketler"
          class="action-card tahsilat"
        >
          <i class="pi pi-money-bill" /><span>{{ t('dashboard.tahsilatOdeme') }}</span>
        </router-link>
        <router-link
          to="/kasa"
          class="action-card kasa"
        >
          <i class="pi pi-wallet" /><span>{{ t('dashboard.kasa') }}</span>
        </router-link>
        <router-link
          to="/stoklar"
          class="action-card stok"
        >
          <i class="pi pi-box" /><span>{{ t('dashboard.stoklar') }}</span>
        </router-link>
      </div>

      <div
        v-if="authStore.isAdmin && yedekUyarisiGoster"
        class="backup-reminder"
      >
        <i class="pi pi-save" />
        <span>{{ t('dashboard.yedekUyari') }}
          <router-link to="/yedekler">{{ t('dashboard.yedekAlin') }}</router-link></span>
        <button
          class="reminder-close"
          @click="yedekUyarisiGoster = false"
        >
          &times;
        </button>
      </div>

      <!-- 3. GRAFİK VE ANALİZ PANELLERİ -->
      <h2
        v-if="widgets.grafikler.gorunur"
        class="section-title"
      >
        <i class="pi pi-chart-line" /> {{ t('dashboard.grafiklerAnaliz') }}
      </h2>
      <div
        v-if="widgets.grafikler.gorunur"
        class="charts-row"
      >
        <Card>
          <template #title>
            <i
              class="pi pi-chart-pie"
              style="margin-right: 8px"
            />{{ t('dashboard.cariBakiyeDagilimi') }}
          </template>
          <template #content>
            <div
              v-if="bakiyeChart.datasets.length"
              class="chart-wrapper gizli-veri"
            >
              <Doughnut
                :data="bakiyeChart"
                :options="pieOptions"
              />
            </div>
            <div class="chart-summary gizli-veri">
              <span class="dot pos" /> {{ t('dashboard.alacakLabel') }} {{ formatCurrency(dashboardStore?.pozitifBakiye || 0) }}
              <span class="dot neg" /> {{ t('dashboard.borcLabel') }} {{ formatCurrency(Math.abs(dashboardStore?.negatifBakiye || 0)) }}
            </div>
          </template>
        </Card>

        <Card>
          <template #title>
            <i
              class="pi pi-chart-bar"
              style="margin-right: 8px"
            />{{ t('dashboard.aylikGelirGiderTrendi') }}
          </template>
          <template #content>
            <div
              v-if="aylikKarsilastirmaChart.datasets.length"
              class="chart-wrapper gizli-veri"
            >
              <Bar
                :data="aylikKarsilastirmaChart"
                :options="aylikKarsilastirmaOptions"
              />
            </div>
            <div
              v-else
              class="chart-empty"
            >
              {{ t('dashboard.gelirGiderVerisiYok') }}
            </div>
          </template>
        </Card>

        <Card>
          <template #title>
            <i
              class="pi pi-trophy"
              style="margin-right: 8px"
            />{{ t('dashboard.enCokSatanUrunler') }}
          </template>
          <template #content>
            <div
              v-if="enCokSatanlarChart.datasets.length"
              class="chart-wrapper full gizli-veri"
            >
              <Bar
                :data="enCokSatanlarChart"
                :options="enCokSatanlarOptions"
              />
            </div>
            <div
              v-else
              class="chart-empty"
            >
              {{ t('dashboard.satisVerisiYok') }}
            </div>
          </template>
        </Card>

        <Card>
          <template #title>
            <i
              class="pi pi-user-minus"
              style="margin-right: 8px"
            />{{ t('dashboard.enCokBorcluCariler') }}
          </template>
          <template #content>
            <div
              v-if="enCokBorcCarilerChart.datasets.length"
              class="chart-wrapper full gizli-veri"
            >
              <Bar
                :data="enCokBorcCarilerChart"
                :options="enCokBorcCarilerOptions"
              />
            </div>
            <div
              v-else
              class="chart-empty"
            >
              {{ t('dashboard.borcluCariYok') }}
            </div>
          </template>
        </Card>

        <Card>
          <template #title>
            <i
              class="pi pi-tags"
              style="margin-right: 8px"
            />{{ t('dashboard.kategoriSatisDagilimi') }}
          </template>
          <template #content>
            <div
              v-if="kategoriSatislariChart.datasets.length"
              class="chart-wrapper gizli-veri"
            >
              <Doughnut
                :data="kategoriSatislariChart"
                :options="pieOptions"
              />
            </div>
            <div
              v-else
              class="chart-empty"
            >
              {{ t('dashboard.kategoriVerisiYok') }}
            </div>
          </template>
        </Card>

        <Card>
          <template #title>
            <i
              class="pi pi-user-plus"
              style="margin-right: 8px"
            />{{ t('dashboard.enCokAlacakliCariler') }}
          </template>
          <template #content>
            <div
              v-if="enCokAlacakCarilerChart.datasets.length"
              class="chart-wrapper full gizli-veri"
            >
              <Bar
                :data="enCokAlacakCarilerChart"
                :options="enCokAlacakCarilerOptions"
              />
            </div>
            <div
              v-else
              class="chart-empty"
            >
              {{ t('dashboard.alacakliCariYok') }}
            </div>
          </template>
        </Card>

        <Card>
          <template #title>
            <i
              class="pi pi-wallet"
              style="margin-right: 8px"
            />{{ t('dashboard.kasaBankaDagilimi') }}
          </template>
          <template #content>
            <div
              v-if="kasaBankaChart.datasets.length"
              class="chart-wrapper gizli-veri"
            >
              <Doughnut
                :data="kasaBankaChart"
                :options="pieOptions"
              />
            </div>
            <div class="chart-summary gizli-veri">
              <span class="dot kasa" /> {{ t('dashboard.kasaLabel') }} {{ formatCurrency(dashboardStore?.toplamKasaBakiye || 0) }}
              <span class="dot banka" /> {{ t('dashboard.bankaLabel') }} {{ formatCurrency(dashboardStore?.toplamBankaBakiye || 0) }}
            </div>
          </template>
        </Card>

        <Card>
          <template #title>
            <i
              class="pi pi-history"
              style="margin-right: 8px"
            />{{ t('dashboard.alacakYaslandirma') }}
          </template>
          <template #content>
            <div
              v-if="alacakYaslandirmaChart.datasets.length"
              class="chart-wrapper full gizli-veri"
            >
              <Bar
                :data="alacakYaslandirmaChart"
                :options="alacakYaslandirmaOptions"
              />
            </div>
            <div
              v-else
              class="chart-empty"
            >
              {{ t('dashboard.alacakVerisiYok') }}
            </div>
          </template>
        </Card>

        <Card>
          <template #title>
            <i
              class="pi pi-chart-line"
              style="margin-right: 8px"
            />{{ t('dashboard.nakitProjeksiyon') }}
          </template>
          <template #content>
            <div class="chart-wrapper full gizli-veri">
              <Line
                :data="nakitProjeksiyonVerisi"
                :options="projeksiyonOptions"
              />
            </div>
          </template>
        </Card>

        <Card>
          <template #title>
            <i
              class="pi pi-flag"
              style="margin-right: 8px"
            />{{ t('dashboard.ciroHedef') }}
          </template>
          <template #content>
            <div
              v-if="ciroHedefVerisi.labels.length"
              class="chart-wrapper full gizli-veri"
            >
              <Bar
                :data="ciroHedefVerisi"
                :options="ciroHedefOptions"
              />
            </div>
            <div
              v-else
              class="chart-empty"
            >
              {{ t('dashboard.gelirGiderVerisiYok') }}
            </div>
          </template>
        </Card>

        <Card>
          <template #title>
            <i
              class="pi pi-chart-bar"
              style="margin-right: 8px"
            />{{ t('dashboard.kategoriPareto') }}
          </template>
          <template #content>
            <div
              v-if="kategoriParetoVerisi.labels.length"
              class="chart-wrapper full gizli-veri"
            >
              <Bar
                :data="kategoriParetoVerisi"
                :options="paretoOptions"
              />
            </div>
            <div
              v-else
              class="chart-empty"
            >
              {{ t('dashboard.kategoriVerisiYok') }}
            </div>
          </template>
        </Card>
      </div>

      <!-- 3b. SON 7 GÜN NAKİT AKIŞI -->
      <div
        v-if="widgets.grafikler.gorunur"
        class="nakit-akisi-kart"
      >
        <Card>
          <template #title>
            <i
              class="pi pi-chart-line"
              style="margin-right: 8px"
            />{{ t('dashboard.son7GunNakitAkisi') }}
          </template>
          <template #content>
            <div
              v-if="nakitAkisiChart.datasets.length"
              class="nakit-akisi-wrapper gizli-veri"
            >
              <Line
                :data="nakitAkisiChart"
                :options="nakitAkisiOptions"
              />
            </div>
            <div
              v-else
              class="chart-empty"
            >
              {{ t('dashboard.nakitAkisiVerisiYok') }}
            </div>
          </template>
        </Card>
      </div>

      <!-- 3c. KRİTİK STOK -->
      <template v-if="widgets.kritikStok.gorunur && (dashboardStore?.kritikStoklar || []).length">
        <h2 class="section-title">
          <i class="pi pi-exclamation-triangle" /> {{ t('dashboard.kritikStokUyarilari') }}
        </h2>
        <div class="kritik-stok-grid">
          <div
            v-for="s in dashboardStore.kritikStoklar"
            :key="s.id"
            class="kritik-stok-kart"
          >
            <div class="ks-kod">
              {{ s.stokKodu || '—' }}
            </div>
            <div class="ks-ad">
              {{ s.ad }}
            </div>
            <div class="ks-miktar gizli-veri">
              <span class="ks-deger">{{ s.miktar || 0 }} {{ s.birim || '' }}</span>
              <span class="ks-min">{{ t('dashboard.minEtiketi') }} {{ s.minMiktar || 0 }}</span>
            </div>
          </div>
        </div>
      </template>

      <!-- 4. ALT BÖLÜM: SON HAREKETLER VE ÖDEME VADELERİ -->
      <h2 class="section-title">
        <i class="pi pi-list" /> {{ t('dashboard.sonIslemlerVade') }}
      </h2>
      <div class="bottom-grid">
        <div
          v-if="widgets.sonHareketler.gorunur"
          class="recent-transactions"
        >
          <h2>{{ t('dashboard.sonFinansalHareketler') }}</h2>
          <DataTable
            :value="dashboardStore?.sonHareketler || []"
            :rows="5"
            striped-rows
            size="small"
          >
            <template #empty>
              <EmptyState />
            </template>
            <Column
              field="cariHesapAd"
              :header="t('dashboard.cariHesap')"
            >
              <template #body="s">
                <strong>{{ s.data.cariHesapAd }}</strong>
              </template>
            </Column>
            <Column
              field="tur"
              :header="t('dashboard.tur')"
              style="width: 100px"
            >
              <template #body="s">
                <span :class="['badge', s.data.tur === 'TAHSILAT' ? 'tahsilat' : 'odeme']">{{
                  s.data.tur
                }}</span>
              </template>
            </Column>
            <Column
              field="tutar"
              :header="t('dashboard.tutar')"
              style="width: 130px; text-align: right"
            >
              <template #body="s">
                <span
                  class="gizli-veri"
                  :class="s.data.tur === 'TAHSILAT' ? 'positive' : 'negative'"
                >
                  {{ formatCurrency(s.data.tutar) }}
                </span>
              </template>
            </Column>
            <Column
              field="hareketTarihi"
              :header="t('dashboard.tarih')"
              style="width: 110px"
            >
              <template #body="s">
                {{ formatDate(s.data.hareketTarihi) }}
              </template>
            </Column>
          </DataTable>
        </div>

        <div
          v-if="widgets.odemeVadeleri.gorunur"
          class="vade-uyarilari"
        >
          <Card class="vade-card vadesi-gecen mb-3">
            <template #title>
              <i
                class="pi pi-exclamation-triangle vade-ikon vade-ikon-gecen"
              />{{ t('dashboard.vadesiGecenFaturalar') }}
            </template>
            <template #content>
              <div
                v-if="!dashboardStore?.vadesiGecenFaturalar?.length"
                class="reminder-empty text-xs text-muted"
              >
                <i class="pi pi-check-circle text-emerald-500 mr-1" /> {{ t('dashboard.vadesiGecenFaturaYok') }}
              </div>
              <div
                v-for="f in (dashboardStore?.vadesiGecenFaturalar || []).slice(0, 4)"
                :key="f.faturaId"
                class="reminder-item"
              >
                <span class="reminder-ad">#{{ f.faturaNumarasi }} <small>{{ f.cariHesapAd }}</small></span>
                <span class="reminder-tutar negative gizli-veri">{{ formatCurrency(f.kalanTutar) }}</span>
              </div>
              <router-link
                v-if="(dashboardStore?.vadesiGecenFaturalar || []).length"
                to="/tahsilat"
                class="tumu-gor"
              >
                {{ t('dashboard.tumunuGor') }} <i class="pi pi-arrow-right" />
              </router-link>
            </template>
          </Card>

          <Card class="vade-card vadesi-yaklasan">
            <template #title>
              <i
                class="pi pi-clock vade-ikon vade-ikon-yaklasan"
              />{{ t('dashboard.vadesiYaklasan') }}
            </template>
            <template #content>
              <div
                v-if="!dashboardStore?.vadesiYaklasanFaturalar?.length"
                class="reminder-empty text-xs text-muted"
              >
                <i class="pi pi-check-circle text-emerald-500 mr-1" /> {{ t('dashboard.yaklasanVadeYok') }}
              </div>
              <div
                v-for="f in (dashboardStore?.vadesiYaklasanFaturalar || []).slice(0, 4)"
                :key="f.faturaId"
                class="reminder-item"
              >
                <span class="reminder-ad">#{{ f.faturaNumarasi }} <small>{{ f.cariHesapAd }}</small></span>
                <div class="reminder-aksiyon">
                  <span class="reminder-tutar gizli-veri">{{ formatCurrency(f.kalanTutar) }}</span>
                  <a
                    v-if="f.cariTelefon"
                    class="whatsapp-buton"
                    :href="whatsappLink(f)"
                    target="_blank"
                    rel="noopener"
                    :title="t('dashboard.whatsappIleHatirlat')"
                  >
                    <i class="pi pi-whatsapp" />
                  </a>
                </div>
              </div>
            </template>
          </Card>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import Skeleton from 'primevue/skeleton'
import { useDashboardStore } from '../stores/dashboardStore.js'
import { useDovizStore } from '../stores/dovizStore.js'
import { useAuthStore } from '../stores/authStore.js'
import { Doughnut, Bar, Line } from 'vue-chartjs'
import Onboarding from '../components/Onboarding.vue'
import SaatGostergesi from '../components/SaatGostergesi.vue'
import KpiKart from '../components/KpiKart.vue'
import DashboardMiniIstatistikler from '../components/DashboardMiniIstatistikler.vue'
import DovizTickerCompact from '../components/DovizTickerCompact.vue'
import {
  Chart as ChartJS,
  ArcElement,
  Tooltip,
  Legend,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  BarElement,
  Filler
} from 'chart.js'
import { formatCurrency } from '../utils/format.js'
import { useChartTema } from '../composables/useChartTema.js'

ChartJS.register(ArcElement, Tooltip, Legend, CategoryScale, LinearScale, PointElement, LineElement, BarElement, Filler)

const router = useRouter()
const { t } = useI18n()
const { palet, lejant } = useChartTema()
const paraTick = (v) => formatCurrency(v)

// Bar/line veri setleri icin dikey/yatay degrade yardimcilari
const dikeyGradyan = (ctx, ust, alt) => {
  const { chart } = ctx
  const { ctx: c, chartArea } = chart
  if (!chartArea) return ust
  const g = c.createLinearGradient(0, chartArea.bottom, 0, chartArea.top)
  g.addColorStop(0, alt)
  g.addColorStop(1, ust)
  return g
}
const yatayGradyan = (ctx, sol, sag) => {
  const { chart } = ctx
  const { ctx: c, chartArea } = chart
  if (!chartArea) return sol
  const g = c.createLinearGradient(chartArea.left, 0, chartArea.right, 0)
  g.addColorStop(0, sol)
  g.addColorStop(1, sag)
  return g
}
const dovizStore = useDovizStore()
const karsilamaMetni = computed(() => {
  const saat = new Date().getHours()
  if (saat < 6) return t('dashboard.iyiGeceler')
  if (saat < 12) return t('dashboard.gunaydin')
  if (saat < 18) return t('dashboard.iyiGunler')
  return t('dashboard.iyiAksamlar')
})

const widgetVarsayilan = () => ({
  bugunOzet: { gorunur: true, etiket: t('dashboard.widgetBugunOzet') },
  istatistikler: { gorunur: true, etiket: t('dashboard.widgetIstatistikler') },
  cariOzet: { gorunur: true, etiket: t('dashboard.widgetCariOzet') },
  kritikStok: { gorunur: true, etiket: t('dashboard.widgetKritikStok') },
  grafikler: { gorunur: true, etiket: t('dashboard.widgetGrafikler') },
  sonHareketler: { gorunur: true, etiket: t('dashboard.widgetSonHareketler') },
  odemeVadeleri: { gorunur: true, etiket: t('dashboard.widgetOdemeVadeleri') }
})

const widgetListesi = ref(Object.entries(widgetVarsayilan()).map(([k, v]) => ({ key: k, ...v })))
const widgetAyarlariGoster = ref(false)
const widgets = reactive(widgetVarsayilan())

const kaydetWidget = () => {
  widgetListesi.value.forEach((w) => {
    widgets[w.key].gorunur = w.gorunur
  })
  widgetAyarlariGoster.value = false
  localStorage.setItem(
    'raspel_erp_widgets',
    JSON.stringify(Object.fromEntries(Object.entries(widgets).map(([k, v]) => [k, v.gorunur])))
  )
}
const iptalWidget = () => {
  widgetAyarlariGoster.value = false
  widgetListesi.value = Object.entries(widgets).map(([k, v]) => ({ key: k, ...v }))
}

const dashboardStore = useDashboardStore()
const authStore = useAuthStore()
const loading = ref(true)

  const refresh = async () => {
    loading.value = true
    try {
      // Kritik yol: yalnizca dashboard verisi beklenir. Doviz kurlari ikincil bilgidir;
      // bloklamaz, arka planda yuklenir (giriste algilanan takilmayi azaltir).
      dovizStore?.kurlariYukle?.().catch(() => {})
      await dashboardStore.getDashboardData()
      grafikleriHesapla()
      guncellemeZamaniAyarla()
    } catch (error) {
      console.error('Dashboard yenilenirken hata:', error)
    }
    loading.value = false
  }

const bakiyeChart = ref({ labels: [], datasets: [] })
const aylikKarsilastirmaChart = ref({ labels: [], datasets: [] })
const enCokSatanlarChart = ref({ labels: [], datasets: [] })
const nakitAkisiChart = ref({ labels: [], datasets: [] })
const enCokBorcCarilerChart = ref({ labels: [], datasets: [] })
const enCokAlacakCarilerChart = ref({ labels: [], datasets: [] })
const kategoriSatislariChart = ref({ labels: [], datasets: [] })
const kasaBankaChart = ref({ labels: [], datasets: [] })
const alacakYaslandirmaChart = ref({ labels: [], datasets: [] })

const pieOptions = computed(() => ({
  responsive: true,
  maintainAspectRatio: false,
  plugins: { legend: lejant() }
}))
const aylikKarsilastirmaOptions = computed(() => ({
  responsive: true,
  maintainAspectRatio: false,
  plugins: { legend: lejant() },
  scales: {
    x: { ticks: { color: palet.value.metin }, grid: { color: palet.value.izgara } },
    y: { ticks: { color: palet.value.metin, callback: paraTick }, grid: { color: palet.value.izgara } }
  }
}))
const enCokSatanlarOptions = computed(() => ({
  indexAxis: 'y',
  responsive: true,
  maintainAspectRatio: false,
  plugins: { legend: { display: false } },
  scales: {
    x: { ticks: { color: palet.value.metin }, grid: { color: palet.value.izgara } },
    y: { ticks: { color: palet.value.metin }, grid: { display: false } }
  }
}))
const enCokBorcCarilerOptions = computed(() => ({
  indexAxis: 'y',
  responsive: true,
  maintainAspectRatio: false,
  plugins: { legend: { display: false } },
  scales: {
    x: { ticks: { color: palet.value.metin, callback: paraTick }, grid: { color: palet.value.izgara } },
    y: { ticks: { color: palet.value.metin }, grid: { display: false } }
  }
}))
const enCokAlacakCarilerOptions = computed(() => ({
  indexAxis: 'y',
  responsive: true,
  maintainAspectRatio: false,
  plugins: { legend: { display: false } },
  scales: {
    x: { ticks: { color: palet.value.metin, callback: paraTick }, grid: { color: palet.value.izgara } },
    y: { ticks: { color: palet.value.metin }, grid: { display: false } }
  }
}))
const alacakYaslandirmaOptions = computed(() => ({
  responsive: true,
  maintainAspectRatio: false,
  plugins: { legend: { display: false } },
  scales: {
    x: { ticks: { color: palet.value.metin }, grid: { display: false } },
    y: { ticks: { color: palet.value.metin, callback: paraTick }, grid: { color: palet.value.izgara } }
  }
}))
const nakitAkisiOptions = computed(() => ({
  responsive: true,
  maintainAspectRatio: false,
  plugins: { legend: lejant() },
  scales: {
    x: { ticks: { color: palet.value.metin }, grid: { display: false } },
    y: { ticks: { color: palet.value.metin, callback: paraTick }, grid: { color: palet.value.izgara } }
  }
}))

// --- Faz A: premium trend/sparkline ve yeni grafikler (mevcut veriyle) ---
const sonGuncellemeZamani = ref('')
const guncellemeZamaniAyarla = () => {
  sonGuncellemeZamani.value = new Date().toLocaleTimeString('tr-TR', { hour: '2-digit', minute: '2-digit' })
}

const _gunlukNakit = computed(() => dashboardStore.gunlukNakitAkisi || [])
const sparkGelir = computed(() => _gunlukNakit.value.map((g) => Number(g.gelir) || 0))
const sparkNet = computed(() => {
  let toplam = 0
  return _gunlukNakit.value.map((g) => {
    toplam += (Number(g.gelir) || 0) - (Number(g.gider) || 0)
    return toplam
  })
})

const _trend = (arr) => {
  if (!arr || arr.length < 2) return null
  const onceki = arr[arr.length - 2]
  const son = arr[arr.length - 1]
  if (!onceki) return null
  return ((son - onceki) / Math.abs(onceki)) * 100
}
const tahsilatTrend = computed(() => _trend(sparkGelir.value))
const nakitTrend = computed(() => _trend(sparkNet.value))

const nakitProjeksiyonVerisi = computed(() => {
  const likidite = toplamLikidite.value || 0
  const alacak = dashboardStore.pozitifBakiye || 0
  const borc = Math.abs(dashboardStore.negatifBakiye || 0)
  const gun30 = likidite + alacak * 0.45 - borc * 0.4
  const gun60 = gun30 + alacak * 0.35 - borc * 0.35
  const gun90 = gun60 + alacak * 0.2 - borc * 0.25
  return {
    labels: [
      t('dashboard.mevcutKasaLabel'),
      t('dashboard.gun30Tahmin'),
      t('dashboard.gun60Tahmin'),
      t('dashboard.gun90Tahmin')
    ],
    datasets: [
      {
        label: t('dashboard.nakitProjeksiyon'),
        data: [likidite, Math.round(gun30), Math.round(gun60), Math.round(gun90)],
        borderColor: '#10b981',
        backgroundColor: (ctx) => dikeyGradyan(ctx, 'rgba(16, 185, 129, 0.35)', 'rgba(16, 185, 129, 0)'),
        fill: true,
        tension: 0.35,
        borderWidth: 2.5,
        pointRadius: 4,
        pointHoverRadius: 6,
        pointBackgroundColor: '#10b981',
        pointBorderColor: 'transparent'
      }
    ]
  }
})

const projeksiyonOptions = computed(() => ({
  responsive: true,
  maintainAspectRatio: false,
  plugins: { legend: { display: false } },
  scales: {
    x: { ticks: { color: palet.value.metin }, grid: { display: false } },
    y: { ticks: { color: palet.value.metin, callback: paraTick }, grid: { color: palet.value.izgara } }
  }
}))

const ciroHedefVerisi = computed(() => {
  const veri = dashboardStore.aylikGelirGider || []
  const hedef = dashboardStore.hedefCiro || 0
  return {
    labels: veri.map((v) => v.ay),
    datasets: [
      {
        label: t('dashboard.etiketGelir'),
        data: veri.map((v) => v.gelir || 0),
        backgroundColor: (ctx) => dikeyGradyan(ctx, '#60a5fa', 'rgba(59, 130, 246, 0.25)'),
        hoverBackgroundColor: '#93c5fd',
        borderRadius: 8,
        maxBarThickness: 34,
        order: 2
      },
      {
        type: 'line',
        label: t('dashboard.hedefCizgi'),
        data: veri.map(() => hedef),
        borderColor: '#f59e0b',
        borderDash: [6, 4],
        borderWidth: 2,
        pointRadius: 0,
        fill: false,
        order: 1
      }
    ]
  }
})

const ciroHedefOptions = computed(() => ({
  responsive: true,
  maintainAspectRatio: false,
  plugins: { legend: lejant() },
  scales: {
    x: { ticks: { color: palet.value.metin }, grid: { display: false } },
    y: { ticks: { color: palet.value.metin, callback: paraTick }, grid: { color: palet.value.izgara } }
  }
}))

const kategoriParetoVerisi = computed(() => {
  const liste = [...(dashboardStore.kategoriSatislari || [])]
    .map((k) => ({ kategori: k.kategori, tutar: Number(k.tutar) || 0 }))
    .sort((a, b) => b.tutar - a.tutar)
  const toplam = liste.reduce((s, k) => s + k.tutar, 0)
  let kumulatif = 0
  const yuzdeler = liste.map((k) => {
    kumulatif += k.tutar
    return toplam > 0 ? Math.round((kumulatif / toplam) * 1000) / 10 : 0
  })
  return {
    labels: liste.map((k) => k.kategori),
    datasets: [
      {
        label: t('dashboard.toplam'),
        data: liste.map((k) => k.tutar),
        backgroundColor: (ctx) => dikeyGradyan(ctx, '#a78bfa', 'rgba(139, 92, 246, 0.25)'),
        hoverBackgroundColor: '#c4b5fd',
        borderRadius: 8,
        maxBarThickness: 34,
        yAxisID: 'y'
      },
      {
        type: 'line',
        label: t('dashboard.kumulatif'),
        data: yuzdeler,
        borderColor: '#f59e0b',
        backgroundColor: '#f59e0b',
        pointRadius: 3,
        tension: 0.3,
        yAxisID: 'y1'
      }
    ]
  }
})

const paretoOptions = computed(() => ({
  responsive: true,
  maintainAspectRatio: false,
  plugins: { legend: lejant() },
  scales: {
    x: { ticks: { color: palet.value.metin }, grid: { display: false } },
    y: { position: 'left', ticks: { color: palet.value.metin, callback: paraTick }, grid: { color: palet.value.izgara } },
    y1: {
      position: 'right',
      min: 0,
      max: 100,
      grid: { drawOnChartArea: false },
      ticks: { color: '#f59e0b', callback: (v) => `${v}%` }
    }
  }
}))

const bosSistem = computed(
  () =>
    (dashboardStore.toplamCariSayisi || 0) === 0 &&
    (dashboardStore.toplamStok || 0) === 0 &&
    (dashboardStore.toplamFatura || 0) === 0
)

const yedekUyarisiGoster = ref(true)

const onboardingGoster = ref(false)

const onboardingAtla = () => {
  localStorage.setItem('raspel_erp_onboarding_atlandi', '1')
  onboardingGoster.value = false
}

const toplamFatura = computed(() => dashboardStore.toplamFatura || 0)
const kesilenFatura = computed(() => dashboardStore.kesilenFatura || 0)
const toplamBankaBakiye = computed(() => dashboardStore.toplamBankaBakiye || 0)
const toplamKasaBakiye = computed(() => dashboardStore.toplamKasaBakiye || 0)
const toplamLikidite = computed(() => (dashboardStore.toplamBankaBakiye || 0) + (dashboardStore.toplamKasaBakiye || 0))
const toplamStok = computed(() => dashboardStore.toplamStok || 0)
const dusukStokAdet = computed(() => dashboardStore.kritikStokSayisi || 0)
const karIlerleme = computed(() => {
  const hedef = dashboardStore.hedefKar || 0
  const gercek = dashboardStore.gerceklesenKar || 0
  if (!hedef) return 0
  return Math.min(100, Math.max(0, (gercek / hedef) * 100))
})

const grafikleriHesapla = () => {
  bakiyeChart.value = {
    labels: [t('dashboard.etiketAlacak'), t('dashboard.etiketBorc')],
    datasets: [
      {
        data: [dashboardStore.pozitifBakiye || 0, Math.abs(dashboardStore.negatifBakiye) || 0],
        backgroundColor: ['#10b981', '#f43f5e'],
        hoverBackgroundColor: ['#34d399', '#fb7185'],
        borderWidth: 0,
        hoverOffset: 8
      }
    ]
  }

  aylikKarsilastirmayiHesapla()
  enCokSatanlariHesapla()
  nakitAkisiniHesapla()
  enCokBorcCarileriHesapla()
  enCokAlacakCarileriHesapla()
  kategoriSatislariniHesapla()
  kasaBankayiHesapla()
  alacakYaslandirmayiHesapla()
}

const enCokSatanlariHesapla = () => {
  const urunler = (dashboardStore.enCokSatanlar || []).slice(0, 7)
  if (!urunler.length) {
    enCokSatanlarChart.value = { labels: [], datasets: [] }
    return
  }
  const renkler = ['#3b82f6', '#10b981', '#f59e0b', '#8b5cf6', '#ec4899', '#06b6d4', '#f97316']
  enCokSatanlarChart.value = {
    labels: urunler.map((u) => u.stokAd),
    datasets: [
      {
        label: t('dashboard.satisMiktari'),
        data: urunler.map((u) => u.satisMiktari),
        backgroundColor: (ctx) => {
          const r = renkler[ctx.dataIndex % renkler.length]
          return yatayGradyan(ctx, r, r + '55')
        },
        borderRadius: 8,
        maxBarThickness: 22
      }
    ]
  }
}

const nakitAkisiniHesapla = () => {
  const gunler = dashboardStore.gunlukNakitAkisi || []
  if (!gunler.length) {
    nakitAkisiChart.value = { labels: [], datasets: [] }
    return
  }
  nakitAkisiChart.value = {
    labels: gunler.map((g) => g.gun.slice(5)),
    datasets: [
      {
        label: t('dashboard.etiketGelir'),
        data: gunler.map((g) => g.gelir),
        borderColor: '#10b981',
        backgroundColor: (ctx) => dikeyGradyan(ctx, 'rgba(16, 185, 129, 0.3)', 'rgba(16, 185, 129, 0)'),
        fill: true,
        tension: 0.35,
        borderWidth: 2.5,
        pointRadius: 3,
        pointHoverRadius: 5
      },
      {
        label: t('dashboard.etiketGider'),
        data: gunler.map((g) => g.gider),
        borderColor: '#f43f5e',
        backgroundColor: (ctx) => dikeyGradyan(ctx, 'rgba(244, 63, 94, 0.28)', 'rgba(244, 63, 94, 0)'),
        fill: true,
        tension: 0.35,
        borderWidth: 2.5,
        pointRadius: 3,
        pointHoverRadius: 5
      }
    ]
  }
}

const enCokBorcCarileriHesapla = () => {
  const cariler = (dashboardStore.enCokBorcCariler || []).slice(0, 7)
  if (!cariler.length) {
    enCokBorcCarilerChart.value = { labels: [], datasets: [] }
    return
  }
  enCokBorcCarilerChart.value = {
    labels: cariler.map((c) => c.cariAd),
    datasets: [
      {
        label: t('dashboard.borcTL'),
        data: cariler.map((c) => c.tutar),
        backgroundColor: (ctx) => yatayGradyan(ctx, '#fb7185', 'rgba(244, 63, 94, 0.3)'),
        borderRadius: 8,
        maxBarThickness: 22
      }
    ]
  }
}

const enCokAlacakCarileriHesapla = () => {
  const cariler = (dashboardStore.enCokAlacakCariler || []).slice(0, 7)
  if (!cariler.length) {
    enCokAlacakCarilerChart.value = { labels: [], datasets: [] }
    return
  }
  enCokAlacakCarilerChart.value = {
    labels: cariler.map((c) => c.cariAd),
    datasets: [
      {
        label: t('dashboard.alacakTL'),
        data: cariler.map((c) => c.tutar),
        backgroundColor: (ctx) => yatayGradyan(ctx, '#34d399', 'rgba(16, 185, 129, 0.3)'),
        borderRadius: 8,
        maxBarThickness: 22
      }
    ]
  }
}

const kasaBankayiHesapla = () => {
  const kasa = dashboardStore.toplamKasaBakiye || 0
  const banka = dashboardStore.toplamBankaBakiye || 0
  if (!kasa && !banka) {
    kasaBankaChart.value = { labels: [], datasets: [] }
    return
  }
  kasaBankaChart.value = {
    labels: [t('dashboard.kasa'), t('dashboard.etiketBanka')],
    datasets: [
      {
        data: [kasa, banka],
        backgroundColor: ['#f59e0b', '#3b82f6'],
        hoverBackgroundColor: ['#fbbf24', '#60a5fa'],
        borderWidth: 0,
        hoverOffset: 8
      }
    ]
  }
}

const kategoriSatislariniHesapla = () => {
  const kategoriler = dashboardStore.kategoriSatislari || []
  if (!kategoriler.length) {
    kategoriSatislariChart.value = { labels: [], datasets: [] }
    return
  }
  const renkler = ['#3b82f6', '#10b981', '#f59e0b', '#8b5cf6', '#ec4899', '#06b6d4', '#f97316', '#84cc16']
  kategoriSatislariChart.value = {
    labels: kategoriler.map((k) => k.kategori),
    datasets: [
      {
        data: kategoriler.map((k) => k.tutar),
        backgroundColor: kategoriler.map((_, i) => renkler[i % renkler.length]),
        borderWidth: 0,
        hoverOffset: 8
      }
    ]
  }
}

const alacakYaslandirmayiHesapla = () => {
  const yaslar = dashboardStore.alacakYaslandirma || []
  if (!yaslar.length) {
    alacakYaslandirmaChart.value = { labels: [], datasets: [] }
    return
  }
  alacakYaslandirmaChart.value = {
    labels: yaslar.map((y) => y.aralik),
    datasets: [
      {
        label: t('dashboard.kalanTutarTL'),
        data: yaslar.map((y) => y.tutar),
        backgroundColor: ['#fb7185', '#fbbf24', '#60a5fa', '#a78bfa'],
        borderRadius: 8,
        maxBarThickness: 46
      }
    ]
  }
}

const aylikKarsilastirmayiHesapla = () => {
  const aylikVeri = dashboardStore.aylikGelirGider || []
  if (!aylikVeri.length) {
    aylikKarsilastirmaChart.value = { labels: [], datasets: [] }
    return
  }

  aylikKarsilastirmaChart.value = {
    labels: aylikVeri.map((v) => v.ay),
    datasets: [
      {
        label: t('dashboard.etiketGelir'),
        data: aylikVeri.map((v) => v.gelir),
        backgroundColor: (ctx) => dikeyGradyan(ctx, '#34d399', 'rgba(16, 185, 129, 0.25)'),
        hoverBackgroundColor: '#6ee7b7',
        borderRadius: 8,
        maxBarThickness: 28
      },
      {
        label: t('dashboard.etiketGider'),
        data: aylikVeri.map((v) => v.gider),
        backgroundColor: (ctx) => dikeyGradyan(ctx, '#fb7185', 'rgba(244, 63, 94, 0.25)'),
        hoverBackgroundColor: '#fda4af',
        borderRadius: 8,
        maxBarThickness: 28
      },
      {
        label: t('dashboard.net'),
        type: 'line',
        data: aylikVeri.map((v) => (v.gelir || 0) - (v.gider || 0)),
        borderColor: '#a78bfa',
        backgroundColor: '#a78bfa',
        borderWidth: 2.5,
        pointRadius: 3,
        pointHoverRadius: 5,
        tension: 0.35
      }
    ]
  }
}

onMounted(async () => {
  if (authStore.isSaha) {
    router.replace('/saha-portali')
    return
  }

  onboardingGoster.value = !localStorage.getItem('raspel_erp_onboarding_atlandi')

  try {
    const kayitli = JSON.parse(localStorage.getItem('raspel_erp_widgets'))
    if (kayitli) {
      Object.keys(widgets).forEach((k) => {
        if (kayitli[k] !== undefined) widgets[k].gorunur = kayitli[k]
      })
      widgetListesi.value = Object.entries(widgets).map(([k, v]) => ({ key: k, ...v }))
    }
  } catch (e) {
    /* yok */
  }

  try {
    await Promise.all([
      dashboardStore.getDashboardData(),
      dovizStore?.kurlariYukle ? dovizStore.kurlariYukle() : Promise.resolve()
    ])
    grafikleriHesapla()
    guncellemeZamaniAyarla()
  } catch (error) {
    console.error('Dashboard yüklenirken hata:', error)
  }
  loading.value = false
})

import { formatTarih as formatDate } from '../utils/format.js'

const whatsappLink = (f) => {
  const tel = (f.cariTelefon || '').replace(/\D/g, '')
  const mesaj = t('dashboard.whatsappMesaj', {
    ad: f.cariHesapAd || '',
    no: f.faturaNumarasi || '',
    tutar: formatCurrency(f.kalanTutar)
  })
  return `https://wa.me/${tel}?text=${encodeURIComponent(mesaj)}`
}
</script>

<style scoped>
@import '../assets/dashboard.css';
</style>
