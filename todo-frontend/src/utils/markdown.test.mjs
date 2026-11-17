// 运行：node src/utils/markdown.test.mjs
import assert from 'node:assert'
import { renderMarkdown } from './markdown.js'

// XSS：script 标签和事件处理器必须被转义
assert.ok(!renderMarkdown('<script>alert(1)</script>').includes('<script>'))
// onerror 绝不能进入真实的标签属性中
assert.ok(!/<img[^>]*onerror/.test(renderMarkdown('![x](/a.png" onerror="alert(1))')))
assert.ok(!/<img[^>]*onerror/.test(renderMarkdown('![x](/a.png)')))
// javascript: 协议 URL 会被丢弃
assert.ok(!renderMarkdown('[x](javascript:alert(1))').includes('<a href="javascript'))
assert.ok(renderMarkdown('[x](javascript:alert(1))').includes('x'))
// 图片与链接
assert.strictEqual(
  renderMarkdown('![p](/uploads/a.png)'),
  '<p><img src="/uploads/a.png" alt="p" loading="lazy"></p>'
)
assert.ok(renderMarkdown('[go](https://x.com)').includes('href="https://x.com"'))
// 粗体 / 斜体 / 行内代码
assert.ok(renderMarkdown('**b** *i* `c`').includes('<strong>b</strong> <em>i</em> <code>c</code>'))
// 标题与列表
assert.ok(renderMarkdown('## 标题').includes('<h2>标题</h2>'))
assert.ok(renderMarkdown('- a\n- b').includes('<li>a</li>') && renderMarkdown('- a\n- b').includes('<li>b</li>'))
// 代码块内容原样保留
assert.ok(renderMarkdown('```\n<b>\n```').includes('<pre><code>&lt;b&gt;</code></pre>'))
// 空输入
assert.strictEqual(renderMarkdown(''), '')
assert.strictEqual(renderMarkdown(null), '')

console.log('markdown tests passed')
