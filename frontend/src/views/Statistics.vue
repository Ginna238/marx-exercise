<template>
  <div class="statistics-page">
    <el-header class="page-header">
      <div class="header-left">
        <el-button text @click="$router.push('/')">
          <el-icon><ArrowLeft /></el-icon> 返回
        </el-button>
        <h3 class="header-title">📊 统计信息</h3>
      </div>
    </el-header>

    <div class="page-body">
      <!-- 概览卡片 -->
      <el-row :gutter="16" class="stat-row">
        <el-col :xs="12" :sm="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-value">{{ stats.totalQuestions }}</div>
            <div class="stat-label">总题目数</div>
          </el-card>
        </el-col>
        <el-col :xs="12" :sm="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-value primary">{{ stats.totalAnswered }}</div>
            <div class="stat-label">已答题数</div>
          </el-card>
        </el-col>
        <el-col :xs="12" :sm="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-value success">{{ stats.totalCorrect }}</div>
            <div class="stat-label">正确数</div>
          </el-card>
        </el-col>
        <el-col :xs="12" :sm="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-value" :class="accuracyColor">{{ stats.accuracy }}%</div>
            <div class="stat-label">正确率</div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 进度概览 -->
      <el-card class="section-card" shadow="never">
        <template #header>
          <span>📈 学习进度</span>
        </template>
        <div v-if="loading" class="loading-container">
          <el-skeleton :rows="4" animated />
        </div>
        <div v-else-if="progressList.length === 0" class="empty-tip">
          暂无学习记录，去刷题吧！
        </div>
        <div v-else class="progress-list">
          <div v-for="p in progressList" :key="`${p.bankId}-${p.questionType}-${p.roundNum}`" class="progress-item">
            <div class="progress-item-header">
              <div class="progress-item-left">
                <el-tag size="small">{{ p.bankName }}</el-tag>
                <span class="progress-type">{{ p.typeName }}</span>
                <el-tag size="small" type="info">第{{ p.roundNum }}轮</el-tag>
              </div>
              <div class="progress-item-right">
                <span class="progress-accuracy" :class="getAccuracyClass(p.accuracy)">{{ p.accuracy }}%</span>
                <el-tag v-if="p.status === 'completed'" type="success" size="small">已完成</el-tag>
                <el-tag v-else type="warning" size="small">进行中</el-tag>
              </div>
            </div>
            <el-progress
              :percentage="p.progressPercent"
              :status="p.status === 'completed' ? 'success' : ''"
              :stroke-width="12"
              class="progress-bar-item"
            >
              <span>{{ p.answeredCount }}/{{ p.totalCount }}</span>
            </el-progress>
          </div>
        </div>
      </el-card>

      <!-- 题型统计 -->
      <el-card class="section-card" shadow="never">
        <template #header>
          <span>📊 题型统计</span>
        </template>
        <el-table :data="stats.typeStats" stripe style="width: 100%">
          <el-table-column prop="typeName" label="题型" />
          <el-table-column prop="total" label="总题数" align="center" />
          <el-table-column prop="answered" label="已答" align="center" />
          <el-table-column prop="correct" label="正确" align="center" />
          <el-table-column label="正确率" align="center">
            <template #default="{ row }">
              <el-progress
                :percentage="row.accuracy"
                :status="row.accuracy >= 70 ? 'success' : row.accuracy >= 40 ? 'warning' : 'exception'"
                :stroke-width="16"
              />
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <!-- 每日统计 -->
      <el-card class="section-card" shadow="never">
        <template #header>
          <span>📅 近7天答题趋势</span>
        </template>
        <div v-if="stats.dailyStats && stats.dailyStats.length" class="daily-chart">
          <div class="daily-bars">
            <div v-for="day in stats.dailyStats" :key="day.date" class="daily-bar-item">
              <div class="bar-container">
                <div
                  class="bar bar-correct"
                  :style="{ height: getBarHeight(day.correct) + '%' }"
                  :title="`正确: ${day.correct}`"
                />
                <div
                  class="bar bar-wrong"
                  :style="{ height: getBarHeight(day.count - day.correct) + '%' }"
                  :title="`错误: ${day.count - day.correct}`"
                />
              </div>
              <div class="bar-label">{{ day.date }}</div>
              <div class="bar-count">{{ day.count }}题</div>
            </div>
          </div>
        </div>
        <div v-else class="empty-tip">暂无数据</div>
      </el-card>

      <!-- 错题统计 -->
      <el-card class="section-card" shadow="never">
        <template #header>
          <span>❌ 错题统计</span>
        </template>
        <el-descriptions :column="3" border>
          <el-descriptions-item label="总错题数">
            <el-tag type="danger" size="large">{{ wrongStats.total }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="单选题">
            <el-tag>{{ wrongStats.single }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="多选题">
            <el-tag type="success">{{ wrongStats.multiple }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="判断题">
            <el-tag type="warning">{{ wrongStats.judgment }}</el-tag>
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 模拟考试统计 -->
      <el-card class="section-card" shadow="never">
        <template #header>
          <span>📝 模拟考试记录</span>
        </template>
        <div v-if="examLoading" class="loading-container">
          <el-skeleton :rows="3" animated />
        </div>
        <div v-else-if="examStats.totalExams === 0" class="empty-tip">
          暂无模拟考试记录
        </div>
        <div v-else>
          <el-descriptions :column="3" border style="margin-bottom:16px">
            <el-descriptions-item label="考试次数">
              <el-tag type="danger" size="large">{{ examStats.totalExams }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="平均分">
              <el-tag :type="examStats.avgScore >= 70 ? 'success' : examStats.avgScore >= 60 ? 'warning' : 'danger'">
                {{ examStats.avgScore }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="最高分">
              <el-tag type="success">{{ examStats.bestScore }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="总用时">
              <el-tag>{{ examStats.totalDurationText }}</el-tag>
            </el-descriptions-item>
          </el-descriptions>

          <!-- 各题库考试统计 -->
          <div v-if="examStats.bankStats && examStats.bankStats.length" class="bank-exam-stats">
            <h4 class="bank-exam-title">各题库考试情况</h4>
            <div v-for="bs in examStats.bankStats" :key="bs.bankId" class="bank-exam-item">
              <div class="bank-exam-header">
                <span class="bank-exam-name">{{ bs.bankName }}</span>
                <span class="bank-exam-count">{{ bs.examCount }} 次</span>
                <span class="bank-exam-avg">平均 {{ bs.avgScore }} 分</span>
              </div>
            </div>
          </div>

          <!-- 考试记录列表 -->
          <div v-if="examRecords.length" class="exam-records-list">
            <h4 class="bank-exam-title">最近考试记录</h4>
            <div v-for="r in examRecords.slice(0, 10)" :key="r.id" class="exam-record-item" @click="viewExamResult(r)">
              <div class="exam-record-header">
                <span class="exam-record-bank">{{ r.bankName }}</span>
                <el-tag :type="r.score >= 70 ? 'success' : r.score >= 60 ? 'warning' : 'danger'" size="small">
                  {{ r.score }}分
                </el-tag>
              </div>
              <div class="exam-record-meta">
                <span>{{ r.durationText }}</span>
                <span>{{ formatExamTime(r.completedAt) }}</span>
              </div>
            </div>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getStatistics, getProgress } from '../api/statistics'
import { getWrongStats } from '../api/wrong'
import { getExamRecords, getExamStats } from '../api/exam'

const router = useRouter()

const loading = ref(true)
const stats = ref({
  totalQuestions: 0,
  totalAnswered: 0,
  totalCorrect: 0,
  accuracy: 0,
  typeStats: [],
  dailyStats: []
})
const progressList = ref([])
const wrongStats = ref({ total: 0, single: 0, multiple: 0, judgment: 0 })
const examRecords = ref([])
const examStats = ref({ totalExams: 0, avgScore: 0, bestScore: 0, totalDurationText: '', bankStats: [] })
const examLoading = ref(true)

const accuracyColor = computed(() => {
  const a = stats.value.accuracy
  if (a >= 70) return 'success'
  if (a >= 40) return 'warning'
  return 'danger'
})

onMounted(async () => {
  loading.value = true
  try {
    const [statsRes, progressRes, wrongRes] = await Promise.all([
      getStatistics(),
      getProgress(),
      getWrongStats()
    ])
    if (statsRes.code === 200) stats.value = statsRes.data
    if (progressRes.code === 200) progressList.value = progressRes.data
    if (wrongRes.code === 200) wrongStats.value = wrongRes.data
  } catch (e) {
    console.error('加载统计失败:', e)
  } finally {
    loading.value = false
  }

  // 加载考试数据
  examLoading.value = true
  try {
    const [recordsRes, statsRes2] = await Promise.all([
      getExamRecords(),
      getExamStats()
    ])
    if (recordsRes.code === 200) examRecords.value = recordsRes.data
    if (statsRes2.code === 200) examStats.value = statsRes2.data
  } catch (e) {
    console.error('加载考试数据失败:', e)
  } finally {
    examLoading.value = false
  }
})

function getBarHeight(count) {
  const max = Math.max(...stats.value.dailyStats.map(d => d.count), 1)
  return Math.max((count / max) * 100, 2)
}

function viewExamResult(record) {
  ElMessage.info('考试详情可在模拟考试页面查看')
}

function formatExamTime(time) {
  if (!time) return ''
  const d = new Date(time)
  const pad = n => n.toString().padStart(2, '0')
  return `${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function getAccuracyClass(accuracy) {
  if (accuracy >= 70) return 'success'
  if (accuracy >= 40) return 'warning'
  return 'danger'
}
</script>

<style scoped>
.statistics-page {
  min-height: 100vh;
  background: #f5f7fa;
}

.page-header {
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
  font-size: 18px;
  font-weight: 600;
}

.page-body {
  max-width: 1000px;
  margin: 0 auto;
  padding: 24px 20px;
}

.stat-row {
  margin-bottom: 20px;
}

.stat-card {
  text-align: center;
  padding: 12px 0;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: #303133;
}

.stat-value.primary { color: #409eff; }
.stat-value.success { color: #67c23a; }
.stat-value.warning { color: #e6a23c; }
.stat-value.danger { color: #f56c6c; }

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 4px;
}

.section-card {
  margin-bottom: 20px;
}

.loading-container {
  padding: 20px;
}

.empty-tip {
  text-align: center;
  color: #909399;
  padding: 24px;
}

.progress-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.progress-item {
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.progress-item:last-child {
  border-bottom: none;
}

.progress-item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.progress-item-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.progress-type {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.progress-item-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.progress-accuracy {
  font-weight: 600;
  font-size: 14px;
}

.progress-accuracy.success { color: #67c23a; }
.progress-accuracy.warning { color: #e6a23c; }
.progress-accuracy.danger { color: #f56c6c; }

.progress-bar-item {
  :deep(.el-progress__text) {
    font-size: 12px;
  }
}

.daily-chart {
  padding: 16px 0;
}

.daily-bars {
  display: flex;
  justify-content: space-around;
  align-items: flex-end;
  height: 180px;
  gap: 12px;
}

.daily-bar-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.bar-container {
  display: flex;
  flex-direction: column-reverse;
  align-items: center;
  height: 150px;
  width: 100%;
  gap: 2px;
}

.bar {
  width: 60%;
  border-radius: 4px 4px 0 0;
  transition: height 0.5s ease;
  min-height: 4px;
}

.bar-correct {
  background: #67c23a;
}

.bar-wrong {
  background: #f56c6c;
}

.bar-label {
  font-size: 12px;
  color: #909399;
}

.bar-count {
  font-size: 11px;
  color: #909399;
}

/* 模拟考试统计 */
.bank-exam-title {
  font-size: 14px;
  color: #303133;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #f0f0f0;
}

.bank-exam-item {
  display: flex;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid #f5f5f5;
}

.bank-exam-item:last-child {
  border-bottom: none;
}

.bank-exam-header {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
}

.bank-exam-name {
  font-weight: 500;
  color: #303133;
  flex: 1;
}

.bank-exam-count {
  font-size: 13px;
  color: #606266;
}

.bank-exam-avg {
  font-size: 14px;
  font-weight: 600;
  color: #409eff;
}

.exam-records-list {
  margin-top: 8px;
}

.exam-record-item {
  padding: 12px;
  border: 1px solid #e9ecef;
  border-radius: 8px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.exam-record-item:hover {
  border-color: #409eff;
  background: #f5f7fa;
}

.exam-record-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.exam-record-bank {
  font-weight: 500;
  color: #303133;
  font-size: 14px;
}

.exam-record-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #909399;
}
</style>
