<template>
  <div class="dashboard-container">
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
        <div class="doviz-ticker-compact">
          <div
            v-for="k in (dovizStore?.kurlar || [])"
            :key="k.kod || k.dovizKodu"
            class="ticker-chip"
          >
            <span class="chip-kod">{{ k.kod || k.dovizKodu }}:</span>
            <span class="chip-fiyat gizli-veri">{{ dovizStore?.formatPara ? dovizStore.formatPara(k.satisFiyati || k.satisKuru, 'TRY') : '' }}</span>
          </div>
          <button
            class="chip-refresh-btn"
            :disabled="dovizStore?.loading || false"
            :title="t('dashboard.kurlariYenile')"
            @click="dovizStore?.kurlariGuncelle"
          >
            <i :class="dovizStore?.loading ? 'pi pi-spin pi-spinner' : 'pi pi-sync'" />
          </button>
        </div>
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
      @demo-loaded="demoYuklendi"
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
      <div
        v-if="widgets.istatistikler.gorunur"
        class="mini-stats-grid"
      >
        <div class="mini-stat">
          <div class="mini-stat-icon siparis">
            <i class="pi pi-shopping-cart" />
          </div>
          <div class="mini-stat-icerik">
            <span class="mini-stat-etiket">{{ t('dashboard.bugunkuSiparis') }}</span>
            <strong class="mini-stat-deger gizli-veri">{{ dashboardStore?.bugunkuSiparis || 0 }}</strong>
          </div>
        </div>
        <div class="mini-stat">
          <div class="mini-stat-icon teslimat">
            <i class="pi pi-truck" />
          </div>
          <div class="mini-stat-icerik">
            <span class="mini-stat-etiket">{{ t('dashboard.bekleyenTeslimat') }}</span>
            <strong class="mini-stat-deger gizli-veri">{{ dashboardStore?.bekleyenTeslimat || 0 }}</strong>
          </div>
        </div>
        <div class="mini-stat">
          <div class="mini-stat-icon iade">
            <i class="pi pi-replay" />
          </div>
          <div class="mini-stat-icerik">
            <span class="mini-stat-etiket">{{ t('dashboard.iadeOrani') }}</span>
            <strong class="mini-stat-deger gizli-veri">{{ dashboardStore?.iadeOrani || 0 }}%</strong>
          </div>
        </div>
        <div class="mini-stat">
          <div class="mini-stat-icon devir">
            <i class="pi pi-sync" />
          </div>
          <div class="mini-stat-icerik">
            <span class="mini-stat-etiket">{{ t('dashboard.stokDevirHizi') }}</span>
            <strong class="mini-stat-deger gizli-veri">{{ dashboardStore?.stokDevirHizi || 0 }}</strong>
          </div>
        </div>
        <div class="mini-stat">
          <div class="mini-stat-icon calisan">
            <i class="pi pi-id-card" />
          </div>
          <div class="mini-stat-icerik">
            <span class="mini-stat-etiket">{{ t('dashboard.aktifCalisan') }}</span>
            <strong class="mini-stat-deger gizli-veri">{{ dashboardStore?.aktifCalisan || 0 }}</strong>
          </div>
        </div>
        <div class="mini-stat">
          <div class="mini-stat-icon izinli">
            <i class="pi pi-calendar" />
          </div>
          <div class="mini-stat-icerik">
            <span class="mini-stat-etiket">{{ t('dashboard.bugunIzinli') }}</span>
            <strong class="mini-stat-deger gizli-veri">{{ dashboardStore?.bugunIzinli || 0 }}</strong>
          </div>
        </div>
        <div class="mini-stat">
          <div class="mini-stat-icon bekleyen-izin">
            <i class="pi pi-hourglass" />
          </div>
          <div class="mini-stat-icerik">
            <span class="mini-stat-etiket">{{ t('dashboard.bekleyenIzin') }}</span>
            <strong class="mini-stat-deger gizli-veri">{{ dashboardStore?.bekleyenIzinSayisi || 0 }}</strong>
          </div>
        </div>
        <div class="mini-stat">
          <div class="mini-stat-icon odeme">
            <i class="pi pi-money-bill" />
          </div>
          <div class="mini-stat-icerik">
            <span class="mini-stat-etiket">{{ t('dashboard.bugunkuOdemeKart') }}</span>
            <strong class="mini-stat-deger gizli-veri">{{ formatCurrency(dashboardStore?.bugunkuOdeme || 0) }}</strong>
          </div>
        </div>
      </div>

      <!-- 1c. CARİ ÖZET & TAHSİLAT TAKİBİ -->
      <template v-if="widgets.cariOzet.gorunur">
        <h2 class="section-title">
          <i class="pi pi-handshake" /> {{ t('dashboard.cariOzetTahsilat') }}
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
    await Promise.all([
      dashboardStore.getDashboardData(),
      dovizStore?.kurlariYukle ? dovizStore.kurlariYukle() : Promise.resolve()
    ])
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

