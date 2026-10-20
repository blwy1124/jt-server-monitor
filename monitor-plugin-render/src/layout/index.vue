<template>
  <el-container class="app-layout">
    <!-- 侧边栏 -->
    <el-aside :width="isCollapse ? '64px' : '210px'" class="app-aside">
      <sidebar :collapse="isCollapse" />
    </el-aside>

    <el-container class="app-body">
      <!-- 顶栏 -->
      <el-header class="app-header" height="56px">
        <navbar :collapse="isCollapse" @toggle-collapse="toggleCollapse" />
      </el-header>

      <!-- 内容区 -->
      <el-main class="app-main">
        <transition name="fade" mode="out-in">
          <keep-alive>
            <router-view></router-view>
          </keep-alive>
        </transition>
      </el-main>

      <el-footer class="app-footer" height="36px">
        jt-server-monitor 服务监控管理控制台 · v1.0.0
      </el-footer>
    </el-container>
  </el-container>
</template>

<script>
import Sidebar from './Sidebar'
import Navbar from './Navbar'

export default {
  name: 'Layout',
  components: { Sidebar, Navbar },
  data() {
    return {
      isCollapse: false
    }
  },
  methods: {
    toggleCollapse() {
      this.isCollapse = !this.isCollapse
    }
  }
}
</script>

<style lang="scss" scoped>
.app-layout {
  height: 100vh;
  width: 100%;
}

.app-aside {
  background: var(--sidebar-bg);
  transition: width 0.28s ease;
  overflow-x: hidden;
}

.app-body {
  height: 100%;
}

.app-header {
  padding: 0;
  border-bottom: 1px solid var(--border-color);
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  z-index: 10;
}

.app-main {
  background: var(--bg-page);
  padding: 0;
  overflow-y: auto;
}

.app-footer {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: var(--text-secondary);
  background: var(--bg-page);
}
</style>
