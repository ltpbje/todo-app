import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import './styles/glass.css'

// mouse-follow glow: updates --gx/--gy (px) and --x/--y (%) on the host element
const vGlassGlow = {
  mounted(el) {
    el.classList.add('glass-glow-host')
    const move = (e) => {
      const r = el.getBoundingClientRect()
      const x = e.clientX - r.left
      const y = e.clientY - r.top
      el.style.setProperty('--gx', `${x}px`)
      el.style.setProperty('--gy', `${y}px`)
      el.style.setProperty('--x', `${(x / r.width) * 100}%`)
      el.style.setProperty('--y', `${(y / r.height) * 100}%`)
    }
    el.addEventListener('mousemove', move)
    el._glassGlowMove = move
  },
  unmounted(el) {
    el.removeEventListener('mousemove', el._glassGlowMove)
  }
}

createApp(App).use(router).directive('glass-glow', vGlassGlow).mount('#app')
