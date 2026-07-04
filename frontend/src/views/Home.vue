<template>
  <div class="home">
    <!-- 顶部导航栏 -->
    <el-header class="home-header">
      <div class="header-left">
        <h2 class="header-title">📚 刷题系统</h2>
      </div>
      <div class="header-right">
        <el-dropdown trigger="click">
          <span class="user-dropdown-trigger">
            {{ userStore.nickname || userStore.username }}
            <el-icon class="el-icon--right"><arrow-down /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="$router.push('/about')">
                <el-icon><info-filled /></el-icon>关于
              </el-dropdown-item>
              <el-dropdown-item divided @click="dialogPasswordVisible = true">
                <el-icon><key /></el-icon>修改密码
              </el-dropdown-item>
              <el-dropdown-item divided @click="handleLogout">
                <el-icon><switch-button /></el-icon>退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-header>

    <!-- 修改密码对话框 -->
    <el-dialog v-model="dialogPasswordVisible" title="修改密码" width="400px" :close-on-click-modal="false">
      <el-form :model="pwdForm" :rules="pwdRules" ref="pwdFormRef" label-width="80px">
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="pwdForm.newPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="pwdForm.confirmPassword" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogPasswordVisible = false">取消</el-button>
        <el-button type="primary" :loading="pwdSubmitting" @click="handleChangePassword">确认修改</el-button>
      </template>
    </el-dialog>

    <div class="home-body">
      <div class="home-main">
        <!-- 欢迎区域 -->
        <div class="welcome-section">
          <h1>欢迎回来，{{ userStore.nickname || userStore.username }}！</h1>
          <p>选择题库和题型开始刷题练习</p>
        </div>

        <!-- 题库选择 -->
        <div class="section">
          <h3 class="section-title">📖 选择题库</h3>
          <el-row :gutter="20">
            <el-col :xs="24" :sm="12" v-for="bItem in banks" :key="bItem.id">
            <el-card
              class="bank-card"
              :class="{ 'bank-active': selectedBank?.id === bItem.id }"
              shadow="hover"
              @click="selectBank(bItem)"
            >
              <div class="bank-card-content">
                <div class="bank-icon">📝</div>
                <div>
                  <h4>{{ bItem.name }}</h4>
                  <p class="bank-desc">共 {{ bItem.questionCount }} 道题</p>
                  <p class="bank-time">更新时间：{{ formatTime(bItem.createdAt) }}</p>
                </div>
                <div v-if="selectedBank?.id === bItem.id" class="type-check">✓</div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>

      <!-- 题型选择 -->
      <div class="section" v-if="selectedBank">
        <h3 class="section-title">🎯 选择题型</h3>
        <el-row :gutter="20">
          <el-col :xs="24" :sm="8" v-for="type in questionTypes" :key="type.value">
            <el-card
              class="type-card"
              :class="{ 'type-active': selectedType === type.value }"
              shadow="hover"
              @click="selectedType = type.value"
            >
              <div class="type-icon">{{ type.icon }}</div>
              <h4>{{ type.label }}</h4>
              <el-tag size="small" :type="selectedType === type.value ? 'primary' : 'info'">
                {{ getBankQuestionCount(type.value) }} 题
              </el-tag>
              <div v-if="selectedType === type.value" class="type-check">✓</div>
            </el-card>
          </el-col>
        </el-row>
      </div>

      <!-- 开始按钮 -->
      <div class="section action-section" v-if="selectedBank && selectedType">
        <div class="action-btn-wrap" style="display:flex;gap:12px;justify-content:center">
          <el-button size="large" type="success" @click="goReview">
            📖 背题模式
          </el-button>
          <el-button size="large" type="primary" class="start-btn" @click="startPractice">
            🚀 练习模式
          </el-button>
        </div>
      </div>

      <!-- 模拟考试入口 -->
      <div class="section" v-if="selectedBank">
        <el-row :gutter="20">
          <el-col :xs="24">
            <el-card
              class="type-card"
              shadow="hover"
              @click="$router.push({ path: '/exam', query: { bankId: selectedBank?.id, bankName: selectedBank?.name } })"
            >
              <div class="type-icon">📝</div>
              <h4>模拟考试</h4>
            </el-card>
          </el-col>
        </el-row>
      </div>

      <!-- 快捷入口 -->
      <div class="section">
        <h3 class="section-title">🔗 快捷入口</h3>
        <div class="quick-grid">
          <el-card shadow="hover" class="quick-card" @click="$router.push('/wrong-book')">
            <div class="quick-card-content">
              <div class="quick-icon wrong-icon">❌</div>
              <div>
                <h4>错题本</h4>
                <p class="quick-desc">复习做错的题目</p>
              </div>
            </div>
          </el-card>
          <el-card shadow="hover" class="quick-card" @click="$router.push('/statistics')">
            <div class="quick-card-content">
              <div class="quick-icon stats-icon">📊</div>
              <div>
                <h4>统计信息</h4>
                <p class="quick-desc">查看学习进度与数据</p>
              </div>
            </div>
          </el-card>
          <el-card v-if="isAdmin" shadow="hover" class="quick-card admin-card" @click="$router.push('/admin')">
            <div class="quick-card-content">
              <div class="quick-icon admin-icon">⚙️</div>
              <div>
                <h4>后台管理</h4>
                <p class="quick-desc">用户/题库/题目管理</p>
              </div>
            </div>
          </el-card>
        </div>
      </div>
    </div>

    <!-- 右侧边栏（桌面端显示） -->
    <aside class="home-sidebar">
      <!-- 公告 -->
      <el-card class="sidebar-card" shadow="hover">
        <template #header>
          <div class="sidebar-header">
            <span @click="noticeCollapsed = !noticeCollapsed" style="cursor:pointer;flex:1;display:flex;align-items:center">
              📢 公告
              <el-icon style="font-size:14px;margin-left:4px;transition:transform 0.3s" :style="{ transform: noticeCollapsed ? 'rotate(-90deg)' : 'rotate(0deg)' }">
                <arrow-down />
              </el-icon>
            </span>
            <el-button text size="small" @click="$router.push('/notice')">更多</el-button>
          </div>
        </template>
        <div class="sidebar-body" v-show="!noticeCollapsed">
          <div
            v-for="(item, i) in notices.slice(0, 2)"
            :key="i"
            class="notice-item"
            @click="$router.push('/notice')"
          >
            <el-tag size="small" :type="noticeTag(i)">{{ item.title }}</el-tag>
            <span class="text-clamp-2">
              {{ sidebarText(item) }}
              <small v-if="item.listItems?.length" class="sidebar-count">包含 {{ item.listItems.length }} 项更新内容</small>
            </span>
            <div class="changelog-date" v-if="item.date">{{ item.date }}</div>
          </div>
          <div v-if="notices.length === 0" class="sidebar-empty">暂无公告</div>
        </div>
      </el-card>

      <!-- 更新日志 -->
      <el-card class="sidebar-card" shadow="hover">
        <template #header>
          <div class="sidebar-header">
            <span @click="changelogCollapsed = !changelogCollapsed" style="cursor:pointer;flex:1;display:flex;align-items:center">
              📋 更新日志
              <el-icon style="font-size:14px;margin-left:4px;transition:transform 0.3s" :style="{ transform: changelogCollapsed ? 'rotate(-90deg)' : 'rotate(0deg)' }">
                <arrow-down />
              </el-icon>
            </span>
            <el-button text size="small" @click="$router.push('/changelog')">更多</el-button>
          </div>
        </template>
        <div class="sidebar-body" v-show="!changelogCollapsed">
          <div
            v-for="(item, i) in changelogs.slice(0, 3)"
            :key="i"
            class="changelog-item"
            @click="$router.push('/changelog')"
          >
            <el-tag size="small" :type="logTag(i)">{{ item.title }}</el-tag>
            <span class="text-clamp-2">
              {{ sidebarText(item) }}
              <small v-if="item.listItems?.length" class="sidebar-count">包含 {{ item.listItems.length }} 项更新内容</small>
            </span>
            <div class="changelog-date" v-if="item.date">{{ item.date }}</div>
          </div>
          <div v-if="changelogs.length === 0" class="sidebar-empty">暂无更新日志</div>
        </div>
      </el-card>
    </aside>

    <!-- 移动端浮动入口按钮 -->
    <div class="mobile-sidebar-btns" v-if="isMobile">
      <el-button circle type="warning" size="large" @click="noticeDialogVisible = true" title="公告">
        📢
      </el-button>
      <el-button circle type="primary" size="large" @click="changelogDialogVisible = true" title="更新日志">
        📋
      </el-button>
    </div>

    <!-- 移动端弹窗：公告 -->
    <el-dialog v-model="noticeDialogVisible" title="📢 公告" width="90%" destroy-on-close>
      <div
        v-for="(item, i) in notices"
        :key="i"
        class="notice-item dialog-item"
        @click="noticeDialogVisible = false; $router.push('/notice')"
      >
        <el-tag size="small" :type="noticeTag(i)">{{ item.title }}</el-tag>
        <span class="text-clamp-2">
          {{ sidebarText(item) }}
          <small v-if="item.listItems?.length" class="sidebar-count">包含 {{ item.listItems.length }} 项更新内容</small>
        </span>
        <div class="changelog-date" v-if="item.date">{{ item.date }}</div>
      </div>
      <div v-if="notices.length === 0" style="text-align:center;color:#c0c4cc;padding:20px">暂无公告</div>
      <div style="text-align:center;margin-top:12px">
        <el-button type="primary" link @click="noticeDialogVisible = false; $router.push('/notice')">查看全部公告 →</el-button>
      </div>
    </el-dialog>

    <!-- 移动端弹窗：更新日志 -->
    <el-dialog v-model="changelogDialogVisible" title="📋 更新日志" width="90%" destroy-on-close>
      <div
        v-for="(item, i) in changelogs"
        :key="i"
        class="changelog-item dialog-item"
        @click="changelogDialogVisible = false; $router.push('/changelog')"
      >
        <el-tag size="small" :type="logTag(i)">{{ item.title }}</el-tag>
        <span class="text-clamp-2">
          {{ sidebarText(item) }}
          <small v-if="item.listItems?.length" class="sidebar-count">包含 {{ item.listItems.length }} 项更新内容</small>
        </span>
        <div class="changelog-date" v-if="item.date">{{ item.date }}</div>
      </div>
      <div v-if="changelogs.length === 0" style="text-align:center;color:#c0c4cc;padding:20px">暂无更新日志</div>
      <div style="text-align:center;margin-top:12px">
        <el-button type="primary" link @click="changelogDialogVisible = false; $router.push('/changelog')">查看全部更新日志 →</el-button>
      </div>
    </el-dialog>
  </div>
