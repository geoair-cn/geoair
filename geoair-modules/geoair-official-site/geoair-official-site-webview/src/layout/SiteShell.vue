<template>
  <div class="site-shell">
    <header class="site-header">
      <div class="section-block header-inner">
        <router-link class="brand" to="/">
          <span class="brand-mark">GA</span>
          <span class="brand-text">
            <strong>{{ siteMeta.name }}</strong>
            <small>{{ siteMeta.tagline }}</small>
          </span>
        </router-link>

        <nav class="desktop-nav">
          <router-link
            v-for="item in topNav"
            :key="item.to"
            :to="item.to"
            class="nav-link"
            :class="{ active: isActive(item.to) }"
          >
            {{ item.label }}
          </router-link>
        </nav>

        <a class="repo-link" :href="siteMeta.footer.repo" target="_blank" rel="noreferrer">
          GitHub
        </a>
      </div>
    </header>

    <main class="site-main">
      <slot />
    </main>

    <footer class="site-footer">
      <div class="section-block footer-inner">
        <div>
          <div class="footer-title">{{ siteMeta.name }}</div>
          <div class="footer-text">{{ siteMeta.description }}</div>
        </div>
        <div class="footer-meta">
          <span>组织：{{ siteMeta.footer.organization }}</span>
          <span>维护：{{ siteMeta.footer.maintainer }}</span>
          <span>邮箱：{{ siteMeta.footer.email }}</span>
          <span>协议：{{ siteMeta.footer.license }}</span>
        </div>
      </div>
    </footer>
  </div>
</template>

<script>
import { siteMeta, topNav } from '@/content/site'

export default {
  name: 'SiteShell',
  data() {
    return {
      siteMeta,
      topNav
    }
  },
  methods: {
    isActive(path) {
      return this.$route.path === path || this.$route.path.startsWith(`${path}/`)
    }
  }
}
</script>

<style scoped lang="less">
.site-shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.site-header {
  position: sticky;
  top: 0;
  z-index: 20;
  backdrop-filter: blur(14px);
  background: rgba(10, 15, 27, 0.86);
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.header-inner {
  height: 72px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: 14px;
  color: #fff;
}

.brand-mark {
  width: 40px;
  height: 40px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  background: linear-gradient(135deg, #2563eb, #0f766e);
  font-weight: 700;
  letter-spacing: 1px;
}

.brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.2;

  strong {
    font-size: 18px;
    font-weight: 700;
  }

  small {
    color: rgba(255, 255, 255, 0.7);
    font-size: 12px;
  }
}

.desktop-nav {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
  justify-content: center;
}

.nav-link {
  color: rgba(255, 255, 255, 0.78);
  padding: 10px 14px;
  border-radius: 999px;
  transition: all 0.25s ease;

  &:hover,
  &.active {
    color: #fff;
    background: rgba(255, 255, 255, 0.12);
  }
}

.repo-link {
  color: #fff;
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 999px;
  padding: 10px 16px;
  transition: all 0.25s ease;

  &:hover {
    background: rgba(255, 255, 255, 0.1);
  }
}

.site-main {
  flex: 1;
}

.site-footer {
  background: #0f172a;
  color: rgba(255, 255, 255, 0.85);
  margin-top: 72px;
}

.footer-inner {
  padding-top: 36px;
  padding-bottom: 36px;
  display: grid;
  grid-template-columns: 1.2fr 1fr;
  gap: 24px;
}

.footer-title {
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 10px;
}

.footer-text {
  color: rgba(255, 255, 255, 0.72);
  max-width: 720px;
}

.footer-meta {
  display: grid;
  gap: 8px;
  justify-items: start;
}

@media (max-width: 900px) {
  .header-inner {
    height: auto;
    padding-top: 16px;
    padding-bottom: 16px;
    flex-wrap: wrap;
  }

  .desktop-nav {
    width: 100%;
    justify-content: flex-start;
    overflow-x: auto;
    padding-bottom: 4px;
  }

  .footer-inner {
    grid-template-columns: 1fr;
  }
}
</style>
