<template>
  <div class="page-container not-found-page">
    <section class="not-found-wrap">
      <div class="section-block center-box surface-card">
        <span class="code">404</span>
        <h1>页面不存在</h1>
        <p>当前访问的路由没有对应页面，请返回首页或继续浏览 GeoAir 的模块目录。</p>
        <div class="actions">
          <router-link to="/" class="go-home">返回首页</router-link>
          <router-link to="/modules" class="go-outline">查看业务模块</router-link>
        </div>
      </div>
    </section>
  </div>
</template>

<script>
export default {
  name: 'NotFoundPage'
}
</script>

<style scoped lang="less">
.not-found-wrap {
  padding: 84px 0;
}

.center-box {
  max-width: 760px;
  margin: 0 auto;
  padding: 48px 32px;
  text-align: center;
}

.code {
  display: inline-flex;
  margin-bottom: 18px;
  font-size: 14px;
  font-weight: 700;
  color: var(--primary);
  background: var(--primary-soft);
  border-radius: 999px;
  padding: 6px 12px;
}

h1 {
  font-size: 36px;
  margin-bottom: 16px;
}

p {
  color: var(--text-secondary);
}

.actions {
  margin-top: 24px;
  display: flex;
  justify-content: center;
  gap: 12px;
  flex-wrap: wrap;
}

.go-home,
.go-outline {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 12px 18px;
  border-radius: 10px;
}

.go-home {
  background: #0f172a;
  color: #fff;
}

.go-outline {
  background: #fff;
  color: var(--primary);
  border: 1px solid var(--border-color);
}
</style>
