plugins {
    java
}

group = "me.jibai"
version = "1.0.0"

repositories {
    mavenCentral()
    // Spigot API 官方快照仓库
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    // Sonatype 快照仓库（spigot-api 的部分传递依赖需要）
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    // Paper 仓库作为镜像补充，只在解析依赖时使用
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    // 使用 Spigot API 1.20.1 编译。
    // 之所以选 spigot-api 而不是 paper-api，是为了在编译期就强制只使用 Bukkit/Spigot API，
    // 避免误用 Paper 专属方法，从而保证插件可在 Bukkit / Spigot / Paper / Purpur 上通用。
    compileOnly("org.spigotmc:spigot-api:1.20.1-R0.1-SNAPSHOT")
}

java {
    // 编译目标 Java 17，兼容 Minecraft 1.20.x ~ 1.21.x 常见运行环境。
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
    archiveFileName.set("JibaiServerDoctor-${version}.jar")
}
