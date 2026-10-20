import Vue from 'vue'
import Router from 'vue-router'
import Layout from '@/layout/index'
import Login from '@/components/Login'
import Dashboard from '@/views/Dashboard'
import PluginMana from '@/views/PluginMana'
import ExtensionMana from '@/views/ExtensionMana'
import UpdateMana from '@/views/UpdateMana'
import SelfUpgrade from '@/views/SelfUpgrade'

Vue.use(Router)

export const router = new Router({
  mode: 'hash',
  routes: [
    {
      path: '/',
      name: '登录',
      component: Login,
      hidden: true
    },
    {
      path: '/home',
      component: Layout,
      redirect: '/dashboard',
      children: [
        {
          path: '/dashboard',
          component: Dashboard,
          name: '监控总览',
          meta: { title: '监控总览', icon: 'el-icon-s-home' }
        },
        {
          path: '/plugin',
          component: PluginMana,
          name: '插件管理',
          meta: { title: '插件管理', icon: 'el-icon-s-operation' }
        },
        {
          path: '/extension',
          component: ExtensionMana,
          name: '扩展管理',
          meta: { title: '扩展管理', icon: 'el-icon-s-platform' }
        },
        {
          path: '/update',
          component: UpdateMana,
          name: '插件更新',
          meta: { title: '插件更新', icon: 'el-icon-refresh' }
        },
        {
          path: '/self-upgrade',
          component: SelfUpgrade,
          name: '系统升级',
          meta: { title: '系统升级', icon: 'el-icon-upload2' }
        }
      ]
    }
  ]
})

// 免登录白名单
function inWhiteList(toPath) {
  const whiteList = ['/', '/login', '/404']
  return whiteList.indexOf(toPath) !== -1
}

router.beforeEach((to, from, next) => {
  const token = sessionStorage.getItem('token')
  if (inWhiteList(to.path)) {
    next()
  } else if (token) {
    next()
  } else {
    next('/')
  }
})

export default router
