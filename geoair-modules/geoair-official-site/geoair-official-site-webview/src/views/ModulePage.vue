<template>
  <div v-if="moduleItem" class="page-container module-page">
    <section class="module-hero">
      <div class="section-block hero-layout">
        <div>
          <span class="hero-eyebrow">{{ badgeText }}</span>
          <h1 class="page-title">{{ moduleItem.title }}</h1>
          <p class="hero-copy">{{ moduleItem.summary }}</p>
          <div class="hero-tags">
            <el-tag v-for="tag in moduleItem.tags || []" :key="tag" size="small" effect="plain">{{ tag }}</el-tag>
          </div>
        </div>
        <div class="hero-aside surface-card">
          <h3>适合什么场景</h3>
          <p>{{ moduleItem.example }}</p>
          <div class="hero-links">
            <router-link v-if="sectionLink" :to="sectionLink.route">返回{{ sectionLink.title }}</router-link>
            <router-link v-if="relatedRoutes.length" :to="relatedRoutes[0].route">查看相关模块</router-link>
          </div>
        </div>
      </div>
    </section>

    <nav class="module-nav" aria-label="当前模块导航">
      <div class="section-block module-nav-inner">
        <span class="module-nav-label">{{ moduleItem.title }}</span>
        <div class="module-nav-items">
          <button v-for="item in navItems" :key="item.id" type="button" @click="scrollToSection(item.id)">
            {{ item.label }}
          </button>
        </div>
      </div>
    </nav>

    <section class="page-section">
      <div class="section-block module-page-layout" :class="{ 'has-md': !!moduleDoc }">
        <aside v-if="moduleDoc && navItems.length" class="left-sidebar surface-card">
          <div class="sidebar-title">当前页面</div>
          <button v-for="item in navItems" :key="item.id" type="button" class="sidebar-link" @click="scrollToSection(item.id)">
            {{ item.label }}
          </button>
        </aside>

        <div class="module-main">
          <div class="module-grid">
            <div>
              <template v-if="moduleDoc">
                <div id="doc-body">
                  <MarkdownArticle :document="moduleDoc" />
                </div>
              </template>

              <template v-else>
            <div id="capabilities">
              <SectionIntro
                eyebrow="核心能力"
                :title="`${moduleItem.title} 能解决什么问题`"
                description="以官网化方式提炼出能力边界，帮助快速判断该模块是否适合当前项目。"
              />
              <div class="surface-card capability-card">
                <ul>
                  <li v-for="item in moduleItem.capabilities || []" :key="item">{{ item }}</li>
                </ul>
              </div>
            </div>

            <div id="quick-start">
              <SectionIntro
                eyebrow="快速接入"
                title="最短使用路径"
                description="先明确依赖和入口，再逐步深入到具体实现。"
              />
              <CodeBlock :title="`${moduleItem.title} 接入示例`" :code="moduleItem.quickStart" />
            </div>

            <template v-if="detailSections.length">
              <div id="details">
                <SectionIntro
                  eyebrow="深度说明"
                  title="模块拆解与使用建议"
                  description="针对该模块的职责边界、使用方式和常见落地场景做更细化说明。"
                />
                <div class="detail-sections">
                  <article v-for="section in detailSections" :key="section.title" class="surface-card detail-card">
                    <h3>{{ section.title }}</h3>
                    <ul>
                      <li v-for="item in section.items" :key="item">{{ item }}</li>
                    </ul>
                  </article>
                </div>
              </div>
            </template>

            <template v-if="usageExamples.length">
              <div id="examples">
                <SectionIntro
                  eyebrow="示例"
                  title="更多可复制的查询片段"
                  description="这些示例覆盖基础检索、空间范围查询、组合条件、分页与动态数据源等典型场景。"
                />
                <div class="example-list">
                  <article v-for="example in usageExamples" :key="example.title" class="example-item">
                    <h3>{{ example.title }}</h3>
                    <p>{{ example.description }}</p>
                    <CodeBlock :title="example.title" :code="example.code" />
                  </article>
                </div>
              </div>
            </template>
          </template>
        </div>

        <div>
          <template v-if="sourceExamples.length">
            <div id="sources">
              <SectionIntro
                eyebrow="源码示例"
                title="直接对应的 test / 主源码入口"
                description="如果你要继续深入，不必只看官网片段，直接从这些类开始读会更快。"
              />
              <div class="source-list">
                <article v-for="item in sourceExamples" :key="`${item.title}-${item.path}`" class="source-item surface-card">
                  <strong>{{ item.title }}</strong>
                  <code>{{ item.path }}</code>
                  <p>{{ item.description }}</p>
                </article>
              </div>
            </div>
          </template>

          <div id="related">
            <SectionIntro
              eyebrow="关联模块"
              title="推荐一起了解"
              description="GeoAir 的模块之间存在天然协作关系，按关联模块继续阅读能更快建立全局认识。"
            />
            <div v-if="relatedRoutes.length" class="related-list">
              <router-link v-for="item in relatedRoutes" :key="item.slug" class="related-item surface-card" :to="item.route">
                <strong>{{ item.title }}</strong>
                <span>{{ item.summary }}</span>
              </router-link>
            </div>
            <el-empty v-else description="暂无更多关联模块" :image-size="90" />

            <SectionIntro
              v-if="childModules.length"
              eyebrow="子模块"
              title="继续查看下一级能力"
              description="该模块下还有更细的能力拆分，可以逐个进入独立路由查看。"
            />
            <div v-if="childModules.length" class="related-list">
              <router-link v-for="item in childModules" :key="item.slug" class="related-item surface-card" :to="item.route">
                <strong>{{ item.title }}</strong>
                <span>{{ item.summary }}</span>
              </router-link>
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script>
import SectionIntro from '@/components/SectionIntro.vue'
import CodeBlock from '@/components/CodeBlock.vue'
import MarkdownArticle from '@/components/module/MarkdownArticle.vue'
import { getModuleBySlug, getSectionByKey } from '@/content/modules'
import { getModuleDoc } from '@/content/module-docs'

