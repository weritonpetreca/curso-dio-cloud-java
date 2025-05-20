plugins {
    id("java")
}

group = "com.petreca"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    // Adicione repositórios usando a sintaxe correta para Kotlin DSL
    maven {
        url = uri("https://mvnrepository.com/artifact/com.mysql/mysql-connector-j")
    }

    maven {
        url = uri("https://repo.maven.apache.org/maven2/")
    }
    maven {
        url = uri("https://repo1.maven.org/maven2/")
    }
    // O jcenter() é uma função, portanto permanece igual
}

dependencies {
    implementation("org.liquibase:liquibase-core:4.29.1")
    implementation("com.mysql:mysql-connector-j:9.3.0")
    implementation("org.projectlombok:lombok:1.18.34")

    annotationProcessor("org.projectlombok:lombok:1.18.34")
}

tasks.test {
    useJUnitPlatform()
}