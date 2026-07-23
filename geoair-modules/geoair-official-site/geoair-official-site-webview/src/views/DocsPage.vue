<template>
  <div class="page-container docs-page">
    <section class="docs-hero">
      <div class="section-block">
        <span class="hero-eyebrow">文档索引</span>
        <h1 class="page-title">先从现有 README 建立整体认知</h1>
        <p class="hero-copy">GeoAir 当前最完整的信息来源仍然是仓库内的 README、模块说明和 Maven 模块结构。官网把这些内容重新组织成更易浏览的形式，但原始文档依然是深入理解框架的第一入口。</p>
      </div>
    </section>

    <section class="page-section">
      <div class="section-block docs-layout">
        <div>
          <SectionIntro
            eyebrow="快速搜索"
            title="按模块名、路由、说明检索文档入口"
            description="搜索结果同时覆盖 README 入口和官网中的模块详情页，可以直接跳转到你关心的模块。"
          />
          <div class="search-panel surface-card">
            <el-input v-model.trim="keyword" placeholder="例如：adv-query、dynamic-ds、GeoServer、数据库服务" clearable />
            <div class="search-meta">
              <span>共 {{ filteredEntries.length }} 条结果</span>
            </div>
          </div>

          <div class="doc-grid">
            <template v-for="item in filteredEntries">
              <router-link v-if="item.to" :key="`${item.title}-${item.kind}`" class="doc-item surface-card" :to="item.to">
                <strong>{{ item.title }}</strong>
                <span class="doc-kind">{{ item.kind }}</span>
                <p>{{ item.description }}</p>
              </router-link>
              <a v-else :key="`${item.title}-${item.kind}`" class="doc-item surface-card" :href="item.href" target="_blank" rel="noreferrer">
                <strong>{{ item.title }}</strong>
                <span class="doc-kind">{{ item.kind }}</span>
                <p>{{ item.description }}</p>
              </a>
            </template>
          </div>
        </div>

        <div class="surface-card reading-card">
          <h3>推荐阅读路径</h3>
          <ol>
            <li>先读根 README，了解整个 geoair-root 与 geoair-framework 的位置。</li>
            <li>再读 Framework README，建立标准基础库、GIS 组件与业务模块的整体结构。</li>
            <li>随后进入 Modules README，确认 geoair-geo、dynamic-ds、apidoc、db-service 等重点模块。</li>
            <li>最后从本官网各模块详情页按需回到源码与原始文档。</li>
          </ol>
        </div>
      </div>
    </section>
  </div>
</template>

<script>
import SectionIntro from '@/components/SectionIntro.vue'
import { siteMeta } from '@/content/site'
import { allModules } from '@/content/modules'

export default {
  name: 'DocsPage',
  components: {
    SectionIntro
  },
  data() {
    return {
      keyword: '',
      siteMeta
    }
  },
  computed: {
    searchableEntries() {
      const docEntries = this.siteMeta.docLinks.map(item => ({
        ...item,
        kind: 'README / 外部文档',
        text: `${item.title} ${item.description}`.toLowerCase()
      }))

      const moduleEntries = allModules.map(item => ({
        title: item.title,
        description: `${item.summary} 路由：${item.route}`,
        to: item.route,
        kind: '官网模块页',
        text: `${item.title} ${item.summary} ${item.route} ${(item.tags || []).join(' ')}`.toLowerCase()
      }))

      return [...docEntries, ...moduleEntries]
    },
    filteredEntries() {
      const keyword = this.keyword.toLowerCase()
      if (!keyword) {
        return this.searchableEntries
      }
      return this.searchableEntries.filter(item => item.text.includes(keyword))
    }
  }
}
</script>

<style scoped lang="less">
.docs-hero {
  padding: 58px 0 36px;
  background: linear-gradient(180deg, rgba(232, 240, 255, 0.82), rgba(245, 247, 251, 0.96));
}

.hero-eyebrow {
  display: inline-flex;
  padding: 6px 12px;
  border-radius: 999px;
  background: var(--primary-soft);
  color: var(--primary);
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 16px;
}

.hero-copy {
  max-width: 980px;
  color: var(--text-secondary);
  font-size: 17px;
}

.page-section {
  padding-top: 48px;
}

.docs-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(320px, 0.65fr);
  gap: 28px;
}

.search-panel {
  padding: 18px;
  margin-bottom: 18px;
}

.search-meta {
  margin-top: 10px;
  color: var(--text-muted);
  font-size: 13px;
}

.doc-grid {
  display: grid;
  gap: 16px;
}

.doc-item {
  display: block;
  padding: 20px;

  strong {
    display: block;
    font-size: 18px;
    margin-bottom: 8px;
  }

  p {
    color: var(--text-secondary);
    margin-top: 8px;
  }
}

.doc-kind {
  display: inline-flex;
  padding: 4px 10px;
  border-radius: 999px;
  background: #edf3ff;
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 600;
}

.reading-card {
  padding: 24px;
  align-self: start;
  position: sticky;
  top: 148px;

  h3 {
    font-size: 22px;
    margin-bottom: 16px;
  }

  ol {
    display: grid;
    gap: 12px;
    padding-left: 20px;
    list-style: decimal;
    color: var(--text-secondary);
  }
}

@media (max-width: 960px) {
  .docs-layout {
    grid-template-columns: 1fr;
  }

  .reading-card {
    position: static;
  }
}
</style>
