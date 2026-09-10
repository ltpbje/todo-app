<template>
  <div class="login-page">
    <div class="login-card glass-card" v-glass-glow>
      <div class="glare glass-glare"></div>
      <div class="logo">
        <svg viewBox="0 0 24 24" width="36" height="36" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M9 11l3 3L22 4" /><path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11" />
        </svg>
      </div>
      <h2>{{ isRegister ? '创建账号' : '欢迎回来' }}</h2>
      <p class="subtitle">{{ isRegister ? '注册账号，开始管理你的任务' : '登录以继续管理任务' }}</p>

      <p v-if="error" class="error">{{ error }}</p>
      <p v-if="success" class="success">{{ success }}</p>

      <label class="field">
        <span class="field-label">用户名</span>
        <input v-model.trim="form.username" class="glass-input" placeholder="请输入用户名" autocomplete="username" />
      </label>
      <label class="field">
        <span class="field-label">密码</span>
        <input v-model.trim="form.password" type="password" class="glass-input" placeholder="至少 6 位" autocomplete="current-password" @keyup.enter="submit" />
      </label>
      <label class="field" v-if="isRegister">
        <span class="field-label">邮箱（可选）</span>
        <input v-model.trim="form.email" class="glass-input" placeholder="name@example.com" @keyup.enter="submit" />
      </label>

      <button class="primary glass-button glass-glossy" v-glass-glow :disabled="loading" @click="submit">
        {{ loading ? '请稍候…' : (isRegister ? '注 册' : '登 录') }}
      </button>
      <a href="#" class="switch" @click.prevent="toggle">{{ isRegister ? '已有账号？去登录' : '没有账号？去注册' }}</a>
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
  padding: 24px;
  position: relative;
  z-index: 1;
}

.login-card {
  width: 100%;
  max-width: 400px;
  padding: 40px 36px;
}

.logo {
  position: relative;
  width: 56px;
  height: 56px;
  border-radius: 16px;
  background: var(--accent-grad);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 20px;
  box-shadow: 0 6px 18px rgba(110, 120, 255, 0.4);
  transition: transform 0.3s;
}
.login-card:hover .logo { transform: scale(1.06); }

h2 { position: relative; margin: 0 0 8px; text-align: center; font-size: 22px; font-weight: 600; color: var(--text-primary); }
.subtitle { position: relative; margin: 0 0 24px; text-align: center; font-size: 14px; color: var(--text-secondary); }

.field { display: block; margin-bottom: 16px; position: relative; }
.field-label { display: block; font-size: 13px; color: var(--text-secondary); margin-bottom: 6px; }
.field .glass-input { width: 100%; }

.primary {
  position: relative;
  width: 100%;
  padding: 13px;
  border-radius: 16px;
  font-size: 16px;
  margin-top: 4px;
}

.switch { display: block; position: relative; text-align: center; margin-top: 16px; font-size: 14px; color: var(--accent); text-decoration: none; transition: color 0.2s; }
.switch:hover { color: var(--accent); opacity: 0.8; }

.error { position: relative; color: var(--danger); font-size: 13px; margin: 0 0 12px; text-align: center; }
.success { position: relative; color: #0f9d58; font-size: 13px; margin: 0 0 12px; text-align: center; }
</style>
