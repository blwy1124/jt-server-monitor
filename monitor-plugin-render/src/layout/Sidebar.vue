<template>
  <div class="sidebar">
    <div class="sidebar-logo">
      <i class="el-icon-s-platform logo-icon"></i>
      <span v-show="!collapse" class="logo-text">监控管理台</span>
    </div>

    <el-menu
      :default-active="activeMenu"
      :collapse="collapse"
      :collapse-transition="false"
      background-color="#263445"
      text-color="#bfcbd9"
      active-text-color="#409EFF"
      router
      class="sidebar-menu">
      <el-menu-item
        v-for="item in menus"
        :key="item.path"
        :index="item.path">
        <i :class="item.icon"></i>
        <span slot="title">{{ item.title }}</span>
      </el-menu-item>
    </el-menu>
  </div>
</template>

<script>
export default {
  name: 'Sidebar',
  props: {
    collapse: {
      type: Boolean,
      default: false
    }
  },
  computed: {
    menus() {
      const layoutRoute = this.$router.options.routes.find(r => r.path === '/home')
      if (!layoutRoute || !layoutRoute.children) return []
      return layoutRoute.children.map(c => ({
        path: c.path,
        title: c.meta && c.meta.title ? c.meta.title : c.name,
        icon: c.meta && c.meta.icon ? c.meta.icon : 'el-icon-menu'
      }))
    },
    activeMenu() {
      return this.$route.path
    }
  }
}
</script>

<style lang="scss" scoped>
.sidebar {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.sidebar-logo {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  background: #22303f;
  overflow: hidden;
  white-space: nowrap;
}

.logo-icon {
  font-size: 22px;
  color: #409EFF;
}

.logo-text {
  margin-left: 8px;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 1px;
}

.sidebar-menu {
  border-right: none;
  flex: 1;
}

.sidebar-menu:not(.el-menu--collapse) {
  width: 210px;
}
</style>
