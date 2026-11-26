<template>
  <div class="page">
    <div class="content">
      <GlassObject class="glass-card" v-glass-glow>
        <div class="glare glass-glare"></div>
        <div class="stat-grid">
          <div><strong :key="todayDone">{{ todayDone }}</strong><span>今日完成</span></div>
          <div><strong :key="yesterdayDone">{{ yesterdayDone }}</strong><span>昨日完成</span></div>
          <div><strong :key="weekDone">{{ weekDone }}</strong><span>近 7 天</span></div>
          <div><strong :key="pomodoroMinutes">{{ pomodoroMinutes }}</strong><span>番茄分钟</span></div>
        </div>

        <section class="panel">
          <h3>近 7 天番茄</h3>
          <div class="pomo-chart">
            <div v-for="d in pomodoroTrend" :key="d.key" class="pomo-bar">
              <div class="bar-track"><div class="bar-fill" :style="{ height: Math.min(d.count * 14, 70) + 'px' }" :key="d.count"></div></div>
              <span>{{ d.label }}</span>
            </div>
          </div>
        </section>

        <section class="panel">
          <h3>昨日小结</h3>
          <p v-if="!yesterdayTasks.length" class="empty">昨天没有完成的任务</p>
          <ul v-else class="review-list">
            <li v-for="t in yesterdayTasks" :key="t.id">{{ t.title }}</li>
          </ul>
        </section>

        <section class="panel">
          <h3>清单分布</h3>
          <p v-if="!categoryStats.length" class="empty">还没有清单数据</p>
          <div v-else class="category-list">
            <div v-for="c in categoryStats" :key="c.name" class="category-row">
              <span>{{ c.name }}</span>
              <div class="track"><div class="fill" :style="{ width: c.percent + '%' }" :key="c.percent"></div></div>
              <b>{{ c.count }}</b>
            </div>
          </div>
        </section>
      </GlassObject>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import api from '../api/request'
import GlassObject from '../components/GlassObject.vue'
import { dateKey, completedOn, completedSince } from '../utils/plan'

const todos = ref([])
const sessions = ref([])

const todayDone = computed(() => todos.value.filter(t => !t.parentId && completedOn(t, dateKey(0))).length)
const yesterdayDone = computed(() => todos.value.filter(t => !t.parentId && completedOn(t, dateKey(-1))).length)
const weekDone = computed(() => todos.value.filter(t => !t.parentId && completedSince(t, 7)).length)
const yesterdayTasks = computed(() => todos.value.filter(t => !t.parentId && completedOn(t, dateKey(-1))))
const pomodoroMinutes = computed(() => Math.round(sessions.value.reduce((n, s) => n + (s.durationSec || 0), 0) / 60))
const pomodoroTrend = computed(() => Array.from({ length: 7 }, (_, i) => {
  const key = dateKey(i - 6)
  return { key, label: key.slice(5), count: sessions.value.filter(s => s.endedAt?.slice(0, 10) === key).length }
}))
const categoryStats = computed(() => {
  const counts = {}
  for (const t of todos.value.filter(t => !t.parentId && !t.completed)) {
    const name = t.category || '未分类'
    counts[name] = (counts[name] || 0) + 1
  }
  const total = Object.values(counts).reduce((a, b) => a + b, 0) || 1
  return Object.entries(counts)
    .map(([name, count]) => ({ name, count, percent: Math.round(count / total * 100) }))
    .sort((a, b) => b.count - a.count)
    .slice(0, 6)
})

onMounted(async () => {
  try {
    todos.value = (await api.get('/todos')).data
  } catch { /* 空统计 */ }
  try {
    sessions.value = (await api.get('/pomodoros')).data
  } catch { /* 旧后端暂无番茄历史接口 */ }
})
</script>

<style scoped>
.page { position: relative; z-index: 1; }
.content { max-width: 720px; margin: 0 auto; }
.glass-card { padding: 24px; position: relative; }

.stat-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px; text-align: center; position: relative; }
.stat-grid div { display: flex; flex-direction: column; gap: 2px; }
.stat-grid strong { font-size: 24px; color: var(--accent); animation: number-pop 0.35s var(--liquid-ease); }
.stat-grid span { font-size: 12px; color: var(--text-secondary); }

.panel { margin-top: 22px; border-top: 1px solid var(--glass-border); padding-top: 16px; position: relative; }
.panel h3 { margin: 0 0 12px; font-size: 14px; color: var(--text-primary); }
.pomo-chart { display: flex; justify-content: space-between; gap: 4px; }
.pomo-bar { flex: 1; display: flex; flex-direction: column; align-items: center; gap: 4px; }
.bar-track { width: 100%; height: 70px; display: flex; align-items: flex-end; justify-content: center; }
.bar-fill { width: 60%; min-height: 3px; border-radius: 6px 6px 0 0; background: var(--accent-grad); transform-origin: bottom; animation: bar-grow 0.55s var(--liquid-ease); }
.pomo-bar span { font-size: 10px; color: var(--text-secondary); }

.review-list { list-style: none; margin: 0; padding: 0; }
.review-list li { padding: 7px 10px; border-radius: 10px; font-size: 13px; color: var(--text-primary); }
.review-list li:nth-child(odd) { background: var(--glass-bg); }
.category-list { display: flex; flex-direction: column; gap: 8px; }
.category-row { display: grid; grid-template-columns: 90px 1fr 24px; align-items: center; gap: 10px; font-size: 12px; color: var(--text-primary); }
.category-row .track { height: 8px; border-radius: 999px; background: var(--glass-bg-strong); overflow: hidden; }
.category-row .fill { height: 100%; border-radius: 999px; background: var(--accent-grad); transform-origin: left; }
.category-row b { color: var(--accent); text-align: right; }
@keyframes number-pop { from { opacity: 0.5; transform: translateY(4px) scale(0.9); } to { opacity: 1; transform: translateY(0) scale(1); } }
@keyframes bar-grow { from { transform: scaleY(0); opacity: 0.5; } to { transform: scaleY(1); opacity: 1; } }
.empty { text-align: center; color: var(--text-secondary); font-size: 13px; padding: 14px 0; margin: 0; }
@media (max-width: 600px) {
  .stat-grid { grid-template-columns: repeat(2, 1fr); row-gap: 12px; }
}
</style>
