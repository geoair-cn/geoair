<template>
  <div class="page-container">
    <section class="section-hero">
      <div class="section-block hero-inner">
        <span class="hero-eyebrow">模块分组</span>
        <h1 class="page-title">{{ section.title }}</h1>
        <p class="hero-copy">{{ section.description }}</p>
      </div>
    </section>

    <section class="page-section">
      <div class="section-block">
        <SectionIntro
          eyebrow="模块列表"
          :title="sectionTitle"
          :description="sectionDescription"
        />
        <div class="grid-columns">
          <ModuleCard
            v-for="item in section.modules"
            :key="item.slug"
            :item="item"
            :badge-text="badgeText"
          />
        </div>
      </div>
    </section>
  </div>
</template>

<script>
import SectionIntro from '@/components/SectionIntro.vue'
import ModuleCard from '@/components/ModuleCard.vue'
import { getSectionByKey } from '@/content/modules'

export default {
  name: 'SectionPage',
  components: {
    SectionIntro,
    ModuleCard
  },
  props: {
    sectionKey: {
      type: String,
      required: true
    }
  },
  computed: {
    section() {
      return getSectionByKey(this.sectionKey) || { title: '未知分组', description: '', modules: [] }
    },
    sectionTitle() {
      if (this.sectionKey === 'geo') {
        return '围绕 geoair-geo 展开的 GIS 子模块'
      }
      if (this.sectionKey === 'business') {
        return '可直接接入项目的业务能力模块'
      }
      return 'GeoAir 的标准基础层与工程管理入口'
    },
    sectionDescription() {
      if (this.sectionKey === 'geo') {
        return '每个子模块都是独立路由，便于逐个查看适用场景、核心能力和接入方式。'
      }
      if (this.sectionKey === 'business') {
        return '面向 API 文档、代码生成、数据库服务、动态数据源和 GIS 能力等实际场景。'
      }
      return '从依赖管理到抽象接口、SPI 实现与 Web / ORM 能力，构成 GeoAir 的整体底座。'
    },
    badgeText() {
      if (this.sectionKey === 'geo') {
        return 'GIS'
      }
      if (this.sectionKey === 'business') {
        return '业务'
      }
      return '基础层'
    }
  }
}
</script>

<style scoped lang="less">
.section-hero {
  padding: 58px 0 36px;
  background: linear-gradient(180deg, rgba(232, 240, 255, 0.8), rgba(245, 247, 251, 0.95));
}

.hero-inner {
  text-align: left;
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

.page-section {
  padding-top: 48px;
}
</style>
