import Vue from 'vue'
import VueRouter from 'vue-router'
import { allModules } from '@/content/modules'

Vue.use(VueRouter)

const HomePage = () => import('@/views/HomePage.vue')
const SectionPage = () => import('@/views/SectionPage.vue')
const ModulePage = () => import('@/views/ModulePage.vue')
const DocsPage = () => import('@/views/DocsPage.vue')
const NotFoundPage = () => import('@/views/NotFoundPage.vue')

const routes = [
  {
    path: '/',
    name: 'home',
    component: HomePage,
    meta: { title: 'GeoAir Framework' }
  },
  {
    path: '/docs',
    name: 'docs',
    component: DocsPage,
    meta: { title: '文档索引' }
  },
  {
    path: '/standard',
    name: 'standard',
    component: SectionPage,
    props: { sectionKey: 'standard' },
    meta: { title: '标准基础库' }
  },
  {
    path: '/modules',
    name: 'modules',
    component: SectionPage,
    props: { sectionKey: 'business' },
    meta: { title: '业务模块' }
  },
  {
    path: '/modules/geo',
    name: 'geo',
    component: SectionPage,
    props: { sectionKey: 'geo' },
    meta: { title: 'geoair-geo' }
  },
  ...allModules.filter(item => item.route !== '/modules/geo').map(item => ({
    path: item.route,
    name: item.slug,
    component: ModulePage,
    props: route => ({ slug: item.slug, sectionKey: item.group, routePath: route.path }),
    meta: { title: item.title }
  })),
  {
    path: '*',
    name: 'not-found',
    component: NotFoundPage,
    meta: { title: '页面不存在' }
  }
]

const router = new VueRouter({
  mode: 'hash',
  routes,
  scrollBehavior() {
    return { x: 0, y: 0 }
  }
})

router.beforeEach((to, from, next) => {
  const title = to.meta && to.meta.title ? `${to.meta.title} · GeoAir Framework` : 'GeoAir Framework'
  document.title = title
  next()
})

export default router
