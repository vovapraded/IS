# Настройка WildFly для работы с PostgreSQL

## Проблема
Ошибка "This connection has been closed" означает, что DataSource в WildFly неправильно настроен.

## Быстрое решение:

### 1. Скачайте PostgreSQL JDBC драйвер
```bash
wget https://jdbc.postgresql.org/download/postgresql-42.7.3.jar -P ~/Downloads/
```

### 2. Добавьте драйвер как модуль в WildFly
```bash
cd ~/Documents/wildfly-37.0.1.Final
mkdir -p modules/system/layers/base/org/postgresql/main
cp ~/Downloads/postgresql-42.7.3.jar modules/system/layers/base/org/postgresql/main/
```

### 3. Создайте module.xml
```bash
cat > modules/system/layers/base/org/postgresql/main/module.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<module xmlns="urn:jboss:module:1.9" name="org.postgresql">
    <resources>
        <resource-root path="postgresql-42.7.3.jar"/>
    </resources>
    <dependencies>
        <module name="java.base"/>
        <module name="java.logging"/>
        <module name="java.xml"/>
        <module name="java.sql"/>
        <module name="jakarta.transaction.api"/>
    </dependencies>
</module>
EOF
```

### 4. Остановите WildFly
```bash
# Если WildFly запущен, остановите его (Ctrl+C в терминале)
```

### 5. Отредактируйте standalone.xml
Откройте файл `~/Documents/wildfly-37.0.1.Final/standalone/configuration/standalone.xml`

Найдите секцию `<subsystem xmlns="urn:jboss:domain:datasources:7.0">` и замените содержимое на:

```xml
<subsystem xmlns="urn:jboss:domain:datasources:7.0">
    <datasources>
        <datasource jndi-name="java:jboss/datasources/ExampleDS" pool-name="ExampleDS" enabled="true" use-java-context="true" statistics-enabled="${wildfly.datasources.statistics-enabled:${wildfly.statistics-enabled:false}}">
            <connection-url>jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=LEGACY</connection-url>
            <driver>h2</driver>
            <security>
                <user-name>sa</user-name>
                <password>sa</password>
            </security>
        </datasource>
        <datasource jndi-name="java:/jdbc/RoutesDS" pool-name="RoutesDS" enabled="true" use-java-context="true">
            <connection-url>jdbc:postgresql://localhost:5432/database</connection-url>
            <driver>postgresql</driver>
            <pool>
                <min-pool-size>5</min-pool-size>
                <max-pool-size>20</max-pool-size>
                <prefill>true</prefill>
            </pool>
            <security>
                <user-name>admin</user-name>
                <password>admin</password>
            </security>
            <validation>
                <validate-on-match>true</validate-on-match>
                <background-validation>false</background-validation>
            </validation>
            <timeout>
                <idle-timeout-minutes>1</idle-timeout-minutes>
                <query-timeout>600</query-timeout>
            </timeout>
        </datasource>
        <drivers>
            <driver name="h2" module="com.h2database.h2">
                <xa-datasource-class>org.h2.jdbcx.JdbcDataSource</xa-datasource-class>
            </driver>
            <driver name="postgresql" module="org.postgresql">
                <xa-datasource-class>org.postgresql.xa.PGXADataSource</xa-datasource-class>
            </driver>
        </drivers>
    </datasources>
</subsystem>
```

### 6. Запустите WildFly заново
```bash
cd ~/Documents/wildfly-37.0.1.Final
bin/standalone.sh
```

### 7. Проверьте DataSource
После запуска WildFly откройте http://localhost:9990/console и проверьте, что DataSource "RoutesDS" создался и активен.

## Альтернативное решение (если не работает)

Попробуйте использовать H2 базу данных (встроенную) для тестирования. Измените persistence.xml:

```xml
<jta-data-source>java:jboss/datasources/ExampleDS</jta-data-source>
```

И в properties замените:
```xml
<property name="hibernate.dialect" value="org.hibernate.dialect.H2Dialect"/>