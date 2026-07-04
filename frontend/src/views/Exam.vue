<template>
  <div class="exam-page">
    <!-- 顶部导航 -->
    <el-header class="exam-header">
      <div class="header-left">
        <el-button text @click="confirmExit">
          <el-icon><ArrowLeft /></el-icon> 退出
        </el-button>
        <h3 class="header-title">📝 模拟考试 · {{ bankName }}</h3>
      </div>
      <div class="header-center">
        <div class="timer">
          <el-icon style="margin-right:4px"><Timer /></el-icon>
          <span class="timer-text">{{ formattedTime }}</span>
        </div>
      </div>
      <div class="header-right">
        <el-tag type="warning">{{ answeredCount }}/{{ totalQuestions }} 已答</el-tag>
      </div>
    </el-header>

    <div class="exam-body">
      <!-- 加载中 -->
      <div v-if="loading" class="loading-container">
        <el-skeleton :rows="8" animated />
      </div>

      <!-- 考试结果 -->
      <div v-else-if="examResult" class="result-container">
        <el-card class="result-card">
          <div class="result-header">
            <div class="result-big-score" :class="scoreLevel">
              <span class="score-number">{{ examResult.score }}</span>
              <span class="score-divider">/</span>
              <span class="score-total">{{ examResult.totalScore }}</span>
            </div>
            <div class="result-label">总分</div>
          </div>

          <el-divider />

          <div class="result-stats">
            <div class="stat-item">
              <span class="stat-label">用时</span>
              <span class="stat-value">{{ examResult.durationText }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">正确率</span>
              <span class="stat-value" :class="scoreLevel">{{ accuracy }}%</span>
            </div>
          </div>

          <el-divider />

          <div class="type-results">
            <div class="type-result-item">
              <div class="type-result-header">
                <el-tag type="primary">单选题</el-tag>
                <span class="type-score">
                  {{ examResult.singleCorrect }}/{{ examResult.singleTotal }}
                  （{{ examResult.singleCorrect * 1 }}分）
                </span>
              </div>
            </div>
            <div class="type-result-item">
              <div class="type-result-header">
                <el-tag type="success">多选题</el-tag>
                <span class="type-score">
                  {{ examResult.multipleCorrect }}/{{ examResult.multipleTotal }}
                  （{{ examResult.multipleCorrect * 2 }}分）
                </span>
              </div>
            </div>
            <div class="type-result-item">
              <div class="type-result-header">
                <el-tag type="warning">判断题</el-tag>
                <span class="type-score">
                  {{ examResult.judgmentCorrect }}/{{ examResult.judgmentTotal }}
                  （{{ examResult.judgmentCorrect * 2 }}分）
                </span>
              </div>
            </div>
          </div>

          <el-divider />

          <!-- 答题详情 -->
          <div class="result-details">
            <h4>答题详情</h4>

            <!-- 单选题 -->
            <div v-if="examResult.singleResults && examResult.singleResults.length" class="detail-section">
              <h5 class="section-label">🔘 单选题（1分/题）</h5>
              <div
                v-for="(q, i) in examResult.singleResults"
                :key="q.questionId"
                class="detail-item"
                :class="q.correct ? 'correct' : 'wrong'"
              >
                <div class="detail-q-header">
                  <span class="detail-q-num">{{ i + 1 }}.</span>
                  <el-tag v-if="q.correct" type="success" size="small" effect="dark">✓</el-tag>
                  <el-tag v-else type="danger" size="small" effect="dark">✗</el-tag>
                  <span class="detail-q-score">+{{ q.score }}分</span>
                  <el-tag v-if="markedIds.has(q.questionId)" type="warning" size="small" class="detail-mark-tag">⭐ 标记</el-tag>
                </div>
                <div class="detail-q-stem">{{ q.stem }}</div>
                <div class="detail-q-answer">
                  <span class="detail-label">你的答案：</span>
                  <span :class="q.correct ? 'text-correct' : 'text-wrong'">{{ formatAnswerText(q.userAnswer, q.options) || '未作答' }}</span>
                </div>
                <div v-if="!q.correct" class="detail-q-answer">
                  <span class="detail-label">正确答案：</span>
                  <span class="text-correct">{{ formatAnswerText(q.correctAnswer, q.options) }}</span>
                </div>
              </div>
            </div>

            <!-- 多选题 -->
            <div v-if="examResult.multipleResults && examResult.multipleResults.length" class="detail-section">
              <h5 class="section-label">✅ 多选题（2分/题）</h5>
              <div
                v-for="(q, i) in examResult.multipleResults"
                :key="q.questionId"
                class="detail-item"
                :class="q.correct ? 'correct' : 'wrong'"
              >
                <div class="detail-q-header">
                  <span class="detail-q-num">{{ i + 1 }}.</span>
                  <el-tag v-if="q.correct" type="success" size="small" effect="dark">✓</el-tag>
                  <el-tag v-else type="danger" size="small" effect="dark">✗</el-tag>
                  <span class="detail-q-score">+{{ q.score }}分</span>
                  <el-tag v-if="markedIds.has(q.questionId)" type="warning" size="small" class="detail-mark-tag">⭐ 标记</el-tag>
                </div>
                <div class="detail-q-stem">{{ q.stem }}</div>
                <div class="detail-q-answer">
                  <span class="detail-label">你的答案：</span>
                  <span :class="q.correct ? 'text-correct' : 'text-wrong'">{{ formatAnswerText(q.userAnswer, q.options) || '未作答' }}</span>
                </div>
                <div v-if="!q.correct" class="detail-q-answer">
                  <span class="detail-label">正确答案：</span>
                  <span class="text-correct">{{ formatAnswerText(q.correctAnswer, q.options) }}</span>
                </div>
              </div>
            </div>

            <!-- 判断题 -->
            <div v-if="examResult.judgmentResults && examResult.judgmentResults.length" class="detail-section">
              <h5 class="section-label">⚖️ 判断题（2分/题）</h5>
              <div
                v-for="(q, i) in examResult.judgmentResults"
                :key="q.questionId"
                class="detail-item"
                :class="q.correct ? 'correct' : 'wrong'"
              >
                <div class="detail-q-header">
                  <span class="detail-q-num">{{ i + 1 }}.</span>
                  <el-tag v-if="q.correct" type="success" size="small" effect="dark">✓</el-tag>
                  <el-tag v-else type="danger" size="small" effect="dark">✗</el-tag>
                  <span class="detail-q-score">+{{ q.score }}分</span>
                  <el-tag v-if="markedIds.has(q.questionId)" type="warning" size="small" class="detail-mark-tag">⭐ 标记</el-tag>
                </div>
                <div class="detail-q-stem">{{ q.stem }}</div>
                <div class="detail-q-answer">
                  <span class="detail-label">你的答案：</span>
                  <span :class="q.correct ? 'text-correct' : 'text-wrong'">{{ formatAnswerText(q.userAnswer, q.options) || '未作答' }}</span>
                </div>
                <div v-if="!q.correct" class="detail-q-answer">
                  <span class="detail-label">正确答案：</span>
                  <span class="text-correct">{{ formatAnswerText(q.correctAnswer, q.options) }}</span>
                </div>
              </div>
            </div>
          </div>

          <div class="result-actions">
            <el-button type="primary" @click="goHome">返回首页</el-button>
            <el-button @click="startNewExam">再来一次</el-button>
          </div>
        </el-card>
      </div>

      <!-- 考试界面 -->
      <div v-else class="exam-content">
        <!-- 左侧栏：进度总览 + 交卷 -->
        <aside class="exam-sidebar">
          <!-- 计时器 + 移动端展开按钮 -->
          <div class="sidebar-header">
            <div class="sidebar-timer">
              <el-icon style="margin-right:4px"><Timer /></el-icon>
              <span class="sidebar-timer-text">{{ formattedTime }}</span>
            </div>
            <el-button
              class="sidebar-toggle-btn"
              text
              size="small"
              @click="sidebarCollapsed = !sidebarCollapsed"
            >
              <el-icon><ArrowDown /></el-icon>
              <span style="margin-left:2px">{{ sidebarCollapsed ? '展开' : '收起' }}</span>
            </el-button>
          </div>

          <el-divider style="margin:12px 0" />

          <!-- 进度总览（可折叠） -->
          <div class="sidebar-progress" :class="{ 'sidebar-collapsed': sidebarCollapsed }">
            <h4 class="sidebar-title">答题进度</h4>

            <!-- 单选题分区 -->
            <div
              class="type-zone"
              :class="{ 'zone-active': currentSection === 'single' }"
              @click="switchSection('single')"
            >
              <div class="zone-header">
                <el-tag type="primary" size="small">单选题</el-tag>
                <span class="zone-count">{{ singleAnsweredCount }}/{{ singleQuestions.length }}</span>
              </div>
              <div class="zone-dots">
                <span
                  v-for="(q, i) in singleQuestions"
                  :key="q.id"
                  class="zdot"
                  :class="{
                    'zdot-answered': getAnswer(q.id),
                    'zdot-current': currentSection === 'single' && i === sectionIndex,
                    'zdot-marked': markedIds.has(q.id)
                  }"
                  @click.stop="gotoQuestion('single', i)"
                >{{ i + 1 }}</span>
              </div>
            </div>

            <!-- 多选题分区 -->
            <div
              class="type-zone"
              :class="{ 'zone-active': currentSection === 'multiple' }"
              @click="switchSection('multiple')"
            >
              <div class="zone-header">
                <el-tag type="success" size="small">多选题</el-tag>
                <span class="zone-count">{{ multipleAnsweredCount }}/{{ multipleQuestions.length }}</span>
              </div>
              <div class="zone-dots">
                <span
                  v-for="(q, i) in multipleQuestions"
                  :key="q.id"
                  class="zdot"
                  :class="{
                    'zdot-answered': getAnswer(q.id),
                    'zdot-current': currentSection === 'multiple' && i === sectionIndex,
                    'zdot-marked': markedIds.has(q.id)
                  }"
                  @click.stop="gotoQuestion('multiple', i)"
                >{{ i + 1 }}</span>
              </div>
            </div>

            <!-- 判断题分区 -->
            <div
              class="type-zone"
              :class="{ 'zone-active': currentSection === 'judgment' }"
              @click="switchSection('judgment')"
            >
              <div class="zone-header">
                <el-tag type="warning" size="small">判断题</el-tag>
                <span class="zone-count">{{ judgmentAnsweredCount }}/{{ judgmentQuestions.length }}</span>
              </div>
              <div class="zone-dots">
                <span
                  v-for="(q, i) in judgmentQuestions"
                  :key="q.id"
                  class="zdot"
                  :class="{
                    'zdot-answered': getAnswer(q.id),
                    'zdot-current': currentSection === 'judgment' && i === sectionIndex,
                    'zdot-marked': markedIds.has(q.id)
                  }"
                  @click.stop="gotoQuestion('judgment', i)"
                >{{ i + 1 }}</span>
              </div>
            </div>

            <!-- 总体进度条 -->
            <div class="total-progress">
              <span class="total-progress-label">总体进度</span>
              <div class="total-progress-bar">
                <div class="total-progress-fill" :style="{ width: totalProgressPercent + '%' }" />
              </div>
              <span class="total-progress-text">{{ answeredCount }}/{{ totalQuestions }}</span>
            </div>
          </div>

          <el-divider style="margin:12px 0" />

          <!-- 交卷按钮 -->
          <div class="sidebar-submit">
            <el-button
              type="danger"
              size="large"
              class="sidebar-submit-btn"
              :loading="submitting"
              :disabled="answeredCount === 0"
              @click="confirmSubmit"
            >
              📤 交卷
            </el-button>
            <p class="sidebar-submit-tip">{{ answeredCount }}/{{ totalQuestions }} 已答 · 交卷后自动评分</p>
          </div>
        </aside>

        <!-- 右侧主内容 -->
        <main class="exam-main">
          <!-- 题目标题 -->
          <div class="question-title-bar">
            <el-tag :type="sectionTagType" size="small">{{ sectionName }}</el-tag>
            <span class="question-number">
              第 {{ getGlobalIndex() + 1 }}/{{ totalQuestions }} 题
            </span>
            <span class="question-local-num">(本区第 {{ sectionIndex + 1 }}/{{ currentSectionQuestions.length }} 题)</span>
            <el-button
              size="small"
              :type="markedIds.has(currentQuestion.id) ? 'warning' : 'default'"
              :icon="Star"
              @click="toggleMark(currentQuestion.id)"
              class="mark-btn"
            >
              {{ markedIds.has(currentQuestion.id) ? '已标记' : '标记' }}
            </el-button>
          </div>

          <!-- 题目卡片 -->
          <el-card class="question-card" shadow="never">
            <div class="question-stem">{{ currentQuestion.stem }}</div>

            <div class="options-area">
              <!-- 单选题 -->
              <template v-if="currentSection === 'single'">
                <el-radio-group v-model="answers[currentQuestion.id]">
                  <div
                    v-for="opt in parsedOptions"
                    :key="opt.label"
                    class="option-item"
                    :class="{
                      'option-selected': answers[currentQuestion.id] === opt.label
                    }"
                    @click="answers[currentQuestion.id] = opt.label"
                  >
                    <el-radio :value="opt.label" class="option-radio">
                      <span class="option-label">{{ opt.label }}.</span>
                      <span class="option-text">{{ opt.text }}</span>
                    </el-radio>
                  </div>
                </el-radio-group>
              </template>

              <!-- 多选题 -->
              <template v-else-if="currentSection === 'multiple'">
                <el-checkbox-group v-model="multiAnswers[currentQuestion.id]">
                  <div
                    v-for="opt in parsedOptions"
                    :key="opt.label"
                    class="option-item"
                    :class="{
                      'option-selected': multiAnswers[currentQuestion.id]?.includes(opt.label)
                    }"
                  >
                    <el-checkbox :value="opt.label" class="option-checkbox">
                      <span class="option-label">{{ opt.label }}.</span>
                      <span class="option-text">{{ opt.text }}</span>
                    </el-checkbox>
                  </div>
                </el-checkbox-group>
              </template>

              <!-- 判断题 -->
              <template v-else-if="currentSection === 'judgment'">
                <el-radio-group v-model="answers[currentQuestion.id]">
                  <div
                    class="option-item"
                    :class="{ 'option-selected': answers[currentQuestion.id] === '正确' }"
                    @click="answers[currentQuestion.id] = '正确'"
                  >
                    <el-radio value="正确" class="option-radio">
                      <span class="option-label">○</span>
                      <span class="option-text">正确</span>
                    </el-radio>
                  </div>
                  <div
                    class="option-item"
                    :class="{ 'option-selected': answers[currentQuestion.id] === '错误' }"
                    @click="answers[currentQuestion.id] = '错误'"
                  >
                    <el-radio value="错误" class="option-radio">
                      <span class="option-label">○</span>
                      <span class="option-text">错误</span>
                    </el-radio>
                  </div>
                </el-radio-group>
              </template>
            </div>
          </el-card>

          <!-- 导航按钮 -->
          <div class="nav-buttons">
            <el-button @click="prevQuestion" :disabled="isFirstQuestion">
              <el-icon><ArrowLeft /></el-icon> 上一题
            </el-button>
            <el-button @click="nextQuestion" :disabled="isLastQuestion">
              下一题 <el-icon><ArrowRight /></el-icon>
            </el-button>
          </div>
        </main>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, ArrowRight, ArrowDown, Timer, Star } from '@element-plus/icons-vue'
