<template>
  <div class="markdown-article">
    <div class="markdown-body" v-html="document.html" @click="handleClick"></div>
  </div>
</template>

<script>
export default {
  name: 'MarkdownArticle',
  props: {
    document: {
      type: Object,
      required: true
    }
  },
  methods: {
    async handleClick(event) {
      const button = event.target.closest('[data-copy-code]')
      if (!button) {
        return
      }
      const block = button.closest('.markdown-code-block')
      const codeElement = block && block.querySelector('pre code')
      if (!codeElement) {
        return
      }
      const text = codeElement.innerText
      try {
        if (navigator && navigator.clipboard && navigator.clipboard.writeText) {
          await navigator.clipboard.writeText(text)
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
.markdown-article {
  width: 100%;
}
</style>
