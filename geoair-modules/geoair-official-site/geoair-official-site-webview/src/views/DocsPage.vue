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
            eyebrow="核心文档"
            title="建议阅读顺序"
            description="先总览，再看标准基础库与业务模块，最后按场景进入具体模块。"
          />
          <div class="doc-grid">
            <a v-for="item in siteMeta.docLinks" :key="item.href" class="doc-item surface-card" :href="item.href" target="_blank" rel="noreferrer">
              <strong>{{ item.title }}</strong>
              <p>{{ item.description }}</p>
            </a>
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

export default {
  name: 'DocsPage',
  components: {
    SectionIntro
  },
  data() {
    return {
      siteMeta
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
  max-width: 820px;
  color: var(--text-secondary);
  font-size: 17px;
}

.page-section {
  padding-top: 48px;
}

.docs-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(300px, 0.8fr);
  gap: 24px;
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
  }
}

.reading-card {
  padding: 24px;
  align-self: start;

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
}
</style>
