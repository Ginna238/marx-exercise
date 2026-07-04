<template>
  <div class="admin-page">
    <el-header class="admin-header">
      <div class="header-left">
        <el-button text @click="$router.push('/')">
          <el-icon><ArrowLeft /></el-icon> 返回首页
        </el-button>
        <h3 class="header-title">⚙️ 后台管理</h3>
      </div>
      <div class="header-right">
        <el-tag type="danger">超级管理员</el-tag>
        <span class="admin-name">{{ adminInfo?.nickname }}</span>
      </div>
    </el-header>

    <div class="admin-body">
      <!-- 概览卡片 -->
      <el-row :gutter="16" class="stat-row">
        <el-col :span="6" v-for="s in stats" :key="s.key">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-icon" :style="{ background: s.color }">{{ s.icon }}</div>
            <div class="stat-info">
              <div class="stat-value">{{ s.value }}</div>
              <div class="stat-label">{{ s.label }}</div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- Tab 切换 -->
      <el-card shadow="never">
        <el-tabs v-model="activeTab">
          <el-tab-pane label="👥 用户管理" name="users">
            <el-table :data="users" stripe style="width:100%" v-loading="loading.users">
              <el-table-column prop="id" label="ID" width="60" />
              <el-table-column prop="username" label="用户名" />
              <el-table-column prop="nickname" label="昵称" />
              <el-table-column prop="role" label="角色" width="100">
                <template #default="{ row }">
                  <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'info'" size="small">{{ row.role }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="createdAt" label="注册时间" width="180" />
              <el-table-column label="操作" width="220">
                <template #default="{ row }">
                  <el-button size="small" @click="handleResetPwd(row)">重置密码</el-button>
                  <el-popconfirm title="确定删除此用户？" @confirm="handleDeleteUser(row)">
                    <template #reference>
                      <el-button size="small" type="danger" :disabled="row.role === 'ADMIN'">删除</el-button>
                    </template>
                  </el-popconfirm>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane label="📚 题库管理" name="banks">
            <div style="margin-bottom:12px">
              <el-button type="primary" :loading="loading.reload" @click="handleReloadBanks">🔄 重新加载题库</el-button>
            </div>
            <el-table :data="banks" stripe style="width:100%">
              <el-table-column prop="id" label="ID" width="60" />
              <el-table-column prop="name" label="题库名称" />
              <el-table-column prop="questionCount" label="题目数" width="100" />
              <el-table-column prop="fileName" label="文件名" />
              <el-table-column prop="createdAt" label="创建时间" width="180" />
            </el-table>
          </el-tab-pane>

          <el-tab-pane label="📝 题目列表" name="questions">
            <el-row :gutter="12" style="margin-bottom:12px">
              <el-col :span="6">
                <el-select v-model="qBankId" placeholder="选择题库" clearable style="width:100%">
                  <el-option v-for="b in banks" :key="b.id" :label="b.name" :value="b.id" />
                </el-select>
              </el-col>
              <el-col :span="6">
                <el-select v-model="qType" placeholder="选择题型" clearable style="width:100%">
                  <el-option label="单选题" value="single" />
                  <el-option label="多选题" value="multiple" />
                  <el-option label="判断题" value="judgment" />
                </el-select>
              </el-col>
              <el-col :span="6">
                <el-button type="primary" @click="loadQuestions" :icon="Search">查询</el-button>
              </el-col>
            </el-row>
            <el-table :data="questions" stripe style="width:100%" max-height="500" v-loading="loading.questions">
              <el-table-column prop="id" label="ID" width="60" />
              <el-table-column prop="type" label="题型" width="80">
                <template #default="{ row }">
                  <el-tag :type="typeTag(row.type)" size="small">{{ typeName(row.type) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="stem" label="题干" min-width="300" show-overflow-tooltip />
              <el-table-column prop="answer" label="答案" width="100" />
              <el-table-column prop="bankId" label="题库ID" width="70" />
            </el-table>
          </el-tab-pane>

          <el-tab-pane label="📊 刷题情况" name="userStats">
            <el-table :data="userStats" stripe style="width:100%" v-loading="loading.userStats" :default-sort="{ prop: 'totalAnswered', order: 'descending' }">
              <el-table-column prop="username" label="用户" width="80" />
              <el-table-column prop="totalAnswered" label="答题/正确" width="135" sortable>
                <template #default="{ row }">
                  {{ row.totalAnswered }}/{{ row.totalCorrect }}
                </template>
              </el-table-column>
              <el-table-column prop="accuracy" label="正确率" width="95" sortable>
                <template #default="{ row }">
                  <el-tag :type="row.accuracy &gt;= 70 ? 'success' : row.accuracy &gt;= 40 ? 'warning' : 'danger'" size="small">{{ row.accuracy }}%</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="wrongCount" label="错题" width="85" sortable />
              <el-table-column prop="examCount" label="考次" width="85" sortable />
              <el-table-column label="操作" width="55">
                <template #default="{ row }">
                  <el-button size="small" link type="primary" @click="viewUserDetail(row)">详情</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane label="🔄 题库更新" name="update">
            <div style="margin-bottom:16px">
              <el-button type="success" :loading="loading.scan" @click="scanGitHub">🔍 扫描 GitHub 新题库</el-button>
              <el-button type="warning" :loading="loading.syncAll" @click="handleSyncAll" style="margin-left:12px">
                📥 同步全部新题库
              </el-button>
            </div>
            <el-alert
              v-if="syncMessage"
              :title="syncMessage"
              :type="syncType"
              :closable="true"
              show-icon
              style="margin-bottom:12px"
              @close="syncMessage = ''"
            />
            <el-table :data="githubBanks" stripe style="width:100%" v-loading="loading.scan">
              <el-table-column prop="dir" label="目录" width="140" />
              <el-table-column prop="name" label="文件名" />
              <el-table-column label="状态" width="100">
                <template #default="{ row }">
                  <el-tag v-if="isImported(row)" type="info" size="small">已导入</el-tag>
                  <el-tag v-else type="success" size="small">新题库</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="120">
                <template #default="{ row }">
                  <el-button
                    size="small" type="primary"
                    :disabled="isImported(row)"
                    :loading="row._importing"
                    @click="handleImport(row)"
                  >
                    {{ isImported(row) ? '已导入' : '导入' }}
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </el-card>
    </div>
  </div>

  <!-- 用户详情弹窗 -->
  <el-dialog v-model="detailVisible" :title="'📊 ' + (detailUser?.nickname || detailUser?.username) + ' 的刷题详情'" width="600px" destroy-on-close>
    <template v-if="detailUser">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="用户名" :span="1">{{ detailUser.username }}</el-descriptions-item>
        <el-descriptions-item label="昵称" :span="1">{{ detailUser.nickname }}</el-descriptions-item>
        <el-descriptions-item label="答题总数" :span="1">
          <el-tag type="primary">{{ detailUser.totalAnswered }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="正确总数" :span="1">
          <el-tag type="success">{{ detailUser.totalCorrect }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="错题数" :span="1">
          <el-tag type="danger">{{ detailUser.wrongCount }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="考试次数" :span="1">
          <el-tag type="warning">{{ detailUser.examCount }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="总正确率" :span="2">
          <el-tag :type="detailUser.accuracy &gt;= 70 ? 'success' : detailUser.accuracy &gt;= 40 ? 'warning' : 'danger'" size="default">{{ detailUser.accuracy }}%</el-tag>
        </el-descriptions-item>
      </el-descriptions>

      <el-divider />

      <h4 style="margin:0 0 12px 0">📝 模拟考试</h4>
      <el-descriptions :column="3" border size="small">
        <el-descriptions-item label="考试次数">{{ detailUser.examCount }}</el-descriptions-item>
        <el-descriptions-item label="平均分">{{ detailUser.examAvgScore }}</el-descriptions-item>
        <el-descriptions-item label="最高分">{{ detailUser.examBestScore }}</el-descriptions-item>
      </el-descriptions>

      <el-divider />

      <h4 style="margin:0 0 12px 0">📖 各题型统计</h4>
      <el-table :data="typeStats" stripe size="small" style="width:100%">
        <el-table-column prop="name" label="题型" width="80" />
        <el-table-column prop="answered" label="答题数" width="90" />
        <el-table-column prop="correct" label="正确数" width="90" />
        <el-table-column prop="accuracy" label="正确率">
          <template #default="{ row }">
            <el-tag :type="row.accuracy >= 70 ? 'success' : row.accuracy >= 40 ? 'warning' : 'danger'" size="small">{{ row.accuracy }}%</el-tag>
          </template>
        </el-table-column>
      </el-table>

      <el-divider />

      <h4 style="margin:0 0 12px 0">⚖️ 刷题评估</h4>
      <el-alert :title="assessmentText" :type="assessmentType" show-icon :closable="false" />
    </template>
  </el-dialog>
</template>

<script setup>

import { ref, reactive, computed, onMounted } from 'vue'
import { ArrowLeft, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getAdminInfo, getDashboard, getUsers, resetPassword, deleteUser,
         getAdminBanks, reloadBanks, getAdminQuestions,
         listGitHubBanks, importFromGitHub, syncAllGitHub,
         getUserStats as fetchUserStats } from '../../api/admin'
import { useUserStore } from '../../stores/user'
import { TYPE_NAMES, TYPE_TAGS } from '../../utils/constants'

const userStore = useUserStore()
const activeTab = ref('users')
const adminInfo = ref(null)

const stats = reactive([])
const users = ref([])
const banks = ref([])
const questions = ref([])
const userStats = ref([])
const detailVisible = ref(false)
const detailUser = ref(null)

const typeStats = computed(() => {
  const u = detailUser.value
  if (!u) return []
  return [
    { name: '单选题', answered: u.singleAnswered, correct: u.singleCorrect, accuracy: u.singleAccuracy },
    { name: '多选题', answered: u.multipleAnswered, correct: u.multipleCorrect, accuracy: u.multipleAccuracy },
    { name: '判断题', answered: u.judgmentAnswered, correct: u.judgmentCorrect, accuracy: u.judgmentAccuracy },
  ]
})

const assessmentText = computed(() => {
  const u = detailUser.value
  if (!u) return ''
  if (u.totalAnswered === 0) return '该用户还没有开始刷题哦～ 📚'
  const parts = []
  if (u.accuracy >= 70) parts.push('🎉 总体表现优秀，继续保持！')
  else if (u.accuracy >= 40) parts.push('💪 还有提升空间，多多练习！')
  else parts.push('😅 正确率偏低，建议先复习知识点再做题。')
  if (u.wrongCount > 0) parts.push(`错题本中有 ${u.wrongCount} 道错题，建议定期复习。`)
  if (u.examCount > 0) parts.push(`已参加 ${u.examCount} 次模拟考试，平均分 ${u.examAvgScore}，最高分 ${u.examBestScore}。`)
  else parts.push('还没有参加过模拟考试，可以尝试一下。')
  return parts.join(' ')
})

const assessmentType = computed(() => {
  const u = detailUser.value
  if (!u || u.totalAnswered === 0) return 'info'
  if (u.accuracy >= 70) return 'success'
  if (u.accuracy >= 40) return 'warning'
  return 'error'
})
const qBankId = ref(null)
const qType = ref('')

const loading = reactive({ users: false, questions: false, reload: false, scan: false, syncAll: false, userStats: false })
const githubBanks = ref([])
const importedNames = ref(new Set())
const syncMessage = ref('')
const syncType = ref('success')

onMounted(async () => {
  try {
    const [infoRes, dashRes] = await Promise.all([
      getAdminInfo(),
      getDashboard()
    ])
    if (infoRes.code === 200) adminInfo.value = infoRes.data
    if (dashRes.code === 200) {
      const d = dashRes.data
      stats.length = 0
      stats.push(
        { key: 'users', icon: '👥', label: '用户数', value: d.userCount, color: '#409eff' },
        { key: 'banks', icon: '📚', label: '题库数', value: d.bankCount, color: '#67c23a' },
        { key: 'questions', icon: '📝', label: '题目总数', value: d.questionCount, color: '#e6a23c' },
        { key: 'answers', icon: '📊', label: '答题记录', value: d.answerCount, color: '#f56c6c' },
      )
    }
  } catch (e) {
    ElMessage.error('加载失败，可能无管理员权限')
  }
  loadUsers()
  loadBanks()
  loadUserStats()
})

async function loadUsers() {
  loading.users = true
  try {
    const res = await getUsers()
    if (res.code === 200) users.value = res.data
  } finally { loading.users = false }
}

async function loadBanks() {
  try {
    const res = await getAdminBanks()
    if (res.code === 200) banks.value = res.data
  } catch (e) {}
}

async function loadUserStats() {
  loading.userStats = true
  try {
    const res = await fetchUserStats()
    if (res.code === 200) userStats.value = res.data
  } catch (e) {}
  finally { loading.userStats = false }
}

async function loadQuestions() {
  loading.questions = true
  try {
    const res = await getAdminQuestions(qBankId.value, qType.value || null)
    if (res.code === 200) questions.value = res.data
  } finally { loading.questions = false }
}

async function handleResetPwd(row) {
  try {
    await resetPassword(row.id)
    ElMessage.success(`用户 ${row.username} 密码已重置为 123456`)
  } catch (e) {}
}

async function handleDeleteUser(row) {
  try {
    await deleteUser(row.id)
    users.value = users.value.filter(u => u.id !== row.id)
    ElMessage.success(`用户 ${row.username} 已删除`)
  } catch (e) {}
}

async function handleReloadBanks() {
  loading.reload = true
  try {
    await reloadBanks()
    ElMessage.success('题库已重新加载')
    await loadBanks()
  } finally { loading.reload = false }
}

function typeTag(type) {
  return TYPE_TAGS[type] || 'info'
}
function typeName(type) {
  return TYPE_NAMES[type] || type
}

function viewUserDetail(row) {
  detailUser.value = row
  detailVisible.value = true
}

function isImported(row) {
  return importedNames.value.has(row.name)
}

async function scanGitHub() {
  loading.scan = true
  syncMessage.value = ''
  try {
    const res = await listGitHubBanks()
    if (res.code === 200) {
      // 标记已存在的题库
      const localNames = new Set(banks.value.map(b => b.fileName))
      importedNames.value = localNames
      githubBanks.value = (res.data || []).map(f => ({
        ...f,
        _importing: false,
        _exists: localNames.has(f.name)
      }))
      syncMessage.value = `扫描完成，找到 ${githubBanks.value.length} 个题库文件`
      syncType.value = 'success'
    }
  } catch (e) {
    syncMessage.value = '扫描失败，请检查网络或 GitHub API 限流'
    syncType.value = 'error'
  } finally { loading.scan = false }
}

async function handleImport(row) {
  row._importing = true
  syncMessage.value = ''
  try {
    const res = await importFromGitHub({ name: row.name, path: row.path, downloadUrl: row.downloadUrl })
    if (res.code === 200) {
      importedNames.value.add(row.name)
      syncMessage.value = res.message || '导入成功'
      syncType.value = 'success'
      await loadBanks()
    }
  } catch (e) {
    syncMessage.value = '导入失败'
    syncType.value = 'error'
  } finally { row._importing = false }
}

async function handleSyncAll() {
  loading.syncAll = true
  syncMessage.value = ''
  try {
    const res = await syncAllGitHub()
    if (res.code === 200) {
      syncMessage.value = res.message || '同步完成'
      syncType.value = 'success'
      await loadBanks()
      await scanGitHub()
    }
  } catch (e) {
    syncMessage.value = '同步失败'
    syncType.value = 'error'
  } finally { loading.syncAll = false }
}
</script>

<style scoped>
.admin-page { min-height: 100vh; background: #f5f7fa; }
.admin-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 0 24px; background: #fff; box-shadow: 0 2px 8px rgba(0,0,0,0.06);
  height: 60px !important; position: sticky; top: 0; z-index: 100;
}
.header-left { display: flex; align-items: center; gap: 12px; }
.header-title { font-size: 18px; font-weight: 600; }
.header-right { display: flex; align-items: center; gap: 8px; }
.admin-name { font-size: 14px; color: #606266; }
.admin-body { max-width: 1200px; margin: 0 auto; padding: 24px 20px; }
.stat-row { margin-bottom: 20px; }
.stat-card { display: flex; align-items: center; gap: 16px; padding: 8px 0; }
.stat-icon { font-size: 28px; width: 56px; height: 56px; border-radius: 12px; display: flex; align-items: center; justify-content: center; }
.stat-value { font-size: 28px; font-weight: 700; color: #303133; }
.stat-label { font-size: 14px; color: #909399; margin-top: 2px; }
</style>