import { startExam, submitExam } from '../api/exam'
import { TYPE_NAMES, TYPE_TAGS } from '../utils/constants'
import { parseOptions } from '../utils/helpers'

const route = useRoute()
const router = useRouter()

const bankId = ref(parseInt(route.query.bankId))
const bankName = ref(route.query.bankName || '')
const examId = ref(null)
const loading = ref(true)
const submitting = ref(false)
const examResult = ref(null)

// 题目数据
const singleQuestions = ref([])
const multipleQuestions = ref([])
const judgmentQuestions = ref([])
const currentSection = ref('single')
const sectionIndex = ref(0)

// 答案存储
const answers = ref({})         // 单选/判断题答案: questionId -> answer
const multiAnswers = ref({})    // 多选题答案: questionId -> string[]

// 标记题目（仅本场考试有效）
const markedIds = ref(new Set())

function toggleMark(questionId) {
  const s = markedIds.value
  if (s.has(questionId)) {
    s.delete(questionId)
  } else {
    s.add(questionId)
  }
  // 触发响应式更新
  markedIds.value = new Set(s)
}

// 计时
const startTime = ref(null)
const elapsedSeconds = ref(0)
const timerInterval = ref(null)

// 侧栏折叠（移动端默认折叠）
const sidebarCollapsed = ref(true)

