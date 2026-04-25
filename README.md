# Spring MVC — заготовка под деплой

Этот репозиторий — **стартовый шаблон** для любого проекта на **Spring Boot 3**: серверный рендеринг 
**HTML-страниц** через **Thymeleaf** (`src/main/resources/templates/`), 
при необходимости **REST API**, **JPA** и **PostgreSQL**. 

В комплекте — пример **Docker Compose** для локальной разработки и сценарий **деплоя на VPS** 
(Nginx, HTTPS, Let’s Encrypt или самоподписанный сертификат).

Слои кода (пакеты под `com.example.mvc_default`):

| Пакет | Назначение |
|--------|------------|
| `controller` | HTTP: маршруты к шаблонам и API |
| `service` | Бизнес-логика |
| `repository` | Доступ к данным (Spring Data JPA) |
| `entity` | Сущности БД (JPA) |

Страницы — обычные HTML (Thymeleaf при желании добавляет подстановки). Статика — `src/main/resources/static/`.

## Требования

- **JDK 17**
- **Docker** и **Docker Compose** (для контейнеров)
- Для деплоя на сервер: **SSH**, на сервере — Docker

---------------------------------------------------
## Деплой на VPS (Nginx + HTTPS + PostgreSQL)
Основной способ с **Windows**: скрипт сам собирать проект не обязан, 
но JAR должен уже лежать в `build\libs\` (см. раздел «Сборка JAR»).

1. Соберите проект: `gradlew.bat build` (из корня репозитория).
2. Запустите **`deploy-beget.bat`** (двойной щелчок или из `cmd` в корне проекта).

Скрипт по шагам сам: спросит 
**IP сервера**, 
**SSH-пользователя**, 
**полный путь к папке на сервере**, 
**домен**
**email для Let’s Encrypt**; 

создаст каталоги по SSH; скопирует 
`Dockerfile.deploy`, 
`docker-compose.deploy.yml`, 
`nginx/nginx.conf`, 
`init-ssl.sh`, 
`backup-db.sh`
и нужный **JAR** (не `*-plain.jar`); 
при наличии — `init-db.sql`; 
на сервере выставит права на скрипты и выполнит **`init-ssl.sh`** — получение сертификата (**Let’s Encrypt**, если получится; иначе **самоподписанный**) и запуск **`docker compose`** по `docker-compose.deploy.yml`.

Нужны **SSH** и **`scp`** в PATH (как в OpenSSH для Windows), на сервере — **Docker**. DNS домена должен указывать на сервер, порты **80** и **443** — быть доступны для выпуска сертификата.


-----------------------------------
## Локальный запуск (без полного Docker-стека приложения)

1. Поднять только PostgreSQL (из корня проекта):

   ```bash
   docker compose up -d db
   ```

2. Запустить приложение с URL к локальной БД:

   ```bash
   # Windows (PowerShell)
   $env:SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/mvcdb"
   $env:SPRING_DATASOURCE_USERNAME="postgres"
   $env:SPRING_DATASOURCE_PASSWORD="postgres"
   .\gradlew.bat bootRun

   # Linux / macOS
   export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/mvcdb
   export SPRING_DATASOURCE_USERNAME=postgres
   export SPRING_DATASOURCE_PASSWORD=postgres
   ./gradlew bootRun
   ```

Приложение по умолчанию слушает порт из конфигурации Spring (в шаблоне `application.properties` задан `server.port`; при необходимости переопределите переменной `SERVER_PORT`).

Откройте в браузере главную страницу (корень `/`). Дополнительные маршруты добавляйте в `WebController` и соответствующие шаблоны в `templates/`.


Исполняемый (fat) JAR лежит в `build/libs/`, файл **без** суффикса `-plain.jar`.

-----------------------------------
## Переменные окружения (.env)

Проект подготовлен для деплоя через `.env` (секреты не хардкодятся в compose).

1. Скопируйте шаблон:

```bash
cp .env.example .env
```

2. Заполните свои значения в `.env`:
- `POSTGRES_PASSWORD`
- `SPRING_DATASOURCE_PASSWORD`
- `APP_ADMIN_USERNAME`
- `APP_ADMIN_PASSWORD`

3. Запускайте compose как обычно — значения автоматически подхватятся из `.env`.

Важно: файл `.env` добавлен в `.gitignore`, в репозиторий не отправляется.

### Файлы загрузок (pdf/video/audio/images)

Загрузки вынесены во внешний volume:
- в compose: `./uploads:/app/uploads`
- в приложении путь задается через `APP_STORAGE_UPLOAD_DIR` (по умолчанию `/app/uploads`)

Это значит, что при пересборке/перезапуске контейнера ваши загруженные файлы сохраняются на сервере в папке `uploads` рядом с `docker-compose`.
