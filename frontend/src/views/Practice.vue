<template>
  <div class="practice">
    <!-- 顶部导航 -->
    <el-header class="practice-header">
      <div class="header-left">
        <el-button text @click="goBack">
          <el-icon><ArrowLeft /></el-icon> 返回
        </el-button>
        <h3 class="header-title">{{ bankName }} · {{ typeName }}</h3>
      </div>
      <div class="header-center">
        <div class="custom-progress">
          <div class="progress-bg">
            <div class="progress-correct-seg" :style="{ width: correctPercent + '%' }" />
            <div class="progress-wrong-seg" :style="{ width: wrongPercent + '%' }" />
          </div>
          <span class="progress-text">{{ answeredCount }}/{{ totalCount }}</span>
        </div>
      </div>
      <div class="header-right">
        <el-tag v-if="questionType !== 'wrong'" type="info">第 {{ roundNum }} 轮</el-tag>
        <el-tag v-if="questionType === 'wrong'" type="warning">错题练习</el-tag>
        <el-tag :type="accuracy > 70 ? 'success' : accuracy > 40 ? 'warning' : 'danger'">
          正确率 {{ accuracy }}%
        </el-tag>
      </div>
    </el-header>

    <div class="practice-body">
      <!-- 加载中 -->
      <div v-if="loading" class="loading-container">
        <el-skeleton :rows="6" animated />
      </div>

      <!-- 完成的提示 -->
      <div v-else-if="completed" class="complete-container">
        <el-result icon="success" title="本轮已完成！" :sub-title="`正确率：${accuracy}%`">
          <template #extra>
            <el-button type="primary" @click="startNewRound">开始新一轮</el-button>
            <el-button @click="goHome">返回首页</el-button>
          </template>
        </el-result>
      </div>

      <!-- 题目区域 -->
      <div v-else-if="currentQuestion" class="question-area">
        <!-- 进度信息 -->
        <div class="progress-info">
          <span>已答 {{ answeredCount }}/{{ totalCount }} 题</span>
          <span class="progress-correct">正确 {{ correctCount }} 题</span>
        </div>

        <!-- 题目卡片 -->
        <el-card class="question-card" shadow="never">
          <template #header>
            <div class="question-header">
              <el-tag :type="questionTagType" size="small">{{ typeName }}</el-tag>
              <span class="question-number">第 {{ currentIndex + 1 }} 题</span>
            </div>
          </template>

          <div class="question-stem">{{ currentQuestion.stem }}</div>

          <!-- 选项区域 -->
          <div class="options-area">
            <!-- 单选题 -->
            <template v-if="effectiveType === 'single'">
              <el-radio-group v-model="selectedAnswer" class="option-group">
                <div
                  v-for="opt in parsedOptions"
                  :key="opt.label"
                  class="option-item"
                  :class="{
                    'option-correct': showResult && opt.label === correctAnswer,
                    'option-wrong': showResult && selectedAnswer === opt.label && opt.label !== correctAnswer
                  }"
                  @click="!showResult && (selectedAnswer = opt.label)"
                >
                  <el-radio :value="opt.label" :disabled="showResult" class="option-radio">
                    <span class="option-label">{{ opt.label }}.</span>
                    <span class="option-text">{{ opt.text }}</span>
                  </el-radio>
                </div>
              </el-radio-group>
            </template>

            <!-- 多选题 -->
            <template v-else-if="effectiveType === 'multiple'">
              <el-checkbox-group v-model="selectedAnswers" class="option-group">
                <div
                  v-for="opt in parsedOptions"
                  :key="opt.label"
                  class="option-item"
                  :class="{
                    'option-correct': showResult && correctAnswerArray.includes(opt.label),
                    'option-wrong': showResult && selectedAnswers.includes(opt.label) && !correctAnswerArray.includes(opt.label)
                  }"
                >
                  <el-checkbox :value="opt.label" :disabled="showResult" class="option-checkbox">
                    <span class="option-label">{{ opt.label }}.</span>
                    <span class="option-text">{{ opt.text }}</span>
                  </el-checkbox>
                </div>
              </el-checkbox-group>
            </template>

            <!-- 判断题 -->
            <template v-else-if="effectiveType === 'judgment'">
              <el-radio-group v-model="selectedAnswer" class="option-group">
                <div
                  class="option-item"
                  :class="{
                    'option-correct': showResult && '正确' === correctAnswer,
                    'option-wrong': showResult && selectedAnswer === '正确' && '正确' !== correctAnswer
                  }"
                  @click="!showResult && (selectedAnswer = '正确')"
                >
                  <el-radio value="正确" :disabled="showResult" class="option-radio">
                    <span class="option-label">○</span>
                    <span class="option-text">正确</span>
                  </el-radio>
                </div>
                <div
                  class="option-item"
                  :class="{
                    'option-correct': showResult && '错误' === correctAnswer,
                    'option-wrong': showResult && selectedAnswer === '错误' && '错误' !== correctAnswer
                  }"
                  @click="!showResult && (selectedAnswer = '错误')"
                >
                  <el-radio value="错误" :disabled="showResult" class="option-radio">
                    <span class="option-label">○</span>
                    <span class="option-text">错误</span>
                  </el-radio>
                </div>
              </el-radio-group>
            </template>
          </div>

          <!-- 解析区域 -->
          <div v-if="showResult" class="result-area" :class="isCurrentCorrect ? 'correct' : 'wrong'">
            <div class="result-icon">{{ isCurrentCorrect ? '✅' : '❌' }}</div>
            <div class="result-text">
              <strong>{{ isCurrentCorrect ? '回答正确！' : '回答错误' }}</strong>
              <p class="correct-answer">正确答案：{{ correctAnswer }}
                <span v-if="correctAnswerText && correctAnswerText !== correctAnswer">
                  ({{ correctAnswerText }})
                </span>
              </p>
              <p v-if="wrongCount > 0" class="wrong-count-hint">
                ⚠️ 本题已做错 <strong>{{ wrongCount }}</strong> 次
              </p>
            </div>
          </div>
        </el-card>

        <!-- 操作按钮 -->
        <div class="action-buttons">
          <el-button v-if="!showResult" type="primary" size="large" @click="submitCurrentAnswer" :disabled="!canSubmit">
            提交答案
          </el-button>
          <el-button v-else type="primary" size="large" @click="nextQuestion">
            {{ hasMoreQuestions ? (isCurrentCorrect ? '下一题 (1s)' : '下一题') : '查看结果' }}
          </el-button>
          <el-button
            v-if="showResult && wrongCount > 0"
            type="danger"
            size="large"
            plain
            @click="removeFromWrongBook"
          >
            移出错题本
          </el-button>
        </div>
      </div>

      <!-- 没有题目 -->
      <div v-else class="empty-container">
        <el-empty description="暂无更多题目">
          <el-button type="primary" @click="goHome">返回首页</el-button>
        </el-empty>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { startPractice, submitAnswer, checkRoundComplete, startWrongPractice } from '../api/practice'
