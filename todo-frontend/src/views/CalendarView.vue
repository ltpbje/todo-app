<template>
  <div class="page">
    <div class="content">
      <GlassObject class="glass-card" v-glass-glow>
        <div class="glare glass-glare"></div>
        <div class="calendar">
          <div class="cal-head">
            <button class="glass-button" aria-label="上个月" @click="shiftMonth(-1)">‹</button>
            <strong>{{ label }}</strong>
            <button class="glass-button" aria-label="下个月" @click="shiftMonth(1)">›</button>
          </div>
          <div class="cal-week-row"><span v-for="w in weekdays" :key="w">{{ w }}</span></div>
          <div class="cal-grid">
            <button
              v-for="cell in cells"
              :key="cell"
              class="cal-day"
              :class="{ other: cell.slice(0, 7) !== monthKey, today: cell === dateKey(0), selected: cell === selectedDay }"
              @click="selectedDay = cell"
            >
              <span>{{ Number(cell.slice(8)) }}</span>
              <i v-if="dueCounts[cell]">{{ dueCounts[cell] }}</i>
            </button>
          </div>
        </div>

        <div class="day-panel">
          <div class="day-head">
            <strong>{{ selectedDay === dateKey(0) ? '今天' : selectedDay }} 的安排</strong>
            <button class="glass-button accent" @click="openInList">在清单中查看</button>
          </div>
          <p v-if="!selectedTasks.length" class="empty">这一天还没有安排任务</p>
          <ul v-else class="day-list">
            <li v-for="t in selectedTasks" :key="t.id" :class="{ done: t.completed }">
              <span>{{ t.title }}</span>
              <span class="tag" v-if="t.category">{{ t.category }}</span>
            </li>
          </ul>
        </div>
      </GlassObject>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api/request'
import GlassObject from '../components/GlassObject.vue'
import { dateKey, monthCells } from '../utils/plan'

const router = useRouter()
const todos = ref([])
const selectedDay = ref(dateKey(0))
const weekdays = ['日', '一', '二', '三', '四', '五', '六']
const year = ref(Number(dateKey(0).slice(0, 4)))
const month = ref(Number(dateKey(0).slice(5, 7)) - 1)

const monthKey = computed(() => `${year.value}-${String(month.value + 1).padStart(2, '0')}`)
const label = computed(() => `${year.value}年${month.value + 1}月`)
const cells = computed(() => monthCells(year.value, month.value))
const dueCounts = computed(() => todos.value
  .filter(t => !t.parentId && !t.completed && t.dueDate)
  .reduce((acc, t) => { acc[t.dueDate] = (acc[t.dueDate] || 0) + 1; return acc }, {}))
const selectedTasks = computed(() => todos.value.filter(t => !t.parentId && t.dueDate === selectedDay.value))

function shiftMonth(delta) {
  const d = new Date(year.value, month.value + delta, 1)
  year.value = d.getFullYear()
  month.value = d.getMonth()
}
function openInList() {
  router.push({ path: '/', query: { date: selectedDay.value } })
}
onMounted(async () => {
  try {
    todos.value = (await api.get('/todos')).data
  } catch { /* 加载失败时显示空日历 */ }
})
</script>

<style scoped>
.page { position: relative; z-index: 1; }
.content { max-width: 720px; margin: 0 auto; }
.glass-card { padding: 24px; position: relative; }

.cal-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; position: relative; }
.cal-head strong { color: var(--text-primary); }
.cal-head .glass-button { padding: 4px 14px; }
.cal-week-row, .cal-grid { display: grid; grid-template-columns: repeat(7, 1fr); gap: 4px; position: relative; }
.cal-week-row { margin-bottom: 4px; }
.cal-week-row span { text-align: center; font-size: 11px; color: var(--text-secondary); }
.cal-day { position: relative; aspect-ratio: 1; border: 1px solid transparent; border-radius: 10px; background: var(--glass-bg-strong); color: var(--text-primary); cursor: pointer; font-size: 12px; }
.cal-day:hover { background: var(--glass-bg-hover); }
.cal-day.other { opacity: 0.32; }
.cal-day.today { border-color: var(--accent); }
.cal-day.selected { background: var(--accent-grad); color: #fff; }
.cal-day i { position: absolute; right: 4px; top: 3px; font-size: 9px; font-style: normal; color: var(--accent); }
.cal-day.selected i { color: #fff; }

.day-panel { margin-top: 20px; border-top: 1px solid var(--glass-border); padding-top: 16px; position: relative; }
.day-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; }
.day-head strong { color: var(--text-primary); font-size: 14px; }
.day-head .glass-button { padding: 6px 12px; font-size: 12px; }
.day-list { list-style: none; margin: 0; padding: 0; }
.day-list li { display: flex; align-items: center; gap: 8px; padding: 8px 10px; border-radius: 10px; font-size: 13px; color: var(--text-primary); }
.day-list li:nth-child(odd) { background: var(--glass-bg); }
.day-list li.done span:first-child { text-decoration: line-through; color: var(--text-secondary); }
.day-list .tag { font-size: 11px; color: var(--accent); }
.empty { text-align: center; color: var(--text-secondary); font-size: 13px; padding: 14px 0; margin: 0; }
</style>
