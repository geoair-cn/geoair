import Vue from 'vue'
import VueRouter from 'vue-router'
import Home from '../views/Home.vue'
import Login from '../views/Login.vue'

import datasource from '../components/datasource/datasource'
import datasourceEdit from '../components/datasource/edit'
import datasourceAdd from '../components/datasource/add'
import datasourceDetail from '../components/datasource/detail'
import api from '../components/api/api'
import {validateToken} from '@/api/dsApi'
import apiAdd from '../components/api/add'
import apiEdit from '../components/api/edit'
import detail from '../components/api/detail'
import request from '../components/api/request'

Vue.use(VueRouter)

const routes = [
    {
        path: '/',
        name: 'Home',
        redirect: '/api',
        component: Home,
        children: [
            {path: '/datasource', name: 'datasource', component: datasource},
            {path: '/datasource/edit', component: datasourceEdit},
            {path: '/datasource/detail', component: datasourceDetail},
            {path: '/datasource/add', component: datasourceAdd},
            {path: '/api', name: 'api', component: api},
            {path: '/api/add', name: 'apiAdd', component: apiAdd},
            {path: '/api/edit', name: 'apiEdit', component: apiEdit},
            {path: '/api/detail', name: 'detail', component: detail},
            {path: '/api/request', name: 'request', component: request}
        ]
    },
    // 免登录的requestApi路由
    {
        path: '/requestApi',
        name: 'directRequest',
        component: request
    },
    // 登录路由
    {
        path: '/login',
        name: 'Login',
        component: Login
    }
]

const router = new VueRouter({
    mode: 'hash',
    routes
})

// 1. 定义免登录白名单（无需校验登录的路由）
const whiteList = ['/login', '/requestApi']

// 防止重复请求token校验（防抖）
let isCheckingToken = false
// 全局路由守卫：拦截路由跳转，校验登录状态
router.beforeEach(async (to, from, next) => {
    // 1. 白名单路由直接放行
    if (whiteList.includes(to.path)) {
        next()
        return
    }

    // 2. 非白名单路由：先获取本地token
    const token = localStorage.getItem('dsToken')

    // 3. 本地无token：直接跳登录页
    if (!token) {
        next({
            path: '/login',
            query: {redirect: to.fullPath}
        })
        return
    }

    try {
        // 防止重复请求（比如快速切换路由）
        if (isCheckingToken) return
        isCheckingToken = true

        // 调用校验token接口
        const response = await validateToken(token)


        if (response.data.success) {
            // token有效：放行
            next()
        } else {
            // token无效：清除本地token，跳登录页
            localStorage.removeItem('token')
            localStorage.removeItem('username')
            Vue.prototype.$message.error('Token已失效，请重新登录')
            next({
                path: '/login',
                query: {redirect: to.fullPath}
            })
        }
    } catch (error) {
        // 接口请求异常（网络错误/500等）：兜底处理
        console.error('Token校验接口请求失败：', error)
        Vue.prototype.$message.error('登录状态校验失败，请重新登录')
        localStorage.removeItem('token')
        localStorage.removeItem('username')
        localStorage.removeItem('userId')
        next({
            path: '/login',
            query: {redirect: to.fullPath}
        })
    } finally {
        // 无论成功失败，重置请求状态
        isCheckingToken = false
    }
})


export default router
