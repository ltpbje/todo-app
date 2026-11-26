<template>
  <div class="pomo-root">
    <button class="glass-button glass-glossy pomo-fab" @click="open = !open">
      <span class="tomato-dot"></span>
      {{ open ? '收起' : '专注' }}
    </button>

    <transition name="pomo">
    <div v-if="open" class="glass-panel pomo-panel" v-glass-glow>
      <div class="pomo-head">
        <strong>番茄专注</strong>
        <button class="pomo-close" aria-label="关闭" @click="open = false">✕</button>
      </div>
      <div class="clock" :class="{ running, finished }">
        <svg class="progress-ring" viewBox="0 0 120 120" aria-hidden="true">
          <circle class="ring-track" cx="60" cy="60" r="54" />
          <circle class="ring-fill" cx="60" cy="60" r="54" :style="{ strokeDashoffset: ringOffset }" />
        </svg>
        <span>{{ mm }}:{{ ss }}</span>
      </div>
      <SelectPicker v-model="selectedId" :options="taskOptions" label="关联任务" class="pomo-select" />
      <SelectPicker v-model="noiseType" :options="noiseOptions" label="白噪音类型" class="pomo-select" />
      <div class="pomo-actions">
        <button class="glass-button accent" @click="running ? pause() : start()">{{ running ? '暂停' : '开始' }}</button>
        <button class="glass-button" @click="reset">重置</button>
        <button class="glass-button" :class="{ active: noiseOn }" @click="toggleNoise">{{ noiseOn ? '关闭' : '白噪音' }}</button>
      </div>
      <p class="tip">完成一个番茄后自动计入所选任务</p>
    </div>
    </transition>
  </div>
</template>

<script setup>
import { computed, onUnmounted, ref, watch } from 'vue'
import SelectPicker from './SelectPicker.vue'

const props = defineProps({ todos: { type: Array, default: () => [] } })
const emit = defineEmits(['finish'])

const DURATION = 25 * 60
const open = ref(false)
const running = ref(false)
const remaining = ref(DURATION)
const selectedId = ref(undefined)
const noiseType = ref('brown')
const taskOptions = computed(() => [
  { label: '不关联任务', value: null },
  ...props.todos.map(t => ({ label: t.title, value: t.id }))
])
const noiseOptions = [
  { label: '棕噪', value: 'brown' }, { label: '雨声', value: 'rain' }, { label: '海浪', value: 'ocean' }
]
const mm = computed(() => String(Math.floor(remaining.value / 60)).padStart(2, '0'))
const ss = computed(() => String(remaining.value % 60).padStart(2, '0'))
const ringOffset = computed(() => 339.29 * (1 - remaining.value / DURATION))
const finished = ref(false)

watch(() => props.todos, list => {
  if (selectedId.value === undefined && list.length) selectedId.value = list[0].id
  // 关联任务被删除后不再向已失效 id 记录番茄
  if (selectedId.value != null && !list.some(t => t.id === selectedId.value)) {
    selectedId.value = list[0]?.id ?? null
  }
}, { immediate: true })

watch(noiseType, () => onNoiseChange())

let endAt = 0
let timer = null

function start() {
  if (running.value) return
  endAt = Date.now() + remaining.value * 1000
  running.value = true
  timer = setInterval(tick, 250)
}

function pause() {
  running.value = false
  clearInterval(timer)
  timer = null
}

function reset() {
  pause()
  remaining.value = DURATION
  finished.value = false
}

function tick() {
  remaining.value = Math.max(0, Math.ceil((endAt - Date.now()) / 1000))
  if (remaining.value) return
  pause()
  emit('finish', selectedId.value)
  finished.value = true
  setTimeout(() => { finished.value = false }, 1600)
}

// ponytail: 固定音量的循环合成噪声；没有音量滑块，后续要调音可加 gain 控件
let audio = null
const noiseOn = ref(false)

function fillNoise(data, sampleRate, type) {
  let last = 0
  for (let i = 0; i < data.length; i++) {
    const white = Math.random() * 2 - 1
    last = (last + 0.02 * white) / 1.02
    if (type === 'rain') data[i] = white * 0.3 + (Math.random() < 0.001 ? white * 0.7 : 0)
    else if (type === 'ocean') data[i] = last * 3 * (0.55 + 0.45 * Math.sin((i / sampleRate) * 0.8))
    else data[i] = last * 3
  }
}

