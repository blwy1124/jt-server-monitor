<template>
  <div class="page-wrap">
    <!-- 顶部指标卡 -->
    <el-row :gutter="16" class="metric-row">
      <el-col :xs="12" :sm="12" :md="6" v-for="m in metrics" :key="m.key">
        <div class="metric-card" :style="{ borderTopColor: m.color }">
          <div class="metric-icon" :style="{ background: m.color }">
            <i :class="m.icon"></i>
          </div>
          <div class="metric-body">
            <div class="metric-label">{{ m.label }}</div>
            <div class="metric-value">
              {{ m.value }}<span class="metric-unit">{{ m.unit }}</span>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <!-- 资源占用 -->
      <el-col :xs="24" :md="12">
        <div class="page-card">
          <div class="page-card-header">
            <span class="page-card-title">资源占用</span>
            <el-button type="text" icon="el-icon-refresh" @click="fetchInfo">刷新</el-button>
          </div>

          <div class="usage-item" v-for="u in usages" :key="u.label">
            <div class="usage-label">
              <span>{{ u.label }}</span>
              <span>{{ u.value }}%</span>
            </div>
            <el-progress
              :percentage="Number(u.value) || 0"
              :color="usageColor(u.value)"
              :stroke-width="12"
              :show-text="false"></el-progress>
          </div>
        </div>
      </el-col>

      <!-- 系统信息 -->
      <el-col :xs="24" :md="12">
        <div class="page-card">
          <div class="page-card-header">
            <span class="page-card-title">系统信息</span>
          </div>
          <el-descriptions :column="1" border size="medium">
            <el-descriptions-item label="操作系统">{{ info.system || '-' }}</el-descriptions-item>
            <el-descriptions-item label="运行时长">{{ runtimeText }}</el-descriptions-item>
            <el-descriptions-item label="插件总数">{{ pluginCount }}</el-descriptions-item>
            <el-descriptions-item label="运行环境版本">
              <template v-if="envVersions.length">
                <div v-for="(v, i) in envVersions" :key="i">
                  {{ v.environment }}：{{ v.version }}
                </div>
              </template>
              <span v-else>-</span>
            </el-descriptions-item>
          </el-descriptions>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { mainApi, pluginApi } from '@/api'

export default {
  name: 'Dashboard',
  data() {
    return {
      info: { system: '', cpu: 0, memory: 0, disk: 0, runtime: 0 },
      envVersions: [],
      pluginCount: 0,
      runtimeText: '-',
      timer: null,
      baseSeconds: 0
    }
  },
  computed: {
    metrics() {
      return [
        { key: 'cpu', label: 'CPU 使用率', value: this.info.cpu, unit: '%', color: '#337ecc', icon: 'el-icon-cpu' },
        { key: 'memory', label: '内存使用率', value: this.info.memory, unit: '%', color: '#17b3a3', icon: 'el-icon-coin' },
        { key: 'disk', label: '磁盘使用率', value: this.info.disk, unit: '%', color: '#e6a23c', icon: 'el-icon-files' },
        { key: 'plugin', label: '已加载插件', value: this.pluginCount, unit: '个', color: '#f56c6c', icon: 'el-icon-menu' }
      ]
    },
    usages() {
      return [
        { label: 'CPU', value: this.info.cpu },
        { label: '内存', value: this.info.memory },
        { label: '磁盘', value: this.info.disk }
      ]
    }
  },
  mounted() {
    this.fetchInfo()
    this.fetchPluginCount()
  },
  beforeDestroy() {
    if (this.timer) clearInterval(this.timer)
  },
  methods: {
    fetchInfo() {
      mainApi.systemInfo().then(resp => {
        const d = resp.data
        if (d && d.state) {
          const data = d.data || {}
          this.info.system = data.system
          this.info.cpu = this.round(data.cpu)
          this.info.memory = this.round(data.memory)
          this.info.disk = this.round(data.disk)
          this.envVersions = data.versionForEnvironment || []
          this.startRuntime(data.runtime || 0)
        } else {
          this.$message({ type: 'error', message: d && d.msg ? d.msg : '获取系统信息失败' })
        }
      }).catch(() => {
        this.$message({ type: 'error', message: '获取系统信息失败，请确认后端服务已启动' })
      })
    },
    fetchPluginCount() {
      pluginApi.list().then(resp => {
        const d = resp.data
        if (d && (d.state === 1 || d.state === true) && Array.isArray(d.data)) {
          this.pluginCount = d.data.length
        }
      }).catch(() => {})
    },
    round(v) {
      const n = Number(v)
      return isNaN(n) ? 0 : Math.round(n * 100) / 100
    },
    usageColor(v) {
      const n = Number(v) || 0
      if (n >= 90) return '#f56c6c'
      if (n >= 70) return '#e6a23c'
      return '#17b3a3'
    },
    startRuntime(ms) {
      this.baseSeconds = Math.floor(ms / 1000)
      this.updateRuntime()
      if (this.timer) clearInterval(this.timer)
      this.timer = setInterval(() => {
        this.baseSeconds++
        this.updateRuntime()
      }, 1000)
    },
    updateRuntime() {
      const s = this.baseSeconds
      const days = Math.floor(s / 86400)
      const hours = Math.floor((s % 86400) / 3600)
      const mins = Math.floor((s % 3600) / 60)
      const secs = s % 60
      const pad = n => (n < 10 ? '0' + n : n)
      this.runtimeText = `${days}天 ${pad(hours)}:${pad(mins)}:${pad(secs)}`
    }
  }
}
</script>

<style scoped>
.metric-row {
  margin-bottom: 16px;
}

.metric-card {
  background: #fff;
  border-radius: 6px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.08);
  border-top: 3px solid #337ecc;
  padding: 18px 20px;
  display: flex;
  align-items: center;
  margin-bottom: 16px;
}

.metric-icon {
  width: 52px;
  height: 52px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16px;
}

.metric-icon i {
  font-size: 26px;
  color: #fff;
}

.metric-label {
  font-size: 13px;
  color: #909399;
}

.metric-value {
  font-size: 26px;
  font-weight: 600;
  color: #303133;
  line-height: 1.3;
}

.metric-unit {
  font-size: 13px;
  font-weight: 400;
  margin-left: 4px;
  color: #909399;
}

.usage-item {
  margin-bottom: 18px;
}

.usage-label {
  display: flex;
  justify-content: space-between;
  font-size: 14px;
  color: #606266;
  margin-bottom: 6px;
}
</style>
