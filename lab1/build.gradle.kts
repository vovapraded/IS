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

dependencies {
    // CDI (Weld SE для локального запуска)
    implementation("org.jboss.weld.se:weld-se-core:$weldVersion")

    // Jakarta EE APIs
    implementation("jakarta.ejb:jakarta.ejb-api:4.0.1")
    implementation("jakarta.persistence:jakarta.persistence-api:3.1.0")
    implementation("jakarta.validation:jakarta.validation-api:3.0.2")
    implementation("jakarta.ws.rs:jakarta.ws.rs-api:3.1.0")

    // Jersey (JAX-RS implementation)
    implementation("org.glassfish.jersey.containers:jersey-container-servlet:3.1.0")
    implementation("org.glassfish.jersey.inject:jersey-hk2:3.1.0")
    implementation("org.glassfish.jersey.core:jersey-server:3.1.0")
    implementation("org.glassfish.jersey.containers:jersey-container-grizzly2-http:3.1.0")
    implementation("org.glassfish.jersey.media:jersey-media-sse:3.1.0")
    implementation("org.glassfish.jersey.media:jersey-media-json-jackson:3.1.0")

    // JPA provider
    implementation("org.hibernate.orm:hibernate-core:$hibernateVersion")

    // Databases
    implementation("com.h2database:h2:2.2.224")
    implementation("org.postgresql:postgresql:42.6.0")

    // Logging
    implementation("org.jboss.logging:jboss-logging:3.5.1.Final")
    implementation("org.slf4j:slf4j-simple:2.0.9")

    // Lombok — только на compile-time
    compileOnly("org.projectlombok:lombok:$lombokVersion")
    annotationProcessor("org.projectlombok:lombok:$lombokVersion")

    // Тесты
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")

    compileOnly("jakarta.servlet:jakarta.servlet-api:6.0.0")
}

application {
    mainClass.set("org.example.App")
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
