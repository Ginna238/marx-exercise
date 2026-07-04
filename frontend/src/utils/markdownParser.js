/**
 * 解析 update-logs.md / notice.md 文件内容
 *
 * 格式约定：
 * - # 一级标题 → 版本号/标题
 * - 普通文本 → 日志/公告内容（多行合并，空行为段落分隔）
 * - - 无序列表项 → 列表项，详情页展示，侧边栏仅显示数量
 * - @ 开头行 → 更新日期
 * - 条目之间用空行分隔
 */

export function parseUpdateLogs(text) {
  const lines = text.split('\n')
  const entries = []
  let current = null

  for (const raw of lines) {
    const line = raw.trimEnd()

    if (line.startsWith('# ')) {
      if (current && current.title) {
        flushEntry(current, entries)
      }
      current = {
        title: line.slice(2).trim(),
        content: '',
        listItems: [],
        date: ''
      }
    } else if (line.startsWith('@')) {
      if (current) current.date = line.slice(1).trim()
    } else if (current) {
      if (line.startsWith('- ')) {
        current.listItems.push(line.slice(2).trim())
      } else {
        // 非列表文本：保留内容（含空行 → 段落分隔）
        current.content += (current.content ? '\n' : '') + line
      }
    }
  }
  if (current && current.title) flushEntry(current, entries)

  return entries
}

function flushEntry(entry, entries) {
  entry.content = entry.content.replace(/\n{3,}/g, '\n\n')
  entries.push(entry)
}

/**
 * 截取简略内容：最多显示两行（按换行符或中文字数 ~50字/行）
 */
export function getSummary(content, maxLines = 2) {
  if (!content) return ''
  const lines = content.split('\n')
  if (lines.length <= maxLines) return content
  // 取前 maxLines 行，每行截断
  const summary = lines.slice(0, maxLines).join('\n')
  return summary + '\n...'
}
