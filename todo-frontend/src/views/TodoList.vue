<template>
  <div class="page">
    <div class="content">
      <GlassObject class="glass-card" v-glass-glow>
        <div class="glare glass-glare"></div>
        <form class="add-form" @submit.prevent="add">
          <div class="add-main">
            <input v-model.trim="newTitle" class="glass-input" placeholder="添加新任务，回车确认…" :disabled="adding" />
            <DatePicker v-model="newDue" class="due-input" />
            <button type="button" class="glass-button options-btn" :class="{ active: showOptions }" :aria-expanded="showOptions" @click="showOptions = !showOptions">选项</button>
            <button type="submit" class="glass-button glass-glossy" v-glass-glow :disabled="!newTitle || adding">
              <span v-if="adding" class="spinner"></span>
              {{ adding ? '添加中' : '添加' }}
            </button>
          </div>
          <div v-if="showOptions" class="add-meta">
            <input v-model.trim="newCategory" class="glass-input" list="category-options" placeholder="清单（默认）" aria-label="清单" />
            <datalist id="category-options"><option v-for="c in categories" :key="c" :value="c" /></datalist>
            <SelectPicker v-model="newPriority" :options="priorityOptions" label="优先级" class="meta-select" />
            <SelectPicker v-model="newRepeat" :options="repeatOptions" label="重复" class="meta-select" />
          </div>
        </form>

        <div class="ai-bar">
          <button class="glass-pill" :class="{ active: aiPanel }" @click="aiPanel = !aiPanel">✨ AI 添加</button>
          <input
            v-model.trim="searchQ"
            class="glass-input search-input"
            placeholder="搜索任务（回车，AI 语义搜索）"
            @keyup.enter="doSearch"
            @input="onSearchInput"
          />
          <button v-if="searchResults" class="glass-pill" @click="clearSearch">清除搜索</button>
        </div>

        <div v-if="aiPanel" class="ai-panel">
          <textarea
            v-model.trim="nlText"
            class="glass-input nl-input"
            rows="2"
            placeholder="用自然语言描述，例如：明天下午3点部门会议；周五前写完周报"
          ></textarea>
          <button class="glass-button accent" :disabled="!nlText || parsing" @click="parseNl">
            <span v-if="parsing" class="spinner"></span>
            {{ parsing ? '解析中' : '解析' }}
          </button>
          <p v-if="parseError" class="error">{{ parseError }}</p>
          <div v-if="parsed.length" class="nl-preview">
            <label v-for="(t, i) in parsed" :key="i" class="nl-task">
              <input type="checkbox" v-model="t.checked" />
              <span>{{ t.title }}</span>
              <span v-if="t.dueDate" class="due">{{ t.dueDate }}</span>
            </label>
            <button class="glass-button accent" :disabled="!parsedCount || addingBatch" @click="addParsed">
              <span v-if="addingBatch" class="spinner"></span>
              添加选中（{{ parsedCount }}）
            </button>
          </div>
        </div>

        <div class="filters">
          <button
            v-for="opt in views"
            :key="opt.key"
            class="glass-pill"
            :class="{ active: view === opt.key }"
            @click="view = opt.key"
          >{{ opt.label }}</button>
          <SelectPicker v-model="categoryFilter" :options="categoryOptions" label="筛选清单" class="filter-select" />
          <button v-if="dateFilter" class="glass-pill" @click="dateFilter = null">{{ dateFilter }} ×</button>
          <span v-if="searching" class="spinner"></span>
        </div>

        <div v-if="view === 'today'" class="ivy">
          <button type="button" class="ivy-head" :aria-expanded="ivyOpen" @click="ivyOpen = !ivyOpen">
            <strong>今日六件事</strong>
            <span>{{ todayDone }}/6 · {{ ivyOpen ? '收起' : '展开' }}</span>
          </button>
          <div class="ivy-progress"><i :style="{ width: Math.min(todayDone / 6 * 100, 100) + '%' }"></i></div>
          <template v-if="ivyOpen">
            <ol v-if="todayTasks.length">
              <li v-for="(t, i) in todayTasks.slice(0, 6)" :key="t.id" :class="{ done: t.completed }">
                <span class="ivy-no">{{ i + 1 }}</span>{{ t.title }}
              </li>
            </ol>
            <p v-else class="ivy-empty">按重要性拖动排序，先完成前六件</p>
            <p v-if="todayTasks.length > 6" class="ivy-warn">今天超过 6 件，先专注前六件，其余安排到明天</p>
          </template>
        </div>

        <p v-if="error" class="error">{{ error }}</p>
        <p v-if="!loading && !filtered.length" class="empty">
          {{ view === 'all' ? '还没有任务，添加一个吧' : view === 'today' ? '今天还没有任务，添加一个或到「全部」里安排' : '这个分类下暂无任务' }}
        </p>

        <transition-group name="list" tag="ul" class="todo-list">
          <li
            v-for="todo in filtered"
            :key="todo.id"
            :class="{ done: todo.completed, dragging: dragId === todo.id }"
            :draggable="!searchResults"
            @dragstart="onDragStart(todo, $event)"
            @dragover="onDragOver($event)"
            @drop="onDrop(todo, $event)"
            @dragend="dragId = null"
          >
            <button class="check" :title="todo.completed ? '标记为未完成' : '标记为完成'" @click="toggle(todo)">
              <svg v-if="todo.completed" viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><path d="M20 6L9 17l-5-5" /></svg>
            </button>
            <span v-if="editingId !== todo.id" class="title" @dblclick="startEdit(todo)">{{ todo.title }}</span>
            <span v-if="todo.dueDate" class="due-chip">{{ dueLabel(todo.dueDate) }}</span>
            <span v-if="todo.category" class="cat-chip">{{ todo.category }}</span>
            <span v-if="todo.priority" class="pri-chip" :class="'p' + todo.priority">{{ ['', '重要', '紧急'][todo.priority] }}</span>
            <span v-if="subProgress(todo)" class="sub-chip">{{ subProgress(todo) }}</span>
            <span v-if="todo.repeatRule && todo.repeatRule !== 'none'" class="repeat-chip">{{ todo.repeatRule === 'daily' ? '每天' : '每周' }}</span>
            <span v-if="todo.pomodoroCount" class="pomodoro-chip" title="已完成番茄数">番茄 ×{{ todo.pomodoroCount }}</span>
            <input
              v-if="editingId === todo.id"
              v-model.trim="editTitle"
              class="edit glass-input"
              v-focus
              @keyup.enter="saveEdit(todo)"
              @keyup.esc="cancelEdit"
              @blur="saveEdit(todo)"
            />
            <button class="del" title="详情" @click="detailTodo = todo">
              <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" /><path d="M14 2v6h6M9 13h6M9 17h6" /></svg>
            </button>
            <button class="del" title="删除任务" :disabled="deletingId === todo.id" @click="remove(todo)">
              <span v-if="deletingId === todo.id" class="spinner spinner-dark"></span>
              <svg v-else viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 6h18M8 6V4a1 1 0 0 1 1-1h6a1 1 0 0 1 1 1v2m3 0v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6" /></svg>
            </button>
          </li>
        </transition-group>
      </GlassObject>

      <PomodoroTimer :todos="todos" @finish="onPomodoro" />

      <TodoDetailModal
        v-if="detailTodo"
        :todo="detailTodo"
        :todos="todos"
        @close="detailTodo = null"
        @saved="onDetailSaved"
        @saved-list="onSavedList"
        @changed="load"
      />
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import api from '../api/request'
import { aiPost } from '../api/ai'
import TodoDetailModal from '../components/TodoDetailModal.vue'
import DatePicker from '../components/DatePicker.vue'
import SelectPicker from '../components/SelectPicker.vue'
import PomodoroTimer from '../components/PomodoroTimer.vue'
import GlassObject from '../components/GlassObject.vue'
import { dateKey, completedOn } from '../utils/plan'

