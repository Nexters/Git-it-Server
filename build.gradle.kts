plugins {
    kotlin("jvm") version "2.3.21"
    kotlin("plugin.spring") version "2.3.21"
    id("org.springframework.boot") version "4.1.0"
    id("io.spring.dependency-management") version "1.1.7"
    id("dev.detekt") version "2.0.0-alpha.3"
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
}

group = "com.nexters"
version = "0.0.1-SNAPSHOT"
description = "git-it"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("tools.jackson.module:jackson-module-kotlin")

    // Logging
    implementation("io.github.oshai:kotlin-logging-jvm:${properties["kotlinLoggingVersion"]}")

    // API Docs
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:${properties["springdocVersion"]}")

    // JWT
    implementation("com.nimbusds:nimbus-jose-jwt:${properties["nimbusVersion"]}")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-mongodb-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("io.kotest:kotest-assertions-core:${properties["kotestVersion"]}")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-mongodb")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

detekt {
    buildUponDefaultConfig = true
    parallel = true
    config.setFrom("$projectDir/config/detekt/detekt.yml")
}

ktlint {
    version = properties["ktlintVersion"] as String
}

tasks.test {
    useJUnitPlatform {
        excludeTags("network")
    }
}

// 외부 상태에 결과가 달려 있어 기본 test에서 뺀다. 네트워크나 GitHub이 흔들리면 우리 잘못 없이 빨간불이 된다.
// 캐시도 끈다 — 지난번 통과했다고 건너뛰면 정작 확인하려던 것을 확인하지 않는다.
tasks.register<Test>("networkTest") {
    testClassesDirs =
        sourceSets.test
            .get()
            .output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform {
        includeTags("network")
    }
    outputs.upToDateWhen { false }
}
