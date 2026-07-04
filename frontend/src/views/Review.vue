<template>
  <div class="review-page">
    <el-header class="review-header">
      <div class="header-left">
        <el-button text @click="$router.push('/')">
          <el-icon><ArrowLeft /></el-icon> 返回
        </el-button>
        <h3 class="header-title">📖 背题模式 · {{ bankName }}</h3>
      </div>
      <div class="header-right">
        <el-tag type="info" v-if="currentQuestion">
          {{ currentIndex + 1 }}/{{ filteredQuestions.length }}
        </el-tag>
      </div>
    </el-header>

    <div class="review-body">
      <!-- 信息栏 -->
      <el-card class="filter-card" shadow="never">
        <el-row :gutter="12" align="middle">
          <el-col :xs="24" :sm="3">
            <el-tag :type="typeTagAll" size="large">{{ typeNameAll }}</el-tag>
          </el-col>
          <el-col :xs="24" :sm="8">
            <el-input v-model="keyword" placeholder="搜索题干或选项..." clearable @keyup.enter="loadQuestions">
              <template #prefix><el-icon><Search /></el-icon></template>
            </el-input>
          </el-col>
          <el-col :xs="12" :sm="3">
            <el-button type="primary" @click="loadQuestions" :icon="Search" style="width:100%">搜索</el-button>
          </el-col>
          <el-col :xs="12" :sm="3">
          </el-col>
          <el-col :xs="12" :sm="3">
            <el-input-number placeholder="题号" v-if="filteredQuestions.length > 0" v-model="jumpNum" :min="1" :max="filteredQuestions.length" style="width:100%" :controls="false" />
          </el-col>
          <el-col :xs="12" :sm="2">
            <el-button type="primary" @click="handleJump(jumpNum)" style="width:100%">跳转</el-button>
          </el-col>
        </el-row>
      </el-card>

      <!-- 加载中 -->
      <div v-if="loading" class="loading-container">
        <el-skeleton :rows="6" animated />
      </div>

      <!-- 空状态 -->
      <div v-else-if="filteredQuestions.length === 0" class="empty-container">
        <el-empty description="暂无题目" />
      </div>

      <!-- 题目展示 -->
      <div v-else class="review-main">

        <el-card class="question-card" shadow="hover">
          <!-- 题目标题 -->
          <div class="question-title-bar">
            <el-tag :type="typeTag" size="small">{{ typeName }}</el-tag>
            <span class="question-number">第 {{ currentIndex + 1 }}/{{ filteredQuestions.length }} 题</span>
            <el-button size="small" :type="showAnswer ? 'warning' : 'default'" @click="showAnswer = !showAnswer">
              {{ showAnswer ? '隐藏答案' : '显示答案' }}
            </el-button>
          </div>

          <!-- 题干 -->
          <div class="question-stem">{{ currentQuestion.stem }}</div>

          <!-- 选项 -->
          <div class="options-area" v-if="currentQuestion.options && currentQuestion.options.length">
            <div v-for="opt in currentQuestion.options" :key="opt" class="option-item"
              :class="{ 'option-correct': showAnswer && isCorrectOption(opt) }">
              <span class="option-text">{{ opt }}</span>
            </div>
          </div>

          <!-- 答案 -->
          <div v-if="showAnswer" class="answer-area">
            <el-alert :title="'正确答案：' + getAnswerText(currentQuestion)" type="success" :closable="false" show-icon />
          </div>
        </el-card>

        <!-- 导航按钮 -->
        <div class="nav-buttons">
          <el-button @click="prevQuestion" :disabled="currentIndex === 0">
            <el-icon><ArrowLeft /></el-icon> 上一题
          </el-button>
          <el-button @click="nextQuestion" :disabled="currentIndex >= filteredQuestions.length - 1">
            下一题 <el-icon><ArrowRight /></el-icon>
          </el-button>
        </div>

      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, ArrowRight, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getReviewQuestions } from '../api/review'
