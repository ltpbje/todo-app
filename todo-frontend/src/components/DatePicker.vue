<template>
  <div ref="root" class="date-picker">
    <button ref="trigger" type="button" class="date-trigger glass-input" :aria-label="label" :aria-expanded="open" @click="toggle">
      <span :class="{ placeholder: !modelValue }">{{ display }}</span>
      <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
        <rect x="3" y="5" width="18" height="16" rx="2" /><path d="M16 3v4M8 3v4M3 10h18" />
      </svg>
    </button>

    <Teleport to="body">
    <div v-if="open" ref="pop" class="date-pop glass-panel" :style="{ top: popTop + 'px', left: popLeft + 'px' }">
      <div class="dp-head">
        <button type="button" aria-label="上个月" @click="shiftMonth(-1)">‹</button>
        <strong>{{ year }}年{{ month + 1 }}月</strong>
        <button type="button" aria-label="下个月" @click="shiftMonth(1)">›</button>
      </div>
      <div class="dp-week"><span v-for="w in weekdays" :key="w">{{ w }}</span></div>
      <div class="dp-grid">
        <button
          v-for="cell in cells"
          :key="cell"
          type="button"
          class="dp-day"
          :class="{ other: cell.slice(0, 7) !== monthKey, today: cell === dateKey(0), selected: cell === modelValue }"
          @click="choose(cell)"
        >{{ Number(cell.slice(8)) }}</button>
      </div>
      <div class="dp-foot">
        <button type="button" @click="choose('')">清除</button>
        <button type="button" @click="choose(dateKey(0))">今天</button>
        <button type="button" @click="choose(dateKey(1))">明天</button>
      </div>
    </div>
    </Teleport>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { dateKey, monthCells } from '../utils/plan'

const props = defineProps({
  modelValue: { type: String, default: '' },
  label: { type: String, default: '安排日期' },
  placeholder: { type: String, default: '选择日期' }
})
const emit = defineEmits(['update:modelValue'])

const root = ref(null)
const trigger = ref(null)
const pop = ref(null)
const open = ref(false)
const popTop = ref(0)
const popLeft = ref(0)
const weekdays = ['日', '一', '二', '三', '四', '五', '六']
const today = dateKey(0)
const year = ref(Number(today.slice(0, 4)))
const month = ref(Number(today.slice(5, 7)) - 1)

watch(() => props.modelValue, value => {
  if (!/^\d{4}-\d{2}-\d{2}/.test(value || '')) return
  year.value = Number(value.slice(0, 4))
  month.value = Number(value.slice(5, 7)) - 1
}, { immediate: true })

const monthKey = computed(() => `${year.value}-${String(month.value + 1).padStart(2, '0')}`)
const cells = computed(() => monthCells(year.value, month.value))
const display = computed(() => {
  if (!props.modelValue) return props.placeholder
  if (props.modelValue === dateKey(0)) return '今天'
  if (props.modelValue === dateKey(1)) return '明天'
  return props.modelValue
})

function choose(value) {
  emit('update:modelValue', value)
  open.value = false
}

function shiftMonth(delta) {
  const d = new Date(year.value, month.value + delta, 1)
  year.value = d.getFullYear()
  month.value = d.getMonth()
}

function toggle() {
  open.value = !open.value
  if (!open.value) return
  const r = trigger.value?.getBoundingClientRect()
  if (!r) return
  popLeft.value = Math.max(8, Math.min(r.left, window.innerWidth - 284))
  popTop.value = r.bottom + 8
}

function onDocumentClick(e) {
  if (!open.value) return
  if (root.value?.contains(e.target) || pop.value?.contains(e.target)) return
  open.value = false
}
const close = () => { open.value = false }
onMounted(() => {
  document.addEventListener('click', onDocumentClick)
  window.addEventListener('resize', close)
  window.addEventListener('scroll', close, true)
})
onUnmounted(() => {
  document.removeEventListener('click', onDocumentClick)
  window.removeEventListener('resize', close)
  window.removeEventListener('scroll', close, true)
})
</script>

<style scoped>
.date-picker { position: relative; }
.date-trigger {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  text-align: left;
  padding: 9px 12px;
  font-size: 14px;
  white-space: nowrap;
}
.date-trigger .placeholder { color: var(--text-secondary); }
.date-trigger svg { color: var(--accent); flex-shrink: 0; }

.date-pop {
  position: fixed;
  z-index: 500;
  width: 268px;
  max-width: calc(100vw - 48px);
  padding: 12px;
  border: 1px solid rgba(255, 255, 255, 0.55);
  border-radius: var(--glass-radius-sm);
  background: #fafaff;
  box-shadow: 0 18px 44px rgba(40, 34, 90, 0.28), 0 2px 8px rgba(40, 34, 90, 0.12);
  transform-origin: top left;
  animation: pop-in 0.2s var(--liquid-ease) both;
}
html.dark .date-pop {
  border-color: rgba(255, 255, 255, 0.14);
  background: #1c1b30;
  box-shadow: 0 18px 44px rgba(0, 0, 0, 0.55), 0 2px 8px rgba(0, 0, 0, 0.4);
}
.dp-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; }
.dp-head strong { color: var(--text-primary); font-size: 13px; }
.dp-head button {
  width: 26px; height: 26px; border: none; border-radius: 8px;
  background: var(--glass-bg); color: var(--text-primary); cursor: pointer; font-size: 18px; line-height: 1;
}
.dp-head button:hover { background: var(--accent-grad); color: #fff; }
.dp-week, .dp-grid { display: grid; grid-template-columns: repeat(7, 1fr); gap: 3px; }
.dp-week { margin-bottom: 4px; }
.dp-week span { text-align: center; font-size: 10px; color: var(--text-secondary); }
.dp-day {
  aspect-ratio: 1; border: 1px solid transparent; border-radius: 8px;
  background: transparent; color: var(--text-primary); cursor: pointer; font-size: 12px;
}
.dp-day:hover { background: var(--glass-bg-hover); }
.dp-day.other { opacity: 0.3; }
.dp-day.today { border-color: var(--accent); }
.dp-day.selected { background: var(--accent-grad); color: #fff; border-color: transparent; box-shadow: 0 3px 10px rgba(110, 120, 255, 0.28); animation: day-pick 0.22s var(--liquid-ease); }
.dp-foot { display: flex; gap: 6px; margin-top: 10px; }
.dp-foot button {
  flex: 1; padding: 6px 0; border: 1px solid var(--glass-border); border-radius: 8px;
  background: var(--glass-bg); color: var(--text-secondary); cursor: pointer; font-size: 12px;
}
.dp-foot button:hover { color: var(--accent); border-color: var(--accent); }
@keyframes pop-in { from { opacity: 0; transform: translateY(-5px) scale(0.97); } to { opacity: 1; transform: translateY(0) scale(1); } }
@keyframes day-pick { from { transform: scale(0.82); } to { transform: scale(1); } }
</style>
