<template>
  <div ref="host" class="glass-object is-resting" :class="{ 'tilt-on': tiltEnabled }" @mousemove="onMove" @mouseleave="onLeave">
    <span class="glass-box" aria-hidden="true"></span>
    <div class="glass-content"><slot /></div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { tiltEnabled } from '../utils/glassTilt'

const host = ref(null)
let raf = 0
let mouseX = 0
let mouseY = 0

function onMove(e) {
  if (!tiltEnabled.value) return
  mouseX = e.clientX
  mouseY = e.clientY
  if (raf) return
  raf = requestAnimationFrame(() => {
    raf = 0
    const el = host.value
    if (!el) return
    el.classList.remove('is-resting')
    const r = el.getBoundingClientRect()
    const nx = (mouseX - r.left) / r.width * 2 - 1
    const ny = (mouseY - r.top) / r.height * 2 - 1
    el.style.setProperty('--rx', `${(-ny * 3).toFixed(2)}deg`)
    el.style.setProperty('--ry', `${(nx * 4).toFixed(2)}deg`)
  })
}

function onLeave() {
  if (raf) { cancelAnimationFrame(raf); raf = 0 }
  const el = host.value
  el.classList.add('is-resting')
  el.style.setProperty('--rx', '0deg')
  el.style.setProperty('--ry', '0deg')
}

watch(tiltEnabled, (enabled) => {
  if (!enabled && host.value) onLeave()
})
</script>

<style scoped>
.glass-object {
  --glass-radius: 32px;
  --glass-depth: 10px;
  position: relative;
  border-radius: var(--glass-radius);
  perspective: 1000px;
  overflow: visible;
}
.glass-object.glass-card {
  border: none;
  background: none;
  -webkit-backdrop-filter: none;
  backdrop-filter: none;
  box-shadow: none;
}
.glass-object.glass-card:hover { box-shadow: none; }
.glass-object.glass-card::before { display: none; }

.glass-object.tilt-on .glass-box,
.glass-object.tilt-on .glass-content {
  transform: rotateX(var(--rx, 0deg)) rotateY(var(--ry, 0deg));
  transform-style: preserve-3d;
}
.glass-object.tilt-on.is-resting .glass-box,
.glass-object.tilt-on.is-resting .glass-content {
  transition: transform 0.3s cubic-bezier(0.22, 1, 0.36, 1);
}
.glass-box {
  position: absolute;
  inset: 0;
  z-index: 1;
  border-radius: inherit;
  pointer-events: none;
}
.glass-box::before,
.glass-box::after {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: inherit;
}
.glass-box::before {
  background: linear-gradient(
    135deg,
    rgba(255, 255, 255, 0.04) 0%,
    rgba(255, 255, 255, 0) 45%,
    rgba(255, 255, 255, 0.025) 100%
  );
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.75),
    inset 0 -1px 0 rgba(255, 255, 255, 0.2),
    0 14px 36px rgba(0, 0, 0, 0.3);
}
.glass-box::after {
  transform: translateZ(calc(-1 * var(--glass-depth)));
  background: rgba(90, 60, 180, 0.1);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.18);
}
.glass-content {
  position: relative;
  z-index: 2;
}
.glass-content > * {
  position: relative;
  z-index: 1;
}
.glass-content > .glare {
  position: absolute;
  z-index: 0;
}
html.dark .glass-box::before {
  background: linear-gradient(
    135deg,
    rgba(255, 255, 255, 0.03) 0%,
    rgba(255, 255, 255, 0) 45%,
    rgba(255, 255, 255, 0.02) 100%
  );
  border-color: rgba(255, 255, 255, 0.28);
}
html.dark .glass-box::after { background: rgba(120, 90, 220, 0.08); }
</style>
