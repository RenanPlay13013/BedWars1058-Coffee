plugins {
    id("com.gradleup.shadow") version "9.4.2" apply false
}

group = "com.andrei1058.bedwars"
version = "25.9"

allprojects {
    group = rootProject.group
    version = rootProject.version
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