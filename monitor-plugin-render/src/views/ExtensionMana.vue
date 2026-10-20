<template>
  <div class="page-wrap">
    <el-row :gutter="16">
      <!-- 扩展列表 -->
      <el-col :xs="24" :md="12">
        <div class="page-card">
          <div class="page-card-header">
            <span class="page-card-title">扩展列表</span>
            <div class="toolbar" style="margin-bottom: 0;">
              <el-select v-model="extensionClass" size="small" style="width: 260px;">
                <el-option label="ActionExtension（主扩展点）" :value="ACTION_CLS"></el-option>
                <el-option label="OperationButtonExtension（旧按钮）" :value="BUTTON_CLS"></el-option>
              </el-select>
              <el-button size="small" type="primary" icon="el-icon-search" @click="loadList">查询</el-button>
            </div>
          </div>

          <el-table :data="list" border v-loading="loading" style="width: 100%;" max-height="520">
            <el-table-column prop="name" label="扩展名称(name)" min-width="140"></el-table-column>
            <el-table-column label="所属插件" min-width="150">
              <template slot-scope="scope">
                {{ scope.row.plugin ? scope.row.plugin.id : '-' }}
              </template>
            </el-table-column>
            <el-table-column label="版本" width="90" align="center">
              <template slot-scope="scope">{{ scope.row.plugin ? scope.row.plugin.version : '-' }}</template>
            </el-table-column>
            <el-table-column label="状态" width="100" align="center">
              <template slot-scope="scope">
                <span class="status-badge">{{ scope.row.plugin ? scope.row.plugin.state : '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="90" align="center">
              <template slot-scope="scope">
                <el-button size="mini" type="text" @click="fillForm(scope.row)">调用</el-button>
              </template>
            </el-table-column>
          </el-table>
          <p v-if="!loading && list.length === 0" class="empty-tip">未查询到扩展，请确认插件已启动后重试。</p>
        </div>
      </el-col>

      <!-- 动作调用调试 -->
      <el-col :xs="24" :md="12">
        <div class="page-card">
          <div class="page-card-header">
            <span class="page-card-title">扩展动作调用 (/ext/action)</span>
          </div>

          <el-form :model="form" label-width="110px" size="small">
            <el-form-item label="extensionClass">
              <el-input v-model="form.extensionClass" placeholder="扩展点接口全类名"></el-input>
            </el-form-item>
            <el-form-item label="extensionName">
              <el-input v-model="form.extensionName" placeholder="扩展 name，如 JvmMonitor"></el-input>
            </el-form-item>
            <el-form-item label="targetAction">
              <el-input v-model="form.targetAction" placeholder="目标操作，如 ping / listJavaProcess"></el-input>
            </el-form-item>
            <el-form-item label="parameters">
              <el-input
                type="textarea"
                :rows="5"
                v-model="form.parameters"
                placeholder='JSON 格式，例如 {"pid":"12345"}'></el-input>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" icon="el-icon-video-play" :loading="calling" @click="doAction">发起调用</el-button>
              <el-button icon="el-icon-delete" @click="resetResult">清空结果</el-button>
            </el-form-item>
          </el-form>

          <div v-if="result !== null" class="result-box">
            <div class="result-title">
              返回结果
              <el-tag :type="resultOk ? 'success' : 'danger'" size="mini">{{ resultOk ? '成功' : '失败' }}</el-tag>
            </div>
            <pre class="result-pre">{{ result }}</pre>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { extApi, isOk, ACTION_EXTENSION_CLASS, BUTTON_EXTENSION_CLASS } from '@/api'

export default {
  name: 'ExtensionMana',
  data() {
    return {
      ACTION_CLS: ACTION_EXTENSION_CLASS,
      BUTTON_CLS: BUTTON_EXTENSION_CLASS,
      extensionClass: ACTION_EXTENSION_CLASS,
      list: [],
      loading: false,
      calling: false,
      result: null,
      resultOk: false,
      form: {
        extensionClass: ACTION_EXTENSION_CLASS,
        extensionName: '',
        targetAction: '',
        parameters: '{}'
      }
    }
  },
  methods: {
    loadList() {
      this.loading = true
      extApi.list(this.extensionClass).then(resp => {
        this.loading = false
        this.list = resp.data || []
      }).catch(() => {
        this.loading = false
        this.$message({ type: 'error', message: '获取扩展列表失败' })
      })
    },
    fillForm(row) {
      this.form.extensionClass = this.extensionClass
      this.form.extensionName = row.name
      this.$message({ type: 'info', message: '已填入扩展名，请填写 targetAction 与参数后调用' })
    },
    doAction() {
      let parsedParams
      try {
        parsedParams = this.form.parameters ? JSON.parse(this.form.parameters) : {}
      } catch (e) {
        this.$message({ type: 'error', message: 'parameters 不是合法 JSON' })
        return
      }
      this.calling = true
      extApi.action({
        extensionClass: this.form.extensionClass,
        extensionName: this.form.extensionName,
        targetAction: this.form.targetAction,
        parameters: parsedParams
      }).then(resp => {
        this.calling = false
        this.resultOk = isOk(resp)
        this.result = JSON.stringify(resp.data, null, 2)
      }).catch(err => {
        this.calling = false
        this.resultOk = false
        this.result = '调用异常：' + (err && err.message ? err.message : err)
      })
    },
    resetResult() {
      this.result = null
      this.resultOk = false
    }
  }
}
</script>

<style scoped>
.empty-tip {
  text-align: center;
  color: #909399;
  font-size: 13px;
  padding: 20px 0;
}

.result-box {
  margin-top: 12px;
  border-top: 1px dashed #dcdfe6;
  padding-top: 12px;
}

.result-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 600;
  font-size: 14px;
  margin-bottom: 8px;
}

.result-pre {
  background: #263445;
  color: #d0e3ff;
  padding: 12px 14px;
  border-radius: 6px;
  font-size: 12px;
  line-height: 1.6;
  max-height: 320px;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-all;
  margin: 0;
}
</style>
