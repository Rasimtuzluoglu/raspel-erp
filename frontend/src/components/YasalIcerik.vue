<template>
  <div class="yasal-bolum">
    <div class="hero-card">
      <div class="hero-icon">
        <i :class="heroIcon" />
      </div>
      <div class="hero-text">
        <h2>{{ heroBaslik }}</h2>
        <p>{{ sonGuncelleme }}</p>
      </div>
      <div class="search-box">
        <IconField class="w-full">
          <InputIcon class="pi pi-search" />
          <InputText
            v-model="aramaMetni"
            :placeholder="aramaPlaceholder"
            class="w-full search-input"
          />
        </IconField>
      </div>
    </div>

    <div class="layout-grid">
      <div class="toc-card">
        <h3><i class="pi pi-list" /> {{ icindekiler }}</h3>
        <nav class="toc-nav">
          <a
            v-for="(s, idx) in filtrelenmisMadde"
            :key="s.id"
            :href="`#section-${kod}-${s.id}`"
            class="toc-item"
            :class="{ active: aktifSecim === s.id }"
            @click="aktifSecim = s.id"
          >
            <span class="toc-num">{{ idx + 1 }}</span>
            <span class="toc-title">{{ s.baslik }}</span>
          </a>
        </nav>
      </div>

      <div class="content-cards">
        <div
          v-if="filtrelenmisMadde.length === 0"
          class="no-results"
        >
          <i class="pi pi-filter-slash" />
          <h4>{{ bulunamadi }}</h4>
          <Button
            :label="filtreTemizle"
            icon="pi pi-refresh"
            class="p-button-text"
            @click="aramaMetni = ''"
          />
        </div>

        <div
          v-for="(s, idx) in filtrelenmisMadde"
          :id="`section-${kod}-${s.id}`"
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

const props = defineProps({
  /** Bölüm kodu: anchor id çakışmasını önler (kullanim | gizlilik). */
  kod: { type: String, required: true },
  maddeler: { type: Array, required: true },
  heroBaslik: { type: String, default: '' },
  sonGuncelleme: { type: String, default: '' },
  aramaPlaceholder: { type: String, default: '' },
  icindekiler: { type: String, default: '' },
  bulunamadi: { type: String, default: '' },
  filtreTemizle: { type: String, default: '' },
  heroIcon: { type: String, default: 'pi pi-file-check' }
})

const aramaMetni = ref('')
const aktifSecim = ref(1)

const filtrelenmisMadde = computed(() => {
  if (!aramaMetni.value.trim()) return props.maddeler
  const query = aramaMetni.value.toLowerCase()
  return props.maddeler.filter(
    (m) =>
      m.baslik.toLowerCase().includes(query) ||
      m.icerik.toLowerCase().includes(query) ||
      m.kategori.toLowerCase().includes(query)
  )
})
</script>

<style scoped>
.hero-card {
  background: linear-gradient(135deg, var(--accent-soft) 0%, rgba(139, 92, 246, 0.12) 100%);
  border: 1px solid var(--accent-soft-strong);
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
  background: var(--accent);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
  flex-shrink: 0;
  box-shadow: 0 4px 12px var(--accent-border);
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
  background: var(--accent-soft);
  color: var(--text-primary);
}
.toc-item.active {
  background: var(--accent);
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
  border-color: var(--accent-border);
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.15);
}
.madde-card.highlighted {
  border-color: var(--accent);
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
  background: var(--accent-soft);
  border: 1px solid var(--accent-soft-strong);
  border-radius: 8px;
  padding: 8px 12px;
  font-size: 0.82rem;
  color: var(--accent);
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
