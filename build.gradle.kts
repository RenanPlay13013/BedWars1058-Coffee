plugins {
    id("com.gradleup.shadow") version "9.4.2" apply false
}

group = "com.andrei1058.bedwars"
version = "25.9"

allprojects {
    group = rootProject.group
    version = rootProject.version

    repositories {
        mavenCentral()
        mavenLocal()

        maven("https://oss.sonatype.org/content/repositories/snapshots")
        maven("https://repo.codemc.io/repository/nms/")
        maven("https://repo.andrei1058.com/snapshots")
        maven("https://repo.andrei1058.com/releases")

        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
        maven("https://hub.spigotmc.org/nexus/repository/public/")
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

subprojects {

    apply(plugin = "java")

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }

    tasks.withType<Javadoc> {
        enabled = false
    }

    // FORÇA Java 21 do jeito certo (sem java toolchain DSL quebrando)
    extensions.configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}