<template>
  <article class="module-card surface-card">
    <div class="card-head">
      <div>
        <router-link class="title" :to="item.route">{{ item.title }}</router-link>
        <p class="summary">{{ item.summary }}</p>
      </div>
      <span class="badge">{{ badgeText }}</span>
    </div>

    <div class="tags">
      <el-tag v-for="tag in item.tags || []" :key="tag" size="mini" effect="plain">{{ tag }}</el-tag>
    </div>

    <p class="example">{{ item.example }}</p>

    <router-link class="more-link" :to="item.route">查看详情 →</router-link>
  </article>
</template>

<script>
export default {
  name: 'ModuleCard',
  props: {
    item: {
      type: Object,
      required: true
    },
    badgeText: {
      type: String,
      default: '模块'
    }
  }
}
</script>

<style scoped lang="less">
.module-card {
  padding: 22px;
  transition: transform 0.25s ease, box-shadow 0.25s ease;

  &:hover {
    transform: translateY(-3px);
    box-shadow: 0 14px 30px rgba(15, 23, 42, 0.12);
  }
}

.card-head {
  display: flex;
  justify-content: space-between;
  gap: 14px;
}

.title {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-main);
}

.summary {
  margin-top: 10px;
  color: var(--text-secondary);
}

.badge {
  flex-shrink: 0;
  height: fit-content;
  padding: 6px 10px;
  border-radius: 999px;
  background: var(--primary-soft);
  color: var(--primary);
  font-size: 12px;
  font-weight: 600;
}

.tags {
  margin-top: 16px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.example {
  margin-top: 16px;
  color: var(--text-muted);
  min-height: 48px;
}

.more-link {
  display: inline-flex;
  margin-top: 16px;
  color: var(--primary);
  font-weight: 600;
}
</style>
