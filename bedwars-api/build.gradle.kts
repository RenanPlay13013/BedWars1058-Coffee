plugins {
    `java-library`
    `maven-publish`
}

repositories {
    maven("https://repo.fusesource.com/nexus/content/repositories/releases-3rd-party/")
}

dependencies {
    compileOnly("com.google.common:google-collect:1.0")
    compileOnly("org.spigotmc:spigot:1.16.5-R0.1-SNAPSHOT")
    implementation("com.andrei1058.spigot.sidebar:sidebar-dist:25.2.2-SNAPSHOT")
    compileOnly("net.md-5:bungeecord-chat:1.21-R0.2")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
