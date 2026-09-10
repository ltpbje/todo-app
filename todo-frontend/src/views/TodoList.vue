<template>
  <div class="page" @mousemove="onMove">
    <div class="blob blob-1"></div>
    <div class="blob blob-2"></div>
    <div class="blob blob-3"></div>

    <div class="content">
      <div class="header">
        <div>
          <h2>我的任务</h2>
          <p class="stat" v-if="todos.length">共 {{ todos.length }} 项 · {{ doneCount }} 已完成</p>
        </div>
        <button class="logout" v-gloss @click="logout">退出登录</button>
      </div>

      <div class="card" ref="cardEl">
        <div class="glare" :style="glareStyle"></div>
        <form class="add-form" @submit.prevent="add">
          <input v-model.trim="newTitle" placeholder="添加新任务，回车确认…" />
          <button type="submit" v-gloss :disabled="!newTitle">添加</button>
        </form>

        <div class="filters">
          <button
            v-for="opt in ['all', 'active', 'done']"
            :key="opt"
            :class="{ active: filter === opt }"
            @click="filter = opt"
          >{{ { all: '全部', active: '未完成', done: '已完成' }[opt] }}</button>
        </div>

        <p v-if="error" class="error">{{ error }}</p>
        <p v-if="!loading && !filtered.length" class="empty">
          {{ filter === 'all' ? '还没有任务，添加一个吧' : '这个分类下暂无任务' }}
        </p>

        <transition-group name="list" tag="ul" class="todo-list">
          <li v-for="todo in filtered" :key="todo.id" :class="{ done: todo.completed }">
            <button class="check" :title="todo.completed ? '标记为未完成' : '标记为完成'" @click="toggle(todo)">
              <svg v-if="todo.completed" viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><path d="M20 6L9 17l-5-5" /></svg>
            </button>
            <span v-if="editingId !== todo.id" class="title" @dblclick="startEdit(todo)">{{ todo.title }}</span>
            <input
              v-else
              v-model.trim="editTitle"
              class="edit"
              v-focus
              @keyup.enter="saveEdit(todo)"
              @keyup.esc="cancelEdit"
              @blur="saveEdit(todo)"
            />
            <button class="del" title="删除任务" @click="remove(todo)">
              <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 6h18M8 6V4a1 1 0 0 1 1-1h6a1 1 0 0 1 1 1v2m3 0v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6" /></svg>
            </button>
          </li>
        </transition-group>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api/request'

const router = useRouter()
const todos = ref([])
const newTitle = ref('')
const filter = ref('all')
const loading = ref(true)
const error = ref('')
const editingId = ref(null)
const editTitle = ref('')

const vFocus = { mounted: (el) => el.focus() }

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

// soft highlight that follows the cursor inside the card
const mouse = reactive({ x: 0.5, y: 0.5 })
const cardEl = ref(null)

function onMove(e) {
  mouse.x = e.clientX / window.innerWidth
  mouse.y = e.clientY / window.innerHeight
}

// glare position tracked relative to the card
const glareStyle = computed(() => {
  if (!cardEl.value) return { opacity: 0 }
  const rect = cardEl.value.getBoundingClientRect()
  const x = (mouse.x * window.innerWidth - rect.left) / rect.width * 100
  const y = (mouse.y * window.innerHeight - rect.top) / rect.height * 100
  return {
    background: `radial-gradient(320px circle at ${x}% ${y}%, rgba(255,255,255,0.35) 0%, rgba(255,255,255,0) 60%)`
  }
})

const filtered = computed(() => {
  if (filter.value === 'active') return todos.value.filter(t => !t.completed)
  if (filter.value === 'done') return todos.value.filter(t => t.completed)
  return todos.value
})

const doneCount = computed(() => todos.value.filter(t => t.completed).length)

async function load() {
  loading.value = true
  try {
    const { data } = await api.get('/todos')
    todos.value = data
  } catch (e) {
    error.value = '加载任务失败'
  } finally {
    loading.value = false
  }
}

async function add() {
  if (!newTitle.value) return
  try {
    const { data } = await api.post('/todos', { title: newTitle.value, completed: false })
    todos.value.unshift(data)
    newTitle.value = ''
  } catch (e) {
    error.value = e.response?.data?.message || '添加失败'
  }
}