const demoYuklendi = async () => {
  localStorage.setItem('raspel_erp_onboarding_atlandi', '1')
  onboardingGoster.value = false
  try {
    await dashboardStore.getDashboardData()
    grafikleriHesapla()
  } catch (e) {
    console.error('Demo sonrası dashboard yenilenemedi:', e)
  }
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
.dashboard-container {
  padding: 0;
}
.dashboard-header {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  gap: 16px;
  flex-wrap: wrap;
  padding: 18px 22px;
  border-radius: 18px;
  background:
    radial-gradient(900px 200px at 0% 0%, var(--accent-soft-strong), transparent 60%),
    radial-gradient(700px 220px at 100% 0%, rgba(139, 92, 246, 0.14), transparent 60%),
    var(--bg-card);
  border: 1px solid var(--border);
  box-shadow: var(--shadow);
  overflow: hidden;
}
.dashboard-header h1 {
  margin: 0;
  font-size: 28px;
  font-weight: 700;
}
.dashboard-baslik-blok {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.karsilama-mesaji {
  margin: 0;
  font-size: 14px;
  color: var(--text-secondary);
}
.karsilama-mesaji strong {
  color: var(--text-primary);
}
.karsilama-sirket {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-left: 8px;
  padding: 2px 10px;
  border-radius: 20px;
  background: var(--accent-soft);
  color: var(--accent);
  font-size: 12px;
  font-weight: 600;
}
.dashboard-canli {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 6px;
  flex-wrap: wrap;
}
.canli-rozet {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 2px 10px;
  border-radius: 999px;
  background: rgba(16, 185, 129, 0.14);
  color: #34d399;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.3px;
}
.canli-nokta {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #10b981;
  box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.6);
  animation: canli-nabiz 1.8s infinite;
}
@keyframes canli-nabiz {
  0% {
    box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.55);
  }
  70% {
    box-shadow: 0 0 0 7px rgba(16, 185, 129, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(16, 185, 129, 0);
  }
}
.canli-zaman {
  font-size: 11.5px;
  color: var(--text-muted);
}
@media (prefers-reduced-motion: reduce) {
  .canli-nokta {
    animation: none;
  }
}
.header-sag {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  min-width: 0;
}
.dashboard-datetime {
  font-size: 13px;
  color: var(--text-secondary);
  white-space: nowrap;
}
.dashboard-datetime i {
  margin-right: 6px;
}
.skeleton-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(230px, 100%), 1fr));
  gap: 16px;
  margin-bottom: 24px;
}
.skeleton-bottom {
  grid-column: 1 / -1;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}
.skeleton-card {
  border-radius: 14px;
  overflow: hidden;
}
.skeleton-grid :deep(.p-skeleton) {
  background: linear-gradient(
    90deg,
    rgba(148, 163, 184, 0.12) 25%,
    rgba(148, 163, 184, 0.24) 37%,
    rgba(148, 163, 184, 0.12) 63%
  );
  background-size: 400% 100%;
  animation: iskelet-parlama 1.4s ease infinite;
}
@keyframes iskelet-parlama {
  0% {
    background-position: 100% 50%;
  }
  100% {
    background-position: 0 50%;
  }
}
@media (prefers-reduced-motion: reduce) {
  .skeleton-grid :deep(.p-skeleton) {
    animation: none;
  }
}

