<template>
  <div class="page-wrap">
    <!-- 仓库配置 -->
    <div class="page-card">
      <div class="page-card-header">
        <span class="page-card-title">插件仓库配置</span>
      </div>
      <el-form :inline="true" size="small">
        <el-form-item label="仓库地址">
          <el-input
            v-model="repositoryUrl"
            placeholder="例如 https://nexus.example.com/repository/plugins/"
            style="width: 460px;"></el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="savingRepo" @click="saveRepository">保存仓库</el-button>
        </el-form-item>
      </el-form>
      <p class="tip">说明：仓库地址用于检查/下载远程插件元数据（plugins.json）。保存后对检查更新与安装生效。</p>
    </div>

    <el-row :gutter="16">
      <!-- 检查更新 -->
      <el-col :xs="24" :md="12">
        <div class="page-card">
          <div class="page-card-header">
            <span class="page-card-title">检查 / 安装更新</span>
          </div>
          <el-form label-width="90px" size="small">
            <el-form-item label="目标插件">
              <el-select v-model="pluginId" filterable placeholder="选择已安装插件" style="width: 100%;">
                <el-option
                  v-for="p in plugins"
                  :key="p.id"
                  :label="p.id + '  (' + p.version + ')'"
                  :value="p.id"></el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="插件ID">
              <el-input v-model="pluginId" placeholder="或直接输入插件ID" clearable></el-input>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" icon="el-icon-search" :loading="checking" @click="check">检查更新</el-button>
              <el-button type="success" icon="el-icon-download" :loading="installing" @click="updateOrInstall">安装 / 更新</el-button>
            </el-form-item>
          </el-form>

          <div v-if="checkResult" class="check-result">
            <el-alert
              :title="checkResult.hasUpdate ? '发现新版本' : '当前已是最新版本'"
              :type="checkResult.hasUpdate ? 'warning' : 'success'"
              :closable="false"
              show-icon>
            </el-alert>
            <div class="version-compare">
              <span>本地版本：<b>{{ checkResult.localVersion }}</b></span>
              <i class="el-icon-right"></i>
              <span>远程版本：<b>{{ checkResult.remoteVersion }}</b></span>
            </div>
          </div>
        </div>
      </el-col>

      <!-- 操作结果 -->
      <el-col :xs="24" :md="12">
        <div class="page-card">
          <div class="page-card-header">
            <span class="page-card-title">最近操作结果</span>
          </div>
          <pre class="op-result">{{ opResult || '暂无操作记录' }}</pre>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { updateApi, pluginApi, isOk, getMsg } from '@/api'

export default {
  name: 'UpdateMana',
  data() {
    return {
      repositoryUrl: '',
      plugins: [],
      pluginId: '',
      savingRepo: false,
      checking: false,
      installing: false,
      checkResult: null,
      opResult: ''
    }
  },
  mounted() {
    this.loadPlugins()
  },
  methods: {
    loadPlugins() {
      pluginApi.list().then(resp => {
        if (isOk(resp)) this.plugins = resp.data.data || []
      }).catch(() => {})
    },
    saveRepository() {
      if (!this.repositoryUrl) {
        this.$message({ type: 'warning', message: '请输入仓库地址' })
        return
      }
      this.savingRepo = true
      updateApi.setRepository(this.repositoryUrl).then(resp => {
        this.savingRepo = false
        this.opResult = getMsg(resp) || '仓库地址已保存'
        this.$message({ type: isOk(resp) ? 'success' : 'error', message: this.opResult })
      }).catch(() => {
        this.savingRepo = false
        this.$message({ type: 'error', message: '保存仓库地址失败' })
      })
    },
    check() {
      if (!this.pluginId) {
        this.$message({ type: 'warning', message: '请选择或输入插件ID' })
        return
      }
      this.checking = true
      this.checkResult = null
      updateApi.check(this.pluginId).then(resp => {
        this.checking = false
        if (isOk(resp)) {
          this.checkResult = resp.data.data
          this.opResult = getMsg(resp)
        } else {
          this.opResult = getMsg(resp) || '检查更新失败'
          this.$message({ type: 'warning', message: this.opResult })
        }
      }).catch(() => {
        this.checking = false
        this.$message({ type: 'error', message: '检查更新请求失败' })
      })
    },
    updateOrInstall() {
      if (!this.pluginId) {
        this.$message({ type: 'warning', message: '请选择或输入插件ID' })
        return
      }
      this.installing = true
      updateApi.updateOrInstall(this.pluginId).then(resp => {
        this.installing = false
        this.opResult = getMsg(resp) || '操作完成'
        this.$message({ type: isOk(resp) ? 'success' : 'warning', message: this.opResult })
        if (isOk(resp)) {
          this.checkResult = null
          this.loadPlugins()
        }
      }).catch(() => {
        this.installing = false
        this.$message({ type: 'error', message: '安装 / 更新请求失败' })
      })
    }
  }
}
</script>

<style scoped>
.tip {
  font-size: 12px;
  color: #909399;
  margin: 6px 0 0;
}

.check-result {
  margin-top: 12px;
}

.version-compare {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 10px;
  font-size: 14px;
  color: #606266;
}

.version-compare b {
  color: #303133;
}

.op-result {
  background: #f5f7fa;
  border: 1px solid #eaeaea;
  border-radius: 6px;
  padding: 12px 14px;
  font-size: 13px;
  line-height: 1.6;
  min-height: 120px;
  white-space: pre-wrap;
  word-break: break-all;
  color: #606266;
  margin: 0;
}
</style>