async function toggle(todo) {
  try {
    const { data } = await api.put(`/todos/${todo.id}`, { ...todo, completed: !todo.completed })
    Object.assign(todo, data)
  } catch (e) {
    error.value = '更新失败'
  }
}

async function remove(todo) {
  try {
    await api.delete(`/todos/${todo.id}`)
    todos.value = todos.value.filter(t => t.id !== todo.id)
  } catch (e) {
    error.value = '删除失败'
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

function logout() {
  localStorage.removeItem('token')
  router.push('/login')
}

onMounted(load)
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: linear-gradient(160deg, #dfe9ff 0%, #f3e8ff 45%, #ffe8f3 100%);
  padding: 32px 20px;
  box-sizing: border-box;
  position: relative;
  overflow: hidden;
}

/* floating color blobs behind the glass */
.blob {
  position: fixed;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.6;
  pointer-events: none;
  animation: float 14s ease-in-out infinite alternate;
  z-index: 0;
}
.blob-1 { width: 440px; height: 440px; background: #7aa8ff; top: -140px; left: -120px; }
.blob-2 { width: 380px; height: 380px; background: #c88bff; bottom: -120px; right: -100px; animation-delay: -5s; }
.blob-3 { width: 300px; height: 300px; background: #ffa8c9; top: 45%; left: 60%; animation-delay: -9s; }
@keyframes float {
  from { transform: translateY(0) scale(1); }
  to { transform: translateY(50px) scale(1.1); }
}

.content { position: relative; z-index: 1; max-width: 620px; margin: 0 auto; }

.header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  color: #1d1d2b;
  margin-bottom: 24px;
}
.header h2 { margin: 0; font-size: 28px; font-weight: 700; }
.stat { margin: 6px 0 0; font-size: 13px; color: #5c5c72; }
.logout {
  padding: 9px 18px;
  border: 1px solid rgba(255, 255, 255, 0.75);
  background: rgba(255, 255, 255, 0.3);
  backdrop-filter: blur(12px) saturate(160%);
  -webkit-backdrop-filter: blur(12px) saturate(160%);
  border-radius: 14px;
  font-size: 14px;
  font-weight: 600;
  color: #3a3a5c;
  cursor: pointer;
  box-shadow:
    0 4px 14px rgba(31, 38, 135, 0.1),
    inset 0 1px 0 rgba(255, 255, 255, 0.85),
    inset 0 -1px 0 rgba(255, 255, 255, 0.25);
  transition: background 0.2s, transform 0.15s, box-shadow 0.25s;
}
.logout:hover {
  background: rgba(255, 255, 255, 0.5);
  box-shadow:
    0 8px 20px rgba(31, 38, 135, 0.15),
    inset 0 1px 0 rgba(255, 255, 255, 0.9),
    inset 0 -1px 0 rgba(255, 255, 255, 0.3);
  transform: translateY(-1px);
}

.card {
  position: relative;
  background: rgba(255, 255, 255, 0.42);
  backdrop-filter: blur(28px) saturate(180%);
  -webkit-backdrop-filter: blur(28px) saturate(180%);
  border: 1px solid rgba(255, 255, 255, 0.6);
  border-radius: 28px;
  padding: 24px;
  box-shadow:
    0 8px 32px rgba(31, 38, 135, 0.15),
    inset 0 1px 0 rgba(255, 255, 255, 0.7);
  overflow: hidden;
}
.card > * { position: relative; }

/* moving highlight that tracks the cursor across the glass */
.glare {
  position: absolute;
  inset: 0;
  pointer-events: none;
  border-radius: inherit;
  z-index: 0;
}

/* gloss highlight that follows the cursor on buttons */
.glossy { position: relative; overflow: hidden; }
.glossy::after {
  content: '';
  position: absolute;
  left: var(--gx, 50%);
  top: var(--gy, 50%);
  width: 120px;
  height: 120px;
  transform: translate(-50%, -50%);
  background: radial-gradient(circle, rgba(255, 255, 255, 0.5) 0%, rgba(255, 255, 255, 0) 65%);
  opacity: 0;
  transition: opacity 0.25s;
  pointer-events: none;
  border-radius: 50%;
}
.glossy:hover::after { opacity: 1; }
.logout { transition: background 0.2s, transform 0.15s; }
.logout:hover { transform: translateY(-1px); }

.add-form { display: flex; gap: 10px; margin-bottom: 16px; }
.add-form input {
  flex: 1;
  padding: 12px 14px;
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 14px;
  font-size: 15px;
  outline: none;
  background: rgba(255, 255, 255, 0.5);
  color: #1d1d2b;
  transition: border-color 0.2s, box-shadow 0.2s, background 0.2s;
}
.add-form input::placeholder { color: #9a9ab0; }
.add-form input:focus {
  border-color: rgba(110, 130, 255, 0.8);
  background: rgba(255, 255, 255, 0.75);
  box-shadow: 0 0 0 4px rgba(110, 130, 255, 0.18);
}
.add-form button {
  padding: 12px 20px;
  border: 1px solid rgba(255, 255, 255, 0.75);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.3);
  backdrop-filter: blur(12px) saturate(160%);
  -webkit-backdrop-filter: blur(12px) saturate(160%);
  color: #3a3a5c;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s, transform 0.15s, box-shadow 0.25s;
  box-shadow:
    0 4px 14px rgba(31, 38, 135, 0.1),
    inset 0 1px 0 rgba(255, 255, 255, 0.85),
    inset 0 -1px 0 rgba(255, 255, 255, 0.25);
}
.add-form button:disabled { opacity: 0.5; cursor: not-allowed; }
.add-form button:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.5);
  box-shadow:
    0 8px 20px rgba(31, 38, 135, 0.15),
    inset 0 1px 0 rgba(255, 255, 255, 0.9),
    inset 0 -1px 0 rgba(255, 255, 255, 0.3);
  transform: translateY(-1px);
}

.filters { display: flex; gap: 8px; margin-bottom: 16px; }
.filters button {
  padding: 7px 18px;
  border: 1px solid rgba(255, 255, 255, 0.7);
  background: rgba(255, 255, 255, 0.4);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border-radius: 999px;
  font-size: 14px;
  color: #5c5c72;
  cursor: pointer;
  transition: all 0.2s;
}
.filters button.active {
  background: linear-gradient(135deg, rgba(90, 130, 255, 0.9), rgba(160, 100, 255, 0.9));
  border-color: rgba(255, 255, 255, 0.4);
  color: #fff;
  box-shadow: 0 4px 14px rgba(110, 120, 255, 0.35);
}

.todo-list { list-style: none; padding: 0; margin: 0; }
.todo-list li {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 10px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.45);
  border-radius: 14px;
  transition: background 0.15s;
}
.todo-list li:hover { background: rgba(255, 255, 255, 0.35); }
.todo-list li:last-child { border-bottom: none; }

.check {
  width: 24px;
  height: 24px;
  flex-shrink: 0;
  border: 2px solid rgba(93, 93, 120, 0.35);
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.5);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  padding: 0;
  transition: all 0.2s;
}
li.done .check {
  background: linear-gradient(135deg, rgba(90, 130, 255, 0.95), rgba(160, 100, 255, 0.95));
  border-color: transparent;
  box-shadow: 0 3px 10px rgba(110, 120, 255, 0.4);
}

.title { flex: 1; cursor: pointer; font-size: 15px; color: #1d1d2b; word-break: break-all; }
li.done .title { text-decoration: line-through; color: #9a9ab0; }

.edit {
  flex: 1;
  padding: 8px 10px;
  border: 1px solid rgba(110, 130, 255, 0.8);
  border-radius: 10px;
  font-size: 15px;
  outline: none;
  background: rgba(255, 255, 255, 0.75);
  color: #1d1d2b;
  box-shadow: 0 0 0 4px rgba(110, 130, 255, 0.15);
}

.del {
  background: none;
  border: none;
  color: rgba(93, 93, 120, 0.4);
  border-radius: 10px;
  padding: 6px;
  cursor: pointer;
  display: flex;
  transition: color 0.2s, background 0.2s;
}
.todo-list li:hover .del { color: #e0344b; }
.del:hover { background: rgba(224, 52, 75, 0.12); }

.error { color: #e0344b; font-size: 13px; margin: 8px 0 0; }
.empty { color: #5c5c72; text-align: center; padding: 32px 0 16px; margin: 0; font-size: 14px; }

.list-enter-active, .list-leave-active { transition: all 0.25s ease; }
.list-enter-from, .list-leave-to { opacity: 0; transform: translateX(-12px); }
</style>