const totalQuestions = computed(() =>
  singleQuestions.value.length + multipleQuestions.value.length + judgmentQuestions.value.length
)

const answeredCount = computed(() => {
  let count = Object.keys(answers.value).length
  // 多选题答案已通过 saveMultiAnswers 合并到 answers，这里只补计不在 answers 中的
  for (const key of Object.keys(multiAnswers.value)) {
    if (multiAnswers.value[key] && multiAnswers.value[key].length > 0 && !(parseInt(key) in answers.value)) {
      count++
    }
  }
  return count
})

const singleAnsweredCount = computed(() => {
  let c = 0
  for (const q of singleQuestions.value) {
    if (getAnswer(q.id)) c++
  }
  return c
})

const multipleAnsweredCount = computed(() => {
  let c = 0
  for (const q of multipleQuestions.value) {
    if (getAnswer(q.id)) c++
  }
  return c
})

const judgmentAnsweredCount = computed(() => {
  let c = 0
  for (const q of judgmentQuestions.value) {
    if (getAnswer(q.id)) c++
  }
  return c
})

const totalProgressPercent = computed(() => {
  return totalQuestions.value > 0 ? Math.round(answeredCount.value / totalQuestions.value * 100) : 0
})

const currentSectionQuestions = computed(() => {
  switch (currentSection.value) {
    case 'single': return singleQuestions.value
    case 'multiple': return multipleQuestions.value
    case 'judgment': return judgmentQuestions.value
    default: return []
  }
})