const route = useRoute()
const todos = ref([])
const detailTodo = ref(null)
const views = [
  { key: 'today', label: '今天' },
  { key: 'tomorrow', label: '明天' },
  { key: 'all', label: '全部' },
  { key: 'done', label: '已完成' }
]
const view = ref('today')
const newDue = ref(dateKey(0))
const showOptions = ref(false)
const ivyOpen = ref(false)
const newCategory = ref('')
const newPriority = ref(0)
const newRepeat = ref('none')
const priorityOptions = [
  { label: '普通', value: 0 }, { label: '重要', value: 1 }, { label: '紧急', value: 2 }
]
const repeatOptions = [
  { label: '不重复', value: 'none' }, { label: '每天', value: 'daily' }, { label: '每周', value: 'weekly' }
]
const categoryFilter = ref('all')
const dateFilter = ref(null)
const dragId = ref(null)
const notifiedIds = new Set()

function onDetailSaved(data) {
  const idx = todos.value.findIndex(t => t.id === data.id)
  if (idx !== -1) Object.assign(todos.value[idx], data)
}
function onSavedList(list) { todos.value.unshift(...list) }

// --- AI：自然语言添加 ---
const aiPanel = ref(false)
const nlText = ref('')
const parsed = ref([])
const parsing = ref(false)
const parseError = ref('')
const addingBatch = ref(false)
const parsedCount = computed(() => parsed.value.filter(t => t.checked).length)