import { TYPE_NAMES, TYPE_TAGS } from '../utils/constants'
import { getAnswerText } from '../utils/helpers'

const route = useRoute()
const router = useRouter()

const bankId = ref(parseInt(route.query.bankId))
const bankName = ref(route.query.bankName || '')
const filterType = ref(route.query.type || '')

const filteredQuestions = ref([])
const loading = ref(false)
const currentIndex = ref(0)
const showAnswer = ref(false)
const keyword = ref('')
const jumpNum = ref()

const typeTagAll = computed(() => TYPE_TAGS[filterType.value] || 'info')

const typeNameAll = computed(() => TYPE_NAMES[filterType.value] || filterType.value)

const currentQuestion = computed(() => {
  return filteredQuestions.value[currentIndex.value] || {}
})

const typeTag = computed(() => TYPE_TAGS[currentQuestion.value.type] || 'info')

const typeName = computed(() => TYPE_NAMES[currentQuestion.value.type] || '')

onMounted(async () => {
  if (bankId.value) {
    await loadQuestions()
  }
})

watch(currentIndex, () => {
  showAnswer.value = false
  jumpNum.value = currentIndex.value + 1
})

async function loadQuestions() {
  if (!bankId.value) return
  loading.value = true
  try {
    const res = await getReviewQuestions(
      bankId.value,
      filterType.value || '',
      keyword.value || ''
    )
    if (res.code === 200) {
      filteredQuestions.value = res.data.questions || []
      currentIndex.value = 0
    }
  } catch (e) {
    ElMessage.error('加载题目失败')
  } finally {
    loading.value = false
  }
}

function prevQuestion() {
  if (currentIndex.value > 0) currentIndex.value--
}

function nextQuestion() {
  if (currentIndex.value < filteredQuestions.value.length - 1) currentIndex.value++
}

function handleJump(val) {
  if (val >= 1 && val <= filteredQuestions.value.length) {
    currentIndex.value = val - 1
  }
}



function isCorrectOption(optText) {
  const answer = currentQuestion.value.answer || ''
  const label = optText.match(/^([A-Z])\./)?.[1]
  if (!label) return false
  return answer.includes(label)
}


</script>

<style scoped>
.review-page {
  min-height: 100vh;
  background: #f5f7fa;
}

.review-header {
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
}

.header-title {
  margin: 0;
  font-size: 18px;
  color: #303133;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.review-body {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px 16px 40px;
}

.filter-card {
  margin-bottom: 16px;
}

.filter-stat {
  margin-top: 8px;
  font-size: 13px;
  color: #909399;
}

.loading-container,
.empty-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 300px;
}

.review-main {
  max-width: 800px;
  margin: 0 auto;
}

.progress-bar-area {
  margin-bottom: 16px;
}

.question-card {
  margin-bottom: 16px;
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

.question-stem {
  font-size: 16px;
  line-height: 1.8;
  color: #303133;
  margin-bottom: 24px;
}

.options-area {
  margin-bottom: 16px;
}

.option-item {
  padding: 12px 16px;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  margin-bottom: 8px;
  transition: all 0.2s;
  background: #fafafa;
}

.option-item.option-correct {
  border-color: #67c23a;
  background: #f0f9eb;
}

.option-item .option-text {
  font-size: 15px;
  line-height: 1.6;
  color: #303133;
}

.answer-area {
  margin-bottom: 8px;
}

.nav-buttons {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-bottom: 20px;
}

.quick-nav {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  justify-content: center;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.qdot {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 1px solid #dcdfe6;
  font-size: 12px;
  color: #606266;
  cursor: pointer;
  transition: all 0.2s;
  user-select: none;
}

.qdot:hover {
  border-color: #409eff;
  color: #409eff;
}

.qdot-current {
  background: #409eff;
  border-color: #409eff;
  color: #fff;
}

@media (max-width: 768px) {
  .review-body {
    padding: 12px 10px 40px;
  }
  .question-stem {
    font-size: 15px;
  }
}
</style>
