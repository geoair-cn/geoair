import Vue from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'
import './plugins/element.js'

import i18n from './i18n/i18n'

import './theme/index.css'
import './icon/iconfont.css'

// import VueCodeMirror from 'vue-codemirror'
import 'codemirror/lib/codemirror.css'

import install from '@/components/common/index.js'
import VueClipboard from 'vue-clipboard2'


import moment from 'moment';

import {CONTENT_TYPE} from "@/constant";

import 'echarts';
import ECharts from 'vue-echarts';

Vue.component('v-chart', ECharts);

Vue.use(VueClipboard)
// Vue.use(VueCodeMirror)
Vue.use(install) // 导入模块
moment.locale('zh-cn'); // 设置语言 或 moment.lang('zh-cn');
Vue.prototype.$moment = moment;// 赋值使用

Vue.config.productionTip = false

// 使用vue-axios，这样才可以全局使用this.axios调用



// //过滤器
Vue.filter('dateFormat', function (originVal) {
    const dt = new Date(originVal * 1000)
    return moment(dt).format('YYYY-MM-DD HH:mm:ss')
})


new Vue({
    router,
    i18n,
    store,
    render: h => h(App)
}).$mount('#app')
