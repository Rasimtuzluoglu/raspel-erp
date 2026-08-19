<template>
  <div class="yasal-page">
    <PageHeader
      title="Kullanım Şartları"
      subtitle="RasPel ERP platformunu kullanırken kabul ettiğiniz koşullar, haklar ve yükümlülükler."
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
        <i class="pi pi-file-check" />
      </div>
      <div class="hero-text">
        <h2>Kullanım Koşulları & Sözleşme Detayları</h2>
        <p>Son Güncelleme: 01 Ağustos 2026 | Sürüm: v2.4.0</p>
      </div>
      <div class="search-box">
        <span class="p-input-icon-left w-full">
          <i class="pi pi-search" />
          <InputText
            v-model="aramaMetni"
            placeholder="Şartlarda ara... (örn. yedekleme, veri, hesap)"
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
          v-if="filtrelenmisMadde.length === 0"
          class="no-results"
        >
          <i class="pi pi-filter-slash" />
          <h4>Aramanızla eşleşen şart maddesi bulunamadı.</h4>
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
            <i class="pi pi-info-circle" />
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
    baslik: 'Hizmet Tanımı',
    kategori: 'Genel Tanımlar',
    icon: 'pi pi-server',
    iconBg: 'rgba(59, 130, 246, 0.15)',
    iconColor: '#3b82f6',
    icerik:
      'RasPel ERP, kurumsal kaynak planlama (ERP) ve müşteri ilişkileri yönetimi (CRM) hizmetleri sunan bir yazılım platformudur. Bu platform; stok, cari hesap, fatura, sipariş, personel, puantaj, masraf ve detaylı raporlama modüllerini içerir.',
    ipucu: 'Sistem bulut ortamında 7/24 kesintisiz hizmet vermek üzere tasarlanmıştır.'
  },
  {
    id: 2,
    baslik: 'Hesap Güvenliği ve Sorumluluk',
    kategori: 'Kullanıcı Hesapları',
    icon: 'pi pi-user-edit',
    iconBg: 'rgba(16, 185, 129, 0.15)',
    iconColor: '#10b981',
    icerik:
      'Hesabınızın güvenliğinden ve şifrenizin gizliliğinden siz sorumlusunuz. Hesabınız üzerinden yapılan tüm işlemler doğrudan şirket yetkilisi olarak sizi bağlar. Başkalarıyla yetki paylaşımı yapılmamalıdır.',
    ipucu: 'Güçlü bir şifre kullanmanız ve düzenli olarak değiştirmeniz tavsiye edilir.'
  },
  {
    id: 3,
    baslik: 'Veri Sorumluluğu & Mülkiyet',
    kategori: 'Veri Yönetimi',
    icon: 'pi pi-database',
    iconBg: 'rgba(245, 158, 11, 0.15)',
    iconColor: '#f59e0b',
    icerik:
      'Platforma yüklediğiniz tüm verilerin (müşteri, stok, finansal bilgiler, belgeler) mülkiyeti tamamen şirketinize aittir. Verilerinizin yasalara uygun şekilde işlenmesinden ve saklanmasından şirketiniz sorumludur.',
    ipucu: 'Kişisel Verilerin Korunması Kanunu (KVKK) yükümlülüklerinizi yerine getirmeniz gerekmektedir.'
  },
  {
    id: 4,
    baslik: 'Hizmet Kesintisi & Sorumluluk Sınırı',
    kategori: 'Sistem Erişilebilirliği',
    icon: 'pi pi-exclamation-triangle',
    iconBg: 'rgba(239, 68, 68, 0.15)',
    iconColor: '#ef4444',
    icerik:
      'Platform bakım, güncelleme veya mücbir sebep durumlarında geçici olarak erişilemez olabilir. Planlı bakımlar önceden bildirilir. Bu durumlardan kaynaklanan olası doğrudan veya dolaylı zararlardan sağlayıcı sorumlu tutulamaz.',
    ipucu: 'Kritik işlemler öncesinde sistem duyurularını takip etmeniz önerilir.'
  },
  {
    id: 5,
    baslik: 'Veri Yedekleme & Arşivleme',
    kategori: 'Güvenlik',
    icon: 'pi pi-cloud-download',
    iconBg: 'rgba(139, 92, 246, 0.15)',
    iconColor: '#8b5cf6',
    icerik:
      'Sistem, düzenli aralıklarla otomatik veritabanı yedeklemesi yapmaktadır. Ancak verilerinizin anlık kopyalarını ve rapor çıktılarınızı düzenli aralıklarla dışa aktarıp arşivlemek kullanıcının sorumluluğundadır.',
    ipucu: 'Yedekler modülünden dilediğiniz an manuel yedek oluşturup indirebilirsiniz.'
  },
  {
    id: 6,
    baslik: 'Fikri Mülkiyet Hakları',
    kategori: 'Yasal Haklar',
    icon: 'pi pi-lock',
    iconBg: 'rgba(236, 72, 153, 0.15)',
    iconColor: '#ec4899',
    icerik:
      'Platformun kaynak kodları, veritabanı mimarisi, arayüz tasarımları ve ticari markası sağlayıcıya aittir. Yazılımın kopyalanması, tersine mühendislik yapılması veya izinsiz dağıtılması kesinlikle yasaktır.',
    ipucu: 'Tüm fikri mülkiyet hakları uluslararası telif kanunları ile korunmaktadır.'
  },
  {
    id: 7,
    baslik: 'İletişim & Destek Kanalları',
    kategori: 'Destek',
    icon: 'pi pi-headphones',
    iconBg: 'rgba(6, 182, 212, 0.15)',
    iconColor: '#06b6d4',
    icerik:
      'Sorularınız, teknik destek talepleriniz veya yetki değişiklikleriniz için sistem yöneticinizle veya platform içi e-posta destek hatlarıyla anında iletişime geçebilirsiniz.',
    ipucu: 'Destek talepleriniz mesai saatleri içerisinde en kısa sürede yanıtlanır.'
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
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.12) 0%, rgba(139, 92, 246, 0.12) 100%);
  border: 1px solid rgba(59, 130, 246, 0.25);
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
  background: #3b82f6;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
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
  background: rgba(59, 130, 246, 0.1);
  color: var(--text-primary);
}

.toc-item.active {
  background: #3b82f6;
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
  border-color: rgba(59, 130, 246, 0.4);
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.15);
}

.madde-card.highlighted {
  border-color: #3b82f6;
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
  background: rgba(59, 130, 246, 0.08);
  border: 1px solid rgba(59, 130, 246, 0.2);
  border-radius: 8px;
  padding: 8px 12px;
  font-size: 0.82rem;
  color: #60a5fa;
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
