<template>
  <div class="hero-grid section-block">
    <div class="hero-copy surface-card">
      <div class="eyebrow">{{ siteMeta.version }}</div>
      <h1 class="title">{{ siteMeta.tagline }}</h1>
      <p class="description">{{ siteMeta.description }}</p>

      <div class="action-row">
        <template v-for="action in siteMeta.heroActions">
          <router-link
            v-if="action.to"
            :key="action.label"
            :to="action.to"
            class="action"
            :class="action.type"
          >
            {{ action.label }}
          </router-link>
          <a
            v-else
            :key="action.label"
            class="action"
            :class="action.type"
            :href="action.href"
            target="_blank"
            rel="noreferrer"
          >
            {{ action.label }}
          </a>
        </template>
      </div>
    </div>

    <div class="hero-side surface-card">
      <div class="side-title">核心亮点</div>
      <div class="side-list">
        <div v-for="item in siteMeta.highlights" :key="item.label" class="side-item">
          <strong>{{ item.value }}</strong>
          <span>{{ item.label }}</span>
          <small>{{ item.detail }}</small>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { siteMeta } from '@/content/site'

export default {
  name: 'HomeHero',
  data() {
    return {
      siteMeta
    }
  }
}
</script>

<style scoped lang="less">
.hero-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(300px, 0.85fr);
  gap: 24px;
  margin-top: -72px;
}

.hero-copy,
.hero-side {
  padding: 28px;
}

.hero-copy {
  background: linear-gradient(135deg, rgba(15, 23, 42, 0.96), rgba(29, 78, 216, 0.94));
  color: #fff;
}

.eyebrow {
  display: inline-flex;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.12);
  margin-bottom: 16px;
}

.title {
  font-size: 46px;
  line-height: 1.08;
  margin-bottom: 18px;
}

.description {
  max-width: 660px;
  color: rgba(255, 255, 255, 0.82);
  font-size: 17px;
}

.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 28px;
}

.action {
  padding: 12px 18px;
  border-radius: 10px;
  border: 1px solid rgba(255, 255, 255, 0.18);
  color: #fff;
  transition: all 0.25s ease;

  &.primary {
    background: #fff;
    color: #0f172a;
  }

  &.default,
  &.ghost {
    background: rgba(255, 255, 255, 0.08);
  }
}

.hero-side {
  display: grid;
  gap: 18px;
}

.side-title {
  font-size: 18px;
  font-weight: 700;
}

.side-list {
  display: grid;
  gap: 16px;
}

.side-item {
  padding-top: 16px;
  border-top: 1px solid var(--border-color);

  strong {
    display: block;
    font-size: 22px;
    color: var(--primary);
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
  }
}

@media (max-width: 900px) {
  .hero-grid {
    grid-template-columns: 1fr;
    margin-top: 0;
  }

  .title {
    font-size: 34px;
  }
}
</style>