async function parseNl() {
  parsing.value = true
  parseError.value = ''
  try {
    const { tasks } = await aiPost('/ai/parse', { text: nlText.value })
    parsed.value = (tasks || []).map(t => ({ ...t, checked: true }))
    if (!parsed.value.length) parseError.value = '没有解析出任务，换个说法试试'
  } catch (e) {
    parseError.value = e.response?.data?.message || e.message || '解析失败'
  } finally {
    parsing.value = false
  }
}

async function addParsed() {
  addingBatch.value = true
  try {
    const items = parsed.value.filter(t => t.checked).map(t => ({
      title: t.title,
      dueDate: /^\d{4}-\d{2}-\d{2}/.test(t.dueDate || '') ? String(t.dueDate).slice(0, 10) : null,
      completed: false
    }))
    const { data } = await api.post('/todos/batch', items)
    todos.value.unshift(...data)
    parsed.value = []
    nlText.value = ''
  } catch (e) {
    parseError.value = e.response?.data?.message || '添加失败'
  } finally {
    addingBatch.value = false
  }
}

// --- AI 语义搜索 ---
const searchQ = ref('')
const searchResults = ref(null)
const searching = ref(false)

async function doSearch() {
  if (!searchQ.value) { searchResults.value = null; return }
  searching.value = true
  try {
    searchResults.value = await aiPost('/ai/search', { q: searchQ.value })
  } catch (e) {
    error.value = e.response?.data?.message || e.message || '搜索失败'
  } finally {
    searching.value = false
  }
}
function onSearchInput() { if (!searchQ.value) searchResults.value = null }
function clearSearch() { searchQ.value = ''; searchResults.value = null }
const newTitle = ref('')
const loading = ref(true)
const adding = ref(false)
const deletingId = ref(null)
const error = ref('')
const editingId = ref(null)
const editTitle = ref('')

const vFocus = { mounted: (el) => el.focus() }

const categories = computed(() => [...new Set(todos.value.map(t => t.category).filter(Boolean))].sort())
const categoryOptions = computed(() => [{ label: '全部清单', value: 'all' }, ...categories.value.map(c => ({ label: c, value: c }))])

