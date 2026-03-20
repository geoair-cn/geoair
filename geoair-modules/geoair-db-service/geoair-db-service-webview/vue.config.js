module.exports = {
    publicPath: "./",
    devServer: {
        // proxy: "http://127.0.0.1:6106/one-map-server", //开发环境的跨域问题解决
        proxy: "http://localhost:39222/demo", //开发环境的跨域问题解决
        port: 8521
    }
}