const currentQuestion = computed(() => {
  return currentSectionQuestions.value[sectionIndex.value] || {}
})

const sectionName = computed(() => TYPE_NAMES[currentSection.value] || '')

const sectionTagType = computed(() => TYPE_TAGS[currentSection.value] || 'info')

const parsedOptions = computed(() => parseOptions(currentQuestion.value?.options))

const isFirstQuestion = computed(() => {
  return currentSection.value === 'single' && sectionIndex.value === 0
})

const isLastQuestion = computed(() => {
  return (
    currentSection.value === 'judgment' &&
    sectionIndex.value === judgmentQuestions.value.length - 1
  )
})

const formattedTime = computed(() => {
  const h = Math.floor(elapsedSeconds.value / 3600)
  const m = Math.floor((elapsedSeconds.value % 3600) / 60)
  const s = elapsedSeconds.value % 60
  const pad = n => n.toString().padStart(2, '0')
  if (h > 0) return `${pad(h)}:${pad(m)}:${pad(s)}`
  return `${pad(m)}:${pad(s)}`
})

const scoreLevel = computed(() => {
  if (!examResult.value) return ''
  const pct = examResult.value.totalScore > 0
    ? examResult.value.score / examResult.value.totalScore
    : 0
  if (pct >= 0.9) return 'score-excellent'
  if (pct >= 0.7) return 'score-good'
  if (pct >= 0.6) return 'score-pass'
  return 'score-fail'
})