const filtered = computed(() => {
  if (searchResults.value) return searchResults.value
  let list = todos.value
  if (dateFilter.value) list = list.filter(t => t.dueDate === dateFilter.value)
  else if (view.value === 'today') list = list.filter(t => t.dueDate === dateKey(0))
  else if (view.value === 'tomorrow') list = list.filter(t => t.dueDate === dateKey(1))
  else if (view.value === 'done') list = list.filter(t => t.completed)
  if (categoryFilter.value !== 'all') list = list.filter(t => (t.category || '') === categoryFilter.value)
  return list
})

const todayTasks = computed(() => todos.value.filter(t => !t.parentId && t.dueDate === dateKey(0)))
const todayDone = computed(() => todos.value.filter(t => !t.parentId && completedOn(t, dateKey(0))).length)

function subProgress(todo) {
  const subs = todos.value.filter(s => s.parentId === todo.id)
  if (!subs.length) return ''
  return `${subs.filter(s => s.completed).length}/${subs.length} 子任务`
}

function dueLabel(value) {
  if (value === dateKey(0)) return '今天'
  if (value === dateKey(1)) return '明天'
  return value.slice(5)
}

function checkReminders() {
  if (typeof Notification === 'undefined' || Notification.permission !== 'granted') return
  const now = Date.now()
  for (const t of todos.value) {
    if (!t.remindAt || t.completed || notifiedIds.has(t.id)) continue
    const when = new Date(t.remindAt).getTime()
    if (when <= now && now - when < 3600000) {
      new Notification(`任务提醒：${t.title}`, { body: t.dueDate ? `截止 ${t.dueDate}` : '该处理这个任务了' })
      notifiedIds.add(t.id)
    }
  }
}

function onDragStart(todo, e) {
  if (searchResults.value) return
  dragId.value = todo.id
  e.dataTransfer.effectAllowed = 'move'
  e.dataTransfer.setData('text/plain', String(todo.id))
}
function onDragOver(e) { if (dragId.value) e.preventDefault() }
async function onDrop(target, e) {
  e.preventDefault()
  const id = dragId.value || Number(e.dataTransfer.getData('text/plain'))
  dragId.value = null
  if (!id || id === target.id) return
  const from = todos.value.findIndex(t => t.id === id)
  const to = todos.value.findIndex(t => t.id === target.id)
  if (from < 0 || to < 0) return
  const [moved] = todos.value.splice(from, 1)
  todos.value.splice(to, 0, moved)
  try {
    const { data } = await api.put('/todos/reorder', todos.value.map(t => t.id))
    todos.value = data
  } catch {
    error.value = '排序保存失败'
  }
}

async function onPomodoro(id) {
  if (!id) return
  try {
    const { data } = await api.post(`/todos/${id}/pomodoro`)
    const idx = todos.value.findIndex(t => t.id === data.id)
    if (idx !== -1) Object.assign(todos.value[idx], data)
  } catch {
    error.value = '番茄记录失败'
  }
}

async function load() {
  loading.value = true
  try {
    todos.value = (await api.get('/todos')).data
    checkReminders()
  } catch {
    error.value = '加载任务失败'
  } finally {
    loading.value = false
  }
}

async function add() {
  if (!newTitle.value || adding.value) return
  adding.value = true
  try {
    const { data } = await api.post('/todos', {
      title: newTitle.value,
      dueDate: newDue.value || null,
      category: newCategory.value || null,
      priority: newPriority.value,
      repeatRule: newRepeat.value,
      completed: false
    })
    todos.value.unshift(data)
    newTitle.value = ''
  } catch (e) {
    error.value = e.response?.data?.message || '添加失败'
  } finally {
    adding.value = false
  }
}

async function toggle(todo) {
  try {
    const { data } = await api.put(`/todos/${todo.id}`, { ...todo, completed: !todo.completed })
    Object.assign(todo, data)
    if (data.repeatRule && data.repeatRule !== 'none' && data.completed) await load()
  } catch (e) {
    error.value = '更新失败'
  }
}

