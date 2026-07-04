<template>
  <div class="wrong-book">
    <el-header class="page-header">
      <div class="header-left">
        <el-button text @click="$router.push('/')">
          <el-icon><ArrowLeft /></el-icon> 返回
        </el-button>
        <h3 class="header-title">❌ 错题本</h3>
      </div>
      <div class="header-right">
        <el-tag type="danger">共 {{ wrongStats.total }} 道错题</el-tag>
      </div>
    </el-header>

    <div class="page-body">
      <!-- 筛选 -->
      <el-card class="filter-card" shadow="never">
        <el-row :gutter="16">
          <el-col :xs="24" :sm="6">
            <el-select v-model="filterType" placeholder="选择题型" clearable class="filter-select">
              <el-option label="全部题型" value="" />
              <el-option label="单选题" value="single" />
              <el-option label="多选题" value="multiple" />
              <el-option label="判断题" value="judgment" />
            </el-select>
          </el-col>
          <el-col :xs="24" :sm="6">
            <el-select v-model="filterBank" placeholder="选择题库" clearable class="filter-select">
              <el-option label="全部题库" value="" />
              <el-option v-for="b in banks" :key="b.id" :label="b.name" :value="b.id" />
            </el-select>
          </el-col>
          <el-col :xs="24" :sm="8">
            <el-input
              v-model="keyword"
              placeholder="搜索题干关键词..."
              clearable
              class="filter-select"
              @keyup.enter="loadWrongQuestions"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
          </el-col>
          <el-col :xs="24" :sm="4">
            <el-button type="primary" @click="loadWrongQuestions" :icon="Search">筛选</el-button>
          </el-col>
        </el-row>
      </el-card>

      <!-- 刷错题入口 -->
      <div class="wrong-practice-bar">
        <el-button
          type="warning"
          size="large"
          :icon="EditPen"
          :disabled="displayCount === 0"
          @click="startWrongPractice"
        >
          🎯 刷错题
        </el-button>
        <el-button
          v-if="questions.length > 0 && !batchMode"
          size="large"
          plain
          @click="enterBatchMode"
        >
          ☑️ 批量选择
        </el-button>
        <span class="wrong-practice-tip" v-if="displayCount > 0">
          共 {{ displayCount }} 道错题
        </span>
      </div>

      <!-- 批量操作栏 -->
      <div v-if="batchMode" class="batch-bar">
        <el-checkbox
          v-model="selectAllAll"
          :indeterminate="isIndeterminate"
          @change="handleSelectAllChange"
          class="batch-select-all"
        >
          全选
        </el-checkbox>
        <span class="batch-info" v-if="selectedIds.length > 0">
          已选择 <strong>{{ selectedIds.length }}</strong> 题
        </span>
        <el-button
          type="danger"
          size="small"
          :loading="batchLoading"
          :disabled="selectedIds.length === 0"
          @click="batchRemove"
        >
          批量移出错题本
        </el-button>
        <el-button size="small" @click="exitBatchMode">退出批量</el-button>
      </div>

      <!-- 错题列表 -->
      <div v-if="loading" class="loading-container">
        <el-skeleton :rows="5" animated />
      </div>

      <div v-else-if="questions.length === 0" class="empty-container">
        <el-empty description="暂无错题，继续保持！🎉">
          <el-button type="primary" @click="$router.push('/')">去刷题</el-button>
        </el-empty>
      </div>

      <div v-else class="question-list">
        <div v-for="(q, index) in questions" :key="q.id" class="question-wrapper">
          <el-card
            class="wrong-question-card"
            :class="{ 'card-selected': selectedIds.includes(q.id) }"
            shadow="hover"
          >
            <div class="question-header">
              <el-checkbox
                v-if="batchMode"
                :model-value="selectedIds.includes(q.id)"
                class="question-checkbox"
                @click.stop="toggleSelect(q.id)"
              />
              <el-tag :type="getTypeTag(q.type)" size="small">{{ getTypeName(q.type) }}</el-tag>
              <span class="question-index">#{{ index + 1 }}</span>
            </div>
            <div class="question-stem">{{ q.stem }}</div>
            <div class="question-options" v-if="q.options && q.options.length">
              <div v-for="opt in q.options" :key="opt" class="option-text">{{ opt }}</div>
            </div>
            <div class="question-footer">
              <el-button type="primary" size="small" @click="showAnswer(q)">查看答案</el-button>
              <el-button size="small" @click="removeFromWrong(q)">移出错题本</el-button>
            </div>
            <div v-if="visibleAnswerId === q.id" class="answer-reveal">
              <el-alert
                :title="`正确答案：${getCorrectAnswerText(q)}`"
                type="success"
                :closable="false"
                show-icon
              />
            </div>
          </el-card>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Search, EditPen } from '@element-plus/icons-vue'
