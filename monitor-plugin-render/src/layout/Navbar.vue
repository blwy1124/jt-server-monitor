<template>
  <div class="navbar">
    <div class="navbar-left">
      <i
        :class="collapse ? 'el-icon-s-unfold' : 'el-icon-s-fold'"
        class="collapse-btn"
        @click="$emit('toggle-collapse')"></i>
      <breadcrumb />
    </div>

    <div class="navbar-right">
      <el-tooltip content="刷新当前页" placement="bottom">
        <i class="nav-icon el-icon-refresh" @click="reload"></i>
      </el-tooltip>

      <el-popover placement="bottom" width="280" trigger="click">
        <div class="alert-panel">
          <div class="alert-panel__head">
            <span>系统告警</span>
            <el-button type="text" size="mini" @click="reportAlert">上报心跳告警</el-button>
          </div>
          <div class="alert-panel__body">
            <p v-if="alerts.length === 0" class="alert-empty">暂无告警记录</p>
            <ul v-else class="alert-list">
              <li v-for="(a, i) in alerts" :key="i">
                <i class="el-icon-warning-outline"></i>
                <span>{{ a }}</span>
              </li>
            </ul>
          </div>
        </div>
        <i slot="reference" class="nav-icon el-icon-bell">
          <span v-if="alerts.length" class="alert-dot"></span>
        </i>
      </el-popover>

      <el-dropdown class="user-dropdown" @command="handleCommand">
        <span class="user-info">
          <i class="el-icon-user-solid"></i>
          {{ currentUser || '未登录' }}
          <i class="el-icon-arrow-down"></i>
        </span>
        <el-dropdown-menu slot="dropdown">
          <el-dropdown-item command="profile">个人信息</el-dropdown-item>
          <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
        </el-dropdown-menu>
      </el-dropdown>
    </div>
  </div>
</template>

<script>
import Breadcrumb from './Breadcrumb'
import { authApi } from '@/api'

export default {
  name: 'Navbar',
  components: { Breadcrumb },
  props: {
    collapse: { type: Boolean, default: false }
  },
  data() {
    return {
      currentUser: '',
      alerts: []
    }
  },
  mounted() {
    const token = sessionStorage.getItem('token')
    if (token) {
      authApi.currentUserName(token).then(resp => {
        if (resp.data && resp.data.data) {
          this.currentUser = resp.data.data.username
        }
      }).catch(() => {})
    } else {
      this.currentUser = sessionStorage.getItem('user') || ''
    }
  },
  methods: {
    reload() {
      window.location.reload()
    },
    reportAlert() {
      authApi.alert({ source: 'navbar', time: Date.now() }).then(() => {
        this.alerts.unshift('已上报一条心跳告警 · ' + this.now())
      }).catch(() => {
        this.alerts.unshift('告警上报失败（后端未实现） · ' + this.now())
      })
    },
    now() {
      const d = new Date()
      const pad = n => (n < 10 ? '0' + n : n)
      return `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
    },
    handleCommand(command) {
      if (command === 'logout') {
        this.$confirm('确定退出登录吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }).then(() => {
          sessionStorage.removeItem('token')
          sessionStorage.removeItem('user')
          this.$router.replace('/')
        }).catch(() => {})
      } else if (command === 'profile') {
        this.$alert('当前登录用户：' + (this.currentUser || '未知'), '个人信息', {
          confirmButtonText: '关闭'
        })
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.navbar {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
}

.navbar-left {
  display: flex;
  align-items: center;
}

.collapse-btn {
  font-size: 20px;
  cursor: pointer;
  color: var(--text-regular);
  margin-right: 16px;
}

.collapse-btn:hover {
  color: var(--primary);
}

.navbar-right {
  display: flex;
  align-items: center;
}

.nav-icon {
  font-size: 18px;
  color: var(--text-regular);
  cursor: pointer;
  margin: 0 12px;
  position: relative;
}

.nav-icon:hover {
  color: var(--primary);
}

.alert-dot {
  position: absolute;
  top: -2px;
  right: -4px;
  width: 8px;
  height: 8px;
  background: var(--danger);
  border-radius: 50%;
}

.user-dropdown {
  cursor: pointer;
}

.user-info {
  display: flex;
  align-items: center;
  font-size: 14px;
  color: var(--text-regular);
}

.user-info i {
  margin: 0 4px;
}

.alert-panel__head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  border-bottom: 1px solid var(--border-color);
  padding-bottom: 6px;
}

.alert-panel__body {
  max-height: 220px;
  overflow-y: auto;
}

.alert-empty {
  text-align: center;
  color: var(--text-secondary);
  font-size: 13px;
  padding: 16px 0;
}

.alert-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.alert-list li {
  font-size: 13px;
  color: var(--text-regular);
  padding: 6px 0;
  border-bottom: 1px dashed #eee;
}

.alert-list li i {
  color: var(--warning);
  margin-right: 6px;
}
</style>
