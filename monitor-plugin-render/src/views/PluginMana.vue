<template>
  <div class="page-wrap">
    <div class="page-card">
      <div class="page-card-header">
        <span class="page-card-title">插件管理</span>
        <div class="toolbar" style="margin-bottom: 0;">
          <el-input
            v-model="search"
            size="small"
            placeholder="搜索插件ID / 状态"
            prefix-icon="el-icon-search"
            clearable
            style="width: 220px;"></el-input>
          <el-button size="small" icon="el-icon-refresh" @click="loadList">刷新</el-button>
          <el-button size="small" type="warning" icon="el-icon-refresh-1" :loading="reloading" @click="reloadAll">重载全部</el-button>
          <el-button size="small" type="primary" icon="el-icon-upload2" @click="uploadDialog = true">上传插件</el-button>
        </div>
      </div>

      <el-table :data="filteredList" border stripe v-loading="loading" style="width: 100%;">
        <el-table-column type="index" label="#" width="55" align="center"></el-table-column>
        <el-table-column prop="id" label="插件ID" min-width="180" show-overflow-tooltip></el-table-column>
        <el-table-column prop="version" label="版本" width="90" align="center"></el-table-column>
        <el-table-column prop="provider" label="提供者" width="110" align="center"></el-table-column>
        <el-table-column label="状态" width="110" align="center">
          <template slot-scope="scope">
            <span class="status-badge" :class="'state-' + scope.row.state.toLowerCase()">
              {{ stateText(scope.row.state) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="path" label="路径" min-width="220" show-overflow-tooltip></el-table-column>
        <el-table-column label="操作" width="300" align="center" fixed="right">
          <template slot-scope="scope">
            <el-button
              v-if="scope.row.state !== 'STARTED' && scope.row.state !== 'DISABLED'"
              size="mini" type="success" @click="doAction('start', scope.row)">启动</el-button>
            <el-button
              v-if="scope.row.state === 'STARTED'"
              size="mini" type="warning" @click="doAction('stop', scope.row)">停止</el-button>
            <el-button
              v-if="scope.row.state === 'DISABLED'"
              size="mini" type="success" @click="doAction('enable', scope.row)">启用</el-button>
            <el-button
              v-if="scope.row.state !== 'DISABLED'"
              size="mini" plain @click="doAction('disable', scope.row)">禁用</el-button>
            <el-button
              v-if="scope.row.state === 'STARTED' || scope.row.state === 'STOPPED' || scope.row.state === 'CREATED' || scope.row.state === 'RESOLVED' || scope.row.state === 'LOADED'"
              size="mini" type="info" plain @click="doAction('unload', scope.row)">卸载</el-button>
            <el-button size="mini" type="danger" plain @click="doAction('delete', scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 上传插件对话框 -->
    <el-dialog title="上传插件 JAR" :visible.sync="uploadDialog" width="480px">
      <el-upload
        drag
        action="#"
        :auto-upload="false"
        :limit="1"
        accept=".jar"
        :file-list="fileList"
        :on-change="onFileChange"
        :on-remove="onFileRemove">
        <i class="el-icon-upload"></i>
        <div class="el-upload__text">将 .jar 文件拖到此处，或<em>点击选择</em></div>
        <div class="el-upload__tip" slot="tip">仅支持 .jar 格式，单个文件不超过 100MB</div>
      </el-upload>
      <div slot="footer">
        <el-button @click="uploadDialog = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="doUpload">开始上传</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { pluginApi, isOk, getMsg } from '@/api'

export default {
  name: 'PluginMana',
  data() {
    return {
      list: [],
      search: '',
      loading: false,
      reloading: false,
      uploading: false,
      uploadDialog: false,
      fileList: [],
      currentFile: null
    }
  },
  computed: {
    filteredList() {
      const kw = this.search.trim().toLowerCase()
      if (!kw) return this.list
      return this.list.filter(p =>
        (p.id && p.id.toLowerCase().indexOf(kw) !== -1) ||
        (p.state && p.state.toLowerCase().indexOf(kw) !== -1))
    }
  },
  mounted() {
    this.loadList()
  },
  methods: {
    stateText(state) {
      const map = {
        STARTED: '运行中',
        STOPPED: '已停止',
        DISABLED: '已禁用',
        CREATED: '已创建',
        RESOLVED: '已解析',
        LOADED: '已加载',
        UNLOADED: '已卸载',
        FAILED: '失败'
      }
      return map[state] || state
    },
    loadList() {
      this.loading = true
      pluginApi.list().then(resp => {
        this.loading = false
        if (isOk(resp)) {
          this.list = resp.data.data || []
        } else {
          this.$message({ type: 'error', message: getMsg(resp) || '获取插件列表失败' })
        }
      }).catch(() => {
        this.loading = false
        this.$message({ type: 'error', message: '获取插件列表失败，请确认后端服务已启动' })
      })
    },
    reloadAll() {
      this.reloading = true
      pluginApi.reload().then(resp => {
        this.reloading = false
        this.$message({ type: isOk(resp) ? 'success' : 'error', message: getMsg(resp) || '重载完成' })
        this.loadList()
      }).catch(() => {
        this.reloading = false
        this.$message({ type: 'error', message: '重载插件失败' })
      })
    },
    doAction(action, row) {
      const confirmMap = {
        delete: '确定删除插件「' + row.id + '」吗？该操作将删除插件文件。',
        unload: '确定卸载插件「' + row.id + '」吗？',
        disable: '确定禁用插件「' + row.id + '」吗？'
      }
      const run = () => {
        const fn = {
          start: pluginApi.start,
          stop: pluginApi.stop,
          enable: pluginApi.enable,
          disable: pluginApi.disable,
          unload: pluginApi.unload,
          delete: pluginApi.remove
        }[action]
        fn(row.id).then(resp => {
          this.$message({ type: isOk(resp) ? 'success' : 'warning', message: getMsg(resp) || '操作完成' })
          this.loadList()
        }).catch(() => {
          this.$message({ type: 'error', message: '操作失败' })
        })
      }
      if (confirmMap[action]) {
        this.$confirm(confirmMap[action], '提示', { type: 'warning' }).then(run).catch(() => {})
      } else {
        run()
      }
    },
    onFileChange(file, fileList) {
      this.currentFile = file.raw
      this.fileList = fileList
    },
    onFileRemove(file, fileList) {
      this.currentFile = null
      this.fileList = fileList
    },
    doUpload() {
      if (!this.currentFile) {
        this.$message({ type: 'warning', message: '请先选择插件文件' })
        return
      }
      this.uploading = true
      pluginApi.upload(this.currentFile).then(resp => {
        this.uploading = false
        if (isOk(resp)) {
          this.$message({ type: 'success', message: getMsg(resp) || '上传成功' })
          this.uploadDialog = false
          this.fileList = []
          this.currentFile = null
          this.loadList()
        } else {
          this.$message({ type: 'error', message: getMsg(resp) || '上传失败' })
        }
      }).catch(() => {
        this.uploading = false
        this.$message({ type: 'error', message: '上传失败' })
      })
    }
  }
}
</script>

<style scoped>
.state-started { background: #e6f7f4; color: #17b3a3; }
.state-stopped,
.state-created,
.state-resolved,
.state-loaded { background: #fdf6ec; color: #e6a23c; }
.state-disabled { background: #fef0f0; color: #f56c6c; }
.state-unloaded,
.state-failed { background: #f4f4f5; color: #909399; }
</style>
