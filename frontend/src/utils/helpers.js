/**
 * 解析选项字符串 "A. text" → { label: 'A', text: 'text' }
 * 兼容已解析的 { label, text } 对象格式
 */
export function parseOptions(options) {
  if (!options || !Array.isArray(options)) return []
  return options.map(opt => {
    if (typeof opt === 'object' && opt !== null) {
      return { label: opt.label || '', text: opt.text || opt.value || '' }
    }
    const match = String(opt).match(/^([A-Z])\.\s*(.*)/)
    if (match) return { label: match[1], text: match[2] }
    return { label: '', text: String(opt) }
  })
}

/**
 * 获取答案显示文本（判断题显示 正确/错误）
 */
export function getAnswerText(q) {
  if (!q || !q.answer) return '无答案'
  if (q.type === 'judgment') return q.answer === 'true' ? '正确' : '错误'
  return q.answer
}

/**
 * 格式化时间戳 → YYYY-MM-DD HH:mm
 */
export function formatTime(ts) {
  if (!ts) return ''
  const d = new Date(ts)
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

/**
 * 截取简要内容（CSS line-clamp 优先，此为备选）
 */
export function getSummary(content, maxLines = 2) {
  if (!content) return ''
  const lines = content.split('\n')
  if (lines.length <= maxLines) return content
  return lines.slice(0, maxLines).join('\n') + '\n...'
}
