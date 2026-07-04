<template>
  <div class="about">
    <el-header class="about-header">
      <div class="header-left">
        <h2 class="header-title">📚 刷题系统</h2>
      </div>
      <div class="header-right">
        <el-button text @click="$router.push('/')">← 返回首页</el-button>
      </div>
    </el-header>

    <div class="about-body">
      <el-card class="about-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <span class="card-title">关于本系统</span>
          </div>
        </template>
        <div class="about-content">
          <h2>马克思主义理论课程练习平台</h2>
          <p class="version">版本 1.0.0</p>
          <el-divider />
          <p class="desc">
            本系统是一款专为马克思主义理论课程设计的在线刷题平台，支持单选题、多选题和判断题三种题型。
            提供智能刷题、错题回顾、学习统计等功能，帮助高效备考。
          </p>
          <el-divider />
          <h3>功能特性</h3>
          <ul class="feature-list">
            <li>📝 多题库支持，涵盖多门课程</li>
            <li>🔘 三种题型：单选 / 多选 / 判断</li>
            <li>🔄 随机出题，打乱选项顺序</li>
            <li>📊 学习进度统计与数据分析</li>
            <li>❌ 错题自动收录，针对性复习</li>
            <li>🌐 后台管理，题库在线更新</li>
          </ul>
          <el-divider />
          <h3>鸣谢</h3>
          <p class="thanks">感谢 <strong>keggin-CHN</strong> 提供的题库数据支持。</p>
          <el-divider />
          <h3>技术支持</h3>
          <div class="tech-stack">
            <el-tag type="primary">DeepSeek</el-tag>
            <el-tag type="success">Vue 3</el-tag>
            <el-tag type="warning">Spring Boot</el-tag>
            <el-tag type="danger">Element Plus</el-tag>
            <el-tag type="info">MyBatis-Plus</el-tag>
            <el-tag>MySQL</el-tag>
          </div>
          <el-divider />
          <h3>联系方式</h3>
          <div class="contact-info">
            <p>管理员：Ginna_238</p>
            <p>邮箱：ginna_238@qq.com</p>
            <p>如有问题或建议，请联系管理员</p>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
</script>

<style scoped>
.about {
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
}

.about-header {
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

.about-body {
  max-width: 700px;
  margin: 0 auto;
  padding: 40px 20px;
}

.about-card {
  border-radius: 12px;
}

.card-header {
  display: flex;
  align-items: center;
}

.card-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.about-content {
  padding: 0 8px;
}

.about-content h2 {
  font-size: 24px;
  color: #303133;
  margin-bottom: 8px;
}

.version {
  color: #909399;
  font-size: 14px;
}

.desc {
  color: #606266;
  font-size: 15px;
  line-height: 1.8;
}

.feature-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.feature-list li {
  padding: 6px 0;
  color: #606266;
  font-size: 15px;
}

.thanks {
  color: #606266;
  font-size: 15px;
  line-height: 1.8;
}

.contact-info {
  color: #606266;
  font-size: 15px;
  line-height: 1.8;
}

.contact-info p {
  margin: 4px 0;
}

.tech-stack {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 12px;
}
</style>
