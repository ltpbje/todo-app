// 面向待办详情的极简 markdown → HTML 渲染器。
// 安全模型：先把全部输入做 HTML 转义，之后只追加我们自己的标签——
// 因此注入的 <script> 或 onerror= 属性永远无法存活。
// 支持：#/##/### 标题、**粗体**、*斜体*、`行内代码`、``` 代码块、
// ![alt](url) 图片、[text](url) 链接、- 列表、空行分段。

const esc = (s) =>
  s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
   .replace(/"/g, '&quot;').replace(/'/g, '&#39;')

// 只允许 http(s) 和站内相对 URL；拦截 javascript: 等
const safeUrl = (u) => (/^(https?:\/\/|\/)/.test(u) ? u : '')

function inline(s) {
  return s
    .replace(/!\[([^\]]*)\]\(([^)\s]+)\)/g, (_, alt, url) =>
      safeUrl(url) ? `<img src="${url}" alt="${alt}" loading="lazy">` : alt)
    .replace(/\[([^\]]+)\]\(([^)\s]+)\)/g, (_, text, url) =>
      safeUrl(url) ? `<a href="${url}" target="_blank" rel="noopener">${text}</a>` : text)
    .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
    .replace(/\*([^*]+)\*/g, '<em>$1</em>')
    .replace(/`([^`]+)`/g, '<code>$1</code>')
}

export function renderMarkdown(src) {
  if (!src) return ''
  const lines = esc(src).split(/\r?\n/)
  const out = []
  let para = []
  let list = null // 'ul' while inside a list
  let code = null // accumulated ``` block

  const flushPara = () => { if (para.length) { out.push(`<p>${para.join('<br>')}</p>`); para = [] } }
  const flushList = () => { if (list) { out.push(`</ul>`); list = null } }

  for (const line of lines) {
    const fence = line.match(/^```(\w*)$/)
    if (code !== null) {
      if (fence) { out.push(`<pre><code>${code.join('\n')}</code></pre>`); code = null }
      else code.push(line)
      continue
    }
    if (fence) { flushPara(); flushList(); code = []; continue }

    const h = line.match(/^(#{1,3})\s+(.*)$/)
    if (h) { flushPara(); flushList(); out.push(`<h${h[1].length}>${inline(h[2])}</h${h[1].length}>`); continue }

    const li = line.match(/^[-*]\s+(.*)$/)
    if (li) { flushPara(); if (!list) { out.push('<ul>'); list = true } out.push(`<li>${inline(li[1])}</li>`); continue }
    flushList()

    if (!line.trim()) { flushPara(); continue }
    para.push(inline(line))
  }
  flushPara(); flushList()
  if (code !== null) out.push(`<pre><code>${code.join('\n')}</code></pre>`)
  return out.join('\n')
}
