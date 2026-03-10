import Vue from 'vue'
import VueRouter from 'vue-router'
import Home from '../views/Home.vue'

import datasource from '../components/datasource/datasource'
import datasourceEdit from '../components/datasource/edit'
import datasourceAdd from '../components/datasource/add'
import datasourceDetail from '../components/datasource/detail'
import api from '../components/api/api'

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
    // 添加一个直接指向request组件的顶级路由
    {
        path: '/requestApi',  // 新的直接访问路径
        name: 'directRequest',
        component: request  // 直接指向request组件
    }

]

const router = new VueRouter({
    routes
})

export default router
