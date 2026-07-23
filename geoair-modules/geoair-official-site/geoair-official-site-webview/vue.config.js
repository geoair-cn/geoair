const path = require('path')

module.exports = {
  publicPath: './',
  devServer: {
    port: 8522
  },
  chainWebpack(config) {
    config.module
      .rule('markdown')
      .test(/\.md$/)
      .use('geoair-markdown-loader')
      .loader(path.resolve(__dirname, 'build/markdown-loader.js'))
  }
}