</div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowDown, InfoFilled, Key, SwitchButton } from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'
import { getBanks, getQuestionCounts } from '../api/auth'
import { getSuggestedRound } from '../api/practice'
import { changePassword } from '../api/auth'
import { parseUpdateLogs } from '../utils/markdownParser'
import { QUESTION_TYPES, NOTICE_TAGS, getVersionTag } from '../utils/constants'
import { formatTime } from '../utils/helpers'

const router = useRouter()
const userStore = useUserStore()

const banks = ref([])
const questionCounts = ref({})
const selectedBank = ref(null)
const selectedType = ref('')

const questionTypes = QUESTION_TYPES

const isAdmin = computed(() => userStore.user?.role === 'ADMIN')

function noticeTag(i) {
  return NOTICE_TAGS[i % NOTICE_TAGS.length]
}

function logTag(i) {
  return getVersionTag(changelogs.value[i]?.title)
}

/** 侧边栏：返回段落文本前两行 */
function sidebarText(item) {
  if (!item.content) return ''
  const lines = item.content.split('\n').filter(Boolean)
  if (lines.length === 0) return ''
  const summary = lines.slice(0, 2).join(' ')
  return lines.length > 2 ? summary + ' ...' : summary
}

// 响应式：检测移动端
const isMobile = ref(window.innerWidth < 768)
const updateMobile = () => { isMobile.value = window.innerWidth < 768 }

