#!/bin/bash
# Deploy: get SSL (Let's Encrypt or self-signed) and start stack.
set -e
DEPLOY_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$DEPLOY_DIR"

# DOMAIN и EMAIL могут быть переданы извне (из deploy-скриптов).
# Если не переданы — спросим у пользователя.
if [ -z "$DOMAIN" ]; then
  read -rp "Enter main domain (e.g. example.com): " DOMAIN
fi

if [ -z "$EMAIL" ]; then
  read -rp "Enter email for Let's Encrypt (e.g. admin@${DOMAIN}): " EMAIL
fi

WWW_DOMAIN="www.${DOMAIN}"

mkdir -p "$DEPLOY_DIR/nginx/ssl" "$DEPLOY_DIR/letsencrypt" "$DEPLOY_DIR/uploads" "$DEPLOY_DIR/files"
# Bind-mount ./uploads: приложение в контейнере под не-root; без прав на запись старт может упасть на createDirectories.
chmod -f a+rwx "$DEPLOY_DIR/uploads" 2>/dev/null || true
chmod -f a+rwx "$DEPLOY_DIR/files" 2>/dev/null || true

if [ ! -f "$DEPLOY_DIR/nginx/ssl/cert.pem" ] || [ ! -f "$DEPLOY_DIR/nginx/ssl/key.pem" ]; then
  echo "Getting SSL certificate for ${DOMAIN}..."

  # Обновляем nginx.conf под указанный домен
  if [ -f "$DEPLOY_DIR/nginx/nginx.conf" ]; then
    sed -i "s/devi-guide.ru/${DOMAIN}/g" "$DEPLOY_DIR/nginx/nginx.conf"
    sed -i "s/www\.devi-guide\.ru/${WWW_DOMAIN}/g" "$DEPLOY_DIR/nginx/nginx.conf"
  fi

  if docker run --rm -p 80:80 -v "$DEPLOY_DIR/letsencrypt:/etc/letsencrypt" \
    certbot/certbot certonly --standalone \
    -d "$DOMAIN" -d "$WWW_DOMAIN" \
    --email "$EMAIL" --agree-tos --non-interactive 2>/dev/null; then
    if [ -f "$DEPLOY_DIR/letsencrypt/live/${DOMAIN}/fullchain.pem" ]; then
      cp "$DEPLOY_DIR/letsencrypt/live/${DOMAIN}/fullchain.pem" "$DEPLOY_DIR/nginx/ssl/cert.pem"
      cp "$DEPLOY_DIR/letsencrypt/live/${DOMAIN}/privkey.pem" "$DEPLOY_DIR/nginx/ssl/key.pem"
      echo "Let's Encrypt certificate installed."
    fi
  fi
  if [ ! -f "$DEPLOY_DIR/nginx/ssl/cert.pem" ]; then
    echo "Using self-signed certificate."
    openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
      -keyout "$DEPLOY_DIR/nginx/ssl/key.pem" -out "$DEPLOY_DIR/nginx/ssl/cert.pem" \
      -subj "/CN=${DOMAIN}"
  fi
fi

echo "Starting containers (postgres, app, nginx)..."
set +e
docker compose -f "$DEPLOY_DIR/docker-compose.deploy.yml" --project-directory "$DEPLOY_DIR" up -d --build
compose_exit=$?
set -e

if [ $compose_exit -ne 0 ]; then
  echo "docker compose failed. Diagnostic info:"
  docker compose -f "$DEPLOY_DIR/docker-compose.deploy.yml" --project-directory "$DEPLOY_DIR" ps || true
  echo "---- app logs (last 200) ----"
  docker compose -f "$DEPLOY_DIR/docker-compose.deploy.yml" --project-directory "$DEPLOY_DIR" logs --tail 200 app || true
  exit $compose_exit
fi
echo "Done. Site: https://${DOMAIN}"
