#!/bin/bash

# Скрипт деплоя DevOps Lesson приложения
# Использование: ./deploy.sh [start|stop|restart|logs|clean]

set -e

# Цвета для вывода
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Функции для логирования
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Проверка наличия Docker и Docker Compose
check_dependencies() {
    log_info "Проверка зависимостей..."
    
    if ! command -v docker &> /dev/null; then
        log_error "Docker не установлен. Установите Docker и попробуйте снова."
        exit 1
    fi
    
    if ! command -v docker-compose &> /dev/null; then
        log_error "Docker Compose не установлен. Установите Docker Compose и попробуйте снова."
        exit 1
    fi
    
    log_success "Все зависимости установлены"
}

# Создание SSL сертификатов для разработки
create_ssl_certs() {
    log_info "Создание SSL сертификатов для разработки..."
    
    mkdir -p nginx/ssl
    
    if [ ! -f nginx/ssl/cert.pem ] || [ ! -f nginx/ssl/key.pem ]; then
        openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
            -keyout nginx/ssl/key.pem \
            -out nginx/ssl/cert.pem \
            -subj "/C=RU/ST=Moscow/L=Moscow/O=DevOpsLesson/OU=IT/CN=localhost"
        
        log_success "SSL сертификаты созданы"
    else
        log_info "SSL сертификаты уже существуют"
    fi
}

# Запуск приложения
start_app() {
    log_info "Запуск DevOps Lesson приложения..."
    
    check_dependencies
    create_ssl_certs
    
    # Остановка существующих контейнеров
    docker-compose down 2>/dev/null || true
    
    # Сборка и запуск
    docker-compose up --build -d
    
    log_success "Приложение запущено!"
    log_info "Доступные URL (docker-compose.yml: app + db):"
    log_info "  - App: http://localhost:8181"
    log_info "  - DB:  localhost:5432"
}

# Остановка приложения
stop_app() {
    log_info "Остановка DevOps Lesson приложения..."
    
    docker-compose down
    
    log_success "Приложение остановлено"
}

# Перезапуск приложения
restart_app() {
    log_info "Перезапуск DevOps Lesson приложения..."
    
    stop_app
    start_app
}

# Просмотр логов
show_logs() {
    log_info "Просмотр логов приложения..."
    
    if [ -z "$2" ]; then
        docker-compose logs -f
    else
        docker-compose logs -f "$2"
    fi
}

# Очистка
clean_app() {
    log_warning "Очистка всех контейнеров и образов..."
    
    read -p "Вы уверены? Это удалит все данные! (y/N): " -n 1 -r
    echo
    
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        docker-compose down -v --rmi all
        docker system prune -f
        
        log_success "Очистка завершена"
    else
        log_info "Очистка отменена"
    fi
}

# Проверка статуса
check_status() {
    log_info "Проверка статуса сервисов..."
    
    echo "=== Статус контейнеров ==="
    docker-compose ps
    
    echo -e "\n=== Health checks ==="
    
    # Проверка PostgreSQL
    if docker-compose exec -T db pg_isready -U postgres >/dev/null 2>&1; then
        echo -e "${GREEN}✓ PostgreSQL: OK${NC}"
    else
        echo -e "${RED}✗ PostgreSQL: ERROR${NC}"
    fi
    
    # Проверка Spring Boot приложения (порт проброса см. docker-compose.yml)
    if curl -f http://localhost:8181/actuator/health >/dev/null 2>&1; then
        echo -e "${GREEN}✓ Spring Boot App: OK${NC}"
    else
        echo -e "${RED}✗ Spring Boot App: ERROR${NC}"
    fi
}

# Основная логика
case "${1:-start}" in
    start)
        start_app
        ;;
    stop)
        stop_app
        ;;
    restart)
        restart_app
        ;;
    logs)
        show_logs "$@"
        ;;
    status)
        check_status
        ;;
    clean)
        clean_app
        ;;
    *)
        echo "Использование: $0 {start|stop|restart|logs|status|clean}"
        echo ""
        echo "Команды:"
        echo "  start   - Запустить приложение"
        echo "  stop    - Остановить приложение"
        echo "  restart - Перезапустить приложение"
        echo "  logs    - Показать логи (опционально: имя сервиса)"
        echo "  status  - Проверить статус сервисов"
        echo "  clean   - Очистить все контейнеры и образы"
        exit 1
        ;;
esac