import { removeWrongQuestion } from '../api/wrong'
import { TYPE_NAMES, TYPE_TAGS } from '../utils/constants'
import { parseOptions } from '../utils/helpers'

const route = useRoute()
const router = useRouter()

const bankId = ref(parseInt(route.query.bankId))
const bankName = ref(route.query.bankName || '')
const questionType = ref(route.query.questionType || 'single')
const roundNum = ref(parseInt(route.query.roundNum) || 1)

const loading = ref(true)
const submitting = ref(false)
const completed = ref(false)
const autoNextTimer = ref(null)

const questions = ref([])
const currentIndex = ref(0)
const totalCount = ref(0)
const answeredCount = ref(0)
const correctCount = ref(0)
const accuracy = ref(0)
const progressPercent = ref(0)

// 当前题目状态
const selectedAnswer = ref('')
const selectedAnswers = ref([])
const showResult = ref(false)
const isCurrentCorrect = ref(false)
const correctAnswer = ref('')
const correctAnswerText = ref('')
const wrongCount = ref(0)

const typeNames = { single: '单选题', multiple: '多选题', judgment: '判断题' }
const effectiveType = computed(() => {
  // 错题练习模式下使用当前题目的实际题型
  if (questionType.value === 'wrong' && currentQuestion.value?.type) {
    return currentQuestion.value.type
  }
  return questionType.value
})
const typeName = computed(() => TYPE_NAMES[effectiveType.value] || effectiveType.value || '错题练习')
const questionTagType = computed(() => TYPE_TAGS[effectiveType.value] || 'info')

