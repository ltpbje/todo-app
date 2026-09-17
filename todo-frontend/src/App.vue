<template>
  <div class="molten-bg" aria-hidden="true">
    <MoltenMetal :speed="0.3" :mouse-strength="0.25" />
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
  <router-view />
</template>

<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import MoltenMetal from './components/MoltenMetal.vue'

const isDark = ref(document.documentElement.classList.contains('dark'))

function toggleTheme() {
  const nextDark = !isDark.value
  const root = document.documentElement
  const apply = () => {
    root.classList.toggle('dark', nextDark)
    localStorage.setItem('theme', nextDark ? 'dark' : 'light')
    isDark.value = nextDark
  }
  // reveal origin: dark sweeps in from top-right, light from bottom-left
  root.style.setProperty('--vt-x', nextDark ? '100%' : '0%')
  root.style.setProperty('--vt-y', nextDark ? '0%' : '100%')
  const reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  if (document.startViewTransition && !reduced) {
    document.startViewTransition(apply)
  } else {
    apply()
  }
}

// keep in sync if user changes OS theme while page is open
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
/* MoltenMetal background wrapper: fixed full-viewport, behind everything */
.molten-bg {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}
html.dark .molten-bg {
  opacity: 0.5;
}

.theme-toggle {
  position: fixed;
  top: 16px;
  right: 16px;
  z-index: 150;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  padding: 0;
  border-radius: 14px;
}
</style>
