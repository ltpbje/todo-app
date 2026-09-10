<template>
  <div class="page">
    <div class="header">
      <h2>我的任务</h2>
      <button class="link" @click="logout">退出登录</button>
    </div>

    <form class="add-form" @submit.prevent="add">
      <input v-model.trim="newTitle" placeholder="新任务标题" />
      <button type="submit" :disabled="!newTitle">添加</button>
    </form>

    <div class="filters">
      <label v-for="opt in ['all', 'active', 'done']" :key="opt">
        <input type="radio" :value="opt" v-model="filter" /> {{ { all: '全部', active: '未完成', done: '已完成' }[opt] }}
      </label>
    </div>

    <p v-if="error" class="error">{{ error }}</p>
    <p v-if="!loading && !filtered.length" class="empty">暂无任务</p>

    <ul class="todo-list">
      <li v-for="todo in filtered" :key="todo.id" :class="{ done: todo.completed }">
        <input type="checkbox" :checked="todo.completed" @change="toggle(todo)" />
        <span class="title" @dblclick="startEdit(todo)">{{ todo.title }}</span>
        <template v-if="editingId === todo.id">
          <input v-model.trim="editTitle" class="edit" @keyup.enter="saveEdit(todo)" @keyup.esc="cancelEdit" @blur="saveEdit(todo)" />
        </template>
        <button class="del" @click="remove(todo)">删除</button>
      </li>
    </ul>
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

const filtered = computed(() => {
  if (filter.value === 'active') return todos.value.filter(t => !t.completed)
  if (filter.value === 'done') return todos.value.filter(t => t.completed)
  return todos.value
})

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
.page { max-width: 560px; margin: 40px auto; padding: 0 16px; }
.header { display: flex; justify-content: space-between; align-items: center; }
.header h2 { margin: 0; }
.link { background: none; border: none; color: #4098ff; cursor: pointer; }
.add-form { display: flex; gap: 8px; margin: 16px 0; }
.add-form input { flex: 1; padding: 10px; border: 1px solid #ddd; border-radius: 6px; }
.add-form button { padding: 10px 18px; border: none; border-radius: 6px; background: #4098ff; color: #fff; cursor: pointer; }
.filters { display: flex; gap: 16px; font-size: 14px; margin-bottom: 12px; }
.todo-list { list-style: none; padding: 0; margin: 0; }
.todo-list li { display: flex; align-items: center; gap: 10px; padding: 10px; border-bottom: 1px solid #eee; }
.todo-list li.done .title { text-decoration: line-through; color: #999; }
.title { flex: 1; cursor: pointer; }
.edit { flex: 1; padding: 6px; border: 1px solid #4098ff; border-radius: 4px; }
.del { background: none; border: 1px solid #e5484d; color: #e5484d; border-radius: 6px; padding: 4px 10px; cursor: pointer; }
.error { color: #e5484d; }
.empty { color: #999; text-align: center; }
</style>