const accuracy = computed(() => {
  if (!examResult.value) return 0
  return examResult.value.totalScore > 0
    ? Math.round(examResult.value.score / examResult.value.totalScore * 100)
    : 0
})

const showAllResults = computed(() => false)

onMounted(async () => {
  await loadExam()
  startTimer()
})

onUnmounted(() => {
  clearInterval(timerInterval.value)
})

function startTimer() {
  startTime.value = Date.now()
  timerInterval.value = setInterval(() => {
    elapsedSeconds.value = Math.floor((Date.now() - startTime.value) / 1000)
  }, 1000)
}

function getAnswer(questionId) {
  if (answers.value[questionId]) return answers.value[questionId]
  if (multiAnswers.value[questionId] && multiAnswers.value[questionId].length > 0) {
    return multiAnswers.value[questionId].sort().join('')
  }
  return null
}

function isAnswerCorrect(q) {
  return false // not used during exam
}

function formatAnswerText(answer, options) {
  if (!answer || !options || !options.length) return answer || ''
  // 判断题：直接返回（正确/错误）
  if (answer === '正确' || answer === '错误') return answer
  // 拆分多选答案（如 "ABC" -> ["A","B","C"]）
  const labels = answer.split('').filter(c => c >= 'A' && c <= 'Z')
  return labels.map(label => {
    const found = options.find(o => o.startsWith(label + '.'))
    return found || label
  }).join('，')
}

function getGlobalIndex() {
  let idx = 0
  if (currentSection.value === 'single') idx = sectionIndex.value
  else if (currentSection.value === 'multiple') idx = singleQuestions.value.length + sectionIndex.value
  else if (currentSection.value === 'judgment') idx = singleQuestions.value.length + multipleQuestions.value.length + sectionIndex.value
  return idx
}

function gotoQuestion(section, index) {
  saveMultiAnswers()
  currentSection.value = section
  sectionIndex.value = index
}

function switchSection(section) {
  // 保存当前多选题答案（合并到answers）
  saveMultiAnswers()
  currentSection.value = section
  sectionIndex.value = 0
}

function saveMultiAnswers() {
  for (const [qid, vals] of Object.entries(multiAnswers.value)) {
    if (vals && vals.length > 0) {
      answers.value[parseInt(qid)] = vals.sort().join('')
    }
  }
}

