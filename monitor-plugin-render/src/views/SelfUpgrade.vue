<template>
  <div class="page-wrap">
    <el-row :gutter="16">
      <!-- 升级操作 + 状态 -->
      <el-col :xs="24" :md="12">
        <div class="page-card">
          <div class="page-card-header">
            <span class="page-card-title">主程序自升级</span>
            <el-button type="text" icon="el-icon-refresh" @click="loadStatus">刷新状态</el-button>
          </div>

          <el-descriptions :column="1" border size="medium" style="margin-bottom: 16px;">
            <el-descriptions-item label="当前版本">{{ status.currentVersion || '-' }}</el-descriptions-item>
            <el-descriptions-item label="升级状态">
              <el-tag :type="status.isUpgrading ? 'warning' : 'success'" size="mini">
                {{ status.isUpgrading ? '升级中' : '空闲' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="最近检查">{{ lastCheckText }}</el-descriptions-item>
          </el-descriptions>

          <div class="upgrade-actions">
            <el-checkbox v-model="autoConfirm">自动确认升级</el-checkbox>
            <div class="toolbar" style="margin-top: 12px;">
              <el-button
                type="primary"
                icon="el-icon-upload2"
                :loading="performing"
                @click="perform">执行自升级</el-button>
              <el-button icon="el-icon-search" :loading="performing" @click="checkNow">立即检查</el-button>
            </div>
          </div>

          <el-alert
            class="upgrade-tip"
            title="自升级将备份当前程序后拉取新版本并重启，请谨慎操作。"
            type="info"
            :closable="false"
            show-icon>
          </el-alert>

          <div v-if="performResult" class="perform-result">
            <pre class="result-pre">{{ performResult }}</pre>
          </div>
        </div>
      </el-col>

      <!-- 备份列表 -->
      <el-col :xs="24" :md="12">
        <div class="page-card">
          <div class="page-card-header">
            <span class="page-card-title">备份列表</span>
            <el-button type="text" icon="el-icon-refresh" @click="loadBackups">刷新</el-button>
          </div>
          <p class="backup-dir" v-if="backupDir">备份目录：{{ backupDir }}</p>
          <el-table :data="backups" border v-loading="loadingBackups" style="width: 100%;" max-height="420">
            <el-table-column type="index" label="#" width="55" align="center"></el-table-column>
            <el-table-column prop="name" label="备份文件" show-overflow-tooltip></el-table-column>
          </el-table>
          <p v-if="!loadingBackups && backups.length === 0" class="empty-tip">暂无备份文件</p>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { selfUpgradeApi, isOk, getMsg } from '@/api'

export default {
  name: 'SelfUpgrade',
  data() {
    return {
      status: { isUpgrading: false, currentVersion: '', lastCheckTime: 0 },
      autoConfirm: false,
      performing: false,
      performResult: '',
      backups: [],
      backupDir: '',
      loadingBackups: false
    }
  },
  computed: {
    lastCheckText() {
      const t = this.status.lastCheckTime
      if (!t) return '-'
      const d = new Date(t)
      const pad = n => (n < 10 ? '0' + n : n)
      return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
    }
  },
  mounted() {
    this.loadStatus()
    this.loadBackups()
  },
  methods: {
    loadStatus() {
      selfUpgradeApi.status().then(resp => {
        if (isOk(resp)) this.status = resp.data.data || this.status
      }).catch(() => {})
    },
    loadBackups() {
      this.loadingBackups = true
      selfUpgradeApi.backups().then(resp => {
        this.loadingBackups = false
        if (isOk(resp)) {
          const d = resp.data.data || {}
          this.backupDir = d.backupDir || ''
          this.backups = (d.backups || []).map(name => ({ name }))
        }
      }).catch(() => {
        this.loadingBackups = false
      })
    },
    perform() {
      this.$confirm('确定执行主程序自升级吗？该操作可能重启服务。', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.runUpgrade(true)
      }).catch(() => {})
    },
    checkNow() {
      this.runUpgrade(false)
    },
    runUpgrade(confirm) {
      this.performing = true
      this.performResult = ''
      selfUpgradeApi.perform(this.autoConfirm || confirm).then(resp => {
        this.performing = false
        this.performResult = (isOk(resp) ? '成功：' : '失败：') + (getMsg(resp) || JSON.stringify(resp.data))
        this.loadStatus()
        this.loadBackups()
      }).catch(err => {
        this.performing = false
        this.performResult = '请求异常：' + (err && err.message ? err.message : err)
      })
    }
  }
}
</script>

<style scoped>
.upgrade-actions {
  margin-bottom: 12px;
}

.upgrade-tip {
  margin-top: 8px;
}

.perform-result {
  margin-top: 12px;
}

.result-pre {
  background: #f5f7fa;
  border: 1px solid #eaeaea;
  border-radius: 6px;
  padding: 12px 14px;
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
  color: #606266;
  margin: 0;
}

.backup-dir {
  font-size: 12px;
  color: #909399;
  margin: 0 0 10px;
  word-break: break-all;
}

.empty-tip {
  text-align: center;
  color: #909399;
  font-size: 13px;
  padding: 16px 0;
}
</style>
