import { describe, it, expect } from 'vitest'
import { parseOptions, getAnswerText, formatTime, getSummary } from '../utils/helpers'
import { QUESTION_TYPES, TYPE_TAGS, TYPE_NAMES } from '../utils/constants'

describe('helpers', () => {
  describe('parseOptions', () => {
    it('parses "A. text" format', () => {
      const result = parseOptions(['A. 北京', 'B. 上海', 'C. 广州'])
      expect(result).toEqual([
        { label: 'A', text: '北京' },
        { label: 'B', text: '上海' },
        { label: 'C', text: '广州' }
      ])
    })

    it('handles already parsed objects', () => {
      const result = parseOptions([{ label: 'A', text: '北京' }])
      expect(result).toEqual([{ label: 'A', text: '北京' }])
    })

    it('returns empty array for null/undefined', () => {
      expect(parseOptions(null)).toEqual([])
      expect(parseOptions(undefined)).toEqual([])
    })
  })

  describe('getAnswerText', () => {
    it('shows 正确/错误 for judgment type', () => {
      expect(getAnswerText({ type: 'judgment', answer: 'true' })).toBe('正确')
      expect(getAnswerText({ type: 'judgment', answer: 'false' })).toBe('错误')
    })

    it('returns answer directly for other types', () => {
      expect(getAnswerText({ type: 'single', answer: 'A' })).toBe('A')
    })

    it('returns 无答案 if no answer', () => {
      expect(getAnswerText({})).toBe('无答案')
      expect(getAnswerText(null)).toBe('无答案')
    })
  })

  describe('formatTime', () => {
    it('formats timestamp correctly', () => {
      const ts = new Date('2026-07-04 12:30').getTime()
      const result = formatTime(ts)
      expect(result).toMatch(/2026-07-04/)
    })

    it('returns empty string for falsy input', () => {
      expect(formatTime(null)).toBe('')
      expect(formatTime('')).toBe('')
    })
  })

  describe('getSummary', () => {
    it('returns full content if within max lines', () => {
      expect(getSummary('hello')).toBe('hello')
    })

    it('truncates with ... for more than 2 lines', () => {
      const text = 'line1\nline2\nline3\nline4'
      const summary = getSummary(text)
      expect(summary).toContain('...')
      expect(summary.split('\n').length).toBe(3) // 2 lines + ...
    })
  })
})

describe('constants', () => {
  it('QUESTION_TYPES has 3 types', () => {
    expect(QUESTION_TYPES).toHaveLength(3)
    expect(QUESTION_TYPES[0].value).toBe('single')
  })

  it('TYPE_TAGS covers all types', () => {
    expect(TYPE_TAGS.single).toBe('primary')
    expect(TYPE_TAGS.multiple).toBe('success')
    expect(TYPE_TAGS.judgment).toBe('warning')
  })

  it('TYPE_NAMES covers all types', () => {
    expect(TYPE_NAMES.single).toBe('单选题')
    expect(TYPE_NAMES.multiple).toBe('多选题')
    expect(TYPE_NAMES.judgment).toBe('判断题')
  })
})
