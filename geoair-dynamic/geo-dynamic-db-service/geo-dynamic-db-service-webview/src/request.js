import axios from "axios";
import {CONTENT_TYPE} from "@/constant";
import qs from "qs";

// 从Cookie中获取指定名称的值
function getCookie(name) {
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    if (parts.length === 2) {
        return parts.pop().split(';').shift();
    }
    return null;
}

axios.defaults.headers["Content-Type"] = "application/json;charset=utf-8";

// 创建axios实例
const service = axios.create({
    baseURL: window.Config.baseUrl, // process.env.VUE_APP_BASE_API,
    timeout: 50000,
});

service.defaults.headers = {'Content-Type': CONTENT_TYPE.FORM_URLENCODED}

// 请求拦截器
service.interceptors.request.use(config => {
    // 处理post请求参数序列化
    if (config.method === 'post' && config.headers['Content-Type'] === CONTENT_TYPE.FORM_URLENCODED) {
        config.data = qs.stringify(config.data, {indices: false});
    }


    return config;
})

// 响应拦截器
// 响应拦截器
service.interceptors.response.use(response => {
    return response;
}, error => {
    // 处理401未授权错误
    if (error.response && error.response.status === 401) {
        // 检查window.Config中是否有指定的跳转页面
        if (window.Config && window.Config.loginPage) {
            // 跳转到配置的页面
            window.location.href = window.Config.loginPage;
        } else {
            console.warn('未配置401跳转页面，请检查window.Config.loginPage');
        }
    }
    return Promise.reject(error);
})

export default service;