function onNoiseChange() {
  if (!audio) return
  stopNoise()
  toggleNoise()
}

function toggleNoise() {
  if (audio) {
    stopNoise()
    return
  }
  const Ctx = window.AudioContext || window.webkitAudioContext
  if (!Ctx) return
  const ctx = new Ctx()
  const frames = ctx.sampleRate * 2
  const buffer = ctx.createBuffer(1, frames, ctx.sampleRate)
  fillNoise(buffer.getChannelData(0), ctx.sampleRate, noiseType.value)
  const src = ctx.createBufferSource()
  src.buffer = buffer
  src.loop = true
  const gain = ctx.createGain()
  gain.gain.value = 0.22
  src.connect(gain).connect(ctx.destination)
  src.start()
  audio = { ctx, src, gain }
  noiseOn.value = true
}

function stopNoise() {
  if (!audio) return
  try { audio.src.stop() } catch { /* already stopped */ }
  audio.src.disconnect()
  audio.gain.disconnect()
  audio.ctx.close()
  audio = null
  noiseOn.value = false
}

onUnmounted(() => {
  pause()
  stopNoise()
})
</script>

<style scoped>
.pomo-root { position: fixed; right: 18px; bottom: 18px; z-index: 140; display: flex; flex-direction: column; align-items: flex-end; gap: 10px; }
.pomo-fab { display: flex; align-items: center; gap: 8px; padding: 10px 16px; }
.tomato-dot { width: 12px; height: 12px; border-radius: 50%; background: linear-gradient(135deg, #ff6b6b, #e0344b); box-shadow: 0 0 12px rgba(224, 52, 75, 0.6); }
.pomo-root:has(.pomo-panel) .tomato-dot { animation: focus-pulse 2s ease-in-out infinite; }
.pomo-panel { width: 272px; padding: 16px; position: relative; }
.pomo-head { display: flex; align-items: center; justify-content: space-between; position: relative; }
.pomo-head strong { color: var(--text-primary); font-size: 15px; }
.pomo-close { background: none; border: none; color: var(--text-secondary); cursor: pointer; }
.clock { position: relative; width: 120px; height: 120px; margin: 6px auto 12px; display: grid; place-items: center; color: var(--accent); font-size: 36px; font-weight: 700; letter-spacing: 1px; font-variant-numeric: tabular-nums; }
.clock > span { position: relative; z-index: 1; }
.progress-ring { position: absolute; inset: 0; width: 100%; height: 100%; transform: rotate(-90deg); overflow: visible; }
.progress-ring circle { fill: none; stroke-width: 3; }
.ring-track { stroke: var(--glass-border); }
.ring-fill { stroke: var(--accent); stroke-linecap: round; stroke-dasharray: 339.29; transition: stroke-dashoffset 0.5s linear; }
.clock.running .ring-fill { filter: drop-shadow(0 0 4px rgba(110, 130, 255, 0.5)); }
.clock.finished { animation: pomo-done 0.7s var(--liquid-ease); }
.pomo-enter-active, .pomo-leave-active { transition: opacity 0.2s ease, transform 0.3s var(--liquid-ease); transform-origin: bottom right; }
.pomo-enter-from, .pomo-leave-to { opacity: 0; transform: translateY(10px) scale(0.92); }
@keyframes focus-pulse { 0%, 100% { transform: scale(1); box-shadow: 0 0 12px rgba(224, 52, 75, 0.6); } 50% { transform: scale(1.25); box-shadow: 0 0 20px rgba(224, 52, 75, 0.85); } }
@keyframes pomo-done { 0%, 100% { transform: scale(1); } 40% { transform: scale(1.08); } 70% { transform: scale(0.98); } }
.pomo-select { width: 100%; position: relative; }
.pomo-select .select-trigger { padding: 8px 10px; font-size: 13px; }
.pomo-actions { display: flex; gap: 6px; margin-top: 10px; position: relative; }
.pomo-actions .glass-button { flex: 1; padding: 8px 4px; font-size: 13px; }
.pomo-actions .glass-button.active { background: var(--accent-grad); color: #fff; }
.tip { position: relative; margin: 10px 0 0; font-size: 11px; color: var(--text-secondary); text-align: center; }
</style>