async function remove(todo) {
  if (deletingId.value) return
  deletingId.value = todo.id
  try {
    await api.delete(`/todos/${todo.id}`)
    todos.value = todos.value.filter(t => t.id !== todo.id)
  } catch (e) {
    error.value = '删除失败'
  } finally {
    deletingId.value = null
  }
}

function startEdit(todo) {
  editingId.value = todo.id
  editTitle.value = todo.title
}

function cancelEdit() {
  editingId.value = null
}

async function saveEdit(todo) {
  if (editingId.value !== todo.id) return
  editingId.value = null
  if (!editTitle.value || editTitle.value === todo.title) return
  try {
    const { data } = await api.put(`/todos/${todo.id}`, { ...todo, title: editTitle.value })
    Object.assign(todo, data)
  } catch (e) {
    error.value = '更新失败'
  }
}

let reminderTimer = null
onMounted(() => {
  if (route.query.date) {
    dateFilter.value = String(route.query.date)
    view.value = 'all'
  }
  if (typeof Notification !== 'undefined' && Notification.permission === 'default') Notification.requestPermission()
  load()
  reminderTimer = setInterval(checkReminders, 30000)
})
onUnmounted(() => clearInterval(reminderTimer))
</script>

<style scoped>
.page { position: relative; z-index: 1; }
.content { max-width: 720px; margin: 0 auto; }
.glass-card { padding: 24px; }