function prevQuestion() {
  if (currentSection.value === 'multiple') {
    saveMultiAnswers()
  }
  if (sectionIndex.value > 0) {
    sectionIndex.value--
  } else {
    // 切换到上一题型
    if (currentSection.value === 'multiple') {
      currentSection.value = 'single'
      sectionIndex.value = singleQuestions.value.length - 1
    } else if (currentSection.value === 'judgment') {
      currentSection.value = 'multiple'
      sectionIndex.value = multipleQuestions.value.length - 1
    }
  }
}

function nextQuestion() {
  if (currentSection.value === 'multiple') {
    saveMultiAnswers()
  }
  if (sectionIndex.value < currentSectionQuestions.value.length - 1) {
    sectionIndex.value++
  } else {
    // 切换到下一题型
    if (currentSection.value === 'single') {
      currentSection.value = 'multiple'
      sectionIndex.value = 0
    } else if (currentSection.value === 'multiple') {
      currentSection.value = 'judgment'
      sectionIndex.value = 0
    }
  }
}

async function loadExam() {
  loading.value = true
  try {
    const res = await startExam(bankId.value)
    if (res.code === 200) {
      const data = res.data
      examId.value = data.examId
      bankName.value = data.bankName
      singleQuestions.value = data.singleQuestions || []
      multipleQuestions.value = data.multipleQuestions || []
      judgmentQuestions.value = data.judgmentQuestions || []
    } else {
      ElMessage.error(res.message || '加载考试失败')
      goHome()
    }
  } catch (e) {
    console.error('加载考试失败:', e)
    ElMessage.error('加载考试失败，请重试')
    goHome()
  } finally {
    loading.value = false
  }
}

async function confirmSubmit() {
  try {
    await ElMessageBox.confirm(
      `确定要交卷吗？\n当前已答 ${answeredCount.value}/${totalQuestions.value} 题\n用时 ${formattedTime.value}`,
      '确认交卷',
      { confirmButtonText: '确认交卷', cancelButtonText: '继续答题', type: 'warning' }
    )
    await doSubmit()
  } catch {
    // cancelled
  }
}

async function doSubmit() {
  submitting.value = true
  // 保存多选题答案
  saveMultiAnswers()

  clearInterval(timerInterval.value)

  try {
    const res = await submitExam({
      examId: examId.value,
      bankId: bankId.value,
      durationSeconds: elapsedSeconds.value,
      answers: answers.value
    })
    if (res.code === 200) {
      examResult.value = res.data
      ElMessage.success('交卷成功！')
    } else {
      ElMessage.error(res.message || '提交失败')
      startTimer()
    }
  } catch (e) {
    console.error('提交考试失败:', e)
    ElMessage.error('提交失败，请重试')
    startTimer()
  } finally {
    submitting.value = false
  }
}

function confirmExit() {
  if (examResult.value) {
    goHome()
    return
  }
  ElMessageBox.confirm('退出将丢失当前考试进度，确定退出吗？', '提示', {
    confirmButtonText: '确定退出', cancelButtonText: '继续答题', type: 'warning'
  }).then(() => goHome()).catch(() => {})
}

function startNewExam() {
  examResult.value = null
  answers.value = {}
  multiAnswers.value = {}
  currentSection.value = 'single'
  sectionIndex.value = 0
  elapsedSeconds.value = 0
  loadExam()
  startTimer()
}

function goHome() {
  router.push('/')
}
</script>

<style scoped>
.exam-page {
  min-height: 100vh;
  background: #f5f7fa;
}

.exam-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  height: 60px !important;
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 200px;
}

.header-title {
  font-size: 16px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 200px;
}

.header-center {
  display: flex;
  align-items: center;
  justify-content: center;
}

.timer {
  display: flex;
  align-items: center;
  background: #f0f2f5;
  padding: 6px 16px;
  border-radius: 20px;
}

.timer-text {
  font-size: 18px;
  font-weight: 700;
  color: #303133;
  font-variant-numeric: tabular-nums;
  letter-spacing: 1px;
}

.header-right {
  display: flex;
  gap: 8px;
  min-width: 200px;
  justify-content: flex-end;
}