.widget-ayarlari {
  margin-bottom: 24px;
}
.widget-togglar {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}
.ozet-kart {
  margin-bottom: 20px;
  border-color: rgba(139, 92, 246, 0.3) !important;
}
.ozet-metin {
  margin: 0;
  font-size: 14px;
  line-height: 1.6;
  color: var(--text-primary);
}
.ozet-ikon {
  margin-right: 8px;
  color: var(--accent);
}
.vade-ikon {
  margin-right: 8px;
}
.vade-ikon-gecen {
  color: #ef4444;
}
.vade-ikon-yaklasan {
  color: #f59e0b;
}
.widget-toggle {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  cursor: pointer;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(230px, 100%), 1fr));
  gap: 16px;
  margin-bottom: 24px;
}
.stat-card {
  background: var(--bg-card);
  padding: 18px;
  border-radius: 14px;
  border: 1px solid var(--border);
  display: flex;
  align-items: center;
  gap: 16px;
  transition: all 0.3s;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.2);
  position: relative;
  overflow: hidden;
}
.stat-card::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  background: linear-gradient(180deg, var(--accent), var(--accent-hover));
}
.stat-card.cari::before {
  background: linear-gradient(180deg, var(--accent), var(--accent-hover));
}
.stat-card.finans::before {
  background: linear-gradient(180deg, #6366f1, #4f46e5);
}
.stat-card.fatura::before {
  background: linear-gradient(180deg, #10b981, #059669);
}
.stat-card.banka::before {
  background: linear-gradient(180deg, #f59e0b, #d97706);
}
.stat-card.kasa::before {
  background: linear-gradient(180deg, #14b8a6, #0d9488);
}
.stat-card.stok::before {
  background: linear-gradient(180deg, #f97316, #ea580c);
}
.stat-card.beklemede::before {
  background: linear-gradient(180deg, #ef4444, #dc2626);
}
.stat-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.3);
  border-color: var(--accent-soft-strong);
}
.stat-icon {
  width: 52px;
  height: 52px;
  background: linear-gradient(135deg, var(--accent), #1565c0);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
  color: white;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.25);
}
.stat-icon.cari {
  background: linear-gradient(135deg, var(--accent), var(--accent-hover));
}
.stat-icon.finans {
  background: linear-gradient(135deg, #6366f1, #4f46e5);
}
.stat-icon.fatura {
  background: linear-gradient(135deg, #10b981, #059669);
}
.stat-icon.banka {
  background: linear-gradient(135deg, #f59e0b, #d97706);
}
.stat-icon.banka-bakiye {
  background: linear-gradient(135deg, #8b5cf6, #7c3aed);
}
.stat-icon.kasa {
  background: linear-gradient(135deg, #14b8a6, #0d9488);
}
.stat-icon.stok {
  background: linear-gradient(135deg, #f97316, #ea580c);
}
.stat-icon.ticaret {
  background: linear-gradient(135deg, #22c55e, #16a34a);
}
.stat-icon.beklemede {
  background: linear-gradient(135deg, #ef4444, #dc2626);
}
.stat-icon.iade {
  background: linear-gradient(135deg, #f59e0b, #d97706);
}
.stat-icon.devir {
  background: linear-gradient(135deg, #06b6d4, #0891b2);
}
.stat-icon.tahsilat {
  background: linear-gradient(135deg, #22c55e, #16a34a);
}
.stat-icon.odeme {
  background: linear-gradient(135deg, #ef4444, #dc2626);
}
.stat-icon.bekleyen-izin {
  background: linear-gradient(135deg, #f59e0b, #d97706);
}
.stat-icon.calisan {
  background: linear-gradient(135deg, #22c55e, #16a34a);
}
.stat-icon.izinli {
  background: linear-gradient(135deg, #eab308, #ca8a04);
}
.stat-icon.ise-baslayacak {
  background: linear-gradient(135deg, var(--accent), var(--accent-hover));
}
.stat-content {
  flex: 1;
  min-width: 0;
}
.stat-label {
  font-size: 12px;
  color: var(--text-secondary);
  margin: 0 0 6px;
}
.stat-value {
  font-size: 22px;
  font-weight: 700;
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.stat-value small {
  font-size: 12px;
  color: var(--text-secondary);
  font-weight: 400;
}
.stat-value.positive {
  color: #4ade80;
}
.stat-value.negative {
  color: #f87171;
}
[data-theme='light'] .stat-value.positive {
  color: #059669 !important;
}
[data-theme='light'] .stat-value.negative {
  color: #dc2626 !important;
}
.critical-hint {
  margin: 4px 0 0;
  font-size: 11px;
  color: #f87171;
}
.stat-sub {
  margin: 4px 0 0;
  font-size: 11px;
  color: var(--text-secondary);
}

.mini-stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(min(170px, 100%), 1fr));
  gap: 12px;
  margin-bottom: 24px;
}
.mini-stat {
  display: flex;
  align-items: center;
  gap: 12px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 12px 14px;
  transition: all 0.2s;
  box-shadow: var(--shadow);
}
.mini-stat:hover {
  border-color: var(--accent-border);
  transform: translateY(-2px);
}
.mini-stat-icon {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  color: white;
  flex-shrink: 0;
}
.mini-stat-icon.siparis {
  background: linear-gradient(135deg, var(--accent), var(--accent-hover));
}
.mini-stat-icon.teslimat {
  background: linear-gradient(135deg, #8b5cf6, #7c3aed);
}
.mini-stat-icon.iade {
  background: linear-gradient(135deg, #ef4444, #dc2626);
}
.mini-stat-icon.devir {
  background: linear-gradient(135deg, #06b6d4, #0891b2);
}
.mini-stat-icon.calisan {
  background: linear-gradient(135deg, #22c55e, #16a34a);
}
.mini-stat-icon.izinli {
  background: linear-gradient(135deg, #eab308, #ca8a04);
}
.mini-stat-icon.bekleyen-izin {
  background: linear-gradient(135deg, #f59e0b, #d97706);
}
.mini-stat-icon.odeme {
  background: linear-gradient(135deg, #ef4444, #dc2626);
}
.mini-stat-icerik {
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.mini-stat-etiket {
  font-size: 11px;
  color: var(--text-muted);
  margin-bottom: 2px;
}
.mini-stat-deger {
  font-size: 17px;
  font-weight: 700;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.bugun-ozet-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(200px, 100%), 1fr));
  gap: 12px;
  margin-bottom: 16px;
}
.bugun-kart {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border-radius: 12px;
  color: white;
  box-shadow: var(--shadow);
}
.bugun-kart i {
  font-size: 22px;
  opacity: 0.9;
}
.bugun-kart span {
  display: block;
  font-size: 11px;
  opacity: 0.9;
  margin-bottom: 2px;
}
.bugun-kart strong {
  font-size: 18px;
  font-weight: 800;
}
.bugun-kart.tahsilat {
  background: linear-gradient(135deg, #059669, #10b981);
}
.bugun-kart.odeme {
  background: linear-gradient(135deg, #dc2626, #ef4444);
}
.bugun-kart.siparis {
  background: linear-gradient(135deg, var(--accent-hover), var(--accent));
}
.bugun-kart.teslimat {
  background: linear-gradient(135deg, #7c3aed, #8b5cf6);
}

.cari-ozet-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(220px, 100%), 1fr));
  gap: 12px;
  margin-bottom: 20px;
}
.cari-ozet-kart {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border-radius: 12px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  box-shadow: var(--shadow);
}
.cari-ozet-kart i {
  width: 42px;
  height: 42px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  color: white;
  flex-shrink: 0;
}
.cari-ozet-kart.alacak i {
  background: linear-gradient(135deg, #059669, #10b981);
}
.cari-ozet-kart.borc i {
  background: linear-gradient(135deg, #dc2626, #ef4444);
}
.cari-ozet-kart.enborc i {
  background: linear-gradient(135deg, #b45309, #f59e0b);
}
.cari-ozet-kart span {
  display: block;
  font-size: 11px;
  color: var(--text-muted);
  margin-bottom: 2px;
}
.cari-ozet-kart strong {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
  display: block;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.cari-ozet-kart small {
  font-size: 11px;
  color: #ef4444;
  font-weight: 600;
}

.hedef-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(280px, 100%), 1fr));
  gap: 14px;
  margin-bottom: 20px;
}
.hedef-kart {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 16px;
  box-shadow: var(--shadow);
}
.hedef-baslik {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
  font-size: 13px;
}
.hedef-baslik span {
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  gap: 6px;
}
.hedef-baslik span i {
  color: var(--accent);
}
.hedef-baslik strong {
  color: var(--text-primary);
  font-size: 14px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.hedef-track {
  height: 8px;
  border-radius: 999px;
  background: var(--bg-muted, rgba(148, 163, 184, 0.12));
  overflow: hidden;
}
.hedef-fill {
  height: 100%;
  border-radius: 999px;
  transition: width 0.4s ease;
}
.hedef-fill.ciro {
  background: linear-gradient(90deg, var(--accent-hover), var(--accent));
}
.hedef-fill.kar {
  background: linear-gradient(90deg, #7c3aed, #8b5cf6);
}
.hedef-yuzde {
  display: block;
  margin-top: 6px;
  font-size: 12px;
  font-weight: 700;
  color: var(--accent);
  text-align: right;
}
.alacak-borc-satir {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  margin-top: 8px;
  font-size: 12px;
  font-weight: 600;
}
.alacak-borc-satir .alacak {
  color: #10b981;
}
.alacak-borc-satir .borc {
  color: #ef4444;
}

.kritik-stok-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(min(180px, 100%), 1fr));
  gap: 12px;
  margin-bottom: 20px;
}
.kritik-stok-kart {
  background: var(--bg-card);
  border: 1px solid rgba(239, 68, 68, 0.25);
  border-radius: 12px;
  padding: 12px 14px;
  box-shadow: var(--shadow);
}
.ks-kod {
  font-size: 11px;
  font-weight: 700;
  color: var(--accent);
  font-family: monospace;
}
.ks-ad {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 2px 0 6px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.ks-miktar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
}
.ks-deger {
  font-weight: 700;
  color: #ef4444;
}
.ks-min {
  color: var(--text-muted);
}

.charts-row {
  display: grid;
  grid-template-columns: repeat(12, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}
.charts-row > :deep(.p-card) {
  margin: 0;
  border-radius: 16px;
  border: 1px solid var(--border);
  background:
    linear-gradient(180deg, rgba(148, 163, 184, 0.05), rgba(148, 163, 184, 0) 55%),
    var(--bg-card);
  box-shadow: var(--shadow);
  transition:
    transform 0.18s ease,
    box-shadow 0.18s ease,
    border-color 0.18s ease;
}
.charts-row > :deep(.p-card):hover {
  transform: translateY(-3px);
  box-shadow: 0 12px 28px rgba(0, 0, 0, 0.14);
  border-color: var(--accent-border);
}
.charts-row > :deep(.p-card) .p-card-title {
  display: flex;
  align-items: center;
  font-size: 14px;
  font-weight: 700;
}
.charts-row > :deep(.p-card):nth-child(1) {
  grid-column: span 4;
}
.charts-row > :deep(.p-card):nth-child(2) {
  grid-column: span 8;
}
.charts-row > :deep(.p-card):nth-child(3) {
  grid-column: span 7;
}
.charts-row > :deep(.p-card):nth-child(4) {
  grid-column: span 5;
}
.charts-row > :deep(.p-card):nth-child(5) {
  grid-column: span 4;
}
.charts-row > :deep(.p-card):nth-child(6) {
  grid-column: span 8;
}
.charts-row > :deep(.p-card):nth-child(7) {
  grid-column: span 4;
}
.charts-row > :deep(.p-card):nth-child(8) {
  grid-column: span 8;
}
.charts-row > :deep(.p-card):nth-child(9) {
  grid-column: span 12;
}
.charts-row > :deep(.p-card):nth-child(10) {
  grid-column: span 6;
}
.charts-row > :deep(.p-card):nth-child(11) {
  grid-column: span 6;
}
@media (prefers-reduced-motion: reduce) {
  .charts-row > :deep(.p-card),
  .charts-row > :deep(.p-card):hover {
    transition: none;
    transform: none;
  }
}
.chart-wrapper {
  width: 100%;
  height: 260px;
}
.chart-wrapper.full {
  max-width: 100%;
  height: 260px;
  margin: 0;
}
.nakit-akisi-kart {
  margin-bottom: 16px;
}
.nakit-akisi-wrapper {
  height: 260px;
}
.tumu-gor {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  margin-top: 10px;
  font-size: 12px;
  font-weight: 600;
  color: var(--accent);
  text-decoration: none;
}
.tumu-gor:hover {
  color: var(--accent-hover);
}
.aylik-chart {
  max-width: 100%;
  height: 260px;
}
.chart-wrapper.line-chart {
  max-width: 100%;
}
.chart-summary {
  text-align: center;
  margin-top: 12px;
  font-size: 13px;
  color: var(--text-secondary);
  display: flex;
  justify-content: center;
  gap: 20px;
}
.chart-empty {
  text-align: center;
  padding: 30px;
  color: var(--text-muted);
}
.dot {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  margin-right: 6px;
  vertical-align: middle;
}
.dot.pos {
  background: var(--success);
}
.dot.neg {
  background: var(--danger);
}
.dot.kasa {
  background: #f59e0b;
}
.dot.banka {
  background: var(--accent);
}

.bottom-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}
.recent-transactions {
  background: var(--bg-card);
  padding: 20px;
  border-radius: 14px;
  border: 1px solid var(--border);
}
.recent-transactions h2 {
  margin: 0 0 12px;
  font-size: 18px;
}

.reminder-grid {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.vade-uyarilari {
  margin-bottom: 24px;
}
.vade-card .p-card-title {
  font-size: 14px !important;
}
.reminder-item small {
  color: var(--text-muted);
  margin-left: 6px;
}
.reminder-card .p-card-title {
  font-size: 14px !important;
}
.reminder-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  padding: 8px 0;
  border-bottom: 1px solid var(--border);
  min-width: 0;
}
.reminder-ad {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
}
.reminder-aksiyon {
  display: flex;
  align-items: center;
  gap: 10px;
}
.whatsapp-buton {
  color: #25d366;
  font-size: 18px;
  display: inline-flex;
}
.whatsapp-buton:hover {
  transform: scale(1.15);
}
.reminder-item:last-child {
  border-bottom: none;
}
.reminder-tutar {
  font-size: 13px;
  font-weight: 600;
  color: #4ade80;
}
.reminder-tutar.negative {
  color: #f87171;
}
.reminder-empty {
  text-align: center;
  padding: 12px;
  color: #4ade80;
  font-size: 13px;
}
.reminder-empty i {
  display: block;
  font-size: 22px;
  margin-bottom: 4px;
}

.badge {
  padding: 2px 10px;
  border-radius: 20px;
  font-size: 11px;
  font-weight: 600;
}
.badge.tahsilat {
  background: rgba(76, 175, 80, 0.15);
  color: #4ade80;
}
.badge.odeme {
  background: rgba(244, 67, 54, 0.15);
  color: #f87171;
}
.positive {
  color: var(--success);
  font-weight: bold;
}
.negative {
  color: var(--danger);
  font-weight: bold;
}

.section-title {
  font-size: 16px;
  font-weight: 800;
  margin: 24px 0 14px;
  color: var(--text-primary);
  display: flex;
  align-items: center;
  gap: 10px;
  letter-spacing: -0.01em;
}
.section-title i {
  color: var(--accent);
  font-size: 15px;
  width: 30px;
  height: 30px;
  border-radius: 9px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: rgba(59, 130, 246, 0.14);
  flex-shrink: 0;
}
.section-title::after {
  content: '';
  flex: 1;
  height: 1px;
  background: linear-gradient(90deg, var(--border), transparent);
  margin-left: 4px;
}

@media (max-width: 1100px) {
  .charts-row > :deep(.p-card) {
    grid-column: span 6 !important;
  }
  .charts-row > :deep(.p-card):nth-child(9),
  .charts-row > :deep(.p-card):nth-child(2),
  .charts-row > :deep(.p-card):nth-child(3) {
    grid-column: span 12 !important;
  }
}
@media (max-width: 760px) {
  .charts-row {
    grid-template-columns: 1fr;
  }
  .charts-row > :deep(.p-card) {
    grid-column: auto !important;
  }
  .bottom-grid {
    grid-template-columns: 1fr;
  }
}

.doviz-ticker-compact {
  display: flex;
  align-items: center;
  gap: 8px;
  background: var(--bg-card, rgba(255, 255, 255, 0.05));
  border: 1px solid var(--border, rgba(255, 255, 255, 0.12));
  border-radius: 20px;
  padding: 4px 10px;
  font-size: 11px;
  flex-wrap: wrap;
}

.ticker-chip {
  display: flex;
  align-items: center;
  gap: 4px;
}

.chip-kod {
  font-weight: 700;
  color: var(--text-secondary, #94a3b8);
}

.chip-fiyat {
  font-weight: 600;
  color: #10b981;
}

.chip-refresh-btn {
  background: transparent;
  border: none;
  color: var(--text-muted, #64748b);
  cursor: pointer;
  padding: 2px 4px;
  display: flex;
  align-items: center;
  transition: color 0.15s;
}

.chip-refresh-btn:hover {
  color: var(--accent);
}

.quick-actions {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(min(180px, 100%), 1fr));
  gap: 12px;
  margin-bottom: 24px;
}
.action-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 18px;
  border-radius: 12px;
  text-decoration: none;
  color: white;
  font-weight: 600;
  font-size: 13.5px;
  transition:
    transform 0.15s,
    box-shadow 0.15s;
}
.action-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}
.action-card i {
  font-size: 1.3rem;
}
.action-card.kokpit {
  background: linear-gradient(135deg, #d97706, #b45309);
}
.action-card.teklif {
  background: linear-gradient(135deg, var(--accent-hover), var(--accent));
}
.action-card.saha {
  background: linear-gradient(135deg, #7c3aed, #8b5cf6);
}
.action-card.fatura {
  background: linear-gradient(135deg, #4f46e5, #6366f1);
}
.action-card.satis {
  background: linear-gradient(135deg, #059669, #10b981);
}
.action-card.cari {
  background: linear-gradient(135deg, #d97706, #f59e0b);
}
.action-card.tahsilat {
  background: linear-gradient(135deg, #dc2626, #ef4444);
}
.action-card.kasa {
  background: linear-gradient(135deg, #0284c7, #0ea5e9);
}
.action-card.stok {
  background: linear-gradient(135deg, #b45309, #d97706);
}

.backup-reminder {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  padding: 12px 20px;
  margin-bottom: 20px;
  background: rgba(245, 158, 11, 0.12);
  border: 1px solid rgba(245, 158, 11, 0.35);
  border-radius: 12px;
  font-size: 14px;
  color: var(--text-primary);
}
.backup-reminder a {
  color: var(--accent);
  font-weight: 600;
}
.reminder-close {
  margin-left: auto;
  background: none;
  border: none;
  font-size: 20px;
  cursor: pointer;
  color: var(--text-muted);
}

@media (max-width: 768px) {
  .quick-actions {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
@media (max-width: 480px) {
  .quick-actions,
  .stats-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 900px) {
  .dashboard-header {
    flex-direction: column;
    align-items: flex-start;
  }
  .header-sag {
    flex-wrap: wrap;
    width: 100%;
    justify-content: flex-end;
  }
  .dashboard-datetime {
    white-space: normal;
  }
  .skeleton-bottom,
  .bottom-grid {
    grid-template-columns: 1fr;
  }
}
</style>
