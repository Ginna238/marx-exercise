<p align="center">
  <img src="https://img.icons8.com/fluency/96/book.png" alt="logo" width="80"/>
</p>

<h1 align="center">Marx-Exercise 刷题系统</h1>

<p align="center">
  <strong>马克思主义理论课程在线练习平台</strong>
  <br>
  支持刷题练习、模拟考试、错题本、背题模式、学习统计
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Spring%20Boot-4.1.0-6DB33F?logo=springboot" alt="Spring Boot"/>
  <img src="https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk" alt="Java"/>
  <img src="https://img.shields.io/badge/Vue-3.5-4FC08D?logo=vue.js" alt="Vue"/>
  <img src="https://img.shields.io/badge/MySQL-8.4-4479A1?logo=mysql" alt="MySQL"/>
  <img src="https://img.shields.io/badge/license-MIT-yellow" alt="License"/>
</p>

---

## 📋 目录

- [功能特性](#-功能特性)
- [技术栈](#-技术栈)
- [项目结构](#-项目结构)
- [快速开始](#-快速开始)
- [API 概览](#-api-概览)
- [部署](#-部署)
- [开发指南](#-开发指南)
- [许可证](#-许可证)

---

## ✨ 功能特性

### 👤 用户端
| 功能 | 说明 |
|------|------|
| 🎯 **刷题练习** | 选择题库 → 选择题型 → 分轮次练习，自动下一题，实时反馈正确/错误 |
| 📝 **模拟考试** | 随机组卷（60单选+10多选+10判断），60分钟倒计时，自动评分（100分制） |
| 📖 **背题模式** | 顺序浏览题目，显示答案，关键词搜索，快速跳转 |
| ❌ **错题本** | 自动记录错题，按题型/题库筛选，批量管理，错题重练 |
| 📊 **学习统计** | 答题概览、进度列表、题型分布、7天趋势、考试记录 |
| 📢 **公告 & 更新日志** | 侧边栏动态展示，Markdown 格式，版本号语义化着色 |

### 🔐 管理员端
| 功能 | 说明 |
|------|------|
| 👥 **用户管理** | 用户列表、重置密码、删除用户 |
| 📚 **题库管理** | 查看题库、重新加载题库 |
| 📋 **题目管理** | 按题库 + 题型筛选查看题目列表 |
| 🔄 **GitHub 同步** | 从 GitHub 仓库在线导入/同步题库 |
| 📈 **用户刷题统计** | 查看每个用户的答题量、正确率、错题数、考试详情 |

---

## 🛠️ 技术栈

### 后端
| 组件 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 4.1.0 | 应用框架 |
| Java | 21 | 运行环境 |
| Spring Security | 内置 | JWT 无状态认证 |
| MyBatis-Plus | 3.5.16 | ORM 框架 |
| MySQL | 8.4 | 数据库 |
| JJWT | 0.12.6 | JWT 令牌 |
| Lombok | 最新 | 代码简化 |
| Maven | 3.9+ | 构建工具 |

### 前端
| 组件 | 版本 | 说明 |
|------|------|------|
| Vue | 3.5 | 前端框架 |
| Vue Router | 4.4 | 路由管理 |
| Pinia | 2.2 | 状态管理 |
| Element Plus | 2.8 | UI 组件库 |
| Axios | 1.7 | HTTP 客户端 |
| Vite | 5.4 | 构建工具 |
| Vitest | 4.1 | 单元测试 |

---

## 📁 项目结构

```
marx-exercise/
├── backend/                          # Spring Boot 后端
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/cn/ginna/marxexercise/
│   │   │   │   ├── common/           # 公共模块
│   │   │   │   │   ├── config/       # 安全、异常处理、JWT过滤器
│   │   │   │   │   ├── dto/          # 数据传输对象
│   │   │   │   │   ├── entity/       # 数据库实体
│   │   │   │   │   ├── mapper/       # MyBatis-Plus 映射接口
│   │   │   │   │   └── util/         # JWT工具类
│   │   │   │   ├── admin/            # 管理员模块
│   │   │   │   └── user/             # 用户模块（Controller + Service）
│   │   │   └── resources/
│   │   │       ├── application.yaml.example  # 配置模板
│   │   │       ├── schema.sql                # 数据库建表脚本
│   │   │       └── ques-bank/                # 题库 JSON 文件
│   │   └── test/                      # 单元测试
│   ├── pom.xml
│   ├── Dockerfile
│   └── mvnw / mvnw.cmd
│
├── frontend/                         # Vue 3 前端
│   ├── src/
│   │   ├── api/                      # API 接口封装
│   │   ├── router/                   # 路由配置
│   │   ├── stores/                   # Pinia 状态管理
│   │   ├── utils/                    # 工具函数 & 常量
│   │   ├── views/                    # 页面组件
│   │   ├── App.vue
│   │   └── main.js
│   ├── public/
│   │   ├── notice.md                 # 公告数据
│   │   └── update-logs.md            # 更新日志数据
│   └── package.json
│
├── deploy/                           # 部署配置
│   ├── docker-compose.yml            # Docker 编排
│   ├── nginx.conf                    # Nginx 反向代理
│   └── setup.sh                      # 一键部署脚本
│
└── e:/desktop/                       # 运行脚本
    ├── start-local.bat               # 一键本地启动
    ├── start-backend.bat             # 单独启动后端
    ├── start-frontend.bat            # 单独启动前端
    ├── run-tests.bat                 # 运行全部测试
    ├── deploy-update.bat             # Windows 部署入口
    └── deploy-update.sh              # Linux 部署脚本
```

---

## 🚀 快速开始

### 前置要求
- JDK 21+
- Node.js 18+
- MySQL 8.4+
- Maven 3.9+

### 1. 配置数据库

```bash
# 创建数据库
mysql -u root -p -e "CREATE DATABASE marx_exercise CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 导入表结构
mysql -u root -p marx_exercise < backend/src/main/resources/schema.sql
```

### 2. 配置后端

```bash
# 复制配置模板
cp backend/src/main/resources/application.yaml.example backend/src/main/resources/application.yaml

# 编辑配置文件，填入数据库密码和 JWT 密钥
```

### 3. 启动后端

```bash
cd backend
./mvnw spring-boot:run
```

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

### 5. 访问

打开浏览器访问 `http://localhost:5173`

---

## 📡 API 概览

| 前缀 | 权限 | 说明 |
|------|------|------|
| `POST /api/auth/login` | 公开 | 用户登录 |
| `POST /api/auth/register` | 公开 | 用户注册 |
| `GET /api/auth/me` | 用户 | 获取当前用户 |
| `PUT /api/auth/change-password` | 用户 | 修改密码 |
| `GET /api/banks` | 公开 | 获取题库列表 |
| `POST /api/exam/start` | 用户 | 开始考试 |
| `POST /api/exam/submit` | 用户 | 提交考试 |
| `GET /api/practice/start` | 用户 | 开始练习 |
| `POST /api/practice/submit` | 用户 | 提交练习答案 |
| `GET /api/review/questions` | 用户 | 背题模式查题 |
| `GET /api/statistics` | 用户 | 学习统计 |
| `GET /api/wrong/list` | 用户 | 错题列表 |
| `GET /api/admin/users` | 管理员 | 用户管理 |
| `POST /api/admin/banks/reload` | 管理员 | 重新加载题库 |

---

## 🐳 部署

### Docker 部署

```bash
cd deploy
docker compose -p marx up -d --build
```

### 手动部署

```bash
# 1. 构建后端
cd backend && ./mvnw clean package -DskipTests

# 2. 构建前端
cd frontend && npm install && npm run build

# 3. 部署到服务器
# 将 backend/target/*.jar 和 frontend/dist/* 上传到服务器
# 配置 Nginx 反向代理
```

---

## 🧪 测试

```bash
# 后端测试
cd backend && ./mvnw test

# 前端测试
cd frontend && npm test
```

---

## 📜 许可证

[MIT](LICENSE)

---

<p align="center">
  如有问题或建议，请联系：<a href="mailto:ginna_238@qq.com">ginna_238@qq.com</a>
</p>
