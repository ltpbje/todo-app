<template>
  <div class="login-page">
    <div class="login-container">
      <div class="login-card">
        <div class="logo">
          <svg viewBox="0 0 24 24" width="40" height="40" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M9 11l3 3L22 4" /><path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11" />
          </svg>
        </div>
        <h2>{{ isRegister ? '创建账号' : '欢迎回来' }}</h2>
        <p class="subtitle">{{ isRegister ? '注册账号，开始管理你的任务' : '登录以继续管理任务' }}</p>

        <p v-if="error" class="error">{{ error }}</p>
        <p v-if="success" class="success">{{ success }}</p>

        <label class="field">
          <span class="field-label">用户名</span>
          <input v-model.trim="form.username" placeholder="请输入用户名" autocomplete="username" />
        </label>
        <label class="field">
          <span class="field-label">密码</span>
          <input v-model.trim="form.password" type="password" placeholder="至少 6 位" autocomplete="current-password" @keyup.enter="submit" />
        </label>
        <label class="field" v-if="isRegister">
          <span class="field-label">邮箱（可选）</span>
          <input v-model.trim="form.email" placeholder="name@example.com" @keyup.enter="submit" />
        </label>

        <button class="primary" :disabled="loading" @click="submit">
          {{ loading ? '请稍候…' : (isRegister ? '注 册' : '登 录') }}
        </button>
        <a href="#" class="switch" @click.prevent="toggle">{{ isRegister ? '已有账号？去登录' : '没有账号？去注册' }}</a>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api/request'

const router = useRouter()
const isRegister = ref(false)
const loading = ref(false)
const error = ref('')
const success = ref('')
const form = reactive({ username: '', password: '', email: '' })

function toggle() {
  isRegister.value = !isRegister.value
  error.value = ''
  success.value = ''
}

async function submit() {
  error.value = ''
  success.value = ''
  if (!form.username || !form.password) {
    error.value = '请输入用户名和密码'
    return
  }
  loading.value = true
  try {
    if (isRegister.value) {
      await api.post('/register', form)
      isRegister.value = false
      success.value = '注册成功，请登录'
    }
    const { data } = await api.post('/login', form)
    localStorage.setItem('token', data.token)
    router.push('/')
  } catch (e) {
    error.value = e.response?.data?.message || '请求失败，请稍后重试'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(180deg, #ffffff 0%, #f0f4ff 100%);
  padding: 24px;
}
.login-container {
  width: 100%;
  max-width: 400px;
}
.login-card {
  background: #ffffff;
  border-radius: 20px;
  padding: 40px 36px;
  box-shadow: 0 8px 40px rgba(0, 0, 0, 0.08);
}
.logo {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 20px;
}
h2 {
  margin: 0 0 8px;
  text-align: center;
  font-size: 22px;
  font-weight: 600;
  color: #1d1d1f;
}
.subtitle {
  margin: 0 0 24px;
  text-align: center;
  font-size: 14px;
  color: #86868b;
}
.field {
  display: block;
  margin-bottom: 16px;
}
.field-label {
  display: block;
  font-size: 13px;
  color: #86868b;
  margin-bottom: 6px;
}
input {
  width: 100%;
  padding: 12px 14px;
  border: 1px solid #e5e5ea;
  border-radius: 12px;
  font-size: 15px;
  outline: none;
  transition: border-color 0.2s, box-shadow 0.2s;
}
input:focus {
  border-color: #007aff;
  box-shadow: 0 0 0 3px rgba(0, 122, 255, 0.15);
}
.primary {
  width: 100%;
  padding: 13px;
  border: none;
  border-radius: 12px;
  background: linear-gradient(135deg, #007aff, #5ac8fa);
  color: #ffffff;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: opacity 0.2s, transform 0.1s;
  margin-top: 4px;
}
.primary:hover:not(:disabled) { opacity: 0.9; }
.primary:active:not(:disabled) { transform: scale(0.98); }
.primary:disabled { opacity: 0.6; cursor: not-allowed; }
.switch {
  text-align: center;
  margin-top: 16px;
  font-size: 14px;
  color: #007aff;
  text-decoration: none;
}
.switch:hover { text-decoration: underline; }
.error { color: #ff3b30; font-size: 13px; margin: 0; text-align: center; }
.success { color: #34c759; font-size: 13px; margin: 0; text-align: center; }
</style>
