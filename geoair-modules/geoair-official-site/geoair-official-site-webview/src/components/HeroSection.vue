<template>
  <section class="hero-section">
    <div class="section-block hero-grid">
      <div class="hero-copy">
        <span class="eyebrow">GeoAir Framework</span>
        <h1 class="page-title">{{ title }}</h1>
        <p class="page-subtitle">{{ subtitle }}</p>

        <div class="hero-actions">
          <template v-for="action in actions">
            <router-link
              v-if="action.to"
              :key="action.label"
              :to="action.to"
              class="hero-action"
              :class="action.type"
            >
              {{ action.label }}
            </router-link>
            <a
              v-else
              :key="action.label"
              class="hero-action"
              :class="action.type"
              :href="action.href"
              target="_blank"
              rel="noreferrer"
            >
              {{ action.label }}
            </a>
          </template>
        </div>

        <div class="hero-stats">
          <div v-for="item in highlights" :key="item.label" class="stat-item surface-card">
            <strong>{{ item.value }}</strong>
            <span>{{ item.label }}</span>
            <small>{{ item.detail }}</small>
          </div>
        </div>
      </div>

      <div class="hero-panel surface-card">
        <div class="panel-head">
          <span class="panel-pill">为什么选择 GeoAir</span>
        </div>
        <div class="panel-list">
          <div v-for="item in principles" :key="item.title" class="panel-item">
            <h3>{{ item.title }}</h3>
            <p>{{ item.description }}</p>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script>
export default {
  name: 'HeroSection',
  props: {
    title: {
      type: String,
      required: true
    },
    subtitle: {
      type: String,
      required: true
    },
    actions: {
      type: Array,
      default: () => []
    },
    highlights: {
      type: Array,
      default: () => []
    },
    principles: {
      type: Array,
      default: () => []
    }
  }
}
</script>

<style scoped lang="less">
.hero-section {
  background: radial-gradient(circle at top left, rgba(59, 130, 246, 0.3), transparent 28%),
    linear-gradient(135deg, #0f172a 0%, #102a57 52%, #0f766e 100%);
  color: #fff;
  padding: 72px 0 56px;
}

.hero-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.25fr) minmax(320px, 0.9fr);
  gap: 28px;
  align-items: center;
}

.eyebrow {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.12);
  color: rgba(255, 255, 255, 0.88);
  margin-bottom: 18px;
  font-size: 13px;
}

.hero-actions {
  margin-top: 28px;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.hero-action {
  padding: 12px 18px;
  border-radius: 10px;
  border: 1px solid rgba(255, 255, 255, 0.18);
  color: #fff;
  transition: all 0.25s ease;

  &:hover {
    transform: translateY(-1px);
  }

  &.primary {
    background: #fff;
    color: #0f172a;
  }

  &.default,
  &.ghost {
    background: rgba(255, 255, 255, 0.08);
  }
}

.hero-stats {
  margin-top: 30px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.stat-item {
  background: rgba(255, 255, 255, 0.96);
  padding: 18px;
  border-radius: 14px;
  color: var(--text-main);

  strong {
    display: block;
    font-size: 26px;
    color: var(--primary);
    line-height: 1;
  }

  span {
    display: block;
    margin-top: 8px;
    font-weight: 600;
  }

  small {
    display: block;
    margin-top: 6px;
    color: var(--text-muted);
    line-height: 1.5;
  }
}

.hero-panel {
  padding: 26px;
  background: rgba(255, 255, 255, 0.96);
  color: var(--text-main);
}

.panel-pill {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  border-radius: 999px;
  background: var(--primary-soft);
  color: var(--primary);
  font-size: 13px;
  font-weight: 600;
}

.panel-list {
  margin-top: 18px;
  display: grid;
  gap: 16px;
}

.panel-item {
  padding: 16px 0;
  border-top: 1px solid var(--border-color);

  h3 {
    font-size: 18px;
    margin-bottom: 8px;
  }

  p {
    color: var(--text-secondary);
  }
}

@media (max-width: 900px) {
  .hero-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .hero-section {
    padding-top: 54px;
  }

  .hero-stats {
    grid-template-columns: 1fr;
  }
}
</style>
