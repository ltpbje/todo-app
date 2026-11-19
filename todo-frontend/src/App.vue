<template>
  <div class="molten-bg" aria-hidden="true">
    <!--
      `scale` 特意比组件默认值（4）更细。
      液态玻璃只有在背后有结构可供折射时才像玻璃——在毫无细节的渐变上，
      无论卡片本身怎么调，玻璃面都会退化成一片扁平的白板。
    -->
    <MoltenMetal :speed="0.3" :mouse-strength="0.25" :scale="6.5" />
  </div>
  <button
    class="glass-button theme-toggle glass-glossy"
    :title="isDark ? '切换到浅色模式' : '切换到深色模式'"
    @click="toggleTheme"
  >
    <svg v-if="isDark" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
      <circle cx="12" cy="12" r="4" /><path d="M12 2v2m0 16v2M4.9 4.9l1.4 1.4m11.4 11.4l1.4 1.4M2 12h2m16 0h2M4.9 19.1l1.4-1.4m11.4-11.4l1.4-1.4" />
    </svg>
    <svg v-else viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
      <path d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z" />
    </svg>
  </button>
  <button
    class="glass-button tilt-toggle glass-glossy"
    :class="{ active: tiltEnabled }"
    :title="tiltEnabled ? '关闭卡片倾斜' : '开启卡片倾斜'"
    :aria-label="tiltEnabled ? '关闭卡片倾斜' : '开启卡片倾斜'"
    :aria-pressed="tiltEnabled"
    @click="toggleTilt"
  >
    <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
      <path d="M12 2 3 7v10l9 5 9-5V7z" /><path d="m3 7 9 5 9-5M12 12v10" />
    </svg>
    <span>{{ tiltEnabled ? '倾斜开' : '倾斜关' }}</span>
  </button>
  <router-view />
</template>

<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import MoltenMetal from './components/MoltenMetal.vue'
import { tiltEnabled, toggleTilt } from './utils/glassTilt'

const isDark = ref(document.documentElement.classList.contains('dark'))

function toggleTheme() {
  const nextDark = !isDark.value
  const root = document.documentElement
  const apply = () => {
    root.classList.toggle('dark', nextDark)
    localStorage.setItem('theme', nextDark ? 'dark' : 'light')
    isDark.value = nextDark
  }
  // 揭示起点：深色从右上角扫入，浅色从左下角
  root.style.setProperty('--vt-x', nextDark ? '100%' : '0%')
  root.style.setProperty('--vt-y', nextDark ? '0%' : '100%')
  const reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  if (document.startViewTransition && !reduced) {
    document.startViewTransition(apply)
  } else {
    apply()
  }
}

// 用户在使用中切换系统主题时保持同步
const mq = window.matchMedia('(prefers-color-scheme: dark)')
const onSystemChange = (e) => {
  if (!localStorage.getItem('theme')) {
    isDark.value = e.matches
    document.documentElement.classList.toggle('dark', e.matches)
  }
}
onMounted(() => mq.addEventListener('change', onSystemChange))
onUnmounted(() => mq.removeEventListener('change', onSystemChange))
</script>

<style>
/* MoltenMetal 背景容器：固定铺满视口，位于所有内容之下 */
.molten-bg {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}
html.dark .molten-bg {
  opacity: 0.8;
}

.theme-toggle,
.tilt-toggle {
  position: fixed;
  top: 16px;
  z-index: 150;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  padding: 0;
  border-radius: 14px;
}
.theme-toggle { right: 16px; }
.tilt-toggle {
  right: 66px;
  width: auto;
  padding: 0 12px;
  gap: 6px;
  font-size: 13px;
  white-space: nowrap;
  color: var(--text-secondary);
  opacity: 0.65;
}
.tilt-toggle.active {
  color: var(--accent);
  opacity: 1;
}
</style>
