import { ref } from 'vue'

const stored = localStorage.getItem('glass-tilt')
export const tiltEnabled = ref(stored === '1')

export function toggleTilt() {
  tiltEnabled.value = !tiltEnabled.value
  localStorage.setItem('glass-tilt', tiltEnabled.value ? '1' : '0')
}
