<template>
  <div class="page">
    <div class="header">
      <div>
        <h2>我的任务</h2>
        <p class="stat" v-if="todos.length">
          共 {{ todos.length }} 项 · {{ doneCount }} 已完成
        </p>
      </div>
      <button class="logout" @click="logout">退出登录</button>
    </div>

    <div class="card">
      <form class="add-form" @submit.prevent="add">
        <input v-model.trim="newTitle" placeholder="添加新任务，回车确认…" />
        <button type="submit" :disabled="!newTitle">添加</button>
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
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
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
  background: linear-gradient(180deg, #ffffff 0%, #f2f4ff 100%);
  padding: 32px 20px;
  box-sizing: border-box;
}
.page > * { max-width: 620px; margin-left: auto; margin-right: auto; }

.header { display: flex; justify-content: space-between; align-items: flex-start; color: #1d1d1f; margin-bottom: 24px; }
.header h2 { margin: 0; font-size: 26px; font-weight: 600; }
.stat { margin: 6px 0 0; font-size: 13px; color: #86868b; }
.logout {
  padding: 8px 16px;
  border: 1px solid #d1d1d6;
  background: #ffffff;
  border-radius: 12px;
  font-size: 14px;
  color: #6e6e73;
  cursor: pointer;
  transition: background 0.2s, color 0.2s;
}
.logout:hover { background: #f0f0f3; color: #007aff; }

.card {
  background: #ffffff;
  border-radius: 20px;
  padding: 24px;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.08);
}

.add-form { display: flex; gap: 10px; margin-bottom: 16px; }
.add-form input {
  flex: 1;
  padding: 12px 14px;
  border: 1px solid #e5e5ea;
  border-radius: 12px;
  font-size: 15px;
  outline: none;
  transition: border-color 0.2s, box-shadow 0.2s;
}
.add-form input:focus {
  border-color: #007aff;
  box-shadow: 0 0 0 3px rgba(0, 122, 255, 0.15);
}
.add-form button {
  padding: 12px 20px;
  border: none;
  border-radius: 12px;
  background: linear-gradient(135deg, #007aff, #5ac8fa);
  color: #ffffff;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: opacity 0.2s;
}
.add-form button:disabled { opacity: 0.5; cursor: not-allowed; }

.filters { display: flex; gap: 8px; margin-bottom: 16px; }
.filters button {
  padding: 7px 16px;
  border: 1px solid #e5e5ea;
  background: #ffffff;
  border-radius: 999px;
  font-size: 14px;
  color: #86868b;
  cursor: pointer;
  transition: all 0.2s;
}
.filters button.active {
  background: #007aff;
  border-color: #007aff;
  color: #ffffff;
}

.todo-list { list-style: none; padding: 0; margin: 0; }
.todo-list li {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 8px;
  border-bottom: 1px solid #f0f0f3;
  border-radius: 12px;
  transition: background 0.15s;
}
.todo-list li:hover { background: #f7f8fb; }
.todo-list li:last-child { border-bottom: none; }

.check {
  width: 24px;
  height: 24px;
  flex-shrink: 0;
  border: 2px solid #d1d1d6;
  border-radius: 50%;
  background: #ffffff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ffffff;
  padding: 0;
  transition: all 0.2s;
}
li.done .check { background: linear-gradient(135deg, #007aff, #5ac8fa); border-color: transparent; }

.title { flex: 1; cursor: pointer; font-size: 15px; color: #1d1d1f; word-break: break-all; }
li.done .title { text-decoration: line-through; color: #b0b0b5; }

.edit {
  flex: 1;
  padding: 8px 10px;
  border: 1px solid #007aff;
  border-radius: 10px;
  font-size: 15px;
  outline: none;
  box-shadow: 0 0 0 3px rgba(0, 122, 255, 0.12);
}

.del {
  background: none;
  border: none;
  color: #c7c7cc;
  border-radius: 8px;
  padding: 6px;
  cursor: pointer;
  display: flex;
  transition: color 0.2s, background 0.2s;
}
.todo-list li:hover .del { color: #ff3b30; }
.del:hover { background: #fff0f0; }

.error { color: #ff3b30; font-size: 13px; margin: 8px 0 0; }
.empty { color: #86868b; text-align: center; padding: 32px 0 16px; margin: 0; font-size: 14px; }

.list-enter-active, .list-leave-active { transition: all 0.25s ease; }
.list-enter-from, .list-leave-to { opacity: 0; transform: translateX(-12px); }
</style>
