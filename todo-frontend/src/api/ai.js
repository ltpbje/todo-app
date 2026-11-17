import api from './request'

/**
 * 通过 fetch 流式读取 AI 接口的 SSE 响应（EventSource 无法携带
 * Authorization 头，所以不能用它）。每个 `data:` 载荷是一个 JSON 值：
 * 一个带引号的字符串片段，或表示结束的 {"done":true}。
 */
export async function aiStream(path, body, onDelta) {
  const token = localStorage.getItem('token')
  const res = await fetch('/api' + path, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
    body: JSON.stringify(body)
  })
  if (!res.ok || !res.body) {
    const err = await res.json().catch(() => ({}))
    throw new Error(err.message || 'AI 请求失败')
  }
  const reader = res.body.getReader()
  const dec = new TextDecoder()
  let buf = ''
  while (true) {
    const { done, value } = await reader.read()
    if (done) break
    buf += dec.decode(value, { stream: true })
    const events = buf.split('\n\n')
    buf = events.pop()
    for (const ev of events) {
      for (const line of ev.split('\n')) {
        if (!line.startsWith('data:')) continue
        const payload = line.slice(5).trim()
        if (!payload) continue
        const parsed = JSON.parse(payload)
        if (parsed.done) return
        onDelta(parsed)
      }
    }
  }
}

export const aiPost = (path, body) => api.post(path, body).then(r => r.data)