.add-form { display: flex; flex-direction: column; gap: 8px; margin-bottom: 16px; position: relative; }
.add-main, .add-meta { display: flex; gap: 8px; align-items: center; }
.add-form .glass-input { flex: 1; min-width: 0; }
.add-meta .glass-input { padding: 8px 10px; font-size: 13px; }
.meta-select { flex: 0 0 92px; }
.due-input { flex: 0 0 140px; padding: 10px; font-size: 14px; }
.options-btn { flex: 0 0 auto; padding: 9px 12px; font-size: 13px; }
.options-btn.active { background: var(--accent-grad); color: #fff; }

.spinner {
  width: 14px;
  height: 14px;
  flex-shrink: 0;
  border: 2px solid rgba(255, 255, 255, 0.35);
  border-top-color: currentColor;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
  display: inline-block;
  vertical-align: -2px;
  margin-right: 6px;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* 删除按钮用的深色 spinner（跟随图标颜色，而非白色） */
.spinner-dark { border-color: rgba(93, 93, 120, 0.25); border-top-color: currentColor; }

.filters { display: flex; gap: 8px; margin-bottom: 16px; position: relative; align-items: center; flex-wrap: wrap; }
.filter-select { flex: 0 0 112px; }
.filter-select .select-trigger { padding: 7px 10px; font-size: 13px; }

.ivy {
  border: 1px solid var(--glass-border);
  border-radius: var(--glass-radius-sm);
  background: var(--glass-bg);
  padding: 10px 14px 12px;
  margin-bottom: 16px;
  position: relative;
}
.ivy-head { width: 100%; display: flex; justify-content: space-between; align-items: center; padding: 0; border: none; background: none; cursor: pointer; }
.ivy-head strong { color: var(--text-primary); font-size: 14px; }
.ivy-head span { font-size: 12px; color: var(--accent); }
.ivy-progress { height: 4px; border-radius: 999px; background: var(--glass-bg-strong); overflow: hidden; margin-top: 8px; }
.ivy-progress i { display: block; height: 100%; border-radius: 999px; background: var(--accent-grad); }
.ivy ol { margin: 10px 0 0; padding-left: 4px; list-style: none; }
.ivy li { display: flex; align-items: center; gap: 8px; font-size: 13px; color: var(--text-primary); padding: 3px 0; }
.ivy li.done { text-decoration: line-through; color: var(--text-secondary); }
.ivy-no { display: inline-flex; width: 18px; height: 18px; align-items: center; justify-content: center; border-radius: 50%; background: var(--accent-grad); color: #fff; font-size: 11px; flex-shrink: 0; }
.ivy-empty, .ivy-warn { margin: 4px 0 0; font-size: 12px; color: var(--text-secondary); }
.ivy-warn { color: var(--danger); }

.ai-bar { display: flex; gap: 8px; margin-bottom: 16px; position: relative; }
.ai-bar .search-input { flex: 1; padding: 8px 12px; }

.ai-panel {
  border: 1px solid var(--glass-border);
  border-radius: var(--glass-radius-sm);
  background: var(--glass-bg);
  padding: 14px;
  margin-bottom: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.ai-panel .nl-input { width: 100%; resize: vertical; }
.nl-preview { display: flex; flex-direction: column; gap: 6px; }
.nl-task { display: flex; align-items: center; gap: 8px; font-size: 14px; color: var(--text-primary); cursor: pointer; }
.nl-task .due { margin-left: auto; font-size: 12px; color: var(--accent); }

.todo-list { list-style: none; padding: 0; margin: 0; position: relative; }
.todo-list li {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 10px;
  border-bottom: 1px solid var(--glass-border);
  border-radius: var(--glass-radius-sm);
  transition: background 0.15s;
}
.todo-list li:hover { background: var(--glass-bg); }
.todo-list li:last-child { border-bottom: none; }
.todo-list li[draggable="true"] { cursor: grab; }
.todo-list li.dragging { opacity: 0.4; }
.due-chip, .pomodoro-chip, .cat-chip, .pri-chip, .sub-chip, .repeat-chip {
  flex-shrink: 0;
  font-size: 11px;
  padding: 3px 8px;
  border-radius: 999px;
  background: var(--glass-bg-strong);
  color: var(--accent);
}
.pomodoro-chip, .sub-chip { color: var(--text-secondary); }
.pri-chip.p2 { background: rgba(224, 52, 75, 0.16); color: var(--danger); }

.check {
  width: 24px;
  height: 24px;
  flex-shrink: 0;
  border: 2px solid var(--glass-border);
  border-radius: 50%;
  background: var(--glass-bg-strong);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  padding: 0;
  transition: all 0.2s;
}
li.done .check {
  background: var(--accent-grad);
  border-color: transparent;
  box-shadow: 0 3px 10px rgba(110, 120, 255, 0.4);
}

.title { flex: 1; cursor: pointer; font-size: 15px; color: var(--text-primary); word-break: break-all; transition: color 0.25s; }
li.done .title { text-decoration: line-through; color: var(--text-secondary); }

.edit { flex: 1; padding: 8px 10px; }

.del {
  background: none;
  border: none;
  color: var(--text-secondary);
  opacity: 0.75;
  border-radius: 10px;
  padding: 6px;
  cursor: pointer;
  display: flex;
  transition: color 0.2s, background 0.2s;
}
.todo-list li:hover .del { color: var(--danger); opacity: 1; }
.del:hover { background: rgba(224, 52, 75, 0.12); }

.error { color: var(--danger); font-size: 13px; margin: 8px 0 0; position: relative; }
.empty { color: var(--text-secondary); text-align: center; padding: 32px 0 16px; margin: 0; font-size: 14px; position: relative; }

/* 特异性高于 `.todo-list li`（后者设置了 `transition: background`） */
.todo-list li.list-enter-active, .todo-list li.list-leave-active { transition: opacity 0.25s ease, transform 0.25s ease; }
.list-enter-from, .list-leave-to { opacity: 0; transform: translateX(-12px); }
.list-leave-active { position: absolute; width: 100%; }
.list-move { transition: transform 0.25s ease; }

@media (max-width: 600px) {
  .due-input { flex-basis: 130px; }
  .add-main, .add-meta { flex-wrap: wrap; }
  .add-meta .glass-input { flex: 1 1 45%; }
  .meta-select { flex: 1 1 45%; }
  .filter-select { flex: 1 1 100px; }
}
</style>
