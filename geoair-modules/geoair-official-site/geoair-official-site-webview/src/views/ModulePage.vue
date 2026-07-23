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

    <section class="page-section">
      <div class="section-block module-grid">
        <div>
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

          <SectionIntro
            eyebrow="快速接入"
            title="最短使用路径"
            description="先明确依赖和入口，再逐步深入到具体实现。"
          />
          <CodeBlock :title="`${moduleItem.title} 接入示例`" :code="moduleItem.quickStart" />
        </div>

        <div>
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
    </section>
  </div>
</template>

<script>
import SectionIntro from '@/components/SectionIntro.vue'
import CodeBlock from '@/components/CodeBlock.vue'
import { getModuleBySlug, getSectionByKey } from '@/content/modules'

export default {
  name: 'ModulePage',
  components: {
    SectionIntro,
    CodeBlock
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
  computed: {
    moduleItem() {
      return getModuleBySlug(this.slug)
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
    }
  }
}
</script>

<style scoped lang="less">
.module-hero {
  padding: 58px 0 36px;
  background: linear-gradient(180deg, rgba(232, 240, 255, 0.85), rgba(245, 247, 251, 0.96));
}

.hero-layout,
.module-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(300px, 0.85fr);
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

.page-section {
  padding-top: 48px;
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

.related-list {
  display: grid;
  gap: 16px;
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