export default {
  name: 'ModulePage',
  components: {
    SectionIntro,
    CodeBlock,
    MarkdownArticle
  },
  props: {
    slug: {
      type: String,
      required: true
    },
    sectionKey: {
      type: String,
      default: ''
    }
  },
  methods: {
    scrollToSection(id) {
      const element = document.getElementById(id)
      if (!element) {
        return
      }
      const offset = 116
      const top = element.getBoundingClientRect().top + window.pageYOffset - offset
      window.scrollTo({ top, behavior: 'smooth' })
    }
  },
  computed: {
    moduleItem() {
      return getModuleBySlug(this.slug)
    },
    moduleDoc() {
      return getModuleDoc(this.slug)
    },
    navItems() {
      const docItems = this.moduleDoc && this.moduleDoc.toc
        ? this.moduleDoc.toc.map(item => ({ id: item.id, label: item.title }))
        : []
      if (docItems.length) {
        return [
          ...docItems,
          ...(this.sourceExamples.length ? [{ id: 'sources', label: '源码示例' }] : []),
          { id: 'related', label: '关联模块' }
        ]
      }
      return [
        { id: 'capabilities', label: '核心能力' },
        { id: 'quick-start', label: '快速接入' },
        ...(this.detailSections.length ? [{ id: 'details', label: '深度说明' }] : []),
        ...(this.usageExamples.length ? [{ id: 'examples', label: '示例' }] : []),
        ...(this.sourceExamples.length ? [{ id: 'sources', label: '源码示例' }] : []),
        { id: 'related', label: '关联模块' }
      ]
    },
    sectionLink() {
      return getSectionByKey(this.sectionKey)
    },
    badgeText() {
      if (this.sectionKey === 'geo') {
        return 'GIS 子模块'
      }
      if (this.sectionKey === 'business') {
        return '业务模块'
      }
      return '基础模块'
    },
    relatedRoutes() {
      const related = (this.moduleItem && this.moduleItem.related) || []
      return related
        .map(slug => getModuleBySlug(slug))
        .filter(Boolean)
    },
    childModules() {
      const children = (this.moduleItem && this.moduleItem.children) || []
      return children
        .map(slug => getModuleBySlug(slug))
        .filter(Boolean)
    },
    detailSections() {
      return (this.moduleItem && this.moduleItem.detailSections) || []
    },
    usageExamples() {
      return (this.moduleItem && this.moduleItem.usageExamples) || []
    },
    sourceExamples() {
      return (this.moduleItem && this.moduleItem.sourceExamples) || []
    }
  }
}
</script>