const currentQuestion = computed(() => questions.value[currentIndex.value])
const hasMoreQuestions = computed(() => currentIndex.value + 1 < questions.value.length)
const canSubmit = computed(() => {
  if (effectiveType.value === 'multiple') return selectedAnswers.value.length > 0
  return selectedAnswer.value !== ''
})

const parsedOptions = computed(() => parseOptions(currentQuestion.value?.options))

const correctAnswerArray = computed(() => {
  return correctAnswer.value ? correctAnswer.value.split('').sort() : []
})

const correctPercent = computed(() => {
  return totalCount.value > 0 ? Math.round(correctCount.value / totalCount.value * 100) : 0
})

const wrongPercent = computed(() => {
  return totalCount.value > 0 ? Math.round((answeredCount.value - correctCount.value) / totalCount.value * 100) : 0
})

onMounted(async () => {
  await loadPractice()
})

onUnmounted(() => {
  clearTimeout(autoNextTimer.value)
})

async function loadPractice() {
  loading.value = true
  try {
    // 错题练习模式
    if (questionType.value === 'wrong') {
      const res = await startWrongPractice(bankId.value)
      if (res.code === 200) {
        questions.value = res.data.questions || []
        totalCount.value = res.data.totalCount || questions.value.length
        answeredCount.value = 0
        correctCount.value = 0
      }
      loading.value = false
      return
    }

    const res = await startPractice(bankId.value, questionType.value, roundNum.value)
    if (res.code === 200) {
      questions.value = res.data.questions || []
      const progress = res.data.progress
      if (progress) {
        totalCount.value = progress.totalCount
        answeredCount.value = progress.answeredCount
        correctCount.value = progress.correctCount
        accuracy.value = answeredCount.value > 0
          ? Math.round(correctCount.value / answeredCount.value * 100)
          : 0
        progressPercent.value = totalCount.value > 0
          ? Math.round(answeredCount.value / totalCount.value * 100)
          : 0
        if (progress.status === 'completed') {
          completed.value = true
        }
      }

      if (!questions.value.length && !completed.value) {
        // 检查是否完成了
        await checkComplete()
      }
    }
  } catch (e) {
    console.error('加载练习失败:', e)
  } finally {
    loading.value = false
  }
}

async function checkComplete() {
  try {
    const res = await checkRoundComplete(bankId.value, questionType.value, roundNum.value)
    if (res.code === 200 && res.data.completed) {
      completed.value = true
    }
  } catch (e) {
    console.error(e)
  }
}

async function submitCurrentAnswer() {
  if (!canSubmit.value) {
    ElMessage.warning('请选择一个答案')
    return
  }

  submitting.value = true
  try {
    const answer = effectiveType.value === 'multiple'
      ? selectedAnswers.value.sort().join('')
      : selectedAnswer.value

    const res = await submitAnswer({
      questionId: currentQuestion.value.id,
      bankId: bankId.value,
      questionType: questionType.value,
      selectedAnswer: answer,
      roundNum: roundNum.value
    })

    if (res.code === 200) {
      showResult.value = true
      isCurrentCorrect.value = res.data.isCorrect
      correctAnswer.value = res.data.correctAnswer
      correctAnswerText.value = res.data.correctAnswerText || ''
      wrongCount.value = res.data.wrongCount || 0

      if (res.data.isCorrect) {
        correctCount.value++
      }
      answeredCount.value++
      accuracy.value = Math.round(correctCount.value / answeredCount.value * 100)
      progressPercent.value = Math.round(answeredCount.value / totalCount.value * 100)

      // 回答正确后 1 秒自动下一题
      if (res.data.isCorrect) {
        clearTimeout(autoNextTimer.value)
        autoNextTimer.value = setTimeout(() => {
          nextQuestion()
        }, 1000)
      }
    }
  } catch (e) {
    console.error('提交答案失败:', e)
  } finally {
    submitting.value = false
  }
}

