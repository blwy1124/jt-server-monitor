import { request } from '@/utils/api'

/**
 * 后端存在两种统一返回包装：
 *  - ResultMsg: { data, message, state(1/0), timestamp, errorCode }  —— 插件/扩展/更新/自升级
 *  - Result:    { state(true/false), code, msg, data }               —— 登录/系统信息
 * 下列工具方法用于兼容两者。
 */
export function isOk(res) {
  // res 为 axios response
  const d = res && res.data
  if (!d) return false
  return d.state === 1 || d.state === true
}

export function getMsg(res) {
  const d = res && res.data
  if (!d) return ''
  return d.message || d.msg || ''
}

export function getData(res) {
  const d = res && res.data
  return d ? d.data : null
}

/** 把普通参数封装成后端 ExtensionRequestParam.parameters 结构 */
function params(obj) {
  return { parameters: obj || {} }
}

/* ==================== 插件管理 /plugin ==================== */
export const pluginApi = {
  list() {
    return request.post('/plugin/list')
  },
  reload() {
    return request.post('/plugin/reload')
  },
  upload(file) {
    const fd = new FormData()
    fd.append('file', file)
    return request.post('/plugin/upload', fd, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  load(pluginId) {
    return request.post('/plugin/load', params({ pluginId }))
  },
  unload(pluginId) {
    return request.post('/plugin/unload', params({ pluginId }))
  },
  start(pluginId) {
    return request.post('/plugin/start', params({ pluginId }))
  },
  stop(pluginId) {
    return request.post('/plugin/stop', params({ pluginId }))
  },
  disable(pluginId) {
    return request.post('/plugin/disable', params({ pluginId }))
  },
  enable(pluginId) {
    return request.post('/plugin/enable', params({ pluginId }))
  },
  remove(pluginId) {
    return request.post('/plugin/delete', params({ pluginId }))
  }
}

/* ==================== 扩展调用 /ext ==================== */
export const extApi = {
  list(extensionClass) {
    return request.get('/ext/list', { params: { extensionClass } })
  },
  action(payload) {
    // payload: { extensionClass, extensionName, targetAction, parameters }
    return request.post('/ext/action', payload)
  }
}

/* ==================== 系统信息 /main ==================== */
export const mainApi = {
  systemInfo() {
    return request.post('/main/getSystemInfo')
  }
}

/* ==================== 插件更新 /update ==================== */
export const updateApi = {
  setRepository(repositoryUrl) {
    return request.post('/update/repository', params({ repositoryUrl }))
  },
  check(pluginId) {
    return request.post('/update/check', params({ pluginId }))
  },
  updateOrInstall(pluginId) {
    return request.post('/update/update-or-install', params({ pluginId }))
  },
  install(pluginId) {
    return request.post('/update/install', params({ pluginId }))
  },
  update(pluginId) {
    return request.post('/update/update', params({ pluginId }))
  }
}

/* ==================== 主程序自升级 /self-upgrade ==================== */
export const selfUpgradeApi = {
  perform(autoConfirm) {
    return request.post('/self-upgrade/perform', params({ autoConfirm: String(!!autoConfirm) }))
  },
  status() {
    return request.get('/self-upgrade/status')
  },
  backups() {
    return request.get('/self-upgrade/backups')
  }
}

/* ==================== 登录鉴权 ==================== */
export const authApi = {
  login(username, password) {
    return request.post('/login', { username, password })
  },
  currentUserName(token) {
    return request.post('/currentUserName', { token })
  },
  alert(payload) {
    return request.post('/alert', payload || {})
  }
}

/* 扩展点接口全类名（后端主扩展点） */
export const ACTION_EXTENSION_CLASS = 'com.jt.plugins.api.monitor.ActionExtension'
export const BUTTON_EXTENSION_CLASS = 'com.jt.plugins.api.OperationButtonExtension'
