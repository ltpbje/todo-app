import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import './styles/glass.css'

// 鼠标跟随光泽已停用：原先每次 mousemove 都调用 getBoundingClientRect，
// 强制同步布局，是点击时卡顿的主因
const vGlassGlow = {
  mounted(el) {
    el.classList.add('glass-glow-host')
  }
}

createApp(App).use(router).directive('glass-glow', vGlassGlow).mount('#app')