.exam-body {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.loading-container {
  padding: 60px 0;
}

/* ========== 考试界面 ========== */

.exam-content {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}

/* ---- 左侧栏 ---- */
.exam-sidebar {
  width: 280px;
  flex-shrink: 0;
  background: #fff;
  border-radius: 12px;
  padding: 20px 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  position: sticky;
  top: 80px;
  max-height: calc(100vh - 100px);
  overflow-y: auto;
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.sidebar-timer {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f0f2f5;
  padding: 8px 16px;
  border-radius: 20px;
  font-size: 20px;
  font-weight: 700;
  color: #303133;
  font-variant-numeric: tabular-nums;
  letter-spacing: 1px;
}

.sidebar-timer-text {
  margin-left: 4px;
}

.sidebar-toggle-btn {
  display: none;
}

.sidebar-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
}

/* 题型分区 */
.type-zone {
  padding: 10px;
  border-radius: 8px;
  margin-bottom: 8px;
  cursor: pointer;
  border: 2px solid transparent;
  transition: all 0.2s;
}

.type-zone:hover {
  background: #f5f7fa;
}

.type-zone.zone-active {
  border-color: #409eff;
  background: #ecf5ff;
}

.zone-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.zone-count {
  font-size: 13px;
  font-weight: 600;
  color: #606266;
}

.zone-dots {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
}

.zdot {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 4px;
  background: #e9ecef;
  cursor: pointer;
  transition: all 0.2s;
  flex-shrink: 0;
  font-size: 11px;
  font-weight: 600;
  color: #909399;
  border: 2px solid transparent;
  user-select: none;
}

.zdot:hover {
  transform: scale(1.15);
  border-color: #409eff;
  color: #409eff;
}

.zdot-answered {
  background: #409eff;
  border-color: #409eff;
  color: #fff;
}

.zdot-current {
  box-shadow: 0 0 0 2px #fff, 0 0 0 3px #409eff;
  border-color: #409eff;
  color: #fff;
  background: #409eff;
}

.zdot-marked {
  border-color: #e6a23c !important;
  box-shadow: 0 0 0 2px #fff, 0 0 0 3px #e6a23c !important;
}

/* 总体进度 */
.total-progress {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 0;
  margin-top: 4px;
}

.total-progress-label {
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
}

.total-progress-bar {
  flex: 1;
  height: 6px;
  background: #e9ecef;
  border-radius: 3px;
  overflow: hidden;
}

.total-progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #409eff, #67c23a);
  border-radius: 3px;
  transition: width 0.3s ease;
}

.total-progress-text {
  font-size: 12px;
  color: #606266;
  font-weight: 500;
  white-space: nowrap;
}

/* 交卷按钮 */
.sidebar-submit {
  text-align: center;
}

.sidebar-submit-btn {
  width: 100%;
  font-size: 15px;
}

.sidebar-submit-tip {
  color: #909399;
  font-size: 12px;
  margin-top: 6px;
}

/* ---- 右侧主内容 ---- */
.exam-main {
  flex: 1;
  min-width: 0;
}

.question-title-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}

.question-number {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
}

.question-local-num {
  font-size: 12px;
  color: #909399;
}

.mark-btn {
  margin-left: auto;
}

.question-card {
  margin-bottom: 20px;
}

.question-stem {
  font-size: 16px;
  line-height: 1.8;
  color: #303133;
  margin-bottom: 24px;
  padding: 8px 0;
}

.options-area {
  margin-top: 16px;
}

.option-item {
  padding: 12px 16px;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  width: 100%;
  margin-bottom: 8px;
}

.option-item:hover {
  border-color: #409eff;
  background: #f5f7fa;
}

.option-selected {
  border-color: #409eff !important;
  background: #ecf5ff !important;
}

.option-item :deep(.el-radio),
.option-item :deep(.el-checkbox) {
  display: flex !important;
  align-items: flex-start !important;
  width: 100%;
  padding: 0;
  height: auto;
}

.option-item :deep(.el-radio__label),
.option-item :deep(.el-checkbox__label) {
  display: inline;
  white-space: normal;
  word-break: break-word;
  padding-left: 8px;
  line-height: 1.6;
  flex: 1;
}

.option-radio, .option-checkbox {
  display: flex;
  align-items: flex-start;
  width: 100%;
  height: auto;
  white-space: normal;
  text-align: left;
}

.option-label {
  font-weight: 600;
  margin-right: 8px;
  color: #606266;
  flex-shrink: 0;
}

.option-text {
  color: #303133;
}

.nav-buttons {
  display: flex;
  justify-content: space-between;
  margin-bottom: 24px;
}

/* ========== 考试结果 ========== */

.result-container {
  padding: 20px 0;
}

.result-card {
  max-width: 700px;
  margin: 0 auto;
}

.result-header {
  text-align: center;
  padding: 24px 0 12px;
}

.result-big-score {
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: 4px;
}

