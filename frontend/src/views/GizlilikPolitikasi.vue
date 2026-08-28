<template>
  <div class="yasal-page">
    <PageHeader
      title="Gizlilik Politikası & KVKK"
      subtitle="Verilerinizin toplama, işlenme, saklanma ve korunma prensipleri."
    >
      <template #actions>
        <Button
          label="Yazdır"
          icon="pi pi-print"
          class="p-button-outlined p-button-secondary"
          @click="yazdir"
        />
      </template>
    </PageHeader>

    <div class="hero-card">
      <div class="hero-icon">
        <i class="pi pi-shield" />
      </div>
      <div class="hero-text">
        <h2>Veri Güvenliği ve Gizlilik Taahhüdü</h2>
        <p>6698 Sayılı KVKK Uyumlu | SSL Encrypted 256-bit | Son Güncelleme: 01 Ağustos 2026</p>
      </div>
      <div class="search-box">
        <span class="p-input-icon-left w-full">
          <i class="pi pi-search" />
          <InputText
            v-model="aramaMetni"
            placeholder="Gizlilik şartlarında ara... (örn. kvkk, güvenlik, imha)"
            class="w-full search-input"
          />
        </span>
      </div>
    </div>

    <div class="layout-grid">
      <!-- Sol Taraf: İçindekiler Navigasyon Barı -->
      <div class="toc-card">
        <h3><i class="pi pi-list" /> İçindekiler</h3>
        <nav class="toc-nav">
          <a
            v-for="(s, idx) in filtrelenmisMadde"
            :key="s.id"
            :href="`#section-${s.id}`"
            class="toc-item"
            :class="{ active: aktifSecim === s.id }"
            @click="secimDegistir(s.id)"
          >
            <span class="toc-num">{{ idx + 1 }}</span>
            <span class="toc-title">{{ s.baslik }}</span>
          </a>
        </nav>
      </div>

      <!-- Sağ Taraf: Kart Bazlı Dağıtılmış İçerik Grid -->
      <div class="content-cards">
        <div
          v-if="filtrelenmisMadde && filtrelenmisMadde.length === 0"
          class="no-results"
        >
          <i class="pi pi-filter-slash" />
          <h4>Aramanızla eşleşen gizlilik maddesi bulunamadı.</h4>
          <Button
            label="Filtreyi Temizle"
            icon="pi pi-refresh"
            class="p-button-text"
            @click="aramaMetni = ''"
          />
        </div>

        <div
          v-for="(s, idx) in filtrelenmisMadde"
          :id="`section-${s.id}`"
          :key="s.id"
          class="madde-card"
          :class="{ highlighted: aktifSecim === s.id }"
        >
          <div class="card-header">
            <div
              class="icon-badge"
              :style="{ background: s.iconBg, color: s.iconColor }"
            >
              <i :class="s.icon" />
            </div>
            <div class="title-wrap">
              <span class="card-tag">{{ s.kategori }}</span>
              <h3>{{ idx + 1 }}. {{ s.baslik }}</h3>
            </div>
          </div>
          <p class="card-text">
            {{ s.icerik }}
          </p>
          <div
            v-if="s.ipucu"
            class="card-tip"
          >
            <i class="pi pi-shield" />
            <span>{{ s.ipucu }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const aramaMetni = ref('')
const aktifSecim = ref(1)

const maddeler = [
  {
    id: 1,
    baslik: 'Toplanan Veri Türleri',
    kategori: 'Veri Toplama',
    icon: 'pi pi-folder-open',
    iconBg: 'rgba(59, 130, 246, 0.15)',
    iconColor: '#3b82f6',
    icerik:
      'Platform; şirket bilgileri, kullanıcı hesap detayları, müşteri/tedarikçi kayıtları, stok ve depo hareketleri, fatura verileri ve finansal işlem günlüklerini işler. Bu veriler yalnızca ERP sisteminin işlevlerini tam getirmesi amacıyla toplanır.',
    ipucu: 'Hiçbir finansal veya ticari veriniz üçüncü taraflarla reklam veya pazarlama amacıyla paylaşılmaz.'
  },
  {
    id: 2,
    baslik: 'Veri Kullanım ve İşleme Amaçları',
    kategori: 'İşleme Standartları',
    icon: 'pi pi-cog',
    iconBg: 'rgba(16, 185, 129, 0.15)',
    iconColor: '#10b981',
    icerik:
      'Toplanan veriler; e-fatura kesme, stok ve envanter takibi, finansal raporlama, denetim izi (audit log) kayıtları ve yasal vergilendirme yükümlülüklerinin eksiksiz yerine getirilmesi amacıyla kullanılır.',
    ipucu: 'Tüm veri işleme faaliyetleri yetki matrisine (RBAC) göre sınırlandırılmıştır.'
  },
  {
    id: 3,
    baslik: 'Veri Güvenliği ve Altyapı Tedbirleri',
    kategori: 'Siber Güvenlik',
    icon: 'pi pi-lock',
    iconBg: 'rgba(139, 92, 246, 0.15)',
    iconColor: '#8b5cf6',
    icerik:
      'Verileriniz 256-bit SSL şifreli şebeke bağlantısı, JWT tabanlı kimlik doğrulama, Rol Bazlı Erişim Kontrolü (RBAC) ve otomatik günlük veritabanı yedekleme mekanizmalarıyla korunur.',
    ipucu: 'Sistem altyapısı düzenli penetrasyon ve güvenlik taramalarından geçirilmektedir.'
  },
  {
    id: 4,
    baslik: 'KVKK (6698 Sayılı Kanun) Kapsamı',
    kategori: 'Mevzuat Uyum',
    icon: 'pi pi-verified',
    iconBg: 'rgba(245, 158, 11, 0.15)',
    iconColor: '#f59e0b',
    icerik:
      '6698 sayılı Kişisel Verilerin Korunması Kanunu uyarınca, verileriniz hukuka ve dürüstlük kurallarına uygun, belirli ve meşru amaçlar için sınırlı şekilde işlenmektedir.',
    ipucu: 'KVKK kapsamındaki bilgi alma ve düzeltme haklarınızı dilediğiniz zaman kullanabilirsiniz.'
  },
  {
    id: 5,
    baslik: 'Veri Saklama ve Arşiv Süreleri',
    kategori: 'Saklama & İmha',
    icon: 'pi pi-history',
    iconBg: 'rgba(6, 182, 212, 0.15)',
    iconColor: '#06b6d4',
    icerik:
      'Ticari ve finansal verileriniz ilgili vergi mevzuatı ve Türk Ticaret Kanunu gereği yasal saklama süreleri boyunca (10 yıl) emniyetli ortamda saklanır. Süre sonunda güvenli şekilde imha edilir.',
    ipucu: 'Hesabınızın kapatılması durumunda yasal zorunluluk haricindeki kişisel veriler silinir.'
  },
  {
    id: 6,
    baslik: 'Kullanıcı Hakları (Erişim, Düzeltme, Silme)',
    kategori: 'Kullanıcı Hakları',
    icon: 'pi pi-user-check',
    iconBg: 'rgba(236, 72, 153, 0.15)',
    iconColor: '#ec4899',
    icerik:
      'İşlenen verilerinizi görüntüleme, hatalı bilgileri düzeltme, silinmesini veya anonim hale getirilmesini talep etme hakkına sahipsiniz. Talepleriniz veri sorumlumuz tarafından incelenir.',
    ipucu: 'Kendi şirket verilerinizi Veri İçe/Dışa Aktar modüllerinden dilediğiniz an indirebilirsiniz.'
  },
  {
    id: 7,
    baslik: 'Politika Güncellemeleri ve Bildirimler',
    kategori: 'Güncellemeler',
    icon: 'pi pi-bell',
    iconBg: 'rgba(239, 68, 68, 0.15)',
    iconColor: '#ef4444',
    icerik:
      'Bu gizlilik politikası mevzuat değişiklikleri veya yeni platform modülleri eklendikçe güncellenebilir. Güncellemeler platform içi duyuru kanallarıyla tarafınıza bildirilir.',
    ipucu: 'Politikadaki önemli değişiklikler kullanıcılara e-posta veya sistem içi bildirimle iletilir.'
  }
]

const filtrelenmisMadde = computed(() => {
  if (!aramaMetni.value.trim()) return maddeler
  const query = aramaMetni.value.toLowerCase()
  return maddeler.filter(
    (m) =>
      m.baslik.toLowerCase().includes(query) ||
      m.icerik.toLowerCase().includes(query) ||
      m.kategori.toLowerCase().includes(query)
  )
})

const secimDegistir = (id) => {
  aktifSecim.value = id
}

const yazdir = () => {
  window.print()
}
</script>

<style scoped>
.yasal-page {
  padding: 1.5rem;
  max-width: 1300px;
  margin: 0 auto;
}

.hero-card {
  background: linear-gradient(135deg, rgba(16, 185, 129, 0.12) 0%, rgba(59, 130, 246, 0.12) 100%);
  border: 1px solid rgba(16, 185, 129, 0.25);
  border-radius: 16px;
  padding: 1.5rem 2rem;
  margin-bottom: 2rem;
  display: flex;
  align-items: center;
  gap: 1.5rem;
  flex-wrap: wrap;
}

.hero-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  background: #10b981;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.3);
}

