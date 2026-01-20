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

val lombokVersion = "1.18.30"
val swaggerVersion = "2.2.8"

dependencies {
    // Jakarta EE APIs - provided by WildFly
    providedCompile("jakarta.ejb:jakarta.ejb-api:4.0.1")
    providedCompile("jakarta.persistence:jakarta.persistence-api:3.1.0")
    providedCompile("jakarta.validation:jakarta.validation-api:3.0.2")
    providedCompile("jakarta.ws.rs:jakarta.ws.rs-api:3.1.0")
    providedCompile("jakarta.servlet:jakarta.servlet-api:6.0.0")
    providedCompile("jakarta.enterprise:jakarta.enterprise.cdi-api:4.0.1")

    // Lombok
    compileOnly("org.projectlombok:lombok:$lombokVersion")
    annotationProcessor("org.projectlombok:lombok:$lombokVersion")

    // OpenAPI/Swagger dependencies
    implementation("org.webjars:swagger-ui:5.10.3")
    implementation("io.swagger.core.v3:swagger-core-jakarta:$swaggerVersion")
    implementation("io.swagger.core.v3:swagger-jaxrs2-jakarta:$swaggerVersion")
    implementation("io.swagger.core.v3:swagger-annotations-jakarta:$swaggerVersion")
    implementation("io.swagger.core.v3:swagger-models-jakarta:$swaggerVersion")
    implementation("io.swagger.core.v3:swagger-integration-jakarta:$swaggerVersion")
    
    // Apache Commons DBCP2 для Connection Pool
    implementation("org.apache.commons:commons-dbcp2:2.11.0")
    implementation("org.apache.commons:commons-pool2:2.12.0")
    
    // PostgreSQL JDBC драйвер
    implementation("org.postgresql:postgresql:42.7.3")
}


tasks.test {
    useJUnitPlatform()
}

tasks.war {
    archiveBaseName.set("server")
}
