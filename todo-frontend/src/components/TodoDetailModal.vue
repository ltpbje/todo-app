<template>
  <div class="glass-overlay" @click.self="$emit('close')">
    <div class="glass-modal detail-modal" v-glass-glow>
      <div class="glare glass-glare"></div>

      <div class="modal-head">
        <h3 :class="{ done: todo.completed }">{{ todo.title }}</h3>
        <button class="glass-button close-btn" @click="$emit('close')">✕</button>
      </div>

      <div class="meta-grid">
        <label>安排到
          <DatePicker v-model="dueDate" />
        </label>
        <label>清单
          <input v-model.trim="category" class="glass-input" list="modal-categories" placeholder="默认" />
          <datalist id="modal-categories"><option v-for="c in categories" :key="c" :value="c" /></datalist>
        </label>
        <label>优先级
          <SelectPicker v-model="priority" :options="priorityOptions" label="优先级" />
        </label>
        <label>重复
          <SelectPicker v-model="repeatRule" :options="repeatOptions" label="重复" />
        </label>
        <label class="meta-wide">提醒时间
          <input v-model="remindAt" type="datetime-local" class="glass-input" />
        </label>
      </div>

      <div class="subtasks">
        <div class="sub-head"><strong>子任务</strong><span>{{ doneSubs.length }} / {{ subs.length }}</span></div>
        <label v-for="sub in subs" :key="sub.id" class="sub-task">
          <input type="checkbox" :checked="sub.completed" @change="toggleSub(sub)" />
          <span :class="{ done: sub.completed }">{{ sub.title }}</span>
          <button type="button" class="sub-del" aria-label="删除子任务" @click="removeSub(sub)">×</button>
        </label>
        <form class="sub-add" @submit.prevent="addSub">
          <input v-model.trim="subtaskTitle" class="glass-input" placeholder="添加子任务，回车确认" />
          <button type="submit" class="glass-button" :disabled="!subtaskTitle">添加</button>
        </form>
      </div>

      <div class="toolbar">
        <span class="hint">点击任意段落就地编辑 · 空白处点击新建段落</span>
        <span class="spacer"></span>
        <button class="glass-button" :disabled="uploading" @click="fileInput.click()" title="插入图片">
          <span v-if="uploading" class="spinner"></span>
          <svg v-else viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="18" height="18" rx="2" /><circle cx="8.5" cy="8.5" r="1.5" /><path d="M21 15l-5-5L5 21" /></svg>
          {{ uploading ? '上传中' : '插图' }}
        </button>
        <button class="glass-button" :disabled="breaking" @click="breakdown" title="AI 把这个任务拆解为子任务">
          {{ breaking ? '拆解中…' : '✨ AI 拆解' }}
        </button>
        <input ref="fileInput" type="file" accept="image/png,image/jpeg,image/gif,image/webp" hidden @change="upload" />
      </div>

      <div v-if="bdOpen" class="bd-panel">
        <p v-if="bdError" class="error">{{ bdError }}</p>
        <template v-if="bdTasks.length">
          <label v-for="(t, i) in bdTasks" :key="i" class="bd-task">
            <input type="checkbox" v-model="t.checked" />
            <input v-model.trim="t.text" class="glass-input bd-text" />
          </label>
          <button class="glass-button accent bd-add" :disabled="!selectedCount || bdSaving" @click="addSelected">
            <span v-if="bdSaving" class="spinner"></span>
            添加选中（{{ selectedCount }}）
          </button>
        </template>
        <pre v-else-if="bdStream" class="bd-stream">{{ bdStream }}</pre>
      </div>

      <!-- Typora 式单栏：渲染后的块，点击就地编辑 -->
      <div class="doc md-body" @click.self="editAt(blocks.length)">
        <template v-for="(block, i) in blocks" :key="i">
          <textarea
            v-if="editing === i"
            ref="editors"
            v-model="draft"
            class="block-editor"
            @blur="commit(i)"
            @keydown.esc.prevent="editing = null"
            @input="autosize"
            @paste="onPaste"
          ></textarea>
          <div
            v-else
            class="block md-body"
            :class="{ empty: !block.trim() }"
            @click="editAt(i)"
            v-html="renderBlock(i)"
          ></div>
        </template>
        <textarea
          v-if="editing === blocks.length"
          ref="editors"
          v-model="draft"
          class="block-editor"
          @blur="commit(blocks.length)"
          @keydown.esc.prevent="editing = null"
          @input="autosize"
          @paste="onPaste"
        ></textarea>
        <div v-if="editing !== blocks.length && !blocks.length" class="block empty placeholder" @click="editAt(0)">
          开始编写详情…支持 ![图片](url)、**粗体**、`代码`、## 标题、- 列表
        </div>
      </div>

      <p v-if="error" class="error">{{ error }}</p>

      <div class="modal-foot">
        <button class="glass-button" @click="$emit('close')">取消</button>
        <button class="glass-button accent" :disabled="saving" @click="save">
          <span v-if="saving" class="spinner"></span>
          {{ saving ? '保存中' : '保存' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, ref } from 'vue'
import api from '../api/request'
import { aiStream } from '../api/ai'
import { renderMarkdown } from '../utils/markdown.js'
import DatePicker from './DatePicker.vue'
import SelectPicker from './SelectPicker.vue'

const props = defineProps({
  todo: { type: Object, required: true },
  todos: { type: Array, default: () => [] }
})
const emit = defineEmits(['close', 'saved', 'savedList', 'changed'])

const priorityOptions = [
  { label: '普通', value: 0 }, { label: '重要', value: 1 }, { label: '紧急', value: 2 }
]
const repeatOptions = [
  { label: '不重复', value: 'none' }, { label: '每天', value: 'daily' }, { label: '每周', value: 'weekly' }
]

// --- AI 拆解 ---
const bdOpen = ref(false)
const bdStream = ref('')
const bdTasks = ref([])
const bdError = ref('')
const bdSaving = ref(false)
const breaking = ref(false)
const selectedCount = computed(() => bdTasks.value.filter(t => t.checked && t.text).length)

async function breakdown() {
  bdOpen.value = true
  bdStream.value = ''
  bdTasks.value = []
  bdError.value = ''
  breaking.value = true
  try {
    await aiStream('/ai/breakdown', { title: props.todo.title, description: props.todo.description || '' },
      d => { bdStream.value += d })
    bdTasks.value = bdStream.value
      .split('\n')
      .map(s => s.replace(/^[-*\d.、\s]+/, '').trim())
      .filter(Boolean)
      .map(text => ({ text, checked: true }))
    if (!bdTasks.value.length) bdError.value = '没有解析到子任务，换个标题试试'
  } catch (e) {
    bdError.value = e.message
  } finally {
    breaking.value = false
  }
}

async function addSelected() {
  bdSaving.value = true
  bdError.value = ''
  try {
    const items = bdTasks.value.filter(t => t.checked && t.text).map(t => ({
      title: t.text,
      parentId: props.todo.id,
      dueDate: props.todo.dueDate || null,
      category: props.todo.category || null,
      completed: false
    }))
    const { data } = await api.post('/todos/batch', items)
    emit('savedList', data)
    emit('changed')
    bdOpen.value = false
  } catch (e) {
    bdError.value = e.response?.data?.message || '添加失败'
  } finally {
    bdSaving.value = false
  }
}

// -- 块模型：把原始 markdown 切成可独立编辑的块 ---------------
// 以空行分隔段落；围栏代码块整体保持为一块
function splitBlocks(src) {
  if (!src) return []
  const lines = src.split(/\r?\n/)
  const out = []
  let cur = []
  let fence = false
  for (const line of lines) {
    if (/^```/.test(line.trim())) fence = !fence
    if (!fence && !line.trim()) { if (cur.length) out.push(cur.join('\n')); cur = []; continue }
    cur.push(line)
  }
  if (cur.length) out.push(cur.join('\n'))
  return out
}

const blocks = ref(splitBlocks(props.todo.description))
const dueDate = ref(props.todo.dueDate || '')
const category = ref(props.todo.category || '')
const priority = ref(props.todo.priority || 0)
const repeatRule = ref(props.todo.repeatRule || 'none')
const remindAt = ref(props.todo.remindAt ? String(props.todo.remindAt).slice(0, 16) : '')
const subtaskTitle = ref('')
const editing = ref(null)
const draft = ref('')
const uploading = ref(false)
const saving = ref(false)
const error = ref('')
const fileInput = ref(null)
const editors = ref([])

const categories = computed(() => [...new Set(props.todos.map(t => t.category).filter(Boolean))].sort())
const subs = computed(() => props.todos.filter(t => t.parentId === props.todo.id))
const doneSubs = computed(() => subs.value.filter(t => t.completed))

async function addSub() {
  if (!subtaskTitle.value) return
  const title = subtaskTitle.value
  subtaskTitle.value = ''
  try {
    await api.post('/todos', {
      title,
      parentId: props.todo.id,
      dueDate: props.todo.dueDate || null,
      category: props.todo.category || null,
      priority: 0,
      completed: false
    })
    emit('changed')
  } catch (e) {
    error.value = e.response?.data?.message || '添加子任务失败'
  }
}

async function toggleSub(sub) {
  try {
    await api.put(`/todos/${sub.id}`, { ...sub, completed: !sub.completed })
    emit('changed')
  } catch {
    error.value = '更新子任务失败'
  }
}

async function removeSub(sub) {
  try {
    await api.delete(`/todos/${sub.id}`)
    emit('changed')
  } catch {
    error.value = '删除子任务失败'
  }
}

const renderBlock = (i) => renderMarkdown(blocks.value[i])

function editAt(i) {
  editing.value = i
  draft.value = blocks.value[i] || ''
  requestAnimationFrame(() => {
    const el = Array.isArray(editors.value) ? editors.value[0] : editors.value
    if (el) { el.focus(); el.style.height = el.scrollHeight + 'px' }
  })
}

function commit(i) {
  if (editing.value !== i) return
  const text = draft.value.replace(/\s+$/, '')
  if (text.trim()) blocks.value[i] = text
  else blocks.value.splice(i, 1) // emptied block is deleted
  editing.value = null
}

function autosize(e) { e.target.style.height = 'auto'; e.target.style.height = e.target.scrollHeight + 'px' }

// 直接从剪贴板粘贴图片（截图 → Ctrl+V）
async function onPaste(e) {
  const item = [...(e.clipboardData?.items || [])].find(i => i.type.startsWith('image/'))
  if (!item) return // 普通文本粘贴
  e.preventDefault()
  const file = item.getAsFile()
  if (!file) return
  uploading.value = true
  error.value = ''
  const el = e.target
  const start = el.selectionStart ?? draft.value.length
  const end = el.selectionEnd ?? start
  try {
    const fd = new FormData()
    fd.append('file', file)
    const { data } = await api.post('/uploads', fd)
    const md = `![${file.name === 'image.png' ? 'paste' : file.name}](${data.url})`
    draft.value = draft.value.slice(0, start) + md + draft.value.slice(end)
    nextTick(() => { el.focus(); el.selectionStart = el.selectionEnd = start + md.length })
  } catch (err) {
    error.value = err.response?.data?.message || '上传失败'
  } finally {
    uploading.value = false
  }
}

async function upload(e) {
  const file = e.target.files?.[0]
  e.target.value = ''
  if (!file || uploading.value) return
  uploading.value = true
  error.value = ''
  try {
    const fd = new FormData()
    fd.append('file', file)
    const { data } = await api.post('/uploads', fd)
    blocks.value.push(`![${file.name}](${data.url})`)
    editAt(blocks.value.length - 1)
  } catch (err) {
    error.value = err.response?.data?.message || '上传失败'
  } finally {
    uploading.value = false
  }
}

async function save() {
  if (saving.value) return
  saving.value = true
  error.value = ''
  try {
    const description = blocks.value.join('\n\n')
    const { data } = await api.put(`/todos/${props.todo.id}`, {
      ...props.todo,
      dueDate: dueDate.value || null,
      category: category.value || null,
      priority: priority.value,
      repeatRule: repeatRule.value,
      remindAt: remindAt.value || null,
      description
    })
    emit('saved', data)
    emit('close')
  } catch (err) {
    error.value = err.response?.data?.message || '保存失败'
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.detail-modal {
  display: flex;
  flex-direction: column;
  max-height: 82vh;
}

.modal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
  position: relative;
}
.modal-head h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  word-break: break-all;
}
.modal-head h3.done { text-decoration: line-through; color: var(--text-secondary); }
.close-btn { padding: 6px 10px; }

.meta-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px; margin-bottom: 12px; position: relative; }
.meta-grid label { display: flex; flex-direction: column; gap: 4px; font-size: 12px; color: var(--text-secondary); }
.meta-grid .meta-wide { grid-column: span 2; }
.meta-grid .glass-input { padding: 6px 10px; font-size: 13px; }

.subtasks {
  border: 1px solid var(--glass-border);
  border-radius: var(--glass-radius-sm);
  background: var(--glass-bg);
  padding: 10px 12px;
  margin-bottom: 12px;
  position: relative;
}
.sub-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; }
.sub-head strong { font-size: 13px; color: var(--text-primary); }
.sub-head span { font-size: 12px; color: var(--accent); }
.sub-task { display: flex; align-items: center; gap: 8px; font-size: 13px; color: var(--text-primary); padding: 3px 0; }
.sub-task span { flex: 1; }
.sub-task span.done { text-decoration: line-through; color: var(--text-secondary); }
.sub-del { background: none; border: none; color: var(--text-secondary); cursor: pointer; font-size: 16px; }
.sub-add { display: flex; gap: 8px; margin-top: 8px; }
.sub-add .glass-input { flex: 1; padding: 6px 10px; font-size: 13px; }
.sub-add .glass-button { padding: 6px 12px; font-size: 13px; }

.toolbar {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 10px;
  position: relative;
}
.toolbar .hint { font-size: 12px; color: var(--text-secondary); }
.toolbar .spacer { flex: 1; }

/* AI 拆解面板 */
.bd-panel {
  border: 1px solid var(--glass-border);
  border-radius: var(--glass-radius-sm);
  background: var(--glass-bg);
  padding: 12px;
  margin-bottom: 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  position: relative;
}
.bd-task { display: flex; align-items: center; gap: 8px; }
.bd-task .bd-text { flex: 1; padding: 6px 10px; font-size: 13.5px; }
.bd-stream {
  margin: 0;
  font-family: inherit;
  font-size: 13px;
  color: var(--text-secondary);
  white-space: pre-wrap;
  max-height: 160px;
  overflow: auto;
}
.bd-add { align-self: flex-end; }

/* Typora 风格的文档面板 */
.doc {
  flex: 1;
  min-height: 280px;
  max-height: 56vh;
  overflow: auto;
  padding: 14px 16px;
  border: 1px solid var(--glass-border);
  border-radius: var(--glass-radius-sm);
  background: var(--glass-bg);
  cursor: text;
  position: relative;
}
.block { min-height: 1em; cursor: text; }
.block.empty { color: var(--text-secondary); }
.placeholder { opacity: 0.7; }

.block-editor {
  width: 100%;
  border: none;
  outline: none;
  resize: none;
  overflow: hidden;
  background: var(--glass-bg-strong);
  border-radius: 8px;
  padding: 4px 8px;
  font-family: inherit;
  font-size: 14px;
  line-height: 1.7;
  color: var(--text-primary);
  box-shadow: var(--accent-ring);
  box-sizing: border-box;
}

.error { color: var(--danger); font-size: 13px; margin: 8px 0 0; position: relative; }

.modal-foot {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 14px;
  position: relative;
}

@media (max-width: 600px) {
  .meta-grid { grid-template-columns: repeat(2, 1fr); }
}
</style>

<style>
/* markdown 正文样式——不加 scoped，因为 v-html 的内容没有 data-v 属性 */
.md-body {
  font-size: 14px;
  line-height: 1.7;
  color: var(--text-primary);
  word-break: break-word;
}
.md-body h1, .md-body h2, .md-body h3 { margin: 10px 0 4px; }
.md-body h1 { font-size: 19px; } .md-body h2 { font-size: 17px; } .md-body h3 { font-size: 15px; }
.md-body p { margin: 6px 0; }
.md-body img {
  max-width: 100%;
  border-radius: 12px;
  border: 1px solid var(--glass-border);
  box-shadow: var(--glass-shadow);
}
.md-body code {
  font-family: 'Cascadia Code', Consolas, monospace;
  font-size: 12.5px;
  background: var(--glass-bg-strong);
  border: 1px solid var(--glass-border);
  border-radius: 6px;
  padding: 1px 5px;
}
.md-body pre {
  background: var(--glass-bg-strong);
  border: 1px solid var(--glass-border);
  border-radius: 12px;
  padding: 12px 14px;
  overflow: auto;
}
.md-body pre code { background: none; border: none; padding: 0; }
.md-body a { color: var(--accent); }
.md-body ul { padding-left: 22px; margin: 6px 0; }
</style>