.hero-text {
  flex: 1;
  min-width: 250px;
}

.hero-text h2 {
  margin: 0 0 4px;
  font-size: 1.35rem;
  font-weight: 700;
  color: var(--text-primary);
}

.hero-text p {
  margin: 0;
  font-size: 0.85rem;
  color: var(--text-secondary);
}

.search-box {
  width: 320px;
}

.search-input {
  border-radius: 10px;
}

.layout-grid {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 1.75rem;
}

@media (max-width: 900px) {
  .layout-grid {
    grid-template-columns: 1fr;
  }
}

.toc-card {
  background: var(--bg-card, #1e293b);
  border: 1px solid var(--border, rgba(255, 255, 255, 0.1));
  border-radius: 14px;
  padding: 1.25rem;
  height: fit-content;
  position: sticky;
  top: 1.5rem;
}

.toc-card h3 {
  font-size: 1rem;
  margin: 0 0 1rem;
  color: var(--text-primary);
  display: flex;
  align-items: center;
  gap: 8px;
}

.toc-nav {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.toc-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border-radius: 8px;
  text-decoration: none;
  color: var(--text-secondary);
  font-size: 0.85rem;
  font-weight: 500;
  transition: all 0.2s;
}

.toc-item:hover {
  background: rgba(16, 185, 129, 0.1);
  color: var(--text-primary);
}

.toc-item.active {
  background: #10b981;
  color: #ffffff;
}

.toc-num {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.15);
  font-size: 0.75rem;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
}

