<template>
  <div class="detail-page">
    <div class="page-back">
      <el-button text @click="goBack">
        <el-icon><arrow-left /></el-icon>
        返回
      </el-button>
      <span class="page-title">{{ pageTitle }}</span>
    </div>

    <div class="detail-content" v-loading="loading">
      <div v-if="!loading && entries.length === 0" class="empty">暂无内容</div>
      <div v-for="(entry, i) in entries" :key="i" class="entry">
        <div class="entry-header">
          <el-tag size="large" :type="getTagType(entry)" effect="plain">{{ entry.title }}</el-tag>
          <span class="entry-date">{{ entry.date }}</span>
        </div>
        <div class="entry-body">
          <p v-for="(p, j) in entry.content.split('\n').filter(Boolean)" :key="j">{{ p }}</p>
          <ul v-if="entry.listItems && entry.listItems.length" class="entry-list">
            <li v-for="(item, k) in entry.listItems" :key="k">{{ item }}</li>
          </ul>
        </div>
        <el-divider v-if="i < entries.length - 1" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { parseUpdateLogs } from '../utils/markdownParser'
import { getVersionTag, NOTICE_TAGS } from '../utils/constants'

const props = defineProps({
  type: { type: String, default: '' }
})

const router = useRouter()
const route = useRoute()
const entries = ref([])
const loading = ref(true)

// 从路由名推断类型
const effectiveType = computed(() => {
  if (props.type) return props.type
  if (route.name === 'NoticeDetail') return 'notice'
  if (route.name === 'ChangelogDetail') return 'changelog'
  return route.query.type || 'changelog'
})

const pageTitle = computed(() => effectiveType.value === 'notice' ? '公告' : '更新日志')
const mdFile = computed(() => effectiveType.value === 'notice' ? '/notice.md' : '/update-logs.md')

function getTagType(entry) {
  if (effectiveType.value === 'notice') {
    // 公告使用轮换色
    const tags = ['danger', 'warning', 'info', 'success']
    return tags[entries.value.indexOf(entry) % tags.length]
  }
  // 更新日志按版本号着色
  return getVersionTag(entry.title)
}

function goBack() {
  router.back()
}

onMounted(async () => {
  try {
    const res = await fetch(mdFile.value)
    const text = await res.text()
    entries.value = parseUpdateLogs(text)
  } catch (e) {
    console.error('加载失败', e)
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.detail-page {
  max-width: 800px;
  margin: 0 auto;
  padding: 24px 20px;
}

.detail-content {
  margin-top: 24px;
}

.empty {
  text-align: center;
  color: #c0c4cc;
  padding: 60px 0;
  font-size: 16px;
}

.page-back {
  display: flex;
  align-items: center;
  gap: 16px;
}

.page-back .el-button {
  font-size: 14px;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.entry {
  margin-bottom: 8px;
}

.entry-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.entry-date {
  font-size: 13px;
  color: #c0c4cc;
}

.entry-body {
  font-size: 15px;
  color: #303133;
  line-height: 1.8;
  padding-left: 4px;
}

.entry-body p {
  margin: 6px 0;
}

.entry-list {
  margin: 8px 0 4px 20px;
  padding: 0;
  list-style: disc;
}

.entry-list li {
  margin: 4px 0;
  line-height: 1.6;
  color: #303133;
}
</style>