import { getWrongQuestions, getWrongStats, batchRemoveWrongQuestions } from '../api/wrong'
import { getBanks } from '../api/auth'
import { startWrongPractice as startWrongPracticeApi } from '../api/practice'
import { TYPE_NAMES, TYPE_TAGS } from '../utils/constants'
import { getAnswerText } from '../utils/helpers'

const router = useRouter()

const banks = ref([])
const questions = ref([])
const wrongStats = ref({ total: 0, single: 0, multiple: 0, judgment: 0 })
const loading = ref(true)
const filterType = ref('')
const filterBank = ref('')
const keyword = ref('')
const visibleAnswerId = ref(null)
const selectedIds = ref([])
const selectAll = ref(false)
const batchLoading = ref(false)
const isIndeterminate = ref(false)
const batchMode = ref(false)

// 监听选择变化，更新全选和不定状态
watch(selectedIds, (ids) => {
  const total = questions.value.length
  const selected = ids.length
  if (total === 0 || selected === 0) {
    selectAll.value = false
    isIndeterminate.value = false
  } else if (selected === total) {
    selectAll.value = true
    isIndeterminate.value = false
  } else {
    selectAll.value = false
    isIndeterminate.value = true
  }
}, { deep: true })

// 题库/题型筛选变化时自动刷新
watch([filterBank, filterType], () => {
  loadWrongQuestions()
})

const displayCount = computed(() => {
  // 有筛选条件时显示筛选后的数量，否则显示总错题数
  if (filterBank.value || filterType.value || keyword.value) {
    return questions.value.length
  }
  return wrongStats.value.total
})

const selectAllAll = computed({
  get: () => selectAll.value,
  set: (val) => { selectAll.value = val }
})

function handleSelectAllChange(val) {
  if (val) {
    selectedIds.value = questions.value.map(q => q.id)
  } else {
    selectedIds.value = []
  }
}

onMounted(async () => {
  await Promise.all([loadBanks(), loadWrongQuestions(), loadStats()])
})

async function loadBanks() {
  try {
    const res = await getBanks()
    if (res.code === 200) banks.value = res.data
  } catch (e) { console.error(e) }
}

async function loadWrongQuestions() {
  loading.value = true
  try {
    const res = await getWrongQuestions(
      filterBank.value || null,
      filterType.value || null,
      keyword.value || null
    )
    if (res.code === 200) questions.value = res.data
  } catch (e) { console.error(e) }
  finally { loading.value = false }
}

async function loadStats() {
  try {
    const res = await getWrongStats()
    if (res.code === 200) wrongStats.value = res.data
  } catch (e) { console.error(e) }
}

async function startWrongPractice() {
  // 使用第一个有错题的题库（可从当前筛选获取）
  const bankId = filterBank.value || null
  try {
    const res = await startWrongPracticeApi(bankId)
    if (res.code === 200) {
      router.push({
        path: '/practice',
        query: {
          bankId: res.data.bank?.id || bankId,
          bankName: res.data.bank?.name || '',
          questionType: 'wrong',
          roundNum: -1
        }
      })
    } else {
      ElMessage.warning(res.message || '暂时没有错题可练习')
    }
  } catch (e) {
    ElMessage.warning('暂时没有错题可练习')
  }
}

function enterBatchMode() {
  batchMode.value = true
  selectedIds.value = []
}