<style scoped lang="less">
.module-hero {
  padding: 58px 0 36px;
  background: linear-gradient(180deg, rgba(232, 240, 255, 0.85), rgba(245, 247, 251, 0.96));
}

.hero-layout {
  display: grid;
  grid-template-columns: minmax(0, 4fr) minmax(220px, 1fr);
  gap: 24px;
}

.module-grid {
  display: grid;
  grid-template-columns: minmax(0, 4fr) minmax(220px, 1fr);
  gap: 24px;
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
  max-width: 760px;
  color: var(--text-secondary);
  font-size: 17px;
}

.hero-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 18px;
}

.hero-aside,
.capability-card,
.related-item {
  padding: 22px;
}

.hero-aside {
  align-self: start;

  h3 {
    font-size: 20px;
    margin-bottom: 10px;
  }

  p {
    color: var(--text-secondary);
  }
}

.hero-links {
  margin-top: 20px;
  display: grid;
  gap: 10px;

  a {
    color: var(--primary);
    font-weight: 600;
  }
}

.module-nav {
  position: sticky;
  top: 72px;
  z-index: 12;
  background: rgba(245, 247, 251, 0.94);
  backdrop-filter: blur(10px);
  border-top: 1px solid rgba(37, 99, 235, 0.08);
  border-bottom: 1px solid rgba(37, 99, 235, 0.1);
}

.module-nav-inner {
  display: flex;
  align-items: center;
  gap: 18px;
  min-height: 60px;
  flex-wrap: wrap;
  padding-top: 10px;
  padding-bottom: 10px;
}

.module-nav-label {
  flex-shrink: 0;
  font-weight: 700;
  color: var(--text-main);
}

.module-nav-items {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.module-nav-items button {
  border: 1px solid var(--border-color);
  background: #fff;
  color: var(--text-secondary);
  border-radius: 999px;
  padding: 8px 14px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.module-nav-items button:hover {
  color: var(--primary);
  border-color: rgba(37, 99, 235, 0.35);
  background: var(--primary-soft);
}

.page-section {
  padding-top: 36px;
}

.capability-card ul {
  display: grid;
  gap: 14px;
}

.capability-card li {
  position: relative;
  padding-left: 18px;
  color: var(--text-secondary);

  &::before {
    content: '';
    position: absolute;
    left: 0;
    top: 10px;
    width: 8px;
    height: 8px;
    border-radius: 999px;
    background: var(--primary);
  }
}

.source-list {
  display: grid;
  gap: 14px;
  margin-bottom: 32px;
}

.source-item {
  padding: 18px;
}

.source-item strong {
  display: block;
  margin-bottom: 8px;
  color: var(--text-main);
}

.source-item code {
  display: block;
  padding: 8px 10px;
  border-radius: 6px;
  background: #edf3ff;
  color: #1d4ed8;
  font-size: 12px;
  line-height: 1.5;
  overflow-wrap: anywhere;
}

.source-item p {
  margin-top: 10px;
  color: var(--text-secondary);
  font-size: 14px;
}

.related-list {
  display: grid;
  gap: 16px;
}

.detail-sections,
.example-list {
  display: grid;
  gap: 18px;
  margin-top: 22px;
}

.detail-card,
.example-item {
  padding: 22px;
}

.detail-card h3,
.example-item h3 {
  font-size: 18px;
  margin-bottom: 10px;
}

.detail-card ul {
  display: grid;
  gap: 12px;
}

.detail-card li {
  position: relative;
  padding-left: 18px;
  color: var(--text-secondary);

  &::before {
    content: '';
    position: absolute;
    left: 0;
    top: 10px;
    width: 8px;
    height: 8px;
    border-radius: 999px;
    background: var(--accent);
  }
}

.example-item {
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid var(--border-color);
  border-radius: 16px;
}

.example-item p {
  color: var(--text-secondary);
  margin-bottom: 14px;
}

.related-item {
  display: block;

  strong {
    display: block;
    font-size: 17px;
    margin-bottom: 8px;
    color: var(--text-main);
  }

  span {
    display: block;
    color: var(--text-secondary);
  }
}

@media (max-width: 960px) {
  .hero-layout,
  .module-grid {
    grid-template-columns: 1fr;
  }
}
</style>