// 公告 & 更新日志
const notices = ref([])
const changelogs = ref([])
const noticeCollapsed = ref(false)
const changelogCollapsed = ref(false)

async function fetchNotices() {
  try {
    const res = await fetch('/notice.md')
    const text = await res.text()
    notices.value = parseUpdateLogs(text)
  } catch (e) {
    console.error('加载公告失败', e)
  }
}

async function fetchChangelogs() {
  try {
    const res = await fetch('/update-logs.md')
    const text = await res.text()
    changelogs.value = parseUpdateLogs(text)
  } catch (e) {
    console.error('加载更新日志失败', e)
  }
}

// 公告 & 更新日志弹窗
const noticeDialogVisible = ref(false)
const changelogDialogVisible = ref(false)

const dialogPasswordVisible = ref(false)
const pwdSubmitting = ref(false)
const pwdFormRef = ref(null)
const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})
const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 4, message: '新密码长度至少4位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: (rule, value, callback) => {
      if (value !== pwdForm.newPassword) callback(new Error('两次输入的密码不一致'))
      else callback()
    }, trigger: 'blur' }
  ]
}

async function handleChangePassword() {
  const valid = await pwdFormRef.value.validate().catch(() => false)
  if (!valid) return
  pwdSubmitting.value = true
  try {
    const res = await changePassword({
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword
    })
    if (res.code === 200) {
      ElMessage.success('密码修改成功')
      dialogPasswordVisible.value = false
      pwdForm.oldPassword = ''
      pwdForm.newPassword = ''
      pwdForm.confirmPassword = ''
    }
  } catch (e) {
    console.error('修改密码失败:', e)
  } finally {
    pwdSubmitting.value = false
  }
}

