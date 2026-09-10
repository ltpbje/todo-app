<template>
  <div class="login-page">
    <div class="card">
      <h2>{{ isRegister ? '注册' : '登录' }}</h2>
      <p v-if="error" class="error">{{ error }}</p>
      <input v-model.trim="form.username" placeholder="用户名" @keyup.enter="submit" />
      <input v-model.trim="form.password" type="password" placeholder="密码" @keyup.enter="submit" />
      <input v-if="isRegister" v-model.trim="form.email" placeholder="邮箱（可选）" @keyup.enter="submit" />
      <button :disabled="loading" @click="submit">{{ loading ? '请稍候…' : (isRegister ? '注册' : '登录') }}</button>
      <a href="#" @click.prevent="toggle">{{ isRegister ? '已有账号？去登录' : '没有账号？去注册' }}</a>
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
const form = reactive({ username: '', password: '', email: '' })

function toggle() {
  isRegister.value = !isRegister.value
  error.value = ''
}

async function submit() {
  error.value = ''
  if (!form.username || !form.password) {
    error.value = '请输入用户名和密码'
    return
  }
  loading.value = true
  try {
    if (isRegister.value) {
      await api.post('/register', form)
      isRegister.value = false
      error.value = ''
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
.login-page { display: flex; justify-content: center; align-items: center; min-height: 100vh; background: #f5f6f8; }
.card { background: #fff; padding: 32px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,.08); width: 320px; display: flex; flex-direction: column; gap: 12px; }
.card h2 { margin: 0 0 8px; text-align: center; }
input { padding: 10px; border: 1px solid #ddd; border-radius: 6px; font-size: 14px; }
button { padding: 10px; border: none; border-radius: 6px; background: #4098ff; color: #fff; font-size: 15px; cursor: pointer; }
button:disabled { opacity: .6; }
.error { color: #e5484d; margin: 0; font-size: 13px; }
a { text-align: center; font-size: 13px; color: #4098ff; text-decoration: none; }
</style>
