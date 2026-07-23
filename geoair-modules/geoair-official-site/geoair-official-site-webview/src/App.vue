<template>
  <div id="app">
    <SiteShell>
      <router-view />
    </SiteShell>
    <el-backtop :right="24" :bottom="24" />
  </div>
</template>

<script>
import SiteShell from '@/layout/SiteShell.vue'

export default {
  name: 'App',
  components: {
    SiteShell
  }
}
</script>

<style lang="less">
:root {
  --page-bg: #f5f7fb;
  --surface: #ffffff;
  --text-main: #1f2937;
  --text-secondary: #4b5563;
  --text-muted: #6b7280;
  --border-color: #dbe4f0;
  --primary: #2563eb;
  --primary-soft: #e8f0ff;
  --accent: #0f766e;
  --hero-top: #0f172a;
  --hero-bottom: #1d4ed8;
  --shadow-soft: 0 14px 40px rgba(15, 23, 42, 0.08);
  --shadow-card: 0 10px 24px rgba(15, 23, 42, 0.08);
  --radius-card: 8px;
  --content-gutter: clamp(20px, 3vw, 40px);
}

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html {
  scroll-behavior: smooth;
}

body {
  font-family: 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  background: var(--page-bg);
  color: var(--text-main);
  line-height: 1.6;
}

body,
button,
input,
textarea {
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

ul,
ol {
  list-style: none;
}

a {
  color: inherit;
  text-decoration: none;
}

code,
pre {
  font-family: 'Consolas', 'Courier New', monospace;
}

#app {
  min-height: 100vh;
}

.page-container {
  width: 100%;
}

.section-block {
  width: 100%;
  max-width: none;
  margin: 0;
  padding: 0 var(--content-gutter);
}

.surface-card {
  background: var(--surface);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-card);
  box-shadow: var(--shadow-card);
}

.muted-text {
  color: var(--text-muted);
}

.page-title {
  font-size: 40px;
  line-height: 1.15;
  margin-bottom: 16px;
}

.page-subtitle {
  max-width: 760px;
  color: rgba(255, 255, 255, 0.85);
  font-size: 17px;
}

.section-title {
  font-size: 30px;
  line-height: 1.2;
  margin-bottom: 12px;
  color: var(--text-main);
}

.section-description {
  font-size: 16px;
  color: var(--text-secondary);
  max-width: 760px;
}

.markdown-body {
  color: var(--text-secondary);
  line-height: 1.85;
}

.markdown-body h2,
.markdown-body h3 {
  color: var(--text-main);
  line-height: 1.25;
  scroll-margin-top: 124px;
}

.markdown-body h2 {
  font-size: 28px;
  margin: 36px 0 14px;
}

.markdown-body h3 {
  font-size: 21px;
  margin: 28px 0 12px;
}

.markdown-body p,
.markdown-body ul,
.markdown-body ol {
  margin: 0 0 14px;
}

.markdown-body ul,
.markdown-body ol {
  padding-left: 20px;
}

.markdown-body li {
  margin-bottom: 8px;
}

.markdown-body code {
  background: rgba(37, 99, 235, 0.08);
  color: #1d4ed8;
  padding: 2px 6px;
  border-radius: 4px;
}

.markdown-body .markdown-code-block {
  margin: 18px 0 22px;
  border-radius: 16px;
  overflow: hidden;
  background: #0f172a;
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.markdown-body .markdown-code-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  color: rgba(255, 255, 255, 0.82);
}

.markdown-body .markdown-code-header button {
  border: 1px solid rgba(255, 255, 255, 0.16);
  color: #fff;
  background: transparent;
  padding: 6px 10px;
  border-radius: 8px;
  cursor: pointer;
}

.markdown-body .markdown-code-block pre {
  margin: 0;
  padding: 18px 16px;
  white-space: pre-wrap;
  color: #e5eefc;
  overflow-x: auto;
}

.markdown-body .markdown-code-block code {
  background: transparent;
  color: inherit;
  padding: 0;
}

.grid-columns {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 20px;
}

@media (max-width: 1040px) {
  .grid-columns {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .section-block {
    padding: 0 16px;
  }

  .page-title {
    font-size: 32px;
  }

  .section-title {
    font-size: 26px;
  }

  .grid-columns {
    grid-template-columns: 1fr;
  }
}
</style>
