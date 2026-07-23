<template>
  <div class="code-block surface-card">
    <div class="block-header">
      <span>{{ title }}</span>
      <button type="button" class="copy-button" @click="copyCode">复制</button>
    </div>
    <pre><code>{{ code }}</code></pre>
  </div>
</template>

<script>
export default {
  name: 'CodeBlock',
  props: {
    title: {
      type: String,
      default: '示例代码'
    },
    code: {
      type: String,
      required: true
    }
  },
  methods: {
    async copyCode() {
      try {
        if (navigator && navigator.clipboard && navigator.clipboard.writeText) {
          await navigator.clipboard.writeText(this.code)
          this.$message.success('代码已复制')
          return
        }
      } catch (error) {
      }
      this.$message.warning('当前环境不支持自动复制，请手动复制代码')
    }
  }
}
</script>

<style scoped lang="less">
.code-block {
  overflow: hidden;
  border-radius: 16px;
  background: #0f172a;
  border-color: rgba(255, 255, 255, 0.08);
}

.block-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 18px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  color: rgba(255, 255, 255, 0.86);
}

.copy-button {
  border: 1px solid rgba(255, 255, 255, 0.16);
  color: #fff;
  background: transparent;
  padding: 8px 12px;
  border-radius: 8px;
  cursor: pointer;
}

pre {
  margin: 0;
  padding: 20px 18px;
  white-space: pre-wrap;
  color: #e5eefc;
  font-size: 14px;
  line-height: 1.7;
  overflow-x: auto;
}
</style>
