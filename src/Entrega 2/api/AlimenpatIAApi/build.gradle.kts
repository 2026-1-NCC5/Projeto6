plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(ktorLibs.plugins.ktor)
    kotlin("plugin.serialization") version "1.9.0"  // 请确保版本号是最新的

}

group = "com.alimenpatia"
version = "1.0.0-SNAPSHOT"


kotlin {
    jvmToolchain(21)
}
dependencies {
    implementation("mysql:mysql-connector-java:8.0.33")
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("mysql:mysql-connector-java:8.0.33")
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("io.ktor:ktor-server-core:3.4.0")  // 替换为最新版本
    implementation("io.ktor:ktor-server-netty:3.4.0")  // 替换为最新版本
    implementation("io.ktor:ktor-server-content-negotiation:3.4.0")  // ContentNegotiation 插件
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.4.0")  // Kotlinx JSON 序列化
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")  // 确保使用最新版本
    implementation("ch.qos.logback:logback-classic:1.5.6")  // 添加 Logback 依赖
    implementation("org.slf4j:slf4j-api:2.0.0") // 添加 SLF4J API 依赖
    implementation("io.ktor:ktor-server-config-yaml")

}
