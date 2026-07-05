plugins {
    java
}

group = "me.jibai"
version = "1.0.0"

repositories {
    mavenCentral()
    // Spigot API（spigot-api）官方快照仓库
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    // Sonatype 快照仓库（spigot-api 的部分传递依赖需要）
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
}

dependencies {
    // 使用 Spigot API 编译，保证真正兼容 Bukkit / Spigot / Paper / Purpur 全核心。
    // 只用 Bukkit/Spigot 通用 API，不碰 Paper 专属的 Adventure / getTPS 等方法。
    // compileOnly 表示只在编译期使用，运行时由服务端提供。
    compileOnly("org.spigotmc:spigot-api:1.20.1-R0.1-SNAPSHOT")
}

java {
    // 编译目标 Java 17，兼容 Minecraft 1.20.x ~ 1.21.x 常见运行环境。
    // 使用 sourceCompatibility/targetCompatibility，可用 JDK 17 及以上任意版本构建。
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    // 以 Java 17 为发布目标，产出可在 Java 17 运行的字节码
    options.release.set(17)
    // 保留参数名，便于调试
    options.compilerArgs.add("-parameters")
}

tasks.processResources {
    // 让资源文件中的 ${version} 占位符自动替换为项目版本号
    val props = mapOf("version" to version)
    inputs.properties(props)
    filesMatching("plugin.yml") {
        expand(props)
    }
}

tasks.jar {
    archiveFileName.set("JibaiOptimizer-${version}.jar")
}
