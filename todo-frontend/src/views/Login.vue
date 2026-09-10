<template>
  <div class="login-page" @mousemove="onMove">
    <div class="blob blob-1"></div>
    <div class="blob blob-2"></div>
    <div class="blob blob-3"></div>

    <div class="login-card">
      <div class="glare" :style="glareStyle"></div>
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

      <button class="primary" v-gloss :disabled="loading" @click="submit">
        {{ loading ? '请稍候…' : (isRegister ? '注 册' : '登 录') }}
      </button>
      <a href="#" class="switch" @click.prevent="toggle">{{ isRegister ? '已有账号？去登录' : '没有账号？去注册' }}</a>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api/request'

const router = useRouter()
const isRegister = ref(false)
const loading = ref(false)
const error = ref('')
const success = ref('')
const form = reactive({ username: '', password: '', email: '' })

// soft highlight that follows the cursor inside the card
const mouse = reactive({ x: 0.5, y: 0.5 })

function onMove(e) {
  mouse.x = e.clientX / window.innerWidth
  mouse.y = e.clientY / window.innerHeight
}

const glareStyle = computed(() => ({
  background: `radial-gradient(320px circle at ${mouse.x * 100}% ${mouse.y * 100}%, rgba(255,255,255,0.4) 0%, rgba(255,255,255,0) 60%)`
}))

// gloss highlight follows the cursor within a button
const vGloss = {
  mounted(el) {
    el.classList.add('glossy')
    const move = (e) => {
      const r = el.getBoundingClientRect()
      el.style.setProperty('--gx', `${e.clientX - r.left}px`)
      el.style.setProperty('--gy', `${e.clientY - r.top}px`)
    }
    el.addEventListener('mousemove', move)
    el._glossMove = move
  },
  unmounted(el) {
    el.removeEventListener('mousemove', el._glossMove)
  }
}

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
  background: linear-gradient(160deg, #dfe9ff 0%, #f3e8ff 45%, #ffe8f3 100%);
  padding: 24px;
  position: relative;
  overflow: hidden;
}

/* floating color blobs, parallax follows the mouse */
.blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(70px);
  opacity: 0.65;
  pointer-events: none;
  transition: transform 0.35s cubic-bezier(0.22, 1, 0.36, 1);
  will-change: transform;
}
.blob-1 { width: 420px; height: 420px; background: #7aa8ff; top: -120px; left: -100px; animation: float 12s ease-in-out infinite alternate; }
.blob-2 { width: 360px; height: 360px; background: #c88bff; bottom: -100px; right: -80px; animation: float 12s ease-in-out -4s infinite alternate-reverse; }
.blob-3 { width: 280px; height: 280px; background: #ffa8c9; top: 40%; left: 65%; animation: float 10s ease-in-out -8s infinite alternate; }
@keyframes float {
  from { translate: 0 0; scale: 1; }
  to { translate: 0 40px; scale: 1.08; }
}

.login-card {
  position: relative;
  width: 100%;
  max-width: 400px;
  background: rgba(255, 255, 255, 0.42);
  backdrop-filter: blur(28px) saturate(180%);
  -webkit-backdrop-filter: blur(28px) saturate(180%);
  border: 1px solid rgba(255, 255, 255, 0.6);
  border-radius: 28px;
  padding: 40px 36px;
  box-shadow:
    0 8px 32px rgba(31, 38, 135, 0.18),
    inset 0 1px 0 rgba(255, 255, 255, 0.7);
  overflow: hidden;
}
.login-card:hover {
  box-shadow:
    0 16px 48px rgba(31, 38, 135, 0.24),
    inset 0 1px 0 rgba(255, 255, 255, 0.8);
}

/* moving highlight that tracks the cursor across the glass */
.glare {
  position: absolute;
  inset: 0;
  pointer-events: none;
  border-radius: inherit;
  opacity: 0;
  transition: opacity 0.3s;
}
.login-card:hover .glare { opacity: 1; }

.logo {
  position: relative;
  width: 56px;
  height: 56px;
  border-radius: 16px;
  background: linear-gradient(135deg, rgba(90, 130, 255, 0.85), rgba(170, 110, 255, 0.85));
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 20px;
  box-shadow: 0 6px 18px rgba(110, 120, 255, 0.4);
  transition: transform 0.3s;
}
.login-card:hover .logo { transform: scale(1.06); }

h2 { position: relative; margin: 0 0 8px; text-align: center; font-size: 22px; font-weight: 600; color: #1d1d2b; }
.subtitle { position: relative; margin: 0 0 24px; text-align: center; font-size: 14px; color: #5c5c72; }

.field { display: block; margin-bottom: 16px; position: relative; }
.field-label { display: block; font-size: 13px; color: #5c5c72; margin-bottom: 6px; }

input {
  width: 100%;
  padding: 12px 14px;
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 14px;
  font-size: 15px;
  outline: none;
  background: rgba(255, 255, 255, 0.5);
  color: #1d1d2b;
  transition: border-color 0.2s, box-shadow 0.2s, background 0.2s, transform 0.2s;
  box-sizing: border-box;
}
input::placeholder { color: #9a9ab0; }
input:hover { background: rgba(255, 255, 255, 0.62); }
input:focus {
  border-color: rgba(110, 130, 255, 0.8);
  background: rgba(255, 255, 255, 0.78);
  box-shadow: 0 0 0 4px rgba(110, 130, 255, 0.18);
  transform: translateY(-1px);
}

.primary {
  position: relative;
  width: 100%;
  padding: 13px;
  border: 1px solid rgba(255, 255, 255, 0.75);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.28);
  backdrop-filter: blur(12px) saturate(160%);
  -webkit-backdrop-filter: blur(12px) saturate(160%);
  color: #3a3a5c;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  margin-top: 4px;
  overflow: hidden;
  transition: transform 0.15s, background 0.2s, box-shadow 0.25s;
  box-shadow:
    0 6px 18px rgba(31, 38, 135, 0.12),
    inset 0 1px 0 rgba(255, 255, 255, 0.85),
    inset 0 -1px 0 rgba(255, 255, 255, 0.25);
}
.primary:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.45);
  box-shadow:
    0 10px 26px rgba(31, 38, 135, 0.18),
    inset 0 1px 0 rgba(255, 255, 255, 0.9),
    inset 0 -1px 0 rgba(255, 255, 255, 0.3);
  transform: translateY(-1px);
}
.primary:active:not(:disabled) { transform: scale(0.97); }
.primary:disabled { opacity: 0.6; cursor: not-allowed; }

/* gloss highlight that follows the cursor */
.glossy::after {
  content: '';
  position: absolute;
  left: var(--gx, 50%);
  top: var(--gy, 50%);
  width: 140px;
  height: 140px;
  transform: translate(-50%, -50%);
  background: radial-gradient(circle, rgba(255, 255, 255, 0.45) 0%, rgba(255, 255, 255, 0) 65%);
  opacity: 0;
  transition: opacity 0.25s;
  pointer-events: none;
  border-radius: 50%;
}
.glossy:hover::after { opacity: 1; }

.switch { display: block; position: relative; text-align: center; margin-top: 16px; font-size: 14px; color: #4a6cf7; text-decoration: none; transition: color 0.2s; }
.switch:hover { color: #7a5cff; }

.error { position: relative; color: #e0344b; font-size: 13px; margin: 0 0 12px; text-align: center; }
.success { position: relative; color: #0f9d58; font-size: 13px; margin: 0 0 12px; text-align: center; }
</style>
