import api from '../api/request'

export function getAiSettings() {
  return api.get('/ai/settings').then(r => r.data)
}

export function saveAiSettings(settings) {
  return api.put('/ai/settings', settings).then(r => r.data)
}

export function clearAiSettings() {
  return api.delete('/ai/settings')
}
