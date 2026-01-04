# Liquibase Database Migrations

Проект использует Liquibase для управления схемой базы данных.

## Структура

```
db/liquibase/
├── db.changelog-master.yaml          # Главный changelog файл
└── changelogs/
    ├── 001-initial-schema.yaml       # Создание таблицы routes
    └── 002-functions.yaml            # PostgreSQL функции
```

## Использование

### Запуск миграций
```bash
docker-compose up
```

Liquibase автоматически применит все миграции при запуске.

### Просмотр статуса
```bash
docker-compose run --rm liquibase liquibase status
```

### Просмотр истории
```bash
docker-compose run --rm liquibase liquibase history
```

### Rollback (если нужен)
```bash
docker-compose run --rm liquibase liquibase rollback-count 1
```

## Создание новых миграций

1. Создайте новый файл `db/liquibase/changelogs/003-your-change.yaml`
2. Добавьте включение в `db.changelog-master.yaml`:
   ```yaml
   - include:
       file: db/liquibase/changelogs/003-your-change.yaml
   ```
3. Запустите `docker-compose up liquibase`

## Преимущества Liquibase

• Версионированные изменения схемы БД
• Автоматическое применение миграций
• Откат изменений при необходимости
• Контроль изменений в команде
• Поддержка различных БД