.toc-title {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.content-cards {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.madde-card {
  background: var(--bg-card, #1e293b);
  border: 1px solid var(--border, rgba(255, 255, 255, 0.1));
  border-radius: 14px;
  padding: 1.5rem;
  transition: all 0.25s;
}

.madde-card:hover {
  border-color: rgba(16, 185, 129, 0.4);
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.15);
}

.madde-card.highlighted {
  border-color: #10b981;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 1rem;
}

.icon-badge {
  width: 42px;
  height: 42px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.2rem;
  flex-shrink: 0;
}

.title-wrap h3 {
  margin: 2px 0 0;
  font-size: 1.1rem;
  font-weight: 600;
  color: var(--text-primary);
}

.card-tag {
  font-size: 0.7rem;
  font-weight: 700;
  text-transform: uppercase;
  color: var(--text-muted, #94a3b8);
  letter-spacing: 0.5px;
}

.card-text {
  font-size: 0.92rem;
  line-height: 1.65;
  color: var(--text-secondary);
  margin: 0 0 1rem;
}

.card-tip {
  display: flex;
  align-items: center;
  gap: 8px;
  background: rgba(16, 185, 129, 0.08);
  border: 1px solid rgba(16, 185, 129, 0.2);
  border-radius: 8px;
  padding: 8px 12px;
  font-size: 0.82rem;
  color: #34d399;
}

.no-results {
  text-align: center;
  padding: 3rem;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 14px;
  color: var(--text-secondary);
}

.no-results i {
  font-size: 3rem;
  margin-bottom: 1rem;
  opacity: 0.5;
}

.w-full {
  width: 100% !important;
}

@media print {
  .hero-card,
  .toc-card,
  .search-box {
    display: none !important;
  }
  .layout-grid {
    display: block !important;
  }
  .madde-card {
    break-inside: avoid;
    border: 1px solid #ddd !important;
    margin-bottom: 1rem;
  }
}
</style>
