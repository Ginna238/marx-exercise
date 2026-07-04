import { describe, it, expect } from 'vitest'
import { parseUpdateLogs } from '../utils/markdownParser'

describe('markdownParser', () => {
  const sample = `# v1.1.2
新增背题模式
@2026-07-04

# v1.1.1
修复错题本bug
修复模拟考试显示问题
@2026-07-03`

  it('parses version title', () => {
    const result = parseUpdateLogs(sample)
    expect(result).toHaveLength(2)
    expect(result[0].title).toBe('v1.1.2')
    expect(result[1].title).toBe('v1.1.1')
  })

  it('parses content lines', () => {
    const result = parseUpdateLogs(sample)
    expect(result[0].content).toContain('新增背题模式')
    expect(result[1].content).toContain('修复错题本bug')
    expect(result[1].content).toContain('修复模拟考试显示问题')
  })

  it('parses date from @ line', () => {
    const result = parseUpdateLogs(sample)
    expect(result[0].date).toBe('2026-07-04')
    expect(result[1].date).toBe('2026-07-03')
  })

  it('handles empty input', () => {
    expect(parseUpdateLogs('')).toEqual([])
  })

  it('handles single entry without date', () => {
    const result = parseUpdateLogs('# Test\nsome content')
    expect(result).toHaveLength(1)
    expect(result[0].title).toBe('Test')
    expect(result[0].content).toBe('some content')
    expect(result[0].date).toBe('')
  })

  it('preserves blank lines within entry as newlines', () => {
    const md = `# v1.0.0
第一段内容

第二段内容（空行分隔）

第三段
@2026-07-01`
    const result = parseUpdateLogs(md)
    expect(result).toHaveLength(1)
    // 空行应保留为 \n\n（段落分隔）
    expect(result[0].content).toContain('第一段内容')
    expect(result[0].content).toContain('第二段内容')
    expect(result[0].content).toContain('第三段')
    // 至少有两个连续换行表示段落分隔
    expect(result[0].content).toMatch(/\n\n/)
  })

  it('does not split entry on blank lines', () => {
    const md = `# v1.0.0
line1

line2
@2026-07-01`
    const result = parseUpdateLogs(md)
    expect(result).toHaveLength(1)
    expect(result[0].content).toBe('line1\n\nline2')
  })

  it('extracts list items from - prefixed lines', () => {
    const md = `# v1.0.0
介绍文字
- 功能A
- 功能B
- 功能C
@2026-07-01`
    const result = parseUpdateLogs(md)
    expect(result).toHaveLength(1)
    expect(result[0].listItems).toEqual(['功能A', '功能B', '功能C'])
    expect(result[0].content).toContain('介绍文字')
    expect(result[0].content).not.toContain('功能A')
  })

  it('handles entry with only list items', () => {
    const md = `# v1.0.0
- 第一项
- 第二项
@2026-07-01`
    const result = parseUpdateLogs(md)
    expect(result).toHaveLength(1)
    expect(result[0].listItems).toHaveLength(2)
    expect(result[0].content).toBe('')
  })
})