onMounted(async () => {
  window.addEventListener('resize', updateMobile)
  fetchNotices()
  fetchChangelogs()
  try {
    const res = await getBanks()
    if (res.code === 200) {
      banks.value = res.data
      if (banks.value.length > 0) {
        await selectBank(banks.value[0])
      }
    }
  } catch (e) {
    console.error('获取题库列表失败:', e)
  }
})

function getBankQuestionCount(type) {
  return questionCounts.value[type] || 0
}

async function selectBank(bank) {
  selectedBank.value = bank
  selectedType.value = ''
  // 获取该题库各题型真实数量
  try {
    const res = await getQuestionCounts(bank.id)
    if (res.code === 200) {
      questionCounts.value = res.data
    }
  } catch (e) {
    console.error('获取题型数量失败:', e)
  }
}

async function startPractice() {
  if (!selectedBank.value || !selectedType.value) return
  try {
    // 获取建议轮次（自动跳到未完成或下一轮）
    const res = await getSuggestedRound(selectedBank.value.id, selectedType.value)
    const roundNum = res.code === 200 ? res.data : 1
    router.push({
      path: '/practice',
      query: {
        bankId: selectedBank.value.id,
        bankName: selectedBank.value.name,
        questionType: selectedType.value,
        roundNum
      }
    })
  } catch (e) {
    router.push({
      path: '/practice',
      query: {
        bankId: selectedBank.value.id,
        bankName: selectedBank.value.name,
        questionType: selectedType.value,
        roundNum: 1
      }
    })
  }
}

function goReview() {
  if (!selectedBank.value || !selectedType.value) return
  router.push({
    path: '/review',
    query: {
      bankId: selectedBank.value.id,
      bankName: selectedBank.value.name,
      type: selectedType.value
    }
  })
}

function handleLogout() {
  userStore.logout()
  ElMessage.success('已退出登录')
  router.push('/login')
}
</script>

<style scoped>
.home {
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
}

.home-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 40px;
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  height: 60px !important;
}

.header-title {
  font-size: 20px;
  font-weight: 700;
  color: #303133;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-dropdown-trigger {
  color: #606266;
  font-size: 14px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
}
.user-dropdown-trigger:hover {
  color: #409eff;
}

.home-body {
  max-width: 1000px;
  margin: 0 auto;
  padding: 32px 20px;
}

.welcome-section {
  text-align: center;
  margin-bottom: 36px;
}

.welcome-section h1 {
  font-size: 28px;
  color: #303133;
  margin-bottom: 8px;
}

.welcome-section p {
  color: #909399;
  font-size: 16px;
}

.section {
  margin-bottom: 32px;
}

.section-title {
  font-size: 18px;
  color: #303133;
  margin-bottom: 16px;
  padding-left: 12px;
  border-left: 4px solid #409eff;
}

.bank-card {
  cursor: pointer;
  transition: all 0.3s ease;
  margin-bottom: 12px;
  border: 2px solid transparent;
}

.bank-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.bank-active {
  border-color: #409eff !important;
  background: linear-gradient(135deg, #ecf5ff 0%, #ffffff 100%);
  box-shadow: 0 4px 16px rgba(64, 158, 255, 0.2) !important;
}

.bank-card-content {
  display: flex;
  align-items: center;
  gap: 16px;
  position: relative;
}

.selected-badge {
  position: absolute;
  right: -8px;
  top: -8px;
  background: #409eff;
  color: #fff;
  font-size: 12px;
  padding: 2px 10px;
  border-radius: 12px;
  font-weight: 600;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.4);
  animation: badgePop 0.3s ease;
}

@keyframes badgePop {
  0% { transform: scale(0); }
  50% { transform: scale(1.2); }
  100% { transform: scale(1); }
}

.bank-icon {
  font-size: 36px;
  transition: transform 0.3s ease;
}

