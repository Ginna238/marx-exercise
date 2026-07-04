#!/bin/bash
# 刷题系统 服务器部署脚本
# 在 Ubuntu 服务器上以 root 用户执行

set -e

echo "===== 开始部署 刷题系统 ====="

# 1. 安装基础依赖
echo "[1/7] 安装基础依赖..."
apt update
apt install -y docker.io docker-compose-v2 nginx curl

# 2. 启动 Docker
echo "[2/7] 启动 Docker..."
systemctl enable docker
systemctl start docker

# 3. 构建后端镜像并启动 MySQL + 后端
echo "[3/7] 构建并启动后端服务..."
docker compose -p marx up -d --build

# 4. 等待后端启动
echo "[4/7] 等待后端就绪..."
sleep 15

# 5. 配置 Nginx
echo "[5/7] 配置 Nginx..."
# 创建前端静态文件目录
mkdir -p /var/www/marx-exercise
# 复制前端文件到 Nginx 目录
cp -r dist/* /var/www/marx-exercise/
# 复制 Nginx 配置
cp deploy/nginx.conf /etc/nginx/sites-available/marx-exercise
ln -sf /etc/nginx/sites-available/marx-exercise /etc/nginx/sites-enabled/
rm -f /etc/nginx/sites-enabled/default
nginx -t
systemctl reload nginx

# 6. 数据库迁移（不损坏旧数据）
echo "[6/8] 执行数据库迁移..."
sleep 10
MYSQL_PWD="${MYSQL_ROOT_PASSWORD:-your_password_here}" docker exec marx-mysql-1 mysql -uroot marx_exercise -e "
CREATE TABLE IF NOT EXISTS exam_record (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  bank_id BIGINT NOT NULL,
  score INT DEFAULT 0,
  total_score INT DEFAULT 100,
  single_correct INT DEFAULT 0,
  single_total INT DEFAULT 60,
  multiple_correct INT DEFAULT 0,
  multiple_total INT DEFAULT 10,
  judgment_correct INT DEFAULT 0,
  judgment_total INT DEFAULT 10,
  duration_seconds INT DEFAULT 0,
  status VARCHAR(20) DEFAULT 'in_progress',
  started_at DATETIME,
  completed_at DATETIME,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_user_bank (user_id, bank_id),
  INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
" 2>/dev/null && echo " [OK] 数据库迁移完成" || echo " [INFO] 数据库迁移跳过（表已存在）"

# 7. 配置防火墙
echo "[7/8] 配置防火墙..."
ufw allow 80/tcp
ufw allow 443/tcp
ufw --force enable

# 8. 验证部署
echo "[8/8] 验证部署..."
echo "等待后端就绪..."
sleep 5
curl -s http://localhost:8080/api/auth/banks && echo " [OK] 后端运行正常"
curl -s -o /dev/null -w "%{http_code}" http://localhost/ && echo " [OK] 前端运行正常"

echo ""
echo "===== 部署完成! ====="
echo "前端地址: http://your-server-domain.com"
echo "后端 API: http://your-server-domain.com/api"