.score-number {
  font-size: 56px;
  font-weight: 800;
}

.score-divider {
  font-size: 32px;
  font-weight: 300;
  color: #909399;
}

.score-total {
  font-size: 32px;
  font-weight: 600;
  color: #909399;
}

.result-label {
  font-size: 16px;
  color: #909399;
  margin-top: 4px;
}

/* 分数等级 */
.score-excellent .score-number { color: #67c23a; }
.score-good .score-number { color: #409eff; }
.score-pass .score-number { color: #e6a23c; }
.score-fail .score-number { color: #f56c6c; }
.score-excellent { color: #67c23a; }
.score-good { color: #409eff; }
.score-pass { color: #e6a23c; }
.score-fail { color: #f56c6c; }

.result-stats {
  display: flex;
  justify-content: space-around;
  padding: 8px 0;
}

.stat-item {
  text-align: center;
}

.stat-label {
  display: block;
  font-size: 13px;
  color: #909399;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 20px;
  font-weight: 600;
}

.type-results {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 8px 0;
}

.type-result-item {
  padding: 8px 0;
}

.type-result-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.type-score {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}

.result-details {
  padding: 8px 0;
}

.result-details h4 {
  font-size: 16px;
  margin-bottom: 16px;
  color: #303133;
}

.detail-section {
  margin-bottom: 24px;
}

.section-label {
  font-size: 14px;
  color: #606266;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #f0f0f0;
}

.detail-item {
  padding: 12px;
  border-radius: 8px;
  margin-bottom: 8px;
  border: 1px solid #e9ecef;
}

.detail-item.correct {
  background: #f0f9eb;
  border-color: #e1f3d8;
}

.detail-item.wrong {
  background: #fef0f0;
  border-color: #fde2e2;
}

.detail-q-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.detail-q-num {
  font-weight: 600;
  color: #303133;
  font-size: 14px;
}

.detail-q-score {
  font-size: 12px;
  color: #909399;
  margin-left: 8px;
}

.detail-mark-tag {
  margin-left: auto;
}

.detail-q-stem {
  font-size: 14px;
  line-height: 1.6;
  color: #303133;
  margin-bottom: 8px;
  padding-left: 4px;
}

.detail-q-answer {
  font-size: 13px;
  padding-left: 4px;
  margin-bottom: 4px;
}

.detail-label {
  color: #909399;
  margin-right: 4px;
}

.text-correct { color: #67c23a; font-weight: 500; }
.text-wrong { color: #f56c6c; font-weight: 500; }

.result-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
  padding: 24px 0 8px;
}

/* 移动端响应式 */
@media (max-width: 768px) {
  .exam-header {
    flex-wrap: wrap;
    height: auto !important;
    padding: 8px 12px !important;
    gap: 6px;
  }
  .header-left { min-width: 0; }
  .header-title { font-size: 14px; max-width: 120px; }
  .header-right { min-width: 0; }
  .exam-body { padding: 12px; }

  .exam-content {
    flex-direction: column;
  }

  .exam-sidebar {
    width: 100%;
    position: static;
    max-height: none;
    padding: 12px;
    margin-bottom: 12px;
  }

  .sidebar-header {
    width: 100%;
  }

  .sidebar-toggle-btn {
    display: inline-flex !important;
  }

  .sidebar-timer {
    font-size: 16px;
    padding: 4px 12px;
    flex-shrink: 0;
  }

  .exam-sidebar .el-divider { margin: 8px 0 !important; }

  /* 折叠状态：隐藏进度区域，只保留提示文字 */
  .sidebar-progress.sidebar-collapsed {
    display: none;
  }

  .sidebar-progress {
    width: 100%;
  }

  .sidebar-title {
    font-size: 13px;
    margin-bottom: 8px;
  }

  .type-zone {
    padding: 8px;
  }

  .zone-header { margin-bottom: 6px; }
  .zone-header .el-tag { font-size: 11px; }
  .zone-count { font-size: 12px; }
  .zdot { width: 22px; height: 22px; font-size: 10px; }

  .total-progress { display: flex; }

  .sidebar-submit {
    width: 100%;
  }

  .sidebar-submit-btn { font-size: 14px; }
  .sidebar-submit-tip { font-size: 11px; }

  .exam-main { width: 100%; }

  .question-stem { font-size: 15px; }
  .score-number { font-size: 40px; }
  .score-divider, .score-total { font-size: 24px; }
  .nav-buttons { flex-direction: row; gap: 8px; }
}
</style>
