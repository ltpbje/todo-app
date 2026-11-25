<template>
  <div class="molten-bg" aria-hidden="true">
    <MoltenMetal :speed="0.3" :mouse-strength="0.25" :scale="6.5" />
  </div>

  <header v-if="route.path !== '/login'" class="app-topbar glass-panel" v-glass-glow>
    <div class="brand"><span class="brand-dot"></span>TODO清单</div>

    <nav class="app-nav" aria-label="主导航">
      <router-link to="/" class="nav-link">清单</router-link>
      <router-link to="/calendar" class="nav-link">日历</router-link>
      <router-link to="/stats" class="nav-link">统计</router-link>
    </nav>

    <div class="top-actions">
      <button
        class="glass-button icon-btn"
        :class="{ active: tiltEnabled }"
        :title="tiltEnabled ? '关闭卡片倾斜' : '开启卡片倾斜'"
        :aria-label="tiltEnabled ? '关闭卡片倾斜' : '开启卡片倾斜'"
        :aria-pressed="tiltEnabled"
        @click="toggleTilt"
      >
        <svg viewBox="0 0 24 24" width="17" height="17" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2 3 7v10l9 5 9-5V7z" /><path d="m3 7 9 5 9-5M12 12v10" /></svg>
      </button>
      <button
        class="glass-button icon-btn"
        :title="isDark ? '切换到浅色模式' : '切换到深色模式'"
        :aria-label="isDark ? '切换到浅色模式' : '切换到深色模式'"
        @click="toggleTheme"
      >
        <svg v-if="isDark" viewBox="0 0 24 24" width="17" height="17" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="4" /><path d="M12 2v2m0 16v2M4.9 4.9l1.4 1.4m11.4 11.4l1.4 1.4M2 12h2m16 0h2M4.9 19.1l1.4-1.4m11.4-11.4l1.4-1.4" /></svg>
        <svg v-else viewBox="0 0 24 24" width="17" height="17" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z" /></svg>
      </button>
      <button class="glass-button logout-btn" @click="logout">退出</button>
    </div>
  </header>

  <main class="app-main" :class="{ 'with-topbar': route.path !== '/login' }">
    <router-view />
  </main>
</template>

<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import MoltenMetal from './components/MoltenMetal.vue'
import { tiltEnabled, toggleTilt } from './utils/glassTilt'

const route = useRoute()
const router = useRouter()
const isDark = ref(document.documentElement.classList.contains('dark'))

function logout() {
  localStorage.removeItem('token')
  router.push('/login')
}

function toggleTheme() {
  const nextDark = !isDark.value
  const root = document.documentElement
  const apply = () => {
    root.classList.toggle('dark', nextDark)
    localStorage.setItem('theme', nextDark ? 'dark' : 'light')
    isDark.value = nextDark
  }
  root.style.setProperty('--vt-x', nextDark ? '100%' : '0%')
  root.style.setProperty('--vt-y', nextDark ? '0%' : '100%')
  const reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  if (document.startViewTransition && !reduced) {
    document.startViewTransition(apply)
  } else {
    apply()
  }
}

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
.molten-bg { position: fixed; inset: 0; z-index: 0; pointer-events: none; }
html.dark .molten-bg { opacity: 0.8; }

.app-topbar {
  position: fixed;
  top: 12px;
  left: 50%;
  transform: translateX(-50%);
  width: min(920px, calc(100% - 32px));
  height: 56px;
  z-index: 150;
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 0 12px 0 18px;
  border-radius: 18px;
}
.brand { display: flex; align-items: center; gap: 8px; font-weight: 700; color: var(--text-primary); white-space: nowrap; }
.brand-dot { width: 10px; height: 10px; border-radius: 50%; background: linear-gradient(135deg, #ff6b6b, #e0344b); box-shadow: 0 0 12px rgba(224, 52, 75, 0.6); }
.app-nav { flex: 1; display: flex; justify-content: center; gap: 4px; min-width: 0; }
.nav-link { padding: 8px 16px; border-radius: 12px; color: var(--text-secondary); text-decoration: none; font-size: 14px; white-space: nowrap; transition: background 0.2s, color 0.2s; }
.nav-link:hover { background: var(--glass-bg-hover); color: var(--text-primary); }
.nav-link.router-link-exact-active { background: var(--accent-grad); color: #fff; box-shadow: 0 4px 14px rgba(110, 120, 255, 0.3); }
.top-actions { display: flex; align-items: center; gap: 6px; }
.top-actions .glass-button { width: 36px; height: 36px; padding: 0; border-radius: 12px; display: flex; align-items: center; justify-content: center; }
.top-actions .icon-btn.active { color: var(--accent); }
.top-actions .logout-btn { width: auto; padding: 0 12px; font-size: 13px; }
.app-main { position: relative; z-index: 1; }
.app-main.with-topbar { padding: 84px 20px 40px; }
@media (max-width: 600px) {
  .app-topbar { gap: 8px; padding-left: 12px; }
  .brand { font-size: 0; gap: 0; }
  .brand-dot { width: 12px; height: 12px; }
  .nav-link { padding: 8px 12px; }
  .top-actions .logout-btn { display: none; }
}
</style>
