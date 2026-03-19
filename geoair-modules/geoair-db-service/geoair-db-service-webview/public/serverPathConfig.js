// serverPathConfig.js
const _basePath = "http://127.0.0.1:39222/demo";
window.Config = {
    baseUrl: _basePath,
    loginPage: ""
};

// 同步加载动态配置（阻塞式）
function loadDynamicConfig() {
    // 创建同步XHR对象
    const xhr = new XMLHttpRequest();
    try {
        // 1. 配置请求：GET方法、接口地址、false（同步）
        xhr.open('GET', '../ds_api/system/context', false); // false = 同步请求
        // 2. 发送请求（这里会阻塞，直到请求完成/失败）
        xhr.send();

        // 3. 处理响应
        if (xhr.status === 200) { // 请求成功（200）
            const configData = JSON.parse(xhr.responseText);
            if (configData.baseUrl) {
                window.Config.baseUrl = configData.baseUrl;
                console.log('动态配置加载成功：', window.Config.baseUrl);
            }
            if (configData.loginPage) {
                window.Config.loginPage = configData.loginPage;
            }
        } else if (xhr.status === 404) { // 404 用默认值
            console.warn('配置接口404，使用默认配置：', _basePath);
        } else { // 其他错误（500/403等）用默认值
            console.warn(`配置接口请求失败（状态码：${xhr.status}），使用默认配置`);
        }
    } catch (err) { // 网络错误/解析错误等，用默认值
        console.warn('加载动态配置异常，使用默认配置：', err);
    }
}

// 执行同步加载（这里会阻塞，直到配置加载完成/失败）
loadDynamicConfig();
