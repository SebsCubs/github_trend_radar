plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

dependencies {
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    
    // Kafka
    implementation("org.springframework.kafka:spring-kafka")
    
    // Common models
    implementation(project(":common_models"))
    
    // Jackson for JSON
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
}