function nextQuestion() {
  if (hasMoreQuestions.value) {
    currentIndex.value++
    resetQuestionState()
  } else {
    completed.value = true
  }
}

async function removeFromWrongBook() {
  try {
    const res = await removeWrongQuestion(currentQuestion.value.id)
    if (res.code === 200) {
      wrongCount.value = 0
      ElMessage.success('已移出错题本')
    }
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

function resetQuestionState() {
  clearTimeout(autoNextTimer.value)
  autoNextTimer.value = null
  selectedAnswer.value = ''
  selectedAnswers.value = []
  showResult.value = false
  isCurrentCorrect.value = false
  correctAnswer.value = ''
  correctAnswerText.value = ''
  wrongCount.value = 0
}

function startNewRound() {
  roundNum.value++
  completed.value = false
  currentIndex.value = 0
  questions.value = []
  answeredCount.value = 0
  correctCount.value = 0
  accuracy.value = 0
  progressPercent.value = 0
  resetQuestionState()
  // 同步更新 URL，防止刷新后回退
  router.replace({
    query: { ...route.query, roundNum: roundNum.value }
  })
  loadPractice()
}

function goBack() {
  router.push('/')
}

function goHome() {
  router.push('/')
}
</script>

<style scoped>
.practice {
  min-height: 100vh;
  background: #f5f7fa;
}

.practice-header {
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
  max-width: 180px;
}

@media (max-width: 768px) {
  .practice-header {
    flex-wrap: wrap;
    height: auto !important;
    padding: 8px 12px !important;
    gap: 6px;
  }
  .header-left {
    min-width: 0;
    flex: 1;
  }
  .header-title {
    font-size: 14px;
    max-width: 120px;
  }
  .header-center {
    display: none;
  }
  .custom-progress {
    max-width: 100%;
  }
  .header-right {
    gap: 4px;
  }
  .header-right .el-tag {
    font-size: 12px;
    padding: 0 6px;
    height: 24px;
  }
}

.header-center {
  flex: 1;
  max-width: 400px;
  margin: 0 20px;
}

.custom-progress {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
}

.progress-bg {
  flex: 1;
  height: 16px;
  background: #e9ecef;
  border-radius: 8px;
  overflow: hidden;
  display: flex;
  position: relative;
}

.progress-correct-seg {
  height: 100%;
  background: linear-gradient(90deg, #67c23a, #85ce61);
  border-radius: 8px 0 0 8px;
  transition: width 0.4s ease;
  min-width: 0;
}

.progress-wrong-seg {
  height: 100%;
  background: linear-gradient(90deg, #f56c6c, #f89898);
  transition: width 0.4s ease;
  min-width: 0;
}

.progress-text {
  font-size: 12px;
  color: #606266;
  white-space: nowrap;
  font-weight: 500;
  min-width: 50px;
  text-align: right;
}

.header-right {
  display: flex;
  gap: 8px;
  min-width: 200px;
  justify-content: flex-end;
}

.practice-body {
  max-width: 800px;
  margin: 0 auto;
  padding: 24px 20px;
}

.loading-container {
  padding: 40px;
}

.complete-container {
  padding: 60px 0;
}

.question-area {
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.progress-info {
  display: flex;
  justify-content: space-between;
  margin-bottom: 16px;
  color: #606266;
  font-size: 14px;
}

.progress-correct {
  color: #67c23a;
}

.question-card {
  margin-bottom: 20px;
}

.question-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.question-number {
  font-size: 14px;
  color: #909399;
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

.option-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
  align-items: flex-start !important;
}

.option-item {
  padding: 12px 16px;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: flex-start;
  width: 100%;
}

.option-item {
  padding: 0;
}

.option-item :deep(.el-radio),
.option-item :deep(.el-checkbox) {
  display: flex !important;
  align-items: flex-start !important;
  width: 100%;
  padding: 12px 16px;
  box-sizing: border-box;
  height: 100%;
}

.option-item :deep(.el-checkbox__label),
.option-item :deep(.el-radio__label) {
  display: inline;
  text-align: left;
  white-space: normal;
  word-break: break-word;
  padding-left: 8px;
  line-height: 1.6;
  flex: 1;
}

.option-item:hover {
  border-color: #409eff;
  background: #f5f7fa;
}

.option-correct {
  border-color: #67c23a !important;
  background: #f0f9eb !important;
}

.option-wrong {
  border-color: #f56c6c !important;
  background: #fef0f0 !important;
}

.option-radio, .option-checkbox {
  display: flex;
  align-items: flex-start;
  justify-content: flex-start;
  width: 100%;
  height: auto;
  white-space: normal;
  text-align: left;
  margin-left: 0;
  padding-left: 0;
}

.option-radio :deep(.el-radio) {
  display: inline-flex;
  align-items: flex-start;
  margin-right: 0;
  white-space: normal;
  text-align: left;
}

.option-radio :deep(.el-radio__input) {
  flex-shrink: 0;
  margin-top: 2px;
}

.option-radio :deep(.el-radio__label),
.option-checkbox :deep(.el-checkbox__label) {
  display: inline;
  text-align: left;
  white-space: normal;
  word-break: break-word;
  padding-left: 8px;
  line-height: 1.6;
}

.option-checkbox :deep(.el-checkbox) {
  display: inline-flex;
  align-items: flex-start;
  margin-right: 0;
  white-space: normal;
  text-align: left;
}

.option-checkbox :deep(.el-checkbox__input) {
  flex-shrink: 0;
  margin-top: 2px;
}

.option-label {
  font-weight: 600;
  margin-right: 8px;
  color: #606266;
  flex-shrink: 0;
}

.option-text {
  color: #303133;
  text-align: left;
}

.result-area {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  border-radius: 8px;
  margin-top: 20px;
}

.result-area.correct {
  background: #f0f9eb;
  border: 1px solid #e1f3d8;
}

.result-area.wrong {
  background: #fef0f0;
  border: 1px solid #fde2e2;
}

.result-icon {
  font-size: 28px;
}

.result-text {
  flex: 1;
}

.result-text strong {
  font-size: 16px;
}

.correct-answer {
  color: #606266;
  font-size: 14px;
  margin-top: 4px;
}

.wrong-count-hint {
  color: #e6a23c;
  font-size: 13px;
  margin-top: 6px;
  padding: 4px 8px;
  background: #fdf6ec;
  border-radius: 4px;
  display: inline-block;
}

.action-buttons {
  text-align: center;
  padding: 12px 0;
}

.empty-container {
  padding: 80px 0;
}

/* 移动端响应式 */
@media (max-width: 768px) {
  .practice-body {
    padding: 16px 12px;
  }
  .question-stem {
    font-size: 15px;
    line-height: 1.7;
    padding: 4px 0;
  }
  .option-label {
    font-size: 14px;
  }
  .action-buttons .el-button {
    font-size: 14px;
    padding: 12px 20px;
  }
}

@media (max-width: 480px) {
  .practice-body {
    padding: 12px 10px;
  }
  .question-stem {
    font-size: 14px;
  }
  .action-buttons .el-button {
    width: 100%;
    margin-left: 0 !important;
    margin-top: 8px;
  }
}
</style>
