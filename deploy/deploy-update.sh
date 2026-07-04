#!/bin/bash
# ============================================
# Marx-Exercise 生产部署更新脚本 (Linux版)
# 用法: ./deploy-update.sh [server_ip]
# 默认服务器: ginna.cn
#
# 也可在服务器上直接运行：
#   git pull
#   bash deploy/deploy-update.sh
# ============================================
set -e

SERVER="${1:-ginna.cn}"
SSH_USER="root"
REMOTE_DIR="/root/marx-exercise"
NGINX_WWW="/var/www/marx-exercise"
PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

echo "========================================"
echo "  Marx-Exercise 部署更新"
echo "  项目: $PROJECT_DIR"
echo "  目标: $SERVER"
echo "========================================"
echo ""

# 1. 构建后端
echo "[1/5] 构建后端..."
cd "$PROJECT_DIR/backend"
./mvnw clean package -DskipTests -q
echo "  √ 后端构建完成"
echo ""

# 2. 构建前端
echo "[2/5] 构建前端..."
cd "$PROJECT_DIR/frontend"
npm install --silent
npm run build
echo "  √ 前端构建完成"
echo ""

# 3. 打包
echo "[3/5] 打包部署文件..."
DEPLOY_TMP="/tmp/marx-deploy-$(date +%s)"
mkdir -p "$DEPLOY_TMP/backend" "$DEPLOY_TMP/frontend" "$DEPLOY_TMP/deploy"

cp "$PROJECT_DIR/backend/target/"*.jar "$DEPLOY_TMP/backend/"
cp -r "$PROJECT_DIR/frontend/dist/"* "$DEPLOY_TMP/frontend/"
cp "$PROJECT_DIR/deploy/docker-compose.yml" "$DEPLOY_TMP/deploy/"
cp "$PROJECT_DIR/deploy/nginx.conf" "$DEPLOY_TMP/deploy/"
cp "$PROJECT_DIR/deploy/setup.sh" "$DEPLOY_TMP/deploy/"
echo "  √ 打包完成"
echo ""

# 4. 上传（如果是远程部署）
if [ "$SERVER" != "localhost" ]; then
    echo "[4/5] 上传到 $SERVER ..."
    ssh "$SSH_USER@$SERVER" "mkdir -p $REMOTE_DIR/backend $REMOTE_DIR/frontend $REMOTE_DIR/deploy"
    scp -rq "$DEPLOY_TMP/backend/"* "$SSH_USER@$SERVER:$REMOTE_DIR/backend/"
    scp -rq "$DEPLOY_TMP/frontend/"* "$SSH_USER@$SERVER:$REMOTE_DIR/frontend/"
    scp -rq "$DEPLOY_TMP/deploy/"* "$SSH_USER@$SERVER:$REMOTE_DIR/deploy/"
    echo "  √ 上传完成"
    REMOTE_CMD="ssh $SSH_USER@$SERVER"
    REMOTE_WORKDIR="$REMOTE_DIR"
else
    echo "[4/5] 本地部署..."
    REMOTE_CMD=""
    REMOTE_WORKDIR="$DEPLOY_TMP"
fi
echo ""

# 5. 远程/本地部署
echo "[5/5] 部署服务..."
DEPLOY_CMD="cd $REMOTE_WORKDIR && \
  docker compose -p marx down 2>/dev/null || true && \
  cp deploy/docker-compose.yml . && \
  docker compose -p marx up -d --build && \
  mkdir -p $NGINX_WWW && \
  cp -r frontend/* $NGINX_WWW/ && \
  if [ -f /etc/nginx/sites-available/marx-exercise ]; then cp deploy/nginx.conf /etc/nginx/sites-available/marx-exercise; fi && \
  nginx -s reload 2>/dev/null || systemctl reload nginx 2>/dev/null || true && \
  docker image prune -f >/dev/null 2>&1 || true"

if [ -n "$REMOTE_CMD" ]; then
    $REMOTE_CMD "$DEPLOY_CMD"
else
    eval "$DEPLOY_CMD"
fi
echo "  √ 服务已启动"
echo ""

rm -rf "$DEPLOY_TMP"

echo "========================================"
echo "  ✅ 部署完成！"
echo "  前端: https://$SERVER"
echo "  后端: https://$SERVER/api/"
echo "========================================"

echo "========================================"
echo "  ✅ 部署完成！"
echo "  前端: https://$SERVER"
echo "  后端: https://$SERVER/api/"
echo "========================================"
