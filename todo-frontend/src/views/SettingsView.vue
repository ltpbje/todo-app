<template>
  <div class="page">
    <div class="content">
      <GlassObject class="glass-card" v-glass-glow>
        <div class="glare glass-glare"></div>

        <div class="head">
          <h2>AI 配置</h2>
          <p>配置保存在当前账号下，留空则使用后端默认配置。</p>
        </div>

        <div class="field">
          <label for="base-url">API 地址</label>
          <input id="base-url" v-model.trim="form.baseUrl" class="glass-input" placeholder="https://api.deepseek.com" autocomplete="off" />
        </div>

        <div class="field">
          <label for="api-key">API Key</label>
          <div class="key-row">
            <input
              id="api-key"
              v-model.trim="form.apiKey"
              :type="showKey ? 'text' : 'password'"
              class="glass-input"
              :placeholder="savedKeyHint ? `已保存 ${savedKeyHint}，留空表示不修改` : 'sk-...'"
              autocomplete="off"
            />
            <button type="button" class="glass-button" @click="showKey = !showKey">{{ showKey ? '隐藏' : '显示' }}</button>
          </div>
        </div>

        <div class="field-grid">
          <div class="field">
            <label for="chat-model">对话模型</label>
            <input id="chat-model" v-model.trim="form.chatModel" class="glass-input" placeholder="deepseek-chat" autocomplete="off" />
          </div>
          <div class="field">
            <label for="embed-model">Embedding 模型（可选）</label>
            <input id="embed-model" v-model.trim="form.embedModel" class="glass-input" placeholder="text-embedding-3-small" autocomplete="off" />
          </div>
        </div>

        <p class="note">API Key 使用 AES-GCM 加密后存入数据库，页面只显示脱敏值；留空保存表示保留原 Key。</p>

        <div class="actions">
          <button class="glass-button accent" :disabled="saving" @click="save">{{ saving ? '保存中' : '保存配置' }}</button>
          <button class="glass-button" :disabled="testing" @click="test">
            <span v-if="testing" class="spinner"></span>{{ testing ? '测试中' : '测试连接' }}
          </button>
          <button class="glass-button danger" :disabled="clearing" @click="clear">清空</button>
        </div>

        <p v-if="message" class="message" :class="{ error: !ok }">{{ message }}</p>
      </GlassObject>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import GlassObject from '../components/GlassObject.vue'
import { aiPost } from '../api/ai'
import { clearAiSettings, getAiSettings, saveAiSettings } from '../utils/aiConfig'

const form = reactive({ baseUrl: '', apiKey: '', chatModel: '', embedModel: '' })
const savedKeyHint = ref('')
const showKey = ref(false)
const saving = ref(false)
const testing = ref(false)
const clearing = ref(false)
const message = ref('')
const ok = ref(true)

function applySettings(data) {
  Object.assign(form, {
    baseUrl: data.baseUrl || '',
    apiKey: '',
    chatModel: data.chatModel || '',
    embedModel: data.embedModel || ''
  })
  savedKeyHint.value = data.apiKey || ''
}

async function load() {
  try {
    applySettings(await getAiSettings())
  } catch {
    ok.value = false
    message.value = '加载配置失败'
  }
}

async function save() {
  saving.value = true
  message.value = ''
  try {
    applySettings(await saveAiSettings({ ...form }))
    ok.value = true
    message.value = '配置已保存，API Key 已加密'
  } catch (e) {
    ok.value = false
    message.value = e.response?.data?.message || e.message || '保存失败'
  } finally {
    saving.value = false
  }
}

async function clear() {
  clearing.value = true
  message.value = ''
  try {
    await clearAiSettings()
    await load()
    ok.value = true
    message.value = '已清空，恢复使用后端默认配置'
  } catch (e) {
    ok.value = false
    message.value = e.response?.data?.message || e.message || '清空失败'
  } finally {
    clearing.value = false
  }
}

async function test() {
  testing.value = true
  message.value = ''
  try {
    if (form.apiKey) await saveAiSettings({ ...form })
    const { tasks } = await aiPost('/ai/parse', { text: '明天上午十点开会' })
    ok.value = true
    message.value = `连接成功，解析出 ${tasks?.length || 0} 个任务`
  } catch (e) {
    ok.value = false
    message.value = e.response?.data?.message || e.message || '连接失败'
  } finally {
    testing.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.page { position: relative; z-index: 1; }
.content { max-width: 640px; margin: 0 auto; }
.glass-card { padding: 28px; position: relative; }
.head h2 { margin: 0 0 6px; font-size: 22px; color: var(--text-primary); }
.head p { margin: 0 0 22px; font-size: 13px; color: var(--text-secondary); }
.field { display: flex; flex-direction: column; gap: 6px; margin-bottom: 16px; position: relative; }
.field label { font-size: 12px; color: var(--text-secondary); }
.field .glass-input { width: 100%; }
.key-row { display: flex; gap: 8px; }
.key-row .glass-input { flex: 1; min-width: 0; }
.key-row .glass-button { flex: 0 0 auto; padding: 8px 14px; }
.field-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.note { font-size: 12px; color: var(--text-secondary); margin: 4px 0 20px; }
.actions { display: flex; gap: 10px; flex-wrap: wrap; position: relative; }
.actions .glass-button { padding: 9px 16px; }
.actions .danger { color: var(--danger); }
.message { margin: 16px 0 0; font-size: 13px; color: var(--accent); position: relative; }
.message.error { color: var(--danger); }
.spinner { width: 12px; height: 12px; border: 2px solid rgba(255,255,255,.35); border-top-color: currentColor; border-radius: 50%; display: inline-block; animation: spin .7s linear infinite; margin-right: 6px; vertical-align: -1px; }
@keyframes spin { to { transform: rotate(360deg); } }
@media (max-width: 600px) { .field-grid { grid-template-columns: 1fr; } }
</style>