.bank-active .bank-icon {
  animation: iconBounce 0.4s ease;
}

@keyframes iconBounce {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.2); }
}

.bank-desc {
  color: #909399;
  font-size: 13px;
  margin-top: 4px;
}

.bank-time {
  color: #b0b3b8;
  font-size: 12px;
  margin-top: 2px;
}

.type-card {
  cursor: pointer;
  text-align: center;
  padding: 24px 0;
  transition: all 0.3s ease;
  border: 2px solid #e4e7ed;
  position: relative;
  background: #fff;
}

.type-card:hover {
  border-color: #c0c4cc;
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
}

.type-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
}

.type-active {
  border-color: #409eff !important;
  background: linear-gradient(135deg, #ecf5ff 0%, #f0f8ff 100%);
  box-shadow: 0 4px 16px rgba(64, 158, 255, 0.2) !important;
  transform: translateY(-2px);
}

.type-check {
  position: absolute;
  top: 4px;
  right: 12px;
  color: #409eff;
  font-size: 20px;
  font-weight: 700;
  animation: checkPop 0.3s ease;
}

@keyframes checkPop {
  0% { transform: scale(0) rotate(-45deg); }
  100% { transform: scale(1) rotate(0deg); }
}

.type-icon {
  font-size: 40px;
  margin-bottom: 8px;
}

.action-section {
  text-align: center;
  padding: 20px 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.action-btn-wrap {
  width: 100%;
  max-width: 320px;
}

.action-btn-wrap .el-button {
  width: 100%;
  margin-left: 0px;
}

.quick-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
}

.quick-grid .quick-card {
  flex: 1;
  min-width: 180px;
}

.quick-card {
  cursor: pointer;
  transition: transform 0.2s;
}

.quick-card:hover {
  transform: translateY(-2px);
}

.admin-card {
  border-color: #f56c6c;
}

.admin-card:hover {
  box-shadow: 0 4px 16px rgba(245, 108, 108, 0.2) !important;
}

.quick-card-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.quick-icon {
  font-size: 32px;
}

.quick-desc {
  color: #909399;
  font-size: 13px;
  margin-top: 4px;
}

.exam-card {
  border-color: #f56c6c;
}

.exam-card:hover {
  box-shadow: 0 4px 16px rgba(245, 108, 108, 0.2) !important;
}

/* ========== 右侧边栏 ========== */
.home-body {
  display: flex;
  gap: 28px;
  align-items: flex-start;
}

.home-main {
  flex: 1;
  min-width: 0;
}

.home-sidebar {
  width: 280px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 20px;
  position: sticky;
  top: 80px;
}

.sidebar-card {
  border-radius: 12px;
}

.sidebar-card .el-card__header {
  padding: 12px 16px;
}

.sidebar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  font-size: 15px;
}

.sidebar-body {
  font-size: 13px;
  color: #606266;
}

.sidebar-count {
  display: block;
  font-size: 11px;
  color: #c0c4cc;
  margin-top: 2px;
}

.text-clamp-2 {
  display: -webkit-inline-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.6;
  flex: 1;
  min-width: 0;
  margin-top: 4px;
}

.notice-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
  margin-bottom: 10px;
  line-height: 1.6;
}

.changelog-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
  margin-bottom: 14px;
  line-height: 1.6;
}

.changelog-item .el-tag,
.notice-item .el-tag,
.dialog-item .el-tag {
  width: fit-content;
}

.changelog-date {
  font-size: 12px;
  color: #c0c4cc;
  margin-top: 2px;
}

.sidebar-empty {
  text-align: center;
  color: #c0c4cc;
  padding: 12px 0;
  font-size: 13px;
}

.dialog-item {
  cursor: pointer;
  padding: 8px 4px;
  border-radius: 8px;
  transition: background 0.2s;
}

.dialog-item:hover {
  background: #f5f7fa;
}

.notice-item, .changelog-item {
  cursor: pointer;
}

@media screen and (max-width: 768px) {
  .action-btn-wrap {
    max-width: 100%;
  }

  .home-body {
    flex-direction: column;
  }

  .home-sidebar {
    display: none;
  }

  .home-main {
    width: 100%;
  }
}

/* 移动端浮动按钮 */
.mobile-sidebar-btns {
  position: fixed;
  bottom: 80px;
  right: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  z-index: 1000;
}

.mobile-sidebar-btns .el-button {
  width: 44px;
  height: 44px;
  font-size: 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}
</style>
