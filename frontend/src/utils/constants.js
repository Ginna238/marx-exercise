/** 题型常量 */
export const QUESTION_TYPES = [
  { value: 'single', label: '单选题', icon: '🔘' },
  { value: 'multiple', label: '多选题', icon: '✅' },
  { value: 'judgment', label: '判断题', icon: '⚖️' }
]

/** 题型 → element-plus tag 类型 */
export const TYPE_TAGS = {
  single: 'primary',
  multiple: 'success',
  judgment: 'warning'
}

/** 题型 → 中文名 */
export const TYPE_NAMES = {
  single: '单选题',
  multiple: '多选题',
  judgment: '判断题'
}

/** 公告 tag 颜色轮换 */
export const NOTICE_TAGS = ['danger', 'warning', 'info', 'success']

/** 更新日志版本标签：按版本号着色 */
export function getVersionTag(version) {
  const parts = String(version).replace(/^v/i, '').split('.').map(Number)
  const [, minor, patch] = parts
  if (patch > 0) return 'primary'   // 修订版本更新 → 蓝色
  if (minor > 0) return 'warning'   // 次版本号更新 → 橙色
  return 'danger'                    // 主版本号更新 → 红色
}
