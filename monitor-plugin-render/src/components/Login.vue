<template>
  <div class="login-page">
    <div class="login-brand">
      <div class="brand-inner">
        <i class="el-icon-s-platform brand-icon"></i>
        <h1 class="brand-title">服务监控管理控制台</h1>
        <p class="brand-desc">插件热插拔 · JVM / 数据库 / 日志 一体化监控运维平台</p>
        <ul class="brand-feature">
          <li><i class="el-icon-circle-check"></i> 插件全生命周期管理</li>
          <li><i class="el-icon-circle-check"></i> 扩展能力动态调用</li>
          <li><i class="el-icon-circle-check"></i> 远程更新与自升级</li>
        </ul>
      </div>
    </div>

    <div class="login-form-wrap">
      <el-form
        :rules="rules"
        ref="loginForm"
        :model="loginForm"
        class="login-form"
        label-position="top"
        @keydown.native.enter="submitClick"
        v-loading="loading">
        <h3 class="login-title">欢迎登录</h3>
        <p class="login-subtitle">请使用管理员账号进入控制台</p>

        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            prefix-icon="el-icon-user"
            placeholder="账号"
            auto-complete="off"
            ref="usernameInput"></el-input>
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            prefix-icon="el-icon-lock"
            placeholder="密码"
            show-password
            auto-complete="off"
            ref="passwordInput"></el-input>
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            class="login-btn"
            :loading="loading"
            @click.native.prevent="submitClick">登 录</el-button>
        </el-form-item>
      </el-form>

      <div class="login-version">版本号：v1.0.0</div>
    </div>
  </div>
</template>

<script>
import { authApi } from '@/api'

export default {
  name: 'Login',
  data() {
    return {
      rules: {
        username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
        password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
      },
      loginForm: {
        username: '',
        password: ''
      },
      loading: false
    }
  },
  methods: {
    submitClick() {
      this.$refs.loginForm.validate(valid => {
        if (!valid) {
          return
        }
        this.loading = true
        authApi.login(this.loginForm.username, this.loginForm.password).then(resp => {
          this.loading = false
          const json = resp.data
          if (json.state) {
            sessionStorage.setItem('user', json.data.username)
            sessionStorage.setItem('token', json.data.token)
            this.$router.replace({ path: '/home' })
          } else {
            this.$alert(json.msg || '用户名或密码错误!', '登录失败', {
              type: 'warning',
              callback: () => {
                this.loginForm.password = ''
                this.$refs.usernameInput.focus()
              }
            })
          }
        }).catch(() => {
          this.loading = false
          this.$alert('找不到服务器，请确认后端服务已启动!', '登录失败', { type: 'warning' })
        })
      })
    }
  }
}
</script>

<style scoped>
.login-page {
  display: flex;
  height: 100vh;
  width: 100%;
  background: #f2f4f8;
}

.login-brand {
  flex: 1;
  background: linear-gradient(135deg, #26344f 0%, #337ecc 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.brand-inner {
  text-align: center;
  max-width: 460px;
  padding: 0 40px;
}

.brand-icon {
  font-size: 72px;
  color: rgba(255, 255, 255, 0.9);
}

.brand-title {
  font-size: 30px;
  font-weight: 700;
  margin: 20px 0 12px;
  letter-spacing: 2px;
}

.brand-desc {
  font-size: 15px;
  color: rgba(255, 255, 255, 0.8);
  line-height: 1.8;
}

.brand-feature {
  list-style: none;
  padding: 0;
  margin: 30px 0 0;
  text-align: left;
  display: inline-block;
}

.brand-feature li {
  font-size: 15px;
  color: rgba(255, 255, 255, 0.9);
  line-height: 2.2;
}

.brand-feature li i {
  margin-right: 10px;
  color: #7de3c7;
}

.login-form-wrap {
  width: 460px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #fff;
  position: relative;
}

.login-form {
  width: 340px;
}

.login-title {
  font-size: 26px;
  color: #303133;
  margin: 0 0 6px;
}

.login-subtitle {
  font-size: 14px;
  color: #909399;
  margin: 0 0 30px;
}

.login-btn {
  width: 100%;
  letter-spacing: 4px;
  font-size: 15px;
}

.login-version {
  position: absolute;
  bottom: 20px;
  right: 24px;
  font-size: 12px;
  color: #c0c4cc;
}

@media screen and (max-width: 900px) {
  .login-brand {
    display: none;
  }
  .login-form-wrap {
    width: 100%;
  }
}
</style>
