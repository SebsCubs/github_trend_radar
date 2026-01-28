plugins {
    // These define the versions but don't apply them to the root project
    java
    id("org.springframework.boot") version "3.3.0" apply false
    id("io.spring.dependency-management") version "1.1.5" apply false
}

allprojects {
    group = "com.trendradar"
    version = "1.0.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "io.spring.dependency-management")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    // Common dependencies for ALL services (Logging, Testing, etc.)
    dependencies {
        implementation("org.slf4j:slf4j-api")
        
        // Testing Essentials
        testImplementation("org.junit.jupiter:junit-jupiter")
        testImplementation("org.testcontainers:junit-jupiter:1.19.7")
        testImplementation("org.testcontainers:kafka:1.19.7")
        testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}