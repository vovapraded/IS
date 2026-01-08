import org.gradle.api.JavaVersion

plugins {
    java
    application
    war
}

group = "org.example"
version = "0.1.0"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

val jakartaVersion = "3.1.0"
val weldVersion = "5.0.0.Final"
val hibernateVersion = "6.3.0.Final"
val lombokVersion = "1.18.30" // актуальная версия на 2024
val swaggerVersion = "2.2.8"
val openApiVersion = "3.0.1"

dependencies {
    // Jakarta EE APIs - provided by WildFly
    providedCompile("jakarta.ejb:jakarta.ejb-api:4.0.1")
    providedCompile("jakarta.persistence:jakarta.persistence-api:3.1.0")
    providedCompile("jakarta.validation:jakarta.validation-api:3.0.2")
    providedCompile("jakarta.ws.rs:jakarta.ws.rs-api:3.1.0")
    providedCompile("jakarta.servlet:jakarta.servlet-api:6.0.0")
    providedCompile("jakarta.enterprise:jakarta.enterprise.cdi-api:4.0.1")

    // Lombok — только на compile-time
    compileOnly("org.projectlombok:lombok:$lombokVersion")
    annotationProcessor("org.projectlombok:lombok:$lombokVersion")

    // OpenAPI/Swagger dependencies - совместимые с WildFly
    implementation("io.swagger.core.v3:swagger-core-jakarta:$swaggerVersion")
    implementation("io.swagger.core.v3:swagger-jaxrs2-jakarta:$swaggerVersion")
    implementation("io.swagger.core.v3:swagger-annotations-jakarta:$swaggerVersion")
    implementation("io.swagger.core.v3:swagger-models-jakarta:$swaggerVersion")
    implementation("io.swagger.core.v3:swagger-integration-jakarta:$swaggerVersion")
    
    // Swagger UI статические ресурсы через WebJars
    implementation("org.webjars:swagger-ui:5.10.3")
    
    // Jackson для JSON обработки (если не provided by WildFly)
    implementation("com.fasterxml.jackson.core:jackson-core:2.15.2")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.15.2")

    // Тесты
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    
    // Для локального тестирования - добавляем как testImplementation
    testImplementation("org.jboss.weld.se:weld-se-core:$weldVersion")
    testImplementation("org.glassfish.jersey.containers:jersey-container-servlet:3.1.0")
    testImplementation("org.glassfish.jersey.inject:jersey-hk2:3.1.0")
    testImplementation("org.hibernate.orm:hibernate-core:$hibernateVersion")
    testImplementation("com.h2database:h2:2.2.224")
    testImplementation("org.postgresql:postgresql:42.6.0")
}


tasks.test {
    useJUnitPlatform()
}

// Make SQL init script available on classpath at /db/init.sql
sourceSets {
    named("main") {
        resources {
            srcDir("db")
        }
    }
}

// Configure WAR packaging so we can produce server.war
tasks.war {
    archiveBaseName.set("server")
    // By default Gradle WAR includes main outputs and runtimeClasspath deps under WEB-INF/lib
    // Keep embedded libs to make it self-contained.
    // Если деплоить на WildFly, лучше вынести jakarta.* как providedCompile.
}
