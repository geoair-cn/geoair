<template>
  <div class="page-container home-page">
    <section class="top-hero">
      <div class="section-block intro-copy">
        <span class="intro-badge">开发者官网</span>
        <h1 class="page-title">{{ siteMeta.name }}</h1>
        <p class="page-subtitle">{{ siteMeta.description }}</p>
      </div>
    </section>

    <HomeHero />

    <section id="quick-start" class="page-section">
      <div class="section-block section-layout two-columns">
        <div>
          <SectionIntro
            eyebrow="快速开始"
            :title="siteMeta.quickStart.title"
            :description="siteMeta.quickStart.summary"
          />
          <CodeBlock :title="siteMeta.quickStart.title" :code="siteMeta.quickStart.code" />
        </div>
        <div>
          <SectionIntro
            eyebrow="统一入口"
            :title="siteMeta.sampleCode.title"
            description="通过 Gir 门面与 GirGeoTools，快速接入配置、日志、统一结果和 GIS 能力。"
          />
          <CodeBlock :title="siteMeta.sampleCode.title" :code="siteMeta.sampleCode.code" />
        </div>
      </div>
    </section>

    <section class="page-section alt-bg">
      <div class="section-block">
        <SectionIntro
          eyebrow="设计原则"
          title="面向企业级 GIS 的模块化底座"
          description="GeoAir 把依赖管理、标准抽象和 GIS 业务能力拆成清晰层次，适合渐进接入。"
        />

        <div class="principle-grid">
          <article v-for="item in siteMeta.principles" :key="item.title" class="principle-card surface-card">
            <h3>{{ item.title }}</h3>
            <p>{{ item.description }}</p>
          </article>
        </div>
      </div>
    </section>

    <section v-for="section in homeSections" :key="section.title" class="page-section">
      <div class="section-block">
        <SectionIntro
          eyebrow="模块总览"
          :title="section.title"
          :description="section.description"
        />
        <div class="grid-columns">
          <ModuleCard
            v-for="item in section.items"
            :key="item.slug"
            :item="item"
            :badge-text="badgeText(section.route)"
          />
        </div>
        <div class="section-actions">
          <router-link class="view-all-link" :to="section.route">查看全部 →</router-link>
        </div>
      </div>
    </section>

    <section class="page-section alt-bg">
      <div class="section-block docs-grid">
        <div>
          <SectionIntro
            eyebrow="文档入口"
            title="先读 README，再按模块深入"
            description="现有 README 已经覆盖了项目概览、模块目录、技术栈和快速开始，是官网内容的主要信息来源。"
          />
          <div class="doc-list">
            <a v-for="item in siteMeta.docLinks" :key="item.href" class="doc-card surface-card" :href="item.href" target="_blank" rel="noreferrer">
              <strong>{{ item.title }}</strong>
              <p>{{ item.description }}</p>
            </a>
          </div>
        </div>
        <div class="surface-card contact-card">
          <h3>GeoAir 信息</h3>
          <ul>
            <li><strong>组织：</strong>{{ siteMeta.footer.organization }}</li>
            <li><strong>维护：</strong>{{ siteMeta.footer.maintainer }}</li>
            <li><strong>邮箱：</strong>{{ siteMeta.footer.email }}</li>
            <li><strong>协议：</strong>{{ siteMeta.footer.license }}</li>
          </ul>
          <a class="repo-cta" :href="siteMeta.footer.repo" target="_blank" rel="noreferrer">打开 GitHub 仓库</a>
        </div>
      </div>
    </section>
  </div>
</template>

<script>
import HomeHero from '@/components/HomeHero.vue'
import SectionIntro from '@/components/SectionIntro.vue'
import ModuleCard from '@/components/ModuleCard.vue'
import CodeBlock from '@/components/CodeBlock.vue'
import { siteMeta } from '@/content/site'
import { homeSections } from '@/content/modules'

export default {
  name: 'HomePage',
  components: {
    HomeHero,
    SectionIntro,
    ModuleCard,
    CodeBlock
  },
  data() {
    return {
      siteMeta,
      homeSections
    }
  },
  methods: {
    badgeText(route) {
      if (route === '/standard') {
        return '基础层'
      }
      if (route === '/modules/geo') {
        return 'GIS'
      }
      return '业务'
    }
  }
}
</script>

<style scoped lang="less">
.top-hero {
  background: radial-gradient(circle at top left, rgba(37, 99, 235, 0.32), transparent 28%),
    linear-gradient(135deg, #0f172a 0%, #102a57 55%, #0f766e 100%);
  padding: 66px 0 108px;
  color: #fff;
}

.intro-copy {
  text-align: left;
}

.intro-badge {
  display: inline-flex;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.12);
  margin-bottom: 18px;
}

.page-section {
  padding: 72px 0 0;
}

.alt-bg {
  margin-top: 72px;
  padding: 72px 0;
  background: linear-gradient(180deg, rgba(232, 240, 255, 0.45), rgba(245, 247, 251, 0.8));
}

.section-layout.two-columns,
.docs-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 24px;
}

.principle-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 20px;
}

.principle-card {
  padding: 24px;

  h3 {
    font-size: 20px;
    margin-bottom: 10px;
  }

  p {
    color: var(--text-secondary);
  }
}

.section-actions {
  margin-top: 18px;
}

.view-all-link,
.repo-cta {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 12px 18px;
  border-radius: 10px;
  background: #0f172a;
  color: #fff;
}

.doc-list {
  display: grid;
  gap: 16px;
}

.doc-card {
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

.contact-card {
  padding: 24px;
  align-self: start;

  h3 {
    font-size: 22px;
    margin-bottom: 16px;
  }

  ul {
    display: grid;
    gap: 12px;
    color: var(--text-secondary);
    margin-bottom: 24px;
  }
}

@media (max-width: 960px) {
  .section-layout.two-columns,
  .docs-grid,
  .principle-grid {
    grid-template-columns: 1fr;
  }
}
</style>
