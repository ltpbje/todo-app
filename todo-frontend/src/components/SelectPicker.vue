<template>
  <div ref="root" class="select-picker">
    <button ref="trigger" type="button" class="select-trigger glass-input" :aria-label="label" :aria-expanded="open" @click="toggle">
      <span class="select-value">{{ currentLabel }}</span>
      <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
        <path d="m6 9 6 6 6-6" />
      </svg>
    </button>

    <Teleport to="body">
      <div
        v-if="open"
        ref="pop"
        class="select-pop"
        :style="{ top: popTop + 'px', left: popLeft + 'px', width: popWidth + 'px' }"
      >
        <button
          v-for="opt in options"
          :key="String(opt.value)"
          type="button"
          class="select-option"
          :class="{ active: opt.value === modelValue }"
          @click="choose(opt.value)"
        >
          <span>{{ opt.label }}</span>
          <svg v-if="opt.value === modelValue" viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M20 6 9 17l-5-5" /></svg>
        </button>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'

const props = defineProps({
  modelValue: { default: '' },
  options: { type: Array, required: true },
  label: { type: String, default: '选择' },
  placeholder: { type: String, default: '请选择' }
})
const emit = defineEmits(['update:modelValue'])

const root = ref(null)
const trigger = ref(null)
const pop = ref(null)
const open = ref(false)
const popTop = ref(0)
const popLeft = ref(0)
const popWidth = ref(140)

const currentLabel = computed(() => props.options.find(o => o.value === props.modelValue)?.label ?? props.placeholder)

function choose(value) {
  emit('update:modelValue', value)
  open.value = false
}

function toggle() {
  open.value = !open.value
  if (!open.value) return
  const r = trigger.value?.getBoundingClientRect()
  if (!r) return
  popWidth.value = Math.max(r.width, 140)
  popLeft.value = Math.max(8, Math.min(r.left, window.innerWidth - popWidth.value - 8))
  popTop.value = r.bottom + 6
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
.select-picker { position: relative; min-width: 0; }
.select-trigger {
  width: 100%; display: flex; align-items: center; justify-content: space-between; gap: 6px;
  padding: 9px 12px; font-size: 14px; text-align: left; white-space: nowrap;
}
.select-trigger svg { flex-shrink: 0; color: var(--accent); transition: transform 0.25s var(--liquid-ease); }
.select-trigger[aria-expanded="true"] svg { transform: rotate(180deg); }

.select-pop {
  position: fixed;
  z-index: 500;
  max-height: 280px;
  overflow: auto;
  padding: 5px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: var(--glass-radius-sm);
  background: #fafaff;
  box-shadow: 0 16px 40px rgba(40, 34, 90, 0.26), 0 2px 8px rgba(40, 34, 90, 0.12);
  transform-origin: top left;
  animation: pop-in 0.2s var(--liquid-ease) both;
}
html.dark .select-pop {
  border-color: rgba(255, 255, 255, 0.14);
  background: #1c1b30;
  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.55), 0 2px 8px rgba(0, 0, 0, 0.4);
}
.select-option {
  width: 100%; display: flex; align-items: center; justify-content: space-between; gap: 8px;
  padding: 9px 10px; border: none; border-radius: 10px; background: transparent;
  color: var(--text-primary); font-size: 13px; text-align: left; cursor: pointer;
}
.select-option:hover { background: var(--glass-bg-hover); }
.select-option.active { color: var(--accent); font-weight: 600; }
.select-option svg { flex-shrink: 0; animation: check-pop 0.2s var(--liquid-ease); }
@keyframes pop-in { from { opacity: 0; transform: translateY(-5px) scale(0.97); } to { opacity: 1; transform: translateY(0) scale(1); } }
@keyframes check-pop { from { opacity: 0; transform: scale(0.5); } to { opacity: 1; transform: scale(1); } }
</style>