function exitBatchMode() {
  batchMode.value = false
  selectedIds.value = []
  selectAll.value = false
}

function toggleSelect(id) {
  const idx = selectedIds.value.indexOf(id)
  if (idx >= 0) {
    selectedIds.value.splice(idx, 1)
  } else {
    selectedIds.value.push(id)
  }
}

function showAnswer(q) {
  visibleAnswerId.value = visibleAnswerId.value === q.id ? null : q.id
}

async function removeFromWrong(q) {
  try {
    await batchRemoveWrongQuestions([q.id])
    questions.value = questions.value.filter(item => item.id !== q.id)
    selectedIds.value = selectedIds.value.filter(id => id !== q.id)
    wrongStats.value.total--
    if (q.type === 'single') wrongStats.value.single--
    else if (q.type === 'multiple') wrongStats.value.multiple--
    else if (q.type === 'judgment') wrongStats.value.judgment--
    ElMessage.success('已移出错题本')
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

async function batchRemove() {
  if (selectedIds.value.length === 0) return
  batchLoading.value = true
  try {
    const ids = selectedIds.value.slice()
    await batchRemoveWrongQuestions(ids)
    questions.value = questions.value.filter(q => !selectedIds.value.includes(q.id))
    exitBatchMode()
    // 刷新统计
    await loadStats()
    ElMessage.success(`已批量移出 ${ids.length} 道错题`)
  } catch (e) {
    ElMessage.error('批量操作失败')
  } finally {
    batchLoading.value = false
  }
}

function getTypeTag(type) {
  return TYPE_TAGS[type] || 'info'
}

function getTypeName(type) {
  return TYPE_NAMES[type] || type
}

function getCorrectAnswerText(q) {
  return getAnswerText(q)
}
</script>

<style scoped>
.wrong-book {
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
  max-width: 800px;
  margin: 0 auto;
  padding: 24px 20px;
}

.filter-card {
  margin-bottom: 20px;
}

.filter-select {
  width: 100%;
}

.filter-card .el-col {
  margin-bottom: 0;
}

.wrong-practice-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
  padding: 12px 16px;
  background: #fdf6ec;
  border-radius: 8px;
  border: 1px solid #faecd8;
}

.wrong-practice-tip {
  font-size: 13px;
  color: #909399;
}

/* 批量操作栏 */
.batch-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  background: #ecf5ff;
  border: 1px solid #d9ecff;
  border-radius: 8px;
  margin-bottom: 16px;
}

.batch-info {
  font-size: 14px;
  color: #303133;
}

.batch-info strong {
  color: #409eff;
  font-size: 16px;
}

.batch-select-all {
  margin-right: 4px;
}

/* 选中卡片高亮 */
.card-selected {
  border-color: #409eff !important;
  background: #f0f8ff !important;
}

.question-checkbox {
  margin-right: 4px;
}

@media (max-width: 768px) {
  .wrong-practice-bar {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
  .wrong-practice-bar .el-button {
    width: 100%;
  }
  .batch-bar {
    flex-wrap: wrap;
  }
  .filter-card .el-col {
    margin-bottom: 12px;
  }
  .filter-card .el-col:last-child {
    margin-bottom: 0;
  }
}

.loading-container {
  padding: 40px;
}

.empty-container {
  padding: 60px 0;
}

.question-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.wrong-question-card {
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.question-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.question-index {
  font-size: 12px;
  color: #909399;
}

.question-stem {
  font-size: 15px;
  line-height: 1.8;
  color: #303133;
  margin-bottom: 12px;
}

.question-options {
  margin-bottom: 12px;
}

.option-text {
  font-size: 14px;
  color: #606266;
  line-height: 1.8;
  padding: 2px 0;
}

.question-footer {
  display: flex;
  gap: 8px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.answer-reveal {
  margin-top: 12px;
}

@media (max-width: 768px) {
  .page-body {
    padding: 16px 12px;
  }
  .question-stem {
    font-size: 14px;
  }
  .question-footer {
    flex-direction: column;
  }
  .question-footer .el-button {
    width: 100%;
  }
}
</style>
